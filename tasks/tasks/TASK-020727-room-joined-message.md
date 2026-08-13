---
schema: 2
id: TASK-020727
title: ServerMessage.RoomJoined names the room and the seat the server gave you
type: task
status: ready
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, rooms]
depends_on: [TASK-020726]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests '*ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:check
---

## Goal

`ServerMessage` has a `RoomJoined` variant carrying the room's code and the seat the server put the
recipient in — the answer to `CreateRoom` and to a successful `JoinRoom`.

The seat is on this message and on no client message, ever: `ADR-0002` and `ClientMessage`'s own
KDoc both say a client never asserts which seat is acting, so the seat is something the server
**tells** a client, once, and then remembers on its own.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `docs/protocol.md` | modify |

`DuelSocket.kt` is in the budget for one line, for the same reason as in `TASK-020720`: `serve()`'s
`when` over `ServerMessage` lists every variant a handshake may not return, and stops compiling when
a variant is added.

## Scope

- Add to `ServerMessage`:

  ```kotlin
  @Serializable
  @SerialName("RoomJoined")
  public data class RoomJoined(val code: String, val seat: Int) : ServerMessage {
      init { require(seat in 0..1) { "seat must be 0 or 1, was $seat" } }
  }
  ```

- `code` is a `String`, not a `RoomCode`: `RoomCode` is a server-side value type with its own
  parsing rules and is not `@Serializable`. The wire carries its text, and the server parses it back
  with `RoomCode.parse` — which is where a malformed code is caught, exactly as it is for an inbound
  frame.
- KDoc it: this is the only place a client learns its seat, and a client that later sends an `Act`
  is not believed about the seat — the server derives it from the session again.
- Add `is ServerMessage.RoomJoined,` to `DuelSocket.serve()`'s `error(...)` branch list. Nothing
  else in `DuelSocket.kt` changes.
- Add one row to `docs/protocol.md`'s message table:
  `` | `RoomJoined` | server → client | `code`, `seat` | The server seated you in a room ``.

## Out of scope

- Sending one — `TASK-020731`.
- The client messages that provoke it — `TASK-020728`.
- Any room *state* on the wire: no player ids, no stacks, no format. A `RoomJoined` says where you
  are sitting and nothing about the game, which the `Snapshot` that follows already says.

## Tests

No new test file, for the same reason as `TASK-020720`: every check is descriptor-driven or
document-driven and already exists.

`ProtocolDocumentationTest`

| Test | Proves |
| --- | --- |
| `everyServerMessageHasARowSayingServerToClient` | the new variant has its documentation row |
| `theDocumentNamesNoMessageThatDoesNotExist` | the row names a message that really exists |

`ProtocolPayloadTest`

| Test | Proves |
| --- | --- |
| `theOnlyStateAServerMessageCarriesIsAPlayerView` | `Snapshot` is still the only state carrier |
| `noServerMessageNamesADeckAnRngOrASeed` | `code` and `seat` are neither |

`ProtocolDiscriminatorTest`

| Test | Proves |
| --- | --- |
| `noDiscriminatorIsAFullyQualifiedClassName` | `@SerialName("RoomJoined")` is explicit |
| `everyDiscriminatorIsShortAndUnique` | `RoomJoined` is 10 characters and collides with nothing |

## Acceptance criteria

- [ ] `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient` passes
- [ ] `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` passes
- [ ] `ProtocolPayloadTest.theOnlyStateAServerMessageCarriesIsAPlayerView` passes
- [ ] `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` passes
- [ ] No test file appears in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
