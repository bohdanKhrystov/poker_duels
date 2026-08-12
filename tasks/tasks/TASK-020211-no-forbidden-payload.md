---
schema: 2
id: TASK-020211
title: Structurally, no seed goes out and no card comes in
type: task
status: done
parent: STORY-0202
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, protocol, security, adr-0002]
depends_on: [TASK-020210]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolPayloadTest'
  - ./gradlew :poker-server:check
---

## Goal

The two rules this protocol exists to enforce are checked by a walk over the serial descriptors
rather than by a reviewer's memory: nothing the server sends names a deck, an rng or a seed, and
nothing a client sends carries a card, a stack or a pot.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolPayloadTest.kt` | create |

Read, do not modify: `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDescriptors.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/card/CardSerializer.kt` (for the exact serial name
a `Card` carries).

## Scope

- A test class only. No main-source change: a failure here means a message type is wrong, and
  fixing it is a new ticket.
- One private recursive walk in the test file:

  ```kotlin
  private fun walk(descriptor: SerialDescriptor, seen: MutableSet<String> = mutableSetOf()): List<SerialDescriptor>
  ```

  It returns `descriptor` and everything reachable through `getElementDescriptor(i)` for
  `0 until elementsCount`, guarding recursion with `seen += descriptor.serialName` and recursing
  only when `elementsCount > 0` — a primitive descriptor throws if asked for an element. Collect
  element **names** the same way. Use `subtypeDescriptors` from `ProtocolDescriptors.kt` to enter
  each hierarchy; do not re-implement it.
- The forbidden `Card` serial name is exactly `duels.poker.engine.card.Card` — `CardSerializer`
  declares it as a primitive with that name, so this is an exact-match check, not a substring guess.
- The two directions are asserted separately, because they forbid different things. Note in a
  comment at the top of the file that `GameState`, `Deck` and `Rng` carry no `@Serializable` at all,
  so a message declaring one would not compile — these tests exist for the cases the compiler
  cannot catch: an `Int` seed, a `String` holding a card, a copied field.

## Out of scope

- Anything about `PlayerView`'s internals: `PlayerViewSerializationTest` in `poker-engine` already
  pins those, and duplicating it here would leave two places to update.
- The discriminator assertions — `TASK-020210`.
- Runtime redaction: no `PlayerView` is projected here, this ticket reads shapes only.

## Tests

`ProtocolPayloadTest`, JUnit 5, package `duels.poker.server.protocol`.

| Test | Proves |
| --- | --- |
| `noServerMessageNamesADeckAnRngOrASeed` | across every descriptor reachable from every `ServerMessage` subtype, no element name equals `deck`, `rng`, `seed`, `holeCardsOf` or `deckRemaining`, ignoring case |
| `noServerMessageCarriesAStateDeckOrRngType` | no reachable descriptor's `serialName` contains `GameState`, `Deck` or `Rng` |
| `noClientMessageCarriesACard` | no descriptor reachable from a `ClientMessage` subtype has `serialName == "duels.poker.engine.card.Card"` |
| `noClientMessageNamesAChipOrStateField` | across every descriptor reachable from every `ClientMessage` subtype, no element name equals `stack`, `pot`, `card`, `cards`, `holeCards`, `board`, `view`, `betToMatch` or `seatToAct`, ignoring case |
| `theOnlyStateAServerMessageCarriesIsAPlayerView` | among the six `ServerMessage` subtypes, exactly one (`Snapshot`) has an element whose descriptor `serialName` ends with `PlayerView`, and that subtype has exactly one element |
| `aClientMessagesOnlyEngineTypeIsAPlayerAction` | for every `ClientMessage` subtype, every element descriptor whose `serialName` starts with `duels.poker.engine` is `PlayerAction` or one of its subtypes — an intent, never a fact |

## Acceptance criteria

- [ ] `ProtocolPayloadTest.noServerMessageNamesADeckAnRngOrASeed` passes
- [ ] `ProtocolPayloadTest.noServerMessageCarriesAStateDeckOrRngType` passes
- [ ] `ProtocolPayloadTest.noClientMessageCarriesACard` passes
- [ ] `ProtocolPayloadTest.noClientMessageNamesAChipOrStateField` passes
- [ ] `ProtocolPayloadTest.theOnlyStateAServerMessageCarriesIsAPlayerView` passes
- [ ] `ProtocolPayloadTest.aClientMessagesOnlyEngineTypeIsAPlayerAction` passes
- [ ] No file outside the one in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
