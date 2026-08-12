package duels.poker.engine.game

import duels.poker.engine.card.cards
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoardSerializationTest {
    private val json = Json { prettyPrint = false }

    @Test
    fun anEmptyBoardRoundTrips() {
        val board = Board.EMPTY
        val encoded = json.encodeToString(Board.serializer(), board)
        val decoded = json.decodeFromString(Board.serializer(), encoded)
        assertEquals(board, decoded)
    }

    @Test
    fun aCompleteBoardRoundTrips() {
        val board = Board(cards("As Kh 2d 7c 9s"))
        val encoded = json.encodeToString(Board.serializer(), board)
        val decoded = json.decodeFromString(Board.serializer(), encoded)
        assertEquals(board, decoded)
    }

    @Test
    fun theJsonNamesEachCardInStandardNotation() {
        val board = Board(cards("As Kh 2d"))
        val encoded = json.encodeToString(Board.serializer(), board)
        assert(encoded.contains("As"))
        assert(encoded.contains("Kh"))
        assert(encoded.contains("2d"))
    }
}
