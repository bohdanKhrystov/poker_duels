---
schema: 2
id: TASK-020808
title: The room folds for a seat nobody is sitting in, so the duel never stalls
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, rooms, duel, resilience]
depends_on: [TASK-020807]
verify:
  - ./gradlew :poker-server:test --tests '*RoomAbsentSeatTest'
  - ./gradlew :poker-server:test --tests '*RoomPausedTest'
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
---

## Goal

Once a seat's grace window has run out, the duel keeps moving without it: every action from the
player who is still there is followed, in the same step, by a fold for the seat that is not — so a
duel can never be left waiting on a decision nobody will make.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomAbsentSeatTest.kt` | create |

## Scope

- `Room.act`'s last line becomes a composition of the two:

  ```kotlin
  return foldAbsent(advanceDuel(liveRunner, seat, message, seeds), absentSeats, seeds)
  ```

  `foldAbsent` (`TASK-020806`) returns its argument untouched when no absent seat is on turn, so a
  room with nobody absent behaves exactly as it does today — that is the property `RoomDuelTest`
  keeps proving, unchanged.
- One new method for the expiry path, which has no inbound action to apply:

  ```kotlin
  public fun foldAbsentSeats(seeds: HandSeedSource): DuelStep?
  ```

  `null` when the room is not `PLAYING`, carries no runner, has an empty `absentSeats`, or has
  nothing to fold because the turn belongs to the player who is still there. Otherwise the step
  `foldAbsent(DuelStep(runner, emptyList()), absentSeats, seeds)` produced. **Name it
  `foldAbsentSeats`, not `foldAbsent`** — a member with the same name as the imported top-level
  function makes overload resolution the reader's problem, and `Room.kt` already prefers the other
  fix (`import duels.poker.server.duel.act as advanceDuel`).
- Update `Room.act`'s KDoc: it currently says legality, turn order and hand advancement are all
  decided in `duels.poker.server.duel.act` and never re-decided here. That stays true — say that
  the fold-through decides nothing either, it only re-asks the same function for a seat whose
  player is gone. Note too that the fold-through runs even when the inbound action was rejected:
  the rejection leaves the turn where it was, and if that turn belongs to an absent seat it still
  has to be folded, or a bad frame from the present player would strand the duel.

## Out of scope

- Deciding when a seat becomes absent — `TASK-020805` labels it, `TASK-020812` drives it on a
  clock.
- Anything about `gracePeriods` or `isPaused` — `TASK-020807` owns that branch and this ticket
  leaves it exactly as it stands.
- `RoomRegistry` — it calls `foldAbsentSeats` in `TASK-020812`, not here.

## Tests

`RoomAbsentSeatTest` — a new file. Same fixture shape as `RoomPausedTest`, with
`threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))` so the duel has
somewhere to go after a folded hand, `seeds = HandSeedSource { 7L }`, and
`onTurn = room.runner!!.hand!!.state.seatToAct!!`.

| Test | Proves |
| --- | --- |
| `aCallFromThePresentSeatIsFollowedByTheAbsentSeatsFold` | with `absentSeats = setOf(1 - onTurn)`, a legal `Call` from `onTurn` comes back as a step whose hand one log ends with `PlayerAction.Fold(1 - onTurn)` — the turn passed to the absent seat and did not stop there |
| `theDuelIsOnTheNextHandAfterwards` | on the same step, `runner.hand!!.state.handNumber == 2` and `runner.outcome == null` |
| `oneStepCarriesBothTheActionAndTheFold` | the same call against a room with `absentSeats = emptySet()` yields strictly fewer `outbound` frames and leaves the duel on hand one — the comparison is what makes the case above falsifiable rather than merely green |
| `aRoomWithNobodyAbsentIsUnchanged` | with `absentSeats = emptySet()`, `act` returns exactly what `duels.poker.server.duel.act` returns for the same arguments: same hand number, same seat to act, no fold in the log |
| `foldAbsentSeatsFoldsTheSeatOnTurn` | with `absentSeats = setOf(onTurn)`, `foldAbsentSeats(seeds)` returns a non-null step whose hand one log ends with `PlayerAction.Fold(onTurn)` |
| `foldAbsentSeatsAnswersNullWhenTheTurnIsNotTheirs` | with `absentSeats = setOf(1 - onTurn)` and no action applied first, `foldAbsentSeats(seeds)` is `null` — there is nothing to fold yet |
| `foldAbsentSeatsAnswersNullWithNobodyAbsent` | `absentSeats = emptySet()` gives `null` |
| `foldAbsentSeatsAnswersNullForARoomThatIsNotPlaying` | a `WAITING` room with no runner, and a `FINISHED` room carrying `absentSeats = setOf(0)`, both give `null` |

## Acceptance criteria

- [ ] All eight `RoomAbsentSeatTest` cases named above pass
- [ ] `RoomPausedTest` passes with the file unchanged
- [ ] `RoomDuelTest` and `RoomReapTest` pass with those files **unchanged** — both build rooms
      through `RoomRegistry`, where `absentSeats` is empty, so neither observes the fold-through
- [ ] `Room.kt` declares no member named `foldAbsent`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
