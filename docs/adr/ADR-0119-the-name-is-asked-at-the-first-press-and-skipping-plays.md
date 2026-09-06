# ADR-0119 — The name is asked at the player's own first press, the suggestion promises nothing, and skipping plays

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-129` — **the product owner's** — is a player **asked for a name before their
  first duel**, where does the **suggestion** come from, and may the dialog be dismissed without
  naming? Raised 2026-09-06 by [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md)
  item 1a, from the human's own instruction after playing the product on a laptop and an iPhone
  against a local-network stack.
- **Where the answer came from — three parts, from three places, marked separately:**
  - **That a player is asked at all is the human's, stated verbatim, and recorded here rather than
    chosen:** *"First time you play dialog "choose your name" appears with sugestion(before duel)."*
    This ADR does not argue that call and does not re-open it.
  - **When the ask stands, and that it can be skipped, is derived** from `docs/vision.md`'s first
    success condition — ***"Send a link. She opens it in a browser. We play a full heads-up match.
    Someone wins. We hit Rematch."*** That sentence is about the **invited** player, and it is the
    reason nothing may stand between her link and the room. With it, *Positioning* — *"The reference
    points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal."*
  - **What a suggestion is, is derived** from the same *Positioning* sentence and from what
    [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) and
    [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) already built: a name is spent
    out of a namespace that never gives anything back.
- **Applies, and reopens none of:** `ADR-0029` §4 (permanence), §5 (`PUT /api/me/name`, its four
  answers, and the refusal of an availability-check endpoint) and §6 (the server fabricates
  nothing); `ADR-0051` §1 (a string that enters `name_registry` never leaves it), §2 (the write is
  one transaction), §5 (screening is a set-time event) and §9 (no enumeration surface, no
  un-retire); [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) (a name is
  screened when set); [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §1
  (the removal is told on the name surface **and nowhere else**);
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) in full — the account offer after a
  first win, dismissible and permanent, is untouched, and *"no screen gates on having a credential"*
  is untouched because the ask gates on nothing;
  [`ADR-0058`](ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) (`No name`, one string,
  every surface); [`ADR-0063`](ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §2 (a nameless player has a ladder row and it reads `No name`);
  [`ADR-0085`](ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md) §§1–2, whose
  shape §5 below borrows for a second surface without changing a word of it.
- **Supersedes nothing, and amends nothing.** Every merged rule about a nameless player stays true
  **because** the ask can be skipped; that is the load-bearing half of this decision, not a
  softening of it.
- **Registers [`DEC-142`](README.md#open-decisions) — the architect's:** by what mechanism a
  suggestion is produced. §4 fixes what a suggestion *is*; it does not choose a generator.
- **Constrains:** `STORY-1407` and the design card that is its first ticket (`ADR-0091` §2);
  `EPIC-14` item 1a.
- **Decides nothing about** `DEC-130` (the account screen), `DEC-131` (whether a name can be
  changed) or `DEC-132` (what sign-out does). This ADR is about the door only. Where §3 depends on
  `DEC-131`, it says so.

## Context

**The human asked for the ask, and that half is settled before this ADR starts.** What is not
settled is everything the sentence leaves under-determined, and each piece pulls a different way.

**The vision's own success condition is about a player who presses nothing.** *"Send a link. She
opens it in a browser. We play."* Measured on `develop`: the invited rival's browser reads the code
out of the URL (`main.tsx:160`, `roomCodeFromSearch`) and `store/boot.ts` sends `JoinRoom` on the
socket's `Welcome`, outside React, before any screen has an opinion. She does not press *Join*; the
link is the press. So *"before duel"* has no press to hang off on the one path the vision names,
and any ask placed there is an interstitial between the link and the room — the exact thing the
success condition is a sentence against.

**A name here is not like a name in any other product.** `ADR-0029` §4 makes it permanent by
database trigger, and `ADR-0051` §1 makes the string itself unrecyclable: it enters `name_registry`
and never leaves, with no un-retire and no release anywhere in §9's list of what is deliberately not
built. `ADR-0029`'s own *Consequences* names the outcome plainly — *"A typo is forever. The most
common outcome of this decision will not be an impersonation defeated; it will be a player named
`Bobb` who meant `Bob`."* Putting that choice in front of every first-time player, at speed, on the
way into a duel, multiplies that outcome by every player the product ever gets.

**Three merged decisions are built on the nameless player being ordinary, and a mandatory ask does
not overturn them — it falsifies them quietly.** `ADR-0036` makes an account-less player a full
participant; `ADR-0058` makes `No name` *"the product's default, not its edge case"*;
`ADR-0063` §2 gives a nameless player a ladder row in their correct position. An ask with no way
past leaves all three on the shelf, formally accepted, describing a state no player is in.

**And the suggestion cannot be promised.** `ADR-0029` §1 makes a name's freedom a database fact —
*"the index is the reservation"* — and §5 refuses an availability-check endpoint outright, as *"a
pure enumeration surface bought for a nicer form"*. A name that is free when it is offered may be
taken when it is confirmed, and there is no mechanism in this product, by decision, that can say
otherwise.

**What exists today is an offer, not an ask.** `Lobby.tsx:435` already renders `NameSurface` on the
front door: a text field, a *Set my name* button, and `name-text.ts`'s `PERMANENCE_LINE` —
*"A name is chosen once. You cannot change it later, and it can be taken away."* The human played
that screen and did not report it as an ask; he reported the absence of one. The change this
decision makes is not a new capability, it is a moment.

### The deadline, honestly

**The namespace is the thing that cannot be un-spent.** Every name a player accepts burns a string
out of `name_registry` forever. A first version of this ask that writes a suggestion the player did
not deliberately take — on a skip, on a close, on a background pre-registration — burns one string
per browser that ever reaches the screen, including crawlers, e2e tabs and the author's own devices,
and `ADR-0051` §9 builds no path back. That rule (§5) is free to fix today and unfixable after the
first week the ask is live. Everything else here is a client surface and a browser key: cheap now,
cheap later, and deliberately so.

## Decision

### 1. The ask stands at the player's **own first press**, and nowhere else

**The first time, in this browser, that a player who holds no display name presses a front-door
control that starts a duel — *Create a duel room* or *Join the duel* — the ask stands between that
press and the room.** It is a screen, in place of the front door, for as long as it stands. When it
is answered, the press the player already made goes through and they continue into the duel; they do
not press twice.

**The room is created — or joined — after the answer, never before it.** A player who abandons the
ask leaves no waiting room behind them.

Exactly where it is **never** shown:

| The player | What they see |
| --- | --- |
| Opens an invite link and is auto-joined at boot | **Nothing.** The link path is untouched, end to end |
| Presses *Rematch* | **Nothing**, ever. *"We hit Rematch"* is in the success condition and nothing goes in front of it |
| Returns to a room their tab remembers, or resumes after a reload | **Nothing.** No press, no ask |
| Already holds a display name | **Nothing.** The condition is holding no name, not holding no account |
| Held a name that was **removed** (`ADR-0053`'s `displayNameRemoved`) | **Nothing.** `ADR-0052` §1 puts that conversation on the name surface *and nowhere else*, and an ask that said nothing about why their name vanished would be the product pretending nothing happened |
| Has already answered the ask in this browser | **Nothing.** See §5 |
| Was shown the ask and answered neither control | **The ask again**, at their next press. Being rendered spends nothing (`ADR-0085` §2's rule, applied) |

**The ask is not a modal.** This product has no modal of any kind, and `DEC-138` — open, not mine to
touch here — asks whether the rematch offer becomes its first. Inventing one for this surface would
answer that question sideways for the surface that needs it least. It is drawn as a screen; if
`DEC-138` later lands on a modal vocabulary, this surface can adopt it without changing one rule
above.

### 2. Two ways out, and neither of them can fail into a dead end

**Take a name.** A single field, carrying the suggestion (§4) as its initial value. The player may
accept it, edit it, or replace it with their own. Confirming sends the shipped
`PUT /api/me/name` — no new endpoint, no new outcome, no second write path.

**Skip, and play.** One control, which sends nothing, writes nothing and takes the player straight
into the duel their press asked for, holding no name. This is the answer to *"may the dialog be
dismissed without naming?"* — **yes**, and the skip is not a corner of the screen: it is one of two
equally reachable ways out, because it is the one that keeps `ADR-0036`, `ADR-0058` and `ADR-0063`
true.

**No state of this screen ever leaves a player unable to duel.** Every refusal `set-name.ts` can
settle — `conflict`, `rejected`, `permanent`, `no-profile`, `unavailable` — leaves the skip
available and working. A name that cannot be set is never a reason a duel cannot start. That is the
whole content of *"an offer, not a gate"* (`ADR-0036`) applied to a surface that is not about
credentials.

### 3. What the ask says

Three obligations. The words, the heading, the layout and the control labels are the design card's
(`ADR-0091` §2), within these:

1. **It says what a name is for** — that it is what a rival and the ladder see. A permanent choice
   asked for with no reason given is a form, not a question.
2. **It says the choice is permanent, for as long as it is.** The string already ships:
   `name-text.ts`'s `PERMANENCE_LINE`. This ADR adds no new sentence about permanence and invents no
   softer one. **If `DEC-131` releases `ADR-0029` §4, this sentence goes with it** — the ask must not
   outlive the fact it states, and `STORY-1409` is where that lands.
3. **A refusal reads in the words the product already uses.** `refusalSentence` and `mayTryAgain`
   ship in `name-text.ts` and their strings are `ADR-0052`'s golden ones. The ask introduces no
   second vocabulary for the same failures.

**The skip control says what it does** — it takes the player into the duel with no name — and does
not read as a *close*, a *cancel* or a dismissal. A player pressing it must know they are about to
duel, not that they are backing out of one.

### 4. The suggestion is an offer to type, and it promises nothing

- **It arrives as the field's initial value, and it is text.** It is not a reservation, not a hold,
  not a claim, and not a statement that the name is available. Nothing is written until the player
  confirms.
- **The product never tells a player that a name is free.** The only authority on that is the write,
  and the way it says so is by succeeding. `ADR-0029` §5's refusal of an availability-check endpoint
  is applied here, not reopened, and the client asserts nothing about a name it has not been granted.
- **A refused suggestion costs one press, never a dead end.** The ask can put a different suggestion
  in the field. A suggestion the player did not edit may be replaced outright; a string the player
  typed themselves is never overwritten.
- **It encodes nothing about the player.** Not the device id, not the player id, not the session, not
  the room code, not an email, not a duel count. `ADR-0029` §6 forbids the server minting
  `Player-3F2A` and `ADR-0067` keeps ids off public rows; a suggestion is the same string on a
  ladder that is read by strangers, forever.
- **It is drawn from the product's own register** — the vision's duelling vocabulary, *dark, quiet,
  minimal* — and never from the vocabulary this product refuses.
- **It must be a name whose refusal is rare**, so that accepting the suggestion normally works the
  first time. That is what makes it a suggestion rather than a taunt, and it is the property
  `DEC-142` is registered to achieve.

### 5. An **answer** spends the ask, and nothing else does

- **Skipping spends it.** *Not now* means not again, in this browser: `ADR-0036`'s rule and
  `ADR-0085` §2's mechanism, applied to a second surface without amending either.
- **Naming ends it by construction.** The condition in §1 is holding no display name; a player who
  holds one is never asked, whatever any bit says.
- **Nothing else sets it.** Not a refusal, not a reload, not closing the tab, not the ask merely
  having been rendered, not a later duel, and not the passage of time.
- **Nothing in the product clears it.** There is no *ask me again*, no setting and no support path.
  A player who skipped sets a name at the surface that offers one — today `NameSurface` on the front
  door, which this ADR leaves exactly where it is; where it finally lives is `DEC-130`'s.
- **The skipped bit is a fact about this browser and does not travel.** No request is made when a
  player skips, no column records it, and nothing on `GET /api/me` carries it — `ADR-0085` §1's rule
  for the offer's answer, verbatim, for this one. It takes `ADR-0086`'s shape: one key, owned beside
  the predicate it feeds, with a row in
  `web-client/src/protocol/one-module-owns-each-storage-key.test.ts`. Which key and which module is
  the implementing ticket's, not a decision.

### 6. What this changes about a nameless player: nothing

`No name` is still what the client prints (`ADR-0058`), on the same surfaces, for the same reason.
A nameless player still holds their ladder row, in their correct position (`ADR-0063` §2). The
account offer after a first win is untouched (`ADR-0036`), and the ask says nothing about accounts,
passwords, email or coins — it is not a claim prompt wearing a different hat, and a player must be
able to answer it without learning that an account exists.

## Consequences

**What it buys.** The human's ask ships in the shape the product can actually keep: a player who is
about to be seen by a rival is asked who they are, once, at the moment the answer first means
something, and a player who does not want to answer is not held at a door. `STORY-1407` and its card
have a subject: two ways out, one field, one suggestion, and a named list of the moments the ask
never appears. Three merged ADRs keep describing a state players are really in. And the invite link
— the one flow the vision tests itself against — is byte-unchanged.

**What it costs.**

- **The invited rival is not asked before her first duel.** Item 1a's own sentence is true for the
  host and false for her, and this ADR chooses that rather than hiding it: she plays as `No name`,
  and she meets the ask the first time she starts a duel of her own. Half the players in a
  two-player product are invited players, so this is not a corner case — it is a coin flip. The
  softening is real but partial: `ADR-0051` §6 keeps history's name a live join, so a name she sets
  later appears on the older lines too.
- **The host meets an interstitial on the way into their first duel**, in a product whose
  positioning sentence is *fast*. One screen, once ever, entered from a press the player made — but
  it is a screen between wanting to duel and duelling, and that is exactly the thing this product
  says it is not.
- **Saying it is permanent will cost names.** A player told at the door that the choice cannot be
  undone will skip more often than one who is not told. That is accepted: a smaller number of
  deliberate names is the better outcome, and the alternative is buying names by not mentioning
  `ADR-0029` §4 to the people it binds.
- **A suggestion can be refused**, because the product will not promise what `ADR-0029` §5 refuses to
  answer. The player pays one press for a rare event.
- **Every name taken here burns a string forever** (`ADR-0051` §1). The ask will spend the namespace
  faster than any surface before it, and §5's *nothing is written unless the player takes it* is the
  only thing bounding that.
- **A second surface can now set a name.** The ask and `NameSurface` must not drift; they share one
  write path and one set of sentences, which is a rule someone has to keep rather than a property of
  the code, until `DEC-130` decides where the name form finally lives.
- **A signed-in player with no name is asked once per browser.** The bit is a browser fact, so a
  second browser asks again. Small, honest, and the cost of not putting a UI preference on the
  server.

**What it forecloses.**

- **An ask that cannot be skipped**, without a new ADR that also answers what happens to the invited
  player — the question that makes the mandatory version expensive rather than merely different.
- **Writing a name the player did not take**: on skip, on close, on a background pre-registration,
  or as a default accepted by silence.
- **Any client surface that tells a player a name is available** before the write says so.
- **The ask moving onto the invite path, onto a rematch, or over the table**, each of which is now a
  rule with a reason rather than an omission.
- It does **not** foreclose the ask becoming a modal (§1), moving to a different moment, or being
  asked a second time by a later decision. Every part of this is a client screen and one browser key;
  the only irreversible thing in the whole area is the namespace, and §5 is what protects it.

## Alternatives considered

**A hard ask: no skip, no way past.** The strongest case is that it is the shortest route to what
the human actually wants — every player named, `No name` gone from every list, the ladder reading
like a ladder, and one branch fewer on every surface that prints a name. It also removes the
awkwardness of a product that asks a question and then shrugs at the answer. Rejected on two counts.
It makes a **permanent, unrecyclable** choice (`ADR-0029` §4, `ADR-0051` §1) the price of a first
duel, which is the one kind of decision a person should not be hurried into. And it must then either
intercept the invited player's link — against the vision's own success condition — or be a rule that
silently does not apply to half of all players, which is worse than not having the rule.

**Ask on arrival at the front door, before anything is pressed.** Strongest case: it is literally
*before the duel* for everyone who reaches the front door, it needs no press to hang off, and it is
the simplest predicate in the whole area. Rejected because the first thing a new player would see is
a form, before they have seen what the product is — a signup wall's shape with a skip button — and
it still does not reach the invited player, so it spends the first impression and buys no coverage.

**Ask on the invite path too: hold the boot-time `JoinRoom` until the ask is answered.** Strongest
case, and the only shape in which item 1a's sentence is true for everybody: every player named
before their first duel, no split rule, no coin flip. Rejected on the success condition itself —
*"Send a link. She opens it in a browser. We play"* — and on what it would cost around it: the boot
reaction lives outside React by `ADR-0032`, the host is already sitting on a waiting screen watching
for a rival, and the room has a ten-minute life. The invited player's entire first experience of
this product is that the link worked.

**A modal over the front door.** Strongest case: it is the human's own word, it keeps the front door
visible behind the ask so the ask reads as a step rather than a place, and it is what every product
in this space does. Rejected for now because this product has no modal, `DEC-138` is the open
question about whether it gets one, and answering that sideways — for the surface with the weakest
claim to it — is how a vocabulary gets set by accident. Nothing here is lost if `DEC-138` says yes.

**Reserve the suggestion the moment it is shown.** Strongest case: it removes the one genuinely bad
moment in this flow — a player accepts the name the product offered and is told it is taken — and
`ADR-0029` §1's index is already a reservation, so the mechanism exists. Rejected because it writes
into a namespace that never releases anything, on behalf of a player who has chosen nothing, once
per browser that reaches the screen — including every crawler, every e2e tab and every device the
author owns. `ADR-0051` §9 builds no un-retire, by decision, and this would be the feature that made
one necessary.

**A suggestion derived from the player** — `Player-3F2A` from the device or player id, or a hash.
Strongest case: collisions become impossible by construction, no word list has to be authored,
curated or translated, and the string is stable across a reload. Rejected because `ADR-0029` §6
names that exact string as one the server may not mint, and because the fragment would then be
printed on a public ladder, permanently, against `ADR-0067`'s rule that no id turns into a profile.

**Skip, but ask again next time.** Strongest case: a player who skipped in a hurry gets another
chance, and the human's goal is that people have names. Rejected because *"not now means not again"*
is this product's shipped posture in both places it has been decided (`ADR-0036`, `ADR-0085` §2),
and because a repeat ask is a nag sitting on the one path that must stay clear: the path into a duel.

**No suggestion at all — an empty field.** Strongest case: nothing to generate, nothing to curate,
nothing to collide with, and the player types their own name, which is the name they want. Rejected
because the human asked for a suggestion and the reason is real: a blank field at the door of a first
duel is a decision most people will not make in a hurry, and the ones who do will type the first
thing that comes to mind — permanently.

## What this does not settle

- **How a suggestion is produced.** Registered as **`DEC-142`, the architect's**: what generates the
  string, whether the generator consults `name_registry` before offering it, and whether the
  suggestion crosses the wire at all or is a client string. §4 fixes the product rules it must
  satisfy; it chooses no mechanism, and `ADR-0029` §5's refusal of an availability-check endpoint is
  not reopened by either.
- **The suggestion vocabulary's contents.** Whatever words a generator draws from are operational
  data of the same class as `ADR-0051` §5's blocklist — curated, not architecture. This ADR ships no
  word and seeds no list, and whoever ships one owns that it says nothing about anybody.
- **The words, the heading, the control labels and the layout** — the design card's, within §3.
- **Where a player who skipped later sets a name.** Today it is the front door's `NameSurface`,
  untouched here; `DEC-130` decides whether that moves to the account screen.
- **Whether a name can be changed** — `DEC-131`. If it says yes, §3's permanence sentence goes with
  `ADR-0029` §4, and §2's `permanent` refusal changes meaning; nothing else in this ADR moves.
- **What sign-out does** — `DEC-132`. Note the interaction rather than deciding it: the skipped bit
  is a **browser** fact (§5), so a browser handed a brand-new profile would keep its answer and not
  be asked again. Whether that is right is a cost for `DEC-132` to weigh.
- **Moderation, blocklists and what a name may contain.** `DEC-017`'s remaining half is still the
  human's, `ADR-0038`'s screening is unchanged, and nothing here refuses a name that the write path
  does not already refuse.
