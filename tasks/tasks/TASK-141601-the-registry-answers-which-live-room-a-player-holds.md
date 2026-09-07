---
schema: 2
id: TASK-141601
title: The registry answers which live room a player holds
type: task
status: done
parent: STORY-1416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, room]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomRegistryHeldRoomTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomRegistryHeldRoomTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==6 else 1)"
  - sh -c 'test "$(grep -c "public fun heldRoom(player: PlayerId): Room?" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 1'
  - sh -c '! grep -q "suspend fun heldRoom" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt'
  - sh -c 'test "$(grep -c "tryLock" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 0'
  - sh -c 'test "$(grep -c "\.withLock {" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 2'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RoomRegistry` can answer *which live room does this player hold a seat in* — `heldRoom(player)`,
non-suspending, taking no mutex, returning the whole `Room` — where **held** means a seat in a
`WAITING` or `PLAYING` room and nothing else. Nothing calls it yet.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldRoomTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryJoinTest.kt` | read |
| `docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md` | read |

## Scope

- Add one public, **non-suspending** query to `RoomRegistry`:
  `public fun heldRoom(player: PlayerId): Room?`.
- A room is **held** when `Room.seatOf(player)` is non-null **and** `Room.state` is
  `RoomState.WAITING` or `RoomState.PLAYING`. `FINISHED` and `ABANDONED` are excluded **inside this
  method**, never by a caller (`ADR-0133` §1): `ADR-0105` §2 allows a holder of a finished seat a
  fresh room, and `Room.offerRematch` needs that seat to stay where it is, so a caller that forgot
  would silently break both.
- It returns the whole `Room`, never a boolean and never a bare code — a caller needs the state, the
  code and the seat off **one** snapshot.
- It iterates `rooms` and reads each `Holder.room` through the existing `@Volatile` field, exactly
  as `get(code)` does. It acquires **no mutex** — it must stay declared `fun`, not `suspend fun`,
  which is what makes taking one impossible (`ADR-0133` §§2, 4).
- KDoc on the method: what *held* means and why the filter is here; that it takes no lock and that
  the non-suspending declaration is the proof; that when more than one room qualifies the choice is
  arbitrary **today** and becomes total in `TASK-141602`.
- Every test in this ticket arranges **exactly one** qualifying room for the player it asks about,
  so no assertion here depends on which one is picked.

## Out of scope

- **The total order** — `PLAYING` first, then oldest `lastActivityAt`, then lowest code. That is
  `TASK-141602`, and writing it here makes this ticket two.
- **`heldOrOpen`, the stripes, and any call site at all.** `TASK-141603` and `TASK-141604`.
  `create`, `join`, `resume`, `reap`, `mutate` and `Holder` are read, never edited.
- **`DuelSocket.kt`.** Not opened.
- **Turning the scan into an index.** `ADR-0133` §2 and §10; nobody is working it.

## Tests

New file `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldRoomTest.kt`, package
`duels.poker.server.room`, class `internal class RoomRegistryHeldRoomTest`. Build registries with
`RoomRegistry(RandomRoomCodeSource(), MutableClock())` and a private top-level
`newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())`, the shape
`RoomRegistryJoinTest.kt` already uses. Tests that call `join`, `finish` or `abandon` wrap their body
in `runBlocking`; the rest need no coroutine.

**Declare no top-level `class` in this file.** `RoomRegistryTest.kt` already declares
`private class ScriptedCodes` in this same package, and Kotlin's per-file privacy covers top-level
functions but **not** top-level classes, so a second one is a redeclaration error. A private
top-level *function* is fine — three files in this package already carry `newPlayerId`.

`RoomRegistryHeldRoomTest` — exactly six tests:

| Test | Proves |
| --- | --- |
| `heldRoomIsNullForAPlayerHoldingNothing` | An empty registry, and a registry holding one room belonging to **another** player, both answer `null` — so the method reads the player rather than the map's emptiness |
| `heldRoomFindsTheWaitingRoomItsHostOpened` | After `create(host)`, `heldRoom(host)` is the same `Room` (`assertEquals` on the value, and its `code` equals the created code) and its `state` is `WAITING` |
| `heldRoomFindsThePlayingRoomTheGuestSitsIn` | `create(host)` then `join(code, guest)`; `heldRoom(guest)` returns that room and `seatOf(guest)` on the returned room is **`1`** — the lookup is not host-only. `heldRoom(host)` returns it too |
| `heldRoomIgnoresAFinishedRoom` | `create` → `join` → `finish(code)`; `heldRoom` is `null` for **both** host and guest, while `get(code)` still returns the room and its `seatOf(host)` is still `0` — so the exclusion is the state, not a vacated seat |
| `heldRoomIgnoresAnAbandonedRoom` | `create(host)` → `abandon(code)`; `heldRoom(host)` is `null`, while `get(code)` still returns the room and `seatOf(host)` is still `0` |
| `heldRoomIsDeclaredNonSuspendingSoItCanTakeNoMutex` | Reads `src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` as text (the shape `RoomRegistryTest.importsNoTransportOrProtocolTypes` uses). Asserts it contains `public fun heldRoom(`, does **not** contain `suspend fun heldRoom`, and contains no `tryLock` — a non-suspending function cannot call `Mutex.withLock`, which is `ADR-0133` §4's whole proof |

## Acceptance criteria

- [ ] `RoomRegistryHeldRoomTest.heldRoomIsNullForAPlayerHoldingNothing` passes
- [ ] `RoomRegistryHeldRoomTest.heldRoomFindsTheWaitingRoomItsHostOpened` passes
- [ ] `RoomRegistryHeldRoomTest.heldRoomFindsThePlayingRoomTheGuestSitsIn` passes, and its seat
      assertion is `1`
- [ ] `RoomRegistryHeldRoomTest.heldRoomIgnoresAFinishedRoom` passes
- [ ] `RoomRegistryHeldRoomTest.heldRoomIgnoresAnAbandonedRoom` passes
- [ ] `RoomRegistryHeldRoomTest.heldRoomIsDeclaredNonSuspendingSoItCanTakeNoMutex` passes
- [ ] `TEST-duels.poker.server.room.RoomRegistryHeldRoomTest.xml` reports exactly **6** tests — the
      absolute number, because a `--tests` filter that matches nothing still exits 0
- [ ] `RoomRegistry.kt` holds exactly **two** `.withLock {` sites, unchanged from `develop` — this
      ticket adds no lock
- [ ] `RoomRegistryTest`, `RoomRegistryJoinTest`, `RoomRegistryLifecycleTest` and
      `RoomRegistryConcurrencyTest` are not edited and stay green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
