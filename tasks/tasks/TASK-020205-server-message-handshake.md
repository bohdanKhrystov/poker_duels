---
schema: 2
id: TASK-020205
title: ProtocolError and the two handshake ServerMessages
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, serialization]
depends_on: [TASK-020201]
verify:
  - ./gradlew :poker-server:test --tests '*ServerMessageHandshakeTest'
  - ./gradlew :poker-server:check
---

## Goal

The `ServerMessage` hierarchy exists, opened with the two messages the connection boundary needs —
an accepted handshake and a typed refusal drawn from a closed error set.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt` (the annotation pattern).

## Scope

- `ProtocolError.kt`: `@Serializable public enum class ProtocolError` with exactly seven entries,
  in this order — `UNKNOWN_MESSAGE`, `MALFORMED_MESSAGE`, `VERSION_MISMATCH`, `NOT_YOUR_TURN`,
  `UNKNOWN_ROOM`, `ROOM_FULL`, `NOT_IN_DUEL`. KDoc: the set is closed on purpose, because an open
  `error: String` cannot be branched on or tested by a client. One line per entry saying when the
  server sends it. `MALFORMED_MESSAGE` is the seventh alongside the story's six: a frame that is not
  JSON and a frame naming a message we do not have are different client bugs and a client should be
  able to tell them apart.
- `ServerMessage.kt`: `@Serializable public sealed interface ServerMessage` with exactly two nested
  members for now, each `@Serializable` with an explicit `@SerialName`:

  ```kotlin
  @Serializable
  @SerialName("Welcome")
  public data class Welcome(
      val deviceId: String,
      val protocolVersion: Int = PROTOCOL_VERSION,
  ) : ServerMessage

  @Serializable
  @SerialName("Failure")
  public data class Failure(val error: ProtocolError) : ServerMessage
  ```

- `Welcome.deviceId` is the id the server issued or recognised (`ADR-0012`, `STORY-0205`); this
  ticket only declares the field, it mints nothing.
- KDoc on the interface must state the hierarchy's standing rule, because everything after this
  ticket inherits it: **a `ServerMessage` that carries game state carries a `PlayerView`, never a
  `GameState`.** Record that this is currently impossible by construction, not by convention —
  `GameState`, `Deck` and `Rng` carry no `@Serializable`, so a `@Serializable` subtype declaring one
  of them does not compile. What is *not* impossible by construction is a raw `Int` seed or a stray
  `Card`, which is why `TASK-020211` walks the descriptors.
- `Failure` carries no free-text detail. Adding one would reopen the closed set through the back
  door.

## Out of scope

- `Snapshot`, `Events`, `YourTurn`, `Rejected` — `TASK-020206` adds them to this same file.
- The handshake function that chooses between `Welcome` and `Failure` — `TASK-020207`.
- Asserting the *set* of subtypes: do not write a test here that pins how many members
  `ServerMessage` has, because `TASK-020206` legitimately adds four. `TASK-020210` owns that
  assertion, once the hierarchy is complete.

## Tests

`ServerMessageHandshakeTest`, JUnit 5, package `duels.poker.server.protocol`. Encode through
`ServerMessage.serializer()` with `protocolJson`.

| Test | Proves |
| --- | --- |
| `welcomeRoundTrips` | `Welcome("device-1")` encodes and decodes back to an equal value |
| `welcomeCarriesItsVersionEvenWhenDefaulted` | the encoded string of `Welcome("device-1")` contains `"protocolVersion":1` |
| `failureRoundTrips` | `Failure(ProtocolError.VERSION_MISMATCH)` encodes and decodes back to an equal value |
| `theDiscriminatorsAreExplicit` | the encoded strings contain `"type":"Welcome"` and `"type":"Failure"` |
| `theErrorSetIsTheDeclaredSeven` | `ProtocolError.entries.map { it.name }` equals the seven names above, in that order |

## Acceptance criteria

- [ ] `ServerMessageHandshakeTest.welcomeRoundTrips` passes
- [ ] `ServerMessageHandshakeTest.welcomeCarriesItsVersionEvenWhenDefaulted` passes
- [ ] `ServerMessageHandshakeTest.failureRoundTrips` passes
- [ ] `ServerMessageHandshakeTest.theDiscriminatorsAreExplicit` passes
- [ ] `ServerMessageHandshakeTest.theErrorSetIsTheDeclaredSeven` passes
- [ ] No test in this ticket asserts the number of `ServerMessage` members
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
