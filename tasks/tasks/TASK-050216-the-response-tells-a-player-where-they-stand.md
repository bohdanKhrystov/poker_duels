---
schema: 2
id: TASK-050216
title: The response tells a player where they stand — on the page drawn, and on a page they are not on
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, db, leaderboard, self-standing, tests]
depends_on: [TASK-050215]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsSelfDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The self standing in the response is a real whole-season aggregate over a real database — right for
a player sitting on page three of a ladder whose page one they asked for, and identical on every
page of one walk.

## The two fixtures, and why one is not enough

`ADR-0065` §8: *"two inputs, one player on the page and one off it, because a single fixture cannot
tell a real aggregate from an echo of the page."* An implementation that finds the requester among
the rows it just drew is **correct whenever they are on screen** and silently wrong the rest of the
time — which is the entire case the self line exists for. Both inputs are in this ticket, and a
reviewer's first check is that the off-page one exists.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsSelfDatabaseTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | read |

## Scope

- A new Testcontainers-backed HTTP test class, set up exactly like `StandingsWalkDatabaseTest`:
  fresh database, migrations, `PostgresPlayerDirectory`, `PostgresDuelResultStore`, and the route
  installed with `PostgresProfileReads`, `PostgresStandingsReads` and
  `Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`.
- The requester is named by the `X-Device-Id` header — the same credential `GET /api/me` takes,
  and the **only** way this endpoint learns who is asking.
- No production file changes.

## Out of scope

- **A `playerId` query parameter.** `ADR-0065` §3: an endpoint answering *what is player X's
  standing* for any X asked is `DEC-057`'s question, and this story does not pre-empt it. No request
  in this file carries one.
- **The three answers, the unknown device and the row count** — `TASK-050217`, in this same class.
- **A marker on the requester's row, or a *jump to me*** — `ADR-0065` §5. A player appearing both in
  the self line and in the page is correct, not a duplicate (`ADR-0065` §6), and no test here treats
  it as one.

## Tests

`StandingsSelfDatabaseTest`, `-PrequireDocker=true`, `limit = 2`.

**The fixture — five players, distinct standings, recorded out of order.** Create the profiles as
`e, c, a, d, b` and record:

1. `a` beats `d`;
2. `a` beats `e`;
3. `b` beats `e`;
4. `c` beats `d`;
5. `d` beats `c`.

Standings: `a +2`, `b +1`, `c 0`, `d -1`, `e -2` — sum zero, ranks `1, 2, 3, 4, 5`, pages
`[a, b] [c, d] [e]`.

| Test | Proves |
| --- | --- |
| `theSelfStandingIsTheWholeLaddersForAPlayerOnALaterPage` | page one asked for with `e`'s device: `rows` holds `a` and `b` only, `e`'s id is on none of them, and `self` reads rank `5`, coins `-2`. The number cannot have come from the page |
| `theSelfStandingEqualsTheRowWhenThePlayerIsOnThePageDrawn` | page one asked for with `a`'s device: `self` reads rank `1`, coins `2`, and both equal the values on `a`'s own row in the same response, which is present and is not a duplicate |
| `theSelfStandingIsIdenticalOnEveryPageOfOneWalk` | pages one and two of a single walk, both asked for with `e`'s device: the two `self` objects are **equal**, and `e`'s id is on neither page. It is delivered by the server under one cutoff, not remembered by the client |

**Named mutations.** Computing the self standing from the rows of the page reddens the first test
and leaves the second green — the exact asymmetry that makes one fixture insufficient. Recomputing
`clock.instant()` on a cursored request, or reading the self standing under a different window than
the page, reddens the third the moment the ladder moves and is caught here by the equality of the
two objects.

## Acceptance criteria

- [ ] `StandingsSelfDatabaseTest.theSelfStandingIsTheWholeLaddersForAPlayerOnALaterPage` passes,
      asserting rank `5` and coins `-2` for a player absent from the page returned
- [ ] `StandingsSelfDatabaseTest.theSelfStandingEqualsTheRowWhenThePlayerIsOnThePageDrawn` passes,
      asserting the two numbers against that player's own row
- [ ] `StandingsSelfDatabaseTest.theSelfStandingIsIdenticalOnEveryPageOfOneWalk` passes, comparing
      the whole `self` object across two pages of one walk
- [ ] No request in this file carries a `playerId` parameter
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
