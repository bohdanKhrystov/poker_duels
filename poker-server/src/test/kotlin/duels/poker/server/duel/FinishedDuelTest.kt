package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.EndCondition
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class FinishedDuelTest {
    @Test
    fun aFinishedDuelCarriesBothSeatsAndItsOutcome() {
        val id = UUID.randomUUID()
        val seat0 = PlayerId("11111111-1111-1111-1111-111111111111")
        val seat1 = PlayerId("22222222-2222-2222-2222-222222222222")
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val startTime = Instant.parse("2026-08-13T10:00:00Z")
        val endTime = Instant.parse("2026-08-13T10:15:00Z")

        val duel = FinishedDuel(
            id = id,
            format = "FREEZEOUT",
            startedAt = startTime,
            finishedAt = endTime,
            seats = listOf(seat0, seat1),
            outcome = outcome,
        )

        assertEquals(seat0, duel.seats[0])
        assertEquals(seat1, duel.seats[1])
        assertEquals(outcome, duel.outcome)
    }

    @Test
    fun rejectsAnythingOtherThanTwoSeats() {
        val id = UUID.randomUUID()
        val seat0 = PlayerId("11111111-1111-1111-1111-111111111111")
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val startTime = Instant.parse("2026-08-13T10:00:00Z")
        val endTime = Instant.parse("2026-08-13T10:15:00Z")

        // One seat
        assertThrows<IllegalArgumentException> {
            FinishedDuel(
                id = id,
                format = "FREEZEOUT",
                startedAt = startTime,
                finishedAt = endTime,
                seats = listOf(seat0),
                outcome = outcome,
            )
        }

        // Three seats
        assertThrows<IllegalArgumentException> {
            FinishedDuel(
                id = id,
                format = "FREEZEOUT",
                startedAt = startTime,
                finishedAt = endTime,
                seats = listOf(
                    PlayerId("11111111-1111-1111-1111-111111111111"),
                    PlayerId("22222222-2222-2222-2222-222222222222"),
                    PlayerId("33333333-3333-3333-3333-333333333333"),
                ),
                outcome = outcome,
            )
        }
    }

    @Test
    fun rejectsTheSamePlayerInBothSeats() {
        val id = UUID.randomUUID()
        val seat = PlayerId("11111111-1111-1111-1111-111111111111")
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val startTime = Instant.parse("2026-08-13T10:00:00Z")
        val endTime = Instant.parse("2026-08-13T10:15:00Z")

        assertThrows<IllegalArgumentException> {
            FinishedDuel(
                id = id,
                format = "FREEZEOUT",
                startedAt = startTime,
                finishedAt = endTime,
                seats = listOf(seat, seat),
                outcome = outcome,
            )
        }
    }

    @Test
    fun rejectsABlankFormatLabel() {
        val id = UUID.randomUUID()
        val seat0 = PlayerId("11111111-1111-1111-1111-111111111111")
        val seat1 = PlayerId("22222222-2222-2222-2222-222222222222")
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val startTime = Instant.parse("2026-08-13T10:00:00Z")
        val endTime = Instant.parse("2026-08-13T10:15:00Z")

        assertThrows<IllegalArgumentException> {
            FinishedDuel(
                id = id,
                format = "  ",
                startedAt = startTime,
                finishedAt = endTime,
                seats = listOf(seat0, seat1),
                outcome = outcome,
            )
        }
    }

    @Test
    fun rejectsAFinishTimeBeforeTheStart() {
        val id = UUID.randomUUID()
        val seat0 = PlayerId("11111111-1111-1111-1111-111111111111")
        val seat1 = PlayerId("22222222-2222-2222-2222-222222222222")
        val outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))
        val startTime = Instant.parse("2026-08-13T10:00:00Z")
        val endTime = Instant.parse("2026-08-13T09:59:59Z")

        assertThrows<IllegalArgumentException> {
            FinishedDuel(
                id = id,
                format = "FREEZEOUT",
                startedAt = startTime,
                finishedAt = endTime,
                seats = listOf(seat0, seat1),
                outcome = outcome,
            )
        }
    }

    @Test
    fun theDefaultFormatIsLabelledFreezeout() {
        assertEquals("FREEZEOUT", formatLabel(DuelFormat.DEFAULT))
    }

    @Test
    fun aFixedHandsFormatIsLabelledFixedHands() {
        val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))
        assertEquals("FIXED_HANDS", formatLabel(format))
    }
}
