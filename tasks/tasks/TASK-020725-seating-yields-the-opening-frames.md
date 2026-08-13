---
schema: 2
id: TASK-020725
title: Seating the second player hands back the opening hand's frames
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, rooms, duel]
depends_on: [TASK-020724]
verify:
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:test --tests '*RoomRegistryJoinTest'
  - ./gradlew :poker-server:test --tests '*RoomJoinTest'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry.join` returns the frames the opening hand produced, so the two players can actually be
shown the hand that just started.

`withFreshRunner` calls `startDuel`, which returns a `DuelStep` — a runner **and** the opening
`Events`, `Snapshot` and `YourTurn` for both seats — and then keeps only the runner and drops the
frames on the floor. Nothing else can rebuild them: they come from the engine's projection of a hand
that has already been dealt.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/JoinResult.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDuelTest.kt` | modify |

`RoomJoinTest`, `RoomRegistryJoinTest`, `RoomLifecycleTest`, `RoomRematchTest` and `RoomReapTest`
all reach `JoinResult.Seated` through `.room` or `is`, never by equality or destructuring, so the
new property must be **defaulted** and they must pass unchanged. They are in `verify:`, not in the
budget. If one of them stops compiling, the default is missing.

## Scope

- `JoinResult.Seated` gains `val outbound: List<Addressed> = emptyList()`, KDoc'd as "the frames the
  duel's opening hand produced, empty when this result came from the pure `Room.join`, which deals
  no hand".
- The default is what keeps `Room.join` — a pure function that starts a `MatchState` but no hand —
  compiling and correct: it seats a guest and produces no frames, and says so.
- `RoomRegistry.withFreshRunner` returns the room **and** `startDuel`'s `outbound`, and
  `RoomRegistry.join` puts them into the `JoinResult.Seated` it returns.
- `offerRematch` keeps calling `withFreshRunner` and keeps discarding the frames. Say so in a
  comment naming that this is a known gap, not an oversight: `RematchResult.Agreed` carries no
  frames yet, so a rematch's opening hand reaches nobody. That is a separate ticket, not yet
  ticketed, and widening it here would drag `RematchResult`, `RoomRematchTest` and `RoomRegistry`'s
  rematch race guard into this diff.
- No new `withLock`. The frames are produced inside the same critical section that already seats the
  guest and attaches the runner, and travel out on the value `mutate` already returns.

## Out of scope

- Sending the frames anywhere — `TASK-020731`.
- The rematch path, as above.
- Any change to `startDuel`, `Room` or the engine.

## Tests

`RoomDuelTest` — new cases; every existing case is untouched.

| Test | Proves |
| --- | --- |
| `seatingTheGuestHandsBackTheOpeningFrames` | `join` returns a `Seated` whose `outbound` is non-empty and contains a `Snapshot` for seat 0 and one for seat 1 |
| `theOpeningFramesAreTheOnesTheRunnerProduced` | with a fixed `HandSeedSource`, the returned `outbound` equals `startDuel(format, openingButtonSeat, thatSeed).outbound` |
| `exactlyOneSeatIsToldItIsItsTurn` | the returned `outbound` contains exactly one `ServerMessage.YourTurn`, addressed to the seat the engine says is on turn |
| `apureRoomJoinYieldsNoFrames` | `Room.join(guest, now)` — the pure one — returns a `Seated` whose `outbound` is empty |

## Acceptance criteria

- [ ] `RoomDuelTest.seatingTheGuestHandsBackTheOpeningFrames` passes
- [ ] `RoomDuelTest.theOpeningFramesAreTheOnesTheRunnerProduced` passes
- [ ] `RoomDuelTest.exactlyOneSeatIsToldItIsItsTurn` passes
- [ ] `RoomDuelTest.apureRoomJoinYieldsNoFrames` passes
- [ ] Every test method already in `RoomDuelTest` is byte-identical in the diff
- [ ] `RoomJoinTest`, `RoomRegistryJoinTest`, `RoomLifecycleTest`, `RoomRematchTest` and
      `RoomReapTest` pass with all five files unchanged
- [ ] `RoomRegistry.kt` still contains exactly one `withLock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
