package duels.poker.engine.duel

import duels.poker.engine.game.otherSeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * Duels nobody designed, each reproducible from its seed alone: the button alternates, every hand
 * plays the blinds its schedule promises, the ladder only ever climbs, and the chip total across
 * both stacks never moves. `RandomDuelPlayer` already owns per-action betting and per-hand
 * settlement — see `BettingInvariantTest` and `SettlementInvariantTest` — so these assertions look
 * only at what a whole duel adds: the button, the blind ladder and the chips across hands.
 */
class DuelInvariantTest {

    private val seeds = 1L..20L
    private val fastLadder =
        DuelFormat.DEFAULT.copy(
            blinds = BlindSchedule(DuelFormat.DEFAULT.blinds.levels, handsPerLevel = 1),
        )

    @Test
    @Timeout(60)
    fun theButtonAlternatesEveryHand() {
        for (seed in seeds) {
            val played = playRandomDuel(seed)
            val hands = played.hands

            assertEquals(
                (seed % 2).toInt(),
                hands.first().buttonSeat,
                "seed $seed: expected the first hand's button to be seed % 2",
            )

            for (i in 1 until hands.size) {
                val previous = hands[i - 1]
                val current = hands[i]
                assertEquals(
                    otherSeat(previous.buttonSeat),
                    current.buttonSeat,
                    "seed $seed: expected hand ${current.handNumber}'s button to be the other seat " +
                        "from hand ${previous.handNumber}'s button (${previous.buttonSeat})",
                )
            }
        }
    }

    @Test
    @Timeout(60)
    fun everyHandUsesTheScheduledBlinds() {
        for (seed in seeds) {
            val played = playRandomDuel(seed)

            for (hand in played.hands) {
                assertEquals(
                    played.format.blinds.blindsFor(hand.handNumber),
                    hand.blinds,
                    "seed $seed: expected hand ${hand.handNumber} to play the blinds its schedule " +
                        "promised",
                )
            }
        }
    }

    @Test
    @Timeout(60)
    fun theBlindLevelRisesBetweenHandsAndNeverFalls() {
        var sawARise = false

        for (seed in seeds) {
            val played = playRandomDuel(seed, format = fastLadder)
            val hands = played.hands

            if (!sawARise && hands.size >= 2) {
                assertTrue(
                    hands[1].blinds.bigBlind > hands[0].blinds.bigBlind,
                    "seed $seed: expected the second hand's big blind (${hands[1].blinds.bigBlind}) " +
                        "to be greater than the first's (${hands[0].blinds.bigBlind}) under fastLadder",
                )
                sawARise = true
            }

            for (i in 1 until hands.size) {
                assertTrue(
                    hands[i].blinds.bigBlind >= hands[i - 1].blinds.bigBlind,
                    "seed $seed: expected hand ${hands[i].handNumber}'s big blind " +
                        "(${hands[i].blinds.bigBlind}) to be at least hand ${hands[i - 1].handNumber}'s " +
                        "(${hands[i - 1].blinds.bigBlind})",
                )
            }
        }

        assertTrue(sawARise, "expected at least one seed's fastLadder duel to reach a second hand")

        for (seed in seeds) {
            val played = playRandomDuel(seed)
            val hands = played.hands

            for (i in 1 until hands.size) {
                assertTrue(
                    hands[i].blinds.bigBlind >= hands[i - 1].blinds.bigBlind,
                    "seed $seed: expected hand ${hands[i].handNumber}'s big blind " +
                        "(${hands[i].blinds.bigBlind}) to be at least hand ${hands[i - 1].handNumber}'s " +
                        "(${hands[i - 1].blinds.bigBlind}) under DuelFormat.DEFAULT",
                )
            }
        }
    }

    @Test
    @Timeout(60)
    fun chipsAreConstantFromTheFirstDealToTheLast() {
        val expectedTotal = 2 * DuelFormat.DEFAULT.startingStack

        for (seed in seeds) {
            val played = playRandomDuel(seed)

            for (hand in played.hands) {
                assertEquals(
                    expectedTotal,
                    hand.stacksAfter.sum(),
                    "seed $seed: expected hand ${hand.handNumber}'s stacks to sum to $expectedTotal",
                )
                assertTrue(
                    hand.stacksAfter.all { it >= 0 },
                    "seed $seed: expected hand ${hand.handNumber}'s stacks to be non-negative, " +
                        "got ${hand.stacksAfter}",
                )
            }

            assertEquals(
                expectedTotal,
                played.outcome.finalStacks.sum(),
                "seed $seed: expected the outcome's final stacks to sum to $expectedTotal",
            )
        }
    }
}
