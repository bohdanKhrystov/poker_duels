package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.DuelRunner
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.act
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.duel.startDuel
import duels.poker.server.protocol.Act
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

private val seeds = HandSeedSource { 7L }

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

/** A room hosting no duel of its own: [Room.clocked] reads only the [DuelRunner] it is handed. */
private fun aRoom(turnDeadline: TurnDeadline? = null, banks: Map<Int, Long> = emptyMap()): Room =
    Room(
        code = RoomCode("2B7KMNPQ"),
        host = newPlayerId(),
        guest = null,
        state = RoomState.WAITING,
        format = DuelFormat.DEFAULT,
        match = null,
        openingButtonSeat = 0,
        rematchOffers = emptySet(),
        lastActivityAt = 0L,
        turnDeadline = turnDeadline,
        timebankRemainingMillis = banks,
    )

/** A fresh duel with [buttonSeat] to act first in hand 1. */
private fun freshRunner(buttonSeat: Int): DuelRunner =
    startDuel(DuelFormat.DEFAULT, buttonSeat, seeds.newHandSeed()).runner

/** A fresh, one-hand duel with [buttonSeat] to act first — folding it ends the whole duel. */
private fun freshOneHandRunner(buttonSeat: Int): DuelRunner {
    val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    return startDuel(oneHand, buttonSeat, seeds.newHandSeed()).runner
}

/** [runner] after the seat on turn folds — the fastest way to close a decision outright. */
private fun folded(runner: DuelRunner): DuelRunner {
    val hand = runner.hand!!
    val toActSeat = hand.state.seatToAct!!
    val fold = Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Fold(toActSeat),
    )
    return act(runner, toActSeat, fold, seeds).runner
}

internal class RoomTurnClockTest {

    @Test
    fun aFreshDecisionPointStartsTheAllowanceAndThenTheBank() {
        val runner = freshRunner(buttonSeat = 0)
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 180_000L))

        val result = room.clocked(runner, now = 1_000L, turnMillis = 10_000L)

        val deadline = result.turnDeadline
        assertNotNull(deadline)
        assertEquals(0, deadline!!.seat)
        assertEquals(1, deadline.handNumber)
        assertEquals(decisionPointOf(runner.hand!!.log.events)!!.sequence, deadline.actionSequence)
        assertEquals(11_000L, deadline.bankBeginsAt)
        assertEquals(191_000L, deadline.expiresAt)
    }

    @Test
    fun aDecisionAnsweredInsideTheAllowanceSpendsNothing() {
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(freshRunner(buttonSeat = 0), now = 0L, turnMillis = 5_000L)

        val result = room.clocked(freshRunner(buttonSeat = 1), now = 4_999L, turnMillis = 5_000L)

        assertEquals(180_000L, result.timebankRemainingMillis[0])
        assertEquals(170_000L, result.timebankRemainingMillis[1])
    }

    @Test
    fun fiveSecondsIntoTheBankDebitsExactlyFiveThousand() {
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(freshRunner(buttonSeat = 0), now = 0L, turnMillis = 5_000L)

        val result = room.clocked(freshRunner(buttonSeat = 1), now = 10_000L, turnMillis = 5_000L)

        assertEquals(175_000L, result.timebankRemainingMillis[0])
    }

    @Test
    fun eightSecondsIntoTheBankDebitsExactlyEightThousand() {
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(freshRunner(buttonSeat = 0), now = 0L, turnMillis = 5_000L)

        val result = room.clocked(freshRunner(buttonSeat = 1), now = 13_000L, turnMillis = 5_000L)

        assertEquals(172_000L, result.timebankRemainingMillis[0])
    }

    @Test
    fun seatOneOverrunDebitsSeatOne() {
        val room = aRoom(banks = mapOf(0 to 170_000L, 1 to 180_000L))
            .clocked(freshRunner(buttonSeat = 1), now = 0L, turnMillis = 5_000L)

        val result = room.clocked(freshRunner(buttonSeat = 0), now = 13_000L, turnMillis = 5_000L)

        assertEquals(172_000L, result.timebankRemainingMillis[1])
    }

    @Test
    fun theRivalsBankIsUntouchedByAnOverrun() {
        val seatZeroOverrun = aRoom(banks = mapOf(0 to 180_000L, 1 to 55_000L))
            .clocked(freshRunner(buttonSeat = 0), now = 0L, turnMillis = 5_000L)
            .clocked(freshRunner(buttonSeat = 1), now = 13_000L, turnMillis = 5_000L)
        assertEquals(55_000L, seatZeroOverrun.timebankRemainingMillis[1])

        val seatOneOverrun = aRoom(banks = mapOf(0 to 55_000L, 1 to 180_000L))
            .clocked(freshRunner(buttonSeat = 1), now = 0L, turnMillis = 5_000L)
            .clocked(freshRunner(buttonSeat = 0), now = 13_000L, turnMillis = 5_000L)
        assertEquals(55_000L, seatOneOverrun.timebankRemainingMillis[0])
    }

    @Test
    fun aDecisionClosedPastTheDeadlineSpendsTheBankToZeroAndNoFurther() {
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(freshRunner(buttonSeat = 0), now = 0L, turnMillis = 5_000L)

        val result = room.clocked(freshRunner(buttonSeat = 1), now = 245_000L, turnMillis = 5_000L)

        assertEquals(0L, result.timebankRemainingMillis[0])
    }

    @Test
    fun theDeadlineIsUnchangedWhileTheDecisionPointStands() {
        val runner = freshRunner(buttonSeat = 0)
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(runner, now = 0L, turnMillis = 5_000L)

        val result = room.clocked(runner, now = 999_999L, turnMillis = 123_456L)

        assertEquals(room.turnDeadline, result.turnDeadline)
        assertEquals(room.timebankRemainingMillis, result.timebankRemainingMillis)
    }

    @Test
    fun aRunnerWithNoLiveDecisionPointClearsTheDeadline() {
        val runner = freshOneHandRunner(buttonSeat = 0)
        val room = aRoom(banks = mapOf(0 to 180_000L, 1 to 170_000L))
            .clocked(runner, now = 0L, turnMillis = 5_000L)

        val result = room.clocked(folded(runner), now = 13_000L, turnMillis = 5_000L)

        assertNull(result.turnDeadline)
        assertEquals(172_000L, result.timebankRemainingMillis[0])
    }

    @Test
    fun freshClocksRefillBothBanksAndClearTheDeadline() {
        val spent = aRoom(
            turnDeadline = TurnDeadline(
                seat = 0,
                handNumber = 1,
                actionSequence = 0,
                bankBeginsAt = 100L,
                expiresAt = 200L,
            ),
            banks = mapOf(0 to 42_000L, 1 to 13_000L),
        )

        val result = spent.withFreshClocks(180_000L)

        assertEquals(mapOf(0 to 180_000L, 1 to 180_000L), result.timebankRemainingMillis)
        assertNull(result.turnDeadline)
    }

    @Test
    fun aRoomRefusesAnImpossibleClock() {
        assertThrows(IllegalArgumentException::class.java) {
            aRoom(
                turnDeadline = TurnDeadline(
                    seat = 2,
                    handNumber = 1,
                    actionSequence = 0,
                    bankBeginsAt = 0L,
                    expiresAt = 1L,
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            aRoom(banks = mapOf(0 to -1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            aRoom(
                turnDeadline = TurnDeadline(
                    seat = 0,
                    handNumber = 1,
                    actionSequence = 0,
                    bankBeginsAt = 10L,
                    expiresAt = 5L,
                ),
            )
        }
    }
}
