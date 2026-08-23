---
schema: 2
id: TASK-021305
title: Three ways to hold no seat answer one indistinguishable UNKNOWN_ROOM
type: task
status: backlog
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021304]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0044` §6's first bullet, made executable: an `OfferRematch` from a socket that entered no room,
from a player who holds no seat in the room, and for a room that has been reaped all answer
`Failure(UNKNOWN_ROOM)` — and are **indistinguishable from each other**, so an offer is never an
oracle for which rooms are alive.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | modify — one test added, fixture and existing tests unchanged |

Read, not edited: `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRoomTest.kt` — its
private `RoomRegistry.removeRoom(code)` reflection helper is how a test reaps a room, and this file
needs its own private copy.

## Why one test and not three

Indistinguishability is a property of the three answers **together**. Three separate tests can each
assert `Failure(UNKNOWN_ROOM)` and still leave the interesting claim — that nothing tells them
apart — asserted nowhere. So one test builds all three sockets in one `testApplication`, sends one
`OfferRematch` on each, and compares the three answers to each other as well as to the expected
frame.

Two of the three arrive at the refusal through the same branch, and that is the point rather than a
weakness: `RoomMembership.code` is set only on a path that seated the player, so a stranger who knows
the code and a socket that never entered a room are the *same* state by construction. Say so in the
test's KDoc. `RematchRefusal.NOT_A_PLAYER` is for that reason unreachable through the socket at all;
its branch in `replyToOfferRematch` exists because `RoomRegistry` can return it and because the
`when` must be exhaustive, and this ticket does not try to reach it.

## Scope

The three sockets, all inside one `testApplication`:

1. **Entered no room** — handshake only, then `OfferRematch`.
2. **Holds no seat** — a third device sends `JoinRoom` for a full room, is refused `ROOM_FULL`
   (which sets no membership), then sends `OfferRematch`.
3. **Room reaped** — a seated socket from `TASK-021302`'s `finishedDuel` fixture, after
   `rooms.removeRoom(code)` has taken the room out of the registry, then `OfferRematch`.

Each socket is drained after its offer. Assert, for each: exactly **one** frame arrived, and it
equals `ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)`. Then assert the three frames are equal
to one another — the frame count is part of the claim, because an extra frame on one of them is a
way to tell them apart.

## Out of scope

- `REMATCH_UNAVAILABLE` — `TASK-021306`.
- Whether the reaper would have removed the room on its own. `removeRoom` stands in for a reap
  deliberately: this test is about the answer, not about `RoomTimeouts`.
- Any change to `DuelSocketRoomTest.kt`. Copy the helper; do not extract it.

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `theThreeWaysToHoldNoSeatAnswerOneIndistinguishableUnknownRoom` | all three sockets receive exactly one frame, every one of them is `Failure(UNKNOWN_ROOM)`, and all three are equal |

## Acceptance criteria

- [ ] `DuelSocketRematchTest.theThreeWaysToHoldNoSeatAnswerOneIndistinguishableUnknownRoom` passes
- [ ] The test asserts a frame **count** of exactly one on each of the three sockets
- [ ] The test asserts the three answers equal to each other, not only each to a literal
- [ ] Every test from `TASK-021302`–`TASK-021304` passes with every assertion it already had
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
