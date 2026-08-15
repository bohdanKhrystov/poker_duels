import { render, screen, within } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { DuelTable } from "./DuelTable";
import { aView, aSeat } from "./view-fixture";

/** The seat plate whose name is `name`, so a stack can be pinned to a seat. */
function plateFor(name: string): HTMLElement {
  const plate = screen.getByText(name).closest("div");
  if (plate === null) throw new Error(`no seat plate named ${name}`);
  return plate;
}

describe("the duel table", () => {
  it("seats you and your rival from the view's viewerSeat", () => {
    const view = aView({
      viewerSeat: 1,
      seats: [
        aSeat({ index: 0, stack: 4150 }),
        aSeat({ index: 1, stack: 13400 }),
      ],
    });

    render(<DuelTable view={view} />);

    expect(within(plateFor("You")).getByText("13,400")).toBeDefined();
    expect(within(plateFor("Your rival")).getByText("4,150")).toBeDefined();

    // The design is a column: rival above, board between, you below — and
    // finding each plate by name cannot see which came first, so inverting the
    // whole column passes the two assertions above. Assert the order itself.
    expect(
      plateFor("Your rival").compareDocumentPosition(plateFor("You")) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeGreaterThan(0);
  });

  it("gives the button to the seat the view names", () => {
    const view = aView({
      viewerSeat: 0,
      buttonSeat: 1,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    render(<DuelTable view={view} />);

    const rivalPlate = plateFor("Your rival");
    expect(within(rivalPlate).queryByLabelText("the button")).not.toBeNull();

    const yourPlate = plateFor("You");
    expect(within(yourPlate).queryByLabelText("the button")).toBeNull();
  });

  it("marks the seat to act and no other", () => {
    const view = aView({
      viewerSeat: 0,
      seatToAct: 1,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    render(<DuelTable view={view} />);

    expect(screen.getByText("Their turn")).toBeDefined();
    expect(screen.queryByText("Your turn")).toBeNull();
  });

  it("shows the pot and the board the view carries", () => {
    const view = aView({
      pot: 2450,
      board: {
        cards: ["As", "7d", "2c"],
      },
    });

    render(<DuelTable view={view} />);

    expect(screen.getByText(/Pot 2,450/)).toBeDefined();
    expect(screen.getByLabelText("ace of spades")).toBeDefined();
    expect(screen.getByLabelText("turn card, not yet dealt")).toBeDefined();
  });

  it("draws your two cards face up and your rival's face down", () => {
    const view = aView({
      viewerSeat: 0,
      seatToAct: 1,
      seats: [
        aSeat({ index: 0, holeCards: ["Ah", "Ks"] }),
        aSeat({ index: 1, holeCards: [] }),
      ],
    });

    render(<DuelTable view={view} />);

    const yourCard = screen.getByLabelText("ace of hearts");
    const rivalHand = screen.getByLabelText("your rival's hidden hand");

    expect(yourCard).toBeDefined();
    expect(screen.getByLabelText("king of spades")).toBeDefined();
    expect(rivalHand).toBeDefined();

    // Both hands sit toward the board: yours above your plate, your rival's
    // below theirs. Label queries cannot see that — moving each hand to the far
    // side of its plate passes every assertion above. The containment checks
    // keep these honest: `closest("div")` could otherwise return a wrapper that
    // holds the hand, and a descendant also counts as "following".
    expect(plateFor("You").contains(yourCard)).toBe(false);
    expect(plateFor("Your rival").contains(rivalHand)).toBe(false);
    expect(
      yourCard.compareDocumentPosition(plateFor("You")) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeGreaterThan(0);
    expect(
      plateFor("Your rival").compareDocumentPosition(rivalHand) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeGreaterThan(0);
  });

  it("turns your rival's cards face up when the view carries them", () => {
    const view = aView({
      seats: [
        aSeat({ index: 0, holeCards: [] }),
        aSeat({ index: 1, holeCards: ["7c", "7h"] }),
      ],
    });

    render(<DuelTable view={view} />);

    expect(screen.getByLabelText("seven of clubs")).toBeDefined();
    expect(screen.getByLabelText("seven of hearts")).toBeDefined();
    expect(screen.queryByLabelText("your rival's hidden hand")).toBeNull();
  });

  it("keeps a folded rival's hand two places wide", () => {
    const view = aView({
      seats: [
        aSeat({ index: 0, holeCards: [] }),
        aSeat({ index: 1, hasFolded: true, holeCards: [] }),
      ],
    });

    render(<DuelTable view={view} />);

    expect(screen.getByText("Folded")).toBeDefined();
    expect(screen.getByLabelText("your rival's hidden hand")).toBeDefined();
  });
});
