---
schema: 2
id: TASK-020805
title: Disconnect starts the window, reconnect clears it, expiry makes the seat absent
type: task
status: done
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, resilience]
depends_on: [TASK-020804]
verify:
  - ./gradlew :poker-server:test --tests '*RoomPresenceTest'
  - ./gradlew :poker-server:test --tests '*RoomTest'
  - ./gradlew :poker-server:test --tests '*RoomLifecycleTest'
---

## Goal

The three transitions of `ADR-0013`'s per-seat timer exist as pure `Room` methods: a seat drops and
starts counting down, a seat returns and stops counting down, and a window runs out and the seat
becomes absent.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceTest.kt` | modify |

## Scope

Three methods, all pure and total in the style of `join`, `finish` and `abandon` — no clock, no
exception on an ordinary condition:

- `public fun disconnect(seat: Int, deadline: Long): Room` — puts `seat to deadline` into
  `gracePeriods` and removes `seat` from `absentSeats`. **The timer restarts on every disconnect**
  (`STORY-0208`'s design notes): a second call for the same seat overwrites the first deadline, it
  does not keep the earlier one. Throws `IllegalArgumentException` for a seat this room has not
  seated — that is a server bug, not a network event, and `init` would throw anyway; throwing here
  names it better.
- `public fun reconnect(seat: Int): Room` — removes `seat` from both `gracePeriods` and
  `absentSeats`, and returns this room unchanged when the seat was in neither. A reconnect from a
  player nobody was waiting for is an ordinary thing, not an error.
- `public fun expireGrace(now: Long): Room` — moves every seat whose deadline is `<= now` out of
  `gracePeriods` and into `absentSeats`, leaving the rest counting down. Returns this room
  unchanged (`this`, so a caller can compare by identity or equality) when nothing has run out.
  Comparison is `<=`, not `<`, so a window of exactly zero remaining is over — state that in the
  KDoc, since `TASK-020815` asserts the boundary instant and the two must agree.

Each gets KDoc naming `ADR-0013` and stating that the deadline is an instant on the caller's clock.

## Out of scope

- Reading the clock or the configured window — the caller passes `now` and `deadline`.
  `RoomRegistry` computes them from its own `ServerClock` and `RoomTimeouts` in `TASK-020809` and
  `TASK-020812`.
- Folding anything. `expireGrace` only re-labels a seat; the fold is `TASK-020806` and
  `TASK-020812`.
- Abandoning a room whose seats are both gone — `TASK-020812`.

## Tests

`RoomPresenceTest` — modified by appending the tests below to the seven from `TASK-020804`. Those
seven do not change and no assertion in them is weakened. Reuse the same `playingRoom()` fixture,
which has both seats filled, so every assertion about seat 1 below is falsifiable.

| Test | Proves |
| --- | --- |
| `aDisconnectStartsTheWindowAndPausesTheRoom` | `playingRoom().disconnect(1, 9_000L)` has `gracePeriods == mapOf(1 to 9_000L)` and `isPaused` |
| `aSecondDisconnectRestartsTheWindow` | `disconnect(1, 9_000L).disconnect(1, 20_000L)` has `gracePeriods == mapOf(1 to 20_000L)` — the later deadline replaces the earlier one |
| `aDisconnectAfterExpiryPutsTheSeatBackInAWindow` | from `copy(absentSeats = setOf(1))`, `disconnect(1, 9_000L)` leaves `absentSeats` empty and the seat counting down again |
| `disconnectingASeatTheRoomDoesNotHoldThrows` | `waitingRoom().disconnect(1, 9_000L)` throws `IllegalArgumentException`, and so does `playingRoom().disconnect(2, 9_000L)` |
| `aReconnectClearsBothTheWindowAndTheAbsence` | from `disconnect(1, 9_000L)` and separately from `copy(absentSeats = setOf(1))`, `reconnect(1)` leaves both collections empty and `isPaused == false` |
| `reconnectingASeatNobodyWasWaitingForChangesNothing` | `playingRoom().reconnect(1) == playingRoom()` |
| `expireGraceMovesOnlyTheWindowsThatRanOut` | from `disconnect(0, 5_000L).disconnect(1, 20_000L)`, `expireGrace(5_000L)` gives `absentSeats == setOf(0)` and `gracePeriods == mapOf(1 to 20_000L)` — the two-seat fixture is the point: a single-seat one would pass whether or not the survivor is kept |
| `expireGraceLeavesARoomWithTimeLeftAlone` | `disconnect(1, 20_000L).expireGrace(19_999L)` returns an equal room, still `isPaused` |
| `expireGraceIsIdempotent` | applying it twice at the same `now` gives an equal room the second time |

## Acceptance criteria

- [ ] All nine `RoomPresenceTest` cases named above pass
- [ ] The seven cases from `TASK-020804` still pass, unmodified
- [ ] `RoomTest` and `RoomLifecycleTest` pass with those files unchanged
- [ ] `Room.kt` still names neither `ServerClock` nor `System`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
