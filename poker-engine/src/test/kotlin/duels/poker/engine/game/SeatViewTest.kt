package duels.poker.engine.game

import duels.poker.engine.card.Card
import duels.poker.engine.card.Rank
import duels.poker.engine.card.Suit
import duels.poker.engine.card.cards
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class SeatViewTest {
    @Test
    fun hidesHoleCardsByDefault() {
        val seat = SeatView(
            index = 0,
            stack = 1000,
            committedThisStreet = 0,
            committedThisHand = 0,
            hasFolded = false,
            isAllIn = false,
        )
        seat.holeCards shouldBe emptyList()
    }

    @Test
    fun carriesTheHoleCardsItIsGiven() {
        val hole = cards("As Kh")
        val seat = SeatView(
            index = 0,
            stack = 1000,
            committedThisStreet = 0,
            committedThisHand = 0,
            hasFolded = false,
            isAllIn = false,
            holeCards = hole,
        )
        seat.holeCards shouldBe hole
    }

    @Test
    fun rejectsASeatIndexOutsideZeroOrOne() {
        shouldThrow<IllegalArgumentException> {
            SeatView(
                index = 2,
                stack = 1000,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
            )
        }
    }

    @Test
    fun rejectsANegativeStack() {
        shouldThrow<IllegalArgumentException> {
            SeatView(
                index = 0,
                stack = -1,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
            )
        }
    }

    @Test
    fun rejectsOneHoleCard() {
        shouldThrow<IllegalArgumentException> {
            SeatView(
                index = 0,
                stack = 1000,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
                holeCards = cards("As"),
            )
        }
    }

    @Test
    fun rejectsDuplicateHoleCards() {
        shouldThrow<IllegalArgumentException> {
            val duplicateCard = Card.of(Rank.ACE, Suit.SPADES)
            SeatView(
                index = 0,
                stack = 1000,
                committedThisStreet = 0,
                committedThisHand = 0,
                hasFolded = false,
                isAllIn = false,
                holeCards = listOf(duplicateCard, duplicateCard),
            )
        }
    }

    @Test
    fun rejectsAStreetCommitmentAboveTheHandCommitment() {
        shouldThrow<IllegalArgumentException> {
            SeatView(
                index = 0,
                stack = 1000,
                committedThisStreet = 200,
                committedThisHand = 100,
                hasFolded = false,
                isAllIn = false,
            )
        }
    }

    @Test
    fun roundTripsThroughJson() {
        val original = SeatView(
            index = 0,
            stack = 1000,
            committedThisStreet = 50,
            committedThisHand = 150,
            hasFolded = false,
            isAllIn = false,
            holeCards = cards("As Kh"),
        )
        val encoded = Json.encodeToString(SeatView.serializer(), original)
        val decoded = Json.decodeFromString(SeatView.serializer(), encoded)
        decoded shouldBe original
    }
}
