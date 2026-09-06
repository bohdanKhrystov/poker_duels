# ADR-0131 — Signing out of your own account hands the browser a new profile, and leaves the old one whole

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-132` — **does signing out abandon the anonymous profile and issue a new one?**
  *Yes, when the account being signed out of is the profile this browser owns. No, when it is
  somebody else's.* Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 1d, last of that
  epic's original eleven.
- **Where the answer came from, and the one reading this ADR makes.** Both halves are the human's,
  stated eleven months apart in the same product's terms, and **this ADR chooses neither**. What it
  chooses is which sentence governs which case, and that is a reading it states out loud rather
  than hides:
  - **The abandoning half is the human's, 2026-09-06, recorded verbatim** — *"if logout you
    comeback to new annonumus profile"*. It is the fourth clause of item 1's account flow, and
    every other clause of that message is about **this browser's own profile and its promotion**:
    the name ask, the profile *"tied to browser"*, *"Annonymus Account"* on the account screen,
    *"You can 'promote' your account to real by set email and password"*, and — the clause
    immediately after — *"when account promoted duel coins trasfered from annonymus to real
    profile"*. The message never mentions signing into an account this browser does not own.
  - **The keeping half is the human's, 2026-08-14, recorded verbatim** in
    [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) — *"if it is sign in from
    annonomus to some different account i would like to **keep anonimous accout if user log out**
    but no data/history migration needded"*. That is the same question asked and answered for the
    other case, and nothing in the 2026-09-06 message reopens it.
  - **The reading:** the newer sentence governs the case it was written about and does not reach
    the case the older one already settled. It is stated as a reading because it could be wrong,
    and the one sentence that would correct it is in §*What this does not settle*. It is also the
    reversible direction — see §*The deadline*.
  - **Everything the two sentences do not reach is derived**, and each derivation names its source
    in place: `docs/vision.md`'s *"One duel coin per win. Not chips, not currency, not a balance.
    **A counter of duels won.**"* and its *Positioning* paragraph; `ADR-0037`'s shipped rule that
    the account screens must state which routes are live; `ADR-0050`'s asymmetry between a final
    act and the cheapest one; `ADR-0119` §5's *an answer spends the ask, and nothing else does*.
- **Supersedes, in one case only:**
  [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §3's outcome clause —
  *"the anonymous profile is back with its coins, its history and its name … sign-out is therefore
  idempotent and total"* — and its third bullet, *"The client discards the token and **keeps its
  device id**"*; and §8's first bullet where it reads *"never cleared and never overwritten — not
  on sign-in, **not on sign-out**"*, together with §8's third bullet, *"Sign-out clears the token
  and only the token."* **All four are superseded only for a sign-out whose session belongs to the
  profile this browser owns**, and stand verbatim for every other sign-out and for every other
  moment in the life of a browser. §3's `DELETE FROM auth_session`, its uniform `204`, its
  *writes nothing to `player`, `credential`, `duel` or `duel_result`*, its *does not close live
  sockets*, and §§1, 2, 5, 6 and 7 are **untouched**.
- **Supersedes nothing in** [`ADR-0012`](ADR-0012-device-bound-anonymous-profiles.md) (a player is
  still an anonymous profile bound to a device id — the browser is handed a **new** one),
  [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) (precedence is unchanged; §5's harm
  is a **stale client abandoning a profile by accident**, and §4 of this ADR keeps that forbidden),
  [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md) (the device id stays a valid
  route until revoked — what changes is that the browser no longer holds the string, not what the
  server does with it), [`ADR-0049`](ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md)
  or [`ADR-0050`](ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md). **§3 is the whole
  answer to what `ADR-0049`'s finality means for a browser about to be somebody else: nothing,
  because a sign-out is not a revocation.**
- **Registers [`DEC-152`](README.md#answered-decisions) — the architect's:** by what mechanism a
  browser stops owning the profile it owned, at a sign-out and only there, and how the
  confirmation learns **before** it acts which of the two sign-outs this is. §7 fixes what the
  mechanism may not do and chooses nothing. **Answered on 2026-09-07 by
  [`ADR-0135`](ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md)**,
  inside that fence.
- **Constrains:** `STORY-1410` (item 1d, and item 1e's proof), and the account card that carries
  §6's words.
- **Moves no wire, no schema and no `PROTOCOL_VERSION`** by itself. Whether the mechanism does is
  `DEC-152`'s, and §7 says what it may not spend.

## Context

**Today, a sign-out from your own account signs you out of nothing.** `ADR-0030` §3 made sign-out a
subtraction: delete the session row, keep the device id, and the lower-precedence edge is simply
visible again. That is exactly right when the session was somebody else's account. It is empty when
the session was *this browser's own profile with a password on it*, because `ADR-0030` §1 makes a
promotion move no row — the profile the device owns **becomes** the account. Subtract the session
and the browser is the same player it was a second ago: same name, same coins, same ladder row, and
still inside the account without presenting the password. The player pressed *Sign out* and nothing
happened that they can see. That is the state the human played and reported.

**The counter-force is the only thing in this product that can lose a coin, and it got sharper
while this decision waited.** `EPIC-14` registered `DEC-132` against a product in which a first win
put an offer on the result screen — *your duel coins are only in this browser* — precisely so that
a browser-bound profile could be made reachable before anything could strand it.
[`ADR-0125`](ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) has since
**retired that offer whole**, on the human's own instruction, and names as its own cost that *"a
player who never opens the account screen is never told"*. So the warning this decision could once
have leaned on does not exist, and a sign-out that abandons a profile is now the **only** mechanism
in the product that can put a duel coin somewhere nobody can reach. `ADR-0030` §5's P1 and P2 hold
across every identity operation there is; this would be the first act that could put a whole profile
out of reach without breaking them.

**The two forces do not actually collide, and where they don't is the seam.** The profile a
sign-out would abandon in the reported case is, by construction, a profile with a password on it —
you had to sign in to it. It is not stranded by being left; it is one sign-in away, forever. The
profile a sign-out would abandon in the *other* case — a phone that already had coins, signed into
an account it does not own — is anonymous, browser-bound, and abandoning it would be exactly the
loss `ADR-0125` deleted the warning for. **The case the human's sentence is about is the safe case.
The case it is not about is the dangerous one.** That convergence is why this ADR splits rather
than picks a side.

**Measured, so the mechanism question is not confused with the product one.**

- The control exists and is offered only to a signed-in browser:
  `web-client/src/account/SignOutControl.tsx` returns `null` unless `props.signedIn`, and it
  already confirms before it acts, in-page, with `SIGN_OUT_WARNING` — the same shape `RevokeControl`
  uses. There is no sign-out for a browser that never signed in, so this decision creates no
  surface.
- The shipped copy is `web-client/src/account/account-text.ts:23` — *"Signing out leaves any duel
  room this browser is in, and a duel left this way can be lost. **This browser goes back to the
  profile it had before.**"* The second sentence becomes false in the abandoning case on the day
  the behaviour lands.
- `web-client/src/account/sign-out.ts` clears the token and the room code and **never touches the
  device id**, with `ADR-0030` §8 quoted in its own KDoc as the reason.
- **The client cannot tell the two cases apart.**
  `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt:168` answers a session-identified
  handshake with `Welcome(playerId = <the session's player>, deviceId = null)`, and `ADR-0027` §4
  ignores a device id presented alongside a token *"not even validated"*. The browser holds
  `pd.deviceId` in storage and has no way, while it holds a token, to learn which player that
  string names. **Which sign-out this is, is a server fact** — the same class of fact `DEC-147`
  registers, and for the same reason.

### The deadline

**Three things, and the third is the one that matters.**

The narrow one: `STORY-1410` cannot be split until this is settled, and `EPIC-14`'s account items
are a chain — `DEC-129` → `DEC-130` → `DEC-131` → `DEC-132` — each written against the screen the
last one leaves behind. This is the last link.

The cheap one: `SIGN_OUT_WARNING` is a shipped string that this decision falsifies. Behaviour and
copy have to land in one diff, which is a note for `STORY-1410` and not a reason to hurry.

The real one: **this decision is the first thing in the product that creates an unreachable
profile, and today there are no profiles anybody minds losing.** `tasks/BOARD.md` carries
`EPIC-07 | Infrastructure and delivery | *not written*` and `DEC-126` has not chosen a deployment,
so every browser that has ever held a profile here is a QA profile driven against a localhost or
local-network stack. Shipping this before the first real player means the set of abandoned profiles
starts empty and every later one was abandoned by a player who was told. Shipping it after means
the first sign-out is somebody's coins.

That is a reason to decide **now** and not a reason to decide a particular way — and it cuts
towards the narrower answer. Widening abandonment to the second case later is one branch deleted.
Narrowing it later cannot un-strand the coins the wide version already stranded.

## Decision

**Signing out ends the browser's standing in the account it signed into. If that account is the
profile this browser owns, the browser is handed a new, empty anonymous profile. If it is somebody
else's account, the browser returns to the profile it owns, exactly as it left it.**

Everything below is that sentence applied.

### 1. The three sign-outs, and what each one leaves behind

| The session being ended belongs to | After the sign-out this browser is | Why |
| --- | --- | --- |
| **the profile this browser owns** (a promotion, `ADR-0030` §1) | a **new, empty anonymous profile** — no name, no coins, no duels, no credential | the human's, 2026-09-06 |
| **an account this browser does not own** (a second device, a shared browser) | the profile it already owned, unchanged — same name, same coins, same duels | the human's, 2026-08-14, `ADR-0030` §2 |
| **no profile at all** (`ADR-0030` §2's recovery sign-in on a fresh browser, which is issued no device id) | a first-time visitor, whose next contact mints a profile | already true; nothing changes |

Rows two and three ship today and this ADR leaves them byte-unchanged. **Row one is the whole
change.**

The rule reads on the *session*, never on whether a credential exists, and that has one consequence
worth stating because it will otherwise be found by surprise: **a browser is handed a fresh profile
at most once per promotion.** A player who promotes, signs out into `P′`, then signs back in with
their password and signs out again is in row two the second time — the session's player is not the
profile the browser now owns — so they return to `P′`. Signing in and out does not churn profiles.

### 2. The invariant this rests on: nothing is ever abandoned that cannot be signed back into

**A browser is handed a new profile only when the profile it is leaving holds a credential**, and
that is not a rule anyone has to remember — it follows from §1. To be in row one you signed in to
that profile, so it has a password. The profile is left, not lost: it keeps its name, its coins,
its duels, its ladder row and its place in every finished duel's history, and it is reached again
by signing in.

This is the invariant that answers `EPIC-14`'s framing of `DEC-132` as *"a new way to lose coins"*.
Under §1 it is not one. **No route in this product abandons a profile a player cannot get back to**,
and `DEC-152` may not build one.

### 3. Nothing is revoked, deleted, moved or destroyed

- **A sign-out is not a revocation.** `ADR-0049` §2's `UPDATE device_binding SET revoked_at` does
  not run on this path, its finality trigger never fires from it, and §3 — *a revoked device may
  bind again, only to a profile that does not yet exist* — is not entered. The binding the browser
  leaves behind stays **live**. `ADR-0050`'s own sentence is why: *"Revocation is final forever;
  signing out is the cheapest act in the product."* An irreversible database act firing behind the
  cheapest press in the product is exactly the shape `ADR-0037` refused when it made revocation a
  deliberate, warned, separate choice.
- **No coin moves, in either direction.** Nothing is copied to the new profile and nothing is taken
  from the old one. `ADR-0014`'s ledger and `ADR-0030` §5's P1/P2 hold across a sign-out exactly as
  they held before, and the vision's *"One duel coin per win … **a counter of duels won**"* is why:
  a counter of duels won cannot be moved by an act that is not a duel, and cannot be minted by one
  either.
- **`player`, `credential`, `duel` and `duel_result` are untouched.** `STORY-0406`'s
  byte-identical-`player` criterion survives this decision unchanged.
- **The account keeps its live device binding to a string the browser no longer holds.** That is not
  a leak this ADR closes, and it is not a guarantee it delivers: anyone who copied `pd.deviceId` out
  of that browser's storage beforehand still resolves to the account. **`ADR-0037`'s revoke stays
  the only thing that kills the route**, and this decision makes it no less necessary — only less
  often reached by accident.

### 4. This licenses one abandonment, at one moment, and nothing else

The browser stops owning its profile **only** at a sign-out the player confirmed, from row one of
§1. It does not happen on a reload, on a failed handshake, on an expired or refused session, on a
`Welcome` carrying a null `deviceId`, on clearing a room, or on any error path. `ADR-0027` §5's
named harm — a stale client that *"conclude[s] it has no device, mint[s] a fresh one and abandon[s]
the profile it was holding"* — stays forbidden, and stays the reason `ADR-0030` §8's write-once rule
governs every other moment in the life of a browser.

**An invalid session still refuses rather than falling back** (`ADR-0027` §4). A refusal is not a
sign-out and hands out no profile.

### 5. The player is told, before it happens, on the surface that already asks

The confirmation `SignOutControl` already renders is where this is said. **No new surface, no
banner, no badge, no second screen** — `ADR-0125` §2 refused to replace the offer it deleted with a
pointer, and *Positioning*'s *"Dark, quiet, fast, minimal."* is the same sentence that governs here.

Because §1 has two outcomes, **the confirmation states the one that applies** — which is why
`DEC-152` must make the case knowable before the press and not only after. What it owes, in the
abandoning case:

1. that this browser will be a **new, empty profile** afterwards, and not the one it is now;
2. that the profile being left **keeps its duel coins and its duels**, and is reached again by
   signing in with the password.

In the returning case, the shipped second sentence — *"This browser goes back to the profile it had
before."* — is already exactly right and stays.

Three refusals, taken from `ADR-0125` §3 and re-applied:

- **It states; it does not urge.** No *are you sure*, no red, no count of what is at stake, no plea
  to set a recovery address first. A player leaving an account they hold the password to is doing
  an ordinary thing.
- **It is not a warning about loss**, because nothing is lost. The sentence about the coins is there
  so that a number going to zero on the next screen is not a surprise, not so that the player is
  frightened out of the press.
- **The words are the card's**, within those two obligations and these three refusals. This ADR
  mints no string, and `ADR-0091` §3 governs the ones that are minted.

`ADR-0037`'s standing rule is the licence: *the account screens must state which routes are live*,
chosen because the status quo *"cannot be described honestly on a screen that also asks for a
password."* A screen that says *sign out* while leaving the player signed in is that same
dishonesty; a screen that hands them a different profile without saying so is its mirror.

### 6. The name ask is not asked again, and `ADR-0119` §5 is not amended

A browser handed a new profile holds **no display name**, so `ADR-0119` §1's condition is met and
the ask stands at that player's own next press of *Create a duel room* or *Join the duel*, with a
fresh suggestion. A player who **named** themselves before the promotion is therefore asked again,
for the new profile, which is right: it is a new profile and it has no name.

A player who **skipped** the ask is **not** asked again. `ADR-0119` §5 makes the skipped bit a fact
about *this browser*, sets out what spends it, and enumerates what does not — *"Not a refusal, not a
reload, not closing the tab, not the ask merely having been rendered, not a later duel, and not the
passage of time."* **A new profile is not an answer either**, so the bit stands and the fresh
profile plays as `No name` until the player sets one at the surface that offers one. `ADR-0119`
§*What this does not settle* asked for this to be weighed rather than inherited; it is weighed here
and named as a cost, and the alternative — clearing the bit at sign-out — is refused because it
would amend a merged rule to buy a second prompt for a player who already said *not now* and was
told that meant not again.

### 7. What `DEC-152` may not do

Registered for the architect, with the product half fixed and no repair written — `ADR-0105` §6's
precedent. The mechanism must satisfy all of:

- **It revokes nothing** (§3). `ADR-0049` §2's statement and trigger stay out of this path.
- **It writes nothing to `player`, `credential`, `duel` or `duel_result`**, and no coin moves.
- **The abandoned profile stays reachable by its credential**, and the invariant in §2 holds for
  every path the mechanism creates.
- **The confirmation can be told which case it is in, before the press.** It is a server fact
  (§Context), it is the same class of fact as `DEC-147`'s, and neither may be answered by inferring
  from `signedIn` or from the presence of a token.
- **`ADR-0030` §8's write-once rule stands everywhere else.** It no longer forbids the change at
  this one moment, and it forbids it at every other.
- **`ADR-0002` holds**: a browser may forget a credential it holds; it may not assert which player
  it is.

Named without choosing, because each has a different answer to *what does the old live
`device_binding` row mean afterwards*: the client forgets `pd.deviceId` and `ADR-0027` path 3 mints
on the next `Hello`, which spends no schema, no wire and no server code; the sign-out response
carries a new device id; or the server does something the client only obeys. **`DEC-152` picks
one.**

## Consequences

**What it buys.** *Sign out* means what the word means. The state the human reported — press it,
still be the same player, still be inside the account with no password — stops existing. A shared
laptop stops handing the next person the previous person's account by default, which `ADR-0030`
named as a cost it could not pay and `ADR-0037` could only offer as an opt-in. `ADR-0125`'s second
false-anonymity case — *a player who signed out in this browser* — stops existing, exactly as that
ADR predicted it might, which narrows what `DEC-147` has to be sound against. And item 1e's proof
gets sharper rather than weaker: a promotion still moves no row, and now the only act that changes
which profile a browser owns is one the player confirms.

**What it costs.**

- **A player who signs into their own account and out again watches their coin count go to zero**,
  and the product's entire explanation is one sentence on a confirmation they may have clicked
  through. This is new, it is the sharpest cost, and §5 exists because of it.
- **A player who forgets their password loses a recovery path they had for free.** Before this,
  their browser was still inside the account and doing nothing kept it that way. After this, one
  press of *Sign out* leaves them with the password and `ADR-0031`'s optional recovery email, which
  they may have declined — and `ADR-0031` is explicit that declining means no path back at all.
  This is a weaker version of the risk `ADR-0037` refused to force on everybody when it rejected
  *retired at the moment of claim*, and it is now imposed on everyone who presses the button. It is
  defensible because the button says *Sign out* and leaving is what a player pressing it intends —
  but it is a real loss, it is the strongest argument against this decision, and it raises the value
  of `ADR-0031`'s email and of `DEC-090`.
- **`SIGN_OUT_WARNING` is false the day the behaviour lands.** Behaviour and copy ship in one diff
  or `STORY-1410` ships a lie.
- **`STORY-1410` inherits a knowledge dependency** of exactly `DEC-147`'s kind: a screen that must
  be *told* something before it may assert it.
- **Empty profiles accumulate** — one `player` row and one live `device_binding` per sign-out from
  an own account — and `ADR-0039` ships no deletion, so nothing collects them. It is a row per
  press, not per player, and it is cheap while `EPIC-07` is unwritten.
- **A player who skipped the name ask plays their new profile as `No name`** (§6), until they set
  one themselves.
- **The product gains a second act a player cannot undo by waiting**, after `ADR-0037`'s revoke —
  and unlike that one, this is not the cautious player's deliberate choice, it is the ordinary
  button.
- **Two sign-outs behave differently under one label**, which is a distinction the player did not
  ask for and will meet without warning on a second device. §5's sentence is the whole mitigation.

**What it forecloses.** A sign-out that is a no-op — after this, the word means leaving, and no
later ADR may quietly restore *keep me signed in as this profile* under that label. A browser
holding two profiles warm at once, so a *switch account* affordance would have to be designed
against one device, one live binding, one profile. And an implementation in which the browser
abandons a profile without being told to at that exact moment (§4).

**What it does not foreclose.** Widening abandonment to the second case, which is one branch of §1
deleted if the human says the 2026-09-06 sentence meant all sign-outs. Narrowing it back, which is
the same branch restored. `ADR-0037`'s revoke, unchanged and still the only thing that kills the
route. And the two-affordance shape in *Alternatives* 4, if the second device's case turns out to
need its own word.

### The reversal trigger

The first player who is not the author signs out and asks where their coins went — `ADR-0095` §6's
trigger, applied here. Two readings would be reopened by it: whether §5's one sentence is enough
warning for a number going to zero, and whether the second case in §1 should have been abandoned
too. Either reversal must settle **the behaviour and the sentence together**, because the copy is
what makes the behaviour honest and neither is worth moving alone.

## Alternatives considered

**1. Always a fresh profile — both cases, one rule, no branch.** The strongest case, and it is
strong: it is the plainest reading of the words the human wrote, *sign out* would mean exactly one
thing on every device, the confirmation would need no knowledge of which case it is in — killing
§5's dependency and half of `DEC-152` — and a rule with no branch is a rule nobody implements twice.
Rejected on two grounds. It overturns a second verbatim human call that the 2026-09-06 message never
mentions — *"i would like to keep anonimous accout if user log out"*, 2026-08-14, `ADR-0030` §2 —
and a newer sentence written about one case is not authority over a case it does not name. And it is
the one shape that actually strands a coin: a phone with duel coins of its own, signed into an
account it does not own, signed out, loses a browser-bound anonymous profile with no credential, no
warning — `ADR-0125` deleted the only one — and no recourse. `ADR-0012` already names that harm as
the thing `EPIC-04` exists to repair. Reversing it after one real player has done it is archaeology
against rows nobody can identify.

**2. Never a fresh profile — keep today's behaviour and repair the words instead.** It supersedes
nothing, costs no code, strands nothing ever, and has a genuine reading of the report behind it:
what the human met may have been an account screen that could not say what the browser was, and
`DEC-147`'s fact plus `ADR-0125` §3's block might have been the whole repair. Rejected because it
leaves *Sign out* meaning nothing in the case a player is most likely to be in. A player who set a
password, pressed the button, and is still that player — same coins, same ladder row, no password
required — has not been signed out of anything, and no sentence on a screen makes that true. The
human reported the behaviour, not the copy.

**3. The sign-out revokes the device binding.** The server does it, so `ADR-0002` is unarguable; the
browser cannot get back by restoring storage; the statement already exists and is already tested
(`ADR-0049` §2, `ADR-0050` §1); and it makes the abandonment a fact of the database rather than a
fact about what a client remembers, which is the direction this repository normally prefers.
Rejected on `ADR-0050`'s own sentence — *"Revocation is final forever; signing out is the cheapest
act in the product"* — because it puts a `restrict_violation` trigger behind the cheapest press
there is, and turns the deliberate, warned, opt-in guarantee `ADR-0037` designed into a side effect
of an ordinary button. It would also make §1's *at most once per promotion* impossible to state
honestly: a device that revoked itself can never bind to that profile again (`ADR-0049` §3), so a
player signing back in on the same browser would be permanently in a different relationship to
their own account than a player who never signed out.

**4. Two affordances — *Sign out* that returns, and *Sign out and start fresh*.** It guesses at
nothing, it is exactly `ADR-0037`'s *"a cautious player can reach the strong guarantee, and a
careless one cannot fall into it"* shape applied one level down, and it would let §5's sentence be
replaced by the labels themselves. Rejected on the precedent for this same question one level up:
`ADR-0050` refused two buttons whose difference is *"the difference between a route and a token"* as
a distinction moved onto the player, on `docs/vision.md`'s *"Dark, quiet, fast, minimal."* Two
sign-out buttons, whose difference most players cannot articulate and none asked for, is the shape
this product has already declined once. Not foreclosed — see *What it does not foreclose*.

**5. Abandon the profile, but carry the coins to the new one.** Nobody ever watches a number fall,
which is the single largest cost in *Consequences* paid off in one stroke. Rejected outright: it
mints duel coins no duel paid for, which breaks `ADR-0014`'s ledger and `ADR-0030` §5's P1 and P2,
and the `duel_result` rows that justify the balance cannot follow — this is `ADR-0030`'s named
failure mode, the copy that makes an opponent's single duel appear twice, wearing a different hat.
The vision's *"a counter of duels won"* refuses it in one line.

**6. Abandon on the *next* sign-in instead of at sign-out**, so a browser that signs out and changes
its mind still finds its account. It softens the recovery-path cost, and it is the most forgiving
shape available. Rejected because it makes *Sign out* asynchronous — the browser is in one state
until some later event puts it in another — so no sentence on the confirmation can say what will be
true, and the player's own coin count would depend on how they next arrive. §5 cannot be written
against it.

## What this does not settle

- **The mechanism** — `DEC-152`, registered above, fenced by §7.
- **Whether the human's 2026-09-06 sentence was meant to cover *every* sign-out**, including a
  browser signed into an account it does not own. This ADR reads it as covering the promoted case
  and says so as a reading, and §1's second row is the branch that would be deleted if the answer
  is *every*. **One sentence settles it: "when you sign out on a second device that already had its
  own anonymous coins, should that browser also come back as a brand-new profile?"** Until it is
  answered, the narrower rule stands, because it is the one that can still be widened.
- **The words on the confirmation** — the card's, within §5's two obligations and three refusals.
- **Whether *Attach a recovery address* is offered before a password exists** — `DEC-090`, open,
  untouched, and made more valuable by this decision rather than answered by it.
- **What the account screen says to a browser that has just been handed a new profile.** It says
  what `ADR-0125` §3 says to any anonymous profile, once `DEC-147` supplies the fact; nothing here
  adds a state to that screen.
- **Whether a name can be changed** — `DEC-131`, untouched. A player who names their fresh profile
  is setting a first name on a new profile, not changing one, so §6 needs no answer from it.
- **Account deletion**, and what becomes of an abandoned profile in the long run.
  [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md)'s *not in v0.1* stands and this ADR does
  not reopen it; the rows §Consequences names accumulating are the same rows that ADR already
  keeps.
- **Anything about a session's lifetime, sockets, or a duel in progress.** `ADR-0030` §3's *sign-out
  does not close live sockets* and §6's *signing out mid-duel abandons the seat* stand verbatim, and
  `SIGN_OUT_WARNING`'s first sentence is already about exactly that.
