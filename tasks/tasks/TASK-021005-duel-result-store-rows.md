---
schema: 2
id: TASK-021005
title: Record a finished duel as one duel row and two result rows, in one transaction
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, persistence, duel]
depends_on: [TASK-021004]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresDuelResultStore.record(duel)` writes the `duel` row and one `duel_result` row per seat,
each carrying that seat's signed coin delta, inside a single transaction.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt`,
`poker-server/src/main/kotlin/duels/poker/server/duel/CoinDeltas.kt`,
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt` (the JDBC and
`withContext` idiom, and the `PlayerId`-is-a-UUID-string invariant).

## Scope

- Package `duels.poker.server.db`, one public class:

  ```kotlin
  public class PostgresDuelResultStore(private val dataSource: DataSource) {
      public suspend fun record(duel: FinishedDuel): Unit = withContext(Dispatchers.IO) {
          val deltas = coinDeltas(duel.outcome)
          // A fixed global order over the two player rows: every transaction touches the lower
          // player id first, so two duels sharing a player can never deadlock by locking the
          // same two rows in opposite orders.
          val moves = (0..1)
              .map { seat -> duel.seats[seat] to deltas.forSeat(seat) }
              .sortedBy { (player, _) -> player.value }
          dataSource.connection.use { connection ->
              connection.autoCommit = false
              try {
                  insertDuel(connection, duel)
                  moves.forEach { (player, delta) -> insertResult(connection, duel.id, player, delta) }
                  connection.commit()
              } catch (failure: SQLException) {
                  connection.rollback()
                  throw failure
              }
          }
      }
  }
  ```

- `insertDuel`: `INSERT INTO duel (id, format, started_at, finished_at) VALUES (?, ?, ?, ?)`, with
  `setObject(1, duel.id)`, `setString(2, duel.format)` and the two instants bound as
  `duel.startedAt.atOffset(ZoneOffset.UTC)` — `timestamptz` takes an `OffsetDateTime`, and UTC is
  the only offset this server writes.
- `insertResult`: `INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)`, with
  the player bound as `UUID.fromString(player.value)`.
- **The delta comes from `coinDeltas` and nowhere else.** No `if (winner)` in this file, no `+1`
  or `-1` literal, no arithmetic on a balance: `TASK-021001` owns the rule and `ADR-0014` owns the
  numbers.
- `autoCommit = false`, one `commit()` on the happy path, `rollback()` then rethrow on
  `SQLException`. The exception is never swallowed — a caller that is not told the write failed
  will not retry it.
- KDoc on the class and on `record`: `ADR-0011`'s repository boundary (this is the only place that
  knows the duel tables exist), and that the whole of `record` is one transaction — a duel row
  without its result rows is the inconsistency this shape exists to prevent.

## Out of scope

- Moving the coin balances — `TASK-021006` adds the `player` update inside this same transaction
  and this same loop. **Do not assert `coin_balance` anywhere in this ticket's tests**, and do not
  assert it is still zero: `TASK-021006` would then have to rewrite an assertion this ticket
  merged.
- Idempotency on the duel id — `TASK-021009`.
- Rollback, draw and negative-balance assertions — `TASK-021007`, `TASK-021008`, `TASK-021010`.
- A `DuelResultSink` port or any interface over this class: `STORY-0207` declares that port at the
  duel runner and points it here.

## Tests

`PostgresDuelResultStoreTest`, JUnit 5, package `duels.poker.server.db`. `@BeforeEach` takes
`PostgresTestSupport.freshDatabase()`, runs `Migrations.migrate(dataSource)`, builds a
`PostgresPlayerDirectory` and a `PostgresDuelResultStore` over it, and resolves two profiles —
`alice` from `DeviceId("alice")` and `bob` from `DeviceId("bob")` — inside `runBlocking`. Private
helpers: `duelRowCount()`, `duelResultRowCount()`, `resultDeltaOf(duelId, playerId)`, and a
`finishedDuel(winner: Int?, id: UUID = UUID.randomUUID())` builder that fills `format` with
`formatLabel(DuelFormat.DEFAULT)`, fixed `Instant`s and `seats = listOf(alice.id, bob.id)`.

| Test | Proves |
| --- | --- |
| `recordingAFinishedDuelWritesOneDuelRow` | after `record(finishedDuel(winner = 0))`, `duelRowCount() == 1` and that row's `format` is `FREEZEOUT` |
| `recordingAFinishedDuelWritesOneResultRowPerSeat` | `duelResultRowCount() == 2`, `resultDeltaOf(id, alice.id) == 1` and `resultDeltaOf(id, bob.id) == -1` |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.recordingAFinishedDuelWritesOneDuelRow` passes
- [ ] `PostgresDuelResultStoreTest.recordingAFinishedDuelWritesOneResultRowPerSeat` passes
- [ ] `PostgresDuelResultStore.kt` calls `coinDeltas` and contains no other decision about who won:
      no `winner ==`, no `isDraw`, no `1` or `-1` coin literal
- [ ] `PostgresDuelResultStore.kt` sets `autoCommit = false`, calls `commit()` exactly once and
      `rollback()` exactly once, and rethrows the caught `SQLException`
- [ ] `PostgresDuelResultStoreTest.kt` contains no reference to `coin_balance`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
