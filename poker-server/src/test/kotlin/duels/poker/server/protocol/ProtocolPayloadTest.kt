package duels.poker.server.protocol

import duels.poker.engine.game.PlayerAction
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `GameState`, `Deck` and `Rng` carry no `@Serializable` annotation, so a `ServerMessage` or
 * `ClientMessage` subtype declaring one of them would not compile — that guarantee is the
 * compiler's, and needs no test here.
 *
 * What the compiler cannot see is an `Int` that is really a seed (or named like one) going out
 * to a client, a `Card` coming in on a `ClientMessage` — a client stating a card is asserting a
 * game fact, which ADR-0002 forbids — or a field copied by hand that reintroduces something
 * secret. This file walks the serial descriptors structurally to catch exactly those.
 */
class ProtocolPayloadTest {

    private val cardSerialName = "duels.poker.engine.card.Card"

    /**
     * `descriptor` and everything reachable through its elements, depth-first.
     *
     * Guards recursion with [seen] rather than a depth limit: a primitive descriptor throws if
     * asked for `getElementDescriptor`, so recursion only happens when `elementsCount > 0`, and a
     * descriptor already visited (by serial name) is not walked twice.
     */
    private fun walk(descriptor: SerialDescriptor, seen: MutableSet<String> = mutableSetOf()): List<SerialDescriptor> {
        if (!seen.add(descriptor.serialName)) return emptyList()
        val children = if (descriptor.elementsCount > 0) {
            (0 until descriptor.elementsCount).flatMap { walk(descriptor.getElementDescriptor(it), seen) }
        } else {
            emptyList()
        }
        return listOf(descriptor) + children
    }

    /** Every element name reachable from `descriptor`, collected over the same walk. */
    private fun elementNames(descriptor: SerialDescriptor): List<String> =
        walk(descriptor).flatMap { d -> (0 until d.elementsCount).map(d::getElementName) }

    @Test
    fun noServerMessageNamesADeckAnRngOrASeed() {
        val forbidden = setOf("deck", "rng", "seed", "holecardsof", "deckremaining")
        subtypeDescriptors(serializer<ServerMessage>().descriptor).forEach { subtype ->
            elementNames(subtype).forEach { name ->
                assertFalse(
                    name.lowercase() in forbidden,
                    "ServerMessage subtype ${subtype.serialName} names a forbidden field: $name",
                )
            }
        }
    }

    @Test
    fun noServerMessageCarriesAStateDeckOrRngType() {
        val forbidden = listOf("GameState", "Deck", "Rng")
        subtypeDescriptors(serializer<ServerMessage>().descriptor).forEach { subtype ->
            walk(subtype).forEach { descriptor ->
                forbidden.forEach { name ->
                    assertFalse(
                        descriptor.serialName.contains(name),
                        "ServerMessage subtype ${subtype.serialName} reaches a $name-shaped " +
                            "descriptor: ${descriptor.serialName}",
                    )
                }
            }
        }
    }

    @Test
    fun noClientMessageCarriesACard() {
        subtypeDescriptors(serializer<ClientMessage>().descriptor).forEach { subtype ->
            walk(subtype).forEach { descriptor ->
                assertFalse(
                    descriptor.serialName == cardSerialName,
                    "ClientMessage subtype ${subtype.serialName} carries a Card",
                )
            }
        }
    }

    @Test
    fun noClientMessageNamesAChipOrStateField() {
        val forbidden = setOf(
            "stack", "pot", "card", "cards", "holecards", "board", "view", "bettomatch", "seattoact",
        )
        subtypeDescriptors(serializer<ClientMessage>().descriptor).forEach { subtype ->
            elementNames(subtype).forEach { name ->
                assertFalse(
                    name.lowercase() in forbidden,
                    "ClientMessage subtype ${subtype.serialName} names a forbidden field: $name",
                )
            }
        }
    }

    @Test
    fun theOnlyStateAServerMessageCarriesIsAPlayerView() {
        val names = subtypeNames(serializer<ServerMessage>().descriptor)
        val descriptors = subtypeDescriptors(serializer<ServerMessage>().descriptor)
        // Vacuity guard: ensure we are actually inspecting messages and not walking an empty set.
        // The exact count is pinned by ProtocolDocumentationTest against docs/protocol.md.
        assertTrue(descriptors.isNotEmpty(), "descriptors should not be empty")

        val carriers = names.zip(descriptors).filter { (_, descriptor) ->
            (0 until descriptor.elementsCount).any {
                descriptor.getElementDescriptor(it).serialName.endsWith("PlayerView")
            }
        }

        assertEquals(listOf("Snapshot"), carriers.map { it.first })
        val (_, snapshot) = carriers.single()
        assertEquals(1, snapshot.elementsCount)
    }

    @Test
    fun aClientMessagesOnlyEngineTypeIsAPlayerAction() {
        val playerActionFamily = walk(serializer<PlayerAction>().descriptor).map { it.serialName }.toSet()

        subtypeDescriptors(serializer<ClientMessage>().descriptor).forEach { subtype ->
            walk(subtype).forEach { descriptor ->
                if (descriptor.serialName.startsWith("duels.poker.engine")) {
                    assertTrue(
                        descriptor.serialName in playerActionFamily,
                        "ClientMessage subtype ${subtype.serialName} carries an engine type " +
                            "that isn't a PlayerAction: ${descriptor.serialName}",
                    )
                }
            }
        }
    }
}
