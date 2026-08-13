---
schema: 2
id: TASK-020718
title: The wire vocabulary is pinned in one place — the protocol document
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, protocol, tests]
depends_on: [TASK-020714, TASK-020717]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:test --tests '*ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - grep -c 'IsExactlyTheDeclaredMessages' poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDiscriminatorTest.kt | grep -qx 0
  - grep -c 'descriptors.size' poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolPayloadTest.kt | grep -qx 0
  - ./gradlew :poker-server:check
---

## Goal

The exact set of messages on the wire is pinned by `docs/protocol.md` and the two
`ProtocolDocumentationTest` cases that check it against the code — and by nothing else. Two
duplicate hard-coded copies of that set go away.

## Why now

`ADR-0017` grows the protocol: `STORY-0207` adds `ServerMessage.DuelFinished`, `RoomJoined`,
`ClientMessage.CreateRoom` and `JoinRoom`. Today each of those additions must edit **four** files —
the message type, `docs/protocol.md`, `ProtocolDiscriminatorTest`'s hard-coded set, and
`ProtocolPayloadTest`'s hard-coded subtype count — which is over this project's three-file budget
before a single line of behaviour is written.

Three of those four are not the same claim stated three times. `ProtocolDocumentationTest` already
proves set **equality** between the code and the document, in both directions:

- `everyServerMessageHasARowSayingServerToClient` — every declared message is documented;
- `theDocumentNamesNoMessageThatDoesNotExist` — every documented message is declared.

`ProtocolDiscriminatorTest.theServerHierarchyIsExactlyTheDeclaredMessages` and its client twin
restate the same fact against a literal in a test file, and `ProtocolPayloadTest`'s
`assertEquals(6, descriptors.size)` restates it again, more weakly, as a count. Adding a message
still stays a deliberate, reviewable act: it requires a new row in the contract document.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDiscriminatorTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolPayloadTest.kt` | modify |

`ProtocolDocumentationTest.kt` is **not** in the budget and must not be edited: it is the pin this
ticket is consolidating onto. Editing it would be the weakening this ticket is careful not to do.

## Scope

- Delete `ProtocolDiscriminatorTest.theClientHierarchyIsExactlyTheDeclaredMessages` and
  `ProtocolDiscriminatorTest.theServerHierarchyIsExactlyTheDeclaredMessages`. Delete nothing else
  in that file: `noDiscriminatorIsAFullyQualifiedClassName`, `everyDiscriminatorIsShortAndUnique`
  and `everySubtypeDescriptorCarriesItsOwnName` all stay, unchanged, and are descriptor-driven so
  they keep covering every message added later.
- Extend that class's KDoc with one sentence naming where the exact vocabulary is pinned now:
  `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient` and
  `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist`, against
  `docs/protocol.md`.
- In `ProtocolPayloadTest.theOnlyStateAServerMessageCarriesIsAPlayerView`, replace
  `assertEquals(6, descriptors.size)` with a vacuity guard — `assertTrue(descriptors.isNotEmpty())`
  with a message — and a comment saying why it is a guard and not a pin. This project has twice
  shipped a reflection test that inspected nothing; the guard is what stops that, and it is the
  only job the literal `6` was doing that this ticket keeps.
- Change nothing else in `ProtocolPayloadTest`. In particular
  `assertEquals(listOf("Snapshot"), carriers.map { it.first })` and the following
  `assertEquals(1, snapshot.elementsCount)` are the test's actual claim and stay exactly as they
  are.

## Out of scope

- Adding any message. `TASK-020720`, `TASK-020727` and `TASK-020728` each add one and each pays
  for its own document row.
- Any change to `docs/protocol.md` — `TASK-020719` moves the version line.

## Tests

No new test. This ticket removes two redundant assertions and keeps every other one, so its
evidence is that the surviving named cases still pass and the two named cases are gone.

`ProtocolDiscriminatorTest`

| Test | Proves |
| --- | --- |
| `noDiscriminatorIsAFullyQualifiedClassName` | every subtype still carries an explicit `@SerialName` |
| `everyDiscriminatorIsShortAndUnique` | discriminators stay short and distinct across both hierarchies |
| `everySubtypeDescriptorCarriesItsOwnName` | a subtype's descriptor name matches its element name |

`ProtocolPayloadTest`

| Test | Proves |
| --- | --- |
| `theOnlyStateAServerMessageCarriesIsAPlayerView` | `Snapshot` is still the only state-carrying message, and the walk is not vacuous |

`ProtocolDocumentationTest` — untouched, and in `verify:` as the pin that now stands alone.

## Acceptance criteria

- [ ] `ProtocolDiscriminatorTest.noDiscriminatorIsAFullyQualifiedClassName` passes
- [ ] `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` passes
- [ ] `ProtocolDiscriminatorTest.everySubtypeDescriptorCarriesItsOwnName` passes
- [ ] `ProtocolPayloadTest.theOnlyStateAServerMessageCarriesIsAPlayerView` passes
- [ ] `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient`,
      `.everyClientMessageHasARowSayingClientToServer` and `.theDocumentNamesNoMessageThatDoesNotExist`
      pass, with `ProtocolDocumentationTest.kt` unchanged in the diff
- [ ] Neither `theClientHierarchyIsExactlyTheDeclaredMessages` nor
      `theServerHierarchyIsExactlyTheDeclaredMessages` still appears in
      `ProtocolDiscriminatorTest.kt` — the first `grep` command in `verify:` exits 0
- [ ] `descriptors.size` no longer appears in `ProtocolPayloadTest.kt` — the second `grep`
      command in `verify:` exits 0
- [ ] `git diff --stat` names exactly the two files in the table above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
