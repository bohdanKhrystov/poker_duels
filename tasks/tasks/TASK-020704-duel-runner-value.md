---
schema: 2
id: TASK-020704
title: The DuelRunner value — a live hand, its match, its logs, and the invariants tying them together
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, duel, engine-integration]
depends_on: [TASK-020703]
verify:
  - ./gradlew :poker-server:test --tests '*DuelRunnerTest'
  - ./gradlew :poker-server:check
---

## Goal

`DuelRunner` exists as an immutable value holding one duel: the `MatchState`, the live hand and its
`HandLog`, the `MatchLog` so far, and the outcome once there is one — with the three ways those
four can disagree rejected in `init`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelRunner.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelRunnerTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/log/HandLog.kt` and
`poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLog.kt` (both accept an unfinished
duel), `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchState.kt` (`nextHandNumber`),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt` (`outcomeOf`).

## Scope

- Package `duels.poker.server.duel`. Three declarations, KDoc included:

  ```kotlin
  public data class LiveHand(val state: GameState, val log: HandLog)

  public data class DuelRunner(
      val match: MatchState,
      val hand: LiveHand?,
      val log: MatchLog,
      val outcome: DuelOutcome?,
  )

  public data class DuelStep(val runner: DuelRunner, val outbound: List<Addressed>)
  ```

- **Immutable, `val` only, no `var` anywhere.** Every operation on a runner returns a `DuelStep`
  carrying the next runner. A duel therefore has no internal race of its own, and how a room
  serialises callers stays somebody else's problem (`DEC-013`, `TASK-020714`).
- `LiveHand.init` requires `state.handNumber == log.handNumber` and `log.events.size ==
  state.eventCount` — the log is exactly this state's history, not a parallel one. Both hold by
  construction because event sequences are dense from 0 and `StateProjection` sets `eventCount` to
  `sequence + 1`.
- `DuelRunner.init` requires:
  - `(hand == null) == (outcome != null)` — a duel has a live hand until it is over, and never
    after; the message names which side was violated.
  - `hand == null || hand.state.handNumber == match.nextHandNumber` — the live hand is the hand the
    match says comes next.
  - `outcome == null || outcome == outcomeOf(match)` — a recorded outcome is the engine's own, never
    one this module computed.
- No `startDuel`, no `act`, no `advance` here: this ticket ships the value and its invariants only.
- The KDoc records one thing `init` deliberately does **not** check: that a live hand has a seat to
  act. A hand that has just ended is briefly held in a `DuelRunner` before `TASK-020707` folds it
  back into the match, so requiring it here would forbid a legal intermediate value. That a runner
  *returned* from `startDuel`, `act` or `advance` always awaits an action is those functions'
  postcondition, and each has a test for it.
- KDoc on `DuelRunner` states that the runner adds no rules — blinds, button, hand numbering and the
  end condition all come from the engine — and that arithmetic on a blind or a button in this
  package is a review finding.

## Out of scope

- Opening a hand or a duel — `TASK-020705`.
- Applying an action, ending a hand, ending the duel — `TASK-020706` … `TASK-020708`.
- Where a live runner is stored between frames — `DEC-013`, `TASK-020714`.

## Tests

`DuelRunnerTest`, JUnit 5, package `duels.poker.server.duel`. Build a live hand from
`startNextHand(MatchState.start(DuelFormat.DEFAULT, 0), SplitMix64Rng(7))` and a `HandLog` whose
`seed`, `handNumber`, `buttonSeat`, `stacks` and blinds come from that same `MatchState`, with
`actions = emptyList()` and `events = result.events`. An empty `MatchLog` is
`MatchLog(DuelFormat.DEFAULT, 0, emptyList(), emptyList())`.

| Test | Proves |
| --- | --- |
| `aRunningDuelHoldsALiveHandAndNoOutcome` | the fixture constructs, and `hand` is non-null with `outcome` null |
| `aLiveHandWithAnOutcomeIsRejected` | constructing with both `hand` and a non-null `outcome` throws `IllegalArgumentException` |
| `aDuelWithNeitherHandNorOutcomeIsRejected` | constructing with `hand = null` and `outcome = null` throws `IllegalArgumentException` |
| `aLiveHandMustBeTheHandTheMatchExpectsNext` | a `LiveHand` for hand 2 against a `MatchState` with `handsPlayed = 0` throws `IllegalArgumentException` |
| `aRecordedOutcomeMustBeTheEnginesOwn` | `hand = null` with an outcome that is not `outcomeOf(match)` throws `IllegalArgumentException` |
| `aLogWhoseEventsDoNotMatchTheStateIsRejected` | a `LiveHand` whose `log.events` drops its last event throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelRunnerTest.aRunningDuelHoldsALiveHandAndNoOutcome` passes
- [ ] `DuelRunnerTest.aLiveHandWithAnOutcomeIsRejected` passes
- [ ] `DuelRunnerTest.aDuelWithNeitherHandNorOutcomeIsRejected` passes
- [ ] `DuelRunnerTest.aLiveHandMustBeTheHandTheMatchExpectsNext` passes
- [ ] `DuelRunnerTest.aRecordedOutcomeMustBeTheEnginesOwn` passes
- [ ] `DuelRunnerTest.aLogWhoseEventsDoNotMatchTheStateIsRejected` passes
- [ ] `DuelRunner.kt` declares no `var` and no mutable collection
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
