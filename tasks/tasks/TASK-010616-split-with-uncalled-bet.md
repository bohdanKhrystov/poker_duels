---
schema: 2
id: TASK-010616
title: Pin a split pot that also returns an uncalled bet
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [engine, rules, chips, test]
depends_on: [TASK-010606]
verify:
  - ./gradlew :poker-engine:test --tests '*SplitPotTest'
  - ./gradlew :poker-engine:check
---

## Goal

`TASK-010606` pinned the odd chip, and it pinned an uncalled bet, but never both at once. Every
odd-pot case there gives the two seats equal `committedThisHand` precisely so that no
`UncalledBetReturned` fires. So the one ordering that is easiest to get wrong — return the
uncalled part *first*, then split what is left — is asserted by no test.

The production code already does this correctly. This ticket pins it, so that a later refactor
that splits before returning fails a test instead of silently paying a chip to the wrong seat.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SplitPotTest.kt` | modify |

Read `poker-engine/src/main/kotlin/duels/poker/engine/game/UncalledPortion.kt`. Modify it only if
a test proves it wrong — in which case say so in the PR rather than quietly changing behaviour.

## Scope

Add tests to `SplitPotTest.kt`. Add no production code.

Construct a state where the two seats' `committedThisHand` differ *and* the pot left after the
return is odd. Worked example:

- seat 0 committed 400, seat 1 committed 301, pot 701, button on seat 0
- the uncalled portion is 99 to seat 0, leaving 602 — even, so pick different numbers if you want
  the odd case; e.g. seat 0 committed 400, seat 1 committed 300, pot 700, uncalled 100 to seat 0,
  leaving 600 — also even

Both seats' commitments summing to an odd pot is what you are after. Derive the numbers rather
than copying these; the point of the test is the arithmetic, so getting there by hand is the work.

## Tests

| Name | Asserts |
| --- | --- |
| `theUncalledBetIsReturnedBeforeTheSplit` | `UncalledBetReturned` precedes both `PotAwarded` events, and the split is computed on the pot *after* the return |
| `anOddSplitAfterAnUncalledReturnStillConservesEveryChip` | stacks + pot before == stacks + pot after |
| `theOddChipAfterAnUncalledReturnGoesOutOfPosition` | the extra chip lands on `bigBlindSeat(state.buttonSeat)`, not on the seat that got the return |

## Done

Both `verify:` commands exit 0, and the three tests above fail if `settleHand` is changed to
split the pot before returning the uncalled portion.
