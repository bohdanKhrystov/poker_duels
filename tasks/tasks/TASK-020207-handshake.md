---
schema: 2
id: TASK-020207
title: The handshake refuses a mismatched protocol version
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, protocol, versioning]
depends_on: [TASK-020204, TASK-020205]
verify:
  - ./gradlew :poker-server:test --tests '*HandshakeTest'
  - ./gradlew :poker-server:check
---

## Goal

One pure function decides a handshake: a matching `PROTOCOL_VERSION` gets a `Welcome`, anything else
gets `Failure(VERSION_MISMATCH)` — never a silent misread, the rule the log codecs already follow.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Handshake.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/HandshakeTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt`.

## Scope

- One top-level function in package `duels.poker.server.protocol`:

  ```kotlin
  public fun handshake(hello: ClientMessage.Hello, deviceId: String): ServerMessage =
      if (hello.protocolVersion == PROTOCOL_VERSION) {
          ServerMessage.Welcome(deviceId)
      } else {
          ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)
      }
  ```

- `deviceId` is passed in, not minted here. Minting needs a secure random source and that is
  `STORY-0205`'s; this function stays pure, with no clock, no randomness and no I/O, so it is
  testable without a server.
- KDoc records what the caller must do with a `Failure`: send it, then close the socket. This
  function does not close anything — it has no socket.
- Comparison is `==` on the whole `Int`. There is no "compatible range" and no negotiation: one
  version, or refusal.

## Out of scope

- Sending the message or closing the connection — `STORY-0205`.
- Resolving a device id to a profile, or issuing one — `STORY-0205` and `STORY-0210`.
- Any handling of a first frame that is not a `Hello`: that is a socket-lifecycle rule and belongs
  to `STORY-0205`, which has it as an acceptance criterion already.

## Tests

`HandshakeTest`, JUnit 5, package `duels.poker.server.protocol`.

| Test | Proves |
| --- | --- |
| `aMatchingVersionIsWelcomed` | `handshake(Hello("d1", PROTOCOL_VERSION), "d1")` equals `ServerMessage.Welcome("d1")` |
| `aDefaultedHelloIsWelcomed` | `handshake(Hello(), "d1")` equals `ServerMessage.Welcome("d1")` |
| `anOlderVersionIsRefused` | `handshake(Hello("d1", PROTOCOL_VERSION - 1), "d1")` equals `ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)` |
| `aNewerVersionIsRefused` | `handshake(Hello("d1", PROTOCOL_VERSION + 1), "d1")` equals `ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)` |
| `aRefusalCarriesNoDeviceId` | the result of a mismatched handshake is not a `Welcome`, asserted with `assertInstanceOf(ServerMessage.Failure::class.java, result)` from `org.junit.jupiter.api.Assertions` — a refused client is told nothing about itself. Do not use `kotlin.test`: it is not a declared test dependency of this module |

## Acceptance criteria

- [ ] `HandshakeTest.aMatchingVersionIsWelcomed` passes
- [ ] `HandshakeTest.aDefaultedHelloIsWelcomed` passes
- [ ] `HandshakeTest.anOlderVersionIsRefused` passes
- [ ] `HandshakeTest.aNewerVersionIsRefused` passes
- [ ] `HandshakeTest.aRefusalCarriesNoDeviceId` passes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
