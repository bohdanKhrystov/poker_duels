# ADR-0082 — A handle is read from a proven address, never from a player id

- **Status:** Accepted
- **Date:** 2026-08-26
- **Resolves:** `DEC-076` — where the login handle `RecoveryMailer.sendPasswordReset` requires comes
  from, given that nothing in this codebase can obtain one from a `PlayerId`
- **Registered and answered in the same PR**, because an implementation attempt raised it rather
  than a planner: a coder on `TASK-041626` found the gap and blocked instead of guessing — the same
  pattern as `DEC-064` and `DEC-065`
- **Upholds:** `Credentials.verifyCurrent`'s merged refusal to add a reverse lookup from player to
  identifier, and promotes it from a KDoc sentence to a build failure for the first time
- **Applies:** [`ADR-0031`](ADR-0031-an-optional-verified-recovery-email.md) §1 (the handle in the
  reset mail is the one exception to *the handle appears in no response body, ever*), §6.2 (the port
  has exactly two members and one of them takes a handle) and §5's *"a `409` is acceptable exactly
  when the caller already holds the secret it would otherwise disclose"*, applied to a lookup rather
  than to a status code
- **Constrains:** `TASK-041626`, and one acceptance criterion of `TASK-041630`
- **Leaves untouched:** `PasswordResets.issue`'s `Boolean` and every merged assertion on it;
  `RecoveryEmails.verifiedOwnerOf`; `RecoveryMailer`'s two signatures; `recoveryRoutes`' parameter
  list; `poker-engine`, which learns nothing here as it learned nothing from `ADR-0031`

## Context

`POST /api/auth/forgot-password` must send a password-reset mail.
`RecoveryMailer.sendPasswordReset(address, token, handle)` requires the player's login handle, and
**nothing in this codebase can obtain a handle from a `PlayerId`.** Every read that exists was
checked in source rather than remembered:

- `PasswordResets.issue(playerId, token): Boolean` — a token, and a yes/no.
- `RecoveryEmails.verifiedOwnerOf(address): PlayerId?` — an owner, and nothing about them.
- `Credentials` declares exactly `verify`, `verifyCurrent`, `create` and `holdsCredential`. There is
  no reverse lookup, and `verifyCurrent`'s own KDoc records that one was deliberately **not** added:
  *"looking one up just to feed `verify` would put a reverse lookup from player to identifier into
  the codebase for no other reason than this one check."*
- `PostgresPlayerDirectory` resolves device ids to players and nothing else.
- `sendPasswordReset` has **no production caller** in this repository. The only call anywhere is
  `NoRecoveryMailerTest`'s, which passes the literal `"handle"` to prove the no-op object does
  nothing. `TASK-041626` is the first real one, and no later ticket in `STORY-0416` fills the gap —
  `TASK-041630` passes `handle` through its decorator and never sources it.

The product half is already settled and is not reopened here. `ADR-0031` §1: *"The handle appears in
no response body and in no `ServerMessage`, ever… The one exception is the password-reset mail (§4),
which goes only to a proven mailbox."* §6.2 gives the reason: *"a player who forgot their handle
would otherwise be unrecoverable despite having opted in… it costs nothing, since anyone holding the
token already controls the account."* **The handle belongs in that mail.** What was open is the
mechanism, and only the mechanism.

What is in tension:

- **The obvious repair breaks a merged refusal.** `Credentials.handleOf(playerId): String?` is four
  lines and one `SELECT`. It is also precisely the reverse lookup `TASK-041615` refused, and once it
  exists nothing stops the next caller. That matters more here than it usually would, because a
  `PlayerId` is in scope almost everywhere on this server — every socket, every profile read, every
  leaderboard row — so a `PlayerId → handle` function makes the handle one call away from all of
  them, and `ProfileResponse` gaining a `handle` field becomes a one-line change that reads as
  helpful.
- **A merged ADR's central argument assumes the handle is not reachable from a player.** `ADR-0031`
  §1 rejected making the display name the handle on the express ground that a leaderboard would
  otherwise become *"a published list of half a credential"* — an argument that holds only while the
  handle is obtainable by the player typing it and by nothing else. `ADR-0067`'s *no id turns into a
  profile* is the neighbouring refusal in the same family: it is about display names rather than
  handles, so it does not decide this, but it is the standing evidence of how this repository has
  answered *may an id be turned into a fact about a person*.
- **Four competent engineers land in four places.** One adds it to `Credentials`. One has
  `PasswordResets.issue` return the handle alongside its boolean. One composes the mail at a layer
  that already holds it. One argues the mail should carry something else. That spread is what makes
  this a decision and not a ticket.
- **`issue`'s `Boolean` is merged and load-bearing.** `TASK-041613` shipped it and `ADR-0031` §5's
  fifteen-minute suppression rides on it: `false` means *nothing was written and the outstanding link
  survives*. Five implementations exist — `PostgresPasswordResets` plus four test doubles — and seven
  assertions in `PostgresPasswordResetsIssueTest` read the two values directly.

### The deadline

Two things are free today and expensive on any later day.

**`TASK-041626` is `ready` and is the first caller.** Deciding now costs one port member and one
statement. Deciding after it merges costs a merged route handler and a merged test file, rewritten.

**A `PlayerId → handle` function is free to refuse today and expensive to withdraw once anything
calls it.** There are zero callers, so the refusal costs nothing; the asymmetry is the whole reason
to answer now rather than to let the first implementation choose.

## Decision

### 1. The handle is read from a proven address, in one statement, and never from a player id

**`RecoveryEmails` gains one member, and it is keyed by an address:**

```kotlin
public suspend fun resetRecipientOf(address: EmailAddress): ResetRecipient?

public data class ResetRecipient(val playerId: PlayerId, val handle: String)
```

It answers everything `POST /api/auth/forgot-password` needs to send one mail — who to mint a token
for, and the handle to put in it — and it answers `null` otherwise. One statement, on the port that
already owns the address:

```sql
SELECT r.player_id, c.identifier
  FROM recovery_email r
  JOIN credential c
    ON c.player_id = r.player_id AND c.kind = 'password'
 WHERE lower(r.address COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu")
```

- **The `WHERE` clause is `SELECT_VERIFIED_OWNER_SQL`'s, character for character**, including the
  pinned `und-x-icu` collation and the reason its comment already gives: a fold applied outside SQL
  uses a different rule from the one that decided uniqueness. Two reads of one table must never
  disagree about which row an address names.
- **`c.kind = 'password'` is a SQL literal**, matching `REWRITE_CREDENTIAL_SQL` in
  `PostgresPasswordResets` verbatim, so the reset path spells the kind one way in both of the
  statements that make it up.
- **A `JOIN`, never a `LEFT JOIN`.** Three states answer `null` and are indistinguishable to the
  caller: an address nobody has mentioned, an address that is only pending (`ADR-0031` §3 — the two
  are the same state as far as the account is concerned), and a verified address whose owner holds no
  `password` credential. The third is unreachable under §3, which requires the current password to
  attach; it is answered rather than assumed, because a `LEFT JOIN` would hand the route a null
  handle to make a decision about.

**There is no `PlayerId` overload of this member and there must never be one.** That is the whole
mechanism, and it is the argument type rather than the member name that carries it: to obtain a
handle you must already hold a **verified recovery address**, which is the exact secret this endpoint
exists to refuse to disclose. `ADR-0031` §5 licensed `verify-email`'s `409` on the same test — *"a
`409` is acceptable exactly when the caller already holds the secret it would otherwise disclose"* —
and it applies here unchanged: a caller holding a player's proven address can already cause the mail
to be sent, and learning the handle adds nothing they could not read out of the mail itself.

### 2. `verifyCurrent`'s refusal is upheld, and gated for the first time

**No reverse lookup from player to identifier enters this codebase.** `TASK-041615`'s refusal stands
exactly as written, and this decision is on its side rather than around it: the direction it forbids
is `PlayerId →` identifier, and §1 adds nothing in that direction.

That refusal has until now been a KDoc sentence. It becomes a build failure:

**A test asserts that `Credentials` declares no member returning `String` or `String?`**, over
`KClass.declaredMemberFunctions`, in the same idiom as `RecoveryMailerShapeTest` and with a
positive-control bait as `PublicApiHasNoHashTest` uses. It is green today — `verify` returns
`PlayerId?`, `verifyCurrent` and `holdsCredential` return `Boolean`, `create` returns
`CreateCredentialResult` — and it reddens on exactly one thing: `handleOf(playerId): String?`, in the
one file where anybody would write it.

**What the gate does not catch, stated rather than discovered.** It is a tripwire, not a proof.
A handle read added to some *other* type passes it, and so does one wrapped in a value class —
Kotlin reflection reports a `@JvmInline` return type as the wrapper, not as `String`, so
`handleOf(playerId): LoginHandle` would sail through. Both remain review matters, and the sentence a
reviewer is applying is this one: **the only read in this system that produces a login handle takes a
proven `EmailAddress`.** The gate exists because the cheap, obvious violation is the `String` one.

### 3. `PasswordResets.issue` keeps its `Boolean`, and nothing merged is rewritten

**`issue(playerId, token): Boolean` is unchanged.** `ADR-0031` §5's two outcomes stay two outcomes,
`PostgresPasswordResetsIssueTest`'s seven assertions stay as written, and the four test doubles
implementing `PasswordResets` are not touched.

This was the closest call in the decision and it is recorded rather than assumed. `ClaimPendingResult`'s
KDoc says out loud that *"if the two ports are ever made symmetric, the cheap direction is to give
`issue` a sealed type, not to take this one away"* — a merged sentence pointing directly at
`issue` returning `Issued(handle)` / `Suppressed`, and the read would have come for free inside a
transaction that already holds the `player_id` and already writes `credential` in `consume`. It lost
on two counts. **It is a `PlayerId → handle` function with a side effect**: `issue(playerId,
newResetToken())` would answer the very question §2 refuses, so the invariant would survive only as
prose about not calling something. And **the credential-less player forces an unreachable third state
to be named** — `Issued` cannot carry a `String` there, so the type would grow a nullable handle or a
third case that no honest test exercises, while §1's `JOIN` answers `null` and needs no new state at
all.

### 4. What the route does, in order

```kotlin
call.respond(HttpStatusCode.Accepted)
val recipient = recoveryEmails.resetRecipientOf(address) ?: return@post
val token = tokens.newResetToken()
if (passwordResets.issue(recipient.playerId, token)) {
    mailer.sendPasswordReset(address, token, recipient.handle)
}
```

- **The `202` is still written first.** `TASK-041626`'s ordering is the timing defence and this
  decision does not move it: the extra join runs after the response, so it costs the caller nothing
  observable, and `ADR-0079`'s `admit` still sits after the `202` as well.
- **The read is not inside `issue`'s transaction, and does not need to be.** `ADR-0031` §1 —
  *"Nothing here changes a handle. No endpoint updates `credential.identifier`"* — was verified in
  source: the only `UPDATE credential` in this codebase is `REWRITE_CREDENTIAL_SQL`, which sets
  `secret_hash`. A handle read a microsecond before the token is minted cannot go stale. This is the
  precise difference from `ADR-0031` §5's suppression window, which *must* share a connection with
  its write because a pre-check on a separate connection is a read-then-write window.
- **A handle-less owner mints no token**, because the route returns before `issue`. That is the right
  order: a token that can never be mailed is a row that spends the player's fifteen-minute window and
  supersedes a link they may be holding.
- **Nothing about this is observable.** Every path answers `202` with an identical body, exactly as
  `ADR-0031` §5 requires and `TASK-041626` already asserts.

### 5. Where it lands

- **`RecoveryEmails.kt`** gains one member and one `data class`, beside `VerifyEmailResult` and
  `ClaimPendingResult`. Its class KDoc's *"no member returns a `String` that could be one"* is
  amended, not deleted, and the amendment makes it checkable rather than weaker: `ResetRecipient.handle`
  is a login handle, and `loginHandleOrNull` permits only `[a-z0-9._-]`, so it structurally cannot be
  an address. `verifiedOwnerOf`'s *"the only read that returns an address's owner"* becomes *"the only
  read that returns an owner and nothing else"* — it keeps its second caller, the attach path's
  already-proven-elsewhere check in `recoveryRoutes`, which must not receive a handle.
- **`PostgresRecoveryEmails`** gains the statement. **No migration and no index**: the join's access
  path — `credential` by `(player_id, kind)` — is the one `verifyCurrent` and `holdsCredential`
  already take, and `credential` has no `player_id` index today. The trigger for adding one is
  written down in Consequences.
- **`ThrowingRecoveryEmails`** in `VerificationSweepTest` gains the member. It is the only double of
  this port in the repository.
- **`recoveryRoutes`' parameter list does not move**, and neither do `Application.kt` or
  `ServerComponents.kt`. The port the route needs is already its first parameter.
- **`RecoveryMailer` is byte-unchanged.** `handle` stays a `String`, and the KDoc reason it gives —
  no `LoginHandle` type exists, `loginHandleOrNull` returns `String?` and `Credentials` takes
  `identifier: String` — is still true and is not disturbed for one call site.
- **`poker-engine` learns nothing.** No handle, address, credential or token type exists in it or
  crosses into it, and its dependency allowlist does not move.
- **No protocol version, no wire change, no `ServerMessage`, no response body.** The handle reaches
  exactly one destination: `RecoveryMailer.sendPasswordReset`.

## Consequences

**What it buys.** `TASK-041626` unblocks with a source for its third argument and one new statement
to write. `sendPasswordReset` gets its first caller and stops being a signature nobody could
satisfy. `ADR-0031` §1's *the handle is known because the player typed it* survives intact, so §1's
own argument against making the display name the handle — that a leaderboard would otherwise publish
half a credential — stays true rather than becoming retroactively false. And a refusal that lived in
one KDoc becomes a test.

**What it costs.**

- **A read now exists whose product is a login handle, and it did not exist yesterday.** That is the
  real price and it is not paid off by any of the fencing above. `RecoveryEmails` is injected in two
  production places — `recoveryRoutes` and `scheduleSweeps` — and each is one call from a handle **if
  it can produce a proven `EmailAddress`**, which today neither can and which nothing in the type
  system enforces. The fence is an argument type plus a KDoc, not an impossibility proof.
- **`RecoveryEmails` reads a third table.** Its charter was *"the two recovery tables `ADR-0031`
  builds"*, and it now joins `credential`. A port that reads the credential table is a port a future
  reader will be slower to trust, and one existing KDoc sentence becomes wrong on its own if the
  amendment in §5 is not made with it.
- **`ResetRecipient` is an ordinary `data class`, so `"$recipient"` prints the handle.** It is
  deliberately not given `EmailAddress`'s redacting `toString()`: `ADR-0031` §6.3 protects the
  *address* from log lines and says nothing about the handle, and inventing a rule the ADR did not
  make would also make every `assertEquals` failure in its tests unreadable. What keeps it out of a
  log today is that nothing on this path logs anything — `ADR-0077` §4 forbids the mailer from
  logging its arguments and forbids a success line at all. **The trigger is named: the first log line
  anywhere on the reset path is the day `ResetRecipient` needs the redaction.**
- **Two reads of `recovery_email` must be kept in step by hand.** `verifiedOwnerOf` and
  `resetRecipientOf` carry the same `WHERE` clause in two string constants, and nothing fails if one
  is edited and the other is not. They were one read until today.
- **The join runs an unindexed scan of `credential`.** So do `verifyCurrent` and `holdsCredential`,
  so this adds a third caller to a pattern rather than a new one — but it does add a third. The
  trigger: when `credential` is large enough for that to matter, all three want one
  `credential (player_id)` index together, as one ticket and not three.
- **`c.kind = 'password'` is a literal, and a second credential kind that carries an identifier makes
  this query ambiguous.** `DEC-027` (*may one player hold several credentials?*) is open and
  `CredentialKind` was deliberately built to admit `oauth:<provider>` rows. The day a second kind with
  an identifier lands, this statement can return two rows and its `handle` stops meaning one thing.
- **One more member on a port four route handlers already depend on.** `ADR-0031` §6's argument is
  that a violation should be a visible diff; every member added to these ports makes the next
  addition slightly less visible.

**What it forecloses.**

- **`Credentials.handleOf(playerId)`, and its family, for as long as this ADR stands.** Not
  discouraged — gated. Adding one means deleting a test, which is a diff a reviewer reads, and
  amending this ADR.
- **A handle in a response body, on the wire, or on a screen**, which `ADR-0031` §1 had already
  foreclosed and which this decision declines to make one step easier.
- **It forecloses nothing about `issue`.** If a second legitimate need for a handle ever appears,
  giving `issue` a sealed result is still available, still cheap, and still the direction
  `ClaimPendingResult`'s KDoc recommends.

**Why this shape rather than a more thorough one.** The evidence is thin — one caller, ever, and no
second use case anybody can name — so the decision is the one that is cheapest to unwind: one member
and one `data class` on an existing port, deleted in a single commit if it turns out wrong. A
dedicated port would have been the more emphatic answer and is available later at the cost of moving
two declarations.

## Alternatives considered

**`Credentials.handleOf(playerId): String?`.** The strongest case is genuinely strong and it is what
almost anyone reaches for first: the handle lives in `credential`, `PostgresCredentials` is the class
that reads that table, the query is one line, it needs no new type, it touches no other port, and it
is symmetrical with `holdsCredential(playerId, kind)` which already reads by exactly that key.
Rejected because it is the reverse lookup `TASK-041615` refused **by name and with a reason**, and
because a `PlayerId` is in scope nearly everywhere on this server: the function would be callable
from every socket, every profile handler and every leaderboard read, and none of them would have to
hold a secret to call it. The next caller would not be malicious, they would be helpful. Overturning
a merged refusal for the first caller it would have inconvenienced is the exact shape of decision
`ADR-0071` refused when it declined to raise a threshold because *"the name I already wrote is two
over"*.

**`PasswordResets.issue` returns `Issued(handle)` / `Suppressed`.** The strongest case is nearly
decisive and is argued in full in §3 above: the transaction already holds the `player_id`, the class
already writes `credential`, the handle would be produced **only** in the branch that is about to
send a mail, `Suppressed` would carry nothing — which is exactly `ClaimPendingResult`'s shape — and
`RecoveryEmails`' own KDoc names this as the cheap direction if the two ports are ever made
symmetric. It lost on two things it could not answer. `issue(playerId, newResetToken())` is a
`PlayerId → handle` function with a loud side effect, so the invariant would hold only as a
convention about not calling something; and a verified address whose owner has no `password`
credential leaves `Issued` with nothing honest to put in a non-null `String`, forcing either a
nullable handle or an unreachable third case. It also costs ten files to what §1 costs four — a port,
an implementation, seven merged assertions, four test doubles and two KDocs that describe `issue` as
answering `false` — and every one of those is a merged artifact rewritten to serve a caller that does
not exist yet.

**`verifiedOwnerOf` returns the pair instead.** One read, not two, no duplicated `WHERE` clause, and
no new type name to choose — and its KDoc already says it exists *"for `forgot-password`"*, so
widening the thing built for this caller looks like the tidy answer. Rejected on a fact in the
source: it has a **second** caller. `recoveryRoutes`' attach handler calls it to decide whether an
address is already proven for someone else, and that handler must never hold a handle. Widening the
shared read would hand a handle to the one path in this system that mails a **stranger's** mailbox if
it gets its branch wrong.

**A dedicated one-member port, `ResetMailRecipients`, injected into `recoveryRoutes`.** The strongest
case is the one `ADR-0031` §6.2 itself makes: a port's *name* is what turns a violation into a
visible diff, which is why `RecoveryMailer` is called `RecoveryMailer`, and a port named for the
password-reset mail would make any second caller absurd on sight. It is the more emphatic answer and
it was close. Rejected on blast radius and reversibility, which this repository treats as first-class
(`ADR-0069`, `ADR-0070`): it is a new port file, a new implementation, a new `recoveryRoutes`
parameter, an edit to `Application.kt` and `ServerComponents.kt`, and **four new stub objects** in
the four route-test files that call `recoveryRoutes` positionally — against one member, one
`data class` and one existing double. And the fence that actually holds is the `EmailAddress`
parameter, which is identical under both shapes; the port name adds signalling, not safety. If the
signalling is ever wanted, moving two declarations into a new file is a small, late, safe change.

**The route composes the mail at a layer that already holds the handle.** The case is that the
cheapest lookup is the one you do not do. Rejected because no such layer exists: `forgot-password` is
**unauthenticated** — that is the whole point of the endpoint — so the caller supplies an address and
nothing else, and there is no session, no `Identity` and no prior context anywhere in the request
that has ever seen this player's handle. The only place the handle exists is the `credential` row.

**A `LoginHandle` value class with a redacting `toString()`, as `ADR-0031` §6.2's illustrative
snippet writes it.** The case is real: the handle is half a credential, `EmailAddress` and
`SessionToken` both redact, and the ADR's own snippet types the parameter that way. Rejected as scope
this decision does not need: `RecoveryMailer.kt` already records, in a merged KDoc, why the parameter
follows `loginHandleOrNull`'s `String?` rather than the snippet, and introducing the type properly
means `loginHandleOrNull`, `Credentials.verify`, `Credentials.create`, `AuthRoutes`, `SignUpFields`
and `PublicApiHasNoHashTest` — while introducing it improperly means two representations of one
concept and a conversion at a boundary. It is a good ticket and a bad rider on this one.

**The mail carries something other than the handle.** Not considered on the merits, because it is
not this agent's to consider: `ADR-0031` §1 names the reset mail as the single exception to the
handle appearing nowhere, and §6.2 gives the product reason. Changing what the mail carries would be
a product decision resting on `docs/vision.md`, and the mail's actual wording is already
`STORY-0412`'s under §5's explicit deferral.
