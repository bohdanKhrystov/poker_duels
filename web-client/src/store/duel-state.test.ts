import { describe, expect, it } from "vitest";
import type {
  ActedForAbsent,
  LegalActions,
  PlayerView,
  SeatView,
} from "../protocol";
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
      rematchOffers: [],
      rivalPresence: null,
      graceRemainingMillis: null,
      presenceCount: 0,
      rivalReturned: false,
      serverAction: null,
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
      playerId: "test-player",
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

  it("records the seat a rematch offer named", () => {
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "RematchOffered",
      seat: 1,
    });
    expect(state.rematchOffers).toEqual([1]);
  });

  it("records an offer from each seat", () => {
    const stateAfterFirstOffer = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RematchOffered",
        seat: 0,
      },
    );
    const state = duelState.applyServerMessage(stateAfterFirstOffer, {
      type: "RematchOffered",
      seat: 1,
    });
    expect(state.rematchOffers).toEqual([0, 1]);
  });

  it("records a repeated offer once, and returns the state it was given", () => {
    const stateAfterFirstOffer = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RematchOffered",
        seat: 0,
      },
    );
    const state = duelState.applyServerMessage(stateAfterFirstOffer, {
      type: "RematchOffered",
      seat: 0,
    });
    expect(state.rematchOffers).toEqual([0]);
    expect(state).toBe(stateAfterFirstOffer);
  });

  it("an offer that arrived before the finish does not reach the result screen", () => {
    const outcome = {
      winner: 0,
      handsPlayed: 5,
      finalStacks: [1500, 500],
    } as const;
    const stateAfterOffer = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RematchOffered",
        seat: 1,
      },
    );
    expect(stateAfterOffer.rematchOffers).toEqual([1]);
    const stateAfterFinish = duelState.applyServerMessage(stateAfterOffer, {
      type: "DuelFinished",
      outcome,
    });
    expect(stateAfterFinish.rematchOffers).toEqual([]);
    expect(stateAfterFinish.outcome).toEqual(outcome);
  });

  it("an offer that arrives after the finish stands", () => {
    const outcome = {
      winner: 0,
      handsPlayed: 5,
      finalStacks: [1500, 500],
    } as const;
    const stateAfterFinish = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "DuelFinished",
        outcome,
      },
    );
    expect(stateAfterFinish.rematchOffers).toEqual([]);
    expect(stateAfterFinish.outcome).toEqual(outcome);
    const stateAfterOffer = duelState.applyServerMessage(stateAfterFinish, {
      type: "RematchOffered",
      seat: 1,
    });
    expect(stateAfterOffer.rematchOffers).toEqual([1]);
    expect(stateAfterOffer.outcome).toEqual(outcome);
  });

  it("the snapshot after a finish clears the result it replaces", () => {
    const finishOutcome = {
      winner: 0,
      handsPlayed: 7,
      finalStacks: [1500, 500],
    } as const;
    const stateAfterFinish = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "DuelFinished",
        outcome: finishOutcome,
      },
    );
    expect(stateAfterFinish.outcome).toEqual(finishOutcome);
    const rematchView = samplePlayerView({ handNumber: 2 });
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterFinish, {
      type: "Snapshot",
      view: rematchView,
    });
    expect(stateAfterSnapshot.outcome).toBeNull();
    expect(stateAfterSnapshot.view).toEqual(rematchView);
  });

  it("the snapshot after a finish clears the offers that started it", () => {
    const outcome = {
      winner: 1,
      handsPlayed: 3,
      finalStacks: [500, 1500],
    } as const;
    const stateAfterFinish = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "DuelFinished",
        outcome,
      },
    );
    const stateAfterOffer = duelState.applyServerMessage(stateAfterFinish, {
      type: "RematchOffered",
      seat: 1,
    });
    expect(stateAfterOffer.rematchOffers).toEqual([1]);
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterOffer, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.rematchOffers).toEqual([]);
    expect(stateAfterSnapshot.outcome).toBeNull();
  });

  it("a rematch the room cannot take yet enters no state", () => {
    const outcome = {
      winner: 0,
      handsPlayed: 5,
      finalStacks: [1500, 500],
    } as const;
    const stateAfterFinish = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "DuelFinished",
        outcome,
      },
    );
    const stateAfterOffer = duelState.applyServerMessage(stateAfterFinish, {
      type: "RematchOffered",
      seat: 1,
    });
    expect(stateAfterOffer.rematchOffers).toEqual([1]);
    const state = duelState.applyServerMessage(stateAfterOffer, {
      type: "Failure",
      error: "REMATCH_UNAVAILABLE",
    });
    expect(state).toBe(stateAfterOffer);
    expect(state.refusal).toBeNull();
    expect(state.rematchOffers).toEqual([1]);
  });

  it("every other refusal is still recorded", () => {
    const outcome = {
      winner: 0,
      handsPlayed: 5,
      finalStacks: [1500, 500],
    } as const;
    const stateAfterFinish = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "DuelFinished",
        outcome,
      },
    );
    const stateAfterOffer = duelState.applyServerMessage(stateAfterFinish, {
      type: "RematchOffered",
      seat: 1,
    });
    expect(stateAfterOffer.rematchOffers).toEqual([1]);
    const state = duelState.applyServerMessage(stateAfterOffer, {
      type: "Failure",
      error: "ROOM_FULL",
    });
    expect(state).not.toBe(stateAfterOffer);
    expect(state.refusal).toBe("ROOM_FULL");
    expect(state.rematchOffers).toEqual([1]);
  });

  it("records the presence and the window the server sent", () => {
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "OpponentPresence",
      presence: "AWAY",
      graceRemainingMillis: 47000,
    });
    expect(state.rivalPresence).toBe("AWAY");
    expect(state.graceRemainingMillis).toBe(47000);
    expect(state.presenceCount).toBe(1);
  });

  it("records a window that ran out, with nothing left of it", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    expect(stateAfterAway.graceRemainingMillis).toBe(47000);
    const stateAfterAbsent = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "ABSENT",
      graceRemainingMillis: null,
    });
    expect(stateAfterAbsent.rivalPresence).toBe("ABSENT");
    expect(stateAfterAbsent.graceRemainingMillis).toBeNull();
    expect(stateAfterAbsent.presenceCount).toBe(2);
  });

  it("counts two windows that carry the same remaining as two", () => {
    const stateAfterFirst = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    expect(stateAfterFirst.presenceCount).toBe(1);
    const stateAfterSecond = duelState.applyServerMessage(stateAfterFirst, {
      type: "OpponentPresence",
      presence: "AWAY",
      graceRemainingMillis: 47000,
    });
    expect(stateAfterSecond.presenceCount).toBe(2);
    expect(stateAfterSecond.rivalPresence).toBe("AWAY");
    expect(stateAfterSecond.graceRemainingMillis).toBe(47000);
  });

  it("a presence changes nothing a snapshot or a turn established", () => {
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
    const state = duelState.applyServerMessage(stateWithPending, {
      type: "OpponentPresence",
      presence: "AWAY",
      graceRemainingMillis: 47000,
    });
    expect(state.view).toEqual(view);
    expect(state.pendingTurn).toEqual({
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    expect(state.narration).toEqual([]);
    expect(state.rejection).toBeNull();
    expect(state.rejectionCount).toBe(0);
    expect(state.outcome).toBeNull();
    expect(state.refusal).toBeNull();
    expect(state.mySeat).toBeNull();
    expect(state.roomCode).toBeNull();
    expect(state.rematchOffers).toEqual([]);
  });

  it("presence persists through snapshot and turn", () => {
    // Step 1: Set presence with a non-null graceRemainingMillis
    const stateWithPresence = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    expect(stateWithPresence.rivalPresence).toBe("AWAY");
    expect(stateWithPresence.graceRemainingMillis).toBe(47000);

    // Step 2: Apply Snapshot and assert presence remains
    const stateAfterSnapshot = duelState.applyServerMessage(stateWithPresence, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.rivalPresence).toBe("AWAY");
    expect(stateAfterSnapshot.graceRemainingMillis).toBe(47000);

    // Step 3: Apply YourTurn and assert presence remains
    const legalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    } as const;
    const stateAfterTurn = duelState.applyServerMessage(stateAfterSnapshot, {
      type: "YourTurn",
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    expect(stateAfterTurn.rivalPresence).toBe("AWAY");
    expect(stateAfterTurn.graceRemainingMillis).toBe(47000);
  });

  it("a rival who was away and is present again has come back", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    const state = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(state.rivalReturned).toBe(true);
    expect(state.rivalPresence).toBe("PRESENT");
  });

  it("a rival who timed out and is present again has come back", () => {
    const stateAfterAbsent = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "ABSENT",
        graceRemainingMillis: null,
      },
    );
    const state = duelState.applyServerMessage(stateAfterAbsent, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(state.rivalReturned).toBe(true);
  });

  it("a presence that never changed is no return", () => {
    // ADR-0046 §2's trap: a resuming client is always sent its rival's current presence,
    // PRESENT included, with no AWAY or ABSENT before it in this client's history.
    const stateAfterFirstPresent = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "PRESENT",
        graceRemainingMillis: null,
      },
    );
    expect(stateAfterFirstPresent.rivalReturned).toBe(false);
    const state = duelState.applyServerMessage(stateAfterFirstPresent, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(state.rivalReturned).toBe(false);
  });

  it("the next snapshot ends the return and leaves the presence", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    const stateAfterReturn = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(stateAfterReturn.rivalReturned).toBe(true);
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterReturn, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.rivalReturned).toBe(false);
    expect(stateAfterSnapshot.rivalPresence).toBe("PRESENT");

    // A snapshot applied after AWAY alone changes nothing else: rivalPresence and
    // graceRemainingMillis stay exactly what the last OpponentPresence stated.
    const stateAfterAwayAlone = duelState.applyServerMessage(stateAfterAway, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterAwayAlone.rivalPresence).toBe("AWAY");
    expect(stateAfterAwayAlone.graceRemainingMillis).toBe(47000);
  });

  it("a resume states the presence after its own snapshot", () => {
    // RoomRegistry.resume sends resumeFrames(runner, seat) + presence, so a resuming client
    // sees the Snapshot before the OpponentPresence that reports the return.
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterAway, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    const state = duelState.applyServerMessage(stateAfterSnapshot, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(state.rivalReturned).toBe(true);
  });

  it("going away again is not a return", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
        graceRemainingMillis: 47000,
      },
    );
    const stateAfterReturn = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(stateAfterReturn.rivalReturned).toBe(true);
    const state = duelState.applyServerMessage(stateAfterReturn, {
      type: "OpponentPresence",
      presence: "AWAY",
      graceRemainingMillis: 47000,
    });
    expect(state.rivalReturned).toBe(false);
    expect(state.rivalPresence).toBe("AWAY");
  });

  it("records the mark exactly as the server sent it", () => {
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 3,
      actionSequence: 7,
      action: "FOLD",
    };
    const state = duelState.applyServerMessage(duelState.initialState(), mark);
    expect(state.serverAction).toEqual(mark);
  });

  it("a later mark replaces an earlier one", () => {
    const firstMark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 3,
      actionSequence: 7,
      action: "FOLD",
    };
    const secondMark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 0,
      handNumber: 41,
      actionSequence: 2,
      action: "CHECK",
    };
    const stateAfterFirst = duelState.applyServerMessage(
      duelState.initialState(),
      firstMark,
    );
    const state = duelState.applyServerMessage(stateAfterFirst, secondMark);
    expect(state.serverAction).toEqual(secondMark);
  });

  it("a mark survives the events that describe the same decision point", () => {
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 3,
      actionSequence: 7,
      action: "FOLD",
    };
    const playerFolded = {
      type: "PlayerFolded",
      sequence: 7,
      seat: 1,
    } as const;
    const stateWithMark = duelState.applyServerMessage(
      duelState.initialState(),
      mark,
    );
    const markThenEvents = duelState.applyServerMessage(stateWithMark, {
      type: "Events",
      events: [playerFolded],
    });
    expect(markThenEvents.serverAction).toEqual(mark);

    const stateWithEvents = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "Events",
        events: [playerFolded],
      },
    );
    const eventsThenMark = duelState.applyServerMessage(stateWithEvents, mark);
    expect(eventsThenMark.serverAction).toEqual(markThenEvents.serverAction);
  });

  it("a mark changes nothing a snapshot or a turn established", () => {
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
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 1,
      actionSequence: 1,
      action: "FOLD",
    };
    const state = duelState.applyServerMessage(stateWithPending, mark);
    expect(state.view).toEqual(view);
    expect(state.pendingTurn).toEqual({
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    expect(state.narration).toEqual([]);
    expect(state.rejection).toBeNull();
    expect(state.rejectionCount).toBe(0);
    expect(state.outcome).toBeNull();
    expect(state.refusal).toBeNull();
    expect(state.rematchOffers).toEqual([]);
    expect(state.rivalPresence).toBeNull();
    expect(state.graceRemainingMillis).toBeNull();
    expect(state.presenceCount).toBe(0);
    expect(state.rivalReturned).toBe(false);
  });

  it("a snapshot and a turn after the mark leave it standing", () => {
    // ADR-0075 §2 & Context: AbsentSeats.kt prepends the mark to the outbound its own action
    // produced, and that outbound is broadcast + turnFor — so the mark, the Snapshot describing
    // its own action, and the next YourTurn all arrive in one delivery. A reducer that cleared on
    // either would erase the mark microseconds after setting it. The mark goes first here, then
    // the two frames that must not touch it.
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 1,
      actionSequence: 1,
      action: "FOLD",
    };
    const stateWithMark = duelState.applyServerMessage(
      duelState.initialState(),
      mark,
    );
    const stateAfterSnapshot = duelState.applyServerMessage(stateWithMark, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    const legalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    } as const;
    const state = duelState.applyServerMessage(stateAfterSnapshot, {
      type: "YourTurn",
      handNumber: 2,
      actionSequence: 5,
      legalActions,
    });
    expect(state.serverAction).toEqual(mark);
  });

  it("a rival still away or still absent keeps the mark", () => {
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 1,
      actionSequence: 1,
      action: "FOLD",
    };
    const stateWithMark = duelState.applyServerMessage(
      duelState.initialState(),
      mark,
    );
    const stateAfterAway = duelState.applyServerMessage(stateWithMark, {
      type: "OpponentPresence",
      presence: "AWAY",
      graceRemainingMillis: 60000,
    });
    expect(stateAfterAway.serverAction).toEqual(mark);

    const stateAfterAbsent = duelState.applyServerMessage(stateWithMark, {
      type: "OpponentPresence",
      presence: "ABSENT",
      graceRemainingMillis: null,
    });
    expect(stateAfterAbsent.serverAction).toEqual(mark);
  });

  it("a rival's return takes the mark off", () => {
    const stateAfterAbsent = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "ABSENT",
        graceRemainingMillis: null,
      },
    );
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 1,
      actionSequence: 1,
      action: "FOLD",
    };
    const stateWithMark = duelState.applyServerMessage(stateAfterAbsent, mark);
    const state = duelState.applyServerMessage(stateWithMark, {
      type: "OpponentPresence",
      presence: "PRESENT",
      graceRemainingMillis: null,
    });
    expect(state.serverAction).toBeNull();
    expect(state.rivalReturned).toBe(true);
  });

  it("the duel ending takes the mark off", () => {
    const mark: ActedForAbsent = {
      type: "ActedForAbsent",
      seat: 1,
      handNumber: 1,
      actionSequence: 1,
      action: "FOLD",
    };
    const stateWithMark = duelState.applyServerMessage(
      duelState.initialState(),
      mark,
    );
    const state = duelState.applyServerMessage(stateWithMark, {
      type: "DuelFinished",
      outcome: {
        winner: 1,
        handsPlayed: 12,
        finalStacks: [0, 2000],
      },
    });
    expect(state.serverAction).toBeNull();
  });
});
