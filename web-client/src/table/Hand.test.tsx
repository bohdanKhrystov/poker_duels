import { render } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { Hand } from "./Hand";

describe("a hand", () => {
  it("draws two faces when the view carries two cards", () => {
    const { container, queryByRole } = render(
      <Hand cards={["Ah", "Ks"]} hiddenLabel="your rival's hidden hand" />,
    );

    const faces = container.querySelectorAll('[role="img"]');
    expect(faces.length).toBe(2);

    const aceOfHearts = queryByRole("img", { name: /ace of hearts/i });
    expect(aceOfHearts).not.toBeNull();

    const kingOfSpades = queryByRole("img", { name: /king of spades/i });
    expect(kingOfSpades).not.toBeNull();

    const hiddenHand = queryByRole("img", { name: "your rival's hidden hand" });
    expect(hiddenHand).toBeNull();
  });

  it("draws two backs when the view carries no card", () => {
    const { container, queryByRole } = render(
      <Hand cards={[]} hiddenLabel="your rival's hidden hand" />,
    );

    const hiddenHand = queryByRole("img", { name: "your rival's hidden hand" });
    expect(hiddenHand).not.toBeNull();

    expect(container.childElementCount).toBe(2);
    expect(container.textContent).toBe("");
  });

  it("is two places wide whatever the view carries", () => {
    // The fourth case is the one that separates "always two" from "at least
    // two": [], one card and two cards all render two places under either rule.
    // A hand that widens breaks the row's layout exactly as badly as one that
    // narrows, and the title says "whatever the view carries".
    const testCases = [[], ["Ah"], ["Ah", "Ks"], ["Ah", "Ks", "2c"]];

    testCases.forEach((cards) => {
      const { container } = render(
        <Hand cards={cards} hiddenLabel="your rival's hidden hand" />,
      );

      expect(container.childElementCount).toBe(2);
    });
  });
});
