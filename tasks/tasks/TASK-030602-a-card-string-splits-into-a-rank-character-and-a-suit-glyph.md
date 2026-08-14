---
schema: 2
id: TASK-030602
title: A card string splits into a rank character and a suit glyph
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030601]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +140 passed \(140\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'splits into the rank character and the suit glyph'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a string it cannot read'
  - cd web-client && npm run check
---

## Goal

The story's second design note, in one pure function: the client splits the engine's two-character
card **for display only** — the suit character picks the glyph and the colour, and nothing else is
attached to it.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/card-text.ts` | create |
| `web-client/src/table/card-text.test.ts` | create |

## Scope

- The whole module, verbatim — the doc comment is part of the deliverable, because it is the only
  place the rule is written down:

  ```ts
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
  ```

- **The glyphs are `U+2660 U+FE0E`, `U+2665 U+FE0E`, `U+2666 U+FE0E`, `U+2663 U+FE0E`** — each suit
  character followed by VARIATION SELECTOR-15, copied from `design/screens/duel-table.html`. Without
  the selector some platforms render an emoji card suit instead of a glyph. Copy them from this
  ticket; do not retype them.
- `SUITS` is typed `Record<string, SuitText | undefined>` **on purpose**: `noUncheckedIndexedAccess`
  is off in `tsconfig.json`, so a plain `Record<string, SuitText>` would type the lookup as
  `SuitText` and `suit === undefined` would be a `tsc` error (TS2367, "no overlap").
- The rank is passed through verbatim, ten included: the design draws one glyph in a slot sized at
  0.24 of the card's width, and `"T"` is the character the engine writes.

## Out of scope

- The spoken name (`"ace of spades"`) — `TASK-030603` adds a `label` field to the same interface and
  appends its own tests. Do not add it here.
- Any React. This file imports nothing.
- Any ordering, `sort`, comparison or numeric rank. That is the whole point of the module's comment.

## Tests

`web-client/src/table/card-text.test.ts`, describe block `"a card string"`. Two module constants
above it, used by this ticket and the next:

```ts
const RANKS = "AKQJT98765432";
const SUITS = "shdc";

const EVERY_CARD = [...RANKS].flatMap((rank) =>
  [...SUITS].map((suit) => `${rank}${suit}`),
);
```

| Test | Proves |
| --- | --- |
| `splits into the rank character and the suit glyph` | `cardText("As")` has `rank` `"A"` and `suit` `"♠︎"`; `cardText("Td")?.rank` is `"T"`; `cardText("2c")?.suit` is `"♣︎"` |
| `draws hearts and diamonds red and spades and clubs black` | `isRed` is `true` for `"Ah"` and `"Ad"`, `false` for `"As"` and `"Ac"` |
| `reads every one of the fifty-two cards the engine writes` | `EVERY_CARD` has 52 entries, none reads `null`, and the 52 `` `${rank}${suit}` `` pairs are 52 distinct strings |
| `refuses a string it cannot read` | `""`, `"A"`, `"Ass"`, `"1s"`, `"Ax"` and `"as"` each read `null` |

Four tests. One hundred and thirty-six exist, so the suite reports **140**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 140 passed (140)` | the four ran and the hundred-and-thirty-six before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

`NO_COLOR=1` prefixes every grep: this environment sets `FORCE_COLOR=3` and Vitest colours a piped
stream without it.

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the `if (card.length !== 2) return null;` line → `refuses a string it cannot read` fails
   with `AssertionError: expected { rank: 'A', suit: '♠︎', isRed: false } to be null` (the `"Ass"`
   case: a three-character string whose first two characters happen to read). Revert.
2. Set diamonds to `isRed: false` → `draws hearts and diamonds red and spades and clubs black`
   fails with `expected false to be true // Object.is equality`. Revert.
3. Return `suit: card[1]` instead of `suit: suit.glyph` → `splits into the rank character and the
   suit glyph` fails with `expected 's' to be '♠︎' // Object.is equality`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a card string > splits into the rank character and the suit glyph` passes
- [ ] `a card string > draws hearts and diamonds red and spades and clubs black` passes
- [ ] `a card string > reads every one of the fifty-two cards the engine writes` passes
- [ ] `a card string > refuses a string it cannot read` passes
- [ ] `npm run --silent test` reports `Tests  140 passed (140)`
- [ ] `card-text.ts` contains no `import` statement and no `sort`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
