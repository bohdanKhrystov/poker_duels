---
schema: 2
id: TASK-020604
title: The Room value and its four states, with the seating invariants in the type
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, rooms]
depends_on: [TASK-020602]
verify:
  - ./gradlew :poker-server:test --tests '*RoomTest'
  - ./gradlew :poker-server:check
---

## Goal

A room is an immutable value with exactly two seats, whose illegal combinations — a waiting room
holding a match, a playing room with no guest, a host sitting opposite themselves — cannot be
constructed.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/room/RoomCode.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` (for `PlayerId`),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchState.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt`.

## Scope

- One file, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public enum class RoomState { WAITING, PLAYING, FINISHED, ABANDONED }

  public data class Room(
      val code: RoomCode,
      val host: PlayerId,
      val guest: PlayerId?,
      val state: RoomState,
      val format: DuelFormat,
      val match: MatchState?,
      val openingButtonSeat: Int,
      val rematchOffers: Set<PlayerId>,
      val lastActivityAt: Long,
  ) {
      init { require(...) }
      public val players: Set<PlayerId>
      public fun seatOf(player: PlayerId): Int?
      public companion object {
          public fun open(code: RoomCode, host: PlayerId, format: DuelFormat, now: Long): Room
      }
  }
  ```

- **The whole field set lands here**, even though only `open` fills it in: later tickets add
  transitions with `copy`, and a data class that grows a field later would break every test that
  built a room before it.
- `init` requires, each with its own message:
  - `guest != host` when `guest != null`;
  - `WAITING` ⟹ `guest == null` and `match == null`;
  - `PLAYING` or `FINISHED` ⟹ `guest != null` and `match != null`;
  - `rematchOffers` is empty unless `state == FINISHED`, and is always a subset of `players`;
  - `openingButtonSeat in 0..1`; `lastActivityAt >= 0`.
  `ABANDONED` constrains nothing beyond the first and fourth: a room can be abandoned from any state.
- `seatOf` returns `0` for the host, `1` for the guest, `null` for anyone else — heads-up seat
  numbering, matching `MatchState.buttonSeat`. `players` is the host plus the guest if seated.
- `open` yields `WAITING`, no guest, no match, no offers, `openingButtonSeat = 0` (the host takes
  the first duel's button) and `lastActivityAt = now`. `now` is a plain millisecond value: `Room`
  takes no clock and reads no time itself.
- No `var`, no mutable collection, no Ktor, no coroutine, no socket type in this file.

## Out of scope

- Every transition: joining is `TASK-020605`, finish/abandon/touch is `TASK-020606`, rematch is
  `TASK-020607`.
- Storing or finding rooms — `TASK-020609`.
- Running a hand inside the room — `STORY-0207`.

## Tests

`RoomTest`, JUnit 5, package `duels.poker.server.room`. Build the valid room through
`Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), DuelFormat.DEFAULT, now = 1_000L)` and reach the
invalid ones with `.copy(...)`, which re-runs `init`. **Tests must never call the primary
constructor directly** — that is what keeps them intact when later tickets add transitions.

| Test | Proves |
| --- | --- |
| `openStartsAWaitingRoomWithNoGuest` | `state == WAITING`, `guest == null`, `match == null`, `rematchOffers.isEmpty()`, `openingButtonSeat == 0`, `lastActivityAt == 1_000L` |
| `theHostSitsInSeatZeroAndAStrangerHasNoSeat` | `seatOf(host) == 0`, `seatOf(PlayerId("nobody")) == null`, `players == setOf(host)` |
| `aRoomWhoseGuestIsTheHostIsRejected` | `copy(guest = host)` throws `IllegalArgumentException` |
| `aWaitingRoomMayNotCarryAMatch` | `copy(match = MatchState.start(DuelFormat.DEFAULT))` throws |
| `aPlayingRoomWithoutAGuestIsRejected` | `copy(state = PLAYING)` throws |
| `anOfferOutsideAFinishedRoomIsRejected` | `copy(rematchOffers = setOf(host))` throws |
| `anOfferFromSomebodyWithNoSeatIsRejected` | a `FINISHED` room copied with `rematchOffers = setOf(PlayerId("nobody"))` throws |

## Acceptance criteria

- [ ] `RoomTest.openStartsAWaitingRoomWithNoGuest` passes
- [ ] `RoomTest.theHostSitsInSeatZeroAndAStrangerHasNoSeat` passes
- [ ] `RoomTest.aRoomWhoseGuestIsTheHostIsRejected` passes
- [ ] `RoomTest.aWaitingRoomMayNotCarryAMatch` passes
- [ ] `RoomTest.aPlayingRoomWithoutAGuestIsRejected` passes
- [ ] `RoomTest.anOfferOutsideAFinishedRoomIsRejected` passes
- [ ] `RoomTest.anOfferFromSomebodyWithNoSeatIsRejected` passes
- [ ] `Room.kt` declares no `var` and imports nothing from `io.ktor` or `kotlinx.coroutines`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
