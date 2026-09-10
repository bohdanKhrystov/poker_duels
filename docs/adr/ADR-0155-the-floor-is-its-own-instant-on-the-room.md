# ADR-0155 — The floor is its own instant on the room, and `lastActivityAt` keeps its meaning

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-167` — **by what mechanism does a finished room's reaping deadline observe
  [`ADR-0153`](ADR-0153-a-rematch-offer-owes-its-recipient-a-minute.md) §1's sixty-second floor?**
  Registered 2026-09-10 by that ADR §6. Four answers, one per part of the question: the floor is a
  **second instant on `Room`** (`rematchFloorEndsAt`), armed in the `Offered` branch and read by
  `isReapable` as a **second conjunct**; sixty seconds is a **constant**, `RoomTimeouts.REMATCH_FLOOR_MILLIS`,
  with **no deployment override**; the floor **arms unconditionally**, on a room past its own deadline
  as on any other; and `ADR-0152` §6's characterisation test is **superseded and replaced** by the six
  cases in §6 below — it was never written, so nothing is deleted and everything is filed once.
- **Measured on `develop` at `faf14216`** — the commit that merged `ADR-0153`. `ADR-0152` §6's
  characterisation test **does not exist**: `RoomReapTest` holds nine cases and none of them mentions a
  rematch, no test in `poker-server/src/test` asserts a `FINISHED` room's `lastActivityAt` after an
  offer, and the board's `DEC-164` entry mints the test without a ticket having been filed for it. So
  *"the thing deliberately rewritten"* is a paragraph in an ADR, not a green test — which changes what
  this ADR has to do about it, and is why §6 states the replacement as cases rather than as a diff.
- **Carries out** [`ADR-0153`](ADR-0153-a-rematch-offer-owes-its-recipient-a-minute.md) §§1–3 and
  contradicts none of it: `max(finish + finishedMillis, offer + 60s)`, a floor and never a ceiling,
  armed once per finished room. §§4–5 are untouched — this ADR moves no wire type, no
  `PROTOCOL_VERSION`, no schema, no stored key, no engine file and no word on any screen, and the
  client learns nothing.
- **Leaves [`ADR-0152`](ADR-0152-the-finished-rooms-five-minutes-stand.md) §3 standing, byte-unchanged,
  and returns the permission it granted unspent.** §3 closed the set of writes to a `FINISHED` room's
  `lastActivityAt` at two — `Room.finish` and `RoomRegistry.resume`'s `touch` — and provided that an
  ADR could grow it. `ADR-0153` §6 expected this ADR to be that growth. It is not: the floor is a
  different field, so the write set is **still exactly two**, and §3 goes on forbidding a third.
- **Supersedes `ADR-0152` §6 entirely**, and **one clause of §4**: *"`Room.offerRematch`'s `Offered`
  branch stays `copy(rematchOffers = offers)`"* is false after this — the branch gains one argument.
  §4's **substance** survives literally: *"A standing offer goes on not restamping the clock"* is
  still true, because the clock it names is `lastActivityAt` and nothing here writes it. What §4's
  one-sentence property becomes is stated in §8.
- **Reads as evidence and leaves alone:** [`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md)
  (the per-room mutex, which is why §4 needs no new locking),
  [`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md) (the one-second sweep),
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) (one intent, one room fact,
  `ALREADY_OFFERED`, no withdrawal), [`ADR-0108`](ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
  §1 (the turn allowance the sixty is derived from) and
  [`ADR-0113`](ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md) §3's
  absolute-instant idiom, which this field follows.
- **Registers nothing.** No part of `DEC-167` turned out to be a product question: `ADR-0153` §1 fixed
  the promise and §4 fixed the silence, and everything left was where a number lives and who may read
  it.

## Context

**§1 is arithmetic over two numbers, and the code has one.** `ADR-0153` §1 says a `FINISHED` room on
which an offer stands dies at `max(finish + finishedMillis, offer + 60s)`. `RoomRegistry.isReapable`
(`RoomRegistry.kt:836-844`) is four lines and computes the left operand only: `now - room.lastActivityAt
>= timeouts.finishedMillis`, where `lastActivityAt` is written by exactly two calls — `Room.finish(now)`
(`Room.kt:238`) and `RoomRegistry.resume`'s `room.reconnect(seat).touch(now)` (`RoomRegistry.kt:390`),
the only `touch(` call site in the server. A `max` of two instants needs both instants to exist
somewhere, and the whole of this decision is **where the second one lives and what it is allowed to
displace**.

**The obvious place is the field that is already there, and that is the trap.** `ADR-0152` §4 named
the `Offered` branch — `copy(rematchOffers = offers)`, `Room.kt:330` — as *"exactly where any minimum
answering window would land … one extra field in one pure copy"*, and `ADR-0153` §6 put that shape
first in the question. Taking it means writing `lastActivityAt` to a value that makes the arithmetic
come out: `max(lastActivityAt, offer + 60s - finishedMillis)`. Three things push back. `Room` holds no
`RoomTimeouts` and reads no configuration, so it cannot compute that expression without being handed
`finishedMillis` — a new parameter on a pure method, threaded through every call site and every test
that has one. The value it would store is **not an activity**: `Room`'s own KDoc says
`lastActivityAt` is *"the timestamp, in milliseconds, of the room's most recent activity"*, and
`offer + 60s - finishedMillis` is an instant nothing happened at, and one in the **future** whenever
`finishedMillis` is under a minute. And a later `touch` overwrites it, discarding a floor that had
been armed — invisible while `finishedMillis` is five minutes, wrong the moment a deployment sets
`ROOM_FINISHED_TIMEOUT_MILLIS` (`ServerConfig.kt:123-125`) below sixty seconds. A bug whose existence
depends on an environment variable is the expensive kind.

**The sweep's granularity stops being a rounding error and becomes a question.** `ADR-0152` measured
the sweep at a fixed one second (`ServerConfig.DEFAULT_SWEEP_PERIOD_MILLIS`, `ServerConfig.kt:135`)
and concluded *"a room dies within a second of its deadline and the sweep's granularity is not a force
in this decision"* — true when nothing depended on the difference between *past its deadline* and
*gone*. A floor does: for up to a sweep period a room is dead by arithmetic and alive in the map, and
an `OfferRematch` frame that arrives in that window is accepted by `Room.offerRematch` — which reads no
clock and cannot know — and answered on the wire with `RematchOffered`. Whether that offer arms the
floor is a real fork, and both branches are defensible before you look at what they cost.

**The number is a promise, and the question is who may unmake it.** Every other duration in
`RoomTimeouts` is a deployment knob: `waitingMillis`, `finishedMillis`, `turnMillis` and
`timebankMillis` each have a `DEFAULT_` constant and an env override, and symmetry argues for a fifth.
But `finishedMillis` is a **hygiene** number — how long dead rooms linger in an unbounded
`ConcurrentHashMap` — while the sixty seconds is `ADR-0153` §1's promise to a player, made in the
product's voice and derived from the turn allowance. An override makes the promise per-deployment and
silently unmakeable, and nobody is asking for one.

**And `ADR-0152` §4's one sentence is being spent, which is the part a mechanism can hide.** §4 wrote
down that *"today a room's life is five minutes after the duel, one sentence, the same for both players
and independent of what either of them does"*, and `ADR-0153` knowingly spent it. A mechanism that
folds the minute into `lastActivityAt` spends it **invisibly** — the field keeps its name, the
predicate keeps its shape, and the only trace is arithmetic in a branch nobody reads. A mechanism that
gives the minute its own named field spends it **in the open**, at the cost of a field and a longer
predicate. That is the trade this ADR actually makes, and §8 says what replaces the sentence.

**Reversal is cheap either way, so the tie-breaker is which one is cheap to *unwind*.** A room is
in-memory only: `rooms` is a `ConcurrentHashMap` (`RoomRegistry.kt:53`), nothing persists a `Room`, and
a restart clears the lot — so a new field costs no migration to add and none to remove, and a `grep`
for its name finds every line that would have to go. Redefining what `lastActivityAt` means costs no
migration either, but nothing finds the code that has quietly come to rely on the new meaning. The
evidence for the sixty seconds is thin by `ADR-0153`'s own admission; the mechanism should therefore be
the one a later ADR can delete by name.

## Decision

### 1. The floor is a second instant on `Room`: `rematchFloorEndsAt`

`Room` gains a sixteenth property, in the defaulted tail so that every existing construction still
compiles:

```kotlin
val rematchFloorEndsAt: Long? = null,
```

placed immediately after `lastActivityAt` — before `runner` — so that the two instants a reaper reads
sit together, and so that every non-defaulted parameter still precedes every defaulted one. Its KDoc
states what it is, in `ADR-0113` §3's idiom of an absolute instant rather than a remaining duration:

> the instant before which this room is not reaped, whatever `lastActivityAt` says — `null` on every
> room no rematch offer has been recorded on. Armed once, by `offerRematch`'s standing-offer branch;
> cleared by agreement and by `abandon`. A floor and never a ceiling: it can only delay a reaping,
> never bring one forward.

It is named for the rule it serves, not for the effect it has. `reapNotBefore` would invite a second
caller with a different reason; `rematchFloorEndsAt` makes any second use a rename, and a rename is an
ADR.

### 2. The `Offered` branch arms it, agreement and `abandon` clear it, and an `init` guard makes that structural

`Room.offerRematch`'s standing-offer branch (`Room.kt:330`) becomes

```kotlin
RematchResult.Offered(copy(rematchOffers = offers, rematchFloorEndsAt = now + RoomTimeouts.REMATCH_FLOOR_MILLIS))
```

and nothing else in the server ever writes the field except the two clears: the `Agreed` branch
(`Room.kt:325`), which already writes `rematchOffers = emptySet()`, adds `rematchFloorEndsAt = null` in
the same `copy`; and `Room.abandon` (`Room.kt:277`), which already clears the offers, adds it to the
same `copy` too. `Room` keeps its purity intact — it still reads no clock, takes `now` from its caller
as it always has, and `REMATCH_FLOOR_MILLIS` is a compile-time constant, not configuration.

`init` gains a sixteenth `require`, beside the fifteen that are already there:

```kotlin
require(rematchFloorEndsAt == null || rematchOffers.isNotEmpty()) {
    "a rematch floor may only stand while a rematch offer does"
}
```

That one line does the work of a paragraph. Because `init` already requires `state == RoomState.FINISHED
|| rematchOffers.isEmpty()`, a floor implies a standing offer implies a `FINISHED` room; a `PLAYING`
room can never carry one, so an agreed rematch cannot leak its predecessor's floor into the next duel's
`finish`; and **a room with a floor and no offer cannot be constructed at all**. It also fixes the
arming site: a registry or a test that tried to arm the floor outside `offerRematch` would have to
construct exactly the room this `require` rejects.

**The one-arming bound (`ADR-0153` §3) is therefore structural, not a convention.** The `Offered`
branch is reachable only past the `ALREADY_OFFERED` guard (`Room.kt:315`), so the same seat pressing
twice never re-arms; the other seat's press takes the `Agreed` branch, which clears the floor and
leaves a `PLAYING` room the reaper never looks at; and nothing returns a room to `FINISHED` with its
offers intact. One arming per finished duel, and the next duel's floor is the next duel's business —
which is what §3's *"a finished room can take exactly one arming"* means once a rematch is agreed.

### 3. Sixty seconds is a constant, and no deployment overrides it

`RoomTimeouts`' companion gains

```kotlin
/** The minimum answering window a recorded rematch offer buys its recipient (60 seconds). */
public const val REMATCH_FLOOR_MILLIS: Long = 60 * 1000L
```

beside `DEFAULT_WAITING_MILLIS`, `DEFAULT_FINISHED_MILLIS`, `DEFAULT_TURN_MILLIS` and
`DEFAULT_TIMEBANK_MILLIS`, and **deliberately without the `DEFAULT_` prefix**: there is no field for it
to be the default of. `RoomTimeouts` gains no fifth field, `ServerConfig` gains no
`ROOM_REMATCH_FLOOR_MILLIS` key and no env variable, and nothing validates `finishedMillis` against it.
A deployment that sets `ROOM_FINISHED_TIMEOUT_MILLIS` below sixty seconds gets `ADR-0153` §1's `max`
applied exactly as written — its own number for rooms nobody offered on, sixty seconds for the ones
somebody did — and boots without complaint.

The constant lives in `RoomTimeouts` rather than in `Room` because that file is where a reader looking
for *how long is X* already goes, and because its KDoc is where §8's sentence has to be written down.

### 4. `isReapable` gains one conjunct, read under the lock that removes the room

```kotlin
RoomState.FINISHED, RoomState.ABANDONED ->
    now - room.lastActivityAt >= timeouts.finishedMillis &&
        (room.rematchFloorEndsAt == null || now >= room.rematchFloorEndsAt)
```

`null` reads as *no floor was ever armed*, never as *a floor of zero*, which is why the null is tested
rather than elvis-defaulted to a number that happens to work. The `WAITING` and `PLAYING` branches and
the `recording` guard above them are untouched, and the `FINISHED`/`ABANDONED` pair stays one branch:
by §2's invariant an `ABANDONED` room's floor is always `null`, so the added clause is free there and
costs one comparison here.

**No new locking, and no race.** `reap()` evaluates `isReapable` twice: once outside the lock as a
cheap early-continue, and once inside `holder.mutex.withLock` before `rooms.remove` — and
`RoomRegistry.offerRematch` arms the floor inside `mutate`, which holds that same mutex
(`ADR-0016`). So the two orders are the only two: an offer that wins the lock is visible to the
re-check, which then declines to remove the room; a reaping that wins removes it, so the press falls
into `mutate`'s `absent` branch and answers `UNKNOWN_ROOM`, exactly as it does today. The floor never
needs to be read outside the critical section that acts on it.

### 5. The floor arms on a room past its own deadline and not yet swept

Unconditionally. `Room.offerRematch` does not consult the deadline before arming, and could not without
being handed `finishedMillis`; `RoomRegistry.offerRematch` does not consult it either, though it could.
The rule is that **the promise attaches to the offer the server accepted, not to the room's remaining
life**: if the server recorded an offer and answered `RematchOffered`, the recipient is owed the minute
`ADR-0153` §1 says they are owed, and a recipient has no way to know their room was a hundred
milliseconds past an arithmetic deadline the wire never carried.

The consequence is one sweep period of slack on `ADR-0153` §3's bound, which the bound already had:
before this ADR a room's worst case was `finish + 5:00` plus up to a sweep, and after it the worst case
is `finish + 6:00` plus up to a sweep. §3's arithmetic is preserved exactly as §3 states it. Refusing
to arm inside that window is rejected in the Alternatives, on the ground that it manufactures the one
outcome `ADR-0153` exists to prevent — a panel that appears in front of a player and dies before they
can answer it — and would produce it precisely in the window where that failure is certain rather than
merely possible.

### 6. `ADR-0152` §6's characterisation test is superseded before it was ever written, and replaced by six cases

**Superseded, and replaced — not amended.** §6's test pinned that *"a standing offer leaves the deadline
where the finish put it"*, which `ADR-0153` §1 makes false; a test that pins the opposite of a merged
rule may not be repaired, only removed. It has never been written (measured above), so no file loses a
case and no green test turns red: what happens is that the replacement is filed **instead of**, and the
board's `DEC-164` entry stops describing work anyone should do.

The replacement is six cases. Five belong in `RoomReapTest`, against the registry and its `MutableClock`,
because the floor's whole meaning is what `reap()` does; one belongs in `RoomRematchTest`, against
`Room`, because that is where the field is written. Each is named here with the mutation it must fail
for — a case that no mutation kills is not a pin.

1. **The floor holds a room past its own deadline.** A `FINISHED` room, one seat's offer recorded at
   `finish + finishedMillis - 1ms`, is **not** reaped at `finish + finishedMillis`, is **not** reaped at
   `offer + 60_000 - 1ms`, and **is** reaped at `offer + 60_000`. Fails if the floor is dropped, and
   fails if the floor never ends.
2. **The floor never lowers a deadline.** With `finishedMillis` well above the floor, an offer recorded
   at `finish + 1_000` leaves the room alive at `offer + 60_000` and reaped at `finish + finishedMillis`
   exactly. Fails if a coder assigns the floor instead of maxing against it — the `ADR-0153` §2
   mutation, and the one that a single-fixture test cannot see.
3. **A repeat press does not move the floor.** The same seat offering again at `offer + 30_000` is
   refused `ALREADY_OFFERED` and the room is still reaped at `offer + 60_000`, not at
   `offer + 90_000`. Fails if the arming is hoisted above the `ALREADY_OFFERED` guard — the `ADR-0153`
   §3 mutation.
4. **A reload does not shorten the guaranteed minute.** With `finishedMillis` below sixty seconds, an
   offer followed by a `resume` mid-minute still leaves the room reaped at `offer + 60_000`. Fails
   under every mechanism that encodes the floor into `lastActivityAt`, which is why it is here.
5. **A room with no offer is unaffected.** The existing `aFinishedRoomIsReapedAtTheFinishedTimeout`
   already pins this and is not touched; it is named here so the replacement set is read as *five new
   cases beside one that stands*, not as a rewrite of the file.
6. **`Room`-level:** `offerRematch`'s `Offered` room carries `rematchFloorEndsAt == now + 60_000`, and
   the `Agreed` room that follows carries `null`. Fails if agreement leaks a stale floor into the next
   duel.

Two things the ticket must carry, because they are how this set stops being vacuous. **Two fixtures, not
one:** `RoomReapTest`'s `TEST_TIMEOUTS` is `finishedMillis = 4_000`, under the floor, so with it alone
the floor always dominates and case 2 cannot be observed at all — case 2 needs its own `RoomTimeouts`
with `finishedMillis` comfortably above `60_000`, and case 4 needs the existing short one. And **the
sixty seconds is asserted as a literal** `60_000` in at least cases 1, 3 and 6, never as
`RoomTimeouts.REMATCH_FLOOR_MILLIS`: a test that reads the constant follows it wherever it is changed
and can prove only that a copy happened.

### 7. What does not change

The write set at `lastActivityAt` is still `Room.finish` and `resume`'s `touch`, and `ADR-0152` §3 still
forbids a third. `finishedMillis`, `DEFAULT_FINISHED_MILLIS` and `ROOM_FINISHED_TIMEOUT_MILLIS` keep
their meanings and their values. The wire is untouched: no frame, type, field or `PROTOCOL_VERSION`
moves, `DuelSocket`'s resume path goes on replaying `RematchOffered` from `room.rematchOffers`
(`DuelSocket.kt:585`) and learns nothing about a deadline, and no client file changes. No screen gains a
word (`ADR-0153` §4). A room's life is still not a function of presence — the floor is armed by a
recorded offer, never by a socket, a screen or a heartbeat. And no counter is built: the instrument
`ADR-0152` and `ADR-0153` both named and neither built is still not built here.

### 8. The sentence that replaces `ADR-0152` §4's, and where it is written down

`ADR-0152` §4's *"a room's life is five minutes after the duel, one sentence, the same for both players
and independent of what either of them does"* is no longer true. What is true after this ADR:

> **A room's life is five minutes after the duel, or one minute after a rematch offer, whichever is
> later — and one rival press can pull the second, once.**

That sentence goes in two KDocs, because a sentence that lives only in an ADR is a sentence a reader of
the code never meets: `RoomTimeouts`' class KDoc, beside the line explaining what `finishedMillis` is
for, and `RoomRegistry.isReapable`'s, beside the `recording` guard it already explains. Naming the
rival press in it is deliberate — it is `ADR-0153`'s first named cost, and the reader who is about to
add a third write is exactly the reader who should meet it.

## Consequences

**What it buys.** `ADR-0153` §1's `max` becomes one conjunct against one field, and every part of §§1–3
becomes something a compiler or a test enforces rather than something a coder remembers: *floor never
ceiling* is a `&&` that can only delay, *armed once* is an `init` guard, *cleared on agreement* is an
invariant a stale floor would violate loudly. `lastActivityAt` still means what its KDoc says, so
`ADR-0152` §3's closed write set survives this decision instead of being the thing it spent — the one
outcome that leaves the next reader of the reaping code with fewer rules than they would otherwise have
had. The field is in-memory only, so this costs no migration to ship and none to remove, and every line
it touches is findable by one `grep` for a name that exists nowhere else.

**What it costs.**

- **The constant outranks the operator, silently.** A deployment that sets
  `ROOM_FINISHED_TIMEOUT_MILLIS` below sixty seconds — a load test, a staging box that wants rooms
  gone fast — gets rooms that outlive its own number by up to the difference, and nothing tells it so:
  no validation at boot, no log line, no metric, and no knob to turn it off short of a code change.
  That is the price of §3's refusal to add an override, and it is charged to the one person who
  configured a number expecting it to be the last word. The mitigation is only that the shipped default
  is five minutes, so the case is a deliberate act by an operator rather than an accident.
- **`isReapable` stops being four lines, and that was the point of it.** `ADR-0152` called it *"a piece
  of code whose whole virtue was that it was four lines"* and `ADR-0153` accepted a second duration on
  principle; this is where the bill arrives. The predicate now has a nullable to reason about, and
  every future reader of a reaping question holds two instants and one constant instead of one instant.
- **A nullable field is an invitation.** `rematchFloorEndsAt` is `null` on almost every room that has
  ever existed, and nothing but this ADR and its name stops a second writer arming it for a second
  reason — which would silently make a room's life a function of something new. There is exactly one
  read and one arming site today, and the only guard on tomorrow is that any other use needs a rename.
- **The worst case per room rises by a fifth, against a map that is still unbounded and still
  uncounted.** `ADR-0152`'s third cost — an unbounded `ConcurrentHashMap` with no cap and no
  instrumentation — is now carried for `finish + 6:00` rather than `finish + 5:00` in the worst case,
  and this ADR neither caps nor counts it.
- **The invariant takes flexibility away from callers on purpose.** A future fixture, replay tool or
  import path that wants a room with a recorded offer and no floor cannot build one: `init` throws. That
  is how §2 makes the one-arming bound structural, and it is a real constraint on code nobody has
  written yet.
- **The replacement test is a promise on paper until a planner files it, and nothing goes red if it is
  forgotten.** The test it supersedes was never written either — the same failure, one ADR earlier. §6
  names the file and the six cases and the two fixtures to make forgetting harder, but the honest
  statement is that this ADR ships no test and the trail depends on the next ticket.

**What it forecloses.** The restamp shape is foreclosed by name: after this, no ticket may express a
room's life as arithmetic on `lastActivityAt`, and `ADR-0152` §3 goes on saying so with this ADR as the
worked example of not doing it. A general *hold this room* primitive is foreclosed by naming — the field
says which rule it serves, so a second reason to hold a room is a rename and an argument rather than an
extra call. And an operator-facing floor is foreclosed only in the cheap direction: adding
`RoomTimeouts.rematchFloorMillis` plus a `ServerConfig` key later is additive and touches nothing
decided here, whereas taking a shipped knob away from a deployment that uses it is not.

**What it deliberately leaves open.** Whether `ServerConfig` should refuse a `finishedMillis` under the
floor at boot — no operator has asked, and the `max` already defines the behaviour, so a validation
would be inventing a constraint to make a cost look smaller. Whether an `ABANDONED` room should ever
carry a floor: it cannot today, by §2's invariant, and nothing in production abandons a `FINISHED` room
anyway (`ADR-0152`'s measurement). And the counter that would settle the sixty — still unbuilt, still
the thing to build before arguing with the number rather than with the mechanism.

## Alternatives considered

**Write the floor into `lastActivityAt` — `ADR-0152` §4's own door, and `ADR-0153` §6's first option.**
The strongest case in the set, and the one two merged ADRs pointed at. It adds **no field**: the
`Offered` branch becomes `copy(rematchOffers = offers, lastActivityAt = max(lastActivityAt, now + 60_000
- finishedMillis))`, `isReapable` is untouched and stays four lines, no invariant is needed, no nullable
enters the predicate, and every existing test that reasons about reaping goes on reasoning about one
number. `ADR-0152` §4 described it as *"one extra field in one pure copy"* and §3 held the door open for
exactly this growth, provided an ADR walked through it. Rejected on four counts, in ascending order of seriousness. It needs `finishedMillis` inside
`Room`, which today reads no configuration at all — so either `offerRematch` grows a parameter that every
call site and every test must pass, or `RoomRegistry` performs the arithmetic itself and becomes the
third writer of a field `ADR-0152` §3 closed at two. It stores a value that is not an activity and, when
`finishedMillis` is under a minute, is not even in the past — a field whose KDoc becomes false and whose
every future reader is reading two concepts. It **loses the floor to a later `touch`**: a reload during
the minute overwrites the arithmetic, harmless at five minutes and wrong below sixty seconds, which is a
defect that only exists in some deployments. And it spends `ADR-0152` §4's property invisibly: the field
keeps its name, the predicate keeps its shape, and the only record that a rival's press can now extend a
stranger's room is a `max` inside a branch. Cheap to write, expensive to unwind, and the mechanism that
hides the thing `ADR-0153` wanted stated.

**Store the room's whole reaping deadline — `reapAt`, computed at `finish` and raised by an offer.**
Genuinely strong, and simpler than what is decided here at the point of use: `isReapable`'s
`FINISHED`/`ABANDONED` branch becomes `now >= room.reapAt` with no configuration lookup and no nullable
at all, the `max` is applied where it is written rather than where it is read, and `TurnDeadline`
already establishes the house idiom of an absolute instant travelling with the room (`ADR-0113` §3).
Rejected because it moves policy into state. `finish` would have to know `finishedMillis` — the same
coupling as the alternative above, on a hotter path — and a deployment that changed
`ROOM_FINISHED_TIMEOUT_MILLIS` would find rooms already finished still carrying the old number, which is
a new class of surprise. It also splits the predicate: `WAITING` rooms would keep reading `timeouts`
while `FINISHED` ones read a field, or every state gains a deadline and the change stops being small.
The shape to reach for if a finished room's absolute life ever needs capping — which is where
`ADR-0152`'s Alternatives already sent it.

**Never reap a room while an offer stands.** The cheapest thing that could possibly work, and it needs
neither a new field nor a new number: `isReapable` reads `room.rematchOffers.isEmpty()`, a field that
already exists, and a control the product offered can always be pressed. Rejected because it is not a
floor, it is a lease with no end. An offer cannot be withdrawn and silence is the decline (`ADR-0044`
§§6–7), so nothing ever clears a standing offer on a room nobody agrees to — one press would hold a
room until the process restarts. That breaks `ADR-0153` §3's bound outright, and §3 is the reason the
minute was affordable in the first place. It is the exact alternative that shows why a *number* is
needed rather than a *flag*.

**A `RoomTimeouts.rematchFloorMillis` field with a `ROOM_REMATCH_FLOOR_MILLIS` override.** Its case is
consistency and it is not weak: the other four durations in that class are all deployment-tunable, an
operator running with a short `finishedMillis` could keep the two numbers coherent instead of being
overruled by a constant, and a test could dial the floor down instead of advancing a clock past it. The
usual objection — *tests will have to wait a minute* — is false here, because the clock is injected and
virtual (`MutableClock`, `ADR-0062`), so advancing `60_000` costs nothing. Rejected because the sixty
seconds is a **promise**, not a capacity setting: `ADR-0153` §1 stated it in the product's voice, and an
env variable makes it a per-deployment claim that can be set to `1` and quietly stop being true, with no
screen and no log to notice. Two knobs whose interaction is a `max` neither of their names mentions is
also worse to reason about than one knob and one constant. Additive later, if an operator ever appears
with a reason.

**Keep the floor in the registry — a `ConcurrentHashMap<RoomCode, Long>` beside `recording`.** It has a
precedent in the very file that would hold it: `recording` is exactly such a side map, consulted by
`isReapable` and keyed by `RoomCode`, and this shape leaves `Room` untouched — no field, no invariant,
no test that constructs a room needing an update. Rejected because `recording` is about an **in-flight
call**, while a floor is a fact about the **room**, and `ADR-0016` put every fact about a room in one
type under one mutex — `Room`'s KDoc for `runner` accepts duplicated data as *"a known cost of keeping
the duel in one lock and one type rather than a second structure"*, which is the trade being made in
reverse here.
A side map also needs its own removal path or it leaks entries for every room ever reaped, and it makes
the floor invisible to everyone holding a `Room`, including the projection layer and every test.

**Arm the floor in `RoomRegistry.offerRematch` rather than in `Room`.** Its case is purity as a
principle: `Room` would keep reading no clock **and** no duration, and the number would live next to the
`timeouts` the registry already holds, which is where policy arguably belongs. Rejected because it
splits one fact across two types — the branch that records the offer and the write that arms the floor
would be in different files, so `Room.offerRematch` on its own would produce a room the `init` guard in
§2 rejects, and that guard is what makes the one-arming bound structural. Choosing this alternative
means giving up the invariant, which is most of what §2 buys. The purity argument does not survive
inspection either: a `const val` is neither a clock nor configuration, and `Room` already takes the only
thing it must not invent — `now` — from its caller.

**Refuse to arm on a room already past its deadline but not yet swept.** Its case is exactness, and it
is the tidiest reading of `ADR-0153` §3: a room whose deadline has passed is dead, the sweep simply has
not caught up, and letting a late offer resurrect it means the bound is really *`finish + 6:00` plus a
sweep period*. It is also implementable — `RoomRegistry.offerRematch` holds both `timeouts` and `now`
and could ask `isReapable` before writing back. Rejected because it buys arithmetic tidiness with the
exact failure `ADR-0153` exists to prevent, and buys it in the worst place: inside that window the
server would accept the offer, answer `RematchOffered`, put the panel in front of the recipient, and let
the room die on the next tick — a lie that is **guaranteed** rather than merely possible. Refusing the
offer outright instead would be worse still: a clock-dependent `UNKNOWN_ROOM` for a room that exists,
inventing a refusal `ADR-0044` never defined. And the slack it objects to is not new — `finish + 5:00`
has always meant *plus up to a sweep* — so it is a rounding error being paid for with the product's one
named promise.
