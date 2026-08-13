---
schema: 2
id: TASK-020806
title: An absent seat folds, as an ordinary action, whenever the turn reaches it
type: task
status: ready
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, resilience]
depends_on: [TASK-020805]
verify:
  - ./gradlew :poker-server:test --tests '*AbsentSeatsTest'
  - ./gradlew :poker-server:test --tests '*DuelActionTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - grep -q 'PlayerAction.Fold' poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt
  - grep -c 'DefaultPokerEngine' poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt | grep -qx 0
---

## Goal

A pure function that, given a duel and the seats nobody is sitting in, folds for those seats every
time the turn falls to one of them — through `duels.poker.server.duel.act`, so the fold reaches
`DefaultPokerEngine.handle` as an ordinary `PlayerAction.Fold` and is indistinguishable from one a
player sent.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/AbsentSeatsTest.kt` | create |

Read `duel/DuelAction.kt` (the `act` this delegates to), `duel/DuelTurn.kt` (`decisionPointOf`) and
`duel/DuelRunner.kt` for the types. Do not open the engine.

## Scope

- One public function:

  ```kotlin
  public fun foldAbsent(step: DuelStep, absent: Set<Int>, seeds: HandSeedSource): DuelStep
  ```

  It loops: while the duel has a live hand, the hand has a `seatToAct`, and that seat is in
  [absent], it builds the `Act` frame that seat's own client would have had to send —

  ```kotlin
  Act(
      handNumber = hand.state.handNumber,
      actionSequence = decisionPointOf(hand.log.events)?.sequence ?: return current,
      action = PlayerAction.Fold(seat),
  )
  ```

  — hands it to `act(runner, seat, frame, seeds)`, and appends that step's `outbound` to what it
  has. It returns as soon as the seat on turn is one somebody is sitting in, the duel is over, or
  there is no decision point to answer.
- **The fold goes through `act`, never around it.** That is what makes `ADR-0013`'s "an ordinary
  fold" literally true: the same `guard`, the same `DefaultPokerEngine.handle`, the same
  `advance` at the hand boundary, the same frames to both seats. Nothing here constructs a
  `GameState`, an event, or a frame of its own.
- **Termination is the review's job and yours.** Guard it two ways and say so in the KDoc:
  - a fold ends a heads-up hand, so `advance` either ends the duel or deals the next hand, and the
    folding seat's stack strictly decreases by the blind it posted — a freezeout with an absent
    seat therefore ends rather than running forever;
  - if a call to `act` ever returns a runner identical to the one passed in — a refusal, which
    would leave the seat on turn unchanged — the loop **returns immediately** instead of trying
    again. A spin here is a hung server thread holding a room's mutex.
- `absent` may hold zero, one or two seats. Two is legal and is what `TASK-020812` uses when both
  players are gone.

## Out of scope

- Deciding *who* is absent, and when — that is the room's `absentSeats` (`TASK-020804`) and the
  expiry sweep (`TASK-020812`).
- Calling this from `Room` — `TASK-020808`.
- Any engine change, and any call into the engine from this file. `DefaultPokerEngine` is reached
  only through `duel/DuelAction.kt`'s `act`, which is what the two `verify` greps pin: this file
  builds the `PlayerAction.Fold` and names `DefaultPokerEngine` nowhere.

## Tests

`AbsentSeatsTest` — a new file. Fixtures, all deterministic, no clock and no `kotlin.random`:

- `seeds = HandSeedSource { 7L }`, matching `RoomDuelTest`'s `fixedSeeds`;
- `oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))`;
- `threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))`;
- an opening step from `startDuel(format, buttonSeat = 0, seed = 7L)`.

Take the seat under test from `step.runner.hand!!.state.seatToAct!!` rather than writing `0` — the
assertions below must not depend on which seat the engine happens to put on turn.

| Test | Proves |
| --- | --- |
| `aSeatSomebodyIsSittingInIsLeftAlone` | with `absent = setOf(1 - seatToAct)`, the returned step equals the one passed in: same runner, same `outbound` |
| `noAbsentSeatAtAllIsLeftAlone` | with `absent = emptySet()`, likewise — the falsifiable half of the case above |
| `anAbsentSeatOnTurnFolds` | with `absent = setOf(seatToAct)` on `oneHand`, the duel is over: `runner.hand == null` and `runner.outcome != null` |
| `theFoldReachesTheEngineAsAnOrdinaryFold` | on the same run, `runner.log.hands.single().actions.last() == PlayerAction.Fold(seatToAct)` — the fold is in the hand log exactly as a played one would be |
| `theOpponentIsToldWhatHappened` | the returned `outbound` carries at least one frame addressed to `1 - seatToAct`, so the present player sees the hand end rather than waiting on a duel that moved without them |
| `theDuelContinuesWhileTheFoldedSeatHasChips` | on `threeHands`, folding hand one for `seatToAct` leaves `runner.outcome == null`, `runner.hand!!.state.handNumber == 2`, and `runner.hand.state.seatToAct == 1 - seatToAct` — the button alternates, so the next decision belongs to the seat that is present, and the loop stops there |
| `bothSeatsAbsentRunTheDuelToItsEnd` | on `threeHands` with `absent = setOf(0, 1)`, one call returns a finished duel: `runner.outcome != null`, `runner.log.hands.size == 3`, and every hand's last action is a `PlayerAction.Fold`. Annotate `@Timeout(10)` — this is the case a non-terminating loop would hang on |
| `aFinishedDuelIsLeftAlone` | the step returned by the case above, fed back in with the same `absent`, comes out equal — nothing folds after `outcome` is set |

## Acceptance criteria

- [ ] All eight `AbsentSeatsTest` cases named above pass
- [ ] `DuelActionTest` and `RunnerLeakTest` pass with those files unchanged
- [ ] `AbsentSeats.kt` contains no `PlayerView`, no `holeCards`, and constructs no `ServerMessage`
      — it returns only what `act` gave it
- [ ] `AbsentSeats.kt` builds a `PlayerAction.Fold` and names `DefaultPokerEngine` nowhere
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
