# ADR-0079 — Five to attach, ten to forget, and the attach budget is the only cap on the mail it causes

- **Status:** Accepted
- **Date:** 2026-08-25
- **Resolves:** `DEC-073` — the two numbers for each of `POST /api/auth/recovery-email` and
  `POST /api/auth/forgot-password`, and whether an over-budget attempt still counts against its own
  window
- **Builds on:** [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §5 (the mechanism, the
  key and the answer — all three fixed there, none re-decided here, and its fifteen-minute
  suppression, whose reach is the whole question),
  [`ADR-0055`](ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md) §§1, 2 and 4 (the
  `AttemptBudget` type, the config pattern, *a refusal that costs nothing costs no budget*, the
  address key and its NAT caveat),
  [`ADR-0074`](ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) §§1–3
  (the sign-in pair, the placement question, and the one rule about hammering),
  [`ADR-0077`](ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md) (the
  detached send these budgets sit in front of, and `NoRecoveryMailer`, which is why the exposure
  starts on the day a sender is configured),
  [`ADR-0078`](ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) (the syntax predicate that
  runs before the attach budget and is deliberately not behind it)
- **Extends:** nothing. `AttemptBudget` gains no member; `refund` exists and **neither** of these two
  endpoints calls it (§3)
- **Constrains:** `TASK-041628` (four config values, two instances, two call sites, seven tests);
  `TASK-041625` and `TASK-041626`, each of whose fixed handler order gains **one line in a place
  named here**; `EPIC-07`'s deployment, which owns both the forwarded-header configuration this key
  depends on and the sender that turns §2's exposure on
- **Leaves open:** a **per-account resend suppression on the attach path**. `ADR-0031` §5's
  fifteen-minute rule is `PasswordResets.issue`'s alone; `RecoveryEmails.claimPending` has no
  equivalent, and this ADR's numbers assume it never gets one. Named in §Consequences with the
  condition that closes it. **Not a `DEC`** — §5 already fixes the mechanism and the number for the
  identical problem on the sibling table, so there is nothing in tension and it is a ticket

## Context

`ADR-0031` §5 settled three of the four things a limiter needs and left the fourth. The mechanism is
`ADR-0055` §2's `AttemptBudget` — a rolling window, state in memory, time from `ServerClock`. The key
is the remote address. The answer over budget is `202`, byte-identical to success, so the limiter is
not itself an oracle. What is left is two pairs of numbers and one semantic.

### The register's premise, checked before it is used

`DEC-073` states that `ADR-0074`'s argument does not transfer, because that ADR turned on the
collateral of a shared address being *visible* — a refused player is told their password is wrong,
and a room full of people can at least perceive that something is happening — while here a refused
player is told `202` and cannot pace, and their neighbours' damage is silent. That is correct, and it
is checked rather than inherited: §5's `202` conflates five cases on `forgot-password` by design, and
`ADR-0056` §1 has already had to say, of sign-in, that a shared error mapper must not manufacture a
throttled state on a form that has none — which these two forms have not either.

But it is not the thing that decides the numbers, and it cuts in a direction the register does not
name. Invisible collateral is a reason to be **generous**, not tight: a limiter nobody can perceive
is one nobody can work around, so the only safe place to put it is where it cannot bite a human
being. What decides the numbers is that the two endpoints, read against what has actually been
built, turn out to be defending different things.

### What the fifteen-minute rule actually covers, read from the tickets rather than from the paragraph

§5's *Budgets* paragraph opens by naming both endpoints and then says the durable defence is the
row: *"a mail is sent only if the player has no live token issued within the last 15 minutes, read
from `issued_at` on the existing `UNIQUE (player_id)` row."* Both `password_reset` and
`email_verification` have an `issued_at` and a `UNIQUE (player_id)`, so the sentence reads naturally
as covering both paths. **The split implements it on one.** `TASK-041613` puts the check inside
`PasswordResets.issue`, which answers `false` and writes nothing inside the window. On the attach
path, `TASK-041607` fixes `RecoveryEmails.claimPending` as returning **`Unit`** — *"there is no
outcome for a caller to branch on"* — and `TASK-041608` has it `DELETE` then `INSERT`
unconditionally. A verification mail therefore goes out on **every** successful attach, for ever.

That asymmetry is the whole of this decision:

| | `POST /api/auth/forgot-password` | `POST /api/auth/recovery-email` |
| --- | --- | --- |
| Who the mail goes to | an address the **server** already holds, verified | an address the **caller just typed** |
| Per-account cap on that mail | **yes** — four an hour, durable, across restarts and across every source address at once | **none** |
| What a request costs the server | one indexed read; on a hit, a mint, a two-statement transaction and a detached coroutine | the same, **plus one Argon2 verify of the current password** |
| What it takes to send one | a corpus of addresses already verified on this product | a session and its own password |
| Who is refused when the budget bites | a player who has lost their password | a player setting recovery up |

### What `forgot-password`'s budget is still for, once the fifteen-minute rule exists

Three candidate jobs, in the order people name them.

1. **Mail-bombing one victim: it adds nothing.** The fifteen-minute rule caps a single account at
   four recovery mails an hour, durably, across restarts, and — the part that matters — across
   *every source address at once*, which is exactly what an address-keyed budget cannot do, since a
   second source address gets a fresh budget. On the attack this endpoint is most often accused of
   enabling, the address budget is strictly the weaker of the two limiters and bounds nothing the
   other does not already bound harder.
2. **A spray across many accounts from one source: it is the only cap there is** — the fifteen-minute
   rule is per-account and cannot see an aggregate — **and the aggregate is hard to aim.** Causing a
   single mail requires an address already *verified* on this product, and nothing in this system
   will say which addresses those are: §5's `202` conflates unknown, pending, sent, over budget and
   no-sender-configured; §6.3 keeps the address out of every response body, message and log line; and
   `verifiedOwnerOf` returns an id. A spray of a breach corpus produces no mail at all for every
   miss, and no way to learn which entries were misses.
3. **The endpoint's own cost: generic.** It is unauthenticated and each admitted request buys one
   indexed read, plus on a hit a token mint, a transaction and a detached coroutine. True of any
   cheap endpoint, and no in-process per-address limiter is the right instrument for it.

The honest summary is the answer `DEC-073` asked for: **on `forgot-password` the address budget is a
modest, trivially-evaded second line behind a durable first one.** That is a reason to put it where
it cannot bite a human, not a reason to make it tight.

And its refusal costs more than any other refusal in this system. The person on the other side has
lost their password. They are told `202` — mail is on its way. Nothing arrives. There is no status,
no client state and no support channel, and `ADR-0031`'s Consequences make no recovery a **total,
permanent** loss of an account, its coins and its ladder place. A player who concludes that recovery
does not work here reaches that loss through a system that is working exactly as designed.

### What `recovery-email`'s budget is for, which is more

Two jobs, both concrete.

1. **It is the only cap on outbound verification mail, and the caller chooses the recipient.**
   Unbudgeted, one account and one script emit mail from this product's sender to arbitrary mailboxes
   at whatever rate the server sustains. That is a spam relay with a `RecoveryMailer` in front of it,
   and *recovery only* (§6) is the spine of the entire email design — §5 refuses even a *"this
   address is already in use"* mail on the ground that it would reach a mailbox whose owner did
   nothing. The cost is not the bill; §7 defers the transport and therefore any bill to `EPIC-07`.
   The cost is that a sender domain which has emitted unsolicited mail stops being delivered, which
   silently breaks recovery for every player who opted in.
2. **It bounds Argon2, and it is a second door to the guess `ADR-0074` budgeted at the front.** This
   endpoint verifies the current password inside a valid session, and §3 says why: a session token is
   a bearer credential in web storage, and the password is what stands between a minute at an
   unattended browser and permanent ownership of the account. So a thief holding a session guesses
   the password *here*, against the same four `limitedParallelism` slots `ADR-0055` measured at ~50
   hashes a second for the whole server. `ADR-0074` priced that guess at ten wrong passwords a minute
   at the front door. **This door must not be cheaper than that one**, and nothing else in the system
   makes it so.

### One window, sixty seconds, for a reason and then a second reason

`ADR-0074` §Context established that the window is the recovery time, and that a long window
combined with an *over-budget-still-counts* rule is what turns a burst into a lockout. That
interaction is worse here than at sign-in, because a player cannot see it: at sixty seconds a person
who presses the button again a minute later succeeds; at fifteen minutes a hammered address is
switched off for a quarter of an hour past the last attempt and nobody behind it can tell.

There is a second reason, smaller, and it is a property of the shipped type rather than of the
design. `AttemptBudget` keeps one timestamp per recorded attempt in a list per key and prunes by a
full scan on every call. With §4's rule, a hammered key's list grows with the attacker's rate times
the window, and each subsequent `admit` scans it under that instance's `Mutex`. A sixty-second window
holds that at a fifteenth of what sign-up's window already accepts. The map's unbounded size is
`ADR-0055`'s recorded cost and `TASK-041628` puts it out of scope; the window is the lever this
decision does hold, and it is set knowing that.

### The deadline, honestly

Nothing here is free today and impossible later: four environment variables and one line in each of
two handlers, reversible on any day, no schema, no wire field. The binding condition is inherited
from `ADR-0055` and `ADR-0074` — **no deployment may expose these endpoints without this** — and it
is sharper on the attach path than at either earlier budget. Until a transport is configured,
`ADR-0077`'s `NoRecoveryMailer` sends nothing, so §2's relay exposure begins on the day `EPIC-07`
configures a sender rather than the day these routes merge. **The budget must be in place before the
sender is.** If a story boundary and that ordering ever disagree, the ordering wins.

## Decision

### 1. `POST /api/auth/forgot-password` admits ten attempts per remote address per rolling sixty seconds

| Field | Config key | Environment | Default |
| --- | --- | --- | --- |
| `forgotPasswordMaxAttempts` | `auth.forgotPasswordMaxAttempts` | `AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS` | `10` |
| `forgotPasswordWindowMillis` | `auth.forgotPasswordWindowMillis` | `AUTH_FORGOT_PASSWORD_WINDOW_MILLIS` | `60000` |

with a `forgotPasswordLimits(): AttemptLimits` beside `signUpLimits()` and `signInLimits()`, in
`ServerConfig`'s existing pattern and precedence.

Why ten, and why sixty seconds:

- **Its defensive value is modest, so it is set where it cannot bite.** Per §Context it adds nothing
  against the attack the fifteen-minute rule already covers, and what it does add — an aggregate cap
  across distinct victims — is bought back by an attacker with a second source address. A number
  chosen to look prudent would buy nothing and cost the one thing this endpoint must not cost.
- **Ten per sixty seconds is this repository's pair for an aggregating key on a repeated action**
  (`ADR-0022` §2, `ADR-0074` §1), *"deliberately generous against human typo rates."* Forgetting a
  password is something a group behind one address plausibly does together — an office returning
  after a holiday — and unlike attaching an address it costs a player nothing to reach.
- **The window is the recovery time**, and it is the number doing the work: sixty seconds is short
  enough that §4 cannot compound into a lockout nobody can see, and short enough that a player who
  presses the button again succeeds before they give up on the product.

### 2. `POST /api/auth/recovery-email` admits five attempts per remote address per rolling sixty seconds

| Field | Config key | Environment | Default |
| --- | --- | --- | --- |
| `recoveryEmailMaxAttempts` | `auth.recoveryEmailMaxAttempts` | `AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS` | `5` |
| `recoveryEmailWindowMillis` | `auth.recoveryEmailWindowMillis` | `AUTH_RECOVERY_EMAIL_WINDOW_MILLIS` | `60000` |

with a `recoveryEmailLimits(): AttemptLimits`, and a **separate `AttemptBudget` instance** per
endpoint in `serverComponents` — four instances in total over the same type. `ADR-0074` §1's reason
applies verbatim and needs no new argument: one shared instance lets either endpoint spend the
other's budget, which is a coupling nothing wants and no test would notice.

Why five rather than ten, when the key is the same and the window is the same:

- **This is the only cap on the mail it causes**, and the recipient is attacker-chosen. Five per
  minute is 300 mails an hour from one source instead of 600 — a halving of the one bound that
  exists, bought with headroom nothing legitimate uses.
- **Attaching an address is a once-per-account setup act.** The realistic repeat is a mistyped
  address: the player is told `202`, nothing arrives, they re-read what they typed and try again.
  Three attempts in a minute is a determined person; five is beyond one.
- **It must not be a cheaper door to the current-password guess than sign-in is.** Sign-in permits
  ten *wrong* passwords a minute from one address (`ADR-0074` §1, successes refunded). Five attempts
  a minute here, with no refund, permits at most five, so the front door stays the cheaper one by a
  factor of two. **If `signInMaxAttempts` ever moves, this number moves with it and
  `forgotPasswordMaxAttempts` does not** — they are equal today by coincidence of the same window,
  not by a shared reason.
- **`ADR-0074` rejected five-per-sixty-seconds for sign-in and that objection is answered, not
  ignored.** Its ground was that *"five is a per-person number applied to a group"* — an aggregating
  key should be at least as generous as a per-player one. It holds at sign-in, which is what a whole
  café does on arrival. It does not hold here: a group collides only if several people behind one
  address set recovery up within the same sixty seconds. That happens in a classroom demo, and it
  costs them a minute. Recorded as a cost rather than argued away.

### 3. Where each check sits, and neither endpoint refunds

Both call sites use `AttemptBudget.admit` and nothing else. `ADR-0074` §2's `refund` is **not**
called by either, and that is a decision rather than an omission: refunding exists so that a
*frequent* action whose common outcome on a shared address is success does not spend a shared
budget. Attaching an address happens about once per account, and `forgot-password` hashes nothing and
has nothing to reserve against. Refunding would buy a legitimate player almost nothing while adding
two more call sites to a method whose omission is a silent defect no compiler catches.

**`POST /api/auth/recovery-email`** — `TASK-041625`'s fixed order gains one step, between 3 and 4:

1. Resolve identity. Unresolved ⇒ `401`, before the body is read. **No budget.**
2. Decode. Failure ⇒ `400`. **No budget.**
3. `emailAddressOrNull` (`ADR-0078`) ⇒ `400` on `null`. **No budget.**
4. **`recoveryEmailBudget.admit(call.request.origin.remoteAddress)`. Not admitted ⇒ `202`, before
   the hash, before any row is written and before anything is sent.**
5. `credentials.verifyCurrent` ⇒ `403` on `false`.
6. `claimPending`, then `202`, then the send.

Three properties, each chosen:

- **The check is before the Argon2 verify**, which is `ADR-0074` §2's placement and its reason: a
  check after the hash bounds no pool and limits nothing, because every attempt has already been paid
  for by the time the budget is consulted.
- **The check is after the `401`.** A request with no identity costs no hash, so by `ADR-0055` §1's
  rule it costs no budget — and the stronger reason is that budgeting before identity would let
  *unauthenticated* traffic exhaust the budget of a signed-in player behind the same address, which
  is a strictly larger surface for no gain.
- **The budget therefore meters traffic, not failure.** A successful attach spends one of the five.
  That is deliberate: successes are exactly what causes mail, and capping mail is this budget's first
  job.

**`POST /api/auth/forgot-password`** — `TASK-041626`'s order gains one step, and it is the only
budget in this system consulted **after** the response is written:

1. Decode. Failure ⇒ `202` per `TASK-041626`, and nothing further happens. **No budget.**
2. **Respond `202`**, unchanged: §5 requires the answer before any mail work, and `TASK-041626` calls
   that ordering the timing defence rather than an optimisation.
3. **`forgotPasswordBudget.admit(call.request.origin.remoteAddress)`. Not admitted ⇒ return: no
   lookup, no `issue`, no send.**
4. `verifiedOwnerOf`, `issue`, and — only if `issue` returned `true` — the send.

The budget sits after the response **because** `admit` takes a `Mutex`. Consulting it first would put
a contended lock on the response path of the one endpoint whose whole design is that its latency
must not vary with what the server found, and it would violate §5's ordering as `TASK-041626` states
it. Nothing is lost: the answer never depends on the budget, so there is nothing for the check to
inform.

### 4. An over-budget attempt still counts against its own window, on both

`admit` records unconditionally, over budget or not — `ADR-0055` §1's rule, `ADR-0022` §2's rule,
`ADR-0074` §3's rule, and the warning in `AttemptBudget`'s own KDoc against "simplifying" it away.
**One rule for every limiter in this system** — `ADR-0022`'s specified join budget, sign-up's,
sign-in's and both of these — **and no caller-specific behaviour on a shared type.**

Here it is also the rule that works, and that is worth stating rather than inheriting. Over budget
answers `202`, so an attacker gets no feedback and has no reason to slow down. Counting means a
sprayer's first five or ten requests are all they ever get while they keep hammering; not counting
would hand the same sprayer five or ten *every minute, for ever*. On the attach path — where this
budget is the only bound on outbound mail — that is the difference between five mails and 300 an
hour. **The rule that reads as kinder is the one that gives a mail sprayer sustained throughput.**

The consequence, stated where it cannot be missed: **an exhausted address is clear sixty seconds
after its last *attempt*, not sixty seconds after its last *refusal*.** A client retrying in a loop
would hold its own address out indefinitely. Nothing in this repository retries an HTTP call in a
loop — the one automatic retry is `reconnecting.ts`'s socket reconnect, which posts to no endpoint —
and neither of these endpoints has a client screen yet. **If either ever grows one that retries on a
timer, this clause is what changes with it**, and the screen's ticket is where that must be checked.

### 5. The key is `ADR-0031` §5's, and no part of it is this ADR's to choose

`call.request.origin.remoteAddress`, alone, on both endpoints. §5 fixes it; `ADR-0055` §4 already
argued it and recorded the NAT caveat; `ADR-0074` §1 reused it. **Until `EPIC-07` installs the
forwarded-header plugin, neither handler may read `X-Forwarded-For` or `X-Forwarded-Host`** — reading
a client-supplied header without it lets the caller choose their own budget key, which is a limiter
that looks green in every test and stops nothing. This paragraph is a transcription so that
`TASK-041628` need not go looking; nothing in it is new.

### 6. What is observable from outside, including the part that is

`forgot-password` discloses **nothing**: the `202` is written before the budget is consulted, so an
over-budget request and an admitted one are identical in status, body, headers and latency.

`recovery-email` is different and the difference is accepted rather than overlooked. A caller who
submits a password they know to be wrong gets `403` when within budget and `202` when over it, so
**the over-budget state is detectable to a caller holding a session** — and knowing it helps a
guesser, who can otherwise burn guesses into a void. It is accepted on `ADR-0055` §3's test: the only
fact disclosed is a property of the caller's own address; it cannot be observed without being spent;
and the guess rate it permits is five a minute paced or unpaced, half of what the front door already
gives. Closing it would mean answering `403` over budget, which §5 forbids and which would in any
case tell a legitimate player their own correct password was wrong.

Neither endpoint ever answers `429`. `ADR-0055` §3's `429` was argued from sign-up's refusals being
*deliberately informative*; both of these are deliberately not, and a status that distinguished a
throttled request would be the oracle the whole endpoint pair is built to avoid. No `Retry-After`, no
new header, no protocol version movement, and `docs/protocol.md` gains nothing.

## Consequences

**What it buys.** `TASK-041628` is unblocked with four numbers, two placements and a counting rule it
does not have to invent, and the placement question — the half of `DEC-073` that is not a number and
that would otherwise have been settled inside a handler nobody reviews as a decision — is answered in
the open, differently for each endpoint and for reasons written down. The Argon2 pool gains its third
bound. The attach path gains the only cap it has on outbound mail. All four `AttemptBudget` instances
now share one rule about hammering — as does `ADR-0022`'s specified join budget — and three of the
four share one window, with sign-up's fifteen minutes the single departure and its reason already in
`ADR-0055` §1. And §Context leaves behind a
fact that was not written anywhere before: the fifteen-minute rule covers one of the two mail paths.

**What it costs.**

- **An attacker can switch off password recovery for everybody behind one address, silently, for as
  long as they care to pay ten requests a minute — and every player it hits is told, in the product's
  own words, that mail is on its way.** With §4 the address is clear sixty seconds after the last
  *attempt*, so a script that never stops holds it open for ever. There is no status, no client
  state, no support channel, and `ADR-0031`'s Consequences make no recovery a total, permanent loss
  of the account. This is the price of an address key on an endpoint whose refusal is invisible, and
  it is chosen rather than overlooked: the only key that would bound the harm to the attacker is the
  *submitted address*, which hands a stranger a switch that turns off one named player's recovery
  from anywhere.
- **Five a minute on the attach path is a per-person number on an aggregating key**, which is the
  objection `ADR-0074` used to reject the same count at sign-in. Several people behind one address
  setting recovery up in the same minute — a classroom, a demo, a launch event — will find that the
  sixth is answered `202` and receives nothing. They wait a minute. Accepted, and it is the direct
  cost of §2's first reason.
- **`202` on the attach path now means one more thing.** `ADR-0078` already recorded that a player
  can believe recovery is on when it is not; an over-budget attach is a second route to exactly that
  belief, and the profile's `hasRecoveryEmail` staying `false` is the only thing that contradicts it.
- **300 verification mails an hour from one address is still a relay, just a slower one.** This
  budget does not make the product safe to point at a real transport on its own, and nobody should
  read it as doing so.
- **The residual that follows from that, named so it cannot evaporate: the attach path has no
  per-account resend suppression.** §5's sentence reads naturally as covering both paths and the
  split built it on one, so on the best reading this is a defect against `TASK-041607`,
  `TASK-041608` and `TASK-041625` rather than a new question — `email_verification.issued_at` already
  exists, and the mechanism and the number are §5's own. It needs `claimPending` to answer whether it
  wrote, and the handler to send only then; the endpoint still answers `202` either way, so no
  response, no DTO and no test in `TASK-041625` moves. **A ticket for the planner, due before
  `EPIC-07` configures a sender** — the same condition that binds this ADR. It is deliberately not a
  `DEC`: there is nothing in tension to decide.
- **`AttemptBudget`'s per-key list still grows with an attacker's rate times the window.** Sixty
  seconds bounds it; it does not remove it, and every `admit` on a hammered key scans that list under
  the instance's `Mutex`. Inherited from `ADR-0055` §2, explicitly out of `TASK-041628`'s scope, and
  now true on two unauthenticated-or-cheap endpoints instead of one.
- **An operator now holds four limiters, eight numbers and two windows.** Raising a count for a NAT
  means knowing which of the four is biting, and the four are indistinguishable from outside on three
  of the endpoints. Nothing in this system reports that a budget refused anything.
- **Both endpoints ship unbudgeted until `TASK-041628` merges**, and no test anywhere can fail for a
  missing rate limit. The only thing between that state and §Context is that no sender is configured.

**What it forecloses.** Very little by construction — all four numbers are environment variables, and
an operator whose players share one address raises them without a deploy, though only after
discovering a problem whose only signal is a player who quietly leaves. What it does foreclose is the
comfortable reading of §5 that the two endpoints are symmetric: this ADR writes down that they are
not, and any later work that budgets, caps or tests them as one thing is wrong. It also declines,
for now, to make `AttemptBudget`'s counting rule configurable — the fourth caller was the last
cheap moment to fork that type, and §4 spends it on keeping one rule.

## Alternatives considered

**One pair for both — ten per sixty seconds, `ADR-0074`'s numbers unchanged.** The strongest case is
strong: one pair for an operator to hold rather than two, a fourth limiter nobody has to think about,
and it is precisely the pair this repository has already chosen twice for an aggregating key on a
repeated action. Five versus ten is a factor of two on a scale where `ADR-0074` demonstrated that
factors of thirty do not decide anything, and the extra headroom lands on a shared address where the
collateral is invisible. Rejected because the two endpoints are not one endpoint: only one has a
durable per-account cap behind it, and only one mails an address the caller typed. Ten on the attach
path doubles the rate of the **only** outbound-mail bound in the design in exchange for headroom that
a once-per-account setup action does not use.

**Sign-up's pair — five per fifteen minutes — on both.** Its case is real: tighter on every axis,
and `forgot-password` resembles sign-up in *frequency* far more than it resembles sign-in, since a
player forgets a password rarely. A fifteen-minute window would also make the mail cap mean
something, at 20 an hour instead of 600. Rejected on the interaction `ADR-0074` §Context isolated:
fifteen minutes with §4's *still counts* is what turns a burst into an indefinite lockout, and here
the lockout is silent and lands on a player who has already lost their password and is being told
that mail is coming. It would also multiply the hammered-key list length by fifteen on an
unauthenticated endpoint.

**Do not count over-budget attempts, and buy a longer window with the room that frees.** The
strongest case, stated properly: recovery becomes predictable and independent of retries; a
legitimate player behind a NAT can never make things worse by pressing the button again; the per-key
list is bounded by `maxAttempts` regardless of the attacker's rate, which is a genuine property on an
unauthenticated endpoint whose refusal gives an attacker no reason to stop; and it would permit a
window long enough for the mail cap in §2 to bind meaningfully. Rejected on three counts. It forks a
shared type's semantics across four call sites — `ADR-0055` §1 and `ADR-0074` §3 both fix the
opposite, and a per-caller flag on a limiter is a thing that gets set wrong. It does not rescue the
player it is meant to rescue: an attacker pacing at exactly the limit consumes the whole budget every
window and the neighbour behind that address is refused just the same, so non-counting moves the
collateral rather than removing it. And here it makes the attacker's job strictly better, because
`202` gives a sprayer no reason to pace: counting caps a hammering sprayer at one window's worth
total, non-counting hands them a window's worth every window for ever. If the evidence ever says
otherwise this is still the cheaper thing to revisit than the placements in §3, since it is one
branch in one method.

**Key `forgot-password` by the submitted address instead of the remote address.** The sharpest case
in this list: it is the only key that bounds mail to one mailbox regardless of how many source
addresses an attacker holds, which is exactly the gap §Context says the remote-address key leaves —
and the fifteen-minute rule is itself the proof that a per-target bound is the effective instrument
on this endpoint. Rejected twice over. `ADR-0031` §5 fixes the key, so it is not this ADR's to move.
And if it were, an identifier key is `ADR-0027` §6's refused shape with worse consequences here: it
would be the first state in this product a stranger can put a *specific* player's account into, and
its effect is to switch off that player's password recovery from anywhere in the world. The per-target
bound it wants already exists on this endpoint, durably, in `PasswordResets.issue`.

**Budget `recovery-email` after the password check, metering mail rather than traffic** —
`ADR-0055` §1's *meter the spending* shape, which is the other precedent available. Its case: the
budget would then count exactly the thing it exists to bound, and a legitimate player who mistypes
their current password would never be charged for it. Rejected because it leaves the Argon2 verify
unbudgeted — the resource both earlier budgets exist to protect — and it makes this endpoint a
*cheaper* door to the current-password guess than sign-in's front door, which is the one property §2
will not give up. Metering traffic costs a legitimate player one of five on an action they perform
once.

**Reserve and refund on `recovery-email`, exactly as sign-in does.** `ADR-0074` §2's shape, applied
by analogy: a successful attach would spend nothing, a shared address would be charged only for
mistakes, and the reservation would still cap in-flight verifications per address. Rejected because
the property that justified the refund at sign-in is absent — that argument turned on a *frequent*
action whose common outcome on a shared address is success, and attaching an address happens about
once per account. A refund here buys a legitimate player almost nothing, adds a call site to a method
whose omission is a silent defect, and removes the half of the bound that caps mail, since it is
precisely the successes that send.

**Leave both unbudgeted and let `EPIC-07`'s edge throttle.** The case is `ADR-0022`'s own: volumetric
defence belongs at the edge, and an in-process limiter behind a proxy duplicates work in the layer
least able to see the traffic. Rejected because `ADR-0031` §5 already requires the budget, and
because the resource on the attach path is not volumetric at all: one request per second is
indistinguishable from ordinary traffic at any edge threshold that does not also break the WebSocket,
and it is 3 600 unsolicited mails an hour from this product's sender.

**Fix the attach path's missing per-account suppression here instead of choosing a number for it.**
Its case is the best in this list, because it is the mechanism that actually works: §5's own
fifteen-minute rule, applied to the sibling table, would cap verification mail per account across
every source address at once — which no address-keyed budget can do — and `email_verification`
already carries the `issued_at` it needs. Rejected as scope rather than as wrong. `TASK-041628` names
it out of scope, it changes a port's return type and a handler's branch rather than a config value,
and `DEC-073` asked for numbers. It is recorded in §Consequences with the condition that closes it,
and this ADR's numbers assume it never arrives — so if it does, `recoveryEmailMaxAttempts` becomes a
candidate for raising rather than a thing to leave alone.
