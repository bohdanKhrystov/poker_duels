---
schema: 2
id: TASK-040904
title: The search is a substring of the opponent's name, folded under the pinned collation
type: task
status: ready
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, db, read-path, history, filters, search]
depends_on: [TASK-040903]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileReads` can be asked for only the duels whose opponent's display name contains a
given term, whatever case either was typed in.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify — one clause, two binds |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — one fixture helper, four tests |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §1 on the pinned collation, §7 on names and identity |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — `DuelFilter.opponent` |

## Scope — the SQL

`DUEL_LINES` gains **one line**, directly below the outcome clause `TASK-040903` added:

```sql
  AND (?::text IS NULL OR POSITION(lower(?::text COLLATE "und-x-icu") IN lower(p.display_name COLLATE "und-x-icu")) > 0)
```

Four things in that line, each of which is the whole reason it is written this way:

- **`POSITION(… IN …)`, not `LIKE` or `ILIKE`.** A `LIKE` pattern is a small language, so
  `ILIKE '%' || term || '%'` makes every `%` and `_` a client sends into a wildcard —
  `STORY-0409` forbids exactly that, and escaping them is a rule that has to be got right in Kotlin
  and can be got wrong silently. `POSITION` has no pattern language at all, so the guarantee is
  structural rather than defended. **Measured against `postgres:16-alpine` before this ticket was
  written**: with names `100%Sure` and `1004Sure`, `POSITION` answers `{100%Sure}` for the term
  `%Sure` while `ILIKE '%' || '%Sure' || '%'` answers **both**; with `a_b` and `axb`, `POSITION`
  answers `{a_b}` for `a_b` while `ILIKE` answers both. `TASK-040905` pins those two results.
- **Substring, not prefix.** `STORY-0409` says *"a prefix or substring match"*; substring is the
  reading under which both halves of that sentence are true at once, since every prefix match is a
  substring match, and it is the useful one for a player who remembers the middle of a name.
  Widening later would have been additive; narrowing would not, so the choice is pinned by
  `aSearchMatchesInsideTheName` below rather than left to fall out of the implementation.
- **The collation is pinned on both sides**, because `ADR-0029` §1 says of this exact function:
  *"`lower()` folds according to the collation of its argument. Left to the default it follows the
  cluster's `LC_CTYPE`, so the same schema would enforce a different rule on the
  `postgres:16-alpine` container the tests use and on whatever `EPIC-07` deploys."* `und-x-icu` is
  the collation the unique index in `V3__player_display_name.sql` already folds under, so search and
  uniqueness agree about what two names differing only in case are.
- **The clause is always present and neutralised by a bound `NULL`**, exactly as the outcome clause
  is, so both statements stay fixed strings with fixed bind positions and every existing test keeps
  its assertions. `TRUE OR NULL` is `TRUE`, so a `NULL` term never reaches `POSITION`'s result.

**An opponent who has never set a name is excluded for free**, and this is worth understanding
before writing anything: `p.display_name` is `NULL`, `POSITION(term IN NULL)` is `NULL`, `NULL > 0`
is `NULL`, and `FALSE OR NULL` is `NULL`, which `WHERE` does not admit. The server therefore invents
no placeholder to match against, which is what `STORY-0409` requires and what `ADR-0029` §6 requires
of every other read. `TASK-040905` asserts it.

**This is not an authentication path.** `ADR-0029` §7 forbids a function that takes a name and
returns a `PlayerId`, a `DeviceId`, a session or a profile. This one takes a name and returns
**duels the requesting player already sat in**, filtered by `WHERE r.player_id = ?` before anything
else — so a name that matches nobody and a name that matches a stranger are the same empty answer,
and no new row becomes reachable. Do not add a lookup, an exists probe or a count that would answer
*does this name exist?*

## Scope — the Kotlin

Bind positions move by two again:

| Position | Value |
| --- | --- |
| 1 | `UUID.fromString(playerId.value)` |
| 2 and 3 | the outcome's sign, or `setNull(_, Types.INTEGER)` |
| 4 and 5 | `filter.opponent`, or `setNull(_, Types.VARCHAR)` |
| 6 (no cursor) | `limit` |
| 6, 7, 8 (cursor) | `finishedAt`, `duelId`, `limit` |

Bind the term with a helper shaped like `TASK-040903`'s `bindOutcome` — two positions, one value,
because JDBC has no named parameters. Nothing lower-cases the term in Kotlin: folding happens once,
in SQL, under the pinned collation, or the two sides could disagree.

## Out of scope

- `%`, `_` and the unnamed opponent as *assertions* — `TASK-040905` owns those three tests, so this
  ticket's diff stays one clause and its four tests.
- Any index, any migration, `pg_trgm`, and any use of `text_pattern_ops`. `ADR-0029`'s consequences
  note that a pattern-ops index *remains available*; a `%term%` search cannot use one anyway, and a
  new `V<n>` would race `STORY-0410`'s migration number.
- Searching for *players* rather than for duels — `EPIC-05`.
- Editing any existing test or call site in `PostgresProfileReadsTest.kt`.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`. One new fixture helper, using the file's own
`setPlayerDisplayName` (which writes the column directly and so may store a name the write path
would refuse — that is deliberate and `TASK-040905` depends on it):

```kotlin
/**
 * Alice plays bob (named `Halvard`) at 10:02, carol (`Halvardsen`) at 10:03 and dave (unnamed)
 * at 10:01 — recorded in that order, so newest-first is carol, bob, dave and matches neither the
 * insertion order nor the alphabetical one. Returns display name (or null) -> duelId.
 */
private suspend fun threeDuelsAgainstAPrefixPair(): Map<String?, String>
```

| Test | Proves |
| --- | --- |
| `aSearchReturnsOnlyTheDuelsAgainstThatOpponent` | term `Halvardsen` returns exactly carol's duel id. `Halvard` is present and is a **prefix** of the term, so the test fails against an implementation that matched a name *contained in* the term rather than the other way round — the direction it is easiest to get backwards |
| `aSearchMatchesInsideTheName` | term `vard` returns bob's and carol's ids, in newest-first order (carol, then bob). This is what pins **substring**: a prefix-only `LIKE 'vard%'` answers nothing here, and an exact match answers nothing either |
| `aSearchIgnoresTheCaseOfBothSides` | term `HALVARDSEN` returns carol's duel id, and term `halvardsen` returns it too. Fails against a `POSITION` with no `lower()` on either side |
| `noSearchStillReadsEveryOpponent` | the same fixture with `DuelFilter.NONE` returns all three ids including dave's unnamed one — a filter that matched everything and a filter that matched nothing are both excluded by having this test beside the three above |

Every one of the four asserts the exact list of duel ids, never `size` alone.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aSearchReturnsOnlyTheDuelsAgainstThatOpponent` passes
- [ ] `PostgresProfileReadsTest.aSearchMatchesInsideTheName` passes and asserts **two** ids in
      newest-first order
- [ ] `PostgresProfileReadsTest.aSearchIgnoresTheCaseOfBothSides` passes for both an upper-cased and
      a lower-cased term
- [ ] `PostgresProfileReadsTest.noSearchStillReadsEveryOpponent` passes and asserts all three ids
- [ ] The clause uses `POSITION`; the strings `LIKE`, `ILIKE` and `ESCAPE` appear nowhere in
      `PostgresProfileReads.kt`
- [ ] `COLLATE "und-x-icu"` appears on **both** `lower(...)` calls in the new clause
- [ ] Nothing in `PostgresProfileReads.kt` calls `lowercase()`, `uppercase()` or `trim()` on the
      search term
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged,
      including `theCursorQueryAndTheFirstPageQueryShareOneJoinText` and
      `aListOfThreeDuelsPreparesExactlyOneStatement`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
