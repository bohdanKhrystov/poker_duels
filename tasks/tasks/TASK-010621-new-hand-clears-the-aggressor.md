---
schema: 2
id: TASK-010621
title: A new hand starts with no last aggressor
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, state]
depends_on: [TASK-010618]
verify:
  - ./gradlew :poker-engine:test --tests '*StateProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

`HandStarted` resets `lastAggressor` to `null` along with everything else it resets, so no seat
carries aggression from the previous hand into the next one.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StateProjection.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StateProjectionTest.kt` | modify |

Read `GameState.kt`. Do not modify it.

## Scope

- Add `lastAggressor = null` to the named arguments of the `is HandStarted ->` branch's `copy`,
  beside `seatToAct = null`. One line.
- Add one additive test to `StateProjectionTest`; do not touch
  `handStartedResetsToAFreshPreflopPosition`, which stays as it is.
- This is hygiene with a due date rather than a bug fix today — a showdown always has a dealt
  street behind it, so `TASK-010620` would clear a stale value anyway. It matters from
  `STORY-0107` on, where one match state runs hand after hand and `HandStarted` is the only reset
  point.

## Out of scope

- `StreetDealt` clearing the field — `TASK-010620`.
- Any change to `startHand`, which builds its opening state with named arguments and gets the
  field's default.

## Tests

`StateProjectionTest`

| Test | Proves |
| --- | --- |
| `handStartedClearsTheLastAggressor` | applying `HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))` to a state built with `copy(lastAggressor = 1)` leaves `lastAggressor == null` |

## Acceptance criteria

- [ ] `StateProjectionTest.handStartedClearsTheLastAggressor` passes
- [ ] Every pre-existing test in `StateProjectionTest` passes with no change to its body
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
