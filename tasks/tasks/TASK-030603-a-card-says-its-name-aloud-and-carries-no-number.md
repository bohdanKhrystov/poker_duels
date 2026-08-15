---
schema: 2
id: TASK-030603
title: A card says its name aloud, and carries no number
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui, a11y]
depends_on: [TASK-030602]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +144 passed \(144\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a card the way the design says it aloud'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'attaches no number to a card'
  - cd web-client && npm run check
---

## Goal

`CardText` gains the `label` the design's `aria-label`s are written in — `"ace of spades"` — and a
test pins the whole shape, so the day someone adds a numeric rank to this object the suite says so.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/card-text.ts` | modify |
| `web-client/src/table/card-text.test.ts` | modify — append one describe block; nothing above it changes |
| `design/screens/duel-table.html` | read — the `aria-label`s the words come from. **Read only: never edit anything under `design/`** |

## Scope

- `CardText` gains a fourth field, after `isRed`:

  ```ts
  /** The spoken name, as the design writes it: `"ace of spades"`. */
  readonly label: string;
  ```

- `RANKS` is replaced by a name table, and `SuitText` gains a `name`:

  ```ts
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
  ```

  with `s: { glyph: "♠︎", name: "spades", isRed: false }` and hearts, diamonds, clubs the same way.
  `Record<string, string | undefined>` again, for the reason `TASK-030602` gave: the lookup must be
  allowed to be `undefined`.

- `cardText` ends:

  ```ts
    const rank = RANK_NAMES[card[0]];
    const suit = SUITS[card[1]];
    if (rank === undefined || suit === undefined) return null;
    return {
      rank: card[0],
      suit: suit.glyph,
      isRed: suit.isRed,
      label: `${rank} of ${suit.name}`,
    };
  ```

  The name table now decides which ranks are readable, so the `RANKS` set goes away and the four
  `refuses a string it cannot read` cases keep passing unchanged.
- The module comment's second sentence becomes "…the glyph to print, whether the suit is drawn red,
  and the words a screen reader says."
- The words are the design's, verbatim: `duel-table.html` labels its cards `ace of spades`, `seven
  of diamonds`, `two of clubs`, `jack of hearts`, `king of spades`. Ten is `"ten"`, not `"10"` —
  the label is speech, and the glyph on the card stays `"T"`.
- **One existing assertion moves, and only one.** `TASK-030602`'s first test closes over the return
  value with `toEqual({ rank: "A", suit: "♠︎", isRed: false })`. A `toEqual` literal is a closed
  shape: adding `label` to `CardText` reddens it with `expected { rank: 'A', suit: '♠︎', …(2) } to
  deeply equal { rank: 'A', suit: '♠︎', isRed: false }`. Add `label: "ace of spades"` to that
  literal, changing nothing else in the block. Keep it a `toEqual` — do not weaken it to
  `toMatchObject`. Two independent closed-shape pins on `CardText` is the point: this one and
  `attaches no number to a card` both fail the day someone adds a numeric rank.

## Out of scope

- Editing any test above the new describe block **beyond the single line named in `Scope`**.
  `TASK-030603` originally claimed all four of `TASK-030602`'s `it` blocks were untouched, on the
  grounds that none of them names `label`. That was wrong: the first one closes over the whole
  object with `toEqual`, so it asserts the *absence* of a fourth field whether or not it names one.
  Three of the four are untouched; the first gains one line.
- Anything that renders. The first component is `TASK-030604`.

## Tests

`web-client/src/table/card-text.test.ts` — a second describe block, `"a card's spoken name"`,
appended below `"a card string"`. It reuses the file's existing `EVERY_CARD` constant.

| Test | Proves |
| --- | --- |
| `names a card the way the design says it aloud` | `"As"` → `"ace of spades"`, `"7d"` → `"seven of diamonds"`, `"2c"` → `"two of clubs"`, `"Jh"` → `"jack of hearts"` — the four labels `duel-table.html` writes |
| `names the picture ranks and the ten in words` | `"Ks"` → `"king of spades"`, `"Qc"` → `"queen of clubs"`, `"Td"` → `"ten of diamonds"` |
| `gives all fifty-two cards fifty-two different names` | `new Set(EVERY_CARD.map((card) => cardText(card)?.label)).size` is `52` |
| `attaches no number to a card` | for **every one of the fifty-two cards**, `Object.keys(...).sort()` equals `["isRed", "label", "rank", "suit"]` and no value of that object is a `number`. Not a single sample: a field added on a condition — `value` on the number ranks only — passes a one-card check, and this is the assertion the story leans on |

Four tests. One hundred and forty exist, so the suite reports **144**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 144 passed (144)` | the four ran and the hundred-and-forty before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Build the label as `` `${rank} ${suit.name}` `` (drop the `of`) → `names a card the way the
   design says it aloud` fails with `expected 'ace spades' to be 'ace of spades' // Object.is
   equality`, and `names the picture ranks and the ten in words` with `expected 'king spades' to be
   'king of spades'`. Revert.
2. Set `T: "10"` in `RANK_NAMES` → `names the picture ranks and the ten in words` fails with
   `expected '10 of diamonds' to be 'ten of diamonds' // Object.is equality`. Revert.
3. Add a `readonly value: number` field to `CardText` and populate it from the rank's position →
   `attaches no number to a card` fails with `expected [ 'isRed', 'label', 'rank', …(2) ] to deeply
   equal [ 'isRed', 'label', 'rank', 'suit' ]`. Revert. This is the one that matters: it is the
   executable form of "the client attaches no value to a card".

   A fourth, because a universal claim deserves a test of its universality: make that field
   **conditional** — `...(Number.isNaN(Number(card[0])) ? {} : { value: Number(card[0]) })`, so the
   thirty-six number ranks carry a pip value and the picture ranks do not. Against a check that
   samples `cardText("As")` alone this ships `Tests 144 passed (144)` — verified, not predicted.
   Against the loop over `EVERY_CARD` it reddens. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a card's spoken name > names a card the way the design says it aloud` passes
- [ ] `a card's spoken name > names the picture ranks and the ten in words` passes
- [ ] `a card's spoken name > gives all fifty-two cards fifty-two different names` passes
- [ ] `a card's spoken name > attaches no number to a card` passes
- [ ] `a card string > splits into the rank character and the suit glyph` still passes, with
      `label: "ace of spades"` added to its `toEqual` literal and nothing else in it changed
- [ ] The other three `it` blocks in `describe("a card string")` are unedited and their assertions
      are byte-identical
- [ ] `npm run --silent test` reports `Tests  144 passed (144)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
