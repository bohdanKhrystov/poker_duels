---
schema: 2
id: TASK-010505
title: Name the heads-up blind and action order once
type: task
status: done
parent: STORY-0105
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010427]
verify:
  - ./gradlew :poker-engine:test --tests '*HeadsUpOrderTest'
  - ./gradlew :poker-engine:check
---

## Goal

The single most-often-inverted rule in heads-up hold'em — the button is the small blind and acts
first only before the flop — lives in one named, tested place instead of in every caller.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/HeadsUpOrder.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/HeadsUpOrderTest.kt` | create |

Read `Street.kt` and `docs/duel-rules.md` ("Positions and blinds"). Modify neither.

## Scope

- `HeadsUpOrder.kt`, package `duels.poker.engine.game`, four public top-level functions and
  nothing else:

  ```kotlin
  public fun otherSeat(seat: Int): Int
  public fun smallBlindSeat(buttonSeat: Int): Int
  public fun bigBlindSeat(buttonSeat: Int): Int
  public fun firstToActOn(street: Street, buttonSeat: Int): Int
  ```

- `otherSeat` requires `seat in 0..1` and returns `1 - seat`.
- `smallBlindSeat(buttonSeat) == buttonSeat` — **the button posts the small blind**, per
  `docs/duel-rules.md`. Cite that line in the KDoc, because the function looks like a typo
  otherwise.
- `bigBlindSeat` is the other seat.
- `firstToActOn` is the button on `PREFLOP` and the non-button on `FLOP`, `TURN` and `RIVER`.
  `SHOWDOWN` and `COMPLETE` have no betting: throw `IllegalArgumentException`. Use `when` over
  `Street` with no `else`.
- Every function validates `buttonSeat in 0..1` with `require`.

## Out of scope

- Who acts *next* inside a betting round — `TASK-010514`.
- Posting the blinds — `TASK-010506`.

## Tests

`HeadsUpOrderTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `otherSeatFlipsTheIndex` | `otherSeat(0) == 1`, `otherSeat(1) == 0` |
| `theButtonPostsTheSmallBlind` | `smallBlindSeat(0) == 0` and `smallBlindSeat(1) == 1` |
| `theNonButtonPostsTheBigBlind` | `bigBlindSeat(0) == 1` and `bigBlindSeat(1) == 0` |
| `theButtonActsFirstPreflop` | `firstToActOn(PREFLOP, 0) == 0`, `firstToActOn(PREFLOP, 1) == 1` |
| `theNonButtonActsFirstOnEveryLaterStreet` | for `FLOP`, `TURN`, `RIVER` and both buttons, the result is `otherSeat(buttonSeat)` |
| `thereIsNoActionOrderAfterTheRiver` | `SHOWDOWN` and `COMPLETE` each throw `IllegalArgumentException` |
| `rejectsASeatOutsideTheTable` | `otherSeat(2)` and `firstToActOn(PREFLOP, -1)` throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `HeadsUpOrderTest.otherSeatFlipsTheIndex` passes
- [ ] `HeadsUpOrderTest.theButtonPostsTheSmallBlind` passes
- [ ] `HeadsUpOrderTest.theNonButtonPostsTheBigBlind` passes
- [ ] `HeadsUpOrderTest.theButtonActsFirstPreflop` passes
- [ ] `HeadsUpOrderTest.theNonButtonActsFirstOnEveryLaterStreet` passes
- [ ] `HeadsUpOrderTest.thereIsNoActionOrderAfterTheRiver` passes
- [ ] `HeadsUpOrderTest.rejectsASeatOutsideTheTable` passes
- [ ] `firstToActOn` contains no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
