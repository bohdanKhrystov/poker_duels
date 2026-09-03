package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.server.duel.DuelStep
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

private val fixedSeeds = HandSeedSource { 7L }

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun registryWith(
    clock: MutableClock,
    turnMillis: Long = 30_000L,
    timebankMillis: Long = 180_000L,
    disconnectGraceMillis: Long = RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS,
): RoomRegistry {
    val timeouts = RoomTimeouts(
        waitingMillis = RoomTimeouts.DEFAULT_WAITING_MILLIS,
        finishedMillis = RoomTimeouts.DEFAULT_FINISHED_MILLIS,
        disconnectGraceMillis = disconnectGraceMillis,
        turnMillis = turnMillis,
        timebankMillis = timebankMillis,
    )
    return RoomRegistry(RoomCodeSource { RoomCode("2B7KMNPQ") }, clock, timeouts, seeds = fixedSeeds)
}

/** Seats a host and guest into a fresh room, starting its duel. */
private suspend fun seatedRoom(registry: RoomRegistry, format: DuelFormat = DuelFormat.DEFAULT): Room {
    val host = newPlayerId()
    val guest = newPlayerId()
    val room = registry.create(host, format)
    return (registry.join(room.code, guest) as JoinResult.Seated).room
}

/** Builds the [Act] frame for whoever is on turn in [room]'s live hand, calling whatever bet stands. */
private fun callFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Call(hand.state.seatToAct!!),
    )
}

/**
 * The cheapest legal action for whoever is on turn in [room]'s live hand — check when possible,
 * fold otherwise. Mirrors [duels.poker.server.duel.giveUpDecision]'s own choice, so this always
 * sends a legal frame regardless of the exact spot.
 */
private fun cheapestFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    val toActSeat = hand.state.seatToAct!!
    val legal = legalActions(hand.state)
    val action = if (legal.allows(ActionType.CHECK)) PlayerAction.Check(toActSeat) else PlayerAction.Fold(toActSeat)
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = action,
    )
}

/** Drives [frame] through [registry] at [code], via [Room.act]. */
private suspend fun actOn(registry: RoomRegistry, code: RoomCode, frame: Act): DuelStep =
    registry.act(code) { r -> r.act(frame.action.seat, frame, fixedSeeds) }!!

internal class TurnClockFramesTest {

    @Test
    fun bothSeatsAreToldTheClock() = runBlocking {
        val registry = registryWith(MutableClock(1_000L))
        val seated = seatedRoom(registry)

        val step = actOn(registry, seated.code, callFrame(seated))

        val clocks = step.outbound.filter { it.message is ServerMessage.TurnClock }
        assertEquals(listOf(0, 1), clocks.map { it.seat })
        assertEquals(clocks[0].message, clocks[1].message)
    }

    @Test
    fun theClockIsLastInTheBatch() = runBlocking {
        val registry = registryWith(MutableClock(1_000L))
        val seated = seatedRoom(registry)

        val step = actOn(registry, seated.code, callFrame(seated))

        val last = step.outbound.takeLast(2)
        assertTrue(last.all { it.message is ServerMessage.TurnClock })
        assertEquals(0, last[0].seat)
        assertEquals(1, last[1].seat)
        val earlier = step.outbound.dropLast(2)
        assertTrue(earlier.none { it.message is ServerMessage.TurnClock })
        assertEquals(2, earlier.count { it.message is ServerMessage.Snapshot })
        assertEquals(1, earlier.count { it.message is ServerMessage.YourTurn })
    }

    @Test
    fun theClockNamesTheDecisionItIsFor() = runBlocking {
        val registry = registryWith(MutableClock(1_000L))
        val seated = seatedRoom(registry)

        val step = actOn(registry, seated.code, callFrame(seated))

        val yourTurn = step.outbound.single { it.message is ServerMessage.YourTurn }
        val yourTurnMessage = yourTurn.message as ServerMessage.YourTurn
        val clockMessage = step.outbound.last().message as ServerMessage.TurnClock
        assertEquals(yourTurnMessage.handNumber, clockMessage.handNumber)
        assertEquals(yourTurnMessage.actionSequence, clockMessage.actionSequence)
        assertEquals(yourTurn.seat, clockMessage.seat)
    }

    @Test
    fun aFreshDecisionIsOwedTheWholeAllowance() = runBlocking {
        val registry = registryWith(MutableClock(5_000L), turnMillis = 30_000L)
        val seated = seatedRoom(registry)

        val step = actOn(registry, seated.code, callFrame(seated))

        val clockMessage = step.outbound.last().message as ServerMessage.TurnClock
        assertEquals(30_000L, clockMessage.turnRemainingMillis)
    }

    @Test
    fun bothBanksAreStatedEveryFrame() = runBlocking {
        val timebankMillis = 180_000L
        val registry = registryWith(MutableClock(0L), timebankMillis = timebankMillis)
        val seated = seatedRoom(registry)

        val step = actOn(registry, seated.code, callFrame(seated))

        val clockMessage = step.outbound.last().message as ServerMessage.TurnClock
        assertEquals(listOf(timebankMillis, timebankMillis), clockMessage.bankRemainingMillis)
    }

    @Test
    fun anOverrunReachesTheNextFramesBank() = runBlocking {
        val clock = MutableClock(0L)
        val turnMillis = 5_000L
        val timebankMillis = 180_000L
        val registry = registryWith(clock, turnMillis = turnMillis, timebankMillis = timebankMillis)
        val seated = seatedRoom(registry)

        val opened = actOn(registry, seated.code, callFrame(seated))
        val openedSeat = (opened.outbound.last().message as ServerMessage.TurnClock).seat

        // Past the free allowance for the seat decision B just armed, before that seat answers.
        clock.advance(turnMillis + 2_000L)
        val closed = actOn(registry, seated.code, cheapestFrame(registry.get(seated.code)!!))

        val clockMessage = closed.outbound.last().message as ServerMessage.TurnClock
        val rivalSeat = 1 - openedSeat
        // 180_000 - 2_000: the full bank withFreshClocks seeded, minus the 2s spent past the free
        // allowance. A literal, not a re-derivation through Room.clocked/Room.turnClock — the
        // mechanism this assertion exists to check.
        assertEquals(178_000L, clockMessage.bankRemainingMillis[openedSeat])
        assertEquals(timebankMillis, clockMessage.bankRemainingMillis[rivalSeat])
    }

    @Test
    fun aBatchThatPlaysSeveralDecisionsCarriesOneClock() = runBlocking {
        val clock = MutableClock(0L)
        val disconnectGraceMillis = 1_000L
        val registry = registryWith(clock, disconnectGraceMillis = disconnectGraceMillis)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val openingSeat = seated.runner!!.hand!!.state.seatToAct!!
        val absentSeat = 1 - openingSeat
        val absentPlayer = if (absentSeat == 0) host else guest

        // The opening seat calls, closing hand 1's own opening decision and handing the turn to
        // the seat this test is about to disconnect — with both its preflop response and its
        // postflop first action still ahead of it before the opening seat is next due to act.
        actOn(registry, seated.code, callFrame(seated))
        registry.disconnect(seated.code, absentPlayer)

        clock.advance(disconnectGraceMillis + 1)
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val outbound = expiries.single().outbound
        // Two decisions were actually folded through for the absent seat — one `ActedForAbsent`
        // pair each — or this test could not tell "one clock per write-back" apart from "one
        // clock because there was only ever one decision to clock".
        assertEquals(4, outbound.count { it.message is ServerMessage.ActedForAbsent })
        val clocks = outbound.filter { it.message is ServerMessage.TurnClock }
        assertEquals(2, clocks.size)
        assertEquals(setOf(0, 1), clocks.map { it.seat }.toSet())
        assertEquals(clocks[0].message, clocks[1].message)
    }

    @Test
    fun aFinishedDuelIsOwedNoClock() = runBlocking {
        val registry = registryWith(MutableClock(0L))
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val seated = seatedRoom(registry, oneHand)
        val hand = seated.runner!!.hand!!
        val fold = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(hand.state.seatToAct!!),
        )

        val step = actOn(registry, seated.code, fold)

        assertTrue(step.outbound.none { it.message is ServerMessage.TurnClock })
        assertNull(registry.get(seated.code)!!.turnDeadline)
    }
}
