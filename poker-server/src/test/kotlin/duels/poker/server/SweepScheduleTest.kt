package duels.poker.server

import duels.poker.server.config.ServerConfig
import duels.poker.server.e2e.HAND_SEED
import duels.poker.server.e2e.completeHandshake
import duels.poker.server.e2e.freshMigratedDatabase
import duels.poker.server.e2e.handSeedSource
import duels.poker.server.e2e.nextServerMessage
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.room.RoomCode
import duels.poker.server.session.PlayerId
import duels.poker.server.time.ServerClock
import duels.poker.server.time.SystemClock
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How often the scheduled ticker sweeps; shrunk so both tests finish in well under a second. */
private const val SWEEP_PERIOD_MILLIS = 20L

/** How long a room may sit idle before it is reapable; shrunk to match [SWEEP_PERIOD_MILLIS]. */
private const val WAITING_MILLIS = 60L

/**
 * How many consecutive [ServerClock.nowMillis] calls the poisoned clock in
 * [SweepScheduleTest.aSweepThatThrowsDoesNotStopTheNextOne] fails before it recovers — enough to
 * span several full ticks, so both the grace pass and the reap pass fail repeatedly before either
 * succeeds.
 */
private const val INDUCED_FAILURES = 20

/** The polling interval for a bounded wait on the registry's state — never a blind sleep. */
private val POLL_INTERVAL = 10.milliseconds

/** A [ServerConfig] with every timeout shrunk to [WAITING_MILLIS] and [SWEEP_PERIOD_MILLIS]. */
private fun shrunkServerConfig(): ServerConfig =
    ServerConfig(
        port = ServerConfig.DEFAULT_PORT,
        maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
        maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
        databaseUrl = ServerConfig.DEFAULT_DATABASE_URL,
        databaseUser = ServerConfig.DEFAULT_DATABASE_USER,
        databasePassword = ServerConfig.DEFAULT_DATABASE_PASSWORD,
        databasePoolSize = ServerConfig.DEFAULT_DATABASE_POOL_SIZE,
        roomWaitingTimeoutMillis = WAITING_MILLIS,
        roomFinishedTimeoutMillis = ServerConfig.DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS,
        sweepPeriodMillis = SWEEP_PERIOD_MILLIS,
    )

/**
 * `TASK-021212`: `Application.duelServer` installs the ticker `ADR-0025` specifies, so a running
 * server sweeps its own registry without anything else calling into it. Both tests here boot the
 * real module and only ever observe the registry from the outside — neither one drives a sweep
 * itself.
 */
class SweepScheduleTest {
    @Test
    fun anIdleRoomIsSweptWithoutBeingAsked(): Unit = testApplication {
        val config = shrunkServerConfig()
        val components = serverComponents(config, freshMigratedDatabase(), seeds = handSeedSource(HAND_SEED))
        application { duelServer(components, config.sweepPeriodMillis) }
        val client = createClient { install(WebSockets) }

        val code = withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.completeHandshake("sweep-host")
            session.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
            val created = session.nextServerMessage() as ServerMessage.RoomJoined
            RoomCode(created.code)
        }
        assertNotNull(components.socket.rooms.get(code), "the room must exist right after creation")

        withTimeout(5.seconds) {
            while (components.socket.rooms.get(code) != null) {
                delay(POLL_INTERVAL)
            }
        }

        // The room outlived its configured waitingMillis and vanished with no sweep call in this
        // test — only the scheduled ticker could have removed it.
        assertNull(components.socket.rooms.get(code))
    }

    @Test
    fun aSweepThatThrowsDoesNotStopTheNextOne(): Unit = testApplication {
        val config = shrunkServerConfig()
        val remainingFailures = AtomicInteger(0)
        // Behaves exactly like SystemClock, except that it throws for the next INDUCED_FAILURES
        // calls once armed — a deterministic stand-in for "a pass throws", without touching
        // RoomRegistry: both sweeps read this clock as their very first statement.
        val poisonedClock = ServerClock {
            val before = remainingFailures.getAndUpdate { (it - 1).coerceAtLeast(0) }
            if (before > 0) error("SweepScheduleTest: simulated sweep failure")
            SystemClock.nowMillis()
        }
        val components = serverComponents(
            config,
            freshMigratedDatabase(),
            clock = poisonedClock,
            seeds = handSeedSource(HAND_SEED),
        )
        application { duelServer(components, config.sweepPeriodMillis) }

        // Created, and thus idle, before the clock is armed, so this call itself never throws.
        val room = components.socket.rooms.create(PlayerId("sweep-throws-host"))
        remainingFailures.set(INDUCED_FAILURES)

        withTimeout(5.seconds) {
            startApplication()
            while (components.socket.rooms.get(room.code) != null) {
                delay(POLL_INTERVAL)
            }
        }

        // The ticker kept running through every induced failure and reaped the room the moment
        // the clock recovered: a failing pass was retried, not fatal, exactly as ADR-0025 states.
        assertNull(components.socket.rooms.get(room.code))
    }
}
