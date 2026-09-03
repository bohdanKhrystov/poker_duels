---
schema: 2
id: TASK-130502
title: The two table cards carry the typed total in place, at the phone as well as the laptop
type: task
status: done
parent: STORY-1305
module: design
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [design, table, action-bar]
depends_on: [TASK-130501]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"total\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"total\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "aria-label=\"the total\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "aria-label=\"the total\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "aria-label=\"raise to amount\"") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "value=\"3,650\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "value=\"3,250\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "  .total {") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "  .total {") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "  .total {") { n++ } END { exit (n != 1) }' design/components/action-bar.html
  - sh -c 'grep -h "^  .total {" design/components/action-bar.html design/screens/duel-table.html design/screens/duel-table-states.html | sort -u | awk "END { exit (NR != 1) }"'
  - awk 'index($0, "<span class=\"stepper\">") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "<span class=\"stepper\">") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"chip") { n++ } END { exit (n != 10) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"chip") { n++ } END { exit (n != 15) }' design/screens/duel-table-states.html
  - awk 'index($0, "viewport phone") { n++ } END { exit (n != 5) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 6) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "3,650") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "3,250") { n++ } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"notice\"") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"notice\"") { n++ } END { exit (n != 0) }' design/screens/duel-table-states.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two table cards draw the amount as the merged component card now draws it — a field in the
stepper's readout slot — so the human can see the typed total **in place**, in the `390 × 664` phone
frame that `ADR-0103` §1's fit is measured in, and not only in a components pane.

## Why this is its own diff

`TASK-130501` decides what the field looks like and what its refusals say; this ticket only carries
that decision into the two screens, exactly as `TASK-130402` carried the last-act mark. The two jobs
have different reviewers' questions — *is this the right drawing?* versus *did the copy land
unchanged?* — and the second is a transcription a `haiku` run does well.

**The phone frame is the point.** `design/screens/duel-table.html` is the only card declaring the
`390 × 664` box, and the sizing row there already carries five chips **and** a stepper. Putting the
field in the stepper's readout slot means the row drawn at the phone is a **superset** of the row
`ActionBar.tsx` will ship (the client has no stepper and builds none — `DEC-102` is open), so a fit
that holds here holds a fortiori for the client.

## What is already true, measured on `develop` 2026-09-03

| Marker | `duel-table.html` | `duel-table-states.html` |
| --- | --- | --- |
| `<span class="stepper">` | 2 | 3 |
| `class="chip` | 10 | 15 |
| `class="total"` / `class="notice"` | 0 / 0 | 0 / 0 |
| `aria-label="raise to amount"` | 2 | 0 |
| `3,650` / `3,250` | 4 / 0 | 0 / 6 |
| `viewport phone` / `class="frame"` | 5 / 6 | 0 / 3 |
| `@keyframes` | 1 | 1 |

- The readout markup is `<span aria-label="raise to amount">3,650</span>` on `duel-table.html`
  (twice) and a bare `<span>3,250</span>` on `duel-table-states.html` (three times).
- All three of `duel-table-states.html`'s bars are `<div class="bar off">`, whose sizing row is
  `visibility: hidden`. The field still goes in: that card's own note says the hidden row **mirrors
  the live row's content** so the rows wrap at the same points and the bar's height matches the bar
  it replaced. A hidden row missing the field would break that mirror.
- Neither screen card draws a notice line at all, and neither has a `.bar.disabled` rule — the
  component card is the only card with one.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | modify |
| `design/components/action-bar.html` | read |

## Scope

- **Copy the merged `.total` rule character for character** into each card's `<style>` block, beside
  its `.stepper` rules, at the same column-3 indentation and **as one physical line**, which is how
  `TASK-130501` wrote it. A gate greps `^  .total {` out of all three files, `sort -u`s the result
  and fails unless exactly one distinct line comes back — so a re-derived, re-wrapped or re-indented
  rule reddens the ticket. (`sort -u | awk 'END { exit (NR != 1) }'` is not a vacuous gate: a
  missing rule yields `NR == 0` and fails too; and the three `  .total {` count gates beside it stop
  it passing when only two of the three files carry the rule at all.)
- **`duel-table.html`, twice:** `<span aria-label="raise to amount">3,650</span>` becomes
  `<input class="total" aria-label="the total" value="3,650">`. The `−` and `+` buttons and their
  own `aria-label`s do not move.
- **`duel-table-states.html`, three times:** `<span>3,250</span>` becomes
  `<input class="total" aria-label="the total" value="3,250">`. Same rule for the `−` and `+`.
- **`aria-label="the total"` is the client's own accessible name** (`TASK-130504`), so the card and
  the shipped control answer to the same name. `raise to amount` goes to 0 on `duel-table.html`
  because that span no longer exists.
- **Nothing else moves.** No frame is added, no chip changes, no `@keyframes` is touched, and the
  totals stay the totals: `3,650` still appears 4 times on `duel-table.html` and `3,250` still 6
  times on `duel-table-states.html`, because the digits move from a `<span>` body into an
  `<input>`'s `value` on the same line. Gates pin all of it.

## Out of scope

- **The three refusal frames and the notice line.** They live on the component card
  (`TASK-130501`); neither screen card draws a notice today and neither gains one here. Gates pin
  `class="notice"` at 0 in both. That these screens are a line short of the shipped bar is
  **pre-existing** and is not this story's to repair — it is a card gap worth a ticket of its own,
  not a widening of this one.
- **The stepper's ± buttons and `DEC-102`.** Drawn, unchanged, still unbuilt.
- **Any client code.** `ActionBar.tsx` is `TASK-130504`.
- **Re-judging the drawing.** If the field looks wrong here, that is a repair against
  `TASK-130501`'s card, not a second opinion in this diff.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block: ten say the field
landed, one proves the rule was copied rather than rewritten, and eleven refuse any other movement
on either card.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `class="total"` appears exactly 2 times on `duel-table.html` and exactly 3 on
      `duel-table-states.html`, and `aria-label="the total"` matches those counts
- [ ] `aria-label="raise to amount"` appears 0 times on `duel-table.html`
- [ ] `value="3,650"` appears exactly twice and `value="3,250"` exactly three times
- [ ] The `^  .total {` line is byte-identical across `action-bar.html`, `duel-table.html` and
      `duel-table-states.html` — `sort -u` returns exactly one line
- [ ] `<span class="stepper">` is still 2 and 3, `class="chip` still 10 and 15, `viewport phone`
      still 5, `class="frame"` still 6 and 3, `@keyframes` still 1 in each
- [ ] `3,650` still appears 4 times and `3,250` still 6 times
- [ ] `class="notice"` appears 0 times on both screen cards
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
