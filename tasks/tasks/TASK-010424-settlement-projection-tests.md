---
schema: 2
id: TASK-010424
title: Settlement projection tests and chip conservation
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, contract, chips]
depends_on: [TASK-010423]
verify:
  - ./gradlew :poker-engine:test --tests '*SettlementProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

The three branches that hand chips back — the ones that could quietly create or destroy money —
are pinned down by tests, including over a whole scripted hand.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SettlementProjectionTest.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerProjection.kt` | modify only if a test below proves a branch wrong |

Read `DealerProjection.kt`, `DealerEvents.kt` and `GameStates.kt`.

## Scope

- Tests only. `DealerProjection.kt` already implements these three branches (`TASK-010423`); touch
  it only to fix a defect one of these tests exposes, and never to add behaviour.
- The scripted-hand test is the point of this ticket: chips are conserved across a full sequence,
  not merely per event.

## Out of scope

- Deciding who wins, how an odd chip is split, or what a showdown means — STORY-0106.
- Any new production behaviour. If a test here wants a feature rather than a fix, it does not
  belong in this ticket.

## Tests

`SettlementProjectionTest`, JUnit 5, using `handState()`, `seats()` and `copy`.

| Test | Proves |
| --- | --- |
| `returningAnUncalledBetMovesChipsFromThePotToTheSeat` | `pot = 1_000`, `UncalledBetReturned(1, 0, 400)` → seat 0's stack up 400, `pot == 600`, seat 1 untouched |
| `awardingThePotMovesChipsFromThePotToTheSeat` | `pot = 600`, `PotAwarded(2, 1, 600)` → seat 1's stack up 600, `pot == 0` |
| `aSplitPotEmptiesThePotInTwoAwards` | `pot = 600`, `PotAwarded(2, 0, 300)` then `PotAwarded(3, 1, 300)` → `pot == 0` and both stacks up 300 |
| `beingPaidDoesNotUndoAllIn` | an all-in seat awarded the pot still has `isAllIn == true` |
| `handFinishedCompletesTheHand` | `HandFinished(4)` → `street == Street.COMPLETE`, `isHandOver`, `seatToAct == null` |
| `rejectsAnAwardLargerThanThePot` | `pot = 600`, `PotAwarded(2, 0, 700)` throws `IllegalArgumentException` |
| `chipsAreConservedAcrossAScriptedHand` | folding the sequence below from `handState()` keeps `chipsInPlay == 2 * START_STACK` after **every** step, and ends with `pot == 0`, `seat(0).stack == 10_300`, `seat(1).stack == 9_700` |

The sequence, applied in order with `applyBetting` for `BettingEvent` and `applyDealer` for
`DealerEvent`:

```
PlayerBet(1, 0, 300)                          seat 0 opens
PlayerCalled(2, 1, 300)                       seat 1 calls
BettingRoundEnded(3, Street.PREFLOP)          pot 600
StreetDealt(4, Street.FLOP, cards("As Kd 7c"))
PlayerChecked(5, 1)
PlayerBet(6, 0, 200)
PlayerFolded(7, 1)
BettingRoundEnded(8, Street.FLOP)             pot 800 — sweep before settling
UncalledBetReturned(9, 0, 200)                pot 600, nobody covered the flop bet
PotAwarded(10, 0, 600)                        pot 0
HandFinished(11)
```

> The sweep at step 8 is not optional: settling while seat 0's 200 is still
> `committedThisStreet` would take the return out of the pot **and** leave the commitment
> standing, which invents 200 chips. That ordering is the reason this test exists.

## Acceptance criteria

- [ ] `SettlementProjectionTest.returningAnUncalledBetMovesChipsFromThePotToTheSeat` passes
- [ ] `SettlementProjectionTest.awardingThePotMovesChipsFromThePotToTheSeat` passes
- [ ] `SettlementProjectionTest.aSplitPotEmptiesThePotInTwoAwards` passes
- [ ] `SettlementProjectionTest.beingPaidDoesNotUndoAllIn` passes
- [ ] `SettlementProjectionTest.handFinishedCompletesTheHand` passes
- [ ] `SettlementProjectionTest.rejectsAnAwardLargerThanThePot` passes
- [ ] `SettlementProjectionTest.chipsAreConservedAcrossAScriptedHand` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
