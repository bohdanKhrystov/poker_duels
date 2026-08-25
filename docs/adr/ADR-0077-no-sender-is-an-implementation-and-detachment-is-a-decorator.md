# ADR-0077 — No sender is an implementation, detachment is a decorator, and a test binds neither

- **Status:** Accepted
- **Date:** 2026-08-25
- **Resolves:** `DEC-072` — the shape of the mail seam, and its failure, lifetime and observability
  semantics
- **Builds on:** [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §5 (the `202` before
  any mail work, delivery detached), §6.2 (a port with exactly two `Unit`-returning members), §6.3
  (the address is in no log line), §6.4 (a delivery log carries no address) and §7 (the transport is
  `EPIC-07`'s, and a build with no sender is a valid state);
  [`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md), whose rule that a server-owned
  coroutine's lifetime *is* the application's is applied here unchanged
- **Constrains:** `TASK-041625`, `TASK-041626` and `TASK-041627`, and every transport `EPIC-07`
  writes behind `RecoveryMailer`
- **Chooses no transport.** No relay, no provider API, no SDK, no dependency and no bill. The one
  part of this question whose answer genuinely depends on which transport is chosen — the retry
  policy — is identified below and left to `EPIC-07` rather than guessed
- **Raises:** `DEC-075`, a collision between `ADR-0031` §4's link and
  [`ADR-0076`](ADR-0076-a-screen-the-player-chose-has-an-address.md) §4, found while answering this
  and blocking nothing today

## Context

`ADR-0031` settled what mail this system may send and to whom. It left the seam that sends it in two
sentences that do not, between them, describe a working shape.

§5: *"The response is written **before** any mail work, and delivery runs on a detached coroutine."*
§7: *"A build with no sender configured is a valid state in development and tests."*

Both are right and neither says what the wiring holds, what the coroutine belongs to, what happens
when a send fails after the row is committed, or what a test can see. What is in tension:

**The absence assertions are the hard ones, and they decide the shape.** Four criteria across three
blocked tickets assert that **no** mail was sent: an address already verified to somebody else must
send nothing (`TASK-041625`), the unknown and pending cases must send nothing (`TASK-041626`), a
build with no sender must send nothing (`TASK-041627`). Presence can always be awaited — a channel
receive returns the moment a mail arrives. Absence cannot be awaited at all. Proving that a detached
coroutine did *not* do something means waiting long enough to be convinced, which is a sleep, which
is a flake, and which gets slower and less convincing every time CI is loaded. So the seam has to be
able to **decline to detach**, or the story's central security property is untestable.

**The port cannot report an outcome, by construction.** §6.2 fixes two members returning `Unit`, and
`TASK-041606` ships a test that fails the build if a third member appears or a return type changes.
A caller therefore *cannot* learn whether a send worked. Every question about failure has to be
answered on the far side of the port, because there is no near side to answer it on.

**The row is committed before the send can fail, and §5 then enforces silence.** `PasswordResets.issue`
writes `password_reset` with `issued_at = now` and returns `true`; the `202` is already on the wire;
the send happens after both. If it fails, the row still says a mail went out fifteen minutes ago
(`TASK-041613`), so the player's second attempt is a deliberate no-op. A lost mail is not merely
lost — it buys a quarter hour of silence the player cannot distinguish from anything else.

**Retry is the one part of this that depends on a transport nobody has chosen.** Whether a failure is
permanent or transient is a property of the protocol carrying it: a hard bounce and a relay hiccup
are the same exception until something knows the transport. `ADR-0031` §7 assigns the transport to
`EPIC-07`. A retry policy written now is a policy written blind.

**A transport's own exception is the likeliest place an address will ever appear in a log.** §6.3
puts the address in no log line and §6.4 warns that a delivery log carrying addresses is *"a mailing
list with a different file extension"*. `EmailAddress.toString()` is already a fixed redaction, which
protects this repository's own string templates — and protects nothing at all against a third-party
exception that built its message from a raw `String` it was handed.

**Two things this repository has already settled make the answer cheap.** `ADR-0025` put a
server-owned coroutine on the application's scope and said why: *"stopping the application cancels
the scope this coroutine is a child of, which is the only way this loop ever ends."* And every HTTP
route test here installs its route directly with doubles — `application { module(); authRoutes(reads,
credentials, …) }` — rather than booting `duelServer`. That idiom is what lets a test choose what the
route is holding.

### The deadline, honestly

None of this is urgent by the clock. `TASK-041625` is twenty-four tickets deep in a linear chain and
nothing sends mail until `EPIC-07` exists. It is urgent by consequence: three tickets are blocked on
it, and the observability clause decides whether four of their criteria can be written at all.

One part is cheaper now than later: **every link this system will ever mail is built in one
function**. Written that way today it costs nothing; retrofitted after a transport composes its own
strings, it is a hunt through mail templates for the one that concatenated a header.

## Decision

The route holds one `RecoveryMailer`, calls it, and branches on nothing. Everything `DEC-072` asks
about is a property of what the wiring put behind that reference.

### 1. No sender configured is an object, never a null

**`NoRecoveryMailer` is a `public object` in `duels.poker.server.mail` implementing `RecoveryMailer`
with two empty bodies. It is what the wiring binds when no transport is configured, which is every
developer machine and every CI run.**

No call site is nullable, no route reads a configuration flag, and no endpoint can behave differently
because a sender is absent — which is the property `TASK-041627` exists to make true, and it is a
property of the *type*, not of three handlers each remembering the same `if`.

### 2. Detachment is a decorator over the same port

**`DetachedRecoveryMailer(delegate: RecoveryMailer, scope: CoroutineScope, log: Logger)` is a
`public class` in `duels.poker.server.mail` implementing `RecoveryMailer`. Each of its two members
launches the delegate's corresponding call into `scope` and returns.** The route calls
`mailer.sendVerification(address, token)` — one ordinary suspend call — and there is no
`CoroutineScope`, no `launch` and no `Job` in any route file.

The detachment being a decorator rather than a `launch` in the handler is what makes §5's ordering
enforceable without making the send invisible: the same code, written once, applied by the wiring
that is allowed to decide not to apply it (§7).

The two compose, outermost first: `DetachedRecoveryMailer(transport)` in a configured deployment,
`DetachedRecoveryMailer(NoRecoveryMailer)` with no sender. `TASK-041606`'s shape test is untouched —
it asserts over the interface, and implementations were never its subject.

### 3. The delivery scope is a supervisor child of the application's job

**`Application.duelServer` builds the delivery scope and the decorator, beside the call to
`scheduleSweeps` that `ADR-0025` put there:**

```kotlin
val delivery = CoroutineScope(
    coroutineContext + SupervisorJob(coroutineContext.job) + CoroutineName("recovery-mail"),
)
```

- **A child of the application's job**, so stopping the server cancels every in-flight send.
  `ADR-0025`'s argument transfers whole: structured concurrency is the lifecycle, nothing keeps a
  `Job`, nothing shuts it down explicitly, and there is no `GlobalScope` anywhere.
- **A supervisor**, so one failed send cancels no sibling and never reaches the application's job.
- **Its own scope, not the application's**, because the application's job also carries the sweep
  ticker, which never completes. A scope that cannot be quiesced is a scope nothing can reason
  about.

**The server may exit with mail pending, and pending mail is cancelled rather than drained.** A
`forgot-password` that arrives in the last moment before a shutdown mints its row, answers `202`, and
never mails. No drain window is configured, because a drain timeout is a number about a transport's
latency and no transport exists to measure; adding one later is one `withTimeoutOrNull(join)` in a
shutdown hook.

### 4. A failed send is logged once, by class name, and nothing else happens

**Each launched block catches every `Throwable` except `CancellationException`, which always
rethrows** — the shape `sweepPass` already uses. The line carries the member name that failed and
`failure::class.simpleName`, and **nothing else**:

- **No address**, per §6.3.
- **No exception message and no stack trace.** A transport's message is the likeliest place a
  recipient address will ever appear in this system's logs, and §6.3 admits no exception. The class
  name is a compile-time constant of the transport and cannot carry one.
- **No `player_id`.** §6.4 permits one; the port deliberately does not carry one (§6.2 fixes both
  signatures), and widening the one interface built to be un-widenable in order to decorate a log
  line is not a trade this decision makes. §6.4's ceiling is not a floor.
- **No success line.** A line per delivered mail is a delivery log, and the thing §6.4 is warning
  about is a delivery log.

A transport is free to log more at its own boundary, where it is the only thing that knows which of
its exceptions are address-free.

### 5. Nothing above the port is retried, and no row is compensated

**A failed send changes nothing. There is no retry in `poker-server`, no back-off, no queue, and no
deletion of the token row that was committed before it.** The player's recovery path is to ask
again — immediately for a verification (`claimPending` replaces the pending row in one transaction,
`TASK-041608`), and after fifteen minutes for a reset (§5's suppression window, `TASK-041613`).

Two reasons, and the second is the one that would still hold if a transport existed:

1. **Retry policy is transport-shaped, and the transport is `EPIC-07`'s** (§7). Permanent and
   transient failures are indistinguishable without knowing the protocol. A transport may retry
   internally, behind the port, invisible to every route and every test here — that stays open and is
   the right place for it.
2. **A compensating delete would destroy live links.** A relay that accepts a message and then times
   out its acknowledgement raises the same exception as one that never delivered, so deleting the row
   on failure deletes tokens for mails that arrived. §5 refused to invalidate an outstanding token so
   that *"a double-click does not destroy the link the player is about to use"*; a failure-triggered
   delete is that same destruction on a less predictable trigger.

### 6. `baseUrl` is a `ServerConfig` field, and one function builds every link

**`ServerConfig` gains `val baseUrl: String`, with `BASE_URL_KEY = "server.baseUrl"`,
`BASE_URL_ENV = "BASE_URL"` and `DEFAULT_BASE_URL = "http://localhost:5173"`** — the Vite dev origin
this client is served from, since the link addresses a *client* screen and not this server's port.
The field is defaulted, so the several tests that construct `ServerConfig(…)` field by field are
untouched.

- **Absent is the default. Present but malformed is a startup error**, thrown from `ServerConfig.from`
  exactly as a non-integer port is: a value must be an absolute `http` or `https` origin with no
  trailing slash. Every other field in this class already refuses a value it cannot parse, and a
  wrong origin mails links nobody can click.
- **A defaulted `baseUrl` is harmless in the state it exists for**, because a build with no sender
  builds no link at all. The pairing rule — *a configured sender requires a configured `baseUrl`* — is
  a check about the **sender**, and it belongs beside the sender's startup log line and health check,
  which `ADR-0031`'s Consequences already assigned to `EPIC-07`.
- **`RecoveryLinks(baseUrl)` in `duels.poker.server.mail` is the only place either URL is
  constructed**, with `verification(token)` and `reset(token)`. `reset` returns
  `"$baseUrl/reset#token=$token"` exactly as §4 fixes it; `verification` returns
  `"$baseUrl/verify#token=$token"`, §4's shape applied to the mail §4 did not spell out — fragment,
  never a query string, for §4's reasons unchanged.
- **Nothing under `poker-server/src/main` reads `Host` or `X-Forwarded-Host`**, per §4, and that is
  asserted over the source tree rather than inferred.

Because both links exist in one function, `DEC-075` below — if it lands — is a one-function change.

### 7. What a test can await: the test binds an undecorated double

**Every assertion about a mail is written against a route installed directly, holding a recording
`RecoveryMailer` and no decorator. The send is then an ordinary suspend call inside the handler, and
it has finished by the time the test client's call returns.**

```kotlin
val mailer = RecordingRecoveryMailer()
application { module(); recoveryRoutes(/* … */, mailer) }

val response = client.post("/api/auth/forgot-password") { setBody("""{"address":"bob@x.test"}""") }

assertEquals(HttpStatusCode.Accepted, response.status)
assertEquals(1, mailer.sent.size)                                  // presence
assertTrue(mailer.sent.single().link.startsWith("$baseUrl/reset#token="))
```

and, in the same file and with no waiting of any kind:

```kotlin
assertEquals(emptyList<SentMail>(), mailer.sent)                   // absence, decidable
```

The mechanism is Ktor 3.0.3's test engine, and it was **measured rather than assumed**: a handler
that responds, then suspends in `delay(50)`, then appends to a list, has appended by the time
`client.get` returns. Detached work does not — a `launch` into an external scope is still running
when the call returns, though the child job is already registered, so `job.children.forEach { it.join() }`
after the call is race-free. Both were probed against this repository before this ADR was written.

Three rules follow, and they are the whole answer to *what a test can await*:

- **No test asserts about a mail through `duelServer`.** `duelServer` composes the decorator, and a
  test booted that way would have to join a scope it does not hold. `TASK-041627`'s
  `theServerStartsWithNoSenderConfigured` boots `duelServer` and asserts nothing about a send, which
  is correct and stays that way.
- **The decorator is tested on its own, with no HTTP**, since nothing else exercises it: that
  `sendVerification` returns before a delegate that suspends has finished; that after joining the
  scope the delegate has run; that a delegate which throws leaves the scope alive, the caller
  unaffected, and a second send still delivered. All three are deterministic by the join above.
- **The `202`-before-the-send ordering is not gated, and cannot be.** Under the test engine the
  response is not observable until the handler returns, so moving `call.respond` to the end of the
  handler reddens nothing — which `TASK-041626`'s Proof step 3 already predicts. It stays a review
  criterion: the ordering is visible in one function and a reviewer reads it. This repository has
  named an untestable criterion before rather than manufacture a latency assertion that would flake.

### 8. Where it lands

- `duels.poker.server.mail`: `NoRecoveryMailer.kt`, `DetachedRecoveryMailer.kt`, `RecoveryLinks.kt` —
  one public type per file, named for it, because ktlint's filename rule leaves no other option.
- `duels.poker.server.auth`: `RecoveryMailer` only, unchanged, per §7.
- `ServerConfig` gains one field; `ServerComponents` carries the **undecorated** mailer;
  `Application.duelServer` builds the scope, applies the decorator, and passes the result to
  `recoveryRoutes`.
- **`poker-engine` learns nothing.** No mail, address, token, scope or configuration type exists in
  it or crosses into it, and its dependency allowlist does not move.
- **No new dependency, no `PROTOCOL_VERSION` change, no migration, no wire change.**

## Consequences

**What it buys.** `TASK-041625`, `TASK-041626` and `TASK-041627` unblock with an answer for every
clause they were blocked on. *A mail was sent, with this link* and *no mail was sent* are both
ordinary list assertions with no join, no channel and no timeout — the seam earns its keep on the
absence half, which nothing else made decidable. No route branches on configuration, so the no-sender
state runs the same code as a configured one. `EPIC-07` adds a transport by writing one class and
changing one binding, and can add retry inside it without touching a route or a test.

**What it costs.**

- **A lost reset mail costs the player fifteen minutes of silence, and they cannot tell why.** The row
  is committed with `issued_at = now` before the send can fail, §5 suppresses a second mail inside
  that window, and §5 answers `202` to the retry as it answers `202` to everything. So the player
  asks again, is told the same nothing, and waits. This is the direct price of choosing no retry and
  no compensating delete, and it is the sharpest edge of this decision. The verification path does not
  pay it — a second claim replaces the first immediately.
- **The failure log names a class and nothing else.** No player, no message, no stack trace. An
  operator watching a broken relay sees `sendPasswordReset failed: SendFailedException` and must
  reproduce it outside the server to learn anything more. That is a real operational loss, accepted to
  keep §6.3 absolute rather than "absolute except in the one place an address is most likely to be".
- **The server can exit with mail pending**, and a deploy timed badly against a player's reset request
  costs that player the fifteen minutes above.
- **§5's ordering — the `202` before any mail work — is guarded by a reviewer and by nothing else.**
  It is the timing defence the whole endpoint exists to provide, and no test in this story fails when
  it is broken.
- **Nothing in `STORY-0416` exercises the real composition.** The decorator is unit-tested, the routes
  are tested undecorated, and the two are put together only by `duelServer`, which asserts nothing
  about a send. The first thing that will ever run a detached send against a real transport is
  `EPIC-07`.
- **Three new files whose combined behaviour is to do nothing.** `duels.poker.server.mail` ships an
  object that sends no mail, a decorator that forwards mail nobody sends, and a builder for links
  nobody delivers. Reading the package tells a newcomer almost nothing about how mail is sent, because
  nothing here sends any.
- **A configured sender with a defaulted `baseUrl` mails dead links**, and nothing in this decision
  catches it. That check joins the startup log line and health check `ADR-0031` already made
  `EPIC-07`'s.
- **`TASK-041627` is now bigger than its Files table.** Three implementation files, `ServerConfig`,
  `ServerComponents`, `Application.kt` and its tests exceed what one ticket carries; that is the
  planner's to re-cut, and this ADR names it rather than leaving it to be discovered mid-ticket.

**What it forecloses.**

- **A caller ever learning whether a mail was sent**, permanently and by construction. Any future
  feature needing a delivery receipt needs an ADR superseding §6.2's `Unit` returns.
- **Retry, back-off or a queue above the port.** Not below it: a transport may do all three.
- **A drain-on-shutdown window**, until a transport exists whose latency can be measured.
- It does **not** foreclose a second sender, a per-environment transport, or a mailer that logs
  richly at its own boundary. All three are implementations of the same interface.

**What this does not settle.**

- **Which transport, and therefore any bill** — `ADR-0031` §7's deferral to `EPIC-07`, untouched.
  Nothing above is different under a relay than under an HTTP API, with the single exception named in
  §5: the retry policy, which is left to `EPIC-07` precisely because it is the part that would have
  required guessing one.
- **`DEC-073` and `DEC-074`**, both still open and neither touched. The budgets sit in front of these
  endpoints and change nothing about the seam; over budget answers `202` and sends nothing, which is
  already the unknown-address path.
- **`DEC-075`, raised here.** `ADR-0031` §4 mails `<baseUrl>/reset#token=…` — a **path segment** — and
  `ADR-0076` §4 has since made the opposite call for every client address on the express ground that
  *"a fragment never 404s… no host, proxy or rewrite rule has an opinion about it"*, which is the same
  call `room-link.ts` made for the room link. A recovery link is the one URL in this product whose
  failure is unrecoverable, and it is currently specified in the form that depends on a rewrite rule
  from a host nobody has chosen. This ADR **transcribes §4 rather than reinterpreting it**, and
  registers the conflict: *does the mailed link survive a static host with no rewrite rule, and if it
  becomes a fragment route, what is the slug?* The slug is `STORY-0417`'s under `ADR-0076` §1, which
  keeps screen vocabulary with the story that names the screen. It blocks nothing today — no sender is
  configured, so no link is delivered to anybody — and §6 makes it a one-function change whenever it
  is answered.

## Alternatives considered

**A nullable `RecoveryMailer?`, with each call site branching on it.** The strongest case is honesty:
the no-sender state becomes *visible*, you cannot read a handler without seeing that mail is optional,
and no reader can mistake a silent no-op for a send. It also needs no new file. Rejected because
`TASK-041627`'s property is that **no route branches on whether a sender is configured**, and a
nullable port makes that branch compulsory at every call site — three of them today, each of which has
to get it right, and each of which reads like this endpoint sometimes behaves differently. §5's entire
subject is that it does not. An object that does nothing puts the branch in the wiring, once, where
the decision actually is.

**The route launches the coroutine itself.** The strongest case is locality, and it is a good one: the
detachment sits in the same function as the `202` it must come after, so §5's ordering is legible in
one place and there is no decorator to forget to apply. Rejected because it puts a `CoroutineScope`
into three route handlers and makes the send unobservable in exactly the tests that must observe it —
a test binding a recording double would still be racing a `launch` it does not control, and the
absence assertions would have nothing to assert against. The decorator is the same code written once,
somewhere the wiring can decline to apply it.

**Keep the decorator everywhere and let tests await it** — a channel receive for presence, and
`scope.children.joinAll()` for quiescence. The strongest case is that it tests the real composition,
decorator included, and it works: joining after the client call returns is race-free, and that was
measured, not assumed. Rejected on **absence**. No await proves a negative; the only implementation of
*nothing was sent* against a live scope is a timeout, which is a sleep dressed as an assertion, and
four criteria in this story need exactly that negative. A seam that can decline to detach turns all
four into a list comparison.

**`GlobalScope`, or a delivery scope owned by `ServerComponents`.** The strongest case is that
`serverComponents(…)` already builds every other collaborator, so the mailer would arrive complete and
`Application.kt` would not be touched at all. Rejected because nothing would then cancel it.
`ADR-0025` settled that a server-owned coroutine's lifetime is the application's, on an argument that
transfers without modification, and a delivery scope outside the application's job is a coroutine no
shutdown ends and no test can bound.

**Retry with back-off — three attempts, exponential, then give up.** The strongest case is the cost
named above: mail transports fail transiently more often than permanently, and the player's
alternative is fifteen minutes of nothing. Rejected because the policy is a property of a transport
nobody has chosen: how many attempts, over what window, and above all whether *this* failure is a hard
bounce or a hiccup, which is unanswerable without knowing the protocol. `ADR-0031` §7 assigns that to
`EPIC-07`, and this is the one clause of `DEC-072` that could not be answered provider-independently —
so it is left rather than guessed, and it stays available in the only place that can judge it.

**Delete the `password_reset` row when a send fails**, so the player can ask again at once. The
strongest case is that it erases this decision's sharpest cost and is about four lines. Rejected
because *the send failed* is not reliably knowable: a relay that accepts a message and then times out
its acknowledgement throws what a relay that never delivered throws, so this deletes live tokens for
mail that arrived. §5 deliberately does not invalidate an outstanding token, *"so a double-click does
not destroy the link the player is about to use"* — a failure-triggered delete is the same destruction
on a worse trigger.

**Carry the finished link across the port instead of the token**, so the route builds the URL and a
route test can assert it. The strongest case is real: it moves §4's *never from a header* property to
where a route test can prove it behaviourally, instead of a source-level assertion. Rejected because
§6.2 fixes both signatures around a token, and a port whose members take an address and a string is
one rename from `send(to, subject, body)` — the exact shape §6.2 exists to make impossible. The
property is kept structurally instead: one builder, and nothing in `main` reads a header.

**`baseUrl` with no default, so the server refuses to start without one.** The strongest case is that
it converts the failure that actually matters — a sender configured against a wrong or missing
origin — from a mailbox of dead links into a loud boot failure a deployment fixes in a minute.
Rejected because it makes the value mandatory in precisely the state where nothing reads it: with no
sender configured no link is built, and every developer machine, every CI run and the several tests
that construct `ServerConfig(…)` field by field would have to supply an origin that is never used. The
pairing rule is a fact about the sender, and it belongs with the sender's own startup checks in
`EPIC-07`. A present-but-malformed value still refuses to start, which is where the real risk is.
