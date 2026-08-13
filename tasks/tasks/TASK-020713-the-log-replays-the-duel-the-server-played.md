---
schema: 2
id: TASK-020713
title: The MatchLog the runner wrote replays into the duel the server actually played
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, duel, property, replay]
depends_on: [TASK-020712]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerReplayTest'
  - ./gradlew :poker-server:check
---

## Goal

`replayMatch(runner.log)` reproduces every duel the runner played, card for card, and does so after
a round trip through JSON — which is what makes the seed recorded in each `HandLog` worth recording.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerReplayTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/duel/PlayedDuel.kt` (`playDuel`),
`poker-engine/src/main/kotlin/duels/poker/engine/log/MatchReplay.kt` (`replayMatch` re-runs each
hand from its own seed and compares every event, and rejects a log whose recorded ending is not the
one the engine reaches), `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLogJson.kt`
(`encodeMatchLog`, `decodeMatchLog`).

## Scope

- One test class, no production code. Package `duels.poker.server.duel`. Seeds `1L..20L`,
  `@Timeout(120)`.
- The assertions are against `replayMatch`'s own result: `MatchReplay.outcome`, `finalMatch` and
  `hands`. The test re-derives nothing — if `replayMatch` accepts the log at all, most of the
  property is already proved, because it is the function that compares every replayed event with
  the recorded one and throws on the first divergence.
- Every failure message names the duel seed.

## Out of scope

- Persisting the log anywhere — `DEC-008`, `STORY-0210`.
- Replaying from the *frames* a client received: a client is not given the seed and cannot replay a
  hand, which is exactly the point of `TASK-020712`.

## Tests

`RunnerReplayTest`, JUnit 5, package `duels.poker.server.duel`.

| Test | Proves |
| --- | --- |
| `everyDuelReplaysFromItsLog` | for each seed, `replayMatch(runner.log)` returns without throwing |
| `theReplayReachesTheSameOutcome` | `replayMatch(runner.log).outcome == runner.outcome` |
| `theReplayReachesTheSameMatchState` | `replayMatch(runner.log).finalMatch == runner.match` |
| `theReplayCoversEveryHandThatWasPlayed` | `replayMatch(runner.log).hands.size == runner.log.hands.size`, and each replayed hand's `finalState.handNumber` is its 1-based position |
| `theLogReplaysAfterAJsonRoundTrip` | `replayMatch(decodeMatchLog(encodeMatchLog(runner.log)))` reaches the same outcome and `finalMatch` |
| `atamperedSeedIsRejected` | replaying a log whose first hand's `seed` has been changed throws — the log's events no longer follow from its seed |

## Acceptance criteria

- [ ] `RunnerReplayTest.everyDuelReplaysFromItsLog` passes
- [ ] `RunnerReplayTest.theReplayReachesTheSameOutcome` passes
- [ ] `RunnerReplayTest.theReplayReachesTheSameMatchState` passes
- [ ] `RunnerReplayTest.theReplayCoversEveryHandThatWasPlayed` passes
- [ ] `RunnerReplayTest.theLogReplaysAfterAJsonRoundTrip` passes
- [ ] `RunnerReplayTest.atamperedSeedIsRejected` passes
- [ ] Every failure message names the duel seed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
