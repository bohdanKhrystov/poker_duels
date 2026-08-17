# ADR-0053 — The profile says the name was removed, in one correlated boolean

- **Status:** Accepted
- **Date:** 2026-08-17
- **Resolves:** `DEC-047` — by what shape does `GET /api/me` carry the fact that a name has been
  retired from the requesting player?
  [`ADR-0052`](ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §6 fixed *that* the
  profile read answers it, *that* it is one boolean about the caller and no string, and *that* it
  adds no column — and fixed nothing about how
- **Amends:** [`ADR-0051`](ADR-0051-a-name-is-registered-before-it-is-held.md) §1's bullet **"No read
  path exposes this table."** — that sentence, as an unqualified sentence, is retired and replaced by
  §4 below. The three clauses that follow it in the same bullet survive verbatim, and so does every
  other structural refusal in §1. `ADR-0052` already retired §1's *"Nothing in production reads
  it"*; this ADR is what replaces it with a rule that can be checked. `ADR-0051` §8's migration gains
  **one index** and nothing else — no column, no table, no constraint, no trigger
- **Builds on:** [`ADR-0049`](ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §5 (the
  precedent: `ProfileResponse.deviceRouteLive`, a server-computed current-state boolean from a
  correlated `EXISTS`, and *"no read path returns a device id to a caller that did not present
  one"*), [`ADR-0029`](ADR-0029-a-display-name-is-unique-and-permanent.md) §5 and §6 (`PUT` answers
  the whole profile; the server fabricates nothing for `null`),
  [`ADR-0047`](ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) (what the version ledger
  fingerprints, and therefore what it does not)
- **Constrains:** `STORY-0410` (one field, one `SELECT` expression, one index line, one
  `docs/protocol.md` row, and three criteria — one of which fails unless two fixtures share a
  database) and `PostgresProfileWrites`, which constructs the same DTO and must fill the field with a
  literal
- **No wire change.** `PROTOCOL_VERSION` stays at `2` and `docs/protocol-versions.md` gains no row —
  §5 gives the mechanism rather than asserting it

## Context

`ADR-0052` decided the player is told, and stopped exactly where the response shape starts. Its §6 is
the whole of the brief: *"What crosses the wire is one boolean about the caller, and no string"*, on
the profile read, with no column added. Four forces pull on what is left, and two of them pull
against each other.

**The fact the client needs is a conjunction, and the two halves live in different tables.**
`ADR-0052` §1 is exact: the notice shows when the player **holds no display name** *and* **at least
one name has been retired from them**. The first conjunct is `player.display_name`; the second is
`name_registry.retired_from`. Something has to compose them, and the choice of where is the decision
— because the half that goes wrong silently is *"a player who has since chosen a new name sees
nothing"*, and the wrong version of that ships a moderation notice to somebody who moved on a year
ago, forever.

**`retired_from` is a player id, and this is the first production read of it.** The column exists to
answer *whose name was this* six months later (`ADR-0051` §1). It names a **player**, and one careless
predicate turns it into a fact about a different player: `RECENT_DUELS_SQL` already joins
`player p ON p.id = o.player_id` — the *opponent's* row — so an `EXISTS ... retired_from = p.id`
pasted into that query is a takedown flag about somebody else, and it is one line. `ADR-0052` §5
makes the invisibility of the state to everyone else *"a criterion rather than an omission"*, which
means the shape has to make the leak hard rather than merely forbid it.

**`GET /api/me` is the hottest read in the product, and `name_registry` only grows.** `ADR-0052`
already recorded the cost in the abstract — *"the profile read gains a fact almost every player will
never be in, on the hottest profile query, forever."* Concretely: `name_registry` gains a row per
name ever set and never loses one (`ADR-0051`), `retired_from` has **no index** — a foreign key does
not create one — and the answer for essentially every caller is *no match*, which is the case a
sequential scan pays in full. The cheap version and the expensive version of this read look identical
in a test and diverge with the size of the table.

**The response already has a precedent, and it is one week old.** `ADR-0049` §5 put `deviceRouteLive:
Boolean` on `ProfileResponse` from
`EXISTS (SELECT 1 FROM device_binding WHERE player_id = ? AND revoked_at IS NULL)` — a
server-computed, current-state, correlated boolean that the client renders without composing
anything. Two structurally identical facts on one response should not have two different shapes.

### The deadline, honestly

**One part of this is free today and costs a migration file after `STORY-0410` merges.** `ADR-0051`
§8's migration does not exist yet — the live set is `V1`–`V4` and `STORY-0410` is unsplit — so the
index §3 needs is one line in a file nobody has run. Once that migration merges it is immutable
(`ADR-0051` §8: *"`V1`–`V4` are immutable"*), and the same index is a second migration, a second
review and a second deploy step. That is a reason to decide the index **now**; it is not a reason to
decide it a particular way, and §3 argues it on its own merits.

The rest has no date. `STORY-0410` is blocked on nothing else and the field could be added later at
the price of one more field.

## Decision

**`ProfileResponse` gains `displayNameRemoved: Boolean`, true exactly when the caller holds no
display name and a name has been retired from them, computed by one correlated `EXISTS` inside the
existing profile `SELECT`.**

### 1. The wire shape

```kotlin
@Serializable
public data class ProfileResponse(
    val playerId: String,
    val coinBalance: Int,
    val displayName: String?,
    val displayNameRemoved: Boolean,
)
```

- **`Boolean`, not `Boolean?`.** A nullable boolean is three states, and every client collapses the
  third with `?? false`, so the third state is unobservable by construction. A state no consumer can
  distinguish should not be on the wire.
- **No default value.** This is load-bearing and it is the one thing a competent implementer will get
  wrong: `Application.module()` installs `ContentNegotiation { json() }`, whose `Json` has
  `encodeDefaults = false`, while the DTO tests serialise through `protocolJson`, which sets
  `encodeDefaults = true`. A `= false` default would therefore be **present in every test's JSON and
  absent from the real response** for precisely the ~100% of players whose answer is `false`.
  `RecentDuelsResponse.duels` already carries this warning in its KDoc; this is the second instance
  and it is now a rule for this file rather than a note on one field.
- **It is named for the product's word, not the schema's.** `ADR-0052` §2's shipped copy is *"Your
  display name was removed"* and its *Consequences* record that *"the word removed is the product's,
  forever."* The wire is read by the client that renders that sentence, and the schema's word —
  `RETIRED` — describes what happened to the *string* in the namespace, which is a different fact
  from what happened to this player. `retired` stays the registry's vocabulary; `removed` is the
  player's.
- **It is a predicate about now, in `deviceRouteLive`'s grammar** — noun plus adjective, describing
  the caller's current state, composed by the server, rendered by the client.

### 2. What it means, exactly

`displayNameRemoved` is `true` **iff** the caller currently holds no display name **and** at least
one `name_registry` row exists with `retired_from` equal to their player id.

| The caller | `displayName` | `displayNameRemoved` |
| --- | --- | --- |
| never set a name | `null` | `false` |
| had a name removed, has set none since | `null` | **`true`** |
| holds a name, never had one removed | `"Bobb"` | `false` |
| had a name removed and has set another | `"Cara"` | `false` |

- **Rows 1 and 2 are the requirement `ADR-0052` §1 exists for**, and this shape distinguishes them:
  the two `null` states differ in the second column. That is the whole of what the notice reads.
- **Row 4 is where this decision is a decision.** The bit goes quiet the moment the player sets a new
  name, because the notice's condition stops holding — `ADR-0052` §8's *"shown while it is true and
  gone when it is not"*, made a property of the response rather than of the client's rendering. The
  wire deliberately cannot answer *"was this player ever moderated?"*, and *Consequences* records
  that as a cost.
- **Row 3 with `true` is a state no query can produce**, because §3's expression contains the first
  conjunct. `STORY-0410` asserts it as an invariant of the answer.
- **The conjunction is the server's**, for the reason the alternatives section gives at length: it is
  the half that fails silently, and the server is the only place a database fixture can hold it.

### 3. The query, and the index that makes it cheap

One statement, one round trip. The lookup half is untouched; the expression is added:

```sql
SELECT p.id,
       p.coin_balance,
       p.display_name,
       (p.display_name IS NULL
        AND EXISTS (SELECT 1 FROM name_registry r
                     WHERE r.retired_from = p.id AND r.reason = 'RETIRED')) AS display_name_removed
FROM player p
WHERE p.device_id = ?
```

- **A correlated `EXISTS`, never a join.** A player may hold **more than one** retired name — removed,
  renamed, removed again — so `LEFT JOIN name_registry` returns two rows for one profile, and
  `if (rows.next())` silently takes the first. That bug appears only after a second takedown against
  one player and no fixture will have one. `EXISTS` is a semijoin: one boolean per profile row, by
  construction, whatever the registry holds.
- **It correlates to `p.id`, never to a second bind parameter.** Binding the caller's id a second
  time would work today and decouples the two halves: the boolean and the rest of the row could
  afterwards be made to describe different players by an edit that looks harmless. Correlating to the
  same `player` row the response is built from makes *"this bit is about the player this row is
  about"* a property of the statement's shape.
- **`AND r.reason = 'RETIRED'` is redundant and stays.** `name_registry_retired_from
  CHECK (retired_from IS NULL OR reason = 'RETIRED')` already implies it. It is kept because the
  statement should be readable without the constraint in hand, and it costs nothing the planner will
  not fold away.
- **`ADR-0049` §1 and §5 rewrite the `FROM`/`WHERE` of this query** — `player` loses its `device_id`
  column to `device_binding` — and add `deviceRouteLive` beside this field. The `EXISTS` rides along unchanged,
  because it is correlated to `p.id` and knows nothing about how `p` was found. The two booleans are
  independent; either story may land first.
- **The migration gains one index**, in `ADR-0051` §8's file if it has not yet merged, and in its own
  `V<n>` if it has:

  ```sql
  CREATE INDEX name_registry_retired_from_idx
      ON name_registry (retired_from) WHERE retired_from IS NOT NULL;
  ```

  **Partial, and that is the point.** The index holds only retired rows, which are rare and stay
  rare, so it is near-empty exactly when a third index on a small table is hardest to justify. It
  costs the hot write path **nothing**: `INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')`
  writes `retired_from IS NULL` and so writes no index entry at all. What it buys is that the common
  answer — *no match* — is an index probe rather than a scan of a table that only grows.

### 4. What replaces `ADR-0051` §1's *"No read path exposes this table"*

That sentence is retired as an unqualified sentence, because a reader will check it against
`PostgresProfileReads` and find it false. Its successor, which is the rule from here on:

> **One read path derives from `name_registry`, and it derives one boolean about the caller.**
> `PostgresProfileReads.profileOf` evaluates a single correlated `EXISTS` against
> `name_registry.retired_from`, bound by equality to the `player` row it is already returning. No
> endpoint enumerates the table; no response carries a `name`, a `reason`, a `created_at`, a
> `retired_from` value, a row or a count from it; no response says anything about another player; and
> there is still no availability check — `ADR-0029` §5 refused one and nothing here reopens it.

Everything else in §1 stands, and two clauses are worth naming as surviving rather than leaving to
inference:

- **"Who holds a name is `player.display_name` and nothing else"** survives untouched.
  `displayNameRemoved` is not a source of truth for holding a name; it is `false` for everyone who
  holds one.
- **The registry is still not a read model.** Nothing selects from it. `EXISTS` tests for a row and
  returns no part of one, which is why the amendment is to *"exposes"* and not to the refusals around
  it.

**What stops this leaking another player**, stated as three things rather than one:

1. **The predicate is an equality against the row being returned.** `profileOf` answers for exactly
   one `player` row — the one the caller authenticated as — and the `EXISTS` is correlated to that
   row's `id`. The statement's only bind parameter is the credential the caller already presented;
   no argument a caller supplies reaches `retired_from`.
2. **`DuelSummaryResponse` and `RECENT_DUELS_SQL` gain nothing**, and this is the named prohibition
   rather than an omission: that query already holds the **opponent's** `player` row as `p`, so the
   identical `EXISTS` pasted there compiles, runs, and publishes a takedown to a stranger.
   `retired_from` may appear in exactly one file under `poker-server/src/main/kotlin`, and it is
   `PostgresProfileReads.kt`.
3. **The uncorrelated form is the mis-implementation to test for.**
   `EXISTS (SELECT 1 FROM name_registry WHERE reason = 'RETIRED')` — the correlation dropped — makes
   every caller in the product read `true` the instant any takedown happens to anybody. It is a
   fact about another player disclosed to everyone, and it passes any test whose fixtures do not
   share a database. §6 makes that a criterion.

### 5. `PROTOCOL_VERSION` does not move, and here is the mechanism

`PROTOCOL_VERSION` stays at `2`. `docs/protocol-versions.md` gains no row, and no fingerprint
changes.

**Why, checkably:** `ProtocolVersionLedgerTest.computedFingerprint()` hashes the `text` of every
declaration from `protocolDeclarations()`, whose `protocolRoots` are exactly
`ClientMessage.serializer().descriptor` and `ServerMessage.serializer().descriptor`. `ProfileResponse`
lives in `duels.poker.server.protocol.http` and is reachable from neither root, so the fingerprint is
byte-identical after this change and `:poker-server:check` neither fails nor needs a new claim. This
is the same answer `ADR-0049` §5 and `ADR-0050` gave; it is written out here because *"it is HTTP,
not a socket message"* is an assertion and this is the reason it is true.

**What a bump would have cost, for the record**, since the question is worth answering rather than
waving away: the `PROTOCOL_VERSION` constant, a regenerated `protocol.gen.ts` (byte-compared on
`check`, `ADR-0020`), a new `docs/protocol-versions.md` row carrying a fresh 16-hex fingerprint and a
claiming story, `web-client`'s version constant — and every client on the old number refused at the
handshake with `protocol version mismatch`. None of that is owed here.

**What does move: `docs/protocol.md`.** The `### Profile endpoint` field table gains

| Field | Type | Semantics |
| --- | --- | --- |
| displayNameRemoved | boolean | `true` when the player holds no display name **and** a name has been removed from them by an operator (`ADR-0052`). `false` for a player who never set one, and `false` again once they set a new one. Never says anything about another player. |

`HttpEndpointDocumentationTest` reflects over `ProfileResponse` and enforces **documented ⇒ exists**;
it does **not** enforce **exists ⇒ documented**. So a forgotten row here is caught by nothing, which
is why it is a `STORY-0410` criterion in §6 rather than a note.

### 6. What `STORY-0410` gains, concretely

- **One field** on `ProfileResponse`, non-null, no default, KDoc'd with the two-`null` distinction.
- **One `SELECT` expression** in `PostgresProfileReads.profileOf`; the `ProfileReads` port signature
  is unchanged and gains no method.
- **One index line** in the migration, and the `docs/protocol.md` row above.
- **`PostgresProfileWrites` passes the literal `false`.** `SetNameResult.NameSet` describes a player
  who now holds a name — including `ADR-0051` §2's idempotent retry — so `displayNameRemoved` is
  `false` by construction on every `200` from `PUT /api/me/name`. It is written as a literal with the
  reason beside it, never a second query and never a subquery in `RETURNING`.
- **Three criteria, and the first two must share one database.** In a single test against one
  database: a player whose name was retired reads `true`, **and** a second player in that same
  database who never set a name reads `false`. Two tests with one fixture each pass while the
  correlation is missing entirely (§4.3), which makes them worth nothing here.
- **One criterion for row 4** of §2's table: a player whose name was retired and who has since set a
  new one reads `false`, with a non-null `displayName`. This is the conjunct that fails silently.
- **`ADR-0052` §7's negative criterion is unchanged and is now also structural**: a duel summary line
  for an opponent whose name was retired is byte-identical to one for an opponent who never set a
  name, plus the code-shape assertion that `retired_from` appears in exactly one main source file.
- **Nothing on the write path**, exactly as `ADR-0052` §7 left it: `SetNameResult` keeps three cases,
  the endpoint keeps its four codes, `retire_display_name` takes no third argument.

`STORY-0411` gains nothing beyond `ADR-0052` §7: it reads one boolean and renders §2's four
sentences when it is `true`.

### 7. What is deliberately not built

- **No second endpoint, no `GET /api/me/name-status`, no new route.**
- **No count, no date, no name, no reason** — nothing from a registry row reaches a response.
- **No field on `DuelSummaryResponse`, no field on any response about another player.**
- **No history bit.** The wire cannot answer *"were you ever moderated?"*, only *"are you nameless
  because of it right now?"*
- **No second query and no conditional round trip.** One statement answers `GET /api/me`, for
  everybody, always.

## Consequences

**What it buys.** `ADR-0052`'s notice becomes implementable from one field with no client-side rule
to get wrong, and the conjunction that decides whether a player is shown a moderation notice lands
where database fixtures can hold it. The leak the fact invites — `retired_from` is a player id, and
the opponent's `player` row is already joined one query away — is answered by a statement shape
rather than by care: correlate to the row you are returning, and the boolean cannot be about anybody
else. The cost on the hottest read is bounded at an index probe instead of growing with the registry
forever, and it is bought in the one window where it is a single line in an unwritten migration.
Nothing about the protocol moves, and the reason it does not move is now checkable rather than
asserted.

**What it costs.**

- **`GET /api/me` pays for a rare state on every call, forever.** One semijoin per profile read,
  for an answer that is `false` for essentially every player, on the most frequent read in the
  product. Small, and paid by everybody, permanently.
- **A third index on `name_registry`**, where `ADR-0051` already recorded *"two unique indexes on one
  small table"* as something a reader stops at. It earns nothing for as long as the registry is
  small, it can never be removed once the migration merges, and it is insurance bought before there
  is anything to insure — justified only by the fact that today it is free and later it is not.
- **`ADR-0051` §1's *"No read path exposes this table"* is spent as an absolute.** It was a sentence
  anybody could quote to refuse a proposal; it is now a bounded rule with one named exception, and
  the next person who wants something from `name_registry` on a response argues against a narrower
  refusal. `ADR-0052` spent *"nothing in production reads it"*; this spends the structural one.
- **The wire cannot say *this player was once moderated*.** Row 4 of §2's table reads exactly like a
  player who was never touched. Any future feature that wants to address a player who was moderated
  and has since renamed — a different notice, an account-screen line, a support flow — needs a second
  field, because this one has deliberately gone quiet. That is a real loss of information and it is
  chosen.
- **The invariant is in a SQL string, not in the type.** `ProfileResponse("p", 0, "Bobb", true)`
  compiles. §2 row 3 is guaranteed by the query and asserted by a test; nothing stops a future
  construction site producing the impossible pair, and `PostgresProfileWrites` is already a second
  place that must know a rule about a field it does not compute.
- **The one-file guarantee on `retired_from` is a grep.** `ADR-0051` called this class of assertion
  *"the weakest guarantee in this document"* and it is inherited here unimproved: it survives exactly
  as long as nobody wants an opponent-facing moderation flag in a hurry.
- **`docs/protocol.md` gains a row that no gate requires.** `HttpEndpointDocumentationTest` checks
  documented ⇒ exists and not the reverse, so the field can ship undocumented and green.
- **Two booleans on `ProfileResponse` and more coming.** With `deviceRouteLive` (`ADR-0049` §5) this
  response is on its way to being a bag of independent flags, each cheap and none of them
  individually worth a second endpoint. That is a shape that gets worse one field at a time and there
  is no rule here that stops it.

**What it forecloses.**

- **Putting the fact on anybody else's response cheaply.** After §4 that is overturning a named rule
  and deleting a test, not filling a gap — which is `ADR-0052` §5's asymmetry made structural.
- **Answering the history question without a wire addition.** See above; deliberate.
- **Reading `name_registry` as a read model.** The amendment permits an `EXISTS`, not a `SELECT`;
  anything that wants a row, a count or a date is a new decision against a rule that names exactly
  what is allowed.
- It does **not** foreclose a reason string, a second field, a contact route, or moving
  `display_name` out of `player` (`ADR-0051`'s own *forecloses* section): the `EXISTS` correlates to
  `p.id`, which survives that move as whatever row identifies the player.

## Alternatives considered

**The raw fact — `nameRetired: Boolean` meaning *at least one name has ever been retired from you*,
with the client composing `displayName === null && nameRetired`.** The strongest rejected option and
the one this had to beat. Its case: a response should state facts about the world, not conditions for
drawing something; the server stays ignorant of what the client renders; the fact composes for
`STORY-0412` or anything later with no wire change; and it is one conjunct less in a SQL statement.
Rejected on two grounds. First, it moves `ADR-0052` §1's *"both halves matter"* into client rendering
code, where no database fixture can hold it and where the failure — a player who chose a new name a
year ago still being shown *"Your display name was removed"* — is invisible to every test that has
one fixture, which is the exact failure mode this repository has been bitten by before. Second, it
makes a durable *this profile was moderated* bit ride on every profile read for the life of the
profile, long after `ADR-0052` §8 says the telling should be *"gone when it is not"* true, and it has
no consumer: §8 refuses re-telling and *What this does not settle* says the account screen does not
repeat it. The reversal is also asymmetric in the chosen direction's favour: adding a second,
history-shaped field later is additive, whereas taking a durable bit off the wire after clients
compose it is a removal.

**A three-state field instead of a boolean** — `displayNameState: "SET" | "NEVER_SET" | "REMOVED"`,
or `displayName` replaced by a sealed shape. Genuinely strong, and it is the house style: an
exhaustive `when` over an enum, with the impossible pair of §2 row 3 unrepresentable rather than
merely untested. Rejected because `displayName: String?` must stay on the wire regardless — the
client needs the string — so the enum duplicates `displayName != null` for two of its three values,
and two fields encoding one fact are two fields that can disagree. It is also a third vocabulary for
a state the product has already named, where `ADR-0052` §6 fixed *one boolean and no string*; and it
would leave one response carrying an enum and a boolean (`deviceRouteLive`) for two structurally
identical facts decided a week apart. A boolean's evolution path is a second boolean, which is worse
aesthetically and cheaper in every other way.

**A separate endpoint — `GET /api/me/name-status`.** Its case is real: `GET /api/me` stays
byte-identical, the cost stays off the hottest read entirely, and the only consumer is one screen
that could fetch it on mount. Rejected because it is a second round trip for one bit on the exact
screen where the client has just read the profile that bit qualifies; because `ADR-0052` §6 fixes the
profile read as the carrier; and because an endpoint whose *existence* is the subject is a worse
probing surface than a field on a response the caller already receives. It also adds a route to a
server that has been deliberately kept to a handful.

**Pay only when it can be true — keep the profile query as it is, and run a second query only when
`display_name IS NULL`.** The tempting optimisation, and its case is that it costs the common path
literally nothing. Rejected because the common path is the wrong way round: a display name is
optional and most profiles are anonymous (`ADR-0036`, `ADR-0012`), so `display_name IS NULL` is the
*majority* case and the conditional second round trip would fire for most callers. It also makes
`profileOf` two statements without a transaction around them, for a fact whose whole cost is one
index probe.

**`LEFT JOIN name_registry` instead of a correlated `EXISTS`.** Its case: one flat statement, no
subquery, and it could later carry the retirement's `created_at` without a second query — and a join
reads more naturally to more people than a correlated subquery. Rejected on cardinality: a player may
hold two retired names, so the join returns two rows for one profile and the existing
`if (rows.next())` takes whichever comes first — a bug that requires a second takedown against one
player to appear and that no fixture will have. It also puts `retired_from` in a position to be
*selected* rather than tested, which is the one thing §4 is trying to make hard.

**No index — accept the sequential scan.** Its case is the honest one: `ADR-0051` already flagged two
indexes on this table as a thing a reader stops at, a third is a permanent line in an immutable
migration, and today `name_registry` would be scanned in microseconds because it is empty. Rejected
because the asymmetry runs the wrong way and never stops running: the *common* answer is *no match*,
which is precisely the case that scans the whole table; the table only ever grows, one row per name
ever set (`ADR-0051`); and the read is the most frequent one in the product. The partial index costs
the hot write path nothing — a `TAKEN` row has `retired_from IS NULL` and so is not in it — and it is
one line today against a migration file and a second migration tomorrow.

**Fold the fact into `displayName` with a sentinel** — `""`, or a reserved string, for *removed*.
Its case is that the wire does not grow at all. Rejected immediately: `ADR-0029` §6 forbids the
server fabricating anything for the unset case, every existing client's `null` check becomes wrong
silently, and it makes a type that has one meaning carry two.

## What this does not settle

- **Whether anything is ever told to a player who was moderated and has since renamed.** §2 row 4 is
  `false` and the wire cannot express it. If a surface ever wants that, it is a second field and its
  own decision — and `ADR-0052` §8's *"no re-telling"* is the standing answer today.
- **What `STORY-0411` draws.** `ADR-0052` §1 and §2 fix the copy and the placement; the treatment the
  notice sits above is still that story's.
- **Whether `ProfileResponse` should stop being a flat bag of flags.** Two booleans is not a problem;
  the shape of the fifth one might be. Nothing here is in the way of splitting it later, and nothing
  here argues for doing so now.
