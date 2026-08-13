---
schema: 2
id: TASK-020306
title: Every variant's TypeScript discriminator is its SerialName
type: task
status: backlog
parent: STORY-0203
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, protocol, typescript, test-strength]
depends_on: [TASK-020305]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolTypeScriptTest'
  - ./gradlew :poker-server:test --tests '*ProtocolJsonTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The one property a wrong generator would break invisibly is pinned by a test: every discriminator
literal in the emitted TypeScript equals the Kotlin `@SerialName`, and the key it sits under equals
the key `protocolJson` actually writes.

This is `STORY-0203`'s second acceptance criterion. A test-only ticket, because the emitter is
already complete after `TASK-020305` — if any assertion here fails, the fix is a new ticket, not a
quiet edit to the emitter.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/ProtocolTypeScriptTest.kt` | modify |

Read also, for the structural helpers and the existing precedent for this kind of walk:
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDescriptors.kt`,
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolPayloadTest.kt`.

## Scope

Three tests added to `ProtocolTypeScriptTest`, over the string `protocolTypeScript()` returns.
Every expectation is derived from a descriptor at run time — a hand-written list of today's twelve
message names would pass while proving nothing about the thirteenth.

## Tests

| Test | Proves |
| --- | --- |
| `everyMessageVariantDeclaresItsSerialNameAsItsDiscriminator` | for every name in `subtypeNames(ClientMessage.serializer().descriptor) + subtypeNames(ServerMessage.serializer().descriptor)`, the file contains the line `  type: "$name";`. Vacuity guard: assert the combined name list is not empty and contains `CreateRoom` — the `data object` variant, whose declaration is nothing *but* a discriminator, so an emitter that only wrote discriminators for classes with fields would still fail here |
| `everySealedUnionListsExactlyItsSubtypes` | for each of the five sealed descriptors reachable in the protocol — `ClientMessage`, `ServerMessage`, `PlayerAction`, `Rejection`, `GameEvent` — the file contains the line `export type ${short name} = ${subtypeNames(d).joinToString(" \| ")};`. Assert the loop ran five times, so a walk that quietly lost a hierarchy cannot pass by iterating an empty set |
| `theDiscriminatorKeyIsTheOneOnTheWire` | encode `Hello(deviceId = "d", protocolVersion = PROTOCOL_VERSION)` as a `ClientMessage` with `protocolJson`, parse it to a `JsonObject`, take the key whose value is `"Hello"`, and assert the emitted file uses that same key. The TypeScript's discriminator key is `protocolJson`'s `classDiscriminator`, checked against the actual bytes rather than against a constant either side could get wrong |

## Out of scope

- Any change to the emitter. If a test here fails, stop and report it: the emitter is wrong and
  that is a separate diff.
- Changing `ProtocolJsonTest`, `ProtocolPayloadTest` or `ProtocolDescriptors.kt`. This ticket only
  reads them. `ProtocolJsonTest` is in `verify:` as a bystander — nothing here alters what it
  observes, so it must pass untouched.

## Acceptance criteria

- [ ] `ProtocolTypeScriptTest.everyMessageVariantDeclaresItsSerialNameAsItsDiscriminator` passes
- [ ] `ProtocolTypeScriptTest.everySealedUnionListsExactlyItsSubtypes` passes
- [ ] `ProtocolTypeScriptTest.theDiscriminatorKeyIsTheOneOnTheWire` passes
- [ ] No file outside `ProtocolTypeScriptTest.kt` is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
