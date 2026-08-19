package duels.poker.server.season

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class SeasonOfDuelTest {
    @Test
    fun aDuelIsAttributedToTheMonthItFinishedIn() {
        val duel = FinishedDuel(
            id = UUID.randomUUID(),
            format = "FREEZEOUT",
            startedAt = Instant.parse("2026-08-31T22:00:00Z"),
            finishedAt = Instant.parse("2026-09-01T00:30:00Z"),
            seats = listOf(
                PlayerId("11111111-1111-1111-1111-111111111111"),
                PlayerId("22222222-2222-2222-2222-222222222222"),
            ),
            outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0)),
        )

        assertEquals(Season(2026, 9), seasonOf(duel))
    }

    @Test
    fun aDuelThatRanInsideOneMonthIsAttributedToThatMonth() {
        val duel = FinishedDuel(
            id = UUID.randomUUID(),
            format = "FREEZEOUT",
            startedAt = Instant.parse("2026-08-15T10:00:00Z"),
            finishedAt = Instant.parse("2026-08-15T10:40:00Z"),
            seats = listOf(
                PlayerId("11111111-1111-1111-1111-111111111111"),
                PlayerId("22222222-2222-2222-2222-222222222222"),
            ),
            outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0)),
        )

        assertEquals(Season(2026, 8), seasonOf(duel))
    }

    @Test
    fun theSeasonOfADuelIsAFunctionOfItsFinishAndOfNothingElse() {
        val duel = FinishedDuel(
            id = UUID.randomUUID(),
            format = "FREEZEOUT",
            startedAt = Instant.parse("2019-02-28T22:00:00Z"),
            finishedAt = Instant.parse("2019-02-28T23:30:00Z"),
            seats = listOf(
                PlayerId("11111111-1111-1111-1111-111111111111"),
                PlayerId("22222222-2222-2222-2222-222222222222"),
            ),
            outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0)),
        )

        // Call twice and assert both answers are the same
        val season1 = seasonOf(duel)
        val season2 = seasonOf(duel)
        assertEquals(Season(2019, 2), season1)
        assertEquals(Season(2019, 2), season2)
    }

    @Test
    fun aDuelFinishedExactlyAtAMonthBoundaryBelongsToThatMonth() {
        val duel = FinishedDuel(
            id = UUID.randomUUID(),
            format = "FREEZEOUT",
            startedAt = Instant.parse("2026-08-31T22:00:00Z"),
            finishedAt = Instant.parse("2026-09-01T00:00:00Z"),
            seats = listOf(
                PlayerId("11111111-1111-1111-1111-111111111111"),
                PlayerId("22222222-2222-2222-2222-222222222222"),
            ),
            outcome = DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0)),
        )

        assertEquals(Season(2026, 9), seasonOf(duel))
    }
}
