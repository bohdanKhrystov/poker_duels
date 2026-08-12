package duels.poker.server.protocol

import duels.poker.engine.card.Card
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.Board
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.SeatView
import duels.poker.engine.game.Street

/** A `PlayerView` with typical values for testing and sampling. */
internal fun sampleView(): PlayerView {
    return PlayerView(
        viewerSeat = 0,
        handNumber = 3,
        buttonSeat = 0,
        street = Street.FLOP,
        board = Board(listOf(Card.parse("2c"), Card.parse("7d"), Card.parse("9s"))),
        pot = 300,
        betToMatch = 0,
        minRaiseTo = 100,
        seatToAct = 0,
        smallBlind = 50,
        bigBlind = 100,
        seats = listOf(
            SeatView(
                index = 0,
                stack = 900,
                committedThisStreet = 0,
                committedThisHand = 100,
                hasFolded = false,
                isAllIn = false,
                holeCards = listOf(Card.parse("As"), Card.parse("Kh")),
            ),
            SeatView(
                index = 1,
                stack = 900,
                committedThisStreet = 0,
                committedThisHand = 100,
                hasFolded = false,
                isAllIn = false,
                holeCards = emptyList(),
            ),
        ),
    )
}

/** A `LegalActions` with typical values for testing and sampling. */
internal fun sampleLegalActions(): LegalActions {
    return LegalActions(
        seat = 0,
        allowed = setOf(ActionType.CHECK, ActionType.BET),
        minBetTo = 100,
        allInTo = 900,
    )
}
