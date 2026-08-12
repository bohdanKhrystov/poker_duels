package duels.poker.engine.game

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The project's non-negotiable — a folded or mucked hand appears in no event, anywhere — asserted
 * over a thousand generated hands rather than assumed. `ADR-0008` settles the muck rule: at
 * showdown the loser's hand is never revealed, so it appears in no event, exactly as a folded
 * hand does.
 */
class CardSecrecyTest {

    @Test
    @Timeout(30)
    fun noEventCarriesAFoldersCards() {
        for (seed in foldedHandSeeds()) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            for (seat in finalState.seats.filter { it.hasFolded }) {
                val holeCards = finalState.seats[seat.index].holeCards
                val ownDeal = played.events
                    .filterIsInstance<HoleCardsDealt>()
                    .single { it.seat == seat.index }

                for (event in played.events) {
                    if (event === ownDeal) continue

                    val leaked = cardsIn(event).intersect(holeCards.toSet())
                    assertTrue(
                        leaked.isEmpty(),
                        "seed $seed: event $event leaks folded seat ${seat.index}'s cards $leaked",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(30)
    fun theBoardNeverShowsAFoldersCards() {
        for (seed in foldedHandSeeds()) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            val boardCards = played.events
                .filterIsInstance<StreetDealt>()
                .flatMap { it.cards }
                .toSet()

            for (seat in finalState.seats.filter { it.hasFolded }) {
                val holeCards = finalState.seats[seat.index].holeCards.toSet()
                val onBoard = boardCards.intersect(holeCards)
                assertTrue(
                    onBoard.isEmpty(),
                    "seed $seed: board carries folded seat ${seat.index}'s cards $onBoard",
                )
            }
        }
    }

    @Test
    @Timeout(30)
    fun noEventCarriesAMuckedHandsCards() {
        for (seed in showdownHandSeeds()) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            // The shown seats are the ones ADR-0008 names, not `showdownWinners`: this test must
            // not restate the logic it guards, only the log's own record of who was paid.
            val shownSeats = played.events.filterIsInstance<PotAwarded>().map { it.seat }.toSet()
            val muckedSeats = finalState.seats.map { it.index }.filterNot { it in shownSeats }

            for (seat in muckedSeats) {
                val holeCards = finalState.seats[seat].holeCards
                val ownDeal = played.events
                    .filterIsInstance<HoleCardsDealt>()
                    .single { it.seat == seat }

                for (event in played.events) {
                    if (event === ownDeal) continue

                    val leaked = cardsIn(event).intersect(holeCards.toSet())
                    assertTrue(
                        leaked.isEmpty(),
                        "seed $seed: event $event leaks mucked seat $seat's cards $leaked",
                    )
                }
            }
        }
    }

    @Test
    @Timeout(30)
    fun aHandWonOnAFoldRevealsNothing() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            if (played.events.any { it is ShowdownReached }) continue

            assertTrue(
                played.events.none { it is HandRevealed },
                "seed $seed: HandRevealed appeared though the hand was won on a fold",
            )
        }
    }

    @Test
    @Timeout(30)
    fun theSampleContainsFolds() {
        val foldedHandCount = foldedHandSeeds().count()
        assertTrue(
            foldedHandCount > 100,
            "expected more than 100 of 1000 hands to end with a fold, got $foldedHandCount",
        )
    }

    @Test
    @Timeout(30)
    fun theSampleContainsShowdowns() {
        val showdownHandCount = showdownHandSeeds().count()
        assertTrue(
            showdownHandCount > 100,
            "expected more than 100 of 1000 hands to reach a showdown, got $showdownHandCount",
        )
    }

    private fun foldedHandSeeds(): List<Long> =
        (1L..1000L).filter { seed -> playRandomHand(seed).finalState.seats.any { it.hasFolded } }

    private fun showdownHandSeeds(): List<Long> =
        (1L..1000L).filter { seed -> playRandomHand(seed).events.any { it is ShowdownReached } }
}

/**
 * Every card a single event carries. Exhaustive over [GameEvent] with no `else` branch: a future
 * event that carries cards cannot be added without this test failing to compile.
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
