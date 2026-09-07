---
schema: 2
id: TASK-141604
title: Pressing play again hands back the room the host holds
type: task
status: backlog
parent: STORY-1416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, room, bug]
depends_on: [TASK-141603]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRoomTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.DuelSocketRoomTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==14 else 1)"
  - sh -c 'test "$(grep -cF "heldOrOpen(session.player.id)" poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt)" -eq 1'
  - sh -c 'test "$(grep -cF "deps.rooms.create(" poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt)" -eq 1'
  - sh -c 'test "$(grep -cF "seatOf(session.player.id)" poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt)" -eq 6'
  - sh -c '! grep -qF "RoomJoined(created.code.value, 0)" poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt'
  - sh -c 'test "$(grep -cF "deps.rooms.resume(" poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt)" -eq 1'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A socket that sends `CreateRoom` while its player already holds a `WAITING` seat is answered
`RoomJoined` for **that** room — same code, same seat, no second room opened, nothing written and
nothing else sent. A player holding a `PLAYING` room still gets a fresh room, exactly as today.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRoomTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/time/MutableClock.kt` | read |
| `docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md` | read |

## Scope

- `replyToCreateRoom` calls `deps.rooms.heldOrOpen(session.player.id)` in place of
  `deps.rooms.create(session.player.id)`, and branches on the returned room's `state`:
  - `RoomState.WAITING` — that room **is** the answer. No room opened, nothing written, nothing else
    sent (`ADR-0124` §§1, 3).
  - **anything else** — `deps.rooms.create(session.player.id)`, which is `develop`'s behaviour
    unchanged. See *Out of scope*: this branch is deliberately not the final one.
- `RoomMembership.code` is set to the code of the room the answer **names**, in either branch,
  before the frame goes out (`ADR-0133` §5). `ADR-0104` requires the frames to reach the connection
  in the room they are about, and a later `Act` is routed by that record.
- The seat is `Room.seatOf(session.player.id)` read off **that same** room value, wrapped in
  `checkNotNull` naming why it cannot be null — the value came off a snapshot the registry has
  already committed to. **Never the literal `0`.** The shipped line
  `ServerMessage.RoomJoined(created.code.value, 0)` goes away; a `verify:` gate refuses it.
- The KDoc on `replyToCreateRoom` is rewritten: the press is find-or-create over the player rather
  than an unconditional open; the seat is derived rather than assumed, and why the constant was
  right for the wrong reason; the membership is repointed at the room the answer names, which is
  what disarms `ADR-0118`'s window at the source that ADR named; and the non-`WAITING` branch is
  **transitional**, belonging to `ADR-0105` §1's own ticket.

## Out of scope

- **`ADR-0105` §1's refusal.** The `PLAYING` case **must keep falling through to `create`**
  (`ADR-0133` §9). The lookup will find the `PLAYING` room and this branch must ignore it: handing
  it back silently would ship `ADR-0105` §1's refusal without `ADR-0105` §4's two sentences, which
  is the one thing the product owner fixed. Do not add `ALREADY_IN_DUEL`, do not send a `Failure`,
  do not call `resume` from this function, and do not touch `PROTOCOL_VERSION`.
- **`replyToJoinRoom`.** Not edited, and `deps.rooms.resume(` stays at exactly one call site.
  `ADR-0133` §6's guard is the same other ticket's, and its comparison is load-bearing: it fires
  only when the held room is `PLAYING` **and its code differs from the code that was asked for**.
  A player sending `JoinRoom` for the code of the room they are already in is a **reconnect**, and a
  guard without that comparison breaks `resume`, `ADR-0013`'s grace window and `ADR-0072`'s
  remembered code in one line.
- **Any client file, any string, any design card, `docs/protocol.md`, `RoomTimeouts`.**
  `ADR-0124` §§2, 3: the browser goes on forgetting, nothing is said, and the ten minutes still runs
  from `Room.open`.
- **The ten merged tests in `DuelSocketRoomTest`.** None of them opens two rooms for one device, and
  `createRoomAnswersWithACodeAndSeatZero` stays green because a fresh room still seats its host at
  `0`. Do not edit, reorder or weaken any of them.

## Tests

Four tests are **added** to `DuelSocketRoomTest`, taking it from **10** to **14**. The one helper
change is `testRoomRegistry` gaining a defaulted clock, so no existing call site moves:

```kotlin
private fun testRoomRegistry(clock: ServerClock = SystemClock): RoomRegistry =
    RoomRegistry(RandomRoomCodeSource(), clock, seeds = fixedSeeds)
```

`MutableClock` is `poker-server/src/test/kotlin/duels/poker/server/time/MutableClock.kt`. New imports
sort among the existing `duels.*` group (`MutableClock` < `ServerClock` < `SystemClock`).

`DuelSocketRoomTest` — four new tests:

| Test | Proves |
| --- | --- |
| `aSecondCreateRoomHandsBackTheRoomTheHostAlreadyHolds` | One socket, `CreateRoom` sent twice. The second `RoomJoined` carries the **same** `code` and the **same** `seat` as the first, and `rooms.size` is `1` — the press opened no second room |
| `aReturnedRoomKeepsTheStampItWasOpenedWith` | The same two presses against `testRoomRegistry(MutableClock(1_000))`, with `clock.advance(60_000)` between them. `rooms.get(RoomCode(created.code))!!.lastActivityAt` is **`1_000`**. The clock's two values are distinct, so a re-stamp would read `61_000` and fail — `ADR-0124` §4, a return does not restart the ten minutes |
| `aHolderOfAPlayingRoomStillGetsAFreshRoom` | Host opens a room, `joinRoom("guest", code)` starts the duel, `host.drainServerMessages()` swallows the opening burst, then the host sends `CreateRoom`. The answer is a **different** code, `rooms.size` is `2`, and the first room is still `RoomState.PLAYING`. This is the guard on *Out of scope*'s first bullet: `ADR-0105` §1 is not shipped here |
| `theCreateRoomAnswerReadsItsSeatOffTheRoom` | Reads `src/main/kotlin/duels/poker/server/DuelSocket.kt` as text and slices `replyToCreateRoom` — from the index of `private suspend fun ConnectionWriter.replyToCreateRoom(` to the first `\n}\n` after it. Asserts the slice contains `heldOrOpen(` and `seatOf(session.player.id)`, and matches `Regex("""RoomJoined\([^)]*,\s*0\s*\)""")` **nowhere**. It is structural on purpose: `Room.open` seats the host at `0`, so within this story a returned `WAITING` room and a fresh one carry the same seat and **no behavioural test can tell a derived seat from the constant**. The case where they differ is a `PLAYING` room, and that is the other ticket's (`ADR-0133` §5) |

`assertNotEquals` and `RoomState` are new imports in the test file.

## Acceptance criteria

- [ ] `DuelSocketRoomTest.aSecondCreateRoomHandsBackTheRoomTheHostAlreadyHolds` passes, asserting the
      same code, the same seat and `rooms.size == 1`
- [ ] `DuelSocketRoomTest.aReturnedRoomKeepsTheStampItWasOpenedWith` passes, asserting
      `lastActivityAt == 1_000L` against a clock left at `61_000`
- [ ] `DuelSocketRoomTest.aHolderOfAPlayingRoomStillGetsAFreshRoom` passes, asserting a **different**
      code and `rooms.size == 2`
- [ ] `DuelSocketRoomTest.theCreateRoomAnswerReadsItsSeatOffTheRoom` passes
- [ ] `TEST-duels.poker.server.DuelSocketRoomTest.xml` reports exactly **14** tests
- [ ] The ten merged tests in that file are byte-unchanged and green; no assertion is weakened or
      deleted, and `createRoomAnswersWithACodeAndSeatZero` still asserts `0`
- [ ] `DuelSocket.kt` names `heldOrOpen(session.player.id)` once, `deps.rooms.create(` once,
      `deps.rooms.resume(` once, and `seatOf(session.player.id)` **six** times (five today plus this
      one)
- [ ] `DuelSocket.kt` contains no `RoomJoined(created.code.value, 0)`
- [ ] No file under `web-client/`, `design/` or `docs/` is edited, and `PROTOCOL_VERSION` is
      unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
