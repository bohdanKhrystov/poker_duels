# ADR-0113 — The turn clock is derived state, the wire carries what is left, and the sweep plays the seat

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-120` — **the architect's** — by what mechanism is
  [`ADR-0108`](ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)'s turn
  clock carried, enforced and resumed? Registered open 2026-09-02 by `ADR-0108` §6, which fixes what
  must be true and writes no repair.
- **Implements, and reopens nothing in,
  [`ADR-0108`](ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md).** The
  30 s, the 3 m bank, the conduct on expiry, *an expiry never ends the duel*, the one-clock regime
  and the retirement of the pause are that ADR's and are taken as given here. Where this ADR names a
  behaviour, it is `ADR-0108`'s behaviour with a mechanism under it.
- **Amends, by one clause each:**
  [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) §2 — `graceRemainingMillis` **leaves
  `OpponentPresence`**, and the countdown it fed moves to §1's frame; the rule that made it a
  *duration sent once, never an instant and never a tick* is kept verbatim and re-applied (§2), and
  every rule of §3 stands unchanged.
  [`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md)'s Decision step 2 — the method
  it names, `expireGracePeriods()`, is renamed and its two passes become one (§5); the one period,
  the fixed delay, delay-first, and the independent per-step guards all stand.
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §6's closing
  paragraph — it forecast that *"the day a duel gains a move clock, the frame that starts it must
  not sit in this queue"* and charged a future ADR with an exemption. **The exemption is refused**
  and the debt is discharged a better way: the frame stays in the queue, in FIFO, and the countdown
  is **anchored when the frame arrives and painted when the frame is applied** (§6). Everything else
  in `ADR-0102` stands, `REVEAL_STEP_MS` included.
- **Applies, and reopens none of:** [`ADR-0002`](ADR-0002-server-authoritative.md) (§§1, 6);
  [`ADR-0013`](ADR-0013-disconnect-grace-period.md) (the held seat, the filtered resume, numbers as
  configuration, and *timer tests inject time rather than sleep*);
  [`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md) (the room's own mutex is what
  closes the race — §5);
  [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) (the conduct, reused as
  code — §4); [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) (§9);
  [`ADR-0070`](ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md) (§9);
  [`ADR-0104`](ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) (the new frame is
  an ordinary `Addressed`, so it is room-scoped by `deliver` without a word here).
- **Registers no new `DEC`.** Nothing in this answer needs the product owner or the human: every
  product question the mechanism touches was settled by `ADR-0108` §§1–5, and the words and the
  drawing are already routed to `ADR-0046`'s register and to item 4's card. What this ADR leaves
  unhandsome — a paced runout can spend up to `4 × REVEAL_STEP_MS` of the next decision's 30 s
  (§*Consequences*) — blocks nothing and nobody is working it, and *a `DEC` nobody is working is
  noise in the open table* (`STORY-1211`).
- **Breaks no non-negotiable.** `poker-engine` gains no clock, no field and no action; a timeout
  reaches it as an ordinary `Fold` or `Check` (§4). The server decides every expiry and the client
  asserts nothing (§6). Replay is untouched, and §8 says why in full rather than by assertion.

## Context

**`ADR-0108` fixed a behaviour against a repository that has no clock in it.** No deadline, no
timebank, no countdown and no per-decision timer exists in the engine, the server or the client.
What does exist is a **grace window** built for a different question, and `ADR-0108` retires it: a
per-seat deadline map (`Room.gracePeriods`), a pause that refuses the present player's action
(`Room.isPaused` → `ProtocolError.DUEL_PAUSED`), a remaining-duration field on the wire
(`OpponentPresence.graceRemainingMillis`), a two-pass sweep (`RoomRegistry.expireGracePeriods`), and
thirteen lines of client arithmetic (`presence-countdown.ts`). So this is not a green field: it is a
substitution, and the interesting question at every point is *which of those five pieces is the turn
clock already, wearing the wrong name.*

**What is genuinely in tension, and it is four things.**

**One — a deadline has to be on the wire, and the two sides share no epoch.** `ServerClock` is
`System.nanoTime()`, elapsed from an arbitrary origin, and its own KDoc says why that was chosen:
*"wall-clock time can step backwards when the host corrects its time, which would stretch or collapse
a timeout."* An instant from that clock means nothing in a browser. `ADR-0028` §2 met this exact
force and answered it with a **duration**, computed inside the lock that reads the deadline. Nothing
about the turn clock changes that argument, and re-deciding it would be re-deciding it worse.

**Two — the client already holds a queue that delays frames, and `ADR-0102` §6 saw this coming.**
A hand's ending is painted in steps, and *every* frame behind it is held, FIFO, for up to
`4 × REVEAL_STEP_MS` — 2.4 s at the shipped constant. `ADR-0102` §6 priced what that does to
`graceRemainingMillis` (*"a queued one anchors its deadline up to 2.4 s late and over-states the
grace left by that much"*), decided it was tolerable because *"the countdown decides nothing
anyway"*, and then wrote the debt down explicitly: a move clock's frame *"must not sit in this
queue"*. That instruction, taken literally, produces a screen painting *29… 28…* for a decision on a
hand the player cannot see yet, while the previous hand's river is still coming out — which is the
one thing `ADR-0102` §6 spent its whole argument forbidding: *"what would be new is a screen that
says something the server did not."* The exemption and the principle that motivated it point in
opposite directions, and something has to give.

**Three — the deadline and the act race, and the loser must not move the duel.** A player acting at
29.8 s and a sweep firing at 30.0 s want the same decision point. Every mechanism that *arms* a
timer has to find and cancel it on the act, on the reconnect, on the rematch and on the reap —
which is precisely the per-room task lifecycle
[`ADR-0016`](ADR-0016-a-room-is-serialised-by-its-own-mutex.md) declined and
[`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md) rejected by name (*"every
reconnect, reap and rematch must find and cancel the right timer"*). A mechanism that *derives* the
deadline has nothing to cancel. The two are not equally cheap to get wrong.

**Four — a timeout is one step away from the coin, and there is exactly one path to it.**
`RoomRegistry.act` is the only method that claims a finishing duel in `recording`, calls
`sink.record` outside the lock, and gives the claim back if that throws. A timeout that ends a hand
can end a duel. Any expiry that reaches the engine by a second route is a second, untested path to
the one irreversible write this product makes — and `ADR-0108` §3's *"the coin's single settle path
is untouched by this ADR"* is a promise the mechanism has to keep, not merely not contradict.

**The deadline on deciding.** `EPIC-13` item 4 cannot be split without this, and it is the epic's
only wire move, so `ADR-0047`'s one-bumping-branch lock serialises it against everything else that
would move `PROTOCOL_VERSION`. The lock is free while the ledger is quiet, which it is today.

## Decision

### 1. One new `ServerMessage`, because nothing that exists can carry it

`ServerMessage` gains one variant. Every existing frame is disqualified by something structural,
not by taste:

- **`Snapshot` carries a `PlayerView` and nothing else.** `PlayerView` is a `poker-engine` type;
  putting a deadline in it puts a clock in the engine. That is the non-negotiable, so `Snapshot` is
  out before preference enters.
- **`YourTurn` reaches one seat.** The countdown is *"visible to both players"* (`ADR-0108` §5), and
  the rival's is the answer to *"how long will I wait?"*.
- **`Events` carries `GameEvent`s** — the engine's hierarchy, same objection as `Snapshot`.
- **`OpponentPresence` is per-recipient and about the rival alone**, so a player's own bank would
  have no frame at all.

```kotlin
@Serializable
@SerialName("TurnClock")
public data class TurnClock(
    val seat: Int,
    val handNumber: Int,
    val actionSequence: Int,
    val turnRemainingMillis: Long,
    val bankRemainingMillis: List<Long>,
) : ServerMessage
```

`init` requires `seat in 0..1`, `turnRemainingMillis >= 0`, `bankRemainingMillis.size == 2`, and
every bank `>= 0` — the same shape of construction-time refusal `OpponentPresence` and
`ActedForAbsent` already carry.

- **It names its seat**, because it is sent to **both** seats identically. That is `ADR-0028` §1's
  rule and its worked example: `ActedForAbsent` names a seat, `OpponentPresence` does not.
- **`handNumber` and `actionSequence` say which decision this clock is for**, exactly as `YourTurn`
  repeats them. A client can then discard a clock for a decision it has already seen closed, and the
  clock is bound to a decision rather than floating.
- **`bankRemainingMillis` is indexed by seat, both entries, every frame.** `ADR-0108` §5 makes both
  banks public facts of the table; a client that had to accumulate its rival's bank from frames sent
  while the rival was on turn would have nothing to draw for a resuming player until the rival's
  next decision. Two numbers, always stated, never accumulated.
- **It carries no card, no stack and no pot**, so it is a projection of *room* state and not of
  `GameState`. The engine's projection layer is neither used nor bypassed: there is nothing in this
  frame to filter, and the hole-card rule is untouched.

**When it is sent: one frame per room write-back that changes the live decision point, to both
seats, last in the batch** — after the `Events`, the `Snapshot` and the `YourTurn` that describe the
decision it clocks, so the client never holds a clock for a decision it has not been told about.
A batch that plays three absent seats through carries **one** clock frame, for the decision the duel
came to rest on, not one per intermediate decision. A resuming seat is sent one of its own, in the
same critical section that already builds its presence frames from the same `now`
(`RoomRegistry.resume`) — which is `ADR-0108` §6's *"a resuming client is told the live deadline
again"*, discharged at a seam that already exists.

### 2. The wire states what is **left**, never an instant

`turnRemainingMillis` and `bankRemainingMillis` are what remained **at the instant the server built
the frame**, both clamped at zero. This is `ADR-0028` §2 re-applied without amendment, for its own
reason: the two sides share no epoch, the server's only duration clock is monotonic from an
arbitrary origin, and wall clock is the thing `ServerClock` exists to keep out of timeout code. A
duration needs no agreed epoch and no correct client clock — only that each side can measure its own
elapsed time.

`turnRemainingMillis == 0` with a bank remaining is a real and ordinary frame: it means the fresh
allowance is spent and the bank is running. Both at zero is also real: the allowance is gone and the
sweep has not landed yet. Neither is an error, and neither is an event.

**No `ClientMessage` gains a timestamp, a deadline or a remaining-millis field, now or later**
(`ADR-0028` §3, restated because it is the rule most likely to be coded wrong under a visible
clock). There is nothing on the wire the server would consult, and adding one would be a client
asserting a game fact.

### 3. The clock is derived from the decision point, and is never armed

`Room` gains **one nullable value and one map**, and loses `gracePeriods`:

```kotlin
public data class TurnDeadline(
    val seat: Int,
    val handNumber: Int,
    val actionSequence: Int,
    val bankBeginsAt: Long,   // the instant the fresh 30 s ends and the bank starts spending
    val expiresAt: Long,      // the instant the whole allowance is gone
)
```

on `Room` as `turnDeadline: TurnDeadline?` (`null` exactly when there is no live decision point),
alongside `timebankRemainingMillis: Map<Int, Long>`. Both instants are **absolute, on the server's
monotonic clock** — never a remaining duration, for the reason `Room.gracePeriods` already gives in
its own KDoc: *"a value that has to be decremented is a value someone has to remember to
decrement."* `Room` still reads no clock; it is handed `now`, as `join`, `finish` and `presenceOf`
already are.

**Two expressions are the whole arithmetic, and they are the same expression twice.**

- What is left of the bank at `now`, for the seat on turn — used both to build the frame and to
  debit the bank when the decision closes:
  `(expiresAt - maxOf(now, bankBeginsAt)).coerceAtLeast(0)`
- What is left of the fresh allowance at `now`: `(bankBeginsAt - now).coerceAtLeast(0)`

**One rule places them.** At every write-back that changes the room's live decision point —
`RoomRegistry.act`'s, and `withFreshRunner`'s for a join or an agreed rematch — the room passes
through one pure method, `Room.clocked(runner, now, turnMillis)`, which:

1. **debits** `timebankRemainingMillis[turnDeadline.seat]` to the first expression above, for the
   decision that just closed — so a seat that answered inside its 30 s spends nothing, one that
   answered 5 s into the bank spends exactly 5 s, and one whose decision was given up spends the
   rest;
2. **restarts** the clock when the live decision point differs from the one `turnDeadline` names:
   `bankBeginsAt = now + turnMillis`, `expiresAt = bankBeginsAt + timebankRemainingMillis[seatToAct]`;
3. sets `turnDeadline = null` when the runner has no live decision point.

A **fresh runner** (a join, an agreed rematch) refills both banks to the configured full bank and
clears `turnDeadline`: *"a rematch is a new duel and a fresh bank"* (`ADR-0108` §1).

**Nothing is armed, scheduled or cancelled anywhere.** The deadline is a pair of numbers derived
from the decision point; when the decision point moves, the previous deadline stops existing rather
than being cancelled. That is the property §5's race rests on, and it is the reason `Room` needs no
lifecycle.

The two numbers stay configuration and stay in `RoomTimeouts`, which loses `disconnectGraceMillis`
and gains `turnMillis` (default `30_000`) and `timebankMillis` (default `180_000`), both required
positive. `ServerConfig` swaps `duel.disconnectGraceMillis` / `DISCONNECT_GRACE_MILLIS` for
`duel.turnMillis` / `TURN_MILLIS` and `duel.timebankMillis` / `TIMEBANK_MILLIS`, read once at
startup like every other tunable. The registry computes both instants and the room stores them —
`RoomRegistry.disconnect`'s existing rule, *"the deadline is computed here, and nowhere else"*,
inherited with the machinery.

### 4. An expiry is a synthesised act down the ordinary path, and it reuses `ADR-0023`'s code, not its wording

**No new room event, no new engine action, no second route to the sink.** An expiry produces exactly
what a socket-less seat's turn already produces today.

`AbsentSeats.kt`'s `foldAbsent` loop already does, for one seat, every single thing `ADR-0108` §2
asks for: it reads the engine's `legalActions` at the decision point, takes `Fold` when `FOLD` is in
the set and `Check` otherwise, builds *the same `Act` frame that seat's own client would have had to
send*, hands it to `duel/act` — same guard, same engine call, same `advance` — and prepends a
`ServerMessage.ActedForAbsent` naming the seat, the decision point and the action, addressed to
**both** seats. That single-seat body is **extracted** as `giveUpDecision(runner, seat, seeds)` and
`foldAbsent` calls it from its loop, unchanged in behaviour. `ADR-0108` §2's *"marked to both seats
as an act the server took"* is then true by construction rather than by a second implementation that
has to agree with the first.

The expiry path is `foldAbsent(giveUpDecision(runner, timedOutSeat, seeds), absentSeats, seeds)` —
the exact parallel of `Room.act`'s `foldAbsent(advanceDuel(…), absentSeats, seeds)`. The timed-out
seat is **not** put into `foldAbsent`'s absent set: it gives up one decision and, if it is connected,
its next decision starts a fresh 30 s. *An expiry costs the seat one decision* (`ADR-0108` §3) is
enforced by which argument the seat is passed as, which is the cheapest possible place to enforce
it.

**Whether the seat is latched `ABSENT` is decided by presence, not by the clock.** In the same
write-back: a seat that timed out **while away** moves into `absentSeats` and is thereafter played
without delay at every decision the turn brings it, until it reconnects — today's post-window
behaviour, verbatim, and `foldAbsent` is already the code that does it. A seat that timed out
**while connected** latches nothing and is never `ABSENT`, per `ADR-0108` §4's table. That is the
whole of *"a reconnect is a fact the server can see; attention is not"*, expressed as one condition.

**The both-gone abandon is checked before the give-up, not after**, and is otherwise unchanged: a
room whose give-up would leave both seats `ABSENT` is abandoned instead — no outcome, nothing
recorded — rather than played out by the server against two empty chairs.

### 5. The scheduler is the ticker that already exists, and the race is closed by the mutex and the sequence guard

`ADR-0025`'s single ticker on the application scope keeps **two** steps and one period.
`expireGracePeriods()` is not joined by a clock sweep — it is **replaced** by
`expireTurnClocks()`, and `GraceExpiry` becomes `TurnClockExpiry(room, outbound)`, the same shape.
The ticker's structure — delay first, expiry then reap, each guarded independently against every
`Throwable` but `CancellationException` — is untouched.

**One pass, where there were two.** The unlocked pre-check is `room.turnDeadline != null &&
(now >= room.turnDeadline.expiresAt || seatToAct in room.absentSeats)`, re-decided under the room's
own lock; a candidate then goes through `act(code) { it.giveUpTurn(now, handSeeds) }`, which is
`Room.foldAbsentSeats`' successor, widened from *the seat on turn is absent* to *the seat on turn is
out of time **or** absent*. Going through `act` and never writing back directly is not an
implementation detail: `act` is the one place that claims a finishing duel in `recording`, hands it
to `DuelResultSink` outside the lock, and unclaims it if that throws. **A duel that ends on a
timeout reaches the coin by the same single path a played one does**, which is `ADR-0108` §3's
promise kept mechanically.

`now` is read **once** at the top of the pass and every room is judged against it, so the enforced
expiry can only ever **trail** the stated deadline — by up to `sweepPeriodMillis` plus the previous
pass's duration, about a second at defaults — and can never precede it. `ADR-0108` §6's *"may never
precede it"* is a consequence of the comparison, not a hope about it.

**The race, and why nothing needs cancelling.** Both the player's `Act` and the sweep's give-up go
through `RoomRegistry.act`, so both take that room's own mutex (`ADR-0016`) and one strictly precedes
the other:

| Who wins the mutex | What happens to the loser |
| --- | --- |
| **The player's act** | It moves the duel, so the write-back's `Room.clocked` restarts the clock for the **new** decision point. The sweep then takes the lock, recomputes, finds `now < expiresAt`, and expires nothing. There is no pending timeout to cancel, because none was ever armed. |
| **The sweep's give-up** | It moves the duel and `actionSequence` advances. The player's `Act`, carrying the decision point it was offered, is refused by the engine's own `guard` inside `duel/act` and comes back `Rejected` — the ordinary treatment of any stale frame, in the ordinary place. The player also receives the `ActedForAbsent` explaining what happened instead. |

Either way the decision point is closed **exactly once**, by the ordinary act path, with no
compensation, no rollback and no timer handle in the system.

**A late act inside the sweep's resolution is honoured. The act path never refuses an act for
lateness, and gains no deadline check at all.** One place decides expiry — the sweep — so there is
no second decider to disagree with it, and the clamp in §3's debit expression means a decision
closed after `expiresAt` simply spends the bank to zero. The effective allowance is therefore
`turnMillis` + bank + up to one sweep period, which is exactly the trailing `ADR-0108` §6 licensed.

### 6. What the client does between frames: anchor on arrival, paint on apply, recompute never decrement

**The client owns the ticks and states nothing.** `ADR-0102`'s licence transfers in full — *"the
client may choose when to paint a fact the server sent; it may never choose what the fact is"* — and
a countdown is the smaller case, because the values painted are two numbers the server sent minus
locally-measured elapsed time, which is what every countdown on a network already is.

**The anchor is taken when the frame arrives; the countdown is painted when the frame is applied.**
This is the whole of the amendment to `ADR-0102` §6, and it is one line at the socket seam: the
store stamps a `TurnClock` with its own elapsed-time reading at arrival, and derives
`turnEndsAt = anchor + turnRemainingMillis` and
`expiresAt = turnEndsAt + bankRemainingMillis[seat]` from it. The frame then queues behind a paced
runout in **FIFO with every other frame**, exactly as `ADR-0102` §1 requires, and is painted when
its turn comes — by which time the countdown already reads the reduced number rather than
restarting at thirty.

The exemption `ADR-0102` §6 anticipated is refused because it would paint a countdown for a decision
the player cannot yet see, on a table still running out the previous hand — a screen saying
something the server did not, which is the exact failure §6 exists to prevent. Anchoring is not
painting: nothing reaches the screen early, no control is offered early, and the queue's FIFO
property — *"reordering by frame type is precisely what turns a lag into a contradiction"* — is left
intact.

**The clock source is monotonic and injected.** The countdown reads `performance.now()`, not
`Date.now()`, for the reason `ServerClock`'s own KDoc gives — a host time correction must not
stretch or collapse a countdown — and it reaches the store as a parameter at
`web-client/src/store/boot.ts`, the seam `ADR-0102` §4 already established for `REVEAL_STEP_MS`. So
timer tests state time instead of sleeping, which is `ADR-0013`'s rule inherited on the client side.

**Every tick recomputes from the anchored deadline; nothing is decremented.** `secondsRemaining
(deadline, now)` is re-evaluated against a fresh reading each second, so a throttled background tab,
a slow frame or a missed interval costs an update, never accuracy — the number is right again the
moment the tab is. This is `presence-countdown.ts`'s existing arithmetic, unchanged, and it is why
§7 keeps the function.

**Drift, stated as a bound rather than corrected.** The client's countdown differs from the
server's remaining by (frame latency at anchoring) − (elapsed since). It is re-anchored at every
decision point, so error never accumulates across decisions; within one decision it is bounded by
one network trip. **No correction mechanism exists and none is added**, because a correction needs
the client to tell the server something about time, and §2 forbids that.

**A countdown whose deadline has passed and whose expiry frame has not arrived holds at zero and
does nothing.** `ADR-0028` §3, re-applied word for word to a wider occasion, and `ADR-0108` §5's
*zero is not an event*: reaching zero enables no control, sends nothing, marks no hand lost, assumes
no act. The seat reads as *expired* — one of the four states item 4's card owes — until a server
frame carries the consequence, and how that looks is the card's, not this ADR's. **The screen never
invents the act, and it never argues with the frame that carries it**: a server act arriving while
the countdown still shows time left is applied on arrival like every other frame.

**Which treatment is drawn is chosen from the two numbers the server stated** —
`now < turnEndsAt` is the fresh allowance, `turnEndsAt <= now < expiresAt` is *on timebank* — so the
distinction `ADR-0108` §5 requires costs no third field and no client-computed game fact. It is a
comparison of two server-stated numbers to pick a style, which is the same class of act as
`secondsRemaining` itself.

### 7. The grace machinery, dismantled and reused, piece by piece

| Today | Becomes |
| --- | --- |
| `RoomTimeouts.disconnectGraceMillis` (60 s) | **gone**; `turnMillis` and `timebankMillis` (§3) |
| `Room.gracePeriods: Map<Int, Long>` | **gone**; a seat with a dropped socket is in `awaySeats: Set<Int>`, and the deadline it held moves to the room's single `turnDeadline` |
| `Room.absentSeats` | **kept, verbatim**, with its meaning intact: latched on a timeout taken while away, cleared by reconnect, played without delay by `foldAbsent` |
| `Room.isPaused` and `ProtocolError.DUEL_PAUSED` | **both deleted.** The duel never pauses, so the refusal has no occasion. The enum entry goes with it: a closed set that is branched on exhaustively must not carry a value no server can send, and `ServerMessageHandshakeTest.theErrorSetIsExactlyWhatIsDeclared` makes its removal a deliberate, visible act |
| `Room.disconnect(seat, deadline)` | `Room.disconnect(seat)` — it marks the seat away and starts nothing |
| `Room.expireGrace(now)` | **gone**; the latch happens in the expiry write-back (§4) |
| `Room.presenceOf(seat, now)` | `Room.presenceOf(seat)` — presence stops being time-derived and becomes a lookup: `ABSENT` if latched, `AWAY` if the socket is down, else `PRESENT` |
| `OpponentPresence.graceRemainingMillis` | **gone from the frame** (§1); `SeatPresence`'s three values stay, and only the KDoc that says a grace period is running and the duel is paused changes |
| `Room.foldAbsentSeats(seeds)` | `Room.giveUpTurn(now, seeds)`, widened (§5) |
| `RoomRegistry.expireGracePeriods()` / `GraceExpiry` | `expireTurnClocks()` / `TurnClockExpiry`, one pass (§5) |
| `AbsentSeats.foldAbsent` | **kept, verbatim**, with its single-seat body extracted so §4 can call it |
| `web-client/src/table/presence-countdown.ts` | **kept, and renamed** `countdown.ts` with its test — `secondsRemaining`'s body does not change, and its citation re-points from `ADR-0028` §3 to `ADR-0108` §5, the same rule at a wider occasion |
| `PresenceNotice.tsx`'s countdown | **gone from that component**; the notice keeps `presenceLine` and the seconds move to the clock the table draws |

### 8. Why a wall-clock deadline does not break replay — because there is no wall clock, and no clock in the replay

Four facts, in order, and each is checkable:

1. **The engine never sees a clock, a deadline or a timeout.** It receives `PlayerAction`s. A
   timed-out decision arrives as an ordinary `Fold` or `Check` through the same `duel/act` call a
   played one takes, and `AbsentSeats.kt`'s own KDoc already states the resulting property: *"a turn
   given up for absence is, in the log, indistinguishable from one a player acted on themselves."*
2. **The determinism promise is over actions, not over how they were chosen.** *Same seed + same
   actions ⇒ byte-identical game.* Time is an input to the server's choice of **which action to
   submit and when** — exactly as a player's click is — and never an input to what an action *does*.
   Replaying a recorded action sequence against the same seed reproduces the game byte for byte with
   no clock consulted anywhere, because none is reachable from the replay.
3. **Nothing time-derived enters `MatchLog`, `GameState` or any event.** `turnDeadline` and
   `timebankRemainingMillis` are room state; room state is not replay input, is not persisted with
   the duel, and does not reach the sink.
4. **The server's clock is monotonic and injected, and it is not a wall clock.** `ServerClock` is
   `System.nanoTime()`-based by construction and its KDoc forbids stamping a row with it. Tests
   drive `MutableClock` and never sleep — `ADR-0013`'s rule, and the existing `GraceExpiryTest`'s
   practice, inherited with the machinery.

### 9. The bump, the ticket, and the one seam it may be split along

**It moves the wire**, so `PROTOCOL_VERSION` steps under `ADR-0047`'s one-bumping-branch lock, and
**this ADR names neither the number nor the fingerprint** — §6 of that ADR forbids it: *"the number
is still never written down in advance, and neither is the fingerprint: no ADR, story or ticket
names either."* The bump ticket rebases on `develop`, moves the constant, regenerates, runs the
ledger test **expecting red**, and pastes the fingerprint the failure hands it into one
hand-written `docs/protocol-versions.md` row.

**The implementing ticket is `atomic:` and its `## Files` table is the output of one green probe
run** of the CI gate set (`ADR-0070` §§1–2), never a list recited from anywhere — this ADR
publishes no file list and no count, and `ADR-0070` §5's *"seventeen appears in that ticket and
nowhere else"* is why. The probe's stub edit is every declaration this ADR adds, removes or
re-values (`ADR-0070` §3): the new `ServerMessage` variant **with its five fields**, the removal of
`OpponentPresence.graceRemainingMillis`, the removal of the `ProtocolError.DUEL_PAUSED` **enum
entry**, and the moved constant.

**One artifact the probe cannot find, named here so it is not dropped:** `docs/test-plan.md`'s
`CORE-23` asserts that an action under *The duel is paused.* is refused with `DUEL_PAUSED`. No test
reads that document, so no gate will fail for it. The ticket that lands this owes it a revision, and
`CORE-18`/`CORE-19` — the away marking — survive untouched.

**The split.** Item 4 is three tickets in this order, and the epic's own rule already fixes the
first: the **card** (`ADR-0091` §2, drawing all four states), then **one atomic ticket** carrying
the wire, the server's clock and sweep, the dismantle of §7 and the client *storing* the frame, then
**one `web-client` ticket** in which the table draws the countdown. That is the only seam, and it is
`ADR-0102` §8's precedent: the half that lands first leaves the client holding the clock and drawing
nothing — which is today's screen exactly — while the server's enforcement is complete, so nothing
half-clocked reaches `develop`. Both halves land before the story closes.

### 10. What a test must prove

Not a list of files; a list of claims that must fail if the code is wrong. All of these run on
`MutableClock` and none sleeps.

1. **The race, both directions, with a count.** An act applied at `expiresAt − 1` moves the duel and
   the next sweep expires nothing; a sweep's give-up applied first makes the player's `Act` carrying
   the offered decision point come back `Rejected`. In both, **exactly one** action is recorded at
   that decision point.
2. **The bank is spent by arithmetic, not by decree.** A decision closed 5 000 ms into the bank
   debits exactly 5 000 from that seat and **nothing** from the rival's — asserted with two
   different seats and two different overruns, because one seat and one value cannot tell a debit
   from a constant.
3. **A connected seat is never `ABSENT`.** After an expiry with the socket up, presence stays
   `PRESENT` and the next decision starts a full fresh `turnMillis` with a zero bank.
4. **An away seat's first expiry latches, and the second decision is not waited for.** The give-up
   of the following decision happens at the same `now`, with no deadline in between.
5. **An expiry never ends the duel.** A duel driven entirely by expiries reaches an outcome by the
   blind schedule and settles through the same single `sink.record` path, with no forfeit anywhere.
6. **The anchor is taken on arrival, not on apply.** A clock frame queued behind a paced runout is
   painted with a countdown already reduced by the queue's dwell — asserted at two different dwells,
   because one cannot distinguish an anchor from a constant.
7. **Zero is not an event.** At zero, no control changes state, nothing is sent, and the view does
   not move until a server frame arrives — the existing `ADR-0028` §3 assertion, re-pointed and
   kept.

## Consequences

**What it buys.**

- **There is nothing to cancel, anywhere.** No timer handle, no per-room task, no reconnect hook, no
  rematch cleanup. The deadline is two numbers derived from the decision point, so the entire class
  of bug in which a stale timer fires on a duel that has moved on is unreachable rather than
  guarded against. This is the reason to prefer derivation over arming even where arming is more
  precise.
- **The expiry path is the act path, so the coin's single settle path survives its first clock.**
  A duel that ends on a timeout goes through the same `RoomRegistry.act` claim/record/unclaim
  sequence as a played one, and `ADR-0108` §3 is kept by construction rather than by care.
- **Five pieces of machinery are re-used rather than re-implemented**, and each carries its merged
  tests forward: `foldAbsent`'s conduct, `absentSeats`' played-instantly semantics, the ticker's
  shape, `ADR-0028` §2's duration-not-instant rule, and `secondsRemaining`'s arithmetic. What is
  genuinely new is one wire frame, two room fields and one client anchor.
- **The sweep gets simpler, not more complex.** `expireGracePeriods`' two-pass structure — the one
  whose KDoc spends a paragraph explaining why a first pass that commits and a second that can throw
  must isolate every room — collapses into one pass through `act`. That is a page of reasoning
  deleted, not relocated.
- **Presence stops being time-derived.** `presenceOf` loses its `now`, and `AWAY`/`ABSENT` become a
  lookup. One clock in the system, in one place.

**What it costs.**

- **A paced runout can spend up to `4 × REVEAL_STEP_MS` — 2.4 s today — of the next decision's
  30 s.** The server's clock starts when the decision point opens; the player sees it up to a full
  queue drain later. Anchoring on arrival makes the **screen** honest about this, and does not give
  the time back: the player genuinely has ~27.6 s of thinking time after the worst-case runout. It
  is named, accepted, and the escape is known if it ever bites — exempt the frame from the queue and
  accept the contradiction, or shorten the queue, neither of which this ADR forecloses.
- **The frame is one more thing to send at every decision point** — one `TurnClock` per seat per
  decision, on top of the `Snapshot` and `YourTurn` already there. That is a measurable increase in
  frame count in a product whose duels are two players long, and it is the price of not ticking
  per-second.
- **`bankRemainingMillis` is a two-element list, and a list used as a tuple is a smell.** Its arity
  and its per-seat meaning live in an `init` block and a KDoc rather than in the type, and the
  generated TypeScript is `number[]`. The alternatives were worse (§*Alternatives*), but a reader
  who indexes it by anything other than a seat number will not be stopped by the compiler.
- **A player can act up to one sweep period after their deadline and be honoured.** The stated
  30 s is a floor, not a ceiling — and the ceiling is `sweepPeriodMillis` higher. Tightening it
  means either a second decider in the act path or a shorter period for both sweeps.
- **`ADR-0102` §6's forecast is contradicted rather than fulfilled**, which costs a paragraph of
  explanation at the top of this file and leaves a merged ADR whose last paragraph now reads
  wrongly on its own. That is the standard price of amending an immutable ADR.
- **`DEC-108`'s subject leaves the screen.** *"May the action bar stay enabled while the table says
  the duel is paused?"* is asked about a sentence this ADR deletes. `DEC-108` is **not** answered,
  struck or restated here — `ADR-0108` left it open deliberately and it is the product owner's —
  but whoever reaches it will find its ground gone.
- **Every wire-shape change costs a bump with no escape hatch**, so this serialises against anything
  else touching `ClientMessage`/`ServerMessage` for as long as its branch is open (`ADR-0047`).

**What it forecloses.**

- **A per-deadline timer.** Sub-sweep-period precision now needs a superseding ADR, exactly as
  `ADR-0025` said it would. Nothing here makes that harder than it already was; the derived deadline
  is in fact what such an ADR would arm.
- **A server that consults the client about time.** §2 closes it permanently: there is no field to
  add it to and no reason a correct mechanism would want one.
- **A clock that starts when the player can see the decision.** The server cannot observe that, and
  letting the client say so is a client asserting a game fact. So the runout's 2.4 s is not
  recoverable by any mechanism this repository's non-negotiables permit — which is a reason to keep
  `REVEAL_STEP_MS` small, and a reason it is a constant in one file.
- **A `DUEL_PAUSED` refusal.** Reinstating a pause would need the enum entry back, which is a wire
  bump and a reversal of `ADR-0108` rather than of this ADR.

**Why this shape, on thin evidence.** There are no players and no deployed client, so every piece
chosen here is one already merged and already argued for, with the smallest new commitment on top:
one frame, two room fields, one anchor. Where the evidence was genuinely thin — the frame's field
list, the two configured numbers, the sweep's period — the choice is the one that is cheapest to
reverse: a field added to a frame that already exists is a bump and nothing else, both numbers are
configuration by `ADR-0108` §1, and the period is one key shared with the reap. Where the evidence
was **not** thin — that arming timers is expensive to get right, and that a second path to the sink
is dangerous — the choice follows the two merged ADRs that already paid for that knowledge.

## Alternatives considered

**A field on `YourTurn`, and no new frame.** By far the cheapest thing on the wire: `YourTurn`
already carries `handNumber` and `actionSequence`, already arrives exactly once per decision point,
and already reaches the seat that has to act — so a `deadlineMillis` beside `legalActions` would
need no new type, no new client case and no new delivery seam. Rejected because `YourTurn` reaches
**one** seat, and `ADR-0108` §5 requires one countdown *"visible to both players"*: the rival's clock
is the answer to *"how long will I wait?"*, and it is the whole reason the pause could be retired
without leaving the present player in the dark. Sending `YourTurn` to a seat whose turn it is not
would be worse than a new frame — it would make the one message a client keys its action bar on
ambiguous about whose turn it is.

**A timer armed per deadline — a coroutine per decision point.** Exact expiry, no scanning, no
`sweepPeriodMillis` slack, and a player's 30 s would mean 30 s rather than 30 s plus up to a second.
It is also the mechanism most poker servers actually use. Rejected on the record: `ADR-0025` weighed
this exact option and refused it because it *"resurrects the per-room task lifecycle `ADR-0016`
declined — every reconnect, reap and rematch must find and cancel the right timer"*, and a turn
clock arms and cancels one **per decision** rather than per disconnect, multiplying that surface by
every act in a duel. The precision it buys is invisible at these timescales — a second on thirty —
and the failure it risks is a stale timer folding a seat that has already acted, which is the one
class of bug that costs a duel and cannot be recovered.

**A new room event — a `SeatTimedOut` frame the server emits instead of an act.** The most honest
naming: an expiry *is* a distinct thing that happened, and a frame that says so needs no reader to
infer it from an `ActedForAbsent`. It would also let the client distinguish a timeout from a
disconnect-driven give-up without comparing presence. Rejected because it invents a second way for
the server to move a duel, and every one of `ADR-0023`'s guarantees would have to be re-established
on that path: the legal-set read, the `guard`, the `advance` at the hand boundary, the projection
per seat, and — the sharp one — `RoomRegistry.act`'s claim/record/unclaim around the sink. `ADR-0108`
§3 promises the coin's single settle path is untouched; a second route to the engine is exactly how
that promise gets broken by accident later. And the naming argument is thinner than it looks:
`ADR-0108` §2 requires a timeout to be marked as *the server's act*, which is what `ActedForAbsent`
already is.

**An absolute deadline on the wire, on a shared epoch.** It removes the anchoring problem outright —
no arrival stamp, no queue interaction, no `ADR-0102` §6 debt to discharge, and a resuming client
computes its countdown from the same number as everyone else. Rejected for `ADR-0028` §2's reason,
which has not changed: the server's duration clock is monotonic from an arbitrary origin, so the
only shareable epoch is wall clock — the thing `ServerClock` was written to keep out of timeout
code — and it would make a duel's fairness depend on the correctness of a browser's system clock.
Sending both a monotonic instant and a wall-clock one, so the client could pick, is worse: two
epochs, one of them wrong, and a client choosing between them.

**Carry only the on-turn seat's bank, and let the client accumulate the rival's.** Smaller frame,
no two-element list, no arity to police — and correct almost always, because a bank only spends
while its seat is on turn, so a remembered value is a current value. Rejected on the resume case,
which is not an edge: a client that reloads mid-duel has no memory, and would have nothing to draw
for its rival's bank until the rival's next decision — a hole in `ADR-0108` §5's *"both banks are
public facts of the table"* that appears exactly when a player is most confused about what happened.
Every state a client can be in should be reachable from one frame, and this is what that costs.

**Keep `graceRemainingMillis` and let it mean the turn clock.** No wire change at all for the
countdown half, `presence-countdown.ts` and `PresenceNotice` untouched, and the bump reduced to
whatever else moves. Rejected because the field is per-recipient and about the rival only, so a
player would have no frame stating their own clock; because it is present *exactly when presence is
`AWAY`*, enforced by an `init` block, and the turn clock runs for a connected seat; and because a
field whose name says *grace* and whose meaning is *turn clock* is the kind of saving that costs a
future reader an hour. `ADR-0028` §8 forces a bump for the rest of this change regardless, so the
saving was never real.

**Exempt the clock frame from `ADR-0102`'s queue, as `ADR-0102` §6 instructed.** Its case is the
strongest here, because it is a merged instruction and following it needs no amendment: the
countdown would be exact, anchored at arrival and painted at arrival, with no dwell to reason about.
Rejected because it paints a live countdown for a decision on a hand the player cannot see, over a
table still running out the previous one — the screen saying something the server did not, which is
the failure `ADR-0102` §6's own principle exists to prevent, and which it would have seen had it
been considering a frame that *draws* rather than one that merely informs. Anchor-on-arrival gets
everything the exemption was for — an accurate number — without reordering a single painted frame,
so the instruction is refused on its own reasoning rather than against it.
