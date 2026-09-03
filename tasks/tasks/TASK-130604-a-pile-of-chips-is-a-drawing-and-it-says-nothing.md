---
schema: 2
id: TASK-130604
title: A pile of chips is a drawing, it arrives, and it says nothing
type: task
status: ready
parent: STORY-1306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table]
depends_on: [TASK-130603]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ChipPile.test.tsx 2>&1 | grep -qE '^ *Tests +3 passed \(3\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/theme.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/tokens.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - awk '{ n += gsub(/@keyframes pd-chip-flight/, "&") } END { exit (n != 1) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/--pd-chip-size/, "&") } END { exit (n < 1) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/--pd-chip-face/, "&") } END { exit (n < 1) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/--pd-motion-chip-flight/, "&") } END { exit (n < 1) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/--pd-motion-chip-travel/, "&") } END { exit (n < 1) }' web-client/src/styles/app.css
  - awk '{ n += gsub(/aria-hidden="true"/, "&") } END { exit (n != 1) }' web-client/src/table/ChipPile.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent build
  - sh -c 'test $(grep -o -- "pd-chip-flight" web-client/dist/assets/*.css | wc -l) -ge 2'
  - sh -c 'test $(grep -o -- "--pd-motion-chip-flight" web-client/dist/assets/*.css | wc -l) -ge 2'
  - sh -c 'test $(grep -o -- "--pd-motion-chip-travel" web-client/dist/assets/*.css | wc -l) -ge 2'
  - sh -c 'test $(grep -o -- "--pd-chip-size" web-client/dist/assets/*.css | wc -l) -ge 2'
  - sh -c 'test $(grep -o -- "chip-disc" web-client/dist/assets/*.css | wc -l) -ge 1'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The client has the chip the merged cards drew: `web-client/src/styles/app.css` carries
`@keyframes pd-chip-flight` and the `chip-pile`, `chip-disc` and `chip-flight` utilities, and
`web-client/src/table/ChipPile.tsx` is the one component that draws a pile — three discs, silent,
`aria-hidden`, arriving once on mount and then standing still.

## The two things this component's shape is for

**1. It takes no amount, so it cannot draw one.** `ChipPile` has **no props at all**. A pile whose
size depended on a figure would invent a denomination — *what one chip is worth* — a fact no server
ever sent and a product question nobody has asked (`ADR-0002`, `docs/vision.md` — *no money*). The
component is a constant drawing; the numeral beside it, at each call site, is the whole of the
amount. `ADR-0115` §1 falls out of that for free.

**2. It arrives and never leaves.** `pd-chip-flight` runs **from** an offset **to** the resting
position, once, with `animation-fill-mode: both`, so its final frame is the frame the element
would have had with no animation at all. Three consequences, and every one of them is why the
flight is safe:

- Under `prefers-reduced-motion: reduce` the sheet's one merged block sets
  `animation: none !important` on `*`, and the pile renders at its base declarations — which are
  the resting position. `ADR-0115` §2's *same surface at rest*, with nothing added and nothing
  hidden.
- The caller replays the flight by changing the element's **`key`**, which remounts it. A store
  frame that arrives mid-flight with the same amount changes no key and interrupts nothing.
- Nothing is ever animated **out**. An exit animation is the one shape whose stilled form is
  wrong — the chips would sit where they no longer are — and `ADR-0115` §1 forbids exactly that.

## The trap in `app.css`, read it before writing

`web-client/src/styles/theme.test.ts` asserts that **every line** inside `app.css`'s
`@theme static { … }` block is either a reset (`--x-*: initial;`) or a reference
(`--x: var(--pd-y);`). A `@keyframes` block or an `@utility` written inside that block reddens a
merged test. The keyframes and the three utilities go **outside** it, beside the merged
`acting-mark` and `last-act` utilities that are already there for the same reason.

## What is already true, measured on `develop` 2026-09-03

- `app.css` carries `@keyframes` 1 (`pd-acting-seat`), `transition:` **0**, and the merged
  `@utility acting-mark` / `@utility last-act`.
- **Tailwind 4.3.3's `@utility` form does emit** into the production bundle — `TASK-130303` proved
  it, so the plain-`.class` fallback it carried is not needed here.
- **A bundle grep for a class name proves the class exists, never that it styles anything.** The
  gates below therefore count **occurrences of the token names inside the built CSS**: the sheet
  contributes one occurrence as a declaration, and a second can only come from a utility or
  keyframe body that actually emitted. Measured on the merged precedent at this commit:
  `--pd-motion-turn-period` appears exactly **2** times in `dist/assets/*.css` and `pd-acting-seat`
  exactly **2**. An inert rule would leave 1.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |
| `web-client/src/table/ChipPile.tsx` | create |
| `web-client/src/table/ChipPile.test.tsx` | create |
| `design/components/seat-and-pot.html` | read |
| `web-client/src/styles/theme.test.ts` | read |

## Scope

- **`app.css`, below the `@theme static` block**, transcribed from the merged component card:
  - one `@keyframes pd-chip-flight` block whose `from` frame carries
    `var(--pd-motion-chip-travel)` and whose `to` frame is the resting state;
  - `@utility chip-pile` — the inline container the discs sit in;
  - `@utility chip-disc` — one disc, sized by `var(--pd-chip-size)`, faced by
    `var(--pd-chip-face)`, rimmed by `var(--pd-chip-edge)`;
  - `@utility chip-flight` — `animation-name: pd-chip-flight`, duration
    `var(--pd-motion-chip-flight)`, timing `var(--pd-motion-chip-ease)`, one iteration, fill mode
    `both`. Never a literal duration and never a literal length.
  - **No `prefers-reduced-motion` block here.** The sheet holds the product's one block
    (`ADR-0115` §4) and `app.css` imports it.
  - **No `transition:` anywhere in the file, comments included.** A CSS transition over a value
    the store replaces is `ADR-0102`'s own recorded failure; the gate counts the string and
    cannot tell a comment from a declaration, so do not write it in prose either.
- **`ChipPile.tsx`** — one exported function component, no props:

  ```tsx
  export function ChipPile(): ReactElement
  ```

  rendering a single `<span>` carrying `chip-pile chip-flight` and `aria-hidden="true"`, holding
  **exactly three** `<span className="chip-disc" />`. Three is a literal in this file and a
  literal in its test; a shared constant would let one edit move both and leave the test green.
  A `<span>` and not a `<div>`: it rides inside rows that already exist, so no box grows
  (`ADR-0103` §1).
- **The pile speaks nothing.** No `aria-label`, no `title`, no `role`, no text node, no new word.
  Exactly one `aria-hidden="true"` in the file, and a gate pins it.
- **Run `npm run format` before `format:check`** — `prettier-plugin-tailwindcss` sorts class lists
  and will reorder the string you write.

## Out of scope

- **Every call site.** `SeatPlate`, `PotStrip` and `DuelTable` mount this in `TASK-130605`,
  `TASK-130606` and `TASK-130607`. This ticket renders the pile nowhere on a real screen, which is
  why no merged suite can move.
- **A direction prop.** Whether a pile arrives from above or below is a drawing the human has not
  been shown; one keyframe, one direction. Additive later, not yet ticketed.
- **A pile that grows with the amount.** Refused above and in `TASK-130601` for the same reason;
  if the pane verdict asks for one it is a repair ticket **and** a product owner's `DEC`.
- **The token sheet and the cards.** Merged in `TASK-130601`–`TASK-130603`.
- **`chips.ts`.** `formatChips` is digit grouping and is not touched.

## Tests

`ChipPile.test.tsx` — a new file, **3** tests. `ADR-0100` §5 holds: no `data-testid`, no test-only
prop, no exported setter.

`ChipPile`

| Test | Proves |
| --- | --- |
| `draws three discs inside one pile` | `container.querySelectorAll(".chip-pile")` has length 1 and `.chip-disc` has length **3** — the literal, so a changed constant reddens this |
| `says nothing to anybody` | the root carries `aria-hidden="true"`; `container.querySelectorAll("[aria-label], [title]")` is empty; `container.textContent` is `""`. A pile that ever spoke an amount would reach a player exactly as printed text does |
| `carries the flight class beside the pile class` | the root's class list contains **both** `chip-pile` and `chip-flight`, and the root is a `SPAN`. The bundle gates below are what say those classes carry declarations; this says the component asks for them |

`theme.test.ts` (1) and `tokens.test.ts` (1) are pinned unmoved. Neither observes anything this
ticket changes — `theme.test.ts` reads only the lines **inside** `@theme static`, which this
ticket does not touch, and `tokens.test.ts` compares the two sheets, which merged in
`TASK-130601`.

**The bundle gates are the whole proof that the CSS is not inert.** `pd-chip-flight`,
`--pd-motion-chip-flight`, `--pd-motion-chip-travel` and `--pd-chip-size` are each required at
**two or more** occurrences in `dist/assets/*.css`: one is the declaration the sheet contributes,
and the second can only come from a keyframe or utility body that actually emitted. `chip-disc` is
required at one or more, which only an emitted rule can supply — the class name alone lives in
`.tsx`, and `.tsx` is not in the stylesheet.

## Acceptance criteria

- [ ] `ChipPile.test.tsx` reports `Tests  3 passed (3)`
- [ ] `ChipPile.draws three discs inside one pile` passes
- [ ] `ChipPile.says nothing to anybody` passes
- [ ] `ChipPile.carries the flight class beside the pile class` passes
- [ ] `src/styles/theme.test.ts` reports `Tests  1 passed (1)`
- [ ] `src/styles/tokens.test.ts` reports `Tests  1 passed (1)`
- [ ] `app.css` contains exactly one `@keyframes pd-chip-flight`, no `transition:` at all, and
      mentions `--pd-chip-size`, `--pd-chip-face`, `--pd-motion-chip-flight` and
      `--pd-motion-chip-travel`
- [ ] `ChipPile.tsx` contains exactly one `aria-hidden="true"`
- [ ] `cd web-client && npm run build` exits 0, and the built CSS under `dist/assets/` contains
      `pd-chip-flight`, `--pd-motion-chip-flight`, `--pd-motion-chip-travel` and `--pd-chip-size`
      at least **twice** each, and `chip-disc` at least once
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
