package duels.poker.server

import duels.poker.server.auth.ClaimPendingResult
import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryEmails
import duels.poker.server.auth.ResetRecipient
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.session.PlayerId
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How often the scheduled ticker sweeps; shrunk so every test here finishes quickly. */
private const val SWEEP_PERIOD_MILLIS = 20L

/** How long a room may sit idle before it is reapable; shrunk to match [SWEEP_PERIOD_MILLIS]. */
private const val WAITING_MILLIS = 60L

/**
 * The bounded wait [VerificationSweepTest.anExpiredClaimIsSweptWithoutBeingAsked] polls within
 * and [VerificationSweepTest.aLiveClaimSurvivesTheSweep] sleeps for outright — the same duration
 * either way, so "gone within this wait" and "still here after this wait" are directly
 * comparable.
 */
private val SWEEP_WAIT = 500.milliseconds

/** The polling interval for a bounded wait on database state — never a blind sleep. */
private val POLL_INTERVAL = 10.milliseconds

/**
 * Comfortably past a claimed row's lifetime: claiming under a clock backdated by this much always
 * produces an `expires_at` already behind Postgres's real `now()`.
 */
private val STALE_MARGIN: Duration = Duration.ofHours(25)

/**
 * Comfortably inside that same lifetime: claiming under a clock backdated by only this much
 * produces a row that is old, but not expired — the negative half of the pairing with
 * [STALE_MARGIN], and the reason this ticket writes two tests rather than one.
 */
private val LIVE_MARGIN: Duration = Duration.ofHours(1)

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
 * `TASK-041612`: `sweepPass` gains a third step that deletes expired `email_verification` rows
 * (`ADR-0031` §3), on the one ticker `ADR-0025` already runs — never a second coroutine. Every
 * test here boots the real module through [duelServer] and observes the database from the
 * outside; nothing calls [RecoveryEmails.deleteExpiredVerifications] directly.
 */
class VerificationSweepTest {
    @Test
    fun anExpiredClaimIsSweptWithoutBeingAsked(): Unit = testApplication {
        val config = shrunkServerConfig()
        val dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val clock = MutableClock(Instant.now())
        val components = serverComponents(config, dataSource, wallClock = clock)
        application { duelServer(components, config.sweepPeriodMillis) }

        val playerId = insertPlayer(dataSource)
        // Backdated before the claim, never advanced after: deleteExpiredVerifications reads
        // Postgres's own now(), so only a claim genuinely written in the past — never one written
        // now and compared against a moved-forward clock — produces a row that read will delete.
        clock.advance(STALE_MARGIN.negated())
        components.recoveryEmails.claimPending(
            playerId,
            EmailAddress("stale@x.test"),
            VerificationToken("stale-token"),
        )

        withTimeout(SWEEP_WAIT) {
            startApplication()
            while (countPendingRowsFor(dataSource, playerId) > 0) {
                delay(POLL_INTERVAL)
            }
        }

        // Nothing above called deleteExpiredVerifications directly — only the scheduled ticker
        // could have removed the row.
        assertEquals(0, countPendingRowsFor(dataSource, playerId))
    }

    @Test
    fun aLiveClaimSurvivesTheSweep(): Unit = testApplication {
        val config = shrunkServerConfig()
        val dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val clock = MutableClock(Instant.now())
        val components = serverComponents(config, dataSource, wallClock = clock)
        application { duelServer(components, config.sweepPeriodMillis) }

        val playerId = insertPlayer(dataSource)
        // An hour old, not twenty-five: still comfortably inside the claim's lifetime, the
        // negative half of the pairing with the expired row above. Restored afterwards since
        // nothing else in this test needs the backdated value.
        clock.advance(LIVE_MARGIN.negated())
        components.recoveryEmails.claimPending(
            playerId,
            EmailAddress("live@x.test"),
            VerificationToken("live-token"),
        )
        clock.advance(LIVE_MARGIN)

        startApplication()
        delay(SWEEP_WAIT)

        // A sweep with no WHERE clause at all would pass the test above and fail this one.
        assertEquals(1, countPendingRowsFor(dataSource, playerId))
    }

    @Test
    fun aFailingVerificationSweepDoesNotStopRoomReaping(): Unit = testApplication {
        val config = shrunkServerConfig()
        val dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val components = serverComponents(config, dataSource).copy(recoveryEmails = ThrowingRecoveryEmails)
        application { duelServer(components, config.sweepPeriodMillis) }

        // Created, and thus idle, before the ticker starts — the same shape as
        // SweepScheduleTest.aSweepThatThrowsDoesNotStopTheNextOne's room.
        val room = components.socket.rooms.create(PlayerId("verification-sweep-throws-host"))

        withTimeout(5.seconds) {
            startApplication()
            while (components.socket.rooms.get(room.code) != null) {
                delay(POLL_INTERVAL)
            }
        }

        // The stubbed port throws on every single pass; the room being gone is room reaping's
        // own effect, not merely the absence of an escaped exception.
        assertNull(components.socket.rooms.get(room.code))
    }

    private fun insertPlayer(dataSource: DataSource): PlayerId {
        val id = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, ?)",
            ).use { statement ->
                statement.setObject(1, id)
                statement.setInt(2, 100)
                statement.executeUpdate()
            }
        }
        return PlayerId(id.toString())
    }

    private fun countPendingRowsFor(dataSource: DataSource, playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM email_verification WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    // A Clock whose "now" a test can move before a claim is written, instead of reconstructing
    // it — byte-identical to PostgresRecoveryEmailsDeletesTest.MutableClock.
    private class MutableClock(instant: Instant) : Clock() {
        private var fixed: Clock = Clock.fixed(instant, ZoneOffset.UTC)

        override fun getZone(): ZoneId = fixed.zone

        override fun withZone(zone: ZoneId): Clock = fixed.withZone(zone)

        override fun instant(): Instant = fixed.instant()

        fun advance(duration: Duration) {
            fixed = Clock.fixed(fixed.instant().plus(duration), fixed.zone)
        }
    }

    // Every member throws, so aFailingVerificationSweepDoesNotStopRoomReaping fails the port
    // deterministically without touching the database — the port is stubbed, not the database.
    private object ThrowingRecoveryEmails : RecoveryEmails {
        override suspend fun claimPending(
            playerId: PlayerId,
            address: EmailAddress,
            token: VerificationToken,
        ): ClaimPendingResult = error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun verifyPending(token: VerificationToken): VerifyEmailResult =
            error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun hasRecoveryEmail(playerId: PlayerId): Boolean =
            error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun verifiedOwnerOf(address: EmailAddress): PlayerId? =
            error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun resetRecipientOf(address: EmailAddress): ResetRecipient? =
            error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun detach(playerId: PlayerId): Unit =
            error("ThrowingRecoveryEmails: not used by this test")

        override suspend fun deleteExpiredVerifications(): Int =
            error("VerificationSweepTest: simulated verification-sweep failure")
    }
}
