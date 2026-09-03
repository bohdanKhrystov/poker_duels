---
schema: 2
id: TASK-130702
title: Draw the clock on the timebank, holding at zero, and the plate after the server acted
type: task
status: ready
parent: STORY-1307
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table, clock]
depends_on: [TASK-130701]
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation:/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation-/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock/, "&") } END { exit (n != 5) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock on-timebank"/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock expired"/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/\.clock\.on-timebank/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/\.clock\.expired/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="timebank"/, "&") } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;2:47/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;0:00/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;3:00/, "&") } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;1:12/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat/, "&") } END { exit (n != 24) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat on-turn"/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat away"/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 24) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="l"/, "&") } END { exit (n != 27) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="last-act"/, "&") } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>Check</, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="dealer"/, "&") } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/forfeit/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/on timebank — the fresh allowance is spent and the bank is what falls/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/expired — the figure holds at zero, and nothing a player reads has changed yet/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/after the server acted — the mark is the consequence, and it arrived in a frame/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/components/seat-and-pot.html` draws the two states after the fresh 30 s is gone — *on
timebank* and *expired* — and, beside them, the plate as it stands **after** the server's act, so
the card shows that reaching zero changed nothing and that the frame did.

## Why *expired* is drawn twice, and which clause each row is

Two merged sources word this state differently, and the card must not pick one silently.

- [`ADR-0113`](../../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md)
  §6: *"A countdown whose deadline has passed and whose expiry frame has not arrived **holds at
  zero and does nothing** … The seat reads as **expired** — one of the four states item 4's card
  owes — **until** a server frame carries the consequence, and how that looks is the card's."*
- [`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
  §5, as `STORY-1307` glosses it: *"The expired state the card draws is the state **after** that
  frame, and the card says so."*

They name two consecutive moments. `ADR-0113` is later and specific, so **row F is the state named
*expired***: the figure at zero, the turn still on the seat, nothing else changed. **Row G is what
it becomes** when the frame lands: the turn has moved, the plate carries the act the server took,
and there is no countdown here at all. Drawing both discharges `ADR-0108` §5's *"the card says
so"* without reopening either ADR, and the two captions say which clause each row is.

This is also the state a reader is most likely to draw wrong. `ADR-0108` §5: *"nothing a player
reads changes when the countdown reaches zero until a server frame carries the consequence."*
There is no *Time's up*, no error colour, no sound, no disabled control and no second string —
`ADR-0046` §3's rule, re-applied at a wider occasion.

## What is already true, after `TASK-130701` merges

`seat-and-pot.html` carries: `class="seat` 21, `class="seat on-turn"` 4,
`class="seat on-turn stilled"` 2, `class="seat away"` 1, `class="l"` 24, `class="chips"` 21,
`class="dealer"` 7, `class="last-act"` 6, `class="pile` 6, `class="clock` 3, `class="timebank"` 4,
`>Check<` 1, `@keyframes` 2, `animation:` 4, `animation-` 4, `transition:` 0,
`@media (prefers-reduced-motion: reduce)` 1, `forfeit` 0. `.clock`, `.clock.running-out` and
`.timebank` exist as rules; `.clock.on-timebank` and `.clock.expired` do not.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | modify |
| `docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md` | read |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read |

## Scope

- **Two CSS rules**, beside the ones `TASK-130701` merged:
  - `.clock.on-timebank` — **visibly distinct from `.clock.running-out`**, by something other than
    the figure's format alone: colour, weight, a rule under it, a box. `ADR-0108` §5 requires it —
    *"the 30 s and the bank are visibly distinct… a player must be able to see the bank begin to
    spend"* — and which of those it is, is the human's eye (`ADR-0024` §3).
  - `.clock.expired` — the spent figure, quieter than either.
  - Both compose from tokens the sheet already declares. Inlining a further **declared** token in
    the card's `:root` is composing, not minting, and `check-drift.sh` clause 3 checks the value.
    If the treatment genuinely needs a value **the sheet does not have**, stop and say so in the
    PR: this ticket cannot reach the sheet, and minting is a repair against `TASK-130701`.
  - **Neither rule may carry `animation:`, `animation-…` or `transition:`.** Three gates pin the
    counts unchanged; the clock states nothing in motion (`ADR-0115` §§1, 3), and the gates count
    the strings and cannot tell a comment from a declaration.
- **Three rows**, appended to *The seat, in its states*, captions in the `.l` idiom, **verbatim**:

  | Row | Plate | Name / status | `.clock` | `.last-act` | `.timebank` | `.chips` | Caption, verbatim |
  | --- | --- | --- | --- | --- | --- | --- | --- |
  | E | `seat on-turn` | ImKate / `Their turn` | `<span class="clock on-timebank">2:47</span>` | none | `Timebank&nbsp;2:47` | `4,550` | `on timebank — the fresh allowance is spent and the bank is what falls` |
  | F | `seat on-turn` | ImKate / `Their turn` | `<span class="clock expired">0</span>` | none | `Timebank&nbsp;0:00` | `4,550` | `expired — the figure holds at zero, and nothing a player reads has changed yet` |
  | G | `seat` | ImKate / empty status | **none** | `Check` | `Timebank&nbsp;0:00` | `4,550` | `after the server acted — the mark is the consequence, and it arrived in a frame` |

  - Row E draws the countdown and the bank figure as **one number**, because while the bank spends
    they are one number (`ADR-0113` §3's second expression). The caption says so.
  - Row F keeps `on-turn`: the turn has not moved, the sweep has not landed, and the accent edge
    and pulse are exactly as they were. That is *zero is not an event*, drawn.
  - Row G's mark is `Check` — `ADR-0023`'s conduct at a decision where nothing is owed, and
    `ADR-0109`'s one mark at the seat that made the act. It is **not** labelled as the server's on
    the plate: `ADR-0046` §4 puts *The server checked for your rival.* in the line under the table,
    which is `STORY-1309`'s to place and is not drawn here.
  - No row gains a `.dealer` or a `.pile`; both counts are pinned unchanged.
- **`forfeit` appears nowhere**, in copy, caption or comment (`ADR-0046` §5, `ADR-0108` §3). The
  gate counts the string, so do not write it even to deny it.

## Out of scope

- **`AWAY` and `ABSENT`.** `TASK-130703`. `class="seat away"` is pinned at 1 here, so the merged
  `reconnecting…` row must still be standing when this ticket is done.
- **The two screen cards.** `TASK-130704`.
- **Any word for zero.** No *Time's up*, no *Expired* on the plate, no colour change on the status
  line, no control that becomes disabled. `ADR-0046` §3 and `ADR-0108` §5 both forbid it, and the
  only place *expired* is written is the caption naming the state.
- **The line under the table**, including *The server checked for your rival.* and whatever
  replaces *"The duel is paused."* — `STORY-1309` derives those against this card (`ADR-0108` §5).
- **Any client or server code**, and any motion on the clock.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports (`ADR-0089` §2b).
The gates are the `verify:` block: fourteen say what must now be on the card, thirteen refuse what
must not have moved, and `check-drift.sh` says every inlined `--pd-` value still equals the sheet's.

The refusal that matters most is the trio `@keyframes` 2 / `animation:` 4 / `animation-` 4. A
depleting ring or a pulsing zero would break all three, which is the point: the two states this
ticket adds are the two most tempting places to put a fact in motion.

| Marker | After `TASK-130701` | After this |
| --- | --- | --- |
| `class="clock` | 3 | **5** |
| `class="clock on-timebank"` / `class="clock expired"` | 0 / 0 | **1 / 1** |
| `.clock.on-timebank` / `.clock.expired` (the CSS rules) | 0 / 0 | **1 / 1** |
| `class="timebank"` | 4 | **7** |
| `Timebank&nbsp;2:47` / `Timebank&nbsp;0:00` | 0 / 0 | **1 / 2** |
| `Timebank&nbsp;3:00` / `Timebank&nbsp;1:12` | 3 / 1 | **3 / 1** |
| `class="seat` / `class="seat on-turn"` / `class="seat away"` | 21 / 4 / 1 | **24 / 6 / 1** |
| `class="chips"` / `class="l"` | 21 / 24 | **24 / 27** |
| `class="last-act"` / `>Check<` | 6 / 1 | **7 / 2** |
| `class="dealer"` / `class="pile` | 7 / 6 | **7 / 6** |
| `@keyframes` / `animation:` / `animation-` / `transition:` | 2 / 4 / 4 / 0 | **2 / 4 / 4 / 0** |
| `forfeit` | 0 | **0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `seat-and-pot.html` contains `class="clock` exactly 5 times, with `class="clock on-timebank"`
      and `class="clock expired"` exactly once each
- [ ] The CSS rules `.clock.on-timebank` and `.clock.expired` each appear exactly once
- [ ] `class="timebank"` is exactly 7, `Timebank&nbsp;2:47` exactly 1 and `Timebank&nbsp;0:00`
      exactly 2, with `Timebank&nbsp;3:00` and `Timebank&nbsp;1:12` unchanged at 3 and 1
- [ ] `class="seat` is exactly 24 and `class="seat on-turn"` exactly 6
- [ ] `class="seat away"` is still exactly 1 — the merged `reconnecting…` row is untouched
- [ ] `class="last-act"` is exactly 7 and `>Check<` exactly 2
- [ ] `class="dealer"` and `class="pile` are unchanged at 7 and 6
- [ ] `@keyframes` is still exactly 2, `animation:` exactly 4, `animation-` exactly 4 and
      `transition:` zero — the two new states added no motion
- [ ] Each of the three captions in the table above appears exactly once, verbatim
- [ ] `forfeit` appears nowhere on the card
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
