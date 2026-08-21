---
schema: 2
id: TASK-050207
title: The number is the window and not the column, and a season's standings sum to exactly zero
type: task
status: ready
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, leaderboard, coins, tests]
depends_on: [TASK-050206]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresStandingsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The two arithmetic properties `ADR-0061` §4 and `ADR-0063` §4 make load-bearing are asserted: the
ladder's number is the season window and **not** `player.coin_balance`, and the whole ladder's
standings add up to exactly `0`.

## Why the first one replaces a criterion the story was written with

`EPIC-05` was written promising the ladder and the profile strip would agree.
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §4
contradicts that **on purpose**: the strip prints an all-time counter that never resets, the ladder
prints `SUM(coin_delta)` inside a month, and from the second season on they differ for anyone who
played in both. This test is the one that would catch an implementation quietly reading the column
— and one fixture where the two happen to agree cannot tell a window from a column, so this one
holds a player where they **disagree** and a player where they **agree**.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | read |

## Scope

- Two tests added to the existing class, each recording its own duels.
- The first reads `player.coin_balance` through `PostgresProfileReads.profileOf(deviceId)` — the
  same path the profile strip uses — rather than by raw SQL, so the comparison is between the two
  answers the product actually gives.
- The second walks **every** page with `limit = 2` and sums the `coins` of every row returned.
- Nothing in production changes.

## Out of scope

- **Changing `ProfileResponse` or `GET /api/me`** — `ADR-0065` §2. `PostgresProfileReads` is read
  and called; it is not edited, and none of its tests moves.
- **An all-time ladder or a second column on the response** — `ADR-0061` §7 and the story's
  out-of-scope table.
- **Asserting the sum over anything narrower than the whole ladder.** A sum over one page proves
  nothing; the walk is the point.

## Tests

`PostgresStandingsReadsTest`, `-PrequireDocker=true`. Season `Season(2026, 8)`,
`asOf = season.endExclusive`.

| Test | Proves |
| --- | --- |
| `theCoinsAreTheSeasonsWindowAndNotTheAllTimeColumn` | alice beat bob twice in **July** and once in **August**: her `ProfileResponse.coinBalance` is `3` and her ladder `coins` is `1`, asserted as two different numbers in one test. Carol beat dave once, in **August only**: her `coinBalance` and her ladder `coins` are both `1`. Two inputs — the disagreeing player is what makes the test able to fail, the agreeing one is what stops the assertion being *the two are never equal* |
| `theSeasonsStandingsSumToExactlyZero` | over a fixture holding at least one **draw** and at least two decisive duels among five or more players, walking every page with `limit = 2` returns rows whose `coins` sum to `0`, and the walk returned at least five rows |

**Named mutations.** Reading `p.coin_balance` instead of the CTE's `coins` reddens the first test on
alice. Dropping the season's lower bound reddens it too, by counting July into August. Dropping
either player's row of a duel — the `LEFT JOIN`/`coin_delta <> 0` family of mistakes — reddens the
second, because every duel writes two rows summing to zero and a ladder whose total is non-zero has
lost a row or invented one (`ADR-0063` §4).

**The row-count assertion in the second test is not decoration.** An empty ladder also sums to zero,
so the test asserts the walk returned the rows the fixture recorded before it asserts their total.

## Acceptance criteria

- [ ] `PostgresStandingsReadsTest.theCoinsAreTheSeasonsWindowAndNotTheAllTimeColumn` passes,
      asserting `coinBalance = 3` and ladder `coins = 1` for alice **and** equality for carol
- [ ] The `coinBalance` in that test is read through `PostgresProfileReads`, not by raw SQL
- [ ] `PostgresStandingsReadsTest.theSeasonsStandingsSumToExactlyZero` passes, summing over every
      page of a walk and asserting the row count before the total
- [ ] `PostgresProfileReads.kt` is unchanged and `PostgresProfileReadsTest` passes with its
      assertions unchanged
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
