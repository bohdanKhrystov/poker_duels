# ADR-0030 — A claim adds a credential and moves nothing; a sign-in swaps identity and moves nothing

- **Status:** Accepted
- **Date:** 2026-08-14
- **Records:** the human's answer to `DEC-026` — *claim migrates balance and history; signing into a
  different account keeps the anonymous profile, with no migration* — and settles what that answer
  requires technically
- **Amends:** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) in two narrow places: its
  endpoint list loses `/api/auth/claim` (§1 below), and the profile read path is keyed by the
  resolved player rather than by the device id (§4). Its credential table, its session table, its
  precedence rule and its wire changes are unchanged
- **Constrains:** `EPIC-04`'s `STORY-0406` (the claim), `STORY-0407` (recovery), `STORY-0412` (the
  account screens) and `STORY-0414` (the end-to-end proof)
- **Leaves open:** `DEC-025`, `DEC-029`, the moderation half of `DEC-017`, and one new question this
  answer creates — see *What this does not settle*
- **Adds no migration.** The schema this needs is already on disk or already decided

## Context

The human has chosen, verbatim: *"i want migration, when user has annonimus profile and then try too
create accout we shoud migrate balance and history from anonimus account; if it is sign in from
annonomus to some different account i would like to keep anonimous accout if user log out but no
data/history migration needded"*. That choice is not re-argued here.

It splits a question the register had conflated. `DEC-026` asked one thing — *what happens to an
anonymous profile with coins* — and the answer is two different things depending on which account
the device ends up as:

- **Case A, the claim.** The device's own profile becomes an account.
- **Case B, a sign-in to somebody else's account.** The device's profile stays where it is and the
  device presents a different identity for as long as it holds a session.

The forces that make this a real decision, rather than a transcription:

**The word "migrate" points at the wrong implementation.** Case A reads like data movement, and the
naive reading of it — create a `player` row for the new account, copy the `duel_result` rows across —
compiles, passes a test its own author would write, and is wrong. `duel_result`'s primary key is
`(duel_id, player_id)`, so a copy is *permitted* by the schema; and the opponent's history is a
self-join on `duel_result` that finds *every other* row of the same duel. A copied row makes the
opponent's single duel appear twice, against two players, one of which never sat down. Row identity
is load-bearing, and the request that sounds most like "move the data" is the one that must move
none.

**`ADR-0014`'s balance is a ledger, and the register said so.** A balance is `wins − losses`, signed
and unclamped; `ADR-0015` gives a draw two rows of zero. Every duel therefore contributes `+1` and
`−1`, or `0` and `0`, and the whole database sums to zero. The register's own framing of `DEC-026`
was that *every answer except "refuse" either mints coins that no duel paid for or destroys coins a
duel did*. The human's answer is the one that does neither, because it swaps which identity a
connection presents rather than moving anything between profiles — but that property is only real if
the implementation never writes to `player` on an identity operation, and nothing yet says it must
not.

**`ADR-0027` deliberately stopped one inch short of here.** It fixed precedence — a session outranks
a device id, never falls back to it, and *"when the session's player is not the device's player,
nothing moves"* — and then recorded that reconciliation was `DEC-026`'s and not its business. So the
mechanism for Case B exists and its meaning does not. In particular `ADR-0027` never defined
sign-out, and sign-out is where Case B either works or quietly loses a profile.

**The device id is `NOT NULL UNIQUE` and has no second meaning.** `player.device_id` is today's sole
credential ([`ADR-0012`](ADR-0012-device-bound-anonymous-profiles.md)). Once a session can outrank
it, the column is either *the profile this device owns* or *the identity this device is currently
using* — and it cannot be both. Every design that answers Case B by rewriting that column has to
answer what the abandoned profile's device id becomes, and the schema physically refuses the answer.

### The deadline, honestly

There is no wire freeze here and no migration to get in before the first row: nothing in this ADR
changes the schema or `PROTOCOL_VERSION`. **The deadline is `STORY-0406`.** Until it is written the
decision is free; the moment a copy-based claim runs against real data, the original row and the
copy are indistinguishable in `duel_result` and the repair is archaeology against an opponent's
history that has already been served. Deciding now costs a document; deciding after costs a data fix
nobody can verify.

## Decision

**Nothing that changes who a device is may write to `player`.** Everything below is that sentence
applied twice.

### 1. Case A — the claim is one `INSERT`, and it is the only account-creating endpoint

Claiming attaches a credential row to the `player` row this request already resolves to:

```sql
INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)
```

That is the whole of it. **No `player` row is created. No `duel_result` row is written, moved,
copied or deleted. `player.device_id`, `player.coin_balance`, `player.display_name` and
`player.created_at` are untouched.** The profile the device already owns *becomes* the account by
gaining a credential; `wins − losses` is unchanged because the same `player.id` still owns exactly
the same `duel_result` rows. **The correct implementation of the human's "migrate balance and
history" is that no row migrates at all** — the balance and the history were never anywhere else.

- **`player_id` comes from `ADR-0027`'s `IdentityResolver`, never from the request body.** A body
  carrying a player id is a client asserting who it is, which
  [`ADR-0002`](ADR-0002-server-authoritative.md) forbids and `EPIC-04` lists as a defect wearing a
  hat. The endpoint reads identity exactly as `GET /api/me` does.
- **There is one endpoint, `POST /api/auth/sign-up`, and there is no `POST /api/auth/claim`.**
  `ADR-0027` listed both; the human's answer collapses them, because on a device that has a profile
  — which is every device that has ever connected — *creating an account* and *claiming this
  profile* are the same operation, and the only difference a separate `/sign-up` could express is
  the second `player` row this ADR exists to forbid. A second endpoint would be exactly the place
  someone writes `INSERT INTO player`.
- **Sign-up creates no profile.** It requires one: the request resolves to a player by device id or
  session, or it answers `401` with an empty body and writes nothing. `ADR-0012`'s rule that HTTP
  never mints a `player` row — *"a crawler hitting this endpoint mints no rows"* — is preserved
  without an exception. The cost is a client ordering constraint, named in *Consequences*.
- **If the resolved player already holds a credential of the same `kind`, the endpoint answers
  `409 Conflict` and writes nothing.** This is deliberately the conservative direction: whether one
  player may hold several credentials is `DEC-027`'s, and loosening a refusal later is additive
  while retracting a permission is not. It is a guard, not a rule about what an account is.
- **A claimed profile keeps its display name.** `ADR-0029`'s permanence trigger fires only on
  statements naming `display_name`; the claim names no column of `player` at all, so it cannot fire.
- **The `INSERT` is the whole transaction**, so a failed sign-up leaves nothing behind. There is
  nothing to roll back but itself.

**The failure mode, stated so a reviewer can name it.** An implementation that creates a second
`player` row for the account and copies the claimer's `duel_result` rows onto it satisfies a naive
reading of "migrate" and is wrong. Concretely, against
[`PostgresProfileReads`](../../poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt)'s
`RECENT_DUELS_SQL`, which finds the opponent as `o.player_id <> r.player_id` within the same duel:
a duel with three result rows returns **two rows to the opponent for one duel**, one of them naming
a player they never faced, and returns the claimer their own abandoned profile as an opponent. The
`UPDATE`-in-place variant — repointing every `duel_result.player_id` at a new row — avoids the
double-count and still leaves an abandoned `player` whose `coin_balance` no longer matches any
result rows, which breaks the property in §5. Neither buys anything: the credential can simply point
at the row that already has the history.

### 2. Case B — signing in writes nothing to `player`, and the anonymous profile is simply left alone

A device holding anonymous profile `P` signs into a different existing account `Q`:

1. `POST /api/auth/sign-in` verifies the credential and inserts one `auth_session` row with
   `player_id = Q`. **That is the only row written anywhere.**
2. From then on the client presents the token, and `ADR-0027`'s precedence makes the connection `Q`.
   The device id travelling alongside is ignored and not even validated.
3. `P` is untouched: same `id`, same `device_id`, same `coin_balance`, same `display_name`, same
   `duel_result` rows. It is not merged, not relinked, not tombstoned and not deleted. Nothing
   records that it is "parked", because nothing needs to.

**"Keep the anonymous profile" means precisely: `player.device_id` is never rewritten by any identity
operation.** The column keeps the one meaning it has always had — *the device that owns this
profile* — and never acquires the second one, *the identity this device is currently using*. That
second fact lives entirely in the `auth_session` row and in the token the client holds; it is
resolved at read time by `IdentityResolver` and stored nowhere else. There is one device→profile
edge, it is permanent, and a session is a higher-precedence edge layered over it rather than a
replacement for it.

Three reasons sign-in writes nothing to `player`, in increasing order of importance:

- **There is nothing to write.** The device→profile edge is already correct. A "currently signed in
  as" column would duplicate a fact `auth_session` already holds, and two copies of a fact can
  disagree — here, in the direction where a device silently plays as the wrong player.
- **The schema refuses the alternative.** `device_id` is `NOT NULL UNIQUE`
  ([`V1__initial_schema.sql`](../../poker-server/src/main/resources/db/migration/V1__initial_schema.sql)),
  so repointing `D` at `Q` requires first giving `P` some other device id — an invented one, for a
  profile the human asked to keep reachable. The constraint is doing its job.
- **It keeps the coin invariant structural.** `player` is the row that holds the balance. An
  identity operation that issues any `UPDATE player` is one careless `SET` away from the ledger, and
  then "no identity operation moves a coin" becomes a claim each endpoint must be trusted to honour
  rather than a fact about which statements exist. After `EPIC-04` there are exactly three
  statements that write `player`: `PlayerDirectory.resolve`'s upsert, `ADR-0029`'s
  `SET display_name`, and
  [`PostgresDuelResultStore`](../../poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt)'s
  `SET coin_balance = coin_balance + ?`. **Sign-up, sign-in and sign-out add none.** (`resolve`'s
  `ON CONFLICT DO UPDATE SET device_id = EXCLUDED.device_id` assigns the column to the value it
  already holds, purely to make `RETURNING id` fire on the conflict path. It is not a rewrite, and
  it is the only place in the codebase where `device_id` appears in a `SET` at all.)

A device that has never had a profile and signs in — `STORY-0407`, recovery — **is issued no device
id and gets no `player` row.** `ADR-0027` path 1 short-circuits before the minting in path 3, so a
recovery sign-in on a fresh browser litters no orphan profile, and `Welcome.deviceId` is null
because this connection's identity did not come from a device id. On sign-out that device has no
anonymous profile to return to and becomes a first-time visitor, which is correct: sign-out restores
whatever the device had, and that may be nothing.

### 3. Sign-out is a deletion, and the device id resurfacing *is* the restore

`POST /api/auth/sign-out`, with `Authorization: Bearer <token>`:

- `DELETE FROM auth_session WHERE token_hash = ?`. **`204 No Content` whether or not a row was
  deleted** — an already-expired or already-deleted token is not an error, and a `404` would tell a
  caller which tokens exist.
- It writes nothing to `player`, `credential`, `duel` or `duel_result`.
- The client discards the token and **keeps its device id**.

There is no restore step, and that is the point. The next request or `Hello` carries only the device
id, `ADR-0027` path 2 resolves it exactly as it did before the sign-in ever happened, and the
anonymous profile is back with its coins, its history and its name. **The mechanism the human asked
for is subtraction: remove the higher-precedence edge and the lower one is simply visible again.**
No parking table, no restore endpoint, no reconciliation, no sweep. Sign-out is therefore idempotent
and total — it always returns the device to the one profile its device id names, or to none — and it
cannot fail halfway, because it is one `DELETE`.

**Sign-out does not close live sockets.** `ADR-0027` fixes a connection's identity at `Hello` for the
life of the socket, and a socket opened as `Q` stays `Q` until it closes. Tearing one down because a
token was deleted would abandon a seat mid-duel, and `ADR-0013`'s grace period plus
[`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) would then fold it — **an
authentication operation would have cost somebody a coin**, which is the exact class of thing this
ADR exists to prevent. The client closes its own socket and reconnects; the server revokes future
authentications only. The cost is named in *Consequences*.

### 4. The read path follows the resolved player, not the device

`ADR-0027` left this implicit and Case B makes it mandatory.
[`ProfileReads.profileOf`](../../poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt)
takes a `DeviceId` today and its implementation selects `WHERE device_id = ?`. Under Case B the
session's player `Q` has a different `device_id` — possibly another device's entirely — so a
device-keyed `GET /api/me` would answer with `P` while the socket plays as `Q`. Two identities, one
screen, and the balance shown would be the wrong one.

**`profileOf` takes a `PlayerId`.** Every route resolves identity through `IdentityResolver` first
and passes the resolved player down; the *unknown device id → `401`* rule moves up into the
resolver, where it already lives for the socket. `recentDuelsOf` is already player-keyed and does not
change. `ProfileReads` gains no function that takes a device id, so no read path can key on a device
again by accident.

### 5. The coin invariant, stated as a property and asserted as one

**No claim, sign-in, sign-out or rename mints, destroys or clamps a coin.** Written as two
properties over the whole schema, true at every committed state:

- **P1, per player.** `player.coin_balance = COALESCE(SUM(duel_result.coin_delta) WHERE player_id =
  player.id, 0)`, for every row of `player`.
- **P2, globally.** `SUM(duel_result.coin_delta) = 0` and `SUM(player.coin_balance) = 0` across the
  whole table. Every duel writes `+1`/`−1` or `0`/`0` (`ADR-0014`, `ADR-0015`), all four rows in one
  transaction, so the ledger sums to zero at every commit boundary and a mint or a burn anywhere
  shows up as a non-zero total.

```sql
-- P1: must return zero rows
SELECT p.id FROM player p
LEFT JOIN duel_result r ON r.player_id = p.id
GROUP BY p.id, p.coin_balance
HAVING p.coin_balance <> COALESCE(SUM(r.coin_delta), 0);

-- P2: both must be 0
SELECT (SELECT COALESCE(SUM(coin_balance), 0) FROM player),
       (SELECT COALESCE(SUM(coin_delta), 0) FROM duel_result);
```

**How it is tested — across the flow, not per endpoint.** One test-fixture helper asserts P1 and P2
against the live schema, and a single scenario test drives the whole identity flow against a real
database, calling it **after every step**: connect anonymously, play a duel and win, set a name, sign
up, reconnect with the token, sign into a *second* account from the same device, play a duel as that
account, sign out, reconnect anonymously, read the profile back. Asserting only at the end is not
enough — a mint and a burn cancel. The same test snapshots every `player` row before and after each
identity operation and asserts the multiset is byte-identical, with the single exception of the one
row and one column a rename is permitted to touch.

**Why this shape rather than an assertion per endpoint.** A per-endpoint test asserts what its author
remembered to think about; P1 and P2 are total over the schema, so an endpoint added in a year — by
someone who never read this ADR — trips them without anyone having updated a test. It is the same
argument `ADR-0029` used for putting permanence in a trigger: the guarantee should not depend on
every future write path remembering it. `EPIC-04`'s definition of done already requires *"a claim
leaves the balance byte-identical"*; this is that requirement made checkable, and widened to Case B,
which the definition of done does not currently cover.

### 6. What this makes legal, spelled out

A device holds anonymous profile `P`, signs into account `X`, plays duels as `X`, signs out, and is
`P` again. **This is coherent and nothing forbids it.** Every duel played as `X` wrote `duel_result`
rows for `X`; `P` gained and lost nothing; the opponent's history names `X`, who is who actually sat
down. P1 and P2 hold throughout because no identity operation touched a balance.

Two things constrain it, and both are already true:

- **A player cannot be two identities at once.** While a token is presented the connection is `X`,
  and `P` earns nothing and is not reachable. There is no state in which both apply, because
  precedence is a total order over one request.
- **Signing out mid-duel abandons the seat.** The socket is `X`'s; reconnecting as `P` resolves to a
  different player, `RoomRegistry.resume` answers `null`, and the ordinary join refuses a full
  `PLAYING` room. `X`'s seat goes absent and may lose the duel to a grace-period fold. That is a coin
  lost **the ordinary way** — a fold the engine adjudicated — not a coin destroyed by an identity
  operation, and P1 and P2 hold across it. It is worth a client warning and it is not a server rule.

### 7. What is deliberately not built

- **No merge, in either direction.** There is no endpoint, no transaction and no plan that combines
  two profiles' balances or histories.
- **No parking table**, no `device_session`, no column recording which account a device is currently
  using. The client's token is that state.
- **No detach-this-device operation.** `player.device_id` is never rewritten and never cleared, so a
  device that once owned a profile keeps password-free access to it after the claim. See *What this
  does not settle*.
- **`poker-engine` learns nothing.** No credential, session, account or profile type exists in it or
  crosses into it. No wire type changes and `PROTOCOL_VERSION` does not move: sign-up, sign-in and
  sign-out are plain HTTP, contracted in [`docs/protocol.md`](../protocol.md), and the socket's
  `Hello`/`Welcome` shape is `ADR-0027`'s, unchanged.
- **No migration.** No new table, no new column, no new constraint, no new index. `credential` and
  `auth_session` are `ADR-0027`'s and are unaffected by this decision.

### 8. The client's half, because the mechanism depends on it

- **The stored device id is write-once.** It is set from the first `Welcome` that carries one, and is
  then **never cleared and never overwritten** — not on sign-in, not on sign-out, not when a
  `Welcome` arrives with a null `deviceId`. A client that re-mints one abandons a profile, which is
  the precise harm `ADR-0012` named and `ADR-0027` §5 spent a wire version to prevent.
- **The client keeps sending its device id whether or not it holds a token.** The server ignores it
  under a session (`ADR-0027` §1), so the alternative buys nothing and costs a conditional whose bug
  mode is the abandonment above.
- **Sign-out clears the token and only the token.**

## Consequences

**What it buys.** `STORY-0406` and `STORY-0407` unblock, and `STORY-0406` turns out to be one
`INSERT`, one `409` guard and a test — the smallest story in the credential chain rather than the
transaction over `duel_result` the register anticipated. Coin conservation stops being a promise each
endpoint makes and becomes a property of which statements exist. Case B needs no code at all beyond
sign-out: `ADR-0027`'s precedence already implements it, and this ADR mostly forbids the code someone
would otherwise add. `EPIC-04`'s definition-of-done test — claim here, recover there, same balance —
passes by construction, because there is no step at which a balance could differ.

**What it costs.**

- **A claim does not revoke the claiming device's password-free access.** After the claim the account
  is reachable two ways: by credential, and by the original device id, which is a bearer secret in
  web storage that nothing here can rotate or revoke. Somebody who once used that browser — a shared
  machine, a sold laptop whose site data survives — keeps access to the account forever. This is not
  a regression (`ADR-0012` already makes the device id the sole credential) but the claim is the
  moment a player starts believing their account is protected by a password, and it is not only. The
  technical remedy stays cheap and is not built; whether the product wants it is not mine, and is
  registered below.
- **A parked anonymous profile is exactly as fragile as it always was.** Nothing "keeps" `P` beyond
  its device id. A player who signs into another account and later clears their browser loses the
  anonymous coins permanently — the account survives via its credential, the profile does not. The
  human asked for the profile to be kept, and it is kept to the exact strength `ADR-0012` provides,
  which is not much.
- **Sign-out revokes future authentications, not the current socket.** A stolen token that was signed
  out remains effective on an already-open socket until it closes. Small — the socket already exists
  and the token is not re-verified on it — and deliberate, because the alternative can move a coin.
- **A client cannot sign up before it has a device id**, since sign-up creates no profile. Today the
  client obtains one from its first `Welcome`. If `EPIC-04` builds an account screen reachable before
  any socket has connected, the remedy is the client obtaining a device id first, **not** a new
  server path that mints `player` rows over HTTP.
- **An abandoned-profile tail.** Every device that ever signs into somebody else's account leaves a
  `player` row nobody will use again, and unlike `ADR-0012`'s stranded profiles these are created
  deliberately by a supported flow. They are rows, they are small, and they are input to `DEC-029`.
- **A signature change on a shipped port.** `ProfileReads.profileOf` moves from `DeviceId` to
  `PlayerId`, touching `ProfileRoutes`, `PostgresProfileReads` and their tests. Cheap now, and it is
  the change that stops `GET /api/me` answering as the wrong player.
- **Two identities on one device is a support conversation waiting to happen.** "My coins are gone"
  will sometimes mean "you are signed into your other account", and the only cure is the client
  showing plainly which identity is active. That is `STORY-0412`'s work and this ADR does not do it.

**What it forecloses.**

- **A merge, in practice, forever.** If the human ever wants an anonymous profile's coins to follow a
  player into an existing account, the only two implementations are the two rejected here: copy the
  `duel_result` rows (double-counting in the opponent's history) or repoint them (rewriting the
  opponent's history to name a player they never faced, under a display name that was never at that
  table). A merge is not a feature that gets added later; it is a rewrite of history that has already
  been served, and this ADR is a good place to have said so.
- **A device id that means "current identity".** The column is fixed to one meaning. Any future
  design wanting a device to *belong* to an account needs a new table, not this column.
- **Signing out of a live duel gracefully.** Identity is fixed at `Hello`; there is no mid-socket
  identity change and this ADR does not open one.

It **does not** foreclose:

- **`DEC-025`** (is an account ever required). Requiring one is a refusal layered above
  `IdentityResolver`, exactly as `ADR-0027` recorded. Nothing here makes anonymous play more or less
  permanent; it makes the anonymous profile *survivable*, which is orthogonal.
- **`DEC-029`** (deletion). No cascade is added, no row is hard-deleted, and the two profiles a
  Case-B device leaves behind are ordinary independent rows. Deleting account `Q` leaves `P`
  untouched, and vice versa. Whoever answers `DEC-029` now has one more input: a device may be
  associated with two profiles, and "delete everything about me" is ambiguous until they say which.
- **The moderation half of `DEC-017`.** No name is set, moved, released or reused by anything here. A
  device that signs into another account does not release its anonymous profile's permanent name, so
  the namespace never churns.

## What this does not settle

**One new question, and it is the human's.** Phrased so it can be answered in one sentence: *after a
player attaches a credential to their profile, may the device id that created that profile still
sign in without the credential — forever, until the player revokes it, or not at all?* This is a
risk-acceptance question about what a player is entitled to believe an account protects, not a
question two engineers would answer identically, so it is not answered here. The technical shapes are
all cheap and all stay available: leave it (today), a `POST /api/me/devices/forget` that rewrites
`player.device_id` to a fresh unguessable value, or an automatic rewrite at claim time. Recommended
bookkeeping: register it as the next free `DEC-NNN` — `DEC-030` if that number is still free when
this lands — against `EPIC-04`, due before the account screens ship. **The register row is the
driver's to write; this ADR does not edit the register.**

Also still open and untouched: `DEC-025`, `DEC-027` (what a credential contains — this ADR names
`credential` columns but decides nothing about their values), `DEC-029`, and the moderation half of
`DEC-017`.

## Alternatives considered

**Case A as a real migration: a new `player` row for the account, `duel_result` rows copied across.**
Its strongest case is conceptual, and it is not silly: it makes *account* and *device profile* two
distinct things, which is what most systems mean by those words; it would let a device keep its
anonymous profile *and* have an account, which composes neatly with Case B; and it reads like a
literal implementation of what the human asked for. Rejected on row identity. `duel_result`'s primary
key is `(duel_id, player_id)`, so the copy is legal and the opponent's self-join then returns their
one duel twice, naming a player who never sat down. Worse, it is a defect nobody sees from the
claiming side — the claimer's own history looks perfect — so it would be discovered by an opponent,
later, in production, and the repair would need to distinguish an original row from a copy that no
longer differs from it.

**Case A by moving rows: `UPDATE duel_result SET player_id = <new> WHERE player_id = <old>`.** Better
than copying — no double count, and the opponent's join keeps returning one row. Its case is that it
leaves a clean, single, purpose-built account row and lets the abandoned device profile be reset to
zero. Rejected because it does real work under a lock over a player's whole history to reach a state
identical to doing nothing, and because it leaves an abandoned `player` row whose `coin_balance` must
be separately zeroed or P1 breaks — an identity operation issuing an `UPDATE` against the ledger
column, which is precisely the shape §2 exists to keep out of the codebase.

**Case B by rebinding the device: `UPDATE player SET device_id = <D> WHERE id = <Q>`.** Its strongest
case is genuinely attractive: every existing read path keeps working untouched, `GET /api/me` needs
no change, `ProfileReads.profileOf` keeps its `DeviceId` signature, and there is arguably no need for
an `IdentityResolver` on the HTTP side at all. Rejected three times over. `device_id` is
`NOT NULL UNIQUE`, so `P` must be given an invented device id — a profile the human asked to keep,
made reachable only by a value nobody holds. It writes to the ledger row on sign-in. And it makes
sign-out a *restore* operation that can fail halfway, where the chosen design makes it a single
`DELETE` that cannot.

**Case B with a parking table** — `device_session(device_id, active_player_id)`, or a nullable
`player.parked_from_device` — recording server-side which account a device is currently using. Its
strongest case is real: the server would know the association, which enables "resume where you left
off" after a browser restart without a token, and makes sign-out an explicit, auditable restore
rather than an emergent one. Rejected because it stores a fact the client's token already carries, in
a second place, where the two can disagree — and the disagreement is silent and plays duels as the
wrong player. It also needs a migration, a lifecycle and a sweep, to serve a read nobody has asked
for.

**Refuse the sign-in while the device holds a profile with history** — the register's third option,
and the only one it conceded was conservation-safe. Its case is the strongest of the rejected set:
the conservation question becomes moot, nobody is ever surprised, and there is no second identity to
explain in a support conversation. Rejected because the human chose otherwise, and because it breaks
`STORY-0407` on the devices that matter most: recovery *from a device that has already played* is the
common case, not the exotic one, and a shared or second-hand browser would be permanently unable to
sign in.

**Delete the anonymous profile on sign-in.** Tidy: no orphan rows, no retention tail, no ambiguity
about which identity a device "really" is. Rejected on the invariant — deleting a `player` with
`duel_result` rows is refused by the foreign key, and deleting the rows too would destroy coins a
duel paid for, break P2, and silently rewrite an opponent's history. The human also said keep.

**Sign-out closes every socket the player holds.** Its case is proper revocation: after sign-out the
token is effective nowhere, which is what "sign out" means to most people. Rejected because a socket
closed mid-duel abandons a seat, and `ADR-0013`'s grace period turns that into a fold and a lost
coin. An authentication operation that can cost a coin is exactly what this ADR is written to
prevent, and the residual exposure — a signed-out token remaining effective on a socket that is
already open — is bounded by that socket's lifetime.
