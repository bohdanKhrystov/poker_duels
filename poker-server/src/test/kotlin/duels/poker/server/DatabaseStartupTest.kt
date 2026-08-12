package duels.poker.server

import duels.poker.server.config.ServerConfig
import duels.poker.server.db.DatabaseCoordinates
import duels.poker.server.db.PostgresTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Proves [startDatabase] opens the pool and brings the schema up to date, and that calling it
 * twice is idempotent.
 */
class DatabaseStartupTest {
    @BeforeEach
    fun requireDocker() {
        PostgresTestSupport.requireDocker()
    }

    @Test
    fun startupAppliesTheSchema() {
        startDatabase(containerConfig()).use { pool ->
            pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    // Verify that the schema tables exist
                    val resultSet = statement.executeQuery(
                        """
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name IN ('player', 'duel', 'duel_result')
                        ORDER BY table_name
                        """.trimIndent(),
                    )
                    val tables = mutableSetOf<String>()
                    while (resultSet.next()) {
                        tables.add(resultSet.getString("table_name"))
                    }
                    resultSet.close()

                    assertEquals(setOf("player", "duel", "duel_result"), tables)
                }
            }
        }
    }

    @Test
    fun aSecondStartupAppliesNothing() {
        // Get coordinates once to avoid resetting the schema between calls
        val coordinates = PostgresTestSupport.containerCoordinates()
        val config = buildServerConfig(coordinates)

        // First startup
        val firstCount = startDatabase(config).use { pool ->
            pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(
                        "SELECT COUNT(*) as row_count FROM flyway_schema_history",
                    )
                    assertTrue(resultSet.next())
                    val count = resultSet.getInt("row_count")
                    resultSet.close()
                    count
                }
            }
        }

        // Second startup against the same database
        val secondCount = startDatabase(config).use { pool ->
            pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(
                        "SELECT COUNT(*) as row_count FROM flyway_schema_history",
                    )
                    assertTrue(resultSet.next())
                    val count = resultSet.getInt("row_count")
                    resultSet.close()
                    count
                }
            }
        }

        // Second startup should not apply any migrations
        assertEquals(firstCount, secondCount)
    }

    @Test
    fun startupReturnsAUsablePool() {
        val pool = startDatabase(containerConfig())
        try {
            pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery("SELECT 1")
                    assertTrue(resultSet.next())
                    assertEquals(1, resultSet.getInt(1))
                    resultSet.close()
                }
            }
        } finally {
            pool.close()
        }
    }

    /**
     * A [ServerConfig] pointed at the shared container, via the coordinates from
     * [PostgresTestSupport.containerCoordinates] — the container itself is private to
     * [PostgresTestSupport], so this is its only public route out. Schema is reset so each
     * test starts fresh.
     */
    private fun containerConfig(databasePoolSize: Int = 2): ServerConfig {
        val coordinates = PostgresTestSupport.containerCoordinates()
        return buildServerConfig(coordinates, databasePoolSize)
    }

    /**
     * Build a [ServerConfig] from the given coordinates.
     */
    private fun buildServerConfig(
        coordinates: DatabaseCoordinates,
        databasePoolSize: Int = 2,
    ): ServerConfig {
        return ServerConfig(
            port = ServerConfig.DEFAULT_PORT,
            maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
            maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
            databaseUrl = coordinates.url,
            databaseUser = coordinates.user,
            databasePassword = coordinates.password,
            databasePoolSize = databasePoolSize,
            roomWaitingTimeoutMillis = ServerConfig.DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS,
            roomFinishedTimeoutMillis = ServerConfig.DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS,
        )
    }
}
