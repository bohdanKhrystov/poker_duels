# ADR-0134 — A rename spends before it replaces, and no name is ever released

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-151` — by what mechanism does a display name **change**? Registered 2026-09-07
  by [`ADR-0130`](ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md), which fixed
  what a player sees and designed nothing
- **Serves, and may not contradict:** `ADR-0130` §§1–7. *That* a name may be changed is the
  human's; *what becomes of the string* is `ADR-0130`'s. Everything below is the third question —
  by what statements — and where a choice here could have been read as changing what a player
  sees, it is decided the way that keeps `ADR-0130` true
- **Amends:** [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) §1's
  `name_registry_reason` and `name_registry_retired_from` `CHECK`s (a fourth `reason`, `REPLACED`),
  §2's statement list and its result table (four statements, and `AlreadyNamed` is deleted), §3's
  monotonicity trigger (a second permitted transition) and §3's trigger body, whose one remaining
  guard is now the whole of it — **renamed** with it.
  [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §4's trigger and function
  **names**, and §5's response table. **Neither clause is overturned here** — `ADR-0130` superseded
  `ADR-0029` §4's *`name → a different name` raises*, its title's *permanent*, §5's `403` and
  `ADR-0051` §3's *no exception at all*; this ADR is the machinery that carries out that
  supersession
- **Leans on, and does not touch:** `ADR-0029` §1's fold and pinned collation, §2's
  canonicalisation, §3's character rules, §5's refusal of an availability endpoint and its
  idempotent retry, §6's *the server fabricates nothing*, §7's *a name is never an authentication
  factor*; `ADR-0051` §1's *a string that enters that table never leaves it*, its foreign key and
  its folded index, §4's `retire_display_name` **whole and unedited**, §5's blocklist, §7's writer
  list; [`ADR-0038`](ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md)'s screening;
  [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §§1–8 whole;
  [`ADR-0053`](ADR-0053-the-profile-says-the-name-was-removed.md) §3's query and its index, both
  unchanged; [`ADR-0022`](ADR-0022-the-room-code-is-the-invite.md) §2's budget shape and
  [`ADR-0074`](ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) §2's
  reserve-then-refund
- **Adds one migration**, taking the next free `V<n>` at merge time — `V9` as `develop` stands.
  Never an edit to `V3` or `V5`
- **No wire change.** `PROTOCOL_VERSION` **does not move** and `protocol.gen.ts` is not
  regenerated: `ADR-0020` emits it from `ClientMessage` and `ServerMessage` only, no
  `ServerMessage` carries a display name (`ADR-0130` §3, measured), and nothing here adds one.
  `STORY-1409`'s tickets are therefore **not** `atomic:` under `ADR-0047`'s lock — see §7
- **Constrains:** `STORY-1409`, whose implementing tickets this unblocks
- **Registers nothing.** Every sub-question `DEC-151` named is answered here

## Context

`ADR-0130` decided a name may be changed and that the string it leaves is spent forever. It
deliberately designed nothing, because the machinery under it was built to make the change
impossible: a trigger whose stated reason was that *"every other invariant this codebase enforces
in Kotlin can be repaired by an `UPDATE`, and this one is defined by the impossibility of that
`UPDATE`"* (`ADR-0029` §4), a registry no string ever leaves (`ADR-0051` §1), and a `403` on the
endpoint whose whole cause was *this player already has a name*.

Five forces pull on the replacement, and they do not point the same way.

**The trigger's name is now a false statement, and the trigger is the product's most trusted
witness.** `ADR-0029` §4 put permanence in the database precisely so it would hold *"against a
migration, a `psql` session, a future admin tool and any second write path somebody adds in a
year"*. `ADR-0130` §5 removes `PERMANENCE_LINE` from the client because a product must not say a
false thing to a player. An operator at a `psql` prompt reading `player_display_name_permanent` on
a column that is not permanent is the same failure with a smaller audience, and it is the one
audience that acts on what it reads.

**What actually has to survive is not permanence but non-release.** `ADR-0051` §3 stated the
property twice and called it an exception both times: *"a name may be given up only by being
spent"*, and *"the only way past the trigger is to spend the string first, so a vacated name is
never a claimable name."* That sentence is true of a rename word for word. The invariant that has
to hold after `ADR-0130` is already written down; what changes is that it stops being an exception
to permanence and becomes the rule.

**A rename retires one string and spends another, and a half-done rename is worse than a refused
one.** `ADR-0051` §2 already names the defect a competent implementer ships without the rollback —
*"a registry row left behind by a refused claim permanently burns a string nobody holds"*. A rename
doubles the exposure: a transaction that retires the old string and then fails to land the new one
leaves the player holding a name the registry says is spent, which no later write can repair,
because `ADR-0051` §9 refuses an un-retire.

**`ADR-0130` §4 constrains a row the player never sees.** `ADR-0052` §1's removal notice is derived
from `name_registry`, and §4 requires it to stay silent for a player who renamed themselves —
*"whatever the mechanism writes to `name_registry`"*. So the registry row a rename writes is
constrained by a screen, and the constraint has to be met by the schema rather than by a client
remembering to check something.

**The namespace has no brake, and it did not need one before.** Under `ADR-0029` a successful name
write could happen **once per profile in its lifetime**, which is why §5 could say *"failed
attempts are not budgeted"* and be right: the successful path was self-limiting and the failing
path cost nothing. `ADR-0130` §1 removed the self-limit and left the other half — `ADR-0051` §2's
rollback still means a refusal burns nothing. So the thing that now needs metering is the
**success**, which inverts every budget this repository has shipped.

### The deadline, honestly

**One of the five has a date, and `ADR-0130` set it.** *"There is no brake in the product today.
That is the reason `DEC-151` is due **before** `STORY-1409` ships, not after it."* A `PUT` that
succeeds spends a string irreversibly; a day of `STORY-1409` shipped without a brake is a day of
strings that no decision reaches back for.

**The rest have no date but one has a direction that closes.** Whether the vacated row records
*this player left this string* is free to get right today and impossible to recover later:
`ADR-0130` chose retirement over holding partly because *"`retired_from` already records who left
which string, so a later decision may let a player re-take a name retired from them, with no data
lost"*. A mechanism that writes rename-retirements indistinguishably from takedown-retirements
destroys that fact at write time, and no migration recovers it a year later. That is a reason to
decide it now, not a reason to decide it a particular way — the argument for the particular way is
in §3.

## Decision

**A name write locks the profile, spends the new string, marks the old one `REPLACED`, and hands
the new one over — four statements in one transaction. The permanence trigger becomes the
never-released trigger: a name may leave a player only after the registry has spent it, which is
now the whole of what it says rather than an exception to what it says. `PUT /api/me/name` loses
`403` and gains `429`.**

### 1. The trigger stops saying *permanent* and says *never released*

The migration drops the trigger, then the function, then creates both under names that are true:

```sql
DROP TRIGGER player_display_name_permanent ON player;
DROP FUNCTION player_display_name_is_permanent();

CREATE FUNCTION player_display_name_is_never_released() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        IF NOT EXISTS (
            SELECT 1 FROM name_registry
             WHERE name = OLD.display_name AND reason IN ('RETIRED', 'REPLACED')
        ) THEN
            RAISE EXCEPTION 'a display name is spent before it is left (ADR-0051, ADR-0134)'
                USING ERRCODE = 'restrict_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER player_display_name_never_released
    BEFORE UPDATE OF display_name ON player
    FOR EACH ROW EXECUTE FUNCTION player_display_name_is_never_released();
```

**The body is `ADR-0051` §3's with one disjunct deleted.** `NEW.display_name IS NOT NULL OR` is
gone, and the guard that survives is the one `ADR-0051` §3 wrote the exception around. That is the
whole behavioural change, and it is worth reading as a subtraction rather than a rewrite.

Exactly what it enforces now:

- **`NULL → name` succeeds**, unchanged. There is nothing to leave.
- **`name → the identical name` changes nothing**, unchanged — `IS DISTINCT FROM` is false, so the
  guard is not reached. `ADR-0029` §5's idempotent retry is untouched, and identity is still exact
  equality of the canonical form.
- **`name → a different name` succeeds only if the old string is already `RETIRED` or `REPLACED`.**
  A rename must spend the string it leaves, in the same transaction, before the column moves. No
  `psql` session, migration, admin tool or second write path can move a name off a player without
  retiring it first.
- **`name → NULL` is unchanged**, so `retire_display_name` is still the only route to nameless and
  `ADR-0130` §1's *a change never leaves the player nameless* is a property of the schema rather
  than of the write path's care. **`retire_display_name` is not edited**: its statement order still
  satisfies this guard, because it promotes the registry row before it nulls the column.
- The `OF display_name` clause stays, so the coin write on every finished duel still does not fire
  it. The error code stays `restrict_violation` (`23001`), matched on the code and never on the
  message (`ADR-0029` §4).

**Why the names change rather than the body alone.** `ADR-0051` §3 replaced the body in place and
kept the names, correctly, because the property was still permanence with one hole in it. It is not
any more. A function called `..._is_permanent` guarding a column that four `PUT`s a minute may
change is the schema asserting the thing `ADR-0130` §5 deletes from the client, to the one reader
who can act on it. The cost is three extra DDL statements and one assertion in `MigrationsTest`,
and this is the last moment the rename is free — after `STORY-1409` there is a merged migration
whose name is wrong and no reason large enough to write another.

### 2. A name write is four statements in one transaction

`PostgresProfileWrites` keeps its shape — `ADR-0011`'s repository boundary, `SQLSTATE` read from
what PostgreSQL returns and never from a message, a sealed result, and no `SQLException` escaping
for a refusal. The statements become:

```sql
-- 1. Lock the profile and read the name it holds. The player row is locked before any registry
--    row: that is the order retire_display_name already takes, and it is stated as a rule rather
--    than left to a case analysis a fifth statement could invalidate.
SELECT display_name FROM player WHERE id = ? FOR UPDATE;

-- 2. Spend the new string, or fail: 23505 means the namespace has already spent it.
INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN');

-- 3. Only when statement 1 returned a name — mark the string it is leaving.
UPDATE name_registry SET reason = 'REPLACED', retired_from = ? WHERE name = ?;

-- 4. Hand the new string over.
UPDATE player SET display_name = ? WHERE id = ?
RETURNING id, coin_balance, display_name, <ADR-0053's device_route_live EXISTS>;
```

| What happens | `SetNameResult` | HTTP |
| --- | --- | --- |
| One row from statement 4 | `NameSet(profile)` | `200` |
| `23505` from statement 2, and this player already holds that exact canonical form | `NameSet(profile)` | `200` — the idempotent retry, unchanged |
| `23505` from statement 2, in every other case | `NameTaken` | `409` |
| Statement 1 returns no row | — | `500`, on the `check` `readProfile` already carries |

- **Statement 4 loses `AND display_name IS NULL`.** That predicate was `ADR-0029` §5's whole
  mechanism for the `403`, and `ADR-0130` §1 abolished its only cause. Statement 1's `FOR UPDATE`
  replaces it as the concurrency interlock, which is the stronger of the two jobs it was doing.
- **`SetNameResult.AlreadyNamed` is deleted**, and with it the `403` branch in `ProfileRoutes`. The
  port answers `NameSet | NameTaken` and the `when` stays exhaustive with no `else`, so a third
  case added later fails to compile rather than falling through.
- **Statement 1 is the interlock, and `FOR UPDATE` is why there is no fifth outcome.** Two
  concurrent `PUT`s from one player are both legal now. The second blocks at statement 1 until the
  first commits, then reads the name the first left and replaces *that*. Without the lock both
  would read the same old string, the second's statement 3 would meet a row that is no longer
  `TAKEN`, and there would be a lost-update state to invent an answer for. There is no such state.
- **Statement 1 finding no row is unreachable and is a `500` anyway.** `ADR-0039` deletes no
  profile and the route resolved identity before the body was read; the `check(rows.next())` the
  shipped `readProfile` already uses is the right shape, and a fabricated refusal would be worse.
- **Statement 3 carries no `AND reason = 'TAKEN'`, deliberately.** With the predicate, a row that
  is somehow not `TAKEN` updates zero rows in silence and statement 4's trigger then *passes*,
  because a `RETIRED` row satisfies §1's guard — a rename that never recorded what it spent.
  Without it, `ADR-0051` §3's monotonicity trigger raises `23001` and the whole call fails loudly.
  An impossible state must be loud.
- **`ADR-0051` §2's rollback now protects two strings, not one**, and it is more load-bearing than
  it was. Anything other than one row from statement 4 rolls the transaction back: the new string
  is un-spent *and* the old string goes back to `TAKEN`, because its promotion was statement 3 of
  the same transaction. Left out, a failed rename strands a player holding a string the registry
  says is spent, and `ADR-0051` §9 refuses the un-retire that would fix it.
- **Statement 2 stays first among the writes**, so the common refusal — the name is spent — is
  decided before anything else is touched, and `ADR-0051` §2's spend-then-hand order is unchanged.
- **The transaction writes exactly one `player` column and reads no other.** `ADR-0051` §7's writer
  list keeps its four entries: this is the same `SET display_name` statement with a different
  `WHERE`, not a fifth writer. `ADR-0030` §5's P1 and P2 hold — nothing here reads or writes
  `coin_balance` or `duel_result`.

### 3. The vacated row moves `TAKEN → REPLACED`, and `retired_from` is populated

`name_registry` gains a fourth `reason`. The string is spent for everyone under `ADR-0051` §1's
folded index, which does not consult `reason` at all — so `ADR-0130` §2 holds in full, and the row
also records *which* of the two ways the name left its holder.

```sql
ALTER TABLE name_registry DROP CONSTRAINT name_registry_reason;
ALTER TABLE name_registry ADD CONSTRAINT name_registry_reason
    CHECK (reason IN ('TAKEN', 'BLOCKED', 'RETIRED', 'REPLACED'));

ALTER TABLE name_registry DROP CONSTRAINT name_registry_retired_from;
ALTER TABLE name_registry ADD CONSTRAINT name_registry_retired_from
    CHECK (retired_from IS NULL OR reason IN ('RETIRED', 'REPLACED'));

CREATE OR REPLACE FUNCTION name_registry_is_monotone() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.reason <> 'BLOCKED' THEN
            RAISE EXCEPTION 'a name that has been held is never released (ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
        RETURN OLD;
    END IF;
    IF NEW.name <> OLD.name
       OR OLD.reason <> 'TAKEN'
       OR NEW.reason NOT IN ('RETIRED', 'REPLACED') THEN
        RAISE EXCEPTION 'a registered name may only go TAKEN to RETIRED or REPLACED (ADR-0134)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

- **`REPLACED` means *its holder replaced it*; `RETIRED` keeps its one meaning — *an operator took
  it away*.** Both are terminal, neither may become the other, no row may be deleted but a
  `BLOCKED` one, and no `name` may ever be rewritten. `ADR-0051` §1's *a string that enters that
  table never leaves it* is not amended, and its monotonicity is not weakened: one more terminal
  state is reachable from `TAKEN`, and nothing is reachable from either terminal state.
- **The word is not `RELEASED`, and that is not a style choice.** In this repository's vocabulary
  *released* is the answer `ADR-0130` §2 refused — *given back for anyone to take* — and a `psql`
  session showing `reason = 'RELEASED'` on a string nobody may ever have would be the schema
  asserting the opposite of the decision. `REPLACED` says what happened and cannot be misread as
  availability.
- **`retired_from` is populated on a rename**, holding the player who left the string, exactly as
  `ADR-0051` §4 populates it for a takedown. `ADR-0051` §1's *"record-keeping, not a source of
  truth … so an operator can answer whose name was this six months later"* is the reason it exists
  and it applies unchanged; nothing in production reads it except §4's derived bit.
- **The distinction is what keeps `ADR-0130`'s own door open.** `ADR-0130` chose retirement over
  holding on the ground that *"a later decision may let a player re-take a name retired from them,
  with no data lost"*. A future ADR wanting that must not also hand back names an operator removed
  (`ADR-0038`: *retired forever, including the player it was taken from*), so it needs to tell the
  two apart. Collapsed into one `reason`, they never can be — the fact is destroyed at write time.
- **Nothing that reads the registry breaks, by construction.** `ADR-0051` §1's design is that a
  write path *"does not check three things. It inserts one row, and the index refuses"* — the
  folded unique index is indifferent to `reason`, so a fourth value cannot make a spent string
  claimable. The only production reader of `reason` is `ADR-0053` §3's expression, and §4 is what
  it does.
- **The partial index is unchanged and now carries rename rows too.**
  `name_registry_retired_from_idx` is `WHERE retired_from IS NOT NULL`, so a `REPLACED` row writes
  an entry where a `TAKEN` row did not. `ADR-0053` §3 sized it for the profile read, which is hot;
  a name write is not, and the profile read's `reason = 'RETIRED'` filter still matches the partial
  index. Narrowing the index predicate is available later and buys nothing on a table this size.

### 4. `ADR-0052` §6's bit reads false for a renamer, structurally

`ADR-0130` §4 requires `displayNameRemoved` to stay false for a player who renamed themselves.
**`PostgresProfileReads.PROFILE_OF_SQL` and `PostgresProfileWrites` change not at all**, and that
is the answer rather than a coincidence to be re-derived by whoever reads this next.

- **The read already carries the conjunction.** `ADR-0053` §2 put both halves in the server:
  `(p.display_name IS NULL AND EXISTS (… r.retired_from = p.id AND r.reason = 'RETIRED'))`. A
  renamer holds a name, so the first conjunct is false; and their row is `REPLACED`, so the second
  is false as well. **Two independent reasons, and the ticket asserts both** — a test that only
  pins the conjunction would pass on a mechanism that wrote `RETIRED`.
- **`ADR-0053`'s comment that `r.reason = 'RETIRED'` is *"redundant under the partial index … and
  stays"* stops being redundant.** It is now the clause that distinguishes the two ways a string
  left a player, and the comment is corrected to say so rather than left describing a world in
  which it does nothing.
- **The bit cannot read `true` for a player no operator acted on.** It requires
  `display_name IS NULL`, and §1 keeps `retire_display_name` the only writer of that value. A
  player who renamed and was *later* taken down reads `true`, which is correct — they were taken
  down.
- **`PostgresProfileWrites` keeps its `false` literal** for `displayNameRemoved` on every `200`.
  Its shipped comment — *"`NameSet` describes a player who now holds a name … so
  `displayNameRemoved` is false by construction"* — is as true of a rename as of a first name, and
  `ADR-0053` §6's prohibition on a second query or a subquery in `RETURNING` is untouched.
- **`ADR-0053` §2's table gains a fifth row**, in that ADR's index entry and in `docs/protocol.md`:
  *holds a name, replaced one by renaming → `false`*. A reader who now knows a rename writes
  `retired_from` will otherwise conclude the field is broken, so the row exists to stop that
  reading, not because the field's meaning moved.

### 5. `PUT /api/me/name` answers `200 | 400 | 401 | 409 | 429`, and `403` leaves

`ADR-0130` superseded `ADR-0029` §5's `403` row; this is what stands in the table now.

| Outcome | Answer |
| --- | --- |
| Absent, blank or unknown credential | `401`, empty body — unchanged |
| Name set, first or twentieth | `200 OK` with `ProfileResponse` and the canonical `displayName` |
| Empty after trim, over 32 code points, or a refused character | `400`, empty body — unchanged |
| The fold collides with **any** spent string — held, blocked, retired or replaced | `409 Conflict` |
| Over the write budget (§6) | `429 Too Many Requests`, empty body, no `Retry-After` |

- **`403` has no cause and is deleted, not repurposed.** Holding a name is no longer a reason a
  name write is refused, and there is no other state this endpoint can reach in which no state
  change is possible. `docs/protocol.md`'s `403` row goes with it.
- **A player changing only the case of their own name is `409`, not `200`.** `Bob` sending `bob`
  collides with the `Bob` registry row under `ADR-0029` §1's fold, which is what *spent* means, and
  it does not equal the string they hold, so it is not the idempotent retry. This case answered
  `403` before. It is a consequence of `ADR-0130` §2 and `ADR-0029` §1 together, and *Consequences*
  records it as a cost rather than smoothing it over.
- **No answer says which source refused.** `ADR-0051` §2's rule is unchanged: held by somebody
  else, blocked, retired by an operator, or replaced by this very player — all `409`. `ADR-0052`
  §7's corrected sentence, *"That name is not available. Try another."*, is already true of all
  four.
- **`docs/protocol.md` gains the `429` row and loses the `403` row**, and two sentences elsewhere
  that give *permanence* as a rule's reason are repaired rather than deleted: the doubled-space
  rule and `DisplayName.kt`'s KDoc both justify themselves by *"permanent once set"*, and the true
  reason is now *spent forever once written* — a doubled-space variant burns a second string that
  renders identically. **The rules do not change; their stated reason does.**
- **The client's `SetNameOutcome` loses `permanent` and gains `throttled`** (429), the same kind
  `sign-up.ts` already models. Removing `case 403` leaves no hole: `set-name.ts`'s `default`
  already maps an unmodelled status to `unavailable`. `PERMANENCE_LINE` and `refusalSentence`'s
  `permanent` sentence leave the product, which is `ADR-0130` §5's, not this ADR's. **The words of
  the `throttled` sentence are the design card's** (`ADR-0091` §2); what is fixed here is that it
  is a statement about *requests*, not about the name — a `429` invalidates nothing the player
  typed.

### 6. The write is budgeted, and the budget meters spending rather than trying

**Yes, the write needs a budget, and it is `ADR-0022`'s shape keyed by `PlayerId`** — the route
`ADR-0029` §5 named as additive, and the one `ADR-0130` pointed at: *"a rate limit on the write,
which is a refusal about requests rather than a rule about names, needs no vocabulary on any
screen"*.

- **`AttemptBudget` is reused, not rebuilt.** A fifth `AttemptLimits` — `nameWriteMaxAttempts`
  (default `5`) and `nameWriteWindowMillis` (default `60_000`) on `ServerConfig`, bundled by
  `nameWriteLimits()`, with its **own** `AttemptBudget` instance in `ServerComponents` beside the
  four that ship. A shared instance would let a name write spend sign-in's budget, which
  `ServerComponents` already warns about in as many words.
- **The key is `PlayerId`, and the check runs after identity is resolved.** A stranger must never
  reach a `429`: they get `401`, before the body is read, exactly as today. `AuthRoutes` records
  the defect this ordering avoids — *"a budget checked earlier turns a `401` into a `429` and vice
  versa"*.
- **It meters the string that was spent, not the attempt that was made — and that inverts every
  other budget here.** `ADR-0022` meters *failed* joins because guessing is the attack; `ADR-0074`
  refunds a *successful* sign-in for the same reason. Here it is the reverse: a `400` or a `409`
  costs the namespace nothing, because `ADR-0051` §2 rolls it back and says so, while a `200` burns
  a string forever. A player hunting for an available name is doing what the product asks of them
  and must not be throttled for it.
- **Mechanically that is `ADR-0074` §2's reserve-then-refund**, not a post-hoc record: `admit`
  before `writes.setDisplayName`, `refund` on any result that is not `NameSet`. The reservation
  also bounds how many name writes one player can have in flight, which matters because
  concurrency is the only way to race a budget.
- **The numbers are chosen to be unreachable by a human**, which is the line that keeps this a rate
  limit rather than the quota `ADR-0130` §1 refuses: nobody types and submits five distinct names
  in a minute, and a throttled player waits under a minute rather than planning around a rule.
  `ADR-0022`'s own test applies — *"deliberately generous against human typo rates; if it ever
  bites a real player, it is too tight"* — and it is then a configuration change, not a decision.
- **The form says nothing about it in advance**, so `ADR-0130` §5's two obligations are untouched
  and no *changes remaining* counter exists. A rate limit is only ever seen after the fact.
- **What it does not solve, said plainly: squatting.** Device ids are trivially mintable
  (`ADR-0012`), so a per-player key is diluted exactly as `ADR-0022` §2 recorded, and the
  volumetric case is the deployment front-end's. What this buys is that one browser burns the
  namespace at a human rate; it does not make `ADR-0029`'s *"a second, irreversible thing to farm"*
  untrue.

### 7. Nothing crosses the socket

Stated plainly because `STORY-1409`'s ticket shape depends on it:

- **`PROTOCOL_VERSION` does not move.** `ADR-0020` emits `protocol.gen.ts` from `ClientMessage` and
  `ServerMessage` alone; `ProfileResponse` and `SetNameRequest` are plain HTTP DTOs and neither
  changes shape here. `ADR-0130` §3 measured the other half on `develop` at `e7061e7b` — no
  `ServerMessage` carries a display name — and nothing here adds one.
- **`STORY-1409`'s tickets are not `atomic:`** under `ADR-0068` §3. There is no merged gate
  forbidding a split, so they stay inside the ordinary `files_touched: 1..3` cap.
- **A rename is invisible at the table**, which is `ADR-0130` §3's and needs no mechanism: a duel in
  progress carries no name to change.
- **`poker-engine` learns nothing.** A duel is played by two seats; what they are called is a
  server fact.
- **The migration can land before the write path, and the product does not change until the write
  path does.** With the trigger relaxed and `PostgresProfileWrites` still carrying
  `AND display_name IS NULL`, a named player still gets zero rows and still sees the shipped
  behaviour. That is an ordering the planner may use; it is not an instruction to use it.

### 8. What is deliberately not built

- **No guard that the name a player *takes* is `TAKEN`.** The trigger governs the string being
  left, not the one arriving; the foreign key only proves the arriving string is registered, so a
  direct `psql` `UPDATE` could still hand a player a `BLOCKED` or `RETIRED` string. That hole
  predates this ADR, is unreachable from the server (statement 2 takes `23505` first), and closing
  it would newly constrain every fixture that writes a name. It is additive later and it is named
  here so nobody reads its absence as an oversight.
- **No un-retire, no release, no reclaim, no un-replace.** `ADR-0051` §9 refused them and
  `ADR-0130` §7 refused them again.
- **No operator rename**, and no third argument on `retire_display_name`. `docs/operations.md`
  gains no step: nothing an operator does changes.
- **No column recording *when* or *how many times* a player renamed.** `created_at` on the registry
  row already dates the spend, nothing reads it, and a count would be the counter `ADR-0130` §1
  refuses under another name.
- **No enumeration surface.** Nothing lists a player's `REPLACED` rows, no response carries one,
  and `ADR-0029` §5's refusal of an availability endpoint stands.
- **No `Retry-After` on the `429`**, matching `ADR-0056`'s shipped sign-up answer.

## Consequences

**What it buys.** `STORY-1409` is startable with every statement, status and constraint named, and
it ships with a brake, so the namespace burn `ADR-0130` opened has a bound from the first day
rather than from a follow-up nobody files. The schema stops asserting a decision the product has
reversed: an operator reading `player_display_name_never_released` learns the invariant that is
actually true, and the invariant is stronger than it looks, because it now holds with **no
exception at all** — the hole `ADR-0051` §3 had to cut is the rule. `ADR-0130` §4's constraint is
met by two independent mechanisms rather than one. And the door `ADR-0130` leaned on when it chose
retiring over holding stays open, because `REPLACED` records which of the two ways each string left
its holder.

**What it costs.**

- **A player can never change the case of their own name.** `Bob` cannot become `bob`: the fold
  that makes a string spent does not care who spent it. This is derived from `ADR-0130` §2 and
  `ADR-0029` §1, and no mechanism under those two can avoid it — changing it means reopening
  `ADR-0051` §1's *a string that enters that table never leaves it*, which is not a mechanism
  question. The player is told *"That name is not available"*, which is true and unhelpful, and it
  is the likeliest `409` a real player will ever see.
- **A name write now takes a row lock on `player` and holds it across a registry insert.** If that
  insert waits on another transaction's uncommitted claim of the same string, a concurrent
  coin-balance write for the same player waits behind it. It is milliseconds and it is the same
  lock order `retire_display_name` takes, but it is a new interaction between the identity path and
  the ledger path, and `ADR-0030` §2's enumeration exists precisely so such things are noticed.
- **Four statements where there was one.** `ADR-0029` §5 opened with *"the write is one statement
  and the index is the reservation"*; `ADR-0051` §2 made it two; this makes it four, plus a lock.
  The reservation is still the index — nothing about the concurrency argument changed — but the
  write path is now something a reader has to follow rather than something they can see.
- **An idempotent retry spends budget.** The port cannot tell *the player re-sent their own name*
  from *the player renamed* without a new `SetNameResult` case, so a player pressing save on their
  existing name six times in a minute meets a `429` on a request that would have changed nothing.
  `set-name.ts` never retries, so no client produces this; a human can.
- **A fourth `reason` is a schema word that must be kept true.** Every future query over
  `name_registry` has to decide whether it means *spent* (any row), *terminal* (`RETIRED` or
  `REPLACED`) or *taken away by an operator* (`RETIRED`), and getting that wrong in a read path is
  how a renamer would be shown a moderation notice. The folded index makes the *spent* case
  impossible to get wrong; the other two are on the reader.
- **Shipped, tested strings and one shipped status are deleted.** `SetNameResult.AlreadyNamed`,
  `ProfileRoutes`'s `403` branch, `set-name.ts`'s `permanent` outcome, `refusalSentence`'s
  `permanent` sentence, `PERMANENCE_LINE`, and `docs/protocol.md`'s `403` row all go, and
  `MigrationsTest` learns two new identifiers.

**What it forecloses.**

- **A rename that does not spend the string it leaves**, from any connection, for as long as §1's
  trigger stands. Reversing that is a migration and an ADR, and it would have to say what happens
  to the rows the previous holder printed — which is `ADR-0130` §2's question, not this one's.
- **Recovering, later, which retirements were renames** — if the `TAKEN → RETIRED` alternative had
  been taken. Taking `REPLACED` forecloses nothing in the other direction: a later decision that
  stops caring can read the two as one set in a single predicate.
- **`AlreadyNamed` as a name for anything else.** It is deleted rather than retired to a new
  meaning, so a future refusal that wants `403` on this endpoint arrives without a misleading
  precedent.

**What this does not settle.**

- **The words of the `throttled` sentence**, and everything else `ADR-0130` §5 left to the design
  card under `ADR-0091` §2.
- **Whether the budget's numbers are right.** They are configuration with defaults, and the first
  real player they bite is the evidence that they are too tight.
- **Whether a player may ever re-take a name they replaced.** `REPLACED` makes it answerable;
  nobody has asked, `ADR-0130` §7 refuses it today, and it is the product owner's if anyone does.

## Alternatives considered

**Keep the trigger's name and replace only its body**, as `ADR-0051` §3 did. Its strongest case is
precedent and cost: `CREATE OR REPLACE FUNCTION` is one statement, the trigger binds by name and is
not re-created, no test changes, and a comment in the migration can explain that *permanent* is now
historical — which is exactly what an ADR-numbered comment is for, and this repository has done it
before, in this very file. Rejected because the two situations are not alike. `ADR-0051` left a
function whose name was still broadly true, with one scoped exception; this leaves a function
called `is_permanent` guarding a column any player may change at will. The whole reason `ADR-0029`
§4 chose a trigger over application logic is that the database is the artefact that survives every
rewrite and is read by whoever has a `psql` prompt — and it is read as a statement of intent.
Shipping a false one to save three DDL statements trades the cheapest thing here for the most
expensive.

**`TAKEN → RETIRED` for a rename, with `retired_from` populated** — the answer `DEC-151` named. Its
strongest case is that it is what `ADR-0130` §2 says in words: the string *"joins the same set a
taken-down name joins"*, *"one rule for both ways a name can leave a player, and no new vocabulary
for either"*, and `ADR-0051` §1 is *"not amended"*. It needs no `CHECK` edit, no monotonicity
change and no new schema word to keep true, and `ADR-0130` §4's constraint is still met, because
`ADR-0053` §3's first conjunct is already in the shipped query — a renamer holds a name, so the bit
is false whatever the row says. Rejected on two grounds. First, it destroys at write time the one
fact `ADR-0130` said made retiring the cheaper mistake — *"`retired_from` already records who left
which string, so a later decision may let a player re-take a name retired from them"* — because a
future ADR must not hand back what an operator removed, and after a year of renames nothing can
tell the two apart. Second, it leaves `ADR-0130` §4 resting on a single conjunct in one SQL string:
a later query asking *was this player ever moderated* would call every renamer moderated, and the
prohibition would be a comment rather than a fact. `REPLACED` costs four `ALTER`s and makes both
true structurally. **`ADR-0130` §4's own wording — *"whatever the mechanism writes to
`name_registry`"* — is what licenses answering wider than the question was framed.**

**A `retire_and_rename` function in the schema, called by the server**, the way
`retire_display_name` is called by an operator. Its strongest case is real: the ordering, the
atomicity and the interlock would live in the migration where `:poker-server:check` can hold them,
which is `ADR-0051` §4's own stated reason for preferring a function to a wrapper, and the four
statements above could not then drift apart in a Kotlin refactor. Rejected because
`retire_display_name` earns its place by being *unreachable from the server* — `STORY-0410` carries
a test that its name appears nowhere under `poker-server/src/main/kotlin`, and that is what makes
it an operator path rather than an endpoint whose body could grow. A function on the hot write path
would be a fifth writer of `player` that `ADR-0051` §7's list cannot point at from Kotlin, and
`ADR-0011` puts the SQL in `duels.poker.server.db`, where it already is.

**One statement, with data-modifying CTEs** — spend, retire and hand over in a single `WITH`. Its
strongest case is that it is genuinely atomic without a transaction block, needs no explicit lock,
and makes the four-statement ordering unforgeable. Rejected because sub-statements of a
data-modifying CTE all see the same snapshot and cannot see each other's effects, while the
`player` trigger fires inside the main `UPDATE` and must observe the retirement the same statement
is performing — a dependency whose correctness rests on PostgreSQL behaviour that is subtle to
state and worse to test. `ADR-0051` §2 already chose statements in a transaction for this write,
and a mechanism nobody can reason about at a glance is the wrong place to save a round trip.

**No budget — ship `STORY-1409` and add a brake if abuse appears.** Its strongest case is
`ADR-0029` §5's own posture, quoted verbatim in `DEC-151`: *"failed attempts are not budgeted; if
abuse appears, the budget takes `ADR-0022`'s shape"* — YAGNI, no players yet, and a limiter is
strictly additive later, so nothing is foreclosed by waiting. Rejected because that sentence was
written about a write that could **succeed once per profile in its lifetime**: the successful path
needed no brake because it had one built in, and `ADR-0130` §1 removed it. The asymmetry is the
same one `ADR-0130` used to choose retirement: a brake that turns out to be unnecessary is deleted
in one diff, while every string burned without one is burned forever, since `ADR-0051` §9 refuses
the un-retire. `ADR-0130` attached a deadline to this exact question and said it is due *before*
`STORY-1409` ships; answering *not yet* would make that deadline mean nothing.

**A budget that counts every attempt, not only the ones that spend a string.** Its strongest case
is simplicity and consistency: it is what `AuthRoutes` does, it needs no refund call, and it also
caps the `409`-hunting a squatter would use to probe which short names are still free — which is
real enumeration, even if `ADR-0029` §5 accepted enumeration on the ground that a leaderboard
publishes names anyway. Rejected because a player who cannot think of an available name is doing
exactly what the product asks, and throttling them punishes the failure while the harm is entirely
in the success. `ADR-0051` §2 states outright that a refused claim burns nothing; a budget that
meters refusals would be metering the one thing that is free.
