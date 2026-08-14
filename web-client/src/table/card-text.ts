/**
 * A card string split for display, and for nothing else.
 *
 * The engine writes a card as a rank character then a suit character — `"As"`,
 * `"Td"`, `"2c"`. This module turns those two characters into ink: the glyph to
 * print and whether the suit is drawn red. It attaches no order, no value and
 * no comparison to a card, because ranking hands is the server's job and a
 * client that can compare two cards is one refactor from asserting who won.
 */
export interface CardText {
  /** The rank character, verbatim — `"A"`, `"T"`, `"2"`. */
  readonly rank: string;
  /** The suit glyph, in text presentation. */
  readonly suit: string;
  /** Whether the suit is drawn with `--pd-suit-red`. */
  readonly isRed: boolean;
}

const RANKS = new Set([..."AKQJT98765432"]);

interface SuitText {
  readonly glyph: string;
  readonly isRed: boolean;
}

const SUITS: Record<string, SuitText | undefined> = {
  s: { glyph: "♠︎", isRed: false },
  h: { glyph: "♥︎", isRed: true },
  d: { glyph: "♦︎", isRed: true },
  c: { glyph: "♣︎", isRed: false },
};

/**
 * The display text of `card`, or `null` when the string is not a card this
 * client can read. A card it cannot read is not one it may guess at.
 */
export function cardText(card: string): CardText | null {
  if (card.length !== 2) return null;
  const suit = SUITS[card[1]];
  if (!RANKS.has(card[0]) || suit === undefined) return null;
  return { rank: card[0], suit: suit.glyph, isRed: suit.isRed };
}
