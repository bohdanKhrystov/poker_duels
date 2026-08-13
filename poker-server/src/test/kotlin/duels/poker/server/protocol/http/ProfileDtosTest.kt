package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileDtosTest {
    @Test
    fun aProfileEncodesItsPlayerIdAndBalance() {
        val profile = ProfileResponse("p-1", 3)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals("""{"playerId":"p-1","coinBalance":3}""", encoded)
    }

    @Test
    fun aNegativeBalanceSurvivesTheRoundTrip() {
        val profile = ProfileResponse("p-1", -3)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""coinBalance":-3"""))

        val decoded = protocolJson.decodeFromString(ProfileResponse.serializer(), encoded)
        assertEquals(-3, decoded.coinBalance)
        assertEquals(profile, decoded)
    }

    @Test
    fun aZeroCoinBalanceIsEncodedInTheJsonAndNotOmitted() {
        val profile = ProfileResponse("p-1", 0)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""coinBalance":0"""))
    }

    @Test
    fun aDuelSummaryRoundTripsEveryField() {
        val summary = DuelSummaryResponse(
            duelId = "d-1",
            opponentPlayerId = "p-2",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = null,
            finishedAt = "2026-08-13T10:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        val decoded = protocolJson.decodeFromString(DuelSummaryResponse.serializer(), encoded)
        assertEquals(summary, decoded)
    }

    @Test
    fun aNullHandsPlayedIsWrittenAsNull() {
        val summary = DuelSummaryResponse(
            duelId = "d-1",
            opponentPlayerId = "p-2",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = null,
            finishedAt = "2026-08-13T10:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""handsPlayed":null"""))
    }

    @Test
    fun aDrawnSummaryCarriesDrewAndAZeroDelta() {
        val summary = DuelSummaryResponse(
            duelId = "d-2",
            opponentPlayerId = "p-3",
            outcome = DuelOutcomeLabel.DREW,
            coinDelta = 0,
            handsPlayed = null,
            finishedAt = "2026-08-13T11:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        val decoded = protocolJson.decodeFromString(DuelSummaryResponse.serializer(), encoded)
        assertEquals(DuelOutcomeLabel.DREW, decoded.outcome)
        assertEquals(0, decoded.coinDelta)
        assertEquals(summary, decoded)
    }

    @Test
    fun aZeroCoinDeltaIsEncodedInTheJsonAndNotOmitted() {
        val summary = DuelSummaryResponse(
            duelId = "d-3",
            opponentPlayerId = "p-4",
            outcome = DuelOutcomeLabel.DREW,
            coinDelta = 0,
            handsPlayed = null,
            finishedAt = "2026-08-13T12:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""coinDelta":0"""))
    }

    @Test
    fun anEmptyRecentDuelsListEncodesAsAnEmptyArray() {
        val response = RecentDuelsResponse(emptyList())
        val encoded = protocolJson.encodeToString(RecentDuelsResponse.serializer(), response)
        assertEquals("""{"duels":[]}""", encoded)
    }
}
