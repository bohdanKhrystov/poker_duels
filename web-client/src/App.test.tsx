import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { App } from "./App";

describe("App", () => {
  it("renders the application heading", () => {
    render(<App />);
    expect(screen.getByRole("heading").textContent).toBe("Poker Duels");
  });

  it("gives the heading a token-derived class", () => {
    render(<App />);
    const heading = screen.getByRole("heading");
    expect(heading.className.split(" ")).toContain("text-title");
  });
});
