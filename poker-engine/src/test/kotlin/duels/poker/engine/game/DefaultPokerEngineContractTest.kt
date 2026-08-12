package duels.poker.engine.game

import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng

internal class DefaultPokerEngineContractTest : PokerEngineContract() {
    override fun engine(): PokerEngine = DefaultPokerEngine

    override fun cases(): List<Pair<GameState, PlayerAction>> {
        // The four additional positions for DefaultPokerEngine
        val openingState = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState
        val afterBigBlindCall = DefaultPokerEngine.handle(openingState, PlayerAction.Call(0)).newState

        val flopPosition = handState().copy(
            street = Street.FLOP,
            board = Board(cards("2c 3c 4c")),
            betToMatch = 300,
            seatToAct = 0,
        )

        val allInPosition = handState().copy(
            street = Street.FLOP,
            board = Board(cards("2c 3c 4c")),
            seats = listOf(
                handState().seat(0),
                handState().seat(1).copy(isAllIn = true),
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
}
