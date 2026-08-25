package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileDtosTest {
    @Test
    fun aProfileEncodesItsPlayerIdAndBalance() {
        val profile = profileResponse("p-1", 3)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals(
            """{"playerId":"p-1","coinBalance":3,"displayName":null,"displayNameRemoved":false,"deviceRouteLive":true,"hasRecoveryEmail":false}""",
            encoded,
        )
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
    fun aRemovedNameIsCarriedOnTheProfile() {
        val profile = profileResponse("p-1", 0, displayName = null, displayNameRemoved = true)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals(
            """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":true,"deviceRouteLive":true,"hasRecoveryEmail":false}""",
            encoded,
        )

        val decoded = protocolJson.decodeFromString(ProfileResponse.serializer(), encoded)
        assertEquals(profile, decoded)
    }

    @Test
    fun aRevokedDeviceRouteIsCarriedOnTheProfile() {
        val profile = profileResponse("p-1", 0, deviceRouteLive = false)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals(
            """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":false,"deviceRouteLive":false,"hasRecoveryEmail":false}""",
            encoded,
        )

        val decoded = protocolJson.decodeFromString(ProfileResponse.serializer(), encoded)
        assertEquals(profile, decoded)
    }

    @Test
    fun aProfileWithRecoveryOnSaysSo() {
        val profile = profileResponse("p-1", 0, hasRecoveryEmail = true)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals(
            """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":false,"deviceRouteLive":true,"hasRecoveryEmail":true}""",
            encoded,
        )
    }

    @Test
    fun aProfileWithNoRecoveryEmailSaysSo() {
        val profile = profileResponse("p-1", 0, hasRecoveryEmail = false)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertEquals(
            """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":false,"deviceRouteLive":true,"hasRecoveryEmail":false}""",
            encoded,
        )
    }

    // Two independent checks, because they guard two different mistakes: a new field named for
    // an address (caught by the key scan below, whatever it is called) and the existing boolean
    // repurposed to carry the address itself under its own name (caught only by the "@" scan,
    // since that key is deliberately excluded from the name check).
    @Test
    fun theProfileNeverCarriesAnAddress() {
        val profile = profileResponse("p-1", 0, hasRecoveryEmail = true)
        val encoded = protocolJson.encodeToString(ProfileResponse.serializer(), profile)
        assertFalse(encoded.contains("@"))

        val keys = Json.parseToJsonElement(encoded).jsonObject.keys
        val suspectKeys = keys.filter { it != "hasRecoveryEmail" }
            .filter { it.contains("address", ignoreCase = true) || it.contains("email", ignoreCase = true) }
        assertEquals(emptyList<String>(), suspectKeys)
    }

    @Test
    fun aProfileWithoutTheFieldIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(
                ProfileResponse.serializer(),
                """{"playerId":"p-1","coinBalance":0,"displayName":null}""",
            )
        }
    }

    @Test
    fun aProfileWithoutTheDeviceRouteFieldIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(
                ProfileResponse.serializer(),
                """{"playerId":"p-1","coinBalance":0,"displayName":null,"displayNameRemoved":false}""",
            )
        }
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
