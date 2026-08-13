---
schema: 2
id: TASK-020303
title: Emit a TypeScript union for an enum and for a sealed hierarchy
type: task
status: ready
parent: STORY-0203
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, protocol, typescript]
depends_on: [TASK-020302]
verify:
  - ./gradlew :poker-server:test --tests '*TypeScriptDeclarationsTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

An `ENUM` descriptor prints as a union of string literals, and a `SEALED` descriptor prints as a
union of its variant type names — read out of the descriptor, never from a hand-kept list.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarations.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarationsTest.kt` | modify |

Read also `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDescriptors.kt` — it is
the same structural trick, and the test here cross-checks against it. It lives in the **test**
source set, so the emitter (in `main`) cannot import it and must do the navigation itself. Do not
move or widen that file.

## Scope

Three `internal` functions added to `TypeScriptDeclarations.kt`, no change to
`interfaceDeclaration`:

```kotlin
internal fun sealedVariants(descriptor: SerialDescriptor): List<Pair<String, SerialDescriptor>>
internal fun enumDeclaration(descriptor: SerialDescriptor): String
internal fun unionDeclaration(descriptor: SerialDescriptor): String
```

- **`sealedVariants`** — element **1** of a sealed descriptor is its `value` slot; that slot's
  element *names* are the subtypes' `@SerialName`s and its element *descriptors* are the subtypes.
  Return them paired, in that order. (Element 0 is the `type` slot, a plain `kotlin.String`; the
  `value` slot's own kind is `CONTEXTUAL` and is never itself a type — navigate through it,
  never emit it.)
- **`enumDeclaration`** — `export type Street = "PREFLOP" | "FLOP" | ... ;` from
  `getElementName(i)` in order. One line, ending in `;`.
- **`unionDeclaration`** — `export type ClientMessage = Act | CreateRoom | Hello | JoinRoom;` from
  `sealedVariants(descriptor).map { it.first }`, in descriptor order. One line, ending in `;`.
  The member names are the serial names, which are also the discriminator literals
  `TASK-020302` writes into each variant, so the two cannot disagree.

Neither function sorts, and neither takes a list of names as a parameter. An enum's element
descriptors are its *entries*, not fields — this ticket must not walk into them.

## Out of scope

- Recursing from a union into its variants' declarations — `TASK-020304`.
- Any change to `interfaceDeclaration` or to `TypeScriptTypes.kt`.

## Tests

Added to `TypeScriptDeclarationsTest`. `Street` and `ActionType` carry no `@Serializable`
annotation and so have no generated `.serializer()`; use `serializer<Street>()` from
`kotlinx.serialization.serializer`.

| Test | Proves |
| --- | --- |
| `anEnumIsAUnionOfItsEntryNames` | equals exactly `export type ActionType = "FOLD" \| "CHECK" \| "CALL" \| "BET" \| "RAISE" \| "ALL_IN";` — `ActionType` is the falsifying fixture because its six entries are in neither alphabetical nor reverse order |
| `aSealedHierarchyIsAUnionOfItsVariants` | `unionDeclaration(ClientMessage.serializer().descriptor)` equals exactly `export type ClientMessage = Act \| CreateRoom \| Hello \| JoinRoom;` |
| `theVariantsComeFromTheDescriptor` | `sealedVariants(ServerMessage.serializer().descriptor).map { it.first }` equals `subtypeNames(ServerMessage.serializer().descriptor)` from `ProtocolDescriptors.kt`, and the list is not empty — the emitter and the existing payload police read the same structure |
| `aVariantDescriptorIsTheSubtypeNotTheValueSlot` | for `Rejection`, the pair named `HandComplete` has `StructureKind.OBJECT` and the pair named `NotYourTurn` has one element called `seatToAct` — a walk that stopped at the `CONTEXTUAL` value slot could not produce either |

## Acceptance criteria

- [ ] `TypeScriptDeclarationsTest.anEnumIsAUnionOfItsEntryNames` passes
- [ ] `TypeScriptDeclarationsTest.aSealedHierarchyIsAUnionOfItsVariants` passes
- [ ] `TypeScriptDeclarationsTest.theVariantsComeFromTheDescriptor` passes
- [ ] `TypeScriptDeclarationsTest.aVariantDescriptorIsTheSubtypeNotTheValueSlot` passes
- [ ] The four tests `TASK-020302` added still pass with their assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
