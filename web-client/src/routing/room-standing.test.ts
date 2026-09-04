import { describe, it, expect } from "vitest";
import type { DuelOutcome } from "../protocol";
import type { DuelState } from "../store/duel-state";
import { initialState } from "../store/duel-state";
import type { Screen } from "./screen";
import { aView } from "../table/view-fixture";
import { roomStanding, rulingOn, spendsOnArrival } from "./room-standing";

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

describe("the ruling on an ask", () => {
  it("honours the first screen whatever the room is doing", () => {
    const standings: RoomStanding[] = [
      "unknown",
      "none",
      "waiting",
      "running",
      "finished",
    ];
    for (const standing of standings) {
      expect(rulingOn("first", standing)).toBe("honour");
    }
  });

  it("refuses every chosen screen while the duel is running", () => {
    const screens: Screen[] = [
      "duels",
      "leaderboard",
      "account",
      "sign-in",
      "verify",
      "reset",
    ];
    for (const screen of screens) {
      expect(rulingOn(screen, "running")).toBe("refuse");
    }
  });

  it("holds only the two mailed screens while the room is unknown", () => {
    expect(rulingOn("verify", "unknown")).toBe("hold");
    expect(rulingOn("reset", "unknown")).toBe("hold");
  });

  it("honours the other four chosen screens while the room is unknown", () => {
    const screens: Screen[] = ["duels", "leaderboard", "account", "sign-in"];
    for (const screen of screens) {
      expect(rulingOn(screen, "unknown")).toBe("honour");
    }
  });

  it("honours every chosen screen over a waiting or a finished room", () => {
    const screens: Screen[] = [
      "duels",
      "leaderboard",
      "account",
      "sign-in",
      "verify",
      "reset",
    ];
    const standings: RoomStanding[] = ["waiting", "finished"];
    for (const standing of standings) {
      for (const screen of screens) {
        expect(rulingOn(screen, standing)).toBe("honour");
      }
    }
  });

  it("names the two screens that spend a secret on arrival, and no others", () => {
    const spendingScreens: Screen[] = ["verify", "reset"];
    const nonSpendingScreens: Screen[] = [
      "first",
      "duels",
      "leaderboard",
      "account",
      "sign-in",
    ];

    for (const screen of spendingScreens) {
      expect(spendsOnArrival(screen)).toBe(true);
    }

    for (const screen of nonSpendingScreens) {
      expect(spendsOnArrival(screen)).toBe(false);
    }
  });
});

type RoomStanding = ReturnType<typeof roomStanding>;
