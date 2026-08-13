---
schema: 2
id: TASK-020710
title: A harness that plays a whole duel through the runner, seeing only what a client would see
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, duel, test-harness]
depends_on: [TASK-020709]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

`playDuel(seed)` plays a duel from the first deal to `MatchFinished` by answering the `YourTurn`
frames the runner sends — a real client's loop — and keeps every frame it produced, which is the
raw material the three properties after it need.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/PlayedDuel.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerDuelTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelStart.kt`,
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelAction.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/LegalActions.kt`.

## Scope

- Test sources only, `internal`, package `duels.poker.server.duel`. Nothing here ships.
- Two declarations, KDoc included:

  ```kotlin
  internal data class PlayedDuel(
      val seed: Long,
      val runner: DuelRunner,
      val outbound: List<Addressed>,
      val actions: Int,
  )

  internal fun playDuel(
      seed: Long,
      format: DuelFormat = DuelFormat.DEFAULT,
      buttonSeat: Int = 0,
      maxActions: Int = 20_000,
  ): PlayedDuel
  ```

- **The loop is a client, not an engine.** It never reads a `GameState`, a `LegalActions` computed
  from one, or the runner's logs to decide anything. Each turn it takes the single `Addressed` whose
  message is a `ServerMessage.YourTurn`, and builds `Act(turn.handNumber, turn.actionSequence,
  action)` from that frame alone, sending it as `act(step.runner, addressed.seat, message, seeds)`.
  This is what makes the properties downstream statements about the *boundary* rather than about the
  engine.
- The loop runs while the last step's outbound contains a `YourTurn`, and ends with
  `check(step.runner.hand == null)`. An `AssertionError` naming the seed if `maxActions` is exceeded
  or a step offers more than one `YourTurn`.
- The action policy is uniform over `turn.legalActions`, and reads nothing else:
  - `val types = legal.allowed.sorted()`, draw `rng.nextInt(types.size)`;
  - `FOLD`/`CHECK`/`CALL`/`ALL_IN` map to the matching `PlayerAction` with `legal.seat`;
  - `BET`/`RAISE` draw once more over 2, choosing `legal.minBetTo` / `legal.minRaiseTo` on 0 and
    `legal.allInTo` on 1.
  This mirrors `poker-ai`'s `RandomBot`, which cannot be imported here — `poker-ai` is not a
  dependency of `poker-server`.
- Determinism: one `SplitMix64Rng(seed)` threaded through the policy, and a second
  `SplitMix64Rng(seed)` behind the `HandSeedSource` handed to `act`. No `kotlin.random.Random`, no
  clock, no I/O. `playDuel(7)` twice must produce equal `PlayedDuel`s.
- `outbound` is every `Addressed` the runner returned, in order, starting with `startDuel`'s.

## Out of scope

- Any assertion about cards, chips or replay — `TASK-020711`, `TASK-020712`, `TASK-020713` are the
  consumers, and each owns its own test file.
- Sockets, `ConnectionWriter`, JSON. The harness deals in `Addressed` values; encoding them is
  `TASK-020712`'s business and writing them is `TASK-020715`'s.

## Tests

`RunnerDuelTest`, JUnit 5, package `duels.poker.server.duel`, `@Timeout(120)` on the seed-range
tests. The seed range is `1L..20L` unless a test says otherwise.

| Test | Proves |
| --- | --- |
| `everyDuelReachesAnOutcome` | for each seed, `runner.outcome != null` and `runner.hand == null` |
| `theOutcomeIsTheEnginesOwn` | for each seed, `runner.outcome == outcomeOf(runner.match)` |
| `theSameSeedPlaysTheSameDuel` | `playDuel(7)` twice gives equal `outbound`, `runner` and `actions` |
| `everyHandIsRecordedInOrder` | `runner.log.hands.map { it.handNumber }` is `1..runner.match.handsPlayed` |
| `theMatchLogRecordsExactlyOneEnding` | `runner.log.events.filterIsInstance<MatchFinished>().size == 1` |
| `aClientThatObeysYourTurnIsNeverRejected` | no `outbound` message is a `ServerMessage.Rejected` |
| `theSampleContainsBothShowdownsAndFolds` | across the range, some hand's events hold a `ShowdownReached` and some hold a `PlayerFolded` |

## Acceptance criteria

- [ ] `RunnerDuelTest.everyDuelReachesAnOutcome` passes
- [ ] `RunnerDuelTest.theOutcomeIsTheEnginesOwn` passes
- [ ] `RunnerDuelTest.theSameSeedPlaysTheSameDuel` passes
- [ ] `RunnerDuelTest.everyHandIsRecordedInOrder` passes
- [ ] `RunnerDuelTest.theMatchLogRecordsExactlyOneEnding` passes
- [ ] `RunnerDuelTest.aClientThatObeysYourTurnIsNeverRejected` passes
- [ ] `RunnerDuelTest.theSampleContainsBothShowdownsAndFolds` passes
- [ ] `PlayedDuel.kt` does not import `duels.poker.engine.game.GameState`, does not call
      `legalActions(`, and does not read `step.runner.hand` other than in the closing `check`
- [ ] Neither file imports `kotlin.random.Random`
- [ ] Nothing under `poker-server/src/main/` or `poker-engine/` is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
