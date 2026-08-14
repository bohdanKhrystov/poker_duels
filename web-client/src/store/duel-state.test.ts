import { describe, expect, it } from "vitest";
import type { LegalActions } from "../protocol";
import * as duelState from "./duel-state";

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
});
