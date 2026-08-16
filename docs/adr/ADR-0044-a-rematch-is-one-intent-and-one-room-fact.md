# ADR-0044 — A rematch is one client intent and one room fact, and EPIC-02 ships the server half

- **Status:** Accepted
- **Date:** 2026-08-16
- **Resolves:** `DEC-023`
- **Constrains:** [`STORY-0213`](../../tasks/stories/STORY-0213-the-wire-carries-a-rematch.md) (the
  server half), [`STORY-0309`](../../tasks/stories/STORY-0309-rematch.md) (the client half), and
  the wire, via `PROTOCOL_VERSION`

## Context

The room layer has been able to run a rematch since `EPIC-02`. `Room.offerRematch` records one
offer per seat, agrees when both have offered, flips `openingButtonSeat`, and starts a fresh
`MatchState`. `RoomRegistry.offerRematch` wraps that in the room's own mutex, refuses while the
previous duel's result is still in flight (the `recording` guard), and — since `TASK-020733` —
hands back the new duel's opening frames on `RematchResult.Agreed.outbound`, exactly as seating
does. `ADR-0022` keeps a finished room alive for `RoomTimeouts.finishedMillis` (default five
minutes) precisely so a rematch has somewhere to happen.

None of it is reachable. `ClientMessage` is `Hello | CreateRoom | JoinRoom | Act`, `ServerMessage`
carries nothing about a room beyond `RoomJoined`, and `DuelSocket`'s exhaustive `when` over
`ClientMessage` has no branch that could call `offerRematch`. `docs/vision.md`'s success condition
ends on *"We hit Rematch"*, and `STORY-0309` is the only story in `EPIC-03` still blocked.

What makes this a decision rather than a chore:

**The obvious cheap answer is already taken.** Re-`JoinRoom`ing a finished room looks free — the
client holds the code, the message exists, no version moves. But `replyToJoinRoom` calls
`RoomRegistry.resume` *first*, and a seated player in a `FINISHED` room already gets `RoomJoined`
plus `DuelFinished` from it. That path is the reconnect `STORY-0310` is built on. One frame cannot
mean both "put me back where I was" and "start another duel", because the client cannot see which
one the server will pick.

**Every extra frame is free today and expensive later.** `protocolJson` sets
`ignoreUnknownKeys = false` and the handshake compares versions for exact equality, so version
equality is the *only* compatibility mechanism this protocol has (`ADR-0028` §8). Before the first
browser caches a client, adding a message costs a constant, a doc line and one
`generateProtocolTypes` run. Afterwards it costs a compatibility window. That is a reason to settle
the whole shape now — and, symmetrically, a reason not to put anything on the wire that the client
would not be allowed to act on.

**A deadline on the wire would be cosmetic.** `ADR-0028` already decided the shape of a countdown:
the client renders it and *never* acts on it, because the server is authoritative about when a
window has closed. Applied here, a rematch deadline could not retire the button — only a refusal
could — so it buys a rendering, not a behaviour.

**And `EPIC-03` cannot write any of it.** That epic's own out-of-scope rule is *"any change to the
server, the protocol, or the rules: nowhere in this epic"*, and its module is `web-client`. So the
answer is incomplete until it names who ships the Kotlin — and the epic that promised rematch in
its scope line, `EPIC-02`, is `done`.

## Decision

**A rematch is one client intent and one room fact.** `ClientMessage` gains `OfferRematch`,
`ServerMessage` gains `RematchOffered`, the rematch's *start* is the frames it already produces,
and `EPIC-02` reopens with `STORY-0213` to ship the server half.

### 1. `ClientMessage.OfferRematch` is a `data object`

```kotlin
@Serializable
@SerialName("OfferRematch")
public data object OfferRematch : ClientMessage
```

It names no room, no seat and no duel. The socket's own `RoomMembership.code` names the room and
the handshake's session names the player, so a client cannot offer a rematch in a room it never
entered — structurally, not by a check. `Act` set this precedent: the frame that acts *inside* a
room names no room.

### 2. `ServerMessage.RematchOffered(seat)` is the room fact, and both seats get it

```kotlin
@Serializable
@SerialName("RematchOffered")
public data class RematchOffered(val seat: Int) : ServerMessage {
    init { require(seat in 0..1) { "seat must be 0 or 1, was $seat" } }
}
```

It means exactly one thing: **an offer from this seat stands.** It is delivered to *both* seats
through `deliver`, so the offering client learns its offer was recorded from the server rather
than by assuming it, and the opponent learns it from the same frame. Each client already knows its
own seat from `RoomJoined` and compares.

`seat` is the only field. The frame carries no card, no `PlayerView`, no `GameEvent` and nothing
derived from engine state, so it is built where `RoomJoined` is built — in transport, from the
`Room` the registry just returned — and not in the engine's projection layer. That layer keeps its
monopoly on every frame that carries game state; this one carries none.

### 3. The offer is idempotent on the wire

`Room.offerRematch` refuses a repeat with `RematchRefusal.ALREADY_OFFERED` and leaves the recorded
offers untouched. On the wire that is **not** an error: the socket answers it with the same
`RematchOffered(seat)` it already sent, rebuilt from `Room.rematchOffers` on a room read fresh from
the registry — never from the request. `replyToJoinRoom` already does exactly this for
`RoomRefusal.ALREADY_SEATED`, answering with the seat the player holds rather than a failure.

A double click therefore cannot produce an error state, and no client needs an in-flight lock to
avoid one.

### 4. The rematch's start is the frames it already produces

There is no `RematchStarted`. `RematchResult.Agreed.outbound` is `startDuel`'s output — for each
seat an `Events` and a `Snapshot`, plus a `YourTurn` for the seat on turn — and the socket delivers
it through `deliver`, exactly as `JoinResult.Seated.outbound` is delivered. The button has already
changed sides in the `Snapshot`, because `Room.offerRematch` flipped `openingButtonSeat` before the
runner was built.

This makes one rule, which `docs/protocol.md` states in the same change: **after a `DuelFinished`, a
`Snapshot` means a new duel has begun in that room — the rematch.** Nothing else can produce one.
`resumeFrames` gives a finished duel `finishedFrames` alone, so a reconnect into a finished room
carries `DuelFinished` and never a `Snapshot`.

### 5. A standing offer survives a reconnect

When `RoomRegistry.resume` answers for a `FINISHED` room, the socket sends one `RematchOffered` per
player in `Room.rematchOffers`, mapped to a seat by `Room.seatOf`, **after** `RoomJoined` and after
the resumed frames. The order is load-bearing: `DuelFinished` is where a client (re)enters its
result screen, so an offer stated before it would be discarded by a reducer that treats
`DuelFinished` as that screen's beginning.

Without this, an offer made while the opponent was inside their disconnect grace window would be
delivered to nobody and never restated — the one way this feature could silently lose a fact the
room is still holding.

### 6. Refusals map onto the closed `ProtocolError` set, which gains one value

- **The room is gone, or this player holds no seat in it** — `RematchRefusal.UNKNOWN_ROOM`,
  `RematchRefusal.NOT_A_PLAYER`, and a socket that has entered no room — all answer
  `Failure(UNKNOWN_ROOM)`. Collapsed for the reason `replyToAct` collapses its three checks into
  `NOT_IN_DUEL`: an offer must not become an oracle for which rooms are alive. **This is the frame
  that ends a rematch**: the room has been reaped, and the client says so and offers the way back
  to the lobby.
- **The room cannot take an offer yet** — `RematchRefusal.NOT_FINISHED` — answers
  `Failure(REMATCH_UNAVAILABLE)`, a new value in `ProtocolError`, documented as **transient**:
  nothing was recorded and the same offer may be sent again. It covers both a duel still running
  and `RoomRegistry`'s `recording` guard — the window, immediately after `DuelFinished` reaches the
  player, in which the finished duel's result is still being written. A client told *"the duel is
  not finished"* there would be lying to a player who has just been shown the result, which is why
  the new value is named for the rematch and not for the duel.
- **`ALREADY_OFFERED` is not a refusal on the wire** — see §3.

### 7. `PROTOCOL_VERSION` moves, one step, and this ADR does not name the number

By `ADR-0028` §8's rule, which this change follows rather than re-argues: one number must name
exactly one wire shape, so this takes **its own step, the next number free when it lands**. It is
not "3". `ADR-0027`'s bump (`STORY-0405`) and `ADR-0028`'s are both unlanded and each already
claims the next free number; whichever of the three lands first takes 3, the second 4, the third 5.

`docs/protocol.md` gains two message rows, the `REMATCH_UNAVAILABLE` bullet, the §4 paragraph and a
new version line — **in the same change as the Kotlin, and it cannot move before it**:
`ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` fails the build on a
documented message that does not exist. That is why this ADR does not touch that document.

### 8. `EPIC-02` owns the server half, as `STORY-0213`, and reopens for it

`EPIC-02`'s scope line reads *"Rooms: create, join by code, seat exactly two, **rematch**"*. It
shipped the rematch as far as `RoomRegistry.offerRematch` and stopped one wire message short. The
unfinished half returns to the epic that promised it, in the module that owns the code
(`poker-server`), as a new story: `STORY-0213 — The wire carries a rematch`. `EPIC-02`'s status
returns to `in-progress` until that story is `done`, and its metrics table is annotated with the
date it first closed rather than quietly re-measured.

`EPIC-03` does not widen. `STORY-0309` consumes `STORY-0213` and writes no Kotlin, which is the
rule that produced this decision in the first place.

### 9. What does not change

`poker-engine` gains nothing: no clock, no room, no networking, no event, no schema bump. `Room`,
`RoomRegistry`, `RematchResult`, `RematchRefusal`, `RoomTimeouts` and `SeatDelivery` are all
untouched — the server half is two protocol types, one enum value, one `when` branch in
`DuelSocket`, the resume path's restatement, `docs/protocol.md`, and one regeneration of
`web-client/src/protocol/protocol.gen.ts`. No new room state, no new timer, no persistence.

### 10. What the tests must prove

- Two seats, one `OfferRematch` each: the first produces exactly one `RematchOffered(0)` on **both**
  sockets and no new duel; the second produces the opening frames of a fresh duel on both, with the
  button on the other seat.
- A second `OfferRematch` from the same seat produces a `RematchOffered` for that seat and **no**
  second recorded offer — proven by the opponent's offer being the one that starts the duel.
- An `OfferRematch` from a socket that entered no room, from a stranger, and for a reaped room all
  answer `Failure(UNKNOWN_ROOM)` and are indistinguishable from each other.
- An `OfferRematch` while the duel is still running answers `Failure(REMATCH_UNAVAILABLE)` and
  changes nothing, and a later offer after the duel ends is accepted — the transient claim made
  executable.
- A seat that offers while the opponent is disconnected, followed by the opponent reconnecting via
  `JoinRoom`, puts `RematchOffered` on the returning socket **after** its `DuelFinished`.
- No frame introduced here carries a card: the existing descriptor walk (`ProtocolPayloadTest`)
  covers both new types the day they are added, and a rematch's opening frames are the projection
  layer's, unchanged.

## Consequences

**What it buys.** The vision's last unimplemented sentence becomes buildable, in the smallest wire
surface that can carry it: one intent in, one fact out, and a start that reuses frames the server
already sends. The offer is idempotent, so no client has to guard a button; it survives a
reconnect, so no fact the room holds is unreachable; and every refusal collapses to either "not
now, try again" or "the room is gone", which are the only two things a screen has to say.

**What it costs.**

- **`EPIC-02` reopens.** A closed epic's metrics ledger (`tasks/BOARD.md`) no longer covers every
  ticket that will eventually sit under it, and *"done"* becomes a state an epic can leave. That is
  a real dent in Product B's trail, and it is taken deliberately rather than by inventing a home
  where the code does not live. It also sets a precedent that already has a second claimant:
  `ADR-0028`'s server half (`OpponentPresence`, `ActedForAbsentSeat`) is registered in no epic
  either, and belongs the same way. This ADR does not file it — that would be widening — but it
  names it, because the same argument decides it.
- **A third pending version bump.** Three unlanded ADRs now each claim "the next free number", so
  until they land no single document can state what the wire's version will be. The rule keeps them
  correct; the bookkeeping is the price. Each also costs a `protocol.gen.ts` regeneration and a
  line in `web-client/src/protocol/version.ts`, and the client's build fails until it moves.
- **"A `Snapshot` after a `DuelFinished` is the rematch" lives in prose and one reducer branch, not
  in the type system.** It is true only because `resumeFrames` gives a finished duel nothing else.
  A future frame that legitimately snapshots a finished room — spectating (`ADR-0040`), a replay
  viewer — would break the inference, and the protocol would then need the explicit start frame
  after all, at the price of another bump.
- **The wire carries no deadline, so the button can lie.** A player may sit on a live-looking
  Rematch control for minutes after the room was reaped; the click answers truthfully and the
  screen recovers, but nothing tells them beforehand. The result screen cannot show *"4:52 left"*
  without one more field and one more bump.
- **An offer cannot be withdrawn.** `Room` records no retraction and this adds none, so a player
  who offers is committed until the room dies. Declining is silence.
- **The client's store gains state no single frame establishes.** Which seats have offered is
  accumulated across `RematchOffered` frames and cleared by the `Snapshot` that starts the rematch
  — bookkeeping in the same class as `ADR-0043`'s `rejectionCount`, and the second time the store
  has held something the server did not send whole.

**What it forecloses.** Nothing structural — every rejected option below stays addable behind one
version bump. It does deliberately *not* build: offer withdrawal, an explicit decline, a countdown,
a rematch offered to anyone who was not seated, and a rematch that outlives the room. That last one
is a different feature: two players who want another duel after the window closes create a room and
share a link, which is what the lobby already does.

## Alternatives considered

**The client re-`JoinRoom`s the finished room.** Its strongest case: zero new wire surface, no
version bump, no new error, and the room code is a handle the client already holds — the server
could simply read "a seated player joining a `FINISHED` room" as an offer. Rejected: that frame
already means something else. `replyToJoinRoom` calls `resume` first, and a seated player in a
`FINISHED` room receives `RoomJoined` + `DuelFinished` — the path `STORY-0310`'s reconnect depends
on. Overloading it would make a browser refresh on the result screen indistinguishable from
committing to another duel. And it answers only half the question: nothing would tell the
*opponent* an offer had been made, so a server → client message would be needed anyway, and the
saving evaporates.

**HTTP: `POST /api/rooms/{code}/rematch`.** Its strongest case: `GET /api/me` and `/api/me/duels`
show the pattern is available and documented; HTTP has a status code for every refusal without
touching the closed `ProtocolError` set; and `PROTOCOL_VERSION` would not move at all. Rejected:
the *answer* has to reach two sockets whatever carries the request, so the socket stays in the path
and this adds a second entry point rather than removing one. It would also be the first **write**
over HTTP, needing its own authentication story (`ADR-0027`'s Bearer token, unlanded) for a feature
that is not waiting on it; and a code in a URL reintroduces "the client says which room", which the
socket's `RoomMembership` answers structurally.

**An explicit `RematchStarted` frame ahead of the opening frames.** Its strongest case: the
transition becomes a fact on the wire instead of an inference, it is immune to any future frame
that makes a `Snapshot` legal after a finish, and it gives the client one unambiguous place to
discard whatever it was still holding from the duel that ended. Rejected, narrowly: it carries
nothing the `Snapshot` immediately behind it does not already carry, and `ADR-0017`'s rule is that
the protocol invents no second vocabulary for a fact another frame states. Recorded here because it
is the alternative most likely to become right — if spectating or replay ever puts a `Snapshot`
after a `DuelFinished`, this is the change to make, and §4's paragraph in `docs/protocol.md` is
where the breakage will show.

**A deadline on the wire** — `expiresInMillis` on `RematchOffered`, or a room-lifetime field on
`DuelFinished`. Its strongest case: direct precedent in `ADR-0028`, which put a remaining-millis
countdown on `OpponentPresence` for exactly this kind of window, and it lets the screen say how
long the offer is good for instead of showing a control that may already be dead. Rejected: under
`ADR-0028`'s own rule the client renders such a number but never acts on it, so a countdown cannot
retire the button — only the server's refusal can — which makes it cosmetic rather than
behavioural. On `RematchOffered` it would also reach a player only once *someone* had offered,
missing the case it was wanted for; and moving it to `DuelFinished` would make a duel's outcome
carry a room's lifetime, which `ADR-0017` deliberately kept apart. It stays one field and one bump
away for whenever the product asks for the countdown.

**`OfferRematch(code)`, symmetric with `JoinRoom`.** Its strongest case: self-describing frames are
easier to log, replay and test, and it would survive a socket that had forgotten its membership.
Rejected: the socket cannot forget — `RoomMembership.code` is set on every path that enters a room
and cleared on none — so the code would be a claim the server must check against the membership it
already trusts, which is strictly more surface for no new capability. `Act` is the precedent, and
`ADR-0002` prefers the version where the client asserts less.

**A `RematchDeclined`, or a withdrawal.** Its strongest case: a player who is leaving could say so,
and the opponent would stop waiting instead of staring at a hopeful screen for five minutes.
Rejected: `Room` records neither, so this would be new room state, a new refusal, and a new race
(declining a rematch that has just agreed), for a case the finite window already resolves. Silence
is a decline. If the waiting screen turns out to be the complaint, that is one message on a later
bump.

**The server half lands inside `EPIC-03` as a recorded exception.** Its strongest case: one epic,
one branch, one story, and the client is the only reason the frames exist — splitting the work
across two epics costs an extra story, an extra dependency and an extra merge order to get wrong.
Rejected: `EPIC-03`'s out-of-scope rule is the *reason this decision exists*, and its module is
`web-client` — tickets under it touching `poker-server` would make the board's module column false
and would put a protocol change behind a client review. The epic's rule survives this decision
intact, which is the point of having it.
