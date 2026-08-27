import { describe, it, expect, vi } from "vitest";
import { render, screen, cleanup, fireEvent } from "@testing-library/react";
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

  it("states the hand count and both final stacks", () => {
    render(<DuelResult outcome={anOutcome()} mySeat={0} />);

    expect(
      screen.getByText("17 hands · You 19,400 · Your rival 4,600"),
    ).toBeDefined();

    cleanup();
    render(
      <DuelResult
        outcome={anOutcome({ finalStacks: [10250, 13750] })}
        mySeat={0}
      />,
    );

    expect(
      screen.getByText("17 hands · You 10,250 · Your rival 13,750"),
    ).toBeDefined();
  });

  it("counts one hand in the singular", () => {
    render(<DuelResult outcome={anOutcome({ handsPlayed: 1 })} mySeat={0} />);

    expect(
      screen.getByText("1 hand · You 19,400 · Your rival 4,600"),
    ).toBeDefined();
    expect(screen.queryByText(/1 hands/)).toBeNull();
  });

  it("follows your seat when it says which stack is yours", () => {
    render(<DuelResult outcome={anOutcome()} mySeat={1} />);

    expect(
      screen.getByText("17 hands · Your rival 19,400 · You 4,600"),
    ).toBeDefined();

    cleanup();
    render(
      <DuelResult
        outcome={anOutcome({ finalStacks: [10250, 13750] })}
        mySeat={1}
      />,
    );

    expect(
      screen.getByText("17 hands · Your rival 10,250 · You 13,750"),
    ).toBeDefined();
  });

  it("drops the owner words when the client holds no seat", () => {
    render(<DuelResult outcome={anOutcome()} mySeat={null} />);

    expect(screen.getByText("17 hands · 19,400 · 4,600")).toBeDefined();
    expect(screen.queryByText(/You/)).toBeNull();
    expect(screen.queryByText(/Your rival/)).toBeNull();

    cleanup();
    render(
      <DuelResult
        outcome={anOutcome({ finalStacks: [10250, 13750] })}
        mySeat={null}
      />,
    );

    expect(screen.getByText("17 hands · 10,250 · 13,750")).toBeDefined();
    expect(screen.queryByText(/You/)).toBeNull();
    expect(screen.queryByText(/Your rival/)).toBeNull();
  });

  it("offers a way back to the lobby", () => {
    render(<DuelResult outcome={anOutcome()} mySeat={0} />);

    const back = screen.getByRole("link", { name: "Back to the lobby" });
    expect(back.getAttribute("href")).toBe("/");
    expect(back.className.split(" ")).toContain("border-hairline");
    expect(back.className.split(" ")).not.toContain("bg-accent-fill");
  });

  it("adds no rematch of its own", () => {
    const { container } = render(
      <DuelResult outcome={anOutcome()} mySeat={0} />,
    );

    expect(screen.queryByRole("button", { name: /rematch/i })).toBeNull();
    expect(screen.queryByRole("link", { name: /rematch/i })).toBeNull();
    expect(
      container
        .querySelector('[aria-label="the result"]')
        ?.textContent?.match(/rematch/i),
    ).toBeNull();
  });

  it("puts the rematch it is handed above the way back", () => {
    const { container } = render(
      <DuelResult
        outcome={anOutcome()}
        mySeat={0}
        rematch={<button type="button">Rematch</button>}
      />,
    );

    const button = screen.getByRole("button", { name: "Rematch" });
    const back = screen.getByRole("link", { name: "Back to the lobby" });
    const result = container.querySelector('[aria-label="the result"]');

    expect(button).toBeDefined();
    expect(result?.contains(button)).toBe(true);
    expect(
      button.compareDocumentPosition(back) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it("calls onLeave when the way back is taken", () => {
    const onLeave = vi.fn();
    render(<DuelResult outcome={anOutcome()} mySeat={0} onLeave={onLeave} />);

    const back = screen.getByRole("link", { name: "Back to the lobby" });
    const clickReturn = fireEvent.click(back);

    expect(onLeave).toHaveBeenCalledOnce();
    expect(clickReturn).toBe(true);
    expect(back.getAttribute("href")).toBe("/");
  });

  it("takes the way back with no onLeave to call", () => {
    render(<DuelResult outcome={anOutcome()} mySeat={0} />);

    const back = screen.getByRole("link", { name: "Back to the lobby" });

    expect(() => fireEvent.click(back)).not.toThrow();
    expect(back.getAttribute("href")).toBe("/");
  });

  it("adds no offer of its own", () => {
    const { container } = render(
      <DuelResult outcome={anOutcome()} mySeat={0} />,
    );

    expect(container.querySelector('[aria-label="the offer"]')).toBeNull();
    expect(
      container
        .querySelector('[aria-label="the result"]')
        ?.textContent?.match(/password/i),
    ).toBeNull();
  });

  it("puts the offer it is handed between the rematch and the way back", () => {
    const { container } = render(
      <DuelResult
        outcome={anOutcome()}
        mySeat={0}
        rematch={<button type="button">Rematch</button>}
        offer={<section aria-label="the offer">An offer</section>}
      />,
    );

    const rematch = screen.getByRole("button", { name: "Rematch" });
    const offer = container.querySelector(
      '[aria-label="the offer"]',
    ) as Element;
    const back = screen.getByRole("link", { name: "Back to the lobby" });

    expect(offer).toBeDefined();
    expect(
      rematch.compareDocumentPosition(offer) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(
      (offer as Node).compareDocumentPosition(back) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it("disables nothing it already carried when it carries an offer", () => {
    render(
      <DuelResult
        outcome={anOutcome()}
        mySeat={0}
        offer={<section aria-label="the offer">An offer</section>}
      />,
    );

    const rematch = screen.queryByRole("button", { name: /rematch/i });
    const back = screen.getByRole("link", { name: "Back to the lobby" });

    expect(rematch).toBeNull();
    expect(screen.getByText("+1 duel coin")).toBeDefined();
    expect(back.getAttribute("href")).toBe("/");
  });
});
