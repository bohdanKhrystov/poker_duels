package duels.poker.server.db

import duels.poker.server.auth.PresentedSecret
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SECRET = PresentedSecret("correct horse battery staple")

// Clearly more than ARGON2_MAX_PARALLEL: with only four probes, a dispatcher that let every one
// of them through immediately, one after another — never running two at once — could still show
// a peak of four by accident of scheduling. Launching a dozen makes "one at a time" and "four at
// once" produce readings that cannot be confused for each other.
private const val PROBE_COUNT = 12

// Bounds how long a probe holds its dispatcher slot open waiting for the rest of its generation
// to join it before giving up. Long enough that a probe the dispatcher schedules a little after
// its peers still arrives and is counted alongside them, at any bound this file might declare;
// short enough that the whole test finishes in a few seconds even at a bound of one, where every
// probe waits out the full timeout on its own.
private const val HOLD_MILLIS = 300L

/**
 * Tests for [argon2Dispatcher] itself, not for [Argon2Hasher].
 *
 * [exactlyFourCoroutinesRunAtOnceOnTheArgon2Dispatcher] proves the dispatcher's bound — that
 * coroutines launched on it never observe more than four peers running at once, and that four
 * peers really do run at once, so the ceiling was exercised rather than trivially satisfied by
 * work that never overlapped. It says nothing about whether [Argon2Hasher] actually dispatches
 * onto [argon2Dispatcher]; that is what the second `verify` command's grep checks, over
 * `Argon2Hasher.kt` itself.
 *
 * [eightConcurrentVerificationsAllSucceed] proves the bound is safe to use for real work: a
 * blocking call made from inside a coroutine running on a [kotlinx.coroutines.CoroutineDispatcher]
 * with limited parallelism can starve or deadlock the dispatcher if the limit is undersized for
 * the calls it serialises. Eight real [Argon2Hasher.matches] calls, all succeeding, rules that out
 * for the bound this file declares.
 */
class Argon2ConcurrencyTest {
    @Test
    fun exactlyFourCoroutinesRunAtOnceOnTheArgon2Dispatcher() {
        runBlocking {
            val running = AtomicInteger(0)
            val peak = AtomicInteger(0)
            // Opens only once every one of the PROBE_COUNT probes has joined it, or after
            // HOLD_MILLIS gives up on the rest of the generation arriving — never the instant a
            // handful happen to arrive first. That is what keeps a probe's slot observably open
            // long enough for a slower peer to join it, so a peak above the real bound would show
            // up here instead of hiding behind a gate a few fast arrivals already opened.
            val everyoneArrived = CountDownLatch(PROBE_COUNT)
            val completed = CountDownLatch(PROBE_COUNT)

            val jobs =
                List(PROBE_COUNT) {
                    async(argon2Dispatcher) {
                        val liveNow = running.incrementAndGet()
                        peak.updateAndGet { previousPeak -> maxOf(previousPeak, liveNow) }
                        everyoneArrived.countDown()
                        // Blocks the real thread the dispatcher handed this coroutine — the same
                        // way the Argon2 work it stands in for holds one — until this whole
                        // generation has joined it or it gives up waiting.
                        everyoneArrived.await(HOLD_MILLIS, TimeUnit.MILLISECONDS)
                        running.decrementAndGet()
                        completed.countDown()
                    }
                }

            // A safety valve, not the proof: every probe's own wait above is already bounded by
            // HOLD_MILLIS, so twelve of them cannot take more than twelve times that even run one
            // at a time. A timeout here means a probe never got scheduled at all — the dispatcher
            // is stuck, not merely bounded smaller than four; that case is caught below, by peak
            // falling short of four.
            val allCompleted = completed.await(10, TimeUnit.SECONDS)
            assertTrue(allCompleted, "not every probe finished within ten seconds — argon2Dispatcher looks stuck")
            jobs.awaitAll()

            // Equality, not <=: a bound of one would also keep peak within four, and is a
            // different, equally broken dispatcher.
            assertEquals(ARGON2_MAX_PARALLEL, peak.get())
        }
    }

    @Test
    fun eightConcurrentVerificationsAllSucceed() {
        runBlocking {
            val hasher = Argon2Hasher()
            val phc = hasher.hash(SECRET)

            val results = List(8) { async { hasher.matches(SECRET, phc) } }.awaitAll()

            assertEquals(List(8) { true }, results)
        }
    }
}
