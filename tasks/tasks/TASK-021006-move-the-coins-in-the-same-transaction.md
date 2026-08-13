---
schema: 2
id: TASK-021006
title: Move both coin balances inside the same transaction, by SQL increment
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [server, persistence, coins]
depends_on: [TASK-021005]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Recording a finished duel also moves both coin balances — winner `+1`, loser `−1` — in the same
transaction that writes the rows, so a stored result and the balance it produced can never disagree.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

Read, do not modify:
`docs/adr/ADR-0014-duel-coin-economy.md`,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql` (`coin_balance` is a signed
`INTEGER` with no `CHECK`).

## Scope

- Add one private helper to the store and call it from the existing `moves` loop, immediately after
  that seat's result row:

  ```kotlin
  private fun addToBalance(connection: Connection, player: PlayerId, delta: Int) {
      connection.prepareStatement("UPDATE player SET coin_balance = coin_balance + ? WHERE id = ?")
          .use { statement ->
              statement.setInt(1, delta)
              statement.setObject(2, UUID.fromString(player.value))
              statement.executeUpdate()
          }
  }
  ```

- **The increment happens in SQL.** No `SELECT coin_balance` followed by an `UPDATE`, no new
  balance computed in Kotlin: two duels finishing at once for one player must both land, and a
  read-modify-write loses one of them. `TASK-021011` is the test that would catch it.
- The call goes inside the loop over `moves`, which is already sorted by player id — the lock order
  that keeps two concurrent duels sharing a player from deadlocking. Do not reorder that loop and
  do not add a second loop.
- Nothing clamps: no `coerceAtLeast`, no `maxOf(0, …)`, no `abs`. A balance is `wins − losses` and
  a new profile's first loss puts it at `−1` (`ADR-0014`). Extend the class KDoc to say the two
  result rows and the two balance moves commit together or not at all.

## Out of scope

- Idempotency on the duel id — `TASK-021009`.
- Proving the rollback — `TASK-021007`.
- The draw case and the negative-balance cases — `TASK-021008`, `TASK-021010`.
- Returning the new balances from `record`. It stays `Unit`; `STORY-0211` owns the read path.

## Tests

`PostgresDuelResultStoreTest`, the existing class, two tests added. They use the class's existing
`finishedDuel(...)` builder and a new private helper `coinBalanceOf(playerId): Int` that reads
`player.coin_balance` straight from the database.

| Test | Proves |
| --- | --- |
| `theWinnersBalanceRisesByExactlyOne` | after `record(finishedDuel(winner = 0))`, `coinBalanceOf(alice.id) == 1` |
| `theLosersBalanceFallsByExactlyOne` | after the same record, `coinBalanceOf(bob.id) == -1` — stored, read back, unclamped |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.theWinnersBalanceRisesByExactlyOne` passes
- [ ] `PostgresDuelResultStoreTest.theLosersBalanceFallsByExactlyOne` passes
- [ ] The two tests already in `PostgresDuelResultStoreTest` pass unchanged — they assert row
      counts and deltas only, so this ticket adds assertions rather than moving any
- [ ] `PostgresDuelResultStore.kt` contains no `SELECT coin_balance`, and its only balance
      statement is an `UPDATE … SET coin_balance = coin_balance + ?`
- [ ] `PostgresDuelResultStore.kt` contains no `coerceAtLeast`, `coerceIn`, `maxOf`, `abs` or
      `absoluteValue`
- [ ] `commit()` is still called exactly once, after both balance updates
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
