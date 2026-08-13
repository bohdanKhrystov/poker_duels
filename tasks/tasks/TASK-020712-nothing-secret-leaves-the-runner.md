---
schema: 2
id: TASK-020712
title: No opponent's card and no hand seed ever leaves the runner, and transport filters nothing itself
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, duel, property, secrecy]
depends_on: [TASK-020711]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:check
---

## Goal

Across whole duels, no frame addressed to a seat carries the opponent's hole cards before a reveal,
no encoded frame carries a hand seed, and the transport layer holds no card-filtering code of its
own to get wrong.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerLeakTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/duel/PlayedDuel.kt` (`playDuel`),
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt` (`encode`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt`,
`poker-server/src/test/kotlin/duels/poker/server/duel/RunnerChipConservationTest.kt` (the shape to
follow: frames only, seed named in every failure message).

## Scope

- One test class, no production code. Package `duels.poker.server.duel`. Seeds `1L..20L`,
  `@Timeout(120)`.
- Reveal tracking is done **from the frames**, exactly as a client would: walking one seat's frames
  in order, reset the revealed set on each `HandStarted` and add `event.seat` on each
  `HandRevealed`. Nothing is read from a `GameState` or a log.
- The seed check encodes every `Addressed.message` with `ProtocolCodec.encode` and asserts no
  encoded frame contains the decimal text of any `runner.log.hands[i].seed`. This is the one place
  the test reads a log, and only to learn which numbers must be absent.
- The source scan reads `File("src/main/kotlin/duels/poker/server")` — Gradle runs tests with the
  module directory as the working directory, as `DevDatabaseComposeTest` already relies on — walks
  every `.kt` file under it, and asserts:
  - none contains the string `holeCards`;
  - the only file containing `PlayerView.of(` is `duel/Addressed.kt`;
  - the only file containing `ServerMessage.Snapshot(` or `ServerMessage.Events(` is
    `duel/Addressed.kt`.
  The failure message lists the offending paths. This is the executable form of "hole cards are
  filtered in the engine's projection layer, never ad hoc in transport".

## Out of scope

- Chip conservation — `TASK-020711`.
- Replay — `TASK-020713`.
- Widening the scan to `poker-engine`: the engine is where card filtering is *supposed* to live, and
  `TASK-020410` and `TASK-020411` already hold that line.

## Tests

`RunnerLeakTest`, JUnit 5, package `duels.poker.server.duel`.

| Test | Proves |
| --- | --- |
| `noSnapshotShowsTheOpponentsCardsBeforeAReveal` | in every `Snapshot` addressed to seat `s`, `view.seats[1 - s].holeCards` is empty unless a `HandRevealed` naming `1 - s` reached `s` earlier in the same hand |
| `aSeatAlwaysSeesItsOwnCards` | in every `Snapshot` addressed to `s` after the deal, `view.seats[s].holeCards` has two cards |
| `noEventFrameCarriesTheOpponentsHoleCardsDealt` | no `Events` frame addressed to `s` contains a `HoleCardsDealt` naming `1 - s` |
| `noEncodedFrameContainsAHandSeed` | no encoded frame contains the decimal text of any hand's seed |
| `noServerSourceFileTouchesHoleCards` | no `.kt` file under `src/main/kotlin/duels/poker/server` contains `holeCards` |
| `onlyTheBroadcastFileBuildsAStateCarryingFrame` | `PlayerView.of(`, `ServerMessage.Snapshot(` and `ServerMessage.Events(` appear in `duel/Addressed.kt` and nowhere else under `src/main/kotlin/duels/poker/server` |

## Acceptance criteria

- [ ] `RunnerLeakTest.noSnapshotShowsTheOpponentsCardsBeforeAReveal` passes
- [ ] `RunnerLeakTest.aSeatAlwaysSeesItsOwnCards` passes
- [ ] `RunnerLeakTest.noEventFrameCarriesTheOpponentsHoleCardsDealt` passes
- [ ] `RunnerLeakTest.noEncodedFrameContainsAHandSeed` passes
- [ ] `RunnerLeakTest.noServerSourceFileTouchesHoleCards` passes
- [ ] `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` passes
- [ ] Every failure message names the duel seed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
