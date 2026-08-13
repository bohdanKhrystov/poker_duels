---
schema: 2
id: TASK-020728
title: ClientMessage learns to open a room and to join one by code
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, rooms]
depends_on: [TASK-020727]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests '*ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:check
---

## Goal

`ClientMessage` has `CreateRoom` and `JoinRoom`, so a socket has a way to say which room it is in.
That was the missing half of `DEC-010`; `ADR-0017` answered the shape of the fix — later stories
extend the existing sealed hierarchies rather than introducing a parallel protocol.

This ticket puts both messages on the wire and leaves them refused. `TASK-020731` gives them a
registry.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `docs/protocol.md` | modify |

`DuelSocket.kt` is in the budget because `replyTo`'s `when` over `ClientMessage` is exhaustive and
used as an expression: it stops compiling the moment a variant is added. That is the property this
project keeps deliberately, and the cost of keeping it is this line.

## Scope

- Add to `ClientMessage`:

  ```kotlin
  @Serializable
  @SerialName("CreateRoom")
  public data object CreateRoom : ClientMessage

  @Serializable
  @SerialName("JoinRoom")
  public data class JoinRoom(val code: String) : ClientMessage
  ```

- `CreateRoom` carries **no format**. `DuelFormat` is server configuration under an open decision
  (`DEC-001`); a client that chose one would be asserting a rule of the game, which `ADR-0002`
  forbids. The server opens the room with `DuelFormat.DEFAULT`.
- `JoinRoom.code` is the room code's text. Declare **no** `init` validation on it: `RoomCode.parse`
  is the single place a code's shape is decided, and duplicating its rules in the wire type is how
  the two drift apart. A code that does not parse is refused by the server, not by the constructor.
- KDoc both against `ClientMessage`'s standing rule: neither carries a seat, a stack, a card or any
  other fact the server owns. `JoinRoom` names a room; it does not claim a seat in it.
- In `replyTo`, both new branches answer `ServerMessage.Failure(ProtocolError.NOT_IN_DUEL)` — the
  same answer `Act` gets — with a comment saying exactly why it is provisional: this socket reaches
  no `RoomRegistry` yet, so it can neither open a room nor find one, and `TASK-020731` replaces both
  branches with the real lookups. Do not pin this refusal with a test: it is scaffolding with two
  tickets to live, and a test written now is a test deleted later.

## Out of scope

- Handling either message — `TASK-020731`.
- `ServerMessage.RoomJoined` — added by `TASK-020727`, already merged when this starts.
- Rematch, leave, spectate or any other room message. This story needs exactly the two that let two
  sockets end up in one room; the rest are later stories' and extend the same hierarchy by the same
  precedent.

## Tests

No new test file. The protocol facts this ticket adds are covered by named, descriptor-driven and
document-driven cases that already exist; the behaviour is provisional and deliberately unpinned.

`ProtocolDocumentationTest`

| Test | Proves |
| --- | --- |
| `everyClientMessageHasARowSayingClientToServer` | both new messages have documentation rows |
| `theDocumentNamesNoMessageThatDoesNotExist` | the rows name messages that really exist |

`ProtocolPayloadTest`

| Test | Proves |
| --- | --- |
| `noClientMessageCarriesACard` | neither new message reaches a `Card` |
| `noClientMessageNamesAChipOrStateField` | neither names a stack, pot, card, board, view, bet or seat-to-act field |
| `aClientMessagesOnlyEngineTypeIsAPlayerAction` | neither drags an engine type onto the client's side of the wire |

`ProtocolDiscriminatorTest`

| Test | Proves |
| --- | --- |
| `noDiscriminatorIsAFullyQualifiedClassName` | both carry an explicit `@SerialName` |
| `everyDiscriminatorIsShortAndUnique` | `CreateRoom` and `JoinRoom` are short and collide with nothing |

`DuelSocketFrameLoopTest` must pass unchanged — in `verify:`, not in the budget.

## Acceptance criteria

- [ ] `ProtocolDocumentationTest.everyClientMessageHasARowSayingClientToServer` passes
- [ ] `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` passes
- [ ] `ProtocolPayloadTest.noClientMessageNamesAChipOrStateField` passes
- [ ] `ProtocolPayloadTest.aClientMessagesOnlyEngineTypeIsAPlayerAction` passes
- [ ] `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` passes
- [ ] `DuelSocketFrameLoopTest` passes with the file unchanged
- [ ] `replyTo` still has no `else` branch
- [ ] No test file appears in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
