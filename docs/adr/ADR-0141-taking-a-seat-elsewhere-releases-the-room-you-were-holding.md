# ADR-0141 — Taking a seat elsewhere releases the room you were holding

- **Status:** Accepted
- **Date:** 2026-09-08
- **Resolves:** `DEC-145` — **may a player who holds a `WAITING` room take a seat in another room by
  invite, and what becomes of the room they hold?** *Yes, they take the seat — and the room they
  were holding is released in the same act.* Registered open 2026-09-06 by
  [`ADR-0124`](ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) §7, which answered
  `DEC-111` for the **create** route and named this as *"the last route by which one player can end
  up in two duels at once"*.
- **Where the answer came from: derived from `docs/vision.md`; the human did not state this call.**
  The two halves are licensed by two different sentences, and saying which is which is the point.
  - **The seat is taken** on the first success condition — *"Send a link. She opens it in a browser.
    We play a full heads-up match. Someone wins. We hit Rematch."* — of which the vision then says
    *"Everything else is downstream of that moment."*
    [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md) already read that sentence to
    settle this exact path, counting its five verbs and finding that none of them is *accepts*.
    Refusing the seat is strictly more friction than the single confirming click `ADR-0094` refused,
    so a refusal needs a sentence stronger than the one that shipped the join. There is none.
  - **The held room is released** on *What it is*'s *"One duel coin per win. Not chips, not
    currency, not a balance. **A counter of duels won.**"*, read with the one-sentence version's
    *"Two people, **one link**, one heads-up poker match, one winner"* and *What it is not*'s *"Not
    a multi-table poker room."* Those are the three sentences `ADR-0124` derived `DEC-111` from,
    applied to the state it left open: a room that can settle a coin for a duel its holder never saw
    is the counter counting something that is not a duel won.
- **Supersedes** two sentences, both in `ADR-0073`, and both restatements of `ADR-0022` rather than
  clauses of `ADR-0022` itself. Quoted whole, because a paraphrase here would hide which words move:
  - its header bullet's *"What happens to the **room** is not decided here at all — it was decided
    by [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md), which gives a `WAITING` room one way to
    end, an idle timeout, and no other."* — the final six words go. A `WAITING` room now has two
    endings, and this is the second.
  - §3's first sentence, *"The room stays `WAITING`, the host keeps seat 0, the code keeps resolving,
    and the room ends the one way `ADR-0022` gives it — the idle timeout."* — superseded **only** as
    a statement about a `WAITING` room in general. Read as a statement about the control §3 is
    about, it stands byte-unchanged, and so does the sentence after it: *"The control changes none
    of that"*. `Back to the lobby` still does nothing to the room.
  - **`ADR-0022`'s own Decision is untouched.** §3 — *"A code's life as an invite ends when the seat
    is taken"* — is not amended, contradicted or extended; a released code answers `UNKNOWN_ROOM`,
    which §3 already makes *"indistinguishable from a room that never existed"*, so its no-oracle
    property holds unchanged. §§1, 2 and 4 are not touched at all.
- **Amends:** `ADR-0105` §2's **`WAITING` row, and only its `JoinRoom` half**. That row's
  *"allowed, exactly as today"* is what changes, and only the second clause of it: the join is still
  **not refused**, its stated reason — *"no duel, no opponent, no coin — and `ADR-0073` §3 told the
  host on screen that they may walk away and the room stays open. Refusing here would make a shipped
  promise false"* — still holds, and still holds as a reason not to refuse. What stops being true is
  *as today*: the room the player was holding no longer stands. The row's `CreateRoom` half was
  amended by `ADR-0124` §1 and is untouched here. `ADR-0105` §§1 and 3–7, and §2's `PLAYING` and
  `FINISHED` rows, stand **byte-unchanged in words and behaviour**.
- **Fills in:** `ADR-0124` §5's third row — *"| a `WAITING` seat | **another live** room's code |
  **not decided here** — `DEC-145` |"*. §7 below restates that table with the row answered; the
  other rows are unchanged.
- **Upholds:** `ADR-0094` §1 — *"Opening an invite link seats the player, and nothing stands between
  the link and the table"* — which gains **no second qualification** here; `ADR-0094`'s header
  bullet says it is *Qualified by* `ADR-0105` *"in exactly one case"*, and after this ADR that is
  still exactly one case. `ADR-0124` §§1–6 entire. `ADR-0073` §§1, 2, 4, 5 and its two strings.
  `ADR-0002` — which room a player is seated in is the server's fact.
  [`ADR-0018`](ADR-0018-a-second-socket-adopts-the-seat.md), which is why a player has one live
  socket and therefore one place they can actually be.
- **Applies:** [`ADR-0133`](ADR-0133-the-room-a-player-holds-is-scanned-for.md) — `heldRoom` ships,
  so *does this player hold a room* is answerable and this decision is about what should happen
  rather than about whether it can be detected; §4's lock-order rule is the rule the repair lands
  under. `ADR-0022` §3 (what a dead code answers). `ADR-0035` (a duel is a freezeout, which is why
  the rejected alternative's ghost duel runs to a coin rather than stalling).
- **Answers what `ADR-0133` §6 deferred**, in its own words: *"A holder of a `WAITING` room asking to
  join **another** room is not touched by this decision at all — that is `DEC-145`, the product
  owner's, and until it is answered `JoinRoom` behaves exactly as it does at `develop`."* It no
  longer does. `ADR-0133` is not amended — its `PLAYING`-only guard, its scan, its stripes and its
  lock order all stand byte-unchanged — but `replyToJoinRoom` is no longer a function this epic's
  work leaves alone, and `DEC-154` is where that lands.
- **Registers, and does not answer:** `DEC-154` — **the architect's** — by what call, at what point
  in `replyToJoinRoom`, and under what lock order is the held room released, given that the release
  touches a **second room's** mutex while the join holds the first? §8.
- **Constrains:** the ticket that builds this, and `replyToJoinRoom`. It constrains **no client
  file, no string, no wire type, no `PROTOCOL_VERSION` step (`ADR-0047`) and nothing stored.**
- **Leaves open:** whether a host may ever deliberately **close** the room they hold, or ask for a
  fresh code — `ADR-0073` §7's own open item, which `ADR-0124` made load-bearing and this decision
  makes stranger still, since a room can now end early as a *side effect* while no control ends one
  on purpose. Named in Consequences, deliberately unregistered on `ADR-0105` §6's rule: nobody is
  working it.

## Context

Two shipped promises fall due at the same instant and cannot both be kept.

**The invite promises a seat.** `ADR-0094` §1 shipped *"A player who opens `…/?room=CODE` is seated,
the duel starts, and the first thing they see is the table."* It was derived from the vision's
success condition and it is the product's core act.

**The room promises to stand.** `ADR-0073` §3 prints on the waiting screen: *"The room stays open.
That link still works for your rival, and it brings you back."* A host reads that, walks off, and
the code they may already have sent goes on resolving for ten minutes.

A player holding a waiting room who clicks a friend's link makes both promises due at once. Today
the tree keeps both, and keeping both is the defect: `Room.join` seats them and starts that duel,
and their own code goes on resolving for whoever has it.

**What the rival holding the abandoned code meets today, read off `develop` at `7016b5b4` rather
than assumed.** Call the holder `A`, their room `X`, the friend's room `Y`, and the rival who was
sent `X`'s code `R`. Following `Y`'s link is a page load, so `A`'s socket closes and
`RoomRegistry.disconnect(X, A)` puts seat 0 in `X.awaySeats` — and builds no frame, because
*"a `RoomState.WAITING` room has no guest, so a host's drop there always answers an empty
`Disconnection.outbound`"* (`RoomRegistry.kt`). Then:

- `R` clicks. `Room.join`'s `WAITING` branch seats them, `withFreshRunner` deals, and the frames
  `R` receives are the duel's own plus the turn clock's. **No presence frame is among them**, so
  `R`'s table shows an opponent it has been told nothing about — `ADR-0118`'s principle working
  exactly as designed, on a fact nobody thought to send.
- The clock runs on seat 0: `turnMillis` 30 s and then `timebankMillis`, `3 * 60 * 1000`
  (`RoomTimeouts.kt`). **Up to three minutes thirty of a countdown against a chair.**
- `Room.giveUpTurn` fires. Because seat 0 is in `awaySeats` it latches into `absentSeats`, and the
  expiry's frames carry `Addressed(otherSeat, room.presenceOf(seat))` — `ABSENT`, which
  `ADR-0046` renders as *timed out*. That is the first thing `R` is told about any of this.
- From there `foldAbsent` plays the seat at every decision with no wait at all. The freezeout
  (`ADR-0035`) empties in seconds, `ADR-0014` takes a coin off `A` and gives one to `R`.

`R` did nothing wrong. `A` did nothing wrong either, beyond clicking a link they were sent. **The
coin is settled out of a duel neither of them chose**, and the *"counter of duels won"* counts it.

**The state up to the latch is described by nothing.** Every ADR about an away seat — `ADR-0013`,
`ADR-0023`, `ADR-0028`, `ADR-0046`, `ADR-0108` — describes a player who *was* at the table and left
it. A duel that **begins** with a seat already away is in none of them, and the three and a half
minutes in which the table shows a present opponent who is not there is the part no merged source
names. That is worth stating plainly, because it inverts the usual objection to changing shipped
behaviour: here it is the **status quo** that leaves an undescribed state, and the change removes
it.

**The mechanism half is already merged, so nothing is blocked on feasibility.** `ADR-0133` shipped
`heldRoom` — a lock-free scan answering *which live room does this player hold* — and §4 anticipated
this decision by name: *"`DEC-145`, when it is answered, is the change most likely to want a stripe
on the join path, and this is the rule it must land under."*

**One neighbouring route is decided and not yet built, and this ADR is not what makes it true.**
`ADR-0105` §1 refuses a `JoinRoom` from a holder of a `PLAYING` seat, and `ADR-0133` §6 fixes the
guard's shape; neither has shipped, so `ALREADY_IN_DUEL` exists nowhere in the tree. That is its own
`atomic:` ticket, outside this decision.

### The deadline

`DEC-145` blocks no ticket, and its register row says so. Two things argue for answering it now, and
neither argues for a particular answer.

**The harm is reachable on shipped code with two people and one link**, which is the product's own
core flow — the flow the vision's success condition describes. Every day this stays open is a day
that flow can settle a coin for a duel nobody played.

**Nothing is stored and no wire moves under any of the three answers**, so the shape is as cheap to
reverse today as it will ever be. `DEC-145`'s *Due* column says *before the first public link*, and
that remains the true deadline: after strangers hold codes, the first person to meet the wrong
answer is not somebody who can be told about it.

## Decision

### 1. The seat is taken, and nothing stands in front of it

A player who holds a `WAITING` room and follows another live room's invite **takes that seat**. The
join is not refused, not confirmed, not delayed, and not announced. `ADR-0094` §1 applies to them
exactly as it applies to a player who holds nothing, and this decision adds no qualification to it.

### 2. The room they were holding is released in the same act

At the instant the seat elsewhere is taken, the `WAITING` room the player held stops being a live
room. What is fixed here is only what is observable:

- its code answers a later `JoinRoom` exactly as a code naming no room does — `UNKNOWN_ROOM`, which
  `ADR-0022` §3 already makes *"indistinguishable from a room that never existed"*, so no oracle is
  created and `ADR-0022` §2's failed-join budget meters it exactly as it meters any other miss;
- the client prints the string it already prints for a reaped room — `No duel room has that code.`
  (`Lobby.tsx:436`, two shipped tests) — so **no string is added anywhere**;
- nothing else about the room is visible to anyone, because nobody but its holder was ever in it.

**A released room is indistinguishable from a room the reaper took.** The whole of this decision, at
the surface, is that the reaper's outcome arrives at an instant the product chose instead of one
`waitingMillis` chose. Every screen, string and refusal path it needs already ships.

**Why the release is licensed and walking away is not.** The room is not released because its holder
left — `ADR-0073` §3 promises that leaving is safe, and it stays safe. It is released because they
have **taken a seat in another duel**, which is the one act after which *"and it brings you back"* is
something the product will not do for them: their held seat is a running duel's now, and going back
either costs them that duel or, once `ADR-0105` §1 ships, is refused outright. A room that can no
longer do the true half of what its screen promised can still do the harmful half, and that
asymmetry is the whole reason this is a decision rather than a preference.

### 3. Only a room still `WAITING`, and still holding nobody else, is released

If a rival has already taken the second seat, the room is `PLAYING`, a duel is running in it, and it
is **left exactly as it is found**. Nothing in this decision ever ends a duel, empties a seat a
second player is sitting in, forfeits a hand or settles a coin. The rival who got there first is
owed their duel, and they get it; what happens to the holder in that case is `ADR-0105` §1's, not
this ADR's.

### 4. A join that seats nobody changes nothing

If the join is refused for any reason — a code that does not parse, `UNKNOWN_ROOM`, `ROOM_FULL`, or
`TOO_MANY_ATTEMPTS` once `ADR-0022` §2's budget is built — the held room stands exactly as it was
found. **The release follows sitting down, never trying to.** A mistyped code, a dead link and a
stale invite cost the player nothing, and repeating any of them does the same thing every time.

### 5. What is not released, and this is the larger half of the rule

Nothing here releases a room whose holder merely stopped looking at it. Pressing `Back to the lobby`,
closing the tab, dropping the connection, going quiet for nine minutes, or pressing the create
control again all keep the room, keep the code resolving and keep `ADR-0073` §3's promise whole —
the last of those is `ADR-0124` §1, untouched.

It follows, and is stated so nobody reads more into this than it says: **a duel can still begin with
a seat already away.** A host who opens a room and closes their laptop still leaves a live code, and
a rival who clicks it still meets the Context's countdown. That is `ADR-0073` §3 working as intended
— the host may yet come back — and this decision does not touch it. What this removes is the one
case where the holder **provably cannot** come back, because they are sitting somewhere else.

### 6. Nothing is said, to anyone

The player is told nothing: they land at a dealt table, which is `ADR-0094` §1's own promise, and no
banner, toast, dialog or line is added to it. Nobody is in the released room to tell, exactly as
`ADR-0105` §5 found for its own path. No new string enters the product.

Three reasons, in order of weight:

- **The only surface available is the worst one.** The instant the release happens, the player is
  looking at a hand with blinds posted and a decision due. `ADR-0105` §4 already fixes that a notice
  *"does not take the table away from the player it is explaining the table to"*; a line about a
  room they are no longer in, delivered over their first decision, is the shape that rule exists to
  refuse.
- **The rival is told at the only moment they can be told**, and told by a shipped string: they
  click, and the lobby says `No duel room has that code.` Nothing earlier could reach them — no
  frame is addressed to a person who is not connected to anything.
- ***Dark, quiet, fast, minimal.*** `docs/vision.md`'s *Positioning*, applied as `ADR-0124` §3
  applied it.

**What would reopen this, stated so it can be observed rather than argued.** A player who sends a
code, joins a friend's duel, and then tells their rival the link is broken — or asks where their
room went. That is a report, not a prediction, and it is one string away from being answered. It is
the clause of this decision most likely to be reversed.

### 7. Every route a player can take while holding a live seat

`ADR-0124` §5's table with its third row answered. The other rows are unchanged and are restated
only so the set can be read in one place.

| The player holds | They press / follow | What happens |
| --- | --- | --- |
| a `WAITING` seat | *Create a duel room* | **the room they hold** — `ADR-0124` §1, no second room, nothing said |
| a `WAITING` seat | their **own** code, by link or by the tab's memory | `RoomJoined(code, seat)` through `replyToJoinRoom`'s `ALREADY_SEATED` branch — **unchanged**, no seat taken elsewhere, **nothing released** |
| a `WAITING` seat | **another live** room's code | **seated there, and the room they held is released** — §§1, 2 |
| a `WAITING` seat | a code that is dead, unparseable, full, or over budget | refused as today, and **the room they hold stands** — §4 |
| a `PLAYING` seat | either | `ADR-0105` §1's refusal, with §4's two sentences — **untouched, and not yet built** |
| a `FINISHED` seat | *Create a duel room* | a new room — **untouched**, on `ADR-0105` §2's reason |
| no seat, or a room since reaped | anything | **unchanged** |

Repeating any row does the same thing every time. Nothing accumulates, and a player who has released
a room holds exactly one live seat afterwards — the new one.

### 8. What must be true of the mechanism, and whose it is

The repair is not written here, on `ADR-0105` §6's and `ADR-0124` §6's precedent. What it must
satisfy:

- **The room is found from the player, never from the connection.** `heldRoom` already answers this
  (`ADR-0133` §1), and a second device or a cleared browser has no membership record to read.
- **Only a `WAITING` room is ever released, and only when the join actually seated somebody.** §§3
  and 4 are the two conditions, and they are conditions on a room's state at the moment of the
  write, not on what it was when the join began.
- **Nothing else moves.** No duel ends, no seat is vacated in the room being joined, no coin
  settles, no `lastActivityAt` is written anywhere but in the room being joined, and the joined room
  is left exactly as an ordinary join leaves it.
- **It needs no new frame.** The joining player gets `RoomJoined` and the duel's frames, as today;
  the rival gets `UNKNOWN_ROOM`, as today. **No `ProtocolError` value, no `PROTOCOL_VERSION` step,
  no `docs/protocol.md` edit, no client file.**

**`DEC-154` is what is left, and it is the architect's**, because it is a lock question this ADR
cannot settle from the product side. `RoomRegistry.mutate` runs its block under the joined room's
mutex and its `block` parameter is a plain function type, not a `suspend` one, so a second room's
mutex **cannot** be acquired inside it — `ADR-0133` §4's structural guarantee, working. The release
therefore sits outside that critical section, before it or after it, and each has a price: before,
and a refused join has already destroyed a room, which §4 forbids; after, and a window opens in
which both rooms are live, which §3 bounds but does not close. Which one, whether a player stripe is
taken across both, and how `ADR-0133` §4's *"a player stripe is always outermost"* is honoured, is
`DEC-154`.

### 9. What this deliberately does not decide

- **The ghost duel by every route other than this one** — §5. A host who simply goes away still
  leaves a live code, and a rival who clicks it still meets the Context's countdown. Whether a duel
  should begin at all into a seat that is already away is a different question, about
  `Room.join`'s `WAITING` branch rather than about anyone's second seat, and nobody has asked it.
  **The finding that question rests on is recorded here rather than left in the Context, because it
  outlives this decision:** a duel that *begins* with a seat already away is described by **no
  merged ADR**. `ADR-0013`, `ADR-0023`, `ADR-0028`, `ADR-0046` and `ADR-0108` all describe a player
  who *was* at the table and left it. Measured on `develop` at `7016b5b4`: the joining rival is sent
  **no presence frame at all** — `withFreshRunner` builds none, and the host's drop from a `WAITING`
  room built none because there was no guest to address — so the table **counts down for up to
  3 min 30 s against an opponent it has been told nothing about** before `giveUpTurn` latches the
  seat and `ADR-0046`'s *timed out* finally says so. This decision removes the one producer of that
  state it is entitled to remove, and leaves the state itself standing.
- **Whether a host may close a room deliberately, or ask for a fresh code.** `ADR-0073` §7 left it
  open, `ADR-0124` made it matter more, and this makes it stranger without answering it.
- **Anything about a `FINISHED` or `ABANDONED` room, a rematch, or the address.**
- **The room's lifetime.** Ten minutes from `Room.open`, unchanged; `RoomTimeouts` is not edited.
- **What a player is told.** §6 chooses silence and names what would reopen it; it does not decide
  what a future line would say.

## Consequences

**What it buys.**

- **The coin's meaning is defended on the last route into it.** `ADR-0124` narrowed the harm and
  said so — *"`DEC-145` is what is left, and it needs a second person's link"*. With this, no
  sequence of shipped presses produces a room that can settle a coin for a duel its holder never
  saw, and *"a counter of duels won"* counts only duels somebody played.
- **The rival's worst case becomes a state that is already described.** A dead code at the instant
  of the click is `ADR-0073` §3's own named correction and `Lobby.tsx`'s shipped string. The
  alternative it replaces — a countdown against a chair, a *timed out* mark, then a rush of instant
  folds and a coin — is described by nothing.
- **The invite keeps its promise, whole.** `ADR-0094` §1 gains no second exception, so the vision's
  success condition still holds for every link a friend sends.
- **It costs no wire, no string, no client file and no stored data**, so the whole footprint is one
  server write on one path, and deleting it restores today's behaviour exactly.

**What it costs.**

- **A rival who was sent a live code can meet a dead one, and it is the product that killed it.**
  This is the cost, and it lands on the person who did nothing. `ADR-0124`'s Alternatives rejected
  closing a held room precisely because *"the first room's code may already be in a rival's hands,
  so closing it lands `No duel room has that code.` on a blameless stranger who did nothing but
  click the link they were sent."* That sentence is quoted here and not dodged: **the same thing
  happens under this decision, on a different trigger.** What has changed is what the rival's
  alternative is. On `ADR-0124`'s route the alternative was a live room its holder could still
  return to; here it is a duel against a seat nobody occupies, ending in a coin. The dead code is
  the better of the two experiences actually available, and it is not a good one.
- **A `WAITING` room gains a second way to end, and a shipped on-screen line can be made false by
  the reader's own next act.** *"The room stays open. That link still works for your rival, and it
  brings you back."* is true when it is read and true until its reader sits down somewhere else.
  The string is not changed and the screen is not corrected, so the product now renders a sentence
  with an unstated condition on it.
- **This is the first act in the product that ends a room ahead of its clock.** Every future reader
  of `ADR-0073` §5's refused vocabulary — *Cancel the room*, *Close the room*, *End the room* — must
  now know that the *fact* those words asserted is, on exactly one path, real. The words stay
  refused: no control says any of them, because no control does this.
- **Two duels at once is narrowed to a window, not closed.** Between the seat being taken and the
  release landing, a rival can seat themselves in the room being released; §3 then leaves it alone
  and the outcome is today's. The window is the width of one registry operation, it is not zero, and
  nothing in this decision measures it.
- **The player is not told their code died** (§6), so they may go on believing a link they sent is
  live. Named as the reopening observation rather than defended as a virtue.
- **`replyToJoinRoom` grows a write on its success path.** The entry point every invite goes through
  now changes a *second* room, which is a new shape for that function and a new thing every future
  reader of it has to hold. The price is the architect's under `DEC-154`.
- **`ADR-0105` §2's `WAITING` row is now amended by two different ADRs, one clause each**, and is no
  longer a complete statement of anything on its own. A reader who finds `ADR-0105` first will get
  both halves wrong; the amendment lines at the top of this file and of `ADR-0124`, and
  `ADR-0105`'s index row, are the only things that catch them.

**What it forecloses.** One player holding a live invite while sitting in a duel — and with it the
shape where a player keeps a room warm for later while playing elsewhere, which is the queueing the
vision's *"Not a multi-table poker room"* refuses. It forecloses nothing about closing a room on
purpose, about `FINISHED` rooms, or about what a duel does when it begins into an away seat.

## Alternatives considered

**The held room stands, and the player is simply in two places.** Its strongest case is the
strongest in this set: it destroys nothing, and this product has never destroyed a room — `ADR-0073`
§5 refused every word that claims one was destroyed, and `ADR-0124` refused the act itself. It is
today's behaviour, so it can regress nothing and costs no code. The rival's code goes on working,
which is literally what the waiting screen promised them, and their duel is bounded and
self-correcting: `ADR-0046`'s *timed out* arrives within one turn clock and they win. Rejected on
the coin. *"One duel coin per win… A counter of duels won"* is the sentence `ADR-0124` derived
`DEC-111` from, and a coin taken off a player who was at a different table for the whole duel is
that counter counting a duel nobody played — with the loss falling on the one person in the whole
sequence who cannot possibly have known. The rival's side is worse than it looks, too, and it was
measured rather than imagined: up to three and a half minutes of a countdown against a table that
has been told nothing, because no presence frame is built for a host who dropped out of a room with
no guest in it. That is the one state in this area that no merged ADR describes.

**The invite is refused while they hold a room.** Its strongest case: it is the only option that
protects the rival holding the code *completely* — the room stands, the holder can still come back,
and the code they were sent stays true. It is also the option most consistent with `ADR-0105` §1,
which refuses exactly this act one room-state later, so the product would speak with one voice about
holding a seat. Rejected on the vision's own success condition. `ADR-0094` §1 shipped *"There is no
confirmation, no *Take the seat*, no accept-or-decline, and no pre-join view of the room, in v0.1"*
on the reading that the sentence has five verbs and *accepts* is not among them; a refusal is more
friction than the click that reading refused, and it lands on a link a friend correctly sent. It is
worse than it looks in one further respect: the product has **no way to close a room** (`ADR-0073`
§7), so a player refused here has no control that fixes it and must wait out up to ten minutes of a
room they no longer want. That is a trap of exactly the kind `ADR-0073` was written to remove,
rebuilt on the invite path.

**Refuse the invite, but let the create control release the room.** Its strongest case: it keeps
`ADR-0073` §3's promise unconditionally true — only a deliberate press ever ends a room — and it
gives the player the escape the plain refusal denies them. Rejected because it makes the create
control mean two opposite things depending on unseen state: `ADR-0124` §1 fixed that a press of it
while holding a room **hands that room back**, and this would make the same press sometimes destroy
it. It also fails the case it is aimed at, since the player who followed a link does not want a
room — they want their friend's table — and telling them to press *create* to get there is the
product explaining its own internals.

**Release the room, and say so in one new line.** Its strongest case, and the closest call after the
main three: the player is losing something they may have handed to another person, which is a larger
fact than any silence `ADR-0124` §3 chose, and `ADR-0110` §6's rule allows a new string by a new ADR
— and this is one. Rejected on where the line would have to sit. The only surface the player is on
at that instant is a dealt table with a decision due, and `ADR-0105` §4 already rules that a notice
may not take the table away from the player it is explaining it to. A line there would be the
product's first interruption of a hand, spent on a room. §6 records the observation that would
reverse this, and the reversal is one string and one frame in either direction.

**Answer only the first half — the seat is taken — and register what becomes of the room.** Its
strongest case: `DEC-145` is visibly two questions, the seat half is settled by a merged ADR almost
mechanically, and the smallest answer is usually the right one here. Rejected because the seat half
alone is not a decision — `ADR-0094` §1 already says it, and an ADR that only restated it would
close nothing. Everything that made `DEC-145` worth registering is in the second half, and leaving
it open would leave the register carrying a question whose visible half had been answered, which is
the shape `docs/adr/README.md` warns about by name.
