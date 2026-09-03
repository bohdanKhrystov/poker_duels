package duels.poker.server.protocol

import duels.poker.engine.game.ActionType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Constructs [ServerMessage.OpponentPresence], [ServerMessage.TurnClock] and
 * [ServerMessage.ActedForAbsent] directly and proves the `require` blocks each carries. It emits
 * nothing — no `Room`, `RoomRegistry` or `DuelSocket` behaviour is exercised here
 * (`TASK-021403`–`TASK-021409`).
 */
class PresenceFramesTest {
    @Test
    fun awayEncodesAndDecodes() {
        val presence: ServerMessage = ServerMessage.OpponentPresence(SeatPresence.AWAY)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), presence)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(presence, decoded)
    }

    @Test
    fun aTurnClockNamesASeatAtTheTable() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(2, 1, 0, 10_000L, listOf(60_000L, 60_000L))
        }
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(-1, 1, 0, 10_000L, listOf(60_000L, 60_000L))
        }
    }

    @Test
    fun aTurnClockRefusesANegativeDuration() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(0, 1, 0, -1L, listOf(60_000L, 60_000L))
        }
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(0, 1, 0, 10_000L, listOf(-1L, 60_000L))
        }
    }

    @Test
    fun aTurnClockNamesBothBanks() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(0, 1, 0, 10_000L, listOf(60_000L))
        }
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.TurnClock(0, 1, 0, 10_000L, listOf(60_000L, 60_000L, 60_000L))
        }

        val clock: ServerMessage = ServerMessage.TurnClock(0, 1, 0, 10_000L, listOf(60_000L, 45_000L))
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), clock)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(clock, decoded)
    }

    @Test
    fun theServerOnlyEverFoldsOrChecksForAnAbsentSeat() {
        ServerMessage.ActedForAbsent(0, 1, 0, ActionType.FOLD)
        ServerMessage.ActedForAbsent(1, 1, 0, ActionType.CHECK)

        for (action in listOf(ActionType.CALL, ActionType.BET, ActionType.RAISE, ActionType.ALL_IN)) {
            assertFailsWith<IllegalArgumentException> {
                ServerMessage.ActedForAbsent(0, 1, 0, action)
            }
        }
    }

    @Test
    fun aMarkNamesASeatAtTheTable() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.ActedForAbsent(2, 1, 0, ActionType.FOLD)
        }
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.ActedForAbsent(-1, 1, 0, ActionType.FOLD)
        }
    }
}
