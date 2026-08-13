---
schema: 2
id: TASK-020714
title: Give the live DuelRunner a home in the room, and publish the duel when it ends
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, duel, rooms, concurrency, blocked]
depends_on: [TASK-020713]
verify:
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:test --tests '*RoomRegistryConcurrencyTest'
  - ./gradlew :poker-server:check
---

## Goal

A room holds the live `DuelRunner` between frames, exactly one caller at a time moves it, and the
finished duel reaches `DuelResultSink` exactly once.

## Blocked on DEC-013

**Do not start this ticket.** No ADR answers the question, and guessing it here would decide it in
the least visible place. It is registered as `DEC-013` in
[`docs/adr/README.md`](../../docs/adr/README.md) and discussed in
[`STORY-0206`](../stories/STORY-0206-rooms-and-matchmaking.md).

> **DEC-013** — is a per-room `Mutex` enough to serialise a room, or does the room become a
> channel-fed actor once a duel runs inside it?

What `STORY-0206` shipped is a `ConcurrentHashMap<RoomCode, Holder>` where each holder carries its
own `Mutex` and **every** mutation goes through one private `mutate` helper — a single `withLock`
call site, whose atomicity `TASK-020610` and `TASK-020614` prove by racing a hundred callers. That
property is not negotiable here: whatever this ticket does, the registry must still have exactly
one place that takes a lock.

The answer also settles where the runner lives, which is the part this ticket cannot invent:

- **A field on `Room`, beside its `MatchState`.** Everything stays in one lock and one type, but
  `Room` is a pure, tested value with seating invariants, and `Room.match` becomes a second copy of
  something `DuelRunner.match` already holds — two sources of truth for the stacks.
- **`Room.match` replaced by the runner.** No duplication, but `TASK-020604`'s invariants and
  `RoomTest`, `RoomJoinTest`, `RoomRematchTest` all move with it, which is a diff far past this
  ticket's budget and probably its own ticket.
- **A separate registry keyed by `RoomCode`.** `Room` is untouched and the runner's lifetime is
  explicit, but "one room, one writer" now spans two structures, and `STORY-0208` will have to
  reconnect a player against both.

`STORY-0208` (a disconnect that becomes a fold) reads whatever this decision produces, which is why
`DEC-013` is due before this story rather than before v0.2.

## Files

Provisional — the third entry depends on the answer, and the ticket is rewritten by
`/plan-story STORY-0207` once `DEC-013` is recorded.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDuelTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |

`RoomRegistryConcurrencyTest` is in `verify` rather than in the budget: it must keep passing
unchanged, because nothing here may weaken the single-writer property it proves.

## Scope

- Written when `DEC-013` is answered. Whatever the answer:
  - the runner is created with `startDuel(room.format, room.openingButtonSeat, seeds.newHandSeed())`
    when the second seat is taken, and again on an agreed rematch — `STORY-0206` owns the agreement,
    this ticket only starts the duel the room asks for;
  - hand seeds come from an injected `HandSeedSource`, defaulting to `SecureHandSeedSource`, so a
    test can play a reproducible duel through a real room;
  - `act` is called under whatever serialisation the decision names, and no two callers can move one
    duel at once;
  - when a step returns a runner with a non-null `outcome`, the room is finished and
    `DuelResultSink.record` is called **exactly once** with the two seated `PlayerId`s in seat order
    — a second frame, a retry or a rematch must not record a second result;
  - `RoomRegistry` still has exactly one `withLock` call site.
- The engine, the runner and their tests are untouched: this is wiring.

## Out of scope

- Reading frames off a socket and writing them back — `TASK-020715`.
- Any implementation of `DuelResultSink` — `STORY-0210`.
- Disconnects, timeouts and resync — `ADR-0013`, `STORY-0208`.

## Tests

To be named once `DEC-013` is answered. `RoomDuelTest` will assert, at minimum: a duel starts when
the second player joins; an `Act` routed through the room moves the duel and returns the same
frames the runner produced; two concurrent callers on one room never interleave; and a finished duel
calls a recording sink exactly once, with the winner's `PlayerId`.

## Acceptance criteria

- [ ] `DEC-013` is answered and recorded before any code is written
- [ ] The tests named in this ticket after that answer all pass
- [ ] `RoomRegistry.kt` contains exactly one `withLock`
- [ ] `RoomRegistryConcurrencyTest` and `RoomRegistryJoinTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
