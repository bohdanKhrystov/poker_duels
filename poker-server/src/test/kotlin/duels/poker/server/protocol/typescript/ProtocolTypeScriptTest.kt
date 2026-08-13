package duels.poker.server.protocol.typescript

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
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
}
