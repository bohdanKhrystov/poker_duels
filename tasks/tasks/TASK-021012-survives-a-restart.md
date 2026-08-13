---
schema: 2
id: TASK-021012
title: Prove profiles, results and balances survive a restart
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, profiles, coins]
depends_on: [TASK-021011]
verify:
  - ./gradlew :poker-server:test --tests '*PersistenceSurvivesRestartTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A profile, its duel result and its coin balance are still there after the application's database
layer is shut down and started again — the reason `ADR-0011` put PostgreSQL in v0.1 at all.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/PersistenceSurvivesRestartTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/DatabaseStartupTest.kt` (`startDatabase`, and the
`ServerConfig`-from-`containerCoordinates` idiom to copy),
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt`,
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt`.

## Scope

- New class in package `duels.poker.server`. The "restart" is a real one at the layer this story
  owns: `startDatabase(config)` → write → `pool.close()` → `startDatabase(config)` again → read
  back through a *new* directory and store built over the new pool.
- `@BeforeEach` calls `PostgresTestSupport.containerCoordinates()` **once** and keeps the resulting
  `ServerConfig` — it resets the schema, so calling it a second time inside a test would erase what
  the test is proving. Build the config exactly as `DatabaseStartupTest.buildServerConfig` does,
  with `databasePoolSize = 2`.
- A private helper does the write phase against a given pool: resolve `DeviceId("survivor")` and
  `DeviceId("opponent")`, record one duel won by seat 0, and return the two `PlayerId`s.
- Private read helpers over a pool: `coinBalanceOf(pool, playerId)`, `duelRowCount(pool)`,
  `duelResultRowCount(pool)`.
- Both pools are closed in a `finally` or with `use`, so the suite does not leak connections into
  later classes.

## Out of scope

- Restarting a Ktor application, a socket, or anything above the database layer — `STORY-0212`
  owns end-to-end wiring, and `Application.module()` still takes no `DataSource`.
- Restarting the PostgreSQL container itself. The container is shared by the whole suite and
  `PostgresTestSupport` owns its lifecycle; what this story can lose on restart is process state,
  and that is what the test kills.
- In-flight duel state surviving a restart — `ADR-0011` explicitly does not require it.

## Tests

`PersistenceSurvivesRestartTest`, JUnit 5, package `duels.poker.server`. Bodies that call `resolve`
or `record` run inside `runBlocking`.

| Test | Proves |
| --- | --- |
| `theSameDeviceResolvesToTheSameProfileAfterARestart` | after the write phase, a close and a second `startDatabase`, resolving `DeviceId("survivor")` returns the same `PlayerId` and leaves the `player` row count at `2` |
| `resultsAndBalancesSurviveARestart` | after the same restart, `duelRowCount(pool) == 1`, `duelResultRowCount(pool) == 2`, and the balances read back as `1` for the winner and `-1` for the loser |

## Acceptance criteria

- [ ] `PersistenceSurvivesRestartTest.theSameDeviceResolvesToTheSameProfileAfterARestart` passes
- [ ] `PersistenceSurvivesRestartTest.resultsAndBalancesSurviveARestart` passes
- [ ] `PostgresTestSupport.containerCoordinates()` is called at most once per test
- [ ] Every pool opened in the test is closed
- [ ] No file other than the new test class is added or changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
