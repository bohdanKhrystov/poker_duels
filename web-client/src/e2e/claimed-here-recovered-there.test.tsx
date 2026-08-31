import { act, cleanup, fireEvent, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import {
  ACCOUNT_HEADING,
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGNED_UP,
  SIGN_IN_HEADING,
  SIGN_IN_LABEL,
  SIGN_OUT_LABEL,
  SIGN_OUT_WARNING,
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
  offerSettled: () => false,
  settleOffer: () => {},
}));

vi.mock("../main", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../main")>();
  return {
    ...actual,
    useHistory: () => wiring.history,
    useSignedIn: () => wiring.signedIn,
    offerSettledHere: () => wiring.offerSettled(),
    settleOfferHere: () => wiring.settleOffer(),
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

it("no request the second client made carries a player id", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageB = inMemoryStorage();
  const HANDLE = "duelist-three";
  const PASSWORD = "a-third-password";

  // B's arc: play, boot, sign-in, reboot
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const { container: containerB1 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  // B signs up and signs in
  act(() => {
    fireEvent.click(
      within(containerB1).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB1 = await within(containerB1).findByLabelText("account");
  const signUpFormB1 = within(accountRegionB1).getByLabelText(
    "sign up for an account",
  );

  fireEvent.change(within(signUpFormB1).getByLabelText(HANDLE_LABEL), {
    target: { value: HANDLE },
  });
  fireEvent.change(within(signUpFormB1).getByLabelText(PASSWORD_LABEL), {
    target: { value: PASSWORD },
  });
  fireEvent.click(
    within(signUpFormB1).getByRole("button", { name: SIGN_UP_LABEL }),
  );

  await within(signUpFormB1).findByText(SIGNED_UP);

  // Now sign in with new credentials
  const accountRegionB2 = await within(containerB1).findByLabelText("account");
  act(() => {
    fireEvent.click(
      within(accountRegionB2).getByRole("button", { name: SIGN_IN_HEADING }),
    );
  });

  const signInFormB2 = await within(containerB1).findByLabelText(
    "sign in to an account",
  );
  fireEvent.change(within(signInFormB2).getByLabelText(HANDLE_LABEL), {
    target: { value: HANDLE },
  });
  fireEvent.change(within(signInFormB2).getByLabelText(PASSWORD_LABEL), {
    target: { value: PASSWORD },
  });
  await act(async () => {
    fireEvent.click(
      within(signInFormB2).getByRole("button", { name: SIGN_IN_LABEL }),
    );
  });

  // Reboot with session
  cleanup();
  bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  const sessionToken = readSessionToken(storageB);

  // Sweep through all recorded requests
  const requests = server.requests;

  // Presence first: the log is non-empty
  expect(requests.length).toBeGreaterThan(0);

  // Check that no path contains player seat identifiers
  for (const request of requests) {
    expect(
      request.path.includes("player-seat-0"),
      `Path ${request.path} must not contain player-seat-0`,
    ).toBe(false);
    expect(
      request.path.includes("player-seat-1"),
      `Path ${request.path} must not contain player-seat-1`,
    ).toBe(false);

    // Check that session token is not in path or body
    if (sessionToken) {
      expect(
        request.path.includes(sessionToken),
        `Path ${request.path} must not contain session token`,
      ).toBe(false);
      if (request.body) {
        expect(
          request.body.includes(sessionToken),
          `Body must not contain session token`,
        ).toBe(false);
      }
    }

    // Check body keys for forbidden ones. Parsing is the only thing this
    // try guards — the key-check expect()s below run outside it, or a
    // failing assertion (itself a throw) would be caught by a catch meant
    // only for a body that isn't JSON, and silently discarded.
    if (request.body) {
      let parsedBody: unknown;
      try {
        parsedBody = JSON.parse(request.body);
      } catch {
        // If body is not JSON, skip the key check — nothing to inspect.
        parsedBody = undefined;
      }
      if (parsedBody !== undefined) {
        const keys = Object.keys(parsedBody as Record<string, unknown>);
        const forbiddenKeys = ["playerId", "player_id", "id"];
        for (const forbidden of forbiddenKeys) {
          expect(
            keys.includes(forbidden),
            `Body keys ${keys.join(", ")} must not include ${forbidden}`,
          ).toBe(false);
        }
      }
    }
  }

  cleanup();
});

it("the second client learns who it is only from an answer", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageA = inMemoryStorage();
  const storageB = inMemoryStorage();
  const HANDLE = "duelist-four";
  const PASSWORD = "a-fourth-password";

  // A's arc: play and sign up. The credential this mints names player-seat-0
  // (ADR-0030 §1: sign-up resolves by the requesting device) — the same
  // shape test 4 already proved. B must sign in with A's credentials below,
  // never its own: a self-sign-up would mint a credential naming
  // player-seat-1, and this test could then never observe player-seat-0 in
  // any answer at all.
  driveScriptedDuel({ viewerSeat: 0, storage: storageA });
  cleanup();

  const { container: containerA } = bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

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

  // B's arc: its own boot and its own device id, then sign in with A's
  // credentials above.
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const { container: containerB1 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  act(() => {
    fireEvent.click(
      within(containerB1).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB = await within(containerB1).findByLabelText("account");
  act(() => {
    fireEvent.click(
      within(accountRegionB).getByRole("button", { name: SIGN_IN_HEADING }),
    );
  });

  const signInForm = await within(containerB1).findByLabelText(
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

  cleanup();

  // Reboot under the session — the same shape a document reload is
  // elsewhere in this file. The mount's own profile read is the answer
  // under test.
  bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  // The request that told B who it is: the reboot's GET /api/me, the only
  // request in this arc carrying a live session's Authorization header.
  const meRead = server.requests.find(
    (request) =>
      request.path === "/api/me" &&
      request.method === "GET" &&
      request.headers["Authorization"] !== undefined,
  );

  expect(meRead).toBeDefined();
  if (meRead === undefined) {
    throw new Error("expected a session-authorized GET /api/me in the log");
  }

  // The positive half: player-seat-0 appears in what the server answered.
  // A fresh call replaying meRead's own recorded headers, not a second
  // request invented here — GET /api/me is a pure read with no side
  // effects (the raw fetch a few tests up relies on the same fact), so this
  // reproduces exactly what B received without account-server.ts needing
  // to log responses too.
  const response = await server.fetch(meRead.path, {
    method: meRead.method,
    headers: meRead.headers,
  });
  const responseBody = (await response.json()) as {
    readonly playerId: string;
  };
  expect(responseBody.playerId).toBe(PLAYER_SEAT_0.playerId);

  // ...and the request that earned it carried no player id anywhere: only
  // Authorization and B's own X-Device-Id.
  expect(meRead.path.includes(PLAYER_SEAT_0.playerId)).toBe(false);
  expect(meRead.body).toBeNull();
  expect(meRead.headers["X-Device-Id"]).toBe(PLAYER_SEAT_1.deviceId);

  const headerKeys = Object.keys(meRead.headers);
  const allowedKeys = [
    "Authorization",
    "X-Device-Id",
    "Content-Type",
    "content-type",
  ];
  for (const key of headerKeys) {
    expect(
      allowedKeys.includes(key),
      `Unexpected header ${key} in the request that told B who it is`,
    ).toBe(true);
  }
  expect(meRead.headers["Authorization"]?.startsWith("Bearer ")).toBe(true);

  cleanup();
});

it("signing out on the second client returns it to the profile it had", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageA = inMemoryStorage();
  const storageB = inMemoryStorage();
  const HANDLE = "duelist-sign-out-test";
  const PASSWORD = "sign-out-password-test";
  const NEW_NAME = "Signed In Test";

  // A's arc: play, boot, name, sign up
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

  // B's arc: play, boot
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const { container: containerB1 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  // Capture B's anonymous profile before signing in
  const profileRegionB1 =
    await within(containerB1).findByLabelText("your profile");
  const nameRegionB1 =
    await within(containerB1).findByLabelText("your display name");
  const balanceFromB =
    within(profileRegionB1).getByText(/Duel coins$/).textContent;
  const nameFromB = within(nameRegionB1).getByText(
    nameOrNone(PLAYER_SEAT_1.displayName),
  ).textContent;

  // Sign in B with A's credentials
  act(() => {
    fireEvent.click(
      within(containerB1).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB1 = await within(containerB1).findByLabelText("account");
  act(() => {
    fireEvent.click(
      within(accountRegionB1).getByRole("button", { name: SIGN_IN_HEADING }),
    );
  });

  const signInForm = await within(containerB1).findByLabelText(
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
  expect(readDeviceId(storageB)).toBe(PLAYER_SEAT_1.deviceId);

  cleanup();

  // Boot B again (simulating the reload from sign-in)
  const { container: containerB2 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  act(() => {
    fireEvent.click(within(containerB2).getByRole("button", { name: "Back" }));
  });

  const profileRegionB2 =
    await within(containerB2).findByLabelText("your profile");
  const nameRegionB2 =
    await within(containerB2).findByLabelText("your display name");

  // Verify B is signed in with A's profile at this point
  within(profileRegionB2).getByText(
    `${coinBalanceText(PLAYER_SEAT_0.coinBalance)} Duel coins`,
  );
  within(nameRegionB2).getByText(NEW_NAME);

  // Navigate to account screen and sign out
  act(() => {
    fireEvent.click(
      within(containerB2).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB2 = await within(containerB2).findByLabelText("account");

  // Click sign-out label (first click shows confirmation)
  fireEvent.click(
    within(accountRegionB2).getByRole("button", { name: SIGN_OUT_LABEL }),
  );

  // Verify warning appears
  within(accountRegionB2).getByText(SIGN_OUT_WARNING);

  // Click confirmation button (also labeled with SIGN_OUT_LABEL)
  await act(async () => {
    fireEvent.click(
      within(accountRegionB2).getByRole("button", { name: SIGN_OUT_LABEL }),
    );
  });

  // After sign-out, storage should be cleared
  expect(readSessionToken(storageB)).toBeNull();
  expect(readDeviceId(storageB)).toBe(PLAYER_SEAT_1.deviceId);

  cleanup();

  // Boot B again with wiring.signedIn = false (because token is gone)
  window.location.hash = "";
  const { container: containerB3 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  const profileRegionB3 =
    await within(containerB3).findByLabelText("your profile");
  const nameRegionB3 =
    await within(containerB3).findByLabelText("your display name");

  const balanceAfterSignOut =
    within(profileRegionB3).getByText(/Duel coins$/).textContent;
  const nameAfterSignOut = within(nameRegionB3).getByText(
    nameOrNone(PLAYER_SEAT_1.displayName),
  ).textContent;

  // Verify B is back to its anonymous profile
  expect(balanceAfterSignOut).toBe(balanceFromB);
  expect(nameAfterSignOut).toBe(nameFromB);

  // Verify B's profile is NOT A's
  expect(within(profileRegionB3).queryByText(balanceFromA)).toBeNull();
  expect(within(nameRegionB3).queryByText(nameFromA)).toBeNull();

  cleanup();
});

it("the first client is unaffected by the second signing out", async () => {
  const server = accountServer([PLAYER_SEAT_0, PLAYER_SEAT_1]);
  const storageA = inMemoryStorage();
  const storageB = inMemoryStorage();
  const HANDLE = "duelist-first-unaffected";
  const PASSWORD = "first-unaffected-password";
  const NEW_NAME = "First Unaffected Name";

  // A's arc: play, boot, name, sign up
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

  // Verify A's storage before B's sign-out. A's claim signs it in right afterwards
  // (`TASK-120601`), so this browser now holds a session rather than none.
  expect(readDeviceId(storageA)).toBe(PLAYER_SEAT_0.deviceId);
  expect(readSessionToken(storageA)).not.toBeNull();

  cleanup();

  // B's arc: play, boot, sign in, sign out
  driveScriptedDuel({ viewerSeat: 1, storage: storageB });
  cleanup();

  const { container: containerB1 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  // Sign in B with A's credentials
  act(() => {
    fireEvent.click(
      within(containerB1).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB1 = await within(containerB1).findByLabelText("account");
  act(() => {
    fireEvent.click(
      within(accountRegionB1).getByRole("button", { name: SIGN_IN_HEADING }),
    );
  });

  const signInForm = await within(containerB1).findByLabelText(
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

  cleanup();

  // Boot B again
  const { container: containerB2 } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_1.deviceId),
  });

  act(() => {
    fireEvent.click(within(containerB2).getByRole("button", { name: "Back" }));
  });

  await within(containerB2).findByLabelText("your profile");

  // Navigate to account and sign out
  act(() => {
    fireEvent.click(
      within(containerB2).getByRole("button", { name: ACCOUNT_HEADING }),
    );
  });
  const accountRegionB2 = await within(containerB2).findByLabelText("account");

  fireEvent.click(
    within(accountRegionB2).getByRole("button", { name: SIGN_OUT_LABEL }),
  );

  within(accountRegionB2).getByText(SIGN_OUT_WARNING);

  await act(async () => {
    fireEvent.click(
      within(accountRegionB2).getByRole("button", { name: SIGN_OUT_LABEL }),
    );
  });

  cleanup();

  // Verify A's storage is unchanged: still its device id, and still the session
  // its own claim opened — B signing out never touches A's storage.
  expect(readDeviceId(storageA)).toBe(PLAYER_SEAT_0.deviceId);
  expect(readSessionToken(storageA)).not.toBeNull();

  // Boot A fresh to verify it renders unchanged
  window.location.hash = "";
  const { container: containerA2 } = bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(PLAYER_SEAT_0.deviceId),
  });

  const profileRegionA2 =
    await within(containerA2).findByLabelText("your profile");
  const nameRegionA2 =
    await within(containerA2).findByLabelText("your display name");

  const balanceAfterB =
    within(profileRegionA2).getByText(/Duel coins$/).textContent;
  const nameAfterB = within(nameRegionA2).getByText(NEW_NAME).textContent;

  expect(balanceAfterB).toBe(balanceFromA);
  expect(nameAfterB).toBe(nameFromA);

  cleanup();
});
