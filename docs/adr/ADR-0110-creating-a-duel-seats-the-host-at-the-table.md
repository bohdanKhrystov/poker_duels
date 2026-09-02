# ADR-0110 — Creating a duel seats the host at the table, and the promises move with them

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-116` — does creating a duel land the host **at the table** rather than on the
  waiting screen, what stands in the rival's seat until they arrive, and what becomes of
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) §3's
  promise? Registered 2026-09-02 by [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md),
  item 5.
- **Where the answer came from:** the **direction is the human's**, stated 2026-09-02 and recorded
  verbatim — *"when player create a duel it shoud be redirected to room immidietly; copy
  link/invite btn shoud be where the table is(on the table, it shoud be drawn) and opponent icon
  shoud say:'waiting for oppent', after opponent join duel starts"* — and this ADR does not choose
  it. `EPIC-13` takes that feedback as *"the source, not the specification"*, so everything the
  report does not reach is decided here, from merged sources: the fate of the two shipped promises
  from `ADR-0073` §§1–3 and
  [`ADR-0105`](ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §2, which leans
  on them; the seat's exact words from [`docs/vision.md`](../vision.md)'s *Positioning* — *"The
  vocabulary is duelling, not gambling: challenge, duel, rematch, **rival**, streak, season"* —
  applied the way [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) and `ADR-0073`
  §2 apply it, one voice per action; what the empty table may show from `CLAUDE.md`'s *"The server
  is authoritative. A client may never assert a game fact"*
  ([`ADR-0002`](ADR-0002-server-authoritative.md)); and the invite's parts from the vision's first
  success condition — *"**Send a link.** She opens it in a browser."* — with
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) and
  [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md) §2.
- **Amends:** `ADR-0073` §6 only — the *Created — waiting for your rival* frame of
  `design/screens/create-duel.html` is retired with the screen it draws (§8). `ADR-0073` §§1–5
  stand **byte-unchanged in words and behaviour**: what moves is where they render, not what they
  say or do. §3's closing rule — *"a screen that needs a third string … the answer is a new ADR"*
  — is the rule under which this ADR exists, and §6 below carries it forward.
- **Upholds:** `ADR-0105` §2, explicitly — the ground it stands on (*"`ADR-0073` §3 told the host
  on screen that they may walk away and the room stays open"*) remains true after this ADR,
  because the telling moves with the host (§4).
- **Applies:** `ADR-0094` §§1–2 (the rival never waits; a code is typed on the first screen);
  `ADR-0022` (nothing is said *of* a code); `ADR-0072` §§4–6 (`forgetRoom`, the way back, no
  client clock against a server window); [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md)
  §3 and [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 (the drawing is
  the card's and the human's, and the card comes first);
  [`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) (the
  table's size budget, which the new frames live under).
- **Constrains:** `EPIC-13` item 5's story — its card ticket and every string it renders — and the
  `state.roomCode !== null && state.view === null` branch of the client. It constrains **no
  Kotlin, no wire type, no protocol version and no stored data**: `RoomJoined(code, seat)` already
  carries everything this state needs, and the transition out of it is the opening `Snapshot`,
  which already renders first.
- **Leaves open:** `DEC-111` (may a player hold several `WAITING` rooms — untouched in either
  direction, §7) and `DEC-119` (what the address names while a player holds a room — this ADR
  says nothing about the URL).

## Context

The host who creates a duel today lands on a dedicated waiting screen: a heading, the room code,
the invite link in a selectable box, a copy control, `Back to the lobby`, and one line of promise.
When the rival joins, that screen is replaced wholesale by the table. Five things are in tension.

**The human played it and reported the trip.** The feedback quoted in the header asks for the
table itself to be the place the host waits: the invite drawn on it, the empty seat saying so, the
duel starting in place when the rival arrives. `EPIC-13` treats that as the source rather than the
specification, which is why this is a decision and not a transcription — the report does not say
what happens to the two promises the waiting screen makes, and those promises are load-bearing.

**Two shipped promises are made on the screen being retired.** `ADR-0073` §2 put
`Back to the lobby` on it, and §3 put one line beside the control: *"The room stays open. That
link still works for your rival, and it brings you back."* `ADR-0105` §2 then leaned on exactly
that sentence to justify **not** refusing a `WAITING` seat when *Create a duel room* refuses a
`PLAYING` one: *"`ADR-0073` §3 told the host on screen that they may walk away and the room stays
open. Refusing here would make a shipped promise false."* A promise that silently stops being
said is the expensive failure here — the reasoning of a merged ADR would go on citing a sentence
no screen renders.

**`ADR-0073` §3 closed the waiting screen's string set at two, and named the way out.** *"A screen
that needs a third string is a screen that has outgrown this decision, and the answer is a new
ADR, not an invented sentence."* The seat label the human asks for is that third string. This is
the ADR that rule anticipated.

**The invite has three parts, and each is the fallback for the next.** Verified in
`Lobby.tsx`: the bare code (read aloud, and typed into the first screen's room code box —
`ADR-0094` §2 keeps that entry path, and `ADR-0022` chose Crockford base32 precisely so a code
survives being spoken); the link in a selectable read-only box (the component's own comment: *"the
invite is selectable text before it is anything else: the one interaction this product depends on
cannot need a working clipboard"*); and `Copy the link`, which renders **only** when
`navigator.clipboard` exists and otherwise leaves the box as the whole mechanism. Which parts move
to the table is open, and dropping one severs a join path the product's success condition depends
on.

**A table before the duel has no facts to show.** Until the rival joins there is no
`MatchState`, no `Snapshot` and no `PlayerView` — the client holds a room code and a seat number,
and nothing else. The server is authoritative and a client may never assert a game fact, so
whatever the host sees at the table cannot include stacks, blinds, cards, a pot or a dealer
button, however knowable those numbers feel from configuration.

**The rival is not part of this state at all.** `ADR-0094` §1: opening the invite renders no
screen of its own — the rival is seated, the duel starts, and the first thing they see is the
live table. So everything decided here is seen by exactly one person, and the transition out of it
is already built: `Lobby.tsx` renders `state.view !== null` ahead of `state.roomCode !== null`,
so the opening `Snapshot`'s arrival is what ends the wait, with no navigation and no wire change
under any answer.

### The deadline

`EPIC-13` is `backlog` until `DEC-114`–`DEC-119` are answered, and item 5 cannot be split without
this one. The split's first ticket is the card (`ADR-0091` §2, the epic's *Design first*), and the
card cannot be drawn until the product says what the frames contain — deciding after it is drawn
costs the drawing twice. Nothing else is urgent: no wire moves, and every string involved already
ships.

## Decision

### 1. Creating a duel lands the host at the table

The dedicated waiting screen is retired. From the moment `RoomJoined` answers `CreateRoom`, the
host is looking at the duel table — the same surface the duel will be played on — with the rival's
seat standing empty. There is no separate waiting surface and no screen swap when the rival
arrives: the table fills.

This is the state `state.roomCode !== null && state.view === null` already denotes; what changes
is what it renders, not when it exists or how it ends.

### 2. The rival's seat says `Waiting for your rival`

Verbatim, capital *W*, no full stop — byte-identical to the heading the waiting screen renders
today, relocated to the seat. It renders **once**, at the seat that will become the plate the
table already names `Your rival`, and not additionally as a heading.

The human's reported words were *"waiting for oppent"*. The product's word for this person is
**rival** — the vision's *Positioning* names it in the duelling vocabulary, the shipped screen
already says *Waiting for your rival*, and the plate that fills this seat already says
*Your rival*. Introducing *opponent* here would give the same person two names on the same
surface, which is the failure `ADR-0046` opens by naming and `ADR-0073` §2 refused for
`Back to the lobby`. If the human wants *opponent* verbatim, that is one string and one card
frame to change — recorded so the reversal is known to be cheap.

### 3. The table states no game fact before the `Snapshot`

While the rival's seat is empty the table shows **no stack, no blind, no card, no pot, no dealer
button and no action bar** — nothing numeric and nothing dealt. No duel exists yet, the server
has stated no fact, and a client may never assert one; a starting stack read from configuration
would be the client's guess wearing the server's voice. The host's own seat may carry its shipped
name, `You`, and nothing more.

What the empty table *is* made of is therefore: two seats — the host's, and the empty one
carrying §2's line — the invite (§5), and the way out with its promise (§4). How that is composed
and drawn is the card's (§8).

### 4. Both promises move with the host, verbatim, and keep being made

The host-alone state carries, exactly as `ADR-0073` §§1–3 decided them:

- **`Back to the lobby`** — the same words, the same act: `forgetRoom()` from an event handler,
  then the first screen. Nothing about the control's meaning changes, and `ADR-0073` §5's refused
  words stay refused.
- **`The room stays open. That link still works for your rival, and it brings you back.`** —
  placed with the control, as §3 placed it. Every clause survives the move still true: nothing is
  sent, so the room is untouched; a rival's join is still `Room.join`'s `WAITING` branch; and the
  host's own link still answers `ALREADY_SEATED` as a fresh `RoomJoined`, landing them back in
  this state — or, if the duel began meanwhile, at the live table through `resume`.

This is the section that keeps `ADR-0105` §2 standing. That ADR declines to refuse a `WAITING`
seat *because the host was told, on screen, that walking away is safe*. The telling continues, at
the same moment — host alone, room `WAITING` — in the same words. Retiring the screen retires
none of what it promised.

### 5. The invite moves whole: the code, the link box, and the copy control

All three parts render at the table in the host-alone state, with their shipped strings and
their shipped fallback behaviour: the bare room code; the `Invite link` label with the selectable
read-only box; and `Copy the link` with its two feedback lines, `Link copied.` and
`Copy it from the box above.`, the control absent where `navigator.clipboard` is.

All three, because each is load-bearing on its own: the link is the success condition's own verb;
the box is the invite where no clipboard works; and the bare code is the read-aloud path —
`ADR-0022` bought a typable alphabet for it and `ADR-0094` §2 keeps the first screen's box that
receives it. Dropping any one severs a shipped way in.

Two constraints carry over unchanged. Nothing is said **about** the code — no validity mark, no
liveness claim beyond §4's line, per `ADR-0022`'s no-oracle discipline, which `room-link.ts`
already records in its own comment. And nothing prints a duration, countdown or expiry — the
client owns no clock against `waitingMillis` (`ADR-0072` §6).

### 6. The state adds no new string

Every string the host-alone table renders is one that already ships, relocated:
`Waiting for your rival`, the code itself, `Invite link`, the link itself, `Copy the link`,
`Link copied.`, `Copy it from the box above.`, `Back to the lobby`,
`The room stays open. That link still works for your rival, and it brings you back.`, and `You`.
That enumeration is exhaustive. A state that needs one more string has outgrown this decision,
and the answer is a new ADR, not an invented sentence — `ADR-0073` §3's rule, carried to the
surface that replaces its screen.

### 7. The arrival is the `Snapshot`, and it is silent

When the rival joins, the duel starts — already true (`Room.join` → `PLAYING`,
`MatchState.start`) — and the opening `Snapshot` fills the table: the empty seat becomes the
`Your rival` plate, cards are dealt, and the invite, §2's line and §4's control and promise leave
with the state that carried them. No announcement string is added; the seat filling and the hand
arriving state the fact better than a sentence could, and *quiet* is the vision's word. The
rival sees none of this state, ever (`ADR-0094` §1).

Nothing here touches how many `WAITING` rooms a player may hold. The host-at-the-table state is
reached the same way, ended the same way, and forgotten the same way as the screen it replaces,
so `DEC-111`'s question is exactly as open after this ADR as before it.

### 8. The states the card owes, and what stays the card's

The epic's *Design first* rule applies: the implementing story's first ticket is the card, merged
before any implementing ticket is startable, drawing **every state it has, named**. This decision
creates these:

1. **Host alone at the table** — the frame this ADR is about, in four named variants: at rest;
   after `Link copied.`; after `Copy it from the box above.`; and with no clipboard API, where
   the copy control is absent and the box is the invite.
2. **The moment the rival arrives** — which is the live table `design/screens/duel-table.html`
   already draws; the card owes the transition nothing more than showing that the waiting
   furniture is gone from it.

The frames live under `ADR-0103`'s size budget and its card constraints — the fit at 390 × 664
is the card's to prove, and this state spends none of the live table's budget on an action bar, a
board or a pot line, which is the room it has to spend on the invite. How the empty seat is drawn,
where the invite sits on the table, and what weight each element carries are **taste, and taste
is the human's** (`ADR-0024` §3), given by looking at the rendered card. `ADR-0073` §6's frame —
*Created — waiting for your rival* in `design/screens/create-duel.html` — is retired with the
screen; the front door's own frame in that file is untouched.

## Consequences

**What it buys.** The host waits where the duel will happen, and the product's central moment —
the rival arriving — is a table filling rather than a screen being torn down: no navigation, no
swap, nothing for the eye to re-find. Both shipped promises keep being made at the moment they
matter, so `ADR-0105` §2 cites a sentence a screen still renders. The whole change is a render
branch and relocated strings — no Kotlin, no wire type, no version step, nothing stored — which
makes it the cheapest shape of this decision to reverse, and with no players yet that is the
right side to err on.

**What it costs.**

- **The table acquires a state in which it is not a duel.** Every future table surface — the turn
  clock, the chips, the act-just-made line, all of them this same epic — must now say what it
  shows when `view === null`, or be absent from that state on purpose. That is a standing tax on
  every card and test that touches the table, paid first by this epic's own items.
- **The card must prove the fit, and this ADR asserts room it has not measured.** §8's claim that
  the absent action bar, board and pot line leave space for the invite at 390 × 664 is an
  argument, not a reading. If the card cannot fit all three invite parts inside `ADR-0103`'s
  frame, that finding reopens §5 here — it does not license quietly dropping a part.
- **Churn in what already ships.** `Lobby.test.tsx`'s waiting-screen pins move with the strings;
  `design/screens/create-duel.html` loses a frame and the design tree goes briefly out of step
  with the client, the cost `ADR-0024` accepts and `ADR-0073` §6 already paid once on this exact
  frame.
- **An empty table can read as a broken one.** The host's first sight of every duel is now a
  table with no numbers on it. §2's line and the invite are the whole explanation, and §6 forbids
  adding a soothing sentence without a new ADR. That friction is bought on purpose — it is what
  keeps this surface from accreting copy — but it is friction, and it will be felt the first time
  someone wants the table to say more.
- **The promise strings keep their unenforced coupling.** `Back to the lobby` still renders in
  two components and §4's line in one, and nothing fails the day somebody rewords one of them.
  `ADR-0073` named this cost; the move neither pays it down nor worsens it, and it transfers to
  the new surface.

**What it forecloses.** The host-alone table can never show fabricated pre-duel facts — §3 is not
a styling choice but `ADR-0002` applied, so a future "preview stacks" idea is an amendment to a
foundational rule, not a card revision. The arrival stays silent unless a new ADR says otherwise.
And nothing about `DEC-111` or `DEC-119` is foreclosed in either direction: this ADR changes what
the waiting state renders, and deliberately nothing about how many may exist or what the address
says while one does.

## Alternatives considered

**Keep the dedicated waiting screen, and add the invite polish there.** Its strongest case: the
two promises stay exactly where the shipped tests pin them, `ADR-0105` §2's ground is untouched
without argument, no size-budget question arises, and a screen whose one job is getting the link
out arguably does it better with nothing else drawn. Rejected because it declines the human's
stated direction without a reason the vision supplies — the report is the source, and the only
product argument against it is cheapness, which is not one. It also keeps the trip: a screen
swap at the exact moment the product's success condition centres on, where the same table filling
in place says *the duel has begun* without moving the host's eye.

**Land at the table with the knowable facts drawn — starting stacks and blinds from the duel's
configuration.** Its strongest case: the table looks real immediately rather than hollow, the
numbers are not guesses in any practical sense — `docs/duel-rules.md` fixes them — and the host
learns the stakes while they wait. Rejected on the foundational rule: the client would be
asserting game facts the server has not stated, and the day a configuration drifts, the first
thing every host sees of every duel is a lie told in the server's voice. *The server is
authoritative* is not a style preference, and no layout benefit outbids it.

**Label the seat as the human typed it: *Waiting for opponent*.** Its strongest case: they are
the human's own words, *opponent* is plain English carrying no casino freight, and honouring a
report verbatim is the cheapest way to be sure it was honoured at all. Rejected because the
product already has a name for this person, in the vision's vocabulary list and on the very plate
this seat becomes — *Your rival* — and one person with two names on one surface is the
different-voice failure `ADR-0046` names. The report is the source, not the specification, and
this is the one place this ADR normalises it; §2 records the one-string cost of being overruled.

**Move only the copy control; drop the bare code and the link box from the table.** Its strongest
case: it is the minimal reading of *"copy link/invite btn shoud be where the table is"*, it puts
the least furniture on a size-budgeted surface, and the copy button is the path nearly every host
uses. Rejected because the other two parts are not decoration: without the box the invite does
not exist in a browser with no clipboard API — `CopyLink` renders `null` there, by design — and
without the visible code the read-aloud path keeps its entry half (`ADR-0094` §2's first-screen
box) while losing the half that produces the thing to say. A fallback chain with its middle
removed is not a smaller version of itself.

**Retire §4's line and ship `Back to the lobby` alone, as the result screen does.** Its strongest
case: it is the quietest option, the table is already carrying more than the old screen did, and
`ADR-0073` itself records that the result screen's identical control needs no explanation.
Rejected because the two surfaces still differ exactly as `ADR-0073` argued: here the room is
live, a link may already be in somebody's hand, and the natural reading of a way out is that it
took the room with it. And a merged ADR now leans on the line being *said* — `ADR-0105` §2 cites
the telling, not merely the fact. Unsaying it would leave a shipped ADR's reasoning pointing at a
sentence nothing renders, which is this decision's named worst outcome.

**Announce the arrival — a *Your rival has joined* notice before or over the first hand.** Its
strongest case: the arrival is the most important moment in the product, and stating it in words
is the honest, legible thing; the fraction of a second in which a seat fills is easy to miss.
Rejected because the filled seat, the dealt cards and the action bar appearing *are* the
announcement, made of facts rather than copy; *quiet* is the vision's own word for the register;
and the string does not exist, so inventing it here would break §6 in the same breath that
creates it. If a card ever shows the moment genuinely reads as unexplained, that is a new ADR's
case to make.
