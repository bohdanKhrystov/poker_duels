package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Proves, through the generated wire descriptors rather than a reading of [ClientMessage] or
 * [ServerMessage]'s source, that every subtype's discriminator is an explicit `@SerialName`.
 *
 * The default serial name kotlinx.serialization assigns an unannotated subtype *is* its fully
 * qualified class name — so a subtype missing `@SerialName` shows up here as a name containing a
 * `.`, or as a name that is suspiciously long and starts with `duels.`. A subtype added to either
 * hierarchy tomorrow without an explicit `@SerialName` fails
 * [noDiscriminatorIsAFullyQualifiedClassName] before it ever reaches the wire.
 */
class ProtocolDiscriminatorTest {
    @Test
    fun theClientHierarchyIsExactlyTheDeclaredMessages() {
        val names = subtypeNames(ClientMessage.serializer().descriptor).toSet()

        assertEquals(setOf("Hello", "Act"), names)
    }

    @Test
    fun theServerHierarchyIsExactlyTheDeclaredMessages() {
        val names = subtypeNames(ServerMessage.serializer().descriptor).toSet()

        assertEquals(
            setOf("Welcome", "Failure", "Snapshot", "Events", "YourTurn", "Rejected"),
            names,
        )
    }

    @Test
    fun noDiscriminatorIsAFullyQualifiedClassName() {
        val names = subtypeNames(ClientMessage.serializer().descriptor) +
            subtypeNames(ServerMessage.serializer().descriptor)

        val leaked = names.filter { it.contains(".") || it.startsWith("duels.") }

        assertTrue(leaked.isEmpty()) { "Discriminator derived from a class name, not @SerialName: $leaked" }
    }

    @Test
    fun everyDiscriminatorIsShortAndUnique() {
        val names = subtypeNames(ClientMessage.serializer().descriptor) +
            subtypeNames(ServerMessage.serializer().descriptor)

        assertEquals(names.size, names.toSet().size) { "Discriminators must be distinct across both hierarchies: $names" }
        val tooLong = names.filter { it.length > 16 }
        assertTrue(tooLong.isEmpty()) { "Discriminator longer than 16 characters: $tooLong" }
    }

    @Test
    fun everySubtypeDescriptorCarriesItsOwnName() {
        for (parent in listOf(ClientMessage.serializer().descriptor, ServerMessage.serializer().descriptor)) {
            val names = subtypeNames(parent)
            val descriptorNames = subtypeDescriptors(parent).map { it.serialName }

            assertEquals(names, descriptorNames)
        }
    }
}
