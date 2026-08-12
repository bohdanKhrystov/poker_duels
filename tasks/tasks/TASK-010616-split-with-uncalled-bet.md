---
schema: 2
id: TASK-010616
title: Pin a split pot that also returns an uncalled bet
type: task
status: done
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

Construct a state where the two seats' `committedThisHand` differ, so that a return fires before
the split.

**This ticket originally asked for an odd pot after the return. That state is unreachable**, and
the correction is worth recording rather than quietly dropping:

```
pot       = c0 + c1
uncalled  = |c0 - c1|
remaining = (c0 + c1) - |c0 - c1| = 2 * min(c0, c1)
```

The pot left after an uncalled return is twice the smaller commitment — always even. In heads-up,
an odd chip can therefore never arise on the same hand as an uncalled return. The two features of
`settleHand` are mutually exclusive by arithmetic, not by accident.

That leaves the ordering as the thing genuinely worth pinning.

## Tests

| Name | Asserts |
| --- | --- |
| `theUncalledBetIsReturnedBeforeTheSplit` | `UncalledBetReturned` precedes both `PotAwarded` events, and the split is computed on the pot *after* the return — must fail if `settleHand` splits first |
| `anOddSplitAfterAnUncalledReturnStillConservesEveryChip` | stacks + pot before == stacks + pot after |
| `anUncalledReturnAlwaysLeavesAnEvenPot` | the split is exactly equal, with no odd chip, and a comment records the `2 * min(committedThisHand)` identity that makes it so |

## Done

Both `verify:` commands exit 0, and the three tests above fail if `settleHand` is changed to
split the pot before returning the uncalled portion.
