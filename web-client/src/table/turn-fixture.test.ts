import { describe, it, expect } from "vitest";
import { aLegalActions, aTurn } from "./turn-fixture";

describe("the turn fixture", () => {
  it("carries every field LegalActions declares", () => {
    expect(Object.keys(aLegalActions()).sort()).toEqual([
      "allInTo",
      "allowed",
      "callTo",
      "minBetTo",
      "minRaiseTo",
      "seat",
    ]);
  });

  it("carries every field a pending turn declares", () => {
    expect(Object.keys(aTurn()).sort()).toEqual([
      "actionSequence",
      "handNumber",
      "legalActions",
    ]);
  });

  it("lets a test bend any field it names", () => {
    expect(aLegalActions({ seat: 1 }).seat).toBe(1);
    expect(aLegalActions({ callTo: 500 }).callTo).toBe(500);
    expect(aLegalActions({ seat: 1, callTo: 500 }).minBetTo).toBe(175);
    expect(aTurn({ handNumber: 20 }).handNumber).toBe(20);
    expect(aTurn({ actionSequence: 50 }).actionSequence).toBe(50);
    expect(
      aTurn({ handNumber: 20, actionSequence: 50 }).legalActions.seat,
    ).toBe(0);
  });

  it("keeps its amounts independent of one another", () => {
    const actions = aLegalActions();
    const money = [
      actions.callTo,
      actions.minBetTo,
      actions.minRaiseTo,
      actions.allInTo,
      aTurn().handNumber,
      aTurn().actionSequence,
    ];
    for (const a of money) {
      expect(money).not.toContain(a * 2);
      for (const b of money) {
        if (a === b) continue;
        expect(money).not.toContain(a + b);
        expect(money).not.toContain(Math.abs(a - b));
      }
    }
  });
});
