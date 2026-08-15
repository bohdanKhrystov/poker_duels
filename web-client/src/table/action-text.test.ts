import { describe, it, expect } from "vitest";
import { actionText, actionVerb } from "./action-text";
import { aLegalActions } from "./turn-fixture";

describe("the action text", () => {
  it("names each of the six actions the wire declares", () => {
    const named = ["FOLD", "CHECK", "CALL", "BET", "RAISE", "ALL_IN"] as const;
    expect(named.map(actionVerb)).toEqual([
      "Fold",
      "Check",
      "Call",
      "Bet",
      "Raise to",
      "All in",
    ]);
  });

  it("prices a call from the server's callTo", () => {
    expect(actionText("CALL", aLegalActions(), 9999)).toEqual({
      verb: "Call",
      amount: 400,
    });
    expect(actionText("CALL", aLegalActions({ callTo: 925 }), 9999)).toEqual({
      verb: "Call",
      amount: 925,
    });
  });

  it("prices an all-in from the server's allInTo", () => {
    expect(actionText("ALL_IN", aLegalActions(), 9999)).toEqual({
      verb: "All in",
      amount: 13400,
    });
    expect(
      actionText("ALL_IN", aLegalActions({ allInTo: 8500 }), 9999),
    ).toEqual({
      verb: "All in",
      amount: 8500,
    });
  });

  it("prices a bet and a raise from the total the player dialled in", () => {
    expect(actionText("BET", aLegalActions(), 3250)).toEqual({
      verb: "Bet",
      amount: 3250,
    });
    expect(actionText("RAISE", aLegalActions(), 3250)).toEqual({
      verb: "Raise to",
      amount: 3250,
    });
    expect(actionText("BET", aLegalActions(), 5000)).toEqual({
      verb: "Bet",
      amount: 5000,
    });
    expect(actionText("RAISE", aLegalActions(), 5000)).toEqual({
      verb: "Raise to",
      amount: 5000,
    });
  });

  it("puts no figure on a fold or a check", () => {
    expect(actionText("FOLD", aLegalActions(), 9999)).toEqual({
      verb: "Fold",
      amount: null,
    });
    expect(actionText("CHECK", aLegalActions(), 9999)).toEqual({
      verb: "Check",
      amount: null,
    });
  });
});
