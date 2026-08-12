---
schema: 2
id: TASK-010713
title: Play a whole duel from one seed, and prove it produces a winner
type: task
status: done
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, duel, tests]
depends_on: [TASK-010710, TASK-010712]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelPlaythroughTest'
  - ./gradlew :poker-engine:test --tests '*BettingInvariantTest'
  - ./gradlew :poker-engine:test --tests '*SettlementInvariantTest'
  - ./gradlew :poker-engine:test --tests '*CardSecrecyTest'
  - ./gradlew :poker-engine:check
---

## Goal

A duel plays end to end from a seed alone — deal, bet, settle, next hand — and ends with one
winner holding every chip.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/RandomDuelPlayer.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelPlaythroughTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt` and
`.../duel/DuelOutcome.kt`. Modify neither.

## Scope

- `RandomHandPlayer.kt`: `private fun pickAction` becomes `internal fun pickAction`, with a
  one-line comment saying the duel harness reuses it so both harnesses make decisions the same
  way. **Nothing else in the file changes** — not its signature, not its arithmetic, not a seed,
  not an invariant check. Visibility is not observable to a caller inside the same module, so
  `BettingInvariantTest`, `SettlementInvariantTest` and `CardSecrecyTest` all keep passing with
  no edit; the `verify` block runs them to prove it.
- `RandomDuelPlayer.kt`, package `duels.poker.engine.duel`, test source set, everything
  `internal`, modelled on `RandomHandPlayer.kt`:
  - `internal data class HandSummary(val handNumber: Int, val buttonSeat: Int, val blinds: BlindLevel, val stacksAfter: List<Int>)`
  - `internal data class PlayedDuel(val seed: Long, val format: DuelFormat, val outcome: DuelOutcome, val hands: List<HandSummary>)`
  - `internal fun playRandomDuel(seed: Long, format: DuelFormat = DuelFormat.DEFAULT, maxHands: Int = 500, maxActionsPerHand: Int = 200): PlayedDuel`
  - Start from `MatchState.start(format, buttonSeat = (seed % 2).toInt())`. While
    `outcomeOf(match) == null`: open the hand with `startNextHand(match, rng)`, drive it to
    `state.isHandOver` by feeding `pickAction(seatToAct, legalActions(state), decisionRng)` to
    `DefaultPokerEngine.handle`, then `match = recordHand(match, state)` and append a
    `HandSummary`.
  - Two random sources, exactly as `playRandomHand` splits them: the shuffle `Rng` is carried
    forward from `state.rng` after each hand, so one seed deals a different board every hand; the
    decision `Rng` is a separate `SplitMix64Rng(seed)` advanced by `pickAction`.
  - Both ceilings throw `AssertionError` naming the seed and the hand number: `maxHands` hands
    without an outcome, and `maxActionsPerHand` actions inside one hand. **The hand ceiling is
    checked before a hand is opened**, so `maxHands = 0` fails immediately — `TASK-010715` relies
    on that.
  - A rejected action and a `null` `seatToAct` in a hand that has not ended each throw
    `AssertionError` naming the seed, as in `playRandomHand`. The harness does **not** re-assert
    the per-action betting or settlement invariants: `BettingInvariantTest` and
    `SettlementInvariantTest` already own those.
  - Head the file with `@file:Suppress("ktlint:standard:filename")` and the same one-line reason
    `RandomHandPlayer.kt` carries: the file is named after its entry point, not after a class.

## Out of scope

- Button, blind and chip invariants across the duel — `TASK-010714`.
- Termination over a large sample — `TASK-010715`.
- Fixed-length duels — `TASK-010716`.
- Any change to `playRandomHand` itself, or to any file under `src/main`.

## Tests

`DuelPlaythroughTest`, over seeds `1L..20L` under `DuelFormat.DEFAULT`, with `@Timeout(60)` on
each test

| Test | Proves |
| --- | --- |
| `aDefaultFreezeoutEndsWithExactlyOneWinner` | for each seed: `outcome.winner != null`, the winner's final stack is `2 * DuelFormat.DEFAULT.startingStack` and the loser's is `0` |
| `theOutcomeAgreesWithTheHandsPlayed` | for each seed: `outcome.handsPlayed == hands.size`, `hands.last().stacksAfter == outcome.finalStacks`, and the hand numbers run `1..hands.size` with no gap |

Every failure message names its seed.

## Acceptance criteria

- [ ] `DuelPlaythroughTest.aDefaultFreezeoutEndsWithExactlyOneWinner` passes
- [ ] `DuelPlaythroughTest.theOutcomeAgreesWithTheHandsPlayed` passes
- [ ] `BettingInvariantTest`, `SettlementInvariantTest` and `CardSecrecyTest` pass with no edit
      to their files — the only change to `RandomHandPlayer.kt` is one visibility keyword and a
      comment
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
