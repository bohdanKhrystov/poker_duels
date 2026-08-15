---
schema: 2
id: TASK-030605
title: A face-up card draws its rank, its suit and the suit's colour
type: task
status: done
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030604]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +151 passed \(151\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a face-up card in words'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints the rank character and the suit glyph'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps a card it cannot read face down'
  - cd web-client && npm run check
---

## Goal

The brightest object on the screen: a paper-white card carrying the rank in its corner, the suit
under it, the big pip opposite, and the suit's colour — everything scaled from the row's `--w`, as
the design draws it.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/PlayingCard.tsx` | modify — add `CardFace` above `CardBack` |
| `web-client/src/table/PlayingCard.test.tsx` | modify — append three `it` blocks and one describe |
| `design/screens/duel-table.html` | read — the `.pc`, `.pc .ix`, `.pc .pip` rules. **Read only: never edit anything under `design/`** |

## Scope

- One import joins the top of the file: `import { cardText } from "./card-text";`
- `CardFace` goes **above** `CardBack`, verbatim — every class string below is already in the order
  `prettier-plugin-tailwindcss` produces, so run `npm run format` and expect no diff here:

  ```tsx
  /** A face-up card: the rank character, the suit glyph, and the suit's colour. */
  export function CardFace(props: { card: string }): ReactElement {
    const text = cardText(props.card);
    if (text === null) {
      // A card the client cannot read is not one it may guess at — and it still
      // holds its place, so an unreadable card never reflows the row.
      return <CardBack label="an unreadable card" />;
    }
    return (
      <span
        role="img"
        aria-label={text.label}
        className={`${SHELL} relative bg-card-face shadow-[var(--pd-shadow-card)] ${
          text.isRed ? "text-suit-red" : "text-suit-black"
        }`}
      >
        <span
          aria-hidden="true"
          className="absolute top-[calc(var(--w)*0.07)] left-[calc(var(--w)*0.09)] leading-[1] font-bold"
        >
          <span className="block text-[calc(var(--w)*0.24)]">{text.rank}</span>
          <span className="mt-[calc(var(--w)*0.02)] block text-[calc(var(--w)*0.2)]">
            {text.suit}
          </span>
        </span>
        <span
          aria-hidden="true"
          className="absolute right-[calc(var(--w)*0.09)] bottom-[calc(var(--w)*0.07)] text-[calc(var(--w)*0.34)] leading-[1]"
        >
          {text.suit}
        </span>
      </span>
    );
  }
  ```

- The fractions `0.07 / 0.09 / 0.24 / 0.20 / 0.02 / 0.34` are the design's, copied from
  `duel-table.html`'s `.pc .ix` and `.pc .pip` rules. `leading-[1]` rather than `leading-none`
  because `app.css` resets `--leading-*`.
- The card's accessible name is `text.label` and its ink is `aria-hidden`: a reader hears "ace of
  spades" once, not "A ♠ ♠".
- An unreadable string draws a back, not nothing and not a throw. The row keeps its geometry and
  the client never guesses a card it could not parse.

## Out of scope

- Sizing. `CardFace` sets no width: `--w` is inherited from the row, and the rows are
  `TASK-030606`'s and `TASK-030607`'s.
- Any card state the wire does not carry — a highlight for "part of the winning hand", a dim for a
  mucked card. Both need facts no `PlayerView` field holds.

## Tests

`web-client/src/table/PlayingCard.test.tsx`. The import line becomes
`import { CardBack, CardFace, CardSlot } from "./PlayingCard";`, a new describe block
`"a face-up card"` is added **above** the existing `"a card back and an undealt place"` block, and
`TASK-030604`'s three `it` blocks are not edited.

| Test | Proves |
| --- | --- |
| `names a face-up card in words` | `<CardFace card="As" />` is findable by `getByRole("img", { name: "ace of spades" })` |
| `prints the rank character and the suit glyph` | `<CardFace card="Td" />` renders `container.textContent` exactly `"T♦︎♦︎"` — rank, corner suit, pip |
| `paints a red suit with the suit-red token and a black one without it` | the root element's `className.split(" ")` contains `text-suit-red` for `"Ah"` and `text-suit-black` for `"Ks"`, **and each lacks the other's class**. Presence alone is only half the title's claim: a card carrying both ships `Tests 151 passed (151)` — verified, not predicted — and leaves the colour to whichever rule the stylesheet defines last, painting every suit alike |
| `keeps a card it cannot read face down` | `<CardFace card="Zz" />` is findable by `getByRole("img", { name: "an unreadable card" })` and its `container.textContent` is `""` |

Four tests. One hundred and forty-seven exist, so the suite reports **151**.

jsdom does not run Tailwind, so the colour test can only prove the class is on the element — the
same limit `TASK-030208` recorded, and the same answer: the class name resolves through
`--color-suit-red` to `--pd-suit-red` in the built stylesheet.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 151 passed (151)` | the four ran and the hundred-and-forty-seven before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, and the colour-literal guard passes |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Swap the ternary to `text.isRed ? "text-suit-black" : "text-suit-red"` → `paints a red suit with
   the suit-red token and a black one without it` fails with `expected [ Array(8) ] to include
   'text-suit-red'`. Revert.
2. Delete the pip span (the second `aria-hidden` block) → `prints the rank character and the suit
   glyph` fails with `expected 'T♦︎' to be 'T♦︎♦︎' // Object.is equality`. Revert.
3. Return `<></>` instead of the back when `cardText` is `null` → `keeps a card it cannot read face
   down` fails with `TestingLibraryElementError: Unable to find an accessible element with the role
   "img" and name "an unreadable card"`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a face-up card > names a face-up card in words` passes
- [ ] `a face-up card > prints the rank character and the suit glyph` passes
- [ ] `a face-up card > paints a red suit with the suit-red token and a black one without it` passes
- [ ] `a face-up card > keeps a card it cannot read face down` passes
- [ ] `TASK-030604`'s three `it` blocks are unedited and their assertions are byte-identical
- [ ] `npm run --silent test` reports `Tests  151 passed (151)`
- [ ] `PlayingCard.tsx` contains no hex, `rgb()` or `hsl()` literal
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
