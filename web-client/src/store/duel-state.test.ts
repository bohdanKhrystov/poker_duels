import { describe, expect, it } from "vitest";
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
});
