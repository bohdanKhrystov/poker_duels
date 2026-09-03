---
schema: 2
id: TASK-130704
title: The two table cards carry the clock and both timebanks, and the host-alone frames carry none
type: task
status: ready
parent: STORY-1307
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [design, table, clock]
depends_on: [TASK-130703]
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/class="clock/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="clock"/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="clock on-timebank"/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="timebank"/, "&") } END { exit (n != 6) }' design/screens/duel-table.html
  - awk '{ n += gsub(/Timebank&nbsp;3:00/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/Timebank&nbsp;1:12/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/Timebank&nbsp;2:47/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Host alone") { seen = 1 } seen { n += gsub(/class="clock/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "Host alone") { seen = 1 } seen { n += gsub(/class="timebank/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/no clock and no timebank — nobody is on turn before the duel begins/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="clock/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="clock running-out"/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="timebank"/, "&") } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/Timebank&nbsp;3:00/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/Timebank&nbsp;1:12/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/\.clock\.running-out/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/\.clock\.on-timebank/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/\.clock\.expired/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/\.clock\.running-out/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/\.clock\.on-timebank/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/\.clock\.expired/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/--pd-warn/, "&") } END { exit (n < 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/--pd-warn/, "&") } END { exit (n < 2) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/animation:/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/animation:/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/animation-/, "&") } END { exit (n != 4) }' design/screens/duel-table.html
  - awk '{ n += gsub(/animation-/, "&") } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="frame"/, "&") } END { exit (n != 7) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="frame"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="note"/, "&") } END { exit (n != 4) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="seat/, "&") } END { exit (n != 14) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="seat/, "&") } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 6) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="bet-line"/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="bet-line"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 12) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 7) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 36) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 21) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/forfeit/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/forfeit/, "&") } END { exit (n != 0) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two composed table cards carry the clock in place: one countdown at the seat on turn, a
timebank figure at **both** plates of every live frame, and **nothing at all** in the four
host-alone frames — and the phone frame at 390 × 664 still holds the whole column.

## Why this is a transcription and not a design

`TASK-130701`–`TASK-130703` merged the clock's vocabulary: `--pd-warn` inlined, `.clock`,
`.clock.running-out`, `.clock.on-timebank`, `.clock.expired`, `.timebank`, and ten drawn states, all
judged at the pane. This ticket copies those rules character for character into the two screen
cards and puts the figures where the composed table already puts the plate they belong to. Nothing
new is designed. `check-drift.sh`'s value clause compares every `--pd-` a card inlines against the
sheet, so a mistyped value fails rather than drifting.

**Both `.clock` and `.timebank` sit in the same relation to the plate's other children as they do
on the component card.** Placement was decided there, under the human's eye; re-deciding it here
would fork one plate into three.

## What is already true, measured on `develop` 2026-09-03

| Marker | `duel-table.html` | `duel-table-states.html` |
| --- | --- | --- |
| `class="frame"` | 7 — laptop, phone, *the bet-line retired*, and **four** `Host alone` | 3 |
| `class="seat` / `class="seat on-turn` | 14 / 3 | 6 / 1 |
| `class="chips"` | 6 — two seats × three live frames | 6 |
| `class="pot"` / `class="bet-line"` | 3 / 2 | 3 / 3 |
| `class="pile` / `class="disc"` | 12 / 36 | 7 / 21 |
| `class="note"` | 3 | 3 |
| `@keyframes` / `animation:` / `animation-` / `transition:` | 2 / 3 / 4 / 0 | 2 / 3 / 4 / 0 |
| `@media (prefers-reduced-motion: reduce)` | 1 | 1 |
| `--pd-warn` / `class="clock` / `class="timebank"` | 0 / 0 / 0 | 0 / 0 / 0 |

**These are not the numbers `TASK-130602` recorded.** `TASK-130603` added a seventh frame to
`duel-table.html` after that ticket was written, so `class="frame"` went 6 → 7 and `class="chips"`
4 → 6. Everything above was re-measured for this ticket; do not carry a count over from an earlier
one.

**The host-alone frames already carry no stack numeral at all** — that is why `class="chips"` reads
6 on a file with seven frames. `ADR-0110` §3 forbids a game fact there, and there is no seat on turn
before the rival arrives, so a clock would be doubly wrong.

**`class="chip"` on both cards is the sizing button**, not a chip and not a clock. The drawn
countdown is `.clock`, so no gate here can conflate them.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | modify |
| `design/components/seat-and-pot.html` | read |
| `docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **Copy from the merged component card into each screen card's `<style>`**, unchanged:
  `--pd-warn: #c99a4a;` inlined in the card's own `:root`, and the `.clock`,
  `.clock.running-out`, `.clock.on-timebank`, `.clock.expired` and `.timebank` rules. All five
  rules go into both cards even where a card draws only some of the states — the two files must
  not diverge, which is `TASK-130602`'s precedent for `.pile.flying` and `.pile.stilled`. Each card
  keeps its **one** existing `@media (prefers-reduced-motion: reduce)` block; none is added.
- **`duel-table.html` — three clocks and six timebanks, all inside the three live frames.**
  - **The laptop frame and the 390 × 664 phone frame are the same markup** (`ADR-0103` §4: *"the
    only difference between them is the width and height of the box they are drawn in"*), so both
    carry the identical pair: `<span class="clock">24</span>` at the hero's plate — the hero holds
    the turn in both — `Timebank&nbsp;3:00` at the rival's plate and `Timebank&nbsp;1:12` at the
    hero's.
  - The third frame, *Phone — the bet-line retired*, carries
    `<span class="clock on-timebank">2:47</span>` at the hero's plate, with `Timebank&nbsp;3:00`
    at the rival's and `Timebank&nbsp;2:47` at the hero's. It is a phone frame, so it is where the
    **widest** figures are proven to fit.
  - Two different bank values, deliberately: one value on both plates cannot show that the two
    banks are two facts.
- **`duel-table.html` — the four `Host alone` frames gain nothing**, and say so once, in the
  existing `.note` idiom, **verbatim**:
  `no clock and no timebank — nobody is on turn before the duel begins`
  Two region gates count `class="clock` and `class="timebank` from the first `Host alone` heading
  to the end of the file and require **zero** of each.
- **`duel-table-states.html` — one clock and six timebanks.**
  - Frame 1 (*Waiting — their turn on the turn card*): the **rival** holds the turn, so
    `<span class="clock running-out">6</span>` goes at the rival's plate. It is the only frame in
    either card that draws the urgent treatment, which is why `--pd-warn` is used and not merely
    declared.
  - Frames 2 and 3 (*Showdown* and *Fold*) carry **no clock**: the hand has ended and no seat is on
    turn. `ADR-0108` §1 — *"a runout, the rival's turn and the gap between hands spend nothing."*
  - All three frames carry both timebanks: `Timebank&nbsp;3:00` at the rival's plate and
    `Timebank&nbsp;1:12` at the hero's. A bank is a fact about the duel, not about the hand, so it
    stands between hands.
- **No frame is added and no figure moves.** `class="frame"`, `class="seat`, `class="chips"`,
  `class="pot"`, `class="bet-line"`, `class="pile` and `class="disc"` are all pinned at today's
  numbers on both cards. If a clock forces a figure to move to fit, **stop and say so in the PR**
  rather than editing the number.
- **Neither card gains `@keyframes`, `animation:`, `animation-…` or `transition:`.** Four gates per
  card pin the counts unchanged. The clock states nothing in motion (`ADR-0115` §§1, 3), and the
  gates count the strings and cannot tell a comment from a declaration, so do not write them in
  prose either.
- **`forfeit` appears on neither card** (`ADR-0046` §5, `ADR-0108` §3).

## The fit, and what to do when it does not

The seat plate at 390 px now holds `.who`, `.last-act`, `.dealer`, `.clock`, `.timebank`,
`.chips` and `.pile` — seven children where the epic began with four. This is the last card in
`EPIC-13`'s sequence and it proves `ADR-0103` against everything merged before it.

`ADR-0089` §2b forbids a pull request waiting on a browser, so the fit is **not** a gate. Instead:

- **The PR states in prose** whether the 390 × 664 phone frame's column still fits — `scrollHeight`
  against `clientHeight` — and whether any plate's flex row wraps.
- **If it does not fit, stop.** `ADR-0103` §3's give list is exhaustive and ends at *"Nothing else
  gives. Both seat plates' names and stacks … keep their type size, their labels and their place"*,
  and the ADR is explicit: *"If the list runs out before the column fits, that is a decision to
  re-open, not a scroll to accept."* Register a `DEC` naming which element ran out of room. Do not
  shrink a figure, drop the dealer button, truncate a label or add a scroll.
- The human's pane verdict decides the look either way, and it may trail the merge (`ADR-0024` §3,
  `ADR-0091` §3).

## Out of scope

- **The component card.** Merged in `TASK-130701`–`TASK-130703`. A wrong value is a repair ticket
  against that card, not an edit here.
- **`design/components/action-bar.html` and `playing-card.html`.** No clock reaches them.
- **Any client or server code.** `STORY-1308` carries the server and the wire; `STORY-1309` draws
  the countdown in the client. This story draws.
- **The line under the table**, and the replacement for *"The duel is paused."* — `STORY-1309`,
  against these cards (`ADR-0108` §5).
- **Any away or timed-out frame on a screen card.** `ADR-0108` §4's table is drawn on the component
  card (`TASK-130703`); adding a fourth composed frame for it is a card nobody has asked for and is
  not ticketed.
- **A second countdown of any kind** — `ADR-0108` *Consequences* forecloses it by construction.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports (`ADR-0089` §2b).
The gates are the `verify:` block: twenty-one say what must now be on the two cards, twenty-eight
refuse what must not have moved, and `check-drift.sh` says every inlined `--pd-` value still equals
the sheet's.

The refusals that matter most are the two **region gates**: `awk` sets a flag at the first
`Host alone` heading and counts `class="clock` and `class="timebank` from there to the end of the
file, requiring zero of each. A clock drawn on a table that is not a duel fails them, which is
`ADR-0110` §3 made mechanical. Next after those, `class="clock` = 1 on `duel-table-states.html`
is the one that says the two ended frames grew no countdown.

| Marker | `duel-table.html` today → after | `duel-table-states.html` today → after |
| --- | --- | --- |
| `class="clock` | 0 → **3** | 0 → **1** |
| `class="clock"` / `class="clock on-timebank"` | 0 / 0 → **2 / 1** | n/a |
| `class="clock running-out"` | n/a | 0 → **1** |
| `class="timebank"` | 0 → **6** | 0 → **6** |
| `Timebank&nbsp;3:00` / `1:12` / `2:47` | 0 / 0 / 0 → **3 / 2 / 1** | 0 / 0 → **3 / 3** |
| `class="clock` / `class="timebank` after `Host alone` | 0 → **0** | n/a |
| `--pd-warn` | 0 → **≥ 2** | 0 → **≥ 2** |
| `.clock.running-out` / `.clock.on-timebank` / `.clock.expired` | 0 → **1 / 1 / 1** | 0 → **1 / 1 / 1** |
| `class="note"` | 3 → **4** | 3 → 3 |
| `class="frame"` / `class="seat` / `class="chips"` | 7 / 14 / 6 → **unchanged** | 3 / 6 / 6 → **unchanged** |
| `class="pot"` / `class="bet-line"` / `class="pile` / `class="disc"` | 3 / 2 / 12 / 36 → **unchanged** | 3 / 3 / 7 / 21 → **unchanged** |
| `@keyframes` / `animation:` / `animation-` / `transition:` | 2 / 3 / 4 / 0 → **unchanged** | 2 / 3 / 4 / 0 → **unchanged** |
| `@media (prefers-reduced-motion: reduce)` | 1 → **1** | 1 → **1** |
| `forfeit` | 0 → **0** | 0 → **0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `duel-table.html` contains `class="clock` exactly 3 times — `class="clock"` twice and
      `class="clock on-timebank"` once — and `class="timebank"` exactly 6 times
- [ ] `duel-table.html` contains `Timebank&nbsp;3:00` 3 times, `Timebank&nbsp;1:12` twice and
      `Timebank&nbsp;2:47` once
- [ ] `duel-table.html` contains **zero** `class="clock` and **zero** `class="timebank` at or after
      its first `Host alone` heading, and the note
      `no clock and no timebank — nobody is on turn before the duel begins` exactly once
- [ ] `duel-table-states.html` contains `class="clock` exactly once, as `class="clock running-out"`,
      and `class="timebank"` exactly 6 times, with `Timebank&nbsp;3:00` and `Timebank&nbsp;1:12`
      three times each
- [ ] Both cards contain the CSS rules `.clock.running-out`, `.clock.on-timebank` and
      `.clock.expired` exactly once each, and mention `--pd-warn` at least twice
- [ ] Both cards still contain `@keyframes` twice, `animation:` 3 times, `animation-` 4 times,
      `transition:` zero times and exactly one `@media (prefers-reduced-motion: reduce)`
- [ ] `class="frame"`, `class="seat`, `class="chips"`, `class="pot"`, `class="bet-line"`,
      `class="pile` and `class="disc"` are unchanged on both cards — 7/14/6/3/2/12/36 and
      3/6/6/3/3/7/21
- [ ] `forfeit` appears on neither card
- [ ] The PR states, in prose, whether the 390 × 664 frame's column still fits and whether any
      plate's row wraps
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
