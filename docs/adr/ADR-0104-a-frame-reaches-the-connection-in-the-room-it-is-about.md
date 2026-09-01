# ADR-0104 — A frame reaches the connection that is in the room it is about

- **Status:** Accepted
- **Date:** 2026-09-01
- **Resolves:** `DEC-107`
- **Upholds** [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) §1 rather than reopening
  it: `OpponentPresence` keeps its two fields and gains neither a room nor a seat, because §1's
  reason — *"a second thing to get wrong at the one place that already decides where a frame
  goes"* — is exactly the reason to make that one place correct instead. §5's emission table is
  untouched; production is unchanged.
- **Amends** the delivery contract stated in `SeatDelivery.kt` and `ConnectionDirectory.kt`'s
  KDoc: a writer is looked up by player **and room**, and `ConnectionDirectory`'s documented
  ignorance of rooms is retracted by exactly one value type.
- **Constrains:** [`TASK-121403`](../../tasks/tasks/TASK-121403-presence-is-about-the-room-the-reader-is-in.md),
  whose *Files* table and `verify:` block the planner re-cuts from this ADR, and every future
  `deliver` call site
- **Registers, and does not answer:** `DEC-109` — **the product owner's** — may one player hold
  seats in two live rooms at once? See §10.

## Context

On 2026-09-01 a human played a duel through two browsers and could not play it. Both tables
rendered correct, agreeing game state and each seat was told the other one had gone.
[`STORY-1214`](../../tasks/stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md)
measured the mechanism on the wire across four reproductions and a controlled negative: an
`OpponentPresence(AWAY, 60000)` arrives **320 ms before the recipient sends `CreateRoom`**, on a
connection that holds no seat anywhere, and with no stale frame in flight the same create-and-join
produces **zero** presence frames and a clean duel.

### The reading is confirmed, and the defect is wider than the reading

`STORY-1214` offered its reading of the source as a reading. Read again here, it holds — and it
understates what is broken.

`deliver` composes two lookups. The first, `seat -> PlayerId`, is a fact about **one room**:
`room.host` and `room.guest`. The second, `PlayerId -> ConnectionWriter`, is a fact about **the
player anywhere**: `ConnectionDirectory` answers, in its own words, *"which writer belongs to this
player right now"*. Composing them produces a third thing neither of them claims — *the writer this
player is using, for a seat they hold in some room* — and nothing in the composition requires those
two rooms to be the same room.

**That widening is not specific to presence.** Counted from every `Addressed(...)` construction in
`poker-server`, **nine of `ServerMessage`'s eleven subtypes** are routed through `deliver`:
`Snapshot`, `Events`, `YourTurn`, `DuelFinished`, `Rejected`, `Failure` (`Room.act`'s `DUEL_PAUSED`),
`ActedForAbsent`, `RematchOffered` and `OpponentPresence`. Only `Welcome` and `RoomJoined` are never
routed, and both are direct replies on the asking socket's own writer. A player who holds a seat in
room X while connected somewhere else receives **all nine** for room X. This needs no race: a
player who drops out of a live room X and does not rejoin keeps their seat there — nothing on the
wire says *leave* ([`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md)
§Context, force 3; `ClientMessage` has five subtypes and none of them is `LeaveRoom`) — so
`ADR-0025`'s ticker expires their grace window, `foldAbsentSeats` plays their hand for them, and the
resulting `ActedForAbsent`, `Events`, `Snapshot` and eventually `DuelFinished` are addressed to that
seat and delivered to whatever connection that player now has. `ADR-0072` §Context records what a
`DuelFinished` does when it lands on a store: *"`Lobby.tsx`'s first branch (`state.outcome !== null`,
deliberately ahead of `view` and `roomCode`) puts the same result screen back up."*

Nothing here can leak a card. `deliver` still resolves a seat to the player who holds it, so the
recipient of a projected frame is always the player it was projected for; the widening is about
**which of that player's screens** receives it, never about **who** does.

### Three facts about membership that any answer has to survive

- **A connection knows its room; the directory does not.** `RoomMembership` — a `private class` in
  `DuelSocket.kt` with a single `var code: RoomCode?` — is the per-connection record, written at the
  four places a connection enters a room and read by `replyToAct`, `replyToOfferRematch` and the
  `finally` block. It is precisely the fact `deliver` lacks, and it is one field away from it.
- **A connection can change rooms without disconnecting.** `replyToCreateRoom` overwrites
  `room.code` unconditionally, and `replyToJoinRoom` does too. Neither vacates the seat the
  connection already held, because nothing can. So *"the room this connection is in"* and *"the
  rooms this player is seated in"* are different sets, and only the second is visible to `deliver`
  today.
- **A writer is a mailbox, not a socket.** `ConnectionWriter` buffers up to 64 frames on a
  `Channel` drained by one coroutine. A frame handed to `send` has been *queued*, not written, so
  even a perfect check at hand-off cannot be a check at arrival.

### What is in tension

**`ADR-0002` against the cheapest repair.** A client-side reset on `RoomJoined` is one line and it
kills the measured reproduction outright — the stale frame arrives *before* `RoomJoined`. It also
puts the rule that decides whether a server statement counts in the least authoritative place, and
`ADR-0002`'s inbound half already shows the house answer: an out-of-order client action is *dropped
by the server*, not filtered by the client. The symmetric rule is that a frame for a room the
recipient is not in is not sent.

**`ADR-0028` §1 against a self-describing frame.** A room on the wire would let any client, present
or future, check the frame with no server bookkeeping at all. It also costs `PROTOCOL_VERSION`
5 → 6 and everything `ADR-0047` §6 and `ADR-0068` attach to a bump, reopens the one rule `ADR-0028`
§1 wrote down, and fixes exactly one of the nine message types the defect reaches.

**`ConnectionDirectory`'s ignorance is load-bearing and wrong.** Its KDoc — *"no room, no seat, no
`ServerMessage`, no Ktor"* — is why it is easy to reason about, and it is also the exact reason it
cannot answer the question delivery needs to ask. Something has to give.

### The deadline, honestly

There is one, and it argues for the option not taken. **No client is deployed**, so a wire break is
free today and a compatibility window later — the same deadline `ADR-0027` and `ADR-0028` §8 both
named. It is recorded here so the next reader knows it was weighed rather than missed: a version
step is cheap *now*, and this answer still declines to spend one, because the frame is not where the
defect is. Against that, the frames are reaching lobbies today and the product is unplayable by
hand, so the cost of not deciding is measured in duels, not in foreclosure.

## Decision

**A frame is delivered to the connection that is in the room the frame is about. `deliver` resolves
a seat to a player exactly as it does today and then asks for that player's writer *for this
room*; a connection in another room, or in none, is skipped silently, exactly as a seat with no
writer already is. Nothing is added to the wire, and nothing is added to the room model.**

### 1. There is no unscoped writer lookup

`ConnectionDirectory.writerFor(player: PlayerId)` is **deleted** and replaced by

```kotlin
public fun writerFor(player: PlayerId, room: RoomCode): ConnectionWriter?
```

which answers the registered writer only when the connection registered for `player` is, at the
instant of the call, in `room`. A connection that has entered no room answers `null` for every room.

Deleting the unscoped overload rather than adding a scoped one beside it is the whole mechanism. A
rule that can be bypassed by calling the other method is a convention; a rule with no other method
is structural, and the next `deliver` call site inherits it without being told.

`deliver`'s signature does not change. It already takes the `Room` the frames are about and
`Room.code` is one of its fields, so the one edited line is

```kotlin
val writer = connections.writerFor(player, room.code) ?: continue
```

**Seven `deliver` call sites, none of them edited** — `Application.kt`'s sweep and six in
`DuelSocket.kt`.

### 2. A connection's room is registered with its writer

`RoomMembership` moves out of `DuelSocket.kt` into `duels.poker.server.session`, beside `Session`,
and is registered with the writer it belongs to:

```kotlin
public fun register(player: PlayerId, writer: ConnectionWriter, membership: RoomMembership)
```

There is **one** `RoomMembership` per connection, shared by reference between the socket loop that
writes it and the directory that reads it, so the two cannot drift. Its `code` is `@Volatile`: it is
written by one coroutine and read by others — the ticker's sweep and the other seat's socket — and
without it a delivery decision may read a room the connection left. That is the single most
important word in this ADR, because getting it wrong fails invisibly and never on one thread.

Two invariants make the whole thing sound, and both are already true of the merged code:

- **Membership is written before any `deliver` on that path.** `replyToJoinRoom` sets `room.code`
  before delivering resume frames and before delivering a seating's outbound; `replyToCreateRoom`
  sets it before it replies. A future call site that delivers before entering the room would
  silently drop the frames it just produced, so this is pinned by a test (§7), not by a comment.
- **Removal still matches on the writer.** `forget(player, writer)` keeps `ADR-0018`'s guard
  exactly: an adopted socket's cleanup removes nothing and returns `false`. How the entry is
  stored is the ticket's; the requirements are that removal compares the writer, and that the room
  a lookup compares against is read at lookup time and never cached by the caller.

`ConnectionDirectory` now knows one more thing than it did: a `RoomCode`. Not a `Room`, not seating,
not a `ServerMessage`, not Ktor — the rest of its KDoc stands, and `duels.poker.server.session`
already imports from `duels.poker.server.room` (`SocketDependencies` holds a `RoomRegistry`), so no
module boundary moves.

### 3. Production does not change, and `disconnect()` still builds the frame

`RoomRegistry.disconnect` produces `OpponentPresence(AWAY, …)` addressed to the other seat whether
or not that seat has a connection in this room, exactly as it does today, and `ADR-0028` §5's
emission table is untouched. The registry knows nothing of writers by design and must not learn:
its own KDoc is *"nothing here reaches outside the engine and the room model"*, and `Room.presenceOf`
is a pure projection of room state. A frame that finds no eligible connection is dropped at
delivery, which is the idiom `deliver` already documents for a seat mid-reconnect.

This answers the half of `DEC-107` registered as *"should `disconnect()` produce a frame at all when
the other seat is no longer connected to that room?"* — **yes, unchanged.**

### 4. The client's store is scoped to the room the server last named

`applyServerMessage`'s `RoomJoined` case: when the store already holds a **different** `roomCode`,
the state is re-initialised to `initialState()` before the new room's fields are applied. The
monotone counters — `rejectionCount` and `presenceCount` — carry over rather than reset, because
they exist to be strictly-increasing change tokens and a reset can make two different states compare
equal. A `RoomJoined` naming the room the store already holds clears nothing beyond the `refusal` it
clears today, so a resume is untouched: `ADR-0028` §5 sends the returning seat its opponent's
current presence immediately after `RoomJoined`, and the `ALREADY_SEATED` branch — which sends no
presence — keeps whatever the store had.

This is the client scoping **its own derived state** to the room the server last named. It is not
the client judging a server statement, and it is **not** what makes the system correct: §1 is. It
bounds the one window §1 cannot close, and §5 says exactly which.

### 5. A frame in flight when a seat leaves

Three windows, and the rule survives all three:

| When the seat leaves | What happens to the frame |
| --- | --- |
| **Built, not yet delivered** — `RoomRegistry` builds under the room's mutex, `deliver` runs after it is released | Dropped. The check is at `writerFor`, so the membership that decides is the one at the instant of the write, not the one at the instant of the build. Correct by construction: the recipient no longer holds that room's view, and `ADR-0028` §5 hands them the current presence the moment they resume. |
| **Queued in the writer's mailbox, or on the network, and the recipient leaves by disconnecting** | Never read. `ConnectionWriter.send` answers `false` once closed and the socket is gone; the client that reconnects gets a fresh state and, on resume, a fresh presence. |
| **Queued or on the network, and the recipient moves to another room on the same socket** | Delivered and applied — this is the one case a server-side check cannot close, because the frame was correct when it was written. §4 is what makes it harmless: the `RoomJoined` for the new room arrives after it, on the same ordered connection, and re-initialises the store. |

The third window is **not reachable from the shipped client**, and that is worth stating rather than
relying on: every way out of a room in the web client is a real navigation (`ADR-0072` §5 — an
`<a href="/">`, deliberately not a `window.location` call), so the socket dies and the next one
registers with no room at all. The protocol permits the move — `CreateRoom` is accepted on a
connection already seated — and §4 is the insurance against a client that one day takes it.

### 6. `PROTOCOL_VERSION` does not move

No `ClientMessage` or `ServerMessage` declaration changes, so `protocolDeclarations()` emits the
same text, `ADR-0047` §2's fingerprint is unchanged, `docs/protocol-versions.md` gains no row, and
`protocol.gen.ts` is byte-identical. `TASK-121403` is therefore **not** `atomic:` and carries none
of the twelve artifacts a bump carries.

### 7. What the tests must prove

- **The measured reproduction, as a server test.** A player seated in room X, a disconnect in X, and
  that player's *new* connection — which has entered no room — receives **nothing**. This is the one
  that would have caught it.
- A presence frame produced for a seat whose player is now connected to room **Y** reaches neither
  Y's socket nor anyone else's, and Y's own frames are unaffected.
- The same, for a frame that is not presence: room X's `Snapshot` (or `DuelFinished`) does not reach
  a connection in room Y. Presence-only assertions would let the widening survive under a different
  message type.
- **A negative that fails if scoping is too strong**: an ordinary two-seat duel, a disconnect and a
  resume all deliver everything they deliver today, **including the routed refusals** — `Rejected`
  and `Room.act`'s `Failure(DUEL_PAUSED)` travel through `deliver` back to the seat that acted, and
  a reader who thinks of `deliver` as the fan-out path alone will not expect them. A gate that only
  proves frames are dropped is passed by a `deliver` that drops everything.
- Membership is set before delivery on every path that delivers: a resume delivers its resume
  frames, and a seating delivers the host's opening frames.
- **The reducer clears on a different room and not on the same one** — two inputs, or the assertion
  cannot tell a room-scoped reset from an unconditional one.

### 8. What does not change

The wire. `poker-engine` — nothing here reaches it. `Room`, `RoomRegistry` and `Room.presenceOf`.
`ADR-0028` §§1–6 and its emission table. `ADR-0018`'s adoption rule and `forget`'s writer match.
`ADR-0013`'s window, `ADR-0023`'s choice of action, `ADR-0025`'s sweep. Every direct `send` on a
socket's own writer, which is about that socket's own room by construction and is not routed.
`ADR-0046`'s copy, and `DEC-108`, which is the product owner's and is untouched by this.

### 9. What this says about the fold, and what it does not

The human reported a player being folded repeatedly. **No reproduction ever produced a fold** — every
run stayed at Hand 1 preflop — and `STORY-1214` records it as unexplained. This ADR does not explain
it either.

What it can say is that the widening in §Context reaches the frames a fold produces:
`ActedForAbsent`, `Events` and the `Snapshot` after them are addressed to the absent seat and
delivered to that player's current connection wherever it is, so a player who left room X and is
sitting elsewhere would see room X's fold arrive on the screen they are actually looking at. That is
a **mechanism that fits the report**, derived from merged code, and it is **not** evidence that this
is what happened — nothing measured it, and a plain `RoomRegistry.expireGracePeriods()` folding a
genuinely absent seat fits the report just as well. This decision closes the delivery path whether
or not it was the cause. If a fold of a *present* player is ever reproduced after this lands, it is
a new defect and a new ticket, and this ADR is not its answer.

### 10. What is registered rather than answered

**`DEC-109` — the product owner's: may one player hold seats in two live rooms at once?** The
evidence forced the question and this answer does not need it. `CreateRoom` and `JoinRoom` are
accepted on a connection that already holds a seat, nothing vacates the old one, and the old room
then plays that seat's hands for it under `ADR-0023` and, under `ADR-0014`, settles a coin on the
result. Whether that is a second duel, a forfeit of the first, or something the server should refuse
outright is a question about what a duel is and what a coin is worth — `docs/vision.md`'s, not an
architect's. Room-scoped delivery is correct under every answer: if two rooms are allowed their
frames must not cross, and if they are not the second request is refused and delivery is scoped
anyway. It blocks nothing here.

## Consequences

**What it buys.** The composition that produced the defect is closed at the single point that
performs it, for every message type at once and with no call site edited. The server stops saying
something untrue about a room the listener is not in, which is `ADR-0002` applied in the direction it
had not been applied yet. `OpponentPresence` keeps the shape `ADR-0028` §1 argued for, and the
argument is vindicated rather than reopened: the one place that decides where a frame goes was the
right place to fix. The wire does not move, so `TASK-121403` is an ordinary ticket rather than a
twelve-artifact bump, and it is cheap to reverse — a deleted overload, a moved class and one
comparison, against a version step that can never be taken back.

**What it costs.**

- **`ConnectionDirectory` stops being ignorant of rooms.** A class whose whole value was that it
  knew about players and writers *and nothing else* now has a second reason to change, and the next
  fact somebody wants to scope by will find the precedent already set.
- **A mutable field is now read across coroutines.** `RoomMembership.code` needs `@Volatile`, and
  that obligation did not exist before. Omit it and delivery decisions read a stale room under load
  — a frame dropped or delivered at random, invisible to every single-threaded test in the suite.
  Nothing in the type system asks for it; only §2 and a reviewer do.
- **Silent drops get harder to debug.** A frame that should have arrived and did not is now
  indistinguishable from a frame for a seat with no writer, and neither is counted or logged. *"Why
  did my client not receive that?"* has one more answer than it had, and no instrument points at it.
- **An ordering requirement becomes load-bearing.** Membership must be written before `deliver` on
  every path that delivers. It is true at all four sites today; a future path that delivers first
  drops its own frames and reads as a mystery, not as a rule broken.
- **Sending to a player wherever they are stops being available.** That is deliberate, and it has a
  named victim: `EPIC-11` — status notifications — has no scoped room to deliver into, so it will
  need its own path and its own decision about what a player may be told while sitting at another
  table. This ADR makes that a decision instead of an accident.
- **The client half is unreachable from the shipped UI.** §4's branch guards a window only a client
  that moves rooms on one socket can open, so it can rot with every test green unless a test drives
  the reducer directly. It is written because the protocol permits the move, not because the product
  takes it — and that is exactly the kind of code that gets deleted by someone tidying up.
- **The last window is not closed, and cannot be.** A frame written while the connection was in the
  room and read after it left is correct at the write and stale at the read; no server-side check
  removes that. §4 bounds it; nothing eliminates it.

**What it forecloses.** The unscoped lookup, on purpose — see the `EPIC-11` cost above. It does not
foreclose a room on the wire: if a future client ever needs to check a frame's room itself (a
spectator under `ADR-0040`, a replay, a proxy), `OpponentPresence` can gain the field at the price of
a version step, and this decision makes that a choice rather than a repair. It does not foreclose
`DEC-109` in either direction, and it takes nothing away from `DEC-108`.

## Alternatives considered

**Put the room on the wire and have the client discard a mismatch.** The strongest case, and it is
strong: the frame becomes self-describing, so *any* consumer — a client, a log, a proxy, a future
spectator under `ADR-0040` — can check it with no server-side bookkeeping, no shared mutable
membership and no `@Volatile`. It survives a server that gets its own routing wrong, which is the
failure actually observed. And the wire break is **free today** — nothing is deployed — while it will
not be free later. Rejected on three counts. It fixes one message type while the defect is in a
composition shared by nine, so `Snapshot`, `Events`, `YourTurn` and `DuelFinished` would keep
crossing rooms and the honest version of this option is a room field on *every* routed
`ServerMessage`. It
leaves the server sending a frame it should not have sent and asks the least authoritative party to
notice, which is the wrong side of `ADR-0002`. And it costs `PROTOCOL_VERSION` 5 → 6 with `ADR-0047`
§6's ledger row and `ADR-0068`'s `atomic:` gate, spent to make a correct server's output
self-checking — the most expensive and least reversible thing in the option set, bought for a
property the fix in §1 makes unnecessary.

**A client-side reset on `RoomJoined`, and nothing else.** One line in a reducer, no server change,
no wire change, no new server state, and it demonstrably kills the measured reproduction — the stale
frame arrives 320 ms *before* `RoomJoined`, so the reset wipes it and the duel is playable. It is
the cheapest thing that turns the recorded trace green. Rejected because that is all it does. Room X
lives until `ADR-0022` reaps it and goes on producing frames the whole time — `ADR-0025`'s ticker
expiring a grace window is precisely such a producer — so every one of them that arrives *after*
`RoomJoined` is applied with nothing to test it against, and the general case is the one that
arrives later. It also leaves a lobby receiving a `DuelFinished` for a room it left, which
`ADR-0072` §Context tells us renders a result screen. This is the trap in the option set: it passes
the reproduction and leaves the defect. It survives here only as §4, where it is bounded, labelled
and explicitly not load-bearing.

**Bind presence production to the seat a connection currently occupies** — have `RoomRegistry` ask,
before building the frame, whether the other seat's player is connected to *this* room. Its case: no
frame is ever produced that will be dropped, `Disconnection.outbound` becomes honest about what it
achieved, and no shared mutable membership is read from another coroutine. Rejected because it puts
a transport fact inside the room model, which `RoomRegistry`'s own contract forbids — *"nothing here
reaches outside the engine and the room model"* — and because it would not even be sufficient: the
check would run under the room's mutex at build time and the connection can move before `deliver`
runs, so the delivery check is needed anyway and this would be a second, weaker copy of it in a
layer that should not have one. It is also presence-only, and the defect is not.

**A room check inside `deliver`, comparing `room.code` against a membership the caller passes in.**
Its case: `ConnectionDirectory` keeps its ignorance intact, and the check is visible at the one
function that already decides where a frame goes. Rejected because `deliver` has no recipient
connection to ask — it has the *sender's* context, and the recipient is the other seat — so the
membership would have to be looked up by player, which is `ConnectionDirectory`'s job and is §1. The
version of this that works is §1 with the comparison written out in `deliver` instead of inside
`writerFor`, and that leaves the unscoped lookup in place for the next call site to use by mistake.

**Give the room its own two writers, so no `PlayerId` lookup is involved.** The most direct
expression of the invariant: a room delivers to the connections seated in it, and the widening
cannot be written down. Rejected because `Room` is a pure `data class` of `val`s that
`RoomRegistry` copies on every mutation and hands out as a snapshot; putting a `ConnectionWriter`
in it would give the room model transport objects, break its equality and its copy-on-write
discipline, and keep dead sockets alive inside room snapshots the registry has already replaced.

**Refuse `CreateRoom` and `JoinRoom` from a connection already seated elsewhere.** Its case: it
removes the second room, so the frames have nowhere wrong to go, and it is the only option that
addresses the underlying oddity rather than its symptom. Rejected because it is not an architect's
to make — whether a player may be in two duels at once is `DEC-109`, the product owner's — and
because it would not be sufficient if answered *yes*, nor necessary if answered *no*: delivery must
be room-scoped under either answer, since a player disconnecting and reconnecting is not a second
room and produces the same crossing.

**Do nothing on the server and delete the presence feature until it can be scoped.** The honest
minimal option, and it would make the product playable today: no `OpponentPresence`, no stale frame,
no deadlock. Rejected because `ADR-0028` records a human product decision made verbatim, the pause
is real whether or not it is announced, and withdrawing an announced pause returns the product to
the dishonesty `ADR-0028` §Context exists to end. The frame is not the defect; where it is sent is.
