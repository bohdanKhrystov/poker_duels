---
schema: 2
id: TASK-130903
title: One seat draws the clock, and two server-stated numbers choose its treatment
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, clock]
depends_on: [TASK-130902]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/turn-clock.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 14) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/turn-clock.test.ts -t "draws the countdown only at the seat the clock names" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - sh -c '! grep -qF "Date.now" web-client/src/table/turn-clock.ts'
  - sh -c '! grep -qF "performance.now" web-client/src/table/turn-clock.ts'
  - sh -c 'grep -qF "RUNNING_OUT_SECONDS = 10" web-client/src/table/turn-clock.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

One pure function answers, for a seat, the three questions the table is about to ask: does this
seat draw a countdown, what does it read, and which of the card's four treatments is it in — with
the bank figure beside it, live at the seat that is spending it and the frame's own number
everywhere else.

## The whole model, and where each line of it comes from

`ADR-0113` §6 licenses exactly this and no more: *"Which treatment is drawn is chosen from the two
numbers the server stated — `now < turnEndsAt` is the fresh allowance, `turnEndsAt <= now <
expiresAt` is on timebank — so the distinction `ADR-0108` §5 requires costs no third field and no
client-computed game fact. It is a comparison of two server-stated numbers to pick a style, which
is the same class of act as `secondsRemaining` itself."*

Both instants are already in the store, anchored at the frame's **arrival** (`TASK-130811`). This
function re-derives nothing: it is handed the anchored pair and a reading, and compares.

| Reading | Figure | Treatment | That seat's bank reads |
| --- | --- | --- | --- |
| `now < turnEndsAt`, more than `RUNNING_OUT_SECONDS` left | `clockFigure(secondsRemaining(turnEndsAt, now))` | `regular` | `bankFigure` of `expiresAt − turnEndsAt` |
| `now < turnEndsAt`, `RUNNING_OUT_SECONDS` or fewer left | the same figure | `running-out` | the same |
| `turnEndsAt <= now < expiresAt` | `clockFigure(secondsRemaining(expiresAt, now))` | `on-timebank` | `bankFigure` of `expiresAt − now` — the same number as the clock, exactly as the card draws it |
| `now >= expiresAt` | `"0"` | `expired` | `"0:00"` |

Every row above was read off `design/components/seat-and-pot.html`. The *on timebank* row is the
one worth naming: the card draws `.clock` `2:47` and `.timebank` `Timebank 2:47` on the same
plate — identical, because once the fresh allowance is gone the bank **is** what is falling. The
single expression `expiresAt − max(now, turnEndsAt)`, clamped at zero, produces every one of the
four bank cells above. It is `ADR-0113` §3's own server-side expression — *"what is left of the
bank at `now`"* — applied to the client's anchored copy of the same two instants.

**A seat the clock does not name reads its bank straight from the frame**,
`bankRemainingMillis[seat]`, because a bank only spends while its seat is on turn (`ADR-0108` §4:
*"Away, not on turn — spends nothing"*). That is the card's *not on turn* row, `Timebank 1:12`
beside a plate with no clock at all.

**`RUNNING_OUT_SECONDS = 10`, and where the number comes from.** No merged source fixes the switch
point; the merged card fixes its bounds by drawing `24` regular and `6` running out, so anything in
7…24 agrees with the drawing the human accepted. Ten is the round number inside those bounds, named
once in this file — `ADR-0102` §4's precedent for a feel number, which is *"the cheapest thing here
to be wrong about"*: one constant, one file, no interface, and the pane's verdict overrules it in
one line (`ADR-0024` §3). Do not spread it; a gate pins the declaration.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/turn-clock.ts` | modify |
| `web-client/src/table/turn-clock.test.ts` | modify |
| `web-client/src/table/countdown.ts` | read |
| `web-client/src/store/duel-state.ts` | read — `TurnClockState` is the anchored pair, exported there |
| `design/components/seat-and-pot.html` | read |

## Scope

- **`turn-clock.ts` gains, beside the two figures it already has:**

  ```ts
  export type ClockTreatment = "regular" | "running-out" | "on-timebank" | "expired";

  /** The store's clock and the reading to draw it against — both, or neither. */
  export interface ClockReading {
    readonly clock: TurnClockState;
    readonly nowMillis: number;
  }

  export interface SeatClock {
    readonly figure: string | null;
    readonly treatment: ClockTreatment;
    readonly bank: string | null;
  }

  export const RUNNING_OUT_SECONDS = 10;

  export function seatClock(
    reading: ClockReading | null,
    seat: number,
    seatToAct: number | null,
    handNumber: number,
  ): SeatClock;
  ```

  **One argument carries both the clock and the reading** so that no caller can supply a deadline
  without the instant to read it against — the failure that would draw a thirty-second countdown
  from a reading of zero.
- **`reading === null` gives `{ figure: null, treatment: "regular", bank: null }`.** Before the
  server has sent a clock there is neither a countdown nor a bank to draw, and `treatment` is
  unread when `figure` is `null`.
- **A clock the view has moved past draws nothing at all** — neither figure nor bank — when
  `clock.handNumber !== handNumber` **or** `clock.seat !== seatToAct`. `ADR-0113` §1 puts
  `handNumber` and `actionSequence` on the frame precisely so *"a client can discard a clock for a
  decision it has already seen closed"*; `PlayerView` carries no `actionSequence`, so the pair the
  view can compare is the hand number and the seat to act, and that is the pair this uses.
- **The figure is drawn only at `clock.seat`.** Every other seat gets `figure: null` and its bank.
- **`secondsRemaining` from `countdown.ts` is what turns an instant into seconds**, in both live
  branches. Nothing here re-implements a subtraction that is already merged.
- **Nothing is decremented and no state is held** (`ADR-0113` §6). The function is called afresh at
  every render with a fresh reading; a throttled tab costs an update, never accuracy.
- **No clock is read here either.** `Date.now` and `performance.now` stay gated absent.

## Out of scope

- **Markup, class names, colours and the word `Timebank`** — `TASK-130906`. This module returns
  strings and a treatment name, never a `className`.
- **What produces the reading.** The store's second hand is `TASK-130904` and `TASK-130905`.
- **`clockFigure` and `bankFigure`.** Merged in `TASK-130902`; called, not edited.
- **Any refusal to draw based on presence.** An away seat's clock is *"exactly the same 30 s plus
  remaining bank"* (`ADR-0108` §4, drawn by the card's *away, on turn* row), so presence is not an
  input to this function.

## Tests

`turn-clock.test.ts` — **7** added to the 7 `TASK-130902` left, so the file reports **14**. A local
`aClock(overrides)` helper builds a `TurnClockState`; no test asserts against the helper's defaults
alone.

| Test | Proves |
| --- | --- |
| `draws no clock and no bank before the server has sent one` | `seatClock(null, 0, 0, 1)` gives `figure` `null` **and** `bank` `null` |
| `draws the countdown only at the seat the clock names` | with the clock at seat 0: `figure` is non-null at seat 0 and `null` at seat 1; **and the mirror**, with the clock at seat 1 and `seatToAct` 1: non-null at seat 1, `null` at seat 0. Two seats, because a hard-coded seat passes one of them |
| `draws nothing for a decision the view has moved past` | same clock, once with `handNumber` one greater and once with `seatToAct` the other seat: `figure` and `bank` both `null` in both |
| `is regular until the last ten seconds, and running out after` | 11 000 ms left is `regular`, 10 000 ms left is `running-out`, and the figure is `"11"` then `"10"` — the boundary asserted from both sides |
| `is on timebank once the allowance is spent, and expired once the bank is` | at `turnEndsAt + 13 000` with a 180 000 ms bank: `on-timebank`, figure `"2:47"`; at `expiresAt + 5 000`: `expired`, figure `"0"` |
| `reads the acting seat's bank down as the bank spends` | the same clock at three readings: inside the allowance gives `"3:00"`, on timebank gives `"2:47"` — the same string as the clock's own figure — and past expiry gives `"0:00"` |
| `reads a seat that is not on turn from the frame's own number` | `bankRemainingMillis` of `[180_000, 72_000]` with the clock at seat 0: seat 1 reads `"1:12"` and seat 0 reads `"3:00"`. Two seats, two values, so a bank read from the wrong index fails |

## Acceptance criteria

- [ ] `turn-clock.test.ts` reports at least **14** passing tests and none failing
- [ ] `draws the countdown only at the seat the clock names` passes when run alone by name
- [ ] Each of the other six tests above passes, by name
- [ ] `turn-clock.ts` declares `RUNNING_OUT_SECONDS = 10`
- [ ] `turn-clock.ts` contains neither `Date.now` nor `performance.now`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
