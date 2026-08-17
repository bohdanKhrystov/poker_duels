# ADR-0049 — A device binding is its own row, and revoking one is final

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-041` — the shape revocation takes in the schema, which
  [`ADR-0037`](ADR-0037-the-device-is-a-credential-until-revoked.md) deliberately left to
  `STORY-0406`
- **Amends:** [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §2 (the column
  it protects moves out of `player`), §7 (*"no detach-this-device operation"*, already overturned by
  `ADR-0037`, and *"no migration"*, which this one adds). Its coin invariant and its precedence rules
  are untouched, its permitted writers of `player` **stay three** — the first of them losing its
  ability to `SET` anything — and §2's guarantee comes out stronger than it went in; see §7 below
- **Constrains:** `STORY-0406` (the revoke path and the schema property test), `STORY-0412` (the
  account screens), `STORY-0414` (the end-to-end proof)
- **Adds one migration**, taking the next free `V<n>` — `V5` unless something else has landed one
- **Raises:** `DEC-045`, the product owner's — see *What this does not settle*. **Since answered by
  [`ADR-0050`](ADR-0050-revoking-the-device-signs-the-player-out-everywhere-but-here.md), which
  amends this ADR on one point:** §6's *"revocation writes nothing to `auth_session`"* no longer
  holds — the endpoint gains one `DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?`
  in the same transaction, so `auth_session` leaves §2's "writes nothing to" list. `player`,
  `credential`, `duel` and `duel_result` stay on it, and the byte-identical-`player` argument is
  unaffected
- **No wire change.** `PROTOCOL_VERSION` does not move; revocation is plain HTTP

## Context

`ADR-0037` settled that a player may revoke the device's standing as a credential, that revoking
does not end the revoking session, and that revocation is offered only once a credential exists. It
then said, in as many words, that the schema shape is *"a technical question with more than one
defensible answer and no reason to guess it here."* This is that question.

The forces, and they genuinely pull against each other:

**`ADR-0030` §2 spent its argument on this exact column.** *"`player.device_id` is never rewritten
by any identity operation"* is not a style preference — it is the reason the coin invariant is
structural rather than promised. `player` is the row that holds the balance, so §2 enumerates the
statements that write it (`resolve`'s upsert, `SET display_name`, `SET coin_balance`) and observes
that an identity operation issuing any `UPDATE player` is one careless `SET` away from the ledger.
Every shape that nulls the column, or adds a `revoked_at` beside it, adds a fourth writer to the
ledger row and converts a fact about which statements exist into a promise each endpoint makes.
`STORY-0406`'s acceptance criteria already say *"the `player` table is byte-identical across
sign-up, sign-in, sign-out and revocation"*, so the story is written against a shape that does not
write `player` at all.

**The schema physically refuses the obvious answers.** `player.device_id` is `NOT NULL UNIQUE`
([`V1__initial_schema.sql`](../../poker-server/src/main/resources/db/migration/V1__initial_schema.sql)).
`ADR-0037` records as a consequence that *"a revoked device that later returns is an anonymous
device with no profile — it mints a fresh one under `ADR-0012` rather than failing"*. Both cannot
hold while the revoked binding still occupies that unique value: either the device id string is
burned server-side forever — the resold phone that can never play again — or the same string has to
be able to name a dead binding and a live one at the same time. That is the whole of the problem,
and it is a uniqueness problem, not a nullability one.

**Revocation must not be undoable by one statement.** `ADR-0037`'s value is that *"a cautious player
can reach the strong guarantee"*. A guarantee that a single later `INSERT` or `UPDATE` restores is
weaker than one that needs a migration, and the difference is invisible in code review a year from
now. Whatever shape is chosen, the property that matters is **monotonicity**, and the codebase
already has a precedent for enforcing exactly that class of property in the database rather than in
Kotlin: [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §4's permanence trigger,
whose reasoning — *"this invariant is defined by the impossibility of that `UPDATE`"* — transfers
without modification.

**Nothing records how a session was obtained, deliberately.** `ADR-0027` §2 declined `ip`,
`user_agent` and `last_used_at` because they exist to power a devices screen nobody asked for. So
"end the sessions this device binding established" is not a query the schema can answer — and it
turns out not to be a question either: `STORY-0404` fixes that sign-up issues **no** session, so
every row in `auth_session` was minted by `POST /api/auth/sign-in` proving a password. There are no
sessions the device binding established.

**The ordering is favourable exactly once.** `STORY-0405` moves `ProfileReads.profileOf` from
`DeviceId` to `PlayerId` (`ADR-0030` §4), which removes the last read of `player.device_id` outside
[`PostgresPlayerDirectory.resolve`](../../poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt).
In `STORY-0406`, one production statement reads that column. Before `STORY-0405` it was two, and
after `EPIC-07` hosts anything it is a coordinated deploy.

### The deadline, honestly

**Dropping a column is free while no deployment exists and expensive afterwards.** `EPIC-07` has
hosted nothing, so the migration below is a backfill and a `DROP COLUMN` inside one transaction
against a database whose only rows are local and test-container ones. Once a server is running, the
same change is add-table → dual-write → backfill → drop, across two deploys, because the old
process selects a column the new schema no longer has. This is a reason to take the shape now rather
than a reason to prefer it: the cheap-today alternatives stay equally cheap forever, and this one is
the only option whose price rises. It is also the argument against deferring the *shape* while
shipping the *feature* — a revocation flag added to `player` now becomes a second migration and a
data move later, at the price above.

## Decision

**The device→profile edge moves out of `player` into its own table, revocation marks that row
rather than removing it, and a revoked binding can never come back.**

### 1. `device_binding` owns the edge; `player` loses the column

```sql
CREATE TABLE device_binding (
    device_id  TEXT        NOT NULL,
    player_id  UUID        NOT NULL REFERENCES player (id),
    bound_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT device_binding_pkey PRIMARY KEY (device_id, player_id)
);

CREATE UNIQUE INDEX device_binding_live_device
    ON device_binding (device_id) WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX device_binding_live_player
    ON device_binding (player_id) WHERE revoked_at IS NULL;
```

- **The primary key is the natural pair, not a surrogate id.** `(device_id, player_id)` is what makes
  a device returning to a profile it has already revoked a primary-key violation rather than a rule
  somebody has to remember. A surrogate `id UUID` would buy nothing and would drop that guarantee.
- **`device_binding_live_device` is `ADR-0012`'s one-profile-per-device rule**, narrowed to live
  bindings. It replaces `player_device_id_unique` exactly, and it is still the database — not
  application locking — that resolves a race between two first contacts.
- **`device_binding_live_player` fixes one live binding per player.** A player holding several
  devices at once is a feature nobody has asked for, and it would let a player re-widen the surface
  revocation exists to narrow. Restricting now is reversible (drop an index); permitting now and
  restricting later means deleting rows somebody is using.
- **The two indexes that enforce the invariants are the two indexes the two hot reads need**:
  `WHERE device_id = ? AND revoked_at IS NULL` on every anonymous connection, and
  `WHERE player_id = ? AND revoked_at IS NULL` for the account screen. No index exists here that is
  not also a constraint.
- **Liveness is `revoked_at IS NULL`, never a timestamp comparison.** Nothing compares these columns
  to a clock, which is why they may be database `now()` and why no `ServerClock` is threaded into
  this table. `bound_at` and `revoked_at` are there to be *read by a human*, not by a predicate.
- **No `ON DELETE` clause and no `CHECK`**, matching `V4`'s two deliberate omissions. Whether a
  player row is ever deleted is `ADR-0039`'s *not in v0.1*, and the default `NO ACTION` forces a
  deletion feature to state out loud what happens to bindings instead of cascading silently.
- **`player.device_id` and `player_device_id_unique` are dropped.** They are not kept "for history":
  a second copy of the edge is a fact stored twice that can disagree, which is the reason `ADR-0030`
  rejected the parking table, and an unmaintained copy is worse than none.
- **The Kotlin `Player(PlayerId, DeviceId)` and `PlayerDirectory.resolve(DeviceId)` signatures do not
  change.** This is a change of where the edge is stored, not of what the server's identity ports
  look like.

### 2. Revocation is one `UPDATE` against `device_binding`, and a trigger makes it final

```sql
UPDATE device_binding SET revoked_at = now()
WHERE player_id = ? AND revoked_at IS NULL;
```

```sql
CREATE FUNCTION device_binding_revocation_is_final() RETURNS trigger AS $$
BEGIN
    IF OLD.revoked_at IS NOT NULL AND NEW.revoked_at IS DISTINCT FROM OLD.revoked_at THEN
        RAISE EXCEPTION 'a revoked device binding is final (ADR-0049)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER device_binding_revocation_final
    BEFORE UPDATE OF revoked_at ON device_binding
    FOR EACH ROW EXECUTE FUNCTION device_binding_revocation_is_final();
```

- **`NULL → a timestamp` succeeds once. `timestamp → NULL` raises. `timestamp → a different
  timestamp` raises.** There is no un-revoking, from the endpoint, from a future admin path
  (`DEC-042` is about to produce one), or from a `psql` session.
- **`OF revoked_at` means the trigger fires only on statements naming the column**, exactly as
  `ADR-0029` §4's does. `resolve` inserts and never updates, so the identity hot path pays nothing.
- **The error code is `restrict_violation` (`23001`)**, matched on the code and never on the message,
  as `ADR-0029` §4 established.
- **The statement names the player, never a device.** The caller cannot say which binding to revoke,
  because there is at most one live binding per player and the client asserts no fact
  ([`ADR-0002`](ADR-0002-server-authoritative.md)). A request body that named a device id would be
  the shape in which one player revokes another's.
- **This writes nothing to `player`, `credential`, `auth_session`, `duel` or `duel_result`.** It is
  the whole transaction.

### 3. A revoked device may bind again — only to a profile that does not yet exist

- **Never back to the profile it left.** The pair is already in the primary key with a `revoked_at`
  the trigger will not clear, so the re-binding `INSERT` is refused by the database.
- **Never to any other pre-existing profile.** `INSERT INTO device_binding` appears in exactly one
  place in the codebase — inside `resolve`'s mint (§4), where it is written in the same transaction
  as the `INSERT INTO player` that created the row it names. There is no statement anywhere that
  binds a device to a `player.id` that already existed, and there is no endpoint that takes a device
  id as input at all.
- **To a fresh profile, yes**, and that is `ADR-0037`'s recorded consequence working: a revoked
  device that returns anonymously is a device with no live binding, so `resolve` mints a new,
  empty profile and binds it. A shared or resold phone keeps playing; it simply cannot play as the
  account that dismissed it.
- **The client keeps its stored device id.** `ADR-0030` §8's write-once rule is unchanged, because
  the server's answer to that id changes and the client's behaviour need not. Nothing here depends
  on a client forgetting anything — a client that presents a revoked device id forever is refused
  the profile forever, which is what `ADR-0002` requires of a guarantee.

### 4. `resolve` becomes a read and, on a miss, a mint that rolls back whole

```sql
-- 1. the common case: one indexed read.
SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL;

-- 2. no live binding: mint, in an explicit transaction.
WITH minted AS (
    INSERT INTO player (id) VALUES (?) RETURNING id
)
INSERT INTO device_binding (device_id, player_id)
SELECT ?, id FROM minted
ON CONFLICT (device_id) WHERE revoked_at IS NULL DO NOTHING
RETURNING player_id;
```

**If statement 2 returns no row, the transaction is rolled back and step 1 is re-run.** This is not
optional and it is the defect this section exists to prevent: `ON CONFLICT ... DO NOTHING` is a
success, not a failure, so under autocommit the CTE's `INSERT INTO player` commits while the binding
does not — leaving an orphan profile with no device, created by a race, on every contended first
contact. The rollback is what makes the mint atomic; the re-read then returns the winner's player,
because a conflict is only observed after the concurrent inserter has committed or aborted.

**`PostgresPlayerDirectoryTest`'s existing 16-simultaneous-first-contacts test is the proof, and it
must be extended to assert the row count**: sixteen concurrent `resolve` calls for one device id
yield one `player` row and one `device_binding` row, not one binding and sixteen profiles. The
current test asserts a single player id and would pass while littering.

### 5. `DELETE /api/me/device`, with a session and a credential

- **Identity comes from `IdentityResolver`; the request has no body.**
- **A session token is required.** Device-authenticated callers get `401`. This is the only way to
  honour `ADR-0037`'s rule that revocation *"does not kill the revoking session"* — if the caller
  holds no session, there is nothing to keep alive, and the operation would sign them out of the
  screen they are standing on, which is the hostility that rule forbids. It also makes revocation a
  step-up operation for free: the password was proved minutes ago, by construction.
- **`409 Conflict`, empty body, writes nothing, when the player holds no credential.** `ADR-0037`
  offers revocation only when another route exists, and `STORY-0406` requires that to be a server
  rule rather than a hidden button. The two guards are different: the token requirement is about not
  stranding the caller's *screen*, the credential check is about not stranding the *profile*.
- **`204 No Content` otherwise, whether or not a row was updated.** Already revoked, or never bound
  (`ADR-0030` §2's recovery-on-a-fresh-browser case, which mints no binding at all), answers exactly
  as a successful revocation does. A distinct answer would tell a caller which bindings exist, and
  sign-out already sets this precedent.
- **The route is `/api/me/*`, not `/api/auth/*`.** The `auth` family is verbs about a session —
  sign-up, sign-in, sign-out — and the point of this decision is that revocation is not one. It acts
  on the caller's own profile, beside `GET /api/me` and `PUT /api/me/name`.
- **`ProfileResponse` gains `deviceRouteLive: Boolean`**, from
  `EXISTS (SELECT 1 FROM device_binding WHERE player_id = ? AND revoked_at IS NULL)`, so
  `STORY-0412` can satisfy `ADR-0037`'s requirement that the screens state which routes are live.
  It is a JSON field on an HTTP response, not a socket message: `PROTOCOL_VERSION` does not move.
- **No read path returns a device id to a caller that did not present one.** The new table makes a
  "your devices" listing easy to write, and a device id is a bearer credential: handing one to a
  session-holder hands over the profile. `Welcome.deviceId` stays what `ADR-0027` §3 made it —
  present exactly when this connection's identity came from a device id — and nothing else emits one.

### 6. Revocation touches no session and closes no socket, and that is not session expiry

- **Revocation writes nothing to `auth_session`.** Not the revoking row, and not the others. There
  are no sessions to reconsider: `STORY-0404` fixes that sign-up issues none, so every `auth_session`
  row was minted by `POST /api/auth/sign-in` proving a password, and `ADR-0037`'s own reasoning —
  *"the session `ADR-0027` issued is independent of how it was obtained"* — generalises from the
  revoking session to the rest of them.
- **Revocation closes no live socket**, for `ADR-0030` §3's reason exactly: a socket torn down
  mid-duel abandons a seat, `ADR-0013`'s grace period plus
  [`ADR-0023`](ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) fold it, and an identity
  operation would have cost somebody a coin. A socket already seated by the device route stays that
  player until it closes; the next `Hello` is refused.
- **This is a different question from session expiry, and the two must not be conflated.** Expiry is
  a property of a *token* — an absolute 30 days from issue, enforced at read time (`ADR-0027` §2).
  Revocation is a property of a *route*: which credentials may mint new sessions and authenticate
  new requests. Revoking closes a route instantly and shortens no token's life by a second. **The
  residual is therefore real and bounded: a session token already sitting on the revoked device
  keeps working for up to thirty days.** Whether the product wants revocation to *also* mean "sign
  out everywhere" is `DEC-045`, below; the schema is indifferent, because that would be one further
  `DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?` served by an index `ADR-0027`
  already built.

### 7. What replaces `ADR-0030` §2's structural guarantee

§2's sentence — *"`player.device_id` is never rewritten by any identity operation"* — is retired by
being made vacuous: **`player` has no device column to rewrite.** The guarantee gets stronger in the
move, and the three permitted writers of `player` do not become four:

| `ADR-0030` §2's writer | After this ADR |
| --- | --- |
| `PlayerDirectory.resolve`'s `INSERT ... ON CONFLICT DO UPDATE SET device_id` | `INSERT INTO player (id)` — a plain insert that names no device column and can no longer `SET` anything |
| `ADR-0029`'s `SET display_name` | unchanged |
| `PostgresDuelResultStore`'s `SET coin_balance = coin_balance + ?` | unchanged |
| — | **revocation adds none** |

The device-side invariant moves with the data and is enforced the same way, by counting statements:
**`device_binding` has exactly two writers** — the one `INSERT` inside `resolve`'s mint, and the one
monotone `UPDATE ... SET revoked_at` in §2 — with the trigger making the second one-way and the
primary key making the first unable to reach an existing profile. P1 and P2 (`ADR-0030` §5) are
untouched: no statement in this ADR reads or writes `coin_balance` or `duel_result`, and
`STORY-0406`'s *"byte-identical `player` table across revocation"* criterion holds by construction
rather than by inspection.

### 8. The migration

One new file, taking the next free `V<n>`; `V1`–`V4` are immutable.

```sql
CREATE TABLE device_binding (...);            -- §1, with both partial unique indexes

INSERT INTO device_binding (device_id, player_id, bound_at)
SELECT device_id, id, created_at FROM player;

ALTER TABLE player DROP CONSTRAINT player_device_id_unique;
ALTER TABLE player DROP COLUMN device_id;

CREATE FUNCTION device_binding_revocation_is_final() ...;   -- §2
CREATE TRIGGER device_binding_revocation_final ...;
```

- **Every existing row is a live binding**, because that is what `player.device_id NOT NULL` meant.
  `bound_at` is backfilled from `player.created_at` — the binding really was created with the
  profile — and `revoked_at` stays null.
- **Both partial unique indexes are satisfiable by construction**: `device_id` was `UNIQUE` and each
  player had exactly one. If a duplicate somehow existed the migration fails loudly, which is right.
- **The order matters**: create and backfill before dropping, so the whole file is one transaction
  (Flyway runs each migration in one on PostgreSQL, and DDL there is transactional) and a failure
  anywhere leaves `V4`'s schema intact. `DROP COLUMN` is a catalog operation, not a table rewrite.
- **The code lands in the same PR as the migration.** `PostgresPlayerDirectory` (§4),
  `PostgresProfileReads` (already player-keyed by `STORY-0405`), `SchemaConstraintsTest`'s assertion
  on the constraint name `player_device_id_unique`, `PostgresPlayerDirectoryTest`'s two device-keyed
  selects, and every test that writes a `player` row directly — ten
  `INSERT INTO player (id, device_id, …)` statements across seven files on `develop` today — change
  together. There is no intermediate commit at which the tree compiles and the schema matches.

### 9. What is deliberately not built

- **No un-revoke, no admin restore, no support override.** The database refuses it. The remedy for
  revoking by accident is the password, which is guaranteed to exist.
- **No second live binding per player**, and so no "link another device" flow.
- **No `ip`, no `user_agent`, no `last_used_at`, no device label.** `ADR-0027` declined them and a
  table to put them in is not a reason to reconsider.
- **No session or socket teardown**, and no revocation of anything but the device route.
- **No wire change.** `Hello`, `Welcome` and `PROTOCOL_VERSION` are untouched.
- **`poker-engine` learns nothing.** No binding, device, credential or session type exists in it or
  crosses into it.

## Consequences

**What it buys.** `STORY-0406` unblocks with a shape its acceptance criteria already assume: one
`UPDATE` against a table that is not the ledger, so the `player` multiset is byte-identical across
revocation without anyone being careful. `ADR-0030` §2's argument survives intact and gets shorter —
the column it protected no longer exists. `ADR-0037`'s "a revoked device that returns mints a fresh
profile" becomes true mechanically rather than aspirationally, so a resold phone is not bricked.
Revocation is irreversible in the database rather than by convention, which is the only version of
that promise worth putting on a screen. `STORY-0412` gets `deviceRouteLive` to render.

**What it costs.**

- **Revoking cannot be undone, by anybody, ever.** This is the point of the feature and it is
  also its largest cost, paid by the player who clicks it by mistake: the only way back is the
  password, and a player who has also forgotten that and declined `ADR-0031`'s optional recovery
  email has lost the profile. `ADR-0037` already requires the affordance to say so at the moment of
  revoking; this ADR is why that warning cannot be softened later by an admin tool.
- **Revocation is not "sign out everywhere".** A token already sitting on the revoked device stays
  valid for up to thirty days (§6). The player most likely to revoke — *I no longer control that
  machine* — is precisely the one for whom this gap matters, and closing it is `DEC-045`.
- **Revoking requires signing in first.** A player who has never held a session on this device must
  present the password before the account screen can offer the button — an extra step in the flow
  whose whole purpose is to reduce friction around security.
- **The hottest identity statement gets harder to get right.** Today's `resolve` is one atomic
  upsert; the replacement is a read plus, on a miss, a transaction that must be rolled back on a
  statement that *succeeded*. §4 names the orphan-profile defect because it is the one a competent
  implementer will ship.
- **A destructive migration against a shipped table, and a PR that cannot be split.** The column
  drop, `PostgresPlayerDirectory`, `SchemaConstraintsTest`'s constraint-name assertion and ten
  `INSERT INTO player` fixtures across seven test files move together. Nothing about that is hard;
  all of it is in one change.
- **A second row per profile, forever**, and a second insert on first contact. Small, and it is the
  path every anonymous player takes.
- **Every revocation eventually mints one more empty profile** — the revoked device's next anonymous
  connection. It joins `ADR-0030`'s abandoned-profile tail and is input to whatever answers
  retention.
- **Unbounded rows per device, in principle.** Bind → revoke → bind again accumulates rows; each
  cycle costs an account creation and an Argon2 sign-in, so `ADR-0027` §6's address-keyed budget
  meters it, and no rule caps it.
- **A table that invites the screen `ADR-0027` refused.** The row now exists, so "list my devices"
  is one `SELECT` away — and listing device ids would hand out bearer credentials. §5 forbids it in
  prose, which is weaker than the constraint-shaped guarantees elsewhere in this ADR.

**What it forecloses.**

- **A device id as a key for anything.** It is no longer unique in the database, so nothing may
  foreign-key to it, join on it, or treat it as a player's natural identifier. Anything that must
  key on a device keys on a *live* binding, through the partial index.
- **Restoring a binding without a migration.** Deliberate, and it is the difference between this
  shape and every rejected one.
- **`player` as self-contained identity.** Reading which device owns a profile is now a join, and
  a `SELECT * FROM player` no longer tells you how anyone signs in.

It does **not** foreclose multi-device accounts (drop one index and decide what the endpoint means),
"sign out everywhere" (one `DELETE`, index present), or account deletion (`ADR-0039`'s question
gains one more table to name, and the absent `ON DELETE` forces it to).

## What this does not settle

**One question, and it is the product owner's.** Phrased so it can be answered in one sentence:
*does revoking a device also end every other session that player holds — "sign out everywhere" — or
are the two separate affordances on the account screen?* It is not answered here because it is a
question about what a player is promised when they press the button, not about what the schema can
express: the schema is indifferent, both answers are one statement apart, and neither costs a
migration. It is registered as **`DEC-045`**, against `EPIC-04`, due before `STORY-0412` designs the
screen — `STORY-0406` can ship §5's endpoint under either answer, since adding the delete later is
additive and needs no new column, no new index and no new route.

The technical default this ADR takes in the meantime is **no session is touched** (§6), chosen in
the reversible direction: a `DELETE` can be added the day the product asks for one, and a session
deleted today cannot be brought back. If the product owner answers *"revoke means sign out
everywhere"*, the change is one statement on the same endpoint, inside the same transaction, keyed
by `player_id` and excluding the presented token — `ADR-0037`'s rule that the revoking session
survives holds either way.

## Alternatives considered

**Null the column: `UPDATE player SET device_id = NULL`, with `NOT NULL` dropped.** The strongest
case, and it is a good one: the smallest possible change — no new table, no join, no new file beyond
a two-line migration — and the meaning is self-evident, since PostgreSQL treats nulls as distinct in
a unique index, so any number of revoked profiles coexist and the freed string is immediately
reusable by a fresh profile. Rejected on three counts, any one sufficient. It adds a fourth writer
to `player`, the ledger row, which is exactly what `ADR-0030` §2 exists to prevent and what the
register named when it opened `DEC-041`; it breaks `STORY-0406`'s already-written criterion that the
`player` table is byte-identical across revocation; and it destroys the fact instead of recording
it, so nothing distinguishes *revoked* from *never bound* and there is no row on which to hang a
"this is final" trigger. Its cheapness is real, and it is cheapness bought against the one argument
this schema had.

**A `device_revoked_at` column on `player`, beside the binding.** Its case is better than nulling:
the column stays `NOT NULL UNIQUE`, the historical fact of which device created the profile is
preserved, `resolve` needs only a predicate, and the migration is one `ALTER TABLE`. Rejected
because it is the same fourth writer to the ledger row *and* it burns the device id string forever:
the revoked row still holds the unique value, so the same device cannot mint a fresh profile, and
`ADR-0037`'s "mints a fresh one rather than failing" fails. The only repairs are a client that
re-mints its device id — forbidden by `ADR-0030` §8, whose write-once rule exists because that path
abandons profiles — or a new `ProtocolError` and a wire bump to tell it to.

**Revocation as `DELETE FROM device_binding`, with no `revoked_at`, no partial indexes and no
trigger.** Genuinely attractive and materially smaller: a plain `UNIQUE (device_id)` suffices, there
is no tombstone to reason about, no monotonicity to enforce, and it matches the vocabulary
`ADR-0027` already uses — *"revocation is a `DELETE`"*. Rejected because a deleted row makes
*revoked* indistinguishable from *never bound*, and the safety property then rests entirely on "no
statement in the codebase inserts a binding for an existing player" — a code-shape argument of
exactly the kind that erodes when somebody adds a path in a year, which is the erosion `ADR-0030` §2
was written to prevent in the first place. Restoring a revoked device becomes one legal `INSERT`.
Keeping the row is what lets the primary key and the trigger speak for themselves.

**Model the device as a `credential` row — `kind = 'device'`, `identifier = <device id>`,
`secret_hash NULL` — and revoke it there.** Conceptually the tidiest option on offer, and it is what
`ADR-0012` and `ADR-0037`'s own title already say out loud: the device *is* a credential. One table
for every sign-in route, one place to enumerate them for the account screen, and `ADR-0027`'s
`UNIQUE (kind, identifier)` is exactly the uniqueness needed. Rejected because `credential` is built
around a secret that is *proved*: `verify(kind, identifier, presented)`, Argon2 on four threads, a
dummy-hash comparison so an unknown identifier cannot be enumerated, and a hard rule that no
function returns a hash. A device row would be a credential whose identifier is itself the proof,
special-cased out of every one of those paths — and `UNIQUE (kind, identifier)` would then forbid
the revoked row and the fresh binding coexisting, putting us back at the burned-string problem.
`ADR-0041` also fixes `kind` at `"password"` and nothing else for v0.1 and v0.2; widening it here to
hold a non-secret would be answering `DEC-031` sideways.

**Keep the column and record revocations in a `revoked_device` lookup table, keyed by device id
alone.** Its case: `player.device_id` is never touched, §2 survives literally, and `resolve` gains
one cheap `EXISTS` check. Rejected because a revocation keyed by device id and not by the pair
poisons the string globally — the device can never be resolved again, for anybody, including for the
fresh profile `ADR-0037` promises it — and the `NOT NULL UNIQUE` column still holds the value, so
there is nowhere for that fresh profile to go. It is the previous alternative's failure with an
extra table.

**Leave the shape to `STORY-0406`'s implementer, as `ADR-0037` did.** The honest option, and the
register raised `DEC-041` rather than take it: the answer is not obvious, three shapes are named in
the ADR that deferred it, and two of the three quietly contradict a different ADR's central
argument. A ticket-level choice here would have been made in a file nobody re-reads, against a
deadline (§*The deadline*) that closes the moment anything is hosted.
