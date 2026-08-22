---
schema: 2
id: TASK-050605
title: Every standing on the ladder is that player's own season results, and the ladder totals zero
type: task
status: ready
parent: STORY-0506
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, leaderboard, coins, conservation, tests]
depends_on: [TASK-050604]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.everyStandingIsThatPlayersOwnSeasonResultsAndTheLadderTotalsZero' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`STORY-0506`'s conservation criterion, and the one `ADR-0063` §4 names this story as the check for:
after the duel, every number the ladder prints is the sum of that player's own stored results
inside the season, and the whole ladder adds up to exactly `0`.

## The tautology this must not become

**A season's coin deltas sum to zero whatever window you read them through.** Every duel writes two
rows summing to zero (`ADR-0015` makes a draw two zeroes rather than nothing), so a ladder that
included last month's duels, or every duel ever played, would *still* total zero. A test that
asserted only the total would pass under the exact mutation the story is worried about.
`TASK-050207` hit this and answered it inside one query; this ticket answers it end to end.

So the assertion with teeth is **per player**: `row.coins` equals the sum of that player's
`coinDelta`s over the duels the product itself reports, filtered to the season. And the fixture
carries a player for whom those two totals differ — `fillerOne`, whose win last season makes their
all-time record `0` and their season standing `−1`. Reading the wrong window changes that player's
row and nothing else's, so the fixture is the whole test.

The zero total is asserted too, *after* the per-player check, because it catches a different fault:
a ladder that lost one row of a duel, or invented one. Neither assertion subsumes the other, and
the test should say so where it makes them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` —
  `walkAllPages`, the bounded page walk to copy.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketHistoryTest.kt` — `recentDuelsOf`,
  the `GET /api/me/duels` read to copy.
- `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` —
  `RecentDuelsResponse`, `DuelSummaryResponse.coinDelta`, `.finishedAt`.
- `docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md`
  — §4.

## Scope

- Two private helpers on `SocketLadderTest`:
  - `private suspend fun HttpClient.walkLadder(deviceId: String?, limit: Int): List<StandingRow>` —
    follows `nextCursor` from the first page to the last, concatenating `rows`, bounded by a
    `MAX_LADDER_WALK_REQUESTS` constant and failing with a message naming the last cursor if it
    does not terminate, exactly as `StandingsWalkDatabaseTest.walkAllPages` does. The cursor is
    passed straight through, never decoded and re-encoded.
  - `private suspend fun HttpClient.recentDuelsOf(deviceId: String): RecentDuelsResponse`, asserting
    `200`, decoding with `protocolJson`.
- One new test, walking with `limit = 2` so the walk is a real walk — two pages and a cursor
  between them — rather than one page wearing a loop.
- No production file is created or modified.

## An assertion shape this story has already been caught by

`TASK-050603` shipped two *unchanged across the duel* assertions of the form
`assertEquals(beforeFiller, afterFiller)`. Deep review confirmed by isolating them that they **pass
vacuously** under a ladder that is consistently wrong on both reads — they compare a value to itself,
and carry weight only because a *separate, earlier* assertion pinned the before-value to a literal.
That safety net is positional, not structural, and it disappears the moment the idiom is copied.

**So: every "unchanged" assertion in this story must have its value pinned to a literal on at least
one side, in the same test method — never merely equal to its own earlier reading.**

## Out of scope

- **A conservation claim over the `duel_result` table read by SQL.** Everything compared here is
  something the product answers over HTTP; the repository boundary (`ADR-0011`) means no test in
  this package opens a `Connection` to check arithmetic.
- **Any assertion that a *page* sums to zero.** A page total means nothing; the walk is the point.
- **`GET /api/me`'s `coinBalance`** — `TASK-050608`.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

**Fixture.** `openSocketDuel()`, `seedTheLadderTheDuelArrivesInto(host, guest)`, `playToFinish()`.
The season ladder afterwards is `guest +1`, `host 0`, `fillerTwo 0`, `fillerOne −1` — four rows,
totalling zero — while `fillerOne`'s stored record over *all* time is `0` and `fillerTwo`'s is `−1`,
because of the duel at `lastSeasonAt()`.

| Test | Proves |
| --- | --- |
| `everyStandingIsThatPlayersOwnSeasonResultsAndTheLadderTotalsZero` | walks every page with `limit = 2` and asserts, **in this order**: (1) the walk returned exactly four rows, one per player, and the four player ids are `host`, `guest`, `fillerOne`, `fillerTwo`; (2) for each of the four, `row.coins` equals the sum of `coinDelta` over that player's `GET /api/me/duels` entries whose `finishedAt`, parsed with `Instant.parse`, satisfies `ladderSeason.contains(...)`, and that player's `nextCursor` is `null` so no result was left unread; (3) `fillerOne`'s season sum is `−1` while the sum over their whole unfiltered list is `0`, named in the test as the player who makes the window observable; (4) the four `coins` sum to exactly `0` |

**Named mutations.** Dropping the window's lower bound in the standings query — counting last
season — reddens step (2) on `fillerOne` and `fillerTwo` and **leaves step (4) green**, which is
the whole reason step (2) exists. Losing one of a duel's two result rows reddens step (4) and the
affected player's step (2). Reading `player.coin_balance` instead of the window reddens step (2)
for both fillers. Returning a page instead of the walk reddens step (1).

## Acceptance criteria

- [ ] `SocketLadderTest.everyStandingIsThatPlayersOwnSeasonResultsAndTheLadderTotalsZero` passes
- [ ] The walk uses `limit = 2` and follows `nextCursor` to a `null`, and the row count is asserted
      before any total
- [ ] For each of the four players, the ladder's `coins` is compared against a sum computed from
      that player's `GET /api/me/duels`, filtered by `ladderSeason.contains(...)`
- [ ] The test asserts `fillerOne`'s season sum is `−1` and their unfiltered sum is `0`
- [ ] The zero total is asserted after the per-player comparisons, and a comment states that it
      would survive a wrong window
- [ ] No test in this file opens a database connection or runs SQL
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
