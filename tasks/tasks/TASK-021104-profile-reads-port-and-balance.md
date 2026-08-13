---
schema: 2
id: TASK-021104
title: Read a device's profile and balance behind a ProfileReads port
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, http, persistence, profiles, coins]
depends_on: [TASK-021103, TASK-021003]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileReads.profileOf(deviceId)` returns the stored profile and its coin balance for a
known device, `null` for an unknown one, and writes nothing either way.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt` (the JDBC and
`withContext` idiom, and the `PlayerId`-is-a-UUID-string invariant),
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/CoinBalanceIsSignedTest.kt` (the
`PostgresTestSupport` and raw-JDBC helper idiom to copy).

## Scope

- `ProfileReads.kt` in `duels.poker.server.http` — the port, declared at its consumer exactly as
  `PlayerDirectory` is declared in `session` and implemented in `db`:

  ```kotlin
  public interface ProfileReads {
      public suspend fun profileOf(deviceId: DeviceId): ProfileResponse?
  }
  ```

  KDoc: the port exists so the routes can be tested without a database, and so no route ever holds
  a `DataSource` (`ADR-0011`). It returns the response type rather than a parallel domain type
  because the answer's shape *is* the wire's shape; a second identical type would be a copy nobody
  reads. **Nothing on this port creates anything** — an unknown device is `null`, and profile
  creation happens on the socket handshake only (`ADR-0012`), so a crawler cannot mint rows.
- `PostgresProfileReads.kt` in `duels.poker.server.db`:

  ```kotlin
  public class PostgresProfileReads(private val dataSource: DataSource) : ProfileReads {
      override suspend fun profileOf(deviceId: DeviceId): ProfileResponse? = withContext(Dispatchers.IO) {
          dataSource.connection.use { connection ->
              connection.prepareStatement("SELECT id, coin_balance FROM player WHERE device_id = ?")
                  .use { statement ->
                      statement.setString(1, deviceId.value)
                      statement.executeQuery().use { rows ->
                          if (rows.next()) ProfileResponse(rows.getString(1), rows.getInt(2)) else null
                      }
                  }
          }
      }
  }
  ```

- **`SELECT` only, for the whole file and for every ticket that extends it.** No `INSERT`, no
  `UPDATE`, no `DELETE`, no `ON CONFLICT`. That is the story's "an absent or unknown device id is
  refused and creates nothing", enforced by the file containing no statement that could.
- The balance is returned exactly as stored. No `coerceAtLeast`, no `maxOf(0, …)`, no `abs`: `−1`
  is a new player's first loss and is the answer (`ADR-0014`).
- Blocking JDBC runs inside `withContext(Dispatchers.IO)`, like `PostgresPlayerDirectory`.
- KDoc on the class citing `ADR-0011`: this is the repository boundary, and nothing outside
  `duels.poker.server.db` sees SQL.

## Out of scope

- `recentDuelsOf` and the duel join — `TASK-021106` adds it to this port, this class and this test.
- Any route — `TASK-021109`.
- Proving the balance follows recorded duels — `TASK-021105`.
- Installing anything into `Application.module()`: `module()` still takes no `DataSource`, and
  wiring it is `STORY-0212`'s, exactly as it is for `duelSocket`.

## Tests

`PostgresProfileReadsTest`, JUnit 5, package `duels.poker.server.db`. `@BeforeEach` does
`dataSource = PostgresTestSupport.freshDatabase()`, then `Migrations.migrate(dataSource)`, then
builds a `PostgresPlayerDirectory` and a `PostgresProfileReads` over it. Suspending calls run
inside `runBlocking`. One private JDBC helper: `playerRowCount()`.

| Test | Proves |
| --- | --- |
| `aKnownDeviceReadsBackItsProfileAtZero` | after `resolve(DeviceId("alice"))`, `profileOf(DeviceId("alice"))` returns a `ProfileResponse` whose `playerId` equals the resolved `PlayerId.value` and whose `coinBalance` is `0` |
| `anUnknownDeviceReadsBackNull` | `profileOf(DeviceId("ghost"))` is `null` |
| `readingAnUnknownDeviceCreatesNoProfile` | `playerRowCount()` is unchanged across a `profileOf(DeviceId("ghost"))` — the read path mints nothing |
| `twoDevicesReadBackTheirOwnProfiles` | with `alice` and `bob` resolved, each `profileOf` returns that device's own `playerId` and the two differ |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aKnownDeviceReadsBackItsProfileAtZero` passes
- [ ] `PostgresProfileReadsTest.anUnknownDeviceReadsBackNull` passes
- [ ] `PostgresProfileReadsTest.readingAnUnknownDeviceCreatesNoProfile` passes
- [ ] `PostgresProfileReadsTest.twoDevicesReadBackTheirOwnProfiles` passes
- [ ] `PostgresProfileReads.kt` contains no `INSERT`, `UPDATE`, `DELETE` or `ON CONFLICT`
- [ ] `PostgresProfileReads.kt` contains no `coerceAtLeast`, `coerceIn`, `maxOf`, `abs` or
      `absoluteValue`
- [ ] `ProfileReads.kt` names no `DataSource`, `Connection` or SQL string
- [ ] No file outside the three listed above changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
