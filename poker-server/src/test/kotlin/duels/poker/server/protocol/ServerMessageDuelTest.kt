package duels.poker.server.protocol

import duels.poker.engine.game.ActionOn
import duels.poker.engine.game.PlayerBet
import duels.poker.engine.game.Rejection
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerMessageDuelTest {
    @Test
    fun snapshotRoundTrips() {
        val original = ServerMessage.Snapshot(sampleView())
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun eventsRoundTrip() {
        val original = ServerMessage.Events(listOf(ActionOn(3, 0), PlayerBet(4, 0, 300)))
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun yourTurnRoundTrips() {
        val original = ServerMessage.YourTurn(3, 7, sampleLegalActions())
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun rejectedRoundTrips() {
        val original = ServerMessage.Rejected(Rejection.NotYourTurn(1))
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun theDiscriminatorsAreExplicit() {
        val snapshot = ServerMessage.Snapshot(sampleView())
        val snapshotEncoded = protocolJson.encodeToString(ServerMessage.serializer(), snapshot)
        assertTrue(snapshotEncoded.contains("\"type\":\"Snapshot\""), "Snapshot should have explicit type discriminator: $snapshotEncoded")

        val events = ServerMessage.Events(listOf(ActionOn(3, 0), PlayerBet(4, 0, 300)))
        val eventsEncoded = protocolJson.encodeToString(ServerMessage.serializer(), events)
        assertTrue(eventsEncoded.contains("\"type\":\"Events\""), "Events should have explicit type discriminator: $eventsEncoded")

        val yourTurn = ServerMessage.YourTurn(3, 7, sampleLegalActions())
        val yourTurnEncoded = protocolJson.encodeToString(ServerMessage.serializer(), yourTurn)
        assertTrue(yourTurnEncoded.contains("\"type\":\"YourTurn\""), "YourTurn should have explicit type discriminator: $yourTurnEncoded")

        val rejected = ServerMessage.Rejected(Rejection.NotYourTurn(1))
        val rejectedEncoded = protocolJson.encodeToString(ServerMessage.serializer(), rejected)
        assertTrue(rejectedEncoded.contains("\"type\":\"Rejected\""), "Rejected should have explicit type discriminator: $rejectedEncoded")
    }

    @Test
    fun aSnapshotShowsOnlyTheViewersCards() {
        val view = sampleView()
        val snapshot = ServerMessage.Snapshot(view)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), snapshot)
        // Should contain viewer's cards
        assertTrue(encoded.contains("As"), "Encoded snapshot should contain viewer's As: $encoded")
        assertTrue(encoded.contains("Kh"), "Encoded snapshot should contain viewer's Kh: $encoded")
        // Should not contain opponent's cards (seat 1 has no cards in the sample)
        // Verify that we're not leaking the opponent's hidden cards
        // The sample has seat 1 with no hole cards, so any hidden cards would be a leak
        assertTrue(!encoded.contains("Qd"), "Encoded snapshot should not contain undealt Qd: $encoded")
    }

    @Test
    fun yourTurnCarriesItsZeroAmounts() {
        val yourTurn = ServerMessage.YourTurn(3, 7, sampleLegalActions())
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), yourTurn)
        // protocolJson has encodeDefaults = true, so minRaiseTo: 0 should appear
        assertTrue(encoded.contains("\"minRaiseTo\":0"), "Encoded YourTurn should contain minRaiseTo:0 with defaults: $encoded")
    }
}
