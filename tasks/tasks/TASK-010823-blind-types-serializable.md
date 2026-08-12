---
schema: 2
id: TASK-010823
title: The blind types are serializable
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [engine, serialization, duel]
depends_on: [TASK-010813]
verify:
  - ./gradlew :poker-engine:test --tests '*BlindSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A blind level and a blind ladder survive a round trip through JSON, so a stored duel can state the
schedule it was played under.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindLevel.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindSchedule.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/BlindSerializationTest.kt` | create |

## Scope

- `@Serializable` on `BlindLevel` and on `BlindSchedule`, plus the import in each file. Nothing
  else changes — the `init` guards stay, and they run on decode, which is what stops a descending
  ladder from being read back.
- No `@SerialName`: neither type is polymorphic, so no discriminator is written and the class name
  never reaches the wire.

## Out of scope

- `DuelFormat` and `EndCondition` — `TASK-010824`.
- `MatchState`: it is live state, never a log field, and is not serialized by anything.

## Tests

`BlindSerializationTest`, JUnit 5, package `duels.poker.engine.duel`.

| Test | Proves |
| --- | --- |
| `aBlindLevelRoundTrips` | `BlindLevel(50, 100)` encodes to `{"smallBlind":50,"bigBlind":100}` and decodes back equal |
| `aScheduleRoundTripsWithItsLevels` | a three-level schedule with `handsPerLevel = 10` decodes back equal |
| `decodingADescendingLadderFails` | JSON whose levels descend throws `IllegalArgumentException` |
| `decodingABlindLevelWithABigBlindBelowTheSmallFails` | `{"smallBlind":100,"bigBlind":50}` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `BlindSerializationTest.aBlindLevelRoundTrips` passes
- [ ] `BlindSerializationTest.aScheduleRoundTripsWithItsLevels` passes
- [ ] `BlindSerializationTest.decodingADescendingLadderFails` passes
- [ ] `BlindSerializationTest.decodingABlindLevelWithABigBlindBelowTheSmallFails` passes
- [ ] `BlindLevelTest` and `BlindScheduleTest` still pass unchanged
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
