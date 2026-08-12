---
schema: 2
id: TASK-020606
title: Finish, abandon and touch a room
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, rooms]
depends_on: [TASK-020605]
verify:
  - ./gradlew :poker-server:test --tests '*RoomLifecycleTest'
  - ./gradlew :poker-server:check
---

## Goal

A room can reach its end states — `FINISHED` when the duel is over, `ABANDONED` when its players
are gone — and any activity can push its idle clock forward, which is what makes reaping possible
without reaping a live duel.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomLifecycleTest.kt` | create |

`RoomTest` and `RoomJoinTest` assert construction and joining, neither of which changes here, so
both files are untouched and every assertion in them stands.

## Scope

- Three methods on `Room`, KDoc on each:

  ```kotlin
  public fun finish(now: Long): Room
  public fun abandon(now: Long): Room
  public fun touch(now: Long): Room
  ```

- `finish` is `PLAYING` → `FINISHED`, keeping `guest`, `format`, `match` and `openingButtonSeat`
  (the rematch in `TASK-020607` reads all four), clearing nothing, with `lastActivityAt = now`.
  Calling it in any other state is a **server bug, not a player action**: `require(state == PLAYING)`
  and let it throw `IllegalStateException` via `check`.
- `abandon` is legal from `WAITING`, `PLAYING` and `FINISHED` → `ABANDONED`, clearing
  `rematchOffers` (an offer cannot outlive the room it was made in) and setting
  `lastActivityAt = now`. Abandoning an already-`ABANDONED` room returns `this` **unchanged,
  timestamp included** — both players leaving must not keep resetting the reaping clock on a dead
  room, which is exactly how an abandoned room would leak forever.
- `touch` returns `copy(lastActivityAt = now)` and changes nothing else. It exists for `STORY-0207`,
  which will call it on every action inside a live room.
- Use `check`/`require` with a message; do not introduce a result type for these three.

## Out of scope

- Rematch — `TASK-020607`.
- Deciding *when* to call these — the registry exposes them in `TASK-020611`, and `STORY-0207` and
  `STORY-0208` decide the moments.
- The reaping rule itself — `TASK-020612`.

## Tests

`RoomLifecycleTest`, JUnit 5, package `duels.poker.server.room`. Build a playing room with
`Room.open(...).join(guest, now = 1_000L)` and unwrap the `Seated`.

| Test | Proves |
| --- | --- |
| `finishMovesAPlayingRoomToFinishedAndKeepsTheMatch` | `state == FINISHED`, `match` is the same value as before, `guest` still seated, `lastActivityAt == 2_000L` |
| `finishingARoomThatIsNotPlayingThrows` | `finish` on a `WAITING` room and on an already `FINISHED` room both throw `IllegalStateException` |
| `abandonWorksFromWaitingPlayingAndFinished` | all three give `state == ABANDONED` with `lastActivityAt` at the passed `now` |
| `abandonClearsAnyRematchOffer` | a `FINISHED` room copied with `rematchOffers = setOf(host)` abandons to `rematchOffers.isEmpty()` |
| `abandoningAnAbandonedRoomChangesNothing` | the second `abandon(9_999L)` returns a room equal to the first, `lastActivityAt` included |
| `touchOnlyMovesTheClock` | `room.touch(5_000L) == room.copy(lastActivityAt = 5_000L)` |

## Acceptance criteria

- [ ] `RoomLifecycleTest.finishMovesAPlayingRoomToFinishedAndKeepsTheMatch` passes
- [ ] `RoomLifecycleTest.finishingARoomThatIsNotPlayingThrows` passes
- [ ] `RoomLifecycleTest.abandonWorksFromWaitingPlayingAndFinished` passes
- [ ] `RoomLifecycleTest.abandonClearsAnyRematchOffer` passes
- [ ] `RoomLifecycleTest.abandoningAnAbandonedRoomChangesNothing` passes
- [ ] `RoomLifecycleTest.touchOnlyMovesTheClock` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
