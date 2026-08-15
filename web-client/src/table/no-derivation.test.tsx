import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import type { PlayerView } from "../protocol";
import { DuelTable } from "./DuelTable";
import { aView, aSeat } from "./view-fixture";

/** Every number `view` carries, in any field a screen could reach. */
function numbersIn(view: PlayerView): Set<number> {
  return new Set([
    view.viewerSeat,
    view.handNumber,
    view.buttonSeat,
    view.pot,
    view.betToMatch,
    view.minRaiseTo,
    view.seatToAct ?? 0,
    view.smallBlind,
    view.bigBlind,
    ...view.seats.flatMap((seat) => [
      seat.index,
      seat.stack,
      seat.committedThisStreet,
      seat.committedThisHand,
    ]),
  ]);
}

/**
 * Every text node under `container`, joined by a space.
 *
 * Not `textContent`: that runs the last word of one element into the first of
 * the next, and `\b` then fails to see a word boundary that the player's eye
 * sees plainly — a banner reading "You win" beside a card would slip a `\bwin\b`
 * guard entirely. Measured, not reasoned about.
 */
function wordsOnScreen(container: HTMLElement): string {
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  const parts: string[] = [];
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    parts.push(node.textContent ?? "");
  }
  return parts.join(" ");
}

/**
 * Every number the table puts in front of the player, with the card faces
 * removed first: a rank glyph is a character the server sent, not a figure.
 */
function numbersOnScreen(container: HTMLElement): number[] {
  const copy = container.cloneNode(true) as HTMLElement;
  copy.querySelectorAll('[role="img"]').forEach((card) => card.remove());
  // Text nodes are not the whole of what a player receives. `aria-label` is read
  // aloud and `title` is shown on hover, so a figure the client worked out for
  // itself reaches them from either just as surely as from print. Measured, not
  // reasoned about: a derived total in an aria-label, and the same total in a
  // title, each shipped `Tests 186 passed (186)` against a text-only scan.
  const spoken = [...copy.querySelectorAll("[aria-label], [title]")]
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null);
  const digits =
    [wordsOnScreen(copy), ...spoken].join(" ").match(/\d[\d,]*/g) ?? [];
  return digits.map((run) => Number(run.replaceAll(",", "")));
}

describe("the table renders and never derives", () => {
  it("shows no number the view does not carry", () => {
    // Every number is distinct, and no two of them add, subtract, double or halve
    // into a third: a figure the table works out for itself therefore lands
    // outside the allowed set instead of colliding with a legitimate one. The
    // board is three cards on the turn, so a street read off the card count is
    // the wrong street.
    const VIEW: PlayerView = aView({
      viewerSeat: 0,
      handNumber: 14,
      buttonSeat: 1,
      street: "TURN",
      board: { cards: ["As", "7d", "2c"] },
      pot: 5675,
      betToMatch: 1450,
      minRaiseTo: 2025,
      seatToAct: 0,
      smallBlind: 100,
      bigBlind: 175,
      seats: [
        aSeat({
          index: 0,
          stack: 10200,
          committedThisStreet: 125,
          committedThisHand: 775,
          holeCards: ["Ah", "Ks"],
        }),
        aSeat({
          index: 1,
          stack: 14750,
          committedThisStreet: 825,
          committedThisHand: 1725,
          holeCards: [],
        }),
      ],
    });

    // The fixture's independence is asserted, not asserted-in-a-comment. It had
    // already rotted: the values this replaced held 950 x 2 = 1900 and 75 x 2 =
    // 150 — a client deriving `minRaiseTo` from the bet, or the big blind from
    // the small, would have landed exactly on a legitimate number and passed.
    const money = [...numbersIn(VIEW)].filter((value) => value > 1);
    for (const a of money) {
      expect(money).not.toContain(a * 2);
      for (const b of money) {
        if (a === b) continue;
        expect(money).not.toContain(a + b);
        expect(money).not.toContain(Math.abs(a - b));
      }
    }

    const { container } = render(<DuelTable view={VIEW} />);

    const shown = numbersOnScreen(container);
    const allowed = numbersIn(VIEW);

    expect(shown.length).toBeGreaterThan(0);
    const notAllowed = shown.filter((n) => !allowed.has(n));
    expect(notAllowed).toEqual([]);
  });

  it("names the street the view names, not the one the board looks like", () => {
    const VIEW: PlayerView = aView({
      street: "TURN",
      board: { cards: ["As", "7d", "2c"] },
    });

    render(<DuelTable view={VIEW} />);

    expect(screen.getByText(/· Turn$/)).toBeDefined();
    expect(screen.queryByText(/· Flop$/)).toBeNull();
  });
});
