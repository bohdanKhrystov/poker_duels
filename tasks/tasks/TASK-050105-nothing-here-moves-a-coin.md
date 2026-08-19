---
schema: 2
id: TASK-050105
title: Nothing this story adds moves a coin, writes a migration, or reaches the engine
type: task
status: ready
parent: STORY-0501
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 1
labels: [server, seasons, persistence]
depends_on: [TASK-050104]
verify:
  - ./gradlew :poker-server:test --tests '*SeasonMovesNoCoinTest' -PrequireDocker=true
  - ./gradlew :poker-engine:check
  - for t in 'Season(' '.toString()' '.start' '.endExclusive' '.contains(' 'seasonOf(' 'FinishedDuel('; do grep -qF "$t" poker-server/src/test/kotlin/duels/poker/server/season/SeasonMovesNoCoinTest.kt || exit 1; done
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-server/src/main/resources/db/migration)"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine)"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- web-client)"
---

## Goal

The three things `STORY-0501` refuses each have a command that fails if they happen: no coin
moves, no migration is written, and neither `poker-engine` nor the client learns what a season is.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonMovesNoCoinTest.kt` | create |

Read, and nothing else: `poker-server/src/test/kotlin/duels/poker/server/db/CoinBalanceIsSignedTest.kt`
for the fixture SQL and the `@BeforeEach`, `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`
for `freshDatabase()`, and `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt`
for the list of functions to exercise.

## Scope

- One database test, against the shared Testcontainers PostgreSQL that
  `duels.poker.server.db.PostgresTestSupport` already provides. `@BeforeEach` is
  `dataSource = PostgresTestSupport.freshDatabase()` then `Migrations.migrate(dataSource)`, the
  same two lines `CoinBalanceIsSignedTest` opens with.
- A fixture with **rows in it**, inserted with plain SQL the way `CoinBalanceIsSignedTest` does:
  - two `player` rows, `coin_balance` `3` and `-1` (a negative balance is ordinary — `ADR-0014`),
  - one `duel` row whose `finished_at` is `2026-08-20T12:00:00Z`,
  - two `duel_result` rows for that duel, `coin_delta` `+1` and `-1`.
- Two snapshot helpers, each returning a stably ordered list so the comparison is by value:
  `SELECT id, coin_balance FROM player ORDER BY id` and
  `SELECT duel_id, player_id, coin_delta FROM duel_result ORDER BY duel_id, player_id`.
- Between the two snapshots, **exercise every path this story added**: construct a `Season`, call
  `toString()`, read `start` and `endExclusive`, call `contains(…)` on both sides of a boundary,
  call `seasonOf(instant)` and call `seasonOf(duel)` on a `FinishedDuel` built inline.
- `-PrequireDocker=true` is part of the `verify:` command on purpose. Without it a machine with no
  Docker daemon *skips* the test and the gate passes having asserted nothing.
- `STORY-0505` was dropped because `ADR-0061` §5 makes a boundary do nothing at all. This test is
  therefore the **only** place in the product where "a season moved no coin" is checked, which is
  why it is a test and not a sentence.
- The three `git diff` lines below check **this ticket's own branch**, which is all a branch can
  see: by the time this runs, `TASK-050101`–`TASK-050104` have already merged into `develop` and
  are inside the merge base. That is why the same combined check rides on the `verify:` block of
  every ticket in `STORY-0501` rather than only on this one. A story-wide assertion made in one
  place would be vacuous in exactly the way this ticket exists to prevent.

## Out of scope

- **Adding production code.** This ticket is a test and three `verify:` lines. If a season
  function needs changing to make it pass, the finding is that the function writes to the
  database, and that is a defect in the ticket that added it, not work for this one.
- **Asserting anything about the *values* of the balances.** `NoBalanceIsFlooredTest` and
  `CoinBalanceIsSignedTest` already own what a balance may be. This test owns only that the season
  code does not change one.
- **Any read path over a season.** `STORY-0502`.

## Tests

`SeasonMovesNoCoinTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `theFixtureItComparesIsNotEmpty` | before anything is compared: the player snapshot holds **two** rows and the `duel_result` snapshot holds **two** rows. This is the anti-vacuity guard, and it is a separate named test so that it cannot be quietly dropped | deleting any of the fixture inserts — after which `noSeasonFunctionMovesACoin` would compare an empty list to an empty list and pass forever |
| `noSeasonFunctionMovesACoin` | the two snapshots taken either side of exercising `Season(…)`, `toString()`, `start`, `endExclusive`, `contains(…)`, `seasonOf(instant)` and `seasonOf(duel)` are equal, list for list | adding `UPDATE player SET coin_balance = coin_balance - 1` — or any other statement — inside any function in `duels.poker.server.season`. Verify the test is real by making that edit locally, watching it go red, and reverting it |

## Acceptance criteria

- [ ] `SeasonMovesNoCoinTest.theFixtureItComparesIsNotEmpty` passes, asserting two `player` rows
      and two `duel_result` rows
- [ ] `SeasonMovesNoCoinTest.noSeasonFunctionMovesACoin` passes, and the *before* snapshot is taken
      before the season functions are called and the *after* snapshot after them
- [ ] The season functions exercised between the snapshots are all seven named in Scope —
      checked mechanically by the `grep` loop in `verify:`, not by reading the file
- [ ] The test runs rather than skips: the `verify:` command passes `-PrequireDocker=true`
- [ ] `git diff` against the merge base shows **no file** under
      `poker-server/src/main/resources/db/migration` — this branch adds no `V<n>__` file, because
      `ADR-0061` §3 makes a season derived
- [ ] `git diff` against the merge base shows **no file** under `poker-engine` and **no file**
      under `web-client` — the engine learns nothing (`ADR-0061` §3) and no client asserts a season
      (`ADR-0002`)
- [ ] The same combined `git diff` check is present in the `verify:` block of every other ticket in
      `STORY-0501`, so the refusal is checked per branch rather than once at the end
- [ ] `./gradlew :poker-engine:check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
