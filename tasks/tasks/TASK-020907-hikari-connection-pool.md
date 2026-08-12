---
schema: 2
id: TASK-020907
title: Open a HikariCP connection pool from ServerConfig
type: task
status: backlog
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, persistence, pool]
depends_on: [TASK-020902, TASK-020903]
verify:
  - ./gradlew :poker-server:test --tests '*DatabasePoolTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---


> **Blocked: no Docker on the build machine (2026-08-13).** This ticket's `verify:` block carries
> `-PrequireDocker=true`, which is deliberate — it means a machine without Docker cannot honestly
> close a database ticket. Docker is not installed here (`docker` binary absent, no
> `/var/run/docker.sock`), so the block cannot exit 0 and the ticket is not done. The implementation
> may be complete; the verification is not. Unblock by installing Docker, or by running this ticket
> on CI.

## Goal

`Database.connectionPool(config)` returns a HikariCP `DataSource` built from `ServerConfig`'s URL,
credentials and pool size, and it serves connections against a real PostgreSQL.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Database.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/DatabasePoolTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`.

## Scope

- Package `duels.poker.server.db`, one top-level `public object Database`:

  ```kotlin
  public fun connectionPool(config: ServerConfig): HikariDataSource
  ```

- The body builds a `HikariConfig` and nothing else: `jdbcUrl = config.databaseUrl`,
  `username = config.databaseUser`, `password = config.databasePassword`,
  `maximumPoolSize = config.databasePoolSize`, `poolName = "poker-duels"`. Every value comes from
  `ServerConfig` — no literal URL, no `System.getenv`, no second place to configure the database.
- KDoc: this is the only pool in the process, the caller owns closing it, and `ADR-0011` keeps SQL
  behind the repository boundary so nothing outside `duels.poker.server.db` should hold this type.
- The test builds a `ServerConfig` pointing at the shared container by calling the constructor
  directly with `PostgresTestSupport.container.jdbcUrl`, `.username`, `.password`, a
  `databasePoolSize` of `2` and any port, and calls `PostgresTestSupport.requireDocker()` in
  `@BeforeEach` — it does not need a migrated schema, so it does not call `freshDatabase()`.
- Close the pool in each test (`use` or an explicit `close()`), so the suite does not leak
  connections into later classes.

## Out of scope

- Migrations and startup wiring — `TASK-020908`.
- Any repository, query or transaction helper — `STORY-0210`.
- Connection timeouts, leak detection, metrics or a health check on the pool. Not ticketed; add
  them when something needs them.
- Making `Application.module()` take a `DataSource`. It does not, and this ticket does not touch
  `Application.kt`.

## Tests

`DatabasePoolTest`, JUnit 5, package `duels.poker.server.db`.

| Test | Proves |
| --- | --- |
| `servesAConnectionThatAnswersSelectOne` | a pool built from a container-shaped `ServerConfig` gives a connection where `SELECT 1` returns `1` |
| `honoursTheConfiguredPoolSize` | with `databasePoolSize = 2`, `pool.maximumPoolSize == 2` and two connections can be held open at once |
| `closingThePoolClosesIt` | after `close()`, `pool.isClosed` is `true` |

## Acceptance criteria

- [ ] `DatabasePoolTest.servesAConnectionThatAnswersSelectOne` passes
- [ ] `DatabasePoolTest.honoursTheConfiguredPoolSize` passes
- [ ] `DatabasePoolTest.closingThePoolClosesIt` passes
- [ ] `Database.kt` contains no string literal starting with `jdbc:` and no `System.getenv`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
