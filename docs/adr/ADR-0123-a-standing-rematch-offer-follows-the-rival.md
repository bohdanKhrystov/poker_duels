# ADR-0123 — A standing rematch offer follows the rival, and it never takes the screen

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-138` — **does a rematch offer follow the rival off the result screen, and as
  what?** Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 5.
- **Where the answer came from — two halves, and only one of them is chosen here.** The
  ***whether*** is **the human's, stated verbatim** on 2026-09-06 and recorded rather than
  decided: *"rematch flow - when rematch offered it shoud be shown as popup; it shoud appear on
  any screen, like if opp come back to lobby it or any other page in still shoud be shown"*. The
  ***what*** is **derived** from `docs/vision.md`: the success condition everything else is
  downstream of — *"We play a full heads-up match. Someone wins. **We hit Rematch.**"* — is what
  makes a rematch offer worth following a player onto another screen at all; and *Positioning* —
  *"Dark, quiet, fast, minimal."* — together with the one-sentence version's *"free of everything
  that makes online poker feel like a casino"* is what stops it seizing one. The human's word was
  *popup*; §2 reads it as **appears above the screen the player is on** and declines the half of
  that word that means **blocks it**. If a blocking dialog was meant, that is one clause here and
  one state on the card.
- **Pays, and does not reopen,** [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md)'s
  own named cost — *"a finished player on the ladder does not see a rematch offer arrive. No
  notice exists, and none is designed here."* §§2–4 of that ADR stand exactly as merged; this one
  designs the notice it deferred, for the finished room only.
- **Applies, and amends nothing in:**
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§2–6 (one intent, one room
  fact, an idempotent offer, silence as the decline, and `UNKNOWN_ROOM` as the frame that ends a
  rematch); `ADR-0112` §3 (a look-away keeps the seat, the memory and the socket) and §4 (a frame
  that seats a running duel overrules the chosen screen);
  [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) §5 (the
  standing ladder the client already reasons with);
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md) §1
  (no fact lives only in motion);
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §§2–3 (the surface owes
  a card, and a card that mints visual language is worked with the human).
- **Moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no server file, no
  engine change, no schema, no stored key, and **no new player-facing string** — every word §4
  puts on the surface is already merged.
- **Registers, and does not answer:** `DEC-144` — **the architect's** — the mechanism (§9).

## Context

**The offer has exactly one surface, and `EPIC-13` just made that surface optional.**
`RematchControl` is constructed in `Lobby.tsx`'s `state.outcome !== null` branch and handed to
`DuelResult` as its `rematch` prop; it appears in no other branch and in no other file.
`ADR-0112` §3 rules that a `FINISHED` room **honours** an ask for `duels`, `leaderboard`,
`account` or `sign-in`, and `ADR-0114` §5 built the ladder that serves it. So the product now
invites a player to leave the only screen on which a rematch can be seen, and a rival who offers
into that absence is offering into nothing. That is not a defect in `EPIC-13`'s work — it is the
consequence nobody drew.

**The fact is already on every screen; only the drawing is missing.** `ADR-0104` puts a frame on
the connection in the room it is about, and a player reading the ladder is that same connection.
The reducer proves it: `RematchOffered` is accumulated into `state.rematchOffers`
(`duel-state.ts:377-380`) with no reference to which screen is showing, and it is cleared only by
a `Snapshot` or a `DuelFinished`. Nothing has to reach the client that does not already reach it.
This decision is about a surface and nothing else.

**What is in tension is the shape.** The vision's word is *quiet*, and its defining negative is
the casino — a product that interrupts you because it wants something. Against that stands the
human's own requirement, which is that the offer be **unmissable**, and the fact that the offer's
window is short: a `FINISHED` room is reaped after `RoomTimeouts.DEFAULT_FINISHED_MILLIS`, five
minutes. An offer nobody sees for five minutes is *We hit Rematch* — the last beat of the
founding sentence — failing silently.

**The product currently states the loud half and hides the quiet half.** When both seats have
offered, the `Snapshot` moves the standing to `running`, `rulingOn` refuses the chosen screen, and
`Lobby.tsx`'s layout effect restores `/` before the paint: the player is **pulled to the table**
from wherever they were. That is merged, deliberate (`ADR-0112` §4) and shipped. So the product
already interrupts a player for the outcome they cannot influence, and says nothing about the one
moment where their answer is the whole point. Whatever this ADR decides, it cannot be *less*
present than what agreement already does.

**And the wire fixes what may honestly be said.** `ADR-0044` carries one fact — *an offer from
this seat stands* — and deliberately no withdrawal, no decline and no deadline. A surface that
counted down, or that reported a decline, would be stating something the server never sent. That
constrains this decision more than any preference does.

**The deadline.** This is the product's **first surface that sits above another screen**. Whatever
shape it takes becomes the pattern every later notice is measured against — a turn arriving, an
invite link, a rival returning — and each of those is a separate decision this one must not
pre-empt. Today the shape costs one card and one component; after two more notices copy it, it
costs a retrofit. It is also as reversible as a product decision gets: client rendering only, no
wire, nothing persisted, nothing a player keeps.

## Decision

### 1. A standing offer from the rival follows the player onto every screen they can be on while they hold the room

Every member of `Screen` except `first`: `duels`, `leaderboard`, `account`, `sign-in`, `verify`
and `reset`. **No carve-out** — the human's *"on any screen"* is honoured literally, and a rule
with two exceptions for two rare screens is complexity bought with nothing (§2 is what makes the
mailed screens safe, and the residual cost is named in Consequences).

**A player who has left the room is not followed, and cannot be.** *Back to the lobby* calls
`forgetRoom()` and loads `/`; after it this browser holds no room and no frame about one is
addressed to it. Walking to another screen is **not** leaving — `ADR-0112` §3 keeps the seat, the
tab's memory and the socket — and that distinction is the whole rule: **the offer follows a player
who is still in the room, and nobody else.**

### 2. It is a panel above the screen, and it takes nothing from the player

It appears over the screen the player asked for, in the product's own vocabulary. It is **not
modal**:

- nothing beneath it is scrimmed, disabled or made unclickable — the screen underneath stays
  usable, and the player may go on doing what they were doing;
- it takes no focus when it arrives and moves none;
- **no keypress the player was already making can answer it** — accepting is a deliberate press of
  its own control, so a player mid-password, mid-reset or mid-room-code cannot agree to a duel by
  pressing `Enter`;
- it makes no sound and does not blink; whatever motion introduces it is `ADR-0115`'s — the motion
  carries no fact, the panel's still form says everything, and `prefers-reduced-motion` stills it;
- it never retires itself on a timer (§5).

*Cannot miss* is bought by **standing there**, not by blocking. Unrequested seizure of a screen is
the casino's move, and *"Dark, quiet, fast, minimal"* is the sentence that rules it out. What the
panel may **not** do is hide: it is a panel, not a dot, not a badge and not a line tucked into a
corner — a surface a player has to go looking for fails the requirement that produced this
decision.

### 3. Only an **incoming** offer follows, and only where the room's own screen is not showing

Two boundaries, both deliberately narrow.

**Incoming only.** The panel exists when the **rival's** seat has offered and this player has not
answered. A player's *own* standing offer — the merged *Rematch offered — waiting for your rival*
— follows them nowhere. They know what they pressed; a chip trailing them around the product
states nothing they do not already know, and if their offer is matched the table arrives by itself
(§6).

**Where the room's own screen is not.** The result screen keeps `RematchControl` exactly as
merged, unchanged in behaviour and in words. The panel is on screen precisely when a chosen screen
is — the case `ADR-0114`'s `shown` already names — so **one fact never has two live surfaces at
once**.

### 4. It says what the result screen says, and carries two controls

No new words are minted. The panel carries:

- *Your rival offers a rematch* — the result screen's own line;
- a **`Rematch`** control that sends the same `OfferRematch` the result screen sends — which *is*
  the accept, because the room agrees the moment both seats have offered (`ADR-0044` §§1, 4), so
  there is no second frame to invent and no second control to draw;
- a dismiss labelled **`Not now`** — the word this product already puts on an offer that may be
  set aside (`account-offer-text.ts`'s `OFFER_DISMISS`).

Between the press and the new duel it shows the sentence the result screen already shows in that
span — *Rematch. The button changes sides — dealing hand 1…* — because `ADR-0044` §4 sends no
frame that says both sides agreed, and this is the same gap `RematchControl` already covers.

### 5. It stands until it is answered, dismissed, or has nothing left to answer

**No timer, no countdown, no auto-retire.** Exactly three things take it off the screen:

- the player accepts, and the duel takes the screen (§6);
- the player dismisses it (§7);
- the player's press is refused because the room is gone — `Failure(UNKNOWN_ROOM)`, `ADR-0044`
  §6's *"the frame that ends a rematch"* — whereupon the panel states the sentence the result
  screen states, *That duel room is gone.*, and stands there until it is dismissed.

A surface that retired itself would state, by vanishing, that there is nothing left to answer, and
while the offer stands that is false. The client also has nothing truthful to count: the wire
carries no deadline — `ADR-0044` rejected one as cosmetic, since only a refusal can retire the
control — so a countdown here would be the client asserting a fact the server never sent.

### 6. Accepting from another screen ends at the table, and that is already shipped

Nothing new is designed for the accept. The second offer agrees in the room,
`RematchResult.Agreed.outbound` delivers the new duel's opening frames, the `Snapshot` clears
`rematchOffers` and moves the standing to `running`, `rulingOn` refuses the chosen screen, and
`Lobby.tsx`'s layout effect restores `/` in the same commit — `ADR-0112` §4 and `ADR-0114` §3,
both merged, and both already doing exactly this today for a player whose own offer is matched
while they read the ladder. The yank is `ADR-0112`'s named and accepted cost; this ADR adds no
second one.

### 7. Dismissal hides the surface, never the offer, and the rival is told nothing

**`Not now` sends no frame.** The offer stands in the room, and the player can still answer it:
`/` is the room's own screen, and `RematchControl` carries the same standing offer there. What
dismissal buys is that the panel does not follow them onto the *next* screen either — **a
dismissal lasts as long as the offer it was about**, and an offer ends when the duel that answers
it begins or when the room is gone.

**It is not stored.** A reload brings the panel back, because the server restates a standing offer
on resume (`ADR-0044` §5) and this ADR mints no storage key to remember a dismissal against. The
annoyance is small, bounded by the room's five minutes, and it is bought for a real thing: no new
key, no new persistence question, and one less thing to unpick if this shape turns out wrong.

**Declining is still silence.** `Room` records no decline and no retraction, `ADR-0044` §6 chose
that deliberately, and this ADR adds neither. The offerer is told nothing at all — before or after
a dismissal — exactly as today.

### 8. The card is minting, and it draws every state

`ADR-0091` §2 applies (a new player-facing surface owes a card, and the card merges before the
implementing ticket is startable), and §3 makes this one **minting** rather than composing: a
surface that sits above another screen is visual language this product does not have, so the card
is worked interactively with the human, not dispatched. It draws, each state named:

- the offer standing;
- the accepted span (*dealing hand 1…*);
- *That duel room is gone.* after a refused press;
- the screen with the panel dismissed — which is the screen, unchanged;
- each of the first three at rest under `prefers-reduced-motion` (`ADR-0115`);
- and the panel over at least **two screens of different shape** — the ladder and the account
  screen — at the phone width `ADR-0103` §1 fixes as well as on the laptop, because what the panel
  covers is the thing most likely to be wrong.

### 9. Registers, and does not answer: `DEC-144` — the architect's — the mechanism

Named, not designed, because it is entirely *how*: `Lobby.tsx` returns early per screen, so
nothing in the shipped tree renders **across** screens, and where such a surface is mounted, how
it relates to `ADR-0114`'s `shown`/`ruling` computation and the layout-effect restore, where the
dismissal state lives so that it survives a screen change but not a reload (§7), whether the panel
and `RematchControl` are one component or two, and how the surface is announced to assistive
technology **without taking focus** (§2's requirement; `role="status"` is the shipped precedent,
not a design) are all the architect's. Nothing in `DEC-144` may move the wire or
`PROTOCOL_VERSION`, add a stored key, or change what §§1–7 say a player sees.

### 10. What does not change

The wire, `PROTOCOL_VERSION`, `poker-engine`, the schema and every server file. `RematchControl`
and `DuelResult` keep their merged behaviour and their words. The store gains no frame-derived
state: `rematchOffers` is the fact and it is already there, on every screen.

`ADR-0112` §2's *"no notice, no dialog, no new string"* is untouched, and this ADR does not
contradict it: that sentence governs a **refused** navigation over a **running** duel. This panel
exists only where the ask was **honoured** and the room is **finished**.

## Consequences

**What it buys.** The vision's last beat survives `EPIC-13`'s own decision: a rival who walks to
the ladder between duels can still be challenged, and the offer reaches them in the seconds it is
worth anything. The product stops being present only for the thing a player cannot influence (the
agreement's yank) and silent about the thing that is theirs to answer. It costs no wire, no
schema, no storage and no new word, and every claim it makes is one a player can check by looking
at two browsers.

**What it costs.**

- **One fact, two surfaces.** `RematchControl` on the result screen and the panel everywhere else
  will drift — a word changed in one, a state added to the other — and only prose holds them
  together. §3's rule (never both at once) bounds the damage; it does not prevent it.
- **The product acquires an overlay pattern, and the fence around it is this ADR's prose.** The
  next notice that wants one — a turn arriving, the waiting host's invite link, a rival
  returning — will cite this ADR as precedent, and each of those is a decision nobody has made.
  Named here so the citation is visibly not an answer.
- **`Not now` may read as *I told them no*, and it tells them nothing.** The rival's screen is
  unchanged, before and after. This is `ADR-0044`'s *"silence is a decline"* surfacing at a
  control for the first time, and the fix — one `RematchDeclined` frame — stays exactly one
  version bump away, unbuilt, because nobody has reported the waiting screen as the complaint.
- **The panel can point at a dead room until it is pressed.** `ADR-0044`'s *"the button can lie"*
  cost, now told on more screens: after the room is reaped, only the press learns the truth, and
  the panel's answer is a sentence rather than a rematch.
- **A dismissal does not survive a reload** (§7), so a player who put the panel away and then
  refreshed gets it back.
- **On `verify` and `reset` a player mid-recovery can walk out of the form.** Nothing takes their
  keystrokes (§2) and the press is deliberate, but a player who accepts loses the form they were
  filling; the mailed link can be opened again, and a token's lifetime is finite, so a long duel
  can outlive it.
- **It covers part of whatever is under it.** At the phone width `ADR-0103` fixes, that is
  expensive, and the card owes that state (§8).
- **The card is minting work with the human** (§8), so `STORY-1415` cannot be started by an
  unattended run before that card exists.

**What it forecloses.** Nothing structurally — every rejected shape below stays reachable, most of
them behind one card and one clause. It deliberately does **not** build: a decline or a withdrawal
on the wire, a deadline or a countdown, a general notice surface for other room facts, any notice
at all while a duel is **running** (`ADR-0112` §2 refuses the navigation that would need one), and
any notice for a player who has left the room.

## Alternatives considered

**A blocking modal — literally the human's word.** The strongest case in the set: it is what was
asked for, in the message that raised this decision; *cannot miss* becomes a guarantee rather than
a hope; a scrim makes the choice unambiguous and gets an answer now, while the offer is still
alive; and every poker product the human has used does exactly this. Rejected because it is the
one shape that can **destroy** something — a player typing a password on the account screen, a
mailed reset half-filled, a room code half-entered — and the offer's own semantics never needed
it: silence is already a decline (`ADR-0044` §6), so an unanswered offer costs nothing and there
is nothing to force. It is also the expensive direction to reverse. A blocking dialog that must
later stop blocking is a re-minted card and a re-argued rule; a standing panel that must later
block is one clause in a follow-up ADR. If the human reads §2 and means the stronger thing, that
sentence is where to say so.

**A toast that retires itself after a few seconds.** Its case: the least intrusive surface that
still appears, no dismiss control to design, and the pattern nearly every web product already
teaches players to read. Rejected because it states by vanishing that there is nothing left to
answer, and while the offer stands that is false — the same class of defect `ADR-0115` §1 names
when it forbids a fact that lives only in motion. It also fails the requirement outright for the
player who looked away for ten seconds, and the client has no truthful timer to retire it by,
because the wire carries no deadline.

**A badge, dot or counter on the client's chrome.** Its case: the quietest thing that can be built
— it covers nothing, interrupts nothing, and it is the one shape that would scale to every future
notice without redesign, which is the cost this ADR books against itself. Rejected on two facts:
this client has **no persistent chrome** — each chosen screen renders its own section and a
`Back`, and there is no header that survives a screen change to hang a badge on — and a mark a
player must notice is missable by construction, in a window five minutes long. It would answer the
question by not answering it.

**Pull the player back to the result screen when an offer arrives.** Its case: no new surface at
all — the offer already has a merged, carded home; it reuses `ADR-0112` §4's yank, which the
product performs for the agreement anyway; and it leaves one rule ("a rematch takes the screen")
instead of two. Rejected because §4's rule is deliberately about a frame that **seats a duel**,
and `ADR-0112` says in as many words that a standing offer *"seats nothing and therefore overrules
nothing"*. Taking a screen away from a player for something they have not agreed to is a bigger
interruption than the modal rejected above, and it cannot be dismissed: a player who wants to keep
reading the ladder would be yanked back, walk out again, and be yanked again by the next frame.

**Do nothing — the offer waits on the result screen.** Its case: it is what ships, it costs
nothing, the room and the seat are kept, and `/` is one *Back* away, so the offer is not lost —
only unseen. Rejected because the human asked for the opposite in as many words, and because
`ADR-0112` booked this exact silence as a **cost** rather than as an intent. Five minutes is the
whole life of a finished room; an offer that is invisible for all of it makes *We hit Rematch*
untrue on the very screens `EPIC-13` just made reachable.

**Add `RematchDeclined` to the wire so that dismissing tells the rival.** Its case: the offerer
stops staring at a hopeful screen, *Not now* becomes honest, and it removes the one cost above
that a player can actually feel. Rejected because it is a wire change and a version bump under
`ADR-0047`'s one-bumping-branch-at-a-time lock, for a case `ADR-0044` ruled the finite window
already resolves; it needs new room state and a new race (declining an offer that has just been
agreed); and no player has reported the waiting screen as the complaint. It stays exactly where
`ADR-0044` left it — one message on a later bump — and this ADR does not spend the bump `EPIC-14`
may need for the showdown.

**Follow every room fact, not just this one.** Its case: build one notice surface, once, and
`ADR-0112` named the waiting host's invite link in the same sentence as the rematch offer, so the
second claimant already exists. Rejected because it would decide, in passing, whether a **running**
duel may notify a player who is elsewhere — which `ADR-0112` §2 refuses today, on the ground that
no attention surface exists that would make mid-duel wandering honest — and because inventing the
general case from one request is how a decision becomes a feature. One offer, one surface; the
next claimant brings its own decision.
