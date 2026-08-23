---
schema: 2
id: TASK-040519
title: A budget is a rolling window, and an over-budget attempt still counts
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, rate-limit]
depends_on: [TASK-040518]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.AttemptBudgetTest'
  - ./gradlew :poker-server:detekt
---

## Goal

The first rate limiter in this repository exists: `AttemptBudget` records an attempt against a key
and says whether it was within budget, over a rolling window measured by `ServerClock`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/AttemptBudget.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/AttemptBudgetTest.kt` | create |

Read `docs/adr/ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md` §2 — it
specifies the type verbatim — and
`poker-server/src/main/kotlin/duels/poker/server/time/ServerClock.kt`. Nothing else.

## Scope

- Exactly `ADR-0055` §2's shape, both declarations in one file (they are one concept and ktlint's
  filename rule is satisfied by the file name matching the class):

  ```kotlin
  public data class AttemptLimits(val maxAttempts: Int, val windowMillis: Long)

  public class AttemptBudget(private val limits: AttemptLimits, private val clock: ServerClock) {
      public suspend fun admit(key: String): Boolean
  }
  ```

- **`ServerClock`, not `java.time.Clock`.** This is a duration held in memory, which is exactly what
  the monotonic clock is for; `ADR-0062` moved *dates* to `java.time.Clock` and left this alone, and
  an NTP step must not widen a window or void one.
- State is an in-memory map of key → timestamps. A restart forgets it, and that is accepted for the
  same reason rooms are not durable.
- **A `Mutex` is held across the whole read-prune-record.** A check-then-record with any gap admits
  *N* concurrent callers against a budget of one, and concurrency *is* the attack, so the critical
  section is the feature and not an optimisation.
- **An over-budget attempt is still recorded**, so hammering extends the window rather than
  resetting it (`ADR-0055` §1). Say it in the KDoc; it is the clause a later reader will "simplify"
  away.
- `admit` prunes the key it touches. There is no sweep here and no second coroutine.

## Out of scope

- Any caller. Sign-up's is `TASK-040521`; sign-in's is `TASK-040523`, blocked on `DEC-069`.
- The config values — `TASK-040520`.
- `ADR-0022`'s failed-join budget, which stays inside `RoomRegistry` where that ADR put it.
- Sweeping expired keys from `ADR-0025`'s ticker. `ADR-0055` §2 mentions it; nothing in this story
  needs it and a budget that prunes on touch is already bounded by its callers.

## Tests

`AttemptBudgetTest`, on a hand-rolled `ServerClock` the test advances. **No test sleeps.**

| Test | Proves |
| --- | --- |
| `theFirstAttemptsAreAdmitted` | with `AttemptLimits(3, 1000)`, three calls all answer `true` |
| `theFourthIsRefused` | the fourth answers `false` |
| `theWindowRollsForward` | after advancing past `windowMillis`, a call answers `true` again |
| `anOverBudgetAttemptStillCounts` | at the limit, one refused call, then advance to just inside the original window's tail — the refused call is still on the clock, so the next call is still refused. **This is the clause that separates a rolling budget from a resetting one, and nothing else tests it** |
| `twoKeysAreTwoBudgets` | exhausting `"a"` leaves `"b"` admitted — two keys, because one key alone agrees with a global counter |
| `theClockIsReadAndNotAssumed` | a budget on a clock that never advances refuses forever after the limit; the same limits on a clock that jumps a full window between every call never refuse |
| `concurrentCallersDoNotOverspend` | 50 coroutines calling `admit("a")` against `AttemptLimits(5, 60_000)` produce exactly 5 `true`s — the assertion the `Mutex` exists for |

## Acceptance criteria

- [ ] All seven test methods above pass
- [ ] No test in `AttemptBudgetTest` calls `Thread.sleep`, `delay`, or `System.nanoTime`
- [ ] `AttemptBudget` holds no `java.time` type
- [ ] Every command in `verify:` exits 0

## Proof

Return early without recording when the budget is exhausted and `anOverBudgetAttemptStillCounts`
goes red alone. Drop the `Mutex` and `concurrentCallersDoNotOverspend` fails — it is the only one
that can, and if it passes without the mutex the test is not actually concurrent, which is worth
checking before believing it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
