package duels.poker.server.db

import duels.poker.server.http.ProfileReads
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

/**
 * Reads player profiles and coin balances from PostgreSQL.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database or SQL. No `ResultSet`, `Connection` or other JDBC
 * type escapes through the [ProfileReads] port.
 */
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
