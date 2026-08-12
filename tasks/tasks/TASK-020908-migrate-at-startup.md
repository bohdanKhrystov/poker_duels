---
schema: 2
id: TASK-020908
title: Open the pool and migrate at startup, and make a second startup a no-op
type: task
status: backlog
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, persistence, startup]
depends_on: [TASK-020904, TASK-020907]
verify:
  - ./gradlew :poker-server:test --tests '*DatabaseStartupTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*HealthRouteTest' --tests '*ServerPluginsTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Starting the server opens the connection pool and brings the schema up to date exactly once;
starting it again against the same database applies nothing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DatabaseStartupTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/db/Database.kt`,
`poker-server/src/main/kotlin/duels/poker/server/db/Migrations.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`.

## Scope

- Add one testable top-level function to `Application.kt`, and have `main` call it:

  ```kotlin
  /** Opens the single connection pool and brings the schema up to date. */
  public fun startDatabase(config: ServerConfig): HikariDataSource {
      val pool = Database.connectionPool(config)
      Migrations.migrate(pool)
      return pool
  }
  ```

- `main` becomes: load the config, `val pool = startDatabase(config)`, start the embedded server
  as it does today, and `pool.close()` after `start(wait = true)` returns. Migrating before the
  socket opens is the point — a server that accepts a connection before its schema exists answers
  with a runtime error instead of failing at startup.
- **`Application.module()` is unchanged.** It takes no `DataSource`, opens no connection and
  imports nothing from `duels.poker.server.db`. `HealthRouteTest`, `ServerPluginsTest` and
  `PokerServerModuleTest` observe `module()` only, so nothing they assert changes and no
  existing test gains a Docker requirement — which is why the second `verify` command runs two of
  them without `-PrequireDocker=true`.
- The test builds a `ServerConfig` pointing at the container the same way `DatabasePoolTest` does,
  after `PostgresTestSupport.freshDatabase()` has reset the schema, and closes every pool it
  opens.

## Out of scope

- Wiring a `DataSource` into routes, sessions or repositories — `STORY-0210`.
- A graceful shutdown hook, connection draining or a readiness probe that checks the database —
  `EPIC-07`.
- Changing `/health` to report database status.
- Flyway `validate`, `baseline` or `repair` behaviour beyond what `Migrations.migrate` already
  does.

## Tests

`DatabaseStartupTest`, JUnit 5, package `duels.poker.server`.

| Test | Proves |
| --- | --- |
| `startupAppliesTheSchema` | after `startDatabase(config)` on a freshly reset database, `information_schema.tables` contains `player`, `duel` and `duel_result` |
| `aSecondStartupAppliesNothing` | calling `startDatabase(config)` twice leaves `flyway_schema_history` with the same row count as after the first call |
| `startupReturnsAUsablePool` | the returned pool serves a connection answering `SELECT 1`, and is closed by the test |

## Acceptance criteria

- [ ] `DatabaseStartupTest.startupAppliesTheSchema` passes
- [ ] `DatabaseStartupTest.aSecondStartupAppliesNothing` passes
- [ ] `DatabaseStartupTest.startupReturnsAUsablePool` passes
- [ ] `./gradlew :poker-server:test --tests '*HealthRouteTest' --tests '*ServerPluginsTest'` exits
      0 **without** `-PrequireDocker=true` and with no test skipped, proving `module()` still
      needs no database
- [ ] The body of `Application.module()` is unchanged in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
