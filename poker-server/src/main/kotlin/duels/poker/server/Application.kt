package duels.poker.server

import com.zaxxer.hikari.HikariDataSource
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Database
import duels.poker.server.db.Migrations
import duels.poker.server.http.profileRoutes
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
 * Opens the single connection pool and brings the schema up to date.
 */
public fun startDatabase(config: ServerConfig): HikariDataSource {
    val pool = Database.connectionPool(config)
    Migrations.migrate(pool)
    return pool
}

/**
 * Entry point that starts Ktor on Netty at the configured port.
 */
public fun main() {
    val config = ServerConfig.load()
    val pool = startDatabase(config)
    val components = serverComponents(config, pool)
    embeddedServer(Netty, port = config.port) { duelServer(components) }.start(wait = true)
    pool.close()
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

/**
 * Composes the complete duel server by installing the socket, HTTP profile routes, and plugins.
 *
 * The module is invoked first to install plugins and the health route, then the socket and
 * profile routes are installed to share the same application and database connection pool.
 *
 * @param components The server components built from configuration and data source.
 */
public fun Application.duelServer(components: ServerComponents) {
    module()
    duelSocket(components.socket)
    profileRoutes(components.reads)
}
