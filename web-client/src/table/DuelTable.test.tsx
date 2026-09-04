import { render, screen, within } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import type { GameEvent } from "../protocol";
import type { ActEvent } from "../store/duel-state";
import {
  advanceReveal,
  applyServerMessage,
  initialState,
} from "../store/duel-state";
import type { ClockReading } from "./turn-clock";
import { DuelTable } from "./DuelTable";
import { aView, aSeat } from "./view-fixture";

/** The seat plate whose name is `name`, so a stack can be pinned to a seat. */
function plateFor(name: string): HTMLElement {
  const plate = screen.getByText(name).closest("div");
  if (plate === null) throw new Error(`no seat plate named ${name}`);
  return plate;
}

/**
 * The countdown span `SeatPlate` draws — its own class list. `PotStrip`'s pot figure shares
 * `text-large` too, so this is counted per plate, never across the whole container.
 */
const COUNTDOWN_SELECTOR = "span.font-mono.text-large.tabular-nums";

/** How many countdown spans are on screen, summed across both plates and nowhere else. */
function countdowns(): number {
  return (
    plateFor("You").querySelectorAll(COUNTDOWN_SELECTOR).length +
    plateFor("Your rival").querySelectorAll(COUNTDOWN_SELECTOR).length
  );
}

/**
 * The countdown span in `plate` — fails loudly if it is missing. Scoped to one plate for the
 * same reason `countdowns()` above is: `PotStrip`'s figure shares every one of
 * `COUNTDOWN_SELECTOR`'s classes too, and a query over the whole document would rather find
 * whichever of the two sits first in the tree than the one this test means.
 */
function countdownIn(plate: HTMLElement): Element {
  const span = plate.querySelector(COUNTDOWN_SELECTOR);
  if (span === null) throw new Error("no countdown span on this plate");
  return span;
}

/**
 * Whether `element` is a clock's own countdown and not `PotStrip`'s figure sharing its
 * selector: the clock always carries one class beyond the three `COUNTDOWN_SELECTOR` requires
 * — the treatment's own colour, present in every one of the four states — where the pot's
 * figure carries only the three.
 */
function isClockFigure(element: Element): boolean {
  return element.classList.length > 3;
}

/**
 * `container`'s rendered tree with every countdown span removed — the one thing a live
 * clock legitimately redraws on its own, every second, with no server frame behind it
 * (`ADR-0108` §5, `ADR-0113` §6). What is left is what a player reads, and it is this that
 * must not move when the countdown crosses zero. `PotStrip`'s figure matches
 * `COUNTDOWN_SELECTOR` too and is deliberately left standing — the pot is exactly one of the
 * things this comparison must still catch moving.
 */
function withoutCountdowns(container: HTMLElement): string {
  const clone = container.cloneNode(true) as HTMLElement;
  clone.querySelectorAll(COUNTDOWN_SELECTOR).forEach((span) => {
    if (isClockFigure(span)) span.remove();
  });
  return clone.innerHTML;
}

/** A moment fixed once, so every clock test reasons about the same instant. */
const NOW = 1_700_000_000_000;

/**
 * A `ClockReading` at hand 1, seat 0 acting, 24 seconds left on the regular allowance and a
 * minute of bank behind it — every field overridable, so a test states only what it means.
 * No test below relies on these defaults alone; every seat-sensitive case states both the
 * clock's own seat and the view it is read against.
 */
function aReading(
  overrides: Partial<{
    handNumber: number;
    seat: number;
    actionSequence: number;
    turnEndsAt: number;
    expiresAt: number;
    bankRemainingMillis: readonly [number, number];
    nowMillis: number;
  }> = {},
): ClockReading {
  const {
    handNumber = 1,
    seat = 0,
    actionSequence = 1,
    turnEndsAt = NOW + 24_000,
    expiresAt = NOW + 24_000 + 60_000,
    bankRemainingMillis = [60_000, 60_000],
    nowMillis = NOW,
  } = overrides;
  return {
    clock: {
      handNumber,
      seat,
      actionSequence,
      turnEndsAt,
      expiresAt,
      bankRemainingMillis,
    },
    nowMillis,
  };
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

  it("marks the seat the act names, at either seat", () => {
    // Both directions in one test: a mark wired to the rival regardless of
    // the event's own seat — the ask as literally written, which `ADR-0109`
    // §Alternative 6 refused — passes the first half below and fails the
    // second.
    const view = aView({
      viewerSeat: 0,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });
    const rivalBet: ActEvent = {
      type: "PlayerBet",
      sequence: 20,
      seat: 1,
      to: 950,
    };

    const { container, rerender } = render(
      <DuelTable view={view} lastAct={rivalBet} />,
    );

    expect(container.querySelectorAll(".last-act")).toHaveLength(1);
    const rivalMark = container.querySelector(".last-act");
    expect(plateFor("Your rival").contains(rivalMark)).toBe(true);
    expect(plateFor("You").contains(rivalMark)).toBe(false);

    const heroBet: ActEvent = {
      type: "PlayerBet",
      sequence: 21,
      seat: 0,
      to: 950,
    };

    rerender(<DuelTable view={view} lastAct={heroBet} />);

    expect(container.querySelectorAll(".last-act")).toHaveLength(1);
    const heroMark = container.querySelector(".last-act");
    expect(plateFor("You").contains(heroMark)).toBe(true);
    expect(plateFor("Your rival").contains(heroMark)).toBe(false);
  });

  it("moves the mark rather than adding one when the other seat acts", () => {
    // A per-seat mark — the shape `ADR-0109` §Alternative 2 rejected — would
    // leave both this hand's marks standing and fail every assertion below.
    const view = aView({
      viewerSeat: 0,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });
    const rivalBet: ActEvent = {
      type: "PlayerBet",
      sequence: 30,
      seat: 1,
      to: 950,
    };

    const { container, rerender } = render(
      <DuelTable view={view} lastAct={rivalBet} />,
    );

    const heroCall: ActEvent = {
      type: "PlayerCalled",
      sequence: 31,
      seat: 0,
      to: 400,
    };

    rerender(<DuelTable view={view} lastAct={heroCall} />);

    expect(container.querySelectorAll(".last-act")).toHaveLength(1);
    const mark = container.querySelector(".last-act");
    expect(plateFor("You").contains(mark)).toBe(true);
    expect(container.textContent).not.toMatch(/950/);
  });

  it("marks no seat when no act has been made", () => {
    const view = aView({
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    const { container: omitted } = render(<DuelTable view={view} />);
    expect(omitted.querySelectorAll(".last-act")).toHaveLength(0);

    const { container: nulled } = render(
      <DuelTable view={view} lastAct={null} />,
    );
    expect(nulled.querySelectorAll(".last-act")).toHaveLength(0);
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

  it("stands chips at the rival's bet line beside the server's own figure", () => {
    const view = aView({
      seats: [
        aSeat({ index: 0 }),
        aSeat({ index: 1, committedThisStreet: 400 }),
      ],
    });

    const { container } = render(<DuelTable view={view} />);

    expect(container.querySelectorAll("p .chip-pile")).toHaveLength(1);
    expect(screen.getByText(/committed/)).toBeDefined();
    expect(screen.getByText("400")).toBeDefined();
  });

  it("draws no chips at a bet line with nothing on it", () => {
    const view = aView({
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    const { container } = render(<DuelTable view={view} />);

    expect(container.querySelectorAll("p .chip-pile")).toHaveLength(0);
    expect(container.querySelectorAll(".chip-pile").length).toBeGreaterThan(0);
  });

  it("gives the hero no bet line of their own", () => {
    const view = aView({
      viewerSeat: 0,
      seats: [
        aSeat({ index: 0, committedThisStreet: 900 }),
        aSeat({ index: 1, committedThisStreet: 0 }),
      ],
    });

    const { container } = render(<DuelTable view={view} />);

    expect(container.querySelectorAll("p")).toHaveLength(1);
    expect(container.querySelectorAll("p .chip-pile")).toHaveLength(0);
    expect(screen.getByText(/Pot/)).toBeDefined();
  });

  it("draws the countdown at whichever seat the clock names", () => {
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const clockAt0 = aReading({
      seat: 0,
      handNumber: 1,
      turnEndsAt: NOW + 24_000,
    });

    // Seat 0 acting, viewed from seat 0: the countdown is on the viewer's own plate.
    const { rerender } = render(
      <DuelTable
        view={aView({ viewerSeat: 0, seatToAct: 0, handNumber: 1, seats })}
        clock={clockAt0}
      />,
    );
    expect(within(plateFor("You")).getByText("24")).toBeDefined();
    expect(within(plateFor("Your rival")).queryByText("24")).toBeNull();

    // Same clock, same acting seat, but viewed from the other side: the countdown follows the
    // seat the clock names, not the viewer.
    rerender(
      <DuelTable
        view={aView({ viewerSeat: 1, seatToAct: 0, handNumber: 1, seats })}
        clock={clockAt0}
      />,
    );
    expect(within(plateFor("Your rival")).getByText("24")).toBeDefined();
    expect(within(plateFor("You")).queryByText("24")).toBeNull();

    // The mirror: seat 1 acting, with a figure that shares no constant with the run above.
    const clockAt1 = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW + 19_000,
    });

    rerender(
      <DuelTable
        view={aView({ viewerSeat: 1, seatToAct: 1, handNumber: 1, seats })}
        clock={clockAt1}
      />,
    );
    expect(within(plateFor("You")).getByText("19")).toBeDefined();
    expect(within(plateFor("Your rival")).queryByText("19")).toBeNull();

    rerender(
      <DuelTable
        view={aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats })}
        clock={clockAt1}
      />,
    );
    expect(within(plateFor("Your rival")).getByText("19")).toBeDefined();
    expect(within(plateFor("You")).queryByText("19")).toBeNull();
  });

  it("draws exactly one countdown", () => {
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const clockAt0 = aReading({
      seat: 0,
      handNumber: 1,
      turnEndsAt: NOW + 24_000,
    });
    const clockAt1 = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW + 19_000,
    });

    // Counted across both plates, never just the one the clock names — two clocks racing to
    // zero on one screen for one fact is exactly what `ADR-0108` §Consequences forecloses.
    const { rerender } = render(
      <DuelTable
        view={aView({ viewerSeat: 0, seatToAct: 0, handNumber: 1, seats })}
        clock={clockAt0}
      />,
    );
    expect(countdowns()).toBe(1);

    rerender(
      <DuelTable
        view={aView({ viewerSeat: 1, seatToAct: 0, handNumber: 1, seats })}
        clock={clockAt0}
      />,
    );
    expect(countdowns()).toBe(1);

    rerender(
      <DuelTable
        view={aView({ viewerSeat: 1, seatToAct: 1, handNumber: 1, seats })}
        clock={clockAt1}
      />,
    );
    expect(countdowns()).toBe(1);

    rerender(
      <DuelTable
        view={aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats })}
        clock={clockAt1}
      />,
    );
    expect(countdowns()).toBe(1);
  });

  it("draws both seats' banks", () => {
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const view = aView({ viewerSeat: 0, seatToAct: 0, handNumber: 1, seats });
    // Seat 0 is acting: its bank comes back from the live reading, not the frame's array
    // (`ADR-0113` §3), so `expiresAt` is built from the same figure to keep the two agreeing.
    // Seat 1 is not acting, so its bank is read straight off `bankRemainingMillis[1]`.
    const reading = aReading({
      seat: 0,
      handNumber: 1,
      turnEndsAt: NOW + 24_000,
      expiresAt: NOW + 24_000 + 180_000,
      bankRemainingMillis: [180_000, 72_000],
    });

    render(<DuelTable view={view} clock={reading} />);

    // `\s` rather than the literal `NBSP` mark: the text matcher's own normalizer collapses
    // the non-breaking space `SeatPlate` joins the label and figure with down to an ordinary
    // one before comparing.
    expect(within(plateFor("You")).getByText(/Timebank\s3:00/)).toBeDefined();
    expect(
      within(plateFor("Your rival")).getByText(/Timebank\s1:12/),
    ).toBeDefined();
  });

  it("draws no countdown and no bank before a TurnClock has arrived", () => {
    const view = aView({
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    });

    render(<DuelTable view={view} />);

    expect(countdowns()).toBe(0);
    expect(screen.queryByText(/Timebank/)).toBeNull();
  });

  it("draws nothing for a clock the view has moved past", () => {
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const reading = aReading({
      seat: 0,
      handNumber: 1,
      turnEndsAt: NOW + 24_000,
    });

    const staleHand = aView({
      viewerSeat: 0,
      seatToAct: 0,
      handNumber: 2,
      seats,
    });
    const { rerender } = render(<DuelTable view={staleHand} clock={reading} />);
    expect(countdowns()).toBe(0);

    const staleSeat = aView({
      viewerSeat: 0,
      seatToAct: 1,
      handNumber: 1,
      seats,
    });
    rerender(<DuelTable view={staleSeat} clock={reading} />);
    expect(countdowns()).toBe(0);
  });

  it("draws the fresh allowance regular, and its last seconds running out", () => {
    // The card's rows A and B (`design/components/seat-and-pot.html`): 24 seconds left
    // reads bare, 6 reads under the running-out treatment — the same figures the card draws.
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const view = aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats });

    const fresh = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW + 24_000,
    });
    const { rerender } = render(<DuelTable view={view} clock={fresh} />);

    const regular = countdownIn(plateFor("Your rival"));
    expect(regular.textContent).toBe("24");
    expect(regular.className).toBe(
      "font-mono text-large tabular-nums text-text",
    );

    const runningLow = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW + 6_000,
    });
    rerender(<DuelTable view={view} clock={runningLow} />);

    const runningOut = countdownIn(plateFor("Your rival"));
    expect(runningOut.textContent).toBe("6");
    expect(runningOut.className).toContain("text-warn");
  });

  it("draws the bank's own time on timebank", () => {
    // Past turnEndsAt, spending the bank: the card's row for on-timebank draws the same
    // figure twice, once as the countdown and once as that seat's own bank.
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const view = aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats });
    const reading = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW - 5_000,
      expiresAt: NOW + 167_000,
    });

    render(<DuelTable view={view} clock={reading} />);

    const span = countdownIn(plateFor("Your rival"));
    expect(span.textContent).toBe("2:47");
    expect(span.className).toContain("text-accent");
    expect(
      within(plateFor("Your rival")).getByText(/Timebank\s2:47/),
    ).toBeDefined();
  });

  it("draws a spent clock expired, holding at zero", () => {
    // Past expiresAt: the figure holds at zero under the quietest treatment, and the bank
    // it just finished spending reads as fully spent too.
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const view = aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats });
    const reading = aReading({
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW - 10_000,
      expiresAt: NOW - 1_000,
    });

    render(<DuelTable view={view} clock={reading} />);

    const span = countdownIn(plateFor("Your rival"));
    expect(span.textContent).toBe("0");
    expect(span.className).toContain("text-text-faint");
    expect(
      within(plateFor("Your rival")).getByText(/Timebank\s0:00/),
    ).toBeDefined();
  });

  it("changes nothing a player reads when the countdown reaches zero", () => {
    // One clock — turnEndsAt and expiresAt coincide, so there is no bank behind this
    // decision and the acting seat's own bank reads 0:00 on both sides of the crossing,
    // the one construction under which nothing the clock itself draws can differ except
    // the countdown span this test strips before comparing. Two stated readings against
    // it, three seconds before and nine after: no timer, real or fake, is installed.
    const seats = [aSeat({ index: 0 }), aSeat({ index: 1 })];
    const view = aView({ viewerSeat: 0, seatToAct: 1, handNumber: 1, seats });
    const atZero = {
      seat: 1,
      handNumber: 1,
      turnEndsAt: NOW + 3_000,
      expiresAt: NOW + 3_000,
      bankRemainingMillis: [45_000, 0] as const,
    };

    const { container, rerender } = render(
      <DuelTable view={view} clock={aReading({ ...atZero, nowMillis: NOW })} />,
    );
    // Their turn still — the server, not the crossing, is what would ever move this.
    expect(
      within(plateFor("Your rival")).getByText("Their turn"),
    ).toBeDefined();
    const before = withoutCountdowns(container);

    rerender(
      <DuelTable
        view={view}
        clock={aReading({ ...atZero, nowMillis: NOW + 12_000 })}
      />,
    );

    expect(
      within(plateFor("Your rival")).getByText("Their turn"),
    ).toBeDefined();
    const after = withoutCountdowns(container);
    expect(after).toBe(before);

    // The screen never invents the act: no mark that only a server frame may produce.
    expect(container.innerHTML).not.toMatch(/The server (folded|checked)/);
  });
});
