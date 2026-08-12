---
schema: 2
id: TASK-010709
title: Deal the match's next hand at its scheduled blinds
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, duel, rules]
depends_on: [TASK-010708]
verify:
  - ./gradlew :poker-engine:test --tests '*StartNextHandTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `MatchState` opens its next hand — right hand number, right button, right blind level, right
stacks — without duplicating a single rule `startHand` already holds.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/StartNextHandTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt` (the `startHand`
signature and what it already guarantees), `.../duel/MatchState.kt` and
`.../game/GameEvent.kt` (`HandStarted`, `BlindPosted`). Modify none of them.

## Scope

- `MatchProgression.kt` holds top-level functions only and declares no class, so ktlint's
  `standard:filename` rule does not bind its name — do not add a class to it.
- `public fun startNextHand(match: MatchState, rng: Rng): EngineResult`, delegating to
  `startHand(handNumber = match.nextHandNumber, buttonSeat = match.buttonSeat, stacks = match.stacks, smallBlind = match.blinds.smallBlind, bigBlind = match.blinds.bigBlind, rng = rng)`.
- It holds no rules of its own beyond one guard: `require(match.stacks.all { it >= 1 })`, with a
  message naming the match's stacks. A seat with no chips cannot be dealt in, and a match that
  reached that state should already have ended.
- A seat with fewer chips than its blind is **not** rejected: it is dealt in and posts all-in,
  which `startHand` already does. This ticket must not re-implement that.
- KDoc says no `MatchState` comes back, because the hand is not over yet — `recordHand`
  (`TASK-010710`) closes the loop once it is.

## Out of scope

- Folding a finished hand back into the match — `TASK-010710`.
- Anything about whether the match should still be running — `TASK-010712`.
- Touching `startHand` or any file under `duels.poker.engine.game`.

## Tests

`StartNextHandTest`, building matches from `MatchState.start(DuelFormat.DEFAULT)` and `copy`, and
passing `SplitMix64Rng(1L)` as the rng

| Test | Proves |
| --- | --- |
| `opensTheHandAfterTheOnesAlreadyPlayed` | the first event is a `HandStarted` with `handNumber == 1` for a fresh match, and with `handNumber == 4` when `handsPlayed = 3` |
| `postsTheBlindLevelForThatHandNumber` | with `handsPlayed = 10`, the `HandStarted` carries `smallBlind == 75` and `bigBlind == 150` |
| `dealsTheStacksTheMatchCarries` | with `stacks = listOf(3_000, 17_000)`, `HandStarted.stacks == listOf(3_000, 17_000)` and `newState.chipsInPlay == 20_000` |
| `putsTheButtonWhereTheMatchSaysItIs` | with `buttonSeat = 1`, `newState.buttonSeat == 1` and the `BlindPosted` with `isBigBlind = false` is on seat 1 |
| `aSeatShorterThanItsBlindIsDealtInAllIn` | with `stacks = listOf(40, 20_000)` at 50/100, seat 0's `BlindPosted` has `to == 40` and `newState.seat(0).isAllIn` is true |
| `refusesToOpenAHandForASeatWithNoChips` | `stacks = listOf(0, 20_000)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `StartNextHandTest.opensTheHandAfterTheOnesAlreadyPlayed` passes
- [ ] `StartNextHandTest.postsTheBlindLevelForThatHandNumber` passes
- [ ] `StartNextHandTest.dealsTheStacksTheMatchCarries` passes
- [ ] `StartNextHandTest.putsTheButtonWhereTheMatchSaysItIs` passes
- [ ] `StartNextHandTest.aSeatShorterThanItsBlindIsDealtInAllIn` passes
- [ ] `StartNextHandTest.refusesToOpenAHandForASeatWithNoChips` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
