---
schema: 2
id: TASK-010606
title: Split a pot between two winners, odd chip out of position
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010605]
verify:
  - ./gradlew :poker-engine:test --tests '*SplitPotTest'
  - ./gradlew :poker-engine:check
---

## Goal

Two equal hands take half the pot each, and the chip that cannot be halved has an owner named by
the rules rather than by rounding.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Settlement.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SplitPotTest.kt` | create |

Read `HeadsUpOrder.kt` (for `bigBlindSeat`), `DealerEvents.kt` and `GameStates.kt`. Modify none
of them.

## Scope

- `settleHand`'s `require` becomes `winners.size in 1..2` plus
  `require(winners.distinct().size == winners.size)` — the same seat cannot win twice.
- With two winners: `share = pot / 2`, and the remaining `pot % 2` chip goes to
  `bigBlindSeat(state.buttonSeat)` — the player out of position, per
  [`docs/duel-rules.md`](../../docs/duel-rules.md). Chips are integers; nothing is ever divided
  into a fraction and rounded.
- One `PotAwarded` per winner, emitted in **ascending seat order** so a replay of the same hand
  always reads the same way. Skip any award of zero (a pot of 1 chip pays one seat only).
- Everything else in `settleHand` is untouched: the uncalled bet still comes back first,
  `HandFinished` still comes last, and the single-winner path still produces exactly the events
  `TASK-010605` pinned.

## Out of scope

- Deciding *that* two hands are equal — `TASK-010610` computes the winners; this ticket only
  pays them.
- Any change to `SettleHandTest`: the single-winner behaviour it pins is unchanged, and the
  `winners.size == 1` requirement was deliberately left untested there.

## Tests

`SplitPotTest`, JUnit 5. Swept positions built with `handState(...)`, `pot` and
`committedThisHand` set directly.

| Test | Proves |
| --- | --- |
| `anEvenPotSplitsInHalf` | pot 600, winners `listOf(0, 1)` → two `PotAwarded` of 300, both stacks up by 300 |
| `theOddChipGoesToTheSeatOutOfPosition` | pot 601 with the button on seat 0 → seat 1 gets 301, seat 0 gets 300 |
| `theOddChipFollowsTheButton` | the same pot with `buttonSeat = 1` → seat 0 gets 301, seat 1 gets 300 |
| `theAwardsComeInSeatOrder` | the `PotAwarded` for seat 0 precedes the one for seat 1 |
| `anUncalledBetIsReturnedBeforeASplit` | commitments 300/500, pot 800 → `UncalledBetReturned(1, 200)`, then two awards of 300 |
| `aSplitConservesEveryChip` | `chipsInPlay` unchanged and `pot == 0` for every case above |
| `aWinnerCannotWinTwice` | `winners = listOf(1, 1)` throws `IllegalArgumentException` |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition(before, result)` holds for the odd-chip case |

## Acceptance criteria

- [ ] `SplitPotTest.anEvenPotSplitsInHalf` passes
- [ ] `SplitPotTest.theOddChipGoesToTheSeatOutOfPosition` passes
- [ ] `SplitPotTest.theOddChipFollowsTheButton` passes
- [ ] `SplitPotTest.theAwardsComeInSeatOrder` passes
- [ ] `SplitPotTest.anUncalledBetIsReturnedBeforeASplit` passes
- [ ] `SplitPotTest.aSplitConservesEveryChip` passes
- [ ] `SplitPotTest.aWinnerCannotWinTwice` passes
- [ ] `SplitPotTest.theEventsDescribeTheTransition` passes
- [ ] `SettleHandTest` passes unchanged — this ticket adds a branch for two winners and does not
      alter the one-winner path it pins
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
