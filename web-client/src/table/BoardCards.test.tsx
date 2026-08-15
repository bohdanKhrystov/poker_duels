import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BoardCards } from "./BoardCards";

describe("the board", () => {
  it("reserves five places before a card is dealt", () => {
    render(<BoardCards cards={[]} />);
    const images = screen.getAllByRole("img");
    expect(images).toHaveLength(5);
    expect(
      screen.getByRole("img", { name: "turn card, not yet dealt" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("img", { name: "river card, not yet dealt" }),
    ).toBeTruthy();
  });

  it("deals the flop into the first three places", () => {
    render(<BoardCards cards={["As", "7d", "2c"]} />);
    expect(screen.getByRole("img", { name: "ace of spades" })).toBeTruthy();
    expect(screen.getByRole("img", { name: "seven of diamonds" })).toBeTruthy();
    expect(screen.getByRole("img", { name: "two of clubs" })).toBeTruthy();
    expect(
      screen.getByRole("img", { name: "turn card, not yet dealt" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("img", { name: "river card, not yet dealt" }),
    ).toBeTruthy();
  });

  it("still draws five places on the river", () => {
    render(<BoardCards cards={["As", "7d", "2c", "Ks", "3h"]} />);
    const images = screen.getAllByRole("img");
    expect(images).toHaveLength(5);
    expect(screen.queryByRole("img", { name: /not yet dealt/ })).toBeNull();
  });

  it("draws exactly five places on every street", () => {
    // PREFLOP: no cards
    const { rerender } = render(<BoardCards cards={[]} />);
    let flexElement = document.querySelector(".flex");
    expect(flexElement).toBeTruthy();
    expect(flexElement!.childElementCount).toBe(5);

    // FLOP: three cards
    rerender(<BoardCards cards={["As", "7d", "2c"]} />);
    flexElement = document.querySelector(".flex");
    expect(flexElement).toBeTruthy();
    expect(flexElement!.childElementCount).toBe(5);

    // TURN: four cards
    rerender(<BoardCards cards={["As", "7d", "2c", "Ks"]} />);
    flexElement = document.querySelector(".flex");
    expect(flexElement).toBeTruthy();
    expect(flexElement!.childElementCount).toBe(5);

    // RIVER: five cards
    rerender(<BoardCards cards={["As", "7d", "2c", "Ks", "3h"]} />);
    flexElement = document.querySelector(".flex");
    expect(flexElement).toBeTruthy();
    expect(flexElement!.childElementCount).toBe(5);

    // A sixth card. Every case above renders five places under "exactly five"
    // and under "at least five" alike, so none of them can tell the two apart.
    // This one can: a board that widens to Math.max(5, cards.length) passes all
    // four streets above and fails here.
    rerender(<BoardCards cards={["As", "7d", "2c", "Ks", "3h", "9c"]} />);
    flexElement = document.querySelector(".flex");
    expect(flexElement).toBeTruthy();
    expect(flexElement!.childElementCount).toBe(5);
  });
});
