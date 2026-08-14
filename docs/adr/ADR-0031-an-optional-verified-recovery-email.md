# ADR-0031 — An optional recovery email, proven before it can do anything, and a handle that is not a name

- **Status:** Accepted
- **Date:** 2026-08-14
- **Records:** the human's answer to `DEC-027` — *optional email, recovery only* — and settles what
  that answer requires technically
- **Builds on:** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md), which deliberately
  shaped `credential` as `kind` + `identifier` + a **nullable** `secret_hash` so that any answer to
  `DEC-027` would fit. This is what fills that shape
- **Constrains:** `EPIC-04`'s `STORY-0403` (storage and hashing), `STORY-0404` (sign-up),
  `STORY-0407` (recovery), and `STORY-0412` (the account screens)
- **Leaves open:** whether a third-party identity provider is ever offered — the human's, and not
  answered by the option they chose; and `DEC-025`, `DEC-026`, `DEC-029`, all untouched here

## Context

The human has chosen, verbatim:

> **Optional email, recovery only** — a player may attach an email address solely to recover a
> password. Never used for contact or marketing. Recovery works for those who opted in; the rest
> carry the same risk as having no email at all.

That choice is not re-argued. Two of its consequences are stated at the top rather than buried,
because both are the kind of thing that gets softened on the way into a ticket.

**A player who declines the email has no recovery path.** Losing the password means losing the
account, and that loss is the *same* loss [`ADR-0012`](ADR-0012-device-bound-anonymous-profiles.md)
says the claim flow exists to prevent: *"a lost device is a lost profile."* The claim flow removes
the **device-loss** failure — a new phone finds the old profile. It does **not** remove the
**credential-loss** failure, and for an opted-out account nothing else does either. There is no
admin path in this system ([`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) built
none and this ADR builds none), so an opted-out player who forgets their password has lost their
coins exactly as finally as a player who cleared their site data in v0.1. The human accepted that
trade knowingly. It is a property of the product now, not an oversight to be fixed by a support
inbox later.

**"Recovery only" is a constraint on the whole system, not a note.** No marketing, no notifications,
no digests, no "your opponent resigned" mail, no transactional mail beyond the recovery mechanism
itself. A sentence in a document does not survive contact with a feature request; a schema that
cannot hold a mailing list and a port that has no `send(subject, body)` do. So the constraint is
built, not promised.

What is genuinely in tension:

- **The optional email splits accounts into two classes** with different failure modes, and the
  system must not quietly favour the recoverable one. In particular, nothing may *require* an
  address — not sign-up, not the claim, not any endpoint's success path.
- **The identifier problem is sharp and has no comfortable answer.** With email optional, an email
  cannot be what a player signs in with; something else must exist for every account.
  `ADR-0029` has just made display names unique and permanent, which makes them look like a login
  handle already built — and the same ADR states outright that *a name is never an authentication
  factor* and that no code path may resolve a player from one. Both cannot hold unless the third
  string is invented.
- **An unverified address is a weapon, in two directions.** It lets a stranger's mailbox receive
  mail this system was told never to send, and it lets an attacker squat the address of a player who
  has not signed up yet, denying them recovery permanently.
- **Every helpful error message is an enumeration oracle.** `EPIC-04`'s non-negotiables already
  require sign-in to answer identically for *no such account* and *wrong password*. A reset endpoint
  that says *no account has that address* is the identical defect through a different door — worse,
  because the thing enumerated is an email address rather than a handle somebody invented.
- **There is no mail infrastructure and no deployment.** `EPIC-07` owns hosting. Anything decided
  here has to be implementable behind a port and testable without a mail server.

### The deadline, honestly

Two parts are free today and expensive later.

**Verification.** It has to be decided **before the first address is stored**. Retrofitting proof
onto a table of addresses collected without it means treating every stored address as unproven and
mailing every one of them to re-establish something — which is a mass mailing, to people who were
promised recovery-only mail, in order to enforce the promise. There is no version of that which is
cheap or honest.

**The shape of the storage.** One row per player, primary-keyed by `player_id`, is a one-line choice
now and a data migration with a "which of these three addresses is yours" question later. The same
goes for never returning the address in a response body: a field the client has never seen is free
to withhold and awkward to withdraw.

Nothing else here has a deadline. Token lifetimes are constants. Whether a third-party provider is
ever added is left open and stays open, because `kind` already admits one.

## Decision

### 1. A player signs in with a login handle, and it is not their display name

**`credential` gains rows of `kind = 'password'`, whose `identifier` is a player-chosen login
handle and whose `secret_hash` is the Argon2id PHC string `ADR-0027` §1 specifies.** That is the
only kind this ADR creates, and the handle is the only thing a sign-in form ever accepts as an
identifier.

- **The handle is stored already folded**: ASCII-lowercased, 3–32 characters, each of
  `[a-z0-9._-]`, the first of `[a-z0-9]`. Sign-in folds the presented string the same way before the
  lookup, so `Bob` and `bob` are one account. `ADR-0027`'s `UNIQUE (kind, identifier)` then does the
  whole job with no expression index — the contrast with `ADR-0029` is deliberate: a *display name*
  is shown, so the form the player typed has to survive and the fold lives in an index; a handle is
  **never shown to anybody**, so only its canonical form needs to exist and there is no second
  representation to disagree with.
- **The rule lives in the write path, not in a `CHECK`.** `credential` is generic across kinds and a
  character rule is the part most likely to move — the same reasoning `ADR-0029` §3 used.
- **The handle appears in no response body and in no `ServerMessage`, ever.** The client knows it
  because the player typed it. The one exception is the password-reset mail (§4), which goes only to
  a proven mailbox.
- **Nothing here changes a handle.** No endpoint updates `credential.identifier`; the row is written
  once, at sign-up. That is not foreclosed — an `UPDATE` under the same unique constraint would do
  it — but it is not built, and no story should assume it.

**Reconciling this with `ADR-0029`, explicitly.** A unique permanent display name is *not* used as
the handle, and the two strings are unrelated: nothing joins them, no constraint keeps them equal,
no constraint keeps them different, and the server never derives or pre-fills one from the other.
Three reasons, and the second is the one that matters most:

1. `ADR-0029` §7 and `EPIC-04`'s non-negotiable say no code path resolves a player from a display
   name. Copying the name into `credential.identifier` at sign-up would obey that to the letter —
   the query would read `credential`, not `player` — while defeating precisely what it is for.
2. **`ADR-0029` accepted public enumeration of display names on the express ground that "a display
   name is half of nothing."** A leaderboard is a published list of them. Make the name the handle
   and that sentence becomes retroactively false: the leaderboard is a published list of *half a
   credential*, and permanence means no player can ever rotate out of it.
3. `display_name` is nullable — a profile exists before a name does — so sign-up cannot depend on
   one being set, and `ADR-0029`'s trigger means a handle-shaped name could never be changed after a
   leak.

A player *may* choose a handle that happens to equal their own display name, or somebody else's;
the server neither prevents it nor suggests it, and there is no endpoint that offers a default.
A player who chooses a handle equal to their published name has published half their own credential,
which is their business and not something the server can undo. It is also not impersonation: the
handle is never displayed.

### 2. The email lives in its own table, one row per player, and holds only proven addresses

```sql
-- ADR-0031. An address in this table exists for exactly one purpose: recovering a password.
-- It is never used for contact, notification or marketing. One row per player, so this table
-- cannot become a mailing list; one player per address, so a reset is never ambiguous. Only
-- verified addresses live here — an unproven address is a row in email_verification.
CREATE TABLE recovery_email (
    player_id   UUID        PRIMARY KEY REFERENCES player (id),
    address     TEXT        NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX recovery_email_address_unique
    ON recovery_email (lower(address COLLATE "und-x-icu"));
```

- **`player_id` is the primary key.** At most one address per player — the schema *cannot* hold a
  second one, so it cannot hold a contact list. Multiple addresses is a contact feature and the
  cheapest way to never build it is to make it a migration rather than an `INSERT`.
- **`verified_at` is `NOT NULL`**, so the table cannot represent an unproven address at all. The
  "is it verified" branch does not exist in any query, because the row's existence *is* the proof.
- **The unique index is the answer to "two players, one address": the first to verify owns it.**
  The fold is `lower()` under the pinned `und-x-icu` collation, for the reason `ADR-0029` §1 pins
  it — an unpinned fold enforces a different rule on the test container than on whatever `EPIC-07`
  deploys. The local part of an address is case-sensitive to the letter of the RFC and case-
  insensitive at every mail provider anyone uses; folding is what stops `Bob@x.com` and `bob@x.com`
  being two accounts with one mailbox between them.
- **The stored form is the address as the player typed it**, because that is what must be delivered
  to. The fold is a collision test only: never stored, never displayed, never returned.
- **No `ON DELETE` clause**, following `ADR-0027` §2 exactly. `DEC-029` is unanswered and a cascade
  would answer part of it silently.
- **The address is never returned by any endpoint** — see §6. Verification is what makes that
  costless: a player whose address is stored has, by construction, received mail at it, so there is
  no typo left to display back to them.

### 3. An address is proven before it can do anything

**Verification is required. An unverified address resets nothing, and is not stored in
`recovery_email` at all.**

```sql
CREATE TABLE email_verification (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    address    TEXT        NOT NULL,
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT email_verification_one_per_player UNIQUE (player_id)
);
```

- **A pending address lives here and nowhere else**, and this table has **no unique constraint on
  `address`** — deliberately. Two players may hold a pending claim on the same address at once;
  whoever verifies first takes it, and the other's row becomes unverifiable. This is what stops an
  attacker squatting the address of a player who has not signed up yet: without it, a pending row in
  the unique namespace would deny the true owner recovery forever, and refusing the squat would need
  an oracle to detect.
- **`UNIQUE (player_id)`** means one pending attach per player. A second attach replaces the first
  within one transaction (`DELETE` then `INSERT`), so the table is bounded by the player count and
  an abandoned attempt cannot accumulate.
- **Attaching requires the current password**, even inside a valid session. A session token is a
  bearer credential in web storage (`ADR-0027`'s accepted cost); without this, a minute at an
  unattended browser converts into permanent ownership of the account.
- **Expired rows are deleted, and that deletion is not optional.** Unlike `auth_session`, whose
  expired rows are inert garbage, a row here holds an **unproven address** — personal data this
  system was given for one purpose and has not yet been able to use for it. The delete is one
  statement on the existing sweep ([`ADR-0025`](ADR-0025-one-ticker-coroutine-drives-both-sweeps.md)),
  never a second ticker. Expiry is enforced at read time regardless (`WHERE expires_at > now()`), so
  a missed sweep is a retention defect and never a security hole.
- **Lifetime: 24 hours**, computed from the injected `ServerClock` at issue. Attaching an address is
  a setup task a person may finish on another device an hour later; the token it produces cannot
  change a password.

**What the unverified state means for the account in the meantime: nothing.** There is no half-
attached state visible anywhere. Until verification, the account is *exactly* an account with no
email — `hasRecoveryEmail` is `false`, `POST /api/auth/forgot-password` for that address finds
nothing and behaves identically to an address nobody has ever mentioned, and the player carries the
opted-out risk in full. A player who attaches an address and never clicks the link has not opted in;
they have only intended to.

### 4. The reset: one token, one hour, one use, and every session dies

```sql
CREATE TABLE password_reset (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT password_reset_one_per_player UNIQUE (player_id)
);
```

- **The token is 256 bits from `SecureRandom`, URL-safe base64, unpadded** — the same generator and
  the same shape as `ADR-0027`'s session token, and given to nobody but the mailbox.
- **It is stored as SHA-256, in `BYTEA`, and this follows `ADR-0027` §2's rule deliberately.** The
  rule there is that a memory-hard hash exists because *humans* choose passwords; 256 bits of
  uniform entropy the *server* chose has nothing to guess offline, so Argon2 would buy nothing. That
  reasoning applies here unchanged. A reset token is *more* dangerous per unit than a session token
  — it rewrites the credential rather than borrowing it — but that argues for a shorter life and a
  single use, which is what it gets, not for a different hash of an unguessable string.
- **Lifetime: one hour absolute**, from `ServerClock` at issue. Long enough to survive slow mail
  delivery and a person walking to another device; short enough that a mailbox compromised next week
  is not a standing key to the account.
- **Single use, by construction, not by a flag.** Consumption is one statement inside the
  transaction that writes the new password:

  ```sql
  DELETE FROM password_reset
   WHERE token_hash = ? AND expires_at > now()
  RETURNING player_id
  ```

  No `used_at` column, no read-then-write window, no way for two concurrent submissions of the same
  token to both succeed.
- **`UNIQUE (player_id)` means one live reset token per account**, so a new request supersedes the
  old one rather than adding to a pile of valid keys.
- **A successful reset deletes every `auth_session` row for that player, in the same transaction as
  the password write.** The usual reason to reset is that somebody else has the password; leaving
  their 30-day session alive would make the reset theatre. `ADR-0027` gave sessions an absolute
  lifetime and no revocation trigger — this is the first one, and it is served by the existing
  `auth_session_player_id_idx`.
- **The reset issues no session and returns no token.** It answers `204`; the player signs in with
  the new password like anybody else — including on the device they just used, which is the visible
  face of the sweep above. This keeps the endpoint incapable of handing out a credential, so a
  leaked reset link cannot be exchanged for a live session by anything but a full sign-in.
- **The new password goes through the same policy and the same Argon2id parameters as sign-up.**
  This ADR adds no second password rule and no second hashing path.
- **The device-id binding is untouched.** A reset changes a credential; it moves no profile, no
  coins and no history. That is `DEC-026`'s territory and nothing here enters it.

**The token travels in a URL fragment, never in a query string.** The mail contains
`<baseUrl>/reset#token=<token>`; the client reads `location.hash`, `POST`s the token in a request
body, and clears the hash with `history.replaceState`. `ADR-0027` rejected a token in a URL outright
because *"bearer secrets in URLs are written to access logs, proxy logs and browser history"* — a
fragment is never sent to any server, so no access log, no proxy log and no `Referer` header can
contain it. **The endpoint accepts the token only in a request body and never as a query
parameter**, so the safe path is the only path. The residual is honest and named in the
consequences: the link is in the mailbox and in browser history, bounded by one hour and one use.

**`baseUrl` is one configured value and is never derived from a request header.** Not `Host`, not
`X-Forwarded-Host`. Building a reset link from a header is how reset links get rewritten to point at
someone else's domain, and it is the kind of thing that reads as harmless in a diff.

### 5. Every endpoint, and what it answers in every case

All are plain HTTP under `/api/auth/`, contracted in [`docs/protocol.md`](../protocol.md) beside the
endpoints `ADR-0027` §3 named. None is a `ServerMessage`; no wire version moves, because
`protocol.gen.ts` is emitted from `ClientMessage` and `ServerMessage` only.

| Endpoint | Body | Answer |
| --- | --- | --- |
| `POST /api/auth/sign-up` | handle, password — **and no address field exists on it** | per `ADR-0027`; `409` when the handle is taken (see the cost in §Consequences) |
| `POST /api/auth/recovery-email` | address, current password | `202` always, empty body. `401` unauthenticated, `403` wrong current password, `400` an address that is not syntactically an address |
| `POST /api/auth/verify-email` | token | `204`. `400` for a token that is unknown, expired or already consumed — the three are indistinguishable. `409` when the address is already verified to another player |
| `POST /api/auth/forgot-password` | address | **`202` always, empty body** |
| `POST /api/auth/reset-password` | token, new password | `204`. `400` for a bad token, `422` when the token was good and the new password fails policy |
| `DELETE /api/auth/recovery-email` | current password | `204` whether or not one was attached; `401`/`403` as above |

**`forgot-password` answers `202` in every case**, and the cases are: the address is unknown; the
address is pending but unverified; the address is verified and a mail is sent; the request is over
budget; no mail sender is configured at all. The response is written **before** any mail work, and
delivery runs on a detached coroutine, so response latency does not vary with whether an address
matched — the timing side channel is closed for the same reason `ADR-0027` §6 verifies unknown
identifiers against a dummy hash.

**`recovery-email` answers `202` even when the address already belongs to another player**, and
sends nothing in that case. The alternative — a `409`, or a "this address is already in use" mail —
either tells a stranger that an address is registered, or sends unsolicited mail to a mailbox whose
owner did nothing, and the second is forbidden outright by "recovery only".

**`verify-email`'s `409` is not an oracle, and the distinction is exact.** Its caller has already
proven possession of the mailbox by holding a token that was mailed to it. Telling the proven owner
of an address that the address is spoken for reveals nothing they could not learn by requesting a
reset. This is the same test `ADR-0029` §5 applied: a `409` is acceptable exactly when the caller
already holds the secret it would otherwise disclose.

**Budgets.** `forgot-password` and `recovery-email` are budgeted by remote address, reusing the
shape [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) established and `ADR-0027` §6 reused —
a rolling window, values in a config value class, time from `ServerClock`, state in memory.
**Over budget answers `202`, identically to success**, so the limiter is not itself an oracle.
Against mail-bombing a victim from many addresses, the durable defence is the row: **a mail is sent
only if the player has no live token issued within the last 15 minutes**, read from `issued_at` on
the existing `UNIQUE (player_id)` row. Inside that window the request is a complete no-op — nothing
sent, and crucially the outstanding token is *not* invalidated, so a double-click does not destroy
the link the player is about to use. This needs no new state, survives a restart, and caps any one
account at four recovery mails an hour.

### 6. "Recovery only" is enforced by the shape of the code, not by this sentence

Five mechanisms, each of which turns a violation from an easy `INSERT` into a visible diff:

1. **`recovery_email` is keyed by `player_id`**, so no player can accumulate addresses and the table
   cannot become a list (§2).
2. **The mail port has exactly two functions, both named for the only two permitted mails.** There
   is no `send(to, subject, body)` anywhere in the codebase:

   ```kotlin
   public interface RecoveryMailer {
       public suspend fun sendVerification(address: EmailAddress, token: VerificationToken)
       public suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: LoginHandle)
   }
   ```

   Adding a newsletter therefore means adding a member to a port called `RecoveryMailer`, in a diff
   a reviewer reads. **A test asserts the interface declares exactly these two members**, structurally
   over the public API, in the same way `ADR-0027` makes *no function returns a hash* structural.
   The reset mail carries the handle because a player who forgot their handle would otherwise be
   unrecoverable despite having opted in (§Consequences); it costs nothing, since anyone holding the
   token already controls the account.
3. **The address never leaves `duels.poker.server.db` except into that port.** It is in no response
   body, no `ServerMessage`, no log line. `ProfileResponse` gains `hasRecoveryEmail: Boolean` and
   nothing more — the client can say *recovery is on* and can never display the address.
   `EmailAddress` is a value class whose `toString()` returns a fixed redaction, exactly as
   `ADR-0027` did for `SessionToken`, so leaking one into a log line requires intent rather than a
   careless string template.
4. **No delivery log records an address.** If sends are logged at all, the line carries `player_id`
   and a template name. A log that recorded addresses would be a mailing list with a different file
   extension, and would make any future erasure a hunt.
5. **The migration says so in a comment** (§2), where the next person to add a column will read it.

### 7. Where it lands

- **One new migration file** carrying all three tables and the unique index, taking the next free
  `V<n>` at merge time. `V1` and `V2` are byte-unchanged; `ADR-0021`'s `display_name` migration and
  `ADR-0027`'s `credential`/`auth_session` migration are independent work, and whichever lands last
  takes the next number. Never an edit to a merged migration.
- **Ports in `duels.poker.server.auth`** (`RecoveryEmails`, `PasswordResets`, `RecoveryMailer`),
  Postgres implementations in `duels.poker.server.db`, and the mailer implementation in
  `duels.poker.server.mail` — the same port-here, implementation-there layout as
  `PlayerDirectory`/`PostgresPlayerDirectory`.
- **The mail transport is not decided here.** SMTP relay or a provider API is a deployment question
  and belongs to `EPIC-07`; the port is the boundary that lets this ship without it. Configuration
  selects the implementation, and a build with no sender configured is a valid state in development
  and tests: the endpoints behave identically from the outside (`202`), and nothing is sent.
- **`poker-engine` learns nothing.** No address, handle, credential, token or mail type exists in it
  or crosses into it, and its dependency allowlist does not move.
- **No `PROTOCOL_VERSION` change.** These are plain-HTTP DTOs.

## Consequences

**What it buys.** `STORY-0403`, `STORY-0404` and `STORY-0407` unblock with a schema, an identifier,
a flow and an answer for every response code. A player who opts in can lose their device *and* their
password and still find their coins — which is `ADR-0012`'s debt paid one step further than the
claim flow alone pays it. "Recovery only" becomes a property somebody has to work to break rather
than a sentence somebody has to remember. And the whole email half is bolted to the side: remove
`recovery_email`, `email_verification`, `password_reset` and the port, and what remains is a
complete, working handle-and-password system.

**What it costs.**

- **An opted-out account has no recovery, and the failure is total.** Forget the password, or forget
  the handle, and the account is gone with its coins and its ladder place. This is the human's
  accepted trade and it will be somebody's actual Tuesday.
- **Sign-in cannot even tell them which half they got wrong.** `EPIC-04`'s non-negotiable makes
  *no such handle* and *wrong password* indistinguishable, so a player who mistypes their handle
  sees the same refusal as a player with a wrong password — and for an opted-out account there is no
  reset flow to fall back into and discover the truth. The no-oracle rule and the no-recovery case
  compound each other, and this is the sharpest edge of the whole decision.
- **Two unique namespaces, two "taken" errors.** A player picks a handle at sign-up and possibly a
  display name later, and each can be refused for a reason the other does not share. That is the
  price of keeping the name out of the auth path, and the client has to render both.
- **`POST /api/auth/sign-up` leaks that a handle exists**, via its `409`. There is no way around it:
  a sign-up form that will not say *taken* is unusable, and there is no email to verify first because
  the email is optional. The leak is accepted, budgeted by remote address, and worth exactly this
  much: a handle without its password buys nothing, and `ADR-0027` §6 deliberately keys the sign-in
  budget by address rather than by identifier, so an enumerated handle cannot be used to lock its
  owner out.
- **An honest player whose address is already attached elsewhere gets silence.** They typed an
  address, got `202`, and no mail arrives; they will conclude the mail is broken. That is the price
  of refusing the oracle, and it is unfixable without paying it.
- **A misconfigured deployment has silently broken recovery**, and the no-oracle rule makes it
  undetectable from outside — every request answers `202` whether or not anything was sent. `EPIC-07`
  needs a startup log line and a health check for the sender, or the first person to discover it will
  be a player who cannot get back in.
- **The reset link sits in a mailbox and in browser history.** The fragment keeps it out of every
  server log, but not out of those two. One hour and one use is the whole mitigation.
- **Three more tables, a sweep statement, and a mail port to build and fake in tests** — in a schema
  that was four tables until `EPIC-04` opened.
- **The address is personal data, and this system now holds some.** That was true the moment the
  human chose this option; it is written here because it is the first time this repository stores
  anything about a person that was not minted by the server.

**What it forecloses.**

- **Email as a sign-in identifier**, for as long as this ADR stands. Adding it later is additive
  (a `kind = 'email'` credential row) and can be done without a migration; *removing* it later, once
  players have learned to sign in with it, cannot. That asymmetry is the reason for the order, and
  it is the reason this option was chosen over the more familiar one on evidence that is thin either
  way.
- **Any use of the address that is not recovery**, without an ADR that supersedes this one — which
  is the intended effect, not a side effect.
- **More than one address per account**, which would need a migration rather than an insert.
- It does **not** foreclose a third-party identity provider: `kind` admits `oauth:<provider>` rows
  with a null `secret_hash`, exactly the shape `ADR-0027` left room for. Nothing here builds one.

**What this does not settle.**

- **Whether a third-party sign-in is ever offered — the human's, and genuinely unanswered.** The
  `DEC-027` register row noted that this decision *"also decides whether social sign-in is on the
  table at all"*, and the option the human chose says nothing about it: it settles the email
  question completely and leaves the provider question untouched. Phrased so it can be answered in
  one sentence: *may a player ever sign in with a third-party account (Google, Apple), or is a
  handle and password the only credential this product will offer?* Recommended bookkeeping: a new
  `DEC-NNN` marked as the human's, blocking nothing today. **The register row is the driver's to
  write; this ADR does not edit the register.**
- **When, and how insistently, a player is asked for an address**, and the wording of both mails.
  That is what a player sees and is told; it belongs to `STORY-0412` inside `EPIC-06`'s design
  language, under the human's framing. The server has no opinion and offers no default: it never
  prompts, and every endpoint here works identically for a player who never attaches anything.
- **`DEC-025`, `DEC-026` and `DEC-029` are untouched.** Sign-up attaches a credential to whichever
  player `ADR-0027`'s `IdentityResolver` already resolved; a reset moves no coins and no history.
- **`DEC-029` (deletion) is closer to this ADR than to any other, and here is what the schema must
  not foreclose.** An address is personal data, so *erase my email* is a request this system will
  eventually receive, whether or not full account deletion is ever built. Three properties keep the
  answer cheap and are chosen for that reason: the address is stored in exactly **one column of one
  row**, so erasure is a single `DELETE` (`DELETE /api/auth/recovery-email` is already that
  statement); **no history is kept** — no `previous_address`, no mail log carrying an address — so
  an erased address leaves nothing behind; and there is **no `ON DELETE` clause** on any of the three
  tables, so whoever answers `DEC-029` must say out loud what happens rather than discover that a
  cascade already decided. Deleting the address returns it to the free namespace, and reverts the
  account to the opted-out risk in full. Note also the shape of the remaining question: recovery
  never touches `duel_result`, so an erased address orphans nothing.

## Alternatives considered

**The display name is the login handle.** The strongest case is genuinely strong, and it is the
option a reasonable person reaches for first: `ADR-0029` has already built a unique, permanent,
case-folded namespace with a race-safe reservation, and it is *the same machinery* a handle needs.
One string for the player to remember instead of two, one "taken" error instead of two, one fewer
concept in the sign-up form, and no new column anywhere. Rejected on three counts, and the second
alone is decisive: `ADR-0029` accepted that a `409` publicly enumerates display names on the express
ground that *"a display name is half of nothing"* — a leaderboard *is* a published list of them —
and making the name a login identifier converts that published list into a list of half-credentials,
retroactively, for names that permanence guarantees nobody can ever rotate. Add that `EPIC-04`'s
non-negotiable forbids resolving a player from a name (copying it into `credential.identifier` obeys
the letter and defeats the purpose), and that `display_name` is nullable so a profile exists before
a name does, and there is no version of this that survives.

**Sign in with the email when present, a handle when not.** The strongest case is that this is what
almost every product does, that opted-in players then remember one string rather than two, and that
"forgot your handle" stops existing for them. Rejected because it puts the address into the sign-in
path, where it is typed into every form, autofilled, and one careless log line from being recorded —
which is the opposite of the address appearing in exactly one flow. It also makes the account's
identifier *move*: attaching an email would add a way to sign in and deleting it would take one
away, so the answer to "what do I sign in with" would depend on state the player may not remember
changing. And it is available later at low cost, as a `kind = 'email'` row, which is the deciding
argument on thin evidence.

**An unverified address is good enough to reset with.** Its strongest case is real: verification is
an entire flow — a second token, a second table, a second mail template, a second sweep — and it
exists to defeat an attack nobody has yet attempted against a game with no money in it. Every one of
those pieces could be deferred. Rejected because the two failures it prevents are unfixable
afterwards. A squatted address permanently denies its true owner the recovery this decision exists
to provide, and the fix requires an oracle to detect the squat. And an unverified attach makes this
server a mail relay a stranger can point at any mailbox, which is the "recovery only" promise broken
by the very feature that promise was made about. It is also the one part of this ADR with a real
deadline: retrofitting proof onto collected addresses means mailing all of them.

**The pending address stored in `recovery_email` with a nullable `verified_at`.** One table instead
of two, and the "is it verified" state is right there on the row. Rejected because the unique index
would then cover unproven addresses, which is exactly the squat above, and because a nullable
`verified_at` puts an `IS NOT NULL` on every read where forgetting it once is a security defect.
Keeping unproven addresses in a different table makes the dangerous state impossible to select by
accident.

**A nullable `player.email` column.** The smallest possible change, no join, trivially 1:1, and it
reads naturally. Rejected for the reason `ADR-0027` rejected credentials as columns on `player`,
which applies here with more force: `player` is read on the profile path and joined for the
opponent's name, so the address would sit one careless `SELECT *` from a response body — and the
column would be visible to every query in the system, which is precisely the surface "recovery only"
needs to be small.

**A `credential` row with `kind = 'email'` and a null `secret_hash`.** `ADR-0027` shaped the table
for exactly this and it costs no new table at all, which is a serious argument. Rejected on two
grounds. `credential` is the table the sign-in lookup reads, keyed `(kind, identifier)`, and putting
the address in it means the address is one `kind` value away from being a sign-in identifier —
somebody will widen that lookup, and the row will be sitting there inviting it. And `credential`
admits many rows per player by design, so nothing structural would stop five addresses; the
`player_id` primary key is the property that makes a mailing list unrepresentable, and it cannot
exist in a table that must hold several credentials per player.

**Argon2id for the reset token, for consistency with the password.** Consistency is worth something
in a security design, and a reset token is the more dangerous of the two secrets. Rejected on
`ADR-0027` §2's arithmetic, which does not change here: a memory-hard hash defends a *human-chosen*
secret against offline guessing, and there is nothing to guess in 256 bits of server-chosen entropy.
The danger of a reset token is answered by one hour and one use, not by the cost of hashing it.

**A short numeric code the player types, instead of a link.** Its strongest case is that nothing
ever enters a URL, a mailbox, or browser history in a form that can be clicked by the wrong person,
and it works when mail is read on a different device than the game. Rejected because six digits of
entropy have to be defended by a per-account attempt counter, and a per-account counter is both an
enumeration oracle and a way to lock a victim out of their own recovery — the exact failure
`ADR-0027` §6 avoided by keying the sign-in budget on the address instead of the identifier. A
256-bit token in a fragment needs no counter at all.

**A reset that signs the player straight in.** One less step for somebody who has just proven they
own the mailbox, and it removes the awkward moment of typing a password immediately after choosing
it. Rejected because it makes the reset endpoint capable of issuing a credential, so anything that
leaks the link — a shared screen, a forwarded mail, a browser history on a family laptop — yields a
live 30-day session rather than a password change the player will notice. Signing in afterwards
costs one form and proves the player kept what they just set.

**Leave existing sessions alive after a reset.** Its case: the player is not signed out of their own
phone, which is friendlier, and most resets are simple forgetfulness rather than compromise.
Rejected because the resets that matter are the other kind, and a reset that leaves the attacker's
session running for another 30 days is worse than no reset, because it looks like it worked.
