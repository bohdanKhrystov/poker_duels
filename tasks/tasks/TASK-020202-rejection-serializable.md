---
schema: 2
id: TASK-020202
title: Make Rejection serializable with explicit discriminators
type: task
status: ready
parent: STORY-0202
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, serialization, protocol]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*RejectionSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `Rejection` can cross the socket, so `STORY-0202`'s reject message can carry the engine's own
vocabulary instead of inventing a second one.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Rejection.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RejectionSerializationTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt` (the
annotation pattern to copy exactly),
`poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerActionSerializationTest.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/DomainImmutabilityTest.kt` (read only to see
that it is unaffected — see Scope).

## Scope

- `@Serializable` on the `Rejection` sealed interface and on each of its five members, each with an
  explicit `@SerialName` equal to its simple name: `NotYourTurn`, `ActionNotAllowed`,
  `AmountTooSmall`, `AmountTooLarge`, `HandComplete`. Explicit, not inferred, so renaming the
  Kotlin class cannot silently change the wire format.
- `ActionType` is an enum and needs no annotation; `Set<ActionType>` serializes as a JSON array.
- Imports added: `kotlinx.serialization.SerialName`, `kotlinx.serialization.Serializable`. No new
  dependency — `:poker-engine:check` runs `checkNoDependencies` and it must stay green.
- Not a single property, `require`, or KDoc sentence about poker changes. Annotations only, plus
  one KDoc line on the interface noting that these values are sent to clients verbatim.
- `DomainImmutabilityTest` covers `Rejection.NotYourTurn`, `ActionNotAllowed`, `AmountTooSmall` and
  `AmountTooLarge` and is **not** in this ticket's budget: `@Serializable` adds no instance field
  and no setter, and `PlayerAction.Fold` sits in the same list already annotated, which is the
  proof. If that test fails, stop and report rather than editing it.

## Out of scope

- `LegalActions` — `TASK-020203`.
- Any protocol type in `poker-server` — `TASK-020206` is the consumer.
- Changing `Rejection`'s members, or adding one.

## Tests

`RejectionSerializationTest`, JUnit 5, package `duels.poker.engine.game`, with
`private val json = Json { prettyPrint = false }`.

| Test | Proves |
| --- | --- |
| `everyRejectionRoundTripsThroughTheParentSerializer` | encoding `listOf(NotYourTurn(1), ActionNotAllowed(ActionType.BET, setOf(ActionType.FOLD, ActionType.CALL)), AmountTooSmall(50, 100), AmountTooLarge(9000, 1000), HandComplete)` with `ListSerializer(Rejection.serializer())` and decoding it back returns an equal list |
| `nullSeatToActSurvives` | `NotYourTurn(null)` round-trips with `seatToAct == null` |
| `theDiscriminatorsAreTheSimpleNames` | the encoded string of each of the five values contains `"type":"NotYourTurn"`, `"type":"ActionNotAllowed"`, `"type":"AmountTooSmall"`, `"type":"AmountTooLarge"` and `"type":"HandComplete"` respectively |

## Acceptance criteria

- [ ] `RejectionSerializationTest.everyRejectionRoundTripsThroughTheParentSerializer` passes
- [ ] `RejectionSerializationTest.nullSeatToActSurvives` passes
- [ ] `RejectionSerializationTest.theDiscriminatorsAreTheSimpleNames` passes
- [ ] `RejectionTest`, `ActionValidationTest`, `EngineResultTest` and `DomainImmutabilityTest` are
      not modified and still pass
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
