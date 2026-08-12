package duels.poker.engine.game

import duels.poker.engine.card.Card
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The project's non-negotiable — a folded or mucked hand appears in no event, anywhere — asserted
 * over a thousand generated hands rather than assumed. Mucked hands are out of scope here: reveals
 * are not emitted at all yet (`DEC-004`, `TASK-010615`).
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
    fun noHandIsRevealedAnywhereYet() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            assertTrue(
                played.events.none { it is HandRevealed },
                "seed $seed: HandRevealed appeared though reveals are not emitted yet",
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

    private fun foldedHandSeeds(): List<Long> =
        (1L..1000L).filter { seed -> playRandomHand(seed).finalState.seats.any { it.hasFolded } }
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
