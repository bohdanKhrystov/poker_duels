---
schema: 2
id: TASK-050212
title: The shipped server installs the ladder route, on the wall clock the root owns
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, wiring, leaderboard]
depends_on: [TASK-050211]
verify:
  - ./gradlew :poker-server:test --tests '*DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ServerComponentsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - "grep -qF 'standingsRoutes(components.reads, components.standings, components.wallClock)' poker-server/src/main/kotlin/duels/poker/server/Application.kt"
---

## Goal

The server `main` starts answers `GET /api/standings`, backed by the same pool and the same
`java.time.Clock` every other component is built from.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | modify |

## Scope

- `ServerComponents` gains `val standings: StandingsReads`, built by `serverComponents` as
  `PostgresStandingsReads(dataSource)` — the same `dataSource` every other component takes, so the
  ladder reads through the one pool.
- `duelServer` installs the route, spelled exactly as the `verify:` line greps for:

  ```kotlin
  standingsRoutes(components.reads, components.standings, components.wallClock)
  ```

  `components.wallClock` is `TASK-050201`'s single `Clock.systemUTC()`. Nothing here constructs a
  clock, and `Clock.systemUTC()` still appears exactly once under `src/main`.
- One test added to `DuelServerRoutesTest`, which already boots the shipped server against a real
  database.

## Out of scope

- **Changing what the route does.** This ticket wires; `TASK-050209`–`TASK-050211` decided the
  behaviour and their tests still pass unchanged.
- **A second `ServerComponents` field for the clock.** `wallClock` already exists; this ticket adds
  `standings` and nothing else.
- **The sweep, the socket, the auth routes or the profile routes.** Their lines in `duelServer` are
  untouched and their tests do not move.
- **Any HTTP-plus-database assertion about ladder contents** — `TASK-050213` onwards. This test
  proves the route is installed, not what it computes.

## Tests

`DuelServerRoutesTest`, `-PrequireDocker=true`. One test added; every existing test in the file
keeps its assertions.

| Test | Proves |
| --- | --- |
| `theStandingsRouteAnswersWithoutAProfile` | `GET /api/standings` **with no `X-Device-Id` header** answers `200`, its body decodes as a `StandingsResponse`, `rows` is empty on a fresh database, `nextCursor` is `null`, `self` is `null`, and `season` matches `\d{4}-\d{2}` |

**Named mutation.** Deleting the `standingsRoutes(...)` line makes the request `404` and reddens the
test. Installing it with a clock built at the call site would still pass this test, which is why
`verify:` greps for the exact spelling and `TASK-050201`'s one-`Clock.systemUTC()` grep still runs
in CI.

## Acceptance criteria

- [ ] `DuelServerRoutesTest.theStandingsRouteAnswersWithoutAProfile` passes
- [ ] `serverComponents` builds `PostgresStandingsReads(dataSource)` and exposes it as
      `ServerComponents.standings`
- [ ] `duelServer` contains the exact line the `verify:` grep looks for
- [ ] Every test already in `DuelServerRoutesTest` and `ServerComponentsTest` passes with its
      assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
