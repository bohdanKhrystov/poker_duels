package duels.poker.ai

import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.HoleCardsDealt
import duels.poker.engine.game.PlayerFolded
import duels.poker.engine.game.ShowdownReached
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/** Proves [observeDuel] records exactly what the redaction properties built on it will need. */
internal class ObservedDuelTest {

    @Timeout(120)
    @Test
    fun everyHandEndsComplete() {
        for (seed in 1L..20L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                assertTrue(
                    hand.finalState.isHandOver,
                    "${seedReport(seed, hand.handSeed)}: hand ${hand.handNumber} did not end complete",
                )
            }
        }
    }

    @Test
    fun theSameSeedObservesTheSameDuel() {
        val first = observeDuel(7)
        val second = observeDuel(7)
        assertEquals(first, second)
    }

    @Timeout(120)
    @Test
    fun eachStepsEventsExtendThePreviousStep() {
        for (seed in 1L..20L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                for (i in 1 until hand.steps.size) {
                    val earlier = hand.steps[i - 1].eventsSoFar
                    val later = hand.steps[i].eventsSoFar
                    assertEquals(
                        earlier,
                        later.subList(0, earlier.size),
                        "${seedReport(seed, hand.handSeed)}: step $i's events do not extend step ${i - 1}'s",
                    )
                }
            }
        }
    }

    @Test
    fun theFirstStepCarriesTheOpeningEvents() {
        val duel = observeDuel(7)
        val opening = duel.hands.first().steps.first().eventsSoFar

        assertTrue(
            opening.any { it is HandStarted },
            "${seedReport(7, duel.hands.first().handSeed)}: opening events carry no HandStarted",
        )
        assertEquals(
            2,
            opening.count { it is HoleCardsDealt },
            "${seedReport(7, duel.hands.first().handSeed)}: opening events do not carry two HoleCardsDealt",
        )
    }

    @Timeout(120)
    @Test
    fun theSampleContainsShowdownsAndFolds() {
        var sawShowdown = false
        var sawFold = false

        for (seed in 1L..20L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                if (hand.events.any { it is ShowdownReached }) sawShowdown = true
                if (hand.events.any { it is PlayerFolded }) sawFold = true
            }
        }

        assertTrue(sawShowdown, "no hand across seeds 1..20 reached showdown")
        assertTrue(sawFold, "no hand across seeds 1..20 saw a fold")
    }

    @Test
    fun seedReportNamesBothSeeds() {
        val report = seedReport(7, -13)
        assertTrue(report.contains("7"), "report '$report' does not contain the duel seed")
        assertTrue(report.contains("-13"), "report '$report' does not contain the hand seed")
    }
}
