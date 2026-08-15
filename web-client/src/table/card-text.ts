/**
 * A card string split for display, and for nothing else.
 *
 * The engine writes a card as a rank character then a suit character — `"As"`,
 * `"Td"`, `"2c"`. This module turns those two characters into ink: the glyph to
 * print, whether the suit is drawn red, and the words a screen reader says. It
 * attaches no order, no value and no comparison to a card, because ranking hands
 * is the server's job and a client that can compare two cards is one refactor
 * from asserting who won.
 */
export interface CardText {
  /** The rank character, verbatim — `"A"`, `"T"`, `"2"`. */
  readonly rank: string;
  /** The suit glyph, in text presentation. */
  readonly suit: string;
  /** Whether the suit is drawn with `--pd-suit-red`. */
  readonly isRed: boolean;
  /** The spoken name, as the design writes it: `"ace of spades"`. */
  readonly label: string;
}

const RANK_NAMES: Record<string, string | undefined> = {
  A: "ace",
  K: "king",
  Q: "queen",
  J: "jack",
  T: "ten",
  "9": "nine",
  "8": "eight",
  "7": "seven",
  "6": "six",
  "5": "five",
  "4": "four",
  "3": "three",
  "2": "two",
};

interface SuitText {
  readonly glyph: string;
  readonly name: string;
  readonly isRed: boolean;
}

const SUITS: Record<string, SuitText | undefined> = {
  s: { glyph: "♠︎", name: "spades", isRed: false },
  h: { glyph: "♥︎", name: "hearts", isRed: true },
  d: { glyph: "♦︎", name: "diamonds", isRed: true },
  c: { glyph: "♣︎", name: "clubs", isRed: false },
};

/**
 * The display text of `card`, or `null` when the string is not a card this
 * client can read. A card it cannot read is not one it may guess at.
 */
export function cardText(card: string): CardText | null {
  if (card.length !== 2) return null;
  const rank = RANK_NAMES[card[0]];
  const suit = SUITS[card[1]];
  if (rank === undefined || suit === undefined) return null;
  return {
    rank: card[0],
    suit: suit.glyph,
    isRed: suit.isRed,
    label: `${rank} of ${suit.name}`,
  };
}
