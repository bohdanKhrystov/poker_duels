package duels.poker.engine.game

import duels.poker.engine.card.cards
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Proves, through the generated wire descriptor rather than a reading of [PlayerView]'s source,
 * that a view can never carry a secret: no field named after [GameState.deck], [GameState.rng]
 * or a seed, no field typed as one of those, and no card the viewer is not entitled to. A future
 * field that reintroduces a secret fails one of these tests structurally, before it ever reaches
 * a client.
 */
class PlayerViewSerializationTest {
    private val json = Json { prettyPrint = false }

    private val forbiddenNames = setOf("deck", "rng", "seed")

    private fun sampleView(): PlayerView {
        val seat0 = Seat(index = 0, stack = START_STACK, holeCards = cards("As Kh"))
        val seat1 = Seat(index = 1, stack = START_STACK, holeCards = cards("Qd Jc"))
        val state = handState(seats = listOf(seat0, seat1)).copy(
            street = Street.FLOP,
            board = Board(cards("2c 7d 9s")),
        )
        return PlayerView.of(state, seat = 0)
    }

    /** Every element name reachable from [descriptor], depth-first, itself and its children. */
    private fun allElementNames(descriptor: SerialDescriptor): List<String> {
        val names = mutableListOf<String>()
        fun visit(d: SerialDescriptor) {
            for (i in 0 until d.elementsCount) {
                names += d.getElementName(i)
                visit(d.getElementDescriptor(i))
            }
        }
        visit(descriptor)
        return names
    }

    /** Every element descriptor reachable from [descriptor], depth-first, itself and its children. */
    private fun allElementDescriptors(descriptor: SerialDescriptor): List<SerialDescriptor> {
        val descriptors = mutableListOf<SerialDescriptor>()
        fun visit(d: SerialDescriptor) {
            for (i in 0 until d.elementsCount) {
                val child = d.getElementDescriptor(i)
                descriptors += child
                visit(child)
            }
        }
        visit(descriptor)
        return descriptors
    }

    @Test
    fun noFieldOfAViewIsNamedDeckRngOrSeed() {
        val names = allElementNames(PlayerView.serializer().descriptor)

        val leaked = names.filter { it.lowercase() in forbiddenNames }

        assertTrue(leaked.isEmpty()) { "PlayerView's wire shape names a secret field: $leaked" }
    }

    @Test
    fun noFieldOfAViewHasADeckOrRngType() {
        val serialNames = allElementDescriptors(PlayerView.serializer().descriptor).map { it.serialName }

        val leaked = serialNames.filter { it.contains("Deck") || it.contains("Rng") }

        assertTrue(leaked.isEmpty()) { "PlayerView's wire shape carries a secret type: $leaked" }
    }

    @Test
    fun theViewsFieldNamesAreExactlyTheTwelveDeclared() {
        val descriptor = PlayerView.serializer().descriptor
        val names = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

        assertEquals(
            listOf(
                "viewerSeat",
                "handNumber",
                "buttonSeat",
                "street",
                "board",
                "pot",
                "betToMatch",
                "minRaiseTo",
                "seatToAct",
                "smallBlind",
                "bigBlind",
                "seats",
            ),
            names,
        )
    }

    @Test
    fun aViewRoundTripsThroughJson() {
        val view = sampleView()

        val serialized = json.encodeToString(PlayerView.serializer(), view)
        val deserialized = json.decodeFromString(PlayerView.serializer(), serialized)

        assertEquals(view, deserialized)
    }

    @Test
    fun theJsonCarriesTheViewersOwnCards() {
        val serialized = json.encodeToString(PlayerView.serializer(), sampleView())

        assertTrue(serialized.contains("As"))
        assertTrue(serialized.contains("Kh"))
    }

    @Test
    fun theJsonCarriesNoOpponentCard() {
        val serialized = json.encodeToString(PlayerView.serializer(), sampleView())

        assertFalse(serialized.contains("Qd"))
        assertFalse(serialized.contains("Jc"))
    }

    @Test
    fun theJsonCarriesTheBoard() {
        val serialized = json.encodeToString(PlayerView.serializer(), sampleView())

        assertTrue(serialized.contains("2c"))
        assertTrue(serialized.contains("7d"))
        assertTrue(serialized.contains("9s"))
    }
}
