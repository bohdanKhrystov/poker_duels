package duels.poker.server.room

import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ThreadLocalRandom

private fun newPlayerId(index: Int): PlayerId = PlayerId("creator-$index")

/**
 * A [RoomCodeSource] drawing from a fixed, tiny pool of [size] codes instead of the full
 * [RoomCode] alphabet.
 *
 * With a full-entropy source, two concurrent creators minting the same code is astronomically
 * unlikely, so a concurrency test against it would pass whether or not the registry's insert is
 * actually atomic. Shrinking the pool to a handful of codes makes collisions near-certain under
 * concurrent load, so [RoomRegistry.create]'s `putIfAbsent` retry loop is genuinely exercised.
 */
private class TinyCodeSource(size: Int) : RoomCodeSource {
    private val pool = (0 until size).map { RoomCode("0000000$it") }

    override fun newRoomCode(): RoomCode = pool[ThreadLocalRandom.current().nextInt(pool.size)]
}

internal class RoomRegistryConcurrencyTest {

    @Test
    @Timeout(60)
    fun concurrentCreatorsAllReceiveDistinctCodes() = runBlocking(Dispatchers.Default) {
        val registry = RoomRegistry(RandomRoomCodeSource(), MutableClock())
        val gate = CompletableDeferred<Unit>()
        val creators = 100

        val jobs = (0 until creators).map { i ->
            async {
                gate.await()
                registry.create(newPlayerId(i))
            }
        }
        gate.complete(Unit)
        val rooms = jobs.awaitAll()

        val codes = rooms.map { it.code }
        assertEquals(creators, codes.toSet().size)
        assertEquals(creators, registry.size)
    }

    @Test
    @Timeout(60)
    fun aTinyCodeSpaceForcesCollisionsAndStillNeverDuplicates() = runBlocking(Dispatchers.Default) {
        val codeSpace = 8
        val creators = 50
        val registry = RoomRegistry(TinyCodeSource(codeSpace), MutableClock())
        val gate = CompletableDeferred<Unit>()

        val jobs = (0 until creators).map { i ->
            async {
                gate.await()
                runCatching { registry.create(newPlayerId(i)) }
            }
        }
        gate.complete(Unit)
        val results = jobs.awaitAll()

        val successes = results.mapNotNull { it.getOrNull() }
        val failures = results.mapNotNull { it.exceptionOrNull() }

        assertEquals(creators, successes.size + failures.size)
        assertTrue(successes.isNotEmpty())
        assertEquals(successes.size, successes.map { it.code }.toSet().size, "two successes shared a code")
        assertTrue(successes.size <= codeSpace)
        assertTrue(failures.all { it is IllegalStateException })
        assertEquals(successes.size, registry.size)
    }
}
