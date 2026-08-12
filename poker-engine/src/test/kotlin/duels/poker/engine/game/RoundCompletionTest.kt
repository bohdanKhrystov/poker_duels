package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val FLOP_BOARD = Board(cards("2h 7c 9s"))

class RoundCompletionTest {
    @Test
    fun theBigBlindKeepsItsOptionAfterALimp() {
        val (seat0, seat1) = seats()
        val state = handState(
            listOf(
                seat0.copy(committedThisStreet = BIG_BLIND, committedThisHand = BIG_BLIND),
                seat1.copy(committedThisStreet = BIG_BLIND, committedThisHand = BIG_BLIND),
            ),
        ).copy(betToMatch = BIG_BLIND)

        assertTrue(roundContinues(state, PlayerAction.Call(0)))
    }

    @Test
    fun aCalledRaiseEndsTheRound() {
        val (seat0, seat1) = seats()
        val state = handState(
            listOf(
                seat0.copy(committedThisStreet = 300, committedThisHand = 300),
                seat1.copy(committedThisStreet = 300, committedThisHand = 300),
            ),
        ).copy(betToMatch = 300)

        assertFalse(roundContinues(state, PlayerAction.Call(0)))
    }

    @Test
    fun theBigBlindsCheckEndsTheRound() {
        val (seat0, seat1) = seats()
        val state = handState(
            listOf(
                seat0.copy(committedThisStreet = BIG_BLIND, committedThisHand = BIG_BLIND),
                seat1.copy(committedThisStreet = BIG_BLIND, committedThisHand = BIG_BLIND),
            ),
        ).copy(betToMatch = BIG_BLIND)

        assertFalse(roundContinues(state, PlayerAction.Check(1)))
    }

    @Test
    fun theFirstCheckOnAStreetLeavesTheOpponentToAct() {
        val state = handState().copy(street = Street.FLOP, board = FLOP_BOARD)

        assertTrue(roundContinues(state, PlayerAction.Check(1)))
    }

    @Test
    fun theSecondCheckEndsTheRound() {
        val state = handState().copy(street = Street.FLOP, board = FLOP_BOARD)

        assertFalse(roundContinues(state, PlayerAction.Check(0)))
    }

    @Test
    fun anUnmatchedBetLeavesTheOpponentToAct() {
        val (seat0, seat1) = seats()
        val state = handState(
            listOf(
                seat0,
                seat1.copy(committedThisStreet = 300, committedThisHand = 300),
            ),
        ).copy(street = Street.FLOP, board = FLOP_BOARD, betToMatch = 300)

        assertTrue(roundContinues(state, PlayerAction.Bet(1, to = 300)))
    }

    @Test
    fun anAllInForLessEndsTheRound() {
        val (_, seat1) = seats()
        val seat0 = Seat(index = 0, stack = 0, committedThisStreet = 150, committedThisHand = 150, isAllIn = true)
        val state = handState(
            listOf(
                seat0,
                seat1.copy(committedThisStreet = 300, committedThisHand = 300),
            ),
        ).copy(betToMatch = 300)

        assertFalse(roundContinues(state, PlayerAction.Call(0)))
    }

    @Test
    fun anAllInOpponentEndsTheRound() {
        val (seat0, _) = seats()
        val seat1 = Seat(index = 1, stack = 0, committedThisStreet = 300, committedThisHand = 300, isAllIn = true)
        val state = handState(
            listOf(
                seat0.copy(committedThisStreet = 300, committedThisHand = 300),
                seat1,
            ),
        ).copy(betToMatch = 300)

        assertFalse(roundContinues(state, PlayerAction.Call(0)))
    }

    @Test
    fun aFoldEndsTheRound() {
        val (seat0, seat1) = seats()
        val state = handState(
            listOf(
                seat0.copy(hasFolded = true),
                seat1,
            ),
        )

        assertFalse(roundContinues(state, PlayerAction.Fold(0)))
    }
}
