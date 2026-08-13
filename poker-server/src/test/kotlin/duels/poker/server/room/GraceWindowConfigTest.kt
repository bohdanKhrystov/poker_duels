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
 * and [HandSeedSource { 7L }], seat a host and a guest, and disconnect the player whose seat is
 * on turn — so that expiry has a hand to fold at the instant it fires.
 */
private suspend fun setupRoomWithDisconnectedTurnPlayer(config: ServerConfig): Triple<RoomRegistry, MutableClock, RoomCode> {
    val clock = MutableClock()
    val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, config.roomTimeouts(), seeds = fixedSeeds)
    val host = newPlayerId()
    val guest = newPlayerId()
    val room = registry.create(host)
    registry.join(room.code, guest)
    val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
    val onTurnPlayer = if (onTurn == 0) host else guest
    registry.disconnect(room.code, onTurnPlayer)
    return Triple(registry, clock, room.code)
}

internal class GraceWindowConfigTest {

    @Test
    fun theShippedDefaultIsTheDeclaredOne() {
        val config = ServerConfig.from(MapApplicationConfig()) { null }
        assertEquals(
            RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS,
            config.roomTimeouts().disconnectGraceMillis,
        )
    }

    @Test
    fun aFiveSecondWindowFoldsAtFiveSeconds() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig("duel.disconnectGraceMillis" to "5000"),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithDisconnectedTurnPlayer(config)

        // At 4999ms, the window has not expired yet
        clock.advance(4_999)
        val expiryBefore = registry.expireGracePeriods()
        assertEquals(emptyList<GraceExpiry>(), expiryBefore)
        val handLogBefore = registry.get(roomCode)!!.runner!!.hand!!.log.actions
        assertTrue(
            handLogBefore.none { it is duels.poker.engine.game.PlayerAction.Fold },
            "expected no fold at 4999ms",
        )

        // Advance to exactly 5000ms (total advance from start is 5000)
        clock.advance(1)
        val expiryAt = registry.expireGracePeriods()
        assertEquals(1, expiryAt.size)
        val handLogAt = registry.get(roomCode)!!.runner!!.log.hands.first().actions
        assertTrue(
            handLogAt.last() is duels.poker.engine.game.PlayerAction.Fold,
            "expected fold at 5000ms",
        )
    }

    @Test
    fun aFortyFiveSecondWindowFoldsAtFortyFiveSeconds() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig("duel.disconnectGraceMillis" to "45000"),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithDisconnectedTurnPlayer(config)

        // At 5000ms, the window has not expired
        clock.advance(5_000)
        val expiryBefore = registry.expireGracePeriods()
        assertEquals(emptyList<GraceExpiry>(), expiryBefore)
        val handLogBefore = registry.get(roomCode)!!.runner!!.hand!!.log.actions
        assertTrue(
            handLogBefore.none { it is duels.poker.engine.game.PlayerAction.Fold },
            "expected no fold at 5000ms with 45s window",
        )

        // Advance to 45000ms (total advance is 45000)
        clock.advance(40_000)
        val expiryAt = registry.expireGracePeriods()
        assertEquals(1, expiryAt.size)
        val handLogAt = registry.get(roomCode)!!.runner!!.log.hands.first().actions
        assertTrue(
            handLogAt.last() is duels.poker.engine.game.PlayerAction.Fold,
            "expected fold at 45000ms",
        )
    }

    @Test
    fun theEnvironmentAloneMovesTheWindow() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig(),
        ) { name ->
            if (name == ServerConfig.DISCONNECT_GRACE_MILLIS_ENV) "7000" else null
        }

        val (registry, clock, _) = setupRoomWithDisconnectedTurnPlayer(config)

        // At 6999ms, the window has not expired
        clock.advance(6_999)
        val expiryBefore = registry.expireGracePeriods()
        assertEquals(emptyList<GraceExpiry>(), expiryBefore)

        // Advance to 7000ms
        clock.advance(1)
        val expiryAt = registry.expireGracePeriods()
        assertEquals(1, expiryAt.size)
    }

    @Test
    @Timeout(5)
    fun aLongWindowCostsNoRealTime() = runBlocking {
        val config = ServerConfig.from(
            MapApplicationConfig("duel.disconnectGraceMillis" to "45000"),
        ) { null }

        val (registry, clock, roomCode) = setupRoomWithDisconnectedTurnPlayer(config)

        // Advance through the entire 45-second window on MutableClock (instant)
        clock.advance(45_000)
        val expiries = registry.expireGracePeriods()

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
