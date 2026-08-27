import { act, cleanup, fireEvent, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import {
  ACCOUNT_HEADING,
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGNED_UP,
  SIGN_IN_HEADING,
  SIGN_IN_LABEL,
  SIGN_UP_LABEL,
} from "../account/account-text";
import { authorizedFetch } from "../account/authorized-fetch";
import { readDuelPage } from "../profile/duel-page";
import { WHOLE_RECORD } from "../profile/duels-query";
import { nameOrNone } from "../profile/name-text";
import { duelRowBody } from "../profile/profile-fixture";
import { coinBalanceText, outcomeWord } from "../profile/profile-text";
import { PROTOCOL_VERSION } from "../protocol";
import type { ServerMessage } from "../protocol";
import { readDeviceId } from "../protocol/device-id";
import { readSessionToken } from "../protocol/session-token";
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

it("the second client holds its own device id and its own profile", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  // A second, unrelated inMemoryStorage() — never storageA, never a copy of
  // it. B earns its own device id the same way A did.
  const storageA = inMemoryStorage();
  const storageB = inMemoryStorage();

  driveScriptedDuel({ viewerSeat: 0, storage: storageA });
  cleanup();
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const deviceIdA = readDeviceId(storageA);
  const deviceIdB = readDeviceId(storageB);
  expect(deviceIdA).toBe(PLAYER_SEAT_0.deviceId);
  expect(deviceIdB).toBe(PLAYER_SEAT_1.deviceId);
  // The story's own criterion: asserted from the two storages, not arranged
  // and assumed from the two constants above.
  expect(deviceIdB).not.toBe(deviceIdA);
  expect(readSessionToken(storageB)).toBeNull();

  const { container } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  const region = await within(container).findByLabelText("your profile");

  within(region).getByText(
    `${coinBalanceText(PLAYER_SEAT_1.coinBalance)} Duel coins`,
  );
  // Its own number, not the only other one a double conflating the two
  // identities could have answered with.
  expect(
    within(region).queryByText(
      `${coinBalanceText(PLAYER_SEAT_0.coinBalance)} Duel coins`,
    ),
  ).toBeNull();

  within(region).getByText(nameOrNone(PLAYER_SEAT_1.displayName));
  expect(
    within(region).queryByText(nameOrNone(PLAYER_SEAT_0.displayName)),
  ).toBeNull();
});

it("signs in from the second client and reads back the same balance name and duel", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageA = inMemoryStorage();
  const storageB = inMemoryStorage();
  const HANDLE = "duelist-two";
  const PASSWORD = "a-second-password";
  const NEW_NAME = "Riverside";

  // A's arc: play, name, and claim — the same shape as the previous test's,
  // run again here so this test owns the balance and name bindings it
  // compares B's recovery against.
  driveScriptedDuel({ viewerSeat: 0, storage: storageA });
  cleanup();

  const { container: containerA } = bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

  const nameRegionA =
    await within(containerA).findByLabelText("your display name");
  fireEvent.change(within(nameRegionA).getByRole("textbox"), {
    target: { value: NEW_NAME },
  });
  fireEvent.click(
    within(nameRegionA).getByRole("button", { name: "Set my name" }),
  );
  await within(nameRegionA).findByText(NEW_NAME);

  const profileRegionA = within(containerA).getByLabelText("your profile");
  // Bindings, not literals: what B reads back is compared against what A's
  // own strip actually rendered, never against a number retyped here.
  const balanceFromA =
    within(profileRegionA).getByText(/Duel coins$/).textContent;
  const nameFromA = within(nameRegionA).getByText(NEW_NAME).textContent;

  act(() => {
    fireEvent.click(
      within(containerA).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionA = await within(containerA).findByLabelText("account");
  const signUpForm = within(accountRegionA).getByLabelText(
    "sign up for an account",
  );

  fireEvent.change(within(signUpForm).getByLabelText(HANDLE_LABEL), {
    target: { value: HANDLE },
  });
  fireEvent.change(within(signUpForm).getByLabelText(PASSWORD_LABEL), {
    target: { value: PASSWORD },
  });
  fireEvent.click(
    within(signUpForm).getByRole("button", { name: SIGN_UP_LABEL }),
  );
  await within(signUpForm).findByText(SIGNED_UP);

  cleanup();

  // B's arc: a second, unrelated inMemoryStorage() earns its own device id
  // the same way A did.
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const { container: containerB } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  const deviceIdBeforeSignIn = readDeviceId(storageB);
  expect(deviceIdBeforeSignIn).toBe(PLAYER_SEAT_1.deviceId);

  act(() => {
    fireEvent.click(
      within(containerB).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB = await within(containerB).findByLabelText("account");
  act(() => {
    fireEvent.click(
      within(accountRegionB).getByRole("button", { name: SIGN_IN_HEADING }),
    );
  });

  const signInForm = await within(containerB).findByLabelText(
    "sign in to an account",
  );
  fireEvent.change(within(signInForm).getByLabelText(HANDLE_LABEL), {
    target: { value: HANDLE },
  });
  fireEvent.change(within(signInForm).getByLabelText(PASSWORD_LABEL), {
    target: { value: PASSWORD },
  });
  await act(async () => {
    fireEvent.click(
      within(signInForm).getByRole("button", { name: SIGN_IN_LABEL }),
    );
  });

  expect(readSessionToken(storageB)).not.toBeNull();
  // ADR-0030 §8: sign-in never touches the device id.
  expect(readDeviceId(storageB)).toBe(deviceIdBeforeSignIn);

  cleanup();

  // "Boot B again with wiring.signedIn = true" is what a document reload
  // does. The address is real — ADR-0083 §5's reloadAtAccount already moved
  // it to #/account — so this boot lands there too; Back returns to the
  // first screen, and nothing optimistic is lost the way NameSurface's
  // wonName would be, because this is a fresh mount reading the server's
  // own current record, not the tree that ran the name-setting form.
  const { container: containerB2 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  act(() => {
    fireEvent.click(within(containerB2).getByRole("button", { name: "Back" }));
  });

  const profileRegionB =
    await within(containerB2).findByLabelText("your profile");
  const nameRegionB =
    await within(containerB2).findByLabelText("your display name");

  const balanceAfterSignIn =
    within(profileRegionB).getByText(/Duel coins$/).textContent;
  const nameAfterSignIn = within(nameRegionB).getByText(NEW_NAME).textContent;

  expect(balanceAfterSignIn).toBe(balanceFromA);
  expect(nameAfterSignIn).toBe(nameFromA);
  // Not just equal to A's — also not still B's own.
  expect(
    within(profileRegionB).queryByText(
      `${coinBalanceText(PLAYER_SEAT_1.coinBalance)} Duel coins`,
    ),
  ).toBeNull();
  expect(
    within(nameRegionB).queryByText(nameOrNone(PLAYER_SEAT_1.displayName)),
  ).toBeNull();

  // The duel, matched by identity: duelId is a React key only (ProfileStrip
  // and HistoryScreen), never DOM text, so this is asserted over the read.
  const aDuelRow = PLAYER_SEAT_0.duels[0] as {
    readonly duelId: string;
    readonly outcome: "WON" | "LOST" | "DREW";
  };
  const apiFetchB = authorizedFetch(server.fetch, storageB);
  const historyRead = await readDuelPage({
    fetch: apiFetchB,
    storage: storageB,
    query: WHOLE_RECORD,
  });
  expect(historyRead.kind).toBe("page");
  if (historyRead.kind !== "page") {
    throw new Error(`expected a page, got ${historyRead.kind}`);
  }
  expect(historyRead.duels.length).toBe(1);
  expect(historyRead.duels[0]?.duelId).toBe(aDuelRow.duelId);
  expect(historyRead.duels[0]?.outcome).toBe(aDuelRow.outcome);

  // Corroborated by the rendered line too.
  within(profileRegionB).getByText((content) =>
    content.startsWith(`${outcomeWord(aDuelRow.outcome)} `),
  );

  // The other identity — player-seat-1's own anonymous record — is unmoved
  // (ADR-0027 §4: "not merged, not deleted, not relinked"). A raw, unwrapped
  // fetch, because storageB's own live session now answers for player-seat-0
  // regardless of which device id header accompanies it — the same
  // precedence that makes the read above resolve to A in the first place.
  const rawSeat1 = await server.fetch("/api/me", {
    headers: { "X-Device-Id": PLAYER_SEAT_1.deviceId },
  });
  const rawSeat1Body = (await rawSeat1.json()) as {
    readonly coinBalance: number;
    readonly displayName: string | null;
  };
  expect(rawSeat1Body.coinBalance).toBe(PLAYER_SEAT_1.coinBalance);
  expect(rawSeat1Body.displayName).toBe(PLAYER_SEAT_1.displayName);

  // storageA itself, re-read last: two identities across two device ids is
  // exactly the shape a storage-aliasing bug (one shared object standing in
  // for both) would still pass if every earlier read only ever happened
  // after B's own most recent write. Reading storageA again here, after B's
  // whole arc has run, is what a shared object could no longer answer
  // correctly.
  expect(readDeviceId(storageA)).toBe(PLAYER_SEAT_0.deviceId);
});
