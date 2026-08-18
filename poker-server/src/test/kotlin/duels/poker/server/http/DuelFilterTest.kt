package duels.poker.server.http

import duels.poker.server.protocol.http.DuelOutcomeLabel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DuelFilterTest {
    @Test
    fun everyOutcomeLabelParsesFromItsOwnName() {
        // The test is universal: it claims every entry parses. The assertion stops it passing
        // over an empty set.
        assert(DuelOutcomeLabel.entries.isNotEmpty())
        for (entry in DuelOutcomeLabel.entries) {
            assertEquals(entry, duelOutcomeOrNull(entry.name))
        }
    }

    @Test
    fun aLowerCaseOutcomeIsRefused() {
        assertNull(duelOutcomeOrNull("won"))
        assertNull(duelOutcomeOrNull("Drew"))
    }

    @Test
    fun anOutcomeThatIsNotALabelIsRefused() {
        assertNull(duelOutcomeOrNull("FOLDED"))
        assertNull(duelOutcomeOrNull("WON "))
        assertNull(duelOutcomeOrNull(""))
    }

    @Test
    fun noFilterNarrowsNeitherAxis() {
        assertEquals(null, DuelFilter.NONE.outcome)
        assertEquals(null, DuelFilter.NONE.opponent)
    }
}
