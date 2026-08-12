---
schema: 2
id: TASK-010824
title: DuelFormat and its end condition are serializable
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [engine, serialization, duel]
depends_on: [TASK-010823]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelFormatSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The configuration a duel was played under — starting stack, ladder and end condition — round-trips
through JSON, both end conditions included.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/EndCondition.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelFormatSerializationTest.kt` | create |

## Scope

- `@Serializable` on the sealed interface `EndCondition` and on both of its members —
  `data object Freezeout` and `data class FixedHands` — each with `@SerialName` carrying its own
  simple name, `"Freezeout"` and `"FixedHands"`, for the same wire-contract reason as every other
  sealed hierarchy in this story.
- `@Serializable` on `DuelFormat`. Its `init` guard stays and runs on decode.
- `DuelFormat.DEFAULT` is untouched: it is a value, not a wire concern.

## Out of scope

- `BlindLevel` and `BlindSchedule` — already done in `TASK-010823`.
- `DuelOutcome` and `MatchEvent` — `TASK-010825`.
- Deciding what the default format *is*: `DEC-001` is still open and this ticket changes no value.

## Tests

`DuelFormatSerializationTest`, JUnit 5, package `duels.poker.engine.duel`.

| Test | Proves |
| --- | --- |
| `theDefaultFormatRoundTrips` | `DuelFormat.DEFAULT` decodes back equal, five levels and all |
| `freezeoutEncodesAsItsShortName` | the encoding of `EndCondition.Freezeout` through `EndCondition.serializer()` contains `"type":"Freezeout"` and no package path |
| `fixedHandsCarriesItsHandCount` | `EndCondition.FixedHands(40)` round-trips and its encoding contains `"hands":40` |
| `aFixedLengthFormatRoundTrips` | a `DuelFormat` with `FixedHands(20)` decodes back equal |
| `decodingAFormatWhoseStackIsBelowTheBigBlindFails` | JSON with `startingStack` below the first big blind throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelFormatSerializationTest.theDefaultFormatRoundTrips` passes
- [ ] `DuelFormatSerializationTest.freezeoutEncodesAsItsShortName` passes
- [ ] `DuelFormatSerializationTest.fixedHandsCarriesItsHandCount` passes
- [ ] `DuelFormatSerializationTest.aFixedLengthFormatRoundTrips` passes
- [ ] `DuelFormatSerializationTest.decodingAFormatWhoseStackIsBelowTheBigBlindFails` passes
- [ ] `DuelFormatTest` and `EndConditionTest` still pass unchanged
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
