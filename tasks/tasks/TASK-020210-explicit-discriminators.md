---
schema: 2
id: TASK-020210
title: Every message's discriminator is an explicit @SerialName
type: task
status: done
parent: STORY-0202
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, protocol, serialization, wire-stability]
depends_on: [TASK-020204, TASK-020206]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:check
---

## Goal

A test, not a reading of the source, proves that renaming a Kotlin message class cannot change the
wire format — every discriminator is an explicit `@SerialName`, and the two hierarchies contain
exactly the messages we think they do.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDescriptors.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDiscriminatorTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerViewSerializationTest.kt` (the stable
descriptor-walking API this reuses).

## Scope

- `ProtocolDescriptors.kt`, test sources, package `duels.poker.server.protocol`: two `internal`
  helpers and nothing else. `TASK-020211` and `TASK-020212` consume them, so keep the signatures
  exactly:

  ```kotlin
  /** The `@SerialName` of every subtype of a sealed hierarchy, from its parent descriptor. */
  internal fun subtypeNames(parent: SerialDescriptor): List<String>

  /** Each subtype's own descriptor, in the same order as [subtypeNames]. */
  internal fun subtypeDescriptors(parent: SerialDescriptor): List<SerialDescriptor>
  ```

  Both read `parent.getElementDescriptor(1)` — element 1 of a sealed descriptor is the `value` slot,
  whose element names are the subtypes' serial names and whose element descriptors are the subtypes
  themselves — then iterate `0 until elementsCount`. `elementsCount`, `getElementName` and
  `getElementDescriptor` are stable API: no `@OptIn`, no reflection, no new dependency.
- `ProtocolDiscriminatorTest` asserts over `ClientMessage.serializer().descriptor` and
  `ServerMessage.serializer().descriptor` only. It never touches a main-source file; a failure here
  is a new ticket against the message hierarchy, not an edit from this branch.
- Compare name sets as `Set`, not `List`: the order in which the plugin registers subclasses is not
  a property this project should depend on.

## Out of scope

- Forbidden payload types — `TASK-020211`, which uses these same helpers.
- `docs/protocol.md` — `TASK-020212`, which also uses these helpers.
- The engine's own hierarchies (`GameEvent`, `PlayerAction`, `Rejection`): their discriminators are
  asserted in `poker-engine`'s tests.

## Tests

`ProtocolDiscriminatorTest`, JUnit 5, package `duels.poker.server.protocol`.

| Test | Proves |
| --- | --- |
| `theClientHierarchyIsExactlyTheDeclaredMessages` | `subtypeNames(ClientMessage.serializer().descriptor).toSet()` equals `setOf("Hello", "Act")` — a message added without a ticket fails here |
| `theServerHierarchyIsExactlyTheDeclaredMessages` | the same for `ServerMessage` equals `setOf("Welcome", "Failure", "Snapshot", "Events", "YourTurn", "Rejected")` |
| `noDiscriminatorIsAFullyQualifiedClassName` | across both hierarchies, no serial name contains a `.` and none starts with `duels.` — the default serial name *is* the qualified class name, so this is what proves every `@SerialName` is explicit |
| `everyDiscriminatorIsShortAndUnique` | across both hierarchies together, the names are distinct and each is at most 16 characters |
| `everySubtypeDescriptorCarriesItsOwnName` | for each hierarchy, `subtypeDescriptors(parent).map { it.serialName }` equals `subtypeNames(parent)` — the discriminator on the wire is the subtype's own serial name, not an alias registered elsewhere |

## Acceptance criteria

- [ ] `ProtocolDiscriminatorTest.theClientHierarchyIsExactlyTheDeclaredMessages` passes
- [ ] `ProtocolDiscriminatorTest.theServerHierarchyIsExactlyTheDeclaredMessages` passes
- [ ] `ProtocolDiscriminatorTest.noDiscriminatorIsAFullyQualifiedClassName` passes
- [ ] `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` passes
- [ ] `ProtocolDiscriminatorTest.everySubtypeDescriptorCarriesItsOwnName` passes
- [ ] No main-source file is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
