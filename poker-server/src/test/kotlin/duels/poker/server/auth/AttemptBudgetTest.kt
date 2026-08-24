package duels.poker.server.auth

import duels.poker.server.time.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

class AttemptBudgetTest {

    @Test
    fun theFirstAttemptsAreAdmitted(): Unit = runBlocking {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), MutableClock())

        assertTrue(budget.admit("k"))
        assertTrue(budget.admit("k"))
        assertTrue(budget.admit("k"))
    }

    @Test
    fun theFourthIsRefused(): Unit = runBlocking {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), MutableClock())

        repeat(3) { assertTrue(budget.admit("k")) }
        assertFalse(budget.admit("k"))
    }

    @Test
    fun theWindowRollsForward(): Unit = runBlocking {
        val clock = MutableClock()
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), clock)

        repeat(3) { assertTrue(budget.admit("k")) }

        // Exactly windowMillis later, each of the three recorded attempts is exactly
        // windowMillis old. Pruning on `>=` throws them out here; pruning on `>` (i.e. keeping
        // entries whose age equals windowMillis) would not, and this call would answer false
        // instead. This is the fixture that pins the < vs <= boundary.
        clock.advance(1_000)
        assertTrue(budget.admit("k"))
    }

    @Test
    fun anOverBudgetAttemptStillCounts(): Unit = runBlocking {
        val clock = MutableClock()
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 1, windowMillis = 1_000), clock)

        assertTrue(budget.admit("k")) // the one slot, at t=0 -- at the limit

        clock.advance(500)
        assertFalse(budget.admit("k")) // over budget; still recorded, at t=500

        // t=1001 is past the ORIGINAL attempt's own tail (0 + 1000 = 1000): a budget that
        // returns early without recording once exhausted would find nothing left at this
        // point and admit. The refused attempt recorded at t=500 is only 501ms old here --
        // its own tail is at 1500 -- so it is still on the clock and the address is still
        // refused. Nothing else in this file can tell those two implementations apart.
        clock.advance(501)
        assertFalse(budget.admit("k"))
    }

    @Test
    fun twoKeysAreTwoBudgets(): Unit = runBlocking {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), MutableClock())

        repeat(3) { assertTrue(budget.admit("a")) }
        assertFalse(budget.admit("a")) // "a" is exhausted

        assertTrue(budget.admit("b")) // "b" has its own, untouched budget
    }

    @Test
    fun theClockIsReadAndNotAssumed(): Unit = runBlocking {
        val frozenClock = MutableClock()
        val frozenBudget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), frozenClock)
        repeat(3) { assertTrue(frozenBudget.admit("k")) }
        // The clock never advances, so nothing recorded ever ages out: refused forever.
        repeat(5) { assertFalse(frozenBudget.admit("k")) }

        val jumpingClock = MutableClock()
        val jumpingBudget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), jumpingClock)
        // The clock jumps a full window before every call, so the previous attempt has
        // always just aged out: never refused, however many calls are made.
        repeat(10) {
            assertTrue(jumpingBudget.admit("k"))
            jumpingClock.advance(1_000)
        }
    }

    @Test
    @Timeout(60)
    fun concurrentCallersDoNotOverspend() = runBlocking(Dispatchers.Default) {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 5, windowMillis = 60_000), MutableClock())
        val gate = CompletableDeferred<Unit>()
        // 50 concurrent callers against this critical section only exposed a missing Mutex on
        // 4 of 5 runs measured while writing this test -- a flaky guard for the property it
        // exists to check. 500 failed all 4 of 4 measured runs, with the correct implementation
        // still passing every time, so that is the count kept here.
        val callers = 500

        val jobs = (0 until callers).map {
            async {
                gate.await()
                budget.admit("a")
            }
        }
        gate.complete(Unit)
        val results = jobs.awaitAll()

        assertEquals(5, results.count { it })
    }

    @Test
    fun aRefundedSlotIsSpendableAgain(): Unit = runBlocking {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), MutableClock())

        repeat(3) { assertTrue(budget.admit("k")) }

        budget.refund("k")

        assertTrue(budget.admit("k")) // the refunded slot is spendable again
        assertFalse(budget.admit("k")) // and only that one slot -- not the whole window
    }

    @Test
    fun refundingAnUnknownKeyDoesNothing(): Unit = runBlocking {
        val budget = AttemptBudget(AttemptLimits(maxAttempts = 3, windowMillis = 1_000), MutableClock())

        budget.refund("never-seen")

        // "never-seen" still holds its full limit.
        repeat(3) { assertTrue(budget.admit("never-seen")) }
        assertFalse(budget.admit("never-seen"))

        // Every other key is untouched too.
        repeat(3) { assertTrue(budget.admit("another-key")) }
        assertFalse(budget.admit("another-key"))
    }
}
