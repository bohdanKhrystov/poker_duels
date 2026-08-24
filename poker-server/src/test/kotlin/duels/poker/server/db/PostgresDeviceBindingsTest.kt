package duels.poker.server.db

import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [PostgresDeviceBindings], against the container.
 *
 * Every test but [aPlayerWithNoLiveBindingStillLosesTheOtherSessions] gives the revoking player a
 * live binding before calling [PostgresDeviceBindings.revoke] — an `UPDATE` guarded by
 * `if (rowsUpdated > 0)` would still run the `DELETE` in every one of those, and only that one
 * test would catch it.
 *
 * [aFailureBetweenTheStatementsRollsBackTheUpdate] is the one test in this class that tells one
 * transaction apart from two. Nothing reachable through [PostgresDeviceBindings.revoke] can make
 * the `DELETE` fail on its own: the finality trigger only guards `device_binding`, and the
 * `UPDATE`'s own `revoked_at IS NULL` predicate keeps every row this call touches out of its
 * reach, and a `DELETE` with no `CHECK` and no trigger of its own has nothing to violate. So this
 * wraps the real [DataSource] to make the second `prepareStatement` call's `executeUpdate()`
 * throw instead of reaching the driver — a manufactured failure, since no organic one is
 * reachable through this port — but everything downstream of that throw is real: the `UPDATE`
 * before it already ran against the real container, and the `rollback()` inside
 * [PostgresDeviceBindings.revoke]'s `catch` block is a real statement sent to a real connection.
 * The assertions read that outcome back through a second, unwrapped connection, so nothing here
 * trusts the connection under test to describe its own state truthfully.
 */
class PostgresDeviceBindingsTest {
    private lateinit var dataSource: DataSource
    private lateinit var authSessions: PostgresAuthSessions
    private lateinit var bindings: PostgresDeviceBindings

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        authSessions = PostgresAuthSessions(dataSource, Clock.systemUTC())
        bindings = PostgresDeviceBindings(dataSource)
    }

    @Test
    fun theLiveBindingIsRevokedAndTheOtherSessionsAreGone() {
        runBlocking {
            val playerId = insertPlayer()
            insertLiveBinding(playerId)
            val t0 = authSessions.issue(playerId)
            val t1 = authSessions.issue(playerId)
            val t2 = authSessions.issue(playerId)

            bindings.revoke(playerId, keeping = t0)

            assertNotNull(revokedAt(playerId), "the live binding must be revoked")
            assertEquals(playerId, authSessions.playerOf(t0), "the kept token must still resolve")
            assertNull(authSessions.playerOf(t1), "a swept token must resolve to null")
            assertNull(authSessions.playerOf(t2), "a swept token must resolve to null")
        }
    }

    @Test
    fun theSurvivingRowIsTheOnlyRowLeft() {
        runBlocking {
            val playerId = insertPlayer()
            insertLiveBinding(playerId)
            val t0 = authSessions.issue(playerId)
            authSessions.issue(playerId)
            authSessions.issue(playerId)

            bindings.revoke(playerId, keeping = t0)

            assertEquals(1, countAuthSessionRows(playerId), "exactly one session must remain")
        }
    }

    @Test
    fun anotherPlayersSessionsAreUntouched() {
        runBlocking {
            val revoking = insertPlayer()
            insertLiveBinding(revoking)
            val kept = authSessions.issue(revoking)

            val other = insertPlayer()
            val otherFirst = authSessions.issue(other)
            val otherSecond = authSessions.issue(other)

            bindings.revoke(revoking, keeping = kept)

            assertEquals(revoking, authSessions.playerOf(kept), "the revoking player's own kept token must still resolve")
            assertEquals(other, authSessions.playerOf(otherFirst), "the other player's first token must still resolve")
            assertEquals(other, authSessions.playerOf(otherSecond), "the other player's second token must still resolve")
            assertEquals(2, countAuthSessionRows(other), "the other player must still have both sessions")
        }
    }

    @Test
    fun aPlayerWithNoLiveBindingStillLosesTheOtherSessions() {
        runBlocking {
            val playerId = insertPlayer()
            val t0 = authSessions.issue(playerId)
            val t1 = authSessions.issue(playerId)

            bindings.revoke(playerId, keeping = t0)

            assertEquals(playerId, authSessions.playerOf(t0), "the kept token must still resolve")
            assertNull(authSessions.playerOf(t1), "the other token must be gone even with no live binding to revoke")
        }
    }

    @Test
    fun anAlreadyRevokedBindingIsNotRewritten() {
        runBlocking {
            val playerId = insertPlayer()
            insertLiveBinding(playerId)
            val first = authSessions.issue(playerId)

            bindings.revoke(playerId, keeping = first)
            val revokedAtFirstCall = revokedAt(playerId)

            val second = authSessions.issue(playerId)
            bindings.revoke(playerId, keeping = second)
            val revokedAtSecondCall = revokedAt(playerId)

            assertEquals(revokedAtFirstCall, revokedAtSecondCall, "a second revoke must not move revoked_at")
        }
    }

    @Test
    fun nothingElseInTheDatabaseMoves() {
        runBlocking {
            val playerId = insertPlayer()
            insertLiveBinding(playerId)
            insertCredential(playerId)
            insertDuelResult(playerId)
            val t0 = authSessions.issue(playerId)
            authSessions.issue(playerId)

            val playerBefore = snapshot("player", "id")
            val credentialBefore = snapshot("credential", "id")
            val duelBefore = snapshot("duel", "id")
            val duelResultBefore = snapshot("duel_result", "duel_id, player_id")

            bindings.revoke(playerId, keeping = t0)

            assertEquals(playerBefore, snapshot("player", "id"), "player must be byte-identical after a revocation")
            assertEquals(credentialBefore, snapshot("credential", "id"), "credential must be byte-identical after a revocation")
            assertEquals(duelBefore, snapshot("duel", "id"), "duel must be byte-identical after a revocation")
            assertEquals(
                duelResultBefore,
                snapshot("duel_result", "duel_id, player_id"),
                "duel_result must be byte-identical after a revocation",
            )
        }
    }

    @Test
    fun aFailureBetweenTheStatementsRollsBackTheUpdate() {
        runBlocking {
            val playerId = insertPlayer()
            insertLiveBinding(playerId)
            val t0 = authSessions.issue(playerId)
            val t1 = authSessions.issue(playerId)
            val failingBindings = PostgresDeviceBindings(FailingStatementDataSource(dataSource, failOnCall = 2))

            assertFailsWith<SQLException> { failingBindings.revoke(playerId, keeping = t0) }

            assertNull(
                revokedAt(playerId),
                "a failed DELETE must roll back the UPDATE that ran before it in the same transaction",
            )
            assertEquals(playerId, authSessions.playerOf(t0), "the DELETE never really ran, so both sessions must still resolve")
            assertEquals(playerId, authSessions.playerOf(t1), "the DELETE never really ran, so both sessions must still resolve")
        }
    }

    private fun insertPlayer(): PlayerId {
        val id = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, ?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setInt(2, 100)
                statement.executeUpdate()
            }
        }
        return PlayerId(id.toString())
    }

    private fun insertLiveBinding(playerId: PlayerId, deviceId: String = "device-1") {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO device_binding (device_id, player_id) VALUES (?, ?)",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.executeUpdate()
            }
        }
    }

    private fun insertCredential(playerId: PlayerId) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.setString(3, "password")
                statement.setString(4, "player-${playerId.value}@example.test")
                statement.setString(5, "irrelevant-hash")
                statement.executeUpdate()
            }
        }
    }

    private fun insertDuelResult(playerId: PlayerId) {
        val duelId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel (id, format, started_at, finished_at, hands_played) VALUES (?, ?, now(), now(), ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setString(2, "heads-up")
                statement.setInt(3, 12)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.setInt(3, 1)
                statement.executeUpdate()
            }
        }
    }

    private fun revokedAt(playerId: PlayerId): OffsetDateTime? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT revoked_at FROM device_binding WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "no device_binding row for player ${playerId.value}" }
                    rows.getObject("revoked_at", OffsetDateTime::class.java)
                }
            }
        }

    private fun countAuthSessionRows(playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM auth_session WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    // SELECT * rather than a hand-picked column list, so this catches a column this test's
    // author never thought to name. table is always one of the four literals this class passes
    // in below, never anything an outside caller controls.
    private fun snapshot(table: String, orderBy: String): List<List<Any?>> =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM $table ORDER BY $orderBy").use { statement ->
                statement.executeQuery().use { rows ->
                    val columnCount = rows.metaData.columnCount
                    val out = mutableListOf<List<Any?>>()
                    while (rows.next()) {
                        out += (1..columnCount).map { rows.getObject(it) }
                    }
                    out
                }
            }
        }
}

// Wraps a real DataSource so the Nth prepareStatement call across every connection it hands out
// can be made to fail. This is how aFailureBetweenTheStatementsRollsBackTheUpdate manufactures a
// failure between PostgresDeviceBindings's two statements: nothing reachable through revoke()
// can make the DELETE fail on data alone.
private class FailingStatementDataSource(
    private val delegate: DataSource,
    private val failOnCall: Int,
) : DataSource by delegate {
    private var statementsPrepared = 0

    override fun getConnection(): Connection = FailingStatementConnection(delegate.connection)

    private inner class FailingStatementConnection(
        private val connectionDelegate: Connection,
    ) : Connection by connectionDelegate {
        override fun prepareStatement(sql: String): PreparedStatement {
            statementsPrepared++
            val real = connectionDelegate.prepareStatement(sql)
            return if (statementsPrepared == failOnCall) FailingPreparedStatement(real) else real
        }
    }
}

// executeUpdate() throws before delegating, so the statement is prepared for real against the
// real driver — only its execution is intercepted.
private class FailingPreparedStatement(
    private val delegate: PreparedStatement,
) : PreparedStatement by delegate {
    override fun executeUpdate(): Int =
        throw SQLException("manufactured failure standing in for a dropped connection or driver error")
}
