package duels.poker.server.config

import duels.poker.server.room.RoomTimeouts
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ServerConfigTest {
    @Test
    fun readsThePortFromTheConfig() {
        val config = MapApplicationConfig("server.port" to "7000")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(7000, serverConfig.port)
    }

    @Test
    fun theEnvironmentVariableOverridesTheConfig() {
        val config = MapApplicationConfig("server.port" to "7000")
        val serverConfig = ServerConfig.from(config) { name -> if (name == ServerConfig.PORT_ENV) "9001" else null }
        assertEquals(9001, serverConfig.port)
    }

    @Test
    fun fallsBackToTheDefaultWhenNothingIsSet() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(ServerConfig.DEFAULT_PORT, serverConfig.port)
    }

    @Test
    fun rejectsAPortThatIsNotANumber() {
        val config = MapApplicationConfig("server.port" to "eighty-eighty")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun loadsThePortFromTheShippedApplicationConf() {
        val serverConfig = ServerConfig.load { null }
        assertEquals(8080, serverConfig.port)
    }

    @Test
    fun theEnvironmentVariableOverridesTheShippedFile() {
        val serverConfig = ServerConfig.load { name -> if (name == ServerConfig.PORT_ENV) "9001" else null }
        assertEquals(9001, serverConfig.port)
    }

    @Test
    fun readsTheDatabaseUrlFromTheConfig() {
        val config = MapApplicationConfig("database.url" to "jdbc:postgresql://db:5432/x")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals("jdbc:postgresql://db:5432/x", serverConfig.databaseUrl)
    }

    @Test
    fun theEnvironmentVariableOverridesTheDatabaseUrl() {
        val config = MapApplicationConfig("database.url" to "jdbc:postgresql://db:5432/x")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.DATABASE_URL_ENV) "jdbc:postgresql://env:5432/y" else null
        }
        assertEquals("jdbc:postgresql://env:5432/y", serverConfig.databaseUrl)
    }

    @Test
    fun fallsBackToTheDefaultDatabaseSettings() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(ServerConfig.DEFAULT_DATABASE_URL, serverConfig.databaseUrl)
        assertEquals(ServerConfig.DEFAULT_DATABASE_USER, serverConfig.databaseUser)
        assertEquals(ServerConfig.DEFAULT_DATABASE_PASSWORD, serverConfig.databasePassword)
        assertEquals(ServerConfig.DEFAULT_DATABASE_POOL_SIZE, serverConfig.databasePoolSize)
    }

    @Test
    fun rejectsAPoolSizeThatIsNotANumber() {
        val config = MapApplicationConfig("database.poolSize" to "many")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun loadsTheDatabaseSettingsFromTheShippedApplicationConf() {
        val serverConfig = ServerConfig.load { null }
        assertEquals(ServerConfig.DEFAULT_DATABASE_URL, serverConfig.databaseUrl)
        assertEquals(ServerConfig.DEFAULT_DATABASE_USER, serverConfig.databaseUser)
        assertEquals(ServerConfig.DEFAULT_DATABASE_PASSWORD, serverConfig.databasePassword)
        assertEquals(ServerConfig.DEFAULT_DATABASE_POOL_SIZE, serverConfig.databasePoolSize)
    }

    @Test
    fun readsTheRoomWaitingTimeoutFromTheConfig() {
        val config = MapApplicationConfig("room.waitingTimeoutMillis" to "1234")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(1234L, serverConfig.roomWaitingTimeoutMillis)
    }

    @Test
    fun theEnvironmentVariableOverridesTheRoomFinishedTimeout() {
        val config = MapApplicationConfig("room.finishedTimeoutMillis" to "1234")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.ROOM_FINISHED_TIMEOUT_MILLIS_ENV) "5678" else null
        }
        assertEquals(5678L, serverConfig.roomFinishedTimeoutMillis)
    }

    @Test
    fun fallsBackToTheRoomTimeoutDefaults() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(RoomTimeouts.DEFAULT_WAITING_MILLIS, serverConfig.roomWaitingTimeoutMillis)
        assertEquals(RoomTimeouts.DEFAULT_FINISHED_MILLIS, serverConfig.roomFinishedTimeoutMillis)
    }

    @Test
    fun rejectsARoomTimeoutThatIsNotANumber() {
        val config = MapApplicationConfig("room.waitingTimeoutMillis" to "ten minutes")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun rejectsANonPositiveRoomTimeout() {
        val config = MapApplicationConfig("room.finishedTimeoutMillis" to "0")
        val serverConfig = ServerConfig.from(config) { null }
        assertThrows<IllegalArgumentException> {
            serverConfig.roomTimeouts()
        }
    }

    @Test
    fun roomTimeoutsBundlesAllThreeValues() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(
            RoomTimeouts(serverConfig.roomWaitingTimeoutMillis, serverConfig.roomFinishedTimeoutMillis, serverConfig.disconnectGraceMillis),
            serverConfig.roomTimeouts(),
        )
    }

    @Test
    fun readsTheGraceWindowFromTheConfig() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "45000")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(45_000L, serverConfig.disconnectGraceMillis)
    }

    @Test
    fun theEnvironmentVariableOverridesTheGraceWindow() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "45000")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.DISCONNECT_GRACE_MILLIS_ENV) "90000" else null
        }
        assertEquals(90_000L, serverConfig.disconnectGraceMillis)
    }

    @Test
    fun rejectsAGraceWindowThatIsNotANumber() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "a minute")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }
}
