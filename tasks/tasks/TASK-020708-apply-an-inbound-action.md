---
schema: 2
id: TASK-020708
title: An inbound Act reaches the engine, and its result reaches exactly the seats entitled to it
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, engine-integration, authority]
depends_on: [TASK-020707]
verify:
  - ./gradlew :poker-server:test --tests '*DuelActionTest'
  - ./gradlew :poker-server:check
---

## Goal

`act(runner, seat, message, seeds)` is the whole inbound path: guard, `DefaultPokerEngine.handle`,
append to the hand's log, broadcast per recipient, and roll into the next hand when this one ends.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelAction.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelActionTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/duel/ActRefusal.kt`,
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelProgress.kt` (`advance`),
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` (`framesFor`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/DefaultPokerEngine.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/Rejection.kt`.

## Scope

- Package `duels.poker.server.duel`. One declaration, KDoc included:

  ```kotlin
  public fun act(runner: DuelRunner, seat: Int, message: Act, seeds: HandSeedSource): DuelStep
  ```

- The body, in order:
  1. `runner.hand ?: return DuelStep(runner, emptyList())` — a frame for a duel that is already over
     is dropped in silence.
  2. `guard(hand.state, hand.log.events, seat, message)`:
     - `STALE_FRAME` → `DuelStep(runner, emptyList())`. Nothing changes and nobody hears anything.
     - `NOT_YOUR_TURN` → `DuelStep(runner, listOf(Addressed(seat,
       ServerMessage.Rejected(Rejection.NotYourTurn(hand.state.seatToAct)))))`. **One frame, to the
       sender only** — a rejection is never broadcast, and the returned runner is the one passed in.
     - `null` → carry on.
  3. `DefaultPokerEngine.handle(hand.state, message.action)`. A non-null `result.rejection` becomes
     `Addressed(seat, ServerMessage.Rejected(rejection))` — again the sender alone — and the runner
     is returned unchanged, log included. The engine's own `Rejection` is passed through verbatim;
     this file invents no second vocabulary for "illegal".
  4. Accepted: the new `LiveHand` is `result.newState` with `log.copy(actions = log.actions +
     message.action, events = log.events + result.events)`, and the outbound is
     `framesFor(newState, result.events, newLog.events)`.
  5. If `result.newState.isHandOver`, call `advance(played, seeds)` and return its runner with
     `framesFor`'s frames **followed by** `advance`'s: the hand's last events reach both seats before
     the next hand's opening ones.
- `act` never inspects a card, never computes an amount, and never decides whether an action is
  legal. Legality is `DefaultPokerEngine`; visibility is `framesFor`.
- `seeds` is drawn from only when a hand actually ends, and only by `advance`. `act` itself takes no
  seed, reads no clock and performs no I/O.

## Out of scope

- Reading an `Act` off a socket or writing an `Addressed` to one — `TASK-020715`.
- A timeout that turns into a fold — `ADR-0013`, `STORY-0208`.
- Publishing the finished duel — `TASK-020709` declares the port, `TASK-020714` calls it.

## Tests

`DuelActionTest`, JUnit 5, package `duels.poker.server.duel`. Open with `startDuel(DuelFormat.DEFAULT,
buttonSeat = 0, seed = 7L)`; the seat on turn is `runner.hand!!.state.seatToAct` and the current
frame is `Act(1, decisionPointOf(runner.hand!!.log.events)!!.sequence, PlayerAction.Call(seatToAct))`.
Pass `HandSeedSource { 99L }` for `seeds` — a constant is enough here, because the tests that care
about a stream of seeds live in `DuelProgressTest`.

| Test | Proves |
| --- | --- |
| `anAcceptedActionAppearsInTheHandLog` | after `act`, `hand!!.log.actions` is `listOf(theAction)` and `log.events.size` is greater than before |
| `anAcceptedActionReachesBothSeats` | the outbound contains a `Snapshot` addressed to each seat |
| `theNextSeatIsPromptedAndTheOtherIsNot` | the outbound contains exactly one `YourTurn`, addressed to the new `state.seatToAct` |
| `anActionFromTheWrongSeatIsRejectedToThatSeatAlone` | sending the frame as the other seat gives outbound of exactly one `Rejected` addressed to that seat, and `step.runner == runner` |
| `anActionForTheOpponentIsRejectedToTheSenderAlone` | seat on turn sending `PlayerAction.Fold(otherSeat)` gives one `Rejected` to the sender and `step.runner == runner` |
| `areplayedFrameChangesNothingAndSaysNothing` | applying the same frame twice: the second `act` returns `outbound.isEmpty()` and a runner equal to the one the first produced |
| `anIllegalActionIsRejectedWithTheEnginesReason` | a `PlayerAction.Bet(seatToAct, 1)` facing a blind gives one `Rejected` whose `rejection` equals `DefaultPokerEngine.handle(state, action).rejection` |
| `afoldEndsTheHandAndOpensTheNext` | `act` with `PlayerAction.Fold(seatToAct)` returns a runner whose `hand!!.log.handNumber == 2`, whose `log.hands` has one entry, and whose outbound contains `Snapshot`s for hand 1's end and hand 2's opening |
| `aframeForAFinishedDuelIsDropped` | `act` on a runner with `hand == null` returns that runner and an empty outbound |
| `theReturnedRunnerAlwaysAwaitsAnAction` | after each of the accepted actions above, `step.runner.hand == null || step.runner.hand!!.state.seatToAct != null` |

## Acceptance criteria

- [ ] `DuelActionTest.anAcceptedActionAppearsInTheHandLog` passes
- [ ] `DuelActionTest.anAcceptedActionReachesBothSeats` passes
- [ ] `DuelActionTest.theNextSeatIsPromptedAndTheOtherIsNot` passes
- [ ] `DuelActionTest.anActionFromTheWrongSeatIsRejectedToThatSeatAlone` passes
- [ ] `DuelActionTest.anActionForTheOpponentIsRejectedToTheSenderAlone` passes
- [ ] `DuelActionTest.areplayedFrameChangesNothingAndSaysNothing` passes
- [ ] `DuelActionTest.anIllegalActionIsRejectedWithTheEnginesReason` passes
- [ ] `DuelActionTest.afoldEndsTheHandAndOpensTheNext` passes
- [ ] `DuelActionTest.aframeForAFinishedDuelIsDropped` passes
- [ ] `DuelActionTest.theReturnedRunnerAlwaysAwaitsAnAction` passes
- [ ] `DuelAction.kt` contains no `Rejection.` constructor other than `Rejection.NotYourTurn`, and
      no reference to `holeCards`, `PlayerView` or `visibleTo`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
