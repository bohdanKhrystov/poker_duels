---
schema: 2
id: TASK-010825
title: DuelOutcome and MatchFinished are serializable
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [engine, serialization, duel, events]
depends_on: [TASK-010725, TASK-010813]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchEventSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The end of a duel survives being written down: the outcome and the match event that carries it
round-trip through JSON.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchEvent.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/MatchEventSerializationTest.kt` | create |

## Scope

- `@Serializable` on `DuelOutcome`. `outcomeOf`, in the same file, is a function and is untouched.
- `@Serializable` on the sealed interface `MatchEvent` and on `MatchFinished`, the latter with
  `@SerialName("MatchFinished")`. The match hierarchy gets a discriminator of its own, in its own
  sequence space, exactly as `ADR-0009` describes — it is never mixed into a `GameEvent` log.
- `MatchEvent.version` stays a property with a default and no backing field, so it is not written
  to the wire, matching `GameEvent`. One test below pins this.

## Out of scope

- `MatchState`: live state, never a log field.
- `MatchLog` — `TASK-010826`.
- Adding any `MatchEvent` subtype.

## Tests

`MatchEventSerializationTest`, JUnit 5, package `duels.poker.engine.duel`.

| Test | Proves |
| --- | --- |
| `aDuelOutcomeRoundTrips` | `DuelOutcome(1, 12, listOf(0, 20000))` decodes back equal |
| `aDrawRoundTripsWithANullWinner` | `DuelOutcome(null, 40, listOf(500, 500))` decodes back equal and `isDraw` is still true |
| `matchFinishedRoundTripsThroughTheParentSerializer` | a `MatchFinished` encoded and decoded through `MatchEvent.serializer()` comes back equal |
| `theDiscriminatorIsMatchFinished` | that encoding contains `"type":"MatchFinished"` and no package path |
| `theSchemaVersionIsNotWrittenToTheWire` | the encoding contains no `version` member |
| `decodingAnOutcomeWithThreeStacksFails` | `"finalStacks":[1,2,3]` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `MatchEventSerializationTest.aDuelOutcomeRoundTrips` passes
- [ ] `MatchEventSerializationTest.aDrawRoundTripsWithANullWinner` passes
- [ ] `MatchEventSerializationTest.matchFinishedRoundTripsThroughTheParentSerializer` passes
- [ ] `MatchEventSerializationTest.theDiscriminatorIsMatchFinished` passes
- [ ] `MatchEventSerializationTest.theSchemaVersionIsNotWrittenToTheWire` passes
- [ ] `MatchEventSerializationTest.decodingAnOutcomeWithThreeStacksFails` passes
- [ ] `DuelOutcomeTest`, `OutcomeOfTest` and `MatchEventTest` still pass unchanged
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
