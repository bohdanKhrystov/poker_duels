import { act, cleanup, fireEvent, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { PROTOCOL_VERSION } from "../protocol";
import type { ServerMessage } from "../protocol";
import { readDeviceId } from "../protocol/device-id";
import { accountServer, type ServerPlayer } from "./account-server";
import { bootClient, type ArcWiring } from "./drive-arc";
import { inMemoryStorage } from "./drive-duel";

// `Lobby.tsx:6` reads `useHistory` and `useSignedIn` from `../main`'s module
// scope, not from a prop, so there is no seam to pass them through
// `bootClient`. `wiring` is the plain object `bootClient` writes into on
// every boot (see `ArcWiring`'s doc comment in `drive-arc.tsx`); the mock
// below reads it back on every render. A wholesale `vi.mock("../main", …)`
// (`App.test.tsx:41`'s shape) would replace the module for every test in
// this file and export none of its real bindings, so `Lobby` could never
// reach `../main`'s actual `AccountProvider`-adjacent wiring — this partial
// mock, `importOriginal` plus a two-field override, is the merged shape
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
