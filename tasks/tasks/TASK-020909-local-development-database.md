---
schema: 2
id: TASK-020909
title: Give a fresh clone a local database with docker compose
type: task
status: blocked
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, persistence, docs, tooling]
depends_on: [TASK-020902, TASK-020903]
verify:
  - ./gradlew :poker-server:test --tests '*DevDatabaseComposeTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---


> **Blocked: no Docker on the build machine (2026-08-13).** This ticket's `verify:` block carries
> `-PrequireDocker=true`, which is deliberate — it means a machine without Docker cannot honestly
> close a database ticket. Docker is not installed here (`docker` binary absent, no
> `/var/run/docker.sock`), so the block cannot exit 0 and the ticket is not done. The implementation
> may be complete; the verification is not. Unblock by installing Docker, or by running this ticket
> on CI.

## Goal

`docker compose up -d` gives a PostgreSQL that matches `ServerConfig`'s defaults exactly, and
`CONTRIBUTING.md` says what happens to the test suite when Docker is not there.

## Files

| File | Action |
| --- | --- |
| `docker-compose.yml` | create |
| `CONTRIBUTING.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/DevDatabaseComposeTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`.

## Scope

- `docker-compose.yml` at the repository root, Compose v2 (no `version:` key), one service:

  ```yaml
  services:
    postgres:
      image: postgres:16-alpine
      environment:
        POSTGRES_DB: poker_duels
        POSTGRES_USER: poker
        POSTGRES_PASSWORD: poker
      ports:
        - "5432:5432"
      volumes:
        - poker-duels-postgres:/var/lib/postgresql/data

  volumes:
    poker-duels-postgres:
  ```

  The image tag matches `PostgresTestSupport.IMAGE`, so the database a developer runs is the
  database the tests run. The credentials match `ServerConfig`'s defaults, so `./gradlew run`
  against this container needs no environment variable at all.
- A new `### The development database` subsection under `## Local setup` in `CONTRIBUTING.md`,
  covering three things and nothing else:
  1. `docker compose up -d` and `docker compose down -v`, with the credentials.
  2. The tests do **not** use this container — Testcontainers starts its own — so a developer
     needs Docker, not a running compose stack, to run `./gradlew check`.
  3. **What a missing Docker means**: `./gradlew check` *skips* the database tests with a message
     naming the flag, and stays green, so work on the engine and the protocol is unaffected;
     `./gradlew check -PrequireDocker=true` *fails* instead, and that is what CI runs, because a
     suite that skips silently is a suite that has stopped testing.
- The test reads the compose file from the repository root. A Gradle test's working directory is
  the module directory, so the path is `File("../docker-compose.yml")`; if it does not exist the
  test fails with a message naming the path it tried, and never passes by default.

## Out of scope

- A service for the server itself, an image build, a Dockerfile, healthchecks or a production
  compose file — `EPIC-07`.
- A seed-data or fixture script.
- Making the test suite talk to the compose database. Testcontainers owns the test database
  (`TASK-020903`); pointing tests at a hand-started container would make the suite depend on
  local state.
- Running `docker compose` from a Gradle task or from `verify` — the commands here must be
  runnable on a machine with no Docker CLI.

## Tests

`DevDatabaseComposeTest`, JUnit 5, package `duels.poker.server.db`. Plain text assertions on the
file's contents — no YAML library, no Docker.

| Test | Proves |
| --- | --- |
| `theComposeFileExists` | `File("../docker-compose.yml")` exists and is not empty |
| `theComposeDatabaseMatchesTheServerConfigDefaults` | the file contains `POSTGRES_DB: poker_duels`, `POSTGRES_USER: ${ServerConfig.DEFAULT_DATABASE_USER}` and `POSTGRES_PASSWORD: ${ServerConfig.DEFAULT_DATABASE_PASSWORD}`, publishes `"5432:5432"`, and `ServerConfig.DEFAULT_DATABASE_URL` is `jdbc:postgresql://localhost:5432/poker_duels` |
| `theComposeImageMatchesTheTestContainerImage` | the file contains `image: ${PostgresTestSupport.IMAGE}` |

## Acceptance criteria

- [ ] `DevDatabaseComposeTest.theComposeFileExists` passes
- [ ] `DevDatabaseComposeTest.theComposeDatabaseMatchesTheServerConfigDefaults` passes
- [ ] `DevDatabaseComposeTest.theComposeImageMatchesTheTestContainerImage` passes
- [ ] The two default constants are referenced from `ServerConfig`, not retyped as literals in the
      test — a drift in either file fails the test
- [ ] `CONTRIBUTING.md` states both behaviours: skipped and green without Docker, failed with
      `-PrequireDocker=true`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
