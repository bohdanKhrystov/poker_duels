import { describe, it, expect } from "vitest";
import { verdictOf, verdictHeadline, coinLine } from "./outcome-text";
import { anOutcome } from "./outcome-fixture";

describe("the verdict", () => {
  it("is a win when the winner is your seat", () => {
    expect(verdictOf(anOutcome({ winner: 1 }), 1)).toBe("win");
    expect(verdictOf(anOutcome({ winner: 0 }), 0)).toBe("win");
  });

  it("is a loss when the winner is the other seat", () => {
    expect(verdictOf(anOutcome({ winner: 1 }), 0)).toBe("loss");
    expect(verdictOf(anOutcome({ winner: 0 }), 1)).toBe("loss");
  });

  it("is a draw when the outcome names no winner", () => {
    expect(verdictOf(anOutcome({ winner: null }), 0)).toBe("draw");
    expect(verdictOf(anOutcome({ winner: null }), 1)).toBe("draw");
    expect(verdictOf(anOutcome({ winner: null }), null)).toBe("draw");
  });

  it("is unknown when the reader has no seat", () => {
    const verdict = verdictOf(anOutcome({ winner: 1 }), null);
    expect(verdict).toBe("unknown");
    expect(verdict).not.toBe("win");
    expect(verdict).not.toBe("loss");
  });

  it("names each verdict in the words the design uses", () => {
    expect(verdictHeadline("win")).toBe("Victory");
    expect(verdictHeadline("loss")).toBe("Defeat");
    expect(verdictHeadline("draw")).toBe("Draw");
    expect(verdictHeadline("unknown")).toBe("Duel over");
  });
});

describe("the coin line", () => {
  it("gives the winner the coin", () => {
    expect(coinLine("win")).toBe("+1 duel coin");
  });

  it("takes the loser's coin, in a true minus sign", () => {
    expect(coinLine("loss")).toBe("−1 duel coin");
    expect(coinLine("loss")).not.toContain("-");
  });

  it("moves no coin on a draw, or without a seat", () => {
    expect(coinLine("draw")).toBe(null);
    expect(coinLine("unknown")).toBe(null);
  });
});
