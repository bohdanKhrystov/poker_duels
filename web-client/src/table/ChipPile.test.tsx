import { describe, it, expect } from "vitest";
import { render } from "@testing-library/react";
import { ChipPile } from "./ChipPile";

describe("ChipPile", () => {
  it("draws three discs inside one pile", () => {
    const { container } = render(<ChipPile />);

    expect(container.querySelectorAll(".chip-pile")).toHaveLength(1);
    expect(container.querySelectorAll(".chip-disc")).toHaveLength(3);
  });

  it("says nothing to anybody", () => {
    const { container } = render(<ChipPile />);

    const root = container.querySelector("span");
    expect(root?.getAttribute("aria-hidden")).toBe("true");
    expect(container.querySelectorAll("[aria-label], [title]")).toHaveLength(0);
    expect(container.textContent).toBe("");
  });

  it("carries the flight class beside the pile class", () => {
    const { container } = render(<ChipPile />);

    const root = container.querySelector("span");
    expect(root?.tagName).toBe("SPAN");
    const classList = root?.className.split(" ") ?? [];
    expect(classList).toContain("chip-pile");
    expect(classList).toContain("chip-flight");
  });
});
