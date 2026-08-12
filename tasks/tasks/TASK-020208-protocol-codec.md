---
schema: 2
id: TASK-020208
title: ProtocolCodec — encode, and a decode that returns a typed failure
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, protocol, serialization, robustness]
depends_on: [TASK-020204, TASK-020206]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolCodecTest'
  - ./gradlew :poker-server:check
---

## Goal

One object turns messages into frames and frames into either a message or a named reason it was not
one — so a bad frame from one client is a value the connection reports, never an exception thrown
across the connection boundary.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt`,
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolSamples.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/log/HandLogJson.kt` (the parse-then-inspect
pattern this ticket follows, minus the throwing).

## Scope

- `ProtocolCodec.kt`, package `duels.poker.server.protocol`, two public declarations:

  ```kotlin
  public sealed interface Decoded {
      public data class Message(val message: ClientMessage) : Decoded
      public data class Refused(val error: ProtocolError) : Decoded
  }

  public object ProtocolCodec {
      public fun encode(message: ServerMessage): String
      public fun encode(message: ClientMessage): String
      public fun decodeClient(text: String): Decoded
  }
  ```

- `Decoded` is **not** `@Serializable`: it is a server-internal result, not a wire type. Adding an
  annotation would put it in `TASK-020210`'s and `TASK-020212`'s inventories, where it does not
  belong.
- Both `encode` overloads go through `protocolJson` and the **parent** serializer
  (`ServerMessage.serializer()`, `ClientMessage.serializer()`), never a subtype's own — the
  discriminator only exists through the parent.
- `decodeClient` follows `HandLogJson`'s shape — parse first, inspect, then decode — but returns
  instead of throwing:
  1. `protocolJson.parseToJsonElement(text)`; on failure return `Refused(MALFORMED_MESSAGE)`.
  2. If the element is not a `JsonObject`, or has no `type` member, or that member is not a string
     primitive, return `Refused(MALFORMED_MESSAGE)`.
  3. If the discriminator is not one this hierarchy knows, return `Refused(UNKNOWN_MESSAGE)`. Derive
     the known set from the descriptor rather than repeating the strings:

     ```kotlin
     private val clientMessageNames: Set<String> =
         ClientMessage.serializer().descriptor.getElementDescriptor(1).let { sealed ->
             (0 until sealed.elementsCount).map { sealed.getElementName(it) }.toSet()
         }
     ```

     Element 1 of a sealed descriptor is the `value` slot, and its element names are the subtypes'
     `@SerialName`s. `elementsCount`, `getElementName` and `getElementDescriptor` are stable API, so
     no `@OptIn` and no new dependency.
  4. `protocolJson.decodeFromJsonElement(ClientMessage.serializer(), element)`; on failure return
     `Refused(MALFORMED_MESSAGE)`.
- **Catch `IllegalArgumentException`, once, per step.** `SerializationException` extends it, and so
  does every `require` in `ClientMessage.Act` and in `PlayerAction` — a frame carrying
  `"handNumber":0` or a `Bet` of `0` fails inside a constructor, not inside the parser, and that
  path must return a value too. Do not catch `Exception` or `Throwable`: detekt forbids it, and an
  `OutOfMemoryError` is not a protocol error. Name the parameter `_` so detekt's swallowed-exception
  rule stays satisfied.
- No logging, no metrics, no side effect of any kind. The caller decides what a `Refused` means for
  the connection.

## Out of scope

- Decoding a `ServerMessage` from text: nothing on the server needs it, and a test that wants one
  can call `protocolJson` directly.
- The exhaustive junk-input suite — `TASK-020209`, which is where every malformed case is pinned.
- Sending, closing, or reporting anything to a client — `STORY-0205`.

## Tests

`ProtocolCodecTest`, JUnit 5, package `duels.poker.server.protocol`. Use
`org.junit.jupiter.api.Assertions.assertInstanceOf` and `assertEquals`; `kotlin.test` is not a
declared dependency of this module.

| Test | Proves |
| --- | --- |
| `helloSurvivesEncodeThenDecode` | `decodeClient(encode(Hello("d1")))` is `Decoded.Message` holding a value equal to the original |
| `actSurvivesEncodeThenDecode` | `decodeClient(encode(Act(3, 7, PlayerAction.Raise(0, 300))))` is `Decoded.Message` holding a value equal to the original |
| `anEncodedServerMessageCarriesItsDiscriminator` | `encode(Snapshot(sampleView()))` contains `"type":"Snapshot"` |
| `anEncodedWelcomeCarriesItsDefaultedVersion` | `encode(Welcome("d1"))` contains `"protocolVersion":1` |
| `anUnknownDiscriminatorIsRefusedAsUnknownMessage` | `decodeClient("""{"type":"Surrender"}""")` equals `Decoded.Refused(ProtocolError.UNKNOWN_MESSAGE)` |
| `nonJsonIsRefusedAsMalformed` | `decodeClient("not json at all")` equals `Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)` |
| `theKnownNamesComeFromTheDescriptor` | `decodeClient("""{"type":"Hello"}""")` is a `Decoded.Message` — the defaulted `Hello` decodes, proving the name set is derived and not mistyped |

## Acceptance criteria

- [ ] `ProtocolCodecTest.helloSurvivesEncodeThenDecode` passes
- [ ] `ProtocolCodecTest.actSurvivesEncodeThenDecode` passes
- [ ] `ProtocolCodecTest.anEncodedServerMessageCarriesItsDiscriminator` passes
- [ ] `ProtocolCodecTest.anEncodedWelcomeCarriesItsDefaultedVersion` passes
- [ ] `ProtocolCodecTest.anUnknownDiscriminatorIsRefusedAsUnknownMessage` passes
- [ ] `ProtocolCodecTest.nonJsonIsRefusedAsMalformed` passes
- [ ] `ProtocolCodecTest.theKnownNamesComeFromTheDescriptor` passes
- [ ] `ProtocolCodec.kt` contains no `throw` and no `catch (e: Exception)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
