---
schema: 2
id: TASK-010607
title: A fold awards the pot and ends the hand
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, rules, chips]
depends_on: [TASK-010606]
verify:
  - ./gradlew :poker-engine:test --tests '*FoldSettlementTest'
  - ./gradlew :poker-engine:test --tests '*FoldEndsTheHandTest'
  - ./gradlew :poker-engine:check
---

## Goal

The hand a player folds no longer stops with the chips stranded in the pot: the survivor is paid
and the hand reaches `COMPLETE`.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/FoldEndsTheHandTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/FoldSettlementTest.kt` | create |

Read `Settlement.kt`, `DealerEvents.kt`, `HeadsUpOrder.kt`, `GameStates.kt`. Modify none of them.

## Scope

- `continueHand`'s fold branch keeps emitting `BettingRoundEnded` first — the sweep is what makes
  `pot` the whole of what is at stake — and then hands the swept state to
  `settleHand(afterEnd, listOf(otherSeat(foldedSeat)))`, returning that result's state and
  `listOf(endedEvent) + settled.events`.
- The winner is the seat that did **not** fold, never derived from chip counts.
- Delete the "Awarding the pot is STORY-0106" comment in that branch; it has come true.
- No card is dealt and no card is revealed on this path. The engine emits nothing carrying the
  folder's hole cards — this is the security boundary the story exists to hold, not a
  presentation choice.

## Out of scope

- The showdown paths — `TASK-010611` (river close) and `TASK-010612` (run-out) still stop at
  `ShowdownReached`. Do not touch `endBettingRound` or `runOutBoard`.
- The random-hand property harness: a folded hand still satisfies `BettingInvariantTest`'s
  current terminal check, and `RandomHandPlayer` already treats `isHandOver` as ended.

## Tests

### `FoldEndsTheHandTest` — assertions that move, and only these

This ticket changes what a fold emits, so the merged tests that pin it are part of its blast
radius. Nothing else in the file changes, no assertion is weakened, and no test is deleted.

| Test | What moves |
| --- | --- |
| `aFoldEmitsPlayerFoldedThenBettingRoundEnded` | `events.size` 2 → 3; the pot is empty in this fixture, so the third and last event is `HandFinished(sequence = 2)` and there is no award |
| `aFoldSweepsEveryCommitmentIntoThePot` | `pot` 400 → 0; add that the sweep is now visible in the events instead — `UncalledBetReturned(1, 200)` plus `PotAwarded(1, 200)` account for exactly the 400 swept, and `seat(1).stack == 10_100`. Both `committedThisStreet == 0` assertions stay |
| `theUncalledBetIsStillRecoverable` | rename to `theUncalledBetComesBack`; the commitment-difference assertion becomes the event it now produces: `UncalledBetReturned(seat = 1, amount = 200)`, followed by `PotAwarded(seat = 1, amount = 400)`, leaving `seat(1).stack == 10_200` |
| `noFurtherActionIsAcceptedAfterAFold` | `Rejection.NotYourTurn(null)` → `Rejection.HandComplete`; the hand is now over, and `rejectionFor` checks that first |

`aFoldLeavesNobodyToAct`, `aFoldDealsNoCards` and `chipsAreConservedByAFold` are untouched and
must still pass exactly as written.

### `FoldSettlementTest` — new

Fixtures: `handState(...)` on `Street.FLOP` with `board = Board(cards("7h 8h 9h"))`, seat 0
holding `cards("As Ks")` and seat 1 `cards("2c 3d")`, so the folder's cards are real cards a
scan can look for.

| Test | Proves |
| --- | --- |
| `theSurvivorTakesThePot` | seat 0 folds → `seat(1).stack` grows by exactly the swept pot, `pot == 0` |
| `theHandEndsComplete` | `street == Street.COMPLETE`, `seatToAct == null`, `isHandOver`, last event `HandFinished` |
| `theFoldersCardsAppearInNoEvent` | no event in the result carries either of seat 0's two cards |
| `nothingIsReturnedWhenBothSeatsMatched` | equal `committedThisHand` → no `UncalledBetReturned`, one `PotAwarded` for the whole pot |
| `chipsAreConservedByTheFoldSettlement` | `chipsInPlay` identical before and after |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition(before, result)` holds |

## Acceptance criteria

- [ ] `FoldSettlementTest.theSurvivorTakesThePot` passes
- [ ] `FoldSettlementTest.theHandEndsComplete` passes
- [ ] `FoldSettlementTest.theFoldersCardsAppearInNoEvent` passes
- [ ] `FoldSettlementTest.nothingIsReturnedWhenBothSeatsMatched` passes
- [ ] `FoldSettlementTest.chipsAreConservedByTheFoldSettlement` passes
- [ ] `FoldSettlementTest.theEventsDescribeTheTransition` passes
- [ ] `FoldEndsTheHandTest` passes with exactly the four tests above amended and the other three
      unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
