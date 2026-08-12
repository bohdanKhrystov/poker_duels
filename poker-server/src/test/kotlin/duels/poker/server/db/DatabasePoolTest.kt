package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource

/**
 * Proves [Database.connectionPool] serves real connections against the shared container and
 * respects [ServerConfig]'s pool size, without needing a migrated schema.
 */
internal class DatabasePoolTest {
    @BeforeEach
    fun requireDocker() {
        PostgresTestSupport.requireDocker()
    }

    @Test
    fun servesAConnectionThatAnswersSelectOne() {
        Database.connectionPool(containerConfig()).use { pool ->
            pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { result ->
                        assertTrue(result.next())
                        assertEquals(1, result.getInt(1))
                    }
                }
            }
        }
    }

    @Test
    fun honoursTheConfiguredPoolSize() {
        Database.connectionPool(containerConfig(databasePoolSize = 2)).use { pool ->
            assertEquals(2, pool.maximumPoolSize)

            pool.connection.use { first ->
                pool.connection.use { second ->
                    assertTrue(first.isValid(1))
                    assertTrue(second.isValid(1))
                }
            }
        }
    }

    @Test
    fun closingThePoolClosesIt() {
        val pool = Database.connectionPool(containerConfig())

        pool.close()

        assertTrue(pool.isClosed)
    }

    /**
     * A [ServerConfig] pointed at the shared container, via the coordinates of a real connection
     * from [PostgresTestSupport.freshDatabase] — the container itself is private to
     * [PostgresTestSupport], so this is its only public route out. Only a schema reset is
     * incurred; no migration runs, and none is needed to serve `SELECT 1`.
     */
    private fun containerConfig(databasePoolSize: Int = 2): ServerConfig {
        val source = PostgresTestSupport.freshDatabase() as PGSimpleDataSource
        return ServerConfig(
            port = ServerConfig.DEFAULT_PORT,
            maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
            maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
            databaseUrl = requireNotNull(source.getUrl()),
            databaseUser = requireNotNull(source.getUser()),
            databasePassword = requireNotNull(source.getPassword()),
            databasePoolSize = databasePoolSize,
            roomWaitingTimeoutMillis = ServerConfig.DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS,
            roomFinishedTimeoutMillis = ServerConfig.DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS,
        )
    }
}
