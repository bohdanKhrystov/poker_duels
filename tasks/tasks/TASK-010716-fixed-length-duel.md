---
schema: 2
id: TASK-010716
title: A fixed-length duel plays and is decided on chips
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [engine, duel, tests]
depends_on: [TASK-010715]
verify:
  - ./gradlew :poker-engine:test --tests '*FixedLengthDuelTest'
  - ./gradlew :poker-engine:test --tests '*DuelTerminationTest'
  - ./gradlew :poker-engine:check
---

## Goal

The alternative format `docs/duel-rules.md` records is not just expressible — it plays, end to
end, through the same code as the freezeout.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/FixedLengthDuelTest.kt` | create |

Read `poker-engine/src/test/kotlin/duels/poker/engine/duel/RandomDuelPlayer.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt`, `.../duel/EndCondition.kt`
and `docs/duel-rules.md` Part 2 ("Alternative under consideration"). Modify none of them.

## Scope

- One new test class. No production change, no harness change: the point of the ticket is that
  the fixed-length format needs neither.
- The format under test is
  `DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))`, played over seeds
  `1L..40L`. Three hands keeps the sample fast and makes both endings — the distance and an early
  bust — common enough to assert on.
- Every failure message names its seed.

## Out of scope

- The freezeout — `TASK-010713` to `TASK-010715` cover it.
- Unit-level end-condition cases — `OutcomeOfTest` owns those.

## Tests

`FixedLengthDuelTest`, `@Timeout(60)` on each test

| Test | Proves |
| --- | --- |
| `aFixedLengthDuelNeverPlaysPastItsLimit` | for each seed: `outcome.handsPlayed <= 3` and `hands.size == outcome.handsPlayed` |
| `aFixedLengthDuelIsDecidedOnTheFinalStacks` | for each duel with `handsPlayed == 3`: the winner is the seat with the strictly larger final stack, and `isDraw` is true exactly when the stacks are equal |
| `aFixedLengthDuelEndsEarlyWhenASeatIsBroke` | every duel with `handsPlayed < 3` has a final stack of `0`, and its winner is the other seat |
| `theSampleContainsBothEndings` | across the 40 seeds at least one duel reaches hand 3 and at least one ends before it, so neither test above is vacuous |

## Acceptance criteria

- [ ] `FixedLengthDuelTest.aFixedLengthDuelNeverPlaysPastItsLimit` passes
- [ ] `FixedLengthDuelTest.aFixedLengthDuelIsDecidedOnTheFinalStacks` passes
- [ ] `FixedLengthDuelTest.aFixedLengthDuelEndsEarlyWhenASeatIsBroke` passes
- [ ] `FixedLengthDuelTest.theSampleContainsBothEndings` passes
- [ ] All three tests in `DuelTerminationTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
