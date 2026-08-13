---
schema: 2
id: TASK-020301
title: Map a serial descriptor to a TypeScript type reference
type: task
status: ready
parent: STORY-0203
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, protocol, typescript]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*TypeScriptTypesTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

Given any `SerialDescriptor` the protocol reaches, the emitter can name the TypeScript type that
`protocolJson` puts on the wire for it — `string` for a `Card`, `readonly ActionType[]` for a
`Set<ActionType>`, `string | null` for a nullable field.

This is the bottom layer of the emitter `ADR-0020` accepted. Nothing here walks or emits
declarations; it answers one question: *what do I write on the right-hand side of a colon?*

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/typescript/TypeScriptTypes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/TypeScriptTypesTest.kt` | create |

Read also, for the mapping table this ticket implements verbatim:
`docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md`.

## Scope

Two `internal` functions in package `duels.poker.server.protocol.typescript`.

**`internal fun typeNameOf(descriptor: SerialDescriptor): String`** — the last dotted segment of
`descriptor.serialName`, with a trailing `?` removed *before* taking the segment. So
`duels.poker.engine.game.PlayerView` → `PlayerView`, and `Act` → `Act` (a sealed subtype's serial
name is already its `@SerialName`, undotted).

**`internal fun typeReference(descriptor: SerialDescriptor): String`** — `when (descriptor.kind)`,
exactly `ADR-0020`'s table:

| Kind | Result |
| --- | --- |
| `PrimitiveKind.STRING`, `PrimitiveKind.CHAR` | `string` |
| `PrimitiveKind.BOOLEAN` | `boolean` |
| `BYTE`, `SHORT`, `INT`, `LONG`, `FLOAT`, `DOUBLE` | `number` |
| `StructureKind.LIST` | `readonly ` + `typeReference(getElementDescriptor(0))` + `[]` |
| `StructureKind.CLASS`, `StructureKind.OBJECT`, `SerialKind.ENUM`, `PolymorphicKind.SEALED` | `typeNameOf(descriptor)` |
| anything else | throw `IllegalArgumentException` naming the kind **and** the serial name |

Then, if `descriptor.isNullable`, the result is `"$base | null"`.

Two things a wrong guess gets wrong, both load-bearing:

- **Dispatch on the kind, never on the serial name.** `Card` is a `@JvmInline value class` over an
  `Int` whose `CardSerializer` declares `PrimitiveKind.STRING` under the serial name
  `duels.poker.engine.card.Card`. It must come out as `string`. This case is the whole reason
  `ADR-0020` chose descriptors over class reflection.
- **`Set` is `StructureKind.LIST`.** `kotlin.collections.LinkedHashSet` and
  `kotlin.collections.ArrayList` are both LIST descriptors; both become `readonly T[]`.

The `else` branch must throw rather than guess: the first MAP or CONTEXTUAL on the wire is meant to
extend the table in a reviewed diff.

## Out of scope

- Any `export` declaration — `TASK-020302` and `TASK-020303`.
- Walking the protocol, deduplicating, or ordering anything — `TASK-020304`.
- A Gradle task or a generated file — `TASK-020307`.

## Tests

`TypeScriptTypesTest`, in `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/`.

Fixtures are real protocol descriptors; the observed values below are the ones to assert. Reach
elements by name (`getElementIndex("deviceId")`), not by a bare index. `ActionType` and `Street`
have no `@Serializable` annotation and therefore **no** generated `.serializer()` companion — get
them with `serializer<ActionType>()` from `kotlinx.serialization.serializer`.

| Test | Proves |
| --- | --- |
| `aCardIsAString` | `CardSerializer.descriptor` → `string`. `ADR-0020`'s deciding case: a generator reflecting over the Kotlin class would emit an object or a number here |
| `primitivesMapToStringNumberAndBoolean` | `String.serializer()` → `string`; `Int.serializer()` and `Long.serializer()` → `number`; `Boolean.serializer()` → `boolean` |
| `aNullableFieldIsUnionedWithNull` | `Hello.serializer().descriptor`, element `deviceId` → `string \| null`; `DuelOutcome.serializer().descriptor`, element `winner` → `number \| null` |
| `aListIsAReadonlyArray` | `Board.serializer().descriptor`, element `cards` → `readonly string[]` — a `List<Card>`, so it also pins the `Card` case through a list |
| `aSetIsAReadonlyArray` | `LegalActions.serializer().descriptor`, element `allowed` → `readonly ActionType[]` |
| `aNamedTypeIsItsLastDottedSegment` | `typeReference(PlayerView.serializer().descriptor)` → `PlayerView`; `typeReference(serializer<Street>().descriptor)` → `Street` |
| `aMapIsRefused` | `typeReference(serializer<Map<String, Int>>().descriptor)` throws `IllegalArgumentException`, and the message contains `LinkedHashMap` |

## Acceptance criteria

- [ ] `TypeScriptTypesTest.aCardIsAString` passes
- [ ] `TypeScriptTypesTest.primitivesMapToStringNumberAndBoolean` passes
- [ ] `TypeScriptTypesTest.aNullableFieldIsUnionedWithNull` passes
- [ ] `TypeScriptTypesTest.aListIsAReadonlyArray` passes
- [ ] `TypeScriptTypesTest.aSetIsAReadonlyArray` passes
- [ ] `TypeScriptTypesTest.aNamedTypeIsItsLastDottedSegment` passes
- [ ] `TypeScriptTypesTest.aMapIsRefused` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
