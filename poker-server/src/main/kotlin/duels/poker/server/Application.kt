package duels.poker.server

import duels.poker.server.config.ServerConfig
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets

/**
 * Entry point that starts Ktor on Netty at the configured port.
 */
public fun main() {
    val config = ServerConfig.load()
    embeddedServer(Netty, port = config.port) { module() }.start(wait = true)
}

/**
 * Application module that configures the server's routes and features.
 */
public fun Application.module() {
    install(ContentNegotiation) { json() }
    install(WebSockets)
    routing {
        get("/health") {
            call.respondText("OK")
        }
    }
}
