package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileDtosTest {
    @Test
    fun aProfileEncodesItsPlayerIdAndBalance() {
        val profile = profileResponse("p-1", 3)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals("""{"playerId":"p-1","coinBalance":3,"displayName":null}""", encoded)
    }

    @Test
    fun aNegativeBalanceSurvivesTheRoundTrip() {
        val profile = profileResponse("p-1", -3)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""coinBalance":-3"""))

        val decoded = protocolJson.decodeFromString(ProfileResponse.serializer(), encoded)
        assertEquals(-3, decoded.coinBalance)
        assertEquals(profile, decoded)
    }

    @Test
    fun aZeroCoinBalanceIsEncodedInTheJsonAndNotOmitted() {
        val profile = profileResponse("p-1", 0)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""coinBalance":0"""))
    }

    @Test
    fun aDuelSummaryRoundTripsEveryField() {
        val summary = duelSummaryResponse(
            duelId = "d-1",
            opponentPlayerId = "p-2",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 5,
            finishedAt = "2026-08-13T10:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        val decoded = protocolJson.decodeFromString(DuelSummaryResponse.serializer(), encoded)
        assertEquals(summary, decoded)
    }

    @Test
    fun aDrawnSummaryCarriesDrewAndAZeroDelta() {
        val summary = duelSummaryResponse(
            duelId = "d-2",
            opponentPlayerId = "p-3",
            outcome = DuelOutcomeLabel.DREW,
            coinDelta = 0,
            handsPlayed = 10,
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
        val summary = duelSummaryResponse(
            duelId = "d-3",
            opponentPlayerId = "p-4",
            outcome = DuelOutcomeLabel.DREW,
            coinDelta = 0,
            handsPlayed = 3,
            finishedAt = "2026-08-13T12:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""coinDelta":0"""))
    }

    @Test
    fun theHandCountSurvivesEncoding() {
        val summary = duelSummaryResponse(
            duelId = "d-4",
            opponentPlayerId = "p-5",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 0,
            finishedAt = "2026-08-13T13:00:00Z",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""handsPlayed":0"""))

        val decoded = protocolJson.decodeFromString(DuelSummaryResponse.serializer(), encoded)
        assertEquals(0, decoded.handsPlayed)
        assertEquals(summary, decoded)
    }

    @Test
    fun anEmptyRecentDuelsListEncodesAsAnEmptyArray() {
        val response = RecentDuelsResponse(emptyList(), nextCursor = null)
        val encoded = protocolJson.encodeToString(RecentDuelsResponse.serializer(), response)
        assertEquals("""{"duels":[],"nextCursor":null}""", encoded)
    }

    @Test
    fun aNextCursorEncodesAsTheStringItWasGiven() {
        val response = RecentDuelsResponse(emptyList(), nextCursor = "abc")
        val encoded = protocolJson.encodeToString(RecentDuelsResponse.serializer(), response)
        assertTrue(encoded.contains(""""nextCursor":"abc"""))
    }

    @Test
    fun theNextCursorIsOnTheWireAsNullWithoutEncodeDefaults() {
        val response = RecentDuelsResponse(emptyList(), nextCursor = null)
        val wireJson = Json { encodeDefaults = false }
        val encoded = wireJson.encodeToString(RecentDuelsResponse.serializer(), response)
        assertTrue(encoded.contains(""""nextCursor":null"""))
    }

    @Test
    fun aProfileEncodesTheNameItWasGiven() {
        val profile = profileResponse("p-1", 0, displayName = "Élodie")
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""displayName":"Élodie"""))
    }

    @Test
    fun anUnnamedProfileEncodesTheFieldAsNull() {
        val profile = profileResponse("p-1", 0, displayName = null)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertTrue(encoded.contains(""""displayName":null"""))
    }

    @Test
    fun aSetNameRequestDecodesItsName() {
        val decoded = protocolJson.decodeFromString(SetNameRequest.serializer(), """{"name":"bob"}""")
        assertEquals(SetNameRequest("bob"), decoded)
    }

    @Test
    fun aSetNameRequestDecodesAnotherName() {
        val decoded = protocolJson.decodeFromString(SetNameRequest.serializer(), """{"name":"alice"}""")
        assertEquals(SetNameRequest("alice"), decoded)
    }

    @Test
    fun aBodyWithNoNameIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SetNameRequest.serializer(), "{}")
        }
    }

    @Test
    fun aBodyWithAnUnknownFieldIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SetNameRequest.serializer(), """{"name":"bob","playerId":"p-1"}""")
        }
    }

    @Test
    fun aDuelSummaryEncodesTheOpponentNameItWasGiven() {
        val summary = duelSummaryResponse(
            duelId = "d-5",
            opponentPlayerId = "p-6",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 7,
            finishedAt = "2026-08-14T10:00:00Z",
            opponentDisplayName = "Ingrid",
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""opponentDisplayName":"Ingrid"""))

        val decoded = protocolJson.decodeFromString(DuelSummaryResponse.serializer(), encoded)
        assertEquals(summary, decoded)
    }

    @Test
    fun aDuelSummaryWithNoOpponentNameEncodesTheFieldAsNull() {
        val summary = duelSummaryResponse(
            duelId = "d-6",
            opponentPlayerId = "p-7",
            outcome = DuelOutcomeLabel.LOST,
            coinDelta = -1,
            handsPlayed = 3,
            finishedAt = "2026-08-14T11:00:00Z",
            opponentDisplayName = null,
        )
        val encoded = protocolJson.encodeToString(DuelSummaryResponse.serializer(), summary)
        assertTrue(encoded.contains(""""opponentDisplayName":null"""))
    }
}
