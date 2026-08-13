---
schema: 2
id: TASK-020804
title: The room records which seats are inside a grace window and which are absent
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, resilience]
depends_on: [TASK-020803]
verify:
  - ./gradlew :poker-server:test --tests '*RoomPresenceTest'
  - ./gradlew :poker-server:test --tests '*RoomTest'
  - ./gradlew :poker-server:test --tests '*RoomJoinTest'
  - ./gradlew :poker-server:test --tests '*RoomLifecycleTest'
---

## Goal

A `Room` says which of its seats has no live connection and how long each of them has left, so that
`ADR-0013`'s per-seat timer lives in the same value, under the same mutex, as the duel it pauses
(`ADR-0016`).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceTest.kt` | create |

## Scope

- Two fields on `Room`, appended after `duelId`, both with defaults so that every existing
  construction — `Room.open`, every `copy`, every named-argument fixture in the room tests — keeps
  compiling and keeps meaning exactly what it means today:

  ```kotlin
  val gracePeriods: Map<Int, Long> = emptyMap(),
  val absentSeats: Set<Int> = emptySet(),
  ```

  `gracePeriods` maps a seat to the instant, on `ServerClock`'s elapsed-millisecond scale, at which
  its window runs out — an absolute deadline, never a remaining duration, because a value that has
  to be decremented is a value someone has to remember to decrement. `absentSeats` names the seats
  whose window already ran out.
- Four `require`s in `init`, alongside the ones already there:
  - every seat named in either is `0` or `1`;
  - a seat named as `1` requires `guest != null` — a room can only hold a seat it has seated;
  - the two are disjoint: a seat is either counted down or already out, never both;
  - every deadline is `>= 0`, matching the existing rule on `lastActivityAt`.
- One derived property with KDoc:

  ```kotlin
  public val isPaused: Boolean get() = gracePeriods.isNotEmpty()
  ```

  Say in that KDoc what the asymmetry means, because it is the whole design: a seat still inside its
  window pauses the duel, and a seat whose window has run out does **not** — it has become an
  ordinary absent player whose hand gets folded (`TASK-020806`), and pausing on it forever is the
  "hold indefinitely" alternative `ADR-0013` rejected.

## Out of scope

- Every transition between these states: `disconnect`, `reconnect` and `expireGrace` are
  `TASK-020805`. This ticket adds the fields, their invariants and the one derived question; the
  only way to reach a non-empty value here is `copy`.
- `Room.act` behaving differently — `TASK-020807` and `TASK-020808`.
- Anything reading a clock. `Room` reads no clock of its own and this ticket does not change that:
  the deadline arrives as a parameter.

## Tests

`RoomPresenceTest` — a new file. Give it two fixtures and use the right one in each test, because
several assertions below are only falsifiable against a room that actually has a guest:

- `waitingRoom()` = `Room.open(RoomCode("2B7KMNPQ"), host, DuelFormat.DEFAULT, now = 1_000L)` —
  no guest, so seat 1 is not a seat it holds;
- `playingRoom()` = that room `join`ed by a second `PlayerId` at `now = 2_000L`, unwrapped from
  `JoinResult.Seated`, so both seats exist and both may legally be named.

| Test | Proves |
| --- | --- |
| `aFreshRoomHasNobodyGone` | `waitingRoom()` and `playingRoom()` both have empty `gracePeriods`, empty `absentSeats`, and `isPaused == false` — the default really is "everybody is here" |
| `aSeatInsideItsWindowPausesTheRoom` | `playingRoom().copy(gracePeriods = mapOf(1 to 5_000L)).isPaused` is `true` |
| `aSeatWhoseWindowRanOutDoesNotPauseTheRoom` | `playingRoom().copy(absentSeats = setOf(1)).isPaused` is `false` |
| `aSeatCannotBeBothCountingDownAndAbsent` | `playingRoom().copy(gracePeriods = mapOf(1 to 5_000L), absentSeats = setOf(1))` throws `IllegalArgumentException` |
| `anUnseatedSeatCannotBeGone` | `waitingRoom().copy(gracePeriods = mapOf(1 to 5_000L))` throws, and so does `waitingRoom().copy(absentSeats = setOf(1))` — the room has no guest, so seat 1 is nobody |
| `aSeatOutsideTheTableIsRefused` | `playingRoom().copy(absentSeats = setOf(2))` throws, and so does `playingRoom().copy(gracePeriods = mapOf(-1 to 5_000L))` |
| `aNegativeDeadlineIsRefused` | `playingRoom().copy(gracePeriods = mapOf(0 to -1L))` throws |

## Acceptance criteria

- [ ] All seven `RoomPresenceTest` cases named above pass
- [ ] `RoomTest`, `RoomJoinTest` and `RoomLifecycleTest` pass with those files **unchanged** — the
      defaults are what makes that true, and if any of them goes red a default was omitted
- [ ] `Room.kt` reads no clock: it names neither `ServerClock` nor `System`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
