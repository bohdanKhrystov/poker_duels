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
 * `ADR-0051` §2 spends `ADR-0029` §5's *"the write is one statement"*: setting a name is now two
 * statements — a `name_registry` insert that spends the string, then the `player` `UPDATE` that
 * hands it over — run in one transaction, and the rollback on every outcome but the happy one is
 * load-bearing rather than tidy. Left out, a registry row survives a refused claim and permanently
 * burns a string nobody holds (`ADR-0051` §2). `SQLSTATE` is read from what PostgreSQL returns,
 * never from an exception message, and translated here into one of [SetNameResult]'s three
 * answers; no `SQLException` escapes `duels.poker.server.db` for a refusal (`ADR-0011`).
 */
public class PostgresProfileWrites(private val dataSource: DataSource) : ProfileWrites {
    override suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection -> writeName(connection, playerId, canonicalName) }
        }

    // ADR-0051 §2: the registry insert always runs first, so a name already spent — by this
    // player or by anyone else — is refused there before the player row is ever touched. Once
    // PostgreSQL answers 23505 the transaction is aborted and any further statement on it fails
    // with 25P02, so the rollback below must happen before the idempotent-retry read runs.
    private fun writeName(connection: Connection, playerId: PlayerId, canonicalName: String): SetNameResult {
        connection.autoCommit = false
        return try {
            insertRegistryRow(connection, canonicalName)
            claimNameForPlayer(connection, playerId, canonicalName)
        } catch (failure: SQLException) {
            connection.rollback()
            if (failure.sqlState != UNIQUE_VIOLATION_SQLSTATE) throw failure
            resultAfterRegistryConflict(connection, playerId, canonicalName)
        } finally {
            connection.autoCommit = true
        }
    }

    private fun insertRegistryRow(connection: Connection, canonicalName: String) {
        connection.prepareStatement(INSERT_NAME_REGISTRY_SQL).use { statement ->
            statement.setString(1, canonicalName)
            statement.executeUpdate()
        }
    }

    // RETURNING makes the affected row part of the same statement, so the happy path needs no
    // second round trip to build the profile it hands back. player_display_name_unique is a
    // second line of defence here (ADR-0051 §2) — it can only fire if the registry and the
    // column have disagreed, which is unreachable once every writer goes through this class, but
    // a raw fixture that lands a name without a registry row can still reach it in tests.
    private fun claimNameForPlayer(connection: Connection, playerId: PlayerId, canonicalName: String): SetNameResult {
        val setProfile = connection.prepareStatement(SET_NAME_SQL).use { statement ->
            statement.setString(1, canonicalName)
            statement.setObject(2, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows -> if (rows.next()) rows.toProfile() else null }
        }
        if (setProfile != null) {
            connection.commit()
            return SetNameResult.NameSet(setProfile)
        }
        // Zero rows: this player already holds a different name. The string statement 1 just
        // spent belongs to nobody now, so it rolls back with everything else — the loser of a
        // claim costs nothing (ADR-0051 §2).
        connection.rollback()
        return SetNameResult.AlreadyNamed
    }

    // The transaction is already rolled back here, on a clean one. Which of the two remaining
    // answers this is depends on whether this player already holds the exact string they just
    // tried to spend again — the idempotent retry — or someone else does.
    private fun resultAfterRegistryConflict(
        connection: Connection,
        playerId: PlayerId,
        canonicalName: String,
    ): SetNameResult {
        val currentProfile = readProfile(connection, playerId)
        return if (currentProfile.displayName == canonicalName) {
            SetNameResult.NameSet(currentProfile)
        } else {
            SetNameResult.NameTaken
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

    // SetNameResult.NameSet describes a player who now holds a name — including ADR-0051 §2's
    // idempotent retry — so displayNameRemoved is false by construction on every 200 from
    // PUT /api/me/name. Never a second query, never a subquery in RETURNING (ADR-0053 §6). That
    // prohibition is scoped to displayNameRemoved and does not transfer to deviceRouteLive: a
    // player renaming themselves may or may not still have a live binding, so a literal here
    // would be a lie. Both statements below carry the same correlated EXISTS PostgresProfileReads
    // uses — correlated to player.id, the row each statement already returns — so this stays one
    // round trip with no conditional read.
    private fun ResultSet.toProfile(): ProfileResponse =
        ProfileResponse(
            getString("id"),
            getInt("coin_balance"),
            getString("display_name"),
            false,
            getBoolean("device_route_live"),
        )

    private companion object {
        private const val UNIQUE_VIOLATION_SQLSTATE = "23505"

        private const val INSERT_NAME_REGISTRY_SQL =
            "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')"

        private const val DEVICE_ROUTE_LIVE_EXISTS =
            "EXISTS (SELECT 1 FROM device_binding b WHERE b.player_id = player.id AND b.revoked_at IS NULL)"

        private const val SET_NAME_SQL =
            "UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL " +
                "RETURNING id, coin_balance, display_name, $DEVICE_ROUTE_LIVE_EXISTS AS device_route_live"

        private const val CURRENT_PROFILE_SQL =
            "SELECT id, coin_balance, display_name, $DEVICE_ROUTE_LIVE_EXISTS AS device_route_live " +
                "FROM player WHERE id = ?"
    }
}
