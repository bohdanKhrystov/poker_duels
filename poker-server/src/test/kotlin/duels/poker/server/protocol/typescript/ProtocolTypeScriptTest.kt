package duels.poker.server.protocol.typescript

import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.Rejection
import duels.poker.server.protocol.ClientMessage
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.PROTOCOL_VERSION
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.protocol.subtypeNames
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class ProtocolTypeScriptTest {

    @Test
    fun everyDeclarationAppearsExactlyOnce() {
        val names = protocolDeclarations().map { it.name }

        assertTrue(names.isNotEmpty(), "protocolDeclarations() must not be empty")
        assertEquals(names.distinct(), names, "every declaration name must appear exactly once")
    }

    @Test
    fun theTypesReachableOnlyThroughASecondListSurvive() {
        val names = protocolDeclarations().map { it.name }

        // DuelOutcome.finalStacks is a List walked before Events.events and PlayerView.seats. A
        // seen-set that treats a LIST descriptor as "seen" stops the walk from ever reaching a
        // second list's element type, silently dropping everything below.
        assertTrue(names.contains("GameEvent"), "GameEvent is reachable only past a second LIST descriptor")
        assertTrue(names.contains("SeatView"), "SeatView is reachable only past a second LIST descriptor")
        assertTrue(names.contains("HandRevealed"), "HandRevealed is a GameEvent variant reachable the same way")
        assertTrue(names.contains("StreetDealt"), "StreetDealt is a GameEvent variant reachable the same way")
    }

    @Test
    fun enumEntriesAreNotDeclaredAsTypes() {
        val names = protocolDeclarations().map { it.name }

        // Both enums are actually in the surface, so their absence below is a real absence.
        assertTrue(names.contains("ActionType"), "ActionType must be declared")
        assertTrue(names.contains("Street"), "Street must be declared")

        assertFalse(names.contains("FOLD"), "an ActionType entry must not become its own declaration")
        assertFalse(names.contains("ALL_IN"), "an ActionType entry must not become its own declaration")
        assertFalse(names.contains("PREFLOP"), "a Street entry must not become its own declaration")
        assertFalse(names.contains("COMPLETE"), "a Street entry must not become its own declaration")
    }

    @Test
    fun theWholeProtocolWalksWithoutAnUnsupportedKind() {
        val declarations = protocolDeclarations()

        assertTrue(declarations.isNotEmpty(), "the walk must produce declarations without throwing")
    }

    @Test
    fun theOrderIsStable() {
        val first = protocolDeclarations()
        val second = protocolDeclarations()

        assertEquals(first, second, "two calls must return equal lists")
        assertEquals(
            listOf("ClientMessage", "Act", "PlayerAction", "AllIn"),
            first.map { it.name }.take(4),
            "a union must precede its variants, in discovery order rather than sorted order",
        )
    }

    @Test
    fun aShortNameCollisionIsRefused() {
        val aThing = buildClassSerialDescriptor("a.Thing")
        val bThing = buildClassSerialDescriptor("b.Thing")

        val exception = assertFailsWith<IllegalStateException> {
            protocolDeclarations(listOf(aThing, bThing))
        }

        assertTrue(exception.message?.contains("a.Thing") == true, "message must name a.Thing")
        assertTrue(exception.message?.contains("b.Thing") == true, "message must name b.Thing")
    }

    @Test
    fun theHeaderNamesTheRegenerationCommand() {
        val output = protocolTypeScript()

        assertTrue(
            output.lines().first().contains("./gradlew :poker-server:generateProtocolTypes"),
            "first line must contain the regeneration command",
        )
        assertTrue(
            output.lines().first().contains("Generated"),
            "first line must contain the word 'Generated'",
        )
    }

    @Test
    fun theProtocolVersionIsATypeAlias() {
        val output = protocolTypeScript()

        assertTrue(
            output.contains("export type ProtocolVersion = $PROTOCOL_VERSION;"),
            "output must contain the version alias built from PROTOCOL_VERSION",
        )
    }

    @Test
    fun theFileUsesLfAndEndsWithASingleNewline() {
        val output = protocolTypeScript()

        assertFalse(output.contains("\r"), "output must not contain carriage returns")
        assertTrue(output.endsWith("\n"), "output must end with a newline")
        assertFalse(output.endsWith("\n\n"), "output must not end with double newlines")
    }

    @Test
    fun everyDeclarationReachesTheFile() {
        val output = protocolTypeScript()
        val declarations = protocolDeclarations()

        assertTrue(declarations.isNotEmpty(), "there must be declarations to test")
        declarations.forEach { declaration ->
            assertTrue(
                output.contains(declaration.text),
                "output must contain the declaration text for ${declaration.name}",
            )
        }
    }

    @Test
    fun everyMessageVariantDeclaresItsSerialNameAsItsDiscriminator() {
        val clientMessageNames = subtypeNames(ClientMessage.serializer().descriptor)
        val serverMessageNames = subtypeNames(ServerMessage.serializer().descriptor)
        val names = clientMessageNames + serverMessageNames
        val output = protocolTypeScript()

        assertTrue(names.isNotEmpty(), "there must be message variants to check")
        // CreateRoom is a `data object`: its whole declaration is a discriminator line, so an
        // emitter that only wrote discriminators for classes with fields would still pass without it.
        assertTrue(names.contains("CreateRoom"), "the variant names must include CreateRoom")

        names.forEach { name ->
            assertTrue(
                output.contains("  type: \"$name\";"),
                "output must declare \"$name\" as its discriminator literal",
            )
        }
    }

    @Test
    fun everySealedUnionListsExactlyItsSubtypes() {
        val sealedDescriptors = listOf(
            ClientMessage.serializer().descriptor,
            ServerMessage.serializer().descriptor,
            serializer<PlayerAction>().descriptor,
            serializer<Rejection>().descriptor,
            serializer<GameEvent>().descriptor,
        )
        val output = protocolTypeScript()

        // Count actual loop executions, not list size, so a walk that quietly lost a hierarchy
        // before this point cannot pass by iterating an empty set.
        var iterations = 0
        sealedDescriptors.forEach { descriptor ->
            iterations += 1
            val typeName = typeNameOf(descriptor)
            val union = subtypeNames(descriptor).joinToString(" | ")
            assertTrue(
                output.contains("export type $typeName = $union;"),
                "output must union $typeName over exactly its subtypes",
            )
        }
        assertEquals(5, iterations, "the loop must walk all five sealed hierarchies reachable in the protocol")
    }

    @Test
    fun theDiscriminatorKeyIsTheOneOnTheWire() {
        val encoded = protocolJson.encodeToString(
            ClientMessage.serializer(),
            Hello(deviceId = "d", protocolVersion = PROTOCOL_VERSION),
        )
        // Derive the key from the actual bytes rather than assuming "type": that way this test
        // fails if protocolJson's classDiscriminator and the emitted property name ever disagree.
        val key = protocolJson.parseToJsonElement(encoded).jsonObject.entries
            .single { it.value.jsonPrimitive.content == "Hello" }
            .key
        val output = protocolTypeScript()

        assertTrue(
            output.contains("$key: \"Hello\";"),
            "output must key the Hello discriminator as \"$key\", the key protocolJson writes on the wire",
        )
    }
}
