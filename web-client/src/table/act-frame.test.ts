import { describe, it, expect } from "vitest";
import { actFrame } from "./act-frame";
import { aTurn, aLegalActions } from "./turn-fixture";

describe("the act frame", () => {
  it("copies the turn's identity into the frame", () => {
    const turn = aTurn();
    const frame = actFrame(turn, "FOLD", 0);
    expect(frame.handNumber).toBe(14);
    expect(frame.actionSequence).toBe(27);

    // With different identity values, not fixture defaults
    const turn2 = aTurn({ handNumber: 99, actionSequence: 88 });
    const frame2 = actFrame(turn2, "FOLD", 0);
    expect(frame2.handNumber).toBe(99);
    expect(frame2.actionSequence).toBe(88);
  });

  it("takes the seat from the legal actions and nowhere else", () => {
    const turn = aTurn({ legalActions: aLegalActions({ seat: 1 }) });
    const frame = actFrame(turn, "FOLD", 0);
    expect(frame.action.seat).toBe(1);

    // With a different seat value, not the fixture default
    const turn2 = aTurn({ legalActions: aLegalActions({ seat: 5 }) });
    const frame2 = actFrame(turn2, "FOLD", 0);
    expect(frame2.action.seat).toBe(5);
  });

  it("builds each of the six actions the wire declares", () => {
    const built = (
      ["FOLD", "CHECK", "CALL", "BET", "RAISE", "ALL_IN"] as const
    ).map((type) => actFrame(aTurn(), type, 3250).action.type);
    expect(built).toEqual(["Fold", "Check", "Call", "Bet", "Raise", "AllIn"]);
  });

  it("sends a bet and a raise as a street total", () => {
    const bet = actFrame(aTurn(), "BET", 3250).action;
    const raise = actFrame(aTurn(), "RAISE", 3250).action;
    expect(bet).toEqual({ type: "Bet", seat: 0, to: 3250 });
    expect(raise).toEqual({ type: "Raise", seat: 0, to: 3250 });

    // With non-default seat
    const bet2 = actFrame(
      aTurn({ legalActions: aLegalActions({ seat: 1 }) }),
      "BET",
      5000,
    ).action;
    const raise2 = actFrame(
      aTurn({ legalActions: aLegalActions({ seat: 1 }) }),
      "RAISE",
      5000,
    ).action;
    expect(bet2).toEqual({ type: "Bet", seat: 1, to: 5000 });
    expect(raise2).toEqual({ type: "Raise", seat: 1, to: 5000 });
  });

  it("puts no amount on an action that carries none", () => {
    const allIn = actFrame(aTurn(), "ALL_IN", 3250).action;
    expect(allIn).toEqual({ type: "AllIn", seat: 0 });
  });
});
