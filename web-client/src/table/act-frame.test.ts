import { describe, it, expect } from "vitest";
import { actFrame } from "./act-frame";
import { aTurn, aLegalActions } from "./turn-fixture";

describe("the act frame", () => {
  it("copies the turn's identity into the frame", () => {
    const turn = aTurn();
    const frame = actFrame(turn, "FOLD", 0);
    expect(frame.handNumber).toBe(14);
    expect(frame.actionSequence).toBe(27);
  });

  it("takes the seat from the legal actions and nowhere else", () => {
    const turn = aTurn({ legalActions: aLegalActions({ seat: 1 }) });
    const frame = actFrame(turn, "FOLD", 0);
    expect(frame.action.seat).toBe(1);
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
  });

  it("puts no amount on an action that carries none", () => {
    const allIn = actFrame(aTurn(), "ALL_IN", 3250).action;
    expect(allIn).toEqual({ type: "AllIn", seat: 0 });
  });
});
