import { describe, it, expect } from "vitest";
import type { DuelOutcome } from "../protocol";
import type { DuelState } from "../store/duel-state";
import { initialState } from "../store/duel-state";
import { aView } from "../table/view-fixture";
import { roomStanding } from "./room-standing";

describe("the room's standing", () => {
  it("answers unknown before any frame while this tab is awaiting a room", () => {
    const state = initialState();
    expect(roomStanding(state, true)).toBe("unknown");
  });

  it("answers none when this tab is awaiting no room", () => {
    const state = initialState();
    expect(roomStanding(state, false)).toBe("none");
  });

  it("answers none once the server refused the room this tab awaited", () => {
    const state = { ...initialState(), refusal: "UNKNOWN_ROOM" as const };
    expect(roomStanding(state, true)).toBe("none");
  });

  it("answers waiting on a RoomJoined alone", () => {
    const state = { ...initialState(), roomCode: "ABCDEFGH" };
    expect(roomStanding(state, true)).toBe("waiting");
    expect(roomStanding(state, false)).toBe("waiting");
  });

  it("answers running once a Snapshot stands", () => {
    const state = { ...initialState(), roomCode: "ABCDEFGH", view: aView() };
    expect(roomStanding(state, true)).toBe("running");
  });

  it("answers finished while the view the duel ended on is still standing", () => {
    const outcome: DuelOutcome = {
      winner: 0,
      handsPlayed: 3,
      finalStacks: [1000, 0],
    };
    const state = {
      ...initialState(),
      roomCode: "ABCDEFGH",
      view: aView(),
      outcome,
    };
    expect(roomStanding(state, true)).toBe("finished");
  });

  it("answers running while a reveal is still painting", () => {
    const state: DuelState = {
      ...initialState(),
      roomCode: "ABCDEFGH",
      view: aView(),
      outcome: null,
      reveal: {
        steps: [{ board: [], street: "PREFLOP" }],
        queued: [
          {
            message: {
              type: "DuelFinished",
              outcome: {
                winner: 0,
                handsPlayed: 1,
                finalStacks: [1000, 0],
              },
            },
            arrivedAt: 0,
          },
        ],
      },
    };
    expect(roomStanding(state, true)).toBe("running");
  });
});
