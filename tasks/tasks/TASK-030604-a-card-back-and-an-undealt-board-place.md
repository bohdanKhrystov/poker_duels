---
schema: 2
id: TASK-030604
title: A card back and an undealt board place
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030603]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +147 passed \(147\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows a face-down card with no rank and no suit in it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hides the second card of a pair from the accessibility tree'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names an undealt board place'
  - cd web-client && npm run check
---

## Goal

The two card shapes that hold a place without showing a card: the design's face-down back and its
dashed undealt outline, both sized from the `--w` their row sets.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/PlayingCard.tsx` | create |
| `web-client/src/table/PlayingCard.test.tsx` | create |
| `web-client/src/styles/tokens.css` | read — the `--pd-*` names used below |
| `design/screens/duel-table.html` | read — the `.pc`/`.back`/`.slot` rules. **Read only: never edit anything under `design/`** |

## Scope

- **How a component names a token.** Every colour, size, radius and font is a Tailwind utility whose
  name comes from the `@theme static` block in `web-client/src/styles/app.css`, which maps it to a
  `--pd-*` property in `web-client/src/styles/tokens.css`: `bg-card-back` → `--color-card-back` →
  `var(--pd-card-back)`. Where the theme has no utility — the resting card shadow and the back's
  stripes — reference the token directly in an arbitrary value. **A hex, `rgb()` or `hsl()` literal
  anywhere under `src/` fails `npm run check`** (`src/styles/color-literals.ts`), so the design's
  `rgba(...)` values may not be copied across; the two that matter are already tokens.
- The whole file, verbatim:

  ```tsx
  import type { ReactElement } from "react";

  // Every card is drawn from the `--w` reference width its row sets, exactly as
  // design/screens/duel-table.html does: the radius and both glyph sizes are
  // fractions of it, so one inherited property sizes a whole row of cards.
  const SHELL =
    "aspect-[5/7] w-[var(--w)] shrink-0 rounded-[calc(var(--w)*0.0625)]";

  /**
   * A face-down card. It carries no rank and no suit at all — not in its text,
   * not in an attribute — because an empty `holeCards` means "not entitled to
   * see", and a placeholder that knew the card would be the leak itself.
   */
  export function CardBack(props: { label?: string | null }): ReactElement {
    const label = props.label ?? null;
    return (
      <span
        {...(label === null
          ? { "aria-hidden": true }
          : { role: "img", "aria-label": label })}
        className={`${SHELL} bg-card-back [background-image:var(--pd-card-back-stripes)] shadow-[var(--pd-shadow-card)] forced-colors:border forced-colors:border-[CanvasText]`}
      />
    );
  }

  /** A board place the server has not dealt: the design's dashed outline. */
  export function CardSlot(props: { label: string }): ReactElement {
    return (
      <span
        role="img"
        aria-label={props.label}
        className={`${SHELL} border border-dashed border-hairline`}
      />
    );
  }
  ```

- `--pd-card-back-stripes` and `--pd-shadow-card` are declared in `web-client/src/styles/tokens.css`
  (the `Cards & coin` and `Focus & elevation` groups). `EPIC-06`'s open `TASK-060210` only makes the
  *design* screens consume those same two names, so if it renames one this is a one-line change here.
- The design's `.back` also draws a 3px inset ring in `rgba(255,255,255,0.14)`. That literal cannot
  cross into `src/` and no token expresses it, so **the client's back is the fill, the stripes and
  the resting shadow, and no ring**. Say so in the PR; a ring needs an `EPIC-06` token first.
- `CardBack` with no `label` is `aria-hidden`: the design labels the *first* card of a face-down
  pair and hides the second, so a screen reader says "hidden hand" once, not twice.

## Out of scope

- `CardFace` — `TASK-030605`, in this same file. This ticket imports nothing from `card-text.ts`.
- Any row, gap or `--w` value. Sizing a row is the job of whoever renders one:
  `TASK-030606` and `TASK-030607`.
- The showdown's dimmed `.back.mucked` treatment. That state needs to know a hand ended without a
  reveal, which no `PlayerView` field says; it belongs with `STORY-0308`.

## Tests

`web-client/src/table/PlayingCard.test.tsx`, describe block
`"a card back and an undealt place"`.

| Test | Proves |
| --- | --- |
| `shows a face-down card with no rank and no suit in it` | rendering `<CardBack label="your rival's hidden hand" />` leaves `container.textContent` exactly `""`, the element's `aria-label` exactly the label given, and its `childElementCount` `0` |
| `hides the second card of a pair from the accessibility tree` | rendering `<CardBack />` leaves `screen.queryAllByRole("img")` equal to `[]` |
| `names an undealt board place` | `<CardSlot label="river card, not yet dealt" />` is findable by `getByRole("img", { name: "river card, not yet dealt" })` |

Three tests. One hundred and forty-four exist, so the suite reports **147**.

**Do not assert over `container.innerHTML` with a rank/suit regex.** It was tried and it fails on
the honest implementation: the class attribute contains `5/7`, `0.0625` and `3`, so `[2-9]` matches
the stylesheet. Assert over text and accessible names, which is where a rank could actually reach a
player.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 147 passed (147)` | the three ran and the hundred-and-forty-four before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats — including the colour-literal guard |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Give `CardBack` `role="img"` and `aria-label={label ?? ""}` unconditionally → `hides the second
   card of a pair from the accessibility tree` fails with `expected [ <span role="img" …(2)></span>
   ] to deeply equal []`. Revert.
2. Render `{label}` as the back's child instead of leaving it empty → `shows a face-down card with
   no rank and no suit in it` fails with `expected 'your rival\'s hidden hand' to be '' //
   Object.is equality`. Revert.
3. Delete `role="img"` and `aria-label` from `CardSlot` → `names an undealt board place` fails with
   `TestingLibraryElementError: Unable to find an accessible element with the role "img" and name
   "river card, not yet dealt"`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a card back and an undealt place > shows a face-down card with no rank and no suit in it` passes
- [ ] `a card back and an undealt place > hides the second card of a pair from the accessibility tree` passes
- [ ] `a card back and an undealt place > names an undealt board place` passes
- [ ] `npm run --silent test` reports `Tests  147 passed (147)`
- [ ] `npm run check` passes, which is the colour-literal guard reporting no literal was added
- [ ] Nothing under `design/` is modified: `git status --short design/` prints nothing
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
