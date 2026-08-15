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

/**
 * Every string the table speaks without printing it: `aria-label` and `title`.
 *
 * A screen reader reads the first aloud and a browser shows the second on hover,
 * so anything named in either reaches a player exactly as printed text does —
 * and neither is a text node, so `wordsOnScreen` cannot see it. Measured, not
 * reasoned about: a card name and a made-hand name in each of them shipped
 * `Tests 189 passed (189)` before this existed.
 */
function spokenOnScreen(container: HTMLElement): string {
  return [...container.querySelectorAll("[aria-label], [title]")]
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null)
    .join(" ");
}

/** A card named in words, spelled as `cardText` spells it. */
const CARD_NAME =
  /\b(ace|king|queen|jack|ten|nine|eight|seven|six|five|four|three|two) of (spades|hearts|diamonds|clubs)\b/i;

/** Vocabulary of made hands and winner declarations that only a client would derive. */
const HAND_TALK =
  /\b(pair|trips|set|straight|flush|full house|quads|high card|wins?|won|loses?|loser|winner|beats)\b/i;

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

  it("names exactly the cards the view carries and no others", () => {
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

    const { container } = render(<DuelTable view={VIEW} />);

    const cards = screen
      .getAllByRole("img")
      .map((card) => card.getAttribute("aria-label"));

    expect(cards).toEqual([
      "your rival's hidden hand",
      "ace of spades",
      "seven of diamonds",
      "two of clubs",
      "turn card, not yet dealt",
      "river card, not yet dealt",
      "ace of hearts",
      "king of spades",
    ]);

    // A card can be named outside a `role="img"` too. The list above only reads
    // card elements, so `aria-label="queen of clubs"` on anything else ships
    // green — and a screen reader would say it. Every card the table speaks must
    // belong to a card.
    const strays = [...container.querySelectorAll("[aria-label], [title]")]
      .filter((element) => element.getAttribute("role") !== "img")
      .flatMap((element) => [
        element.getAttribute("aria-label"),
        element.getAttribute("title"),
      ])
      .filter((value): value is string => value !== null);
    expect(strays.filter((value) => CARD_NAME.test(value))).toEqual([]);
  });

  it("puts no rank and no suit on a hand the view did not send", () => {
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

    render(<DuelTable view={VIEW} />);

    const hidden = screen.getByRole("img", {
      name: "your rival's hidden hand",
    });
    const row = hidden.parentElement;
    expect(row).not.toBeNull();

    expect(row!.textContent).toBe("");
    expect(row!.innerHTML).not.toMatch(/aria-label="(?!your rival)/);
  });

  it("names no hand and declares no winner", () => {
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

    const { container } = render(<DuelTable view={VIEW} />);

    expect(wordsOnScreen(container)).not.toMatch(HAND_TALK);
    // Spoken as well as printed: "Two pair" in an aria-label and "You win" in a
    // title each shipped green against the line above.
    expect(spokenOnScreen(container)).not.toMatch(HAND_TALK);
  });
});
