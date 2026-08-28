import { act, cleanup, fireEvent, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { PROTOCOL_VERSION } from "../protocol";
import type { ServerMessage } from "../protocol";
import { readDeviceId } from "../protocol/device-id";
import { OFFER_ACCEPT, OFFER_DISMISS } from "../result/account-offer-text";
import {
  accountServer,
  type ServerPlayer,
  type AccountServer,
} from "./account-server";
import { bootClient, type ArcWiring } from "./drive-arc";
import { inMemoryStorage } from "./drive-duel";

// `Lobby.tsx:6` reads `useHistory`, `useSignedIn`, `offerSettledHere` and
// `settleOfferHere` from `../main`'s module scope, not from a prop, so
// there is no seam to pass them through `bootClient`. `wiring` is the plain
// object `bootClient` writes into on every boot (see `ArcWiring`'s doc
// comment in `drive-arc.tsx`); the mock below reads it back on every
// render. A wholesale `vi.mock("../main", …)` (`App.test.tsx:41`'s shape)
// would replace the module for every test in this file and export none of
// its real bindings, so `Lobby` could never reach `../main`'s actual
// `AccountProvider`-adjacent wiring — this partial mock, `importOriginal`
// plus a four-field override, is the merged shape (`Lobby.test.tsx:41-47`).
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

const ALICE: ServerPlayer = {
  playerId: "player-alice",
  deviceId: "device-alice",
  coinBalance: 40,
  displayName: "Alice",
  duels: [],
};

const BOB: ServerPlayer = {
  playerId: "player-bob",
  deviceId: "device-bob",
  coinBalance: 65,
  displayName: "Bob",
  duels: [],
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

/**
 * `RoomJoined` seating this browser at seat 0, then `DuelFinished` naming
 * seat 0 the winner — a duel this browser has just won, encoded exactly as
 * the real server would encode it, the same way `welcomeFrame` above does
 * for `Welcome`, never a hand-written string.
 */
function winFrames(): readonly string[] {
  const roomJoined: ServerMessage = {
    type: "RoomJoined",
    code: "ARCROOM",
    seat: 0,
  };
  const duelFinished: ServerMessage = {
    type: "DuelFinished",
    outcome: {
      winner: 0,
      handsPlayed: 1,
      finalStacks: [2000, 0],
    },
  };
  return [roomJoined, duelFinished].map((message) => JSON.stringify(message));
}

/**
 * One full boot that has already won a duel: `bootClient` over `storage` and
 * `server`, then `winFrames()` delivered through the same socket exactly as
 * `bootClient` itself replays `welcomeFrame`. The offer's three terms — the
 * store's outcome, the session token and the key — never wait on a request,
 * so nothing here is `async` and no caller needs to `await` it.
 */
function bootAndWin(storage: Storage, server: AccountServer): HTMLElement {
  const { container, socket } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });

  winFrames().forEach((frame) => {
    act(() => {
      socket.receive(frame);
    });
  });

  return container;
}

beforeEach(() => {
  // App.test.tsx:136-138's reset: the address is module-global (use-screen.ts),
  // so a hash a previous test left behind would seat this one on the wrong screen.
  window.location.hash = "";
});

afterEach(() => {
  cleanup();
});

it("a first boot mints a device id and asks the server nothing", async () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);

  const { container } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });

  await within(container).findByText("No profile yet.");

  expect(readDeviceId(storage)).toBe(ALICE.deviceId);
  expect(server.requests).toEqual([]);
});

it("a second boot over the same storage reads the profile the server holds", async () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);

  // The first boot only mints the device id (see the test above) — the
  // profile has to be read by a boot that starts after that id already sits
  // in storage, which is the second boot below.
  bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });
  cleanup();

  const { container } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });

  const region = await within(container).findByLabelText("your profile");
  within(region).getByText("Alice");
  within(region).getByText("40 Duel coins");

  expect(server.requests.map((request) => request.path).sort()).toEqual([
    "/api/me",
    "/api/me/duels",
  ]);
});

it("two storages reach two different profiles", async () => {
  const server = accountServer([ALICE, BOB]);

  const storageA = inMemoryStorage();
  bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });
  cleanup();
  const { container: containerA } = bootClient({
    storage: storageA,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });
  const regionA = await within(containerA).findByLabelText("your profile");
  const paragraphsA = regionA.querySelectorAll("p");
  const nameA = paragraphsA[0]?.textContent;
  const balanceA = paragraphsA[1]?.textContent;
  cleanup();

  const storageB = inMemoryStorage();
  bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(BOB.deviceId),
  });
  cleanup();
  const { container: containerB } = bootClient({
    storage: storageB,
    server,
    wiring,
    welcomeFrame: welcomeFrame(BOB.deviceId),
  });
  const regionB = await within(containerB).findByLabelText("your profile");
  const paragraphsB = regionB.querySelectorAll("p");
  const nameB = paragraphsB[0]?.textContent;
  const balanceB = paragraphsB[1]?.textContent;

  expect(nameA).not.toEqual(nameB);
  expect(balanceA).not.toEqual(balanceB);
});

it("offers the account after a win, and never again once this browser has answered", () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);

  const first = bootAndWin(storage, server);
  within(first).getByRole("region", { name: "the offer" });

  act(() => {
    fireEvent.click(within(first).getByRole("button", { name: OFFER_DISMISS }));
  });
  expect(within(first).queryByRole("region", { name: "the offer" })).toBeNull();

  cleanup();

  const second = bootAndWin(storage, server);
  within(second).getByRole("region", { name: "the result" });
  expect(
    within(second).queryByRole("region", { name: "the offer" }),
  ).toBeNull();
});

it("offers it again to a browser that was shown it and answered nothing", () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);

  const first = bootAndWin(storage, server);
  within(first).getByRole("region", { name: "the offer" });

  cleanup();

  const second = bootAndWin(storage, server);
  within(second).getByRole("region", { name: "the offer" });
});

it("spends the offer on the way to the account screen too", () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);

  const first = bootAndWin(storage, server);
  act(() => {
    fireEvent.click(within(first).getByRole("link", { name: OFFER_ACCEPT }));
  });

  cleanup();
  window.location.hash = "";

  const second = bootAndWin(storage, server);
  within(second).getByRole("region", { name: "the result" });
  expect(
    within(second).queryByRole("region", { name: "the offer" }),
  ).toBeNull();
});

it("the account screen is reachable from the first screen", async () => {
  const storage = inMemoryStorage();
  const server = accountServer([ALICE]);
  const { container } = bootClient({
    storage,
    server,
    wiring,
    welcomeFrame: welcomeFrame(ALICE.deviceId),
  });

  act(() => {
    fireEvent.click(within(container).getByRole("button", { name: "Account" }));
  });

  await within(container).findByLabelText("account");
});
