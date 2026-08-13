---
schema: 2
id: TASK-021111
title: Read a just-finished duel and its coin back over HTTP, against the database
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, http, persistence, end-to-end]
depends_on: [TASK-021110]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileEndpointsDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

With the real routes over the real `PostgresProfileReads` over a real PostgreSQL, a duel recorded
moments earlier comes back over HTTP with its opponent, outcome and coin, and an unknown device id
is refused without creating a row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` (the fixture and
`PostgresTestSupport` idiom to copy),
`poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` (the `testApplication`
idiom),
`poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt`,
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt`.

## Scope

- New class in `duels.poker.server.http`. `@BeforeEach` takes `PostgresTestSupport.freshDatabase()`,
  runs `Migrations.migrate(dataSource)`, and builds a `PostgresPlayerDirectory`, a
  `PostgresDuelResultStore` and a `PostgresProfileReads` over it. **Nothing between the HTTP call
  and the database is a double** — a fake here would only repeat `ProfileRouteTest`.
- Each test runs `testApplication { application { module(); profileRoutes(reads) } }` and calls
  `client.get("/api/me")` / `client.get("/api/me/duels")` with a `X-Device-Id` header.
- The duel is recorded with `finishedAt = Instant.now()` and `startedAt` a minute earlier — this is
  the one place a wall clock is right, because the claim under test is that a duel finished *just
  now* is already readable.
- Bodies are decoded with `protocolJson.decodeFromString<…>(response.bodyAsText())` rather than
  matched as strings, so a field in the wrong order does not fail the test and a missing one does.
- One private JDBC helper, `playerRowCount()`, copied from the read-path test.

## Out of scope

- Playing an actual duel over a socket to produce the result — `STORY-0212`, which also wires these
  routes into `Application.module()`.
- Re-testing the limit rules, the blank header or the ordering: `ProfileRouteTest` and
  `PostgresProfileReadsTest` own those. This ticket proves the three claims below and stops.
- Any change to production code. If something is missing to make this test pass, that is a defect
  in an earlier ticket's file and becomes its own ticket.

## Tests

`ProfileEndpointsDatabaseTest`, JUnit 5, package `duels.poker.server.http`. Setup resolves
`DeviceId("alice")` and `DeviceId("bob")` and records one duel won by seat 0 inside `runBlocking`.

| Test | Proves |
| --- | --- |
| `aDuelThatJustFinishedAppearsInTheList` | `GET /api/me/duels` as `alice` answers `200` and decodes to one summary whose `opponentPlayerId` is bob's player id, `outcome` is `WON`, `coinDelta` is `1`, and whose `finishedAt` parses as an `Instant` |
| `theLosersBalanceComesBackOverTheWireAsMinusOne` | `GET /api/me` as `bob` answers `200` and decodes to `coinBalance == -1` |
| `anUnknownDeviceIsRefusedAndCreatesNoProfile` | `GET /api/me` with `X-Device-Id: ghost` answers `401`, and `playerRowCount()` is the same before and after |

## Acceptance criteria

- [ ] `ProfileEndpointsDatabaseTest.aDuelThatJustFinishedAppearsInTheList` passes
- [ ] `ProfileEndpointsDatabaseTest.theLosersBalanceComesBackOverTheWireAsMinusOne` passes
- [ ] `ProfileEndpointsDatabaseTest.anUnknownDeviceIsRefusedAndCreatesNoProfile` passes
- [ ] The test class names no fake or stub of `ProfileReads`, `PlayerDirectory` or
      `PostgresDuelResultStore`
- [ ] No file other than the new test class is added or changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
