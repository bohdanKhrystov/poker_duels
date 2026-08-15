import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { CardBack, CardFace, CardSlot } from "./PlayingCard";

describe("a face-up card", () => {
  it("names a face-up card in words", () => {
    render(<CardFace card="As" />);

    expect(screen.getByRole("img", { name: "ace of spades" })).toBeDefined();
  });

  it("prints the rank character and the suit glyph", () => {
    const { container } = render(<CardFace card="Td" />);

    expect(container.textContent).toBe("T♦︎♦︎");
  });

  it("paints a red suit with the suit-red token and a black one without it", () => {
    const { container: redContainer } = render(<CardFace card="Ah" />);
    const { container: blackContainer } = render(<CardFace card="Ks" />);

    const red = redContainer.querySelector("span")?.className.split(" ");
    const black = blackContainer.querySelector("span")?.className.split(" ");

    expect(red).toContain("text-suit-red");
    expect(black).toContain("text-suit-black");
    // "Without it" is half the claim. Carrying both classes leaves the colour
    // to whichever rule the stylesheet happens to define last, which paints
    // every suit alike — and a presence-only check cannot see it.
    expect(red).not.toContain("text-suit-black");
    expect(black).not.toContain("text-suit-red");
  });

  it("keeps a card it cannot read face down", () => {
    const { container } = render(<CardFace card="Zz" />);

    expect(
      screen.getByRole("img", { name: "an unreadable card" }),
    ).toBeDefined();
    expect(container.textContent).toBe("");
  });
});

describe("a card back and an undealt place", () => {
  it("shows a face-down card with no rank and no suit in it", () => {
    const { container } = render(<CardBack label="your rival's hidden hand" />);
    const element = container.querySelector("span");

    expect(container.textContent).toBe("");
    expect(element?.getAttribute("aria-label")).toBe(
      "your rival's hidden hand",
    );
    expect(element?.childElementCount).toBe(0);
    // A `title` speaks, and shows a tooltip. Checking text and the accessible
    // name leaves it as the one way a rank could still reach a player.
    expect(element?.getAttribute("title")).toBeNull();
  });

  it("hides the second card of a pair from the accessibility tree", () => {
    const { container } = render(<CardBack />);

    expect(screen.queryAllByRole("img")).toEqual([]);
    // Assert the attribute, not only the absent role: a bare <span> is not an
    // `img` either, so `queryAllByRole` alone passes on a card that was never
    // hidden at all. This is the assertion the test's name is about.
    expect(container.querySelector("span")?.getAttribute("aria-hidden")).toBe(
      "true",
    );
  });

  it("names an undealt board place", () => {
    render(<CardSlot label="river card, not yet dealt" />);

    expect(
      screen.getByRole("img", { name: "river card, not yet dealt" }),
    ).toBeDefined();
  });
});
