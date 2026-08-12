---
schema: 2
id: TASK-010710
title: Fold a finished hand back into the match and pass the button
type: task
status: done
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, duel, rules, chips]
depends_on: [TASK-010709]
verify:
  - ./gradlew :poker-engine:test --tests '*RecordHandTest'
  - ./gradlew :poker-engine:test --tests '*StartNextHandTest'
  - ./gradlew :poker-engine:check
---

## Goal

A settled hand becomes the next `MatchState`: stacks carry over, the hand is counted, the button
changes seats.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/RecordHandTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` (`isHandOver`,
`chipsInPlay`), `.../game/HeadsUpOrder.kt` (`otherSeat`) and
`poker-engine/src/test/kotlin/duels/poker/engine/game/GameStates.kt` (the `handState` and `seats`
fixtures, both `internal` and reachable from this package). Modify none of them.

## Scope

- Append `public fun recordHand(match: MatchState, finished: GameState): MatchState` to
  `MatchProgression.kt`. `startNextHand` is not touched — no signature change, no behaviour
  change — so `StartNextHandTest` keeps passing exactly as merged.
- Three guards, each with a message naming the offending numbers:
  - `finished.isHandOver` — an unsettled hand still has chips in a pot that belong to nobody.
  - `finished.handNumber == match.nextHandNumber` — a match records the hand it dealt, in order.
  - `finished.chipsInPlay == match.chips` — chips are conserved across the hand boundary, the
    invariant `docs/duel-rules.md` requires.
- Returns `match.copy(handsPlayed = finished.handNumber, stacks = finished.seats.map { it.stack }, buttonSeat = otherSeat(match.buttonSeat))`.
- KDoc notes that no blind level is stored anywhere, so the next hand's level follows from the
  new `handsPlayed` alone: a level cannot change part-way through a hand.

## Out of scope

- Deciding whether the match is now over — `TASK-010712`.
- Any event: this returns a value, and whether the end of a match is also an event waits on
  `DEC-005` (`TASK-010717`).

## Tests

`RecordHandTest`, building finished hands as
`handState(seats(a, b)).copy(street = Street.COMPLETE, seatToAct = null, handNumber = n)` and
matches from `MatchState.start(DuelFormat.DEFAULT)` plus `copy`

| Test | Proves |
| --- | --- |
| `carriesTheFinalStacksIntoTheMatch` | a hand finishing 13_000/7_000 gives `match.stacks == listOf(13_000, 7_000)` |
| `countsTheHandAndPassesTheButton` | `handsPlayed` goes 0 → 1 and `buttonSeat` goes 0 → 1; recording the next hand puts it back on 0 |
| `theNextBlindLevelFollowsTheNewHandCount` | after recording hand 10, `match.blinds == BlindLevel(75, 150)` |
| `refusesAHandThatIsNotOver` | a `finished` still on `Street.PREFLOP` throws `IllegalArgumentException` |
| `refusesAHandOutOfSequence` | `handNumber = 2` against a match with `handsPlayed = 0` throws `IllegalArgumentException` |
| `refusesAHandThatChangedTheChipTotal` | final stacks summing to 19_999 against a 20_000-chip match throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `RecordHandTest.carriesTheFinalStacksIntoTheMatch` passes
- [ ] `RecordHandTest.countsTheHandAndPassesTheButton` passes
- [ ] `RecordHandTest.theNextBlindLevelFollowsTheNewHandCount` passes
- [ ] `RecordHandTest.refusesAHandThatIsNotOver` passes
- [ ] `RecordHandTest.refusesAHandOutOfSequence` passes
- [ ] `RecordHandTest.refusesAHandThatChangedTheChipTotal` passes
- [ ] All six tests in `StartNextHandTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
