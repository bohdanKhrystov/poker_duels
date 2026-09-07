---
schema: 2
id: TASK-141603
title: Opening a room is find-or-create, serialised per player
type: task
status: done
parent: STORY-1416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, room, concurrency]
depends_on: [TASK-141602]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomRegistryHeldOrOpenTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomRegistryHeldOrOpenTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==5 else 1)"
  - sh -c 'test "$(grep -cF "public suspend fun heldOrOpen(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - sh -c 'test "$(grep -cF "absent: () -> T," poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - sh -c 'test "$(grep -cF "block: (Room) -> Pair<Room?, T>," poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - sh -c '! grep -qE "(absent|block): suspend" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt'
  - sh -c 'test "$(grep -c "\.withLock {" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 3'
  - sh -c 'test "$(grep -c "tryLock" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 0'
  - sh -c '! grep -q "suspend fun heldRoom" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RoomRegistry.heldOrOpen(host)` answers the room `host` already holds, or opens a fresh one, and two
coroutines asking at the same moment for the same player get **one** room rather than two. Nothing
calls it yet.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldOrOpenTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryConcurrencyTest.kt` | read |
| `docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md` | read |

## Scope

- Add one public, **suspending** operation:
  `public suspend fun heldOrOpen(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room`.
  Its body is `heldRoom(host) ?: create(host, format)`, executed under a **per-player lock**.
- The lock is a **second, private** fixed array of `Mutex` on `RoomRegistry` — not `DuelSocket`'s.
  `SeatOwnership`'s shape (`DuelSocket.kt:290-319`), read but not shared: making seat adoption and
  room opening contend on one stripe would serialise two unrelated operations and couple
  `RoomRegistry` to `DuelSocket`'s internals (`ADR-0133` §3).
  - `private const val PLAYER_STRIPE_COUNT = 64` at file level, and
    `private val playerStripes = Array(PLAYER_STRIPE_COUNT) { Mutex() }`, indexed by
    `(player.hashCode() and Int.MAX_VALUE) % PLAYER_STRIPE_COUNT` through a private
    `stripeFor(player: PlayerId): Mutex`.
  - **Allocated once and never pruned.** Carry `SeatOwnership`'s own reason as a comment in this
    file's words: a map keyed by `PlayerId` grows forever because `ADR-0012` device ids are trivially
    minted, and pruning races — removing a lock another coroutine is about to acquire hands out a
    second lock for the same player and reopens the race the lock exists to close.
- **The critical section contains no suspension point** — `heldRoom` and `create` are both
  non-suspending, and `create` stores with `putIfAbsent` and takes no room mutex. That is the whole
  of `ADR-0133` §4's deadlock answer, and the KDoc says so: `heldRoom` acquires nothing;
  `heldOrOpen` acquires exactly one lock across nothing that can wait; **a player stripe is always
  outermost** — a room's mutex may be taken while a player stripe is held, a player stripe may never
  be taken while a room's mutex is held.
- `create` keeps its present contract, signature and behaviour, and gains the same KDoc note
  `finish` already carries: **production code reaches it only through `heldOrOpen`.**
- **Do not make `mutate`'s `absent` or `block` parameters `suspend`.** They are plain function types
  today (`RoomRegistry.kt:768-769`) and `Mutex.withLock` is suspending, so nothing executed inside a
  room's critical section can acquire any mutex at all. `ADR-0133` §4 names adding `suspend` there
  as the single change that reopens the deadlock question; a test in this ticket fails if it
  happens.

## Out of scope

- **Every call site.** `replyToCreateRoom` is `TASK-141604`; `DuelSocket.kt` is read here for
  `SeatOwnership`'s comment and shape and is **not edited**.
- **Fencing `create`.** `ADR-0133` §3 chose the KDoc note over a fence and named the weakness in
  Consequences. Do not make `create` private, internal, or deprecated — `RoomRegistryTest`,
  `RoomRegistryJoinTest` and `TASK-141602`'s fixtures all call it directly.
- **Sharing `DuelSocket`'s `STRIPE_COUNT` or its `stripes` array**, or moving `SeatOwnership`.
- **Anything about `JoinRoom`.** `ADR-0133` §6's guard belongs to `ADR-0105` §1's ticket, and its
  comparison — held room `PLAYING` **and** its code different from the code asked for — is what
  keeps every reconnect working. Not here.

## Tests

New file `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldOrOpenTest.kt`,
package `duels.poker.server.room`, class `internal class RoomRegistryHeldOrOpenTest`. Use
`RoomRegistry(RandomRoomCodeSource(), MutableClock())` and a private top-level
`newPlayerId()`. Every test body is `runBlocking`, and the concurrency test uses
`runBlocking(Dispatchers.Default)` with a `CompletableDeferred` gate and `@Timeout(60)` — the shape
`RoomRegistryConcurrencyTest` already uses.

**Declare no top-level `class` whose name another file in `duels.poker.server.room` already uses.**
`RandomRoomCodeSource` is enough here; no scripted source is needed.

`RoomRegistryHeldOrOpenTest` — exactly five tests:

| Test | Proves |
| --- | --- |
| `heldOrOpenOpensAFreshRoomForAPlayerHoldingNothing` | The returned room is `WAITING`, `get(code)` finds it, `seatOf(host)` is `0`, and `size` is `1` |
| `heldOrOpenHandsBackTheWaitingRoomTheHostAlreadyHolds` | Called twice on a `MutableClock` started at `1_000` and advanced to `61_000` between the calls: the second call returns the **same** `code`, `size` stays `1`, and the returned room's `lastActivityAt` is **`1_000`** — the two clock values are distinct, so a re-stamp is visible (`ADR-0124` §4: a return does not restart the ten minutes) |
| `heldOrOpenOpensAFreshRoomForAHolderOfAFinishedSeat` | `heldOrOpen(host)` → `join(code, guest)` → `finish(code)`; a second `heldOrOpen(host)` returns a **different** code and `size` is `2`. `ADR-0105` §2's finished row, and the reason `ADR-0133` §1 puts the state filter inside `heldRoom` |
| `concurrentCallsForOnePlayerOpenExactlyOneRoom` | Two halves in one test, because one input cannot tell a working stripe from a global lock. **100** gated concurrent `heldOrOpen` calls for **one** player yield one distinct `code` across all 100 results and `size == 1`; **100** for **100 different** players yield 100 distinct codes and `size == 100` |
| `mutateTakesPlainFunctionBlocksSoNoRoomLockCanNestAnother` | Reads `src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` as text. Asserts it contains `absent: () -> T,` and `block: (Room) -> Pair<Room?, T>,`, and contains neither `absent: suspend` nor `block: suspend`. This is `ADR-0133` §4's tripwire, kept as a test rather than as a comment so that reopening the deadlock question fails a gate |

## Acceptance criteria

- [ ] `RoomRegistryHeldOrOpenTest.heldOrOpenOpensAFreshRoomForAPlayerHoldingNothing` passes
- [ ] `RoomRegistryHeldOrOpenTest.heldOrOpenHandsBackTheWaitingRoomTheHostAlreadyHolds` passes, and
      its stamp assertion is `1_000` against a clock left at `61_000`
- [ ] `RoomRegistryHeldOrOpenTest.heldOrOpenOpensAFreshRoomForAHolderOfAFinishedSeat` passes
- [ ] `RoomRegistryHeldOrOpenTest.concurrentCallsForOnePlayerOpenExactlyOneRoom` passes **both**
      halves — one player to one room, and 100 players to 100 rooms
- [ ] `RoomRegistryHeldOrOpenTest.mutateTakesPlainFunctionBlocksSoNoRoomLockCanNestAnother` passes
- [ ] `TEST-duels.poker.server.room.RoomRegistryHeldOrOpenTest.xml` reports exactly **5** tests
- [ ] `RoomRegistry.kt` holds exactly **three** `.withLock {` sites — `reap`, `mutate` and
      `heldOrOpen`'s stripe, and no fourth
- [ ] `RoomRegistry.kt` contains no `tryLock`, and `heldRoom` is still declared `fun`
- [ ] `mutate`'s parameters read `absent: () -> T,` and `block: (Room) -> Pair<Room?, T>,`, and the
      file contains no `absent: suspend` or `block: suspend`
- [ ] `RoomRegistryTest`, `RoomRegistryJoinTest`, `RoomRegistryConcurrencyTest`,
      `RoomRegistryLifecycleTest`, `RoomReapTest` and `RoomRegistryHeldRoomTest` are not edited and
      stay green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
