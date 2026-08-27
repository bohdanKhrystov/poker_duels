import { act, cleanup, fireEvent, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import {
  ACCOUNT_HEADING,
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGNED_UP,
  SIGN_UP_LABEL,
} from "../account/account-text";
import { duelRowBody } from "../profile/profile-fixture";
import { coinBalanceText } from "../profile/profile-text";
import { PROTOCOL_VERSION } from "../protocol";
import type { ServerMessage } from "../protocol";
import { readDeviceId } from "../protocol/device-id";
import { accountServer, type ServerPlayer } from "./account-server";
import { bootClient, type ArcWiring } from "./drive-arc";
import { driveScriptedDuel, inMemoryStorage } from "./drive-duel";

// `Lobby.tsx` reads `useHistory` and `useSignedIn` from `../main`'s module
// scope, not from a prop, so there is no seam `bootClient` could pass them
// through. `wiring` is the plain object `bootClient` writes into on every
// boot (`ArcWiring` in `drive-arc.tsx`); this partial mock reads it back on
// every render. A wholesale `vi.mock("../main", …)` (`App.test.tsx:41`'s
// shape) would replace the module for every test in this file and export
// none of `../main`'s real bindings — this is `TASK-041406`'s shape
// (`Lobby.test.tsx:41-47`).
const wiring = vi.hoisted((): ArcWiring => ({
  history: null,
  signedIn: false,
}));

vi.mock("../main", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../main")>();
  return {
    ...actual,
    useHistory: () => wiring.history,
    useSignedIn: () => wiring.signedIn,
  };
});

// `scripted-duel.gen.json`'s own Welcome frames name these two identities —
// not invented here, so a boot that plays the duel and a boot that reads the
// profile afterwards agree on the one the script actually seated.
const PLAYER_SEAT_0: ServerPlayer = {
  playerId: "player-seat-0",
  deviceId: "device-seat-0",
  // Independent of PLAYER_SEAT_1's balance below (neither adds, subtracts,
  // doubles nor halves into the other): a pair that differed only by a
  // suffix would let a strip showing the wrong player's number still pass.
  coinBalance: 100,
  displayName: null,
  // The script's last frame both seats see carries
  // {"winner":0,"handsPlayed":7,"finalStacks":[3000,0]} — this row states
  // the duel that was actually played, not an invented one.
  duels: [duelRowBody({ outcome: "WON", handsPlayed: 7 })],
};

const PLAYER_SEAT_1: ServerPlayer = {
  playerId: "player-seat-1",
  deviceId: "device-seat-1",
  coinBalance: 37,
  displayName: "Already Named",
  duels: [duelRowBody({ outcome: "LOST", handsPlayed: 7 })],
};

/** A `Welcome` frame naming `deviceId`, encoded exactly as the real server would send it. */
function welcomeFrame(deviceId: string): string {
  const message: ServerMessage = {
    type: "Welcome",
    playerId: `duel-${deviceId}`,
    deviceId,
    protocolVersion: PROTOCOL_VERSION,
  };
  return JSON.stringify(message);
}

beforeEach(() => {
  // The address is module-global (use-screen.ts), so a hash a previous test
  // left behind would seat this one on the wrong screen.
  window.location.hash = "";
});

afterEach(() => {
  cleanup();
});

it("plays a duel anonymously and reads back the coin the server sent", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageA = inMemoryStorage();

  const run = driveScriptedDuel({ viewerSeat: 0, storage: storageA });
  expect(
    within(run.container).getByRole("region", { name: "the result" }),
  ).toBeDefined();
  expect(readDeviceId(storageA)).toBe(PLAYER_SEAT_0.deviceId);
  cleanup();

  const { container } = bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

  const region = await within(container).findByLabelText("your profile");

  within(region).getByText(
    `${coinBalanceText(PLAYER_SEAT_0.coinBalance)} Duel coins`,
  );
  // Not just "a number" — this player's own number, and not the only other
  // one the double could have answered with.
  expect(
    within(region).queryByText(
      `${coinBalanceText(PLAYER_SEAT_1.coinBalance)} Duel coins`,
    ),
  ).toBeNull();
});

it("names the profile and then claims it, and the claim moves neither", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storage = inMemoryStorage();
  const NEW_NAME = "Riverside";

  driveScriptedDuel({ viewerSeat: 0, storage });
  cleanup();

  const { container } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

  const nameRegion =
    await within(container).findByLabelText("your display name");
  fireEvent.change(within(nameRegion).getByRole("textbox"), {
    target: { value: NEW_NAME },
  });
  fireEvent.click(
    within(nameRegion).getByRole("button", { name: "Set my name" }),
  );
  await within(nameRegion).findByText(NEW_NAME);

  const profileRegion = within(container).getByLabelText("your profile");
  const balanceBeforeClaim =
    within(profileRegion).getByText(/Duel coins$/).textContent;
  const nameBeforeClaim = within(nameRegion).getByText(NEW_NAME).textContent;

  act(() => {
    fireEvent.click(
      within(container).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegion = await within(container).findByLabelText("account");
  const signUpForm = within(accountRegion).getByLabelText(
    "sign up for an account",
  );

  fireEvent.change(within(signUpForm).getByLabelText(HANDLE_LABEL), {
    target: { value: "duelist-one" },
  });
  fireEvent.change(within(signUpForm).getByLabelText(PASSWORD_LABEL), {
    target: { value: "a-strong-password" },
  });
  fireEvent.click(
    within(signUpForm).getByRole("button", { name: SIGN_UP_LABEL }),
  );

  await within(signUpForm).findByText(SIGNED_UP);

  // The durable record, not the tree the claim ran beside: a reboot over the
  // same storage re-fetches from the double, rather than trusting whatever
  // client-side state happened to survive the screen swap to #/account (the
  // swap unmounts the strip and the name surface — Lobby.tsx renders them
  // only on the "first" screen — so their own local state is gone by the
  // time a player would come back; only the server's own record survives).
  cleanup();
  window.location.hash = "";
  const { container: containerAfter } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

  const profileRegionAfter =
    await within(containerAfter).findByLabelText("your profile");
  const nameRegionAfter =
    await within(containerAfter).findByLabelText("your display name");

  const balanceAfterClaim =
    within(profileRegionAfter).getByText(/Duel coins$/).textContent;
  const nameAfterClaim =
    within(nameRegionAfter).getByText(NEW_NAME).textContent;

  expect(balanceAfterClaim).toBe(balanceBeforeClaim);
  expect(nameAfterClaim).toBe(nameBeforeClaim);
});
