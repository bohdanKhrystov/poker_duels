---
schema: 2
id: TASK-020304
title: Walk both message roots into an ordered list of declarations
type: task
status: done
parent: STORY-0203
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, protocol, typescript]
depends_on: [TASK-020303]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolTypeScriptTest'
  - ./gradlew :poker-server:test --tests '*TypeScriptDeclarationsTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

One recursive walk from `ClientMessage` and `ServerMessage` produces every declaration the protocol
needs, each exactly once, in a stable order — and refuses, loudly, anything it does not understand.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/typescript/ProtocolTypeScript.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/ProtocolTypeScriptTest.kt` | create |

`TypeScriptTypes.kt` and `TypeScriptDeclarations.kt` are already in the package; call
`interfaceDeclaration`, `enumDeclaration`, `unionDeclaration` and `sealedVariants` and add no new
formatting here.

## Scope

```kotlin
internal data class TypeScriptDeclaration(val name: String, val text: String)

internal val protocolRoots: List<SerialDescriptor>   // ClientMessage, then ServerMessage

internal fun protocolDeclarations(
    roots: List<SerialDescriptor> = protocolRoots,
): List<TypeScriptDeclaration>
```

Depth-first from each root in turn, with a `discriminator: String?` carried down:

| Kind | Emit | Then recurse into |
| --- | --- | --- |
| SEALED | `unionDeclaration` | each `sealedVariants` subtype, passing **its serial name** as the discriminator |
| CLASS, OBJECT | `interfaceDeclaration(d, discriminator)` | each element descriptor, discriminator `null` |
| ENUM | `enumDeclaration` | **nothing** |
| LIST | nothing | `getElementDescriptor(0)`, discriminator `null` |
| PRIMITIVE | nothing | nothing |
| anything else | — | throw `IllegalArgumentException` naming the kind and the serial name |

Three rules that are the whole difficulty of this ticket:

1. **Deduplicate named declarations only** — SEALED, CLASS, OBJECT and ENUM, keyed by
   `serialName.removeSuffix("?")`. A LIST descriptor must **never** enter the seen-set: every
   `List<T>` and `Set<T>` in the protocol shares the serial name `kotlin.collections.ArrayList` or
   `kotlin.collections.LinkedHashSet` regardless of its element type. A seen-set that includes them
   silently drops everything reachable only through a second list — `DuelOutcome.finalStacks` is
   walked first, so `Events.events` and `PlayerView.seats` are then skipped and `GameEvent`, all
   seventeen event types and `SeatView` never appear in the output. The file still compiles as
   TypeScript up to the dangling references, and nothing else notices.
2. **Do not recurse into an ENUM's elements.** An enum descriptor's element descriptors are its
   entries (`duels.poker.engine.game.ActionType.FOLD`, OBJECT kind, zero elements). Recursing emits
   one bogus `export interface FOLD {}` per entry.
3. **A short-name collision is fatal.** Two different serial names whose last dotted segment is
   equal must throw `IllegalStateException` naming both. `typeNameOf` is what makes them collide,
   so this is the guard `ADR-0020` asks for.

Order is discovery order — roots in the order given, elements in descriptor order, a union before
its variants. Do not sort: the byte-comparing check in `TASK-020308` compares against exactly this
order.

## Out of scope

- The header, the `ProtocolVersion` alias, and joining the declarations into a file —
  `TASK-020305`.
- Any Gradle task or committed output — `TASK-020307`.

## Tests

`ProtocolTypeScriptTest`. Build synthetic descriptors for the collision case with
`buildClassSerialDescriptor("a.Thing")` and `buildClassSerialDescriptor("b.Thing")`; add
`@OptIn(ExperimentalSerializationApi::class)` if the compiler asks for it.

| Test | Proves |
| --- | --- |
| `everyDeclarationAppearsExactlyOnce` | `protocolDeclarations().map { it.name }` has no duplicates and is not empty |
| `theTypesReachableOnlyThroughASecondListSurvive` | the names contain `GameEvent`, `SeatView`, `HandRevealed` and `StreetDealt`. Each is reachable only past a LIST descriptor whose serial name has already been seen on `DuelOutcome.finalStacks`, so rule 1 above is exactly what this falsifies |
| `enumEntriesAreNotDeclaredAsTypes` | the names contain `ActionType` and `Street` but none of `FOLD`, `ALL_IN`, `PREFLOP`, `COMPLETE`. Both enums are in the surface, so the absence is a real absence, not an empty search |
| `theWholeProtocolWalksWithoutAnUnsupportedKind` | `protocolDeclarations()` does not throw — this is the standing check on `ADR-0020`'s claim that the reachable surface is closed, and the first MAP or contextual type on the wire turns it red |
| `theOrderIsStable` | two calls return equal lists, and the first four names are `ClientMessage`, `Act`, `PlayerAction`, `AllIn` — pinning that a union precedes its variants and that discovery order, not sorting, is what orders the file |
| `aShortNameCollisionIsRefused` | `protocolDeclarations(listOf(a.Thing, b.Thing))` throws `IllegalStateException` whose message contains both `a.Thing` and `b.Thing` |

## Acceptance criteria

- [ ] `ProtocolTypeScriptTest.everyDeclarationAppearsExactlyOnce` passes
- [ ] `ProtocolTypeScriptTest.theTypesReachableOnlyThroughASecondListSurvive` passes
- [ ] `ProtocolTypeScriptTest.enumEntriesAreNotDeclaredAsTypes` passes
- [ ] `ProtocolTypeScriptTest.theWholeProtocolWalksWithoutAnUnsupportedKind` passes
- [ ] `ProtocolTypeScriptTest.theOrderIsStable` passes
- [ ] `ProtocolTypeScriptTest.aShortNameCollisionIsRefused` passes
- [ ] `TypeScriptDeclarationsTest` passes with every assertion unchanged — this ticket adds a
      caller and changes neither declaration function
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
