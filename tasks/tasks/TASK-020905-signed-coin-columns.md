---
schema: 2
id: TASK-020905
title: Prove a negative coin balance and a negative delta round-trip through PostgreSQL
type: task
status: backlog
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 1
labels: [server, persistence, coins, schema]
depends_on: [TASK-020904]
verify:
  - ./gradlew :poker-server:test --tests '*CoinBalanceIsSignedTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A test stores `−1` in `player.coin_balance` and in `duel_result.coin_delta` and reads `−1` back
out of PostgreSQL, so `ADR-0014`'s signed balance is pinned at the schema level and not only in
Kotlin.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/CoinBalanceIsSignedTest.kt` | create |

Read, do not modify: `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql`,
`poker-server/src/main/kotlin/duels/poker/server/db/Migrations.kt`.

## Scope

- One test class, `CoinBalanceIsSignedTest`, package `duels.poker.server.db`, JUnit 5, plain JDBC.
- `@BeforeEach` builds the fixture: `dataSource = PostgresTestSupport.freshDatabase()` then
  `Migrations.migrate(dataSource)`. `freshDatabase()` gates on Docker, so no extra guard is
  needed.
- Two private helpers in the class, each returning the generated `UUID`:
  `insertPlayer(deviceId: String, coinBalance: Int)` →
  `INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)`, and `insertDuel()` →
  `INSERT INTO duel (id, format, started_at, finished_at) VALUES (?, 'FREEZEOUT', now(), now())`.
  Ids come from `java.util.UUID.randomUUID()`, bound with `setObject`.
- Read values back with a fresh `SELECT`, never from the value that was written — the point of the
  ticket is the database round-trip, and an assertion against the Kotlin variable would prove
  nothing.
- Every statement is a `PreparedStatement` used in `use { }`; no string concatenation of values.

## Out of scope

- Uniqueness and foreign keys — `TASK-020906`.
- Computing a balance from deltas, awarding coins, or any `DuelOutcome` mapping — `STORY-0210`.
  This ticket writes literal numbers with SQL and reads them back.
- A repository type. There is none yet, deliberately.
- Editing `V1__initial_schema.sql`. If a column is missing or misnamed, that is a finding to
  report, not a migration to edit — merged migrations are immutable (`TASK-020904`).

## Tests

`CoinBalanceIsSignedTest`

| Test | Proves |
| --- | --- |
| `aBalanceOfMinusOneRoundTripsThroughTheDatabase` | insert a player with `coin_balance = -1`, select it back, `assertEquals(-1, …)` — `ADR-0014`'s "a new profile whose only duel was a loss reads back −1", at the schema level |
| `aCoinDeltaOfMinusOneRoundTripsThroughTheDatabase` | insert a duel and a player, then a `duel_result` with `coin_delta = -1`; select it back and get `-1` |
| `theBalanceIsNotFlooredAtZero` | `UPDATE player SET coin_balance = -10` succeeds and selects back `-10`; no constraint and no default clamps it |

## Acceptance criteria

- [ ] `CoinBalanceIsSignedTest.aBalanceOfMinusOneRoundTripsThroughTheDatabase` passes
- [ ] `CoinBalanceIsSignedTest.aCoinDeltaOfMinusOneRoundTripsThroughTheDatabase` passes
- [ ] `CoinBalanceIsSignedTest.theBalanceIsNotFlooredAtZero` passes
- [ ] Each assertion reads its value from a `SELECT` issued after the write
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
