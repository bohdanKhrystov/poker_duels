---
schema: 2
id: TASK-020212
title: docs/protocol.md, and the test that keeps it honest
type: task
status: backlog
parent: STORY-0202
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, protocol, docs]
depends_on: [TASK-020210]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew build
---

## Goal

`EPIC-03` can read one table instead of the Kotlin, and a message added without a documentation row
fails the build.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDocumentationTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt`,
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDescriptors.kt`.

## Scope

- `docs/protocol.md`, in this order and no longer than a page:
  1. A one-paragraph statement that this document is the contract for `EPIC-03`, that the Kotlin in
     `duels.poker.server.protocol` is the source of truth, and that the TypeScript is generated from
     it (`ADR-0003`, `STORY-0203`).
  2. A line reading exactly `Protocol version: **1**`.
  3. One table, `| Message | Direction | Payload | Sent when |`, with one row per message type. Every
     row's first cell is the discriminator in backticks and its second is exactly
     `client → server` or `server → client`:

     | Message | Direction |
     | --- | --- |
     | `Hello` | client → server |
     | `Act` | client → server |
     | `Welcome` | server → client |
     | `Failure` | server → client |
     | `Snapshot` | server → client |
     | `Events` | server → client |
     | `YourTurn` | server → client |
     | `Rejected` | server → client |

  4. A short list of the seven `ProtocolError` values with one line each.
  5. Three notes, one sentence each: the discriminator key is `type`; defaults are always written,
     so `protocolVersion` and the zero amounts of `LegalActions` are present in every frame; a frame
     the server cannot decode produces a `Failure`, never a dropped connection for anyone else.
- No prose about rooms, sessions or reconnection: those stories have not been written, and a
  document that describes messages that do not exist is worse than one that is short. `DEC-010` is
  where the room messages are waiting.

## Out of scope

- Linking the document from `CLAUDE.md` or `docs/architecture.md` — not yet ticketed.
- Any TypeScript — `STORY-0203`, blocked on `DEC-007`.
- Describing the HTTP read path — `STORY-0211`.

## Tests

`ProtocolDocumentationTest`, JUnit 5, package `duels.poker.server.protocol`. Locate the document by
walking up from the test's working directory, which Gradle sets to the module directory:

```kotlin
private val doc: String = generateSequence(File("").absoluteFile) { it.parentFile }
    .map { File(it, "docs/protocol.md") }
    .firstOrNull { it.isFile }
    ?.readText()
    ?: error("docs/protocol.md not found above ${File("").absolutePath}")
```

Use `subtypeNames` from `ProtocolDescriptors.kt` for both hierarchies; do not hard-code the message
list in the test, or the test would only be checking one hard-coded list against another.

| Test | Proves |
| --- | --- |
| `everyClientMessageHasARowSayingClientToServer` | for each name from `ClientMessage`'s descriptor there is exactly one line starting with ``| `Name` |``, and it contains `client → server` |
| `everyServerMessageHasARowSayingServerToClient` | the same for `ServerMessage`, containing `server → client` |
| `theDocumentNamesNoMessageThatDoesNotExist` | every table row whose first cell is a backticked identifier names a type present in one of the two hierarchies — a renamed message leaves no stale row behind |
| `theDocumentStatesTheCurrentProtocolVersion` | the text contains `Protocol version: **$PROTOCOL_VERSION**` |
| `theDocumentListsEveryProtocolError` | for each `ProtocolError` entry, the text contains its name |

## Acceptance criteria

- [ ] `ProtocolDocumentationTest.everyClientMessageHasARowSayingClientToServer` passes
- [ ] `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient` passes
- [ ] `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` passes
- [ ] `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion` passes
- [ ] `ProtocolDocumentationTest.theDocumentListsEveryProtocolError` passes
- [ ] `./gradlew build` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
