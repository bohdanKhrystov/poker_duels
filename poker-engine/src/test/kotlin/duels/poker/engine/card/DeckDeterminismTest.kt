package duels.poker.engine.card

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * WARNING: The recorded orderings in this test are contractual. Changing either string is a
 * breaking change to every stored replay and must be an ADR, not a test edit. Do not regenerate
 * them from the current output. If a test fails, the implementation deviates from the algorithm
 * pinned in TASK-010205 and TASK-010207 — fix the implementation, not the test.
 */
class DeckDeterminismTest {
    @Test
    fun seedFortyTwoProducesTheRecordedOrdering() {
        val ordering = Deck.full().shuffled(SplitMix64Rng(42)).deck.deal(52).cards.joinToString(" ")
        val expected = "9h Ad 4h 8s Js 9s Qs Kh Ts 5d 4d Tc Kc 2h Th Ks 8h Td As 3s 9c 8d 6d 4c 6s 3h Ah Qd 7c 5h 5c Ac Jh Qh 5s Jc 9d 7h 8c 3c 2c 2d Jd 2s 6h 6c Qc Kd 3d 7d 7s 4s"
        assertEquals(expected, ordering)
    }

    @Test
    fun seedSevenProducesTheRecordedOrdering() {
        val ordering = Deck.full().shuffled(SplitMix64Rng(7)).deck.deal(52).cards.joinToString(" ")
        val expected = "9d Qs 7c 3d 3c 9h Th 2s 4h 8d 2c 9c 5d 2d 9s 5h Jh 7h 4s Qd 7s Jc 8s Ks 4c 6c Kh 6s Kc 4d 3h 5c Qc Td As Ad 8h Ah 2h 3s 5s 6d Ac 7d Jd Qh Kd Ts Js Tc 8c 6h"
        assertEquals(expected, ordering)
    }
}
