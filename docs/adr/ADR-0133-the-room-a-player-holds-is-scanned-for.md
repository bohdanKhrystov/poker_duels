# ADR-0133 — The room a player holds is scanned for, and only the `PLAYING` refusal spends a frame

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-110` — **the architect's** — by what mechanism is a player who asks for a second
  room while already holding a seat found, and answered? Registered open 2026-09-01 by
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6 for the
  `PLAYING` case, and **widened to the `WAITING` case on 2026-09-06** by
  [`ADR-0124`](ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) §6. One lookup over both
  live states, as those two ADRs asked for.
- **Decides only the mechanism.** What the player experiences is already merged and is not reopened
  here: `ADR-0124` §§1–5 for the `WAITING` state (handed back, nothing said, no string), and
  `ADR-0105` §§1–5 for the `PLAYING` state (refused, `You are already in a duel. Finish it to start
  another.`, landed back at their own table). Every clause of both stands byte-unchanged.
- **Applies:** [`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md) (a room is serialised
  by its own mutex, and nothing under that mutex does I/O);
  [`ADR-0002`](ADR-0002-server-authoritative.md) (which room a player sits in is the server's fact,
  and a connection's record of it is a cache);
  [`ADR-0104`](ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) (a frame reaches
  the connection in the room it is about);
  [`ADR-0018`](ADR-0018-a-second-socket-adopts-the-seat.md) (one live session per player, the
  newest socket holds it); [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) §2 (a refusal hands
  out no oracle about the code that was asked for);
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §5 (a frame a reducer treats
  as *where a screen begins* is never overtaken by one that decorates it);
  [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) (a wire shape change is a
  ledger row and a version step) with
  [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) and
  [`ADR-0070`](ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md) (the ticket
  that spends one is `atomic:` and is sized by a probe run to green).
- **Constrains:** `RoomRegistry`, `DuelSocket.replyToCreateRoom` and `DuelSocket.replyToJoinRoom`,
  `ProtocolError`, and how `STORY-1416` and the ticket implementing `ADR-0105` §1 are split. It
  constrains no engine code, no schema, nothing stored, no string and no layout.
- **Registers nothing.** Two questions are named in §10 and deliberately left unregistered, on
  `ADR-0105` §6's rule that a `DEC` nobody is working is noise in the open table.

## Context

Two merged ADRs decided what happens to a player who asks for a room while already holding a seat,
and both deliberately declined to write the repair. `ADR-0105` §6 and `ADR-0124` §6 between them fix
four things the mechanism must satisfy — the room is found **from the player, never from the
connection**; nothing moves; the client must be able to **name** the room it is put back in, on a
device that has never heard of it; and the refusal must not meter against `ADR-0022` §2's
failed-join budget when that budget is built. What is left is genuinely open, and five forces pull
on it.

**There is no way to ask "where does this player sit".** Read at `develop` `0e83537c`:
`RoomRegistry` is keyed by `RoomCode` and by nothing else — `rooms: ConcurrentHashMap<RoomCode,
Holder>` — and its only lookup is `get(code)`. Every existing question about a player is asked *of a
room already named*: `Room.seatOf(player)`, `join(code, player)`, `resume(code, player)`. The
connection has `RoomMembership`, but `ADR-0124` §6 rules it out by name and gives the reason — a
second device has none, which is precisely the route `ADR-0105`'s Context named and nothing
answered. So the mechanism has to add a lookup this class has never had, and the shape of that
lookup is most of this decision.

**Two shapes are available and they fail differently.** A **scan** derives the answer from the rooms
themselves and therefore cannot disagree with them, at O(live rooms) per call. An **index** —
`Map<PlayerId, RoomCode>`, maintained by `create`, `join`, `finish`, `abandon`, `reap` and the
rematch path — answers in O(1) and becomes a **second source of truth about where a player sits**,
which `ADR-0002` says the room already is. This class has a worked example of what a derived map
costs: `recording`'s own KDoc documents that if `DuelResultSink.record` never returns, its entry
never clears and the room leaks permanently — *"a leaked in-memory room is recoverable; a lost duel
result is not."* The equivalent leak in a player index is a player who can never open a room again.

**Deadlock is the live hazard, and the shape that causes it is obvious once named.** `ADR-0016` puts
every read-modify-write behind that room's own mutex. A lookup that has to *walk* rooms while
another room's mutex is held is a two-lock operation with no natural order — room A's holder waiting
on room B's while B's waits on A's — and it is the one way this registry can hang. Whatever the
lookup is, its lock discipline has to be stated rather than left to be inferred by the next person
who adds a caller.

**The two states need the same lookup and different answers.** `ADR-0124` §1 hands a `WAITING`
holder their room back with nothing said; `ADR-0105` §1 refuses a `PLAYING` holder with two
sentences and lands them at their table. `ADR-0124` §6 predicted **no new frame** for its own half,
because `RoomJoined(code, seat)` already means *you are seated in this room, at this seat* — and
explicitly left the `PLAYING` half's carrier to this decision: *"The architect may still spend one
on `DEC-110`'s `PLAYING` half; that is their call."*

**`replyToCreateRoom` is three lines that read nothing about the player**, and `ADR-0118` named that
as the thing which arms its own window: a press inside the recovery gap is honoured, repoints the
connection's membership and makes `boot.ts` overwrite the remembered code, orphaning the room the
player holds. The guard this decision builds is what disarms it.

### The deadline

`STORY-1416` waits on this and on nothing else, and the ticket implementing `ADR-0105` §1 has never
been cut. Two things argue for answering now; neither argues for a particular answer.

**A wire value is free today and not later.** No client is deployed, so a `ProtocolError` value and
the `PROTOCOL_VERSION` step it forces cost a ledger row and nothing else — `ADR-0044`'s argument,
unchanged, and the same one `ADR-0105`'s own deadline made. Once a client is in a browser it is a
compatibility problem instead.

**Answering half of it builds the design twice.** `DEC-110`'s row says so, and it is the reason the
decision was widened rather than split: a lookup shaped for `WAITING` alone would be reshaped for
`PLAYING` inside one epic.

## Decision

### 1. One lookup, on `RoomRegistry`, and *held* means a seat in a live room

`RoomRegistry` gains one public, **non-suspending** query:

> `public fun heldRoom(player: PlayerId): Room?` — the room this player currently holds a seat in,
> or `null`.

A room is **held** when `Room.seatOf(player)` is non-null **and** `Room.state` is `WAITING` or
`PLAYING`. `FINISHED` and `ABANDONED` rooms are excluded **by this method, not by its callers**:
`ADR-0105` §2 allows a holder of a finished seat a fresh room, and `Room.offerRematch` needs that
seat to stay exactly where it is, so a caller that forgot to filter would silently break both. The
filter lives in the one place a reader will look for it.

It returns the whole `Room`, never a boolean or a bare code: the caller needs the state to choose
its answer, the code to name the room, and the seat to address the frame, and all three must come
off one snapshot rather than three separate reads of a moving registry.

**The answer is total and does not depend on iteration order.** A player can hold two live rooms
today — that is the state `ADR-0105` §2 left open and `ADR-0124` closed one door of — and a server
answer that depends on a hash map's iteration order is neither testable nor explicable. When more
than one room qualifies, `heldRoom` returns the first under this order:

1. `PLAYING` before `WAITING` — a running duel outranks a waiting room, because it is the one that
   ends in a coin, and because `ADR-0105` §1 is the stronger rule of the two;
2. then ascending `Room.lastActivityAt` — for a `WAITING` room that stamp is written at `Room.open`
   and by nothing else, so the oldest room is the one whose code has been out longest and the one
   that dies first;
3. then ascending `RoomCode.value` — so the function is total even when two stamps collide.

### 2. It is a scan, and it takes no lock at all

`heldRoom` iterates `rooms` and reads each `Holder.room` through its existing `@Volatile` field —
the same read `get(code)` makes, and the same read `reap`'s first, unlocked pass makes. It acquires
**no mutex**, holds none, and therefore can neither block nor be blocked. `ConcurrentHashMap`'s
weakly consistent iteration is what makes that safe: the walk never throws, and it needs no
consistent snapshot because it is choosing among rooms rather than mutating one.

It is a scan and not an index because **the rooms are the truth about where a player sits**
(`ADR-0002`), and a scan cannot disagree with them. The costs are named in Consequences, and the
reversal is one method body: an index later changes `heldRoom`'s implementation and no call site,
which is why the method — not the expression — is the seam.

### 3. Opening a room is find-or-create, and it is serialised per player

`RoomRegistry` gains one public, **suspending** operation:

> `public suspend fun heldOrOpen(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room` —
> the room `host` already holds, or a freshly opened one.

Its body is `heldRoom(host) ?: create(host, format)`, executed under a **per-player lock**: a fixed
array of `Mutex`, striped by `PlayerId.hashCode()`, allocated once and **never pruned** — the shape
`SeatOwnership` already uses in `DuelSocket.kt`, for the reason its own comment gives, that removing
a lock another coroutine is about to acquire hands out a second lock for the same player and reopens
the race the lock exists to close. It is a **second array, not a shared one**: making seat adoption
and room opening contend on one stripe would serialise two unrelated operations and would couple
`RoomRegistry` to `DuelSocket`'s internals.

Without it, find-then-create is check-then-act. `ADR-0018` gives a player one live socket, but
eviction is asynchronous — an evicted connection notices only when it returns to its `select` — so
an older socket can be inside `replyToCreateRoom` while a newer one enters it. Both would scan
empty and both would call `create`. The stripe is what makes `ADR-0124` §1's *"a player holds at
most one waiting room"* **true rather than usually true**, and it is enough to make it true: a
`WAITING` seat has exactly one source (`Room.open` is its only constructor, `create` its only
production call site, and no transition returns a room to `WAITING`), so serialising `create` per
player closes that invariant completely. The remaining cross-race — a `CreateRoom` and a `JoinRoom`
from one player at once — cannot produce two `WAITING` rooms, and what it can produce is
`ADR-0105` §1's and `DEC-145`'s, not this decision's.

`create` keeps its present contract and its present signature — *open a fresh room* — and gains the
same KDoc note `finish` already carries: **production code reaches it only through `heldOrOpen`.**
That is the tree's own convention for this, and its weakness is named in Consequences.

### 4. The lock order, and what keeps it true

Three statements, in the order a reader needs them:

- **`heldRoom` takes no lock.** An operation that acquires nothing cannot appear in a wait-for
  cycle. This is the property that makes a scan safe next to `ADR-0016`, and it is the property that
  must survive every future edit: the moment the scan takes a room's mutex, a caller holding room
  A's mutex and scanning for room B has an A→B edge, and another caller can supply B→A.
- **`heldOrOpen` acquires exactly one lock — the player's stripe — and holds it across no suspending
  call.** `heldRoom` and `create` are both non-suspending, and `create` stores with `putIfAbsent`
  and takes no room mutex. The critical section therefore contains no suspension point, so it cannot
  wait on anything.
- **The ordering rule, stated for the code that has not been written yet: a player stripe is always
  outermost.** A room's mutex may be taken while a player stripe is held; a player stripe may
  **never** be taken while a room's mutex is held. `DEC-145`, when it is answered, is the change
  most likely to want a stripe on the join path, and this is the rule it must land under.

The third rule is **structurally enforced today rather than merely written down**, and that is worth
knowing: `mutate`'s `absent` and `block` parameters are plain function types, not `suspend` ones,
and `Mutex.withLock` is suspending — so nothing executed inside a room's critical section can
acquire any mutex at all. `reap`'s own `withLock` body is likewise non-suspending, and `act` calls
`DuelResultSink.record` **outside** the lock by `ADR-0016`'s rule. Making either block `suspend` is
the single change that would reopen this, and it is the tripwire a reviewer should watch for.

### 5. What `CreateRoom` answers, in each state

`replyToCreateRoom` calls `heldOrOpen(session.player.id)` in place of `create`, and branches on the
returned room's state.

| The player holds | The answer |
| --- | --- |
| nothing, or only `FINISHED` / `ABANDONED` rooms | a freshly opened room, `RoomJoined(code, seat)` — today's behaviour, unchanged |
| a `WAITING` room | `RoomJoined(code, seat)` for **that** room. No room opened, nothing written, nothing else sent (`ADR-0124` §§1, 3) |
| a `PLAYING` room | §8's sequence: the frames a resume sends, then the refusal (`ADR-0105` §§1, 3, 4) |

Two details are fixed here because an implementer will otherwise get them wrong.

**The seat is `Room.seatOf(player)`, read off the very `Room` value `heldOrOpen` returned — never
the literal `0`.** Today's line sends `0` and justifies it: `Room.open` always seats the host there.
That stays true of a *freshly opened* room, and `ADR-0124` §4 confirms it is true of a returned
`WAITING` one — but it is not true of a `PLAYING` room, where the holder may be the guest, and a
constant that is right for the wrong reason is the shape that survives every test until it does not.
Because the value is read off a snapshot the registry has already committed to, `seatOf` cannot
return `null` there, and a `checkNotNull` naming that is the honest form. This is the opposite of
`replyToJoinRoom`'s `ALREADY_SEATED` branch, which **must** re-fetch and **must** handle `null`,
because `JoinResult.Refused` carries no room.

**`RoomMembership.code` is set to the room the answer names**, in every branch, before the frames go
out. The connection is now about that room, `ADR-0104` requires the frames to reach it there, and a
later `Act` is routed by that record. This is also what disarms `ADR-0118`'s window: the press
inside the recovery gap still repoints the membership, but it repoints it at the room the player
already holds instead of at a new one, so it can no longer orphan a live seat.

### 6. What `JoinRoom` answers, and the one comparison that keeps a reconnect working

`ADR-0105` §1 refuses `JoinRoom` as well as `CreateRoom`, so `replyToJoinRoom` gains the same guard —
with one comparison, and everything turns on it:

> The guard fires only when the held room is `PLAYING` **and its code is not the code that was asked
> for**.

A player sending `JoinRoom` for the code of the room they are already in is a **reconnect**, and it
is how this product returns every dropped player to their table. Refusing it would break the
`resume` path, `ADR-0013`'s grace window and `ADR-0072`'s remembered code in one line. The guard
sits **before** the `resume` call and skips itself when the codes match; the existing `resume` and
`ALREADY_SEATED` branches then run exactly as they do today.

A holder of a `WAITING` room asking to join **another** room is not touched by this decision at all —
that is `DEC-145`, the product owner's, and until it is answered `JoinRoom` behaves exactly as it
does at `develop`.

### 7. The refusal's carrier: one new `ProtocolError` value, and it does cost a version step

`ADR-0124` §6 predicted no new frame **for its own half**, and this decision agrees: the `WAITING`
hand-back sends `RoomJoined(code, seat)` and nothing else, moves no wire, and spends no version.

**The `PLAYING` half is different, and it spends one.** `ProtocolError` gains one value —
`ALREADY_IN_DUEL` — carried by the existing `ServerMessage.Failure`. The client maps it to
`ADR-0105` §4's two sentences and to nothing else.

- **It is a wire change and a `PROTOCOL_VERSION` step, with no way around it.** `ProtocolError` is a
  generated declaration (`ProtocolTypeScript.kt`'s ENUM branch; `protocol.gen.ts` line 221 lists
  every value), so a new value changes the text `protocolDeclarations()` returns, which changes
  `ADR-0047` §2's fingerprint, which fails `ADR-0047` §3's rule 4 unless the ledger gains a row and
  `PROTOCOL_VERSION` moves to the next number free when it lands.
- **No existing value can carry it, and this is not a matter of taste.** Every value that could be
  reached for — `ROOM_FULL`, `UNKNOWN_ROOM` — asserts something about the room that was **asked
  for**. On the `JoinRoom` path that assertion would be false and would be an oracle pointing the
  wrong way: a player refused for their own live seat would be told a stranger's live room is full.
  `ADR-0022`'s no-oracle rule and `ADR-0105` §4's table both forbid it outright.
- **A `ServerMessage` subtype is not bought.** It costs the same version step plus a declaration and
  a reducer branch, and every fact it would carry is already carried: `RoomJoined` names the room
  and the seat, the resume frames carry the duel, `Failure` carries the reason.
- **The budget is untouched.** `ALREADY_IN_DUEL` is not a failed join and must not spend
  `ADR-0022` §2's ten-per-minute allowance when that allowance is built — `ADR-0105` §6's
  constraint, restated here as a constraint on the code that builds it. `JoinLimits` and
  `TOO_MANY_ATTEMPTS` still do not exist at `develop`.

### 8. The frames, in order: the table first and the notice last

A `CreateRoom`, or a `JoinRoom` for a different code, from a player holding a `PLAYING` room is
answered with exactly what a reconnect to that room is answered with, followed by the refusal:

1. `RoomJoined(heldCode, seat)`;
2. the frames `RoomRegistry.resume(heldCode, player)` produces, delivered as `replyToJoinRoom`'s
   resume branch delivers them;
3. `ServerMessage.Failure(ALREADY_IN_DUEL)`, **last**.

**The order is load-bearing, on `ADR-0044` §5's precedent in the same function.** A refusal that
arrived before `RoomJoined` would reach a client that does not yet know which room it is about, and
a reducer that treats a `Failure` as somewhere to go would take the table away from the player it is
explaining the table to — which `ADR-0105` §4 forbids by name. Arriving last, it decorates a table
that is already on screen.

**There is one way this product returns a player to their duel, and it is `resume`.** The refusal
does not invent a second: reusing it is what makes `ADR-0105` §6's *"a tab that has never heard of
that room must still be able to show it"* true, because `resume` reads the seat's whole entitled
view out of the projection layer rather than assuming the client holds anything.

**`resume`'s write-back is not a violation of *nothing moves*, and the check is worth writing down.**
It calls `Room.reconnect(seat).touch(now)`. Neither clause vacates a seat, abandons a room, forfeits
a duel or settles a coin — `ADR-0105` §6's four — and the room that must be *left exactly as it was
found* is the **refused** one, which is never reached at all. In the ordinary case the player was
never away (their socket never closed, so `disconnect` never fired) and `reconnect` is a no-op; in
the case where they did drop, clearing the away mark is **true**, and it is the opposite of the harm
`ADR-0105` exists to prevent. `touch` is likewise harmless: `isReapable` returns `false` for a
`PLAYING` room however idle.

**The `WAITING` hand-back writes nothing**, which `ADR-0124` §4 requires — the ten minutes runs from
`Room.open` and a return does not restart it. It gets that for free rather than by a special case:
`resume` answers `null` for a `WAITING` room, and the hand-back never calls it.

### 9. How this splits, and what each ticket may not do

Two tickets, because the two halves have different costs and only one of them is blocking.

| | `STORY-1416` — the `WAITING` hand-back | the ticket implementing `ADR-0105` §1 |
| --- | --- | --- |
| Builds | `heldRoom`, the stripes, `heldOrOpen`, `replyToCreateRoom`'s `WAITING` branch | the `PLAYING` branches of both replies, `ALREADY_IN_DUEL`, the client's mapping |
| Wire | none | one `ProtocolError` value |
| `PROTOCOL_VERSION` | **not moved** | **moved** — ledger row, regenerated `protocol.gen.ts` |
| `atomic:` | **no** | **yes**, under `ADR-0047` and `ADR-0068`, sized by `ADR-0070`'s probe |
| Client | **no change** | one error mapping and `ADR-0105` §4's two sentences |

**`STORY-1416` must leave the `PLAYING` case falling through to `create`, exactly as today.** The
lookup it builds will find a `PLAYING` room, and its `CreateRoom` branch must ignore it: handing
that room back silently would ship `ADR-0105` §1's refusal without `ADR-0105` §4's words, which is
the one thing the product owner fixed and the coder may not quietly undo. `ADR-0105` §1 stays
unimplemented until its own ticket lands — as it has been since 2026-09-01.

The planner also owns one scheduling fact this decision does not: `ADR-0047`'s *at most one bumping
branch open at a time*. `EPIC-14` records the lock as free after `ADR-0126` refused item 2b's wire
move; if another bump is in flight when the `PLAYING` ticket is cut, it serialises behind it under
`ADR-0045` §3.

### 10. What this deliberately does not decide

- **When the scan should become an index.** Nobody is working it, there is no measurement, and §2
  makes the swap a one-body change. Named as a cost below, deliberately unregistered on
  `ADR-0105` §6's rule.
- **How long `ADR-0105` §4's notice stands on the client once it has arrived, and whether anything
  dismisses it.** `ADR-0105` §4 fixed the words and left the treatment to `EPIC-06` and the
  implementing ticket; nothing here changes that, and this decision fixes only the frame that
  carries it and the order it arrives in.
- **Anything about a holder of a `WAITING` room following someone else's invite** — `DEC-145`, the
  product owner's, unchanged and untouched.
- **Whether a host may close a room or ask for a fresh code** — `ADR-0073`'s open item, which
  `ADR-0124` made matter more and neither of us answers.

## Consequences

**What it buys.**

- **One lookup answers both live states**, which is what `DEC-110` was widened to ask for. The
  `PLAYING` ticket adds a branch and a carrier, not a design.
- **It works on a device that has never heard of the room** — the whole point of finding the room
  from the player. `ADR-0105`'s Context named that route in and nothing had answered it; a scan of
  the rooms answers it without the connection being consulted at all.
- **`ADR-0124` §1's invariant becomes true rather than nearly true.** With `create` serialised per
  player and `create` the only source of a `WAITING` seat, there is no interleaving that leaves one
  player holding two waiting rooms.
- **`ADR-0118`'s window is disarmed at the source it named.** A *Create a duel room* press inside
  the recovery gap can no longer orphan a live seat in either state, because the press now reads the
  player before it does anything.
- **The deadlock question is closed by construction, not by discipline.** The lookup takes no lock;
  the opener takes one lock across no suspension point; and nothing inside a room's critical section
  can take a lock at all, because `mutate`'s blocks are not `suspend`.
- **It is the cheapest working answer to reverse, and that is why it is the one chosen.** The
  evidence here is thin — there are no players, and the two-room state was met once, by one person,
  on one afternoon. The scan is one method body and the stripe is one array; deleting either
  restores today's behaviour, with nothing persisted to unwind. Every faster answer writes down a
  second copy of where players sit, and copies have to be repaired rather than removed.

**What it costs.**

- **A linear scan of every live room on the product's primary press.** It is O(live rooms) per
  `CreateRoom`, paid by a control a human presses. At this product's scale that is microseconds, and
  nothing measures it; the honest statement is that nothing will *tell* us when it stops being free,
  and the trigger to revisit it is a room count nobody is currently counting.
- **`RoomRegistry`'s concurrency story stops being one sentence.** *A room is serialised by its own
  mutex* becomes that plus *a player is serialised by a stripe, outermost*. One more rule for every
  future contributor, in a class where getting the rule wrong hangs the server.
- **The guard on `create` is a convention, not a fence.** `create` stays public and reachable, held
  only by a KDoc note in `finish`'s style. A future call site can open a second room for a seated
  player and nothing fails.
- **The answer can name a room the reaper removes a moment later.** The frame is sent outside every
  lock and must be (`ADR-0016`), so no amount of locking closes that window. The state it produces
  is the one a host already sits in when their ten minutes expire under them (`ADR-0072` §6) — but
  it is now reachable from a fresh press on a device that never heard of the room, which it was not
  before.
- **The `PLAYING` half costs a `PROTOCOL_VERSION` step**, an `atomic:` ticket, a ledger row, a
  regenerated `protocol.gen.ts` and `ADR-0047`'s lock for as long as its branch is open. This
  contradicts nothing `ADR-0124` said — it scoped its prediction to its own half — but it is the
  half of `DEC-110` that is not free, and it should not be discovered at ticket-cutting time.
- **Between the two tickets, the merged tree contains a lookup one branch of which is deliberately
  ignored.** A reader of `replyToCreateRoom` will see a `PLAYING` room found and dropped, and the
  only thing explaining it is §9 of this file.
- **The client gains one more `ProtocolError` to map, and an unmapped one is silent.**
  `duel-state.ts` already special-cases `REMATCH_UNAVAILABLE`; this adds a second value whose
  absence from the reducer shows as nothing happening rather than as a failure.
- **Two operations now walk the whole room map.** `reap` already does, on the ticker; `heldRoom`
  does, on a press. Nothing coordinates them and nothing needs to today, but the class no longer has
  exactly one full traversal in it.

**What it forecloses.** It forecloses making a connection's `RoomMembership` authoritative for
anything: a second device is now a first-class case in the one place it was most tempting to ignore,
and `replyToAct`'s existing habit of re-deriving the seat from the registry becomes the house rule
rather than one function's caution. It forecloses answering the two live states by two different
mechanisms — after this there is one lookup, and a future state (a paused room, a spectator seat)
extends its definition of *held* rather than adding a second query. It forecloses nothing about
indexing, about `DEC-145`, or about closing a room.

## Alternatives considered

**Read the connection's `RoomMembership`.** Its strongest case is that the answer is already in the
caller's hand: `replyToCreateRoom` is handed a `RoomMembership`, `replyToAct` already trusts it to
route every action frame, it is O(1), and it needs no new registry API at all — the whole repair
would be three lines in one function. Rejected because a second device has no membership, and that
is not an edge case here: it is the case `ADR-0105`'s Context named — *"that device's `localStorage`
has never heard of the room, so it boots to the lobby, where the only thing to press is *Create a
duel room*"* — and the case `ADR-0124` §2 built its whole answer around, which is why `ADR-0124` §6
forbids this by name. It also fails on the tab that has been reloaded, since the membership is
per-socket and starts empty. `replyToAct`'s trust in it is not a precedent: it uses the membership
only to *name* a room and then re-derives the seat from the registry, precisely because the record
can be wrong.

**A `ConcurrentHashMap<PlayerId, RoomCode>` index maintained beside `rooms`.** Its strongest case is
strong, and it is the answer most systems reach for: O(1) instead of O(n) on the product's primary
control, no scan to justify, and a second derived map is not a new pattern in this class —
`recording` is one already. The writers are a small closed set (`create`, `join`, `finish`,
`abandon`, `reap`, and the rematch path back into `PLAYING`), all in one file, all reviewable in one
sitting. Rejected because it makes a **second source of truth about where a player sits**, and
`ADR-0002` already answers that question with the room. Its failure mode is not slowness but
wrongness, and asymmetric wrongness at that: a leaked entry means a player who can never open a room
again, or one handed a code for a room that no longer exists, and neither is visible until a person
reports it. This class documents that exact hazard about its own derived map — `recording`'s KDoc
names a permanent leak as a deliberate trade — which reads as a warning rather than a licence. And
the choice is not symmetric in time: a scan can become an index behind `heldRoom` on any afternoon,
with no call site touched, whereas removing an index the code has come to trust means proving
nothing else reads it.

**Store the held room on the session or the player, rather than in the registry.** Its strongest
case: it is O(1), it survives across a player's sockets rather than being per-connection, and a fact
about a player arguably belongs on the player. Rejected as the index alternative with a worse
address — it would put a room fact into `SessionRegistry` or the player directory, so `RoomRegistry`
would stop being the only thing that knows where seats are, and the same staleness failure would
then be spread across two classes with no single owner to fix it.

**Fold the lookup into `create`, so that `create` itself is find-or-create.** Its strongest case is
the best of the rejected set: no new public member, `replyToCreateRoom` unchanged, and the invariant
becomes unbreakable rather than conventional, because there would be no way in the codebase to open
a second room for a seated player. Rejected on two counts. `create`'s contract is *open a fresh
room*, and a method that sometimes opens nothing is the kind of surprise that makes a reader stop
trusting the class — the same objection that keeps `finish` honest about being a test-only
affordance rather than being quietly repurposed. And it does not cover the ground: `ADR-0105` §1
refuses `JoinRoom` too, and that path needs the **query alone**, with nothing created, so `heldRoom`
has to exist as a member either way.

**Answer the `PLAYING` refusal with frames that already exist, and let the client infer it.** Its
strongest case is real, and it is the option `DEC-110`'s own row held open: it costs no wire value,
no `PROTOCOL_VERSION` step, no ledger row and no `atomic:` ticket, and the inference is sound in the
only case it fires — the client knows it sent `CreateRoom`, so a `RoomJoined` for a room carrying a
running duel can only mean the create did not create. Rejected because it makes the one sentence the
product owner fixed into a client-side deduction from request/response correlation, which is
`ADR-0118`'s standard read backwards: *the client shows what the server told it*, and here the
server would have told it nothing. It also does not extend to `JoinRoom`, where the same refusal is
owed and the only signal is a `RoomJoined` naming a different code than the one asked for — a shape
that also fires for a legitimate resume, so the client would need a second, different rule for the
same string. Two inferences, two clients, one sentence, and no server test can pin any of it.

**Reuse `ROOM_FULL` or `UNKNOWN_ROOM` for the refusal.** Its strongest case: it ships today, costs
nothing, and `ROOM_FULL` is arguably even true — of the room the player is sitting in. Rejected
because on the `JoinRoom` path every existing value asserts something about the room that was
**asked for**, and that assertion would be false: a player refused for their own live seat would be
told that a stranger's perfectly joinable room is full or does not exist. That is `ADR-0022`'s
no-oracle rule broken in the worst direction — not withholding information, but emitting wrong
information about a third party's room — and `ADR-0105` §4's table forbids saying anything at all
about the code that was asked for.

**Send the refusal first and the room after it.** Its strongest case: a refusal is the answer to the
request, and answering the request first is the order every other branch of `replyTo` uses. Rejected
on `ADR-0044` §5's precedent, which lives in the same function and was decided for the same reason:
a frame that lands before the client knows which room it is about is a frame a reducer routes
wrongly, and here the wrong routing takes the table away from the player the notice exists to
explain the table to — the one treatment constraint `ADR-0105` §4 fixed.

**Serialise `heldOrOpen` on a single global lock instead of a striped per-player one.** Its
strongest case: one `Mutex` is far easier to reason about than sixty-four, the critical section is
microseconds long, and a global lock would make the *one waiting room* invariant hold against every
caller rather than only against callers for the same player. Rejected because it puts every player's
press behind one lock on the product's primary control, and because the extra invariant it would buy
is one nobody needs: a room can only be opened for its own host, so two different players' opens
have nothing to serialise. `SeatOwnership` reached the same conclusion for the same operation, and
its reasoning is reused here rather than re-derived.
