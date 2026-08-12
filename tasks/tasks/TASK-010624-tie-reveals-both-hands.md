---
schema: 2
id: TASK-010624
title: A tied showdown reveals both hands, the river aggressor first
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, tests, rules]
depends_on: [TASK-010619, TASK-010623]
verify:
  - ./gradlew :poker-engine:test --tests '*ShowdownRevealOrderTest'
  - ./gradlew :poker-engine:check
---

## Goal

Played through `DefaultPokerEngine`, a tied showdown reveals both hands, and the order is the
betting's: the river aggressor first, or the seat out of position when the river was checked
through.

## Why a second reveal test

`ShowdownRevealTest` pins the rule on a hand-built state; this pins the whole path — a real bet
setting `lastAggressor`, a real showdown reading it — which is the only place the field's setter,
its survival through `BettingRoundEnded`, and the emission meet.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ShowdownRevealOrderTest.kt` | create |

Read `StreetAdvanceTest.kt` (for the river-fixture style), `GameStates.kt`, `Showdown.kt` and
`docs/adr/ADR-0008-loser-mucks-at-showdown.md`. Modify none of them.

## Scope

- One fixture, a river position both seats tie on, built directly the way
  `StreetAdvanceTest.riverState()` is:
  `handState().copy(street = Street.RIVER, board = Board(cards("As Kd Qc Jh Ts")), pot = 600,
  betToMatch = 0, minRaiseTo = 100, seatToAct = 1, seats = ...)` with seat 0 holding `2c 3d`,
  seat 1 holding `4h 5c`, both on `stack = 9_700`, `committedThisStreet = 0`,
  `committedThisHand = 300`. Both seats play the board's broadway straight, neither hole card
  makes a flush, so the hands are equal — assert that equality via the two `PotAwarded` amounts
  rather than trusting the fixture blindly.
- Two lines through it, both driven by `DefaultPokerEngine.handle`:
  - **Bet and call.** `Check(1)`, `Bet(0, 200)`, `Call(1)` — seat 0, the button, is the river
    aggressor, so the reveals come out `[0, 1]` and the pot of 1_000 splits 500/500.
  - **Checked through.** `Check(1)`, `Check(0)` — no aggressor, so the seat out of position, the
    non-button seat 1, shows first: reveals `[1, 0]`, pot of 600 splits 300/300.
- Read reveals as `result.events.filterIsInstance<HandRevealed>()` and compare `map { it.seat }`
  to an expected list — order is the whole point, so assert the list, never a set.

## Out of scope

- Any change to production code. If a test here fails, the bug is in `TASK-010619`, `TASK-010622`
  or `TASK-010623`; report it rather than fixing it from this ticket.
- The single-winner muck, covered by `CardSecrecyTest` and `StreetAdvanceTest`.
- A hand where an earlier street's aggressor is cleared by a later `StreetDealt`: that mechanism
  is pinned at the projection level by `TASK-010620`.

## Tests

`ShowdownRevealOrderTest`

| Test | Proves |
| --- | --- |
| `aTieRevealsBothHands` | the checked-through line emits exactly two `HandRevealed`, one per seat, each carrying that seat's actual hole cards |
| `theRiverAggressorShowsFirst` | the bet-and-call line emits reveal seats in the order `[0, 1]` |
| `aCheckedRiverShowsTheSeatOutOfPositionFirst` | the checked-through line emits reveal seats in the order `[1, 0]` |
| `bothHandsShowBeforeThePotIsSplit` | every `HandRevealed` index precedes every `PotAwarded` index, and the two awards are 500 and 500 on the bet-and-call line |

## Acceptance criteria

- [ ] `ShowdownRevealOrderTest.aTieRevealsBothHands` passes
- [ ] `ShowdownRevealOrderTest.theRiverAggressorShowsFirst` passes
- [ ] `ShowdownRevealOrderTest.aCheckedRiverShowsTheSeatOutOfPositionFirst` passes
- [ ] `ShowdownRevealOrderTest.bothHandsShowBeforeThePotIsSplit` passes
- [ ] No production file is modified, and no file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
