package duels.poker.server.room

import duels.poker.server.config.ServerConfig
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val fixedSeeds = HandSeedSource { 7L }

/**
 * Given a [ServerConfig], build a [RoomRegistry] over a [MutableClock] with [config.roomTimeouts]
 * and [HandSeedSource { 7L }], and seat a host and a guest — so that a live decision is open and
 * expiry has a hand to fold at the instant it fires. Nobody needs to disconnect: a connected seat
 * that simply outlasts its own allowance is folded exactly the same way (`ADR-0113` §5).
 */
private suspend fun setupRoomWithTurnPlayer(config: ServerConfig): Triple<RoomRegistry, MutableClock, RoomCode> {
    val clock = MutableClock()
    val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, config.roomTimeouts(), seeds = fixedSeeds)
    val host = newPlayerId()
    val guest = newPlayerId()
    val room = registry.create(host)
    registry.join(room.code, guest)
    return Triple(registry, clock, room.code)
}

internal class TurnClockConfigTest {

    @Test
    fun theShippedDefaultsAreTheDeclaredOnes() {
        val config = ServerConfig.from(MapApplicationConfig()) { null }
        assertEquals(RoomTimeouts.DEFAULT_TURN_MILLIS, config.roomTimeouts().turnMillis)
        assertEquals(RoomTimeouts.DEFAULT_TIMEBANK_MILLIS, config.roomTimeouts().timebankMillis)
    }

    @Test
    fun aFiveSecondAllowanceExpiresAtFiveSeconds() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig(
                ServerConfig.TURN_MILLIS_KEY to "5000",
                ServerConfig.TIMEBANK_MILLIS_KEY to "1",
            ),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithTurnPlayer(config)

        // At 5000ms, the allowance has not run out yet
        clock.advance(5_000)
        val expiryBefore = registry.expireTurnClocks()
        assertEquals(emptyList<TurnClockExpiry>(), expiryBefore)
        val handLogBefore = registry.get(roomCode)!!.runner!!.hand!!.log.actions
        assertTrue(
            handLogBefore.none { it is duels.poker.engine.game.PlayerAction.Fold },
            "expected no fold at 5000ms",
        )

        // Advance to 5001ms — turnMillis(5000) + timebankMillis(1)
        clock.advance(1)
        val expiryAt = registry.expireTurnClocks()
        assertEquals(1, expiryAt.size)
        val handLogAt = registry.get(roomCode)!!.runner!!.log.hands.first().actions
        assertTrue(
            handLogAt.last() is duels.poker.engine.game.PlayerAction.Fold,
            "expected fold at 5001ms",
        )
    }

    @Test
    fun aFortyFiveSecondAllowanceExpiresAtFortyFiveSeconds() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig(
                // The split is reversed from aFiveSecondAllowanceExpiresAtFiveSeconds — most of
                // the allowance in the bank rather than the flat turn — so a fix that reads only
                // one of the two keys fails at least one of the pair.
                ServerConfig.TURN_MILLIS_KEY to "1",
                ServerConfig.TIMEBANK_MILLIS_KEY to "45000",
            ),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithTurnPlayer(config)

        // At 45000ms, the allowance has not run out yet
        clock.advance(45_000)
        val expiryBefore = registry.expireTurnClocks()
        assertEquals(emptyList<TurnClockExpiry>(), expiryBefore)
        val handLogBefore = registry.get(roomCode)!!.runner!!.hand!!.log.actions
        assertTrue(
            handLogBefore.none { it is duels.poker.engine.game.PlayerAction.Fold },
            "expected no fold at 45000ms",
        )

        // Advance to 45001ms — turnMillis(1) + timebankMillis(45000)
        clock.advance(1)
        val expiryAt = registry.expireTurnClocks()
        assertEquals(1, expiryAt.size)
        val handLogAt = registry.get(roomCode)!!.runner!!.log.hands.first().actions
        assertTrue(
            handLogAt.last() is duels.poker.engine.game.PlayerAction.Fold,
            "expected fold at 45001ms",
        )
    }

    @Test
    fun theEnvironmentAloneMovesTheAllowance() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig(),
        ) { name ->
            if (name == ServerConfig.TURN_MILLIS_ENV) "7000" else null
        }

        val (registry, clock, _) = setupRoomWithTurnPlayer(config)
        val deadline = 7_000L + RoomTimeouts.DEFAULT_TIMEBANK_MILLIS

        // One millisecond before turnMillis(7000, from the environment) + the default timebank,
        // the allowance has not run out
        clock.advance(deadline - 1)
        val expiryBefore = registry.expireTurnClocks()
        assertEquals(emptyList<TurnClockExpiry>(), expiryBefore)

        // Advance to the deadline
        clock.advance(1)
        val expiryAt = registry.expireTurnClocks()
        assertEquals(1, expiryAt.size)
    }

    @Test
    @Timeout(5)
    fun aLongAllowanceCostsNoRealTime() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig(
                ServerConfig.TURN_MILLIS_KEY to "1000",
                ServerConfig.TIMEBANK_MILLIS_KEY to "44000",
            ),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithTurnPlayer(config)

        // Advance through the entire 45-second allowance on MutableClock (instant)
        clock.advance(45_000)
        val expiries = registry.expireTurnClocks()

        // If this used real wall-clock time, it would timeout at 5 seconds
        // On MutableClock it completes instantly
        assertEquals(1, expiries.size)
        val handLogAt = registry.get(roomCode)!!.runner!!.log.hands.first().actions
        assertTrue(
            handLogAt.last() is duels.poker.engine.game.PlayerAction.Fold,
            "expected fold after 45000ms advance",
        )
    }

    @Test
    fun noServerTestWaitsOnRealTime() {
        val testSourceRoot = File("src/test/kotlin/duels/poker/server")

        // First assert the directory is non-empty to catch path errors early
        val files = testSourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(
            files.isNotEmpty(),
            "found no .kt files under ${testSourceRoot.path} — the scan would vacuously pass",
        )

        // Scan for real-time waiting calls
        val sleepMarker = "Thread" + "." + "sleep"
        val badFiles = files.filter { it.readText().contains(sleepMarker) }
        assertTrue(
            badFiles.isEmpty(),
            "expected no test file to contain waits on real time, found: ${badFiles.map { it.path }}",
        )
    }
}
