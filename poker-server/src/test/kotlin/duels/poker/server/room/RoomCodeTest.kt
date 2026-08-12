package duels.poker.server.room

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RoomCodeTest {

    @Test
    fun acceptsAWellFormedCode() {
        val code = RoomCode("2B7KMNPQ")
        assertEquals("2B7KMNPQ", code.value)
    }

    @Test
    fun rejectsACodeOfTheWrongLength() {
        assertFailsWith<IllegalArgumentException> {
            RoomCode("2B7KMNP")
        }
        assertFailsWith<IllegalArgumentException> {
            RoomCode("2B7KMNPQR")
        }
    }

    @Test
    fun rejectsALetterOutsideTheAlphabet() {
        assertFailsWith<IllegalArgumentException> {
            RoomCode("IBCDEFGH")
        }
        assertFailsWith<IllegalArgumentException> {
            RoomCode("LBCDEFGH")
        }
        assertFailsWith<IllegalArgumentException> {
            RoomCode("OBCDEFGH")
        }
        assertFailsWith<IllegalArgumentException> {
            RoomCode("UBCDEFGH")
        }
    }

    @Test
    fun rejectsALowercaseCode() {
        assertFailsWith<IllegalArgumentException> {
            RoomCode("2b7kmnpq")
        }
    }

    @Test
    fun parseUppercasesAndStripsFormatting() {
        val parsed = RoomCode.parse(" 2b7k-mnpq ")
        val expected = RoomCode("2B7KMNPQ")
        assertEquals(expected, parsed)
    }

    @Test
    fun parseReturnsNullForRubbish() {
        assertNull(RoomCode.parse(""))
        assertNull(RoomCode.parse("hello"))
        assertNull(RoomCode.parse("IIIIIIII"))
    }

    @Test
    fun theAlphabetHasThirtyTwoUnambiguousSymbols() {
        assertEquals(32, RoomCode.ALPHABET.length)
        assertEquals(32, RoomCode.ALPHABET.toSet().size)
        assert(!RoomCode.ALPHABET.contains('I'))
        assert(!RoomCode.ALPHABET.contains('L'))
        assert(!RoomCode.ALPHABET.contains('O'))
        assert(!RoomCode.ALPHABET.contains('U'))
    }
}
