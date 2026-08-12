---
schema: 2
id: TASK-021003
title: Resolve a device id to a durable profile, creating it at most once
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, persistence, profiles]
depends_on: [TASK-021002]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresPlayerDirectoryTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
  - grep -q PostgresPlayerDirectory poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt
---

## Goal

`PostgresPlayerDirectory` implements the `PlayerDirectory` port against PostgreSQL: a device id
seen for the first time creates exactly one `player` row, and the same device id returns that same
row's id forever.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` (the port and its
contract),
`poker-server/src/test/kotlin/duels/poker/server/db/CoinBalanceIsSignedTest.kt` (the house JDBC and
`PostgresTestSupport` idiom to copy).

## Scope

- Package `duels.poker.server.db`, one public class:

  ```kotlin
  public class PostgresPlayerDirectory(private val dataSource: DataSource) : PlayerDirectory {
      override suspend fun resolve(deviceId: DeviceId): Player = withContext(Dispatchers.IO) { … }
  }
  ```

- **One statement does both the create and the lookup:**

  ```sql
  INSERT INTO player (id, device_id) VALUES (?, ?)
  ON CONFLICT (device_id) DO UPDATE SET device_id = EXCLUDED.device_id
  RETURNING id
  ```

  The id is a fresh `UUID.randomUUID()` per call, and the candidate row is discarded when the
  device is already known. The no-op `DO UPDATE` is deliberate and worth a `why` comment:
  `DO NOTHING` returns no row when it loses the race, which would force a second query; this form
  returns the surviving row whether it was just inserted or already there. `ADR-0012` puts the
  one-profile-per-device rule in the schema's `UNIQUE (device_id)`, not in application code, so
  there is no read-then-write and no lock taken here.
- The returned `Player` is `Player(PlayerId(resultSet.getString(1)), deviceId)` — the `PlayerId`
  value is the text form of the row's UUID, which is the invariant `TASK-021005` parses back with
  `UUID.fromString`. Say so in the KDoc.
- Blocking JDBC runs inside `withContext(Dispatchers.IO)`: the port is `suspend` precisely so a
  connection's coroutine is not blocked (see `PlayerDirectory`'s KDoc).
- `coin_balance` is not named in the insert — the column defaults to `0` (`ADR-0014`), and a new
  profile starting anywhere else would be a second opinion about the balance.
- KDoc on the class citing `ADR-0011` (this is the repository boundary: nothing outside
  `duels.poker.server.db` sees SQL) and `ADR-0012` (a profile is bound to a device id and outlives
  the connection).
- In `DuelSocket.kt`, **the KDoc paragraph on `duelSocket` only.** It currently claims no shipping
  `PlayerDirectory` exists until `STORY-0210`, which stops being true when this ticket merges.
  Replace that sentence with: a shipping `duels.poker.server.db.PostgresPlayerDirectory` now
  exists, but installing the route from `Application.module()` also means handing `module()` a
  `DataSource`, which `STORY-0212` owns — so the only caller is still a test. No executable line in
  that file changes.

## Out of scope

- Installing `duelSocket` from `Application.module()`, and giving `module()` a `DataSource` —
  `STORY-0212`. It would also change `PokerServerModuleTest`, `HealthRouteTest` and
  `ServerPluginsTest`, which is three more files than this ticket has.
- The concurrency proof — `TASK-021004` adds it to this ticket's test class.
- Reading a balance back for display, or any `SELECT` beyond what `resolve` needs — `STORY-0211`.
- Writing duel results — `TASK-021005`.
- Deleting `InMemoryPlayerDirectory`: it stays, it is the test double for socket tests that want no
  database.

## Tests

`PostgresPlayerDirectoryTest`, JUnit 5, package `duels.poker.server.db`. `@BeforeEach` does
`dataSource = PostgresTestSupport.freshDatabase()` then `Migrations.migrate(dataSource)`, exactly
as `CoinBalanceIsSignedTest` does. `resolve` suspends, so each test body runs inside
`runBlocking`. Private helpers read the database directly with JDBC — `playerRowCount()` and
`coinBalanceOf(uuid)`.

| Test | Proves |
| --- | --- |
| `resolvingANewDeviceCreatesExactlyOneRowWithThatId` | after one `resolve(DeviceId("d1"))`, `playerRowCount() == 1`, the row's `device_id` is `d1`, and the row's `id` equals `UUID.fromString(player.id.value)` |
| `resolvingTheSameDeviceTwiceReturnsTheSamePlayerId` | two `resolve(DeviceId("d1"))` calls return the same `PlayerId` and `playerRowCount()` is still `1` |
| `differentDevicesGetDifferentProfiles` | `d1` and `d2` return different `PlayerId`s and `playerRowCount() == 2` |
| `aNewProfileStartsAtAZeroBalance` | the `coin_balance` of a freshly resolved profile is `0` |

## Acceptance criteria

- [ ] `PostgresPlayerDirectoryTest.resolvingANewDeviceCreatesExactlyOneRowWithThatId` passes
- [ ] `PostgresPlayerDirectoryTest.resolvingTheSameDeviceTwiceReturnsTheSamePlayerId` passes
- [ ] `PostgresPlayerDirectoryTest.differentDevicesGetDifferentProfiles` passes
- [ ] `PostgresPlayerDirectoryTest.aNewProfileStartsAtAZeroBalance` passes
- [ ] `PostgresPlayerDirectory.kt` contains exactly one SQL string, and that string contains
      `ON CONFLICT (device_id)`
- [ ] `PostgresPlayerDirectory.kt` contains no `SELECT` issued before the insert — there is no
      read-then-write path
- [ ] `DuelSocket.kt`'s diff is KDoc only: the file's non-comment lines are unchanged, and the new
      text names `PostgresPlayerDirectory` and `STORY-0212`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
