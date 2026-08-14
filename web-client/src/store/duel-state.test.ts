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
      outcome: null,
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
});
