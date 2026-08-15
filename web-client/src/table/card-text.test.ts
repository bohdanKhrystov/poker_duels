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
      label: "ace of spades",
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

describe("a card's spoken name", () => {
  it("names a card the way the design says it aloud", () => {
    expect(cardText("As")?.label).toBe("ace of spades");
    expect(cardText("7d")?.label).toBe("seven of diamonds");
    expect(cardText("2c")?.label).toBe("two of clubs");
    expect(cardText("Jh")?.label).toBe("jack of hearts");
  });

  it("names the picture ranks and the ten in words", () => {
    expect(cardText("Ks")?.label).toBe("king of spades");
    expect(cardText("Qc")?.label).toBe("queen of clubs");
    expect(cardText("Td")?.label).toBe("ten of diamonds");
  });

  it("gives all fifty-two cards fifty-two different names", () => {
    const labels = EVERY_CARD.map((card) => cardText(card)?.label);
    expect(new Set(labels).size).toBe(52);
  });

  it("attaches no number to a card", () => {
    // Every card, not a sample: a numeric field added on a condition the sample
    // happens not to meet — a pip value on the number ranks — is exactly the
    // change this test exists to stop, and one ace cannot see it.
    for (const name of EVERY_CARD) {
      const card = cardText(name) ?? {};
      expect(Object.keys(card).sort()).toEqual([
        "isRed",
        "label",
        "rank",
        "suit",
      ]);
      for (const value of Object.values(card)) {
        expect(typeof value).not.toBe("number");
      }
    }
  });
});
