---
schema: 2
id: TASK-020719
title: The wire protocol moves to version 2
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 6
labels: [server, protocol]
depends_on: [TASK-020718]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolJsonTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests '*HandshakeTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketHandshakeTest'
  - ./gradlew :poker-server:check
---

## Goal

`PROTOCOL_VERSION` is `2`, and `docs/protocol.md` says so.

`ADR-0017` accepted that "the protocol grows, and `PROTOCOL_VERSION` moves". It moves **once**, here,
before the growth — so no commit in this story ever ships a new message under version 1.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | modify |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ClientMessageTest.kt` | modify |

`ProtocolJsonTest` is in the budget, not a bystander: `theProtocolVersionIsOne` asserts the literal
`1` and is the one test this change is designed to turn red. This ticket owns renaming it and moving
its expected value, and changes nothing else in that file — `defaultValuesReachTheWire` and
`unknownKeysAreRefused` keep every assertion they have.

## Scope

- `PROTOCOL_VERSION` becomes `2`.
- Extend that constant's KDoc with one sentence saying what version 2 is: the version in which
  `ServerMessage` gained `DuelFinished` and both hierarchies gained the room messages (`ADR-0017`).
- `docs/protocol.md` line `Protocol version: **1**` becomes `Protocol version: **2**`. Change no
  other line of the document.
- Rename `ProtocolJsonTest.theProtocolVersionIsOne` to `theProtocolVersionIsTwo` and assert
  `assertEquals(2, PROTOCOL_VERSION)`.

## Out of scope

- Any new message. `TASK-020720`, `TASK-020727` and `TASK-020728` add them.
- Any negotiation, fallback or support for older clients — `PROTOCOL_VERSION`'s KDoc already says
  bumping it breaks every older client, deliberately, and that stays true.

## Tests

`ProtocolJsonTest`

| Test | Proves |
| --- | --- |
| `theProtocolVersionIsTwo` | `PROTOCOL_VERSION == 2` |

Every other test that touches the version — `HandshakeTest`'s four cases, `DuelSocketHandshakeTest`,
`ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion`, `ClientMessageTest` — reads
`PROTOCOL_VERSION` symbolically and must pass **unchanged**. They are in `verify:`, not in the
budget. If any of them needs editing, stop: that is a hard-coded `1` this ticket has not found, and
it belongs in this ticket's diff with a note.

**Three were found**, and this ticket was widened by three files to carry them, exactly as that
instruction directs. They were invisible to the survey that wrote this ticket because none of them
names `PROTOCOL_VERSION` — each asserts against the literal inside an encoded JSON string, so only
`grep protocolVersion` finds them:

| Assertion | Was |
| --- | --- |
| `ServerMessageHandshakeTest.theWelcomeCarriesTheProtocolVersion` | `encoded.contains("\"protocolVersion\":1")` |
| `ProtocolCodecTest` line 34 | `frame.contains("\"protocolVersion\":1")` |
| `ClientMessageTest` line 32 | `assertContains(encoded, "\"protocolVersion\":1")` |

Each makes the same claim — that the field reaches the wire — and none of them is the version pin,
so all three interpolate `PROTOCOL_VERSION` and cannot go stale at the next bump.
`ProtocolJsonTest.theProtocolVersionIsTwo` stays the pin on the value.

`ProtocolCodecJunkTest` lines 74 and 130 also contain `"protocolVersion":1`, inside junk frames fed
to the codec. They are unaffected — the codec does not check the version at parse time — and are
deliberately left alone.

## Acceptance criteria

- [ ] `ProtocolJsonTest.theProtocolVersionIsTwo` passes
- [ ] `ProtocolDocumentationTest.theDocumentStatesTheCurrentProtocolVersion` passes with
      `ProtocolDocumentationTest.kt` unchanged
- [ ] `HandshakeTest` and `DuelSocketHandshakeTest` pass with both files unchanged
- [ ] `theProtocolVersionIsOne` no longer exists
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
