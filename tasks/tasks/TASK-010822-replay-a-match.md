---
schema: 2
id: TASK-010822
title: Replay a whole duel from its log
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, replay, log, duel]
depends_on: [TASK-010821]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchReplayTest'
  - ./gradlew :poker-engine:check
---

## Goal

Replaying a `MatchLog` reproduces every hand in it and the duel's own result, and a log that
disagrees with the engine fails naming the hand it disagreed on.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchReplay.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/MatchReplayTest.kt` | create |

Read, do not modify: `.../log/HandReplay.kt` (it declares `HandReplay` and `replayHand`),
`.../duel/MatchProgression.kt` (`startNextHand`, `recordHand`), `.../duel/DuelOutcome.kt`
(`outcomeOf`).

## Scope

- One data class and one function, both public, both with KDoc, in `duels.poker.engine.log`:

  ```kotlin
  public data class MatchReplay(
      val hands: List<HandReplay>,
      val finalMatch: MatchState,
      val outcome: DuelOutcome?,
  )

  public fun replayMatch(log: MatchLog): MatchReplay
  ```

- Start from `MatchState.start(log.format, log.buttonSeat)`. For each `HandLog` in order:
  1. check the log against what the match says the next hand must be — hand number, button seat,
     opening stacks, and the blinds `MatchState.blinds` derives for that hand. A hand log claiming
     blinds the schedule does not give is corrupt, and this is the only place that can notice;
  2. `replayHand(handLog)`, which already re-runs the hand from its seed and compares every event;
  3. `recordHand(match, replay.finalState)`.
- Wrap any `IllegalStateException` or `IllegalArgumentException` thrown while replaying a hand in
  one whose message starts with `hand <handNumber>: ` and rethrow, so the failure names the hand as
  well as the event index `replayHand` already names. Do not swallow the original message.
- After the last hand, `outcome = outcomeOf(match)`. If the log carries a `MatchFinished`, require
  its `outcome` to equal the replayed one — a log whose recorded ending is not the ending the
  engine reaches is corrupt.
- `replayMatch` re-uses `replayHand` rather than reimplementing any hand logic, and holds no rules
  of its own beyond match progression.

## Out of scope

- Stepping to hand `n` and action `m` for a viewer — not ticketed; `EPIC-03` needs it and
  `MatchReplay.hands[n].statesAfterAction[m]` already gives it.
- Serialising the log — `TASK-010826`.
- Changing `replayHand` or its divergence messages — `TASK-010806` owns those.

## Tests

`MatchReplayTest`, JUnit 5, package `duels.poker.engine.log`. Logs come from
`playLoggedDuel(seed)`; keep the seed ranges small.

| Test | Proves |
| --- | --- |
| `replayingALoggedDuelReproducesItsOutcome` | for seeds `1..10`, `replayMatch(log).outcome` equals the log's `MatchFinished` outcome |
| `replayingReproducesEveryHandsEvents` | for seeds `1..10`, `replay.hands[i].events == log.hands[i].events` for every hand |
| `replayIsStable` | `replayMatch(log) == replayMatch(log)` for one log |
| `aHandMissingItsLastActionFailsNamingTheHand` | dropping the last action of the log's **second** hand throws, and the message contains `hand 2` |
| `aHandLogWithTheWrongBlindsIsRejected` | doubling the first hand log's `smallBlind` and `bigBlind` throws, and the message contains `hand 1` |
| `aMatchFinishedThatDisagreesWithTheReplayIsRejected` | replacing the log's `MatchFinished` with one naming the other winner throws |

## Acceptance criteria

- [ ] `MatchReplayTest.replayingALoggedDuelReproducesItsOutcome` passes
- [ ] `MatchReplayTest.replayingReproducesEveryHandsEvents` passes
- [ ] `MatchReplayTest.replayIsStable` passes
- [ ] `MatchReplayTest.aHandMissingItsLastActionFailsNamingTheHand` passes
- [ ] `MatchReplayTest.aHandLogWithTheWrongBlindsIsRejected` passes
- [ ] `MatchReplayTest.aMatchFinishedThatDisagreesWithTheReplayIsRejected` passes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
