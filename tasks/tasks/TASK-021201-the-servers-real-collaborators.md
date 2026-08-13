---
schema: 2
id: TASK-021201
title: Build the server's real collaborators from config and a DataSource
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, wiring]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ServerComponentsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

One function turns a `ServerConfig` and a `DataSource` into the real `SocketDependencies` and the
real `ProfileReads` the shipping server runs on — the Postgres directory, the Postgres result sink
behind the room registry, and the operator's configured limits.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/ServerComponentsTest.kt` | create |

Read, do not modify: `session/SocketDependencies.kt`, `room/RoomRegistry.kt` (its constructor
parameters), `config/ServerConfig.kt` (`roomTimeouts()`).

## Scope

- One data class and one function, package `duels.poker.server`:

  ```kotlin
  public data class ServerComponents(val socket: SocketDependencies, val reads: ProfileReads)

  public fun serverComponents(
      config: ServerConfig,
      dataSource: DataSource,
      clock: ServerClock = SystemClock,
      seeds: HandSeedSource = SecureHandSeedSource(),
  ): ServerComponents
  ```

- The registry is built as
  `RoomRegistry(RandomRoomCodeSource(), clock, config.roomTimeouts(), seeds, PostgresDuelResultSink(PostgresDuelResultStore(dataSource)))`.
  The sink behind the registry is the whole point: a duel that finishes writes its row and both
  coins without anything else asking it to.
- `directory = PostgresPlayerDirectory(dataSource)`, `reads = PostgresProfileReads(dataSource)`,
  `deviceIds = RandomDeviceIdSource()`, `sessions = SessionRegistry()`,
  `connections = ConnectionDirectory()`, and both frame limits straight off `config`.
- **`clock` and `seeds` are defaulted test seams, and their KDoc must say so**: production takes
  the defaults, and `STORY-0212`'s end-to-end test injects a fixed `HandSeedSource` so a failing
  duel is reproducible. Same shape as `ServerConfig.from(config, env)`.
- This function opens no pool, runs no migration and installs no route. It is construction only.

## Out of scope

- Installing anything on an `Application` — `TASK-021202`.
- Any periodic sweep of `reap()` or `expireGracePeriods()` — `TASK-021212`, blocked on `DEC-019`.
  Do not add a ticker, a plugin or a background coroutine here.
- Changing `startDatabase`, `module()` or `main()`.

## Tests

`ServerComponentsTest`, JUnit 5, package `duels.poker.server`. `@BeforeEach` calls
`PostgresTestSupport.freshDatabase()` and `Migrations.migrate(...)`; a private helper builds a
`ServerConfig` field by field, as `PersistenceSurvivesRestartTest` already does.

| Test | Proves |
| --- | --- |
| `theDirectoryWritesToTheDatabase` | `socket.directory.resolve(DeviceId("wired"))`, then `SELECT COUNT(*) FROM player` is `1` — the directory behind the socket is backed by the given `DataSource`, not an in-memory stand-in |
| `theReadsSeeWhatTheDirectoryWrote` | after that same `resolve`, `reads.profileOf(DeviceId("wired"))` is non-null, its `playerId` equals the resolved player's id and its `coinBalance` is `0` — both halves share one database |
| `theFrameLimitsComeFromTheConfig` | built from a config with `maxFrameLength = 4321` and `maxFrameNestingDepth = 12`, `socket.maxFrameLength == 4321` and `socket.maxFrameNestingDepth == 12`. Both values are deliberately unlike the defaults, so a hard-coded default fails this |
| `theRoomsDrawFromTheSeedSourceGiven` | built with `HandSeedSource { 4242L }`, `socket.rooms.handSeeds.newHandSeed() == 4242L` |

## Acceptance criteria

- [ ] `ServerComponentsTest.theDirectoryWritesToTheDatabase` passes
- [ ] `ServerComponentsTest.theReadsSeeWhatTheDirectoryWrote` passes
- [ ] `ServerComponentsTest.theFrameLimitsComeFromTheConfig` passes
- [ ] `ServerComponentsTest.theRoomsDrawFromTheSeedSourceGiven` passes
- [ ] `ServerComponents.kt` names no timer, ticker, `launch` or `delay`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
