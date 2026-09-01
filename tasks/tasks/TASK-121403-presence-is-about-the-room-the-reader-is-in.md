---
schema: 2
id: TASK-121403
title: Presence is about the room the reader is sitting in
type: task
status: backlog
parent: STORY-1214
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 6
labels: [bug, blocker, presence]
depends_on: [TASK-121404]
atomic:
  - the Kotlin compiler — deleting `ConnectionDirectory.writerFor(player)` breaks its call sites in one step, measured 2026-09-01 by `:poker-server:compileTestKotlin` — `SeatDelivery.kt:40` and `DuelSocketWriterDirectoryTest.kt:72,114,119,155`, all `No value passed for parameter 'room'`
  - the Kotlin compiler — `register` gaining the membership breaks every caller in the same step — `DuelSocket.kt:195`, `SeatDeliveryTest.kt:59` and `ConnectionDirectoryTest.kt:18,36,37,48,49,61,73`, all `No value passed for parameter 'membership'`
  - ADR-0104 §1 — the unscoped overload is deleted rather than left beside the scoped one, because "a rule that can be bypassed by calling the other method is a convention; a rule with no other method is structural". Keeping both is the only smaller cut, and it is the design the ADR rejects by name
verify:
  - ./gradlew :poker-server:test --tests duels.poker.server.SeatDeliveryTest && python3 -c "import xml.etree.ElementTree as ET; r = ET.parse('poker-server/build/test-results/test/TEST-duels.poker.server.SeatDeliveryTest.xml').getroot(); assert r.get('tests') == '8' and r.get('failures') == '0'"
  - ./gradlew :poker-server:test --tests duels.poker.server.session.ConnectionDirectoryTest && python3 -c "import xml.etree.ElementTree as ET; r = ET.parse('poker-server/build/test-results/test/TEST-duels.poker.server.session.ConnectionDirectoryTest.xml').getroot(); assert r.get('tests') == '9' and r.get('failures') == '0'"
  - ./gradlew :poker-server:test --tests duels.poker.server.DuelSocketWriterDirectoryTest && python3 -c "import xml.etree.ElementTree as ET; r = ET.parse('poker-server/build/test-results/test/TEST-duels.poker.server.DuelSocketWriterDirectoryTest.xml').getroot(); assert r.get('tests') == '4' and r.get('failures') == '0'"
  - python3 -c "import pathlib; s = pathlib.Path('poker-server/src/main/kotlin/duels/poker/server/session/ConnectionDirectory.kt').read_text(); d = [l for l in s.splitlines() if l.strip().startswith('public fun writerFor')]; assert len(d) == 1 and 'RoomCode' in d[0], d"
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A frame is delivered to the connection that is in the room the frame is about, so a player reading
a lobby is never told anything about a room they have left.

## The decision this is cut against

`DEC-107` is **answered and merged**:
[`ADR-0104`](../../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) — *a
frame reaches the connection that is in the room it is about*. The placeholder *Files* table and
`verify:` block this ticket carried until now are gone; both are measured below. Nothing here asks
for a `DEC`.

The whole mechanism is three edits to production code:

1. **`ConnectionDirectory.writerFor(player: PlayerId)` is deleted** and replaced by
   `public fun writerFor(player: PlayerId, room: RoomCode): ConnectionWriter?`, which answers the
   registered writer only when the connection registered for `player` is, **at the instant of the
   call**, in `room`. A connection that has entered no room answers `null` for every room (§1).
2. **`register` takes the connection's `RoomMembership`** —
   `register(player: PlayerId, writer: ConnectionWriter, membership: RoomMembership)` — one object
   shared by reference with the socket loop that writes it, never copied, so the two cannot drift (§2).
3. **`deliver`'s one edited line** — `val writer = connections.writerFor(player, room.code) ?: continue`.
   Its signature does not change and **none of its seven call sites is edited**: `Application.kt:182`
   and `DuelSocket.kt:222,498,568,586,642,645` (§1).

## The defect

On 2026-09-01 a human played a real duel through two browsers and could not play it. Both tables
rendered correct, agreeing game state and **each seat was told the other one had vanished**. One
screen: *Your rival is away. The duel is paused.* with a countdown running to `0s` and sticking
there. The other: *Your rival did not come back. The duel continues, and the server acts for
them.* Neither had left. Reproduced four times on the live stack at `e1a37a80`.

**The frame arrives before the room exists.** Window A's wire, captured over CDP, epoch ms:

```
1788289017171 IN   {"type":"Welcome","playerId":"d1d1edee-…","protocolVersion":5}
1788289021602 IN   {"type":"OpponentPresence","presence":"AWAY","graceRemainingMillis":60000}
1788289021922 OUT  {"type":"CreateRoom"}
1788289021924 IN   {"type":"RoomJoined","code":"0QA3WCB6","seat":0}
```

A holds no seat anywhere at `021602`. The frame is about the room A **left**, produced when the
*other* player's socket closed there, and delivered to the connection A opened afterwards.

**Two facts that bound what this ticket may claim.**

- **The server is not confused about the duel.** Under *The duel is paused.* the acting player's
  `Call 100` was **accepted**: stack 9,950 → 9,900, turn passed. Nothing here is a rules or pot
  defect, and nothing in this ticket touches the engine or the room model.
- **Nobody was folded.** In every reproduction the hand stayed at Hand 1, preflop. `ADR-0104` §9
  states a mechanism that fits the human's report of repeated folding and says in as many words
  that it is **not** evidence of the cause. **Do not write a test asserting a fold, and do not
  claim in the PR body that this explains one.** If a fold of a *present* player is ever
  reproduced after this lands, it is a new defect and a new ticket.

## Three things a coder can get wrong here

- **`@Volatile` is not this ticket's, and must not be undone by it.** `RoomMembership.code` carries
  it from [`TASK-121404`](TASK-121404-a-connections-room-becomes-a-session-type-the-directory-can-read.md),
  gated there by `RoomMembershipTest`. This ticket **stores the object and reads `membership.code`
  at lookup time**; it must not copy the `RoomCode` out at registration, because a copied value is
  read once and then stale forever, and no single-threaded test can see the difference.
  `ConnectionDirectoryTest.alookupFollowsTheRoomTheConnectionMovesTo` is the gate on that.
- **Membership-before-delivery becomes load-bearing ordering.** `replyToJoinRoom` sets `room.code`
  *before* delivering its resume frames and before a seating's outbound; `replyToCreateRoom` sets
  it before it replies. All four sites are already correct — a future one that delivered first
  would silently drop the frames it had just produced. `ADR-0104` §2 says to pin this with a test
  rather than a comment, and `DuelSocketWriterDirectoryTest` is where it is pinned: every one of
  its four cases now sends `CreateRoom` and looks the writer up by the room the server named back,
  so a connection whose membership were written after its frames would fail there.
- **A silent drop now has two indistinguishable causes.** A frame for a seat with no writer and a
  frame for a connection in another room are dropped identically, and `ADR-0104` accepts that as a
  cost: *"neither is counted or logged … no instrument points at it."* The ADR designs no counter
  and no log, so **this ticket adds none** — but do not write a comment or a PR sentence claiming
  the ambiguity is absent.

## Files

Measured by probing, not remembered (`ADR-0069`, `ADR-0070`). The three edits above were stubbed in
one tree on top of `TASK-121404`'s move, and `.github/workflows/build.yml`'s pull-request gate set
was run in full and repeatedly until it exited 0: `./gradlew check -PrequireDocker=true` with
Docker up (Colima, Engine 29.5.2), **no suite skipped**, `verifyProtocolTypes` and `verifyDuelScript`
both executed, ending **2 453 tests, 0 failures, exit 0**; then `npm ci`, `npm run check` (117 files,
985 tests) and `npm run build` in `web-client/`, all exit 0. Three red runs preceded it and each
named its own paths: a `ktlint` `no-consecutive-blank-lines`, then the twelve `compileTestKotlin`
errors quoted in `atomic:`, then an import-order violation. The probe was reverted and `git status`
came back empty. **No gate named a seventh path.**

**`files_touched: 2` on the previous cut was a pre-decision placeholder and is not evidence.**

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/ConnectionDirectory.kt` | modify | the change itself — `register` takes the membership, the unscoped `writerFor` is deleted, the scoped one replaces it, and `forget` still compares the writer |
| `poker-server/src/main/kotlin/duels/poker/server/SeatDelivery.kt` | modify | the Kotlin compiler — `writerFor(player)` is gone and line 40 is its **only** production call site in the tree |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify | the Kotlin compiler — `deps.connections.register(player.id, writer)` at line 195 loses its arity. One line; the `RoomMembership` this function already holds is the object to pass |
| `poker-server/src/test/kotlin/duels/poker/server/session/ConnectionDirectoryTest.kt` | modify | the Kotlin compiler — seven `register` and six `writerFor` call sites, `No value passed for parameter 'membership'` / `'room'` at lines 18, 20, 27, 36, 37, 39, 48, 49, 54, 61, 66 and 73 |
| `poker-server/src/test/kotlin/duels/poker/server/SeatDeliveryTest.kt` | modify | the Kotlin compiler — `connections.register(player, writer)` at line 59, `No value passed for parameter 'membership'` |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketWriterDirectoryTest.kt` | modify | the Kotlin compiler — `connections.writerFor(player.id)` at lines 72, 114, 119 and 155, `No value passed for parameter 'room'` |
| `docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md` | read | §1 the deletion, §2 the shared membership, §3 what production keeps doing, §7 what the tests must prove, §8 what does not change |
| `poker-server/src/main/kotlin/duels/poker/server/session/RoomMembership.kt` | read | `TASK-121404` created it; this ticket only registers it and reads `code` |

**Why this declares `atomic:` when `ADR-0104` §6 says it is not.** §6's subject is the version
bump — *"no declaration changes … `TASK-121403` is therefore not `atomic:` and carries none of the
twelve artifacts a bump carries"* — and that half is right: `PROTOCOL_VERSION` does not move.
`atomic:` is not a property of version bumps, though; it is a property of any merged gate that
refuses the intermediate state, and the probe found one. `ADR-0069` is the merged authority for
sizing by measurement over recollection, and it was applied here.

**Paths the gate set did not name, recorded so nobody adds them.** `RoomRegistry.kt`, `Room.kt`,
`ServerMessage.kt`, `protocol.gen.ts`, `docs/protocol-versions.md`, `Application.kt` and every e2e
suite stayed green untouched. `DuelSocketDisconnectTest` also stayed green untouched — including
`aThirdSocketInNoRoomIsToldNothing`, which **passes on the broken product**, because its third
socket uses a different `deviceId` and is therefore a different player. That is the same shape of
blind spot `STORY-1214` found in `CORE-18`, and closing it is
[`TASK-121405`](TASK-121405-the-measured-reproduction-is-a-server-test.md), not this ticket.

## Scope

- `ConnectionDirectory` stores the writer **and** the `RoomMembership` it was registered with, and
  `writerFor(player, room)` answers the writer only when `membership.code == room`, **read at the
  time of the call**. How the pair is stored is the coder's; the requirements are `ADR-0104` §2's
  two — `forget(player, writer)` still compares the writer and still returns `false` for an adopted
  socket's cleanup (`ADR-0018`), and the room is never cached by the caller.
- `deliver` resolves a seat to a player exactly as it does today, then looks the writer up with
  `room.code`, and skips silently when there is none — the same `?: continue` the missing-writer
  case already uses. Its KDoc gains the room-scoping sentence; `ADR-0104` records that it *amends*
  the contract stated there.
- `ConnectionDirectory`'s KDoc retracts *"no room, no seat"* to *"no seat, no `ServerMessage`, no
  Ktor"* and says why exactly one `RoomCode` is now in scope. The rest of that KDoc stands.
- Every existing test in the three test files keeps its intent. The four
  `DuelSocketWriterDirectoryTest` cases become room-scoped by sending `CreateRoom` and using the
  `RoomJoined` code they get back — they are **not** weakened to `connections.size`, and no
  assertion is deleted.

## Out of scope

- **Anything on the wire.** No `ClientMessage` or `ServerMessage` declaration changes, so
  `PROTOCOL_VERSION` stays at 5, `ADR-0047` §2's fingerprint is unchanged, `docs/protocol-versions.md`
  gains no row and `protocol.gen.ts` stays byte-identical (`ADR-0104` §6). `verifyProtocolTypes`
  ran green throughout the probe.
- **Editing any of the seven `deliver` call sites.** `ADR-0104` §1 states they are untouched, and
  the probe confirmed it.
- **`RoomRegistry`, `Room` and `Room.presenceOf`.** `disconnect()` still produces
  `OpponentPresence(AWAY, …)` whether or not the other seat has a connection in that room —
  `ADR-0104` §3 answers that half of `DEC-107` *yes, unchanged* — and the registry must not learn
  about writers.
- **The client half.** `ADR-0104` §4's `RoomJoined` reset is
  [`TASK-121406`](TASK-121406-the-store-is-scoped-to-the-room-the-server-last-named.md), and the
  ADR says in as many words that it is **not** what makes the system correct.
- **The socket-level reproduction.** `TASK-121405`.
- **A counter, a log line or a metric for a dropped frame.** `ADR-0104` leaves that open by name;
  inventing one here would be a design decision this ticket has no mandate for.
- **`DEC-109`** — may one player hold seats in two live rooms at once. The product owner's, and
  `ADR-0104` §10 says it blocks nothing here.
- **The action bar under a paused notice** (`DEC-108`), **the grace window's length**,
  **`ADR-0023`'s absent-seat action**, and **`ADR-0046`'s three sentences**. The words were
  correct; they were shown to the wrong player.
- **`docs/test-plan.md`.** `TASK-121401` owns the catalogue.

## Tests

Three classes. The counts in `verify:` are today's measured numbers plus the additions below —
`SeatDeliveryTest` 5 → 8, `ConnectionDirectoryTest` 6 → 9, `DuelSocketWriterDirectoryTest` 4 → 4.

`SeatDeliveryTest` — its `deliverAndDrain` helper gains one defaulted parameter,
`connectedTo: Map<PlayerId, RoomCode?> = emptyMap()`, naming the room each writer's connection is
in; anybody absent from it is registered in `room.code`, so the five existing tests keep their
current text and their current meaning.

| Test | Proves |
| --- | --- |
| `aframeIsNotDeliveredToAConnectionInAnotherRoom` | the guest's connection is in a second room; seat 1's frame reaches nothing, and seat 0's still reaches the host — so the drop is scoped, not total |
| `aframeIsNotDeliveredToAConnectionInNoRoom` | the measured reproduction at the unit that performs the composition: the guest's connection has entered no room and receives nothing, while the host still receives its own frame |
| `apresenceFrameCrossesNoRoomEither` | the same crossing for an `OpponentPresence`. `deliver` does not read the message, so this cannot fail while the two above pass — it is here because §7 asks for it in as many words, and because a later change that special-cased presence inside `deliver` would break it |

`ConnectionDirectoryTest` — the six existing tests migrate to `writerFor(player, room)`; the
negatives need a second, distinct room code. **`ZYXWVUTS` is not a legal `RoomCode`** — `U` is
excluded from the Crockford alphabet, and the probe hit exactly that `IllegalArgumentException`.
`ZYXWVTSR` is legal.

| Test | Proves |
| --- | --- |
| `awriterIsFoundOnlyForTheRoomItsConnectionIsIn` | two inputs, one registration: the writer for its own room, `null` for the other. One input could not tell scoping from an unconditional answer |
| `aconnectionInNoRoomHasNoWriterForAnyRoom` | a membership with `code == null` answers `null` for both rooms while `size` stays `1` — registered, and reachable from nowhere |
| `alookupFollowsTheRoomTheConnectionMovesTo` | the membership is shared by reference and read at lookup time: mutating `code` after `register` moves the answer. This is the gate on §2's "never cached by the caller" |

`DuelSocketWriterDirectoryTest` — no new tests. All four keep their names and their assertions and
become room-scoped, which is what pins membership-before-delivery at the socket level.

**The negative that fails if scoping is too strong** is the rest of the suite, which is why
`./gradlew check -PrequireDocker=true` is in `verify:` and not the three filters alone. An ordinary
duel, a disconnect and a resume must still deliver everything they deliver today — **including the
routed refusals**, `Rejected` and `Room.act`'s `Failure(DUEL_PAUSED)`, which travel through
`deliver` back to the seat that acted and which a reader thinking of `deliver` as the fan-out path
alone will not expect. A `deliver` that dropped everything would pass the three filters above and
fail here.

**Red before, green after — measured, not asserted.** With the change staged and the single
expression `writerFor` returns mutated back to `develop`'s semantics (the room ignored), exactly
six tests fail and no others: the three new `SeatDeliveryTest` cases and the three new
`ConnectionDirectoryTest` cases. Run it both ways and quote both runs in the PR body.

## Acceptance criteria

- [ ] `SeatDeliveryTest` reports **8** tests, 0 failures — including
      `aframeIsNotDeliveredToAConnectionInAnotherRoom`, `aframeIsNotDeliveredToAConnectionInNoRoom`
      and `apresenceFrameCrossesNoRoomEither`
- [ ] `ConnectionDirectoryTest` reports **9** tests, 0 failures — including
      `awriterIsFoundOnlyForTheRoomItsConnectionIsIn`, `aconnectionInNoRoomHasNoWriterForAnyRoom`
      and `alookupFollowsTheRoomTheConnectionMovesTo`
- [ ] `DuelSocketWriterDirectoryTest` reports **4** tests, 0 failures, with all four names unchanged
- [ ] `ConnectionDirectory.kt` declares exactly one `public fun writerFor` and it names a
      `RoomCode` — the fourth `verify` command, which exits 1 on `develop` today
- [ ] `./gradlew check -PrequireDocker=true` exits 0 with no suite skipped
- [ ] The PR body quotes the run with the scoping and the run with it mutated away, and the second
      shows exactly the six failures named in §Tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
