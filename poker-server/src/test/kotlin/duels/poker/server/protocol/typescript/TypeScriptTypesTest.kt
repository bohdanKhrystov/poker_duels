package duels.poker.server.protocol.typescript

import duels.poker.engine.card.CardSerializer
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.game.Board
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.Street
import duels.poker.server.protocol.Hello
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class TypeScriptTypesTest {

    @Test
    fun aCardIsAString() {
        val cardType = typeReference(CardSerializer.descriptor)
        assertEquals("string", cardType)
    }

    @Test
    fun primitivesMapToStringNumberAndBoolean() {
        assertEquals("string", typeReference(serializer<String>().descriptor))
        assertEquals("number", typeReference(serializer<Int>().descriptor))
        assertEquals("number", typeReference(serializer<Long>().descriptor))
        assertEquals("boolean", typeReference(serializer<Boolean>().descriptor))
    }

    @Test
    fun aNullableFieldIsUnionedWithNull() {
        val helloDescriptor = Hello.serializer().descriptor
        val deviceIdIndex = helloDescriptor.getElementIndex("deviceId")
        val deviceIdDescriptor = helloDescriptor.getElementDescriptor(deviceIdIndex)
        val deviceIdType = typeReference(deviceIdDescriptor)
        assertEquals("string | null", deviceIdType)

        val duelOutcomeDescriptor = DuelOutcome.serializer().descriptor
        val winnerIndex = duelOutcomeDescriptor.getElementIndex("winner")
        val winnerDescriptor = duelOutcomeDescriptor.getElementDescriptor(winnerIndex)
        val winnerType = typeReference(winnerDescriptor)
        assertEquals("number | null", winnerType)
    }

    @Test
    fun aListIsAReadonlyArray() {
        val boardDescriptor = Board.serializer().descriptor
        val cardsIndex = boardDescriptor.getElementIndex("cards")
        val cardsDescriptor = boardDescriptor.getElementDescriptor(cardsIndex)
        val cardsType = typeReference(cardsDescriptor)
        assertEquals("readonly string[]", cardsType)
    }

    @Test
    fun aSetIsAReadonlyArray() {
        val legalActionsDescriptor = LegalActions.serializer().descriptor
        val allowedIndex = legalActionsDescriptor.getElementIndex("allowed")
        val allowedDescriptor = legalActionsDescriptor.getElementDescriptor(allowedIndex)
        val allowedType = typeReference(allowedDescriptor)
        assertEquals("readonly ActionType[]", allowedType)
    }

    @Test
    fun aNamedTypeIsItsLastDottedSegment() {
        val playerViewType = typeReference(PlayerView.serializer().descriptor)
        assertEquals("PlayerView", playerViewType)

        val streetType = typeReference(serializer<Street>().descriptor)
        assertEquals("Street", streetType)
    }

    @Test
    fun aMapIsRefused() {
        val mapDescriptor = serializer<Map<String, Int>>().descriptor
        val exception = assertFailsWith<IllegalArgumentException> {
            typeReference(mapDescriptor)
        }
        assertEquals(true, exception.message?.contains("LinkedHashMap") ?: false)
    }
}
