---
schema: 2
id: TASK-010708
title: MatchState, what survives between two hands
type: task
status: done
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, duel]
depends_on: [TASK-010707]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchStateTest'
  - ./gradlew :poker-engine:check
---

## Goal

The duel between hands is a value: the format, how many hands have been played, both stacks and
the button.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchState.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/MatchStateTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt`, `.../duel/BlindSchedule.kt`
and the design notes in `tasks/stories/STORY-0107-duel-format-and-match.md`. Modify none of them.

## Scope

- `public data class MatchState(val format: DuelFormat, val handsPlayed: Int, val stacks: List<Int>, val buttonSeat: Int)`.
- `init` requires, each with a message naming the offending value: exactly two stacks, no
  negative stack, `buttonSeat in 0..1`, `handsPlayed >= 0`.
- Derived, all `public val` with KDoc:
  - `nextHandNumber: Int` — `handsPlayed + 1`, the 1-based number of the hand about to be dealt.
  - `blinds: BlindLevel` — `format.blinds.blindsFor(nextHandNumber)`.
  - `chips: Int` — both stacks together.
- `public companion object` with `public fun start(format: DuelFormat, buttonSeat: Int = 0): MatchState`
  — zero hands played and both stacks at `format.startingStack`.
- KDoc says what this deliberately is *not*: it holds no live `GameState`. One hand is a
  `GameState`; `MatchState` is only what survives between hands, per the story's design notes.
  It also says why `blinds` is derived and never stored — a level that is recomputed from
  `nextHandNumber` cannot change in the middle of a hand, which makes that rule true by
  construction instead of by discipline.

## Out of scope

- Dealing a hand or recording a finished one — `TASK-010709` and `TASK-010710`.
- Deciding whether the duel is over — `TASK-010712`.

## Tests

`MatchStateTest`

| Test | Proves |
| --- | --- |
| `startDealsBothSeatsTheFormatStack` | `MatchState.start(DuelFormat.DEFAULT)` has `stacks == listOf(10_000, 10_000)`, `handsPlayed == 0` and `buttonSeat == 0` |
| `theNextHandFollowsTheHandsPlayed` | `handsPlayed = 0` gives `nextHandNumber == 1`; `handsPlayed = 10` gives `11` |
| `theBlindsAreTheScheduleAtTheNextHand` | under `DuelFormat.DEFAULT`, `handsPlayed = 0` gives 50/100, `handsPlayed = 10` gives 75/150, `handsPlayed = 40` gives 200/400 |
| `chipsAreTheTwoStacksTogether` | `stacks = listOf(3_000, 17_000)` gives `chips == 20_000` |
| `rejectsAMalformedMatch` | one stack, a negative stack, `buttonSeat = 2` and `handsPlayed = -1` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `MatchStateTest.startDealsBothSeatsTheFormatStack` passes
- [ ] `MatchStateTest.theNextHandFollowsTheHandsPlayed` passes
- [ ] `MatchStateTest.theBlindsAreTheScheduleAtTheNextHand` passes
- [ ] `MatchStateTest.chipsAreTheTwoStacksTogether` passes
- [ ] `MatchStateTest.rejectsAMalformedMatch` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
