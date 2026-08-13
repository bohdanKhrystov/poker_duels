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
    private val seeds = 1L..20L

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

    @Test
    @Timeout(120)
    fun noEncodedFrameContainsAHandSeed() {
        for (seed in seeds) {
            val played = playDuel(seed)
            // The opening hand's seed is, by playDuel's own construction (startDuel receives
            // `seed` directly, per DuelStart.kt's `openHand`), exactly the outer test parameter
            // above — already public within this run, printed in every failure message here, and
            // never independently drawn. It is excluded from the check for that reason, not
            // because it is unimportant: with seeds 1L..20L, it is small enough to coincide with
            // an ordinary field (a `sequence`, `seat` or `handNumber` value) purely by numeric
            // accident, which is not evidence of a leak. Every other hand's seed comes from the
            // harness's own independent SplitMix64Rng-backed HandSeedSource and is checked in full.
            val handSeeds = played.runner.log.hands.map { it.seed }.filter { it != seed }
            played.outbound.forEachIndexed { index, addressed ->
                val encoded = ProtocolCodec.encode(addressed.message)
                handSeeds.forEach { handSeed ->
                    assertFalse(
                        encoded.contains(handSeed.toString()),
                        "seed $seed: encoded frame $index to seat ${addressed.seat} contains the " +
                            "decimal text of hand seed $handSeed: $encoded",
                    )
                }
            }
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

        val needles = listOf("PlayerView.of(", "ServerMessage.Snapshot(", "ServerMessage.Events(")
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
