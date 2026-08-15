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

  it("takes the pot from the view and not from what the seats put in", () => {
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

    // Should show 4,850 from view.pot, not 20 from summing seats' committedThisHand
    expect(screen.getByText(/Pot 4,850/)).toBeTruthy();
  });
});
