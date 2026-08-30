# ADR-0094 — Opening the invite is taking the seat, and the two join cards are corrected to it

- **Status:** Accepted
- **Date:** 2026-08-30
- **Resolves:** `DEC-092` — does the product build the two screens its merged cards draw — an
  offered-seat confirmation on the invite path, and a dedicated code-entry screen — or are those
  two cards corrected to the join path that shipped? Registered and answered in the same PR (the
  `DEC-039` path), so the id never appears in an open table. `TASK-120907`'s first acceptance
  criterion — *"a `DEC` is registered in both registers and routed, before any diff exists"* — is
  met by the answered rows this PR writes into `docs/adr/README.md` and `tasks/BOARD.md`.
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  The licensing sentence is the first success condition — *"Send a link. She opens it in a browser.
  We play a full heads-up match. Someone wins. We hit Rematch."* — of which `docs/vision.md` then
  says *"Everything else is downstream of that moment."* It has five verbs and not one of them is
  *accepts*. Two merged ADRs apply that sentence and are what make this an application rather than
  an invention: [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) recorded *"holding a room
  code is the invite… Whoever presents it takes the second seat"* and priced its own defence as
  keeping *"joining… one click with zero friction for the invited"*;
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §1 and §4
  already describe the first screen as the screen that carries *"the join form"* and *"the room
  code box"*. `docs/vision.md`'s *Positioning* — *"Dark, quiet, fast, minimal"* — is the tiebreak
  where those two leave slack.
- **Builds on:** `ADR-0022` (the code is the invite; a code's life as an invite ends when the seat
  is taken); `ADR-0060` (the first screen is the door, and what it carries);
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §1 (what a card *is* —
  a reference a coder transcribes) and §3 (who authors one);
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 (the visual verdict stays the
  human's); [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
  §3a (conformance is the shipped screen against its merged card) and §5 (*"an answered question
  becomes a merged source"* — a decision that blesses what shipped closes the repeat mechanically)
- **Constrains:** `TASK-120907`, which becomes a `module: design` ticket rather than a client one;
  `design/screens/join-duel.html` and `design/screens/enter-code.html`; two rows of
  `docs/test-plan.md`'s screen table; and every later UAT round's conformance check on the `first`
  screen
- **Amends nothing.** No client change, no server change, no wire type, no `PROTOCOL_VERSION`
  step, no stored data, and no `expect` column in `docs/test-plan.md`. `SMK-05`, `CORE-02` and
  `CORE-05` stand byte-unchanged and keep passing.
- **Raises nothing for the architect.** `TASK-120907` routed a second, technical half — *what the
  wire tells a client that holds a code and no seat* — and made it explicitly conditional: it
  *"only arises if the first is answered yes."* It is answered **no**, so the question does not
  arise and no `DEC` is registered for it.
- **Leaves open:** the refusal *copy* divergence between these cards and the client (`TASK-120911`
  already owns it, and settles it the other way); `create-duel.html`'s front-door and waiting
  frames; and whether a pre-join view of a room ever becomes the right thing — §Consequences names
  the trigger that would reopen it.

## Context

Two merged artefacts describe two different products, and each one cites the same sentence of
`docs/vision.md` as its justification.

**The cards.** `design/screens/join-duel.html` (merged 2026-08-14, `TASK-060402`) draws an offered
seat: *ImKate challenges you*, a stakes line, a room-code chip, a *Playing as* namerow and a *Take
the seat* button. Its lede is *"The link opened. One decision on this screen and only one — take
the seat."* `design/screens/enter-code.html` (merged 2026-08-15, `TASK-060406`) draws a screen of
its own for typing a code — a tracked mono code well, *"Eight characters, letters and numbers"*,
*Open the duel* and *Back* — and exists because `create-duel.html`'s front door draws an *I have a
code* button and nothing had drawn what it opens.

**The client.** `main.tsx` reads `?room=CODE` at boot and `boot.ts` sends `JoinRoom` the moment
`Welcome` arrives; `Room.join` moves the room to `PLAYING` and `MatchState.start` deals. So opening
an invite link seats the joining player into a dealt hand with blinds posted, no screen in between
and no decision of any kind — not even the single click the card promises. And a code is typed into
an inline field on the first screen, beside *Create a duel room*, with the refusal printed above
the still-visible lobby. Both behaviours shipped 2026-08-15 (`TASK-030509`, `TASK-030510`) — within
a day of the cards, on a track that never met them.

**The two artefacts claim the same licence.** The card's own margin says the frame exists because
*"No form stands between her and the table (docs/vision.md's success sentence depends on it)"* —
and then draws a form. That collision is the decision.

### The forces

**The finding recurs by construction.** `ADR-0092` §3a makes conformance *"the shipped screen
against its merged card"*, so while both cards stand, every round that walks the `first` screen
files the same `high` and is right to. Three rounds have — `STORY-1205`, `STORY-1209`,
`STORY-1210`. Nothing in the loop can stop that except a merged source saying which artefact is the
product.

**The card asks for facts the wire does not carry.** *ImKate challenges you* needs the host's
display name, and the stakes line needs the room's terms, **before** a seat exists. An invite
carries `?room=CODE` and nothing else, and the act that would fetch them — `Room.join` — is the act
that starts the duel. Building this screen therefore costs a new protocol step and a
`PROTOCOL_VERSION` claim before it costs a pixel.

**And most of what the screen would show is a constant.** *Heads-up hold'em · freezeout · winner
takes the coin* is true of every duel this product will ever run — heads-up by `docs/vision.md`,
freezeout by [`ADR-0035`](ADR-0035-a-duel-is-a-freezeout.md), the coin by
[`ADR-0014`](ADR-0014-duel-coin-economy.md) — and `CreateRoom` carries no fields, so nothing about a
room varies for a guest to weigh. The one genuinely room-specific fact, who challenged you, arrives
on the seat plate a second later.

**But the seat is not free, and that is what makes this a real decision.** `ADR-0014` gives the
winner a coin and takes one from the loser — *"a balance… may be negative"* — and names that as
*"a real stake in both directions."* A player seated by a link they clicked has staked a coin
without ever being asked, and the vocabulary the vision itself chose is *challenge*, which is a
word whose ordinary companion is *accept*. `ADR-0022` reaches for **Lichess challenge links** by
name as its model, and a Lichess challenge link does show a page with two buttons on it.

**Against that, three merged sentences.** The success condition contains no acceptance step, and
the vision says everything else is downstream of it. `ADR-0022`'s Consequences already priced the
invited player's experience as *"one click with zero friction"* — one click, and it is the link.
And `ADR-0060`, written a week **after** these cards, describes the first screen as carrying *"the
create button, the join form, the strip and the name surface"* and has the way back from the record
restore it *"exactly as it was — same store, same room code box, same strip."* A separate
code-entry screen makes that sentence false. The enter-code card was already answered against by a
merged ADR, and nobody noticed.

**The rules run one way, and this is the direction they do not cover.** `ADR-0091` §2 says a new
screen owes a card. Nothing anywhere says a card owes a screen — and a card is defined in §1 as
*"a versioned, rendered, human-accepted reference that a coder transcribes."* A reference to
something that does not exist is not a reference.

### The deadline

`TASK-120907` is filed, `backlog`, and stated in its own text to be unstartable: whether it is a
client ticket or a design ticket is exactly what is undecided, so the planner cannot split it and a
coder handed it would have to invent a protocol step, which `CLAUDE.md` rule 5 forbids. It shares a
file with `TASK-120911`, also `backlog`, whose `verify:` block greps `design/screens/enter-code.html`
by path — so the longer both sit, the better the odds that whichever lands second silently undoes
the first. Neither reason argues for a particular answer; both argue for one now.

## Decision

### 1. Opening an invite link seats the player, and nothing stands between the link and the table

The invite path renders **no screen of its own**. A player who opens `…/?room=CODE` is seated, the
duel starts, and the first thing they see is the table. There is no confirmation, no *Take the
seat*, no accept-or-decline, and no pre-join view of the room, in v0.1.

Holding the code is the invite (`ADR-0022`); presenting it is taking the seat. The acceptance
happened in whatever conversation carried the link — which is precisely the picture the vision's
success condition draws, and the reason it has no sixth verb in it.

### 2. A room code is typed on the first screen, and nowhere else

The join-by-code field lives on the first screen beside *Create a duel room*, and its refusals are
shown there. There is no second screen for entering a code, and no route that leaves the first
screen in order to type one.

This is not new: `ADR-0060` §1 and §4 already say the first screen carries the join form and the
room code box. What was missing is a merged source saying the card that disagrees is the thing that
is wrong.

### 3. The cards are what changes; the client does not

`web-client/` is untouched by this decision. So is `poker-server`, so is the wire, and so is every
`expect` column in `docs/test-plan.md`. `SMK-05` (*"B is seated at the table without typing a
code"*, walked as `open <link>` then `wait "Blinds"`), `CORE-02` and `CORE-05` describe the product
this ADR blesses, and they keep passing unchanged.

`design/screens/join-duel.html` and `design/screens/enter-code.html` are corrected to draw the
product. That repair is `TASK-120907`, rewritten by the planner as a `module: design` ticket, and
composed from the settled vocabulary as an ordinary dispatched ticket under `ADR-0091` §3 — with
the human's visual verdict on the rendered result, which may trail the merge, exactly as
`ADR-0024` §3 and `ADR-0091` §3 already provide.

### 4. What the corrected card set must be true of

Three things, and they are the whole of what this ADR fixes about the cards:

- **a. No merged card draws a step between an opened invite and the table.** The invite path has no
  screen, so it has nothing for a screen card to draw; what the joining player sees is the table,
  which `design/screens/duel-table.html` already draws. `join-duel.html` has no subject left. The
  ticket may delete it or repurpose its path, on one condition: **no register may be left citing a
  path that is gone** — `docs/test-plan.md`'s screen table has a row for *"joining by a shared
  invite link"* naming this file, and that row moves to the card that draws what she actually sees,
  in the same ticket.
- **b. The first screen's code field and its refusals stay carded, drawn where the client puts
  them** — inline on the first screen, not as a screen of their own. `enter-code.html` **keeps its
  path**: `docs/test-plan.md` cites it, `TASK-120911`'s `verify:` block greps it, and `ADR-0092`
  §4's dedupe key for missing-card tickets *is* the card's own path — deleting it costs three
  registers a repair and buys the correction nothing. What changes is what it claims to be: the
  first screen's join-by-code states, no longer a screen the player travels to.
- **c. Which frames the corrected cards carry, in what order and at what size, is the design
  ticket's**, under `ADR-0024` §5 and the human's eye. This ADR fixes what is true, never what it
  looks like.

### 5. What this does not license

**The client's copy does not win by default.** This decision is about *which screens exist*, not
about *what words are on them*. Where a card and the client disagree over a **string**, the card is
still the human-accepted reference (`ADR-0091` §1) and the direction is decided string by string,
by whichever merged source owns it — which is exactly what `TASK-120911` does, and it happens to
resolve the enter-code refusal in the client's favour on `ADR-0072`, `ADR-0073` and `CORE-04`, not
on this ADR. A coder correcting these cards under §4 changes frames, not verbatim copy that a
merged source has settled elsewhere.

**And "the client shipped, so the client is right" is not a rule.** It is not why this went this
way. The shipped flow won because the vision's own success sentence and two merged ADRs describe
it, and the cards' flow contradicts one of them. Where a card and the client disagree and no merged
source picks a side, the card wins — it is the reference, and the human accepted it by looking.

## Consequences

**What it buys.** The recurring `high` closes for good: `ADR-0092` §5 makes an answered question a
merged source, so once the cards draw the product, a fourth round re-raising this would be
contradicting something merged, and the suppression is mechanical rather than remembered.
`TASK-120907` becomes startable and small — one design ticket instead of a protocol step, a wire
type, a version claim and a screen. The product keeps the one property the whole vision is built
around: a link, opened, is a duel.

**What it costs.**

- **A link is a commitment, and there is no way to decline one.** Whoever opens the link is seated.
  There is no *Back to the lobby* at the table — `ADR-0073` put that on the waiting screen and
  `ADR-0072` §5 on the result screen, not here — so a player who did not want the duel can only
  walk away and let [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md)'s
  absent-seat rules play the hands out, which costs them the coin `ADR-0014` stakes. A link
  forwarded into a group chat gives the seat to whoever clicks first, and the host cannot evict
  them: the room stays open (`ADR-0073`), the code is spent (`ADR-0022` §3), and their only move is
  to create another room. **This is the price of the decision and it is a real one**, accepted
  because the alternative buys a decline at the cost of a click on every honest join and a protocol
  step to feed it.
- **The joining player never learns who challenged them until they are already in.** *ImKate
  challenges you* does not exist in this product, and this ADR forecloses building it in v0.1. The
  rival's name arrives on the seat plate, or reads *No name* where there is none
  ([`ADR-0058`](ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)).
- **Accepted design work is thrown away.** Two cards the human graded by eye lose their frames.
  That is a real loss and it belongs on Product B's record: the client and the card set shipped a
  day apart and never met, and `ADR-0091` §2's rule would not have caught it, because it binds the
  direction *screen → card* and this mismatch ran the other way.
- **Two `backlog` tickets now touch one file.** `TASK-120907` and `TASK-120911` both modify
  `design/screens/enter-code.html`, and §4b keeps the path partly so the second to land does not
  silently undo the first. Whoever sequences them owns that; the ADR can only make the collision
  visible.
- **One register moves with the cards.** `docs/test-plan.md`'s screen table names both files. If it
  is not edited in the same ticket, round 3 walks a table pointing at a file that is gone — which
  would be a harness defect under `ADR-0092` §2, filed against `EPIC-12` and excluded from `B(N)`:
  cheap to fix, and cheaper still not to cause.

**What it forecloses.** A pre-join view of a room in v0.1, and with it the protocol step that would
have carried the host's name and terms to a client holding a code and no seat. The reversal trigger
is nameable now: **the day a duel's terms stop being constant.** `ADR-0035` leaves starting stack
and blind schedule as configuration, and if a host ever chooses them, the confirmation screen stops
showing a constant and starts showing a variable — at which point there is something real to accept
and this decision should be re-argued rather than cited. Until then it is chosen partly for being
the cheapest to reverse: not building the screen costs two card frames, while building it would
ship a wire fact that a later reversal cannot take back.

## Alternatives considered

**Build both carded screens.** Strongest case: it is the only option that treats a merged,
human-accepted card as binding, which is what `ADR-0091` §1 says a card is; it gives the coin
`ADR-0014` stakes a moment of consent before it is risked; it puts the host's name in front of the
guest at the one moment they most want it; and it can cite `ADR-0022`'s own Lichess analogy, since
a Lichess challenge link really does render an accept page. Rejected on the vision sentence and on
what the screen would actually contain: the success condition has no acceptance step, `ADR-0022`
already priced the invited player's join at *"one click with zero friction"*, `ADR-0060` §4 makes a
separate code screen contradict a merged ADR outright, and the confirmation screen's information
content is one constant plus one fact the table shows a second later — bought with a protocol step,
a version claim, and a break of `SMK-05` as written (its walk is `open <link>` then `wait
"Blinds"`, and *Blinds* would never arrive). The Lichess analogy also cuts thinner than it looks:
Lichess's accept page exists because a challenge carries **variables** — time control, colour,
rated or casual, variant — and here nothing varies.

**Build the offered-seat screen only, and keep the inline code field.** Strongest case: it takes
the half of the card set that is about consent — the half with a real stake behind it — and drops
only the half `ADR-0060` already contradicted, so it costs one screen instead of two and leaves the
first screen alone. Rejected: it carries the entire protocol cost of the full option (the name and
terms must still reach a seatless client) for the frame whose content is most nearly a constant,
and it is the one of the two that contradicts the licensing sentence most directly — the link
opens, and the match does not start.

**Build the enter-code screen only, and keep the direct seat.** Strongest case: it is cheap, it
needs no protocol at all, and the code well is the nicest thing in the card set — eight tracked
glyphs at display size, dashes holding their place, which an inline field cannot be. Rejected
because it is the half a merged ADR actually forbids: `ADR-0060` §4 restores the first screen
*"same room code box"*, and the vision's picture of accretion it refuses is *slot machines in the
lobby* — the door growing a second screen for a field it already holds is motion in the same
family. If the code well's typography is worth keeping, it is worth keeping **on** the first
screen, which §4b leaves the design ticket free to do.

**Delete both cards outright and card nothing here.** Strongest case: the shortest path to a green
round — no screen, no card, no conformance check, no finding. Rejected: the first screen's
join-by-code state is a state a player really reaches, and `ADR-0092` §3a can only judge it against
a card. Deleting the coverage would trade a conformance finding for an unjudgeable screen, and
`ADR-0091` §5 registers cardless surfaces as debt rather than as a resolution.

**Leave the question open and suppress the finding in the round.** Strongest case: no artefact is
thrown away, and the product has no players, so the evidence for either flow is thin. Rejected:
`ADR-0092` §3's classifier files an observation as a finding **precisely when** it contradicts
something merged, and this one does — suppressing it would mean teaching a round to ignore a real
contradiction, which is the one thing that would make every other finding untrustworthy. An open
question here is also not free: it is what makes `TASK-120907` unstartable, and it is why three
rounds spent a `high` slot on the same sentence.
