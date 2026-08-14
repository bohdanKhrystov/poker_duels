# ADR-0027 — A session token outranks a device id, and the handshake carries it

- **Status:** Accepted
- **Date:** 2026-08-14
- **Resolves:** `DEC-028` (the credential, session and handshake *mechanism*; everything about what
  a credential **contains** stays with `DEC-027`, the human's)
- **Constrains:** `EPIC-04`'s credential chain — `STORY-0403` (storage and hashing),
  `STORY-0405` (sign-in, the session, and what the socket presents), `STORY-0406` (the claim);
  and the wire, via `PROTOCOL_VERSION`

## Context

Today a device id is the whole of identity. `Hello.deviceId` is a nullable string the client may
present; if it does not, `RandomDeviceIdSource` mints one, `PlayerDirectory.resolve` creates or
finds the profile, and `Welcome` hands the id back. HTTP does the same with `X-Device-Id`, except
that an unknown value is refused rather than created — a deliberate crawler guard. `ADR-0012` is
blunt about what that buys and what it costs: *"a lost device is a lost profile."*

`EPIC-04` pays that debt, and the moment it does, a player can hold **two** things that name them:
the device id already in their browser, and whatever they get for proving a credential. Everything
downstream then hangs on questions nobody has answered. Which of the two wins when both arrive on
the same `Hello`? Is the second thing a signed blob or a row? Does it go in a header the browser
WebSocket API cannot set? Does the wire version move?

The forces pulling against each other:

- **`ADR-0002` versus convenience.** A client presents a credential and is *told* who it is. A
  self-describing token the server never looks up is a client-carried assertion of identity that
  happens to be signed — cheap to verify, impossible to revoke, and one step from the thing that
  ADR forbids.
- **Anonymous play must not become collateral damage.** `GET /api/me` answers for a device that
  never made an account, and `EPIC-04`'s definition of done says it still must. Every new path has
  to be additive to a path that already works.
- **A browser cannot set a header on a WebSocket upgrade.** Whatever HTTP uses, the socket needs an
  answer that survives that fact, and the answer must not be a query parameter, because a query
  parameter is written to every access log between here and the client.
- **Naming has already been spent.** `Session`, `SessionId` and `SessionRegistry` exist and mean
  *a live socket bound to a player*. An authentication session is a different thing with the same
  obvious name, and the cheapest mistake available is to overload one onto the other.
- **Four of the surrounding decisions are the human's and unanswered.** `DEC-025` (is an account
  ever required), `DEC-026` (what a claim converts), `DEC-027` (email, recovery, whether a password
  is even the credential) and `DEC-029` (deletion). A schema or a precedence rule can answer any of
  them by accident, in exactly the way `ADR-0021` refused to answer `DEC-017` with a `UNIQUE`
  constraint nobody asked for.

### The deadline, honestly

One part of this is free today and expensive later: **the wire break.** No client is deployed —
`EPIC-03` is mid-flight and `EPIC-07` has hosted nothing — so changing the handshake costs one
regeneration of `protocol.gen.ts` and one compile error. After the first browser caches a client,
the same change costs a compatibility window and a dual-read path. Nothing else here has a
deadline: tables can be added whenever, and the hash parameters are stored per-row precisely so
they can be raised later.

## Decision

**Three things, three names, and the session always wins.**

### 1. A credential is a row nothing can read back

A new table, in a new migration file — never an edit to an existing one. `ADR-0021`'s
`display_name` migration and this one are independent work on parallel branches, so whichever
lands second takes the next free version number:

```sql
CREATE TABLE credential (
    id          UUID        PRIMARY KEY,
    player_id   UUID        NOT NULL REFERENCES player (id),
    kind        TEXT        NOT NULL,
    identifier  TEXT        NOT NULL,
    secret_hash TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT credential_kind_identifier_unique UNIQUE (kind, identifier)
);
```

- **`kind` and `identifier` are how a credential is found; `secret_hash` is what is proved.**
  `secret_hash` is **nullable** because whether a credential has a server-held secret at all
  depends on what kind of credential the human chooses in `DEC-027`. This shape holds a password,
  a passphrase against a handle, or a third-party subject, without preferring any of them.
- **`UNIQUE (kind, identifier)` is a lookup key, not a product rule.** An identifier that resolves
  to two rows cannot be signed in with at all — the constraint is what makes `verify` a function.
  This is the opposite case to `ADR-0021`'s refusal to make `display_name` unique: that would have
  decided whether two humans may share a label, which is a product question.
- **`credential.identifier` is never `player.display_name` and never a foreign key to it.** If
  `DEC-027` lands on a human-chosen handle, that handle is stored here as a credential identifier
  and the display name stays a separate, non-authenticating label. No code path resolves a player
  from `display_name`, ever.
- **Hashing: Argon2id, via Bouncy Castle** (`org.bouncycastle:bcprov-jdk18on`, a new entry in
  `gradle/libs.versions.toml`), parameters `m = 19456 KiB, t = 2, p = 1`, a 16-byte salt from
  `SecureRandom`, a 32-byte output. Bouncy Castle rather than a JNA-bound native Argon2 because
  `EPIC-07` builds a container and a native binary in it is a deployment hazard for no benefit.
  The parameters are OWASP's Argon2id baseline and are chosen against a small host: 19 MiB per
  verification, not 64.
- **`secret_hash` stores a PHC string** — `$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>` — so the
  parameters travel with the row. Raising them is a constant change plus a rehash on next
  successful verify, never a migration. Bouncy Castle does not emit PHC, so encoding and parsing
  it is ours to write and to test against published vectors.
- **The hash never leaves `duels.poker.server.db`.** The port in `duels.poker.server.auth` exposes
  `verify(kind, identifier, presented): PlayerId?` and functions that write; **no function
  anywhere returns a hash**, so there is no code path that could log or serialise one. This is
  structural, and a test asserts it over the public API rather than by inspection.
- **Verification runs on `Dispatchers.IO.limitedParallelism(4)`**, so peak Argon2 memory is bounded
  at roughly 4 × 19 MiB. A memory-hard hash with unbounded concurrency is a self-service denial of
  service.

### 2. An auth session is an opaque token and a row, and it is not a `Session`

```sql
CREATE TABLE auth_session (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX auth_session_player_id_idx ON auth_session (player_id);
```

- **The token is 256 bits from `SecureRandom`**, URL-safe base64, no padding. It is given to the
  client exactly once — in the body of the response that created it — and is never stored in
  plaintext anywhere, never returned again, never in a `ServerMessage`, never in a URL, never
  logged.
- **The stored form is SHA-256 of the token, not Argon2**, and the reason is the whole point of the
  distinction: Argon2 exists because *humans* choose passwords and a leaked hash must survive an
  offline guessing attack. A session token has 256 bits of uniform entropy that the *server* chose;
  there is nothing to guess, so a memory-hard hash buys nothing and would cost a full Argon2 on
  every authenticated request. What actually matters — a leaked database yields nothing replayable
  — holds either way. **This is a deliberate reading of "hashed with a memory-hard function" as a
  rule about the human-chosen secret**; if that reading is wrong the fix is a one-line change plus
  everyone signing in again.
- **`SessionToken` and the presented secret are value classes whose `toString()` returns a fixed
  redaction.** Leaking one into a log line then requires intent, not a careless string template.
- **Lifetime: an absolute 30 days from issue, computed from the injected `ServerClock`. No sliding
  window, no idle timeout, no refresh token.** A sliding expiry means a write on every authenticated
  request or a lazy-write heuristic to avoid one, and buys little in a game with no money in it.
  Thirty days is a constant with no schema consequence; changing it changes only sessions issued
  afterwards.
- **Expiry is enforced at read time** (`WHERE expires_at > now()`), so a row that outlives its
  expiry is garbage, never a security hole. Deleting expired rows is housekeeping; if it is ever
  built it joins the existing ticker per `ADR-0025` rather than starting a second one.
- **A player may hold many sessions at once** — a phone and a laptop. Sign-out deletes the
  presented row; revoking everything is a delete by `player_id`, which the index serves.
- **No `ON DELETE` clause, deliberately.** Whether a player row is ever deleted is `DEC-029`. The
  default `NO ACTION` means a deletion feature, if it is ever built, has to say out loud what
  happens to credentials and sessions instead of silently cascading.
- **No `ip`, no `user_agent`, no `last_used_at`.** Those columns exist to power a "your devices"
  screen nobody has asked for, and an IP column is personal data with a retention question
  attached.
- **The existing `Session`, `SessionId` and `SessionRegistry` are not renamed and learn nothing.**
  They mean a live socket. The new type is `AuthSession`, in `duels.poker.server.auth`, with
  `PostgresAuthSessions` in `duels.poker.server.db` — the same port-here, implementation-there
  layout as `PlayerDirectory`/`PostgresPlayerDirectory`.

### 3. The wire: `Hello` gains a token, HTTP gains `Authorization`

- **Socket.** `Hello` gains `sessionToken: String? = null`, a generated protocol field like every
  other. In band, because a browser cannot set a header on a WebSocket upgrade and a query
  parameter is logged by everything it passes through.
- **HTTP.** `Authorization: Bearer <token>` beside the unchanged `X-Device-Id`. Sign-up, sign-in,
  sign-out and the claim are plain HTTP — `POST /api/auth/sign-up`, `/sign-in`, `/sign-out`,
  `/claim` — contracted in `docs/protocol.md`. **What their request bodies contain is `DEC-027`'s
  and is not decided here.** They are HTTP and not socket messages because the lobby exists before
  any socket does.
- **`Welcome` gains `playerId: String`, and its `deviceId` becomes `String?`** — present exactly
  when this connection's identity came from a device id. This is the server telling a client who it
  is, which is `ADR-0002` working, not being bent.
- **A connection's identity is fixed at `Hello` and never changes for the life of the socket.**
  There is no sign-in message. `SeatOwnership`, `SessionRegistry`, `ConnectionDirectory` and room
  membership are all keyed by `PlayerId`; a socket that could change its own player would make
  every one of them wrong at once. Sign in over HTTP, then reconnect.

### 4. Precedence: the session wins, unconditionally

One resolver — `IdentityResolver` in `duels.poker.server.auth` — is called by the socket handshake
and by every HTTP route. Not one per entry point; a rule implemented twice is a rule with two
behaviours.

1. **A session token is present** → verify it. Valid: that player, full stop, and any device id
   presented alongside it is **ignored, not even validated**. Invalid, expired or unknown: the
   connection or request is **refused** — `ProtocolError.INVALID_SESSION` on the socket, `401` with
   an empty body over HTTP. Expired and never-existed are indistinguishable.
2. **No session token, a device id is present** → today's behaviour, unchanged: the socket resolves
   or creates the profile, HTTP refuses an unknown id.
3. **Neither** → the server issues a device id and creates the profile, exactly as now, and
   `Welcome` carries both it and the `playerId`.

**An invalid session never falls back to the device id.** A silent downgrade from *signed in as A*
to *anonymous B* would let a player win a coin into an account they think they are not using; a
refusal is loud, and the client's remedy is to discard the token and reconnect anonymously.

**When the session's player is not the device's player, nothing moves.** The connection is the
session's player and the anonymous profile is untouched — not merged, not deleted, not relinked.
That is `DEC-026`'s question, and the precedence rule is written so that no code path answers it by
accident.

### 5. `PROTOCOL_VERSION` moves to 3 — once

In the story that lands the handshake (`STORY-0405`), not once per field, and not again for the
claim, which adds no wire field. The break is real in both directions: `protocolJson` sets
`ignoreUnknownKeys = false`, so an old server rejects a `Hello` carrying `sessionToken`, and an old
client cannot decode a `Failure` naming the new `INVALID_SESSION`. The decisive case is subtler —
a stale cached client would receive a `Welcome` with a null `deviceId`, conclude it has no device,
mint a fresh one and **abandon the profile it was holding**. That is the exact harm `ADR-0012`
named and `EPIC-04` exists to repair, so the version bump is not bookkeeping.

### 6. Sign-in tells a stranger nothing

- **An unknown identifier is verified against a fixed dummy Argon2 hash** before failing, so the
  no-such-account path burns the same time as the wrong-secret path. Without it, Argon2 *is* the
  enumeration oracle: an instant refusal means the identifier is free, a slow one means it is
  taken.
- Both failures answer identically: the same status, an empty body, no header that differs.
- **Failed sign-ins are budgeted by remote address**, reusing the shape `ADR-0022` established for
  failed room joins — a rolling window, values in a config value class, time from `ServerClock`,
  state in memory. Keyed by address rather than by identifier, because an identifier-keyed budget
  lets an attacker lock a victim out. **Over budget answers exactly like a wrong secret**, so the
  limiter is not itself an oracle. Behind a proxy this needs the forwarded-header configuration
  `EPIC-07` will own; until then it meters one address per client, which is what it is for.
- `poker-engine` learns nothing from any of this. No credential, session, name or account type
  exists in it or crosses into it.

## Consequences

**What it buys.** The credential chain becomes writable: `STORY-0403` has a table, a hash and a
parameter set; `STORY-0405` has a precedence rule, a transport and a version bump; `STORY-0406` has
somewhere to attach. Recovery works by construction — a device that has never been seen presents a
token and is told who it is. Revocation is a `DELETE`, which matters the day `DEC-029` is answered.
Anonymous play is untouched: paths 2 and 3 are today's code.

**What it costs.**

- **A bearer token in web storage is exfiltrable by XSS**, where an `HttpOnly` cookie would not be.
  This is accepted with open eyes: the device id already has exactly this exposure and is *today's
  sole credential*, so the model is not weakened, only extended. It is also the layer that is
  cheapest to reverse — swapping to a cookie later changes one storage call and adds one
  `Set-Cookie`, and touches neither table, neither precedence, nor the protocol's semantics. **That
  reversibility is why this option was chosen over the one with the better XSS story**, on evidence
  that is genuinely thin either way.
- **Two devices signed into one account now evict each other**, and no code makes that happen —
  `SeatOwnership` already keys on `PlayerId`, so `ADR-0018`'s "the newest socket takes the seat"
  silently widens from *per device* to *per account* the moment an account can span devices. It is
  the right behaviour (one player, one seat) but it will surprise the first person who leaves a
  laptop connected and opens a phone, and it is recorded here because nothing else would record it.
- **A database round trip per authenticated request**, where a signed token would need none. On one
  indexed primary-key lookup against a small table, against a game that already costs a round trip
  per action, this is not a number anyone will measure.
- **Argon2 is a memory and latency cost on a small host**: ~19 MiB and ~50–100 ms per sign-in, held
  to four at a time. Sign-in is deliberately not fast.
- **A PHC encoder and parser to write and test**, because Bouncy Castle does not ship one.
- **A wire break**, spent deliberately while it is free.

**What it forecloses.** Stateless authentication as the *primary* mechanism — a JWT-shaped world
where a request is trusted on a signature alone. A fast path can be added over these rows later;
what cannot be added later is the ability to revoke a token that was never recorded. It also
forecloses signing in on an already-open socket, which stays a reconnect forever.

**What survives the human's four open decisions, and what does not.**

- **Independent of `DEC-025`** (is an account ever required). Requiring one is a refusal placed
  *above* the resolver — path 2 and 3 still resolve a player; a policy layer declines to seat them.
  No table, no precedence rule and no wire field changes either way.
- **Independent of `DEC-026`** (what a claim converts) *by construction*: precedence decides which
  player a connection is, and reconciliation is a separate, explicit, later step. If the answer is
  "merge", it is a transaction over `duel_result` and the balance, added to the claim endpoint. If
  it is "refuse", it is a check on the same endpoint. Neither touches this ADR.
- **Independent of `DEC-029`** (deletion), by the absence of a cascade.
- **Not independent of `DEC-027`**, and this is the honest boundary. `kind`, `identifier` and a
  nullable `secret_hash` were chosen to fit every answer, but two consequences follow from
  particular ones: if the human chooses third-party sign-in *only*, the Argon2 machinery is built
  and unused, and if the human chooses email recovery, a reset-token table is a later migration
  that this session model serves without change. **What a credential contains, whether it carries
  an email, and what recovery looks like are not answered here and must not be inferred from the
  schema.**

## Alternatives considered

**A stateless signed token (JWT or an HMAC blob).** Its strongest case is real: no database read on
any request, no table, no expiry sweep, and it scales past one server for free. Rejected because
revocation is the property that matters here. `DEC-026` may decide that claiming moves a profile
between identities and `DEC-029` may decide accounts can be deleted; under either, an already-issued
assertion stays valid until it expires, and the usual remedy — a revocation denylist — is the same
table this ADR has, minus the simplicity that was the argument for stateless in the first place.
It also sits closer than is comfortable to `ADR-0002`: an identity the server verified but never
looked up.

**An `HttpOnly; Secure; SameSite=Lax` cookie.** The strongest option on paper, and it nearly won:
the browser attaches it to the WebSocket upgrade *and* to `/api` automatically, and XSS cannot read
it — a genuinely better answer to the one security cost this decision accepts. Rejected because it
splits the model in two: the device id would keep travelling as an explicit header while the
session travelled implicitly, giving the two credentials different failure modes, different
cross-origin behaviour through the `EPIC-03` dev proxy, and a CSRF surface where there is none
today. Moving the whole model to cookies is the coherent version of this and is a bigger change
than `EPIC-04` should carry — and, as noted above, it stays available at a low price.

**A short-lived one-time ticket fetched over HTTP and presented in `Hello`.** Its strongest case:
the long-lived token never crosses the socket at all, and a stolen ticket is worthless in seconds.
Rejected because it is the right pattern when the socket terminates somewhere less trusted than the
API, and here they are the same process behind the same TLS. It would add an endpoint, a store and
a round trip in front of *every* connect — and reconnects are frequent by design, given `ADR-0013`'s
grace period and `ADR-0018`'s adoption.

**The token as a query parameter on `/ws`.** Requires no protocol change at all, which is its only
merit. Rejected outright: bearer secrets in URLs are written to access logs, proxy logs and browser
history, and "never logged" is a stated constraint, not a preference.

**The device id wins when both are present**, or **both must agree or the request is refused.**
The case for the first is that it is the smallest change and cannot break anonymous play; for the
second, that a mismatch is genuinely suspicious. Both were rejected for the same reason: the device
that signs in is usually a device that already has an anonymous profile, so "device wins" would
silently ignore a sign-in that succeeded, and "must agree" would refuse exactly the flow —
`STORY-0407`, signing in from a device that has never been seen or that carries someone else's
anonymous profile — that `EPIC-04` exists to deliver.

**Fall back to the device id when a session token is invalid.** Its case is user-friendliness:
nobody enjoys an error, and there *is* a working identity in hand. Rejected because it converts an
authentication failure into a silent identity swap, which is the one failure mode a player cannot
detect and cannot undo.

**Credentials as columns on `player`.** One table, no join, and trivially 1:1. Rejected: `player`
is read on the profile path and joined for the opponent's name, so a secret would live one careless
`SELECT *` away from a response body — and a fixed set of columns presumes a single credential per
player, which `DEC-027` has not decided.

**Argon2 for the session token too.** Consistent, and consistency is worth something in a security
design. Rejected on the arithmetic: a full memory-hard hash on every authenticated request, to
protect 256 bits of server-chosen entropy that no attacker can guess offline.
