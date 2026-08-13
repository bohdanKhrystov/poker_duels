---
schema: 2
id: TASK-021204
title: A test server on a real database, with the hand seeds the test chooses
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, testing, end-to-end]
depends_on: [TASK-021203]
verify:
  - ./gradlew :poker-server:test --tests '*E2eServerTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Three lines in a test start the shipping composition root against a fresh PostgreSQL schema, with
every hand's seed derived from one long the test names.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServerTest.kt` | create |

Read, do not modify: `ServerComponents.kt`, `Application.kt` (`duelServer`),
`db/PostgresTestSupport.kt`, `test/duel/PlayedDuel.kt` (how it derives hand seeds from one seed).

## Scope

- New package `duels.poker.server.e2e`, one file, `internal` throughout:

  ```kotlin
  internal const val HAND_SEED: Long = 0x5EED_000000000001L

  internal fun freshMigratedDatabase(): DataSource
  internal fun handSeedSource(seed: Long): HandSeedSource
  internal fun e2eConfig(): ServerConfig
  internal fun ApplicationTestBuilder.installDuelServer(dataSource: DataSource, handSeed: Long = HAND_SEED)
  internal suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage
  internal suspend fun DefaultClientWebSocketSession.completeHandshake(deviceId: String): ServerMessage.Welcome
  ```

- `freshMigratedDatabase()` is `PostgresTestSupport.freshDatabase()` followed by
  `Migrations.migrate(...)`, returned.
- `handSeedSource(seed)` threads one `SplitMix64Rng(seed)`, exactly as `playDuel` does: each call
  returns `draw.value` and keeps `draw.next`. Same seed, same sequence of hand seeds, forever —
  that is what lets a failing end-to-end duel be replayed from the number in its message.
- `installDuelServer` is
  `application { duelServer(serverComponents(e2eConfig(), dataSource, seeds = handSeedSource(handSeed))) }`
  and nothing else. **The test never assembles `SocketDependencies` itself**: a harness that
  hand-wires the collaborators cannot catch a route the production root forgot to install, which
  is the defect this whole story exists to catch.
- `e2eConfig()` is `ServerConfig` built field by field with the shipped defaults; the database
  fields are unused here because the `DataSource` is passed separately, so give them the defaults
  and say so in one line of KDoc.
- `completeHandshake` sends `Hello(deviceId = deviceId)` and returns the decoded
  `ServerMessage.Welcome`, failing loudly on anything else.

## Out of scope

- Rooms, seats, duels, and anything that reads a `ServerMessage` past `Welcome` — `TASK-021205`.
- The action policy and its seed — `TASK-021206` declares `POLICY_SEED`.
- Any `@Tag` or new Gradle task. This story's tests run on `:poker-server:test`; if they ever get
  slow enough to need their own task, that is a new ticket, not a guess made here.

## Tests

`E2eServerTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`.

| Test | Proves |
| --- | --- |
| `theSameSeedGivesTheSameHandSeeds` | two `handSeedSource(7L)` instances yield equal first three values, **and those three values are not all equal to each other** — without the second half, a source returning a constant would pass |
| `theInstalledServerWritesToTheDatabaseGiven` | inside `testApplication`, `installDuelServer(dataSource)` then a `/ws` handshake for device `"probe"`; `SELECT COUNT(*) FROM player` on that same `DataSource` is `1` |

## Acceptance criteria

- [ ] `E2eServerTest.theSameSeedGivesTheSameHandSeeds` passes
- [ ] `E2eServerTest.theInstalledServerWritesToTheDatabaseGiven` passes
- [ ] `E2eServer.kt` names `duelServer` and does not name `SocketDependencies`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
