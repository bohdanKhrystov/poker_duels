package duels.poker.server

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.DatabaseCoordinates
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Proves that a profile, its duel result, and its coin balance survive a restart of the database
 * layer — the reason ADR-0011 put PostgreSQL in v0.1 at all.
 *
 * The test simulates an honest restart by discarding everything that held state (the pool, the
 * repositories, any object graph) and building fresh instances against the same database, then
 * reading back through the real read path.
 */
class PersistenceSurvivesRestartTest {
    private lateinit var coordinates: DatabaseCoordinates

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        // Get coordinates once to avoid resetting the schema between calls
        coordinates = PostgresTestSupport.containerCoordinates()
    }

    @Test
    fun theSameDeviceResolvesToTheSameProfileAfterARestart() {
        val config = buildServerConfig(coordinates)
        val (survivorId, opponentId) = runBlocking {
            val pool = startDatabase(config)
            try {
                writePhase(pool)
            } finally {
                pool.close()
            }
        }

        // Simulate the restart: build fresh instances against the same database
        val newPool = startDatabase(config)
        try {
            val resolvedPlayerId = runBlocking {
                val directory = PostgresPlayerDirectory(newPool)
                val player = directory.resolve(DeviceId("survivor"))
                player.id
            }

            assertEquals(survivorId, resolvedPlayerId, "Device should resolve to the same profile ID")

            // Verify player row count is still 2
            val playerCount = playerRowCount(newPool)
            assertEquals(2, playerCount, "Player row count should be 2 after restart")
        } finally {
            newPool.close()
        }
    }

    @Test
    fun resultsAndBalancesSurviveARestart() {
        val config = buildServerConfig(coordinates)
        val (survivorId, opponentId) = runBlocking {
            val pool = startDatabase(config)
            try {
                writePhase(pool)
            } finally {
                pool.close()
            }
        }

        // Simulate the restart: build fresh instances against the same database
        val newPool = startDatabase(config)
        try {
            val duelCount = duelRowCount(newPool)
            val duelResultCount = duelResultRowCount(newPool)
            val survivorBalance = coinBalanceOf(newPool, survivorId)
            val opponentBalance = coinBalanceOf(newPool, opponentId)

            assertEquals(1, duelCount, "Duel row count should be 1 after restart")
            assertEquals(2, duelResultCount, "Duel result row count should be 2 after restart")
            assertEquals(1, survivorBalance, "Survivor (winner) balance should be 1")
            assertEquals(-1, opponentBalance, "Opponent (loser) balance should be -1")
        } finally {
            newPool.close()
        }
    }

    /**
     * Write phase: resolve two devices, record one duel won by seat 0, and return the player IDs.
     */
    private suspend fun writePhase(pool: DataSource): Pair<PlayerId, PlayerId> {
        val directory = PostgresPlayerDirectory(pool)
        val store = PostgresDuelResultStore(pool)

        val survivor = directory.resolve(DeviceId("survivor"))
        val opponent = directory.resolve(DeviceId("opponent"))

        // Create a duel where seat 0 (survivor) wins
        val duel = FinishedDuel(
            id = UUID.randomUUID(),
            format = "FREEZEOUT",
            startedAt = Instant.now(),
            finishedAt = Instant.now(),
            seats = listOf(survivor.id, opponent.id),
            outcome = DuelOutcome(
                winner = 0, // seat 0 wins
                handsPlayed = 10,
                finalStacks = listOf(1000, 0),
            ),
        )

        store.record(duel)

        return survivor.id to opponent.id
    }

    /**
     * Read the coin balance of a player from the database.
     */
    private fun coinBalanceOf(pool: DataSource, playerId: PlayerId): Int {
        return pool.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_balance FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "Player not found: $playerId" }
                    resultSet.getInt("coin_balance")
                }
            }
        }
    }

    /**
     * Read the number of duel rows from the database.
     */
    private fun duelRowCount(pool: DataSource): Int {
        return pool.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) as count FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "COUNT query should return a row" }
                    resultSet.getInt("count")
                }
            }
        }
    }

    /**
     * Read the number of duel_result rows from the database.
     */
    private fun duelResultRowCount(pool: DataSource): Int {
        return pool.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) as count FROM duel_result").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "COUNT query should return a row" }
                    resultSet.getInt("count")
                }
            }
        }
    }

    /**
     * Read the number of player rows from the database.
     */
    private fun playerRowCount(pool: DataSource): Int {
        return pool.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) as count FROM player").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "COUNT query should return a row" }
                    resultSet.getInt("count")
                }
            }
        }
    }

    /**
     * Build a [ServerConfig] from the given coordinates, using databasePoolSize = 2.
     */
    private fun buildServerConfig(coordinates: DatabaseCoordinates): ServerConfig {
        return ServerConfig(
            port = ServerConfig.DEFAULT_PORT,
            maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
            maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
            databaseUrl = coordinates.url,
            databaseUser = coordinates.user,
            databasePassword = coordinates.password,
            databasePoolSize = 2,
            roomWaitingTimeoutMillis = ServerConfig.DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS,
            roomFinishedTimeoutMillis = ServerConfig.DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS,
        )
    }
}
