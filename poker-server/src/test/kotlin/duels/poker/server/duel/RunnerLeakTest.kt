package duels.poker.server.duel

import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.HoleCardsDealt
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File

/**
 * Nothing secret ever leaves the runner: no frame addressed to one seat carries the other seat's
 * hole cards before that hand's reveal, and no encoded frame carries a hand seed.
 *
 * Every card-visibility assertion here is built from the [Addressed] frames [playDuel] hands
 * out — never from [DuelRunner]'s internal [duels.poker.engine.game.GameState]. The harness
 * deliberately never touches it: a client only ever sees frames, so a statement about what a
 * client can learn is only honest if it is built the same way.
 *
 * `RunnerChipConservationTest` proves the same boundary conserves chips; this class proves it
 * conserves secrets, which is the more consequential of the two.
 */
internal class RunnerLeakTest {
    // Use seeds whose decimal text cannot collide with sequence numbers, seats, hand numbers, chip
    // amounts or stacks. The distinctive 0x5EED prefix ensures the seed value is unambiguous in
    // the serialized output; collision avoidance is why they appear unusual rather than as 1L..20L.
    private val seeds = (0..19).map { 0x5EED_000000000001L + it }

    /**
     * Walks one seat's frames in hand order, tracking which seats have been revealed **in the
     * current hand only**: the set resets on every `HandStarted` and gains `event.seat` on every
     * `HandRevealed`. This mirrors exactly what a real client could compute from what it was
     * sent — nothing is read from a `GameState` or a log.
     *
     * @return for each frame index in [frames] that is a `Snapshot`, the set of seats revealed at
     *   that point in the hand.
     */
    private fun revealedAtEachSnapshot(frames: List<Addressed>): Map<Int, Set<Int>> {
        val result = mutableMapOf<Int, Set<Int>>()
        var revealed = mutableSetOf<Int>()
        frames.forEachIndexed { index, addressed ->
            when (val message = addressed.message) {
                is ServerMessage.Events -> {
                    message.events.forEach { event ->
                        when (event) {
                            is HandStarted -> revealed = mutableSetOf()
                            is HandRevealed -> revealed.add(event.seat)
                            else -> Unit
                        }
                    }
                }
                is ServerMessage.Snapshot -> result[index] = revealed.toSet()
                else -> Unit
            }
        }
        return result
    }

    @Test
    @Timeout(120)
    fun noSnapshotShowsTheOpponentsCardsBeforeAReveal() {
        for (seed in seeds) {
            val played = playDuel(seed)
            for (seat in 0..1) {
                val seatFrames = played.outbound.filter { it.seat == seat }
                val revealedByIndex = revealedAtEachSnapshot(seatFrames)
                seatFrames.forEachIndexed { index, addressed ->
                    val message = addressed.message
                    if (message is ServerMessage.Snapshot) {
                        val opponent = 1 - seat
                        val revealed = revealedByIndex.getValue(index)
                        val opponentCards = message.view.seats[opponent].holeCards
                        if (opponent !in revealed) {
                            assertTrue(
                                opponentCards.isEmpty(),
                                "seed $seed: seat $seat's snapshot at frame $index shows opponent " +
                                    "(seat $opponent) holeCards $opponentCards before any reveal",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun aSeatAlwaysSeesItsOwnCards() {
        for (seed in seeds) {
            val played = playDuel(seed)
            for (seat in 0..1) {
                val seatFrames = played.outbound.filter { it.seat == seat }
                seatFrames.forEachIndexed { index, addressed ->
                    val message = addressed.message
                    if (message is ServerMessage.Snapshot) {
                        val ownCards = message.view.seats[seat].holeCards
                        assertTrue(
                            ownCards.size == 2,
                            "seed $seed: seat $seat's snapshot at frame $index shows ${ownCards.size} " +
                                "own hole cards, expected 2",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun noEventFrameCarriesTheOpponentsHoleCardsDealt() {
        for (seed in seeds) {
            val played = playDuel(seed)
            for (seat in 0..1) {
                val opponent = 1 - seat
                played.outbound.filter { it.seat == seat }.forEachIndexed { index, addressed ->
                    val message = addressed.message
                    if (message is ServerMessage.Events) {
                        val leaked = message.events.filterIsInstance<HoleCardsDealt>().filter { it.seat == opponent }
                        assertTrue(
                            leaked.isEmpty(),
                            "seed $seed: seat $seat's Events frame at frame $index carries " +
                                "HoleCardsDealt for opponent (seat $opponent): $leaked",
                        )
                    }
                }
            }
        }
    }

    /**
     * Searches encoded frame strings for any hand seed's decimal text.
     *
     * @param encodedFrames Pre-encoded frame strings to search.
     * @param handSeeds All hand seeds from the duel (including hand 1).
     * @return true if any encoded frame contains any hand seed's decimal text.
     */
    private fun frameContainsAnySeed(
        encodedFrames: List<String>,
        handSeeds: List<Long>,
    ): Boolean {
        encodedFrames.forEach { encoded ->
            handSeeds.forEach { handSeed ->
                if (encoded.contains(handSeed.toString())) {
                    return true // Found a leak
                }
            }
        }
        return false // No leaks found
    }

    @Test
    @Timeout(120)
    fun noEncodedFrameContainsAHandSeed() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val handSeeds = played.runner.log.hands.map { it.seed }

            // Defensive assertions: ensure the collections are not empty, else this test proves nothing
            assertTrue(
                played.outbound.isNotEmpty(),
                "seed $seed: outbound frames collection is empty, cannot verify no seeds leak",
            )
            assertTrue(
                handSeeds.isNotEmpty(),
                "seed $seed: hand seeds collection is empty, including hand 1's seed",
            )

            val encodedFrames = played.outbound.map { ProtocolCodec.encode(it.message) }
            assertFalse(
                frameContainsAnySeed(encodedFrames, handSeeds),
                "seed $seed: encoded frames contain a hand seed's decimal text",
            )
        }
    }

    @Test
    @Timeout(120)
    fun theLeakCheckDetectsASeedWhenOneIsPresent() {
        // Positive control: proves the leak detection logic actually works by verifying it catches
        // a deliberately leaked seed. Production code cannot easily be made to leak a seed due to
        // architectural constraints (test encodes only ServerMessage, PlayerView fields are 32-bit
        // Int, seed values are 64-bit Long). This test keeps the negative assertion honest.
        for (seed in seeds) {
            val played = playDuel(seed)
            val handSeeds = played.runner.log.hands.map { it.seed }

            // Ensure we have seeds to leak (especially hand 1's seed, which was previously excluded)
            assertTrue(
                handSeeds.isNotEmpty(),
                "seed $seed: no hand seeds available to test detection",
            )

            // Create a list of encoded frames with one hand seed deliberately appended to the first
            val leakedSeed = handSeeds.first() // Use the first seed (could be hand 1's)
            val encodedFrames = played.outbound.mapIndexed { index, addressed ->
                val encoded = ProtocolCodec.encode(addressed.message)
                if (index == 0) encoded + leakedSeed.toString() else encoded
            }

            // Use the same detection helper as the real test — if it has a bug, this control will
            // catch it by failing. Verify the helper finds the deliberately leaked seed.
            assertTrue(
                frameContainsAnySeed(encodedFrames, handSeeds),
                "seed $seed: leak detector failed to find deliberately leaked hand seed $leakedSeed " +
                    "using frameContainsAnySeed() — the control is not exercising the real helper",
            )
        }
    }

    /** The module's server source root, resolved with Gradle's per-module working directory. */
    private val serverSourceRoot = File("src/main/kotlin/duels/poker/server")

    /** Every `.kt` file under [serverSourceRoot], asserted non-empty so a bad path fails loudly. */
    private fun serverSourceFiles(): List<File> {
        val files = serverSourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(
            files.isNotEmpty(),
            "found no .kt files under ${serverSourceRoot.path} (resolved from " +
                "${serverSourceRoot.absolutePath}) — the scan below would vacuously pass",
        )
        return files
    }

    @Test
    fun noServerSourceFileTouchesHoleCards() {
        val offending = serverSourceFiles().filter { it.readText().contains("holeCards") }
        assertTrue(
            offending.isEmpty(),
            "expected no server source file to contain \"holeCards\", found: ${offending.map { it.path }}",
        )
    }

    @Test
    fun onlyTheBroadcastFileBuildsAStateCarryingFrame() {
        val files = serverSourceFiles()
        val allowed = "duel${File.separatorChar}Addressed.kt"

        val needles = listOf("PlayerView.of(", "ServerMessage.Snapshot(", "ServerMessage.Events(", "ServerMessage.DuelFinished(")
        needles.forEach { needle ->
            val containing = files.filter { it.readText().contains(needle) }
            val offending = containing.filterNot { it.path.endsWith(allowed) }
            assertTrue(
                containing.isNotEmpty(),
                "expected at least one server source file to contain \"$needle\", found none " +
                    "under ${serverSourceRoot.path}",
            )
            assertTrue(
                offending.isEmpty(),
                "expected only duel/Addressed.kt to contain \"$needle\", also found in: " +
                    offending.map { it.path },
            )
        }
    }
}
