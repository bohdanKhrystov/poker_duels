package duels.poker.engine.game

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegalActionsSerializationTest {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun aFacingBetSetRoundTrips() {
        val original = LegalActions(
            0,
            setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE),
            callTo = 100,
            minRaiseTo = 200,
            allInTo = 1000,
        )
        val encoded = json.encodeToString(LegalActions.serializer(), original)
        val decoded = json.decodeFromString(LegalActions.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun anOpeningSetRoundTrips() {
        val original = LegalActions(
            1,
            setOf(ActionType.CHECK, ActionType.BET),
            minBetTo = 100,
            allInTo = 1000,
        )
        val encoded = json.encodeToString(LegalActions.serializer(), original)
        val decoded = json.decodeFromString(LegalActions.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun theEmptySetRoundTrips() {
        val original = LegalActions.none(0)
        val encoded = json.encodeToString(LegalActions.serializer(), original)
        val decoded = json.decodeFromString(LegalActions.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun defaultAmountsAreWrittenWhenDefaultsAreEncoded() {
        val legalActions = LegalActions.none(0)
        val encoded = json.encodeToString(LegalActions.serializer(), legalActions)
        assertTrue(encoded.contains("\"callTo\":0"), "encoded string should contain \"callTo\":0")
        assertTrue(encoded.contains("\"minBetTo\":0"), "encoded string should contain \"minBetTo\":0")
        assertTrue(encoded.contains("\"minRaiseTo\":0"), "encoded string should contain \"minRaiseTo\":0")
        assertTrue(encoded.contains("\"allInTo\":0"), "encoded string should contain \"allInTo\":0")
    }

    @Test
    fun defaultAmountsAreOmittedWithoutEncodeDefaults() {
        val legalActions = LegalActions.none(0)
        val jsonWithoutDefaults = Json { prettyPrint = false }
        val encoded = jsonWithoutDefaults.encodeToString(LegalActions.serializer(), legalActions)
        assertFalse(encoded.contains("callTo"), "encoded string should not contain callTo")
        assertFalse(encoded.contains("minBetTo"), "encoded string should not contain minBetTo")
        assertFalse(encoded.contains("minRaiseTo"), "encoded string should not contain minRaiseTo")
        assertFalse(encoded.contains("allInTo"), "encoded string should not contain allInTo")
    }
}
