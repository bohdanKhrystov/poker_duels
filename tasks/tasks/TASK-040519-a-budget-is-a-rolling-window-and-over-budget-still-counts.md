---
schema: 2
id: TASK-040519
title: A budget is a rolling window, an over-budget attempt still counts, and a slot can be refunded
type: task
status: done
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
and says whether it was within budget, over a rolling window measured by `ServerClock` — and a
recorded attempt can be given back, which is how `ADR-0074` §2 meters failures while still checking
before the hash.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/AttemptBudget.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/AttemptBudgetTest.kt` | create |

Read `docs/adr/ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md` §2 — it
specifies the type verbatim — `docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md`
§2, which adds the one method, and
`poker-server/src/main/kotlin/duels/poker/server/time/ServerClock.kt`. Nothing else.

## Scope

- Exactly `ADR-0055` §2's shape plus `ADR-0074` §2's one method, both declarations in one file
  (they are one concept and ktlint's filename rule is satisfied by the file name matching the
  class):

  ```kotlin
  public data class AttemptLimits(val maxAttempts: Int, val windowMillis: Long)

  public class AttemptBudget(private val limits: AttemptLimits, private val clock: ServerClock) {
      public suspend fun admit(key: String): Boolean
      public suspend fun refund(key: String)
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
- **`refund` removes the most recently recorded attempt for the key**, under the same `Mutex`, and
  does nothing when the key holds none. `ADR-0074` §2 is why it exists: sign-in must check *before*
  it hashes, or the budget protects nothing, and it must meter *failures* (`ADR-0027` §6), so it
  reserves on arrival and gives the slot back when the password turns out to be right. Sign-up never
  calls it. Say in the KDoc that it is a refund of one reservation and not a reset of the key.

## Out of scope

- Any caller. Sign-up's is `TASK-040521`; sign-in's, which is the only one that refunds, is
  `TASK-040523`.
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
| `aRefundedSlotIsSpendableAgain` | at the limit, one `refund`, then one `admit` answers `true` — and the one after it answers `false`, so the refund returned **one** slot and not the window |
| `refundingAnUnknownKeyDoesNothing` | `refund("never-seen")` on a fresh budget leaves `"never-seen"` with its full limit and every other key untouched |

## Acceptance criteria

- [ ] All nine test methods above pass
- [ ] No test in `AttemptBudgetTest` calls `Thread.sleep`, `delay`, or `System.nanoTime`
- [ ] `AttemptBudget` holds no `java.time` type
- [ ] Every command in `verify:` exits 0

## Proof

Return early without recording when the budget is exhausted and `anOverBudgetAttemptStillCounts`
goes red alone. Make `refund` clear the key instead of removing one entry and
`aRefundedSlotIsSpendableAgain` goes red on its second assertion, which is the half of that test
that is not a tautology. Drop the `Mutex` and `concurrentCallersDoNotOverspend` fails — it is the only one
that can, and if it passes without the mutex the test is not actually concurrent, which is worth
checking before believing it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The concurrency test uses 500 callers, not the 50 this ticket named.** At 50, the mutation it
exists to catch — dropping the `Mutex` from `admit` and `refund` — only failed **4 of 5** runs. At
500 it failed 4 of 4 while the correct implementation passed 3 of 3. The measurement is in a comment
beside the constant. A flaky gate is worse than no gate, because the first person it fails spuriously
deletes it. `AttemptLimits(5, 60_000)` is unchanged.

**Why `anOverBudgetAttemptStillCounts` uses the instants it does.** With `AttemptLimits(1, 1000)`:
an admit at t=0, a **refused** admit at t=500 that must still be recorded, then an admit at t=1001.
That instant is past the first attempt's tail (1000) and before the refused one's (1500). An
implementation that stops recording once over budget has nothing at t=500, sees an empty window, and
wrongly admits — which is a rate limiter that lets an attacker back in sooner than its own budget
promises. Move t=1001 past 1500 or before 1000 and the test distinguishes nothing.

