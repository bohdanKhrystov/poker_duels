import { describe, expect, it } from "vitest";
import type { LegalActions, PlayerView, SeatView } from "../protocol";
import * as duelState from "./duel-state";

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

describe("the duel state", () => {
  it("starts with nothing the server has not sent", () => {
    const state = duelState.initialState();
    expect(state).toEqual({
      mySeat: null,
      roomCode: null,
      view: null,
      pendingTurn: null,
      narration: [],
      rejection: null,
      rejectionCount: 0,
      outcome: null,
      refusal: null,
    });
  });

  it("RoomJoined sets the seat and the room code", () => {
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "RoomJoined",
      code: "ABCD",
      seat: 1,
    });
    expect(state.mySeat).toBe(1);
    expect(state.roomCode).toBe("ABCD");
  });

  it("leaves state unchanged for a message it has no opinion about", () => {
    const initialState = duelState.initialState();
    const stateWithSeat = duelState.applyServerMessage(initialState, {
      type: "RoomJoined",
      code: "ABCD",
      seat: 1,
    });
    const welcomed = duelState.applyServerMessage(stateWithSeat, {
      type: "Welcome",
      deviceId: "test-device",
      protocolVersion: 2,
    });
    expect(welcomed).toBe(stateWithSeat);
  });

  it("exports only the reducer and the initial state", () => {
    expect(Object.keys(duelState).sort()).toEqual([
      "applyServerMessage",
      "initialState",
    ]);
  });

  it("sets a pending turn identified verbatim by the message", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "YourTurn",
      handNumber: 3,
      actionSequence: 7,
      legalActions,
    });
    expect(state.pendingTurn).toEqual({
      handNumber: 3,
      actionSequence: 7,
      legalActions,
    });
  });

  it("replaces rather than merges a pending turn already set", () => {
    const firstLegalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const secondLegalActions: LegalActions = {
      seat: 0,
      allowed: ["FOLD", "CALL", "RAISE"],
      callTo: 20,
      minBetTo: 50,
      minRaiseTo: 100,
      allInTo: 500,
    };
    const stateAfterFirst = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 5,
        legalActions: firstLegalActions,
      },
    );
    const stateAfterSecond = duelState.applyServerMessage(stateAfterFirst, {
      type: "YourTurn",
      handNumber: 3,
      actionSequence: 3,
      legalActions: secondLegalActions,
    });
    expect(stateAfterSecond.pendingTurn).toEqual({
      handNumber: 3,
      actionSequence: 3,
      legalActions: secondLegalActions,
    });
  });

  it("replaces the view wholesale", () => {
    const view = samplePlayerView();
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Snapshot",
      view,
    });
    expect(state.view).toEqual(view);
  });

  it("clears a pending turn set by an earlier YourTurn", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions,
      },
    );
    const state = duelState.applyServerMessage(stateWithPendingTurn, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(state.pendingTurn).toBeNull();
  });

  it("keeps mySeat from RoomJoined when a snapshot's viewerSeat disagrees", () => {
    const stateWithSeat = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RoomJoined",
        code: "ABCD",
        seat: 0,
      },
    );
    const state = duelState.applyServerMessage(stateWithSeat, {
      type: "Snapshot",
      view: samplePlayerView({ viewerSeat: 1 }),
    });
    expect(state.mySeat).toBe(0);
    expect(state.view?.viewerSeat).toBe(1);
  });

  it("keeps an opponent's hole cards empty until the snapshot reveals them", () => {
    const view = samplePlayerView({
      seats: [
        sampleSeat({ index: 0 }),
        sampleSeat({ index: 1, holeCards: [] }),
      ],
    });
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Snapshot",
      view,
    });
    expect(state.view?.seats[1]?.holeCards).toEqual([]);
  });

  it("reflects a seat's hole cards exactly once the snapshot reveals them", () => {
    const view = samplePlayerView({
      seats: [
        sampleSeat({ index: 0 }),
        sampleSeat({ index: 1, holeCards: ["2c", "7h"] }),
      ],
    });
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Snapshot",
      view,
    });
    expect(state.view?.seats[1]?.holeCards).toEqual(["2c", "7h"]);
  });

  it("a rejected action leaves the decision point open", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET", "RAISE"],
      callTo: 0,
      minBetTo: 15,
      minRaiseTo: 30,
      allInTo: 150,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions,
      },
    );
    const rejection = {
      type: "AmountTooSmall",
      attempted: 5,
      minimum: 10,
    } as const;
    const state = duelState.applyServerMessage(stateWithPendingTurn, {
      type: "Rejected",
      rejection,
    });
    expect(state.pendingTurn).toEqual({
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    expect(state.rejection).toEqual(rejection);
    expect(state.rejectionCount).toBe(1);
  });

  it("a rejection stops being shown when the next turn opens", () => {
    const stateWithRejection = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 3, minimum: 25 },
      },
    );
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["FOLD", "CALL"],
      callTo: 15,
      minBetTo: 25,
      minRaiseTo: 55,
      allInTo: 300,
    };
    const state = duelState.applyServerMessage(stateWithRejection, {
      type: "YourTurn",
      handNumber: 2,
      actionSequence: 6,
      legalActions,
    });
    expect(state.rejection).toBeNull();
  });

  it("a rejection stops being shown when the next snapshot arrives", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions,
      },
    );
    const stateWithRejection = duelState.applyServerMessage(
      stateWithPendingTurn,
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 12, minimum: 50 },
      },
    );
    const state = duelState.applyServerMessage(stateWithRejection, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(state.rejection).toBeNull();
    expect(state.pendingTurn).toBeNull();
  });

  it("a rejection stops being shown when the duel finishes", () => {
    const stateWithRejection = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 1, minimum: 2 },
      },
    );
    const outcome = {
      winner: 0,
      handsPlayed: 7,
      finalStacks: [1500, 0],
    } as const;
    const state = duelState.applyServerMessage(stateWithRejection, {
      type: "DuelFinished",
      outcome,
    });
    expect(state.rejection).toBeNull();
    expect(state.outcome).toEqual(outcome);
  });

  it("two rejections at one decision point are two attempts", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["FOLD", "CALL", "RAISE"],
      callTo: 20,
      minBetTo: 50,
      minRaiseTo: 100,
      allInTo: 500,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 4,
        actionSequence: 9,
        legalActions,
      },
    );
    const stateAfterFirstRejection = duelState.applyServerMessage(
      stateWithPendingTurn,
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 5, minimum: 10 },
      },
    );
    const state = duelState.applyServerMessage(stateAfterFirstRejection, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 7, minimum: 10 },
    });
    expect(state.rejectionCount).toBe(2);
    expect(state.pendingTurn).toEqual({
      handNumber: 4,
      actionSequence: 9,
      legalActions,
    });
  });

  it("a new turn does not reset the rejection count", () => {
    const stateAfterFirstRejection = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 4, minimum: 15 },
      },
    );
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 5,
      minRaiseTo: 10,
      allInTo: 200,
    };
    const stateWithNewTurn = duelState.applyServerMessage(
      stateAfterFirstRejection,
      {
        type: "YourTurn",
        handNumber: 5,
        actionSequence: 2,
        legalActions,
      },
    );
    const state = duelState.applyServerMessage(stateWithNewTurn, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 6, minimum: 15 },
    });
    expect(state.rejectionCount).toBe(2);
  });

  it("surfaces the rejection exactly as the server sent it", () => {
    const rejection = {
      type: "AmountTooSmall",
      attempted: 5,
      minimum: 10,
    } as const;
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Rejected",
      rejection,
    });
    expect(state.rejection).toEqual(rejection);
  });

  it("leaves the view untouched", () => {
    const view = samplePlayerView();
    const stateWithView = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Snapshot",
        view,
      },
    );
    const state = duelState.applyServerMessage(stateWithView, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 5, minimum: 10 },
    });
    expect(state.view).toEqual(view);
  });

  it("appends events to the narration log in order", () => {
    const event1 = {
      type: "ActionOn",
      sequence: 1,
      seat: 0,
    } as const;
    const event2 = {
      type: "PlayerBet",
      sequence: 2,
      seat: 1,
      to: 20,
    } as const;
    const stateAfterFirstEvents = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Events",
        events: [event1],
      },
    );
    expect(stateAfterFirstEvents.narration).toEqual([event1]);
    const stateAfterSecondEvents = duelState.applyServerMessage(
      stateAfterFirstEvents,
      {
        type: "Events",
        events: [event2],
      },
    );
    expect(stateAfterSecondEvents.narration).toEqual([event1, event2]);
  });

  it("changes no field a snapshot or a pending turn established", () => {
    const view = samplePlayerView();
    const legalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    } as const;
    const stateWithView = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Snapshot",
        view,
      },
    );
    const stateWithPending = duelState.applyServerMessage(stateWithView, {
      type: "YourTurn",
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    const event = {
      type: "ActionOn",
      sequence: 1,
      seat: 0,
    } as const;
    const stateAfterEvents = duelState.applyServerMessage(stateWithPending, {
      type: "Events",
      events: [event],
    });
    expect(stateAfterEvents.view).toEqual(view);
    expect(stateAfterEvents.pendingTurn).toEqual({
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
  });

  it("does not populate a seat's hole cards from a HandRevealed event", () => {
    const view = samplePlayerView({
      seats: [
        sampleSeat({ index: 0 }),
        sampleSeat({ index: 1, holeCards: [] }),
      ],
    });
    const stateWithView = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Snapshot",
        view,
      },
    );
    const handRevealed = {
      type: "HandRevealed",
      sequence: 1,
      seat: 1,
      cards: ["2c", "7h"],
    } as const;
    const stateAfterEvents = duelState.applyServerMessage(stateWithView, {
      type: "Events",
      events: [handRevealed],
    });
    expect(stateAfterEvents.view?.seats[1]?.holeCards).toEqual([]);
  });

  it("records the outcome exactly as the server sent it", () => {
    const outcome = {
      winner: 1,
      handsPlayed: 12,
      finalStacks: [0, 2000],
    } as const;
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "DuelFinished",
      outcome,
    });
    expect(state.outcome).toEqual(outcome);
  });

  it("clears any pending turn once the duel finishes", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions,
      },
    );
    const state = duelState.applyServerMessage(stateWithPendingTurn, {
      type: "DuelFinished",
      outcome: {
        winner: 1,
        handsPlayed: 12,
        finalStacks: [0, 2000],
      },
    });
    expect(state.pendingTurn).toBeNull();
  });

  it("leaves the view and narration untouched", () => {
    const view = samplePlayerView();
    const event1 = {
      type: "ActionOn",
      sequence: 1,
      seat: 0,
    } as const;
    const event2 = {
      type: "PlayerBet",
      sequence: 2,
      seat: 1,
      to: 20,
    } as const;
    const stateWithView = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Snapshot",
        view,
      },
    );
    const stateWithEvents = duelState.applyServerMessage(stateWithView, {
      type: "Events",
      events: [event1, event2],
    });
    const state = duelState.applyServerMessage(stateWithEvents, {
      type: "DuelFinished",
      outcome: {
        winner: 1,
        handsPlayed: 12,
        finalStacks: [0, 2000],
      },
    });
    expect(state.view).toEqual(view);
    expect(state.narration).toEqual([event1, event2]);
  });

  it("records the room the server does not know", () => {
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Failure",
      error: "UNKNOWN_ROOM",
    });
    expect(state.refusal).toBe("UNKNOWN_ROOM");
  });

  it("records a room that already has a rival in it", () => {
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Failure",
      error: "ROOM_FULL",
    });
    expect(state.refusal).toBe("ROOM_FULL");
  });

  it("a refusal changes nothing a RoomJoined established", () => {
    const stateWithRoom = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RoomJoined",
        code: "ABCDEFGH",
        seat: 0,
      },
    );
    const state = duelState.applyServerMessage(stateWithRoom, {
      type: "Failure",
      error: "ROOM_FULL",
    });
    expect(state.mySeat).toBe(0);
    expect(state.roomCode).toBe("ABCDEFGH");
  });

  it("a join that lands clears the refusal before it", () => {
    const stateWithRefusal = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Failure",
        error: "UNKNOWN_ROOM",
      },
    );
    const state = duelState.applyServerMessage(stateWithRefusal, {
      type: "RoomJoined",
      code: "ABCDEFGH",
      seat: 1,
    });
    expect(state.refusal).toBeNull();
  });

  it("a refusal stops being shown when the next turn opens", () => {
    const stateWithRefusal = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Failure",
        error: "DUEL_PAUSED",
      },
    );
    // Events is narration; it must not clear the refusal a Failure set
    const stateAfterEvents = duelState.applyServerMessage(stateWithRefusal, {
      type: "Events",
      events: [
        {
          type: "ActionOn",
          sequence: 1,
          seat: 0,
        } as const,
      ],
    });
    expect(stateAfterEvents.refusal).toBe("DUEL_PAUSED");
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const state = duelState.applyServerMessage(stateAfterEvents, {
      type: "YourTurn",
      handNumber: 2,
      actionSequence: 5,
      legalActions,
    });
    expect(state.refusal).toBeNull();
  });

  it("a refusal stops being shown when the next snapshot arrives", () => {
    const stateWithRefusal = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Failure",
        error: "DUEL_PAUSED",
      },
    );
    const stateWithRejection = duelState.applyServerMessage(stateWithRefusal, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 5, minimum: 10 },
    });
    // Rejected is about an action attempt; it must not clear the refusal a Failure set
    expect(stateWithRejection.refusal).toBe("DUEL_PAUSED");
    const state = duelState.applyServerMessage(stateWithRejection, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(state.refusal).toBeNull();
    expect(state.rejection).toBeNull();
    expect(state.rejectionCount).toBe(1);
  });

  it("a refusal stops being shown when the duel finishes", () => {
    const stateWithRefusal = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Failure",
        error: "DUEL_PAUSED",
      },
    );
    const outcome = {
      winner: 0,
      handsPlayed: 8,
      finalStacks: [1500, 500],
    } as const;
    const state = duelState.applyServerMessage(stateWithRefusal, {
      type: "DuelFinished",
      outcome,
    });
    expect(state.refusal).toBeNull();
    expect(state.outcome).toEqual(outcome);
  });

  it("a refusal closes no decision point", () => {
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const stateWithPendingTurn = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions,
      },
    );
    const state = duelState.applyServerMessage(stateWithPendingTurn, {
      type: "Failure",
      error: "DUEL_PAUSED",
    });
    expect(state.refusal).toBe("DUEL_PAUSED");
    expect(state.pendingTurn).toEqual({
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    expect(state.rejectionCount).toBe(0);
  });
});
