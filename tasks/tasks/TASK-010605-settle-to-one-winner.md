---
schema: 2
id: TASK-010605
title: Settle a swept hand to a single winner
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010604]
verify:
  - ./gradlew :poker-engine:test --tests '*SettleHandTest'
  - ./gradlew :poker-engine:check
---

## Goal

One function turns a swept hand into the three facts that close it: the uncalled bet comes back,
the pot goes to its winner, and the hand is over.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Settlement.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SettleHandTest.kt` | create |

Read `DealerEvents.kt`, `DealerProjection.kt`, `StateProjection.kt`, `EngineResult.kt` and
`GameStates.kt`. Modify none of them.

## Scope

- Add to `Settlement.kt`:

  ```kotlin
  public fun settleHand(state: GameState, winners: List<Int>): EngineResult
  ```

- `state` must already be swept — `require(state.seats.all { it.committedThisStreet == 0 })`,
  because `pot` is only the whole of what is at stake once `BettingRoundEnded` has run. This
  ticket handles one winner: `require(winners.size == 1)`. `TASK-010606` lifts that to two.
- The events, in this order, each built from the running state's `eventCount` and applied with
  `StateProjection.apply` before the next is built, so sequences stay dense:

  1. `UncalledBetReturned(seat, amount)` — only when `uncalledPortion(state)` is non-null.
     `require(amount <= state.pot)`: taking more than the pot holds would drive `pot` negative.
  2. `PotAwarded(winner, pot)` — the pot **after** the return, and **only when it is positive**.
     `PotAwarded` requires a positive amount, and a hand whose whole pot was one uncalled bet
     leaves nothing to award.
  3. `HandFinished` — always, and always last.

- Return `EngineResult.accepted(finalState, events)`. `HandFinished` is what moves the street to
  `COMPLETE`; this function emits it and lets `DealerProjection` do the rest — no field on
  `GameState` is set here by hand.
- Amounts on both settlement events are **deltas** out of the pot, not street totals like a
  betting event's `to`.

## Out of scope

- Split pots and the odd chip — `TASK-010606`. Do **not** pin the `winners.size == 1` rejection
  in a test: that requirement is lifted next ticket, and a test of it would have to be deleted
  one ticket later.
- Wiring this into `continueHand` — `TASK-010607` (fold) and `TASK-010611`/`TASK-010612`
  (showdown). Nothing calls `settleHand` yet, so no existing test observes a changed engine
  output in this ticket.

## Tests

`SettleHandTest`, JUnit 5. Build swept positions with `handState(...)`, setting `pot` and the
seats' `committedThisHand` directly (`committedThisStreet = 0` everywhere).

| Test | Proves |
| --- | --- |
| `theWinnerTakesThePot` | pot 600, equal commitments, winner seat 1 → `PotAwarded(seat = 1, amount = 600)`, `seat(1).stack` up by 600, `pot == 0` |
| `anUncalledBetComesBackBeforeTheAward` | commitments 300/500 and pot 800 → `UncalledBetReturned(1, 200)` then `PotAwarded(_, 600)`, in that order |
| `aPotOfZeroEmitsNoAward` | pot 450 entirely uncalled by seat 0 → `UncalledBetReturned(0, 450)` then `HandFinished`, and no `PotAwarded` anywhere |
| `theHandFinishesComplete` | last event is `HandFinished`, `street == Street.COMPLETE`, `seatToAct == null`, `isHandOver` |
| `chipsAreConservedBySettlement` | `chipsInPlay` identical before and after, for all three positions above |
| `theSequencesStayDense` | from `eventCount = 9`, the events' sequences are `9, 10, 11` and `newState.eventCount == 12` |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition(before, result)` holds |
| `anUnsweptStateIsRefused` | a state with `committedThisStreet = 100` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `SettleHandTest.theWinnerTakesThePot` passes
- [ ] `SettleHandTest.anUncalledBetComesBackBeforeTheAward` passes
- [ ] `SettleHandTest.aPotOfZeroEmitsNoAward` passes
- [ ] `SettleHandTest.theHandFinishesComplete` passes
- [ ] `SettleHandTest.chipsAreConservedBySettlement` passes
- [ ] `SettleHandTest.theSequencesStayDense` passes
- [ ] `SettleHandTest.theEventsDescribeTheTransition` passes
- [ ] `SettleHandTest.anUnsweptStateIsRefused` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
