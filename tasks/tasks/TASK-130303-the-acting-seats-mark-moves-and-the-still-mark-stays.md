---
schema: 2
id: TASK-130303
title: The acting seat's mark moves on the table, and the still mark stays beside it
type: task
status: ready
parent: STORY-1303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, table]
depends_on: [TASK-130302]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/theme.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - sh -c 'grep -q "acting-mark" web-client/src/table/SeatPlate.tsx && grep -q "border-l-accent" web-client/src/table/SeatPlate.tsx && grep -q "border-l-transparent" web-client/src/table/SeatPlate.tsx'
  - awk 'index($0, "aria-label") { n++ } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk 'index($0, "@keyframes pd-acting-seat") { n++ } END { exit (n != 1) }' web-client/src/styles/app.css
  - awk 'index($0, "acting-mark") { n++ } END { exit (n < 1) }' web-client/src/styles/app.css
  - awk 'index($0, "--pd-motion-") { n++ } END { exit (n < 2) }' web-client/src/styles/app.css
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent build
  - sh -c 'grep -q "pd-acting-seat" web-client/dist/assets/*.css'
  - sh -c 'grep -q "acting-mark" web-client/dist/assets/*.css'
  - sh -c 'grep -q "prefers-reduced-motion" web-client/dist/assets/*.css'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

On a rendered duel table the seat that is on turn carries the mark the merged cards draw: the
accent edge and the caps status line it already had, **plus** the motion `TASK-130301` minted. With
the animation stopped the seat is still marked, which is `ADR-0115` §1 in one sentence.

## Why this is the whole client change

The still form already ships. `SeatPlate.tsx` computes
`onTurn = status === "Your turn" || status === "Their turn"` and paints `border-l-accent` against a
2 px slot that is transparent and reserved off-turn. What is missing is the motion, and the
question `ADR-0115` §1 asks — *does the fact survive the animation stopping?* — is answered by
keeping that branch exactly as it is and adding to it.

`onTurn` is not touched, which is what makes the story's presence criterion true by construction:
`seat-status.ts` returns `Away` or `Timed out` before it ever reaches the turn (`ADR-0046` §1), so
an `AWAY` or `ABSENT` seat that is nonetheless `seatToAct` gets no status, no accent edge and no
mark. The tests below pin that rather than assume it.

## The trap in this file, read it before writing

`web-client/src/styles/theme.test.ts` asserts that **every line** inside `app.css`'s
`@theme static { … }` block is either a reset (`--x-*: initial;`) or a reference
(`--x: var(--pd-y);`). A `@keyframes` block or an `--animate-*` shorthand written inside that block
reddens a merged test. The keyframes and the utility go **outside** it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |
| `design/components/seat-and-pot.html` | read |
| `web-client/src/styles/theme.test.ts` | read |

## Scope

- **`app.css`, below the `@theme static` block:** the `@keyframes pd-acting-seat` block transcribed
  from the merged component card, and the class **`acting-mark`**, whose declarations read
  `var(--pd-motion-turn-period)` and `var(--pd-motion-turn-ease)` and never a literal duration.
  Tailwind v4.3.3's `@utility acting-mark { … }` is the idiomatic form; a plain
  `.acting-mark { … }` rule is the fallback if the utility does not reach the built CSS. The
  `verify:` block decides between them for you — it greps the production bundle for both
  `pd-acting-seat` and `acting-mark`, so a rule that compiles to nothing fails.
- **`SeatPlate.tsx`:** the `onTurn` branch keeps `border-l-accent` and gains `acting-mark`; the
  off-turn branch keeps `border-l-transparent` and gains nothing. Nothing else in the component
  moves — not `onTurn`'s definition, not `seatStatus`, not the status line's classes, not the
  button, not the stack.
- **The mark speaks nothing.** No `aria-label`, no `title`, no `role`, no text node, no new word.
  `SeatPlate.tsx` keeps exactly **one** `aria-label`, `the button`, and a gate pins it there.
  `Your turn` / `Their turn` are the mark's voice and already ship.
- **Run `npm run format` before `format:check`.** `prettier-plugin-tailwindcss` sorts class lists
  and will reorder the string you write.
- **The 390 × 664 fit must be untouched, and say why in the PR.** `ADR-0103` §1 is that the whole
  column fits without scrolling, and the story owes a `scrollHeight ≤ clientHeight` reading. The
  mark is paint inside a 2 px left border the plate reserves whether or not it is on turn, so no
  box changes size — write that sentence in the PR, or, if the transcribed drawing does change a
  box, paste the measured `scrollHeight` and `clientHeight` at 390 × 664 instead. It cannot be a
  `verify:` gate: `ADR-0089` §2b forbids a pull request waiting on a browser.

## Out of scope

- **`DuelTable.tsx` and the seat the view names.** That the mark lands on `view.seatToAct` and
  moves when the view does is `TASK-130304`, which opens `DuelTable.test.tsx` and
  `null-view.test.tsx`. This ticket changes only what a plate does with the `isToAct` prop it is
  handed.
- **The `null-view.test.tsx` contract.** `TASK-130304` amends it. This ticket cannot redden it:
  `Lobby.tsx` renders `WaitingTable` when `view === null`, and `WaitingTable` draws its own seat
  rows and never mounts `SeatPlate`, so nothing this ticket adds can reach that tree.
- **`prefers-reduced-motion` in client code.** The sheet holds the product's one block
  (`TASK-130301`, `ADR-0115` §4) and `app.css` imports it. jsdom implements no media query, so no
  client test can observe the stilling; the gate is the bundle grep.
- **The token sheet.** Merged in `TASK-130301`. If a value is wrong, that is a repair ticket
  against the sheet, not an edit here.
- **The countdown, the last-act mark, the chips.** `STORY-1304`, `STORY-1306`, `STORY-1307`.

## Tests

`SeatPlate.test.tsx` — three added to the five it has (measured 2026-09-02), so the file reports
**8**. Each queries `container.querySelectorAll(".acting-mark")`; `ADR-0100` §5 holds, so no
`data-testid`, no test-only prop and no exported setter.

| Test | Proves |
| --- | --- |
| `marks whichever seat is on turn, hero or rival` | **two inputs on `isViewer`.** With `isToAct: true, isViewer: true` exactly one `.acting-mark`; with `isToAct: true, isViewer: false` exactly one; with `isToAct: false` zero. A mark wired to the hero alone fails the second case — the fixture's own default is `isViewer: true`, so one input could not tell them apart |
| `keeps the still mark beside the moving one` | the on-turn plate's class list contains **both** `border-l-accent` and `acting-mark`, and the off-turn plate contains `border-l-transparent` and neither. `ADR-0115` §1: a reader whose system stopped the animation still sees which seat is marked, because the edge and the caps line are not the animation |
| `leaves an away or timed-out seat unmarked, even on turn` | with `presence: "AWAY", isToAct: true`: zero `.acting-mark`, no `border-l-accent`, and `Away` printed. With `presence: "ABSENT", isToAct: true`: the same, with `Timed out`. `ADR-0046` §1's order, which `seat-status.ts` already keeps and the mark inherits |

The five merged tests in this file assert text and labels only — measured, none of them reads a
class list — so none of their assertions moves and none is weakened.

## Acceptance criteria

- [ ] `SeatPlate.test.tsx` reports `Tests  8 passed (8)`
- [ ] `SeatPlate.marks whichever seat is on turn, hero or rival` passes
- [ ] `SeatPlate.keeps the still mark beside the moving one` passes
- [ ] `SeatPlate.leaves an away or timed-out seat unmarked, even on turn` passes
- [ ] `src/styles/theme.test.ts` still reports `Tests  1 passed (1)`
- [ ] `SeatPlate.tsx` contains `acting-mark`, `border-l-accent` and `border-l-transparent`, and
      exactly one `aria-label`
- [ ] `app.css` contains exactly one `@keyframes pd-acting-seat`, mentions `acting-mark`, and
      mentions `--pd-motion-` on at least two lines
- [ ] `cd web-client && npm run build` exits 0, and the built CSS under `dist/assets/` contains
      `pd-acting-seat`, `acting-mark` and `prefers-reduced-motion`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
