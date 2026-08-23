---
schema: 2
id: TASK-021307
title: A standing offer is restated to a returning socket, after its DuelFinished
type: task
status: done
parent: STORY-0213
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, rooms]
files_touched: 2
depends_on: [TASK-021306]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketReconnectTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRoomTest'
  - ./gradlew :poker-server:check
---

## Goal

`ADR-0044` §5: an offer made while the opponent was inside their disconnect grace window is not
lost. When `RoomRegistry.resume` answers, the returning socket is sent one `RematchOffered` per
player in `Room.rematchOffers` — **after** its `RoomJoined` and **after** the resumed frames.

Without this, the one fact the room is still holding is delivered to nobody and never restated.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify — `replyToJoinRoom`'s resume branch only |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketReconnectTest.kt` | modify — one test added |

Read, not edited: `poker-server/src/main/kotlin/duels/poker/server/room/Resumption.kt` — the
`Resumption` the resume branch already holds carries the `Room`, so nothing new is fetched.

## Scope

- In `replyToJoinRoom`, in the `resumed != null` branch **only**, after
  `deliver(resumed.outbound, resumed.room, deps.connections)`: for each player in
  `resumed.room.rematchOffers`, send this socket one `ServerMessage.RematchOffered` naming the seat
  `resumed.room.seatOf` gives for that player, skipping a player it cannot map.
- `send`, not `deliver`: the frame goes to the socket that just came back, and to nobody else. The
  opponent was told when the offer was recorded and is told nothing again by somebody else's
  reconnect.
- No state check is needed and none is added. `Room` requires `rematchOffers` to be empty unless the
  room is `FINISHED`, so a `PLAYING` resume sends nothing by construction rather than by a branch.
- KDoc the addition with the reason the order is load-bearing, from `ADR-0044` §5: `DuelFinished` is
  where a client enters its result screen, so an offer stated before it would be discarded by a
  reducer that treats `DuelFinished` as that screen's beginning.

## Out of scope

- `JoinResult.Seated` and `RoomRefusal.ALREADY_SEATED` — neither is a resume, and neither can carry a
  standing offer: a room with offers recorded is `FINISHED`, which `resume` answers first.
- Anything a player sees. `STORY-0309` renders this.
- `RoomRegistry.resume` itself, `Resumption`, and `resumeFrames`: untouched.

## Tests

`DuelSocketReconnectTest`

| Test | Proves |
| --- | --- |
| `aStandingOfferIsRestatedAfterTheReturningSocketsDuelFinished` | host and guest finish a one-hand duel; the guest's socket drops and the room pauses; the host offers a rematch into an absent seat; the guest rejoins with `JoinRoom` — and its frames are `RoomJoined` first, then exactly one `DuelFinished`, then exactly one `RematchOffered(seat = 0)` at a **later index** than the `DuelFinished`, with no `Snapshot` anywhere among them |

Assert the two indices, not just the two presences: "both arrived" is true of the order this ticket
exists to prevent. Assert also that the **host's** socket receives no second `RematchOffered` when
the guest returns.

`aReconnectAfterTheDuelFinishedGetsTheFinishedState` — the neighbouring test — passes **unchanged**,
and that is not a coincidence to be checked by hope: no offer stands in it, so the loop added here
runs zero times and its `count DuelFinished == 1` and `none { it is Snapshot }` assertions are
untouched. If it needs editing, stop: that means the restatement fired for a room holding no offer.

## Acceptance criteria

- [ ] `DuelSocketReconnectTest.aStandingOfferIsRestatedAfterTheReturningSocketsDuelFinished` passes
- [ ] The test asserts `indexOf(RematchOffered) > indexOf(DuelFinished)` in the returning socket's
      frame list
- [ ] `DuelSocketReconnectTest.aReconnectAfterTheDuelFinishedGetsTheFinishedState` passes with every
      assertion it already had, and no line of it changes
- [ ] `DuelSocketRematchTest` and `DuelSocketRoomTest` pass with both files unchanged
- [ ] `./gradlew :poker-server:check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
