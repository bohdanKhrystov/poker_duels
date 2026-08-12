---
schema: 2
id: TASK-010816
title: Every betting and dealer event is serializable
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [engine, serialization, log]
depends_on: [TASK-010814]
verify:
  - ./gradlew :poker-engine:test --tests '*EventSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The thirteen events the betting round and the dealer produce round-trip through JSON, cards
included.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingEvents.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/EventSerializationTest.kt` | create |

## Scope

- `@Serializable` on the sealed interface `BettingEvent` and on all six of its data classes;
  `@Serializable` on the sealed interface `DealerEvent` and on all seven of its data classes.
- Each of the thirteen concrete events also carries `@SerialName` with its own simple name —
  `"PlayerFolded"`, `"PlayerChecked"`, `"PlayerCalled"`, `"PlayerBet"`, `"PlayerRaised"`,
  `"PlayerAllIn"`, `"BettingRoundEnded"`, `"StreetDealt"`, `"ShowdownReached"`, `"HandRevealed"`,
  `"UncalledBetReturned"`, `"PotAwarded"`, `"HandFinished"` — for the same reason as
  `TASK-010815`: the discriminator is the wire contract and must not be a package path.
- `GameEvent` itself is **not** annotated here. A `@Serializable` sub-interface whose parent is not
  yet serializable compiles fine, and splitting it this way keeps each ticket inside three files.
  `TASK-010817` annotates the parent.
- The `Street` enum needs no annotation — enums are serializable out of the box.
- No `init` block, field or KDoc changes.

## Out of scope

- `GameEvent.kt` — `TASK-010817`. Do not open it.
- `StateProjection` and the contract suite: `ADR-0009` keeps them out of the serialization work
  entirely, and nothing here adds a subtype.
- `HandLog` — `TASK-010818`.

## Tests

`EventSerializationTest`, JUnit 5, package `duels.poker.engine.game`. Encode through the two
sub-interface serializers, `BettingEvent.serializer()` and `DealerEvent.serializer()`. Build cards
with `Card.parse` — a flop needs three, a revealed hand exactly two.

| Test | Proves |
| --- | --- |
| `everyBettingEventRoundTrips` | all six betting events, encoded and decoded through `BettingEvent.serializer()`, come back equal |
| `everyDealerEventRoundTrips` | all seven dealer events, encoded and decoded through `DealerEvent.serializer()`, come back equal |
| `cardsInsideAnEventAreWrittenAsNotation` | the encoding of `StreetDealt(4, Street.FLOP, listOf(Card.parse("Ah"), Card.parse("Kd"), Card.parse("2c")))` contains `["Ah","Kd","2c"]` |
| `theDiscriminatorIsTheShortName` | the encoding of `PotAwarded` contains `"type":"PotAwarded"` and does not contain `duels.poker.engine` |
| `decodingAnEventWithABadFieldFails` | a `PlayerBet` JSON with `"seat":5` throws `IllegalArgumentException` — the `init` guard runs on decode |

## Acceptance criteria

- [ ] `EventSerializationTest.everyBettingEventRoundTrips` passes
- [ ] `EventSerializationTest.everyDealerEventRoundTrips` passes
- [ ] `EventSerializationTest.cardsInsideAnEventAreWrittenAsNotation` passes
- [ ] `EventSerializationTest.theDiscriminatorIsTheShortName` passes
- [ ] `EventSerializationTest.decodingAnEventWithABadFieldFails` passes
- [ ] `BettingEventsTest`, `DealerEventsTest` and `SettlementEventsTest` still pass unchanged —
      this ticket adds annotations and changes no behaviour they observe
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
