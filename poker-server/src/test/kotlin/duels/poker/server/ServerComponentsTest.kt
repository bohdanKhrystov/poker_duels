package duels.poker.server

import com.zaxxer.hikari.HikariDataSource
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Database
import duels.poker.server.db.DatabaseCoordinates
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.http.SetNameResult
import duels.poker.server.room.RoomTimeouts
import duels.poker.server.session.DeviceId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Proves that [serverComponents] wires together real, database-backed collaborators that
 * work together correctly.
 */
class ServerComponentsTest {
    private lateinit var dataSource: HikariDataSource
    private lateinit var coordinates: DatabaseCoordinates

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        coordinates = PostgresTestSupport.containerCoordinates()
        val config = buildServerConfig(coordinates)
        dataSource = Database.connectionPool(config)
        Migrations.migrate(dataSource)
    }

    @Test
    fun theDirectoryWritesToTheDatabase() {
        val config = buildServerConfig(coordinates)
        val components = serverComponents(config, dataSource)

        runBlocking {
            components.socket.directory.resolve(DeviceId("wired"))
        }

        val count = playerRowCount()
        assertEquals(1, count, "One player row should exist after resolve")
    }

    @Test
    fun theReadsSeeWhatTheDirectoryWrote() {
        val config = buildServerConfig(coordinates)
        val components = serverComponents(config, dataSource)

        val player = runBlocking {
            components.socket.directory.resolve(DeviceId("wired"))
        }

        val profile = runBlocking {
            components.reads.profileOf(DeviceId("wired"))
        }

        assertNotNull(profile, "Profile should exist after directory resolve")
        assertEquals(player.id.value, profile!!.playerId, "Profile playerId should match resolved player id")
        assertEquals(0, profile.coinBalance, "Initial coin balance should be 0")
    }

    @Test
    fun theFrameLimitsComeFromTheConfig() {
        val config = buildServerConfig(
            coordinates,
            maxFrameLength = 4321,
            maxFrameNestingDepth = 12,
        )
        val components = serverComponents(config, dataSource)

        assertEquals(4321, components.socket.maxFrameLength)
        assertEquals(12, components.socket.maxFrameNestingDepth)
    }

    @Test
    fun theRoomsDrawFromTheSeedSourceGiven() {
        val config = buildServerConfig(coordinates)
        val seedSource = HandSeedSource { 4242L }
        val components = serverComponents(
            config,
            dataSource,
            seeds = seedSource,
        )

        assertSame(seedSource, components.socket.rooms.handSeeds)
    }

    @Test
    fun theWritesAndTheReadsShareOneDatabase() {
        val config = buildServerConfig(coordinates)
        val components = serverComponents(config, dataSource)

        val player = runBlocking {
            components.socket.directory.resolve(DeviceId("writes-test"))
        }

        val setResult = runBlocking {
            components.writes.setDisplayName(player.id, "TestName")
        }

        assertTrue(setResult is SetNameResult.NameSet, "setDisplayName should succeed")

        val profile = runBlocking {
            components.reads.profileOf(DeviceId("writes-test"))
        }

        assertNotNull(profile, "Profile should exist after writes")
        assertEquals("TestName", profile!!.displayName, "Display name should be what we wrote")
    }

    /**
     * Build a [ServerConfig] for testing, with reasonable defaults overridable per test.
     */
    private fun buildServerConfig(
        coordinates: DatabaseCoordinates,
        maxFrameLength: Int = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
        maxFrameNestingDepth: Int = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
    ): ServerConfig {
        return ServerConfig(
            port = ServerConfig.DEFAULT_PORT,
            maxFrameLength = maxFrameLength,
            maxFrameNestingDepth = maxFrameNestingDepth,
            databaseUrl = coordinates.url,
            databaseUser = coordinates.user,
            databasePassword = coordinates.password,
            databasePoolSize = 2,
            roomWaitingTimeoutMillis = RoomTimeouts.DEFAULT_WAITING_MILLIS,
            roomFinishedTimeoutMillis = RoomTimeouts.DEFAULT_FINISHED_MILLIS,
        )
    }

    /**
     * Read the number of player rows from the database.
     */
    private fun playerRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) as count FROM player").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "COUNT query should return a row" }
                    resultSet.getInt("count")
                }
            }
        }
    }
}
