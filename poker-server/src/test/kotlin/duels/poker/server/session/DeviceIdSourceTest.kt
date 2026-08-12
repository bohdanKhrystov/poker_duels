package duels.poker.server.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.Random

class DeviceIdSourceTest {
    @Test
    @Timeout(60)
    fun issuesUniqueIdsAcrossOneHundredThousand() {
        val source = RandomDeviceIdSource()
        val ids = (0 until 100_000).map { source.newDeviceId() }.toHashSet()
        assertEquals(100_000, ids.size)
    }

    @Test
    fun everyIdIsTwentyTwoUrlSafeCharacters() {
        val source = RandomDeviceIdSource()
        val pattern = Regex("^[A-Za-z0-9_-]{22}$")
        repeat(1_000) {
            val id = source.newDeviceId()
            assertTrue(pattern.matches(id.value), "ID '${id.value}' does not match URL-safe pattern")
        }
    }

    @Test
    fun theDefaultSourceIsSecure() {
        val source = RandomDeviceIdSource()
        assertTrue(source.isSecure)
    }

    @Test
    fun anInjectedSourceIsTheOnlySourceOfRandomness() {
        val source1 = RandomDeviceIdSource(Random(42))
        val source2 = RandomDeviceIdSource(Random(42))
        repeat(5) {
            assertEquals(source1.newDeviceId(), source2.newDeviceId())
        }
    }

    @Test
    fun aNonSecureInjectedSourceIsReportedAsSuch() {
        val source = RandomDeviceIdSource(Random(1))
        assertEquals(false, source.isSecure)
    }
}
