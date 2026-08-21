package duels.poker.server

import com.zaxxer.hikari.HikariDataSource
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Database
import duels.poker.server.db.Migrations
import duels.poker.server.http.authRoutes
import duels.poker.server.http.profileRoutes
import duels.poker.server.http.standingsRoutes
import duels.poker.server.room.GraceExpiry
import duels.poker.server.room.RoomRegistry
import duels.poker.server.session.ConnectionDirectory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger

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
    embeddedServer(Netty, port = config.port) { duelServer(components, config.sweepPeriodMillis) }.start(wait = true)
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
 * Composes the complete duel server by installing the socket, HTTP profile routes, plugins, and
 * the periodic sweep `ADR-0025` decides on.
 *
 * The module is invoked first to install plugins and the health route, then the socket and
 * profile routes are installed to share the same application and database connection pool.
 * [scheduleSweeps] runs last, once [components] — the only thing the sweep needs — already
 * exists, so every caller of this function, the shipped [main] and any test that boots the server
 * the same way, gets a server that reaps idle rooms and expires disconnect grace windows by
 * itself: the gap `ADR-0025` closes is that nothing used to call either.
 *
 * @param components The server components built from configuration and data source.
 * @param sweepPeriodMillis How often [scheduleSweeps] runs, per `ADR-0025`. Defaults to
 *   [ServerConfig.DEFAULT_SWEEP_PERIOD_MILLIS] so a caller with no [ServerConfig] of its own —
 *   every existing test that builds this server — keeps compiling unchanged; [main] always passes
 *   its loaded configuration's own value instead of relying on the default.
 */
public fun Application.duelServer(
    components: ServerComponents,
    sweepPeriodMillis: Long = ServerConfig.DEFAULT_SWEEP_PERIOD_MILLIS,
) {
    module()
    duelSocket(components.socket)
    authRoutes(components.reads, components.credentials)
    profileRoutes(components.reads, components.writes)
    standingsRoutes(components.reads, components.standings, components.wallClock)
    scheduleSweeps(components.socket.rooms, components.socket.connections, sweepPeriodMillis)
}

/**
 * Launches the ticker coroutine `ADR-0025` specifies (resolving `DEC-019`), as a child of this
 * [Application]'s own coroutine scope.
 *
 * Structured concurrency is the loop's entire lifecycle: no [kotlinx.coroutines.Job] is kept, no
 * plugin is installed and nothing shuts it down explicitly — stopping the application cancels the
 * scope this coroutine is a child of, which is the only way this loop ever ends.
 *
 * @param rooms The registry [sweepPass] sweeps.
 * @param connections Where a grace expiry's outbound frames are delivered.
 * @param sweepPeriodMillis The fixed delay between the end of one pass and the start of the next;
 *   fixed-*delay*, not fixed-rate, so passes never overlap and an overrun stretches the interval
 *   instead of piling up.
 */
private fun Application.scheduleSweeps(
    rooms: RoomRegistry,
    connections: ConnectionDirectory,
    sweepPeriodMillis: Long,
) {
    launch {
        while (true) {
            delay(sweepPeriodMillis)
            sweepPass(rooms, connections, log)
        }
    }
}

/**
 * Runs one pass of both sweeps `ADR-0025` assigns this loop, in order: expire every disconnect
 * grace window that has run out and deliver the frames each expiry produced, then reap every room
 * idle past its configured limit.
 *
 * The two steps are guarded independently: each catches every [Throwable] except
 * [CancellationException], logs it to [log], and moves on. A failing grace pass does not skip
 * reaping, and either step failing here is simply retried the next time [scheduleSweeps]'s loop
 * calls this function. [CancellationException] always rethrows — that, and nothing else, is how
 * the loop that calls this ever ends.
 *
 * @param rooms The registry both sweeps run against.
 * @param connections Where each [GraceExpiry]'s outbound frames are delivered.
 * @param log Where a failing pass is logged; the caller's own [Application.log].
 */
private suspend fun sweepPass(rooms: RoomRegistry, connections: ConnectionDirectory, log: Logger) {
    try {
        for (expiry in rooms.expireGracePeriods()) {
            deliver(expiry.outbound, expiry.room, connections)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        log.error("sweep: expiring disconnect grace windows failed", failure)
    }

    try {
        rooms.reap()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        log.error("sweep: reaping idle rooms failed", failure)
    }
}
