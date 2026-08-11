---
schema: 2
id: TASK-010511
title: Turn an accepted action into the event that records it
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010510]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingActionsTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every action a player may take has exactly one event that records it, with the street total the
state says it is worth — never an amount a client chose.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingActions.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingActionsTest.kt` | create |

Read `BettingEvents.kt`, `BettingRules.kt`, `BettingProjection.kt`. Modify none of them.

## Scope

- `BettingActions.kt`, package `duels.poker.engine.game`, one public function:

  ```kotlin
  public fun eventFor(state: GameState, action: PlayerAction): BettingEvent
  ```

- `sequence` is `state.eventCount` — the next event of the hand, always.
- Amounts come from `legalActions(state)`, never from the caller, except a `Bet` or `Raise`
  whose `to` the client chose and `TASK-010510` has already bounded. Start with
  `require(legal.allows(action.type))` so an unvalidated action throws rather than producing a
  bogus event.
- The mapping, exhaustive over `PlayerAction` with no `else`:

  | Action | Event |
  | --- | --- |
  | `Fold` | `PlayerFolded(seq, seat)` |
  | `Check` | `PlayerChecked(seq, seat)` |
  | `Call` | `PlayerCalled(seq, seat, legal.callTo)` |
  | `AllIn` | `PlayerAllIn(seq, seat, legal.allInTo)` |
  | `Bet(to)` | `PlayerAllIn(seq, seat, to)` if `to == legal.allInTo`, else `PlayerBet(seq, seat, to)` |
  | `Raise(to)` | `PlayerAllIn(seq, seat, to)` if `to == legal.allInTo`, else `PlayerRaised(seq, seat, to)` |

- **Why a whole-stack bet or raise is logged as an all-in**: `applyBetting` raises `minRaiseTo`
  unconditionally for `PlayerBet` and `PlayerRaised`, but for `PlayerAllIn` only when the shove
  reaches a full raise (`TASK-010422`). Logging a short shove as a raise would *lower* the bar
  for the next raise. Put that reason in the KDoc.
- A `Call` stays a `PlayerCalled` even when it commits the last chip: `Seat.commit` sets
  `isAllIn` on its own, and the bar must not move for a call.

## Out of scope

- Applying the event — `StateProjection.apply` already does that; call it from `TASK-010512`.
- What happens next in the hand — `TASK-010513` onwards.

## Tests

`BettingActionsTest`, JUnit 5, positions from `handState()` and `seats()` with `copy`.

| Test | Proves |
| --- | --- |
| `aFoldBecomesAPlayerFolded` | type and seat |
| `aCheckBecomesAPlayerChecked` | type and seat |
| `aCallCarriesTheStreetTotal` | committed 100 facing `betToMatch = 300` gives `PlayerCalled(to = 300)`, not 200 |
| `aCallForLessCarriesTheWholeStack` | stack 150 facing 300 gives `PlayerCalled(to = 150)` |
| `anAllInCarriesTheWholeStack` | stack 900 committed 100 gives `PlayerAllIn(to = 1_000)` |
| `anOrdinaryBetBecomesAPlayerBet` | fresh street, `Bet(0, 300)` gives `PlayerBet(to = 300)` |
| `aRaiseForTheWholeStackIsLoggedAsAnAllIn` | stack leaves `allInTo = 500`, `Raise(0, 500)` gives `PlayerAllIn` |
| `aShortShoveDoesNotLowerTheMinimumRaise` | `betToMatch = 300`, `minRaiseTo = 600`, `allInTo = 500`: after `applyBetting` of the event from `Raise(0, 500)`, `minRaiseTo` is still 600 |
| `everyEventContinuesTheHandSequence` | with `eventCount = 7`, the event's `sequence` is 7 |
| `refusesAnActionThatIsNotLegal` | `Check(0)` while facing a bet throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `BettingActionsTest.aFoldBecomesAPlayerFolded` passes
- [ ] `BettingActionsTest.aCheckBecomesAPlayerChecked` passes
- [ ] `BettingActionsTest.aCallCarriesTheStreetTotal` passes
- [ ] `BettingActionsTest.aCallForLessCarriesTheWholeStack` passes
- [ ] `BettingActionsTest.anAllInCarriesTheWholeStack` passes
- [ ] `BettingActionsTest.anOrdinaryBetBecomesAPlayerBet` passes
- [ ] `BettingActionsTest.aRaiseForTheWholeStackIsLoggedAsAnAllIn` passes
- [ ] `BettingActionsTest.aShortShoveDoesNotLowerTheMinimumRaise` passes
- [ ] `BettingActionsTest.everyEventContinuesTheHandSequence` passes
- [ ] `BettingActionsTest.refusesAnActionThatIsNotLegal` passes
- [ ] `eventFor` contains no `else` branch over `PlayerAction`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
