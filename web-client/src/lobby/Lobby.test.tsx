import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { Lobby } from "./Lobby";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { bootDuelClient } from "../store/boot";
import { ProfileProvider } from "../profile/profile-provider";
import { SetNameProvider } from "../profile/set-name-provider";
import {
  AccountProvider,
  type AccountCalls,
} from "../account/account-provider";
import type { ProfileStripState } from "../profile/profile-strip";
import type { SeatView, ServerMessage } from "../protocol";
import type { SetNameOutcome } from "../profile/set-name";
import { aProfile, aDuelLine } from "../profile/profile-fixture";
import { PROTOCOL_VERSION } from "../protocol";
import { FakeSocket } from "../protocol/fake-socket";
import { openReconnectingConnection } from "../protocol/reconnecting";
import { aView } from "../table/view-fixture";
import { ATTACH_LABEL } from "../account/recovery-text";

// `read` (`useHistory()`) is wired by the real app boot in "../main", which
// this suite never runs — every other test here leaves it `null` and never
// notices, because none of them ever move `screen` off `"first"`. The tests
// below are the first to combine a chosen screen with a store fact, and that
// combination is exactly ADR-0076 §3's branch order, so `read` has to be a
// real function here for that branch to be reachable at all — otherwise the
// gate this file is closing would silently pass no matter which branch runs
// first. `useLadder` is untouched: no test here opens the leaderboard.
//
// It is a spy, not a plain stub, for a reason specific to this file: the
// effect this ticket adds settles the address back to "first" inside the
// same `act()` flush `render()` performs, so a branch-order regression that
// picks the record first and then self-corrects leaves an identical *final*
// DOM either way — the flash never survives to be queried. Whether the
// record's own fetch fired is the one signal that still tells the two apart.
const historyRead = vi.hoisted(() => vi.fn(() => new Promise<never>(() => {})));

// A boolean and a spy, deliberately, and not a real `Storage`: what storage
// does with the answer is `account-offer-settled.test.ts`'s, and what a whole
// browser does with it across two boots is `TASK-041508`'s. This file's
// subject is what `Lobby` asks the seam and what it does with the reply.
const offerWiring = vi.hoisted(() => ({
  signedIn: false,
  settled: false,
  settle: vi.fn(),
}));

vi.mock("../main", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../main")>();
  return {
    ...actual,
    useHistory: () => historyRead,
    useSignedIn: () => offerWiring.signedIn,
    offerSettledHere: () => offerWiring.settled,
    settleOfferHere: offerWiring.settle,
  };
});

const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;

function seatView(index: number): SeatView {
  return {
    index,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
  };
}

const SNAPSHOT: ServerMessage = {
  type: "Snapshot",
  view: {
    viewerSeat: 0,
    handNumber: 1,
    buttonSeat: 0,
    street: "PREFLOP",
    board: { cards: [] },
    pot: 30,
    betToMatch: 20,
    minRaiseTo: 40,
    seatToAct: 0,
    smallBlind: 10,
    bigBlind: 20,
    seats: [seatView(0), seatView(1)],
  },
};

function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

beforeEach(() => {
  window.location.hash = "";
  historyRead.mockClear();
  offerWiring.signedIn = false;
  offerWiring.settled = false;
  offerWiring.settle.mockClear();
});

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
  vi.useRealTimers();
});

function renderLobby(store: DuelStore = createDuelStore()): {
  send: ReturnType<typeof vi.fn>;
  forgetRoom: ReturnType<typeof vi.fn>;
} {
  const send = vi.fn();
  const forgetRoom = vi.fn();
  render(
    <DuelProvider store={store} send={send} forgetRoom={forgetRoom}>
      <Lobby />
    </DuelProvider>,
  );
  return { send, forgetRoom };
}

function renderLobbyWithProfile(
  state: ProfileStripState,
  store: DuelStore = createDuelStore(),
): void {
  const read = (): Promise<ProfileStripState> => Promise.resolve(state);
  const setName = vi.fn((): Promise<SetNameOutcome> =>
    Promise.resolve({ kind: "named", profile: aProfile() }),
  );
  render(
    <ProfileProvider read={read}>
      <SetNameProvider setName={setName}>
        <DuelProvider store={store} send={vi.fn()}>
          <Lobby />
        </DuelProvider>
      </SetNameProvider>
    </ProfileProvider>,
  );
}

/**
 * Renders `Lobby` with a duel this client sat seat 0 of, finished with the
 * given winner (`null` for a draw). Only the three account-offer tests below
 * call this: every other test in this file builds its own store.
 */
function renderFinishedDuel(winner: number | null): void {
  const store = createDuelStore();
  store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 0 });
  renderLobby(store);

  act(() => {
    store.apply({
      type: "DuelFinished",
      outcome: { winner, handsPlayed: 3, finalStacks: [1000, 0] },
    });
  });
}

function typeCode(value: string): void {
  fireEvent.change(screen.getByLabelText("Room code"), { target: { value } });
}

function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

function reconnectingClient(joinRoomCode: string | null = null) {
  const sockets: FakeSocket[] = [];
  const storage = inMemoryStorage();
  const client = bootDuelClient({
    connect: (onMessage) =>
      openReconnectingConnection({
        openSocket: () => {
          const socket = new FakeSocket();
          sockets.push(socket);
          return socket.asWebSocket();
        },
        storage,
        onMessage,
        jitter: () => 0,
      }),
    joinRoomCode,
    storage,
  });
  return { sockets, client, storage };
}

describe("the lobby", () => {
  it("asks the server for a room when the host clicks create", () => {
    const { send } = renderLobby();

    fireEvent.click(screen.getByRole("button", { name: "Create a duel room" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "CreateRoom" });
  });

  it("sends a pasted code trimmed and upper-cased", () => {
    const { send } = renderLobby();

    typeCode("  abcdefgh  ");
    fireEvent.click(screen.getByRole("button", { name: "Join the duel" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "JoinRoom", code: "ABCDEFGH" });
  });

  it("sends nothing when the code box holds only whitespace", () => {
    const { send } = renderLobby();

    typeCode("   ");
    fireEvent.click(screen.getByRole("button", { name: "Join the duel" }));

    expect(send).not.toHaveBeenCalled();
  });

  it("shows the room code the server named", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.getByText("ABCDEFGH")).toBeDefined();
    expect(
      screen.queryByRole("button", { name: "Create a duel room" }),
    ).toBeNull();
  });

  it("shows an invite link carrying that code", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });

  it("leaves the invite link selectable and focused for a copy by hand", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.readOnly).toBe(true);
    expect(document.activeElement).toBe(inviteLink);
  });

  it("copies the invite link when the browser has a clipboard", async () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    const writeText = vi.fn(() => Promise.resolve());
    withClipboard(writeText);
    renderLobby(store);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));

    expect(writeText).toHaveBeenCalledWith(
      "http://localhost:3000/?room=ABCDEFGH",
    );
    await screen.findByText("Link copied.");
  });

  it("offers no copy button when the browser has no clipboard", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.queryByRole("button", { name: "Copy the link" })).toBeNull();
    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });

  it("keeps the link in reach when the clipboard refuses", async () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    withClipboard(() => Promise.reject(new Error("denied")));
    renderLobby(store);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));

    await screen.findByText("Copy it from the box above.");
    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });

  it("says an unknown room is unknown", () => {
    const store = createDuelStore();
    store.apply({ type: "Failure", error: "UNKNOWN_ROOM" });
    renderLobby(store);

    expect(screen.getByText("No duel room has that code.")).toBeDefined();
  });

  it("says a full room is full", () => {
    const store = createDuelStore();
    store.apply({ type: "Failure", error: "ROOM_FULL" });
    renderLobby(store);

    expect(
      screen.getByText("That duel room already has a rival in it."),
    ).toBeDefined();
  });

  it("sends nothing after a refusal until a fresh click", () => {
    const store = createDuelStore();
    store.apply({ type: "Failure", error: "UNKNOWN_ROOM" });
    const { send } = renderLobby(store);

    expect(send).not.toHaveBeenCalled();

    typeCode("abcdefgh");
    fireEvent.click(screen.getByRole("button", { name: "Join the duel" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "JoinRoom", code: "ABCDEFGH" });
  });

  it("leaves the waiting panel when the first Snapshot arrives", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.getByText("Waiting for your rival")).toBeDefined();

    act(() => {
      store.apply(SNAPSHOT);
    });

    expect(
      screen.queryByRole("heading", { name: "Waiting for your rival" }),
    ).toBeNull();
    expect(screen.getByText("Pot 30")).toBeDefined();
  });

  it("puts the action bar under the table", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
    });

    expect(screen.getByRole("region", { name: "your move" })).toBeDefined();
    // Verify no-turn case reaches the bar: waiting line is shown when pendingTurn is null
    expect(screen.getByText("Waiting for your rival…")).toBeDefined();
  });

  it("sends the Act the bar built from the pending turn", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    const { send } = renderLobby(store);

    // First turn: handNumber 4, actionSequence 9, allowed: ["FOLD", "CALL"]
    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "YourTurn",
        handNumber: 4,
        actionSequence: 9,
        legalActions: {
          seat: 0,
          allowed: ["FOLD", "CALL"],
          callTo: 400,
          minBetTo: 0,
          minRaiseTo: 0,
          allInTo: 500,
        },
      });
    });
    fireEvent.click(screen.getByRole("button", { name: "Fold" }));

    expect(send).toHaveBeenCalledWith({
      type: "Act",
      handNumber: 4,
      actionSequence: 9,
      action: { type: "Fold", seat: 0 },
    });

    // Second turn with different identity and allowed actions: handNumber 61, actionSequence 103, allowed: ["CHECK", "CALL"]
    act(() => {
      store.apply({
        type: "YourTurn",
        handNumber: 61,
        actionSequence: 103,
        legalActions: {
          seat: 0,
          allowed: ["CHECK", "CALL"],
          callTo: 0,
          minBetTo: 0,
          minRaiseTo: 0,
          allInTo: 500,
        },
      });
    });
    // Clicking Check proves the bar follows the store's second turn allowed actions,
    // not hardcoded values: first turn had ["FOLD", "CALL"], this has ["CHECK", "CALL"]
    fireEvent.click(screen.getByRole("button", { name: "Check" }));

    expect(send).toHaveBeenLastCalledWith({
      type: "Act",
      handNumber: 61,
      actionSequence: 103,
      action: { type: "Check", seat: 0 },
    });
  });

  it("keeps waiting through every frame that neither seats a table nor ends the duel", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.getByText("Waiting for your rival")).toBeDefined();

    // Every variant of ServerMessage except Snapshot, not a representative two.
    // The claim is universal and the cost of getting it wrong is exact: a
    // reducer that leaks a `view` on DuelFinished shipped `189 passed` while the
    // identical leak on Events failed — same bug, caught only by luck of which
    // frame the test happened to name. DuelFinished and Failure are covered
    // separately by the two tests below.
    const NOT_A_SNAPSHOT: readonly ServerMessage[] = [
      { type: "Welcome", playerId: "p-1", deviceId: "d-1", protocolVersion: 2 },
      { type: "RoomJoined", code: "ABCDEFGH", seat: 0 },
      { type: "Events", events: [{ type: "ActionOn", sequence: 1, seat: 0 }] },
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions: {
          seat: 0,
          allowed: ["CHECK"],
          callTo: 0,
          minBetTo: 0,
          minRaiseTo: 0,
          allInTo: 1000,
        },
      },
      {
        type: "Rejected",
        rejection: {
          type: "ActionNotAllowed",
          attempted: "BET",
          allowed: ["CHECK"],
        },
      },
      { type: "Failure", error: "NOT_YOUR_TURN" },
      { type: "RematchOffered", seat: 1 },
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
      {
        type: "ActedForAbsent",
        seat: 1,
        handNumber: 3,
        actionSequence: 7,
        action: "CHECK",
      },
    ];

    for (const frame of NOT_A_SNAPSHOT) {
      act(() => {
        store.apply(frame);
      });
      expect(screen.getByText("Waiting for your rival")).toBeDefined();
    }
  });

  it("shows the presence beside the table it is about", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      });
    });

    expect(
      screen.getByText("Your rival is away. The duel is paused."),
    ).toBeDefined();
    expect(screen.getByText("47")).toBeDefined();
    expect(screen.getByText("Away")).toBeDefined();
  });

  it("explains a paused action with the presence it already holds", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      });
      store.apply({ type: "Failure", error: "DUEL_PAUSED" });
    });

    expect(
      screen.getByText("Your rival is away. The duel is paused."),
    ).toBeDefined();
    expect(
      screen.getByText("The duel is paused. That action was not applied."),
    ).toBeDefined();
  });

  it("starts a second window fresh, though it carries the same remaining", () => {
    vi.useFakeTimers();
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      });
    });

    expect(screen.getByText("47")).toBeDefined();

    // Advance time by 20 seconds
    act(() => {
      vi.advanceTimersByTime(20000);
    });

    expect(screen.getByText("27")).toBeDefined();

    // Apply the same OpponentPresence frame again
    act(() => {
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      });
    });

    expect(screen.getByText("47")).toBeDefined();
  });

  it("the countdown reaching zero sends nothing and changes nothing", () => {
    vi.useFakeTimers();
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    const { send } = renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "YourTurn",
        handNumber: 4,
        actionSequence: 9,
        legalActions: {
          seat: 1,
          allowed: ["CHECK", "BET"],
          callTo: 0,
          minBetTo: 20,
          minRaiseTo: 0,
          allInTo: 500,
        },
      });
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      });
    });

    // Positive control: the countdown is live at 47 before anything is recorded. A
    // screen that never rendered a real number would pass every clause below for
    // the wrong reason.
    expect(screen.getByText("47")).toBeDefined();

    const barButtons = () =>
      within(screen.getByRole("region", { name: "your move" }))
        .getAllByRole<HTMLButtonElement>("button")
        .map((button) => ({
          name: button.textContent?.trim() ?? "",
          disabled: button.disabled,
        }));
    const before = barButtons();
    expect(before.length).toBeGreaterThan(0);

    act(() => {
      vi.advanceTimersByTime(20_000);
    });

    // Second positive control: still ticking partway through the window, so the
    // final zero below is a crossing, not a value that was never moving.
    expect(screen.getByText("27")).toBeDefined();

    // The remaining 100 000 ms brings the total advance to 120 000 — more than
    // twice the 47 000 ms window — well past the point the deadline was crossed.
    act(() => {
      vi.advanceTimersByTime(100_000);
    });

    // ADR-0028 §3, one assertion per clause.
    // Sends nothing.
    expect(send).toHaveBeenCalledTimes(0);
    // Enables nothing: identical buttons, identical order, identical disabled flags.
    expect(barButtons()).toEqual(before);
    // Enters no state: the pause is still the pause.
    expect(
      screen.getByText("Your rival is away. The duel is paused."),
    ).toBeDefined();
    // Assumes no resumption: the plate still reads Away, never Timed out.
    expect(screen.getByText("Away")).toBeDefined();
    expect(screen.queryByText("Timed out")).toBeNull();
    // The number stops: clamped at zero, scoped to the notice that carries it.
    const notice = screen.getByText("Your rival is away. The duel is paused.");
    expect(within(notice).getByText("0")).toBeDefined();
  });

  it("a window with nothing left of it renders as waiting", () => {
    vi.useFakeTimers();
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    const { send } = renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 0,
      });
    });

    const notice = screen.getByText("Your rival is away. The duel is paused.");
    expect(within(notice).getByText("0")).toBeDefined();
    expect(screen.queryByText("Timed out")).toBeNull();
    expect(screen.queryByText("Your rival did not come back.")).toBeNull();
    expect(send).toHaveBeenCalledTimes(0);
  });

  it("shows the result when the duel finishes", () => {
    // Victory case: viewer at seat 1, winner is seat 1
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.getByText("Victory")).toBeDefined();
    expect(
      screen.queryByRole("heading", { name: "Waiting for your rival" }),
    ).toBeNull();

    cleanup();

    // Defeat case: viewer at seat 1, winner is seat 0
    const store2 = createDuelStore();
    store2.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store2);

    act(() => {
      store2.apply({
        type: "DuelFinished",
        outcome: { winner: 0, handsPlayed: 5, finalStacks: [2000, 500] },
      });
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.getByText("Defeat")).toBeDefined();
    expect(
      screen.queryByRole("heading", { name: "Waiting for your rival" }),
    ).toBeNull();
  });

  it("puts the result over the table it replaces", () => {
    // Victory case: viewer at seat 0, winner is seat 0
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
      });
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByRole("region", { name: "your move" })).toBeNull();
    expect(screen.queryByText("Pot 30")).toBeNull();

    cleanup();

    // Defeat case: viewer at seat 0, winner is seat 1
    const store2 = createDuelStore();
    store2.apply(ROOM_JOINED); // seat 0
    renderLobby(store2);

    act(() => {
      store2.apply(SNAPSHOT);
      store2.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 5, finalStacks: [500, 2000] },
      });
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByRole("region", { name: "your move" })).toBeNull();
    expect(screen.queryByText("Pot 30")).toBeNull();
  });

  it("shows the profile strip under the way into a duel", async () => {
    const state: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ coinBalance: 3 }),
      duels: [],
    };
    renderLobbyWithProfile(state);

    await screen.findByLabelText("your profile");
    expect(screen.getByText("3 Duel coins")).toBeDefined();
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();
  });

  it("keeps the strip off the screen once a table is on it", () => {
    const state: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    store.apply(SNAPSHOT);
    renderLobbyWithProfile(state, store);

    expect(screen.queryByLabelText("your profile")).toBeNull();
    expect(screen.getByText("Pot 30")).toBeDefined();
  });

  it("shows the name surface beside the strip, and only with a profile to show", async () => {
    const state: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ displayName: null }),
      duels: [],
    };
    renderLobbyWithProfile(state);

    const profileStrip = await screen.findByLabelText("your profile");
    const nameSurface = screen.getByLabelText("your display name");
    expect(profileStrip).toBeDefined();
    expect(nameSurface).toBeDefined();
    // Verify document order: name surface comes after profile strip
    expect(profileStrip.compareDocumentPosition(nameSurface)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );

    cleanup();

    const noProfileState: ProfileStripState = { kind: "no-profile" };
    renderLobbyWithProfile(noProfileState);

    expect(screen.queryByLabelText("your display name")).toBeNull();
  });

  it("keeps the name surface off the screen once a table is on it", () => {
    const state: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    store.apply(SNAPSHOT);
    renderLobbyWithProfile(state, store);

    expect(screen.queryByLabelText("your display name")).toBeNull();
    expect(screen.getByText("Pot 30")).toBeDefined();
  });

  it("renders the lobby with no headings from the name surface", async () => {
    const state: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ displayName: "TestPlayer" }),
      duels: [aDuelLine({ opponentDisplayName: "Opponent" })],
    };
    renderLobbyWithProfile(state);

    await screen.findByLabelText("your profile");
    // TestPlayer now appears in both ProfileStrip and NameSurface
    expect(await screen.findAllByText("TestPlayer")).toHaveLength(2);
    // The lobby mounts with SetNameProvider, so NameSurface will render.
    // Verify NameSurface does not add any heading elements
    const headings = screen.queryAllByRole("heading");
    expect(headings.length).toBe(0);
  });

  it("sends one OfferRematch when the rematch is pressed", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    const { send } = renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    fireEvent.click(screen.getByRole("button", { name: "Rematch" }));

    expect(send).toHaveBeenCalledTimes(1);
    expect(send).toHaveBeenCalledWith({ type: "OfferRematch" });
  });

  it("shows your own offer only once the server states it", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    // Immediately after click, the Rematch button is still on screen
    fireEvent.click(screen.getByRole("button", { name: "Rematch" }));
    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
    expect(
      screen.queryByText("Rematch offered — waiting for your rival"),
    ).toBeNull();

    // Then the server sends RematchOffered
    act(() => {
      store.apply({ type: "RematchOffered", seat: 1 });
    });

    // The chip appears and the button is gone
    expect(
      screen.getByText("Rematch offered — waiting for your rival"),
    ).toBeDefined();
    expect(screen.queryByRole("button", { name: "Rematch" })).toBeNull();
  });

  it("the snapshot after a finish returns the table with the button on the other side", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
    });

    // First duel: button at seat 0, inside "You" plate
    const firstButton = screen.getByLabelText("the button");
    const firstPlate = firstButton.closest("div");
    expect(firstPlate?.textContent).toContain("You");

    // Duel finishes, both offer rematch
    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
      });
      store.apply({ type: "RematchOffered", seat: 1 });
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();

    // Second duel: button at seat 1, inside "Your rival" plate
    act(() => {
      store.apply({
        type: "Snapshot",
        view: {
          viewerSeat: 0,
          handNumber: 2,
          buttonSeat: 1,
          street: "PREFLOP",
          board: { cards: [] },
          pot: 30,
          betToMatch: 20,
          minRaiseTo: 40,
          seatToAct: 0,
          smallBlind: 10,
          bigBlind: 20,
          seats: [seatView(0), seatView(1)],
        },
      });
    });

    expect(screen.queryByRole("region", { name: "the result" })).toBeNull();
    expect(screen.getByText("Pot 30")).toBeDefined();

    const secondButton = screen.getByLabelText("the button");
    const secondPlate = secondButton.closest("div");
    expect(secondPlate?.textContent).toContain("Your rival");

    expect(screen.queryByText(/rematch/i)).toBeNull();
  });

  it("takes a rematch offer restated after the rejoins DuelFinished", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    store.apply({
      type: "DuelFinished",
      outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
    });
    store.apply({ type: "RematchOffered", seat: 0 });
    renderLobby(store);

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.getByText("Your rival offers a rematch")).toBeDefined();
    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
  });

  it("takes no offer that arrived before the DuelFinished", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    store.apply({ type: "RematchOffered", seat: 0 });
    store.apply({
      type: "DuelFinished",
      outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
    });
    renderLobby(store);

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByText("Your rival offers a rematch")).toBeNull();
    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
  });

  it("says the room is gone, and keeps the way back", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    fireEvent.click(screen.getByRole("button", { name: "Rematch" }));

    act(() => {
      store.apply({ type: "Failure", error: "UNKNOWN_ROOM" });
    });

    expect(screen.getByText("That duel room is gone.")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Rematch" })).toBeNull();
    const link = screen.getByRole("link", { name: "Back to the lobby" });
    expect(link.getAttribute("href")).toBe("/");
  });

  it("leaves the rematch live when the room cannot take one yet", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    const { send } = renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    fireEvent.click(screen.getByRole("button", { name: "Rematch" }));

    act(() => {
      store.apply({ type: "Failure", error: "REMATCH_UNAVAILABLE" });
    });

    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
    expect(screen.queryByText("That duel room is gone.")).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Rematch" }));

    expect(send).toHaveBeenCalledTimes(2);
    expect(send).toHaveBeenLastCalledWith({ type: "OfferRematch" });
  });

  it("offers the way back to the lobby while the room is still waiting", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const link = screen.getByRole("link", { name: "Back to the lobby" });
    expect(link).toBeDefined();
    expect(link.getAttribute("href")).toBe("/");
    expect(link.className.split(" ")).toContain("border-hairline");
    expect(screen.getByText("Waiting for your rival")).toBeDefined();
  });

  it("forgets the room and sends nothing when the host leaves the waiting screen", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    const { send, forgetRoom } = renderLobby(store);

    const link = screen.getByRole("link", { name: "Back to the lobby" });
    const clickReturn = fireEvent.click(link);

    expect(forgetRoom).toHaveBeenCalledOnce();
    expect(clickReturn).toBe(true);
    expect(send).not.toHaveBeenCalled();
  });

  it("forgets the room when the player takes the way back", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    const { send, forgetRoom } = renderLobby(store);

    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: 1, handsPlayed: 3, finalStacks: [0, 1000] },
      });
    });

    fireEvent.click(screen.getByRole("link", { name: "Back to the lobby" }));

    expect(forgetRoom).toHaveBeenCalledOnce();
    expect(send).not.toHaveBeenCalled();
  });

  it("says the room stays open and the link still works", () => {
    const text =
      "The room stays open. That link still works for your rival, and it brings you back.";
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.getByText(text)).toBeDefined();
    expect(
      screen.getByRole("link", { name: "Back to the lobby" }),
    ).toBeDefined();
  });

  it("keeps that line off the screen where there is no room", () => {
    const text =
      "The room stays open. That link still works for your rival, and it brings you back.";
    renderLobby();

    expect(screen.queryByText(text)).toBeNull();
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();
  });

  it("adds exactly two strings to the waiting screen and no third", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const waiting = screen
      .getByText("Waiting for your rival")
      .closest("section");
    expect(waiting).not.toBeNull();
    const normalizedText = waiting?.textContent?.trim().replace(/\s+/g, " ");

    expect(normalizedText).toBe(
      "Waiting for your rivalABCDEFGHInvite linkBack to the lobbyThe room stays open. That link still works for your rival, and it brings you back.",
    );
  });

  it("puts no confirmation between the press and the lobby", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    const { forgetRoom } = renderLobby(store);

    const confirmSpy = vi.spyOn(window, "confirm");
    const alertSpy = vi.spyOn(window, "alert");

    const back = screen.getByRole("link", { name: "Back to the lobby" });
    const clickReturn = fireEvent.click(back);

    expect(forgetRoom).toHaveBeenCalledOnce();
    expect(clickReturn).toBe(true);
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(
      screen.queryByRole("button", { name: /sure|confirm|really|yes/i }),
    ).toBeNull();
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(alertSpy).not.toHaveBeenCalled();

    confirmSpy.mockRestore();
    alertSpy.mockRestore();
  });

  it("offers none of the words ADR-0073 refuses", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const waiting = screen
      .getByText("Waiting for your rival")
      .closest("section");
    expect(waiting).toBeDefined();
    expect(
      within(waiting!).getByRole("link", { name: "Back to the lobby" }),
    ).toBeDefined();

    const refusedWords = [
      "Cancel",
      "Cancel the room",
      "Cancel the duel",
      "Close the room",
      "Delete the room",
      "End the room",
      "Leave",
      "Leave the room",
      "Leave the duel",
      "Give up",
      "Abandon",
      "Withdraw",
      "Forfeit",
      "Back",
      "Cash out",
      "Exit table",
      "Stand up",
      "Sit out",
    ] as const;

    for (const word of refusedWords) {
      expect(within(waiting!).queryByText(word)).toBeNull();
      expect(within(waiting!).queryByRole("button", { name: word })).toBeNull();
      expect(within(waiting!).queryByRole("link", { name: word })).toBeNull();
    }
  });

  it("prints no duration, countdown or expiry", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const waiting = screen
      .getByText("Waiting for your rival")
      .closest("section");
    const text = waiting?.textContent ?? "";

    expect(text).toContain("Back to the lobby");
    expect(text).toContain(
      "The room stays open. That link still works for your rival, and it brings you back.",
    );
    expect(text).not.toMatch(
      /\b(second|seconds|minute|minutes|hour|hours|day|days|expire|expires|expired|expiry|countdown|remaining|timer|timeout|until)\b/i,
    );
    expect(text).not.toMatch(/\d{1,2}:\d{2}/);
  });

  it("renders the pause a resume came back to", () => {
    vi.useFakeTimers();
    const { sockets, client } = reconnectingClient("ABCDEFGH");
    render(
      <DuelProvider
        store={client.store}
        send={client.send}
        forgetRoom={client.forgetRoom}
      >
        <Lobby />
      </DuelProvider>,
    );

    const WELCOME = JSON.stringify({
      type: "Welcome",
      deviceId: "d-1",
      protocolVersion: PROTOCOL_VERSION,
    });

    act(() => {
      sockets[0].open();
      sockets[0].receive(WELCOME);
      sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
      sockets[0].receive(
        JSON.stringify({
          type: "Snapshot",
          view: aView({ viewerSeat: 1 }),
        }),
      );
      sockets[0].close();
    });

    act(() => {
      vi.advanceTimersByTime(250);
      sockets[1].open();
      sockets[1].receive(WELCOME);
      sockets[1].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
      sockets[1].receive(
        JSON.stringify({
          type: "Snapshot",
          view: aView({ viewerSeat: 1 }),
        }),
      );
      sockets[1].receive(
        JSON.stringify({
          type: "OpponentPresence",
          presence: "AWAY",
          graceRemainingMillis: 47000,
        }),
      );
    });

    expect(
      screen.getByText("Your rival is away. The duel is paused."),
    ).toBeDefined();
    expect(screen.getByText("47")).toBeDefined();
    expect(screen.getByText("Away")).toBeDefined();
  });

  it("says nothing to a resumed client whose rival never left", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    act(() => {
      store.apply(SNAPSHOT);
      store.apply({
        type: "OpponentPresence",
        presence: "PRESENT",
        graceRemainingMillis: null,
      });
    });

    expect(screen.queryByText("Your rival is back.")).toBeNull();
    expect(screen.queryByText("Away")).toBeNull();
    expect(screen.queryByText("Timed out")).toBeNull();
  });

  it("names the server as the actor, for a check as well as a fold", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    const SNAPSHOT_SEAT_1: ServerMessage = {
      type: "Snapshot",
      view: {
        viewerSeat: 1,
        handNumber: 1,
        buttonSeat: 0,
        street: "PREFLOP",
        board: { cards: [] },
        pot: 30,
        betToMatch: 20,
        minRaiseTo: 40,
        seatToAct: 0,
        smallBlind: 10,
        bigBlind: 20,
        seats: [seatView(0), seatView(1)],
      },
    };

    act(() => {
      store.apply(SNAPSHOT_SEAT_1);
      store.apply({
        type: "ActedForAbsent",
        seat: 0,
        handNumber: 3,
        actionSequence: 7,
        action: "FOLD",
      });
    });

    expect(screen.getByText("The server folded for your rival.")).toBeDefined();
    expect(screen.queryByText("Your rival folded")).toBeNull();

    act(() => {
      store.apply({
        type: "ActedForAbsent",
        seat: 1,
        handNumber: 3,
        actionSequence: 9,
        action: "CHECK",
      });
    });

    expect(screen.getByText("The server checked for you.")).toBeDefined();
    expect(screen.queryByText("The server folded for your rival.")).toBeNull();
  });

  it("shows the most recent mark, whichever order the frames arrived in", () => {
    const SNAPSHOT_SEAT_1: ServerMessage = {
      type: "Snapshot",
      view: {
        viewerSeat: 1,
        handNumber: 1,
        buttonSeat: 0,
        street: "PREFLOP",
        board: { cards: [] },
        pot: 30,
        betToMatch: 20,
        minRaiseTo: 40,
        seatToAct: 0,
        smallBlind: 10,
        bigBlind: 20,
        seats: [seatView(0), seatView(1)],
      },
    };

    const ACTED_FOLD: ServerMessage = {
      type: "ActedForAbsent",
      seat: 0,
      handNumber: 3,
      actionSequence: 7,
      action: "FOLD",
    };

    const EVENTS_FRAME: ServerMessage = {
      type: "Events",
      events: [{ type: "ActionOn", sequence: 7, seat: 0 }],
    };

    // First scenario: ActedForAbsent, then Events
    const store1 = createDuelStore();
    store1.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store1);

    act(() => {
      store1.apply(SNAPSHOT_SEAT_1);
      store1.apply(ACTED_FOLD);
      store1.apply(EVENTS_FRAME);
    });

    expect(screen.getByText("The server folded for your rival.")).toBeDefined();

    cleanup();

    // Second scenario: Events, then ActedForAbsent
    const store2 = createDuelStore();
    store2.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store2);

    act(() => {
      store2.apply(SNAPSHOT_SEAT_1);
      store2.apply(EVENTS_FRAME);
      store2.apply(ACTED_FOLD);
    });

    expect(screen.getByText("The server folded for your rival.")).toBeDefined();
  });

  it("a rival who is back leaves no sentence about the server behind", () => {
    const store = createDuelStore();
    store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 1 });
    renderLobby(store);

    const SNAPSHOT_SEAT_1: ServerMessage = {
      type: "Snapshot",
      view: {
        viewerSeat: 1,
        handNumber: 1,
        buttonSeat: 0,
        street: "PREFLOP",
        board: { cards: [] },
        pot: 30,
        betToMatch: 20,
        minRaiseTo: 40,
        seatToAct: 0,
        smallBlind: 10,
        bigBlind: 20,
        seats: [seatView(0), seatView(1)],
      },
    };

    act(() => {
      store.apply(SNAPSHOT_SEAT_1);
      store.apply({
        type: "OpponentPresence",
        presence: "ABSENT",
        graceRemainingMillis: 60000,
      });
      store.apply({
        type: "ActedForAbsent",
        seat: 0,
        handNumber: 3,
        actionSequence: 7,
        action: "FOLD",
      });
    });

    expect(screen.getByText("The server folded for your rival.")).toBeDefined();

    act(() => {
      store.apply({
        type: "OpponentPresence",
        presence: "PRESENT",
        graceRemainingMillis: null,
      });
    });

    expect(screen.getByText("Your rival is back.")).toBeDefined();
    expect(screen.queryByText("The server folded for your rival.")).toBeNull();
  });

  it("shows the duel to a player a frame seats, whatever address they were reading", () => {
    window.location.hash = "#/duels";
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    store.apply(SNAPSHOT);

    renderLobby(store);

    // ADR-0076 §3: the store outranks the address. The duel table wins even
    // though the address was still naming the record when the frame seated
    // this player — asserted from the address side, not from a click.
    expect(screen.getByText("Pot 30")).toBeDefined();
    // The record's own "Back" affordance (rendered only inside the
    // `screen === "duels"` branch) is the sharpest negative available here:
    // present would mean the address, not the store, won the branch.
    expect(screen.queryByRole("button", { name: "Back" })).toBeNull();
    // The branch order is what this test guards, and the settled DOM above
    // cannot tell a correct first pass from a wrong pass that this same
    // ticket's own effect immediately corrects: both end on `DuelTable`.
    // The record's own fetch firing is the one witness a wrong first branch
    // leaves behind even after the correction lands.
    expect(historyRead).not.toHaveBeenCalled();
  });

  it("replaces the address a frame overruled, and stacks no entry doing it", () => {
    window.location.hash = "#/duels";
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    store.apply(SNAPSHOT);
    const lengthBeforeRender = window.history.length;

    renderLobby(store);

    // Two assertions, deliberately: a `push` would satisfy the first and
    // break Back forever, which is exactly why there are two tests here and
    // not one (ADR-0076 §3, and this ticket's own Proof).
    expect(window.location.hash).toBe("");
    expect(window.history.length).toBe(lengthBeforeRender);
  });

  it("leaves the address alone while no frame has seated anybody", () => {
    window.location.hash = "#/duels";

    renderLobby();

    // No outcome, no view, no roomCode: nobody has been seated, so the
    // effect must not touch the address that is legitimately naming the
    // record right now.
    expect(window.location.hash).toBe("#/duels");
    expect(screen.getByRole("button", { name: "Back" })).toBeDefined();
    expect(
      screen.queryByRole("button", { name: "Create a duel room" }),
    ).toBeNull();
  });

  it("offers an account after a win, and after nothing else", () => {
    // Three renders over the same seat, not one: a single case cannot tell a
    // decision from a constant (STORY-0415). The result panel is asserted
    // present in all three, so the offer's absence in the last two is a
    // withheld offer and not an empty screen.
    const cases: ReadonlyArray<{ winner: number | null; offered: boolean }> = [
      { winner: 0, offered: true },
      { winner: 1, offered: false },
      { winner: null, offered: false },
    ];

    for (const { winner, offered } of cases) {
      renderFinishedDuel(winner);

      expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
      if (offered) {
        expect(screen.getByRole("region", { name: "the offer" })).toBeDefined();
      } else {
        expect(screen.queryByRole("region", { name: "the offer" })).toBeNull();
      }

      cleanup();
    }

    // ADR-0085 §2: "not the prompt merely having been rendered". Being shown
    // the offer three times above must not have spent it.
    expect(offerWiring.settle).not.toHaveBeenCalled();
  });

  it("withholds the offer from a browser that answered, and from one holding a credential", () => {
    // Each render is a one-field delta from the offered case above (winner:
    // 0, settled: false, signedIn: false): without this test both terms
    // could be hard-coded and the test above would still pass.
    offerWiring.settled = true;
    renderFinishedDuel(0);

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByRole("region", { name: "the offer" })).toBeNull();

    cleanup();
    offerWiring.settled = false;
    offerWiring.signedIn = true;
    renderFinishedDuel(0);

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByRole("region", { name: "the offer" })).toBeNull();
  });

  it("answers from either control, and only Not now takes the offer off the screen", () => {
    renderFinishedDuel(0);

    fireEvent.click(
      screen.getByRole("link", { name: "Keep them with a password" }),
    );

    // Taking the offer settles it and stops there: the anchor's own page
    // load is what replaces this tree (ADR-0086 §6), so the offer and the
    // rest of the panel are still on screen right after the click.
    expect(offerWiring.settle).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("region", { name: "the offer" })).toBeDefined();
    expect(window.location.hash).toBe("");

    fireEvent.click(screen.getByRole("button", { name: "Not now" }));

    // Dismissing settles it a second time and, unlike accepting, also hides
    // it: nothing else will, since there is no page load coming.
    expect(offerWiring.settle).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("region", { name: "the offer" })).toBeNull();
    expect(screen.getByText("Victory")).toBeDefined();
  });

  it("puts the attach form on the account screen, wired to the account seam", async () => {
    const attachRecoveryEmail = vi.fn(
      async () => ({ kind: "accepted" }) as const,
    );
    const accountCalls: AccountCalls = {
      signUp: vi.fn(),
      signIn: vi.fn(),
      signOut: vi.fn(),
      revokeThisDevice: vi.fn(),
      attachRecoveryEmail,
      forgotPassword: vi.fn(),
      verifyEmail: vi.fn(),
      resetPassword: vi.fn(),
    };

    window.location.hash = "#/account";

    const profileState: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };

    const read = (): Promise<ProfileStripState> =>
      Promise.resolve(profileState);

    render(
      <ProfileProvider read={read}>
        <SetNameProvider setName={vi.fn()}>
          <AccountProvider calls={accountCalls}>
            <DuelProvider
              store={createDuelStore()}
              send={vi.fn()}
              forgetRoom={vi.fn()}
            >
              <Lobby />
            </DuelProvider>
          </AccountProvider>
        </SetNameProvider>
      </ProfileProvider>,
    );

    // Wait for the profile to load by looking for recovery status text
    await screen.findByText(/Recovery is/);

    fireEvent.change(screen.getByLabelText("Email address"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Current password"), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));

    await screen.findByText(
      "If that address can take mail, a link is on its way. Recovery stays off until you follow it.",
    );

    expect(attachRecoveryEmail).toHaveBeenCalledOnce();
    expect(attachRecoveryEmail).toHaveBeenCalledWith(
      "test@example.com",
      "password123",
    );
  });

  it("offers no attach form where no account provider sits above", async () => {
    const read = (): Promise<ProfileStripState> =>
      Promise.resolve({ kind: "no-profile" });
    const setName = vi.fn((): Promise<SetNameOutcome> =>
      Promise.resolve({ kind: "named", profile: aProfile() }),
    );

    window.location.hash = "#/account";

    render(
      <ProfileProvider read={read}>
        <SetNameProvider setName={setName}>
          <DuelProvider
            store={createDuelStore()}
            send={vi.fn()}
            forgetRoom={vi.fn()}
          >
            <Lobby />
          </DuelProvider>
        </SetNameProvider>
      </ProfileProvider>,
    );

    await screen.findByRole("heading", { name: "Account" });
    expect(screen.queryByRole("button", { name: ATTACH_LABEL })).toBeNull();
  });
});
