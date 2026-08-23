# ADR-0074 — Sign-in is ten wrong passwords a minute, reserved before the hash and refunded when right

- **Status:** Accepted
- **Date:** 2026-08-24
- **Resolves:** `DEC-069` — the two numbers of the sign-in attempt budget, and whether an
  over-budget attempt still counts against its own window
- **Builds on:** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §6 (a rolling window,
  keyed by remote address, metering *failed* sign-ins, answering exactly as a wrong secret does —
  all four of those stand untouched here),
  [`ADR-0055`](ADR-0055-sign-up-is-budgeted-by-address-and-over-budget-says-so.md) §§1, 2 and 4 (the
  `AttemptBudget` type, the config pattern, the address key and its NAT caveat),
  [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) §2 (this repository's only precedent for
  budgeting a *repeated* action: ten refusals per rolling sixty seconds),
  [`ADR-0048`](ADR-0048-a-password-has-one-rule-and-it-is-length.md) (what the budget is defending,
  and what it cannot defend),
  [`ADR-0056`](ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §1 (sign-in renders
  no throttled state, and a shared error mapper must not manufacture one)
- **Extends:** `ADR-0055` §2's `AttemptBudget` by **one method** — `refund`. Nothing else about that
  type changes, and sign-up does not call it
- **Constrains:** `TASK-040519` (the type is born with the method), `TASK-040520` (the config pair
  becomes two pairs), `TASK-040523` (the call site, unblocked, five files with `atomic:`);
  `EPIC-07`'s deployment, which owns the forwarded-header configuration this key depends on and
  must not ship without it
- **Leaves open:** a **per-account** guessing budget, which `ADR-0027` §6 refused on purpose and
  `ADR-0048` recorded as the residual of that refusal. Not created here, not closed here

## Context

`ADR-0027` §6 settled everything about this budget except its size. The mechanism is a rolling
window with state in memory and time from `ServerClock`; the key is the remote address, chosen
*because* an identifier-keyed budget lets an attacker lock a victim out; the answer over budget is
byte-identical to a wrong password, so the limiter is not itself an oracle. `ADR-0055` §2 then wrote
the type. What is left is two numbers and one semantic — and the planner that raised `DEC-069` was
right that they are not tuning. Both of its reasons survive checking, and a third force turned up
that neither register mentions.

### What the budget is actually defending, computed rather than asserted

Two resources, and they are not the same resource.

**The four Argon2 slots.** `ADR-0027` §1 holds verification to
`Dispatchers.IO.limitedParallelism(4)`; `ADR-0055`'s context measures the result at *"on the order
of 50 hashes a second for the whole server"*. Sign-in hashes on **every** attempt, including
attempts on identifiers that do not exist —
`ADR-0027` §6's dummy hash is what removes the enumeration oracle, and it removes it by spending a
slot. So on this endpoint there is no equivalent of sign-up's four free refusals: an attacker who
knows nothing at all still costs a full verification per request. Unbudgeted, one address saturates
the pool and a legitimate sign-in queues behind everything sent before it.

**The guess space.** `ADR-0048` accepts `password`, `12345678` and a password equal to the player's
own handle, and says in as many words that what stands between a guesser and such an account is
*"Argon2id itself"* plus this budget. That makes the numbers load-bearing — but not in the way the
phrase suggests, and the arithmetic is worth doing rather than gesturing at:

| Budget | Guesses/hour from one address | Top-1 000 list | A 10-million corpus |
| --- | --- | --- | --- |
| None (pool-bound, ~50/s) | ~180 000 | ~20 seconds | ~2.3 days |
| 5 per 15 minutes (sign-up's pair) | 20 | ~2 days | ~57 years |
| 10 per 60 seconds | 600 | ~1.7 hours | ~1.9 years |

Read the table honestly and it says something uncomfortable: **no number in the plausible range
saves an account whose password is in the first ten guesses, and every number in the plausible range
puts a ten-million-entry corpus years out of reach of one address.** The two candidates differ by
30× in one band — roughly the top thousand — where one is hours and the other is days, and where an
attacker who cares uses a second address instead and gets a fresh budget either way, because
`ADR-0027` §6's key is the address. So the defence is close to *insensitive* to the choice within
this range, while the collateral is not. That asymmetry, not the size of the guess space, is what
decides the numbers.

### Sign-in's shared address is not sign-up's

`ADR-0055` §4 accepted a fifteen-minute shared-address lockout on the ground that a player reaches
sign-up's hashing path *once in their life*, so six people signing up in a quarter of an hour from
one address is the whole exposure. Sign-in is reached whenever a player arrives without a session or
a device credential — a new browser, a cleared store, a second machine — from a café, an office, or
a carrier's CGNAT, where an address can stand for hundreds of people. And the person refused is not
a prospective player who can come back tomorrow; it is somebody who already has an account, a coin
count and a ladder place, being told their password is wrong when it is not. The reasoning does not
transfer, and the register was right to say so.

### The force neither register mentions: the predicate fights its own placement

`ADR-0027` §6 meters **failures**. To protect the pool the check must sit **before** the hash. Those
two requirements cannot both be met by one call to `ADR-0055` §2's `admit`, which records and
answers in the same atomic step:

- Call `admit` before the hash and it meters **traffic** — a successful sign-in spends budget, which
  is not what §6 says and is exactly the wrong thing to charge a shared address for.
- Split it into a non-recording peek before the hash and a `record` after a failed verification, and
  the atomicity `ADR-0055` §2 was explicit about is gone: *N* concurrent requests from one address
  all peek `true`, all queue for Argon2, and all hash. One address gets an unbounded burst per
  window. That is the attack the budget exists to stop, arriving through the obvious implementation.
- Call `admit` only when a failure is about to be answered and the limiter limits nothing at all:
  every attempt has already paid for its hash by the time the budget is consulted.

This is the genuinely technical half of `DEC-069`, it is not a number, and it has to be settled here
because the type is written days before the call site is.

### The window and the semantic are one question, not two

`ADR-0022` §2 and `ADR-0055` §1 both have an over-budget attempt still counting, so hammering
extends the window rather than resetting it. What that rule *costs* depends entirely on the window,
and the interaction is where a wrong pair becomes indefensible:

- At **fifteen minutes**, an exhausted address recovers a quarter of an hour after its last
  attempt — and every retry by anyone behind it pushes that quarter of an hour out again. A room
  full of people pressing *Sign in* keeps their own address locked indefinitely. A burst becomes a
  lockout.
- At **sixty seconds**, the same rule is a nudge: the address is clear a minute after the last
  attempt, and a human retrying three times adds seconds.

So the honest way to keep one rule across all three limiters in this system is to buy it with the
window, not to argue it away.

### The deadline, honestly

Nothing here is free today and impossible later. Both numbers are environment variables; the
`refund` method is four lines on a type that **does not exist yet** — `TASK-040519` is unstarted —
which is precisely why the shape is decided now and not after the type is merged and called from two
places. The binding condition is `ADR-0055`'s and it is inherited verbatim: **no deployment may
expose `POST /api/auth/sign-in` without this**, because until `EPIC-07` hosts something the endpoint
is reachable from a laptop and afterwards it is reachable by everybody. If a story boundary and that
condition ever disagree, the deployment wins.

## Decision

### 1. Ten failed sign-ins per remote address per rolling sixty seconds

`POST /api/auth/sign-in` admits at most **10 attempts per remote address per rolling 60 000
milliseconds**, and a successful sign-in consumes none of them.

Two config values in `ADR-0055` §2's pattern, beside sign-up's:

| Field | Config key | Environment | Default |
| --- | --- | --- | --- |
| `signInMaxAttempts` | `auth.signInMaxAttempts` | `AUTH_SIGN_IN_MAX_ATTEMPTS` | `10` |
| `signInWindowMillis` | `auth.signInWindowMillis` | `AUTH_SIGN_IN_WINDOW_MILLIS` | `60000` |

with a `signInLimits(): AttemptLimits` beside `signUpLimits()`, and a **second `AttemptBudget`
instance** in `serverComponents` over the same type and its own limits. One shared instance would
let sign-ups spend sign-in's budget and the reverse, which is a coupling nothing wants and no test
would notice.

Why this pair and not another:

- **Ten per sixty seconds is `ADR-0022` §2's pair**, chosen there for the same shape of thing — a
  repeated action, budgeted against a guessing attack, *"deliberately generous against human typo
  rates"*. Two limiters carrying the same numbers is one fact for an operator to hold rather than
  two, and the third (sign-up's 5 / 15 min) then differs for a reason that is written down.
- **The count is a NAT's headroom.** An address is refused only after an **eleventh** wrong password
  inside one minute. Successes cost nothing, and sign-in is not what a player does on every visit —
  `ADR-0027` §2's session and `ADR-0037`'s device credential are — so ordinary shared traffic does
  not approach it.
- **The window is the recovery time, and it is the number doing the real work.** Sixty seconds is
  short enough that §3's *still counts* rule cannot compound into a lockout, and short enough that a
  player who is refused can succeed before they give up.
- **The count is also a concurrency cap** — see §2. Ten in-flight verifications from one address
  add about a quarter of a second to everybody else's queue against four slots; a hundred would add
  two and a half.
- **The rate is what an attacker gets, and 600/hour is the price of the collateral being small.**
  Per the table above, that is 30× sign-up's rate in a band where both answers are "too slow to
  matter" against a corpus and "too fast to matter" against `password`.

### 2. The budget is reserved before the hash and refunded when the password was right

`AttemptBudget` gains one method, and only sign-in calls it:

```kotlin
/**
 * Returns one recorded attempt for [key] to the budget, if it holds any.
 *
 * Sign-in reserves before it hashes and refunds when the password turned out to be right, so
 * that only wrong guesses accumulate over the window while the reservation still bounds how many
 * verifications one address can have in flight (ADR-0074 §2).
 */
public suspend fun refund(key: String)
```

It removes the most recently recorded attempt for the key under the same `Mutex` `admit` holds, and
does nothing when there is none.

The sign-in handler's order is then fixed, and the order is the security property exactly as
`TASK-040408` made it for sign-up:

1. Decode, and `ADR-0048` §2's 128-code-point maximum. **No budget, no hash** — a refusal that costs
   no Argon2 costs no budget, which is `ADR-0055` §1's rule and it carries over unchanged.
2. `signInBudget.admit(call.request.origin.remoteAddress)`. Over budget ⇒ answer as §4 says,
   **before the identifier is looked up and before anything is hashed**.
3. The credential lookup and the verification — including `ADR-0027` §6's dummy hash for an
   identifier that does not exist.
4. **On success only:** `signInBudget.refund(...)`, then issue the session.

What the reservation buys, beyond metering:

- **Concurrency cannot overspend.** `admit` is one atomic read-prune-record, so ten is ten however
  many requests arrive at once. The peek-then-record shape has no such property and its failure is
  silent.
- **It bounds in-flight verifications per address, including successful ones.** At most
  `signInMaxAttempts` requests from one address can be inside a hash at any instant, because the
  refund happens after the hash returns. `ADR-0055`'s consequences left *"a stream of successful
  sign-ins still hashes on every request, unmetered"* as an open residual owned by `ADR-0027`; this
  does not close it — a caller with valid credentials can still drive verifications at whatever rate
  the server completes them — but it does cap the **queue depth** one address can create, which is
  the half of that residual that hurt legitimate players.
- **`ADR-0027` §6's predicate survives intact.** Over the window, the recorded attempts are wrong
  guesses. A player who signs in successfully ten times in a row spends nothing.

### 3. An over-budget attempt still counts against its own window

`admit` records unconditionally, over budget or not, exactly as `ADR-0022` §2 and `ADR-0055` §1 both
have it. Hammering extends the window; it never resets it. **One rule for all three limiters in this
system, and no caller-specific behaviour on a shared type.**

The consequence, stated where it cannot be missed rather than left to be discovered: **an exhausted
address is clear sixty seconds after its last *attempt*, not sixty seconds after its last
*failure*.** A client that retried in a loop would hold itself out indefinitely. Nothing in this
repository retries an HTTP call in a loop — the one automatic retry is `reconnecting.ts`'s socket
reconnect, which posts to no endpoint — and `ADR-0056` §1 forbids the client manufacturing a
throttled state here, so sign-in stays a form a person presses. If that ever changes, this clause is
what changes with it.

### 4. Over budget answers exactly as a wrong password does, and nothing new goes on the wire

Restated because it is fixed elsewhere and a reader of this ADR must not have to go looking:
`ADR-0027` §6's answer stands. Same status, empty body, no header that differs, no `Retry-After`,
**no `429`** — the `429` `ADR-0055` §3 introduced is sign-up's, argued from the fact that sign-up's
ordinary refusals are already informative, and sign-in's deliberately are not. `ADR-0056` §1 already
says a client sharing one error mapper between the two forms must not render a throttled state on
this one. No protocol version moves, no frame changes, and `docs/protocol.md`'s sign-in entry gains
nothing, because from outside the server nothing is observable except that a password was refused.

A refusal at step 2 is *faster* than one at step 3, because it skips the lookup and the hash. That
is not an oracle by the test `ADR-0048` §2 and `ADR-0055` §3 both applied: the only fact it
discloses is a property of the caller's own address, and it cannot be observed without being
spent.

### 5. No account is ever locked, and this ADR does not change that

The key is the address. **There is no state anywhere in this design that a stranger can put a
specific account into.** An attacker who knows a handle cannot cause its owner a single refused
sign-in from anywhere but their own address, which is `ADR-0027` §6's reason for the key and is why
"account lockout" — the thing a player would have to be told about, and the thing that would need a
product decision about what they are told — does not exist in this product. What the budget can do
is deny the *address* — for sixty seconds after the last attempt, and for as long as an attacker
keeps paying ten requests a minute to hold it there. A player escapes that by changing networks, and
what they are shown while it lasts is `ADR-0056` §1's already-decided ordinary refusal.

Correspondingly, **nothing here defends one account against a distributed guesser**, and that is
`ADR-0048`'s recorded residual, not a new one: five thousand addresses trying `password` against one
handle get five thousand budgets. Closing it means a per-identifier budget, which `ADR-0027` §6
refused by name, and re-opening that refusal is an amendment to §6 with a lockout-notification
question attached — not this decision.

## Consequences

**What it buys.** `TASK-040523` is unblocked, with numbers it does not have to invent, and the two
questions that would otherwise have been answered in a handler nobody reviews as a decision — where
the check sits, and what a success costs — are answered in the open. One address can no longer queue
more than ten Argon2 verifications, which is the property that keeps a legitimate sign-in from
waiting seconds behind a stranger, and it holds for successful floods as well as failed ones. Over
the window the budget counts wrong guesses and nothing else, so a shared address is charged for its
neighbours' mistakes and never for their successes. All three limiters in the system now share one
rule about hammering, and the one that does not share a *number* (sign-up's 5 / 15 min) differs for
reasons that are written in `ADR-0055` §1 and re-checked here.

**What it costs.**

- **A player behind a busy or hostile address can be locked out of their own account, and is told
  their password is wrong.** Eleven wrong passwords from one address inside sixty seconds is all it
  takes, and an attacker who wants to cause it spends ten cheap requests a minute for as long as
  they care to. The refused player has no way to tell this apart from mistyping, by design
  (`ADR-0027` §6), and no client-side state says otherwise (`ADR-0056` §1). This is the price of an
  address key and it is chosen, not overlooked: the alternative key locks the *account* instead of
  the address, which is worse. What the window buys is that the harm is **sixty seconds past the
  last attempt** rather than a quarter of an hour, and what §3 costs is that a room full of people
  retrying keeps it alive.
- **Ten a minute is 600 guesses an hour from one address, forever.** Against `ADR-0048`'s accepted
  `password`, that is not a defence, and this ADR does not claim to be one. Anybody citing this
  budget as protection for a weak password is citing the wrong thing; it protects the *pool*, and it
  turns a ten-million-entry corpus into two years of uninterrupted work from any single address.
- **The shared type grows a method that one of its two callers must remember to call.** A sign-in
  path that reserves and forgets to refund silently becomes a traffic meter, which is a defect no
  compiler catches and only `aSuccessfulSignInSpendsNoBudget` can see. That test is now load-bearing
  rather than illustrative, and `TASK-040523` says so.
- **Between the reservation and the refund, a player's own concurrent sign-in sees the slot spent.**
  For the duration of one hash. Harmless, and named so that nobody later reads a transient count of
  eleven as a bug.
- **`signInMaxAttempts` now means two things** — guesses per window, and simultaneous verifications
  per address. An operator raising it for a NAT also raises the queue depth one address can create.
  That coupling is real and is the reason the field is not documented as "just a rate".
- **A stream of malformed or over-long requests is still free**, exactly as it is on sign-up: they
  cost a parse and no hash, and nothing in this design refuses them. Generic to every endpoint here,
  inherited, not created.
- **Sign-in ships unbudgeted until `TASK-040523` merges**, and no test anywhere can fail for a
  missing rate limit. The only thing between that state and the arithmetic in §Context is that
  `EPIC-07` has hosted nothing.

**What it forecloses.** Nothing about the numbers — both are environment variables, and an operator
whose players share one address raises them without a deploy, though only *after* discovering the
problem, and the discovery signal is a player who quietly leaves. What is foreclosed is the tidy
version of the type: `AttemptBudget` is now a reserve/refund pair rather than one call, and any
third caller has to decide which of the two shapes it is. And by keeping the address key, this
decision keeps the door shut on a per-account defence for as long as `ADR-0027` §6 stands.

## Alternatives considered

**Borrow sign-up's pair — five per fifteen minutes.** The strongest case is real and nearly won it:
one type, one pair of numbers, nothing to explain, nothing to remember, and it is strictly *tighter*
against a guesser — 20 guesses an hour against 600. Rejected on the interaction in §Context: fifteen
minutes with §3's *still counts* is the combination that turns a burst into an indefinite lockout,
and it does it on the endpoint where the person being locked out already owns an account. The
tightness it buys is 30× in a band where one answer is "days" and the other is "hours" and both are
useless to an attacker who wants a specific account today — while the collateral it costs is
fifteen-fold and lands on players.

**Five per sixty seconds.** Halves the guess rate, keeps the short window, and five wrong passwords
in a minute is more than any one person types. Rejected because the key is an *address*, not a
person: five is a per-person number applied to a group, and it halves a shared address's headroom to
buy a factor of two on a scale where factors of thirty do not matter. `ADR-0022` §2 picked ten for a
per-*player* key on the same reasoning about typo rates; an aggregating key should be at least as
generous, not less.

**A hundred per fifteen minutes — the same rate, a bigger bucket.** Its case is burst tolerance: a
classroom signing in at once never trips it, and the long-run rate is under sign-up's. Rejected
because the window is the recovery time, so a tripped address waits fifteen minutes; and because §2
makes the count a concurrency cap, so a hundred lets one address put a hundred verifications into a
four-slot pool and everybody else waits two and a half seconds.

**Peek before the hash, record after a failure** — an `isWithinBudget` alongside `admit`. The
strongest case: it meters failure exactly and literally, with no reservation to explain, no refund
to forget, and a per-key state bounded by the limit. It is what most people would write. Rejected
because it reintroduces the exact hole `ADR-0055` §2 held a `Mutex` to close: any number of
concurrent requests from one address peek `true` before the first of them records, so one address
buys an unbounded burst of Argon2 work once per window, and at a sixty-second window that is a
sustained saturation. The failure is invisible to every sequential test.

**Meter arrivals as sign-up does, and amend `ADR-0027` §6.** Genuinely attractive: zero new methods,
one shape for both endpoints, a *stronger* bound on the pool, and it makes `ADR-0055` §1's
"meter the spending" a single rule instead of a departure. Rejected because it charges a legitimate
player for succeeding. On a shared address the common event is a **successful** sign-in, so the
budget would be spent by exactly the traffic it exists to protect, and the number would have to be
sized for a whole address's ordinary volume rather than for its mistakes — a number nobody can
estimate before there are players. `ADR-0027` §6 chose the failure predicate and this ADR found no
evidence against it, only an implementation cost, which §2 pays.

**An over-budget attempt does not count.** Its strongest case: recovery becomes predictable and
independent of retries, a legitimate player behind a NAT can never make things worse by trying
again, and the per-key state is bounded by the limit rather than by the attacker's rate. Rejected on
three counts. It forks a shared type's semantics between its two callers, since `ADR-0055` §1 fixes
the opposite for sign-up, and a per-caller flag on a limiter is a thing that gets set wrong. It
hands a hammering client the full budget rate with no penalty at all, so the naive attacker is no
longer worse off than the patient one. And what it buys a legitimate player is bounded by the window
— at sixty seconds, under a minute. Keeping one rule and shortening the window gets most of the
benefit and none of the fork; if the evidence ever says otherwise, this is the cheaper thing to
revisit, because it is one branch in one method rather than a call-site contract.

**Key by identifier, or add a per-account budget beside this one.** The strongest case is the
sharpest one in this list: it is the *only* thing that would defend one account against the
distributed guesser `ADR-0048` names as the residual, and an address key demonstrably does not.
Rejected because `ADR-0027` §6 fixed the key precisely so that a stranger cannot lock a player out,
and reversing that is an amendment to §6, not an application of it. It would also create the first
state in this product that a stranger can put another player's account into — which is a
player-facing consequence, and therefore not a decision an architect should make inside a ticket
about two config values.

**Leave sign-in unbudgeted and let `EPIC-07`'s front end throttle.** Its case is `ADR-0022`'s own:
volumetric defence belongs at the edge, and an in-process limiter behind a proxy duplicates work in
the layer least able to see the traffic. Rejected for the reason `ADR-0055` gave and this endpoint
makes stronger: the resource is four in-process slots no edge proxy can observe, and the rate that
saturates them — about fifty requests a second — is indistinguishable from ordinary traffic at any
threshold that does not also break the WebSocket. `ADR-0027` §6 also already requires the budget;
this alternative is listed only because it is the one somebody proposes when the numbers get
argued about.
