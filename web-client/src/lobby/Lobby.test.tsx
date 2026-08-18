import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import { describe, it, expect, vi, afterEach } from "vitest";
import { Lobby } from "./Lobby";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { ProfileProvider } from "../profile/profile-provider";
import { SetNameProvider } from "../profile/set-name-provider";
import type { ProfileStripState } from "../profile/profile-strip";
import type { SeatView, ServerMessage } from "../protocol";
import type { SetNameOutcome } from "../profile/set-name";
import { aProfile } from "../profile/profile-fixture";

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

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
});

function renderLobby(store: DuelStore = createDuelStore()): {
  send: ReturnType<typeof vi.fn>;
} {
  const send = vi.fn();
  render(
    <DuelProvider store={store} send={send}>
      <Lobby />
    </DuelProvider>,
  );
  return { send };
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

function typeCode(value: string): void {
  fireEvent.change(screen.getByLabelText("Room code"), { target: { value } });
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
      { type: "Welcome", deviceId: "d-1", protocolVersion: 2 },
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
    ];

    for (const frame of NOT_A_SNAPSHOT) {
      act(() => {
        store.apply(frame);
      });
      expect(screen.getByText("Waiting for your rival")).toBeDefined();
    }
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
      duels: [],
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
});
