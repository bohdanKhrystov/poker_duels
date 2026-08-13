---
schema: 2
id: TASK-021004
title: Prove concurrent first contact from one device creates one profile
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, profiles, concurrency]
depends_on: [TASK-021003]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresPlayerDirectoryTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Two connections presenting the same unknown device id at the same moment end up with one profile
between them, proved against a real PostgreSQL rather than argued from the schema.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt`,
`docs/adr/ADR-0012-device-bound-anonymous-profiles.md` (the race this closes, and why the schema
closes it rather than application code).

## Scope

- Add exactly one test to the existing `PostgresPlayerDirectoryTest`. The class's `@BeforeEach`,
  its helpers and its four existing tests are not touched — no assertion in them moves, weakens or
  is renamed.
- The new test launches 16 coroutines on `Dispatchers.IO` inside `runBlocking`, each calling
  `directory.resolve(DeviceId("shared-device"))`, collects the results with `awaitAll` over 16
  `async` calls, and asserts afterwards that `playerRowCount() == 1` and that every returned
  `PlayerId` equals the first.
- A `why` comment naming what is under test: `ADR-0012` puts one-profile-per-device in
  `UNIQUE (device_id)`, so 16 simultaneous inserts produce 15 conflicts that the `ON CONFLICT`
  clause resolves to the surviving row. A directory that checked-then-inserted would create a
  second profile here.
- `PostgresTestSupport.freshDatabase()` hands back an unpooled `DataSource`, so each coroutine
  opens its own connection. Sixteen is chosen to stay well inside PostgreSQL's default connection
  limit while still overlapping.

## Out of scope

- Driving the race through two real WebSocket connections: the socket is not installed from
  `Application.module()` yet (`STORY-0212`), and the race lives in the directory, which is what
  this test exercises.
- Any change to `PostgresPlayerDirectory.kt`. If the test fails, that is a defect to report, not a
  licence to edit the implementation from this ticket.
- Concurrency in the duel write path — `TASK-021011`.

## Tests

`PostgresPlayerDirectoryTest`, the existing class, one test added.

| Test | Proves |
| --- | --- |
| `concurrentFirstContactFromManyConnectionsCreatesOneProfile` | 16 concurrent `resolve` calls for one unknown device leave `playerRowCount() == 1` and return 16 equal `PlayerId`s |

## Acceptance criteria

- [ ] `PostgresPlayerDirectoryTest.concurrentFirstContactFromManyConnectionsCreatesOneProfile`
      passes
- [ ] The four tests already in `PostgresPlayerDirectoryTest` still pass, with their assertions
      unchanged — this ticket only adds a test method
- [ ] No file other than `PostgresPlayerDirectoryTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
