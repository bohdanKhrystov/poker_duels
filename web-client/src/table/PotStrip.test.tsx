import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { PotStrip } from "./PotStrip";
import { aView, aSeat } from "./view-fixture";

describe("the pot strip", () => {
  it("writes the pot the view carries, grouped", () => {
    render(<PotStrip view={aView({ pot: 2450 })} />);

    expect(screen.getByText(/Pot 2,450/)).toBeTruthy();
  });

  it("writes the blinds and the hand number the view carries", () => {
    render(
      <PotStrip
        view={aView({ smallBlind: 75, bigBlind: 150, handNumber: 14 })}
      />,
    );

    expect(screen.getByText(/Blinds 75\/150 · Hand 14/)).toBeTruthy();
  });

  it("names the street the view names", () => {
    const streets = [
      { street: "PREFLOP" as const, name: "Preflop" },
      { street: "FLOP" as const, name: "Flop" },
      { street: "TURN" as const, name: "Turn" },
      { street: "RIVER" as const, name: "River" },
      { street: "SHOWDOWN" as const, name: "Showdown" },
      { street: "COMPLETE" as const, name: "Hand complete" },
    ];

    streets.forEach(({ street, name }) => {
      const { unmount } = render(<PotStrip view={aView({ street })} />);

      const streetRegex = new RegExp(`· ${name}$`);
      expect(screen.queryByText(streetRegex)).toBeTruthy();

      unmount();
    });
  });

  it("names the street the view names even when the board disagrees", () => {
    render(
      <PotStrip
        view={aView({ street: "TURN", board: { cards: ["As", "7d", "2c"] } })}
      />,
    );

    // Should match TURN even though board only has 3 cards (which would be FLOP)
    expect(screen.getByText(/· Turn$/)).toBeTruthy();
    expect(screen.queryByText(/· Flop$/)).toBeNull();
  });

  it("adds this street and never the whole hand, which would count a swept street twice", () => {
    render(
      <PotStrip
        view={aView({
          pot: 4850,
          seats: [
            aSeat({ committedThisHand: 10 }),
            aSeat({ index: 1, committedThisHand: 10 }),
          ],
        })}
      />,
    );

    // Should show 4,850 from view.pot, not 20 from summing seats' committedThisHand (which is from swept streets)
    expect(screen.getByText(/Pot 4,850/)).toBeTruthy();
  });

  it("opens the hand at the blinds, never at nothing", () => {
    render(
      <PotStrip
        view={aView({
          pot: 0,
          smallBlind: 50,
          bigBlind: 100,
          seats: [
            aSeat({ index: 0, committedThisStreet: 50 }),
            aSeat({ index: 1, committedThisStreet: 100 }),
          ],
        })}
      />,
    );

    expect(screen.getByText(/Pot 150/)).toBeTruthy();
    expect(screen.queryByText(/Pot 0/)).toBeNull();
  });

  it("adds what both seats have out this street to the collected pot", () => {
    render(
      <PotStrip
        view={aView({
          pot: 2450,
          seats: [
            aSeat({ index: 0, committedThisStreet: 125 }),
            aSeat({ index: 1, committedThisStreet: 825 }),
          ],
        })}
      />,
    );

    expect(screen.getByText(/Pot 3,400/)).toBeTruthy();
    expect(screen.queryByText(/Pot 2,450/)).toBeNull();
    expect(screen.queryByText(/Pot 2,575/)).toBeNull();
    expect(screen.queryByText(/Pot 3,275/)).toBeNull();
  });

  it("draws a pile beside the pot, and the figure still names the total", () => {
    const { container } = render(
      <PotStrip
        view={aView({
          pot: 2450,
          seats: [
            aSeat({ index: 0, committedThisStreet: 125 }),
            aSeat({ index: 1, committedThisStreet: 825 }),
          ],
        })}
      />,
    );

    expect(screen.getByText(/Pot 3,400/)).toBeTruthy();
    const piles = container.querySelectorAll(".chip-pile");
    expect(piles.length).toBe(1);
  });

  it("draws the same pile for a small pot and a large one", () => {
    const pots = [150, 13400];

    pots.forEach((pot) => {
      const { container, unmount } = render(
        <PotStrip view={aView({ pot })} />,
      );

      const discs = container.querySelectorAll(".chip-disc");
      expect(discs.length).toBe(3);

      unmount();
    });
  });

  it("draws no pile beside the award line", () => {
    const { container, rerender } = render(
      <PotStrip
        view={aView({
          street: "COMPLETE",
          pot: 4850,
          handNumber: 14,
          viewerSeat: 0,
        })}
        narration={[
          {
            type: "HandStarted",
            sequence: 1,
            handNumber: 14,
            buttonSeat: 0,
            smallBlind: 75,
            bigBlind: 150,
            stacks: [13400, 4550],
          },
          { type: "PotAwarded", sequence: 2, seat: 0, amount: 4850 },
        ]}
      />,
    );

    expect(screen.getByText("You win 4,850")).toBeTruthy();
    let piles = container.querySelectorAll(".chip-pile");
    expect(piles.length).toBe(0);

    rerender(
      <PotStrip
        view={aView({
          street: "COMPLETE",
          pot: 4850,
          handNumber: 14,
          viewerSeat: 0,
        })}
      />,
    );

    expect(screen.getByText(/Pot 4,850/)).toBeTruthy();
    piles = container.querySelectorAll(".chip-pile");
    expect(piles.length).toBe(1);
  });
});
