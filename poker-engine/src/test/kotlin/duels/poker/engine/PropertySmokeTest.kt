package duels.poker.engine

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PropertySmokeTest {
    @Test
    fun propertyStackRuns() {
        // This test proves that kotest property testing works under the JUnit 5 platform.
        // We verify a trivial invariant: any integer is equal to itself.
        runBlocking {
            forAll(Arb.int()) { num ->
                num == num
            }
        }
    }
}
