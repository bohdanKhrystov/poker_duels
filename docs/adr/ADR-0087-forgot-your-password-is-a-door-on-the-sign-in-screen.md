# ADR-0087 — *Forgot your password?* is a door on the sign-in screen, not a screen of its own

- **Status:** Accepted
- **Date:** 2026-08-28
- **Resolves:** `DEC-081` — what does the product call the flow a player uses when they have
  **forgotten their password**, is it a screen with its own address or a form on `#/sign-in`, and
  where is its door?
- **Where the answer came from.** The vision's *Positioning* sentence: *"The reference points are
  **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal."* That is the sentence
  that licensed [`ADR-0083`](ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
  to take the plain word over a coined one, and it does two things here: it picks the phrase every
  player on the web already scans for, and it refuses a fourth address for a flow that needs none.
  **Unlike `ADR-0083`, this ADR coins.** *Sign in* was already merged player-facing text before that
  decision named a screen with it; *forgot your password* is player-facing text **nowhere** in this
  product, and no amount of reading produces it. `ADR-0076` §1 leaves the words to the product owner,
  so choosing one is permitted — but it is choosing, not finding, and §Consequences records that as a
  cost rather than hiding it.
- **Builds on:** [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §1 (a slug is a
  literal in `screen.ts` and the address set is **open**, with the count of this story's screens left
  to the story), §2 (an address must never become a second claim about entitlement), §7 (nothing in
  the address space is gated); `ADR-0083` §3 (the door is not on the first screen) and §4 (the way
  back in is refused to nobody); [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md)
  §2 (one spelling per destination, and the crowding argument);
  [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §5 (`forgot-password` answers `202`
  in every case, and a second request inside fifteen minutes sends nothing);
  [`ADR-0078`](ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) §Consequences (*"the
  client's copy has to be honest about a pending state rather than congratulatory about a `202`"*);
  [`ADR-0056`](ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) (a refusal keeps
  what was typed)
- **Narrows:** [`ADR-0081`](ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §3's phrase *"the ***forgot password*** screen's"* — one item in a list of addresses that ADR
  granted `STORY-0417` and did not name. The grant is **left unspent**, because the story turns out
  to have no such screen. Every other sentence of §3 stands byte-unchanged, including both slugs it
  fixes and its reason for fixing them: `reset` and `verify` are addresses a **server writes into
  mail**, and this flow is written into no mail.
- **Constrains:** `STORY-0417`'s three held tickets — the flow's words, its form-or-screen shape and
  its door — and nothing else. The seventeen written tickets are unchanged, `TASK-041708` most of all
- **Amends nothing.** No server change, no wire change, no `PROTOCOL_VERSION` step, no schema, no new
  dependency, no new slug, no ticket rewritten here. Nothing in it moves a roadmap row, costs money,
  or touches the vision's *What it is* / *What it is not*

## Context

`ADR-0031` §5 built the endpoint. `TASK-041708` ships the transport: one field, one status, two
outcomes. What nobody has said is what a player is looking at when it is called.

### The forces

**The product has no word for this, and unlike `DEC-077` there is nothing to find.** Every merged
sentence this product says about a password is about a password that works: *"Your password signs in
to this account."*, *"Your password becomes the only way back to this account."*, *"This profile now
has a password. Sign in with it on any other browser."*, *"That handle and password do not match an
account."* Not one of them addresses the player whose password is gone. `DEC-077` was answerable
without inventing because `ADR-0050` §3 had already said *sign in* to a player twice; the equivalent
search here returns nothing.

**The endpoint spells it, and a path is not player-facing text.** `ADR-0083` §2 took the hyphen in
`sign-in` from `POST /api/auth/sign-in` — but it took the *spelling* of a word a player already read,
not the word itself. `POST /api/auth/forgot-password` offers no such anchor: promoting it to a
heading is coining. That distinction is the whole reason `DEC-081` exists, and pretending otherwise
would be the guess this register was raised to prevent.

**Wherever this lives, its door is on `#/sign-in`.** `ADR-0083` §3 keeps a fourth door off the first
screen on `ADR-0060` §2's crowding argument, and the account screen already carries the one *Sign in*
door for a signed-out visitor. So the door of a *screen* and the door of a *form* sit in the same
place — which makes the screen strictly one navigation further from a player who is already stuck,
in exchange for an address.

**An address is permanent and a form is not.** `ADR-0076` §Consequences: *"the address bar is now
player-facing text this product owns forever"*, and *"every screen story from here owes an address…
a screen shipped without one is now a defect."* So the screen answer must mint a slug, and a slug is
the one thing in this question that cannot be taken back cheaply.

**The stakes are asymmetric, and both halves are merged.**
[`ADR-0041`](ADR-0041-a-handle-and-a-password-are-the-only-credential.md): *"`ADR-0031`'s recovery
email carries the whole weight of account recovery, since there is no provider to fall back on."*
`ADR-0031`'s consequence for a recovery that fails is the account, its duel coins and its ladder
place. Findability here is not polish.

**The server has already refused to say anything, and the screen may not put it back.** `ADR-0031`
§5 answers `202` for five different situations — unknown address, pending but unverified, verified
and sent, over budget, no sender configured at all — with an identical empty body, and `ADR-0079`
makes the budget invisible on purpose. `ADR-0078` §Consequences pushed the consequence onto this
story in its own words: *"everything a player learns about whether their address works comes from
the mail arriving, so the client's copy has to be honest about a pending state rather than
congratulatory about a `202`."*

**And two forms would meet on one screen.** `ADR-0083` §Alternatives refused folding the sign-in form
onto the account screen because a player *"with two password fields in view… is being asked a
question the screen should have asked for them."* The recovery form has **no** password field, so
that exact harm does not recur — but two submit buttons on the one screen this product asks for a
password on is still a cost, and it is the cost that decides between the two shapes a form can take.

The tension is real. The shape that is easiest to find is the one that mints nothing; the shape that
behaves correctly under the browser's *Back* is the one that owns a word forever.

## Decision

**The product calls it *Forgot your password?*. It is not a screen and it has no address: it is a
door on the sign-in screen that opens a one-field form in place of the sign-in form, at `#/sign-in`.
The door is offered to everyone who can see that screen, and every answer the flow gives says the
same thing about the address.**

### 1. The words

Four constants in `web-client/src/account/recovery-text.ts`, the module `TASK-041705` creates and
`TASK-041706`'s own *Notes* reserve for exactly this:

| Name | Value |
| --- | --- |
| `FORGOT_PASSWORD_LABEL` | `"Forgot your password?"` |
| `FORGOT_PASSWORD_SUBMIT` | `"Send a link"` |
| `FORGOT_PASSWORD_ACKNOWLEDGED` | `"If that address is verified on an account here, a link is on its way. Follow it to set a new password."` |
| `FORGOT_PASSWORD_FAILED` | `"That did not go through. Try again."` |

Golden and asserted character for character, like every other string in that module.

**Two strings are imported, not authored.** The field's label is `ADDRESS_LABEL` — *"Email address"*,
`TASK-041705`'s, in this same module — because the attach form and this form ask for the same thing
and a second constant would be a second spelling (`ADR-0060` §2). The way back is `CANCEL`, already
merged in `account-text.ts`.

**`FORGOT_PASSWORD_ACKNOWLEDGED` states a rule; it does not report an outcome.** Its *"if"* is what
makes it honest under `ADR-0031` §5: the product does not claim mail was sent, it states the
condition under which mail is sent, and that sentence is equally true for all five situations the
server refuses to distinguish. It ends by naming its destination in that destination's own merged
words — *set a new password* is `ADR-0081` §1's row for `#/reset`.

**`FORGOT_PASSWORD_FAILED` is about the request, never about the address.** It holds the same six
words as `SIGN_UP_FAILED` and as `ATTACH_FAILED`, in a third literal, deliberately — `ADR-0083`'s
`SIGN_IN_HEADING`/`SIGN_IN_LABEL` precedent, and the shape `TASK-041705` already takes in this
module: one flow's sentence is not another flow's, and a rename moves one.

### 2. The door's words are the form's heading, and there is only one literal

Closed, `FORGOT_PASSWORD_LABEL` is the control. Open, it is the heading of what the control opened,
and the control is gone. The words are never on screen twice at once, and a player who presses
something reads back the words they pressed.

**This departs from `ADR-0083` §3, and here is the reason.** That ADR kept `SIGN_IN_HEADING` and
`SIGN_IN_LABEL` as two literals holding identical text, on the ground that *"a control's verb is not
a screen's name"* and that `EPIC-06` may want to letter-fit one without moving the other. There is no
screen here and therefore no name — both utterances are the same utterance, seen one at a time, and a
divergence between them would be a defect rather than a freedom. One literal, not two.

### 3. It is not a screen, and `screen.ts` does not change

`Screen` gains no member. `hashForScreen` gains no case. `screenFromHash` learns no slug.
`ADR-0076` §1's address table gains no row. The address is `#/sign-in` while the form is open and
while it is closed; the fragment is not pushed, not replaced and not read by this flow.

`ADR-0076` §1 left the count to the story — *"one slug each — however many screens that story turns
out to be"*, and *"the set is **open**"*. The count for this flow is settled here at **zero**, and
`ADR-0081` §3's grant of an address to a *forgot password* screen is left unspent because there is no
such screen to spend it on.

### 4. Where the door is

**On the sign-in screen, below the sign-in form.** Not on the first screen — `ADR-0083` §3 and
`ADR-0060` §2, unchanged, and nothing here adds a lobby door. Not on the account screen, which shows
a signed-out visitor exactly one way forward and whose visitor is one press from this one.

**It is conditional on nothing.** The door is there whether or not a sign-in has been attempted and
whether or not one was refused. `ADR-0083` §4's ground, applied to a control rather than an address:
a door that appeared only after a failure would be a claim about state, and a player who *knows* the
password is gone would have to type a wrong one to be shown the way out.

### 5. What a player sees, in order

| State | What is on the screen |
| --- | --- |
| **Closed** | The sign-in form, and under it one control reading `FORGOT_PASSWORD_LABEL` |
| **Open** | The recovery form **in place of** the sign-in form: `FORGOT_PASSWORD_LABEL` as its heading, one field labelled `ADDRESS_LABEL`, one submit reading `FORGOT_PASSWORD_SUBMIT`, and `CANCEL`, which restores the sign-in form. **Never two forms in view, and no password field anywhere in this flow** |
| **Accepted** (`202`) | `FORGOT_PASSWORD_ACKNOWLEDGED`, rendered **with the form, not instead of it**, and what was typed stays in the field |
| **Failed** (any other status, or a `fetch` that rejected) | `FORGOT_PASSWORD_FAILED`, the form still there, what was typed still in it — `ADR-0056`'s rule, applied to a second form |

**The form survives its own success on purpose.** A player who mistyped their address is told the
same thing as a player who did not, learns nothing, and waits for mail that will never arrive. Their
only route back is to see the mistake and ask again, so the address they typed stays visible and the
submit stays live. Asking again is harmless: `ADR-0031` §5 sends nothing for a second request inside
fifteen minutes and does not invalidate the outstanding link, and `ADR-0079` budgets the endpoint at
ten a minute with an answer that never changes.

### 6. An address the product does not hold gets exactly what everybody else gets

The same sentence, the same controls, the same layout, the same absence of any second state. This
flow has **no** unknown-address case: no hint, no *check the spelling*, no *no account found*, no
count of links sent, no difference in what renders and none in what the client waits for. The client
cannot distinguish those situations — `TASK-041708` gives it two outcomes and neither of them is
about the address — and this decision fixes that it never will.

`FORGOT_PASSWORD_FAILED` is not an exception. It says a request did not go through, which is a fact
about this browser's connection, and it is shown identically whether the address typed was real,
mistyped or invented.

### 7. What this deliberately does not decide

- **Whether this ever becomes a screen.** *Not now*, not *never*. If it does, it is a `DEC`, and it
  costs one row in `ADR-0076` §1's table and one branch — additive, with no new word needed, since
  the phrase is fixed in §1. **This ADR mints no slug**, and nothing may write `forgot-password` into
  `screen.ts` on its authority.
- **Colour, weight, spacing, letter-fitting, and whether the door renders as a button or a link.**
  `EPIC-06`.
- **Which component holds the form, and how the sign-in screen's two states are carried.** The
  planner's and the architect's; `Lobby.tsx` renders that screen today and `STORY-0417`'s split
  already orders the tickets that touch it.
- **A *forgot your handle* flow.** There is none and none is added: `ADR-0031` §6.2 puts the handle
  in the reset mail for exactly that player, which is why one flow is enough.
- **The words the other three recovery screens say.** `TASK-041705` and `TASK-041706`, neither
  reopened here.

## Consequences

**What it costs.**

- **This is the first player-facing phrase in this product that was coined rather than found.**
  Every other string on the account screens came from a merged sentence or a merged path;
  *"Forgot your password?"* comes from neither. `ADR-0076` §1 leaves the words to the product owner,
  so it is permitted — but *"only a word the product already says"* no longer describes every string
  in `recovery-text.ts`, and the next ticket that wants to coin one will cite this ADR. That is a
  real loosening of the discipline that has kept this repository's copy honest, taken knowingly, for
  the one flow where a player is stuck with nothing to read.
- **It is also the product's first question to a player.** Every other control is an imperative —
  *Sign out*, *Give this profile a password*, *Stop this device signing in*, *Attach a recovery
  address*. A reader of `recovery-text.ts` now finds two grammars in one module, and the next person
  writing a label has two precedents instead of one.
- **The browser's *Back* cannot close the form.** Opening it changes no address, so *Back* from the
  open form leaves the sign-in screen altogether and lands on `#/account`, skipping the sign-in form
  the player was looking at. That is precisely the class of harm `ADR-0076` was written to remove — an
  in-page state the most-pressed control in the world cannot undo, which is what `showHistory` and
  `showLadder` used to be — reintroduced in miniature. `CANCEL` is the only way back, and on a phone
  the system back gesture is not `CANCEL`. **This is the strongest argument for the screen that was
  refused, and it is being paid rather than answered.**
- **Recovery is two navigations deep and advertised nowhere.** A player who cannot sign in must find
  *Account*, then *Sign in*, then the door; the first screen says nothing about any of it, because
  `ADR-0060` §2 and `ADR-0083` §3 keep it off there. A screen with an address would not have fixed
  that — its door sits in the same place — but the route is long, `ADR-0041` makes it the only route
  back into an account, and this decision leaves it long.
- **A player who mistypes their address is told the same thing as a player who did not, and this
  screen can never tell them otherwise.** The one honest sentence is honest exactly because it is
  useless as feedback. `ADR-0031`'s total loss meets a typo here, and the whole mitigation this
  decision buys is that the form and what was typed stay on screen so the mistake can be seen. Nothing
  detects it, and nothing ever will while §6 holds.
- **The sign-in screen has gained a mode.** Two submits and one question now belong to the one screen
  a player types a password into, and every later test of that screen has to know which state it is
  in. The `ADR-0041` harm does not recur — no second password field, never two forms in view — but a
  screen with two states is a screen with twice the surface.
- **The words are fixed before `EPIC-06` has letter-fit anything, and the acknowledgement is two
  sentences long.** `ADR-0083` §Consequences took this cost for a two-word heading; this takes it for
  a paragraph, which is the hardest kind of string to lay out and the likeliest to be rewritten.

**What it buys.** `STORY-0417`'s three held tickets lose their block, and with them `EPIC-04`'s last
story. The flow costs the product no new permanent address and no new slug. The player who is stuck
finds the way out on the screen where they got stuck, with no navigation at all. And one literal names
the flow everywhere it is named, so nothing can drift out of step with anything.

**What it forecloses.**

- **`#/forgot-password`, and every other address for this flow, until a `DEC` says otherwise.** Left
  explicitly open rather than shut, because adding a row to `ADR-0076` §1's table is additive and
  removing one is not.
- **Any feedback whatsoever about the address, by rule rather than by omission** — no hint, no count,
  no *we could not find that*, no different render, no different wait. `ADR-0031` §5 chose it on the
  wire; this fixes it in the copy, which is where the temptation to be helpful actually shows up.
- **A *forgot your handle* flow**, for as long as `ADR-0031` §6.2 keeps the handle in the reset mail.

**On reversibility, which is why it went this way.** Four constants and one control. Nothing is minted
in `screen.ts`, nothing is written into a mail (`ADR-0081` owns the two that are), nothing crosses the
wire, nothing is stored, nothing is bookmarked, and nothing is deployed. Promoting the form to a
screen later is `ADR-0076` §1's own *"one entry in one table and one branch"*, and the word it would
need is already fixed above. Demoting a screen to a form is the expensive direction, because an
address that has worked has to keep working. On evidence this thin — no players, no host, no mail
sender configured — take the direction that is undone by adding rather than by removing.

**On timing.** Free today, expensive later, twice. No slug minted costs nothing now and is owned
forever the day one is; and this sits at the head of three unwritten tickets in the last story of
`EPIC-04`, off the critical path only because `TASK-041701` is startable, and on it within the week.

## Alternatives considered

**A screen of its own at `#/forgot-password`, headed *Forgot your password?*.** The strongest case,
and it is strong: it is what `ADR-0081` §3 assumed when it granted the address; it is symmetrical with
`reset` and `verify`, so all three recovery screens would be addresses and no reader would have to
learn an exception; the browser's *Back* would close it correctly, which is the one thing this
decision gives up and the exact harm `ADR-0076` exists to prevent; and it is linkable, so the day
anybody writes a word of help there is something to point at. Rejected on the door. `ADR-0083` §3 and
`ADR-0060` §2 keep it off the first screen, so the screen's only door would sit on `#/sign-in` — where
the form sits — making the screen strictly one navigation further from a player who is already stuck,
in exchange for an address that is in no mail, on no wire and bookmarked by nobody. `reset` and
`verify` are addresses for a reason `ADR-0081` §3 states and that does not transfer: a **server writes
them into a mail**. And the address would be permanent while the form is not, so the cheap direction
is to start without one and add it when something shows it is needed.

**The form always on `#/sign-in`, with no door: two forms, both visible, no mode.** The strongest
case: it is the least machinery of any option here, it has no hidden state and therefore no *Back*
problem at all — the cost this decision actually pays — and the recovery route is visible to a player
who has not yet failed. Rejected because it puts two submits permanently in view on the one screen
this product asks for a password on, and `ADR-0083` §Alternatives refused a comparable arrangement in
so many words: the player *"is being asked a question the screen should have asked for them."* The
vision asks for *quiet*, and a screen whose resting state offers two ways to proceed is louder than
one that offers a way to proceed and a way out. It is kept in reserve deliberately — it is one
deletion away, and it becomes the right answer the moment anything shows players cannot find the door.

**A door on the account screen, beside *Sign in*.** The strongest case: the account screen is this
product's one door to everything about an account, a locked-out player passes through it anyway, and
it would save one press. Rejected on `ADR-0060` §2's crowding argument, applied where `ADR-0083` §3
already applied it once: a signed-out visitor to the account screen is shown exactly one way forward,
and a second control offering a different way forward makes them choose before they have tried the
first. The place to be told a password can be replaced is the place the password was refused.

**Name it *Reset your password*, slug `reset-password`.** The strongest case: it says what the flow
achieves rather than what the player did wrong, it is as recognisable as *forgot*, and the product
already writes `reset` — in `POST /api/auth/reset-password`, in `RecoveryLinks`, and in `ADR-0081`
§1's `#/reset`. Rejected because the name is **taken**: `ADR-0081` §1 fixed `reset` as the address of
the screen this flow *mails you to*, and named that screen *Set a new password*. Two things called
*reset*, one of which sends you to the other, is exactly the drift `ADR-0060` §2 spent a paragraph
closing. The door names the player's situation; the destination already names the act.

**Coin nothing at all: put the way out inside `SIGN_IN_REFUSED`.** The strongest case, and the only
option that satisfies `ADR-0076` §1 without inventing a syllable: the product already says *"That
handle and password do not match an account."* to precisely this player, and extending that merged
sentence with a way forward would name the flow using words that shipped months ago. Rejected because
it makes the route conditional on having failed a sign-in — a player who knows the password is gone
would have to type a wrong one to be shown the way out — and because it buries the only route back
into an account inside the one string a player reads while assuming they simply mistyped. `ADR-0041`
makes recovery mail the whole of account recovery; it may not be a clause in an error message.

**A phrase from the duelling vocabulary — *Locked out*, *Get back in*.** The strongest case: the
vision names this product's register explicitly, this is a moment a player feels something, and a
product with a voice is worth more than one that reads like every other form on the web. Rejected on
the same sentence that rejected it for `DEC-077`: the vocabulary the vision lists — *challenge, duel,
rematch, rival, streak, season* — is a vocabulary of **game** concepts, and the sentence carrying them
also asks for *quiet* and *minimal*. The player who has lost access to their account is the last
player in this product who should have to interpret a metaphor.
