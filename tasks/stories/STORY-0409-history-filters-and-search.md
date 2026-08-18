---
id: STORY-0409
title: History filters and search
type: story
status: ready
parent: EPIC-04
module: poker-server
labels: [server, http, read-path, history]
depends_on: [STORY-0408]
---

## Goal

A player can narrow their history to the duels they are looking for: by outcome, and by the name of
the opponent — with paging still total and disjoint *within* the filter.

## Why

A full history is only useful if it can be interrogated; two hundred rows in reverse chronological
order is an archive, not a feature. `ADR-0029` sized the display-name column for exactly this: it
kept an ordinary deterministic collation on purpose so that `LIKE`, `ILIKE` and a pattern-ops index
stay available *"to `EPIC-05`'s leaderboard and to `STORY-0409`"*.

## Design notes

- **The axes are the ones the stored row already carries**: `outcome` (won, lost, drew — read off
  the stored `coin_delta`, never derived by a client) and the opponent's display name. Nothing here
  adds a column, and a filter that would need data `duel_result` does not hold is out of scope
  rather than a reason to store more. Widening the set later is additive.
- **Search is a prefix or substring match on the opponent's display name, case-insensitively**, over
  the join `STORY-0402` added. An opponent with no name matches no search — the server invents no
  placeholder to match against.
- **A filter is not an authentication path.** Searching by name reads `duel_result` rows this player
  already owns and returns duels, never players; no code path turns a name into an identity
  (`ADR-0029` §7). The distinction matters here more than anywhere: this is the first query that
  takes a name as *input*.
- **Filters compose with the cursor, not around it.** A cursor is only valid for the filter that
  produced it, and the totality property from `STORY-0408` is asserted again *inside* a filter, with
  a matching row inserted mid-read.
- **Every filter value is bound, never interpolated**, and a `%` or `_` in a search term matches
  literally rather than acting as a wildcard — otherwise the search term is a language.
- The endpoint stays one query per page.

## Tasks

Split on 2026-08-18, against what `STORY-0408` actually landed. The chain is linear on purpose: the
run is sequential, and `DuelFilter.kt`, `PostgresProfileReads.kt`, `PostgresProfileReadsTest.kt`,
`ProfileRoutes.kt` and `ProfileRouteTest.kt` are each touched by more than one ticket, so two
startable tickets would be two tickets editing one file.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040901](../tasks/TASK-040901-a-filter-is-two-axes-and-an-outcome-is-one-of-three-names.md) | A filter is two axes, and an outcome is one of exactly three names | ready |
| [TASK-040902](../tasks/TASK-040902-the-search-term-the-server-will-accept.md) | The search term the server will accept, counted in code points | backlog |
| [TASK-040903](../tasks/TASK-040903-the-read-takes-a-filter-and-an-outcome-is-a-sign.md) | The read takes a filter, and an outcome is the sign of the stored delta | backlog |
| [TASK-040904](../tasks/TASK-040904-the-search-is-a-substring-of-the-opponents-name.md) | The search is a substring of the opponent's name, folded under the pinned collation | backlog |
| [TASK-040905](../tasks/TASK-040905-the-search-term-is-not-a-language.md) | The search term is not a language, and an unnamed opponent is not a match | backlog |
| [TASK-040906](../tasks/TASK-040906-paging-inside-a-filter-is-still-total-and-disjoint.md) | Paging inside a filter is still total and disjoint, across an insert that matches it | backlog |
| [TASK-040907](../tasks/TASK-040907-the-port-takes-the-filter-and-both-doubles-follow.md) | The port's duel read takes the filter, and both doubles follow | backlog |
| [TASK-040908](../tasks/TASK-040908-two-parameters-become-one-filter-or-one-refusal.md) | Two query parameters become one filter, or one refusal | backlog |
| [TASK-040909](../tasks/TASK-040909-the-endpoint-reads-the-filter-and-refuses-what-it-refuses.md) | The endpoint reads the filter, and refuses what the parsers refuse | backlog |
| [TASK-040910](../tasks/TASK-040910-over-http-against-the-database-a-filtered-page.md) | Over HTTP, against the database — a filtered page is exactly the filtered rows | backlog |
| [TASK-040911](../tasks/TASK-040911-the-document-contracts-both-filters-and-what-each-refuses.md) | The document contracts both filters, and what each of them refuses | backlog |
| — | *Refusing a cursor issued under a different filter — **blocked on `DEC-050`**. Not ticketed, because every shape it could take is a guess at the answer. Re-run `/plan-story STORY-0409` once its ADR merges.* | blocked |

### The one thing this split would not decide

**A filter changes what a cursor means, and nothing settles how the server tells that apart.** A
keyset cursor is a position in *a particular ordering of a particular set*; narrow the set and the
same `(finishedAt, duelId)` names a place the client never actually walked to. This story requires
such a cursor be **refused rather than silently reinterpreted** — and today's cursor carries nothing
that could name the filter it was drawn under. `STORY-0408`'s split already ruled that changing what
a cursor is *"is an ADR, not a ticket"*, so this is **`DEC-050`**, the architect's, and no ticket
here guesses at it.

The split is ordered so that exactly one guarantee waits on it and the other ten do not.
`TASK-040909` therefore ships an endpoint that **accepts** `after` beside a filter and reads it as a
position in the same `(finishedAt, duelId)` order — well defined, never losing a row *within* a
consistent filter, and a weaker contract than the one above. That is stated in `TASK-040909`, in
`TASK-040910` and in `docs/protocol.md` via `TASK-040911`, rather than left for someone to discover.
No test in the story asserts anything about that combination in either direction, because whichever
way it were asserted the answering ticket would have to undo it.

### What the split settled, and what it sharpened

Five things were decided while splitting, each written into the ticket that carries it rather than
left for a coder to guess. Three of them were **measured against `postgres:16-alpine`** before any
ticket was written, not reasoned about:

- **Substring, not prefix.** The design note above says *"a prefix or substring match"*; substring is
  the reading under which both halves are true at once, since every prefix match is a substring
  match, and it is the useful one for a player who remembers the middle of a name. Widening later
  would have been additive and narrowing would not, so `TASK-040904` pins it with a mid-name term
  rather than letting it fall out of whatever gets written.
- **`POSITION`, not `ILIKE`.** A `LIKE` pattern is a small language, and `ILIKE '%' || term || '%'`
  hands the client `%` and `_` as wildcards; escaping them is a rule that can be got wrong silently.
  `POSITION(… IN …)` has no pattern language at all, so the guarantee is structural. Measured: with
  names `100%Sure` and `1004Sure` the term `%Sure` answers one row under `POSITION` and **both**
  under `ILIKE`; with `a_b` and `axb` the term `a_b` does the same. `TASK-040905` pins both results.
- **The collation is pinned on both sides of the fold.** `ADR-0029` §1 says of this exact function
  that an unpinned `lower()` follows the cluster's `LC_CTYPE` and so means different things on the
  test container and on whatever `EPIC-07` deploys. `lower(x COLLATE "und-x-icu")` is what the
  unique index already folds under, so search and uniqueness agree about case.
- **Both filter clauses are always present and neutralised by a bound `NULL`**, never appended
  conditionally. That keeps `RECENT_DUELS_SQL` and `DUELS_AFTER_SQL` two fixed strings with fixed
  bind positions, keeps the cursor predicate a plain row-value comparison rather than something
  assembled at runtime, keeps `aListOfThreeDuelsPreparesExactlyOneStatement` true — and means every
  existing test in `PostgresProfileReadsTest` passes unchanged through both filter tickets. Verified:
  `(?::int IS NULL OR sign(coin_delta) = ?::int)` answers every row for a bound `NULL`.
- **An outcome is the sign of the stored delta.** `outcomeOf` reads only the sign, on `ADR-0014`'s
  reasoning that a function reading the sign survives a change to the award and one comparing against
  a literal does not. The filter is the inverse of that function and is written the same way, so the
  two cannot disagree about a row.

And two costs are recorded rather than discovered:

- **`FixedProfileReads` ignores the filter**, exactly as `TASK-040807` recorded that it ignores the
  cursor. Any future test that passes a filter through that double proves nothing until it grows a
  `filtersRequested` list of its own. `TASK-040907` says so in its out-of-scope.
- **No index, no migration.** `duel.finished_at`, `duel_result.player_id`, `coin_delta` and
  `display_name` are all unindexed for this query, and a new `V<n>` would race `STORY-0410`'s
  migration number (`ADR-0029` §8). A `%term%` search cannot use a pattern-ops index in any case.
  Correct without one; not yet *fast* at a size v0.1 does not have.

## Acceptance criteria

- [ ] Filtering by each of won, lost and drew returns exactly the duels with that stored outcome —
      all three asserted, against a fixture holding all three.
- [ ] Searching a name returns the duels against that opponent and no others, with a second opponent
      present whose name shares a prefix.
- [ ] The search is case-insensitive, asserted with a query in a different case from the stored name.
- [ ] A search term containing `%` and `_` matches literally, asserted against a stored name that
      would be matched if they were wildcards.
- [ ] Paging inside a filter is total and disjoint, including across an insert that matches the
      filter.
- [ ] A cursor from one filter used with another is refused rather than silently reinterpreted.
      **Blocked on `DEC-050`; not ticketed.**
- [ ] An empty result is `200` with an empty page, not `404`.
- [ ] `docs/protocol.md` contracts every parameter and what each refuses.

## Out of scope

- Any new stored column, and any filter that would need one.
- Searching for *players* rather than for duels — `EPIC-05` owns a name-per-row leaderboard and what
  a row links to.
- The screen — `STORY-0413`.
