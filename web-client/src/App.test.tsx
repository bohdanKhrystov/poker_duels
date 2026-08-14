import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { App } from "./App";
import { DuelProvider } from "./store/duel-provider";
import { createDuelStore } from "./store/duel-store";

function renderApp(): void {
  render(
    <DuelProvider store={createDuelStore()} send={vi.fn()}>
      <App />
    </DuelProvider>,
  );
}

describe("App", () => {
  it("renders the application heading", () => {
    renderApp();
    expect(screen.getByRole("heading").textContent).toBe("Poker Duels");
  });

  it("gives the heading a token-derived class", () => {
    renderApp();
    const heading = screen.getByRole("heading");
    expect(heading.className.split(" ")).toContain("text-title");
  });

  it("renders the lobby beneath the heading", () => {
    renderApp();
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();
  });
});
