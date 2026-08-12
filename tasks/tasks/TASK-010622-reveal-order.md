---
schema: 2
id: TASK-010622
title: Decide who shows at a showdown, and in what order
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, security]
depends_on: [TASK-010618]
verify:
  - ./gradlew :poker-engine:test --tests '*ShowdownRevealTest'
  - ./gradlew :poker-engine:check
---

## Goal

A pure `revealOrder(state, winners)` names the seats that show at a showdown, in the order they
show, implementing [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md): the loser
mucks, the last aggressor shows first.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Showdown.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ShowdownRevealTest.kt` | create |

Read `GameState.kt`, `HeadsUpOrder.kt` and
`docs/adr/ADR-0008-loser-mucks-at-showdown.md`. Modify none of them.

## Scope

- Add `public fun revealOrder(state: GameState, winners: List<Int>): List<Int>` to `Showdown.kt`,
  beside `showdownWinners`. It touches no chips and emits no events — it returns seat indices.
- The rule, which is short because ADR-0008 makes it short:
  - **The seats that show are exactly `winners`.** A losing hand is never revealed, so a single
    winner shows alone and the loser's cards are not in the returned list, whether or not that
    loser was the aggressor.
  - **Order.** With one winner there is one order. With two (a tie) the seat named by
    `state.lastAggressor` comes first; when `lastAggressor` is `null` — the final street was
    checked through — the seat out of position comes first, which is
    `firstToActOn(Street.RIVER, state.buttonSeat)`.
- Use `Street.RIVER` explicitly for that call: at a showdown `state.street` is `Street.SHOWDOWN`
  and `firstToActOn` throws on it. A comment saying so is worth its line.
- `require` that `winners` has one or two entries, that they are distinct, and that each is a seat
  index — the same guards `settleHand` uses on the same argument.
- KDoc the function: what it returns, that a mucked hand appears in no event exactly as a folded
  hand does, and that reveal order is observable only at a tie today because the loser never
  shows — it is tracked because the rules order the showdown, and a future voluntary show
  (ADR-0008's rejected-for-now alternative) would need it.

## Out of scope

- Emitting `HandRevealed` events, or any change to `StreetProgression.kt` — `TASK-010623`.
- Deciding *who won*: that is `showdownWinners`, already merged, and this function takes its
  result as an argument rather than calling it.

## Tests

`ShowdownRevealTest` — fixtures are `handState().copy(...)`; no engine, no events.

| Test | Proves |
| --- | --- |
| `onlyTheWinnerShows` | `revealOrder(state, listOf(0)) == listOf(0)` — seat 1 is absent |
| `theLosingAggressorStillMucks` | with `lastAggressor = 1` and `winners = listOf(0)`, the result is `listOf(0)`: aggression does not put a losing hand on the log |
| `aTieShowsBothHands` | `revealOrder(state, listOf(0, 1))` has size 2 and contains both seats |
| `theLastAggressorShowsFirstInATie` | with `lastAggressor = 1`, `revealOrder(state, listOf(0, 1)) == listOf(1, 0)`; with `lastAggressor = 0` it is `listOf(0, 1)` |
| `aCheckedFinalStreetShowsTheSeatOutOfPositionFirst` | with `lastAggressor = null` and `buttonSeat = 0` the result is `listOf(1, 0)`; with `buttonSeat = 1` it is `listOf(0, 1)` |
| `rejectsAWinnerListThatIsNotOneOrTwoDistinctSeats` | `emptyList()` and `listOf(0, 0)` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `ShowdownRevealTest.onlyTheWinnerShows` passes
- [ ] `ShowdownRevealTest.theLosingAggressorStillMucks` passes
- [ ] `ShowdownRevealTest.aTieShowsBothHands` passes
- [ ] `ShowdownRevealTest.theLastAggressorShowsFirstInATie` passes
- [ ] `ShowdownRevealTest.aCheckedFinalStreetShowsTheSeatOutOfPositionFirst` passes
- [ ] `ShowdownRevealTest.rejectsAWinnerListThatIsNotOneOrTwoDistinctSeats` passes
- [ ] `revealOrder` returns no seat that is not in `winners`
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
