---
schema: 2
id: TASK-020302
title: Emit a TypeScript interface for a class or object descriptor
type: task
status: done
parent: STORY-0203
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, protocol, typescript]
depends_on: [TASK-020301]
verify:
  - ./gradlew :poker-server:test --tests '*TypeScriptDeclarationsTest'
  - ./gradlew :poker-server:test --tests '*TypeScriptTypesTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

A `StructureKind.CLASS` descriptor prints as an `export interface` of its elements, and a
`StructureKind.OBJECT` sealed variant prints as the discriminator alone — `ADR-0020`'s CLASS and
OBJECT rows.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarations.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarationsTest.kt` | create |

`TypeScriptTypes.kt` (from `TASK-020301`) is already in the same package — call `typeReference` and
`typeNameOf`, do not reimplement either.

## Scope

One `internal` function:

```kotlin
internal fun interfaceDeclaration(descriptor: SerialDescriptor, discriminator: String? = null): String
```

The exact output format, which every later ticket byte-compares against:

- Opening line `export interface ${typeNameOf(descriptor)} {`, closing line `}`.
- **Two-space** indent for properties (this is TypeScript, not Kotlin), one property per line,
  `name: reference;` — trailing semicolon, no trailing comma.
- When `discriminator` is non-null, `  type: "$discriminator";` is the **first** property line.
  The key is the literal `type` because `protocolJson` pins `classDiscriminator = "type"`.
- Property order is descriptor element order — `getElementName(i)` / `getElementDescriptor(i)`
  for `i in 0 until elementsCount`. Do not sort.
- No trailing newline: the assembler in `TASK-020305` joins declarations itself.
- Properties are **not** `readonly` and **never** optional. `encodeDefaults = true` means a
  server-emitted field is always present, and `ignoreUnknownKeys = false` means the client must
  send what the server checks. `readonly` appears only inside an array reference, where
  `typeReference` already put it.

A descriptor with zero elements and a discriminator therefore yields exactly `ADR-0020`'s
`{ type: "Name" }` row, with no special case in the code.

## Out of scope

- `ENUM` and `SEALED` — `TASK-020303`, same file.
- Deciding *which* descriptors get a discriminator: the caller passes it. The walk in
  `TASK-020304` is what knows a descriptor was reached as a sealed subtype.

## Tests

`TypeScriptDeclarationsTest`. Assert whole strings with `assertEquals` against a `trimIndent()`
literal — the format is the contract, so a test that only checks `contains` is not enough.

| Test | Proves |
| --- | --- |
| `aVariantCarriesItsDiscriminatorFirst` | `interfaceDeclaration(Hello.serializer().descriptor, "Hello")` equals exactly `export interface Hello {` / `  type: "Hello";` / `  deviceId: string \| null;` / `  protocolVersion: number;` / `}` |
| `aPlainClassHasNoDiscriminator` | `interfaceDeclaration(Board.serializer().descriptor)` equals exactly `export interface Board {` / `  cards: readonly string[];` / `}` — no `type` line, and `Card` arrives as `string` |
| `anObjectIsTheDiscriminatorAlone` | `interfaceDeclaration(CreateRoom.serializer().descriptor, "CreateRoom")` equals exactly `export interface CreateRoom {` / `  type: "CreateRoom";` / `}` |
| `propertiesKeepDescriptorOrder` | `interfaceDeclaration(PlayerView.serializer().descriptor)` has its twelve properties in the order `viewerSeat, handNumber, buttonSeat, street, board, pot, betToMatch, minRaiseTo, seatToAct, smallBlind, bigBlind, seats`, and `seatToAct` reads `number \| null` while `seats` reads `readonly SeatView[]` |

`PlayerView` is the falsifying fixture for ordering: it has twelve elements that are **not** in
alphabetical order, so a sorted or reversed implementation fails. Assert the property lines as a
list, not with `indexOf` comparisons.

## Acceptance criteria

- [ ] `TypeScriptDeclarationsTest.aVariantCarriesItsDiscriminatorFirst` passes
- [ ] `TypeScriptDeclarationsTest.aPlainClassHasNoDiscriminator` passes
- [ ] `TypeScriptDeclarationsTest.anObjectIsTheDiscriminatorAlone` passes
- [ ] `TypeScriptDeclarationsTest.propertiesKeepDescriptorOrder` passes
- [ ] `TypeScriptTypesTest` still passes, unchanged — this ticket adds a caller, it does not
      change `typeReference`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
