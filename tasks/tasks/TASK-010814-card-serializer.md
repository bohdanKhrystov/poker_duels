---
schema: 2
id: TASK-010814
title: A card serialises as its own notation
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [engine, serialization, log]
depends_on: [TASK-010813]
verify:
  - ./gradlew :poker-engine:test --tests '*CardSerializerTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `Card` on the wire is `"As"`, not an integer nobody can read and not the internal code the
class documents as an implementation detail.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/CardSerializer.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/CardSerializerTest.kt` | create |

## Scope

- `CardSerializer.kt`, a public object in the existing package `duels.poker.engine.card`,
  serialising a card as a primitive string through the notation `Card` already parses and prints:

  ```kotlin
  public object CardSerializer : KSerializer<Card> {
      override val descriptor: SerialDescriptor =
          PrimitiveSerialDescriptor("duels.poker.engine.card.Card", PrimitiveKind.STRING)

      override fun serialize(encoder: Encoder, value: Card) {
          encoder.encodeString(value.toString())
      }

      override fun deserialize(decoder: Decoder): Card = Card.parse(decoder.decodeString())
  }
  ```

- `Card.kt`: one annotation on the class, `@Serializable(with = CardSerializer::class)`, above the
  existing `@JvmInline`, plus its import. Nothing else in the file changes — not the private
  constructor, not `all`, not `parse`.
- Why a custom serializer rather than plain `@Serializable`: the integer encoding is documented in
  `Card`'s own KDoc as an implementation detail, and a log full of `37` would bind every future
  reader to it. `toString`/`parse` are already a tested round trip.

## Out of scope

- Annotating any event, action or log type — `TASK-010815` onwards.
- `Rank`, `Suit` or `Deck`: no log carries them, and `Deck` must never reach a log.
- Changing `Card.parse`, `Card.toString` or the order of `Card.all`, which is contractual.

## Tests

`CardSerializerTest`, JUnit 5, package `duels.poker.engine.card`. `Card` has a custom serializer,
so pass `CardSerializer` explicitly — for example
`Json.encodeToString(CardSerializer, Card.parse("As"))` — and `ListSerializer(CardSerializer)` for
lists.

| Test | Proves |
| --- | --- |
| `encodesACardAsItsNotation` | `Card.parse("As")` encodes to exactly `"\"As\""` |
| `roundTripsEveryOneOfTheFiftyTwoCards` | for each card in `Card.all`, decoding its encoding gives the same card back |
| `roundTripsInsideAList` | `["7d","Ah"]` decodes through `ListSerializer(CardSerializer)` to the two cards, and re-encodes to the same text |
| `rejectsNotationThatIsNotACard` | decoding `"\"Zz\""` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `CardSerializerTest.encodesACardAsItsNotation` passes
- [ ] `CardSerializerTest.roundTripsEveryOneOfTheFiftyTwoCards` passes
- [ ] `CardSerializerTest.roundTripsInsideAList` passes
- [ ] `CardSerializerTest.rejectsNotationThatIsNotACard` passes
- [ ] `CardTest`, `CardNotationTest` and `CardsTest` still pass unchanged — this ticket adds an
      annotation and changes no behaviour they observe
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
