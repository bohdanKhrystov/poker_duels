package duels.poker.server.db

import duels.poker.server.auth.CredentialKind
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
 * `ADR-0134` §2 spends `ADR-0029` §5's *"the write is one statement"* a second time: setting a
 * name is four statements — lock the profile and read the name it holds, spend the new string,
 * mark the string being left `REPLACED`, then hand the new string over — run in one transaction,
 * so a rename spends before it replaces and no window exists in which the player holds a name
 * whose registry row still reads `TAKEN`. The rollback on every outcome but the happy one is
 * load-bearing rather than tidy: left out, a registry row survives a refused claim and
 * permanently burns a string nobody holds (`ADR-0051` §2), and with a rename in play it would
 * also strand a player holding a string the registry says is spent (`ADR-0134` §2). `SQLSTATE` is
 * read from what PostgreSQL returns, never from an exception message, and translated here into
 * one of [SetNameResult]'s answers; no `SQLException` escapes `duels.poker.server.db` for a
 * refusal (`ADR-0011`).
 */
public class PostgresProfileWrites(private val dataSource: DataSource) : ProfileWrites {
    override suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection -> writeName(connection, playerId, canonicalName) }
        }

    // ADR-0134 §2: the profile is locked before any registry row, the lock order `ADR-0051` §4's
    // takedown function already takes (V5), so the FOR UPDATE below is the only concurrency
    // interlock this write needs — two renames of the same profile serialise here rather than
    // racing statement 3 against a row the other writer already moved off TAKEN. The registry
    // insert still runs first among the writes, so a name already spent — by this player or by
    // anyone else — is refused there before the player row's retirement is ever attempted. Once
    // PostgreSQL answers 23505 the transaction is aborted and any further statement on it fails
    // with 25P02, so the rollback below must happen before the idempotent-retry read runs.
    private fun writeName(connection: Connection, playerId: PlayerId, canonicalName: String): SetNameResult {
        connection.autoCommit = false
        return try {
            val heldName = lockProfileAndReadHeldName(connection, playerId)
            insertRegistryRow(connection, canonicalName)
            if (heldName != null) retireRegistryRow(connection, playerId, heldName)
            claimNameForPlayer(connection, playerId, canonicalName)
        } catch (failure: SQLException) {
            connection.rollback()
            if (failure.sqlState != UNIQUE_VIOLATION_SQLSTATE) throw failure
            resultAfterRegistryConflict(connection, playerId, canonicalName)
        } finally {
            connection.autoCommit = true
        }
    }

    // Statement 1 (ADR-0134 §2). FOR UPDATE takes the lock `ADR-0051` §4's takedown function
    // already takes before it ever spends a string, so a concurrent rename of this same profile
    // blocks here rather than reading a name that is about to be replaced out from under it. No
    // row is unreachable — ADR-0039 deletes no profile and the route resolved identity before the
    // body was read — so a miss here is the same check(rows.next()) shape readProfile uses, a 500
    // and never a fabricated refusal.
    private fun lockProfileAndReadHeldName(connection: Connection, playerId: PlayerId): String? =
        connection.prepareStatement(LOCK_PROFILE_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.executeQuery().use { rows ->
                check(rows.next()) { "no player row for $playerId" }
                rows.getString(1)
            }
        }

    // Statement 2 (ADR-0134 §2), unchanged from ADR-0051 §2: the new string is spent before
    // anything else is touched, so the common refusal — the name is spent — is decided first.
    private fun insertRegistryRow(connection: Connection, canonicalName: String) {
        connection.prepareStatement(INSERT_NAME_REGISTRY_SQL).use { statement ->
            statement.setString(1, canonicalName)
            statement.executeUpdate()
        }
    }

    // Statement 3 (ADR-0134 §2), only when statement 1 found a held name: the string being left
    // moves to REPLACED, with no guard on the reason it was already carrying. An impossible state
    // — a held name whose registry row is not TAKEN — must fail loudly at the monotonicity
    // trigger (23001), never update zero rows in silence and let statement 4's trigger pass on a
    // row that never recorded what a rename actually spent.
    private fun retireRegistryRow(connection: Connection, playerId: PlayerId, heldName: String) {
        connection.prepareStatement(RETIRE_NAME_REGISTRY_SQL).use { statement ->
            statement.setObject(1, UUID.fromString(playerId.value))
            statement.setString(2, heldName)
            statement.executeUpdate()
        }
    }

    // Statement 4 (ADR-0134 §2). RETURNING makes the affected row part of the same statement, so
    // the happy path needs no second round trip to build the profile it hands back. The WHERE
    // clause dropped its guard that a nameless column was the only one this statement could
    // touch: statement 1's FOR UPDATE is the interlock now, so a zero-row result is unreachable
    // and is a check, not a fourth outcome — unlike the forbidden-rename branch this replaces,
    // holding a name is no longer a reason this statement finds nothing.
    // player_display_name_unique is a second line of defence (ADR-0051 §2) — it can only fire if
    // the registry and the column have disagreed, which is unreachable once every writer goes
    // through this class, but a raw fixture that lands a name without a registry row can still
    // reach it in tests, and that SQLSTATE is still 23505 so it rolls back through the same path.
    private fun claimNameForPlayer(
        connection: Connection,
        playerId: PlayerId,
        canonicalName: String,
    ): SetNameResult.NameSet {
        val setProfile = connection.prepareStatement(SET_NAME_SQL).use { statement ->
            statement.setString(1, canonicalName)
            statement.setObject(2, UUID.fromString(playerId.value))
            // The bound kind sits inside RETURNING, which is textually after the WHERE clause,
            // so it becomes `?` 3 here — unlike the select-list EXISTS in PROFILE_OF_SQL and
            // CURRENT_PROFILE_SQL, where the same EXISTS precedes the WHERE and binds first.
            statement.setString(3, CredentialKind.PASSWORD.value)
            statement.executeQuery().use { rows ->
                check(rows.next()) { "no player row for $playerId" }
                rows.toProfile()
            }
        }
        connection.commit()
        return SetNameResult.NameSet(setProfile)
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
            // The bound kind sits inside a select-list EXISTS, which precedes the WHERE clause
            // textually, so it is `?` 1 and the player id — still bound exactly once — moves to
            // `?` 2, exactly as in PROFILE_OF_SQL.
            statement.setString(1, CredentialKind.PASSWORD.value)
            statement.setObject(2, UUID.fromString(playerId.value))
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
    //
    // hasRecoveryEmail also takes a literal, on ADR-0031 §6.3's ticket-scoped reasoning: a name
    // write neither reads nor changes recovery_email, so this response does not attempt the fact
    // rather than adding a third correlated EXISTS to both statements below. Unlike
    // displayNameRemoved's false above, this one is not false-by-construction — a player with a
    // verified address who renames still reads false on this particular response.
    //
    // hasPassword is explicitly not given hasRecoveryEmail's literal treatment (ADR-0132 §2): it
    // is read off the ResultSet from the same correlated EXISTS both statements below now carry,
    // so PUT /api/me/name's 200 cannot turn a claimed player anonymous.
    private fun ResultSet.toProfile(): ProfileResponse =
        ProfileResponse(
            getString("id"),
            getInt("coin_balance"),
            getString("display_name"),
            false,
            getBoolean("device_route_live"),
            false,
            getBoolean("has_password"),
        )

    private companion object {
        private const val UNIQUE_VIOLATION_SQLSTATE = "23505"

        private const val LOCK_PROFILE_SQL =
            "SELECT display_name FROM player WHERE id = ? FOR UPDATE"

        private const val INSERT_NAME_REGISTRY_SQL =
            "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')"

        private const val RETIRE_NAME_REGISTRY_SQL =
            "UPDATE name_registry SET reason = 'REPLACED', retired_from = ? WHERE name = ?"

        private const val DEVICE_ROUTE_LIVE_EXISTS =
            "EXISTS (SELECT 1 FROM device_binding b WHERE b.player_id = player.id AND b.revoked_at IS NULL)"

        // The same EXISTS PostgresProfileReads.PROFILE_OF_SQL carries, correlated to player.id
        // exactly as that one is (ADR-0132 §2). The kind is bound as a statement parameter from
        // CredentialKind.PASSWORD.value below, never spelled as a SQL literal here.
        private const val HAS_PASSWORD_EXISTS =
            "EXISTS (SELECT 1 FROM credential c WHERE c.player_id = player.id AND c.kind = ?)"

        // The bound kind above sits in RETURNING, which is textually after the WHERE clause, so
        // it is `?` 3 here — the id at `?` 2 and the name at `?` 1 keep the positions they had
        // before this field existed.
        private const val SET_NAME_SQL =
            "UPDATE player SET display_name = ? WHERE id = ? " +
                "RETURNING id, coin_balance, display_name, $DEVICE_ROUTE_LIVE_EXISTS AS device_route_live, " +
                "$HAS_PASSWORD_EXISTS AS has_password"

        // Here the bound kind sits in the select list, textually before the WHERE clause, so it
        // is `?` 1 and the player id — still bound exactly once — moves to `?` 2.
        private const val CURRENT_PROFILE_SQL =
            "SELECT id, coin_balance, display_name, $DEVICE_ROUTE_LIVE_EXISTS AS device_route_live, " +
                "$HAS_PASSWORD_EXISTS AS has_password FROM player WHERE id = ?"
    }
}
