package duels.poker.server.room

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.Random

internal class RoomCodeSourceTest {

    @Test
    fun everyCodeIsEightCharactersFromTheAlphabet() {
        val source = RandomRoomCodeSource(Random(0))
        val pattern = Regex("^[${RoomCode.ALPHABET}]{${RoomCode.LENGTH}}$")
        repeat(1_000) {
            assertTrue(pattern.matches(source.newRoomCode().value))
        }
    }

    @Test
    fun theDefaultSourceIsSecure() {
        assertTrue(RandomRoomCodeSource().isSecure)
    }

    @Test
    fun aNonSecureInjectedSourceIsReportedAsSuch() {
        assertFalse(RandomRoomCodeSource(Random(1)).isSecure)
    }

    @Test
    fun anInjectedSourceIsTheOnlySourceOfRandomness() {
        val a = RandomRoomCodeSource(Random(42))
        val b = RandomRoomCodeSource(Random(42))
        repeat(5) {
            assertEquals(a.newRoomCode(), b.newRoomCode())
        }
    }

    @Test
    @Timeout(60)
    fun noDuplicateInOneHundredThousandDraws() {
        val source = RandomRoomCodeSource(Random(7))
        val codes = HashSet<RoomCode>()
        repeat(100_000) {
            codes.add(source.newRoomCode())
        }
        assertEquals(100_000, codes.size)
    }

    @Test
    fun mintsFromJavaRandomAndNeverTheEngineRng() {
        val fields = RandomRoomCodeSource::class.java.declaredFields
        assertFalse(fields.any { it.type.name.startsWith("duels.poker.engine") })
        assertTrue(fields.any { it.type == Random::class.java })
    }
}
