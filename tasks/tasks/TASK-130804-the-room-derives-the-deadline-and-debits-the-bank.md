---
schema: 2
id: TASK-130804
title: The room derives the deadline from its decision point and debits the bank by arithmetic
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, clock]
depends_on: [TASK-130803]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomTurnClockTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomTurnClockTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==11 else 1)"
  - sh -c '! grep -qE "System\.(nano|current)Time|Instant\.now|nowMillis" poker-server/src/main/kotlin/duels/poker/server/room/Room.kt'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`Room` carries `turnDeadline` and `timebankRemainingMillis` and derives both from the live
decision point through one pure method, `clocked(runner, now, turnMillis)` — so the deadline is
two numbers computed from where the duel stands, never a timer that has to be found and cancelled
(`ADR-0113` §3).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTurnClockTest.kt` | create |

## Scope

- Add `public data class TurnDeadline(val seat: Int, val handNumber: Int, val actionSequence: Int,
  val bankBeginsAt: Long, val expiresAt: Long)` to `Room.kt`, beside `RoomState`. Both instants are
  **absolute**, on the caller's monotonic scale — never a remaining duration, for the reason
  `gracePeriods`' own KDoc already gives.
- Add `val turnDeadline: TurnDeadline? = null` and
  `val timebankRemainingMillis: Map<Int, Long> = emptyMap()` to `Room`, with `init` requiring every
  named seat in `0..1`, every bank `>= 0`, and `bankBeginsAt <= expiresAt`.
- Add `public fun clocked(runner: DuelRunner?, now: Long, turnMillis: Long): Room`, pure and total:
  1. **Debit.** When `turnDeadline` names a seat, subtract what that seat spent of its bank for the
     decision that just closed — the bank left at `now` is
     `(expiresAt - maxOf(now, bankBeginsAt)).coerceAtLeast(0)`, and that becomes the seat's new
     `timebankRemainingMillis`. A decision answered inside the fresh allowance spends nothing.
  2. **Restart.** When `runner`'s live decision point differs from the one `turnDeadline` names —
     a different seat, hand number or action sequence — set
     `bankBeginsAt = now + turnMillis` and `expiresAt = bankBeginsAt + timebankRemainingMillis[seatToAct]`.
  3. **Clear.** When `runner` has no live decision point (no hand, or no `seatToAct`), set
     `turnDeadline = null`, after debiting.
  A call whose decision point has **not** moved leaves `turnDeadline` identical.
- Add `public fun withFreshClocks(timebankMillis: Long): Room`: both banks refilled to
  `timebankMillis`, `turnDeadline` cleared. *"A rematch is a new duel and a fresh bank"*
  (`ADR-0108` §1).
- `Room` still reads **no clock**: `now` is a parameter, exactly as `join`, `finish` and
  `presenceOf` already take one.

## Out of scope

- **Calling any of this.** `RoomRegistry` wires it in `TASK-130806` and `TASK-130807`; the sweep
  reads `turnDeadline` in `TASK-130808`.
- The `TurnClock` frame and the wire — `TASK-130805`.
- `gracePeriods`, `isPaused`, `presenceOf`, `disconnect` — untouched here, retired in
  `TASK-130810`. Every existing `Room` test stands unchanged, because both new fields default.

## Tests

`RoomTurnClockTest` — a new file, 11 tests, all against injected `now` values; nothing sleeps.

| Test | Proves |
| --- | --- |
| `aFreshDecisionPointStartsTheAllowanceAndThenTheBank` | `bankBeginsAt == now + turnMillis` and `expiresAt == bankBeginsAt + bank` |
| `aDecisionAnsweredInsideTheAllowanceSpendsNothing` | Closing at `bankBeginsAt - 1` leaves **both** banks exactly as they were |
| `fiveSecondsIntoTheBankDebitsExactlyFiveThousand` | Closing at `bankBeginsAt + 5_000` leaves that seat `180_000 - 5_000` |
| `eightSecondsIntoTheBankDebitsExactlyEightThousand` | The same at `+ 8_000` leaves `180_000 - 8_000` — a second overrun, because one value cannot tell a debit from a constant |
| `seatOneOverrunDebitsSeatOne` | With the clock on seat **1**, the overrun lands on seat 1 |
| `theRivalsBankIsUntouchedByAnOverrun` | In both of the two tests above, the seat **not** on turn keeps its bank to the millisecond |
| `aDecisionClosedPastTheDeadlineSpendsTheBankToZeroAndNoFurther` | Closing at `expiresAt + 60_000` leaves exactly `0`, never a negative |
| `theDeadlineIsUnchangedWhileTheDecisionPointStands` | A second `clocked` at a later `now`, same hand/sequence/seat, returns the identical `turnDeadline` and the identical banks |
| `aRunnerWithNoLiveDecisionPointClearsTheDeadline` | A finished runner leaves `turnDeadline == null`, and the closing decision is still debited |
| `freshClocksRefillBothBanksAndClearTheDeadline` | `withFreshClocks(180_000)` sets both seats to `180_000` and `turnDeadline` to `null`, from a room whose banks were spent unevenly |
| `aRoomRefusesAnImpossibleClock` | `TurnDeadline(seat = 2, …)`, a negative bank and `expiresAt < bankBeginsAt` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] Each of the eleven tests above passes, by name
- [ ] `RoomTurnClockTest` reports exactly **11** tests
- [ ] `Room.kt` names no clock source — the refusal gate finds no `System.nanoTime`,
      `System.currentTimeMillis`, `Instant.now` or `nowMillis` in it
- [ ] `./gradlew :poker-server:check` is green, so every merged `Room` test still passes with the
      two new fields defaulted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
