package duels.poker.server.db

import duels.poker.server.auth.DeviceBindings
import duels.poker.server.auth.SessionToken
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

/**
 * Implements [DeviceBindings] against the `device_binding` and `auth_session` tables.
 *
 * [revoke] runs one `UPDATE` and one `DELETE` on one connection, inside one transaction:
 * `autoCommit` is turned off before either statement runs, both run on that same connection, and
 * only a successful `commit()` makes either visible. A failure between the two rolls both back —
 * never a revoked binding with the swept sessions still live, and never the sessions swept with
 * the binding still live (`ADR-0050` §1).
 *
 * The `DELETE` runs whether or not the `UPDATE` touched a row: a player who never bound a device
 * still has every session but the one they presented swept. Branching on the `UPDATE`'s row count
 * would leave that player's other sessions running.
 *
 * `revoked_at` is stamped by the database's own `now()`. This class holds no clock — comparing
 * that column to one is no part of writing it (`ADR-0049` §1).
 *
 * This class writes nothing to `player`, `credential`, `duel`, or `duel_result` — only
 * `device_binding` and `auth_session` are touched.
 */
public class PostgresDeviceBindings(
    private val dataSource: DataSource,
) : DeviceBindings {
    override suspend fun revoke(playerId: PlayerId, keeping: SessionToken): Unit =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    revokeLiveBinding(connection, playerId)
                    deleteOtherSessions(connection, playerId, keeping)
                    connection.commit()
                } catch (e: SQLException) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }
        }

    private fun revokeLiveBinding(connection: Connection, playerId: PlayerId) {
        connection.prepareStatement(REVOKE_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeUpdate()
        }
    }

    private fun deleteOtherSessions(connection: Connection, playerId: PlayerId, keeping: SessionToken) {
        connection.prepareStatement(DELETE_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.setBytes(2, sessionTokenDigest(keeping))
            statement.executeUpdate()
        }
    }

    private companion object {
        private const val REVOKE_SQL =
            "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL"

        private const val DELETE_SQL =
            "DELETE FROM auth_session WHERE player_id = ? AND token_hash <> ?"
    }
}
