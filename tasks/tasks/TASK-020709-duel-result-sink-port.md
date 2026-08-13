---
schema: 2
id: TASK-020709
title: Declare the DuelResultSink port at its consumer, so this story stays free of the database
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, duel, ports]
depends_on: [TASK-020708]
verify:
  - ./gradlew :poker-server:test --tests '*DuelResultSinkTest'
  - ./gradlew :poker-server:check
---

## Goal

`DuelResult` and `DuelResultSink` exist in the duel package, so a finished duel has one shape and
one place to go — and `STORY-0210` implements the port against Postgres without this story knowing
a database exists.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelResultSink.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelResultSinkTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLog.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/SessionRegistry.kt` (`PlayerId`).

## Scope

- Package `duels.poker.server.duel`. Two declarations, KDoc included:

  ```kotlin
  public data class DuelResult(
      val outcome: DuelOutcome,
      val seats: List<PlayerId>,
      val log: MatchLog,
  ) {
      public val winner: PlayerId? get() = outcome.winner?.let { seats[it] }
  }

  public fun interface DuelResultSink {
      public suspend fun record(result: DuelResult)
  }
  ```

- `init` requires `seats.size == 2`, `seats[0] != seats[1]`, and that `log` records the same ending:
  `log.events.filterIsInstance<MatchFinished>().singleOrNull()?.outcome == outcome`. A result whose
  log disagrees with its outcome is corrupt and must not reach a database.
- `seats` is indexed by seat, so `seats[0]` is seat 0 — the same numbering as `MatchState.buttonSeat`
  and `DuelOutcome.winner`. The KDoc says so, because an off-by-one here awards the coin to the loser.
- `record` is `suspend` because every real implementation does I/O. Nothing in this story calls it:
  the port is declared at its consumer so the dependency points inward.
- The KDoc notes that whether the whole `MatchLog` is persisted, and where, is `DEC-008` — carrying
  it in this value neither answers nor prejudges that.

## Out of scope

- Any implementation of the sink: Postgres is `STORY-0210`, and a test double belongs to whichever
  test needs one.
- Calling the sink when a duel finishes — `TASK-020714`, which is where the runner meets the room.
- Coins, ratings or profiles — `ADR-0014` and `STORY-0210`.

## Tests

`DuelResultSinkTest`, JUnit 5, package `duels.poker.server.duel`. Build a `MatchLog` with a single
`MatchFinished(0, outcome)` and no hands; `MatchLog` accepts that.

| Test | Proves |
| --- | --- |
| `theWinnerIsThePlayerInTheWinningSeat` | for `outcome.winner == 1`, `result.winner == seats[1]` |
| `adrawHasNoWinningPlayer` | for `outcome.winner == null`, `result.winner` is null |
| `aResultNeedsExactlyTwoSeats` | one seat throws `IllegalArgumentException` |
| `onePlayerCannotHoldBothSeats` | the same `PlayerId` twice throws `IllegalArgumentException` |
| `alogRecordingAnotherOutcomeIsRejected` | a log whose `MatchFinished` carries a different outcome throws `IllegalArgumentException` |
| `theSinkIsSatisfiedByALambda` | `DuelResultSink { recorded = it }` compiles and, under `runBlocking`, records the result it was given |

## Acceptance criteria

- [ ] `DuelResultSinkTest.theWinnerIsThePlayerInTheWinningSeat` passes
- [ ] `DuelResultSinkTest.adrawHasNoWinningPlayer` passes
- [ ] `DuelResultSinkTest.aResultNeedsExactlyTwoSeats` passes
- [ ] `DuelResultSinkTest.onePlayerCannotHoldBothSeats` passes
- [ ] `DuelResultSinkTest.alogRecordingAnotherOutcomeIsRejected` passes
- [ ] `DuelResultSinkTest.theSinkIsSatisfiedByALambda` passes
- [ ] `DuelResultSink.kt` imports nothing from `java.sql`, `javax.sql`, `com.zaxxer`, `org.flywaydb`
      or `duels.poker.server.db`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
