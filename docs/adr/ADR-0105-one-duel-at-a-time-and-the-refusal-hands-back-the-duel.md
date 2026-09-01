# ADR-0105 — One duel at a time, and the refusal hands the player back their duel

- **Status:** Accepted
- **Date:** 2026-09-01
- **Resolves:** `DEC-109` — may one player hold seats in **two live rooms at once**? Registered open
  2026-09-01 by [`ADR-0104`](ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md)
  §10, which answered `DEC-107` and split this off rather than deciding it.
- **Where the answer came from:** **the human stated this call on 2026-09-02.** It is not derived
  from the vision, and an earlier draft of this ADR that claimed it was has been withdrawn — see
  *How this ADR was decided* below. The *shape* of the answer — a refusal rather than a forfeit —
  **is** derived, from *What it is*: ***"One duel coin per win. Not chips, not currency, not a
  balance. A counter of duels won."*** and ***"A duel is a match, not a hand."*** A coin that moves
  because a player clicked a link is not counting a duel won, so a forfeit was never the vision's to
  license either; the human was asked about it in the same breath and declined it. The wording of
  the refusal follows *Positioning* — ***"Dark, quiet, fast, minimal"*** — the same sentence
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)
  and [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) derive their strings from.
- **Applies, and reopens none of:** [`ADR-0013`](ADR-0013-disconnect-grace-period.md) (the seat is
  held for a window, then the duel goes on without the player);
  [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) (what the server plays for
  an absent seat); [`ADR-0014`](ADR-0014-duel-coin-economy.md) (the loser gives a coin);
  [`ADR-0018`](ADR-0018-a-second-socket-adopts-the-seat.md) (one live session per player, the
  newest socket holds it); [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) (the code is the
  invite, and refusals hand out no oracle);
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §4 (a tab forgets a
  room; nothing on the wire vacates a seat); `ADR-0073` §3 (a waiting room stays open, and the host
  was told so); `ADR-0046` §§0–2 (the voice these words are written in).
- **Qualifies** [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md) §1 by exactly one
  case: opening an invite is taking the seat, **except** when the opener is already in a running
  duel, when it takes none and says so. §1's *no confirmation, no accept-or-decline, no pre-join
  view* is untouched for every player who is free to sit down.
- **Constrains:** the server's answer to `CreateRoom` and `JoinRoom` from a player who already
  holds a seat in a `PLAYING` room, and the two sentences the client shows for it. It constrains no
  engine code, no schema and no stored data, and it decides no layout.
- **Registers, and does not answer:** `DEC-110` — **the architect's** — by what mechanism the
  refusal is carried and the player returned to their duel (§6); `DEC-111` — **the product
  owner's** — whether one player may hold more than one `WAITING` room at once (Consequences).
- **Does not explain the fold.** `ADR-0104` §9's discipline is inherited verbatim; see §7.

## Context

`ADR-0104` closed the path by which one room's frames reached a player sitting somewhere else. It
left the state that produced them: a player can be seated in two rooms at once, and the product has
never said whether that is allowed.

**What the tree does today, read at `develop` rather than theorised.** `DuelSocket`'s
`replyToCreateRoom` writes `room.code = created.code` unconditionally; `replyToJoinRoom` writes
`room.code = parsed` on every branch that seats or resumes. Neither looks at the seat the player
already holds, and nothing can vacate it: `ClientMessage` has five subtypes — `Hello`, `Act`,
`CreateRoom`, `JoinRoom`, `OfferRematch` — and none of them is a leave (`ADR-0072` §Context, force
3). The seat therefore stays where it was, held by a player who is somewhere else.

**The old room then finishes without them, and settles a coin.** The socket that left is closed, so
`ADR-0013`'s grace window opens; `ADR-0025`'s ticker expires it; `foldAbsentSeats` plays that seat
under `ADR-0023` — checking when nothing is owed, folding when facing a bet, never putting another
chip in; the duel reaches a chip holder, the room goes `FINISHED`, and `ADR-0014` takes a coin off
the loser. That is the whole of the harm and it is not hypothetical: `RoomRegistry` has exactly one
`sink.record` call site, inside `act`, reached only when the runner has an outcome. A room where
**both** seats are gone is `abandon`ed instead and records nothing. **So every coin this product has
ever settled was settled by a duel the engine ran to a chip holder. Nothing anywhere forfeits a duel
because a player went somewhere else** — which is exactly what makes the alternative below a new
commitment rather than an application of an old one.

**`ADR-0104` makes that loss silent.** Before it, room X's nine routed frames followed the player to
whatever screen they were looking at: wrong, but visible. After its §1 lands they are dropped with
no counter and no log. The coin then moves with nothing at all to show the player it happened. That
raises the price of leaving this open, and it is the deadline.

**The routes in are shipped controls, not exotic ones.**

1. `Back to the lobby` — `ADR-0072` §5 on the result screen, `ADR-0073` §1 on the waiting screen —
   forgets the room and navigates, and says in its own KDoc that *"the socket that is open keeps its
   seat"*. The player is at the lobby, still seated.
2. `?room=CODE`. `boot.ts` prefers it over the memory in as many words — *"The invite wins over the
   memory: a player who has just followed a link to a new room means that room, whatever this
   browser was in last"* — and `ADR-0094` §1 puts nothing between the link and the table. A player
   in a duel who is sent a second link is one click from a second seat.
3. A second device. `ADR-0018` has the newest socket adopt the session and close the older one; that
   device's `localStorage` has never heard of the room, so it boots to the lobby, where the only
   thing to press is *Create a duel room*.

**Not every held seat is a duel, and the difference is the whole scope of this decision.** A
`WAITING` room has no duel, no opponent and no coin at the end of it, and `ADR-0073` §3 already told
its host, on screen, *"The room stays open. That link still works for your rival, and it brings you
back."* A `FINISHED` room has no duel running either, and `Room.offerRematch` agrees only when both
seats have offered — so nothing there can start without the player's own press. Only a `PLAYING`
room has a duel with a coin at the end of it.

**There is no way out of a running duel, and no turn clock.** `Application.sweepPass` runs three
sweeps and no fourth: expire grace windows, reap idle rooms, delete expired verification rows. The
reap's state list is `WAITING` and `FINISHED`/`ABANDONED` — **a `PLAYING` room is never reaped** —
and there is no per-action timer anywhere for a player who is *connected*. So a player in a running
duel cannot end it by choosing to; they can only stop being there. That force pulls against
refusing, it does not disappear by being ignored, and it is named as a cost below rather than
argued away.

### The deadline

Nothing is blocked. `TASK-121403` is correct under every answer, as `ADR-0104` §10 says. Two things
argue for settling it now, and neither argues for a particular answer.

**`ADR-0104` is landing.** The moment room-scoped delivery merges, the second-seat state stops
producing any evidence at all. A defect that leaves a trace is cheaper to leave open than one that
does not.

**The wire has not shipped.** No client is deployed, so if the answer needs a frame, today is when
that is free (`ADR-0044`'s argument, unchanged). That is a reason to decide, not a reason to prefer
the answer that spends one — and this one does not spend one on its own account.

## Decision

### 1. A player holds at most one seat in a running duel

While a player holds a seat in a room whose duel is running, `CreateRoom` and `JoinRoom` from that
player **take no seat**. The request is refused. The room they asked for is untouched, the seat and
the duel they hold are untouched, and no coin moves.

### 2. *Running* means `PLAYING`, and the other two states are not refused

| The player's held seat | A second `CreateRoom` / `JoinRoom` | Why |
| --- | --- | --- |
| `PLAYING` | **refused** (§1) | a duel is running, and it ends in a coin |
| `WAITING` | allowed, exactly as today | no duel, no opponent, no coin — and `ADR-0073` §3 told the host on screen that they may walk away and the room stays open. Refusing here would make a shipped promise false |
| `FINISHED` / `ABANDONED` | allowed, exactly as today | no duel is running, and `Room.offerRematch` agrees only when **both** seats have offered, so nothing can start without this player's own press |

A seat inside `ADR-0013`'s grace window is a seat in a `PLAYING` room and is refused with the rest.
That is the grace period working, not an exception to it: the window exists precisely so that a
player who dropped is still in their duel.

### 3. The refusal hands the player back the duel they are in

The player ends up looking at **their duel** — the table as it stands — and not at a lobby, not at
an empty screen, not at a dialog they must dismiss. This is the half that makes §1 defensible: a
refusal that left the player on a screen with no route back to a duel they are still seated in would
be a worse trap than the one it removes, and the two shipped routes in (§Context 2 and 3) both
arrive holding no memory of that room.

Doing it again does the same thing. Following the same link twice, or reloading a tab whose URL
still carries the code, refuses again and lands in the same duel again — no accumulation, no state
that has to be cleared, nothing that gets worse with repetition.

### 4. The words

One line, in `ADR-0046`'s sentence voice — the fact, then what it means for the reader:

> `You are already in a duel. Finish it to start another.`

Verbatim: capital *Y*, both full stops, no room code, no name. **These two sentences are the whole
of the addition.** A screen that needs a third string is a screen that has outgrown this decision,
and the answer is a new ADR, not an invented sentence (`ADR-0073` §3's rule, applied).

Where the line sits, what weight it carries and how it leaves the screen are **`EPIC-06`'s and the
implementing ticket's** — this ADR fixes the words and the fact, not the layout, on `ADR-0073` §6's
precedent. Two constraints on the treatment are the product's and are fixed here: it is a **notice,
not a dialog** — nothing is destroyed, so nothing needs confirming (`ADR-0073` §4) — and it does not
take the table away from the player it is explaining the table to.

What the line may not say, each with the merged rule that forbids it:

| Not said | Because |
| --- | --- |
| any duration, countdown, or when the player will be free | the client owns no clock against a server window (`ADR-0072` §6), and nothing knows when a duel ends |
| anything about the room that was asked for — that the code was good, dead, full, or unparseable | `ADR-0022`'s no-oracle rule: the same words answer every code, so a player in a duel learns nothing about any code they try |
| that the link will still work later | nothing knows. A `WAITING` room dies on `waitingMillis` and no frame carries that |
| *Leave*, *Cancel*, *Quit*, *Forfeit*, *Resign* as a way out of the line | they name an action the product does not have. `ADR-0073` §5 refuses each of them already, for the same reason: a control that asserts a fact the server does not hold is the one thing this client may not do |
| *sit out*, *another table*, *switch tables*, *cash out* | the casino's furniture, refused by `ADR-0046` §5 and by *What it is not* |

### 5. The player whose invite was clicked sees nothing, because nothing happened to them

Their room is not entered, no seat is taken in it, and no frame is produced for them. Their code goes
on working for whoever holds it, for as long as `ADR-0022` gives it. There is nothing to tell them
and nothing that could carry it.

### 6. What must be true, and what is left to the architect

The repair is not written here. What it must satisfy:

- **Nothing moves.** No seat is vacated, no room abandoned, no duel forfeited, no coin settled, and
  the refused room is left exactly as it was found.
- **The client can name the duel it is putting the player back in.** A tab that has never heard of
  that room must still be able to show it, so the answer has to identify the room the player holds —
  which is the server telling the truth about where a player is sitting, `ADR-0002` in the direction
  `ALREADY_SEATED` already answers in (`replyToJoinRoom` answers a player seated in the room they
  asked for *"exactly as a fresh seating would"*, with the seat they hold).
- **The refusal is not a guess and must not be metered as one.** `ADR-0022` §2 decides a budget of
  ten refused joins per player per minute; nothing in the tree implements it yet — `JoinLimits` and
  `TOO_MANY_ATTEMPTS` do not exist at `develop` — and when it is built, a player refused because of
  their own live seat must not spend it. They are not sweeping for codes; they are being told where
  they already are.

**How that is carried is `DEC-110`, the architect's**: a new `ProtocolError` and the
`PROTOCOL_VERSION` step `ADR-0047` prices, or an answer assembled from frames that already exist.
This decision does not choose, and the choice does not change what the player sees.

### 7. What this does not explain

`ADR-0104` §9 recorded a human's report of a player being folded repeatedly, and that no
reproduction ever produced it. This ADR explains it no better. It removes one way a player can be
folded in a room they are not looking at; that is a mechanism that fits, exactly as `ADR-0104`
called it, and it is not evidence. If a fold of a *present* player is reproduced after this lands,
it is a new defect and a new ticket, and this ADR is not its answer.

## Consequences

**What it buys.** The product says one thing about what a duel is: a match between two people who
are both there. A coin keeps meaning *a duel won*, because no coin can now be settled by a duel the
loser was refused the chance to walk out of mid-way. `ADR-0013`'s grace period starts paying off
across devices rather than only across reconnects — a player who lands anywhere holding no memory of
their room is handed it back, which is what holding the seat was for. And it is the cheapest working
answer to reverse: a refusal deleted is a refusal gone, against a settled result row and a ladder
position, which are not.

**What it costs.**

- **A player who wants out of a duel is stuck, and this is the decision that makes them feel it.**
  There is no resign, no turn clock, and a `PLAYING` room is never reaped — verified above in
  `Application.sweepPass` and the reap's state list. Their only exit is to stop being there and let
  `ADR-0023` play the seat out, and that needs their **rival** to keep acting; a rival who walks off
  without dropping leaves the room running with nothing to end it, and this player refused at the
  front door for as long as that lasts. Until today they could at least start another duel. They no
  longer can. This ADR does not create that state and does not fix it.
- **The product's primary action can now say no.** *Create a duel room* is the front door — the one screen
  that carries the wordmark (`ADR-0098`) — and it acquires a state in which it does not do what it
  says. That is a real loss of directness on the most important control in the product, bought to
  avoid spending a coin on a click.
- **A player who followed a friend's link is not seated, and is told nothing about the link.** §4
  forbids promising it will still be there, because nothing knows. The friend, meanwhile, waits at a
  room nobody joined and learns nothing either (§5). Two people are left slightly in the dark, on
  purpose, because the alternative is copy that guesses.
- **One more string that has to stay in one voice, with nothing enforcing it.** `ADR-0073` named this
  exact cost for `Back to the lobby` living in two components; this adds a third sentence to the same
  register, and the day somebody rewords it, nothing fails.
- **The hole in §2 is real and stays open.** A player may hold several `WAITING` rooms — three presses
  of *Create a duel room* with `Back to the lobby` between them — and each of those can be joined by a
  rival and become a running duel the player is not at. §1 refuses a seat *taken* while a duel runs;
  it does not stop two duels *starting* in rooms whose seats were taken earlier. So "never in two
  duels at once" is what this decision aims at and not what it guarantees, and saying otherwise would
  be the defect this repository has caught most often. It is registered as `DEC-111`.
- **This may cost a protocol version step.** Whether it does is `DEC-110`'s to price, and the answer
  is free today and not later.

**What it forecloses.** Multi-tabling, deliberately and permanently — that is the point. And
`ADR-0094` §1's *opening the invite is taking the seat* stops being unconditional: there is now one
player it does not seat, and any future reader of that ADR needs this one. It does **not** foreclose
a way out of a duel: if the product ever gains a resign, §1 is unchanged by it and simply stops
applying sooner. That feature would settle a coin on a player's press, which is a decision with a
coin in it, and it is not made here.

**Why this shape, on thin evidence.** There are no players. The evidence for what a person expects
when they click a second link is one human's afternoon, and it did not survive reproduction. So the
rule is the one an afternoon undoes: a server answer and two sentences, no schema, nothing stored,
nothing a player keeps. Every heavier option in the set writes something down that cannot be taken
back — a duel result, a coin, a ladder position.

## How this ADR was decided, and why the first attempt was wrong

**This is the record of a boundary being tested, and it belongs in the ADR rather than only in the
git history.**

The first draft answered `DEC-109` on the product owner's own authority and named a licensing
sentence: `docs/vision.md`'s *What it is not* — *"Not a multi-table poker room. No 6-max, no 9-max,
no tournaments, no sit & go, no cash games."* Review rejected that reading, and the rejection holds:

- Every item in that sentence's own elaborating list — 6-max, 9-max, tournaments, sit & go, cash
  games — names a **table or game format**. `DEC-109` asks a different-axis question: whether one
  *account* may hold seats in two *separate* heads-up duels at once. Nothing in that list reaches it,
  and **no other sentence in the vision speaks to per-player concurrency across duel instances.**
- The draft conceded as much in its own words — *"that sentence names the furniture rather than the
  activity only because the furniture is how the activity arrives"* — which is an argued bridge, not
  a citation. Needing the bridge was the tell.
- [`.claude/agents/product-owner.md`](../../.claude/agents/product-owner.md) paraphrases that same
  vision sentence, in its own *What the vision already settles* section, as a statement about table
  size. The agent's settled reading of the sentence contradicted the use it was being put to.

That agent's boundary is *"say which sentence of the vision licensed your answer; if you cannot
point at one, you are inventing."* A competing and textually stronger reading existed under which
the vision is simply **silent**, which by that rule made this **the human's call** — and the draft
carried `FOR THE HUMAN: none`.

**So it was put to the human, who chose the same answer for a different reason.** The conclusion in
§1 is unchanged; what changed is the authority under it. That distinction is the whole point of the
rule: an answer that is right by luck and licensed by a misreading would have stood as precedent for
the next misreading.

**Asked in the same breath and answered:** whether a player may **resign** a duel they are in. The
answer was **not now — record it**. The cost in `## Consequences` therefore stands **knowingly
accepted** rather than merely disclosed: until a resign or a turn clock exists, a player who wants
out of a duel has no exit but to stop being there and let their rival play the seat out.

**The vision is unchanged.** This ADR does not amend `docs/vision.md`, and no one should read it as
having done so. If the product ever wants that sentence to exist, it is its own change and its own
approval.

## Alternatives considered

**Vacate the first: taking a new seat forfeits the duel you are in.** The strongest case in the set.
The client has already decided that the newest room is the one the player means —
`boot.ts`'s *"The invite wins over the memory"* is merged, commented and deliberate — and `ADR-0094`
§1 puts nothing between a link and a table. It is the only answer that lets a player who genuinely
walked out start again immediately instead of waiting on a rival's clicks, and it describes what they
actually did: they left. It also needs no new screen state on the second table, because the second
table is simply their duel now. Rejected because it settles a coin on a navigation. Every coin in the
merged tree comes from a duel the engine ran to a chip holder — one `sink.record` call site, reached
only with an outcome — and the vision calls a coin *"a counter of duels won"*. A duel lost by clicking
a link is not a duel won by anybody, and inventing the product's first forfeit is a commitment
`docs/vision.md` does not make; it belongs to the human, not to me. It is also the least reversible
option available: a result row and a ladder movement, permanent, against a refusal an afternoon
undoes. And it is not even cheap — after `ADR-0104` no frame reaches the player in the room they left
and no screen shows a forfeit, so the honest version of this option is a new frame, a new screen and
a version step, bought to make a player lose faster.

**Permit both, and mean it.** Its case: it is what the code does today, it costs nothing to build,
and there is a real argument that a player who opens a second room wants a second duel — this is
ordinary on a chess server, where the reference points live. Rejected on the vision's own sentence:
*"Not a multi-table poker room."* And on what the second duel would be: *"A duel is a match, not a
hand"* — twenty to forty-five hands of decisions — of which the unattended one would be played by
`ADR-0023`'s absent-seat rule, which never bets, never calls and never raises. The product would be
recording, on a ladder, a duel played by nobody. `ADR-0104` makes it worse rather than better: the
player would not even watch it happen.

**Refuse while *any* seat is held — waiting, playing or finished.** Its case is simplicity, and it is
not a weak one: one rule, no states to reason about, nothing for an implementer to get subtly wrong,
and it closes `DEC-111`'s hole in the same stroke. Rejected because it makes a shipped promise false.
`ADR-0073` §3 renders *"The room stays open. That link still works for your rival, and it brings you
back."* on the waiting screen — a control this project argued for on the vision's own *Positioning*
sentence — and this would refuse the front door for up to ten minutes to a host who took that control
at its word. It would also refuse a player sitting on a finished result screen, where nothing can
start without their own `OfferRematch`, for a room that is doing nothing but waiting to be reaped.

**Refuse the *rival's* join instead — stop a second duel from starting in a room whose seat is held
by someone already playing.** Its case: it acts at the only moment a second running duel can actually
begin, so it closes the hole §2 leaves rather than the one §1 closes, and it needs nothing new on the
first player's screen at all. Rejected because the refusal lands on the wrong person: a stranger who
did everything right is turned away for a fact about somebody else, and the only copy that explains
it tells them where that somebody is. It is also neither sufficient — the second-seat request still
has to be answered — nor an answer to `DEC-109`, which asks about the player who takes two seats.
Recorded here because it is the shape `DEC-111` will have to weigh.

**Seat them in the new room and *pause* the first duel until they come back.** Its case: nothing is
lost, nobody forfeits, and `ADR-0013` already knows how to pause a duel for a seat that is not there
— this is that machinery, reused, with no new concept in the product. Rejected because the pause is
sixty seconds and then `ADR-0023` plays the seat anyway, so it is today's behaviour under a kinder
name; and the only version that is not is one that holds the pause open longer, which spends a
**rival's** time — a present, blameless person kept at a table because their opponent went to play
somebody else. There is no version of this that does not send that bill to the wrong player.

**Say nothing: let `ADR-0104` be enough.** Its case is honest and nearly won on cost. `ADR-0104` §1
stops the crossed frames, which is the defect a human actually hit; there are no players, nobody has
lost a coin, and every line written here is a line that could have been a ticket. Rejected because
`ADR-0104` makes this quieter, not smaller: the room still plays the seat, the duel still finishes,
the coin still moves, and the player now sees none of it. Shipping that means shipping a product in
which the one number it keeps can be spent by a click nobody ever connected to it — and the cheapest
moment to say otherwise is before the first public link, not after the first complaint.
