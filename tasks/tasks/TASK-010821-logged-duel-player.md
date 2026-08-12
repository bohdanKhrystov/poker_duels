---
schema: 2
id: TASK-010821
title: Play a whole duel and keep its log
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, replay, log, test-harness]
depends_on: [TASK-010820]
verify:
  - ./gradlew :poker-engine:test --tests '*LoggedDuelPlayerTest'
  - ./gradlew :poker-engine:check
---

## Goal

A test harness plays one duel from a seed and hands back a `MatchLog` in which every hand replays
exactly.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/LoggedDuelPlayer.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/LoggedDuelPlayerTest.kt` | create |

Read, do not modify: `poker-engine/src/test/kotlin/duels/poker/engine/duel/RandomDuelPlayer.kt`
(the shape to follow), `.../game/RandomHandPlayer.kt` (it declares the `internal fun pickAction`
this harness reuses).

## Scope

- One internal function, test sources only, package `duels.poker.engine.log`:

  ```kotlin
  internal fun playLoggedDuel(
      seed: Long,
      format: DuelFormat = DuelFormat.DEFAULT,
      maxHands: Int = 500,
      maxActionsPerHand: Int = 200,
  ): MatchLog
  ```

- **Per-hand seeds are the point of this harness.** A match `SplitMix64Rng(seed)` yields one seed
  per hand through `nextLong()` — take `draw.value` as the hand's seed and carry `draw.next`
  forward — and the hand is opened with `startNextHand(match, SplitMix64Rng(handSeed))`. That is
  what lets each recorded `HandLog` be replayed on its own by `replayHand`, which reconstructs the
  hand from `SplitMix64Rng(log.seed)`.
- Decisions come from a separate `SplitMix64Rng(seed)` chain advanced through the existing
  `pickAction`, so this harness makes decisions exactly as `playRandomHand` and `playRandomDuel` do.
- The first hand's button is `(seed % 2).toInt()`, as `playRandomDuel` does, via
  `MatchState.start(format, buttonSeat = ...)`.
- Per hand, record a `HandLog` with `seed = handSeed`, the hand number, button, opening stacks and
  blinds the match handed to `startNextHand`, the actions fed in, and every event produced —
  the opening `EngineResult.events` first, then each accepted action's events, in order.
- Drive each hand to `state.isHandOver`, fold it back with `recordHand`, and stop when
  `outcomeOf(match)` is non-null. Finish by returning
  `MatchLog(format, firstButtonSeat, hands, listOfNotNull(matchFinishedEvent(match)))`.
- Throw `AssertionError` naming the seed if `maxHands` hands pass with no outcome, if a hand needs
  more than `maxActionsPerHand` actions, if an action is rejected, or if a hand that is not over
  has no seat to act — the same failure modes `playRandomDuel` already reports.

## Out of scope

- **Do not modify `RandomDuelPlayer.kt`.** `DuelInvariantTest`, `DuelTerminationTest` and
  `DuelPlaythroughTest` are pinned to the duels `playRandomDuel` plays today, and switching it to
  per-hand seeds would change every one of them. This is a second harness that records logs, not a
  change to the first.
- Re-asserting the betting or settlement invariants: `BettingInvariantTest`,
  `SettlementInvariantTest` and `DuelInvariantTest` already own them.
- Replaying a whole match — `TASK-010822`.

## Tests

`LoggedDuelPlayerTest`, JUnit 5, package `duels.poker.engine.log`. Keep the seed ranges small;
each duel plays many hands.

| Test | Proves |
| --- | --- |
| `everyHandInALoggedDuelReplaysExactly` | for seeds `1..10`, `replayHand(handLog).events == handLog.events` for every hand in the log — the per-hand seed is right |
| `theLogEndsWithMatchFinished` | for seeds `1..10`, the log's last event is a `MatchFinished` whose `outcome.handsPlayed == log.hands.size` |
| `theSameSeedProducesTheSameLog` | `playLoggedDuel(7) == playLoggedDuel(7)` |
| `theLastHandsStacksAreTheOutcomesStacks` | the `MatchFinished` outcome's `finalStacks` equal the stacks after replaying the final hand |
| `theCeilingIsAnAssertionNotAnAssumption` | `playLoggedDuel(seed = 1, maxHands = 0)` throws `AssertionError` naming the seed |

## Acceptance criteria

- [ ] `LoggedDuelPlayerTest.everyHandInALoggedDuelReplaysExactly` passes
- [ ] `LoggedDuelPlayerTest.theLogEndsWithMatchFinished` passes
- [ ] `LoggedDuelPlayerTest.theSameSeedProducesTheSameLog` passes
- [ ] `LoggedDuelPlayerTest.theLastHandsStacksAreTheOutcomesStacks` passes
- [ ] `LoggedDuelPlayerTest.theCeilingIsAnAssertionNotAnAssumption` passes
- [ ] `DuelInvariantTest`, `DuelTerminationTest` and `DuelPlaythroughTest` still pass unchanged,
      because `RandomDuelPlayer.kt` is not touched
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
