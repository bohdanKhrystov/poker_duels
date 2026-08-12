package duels.poker.engine.game

import duels.poker.engine.card.cards
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlayerViewTest {
    private fun validPlayerView(
        viewerSeat: Int = 0,
        handNumber: Int = 1,
        buttonSeat: Int = 0,
        street: Street = Street.PREFLOP,
        board: Board = Board.EMPTY,
        pot: Int = 0,
        betToMatch: Int = 0,
        minRaiseTo: Int = 0,
        seatToAct: Int? = 0,
        smallBlind: Int = 1,
        bigBlind: Int = 2,
        seats: List<SeatView> = listOf(
            SeatView(
                index = 0,
                stack = 1000,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
            ),
            SeatView(
                index = 1,
                stack = 1000,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
            ),
        ),
    ) = PlayerView(
        viewerSeat = viewerSeat,
        handNumber = handNumber,
        buttonSeat = buttonSeat,
        street = street,
        board = board,
        pot = pot,
        betToMatch = betToMatch,
        minRaiseTo = minRaiseTo,
        seatToAct = seatToAct,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        seats = seats,
    )

    @Test
    fun carriesTheFieldsItWasBuiltWith() {
        val fiveCardBoard = Board(cards("As Kd Qh Jc Ts"))
        val view = validPlayerView(
            viewerSeat = 1,
            handNumber = 42,
            buttonSeat = 1,
            street = Street.RIVER,
            board = fiveCardBoard,
            pot = 500,
            betToMatch = 100,
            minRaiseTo = 200,
            seatToAct = null,
            smallBlind = 10,
            bigBlind = 20,
        )

        view.viewerSeat shouldBe 1
        view.handNumber shouldBe 42
        view.buttonSeat shouldBe 1
        view.street shouldBe Street.RIVER
        view.board shouldBe fiveCardBoard
        view.pot shouldBe 500
        view.betToMatch shouldBe 100
        view.minRaiseTo shouldBe 200
        view.seatToAct shouldBe null
        view.smallBlind shouldBe 10
        view.bigBlind shouldBe 20
        view.seats.size shouldBe 2
    }

    @Test
    fun viewerAndOpponentNameTheRightSeats() {
        val view = validPlayerView(viewerSeat = 1)

        view.viewer.index shouldBe 1
        view.opponent.index shouldBe 0
    }

    @Test
    fun rejectsAViewerSeatOutsideZeroOrOne() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(viewerSeat = 2)
        }
    }

    @Test
    fun rejectsOtherThanTwoSeats() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(
                seats = listOf(
                    SeatView(
                        index = 0,
                        stack = 1000,
                        committedThisStreet = 0,
                        committedThisHand = 0,
                        hasFolded = false,
                        isAllIn = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsSeatsOutOfIndexOrder() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(
                seats = listOf(
                    SeatView(
                        index = 1,
                        stack = 1000,
                        committedThisStreet = 0,
                        committedThisHand = 0,
                        hasFolded = false,
                        isAllIn = false,
                    ),
                    SeatView(
                        index = 0,
                        stack = 1000,
                        committedThisStreet = 0,
                        committedThisHand = 0,
                        hasFolded = false,
                        isAllIn = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsANegativePot() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(pot = -1)
        }
    }

    @Test
    fun rejectsASeatToActOutsideZeroOrOne() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(seatToAct = 2)
        }
    }

    @Test
    fun acceptsNullSeatToAct() {
        val view = validPlayerView(seatToAct = null)
        view.seatToAct shouldBe null
    }

    @Test
    fun rejectsBlindsThatDoNotAscend() {
        shouldThrow<IllegalArgumentException> {
            validPlayerView(smallBlind = 100, bigBlind = 100)
        }
    }
}
