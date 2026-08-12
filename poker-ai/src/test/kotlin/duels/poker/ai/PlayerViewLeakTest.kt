package duels.poker.ai

import duels.poker.engine.card.Card
import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.PlayerFolded
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.ShowdownReached
import duels.poker.engine.game.revealedSeats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The adversarial half of the redaction property, over a thousand simulated [RandomBot] duels:
 * `PlayerView.of(step.state, viewer, revealedSeats(step.eventsSoFar))`, built exactly as a server
 * would build it at every intermediate point of a hand, never carries a card the viewer is not
 * entitled to. `EventStreamLeakTest` asserts the same secrecy from the redacted event stream's
 * side, so the two can never fail for the same reason.
 */
class PlayerViewLeakTest {

    @Test
    @Timeout(300)
    fun noViewEverShowsAnUnrevealedOpponentCard() {
        forEachStep(1L..1000L) { duel, hand, step ->
            val revealed = revealedSeats(step.eventsSoFar)
            for (viewer in 0..1) {
                val other = 1 - viewer
                if (other in revealed) continue

                val view = PlayerView.of(step.state, viewer, revealed)
                val exposed = exposedCards(view)
                val opponentCards = step.state.seat(other).holeCards.toSet()
                val leaked = exposed.intersect(opponentCards)

                assertTrue(
                    leaked.isEmpty(),
                    "${seedReport(duel.seed, hand.handSeed)}: viewer $viewer's view at street " +
                        "${step.state.street} leaks unrevealed seat $other's cards $leaked",
                )
            }
        }
    }

    @Test
    @Timeout(120)
    fun everyViewShowsTheViewersOwnCards() {
        forEachStep(1L..100L) { duel, hand, step ->
            for (viewer in 0..1) {
                val view = PlayerView.of(step.state, viewer, revealedSeats(step.eventsSoFar))

                assertEquals(
                    step.state.seat(viewer).holeCards,
                    view.viewer.holeCards,
                    "${seedReport(duel.seed, hand.handSeed)}: viewer $viewer's own cards are not shown " +
                        "at street ${step.state.street}",
                )
            }
        }
    }

    @Test
    @Timeout(120)
    fun aRevealedHandAppearsInTheOpponentsView() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                val revealed = revealedSeats(hand.events)
                if (revealed.isEmpty()) continue

                val lastStep = hand.steps.last()
                for (revealedSeat in revealed) {
                    val other = 1 - revealedSeat
                    val view = PlayerView.of(lastStep.state, other, revealedSeats(lastStep.eventsSoFar))

                    assertEquals(
                        lastStep.state.seat(revealedSeat).holeCards,
                        view.seats[revealedSeat].holeCards,
                        "${seedReport(duel.seed, hand.handSeed)}: revealed seat $revealedSeat's cards " +
                            "do not appear in seat $other's final view",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun aFoldedHandNeverAppearsInTheOpponentsView() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                val folders = hand.events.filterIsInstance<PlayerFolded>().map { it.seat }.toSet()
                if (folders.isEmpty()) continue

                for (step in hand.steps) {
                    val revealed = revealedSeats(step.eventsSoFar)
                    for (folder in folders) {
                        if (folder in revealed) continue

                        val other = 1 - folder
                        val view = PlayerView.of(step.state, other, revealed)
                        val folderCards = step.state.seat(folder).holeCards.toSet()
                        val leaked = exposedCards(view).intersect(folderCards)

                        assertTrue(
                            leaked.isEmpty(),
                            "${seedReport(duel.seed, hand.handSeed)}: folded seat $folder's cards " +
                                "$leaked leak into seat $other's view at street ${step.state.street}",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun aMuckedHandNeverAppearsInTheOpponentsView() {
        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                if (hand.events.none { it is ShowdownReached }) continue

                val revealed = revealedSeats(hand.events)
                val mucked = (0..1).filterNot { it in revealed }
                if (mucked.isEmpty()) continue

                for (step in hand.steps) {
                    val revealedSoFar = revealedSeats(step.eventsSoFar)
                    for (muckedSeat in mucked) {
                        val other = 1 - muckedSeat
                        val view = PlayerView.of(step.state, other, revealedSoFar)
                        val muckedCards = step.state.seat(muckedSeat).holeCards.toSet()
                        val leaked = exposedCards(view).intersect(muckedCards)

                        assertTrue(
                            leaked.isEmpty(),
                            "${seedReport(duel.seed, hand.handSeed)}: mucked seat $muckedSeat's cards " +
                                "$leaked leak into seat $other's view at street ${step.state.street}",
                        )
                    }
                }
            }
        }
    }

    @Test
    @Timeout(120)
    fun theSampleContainsFoldsRevealsAndMucks() {
        var foldCount = 0
        var revealCount = 0
        var muckCount = 0
        var lastDuelSeed = 0L
        var lastHandSeed = 0L

        for (seed in 1L..100L) {
            val duel = observeDuel(seed)
            for (hand in duel.hands) {
                lastDuelSeed = duel.seed
                lastHandSeed = hand.handSeed

                if (hand.events.any { it is PlayerFolded }) foldCount++
                if (hand.events.any { it is HandRevealed }) revealCount++
                if (hand.events.any { it is ShowdownReached } &&
                    (0..1).any { it !in revealedSeats(hand.events) }
                ) {
                    muckCount++
                }
            }
        }

        val report = seedReport(lastDuelSeed, lastHandSeed)
        assertTrue(foldCount > 0, "$report: sample over seeds 1L..100L contained no folded hand")
        assertTrue(revealCount > 0, "$report: sample over seeds 1L..100L contained no HandRevealed")
        assertTrue(muckCount > 0, "$report: sample over seeds 1L..100L contained no mucked hand")
    }
}

/** Every card a view exposes: both seats' hole cards it chose to show, plus the board. */
private fun exposedCards(view: PlayerView): Set<Card> =
    (view.seats.flatMap { it.holeCards } + view.board.cards).toSet()

/**
 * Plays every duel of [seeds] and hands each of its [ObservedStep]s, along with the [ObservedDuel]
 * and [ObservedHand] it belongs to, to [body] — the shared iteration every property in this file
 * needs, so no property duplicates the duel/hand/step traversal itself.
 */
private fun forEachStep(seeds: LongRange, body: (ObservedDuel, ObservedHand, ObservedStep) -> Unit) {
    for (seed in seeds) {
        val duel = observeDuel(seed)
        for (hand in duel.hands) {
            for (step in hand.steps) {
                body(duel, hand, step)
            }
        }
    }
}
