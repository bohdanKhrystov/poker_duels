import { describe, it, expect } from "vitest";
import { cardText } from "./card-text";

const RANKS = "AKQJT98765432";
const SUITS = "shdc";

const EVERY_CARD = [...RANKS].flatMap((rank) =>
  [...SUITS].map((suit) => `${rank}${suit}`),
);

describe("a card string", () => {
  it("splits into the rank character and the suit glyph", () => {
    expect(cardText("As")).toEqual({
      rank: "A",
      suit: "♠︎",
      isRed: false,
    });
    expect(cardText("Td")?.rank).toBe("T");
    expect(cardText("2c")?.suit).toBe("♣︎");
  });

  it("draws hearts and diamonds red and spades and clubs black", () => {
    expect(cardText("Ah")?.isRed).toBe(true);
    expect(cardText("Ad")?.isRed).toBe(true);
    expect(cardText("As")?.isRed).toBe(false);
    expect(cardText("Ac")?.isRed).toBe(false);
  });

  it("reads every one of the fifty-two cards the engine writes", () => {
    expect(EVERY_CARD).toHaveLength(52);
    const results = EVERY_CARD.map(cardText);
    expect(results).not.toContain(null);
    expect(new Set(EVERY_CARD).size).toBe(52);
  });

  it("refuses a string it cannot read", () => {
    expect(cardText("")).toBeNull();
    expect(cardText("A")).toBeNull();
    expect(cardText("Ass")).toBeNull();
    expect(cardText("1s")).toBeNull();
    expect(cardText("Ax")).toBeNull();
    expect(cardText("as")).toBeNull();
  });
});
