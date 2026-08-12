---
schema: 2
id: TASK-020903
title: Start one PostgreSQL container for the suite, and decide what a missing Docker means
type: task
status: blocked
parent: STORY-0209
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, persistence, test-harness, ci]
depends_on: [TASK-020901]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresTestSupportTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---


> **Blocked: no Docker on the build machine (2026-08-13).** This ticket's `verify:` block carries
> `-PrequireDocker=true`, which is deliberate — it means a machine without Docker cannot honestly
> close a database ticket. Docker is not installed here (`docker` binary absent, no
> `/var/run/docker.sock`), so the block cannot exit 0 and the ticket is not done. The implementation
> may be complete; the verification is not. Unblock by installing Docker, or by running this ticket
> on CI.
>
> An implementation already exists on branch `task/TASK-020903-postgres-test-harness`,
> unmerged. Its skip path was verified green and its fail path verified to fail correctly; only the
> `-PrequireDocker=true` run is unverifiable here. Review that branch before re-dispatching a coder.

## Goal

Every database test in this module gets an empty PostgreSQL schema from one shared container, and
a machine without Docker skips those tests with a message while CI fails loudly.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupportTest.kt` | create |
| `.github/workflows/build.yml` | modify |

## Scope

- Package `duels.poker.server.db`, **test sources only** — this harness never ships.
- One top-level declaration, `object PostgresTestSupport`, so the file name matches. Everything
  below is a member of it:

  ```kotlin
  const val IMAGE: String = "postgres:16-alpine"

  internal enum class Policy { RUN, SKIP, FAIL }

  internal fun policy(dockerAvailable: Boolean, dockerRequired: Boolean): Policy

  fun requireDocker()
  fun freshDatabase(): DataSource
  ```

- `policy` is the whole decision, and it is a pure function so it can be tested on any machine:
  `dockerAvailable` ⇒ `RUN`; otherwise `dockerRequired` ⇒ `FAIL`; otherwise `SKIP`.
- `requireDocker()` evaluates `dockerRequired` as
  `System.getProperty("poker.requireDocker") == "true" || System.getenv("CI") != null`, reads
  availability from `DockerClientFactory.instance().isDockerAvailable`, and then:
  - `RUN` — return.
  - `SKIP` — `Assumptions.assumeTrue(false, message)`, so JUnit reports the test as skipped and
    `check` stays green. The message must name the remedy: *"Docker is not available, so the
    PostgreSQL tests were skipped. Start Docker, or run with -PrequireDocker=true to make this a
    failure."*
  - `FAIL` — throw `IllegalStateException` with a message saying Docker was required and not
    found. A suite that skips silently in CI is a suite that has stopped testing.
- The container is a JVM-wide singleton, started on first use and never stopped — Testcontainers'
  Ryuk removes it when the JVM exits. One container for the whole suite is why this is a shared
  harness and not a per-class `@Container`:

  ```kotlin
  val container: PostgreSQLContainer<*> by lazy {
      PostgreSQLContainer(DockerImageName.parse(IMAGE)).also { it.start() }
  }
  ```

- `freshDatabase()` calls `requireDocker()` **as its first statement** — so any test that asks for
  a database is gated automatically and no test class has to remember — then resets the schema and
  returns a plain `DataSource` over the container:

  ```kotlin
  val source = PGSimpleDataSource().apply {
      setUrl(container.jdbcUrl); user = container.username; password = container.password
  }
  source.connection.use { connection ->
      connection.createStatement().use { it.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public") }
  }
  ```

  Dropping the schema also drops Flyway's history table, which is what makes "apply every
  migration to an empty database" mean what it says.
- The suite is single-threaded (no parallel test execution is configured, and none is added here),
  so one container with a reset schema is safe.
- In `.github/workflows/build.yml`, change the check step to `./gradlew check -PrequireDocker=true`
  and nothing else. GitHub runners provide Docker; from now on, a runner without it fails the
  build instead of quietly testing nothing.

## Out of scope

- Any schema, table or migration — `TASK-020904`. This ticket's container holds an empty
  `public` schema and knows no poker.
- HikariCP — `TASK-020907`. `freshDatabase()` deliberately returns an unpooled
  `PGSimpleDataSource`, so a pool bug can never look like a schema bug.
- Reusing a container across Gradle runs (`withReuse`), or a fixed host port.
- `docker-compose.yml` and the contributor documentation — `TASK-020909`.

## Tests

`PostgresTestSupportTest`, JUnit 5, package `duels.poker.server.db`. The first three tests call
the pure `policy` function and need no Docker at all.

| Test | Proves |
| --- | --- |
| `runsWhenDockerIsAvailable` | `policy(dockerAvailable = true, dockerRequired = false) == Policy.RUN`, and it is still `RUN` when required |
| `skipsWhenDockerIsAbsentAndNotRequired` | `policy(false, false) == Policy.SKIP` |
| `failsWhenDockerIsAbsentAndRequired` | `policy(false, true) == Policy.FAIL` |
| `theFreshDatabaseAnswersSelectOne` | `freshDatabase()` gives a connection where `SELECT 1` returns `1` |
| `theFreshDatabaseHasNoTables` | after `freshDatabase()`, `SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'` is `0` |

## Acceptance criteria

- [ ] `PostgresTestSupportTest.runsWhenDockerIsAvailable` passes
- [ ] `PostgresTestSupportTest.skipsWhenDockerIsAbsentAndNotRequired` passes
- [ ] `PostgresTestSupportTest.failsWhenDockerIsAbsentAndRequired` passes
- [ ] `PostgresTestSupportTest.theFreshDatabaseAnswersSelectOne` passes
- [ ] `PostgresTestSupportTest.theFreshDatabaseHasNoTables` passes
- [ ] `./gradlew :poker-server:test --tests '*PostgresTestSupportTest' -PrequireDocker=true` exits
      0 with the two container tests reported as executed, not skipped
- [ ] `.github/workflows/build.yml` runs `./gradlew check -PrequireDocker=true`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
