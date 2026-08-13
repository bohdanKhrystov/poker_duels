package duels.poker.server.duel

import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.duel.outcomeOf
import duels.poker.engine.game.PlayerFolded
import duels.poker.engine.game.ShowdownReached
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/** Plays a whole duel through [DuelRunner], seeing only what a client would see. */
internal class RunnerDuelTest {
    // Use seeds whose decimal text cannot collide with sequence numbers, seats, hand numbers, chip
    // amounts or stacks. The distinctive 0x5EED prefix ensures the seed value is unambiguous in
    // the serialized output; collision avoidance is why they appear unusual rather than as 1L..20L.
    private val seeds = (0..19).map { 0x5EED_000000000001L + it }

    @Test
    @Timeout(120)
    fun everyDuelReachesAnOutcome() {
        for (seed in seeds) {
            val played = playDuel(seed)
            assertNotNull(played.runner.outcome, "seed $seed")
            assertNull(played.runner.hand, "seed $seed")
        }
    }

    @Test
    @Timeout(120)
    fun theOutcomeIsTheEnginesOwn() {
        for (seed in seeds) {
            val played = playDuel(seed)
            assertEquals(outcomeOf(played.runner.match), played.runner.outcome, "seed $seed")
        }
    }

    @Test
    fun theSameSeedPlaysTheSameDuel() {
        val testSeed = seeds.first()
        val first = playDuel(testSeed)
        val second = playDuel(testSeed)

        assertEquals(first.outbound, second.outbound)
        assertEquals(first.runner, second.runner)
        assertEquals(first.actions, second.actions)
    }

    @Test
    @Timeout(120)
    fun everyHandIsRecordedInOrder() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val expected = (1..played.runner.match.handsPlayed).toList()
            assertEquals(expected, played.runner.log.hands.map { it.handNumber }, "seed $seed")
        }
    }

    @Test
    @Timeout(120)
    fun theMatchLogRecordsExactlyOneEnding() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val endings = played.runner.log.events.filterIsInstance<MatchFinished>()
            assertEquals(1, endings.size, "seed $seed")
        }
    }

    @Test
    @Timeout(120)
    fun aClientThatObeysYourTurnIsNeverRejected() {
        for (seed in seeds) {
            val played = playDuel(seed)
            val rejections = played.outbound.filter { it.message is ServerMessage.Rejected }
            assertTrue(rejections.isEmpty(), "seed $seed: $rejections")
        }
    }

    @Test
    @Timeout(120)
    fun theSampleContainsBothShowdownsAndFolds() {
        var sawShowdown = false
        var sawFold = false

        for (seed in seeds) {
            val played = playDuel(seed)
            for (hand in played.runner.log.hands) {
                if (hand.events.any { it is ShowdownReached }) sawShowdown = true
                if (hand.events.any { it is PlayerFolded }) sawFold = true
            }
        }

        assertTrue(sawShowdown, "no hand in the sample reached showdown")
        assertTrue(sawFold, "no hand in the sample saw a fold")
    }
}
