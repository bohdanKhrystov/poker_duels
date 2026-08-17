# ADR-0055 — Sign-up is budgeted by address, and over budget says so

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-048`
- **Raises:** `DEC-049` (what a throttled player is *told* — the product owner's)
- **Constrains:** `STORY-0405`, which builds this alongside `ADR-0027` §6's sign-in budget;
  `STORY-0404`, which is **unchanged**; `STORY-0412`'s account screens; `EPIC-07`'s deployment,
  which owns the forwarded-header configuration this depends on and must not ship without it
- **Extends:** `ADR-0048` §6's response table with a seventh row

## Context

`POST /api/auth/sign-up` is the first endpoint in this system that runs Argon2 on demand.
`ADR-0027` §1 bounds the cost with `Dispatchers.IO.limitedParallelism(4)` — live in
`Argon2Hasher.kt` as `ARGON2_MAX_PARALLEL` — so peak memory is roughly 4 × 19 MiB. At ~50–100 ms
per hash that pool sustains **on the order of 50 hashes a second for the whole server**, and every
sign-in needs the same four slots.

### What actually costs a hash, read from the code rather than assumed

`TASK-040408` fixes the handler's order, and the order is the security property: identity, decode,
fields, the `holdsCredential` guard, then `credentials.create`. Of `ADR-0048` §6's six outcomes,
**four cost no Argon2 at all** — `401`, `400`, `422`, and the `409` for a player who already holds a
credential, which `STORY-0404` deliberately made a port read *"so a refused sign-up costs no Argon2
slot"*. Two cost a hash, and both are inside `create`:

```kotlin
// PostgresCredentials.create
val secretHash = hasher.hash(secret)   // ← the hash happens here
try { insertCredential(...) }          // ← the collision is discovered here
catch (failure: SQLException) { if (... UNIQUE_VIOLATION ...) IdentifierTaken else throw failure }
```

The hash is computed **before** the insert discovers the identifier is taken. So the two hashing
outcomes are `201 Created` and the `409` for a taken handle — and they are not symmetrical:

- **Success is self-limiting.** After one successful sign-up the player holds a `password`
  credential, so every later request from that device stops at the `holdsCredential` guard with a
  `409` and no hash. One device id buys exactly one successful sign-up, ever.
- **The taken-handle `409` is not limited by anything.** It writes no row, so the player still holds
  nothing, so the next request reaches `create` and hashes again. One device id, one handle known to
  exist, and an unbounded stream of full Argon2 operations against the four slots that sign-in
  shares. Nothing accumulates server-side, so no other guard ever engages.

That is the attack, and it is narrower and worse than `DEC-048` guessed: not "a caller holding one
device id can saturate every slot" in general, but one specific branch on which a refusal is
*expensive by construction*.

### The forces

- **The pool bounds memory, not latency.** `limitedParallelism` queues; it does not reject. Under
  the attack above the server does not fall over — it gets arbitrarily slow, and a legitimate
  sign-in queued behind five hundred sign-ups waits about nine seconds. The existing bound is
  therefore the wrong instrument for this: it is already doing its job perfectly and the harm
  happens anyway.
- **`ADR-0027` §6 and `ADR-0031` §5 both make their limiters non-oracular by answering exactly as
  the ordinary refusal does**, and neither trick transfers. Sign-in has *one* refusal that
  deliberately conflates two states; `forgot-password` has *one* answer that deliberately conflates
  five. Sign-up has four refusals that are deliberately **informative** — `ADR-0048` §6 says so
  outright, *"the form must know which field to mark"* — and `ADR-0031` §5 already accepts the `409`
  as a public confirmation that a handle exists. There is no conflation here to protect, and each
  of the four is a lie if borrowed for a budget: `409` claims a handle is taken when it may be free,
  `422` claims a rule was broken that was not, `400` claims the body was malformed when it was
  perfect, and `401` claims the caller has no identity when it has one.
- **`401` is the disqualifying one.** A client's correct response to `401` is that it is not known —
  and `ADR-0027` §5 names the resulting behaviour as the decisive reason for the wire break: a
  client that concludes it has no device *mints a fresh one and abandons the profile it was
  holding*. That is the exact harm `ADR-0012` recorded and `EPIC-04` exists to repair. A budget
  answered with `401` can cost a player their profile.
- **Sign-up is a once-in-a-lifetime action, unlike everything else that has been budgeted here.**
  `ADR-0022` budgets joins and `ADR-0027` §6 budgets sign-ins; players do both repeatedly. A player
  reaches sign-up's hashing path once, or a small handful of times if they collide on handles. A
  budget that would be absurdly tight on sign-in is generous here.
- **Nothing in this repository rate-limits anything today.** `ADR-0022`'s `JoinLimits` was specified
  and never built; `grep` finds no budget, no window and no limiter in `poker-server`. So there is
  no shape to reuse in code — only in prose — and whoever builds the first one writes the type.
- **It is also the most conversion-critical endpoint in the product.** A refusal here is a player
  who never arrives, so the cost of a budget that bites wrongly is paid in players, not in support
  tickets.

### The deadline, honestly

Nothing here is free today and expensive later — an in-memory limiter with config values adds no
schema, no wire field and no migration, and can be added, tuned or removed on any day. What has a
deadline is the **exposure**: `EPIC-07` has hosted nothing, so between `STORY-0404` merging and a
first deployment the endpoint is reachable only from a developer's laptop. The real deadline is
therefore *before the server first listens on a network `EPIC-07` does not control*, not a story
boundary, and that is stated below as a condition rather than a preference.

## Decision

### 1. Sign-up carries a budget, and it meters the hash rather than the request

At most **5 requests per remote address per rolling 15 minutes reach `Credentials.create`.** The
budget counts a request when — and only when — it has passed identity, decoding, the field rules and
the `holdsCredential` guard, and is about to hash. A request refused with `401`, `400`, `422`, or the
guard's `409` costs no Argon2 and **consumes no budget**.

This is the one substantive departure from `ADR-0022` and `ADR-0027` §6, and it is a change of
predicate, not of mechanism: **those budgets meter failure, because they are defending a search
space; this one meters spending, because it is defending a rate.** A successful sign-up costs the
same 19 MiB as a colliding one, and a player only ever succeeds once, so counting successes costs a
legitimate player one of their five and costs an attacker the whole thing.

An **over-budget request still counts**, exactly as `ADR-0022` §2 has it, so hammering extends the
window rather than resetting it.

### 2. The mechanism is `ADR-0027` §6's, and it is written for the first time here

One type, in `duels.poker.server.auth`, which `STORY-0405` builds and calls from both sign-up and
sign-in:

```kotlin
public data class AttemptLimits(val maxAttempts: Int, val windowMillis: Long)

public class AttemptBudget(private val limits: AttemptLimits, private val clock: ServerClock) {
    /** Records an attempt against [key] and answers whether it is within budget. */
    public suspend fun admit(key: String): Boolean
}
```

- **Time comes from `ServerClock`**, whose production implementation reads `System.nanoTime()` and
  is therefore monotonic — an NTP step cannot widen a window or void one.
- **The numbers are config, in `ServerConfig`, with the pattern that file already establishes:**
  `auth.signUpMaxAttempts` / `AUTH_SIGN_UP_MAX_ATTEMPTS` (default `5`) and
  `auth.signUpWindowMillis` / `AUTH_SIGN_UP_WINDOW_MILLIS` (default `900000`). This is what makes a
  wrong number cheap: an operator whose users are behind one NAT raises it with an environment
  variable, not a deploy.
- **State is in memory, and a `Mutex` is held across the whole read-prune-record.** A
  check-then-record with any gap between the two admits *N* concurrent requests against a budget of
  one, and concurrency **is** the attack, so the critical section is not an optimisation detail.
- **Expired entries are swept by `ADR-0025`'s existing ticker**, not by a second coroutine, in
  addition to the pruning each `admit` does to the key it touches.
- **`ADR-0022`'s failed-join budget stays where that ADR put it** — inside `RoomRegistry`, keyed by
  player, counting failures. If it is ever built on this type, that is a later and separate change.

### 3. Over budget answers `429 Too Many Requests`, with an empty body — and yes, it discloses something

`ADR-0048` §6's table gains a seventh row:

| Outcome | Answer |
| --- | --- |
| **Over budget for this remote address** | **`429`, empty body, nothing written, no hash computed** |

The other six rows are untouched, and the ordering in §4 is what keeps them that way.

**The oracle question, answered rather than deferred.** A `429` here discloses exactly one fact:
*more than five requests have recently reached the hashing path from this remote address.* It names
no handle, no player, no password and no account. Three things make that acceptable, and they are
given in increasing order of strength:

1. **It is strictly weaker than a refusal this endpoint already gives away by design.** `ADR-0031`
   §5 accepts sign-up's `409` as a public confirmation that a specific handle exists, and
   `ADR-0048` §6 accepts `400`/`422` as public statements about the caller's own input. A status
   that says "this address has been busy" cannot be a worse oracle than the one two rows above it
   that names a handle.
2. **It cannot be read without being spent.** The budget check sits last, so the only request that
   can *observe* a `429` is one that would otherwise have consumed a slot. An attacker sharing a
   NAT with a target who polls to discover whether the target signed up has burned their own five
   doing it, and cannot separate their own consumption from anyone else's. The observation destroys
   the thing it observes.
3. **Pacing under this budget is total defeat, not evasion — which is why `ADR-0027` §6's rule does
   not transfer.** A distinguishable `429` does let an attacker measure the window and sit just
   underneath it. On sign-in that would matter: a budget on *guessing* is defeated by patience,
   because the search space is still being consumed, only slowly. Here there is nothing to find. An
   attacker pacing perfectly at 5 per 15 minutes contributes 0.006 hashes a second against a pool
   that sustains fifty — which is the outcome we wanted. **Hiding a budget is worth something only
   when knowing it helps; here knowing it is compliance.**

**And the alternative is worse in a way that is concrete.** Borrowing an ordinary refusal means
lying to a legitimate player about their own input at the single moment they are trying to join.
`422` sends them to change a perfectly good password — possibly to a worse one — and the second
attempt fails identically. `409` makes them believe three free handles are taken, and they will not
try those again. `401` risks the profile abandonment `ADR-0027` §5 documents. **We are refusing to
spend a player's password, handle or profile to conceal that an address was busy.**

**No `Retry-After`, and no body.** Every other answer this endpoint gives is an empty body, and
`ADR-0048` §6 records that *"no endpoint anywhere in this system returns one."* A `Retry-After`
would hand an attacker the exact window they would otherwise spend a full window measuring, and buys
a human clicking a form button nothing that waiting does not. It is additive on any later day, which
is why it is the half of this that is left undone.

### 4. Keyed by remote address alone

`call.request.origin.remoteAddress`, the same key and the same caveat as `ADR-0027` §6: behind a
proxy this needs the forwarded-header configuration `EPIC-07` owns, and **until that plugin is
installed the budget must not read `X-Forwarded-For`.** Reading a client-supplied header without it
lets the caller choose their own budget key, which is a limiter that looks green in every test and
stops nothing.

What each candidate gives an attacker, stated rather than implied:

- **Device id, or the resolved player.** Exact — it never punishes a neighbour. Rejected because a
  device id is one WebSocket handshake, so minting *N* of them buys *N* whole budgets, and
  `ADR-0022` already recorded this weakness (*"minted devices dilute the budget"*) for a resource
  that was a 40-bit guess space. Against four slots of 19 MiB, dilution is not a residual, it is a
  bypass.
- **The pair, both required to be under budget.** Its case is that the two keys fail in opposite
  directions. Rejected because a conjunction is strictly *more* restrictive than either key alone,
  so it does nothing for the NAT case it appears to help, and the address key alone already kills
  the attack — a second key that stops nothing extra is a second thing to get wrong.
- **Remote address.** Chosen. Immune to minting: an attacker who mints a thousand device ids still
  has one address.

**Behind a NAT, in both directions.** An attacker on a shared address can deny sign-up to everyone
sharing it for fifteen minutes, at a cost of five requests. That is real, cheap and targeted, and it
is accepted: it is bounded by a window rather than a lockout, it cannot touch any account a player
already holds — sign-in is budgeted separately and sign-up is on no path to an existing account — and
the affected player retries and succeeds. Conversely, a legitimate shared address is affected only
when six different people sign up within one quarter of an hour, because each of them reaches this
path once. An attacker with many addresses gets 5 × addresses per window and needs on the order of
ten thousand of them to saturate the pool; that is a volumetric attack, and it is the deployment
front-end's problem for exactly the reason `ADR-0022` gave.

### 5. Nothing new protects the Argon2 pool itself, and that is a decision

No second pool, no bounded queue, no rejection at the dispatcher. The two bounds a memory-hard hash
needs are already both present once §1 lands: **`ARGON2_MAX_PARALLEL` bounds memory, and the
endpoint budget bounds the arrival rate that feeds the queue.** A third bound at the pool would cap
a queue the second bound already keeps short.

The two rejected shapes and why:

- **A separate pool for sign-up**, so it cannot starve sign-in. It doubles peak Argon2 memory to
  8 × 19 MiB against `ADR-0027` §1's *"against a small host"*, or halves sign-in's throughput
  permanently if the existing four are split 2 + 2 — a permanent cost against a case the endpoint
  budget makes unreachable.
- **A bounded queue rejecting at depth *K***. It needs a counter, a rejection path, and a `503` that
  would be a second new status decided with none of §3's reasoning behind it — and it can only fire
  in a state §1 prevents at any plausible arrival rate. **This is deferred on a measurement, not
  rejected on an argument**: the trigger is an observed queue depth under real load, and until
  `EPIC-07` runs something there is nothing to observe.

### 6. `STORY-0405` builds it, and `STORY-0404` is unchanged

None of `STORY-0404`'s fourteen tickets gains this. The planner's assessment is **confirmed**:
`DEC-048` blocked nothing, and this ADR blocks nothing in that story either. Three of its tickets
scope rate limiting out by name — `TASK-040408`, `TASK-040411` and `TASK-040414` — and every one of
those exclusions stands verbatim under this decision. The chain is linear and `AuthRoutes.kt`,
`AuthRouteTest.kt` and `ServerConfig.kt` are each already contended; inserting a limiter into it
would reopen merged reasoning for no gain.

`STORY-0405` builds `AttemptBudget`, `AttemptLimits`, the two config values, the call site in
`AuthRoutes`, and `ADR-0027` §6's sign-in budget on the same type. `ADR-0048` §6's seventh row and
`docs/protocol.md`'s sign-up entry are updated in that story, not this one.

**The due date is corrected from a story boundary to a condition.** *"Before `STORY-0405` merges"*
is the convenient boundary; the binding one is that **no deployment may expose `/api/auth/sign-up`
without this**, because until `EPIC-07` hosts something the endpoint is not reachable by anybody and
after it does the endpoint is reachable by everybody. `STORY-0405` lands well before `EPIC-07`, so
the convenient boundary satisfies the binding one — but if the two ever disagree, the deployment
wins.

## Consequences

**What it buys.** The unbounded branch is closed at the only place it can be closed cheaply: a
request refused for budget costs one device-id lookup and one indexed `SELECT`, and never the 19 MiB
and 50–100 ms that made it worth sending. `STORY-0405` gets one type serving two endpoints, which is
the cheap outcome `EPIC-04` predicted. `ADR-0048` §6's six answers keep their exact meanings,
because the check sits after all of them — a `429` never masks a real refusal, and a player who
mistypes their password still gets the honest `422` even when their address is over budget. And the
project's first limiter finally exists in code rather than in three ADRs' prose.

**What it costs.**

- **A shared address is now a shared fate, and one person can spend it.** Five requests deny sign-up
  to a whole NAT for fifteen minutes — a classroom, an office, a carrier's CGNAT — and the refused
  player is a new player at the one moment they are most likely to leave and not come back. This is
  the real price and it is paid by the wrong people. It is mitigated only by the numbers being
  environment variables, which means an operator can raise them **after** discovering the problem,
  never before.
- **`429` is a genuine disclosure and this ADR ships it knowingly.** Somebody sharing an address
  learns that somebody else on it recently signed up. §3 argues that is worth less than the handle
  the `409` already gives away, but the argument is a judgement about relative value, not a proof,
  and it is the first place in this system where a limiter is deliberately visible. Every later
  endpoint will cite this row as precedent, including ones where the conflation *is* load-bearing —
  so the sentence that must be carried forward is not *"limiters answer `429`"* but *"a limiter may
  answer `429` when the endpoint's ordinary refusals are already informative."*
- **The budget's own state is attacker-influenced memory.** One map entry per distinct address that
  reached the hashing path in the last window; ten thousand addresses is a few hundred kilobytes,
  and getting there costs the attacker ten thousand device ids and fifty thousand hashes, so the map
  is never the binding constraint. It is deliberately given **no hard cap**, because both failure
  modes of one are worse than bounded growth: failing closed at the cap is the global lockout
  `ADR-0022` rejected by name, and failing open is a bypass an attacker reaches by filling it.
- **This bounds sign-up's contribution to the pool and nothing else's.** `ADR-0027` §6's sign-in
  budget counts *failed* sign-ins, so a stream of **successful** sign-ins still hashes on every
  request, unmetered, on the same four slots. That is `ADR-0027`'s to revisit if it ever matters,
  and it is recorded here so the next reader does not mistake this ADR for a bound on total Argon2
  load.
- **Metering the hashing path rather than the request means the four cheap refusals are free
  forever.** A client with a bug that sends ten thousand malformed bodies a second is refused by
  nothing in this design — it costs a JSON parse each, which is the right call today and is the
  thing that will need revisiting first if it is ever wrong.
- **`STORY-0404` merges an endpoint with no limiter on it**, and the only thing standing between
  that state and the attack in §Context is that nothing is deployed. If `EPIC-07` is brought forward
  the ordering breaks silently, because no test anywhere can fail for a missing rate limit.

**What it forecloses.** Sign-up can no longer be the endpoint that answers uniformly, so the
"one status, always" discipline `ADR-0031` §5 built for `forgot-password` is now something this
system does on some endpoints and not others, and each future one must argue its own case. And
fixing the NAT collateral later cannot be done by loosening the key — the alternatives in §4 are
all *more* restrictive or bypassable — so the only lever that will ever exist is the two numbers.

## Alternatives considered

**No budget at all, and rely on `EPIC-07`'s front end.** Its strongest case: `ADR-0022` already
assigned volumetric defence to the deployment front-end, per-IP throttling at the edge is a
solved and configured thing, and an application-level limiter behind a proxy that has one is
duplicated work in the layer least able to see the traffic. Rejected because the resource being
protected is not bandwidth or connections — it is four in-process slots that no edge proxy can
observe, and the attack rate that saturates them is about fifty requests a second, which is
indistinguishable from ordinary traffic at the edge and would sail through any threshold set for a
volumetric attack. An edge limiter tuned tightly enough to catch this would break the WebSocket.

**Fix the branch instead of the endpoint: check the identifier before hashing.** Its strongest case
is that it is precise and needs no limiter, no window, no state and no new status — `create` could
`SELECT` for `(kind, identifier)` first and answer `IdentifierTaken` for a hash's price of nothing,
which removes the *unbounded* half of the problem entirely and is maybe six lines. It is genuinely
attractive. Rejected on three counts: the read-then-insert is a TOCTOU that still needs the unique
violation caught, so it adds a path rather than replacing one; it converts a taken-handle refusal
into a **timing** oracle for handle existence, cheaply enumerable, which is a strictly worse trade
than the `409` `ADR-0031` §5 already reasoned about accepting; and it leaves the *success* path
unmetered, so an attacker minting device ids still buys one full hash each. It fixes the branch this
ADR noticed and not the resource this ADR is about. Nothing here forbids doing it later as an
optimisation, with its own timing analysis.

**Answer `422`, borrowing the password refusal.** Its strongest case: it is the only one of the four
that keeps the limiter perfectly non-oracular *and* directs the player at a field they can act on,
so the form does not need a new state and `STORY-0412` gains nothing to design. Rejected because the
action it directs them to is wrong — a player changes a good password for another good password,
fails identically, and the most likely thing they change it to is something simpler.

**Answer `409`, borrowing the handle collision.** Its strongest case: a `409` already means *try
something else*, which is very nearly correct advice, and the retry it prompts is the retry we want.
Rejected because it teaches the player that specific free handles are taken. They will not try those
again, so a limiter that is supposed to cost fifteen minutes permanently costs them the name they
came for.

**Answer `204`, or `201`, and silently do nothing.** Its strongest case is the `forgot-password`
precedent taken to its conclusion: an attacker cannot detect a limiter that answers success.
Rejected outright — it tells a player they have an account when they do not, and they will spend the
next sign-in attempt, and possibly a password reset, discovering otherwise. `ADR-0031` §5's `202`
works because `202` means *accepted for processing* and asserts no outcome; `201` asserts a row
exists.

**Key by device id or resolved player, or by the pair.** Covered in §4 with their strongest cases:
exactness for the first, opposite failure directions for the second. Rejected on minting and on
conjunction respectively.

**A second Argon2 pool, or a bounded queue at the dispatcher.** Covered in §5. The first is
rejected on memory or throughput; the second is deferred on a measurement rather than rejected, and
§5 names its trigger.

**Build it in `STORY-0404`.** Its strongest case is that the endpoint is otherwise merged unlimited,
and an unlimited endpoint that exists is a thing somebody can deploy — which §Consequences records
as a real residual. Rejected because there is nothing to deploy it onto until `EPIC-07`, the story
is split into a linear chain with three tickets contending three files, and the limiter needs
`ADR-0027` §6's sign-in call site to be worth extracting a type for at all. Building it in
`STORY-0405` costs one story of exposure against nothing; building it now costs a re-split.
