# ADR-0056 — A throttled sign-up says so, and keeps what was typed

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-049` — when sign-up answers `429` (`ADR-0055` §3), what is the player told and
  what does the form do? **Derived from the vision; the human did not state this call.** The
  licensing sentence is *"The reference points are **Lichess** and **Chess.com**, not PokerStars.
  Dark, quiet, fast, minimal"* — a refusal the player cannot act on is told once, quietly, and given
  no countdown, no alarm and no second screen. The other half comes from
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md)'s *"declining does not degrade
  anything"*: sign-up is an offer, so a refusal here postpones an offer rather than breaking a
  product, and the screen must say so. Read beside the vision's *"we are not going to pretend
  otherwise"* — the same reading [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md)
  used to decide that a player is told the true state of their own account
- **Builds on:** [`ADR-0055`](ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md)
  §§1, 3 and 4 (the budget, the empty-bodied `429` with no `Retry-After`, and the address key),
  [`ADR-0048`](ADR-0048-a-password-has-one-rule-and-it-is-length.md) §6 and §7 (the response table
  and *"one sentence a player reads once, and never a verdict on what they typed"*),
  `ADR-0036` (an account is offered, never required)
- **Constrains:** `STORY-0412`, which renders this; `STORY-0415`, which must not spend the offer on
  it. `STORY-0404` and `STORY-0405` gain **nothing** — see §6
- **Leaves open:** the words and the visual treatment, which are `EPIC-06`'s

## Context

`ADR-0055` gave `POST /api/auth/sign-up` a seventh outcome and stopped at the wire: `429`, empty
body, no `Retry-After`. What reaches a person is undecided, and it is undecided in a form where
every other refusal already knows what to do.

**This refusal has no vocabulary on this form.** `ADR-0048` §6's other five answers each name a
field the player can fix — a malformed handle, a taken handle, a password outside 8–128 code points,
an identity the client can re-establish. The form's whole grammar of refusal is *mark the field,
state the rule*. A `429` marks nothing, states no rule, and leaves exactly one action available:
wait.

**The client is told only the status.** `ADR-0055` §3 ships no body and no `Retry-After` on purpose,
and calls the header *"the half of this that is left undone"*. So anything the screen says about
**how long** is a number the client invented — and the window is rolling, so even a true number
stops being true the moment anybody sharing the address sends another request.

**The wrong answer is the default answer, and it is one missing branch.**
`web-client/src/profile/api.ts` maps `200` to a body, `401` to *no profile*, and **everything else
to `unavailable`** — the shape a new form will be written in, because it is the shape that already
exists. Under that mapping a working, deliberate refusal arrives on the screen as *the service is
broken*, and nobody chooses it: it is the absence of a branch, which is invisible in review. This is
precisely the *"a coder quietly invents an answer inside a ticket"* case, and it is one line away.

**And that default makes the problem worse rather than merely misdescribing it.** `ADR-0055` §1:
*"an over-budget request still counts … so hammering extends the window rather than resetting it."*
A message that reads as a fault produces the reflex it deserves — press it again — and the reflex is
the single behaviour that lengthens the wait. A misdescription here is not cosmetic; it is
self-inflicted.

**The person most likely to read it did nothing.** `ADR-0055` §4 keys the budget by remote address,
and its consequences record the price: *"a shared address is now a shared fate, and one person can
spend it."* A second player on a university, office or café address is refused for a stranger's
requests, and nothing in the response distinguishes them — the server does not know either.

**They also did not come here for this.** `ADR-0036` makes the account an offer, made after a first
win, dismissible forever. `ADR-0048` §Context already recorded what that costs: *"a rule that sends
them back to the field a second time is a rule with a real chance of ending with no account at
all."* That is why both loud and silent are wrong — drama tells a new player the product is
hostile, and silence tells them it is broken.

**And a password is sitting in the box.** `ADR-0048` §3 permits anything a password manager
generates, so the string in the field may be one the player cannot reproduce. Clearing it charges
them for a stranger's requests; keeping it leaves a secret in a form on a shared machine — which is
the same café that produced the refusal.

### The deadline, honestly

Nothing here is permanent. A copy string, a status mapping and a form's behaviour are one PR each,
on any day, with no schema, no wire field and no migration — which is why this ADR prefers the
reversible shape wherever the evidence is thin, and says so where it does.

What has a deadline is `STORY-0412`'s split. The first ticket that handles a non-`200` from sign-up
writes an answer to this whether or not anybody decided one, and the cheap answer is the generic
one. Deciding before the split costs nothing; deciding after means changing shipped copy, a shipped
mapping and a shipped test.

## Decision

### 1. `429` is its own outcome on the sign-up form, and it is never the generic failure

The sign-up form renders **three** kinds of outcome, not two: success; a refusal about something the
player typed (`400`, `409`, `422`, and `401`'s re-identification); and **this** — a refusal about
neither the player nor their input.

`429` maps to the third state and to nothing else. A `500`, a `503`, a timeout and a rejected
`fetch` keep the generic *unavailable* treatment the client already gives an unknown status, and
**no other status joins the throttled state**. A player can therefore tell "the product is refusing
me on purpose" from "the product is broken", which is the distinction the bare status destroys.

This applies to the **sign-up** form only. Sign-in has no throttled state to render, because
`ADR-0027` §6 makes its over-budget answer identical to a wrong password; a client that shares one
error mapper between the two forms must not manufacture one there.

### 2. What the message may state, and what it may not

Three facts it **may** state:

1. **Sign-up cannot be completed right now, and the reason is where the request came from, not who
   sent it.** The attribution is to the connection or the network, never to the player.
2. **Nothing they typed was refused.** No field rule rejected the handle or the password, and no
   account was created.
3. **Nothing is lost and nothing is required.** They are still the profile they already were, their
   duel coins and their duels are untouched, they can keep playing now, and they can sign up later.
   (`ADR-0036`: the account is an offer.)

Five things it **may not** state:

- **No time.** No deadline, no countdown, no timer, no *"15 minutes"*, no *"5 attempts"*, no number
  derived from `ADR-0055`'s config. It may say the refusal is temporary; it may not promise when it
  ends, because the client does not know and the window moves.
- **No verdict on a field.** Neither input is marked, neither is described as wrong, and no
  alternative handle is suggested.
- **No claim about the handle's availability, in either direction.** The request never reached the
  taken-handle check — `ADR-0055` §1 puts the budget test before `Credentials.create` — so *"that
  name is still free"* is a fact nobody established.
- **No accusation.** No second person, no *"you have tried too many times"*, no *suspicious*, no
  *blocked*, no *banned*. See §4 for why.
- **No mechanism, and no fault.** No *rate limit*, *throttle*, *budget*, *security*, no error code,
  and nothing that reads as *something went wrong*.

The exact words and where they sit are `STORY-0412`'s inside `EPIC-06`'s design language, exactly as
`ADR-0048` §7 has it. This ADR fixes the three facts and the five prohibitions and nothing else
about the copy.

### 3. The form keeps everything, and the client never retries by itself

- **Both fields keep their values, the password included, exactly as typed.** Nothing is cleared,
  nothing is re-masked differently, nothing is re-generated. The refusal was not about the secret,
  so the secret is not the thing that pays for it.
- **Neither field is marked**, and no field-level message appears.
- **The submit control stays enabled.** The player may press it again whenever they like; that is
  their call, and a disabled button with no stated deadline is a dead end the client cannot honestly
  open again.
- **No automatic retry of any kind** — no timer, no backoff, no background poll, no *"retrying…"*.
  An over-budget request still counts (`ADR-0055` §1), so an unattended retry lengthens the very
  wait it is trying to end. The one action that makes things worse must never be one the client
  takes on its own.
- **No *Retry now* affordance.** The state's way out, if it offers one, is the way back to playing.
- The message clears on the next submit, and that submit's outcome replaces it.

### 4. One message, and it is written for the person who did nothing

Nothing distinguishes the bystander from the person who spent the budget, and nothing will: the key
is the remote address (`ADR-0055` §4) and the body is empty, so the server does not know either.
Both people see the same words.

The tie is broken in one direction on purpose: **the message is written for the bystander.** A
message that is wrong for the person who exhausted the budget is harmless — they know what they did.
A message that is wrong for the bystander accuses somebody whose only act was pressing submit once.

Stated plainly because it is the cost being accepted: **this product cannot tell those two people
apart, will never tell either of them which they are, and will never tell the second one to stop.**

### 5. A refusal spends nothing

A `429` consumes no handle (no row was written), no session, no profile, and **not the offer**:
`ADR-0036`'s prompt is dismissed by the player choosing *"Not now"* and by nothing else, so a
throttled sign-up leaves the offer exactly where it was.

### 6. What the backlog gains

- **`STORY-0404`: nothing.** No ticket, no criterion, no copy. It ships no limiter (`ADR-0055` §6)
  and therefore cannot answer `429`; its three *"rate limiting is out of scope"* notes stand
  verbatim for the second time.
- **`STORY-0405`: nothing on the wire, and one prohibition.** The `429` keeps its empty body — no
  `Retry-After`, no `message`, no `retryAfterSeconds`, no `problem+json`. The screen described here
  needs only the status, and it is designed that way so that nobody adds a field to help it. The
  `docs/protocol.md` row and `ADR-0048` §6's seventh row are still that story's, unchanged in shape;
  the only addition is the note that the body is empty by **product** decision as well as by
  transport decision.
- **`STORY-0412`: the work.** One new outcome state on the sign-up form; the mapping rule in §1; the
  preserved fields and the no-auto-retry rule in §3; and a copy brief for `EPIC-06` consisting of §2's
  three facts and five prohibitions. Five acceptance criteria, written so a test can fail:
  - [ ] A `429` renders the throttled state while a `500`, a `503` and a rejected `fetch` each render
        the generic unavailable state — **all four asserted together**, so a mapping that returns one
        constant cannot pass.
  - [ ] After a `429`, the handle field and the password field hold exactly the strings they held
        when submit was pressed, asserted on both values with a password that is not the fixture
        default.
  - [ ] After a `429`, neither field is marked invalid and no field-level message is rendered.
  - [ ] After a `429`, no further request is sent until the player submits again — asserted by
        request count after the client's timers have been advanced.
  - [ ] The rendered throttled message contains **no digit**, which is the objective proxy for §2's
        *no time, no counts*.
- **`STORY-0415`: one prohibition and one criterion** — a `429` on the offer's form is not a
  dismissal (§5), and the sign-up the player accepted is still there with what they typed.
  *Amended by [`ADR-0085`](ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md) §7,
  which supplies this wording.* §5's holding is untouched — a `429` consumes no handle, no session,
  no profile and dismisses nothing — but what the player returns to is the **form**, not the
  result-screen prompt: under `ADR-0085` §2 taking the offer is itself an answer, so the prompt has
  already been spent by the time a `429` can happen.

## Consequences

**What it buys.** The `429` arrives as what it is: a temporary refusal, about the connection, aimed
at nobody, costing the player nothing they already have. The retry reflex that would extend the
window is not manufactured by the copy and not automated by the client. A player who cannot sign up
keeps the profile, the coins and the duel they were about to play, which is what `ADR-0036` promised
and what a generic error would have quietly retracted. And `STORY-0412` is split with this branch
already decided, rather than filled in by whoever is typing.

**What it costs.**

- **`ADR-0055`'s disclosure gets louder.** That ADR measured the leak as a status code — visible to
  somebody who opens devtools. This prints it in a sentence, so on a shared address the message tells
  every player something true about their neighbours' behaviour that they had no other way to learn.
  It is the same fact `ADR-0055` shipped knowingly, and it is strictly more legible. Accepted because
  the alternative is lying to the person who did nothing.
- **The player is told to wait and never told how long — and *later* can be much later.** If somebody
  keeps hammering the address the window keeps rolling, and the screen says the same calm thing every
  time, with no way to distinguish two minutes from an hour. The only lever that exists is
  `AUTH_SIGN_UP_MAX_ATTEMPTS` / `AUTH_SIGN_UP_WINDOW_MILLIS`, which `ADR-0055` already recorded an
  operator can only reach **after** discovering the problem.
- **A typed password sits in a form on exactly the machine class that produced the refusal.** A café,
  a library or a shared office is both the shared address and the shared keyboard, and this state can
  stay on screen for the length of the wait — longer than any other refusal on this form leaves it.
  The field is masked and the player typed it deliberately, so nothing is exposed that was not
  already there; what changes is how long it is there.
- **One message for two people means the product never tells an abuser to stop.** The only deterrent
  a message could carry is deliberately given up, in exchange for not accusing a stranger.
- **A state that will almost never render is tested and maintained from the day it ships.** `EPIC-07`
  hosts nothing and v0.1 has no players, so the first `429` a real person sees may be months away or
  never — and the copy still has to be written, designed, kept in `EPIC-06`'s language and kept true.
- **The client cannot tell this `429` from one `EPIC-07`'s front end invents for its own reasons.**
  Same status, same state, same words — and the words would be technically true while being about
  something else entirely. Nothing in the client can detect the difference, and this ADR does not
  give it a way to.

**What it forecloses.**

- **A countdown, a timer, or any *"try again in N"* on this screen**, permanently, unless
  `Retry-After` ships. `ADR-0055` left the header additive; the client-invented substitute is refused
  here for good. If the header ever arrives, this ADR is the thing to revisit rather than the thing
  to work around.
- **Folding `429` into the generic failure state** — the cheap default, and the temptation on every
  later form the client grows.
- **Any per-field treatment of this refusal, ever.** Once the product has said *nothing you typed was
  refused*, moving the `429` onto the handle or the password field later makes a liar of the version
  that shipped.

**What this does not settle.**

- **The words and the visual treatment.** `STORY-0412` inside `EPIC-06`, exactly as `ADR-0048` §7
  left the password sentence.
- **Any other endpoint's `429`.** Sign-in's over-budget answer is a wrong-password answer
  (`ADR-0027` §6) and `forgot-password`'s is a `202` (`ADR-0031` §5). This is one status on one form;
  the sentence to carry forward is still `ADR-0055`'s — *a limiter may answer `429` when the
  endpoint's ordinary refusals are already informative* — and the next screen argues its own case.
- **Whether the client is ever given a deadline.** That needs `Retry-After`; it is the architect's if
  anybody wants it, and nothing here asks for it.
- **Whether a NAT'd population is actually served.** A message is not a fix; the two config values
  are, and they are `ADR-0055`'s.

## Alternatives considered

**The generic treatment — the same *something went wrong* any unexpected status gets.** Its strongest
case is real: it is zero new UI, zero new copy and zero design work; it is what
`web-client/src/profile/api.ts` already does with every unknown status, so it is the consistent
choice as well as the cheap one; and it is the perfect non-oracle at the presentation layer,
recovering at the screen some of the disclosure `ADR-0055` spent on the wire. Rejected because it
converts a deliberate, working refusal into evidence that the product is broken, at the one moment
`ADR-0036` says a player was doing us a favour — and because the reflex it produces is an immediate
retry, which under `ADR-0055` §1 extends the window. It is the only option here that makes the
player's situation materially worse rather than merely describing it badly.

**Name the wait — *"try again in 15 minutes"*, or a live countdown.** The strongest case in this
list: when the only available action is waiting, the single most useful thing you can say is how
long, and turning an indefinite dead end into a definite one is what every well-behaved rate-limited
form does. Rejected on four counts, each sufficient. The client has no deadline — `ADR-0055` §3 ships
no `Retry-After` and no body — so the number is invented. The window is rolling and an over-budget
request extends it, so even a true number stops being true when anybody sharing the address presses
submit. The number is an environment variable an operator may change, so shipped copy goes stale
silently and nothing fails. And a countdown is exactly the affordance that invites a player to sit
and hammer the button at zero.

**Mark the password field, or the handle field.** Its strongest case is that the form already owns a
refusal state for both, so `STORY-0412` designs nothing new and the player gets a familiar shape —
the same argument `ADR-0055` weighed when it considered answering `422`. Rejected for the same reason
that ADR refused the status, made worse by the medium: a status code is an ambiguous lie, a sentence
is an unambiguous one. The player changes a good password or abandons a free handle, and the second
attempt fails identically.

**Clear the password, or clear the whole form.** Its strongest case is genuine and this ADR concedes
half of it: a masked secret should not sit on a machine that might be shared, and the shared machine
is precisely the case that produces this refusal — plus clearing on a failed authentication form is
the convention. Rejected because the refusal is not about the secret. Nothing was verified, nothing
was wrong, and `ADR-0048` §3 deliberately permits a password no human can retype, so the cost of
clearing lands on the player who was doing everything right and is charged for a stranger. What
survives from the rejected case is recorded as a cost above rather than argued away.

**Two messages — one for the player who spent the budget, one for the bystander.** Its strongest case
is that they really are two different people in two different situations, and the culprit could be
told plainly to stop while the bystander is reassured. Rejected because the server cannot tell them
apart and never will, so the distinction would have to be a client-side count — wrong after a reload,
wrong in a second tab, wrong on a second device, and wrong in the one direction that matters, since
it would accuse the bystander whose only sin was submitting twice. Two messages are also two things
to keep true.

**Disable the submit control after a `429`.** Its strongest case is that it stops the hammering that
extends the window, which is the one behaviour the budget itself cannot defend against. Rejected
because re-enabling needs a deadline the client does not have: a permanently disabled button is a
dead end on a form the player may legitimately retry in ten minutes, and a button re-enabled by a
timer is the invented number above in a different costume. Refusing to auto-retry buys most of the
benefit and invents nothing.

**Say nothing — stop the spinner and leave the form as it was.** Its strongest case is that it is the
quietest option available, needs no copy at all, and discloses nothing. Rejected because an
unexplained non-event is worse than an explained refusal: the player presses submit again
immediately, which is the retry we are trying not to cause, and the product looks broken while
saying nothing at all.
