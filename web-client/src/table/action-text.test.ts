import { describe, it, expect } from "vitest";
import { actionText, actionVerb, lastActText } from "./action-text";
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

  it("says Fold for a fold, bare", () => {
    expect(lastActText({ type: "PlayerFolded", sequence: 3, seat: 1 })).toEqual(
      {
        verb: "Fold",
        amount: null,
      },
    );
  });

  it("says Check for a check, bare", () => {
    expect(
      lastActText({ type: "PlayerChecked", sequence: 3, seat: 1 }),
    ).toEqual({
      verb: "Check",
      amount: null,
    });
  });

  it("says Call with the call's own total", () => {
    expect(
      lastActText({ type: "PlayerCalled", sequence: 5, seat: 0, to: 400 }),
    ).toEqual({
      verb: "Call",
      amount: 400,
    });
    expect(
      lastActText({ type: "PlayerCalled", sequence: 6, seat: 1, to: 925 }),
    ).toEqual({
      verb: "Call",
      amount: 925,
    });
  });

  it("says Bet with the bet's own total", () => {
    expect(
      lastActText({ type: "PlayerBet", sequence: 7, seat: 0, to: 800 }),
    ).toEqual({
      verb: "Bet",
      amount: 800,
    });
    expect(
      lastActText({ type: "PlayerBet", sequence: 8, seat: 1, to: 3250 }),
    ).toEqual({
      verb: "Bet",
      amount: 3250,
    });
  });

  it("says Raise to with the raise's own total", () => {
    expect(
      lastActText({ type: "PlayerRaised", sequence: 9, seat: 0, to: 1200 }),
    ).toEqual({
      verb: "Raise to",
      amount: 1200,
    });
    expect(
      lastActText({ type: "PlayerRaised", sequence: 10, seat: 1, to: 4750 }),
    ).toEqual({
      verb: "Raise to",
      amount: 4750,
    });
  });

  it("says All in with the all-in's own total", () => {
    expect(
      lastActText({ type: "PlayerAllIn", sequence: 11, seat: 0, to: 13400 }),
    ).toEqual({
      verb: "All in",
      amount: 13400,
    });
    expect(
      lastActText({ type: "PlayerAllIn", sequence: 12, seat: 1, to: 500 }),
    ).toEqual({
      verb: "All in",
      amount: 500,
    });
  });
});
