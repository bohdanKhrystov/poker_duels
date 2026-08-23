package duels.poker.server.protocol

import duels.poker.engine.game.ActionType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Constructs [ServerMessage.OpponentPresence] and [ServerMessage.ActedForAbsent] directly and
 * proves the `require` blocks each carries. It emits nothing — no `Room`, `RoomRegistry` or
 * `DuelSocket` behaviour is exercised here (`TASK-021403`–`TASK-021409`).
 */
class PresenceFramesTest {
    @Test
    fun awayCarriesARemainingDuration() {
        val presence: ServerMessage = ServerMessage.OpponentPresence(SeatPresence.AWAY, 45_000L)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), presence)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(presence, decoded)
    }

    @Test
    fun awayWithZeroRemainingIsLegal() {
        val presence = ServerMessage.OpponentPresence(SeatPresence.AWAY, 0L)
        assertEquals(0L, presence.graceRemainingMillis)
    }

    @Test
    fun awayWithoutARemainingIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.OpponentPresence(SeatPresence.AWAY, null)
        }
    }

    @Test
    fun presentWithARemainingIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.OpponentPresence(SeatPresence.PRESENT, 1L)
        }
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.OpponentPresence(SeatPresence.ABSENT, 1L)
        }
    }

    @Test
    fun aNegativeRemainingIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ServerMessage.OpponentPresence(SeatPresence.AWAY, -1L)
        }
    }

    @Test
    fun presentAndAbsentCarryNothing() {
        assertEquals(null, ServerMessage.OpponentPresence(SeatPresence.PRESENT).graceRemainingMillis)
        assertEquals(null, ServerMessage.OpponentPresence(SeatPresence.ABSENT).graceRemainingMillis)
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
