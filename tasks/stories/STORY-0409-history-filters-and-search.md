---
id: STORY-0409
title: History filters and search
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0409` once `STORY-0408` has merged.* | — |

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
- [ ] An empty result is `200` with an empty page, not `404`.
- [ ] `docs/protocol.md` contracts every parameter and what each refuses.

## Out of scope

- Any new stored column, and any filter that would need one.
- Searching for *players* rather than for duels — `EPIC-05` owns a name-per-row leaderboard and what
  a row links to.
- The screen — `STORY-0413`.
