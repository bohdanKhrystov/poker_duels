---
schema: 2
id: TASK-010411
title: handState test fixture for game states
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, test]
depends_on: [TASK-010410]
verify:
  - ./gradlew :poker-engine:test --tests '*GameStatesTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every later test in this story builds a `GameState` in one line, so no test spends thirteen
arguments describing a position it does not care about.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameStates.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameStatesTest.kt` | create |

Read `GameState.kt` and `Seat.kt`, and
`poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt` as the precedent for a test-only
helper. Do not touch main sources.

## Scope

- `GameStates.kt`, package `duels.poker.engine.game`, test sources only:

  ```kotlin
  internal const val SMALL_BLIND: Int = 50
  internal const val BIG_BLIND: Int = 100
  internal const val START_STACK: Int = 10_000
  internal const val TEST_SEED: Long = 1L

  /** Two untouched seats with the given stacks. */
  internal fun seats(stack0: Int = START_STACK, stack1: Int = START_STACK): List<Seat>

  /**
   * A fresh preflop state: hand 1, button on seat 0, empty board, empty pot, no bet to match,
   * `minRaiseTo = BIG_BLIND`, `seatToAct = 0`, `eventCount = 0`, a full deck and
   * `SplitMix64Rng(TEST_SEED)`.
   * Vary anything else with `copy` — it is a data class.
   */
  internal fun handState(seats: List<Seat> = seats()): GameState
  ```

- Keep the parameter list this short deliberately: detekt's `LongParameterList` fires on wide
  helper functions, and `copy` covers every other field.
- A one-line comment saying the helper is test-only, matching `Cards.kt`.

## Out of scope

- Any assertion helper or custom matcher — not yet ticketed.
- Building *legal* positions. This fixture makes a state that satisfies `GameState`'s `init`
  requires; whether a position could arise in play is STORY-0105's business.

## Tests

`GameStatesTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `handStateIsAFreshPreflopPosition` | `street == PREFLOP`, `board == Board.EMPTY`, `pot == 0`, `betToMatch == 0`, `minRaiseTo == BIG_BLIND`, `seatToAct == 0`, `buttonSeat == 0`, `eventCount == 0` |
| `handStateStartsWithEqualStacks` | both seats have `START_STACK`, and `chipsInPlay == 2 * START_STACK` |
| `seatsAcceptDifferentStacks` | `handState(seats(500, 1_500))` gives stacks 500 and 1 500 at indices 0 and 1 |
| `handStateIsCopyable` | `handState().copy(pot = 300).pot == 300` and the original is unchanged |

## Acceptance criteria

- [ ] `GameStatesTest.handStateIsAFreshPreflopPosition` passes
- [ ] `GameStatesTest.handStateStartsWithEqualStacks` passes
- [ ] `GameStatesTest.seatsAcceptDifferentStacks` passes
- [ ] `GameStatesTest.handStateIsCopyable` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
