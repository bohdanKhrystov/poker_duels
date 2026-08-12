package duels.poker.server

import io.ktor.server.application.pluginOrNull
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ServerPluginsTest {
    @Test
    fun installsContentNegotiation() = testApplication {
        var plugin: Any? = null
        application {
            module()
            plugin = pluginOrNull(ContentNegotiation)
        }
        startApplication()
        assertNotNull(plugin)
    }

    @Test
    fun installsWebSockets() = testApplication {
        var plugin: Any? = null
        application {
            module()
            plugin = pluginOrNull(WebSockets)
        }
        startApplication()
        assertNotNull(plugin)
    }
}
