---
schema: 2
id: TASK-021410
title: A resume tells the returning seat where its opponent stands, and the seat that stayed only if it changed
type: task
status: ready
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, rooms, presence, reconnect]
depends_on: [TASK-021409]
verify:
  - ./gradlew :poker-server:test --tests '*RoomResumeTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketReconnectTest'
  - ./gradlew :poker-server:test --tests '*SocketReconnectTest'
---

## Goal

A returning player is always told the opponent's current presence, and the player who stayed is told
`PRESENT` only when the returning seat had actually been away. `Resumption.outbound` stops being
one seat's frames and becomes the frames this call produced, each addressed to the seat it names —
the contract `JoinResult.Seated` and `GraceExpiry` already have.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Resumption.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify |

Read `room/Room.kt`'s `presenceOf` and `reconnect`, and
`docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §5. Nothing else.

## Scope

- In `RoomRegistry.resume`, inside the existing `mutate` block and after `Room.reconnect`:
  - **always** add `Addressed(seat, returned.presenceOf(otherSeat, now))` for the returning seat —
    including `PRESENT`, because a returning client has no state to compare against;
  - add `Addressed(otherSeat, returned.presenceOf(seat, now))` **only when the returning seat was in
    `gracePeriods` or `absentSeats` before the reconnect** — the client that stayed already has
    state and is told only about a change.
  - `now` is the same `clock.nowMillis()` the `touch` already uses: one clock read, as today.
- **Both presence frames are appended after `resumeFrames(runner, seat)`, never before it.** This is
  forced, not chosen: `SocketReconnectTest.theReturningSocketIsPromptedAgain` asserts by index that
  a resumed `RoomJoined` is followed immediately by a `Snapshot` and then a `YourTurn`. Prepending
  fails that merged test. It is also the order `ADR-0044` §5 established for `RematchOffered`.
- `Resumption.outbound`'s KDoc: *"never another seat's"* becomes the `JoinResult.Seated` contract —
  the frames this call produced, each addressed to the seat it names.
- `deliver` already routes by seat, so **no call site changes** and `DuelSocket.kt` is not edited.
- `resumeFrames` still replays nothing: a returning player gets a fresh `Snapshot`, never the
  events they missed.

## Out of scope

- **A journal of what the server did while a player was away.** `ADR-0028` §6 declines it
  explicitly: the returning player is told the state they come back to and no more. Strictly
  addable later.
- `DuelSocket.kt`, `SeatDelivery.kt`, `Application.kt` — all unchanged.
- `disconnect` and `expireGracePeriods` — `TASK-021404` and `TASK-021407`.

## Tests

`RoomResumeTest` — an existing file, on `MutableClock` and `TEST_TIMEOUTS`.

**Two merged assertions are invalidated by this ticket's own scope and it owns them.** Both move
because `Resumption.outbound` is no longer one seat's frames, and **neither is weakened** — each
becomes a more specific claim about the same frames:

| Merged test | Assertion that moves | Why |
| --- | --- | --- |
| `aReturningPlayerIsToldItsSeatAndItsOwnState` | `assertTrue(resumption.outbound.all { it.seat == 1 })` | the guest had been away, so the host now receives `PRESENT`. It becomes: every frame **that is not an `OpponentPresence`** is addressed to seat 1, plus the two new rows below |
| `aFinishedRoomResumesAsItsOutcome` | `assertEquals(1, resumption!!.outbound.size)` and `outbound.single()` | the returning seat is always handed the opponent's presence, so the count is 2. It becomes: exactly one `DuelFinished`, addressed to the returning seat, and still no `Snapshot` |

`assertTrue(resumption.outbound.isNotEmpty())`, `any { it.message is Snapshot }` and
`none { it.message is Snapshot }` stay exactly as written. `aResumeStopsTheCountdown`,
`astrangerMayNotTakeAHeldSeat`, `aPlayerWhoNeverDroppedMayStillResume`, `aWaitingRoomHasNothingToResume`,
`anAbandonedRoomHasNothingToResume`, `anUnknownCodeAnswersNull` and
`aResumeDoesNotDisturbTheOtherSeatsCountdown` are untouched.

| Test | Proves |
| --- | --- |
| `theReturningSeatIsToldTheOpponentIsPresent` | a guest who dropped and came back receives `OpponentPresence(PRESENT, null)` addressed to seat 1 |
| `theSeatThatStayedIsToldTheOtherIsBack` | in the same run, the host receives `OpponentPresence(PRESENT, null)` addressed to seat 0 |
| `nobodyIsToldAboutASeatNobodyWasWaitingFor` | a resume by a player who never dropped produces **no** frame addressed to the other seat at all, while the returning seat still gets its presence frame — the two halves in one test |
| `theReturningSeatIsToldTheOpponentIsAway` | with the *other* seat inside its window, the returning seat's presence frame is `AWAY` with what is left of that window, not `PRESENT` — the second value, so a constant `PRESENT` cannot pass |
| `theReturningSeatIsToldTheOpponentIsAbsent` | with the other seat in `absentSeats`, it is `ABSENT` with `null` — the third |
| `thePresenceFollowsTheResumedFrames` | the index of every `OpponentPresence` in `outbound` is greater than the index of the `Snapshot`, which is what `SocketReconnectTest` pins end to end |
| `noEventsAreReplayed` | `outbound` carries no `ServerMessage.Events` — unchanged behaviour, asserted here because this ticket is the one that adds frames to the resume path |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] The two invalidated assertions read as described, and no other assertion in `RoomResumeTest`
      is edited, weakened or deleted
- [ ] `DuelSocketReconnectTest` and `SocketReconnectTest` pass **with no edit** — in particular
      `SocketReconnectTest.theReturningSocketIsPromptedAgain`, whose `resumeIndex + 1` and
      `resumeIndex + 2` assertions are what forbid prepending
- [ ] `Resumption`'s KDoc no longer says `outbound` is never another seat's
- [ ] `DuelSocket.kt` and `SeatDelivery.kt` are not in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
