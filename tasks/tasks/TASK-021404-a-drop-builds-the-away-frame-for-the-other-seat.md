---
schema: 2
id: TASK-021404
title: A drop builds AWAY and the configured window, for the other seat only
type: task
status: ready
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, presence]
depends_on: [TASK-021403]
verify:
  - ./gradlew :poker-server:test --tests '*RoomDisconnectTest'
---

## Goal

`RoomRegistry.disconnect` hands back one `OpponentPresence(AWAY, …)` addressed to the seat that
stayed, carrying what is left of the window at the instant the frame was built — and none at all
when there is no other seated player.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDisconnectTest.kt` | modify |

Read `room/Room.kt`'s `presenceOf` and `disconnect`, `room/Disconnection.kt`, and
`docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §5's table. Nothing else.

## Scope

- Inside the **same `mutate` critical section** that already computes the deadline, and nowhere
  else: after applying `Room.disconnect`, build
  `Addressed(otherSeat, disconnected.presenceOf(seat, now))` and put it in `Disconnection.outbound`.
  `now` is the one `clock.nowMillis()` read the deadline is computed from — read once, used twice,
  so the remaining is the configured window exactly.
- **A frame is produced only when there is another seated player to receive it.** A `WAITING` room
  has no guest, so `outbound` is empty there. This is `ADR-0028` §5's rule, not an optimisation.
- The `null` returns are unchanged: an unknown code and an unseated player still answer `null` and
  still change nothing.
- `Room` still reads no clock, and no new field is stored.

## Out of scope

- **Delivering the frame to a socket** — `TASK-021405` puts the `deliver` call inside
  `DuelSocket`'s existing `withContext(NonCancellable)`. Nothing in this ticket touches
  `DuelSocket.kt`, so nothing reaches a client yet.
- `expireGracePeriods` and `resume` — `TASK-021407` and `TASK-021410`.
- The mark — `TASK-021408`.

## Tests

`RoomDisconnectTest` — an existing file, over `TEST_TIMEOUTS`' `disconnectGraceMillis = 30_000`.

**One merged test is replaced, and it is this ticket's own.** `aDisconnectProducesNoFramesYet`
(added by `TASK-021401`, asserting `outbound` is empty) becomes false here by design: it is deleted
and `aDropTellsTheOtherSeatItIsAway` takes its place. **No other assertion in the file moves** — the
`.room.gracePeriods` assertions and the `null` cases are untouched, and none is weakened.

| Test | Proves |
| --- | --- |
| `aDropTellsTheOtherSeatItIsAway` | after `registry.disconnect(code, guest)`, `outbound` is exactly one frame, addressed to seat `0`, carrying `OpponentPresence(AWAY, 30_000L)` |
| `theRemainingIsTheConfiguredWindow` | a registry built with `disconnectGraceMillis = 12_345` answers `12_345L` for the same drop — the second value, so the number cannot be a constant |
| `theDropNamesTheSeatThatStayed` | `registry.disconnect(code, host)` answers one frame addressed to seat `1`, and `registry.disconnect(code, guest)` one addressed to seat `0` — both directions in one test |
| `aRoomWithNobodyElseSeatedProducesNoFrame` | on a `WAITING` room the host's drop still starts the window and `outbound` is empty |
| `anUnseatedPlayerProducesNoFrame` | `registry.disconnect(code, PlayerId("stranger"))` still answers `null`, and the room's `gracePeriods` is still empty |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `RoomDisconnectTest.aDisconnectProducesNoFramesYet` is deleted, and it is the only assertion
      in the file that moves
- [ ] The `OpponentPresence` is built inside the existing `mutate` block, from the same
      `clock.nowMillis()` value the deadline uses — one clock read in `disconnect`, as today
- [ ] `DuelSocket.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
