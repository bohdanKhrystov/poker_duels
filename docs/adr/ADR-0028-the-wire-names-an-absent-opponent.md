# ADR-0028 — The wire names an absent opponent, and marks every action taken for one

- **Status:** Accepted
- **Date:** 2026-08-14
- **Resolves:** `DEC-018` — **the human's product call**, made verbatim as *"away + countdown + mark
  timeout folds"*. This ADR does not choose it; it records it and works out what it costs in types,
  emission points, clock discipline and wire version.
- **Amended by** [`ADR-0071`](ADR-0071-a-discriminator-is-its-kotlin-type-name.md) — §1's second
  subtype is renamed **`ActedForAbsent`**, Kotlin type and `@SerialName` together, because
  `ProtocolDiscriminatorTest` has refused any discriminator over sixteen characters since
  `TASK-020210` and `ActedForAbsentSeat` is eighteen. **Nothing else in §1 moves** — the four
  fields, both `require` blocks and both recipients stand — and `SeatPresence`,
  `OpponentPresence` and §§2–8 are untouched. **Read `ActedForAbsent` for `ActedForAbsentSeat`
  everywhere below.**
- **Amends:** [ADR-0013](ADR-0013-disconnect-grace-period.md) — the pause and the grace window are
  no longer silent; and [ADR-0023](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md), whose
  context took it as settled that an absence action is *"indistinguishable in the log and on the
  wire from one the player sent"*. **The log half stands. The wire half does not.**
- **Constrains:** the follow-on server work for
  [`STORY-0208`](../../tasks/stories/STORY-0208-disconnect-grace-period.md); what
  [`STORY-0306`](../../tasks/stories/STORY-0306-duel-table-screen.md),
  [`STORY-0307`](../../tasks/stories/STORY-0307-action-bar.md) and
  [`STORY-0310`](../../tasks/stories/STORY-0310-reconnect-and-resume.md) may render; and the wire,
  via `PROTOCOL_VERSION`.

## Context

`ADR-0013` holds a dropped player's seat for a grace window, pauses the duel, and folds when the
window expires. It never says what the player who is *still there* is told, and the shipped answer
is nothing: an action during the pause comes back `DUEL_PAUSED` and no other frame is ever sent. A
present player cannot tell a paused duel from a slow opponent, and a fold the server submitted is
byte-identical on the wire to a fold somebody chose. The human has now decided that is dishonest and
has asked for the largest of the four options put to them. What is left is entirely technical, and
six things are in tension.

**There is no clock the two sides share.** `ServerClock` is `System.nanoTime()`-based on purpose —
monotonic, immune to host time corrections, and measured *from an arbitrary epoch*. `Room.gracePeriods`
holds absolute deadlines on that scale. That number is meaningless in a browser, so "send the
deadline" cannot be taken literally, and the obvious repair — convert to wall-clock epoch millis —
puts a second, non-monotonic clock into the one code path `ServerClock` exists to keep off it, and
then makes the countdown depend on the *client's* clock being right.

**`ADR-0002` versus a number counting down in a browser.** A rendered countdown is the most tempting
client-side authority in the product. It will reach zero before the server acts — always: `ADR-0025`
sweeps on a fixed delay, so a window ends up to `sweepPeriodMillis` (default one second) plus the
previous pass late, and network latency is on top of that. A client that treats its own zero as an
event is a client asserting a game fact.

**"Mark timeout folds" does not describe what the server actually does.** Since `ADR-0023`,
`foldAbsent` reads the engine's legal set and sends `Fold` only when the seat faces a bet; otherwise
it sends `Check`. Marking folds alone would label the loud case and leave a checked-down absent seat
silently indistinguishable — the same dishonesty one level down.

**The mark cannot live in the event log.** `poker-engine` is pure and knows nothing of sockets,
clocks or absence, and `GameEvent` is the engine's. Provenance is therefore a fact about the
*server*, not about the game, and it can only ride beside the events rather than inside them.

**Absence is state, and this system loses edges.** `deliver` silently skips a seat with no live
writer, and `resumeFrames` deliberately replays nothing — a returning player gets a fresh
`Snapshot`, never the events they missed. So anything sent only at the moment of a transition is
lost to whoever was not connected for it, and a resume path that carries no presence leaves a
returning player with a confidently wrong picture of the table.

**Two wire breaks are in flight at once.** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md)
already moves `PROTOCOL_VERSION` to 3 in `STORY-0405`, on an independent branch.

### The deadline, honestly

Same one `ADR-0027` names, and it is the reason to decide now rather than a reason to decide a
particular way: **no client is deployed.** A new `ServerMessage` subtype is a breaking change in this
protocol — an old client cannot decode a frame whose `type` discriminator it has never heard of —
so this is free today and a compatibility window later.

## Decision

### 1. Two new `ServerMessage` subtypes and one enum

Generated protocol types, in `duels.poker.server.protocol`, like every other wire type. Nothing is
hand-written and nothing new is added to `poker-engine`.

```kotlin
/** How present the seat a frame describes is, from the server's point of view alone. */
public enum class SeatPresence { PRESENT, AWAY, ABSENT }

@Serializable
@SerialName("OpponentPresence")
public data class OpponentPresence(
    val presence: SeatPresence,
    val graceRemainingMillis: Long? = null,
) : ServerMessage {
    init {
        require((presence == SeatPresence.AWAY) == (graceRemainingMillis != null)) {
            "graceRemainingMillis is present exactly when presence is AWAY"
        }
        require(graceRemainingMillis == null || graceRemainingMillis >= 0) {
            "graceRemainingMillis must not be negative, was $graceRemainingMillis"
        }
    }
}

@Serializable
@SerialName("ActedForAbsentSeat")
public data class ActedForAbsentSeat(
    val seat: Int,
    val handNumber: Int,
    val actionSequence: Int,
    val action: ActionType,
) : ServerMessage {
    init {
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(action == ActionType.FOLD || action == ActionType.CHECK) {
            "the server only ever folds or checks for an absent seat, never $action"
        }
    }
}
```

The three presence values are exactly the three states `Room` already distinguishes: `AWAY` is
`seat in gracePeriods` — the window is running and the duel is **paused**; `ABSENT` is
`seat in absentSeats` — the window ran out, the duel is **live again**, and the server gives up that
seat's turns for it; `PRESENT` is neither.

`OpponentPresence` carries no seat number. It is addressed to exactly one seat and its whole content
is relative to that recipient, the same way `YourTurn` is; a seat field would be a second thing to
get wrong at the one place — `deliver` — that already decides where a frame goes.
`ActedForAbsentSeat` is the opposite kind of frame: it is a fact about the log both seats share, it
goes to both seats identically, and it therefore must name the seat it is about.

`ActedForAbsentSeat`'s `init` pins `ADR-0023` at the wire boundary: a frame claiming the server
called, bet, raised or moved all-in for an absent seat cannot be constructed.

### 2. The countdown is a remaining duration, sent once — not a deadline, not a tick

`graceRemainingMillis` is how much of the window was left **at the instant the server built the
frame**. The client turns it into a local deadline once and counts down from that against its own
elapsed-time source. One frame per transition, never a frame per second.

A duration rather than an absolute deadline because the two sides share no epoch: the server's only
clock for durations is monotonic from an arbitrary origin, and the alternative epoch — wall clock —
is the one `ServerClock` was written to keep out of timeout code. A duration needs no agreed epoch
and no correct client clock; it needs only that each side can measure its own elapsed time. A single
frame rather than a per-second tick because the client can compute every intermediate value itself,
because `ADR-0025`'s ticker exists to expire windows and not to animate them, and because ticking
would silently promote `sweepPeriodMillis` — a server tunable — into a user-visible frame rate.

The remaining is computed in `RoomRegistry`, inside the same `mutate` critical section that reads
the deadline, and nowhere else — the mirror of the rule already written on `RoomRegistry.disconnect`,
where the deadline itself is computed and nowhere else. `Room` still reads no clock: it gains one
pure function,

```kotlin
public fun presenceOf(seat: Int, now: Long): ServerMessage.OpponentPresence
```

which derives the frame describing `seat` from `gracePeriods`, `absentSeats` and the `now` it is
handed. A `Room` method returning a `ServerMessage` is precedented — `Room.act` already builds the
`DUEL_PAUSED` failure. **No new field is stored anywhere**: presence is a projection of state the
room already keeps, so there is nothing new to keep consistent.

`graceRemainingMillis` is clamped at zero. `AWAY` with zero remaining is a real and legal frame: it
means the window has run out but the sweep has not landed yet, which is the honest thing to say and
a precise illustration of the next rule.

### 3. The deadline is server state; the client renders it and never acts on it

This is the failure mode most likely to be coded wrong, so it is stated as a rule rather than left
to be inferred:

- **The client never changes what it does because its countdown reached zero.** It does not
  re-enable an action bar, does not send anything, does not mark a hand lost, does not assume the
  duel resumed. The only facts are the frames the server sent: the duel is paused until an
  `OpponentPresence` carrying `ABSENT` or `PRESENT` says otherwise, and an action sent before then
  is refused with `DUEL_PAUSED` and moves nothing, exactly as `Room.act` already refuses it.
- **The server never reads a client's notion of elapsed time.** No `ClientMessage` gains a
  timestamp, a deadline or a remaining-millis field, now or later. There is nothing on the wire the
  server would consult, and adding one would be a client asserting a game fact.
- **`Room.gracePeriods` remains the single deadline in the system.** `graceRemainingMillis` is a
  projection of it in the same sense that `PlayerView` is a projection of `GameState`: a rendering
  aid, never a contract, and never read back.
- The client's countdown is expected to reach zero *early* by up to `sweepPeriodMillis` plus
  latency. That is not drift to be corrected; it is `ADR-0025`'s stated precision.

### 4. What is marked is an action the *server* took, which may be a check

`ActedForAbsentSeat` is emitted for **every** action `foldAbsent` submits on an absent seat's
behalf — the fold facing a bet and the check with nothing owed, identically. Narrowing it to folds
would leave the checked-down case indistinguishable, which is the thing the human's answer rejects.

It is emitted in `foldAbsent` and nowhere else. That single function is the only place a
server-taken action exists, and both routes into it — `Room.act`'s fold-through and
`Room.foldAbsentSeats`' expiry path — are therefore covered by one edit.

Two details are load-bearing:

- **The mark is emitted only if the action was actually applied.** `foldAbsent` already detects a
  no-progress result by comparing the runner before and after; the mark is inserted only when the
  runner moved. A refused submission produces no mark, so a mark never claims an action that did not
  happen.
- **The mark precedes the frames the action produced**, so a client can label the event as it
  renders it and never has to rewrite a rendered log line. Ordering is a courtesy, not the
  mechanism: `(handNumber, actionSequence)` identifies the decision point uniquely — the same
  coordinates a client already echoes in `Act` and already receives in `YourTurn` — so a client that
  buffers or reorders can still attach the mark correctly.

The engine's event log is untouched. `PlayerFolded` and `PlayerChecked` are exactly what they were,
`EVENT_SCHEMA_VERSION` does not move, and a replay of the log is byte-identical to before. Absence
is a fact about the server, and it stays on the wire.

### 5. When each frame is emitted

| Trigger | Frame | To |
| --- | --- | --- |
| `RoomRegistry.disconnect` starts a window | `OpponentPresence(AWAY, disconnectGraceMillis)` | the other seat |
| `RoomRegistry.expireGracePeriods` moves a seat to `absentSeats` | `OpponentPresence(ABSENT, null)` | the other seat |
| `RoomRegistry.resume` clears a seat that was away or absent | `OpponentPresence(PRESENT, null)` | the other seat |
| `RoomRegistry.resume`, always | `presenceOf(otherSeat, now)` | the returning seat |
| `foldAbsent` applies an action for an absent seat | `ActedForAbsentSeat(...)` | both seats |

A presence frame is produced **exactly when there is another seated player to receive it** — a
`WAITING` room has no guest and an `ABANDONED` room has no one left, so both produce none, and both
seats expiring together still abandons the room and sends nothing.

The asymmetry on `resume` is deliberate. The returning client has no state, so it is always given
the opponent's current presence, including `PRESENT`. The client that stayed already has state, so
it is told only about a change — a `resume` on a seat nobody was waiting for emits nothing to the
other seat.

Two plumbing consequences follow, and both are places to get it wrong:

- **`RoomRegistry.disconnect` must return frames.** It gains a return type shaped like `Resumption`
  and `GraceExpiry` — a room plus `outbound` — and its one call site, `DuelSocket`'s `finally`
  block, delivers them **inside the existing `withContext(NonCancellable)`**. Outside it, on the
  most common close path there is, the frame would never be written for exactly the reason the
  comment already there gives for the `disconnect` call itself.
- **`Resumption.outbound` stops being single-seat.** Its contract becomes the one
  `JoinResult.Seated` and `GraceExpiry` already have — the frames this call produced, each addressed
  to the seat it names. `deliver` already routes by seat, so no call site changes; the KDoc and its
  tests do.

`expireGracePeriods` needs no new plumbing at all: `Application.kt`'s ticker already delivers
`GraceExpiry.outbound`, and the presence frame is prepended to it, before the fold it explains.

### 6. Who sees what

**The present player** gets all three transitions and every mark. Their `YourTurn` is not withdrawn
while the duel is paused — no frame is invented to withdraw it — and `DUEL_PAUSED` remains the
refusal; `OpponentPresence` is what turns that refusal from a mystery into a reason.

**The returning player** gets today's resume frames, unchanged in kind, plus the opponent's current
presence. They are **not** told what the server did on their behalf while they were away. The marks
sent during their absence were dropped by `deliver`, and `resumeFrames` replays nothing by design —
a fresh `Snapshot` is the authoritative last word, and re-sending history would restate facts the
snapshot already carries. Telling them would need a per-seat journal of everything done in their
absence: new unbounded server state, and a replay path `STORY-0208` deliberately declined. It stays
strictly addable later, and nothing here forecloses it.

**A spectator** does not exist. `DEC-009` — whether a duel can be watched at all — is the human's
and unanswered, `deliver` routes to two seats and no one else, and `OpponentPresence` is
recipient-relative, so it has no meaning for a party with no opponent. If `DEC-009` ever lands, what
a watcher sees of an absence is that decision's to make, not this one's.

### 7. Nothing here can leak a card

Neither new message carries a `Card`, a `PlayerView`, a `GameEvent`, or any field derived from a
card, and neither is built in transport: `OpponentPresence` comes from `Room`, `ActedForAbsentSeat`
from `foldAbsent`, and `SeatDelivery.deliver` still only decides where a frame goes. A frame about
absence is not a channel for game state — every field of both messages is either routing (`seat`),
a coordinate both seats already hold (`handNumber`, `actionSequence`), a value both seats already
see as `PlayerFolded` or `PlayerChecked` (`action`), or a duration. A fold reveals nothing:
`PlayerFolded` carries no cards and `ADR-0008`'s mucking is untouched. Both types hang off
`ServerMessage`, so `ProtocolPayloadTest`'s descriptor walk covers them the day they are added,
without being told to.

### 8. `PROTOCOL_VERSION` moves, and it is a **second** step, not a shared one

`ADR-0027` moves the version to 3 in `STORY-0405`. This change takes its own step: **whichever of
the two lands first takes 3, and the second takes 4.** When both have landed the wire is at 4.

One number must name exactly one wire shape. `VERSION_MISMATCH` is exact equality and
`protocolJson` sets `ignoreUnknownKeys = false`, so version equality is the *only* compatibility
mechanism the protocol has; a "3" that means sessions on one branch and absence on another is a
number the handshake cannot check. Sharing a step would also make each story's correctness depend on
the other's merge order, which is precisely the trap `ADR-0027` avoided for its migration file by
taking the next free number instead. A bump is a constant, a KDoc line, the version line in
`docs/protocol.md` and one `generateProtocolTypes` run; ambiguity is not that cheap.

`docs/protocol.md` gains a row per new message and a new version line in the same change.
`ProtocolDocumentationTest` fails the build otherwise — it asserts a documented row for every
`ServerMessage` subtype and that the documented version equals `PROTOCOL_VERSION`.

### 9. What does not change

`poker-engine` gains nothing: no clock, no absence, no networking, no event, no schema bump.
`ADR-0023` still chooses the action from the engine's own `legalActions`, and the action still
reaches the engine through the ordinary `act` path as an ordinary action. `DUEL_PAUSED` keeps its
meaning and its wording. `deliver` keeps its one job. `Room` keeps every field it has and gains no
new state.

### 10. What the tests must prove

- A disconnect puts exactly one `OpponentPresence(AWAY, …)` on the opponent's socket, carrying the
  configured window, and none on any other socket; changing `disconnectGraceMillis` changes the
  number with no code change.
- Expiry puts `ABSENT` on the present seat's socket **before** any frame the resulting fold or check
  produced.
- **An `Act` sent after the client's countdown would have reached zero, but before the server's
  sweep has expired the window, is still refused with `DUEL_PAUSED` and moves nothing.** This is the
  authoritative-server rule made executable; it is the single most important test in the set.
- A checked-down absent seat produces `ActedForAbsentSeat` with `action = CHECK`, on virtual time,
  in a spot where `FOLD` is not legal.
- A submission that makes no progress produces no mark.
- A returning player receives the opponent's presence and no replayed `Events`; the seat that stayed
  receives `PRESENT` only when the returning seat had actually been away.
- Both seats expiring abandons the room and sends nothing.
- No test asserts a frame delivered to a seat that has no writer, and the whole suite still runs on
  injected time with no `Thread.sleep`.

## Consequences

**What it buys.** The pause becomes legible: a present player learns their opponent is gone, how
long the server will wait, when the waiting ended, and when they came back — enough to decide
whether to stay, which is what the human asked for. A timeout is no longer a lie of omission: every
action the server took is labelled, check as well as fold, and a client can render "timed out"
without guessing. Presence costs no new state — it is derived from what `Room` already holds, so it
cannot drift out of sync with the thing it describes. And the whole design is additive: three of the
four emission points ride delivery paths that already exist.

**What it costs.**

- **A wire break, spent deliberately while it is free**, and a second version step to coordinate
  with `ADR-0027`.
- **`ADR-0023`'s indistinguishability property is half retracted**, and `TASK-020806` shipped a test
  that asserts it on the wire. That test and `foldAbsent`'s KDoc — *"nothing here constructs a game
  state, an event, or a frame of its own"* — both become false and must be revised in the same
  change. This is a deliberate reversal of a property that was correct when it was written, not a
  defect being repaired.
- **A number counting down in a browser is a permanent invitation to client-side authority.** No
  type prevents it; only the rule in §3 and the test that pins it do. Every future ticket that
  touches the countdown inherits that obligation.
- **The client's countdown will visibly reach zero before anything happens**, by up to a sweep
  period plus latency, and there is a legal `AWAY, 0` frame besides. The client must render that as
  waiting, not as an event, and it will look like a stall to anyone watching closely.
- **Provenance is not in the log**, so a replay reconstructed from the event log — the persistence
  question `DEC-008` still holds open — cannot say which actions were the server's. Recovering that
  later means either the engine learning about absence (a foundational change, a new ADR) or the
  server journalling `(duelId, handNumber, actionSequence, seat)` in its own table. Neither is built
  here.
- **A returning player is told nothing about what happened while they were away** beyond the state
  they come back to. They can infer that something was folded for them from a changed stack, and
  no more.
- **Absence is now visible to the opponent, which is information a player can use.** Knowing a
  seat is absent is knowing it will check when free and fold to any bet — `ADR-0023` already made
  that true, and this makes it knowable. That is the intended consequence of an honest wire, and it
  is recorded here so no one is surprised by it.

**What it forecloses.** Little, and nothing structurally. `SeatPresence` can gain a value, and
`OpponentPresence` a field, at the price of a version step. What it does close off cheaply is the
per-second tick: a client built against a single frame plus a local countdown would ignore a stream
of them, so moving to ticks later means changing both sides — which is the right trade, since ticks
buy nothing a local countdown does not already give. Nothing here touches `ADR-0013`'s window,
forfeiture, or reconnect rules, and a per-action turn clock still fits alongside.

**What stays the human's.** Two things, and neither is answered here or inferable from this ADR.
Whether a returning player is owed a summary of what the server did while they were away is a
product question; this ADR builds none, and building one later changes nothing decided here. And
every word a player actually reads — "away", "waiting", "timed out" — is `EPIC-03`'s copy and the
human's, not a consequence of these type names.

## Alternatives considered

**Repeat the countdown as a frame per second.** The strongest case is real: it needs no client-side
timer at all, it is self-correcting against latency and clock skew, and a client that misses one
frame is fixed by the next — which also solves the reconnect gap for free. Rejected on cost and on
authority: it puts a frame per second per paused room on the wire for a value the client can compute
exactly, it ties the visible cadence to `sweepPeriodMillis` so a server tuning change becomes a
user-visible animation change, and it would make `ADR-0025`'s ticker responsible for fan-out as well
as expiry. The reconnect gap it would have solved is solved properly instead, by carrying presence
on the resume path.

**Send an absolute deadline as wall-clock epoch millis.** The literal reading of "a deadline the
client counts down from", and the shape most protocols use. Rejected because this server has no
wall clock in its timeout path on purpose: `ServerClock` is monotonic precisely so a host time
correction cannot stretch or collapse a window, and emitting an epoch deadline would mean computing
one from a second clock and then trusting the *browser's* clock to be right. A phone with a skewed
clock would render a wrong countdown, or a negative one. A duration is correct on both sides
without either side agreeing what time it is.

**Add the mark to the `Events` frame** — a parallel list of sequence numbers, or a flag on the
frame. Its case: no new message type, no version step of its own, and the mark travels with the very
events it describes, so ordering is impossible to get wrong. Rejected because `Events` carries
whatever `visibleTo` returned and nothing else — its one job is to be a filtered view of the
engine's log, and mixing a server-provenance field into it puts a non-engine fact inside the
projection boundary `docs/architecture.md` is emphatic about. It would also change an existing
message, breaking the wire just as surely.

**Put provenance in the engine: a `GameEvent`, a `MatchEvent`, or a flag on the action.** The
strongest case by far, and it is the only design where a replay can say "the server did this": the
log becomes the whole truth, and no second channel is needed. Rejected because it requires
`poker-engine` to learn what absence is — a networking condition — inside a pure library that must
not know there is a network. It bumps `EVENT_SCHEMA_VERSION`, changes replay and persistence, and
either lets a real player submit an action flagged as the server's or forces the server to
special-case it. It is also the amendment to a foundational rule that is not an architect's to make.

**Three messages — `OpponentAway`, `OpponentTimedOut`, `OpponentReturned`.** Each self-describing,
each carrying only fields that mean something for it, and no nullable field pinned by an `init`
requirement. It was close. Rejected because absence is *state* and this system must re-establish
state on every resume: with three edge-shaped messages, the resume path has to pick one of the three
to replay, which is the same information in a second shape and a second place to get wrong. One
state-shaped frame serves both the transition and the resume, and adding a fourth state later is an
enum value rather than a fourth message and a fourth version step.

**Mark folds only, as the human's words literally say.** Its case is fidelity to the instruction and
a smaller change. Rejected because `ADR-0023` means a timeout is often a *check*, and a design that
labels the fold and stays quiet about the check reproduces the exact dishonesty the human rejected,
one case further in. The instruction is read as what it plainly intends: mark the actions the
server took.

**Carry presence on `PlayerView` inside `Snapshot`.** Tempting: every client already renders
snapshots, so presence would appear everywhere with no new message and no reconnect gap. Rejected
outright — `PlayerView` is the engine's projection of a `GameState`, and the engine has no concept
of a connection. Putting a transport fact into the security-critical redaction type is the one place
in this codebase where a careless field is most expensive.

**Say nothing on resume, and let the returning client infer.** The smallest change, and defensible:
the snapshot is authoritative and the client can assume presence until told otherwise. Rejected
because the assumption is wrong exactly when it matters — a player who reconnects while their
opponent is away would see a normal table, act, and be refused with `DUEL_PAUSED` for reasons
nothing on their screen explains.

**Share `ADR-0027`'s single step to 3.** Its case: one break instead of two, in a window where
nothing is deployed and both changes will land before any client ships. Rejected because it makes
each story's wire correctness depend on the other's merge order, and because exact-equality version
checking cannot distinguish two different wires wearing the same number. Version integers are the
cheapest thing in this protocol to spend.
