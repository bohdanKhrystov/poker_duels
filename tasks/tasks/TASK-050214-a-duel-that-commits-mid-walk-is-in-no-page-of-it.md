---
schema: 2
id: TASK-050214
title: A duel stamped at the cutoff is in no page of the walk, and the ranks stay the cutoff's
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, db, leaderboard, paging, tests]
depends_on: [TASK-050213]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsWalkDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

A duel that finishes while a client is reading changes nothing about that client's walk: the walk
enumerates the ladder **as it stood at the instant it began**, and it is a test rather than a
sentence in an ADR that says so.

## The fixture this needs, and why an easier one proves nothing

`ADR-0066` §4 and `ADR-0066` §9: *"a fixture in which the mid-walk duel touches only players already
served cannot fail this, so the fixture puts one player on each side of the cursor."* The duel
recorded here therefore has its **loser** on the page already served and its **winner** on a page
not yet reached.

The winner must also be able to cross the cursor, which is what makes the walk's guarantee
load-bearing, so **the roles are assigned by player id**: `player_id DESC` is the second component
of the order (`ADR-0066` §3), and the ids are random. Mint the five profiles, then

```kotlin
val byId = players.sortedByDescending { it.id.value }   // never UUID.compareTo — that is signed
```

and give `byId[0]` the winner's role and `byId[1]` the cursor row's role. `UUID`'s natural ordering
compares two signed longs and does **not** agree with PostgreSQL's byte order; the canonical
lower-case text does.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | read |

## Scope

- Two tests added to the existing class, with a **second** fixture builder beside the first — the
  seven-player ladder stays untouched and its two tests keep their assertions.
- The clock stays `Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`, so the
  cutoff a cursorless request mints is exactly that instant, and a duel stamped **at** it falls
  outside the half-open window `[season.start, asOf)`.
- The sequence is the point. Write it in this order and nothing else:
  1. record the five-duel fixture below;
  2. read page one (`limit = 2`);
  3. record the mid-walk duel, `finishedAt = 2026-08-20T09:00:00Z` — the cutoff exactly;
  4. walk to the end from page one's `nextCursor`.
- No production file changes.

## Out of scope

- **The anomaly** — a duel stamped **before** the cutoff is `TASK-050215`, and it is a different
  fixture and a different assertion. This ticket must not weaken its own assertions to accommodate
  it.
- **Threads.** *Mid-walk* means between two sequential requests, which is the race a real reader
  meets.
- **A refresh, a sweep or a wait.** Nothing here polls; the point is that nothing has to.

## Tests

`StandingsWalkDatabaseTest`, `-PrequireDocker=true`, `limit = 2`.

**Fixture B — five players, distinct standings.** With `w = byId[0]` and `x = byId[1]`, and `a`, `e`,
`d` the other three, record:

1. `a` beats `e`;
2. `a` beats `d`;
3. `x` beats `e`;
4. `w` beats `d`;
5. `e` beats `w`.

Standings: `a +2`, `x +1`, `w 0`, `e -1`, `d -2` — sum zero, ranks `1, 2, 3, 4, 5`, pages
`[a, x] [w, e] [d]`. Page one's cursor names `x` at `+1`; `w` sits on page two and has `x`'s
standing minus one, so a win would lift `w` **above** that cursor. Every duel finishes on
`2026-08-10`.

**The mid-walk duel: `w` beats `x`.** Loser `x` was served on page one; winner `w` has not been
reached.

| Test | Proves |
| --- | --- |
| `aDuelStampedAtTheCutoffIsInNoPageOfTheWalk` | after the four steps above, the five ids come back **exactly once** across the walk, `x`'s row reads `coins = 1` and `w`'s reads `coins = 0` — the standings they held at the cutoff — and no row anywhere in the walk carries `w = +1` or `x = 0` |
| `theRanksALaterPageCarriesAreTheCutoffsRanks` | on the same run, `w`'s rank is `3`, the rank it held before the mid-walk duel, and the concatenated rank sequence over the walk is `[1, 2, 3, 4, 5]` with no rank smaller than one returned before it |

**Named mutations.** Making the window's upper bound `<=` instead of
`<`, or using `season.endExclusive` as the bound, admits the at-cutoff duel and reddens both tests.

Re-reading the clock on a cursored request is **not** a mutation this ticket can catch: under
`Clock.fixed` every call returns the same instant, so a live read reproduces the value already
stored in the cursor and nothing observable changes. The live-versus-pinned property belongs to
`TASK-050209`'s `aCursoredRequestReusesTheCursorsCutoffAndNotTheClock`, whose cursor carries an
`asOf` deliberately different from the clock's.

## Acceptance criteria

- [ ] `StandingsWalkDatabaseTest.aDuelStampedAtTheCutoffIsInNoPageOfTheWalk` passes, with the duel
      recorded **after** page one is read and **before** the rest, and with its winner on a later
      page and its loser on the page already served
- [ ] `StandingsWalkDatabaseTest.theRanksALaterPageCarriesAreTheCutoffsRanks` passes
- [ ] The winner's role is assigned from `sortedByDescending { it.id.value }`, with a comment saying
      why `UUID`'s natural order is not used
- [ ] `everyPlayerOfTheLadderComesBackExactlyOnceOverTheWalk` and
      `theRanksAWalkReturnsAreTheWholeLaddersAndNeverDecrease` pass with their assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
