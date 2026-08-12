---
schema: 2
id: TASK-021002
title: Describe a finished duel as the write path's input
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, persistence, duel]
depends_on: [TASK-021001]
verify:
  - ./gradlew :poker-server:test --tests '*FinishedDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

`FinishedDuel` is the one value the write path takes: which duel, which two profiles sat in it,
when it ran, and the engine's `DuelOutcome` — validated at construction, so an impossible duel
cannot reach the database.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/FinishedDuelTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` (for `PlayerId`),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt`,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql` (the columns this feeds).

## Scope

- Package `duels.poker.server.duel`, one file, two public declarations with KDoc:

  ```kotlin
  public data class FinishedDuel(
      val id: UUID,
      val format: String,
      val startedAt: Instant,
      val finishedAt: Instant,
      val seats: List<PlayerId>,
      val outcome: DuelOutcome,
  ) {
      init {
          require(seats.size == 2) { "a duel has exactly two seats, got ${seats.size}" }
          require(seats[0] != seats[1]) { "one player cannot occupy both seats" }
          require(format.isNotBlank()) { "a duel format label must not be blank" }
          require(!finishedAt.isBefore(startedAt)) { "a duel cannot finish before it started" }
      }
  }

  public fun formatLabel(format: DuelFormat): String =
      when (format.endCondition) {
          is EndCondition.Freezeout -> "FREEZEOUT"
          is EndCondition.FixedHands -> "FIXED_HANDS"
      }
  ```

- `seats` is indexed by seat number, so `seats[0]` is the player the engine calls seat 0. That is
  what lets `TASK-021005` pair `CoinDeltas.forSeat(seat)` with a profile without re-deriving
  anything.
- The times are `java.time.Instant` supplied by the caller — **not** read from a clock in here.
  `ServerClock` is an elapsed-time clock and its own KDoc forbids stamping a row with it, and a
  value object that reads a clock cannot be tested for the row it produces.
- `formatLabel`'s `when` over `EndCondition` has no `else`, so a third end condition stops this
  file compiling instead of silently storing the wrong label. The label is the end condition's
  name only: `DEC-001` is still open on what a duel is, and a label carrying blind levels or a hand
  count would be a format description nobody agreed to.
- KDoc on `FinishedDuel.id`: it is the duel row's primary key and the write path's idempotency key
  (`TASK-021009`). KDoc on `seats`: each `PlayerId.value` is the text form of a `player.id` UUID,
  which is what `PostgresPlayerDirectory` returns — the store parses it back with
  `UUID.fromString`.

## Out of scope

- Any SQL, `DataSource` or coroutine — this file is a value type and a `when`.
- Hands played and final stacks as stored columns: `duel` has no column for either, and whether it
  gains one is `DEC-014`. `outcome` carries both values in memory regardless.
- A `DuelResultSink` port. `STORY-0207` declares that port at its consumer, the duel runner; this
  story delivers the store it will be pointed at.

## Tests

`FinishedDuelTest`, JUnit 5, package `duels.poker.server.duel`. No coroutines, no database. Use
fixed `Instant`s such as `Instant.parse("2026-08-13T10:00:00Z")` so nothing depends on the clock.

| Test | Proves |
| --- | --- |
| `aFinishedDuelCarriesBothSeatsAndItsOutcome` | a valid construction keeps `seats[0]`, `seats[1]` and `outcome.winner` as passed |
| `rejectsAnythingOtherThanTwoSeats` | one seat and three seats each throw `IllegalArgumentException` |
| `rejectsTheSamePlayerInBothSeats` | the same `PlayerId` twice throws `IllegalArgumentException` |
| `rejectsABlankFormatLabel` | `format = "  "` throws `IllegalArgumentException` |
| `rejectsAFinishTimeBeforeTheStart` | `finishedAt` one second before `startedAt` throws `IllegalArgumentException` |
| `theDefaultFormatIsLabelledFreezeout` | `formatLabel(DuelFormat.DEFAULT) == "FREEZEOUT"` |
| `aFixedHandsFormatIsLabelledFixedHands` | `formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))) == "FIXED_HANDS"` |

## Acceptance criteria

- [ ] `FinishedDuelTest.aFinishedDuelCarriesBothSeatsAndItsOutcome` passes
- [ ] `FinishedDuelTest.rejectsAnythingOtherThanTwoSeats` passes
- [ ] `FinishedDuelTest.rejectsTheSamePlayerInBothSeats` passes
- [ ] `FinishedDuelTest.rejectsABlankFormatLabel` passes
- [ ] `FinishedDuelTest.rejectsAFinishTimeBeforeTheStart` passes
- [ ] `FinishedDuelTest.theDefaultFormatIsLabelledFreezeout` passes
- [ ] `FinishedDuelTest.aFixedHandsFormatIsLabelledFixedHands` passes
- [ ] `formatLabel`'s `when` has no `else` branch
- [ ] `FinishedDuel.kt` names no `DataSource`, `Connection`, SQL string or `ServerClock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
