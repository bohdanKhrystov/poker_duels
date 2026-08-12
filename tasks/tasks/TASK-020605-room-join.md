---
schema: 2
id: TASK-020605
title: Seat the second player, and refuse the third
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, rooms]
depends_on: [TASK-020604]
verify:
  - ./gradlew :poker-server:test --tests '*RoomJoinTest'
  - ./gradlew :poker-server:check
---

## Goal

`Room.join` seats exactly one guest and starts the duel's `MatchState`; every other joiner gets a
refusal that says which of three things went wrong, and the room is returned untouched.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/JoinResult.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomJoinTest.kt` | create |

`RoomTest` (from `TASK-020604`) asserts `Room.open` and the constructor invariants only. This
ticket adds a method and changes no existing behaviour, so that file is not touched and every
assertion in it stands.

## Scope

- `JoinResult.kt`, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public sealed interface JoinResult {
      public data class Seated(val room: Room) : JoinResult
      public data class Refused(val reason: RoomRefusal) : JoinResult
  }

  public enum class RoomRefusal { UNKNOWN_ROOM, ROOM_FULL, ALREADY_SEATED }
  ```

  `RoomRefusal` is a **room-domain** enum. It happens to line up with the `ProtocolError` codes
  `STORY-0202` reserved, but nothing here imports the protocol and nothing here decides the
  mapping — that is `STORY-0207` and `DEC-010`.
- `Room.join(guest: PlayerId, now: Long): JoinResult`, checked strictly in this order:
  1. `state` is `FINISHED` or `ABANDONED` → `Refused(UNKNOWN_ROOM)`. A dead room is indistinguishable
     from one that never existed, deliberately: telling a stranger that a code is real but spent
     leaks that the code was guessed correctly.
  2. `seatOf(guest) != null` → `Refused(ALREADY_SEATED)`. This is how a host is refused their own
     room, and it is checked before fullness so a returning player never sees `ROOM_FULL`.
  3. `state == PLAYING` → `Refused(ROOM_FULL)`. Two seats, never three.
  4. `state == WAITING` → `Seated` with `guest` set, `state = PLAYING`,
     `match = MatchState.start(format, openingButtonSeat)`, and `lastActivityAt = now`.
- `join` is pure and total: it never throws, never mutates, and a refusal carries no room because
  the caller already holds the unchanged one.
- Exhaustive `when` over `RoomState` — no `else` branch.

## Out of scope

- Concurrency. `Room` is an immutable value; making two joiners race for one seat safely is the
  registry's job in `TASK-020610`.
- Looking a room up by code, and the `UNKNOWN_ROOM` refusal for a code that was never minted —
  `TASK-020610` returns that value; this ticket only defines it.
- Reconnecting a seated player into a live room — `STORY-0208`.

## Tests

`RoomJoinTest`, JUnit 5, package `duels.poker.server.room`. Build rooms through `Room.open` and
`join`; reach `FINISHED`/`ABANDONED` with `copy(state = ...)` plus the fields those states require.

| Test | Proves |
| --- | --- |
| `aGuestJoiningAWaitingRoomIsSeatedAndTheDuelStarts` | `Seated`, `state == PLAYING`, `guest == the joiner`, `match!!.handsPlayed == 0`, `match!!.buttonSeat == 0`, `lastActivityAt == now` |
| `theGuestSitsInSeatOne` | on the seated room, `seatOf(guest) == 1` and `players` holds both |
| `theHostCannotJoinTheirOwnRoom` | `Refused(ALREADY_SEATED)`, and the room the test still holds is `WAITING` with no guest |
| `aThirdJoinerIsRefusedRoomFull` | joining the seated room as a third player gives `Refused(ROOM_FULL)` |
| `theSeatedGuestJoiningAgainIsAlreadySeatedNotFull` | the guest re-joining gives `Refused(ALREADY_SEATED)` |
| `joiningAFinishedRoomIsUnknownRoom` | a `FINISHED` room refuses with `UNKNOWN_ROOM` — including for the host |
| `joiningAnAbandonedRoomIsUnknownRoom` | an `ABANDONED` room refuses with `UNKNOWN_ROOM` |

## Acceptance criteria

- [ ] `RoomJoinTest.aGuestJoiningAWaitingRoomIsSeatedAndTheDuelStarts` passes
- [ ] `RoomJoinTest.theGuestSitsInSeatOne` passes
- [ ] `RoomJoinTest.theHostCannotJoinTheirOwnRoom` passes
- [ ] `RoomJoinTest.aThirdJoinerIsRefusedRoomFull` passes
- [ ] `RoomJoinTest.theSeatedGuestJoiningAgainIsAlreadySeatedNotFull` passes
- [ ] `RoomJoinTest.joiningAFinishedRoomIsUnknownRoom` passes
- [ ] `RoomJoinTest.joiningAnAbandonedRoomIsUnknownRoom` passes
- [ ] `Room.join` contains no `throw` and no `else` branch over `RoomState`
- [ ] `JoinResult.kt` and `Room.kt` contain no `import duels.poker.server.protocol`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
