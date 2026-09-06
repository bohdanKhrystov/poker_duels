# ADR-0124 — A player holds one waiting room, and pressing play again hands it back

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-139` — **does pressing play again return the host to the room they already
  hold?** *Yes.* And, in the same stroke, `DEC-111` — **may one player hold more than one
  `WAITING` room at once?** *No.* Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 6, and open since
  2026-09-01, respectively.
- **Where each half came from.** They came from different places, and the difference matters.
  - **`DEC-139`'s direction is the human's**, stated 2026-09-06 and recorded verbatim — *"item6:
    room lifecycle - after i created a room same room shoud be "alive" for some time(like 3-5 min).
    for casses like: i go back to lobby and press play again. in this case i shoud be back to the
    same room"* — and this ADR **does not choose it**. `EPIC-14` takes that report as the source
    rather than the specification, so what is decided here is everything the report does not reach:
    *by what* the host is returned, what the browser remembers, and what is said.
  - **`DEC-111` is derived**, from [`docs/vision.md`](../vision.md)'s one-sentence version — *"Two
    people, **one link**, one heads-up poker match, one winner"* — read with *What it is not*'s
    *"Not a multi-table poker room"* and *What it is*'s *"One duel coin per win. Not chips, not
    currency, not a balance. **A counter of duels won.**"* Those are the sentences
    [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) derived its own
    answer from, applied to the state it left open. One player with two live invites out is the
    multi-table shape; a room that can settle a coin for a duel its holder never saw is the counter
    counting something that is not a duel won.
- **Amends:** `ADR-0105` §2's **`WAITING` row only** — that row's *"allowed, exactly as today"* is
  what changes, and only its second clause: a `WAITING` seat is still **not refused**, and its
  stated reason (`ADR-0073` §3's promise) still holds, but a second `CreateRoom` no longer opens a
  second room. `ADR-0105` §§1 and 3–7, and §2's `PLAYING` and `FINISHED` rows, stand
  **byte-unchanged in words and behaviour**.
- **Upholds:** [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §§1 and
  3 — unchanged, and now load-bearing for a second reason (§2);
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) §3's
  promise, whose three clauses stay true and whose third gains a second route;
  [`ADR-0110`](ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §§1–7, including §6's closed
  string set, which this decision adds nothing to.
- **Applies:** [`ADR-0002`](ADR-0002-server-authoritative.md) (which room a player is seated in is
  the server's fact, and a browser's memory of it is a cache);
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) (a `WAITING` room has one way to end, and
  nothing is said *about* a code); [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md)
  §1 (the rival is not part of this state).
- **Extends:** `DEC-110` — the architect's mechanism question now covers the `WAITING` case as well
  as the `PLAYING` one, because both need the same lookup (§6). `DEC-110` therefore blocks
  `STORY-1416` as well as the ticket that implements `ADR-0105` §1.
- **Registers, and does not answer:** `DEC-145` — **the product owner's** — may a player who holds a
  `WAITING` room take a seat in **another** room by invite, and what becomes of the room they hold?
  (§7.)
- **Constrains:** `STORY-1416`, and the server's answer to `CreateRoom` from a player holding a
  `WAITING` seat. It constrains **no client file, no string, no wire type, no `PROTOCOL_VERSION`
  and nothing stored**.
- **Leaves open:** whether a host may ever **close** the room they hold, or ask for a fresh code —
  `ADR-0073`'s own open item, which this decision makes load-bearing and does not answer.

## Context

The human went back to the lobby, pressed play again, and did not land in the room they had. Six
things are in tension, and the first four were read off `develop` at `7c39fd3d` rather than assumed.

**The lifetime they asked for already exists, and is twice what they asked for.**
`RoomTimeouts.DEFAULT_WAITING_MILLIS` is `10 * 60 * 1000` (`RoomTimeouts.kt:35`) against the report's
*"like 3-5 min"*, and `ADR-0073` §3's promise — *"The room stays open. That link still works for
your rival, and it brings you back."* — is already true on the server. So the report cannot be about
the number, and the change it asks for is not a longer life.

**The half that is not true is the browser's.** `ADR-0072` §3 makes `forgetRoom()` one of exactly
two things that clear `pd.roomCode`, and `forgetRoom()` is what the `Back to the lobby` control
calls — `Lobby.tsx:374`, passed into `WaitingTable` as `onLeave`. The tab that leaves stops
remembering, on purpose.

**Nothing on the server stops the next press.** `replyToCreateRoom` is three lines and reads nothing
about the player: `deps.rooms.create(session.player.id)`, `room.code = created.code`, `RoomJoined`.
`ADR-0105` §2 refuses a second room only while the player's held duel is `PLAYING`. Put together, a
host who leaves and presses again **opens a second waiting room**, while the first lives out its ten
minutes holding the code they may already have sent. Three presses of shipped controls, and it is
what the product does today — which is `DEC-111` arriving as a symptom instead of as a question.

**The harm is a coin, and it is `ADR-0105`'s own harm by the route `ADR-0105` §2 left open.** The
orphaned room still answers its code. A rival who clicks the link that was already sent is seated,
`Room.join`'s `WAITING` branch starts the duel, and the host is looking at a different room:
`ADR-0013`'s grace window opens, `ADR-0025`'s ticker expires it, `foldAbsentSeats` plays the seat
under `ADR-0023`, the duel reaches a chip holder, and `ADR-0014` takes a coin off the loser. The
counter of duels won then counts a duel nobody played. `ADR-0105` §1 refused a **second seat taken
while a duel runs**; this is a **duel starting in a seat taken earlier**, and no rule reaches it.

**The obvious fix is the wrong one, and the mechanism says why rather than taste.** Keeping the
memory across `Back to the lobby` looks like the whole repair — the browser that left the room comes
back to it — and it restores `ADR-0073`'s trap exactly. The way back is an `<a href="/">`, so the
press is a full page load: boot's `Welcome` reaction sends `JoinRoom` at the remembered code,
`RoomRegistry.resume` answers `null` for a `WAITING` room, `Room.join` refuses the host with
`ALREADY_SEATED`, `replyToJoinRoom` answers that refusal *exactly as a fresh seating would* with
`RoomJoined(code, seat)`, and `Lobby.tsx`'s `state.roomCode !== null` branch puts the waiting table
straight back up. The lobby becomes unreachable again — the door `ADR-0073` cut. Whatever returns the
host, it cannot be the browser's memory of the room.

**A waiting seat has exactly one source, which is why one decision answers both questions.**
`Room.open` is the only constructor of a `RoomState.WAITING` room; `RoomRegistry.create` has exactly
one production call site, `replyToCreateRoom`; no transition anywhere returns a room to `WAITING`
(`join` → `PLAYING`, `finish` → `FINISHED`, `abandon` → `ABANDONED`); and `Room`'s own `require`
keeps a `WAITING` room's `guest` `null`, so the one seated player in a waiting room is always
`Room.host`. **How many waiting rooms a player may hold is therefore decided entirely by what a
second `CreateRoom` does** — `DEC-111` read from the other end is `DEC-139`.

**The shipped promise cuts the other way from how it reads.** `ADR-0105` §2 declined to refuse a
`WAITING` seat because *"`ADR-0073` §3 told the host on screen that they may walk away and the room
stays open. Refusing here would make a shipped promise false."* That force is real and it stands —
but it argues against a **refusal**, not against a **return**. *"…and it brings you back"* is a
promise this decision keeps by a second route.

### The deadline

`STORY-1416` cannot be split without this, and it blocks nothing else. Two reasons to answer now,
and neither argues for a particular answer.

**`DEC-110` is the deadline.** The architect's open question is by what mechanism a player who asks
for a second room while holding a live seat is handed back the room they hold. Answered for
`PLAYING` alone, it produces a lookup built for one room state; the same lookup serves both, and
widening it after it is built pays for the design twice. Deciding this before `DEC-110` is answered
is free; deciding it after is not.

**Nothing is stored and no wire moves under any answer**, so the shape is as cheap to reverse today
as it will ever be — a reason to decide, not a reason to prefer one answer.

## Decision

### 1. A player holds at most one waiting room, and a second press hands back the first

While a player holds a seat in a `WAITING` room, a `CreateRoom` from that player **opens no second
room**. Nothing is refused and nothing is destroyed: they are handed back the room they already
hold — the same code, the same seat — and that room is left exactly as it was found.

This is the answer to both questions. `DEC-139`: yes, pressing play again returns the host to the
room they hold. `DEC-111`: no, one waiting room — because the only door to one now hands back the
first.

### 2. The browser goes on forgetting, and what returns the host is the server

`ADR-0072` §3 stands **byte-unchanged**: `Back to the lobby` calls `forgetRoom()`, exactly two
things clear `pd.roomCode`, and the memory still means *the room this tab is seated in* — never
*the screen this tab should show* (`ADR-0072` §1). **No second key, no flag, no client clock, and no
client code changes at all.**

Three reasons, in order of weight:

- **The lobby stays reachable.** A browser that remembered the room across the way out would rejoin
  it on the next boot and land back on the waiting table; the Context traces that path frame by
  frame. The forget is what keeps the door open, and it is now doing two jobs.
- **`ADR-0002`.** Which room a player is seated in is the server's fact. The browser's memory is a
  cache of it, and a cache is the wrong thing to make authoritative over a return.
- **It works where the memory cannot.** A second device, or a browser whose storage was cleared, has
  never heard of the room; `ADR-0105`'s Context named that route in — *"that device's
  `localStorage` has never heard of the room, so it boots to the lobby, where the only thing to
  press is *Create a duel room*"* — and nothing answered it. It is answered here, by the same press,
  because the server is asked about the **player**.

### 3. Nothing is said about it, and no string is added

The host lands at their table with no notice, no banner, no toast and no dialog. `ADR-0110` §6's
enumeration of what the host-alone state renders is unchanged and stays exhaustive.

Three reasons:

- **They were already told.** The screen they left says *"The room stays open. That link still works
  for your rival, and it brings you back."* Being brought back is that promise kept. A sentence
  explaining it would explain a promise the product had already made on the same surface.
- **The screen is the statement.** The code shown is the code they may already have sent, and the
  invite beside it is live. Nothing on it is false, so there is nothing to correct.
- ***Dark, quiet, fast, minimal.*** `docs/vision.md`'s *Positioning*, applied the way `ADR-0110` §7
  applied it when it refused an arrival announcement — *"quiet is the vision's word."*

**What would reopen this, stated so it can be observed rather than argued.** A host who is returned
and does not realise it — who sends the same code believing it is a fresh one, or presses again
looking for a different room. That is a report, not a prediction, and it is one string and one card
frame away from being fixed. Until then, `ADR-0110` §6's enumeration is the whole set.

### 4. What the host is returned to is `ADR-0110`'s state, and every promise on it is true of it

The surface is `WaitingTable.tsx`, unchanged: the rival's empty seat carrying `Waiting for your
rival`, the invite panel (the bare code, the selectable link box, `Copy the link` where a clipboard
exists), the host's own `You` plate, `Back to the lobby`, and `ADR-0073` §3's line. Checked clause by
clause against the room being returned to:

- **The room stays open** — nothing is sent and nothing is written; the room's state, its seat and
  its `lastActivityAt` are untouched.
- **That link still works for your rival** — the code the panel draws is the code the invite was
  built from, still resolving through `Room.join`'s `WAITING` branch.
- **…and it brings you back** — now twice over: by the link, as before, and by this press.

The seat is 0 in both cases (`Room.open` always seats the host there), so what the client receives is
shaped exactly like the answer to a fresh create, and nothing in the client has to tell the two
apart.

**What the surface does not say, and must not start saying:** how long the room has left. The client
owns no clock against `waitingMillis` (`ADR-0072` §6), and being returned does **not** restart it — a
`WAITING` room's `lastActivityAt` is written at `Room.open` and by nothing else until a rival joins.
The ten minutes runs from when the room was opened, and this decision does not change that.

### 5. Every way a player can ask for a room, and what each does

| The player holds | They press / follow | What happens |
| --- | --- | --- |
| a `WAITING` seat | *Create a duel room* | **the room they hold**, §1 — no second room, nothing said |
| a `WAITING` seat | their **own** code, by link or by the tab's memory | `RoomJoined(code, seat)` through `replyToJoinRoom`'s `ALREADY_SEATED` branch — **unchanged**, and the same outcome as the row above |
| a `WAITING` seat | **another live** room's code | **not decided here** — `DEC-145` |
| a `PLAYING` seat | either | `ADR-0105` §1's refusal, with §4's two sentences — **untouched** |
| a `FINISHED` seat | *Create a duel room* | a new room — **untouched**, on `ADR-0105` §2's reason: no duel is running, and `Room.offerRematch` agrees only when both seats have offered, so nothing can start without this player's own press |
| no seat, or a room since reaped | *Create a duel room* | a new room — **unchanged** |

Repeating any row does the same thing every time. Nothing accumulates and nothing has to be cleared.

### 6. What must be true of the mechanism, and whose it is

The repair is not written here, on `ADR-0105` §6's precedent. What it must satisfy:

- **The room is found from the player, never from the connection.** `RoomMembership` is per-socket
  and a second device has none; §2's third reason is half the point of the answer, and a mechanism
  that read the connection's own record would silently drop it.
- **Nothing moves.** No room is opened, no seat vacated, no room abandoned, no `lastActivityAt`
  written, no coin settled. The room is left exactly as found — the same constraint `ADR-0105` §6
  states, for the same reason.
- **It needs no new frame.** `RoomJoined(code, seat)` already means *you are seated in this room, at
  this seat*, and `replyToJoinRoom`'s `ALREADY_SEATED` branch already answers exactly this,
  *"derived fresh from the registry rather than assumed"*. So **no `ProtocolError` value and no
  `PROTOCOL_VERSION` step (`ADR-0047`) is spent on this half.** The architect may still spend one on
  `DEC-110`'s `PLAYING` half; that is their call, and this decision does not push it either way.
- **The refusal budget is not touched.** There is no refusal here at all, so `ADR-0022` §2's
  failed-join budget has nothing to meter when it is built.

One fact the mechanism may use, stated because it was measured and not because it is the design: the
one seated player of a `WAITING` room is always `Room.host` — `Room.open` is the only constructor of
that state, and the invariant keeps `guest` `null`. Whether the lookup is a scan, an index, or
something else is `DEC-110`'s, now widened to cover this state.

### 7. What this deliberately does not decide

- **Whether a player who holds a waiting room may take a seat in another room by invite**, and what
  becomes of the room they hold. Registered as `DEC-145`. It is the last route by which one player
  can end up in two duels at once, and it is a different question with different forces: the invite
  winning over the memory is a shipped principle (`boot.ts`: *"a player who has just followed a link
  to a new room means that room"*), and every alternative to allowing it reaches a third party who
  did nothing.
- **Whether a host may close the room they hold, or ask for a fresh code.** `ADR-0073` left it open;
  this decision makes it matter more and still does not answer it. Named in Consequences as a cost,
  deliberately unregistered — nobody is working it, and `ADR-0105` §6's rule is that a `DEC` nobody
  is working is noise in the open table.
- **The room's lifetime.** Ten minutes, from when it was opened, unchanged (§4).
- **Anything about a `FINISHED` room, a rematch, or the address.**

## Consequences

**What it buys.**

- **The human's report is answered with no new lifetime, no new string, and no client change.** The
  ten minutes already ships, the browser already forgets, and `RoomJoined` already says the one
  thing that needed saying. The whole answer is one guard on one server function.
- **`DEC-111` closes rather than being narrowed.** A waiting seat has one source; that source now
  hands back the first room; so a player cannot hold two. It is answered by construction rather than
  by a rule somebody has to remember.
- **The link is single.** One player, one live invite, which is the *"one link"* of the vision's own
  one-sentence version and the shape *"Not a multi-table poker room"* refuses.
- **The coin's meaning is defended on the route that was actually reachable.** Three presses of
  shipped controls no longer produce a room that can settle a coin for a duel its holder never saw.
  Narrowed, not closed — `DEC-145` is what is left, and it needs a second person's link.
- **`ADR-0118`'s window stops being armed by this press.** That ADR named `replyToCreateRoom`'s
  missing seat check as what makes a flashed lobby dangerous: a press inside the recovery window is
  honoured and orphans the room the player holds. With `ADR-0105` §1 for `PLAYING` and this for
  `WAITING`, no press of that control can orphan a live seat in either state.
- **It is the cheapest working answer to reverse.** One guard, no schema, no stored data, no wire
  type, no string, no client file. Deleting it restores today's behaviour exactly, and nothing
  persisted has to be unwound.

**What it costs.**

- **A host cannot get a fresh code for up to ten minutes.** Today they can, by pressing again. The
  product has no way to close a room, so their only exits are the reaper and a rival arriving. The
  case for calling this a serious loss is weaker than it first looks — a second room never revoked
  the first code either, it only added a second live one — but the capability is real and it is
  being removed, and if the human wants it back the answer is a control, an ADR, and `ADR-0073`'s
  open item.
- **A press that appears to do nothing.** A host who reaches the lobby and presses again lands on a
  screen that looks exactly like the one they left. §3 chose silence, so the only evidence they are
  in the same room is a code they may not have memorised. This is the clause of this decision most
  likely to be reversed, and §3 says what would reverse it.
- **Two live states answer the same press differently.** Holding a `PLAYING` seat gets `ADR-0105`
  §4's two sentences; holding a `WAITING` seat gets silence. Defensible — one is a refusal of
  something and the other is a request honoured — but it is one more rule for every future surface
  near that control to know, and the two rules live in two ADRs.
- **`replyToCreateRoom` grows a read it did not have.** The product's primary press now asks a
  question about the player before it does anything. The price is the architect's to weigh under
  `DEC-110`; the product's constraint is only that the question is about the player and not about
  the connection.
- **`ADR-0105` §2's table stops being a complete statement of what a second room request does.** Its
  `WAITING` row says *"allowed, exactly as today"*, and *as today* is precisely what changes. A
  reader who finds that ADR first will get this wrong; the amendment line at the top of this file
  and `ADR-0105`'s index row are the only things that catch them.

**What it forecloses.** Two live invites from one player, and with them any future in which a player
queues or juggles rooms — which is the multi-table shape the vision refuses, so foreclosing it is the
point rather than a side effect. It forecloses nothing about closing a room, replacing a code, or
`FINISHED` rooms and rematches, and it spends no wire budget, so `DEC-110`'s carrier choice is as
open after this as before it.

## Alternatives considered

**The browser remembers the room across `Back to the lobby`, and the next press rejoins it.** Its
strongest case is strong: it is client-only, writes no Kotlin, needs no server read, is the smallest
diff available, and it is the most literal reading of the report — the browser that walked out of the
room walks back into it. It also keeps the whole rule in one file. Rejected on a mechanism read
rather than a preference. A remembered code is exactly what boot's `Welcome` reaction sends
`JoinRoom` for, so the next page load answers `ALREADY_SEATED` → `RoomJoined` → `Lobby.tsx`'s
`state.roomCode !== null` branch, and the waiting table is back up with no way off it: `ADR-0073`'s
room with no door, restored by the fix. Keeping both the memory and the door needs a second stored
fact meaning *which screen to show*, which is precisely what `ADR-0072` §1 forbids this memory to
mean. And it answers `DEC-111` not at all — a second device, or a browser whose storage was cleared,
opens a second room exactly as it does today.

**Refuse the second `CreateRoom`, with `ADR-0105` §4's two sentences adapted.** Its strongest case:
one rule for both live states, told in one voice, and the player is *told* what happened instead of
being left to notice — which is the failure §3 is most exposed to. Rejected because there is nothing
to refuse. The press asks for a duel room; the player has one; handing it over is the request
honoured, not denied. A refusal would also make `ADR-0073` §3's promise false in the exact way
`ADR-0105` §2 predicted — the host was told they may walk away and that the room brings them back,
and being told *no* on returning is that sentence contradicted on the next screen. It costs more,
too: a refusal needs a carrier and a string; a return needs neither.

**Let the second room open, and close the first one for them.** Its strongest case: it answers
`DEC-111` just as completely, it keeps the fresh-code capability this decision removes, and it is
what a player pressing *create* most literally asked for — a new room. Rejected because the first
room's code may already be in a rival's hands, so closing it lands *No duel room has that code.* on a
blameless stranger who did nothing but click the link they were sent. That is the shape `ADR-0105`'s
Alternatives rejected once already, for the same reason. It would also be the product's first
destruction of a room, which nothing in `docs/vision.md` asks for and every word of which `ADR-0073`
§5 refused.

**Read the report as being about the lifetime, and only raise `waitingMillis`.** Its strongest case:
it is the human's literal words — *"same room shoud be 'alive' for some time(like 3-5 min)"* — it is
one constant, it touches nothing else, and it is the only option in this set that could ship in an
afternoon. Rejected on measurement: the life is already ten minutes, twice the top of what was asked,
so the report cannot be about the number. What the player actually met was a **second room**, and a
longer first life makes that strictly worse — the orphaned room then holds the sent code for longer.

**Answer `DEC-139` and leave `DEC-111` open.** Its strongest case: `DEC-139` is one press by one
player and `DEC-111` is a general question about seats; `EPIC-14`'s own Definition of done permits
keeping it open with a reason; and the smallest answer is usually the right one here. Rejected
because the two are one question read from opposite ends. A waiting seat can be taken only by
`CreateRoom` — one production call site, one constructor, no transition back to `WAITING` — so
whatever a second `CreateRoom` does **is** the answer to how many waiting rooms a player may hold.
Leaving `DEC-111` listed open would leave open a question this decision had already answered by
construction, which is the open-and-answered-at-once failure `docs/adr/README.md` warns about by
name.

**Hand the room back and say so, in one new line.** Its strongest case, and it is the closest call in
this set: the control shipped saying `Create a duel room` and no room is created, so the label makes
a promise the press does not keep; a player who has not memorised eight characters cannot tell a
returned room from a fresh one; and `ADR-0110` §6's rule anticipates exactly this — a new string is
allowed, by a new ADR, and this is one. Rejected on *quiet*, and on two things the screen already
does: it renders `ADR-0073` §3's promise, which is the explanation that sentence would repeat, and it
renders the code, which is the fact. Worth recording that the label's half of the objection is being
removed independently — `STORY-1402` renames that control to `Play duel` on the human's own word, and
*Play duel* promises a duel rather than a fresh room — but this decision does not depend on that
rename and holds under either label. The tie-break is the vision's word, and the reversal is one
string and one card frame in either direction.
