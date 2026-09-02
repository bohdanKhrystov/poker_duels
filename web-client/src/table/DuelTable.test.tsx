import { render, screen, within } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import type { GameEvent } from "../protocol";
import {
  advanceReveal,
  applyServerMessage,
  initialState,
} from "../store/duel-state";
import { DuelTable } from "./DuelTable";
import { aView, aSeat } from "./view-fixture";

/** The seat plate whose name is `name`, so a stack can be pinned to a seat. */
function plateFor(name: string): HTMLElement {
  const plate = screen.getByText(name).closest("div");
  if (plate === null) throw new Error(`no seat plate named ${name}`);
  return plate;
}

/** A `HandStarted` opening `handNumber`, with a seed unrelated to any award. */
const started = (handNumber: number): GameEvent => ({
  type: "HandStarted",
  sequence: handNumber * 10,
  handNumber,
  buttonSeat: 0,
  smallBlind: 25,
  bigBlind: 50,
  stacks: [1500, 1500],
});

/** A `PotAwarded` of `amount` to `seat`. */
const awarded = (seat: number, amount: number): GameEvent => ({
  type: "PotAwarded",
  sequence: 99,
  seat,
  amount,
});

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

  it("marks the seat the view says is to act, at either seat", () => {
    // Both directions in one test: a mark hard-coded to a seat index would
    // pass the first half below and fail the second.
    const heroToAct = aView({
      viewerSeat: 0,
      seatToAct: 0,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    const { container, rerender } = render(<DuelTable view={heroToAct} />);

    expect(container.querySelectorAll(".acting-mark")).toHaveLength(1);
    const heroMark = container.querySelector(".acting-mark");
    expect(plateFor("You").contains(heroMark)).toBe(true);
    expect(plateFor("Your rival").contains(heroMark)).toBe(false);

    const rivalToAct = aView({
      viewerSeat: 0,
      seatToAct: 1,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    rerender(<DuelTable view={rivalToAct} />);

    expect(container.querySelectorAll(".acting-mark")).toHaveLength(1);
    const rivalMark = container.querySelector(".acting-mark");
    expect(plateFor("Your rival").contains(rivalMark)).toBe(true);
    expect(plateFor("You").contains(rivalMark)).toBe(false);
  });

  it("marks no seat when the view names none", () => {
    // `seatToAct` is nullable on the wire (`protocol.gen.ts:290`): a mark that
    // appeared whenever a plate rendered would be the client inventing a turn.
    const view = aView({
      seatToAct: null,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    const { container } = render(<DuelTable view={view} />);

    expect(container.querySelectorAll(".acting-mark")).toHaveLength(0);
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

  it("states what your rival has committed on this street", () => {
    const view = aView({
      seats: [
        aSeat({ index: 0 }),
        aSeat({ index: 1, committedThisStreet: 400 }),
      ],
    });

    render(<DuelTable view={view} />);

    expect(screen.getByText(/committed/)).toBeDefined();
    expect(screen.getByText("400")).toBeDefined();
  });

  it("leaves the committed line empty and present when nothing is out", () => {
    const view = aView({
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    const { container } = render(<DuelTable view={view} />);

    const commitLine = container.querySelector("p");
    expect(commitLine).not.toBeNull();
    expect(commitLine?.textContent).toBe("");
  });

  it("puts the presence on the rival, from either seat", () => {
    // Test case 1: viewerSeat is 0, rival is at seat 1
    const view0 = aView({
      viewerSeat: 0,
      seatToAct: 1,
      seats: [
        aSeat({ index: 0, stack: 3500 }),
        aSeat({ index: 1, stack: 8200 }),
      ],
    });

    const { rerender } = render(
      <DuelTable view={view0} rivalPresence="AWAY" />,
    );

    expect(screen.getByText("Away")).toBeDefined();
    expect(within(plateFor("Your rival")).getByText("Away")).toBeDefined();
    expect(within(plateFor("Your rival")).getByText("8,200")).toBeDefined();
    expect(screen.queryByText("Their turn")).toBeNull();

    // Test case 2: viewerSeat is 1, rival is at seat 0
    const view1 = aView({
      viewerSeat: 1,
      seatToAct: 0,
      seats: [
        aSeat({ index: 0, stack: 3500 }),
        aSeat({ index: 1, stack: 8200 }),
      ],
    });

    rerender(<DuelTable view={view1} rivalPresence="AWAY" />);

    expect(screen.getByText("Away")).toBeDefined();
    expect(within(plateFor("Your rival")).getByText("Away")).toBeDefined();
    expect(within(plateFor("Your rival")).getByText("3,500")).toBeDefined();
    expect(screen.queryByText("Their turn")).toBeNull();
  });

  it("puts no presence on your own plate, from either seat", () => {
    // Test case 1: viewerSeat is 0, seatToAct is 0 (viewer's seat)
    const view0 = aView({
      viewerSeat: 0,
      seatToAct: 0,
      seats: [
        aSeat({ index: 0, stack: 3500 }),
        aSeat({ index: 1, stack: 8200 }),
      ],
    });

    const { rerender } = render(
      <DuelTable view={view0} rivalPresence="ABSENT" />,
    );

    expect(within(plateFor("You")).getByText("Your turn")).toBeDefined();
    expect(within(plateFor("You")).getByText("3,500")).toBeDefined();
    const timedOutElements = screen.queryAllByText("Timed out");
    expect(timedOutElements).toHaveLength(1);
    expect(within(plateFor("Your rival")).getByText("Timed out")).toBeDefined();

    // Test case 2: viewerSeat is 1, seatToAct is 1 (viewer's seat)
    const view1 = aView({
      viewerSeat: 1,
      seatToAct: 1,
      seats: [
        aSeat({ index: 0, stack: 3500 }),
        aSeat({ index: 1, stack: 8200 }),
      ],
    });

    rerender(<DuelTable view={view1} rivalPresence="ABSENT" />);

    expect(within(plateFor("You")).getByText("Your turn")).toBeDefined();
    expect(within(plateFor("You")).getByText("8,200")).toBeDefined();
    const timedOutElements2 = screen.queryAllByText("Timed out");
    expect(timedOutElements2).toHaveLength(1);
    expect(within(plateFor("Your rival")).getByText("Timed out")).toBeDefined();
  });

  it("states the viewer's own win in place of the pot line", () => {
    const view = aView({
      viewerSeat: 0,
      handNumber: 3,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [started(3), awarded(0, 4850)];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("You win 4,850")).toBeDefined();
    expect(screen.queryByText(/Pot/)).toBeNull();
  });

  it("names the rival when the rival took the pot", () => {
    const view = aView({
      viewerSeat: 1,
      handNumber: 3,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [started(3), awarded(0, 4850)];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("Your rival wins 4,850")).toBeDefined();
    expect(screen.queryByText(/You win/)).toBeNull();
  });

  it("states only the viewer's share of a split pot", () => {
    const view = aView({
      viewerSeat: 1,
      handNumber: 3,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [
      started(3),
      awarded(0, 2425),
      awarded(1, 2426),
    ];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("Split pot — you win 2,426")).toBeDefined();
    expect(screen.queryByText(/2,425/)).toBeNull();
  });

  it("leaves the pot line alone while the hand is still being played", () => {
    const view = aView({
      handNumber: 3,
      street: "TURN",
      pot: 5675,
    });
    const narration: GameEvent[] = [started(3), awarded(0, 4850)];

    const { container } = render(
      <DuelTable view={view} narration={narration} />,
    );

    expect(screen.getByText(/Pot 5,675/)).toBeDefined();
    expect(container.innerHTML).not.toMatch(/win/i);
  });

  it("leaves the pot line alone when this client never saw the award", () => {
    const view = aView({
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [];

    const { container } = render(
      <DuelTable view={view} narration={narration} />,
    );

    expect(screen.getByText(/Pot 0/)).toBeDefined();
    expect(container.innerHTML).not.toMatch(/win/i);
  });

  it("reads the ended hand's award and not an earlier hand's", () => {
    const view = aView({
      viewerSeat: 0,
      handNumber: 2,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [
      started(1),
      awarded(0, 1200),
      started(2),
      awarded(0, 4850),
    ];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("You win 4,850")).toBeDefined();
    expect(screen.queryByText(/1,200/)).toBeNull();
  });

  it("reads this duel's award and not the previous duel's", () => {
    const view = aView({
      viewerSeat: 0,
      handNumber: 1,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [
      started(1),
      awarded(0, 100),
      awarded(1, 100),
      started(2),
      awarded(1, 200),
      started(1),
      awarded(0, 200),
    ];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("You win 200")).toBeDefined();
    expect(screen.queryByText(/Split pot/)).toBeNull();
  });

  it("names this duel's winner even once the next hand has started", () => {
    const view = aView({
      viewerSeat: 0,
      handNumber: 2,
      street: "COMPLETE",
      pot: 0,
    });
    const narration: GameEvent[] = [
      started(2),
      awarded(1, 9800),
      started(1),
      awarded(0, 200),
      started(2),
      awarded(0, 200),
      started(3),
      awarded(1, 5000),
    ];

    render(<DuelTable view={view} narration={narration} />);

    expect(screen.getByText("You win 200")).toBeDefined();
    expect(screen.queryByText(/Your rival wins/)).toBeNull();
    expect(screen.queryByText(/9,800/)).toBeNull();
    expect(screen.queryByText(/5,000/)).toBeNull();
  });

  it("paints the snapshot's own cards when the StreetDealt carried different ones", () => {
    const view = aView({
      street: "COMPLETE",
      board: { cards: ["As", "7d", "2c"] },
    });
    const narration: GameEvent[] = [
      started(1),
      {
        type: "StreetDealt",
        sequence: 5,
        street: "FLOP",
        // Deliberately not the snapshot's own cards: if the board were ever read from the
        // event instead of `view.board.cards`, this is what would show (ADR-0102 §3).
        cards: ["9h", "9c", "9d"],
      },
    ];

    render(
      <DuelTable
        view={view}
        narration={narration}
        revealStep={{ board: view.board.cards, street: "FLOP" }}
      />,
    );

    expect(screen.getByLabelText("ace of spades")).toBeDefined();
    expect(screen.getByLabelText("seven of diamonds")).toBeDefined();
    expect(screen.getByLabelText("two of clubs")).toBeDefined();
    expect(screen.queryByLabelText("nine of hearts")).toBeNull();
    expect(screen.queryByLabelText("nine of clubs")).toBeNull();
    expect(screen.queryByLabelText("nine of diamonds")).toBeNull();
  });

  it("a runout paints three cards then four then five, naming Flop, Turn and River", () => {
    const board = ["As", "7d", "2c", "Kh", "3s"];
    const withEvents = applyServerMessage(initialState(), {
      type: "Events",
      events: [
        {
          type: "StreetDealt",
          sequence: 10,
          street: "FLOP",
          cards: ["As", "7d", "2c"],
        },
        { type: "StreetDealt", sequence: 11, street: "TURN", cards: ["Kh"] },
        { type: "StreetDealt", sequence: 12, street: "RIVER", cards: ["3s"] },
      ],
    });
    const afterSnapshot = applyServerMessage(withEvents, {
      type: "Snapshot",
      view: aView({ street: "COMPLETE", board: { cards: board } }),
    });
    const view = afterSnapshot.view;
    if (view === null) throw new Error("expected a view");

    const { rerender } = render(
      <DuelTable
        view={view}
        revealStep={afterSnapshot.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.getByLabelText("ace of spades")).toBeDefined();
    expect(screen.getByLabelText("seven of diamonds")).toBeDefined();
    expect(screen.getByLabelText("two of clubs")).toBeDefined();
    expect(screen.getByLabelText("turn card, not yet dealt")).toBeDefined();
    expect(screen.queryByLabelText("king of hearts")).toBeNull();
    expect(screen.getByText(/· Flop$/)).toBeDefined();

    const afterFlopStep = advanceReveal(afterSnapshot);
    rerender(
      <DuelTable
        view={view}
        revealStep={afterFlopStep.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.getByLabelText("king of hearts")).toBeDefined();
    expect(screen.getByLabelText("river card, not yet dealt")).toBeDefined();
    expect(screen.queryByLabelText("three of spades")).toBeNull();
    expect(screen.getByText(/· Turn$/)).toBeDefined();

    const afterTurnStep = advanceReveal(afterFlopStep);
    rerender(
      <DuelTable
        view={view}
        revealStep={afterTurnStep.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.getByLabelText("three of spades")).toBeDefined();
    expect(screen.queryByLabelText("river card, not yet dealt")).toBeNull();
    expect(screen.getByText(/· River$/)).toBeDefined();
  });

  it("holds the award line back until the last step", () => {
    const board = ["As", "7d", "2c", "Kh", "3s"];
    const withEvents = applyServerMessage(initialState(), {
      type: "Events",
      events: [
        {
          type: "StreetDealt",
          sequence: 10,
          street: "FLOP",
          cards: ["As", "7d", "2c"],
        },
        { type: "StreetDealt", sequence: 11, street: "TURN", cards: ["Kh"] },
        { type: "StreetDealt", sequence: 12, street: "RIVER", cards: ["3s"] },
      ],
    });
    const afterSnapshot = applyServerMessage(withEvents, {
      type: "Snapshot",
      view: aView({
        viewerSeat: 0,
        handNumber: 3,
        street: "COMPLETE",
        pot: 0,
        board: { cards: board },
      }),
    });
    const view = afterSnapshot.view;
    if (view === null) throw new Error("expected a view");
    const narration: GameEvent[] = [started(3), awarded(0, 4850)];

    const { rerender } = render(
      <DuelTable
        view={view}
        narration={narration}
        revealStep={afterSnapshot.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.queryByText(/You win/)).toBeNull();

    const afterFlop = advanceReveal(afterSnapshot);
    rerender(
      <DuelTable
        view={view}
        narration={narration}
        revealStep={afterFlop.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.queryByText(/You win/)).toBeNull();

    const afterTurn = advanceReveal(afterFlop);
    rerender(
      <DuelTable
        view={view}
        narration={narration}
        revealStep={afterTurn.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.queryByText(/You win/)).toBeNull();

    const afterRiver = advanceReveal(afterTurn);
    rerender(
      <DuelTable
        view={view}
        narration={narration}
        revealStep={afterRiver.reveal?.steps[0] ?? null}
      />,
    );
    expect(screen.getByText("You win 4,850")).toBeDefined();
  });
});
