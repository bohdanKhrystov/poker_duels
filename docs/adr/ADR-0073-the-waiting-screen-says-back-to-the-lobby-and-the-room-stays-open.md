# ADR-0073 — The waiting screen says *Back to the lobby*, and the room stays open

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-068` — does the *waiting for your rival* screen offer a way out, and what does
  it say?
- **Where the answer came from:** [`docs/vision.md`](../vision.md), *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal. The
  vocabulary is duelling, not gambling."* That is the sentence
  [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) derived its whole string set
  from, and it is the one that makes a screen with no exit indefensible here: a room you cannot
  leave is the casino's design, and *fast* and *minimal* are the two words a ten-minute wait for a
  reaper contradicts. What happens to the **room** is not decided here at all — it was decided by
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md), which gives a `WAITING` room one way to
  end, an idle timeout, and no other. This ADR applies both and adds no commitment
  `docs/vision.md` does not already make.
- **Builds on:** [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §4
  (`forgetRoom`, named for the mechanism because nothing is sent) and §5 (the way back forgets, then
  navigates); `ADR-0046` §5 (the words this product refuses, and why); `ADR-0022` (a waiting room
  lives `waitingMillis` and is then reaped)
- **Constrains:** [`STORY-0314`](../../tasks/stories/STORY-0314-a-host-can-leave-the-room-they-opened.md)
  — every string it renders — and the *Created — waiting for your rival* frame of
  `design/screens/create-duel.html`. It constrains no Kotlin, no wire type, no protocol version and
  no stored data.
- **Leaves open:** whether the product ever lets a host *close* a room, so the invite stops working
  (Consequences); and the `mySeat`/`roomCode` gap this control makes reachable, which is a defect
  and a ticket, not a decision.

## Context

The screen a host lands on after creating a room has one control on it, and it is *Copy link*. Six
things are in tension, and the first is not a hypothesis.

**The screen is a trap, and every route out of it was checked in source rather than assumed.**
`boot.ts` writes `pd.roomCode` on `RoomJoined`, so the tab is remembered in the room. A reload
re-`JoinRoom`s it. `RoomRegistry.resume` answers `null` for a `RoomState.WAITING` room — a waiting
room has no duel to resume — so the rejoin falls through to the ordinary join. `Room.join`
(`Room.kt`, the `RoomState.WAITING` branch) refuses a player who already holds a seat with
`ALREADY_SEATED`, and `DuelSocket.replyToJoinRoom` answers that refusal *exactly as a fresh seating
would*, with `RoomJoined(code, seat)` derived fresh from the registry. The store sets `roomCode`,
and `Lobby.tsx`'s `state.roomCode !== null` branch puts the same waiting screen back up. So the
routes out are: a rival joins; the room is reaped as idle after `RoomTimeouts.waitingMillis`; or the
player clears browser storage. That is not a bug in one function — it is four correct behaviours
composing into a room with no door.

**The mechanism is already built, and nothing else in the client is.** `ADR-0072` §4 shipped
`DuelClient.forgetRoom` and `useForgetRoom()`, and §5 wired the first control that calls it. This
decision costs one control and its words; six days ago it would have cost an interface.

**Nothing is sent, so most of the obvious words are false.** `ADR-0044` ships no `LeaveRoom`, no
seat is vacated, and the server is not told. A control saying *Cancel*, *Close the room* or *Delete
the room* asserts that the room is gone when `Room.join` will still seat a rival at that code. A
control saying *Leave* asserts the seat is vacated when `Room.seatOf` still names the host. This is
worse than `ADR-0046` §5's standing objection — that copy must not name a cause nothing knows.
Here the copy would name a fact that is known to be **false**, in a client whose entire claim is
that the server is the only thing telling the truth.

**The client already has words for exactly this action, one screen along.** `DuelResult.tsx`
renders `Back to the lobby` on the anchor that `ADR-0072` §5 wired to `forgetRoom`. Two controls
that do the same thing to the same memory, in the same client, reading differently is the failure
`ADR-0046` opens by naming: *"a new state that arrives in a different voice reads as a different
product."*

**The press has a consequence a second person can act on.** The host may already have sent the
link. Whatever this control does, that link keeps working for the rest of the room's life, and a
rival who opens it is seated and the duel starts (`Room.join` → `PLAYING`, `MatchState.start`). A
control that leaves the player believing they closed the room is a control that misinforms them
about somebody else's next few minutes.

**The room's clock is not the client's.** `waitingMillis` is a server configuration read against
`lastActivityAt`, and `ADR-0072` §6 settled that this client runs no timer against a window the wire
does not carry. So no string here may print a duration, a countdown, or a time of death.

### The deadline

`STORY-0314` is `blocked` and unsplittable — the planner has verified the mechanism and states
outright that only the words are missing — and it blocks nothing else, so nothing is stalled behind
it today. Two reasons to answer now, and neither argues for a particular answer.

**Strings acquire tests here**, `ADR-0046`'s own argument: `TASK-030802` pins four verdicts by name,
`TASK-030710` pins the absence of five. Choosing the words after the rendering tickets exist costs
the words *and* the tests that quote them.

And **`Back to the lobby` has already shipped.** If a second phrase for the same action lands on
this screen first, consolidating later means changing two components, two design frames and the
tests that quote both. Today it costs a paragraph.

## Decision

### 1. The screen has a way out

The *waiting for your rival* screen carries one control that reaches the lobby in this tab. It
calls `forgetRoom()` from an event handler, `ADR-0072` §4 and `ADR-0032` §3, and the shape of the
navigation is `STORY-0314`'s and already settled.

### 2. The control says **`Back to the lobby`**

Verbatim, capital *B*, lower-case *lobby*, no full stop — byte-identical to the string
`DuelResult.tsx` already renders. It is the product's phrase for *this action*: forget the room this
tab is seated in, and go to the first screen. Where that action appears, it reads the same.

It states where the player goes and asserts nothing about the room, which is the only class of
phrase that is true here.

### 3. It does nothing to the room, and exactly one line says so

The room stays `WAITING`, the host keeps seat 0, the code keeps resolving, and the room ends the one
way `ADR-0022` gives it — the idle timeout. The control changes none of that, and the screen states
it in one line, placed with the control:

> `The room stays open. That link still works for your rival, and it brings you back.`

Every clause is checked against the tree: the room is untouched because nothing is sent; a rival's
join is `Room.join`'s `WAITING` branch seating them; and the host's own follow of that link is
`ALREADY_SEATED` answered as `RoomJoined(code, seat)`, which lands them back on this screen, or —
if the duel has begun meanwhile — on the table, through `resume`.

**The line names no duration and no deadline**, per the Context's last force. When the room is
finally reaped, the correction is already shipped and needs no new string: the code answers
`UNKNOWN_ROOM` and `Lobby.tsx` prints *No duel room has that code.*

**These two strings are the whole of the addition.** `STORY-0314` renders the control, renders that
line, and adds no third string. A screen that needs a third string is a screen that has outgrown
this decision, and the answer is a new ADR, not an invented sentence.

### 4. No confirmation, of any kind

No dialog, no *are you sure*, no second press, no undo toast. The action destroys nothing: the room
stands, the seat is held, and §3's line names the way back. A confirmation would assert that
something irreversible is about to happen, which would be the same lie as *Cancel* — told more
slowly. *Dark, quiet, fast, minimal.*

### 5. The words this refuses, and why

| Not used | Because |
| --- | --- |
| *Cancel*, *Cancel the room*, *Cancel the duel* | claims the room is gone. `Room.join` will still seat a rival at that code |
| *Close the room*, *Delete the room*, *End the room* | same claim, and *close* additionally suggests a server was told. None was |
| *Leave*, *Leave the room*, *Leave the duel* | claims the seat is vacated. `Room.seatOf` still names the host, and their own link brings them straight back. `ADR-0072` §4 refused `leaveRoom` as a *function* name on this ground; a string a player reads earns the objection twice over |
| *Give up*, *Abandon*, *Withdraw*, *Forfeit* | asserts an intention about a duel that has not started, and *forfeit* is false besides — nothing is lost |
| *Back* alone | it is what the lobby's two panel swaps say, and those change nothing and go nowhere. This one navigates and forgets. It also collides with the browser's own *Back*, whose behaviour is `DEC-054`'s and is not yet decided |
| *Cash out*, *Exit table*, *Stand up*, *Sit out* | the casino's furniture. `ADR-0046` §5 already refuses *sitting out* by name: two seats, never three |
| any duration, countdown or expiry time in the line | the client owns no clock against a server window (`ADR-0072` §6) |

### 6. The design frame gains the control and the line

`design/screens/create-duel.html`'s *Created — waiting for your rival* frame shows a screen with no
way out, which currently documents the trap as though it were intended. It gains the control and
§3's line, carrying these words verbatim. **Where they sit, what weight the control has, and
whether the line is a note or body text is `EPIC-06`'s** — this ADR fixes the words and the fact,
not the layout. `STORY-0314` does not wait on that frame: the words are fixed here, so the two
cannot drift apart while they are out of step.

### 7. What this does not decide

- **Whether a host may ever truly close a room**, so the invite stops working. That needs a frame on
  the wire, Kotlin in `EPIC-02` and a protocol version step, and nothing today asks for it. It is
  named in Consequences and left there.
- **Anything the rival reads.** Nobody is in the room to tell, and no frame could carry it
  (`STORY-0314`, *Out of scope*).
- **The element, the navigation and the storage write.** `<a href="/">` plus an `onClick` that
  forgets, `ADR-0072` §5, applied by `STORY-0314`'s design notes. The modifier-click cost
  `ADR-0072` recorded applies here unchanged and is not re-argued.
- **Addresses and browser *Back***, which are `DEC-054`'s for the whole client.

## Consequences

**What it buys.** A screen stops being a trap, for the price of two strings and a control that calls
a function already on the interface. `STORY-0314` becomes splittable the day this merges, and its
third acceptance criterion — *the words are the ADR's, not the implementer's* — is satisfiable
because both strings are quoted above. The client keeps one phrase for one action rather than
growing a second. And the host learns the one fact about their press that a second person can act
on: the link is still live.

**What it costs.**

- **A host who presses this while their tab is open is pulled into the duel with a store that never
  saw `RoomJoined`.** Verified, not predicted: `deliver` addresses frames by *player id* through
  `ConnectionDirectory.writerFor`, not by socket-room membership, so the host's new socket at the
  lobby receives the opening `Snapshot`, and `Lobby.tsx`'s `state.view !== null` branch — ahead of
  `state.roomCode !== null` — renders the table. But `duel-state.ts` sets `mySeat` and `roomCode`
  **only** on `RoomJoined`, which that socket never got: so the duel plays with `mySeat` null, the
  result screen loses the seat it needs, and a reload does not rejoin. This control does not create
  that path — closing the tab reaches it too — but it is the first control that makes it a
  one-press, supported route, and this ADR makes it reachable without fixing it. It is a defect and
  a ticket; it is recorded here so the next reader of `STORY-0314` finds it rather than discovers it.
- **`The room stays open.` is true and incomplete, deliberately.** It stays open until the reaper
  takes it, and §3 forbids saying when. A host who presses this and comes back an hour later finds
  the link dead with no warning they were given in advance — corrected honestly, one round trip
  later, by *No duel room has that code.*, but corrected after the fact.
- **A rival who opens the link after the host has walked away gets a duel with an absent seat.** The
  room is `WAITING`, `join` starts it, and if the host's tab is closed the server plays seat 0 by
  the rules `ADR-0013` and `ADR-0023` already set — and the host can lose a duel and a coin
  (`ADR-0014`) without seeing a card. This too predates the control, and §3's line arguably
  *encourages* the state by telling the host their rival can still join. That trade is taken with
  eyes open: the alternative is not telling them, which does not make the link any less live.
- **Two components must now be changed together.** `Back to the lobby` lives in `DuelResult.tsx` and
  in `WaitingForRival`, and §2 makes them one decision rather than two strings. Nothing enforces
  that. The day somebody rewords one, the other is a grep away and nothing fails if they miss it.
- **A third string on this screen now needs an ADR.** §3's closing rule is real friction: a future
  ticket that genuinely wants one more line pays a document for it. Bought on purpose, because
  `STORY-0314`'s criterion is otherwise unenforceable, but it is friction and it will be felt.
- **The design tree goes briefly out of step.** §6 puts the frame in `EPIC-06`'s hands and lets the
  client ship first, so for some window `design/` shows a screen the client no longer has. `ADR-0024`
  has design follow the code workflow; this is that cost, arriving on a specific screen.

**What it forecloses.** Nothing on the wire, nothing in the store, nothing stored, no schema and no
version. It forecloses one thing on purpose: **this control never quietly becomes a room-closing
control.** If the product ever decides the invite should die with the host, that is a frame, a
server change and a protocol step — and it must arrive with *different words*, because
`Back to the lobby` will have stopped describing it.

**Why this shape, on thin evidence.** Strings are the cheapest thing this product owns to reverse —
no migration, no stored data, nothing a player keeps — and reusing one that already ships is cheaper
still, because it adds no vocabulary at all. Every heavier answer considered below either put a
falsehood on the screen or spent a protocol version. There are no players yet; the right move is the
one an afternoon undoes.

## Alternatives considered

**`Cancel` — the word every lobby in every game uses for this.** Its case is genuinely the
strongest of the six: it is instantly understood, it needs no supporting line at all because
everyone already knows what cancelling a room means, it is one word on a screen the vision wants
minimal, and it matches the host's actual *intention* — they are cancelling their plan to duel.
Rejected on the gap between the intention and the fact: the room is not cancelled, the code still
resolves, and a rival who opens the link ten seconds later is seated into a duel the host believes
they called off. A word understood instantly and wrongly is worse than a word that takes a second
longer, and this product's whole position is that the server is the only thing telling the truth.
It becomes the right word the day a `LeaveRoom` frame exists, and it should be reconsidered then.

**`Leave` — honest about the player's action, silent about the room.** Its case: it says what the
player is doing rather than what the software is doing, which is usually the better copy rule; it is
shorter than `Back to the lobby`; and it sidesteps *Cancel*'s false claim about the room being
destroyed. Rejected because it makes the smaller false claim instead: the player does not leave the
room, they leave the *screen*. The seat is theirs, the room still has them in it, and the very next
thing §3 has to admit is that the link brings them back — a line that reads as a contradiction under
a control labelled *Leave*. `ADR-0072` §4 refused `leaveRoom` for a function that no player ever
sees; the objection does not weaken when the audience is the player.

**`Back` — matching the two swap buttons the lobby already ships.** Its case is consistency with
what is on screen today: `Lobby.tsx` renders `Back` for the history screen and `Back` for the
ladder, so a third `Back` is the pattern, not an exception, and it is the shortest honest word
available. Rejected because those two are panel swaps that change nothing and go nowhere — pressing
them is free and reversible in the most literal sense. This one navigates the browser and deletes a
memory. Giving both the same word teaches the player that the word means *nothing happens*, at the
one place where something does. It also collides head-on with the browser's own *Back*, which
`DEC-054` has not yet decided the behaviour of.

**No way out at all: keep the screen as it is and let the ten-minute reap be the exit.** Its case is
not nothing. The screen has exactly one job — get the link to the rival — and every control that is
not *Copy link* competes with it; a host who creates a room and immediately abandons it is a rare
path; and the reaper genuinely does clean up, so nothing leaks. `ADR-0072` shipped without closing
this and the product survived. Rejected on the vision's *Positioning* sentence, which is the whole
licence for this ADR: a screen a player cannot leave is what the reference points do not do and what
the thing being refused does. Ten minutes of a browser tab is not *fast*, and *clear your browser
storage* is not an exit any player will find.

**A confirmation before leaving — *"Your rival can still join. Leave anyway?"*** Its case: it puts
the one consequence that matters directly in the player's path at the moment of the decision,
instead of in a line beside a control they may not read; the host may already have sent the link, so
this is not a private action; and it is the standard treatment for an action with an audience.
Rejected because the action is not destructive — the seat is held, the link works, and the room is
one press away — and a dialog asserts that it is. It would also be the client's first modal, which
is a component, a focus trap and a set of decisions about dismissal, bought for a press that undoes
itself. Recorded because it becomes the right treatment the moment a control actually destroys the
room.

**Say nothing about the room: ship `Back to the lobby` alone, as the result screen does.** Its case
is the strongest argument against §3: the result screen's identical control carries no explanation
and nobody has missed one; the vision asks for *quiet*; and a line of body text on a three-element
screen is the kind of accretion *minimal* is meant to prevent. Rejected on the difference between
the two screens. On the result screen the duel is over and the player's leaving concerns nobody
else. Here the room is live, a link may already be in somebody's hand, and the natural reading of a
way out is that it took the room with it. The line is also what makes §4's *no confirmation*
defensible: without it, the only thing telling the player the press is safe is the press.

**A stronger warning instead of a statement — *"Leaving does not close the room."*** Its case: it
answers the exact wrong belief the control creates, in the player's own terms, in five words.
Rejected because a negation makes the reader construct the fact themselves, and half of them will
construct *"so how do I close it?"* — a question this product has no answer to. §3's line states
what is true in the affirmative and adds the fact that makes the press safe, which the negation
cannot carry.
