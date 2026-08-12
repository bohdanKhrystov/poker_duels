---
schema: 2
id: TASK-020409
title: A duel harness that records every state and every event
type: task
status: ready
parent: STORY-0204
module: poker-ai
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [ai, simulation, test-harness]
depends_on: []
verify:
  - ./gradlew :poker-ai:test --tests '*ObservedDuelTest'
  - ./gradlew :poker-ai:check
---

## Goal

`observeDuel(seed)` plays a whole `RandomBot` duel and keeps, for every point in it, the state and
every event produced so far — the raw material the redaction properties need.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/test/kotlin/duels/poker/ai/ObservedDuel.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/ObservedDuelTest.kt` | create |

Read, do not modify: `poker-ai/src/main/kotlin/duels/poker/ai/DuelSimulator.kt` (the loop to
mirror: match rng for hand seeds, a separate decision rng threaded across the duel),
`poker-ai/src/main/kotlin/duels/poker/ai/Bot.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt`
(`startNextHand` returns an `EngineResult`, so its `events` are the hand's opening events).

## Scope

- Test sources only, `internal`, package `duels.poker.ai`. Nothing here ships in the `poker-ai`
  library: it exists to feed properties, like `LoggedDuelPlayer` does in the engine.
- Four declarations with KDoc:

  ```kotlin
  internal data class ObservedStep(val state: GameState, val eventsSoFar: List<GameEvent>)

  internal data class ObservedHand(
      val handNumber: Int,
      val handSeed: Long,
      val steps: List<ObservedStep>,
  ) {
      val events: List<GameEvent> get() = steps.last().eventsSoFar
      val finalState: GameState get() = steps.last().state
  }

  internal data class ObservedDuel(val seed: Long, val hands: List<ObservedHand>)

  internal fun observeDuel(seed: Long, maxHands: Int = 500, maxActionsPerHand: Int = 200): ObservedDuel

  internal fun seedReport(duelSeed: Long, handSeed: Long): String
  ```

- The loop mirrors `simulateDuel`: `MatchState.start(DuelFormat.DEFAULT)`, a `SplitMix64Rng(seed)`
  drawing each hand's seed, a second `SplitMix64Rng(seed)` threading `RandomBot`'s decisions,
  `startNextHand`, `DefaultPokerEngine.handle`, `recordHand`, stopping when `outcomeOf` is
  non-null. Every hand opens with one `ObservedStep(openedState, openingEvents)` and appends one
  step per accepted action, whose `eventsSoFar` is everything before it plus `result.events`.
- `seedReport(duelSeed, handSeed)` returns a string containing both numbers, e.g.
  `"duel seed 7, hand seed -1234"`. Every property test built on this harness puts it in its
  assertion messages, so a failure is reproducible from the report alone.
- Determinism: all randomness through `SplitMix64Rng`; no `kotlin.random.Random`, no clock, no
  I/O. `AssertionError` naming the seed if an action is rejected, a hand exceeds
  `maxActionsPerHand`, or the duel exceeds `maxHands`.

## Out of scope

- Any redaction assertion — `TASK-020410` and `TASK-020411` are the consumers.
- Changing `simulateDuel` or anything under `poker-ai/src/main`. `SimulatedHand` carries no
  events and widening it would change a published type for a test's convenience.
- Invariant checks: `firstViolation` already runs inside `simulateDuel`, and re-running it here
  would duplicate `SimulationRunnerTest`'s job.

## Tests

`ObservedDuelTest`, JUnit 5, package `duels.poker.ai`, `@Timeout(120)` on the seed-range tests.

| Test | Proves |
| --- | --- |
| `everyHandEndsComplete` | over seeds `1L..20L`, every `ObservedHand.finalState.isHandOver` is true |
| `theSameSeedObservesTheSameDuel` | `observeDuel(7)` twice returns equal `ObservedDuel`s |
| `eachStepsEventsExtendThePreviousStep` | for every consecutive pair of steps in every hand of seeds `1L..20L`, the earlier `eventsSoFar` is a prefix of the later |
| `theFirstStepCarriesTheOpeningEvents` | the first step of a hand holds a `HandStarted` and two `HoleCardsDealt`, one per seat |
| `theSampleContainsShowdownsAndFolds` | over seeds `1L..20L`, at least one hand's events contain `ShowdownReached` and at least one contain `PlayerFolded` |
| `seedReportNamesBothSeeds` | `seedReport(7, -13)` contains `"7"` and `"-13"` |

## Acceptance criteria

- [ ] `ObservedDuelTest.everyHandEndsComplete` passes
- [ ] `ObservedDuelTest.theSameSeedObservesTheSameDuel` passes
- [ ] `ObservedDuelTest.eachStepsEventsExtendThePreviousStep` passes
- [ ] `ObservedDuelTest.theFirstStepCarriesTheOpeningEvents` passes
- [ ] `ObservedDuelTest.theSampleContainsShowdownsAndFolds` passes
- [ ] `ObservedDuelTest.seedReportNamesBothSeeds` passes
- [ ] Neither file imports `kotlin.random.Random`
- [ ] No file outside the two in the Files table is modified — in particular nothing under
      `poker-ai/src/main/` and nothing under `poker-engine/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
