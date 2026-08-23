---
schema: 2
id: TASK-021401
title: RoomRegistry.disconnect answers with a room and the frames it produced
type: task
status: ready
parent: STORY-0214
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, rooms, presence]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*RoomDisconnectTest'
  - ./gradlew :poker-server:compileTestKotlin
---

## Goal

`RoomRegistry.disconnect` returns a `Disconnection` — a room plus the frames the call produced —
instead of a bare `Room?`, so the emission point `ADR-0028` §5 puts there has somewhere to put a
frame. It emits nothing yet: `outbound` is always empty.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Disconnection.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDisconnectTest.kt` | modify |

Read `room/Resumption.kt` and `room/GraceExpiry.kt` — the two types this one is shaped after — and
`duel/Addressed.kt` for the frame type. Nothing else.

## Scope

- `Disconnection(room: Room, outbound: List<Addressed>)`, a `public data class` in
  `duels.poker.server.room`, KDoc'd like `Resumption` and `GraceExpiry`: the room after the
  transition, plus the frames this call produced, each addressed to the seat it names.
- `RoomRegistry.disconnect` returns `Disconnection?`. The `null` cases are unchanged: a code with
  no live room, and a player this room has not seated. The deadline is still computed in
  `disconnect` and nowhere else, inside the same `mutate` block, and the room written back is still
  the one `Room.disconnect` returned.
- `outbound` is `emptyList()` at every return. **This ticket adds no frame**, so nothing observable
  changes for any caller.
- `DuelSocket.kt` is **not** edited: its one call site — the `finally` block, inside
  `withContext(NonCancellable)` — discards the result today and still compiles.

## Out of scope

- **Emitting `OpponentPresence(AWAY, …)`** and delivering it: `TASK-021404`.
- Any change to `Resumption`, `GraceExpiry`, `Room`, `deliver`, or `DuelSocket`.
- `Room.disconnect`, the pure one — untouched.

## Tests

`RoomDisconnectTest` — an existing file. Five of its tests read `gracePeriods` straight off the
returned value and become `.room.gracePeriods`. **That is the whole of the change to them**: no
assertion is weakened, deleted or re-expected, and no expected value moves.

| Test | Change |
| --- | --- |
| `aDisconnectStartsTheWindowAtNowPlusTheConfiguredLimit` | `disconnected!!.gracePeriods` → `disconnected!!.room.gracePeriods` |
| `theWindowRunsFromWhenTheDropHappened` | the same, one assertion |
| `aSecondDropRestartsTheWindow` | the same, on `first` and `second` |
| `theHostAndTheGuestCountDownSeparately` | the same, on `afterHost` and `afterGuest` — three assertions |
| `theStoredRoomIsThePausedOne`, `somebodyWhoIsNotSeatedChangesNothing`, `anUnknownCodeAnswersNull` | unchanged — they read the registry or assert `null` |

One test is added:

| Test | Proves |
| --- | --- |
| `aDisconnectProducesNoFramesYet` | after `registry.disconnect(room.code, guest)` the result's `outbound` is empty — the falsifiable half of "this ticket emits nothing", and the assertion `TASK-021404` replaces |

## Acceptance criteria

- [ ] `RoomDisconnectTest.aDisconnectProducesNoFramesYet` passes
- [ ] Every other test in `RoomDisconnectTest` passes, and the only edit made to any of them is
      `X!!.gracePeriods` → `X!!.room.gracePeriods`
- [ ] `./gradlew :poker-server:compileTestKotlin` exits 0 with no file outside the *Files* table changed
- [ ] Every command in `verify:` exits 0

## Notes

Sized by the `ADR-0070` §2 probe rather than by reading: a throwaway `Disconnection` and the
signature change were applied on `develop`, the commands `.github/workflows/build.yml` runs were run
in full, the paths they named were propagated, and the whole gate set exited 0 at exactly these
three files. `DuelSocket.kt` never appeared, because it discards `disconnect`'s result. Three files
needs no `atomic:`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
