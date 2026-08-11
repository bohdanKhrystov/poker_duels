---
schema: 2
id: TASK-010512
title: Handle one betting action in a real engine
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010511]
verify:
  - ./gradlew :poker-engine:test --tests '*DefaultPokerEngineTest'
  - ./gradlew :poker-engine:check
---

## Goal

There is a `PokerEngine` that is not a stub: it refuses an illegal action with a reason and
applies a legal one to the state.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DefaultPokerEngine.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DefaultPokerEngineTest.kt` | create |

Read `PokerEngine.kt`, `EngineResult.kt`, `ActionValidation.kt`, `BettingActions.kt`,
`StateProjection.kt`. Modify none of them.

## Scope

- `DefaultPokerEngine.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public object DefaultPokerEngine : PokerEngine
  ```

- `handle` is four lines of composition and holds no rules of its own:
  1. `rejectionFor(state, action)` — non-null gives `EngineResult.rejected(state, reason)`,
     which by construction returns the state untouched and no events.
  2. `eventFor(state, action)` — one `BettingEvent`.
  3. `StateProjection.apply(state, event)` — the only way this module applies an event.
  4. `EngineResult.accepted(newState, listOf(event))`.
- No hidden state, no `var`, no clock, no randomness: `handle` is a pure function of its
  arguments, as ADR-0001 requires.
- **The hand stops after each action for now.** `applyBetting` clears `seatToAct`, and nothing
  yet names the next actor; `TASK-010514` adds the continuation. Say so in the KDoc and delete
  the note when it lands.

## Out of scope

- Whose turn it is next, ending the round, dealing a street — `TASK-010513` onwards.
- Running `PokerEngineContract` against it — `TASK-010517`.

## Tests

`DefaultPokerEngineTest`, JUnit 5, positions from `handState()` with `copy`. Blinds 50/100,
stacks 10 000. `assertEventsDescribeTheTransition` is available from `PokerEngineContract.kt`.

| Test | Proves |
| --- | --- |
| `anIllegalActionComesBackRejectedAndUnchanged` | `Check(0)` facing `betToMatch = 300`: `isRejected`, `newState == state`, no events |
| `anOutOfTurnActionIsRejected` | `Fold(1)` with `seatToAct = 0` gives `NotYourTurn(0)` |
| `aLegalBetEmitsOnePlayerBetAndMovesTheChips` | fresh street, `Bet(0, 300)`: one `PlayerBet`, seat 0 stack 9 700, `betToMatch == 300` |
| `aCallCommitsUpToTheBar` | committed 100 facing 300, `Call(0)`: `committedThisStreet == 300` |
| `chipsAreConservedByAnAcceptedAction` | `chipsInPlay` is the same before and after each of a bet, a call, a fold and an all-in |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition(state, result)` for those same four actions |
| `theSequenceContinuesFromEventCount` | with `eventCount = 5`, the emitted event has `sequence == 5` and `newState.eventCount == 6` |

## Acceptance criteria

- [ ] `DefaultPokerEngineTest.anIllegalActionComesBackRejectedAndUnchanged` passes
- [ ] `DefaultPokerEngineTest.anOutOfTurnActionIsRejected` passes
- [ ] `DefaultPokerEngineTest.aLegalBetEmitsOnePlayerBetAndMovesTheChips` passes
- [ ] `DefaultPokerEngineTest.aCallCommitsUpToTheBar` passes
- [ ] `DefaultPokerEngineTest.chipsAreConservedByAnAcceptedAction` passes
- [ ] `DefaultPokerEngineTest.theEventsDescribeTheTransition` passes
- [ ] `DefaultPokerEngineTest.theSequenceContinuesFromEventCount` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
