package duels.poker.server

import duels.poker.server.config.ServerConfig
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

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
    routing {
        get("/health") {
            call.respondText("OK")
        }
    }
}
