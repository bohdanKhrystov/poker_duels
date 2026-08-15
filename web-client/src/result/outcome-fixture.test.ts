import { describe, it, expect } from "vitest";
import { anOutcome } from "./outcome-fixture";

describe("the outcome fixture", () => {
  it("carries every field DuelOutcome declares", () => {
    expect(Object.keys(anOutcome()).sort()).toEqual([
      "finalStacks",
      "handsPlayed",
      "winner",
    ]);
  });

  it("lets a test bend any field it names", () => {
    expect(anOutcome({ winner: null }).winner).toBe(null);
    expect(anOutcome({ handsPlayed: 3 }).handsPlayed).toBe(3);
    expect(anOutcome({ winner: 1, handsPlayed: 3 }).finalStacks).toEqual([
      19400, 4600,
    ]);
    expect(anOutcome({ finalStacks: [10000, 10000] }).finalStacks).toEqual([
      10000, 10000,
    ]);
    expect(anOutcome({ winner: null, handsPlayed: 50 }).winner).toBe(null);
  });

  it("keeps its numbers independent of one another, and of the coin", () => {
    const outcome = anOutcome();
    // The coin's 1 is in the list: it is the one figure ADR-0014 fixes and the
    // screen prints, so a derived total that happened to equal it would be
    // invisible to TASK-030808's guard.
    const money = [outcome.handsPlayed, ...outcome.finalStacks, 1];
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
