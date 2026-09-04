---
schema: 2
id: TASK-130907
title: The table draws one countdown, at the acting seat, and both seats' banks
type: task
status: ready
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, clock]
depends_on: [TASK-130906]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 35) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx -t "draws the countdown at whichever seat the clock names" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 7) }'
  - awk '{ n += gsub(/seatClock\(/, "&") } END { exit (n != 2) }' web-client/src/table/DuelTable.tsx
  - sh -c '! grep -qF "Date.now" web-client/src/table/DuelTable.tsx'
  - sh -c '! grep -qF "performance.now" web-client/src/table/DuelTable.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Given the clock the store is holding and the reading to draw it against, the duel table puts one
countdown on the plate of the seat the server named, a timebank figure on both plates, and nothing
at all before a `TurnClock` has arrived.

## Why one prop and not two

`ADR-0108` §5 wants *"one countdown: the acting seat's clock, **visible to both players**"*, and
`ADR-0113` §1 puts the seat on the frame precisely because it is sent to both seats identically.
So the table asks `seatClock` once per plate and lets the seat number decide — never `view.viewerSeat`,
which is the one field that differs between the two players and would give each of them a clock of
their own.

The clock and the reading travel as **one** optional prop, `ClockReading`, so no caller can hand
this component a deadline without the instant to read it against. `DuelTable` reads no clock: a
component that called `performance.now()` here would re-derive the anchor `ADR-0113` §6 took at
arrival and undo the whole point of taking it there — a clock released from a paced runout would
restart at thirty. Both `Date.now` and `performance.now` are gated absent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/table/turn-clock.ts` | read |
| `web-client/src/table/SeatPlate.tsx` | read |

## Scope

- **One new optional prop:** `clock?: ClockReading | null`, KDoc'd as *the clock the store anchored
  and the reading to draw it against — never re-derived here*. Absent is the merged behaviour every
  existing caller and every existing test gets.
- **Two calls, one per plate**, gated at exactly two:

  ```tsx
  clock={seatClock(props.clock ?? null, rival.index, view.seatToAct, view.handNumber)}
  ```

  and the same for `you`. Nothing is memoised, cached or hoisted: the function is pure and the
  render is what re-runs when the store's reading moves.
- **The seat argument is the plate's own `index`**, from the view. Not `viewerSeat`, not a literal,
  not a ternary.
- **`view.seatToAct` and `view.handNumber` go through unchanged.** `PlayerView` carries no
  `actionSequence`, so those two are the pair the view can compare a clock against, and
  `TASK-130903` already decided what to do when they disagree.
- **Nothing else in `DuelTable` moves** — not the bet lines, not the centre block, not the reveal
  step, not the last-act routing.

## Out of scope

- **The four treatments, each asserted by name, and zero.** `TASK-130908`, in this same test file,
  because those are about what the clock reads over time and these are about which seat holds it.
- **`no-derivation.test.tsx`'s guard.** `TASK-130909` widens it to admit the clock's own figures.
  It is pinned unmoved here, and can be, because every render in it omits the new prop.
- **`Lobby.tsx`.** `TASK-130910` hands the table the store's clock; until then nothing in the
  running client passes this prop and the screen is unchanged.
- **Any figure or treatment logic.** All of it is merged in `turn-clock.ts`.

## Tests

`DuelTable.test.tsx` — **5** added to the 30 it has, so the file reports **35**. A local
`aReading(overrides)` helper builds a `ClockReading`; **no test asserts against its defaults
alone**, and every seat-sensitive test states both seats.

| Test | Proves |
| --- | --- |
| `draws the countdown at whichever seat the clock names` | with `seatToAct` 0 and the clock at seat 0: `24` appears inside the plate whose seat that is and **not** inside the other. Then the mirror — `seatToAct` 1, clock at seat 1 — with the figure `19`, so the two runs cannot share a constant. Run at `viewerSeat` 0 **and** `viewerSeat` 1, so a table that gave the countdown to the viewer rather than to the actor fails |
| `draws exactly one countdown` | across the same four combinations, the number of elements matching the clock's own class is exactly **1** each time — `ADR-0108` §Consequences forecloses a second |
| `draws both seats' banks` | `bankRemainingMillis` of `[180_000, 72_000]` puts `Timebank 3:00` on one plate and `Timebank 1:12` on the other, each on the right one. Two seats, two values, so a bank read from the wrong index fails |
| `draws no countdown and no bank before a TurnClock has arrived` | with the prop absent: no clock element anywhere, and no text matching `/Timebank/` |
| `draws nothing for a clock the view has moved past` | the same clock against a view whose `handNumber` is one greater, and again against a view whose `seatToAct` is the other seat: no countdown either time |

The 30 merged tests do not move. None of them passes the new prop, so `SeatPlate` receives
`undefined` and draws neither span — the same reason `no-derivation.test.tsx` is pinned at 7 here.

## Acceptance criteria

- [ ] `DuelTable.test.tsx` reports at least **35** passing tests and none failing
- [ ] `draws the countdown at whichever seat the clock names` passes when run alone by name
- [ ] Each of the other four tests above passes, by name
- [ ] `no-derivation.test.tsx` still reports at least **7** passing and none failing
- [ ] `DuelTable.tsx` calls `seatClock(` exactly twice and contains neither `Date.now` nor
      `performance.now`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
