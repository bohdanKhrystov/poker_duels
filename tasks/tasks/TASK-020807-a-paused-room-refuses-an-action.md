---
schema: 2
id: TASK-020807
title: A paused room refuses an action and moves nothing
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, rooms, duel, resilience]
depends_on: [TASK-020806]
verify:
  - ./gradlew :poker-server:test --tests '*RoomPausedTest'
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - grep -c 'isPaused' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt | grep -qx 0
---

## Goal

While one seat is inside its grace window, the duel does not move: an `Act` from the connected
opponent is answered with `Failure(DUEL_PAUSED)`, addressed to the seat that sent it, and the
runner comes back exactly as it went in.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPausedTest.kt` | create |

## Scope

- One branch in `Room.act`, between the two checks already there and the delegation to
  `duels.poker.server.duel.act`:

  ```kotlin
  if (state != RoomState.PLAYING) return null
  val liveRunner = runner ?: return null
  if (isPaused) {
      return DuelStep(liveRunner, listOf(Addressed(seat, ServerMessage.Failure(ProtocolError.DUEL_PAUSED))))
  }
  return advanceDuel(liveRunner, seat, message, seeds)
  ```

  The order matters and belongs in the KDoc: a room that is not `PLAYING` still answers `null`
  first, exactly as it does today, so a finished or abandoned room is not suddenly reported as
  paused.
- **The runner returned is the one passed in, untouched.** No `copy`, no re-derivation. That is
  what makes the story's "changes nothing" checkable: `RoomRegistry.act` writes back
  `room.copy(runner = step.runner, lastActivityAt = now)`, so an identical runner means the duel
  did not move. Only `lastActivityAt` shifts, and a `PLAYING` room is never reaped for idleness
  anyway.
- **This is the whole of the pause, and `DuelSocket.kt` does not change for it.** The refusal is an
  ordinary `Addressed`, so `TASK-020715`'s existing `deliver(step.outbound, room, connections)`
  already routes it to the writer of the seat that sent the frame and to nobody else. The `verify`
  grep pins that: the socket never asks whether a room is paused.
- `isPaused` is `gracePeriods.isNotEmpty()` (`TASK-020804`), so a seat whose window has already run
  out does **not** pause the room. Both cases are tested below; that asymmetry is the design.

## Out of scope

- Folding the absent seat when the room is not paused — `TASK-020808`, the next ticket in this
  file.
- Anything that *sets* `gracePeriods`: this ticket's tests reach the state with `copy`.
- Telling the opponent that their opponent dropped, or how long is left. No frame says that;
  `DEC-018`.

## Tests

`RoomPausedTest` — a new file, testing `Room.act` as the pure function it is. Fixture, spelled out
because every assertion below depends on the room really holding a live duel:

```kotlin
private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
private val seeds = HandSeedSource { 7L }

private fun playingRoom(): Room {
    val opened = Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), oneHand, now = 1_000L)
    val seated = (opened.join(PlayerId("guest"), now = 2_000L) as JoinResult.Seated).room
    val started = startDuel(seated.format, seated.openingButtonSeat, seed = 7L)
    return seated.copy(runner = started.runner, duelId = UUID.randomUUID())
}
```

Take `onTurn = room.runner!!.hand!!.state.seatToAct!!` and build the legal frame for it the way
`RoomDuelTest.callFrame` does — `Act(handNumber, decisionPointOf(events)!!.sequence, PlayerAction.Call(onTurn))`.

| Test | Proves |
| --- | --- |
| `theOpponentOfADroppedSeatIsRefused` | with `gracePeriods = mapOf(1 - onTurn to 9_000L)`, `act(onTurn, frame, seeds)` returns a non-null step whose `outbound` is exactly one `Addressed(onTurn, ServerMessage.Failure(ProtocolError.DUEL_PAUSED))` |
| `aRefusedActionMovesTheDuelNowhere` | on the same call, `assertSame(room.runner, step.runner)` — the runner is the identical instance, so nothing was applied and no hand advanced |
| `nothingIsSentToTheSeatThatIsGone` | `step.outbound.none { it.seat == 1 - onTurn }` — the absent seat is told nothing, and the refusal is not broadcast |
| `aSeatInsideItsOwnWindowIsRefusedToo` | with `gracePeriods = mapOf(onTurn to 9_000L)`, the same frame is refused the same way: pausing is a fact about the room, not about who is speaking |
| `aRoomWhoseSeatIsAlreadyAbsentIsNotPaused` | with `absentSeats = setOf(1 - onTurn)` and empty `gracePeriods`, the same frame is **applied**: the step's runner is a different instance from `room.runner` and no `Failure` appears in `outbound`. This is the falsifying case for the branch above |
| `aFinishedRoomStillAnswersNull` | `playingRoom().copy(state = RoomState.FINISHED, gracePeriods = mapOf(0 to 9_000L)).act(...)` returns `null`, not a paused refusal |
| `aRoomWithNoRunnerStillAnswersNull` | a `WAITING` room with no runner returns `null` |

## Acceptance criteria

- [ ] All seven `RoomPausedTest` cases named above pass
- [ ] `RoomDuelTest` and `RoomReapTest` pass with those files **unchanged** — every room they build
      has an empty `gracePeriods`, so neither observes this branch
- [ ] `DuelSocket.kt` does not mention `isPaused`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
