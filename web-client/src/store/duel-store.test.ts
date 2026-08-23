import { describe, it, expect, vi } from "vitest";
import { createDuelStore } from "./duel-store";
import { initialState } from "./duel-state";

const WELCOME = {
  type: "Welcome",
  playerId: "p-1",
  deviceId: "d-1",
  protocolVersion: 2,
} as const;
const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;

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
});
