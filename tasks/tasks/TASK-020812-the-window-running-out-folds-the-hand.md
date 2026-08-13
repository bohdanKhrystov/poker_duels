---
schema: 2
id: TASK-020812
title: The window running out folds the hand, and both seats gone ends the room
type: task
status: ready
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, rooms, duel, resilience]
depends_on: [TASK-020811]
verify:
  - ./gradlew :poker-server:test --tests '*GraceExpiryTest'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - grep -c 'holder.mutex.withLock {' poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt | grep -qx 2
---

## Goal

One sweep on the injected clock ends every grace window that has run out: the seat becomes absent
and its hand is folded as an ordinary action, or — if nobody is left at the table — the room is
abandoned and nothing keeps running.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/GraceExpiry.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceExpiryTest.kt` | create |

## Scope

- A result type in its own file, alongside `JoinResult` and `Resumption`:

  ```kotlin
  public data class GraceExpiry(val room: Room, val outbound: List<Addressed>)
  ```

- One public sweep, shaped like `reap()` — `now` read **once** from the clock so every room in the
  pass is judged against the same instant:

  ```kotlin
  public suspend fun expireGracePeriods(): List<GraceExpiry>
  ```

  Two passes, in this order:
  1. For each room, an unlocked `gracePeriods.isEmpty()` early-continue (the same cheap pre-check
     `reap` makes, re-decided under the lock), then a `mutate` that applies
     `room.expireGrace(now)`. If nothing ran out, write nothing back. If something did, and the
     room has a guest and `absentSeats == setOf(0, 1)`, write back `expired.abandon(now)` instead —
     both players are gone, so there is no duel left to fold and the room becomes reapable under
     the existing `finishedMillis` rule rather than growing a second timer.
  2. For each room the first pass changed, `act(code) { it.foldAbsentSeats(handSeeds) }`. Going
     through `act` is not a shortcut, it is the requirement: `act` is where a duel that just ended
     is written back as `FINISHED`, claimed in `recording`, handed to `DuelResultSink` outside the
     lock, and unclaimed if that throws. A fold that ended a duel must reach the sink exactly as a
     played one does, and re-implementing that here would be a second copy of the hardest code in
     the file.
- No new lock. The file has exactly two `holder.mutex.withLock {` call sites — `reap` and `mutate`
  — and still has two afterwards; everything here goes through `mutate` and `act`.
- `reap`'s KDoc currently says a silent live duel is `ADR-0013`'s grace period "which ends by
  calling [abandon]". Half of that is now wrong: a window running out ends by *folding*, and only a
  room whose seats are both gone is abandoned. Correct that sentence and point it at this method.

## Out of scope

- Anything that calls this on a timer. Nothing schedules `reap()` either, and what drives both in
  production is **`DEC-019`**, registered in `docs/adr/README.md` and not answered by this story.
  The sweep is driven by its tests, exactly as `reap` has been since `TASK-020612`.
- Telling the opponent that a fold was a timeout rather than a decision. It is an ordinary fold and
  the frames say nothing extra — `ADR-0013`, and `DEC-018` for anything richer.
- Notifying anyone that a room was abandoned.

## Tests

`GraceExpiryTest` — a new file, built like `RoomReapTest`: a `MutableClock`, an explicit
`RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000, disconnectGraceMillis = 30_000)`, a
`RoomCodeSource` with fixed codes and `HandSeedSource { 7L }`. Seat both players in every case.
Take `onTurn = registry.get(code)!!.runner!!.hand!!.state.seatToAct!!` and disconnect **the player
in that seat** wherever a fold is expected at expiry — the turn is what decides when the fold
lands, and a test that drops the other seat would assert the wrong thing and pass for the wrong
reason. Use `FixedHands(3)` where the duel must continue and `FixedHands(1)` where it must end.

| Test | Proves |
| --- | --- |
| `nothingExpiresWhileTheWindowIsStillRunning` | disconnect at `0`, advance `29_999`, sweep returns an empty list, the room is still `isPaused`, and its `runner` is the identical instance |
| `theWindowRunningOutFoldsTheSeatOnTurn` | advance `30_000`, sweep returns one `GraceExpiry`, and the room's hand-one log ends with `PlayerAction.Fold(onTurn)` |
| `theDuelCarriesOnWithoutTheAbsentPlayer` | on `FixedHands(3)`, after that sweep the room is still `PLAYING`, `isPaused` is `false`, and the runner is on hand 2 |
| `theSweepDoesNotFireTwice` | a second `expireGracePeriods()` at the same instant returns an empty list and changes nothing |
| `aSeatOffTurnIsFoldedWhenTheTurnReachesIt` | disconnect `1 - onTurn` instead: the sweep folds nothing and returns an expiry with no fold in the log, but the room is no longer paused; the present player's next action through `registry.act` then comes back with that seat's fold |
| `aPlayerWhoCameBackInTimeIsNotFolded` | disconnect at `0`, `resume(code, that player)` at `29_000`, advance to `30_000`: the sweep returns an empty list and no fold appears in the log |
| `bothSeatsGoneEndsTheRoom` | disconnect both, advance past both deadlines: the room is `ABANDONED`, its `GraceExpiry.outbound` is empty, and after advancing `finishedMillis` a `reap()` removes it — nothing is left running |
| `aFoldThatEndsTheDuelIsRecordedOnce` | on `FixedHands(1)` with a counting `DuelResultSink` (the fixture `RoomDuelTest` uses), the expiring fold leaves the room `FINISHED` and the sink called exactly once, with the room's `duelId` |

## Acceptance criteria

- [ ] All eight `GraceExpiryTest` cases named above pass
- [ ] `RoomReapTest` and `RoomDuelTest` pass with those files unchanged
- [ ] `RoomRegistry.kt` still contains exactly two `holder.mutex.withLock {` call sites
- [ ] No test in `GraceExpiryTest` calls `Thread.sleep`; every instant comes from `MutableClock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
