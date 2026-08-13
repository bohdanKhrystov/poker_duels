package duels.poker.server.db

import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.coinDeltas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Records a finished duel as one duel row and two duel_result rows, inside a single transaction.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database. A duel row without its result rows is an
 * inconsistency this shape exists to prevent — the whole of [record] is one transaction.
 */
public class PostgresDuelResultStore(private val dataSource: DataSource) {
    /**
     * Record a finished duel as one duel row and one duel_result row per seat.
     *
     * All rows are written in a single transaction: either all rows are written or none are,
     * and a duel row without its result rows is impossible. The two player rows are locked in
     * a fixed global order (lower player id first) to prevent deadlock when two duels share
     * a player.
     *
     * @param duel the finished duel to record
     * @throws SQLException if the write fails; the transaction is rolled back and the exception
     *         is rethrown
     */
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

    private fun insertDuel(connection: java.sql.Connection, duel: FinishedDuel) {
        connection.prepareStatement(
            """
            INSERT INTO duel (id, format, started_at, finished_at) VALUES (?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, duel.id)
            statement.setString(2, duel.format)
            statement.setObject(3, duel.startedAt.atOffset(ZoneOffset.UTC))
            statement.setObject(4, duel.finishedAt.atOffset(ZoneOffset.UTC))
            statement.executeUpdate()
        }
    }

    private fun insertResult(
        connection: java.sql.Connection,
        duelId: UUID,
        player: duels.poker.server.session.PlayerId,
        delta: Int,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, duelId)
            statement.setObject(2, UUID.fromString(player.value))
            statement.setInt(3, delta)
            statement.executeUpdate()
        }
    }
}
