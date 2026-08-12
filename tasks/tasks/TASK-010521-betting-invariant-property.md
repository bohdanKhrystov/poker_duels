---
schema: 2
id: TASK-010521
title: Assert the betting invariants over a thousand random hands
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, test, chips]
depends_on: [TASK-010520]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingInvariantTest'
  - ./gradlew :poker-engine:check
---

## Goal

Chip conservation and the other betting invariants hold not only in the hands somebody thought
to write down, but in a thousand nobody designed — and a failure names the seed that reproduces
it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingInvariantTest.kt` | create |

Read `HandSetup.kt`, `BettingRules.kt`, `NoOpEngine.kt`. Modify none of them — this ticket adds
no production code. Anything it finds becomes a new ticket.

## Scope

- `RandomHandPlayer.kt`, test-only, one entry point:

  ```kotlin
  internal fun playRandomHand(
      seed: Long,
      engine: PokerEngine = DefaultPokerEngine,
      maxActions: Int = 200,
  ): PlayedHand
  ```

  with `internal data class PlayedHand(val opening: GameState, val actions: List<PlayerAction>,
  val events: List<GameEvent>, val finalState: GameState)`.

- The hand opens with `startHand(1, (seed % 2).toInt(), stacks, 50, 100, SplitMix64Rng(seed))`,
  where both stacks are derived arithmetically from the seed and range over `60..10_000`, so
  short stacks and forced all-ins occur. All randomness comes from `SplitMix64Rng` — never
  `kotlin.random.Random`, in test code as much as in the engine.
- While a seat is to act: pick uniformly from `legalActions(state).allowed`, sorted for
  determinism; a `BET` takes `minBetTo` or `allInTo`, a `RAISE` takes `minRaiseTo` or `allInTo`,
  each chosen by the same generator. Feed it to `engine.handle`.
- After **every** action, assert, and on failure throw `AssertionError` whose message carries the
  seed and the actions so far:

  | Invariant |
  | --- |
  | `chipsInPlay` equals the opening `chipsInPlay` |
  | no stack is negative and no `committedThisStreet` exceeds `committedThisHand` |
  | the result is not rejected — the harness only ever sends actions `legalActions` allowed |
  | if a seat is to act, it has not folded, is not all-in and has chips |
  | `betToMatch >= seats.maxOf { it.committedThisStreet }` |
  | event sequences are dense and continue `state.eventCount` |

- The hand ends at `SHOWDOWN` or with a folded seat. Exceeding `maxActions` throws an
  `AssertionError` naming the seed — a hand that never terminates is a bug, not a timeout.

## Out of scope

- Showdown correctness and pot awards — STORY-0106.
- Match-level simulation over many hands — `TASK-010803`.

## Tests

`BettingInvariantTest`, JUnit 5, `@Timeout(30)` on the two long tests so a stall fails rather
than hangs CI.

| Test | Proves |
| --- | --- |
| `aThousandRandomHandsHoldEveryInvariant` | `playRandomHand(seed)` for seeds `1..1000` throws nothing |
| `everyRandomHandEndsAtShowdownOrWithAFold` | each final state has `street == SHOWDOWN` or a seat with `hasFolded` |
| `everyRandomHandsLogReproducesItsFinalState` | for seeds `1..50`, `StateProjection.fold(opening, events)` equals `finalState` except `deck` and `rng` |
| `theSameSeedPlaysTheSameHandTwice` | `playRandomHand(7L)` twice gives equal action lists and equal final states |
| `aHandThatCannotProgressNamesItsSeed` | `playRandomHand(3L, engine = NoOpEngine)` throws `AssertionError` whose message contains `3` |

## Acceptance criteria

- [ ] `BettingInvariantTest.aThousandRandomHandsHoldEveryInvariant` passes
- [ ] `BettingInvariantTest.everyRandomHandEndsAtShowdownOrWithAFold` passes
- [ ] `BettingInvariantTest.everyRandomHandsLogReproducesItsFinalState` passes
- [ ] `BettingInvariantTest.theSameSeedPlaysTheSameHandTwice` passes
- [ ] `BettingInvariantTest.aHandThatCannotProgressNamesItsSeed` passes
- [ ] Neither test file references `kotlin.random.Random`
- [ ] No file under `src/main` is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
