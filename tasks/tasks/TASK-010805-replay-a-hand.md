---
schema: 2
id: TASK-010805
title: Replay a hand from its log
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, replay, log]
depends_on: [TASK-010804]
verify:
  - ./gradlew :poker-engine:test --tests '*HandReplayTest'
  - ./gradlew :poker-engine:check
---

## Goal

`replayHand(log)` re-runs a recorded hand through the engine and hands back the opening state,
the state after every action, and every event it regenerated — the payoff `ADR-0001` was chosen
for.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/HandReplay.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandReplayTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt`,
`.../game/DefaultPokerEngine.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt`.

## Scope

- `HandReplay.kt` holds one public data class and one public function, both with KDoc:

  ```kotlin
  public data class HandReplay(
      val opening: GameState,
      val statesAfterAction: List<GameState>,
      val events: List<GameEvent>,
  ) {
      public val finalState: GameState get() = statesAfterAction.lastOrNull() ?: opening
  }

  public fun replayHand(log: HandLog): HandReplay
  ```

- The body, and nothing more:
  1. `startHand(log.handNumber, log.buttonSeat, log.stacks, log.smallBlind, log.bigBlind,
     SplitMix64Rng(log.seed))` — the same seed rebuilds the same shuffle, which is why the deck
     is not in the log.
  2. For each recorded action in order, `DefaultPokerEngine.handle(state, action)`; append its
     events, keep its `newState`, and record that state in `statesAfterAction`.
  3. Return the opening state, the per-action states index-aligned with `log.actions`, and every
     regenerated event in order, opening events first.
- A recorded action the engine rejects — including an action arriving after the hand is over —
  throws `IllegalStateException` whose message contains `action at index $i`, the action and the
  rejection. A log that cannot be replayed is corrupt, and saying so loudly is the whole point.
- The engine is fixed to `DefaultPokerEngine`: no engine parameter, no other knobs.

## Out of scope

- Comparing the regenerated events against `log.events` — `TASK-010806` adds exactly that, so do
  not read `log.events` in this ticket.
- Per-event stepping for a replay viewer — EPIC-03, not yet ticketed.
- Replaying more than one hand — `TASK-010811`.

## Tests

`HandReplayTest`, JUnit 5, package `duels.poker.engine.log`. Two ways to get a log, and no
others — **every test log carries the events the engine actually produced; never hand-write an
event list**, because `TASK-010806` will compare them:

- `val started = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(42))`, giving a
  log with `events = started.events` and whatever actions the test wants;
- `playRandomHand(seed)` from `RandomHandPlayer.kt` (test-only, `internal`, visible here), whose
  `PlayedHand` carries the opening state, actions and events a private `logOf` helper turns into
  a `HandLog`.

| Test | Proves |
| --- | --- |
| `replaysAPlayedHandToTheSameFinalState` | for `playRandomHand(7L)`, `replayHand(log).finalState == played.finalState`, deck and rng included |
| `regeneratesTheSameEventsInTheSameOrder` | for the same hand, `replayHand(log).events == played.events` |
| `recordsOneStateForEachAction` | `statesAfterAction.size == log.actions.size` and `finalState === statesAfterAction.last()` |
| `emptyActionsReplayToTheOpeningState` | a log built from `started` with no actions replays to `opening == started.newState` and `finalState == opening` |
| `rejectsALogWhoseActionTheEngineRefuses` | that same log with `actions = listOf(PlayerAction.Check(0))` — a check facing the big blind — throws `IllegalStateException` whose message contains `action at index 0` |

## Acceptance criteria

- [ ] `HandReplayTest.replaysAPlayedHandToTheSameFinalState` passes
- [ ] `HandReplayTest.regeneratesTheSameEventsInTheSameOrder` passes
- [ ] `HandReplayTest.recordsOneStateForEachAction` passes
- [ ] `HandReplayTest.emptyActionsReplayToTheOpeningState` passes
- [ ] `HandReplayTest.rejectsALogWhoseActionTheEngineRefuses` passes
- [ ] No test in the file constructs a `HandLog` whose `events` were written by hand rather than
      produced by the engine
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
