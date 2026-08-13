---
schema: 2
id: TASK-020811
title: The registry resumes a returning player, and nobody else
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, rooms, resilience, security]
depends_on: [TASK-020810]
verify:
  - ./gradlew :poker-server:test --tests '*RoomResumeTest'
  - ./gradlew :poker-server:test --tests '*RoomDisconnectTest'
  - ./gradlew :poker-server:test --tests '*RoomJoinTest'
  - grep -c 'holder.mutex.withLock {' poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt | grep -qx 2
---

## Goal

A player who is already seated in a room can ask for it back: the registry stops their countdown
and hands over the frames that seat is entitled to. Anyone else asking gets nothing, and the held
seat stays held.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Resumption.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | create |

## Scope

- A small result type in its own file, following `JoinResult.Seated`'s shape exactly:

  ```kotlin
  public data class Resumption(val room: Room, val seat: Int, val outbound: List<Addressed>)
  ```

- One registry method, a single `mutate` call — no new lock, and the `verify` grep pins the count
  at the two `holder.mutex.withLock {` sites the file already has:

  ```kotlin
  public suspend fun resume(code: RoomCode, player: PlayerId): Resumption?
  ```

  Inside the critical section, an exhaustive `when` over `RoomState`:
  - `PLAYING` and `FINISHED`: `room.seatOf(player)` decides the seat and answers `null` for anyone
    else; `room.runner` answers `null` if there is somehow no duel; otherwise write back
    `room.reconnect(seat).touch(clock.nowMillis())` and return
    `Resumption(returned, seat, resumeFrames(runner, seat))`.
  - `WAITING`: `null`. There is no duel to resume, and the ordinary `JoinRoom` path already answers
    a self-rejoin with `ALREADY_SEATED` — leaving that behaviour exactly where it is today is what
    keeps `DuelSocketRoomTest.thehostRejoiningItsOwnRoomIsToldItsSeat` green in `TASK-020814`.
  - `ABANDONED`: `null`. A dead room is not resumable.
- **The frames are built inside the same critical section as the write-back**, for the reason
  `TASK-020725` gives: frames decided outside the lock can describe a room state that never
  existed.
- **Identity, not luck.** The only credential is `seatOf(player)`, and `player` comes from the
  session the handshake established (`ADR-0012`). A caller who is not seated changes nothing at all
  — in particular it must not clear, shorten or restart the absent player's window, which is the
  one thing a "reconnect" from the wrong device could otherwise cost the right one.
- `touch` is deliberate: a `FINISHED` room whose player just came back is not idle, and should not
  be reaped out from under them a moment later.

## Out of scope

- The socket calling this — `TASK-020814`.
- Refusing a stranger with a particular error. `resume` answering `null` lets the caller fall
  through to the ordinary `join` path, which already refuses a full `PLAYING` room with
  `ROOM_FULL`; no new refusal vocabulary is invented here.
- Expiry — `TASK-020812`.

## Tests

`RoomResumeTest` — a new file, built like `RoomDisconnectTest`: a `MutableClock`, an explicit
`RoomTimeouts` with `disconnectGraceMillis = 30_000`, `registry.create(host)` then
`registry.join(code, guest)` so both seats are real. For the finished-duel case, end the duel the
way `RoomReapTest` does — `DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))` and
a fold through `registry.act(code) { it.act(seat, foldFrame(it), registry.handSeeds) }` — and
assert `registry.get(code)!!.state == RoomState.FINISHED` before resuming, so the finished case
cannot pass against a room that is still playing.

| Test | Proves |
| --- | --- |
| `aReturningPlayerIsToldItsSeatAndItsOwnState` | after `disconnect(code, guest)`, `resume(code, guest)` gives `seat == 1`, every frame in `outbound` addressed to seat 1, and at least one `ServerMessage.Snapshot` among them |
| `aResumeStopsTheCountdown` | afterwards `registry.get(code)!!.gracePeriods` is empty and `isPaused` is `false` |
| `astrangerMayNotTakeAHeldSeat` | with seat 1 counting down, `resume(code, PlayerId("stranger"))` is `null` **and** `registry.get(code)!!.gracePeriods == mapOf(1 to 30_000L)` — the seat is still held, on its original deadline |
| `aPlayerWhoNeverDroppedMayStillResume` | `resume(code, guest)` without any prior `disconnect` returns a `Resumption` for seat 1 — a second socket that arrives before the server noticed the first one died (`ADR-0018`) is the common case, not an error |
| `aFinishedRoomResumesAsItsOutcome` | on the finished fixture, `resume(code, host)` returns exactly one frame, a `ServerMessage.DuelFinished`, and no `Snapshot` |
| `aWaitingRoomHasNothingToResume` | `resume(code, host)` on a freshly created room is `null` |
| `anAbandonedRoomHasNothingToResume` | after `registry.abandon(code)`, `resume(code, guest)` is `null` |
| `anUnknownCodeAnswersNull` | `resume(RoomCode("ZZZZZZZZ"), host)` is `null` |

## Acceptance criteria

- [ ] All eight `RoomResumeTest` cases named above pass
- [ ] `RoomDisconnectTest` and `RoomJoinTest` pass with those files unchanged
- [ ] `RoomRegistry.kt` still contains exactly two `holder.mutex.withLock {` call sites
- [ ] `RoomRegistry.kt` names no `PlayerView` and constructs no `ServerMessage`: the frames come
      from `resumeFrames`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
