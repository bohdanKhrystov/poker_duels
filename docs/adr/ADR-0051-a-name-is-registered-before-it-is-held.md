# ADR-0051 — A name is registered before it is held, and a takedown is one function call

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-042` — by what path an operator force-renames a profile, and where the
  blocklist and the retired-name set live.
  [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) fixed that the path
  exists, that a taken name is retired forever, and that it *"does not need a role system to exist
  and will not grow one speculatively"*, and left all three homes unstated
- **Amends:** [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §1 (*"there is no
  reservation table"* — there is now one table, and it is the namespace rather than a reservation
  with an owner and a clock), §4 (the permanence trigger gains **exactly one** exception, scoped to
  one transition) and §5 (the write becomes two statements in one transaction; the reservation moves
  from `player`'s index to the registry's). Its fold, its collation pin, its three `CHECK`s, its
  status codes and its refusal of an availability endpoint are untouched.
  [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md)'s *"uniqueness
  therefore has three sources of truth"* — the three sources remain, as three values of one column,
  consulted by one index rather than by three checks
- **Builds on:** [`ADR-0030`](ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §2 and
  [`ADR-0049`](ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §7 (the enumerated
  writers of `player`, which is the live version of that argument),
  [`ADR-0039`](ADR-0039-v01-offers-no-account-deletion.md) (no `ON DELETE` clause; a deletion
  feature must state out loud what it does)
- **Constrains:** `STORY-0410`, which it unblocks and whose three pieces of work it fixes; and
  `PostgresProfileWrites`, which `STORY-0401` merged and which this changes
- **Adds one migration**, taking the next free `V<n>` at merge time — `V5`, or `V6` if
  `STORY-0406`'s has landed first
- **No wire change.** `PROTOCOL_VERSION` does not move: no socket message carries a display name,
  and nothing here is a socket fact
- **Raises:** `DEC-046`, the product owner's — see *What this does not settle*

## Context

`ADR-0038` answered *may a name be refused, and may one be taken away* and deliberately answered
nothing about mechanism. Four forces pull on what is left.

**The trigger refuses the takedown.** `ADR-0029` §4's `player_display_name_permanent` raises on
`name → NULL` for everybody, and it was installed precisely so that permanence *"holds against a
migration, a `psql` session, a future admin tool and any second write path somebody adds in a
year."* The takedown is that admin tool. Whatever it is, it either gets an exception or the feature
does not exist — and an exception written carelessly gives back exactly what the trigger was
installed to take away, which is the ability to move a name off a player.

**Three sources of truth is a race, not a checklist.** `ADR-0038` says uniqueness must consult names
in use, the retired set and the blocklist. Read literally — `player`'s unique index plus two tables —
the check is not atomic, and the failure is not theoretical. Under `READ COMMITTED`, a claim shaped
`UPDATE player SET display_name = ? WHERE ... AND NOT EXISTS (SELECT 1 FROM retired_name ...)`
evaluates its subquery against the snapshot taken when the statement started, then blocks on the
index entry the current holder still owns. A takedown committing in that window frees the index entry
and inserts the retirement row atomically — and the waiting claimer resumes with an answer from
before the commit and lands the name that was supposed to have been spent forever. Two structures
consulted by one statement are two instants, and the outcome of losing that race is permanent.

**Everything that writes `player` is counted.** `ADR-0030` §2 enumerates the statements that write
the ledger row and `ADR-0049` §7 keeps the count at three, on the argument that an identity
operation issuing any `UPDATE player` is one careless `SET` away from the coin balance. A takedown
writes `player`. It has to arrive as a statement somebody can point at, not as an endpoint whose
body could grow.

**The operator is one person and must not become a role.** `ADR-0038` says so in as many words. An
authenticated admin endpoint needs something to authenticate, and a shared secret in the environment
is a role with one member, no revocation and no audit — a role system with the parts that make it
safe left out. Meanwhile the person who would use it already holds the database credentials, which
grant strictly more than the endpoint would.

One more force, smaller but load-bearing: **the fold belongs to PostgreSQL.** `ADR-0038` requires all
three sources consulted case-insensitively under the ICU collation `ADR-0029` pinned. A blocklist
folded by the JVM is folded by a different function, and the two disagree on exactly the confusable
strings a blocklist exists to catch.

### The deadline, honestly

**There isn't one, and that is worth stating rather than manufacturing one.** No deployment exists,
`name_registry` starts as a copy of names already held, and every part of this is a migration that
would cost the same next month. What exists is a blocked story and a decision point that has been
open since 2026-08-16.

The one thing with a clock is an ordering, not a date: **a takedown must never ship before the
retired set.** A force-rename that only nulls the column returns the string to the pool the instant
it lands, and every name freed that way is unrecoverable — `ADR-0038`'s *retired forever* cannot be
applied retrospectively to a name somebody else has since taken permanently. That is a reason for
this ADR to fix all three pieces at once rather than to let `STORY-0410` land the easy one first.

## Decision

**A display name is registered in `name_registry` before it is held, a string that enters that table
never leaves it, and an operator takes a name away by calling one function in the database.**

### 1. `name_registry` is the namespace, and it is one table

```sql
CREATE TABLE name_registry (
    name         TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    retired_from UUID        REFERENCES player (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT name_registry_pkey PRIMARY KEY (name),
    CONSTRAINT name_registry_reason CHECK (reason IN ('TAKEN', 'BLOCKED', 'RETIRED')),
    CONSTRAINT name_registry_retired_from CHECK (retired_from IS NULL OR reason = 'RETIRED'),
    CONSTRAINT name_registry_length CHECK (char_length(name) BETWEEN 1 AND 32),
    CONSTRAINT name_registry_trimmed CHECK (name = btrim(name)),
    CONSTRAINT name_registry_nfc CHECK (name IS NFC NORMALIZED)
);

CREATE UNIQUE INDEX name_registry_folded
    ON name_registry (lower(name COLLATE "und-x-icu"));

ALTER TABLE player ADD CONSTRAINT player_display_name_registered
    FOREIGN KEY (display_name) REFERENCES name_registry (name);
```

- **The blocklist and the retired set are the same structure**, distinguished by `reason`, and so is
  the set of names in use. `ADR-0038`'s three sources of truth become three values of one column
  behind one index. That is the whole of this decision's answer to *how does a write path check all
  of them without a race*: **it does not check three things. It inserts one row, and the index
  refuses.**
- **A string never leaves this table.** A retirement changes a `reason`; it deletes nothing. So there
  is no instant at which a spent name is absent from the index, and the race described in *Context*
  has nowhere to happen — a claimer conflicts with the row whatever its `reason` is, and never waits
  for a takedown to finish.
- **The fold is `ADR-0029` §1's, character for character**, so the registry refuses what the player
  index would have refused and the three `CHECK`s hold every row in the same shape
  `player.display_name` is held in. Comparing like with like is the reason they are repeated rather
  than assumed.
- **The primary key is the string itself**, which is what the foreign key points at. It is a
  case-sensitive uniqueness rule subsumed by the folded index; it exists so that the foreign key has
  a unique constraint to reference and so that the permanence trigger (§3) can match on equality
  rather than on the fold.
- **The foreign key is what makes the registry complete.** Without it, *"every name ever held is in
  the registry"* is a promise made by one Kotlin file; with it, a `player` row cannot hold a name
  that was never registered, from any write path, including `psql` and including a test fixture.
  This codebase has twice chosen the constraint over the promise for exactly this class of property
  (`ADR-0029` §4, `ADR-0049` §2), and here it also buys the exactness the trigger in §3 relies on.
- **`retired_from` is record-keeping, not a source of truth.** Nothing in production reads it; who
  holds a name is `player.display_name` and nothing else. It is there so an operator can answer
  *whose name was this* six months later.
- **No `ON DELETE` clause**, matching `V4` and `ADR-0049` §1: whether a `player` row is ever deleted
  is `ADR-0039`'s *not in v0.1*, and `NO ACTION` forces a deletion feature to say out loud what
  happens to the names that profile has spent.
- **No read path exposes this table.** No endpoint enumerates it, no response mentions it, and there
  is still no availability check — `ADR-0029` §5 refused one and nothing here reopens it.

### 2. Setting a name is two statements in one transaction, and the failure path must roll back

```sql
-- 1. spend the string, or fail: 23505 means the namespace has already spent it.
INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN');

-- 2. hand it to the player, or fail: zero rows means they already hold a different name.
UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL
RETURNING id, coin_balance, display_name;
```

| What happens | `SetNameResult` | HTTP |
| --- | --- | --- |
| One row from statement 2 | `NameSet(profile)` | `200` |
| `23505` from statement 1, and this player already holds that exact canonical form | `NameSet(profile)` | `200` — the idempotent retry |
| `23505` from statement 1, in every other case | `NameTaken` | `409` |
| Zero rows from statement 2 | `AlreadyNamed` | `403` |

- **`SetNameResult` gains no fourth case, and no answer says which source refused.** Held by
  somebody else, blocked, retired, or retired *from this very player* — all of them are `409`. That
  is `ADR-0038`'s *"the same kind of error a taken name produces"*, it keeps `STORY-0411` at the two
  failure states `ADR-0029` §5 already promised, and it tells a prober nothing beyond what a
  leaderboard already publishes.
- **Anything other than one row from statement 2 rolls the transaction back.** This is the defect a
  competent implementer will otherwise ship, and it is worth naming as plainly as `ADR-0049` §4
  names its orphan-profile race: a registry row left behind by a refused claim **permanently burns a
  string nobody holds**, so a player who is already named could spend the namespace one failed `PUT`
  at a time. The rollback is not a tidiness measure; it is the reason a failed claim costs nothing.
- **Two players sending the same name at the same instant** behave exactly as `ADR-0029` §5
  describes, one table over: the second blocks on the first's uncommitted registry row and takes
  `23505` the moment it commits. **The loser burns nothing** — no registry row survives their
  rollback, their `display_name` is still `NULL`, and they may send another name immediately.
- **Statement 2 cannot raise `23505`.** Any colliding name would already be a registry row, so
  statement 1 would have refused first; `player_display_name_unique` survives as a second line of
  defence that can only fire if the registry and the column have somehow disagreed, which the
  foreign key makes unreachable.
- **The screen fails closed structurally, not by a `catch`.** `ADR-0038` records this as the single
  thing most likely to be got backwards. Here a blocklist that cannot be read is a database that
  cannot be reached, and then statement 1 has not succeeded and nothing is written. There is no state
  in which the screen is skipped and the name is set.
- The cost is one round trip and one index write on a tiny table, once per profile in its lifetime.

### 3. The permanence trigger gains one exception, and it is about the transition, not the writer

The new migration replaces the function body; `V3` is not edited and the trigger definition is
untouched, because it already binds by name.

```sql
CREATE OR REPLACE FUNCTION player_display_name_is_permanent() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        -- ADR-0051: the one exception. A name may be given up only by being spent: the transition
        -- that leaves the player nameless is the same transition that retires the string forever.
        IF NEW.display_name IS NOT NULL OR NOT EXISTS (
            SELECT 1 FROM name_registry
             WHERE name = OLD.display_name AND reason = 'RETIRED'
        ) THEN
            RAISE EXCEPTION 'display_name is permanent once set (ADR-0029, ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

**What the exception is scoped to, exactly:** `name → NULL`, and only when that name is already
`RETIRED` in the registry. Everything else raises as before.

**Why it does not reopen what the trigger closed.** The trigger exists to make two things
impossible, and both are still impossible:

- **`name → a different name` has no exception at all**, for anybody, from any connection. Renaming
  to escape a reputation — the human's stated reason for permanence — is refused as absolutely as it
  was yesterday, and an operator cannot do it either. `ADR-0038`'s *"it must never end up holding a
  name it did not choose"* is enforced by the database rather than by the operator's care.
- **A name cannot be freed.** The only way past the trigger is to spend the string first, so a
  vacated name is never a claimable name. The exception does not weaken permanence; it is the
  mechanism by which `ADR-0038` turned permanence into *permanence to the player*, and the price is
  paid in the same transaction as the benefit.

**Why the check is a transition and not a privilege.** Scoping the exception by `current_user`, a
`SET LOCAL` GUC or `SECURITY DEFINER` would protect nothing that is not already true — anybody with
the privilege to run the takedown can also `ALTER TABLE player DISABLE TRIGGER` — while making the
rule about *who is writing*, which is the first stone of the role system `ADR-0038` refused. A
transition-scoped exception is strictly stronger: even with every privilege in the cluster, the
ordinary route cannot free a name.

**Monotonicity of the registry**, so *retired forever* is not resting on nobody running a `DELETE`:

```sql
CREATE FUNCTION name_registry_is_monotone() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.reason <> 'BLOCKED' THEN
            RAISE EXCEPTION 'a name that has been held is never released (ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
        RETURN OLD;
    END IF;
    IF NEW.name <> OLD.name OR OLD.reason <> 'TAKEN' OR NEW.reason <> 'RETIRED' THEN
        RAISE EXCEPTION 'the only change a registered name may take is TAKEN to RETIRED (ADR-0051)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER name_registry_monotone
    BEFORE UPDATE OR DELETE ON name_registry
    FOR EACH ROW EXECUTE FUNCTION name_registry_is_monotone();
```

A `BLOCKED` row may be deleted, because a curated list must be correctable. Nothing else may be
deleted, no `name` may ever be rewritten, and the only permitted update is `TAKEN → RETIRED`. The
error code is `restrict_violation` (`23001`), matched on the code and never on the message, as
`ADR-0029` §4 established.

### 4. The takedown is `retire_display_name`, called from a `psql` session

```sql
CREATE FUNCTION retire_display_name(target_player UUID, expected_name TEXT)
RETURNS TEXT AS $$
DECLARE
    held TEXT;
BEGIN
    SELECT display_name INTO held FROM player WHERE id = target_player FOR UPDATE;
    IF NOT FOUND OR held IS NULL THEN
        RAISE EXCEPTION 'player % holds no display name', target_player
            USING ERRCODE = 'no_data_found';
    END IF;
    IF lower(held COLLATE "und-x-icu")
       IS DISTINCT FROM lower(btrim(normalize(expected_name, NFC)) COLLATE "und-x-icu") THEN
        RAISE EXCEPTION 'player % does not hold that name', target_player
            USING ERRCODE = 'restrict_violation';
    END IF;

    UPDATE name_registry SET reason = 'RETIRED', retired_from = target_player WHERE name = held;
    UPDATE player SET display_name = NULL WHERE id = target_player;
    RETURN held;
END;
$$ LANGUAGE plpgsql;
```

**The path is: connect to the database with the credentials the server already uses, and call this
function.** Not an HTTP endpoint, not a Gradle task, not a hand-written procedure. The three
alternatives are argued below; what the function buys over each of them is that the ordering,
the atomicity and the interlock are in the schema, where `:poker-server:check` can hold them, rather
than in a wrapper or a document that drifts from the schema in silence.

- **It writes exactly two rows, in one transaction, in this order**: the registry row is promoted to
  `RETIRED`, then the player's column is nulled. The order is not a convention — reverse it and the
  permanence trigger raises, because the exception in §3 looks for a `RETIRED` row that does not yet
  exist. A caller in autocommit gets one transaction for free; a caller inside `BEGIN` gets the
  whole call or none of it.
- **If the registry row is missing** — a name that reached `player` before this migration existed —
  the promotion updates nothing, the null-out then raises, and the call fails whole. The function
  cannot free a name it did not manage to retire.
- **The second argument is the interlock.** The call names a player by id and states the name that
  player must currently hold; a mismatch raises and writes nothing. This is what turns the realistic
  operator accident — the right command against the wrong database, or a mistyped id — from a
  permanent takedown of an innocent profile into a loud error. The comparison is under the fold, so
  case need not be reproduced, and the expected name is normalised the same way the write path
  normalises: an operator pastes what the row shows them.
- **It is keyed by player id, never by name.** A function that took a name would act on whoever
  holds that string in whichever database the session is pointed at, which is precisely the accident
  the interlock exists to catch. Two independent facts about one row must match before anything is
  written.
- **It is not `SECURITY DEFINER` and grants nothing.** There are no roles here: the operator is
  whoever holds the database credentials, which is a deployment fact rather than a product concept.
  The day there are two operators, the answer is a second database role — not a feature.
- **The server never calls it.** No Kotlin references it, no port exposes it, and `STORY-0410`
  carries a test asserting the function's name appears nowhere under
  `poker-server/src/main/kotlin`. That is a code-shape guarantee rather than a constraint, and
  *Consequences* says so.
- **The call site is documented in a new `docs/operations.md`**, created by `STORY-0410`: the
  `SELECT` that finds the player and their exact stored name, this call, and the two blocklist
  statements from §5. It is the only new document, and it is a call site rather than a procedure —
  the procedure is in the migration.

**The takedown leaves the profile unset, not renamed**, and this is worth stating because
`ADR-0038`'s heading sentence (*"A force-rename gives the player a name"*) and its body disagree on
their face. The body settles it — *"a renamed profile may return to unset and be asked to choose
again"* — and `STORY-0410`'s acceptance criterion says the same thing, and `ADR-0021` and
`ADR-0029` §6 both forbid the server minting a name. A server-chosen replacement would be a
fabricated name inside a unique namespace, permanent, that the player did not pick. So the takedown
writes `NULL`, and *force-rename* in `ADR-0038`'s vocabulary means *takedown* here.

### 5. The blocklist is rows, curated in place, and screening is a set-time event

```sql
-- add an entry
INSERT INTO name_registry (name, reason) VALUES (normalize(btrim($1), NFC), 'BLOCKED');
-- remove one
DELETE FROM name_registry WHERE name = $1 AND reason = 'BLOCKED';
```

- **The contents are data, never a migration.** `ADR-0038` fixed that the contents are operational
  data; a migration is immutable and a curated list changes, so seeding one from a `V<n>` file would
  be the one shape guaranteed to be wrong later. **v0.1 ships an empty table**, and the mechanism is
  what this decision owes.
- **The entry is an exact string under the same fold as everything else** — not a substring, not a
  pattern. One mechanism, one index, one fold; and it is the direction that stays open, since a
  second, pattern-shaped source can be consulted alongside later at the price of a scan, whereas a
  substring rule shipped today refuses `Scunthorpe` forever under a permanence rule that offers no
  apology. `ADR-0038` already says a blocklist *"catches strings someone thought of in advance"* and
  that homoglyphs are not solved; this is the same admission made structural.
- **A blocklist entry cannot shadow a name in use.** The insert conflicts with the `TAKEN` row and
  fails. The operator's two options are to leave it or to retire it from its holder, and the schema
  refuses to express a third, quieter state in which a player keeps displaying a name the operator
  has decided is unacceptable.
- **Screening is a set-time event and nothing is re-screened.** A name already held when its string
  is blocked stays held until an operator retires it explicitly. Re-screening would mean a machine
  performing takedowns from a list, which is the human judgement `ADR-0038` deliberately kept — *"an
  impersonation only a human recognises"* — and it would make permanence depend on a mutable list,
  so a name could stop being yours because of a diff you never saw. The operator who adds an entry
  can find the holder with one `SELECT` and decide in the open.
- **A blocklist entry cannot be claimed and then blocked back**; a claim against a `BLOCKED` row
  takes `23505` like any other spent string.

### 6. What the player sees, and what does not move

- **`GET /api/me` returns `displayName: null`**, exactly as it did before they ever chose a name.
  `ADR-0029` §6's rule that the server fabricates nothing is unchanged, and what a client renders for
  `null` is still `STORY-0411`'s.
- **`PUT /api/me/name` works again.** Their column is `NULL`, so §2's statement 1 runs, and any
  available name is accepted. **Their old name is refused with `409`**, like any other spent string,
  which is `ADR-0038`'s *"including the player it was taken from"*.
- **Every history row that named them shows `null` afterwards.** `ADR-0039` forbade denormalising a
  display name into `duel_result`, so `DuelSummaryResponse.opponentDisplayName` is a join against
  the live column: the opponent's own record of who they beat loses the name too, retroactively.
  That is a consequence of a merged decision, not a new one, and it is recorded here because it is
  the part nobody expects.
- **Nothing is pushed and no socket frame changes.** No `ServerMessage` carries a display name, a
  duel in progress is unaffected, and there is no mechanism in this server for telling a player
  something asynchronously outside a duel socket. The technical default is therefore silence: the
  client discovers it on its next `GET /api/me`. **Whether the player is told at all, and in what
  words, is `DEC-046` and it is the product owner's** — see *What this does not settle*.
- **`poker-engine` learns nothing.** A duel is played by two seats; what they are called is a server
  fact.

### 7. What `player`'s writer list becomes

| Writer of `player` | After this ADR |
| --- | --- |
| `PlayerDirectory.resolve`'s `INSERT INTO player (id)` | unchanged |
| `ADR-0029` §5's `SET display_name` | unchanged as a statement; now runs inside a transaction with §2's registry insert |
| `PostgresDuelResultStore`'s `SET coin_balance = coin_balance + ?` | unchanged |
| — | **`retire_display_name`'s `SET display_name = NULL`** — a fourth statement, in the database, invoked by a human |

The fourth writer is a real addition to `ADR-0030` §2's enumeration and is recorded as one rather
than argued away. What holds is the property the enumeration exists for: it names exactly one column,
it lives in a function whose text is in the migration, and it is not reachable from the server
process. **P1 and P2 (`ADR-0030` §5) are untouched — no statement in this ADR reads or writes
`coin_balance` or `duel_result`** — which is what `STORY-0410`'s existing criterion that *a takedown
moves no coin* will assert.

### 8. The migration

One new file, taking the next free `V<n>`; `V1`–`V4` are immutable and `STORY-0406` may take `V5`
first.

```sql
CREATE TABLE name_registry (...);                       -- §1
CREATE UNIQUE INDEX name_registry_folded ...;

INSERT INTO name_registry (name, reason)
SELECT display_name, 'TAKEN' FROM player WHERE display_name IS NOT NULL;

ALTER TABLE player ADD CONSTRAINT player_display_name_registered
    FOREIGN KEY (display_name) REFERENCES name_registry (name);

CREATE FUNCTION name_registry_is_monotone() ...;        -- §3
CREATE TRIGGER name_registry_monotone ...;
CREATE OR REPLACE FUNCTION player_display_name_is_permanent() ...;
CREATE FUNCTION retire_display_name(UUID, TEXT) ...;    -- §4
```

- **The backfill is the answer to *`player` rows already exist*.** Every name already held becomes a
  `TAKEN` row, so no existing player loses a name and no existing name becomes claimable.
- **It is satisfiable by construction.** `player_display_name_unique` guarantees no two held names
  collide under the fold, and `V3`'s three `CHECK`s guarantee every held name is trimmed, NFC and
  within 1–32 — which are exactly the registry's own constraints. If the backfill fails, the
  database was already inconsistent and failing loudly is right.
- **No `player` row is written and nothing is screened retrospectively.** Names already held are
  grandfathered, which is §5's set-time rule applied to the migration itself, and it is why the
  order matters: create and backfill before adding the foreign key, so the key is added against rows
  that already satisfy it. Flyway runs each migration in one transaction on PostgreSQL and DDL there
  is transactional, so a failure anywhere leaves the previous schema intact.
- **`player` and `name_registry` reference each other.** That is legal, and it costs nothing here
  because both columns are nullable: a profile is born nameless, and a registry row for a blocked
  string names no player.
- **The code lands in the same PR as the migration.** `PostgresProfileWrites` becomes §2's
  transaction, and the seven test files that write a display name directly —
  `DisplayNameUniquenessTest`, `DisplayNamePermanenceTest`, `DisplayNameSchemaTest`,
  `SchemaConstraintsTest`, `PostgresProfileWritesTest`, `PostgresProfileWritesConcurrencyTest`,
  `PostgresProfileReadsTest` — gain a registry insert wherever a name is expected to land. One of
  them changes in a way that will otherwise look like a flake:
  `PostgresProfileWritesConcurrencyTest` waits for the loser by polling `pg_stat_activity` for
  `query ILIKE 'UPDATE player SET display_name%'`, and after this the loser blocks on the registry
  insert instead.

### 9. What is deliberately not built

- **No admin endpoint, no admin session, no operator account, no moderation queue.**
- **No un-retire, no release, no recycling of abandoned names**, and no way to give a name back.
- **No re-screening job**, no scheduled scan of held names against the blocklist.
- **No second operator path** — no Gradle task, no CLI, no seed file for the blocklist.
- **No notification of any kind** to the player whose name was taken.
- **No enumeration surface**: nothing lists the blocklist, the retired set or the names in use, and
  `ADR-0029` §5's refusal of an availability endpoint stands.

## Consequences

**What it buys.** `STORY-0410` unblocks with all three of its pieces specified, and the hardest of
them — *how does a write path consult three sources without a race* — stops being a concurrency
problem and becomes an `INSERT`. `ADR-0038`'s *retired forever* is enforced by a primary key and a
trigger rather than by a rule somebody remembers; `ADR-0029` §4's permanence survives with a single
exception whose shape makes the dangerous transition still impossible for everybody, operator
included. Fail-closed screening, which `ADR-0038` called the thing most likely to be got backwards,
becomes structural: there is no path in which the screen is skipped. And the operator path adds no
public surface, no secret, no role, and no code that could grow one — the whole of it is a function
in the schema and a page in `docs/`.

**What it costs.**

- **An operator's mistake is unfixable.** A takedown against the wrong profile is permanent: the
  victim cannot reclaim their own name, nobody can un-retire it, and the only remedy is that they
  choose a different one. The interlock is the whole mitigation and it is a confirmation, which is a
  weaker guarantee than every constraint around it.
- **A takedown rewrites the past.** Because `ADR-0039` forbade denormalising the name, every history
  row on every *other* player's screen that named this profile silently becomes nameless. Someone's
  record of the duel they won against a named rival degrades, and they are told nothing.
- **A name set is no longer one statement.** `ADR-0029` §5's *"the write is one statement"* is spent:
  `PostgresProfileWrites` gains a transaction and a rollback that is load-bearing rather than tidy.
  §2 names the defect that follows — a registry row left behind by a refused claim burns a string
  forever — because it is the one a competent implementer ships and no single-threaded test catches.
- **Seven test files change, and one of them changes invisibly.** Every fixture that lands a display
  name needs its registry row first, and `PostgresProfileWritesConcurrencyTest`'s
  `pg_stat_activity` filter now names the wrong statement.
- **The blocklist ships empty**, so on the day `STORY-0410` merges the screen refuses nothing. The
  mechanism is what an ADR can supply; the contents are the operator's and there is no artifact
  holding anybody to curating them.
- **Exact strings only.** `s l u r` with spaces, a homoglyph, or any variation nobody listed passes
  the screen. `ADR-0038` already refused a script restriction; this refuses substring matching too,
  and the honest summary is that the blocklist catches what somebody typed into it and nothing else.
- **Nothing prevents the server calling `retire_display_name`.** It is in the same database under
  the same user, so *the takedown stays out of the product* rests on a test asserting a string is
  absent from the main sources — the weakest guarantee in this document, and the one that erodes
  first if somebody wants a moderation screen in a hurry.
- **The registry only grows, and now it grows per name rather than per takedown.** `ADR-0038`
  accepted an ever-growing retired set; this makes every name ever set a row, forever. Small in
  absolute terms, and it is a second row per named profile that account deletion will have to name.
- **Two unique indexes on one small table**, and a pair of foreign keys pointing at each other. Both
  are justified above and both are things a reader meets, stops at, and has to be told about.
- **A fourth writer of `player` exists** where `ADR-0049` §7 was able to keep the count at three.
  It is confined and it names one column, but the sentence *"three statements write `player`"* is no
  longer true, and the next person to reason about the ledger row has one more statement to read.

**What it forecloses.**

- **Freeing a name, in every form.** No expiry of retirements, no recycling of names from abandoned
  profiles, no "released after a year". A retention or deletion feature that wanted to return names
  to the pool now needs a migration and an ADR superseding this one — which is the point, and it is
  `ADR-0038`'s decision rather than this one's.
- **A quiet block of a name in use.** The registry cannot express *blocked but still displayed*, so
  every such case forces the operator to act or to leave it alone visibly.
- It does **not** foreclose pattern matching (a second source consulted alongside, additive), a
  future admin endpoint or role (nothing here is in its way, and §4's function would be what it
  called), or moving `display_name` out of `player` altogether — which this makes *easier*, because
  the namespace already lives in its own table and only the holder column would move.

## Alternatives considered

**Three structures and three checks — `player`'s index plus a `retired_name` table plus a
`blocked_name` table — the literal reading of `ADR-0038`.** The strongest case, and it is a real
one: it is by far the smallest diff. `SET_NAME_SQL` gains two `NOT EXISTS` clauses and stays one
statement in autocommit, `player.display_name` remains the single home of a live name with no
duplication to justify, there is no foreign key, no registry row per player, and not one test fixture
changes. Rejected on the race in *Context*: under `READ COMMITTED` the subqueries answer from the
statement's snapshot while the unique index answers from the physical present, and a claimer that
waits out a committing takedown lands the retired name. Closing it needs either an `AFTER` trigger —
correct, but resting on trigger firing order and volatile-function snapshot rules, and whose most
likely mis-implementation, `BEFORE`, passes every non-concurrent test — or an advisory lock on the
folded name, which is the application locking `ADR-0029` §1 refused. Both are ways of making two
structures behave like one. One structure needs no argument at all. Accepting the race instead was
also weighed, on the grounds that the window is milliseconds and the operator can simply take the
name away again: rejected because the remedy is a second irreversible act, and because a namespace
whose central promise holds *except under concurrency* is one nobody can quote.

**An authenticated admin HTTP endpoint.** Its strongest case is genuine: it is the only option that
works from a phone at the moment a report arrives, it needs no database access at all, and it could
reuse `IdentityResolver`, the canonicalisation and the port layer that already exist — the operator
would never touch SQL. Rejected because whatever authenticates it is a role: a shared secret in the
environment is a role with one member, no revocation and no audit, which is a role system with the
safe parts left out, and `ADR-0038` refused to grow one speculatively. It also puts a route whose
blast radius is other people's rows on the public surface of a server with no budget for it
(`ADR-0022`'s shape would have to be extended to cover it), and it buys a capability the operator
already has by other means, since they hold the database credentials that grant strictly more.

**A Gradle or CLI task run against the database.** Its case is the best of the three rejected paths:
it lives in the repository under review, it can reuse `canonicalDisplayNameOrNull` rather than
re-deriving canonicalisation in SQL, it can be exercised by the JUnit suite against Testcontainers,
and `./gradlew retireName --player=...` is easier to get right at 2am than a function call. Rejected
because it buys no safety it does not already have: it would read `DATABASE_URL` from the same
environment `psql` reads, so *what stops it running against production* has the identical answer
either way — production credentials must be deliberately exported — while it adds a `JavaExec` task,
a second configuration path, a `main` that must never reach the server jar, and a Kotlin copy of the
SQL that can drift from the schema it depends on. The thing that actually prevents the accident is
the id-plus-expected-name interlock, and that belongs to the statement, not to the wrapper. If a
task is ever wanted, it can call the same function.

**A documented `psql` procedure with no database function — the two statements written out in
`docs/`.** Its case is the most honest reading of `ADR-0038`: the operator is the human, the human
has `psql`, and zero code is the cheapest possible mechanism. Rejected because the two writes have to
be ordered and atomic and the trigger exception has to be satisfied, so a hand-typed procedure is one
forgotten `BEGIN` away from a name that is nulled but never retired — which is precisely the outcome
`ADR-0038` exists to prevent. Nothing tests a document, and a document drifts from a schema in
silence. The function is that procedure with the ordering, the atomicity and the interlock built in
and a JUnit test holding it to them; what stays a document is the call.

**Scope the trigger's exception by role** — `current_user IN (...)`, a `SET LOCAL poker.takedown`
GUC, or a `SECURITY DEFINER` wrapper. Its case: the trigger keeps its present shape for everybody
else, the exception is one line, and it is how most systems express *an administrator may do this*.
Rejected because it protects nothing that is not already true — the same session can disable the
trigger outright — while making the exception about who is writing rather than about what is being
written, which is the first stone of the role system `ADR-0038` refused. The transition-scoped
exception is strictly stronger: even a superuser following the ordinary route cannot free a name,
because the only way past the trigger is to spend the string.

**Move `display_name` out of `player` into its own table, `ADR-0049`'s shape** — one row per name
with `retired_at`, a partial unique index for one live name per player, and a total unique index on
the fold. It nearly won, and it is the tidier end state: live and spent become one row, there is no
duplication and no foreign key to justify, no `TAKEN` reason, and `player` gains no fourth writer at
all, since the takedown would touch it no more than revocation does. Rejected on timing and blast
radius rather than on shape. `STORY-0401` merged this week and `STORY-0402` is being split against
`player.display_name` right now; `ADR-0029` §§1–4's index, three `CHECK`s and trigger all live on
that column; and `RECENT_DUELS_SQL` would become a second join for a guarantee the registry already
delivers. It is also the change this decision makes cheaper rather than dearer — the namespace is
already its own table, so moving the holder column later is a smaller step than it is today.

**The blocklist as a classpath resource or a Kotlin constant.** Its case is strong and is about
review, not convenience: a word list in the repository is version-controlled and code-reviewed, so
adding an entry is a diff a human approves; it needs no round trip; it ships identically with the
jar; and it cannot be edited without a deploy, which for a policy is a feature rather than a
limitation. Rejected on the fold. `ADR-0038` requires all three sources consulted under the ICU
collation `ADR-0029` pinned, and a JVM `lowercase()` is a different function from
`lower(... COLLATE "und-x-icu")` — they disagree on exactly the confusable strings a blocklist
exists to catch, so the screen would refuse a different set of names than the index does. It also
cannot fail closed cheaply (a resource that fails to load must refuse every name, which is an
availability cliff), and it puts an incident behind a release.

## What this does not settle

**One question, and it is the product owner's.** Phrased so it can be answered in one sentence:
*is a player told that their display name was taken away — and if so, by what and in what words?*
It is not answered here because it is a question about what the product owes a player at the worst
moment it will ever have with them, not about what the schema can express: the schema is indifferent,
and the server has no asynchronous channel to a player outside a duel socket, so any answer other
than silence is a screen and a piece of copy rather than a column. It is registered as **`DEC-046`**
against `EPIC-04`, due before `STORY-0411` renders the name screen — `STORY-0410` ships §4's takedown
under either answer, since the technical default is **silence** and telling the player later is
additive.

Also not settled, deliberately:

- **The blocklist's contents.** Operational data by `ADR-0038`, and this ADR ships the table empty.
  Whether anything ever goes in it is nobody's decision but the operator's.
- **A second operator.** The answer is a database role, and it is not needed until there is a second
  person; nothing here is in its way and nothing here builds it.
- **Any record of who ran a takedown and why.** `retired_from` and `created_at` say which profile
  lost the name and when. There is no actor, no reason and no log, because there is one operator.
- **Pattern or substring screening**, which stays additive and stays refused today.
