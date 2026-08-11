---
schema: 2
id: TASK-010425
title: StateProjection — the one entry point that folds events into a state
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, contract]
depends_on: [TASK-010424]
verify:
  - ./gradlew :poker-engine:test --tests '*StateProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

`fold(state, events)` exists: one exhaustive `when` over every event the engine can emit, and the
single place replay, recovery and the contract suite all go through.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StateProjection.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StateProjectionTest.kt` | create |

Read `GameEvent.kt`, `BettingProjection.kt`, `DealerProjection.kt` and `GameState.kt`. Do not
modify them.

## Scope

- `StateProjection.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public object StateProjection {
      /** Replays [events] over [state], in order. */
      public fun fold(state: GameState, events: List<GameEvent>): GameState =
          events.fold(state, ::apply)

      /** The state after [event]. */
      public fun apply(state: GameState, event: GameEvent): GameState =
          dispatch(state, event).copy(eventCount = event.sequence + 1)

      /** Exhaustive over [GameEvent] — no `else`, ever. */
      private fun dispatch(state: GameState, event: GameEvent): GameState = when (event) {
          is BettingEvent -> applyBetting(state, event)
          is DealerEvent -> applyDealer(state, event)
          is HandStarted -> ...
          is BlindPosted -> ...
          is HoleCardsDealt -> state.withSeat(event.seat) { it.copy(holeCards = event.cards) }
          is ActionOn -> state.copy(seatToAct = event.seat)
      }
  }
  ```

  Every applied event leaves `eventCount == event.sequence + 1`, which is what makes "the next
  event's `sequence` is `state.eventCount`" true by construction rather than by convention.

- `HandStarted` resets to a fresh preflop position: `street = PREFLOP`, `board = Board.EMPTY`,
  `pot = 0`, `betToMatch = 0`, `minRaiseTo = bigBlind`, `seatToAct = null`, two untouched `Seat`s
  built from `event.stacks`, and `handNumber`, `buttonSeat`, `smallBlind`, `bigBlind` from the
  event.
- `BlindPosted` commits the seat up to `event.to`, then sets `betToMatch = max(betToMatch, to)`
  and `minRaiseTo = betToMatch + bigBlind`. After a 50/100 pair that is `betToMatch = 100`,
  `minRaiseTo = 200`. An incomplete blind from a short stack is STORY-0105's problem, not this
  file's.
- **KDoc on the object, stating the one deliberate gap in the fold:** it never changes `deck` or
  `rng`, because no event carries an undealt card or a seed — that is a project non-negotiable.
  A folded state therefore matches the engine's state in every field except those two, and the
  contract suite (`TASK-010426`) compares accordingly. Recovering a hand from a log alone needs
  the seed stored beside it: `TASK-010801`.

## Out of scope

- Redaction per player — `PlayerView`, EPIC-02.
- Any rule about which event may follow which — STORY-0105.
- Changing `applyBetting` or `applyDealer`.

## Tests

`StateProjectionTest`, JUnit 5, using `handState()`, `seats()` and `cards(...)`.

| Test | Proves |
| --- | --- |
| `foldingNoEventsChangesNothing` | `fold(state, emptyList()) == state` |
| `handStartedResetsToAFreshPreflopPosition` | from a mid-hand state, `HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))` → hand 7, button 1, `PREFLOP`, empty board, `pot == 0`, `minRaiseTo == 100`, stacks 9 000 and 11 000, no commitments |
| `handStartedKeepsTheDeckAndTheGenerator` | `deck` and `rng` are the same instances before and after |
| `blindsLeaveTheBarAtTheBigBlind` | `BlindPosted(1, 0, 50, false)` then `BlindPosted(2, 1, 100, true)` → `betToMatch == 100`, `minRaiseTo == 200`, commitments 50 and 100, `pot == 0`, `chipsInPlay` unchanged |
| `holeCardsGoToTheirSeat` | `HoleCardsDealt(3, 1, cards("As Kd"))` → `seat(1).holeCards` is those two and `seat(0).holeCards` is empty |
| `actionOnNamesTheSeatToAct` | `ActionOn(5, 1)` → `seatToAct == 1` |
| `everyAppliedEventAdvancesTheEventCount` | `apply(state, ActionOn(5, 1)).eventCount == 6`, and folding six events with sequences 0..5 ends at `eventCount == 6` |
| `bettingAndDealerEventsAreDispatched` | for `PlayerBet(1, 0, 300)`, `apply(state, event) == applyBetting(state, event).copy(eventCount = 2)`, and the same shape for a `StreetDealt` event through `applyDealer` |
| `foldReplaysAnOpeningInOrder` | folding `HandStarted`, `BlindPosted` ×2, `HoleCardsDealt` ×2, `ActionOn(6, 0)` over `handState()` gives a state with both hands dealt, `betToMatch == 100`, `seatToAct == 0` |

## Acceptance criteria

- [ ] `StateProjectionTest.foldingNoEventsChangesNothing` passes
- [ ] `StateProjectionTest.handStartedResetsToAFreshPreflopPosition` passes
- [ ] `StateProjectionTest.handStartedKeepsTheDeckAndTheGenerator` passes
- [ ] `StateProjectionTest.blindsLeaveTheBarAtTheBigBlind` passes
- [ ] `StateProjectionTest.holeCardsGoToTheirSeat` passes
- [ ] `StateProjectionTest.actionOnNamesTheSeatToAct` passes
- [ ] `StateProjectionTest.everyAppliedEventAdvancesTheEventCount` passes
- [ ] `StateProjectionTest.bettingAndDealerEventsAreDispatched` passes
- [ ] `StateProjectionTest.foldReplaysAnOpeningInOrder` passes
- [ ] `StateProjection.apply` contains no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
