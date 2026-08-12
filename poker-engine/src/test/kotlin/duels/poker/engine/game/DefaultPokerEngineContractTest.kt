package duels.poker.engine.game

import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertFalse

internal class DefaultPokerEngineContractTest : PokerEngineContract() {
    override fun engine(): PokerEngine = DefaultPokerEngine

    override fun cases(): List<Pair<GameState, PlayerAction>> {
        // The four additional positions for DefaultPokerEngine
        val openingState = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState
        val afterBigBlindCall = DefaultPokerEngine.handle(openingState, PlayerAction.Call(0)).newState

        // Deal the flop from a fresh deck to ensure board cards are not in the deck
        val baseFlopState = handState()
        val flopDeal = baseFlopState.deck.deal(3)
        val flopPosition = baseFlopState.copy(
            street = Street.FLOP,
            board = Board(flopDeal.cards),
            deck = flopDeal.deck,
            betToMatch = 300,
            seatToAct = 0,
        )

        val baseAllInState = handState()
        val allInDeal = baseAllInState.deck.deal(3)
        val holeDeal = allInDeal.deck.deal(4)
        val allInPosition = baseAllInState.copy(
            street = Street.FLOP,
            board = Board(allInDeal.cards),
            deck = holeDeal.deck,
            seats = listOf(
                baseAllInState.seat(0).copy(holeCards = listOf(holeDeal.cards[0], holeDeal.cards[1])),
                baseAllInState.seat(1).copy(holeCards = listOf(holeDeal.cards[2], holeDeal.cards[3]), isAllIn = true),
            ),
            betToMatch = 450,
            seatToAct = 0,
        )

        val additionalPositions = listOf(
            openingState,
            afterBigBlindCall,
            flopPosition,
            allInPosition,
        )

        return super.cases() + additionalPositions.flatMap { state ->
            val seat = state.seatToAct ?: 0
            listOf(
                state to PlayerAction.Fold(seat),
                state to PlayerAction.Check(seat),
                state to PlayerAction.Call(seat),
                state to PlayerAction.Bet(seat, to = state.bigBlind * 2),
                state to PlayerAction.Raise(seat, to = state.bigBlind * 3),
                state to PlayerAction.AllIn(seat),
            )
        }
    }

    @org.junit.jupiter.api.Test
    fun everyFixtureHasADeckThatExcludesItsBoard() {
        cases().forEach { (state, _) ->
            val boardCards = state.board.cards.toSet()
            val deckCards = mutableSetOf<duels.poker.engine.card.Card>()

            // Build the set of all cards in the deck
            var remainingCards = state.deck.remaining
            var currentDeck = state.deck
            while (remainingCards > 0) {
                val deal = currentDeck.deal(1)
                deckCards.add(deal.cards[0])
                currentDeck = deal.deck
                remainingCards = deal.deck.remaining
            }

            // Check that no card appears in both board and deck
            val duplicates = boardCards.intersect(deckCards)
            assertFalse(duplicates.isNotEmpty()) {
                "State $state has duplicate card(s) $duplicates on both board and in deck"
            }
        }
    }
}
