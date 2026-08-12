package duels.poker.ai

import duels.poker.engine.card.Card
import duels.poker.engine.game.ActionOn
import duels.poker.engine.game.BettingRoundEnded
import duels.poker.engine.game.BlindPosted
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.HandFinished
import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.HoleCardsDealt
import duels.poker.engine.game.PlayerAllIn
import duels.poker.engine.game.PlayerBet
import duels.poker.engine.game.PlayerCalled
import duels.poker.engine.game.PlayerChecked
import duels.poker.engine.game.PlayerFolded
import duels.poker.engine.game.PlayerRaised
import duels.poker.engine.game.PotAwarded
import duels.poker.engine.game.ShowdownReached
import duels.poker.engine.game.StreetDealt
import duels.poker.engine.game.UncalledBetReturned
import duels.poker.engine.game.visibleTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The adversarial half of the redaction property, over a thousand simulated [RandomBot] duels:
 * `visibleTo(hand.events, seat)`, the event stream `poker-engine`'s projection layer would hand a
 * seat, never carries a card that seat may not see, and never carries anything but the original
 * event object it filtered from `hand.events`. `CardSecrecyTest`, in `poker-engine`, asserts the
 * same secrecy from the log's own side; this test asserts it from the redaction function's side,
 * so the two can never fail for the same reason.
 */
class EventStreamLeakTest {

    @Test
    @Timeout(300)
    fun noSeatReceivesACardItIsNotEntitledTo() {
        for (seed in 1L..1000L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                for (seat in 0..1) {
                    val ownCards = hand.events
                        .filterIsInstance<HoleCardsDealt>()
                        .single { it.seat == seat }
                        .cards
                        .toSet()
                    val boardCards = hand.events
                        .filterIsInstance<StreetDealt>()
                        .flatMap { it.cards }
                        .toSet()
                    val revealedCards = hand.events
                        .filterIsInstance<HandRevealed>()
                        .flatMap { it.cards }
                        .toSet()
                    val entitled = ownCards + boardCards + revealedCards

                    for (event in visibleTo(hand.events, seat)) {
                        val unentitled = cardsIn(event).toSet() - entitled
                        assertTrue(
                            unentitled.isEmpty(),
                            "${seedReport(duel.seed, hand.handSeed)}: seat $seat's stream carries " +
                                "unentitled cards $unentitled in $event",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(300)
    fun noSeatReceivesTheOtherSeatsDeal() {
        for (seed in 1L..1000L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                for (seat in 0..1) {
                    val otherSeat = 1 - seat
                    val otherDeal = hand.events
                        .filterIsInstance<HoleCardsDealt>()
                        .single { it.seat == otherSeat }
                    val stream = visibleTo(hand.events, seat)

                    assertTrue(
                        otherDeal !in stream,
                        "${seedReport(duel.seed, hand.handSeed)}: seat $seat's stream carries the " +
                            "other seat's HoleCardsDealt $otherDeal",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun everyDeliveredEventIsTheOriginalObject() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                val bySequence = hand.events.associateBy { it.sequence }
                for (seat in 0..1) {
                    for (delivered in visibleTo(hand.events, seat)) {
                        val original = bySequence.getValue(delivered.sequence)
                        assertSame(
                            original,
                            delivered,
                            "${seedReport(duel.seed, hand.handSeed)}: seat $seat's stream carries a " +
                                "copy, not the original instance, of $delivered",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun theOnlyEventDroppedIsTheOtherSeatsDeal() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                for (seat in 0..1) {
                    val otherSeat = 1 - seat
                    val stream = visibleTo(hand.events, seat)

                    assertEquals(
                        hand.events.size - 1,
                        stream.size,
                        "${seedReport(duel.seed, hand.handSeed)}: seat $seat's stream should be exactly " +
                            "one event shorter than the full log",
                    )

                    val streamSequences = stream.map { it.sequence }.toSet()
                    val dropped = hand.events.filterNot { it.sequence in streamSequences }
                    assertEquals(
                        1,
                        dropped.size,
                        "${seedReport(duel.seed, hand.handSeed)}: expected exactly one event dropped " +
                            "from seat $seat's stream, dropped $dropped",
                    )

                    val droppedEvent = dropped.single()
                    assertTrue(
                        droppedEvent is HoleCardsDealt && droppedEvent.seat == otherSeat,
                        "${seedReport(duel.seed, hand.handSeed)}: the event dropped from seat $seat's " +
                            "stream was $droppedEvent, not the other seat's HoleCardsDealt",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun aMuckedHandAppearsInNoStream() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                if (hand.events.none { it is ShowdownReached }) continue

                val revealedSeats = hand.events.filterIsInstance<HandRevealed>().map { it.seat }.toSet()
                for (seat in 0..1) {
                    if (seat in revealedSeats) continue

                    val otherSeat = 1 - seat
                    val holeCards = hand.events
                        .filterIsInstance<HoleCardsDealt>()
                        .single { it.seat == seat }
                        .cards
                        .toSet()

                    for (event in visibleTo(hand.events, otherSeat)) {
                        val leaked = cardsIn(event).toSet().intersect(holeCards)
                        assertTrue(
                            leaked.isEmpty(),
                            "${seedReport(duel.seed, hand.handSeed)}: seat $otherSeat's stream leaks " +
                                "mucked seat $seat's cards $leaked in $event",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun theSampleContainsShowdownsFoldsAndMucks() {
        var showdownCount = 0
        var foldCount = 0
        var muckCount = 0
        var lastDuelSeed = 0L
        var lastHandSeed = 0L

        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                lastDuelSeed = duel.seed
                lastHandSeed = hand.handSeed

                if (hand.events.any { it is ShowdownReached }) {
                    showdownCount++
                    val revealedSeats = hand.events.filterIsInstance<HandRevealed>().map { it.seat }.toSet()
                    if (revealedSeats.size < 2) {
                        muckCount++
                    }
                }
                if (hand.events.any { it is PlayerFolded }) {
                    foldCount++
                }
            }
        }

        val report = seedReport(lastDuelSeed, lastHandSeed)
        assertTrue(showdownCount > 0, "$report: sample over seeds 1L..100L contained no showdown hand")
        assertTrue(foldCount > 0, "$report: sample over seeds 1L..100L contained no folded hand")
        assertTrue(muckCount > 0, "$report: sample over seeds 1L..100L contained no mucked hand")
    }
}

/**
 * Every card a single event carries. Exhaustive over [GameEvent] with no `else` branch: a future
 * event that carries cards cannot be added without this test failing to compile. `poker-engine`'s
 * own test sources are not on `poker-ai`'s classpath, so this mirrors `CardSecrecyTest`'s
 * `cardsIn` rather than sharing it.
 */
private fun cardsIn(event: GameEvent): List<Card> = when (event) {
    is HoleCardsDealt -> event.cards
    is StreetDealt -> event.cards
    is HandRevealed -> event.cards
    is HandStarted -> emptyList()
    is BlindPosted -> emptyList()
    is ActionOn -> emptyList()
    is PlayerFolded -> emptyList()
    is PlayerChecked -> emptyList()
    is PlayerCalled -> emptyList()
    is PlayerBet -> emptyList()
    is PlayerRaised -> emptyList()
    is PlayerAllIn -> emptyList()
    is BettingRoundEnded -> emptyList()
    is ShowdownReached -> emptyList()
    is UncalledBetReturned -> emptyList()
    is PotAwarded -> emptyList()
    is HandFinished -> emptyList()
}
