# ADR-0125 — The account screen names the anonymous profile, and it is the only door to a password

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-130` — does the **account screen name the anonymous state** — the human's
  *"Anonymous Account"* — and become the **only** door to promotion, retiring the post-win offer
  entirely? Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 1b, from the
  human's own message and annotated screenshots after playing the product on a laptop and an
  iPhone against a local-network stack.
- **Where the answer came from — two parts, marked separately:**
  - **The retirement is the human's, recorded here rather than chosen.** On `edits4.png` — the
    result screen after a win — the **entire** account-offer block is struck through with one
    word: ***remove***. Heading, paragraph and both controls; `Rematch` and `Back to the lobby`
    beside it carry no mark. And in the message of the same day: *"When you go to profile it says:
    'Annonymus Account'; You can 'promote' your account to real by set email and password"*. This
    ADR does not argue either instruction and does not reopen them.
  - **What the screen says, where the telling goes, and what the front door stops saying are
    derived** from `docs/vision.md`'s *Positioning* — ***"The reference points are Lichess and
    Chess.com, not PokerStars. Dark, quiet, fast, minimal."*** — read with *What it is*'s
    ***"One duel coin per win. Not chips, not currency, not a balance. A counter of duels won."***,
    which is the thing the retired prompt was about, and *What it is not*'s *"Not a casino"*.
- **Supersedes, each on a named clause:**
  - [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) — §Decision's second block,
    *"After a player's first win, the client offers one"* and its four bullets (the trigger, the
    permanent dismissal, offer-not-gate, declining degrades nothing), its *"`EPIC-04` gains one
    story"* paragraph, and §Consequences' second, third and fifth bullets (the first-win fact,
    dismissal as state that must survive, and the claim path as *"the prompt's destination rather
    than a flow a player has to find on their own"* — which this ADR reverses: it is now exactly a
    flow the player reaches on their own). **§Decision's first paragraph is untouched and is
    re-applied here**: *"An account is never required to play, and anonymous play stays fully
    ranked… No screen gates on having a credential."* So are §Consequences' first and fourth
    bullets. That first paragraph was the human's own call in `DEC-025` and is not this ADR's to
    move.
  - [`ADR-0085`](ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md) — **in
    full.** Every section of it is about the answer to a prompt that no longer exists.
    [`ADR-0119`](ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §5 borrowed
    its *shape* for the name ask and **restates the rule in its own words**; that section stands
    whole and is not touched by this supersession.
  - [`ADR-0086`](ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md) —
    **in full**: the key `pd.accountOfferSettled`, its owning module, the `"1"` sentinel, the
    no-clear rule and the gate row all go with the surface (§1).
  - [`ADR-0116`](ADR-0116-the-accept-is-a-door-and-a-door-that-does-not-open-spends-nothing.md) —
    **in full**: its whole subject is a control this ADR deletes.
- **Applies, and reopens none of:** `ADR-0036` §Decision's first paragraph (above);
  [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 (a claim is one
  credential `INSERT` and moves no row — the fact §3.3 puts on a screen);
  [`ADR-0058`](ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) (`No name`, one string,
  every surface — §6); `ADR-0119` §5 and §6 (the ask's own bit, and *the ask says nothing about
  accounts*); [`ADR-0041`](ADR-0041-a-handle-and-a-password-are-the-only-credential.md) and
  [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) (what a credential is, and that the
  recovery address is optional — neither is touched, §7);
  [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md) and
  [`ADR-0050`](ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §4 (the route
  sentences the screen already prints);
  [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §3 and §5's first paragraph
  (a held `WAITING` or `FINISHED` room honours an ask for another screen) — only §5's clause
  naming *the offer's accept* loses its subject, and nothing about which screens a room honours
  changes; [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §4 (a
  screen knows nothing about navigation).
- **Answers `DEC-089` by deletion, and strikes it in this PR.** That question asked whether the
  post-verdict nudge may share the verdict's type weight. There is no nudge: after §1 the verdict
  is the only thing on the result screen above `Rematch`, so the question has an answer rather than
  an owner. No other surface inherits it — §3's block is not a post-verdict nudge, and its type is
  the account card's.
- **Registers `DEC-147` — the architect's:** by what means the client learns that the profile it
  holds carries **no credential**. §4 fixes only that the screen may not assert the state until it
  has been told.
- **Constrains:** `STORY-1408`, its design card, `STORY-1402`'s out-of-scope line about the strip,
  and `EPIC-14` items 1b, 1e's *"sentence on a screen"* and 3a's profile strip. It constrains no
  other screen.
- **No wire change, no schema change, no new endpoint and no migration in this ADR.**
  `PROTOCOL_VERSION` does not move here. Whether the fact `DEC-147` answers costs a field is the
  architect's, and is the only part of this decision that could touch the wire.

## Context

### What ships today, measured on `develop` at `7c39fd3d`

**The promotion already lives on the account screen, and only there.** `SignUpForm` is rendered by
`AccountScreen` and by nothing else; `AccountScreen` is rendered by `Lobby.tsx:211` for the
`account` screen and by nothing else. Its control says `Give this profile a password`. So *"the
account screen owns the promotion"* is not a change — what changes is that a **second** door stops
existing.

**That second door is the post-win offer**, four merged ADRs deep: `ADR-0036` decided it,
`ADR-0085` decided what spends it, `ADR-0086` decided the browser key that remembers, and
`ADR-0116` — two days ago — decided that its accept is a door and that a press which delivers
nothing spends nothing. It renders on the result screen after a win, to a browser holding no
session, that has not answered before. Its heading is *Your duel coins are only in this browser*.

**The account screen already refuses to assert what it was not told.** Twice in `AccountScreen.tsx`
the same comment: *"With no profile in hand — still loading, no-profile, or unavailable — the
screen asserts neither route (`ADR-0037`): a sentence built from a read the client never got back
is not a fact the client was told."* And where it does assert a credential fact, it asserts only
the sound direction — `showPasswordRoute = signedIn && …`, under a comment deriving it: sign-in is
the only endpoint that returns a token, so *a browser holding a live session is a browser whose
player has a password, by construction*.

**The front door prints `No profile yet.`** — `ProfileStrip.tsx:35`, the strip's `no-profile`
branch. Its sibling `unavailable` branch renders `null`, with the reason written beside it: *"Do
not announce every failed background read; the player cannot act on it."*

**The lobby's `Account` control is rendered unconditionally**, on the front door, beside `Your
duels` and `Leaderboard`, under a comment citing `ADR-0036`: *"the door is offered whatever the
profile read answered — nothing here gates on having an account."*

**The result screen's design card draws no offer.** `design/screens/duel-end.html` ends at
`Rematch`; `ADR-0091` §5 registered that accretion as debt, *"the account offer first among them"*.

### The first force: the strike adopts the shape `ADR-0036` rejected

`ADR-0036` §Alternatives rejected *"Optional forever, with no prompt"* in one sentence: *"it is
silent at exactly the moment the player has something to lose: a player who has never been told
their coins are device-bound learns it by losing them."* Deleting the offer takes that rejected
shape. Nothing in the record refutes the argument — it was right when it was written and it is
still right — and the human struck the block anyway, having played the product and seen it.

So this is not a case of new evidence overturning old reasoning. It is a direct instruction that
costs something specific, and the honest form of this ADR is to say what is lost rather than to
build a case that nothing is.

### The second force: a warning at the moment of winning is the casino's shape, not this product's

The other half is derivable, and it is why the instruction is not merely tolerable. The offer's
heading — *Your duel coins are only in this browser* — is the largest type on the screen at the
moment a player has just won, and what it announces is a way to lose. The vision's counter is
*"One duel coin per win. Not chips, not currency, not a balance. A counter of duels won."* A
counter of duels won does not need protecting at the instant it increments; a **balance** would.
Dressing a victory in a loss warning is the register the *Positioning* sentence refuses, and it is
the thing the human saw on the screen and struck.

That does not make the telling worthless — it makes the result screen the wrong place for it. The
account screen is the right one, because that is where a player who has asked *what am I?* is
standing.

### The third force: the client cannot currently tell an anonymous profile from a claimed one

`GET /api/me` carries six fields — `playerId`, `coinBalance`, `displayName`, `displayNameRemoved`,
`deviceRouteLive`, `hasRecoveryEmail` (`docs/protocol.md`, `profile.ts`) — and **none of them says
whether the profile holds a credential**. The client's only signal is `signedIn`, *this browser
holds a session token*, and that supports one direction only. Two reachable states break the
converse:

- **A sign-up whose follow-up sign-in failed.** `sign-up.ts` answers `signed-up` on the `201`
  *"whether or not that follow-up succeeds"*, deliberately, because the credential exists. That
  browser holds a claimed profile and no token — and `SignUpForm` is at that moment showing *"This
  profile now has a password."* A line calling the same profile anonymous would put two
  contradictory sentences on one screen.
- **A player who signed out here.** `ADR-0085` §Consequences named this case and left it *named,
  not solved*; `ADR-0086` §4 kept `signOut` away from the offer's key precisely so as not to decide
  it by accident. The device is a credential until revoked (`ADR-0037`), so the browser resolves to
  the same claimed player, holding no token.

**This product has already had this defect once, on this screen.** `TASK-120601` — round 2 of
`/qa-cycle regression`, reproduced by hand on two browser profiles against a live stack — found a
claimed profile being shown the unclaimed-profile form on every reload, and wrote the reason in the
same words this ADR has to use: *"That derivation runs one way only, and `showSignUp` uses its
converse. A browser with no session is not a browser whose player has no password."* Its verdict on
the harm was *"the screen states a falsehood about the account and nothing else"*. The repair made
`sign-up.ts` sign the browser in after the `201`; it did not give the client the fact, and its own
shipped test — *"a claim whose follow-up sign-in fails is still a claim"* — is the case that
survives.

Naming a state is a stronger act than offering a form. A form that will be refused tells the player
the truth when they press it; a **sentence** that is false is false the moment it is read. This is
what makes §4 a rule rather than an implementation note.

### The fourth force: *anonymous* is a word this product has already ruled on, in a different slot

`ADR-0058` rejected `Anonymous` for the **name slot** — it asserts a choice that is false for a
player whose name was taken away, it is the string `ADR-0029` §6 forbids the server to mint, and it
is the most impersonable candidate. Every one of those reasons is about a word printed **about
other people, where a name would be**. None of them is about a second-person statement on a
player's own account screen. The two rulings can coexist, but only if this ADR says so out loud;
otherwise the next reader finds `No name` and *anonymous* and concludes one of them is a mistake.

`ADR-0119` merged this morning and makes it sharper: a player may now hold a **name** they chose
and still hold no credential. *Anonymous* here is a fact about credentials and never about names.

### The deadline

**The deletion is free today and stops being free at the first deployment.** `tasks/BOARD.md`
carries `EPIC-07 | Infrastructure and delivery | *not written*`; every browser that has ever seen
this offer is a QA profile driven against a localhost or local-network stack. Deleting a surface
and orphaning a storage key costs nothing while that is true, and becomes a decision about other
people's browsers the day it is not. `ADR-0116` §*The deadline* made the same measurement two days
ago in the opposite direction.

**Nothing about the naming expires.** It is one screen's copy on one surface — cheap now, cheap
later. The reason to settle it today is that `STORY-1408` cannot be split until it is settled, and
that `EPIC-14`'s account items are a chain: `DEC-129` → `DEC-130` → `DEC-131` → `DEC-132`, each one
written against the screen the last one leaves behind.

## Decision

### 1. The post-win offer is retired, whole

**No screen in this product offers an account after a duel.** The result screen shows the verdict,
the coin, the hands, `Rematch` and `Back to the lobby`, and nothing else. What goes with it:

- the surface (`AccountOffer`) and its strings, including *Your duel coins are only in this
  browser*, *Keep them with a password* and *Not now*;
- the predicate `offerAccount` and its three terms — `verdict`, `signedIn`, `settled` — and
  `DuelResult`'s `offer` prop;
- the browser key `pd.accountOfferSettled`, its owning module, **and its row in
  `web-client/src/protocol/one-module-owns-each-storage-key.test.ts`**, which go in one diff: a row
  left behind after its module is deleted matches nothing and goes red naming a file that no longer
  exists.

Nothing reads the key afterwards, nothing writes it, and **nothing clears it**. Browsers already
holding it keep a value no code consults — today, all of them are the driver's own.

### 2. Nothing replaces it, on any screen

There is no banner, no badge, no toast, no reminder, no count of coins at risk, no *"you have not
made an account yet"* line, and no second prompt anywhere in the product. The offer is not moved;
it is deleted.

**The standing way to an account is the one already shipped**: the `Account` control on the front
door, rendered to every player whatever their profile read answered, and the address `#/account` it
opens. `ADR-0119` §6 stays true — the name ask says nothing about accounts — and it does not become
a place to advertise one.

A player who has just won and wants a password therefore presses *Back to the lobby* and then
*Account*: two presses, from a screen that points at neither. That is a cost, it is named in
§Consequences, and it is not repaired by adding a pointer, because a pointer on the result screen
is the struck block in a smaller font.

### 3. The account screen names the profile's state, in the word *anonymous*

For a profile the product **knows** holds no credential (§4), the account screen says so, about the
profile, using the human's word. The block says three things and no fourth:

1. **What the profile is** — *anonymous*: it has no password, and this browser is the only thing
   that signs in to it. This sits beside the route sentences the screen already prints
   (`ADR-0037`, `ADR-0050` §4), and it does not replace them.
2. **What that costs** — the profile lives in this browser: the duel coins and the duels go with
   it, and nothing reaches them from another browser. This is `ADR-0036`'s telling, relocated in
   substance from the screen the human struck to the screen the human named.
3. **The way out, and what it keeps** — the password form already on this screen, and the fact that
   giving the profile a password keeps **every duel coin and every duel**, because nothing moves
   (`ADR-0030` §1). That sentence is `EPIC-14` item 1e's *"sentence on a screen"*, and this is the
   screen.

**The words, the heading, the order and the layout are the design card's** (`ADR-0091` §2), within
those three obligations and these three refusals:

- **It is not a tier.** No badge, no pill, no status label, and nothing that reads as a class of
  account which a *real* account is the upgrade from. There is one kind of profile in this product;
  a password is a **route into it**, not a rank. The screen therefore does not use the word *real*
  about an account, and *promote* is not owed a place — `Give this profile a password` already says
  the act plainly.
- **It states, it does not urge.** No deadline, no urgency, no *"you should"*, no consequence for
  leaving the screen as it was found. `ADR-0036`'s *never required* is the standing rule, and a
  screen that nags is a gate wearing a softer word.
- **The screen's own heading stays `Account`.** It is the lobby door's label and the address's
  name, and nothing asked for it to change.

### 4. The screen asserts the state only when it has been told, and `DEC-147` is what tells it

**The client may not print *anonymous* from the absence of a session token.** A browser holding no
token is not evidence that the profile holds no credential (§Context), and this screen's own rule —
it asserts no route it was not told — extends to this fact without exception.

- **Told, and it is anonymous:** the block of §3 renders.
- **Told, and it is not:** nothing about anonymity is said; the screen behaves as it does today,
  and `signedIn`'s `Your password signs in to this account.` is unchanged.
- **Not told at all** — the profile read is loading, `unavailable`, `no-profile`, or the fact has
  no source yet: **the screen says nothing about the state**, and still offers the password form
  exactly as it ships. An offer is not an assertion: a form that cannot succeed answers `409` and
  says so in words the player can act on.

**`DEC-147` is registered for the architect** — by what means the client learns it. Until it is
answered and merged, §3's block does not ship and the rest of this ADR does: §§1, 2, 5 and 6 need
no fact the client does not already hold.

### 5. The front door stops describing an absence

**`ProfileStrip`'s `no-profile` branch renders nothing** — `null`, exactly as its `unavailable`
sibling already does, for the reason already written there: the player cannot act on it. The string
`No profile yet.` leaves the product.

This ADR changes **nothing else about the strip**. The `profile` branch — the name, the duel coin
count, the recent duels — stands where it stands; where it sits on the front door, and how, is item
3a's card and not this decision. The human's mark struck a box that held one sentence about
nothing; it is not read here as striking the counter the vision names.

### 6. *Anonymous* is a state, never a name

Nothing prints `Anonymous` where a display name would be. `ADR-0058` §1's `No name` is unchanged on
every surface, `nameOrNone` stays the only branch on a null display name, and `ADR-0029` §6's
prohibition on the server minting a placeholder is untouched.

The two facts are orthogonal and the screen may not conflate them: a player who took a name at
`ADR-0119`'s ask and holds no password is **named and anonymous**, and a player who skipped it and
signed up is **nameless and claimed**. The block of §3 says nothing about names, and the name
surface says nothing about accounts.

### 7. What this deliberately does not change

- **What a credential is.** `ADR-0041` — a handle and a password — and `ADR-0031`'s optional
  verified recovery address both stand exactly as merged. The human's *"set email and password"* is
  read here as the ordinary phrase for signing up, not as an instruction to move the credential to
  an email address; if it was the latter it reverses `ADR-0041` and it is the human's to say so.
  Nothing in this ADR blocks on the answer.
- **Whether a promotion is measured.** Nothing counts it. `ADR-0085`'s foreclosure of a conversion
  funnel outlives `ADR-0085`: there is no record of who was told, who pressed, or who declined,
  and adding one is a decision, not a column.
- **`ADR-0036`'s first paragraph.** An account is never required, anonymous play stays fully
  ranked, and no screen gates on a credential. Retiring the offer makes the product ask **less**,
  not more.

## Consequences

**What it buys.** The result screen becomes the verdict and the rematch, which is what its own
design card has drawn all along — `ADR-0091` §5's oldest carded-screen accretion is cancelled
rather than paid, and `DEC-089` is answered by there being nothing left to rank. The product stops
saying the same thing in two places with two mechanisms: one screen names the state, offers the
password and states what a password keeps, and one control on the front door leads to it. Four ADRs
about one prompt — a browser key, a sentinel, a gate row, a spend rule and a narrowing clause —
collapse into a screen with a sentence on it. And a player who wants to know what they are can now
find out by asking, which is the thing the product could not do before at all.

**What it costs.**

- **The product no longer volunteers the warning, and some players will lose coins that a prompt
  would have saved.** This is `ADR-0036` §Alternatives' rejected option, adopted: *"a player who
  has never been told their coins are device-bound learns it by losing them."* A player who never
  opens the account screen is never told, and the account screen is one press away from the front
  door — not from the moment of winning. Recorded as the price of the instruction, not argued away.
- **Two presses, and nothing points the way.** From a win to the password form is *Back to the
  lobby* then *Account*, and §2 refuses to shorten it.
- **Four merged decisions are retired to delete one prompt.** A reader who arrives at `ADR-0085`,
  `ADR-0086` or `ADR-0116` alone finds a complete, careful mechanism for a surface that does not
  exist, and `ADR-0116` is two days old. That is the standing cost of supersession, and it is the
  first time this repository has paid it.
- **A shipped storage key is orphaned in every browser that holds it.** `pd.accountOfferSettled` is
  never read, never written and never cleared again. The cost is zero only because nothing is
  hosted; the same deletion after a deployment leaves a key in strangers' browsers.
- **The naming waits on `DEC-147`.** The half of `DEC-130` the human actually asked for — *when you
  go to profile it says Anonymous Account* — cannot ship until the architect says how the client
  knows. `STORY-1408` therefore splits into work that is startable now and one ticket that is not.
- **A named player is still called anonymous.** After `ADR-0119`, a player who chose `Bob` and holds
  no password reads a screen that calls their profile anonymous. It is correct — the word is about
  credentials — and it will read oddly to somebody, which is why §6 exists.
- **The front door goes quiet about a profile that could not be read.** After §5 a `no-profile`
  answer produces nothing on screen, so a player whose profile genuinely failed to appear sees no
  trace of it. Accepted for the reason the `unavailable` branch already accepts it, and it does
  mean one fewer place a broken read shows up.
- **Tests, an arc and a card move with the surface.** The offer's four modules and their tests, the
  whole-client arc's three offer cases in `drive-arc.test.tsx`, its two wiring seams in
  `drive-arc.tsx`, and `ADR-0116` §6's regression obligation all go. Deleting a proof is not free:
  what those tests pinned was a behaviour, and after this ADR there is no behaviour to pin.
- **The manual test plan owes an edit, in the diff that lands §5 and not before.**
  `docs/test-plan.md` §*"Read the strip after a reload"* reasons from *"A fresh profile's first
  render is therefore `No profile yet.`"*, and `04-01` names that string in its *fails if* column.
  Both become false when the branch renders nothing — and both are **true until then**, so editing
  the plan in this ADR's PR would break it for as long as the code still ships the string. The
  story that lands §5 carries it.

**What it forecloses.**

- **Any prompt that reaches a player who did not ask.** Not after a win, not after N wins, not on a
  season boundary, not on the front door. Bringing one back is an ADR that supersedes this one and
  says out loud that the product nags again.
- **Knowing whether a player was ever told.** Nothing recorded it before and nothing records it
  now; the browser key that could have been read as *"was offered"* is deleted rather than kept for
  a future funnel.
- **Distinguishing a browser that answered the old offer from one that never saw it.** The key is
  orphaned, not migrated, so that population is permanently unreadable. Nobody should want it.
- **A second surface that offers the promotion.** §2 makes the account screen the only one, so a
  later feature that wants a claim form in a modal or on the ladder has to move this rule first.

It does **not** foreclose the telling coming back to a *player-initiated* surface — the account
screen is one, and any screen a player chooses to open could be another — nor a later ADR putting
the state's line on the front door once there is a player to learn it from.

## Alternatives considered

**Keep the offer and add the naming.** The strongest case in the file, and it is strong: it is what
four merged ADRs say; it delivers the telling at the one moment the player demonstrably has
something to lose, which is `ADR-0036`'s argument and was never refuted; `ADR-0116` has just
repaired the accept, so the surface finally works end to end; and naming the state on the account
screen requires no deletion at all — the two are independent. Rejected because the human struck the
whole block, on the screen where it renders, with the single word *remove*, having just played the
product. That is an instruction, not an input to be weighed. And keeping both would leave the
product asking twice for a thing it says is never required, which is the shape `ADR-0036` §Decision
warned would *"erode under a growth argument later"*.

**Move the offer rather than delete it** — the same telling as a quiet line on the front door, or a
lobby banner after a first win. Its case is real: it removes the block from the screen the human
struck, which is literally all the annotation says, while keeping the warning unprompted and
putting it where every player passes. Rejected because a nudge relocated is the same nudge, and the
vision's *"Dark, quiet, fast, minimal"* has no word for a banner; because it would re-open
`DEC-089`'s question on a new surface instead of answering it; and because it invents a surface
nobody asked for out of an annotation that asked for a deletion.

**Delete the offer and say nothing about the state** — the account screen keeps the password form
and gains no words. The smallest diff available, it needs no new fact from the server so nothing
waits on `DEC-147`, and the shipped control *Give this profile a password* already implies that the
profile has none. Rejected because the human asked for the state to be named and because a form is
not a statement: a player who has never wondered what their profile is does not learn it from a
control they did not press, and after §1 there is no other place they could learn it.

**Name the state from `signedIn` alone, and accept the two false cases.** It ships today, it is
right for the ordinary player, and both wrong cases are rare and self-correcting — one press of
*Sign in* repairs either. It also needs no new decision, no field and no wait. Rejected because one
of the two cases puts *"This profile now has a password."* and a line calling the same profile
anonymous on one screen at one moment; because the screen's existing discipline — assert no route
you were not told — is not a style the client happens to have but the reason a player can believe
what this screen says; and because taking the converse of `signedIn` for a fact about the profile is
the precise mistake `TASK-120601` filed as a defect on this same screen eight days ago. Doing it
again, in a **sentence** rather than in a form's visibility, would be worse than the original.

**Say it on the front door instead of, or as well as, the account screen.** Its case is the
strongest empirical one here: almost nobody opens an account screen, everybody sees the front door,
and the telling only works if it is read. Rejected because it is the nudge again, in the exact spot
the human struck a box, and because one surface naming the state is what stops two surfaces
drifting — the failure mode `ADR-0058` §2 was written to prevent for the neighbouring string.

**Remove the whole profile strip from the front door**, as `edits1.png`'s arrow can be read. Its
case: the mark points at the whole box, and a front door with nothing on it but the two ways into a
duel is the fastest, quietest door this product could have. Rejected as more than the annotation
can carry. The box the human struck showed one sentence — `No profile yet.` — and the same
component in its other state holds the duel coin count, which is *"One duel coin per win… A counter
of duels won"* on the screen a player sees most. Removing an empty state answers the mark; removing
the counter is a different decision that nobody has asked for, and where the strip sits is item
3a's card.

## What this does not settle

- **How the client learns a profile holds no credential** — registered as **`DEC-147`, the
  architect's**. §4 fixes only that the screen may not assert what it was not told.
- **Whether the credential itself should become an email address**, as the human's *"set email and
  password"* could be read. It would reverse `ADR-0041` and put an address at the centre of
  sign-up, so it is the human's, and it is asked rather than assumed. Nothing here blocks on it.
- **Where a name is set, and whether that form moves onto this screen.** `ADR-0119` §*What this does
  not settle* pointed the question at `DEC-130` by name. The answer is that **the name form does not
  move here**: `NameSurface` stays exactly where `ADR-0119` left it, because whether the surface is
  a *set once* form or a *change whenever you like* one is `DEC-131`'s to say, and a form cannot be
  placed before it is known which of the two it is. `STORY-1409` is where it lands.
- **`DEC-090`** — whether *Attach a recovery address* is offered on a device with no password —
  stays open and is neither answered nor moved. It sits on the same screen, and the fact `DEC-147`
  produces is what would let that section be decided on knowledge rather than on a form's refusal.
- **`DEC-132`**, and its interaction, noted rather than decided: if signing out hands the browser a
  brand-new anonymous profile, the second of §Context's two false-anonymity cases stops existing.
  That is a reason for `DEC-132` to be answered next, not a reason to assume its answer.
- **`DEC-094` and the `Account` door's size.** `edits1.png`'s *bigger font* lands on the open
  `DEC-094`, exactly as `STORY-1402` §Out of scope records. This ADR makes that door the only route
  to an account and still does not dress it.
- **The words, heading and layout of §3's block**, and the account card's frames — the card's,
  within §3's three obligations and three refusals.
