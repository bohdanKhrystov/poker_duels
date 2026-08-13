---
schema: 2
id: TASK-020703
title: The seat on turn gets YourTurn with the engine's legal actions, and the other seat gets nothing
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, duel, protocol]
depends_on: [TASK-020702]
verify:
  - ./gradlew :poker-server:test --tests '*DuelTurnTest'
  - ./gradlew :poker-server:check
---

## Goal

`turnFor(state, handEvents)` addresses one `YourTurn` to `state.seatToAct`, carrying the sequence
of the decision point the client must echo back and the action set `legalActions(state)` computed,
and `framesFor` composes it with `broadcast` into the complete outbound for one transition.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelTurnTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/game/BettingRules.kt` (`legalActions(state)`),
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`
(`YourTurn(handNumber, actionSequence, legalActions)`),
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelBroadcast.kt` (`Addressed`, `broadcast`).

## Scope

- Package `duels.poker.server.duel`. Three declarations, KDoc included:

  ```kotlin
  public fun decisionPointOf(handEvents: List<GameEvent>): ActionOn?

  public fun turnFor(state: GameState, handEvents: List<GameEvent>): Addressed?

  public fun framesFor(
      state: GameState,
      newEvents: List<GameEvent>,
      handEvents: List<GameEvent>,
  ): List<Addressed>
  ```

- `decisionPointOf` is `handEvents.filterIsInstance<ActionOn>().lastOrNull()`. The decision point is
  found by *looking for the last `ActionOn`*, never by arithmetic on `state.eventCount`: which event
  the engine emits last is not a contract this module may lean on.
- `turnFor` returns `null` when `state.seatToAct` is `null` or `decisionPointOf` is `null` — a hand
  that is over, or a street being run out, has nobody to prompt. Otherwise it returns
  `Addressed(seatToAct, ServerMessage.YourTurn(state.handNumber, decisionPoint.sequence,
  legalActions(state)))`.
- If the last `ActionOn` names a seat other than `state.seatToAct`, `check` fails with a message
  naming both. That combination is a server bug, and failing loudly beats prompting the wrong seat.
- `framesFor` is `broadcast(state, newEvents, handEvents) + listOfNotNull(turnFor(state,
  handEvents))`, so the prompt always follows the state it is a prompt about.
- No rules of poker here: the allowed set, the amounts and the seat all come from
  `legalActions(state)` unchanged. No arithmetic on a blind, a stack or a pot appears in this file.

## Out of scope

- Deciding whether an inbound frame's `actionSequence` matches — `TASK-020706` is the guard, and it
  calls `decisionPointOf`.
- Any timer on a turn. `ADR-0013` keeps timing in the server and `STORY-0208` owns it; nothing here
  reads a clock.

## Tests

`DuelTurnTest`, JUnit 5, package `duels.poker.server.duel`. Fixture as in `DuelBroadcastTest`:
`startHand(1, 0, listOf(1_000, 1_000), 50, 100, SplitMix64Rng(7))`, whose opening leaves the button
seat on turn with an `ActionOn` as the last event.

| Test | Proves |
| --- | --- |
| `theSeatOnTurnIsTheOneAddressed` | `turnFor(...)!!.seat == state.seatToAct` |
| `theOtherSeatGetsNoTurnFrame` | `framesFor(...)` contains exactly one `YourTurn`, addressed to `state.seatToAct` |
| `theActionSequenceIsTheLastActionOnsSequence` | the `YourTurn.actionSequence` equals `events.filterIsInstance<ActionOn>().last().sequence`, and `handNumber` equals `state.handNumber` |
| `theLegalActionsAreTheEnginesOwn` | `YourTurn.legalActions == legalActions(state)` |
| `aHandWithNobodyToActPromptsNobody` | for a state copied with `seatToAct = null`, `turnFor` is `null` and `framesFor` contains no `YourTurn` |
| `aDecisionPointNamingAnotherSeatFailsLoudly` | with `handEvents` whose last `ActionOn` names `1 - state.seatToAct!!`, `turnFor` throws `IllegalStateException` |
| `framesForPutsTheTurnAfterTheSnapshot` | in `framesFor(...)`, the index of the `YourTurn` is greater than the index of every `Snapshot` |

## Acceptance criteria

- [ ] `DuelTurnTest.theSeatOnTurnIsTheOneAddressed` passes
- [ ] `DuelTurnTest.theOtherSeatGetsNoTurnFrame` passes
- [ ] `DuelTurnTest.theActionSequenceIsTheLastActionOnsSequence` passes
- [ ] `DuelTurnTest.theLegalActionsAreTheEnginesOwn` passes
- [ ] `DuelTurnTest.aHandWithNobodyToActPromptsNobody` passes
- [ ] `DuelTurnTest.aDecisionPointNamingAnotherSeatFailsLoudly` passes
- [ ] `DuelTurnTest.framesForPutsTheTurnAfterTheSnapshot` passes
- [ ] `DuelTurn.kt` contains no reference to `eventCount`, and no `+` or `-` applied to a chip
      amount, a blind or a stack
- [ ] `DuelBroadcastTest` is not touched and every assertion in it still stands — this ticket adds
      a file and changes no existing behaviour
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
