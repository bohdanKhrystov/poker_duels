package duels.poker.server.protocol.typescript

import duels.poker.engine.game.ActionType
import duels.poker.engine.game.Board
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.Rejection
import duels.poker.server.protocol.ClientMessage
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.subtypeNames
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class TypeScriptDeclarationsTest {

    @Test
    fun aVariantCarriesItsDiscriminatorFirst() {
        val helloDescriptor = Hello.serializer().descriptor
        val result = interfaceDeclaration(helloDescriptor, "Hello")
        val expected = """
            export interface Hello {
              type: "Hello";
              deviceId: string | null;
              protocolVersion: number;
            }
        """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun aPlainClassHasNoDiscriminator() {
        val boardDescriptor = Board.serializer().descriptor
        val result = interfaceDeclaration(boardDescriptor)
        val expected = """
            export interface Board {
              cards: readonly string[];
            }
        """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun anObjectIsTheDiscriminatorAlone() {
        val createRoomDescriptor = CreateRoom.serializer().descriptor
        val result = interfaceDeclaration(createRoomDescriptor, "CreateRoom")
        val expected = """
            export interface CreateRoom {
              type: "CreateRoom";
            }
        """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun propertiesKeepDescriptorOrder() {
        val playerViewDescriptor = PlayerView.serializer().descriptor
        val result = interfaceDeclaration(playerViewDescriptor)

        // Assert the property lines as a list to verify order
        val lines = result.split("\n")
        val propertyLines = lines.drop(1).dropLast(1) // Remove "export interface" and closing "}"

        val expectedProperties = listOf(
            "  viewerSeat: number;",
            "  handNumber: number;",
            "  buttonSeat: number;",
            "  street: Street;",
            "  board: Board;",
            "  pot: number;",
            "  betToMatch: number;",
            "  minRaiseTo: number;",
            "  seatToAct: number | null;",
            "  smallBlind: number;",
            "  bigBlind: number;",
            "  seats: readonly SeatView[];",
        )

        assertEquals(expectedProperties, propertyLines)
        // Verify seatToAct is nullable and seats is readonly array
        assertEquals("  seatToAct: number | null;", propertyLines[8])
        assertEquals("  seats: readonly SeatView[];", propertyLines[11])
    }

    @Test
    fun anEnumIsAUnionOfItsEntryNames() {
        val descriptor = serializer<ActionType>().descriptor
        val result = enumDeclaration(descriptor)
        val expected = "export type ActionType = \"FOLD\" | \"CHECK\" | \"CALL\" | \"BET\" | \"RAISE\" | \"ALL_IN\";"
        assertEquals(expected, result)
    }

    @Test
    fun aSealedHierarchyIsAUnionOfItsVariants() {
        val descriptor = ClientMessage.serializer().descriptor
        val result = unionDeclaration(descriptor)
        val expected = "export type ClientMessage = Act | CreateRoom | Hello | JoinRoom;"
        assertEquals(expected, result)
    }

    @Test
    fun theVariantsComeFromTheDescriptor() {
        val descriptor = ServerMessage.serializer().descriptor
        val variantNames = sealedVariants(descriptor).map { it.first }
        val subTypeNames = subtypeNames(descriptor)
        assertEquals(subTypeNames, variantNames)
        assert(variantNames.isNotEmpty()) { "Variant names must not be empty" }
    }

    @Test
    fun aVariantDescriptorIsTheSubtypeNotTheValueSlot() {
        val descriptor = Rejection.serializer().descriptor
        val variants = sealedVariants(descriptor)

        // Find HandComplete and NotYourTurn
        val handCompleteVariant = variants.find { it.first == "HandComplete" }
        val notYourTurnVariant = variants.find { it.first == "NotYourTurn" }

        assert(handCompleteVariant != null) { "HandComplete variant not found" }
        assert(notYourTurnVariant != null) { "NotYourTurn variant not found" }

        // Check that HandComplete is an OBJECT
        assertEquals(StructureKind.OBJECT, handCompleteVariant!!.second.kind)

        // Check that NotYourTurn has a seatToAct element
        val notYourTurnDescriptor = notYourTurnVariant!!.second
        val elementNames = (0 until notYourTurnDescriptor.elementsCount)
            .map { notYourTurnDescriptor.getElementName(it) }
        assert(elementNames.contains("seatToAct")) { "NotYourTurn must have a seatToAct element" }
    }
}
