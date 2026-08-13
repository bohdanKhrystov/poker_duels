package duels.poker.server.protocol.typescript

import duels.poker.engine.game.Board
import duels.poker.engine.game.PlayerView
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.Hello
import kotlinx.serialization.ExperimentalSerializationApi
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
}
