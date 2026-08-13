package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PostgresDuelResultStoreTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory
    private lateinit var store: PostgresDuelResultStore
    private lateinit var alice: duels.poker.server.session.Player
    private lateinit var bob: duels.poker.server.session.Player

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
        store = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = directory.resolve(DeviceId("alice"))
            bob = directory.resolve(DeviceId("bob"))
        }
    }

    @Test
    fun recordingAFinishedDuelWritesOneDuelRow() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(1, duelRowCount())
        val format = duelFormatOf(duel.id)
        assertEquals("FREEZEOUT", format)
    }

    @Test
    fun recordingAFinishedDuelWritesOneResultRowPerSeat() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(2, duelResultRowCount())
        assertEquals(1, resultDeltaOf(duel.id, alice.id))
        assertEquals(-1, resultDeltaOf(duel.id, bob.id))
    }

    @Test
    fun theWinnersBalanceRisesByExactlyOne() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(1, coinBalanceOf(alice.id))
    }

    @Test
    fun theLosersBalanceFallsByExactlyOne() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(-1, coinBalanceOf(bob.id))
    }

    @Test
    fun aDuelNamingAnUnknownPlayerLeavesNoRowAndNoCoinBehind() = runBlocking {
        val unknownPlayer = PlayerId(UUID.randomUUID().toString())
        val duel = finishedDuel(winner = 0).copy(seats = listOf(alice.id, unknownPlayer))

        // The duel row is inserted before any result row, so by the time the foreign key
        // fires there is already work in the transaction to undo.
        assertFailsWith<SQLException> { store.record(duel) }

        assertEquals(0, duelRowCount())
        assertEquals(0, duelResultRowCount())
        // The third assertion is the point of the ticket: a duel that failed to record
        // must not have paid anybody, or the ladder stops matching its own history.
        assertEquals(0, coinBalanceOf(alice.id))
    }

    @Test
    fun theFailedWriteRollsBackExplicitlyRatherThanRelyingOnTheConnectionClosing() = runBlocking {
        val unknownPlayer = PlayerId(UUID.randomUUID().toString())
        val duel = finishedDuel(winner = 0).copy(seats = listOf(alice.id, unknownPlayer))

        // Use a wrapped DataSource that tracks rollback() calls to prove the
        // transaction boundary uses explicit rollback, not just connection closure.
        // This matters in production where HikariCP pools connections; relying on
        // connection closure would leave dirty transactions.
        val trackingDataSource = TrackingDataSource(dataSource)
        val trackingStore = PostgresDuelResultStore(trackingDataSource)

        assertFailsWith<SQLException> { trackingStore.record(duel) }

        assertEquals(0, duelRowCount())
        assertEquals(0, duelResultRowCount())
        assertEquals(0, coinBalanceOf(alice.id))
        // The critical assertion: prove rollback() was explicitly invoked, not just
        // relying on connection closure to clean up the transaction server-side.
        assertTrue(trackingDataSource.wasRollbackCalled)
    }

    private fun duelRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun duelResultRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel_result").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun duelFormatOf(duelId: UUID): String {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT format FROM duel WHERE id = ?").use { statement ->
                statement.setObject(1, duelId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
    }

    private fun resultDeltaOf(duelId: UUID, playerId: PlayerId): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_delta FROM duel_result WHERE duel_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun coinBalanceOf(playerId: PlayerId): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_balance FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun finishedDuel(winner: Int?, id: UUID = UUID.randomUUID()): FinishedDuel {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        return FinishedDuel(
            id = id,
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:00:00Z"),
            finishedAt = Instant.parse("2026-08-13T10:05:00Z"),
            seats = listOf(alice.id, bob.id),
            outcome = outcome,
        )
    }

    /**
     * Wraps a Connection to track whether rollback() was explicitly called.
     */
    private class TrackingConnection(private val delegate: Connection) : Connection by delegate {
        var rollbackCalled = false
            private set

        override fun rollback() {
            rollbackCalled = true
            delegate.rollback()
        }
    }

    /**
     * Wraps a DataSource to return TrackingConnections that record rollback() calls.
     */
    private class TrackingDataSource(private val delegate: DataSource) : DataSource {
        private var lastTrackingConnection: TrackingConnection? = null
        val wasRollbackCalled: Boolean
            get() = lastTrackingConnection?.rollbackCalled ?: false

        override fun getConnection(): Connection {
            val connection = delegate.connection
            val tracking = TrackingConnection(connection)
            lastTrackingConnection = tracking
            return tracking
        }

        override fun getConnection(username: String?, password: String?): Connection {
            val connection = delegate.getConnection(username, password)
            val tracking = TrackingConnection(connection)
            lastTrackingConnection = tracking
            return tracking
        }

        override fun getLoginTimeout(): Int = delegate.loginTimeout
        override fun setLoginTimeout(seconds: Int) {
            delegate.loginTimeout = seconds
        }

        override fun getLogWriter(): PrintWriter = delegate.logWriter
        override fun setLogWriter(out: PrintWriter) {
            delegate.logWriter = out
        }

        override fun getParentLogger(): java.util.logging.Logger = delegate.parentLogger

        override fun <T : Any?> unwrap(iface: Class<T>?): T = delegate.unwrap(iface)
        override fun isWrapperFor(iface: Class<*>?): Boolean = delegate.isWrapperFor(iface)
    }
}
