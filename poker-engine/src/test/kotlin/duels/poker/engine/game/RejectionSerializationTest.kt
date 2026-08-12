package duels.poker.engine.game

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RejectionSerializationTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun everyRejectionRoundTripsThroughTheParentSerializer() {
        val rejections = listOf(
            Rejection.NotYourTurn(1),
            Rejection.ActionNotAllowed(ActionType.BET, setOf(ActionType.FOLD, ActionType.CALL)),
            Rejection.AmountTooSmall(50, 100),
            Rejection.AmountTooLarge(9000, 1000),
            Rejection.HandComplete,
        )

        val encoded = json.encodeToString(ListSerializer(Rejection.serializer()), rejections)
        val decoded = json.decodeFromString(ListSerializer(Rejection.serializer()), encoded)

        assertEquals(rejections, decoded)
    }

    @Test
    fun nullSeatToActSurvives() {
        val rejection = Rejection.NotYourTurn(null)
        val encoded = json.encodeToString(Rejection.serializer(), rejection)
        val decoded = json.decodeFromString(Rejection.serializer(), encoded)

        assertEquals(rejection, decoded)
        assertEquals(null, decoded.let { (it as Rejection.NotYourTurn).seatToAct })
    }

    @Test
    fun theDiscriminatorsAreTheSimpleNames() {
        val rejections = listOf(
            Rejection.NotYourTurn(1),
            Rejection.ActionNotAllowed(ActionType.BET, setOf(ActionType.FOLD, ActionType.CALL)),
            Rejection.AmountTooSmall(50, 100),
            Rejection.AmountTooLarge(9000, 1000),
            Rejection.HandComplete,
        )
        val expectedDiscriminators = listOf(
            "NotYourTurn",
            "ActionNotAllowed",
            "AmountTooSmall",
            "AmountTooLarge",
            "HandComplete",
        )

        for ((rejection, expectedDiscriminator) in rejections.zip(expectedDiscriminators)) {
            val encoded = json.encodeToString(Rejection.serializer(), rejection)
            assert(encoded.contains(""""type":"$expectedDiscriminator"""")) {
                "Encoded should contain type discriminator \"$expectedDiscriminator\", but got: $encoded"
            }
        }
    }
}
