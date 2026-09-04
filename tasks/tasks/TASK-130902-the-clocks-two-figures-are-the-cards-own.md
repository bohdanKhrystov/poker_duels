---
schema: 2
id: TASK-130902
title: The clock's figure and the bank's figure are the two shapes the card drew
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table, clock]
depends_on: [TASK-130901]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/turn-clock.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 7) }'
  - sh -c '! grep -qF "Date.now" web-client/src/table/turn-clock.ts'
  - sh -c '! grep -qF "performance.now" web-client/src/table/turn-clock.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Two pure functions turn a number of whole seconds into the two figures `STORY-1307`'s merged card
draws — the clock's `24`, `6`, `2:47`, `0` and the bank's `3:00`, `1:12`, `0:00` — and nothing else
in the client spells either shape out.

## Why two shapes and not one

They are the card's, measured on it rather than inferred:

| The card's element | Its figures, verbatim | The shape |
| --- | --- | --- |
| `.clock` | `24`, `6`, `2:47`, `0` | bare whole seconds under a minute; `m:ss` at a minute or more |
| `.timebank` | `Timebank 3:00`, `Timebank 1:12`, `Timebank 2:47`, `Timebank 0:00` | always `m:ss` |

The two differ at exactly one point and it is the point that matters: an empty bank reads `0:00`
and a spent clock reads `0`. `TASK-130701`'s scope names the clock's rule in words — *"bare whole
seconds under a minute (`24`, `6`), `m:ss` at a minute or more (`3:00`, `1:12`)"* — and the merged
card then drew `Timebank 0:00` at the seat whose bank is gone. Where the sentence and the drawing
disagree, **the drawing is what the human accepted** (`ADR-0024` §3), so the bank keeps its minutes
even at zero and the clock does not.

The label `Timebank` and its non-breaking space are **not** here: they are markup, and
`TASK-130906` puts them on the plate in `SeatPlate.tsx`'s own `NBSP` idiom.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/turn-clock.ts` | create |
| `web-client/src/table/turn-clock.test.ts` | create |
| `web-client/src/table/countdown.ts` | read |
| `design/components/seat-and-pot.html` | read |

## Scope

- **`turn-clock.ts`, two exported functions, both taking whole seconds:**

  ```ts
  export function clockFigure(seconds: number): string
  export function bankFigure(seconds: number): string
  ```

  `clockFigure` returns `String(s)` below 60 and `m:ss` at 60 or above. `bankFigure` returns `m:ss`
  always. Both clamp a negative input to zero before formatting — the clamp is belt and braces
  beside `secondsRemaining`'s own, because these two are the last thing between a number and a
  player's eye.
- **Seconds in, string out.** Neither function reads a clock, a deadline or a frame; neither
  imports `countdown.ts`. `TASK-130903` is what pairs `secondsRemaining` with these.
- **`m:ss` pads the seconds to two figures and never pads the minutes**: `1:05`, not `1:5` and not
  `01:05`. The card draws `1:12` and `3:00`; `1:05` is the case those two cannot distinguish, so it
  is a test of its own.
- **No clock of any kind in this file.** `Date.now` and `performance.now` are both gated absent —
  a formatter that reached for the current time would be deriving a fact rather than shaping one.
- **KDoc cites the card**, `design/components/seat-and-pot.html`, and `ADR-0108` §5 for why the
  bank is a public figure at all. Comment *why* the two shapes differ, never *what* `padStart`
  does.

## Out of scope

- **Which seat gets a clock, and which of the four treatments it wears** — `TASK-130903`, in this
  same file.
- **The word `Timebank`, the non-breaking space, and any markup** — `TASK-130906`.
- **`secondsRemaining`.** Merged at `countdown.ts` and read here only to see that it already
  returns whole seconds; it is not imported by this ticket.
- **A threshold, a colour or a class name.** None of them belongs to a formatter.

## Tests

`turn-clock.test.ts` — a new file, **7** tests. Every figure below is copied from the merged card,
so a rewritten shape fails against the drawing rather than against an opinion.

| Test | Proves |
| --- | --- |
| `writes the clock as bare seconds under a minute` | `clockFigure(24)` is `"24"` and `clockFigure(6)` is `"6"` — two inputs, because one cannot tell a formatter from a constant |
| `writes the clock as m:ss from a minute up` | `clockFigure(167)` is `"2:47"` and `clockFigure(72)` is `"1:12"` |
| `switches shape at exactly a minute` | `clockFigure(59)` is `"59"` and `clockFigure(60)` is `"1:00"` |
| `writes a spent clock as a bare zero` | `clockFigure(0)` is `"0"` — the card's *expired* row, and the one place the two functions part |
| `writes the bank as m:ss, at every size` | `bankFigure(180)` is `"3:00"` and `bankFigure(72)` is `"1:12"` |
| `writes an empty bank as 0:00 and not as 0` | `bankFigure(0)` is `"0:00"` — the card's *timed out* row |
| `pads the bank's and the clock's seconds to two figures` | `bankFigure(65)` is `"1:05"`, `bankFigure(61)` is `"1:01"`, `clockFigure(65)` is `"1:05"` — the case `1:12` and `3:00` cannot catch |

## Acceptance criteria

- [ ] `turn-clock.test.ts` reports at least **7** passing tests and none failing
- [ ] Each of the seven tests above passes, by name
- [ ] `turn-clock.ts` contains neither `Date.now` nor `performance.now`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
