---
schema: 2
id: TASK-010516
title: End the betting the moment a player folds
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010515]
verify:
  - ./gradlew :poker-engine:test --tests '*FoldEndsTheHandTest'
  - ./gradlew :poker-engine:check
---

## Goal

A fold stops the hand where it stands: the chips go to the pot, no card is dealt, and nobody is
on turn.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/FoldEndsTheHandTest.kt` | create |

Read `DealerEvents.kt` and `DealerProjection.kt`. Modify neither.

## Scope

- `continueHand` gains a first branch, before the `roundContinues` check: if either seat
  `hasFolded`, emit `BettingRoundEnded(state.eventCount, state.street)` and return. Nothing else
  follows — no `StreetDealt`, no `ActionOn`, no `ShowdownReached`.
- `BettingRoundEnded` sweeps both commitments into `pot` and clears `seatToAct`
  (`DealerProjection`), which leaves the hand in exactly the shape STORY-0106 wants: every chip
  in one place and one seat marked folded.
- KDoc the seam: **awarding the pot is STORY-0106**, and the uncalled part of the last bet is
  still recoverable, because `committedThisHand` is gross and never decreases — the difference
  between the two seats' `committedThisHand` is the amount to return.
- The hand's `street` does not move and `isHandOver` stays false: `HandFinished` belongs to the
  ticket that awards the pot.

## Out of scope

- `UncalledBetReturned`, `PotAwarded`, `HandFinished` — STORY-0106.
- The round ending without a fold — `TASK-010517`.

## Tests

`FoldEndsTheHandTest`, JUnit 5, driving `DefaultPokerEngine.handle` from positions built with
`handState()`/`seats()` and `copy`. Blinds 50/100, button on seat 0.

| Test | Proves |
| --- | --- |
| `aFoldEmitsPlayerFoldedThenBettingRoundEnded` | exactly two events, in that order |
| `aFoldSweepsEveryCommitmentIntoThePot` | flop, seat 1 committed 300 and seat 0 committed 100: after `Fold(0)`, `pot == 400` and both `committedThisStreet == 0` |
| `aFoldLeavesNobodyToAct` | `seatToAct == null` and `seat(0).hasFolded` |
| `aFoldDealsNoCards` | `board` and `deck.remaining` are unchanged |
| `chipsAreConservedByAFold` | `chipsInPlay` is the same before and after |
| `theUncalledBetIsStillRecoverable` | `seat(1).committedThisHand - seat(0).committedThisHand == 200` in that position |
| `noFurtherActionIsAcceptedAfterAFold` | `handle(folded, Check(1))` is rejected with `NotYourTurn(null)` |

## Acceptance criteria

- [ ] `FoldEndsTheHandTest.aFoldEmitsPlayerFoldedThenBettingRoundEnded` passes
- [ ] `FoldEndsTheHandTest.aFoldSweepsEveryCommitmentIntoThePot` passes
- [ ] `FoldEndsTheHandTest.aFoldLeavesNobodyToAct` passes
- [ ] `FoldEndsTheHandTest.aFoldDealsNoCards` passes
- [ ] `FoldEndsTheHandTest.chipsAreConservedByAFold` passes
- [ ] `FoldEndsTheHandTest.theUncalledBetIsStillRecoverable` passes
- [ ] `FoldEndsTheHandTest.noFurtherActionIsAcceptedAfterAFold` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
