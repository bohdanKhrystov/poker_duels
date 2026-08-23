---
schema: 2
id: TASK-021407
title: Expiry says ABSENT before the fold it explains, and an abandoned room says nothing
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, presence]
depends_on: [TASK-021406]
verify:
  - ./gradlew :poker-server:test --tests '*GraceExpiryTest'
---

## Goal

When a grace window runs out, the seat that stayed is told `ABSENT` **before** any frame the
resulting fold or check produced — and when both seats ran out, the room is abandoned and nothing
is sent at all.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceExpiryTest.kt` | modify |

Read `room/Room.kt`'s `presenceOf` and `expireGrace`, `room/GraceExpiry.kt`, and
`docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §5. Nothing else.

## Scope

- In `expireGracePeriods`, for each room whose sweep moved a seat into `absentSeats`: **prepend**
  `Addressed(otherSeat, room.presenceOf(expiredSeat, now))` to the `GraceExpiry.outbound` the fold
  produced. `now` is the single `clock.nowMillis()` this pass already reads, so every room in one
  pass is judged and described against the same instant.
- The presence frame comes **first**, before the fold's frames, so a client can label the event as
  it renders it. Ordering is a courtesy, not the mechanism — `(handNumber, actionSequence)` is what
  identifies a decision point — but it is what this ticket asserts.
- **Nothing is emitted when there is no other seated player to receive it.** A room both of whose
  seats expired is abandoned, and `GraceExpiry.outbound` stays empty. So does a `WAITING` room's.
- No new plumbing: `Application.kt`'s ticker already delivers `GraceExpiry.outbound`, and is not
  edited.
- No new room state, no second timer. `expireGracePeriods` still rides `ADR-0025`'s ticker.

## Out of scope

- **The mark on the fold itself** — `TASK-021408` puts `ActedForAbsentSeat` inside `foldAbsent`.
  This ticket asserts the presence frame precedes *the frames the fold produced*, which today are
  `Events` and `Snapshot`; it does not assert anything about a mark that does not exist yet.
- `Application.kt` — unchanged.
- `disconnect` and `resume` — `TASK-021404` and `TASK-021410`.

## Tests

`GraceExpiryTest` — an existing file, over `TEST_TIMEOUTS`' `disconnectGraceMillis = 30_000` and
`fixedSeeds`, on a `MutableClock`. It derives its seat from `seatedPlayer(onTurn, host, guest)`
rather than writing one, and new tests follow that.

**`bothSeatsGoneEndsTheRoom` already asserts `expiries.single().outbound.isEmpty()` and stays
exactly as written** — it is the acceptance criterion for "both seats expiring abandons the room and
sends nothing", and this ticket must leave it true. No other assertion in the file moves.

| Test | Proves |
| --- | --- |
| `theSeatThatStayedIsToldTheOtherIsAbsent` | after the window runs out, `expiries.single().outbound` opens with `Addressed(presentSeat, OpponentPresence(ABSENT, null))` |
| `theAbsentMarkPrecedesTheFramesTheFoldProduced` | in the same run, the index of that `OpponentPresence` is lower than the index of every other frame in `outbound` — asserted by index, not by presence, because "both arrived" is true of the order this test exists to prevent |
| `noFrameGoesToTheSeatThatExpired` | no frame in `outbound` is addressed to the expired seat carrying an `OpponentPresence` — the frame is recipient-relative and only the seat that stayed has an opponent |
| `aWindowStillRunningProducesNothing` | with the clock short of the deadline, `expireGracePeriods()` returns an empty list, as `nothingExpiresWhileTheWindowIsStillRunning` already proves for the room — this adds the frame half |
| `bothSeatsGoneSendNoPresence` | on the both-gone run, `outbound` contains no `OpponentPresence` — the falsifiable half of `bothSeatsGoneEndsTheRoom`, which only asserts emptiness |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `GraceExpiryTest.bothSeatsGoneEndsTheRoom` passes with `assertTrue(expiries.single().outbound.isEmpty())`
      unchanged, and no other merged assertion in the file is edited
- [ ] The presence frame is built from `Room.presenceOf` and the pass's single `clock.nowMillis()`
      read — `expireGracePeriods` reads the clock once, as today
- [ ] `Application.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
