---
id: STORY-1416
title: Play again returns the host to the room they still hold
type: story
status: ready
parent: EPIC-14
module: poker-server
labels: [server, room, bug]
depends_on: []
---

## Goal

A player who opens a duel room, walks back to the lobby and presses again lands in **the room they
already hold** — same code, same seat, nothing said, nothing written, and the room's ten minutes
still running from when it was opened. The return is the **server's**: `RoomRegistry` learns to
answer *which live room does this player hold*, opening a room becomes find-or-create under a
per-player lock, and `replyToCreateRoom` answers `RoomJoined` for the room the lookup found. No
client file changes, no string is added, no wire moves, and no `PROTOCOL_VERSION` steps.

## Why

**It is `EPIC-14` item 6, and what the human met was not a short-lived room but a second one.**
`RoomTimeouts.DEFAULT_WAITING_MILLIS` is already `10 * 60 * 1000` — twice the *"like 3-5 min"* the
report asked for — so the life was never the defect. `replyToCreateRoom` is three lines
(`DuelSocket.kt:498-506`) that read nothing about the player, and `ADR-0105` §2 refuses a second
room only while the held duel is `PLAYING`. Three presses of shipped controls therefore open a
**second** waiting room while the first lives out its ten minutes holding the code the host may
already have sent — and `ADR-0124`'s Context traces that orphan to a coin settled for a duel its
holder never saw.

**Both decisions are merged and this story invents nothing.**
[`ADR-0124`](../../docs/adr/ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) answered
`DEC-139` and `DEC-111` — a player holds at most one waiting room, a second `CreateRoom` hands back
the first, nothing is refused, nothing is said, and the browser goes on forgetting exactly as
`ADR-0072` §3 wrote it.
[`ADR-0133`](../../docs/adr/ADR-0133-the-room-a-player-holds-is-scanned-for.md) answered the
architect's `DEC-110` with the mechanism: a lock-free scan, `heldRoom(player)`, and find-or-create
under a striped `Mutex`. Its §9 says in as many words what this story builds, that it moves **no
wire**, and that it is **not** `atomic:`.

## Design notes

Everything below is measured on `develop` at `7ac3d59f` or is merged, and no ticket re-litigates it.

- **The `PLAYING` case must keep falling through to `create`, exactly as today.** The lookup this
  story builds *will* find a `PLAYING` room, and `replyToCreateRoom`'s branch must ignore it and
  open a fresh room, which is `develop`'s behaviour. `ADR-0133` §9 states the reason and it is not
  a matter of taste: handing that room back silently would ship `ADR-0105` §1's **refusal** without
  `ADR-0105` §4's **words**, which is the one thing the product owner fixed. `ADR-0105` §1 stays
  unimplemented until its own ticket lands — that ticket is **outside `EPIC-14`**, it is `atomic:`,
  and it spends a new `ProtocolError` value (`ALREADY_IN_DUEL`), `ADR-0047` §2's fingerprint and a
  `PROTOCOL_VERSION` step. None of that is here.
- **`replyToJoinRoom` is not touched at all, and the reason is worth writing down before somebody
  helpfully adds the guard.** `ADR-0133` §6 puts the `JoinRoom` guard in that same `PLAYING` ticket,
  and fixes its shape there: it fires only when the held room is `PLAYING` **and its code differs
  from the code that was asked for**. A player sending `JoinRoom` for the code of the room they are
  already in is a **reconnect** — it is how this product returns every dropped player to their
  table — so a guard without that comparison breaks `resume`, `ADR-0013`'s grace window and
  `ADR-0072`'s remembered code in one line. A holder of a `WAITING` room asking to join **another**
  room is `DEC-145`, the product owner's, still open, and until it merges `JoinRoom` behaves exactly
  as it does at `develop`.
- **The deadlock answer is by construction, and the type system is currently what enforces it.**
  `ADR-0133` §4: `heldRoom` acquires nothing, so it cannot appear in a wait-for cycle; `heldOrOpen`
  acquires exactly one lock — the player's stripe — across **no suspension point**, because
  `heldRoom` and `create` are both non-suspending. The ordering rule (*a player stripe is always
  outermost*) holds today because `mutate`'s two block parameters are **plain function types** —
  `absent: () -> T` and `block: (Room) -> Pair<Room?, T>`, `RoomRegistry.kt:768-769` — while
  `Mutex.withLock` is `suspend`, so nothing executed inside a room's critical section can acquire
  any mutex at all. `ADR-0133` §4 names making either block `suspend` as the single change that
  reopens this. `TASK-141603` carries a test that fails if either gains it, and every ticket in the
  chain gates the count of `.withLock {` sites in `RoomRegistry.kt`, which is **2** today
  (`reap:702`, `mutate:772`) and **3** after `TASK-141603`.
- **The seat is `Room.seatOf` off the returned snapshot, never the literal `0`** (`ADR-0133` §5).
  `DuelSocket.kt:505` sends the literal today and its KDoc justifies it — `Room.open` always seats
  the host at 0. That stays true of a fresh room and `ADR-0124` §4 confirms it of a returned
  `WAITING` one, so **within this story the two are the same number and no behavioural test can tell
  them apart**. It is false of a `PLAYING` room, where the holder may be the guest, and a constant
  that is right for the wrong reason survives every test until it does not. This story pins it in
  two places instead of pretending otherwise: a **behavioural** test in `TASK-141601` that
  `heldRoom` finds the room a **guest** sits in and reports seat `1`, and a **structural** test in
  `TASK-141604` that `replyToCreateRoom`'s own body names `seatOf` and constructs no
  `RoomJoined(…, 0)`.
- **`RoomMembership.code` is repointed at the room the answer names**, in every branch, before the
  frame goes out (`ADR-0133` §5) — `ADR-0104` requires the frames to reach the connection in the
  room they are about, and a later `Act` is routed by that record. This is also what disarms
  `ADR-0118`'s window at the source `ADR-0118` named: the press inside the recovery gap still
  repoints the membership, but at the room the player already holds.
- **A return writes nothing, and the ten minutes is not restarted.** `ADR-0124` §4: a `WAITING`
  room's `lastActivityAt` is written at `Room.open` and by nothing else until a rival joins. The
  hand-back gets that for free rather than by a special case, because `resume` answers `null` for a
  `WAITING` room and the hand-back never calls it. `TASK-141604` proves it with a `MutableClock`
  advanced 60 seconds between the two presses, so the stamp has two distinguishable values.
- **No merged assertion moves, and that was measured rather than assumed.** `CreateRoom` is sent
  from exactly seven files, always through a helper called **once per test** against a registry
  built fresh by `testApplication`, so no merged test asks for a second room for a player who
  already holds one; the two tests that do return a host to their own room —
  `thehostRejoiningItsOwnRoomIsToldItsSeat` and `aRejoinToAnExistingRoomStillAnswersRoomJoined` —
  go through `JoinRoom`, which this story does not touch. Every `RoomRegistryTest` `create` takes a
  fresh `newPlayerId()`. `create` keeps its present contract and signature, so `RoomRegistryTest`'s five
  `create` tests are untouched, and `DuelSocketRoomTest.createRoomAnswersWithACodeAndSeatZero` stays
  green **because a fresh room still seats its host at 0**. `DuelSocketRoomTest` holds **10** `@Test`
  methods today; `TASK-141604` takes it to 14 and edits none of the ten.
- **`private class ScriptedCodes` already exists in `RoomRegistryTest.kt`, in the same package.**
  Per-file privacy in Kotlin covers top-level *functions*, not top-level *classes*, so a second
  `ScriptedCodes` in `duels.poker.server.room` is a redeclaration error. `TASK-141602` names its
  code source `HeldRoomCodes` for that reason. A private `newPlayerId()` **function** is fine —
  three test files in that package already carry one.
- **Nothing outside `poker-server/src` is in this story.** No `web-client` file, no `design/` card,
  no `docs/protocol.md`, no `PROTOCOL_VERSION`, no string, no schema and nothing stored. `ADR-0124`
  §3 chose silence deliberately, and `ADR-0110` §6's enumeration of what the host-alone state
  renders stays exhaustive and unchanged.

## Tasks

Split on **2026-09-07**. One linear chain — every ticket edits a file the next one edits, so exactly
one is startable at a time.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-141601`](../tasks/TASK-141601-the-registry-answers-which-live-room-a-player-holds.md) | S | `RoomRegistry.heldRoom(player)`: a non-suspending, lock-free scan returning the whole `Room`, where *held* means a seat in a `WAITING` or `PLAYING` room. The `FINISHED`/`ABANDONED` filter lives in the method. Proved to find a **guest's** seat, and gated as taking no lock |
| [`TASK-141602`](../tasks/TASK-141602-the-held-room-answer-is-total-and-not-the-maps-order.md) | S | `heldRoom`'s answer becomes **total**: `PLAYING` before `WAITING`, then ascending `lastActivityAt`, then ascending `RoomCode.value`. Each key is proved by a fixture where the next key points the **other** way |
| [`TASK-141603`](../tasks/TASK-141603-opening-a-room-is-find-or-create-serialised-per-player.md) | S | `heldOrOpen(host, format)` = `heldRoom ?: create`, under a per-player striped `Mutex` in a second array. 100 concurrent calls for one player open one room; 100 for 100 players open 100. Carries `ADR-0133` §4's tripwire test |
| [`TASK-141604`](../tasks/TASK-141604-pressing-play-again-hands-back-the-room-the-host-holds.md) | S | `replyToCreateRoom` calls `heldOrOpen`, hands back a `WAITING` room, **falls through to `create` for a `PLAYING` one**, sends the seat from `seatOf` and repoints `RoomMembership` |

**A fifth ticket was considered and refused.** `docs/test-plan.md` gets no row here: `EPIC-14`'s
per-epic suite is the `qa-cases` skill's to write from the epic's Definition of done — *"one case per
promise the epic made… not one per ticket"* (`docs/test-plan.md` §Per-epic suites, rule 1) — and a
story that writes its own catalogue row writes it from a ticket title, which that section forbids in
as many words.

## Acceptance criteria

- [ ] A socket that sends `CreateRoom` twice is answered the **same code and the same seat** both
      times, and the registry holds **one** room
- [ ] The returned room's `lastActivityAt` is the stamp it was opened with, after a clock that moved
      60 seconds between the two presses
- [ ] A player holding a **`PLAYING`** room who sends `CreateRoom` is still answered a **different**
      code, and the registry holds two rooms — `ADR-0105` §1's refusal is not shipped here
- [ ] `heldRoom` answers the room a **guest** sits in, and `seatOf` on the returned room is `1`
- [ ] `heldRoom` answers `null` for a player whose only room is `FINISHED`, and `null` for one whose
      only room is `ABANDONED`, while `get(code)` still finds that room and its `seatOf` is non-null
- [ ] `heldRoom` returns the `PLAYING` room whichever of the two was opened first; the **older**
      `WAITING` room even when it carries the **higher** code; and the **lower** code when the two
      stamps are equal
- [ ] 100 concurrent `heldOrOpen` calls for one player produce exactly one code and one registered
      room; 100 for 100 different players produce 100 of each
- [ ] `RoomRegistry.kt` declares `heldRoom` as `fun`, never `suspend fun`, contains no `tryLock`, and
      holds exactly **three** `.withLock {` sites when the chain has landed
- [ ] `mutate`'s parameters are still `absent: () -> T` and `block: (Room) -> Pair<Room?, T>` —
      neither is `suspend` (`ADR-0133` §4's tripwire)
- [ ] `replyToCreateRoom` names `seatOf` and constructs no `RoomJoined(…, 0)`; `DuelSocket.kt` names
      `heldOrOpen(` once and `rooms.create(` once
- [ ] No file under `web-client/`, `design/` or `docs/` is edited, and `PROTOCOL_VERSION` is
      unchanged
- [ ] `./gradlew check -PrequireDocker=true` exits 0 and `python3 .github/scripts/lint_tickets.py`
      exits 0

## Out of scope

- **`ADR-0105` §1's refusal**, `ALREADY_IN_DUEL`, the `PROTOCOL_VERSION` step it forces and the
  client's mapping of it to `ADR-0105` §4's two sentences. That is a separate `atomic:` ticket,
  **outside `EPIC-14`**, and it is what turns the `PLAYING` fall-through into `ADR-0133` §8's
  sequence. It is not ticketed yet.
- **Any change to `replyToJoinRoom`.** The `JoinRoom` guard belongs to that same ticket, and
  `ADR-0133` §6 fixes its shape: it fires only when the held room is `PLAYING` **and its code
  differs from the code asked for**, or every reconnect breaks.
- **`DEC-145`** — may a holder of a `WAITING` room take a seat in **another** room by invite, and
  what becomes of the room they hold. Registered by `ADR-0124` §7, the product owner's, still open,
  and it blocks nothing here.
- **The room's lifetime.** Ten minutes from `Room.open`, unchanged; `RoomTimeouts` is not edited and
  a return does not restart the clock (`ADR-0124` §4).
- **Anything a player sees.** No string, no banner, no toast, no dialog, no client file
  (`ADR-0124` §§2, 3). `WaitingTable.tsx` and `Lobby.tsx` are not opened, and `forgetRoom()` keeps
  doing exactly what `ADR-0072` §3 says.
- **Turning the scan into an index.** `ADR-0133` §2 makes that a one-body change behind the same
  method and §10 deliberately leaves it unregistered — nobody is working it and nothing measures it.
- **Fencing `create` behind `heldOrOpen`.** `ADR-0133` §3 chose the tree's existing convention — the
  KDoc note `finish` already carries — and named its weakness in Consequences rather than building a
  fence.
- **`ADR-0022` §2's failed-join budget.** `JoinLimits` and `TOO_MANY_ATTEMPTS` do not exist at
  `develop`, and there is no refusal in this story to meter.
