import { describe, it, expect, vi } from "vitest";
import type { PlayerView, SeatView } from "../protocol";
import { createDuelStore, type Schedule } from "./duel-store";
import { initialState } from "./duel-state";

const WELCOME = {
  type: "Welcome",
  playerId: "p-1",
  deviceId: "d-1",
  protocolVersion: 2,
} as const;
const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;

function sampleSeat(overrides: Partial<SeatView> = {}): SeatView {
  return {
    index: 0,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
    ...overrides,
  };
}

function samplePlayerView(overrides: Partial<PlayerView> = {}): PlayerView {
  return {
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
    seats: [sampleSeat({ index: 0 }), sampleSeat({ index: 1 })],
    ...overrides,
  };
}

/**
 * A `Schedule` double that never fires on its own: `schedule` only records the callback, and
 * `fireNext` runs it. Lets a test hold a store mid-reveal and release one step at a time, the
 * way `duel-store.ts` never does on its own at a non-zero step.
 */
function manualSchedule(): {
  readonly schedule: Schedule;
  readonly fireNext: () => void;
} {
  let due: (() => void) | null = null;
  return {
    schedule: (run) => {
      due = run;
    },
    fireNext: () => {
      const run = due;
      due = null;
      if (run === null) {
        throw new Error("manualSchedule: nothing was scheduled");
      }
      run();
    },
  };
}

describe("the duel store", () => {
  it("starts at the reducer's initial state", () => {
    const store = createDuelStore();
    expect(store.getState()).toEqual(initialState());
  });

  it("folds an applied message into the state", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    expect(store.getState().roomCode).toBe("ABCDEFGH");
    expect(store.getState().mySeat).toBe(0);
  });

  it("notifies a subscriber when the state changed", () => {
    const store = createDuelStore();
    const listener = vi.fn();
    store.subscribe(listener);
    store.apply(ROOM_JOINED);
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it("notifies nobody when the reducer had no opinion", () => {
    const store = createDuelStore();
    const listener = vi.fn();
    store.subscribe(listener);
    store.apply(WELCOME);
    expect(listener).not.toHaveBeenCalled();
  });

  it("hands out the same state reference until a message changes it", () => {
    const store = createDuelStore();
    const state1 = store.getState();
    store.apply(WELCOME);
    const state2 = store.getState();
    expect(state2).toBe(state1);
    store.apply(ROOM_JOINED);
    const state3 = store.getState();
    expect(state3).not.toBe(state1);
  });

  it("stops notifying once the unsubscriber has run", () => {
    const store = createDuelStore();
    const listener = vi.fn();
    const unsubscribe = store.subscribe(listener);
    unsubscribe();
    store.apply(ROOM_JOINED);
    expect(listener).not.toHaveBeenCalled();
  });

  it("a step of zero releases in the same turn and schedules nothing", () => {
    const schedule = vi.fn();
    const store = createDuelStore({ stepMillis: 0, schedule });

    store.apply({
      type: "Events",
      events: [
        { type: "StreetDealt", sequence: 1, street: "RIVER", cards: ["3s"] },
      ],
    });
    store.apply({
      type: "Snapshot",
      view: samplePlayerView({
        street: "COMPLETE",
        board: { cards: ["As", "7d", "2c", "Kh", "3s"] },
      }),
    });

    // ADR-0102 §4: fully applied when apply() returns — the property drive-duel.tsx leans on.
    expect(store.getState().reveal).toBeNull();
    expect(store.getState().view?.board.cards).toEqual([
      "As",
      "7d",
      "2c",
      "Kh",
      "3s",
    ]);
    expect(schedule).not.toHaveBeenCalled();
  });

  it("ordinary play never calls the injected schedule", () => {
    const schedule = vi.fn();
    // A non-zero step: only a hand's ending is ever paced (ADR-0102 §1), so this must stay
    // untouched even though a clock is available to reach for.
    const store = createDuelStore({ stepMillis: 250, schedule });

    store.apply({
      type: "Events",
      events: [
        {
          type: "StreetDealt",
          sequence: 1,
          street: "FLOP",
          cards: ["As", "7d", "2c"],
        },
      ],
    });
    store.apply({
      type: "Snapshot",
      view: samplePlayerView({
        street: "FLOP",
        board: { cards: ["As", "7d", "2c"] },
      }),
    });

    expect(store.getState().reveal).toBeNull();
    expect(store.getState().view?.street).toBe("FLOP");
    expect(schedule).not.toHaveBeenCalled();
  });

  it("frames that arrive during the steps are applied in arrival order once the last step has stood", () => {
    const { schedule, fireNext } = manualSchedule();
    const store = createDuelStore({ stepMillis: 250, schedule });

    store.apply({
      type: "Events",
      events: [
        { type: "StreetDealt", sequence: 1, street: "RIVER", cards: ["3s"] },
      ],
    });
    store.apply({
      type: "Snapshot",
      view: samplePlayerView({
        handNumber: 1,
        street: "COMPLETE",
        board: { cards: ["As", "7d", "2c", "Kh", "3s"] },
      }),
    });
    // One RIVER step, then the final step: two steps stand.
    expect(store.getState().reveal?.steps.length).toBe(2);

    // Hand 2's frames arrive while the steps stand.
    store.apply({
      type: "Events",
      events: [
        {
          type: "HandStarted",
          sequence: 90,
          handNumber: 2,
          buttonSeat: 1,
          smallBlind: 25,
          bigBlind: 50,
          stacks: [1500, 1500],
        },
      ],
    });
    store.apply({
      type: "Snapshot",
      view: samplePlayerView({ handNumber: 2, street: "PREFLOP" }),
    });
    store.apply({
      type: "YourTurn",
      handNumber: 2,
      actionSequence: 1,
      legalActions: {
        seat: 0,
        allowed: ["CHECK", "BET"],
        callTo: 0,
        minBetTo: 25,
        minRaiseTo: 50,
        allInTo: 1500,
      },
    });

    // None of hand 2 is visible before the last step: the reveal still owns the screen.
    expect(store.getState().view?.handNumber).toBe(1);
    expect(store.getState().pendingTurn).toBeNull();

    fireNext(); // the RIVER step stands; the final step now shows
    expect(store.getState().view?.handNumber).toBe(1);
    expect(store.getState().pendingTurn).toBeNull();

    fireNext(); // the final step has stood; the queue releases, in arrival order
    expect(store.getState().reveal).toBeNull();
    expect(store.getState().view?.handNumber).toBe(2);
    expect(store.getState().pendingTurn).toEqual({
      handNumber: 2,
      actionSequence: 1,
      legalActions: {
        seat: 0,
        allowed: ["CHECK", "BET"],
        callTo: 0,
        minBetTo: 25,
        minRaiseTo: 50,
        allInTo: 1500,
      },
    });
  });
});
