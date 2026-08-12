---
schema: 2
id: TASK-020209
title: One bad frame is a value, not an exception
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, protocol, robustness]
depends_on: [TASK-020208]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolCodecJunkTest'
  - ./gradlew :poker-server:check
---

## Goal

Every kind of junk a client can put on a socket is pinned to the typed refusal it produces, so no
future change to the codec can turn one malformed frame into a thrown exception that ends somebody's
duel.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecJunkTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt`,
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt`.

## Scope

- A test class only. **No main-source change.** If an input below does not produce the stated
  result, the fix is a new ticket against `ProtocolCodec`, not an edit from this branch.
- Each test loops over its listed inputs and asserts with a message naming the offending input, so a
  failure report says which one broke.
- `ProtocolCodecTest` is not in this ticket's budget and is not edited; the overlapping cases there
  stay as they are.

## Tests

`ProtocolCodecJunkTest`, JUnit 5, package `duels.poker.server.protocol`, using
`org.junit.jupiter.api.Assertions.assertEquals`. Every assertion compares against
`Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)` or `Decoded.Refused(ProtocolError.UNKNOWN_MESSAGE)`
as stated.

Four tests expect `Refused(MALFORMED_MESSAGE)` for every input in their list. Write the JSON inputs
as Kotlin raw strings (`"""…"""`) so no escaping is needed, except the two below that end in a
quote — a raw string cannot, so those use ordinary escapes. Copy the inputs exactly:

```kotlin
// unparseableFramesAreMalformed
"", "   ", "<html>", "{", "{\"type\":\"Hello\"", "a".repeat(100_000)

// framesThatAreNotJsonObjectsAreMalformed
"[1,2,3]", "\"just a string\"", "42", "null", "true"

// framesWithNoUsableDiscriminatorAreMalformed
"{}", """{"deviceId":"d1"}""", """{"type":7}""", """{"type":null}""", """{"type":{"a":1}}"""

// wellNamedButUnusableBodiesAreMalformed
"""{"type":"Act"}""",                                    // required fields missing
"""{"type":"Act","handNumber":0,"actionSequence":0,"action":{"type":"Fold","seat":0}}""",
                                                          // fails Act's own require
"""{"type":"Act","handNumber":1,"actionSequence":0,"action":{"type":"Bet","seat":0,"to":0}}""",
                                                          // fails PlayerAction.Bet's require
"""{"type":"Hello","protocolVersion":1,"extra":true}"""   // unknown key, ignoreUnknownKeys = false
```

The fourth list is the one that matters most: those frames parse cleanly and fail inside a
constructor, which is the path a `catch (_: SerializationException)` alone would miss.

One test expects `Refused(UNKNOWN_MESSAGE)`:

```kotlin
// unknownAndWrongDirectionFramesAreUnknown
"""{"type":"Surrender"}""",
"""{"type":"act"}""",                    // wrong case — discriminators are exact
"""{"type":""}""",
"""{"type":"Welcome","deviceId":"d1"}""", // a client may not send a server message:
"""{"type":"Snapshot"}"""                 // the hierarchy split makes that automatic
```

| Test | Proves |
| --- | --- |
| `aGoodFrameStillDecodesAfterAJunkOne` | after feeding every input above through `decodeClient`, `decodeClient(ProtocolCodec.encode(ClientMessage.Hello("d1")))` is still a `Decoded.Message` — the codec holds no state and one bad frame poisons nothing |

## Acceptance criteria

- [ ] `ProtocolCodecJunkTest.unparseableFramesAreMalformed` passes
- [ ] `ProtocolCodecJunkTest.framesThatAreNotJsonObjectsAreMalformed` passes
- [ ] `ProtocolCodecJunkTest.framesWithNoUsableDiscriminatorAreMalformed` passes
- [ ] `ProtocolCodecJunkTest.wellNamedButUnusableBodiesAreMalformed` passes
- [ ] `ProtocolCodecJunkTest.unknownAndWrongDirectionFramesAreUnknown` passes
- [ ] `ProtocolCodecJunkTest.aGoodFrameStillDecodesAfterAJunkOne` passes
- [ ] No file outside the one in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
