---
schema: 2
id: TASK-010712
title: Decide whether a match is over, and who won it
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, duel, rules]
depends_on: [TASK-010708, TASK-010711]
verify:
  - ./gradlew :poker-engine:test --tests '*OutcomeOfTest'
  - ./gradlew :poker-engine:test --tests '*DuelOutcomeTest'
  - ./gradlew :poker-engine:check
---

## Goal

Given a match between hands, the engine says either "still running" or exactly who won and how.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/OutcomeOfTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchState.kt`, `.../duel/EndCondition.kt`
and `docs/duel-rules.md` Part 2. Modify none of them.

## Scope

- Append `public fun outcomeOf(match: MatchState): DuelOutcome?` to `DuelOutcome.kt` — the file
  stays named after its single class, which is what ktlint's `standard:filename` rule requires.
  The `DuelOutcome` data class itself is not touched, so `DuelOutcomeTest` keeps passing as
  merged.
- `null` means the duel is still running. A returned `DuelOutcome` always carries
  `match.handsPlayed` and `match.stacks`.
- A seat with zero chips ends the duel under **every** end condition, and this is checked first:
  no further hand can be dealt to a seat with no chips, so a match in that state has ended
  whatever its end condition says. The other seat is the winner.
- `EndCondition.Freezeout` — that is the only ending; a freezeout can never be a draw.
- `EndCondition.FixedHands(n)` — also ends once `match.handsPlayed >= n`. The winner is the seat
  with the larger final stack; **equal stacks are a draw**, `winner = null`, never an arbitrary
  pick.
- The `when` over `EndCondition` is exhaustive with no `else`.
- KDoc explains the ordering of the two checks and why a broke seat short-circuits a fixed-length
  duel.

## Out of scope

- Emitting an event when a match ends — blocked on `DEC-005`, `TASK-010717`.
- Refusing actions after a match is over: nothing in the engine accepts a match-level action, and
  `startNextHand` already refuses a seat with no chips.
- Playing the duel that produces these states — `TASK-010713`.

## Tests

`OutcomeOfTest`, building matches from `MatchState.start(DuelFormat.DEFAULT)` and `copy`, with the
fixed-length cases using `DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(25))`

| Test | Proves |
| --- | --- |
| `aFreezeoutHasNoOutcomeWhileBothSeatsHaveChips` | `handsPlayed = 7`, stacks 5_000/15_000 → `outcomeOf` is `null` |
| `aFreezeoutEndsWhenOneSeatHoldsEveryChip` | `handsPlayed = 31`, stacks 0/20_000 → `winner == 1`, `handsPlayed == 31`, `finalStacks == listOf(0, 20_000)`, `isDraw` false |
| `aFixedLengthDuelRunsToItsLastHand` | `FixedHands(25)`: `handsPlayed = 24` with 9_000/11_000 is `null`; `handsPlayed = 25` gives `winner == 1` |
| `aLevelFixedLengthDuelIsADraw` | `FixedHands(25)`, `handsPlayed = 25`, stacks 10_000/10_000 → `winner` is `null` and `isDraw` is true |
| `aFixedLengthDuelStillEndsWhenASeatIsBroke` | `FixedHands(25)`, `handsPlayed = 6`, stacks 20_000/0 → `winner == 0` |

## Acceptance criteria

- [ ] `OutcomeOfTest.aFreezeoutHasNoOutcomeWhileBothSeatsHaveChips` passes
- [ ] `OutcomeOfTest.aFreezeoutEndsWhenOneSeatHoldsEveryChip` passes
- [ ] `OutcomeOfTest.aFixedLengthDuelRunsToItsLastHand` passes
- [ ] `OutcomeOfTest.aLevelFixedLengthDuelIsADraw` passes
- [ ] `OutcomeOfTest.aFixedLengthDuelStillEndsWhenASeatIsBroke` passes
- [ ] All three tests in `DuelOutcomeTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
