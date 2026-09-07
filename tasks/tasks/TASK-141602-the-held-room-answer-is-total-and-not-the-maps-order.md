---
schema: 2
id: TASK-141602
title: The held-room answer is total, and not the map's order
type: task
status: done
parent: STORY-1416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, room]
depends_on: [TASK-141601]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomRegistryHeldRoomTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomRegistryHeldRoomTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==9 else 1)"
  - sh -c '! grep -q "suspend fun heldRoom" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt'
  - sh -c 'test "$(grep -c "tryLock" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 0'
  - sh -c 'test "$(grep -c "\.withLock {" poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt)" -eq 2'
  - sh -c 'test "$(grep -c "private class ScriptedCodes" poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldRoomTest.kt)" -eq 0'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When a player holds more than one live room, `heldRoom` picks the same one every time and for a
stated reason, rather than whichever one `ConcurrentHashMap` happened to hand back first.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryHeldRoomTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomCode.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryTest.kt` | read |
| `docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md` | read |

## Scope

- `heldRoom` returns the **first** qualifying room under this order, exactly as `ADR-0133` §1
  states it:
  1. `RoomState.PLAYING` before `RoomState.WAITING` — a running duel outranks a waiting room,
     because it is the one that ends in a coin and `ADR-0105` §1 is the stronger rule;
  2. then **ascending** `Room.lastActivityAt` — for a `WAITING` room that stamp is written at
     `Room.open` and by nothing else, so the oldest room is the one whose code has been out longest
     and the one that dies first;
  3. then **ascending** `RoomCode.value` — so the function is total even when two stamps collide.
- The filter `TASK-141601` established is unchanged, and so is everything else about the method: it
  stays `public fun` (never `suspend`), takes no mutex, and adds no `.withLock {` site.
- KDoc gains the three keys and one sentence of reason: a server answer that depends on a hash map's
  iteration order is neither testable nor explicable.
- A player *can* hold two live rooms today — that is the state `ADR-0105` §2 left open — so this is
  a reachable case and not a hypothetical.

## Out of scope

- **Preventing a player from holding two rooms.** That is `TASK-141603`'s stripe plus
  `TASK-141604`'s branch. This ticket only makes the answer deterministic while it is still
  possible.
- **Any call site.** Nothing calls `heldRoom` until `TASK-141603`.
- **`create`, `join`, `finish`, `abandon`, `reap`, `mutate` and `Holder`** — read, never edited.
- **`DuelSocket.kt`** — not opened.

## Tests

Three tests are **added** to `RoomRegistryHeldRoomTest`, taking it from six to **nine**. The six
merged in `TASK-141601` are not edited: none of them arranges two qualifying rooms, so none of them
observes an order.

The new tests need scripted codes. Add a private top-level code source **named `HeldRoomCodes`** —
not `ScriptedCodes`, which `RoomRegistryTest.kt` already declares in this same package, where a
second top-level class of that name is a redeclaration error:

```kotlin
private class HeldRoomCodes(vararg codes: String) : RoomCodeSource {
    private val queue = ArrayDeque(codes.map { RoomCode(it) })

    override fun newRoomCode(): RoomCode = queue.removeFirst()
}
```

`A` and `Z` are both in `RoomCode.ALPHABET`, so `"AAAAAAAA"` and `"ZZZZZZZZ"` are valid eight-character
codes and `"AAAAAAAA" < "ZZZZZZZZ"` as strings.

**Every one of the three arranges the *next* key to point the other way.** A fixture whose keys all
agree cannot tell the three-key comparator from a one-key one.

| Test | Proves |
| --- | --- |
| `aPlayingRoomOutranksAWaitingOneWhicheverOpenedFirst` | Two registries. In the first, the `WAITING` room is opened at `1_000` and the `PLAYING` one is opened and joined at `2_000`, so the state key and the stamp key **disagree**; `heldRoom(player)` is the `PLAYING` room. In the second the times are swapped — `PLAYING` opened and joined at `1_000`, `WAITING` opened at `2_000` — and it is still the `PLAYING` room. So neither the stamp nor the insertion order is what is passing |
| `theOlderWaitingRoomWinsEvenWhenItCarriesTheHigherCode` | Two registries, both holding two `WAITING` rooms for one host. In the first, codes are minted `"ZZZZZZZZ"` at `1_000` then `"AAAAAAAA"` at `2_000`, so the stamp key and the code key **disagree**; the answer is `ZZZZZZZZ`. In the second, `"AAAAAAAA"` at `1_000` then `"ZZZZZZZZ"` at `2_000`; the answer is `AAAAAAAA`. A comparator that reached for the code first fails the first half; one that returned the newest fails both |
| `theLowerCodeBreaksATieOnTheStamp` | Two registries on a `MutableClock` that never advances, so both rooms carry the identical stamp and the first two keys are ties. Codes minted `"ZZZZZZZZ"` then `"AAAAAAAA"` in one and `"AAAAAAAA"` then `"ZZZZZZZZ"` in the other; **both** answer `AAAAAAAA`, so the answer cannot be the insertion order |

Both `WAITING` rooms in the second and third tests are opened by calling `registry.create(host)`
twice for the same player — legal, because `create` keeps its present contract and this ticket does
not change it.

## Acceptance criteria

- [ ] `RoomRegistryHeldRoomTest.aPlayingRoomOutranksAWaitingOneWhicheverOpenedFirst` passes, and
      asserts against **two** registries whose stamp order is opposite
- [ ] `RoomRegistryHeldRoomTest.theOlderWaitingRoomWinsEvenWhenItCarriesTheHigherCode` passes, and
      one of its two registries gives the older room the **higher** code
- [ ] `RoomRegistryHeldRoomTest.theLowerCodeBreaksATieOnTheStamp` passes, and both of its registries
      answer `AAAAAAAA`
- [ ] `TEST-duels.poker.server.room.RoomRegistryHeldRoomTest.xml` reports exactly **9** tests
- [ ] The six tests `TASK-141601` merged are byte-unchanged and green; no assertion in them is
      weakened or deleted
- [ ] The file declares no `ScriptedCodes`, and the whole module compiles — a redeclaration in
      `duels.poker.server.room` would fail `./gradlew check` outright
- [ ] `RoomRegistry.kt` still holds exactly **two** `.withLock {` sites and declares `heldRoom` as
      `fun`, not `suspend fun`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
