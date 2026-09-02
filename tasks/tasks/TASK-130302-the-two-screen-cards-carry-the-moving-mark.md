---
schema: 2
id: TASK-130302
title: The two screen cards carry the moving mark, and nothing else on them moves
type: task
status: backlog
parent: STORY-1303
module: design
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [design, table]
depends_on: [TASK-130301]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "--pd-motion-") { n++ } END { exit (n < 3) }' design/screens/duel-table.html
  - awk 'index($0, "--pd-motion-") { n++ } END { exit (n < 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "@keyframes pd-acting-seat") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "@keyframes pd-acting-seat") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "@media (prefers-reduced-motion: reduce)") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "@media (prefers-reduced-motion: reduce)") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 16) }' design/screens/duel-table.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 24) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 6) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "viewport phone") { n++ } END { exit (n != 5) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"seat on-turn") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"seat on-turn") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "Pot&nbsp;") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "Your turn") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "Their turn") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "stilled") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "stilled") { n++ } END { exit (n != 0) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two cards that draw the duel table show the acting seat's mark the way the merged component
card draws it, so a reader of either card sees the mark move rather than sees a still mark and a
note about one — `EPIC-13`'s *a card draws every state of what it draws*, applied to the two
screens that copy `.seat.on-turn`.

## Why both files, and why no new frame

`grep -rl on-turn design/` finds exactly three files: `components/seat-and-pot.html`,
`screens/duel-table.html` and `screens/duel-table-states.html`. The first is canonical and
`TASK-130301` drew it. The other two declare `.seat.on-turn` as *"a faithful copy of
components/seat-and-pot.html"*, so leaving them behind would draw the same class two ways — a mark
that pulses at the hero on one card and stands still at the rival on another.

**No frame is added, and none is needed.** `duel-table.html`'s two live frames already put the hero
on turn with the rival waiting, and `duel-table-states.html`'s `Waiting — their turn on the turn
card` frame already puts the rival on turn with the hero waiting. Between them both arrangements are
drawn; `TASK-130301`'s card owns the *at rest* state. Adding frames here would copy sixty lines of
table markup to say something already on screen, and would move `role="img"` off the number three
earlier tickets pinned it at.

## What is already true, measured on `develop` 2026-09-02

| Marker | `duel-table.html` | `duel-table-states.html` |
| --- | --- | --- |
| `class="frame"` | 6 | 3 |
| `role="img"` | 16 | 24 |
| `viewport phone` | 5 | 0 |
| `class="seat on-turn` | 2 | 1 |
| `@keyframes`, `prefers-reduced-motion` | 0 | 0 |

Every one of those is a refusal gate below, held at today's value.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | modify |
| `design/components/seat-and-pot.html` | read |
| `docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md` | read |

## Scope

- **Copy, do not compose.** Into each screen card, transcribe from the merged
  `seat-and-pot.html`: the `--pd-motion-*` declarations into the card's own `:root`, the
  `@keyframes pd-acting-seat` block, the motion added to `.seat.on-turn`, and one
  `@media (prefers-reduced-motion: reduce)` block. Values are copied character for character —
  `check-drift.sh`'s value clause compares each inlined `--pd-motion-*` against the sheet, so a
  retyped duration fails the gate.
- **Three lines of each head comment** say the mark now moves and name `ADR-0115` §§1–2 and
  `components/seat-and-pot.html` as the canonical.
- **Paint only, inside the slot the plate already reserves.** The seat plate's left border is 2 px
  and always present — `create-duel.html`'s dashed twin exists precisely so *"a rival claiming the
  seat changes pixels, never layout"*. The mark must stay inside that reservation or on a
  positioned pseudo-element, so `ADR-0103`'s 390 × 664 fit is unchanged and the phone frame's
  measurement does not move. **If the chosen drawing cannot: stop and say so in the PR.** That is
  `ADR-0103` §3's give list running out, which its own words make a `DEC` and not a wider ticket.
- **Buttons stay wrapped.** If any element is added, it goes inside the file's `.actions` idiom: a
  bare `.btn` in the `.table` flex column inherits `flex: 1` and balloons — `TASK-130201` found it.

## Out of scope

- **A new frame, at either card.** Gates pin `class="frame"` at 6 and 3 and `viewport phone` at 5.
- **A `stilled` row.** That state is drawn once, on the component card (`TASK-130301`). Gates pin
  `stilled` at zero in both files, so this ticket cannot drift into re-drawing it; the media query
  is what stills these two on a machine that asks.
- **The four host-alone frames.** Nothing on them is on turn — `WaitingTable` renders no seat plate
  — so there is nothing there to mark.
- **Every other number on these cards.** `role="img"` stays at 16 and 24, `Pot&nbsp;` at 2, `Your
  turn` at 2 and `Their turn` at 1. Nothing about the pot, the board, the bar or the invite moves.
- **Any client code.** `TASK-130303`.
- **`duel-end.html` and `rematch-states.html`.** Measured: neither mentions `on-turn`.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block: six say what must
now be on each card, twelve refuse anything else having moved, and `check-drift.sh` says the
tokens, the inlined values and the suit glyphs still hold.

The `ADR-0103` fit claim is **not** a gate for the same reason. It is a scope constraint above, and
the PR says in one sentence why the mark cannot have changed the box — or pastes the measurement if
it could have.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] Each of the two screen cards mentions `--pd-motion-` on at least three lines
- [ ] Each contains exactly one `@keyframes pd-acting-seat` and exactly one
      `@media (prefers-reduced-motion: reduce)`
- [ ] `role="img"` is still 16 in `duel-table.html` and 24 in `duel-table-states.html`
- [ ] `class="frame"` is still 6 and 3, and `viewport phone` still 5
- [ ] `class="seat on-turn` is still 2 and 1
- [ ] `Pot&nbsp;` is still 2, `Your turn` still 2 and `Their turn` still 1
- [ ] `stilled` appears zero times in both files
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
