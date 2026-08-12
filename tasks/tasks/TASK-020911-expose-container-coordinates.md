---
schema: 2
id: TASK-020911
title: The test harness hands out database coordinates without a cast
type: task
status: backlog
parent: STORY-0209
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, persistence, test-harness]
depends_on: [TASK-020903, TASK-020907]
verify:
  - ./gradlew :poker-server:test --tests '*DatabasePoolTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresTestSupport` exposes `freshDatabase(): DataSource` and keeps its container `private`. A
test that needs the *coordinates* — URL, user, password, to build its own pool rather than use the
harness's `DataSource` — has no way to ask for them.

`TASK-020907` worked around this by casting the returned `DataSource` to `PGSimpleDataSource` and
reading the fields off it. That works, but it is a cast that the type system cannot justify: change
`freshDatabase` to return a Hikari `DataSource` tomorrow and every such test fails with a
`ClassCastException` at runtime, pointing at the cast rather than at the change that broke it.

Every remaining `STORY-0209` ticket that opens its own pool will otherwise copy that cast.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/DatabasePoolTest.kt` | modify |

## Scope

- Add a small value to `PostgresTestSupport` carrying the three coordinates — a `data class` of
  `val`s, named for what it is (`DatabaseCoordinates` or similar) — and a function returning it for
  the shared container.
- It must reset the schema the same way `freshDatabase()` does, so a test taking coordinates gets
  the same empty-database guarantee as one taking a `DataSource`. Two functions with different
  cleanliness guarantees is a trap.
- Gate it on `requireDocker()` as its first statement, exactly as `freshDatabase()` does.
- Keep `freshDatabase()` and keep the container `private`. This ticket widens the harness's public
  surface by one function, not by exposing Testcontainers types to every caller — a test that can
  reach the `PostgreSQLContainer` can also stop it out from under the rest of the suite.
- Change `DatabasePoolTest` to use it and **delete the cast**.

## Tests

No new test file. `DatabasePoolTest` already covers the behaviour; this ticket changes how it
obtains its coordinates. It must pass with the cast gone.

Confirm by grep that no `as PGSimpleDataSource` remains anywhere in the test sources.

## Done

Both `verify:` commands exit 0, `DatabasePoolTest` passes with no cast, and `PostgresTestSupport`
still keeps its container private.
