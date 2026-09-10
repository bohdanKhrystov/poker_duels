# ADR-0146 — The held room is abandoned after the seat, under the joiner's own stripe

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-154` — **by what call, at what point in `replyToJoinRoom`, and under what lock
  order is a held `WAITING` room released when its holder takes a seat in another room?** *By
  `Room.abandon` through `RoomRegistry.mutate`; at the one point `replyToJoinRoom` already calls
  `join`, because the release moves inside the registry rather than into the socket; with the
  joining player's own `stripeFor` stripe outermost and never two room mutexes held at once.*
  Registered 2026-09-08 by
  [`ADR-0141`](ADR-0141-taking-a-seat-elsewhere-releases-the-room-you-were-holding.md) §8, which
  fixed the product half and deliberately refused to choose the mechanism.
- **Implements `ADR-0141` and amends none of it.** §§1–7 are the specification this ADR builds
  against, and every observable it produces is one `ADR-0141` already fixed: the seat is taken
  (§1), the released code answers `UNKNOWN_ROOM` (§2), a `PLAYING` room is left as found (§3), a
  refused join changes nothing (§4), a holder who merely left keeps their room (§5), nothing is
  said (§6). §8's four obligations are met **verbatim, including the one about `lastActivityAt`** —
  see Decision §2.
- **Applies:** [`ADR-0133`](ADR-0133-the-room-a-player-holds-is-scanned-for.md) §4 — *"a player
  stripe is always outermost"* — which is the rule this lands under, and which §4 itself named this
  decision as the likely first customer of: *"`DEC-145`, when it is answered, is the change most
  likely to want a stripe on the join path, and this is the rule it must land under."* The answer
  is **yes, a stripe is taken**, and §4's guarantee survives unchanged because nothing new runs
  inside a room's critical section. [`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md)
  — a room is serialised by its own mutex, and this adds no actor, no channel and no second lock
  per room. [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) §3 — what a dead code answers.
- **Changes no wire type, no string, no client file, no `PROTOCOL_VERSION` step (`ADR-0047`), no
  `docs/protocol.md` section and nothing stored.** `ADR-0141` §8 fixed that, and this mechanism was
  chosen partly because it keeps it true: the refusal a released code produces is one `Room.join`
  already returns.
- **Amends one KDoc paragraph and nothing else in shipped behaviour:** `RoomRegistry.heldOrOpen`'s
  second stated property — *"this method acquires exactly one lock, the player's stripe, across
  nothing that can wait"* — is a true statement about `heldOrOpen` that reads as a statement about
  the stripes. It becomes false of the stripes the moment a second holder exists, so the sentence
  moves into `heldOrOpen`'s own scope. Decision §5.
- **Registers no decision.** The one residual — that a player can still hold a `WAITING` room *and*
  a `PLAYING` one at the same time — is `ADR-0105` §1's, already decided and not yet built, so
  registering it would put a second owner on an answered question. Named in Consequences instead,
  on `ADR-0105` §6's rule that nothing is registered unless somebody is working it.
- **Unblocks:** the ticket that implements `ADR-0141`, which has never been written. This ADR is
  what makes it writable; Decision §7 is the list its `Tests` rows come from.

## Context

`ADR-0141` decided *that* the room is released and refused to decide *how*, because the how is a
lock question. The forces are three, and they genuinely pull apart.

**A second room's mutex cannot be taken where the release most obviously belongs.**
`RoomRegistry.mutate` runs its block under the joined room's mutex, and its `block` parameter is
`(Room) -> Pair<Room?, T>` — a plain function type, not a `suspend` one. Nothing inside it can
acquire a `Mutex` at all. That is not an oversight to route around: `ADR-0133` §4 rests its
deadlock argument on exactly this, in `heldOrOpen`'s own KDoc — *"nothing executed inside a room's
critical section can acquire any mutex at all, this stripe included."* So the release sits outside
the join's critical section, and the only question left is which side and under what.

**Both sides have a price, and `ADR-0141` priced them itself.** Before the join, and a join that is
then refused has already destroyed a room, which §4 forbids in as many words. After the join, and a
window opens in which both rooms are live, which §3 bounds — a rival who wins the race gets their
duel — but does not close. The window is not eliminable; what is choosable is its width and what
runs inside it.

**The room to release is not the one `heldRoom` answers with.** This is the trap, and it is
measurable rather than arguable. `HELD_ROOM_ORDER` is
`compareBy<Room> { it.state != RoomState.PLAYING }.thenBy { it.lastActivityAt }.thenBy { it.code.value }`,
so `PLAYING` sorts **first**. By the time the release runs, the room just joined is `PLAYING` and
`heldRoom(player)` answers with **it**, not with the room to release. A mechanism written the
obvious way — *find the held room, abandon it* — abandons the duel the player just sat down in.

Two further facts, read off `develop` rather than assumed, shape the answer.

**A player can hold more than one `WAITING` room today.** `replyToCreateRoom` asks `heldOrOpen`,
takes its answer only when it is `WAITING`, and otherwise falls through to `create` — `develop`'s
own behaviour, kept deliberately until `ADR-0105` §1 ships. So a player in a `PLAYING` duel who
presses *create* twice gets **two** fresh `WAITING` rooms: `heldRoom` answers with the `PLAYING`
room both times, and both presses fall through. The release therefore ranges over a set, not over
a room, or `ADR-0141` §7's closing sentence — *"a player who has released a room holds exactly one
live seat afterwards — the new one"* — is false on a reachable path.

**The registry already contains the release, in two pieces.** `Room.join` answers
`RoomRefusal.UNKNOWN_ROOM` for a `FINISHED` or `ABANDONED` room *"before anything else, so it is
indistinguishable from a room that never existed"* — its own KDoc, which is `ADR-0141` §2's
observable stated as shipped, tested code. And `heldRoom` already excludes `ABANDONED` rooms *"by
this method, not by its callers"* (`ADR-0133` §1). A release that lands a room in `ABANDONED`
inherits both properties without writing either.

### The deadline

There is one, and it is not urgency. `ADR-0141` blocks a ticket that has never been written, so
nothing ships until this is answered — but the reason to answer it *today* rather than at the
ticket is that `ADR-0133` §4's lock-order rule is currently a property of the code, not a
convention: it holds because `mutate`'s block is non-suspending and because exactly one method
takes a stripe. The first second caller is where a rule either gets written down or gets broken by
someone who never read it. That is now.

## Decision

### 1. The release is a state transition through `mutate`, not a removal from the map

The held room is released by applying **`Room.abandon`** to it inside a **`RoomRegistry.mutate`**
critical section. It is not removed from `rooms`.

Four things follow without being written:

- **The code answers `UNKNOWN_ROOM`.** `Room.join`'s first `when` branch is
  `RoomState.FINISHED, RoomState.ABANDONED -> JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM)`, and
  `replyToJoinRoom` already maps that refusal to `ProtocolError.UNKNOWN_ROOM`. `ADR-0141` §2's
  observable — and `ADR-0022` §3's no-oracle property with it — is met by a path that already
  ships and is already tested. **No new refusal, no new branch, no new `ProtocolError` value.**
- **The player provably stops holding it.** `heldRoom` filters to `WAITING` and `PLAYING`, so an
  abandoned room drops out of the scan with `heldRoom` byte-unchanged. A later *create* opens a
  fresh room, which is `ADR-0105` §2's rule, unamended.
- **A resume can never find it.** `RoomRegistry.resume`'s `when` already answers
  `RoomState.WAITING, RoomState.ABANDONED -> Pair(null, null)`.
- **The reaper already collects it**, on `isReapable`'s `FINISHED, ABANDONED` arm.

`RoomState.ABANDONED`'s documented invariant is *"carries no further invariant beyond seating"* —
the one state a `WAITING` room can enter without asserting anything about a duel that never
started.

**Why not remove the entry.** `reap` removes rooms with `rooms.remove(code, holder)` under a
hand-rolled critical section, and its KDoc says why that was kept out of `mutate`: *"[mutate]
always writes a room back, and reaping has no room to write back… Folding that removal into
[mutate]'s contract would mean teaching one shared critical section two different shapes of
'done'."* A release built on removal would be the **third** critical section in the class, written
by hand, on the product's core invite path. The transition costs one `mutate` call.

### 2. The room's own clock is not written, and this is what honours `ADR-0141` §8

`Room.abandon(now)` sets `lastActivityAt = now`. `ADR-0141` §8 forbids exactly that: *"no
`lastActivityAt` is written anywhere but in the room being joined."* The transition is therefore
applied as **`room.abandon(room.lastActivityAt)`** — the room hands its own stamp back, so `state`
moves and the clock does not. The call site carries a `why` comment saying so and naming §8.

This is not a dodge of the clause; it is what the clause buys. Because `DEFAULT_FINISHED_MILLIS`
(5 min) is less than `DEFAULT_WAITING_MILLIS` (10 min), a released room becomes reapable **strictly
no later** than the same room would have been reaped as `WAITING`, and usually sooner — a room
already idle past `finishedMillis` is collected on the very next sweep. Releasing a room shortens
its residency in memory; it never extends it.

That property depends on `finishedMillis ≤ waitingMillis`, which `RoomTimeouts.init` does not
require and nothing else asserts. It is pinned by a test on the constants (§7), because a property
this decision leans on and no gate holds is a comment.

### 3. The rooms are found by their own scan, and `heldRoom` is not touched

A new private, non-suspending scan:

```kotlin
private fun waitingRoomsHeldBy(player: PlayerId, except: RoomCode): List<RoomCode>
```

built from the same lock-free iteration `heldRoom` uses — `rooms.values`, `Holder.room` through
the existing `@Volatile` field, no mutex — filtered to rooms that are `WAITING`, whose code is not
`except`, and that seat `player`; ordered by `RoomCode.value` so the answer is total rather than
whatever the map iterates first, on `heldRoom`'s own stated reason.

Three deliberate choices in that signature:

- **Not `heldRoom`.** Context said why: `heldRoom` answers with the room just joined, because
  `PLAYING` sorts first. `heldRoom` keeps its contract, its order and its tests, and gains no
  parameter — `ADR-0133` is applied here, not amended.
- **`except`, not "the room I just joined is `PLAYING` so the `WAITING` filter excludes it".** That
  reasoning is true and fragile: it would silently become wrong the day a join could leave the
  joined room `WAITING`. The exclusion is stated.
- **Codes, not rooms.** A `Room` returned from a lock-free scan is a snapshot no mutex has
  confirmed, and handing one to a caller invites it to act on a state that has since moved. The
  code is all the next step needs, and the state is re-read under the lock.

### 4. The lock order: the player's stripe outermost, one room mutex at a time

`RoomRegistry` gains **one** public method, and it is the whole of the mechanism:

```kotlin
public suspend fun joinReleasingHeld(code: RoomCode, player: PlayerId): JoinResult =
    stripeFor(player).withLock {
        val result = join(code, player)
        if (result is JoinResult.Seated) {
            for (held in waitingRoomsHeldBy(player, except = code)) release(held, player)
        }
        result
    }
```

The order, stated as the rule a future reader has to keep:

1. **`stripeFor(player)` is taken first and released last.** `ADR-0133` §4's *"a player stripe is
   always outermost"* is honoured by taking it before any room mutex on this path and never inside
   one.
2. **Room mutexes are taken one at a time and never nested.** `join`'s `mutate` releases the joined
   room's mutex before `release` takes the held room's; each `release` releases before the next
   begins. **At no instant does this path hold two room mutexes**, so no ordering rule between
   rooms is introduced and none is needed.
3. **Nothing new runs inside a room's critical section.** `mutate`'s `block` stays a plain function
   type; `release`'s block acquires nothing. `ADR-0133` §4's structural guarantee is untouched, so
   the wait-for graph stays what §4 proved it to be — stripe → room mutex, and nothing the other
   way.

Measured, so the claim is checkable: `DuelSocket`'s `SeatOwnership` stripes are the only other
`Mutex` array in the server, `adopt` is their only user, and it touches no `RoomRegistry` method
inside its lock. There is no third participant to order against.

**Why the stripe is taken at all**, since `ADR-0133` §4 asked and the answer could have been *no*.
The release is a read-modify-write **over a player**: it reads *which rooms does this player hold*
and writes to them. `heldOrOpen` is the same shape over the same question, and the stripe is
precisely what makes acting on a lock-free scan safe there. Without it the two race, and the race
has a visible outcome: a *create* pressed as the join lands can be handed a room the release is
already ending, and the player is sent `RoomJoined` naming a code that answers `UNKNOWN_ROOM` a
moment later. With it, the two serialise into one of two clean orders — the create is answered and
then released, or the create finds nothing and opens a fresh room. The scan stays lock-free and
`heldRoom` stays `fun`.

`release` itself is private, and its guard is inside the critical section that writes:

```kotlin
private suspend fun release(code: RoomCode, player: PlayerId): Boolean =
    mutate(
        code,
        absent = { false },
        block = { room ->
            if (room.state == RoomState.WAITING && room.seatOf(player) != null) {
                Pair(room.abandon(room.lastActivityAt), true)
            } else {
                Pair(null, false)
            }
        },
    )
```

That is `ADR-0141` §8's *"conditions on a room's state at the moment of the write, not on what it
was when the join began"* made structural rather than remembered. A rival who wins the race leaves
the room `PLAYING`, the guard declines, and §3's outcome stands. A room reaped between the scan and
the lock takes `mutate`'s `absent` branch. The existing public `abandon(code)` is **not** reused:
it is unconditional, and a `PLAYING` room reaching it would end a duel — the one thing `ADR-0141`
§3 forbids outright.

### 5. `heldOrOpen`'s second property moves into `heldOrOpen`'s own scope

`heldOrOpen`'s KDoc states three properties its deadlock-freedom rests on. The second —
*"this method acquires exactly one lock, the player's stripe, across nothing that can wait"* — is
true of `heldOrOpen` and reads as a property of the stripes. It is not one: `joinReleasingHeld`
holds a stripe across `join`, which suspends on a room's mutex.

The paragraph is amended to say the property is `heldOrOpen`'s own, and to state what actually
carries the guarantee for both callers: **the outermost rule plus the non-suspending block**, which
is the third bullet and needs no change. Nothing about `heldOrOpen`'s behaviour moves; one sentence
stops over-claiming.

### 6. `replyToJoinRoom` changes by exactly one identifier

```
deps.rooms.join(parsed, session.player.id)  →  deps.rooms.joinReleasingHeld(parsed, session.player.id)
```

No new branch, no second call, no second write, no new failure mode, and no question about where
the release sits relative to the frames — it has already happened when the call returns. The `when`
over `JoinResult` is byte-unchanged, and so is every other branch of the function.

**The `resume` branch above it is not touched.** A resumption enters a room the player is already
seated in; no new seat is taken, so `ADR-0141` §4's *"the release follows sitting down, never
trying to"* and §7's second row — *their own code … nothing released* — both say the same thing:
nothing is released on that path. The `ALREADY_SEATED` branch below is untouched for the same
reason.

`ADR-0141`'s Consequences priced *"`replyToJoinRoom` grows a write on its success path… The price
is the architect's under `DEC-154`."* The price paid is that the write does **not** land in
`replyToJoinRoom`. It lands in the class that owns the locks, because `DuelSocket` cannot take a
`RoomRegistry` stripe — `playerStripes` is private, and exposing it would move `ADR-0133` §4's rule
outside the only class that can enforce it. The method is named `joinReleasingHeld` rather than
`join` so that the second effect is visible at the call site without opening the registry.

### 7. What a ticket must prove

The planner's `Tests` rows come from here. Every row names the `ADR-0141` clause it defends.

| # | What it proves | Source |
| --- | --- | --- |
| 1 | `A` holds `X` (`WAITING`), joins `Y` → seated in `Y`, and a later `JoinRoom(X)` from a **third** player answers `UNKNOWN_ROOM` | §§1, 2 |
| 2 | The frames the joiner receives are **byte-identical** to today's for the same join — no frame about `X` reaches anybody, including `A` | §6, §8 |
| 3 | A rival already seated in `X` (so `X` is `PLAYING`) → `X` untouched, its duel intact, its runner and deadline unchanged | §3 |
| 4 | Each refusal leaves `X` `WAITING` and still joinable: unparseable code, `UNKNOWN_ROOM`, `ROOM_FULL` — three cases, not one | §4 |
| 5 | `A` joining **their own** `X` (`ALREADY_SEATED`) releases nothing; `A` resuming a `PLAYING` room releases nothing | §7 rows 2, 5 |
| 6 | `heldRoom(A)` after the release answers `Y` and never `X` — the guard against the `PLAYING`-sorts-first trap | Decision §3 |
| 7 | `A` holding **two** `WAITING` rooms has **both** released, and the joined room is not among them | §7's closing sentence; Decision §3's `except` |
| 8 | The released room's `lastActivityAt` is unchanged, and its `state` is `ABANDONED` | §8; Decision §2 |
| 9 | `DEFAULT_FINISHED_MILLIS <= DEFAULT_WAITING_MILLIS` — the constants a released room's shortened residency rests on | Decision §2 |
| 10 | A rival's `JoinRoom(X)` racing the release ends in exactly one of two states — `X` `PLAYING` with the rival seated and a duel running, or `X` `ABANDONED` and the rival refused — and never a room that was seated and then abandoned | §3; Decision §4 |

Rows 1, 3, 4 and 7 need **two distinct room codes and at least two distinct players**; a test that
reuses one code or one player cannot tell `except` from a constant, and cannot tell *release the
room I hold* from *release every room*.

### 8. What this deliberately does not decide

- **Whether `join` itself should release.** It should not, and `join` is unchanged: it keeps its
  contract, its callers and its tests, and `joinReleasingHeld` wraps it. Deleting the wrapper and
  pointing `replyToJoinRoom` back at `join` restores today's behaviour exactly — which is the
  reversibility `ADR-0141` claimed for its own footprint, kept.
- **Anything `ADR-0141` §9 left standing** — the ghost duel by every other route, closing a room on
  purpose, the room's lifetime, what a player is told.
- **Whether `replyToCreateRoom`'s `PLAYING` fall-through should go.** It should, and `ADR-0105` §1
  already says so; this ADR only records that until it does, a player can hold a `PLAYING` room and
  a `WAITING` one at once, and sizes the mechanism for it.

## Consequences

**What it buys.**

- **`ADR-0141` §2's observable is inherited, not implemented.** The released code answers
  `UNKNOWN_ROOM` through `Room.join`'s existing first branch. No refusal path, no `ProtocolError`
  value, no `when` arm and no client file is added anywhere, so `ADR-0022` §3's no-oracle property
  needs no re-argument.
- **`ADR-0133` §4's guarantee survives its first second caller intact.** The stripe question §4
  raised is answered *yes* with the rule it named, and the property that makes the order acyclic —
  a non-suspending block inside a room's mutex — is not weakened by a single line.
- **The window is as narrow as the constraint allows, and contains no I/O.** Between the joined
  room's mutex being released and the held room's being taken there is one mutex acquisition and
  one field write. Any design that releases from `replyToJoinRoom` puts `deliver` — two sockets'
  worth of network writes — inside that window instead.
- **A released room costs less memory than the room it replaces**, because `ABANDONED` reaps on
  five minutes and `WAITING` on ten.
- **The trap is caught by construction.** `except` and the `WAITING` filter both have to be wrong
  before the joined room can be abandoned, and row 6 of §7 fails if either is.

**What it costs.**

- **The joiner's first frame now waits on a stranger's lock.** `RoomJoined` and the opening hand
  are sent only after every held room's mutex has been acquired and released. If a rival is
  joining the room being released at that instant, the joiner's table is delayed by that rival's
  entire `join` — a duel start, `withFreshRunner` and all. It is bounded and it is short, and it is
  still the case that one player's table now waits on an operation belonging to somebody they have
  never met. **This is the cost.** Sending the frames first and releasing afterwards removes it and
  widens the window to include two socket writes; that trade was made deliberately and could be
  remade, since it is one statement's position in one method.
- **A player stripe is now held across suspension.** `heldOrOpen`'s critical section could be read
  at a glance as taking one uncontended lock; `joinReleasingHeld`'s spans a join and up to two
  releases. Two unrelated players colliding on one of 64 stripes serialise for that whole span
  instead of for a map lookup, and the KDoc sentence that made the old reading available has to be
  narrowed (Decision §5) rather than deleted, because it is still true of the method it is on.
- **`RoomState.ABANDONED` stops meaning what its neighbours mean.** Every other producer of it
  abandons a room a duel ran in; this one abandons a room nobody ever sat in but the host. Nothing
  in the tree reads `ABANDONED` and assumes a duel — measured, the only code that branches on it is
  `Room.join`, `Room.abandon`, `RoomRegistry.resume` and `isReapable`, and `heldRoom` excludes it by
  filtering to `WAITING` and `PLAYING` — but the next thing that does will be wrong, and will be
  wrong silently.
- **`room.abandon(room.lastActivityAt)` passes a value into a parameter documented as `now`.** It
  is the cheapest way to honour §8 verbatim and it will read as a bug to anyone who meets it
  without the comment. The comment is required, and a comment is a weaker guard than a signature.
- **The window is narrowed, not closed, and this ADR does not measure it either.** `ADR-0141`
  named that cost and it survives the mechanism: §3's outcome — the rival gets their duel, the
  holder is in two — is still reachable, now on a race of one lock acquisition rather than one
  registry operation.
- **A `WAITING` room and a `PLAYING` room can still be held at once, so the harm is not fully
  closed.** A player already in a duel who presses *create* still opens a live `WAITING` room whose
  code can settle a coin for a duel they never see. That is `replyToCreateRoom`'s `PLAYING`
  fall-through, owned by `ADR-0105` §1, decided and unbuilt. **Nothing fails if that ticket is
  never written** — which is exactly why it is named here rather than left to be rediscovered.
- **`RoomRegistry` grows a method that touches two rooms.** Every method on the class until now
  operated on one room named by one code. `joinReleasingHeld` is the first that does not, and the
  lock-order rule it lands under is now load-bearing prose in a KDoc rather than a property of the
  types. A third caller of `stripeFor` is where that stops being enough.

**What it forecloses.** Releasing from `DuelSocket`, and with it any future release that wants to
sit between the seat and the frames — the ordering is now the registry's and a caller cannot
observe the two halves separately. It forecloses nothing about `join`, `heldRoom`, `mutate`,
`reap`, `abandon` or `Room`, all of which are byte-unchanged.

## Alternatives considered

**Release from `replyToJoinRoom`'s `JoinResult.Seated` branch, after `deliver`.** Its strongest
case, and the reading `DEC-154`'s own wording invites: the socket already knows the join seated
somebody, `RoomRegistry.join` keeps one job, the release is visible in the function every reader of
this behaviour will open first, and the joiner's table is delayed by nothing at all — the frames go
out before any second lock is touched. Rejected on two counts, the second decisive. It cannot take
a stripe: `playerStripes` is private to `RoomRegistry`, and publishing it would put `ADR-0133` §4's
*"always outermost"* rule in a file that cannot enforce it, one edit away from someone taking a
stripe inside a room's mutex. And it widens the window to contain `deliver` — two WebSocket writes,
network-bound, unbounded by any lock — so the interval in which both rooms are live stops being a
property of the registry and becomes a property of the slowest client on the table.

**Remove the entry from `rooms`, as `reap` does.** Its strongest case is real and it was the
expected answer: removal is what *released* means, the room's memory goes back immediately rather
than in five minutes, `get` and `join` both answer nothing with no dependence on `Room.join`'s
state table, and `reap` proves the idiom works — `rooms.remove(code, holder)` under `holder.mutex`
succeeds only while the map still holds the exact `Holder` the call locked. Rejected because it
buys nothing the transition does not, and costs a critical section. `reap`'s KDoc already explains
why removal sits outside `mutate`: folding it in *"would mean teaching one shared critical section
two different shapes of 'done'."* A release built on removal is therefore a third hand-rolled
lock-and-check, written on the invite path — the most-travelled path in the product — to reach a
state `Room.abandon` reaches through the shared one. The memory difference is five minutes of one
`Room` object, and Decision §2 makes it shorter than the room's own remaining `WAITING` life
anyway.

**Take no stripe: scan and release after the join, unsynchronised.** Its strongest case: the scan
is already lock-free by design, `ADR-0133` §2 made it so deliberately, the guard inside `release`'s
critical section makes every individual write safe, and the fewest locks is usually the right
answer in a class whose whole discipline is one mutex per room. Genuinely close. Rejected because
the release is a read-modify-write over a *player*, and the product already has one of those —
`heldOrOpen` — whose entire reason for taking a stripe is that a lock-free scan is not safe to
*act* on. Leaving this one unsynchronised means a *create* racing a join can be handed a room that
is already being ended, and answer `RoomJoined` with a code that answers `UNKNOWN_ROOM` a
millisecond later. `ADR-0133` §4 predicted this call site by name as the one most likely to want a
stripe; the cheap answer would have been to disagree with it silently.

**Release before the join, gated on a snapshot read of the target room.** Its strongest case: it
closes the window completely — the held room is gone before the seat exists, so no instant has both
live — and a `get(code)` check first would filter out the obviously doomed joins. Rejected because
the check cannot be made to hold. A snapshot read outside the target room's mutex is exactly the
stale read `mutate` exists to prevent, so a room that passes the check can be full, reaped or
`PLAYING` by the time `join` runs; the player then has no room and no seat, which `ADR-0141` §4
forbids in as many words: *"A mistyped code, a dead link and a stale invite cost the player
nothing."* Making the check sound would mean holding two room mutexes at once and inventing an
ordering rule between rooms, which is the deadlock `ADR-0133` §4 was written to make structurally
impossible.

**Fold the release into `RoomRegistry.join` itself, with no new method.** Its strongest case: the
smallest possible diff — `replyToJoinRoom` is not edited at all — and one obviously correct place
for the behaviour, since `join` is already the method that both seats a player and starts a duel,
so it is not a pure seat-writer whose purity is being spoiled. Rejected on reversibility and blast
radius. Measured: **twelve** test files under `poker-server/src/test/kotlin/` call `join` directly,
and giving it a second effect changes what every one of them is testing — and gives that effect to
every future caller by default rather than by choice. The wrapper is one method that
can be deleted to restore today's behaviour exactly — the cheapest reversal available, which is
what recommends it while the evidence for the release's shape is one ADR old.

**Answer the call site and lock order, and leave which rooms are released to the ticket.** Its
strongest case: `DEC-154` asks three questions and *which rooms* is not literally among them, so
the smallest answer is the three. Rejected because the `PLAYING`-sorts-first trap is invisible from
the register's wording and fatal in the obvious implementation — a coder handed *"release the held
room, after the join, under the player's stripe"* writes `heldRoom(player)` and abandons the duel
the player just joined. A decision that leaves the one non-obvious fact to the ticket has not
closed the question; it has moved it somewhere nobody will look for it.
