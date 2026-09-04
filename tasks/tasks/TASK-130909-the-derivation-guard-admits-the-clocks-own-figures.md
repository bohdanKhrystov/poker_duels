---
schema: 2
id: TASK-130909
title: The derivation guard admits the clock's own figures and no other new number
type: task
status: done
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, table, clock]
depends_on: [TASK-130908]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 8) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx -t "admits the clock's own figures and refuses every other new number" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - awk '{ n += gsub(/turnEndsAt/, "&") } END { exit (n < 1) }' web-client/src/table/no-derivation.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The merged guard that says *the table renders and never derives* now covers a table with a running
clock on it: it names the exact set of figures the `TurnClock` frame licenses and refuses anything
else, at two readings.

## Why the guard has to be widened, and why widening it is not weakening it

`no-derivation.test.tsx` compares every number on screen against `numbersIn(view)` — the
`PlayerView`'s own fields — plus the one sum `ADR-0107` §5 admits. A countdown is not in that set
and never can be: it comes from a different frame. Every test in the file today renders
`<DuelTable view={…} />` with no clock, so the guard is green and **narrower than the shipped
table**. Left there, the file would claim a property of a screen it no longer draws.

The honest widening is not a carve-out but a closed set: with a stated clock and a stated reading,
the numbers the table adds are exactly the digit runs of the three figures `turn-clock.ts` produces
— the countdown and one bank per seat — and the test writes them as **literals**, sorted, compared
with `toEqual`. Anything else on screen fails. A second reading changes exactly one member, which is
what tells a live countdown from a constant.

`ADR-0113` §6 is the licence and its limit: the countdown is *"two numbers the server sent minus
locally-measured elapsed time, which is what every countdown on a network already is"*. What stays
forbidden is unchanged — a stack, a pot, a bound or a made hand the client worked out.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/no-derivation.test.tsx` | modify |
| `web-client/src/table/turn-clock.ts` | read |
| `web-client/src/store/duel-state.ts` | read — `TurnClockState` |

## Scope

- **One added test and one added docstring paragraph.** No source file is opened; no existing
  helper — `numbersIn`, `potTotal`, `allowedNumbers`, `numbersOnScreen`, `wordsOnScreen`,
  `spokenOnScreen` — changes by a character, and neither `allowedNumbers` nor `numbersIn` gains a
  clock term. **The widening lives in the new test alone**, which is what keeps the seven merged
  tests exactly as strict as they are today.
- **The fixture is the file's own `VIEW`** from `shows no number the view does not carry`, lifted to
  module scope or rebuilt identically — the one whose numbers are provably independent (`5675`,
  `1450`, `2025`, `100`, `175`, `10200`, `14750`, `125`, `775`, `825`, `1725`, hand `14`, button
  `1`, viewer and seat-to-act `0`).
- **The clock, stated:** seat `0`, `handNumber` `14`, `turnEndsAt` `24_000`,
  `expiresAt` `204_000`, `bankRemainingMillis` `[180_000, 72_000]`.
- **The two readings and the two expected sets, as literals:**

  | `nowMillis` | On screen | Extra numbers, sorted |
  | --- | --- | --- |
  | `0` | `24`, `Timebank 3:00`, `Timebank 1:12` | `[3, 12, 24]` |
  | `5_000` | `19`, `Timebank 3:00`, `Timebank 1:12` | `[3, 12, 19]` |

  `0` and `1` are absent from both because the view already carries them (`seatToAct` and
  `buttonSeat`), and `00` parses to `0`. Do **not** compute the expected arrays from
  `clockFigure`/`bankFigure`: a golden set derived from the code under test asserts nothing.
- **`toEqual` on the sorted, de-duplicated array**, never `toContain` and never a subset check. The
  assertion has to be closed, or a fourth invented figure passes.
- **A docstring paragraph** in the file's existing tradition, naming what the clock adds, why it is
  admitted, and that the seven merged tests still render no clock at all.

## Out of scope

- **`allowedNumbers` and `numbersIn`.** Widening either would relax all seven merged tests at once;
  `ADR-0107` §5's pot total is the only member either has ever gained and this ticket adds no
  second.
- **The spoken sweep.** Neither span carries an `aria-label` or a `title` (`TASK-130906` gates it),
  so `spokenOnScreen` has nothing new to see and `names no hand and declares no winner` is unmoved.
- **`null-view.test.tsx`.** `TASK-130910` writes the host-alone line it is owed.
- **Any source file.**

## Tests

`no-derivation.test.tsx` — **1** added to the 7 it has, so the file reports **8**.

| Test | Proves |
| --- | --- |
| `admits the clock's own figures and refuses every other new number` | with the clock and reading above, the numbers on screen that the view does not carry are **exactly** `[3, 12, 24]`; at a reading 5 000 ms later they are **exactly** `[3, 12, 19]`. Two readings, because one cannot tell a countdown from a constant — and a closed `toEqual`, because a subset check would admit a fourth figure the table invented |

The seven merged tests do not move and none of them passes a clock.

## Acceptance criteria

- [ ] `no-derivation.test.tsx` reports at least **8** passing tests and none failing
- [ ] `admits the clock's own figures and refuses every other new number` passes when run alone by
      name
- [ ] The test asserts `[3, 12, 24]` at one reading and `[3, 12, 19]` at another, both with
      `toEqual` on a sorted, de-duplicated array
- [ ] `numbersIn` and `allowedNumbers` are unchanged
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
