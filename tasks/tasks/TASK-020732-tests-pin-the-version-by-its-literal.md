---
schema: 2
id: TASK-020732
title: Three tests pin the protocol version by its literal instead of by the constant
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, protocol, tests]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ServerMessageHandshakeTest'
  - ./gradlew :poker-server:test --tests '*ProtocolCodecTest'
  - ./gradlew :poker-server:test --tests '*ClientMessageTest'
  - grep -c 'protocolVersion..:1' poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt | grep -qx 0
  - grep -c 'protocolVersion..:1' poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt | grep -qx 0
  - grep -c 'protocolVersion..:1' poker-server/src/test/kotlin/duels/poker/server/protocol/ClientMessageTest.kt | grep -qx 0
  - ./gradlew :poker-server:check
---

## Goal

`PROTOCOL_VERSION` is the only place the wire version is written down. Three tests currently write
it down a second time, and this ticket takes that second copy away — while the version is still `1`,
so the change is a no-op on the wire and green on its own.

## What was found

`TASK-020719` bumps `PROTOCOL_VERSION` to `2` and budgets three files for it: the constant, its pin
in `ProtocolJsonTest`, and the version line in `docs/protocol.md`. That ticket says of every other
test that touches the version:

> If any of them needs editing, stop: that is a hard-coded `1` this ticket has not found, and it
> belongs in this ticket's diff with a note.

Three were found, and they do not fit: a micro-ticket touches at most three files, so carrying them
would have made `TASK-020719` a six-file ticket. They come out here instead, ahead of the bump.

They were invisible to the survey that wrote `TASK-020719` because none of them names
`PROTOCOL_VERSION`. Each asserts against the literal **inside an encoded JSON string**, so only a
grep for `protocolVersion` finds them:

| Assertion | Today |
| --- | --- |
| `ServerMessageHandshakeTest` line 22 | `encoded.contains("\"protocolVersion\":1")` |
| `ProtocolCodecTest` line 34 | `frame.contains("\"protocolVersion\":1")` |
| `ClientMessageTest` line 32 | `assertContains(encoded, "\"protocolVersion\":1")` |

Each is claiming *the field reaches the wire*, not *the version is 1*. Neither is the pin — that is
`ProtocolJsonTest.theProtocolVersionIsOne`, and it stays the pin. So none of the three should have
had a literal in it, at any version.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ClientMessageTest.kt` | modify |

## Scope

- In each of the three files, replace the literal `1` in the version assertion with an
  interpolation of `PROTOCOL_VERSION` — `"\"protocolVersion\":$PROTOCOL_VERSION"` — adding the
  import where the file does not already have one.
- **Interpolate, do not bump.** Writing `2` would leave the same defect one version later, which is
  the whole reason for touching these files.
- Change nothing else in any of the three. The assertion messages, the other assertions and the
  test names all stay exactly as they are.

## Out of scope

- `PROTOCOL_VERSION`'s value, `docs/protocol.md`, and `ProtocolJsonTest`. `TASK-020719` owns all
  three and lands after this.
- `ProtocolCodecJunkTest` lines 74 and 130, which also read `"protocolVersion":1`. Those are junk
  frames fed to the codec, which does not check the version at parse time, so they assert what they
  always did and are deliberately left alone.

## Tests

No new test. Three existing assertions stop hard-coding a value they were never about.

| Test | Proves |
| --- | --- |
| `ServerMessageHandshakeTest` | a `Welcome` still carries `protocolVersion` on the wire |
| `ProtocolCodecTest` | an encoded frame still carries `protocolVersion` |
| `ClientMessageTest` | a `Hello` still carries `protocolVersion` |

## Acceptance criteria

- [ ] All three assertions interpolate `PROTOCOL_VERSION`; none contains a literal version
- [ ] No other line of any of the three files changed
- [ ] The three `grep` commands in `verify:` exit 0
- [ ] `ProtocolJsonTest.theProtocolVersionIsOne` is untouched and still passes — it is the pin
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
