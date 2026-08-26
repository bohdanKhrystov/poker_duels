# ADR-0083 — The second account screen is *Sign in*, and its address is never refused

- **Status:** Accepted
- **Date:** 2026-08-26
- **Resolves:** `DEC-077` — what does the product call the screen a player opens to reach an account
  from a browser that does not hold it, and therefore what is that screen's permanent slug?
- **Where the answer came from.** Not invented here. The **act** is already merged player-facing
  text — [`ADR-0050`](ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §3 says *"This
  device **signs in** to this account"* and *"You stay **signed in** here"* — and the **spelling**
  is already the product's, in `docs/protocol.md`'s `POST /api/auth/sign-in`. What licensed choosing
  the plain word over a coined one is the vision's *Positioning* sentence: *"The reference points
  are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal."* This ADR picks
  which word the product already holds becomes a screen's name, and refuses the one word the product
  does not hold (*log in*). It coins nothing.
- **Builds on:** [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §1 (a slug is the
  lowercase ASCII form of a word the product already says, written as a literal), §2 (an address
  must never become a second claim about entitlement), §3 (the store outranks the address), §7
  (nothing in the address space is gated);
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §2 (the door's
  word *is* the destination's heading constant, so a destination has one spelling);
  [`ADR-0081`](ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §1 (the first fragment segment names the screen)
- **Constrains:** `TASK-041226` and `TASK-041227` — the whole of what it unblocks — and every later
  screen that extends `ADR-0076` §1's address table, which is `STORY-0417` and `EPIC-05`
- **Amends nothing.** No server change, no wire change, no `PROTOCOL_VERSION` step, no schema, no new
  dependency, no ticket rewritten here. Nothing in it moves a roadmap row, costs money, or touches
  the vision's *What it is* / *What it is not*.

## Context

`ADR-0076` §1 gave `STORY-0412` *"one slug each — however many screens that story turns out to be"*
and said in the same paragraph that it **coins no player-facing vocabulary**. The story then settled
the count at **two**: `#/account`, which claims the profile this browser already holds, and a second
screen, which is how a browser that does *not* hold the account reaches one. Two rather than one
because `ADR-0012` mints an anonymous profile on the first `Welcome` — so *give this profile a
password* and *reach the account I already made* are always both live — and because
[`ADR-0041`](ADR-0041-a-handle-and-a-password-are-the-only-credential.md) is what keeps two
handle-and-password forms off a single screen.

One of the two words was found rather than chosen: the product says *account* to a player in
`ADR-0050` §3, in [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) and in
[`ADR-0056`](ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §2. The second
screen's was not, and that is `DEC-077`.

### The forces

**The product says the act three ways, and only one of them can be the address.** A player reads
*sign in* as two words (`ADR-0050` §3, twice). `docs/protocol.md` exposes `POST /api/auth/sign-in`,
hyphenated. [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md) writes *"two sign-in
routes"* in prose. And the web's default address for this screen — `/login` — is a fourth word this
product says **nowhere**. Whichever is picked becomes the one the product owns; picking none is not
available, because a screen without an address is now a defect (`ADR-0076` §Consequences).

**A slug is a promise, not a label.** `ADR-0076` §1 makes it a literal in `screen.ts` precisely so a
restyled heading cannot break a link, and its *Consequences* record that *"the address bar is now
player-facing text this product owns forever."* `ADR-0081` has already fixed `reset` and `verify`
into strings a **server writes into mail**, so the register of this fourth slug is not a private
choice inside one ticket — it is the pattern three later screens copy.

**There is no natural noun, and the near ones are worse than the verb.** The screen's subject is an
act. `duels` and `leaderboard` came from things; `reset` and `verify` came from acts, which is the
precedent that matters here. A coined noun — *Welcome back*, *Return* — would be a word no other
part of the product says, and the vision asks for *quiet* and *minimal* rather than for a theme: the
duelling vocabulary it does name (*challenge, duel, rematch, rival, streak, season*) is a vocabulary
of **game** concepts, and none of them is about account plumbing.

**The client cannot tell a live session from a dead one, and something has to decide what the
address does about it.** `signedIn` is `readSessionToken(localStorage) !== null` — *this browser
holds a string* — and `TASK-041209` states the client's whole policy on the matter: *"Retrying,
refreshing or reacting to a `401`. Nothing in this client refreshes a session."* Meanwhile `ADR-0050`
§3 signs a player out **on every other device** while those browsers keep their now-dead token, and
`ADR-0027` §2 expires the rest after thirty days. So the browsers most in need of a sign-in screen
are exactly the ones that still look signed in to themselves.

**Signing in reloads the document, and a reload keeps the fragment.** `TASK-041213` stores the token
and calls an injected `reload`, wired in `main.tsx` to `() => window.location.reload()`. At the new
address that lands the browser back on the sign-in screen it has just finished using, now holding a
session. Nobody has said where it should land instead, and if this ADR does not, the ticket will.

## Decision

**The screen is called *Sign in*. Its permanent address is `#/sign-in`. The word is said twice — as
the screen's heading and as the single door to it — from one constant. The address is refused to
nobody, and a successful sign-in leaves it for `#/account`.**

### 1. The word is *Sign in*

`web-client/src/account/account-text.ts` gains `SIGN_IN_HEADING = "Sign in"`, beside
`ACCOUNT_HEADING`, golden and asserted character for character like every other string in that
module.

It is not coined. `ADR-0050` §3's merged confirmation text says *sign in* to a player in two
different sentences, and `TASK-041225`'s form already submits under `SIGN_IN_LABEL`. What is decided
here is that the act's own words become the **screen's name**, and that *log in* — the one spelling
this product has never used — does not enter the vocabulary at this late a door.

### 2. The slug is `sign-in`, hyphen included

`Screen` gains the member `"sign-in"`; `hashForScreen` returns the literal `"#/sign-in"` and
`screenFromHash("#/sign-in")` returns it. The slug is a **literal in `screen.ts`**, never derived
from `SIGN_IN_HEADING` at runtime — `ADR-0076` §1's rule, and the same duplication `duels` and
`leaderboard` already carry with the comment that explains it.

**The hyphen is the product's own spelling, not a slugifier's guess.** `POST /api/auth/sign-in` is
already written that way, so the client's address and the server's path now read the same character
for character. `signin` and `sign_in` are this product's spelling of nothing and appear nowhere.

This widens `ADR-0076` §1's examples from a bare word to a hyphenated compound, **for this word
only**, on the ground that the compound is what the product already writes. The rule that holds
after it: a slug contains only `[a-z-]`, never starts or ends with `-`, and is still the lowercase
form of something the product already says — a hyphen buys a compound the product writes, never a
phrase invented to fill an address.

### 3. Where the word is said: the heading and the one door

- **The heading** of the sign-in screen.
- **The one door to it**, on the account screen, offered only when `signedIn` is false —
  `TASK-041227`'s rule, unchanged. Its word is `SIGN_IN_HEADING`, so the destination has exactly one
  spelling and a rename moves one literal: `ADR-0060` §2, applied a third time.
- **Not on the first screen.** `TASK-041227` already refuses a fourth lobby door on `ADR-0060` §2's
  crowding argument, and nothing here adds one.
- `SIGN_IN_LABEL = "Sign in"` (`TASK-041211`) stays exactly as it is. It is a **second constant
  holding the same six characters**, deliberately: a control's verb is not a screen's name, and the
  two are free to diverge the day `EPIC-06` letter-fits one of them.

### 4. The address is never refused, and holding a token is not a branch

- A browser that **already holds a session token** and opens `#/sign-in` **gets the sign-in screen**.
  The fragment is not replaced, no redirect happens, and the form works.
- `ADR-0076` §3 is untouched and still outranks it. `outcome`, `view` and `roomCode` come first: a
  tab a frame has seated shows the duel and the fragment is replaced with `/`, exactly as at
  `#/account`. **Holding a token is not a fourth branch and does not become one.**
- The reason is that the client does not know what it would be asserting. It knows this browser holds
  a string; whether that string is a live session is the server's answer, and this client never asks
  again (`TASK-041209`). A browser signed out from another device under `ADR-0050` §3 still holds its
  token, and a bounce would hide the only screen that fixes it behind a *Sign out* control on a
  browser that is not signed in. `ADR-0076` §2's rule is the general form: an address must never
  become a second claim about entitlement.
- So: **the product does not advertise this screen to a browser that holds a token, and does not
  refuse it either.** The door is hidden; the address works.

### 5. A successful sign-in lands on `#/account`

The document reload that carries the new identity (`TASK-041213`) starts the next boot at
`#/account`, not back at `#/sign-in`. **A browser that has just signed in never comes back to the
sign-in screen.** How that is arranged is the ticket's; that it happens is not.

Two reasons, and the second is the load-bearing one. The player came from the account screen — it is
the only door — so returning them there is the shape they expect. And it is **the only confirmation
this product has**: `account-text.ts` authors no *you are signed in* sentence anywhere, while the
account screen states which routes now sign in to this profile (`ADR-0037`, `TASK-041217`,
`PASSWORD_ROUTE_LIVE`). A sign-in that landed on the first screen would be a product that never says
it worked.

A **refused** sign-in changes no address: the player stays at `#/sign-in`, reads `SIGN_IN_REFUSED`
and keeps what they typed. That is `TASK-041225`'s behaviour, unchanged by this.

### 6. What this deliberately does not decide

- **The form, its fields and its refusal sentence.** `TASK-041225`, which ships before this and is
  not reopened.
- **A *forgot password* door.** `STORY-0417`, with `ADR-0081`'s `reset` and `verify`.
- **What the client does with a dead token.** Nothing, still. `TASK-041209` refused that and this
  does not reopen it; §4 only declines to build a screen that assumes the token is live.
- **Colour, weight, spacing and letter-fitting.** `EPIC-06`, which may restyle the heading and may
  not move the slug — that split is `ADR-0076` §1's whole point.

## Consequences

**What it costs.**

- ***Sign in* is now said three times in one flow and twice on one screen** — the door, the heading,
  and the submit button under it — and two constants hold the same string with nothing keeping them
  in step. Every test that reaches for that text must query by role rather than by string, and a
  screen reader announces the same two words twice on arrival. `ADR-0060` §2 bought one spelling for
  a *destination*; this pays for it with a duplicate **on** the destination.
- **`sign-in` is the fourth slug this product owns forever and the first with a character outside
  `[a-z]`.** `TASK-041226`'s acceptance criterion *"the slug matches `^[a-z]+$`"* is now wrong and
  must widen to `^[a-z]+(-[a-z]+)*$`. Every later screen inherits the question of when a hyphen is
  allowed, and §2 answers it for one word rather than for the general case.
- **The slug and `POST /api/auth/sign-in` agree by two literals in two modules and nothing
  mechanical.** A fragment crosses no wire (`ADR-0076` §4), so nothing breaks if they diverge and
  nothing catches it either — the same cost `ADR-0081` recorded for `reset` and `verify`, taken a
  third time.
- **Refusing to gate the address means a signed-in browser can reach a working sign-in form.** Using
  it swaps identity on that device: legal, coherent and moving no coin (`ADR-0030` §6), but it is now
  a state the product permits **at an address** rather than only behind a door it hides. §4 accepts
  that in exchange for never stranding the browser whose session was revoked from somewhere else.
- **Every sign-in ends on the account screen**, so a player who signed in in order to *play* pays one
  extra click before reaching the create and join controls. That is the price of the only
  confirmation the product has, and it is paid on every sign-in rather than on the rare one.
- **The word is fixed before `EPIC-06` has letter-fit anything.** If the visual language later
  prefers another heading, the address stays `sign-in` and the destination has two spellings — the
  first cost `ADR-0076` §Consequences recorded, incurred a second time, knowingly.

**What it buys.** The last two tickets of a twenty-seven-ticket chain lose their block, and
`STORY-0412`'s goal sentence — *sign in on another browser* — becomes reachable. One spelling of the
act now runs through the client's address, the server's path and the screen's name. And a player
whose session was ended from another device can always get back in, because the way back is an
address that never asks who they are.

**What it forecloses.** `/login` and `#/login`: this product's address for this screen is not the
web's default one, and adopting it later breaks whatever links exist by then. A nested address —
`#/account/sign-in` — permanently, because `ADR-0081` §1 reads only the first segment and changing
that would reach back into the two links a server writes into mail. And an address in this client
that refuses to render on client-side state, which §4 makes a **rule** rather than an omission.

**On reversibility, which is why it went this way.** The word and the slug are two literals and a
constant. The address is in no mail (`ADR-0081` owns the two that are), crosses no wire, is stored
nowhere, and is bookmarked by nobody because nothing is deployed. Changing it later costs three lines
and any link a player has saved by then. On evidence this thin — no players, no deployment — the
conventional word the product already says beats a considered invention, and the invention is what
would be expensive to unwind.

**On timing.** Free today, expensive later, twice. Nothing has shipped, so the address costs nothing
to change now and costs a broken bookmark after v0.2. And `TASK-041226` sits at the end of a chain of
twenty-five unblocked tickets: this decision is off the critical path today and would be squarely on
it within the week.

## Alternatives considered

**`login`, with the heading *Log in*.** The strongest case, and it is strong: `/login` is the most
recognised address on the web, it is one bare word needing no ruling about hyphens, it satisfies
`ADR-0076` §1's examples without argument, and password managers and browser autofill heuristics have
been tuned against it for twenty years. Rejected because it is the one word in this whole question
the product does **not** say: `ADR-0076` §1 permits only *"a word the product already says to a
player"*, and adopting *log in* would give one act two vocabularies at the exact moment the product
names it — a screen headed *Log in* whose button reads *Sign in*, at an address a third string.
`ADR-0060` §2 spent a paragraph closing precisely that gap by making the door and the heading one
constant. Recognition is worth something; a second word for one act costs more, and it is the kind of
drift nobody notices until every sentence in the product has two forms.

**`signin`, no hyphen.** The strongest case: it satisfies `TASK-041226`'s `^[a-z]+$` criterion as
written, needs no widening and no ruling, avoids the one thing in §2 that is genuinely new, and it is
what several very large sign-in pages use. Rejected because it is the lowercase form of nothing this
product writes — the product writes `sign-in`, in the endpoint the very same screen calls — and
adopting it would create a **third** spelling of one act (*sign in*, *sign-in*, *signin*) in a
repository whose copy discipline is one spelling per thing. Keeping a criterion that was written
before the answer was known is not a reason to pick a worse answer.

**A nested address, `#/account/sign-in`, with no new top-level slug.** The strongest case: it is
*true* — the sign-in screen is reachable only from the account screen and the address would say so —
and it groups the account screens for every later reader. Rejected on a merged mechanism: `ADR-0081`
§1 makes `screenFromHash` match on the **first fragment segment only**, so `#/account/sign-in` *is*
`#/account` today, and `#/duels/anything` renders the record. Changing that rule to give one screen a
second level would reach back into the two token-bearing links a server writes into mail, for a
client with five screens that has never needed a hierarchy.

**A word from the duelling vocabulary — *Return*, *Welcome back*, slug `return`.** The strongest
case: the vision names the product's register explicitly, this is a screen a returning player sees,
and a product with a voice is worth more than a product that reads like every other form on the web.
Rejected on the same sentence: the vocabulary the vision lists is *challenge, duel, rematch, rival,
streak, season*, every one a **game** concept, and the sentence that carries them also asks for
*quiet* and *minimal*. A themed name on a login screen is the loudest thing on it, it makes a player
guess what a door does, and it is exactly the coining `ADR-0076` §1 refused to do and `DEC-077` was
raised to avoid doing carelessly.

**Fold the sign-in form onto the account screen; have no second screen and no new slug at all.** The
strongest case: `DEC-077` evaporates, one screen means one door and one address, and the product owns
one word fewer forever. Rejected because the count is not this ADR's to reopen — `STORY-0412` settled
it at two, on `ADR-0041`'s ground that two handle-and-password forms on one screen is exactly what
that ADR was keeping clean, and on `ADR-0012`'s, that every browser arrives holding a profile so both
intents are always live. A player who must choose between *give this profile a password* and *sign in
to the account you already have*, with two password fields in view, is being asked a question the
screen should have asked for them.

**Bounce a browser that holds a token from `#/sign-in` to `#/account`, replacing the fragment.** The
strongest case, and the one that lost most narrowly: it applies `ADR-0076` §3's instinct — never show
a player a form that cannot help them — it removes the state `TASK-041227`'s own proof step calls
harmful, and it is one branch that any reviewer would wave through. Rejected because the branch would
be built on `readSessionToken(localStorage) !== null`, which means *this browser holds a string*, and
nothing in this client ever learns that the string is dead (`TASK-041209`). `ADR-0050` §3 ends every
other device's session while those devices keep their tokens; `ADR-0027` §2 expires the rest silently
at thirty days. Those browsers are precisely the ones that need this screen, and the bounce would
leave their only way in behind a *Sign out* control offered to a player who has just been signed out.
An address that refuses on a fact the client cannot check is `ADR-0076` §2's forbidden shape — a
second claim about entitlement — and the failure it causes is invisible, permanent-feeling and
discovered by the player. Adding the bounce later is one branch; removing one that stranded somebody
costs an account.
