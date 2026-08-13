---
schema: 2
id: TASK-020707
title: Fold a finished hand back into the duel, deal the next one, or end the duel
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, engine-integration]
depends_on: [TASK-020706]
verify:
  - ./gradlew :poker-server:test --tests '*DuelProgressTest'
  - ./gradlew :poker-server:check
---

## Goal

`advance(runner, seeds)` takes a duel whose hand has just ended and either deals on until some seat
has a decision to make, or ends the duel with the engine's own `MatchFinished` — every finished
hand's `HandLog` landing in the `MatchLog` on the way.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelProgress.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelProgressTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt` (`recordHand`),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchEvent.kt` (`matchFinishedEvent(match,
sequence)`), `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt` (a seat too short
for its blind posts all-in, and a hand whose seat on turn cannot act is run straight out to
showdown — the case the loop below exists for),
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelStart.kt` (`openHand`),
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` (`framesFor`).

## Scope

- Package `duels.poker.server.duel`. One declaration, KDoc included:

  ```kotlin
  public fun advance(runner: DuelRunner, seeds: HandSeedSource): DuelStep
  ```

- `require` that `runner.hand` is non-null and `runner.hand.state.isHandOver`; calling `advance` on a
  live hand is a server bug, not an input to handle.
- It **loops**, because opening a hand can finish it: a seat too short for its blind posts all-in and
  `startHand` runs the board straight out. Returning such a hand as "live" would stall the duel
  forever — no seat to act, so no inbound frame could ever move it. Each turn of the loop:
  1. `val match = recordHand(current.match, hand.state)` — stacks, hand count and button all move
     there and nowhere else.
  2. `val hands = current.log.hands + hand.log`.
  3. `outcomeOf(match)` non-null → return a runner with `hand = null`, that outcome, and
     `log.copy(hands = hands, events = log.events + matchFinishedEvent(match, log.events.size)!!)`,
     carrying whatever outbound has accumulated.
  4. Otherwise `openHand(match, seeds.newHandSeed())`, append
     `framesFor(next.state, next.log.events, next.log.events)` to the outbound, and return that
     runner **unless** `next.state.isHandOver`, in which case go round again.
- A local `var` and a `MutableList` inside the function body are fine; `DuelRunner` and every value
  it holds stay immutable.
- **Postcondition**, asserted by its own test: the returned runner either has no live hand, or has
  one whose `state.seatToAct` is non-null.
- No `ServerMessage` is emitted for the end of the duel: none can carry a `MatchEvent` today, so
  there is nothing correct to send. That gap is `DEC-015` and `TASK-020715`; inventing a message
  here would answer an open decision in the least visible place.
- No arithmetic on a stack, a blind, a button or a hand number: `recordHand`, `outcomeOf`,
  `matchFinishedEvent` and `openHand` own every one of them.

## Out of scope

- Deciding *when* a hand has ended and calling this — `TASK-020708`.
- Publishing the finished duel to a sink — the port is `TASK-020709`, the call is `TASK-020714`.
- Telling a client the duel is over — `DEC-015`, `TASK-020715`.

## Tests

`DuelProgressTest`, JUnit 5, package `duels.poker.server.duel`. Two helpers, since `act` does not
exist yet:

```kotlin
private fun DuelRunner.play(action: PlayerAction): DuelRunner {
    val live = hand!!
    val result = DefaultPokerEngine.handle(live.state, action)
    val log = live.log.copy(actions = live.log.actions + action, events = live.log.events + result.events)
    return copy(hand = LiveHand(result.newState, log))
}

private fun seeds(first: Long): HandSeedSource {
    var rng = SplitMix64Rng(first)
    return HandSeedSource { rng.nextLong().let { rng = it.next; it.value } }
}
```

Fixtures. `flat` is `DuelFormat(startingStack = 10_000, blinds = BlindSchedule(listOf(BlindLevel(50,
100)), 10), endCondition = EndCondition.Freezeout)`; `oneHand` is `flat` with
`EndCondition.FixedHands(1)`. A hand ends deterministically with a preflop fold by the seat on turn.

For `ahandThatDealsItselfOutIsRecordedToo`, start from
`MatchState(flat, handsPlayed = 0, stacks = listOf(120, 300), buttonSeat = 1)` — `MatchState`
constrains stacks only to be non-negative — open hand 1 with `openHand`, and play
`PlayerAction.Call(1)` then `PlayerAction.Fold(0)`. Seat 0 is left with 20 chips and takes the
button for hand 2, so hand 2's small blind puts it all-in and the hand deals itself out.

| Test | Proves |
| --- | --- |
| `theFinishedHandJoinsTheMatchLog` | after a fold and `advance`, `log.hands` has one entry whose `handNumber` is 1 and whose `actions` is the single fold |
| `theNextHandIsDealtWithTheButtonMoved` | the new `hand!!.log.handNumber == 2` and `hand!!.log.buttonSeat == 1` |
| `theNextHandCarriesTheStacksTheLastOneLeft` | `hand!!.log.stacks` equals the finished hand's final seat stacks and sums to 20 000 |
| `theNextHandRecordsTheSeedItWasGiven` | with `HandSeedSource { 99L }`, `hand!!.log.seed == 99L` |
| `bothSeatsSeeTheNextHandOpen` | the outbound holds a `Snapshot` for each seat and exactly one `YourTurn` |
| `theReturnedHandAlwaysHasASeatToAct` | for every fixture in this class, `runner.hand == null || runner.hand!!.state.seatToAct != null` |
| `ahandThatDealsItselfOutIsRecordedToo` | from the 120/300 fixture, `advance` returns `log.hands.size >= 2`, the outbound contains a `Snapshot` with `view.handNumber == 2`, and the postcondition above holds |
| `afinishedDuelKeepsNoLiveHand` | with `oneHand`, after `advance` the runner's `hand` is null and `outcome == outcomeOf(runner.match)` |
| `afinishedDuelRecordsMatchFinishedOnce` | `log.events` is exactly one `MatchFinished`, with `sequence == 0` and that same outcome |
| `advancingALiveHandFailsLoudly` | `advance` on a runner whose hand is not over throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelProgressTest.theFinishedHandJoinsTheMatchLog` passes
- [ ] `DuelProgressTest.theNextHandIsDealtWithTheButtonMoved` passes
- [ ] `DuelProgressTest.theNextHandCarriesTheStacksTheLastOneLeft` passes
- [ ] `DuelProgressTest.theNextHandRecordsTheSeedItWasGiven` passes
- [ ] `DuelProgressTest.bothSeatsSeeTheNextHandOpen` passes
- [ ] `DuelProgressTest.theReturnedHandAlwaysHasASeatToAct` passes
- [ ] `DuelProgressTest.ahandThatDealsItselfOutIsRecordedToo` passes
- [ ] `DuelProgressTest.afinishedDuelKeepsNoLiveHand` passes
- [ ] `DuelProgressTest.afinishedDuelRecordsMatchFinishedOnce` passes
- [ ] `DuelProgressTest.advancingALiveHandFailsLoudly` passes
- [ ] `DuelProgress.kt` contains no `MatchFinished(` constructor call — only `matchFinishedEvent`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
