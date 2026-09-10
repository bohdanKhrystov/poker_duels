import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import type { GameEvent } from "../protocol";
import { Hand } from "./Hand";
import { BoardCards } from "./BoardCards";
import { DuelTable } from "./DuelTable";
import { standingOf, winningCards } from "./winning-cards";
import { aSeat, aView } from "./view-fixture";

/**
 * The five that took the pot are marked at the hand's last beat, and only
 * then: mid-hand every card is drawn alike, because the client holds no set
 * to mark — the five are the engine's, carried on the award, never worked out
 * here.
 */
const BOARD = ["2h", "9s", "Kd", "5c", "Jh"] as const;
const SEAT_0_CARDS = ["As", "Td"] as const;
const SEAT_1_CARDS = ["Qc", "7h"] as const;
// Seat 0 wins with ace high: A, K, J, T, 9.
const WINNING_FIVE = ["As", "Kd", "Jh", "Td", "9s"] as const;

const started: GameEvent = {
  type: "HandStarted",
  sequence: 1,
  handNumber: 4,
  buttonSeat: 0,
  smallBlind: 50,
  bigBlind: 100,
  stacks: [10_000, 10_000],
};

function completedView(viewerSeat: 0 | 1) {
  return aView({
    viewerSeat,
    handNumber: 4,
    street: "COMPLETE",
    board: { cards: [...BOARD] },
    seats: [
      aSeat({ index: 0, holeCards: [...SEAT_0_CARDS] }),
      aSeat({ index: 1, holeCards: [...SEAT_1_CARDS] }),
    ],
  });
}

function standings(container: HTMLElement): Map<string, string> {
  const out = new Map<string, string>();
  for (const card of container.querySelectorAll("[data-standing]")) {
    out.set(
      card.getAttribute("aria-label") ?? "",
      card.getAttribute("data-standing") ?? "",
    );
  }
  return out;
}

describe("the winning five", () => {
  it("collects the five each award carries, and nothing for a fold", () => {
    expect(
      winningCards(
        [
          started,
          { type: "PotAwarded", sequence: 9, seat: 0, amount: 600, hand: [] },
        ],
        4,
      ).size,
    ).toBe(0);

    const marked = winningCards(
      [
        started,
        {
          type: "PotAwarded",
          sequence: 9,
          seat: 0,
          amount: 600,
          hand: [...WINNING_FIVE],
        },
      ],
      4,
    );
    expect([...marked].sort()).toEqual([...WINNING_FIVE].sort());

    // A split marks both seats' five.
    const split = winningCards(
      [
        started,
        {
          type: "PotAwarded",
          sequence: 9,
          seat: 0,
          amount: 300,
          hand: ["As", "Kd", "Jh", "Td", "9s"],
        },
        {
          type: "PotAwarded",
          sequence: 10,
          seat: 1,
          amount: 300,
          hand: ["Qc", "Kd", "Jh", "9s", "7h"],
        },
      ],
      4,
    );
    expect(split.size).toBe(7);
  });

  it("stands every card plain where there is no mark", () => {
    expect(standingOf("As", undefined)).toBe("plain");
    expect(standingOf("As", new Set())).toBe("plain");
    expect(standingOf("As", new Set(["As"]))).toBe("marked");
    expect(standingOf("2h", new Set(["As"]))).toBe("unmarked");
  });

  it("draws every face-up card alike when nothing is marked", () => {
    const view = completedView(0);
    const { container } = render(
      <>
        <Hand cards={view.seats[0].holeCards} hiddenLabel="seat 0" />
        <Hand cards={view.seats[1].holeCards} hiddenLabel="seat 1" />
        <BoardCards cards={view.board.cards} />
      </>,
    );
    const seen = standings(container);
    expect(seen.size).toBe(9);
    expect(new Set(seen.values())).toEqual(new Set(["plain"]));
  });

  it("marks the five on the table's last beat, in both hands and on the board", () => {
    const narration: GameEvent[] = [
      started,
      {
        type: "PotAwarded",
        sequence: 9,
        seat: 0,
        amount: 600,
        hand: [...WINNING_FIVE],
      },
    ];
    for (const viewerSeat of [0, 1] as const) {
      const { container, unmount } = render(
        <DuelTable view={completedView(viewerSeat)} narration={narration} />,
      );
      const seen = standings(container);
      expect(seen.size).toBe(9);
      expect(seen.get("ace of spades")).toBe("marked");
      expect(seen.get("king of diamonds")).toBe("marked");
      expect(seen.get("jack of hearts")).toBe("marked");
      expect(seen.get("ten of diamonds")).toBe("marked");
      expect(seen.get("nine of spades")).toBe("marked");
      expect(seen.get("two of hearts")).toBe("unmarked");
      expect(seen.get("five of clubs")).toBe("unmarked");
      expect(seen.get("queen of clubs")).toBe("unmarked");
      expect(seen.get("seven of hearts")).toBe("unmarked");
      unmount();
    }
  });

  it("marks nothing while a runout is still turning cards, and nothing mid-hand", () => {
    const narration: GameEvent[] = [
      started,
      {
        type: "PotAwarded",
        sequence: 9,
        seat: 0,
        amount: 600,
        hand: [...WINNING_FIVE],
      },
    ];
    const { container, unmount } = render(
      <DuelTable
        view={completedView(0)}
        narration={narration}
        revealStep={{ board: ["2h", "9s", "Kd", "5c"], street: "TURN" }}
      />,
    );
    expect(new Set(standings(container).values())).toEqual(new Set(["plain"]));
    unmount();

    const midHand = render(
      <DuelTable
        view={aView({ ...completedView(0), street: "RIVER" })}
        narration={[started]}
      />,
    );
    expect(new Set(standings(midHand.container).values())).toEqual(
      new Set(["plain"]),
    );
    expect(screen.queryByText(/wins/)).toBeNull();
  });
});
