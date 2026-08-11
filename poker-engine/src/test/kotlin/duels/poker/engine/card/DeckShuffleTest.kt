package duels.poker.engine.card

import duels.poker.engine.random.SplitMix64Rng
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeckShuffleTest {

    @Test
    fun shuffleIsAPermutationOfTheDeck() {
        runBlocking {
            forAll(Arb.long()) { seed ->
                val shuffle = Deck.full().shuffled(SplitMix64Rng(seed))
                val dealt = shuffle.deck.deal(52).cards
                dealt.toSet() == Card.all.toSet()
            }
        }
    }

    @Test
    fun sameSeedShufflesIdentically() {
        val first = Deck.full().shuffled(SplitMix64Rng(2024)).deck.deal(52).cards
        val second = Deck.full().shuffled(SplitMix64Rng(2024)).deck.deal(52).cards

        assertEquals(first, second)
    }

    @Test
    fun differentSeedsShuffleDifferently() {
        val first = Deck.full().shuffled(SplitMix64Rng(1)).deck.deal(52).cards
        val second = Deck.full().shuffled(SplitMix64Rng(2)).deck.deal(52).cards

        assertNotEquals(first, second)
    }

    @Test
    fun shuffleReturnsAnAdvancedRng() {
        val rng = SplitMix64Rng(7)
        val firstShuffle = Deck.full().shuffled(rng)

        assertNotEquals(rng, firstShuffle.rng)

        val firstOrder = firstShuffle.deck.deal(52).cards
        val secondOrder = Deck.full().shuffled(firstShuffle.rng).deck.deal(52).cards

        assertNotEquals(firstOrder, secondOrder)
    }

    @Test
    fun shuffleDoesNotMutateTheReceiver() {
        val deck = Deck.full()
        deck.shuffled(SplitMix64Rng(99))

        assertEquals(Card.all, deck.deal(52).cards)
    }

    @Test
    fun shufflesOnlyTheCardsThatRemain() {
        val deal = Deck.full().deal(10)
        val dealtCards = deal.cards.toSet()

        val shuffled = deal.deck.shuffled(SplitMix64Rng(123)).deck

        assertEquals(42, shuffled.remaining)
        val remainingCards = shuffled.deal(42).cards.toSet()
        assertTrue(remainingCards.none { it in dealtCards })
    }
}
