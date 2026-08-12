---
schema: 2
id: TASK-020509
title: A bad frame mid-session earns a Failure and never closes the socket
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, websocket, protocol, robustness]
depends_on: [TASK-020508]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketSessionTest'
  - ./gradlew :poker-server:check
---

## Goal

After the handshake, every frame gets an answer and no frame gets the connection killed: a
`Decoded.Refused` becomes a `Failure` on that one socket and the loop carries on.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketFrameLoopTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/test/kotlin/duels/poker/server/DuelSocketSessionTest.kt` (the client shape and
`awaitSize` helper to copy — that file is not edited here).

## Scope

- The placeholder `incoming.consumeEach { }` from `TASK-020507` gains its body and nothing else in
  the file moves:

  ```kotlin
  incoming.consumeEach { frame -> writer.replyTo(frame) }
  ```

  with one private extension:

  ```kotlin
  private suspend fun ConnectionWriter.replyTo(frame: Frame) {
      val text = (frame as? Frame.Text)?.readText()
          ?: return send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)))
      val failure = when (val decoded = ProtocolCodec.decodeClient(text)) {
          is Decoded.Refused -> ServerMessage.Failure(decoded.error)
          is Decoded.Message -> when (decoded.message) {
              is Hello -> ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)
              is Act -> ServerMessage.Failure(ProtocolError.NOT_IN_DUEL)
          }
      }
      send(ProtocolCodec.encode(failure))
  }
  ```

- The `when` over `ClientMessage` is **exhaustive with no `else`**, so the day `STORY-0207` adds a
  message type, this function stops compiling instead of silently ignoring it.
- A second `Hello` is `MALFORMED_MESSAGE`: the handshake happened once and is not repeatable.
- An `Act` is `NOT_IN_DUEL` — the code `TASK-020202` reserved for exactly this. `STORY-0207`
  replaces that branch when a duel exists to route the action to.
- `decodeClient` never throws, so this loop has **no `try`/`catch` of its own**. If a frame ever
  did throw here it would be a genuine defect and must not be swallowed.
- Every reply goes through `writer`; `outgoing` still appears exactly once in the file.

## Out of scope

- Acting on an `Act` — `STORY-0207`. This story's socket is in no duel, and saying so is the whole
  answer.
- Frames that are too long or too deeply nested. `TASK-020213` refuses those inside
  `decodeClient`; `TASK-020510` asserts end-to-end that they arrive here as a `Failure`.
- Rate limiting a client that floods the socket with junk. Not ticketed; note it and move on.
- Editing `DuelSocketHandshakeTest` or `DuelSocketSessionTest`. Neither asserts anything about a
  post-handshake frame, which is why they are untouched.

## Tests

`DuelSocketFrameLoopTest`, JUnit 5, package `duels.poker.server`. Every test completes the
handshake first with `Hello(deviceId = "d1")`, reads the `Welcome`, and then exercises the loop.
Replies are decoded to `ServerMessage` with `protocolJson`. `withTimeout(5.seconds)` around each
exchange.

| Test | Proves |
| --- | --- |
| `aMalformedFrameIsAnsweredAndTheSocketStaysOpen` | `"{"` yields `Failure(MALFORMED_MESSAGE)`, a second `"{"` yields another, and `sessions.size` is still `1` |
| `anUnknownTypeIsAnsweredWithUnknownMessage` | `{"type":"Nope"}` yields `Failure(UNKNOWN_MESSAGE)` |
| `anActOutsideADuelIsAnsweredWithNotInDuel` | an encoded `Act(1, 0, PlayerAction.Fold(0))` yields `Failure(NOT_IN_DUEL)` |
| `aSecondHelloIsRefusedWithoutClosing` | an encoded `Hello("d1")` after the handshake yields `Failure(MALFORMED_MESSAGE)` and the socket is still open |
| `aBinaryFrameIsRefusedWithoutClosing` | `Frame.Binary(true, byteArrayOf(1, 2, 3))` yields `Failure(MALFORMED_MESSAGE)` and the socket is still open |
| `fiftyBadFramesProduceFiftyWholeFailures` | 50 `"{"` frames yield exactly 50 received frames, each decoding to a `Failure` on its own — whole frames, none torn or merged |
| `oneClientsBadFrameDoesNotDisturbAnother` | with two welcomed connections, junk on the first yields a `Failure` on the first while `withTimeoutOrNull(200) { second.incoming.receive() }` is `null` and `sessions.size` is still `2` |

## Acceptance criteria

- [ ] `DuelSocketFrameLoopTest.aMalformedFrameIsAnsweredAndTheSocketStaysOpen` passes
- [ ] `DuelSocketFrameLoopTest.anUnknownTypeIsAnsweredWithUnknownMessage` passes
- [ ] `DuelSocketFrameLoopTest.anActOutsideADuelIsAnsweredWithNotInDuel` passes
- [ ] `DuelSocketFrameLoopTest.aSecondHelloIsRefusedWithoutClosing` passes
- [ ] `DuelSocketFrameLoopTest.aBinaryFrameIsRefusedWithoutClosing` passes
- [ ] `DuelSocketFrameLoopTest.fiftyBadFramesProduceFiftyWholeFailures` passes
- [ ] `DuelSocketFrameLoopTest.oneClientsBadFrameDoesNotDisturbAnother` passes
- [ ] `DuelSocketHandshakeTest` and `DuelSocketSessionTest` pass with no edit to either file
- [ ] `DuelSocket.kt` contains no `catch` and no `else ->` branch over `ClientMessage`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
