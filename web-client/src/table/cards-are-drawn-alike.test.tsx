import { describe, it, expect } from "vitest";
import { render, type RenderResult } from "@testing-library/react";
import { Hand } from "./Hand";
import { BoardCards } from "./BoardCards";
import { cardText } from "./card-text";
import { aSeat, aView } from "./view-fixture";

/**
 * `ADR-0126` §1: the drawing of a card does not depend on whether it played.
 * A completed showdown is the one street where that is checkable at all — every
 * place is dealt, both hands are revealed, and the winning five is a fact only
 * the engine holds (`ADR-0126` §2b moves no wire), so this fixture cannot lean
 * on "the winning cards" as a concept. It mixes rank and suit deliberately: a
 * one-colour deal could not tell the normalisation below from a constant.
 */
const BOARD = ["2h", "9s", "Kd", "5c", "Jh"] as const;
const SEAT_0_CARDS = ["As", "Td"] as const;
const SEAT_1_CARDS = ["Qc", "7h"] as const;
const DEALT_IN_RENDER_ORDER = [...SEAT_0_CARDS, ...SEAT_1_CARDS, ...BOARD];
const DEALT_LABELS = new Set(
  DEALT_IN_RENDER_ORDER.map((card) => cardText(card)!.label),
);
// `faceUpCards` returns hole cards before board cards (render order, above),
// so this is where the split falls for the parent check below.
const HOLE_CARD_COUNT = SEAT_0_CARDS.length + SEAT_1_CARDS.length;

function showdownView(viewerSeat: 0 | 1) {
  return aView({
    viewerSeat,
    street: "SHOWDOWN",
    board: { cards: [...BOARD] },
    seats: [
      aSeat({ index: 0, holeCards: [...SEAT_0_CARDS] }),
      aSeat({ index: 1, holeCards: [...SEAT_1_CARDS] }),
    ],
  });
}

/**
 * Neither seat's `Hand` nor `BoardCards` reads `viewerSeat` — a completed
 * showdown reveals both hands to everyone — so this composes the same two
 * `Hand`s and one `BoardCards` regardless. The parameter still varies across
 * calls below: the property under test must hold for a `PlayerView` built
 * from either seat, not just the one a coder happened to try first.
 */
function renderShowdown(viewerSeat: 0 | 1): RenderResult {
  const view = showdownView(viewerSeat);
  return render(
    <>
      <Hand
        cards={view.seats[0].holeCards}
        hiddenLabel="seat 0's hidden card"
      />
      <Hand
        cards={view.seats[1].holeCards}
        hiddenLabel="seat 1's hidden card"
      />
      <BoardCards cards={view.board.cards} />
    </>,
  );
}

function faceUpCards(result: RenderResult): Element[] {
  return result
    .getAllByRole("img")
    .filter((el) => DEALT_LABELS.has(el.getAttribute("aria-label") ?? ""));
}

/**
 * The signature two cards share if and only if nothing but rank, suit and
 * suit-colour distinguishes their markup. Exactly three normalisations:
 * `aria-label` is dropped (checked on its own, in the last test below), the
 * suit-colour class tokens are dropped and the rest sorted, and every text
 * node becomes one placeholder character so a rank or suit glyph is invisible
 * to it. Everything else a developer might reach for to pick a card out — a
 * new class, a `data-*` attribute, a `title`, a `style` — stays in the
 * signature and so breaks the equality this file asserts.
 */
function drawingSignature(el: Element): string {
  const attrs = Array.from(el.attributes)
    .filter((attr) => attr.name !== "aria-label")
    .map((attr) => {
      if (attr.name !== "class")
        return `${attr.name}=${JSON.stringify(attr.value)}`;
      const tokens = attr.value
        .split(/\s+/)
        .filter(
          (token) =>
            token.length > 0 &&
            token !== "text-suit-red" &&
            token !== "text-suit-black",
        )
        .sort();
      return `class=${JSON.stringify(tokens.join(" "))}`;
    })
    .sort()
    .join(" ");

  const children = Array.from(el.childNodes)
    .map((node) =>
      node.nodeType === Node.ELEMENT_NODE
        ? drawingSignature(node as Element)
        : "•",
    )
    .join("");

  return `<${el.tagName.toLowerCase()} ${attrs}>${children}</>`;
}

/**
 * The tag name and sorted class list of `el`'s immediate parent — the one
 * comparison `drawingSignature` cannot make on its own, because it never
 * walks above the element it is handed. A wrapper dropped around a single
 * card (`<div className="ring-2">{face}</div>`) changes only that card's
 * parent, never its own tag, attributes or children, so `drawingSignature`
 * alone would wave it through.
 *
 * Compared only within a card's own container (hole cards against hole
 * cards, board cards against board cards), never across the two: `Hand`
 * mounts its faces straight into whatever encloses it, while `BoardCards`
 * mounts its five into its own `flex` row, so an unmutated hole card and an
 * unmutated board card legitimately have different parents. A global
 * comparison across all nine would fail before any mutation is applied.
 */
function parentSignature(el: Element): string {
  const parent = el.parentElement;
  if (parent === null) return "∅";
  const classes = Array.from(parent.classList).sort().join(" ");
  return `<${parent.tagName.toLowerCase()} class=${JSON.stringify(classes)}>`;
}

describe("a card is drawn the same whether or not it played", () => {
  it("the showdown fixture puts nine face-up cards on the table, from either seat", () => {
    // Without this count control, every assertion below would pass vacuously
    // against a table that drew nothing at all.
    const forSeat0 = renderShowdown(0);
    expect(faceUpCards(forSeat0)).toHaveLength(9);
    forSeat0.unmount();

    const forSeat1 = renderShowdown(1);
    expect(faceUpCards(forSeat1)).toHaveLength(9);
    forSeat1.unmount();
  });

  it("every face-up card at a completed showdown is drawn the same", () => {
    const result = renderShowdown(0);
    const cards = faceUpCards(result);
    const signatures = cards.map(drawingSignature);

    expect(new Set(signatures).size).toBe(1);
    signatures.forEach((signature) => expect(signature).toBe(signatures[0]));

    // `drawingSignature` never looks above the element it is given, so a
    // wrapper placed around a single card is invisible to it — this closes
    // that gap, within each card's own container (see `parentSignature`).
    const holeParents = cards.slice(0, HOLE_CARD_COUNT).map(parentSignature);
    expect(new Set(holeParents).size).toBe(1);
    const boardParents = cards.slice(HOLE_CARD_COUNT).map(parentSignature);
    expect(new Set(boardParents).size).toBe(1);
  });

  it("the same holds when the viewer is the other seat", () => {
    // One seat cannot tell a general rule from a constant.
    const result = renderShowdown(1);
    const cards = faceUpCards(result);
    const signatures = cards.map(drawingSignature);

    expect(new Set(signatures).size).toBe(1);
    signatures.forEach((signature) => expect(signature).toBe(signatures[0]));

    const holeParents = cards.slice(0, HOLE_CARD_COUNT).map(parentSignature);
    expect(new Set(holeParents).size).toBe(1);
    const boardParents = cards.slice(HOLE_CARD_COUNT).map(parentSignature);
    expect(new Set(boardParents).size).toBe(1);
  });

  it("the signature sees a class, a data attribute, a title and a style, and sees nothing in an untouched copy", () => {
    const result = renderShowdown(0);
    const [original] = faceUpCards(result);
    const originalSignature = drawingSignature(original);

    const untouched = original.cloneNode(true) as Element;
    expect(drawingSignature(untouched)).toBe(originalSignature);

    const withClass = original.cloneNode(true) as Element;
    withClass.classList.add("ring-2");
    expect(drawingSignature(withClass)).not.toBe(originalSignature);

    const withDataAttribute = original.cloneNode(true) as Element;
    withDataAttribute.setAttribute("data-winning", "true");
    expect(drawingSignature(withDataAttribute)).not.toBe(originalSignature);

    const withTitle = original.cloneNode(true) as Element;
    withTitle.setAttribute("title", "the winning five");
    expect(drawingSignature(withTitle)).not.toBe(originalSignature);

    const withStyle = original.cloneNode(true) as Element;
    withStyle.setAttribute("style", "opacity:0.5");
    expect(drawingSignature(withStyle)).not.toBe(originalSignature);
  });

  it("no face-up card's label says more than its own rank and suit", () => {
    // The one hole `drawingSignature` cannot close: two cards legitimately
    // carry different labels, because the label is the rank and suit spoken
    // aloud — `ADR-0126` §1 names its own bound at "anything in an
    // `aria-label` beyond the rank and suit `card-text.ts` already produces".
    const result = renderShowdown(0);
    const cards = faceUpCards(result);
    const expectedLabels = DEALT_IN_RENDER_ORDER.map(
      (card) => cardText(card)!.label,
    );

    cards.forEach((el, index) => {
      expect(el.getAttribute("aria-label")).toBe(expectedLabels[index]);
    });
    expect(
      [...cards.map((el) => el.getAttribute("aria-label"))].sort(),
    ).toEqual([...expectedLabels].sort());
  });
});
