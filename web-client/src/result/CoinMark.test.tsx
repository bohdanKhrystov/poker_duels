import { describe, it, expect } from "vitest";
import { render } from "@testing-library/react";
import { CoinMark } from "./CoinMark";

describe("the coin mark", () => {
  it("draws the coin from the coin tokens", () => {
    const { container } = render(<CoinMark />);

    const mark = container.firstElementChild!;
    expect(mark.getAttribute("style")).toContain("var(--pd-coin-face)");
    expect(mark.className.split(" ")).toContain("rounded-pill");
  });

  it("is decorative, and names nothing", () => {
    const { container } = render(<CoinMark />);

    const mark = container.firstElementChild!;
    expect(mark.getAttribute("aria-hidden")).toBe("true");
    expect(mark.getAttribute("aria-label")).toBeNull();
    expect(mark.getAttribute("title")).toBeNull();
    expect(mark.textContent).toBe("");
  });
});
