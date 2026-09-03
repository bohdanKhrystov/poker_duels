---
schema: 2
id: TASK-130703
title: Draw ADR-0108's presence table, and retire the grace window's row
type: task
status: done
parent: STORY-1307
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table, clock, presence]
depends_on: [TASK-130702]
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/reconnecting/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/presence outranks the turn — the ADR-0013 grace window, stated plainly in the same slot/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/\.seat\.away/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat away"/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat on-turn away"/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat on-turn"/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat/, "&") } END { exit (n != 26) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>Away</, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>Timed out</, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock"/, "&") } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="timebank"/, "&") } END { exit (n != 10) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;3:00/, "&") } END { exit (n != 5) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;0:00/, "&") } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 26) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="l"/, "&") } END { exit (n != 29) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="last-act"/, "&") } END { exit (n != 8) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>Fold</, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="dealer"/, "&") } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation:/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation-/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/forfeit/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/dropping the connection is never a way to gain time, stop time or freeze a rival/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/away, on turn — presence outranks the turn; the allowance and the bank are exactly the same/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/away, not on turn — nothing is spent and there is no countdown to draw/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/timed out — the clock is gone, the seat is played without a fresh one, and the bank stays spent/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/components/seat-and-pot.html` draws
[`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
§4's regime — an away seat **on** turn, an away seat **not** on turn, and a `Timed out` seat — and
the row drawn for `ADR-0013`'s grace window leaves with the window.

## Why the merged row has to go, not be edited around

The card carries one presence row today, and it is wrong twice over:

```html
<div class="seat away">
  <span class="who"><span class="name">ImKate</span>
    <span class="status">reconnecting…</span></span>
  <span class="dealer">D</span>
  <span class="chips">4,550</span>
</div>
<span class="l">presence outranks the turn — the ADR-0013 grace window, stated plainly in the same slot</span>
```

- **`reconnecting…` is not a string this product ships.** `ADR-0046` §1 fixes the status words as
  `Away` for `AWAY` and `Timed out` for `ABSENT`, and `web-client/src/table/seat-status.ts` returns
  exactly those. The card has been out of step with merged copy since before this epic.
- **The caption names a mechanism that is being retired.** `ADR-0108` §4 replaces `ADR-0013`'s
  fixed window with the timebank; `ADR-0113` §7 deletes `Room.gracePeriods`, `isPaused`,
  `DUEL_PAUSED` and `graceRemainingMillis` outright. A caption citing the grace window would be a
  card describing a thing no server can do.

Two rows cannot both be *the away row*, so the replacement is a removal plus three additions, and
gates count the old caption and the word `reconnecting` down to zero.

## The rule these three rows exist to draw

`ADR-0108` §4, verbatim in its own table:

| The seat | What its time is |
| --- | --- |
| **Away** (socket dropped), on turn | *exactly the same 30 s plus remaining bank* |
| **Away**, clock exhausted | the seat is `ABSENT`, played *without a fresh clock* at every decision |
| **Away**, not on turn | *spends nothing and shows no countdown* |

And §1's sentence, which row H exists to make visible: *"dropping the connection is never a way to
gain time, stop time, or freeze a rival."* So **the away seat's clock and bank are drawn
identically to a present seat's** — same figure, same size, same colour. A rule that dimmed them
would state, in CSS, that a dropped socket changes what a seat is owed, which is the one thing this
ADR exists to refuse. `\.seat\.away` is pinned at **2** — the two merged rules, which dim the name
and the stack and nothing else — so no third rule can reach `.clock` or `.timebank`.

## What is already true, after `TASK-130702` merges

`seat-and-pot.html` carries: `class="seat` 24, `class="seat on-turn"` 6, `class="seat away"` 1,
`\.seat\.away` 2, `class="l"` 27, `class="chips"` 24, `class="dealer"` 7, `class="last-act"` 7,
`class="pile` 6, `class="clock` 5, `class="clock"` 2, `class="timebank"` 7,
`Timebank&nbsp;3:00` 3, `Timebank&nbsp;0:00` 2, `>Fold<` 1, `>Away<` 0, `>Timed out<` 0,
`reconnecting` 1, `@keyframes` 2, `animation:` 4, `animation-` 4, `transition:` 0, `forfeit` 0.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | modify |
| `docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md` | read |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read |
| `web-client/src/table/seat-status.ts` | read |

## Scope

- **Delete** the `class="seat away"` row quoted above **and its caption**, both entirely.
- **Add three rows** to *The seat, in its states*, captions in the `.l` idiom, **verbatim**:

  | Row | Plate | Status word | `.clock` | `.last-act` | `.dealer` | `.timebank` | `.chips` | Caption, verbatim |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | H | `seat on-turn away` | `Away` | `<span class="clock">24</span>` | none | `D` | `Timebank&nbsp;3:00` | `4,550` | `away, on turn — presence outranks the turn; the allowance and the bank are exactly the same` |
  | I | `seat away` | `Away` | **none** | none | none | `Timebank&nbsp;3:00` | `4,550` | `away, not on turn — nothing is spent and there is no countdown to draw` |
  | J | `seat away` | `Timed out` | **none** | `Fold` | none | `Timebank&nbsp;0:00` | `4,550` | `timed out — the clock is gone, the seat is played without a fresh one, and the bank stays spent` |

  - **The status word is in the plain `.status` slot, never `.status turn`.** `ADR-0046` §1:
    presence outranks the turn, so an away seat on turn reads `Away`, not `Their turn`. Row H keeps
    `on-turn`'s accent edge and pulse, because the turn *is* on it — that combination is the row's
    whole content, and it is why the plate is `seat on-turn away` rather than either alone.
  - Row H carries the `D` the deleted row carried, so `class="dealer"` stays 7.
  - Row H's `<span class="clock">24</span>` is byte-identical to `TASK-130701`'s row A. That
    identity is the drawing of *"exactly the same 30 s plus remaining bank"*.
  - Row J's mark is `Fold` — `ADR-0023`'s other conduct, at a decision where a bet is faced, beside
    `TASK-130702`'s `Check`. Two acts, because one cannot show that the conduct is read from the
    legal set rather than fixed.
- **One comment**, above the three rows, carrying this sentence **verbatim** and citing
  `ADR-0108` §§1, 4:

  `dropping the connection is never a way to gain time, stop time or freeze a rival`

- **No new CSS rule.** `.seat.away` keeps its two merged declarations and gains no third; `.clock`
  and `.timebank` are untouched. Gates pin `\.seat\.away` at 2 and `animation:` / `animation-` /
  `transition:` / `@keyframes` unchanged.
- **`forfeit` appears nowhere**, in copy, caption or comment. A `Timed out` seat *is still playing*
  — that is `ADR-0046` §5's own reason the word is false, and `ADR-0108` §3 keeps it false.

## Out of scope

- **The line under the table.** *"The duel is paused."* loses its occasion when the clock lands,
  and what stands in its place is derived under `ADR-0046`'s register **by `STORY-1309`, against
  this card** (`ADR-0108` §5). Do not write a replacement sentence here; the only strings this card
  gains are the two status words `ADR-0046` §1 already ships.
- **`DEC-108`** — whether the action bar may stay enabled under that sentence. Open, and
  `ADR-0108` does not answer it.
- **The two screen cards.** `TASK-130704`.
- **Any client or server code**, `seat-status.ts` included: it is in the budget to be **read**, so
  the card quotes the shipped words rather than inventing them.
- **Any motion on the clock**, and any rule that makes an away seat's clock look different from a
  present seat's.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports (`ADR-0089` §2b).
The gates are the `verify:` block: fifteen say what must now be on the card, thirteen refuse what
must not have moved or survived, and `check-drift.sh` says every inlined `--pd-` value still
equals the sheet's.

Two are worth naming. `reconnecting` = 0 and the old caption = 0 are the only proof the retired row
actually left rather than being duplicated beside its replacement. `\.seat\.away` = 2 is the proof
that no CSS dims an away seat's clock — it is the *"a dropped socket buys no time"* rule expressed
as a count, and it is escaped because an unescaped `.seat.away` also matches `"seat away"` in the
markup and would read 3.

| Marker | After `TASK-130702` | After this |
| --- | --- | --- |
| `reconnecting` / the old caption | 1 / 1 | **0 / 0** |
| `class="seat away"` / `class="seat on-turn away"` | 1 / 0 | **2 / 1** |
| `\.seat\.away` (the CSS rules) | 2 | **2** |
| `class="seat` / `class="seat on-turn"` | 24 / 6 | **26 / 6** |
| `>Away<` / `>Timed out<` | 0 / 0 | **2 / 1** |
| `class="clock` / `class="clock"` | 5 / 2 | **6 / 3** |
| `class="timebank"` | 7 | **10** |
| `Timebank&nbsp;3:00` / `Timebank&nbsp;0:00` | 3 / 2 | **5 / 3** |
| `class="chips"` / `class="l"` | 24 / 27 | **26 / 29** |
| `class="last-act"` / `>Fold<` | 7 / 1 | **8 / 2** |
| `class="dealer"` / `class="pile` | 7 / 6 | **7 / 6** |
| `@keyframes` / `animation:` / `animation-` / `transition:` | 2 / 4 / 4 / 0 | **2 / 4 / 4 / 0** |
| `forfeit` | 0 | **0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `reconnecting` appears nowhere on the card, and neither does the caption
      `presence outranks the turn — the ADR-0013 grace window, stated plainly in the same slot`
- [ ] `class="seat away"` is exactly 2 and `class="seat on-turn away"` exactly 1, with
      `class="seat on-turn"` unchanged at 6 and `class="seat` at 26
- [ ] `\.seat\.away` still matches exactly 2 CSS rules — no rule dims an away seat's clock or bank
- [ ] `>Away<` is exactly 2 and `>Timed out<` exactly 1
- [ ] `class="clock` is exactly 6 and `class="clock"` exactly 3 — the away seat on turn has the
      same countdown a present seat has
- [ ] `class="timebank"` is exactly 10, `Timebank&nbsp;3:00` exactly 5 and `Timebank&nbsp;0:00`
      exactly 3
- [ ] `class="last-act"` is exactly 8 and `>Fold<` exactly 2
- [ ] `class="dealer"` and `class="pile` are unchanged at 7 and 6
- [ ] `@keyframes` is still 2, `animation:` 4, `animation-` 4 and `transition:` 0
- [ ] The comment sentence *dropping the connection is never a way to gain time …* appears exactly
      once, verbatim, and each of the three captions appears exactly once, verbatim
- [ ] `forfeit` appears nowhere on the card
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
