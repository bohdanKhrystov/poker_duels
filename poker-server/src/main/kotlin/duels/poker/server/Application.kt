package duels.poker.server

import com.zaxxer.hikari.HikariDataSource
import duels.poker.server.auth.RecoveryEmails
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Database
import duels.poker.server.db.Migrations
import duels.poker.server.http.authRoutes
import duels.poker.server.http.deviceRoutes
import duels.poker.server.http.profileRoutes
import duels.poker.server.http.recoveryRoutes
import duels.poker.server.http.standingsRoutes
import duels.poker.server.mail.DetachedRecoveryMailer
import duels.poker.server.room.RoomRegistry
import duels.poker.server.room.TurnClockExpiry
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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
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
 * profile routes are installed to share the same application and database connection pool. The
 * recovery routes are installed against a decorated mailer built here — `ADR-0077` §3 — and
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
    authRoutes(
        components.reads,
        components.credentials,
        components.identities,
        components.sessions,
        components.signUpBudget,
        components.signInBudget,
    )
    profileRoutes(components.reads, components.writes, components.identities)
    deviceRoutes(components.identities, components.credentials, components.bindings)
    standingsRoutes(components.reads, components.standings, components.wallClock, components.identities)
    // A supervisor child of the application's job, per ADR-0077 §3: a child, so shutdown cancels
    // every in-flight send; a supervisor, so one failed send reaches no sibling and never this
    // application's own job; its own scope rather than the application's, because that one also
    // carries the sweep loop scheduleSweeps installs below, which never completes.
    val delivery = CoroutineScope(
        coroutineContext + SupervisorJob(coroutineContext.job) + CoroutineName("recovery-mail"),
    )
    recoveryRoutes(
        components.recoveryEmails,
        components.passwordResets,
        components.identities,
        components.credentials,
        DetachedRecoveryMailer(components.mailer, delivery, log),
    )
    scheduleSweeps(
        components.socket.rooms,
        components.socket.connections,
        components.recoveryEmails,
        sweepPeriodMillis,
    )
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
 * @param recoveryEmails The port [sweepPass] sweeps expired `email_verification` rows from.
 * @param sweepPeriodMillis The fixed delay between the end of one pass and the start of the next;
 *   fixed-*delay*, not fixed-rate, so passes never overlap and an overrun stretches the interval
 *   instead of piling up.
 */
private fun Application.scheduleSweeps(
    rooms: RoomRegistry,
    connections: ConnectionDirectory,
    recoveryEmails: RecoveryEmails,
    sweepPeriodMillis: Long,
) {
    launch {
        while (true) {
            delay(sweepPeriodMillis)
            sweepPass(rooms, connections, recoveryEmails, log)
        }
    }
}

/**
 * Runs one pass of the three sweeps `ADR-0025` assigns this loop, in order: expire every
 * disconnect grace window that has run out and deliver the frames each expiry produced, reap
 * every room idle past its configured limit, then delete every expired `email_verification` row
 * (`ADR-0031` §3). The database step runs last, so a database outage can never delay a
 * grace-period expiry that decides a duel.
 *
 * The three steps are guarded independently: each catches every [Throwable] except
 * [CancellationException], logs it to [log], and moves on. A failing step does not skip the ones
 * after it, and any step failing here is simply retried the next time [scheduleSweeps]'s loop
 * calls this function. [CancellationException] always rethrows — that, and nothing else, is how
 * the loop that calls this ever ends.
 *
 * @param rooms The registry the first two sweeps run against.
 * @param connections Where each [TurnClockExpiry]'s outbound frames are delivered.
 * @param recoveryEmails The port the third sweep deletes expired verification rows from.
 * @param log Where a failing pass is logged; the caller's own [Application.log].
 */
private suspend fun sweepPass(
    rooms: RoomRegistry,
    connections: ConnectionDirectory,
    recoveryEmails: RecoveryEmails,
    log: Logger,
) {
    try {
        for (expiry in rooms.expireTurnClocks()) {
            deliver(expiry.outbound, expiry.room, connections)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        log.error("sweep: expiring turn clocks failed", failure)
    }

    try {
        rooms.reap()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        log.error("sweep: reaping idle rooms failed", failure)
    }

    try {
        recoveryEmails.deleteExpiredVerifications()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        log.error("sweep: deleting expired email verifications failed", failure)
    }
}
