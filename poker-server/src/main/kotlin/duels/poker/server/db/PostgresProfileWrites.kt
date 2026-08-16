package duels.poker.server.db

import duels.poker.server.http.ProfileWrites
import duels.poker.server.http.SetNameResult
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

/**
 * Writes a player's display name to PostgreSQL.
 *
 * The write is the single `UPDATE` `ADR-0029` §5 specifies: the unique index and the permanence
 * trigger are the reservation, so this class runs no `SELECT` before it and takes no lock of its
 * own — one statement is the whole race protocol. `SQLSTATE` is read from what PostgreSQL returns,
 * never from an exception message, and translated here into one of [SetNameResult]'s three
 * answers; no `SQLException` escapes `duels.poker.server.db` for a refusal (`ADR-0011`).
 */
public class PostgresProfileWrites(private val dataSource: DataSource) : ProfileWrites {
    override suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult =
        withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection -> writeName(connection, playerId, canonicalName) }
            } catch (failure: SQLException) {
                // 23505 is unique_violation on player_display_name_unique: another player already
                // holds this name under the case-insensitive fold. 23001 (the permanence trigger)
                // cannot fire here — the WHERE clause only ever selects a row whose OLD.display_name
                // is NULL, which is exactly the condition the trigger lets through.
                if (failure.sqlState == UNIQUE_VIOLATION_SQLSTATE) SetNameResult.NameTaken else throw failure
            }
        }

    private fun writeName(connection: Connection, playerId: PlayerId, canonicalName: String): SetNameResult {
        // RETURNING makes the affected row part of the same statement, so the NameSet case needs
        // no second round trip to build the profile it hands back.
        val setProfile = connection.prepareStatement(SET_NAME_SQL).use { statement ->
            statement.setString(1, canonicalName)
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows -> if (rows.next()) rows.toProfile() else null }
        }
        if (setProfile != null) return SetNameResult.NameSet(setProfile)

        // Zero rows: this player already has a name. Which of the two remaining answers it is
        // depends on what that name is, so — and only so — we read it back.
        val currentProfile = readProfile(connection, playerId)
        return if (currentProfile.displayName == canonicalName) {
            // The identical canonical name again: the idempotent retry, not a rename attempt.
            SetNameResult.NameSet(currentProfile)
        } else {
            SetNameResult.AlreadyNamed
        }
    }

    private fun readProfile(connection: Connection, playerId: PlayerId): ProfileResponse =
        connection.prepareStatement(CURRENT_PROFILE_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                check(rows.next()) { "no player row for $playerId" }
                rows.toProfile()
            }
        }

    private fun ResultSet.toProfile(): ProfileResponse =
        ProfileResponse(getString("id"), getInt("coin_balance"), getString("display_name"))

    private companion object {
        private const val UNIQUE_VIOLATION_SQLSTATE = "23505"

        private const val SET_NAME_SQL =
            "UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL " +
                "RETURNING id, coin_balance, display_name"

        private const val CURRENT_PROFILE_SQL =
            "SELECT id, coin_balance, display_name FROM player WHERE id = ?"
    }
}
