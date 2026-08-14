# ADR-0029 — A display name is unique, case-insensitively, and permanent once set

- **Status:** Accepted
- **Date:** 2026-08-14
- **Records:** the human's answer to `DEC-017` — *unique and permanent*, Lichess-style — and settles
  what that answer requires technically
- **Amends:** [`ADR-0021`](ADR-0021-a-profile-gains-a-display-name.md). The `UNIQUE` constraint it
  deliberately withheld is now wanted; its nullable column, its `PUT /api/me/name` and its
  `ProfileWrites` port are unchanged
- **Constrains:** `EPIC-04`'s `STORY-0401` (the column and the write path), `STORY-0402` (the read
  path), `STORY-0410` (the product rules), `STORY-0411` (the name in the client); and the leaderboard
  rows `EPIC-05` will build
- **Leaves open:** the moderation half of `DEC-017`, which stays the human's

## Context

The human has chosen, verbatim: *"Unique and permanent — a display name is chosen once, unique
across players, and never changed. Strongest identity, no impersonation on a leaderboard, and no
rename to escape a reputation."* That choice is not re-argued here. What follows is everything it
leaves under-determined — the places where, without an answer, a coder writes one into a ticket and
nobody finds it again.

Four things are genuinely in tension.

**Uniqueness is a fold, and every fold is a trade.** Byte equality lets `Bob` and `bob` sit on the
same leaderboard, which is impersonation with an extra keystroke — the precise harm the human named.
A wider fold — case, accents, scripts, confusable shapes — catches more impersonation and refuses
more legitimate names, permanently, with no rename available to the player it refused.

**`display_name` is nullable, so a profile exists before a name does.** `ADR-0012` creates a `player`
row for a device that has said nothing yet; `ADR-0021` made `NULL` mean *never set*. "Permanent"
therefore cannot mean *immutable from birth*. Something has to say when the clock starts, and what
enforces it, or the first `UPDATE` in a support script quietly ends the guarantee.

**Permanence removes the player's remedy for everything.** A typo, a name a squatter took first, a
name that should never have been allowed — under a rename policy all three are the same five-second
fix. Under this one, none of them is fixable by anybody, because no admin path exists in this
system. That is a consequence of the choice, not an argument against it, and it has to be written
down where someone will find it.

**A unique name looks exactly like a login handle.** [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md)
has just given `credential.identifier` a `UNIQUE (kind, identifier)` whose whole purpose is to make
sign-in a function of a string. `display_name` is about to get a unique index that looks identical
and must never be used the same way.

### The deadline, honestly

`ADR-0021` had no deadline and said so. **This one does, and it expires the moment the first name is
stored.** `display_name` does not exist on disk yet — the migration chain stops at `V2` — so today
every uniqueness rule costs one line and touches no data. Afterwards, widening the fold can collide
with names already promised to be permanent, and there are only two ways out: rename somebody
(breaking the promise this ADR exists to keep) or grandfather the collision (breaking the uniqueness
it exists to provide). So the fold is chosen at its intended final width now, and the parts left for
later are named as knowingly deferred rather than silently postponed.

## Decision

### 1. Uniqueness is case-insensitive, and the database is the guarantee

```sql
CREATE UNIQUE INDEX player_display_name_unique
    ON player (lower(display_name COLLATE "und-x-icu"));
```

- **Case-insensitive**, because the human's stated reason for choosing this option was *no
  impersonation on a leaderboard*, and `Bob` beside `bob` on a leaderboard is that impersonation.
- **The collation is pinned, not implicit.** `lower()` folds according to the collation of its
  argument. Left to the default it follows the cluster's `LC_CTYPE`, so the same schema would enforce
  a different rule on the `postgres:16-alpine` container the tests use and on whatever `EPIC-07`
  deploys. `und-x-icu` is ICU's root locale, it is present in `postgres:16-alpine`, and it folds the
  whole of Unicode: `Élodie` and `élodie` collide, not merely `Bob` and `bob`.
- **`NULL` folds to `NULL`**, and a btree unique index admits many nulls, so any number of unnamed
  profiles coexist. No `NULLS NOT DISTINCT`.
- **The stored value is the name as the player typed it**, after the canonicalisation in §2. The fold
  is a collision test only: it is never stored, never displayed and never returned.
- **The index is the reservation.** There is no reservation table — see §5.

### 2. The stored name is canonical: trimmed, NFC, bounded

```sql
ALTER TABLE player ADD CONSTRAINT player_display_name_trimmed
    CHECK (display_name = btrim(display_name));
ALTER TABLE player ADD CONSTRAINT player_display_name_nfc
    CHECK (display_name IS NFC NORMALIZED);
```

`ADR-0021`'s `player_display_name_length` check (1–32) is unchanged.

**NFC is a database constraint because it protects the database's guarantee.** `é` written as
`U+00E9` and as `U+0065 U+0301` are different strings that both survive the unique index — two
players, one visible name, forever. The write path normalises to NFC before it writes
(`java.text.Normalizer.Form.NFC`); the `CHECK` is what makes that a guarantee rather than a habit.

The write path counts **code points**, not UTF-16 units, when it applies the 1–32 bound, so its
answer is the same answer `char_length` gives. A mismatch there is a `500` where a `400` belonged.

### 3. Characters: the write path refuses the invisible; script mixing is not decided here

`PUT /api/me/name` answers `400` and writes nothing when the canonical form contains:

- any character in Unicode category `Cc` or `Cf` — control characters, zero-width spaces and
  joiners, bidirectional overrides, `U+FEFF`. None has a role in a label and each has a role in a
  spoof;
- any whitespace other than `U+0020`, `U+0020` at either end (the value is trimmed first), or two or
  more consecutive `U+0020`. Otherwise `Bob  Smith` and `Bob Smith` are two names that render
  identically, and one of them is permanent.

This is deliberately **not** a script or alphabet rule. Whether a name may be written in Cyrillic,
Greek or CJK — and so whether Latin `a` and Cyrillic `а` may both appear on a leaderboard — is a
question about who this product is for, not a question two engineers would answer the same way. It
is not answered here; see *What this does not settle*.

These live in the write path rather than in a `CHECK` because this is the rule most likely to move
when that question is answered, widening it can never invalidate a stored row, and a merged
migration is the worst place to keep a rule that is expected to change. The three constraints in §1
and §2 are the ones with a guarantee to protect.

### 4. Permanent means immutable once non-null, and a trigger says so

```sql
CREATE FUNCTION player_display_name_is_permanent() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        RAISE EXCEPTION 'display_name is permanent once set (ADR-0029)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER player_display_name_permanent
    BEFORE UPDATE OF display_name ON player
    FOR EACH ROW EXECUTE FUNCTION player_display_name_is_permanent();
```

Exactly what it enforces:

- **`NULL → name` succeeds, and can succeed only once**, because after it the column is not null.
  This is the transition the human's *"chosen once"* means, and it is the only one.
- **`name → a different name` raises. `name → NULL` raises.** There is no un-naming.
- **`name → the identical name` succeeds and changes nothing**, so `PUT` stays idempotent under a
  retry. Identity here is exact equality of the canonical form: a player called `Bob` who sends `bob`
  is attempting a rename and is refused.
- A profile is still born nameless; nothing in `PlayerDirectory.resolve` sets a name. An `INSERT`
  carrying a name is not a permanence violation and needs no trigger.
- The `OF display_name` clause means the trigger fires only on statements whose `SET` list names the
  column, so the coin-balance write on every finished duel does not touch it.
- The error code is `restrict_violation` (`23001`) — in the integrity-constraint class, distinct from
  `unique_violation` (`23505`), matched on the code and never on the message.

**Why a trigger and not application logic alone.** Permanence is the one property here with no
second chance: every other invariant this codebase enforces in Kotlin can be repaired by an
`UPDATE`, and this one is defined by the impossibility of that `UPDATE`. A `CHECK` cannot see `OLD`,
so a trigger is the only in-database mechanism that can say *not different from what it was*, and it
holds against a migration, a `psql` session, a future admin tool and any second write path somebody
adds in a year. The application refuses the write too (§5) — that is what produces a `403` instead of
a `500`, and it means the trigger never fires in normal operation. A guard that fires is a bug
report; both are tested.

### 5. The endpoint, its answers, and the race

`PUT /api/me/name`, on the `ProfileWrites` port `ADR-0021` introduced. Authentication is resolved
before the body is read, as on the existing routes.

| Outcome | Answer |
| --- | --- |
| Absent, blank or unknown credential | `401`, empty body — unchanged from `GET /api/me` |
| Name set | `200 OK` with `ProfileResponse`, including the canonical `displayName` |
| Empty after trim, over 32 code points, or a refused character | `400`, empty body |
| The fold collides with another player's name | `409 Conflict` |
| This player already has a different name | `403 Forbidden` |

`200` carries the profile rather than `204` because the server trims and normalises: the client must
be **told** the exact string it now owns rather than assume it got what it sent. That is
[`ADR-0002`](ADR-0002-server-authoritative.md) in miniature, and it saves a round trip.

`403` rather than `409` for an existing name, deliberately: `409` promises a conflict with the
current state, which invites a retry, and no state this client can ever reach makes the request
succeed. `403` is the honest code, and it lets the client distinguish the two failures without a
response body.

**The write is one statement and the index is the reservation.**

```sql
UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL
```

One row updated is success. `23505` is `409`. Zero rows means the player already has a name: the port
reads it and answers `200` if it equals the requested canonical form (the idempotent retry) and `403`
otherwise. `SQLSTATE` is translated inside `duels.poker.server.db` and never reaches a route; the port
returns a sealed result — `NameSet(profile) | NameTaken | AlreadyNamed` — not an exception.

**Two players sending the same name at the same instant**: the second `UPDATE` blocks on the first
transaction's uncommitted index entry, and fails with `23505` the moment the first commits. Checked
against `postgres:16-alpine`, the image the suite uses: the second writer waited out the first
transaction and then took the violation. **The loser's transaction aborts and burns nothing** — their
`display_name` is still `NULL`, no attempt is spent, no row is half-claimed, and they may send
another name immediately.

**There is no availability-check endpoint.** `GET /api/names/{name}` would be a pure enumeration
surface bought for a nicer form; the `409` answers the question at the only moment it matters.

**Enumeration is accepted, and here is why it is not the `ADR-0027` case.** A `409` does tell a
stranger that a name is taken. Display names are published — a leaderboard is a list of them — so
there is no secret, and refusing to say why a write failed would make the endpoint unusable. The
distinction from `ADR-0027`'s rule that sign-in failures must be indistinguishable is exact: an
identifier there is *half of a credential*, so confirming one exists tells an attacker what to
attack. A display name is half of nothing (§7). Failed attempts are not budgeted; if abuse appears,
the budget takes [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md)'s shape keyed by `PlayerId`,
which is an addition, not a change to any of this.

### 6. A player who never set a name

`ProfileResponse.displayName` and `DuelSummaryResponse.opponentDisplayName` are `null`. **The server
fabricates nothing** — no `Anonymous`, no `Player-3F2A`, no silent fall back to `opponentPlayerId`.
`ADR-0021` forbade a placeholder and uniqueness sharpens the reason: a server-minted name would be a
name inside a unique namespace that two players could hold at once, and a wire fact nobody can
remove.

**What a client renders for `null` is the client's**, decided in `STORY-0411` within `EPIC-06`'s
design language. The server does not care which treatment wins and has no opinion to offer. The one
thing forbidden is a client asking the server for the placeholder.

### 7. A name is never an authentication factor, and uniqueness does not change that

`credential.identifier` and `display_name` now both carry a uniqueness rule and are not the same kind
of thing:

- `credential.identifier` is **looked up** to resolve a player. `display_name` is **never** looked up.
  Its index exists to make a write fail, not to make a read succeed.
- `ProfileReads` and `ProfileWrites` expose no function that takes a name and returns a `PlayerId`,
  a `DeviceId`, an `AuthSession` or a profile. That is structural over the public API and testable
  there, in the same way `ADR-0027` makes *no function returns a hash* structural.
- If `DEC-027` lands on a human-chosen sign-in handle, that handle is a `credential` row and may be a
  completely different string from the same player's display name. Nothing joins the two, and no
  constraint keeps them equal.

### 8. Where it lands

- The index, the trigger and the two checks go **in the same migration file that adds
  `display_name`** — the column has never existed, so there is nothing to deduplicate and no
  backfill. If that migration has already merged by then, they go in the next free `V<n>` file.
  Never an edit to a merged migration.
- **No wire version moves.** `protocol.gen.ts` is emitted from `ClientMessage` and `ServerMessage`
  only ([`ADR-0020`](ADR-0020-typescript-protocol-from-serial-descriptors.md)); these are plain-HTTP
  DTOs. [`docs/protocol.md`](../protocol.md) gains the `PUT /api/me/name` contract above.
- `poker-engine` learns nothing. A duel is played by two seats; what they are called is a server
  fact.

## Consequences

**What it buys.** A name on a leaderboard means one player, guaranteed by the database rather than by
a hope that every write path remembered to check. `STORY-0401` and `STORY-0410` unblock. The
reservation flow the human's option "needs" turns out to need no new machinery at all — the unique
index is it, with no second table, no lock manager and no sweep for abandoned reservations.

**What it costs.**

- **A typo is forever.** The most common outcome of this decision will not be an impersonation
  defeated; it will be a player named `Bobb` who meant `Bob`, with no recourse, ever. There is no
  admin path and this ADR builds none.
- **An offensive name is forever too**, for the same missing reason. Permanence converts moderation
  from *rename them* into *there is nothing we can do*. This is the sharpest edge of the choice and
  it is recorded here rather than discovered by the first person who reports one.
- **Squatting is permanent and strictly first-come.** Device ids are trivially minted (`ADR-0012`),
  so a script can hold every short name forever at the price of one profile each. `ADR-0012` already
  gates that class of abuse on `EPIC-05`; this decision adds a second, irreversible thing to farm.
- **A homoglyph impersonation survives the fold.** `аce` with a Cyrillic `а` is a different name from
  `ace` under any case fold and will stay registered once taken. Closing that later is the expensive
  direction named under *the deadline*.
- **Two more schema objects to keep correct** — an expression index and a PL/pgSQL trigger — in a
  schema that until now was four tables and no procedural code. Every test that touches `player`
  now runs against a trigger.
- **ICU becomes a deployment requirement.** `und-x-icu` must exist in whatever Postgres `EPIC-07`
  runs; `postgres:16-alpine` has it. A build without ICU fails the migration at startup, which is the
  right way to fail. Postgres also records a collation-version dependency for the index, so a major
  ICU upgrade under a running cluster will warn and ask for a `REINDEX` — real operational work,
  small and conservative for a case fold.
- **The client gains two failure states to render** (`409` and `403`) that a rename policy would not
  have needed. That is `STORY-0411`'s work.

**What it forecloses.**

- **Renaming, in every form** — including a future *one change per year*, which would need an ADR
  superseding this one and an answer for what happens to the names it releases back into the
  namespace.
- **Widening the fold cheaply**, from the instant the first name is stored. Narrowing stays safe.
- It does **not** foreclose search. The column keeps an ordinary deterministic collation, so `LIKE`,
  `ILIKE` and a pattern-ops index remain available to `EPIC-05`'s leaderboard and to `STORY-0409`.
  A non-deterministic collation on the column would have forbidden `LIKE` outright.

**What this does not settle.**

- **Moderation and filtering — still the human's, still open.** `DEC-017` asked four questions and
  the human answered three; this ADR does not invent an answer to the fourth, and nothing above
  filters a name for content. Phrased so it can be answered in one sentence: *is any name refused at
  the moment it is set — a blocklist, a script or alphabet restriction, or nothing at all — and is
  there any path by which a name that should never have been allowed is taken away afterwards?*
  Recommended bookkeeping: `DEC-017` stays in the open list, narrowed to that question, noting this
  ADR answered the rest. The register row is the driver's to write; **this ADR does not edit the
  register.**
- The three residuals above — an offensive name, a squatted name, a homoglyph — all need the same
  missing thing, which is a path by which a name is taken away. They are one question, not three,
  and it is the question above.
- **`DEC-029` (deletion) now has a name-shaped edge.** If a `player` row is ever hard-deleted its
  name leaves the unique index and becomes claimable by somebody else, so a reputation is
  inheritable; a soft delete or a tombstone keeps it reserved. Not decided here, recorded so whoever
  answers `DEC-029` sees it rather than discovers it.
- Nothing here requires a player to have a name. Anonymous, nameless play stays possible for as long
  as `DEC-025` says it does.

## Alternatives considered

**Case-sensitive uniqueness — a plain `UNIQUE (display_name)`.** Its strongest case is real: it is
the simplest rule available, needs no collation choice, no ICU, no expression index and no
`REINDEX` story; and it is the only fold that can never refuse a name a human would consider
different from an existing one, which under permanence is a refusal you cannot apologise for.
Rejected because it defeats the reason the human gave for choosing this option at all. `Bob` and
`bob` on one leaderboard is impersonation with an extra keystroke, and permanence means the
impersonator keeps it.

**A non-deterministic ICU collation on the column** (`CREATE COLLATION ... locale = 'und-u-ks-level2',
deterministic = false`, then `UNIQUE (display_name)`). Genuinely elegant, and it nearly won: `=`
itself becomes case-insensitive, so no future query can compare case-sensitively by accident, and the
constraint is a plain `UNIQUE` with no expression to get wrong. Rejected because Postgres refuses
`LIKE`, `ILIKE` and pattern-matching operators against a non-deterministic collation, and `EPIC-05`
puts a name in every leaderboard row while `STORY-0409` wants search. Foreclosing prefix search to
save one index expression is a bad trade — and a column whose equality behaves unlike every other
text column in the schema is the kind of surprise that gets rediscovered at two in the morning.

**`citext`.** The same effect under a name people recognise, at the price of one `CREATE EXTENSION`.
Rejected: it adds an extension to the deployment surface `EPIC-07` has to build, its comparison is
defined by `lower()` under the database's collation — exactly the host-dependence the pinned
collation exists to remove — and the Postgres documentation now points at non-deterministic
collations in its place.

**Fold in Kotlin into a second stored column** (`display_name_folded TEXT UNIQUE`, written by the
application). Its strongest case is good: the fold becomes a unit-testable Kotlin function, complete
over Unicode without depending on the server's ICU build, reviewable in a diff — and the `UNIQUE`
constraint is still the database's, so the race is still handled by the index. Rejected because it
stores the same fact twice in one row, where an application bug can write a fold that does not match
the name and the database cannot tell. `GENERATED ALWAYS AS (...) STORED` repairs that and is then
the expression index again, plus a column that shows up in every `SELECT *`.

**Application-only permanence, no trigger.** Its strongest case is the one this codebase usually
accepts: the `403` has to be decided in Kotlin anyway, it is testable without a container, and it
keeps the schema to tables and constraints with no procedural code. Rejected on an asymmetry — every
other invariant enforced in Kotlin here is recoverable by an `UPDATE`, and this one is *defined* by
that `UPDATE` being impossible. A migration, a support script or a second write path added next year
would each end the guarantee silently. One PL/pgSQL function makes that impossible from all three.

**Permanent from birth: `display_name NOT NULL`, assigned when the profile is created.** Its
strongest case deletes this ADR's hardest question and a whole class of client branch: every profile
has a name, `opponentDisplayName` stops being nullable, and §6 disappears. Rejected because profiles
are created by the socket handshake for a device that has said nothing yet (`ADR-0012`), so the
server would have to invent the name — a fabricated placeholder, which `ADR-0021` forbids, and now a
permanent one inside a unique namespace. It also drags name-setting into the connection path.

**A reservation table with a TTL** (`name_reservation(folded_name, player_id, expires_at)`). Its
strongest case is the flow it is built for: a multi-step sign-up where a client holds a name across
several screens, with a live availability check beside the field. Rejected because there is no
multi-step sign-up here — one `PUT` sets the name — so the reservation would exist for the
milliseconds the unique index already covers, at the price of a second source of truth, an expiry
sweep, and a way for the two to disagree about who owns `ace`.

**A blocklist or content filter at set time.** Not rejected — **not mine.** See *What this does not
settle*.
