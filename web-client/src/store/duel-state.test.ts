import { describe, expect, it } from "vitest";
import type {
  ActedForAbsent,
  LegalActions,
  PlayerView,
  SeatView,
  StreetDealt,
  TurnClock,
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
      presenceCount: 0,
      rivalReturned: false,
      serverAction: null,
      lastAct: null,
      pendingStreetDealt: [],
      reveal: null,
      turnClock: null,
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

  it("exports only the reducer, the initial state and the per-tick advance", () => {
    expect(Object.keys(duelState).sort()).toEqual([
      "advanceReveal",
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
        error: "NOT_IN_DUEL",
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
    expect(stateAfterEvents.refusal).toBe("NOT_IN_DUEL");
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
        error: "NOT_IN_DUEL",
      },
    );
    const stateWithRejection = duelState.applyServerMessage(stateWithRefusal, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 5, minimum: 10 },
    });
    // Rejected is about an action attempt; it must not clear the refusal a Failure set
    expect(stateWithRejection.refusal).toBe("NOT_IN_DUEL");
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
        error: "NOT_IN_DUEL",
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
      error: "NOT_IN_DUEL",
    });
    expect(state.refusal).toBe("NOT_IN_DUEL");
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
    });
    expect(state.rivalPresence).toBe("AWAY");
    expect(state.presenceCount).toBe(1);
  });

  it("records a window that ran out, with nothing left of it", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    const stateAfterAbsent = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "ABSENT",
    });
    expect(stateAfterAbsent.rivalPresence).toBe("ABSENT");
    expect(stateAfterAbsent.presenceCount).toBe(2);
  });

  it("counts two windows that carry the same remaining as two", () => {
    const stateAfterFirst = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    expect(stateAfterFirst.presenceCount).toBe(1);
    const stateAfterSecond = duelState.applyServerMessage(stateAfterFirst, {
      type: "OpponentPresence",
      presence: "AWAY",
    });
    expect(stateAfterSecond.presenceCount).toBe(2);
    expect(stateAfterSecond.rivalPresence).toBe("AWAY");
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
    // Step 1: Set presence to AWAY
    const stateWithPresence = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    expect(stateWithPresence.rivalPresence).toBe("AWAY");

    // Step 2: Apply Snapshot and assert presence remains
    const stateAfterSnapshot = duelState.applyServerMessage(stateWithPresence, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.rivalPresence).toBe("AWAY");

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
  });

  it("a rival who was away and is present again has come back", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    const state = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
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
      },
    );
    const state = duelState.applyServerMessage(stateAfterAbsent, {
      type: "OpponentPresence",
      presence: "PRESENT",
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
      },
    );
    expect(stateAfterFirstPresent.rivalReturned).toBe(false);
    const state = duelState.applyServerMessage(stateAfterFirstPresent, {
      type: "OpponentPresence",
      presence: "PRESENT",
    });
    expect(state.rivalReturned).toBe(false);
  });

  it("the next snapshot ends the return and leaves the presence", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    const stateAfterReturn = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
    });
    expect(stateAfterReturn.rivalReturned).toBe(true);
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterReturn, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.rivalReturned).toBe(false);
    expect(stateAfterSnapshot.rivalPresence).toBe("PRESENT");

    // A snapshot applied after AWAY alone changes nothing else: rivalPresence
    // stays exactly what the last OpponentPresence stated.
    const stateAfterAwayAlone = duelState.applyServerMessage(stateAfterAway, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterAwayAlone.rivalPresence).toBe("AWAY");
  });

  it("a resume states the presence after its own snapshot", () => {
    // RoomRegistry.resume sends resumeFrames(runner, seat) + presence, so a resuming client
    // sees the Snapshot before the OpponentPresence that reports the return.
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterAway, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    const state = duelState.applyServerMessage(stateAfterSnapshot, {
      type: "OpponentPresence",
      presence: "PRESENT",
    });
    expect(state.rivalReturned).toBe(true);
  });

  it("going away again is not a return", () => {
    const stateAfterAway = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "AWAY",
      },
    );
    const stateAfterReturn = duelState.applyServerMessage(stateAfterAway, {
      type: "OpponentPresence",
      presence: "PRESENT",
    });
    expect(stateAfterReturn.rivalReturned).toBe(true);
    const state = duelState.applyServerMessage(stateAfterReturn, {
      type: "OpponentPresence",
      presence: "AWAY",
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
    });
    expect(stateAfterAway.serverAction).toEqual(mark);

    const stateAfterAbsent = duelState.applyServerMessage(stateWithMark, {
      type: "OpponentPresence",
      presence: "ABSENT",
    });
    expect(stateAfterAbsent.serverAction).toEqual(mark);
  });

  it("a rival's return takes the mark off", () => {
    const stateAfterAbsent = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "OpponentPresence",
        presence: "ABSENT",
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

  it("lays out one step per StreetDealt and a final step for the whole snapshot", () => {
    const streetDealt: readonly StreetDealt[] = [
      {
        type: "StreetDealt",
        sequence: 10,
        street: "FLOP",
        cards: ["As", "7d", "2c"],
      },
      { type: "StreetDealt", sequence: 11, street: "TURN", cards: ["Kh"] },
      { type: "StreetDealt", sequence: 12, street: "RIVER", cards: ["3s"] },
    ];
    const stateWithEvents = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: streetDealt },
    );
    const view = samplePlayerView({
      street: "COMPLETE",
      board: { cards: ["As", "7d", "2c", "Kh", "3s"] },
    });
    const state = duelState.applyServerMessage(stateWithEvents, {
      type: "Snapshot",
      view,
    });
    expect(state.reveal?.steps).toEqual([
      { board: ["As", "7d", "2c"], street: "FLOP" },
      { board: ["As", "7d", "2c", "Kh"], street: "TURN" },
      { board: ["As", "7d", "2c", "Kh", "3s"], street: "RIVER" },
      { board: ["As", "7d", "2c", "Kh", "3s"], street: "COMPLETE" },
    ]);
  });

  it("a snapshot at COMPLETE with no events before it takes one step, not four", () => {
    const view = samplePlayerView({
      street: "COMPLETE",
      board: { cards: ["As", "7d", "2c", "Kh", "3s"] },
    });
    const state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Snapshot",
      view,
    });
    expect(state.reveal?.steps).toEqual([
      { board: ["As", "7d", "2c", "Kh", "3s"], street: "COMPLETE" },
    ]);
  });

  it("a snapshot that does not end a hand lays out no steps at all", () => {
    const streetDealt: readonly StreetDealt[] = [
      {
        type: "StreetDealt",
        sequence: 5,
        street: "FLOP",
        cards: ["As", "7d", "2c"],
      },
    ];
    const stateWithEvents = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: streetDealt },
    );
    const view = samplePlayerView({
      street: "FLOP",
      board: { cards: ["As", "7d", "2c"] },
    });
    const state = duelState.applyServerMessage(stateWithEvents, {
      type: "Snapshot",
      view,
    });
    expect(state.reveal).toBeNull();
  });

  it("a RoomJoined naming a different room clears what the old room left behind", () => {
    const stateInRoomA = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RoomJoined",
        code: "ABCD",
        seat: 0,
      },
    );
    const stateWithPresence = duelState.applyServerMessage(stateInRoomA, {
      type: "OpponentPresence",
      presence: "AWAY",
    });
    const state = duelState.applyServerMessage(stateWithPresence, {
      type: "RoomJoined",
      code: "EFGH",
      seat: 1,
    });
    expect(state.roomCode).toBe("EFGH");
    expect(state.mySeat).toBe(1);
    expect(state.rivalPresence).toBeNull();
  });

  it("a RoomJoined naming the room the store already holds clears nothing", () => {
    const stateInRoomA = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RoomJoined",
        code: "ABCD",
        seat: 0,
      },
    );
    const stateWithPresence = duelState.applyServerMessage(stateInRoomA, {
      type: "OpponentPresence",
      presence: "AWAY",
    });
    const state = duelState.applyServerMessage(stateWithPresence, {
      type: "RoomJoined",
      code: "ABCD",
      seat: 0,
    });
    expect(state.rivalPresence).toBe("AWAY");
  });

  it("the monotone counters carry across a room change", () => {
    const stateInRoomA = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "RoomJoined",
        code: "ABCD",
        seat: 0,
      },
    );
    const stateWithPresence = duelState.applyServerMessage(stateInRoomA, {
      type: "OpponentPresence",
      presence: "AWAY",
    });
    // rejectionCount is driven to a value distinct from presenceCount's: two different counters
    // that both happen to read 1 could not catch an implementation that carries one over as the
    // other's value.
    const stateAfterFirstRejection = duelState.applyServerMessage(
      stateWithPresence,
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 3, minimum: 25 },
      },
    );
    const stateWithBothCounters = duelState.applyServerMessage(
      stateAfterFirstRejection,
      {
        type: "Rejected",
        rejection: { type: "AmountTooSmall", attempted: 4, minimum: 25 },
      },
    );
    expect(stateWithBothCounters.presenceCount).toBe(1);
    expect(stateWithBothCounters.rejectionCount).toBe(2);
    const state = duelState.applyServerMessage(stateWithBothCounters, {
      type: "RoomJoined",
      code: "EFGH",
      seat: 1,
    });
    expect(state.presenceCount).toBe(1);
    expect(state.rejectionCount).toBe(2);
  });

  it("records the act exactly as the server sent it", () => {
    // ADR-0109 §1's six acts. Each applied from a fresh initialState() in its own Events
    // frame, so a field the reducer dropped or rewrote fails on at least one of the six.
    const acts = [
      { type: "PlayerFolded", sequence: 40, seat: 0 } as const,
      { type: "PlayerChecked", sequence: 41, seat: 1 } as const,
      { type: "PlayerCalled", sequence: 42, seat: 0, to: 40 } as const,
      { type: "PlayerBet", sequence: 43, seat: 1, to: 60 } as const,
      { type: "PlayerRaised", sequence: 44, seat: 0, to: 120 } as const,
      { type: "PlayerAllIn", sequence: 45, seat: 1, to: 500 } as const,
    ];
    for (const act of acts) {
      const state = duelState.applyServerMessage(duelState.initialState(), {
        type: "Events",
        events: [act],
      });
      expect(state.lastAct).toEqual(act);
    }
  });

  it("a later act replaces an earlier one, at either seat", () => {
    const bet = { type: "PlayerBet", sequence: 50, seat: 1, to: 60 } as const;
    const call = {
      type: "PlayerCalled",
      sequence: 51,
      seat: 0,
      to: 60,
    } as const;
    const raise = {
      type: "PlayerRaised",
      sequence: 52,
      seat: 1,
      to: 180,
    } as const;
    const stateAfterBet = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: [bet] },
    );
    const stateAfterCall = duelState.applyServerMessage(stateAfterBet, {
      type: "Events",
      events: [call],
    });
    expect(stateAfterCall.lastAct).toEqual(call);
    expect(stateAfterCall.lastAct).not.toEqual(bet);
    const stateAfterRaise = duelState.applyServerMessage(stateAfterCall, {
      type: "Events",
      events: [raise],
    });
    expect(stateAfterRaise.lastAct).toEqual(raise);
  });

  it("the deal that opens a hand takes the mark off", () => {
    const bet = { type: "PlayerBet", sequence: 60, seat: 0, to: 40 } as const;
    const stateAfterBet = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: [bet] },
    );
    expect(stateAfterBet.lastAct).toEqual(bet);
    const handStarted = {
      type: "HandStarted",
      sequence: 61,
      handNumber: 2,
      buttonSeat: 1,
      smallBlind: 10,
      bigBlind: 20,
      stacks: [480, 520],
    } as const;
    const blindPosted = {
      type: "BlindPosted",
      sequence: 62,
      seat: 1,
      to: 10,
      isBigBlind: false,
    } as const;
    // A later frame carrying the deal, plus an unrelated event beside it, still takes the
    // mark off — the walk does not care what else the frame contains.
    const state = duelState.applyServerMessage(stateAfterBet, {
      type: "Events",
      events: [handStarted, blindPosted],
    });
    expect(state.lastAct).toBeNull();
  });

  it("the deal that opens a hand takes the mark off, in the order the frame sent it", () => {
    const bet = { type: "PlayerBet", sequence: 70, seat: 0, to: 40 } as const;
    const handStarted = {
      type: "HandStarted",
      sequence: 71,
      handNumber: 2,
      buttonSeat: 1,
      smallBlind: 10,
      bigBlind: 20,
      stacks: [460, 540],
    } as const;
    // Events[act, HandStarted]: the deal came last in the frame, so the mark clears.
    const dealLast = duelState.applyServerMessage(duelState.initialState(), {
      type: "Events",
      events: [bet, handStarted],
    });
    expect(dealLast.lastAct).toBeNull();
    // Events[HandStarted, act]: the act came last in the frame, so it stands. A reducer that
    // special-cases "the frame contains a HandStarted" instead of walking in order gets this
    // one wrong while passing the case above.
    const actLast = duelState.applyServerMessage(duelState.initialState(), {
      type: "Events",
      events: [handStarted, bet],
    });
    expect(actLast.lastAct).toEqual(bet);
  });

  it("stands through the award window and goes only when the next hand is painted", () => {
    const fold = { type: "PlayerFolded", sequence: 100, seat: 1 } as const;
    const stateWithFold = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: [fold] },
    );
    expect(stateWithFold.lastAct).toEqual(fold);

    // ADR-0102 §2: a hand-completing Snapshot with no StreetDealt in front of it lays out one
    // step — the fold case — so reveal is non-null and the award window is open.
    const handOneComplete = samplePlayerView({
      handNumber: 1,
      street: "COMPLETE",
      board: { cards: ["As", "7d", "2c", "Kh", "3s"] },
    });
    const stateAfterHandOneSnapshot = duelState.applyServerMessage(
      stateWithFold,
      { type: "Snapshot", view: handOneComplete },
    );
    expect(stateAfterHandOneSnapshot.reveal).not.toBeNull();
    // ADR-0109 §3: the fold's mark stands through ADR-0095 §4's award window.
    expect(stateAfterHandOneSnapshot.lastAct).toEqual(fold);

    const handStarted = {
      type: "HandStarted",
      sequence: 101,
      handNumber: 2,
      buttonSeat: 1,
      smallBlind: 10,
      bigBlind: 20,
      stacks: [480, 520],
    } as const;
    const stateWithHandTwoEvents = duelState.applyServerMessage(
      stateAfterHandOneSnapshot,
      { type: "Events", events: [handStarted] },
    );
    const handTwoView = samplePlayerView({ handNumber: 2, street: "PREFLOP" });
    const stateWithHandTwoSnapshot = duelState.applyServerMessage(
      stateWithHandTwoEvents,
      { type: "Snapshot", view: handTwoView },
    );
    // ADR-0102 §1: hand 2's Events (carrying HandStarted) and hand 2's Snapshot have both
    // arrived, but reveal's one step still stands, so ADR-0102 §1's queue has only queued them —
    // neither has been applied yet. A reducer that cleared lastAct on the arriving frame rather
    // than the painted one fails exactly this assertion.
    expect(stateWithHandTwoSnapshot.lastAct).toEqual(fold);

    const statePainted = duelState.advanceReveal(stateWithHandTwoSnapshot);
    expect(statePainted.reveal).toBeNull();
    expect(statePainted.lastAct).toBeNull();
  });

  it("a street's end, a deal, a snapshot, a presence and a rejection leave the mark standing", () => {
    const bet = { type: "PlayerBet", sequence: 110, seat: 1, to: 60 } as const;
    let state = duelState.applyServerMessage(duelState.initialState(), {
      type: "Events",
      events: [bet],
    });
    expect(state.lastAct).toEqual(bet);

    const bettingRoundEnded = {
      type: "BettingRoundEnded",
      sequence: 111,
      street: "PREFLOP",
    } as const;
    state = duelState.applyServerMessage(state, {
      type: "Events",
      events: [bettingRoundEnded],
    });
    expect(state.lastAct, "a street's end leaves the mark standing").toEqual(
      bet,
    );

    const streetDealt = {
      type: "StreetDealt",
      sequence: 112,
      street: "FLOP",
      cards: ["As", "7d", "2c"],
    } as const;
    state = duelState.applyServerMessage(state, {
      type: "Events",
      events: [streetDealt],
    });
    expect(state.lastAct, "a street's deal leaves the mark standing").toEqual(
      bet,
    );

    state = duelState.applyServerMessage(state, {
      type: "Snapshot",
      view: samplePlayerView({
        street: "FLOP",
        board: { cards: ["As", "7d", "2c"] },
      }),
    });
    expect(state.lastAct, "a snapshot leaves the mark standing").toEqual(bet);

    state = duelState.applyServerMessage(state, {
      type: "OpponentPresence",
      presence: "AWAY",
    });
    expect(state.lastAct, "a presence frame leaves the mark standing").toEqual(
      bet,
    );

    state = duelState.applyServerMessage(state, {
      type: "Rejected",
      rejection: { type: "AmountTooSmall", attempted: 5, minimum: 10 },
    });
    expect(state.lastAct, "a rejection leaves the mark standing").toEqual(bet);
  });

  it("the duel ending takes the mark off", () => {
    const allIn = {
      type: "PlayerAllIn",
      sequence: 120,
      seat: 0,
      to: 500,
    } as const;
    const stateWithAct = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "Events", events: [allIn] },
    );
    expect(stateWithAct.lastAct).toEqual(allIn);
    const state = duelState.applyServerMessage(stateWithAct, {
      type: "DuelFinished",
      outcome: {
        winner: 0,
        handsPlayed: 9,
        finalStacks: [1000, 0],
      },
    });
    expect(state.lastAct).toBeNull();
  });

  it("a resume rebuilds no mark", () => {
    const stateAfterJoin = duelState.applyServerMessage(
      duelState.initialState(),
      { type: "RoomJoined", code: "ABCD", seat: 0 },
    );
    // ADR-0102 §5: a resume's Snapshot has no Events in front of it.
    const stateAfterSnapshot = duelState.applyServerMessage(stateAfterJoin, {
      type: "Snapshot",
      view: samplePlayerView(),
    });
    expect(stateAfterSnapshot.lastAct).toBeNull();
    const legalActions: LegalActions = {
      seat: 0,
      allowed: ["CHECK", "BET"],
      callTo: 0,
      minBetTo: 10,
      minRaiseTo: 20,
      allInTo: 100,
    };
    const state = duelState.applyServerMessage(stateAfterSnapshot, {
      type: "YourTurn",
      handNumber: 1,
      actionSequence: 1,
      legalActions,
    });
    // ADR-0109 §Consequences: PlayerView carries no last-act field, so a refresh loses the mark
    // until the next act — this is the accepted cost, not a bug to work around.
    expect(state.lastAct).toBeNull();
  });

  it("starts with no turn clock", () => {
    expect(duelState.initialState().turnClock).toBeNull();
  });

  it("anchors the clock at the instant the frame arrived", () => {
    const turnClock: TurnClock = {
      type: "TurnClock",
      seat: 0,
      handNumber: 1,
      actionSequence: 1,
      turnRemainingMillis: 30_000,
      bankRemainingMillis: [10_000, 10_000],
    };
    const state = duelState.applyServerMessage(
      duelState.initialState(),
      turnClock,
      1_000,
    );
    expect(state.turnClock?.turnEndsAt).toBe(31_000);
  });

  it("a second arrival anchors at its own instant", () => {
    // Two inputs, because one cannot tell an anchor from a constant.
    const turnClock: TurnClock = {
      type: "TurnClock",
      seat: 0,
      handNumber: 1,
      actionSequence: 1,
      turnRemainingMillis: 30_000,
      bankRemainingMillis: [10_000, 10_000],
    };
    const state = duelState.applyServerMessage(
      duelState.initialState(),
      turnClock,
      5_500,
    );
    expect(state.turnClock?.turnEndsAt).toBe(35_500);
  });

  it("the expiry is the allowance plus that seat's bank", () => {
    const bankRemainingMillis = [12_000, 18_000];
    const seatZeroState = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "TurnClock",
        seat: 0,
        handNumber: 1,
        actionSequence: 1,
        turnRemainingMillis: 30_000,
        bankRemainingMillis,
      },
      1_000,
    );
    // turnEndsAt (31_000) plus seat 0's own bank (12_000).
    expect(seatZeroState.turnClock?.expiresAt).toBe(43_000);

    const seatOneState = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "TurnClock",
        seat: 1,
        handNumber: 1,
        actionSequence: 1,
        turnRemainingMillis: 30_000,
        bankRemainingMillis,
      },
      1_000,
    );
    // turnEndsAt (31_000) plus seat 1's own, different, bank (18_000).
    expect(seatOneState.turnClock?.expiresAt).toBe(49_000);
  });

  it("holds both banks the server stated", () => {
    const state = duelState.applyServerMessage(
      duelState.initialState(),
      {
        type: "TurnClock",
        seat: 1,
        handNumber: 4,
        actionSequence: 2,
        turnRemainingMillis: 20_000,
        bankRemainingMillis: [15_000, 9_000],
      },
      1_000,
    );
    expect(state.turnClock?.bankRemainingMillis).toEqual([15_000, 9_000]);
  });
});
