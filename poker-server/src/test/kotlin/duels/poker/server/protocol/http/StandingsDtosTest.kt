package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import duels.poker.server.season.Season
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StandingsDtosTest {
    @Test
    fun aRowEncodesEveryFieldWithItsNullDisplayName() {
        val row = StandingRow(rank = 1, playerId = "p-1", displayName = null, coins = 2)
        val encoded = protocolJson.encodeToString(StandingRow.serializer(), row)
        assertEquals(
            """{"rank":1,"playerId":"p-1","displayName":null,"coins":2}""",
            encoded,
        )
    }

    @Test
    fun aNegativeStandingSurvivesTheRoundTrip() {
        val row = StandingRow(rank = 1, playerId = "p-1", displayName = null, coins = -3)
        val encoded = protocolJson.encodeToString(StandingRow.serializer(), row)
        assertTrue(encoded.contains(""""coins":-3"""))

        val decoded = protocolJson.decodeFromString(StandingRow.serializer(), encoded)
        assertEquals(-3, decoded.coins)
        assertEquals(row, decoded)
    }

    @Test
    fun theResponseNamesItsSeasonInWireForm() {
        val season = Season(2026, 8)
        val response = StandingsResponse(
            season = season.toString(),
            rows = emptyList(),
            nextCursor = null,
            self = null,
        )
        val encoded = protocolJson.encodeToString(StandingsResponse.serializer(), response)
        assertTrue(encoded.contains(""""season":"2026-08"""))
    }

    @Test
    fun theLastPageCarriesNextCursorAsNullRatherThanOmittingIt() {
        val response = StandingsResponse(
            season = "2026-08",
            rows = emptyList(),
            nextCursor = null,
            self = null,
        )
        val encoded = protocolJson.encodeToString(StandingsResponse.serializer(), response)
        assertTrue(encoded.contains(""""nextCursor":null"""))
        assertTrue(encoded.contains(""""self":null"""))
    }

    @Test
    fun aSelfStandingIsBothNumbersOrNeither() {
        // Both null is valid
        val both_null = SelfStandingResponse("p-1", null, null)
        assertEquals("p-1", both_null.playerId)

        // Both non-null is valid
        val both_non_null = SelfStandingResponse("p-1", 3, 2)
        assertEquals(3, both_non_null.rank)
        assertEquals(2, both_non_null.coins)

        // Rank null, coins non-null is invalid
        assertThrows<IllegalArgumentException> {
            SelfStandingResponse("p-1", null, 2)
        }

        // Rank non-null, coins null is invalid
        assertThrows<IllegalArgumentException> {
            SelfStandingResponse("p-1", 3, null)
        }
    }

    @Test
    fun aSelfStandingOfZeroIsNotTheSameAsNoPlace() {
        val with_zero = SelfStandingResponse("p-1", 4, 0)
        val encoded = protocolJson.encodeToString(SelfStandingResponse.serializer(), with_zero)
        assertTrue(encoded.contains(""""coins":0"""))

        val decoded = protocolJson.decodeFromString(SelfStandingResponse.serializer(), encoded)
        assertEquals(with_zero, decoded)

        val no_place = SelfStandingResponse("p-1", null, null)
        assertNotEquals(with_zero, no_place)
    }
}
