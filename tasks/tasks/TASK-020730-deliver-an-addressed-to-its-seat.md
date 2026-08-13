---
schema: 2
id: TASK-020730
title: Each Addressed is encoded once and written to that seat's writer only
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, websocket, security]
depends_on: [TASK-020729]
verify:
  - ./gradlew :poker-server:test --tests '*SeatDeliveryTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:check
---

## Goal

One function turns a `List<Addressed>` and a `Room` into frames on the right writers: each
`Addressed` encoded exactly once and sent to the `ConnectionWriter` of the player in the seat it
names, and to no other.

This is the boundary the epic is most able to break quietly. It gets its own file and its own test
so that "seat 1's frame went to seat 1" is asserted directly, not inferred from a socket test.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/SeatDelivery.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/SeatDeliveryTest.kt` | create |

## Scope

- A single internal function:

  ```kotlin
  internal suspend fun deliver(frames: List<Addressed>, room: Room, connections: ConnectionDirectory)
  ```

  For each `Addressed`, in the order given: map `seat` to a player — `0` is `room.host`, `1` is
  `room.guest` — look the writer up in `connections`, encode the message once with
  `ProtocolCodec.encode`, and `send` it to that writer.
- A seat with no seated player, or a player with no live writer, is **skipped silently**. Both are
  ordinary: a `WAITING` room has no guest, and a player mid-reconnect has no writer. Neither is a
  reason to fail the caller or to send the frame to somebody else. Say that in the KDoc, because
  "skip" and "broadcast to whoever is there" look the same at a glance and only one of them is safe.
- Encode inside the loop, once per `Addressed`. Two seats' frames are *different* values — that is
  the whole point of the projection layer — so a single encode hoisted out of the loop would be a
  card leak, not an optimisation.
- Write only to `ConnectionWriter.send`. Nothing in this file may touch a `WebSocketSession`,
  `Frame`, `outgoing`, or any socket: one writer per connection, one writing coroutine, is the rule
  from `TASK-020505` and it is not negotiable here.
- No card logic, no `PlayerView`, no `holeCards`, no filtering. This function reads `Addressed.seat`
  and nothing else about the message it is carrying.

## Out of scope

- Producing the frames — the engine's projection layer already did.
- Calling `deliver` — `TASK-020731` and `TASK-020715`.
- Any queueing, retry, or backpressure policy beyond `ConnectionWriter`'s own: `send` already
  suspends when the buffer is full and returns `false` on a closed writer.

## Tests

`SeatDeliveryTest` — no Ktor and no socket. A `ConnectionWriter` is constructible on its own, and a
test coroutine draining it with `writeAll` collects exactly what was written.

| Test | Proves |
| --- | --- |
| `eachSeatGetsOnlyItsOwnFrames` | given one `Addressed` per seat, the host's writer receives exactly the seat-0 frame and the guest's writer exactly the seat-1 frame |
| `aframeIsEncodedOncePerAddressed` | two `Addressed` for one seat arrive as two frames, in the order given |
| `aseatWithNoWriterIsSkippedAndTheOtherStillReceives` | with only the host registered, the guest's frames are dropped and the host's still arrive |
| `aroomWithNoGuestDropsSeatOnesFrames` | a `WAITING` room delivers seat-0 frames and drops seat-1 frames without throwing |
| `nothingIsWrittenToASeatTheFrameDoesNotName` | across a mixed list, no frame text addressed to one seat ever appears in the other seat's received frames |

`RunnerLeakTest` is in `verify:`, not the budget: `onlyTheBroadcastFileBuildsAStateCarryingFrame` and
`noServerSourceFileTouchesHoleCards` scan every server source file, and `SeatDelivery.kt` is a new
one. Both must pass with `RunnerLeakTest.kt` unchanged — which they will, because this file
constructs no message and names no card.

## Acceptance criteria

- [ ] `SeatDeliveryTest.eachSeatGetsOnlyItsOwnFrames` passes
- [ ] `SeatDeliveryTest.aframeIsEncodedOncePerAddressed` passes
- [ ] `SeatDeliveryTest.aseatWithNoWriterIsSkippedAndTheOtherStillReceives` passes
- [ ] `SeatDeliveryTest.aroomWithNoGuestDropsSeatOnesFrames` passes
- [ ] `SeatDeliveryTest.nothingIsWrittenToASeatTheFrameDoesNotName` passes
- [ ] `RunnerLeakTest.noServerSourceFileTouchesHoleCards` and
      `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` pass with `RunnerLeakTest.kt`
      unchanged
- [ ] `SeatDelivery.kt` contains none of `PlayerView`, `holeCards`, `Frame`, `outgoing`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
