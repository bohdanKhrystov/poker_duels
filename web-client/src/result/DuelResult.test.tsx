import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { DuelResult } from "./DuelResult";
import { anOutcome } from "./outcome-fixture";

describe("the result screen", () => {
  it("declares a victory when the winner is your seat", () => {
    render(<DuelResult outcome={anOutcome({ winner: 1 })} mySeat={1} />);

    const heading = screen.getByRole("heading", { name: "Victory" });
    expect(heading.className.split(" ")).toContain("text-win");
    expect(screen.getByText("+1 duel coin")).toBeDefined();
  });

  it("declares a defeat when the winner is the other seat", () => {
    render(<DuelResult outcome={anOutcome({ winner: 1 })} mySeat={0} />);

    const heading = screen.getByRole("heading", { name: "Defeat" });
    expect(heading.className.split(" ")).toContain("text-loss");
    expect(screen.getByText("−1 duel coin")).toBeDefined();
  });

  it("declares a draw, and moves no coin", () => {
    const { container } = render(
      <DuelResult outcome={anOutcome({ winner: null })} mySeat={1} />,
    );

    const heading = screen.getByRole("heading", { name: "Draw" });
    expect(heading).toBeDefined();
    expect(screen.queryByText(/duel coin/)).toBeNull();
    expect(container.querySelector('[aria-hidden="true"]')).toBeNull();
  });

  it("says the duel is over when the client holds no seat", () => {
    render(<DuelResult outcome={anOutcome({ winner: 1 })} mySeat={null} />);

    const heading = screen.getByRole("heading", { name: "Duel over" });
    expect(heading).toBeDefined();
    expect(screen.queryByText(/duel coin/)).toBeNull();
  });
});
