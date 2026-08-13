---
schema: 2
id: TASK-021206
title: The two clients play a whole duel over the socket to a declared winner
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, testing, end-to-end]
depends_on: [TASK-021205]
verify:
  - ./gradlew :poker-server:test --tests '*SocketDuelTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The harness answers every `YourTurn` the server sends until both clients have been told the duel is
over, and hands back the outcome — reproducibly, from two named seeds.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuelTest.kt` | modify |

Read, do not modify: `test/duel/PlayedDuel.kt` (`playDuel`'s policy, mirrored here),
`e2e/E2eServer.kt`, `protocol/ServerMessage.kt`.

## Scope

- Added to `SocketDuel.kt`:

  ```kotlin
  internal const val POLICY_SEED: Long = 0x0B07_000000000001L

  internal suspend fun SocketDuel.playToFinish(policySeed: Long = POLICY_SEED, maxActions: Int = 20_000): DuelOutcome
  ```

- **One frame at a time, from whichever socket has one.** A private helper races both clients'
  `incoming` channels with `kotlinx.coroutines.selects.select`, decodes the frame, appends it to
  that client's `received`, and returns it. No polling delay and no fixed drain window: the loop
  blocks until the server actually says something, which is what keeps a whole duel down to
  seconds.
- The loop answers a `ServerMessage.YourTurn` with
  `Act(turn.handNumber, turn.actionSequence, action)` on that client's own socket, and ends when
  both clients have received a `ServerMessage.DuelFinished`.
- **The policy is uniform over `turn.legalActions` and reads nothing else**, mirroring `playDuel`
  (`TASK-020710`) line for line: `legal.allowed.sorted()`, one draw for the type, a second draw over
  2 for `BET`/`RAISE` choosing `minBetTo`/`minRaiseTo` on 0 and `allInTo` on 1. One
  `SplitMix64Rng(policySeed)` threaded through the whole duel. `poker-ai`'s `RandomBot` is the same
  policy but cannot be called from here — its `choose` takes a `GameState`, which is exactly the
  thing a client must not hold — so this mirrors it rather than importing it, as `TASK-020710`
  already decided for the runner-level harness.
- **The draw happens when the `Act` is sent, not when the `YourTurn` arrives.** A decision point
  delivered twice — which is what a resumed client gets in `TASK-021211` — must consume exactly one
  draw, or the duel stops being reproducible.
- A `ServerMessage.Rejected` or `ServerMessage.Failure` throws `AssertionError` naming
  `handSeed`, `policySeed`, the seat and the frame. Exceeding `maxActions` throws the same way.
  These messages are `STORY-0212`'s "reports the seeds needed to reproduce any failure".
- The returned `DuelOutcome` is checked to be the same value in both clients' `DuelFinished`
  frames before it is returned.
- If the chosen `POLICY_SEED` produces a duel that runs past `maxActions`, change that constant
  until it does not, and record the number of actions the chosen seed takes in its KDoc.

## Out of scope

- Any assertion about cards, chips, coins, history or reconnection — `TASK-021207` onwards.
- Rematches. One duel per `SocketDuel`.
- Touching `test/duel/PlayedDuel.kt`. Two small mirrored policies in two harnesses are cheaper than
  one shared one that couples six merged test files to this story.

## Tests

Three tests added to `SocketDuelTest`; the two already there keep their bodies.

| Test | Proves |
| --- | --- |
| `theDuelReachesADeclaredWinner` | `playToFinish()` returns an outcome whose `winner` is `0` or `1`, each client received exactly one `DuelFinished`, and the two outcomes are equal |
| `noClientWasEverRejected` | no frame in either client's `received` is a `Rejected` or a `Failure` — a client that answers only what it was prompted for is never refused |
| `theSameSeedsPlayTheSameDuel` | the duel played twice, in two sequential `testApplication` blocks each on its own fresh schema, gives an equal `DuelOutcome` and an equal frame count per seat |

## Acceptance criteria

- [ ] `SocketDuelTest.theDuelReachesADeclaredWinner` passes
- [ ] `SocketDuelTest.noClientWasEverRejected` passes
- [ ] `SocketDuelTest.theSameSeedsPlayTheSameDuel` passes
- [ ] `SocketDuelTest.bothClientsAreSeatedInOneRoom` and `SocketDuelTest.eachDeviceGotItsOwnProfile`
      pass with their bodies unchanged: this ticket adds functions and changes neither
      `openSocketDuel` nor anything those two observe
- [ ] `SocketDuel.kt` imports nothing from `duels.poker.engine.log`, and names neither `GameState`
      nor `DuelRunner`
- [ ] `SocketDuel.kt` names no `kotlin.random.Random` and no fixed delay
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
