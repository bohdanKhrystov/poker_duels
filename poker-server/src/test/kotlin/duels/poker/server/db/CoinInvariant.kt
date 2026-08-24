package duels.poker.server.db

import java.util.UUID
import javax.sql.DataSource

/**
 * Asserts both coin properties `ADR-0030` §5 defines against the live schema, in one call, and
 * fails naming [step] so a scenario calling this after every identity operation says which step
 * broke it.
 *
 * Both statements are issued on every call — neither is behind a parameter, a flag or a branch —
 * because P1 does not subsume P2, and P2 does not subsume P1:
 * [CoinInvariantTest.thePerPlayerCheckCatchesACoinMovedBetweenBalances] constructs a coin moved
 * between two balances, where both global sums stay at zero (P2 holds) while one balance no
 * longer equals its own deltas (P1 fails) — the shape a P2-only check never sees.
 * [CoinInvariantTest.theGlobalSumCatchesWhatThePerPlayerCheckCannot] constructs the opposite: a
 * player's balance and their now-absent deltas agree at zero (P1 holds) while the two global sums
 * both drift to the value the missing row should have cancelled (P2 fails) — the shape a P1-only
 * check never sees. A helper running only one property looks equivalent to this one on every
 * other fixture in this file, and silently stops detecting the one shape it does not run.
 *
 * - **P1, per player.** `player.coin_balance` equals the sum of that player's `duel_result`
 *   deltas, for every row of `player`.
 * - **P2, globally.** `SUM(player.coin_balance)` and `SUM(duel_result.coin_delta)` are both zero
 *   across the whole table.
 */
internal fun DataSource.assertCoinInvariantHolds(step: String) {
    val brokenPlayers = p1BrokenBalancePlayerIds()
    val (playerBalanceSum, duelResultDeltaSum) = coinInvariantP2Sums()

    check(brokenPlayers.isEmpty()) {
        "[$step] P1 violated (ADR-0030 §5, per player): coin_balance disagrees with the sum of " +
            "duel_result deltas for player(s) $brokenPlayers"
    }
    check(playerBalanceSum == 0 && duelResultDeltaSum == 0) {
        "[$step] P2 violated (ADR-0030 §5, global): SUM(player.coin_balance) = " +
            "$playerBalanceSum, SUM(duel_result.coin_delta) = $duelResultDeltaSum — both must be 0"
    }
}

/**
 * Every column of every `player` row, ordered by `id` for a deterministic comparison. Column
 * count comes from [java.sql.ResultSetMetaData], never a hard-coded column list, so this keeps
 * comparing correctly across a migration that adds or drops one — `SignUpDatabaseTest.snapshot`
 * is the shape this generalises, so every database suite that touches `player` can reach it, not
 * only sign-up's.
 */
internal fun DataSource.playerTableSnapshot(): List<List<Any?>> {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM player ORDER BY id").use { rs ->
                val columnCount = rs.metaData.columnCount
                val rows = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    rows.add((1..columnCount).map { rs.getObject(it) })
                }
                return rows
            }
        }
    }
}

/**
 * P1 of `ADR-0030` §5, run with the exact SQL the ADR gives. Every id this returns names a player
 * whose `coin_balance` no longer equals the sum of their `duel_result` deltas; P1 holds when this
 * returns no ids. [assertCoinInvariantHolds] uses this, and [CoinInvariantTest] also calls it
 * directly to check what a mutation actually did before asking the combined helper to notice it.
 */
internal fun DataSource.p1BrokenBalancePlayerIds(): List<UUID> {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT p.id FROM player p
                LEFT JOIN duel_result r ON r.player_id = p.id
                GROUP BY p.id, p.coin_balance
                HAVING p.coin_balance <> COALESCE(SUM(r.coin_delta), 0)
                """.trimIndent(),
            ).use { rs ->
                val ids = mutableListOf<UUID>()
                while (rs.next()) ids.add(rs.getObject(1) as UUID)
                return ids
            }
        }
    }
}

/**
 * P2 of `ADR-0030` §5, run with the exact SQL the ADR gives: `SUM(player.coin_balance)` paired
 * with `SUM(duel_result.coin_delta)`. P2 holds when both are zero. [assertCoinInvariantHolds]
 * uses this, and [CoinInvariantTest] also calls it directly for the same reason as
 * [p1BrokenBalancePlayerIds].
 *
 * Named `coinInvariantP2Sums` rather than the shorter, more obvious `p2LedgerSums`: that name is
 * already a file-private extension on `DataSource` in `SignUpDatabaseTest.kt`, and Kotlin does not
 * scope a file-private top-level declaration away from an `internal` one in the same module — the
 * two collide as an unresolvable overload ambiguity at every call site in that file, which this
 * ticket's `SignUpDatabaseTest.kt` is not allowed to touch. `TASK-040617` retires the private copy.
 */
internal fun DataSource.coinInvariantP2Sums(): Pair<Int, Int> {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT (SELECT COALESCE(SUM(coin_balance), 0) FROM player),
                       (SELECT COALESCE(SUM(coin_delta), 0) FROM duel_result)
                """.trimIndent(),
            ).use { rs ->
                rs.next()
                return rs.getInt(1) to rs.getInt(2)
            }
        }
    }
}
