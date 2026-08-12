---
schema: 2
id: TASK-010706
title: The two duel end conditions as a sealed type
type: task
status: ready
parent: STORY-0107
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, duel]
depends_on: [TASK-010616]
verify:
  - ./gradlew :poker-engine:test --tests '*EndConditionTest'
  - ./gradlew :poker-engine:check
---

## Goal

Both answers `DEC-001` is choosing between are expressible as data, so choosing one later is a
configuration change and not an engine change.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/EndCondition.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/EndConditionTest.kt` | create |

Read `docs/duel-rules.md` Part 2 ("Default: freezeout" and "Alternative under consideration") and
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt` for the nested sealed shape
this file copies. Modify neither.

## Scope

- `public sealed interface EndCondition`, with its two members nested inside it exactly as
  `PlayerAction` nests its own:
  - `public data object Freezeout : EndCondition` — play until one seat holds every chip.
  - `public data class FixedHands(val hands: Int) : EndCondition`, `init` requiring `hands >= 1`
    with a message naming the value — a set number of hands, most chips wins.
- KDoc says these are the two candidates `docs/duel-rules.md` records, that neither is chosen
  here because `DEC-001` is open, and that this type is what makes the answer cheap.

## Out of scope

- Evaluating an end condition against a match — `TASK-010712`.
- Deciding `DEC-001`. This ticket makes the decision cheap; it does not pre-empt it.

## Tests

`EndConditionTest`

| Test | Proves |
| --- | --- |
| `fixedHandsCarriesItsHandCount` | `EndCondition.FixedHands(25).hands == 25` |
| `fixedHandsRejectsANonPositiveCount` | `FixedHands(0)` and `FixedHands(-1)` each throw `IllegalArgumentException` |
| `bothConditionsShareTheSealedType` | a local `when` over an `EndCondition` with a branch for `Freezeout` and one for `is FixedHands` compiles with no `else` and returns the right branch for each |

## Acceptance criteria

- [ ] `EndConditionTest.fixedHandsCarriesItsHandCount` passes
- [ ] `EndConditionTest.fixedHandsRejectsANonPositiveCount` passes
- [ ] `EndConditionTest.bothConditionsShareTheSealedType` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
