package duels.poker.server.db

import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerDirectory
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.sql.DataSource

/**
 * Resolves a device id to a durable player profile against PostgreSQL.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database. The one-profile-per-device rule lives in the
 * schema's `UNIQUE (device_id)` constraint (ADR-0012), so a concurrent race between two
 * first-contacts is resolved by the database, not by application-level locking.
 */
public class PostgresPlayerDirectory(private val dataSource: DataSource) : PlayerDirectory {
    /**
     * Resolve a device id to a player profile, creating it if it does not exist.
     *
     * Uses an INSERT ... ON CONFLICT ... RETURNING statement to atomically create or retrieve
     * the profile. The [PlayerId.value] is the text form of the row's UUID.
     *
     * @param deviceId The device identifier to resolve.
     * @return The player profile for this device.
     */
    override suspend fun resolve(deviceId: DeviceId): Player = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO player (id, device_id) VALUES (?, ?)
                ON CONFLICT (device_id) DO UPDATE SET device_id = EXCLUDED.device_id
                RETURNING id
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, UUID.randomUUID())
                statement.setString(2, deviceId.value)
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "INSERT ... RETURNING should have returned exactly one row" }
                    val playerId = resultSet.getString(1)
                    Player(PlayerId(playerId), deviceId)
                }
            }
        }
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
            connection.prepareStatement(
                "SELECT id FROM player WHERE device_id = ?",
            ).use { statement ->
                statement.setString(1, deviceId.value)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        Player(PlayerId(resultSet.getString(1)), deviceId)
                    } else {
                        null
                    }
                }
            }
        }
    }
}
