package duels.poker.engine.duel

import duels.poker.engine.game.BlindPosted
import duels.poker.engine.game.HandStarted
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests for [startNextHand]: it opens exactly the hand [MatchState] says is next. */
class StartNextHandTest {
    @Test
    fun opensTheHandAfterTheOnesAlreadyPlayed() {
        val freshMatch = MatchState.start(DuelFormat.DEFAULT)
        val freshResult = startNextHand(freshMatch, rng = SplitMix64Rng(1L))
        val freshHandStarted = freshResult.events.first()
        assertTrue(freshHandStarted is HandStarted)
        assertEquals(1, (freshHandStarted as HandStarted).handNumber)

        val playedMatch = freshMatch.copy(handsPlayed = 3)
        val playedResult = startNextHand(playedMatch, rng = SplitMix64Rng(1L))
        val playedHandStarted = playedResult.events.first()
        assertTrue(playedHandStarted is HandStarted)
        assertEquals(4, (playedHandStarted as HandStarted).handNumber)
    }

    @Test
    fun postsTheBlindLevelForThatHandNumber() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(handsPlayed = 10)

        val result = startNextHand(match, rng = SplitMix64Rng(1L))

        val handStarted = result.events.first() as HandStarted
        assertEquals(75, handStarted.smallBlind)
        assertEquals(150, handStarted.bigBlind)
    }

    @Test
    fun dealsTheStacksTheMatchCarries() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(stacks = listOf(3_000, 17_000))

        val result = startNextHand(match, rng = SplitMix64Rng(1L))

        val handStarted = result.events.first() as HandStarted
        assertEquals(listOf(3_000, 17_000), handStarted.stacks)
        assertEquals(20_000, result.newState.chipsInPlay)
    }

    @Test
    fun putsTheButtonWhereTheMatchSaysItIs() {
        val match = MatchState.start(DuelFormat.DEFAULT, buttonSeat = 1)

        val result = startNextHand(match, rng = SplitMix64Rng(1L))

        assertEquals(1, result.newState.buttonSeat)
        val smallBlindPosted = result.events.filterIsInstance<BlindPosted>().first { !it.isBigBlind }
        assertEquals(1, smallBlindPosted.seat)
    }

    @Test
    fun aSeatShorterThanItsBlindIsDealtInAllIn() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(stacks = listOf(40, 20_000))

        val result = startNextHand(match, rng = SplitMix64Rng(1L))

        val smallBlindPosted = result.events.filterIsInstance<BlindPosted>().first { it.seat == 0 }
        assertEquals(40, smallBlindPosted.to)
        assertTrue(result.newState.seat(0).isAllIn)
    }

    @Test
    fun refusesToOpenAHandForASeatWithNoChips() {
        val match = MatchState.start(DuelFormat.DEFAULT).copy(stacks = listOf(0, 20_000))

        assertThrows(IllegalArgumentException::class.java) {
            startNextHand(match, rng = SplitMix64Rng(1L))
        }
    }
}
