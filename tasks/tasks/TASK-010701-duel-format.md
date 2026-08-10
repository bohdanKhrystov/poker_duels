---
id: TASK-010701
title: DuelFormat — stacks, blind schedule, end condition
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
labels: [engine, duel]
depends_on: [TASK-010603]
---

## Goal

The rules of a duel expressed as data, so that changing what a duel *is* never means changing
the engine.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — **`DEC-001` is still open.** This ticket
  exists precisely because the answer is not settled; the type must make either answer a
  configuration change.

## Scope

- `DuelFormat`: starting stack, blind schedule, end condition.
- `BlindSchedule`: levels of small/big blind with the hand count at which each begins, and a
  rule for extending past the last defined level.
- `EndCondition` as a sealed type with two cases:
  - `Freezeout` — play until one seat holds every chip,
  - `FixedHands(count)` — play a set number of hands, most chips wins.
- `DuelFormat.default` — the freezeout described in the rules document.
- No amount from the default format may appear as a literal anywhere else in the engine.

## Out of scope

- Applying the format — `TASK-010702`.
- Deciding `DEC-001`. This ticket makes the decision cheap to make later, which is the whole
  point; it does not pre-empt it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../duel/DuelFormat.kt` | create |
| `poker-engine/src/main/kotlin/.../duel/BlindSchedule.kt` | create |
| `poker-engine/src/test/kotlin/.../duel/DuelFormatTest.kt` | create |

## Acceptance criteria

- [ ] The default format matches the table in `duel-rules.md` exactly.
- [ ] A blind schedule returns the correct level for any hand number, including beyond the last
      defined level.
- [ ] Both `Freezeout` and `FixedHands` are expressible and constructible.
- [ ] A schedule with non-increasing blinds or a non-positive stack is rejected at construction.
- [ ] Grepping the engine for `50`, `100` or `10000` finds them only in `DuelFormat.default`.

## Tests

- `DuelFormatTest` — level lookup at boundaries and beyond, invalid schedules, both end
  conditions.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
