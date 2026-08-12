package duels.poker.server

import duels.poker.engine.card.Card
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PokerServerModuleTest {
    @Test
    fun theEngineIsOnTheModulesClasspath() {
        assertEquals(52, Card.all.size)
    }

    @Test
    fun ktorIsOnTheModulesClasspath() {
        assertEquals(200, HttpStatusCode.OK.value)
    }
}
