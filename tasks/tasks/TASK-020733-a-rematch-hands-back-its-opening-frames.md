---
schema: 2
id: TASK-020733
title: A rematch hands back its opening frames, the way seating does
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, rooms, duel]
depends_on: [TASK-020715]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRematchTest'
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

`RematchResult.Agreed` carries the rematch's opening frames, and `RoomRegistry.offerRematch` hands
them back instead of discarding them — so a rematch's first hand reaches both players, exactly as a
first duel's does.

## What was found

`TASK-020725` made seating the second player return the opening hand's frames on
`JoinResult.Seated.outbound`. It deliberately left the rematch path alone, and said so:

> `offerRematch` keeps calling `withFreshRunner` and keeps discarding the frames. Say so in a
> comment naming that this is a known gap, not an oversight: `RematchResult.Agreed` carries no
> frames yet, so a rematch's opening hand reaches nobody. That is a separate ticket, not yet
> ticketed.

This is that ticket. Until it lands, two players who agree a rematch get a running duel that
neither of them can see: the runner opens the hand, builds the frames, and drops them on the floor.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RematchResult.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRematchTest.kt` | modify |

## Scope

- `RematchResult.Agreed` gains `outbound: List<Addressed>`, mirroring `JoinResult.Seated.outbound`.
  Follow that type's decision on whether the parameter defaults.
- `offerRematch` threads `withFreshRunner`'s frames onto the `Agreed` it returns, and the inline
  comment naming the gap comes out with it.
- The frames must be decided **inside the same `mutate` critical section** as the room write-back,
  for the reason `TASK-020725` gives: frames built outside it can describe a room state that never
  existed. `RoomRegistry.kt` gains no new `withLock`.
- Both seats must be told, and exactly one seat must be on turn — the same two claims
  `TASK-020725` makes about seating.

## Out of scope

- The rematch race guard: both seats must still have to offer before a rematch starts, and the
  button must still change sides (`TASK-020607`). This ticket changes what `Agreed` carries, never
  when it is returned.
- Delivering the frames to sockets. `TASK-020730` owns delivery.

## Tests

`RoomRematchTest`

| Test | Proves |
| --- | --- |
| a rematch's opening frames reach both seats | the `Agreed` result carries a `Snapshot` for each seat |
| exactly one seat is told it is its turn | the rematch's first hand puts the action on one seat only |
| the frames are the ones the runner produced | they are not rebuilt, reordered or invented in the registry |

Every existing case in `RoomRematchTest` keeps the assertion it makes today.

## Acceptance criteria

- [ ] `RematchResult.Agreed` carries the frames, and no call site discards them
- [ ] The comment naming the gap is gone, because the gap is gone
- [ ] `RoomRegistry.kt` gains no new `withLock`, and the frames are built inside `mutate`
- [ ] No frame is constructed outside the projection layer — `RunnerLeakTest` still passes
- [ ] Every existing `RoomRematchTest` case still passes, unweakened
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
