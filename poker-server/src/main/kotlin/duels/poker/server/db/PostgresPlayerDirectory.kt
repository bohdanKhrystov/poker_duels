package duels.poker.server.db

import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

/**
 * Resolves a device id to a durable player profile against PostgreSQL.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database. The one-profile-per-device rule lives in
 * `device_binding`'s partial unique index on a live `device_id` (ADR-0049 §1), so a concurrent
 * race between two first contacts is resolved by the database, not by application-level locking.
 */
public class PostgresPlayerDirectory(private val dataSource: DataSource) : PlayerDirectory {
    /**
     * Resolve a device id to a player profile, creating it if it does not exist.
     *
     * A device with a live binding is answered by a plain read. On a miss, this mints a profile
     * and its binding inside an explicit transaction, because `ON CONFLICT ... DO NOTHING` on the
     * binding insert is a *success*: under autocommit a losing racer's `INSERT INTO player` would
     * commit while its binding insert silently does nothing, leaving an orphan profile with no
     * device on every contended first contact (ADR-0049 §4). Losing the race instead rolls back
     * the whole attempt and re-reads for the winner's binding.
     *
     * @param deviceId The device identifier to resolve.
     * @return The player profile for this device.
     */
    override suspend fun resolve(deviceId: DeviceId): Player = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection -> resolveOverConnection(connection, deviceId) }
    }

    /**
     * Find the player profile bound to a device id, without creating one.
     *
     * A plain `SELECT`: no row is written, so an HTTP route resolving identity through this
     * method cannot be used to mint a profile.
     *
     * @param deviceId The device identifier to look up.
     * @return The player profile for this device, or `null` if none exists.
     */
    override suspend fun findOrNull(deviceId: DeviceId): Player? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            liveBindingOf(connection, deviceId)?.let { Player(it, deviceId) }
        }
    }

    private fun resolveOverConnection(connection: Connection, deviceId: DeviceId): Player {
        liveBindingOf(connection, deviceId)?.let { return Player(it, deviceId) }
        mintAndBind(connection, deviceId)?.let { return Player(it, deviceId) }
        return rereadAfterLosingTheRace(connection, deviceId)
    }

    // ADR-0049 §4: the mint and its binding are one transaction, committed only when the binding
    // insert actually won the race. A conflicted binding insert returning no row is a refusal to
    // this caller, not a database error, so it is handled with a plain rollback rather than a
    // caught exception -- the shape PostgresProfileWrites.writeName already uses for its own
    // conflict branch.
    private fun mintAndBind(connection: Connection, deviceId: DeviceId): PlayerId? {
        connection.autoCommit = false
        return try {
            val minted = mintPlayerAndBind(connection, deviceId)
            if (minted != null) {
                connection.commit()
            } else {
                connection.rollback()
            }
            minted
        } catch (failure: SQLException) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    // Bounded, not `while (true)` and not recursion: a conflict is only observed after the
    // concurrent inserter has committed or aborted, so the very next read already finds the
    // winner. A ceiling this small is not a timeout -- more than a handful of consecutive empty
    // reads means something other than an ordinary two-way race is happening.
    private fun rereadAfterLosingTheRace(connection: Connection, deviceId: DeviceId): Player {
        repeat(MAX_LOST_RACE_REREADS) {
            liveBindingOf(connection, deviceId)?.let { return Player(it, deviceId) }
        }
        error("no live device_binding for $deviceId after $MAX_LOST_RACE_REREADS re-reads of a lost mint race")
    }

    private fun liveBindingOf(connection: Connection, deviceId: DeviceId): PlayerId? =
        connection.prepareStatement(
            "SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL",
        ).use { statement ->
            statement.setString(1, deviceId.value)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) PlayerId(resultSet.getString(1)) else null
            }
        }

    private fun mintPlayerAndBind(connection: Connection, deviceId: DeviceId): PlayerId? =
        connection.prepareStatement(
            """
            WITH minted AS (INSERT INTO player (id) VALUES (?) RETURNING id)
            INSERT INTO device_binding (device_id, player_id)
            SELECT ?, id FROM minted
            ON CONFLICT (device_id) WHERE revoked_at IS NULL DO NOTHING
            RETURNING player_id
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setString(2, deviceId.value)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) PlayerId(resultSet.getString(1)) else null
            }
        }

    private companion object {
        private const val MAX_LOST_RACE_REREADS = 5
    }
}
