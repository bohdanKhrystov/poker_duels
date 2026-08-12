---
schema: 2
id: TASK-010828
title: Simulate one duel between two bots, checking after every action
type: task
status: done
parent: STORY-0108
module: poker-ai
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [simulation, ai, duel]
depends_on: [TASK-010827]
verify:
  - ./gradlew :poker-ai:test --tests '*DuelSimulatorTest'
  - ./gradlew :poker-ai:check
---

## Goal

Two bots play a whole duel from one seed with no UI, the invariants are checked after every
action, and a failure hands back the `(seed, actions)` pair that reproduces it.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/main/kotlin/duels/poker/ai/DuelSimulator.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/DuelSimulatorTest.kt` | create |

Read, do not modify: `poker-ai/src/main/kotlin/duels/poker/ai/Bot.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt`
(`startNextHand`, `recordHand`), `.../duel/DuelOutcome.kt` (`outcomeOf`).

## Scope

- Three public types and one public function with KDoc, package `duels.poker.ai`:

  ```kotlin
  public data class SimulatedHand(val handNumber: Int, val handSeed: Long, val actions: List<PlayerAction>)

  public data class SimulatedDuel(val seed: Long, val outcome: DuelOutcome, val hands: List<SimulatedHand>)

  public class SimulationFailure(
      public val seed: Long,
      public val handNumber: Int,
      public val handSeed: Long,
      public val actions: List<PlayerAction>,
      message: String,
  ) : RuntimeException(message)

  public fun simulateDuel(
      seed: Long,
      bots: List<Bot>,
      format: DuelFormat = DuelFormat.DEFAULT,
      maxHands: Int = 500,
      maxActionsPerHand: Int = 200,
  ): SimulatedDuel
  ```

- Per-hand seeds are drawn from a match `SplitMix64Rng(seed)` through `nextLong()` — the hand's
  seed is `draw.value`, the match carries `draw.next` forward — and the hand is opened with
  `startNextHand(match, SplitMix64Rng(handSeed))`. This is the same convention `TASK-010821` uses,
  and it is what makes a reported `(handSeed, actions)` pair replayable through the engine's
  `replayHand`.
- Decisions: `bots[seatToAct].choose(state, legalActions(state), decisionRng)`, with `decisionRng`
  starting as `SplitMix64Rng(seed)` and advanced to `choice.rng` after every decision. `bots` must
  hold exactly two entries, seat-indexed.
- After every accepted action, call `firstViolation(state, chipsAtOpen)` where `chipsAtOpen` is the
  hand's chip total at the deal. A non-null answer throws `SimulationFailure` carrying the duel
  seed, the hand number, the hand seed, the actions taken in that hand so far, and the message.
- The same `SimulationFailure` is thrown when the engine rejects a bot's action, when a hand that
  is not over has no seat to act, when a hand exceeds `maxActionsPerHand`, and when `maxHands`
  hands pass with no outcome. Every failure a run can produce is one type, carrying the two lines
  of data that reproduce it — `ADR-0001`.
- Single-threaded. Randomness comes only from `Rng`; no `kotlin.random.Random`, no clock.

## Out of scope

- Running many duels or aggregating them — `TASK-010829`.
- Building a `MatchLog` — that is the engine's `TASK-010821`, and `poker-ai` does not need it to
  report a failure.
- Bots that play well — `EPIC-09`.

## Tests

`DuelSimulatorTest`, JUnit 5, package `duels.poker.ai`. Use `listOf(RandomBot, RandomBot)`.

| Test | Proves |
| --- | --- |
| `aDuelBetweenTwoRandomBotsProducesAnOutcome` | for seeds `1..20`, `simulateDuel` returns an outcome with a winner and at least one hand |
| `theSameSeedProducesTheSameDuel` | `simulateDuel(7, bots) == simulateDuel(7, bots)` |
| `everyHandRecordsItsSeedAndItsActions` | every `SimulatedHand` has a non-empty `actions` list and hand numbers run `1..n` without a gap |
| `anIllegalBotActionIsReportedWithTheSeedAndActions` | a stub `Bot` that always returns `PlayerAction.Bet(seat, 1)` makes `simulateDuel` throw `SimulationFailure` whose `seed` and `handSeed` are set and whose `actions` are the ones taken |
| `theHandCeilingIsAnAssertionNotAnAssumption` | `maxHands = 0` throws `SimulationFailure` naming the seed |

## Acceptance criteria

- [ ] `DuelSimulatorTest.aDuelBetweenTwoRandomBotsProducesAnOutcome` passes
- [ ] `DuelSimulatorTest.theSameSeedProducesTheSameDuel` passes
- [ ] `DuelSimulatorTest.everyHandRecordsItsSeedAndItsActions` passes
- [ ] `DuelSimulatorTest.anIllegalBotActionIsReportedWithTheSeedAndActions` passes
- [ ] `DuelSimulatorTest.theHandCeilingIsAnAssertionNotAnAssumption` passes
- [ ] `DuelSimulator.kt` names no platform random source and no clock
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
