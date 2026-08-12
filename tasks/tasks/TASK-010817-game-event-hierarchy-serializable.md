---
schema: 2
id: TASK-010817
title: The whole GameEvent hierarchy serialises polymorphically
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, serialization, log]
depends_on: [TASK-010816]
verify:
  - ./gradlew :poker-engine:test --tests '*GameEventSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `List<GameEvent>` — the thing a hand log is made of — writes and reads back as itself, every
one of the seventeen subtypes included.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/GameEvent.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameEventSerializationTest.kt` | create |

## Scope

- `@Serializable` on the sealed interface `GameEvent` and on the four events declared in this file
  — `HandStarted`, `BlindPosted`, `HoleCardsDealt`, `ActionOn` — each with `@SerialName` carrying
  its own simple name, as `TASK-010816` did for the other thirteen.
- Once the parent is annotated, the hierarchy nests: `GameEvent.serializer()` dispatches to
  `BettingEvent` and `DealerEvent`, which dispatch to their own subtypes. Nothing in
  `BettingEvents.kt` or `DealerEvents.kt` needs another edit.
- `version` stays a property with a default and no backing field, so it is **not** written to the
  wire — `EVENT_SCHEMA_VERSION` is a fact about the build, and the log carries its own version
  (`TASK-010819`). One test below pins this.

## Out of scope

- `StateProjection`, `DefaultPokerEngineContractTest`, `ContractDetectsDriftTest`: `ADR-0009` chose
  a separate `MatchEvent` hierarchy precisely so no new `GameEvent` subtype exists and the
  exhaustive `when` is never touched. This ticket adds no subtype.
- `HandLog` — `TASK-010818`.

## Tests

`GameEventSerializationTest`, JUnit 5, package `duels.poker.engine.game`. Round-trip through
`ListSerializer(GameEvent.serializer())`.

| Test | Proves |
| --- | --- |
| `everySubtypeRoundTripsThroughTheParentSerializer` | a list holding one instance of each of the seventeen `GameEvent` subtypes decodes back equal, element for element |
| `theListIsNotShorterThanTheHierarchy` | the list under test has exactly 17 entries and 17 distinct runtime classes, so the test above cannot pass vacuously |
| `theSchemaVersionIsNotWrittenToTheWire` | the encoding of `ActionOn(3, 1)` is exactly `{"type":"ActionOn","sequence":3,"seat":1}` and contains no `version` member |
| `decodingAnUnknownEventTypeFails` | `{"type":"HandExploded","sequence":0}` throws `SerializationException` |

## Acceptance criteria

- [ ] `GameEventSerializationTest.everySubtypeRoundTripsThroughTheParentSerializer` passes
- [ ] `GameEventSerializationTest.theListIsNotShorterThanTheHierarchy` passes
- [ ] `GameEventSerializationTest.theSchemaVersionIsNotWrittenToTheWire` passes
- [ ] `GameEventSerializationTest.decodingAnUnknownEventTypeFails` passes
- [ ] `GameEventTest` and `StateProjectionTest` still pass unchanged — no subtype is added and no
      projection branch changes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
