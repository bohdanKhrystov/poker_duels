# ADR-0108 — An expiry plays the seat, never the duel, and the timebank replaces the grace window

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-115` — what happens when a player's clock runs out, and how does the
  **timebank** meet [`ADR-0013`](ADR-0013-disconnect-grace-period.md)'s **disconnect grace
  window**? Raised 2026-09-02 by the human, registered by
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md), and gating its item 4 — the largest
  item in the epic.
- **Where the answer came from — three parts, each with its source named.** (1) **The numbers and
  the bank's reach are the human's verbatim** — *"player shoud have 30seconds for move + 3m
  timebank; timebank also work for disconnection case; clock shoud visibly change each second"* —
  recorded here, not chosen. (2) **The expiry conduct is
  [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) applied**, not re-decided:
  a clock expiry and a grace expiry are the same situation — a seat that did not act, with the
  turn on it — and the merged conduct for it reads the act from the engine's legal set. (3) **The
  never-the-duel half is derived from `docs/vision.md`'s *What it is***: *"One duel coin per win.
  Not chips, not currency, not a balance. A counter of duels won."* and *"A duel is a match, not a
  hand."* — the same derivation
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) made for a coin
  moved by a click, endorsed by the human on 2026-09-02 when a resign was offered and declined
  *"not now"*. The one-clock shape of §4 follows *Positioning* — *"Dark, quiet, fast, minimal"* —
  the sentence one countdown satisfies and two racing countdowns do not.
- **Amends:** [`ADR-0013`](ADR-0013-disconnect-grace-period.md) — the fixed disconnect window is
  replaced by the turn clock; the held seat, the projection-filtered resume, the `ADR-0023`
  conduct and the numbers-are-configuration rule all stand. And, by one clause each:
  [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) and
  [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) — `AWAY` stops pausing the
  duel, so `DUEL_PAUSED`'s occasion and *"The duel is paused."* go when the clock lands. Every
  other clause of both stands: the away fact is still told, every act the server takes is still
  marked, and the words *Away* and *Timed out* keep their seats.
- **Applies, and reopens none of:** [`ADR-0002`](ADR-0002-server-authoritative.md) (the server
  decides the timeout; a client asserts no game fact);
  [`ADR-0014`](ADR-0014-duel-coin-economy.md) (a coin moves only on the outcome);
  [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) (the conduct, verbatim);
  [`ADR-0035`](ADR-0035-a-duel-is-a-freezeout.md) and `docs/duel-rules.md` (the escalating
  schedule is what guarantees the duel this ADR refuses to forfeit still ends);
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) (the client owns
  pacing against a server-stated fact, and states nothing of its own).
- **Registers, and does not answer:** `DEC-120` — **the architect's** — the mechanism (§6),
  following `ADR-0105` §6's precedent: this ADR fixes what must be true and writes no repair.
- **Constrains no engine code.** `poker-engine` stays pure and clock-free; `docs/duel-rules.md`
  Part 3 already assigns time limits to the server, and nothing here reopens that.

## Context

**A player who stops acting holds their rival's duel open indefinitely, and a merged ADR accepted
that with eyes open.** `ADR-0105` names it: *"a player who wants out of a duel is **stuck** — no
resign, **no turn clock**, and a `PLAYING` room is never reaped"* — then §1 sharpened it by letting
*Create a duel room* refuse a player who holds a running duel. The refusal is defensible only while
the duel it points at can actually end; today its end depends on the rival's clicks. `EPIC-13`'s
definition of done states the repair as a product fact: *"A player who stops acting no longer holds
their rival's duel open indefinitely."*

**Nothing exists to build on.** No clock, deadline or timebank exists in the engine, the server or
the client. The only countdown on `develop` is `presence-countdown.ts`: thirteen lines that turn a
server-sent deadline into whole seconds, floor at zero, with `ADR-0046` §3's rule that zero is not
an event. That is the turn clock's whole shape, in miniature, already merged and already licensed.

**What `ADR-0013`'s machinery does today, read at `develop` rather than remembered.**
`RoomTimeouts.disconnectGraceMillis` is 60 s, per seat, configuration. A dropped socket holds the
seat and pauses the duel — an action sent by the player who stayed comes back `DUEL_PAUSED`
(`ADR-0028`). When the window expires the seat is played by `foldAbsent`
(`poker-server/…/duel/AbsentSeats.kt`): at every decision the turn reaches, immediately and without
a fresh window, it reads the engine's `legalActions` and sends `Fold` when `FOLD` is legal, `Check`
otherwise — never a chip — until the player reconnects or the duel ends. Both seats gone abandons
the room and records nothing. Every frame it applies is marked `ActedForAbsent` to both seats.

**The human's instruction is what makes the two mechanisms collide.** `ADR-0013` forecast that a
per-action turn clock *"fits alongside this rather than replacing it"* — a forecast made before
anyone said *"timebank also work for disconnection case"*. That sentence gives the bank the
window's job. Kept alongside each other, the window and the clock both answer *"the player is not
acting"* with different durations and different screens, and `ADR-0046` priced the copy half of the
same collision in advance: *"`Timed out` will collide with a turn clock… This is the most likely
reason a future ADR supersedes this one."*

**A timeout is near a coin, which is why this is the product owner's.** Every coin this product has
ever settled was settled by a duel the engine ran to a chip holder — `RoomRegistry` has one
`sink.record` call site, reached only with an outcome (`ADR-0105`'s context, still true). No
forfeit exists anywhere. Whatever expiry does, it either keeps that single path or invents the
product's second one.

**The deadline.** `EPIC-13` cannot split item 4 without this answer, and the mechanism it needs
moves the wire — a deadline the client counts down to must be a fact the server sends — so the
version step should be priced (`DEC-120`) while `ADR-0047`'s ledger is quiet rather than after the
next claim.

## Decision

### 1. One clock answers "the seat on turn is not acting", and it does not care about the socket

A seat on turn has **30 seconds** to act at each decision point, fresh every time the turn arrives.
When the 30 s is spent, the seat's **timebank** spends: one budget of **3 minutes** per player per
duel, carried across hands, never refilled within the duel. A rematch is a new duel and a fresh
bank. The clock runs only while its seat is on turn — a runout, the rival's turn and the gap
between hands spend nothing — **and it keeps running when the seat's socket drops**. Disconnection
changes what a player sees, never what their seat is owed; dropping the connection is never a way
to gain time, stop time, or freeze a rival.

Both numbers are the human's, and both are **configuration, not literals** (`ADR-0013`'s rule,
inherited). This ADR decides the consequence of the numbers, not the numbers.

### 2. Expiry gives up the decision with `ADR-0023`'s conduct, marked as the server's

When the clock is exhausted, the server gives up that seat's decision exactly as it already gives
up an absent seat's: it reads the engine's `legalActions` at that decision point and sends
`Fold` when `FOLD` is legal — a bet is faced — and `Check` otherwise. Never a call, bet, raise or
all-in; never a chip. The act goes down the ordinary act path, and it is marked to both seats as an
act **the server** took (`ADR-0028`'s mark): a timed-out player is never presented as having chosen
what the clock chose for them.

The occasion widens — from *"the grace window expired"* to *"the clock ran out"* — and the conduct
does not change. `ADR-0023` is applied, not reopened, and the always-fold alternative it rejected
stays rejected for the same reason it was unimplementable then: at a free decision point the engine
does not offer `FOLD`, and the engine is not open here.

### 3. An expiry ends nothing by decree

An expiry costs the seat **one decision**. A fold costs the hand. **Nothing costs the duel**: no
count of expiries forfeits it, no timeout awards it, and no coin moves except by `ADR-0014` on the
outcome the engine reaches. A seat that never acts again blinds off — the escalating schedule that
guarantees a duel terminates (`docs/duel-rules.md`, `ADR-0035`) is the same guarantee that this
duel, played by the clock against an empty chair, still ends the only way a duel has ever ended:
one player holding every chip. The clock feeds the board; it never bypasses it. **The coin's
single settle path is untouched by this ADR, and that is the resolution of the coin question the
`DEC` was routed on.**

### 4. The timebank replaces the grace window, and the pause goes with it

`ADR-0013`'s fixed window is retired. In its place, one regime:

| The seat | What its time is |
| --- | --- |
| **Connected**, on turn | 30 s per decision, plus whatever bank remains — every decision, indefinitely. A connected seat is never `ABSENT`. |
| **Away** (socket dropped), on turn | exactly the same 30 s plus remaining bank. Reconnecting mid-clock resumes with what is left, state resent through the projection layer — `ADR-0013`'s reconnect promise, kept verbatim. |
| **Away**, clock exhausted | the seat is `ABSENT`: played by §2 **without a fresh clock** at every decision the turn brings it — today's post-window behaviour, verbatim — until the player reconnects. Reconnecting restores presence and the fresh 30 s; the bank stays spent. |
| **Away**, not on turn | spends nothing and shows no countdown. Its clock starts when the turn reaches it. |
| **Both seats gone** | the abandon path, unchanged: no outcome, nothing recorded. |

The line between the regimes is principled, not tuned: **a reconnect is a fact the server can see;
attention is not** — the only signal a connected player can send is an act, and an act needs time
to be sent in. So presence always buys a clock per decision, and absence, once the clock is spent,
buys nothing until the socket returns.

**The duel never pauses.** `AWAY` remains a true, shown fact about a seat (`ADR-0046` §1's word)
and stops being a brake on the duel: no action of the present player is refused because a rival's
socket dropped, and `DUEL_PAUSED`'s occasion leaves the product when the clock lands. The present
player's answer to *"how long will I wait?"* is the rival's clock — one countdown, one meaning.

### 5. What the player sees — requirements, not layout

- **One countdown: the acting seat's clock, visible to both players, ticking once per second** —
  the human's *"clock shoud visibly change each second"* — counting down to a **deadline the
  server stated**. The client interpolates the ticks between frames and asserts nothing
  (`ADR-0102`'s licensed shape; `presence-countdown.ts`'s arithmetic is the pattern).
- **The 30 s and the bank are visibly distinct.** A player must be able to see the bank begin to
  spend. The card owes the states the epic already names: *regular*, *running out*, *on timebank*,
  *expired* — drawn, not noted (`ADR-0091` §2 as the epic applies it).
- **Zero is not an event** (`ADR-0046` §3's rule, re-applied): nothing a player reads changes when
  the countdown reaches zero until a server frame carries the consequence. The enforced expiry may
  trail the visible zero; the screen never invents the act.
- **Both banks are public facts of the table.** Hiding the rival's bank would make the wait it buys
  unexplainable to the person waiting.
- **No new strings are chosen here.** *"The duel is paused."* leaves the screen when the pause
  leaves the product; what stands in its place is derived under `ADR-0046`'s register by the story
  that lands it, against its card. One constraint on those words is this ADR's: an expiry is never
  called a *forfeit* — `ADR-0046` §5 forbids the word because it is false, and under §3 it stays
  false.

### 6. What must be true of the mechanism — registered as `DEC-120`, the architect's

This ADR writes no repair. What the repair must satisfy:

- **The deadline the client counts down to is a server-sent fact** (`ADR-0002`). Which frame
  carries it — a field on an existing message or a new one — is the architect's.
- **Expiry is the server's own observation** and produces §2's act. Whether that is a
  server-synthesised act through the ordinary act path (`foldAbsent`'s shape) or a new room event
  is the architect's; either way the mark of §2 reaches both seats.
- **It moves the wire**: a `PROTOCOL_VERSION` step claimed in `ADR-0047`'s ledger under its
  one-bumping-branch lock, implemented as an `atomic:` ticket sized by `ADR-0070`'s probe run to
  green.
- **The enforced expiry may trail the stated deadline** by the scheduler's resolution — a sweep
  period, in today's terms (`ADR-0025`) — and may never precede it.
- **A resuming client is told the live deadline again**, or it has a countdown it cannot draw.
- **What becomes of `disconnectGraceMillis`, `GraceExpiry`, the `DUEL_PAUSED` path and
  `graceRemainingMillis`** as `presence-countdown.ts`'s input is the architect's to dismantle or
  reuse.
- **`poker-engine` stays closed** — no clock, no new action — and **timer tests inject time**
  rather than sleep (`ADR-0013`'s own rule, inherited with the machinery).

## Consequences

**What it buys.**

- `ADR-0105`'s named cost closes. *"No turn clock"* stops being true, and the epic's *"no longer
  holds their rival's duel open indefinitely"* becomes checkable arithmetic: 30 s a decision, one
  3 m bank, and a blind schedule that ends the duel. The refusal `ADR-0105` §1 shipped stops being
  a trap, because the duel a player is told to finish is now finishable on a bounded schedule even
  if their rival never clicks again.
- **Disconnecting is never a strategy.** Under a second-budget answer, killing the socket at 1 s
  left buys a fresh cushion, every hand; under a surviving pause, it freezes the table. Under this
  one the clock cannot tell and does not care, so there is nothing to exploit and nothing for a
  future anti-abuse rule to patch.
- **One countdown means one thing.** `ADR-0046`'s priced collision — *"`Timed out` will collide
  with a turn clock"* — dissolves instead of landing: there is one clock, and *Timed out* names the
  seat whose clock it was.
- **The coin's meaning survives its first clock.** Still one settle path, still only a duel the
  engine ran to a chip holder. A ladder position can still not be produced by anything but the
  board.

**What it costs.**

- **A connected ghost still costs the rival real time.** A player who sits connected and never
  acts spends their bank once, then ~30 s per decision across a blind-off — plausibly twenty to
  forty minutes of thirty-second waits. Named and accepted rather than engineered away: the repair
  is an away state a connected player can *leave*, which needs an *"I'm back"* control and new
  wire, bought against zero evidence the griefer exists. If one turns up, that is a new `DEC`, not
  a quiet widening of this one.
- **A bank-empty player who drops gets 30 s where today they get 60.** Deliberate: the bank *is*
  the disconnect protection now, and a player with any bank left gets up to 3.5 minutes — more
  than today's window ever gave. The tunnel still costs hands, never the duel. Both numbers stay
  tunable without a code change.
- **`ADR-0013`'s title overstates again** — *"a grace period, then folds"* is now a clock, then
  checks-or-folds — the standard price of amending an immutable ADR, paid on this same file once
  already by `ADR-0023`.
- **The pause's copy dies with the pause.** *"The duel is paused."* and the `DUEL_PAUSED` refusal
  lose their occasion; the implementing story owes `ADR-0046`'s §2 table a revision in its own
  register, and `DEC-108` — whether the bar may stay enabled under that sentence — becomes a
  question about a sentence that is leaving the screen. `DEC-108` stays open and is **not**
  answered here.
- **The wire moves.** The version step the `DEC`'s registration priced is real, and `DEC-120`
  spends it.
- **The table gains public information.** A rival's remaining bank is readable, so time trouble
  becomes visible and playable-around — the same honesty trade `ADR-0046` made when its copy taught
  players how an absent seat plays. And the checked-down absent seat can still win a hand:
  `ADR-0023`'s cost, re-accepted here, not repaired — luck deciding a hand is the vision's own
  line.

**What it forecloses.**

- **A pause.** Any future *"the duel freezes while a socket is down"* reverses this ADR rather than
  tuning it.
- **A second countdown.** Two time regimes answering *"the player is not acting"* on one screen are
  refused by construction, which is most of why *replace* won.
- It does **not** foreclose a resign — a resign would simply end some duels before their clocks
  matter, touches a coin, and stays the human's, declined *"not now"* on 2026-09-02 (`ADR-0105`).
  And it does not foreclose tuning either number, which is configuration. What is *not licensed*
  here or by any vision sentence is time as a reward — a bank that refills for winning, daily
  minutes, purchased seconds. That is the bonus furniture *What it is not* refuses, and no future
  reading of this ADR should treat "the bank is configuration" as a door to it.

**Why this shape, on thin evidence.** There are no players, so every piece chosen here is one
already merged and already argued for — `ADR-0023`'s conduct, `foldAbsent`'s loop, `ABSENT`'s
played-instantly semantics, `presence-countdown.ts`'s arithmetic — with the smallest new
commitment on top: a deadline on the wire and two numbers in configuration. The options that write
something a player keeps — a forfeit's result row, a ladder movement by decree, a new engine
action — are exactly the options refused.

## Alternatives considered

**Expiry always folds.** The simplest rule to state, it matches `docs/duel-rules.md` Part 3's own
loose sentence (*"it is the server that decides to submit a fold on their behalf"*), and it closes
the checked-down-to-a-win edge outright. Rejected because it is unimplementable without reopening
the engine: `BettingRules.kt` puts `FOLD` in the legal set only when a bet is faced — the exact
collision `ADR-0023` documented when `ADR-0013`'s letter stalled duels forever at the big blind's
option — and `EPIC-13` bars the engine by name. It is also strictly worse for the timed-out player
at zero gain to anyone: folding a decision that checking keeps free hands over pots nobody bet at.

**Expiry forfeits the duel — immediately, or after some count of expiries.** The strongest
alternative, and the one the reference products use: Lichess flags a game, and a flag unsticks the
rival in seconds instead of a blind-off's half hour. Rejected on the vision's own sentences: a coin
is *"a counter of duels won"*, every coin ever settled came from a duel the engine ran to a chip
holder, and a forfeit is the product's first coin moved by decree rather than by the board — the
same shape `ADR-0105` put outside the vision's licence when the decree was a navigation, with the
adjacent call (a resign) put to the human the same day and answered *"not now"*. Chess flags
because chess has no free move to make for you; poker has `check` and `fold` built into its rules,
so the game itself supplies the graceful degradation and the harsh rule would buy speed the format
does not need. It is also the least reversible option on the table — a result row and a ladder
movement, permanent, against a rule an afternoon retunes.

**The timebank as a second budget, spent after the grace window.** The conservative reading: it
amends nothing merged, keeps a clean division — the window answers sockets, the clock answers
attention — and never gives a disconnected player less than today. Rejected because it makes
killing the socket strictly better than sitting at the table: a player at 1 s left buys a fresh
60 s cushion with a dropped connection, repeatable every hand, so the clock's integrity would
depend on the socket's honesty — backwards, in a product whose server trusts no client. It also
stacks to ~4.5 minutes of rival-waiting per drop, and it keeps two countdowns with different
durations and different screens answering one question, which is the confusion `DEC-115` was
registered about.

**The two run concurrently, first to expire governs.** Its case: no free cushion — the clock keeps
running through a drop — without formally retiring the window. Rejected because whichever number is
smaller silently governs, so one mechanism is always a dead letter that still has to be drawn: with
bank remaining the window never fires; bank-empty, the window outlives the clock and drags the
pause questions back with it. Two numbers racing to zero on one screen for one fact is the opposite
of *"dark, quiet, fast, minimal"*, and every drawn state of the loser is design debt for a
mechanism that cannot act.

**Replace the window but keep the pause for a seat that is not on turn.** The kindest option to the
dropped player — they miss no action at all, and it is today's behaviour narrowed rather than
removed. Rejected because a pause is a brake on both clocks, so it hands the socket exactly the
lever §1 exists to remove: a losing player can freeze the duel's progress by dropping, and making
that freeze finite needs the pause's own expiry — which resurrects the second window and the
two-budget mess. Meanwhile the player it protects loses nothing under full replacement: their own
clock runs only on their own turn, and the state they missed is resent, projection-filtered, the
moment they return — the half of `ADR-0013` this ADR keeps verbatim.
