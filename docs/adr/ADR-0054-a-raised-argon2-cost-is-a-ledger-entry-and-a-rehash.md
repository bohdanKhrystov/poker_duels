# ADR-0054 — A raised Argon2 cost is a ledger entry and a rehash on the next sign-in

- **Status:** Accepted — amends [`ADR-0027`](ADR-0027-the-session-outranks-the-device-id.md) §1
- **Date:** 2026-08-17
- **Resolves:** `DEC-044`
- **Constrains:** the day `ARGON2_MEMORY_KIB`, `ARGON2_ITERATIONS` or `ARGON2_PARALLELISM` changes.
  Nothing is built today

## Context

Two shipped documents disagree, and the disagreement is dormant only because nobody has raised the
cost.

`ADR-0027` §1 says the parameters travel in the stored PHC string precisely so that raising them is
*"a constant change plus a rehash on next successful verify, never a migration"*. A rehash on verify
requires a parser that **accepts** a string carrying parameters other than the current ones — you
cannot verify a legacy row you refuse to read.

`STORY-0403` shipped the opposite, deliberately. `parseArgon2PhcOrNull` compares the three fixed
sections — `argon2id`, `v=19`, `m=19456,t=2,p=1` — as **whole literal strings** against the current
constants:

```kotlin
if (parts[1] != "argon2id") return null
if (parts[2] != "v=$ARGON2_VERSION") return null
if (parts[3] != "m=$ARGON2_MEMORY_KIB,t=$ARGON2_ITERATIONS,p=$ARGON2_PARALLELISM") return null
```

`TASK-040306`'s reasoning is good and survives this ADR: a helper that accepts `m=8` is a downgrade
attack hiding in a utility function, and comparing whole strings means `m=019456`, `m=19456, t=2,
p=1` and `t=2,m=19456,p=1` are all refused without a number parser existing for anyone to fool. It
was the conservative side, taken because loosening a refusal is additive and no row with other
parameters has ever existed.

What makes this a real decision rather than a preference is what happens on the day the constants
change. Today that day is **one commit away and silently catastrophic**: someone edits three
constants, watches `Argon2PhcEncodeTest`'s two literal PHC strings go red, updates the literals to
match, and merges green — and from that moment every existing credential row fails to parse,
`matches` answers `false`, and every account created before the raise is locked out with no error
anyone can read. `ADR-0031` makes the recovery email optional and says declining it means no
recovery path at all; `ADR-0039` says there is no deletion and no support desk. So the lockout is
permanent for whoever declined the email, and `ADR-0051` retires a display name forever, so those
players cannot even re-create the identity they lost.

The forces actually in tension:

- **A downgrade must stay impossible, and history must stay readable.** These pull opposite ways
  only if the mechanism tries to distinguish them by *strength*, because a historical parameter set
  is weaker than the current one by definition — that is what raising means.
- **The upgrade needs the plaintext.** There is no background job that can fix this. A new hash
  cannot be computed from an old hash; it needs the password, which exists on the server for the
  few milliseconds of a sign-in and never anywhere else. That single fact is why `ADR-0027` §1 says
  *"on next successful verify"* and why the mechanism must live on the verify path.
- **The verify path is deliberately expensive and deliberately narrow.** `TASK-040309` bounds Argon2
  to four concurrent operations. A rehash puts a second Argon2 operation on a path sized for one.
- **`verify` writes nothing today.** `Credentials.verify` returns `PlayerId?` and
  `PostgresCredentials.verify` is one `SELECT`. A rehash makes a read path write.
- **No hash leaves `duels.poker.server.db`** (`ADR-0027` §1, structural, with `TASK-040314`
  asserting it over the public API). That forecloses two of the three obvious homes for the rehash
  before the argument starts.
- **A row may outlive every attempt to fix it.** There is no expiry on `credential`, no sweep, and
  no deletion. A dormant row persists until its owner signs in, which may be never.

### The deadline, honestly

There is none in the usual sense. Nothing here gets more expensive by waiting, because no row can
carry parameters other than the current ones until the day the constants change — and that is the
same day the code would change. What has a deadline is the **decision**, not the code: if this is
still open on raise day, the PR that raises the cost is also the PR that invents the mechanism,
written under whatever pressure prompted the raise. That is the wrong moment to be designing a
downgrade guard.

## Decision

**A cost parameter set is a row in a closed, append-only ledger in source; a stored hash at any
ledger entry verifies; and a successful verification at anything but the newest entry rewrites the
row at the newest entry. Nothing is built until the day the cost is raised.**

### 1. The mechanism is the parser plus a rehash, not a dual path and not a reset

`parseArgon2PhcOrNull` gains a set of accepted cost sections, and `PostgresCredentials.verify` gains
one conditional `UPDATE`. The other two candidates are rejected in *Alternatives considered*: a
dual-parameter verify path pays two Argon2 operations on every wrong password and lets a stored
string stop being authoritative about how its own tag was computed; a forced reset destroys every
account without a recovery email.

The algorithm section (`argon2id`) and the version section (`v=19`) stay **single literal
comparisons**. This ledger models *cost* and nothing else.

### 2. The ledger: an ordered list of triples, and the strings are generated, never typed

In `Argon2Phc.kt`, beside the constants it replaces:

```kotlin
internal data class Argon2Cost(val memoryKib: Int, val iterations: Int, val parallelism: Int) {
    fun section(): String = "m=$memoryKib,t=$iterations,p=$parallelism"
}

/** Every cost this project has ever written, oldest first. Append only; the last is what we write now. */
internal val ARGON2_COSTS: List<Argon2Cost> = listOf(Argon2Cost(19456, 2, 1))

internal val ARGON2_CURRENT_COST: Argon2Cost = ARGON2_COSTS.last()
```

Three properties, each load-bearing:

- **The parser still compares whole strings.** `parts[3]` is matched against `cost.section()` for
  each entry, and the entry that matched is carried in the returned `Argon2Phc`. There is still no
  number parser on the verify path for an attacker-supplied string to fool — the only change is
  that one literal comparison becomes *n*.
- **The strings are derived from the triples by the same `section()` the encoder uses.** The ledger
  therefore cannot describe a string this project's encoder could not have emitted. A hand-typed
  literal in an allow list is a place for a typo to become an accepted format; there is no literal
  to typo.
- **`Argon2Phc` carries its cost**, defaulting to `ARGON2_CURRENT_COST`, so `encode()` round-trips a
  parsed legacy string byte-identically and the only way to *write* a legacy string is to ask for
  one by name. `Argon2Hasher.tagFor` takes the cost as an argument: `matches` passes the one it
  parsed out of the stored string, `hash` always passes `ARGON2_CURRENT_COST`. From the raise commit
  onward every new row is written at the new cost.

### 3. What refuses a downgrade is a floor that never moves — not a comparison with the current cost

The safety property cannot be *"every accepted set is at least as strong as the current one"*, which
is unsatisfiable: every historical entry is weaker than current, and that is the entire point. So
the ledger does not compare against current at all. It compares against a fixed floor:

```kotlin
/** The first parameters this project shipped, and OWASP's Argon2id baseline. This never moves. */
internal val ARGON2_FLOOR: Argon2Cost = Argon2Cost(19456, 2, 1)
```

One test, over the whole list, asserts all of:

1. **Every entry is at or above the floor**: `memoryKib >= 19456`, `iterations >= 2`, and
   `parallelism == 1` exactly. Parallelism is pinned rather than bounded because more lanes is not
   *stronger* — it splits the same memory — so an ordering on `p` would be a fiction. Changing `p`
   is an amendment to this ADR, not an append to the ledger.
2. **The list strictly ascends in `memoryKib × iterations`**, so a later entry can never be cheaper
   overall than an earlier one, and duplicates are red.
3. **`memoryKib` never decreases**, which is the axis the product alone would let you trade away:
   halving memory and doubling passes keeps the product equal and is materially weaker against
   parallel hardware.
4. **The last entry is what the encoder writes** — asserted by the encoder's output containing
   `ARGON2_COSTS.last().section()`, on top of `Argon2PhcEncodeTest`'s existing literal strings.

That is precisely what stops the list acquiring a weak entry by accident, and it is worth being
exact about how each shape of accident dies. Appending `m=8` makes it the *current* cost and turns
the pinned literals in `Argon2PhcEncodeTest` red. Inserting `m=8` in the middle breaks the ascent.
Prepending `m=8` — the one an ascent check alone would happily allow, since `8 < 19456 < current` is
a perfectly ascending list — is below the floor and dies there. `m=19456,t=99999,p=1` is above the
floor and ascending and would be accepted, which is correct: it is stronger, and if we ever wrote
it, rows carrying it exist.

The floor is not proof against a determined edit — nothing in one repository is. It is proof against
the accident this decision exists to prevent, which is a weak entry arriving in a diff that a
reviewer skims because the surrounding change is routine. Lowering `ARGON2_FLOOR` is a one-line
change to a constant whose comment says it never moves, in the file this ADR names, and it is the
one line in `poker-server` where a reviewer's whole job is to say no.

### 4. The rehash happens in `PostgresCredentials.verify`, and it can never fail a sign-in

Not in `matches`: `Argon2Hasher` has no `DataSource` and must not acquire one — it is a pure
function of a secret and a string, testable without a container, and it does not know which row it
is verifying. Not in the endpoint: the endpoint is in `duels.poker.server.http` and would have to
receive or write a hash to do it, which `ADR-0027` §1 forbids structurally and `TASK-040314` asserts.
`PostgresCredentials` is the only place that holds the row's identity, the `DataSource` and the
right to touch a hash at once.

`verify`'s `SELECT` gains `id`. After a successful `matches` whose parsed cost is not
`ARGON2_CURRENT_COST`:

```sql
UPDATE credential SET secret_hash = ? WHERE id = ? AND secret_hash = ?
```

- The new value is `hasher.hash(presented)` — a fresh salt at the current cost, computed from the
  password the player has just proved. There is no other way to obtain it.
- **The third bind is the exact string that was read**: a compare-and-set. Two concurrent sign-ins of
  the same credential cannot lose an update or interleave into a torn write; the loser updates zero
  rows, which is a no-op and not an error.
- **Any `SQLException` from this statement is caught and dropped, and the `PlayerId` is returned
  anyway.** A verification that succeeded must never be turned into a failure by an optimisation.
  Nothing is logged that contains any part of either hash.
- Skipped when `secret_hash` is `NULL`, when the secret did not match, and on the no-such-account
  path — there is no row to write.

**The port's contract changes and this ADR is where that is recorded.** `Credentials.verify` keeps
its signature — `PlayerId?`, no new sealed result, no `needsRehash` — but stops being a read. Its
KDoc gains the sentence that a successful verification may rewrite the row it verified. No rehash
flag reaches `duels.poker.server.auth`: *"there is a hash and it is stale"* is one step from putting
a hash there, and the endpoint has nothing useful to do with the fact.

### 5. The dummy hash is re-minted in the same commit, and a test pins its cost

`PostgresCredentials.DUMMY_PHC` costs the no-such-account path one real Argon2 verification, which
`ADR-0027` §6 requires because without it Argon2 *is* the enumeration oracle. It is a literal at the
current parameters, and it fails in two different ways if it is left alone on raise day:

- **Under today's strict parser** it stops parsing, so `matches` returns `false` immediately having
  done no work. The defence disappears entirely and every test stays green — `TASK-040312` predicted
  exactly this and it is the reason that constant carries a paragraph of comment.
- **Under the ledger parser** it still parses, at the *old* cost. The unknown-identifier path then
  becomes measurably cheaper than every current-parameter account: a fast answer means the handle is
  free, a slow one means it is taken. That is a **stronger** oracle than the one §6 closed, and it
  grows as accounts rehash.

So the raise-day commit re-mints `DUMMY_PHC`, and a test asserts
`parseArgon2PhcOrNull(DUMMY_PHC)?.cost == ARGON2_CURRENT_COST` — the cost, not merely that it parses.
`TASK-040313`'s parity test cannot catch this: it counts *calls* through the `SecretHasher` seam, and
two calls at two different costs is still two calls.

A residual signal survives and is accepted: during the migration window a wrong password against a
legacy account costs less than an unknown handle, so timing distinguishes *"exists and has not signed
in since the raise"* from *"does not exist"*. It is bounded by the gap between two adjacent ledger
entries and it shrinks every time someone signs in.

### 6. What it costs on the verify path

- An affected sign-in performs **two** Argon2 operations: a verify at the old cost, then a hash at
  the new one. Sign-in latency for that account is the sum.
- They are **sequential** — two separate `withContext(argon2Dispatcher)` entries — so **peak memory
  does not double**. It stays `ARGON2_MAX_PARALLEL × m`. What doubles is how long an affected
  sign-in occupies a slot, so sustained sign-in throughput roughly halves for the affected cohort.
- The second operation runs **only after a correct password**, so it is not a lever for an
  unauthenticated attacker. Someone who already knows a password amplifies their own cost by two,
  and the four slots bound the absolute number regardless.
- The rehash is **not the dominant cost of raising the parameters**; `m` is. Moving 19456 → 65536 KiB
  takes peak memory across four slots from ~76 MiB to ~256 MiB, and `ADR-0027` §1 chose 19 MiB and
  not 64 *"against a small host"*. **The raise-day commit therefore re-derives `ARGON2_MAX_PARALLEL`
  from the host it will run on**, in the same change. `Argon2ConcurrencyTest` asserts the observed
  peak equals `ARGON2_MAX_PARALLEL`, reading the constant — it proves the bound is honoured, never
  that the bound is affordable, so nothing in CI will tell you the host cannot pay for the new
  parameters.

### 7. A dormant row keeps its old cost forever

There is no expiry on `credential`, no sweep, and no deletion (`ADR-0039`). A row written under the
first cost survives until its owner signs in, which may be never. The ledger is therefore append-only
and **never shrinks on its own**: every cost this project has ever shipped stays in the source
indefinitely, and the verify path stays willing to accept the weakest of them.

The consequence has to be said plainly rather than left to be discovered. The cost parameter exists
to defend against an **offline attack on a stolen dump**. Rehash-on-verify improves that defence for
*active* accounts only; a dump taken the day after the raise yields every dormant row at the old
cost. Raising the parameters does not fix those rows. Nothing except a forced reset does.

That escape hatch stays available and is deliberately not built: removing a ledger entry is a
one-line deletion, after which rows at that cost verify against nothing and their owners are locked
out. It is a forced reset scoped to one cohort, it needs a new ADR because it destroys accounts, and
**whether that trade is acceptable is the product owner's call, not the architect's** — it is a
consequence to a player, not a fact about the software. Naming it here means the day it is wanted,
the question is already phrased.

### 8. Nothing is built now; here is the trigger and the commit

**No file changes today.** With one entry the ledger is not a ledger: the permissive parser and the
strict parser are the same function, and the floor test, the ascent test and the legacy round-trip
test all quantify over a single element, which proves nothing about the case they exist for. A guard
whose only test datum is the value it is guarding is vacuous, and shipping the *shape* of a
downgrade-tolerant parser whose refusal has never been demonstrated is strictly worse than shipping
today's refusal.

**The trigger** is the first change to `ARGON2_MEMORY_KIB`, `ARGON2_ITERATIONS` or
`ARGON2_PARALLELISM`. It is already enforced without new machinery: `Argon2PhcEncodeTest` pins two
literal PHC strings containing `m=19456,t=2,p=1`, so the raise cannot merge without someone
deliberately editing an assertion about the exact string this project writes.

**The first commit of that day**, one PR, no migration and no schema change:

1. `Argon2Cost`, `ARGON2_COSTS` with the old triple and the new one, `ARGON2_CURRENT_COST` and
   `ARGON2_FLOOR` in `Argon2Phc.kt`; the three loose constants derived from the last entry or
   removed.
2. `Argon2Phc` carries its cost, defaulted to current; `encode()` uses it.
3. `parseArgon2PhcOrNull` matches `parts[3]` against each entry's `section()` and returns the one
   that matched. `parts[1]` and `parts[2]` are untouched.
4. `Argon2Hasher.tagFor` takes a cost; `matches` passes the parsed one, `hash` passes current.
5. `PostgresCredentials.verify` selects `id` and runs the compare-and-set `UPDATE` on a successful
   match below current, swallowing `SQLException`.
6. `DUMMY_PHC` re-minted at the new cost.
7. `ARGON2_MAX_PARALLEL` re-derived from the host's memory.
8. Tests: the floor/ascent/last-equals-encoder test; a below-floor string refused; a legacy string
   round-tripping through parse and encode; `matches` accepting a legacy string; `DUMMY_PHC` parsing
   **at the current cost**; and against the container, a row written at the old cost that verifies,
   is rewritten at the new cost, and verifies again afterwards.

`PROTOCOL_VERSION` does not move and `docs/protocol.md` is untouched: nothing here crosses the wire,
and `ADR-0047`'s fingerprint hashes `protocolDeclarations()`, rooted at `ClientMessage` and
`ServerMessage`, from neither of which any of this is reachable.

The operator's one useful number during the window needs no column and no tooling:
`SELECT count(*) FROM credential WHERE secret_hash LIKE '$argon2id$v=19$m=19456,t=2,p=1$%'`.

## Consequences

**What it buys.** `ADR-0027` §1's promise becomes true: raising the cost is a constant change and a
rehash, never a migration, and nobody is locked out. The strict-parser reasoning that produced
`TASK-040306` survives intact — whole-string comparison, no runtime number parser, no
attacker-influenced format. The raise stays a small, reviewable commit whose contents are written
down before the pressure arrives. And today's tree does not change, so if a better mechanism appears
before raise day, nothing has to be un-shipped: **this is the cheapest position to reverse, which is
why it was chosen on evidence this thin.**

**What it costs.**

- **`Credentials.verify` stops being a read.** The signature still says `PlayerId?` and the contract
  that it may write lives in KDoc and in this ADR. Any future caller who puts it behind a read-only
  transaction, a replica read, or a retry loop will be wrong and the compiler will not say so.
- **The weakest parameters this project ever shipped stay acceptable to the verify path forever**,
  in source, under a test whose name asserts they are accepted. An auditor reading that file finds a
  list of deliberately weak parameters and a green test proving the code accepts them — which is
  exactly the shape a downgrade attack would take. The floor is the only thing distinguishing the
  two, and it is one constant.
- **The raise protects active accounts only.** Dormant rows are as exposed after it as before, for
  as long as the account exists, which is forever.
- **Doubled Argon2 work per affected sign-in** on a dispatcher deliberately sized for one operation
  per attempt, for a window with no end date — it closes when the last legacy row is rehashed or
  abandoned, and nothing can tell those apart.
- **The `DUMMY_PHC` trap degrades a security property with every existing test still green**, in
  both directions, and the mitigation is a test that has to be written *on the day* by someone who
  read §5. That dependence on a person following an instruction is the weakest link in this
  decision, and it is recorded here rather than assumed away.
- **Machinery for a day that may never come.** If the cost is never raised, none of §8 is ever
  written — which is the argument for not writing it now, and also an admission that this ADR may
  turn out to have been the whole deliverable.

**What it forecloses.**

- **A hash-format change that is not a cost change.** The ledger models cost only; the algorithm and
  version sections stay single literals. Argon2 v1.4, a different KDF, a different tag or salt
  length, or a second `credential.kind` with its own hashing are not appends to this list and get
  their own ADR.
- **Trading memory for passes.** §3's third rule refuses a lower `m` at a higher `t` even when the
  product rises. If that trade is ever right, this ADR is amended.
- **A `p` other than 1**, for the same reason and by the same route.
- **A background migration of stored hashes**, permanently and by arithmetic rather than by choice —
  see the first alternative below.

## Alternatives considered

**Rehash in a background job that walks the table.** By far the most attractive option on its face:
it takes the doubled work off the sign-in path entirely, finishes in bounded time, needs no ledger,
and leaves no dormant row at the old cost — it answers the one thing this decision cannot answer. It
is also impossible, and the reason is worth stating because it is the first idea any reader will
have. A new hash cannot be derived from an old one; it requires the password, which exists on this
server for the milliseconds of a sign-in and must never exist anywhere else. Every option in this
ADR is shaped by that arithmetic, and it is why `ADR-0027` §1 says *"on next successful verify"*.

**A dual-parameter verify path.** Its strongest case is real: the parser stays byte-for-byte as
strict as it is today about the current parameters, and everything that tolerates a legacy row lives
in one function with `legacy` in its name that a reader cannot mistake for the main path, and that
can be deleted on a date. Rejected on both readings of what it means. If it means *"decide which
parameters to recompute under by reading the stored string"*, it **is** a parser that accepts other
parameters, spelled twice and with the acceptance rule further from the refusal rule that guards it.
If it means *"recompute at the current cost, and on failure recompute at the old one"*, it pays two
Argon2 operations for **every wrong password** — on four slots, on the path an attacker chooses —
because it cannot know the row is legacy until it has already failed; and it accepts a tag computed
under legacy parameters against a string *claiming* the current ones, which makes the stored string
stop being authoritative about how its own tag was made. That is a worse property than anything the
strict parser was protecting.

**A forced reset for affected accounts.** The strongest security posture available, and the only one
that guarantees no hash below the current cost survives anywhere: the parser stays strict, no list of
deliberately weak parameters ever exists in the source, and the verify path gains nothing.
Rejected on blast radius. `ADR-0031` makes the recovery email optional and states that declining it
means **no recovery path at all**; `ADR-0039` ships no deletion and no support desk; `ADR-0051`
retires a display name forever, so a locked-out player cannot even rebuild the identity they lost.
A reset would therefore delete an unknown fraction of accounts — a fraction fixed by how many people
declined an optional email, which nobody controls — as a side effect of routine hygiene. And a
mechanism whose per-use price is *"some players lose their accounts"* is a mechanism that guarantees
the raise never happens, which is the worst available outcome. It is kept as the explicit last
resort in §7, for the case where a parameter set is found to be actively broken rather than merely
dated, and it is the product owner's call on the day.

**A floor check with no ledger — accept any well-formed parameters at or above `ARGON2_FLOOR`.**
Genuinely tempting: no list to append to, no ordering rule, no test about history, and it accepts a
row written by any future version of this server without an edit. Rejected because it requires a real
number parser on the verify path, over a string an attacker can write if they can write a row at all
— `m=019456`, `m=+19456`, `m=19456 `, an `Int` overflow, an empty field — which is exactly the parser
`TASK-040306` congratulated itself on not having. It would also accept `m=19456,t=99999,p=1`: above
the floor, well-formed, and a self-inflicted denial of service any row-writer could plant. The ledger
keeps the whole-string comparison and needs no number parsing at runtime at all; the numbers are read
only by a test, over a compile-time constant.

**Store the cost in its own column beside the hash.** `credential.cost_version SMALLINT` naming a
ledger row, so the PHC string is not the authority. Its strongest case is operational and real: the
one number you actually want during a migration window is *"how many rows are still at the old
cost"*, and a column makes that indexable rather than a scan. Rejected because it puts one fact in
two places that can disagree, and the disagreement would be resolved by trusting the column — which
is the wrong one, since the string is what the tag was actually computed from. `ADR-0027` §1 already
chose *"the parameters travel with the row"*, and the PHC string **is** that choice. The operational
argument also evaporates: a `LIKE` against a literal prefix answers the same question today with no
migration.

**Keep the strict parser and simply never raise the cost.** Requires no code, and there is no
evidence today that the current parameters are inadequate — they are OWASP's Argon2id baseline.
Rejected because *never raise* is not a decision anyone can keep: hardware moves, and the failure
mode is not that the raise is skipped but that it happens anyway, in a hurry, as a three-constant
edit whose only resistance is two literal test strings that the person raising it will update to
match. That is one commit away right now, and it locks out every existing account.

**Build the ledger now, with one entry.** Its case is the strongest of any rejected option here: raise
day will arrive under pressure — a CVE, an audit — and machinery already in place and already tested
makes it the one-line change `ADR-0027` §1 promised. Rejected because with one entry every test that
guards it is vacuous: a floor test, an ascent test and a legacy round-trip test over a single element
that is also the current value prove nothing about the case they exist for, and a permissive parser
whose refusal has never been demonstrated on real input is a refusal nobody has tested. The deadline
argument runs the other way too — no row with other parameters can exist until the constants change,
and that is the same commit that would add the second ledger entry, so waiting costs nothing and
keeps this decision at its cheapest point to reverse.
