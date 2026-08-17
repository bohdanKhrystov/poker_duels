# ADR-0050 — Revoking the device signs the player out everywhere but here

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-045` — does revoking a device also end every other session that player holds
  ("sign out everywhere"), or are the two separate affordances on the account screen? **Derived from
  the vision; the human did not state this call.** The licensing sentence is *"The reference points
  are Lichess and Chess.com, not PokerStars. Dark, quiet, fast, minimal."* One action that keeps one
  promise is the minimal shape here; two actions whose difference is the difference between a *route*
  and a *token* is not minimal, it is a distinction moved onto the player. Read beside
  [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md)'s shipped rule that the account
  screens must state which routes are live, chosen because the status quo *"cannot be described
  honestly on a screen that also asks for a password"*
- **Amends:** [`ADR-0049`](ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §2's last
  bullet (*"this writes nothing to `player`, `credential`, `auth_session`, `duel` or
  `duel_result`"* — `auth_session` leaves that list, the other four stay) and §6's first bullet
  (*"revocation writes nothing to `auth_session`"*), which that ADR took explicitly as the reversible
  default pending this decision. Its §5 endpoint, guards and status codes, its §6 socket rule, its
  finality trigger and its schema are untouched
- **Builds on:** [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md) (revocation
  exists, does not kill the revoking session, and the screens must say which routes are live),
  [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §2 (the session row, the thirty-day
  absolute expiry, `auth_session_player_id_idx`, and the refusal of a devices screen),
  [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §4 (the one existing precedent: a
  reset deletes every session in the same transaction),
  [`ADR-0036`](ADR-0036-an-account-is-offered-never-required.md) (why an account is offered at all)
- **Constrains:** `STORY-0406` (the endpoint gains one statement and one criterion — a correction to
  `ADR-0049`, see §4) and `STORY-0412` (what the screen says before it acts)
- **No schema change, no migration, no new route, no `ProfileResponse` field.** The `DELETE` is
  served by the index `ADR-0027` §2 already built
- **No wire change.** `PROTOCOL_VERSION` does not move; this is plain HTTP

## Context

`ADR-0049` settled the shape of revocation and left one thing open on purpose. Its §6 records the
residual in as many words: **a session token already sitting on the revoked device keeps working for
up to thirty days.** The schema is indifferent — closing it is one
`DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?` on the same endpoint, against an
index that exists. Nothing here is about cost or feasibility. It is about what a player is promised
when they press the button.

What is genuinely in tension:

**The button exists for the player who no longer holds the machine.** `ADR-0037` built revocation so
that *"a cautious player can reach the strong guarantee"*. Sold, lost, stolen, or a shared computer
in a house — that is the person who opens this screen. For exactly that person the thirty-day token
is the part that matters, it is the part they cannot see, and under `ADR-0049`'s default the button
does not touch it.

**`ADR-0036` already said why the account exists.** *"A device-bound profile is one dropped phone
away from nothing"* is the reason the product offers an account at all. An account whose device
affordance is incomplete precisely in the losing-the-device case contradicts the reason it was
offered. That is a contradiction inside a chain of merged decisions, not a matter of taste.

**But the two acts have opposite reversibility, and this is the real argument for two buttons.**
Revocation is final forever: `ADR-0049` §2's trigger refuses `revoked_at → NULL` and its primary key
refuses the old pair, so nobody — no admin, no `psql` session — can give the route back. Signing out
is the cheapest act in the system; the remedy is to sign in again, with a password that
`ADR-0037`'s "offered only when a credential exists" rule guarantees exists. Binding the cheap act to
the permanent one means a player who wants only the cheap one pays the permanent price.

**The distinction the two names would rest on is one the player does not have.** `ADR-0049` §6 needs
a paragraph to keep it straight for engineers: expiry is a property of a *token*, revocation is a
property of a *route*. Two buttons ask a player to make that distinction, and the button whose name
matches the emergency — *that machine, the one I no longer have* — is the one that leaves the token
alive.

**Sessions are rare in this product, which changes the arithmetic of "everywhere".** `ADR-0049` §6
leans on the same fact: `STORY-0404` fixes that sign-up issues **no** session, so every
`auth_session` row was minted by a sign-in proving a password. A player standing at this screen
typically holds one or two, and one of them is usually the machine they are worried about.

**A selective sign-out is not available at any price.** `ADR-0027` §2 declined `ip`, `user_agent`
and `last_used_at` because they exist to power a devices screen nobody asked for; `ADR-0049` §9
refused the listing again, because a device id is a bearer credential and handing one to a
session-holder hands over the profile. So the choice is genuinely between *all but here* and *none*,
and no third option is on the table.

**One precedent exists, in a merged ADR, for this exact shape of act.** `ADR-0031` §4: a successful
password reset deletes every `auth_session` row for that player, in the same transaction as the
password write, because *"leaving their 30-day session alive would make the reset theatre."* The
sentence is true here with one word changed.

### The deadline, honestly

Nothing here is irreversible in the software: no schema, no migration, no wire step, and the `DELETE`
can be added or removed on any day. Two things do have a clock.

**`STORY-0406` writes the endpoint next**, and it is the story after the one in flight. Deciding
after it is written means the endpoint ships, then ships again — `ADR-0049` was right that the change
is additive, and it is still one PR nobody needs to spend.

**The copy is the part that does not come back.** Once the button has promised *signed out
everywhere*, narrowing it later withdraws a security promise from players who learned it, which is
the failure mode `ADR-0036`'s rejected alternatives already named: withdrawing a capability players
have is worse than never offering it. The direction that stays cheap forever is the other one —
adding a standalone sign-out later.

## Decision

**One button. Revoking the device also ends every other session that player holds, and the account
screen says so before it acts.**

### 1. One affordance, and it is `ADR-0049`'s endpoint doing two statements

`DELETE /api/me/device` — unchanged in route, verb, empty body, both guards (`401` without a session,
`409` without a credential) and its `204` — performs both writes in one transaction:

```sql
UPDATE device_binding SET revoked_at = now()
 WHERE player_id = ? AND revoked_at IS NULL;

DELETE FROM auth_session
 WHERE player_id = ? AND token_hash <> ?;
```

- **There is no second endpoint and no standalone "sign out everywhere."** v0.2 ships one action on
  the account screen and that action keeps one promise.
- **The `DELETE` runs unconditionally**, whether or not the `UPDATE` touched a row. `ADR-0049` §5
  already makes the *answer* uniform — `204` whether the binding was live, already revoked, or never
  created — and an effect that were conditional while the answer is uniform would mean a player who
  was never bound presses the button, is told nothing, and keeps the other session running.
- **It is served by `auth_session_player_id_idx`** (`ADR-0027` §2), which was built for exactly this
  statement: *"revoking everything is a delete by `player_id`, which the index serves."*
- **`player`, `credential`, `duel` and `duel_result` are still untouched**, so `STORY-0406`'s
  byte-identical-`player` criterion and `ADR-0030` §5's P1/P2 hold exactly as `ADR-0049` left them.

### 2. The revoking session survives, so the words are "except here"

- **The excluded row is the caller's own**, identified by the SHA-256 of the token they presented and
  never by anything in a request body. `ADR-0049` §5 requires a session (`401` otherwise), so there
  is always exactly one row to exclude and it is always the caller's: `ADR-0037`'s *"revocation does
  not kill the revoking session"* holds by construction rather than by care.
- **Because it survives, the screen says "everywhere except here", never "everywhere."** A screen
  that tells a player they have been signed out everywhere while they are demonstrably still signed
  in is the small dishonesty `ADR-0037`'s screen rule exists to prevent, and it is the kind that
  teaches a player not to believe the next sentence.
- **No live socket is closed.** `ADR-0049` §6's rule is untouched and its reason is unchanged: a
  socket torn down mid-duel abandons a seat, `ADR-0013` and `ADR-0023` fold it, and an identity
  operation would have cost somebody a coin. A duel already running on a swept device plays to its
  end, because identity is fixed at `Hello` and a socket re-reads no session (`ADR-0027` §3).
  Everything else — the next request, the next `Hello`, the next reconnect — is refused.

### 3. The words

`EPIC-06` owns the visual language and may letter-fit the strings. What follows is the promise and
the facts, which are not letter-fitting.

- **One action, beside the routes-are-live statement `ADR-0037` requires**, labelled with the
  permanent half, because the permanent half is the one that must not surprise:

  > **Stop this device signing in**

- **Offered only while the device route is live** (`ProfileResponse.deviceRouteLive`, `ADR-0049` §5)
  and only when a credential exists — `ADR-0037`'s rule and `STORY-0412`'s existing criterion, both
  unchanged. After revocation the screen states that the device no longer signs in, and offers
  nothing.
- **One confirmation step, stating three facts**, in whatever words `EPIC-06` settles on:
  1. **It is permanent.** *This device will never sign in to this account again. This cannot be
     undone.*
  2. **It ends the other sessions.** *You will be signed out on every other device. You stay signed
     in here.*
  3. **The password becomes the only way back**, and for a player with no recovery email attached,
     `ADR-0037`'s requirement that the affordance says so at the moment of revoking.
- **No count and no list of other sessions.** A count is the first column of the devices screen
  `ADR-0027` §2 declined and `ADR-0049` §9 refused, it needs a field on `ProfileResponse` that does
  not exist, and it tells a player nothing they can act on.
- **The screen does not say "revoke".** That word belongs to the schema, this register and
  `ADR-0049`'s title. A player reads what happens.

### 4. What changes where — and a correction to `ADR-0049`

- **`STORY-0406` changes.** `ADR-0049` said it *"ships under either answer"*; that was true while the
  answer was unknown and it is now moot, because the answer arrives before the story is split. The
  `DELETE` lands **with** the endpoint, not after it. The story gains one acceptance criterion:
  *a second session held by the same player stops working immediately after revocation, while the
  revoking session still works* — both asserted by using both tokens, which extends the criterion
  already there for the revoking session alone.
- **`STORY-0412` changes** by gaining §3's confirmation. It needs no new server fact:
  `deviceRouteLive` is the whole of what the screen reads.
- **Nothing else moves.** No migration, no new route, no `ProfileResponse` field, no
  `PROTOCOL_VERSION` step, and `poker-engine` learns nothing — no session, device or credential type
  exists in it or crosses into it.

### 5. What is deliberately not built

- **No standalone "sign out everywhere."** Not a second button, not a second endpoint, not a flag on
  this one. If one is ever wanted it is additive — a new route running the same `DELETE`, no schema
  change — and *Consequences* names the player who will ask for it.
- **No per-session list, no per-session sign-out, no device names, no "last used."** Refused twice
  already; a third refusal changes nothing.
- **No change to session lifetime.** Thirty days absolute (`ADR-0027` §2) is what a *surviving* token
  gets. This decision only says which tokens survive.
- **No forced socket teardown**, for `ADR-0049` §6's reason.
- **Nothing new is explained to the player about tokens, expiry or sessions as a concept.** The
  screen speaks in devices and signing in.

## Consequences

**What it buys.** `ADR-0037`'s *"a cautious player can reach the strong guarantee"* becomes true
rather than nearly true: the player who sold, lost or lent the machine presses one thing and every
route it had is gone — the device route forever, the token immediately. The screen can state one
promise and have it be complete, which is the test `ADR-0037` set for this exact feature. It closes
the gap `ADR-0049` recorded as its own second-largest cost without a migration, a route or a wire
step. And the product ends up with one rule for *make sure nobody else is inside*, shared with
`ADR-0031` §4's reset, rather than two rules that differ for no reason a player could state.

**What it costs.**

- **A player who has already revoked has no way to end a session at all.** The action is offered only
  while the device route is live, so the second time somebody worries — a session left on a friend's
  laptop, a month after revoking — there is no button, no per-session list, and no
  change-password-while-signed-in endpoint anywhere in `ADR-0031` §5's table. Their remedies are a
  password reset, which sweeps sessions, *if* they attached a recovery email, and otherwise waiting
  out thirty days. This is the sharpest cost of choosing one button, and it is the case that will
  eventually justify a second one.
- **A cheap, repeatable act now costs an irreversible one.** A player who wants only to sign out
  everywhere must permanently give up their device route to get it, and `ADR-0049`'s trigger means
  nobody can give it back. The confirmation is the whole mitigation, and a confirmation is a weaker
  guarantee than the constraint-shaped ones around it.
- **Every other session dies and the player cannot spare one.** Their own phone and tablet are signed
  out alongside the machine they were worried about, and there is no list to be selective with. Cheap
  today because sessions are rare; it grows with however many devices a player later uses.
- **"Except here" is not instant everywhere.** A duel already running on a swept device plays to its
  end. The exception is bounded by one duel rather than by thirty days, and it is deliberately kept
  off the screen: it is unreadable to the player who needs that screen, and what it leaves open is
  the ability to finish a duel, not to enter the account. It is recorded here so that the next reader
  of the promise knows exactly where it stops.
- **The endpoint is no longer one statement**, so `ADR-0049` §2's *"it is the whole transaction"* is
  now two writes against two tables, and any test asserting `auth_session` is untouched by revocation
  must change with the story rather than be discovered failing.
- **Revocation can never be quiet.** Every use of it has a visible consequence on the player's other
  devices, so it can never be a background tidy-up; it is always an event, including for the player
  who is merely retiring an old laptop tidily.

**What it forecloses.**

- **Describing revocation narrowly, later.** Once the button has promised the sweep, taking the sweep
  away withdraws a security promise from players who learned it. The other direction stays open and
  cheap, which is why the evidence being thin points this way.
- It does **not** foreclose a standalone sign-out-everywhere, a change-password endpoint that sweeps
  the same way, or per-session control — which is refused by `ADR-0027` §2's missing columns and not
  by this ADR.

## Alternatives considered

**Two affordances — "Stop this device signing in" and "Sign out everywhere" — each doing exactly what
its name says.** The strongest case, and it turns on reversibility rather than on tidiness: the two
acts are not the same kind of object. Revocation is final forever; signing out is the cheapest act in
the system. Two buttons let the cheap act stay cheap, keep the permanent one behind its own warning,
and let a player who left a session on a friend's laptop fix exactly that without surrendering their
own device route — the case this decision's *Consequences* has to admit it cannot serve. It is also
the shape most players have already met elsewhere. Rejected on the failure mode rather than on the
design: the two names differ by the difference between a route and a token, and the button whose name
matches the emergency is the one that leaves the token alive for thirty days. The player presses it
and believes they are finished. A screen closes that only by teaching the distinction, which is
neither quiet nor minimal, and a security affordance whose correctness depends on a stressed player
reading two paragraphs correctly is not one. What is lost by rejecting it stays available additively;
the silent failure would not have been.

**One button, revoking only, with the gap stated on the screen** — `ADR-0049` §6's default made
permanent, plus a sentence like *"A device that is already signed in stays signed in for up to 30
days."* Its case is honesty at the lowest possible price, and it keeps revocation the narrow,
single-meaning act the schema says it is; it is also the option that needs no new statement at all.
Rejected because that sentence is an admission with no remedy attached, printed on the one screen
whose purpose is to provide the remedy, and read by the one player who cannot act on it. `ADR-0037`
chose revocation over the status quo precisely because the status quo *"cannot be described honestly
on a screen that also asks for a password"* — a revoke button that leaves the lost machine signed in
reproduces that defect one layer down.

**One button, sweeping the revoking session too** — sign out everywhere, full stop, and the player
signs straight back in with the password they must have. Its case is that it is the simplest promise
on offer, it needs no `token_hash <> ?`, and it matches `ADR-0031` §4 exactly, where a reset signs out
everybody including the person who did it. Rejected because `ADR-0037` already decided it: *"Signing
someone out of the screen they are using to secure their account is hostile."* The reset case is
genuinely different — there the player is choosing a new password and typing it once more proves they
kept it, whereas here they may not have the password in front of them at all, and the screen they
would be thrown out of is the one that just told them their password is now the only way in.

**Sweep the sessions, but leave the choice to the client** — a flag in the request body, or two
endpoints the screen calls one after the other. Its case: the server keeps both behaviours, the
product can change its mind by changing a screen, and nothing is foreclosed. Rejected because it
moves the decision into the client, where each screen answers it separately and no artifact records
which answer is the product's. A promise made by a screen and not by the server is a promise no test
can hold anybody to.

**Shorten the session lifetime instead, so the gap closes itself.** Its case is real: it needs no new
statement, no copy and no argument about what a button means, and it shrinks the residual for every
device rather than only for revoked ones. Rejected because it is a global tax for a local problem —
every player signs in more often to mitigate a case that arises rarely — because `ADR-0027` §2 chose
thirty days deliberately, with no sliding window and no idle timeout, and above all because it does
not close the gap. It only makes it shorter, and the player still cannot see it or act on it.

## What this does not settle

- **Whether a standalone "sign out everywhere" is ever offered, and where.** Not registered as a
  `DEC` today, because nothing is blocked by it and no story wants it; it is additive whenever
  somebody asks. The player who will ask is named in *Consequences*.
- **How a player who suspects their password is known, and who has no recovery email, ends a
  session.** `ADR-0031` §5's endpoint table has no change-password-while-signed-in path, so today
  the answer is that they cannot. That is a hole in the identity chain rather than in this decision,
  and it belongs to whoever next reads `EPIC-04`'s endpoint list end to end.
- **The exact strings.** `EPIC-06` owns the visual language. The three facts §3 requires the
  confirmation to state are not part of what it may re-scope.
- **Sockets.** `ADR-0049` §6's rule that revocation closes none is untouched, and is not re-argued
  here.
