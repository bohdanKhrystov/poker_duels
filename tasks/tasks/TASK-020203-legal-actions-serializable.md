---
schema: 2
id: TASK-020203
title: Make LegalActions serializable, with its defaults on the wire
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
  - ./gradlew :poker-engine:test --tests '*LegalActionsSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The seat on turn can be sent `LegalActions` over the socket, and the four amount fields arrive even
when they hold their default of `0`.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/LegalActions.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/LegalActionsSerializationTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt` (a
serializable type with a defaulted property),
`poker-engine/src/test/kotlin/duels/poker/engine/game/DomainImmutabilityTest.kt` (read only to see
that it is unaffected — see Scope).

## Scope

- One `@Serializable` annotation on `LegalActions` and the `kotlinx.serialization.Serializable`
  import. Nothing else in the file changes: not a default, not a `require`, not the `allows`
  helper, not `none`.
- `LegalActions` is a single class, not a hierarchy, so it carries no `@SerialName` and no
  discriminator.
- Add one KDoc paragraph recording the trap for whoever reads this next: **all four amounts have a
  default of `0`, so a `Json` without `encodeDefaults = true` omits them and a client reading the
  frame sees `callTo` absent rather than zero.** The protocol's own `Json` sets it
  (`TASK-020201`); this note is why it must stay set.
- `checkNoDependencies` must stay green — no new dependency, `:poker-engine:check` proves it.
- `DomainImmutabilityTest` lists `LegalActions` and is **not** in this ticket's budget:
  `@Serializable` adds no instance field and no setter, and the already-annotated `PlayerAction`
  members in the same list are the proof. If it fails, stop and report rather than editing it.

## Out of scope

- `Rejection` — `TASK-020202`.
- Computing a `LegalActions` from a state: `legalActions(state)` in `BettingRules.kt` already does
  it and is not touched here.
- Any protocol type in `poker-server` — `TASK-020206` is the consumer.

## Tests

`LegalActionsSerializationTest`, JUnit 5, package `duels.poker.engine.game`, with
`private val json = Json { encodeDefaults = true; prettyPrint = false }`.

| Test | Proves |
| --- | --- |
| `aFacingBetSetRoundTrips` | `LegalActions(0, setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), callTo = 100, minRaiseTo = 200, allInTo = 1000)` encodes and decodes to an equal value |
| `anOpeningSetRoundTrips` | `LegalActions(1, setOf(ActionType.CHECK, ActionType.BET), minBetTo = 100, allInTo = 1000)` encodes and decodes to an equal value |
| `theEmptySetRoundTrips` | `LegalActions.none(0)` encodes and decodes to an equal value |
| `defaultAmountsAreWrittenWhenDefaultsAreEncoded` | the encoded string of `LegalActions.none(0)` contains `"callTo":0`, `"minBetTo":0`, `"minRaiseTo":0` and `"allInTo":0` |
| `defaultAmountsAreOmittedWithoutEncodeDefaults` | the same value encoded with `Json { prettyPrint = false }` contains none of those four names — the trap, pinned so nobody removes the protocol's `encodeDefaults` and calls it tidying |

## Acceptance criteria

- [ ] `LegalActionsSerializationTest.aFacingBetSetRoundTrips` passes
- [ ] `LegalActionsSerializationTest.anOpeningSetRoundTrips` passes
- [ ] `LegalActionsSerializationTest.theEmptySetRoundTrips` passes
- [ ] `LegalActionsSerializationTest.defaultAmountsAreWrittenWhenDefaultsAreEncoded` passes
- [ ] `LegalActionsSerializationTest.defaultAmountsAreOmittedWithoutEncodeDefaults` passes
- [ ] `LegalActionsTest`, `BettingRulesTest` and `DomainImmutabilityTest` are not modified and still
      pass
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
