---
schema: 2
id: TASK-020810
title: The frames a returning player is entitled to, rebuilt through the projection layer
type: task
status: done
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, resilience, security]
depends_on: [TASK-020809]
verify:
  - ./gradlew :poker-server:test --tests '*DuelResumeTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:test --tests '*DuelBroadcastTest'
  - grep -c 'PlayerView' poker-server/src/main/kotlin/duels/poker/server/duel/DuelResume.kt | grep -qx 0
---

## Goal

Given a duel and a seat, the frames that seat may see *right now* — rebuilt from the live state
through `PlayerView.of` and the per-seat filter, never a cached payload and never the other seat's
copy. This is the half of reconnection that `ADR-0013` calls out by name: a reconnect can never
reveal the opponent's hole cards.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelResume.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelResumeTest.kt` | create |

Read `duel/DuelTurn.kt` (`framesFor`), `duel/Addressed.kt` (`broadcast`, `finishedFrames`) and
`test/duel/PlayedDuel.kt` (`playDuel`, for the finished-duel fixture).

## Scope

- One public function:

  ```kotlin
  public fun resumeFrames(runner: DuelRunner, seat: Int): List<Addressed>
  ```

  A live hand gives `framesFor(hand.state, newEvents = emptyList(), handEvents = hand.log.events)`;
  a finished duel gives `finishedFrames(outcome)`. Either way the result is filtered to
  `it.seat == seat` before it is returned. `seat` outside `0..1` is `require`d against.
- **`newEvents` is deliberately empty.** `broadcast` emits no `Events` frame when a seat's visible
  events are empty, so a resume carries a `Snapshot` — the authoritative last word on state — plus
  a `YourTurn` if the returning player is the one holding everybody up. Replaying the hand's events
  would be a second, redundant description of the same facts and would have to be re-filtered; the
  snapshot already says everything. Put that reasoning in the KDoc: the next person to read this
  will wonder why the log is not resent.
- **This file builds no frame of its own.** It calls the two functions that already exist and
  filters the result. `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` allows
  `PlayerView.of(`, `ServerMessage.Snapshot(`, `ServerMessage.Events(` and
  `ServerMessage.DuelFinished(` in `duel/Addressed.kt` and **nowhere else** — that test goes red if
  this file constructs any of them, and the `verify` grep says the same thing sooner.
- A finished duel has no `GameState` left to project, so the resume is the `DuelFinished` frame
  alone. That is `STORY-0208`'s "a reconnect after the duel has already finished gets the finished
  state, not a resumed duel", and it needs no extra code — say so in the KDoc rather than inventing
  a final snapshot.
- `runner.outcome` on a duel with no hand is non-null by `DuelRunner`'s own `init`; read it with
  `checkNotNull` and a message, never `!!`.

## Out of scope

- Deciding *whether* somebody may resume — `TASK-020811` answers that from the room's seating.
- Delivery. These are `Addressed` values; `SeatDelivery.deliver` routes them, unchanged.
- Any new `ServerMessage`.

## Tests

`DuelResumeTest` — a new file. Two fixtures, both deterministic:

- a live duel: `startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L).runner`, with
  `onTurn = runner.hand!!.state.seatToAct!!`;
- a finished duel: `playDuel(seed = 7L, format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))).runner`,
  asserted to have `hand == null` and `outcome != null` before anything else, so the finished cases
  cannot pass against a duel that is secretly still running.

| Test | Proves |
| --- | --- |
| `aReturningSeatSeesItsOwnCards` | seat 0's resume has exactly one `Snapshot`, whose `view.viewer.holeCards` has size 2 |
| `aReturningSeatNeverSeesTheOpponentsCards` | in that same snapshot, `view.opponent.holeCards` is empty — and the same for seat 1 |
| `nothingIsAddressedToTheOtherSeat` | `resumeFrames(runner, 0).all { it.seat == 0 }`, and likewise for seat 1. Falsifiable: `framesFor` builds both seats' frames, so an unfiltered implementation returns four |
| `theSeatOnTurnIsPrompted` | `resumeFrames(runner, onTurn)` holds exactly one `YourTurn`, whose `handNumber` and `actionSequence` match the hand's current decision point |
| `theSeatNotOnTurnIsNotPrompted` | `resumeFrames(runner, 1 - onTurn)` holds no `YourTurn` |
| `noEventsAreReplayed` | neither seat's resume holds a `ServerMessage.Events`, asserted after `assertTrue(runner.hand!!.log.events.isNotEmpty())` — without that guard the assertion would hold for a duel with no events at all |
| `aFinishedDuelResumesAsItsOutcome` | each seat's resume from the finished fixture is exactly one `DuelFinished` carrying `runner.outcome`, and holds no `Snapshot` and no `YourTurn` |
| `aSeatOutsideTheTableIsRefused` | `resumeFrames(runner, 2)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] All eight `DuelResumeTest` cases named above pass
- [ ] `RunnerLeakTest` passes with the file unchanged, including
      `onlyTheBroadcastFileBuildsAStateCarryingFrame` and `noServerSourceFileTouchesHoleCards`
- [ ] `DuelBroadcastTest` passes with the file unchanged
- [ ] `DuelResume.kt` contains no `PlayerView` and no `!!`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
