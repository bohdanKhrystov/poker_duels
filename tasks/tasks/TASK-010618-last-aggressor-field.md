---
schema: 2
id: TASK-010618
title: Carry the last aggressor on GameState
type: task
status: ready
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, state]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*GameStateTest'
  - ./gradlew :poker-engine:check
---

## Goal

`GameState` carries `lastAggressor: Int?` — the seat that last bet or raised on the current
street — so a showdown can order its reveals without re-deriving the betting history.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameStateTest.kt` | modify |

## Scope

- Add `val lastAggressor: Int? = null` as the **last** constructor parameter of `GameState`,
  after `rng`. It must have a default: every existing construction site uses named arguments and
  none of them may change in this ticket.
- Add to `init`: `require(lastAggressor == null || lastAggressor in SEAT_INDICES)` with a message
  naming the bad value, in the style of the neighbouring requires.
- KDoc the property: the seat that last bet or raised **on the current street**, or `null` when
  the street has seen no aggression. Say what it is for — reveal order at showdown, per
  [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md) — and that this ticket only
  declares it: `TASK-010619` sets it, `TASK-010620` clears it on a new street.
- Two tests in `GameStateTest`, using its existing `preflopState()` helper.

## Out of scope

- Setting the field from betting events — `TASK-010619`.
- Clearing it when a street is dealt — `TASK-010620`, or on a new hand — `TASK-010621`.
- Any change to `GameStates.kt`'s `handState()` fixture: the default covers it.

## Tests

`GameStateTest`

| Test | Proves |
| --- | --- |
| `lastAggressorDefaultsToNull` | a state built without the argument has `lastAggressor == null` |
| `rejectsALastAggressorThatIsNotASeat` | `preflopState().copy(lastAggressor = 2)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `GameStateTest.lastAggressorDefaultsToNull` passes
- [ ] `GameStateTest.rejectsALastAggressorThatIsNotASeat` passes
- [ ] Every other test in `GameStateTest` passes with no change to its body
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
