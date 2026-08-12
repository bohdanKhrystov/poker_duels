package duels.poker.server.config

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
        val serverConfig = ServerConfig.from(config) { "9001" }
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
        val serverConfig = ServerConfig.load { "9001" }
        assertEquals(9001, serverConfig.port)
    }
}
