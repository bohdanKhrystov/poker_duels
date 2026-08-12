package duels.poker.server.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionRegistryTest {
    private fun session(player: String) =
        Session(SessionRegistry.newSessionId(), Player(PlayerId(player), DeviceId(player)))

    @Test
    fun aRegisteredSessionIsFoundById() {
        val registry = SessionRegistry()
        val s = session("alice")

        registry.register(s)

        assertEquals(s, registry.get(s.id))
        assertEquals(1, registry.size)
    }

    @Test
    fun removeReturnsTheSessionOnceAndNullAfterwards() {
        val registry = SessionRegistry()
        val s = session("alice")
        registry.register(s)

        val first = registry.remove(s.id)
        val second = registry.remove(s.id)

        assertEquals(s, first)
        assertNull(second)
        assertEquals(0, registry.size)
    }

    @Test
    fun removingAnUnknownIdReturnsNull() {
        val registry = SessionRegistry()

        val removed = registry.remove(SessionRegistry.newSessionId())

        assertNull(removed)
    }

    @Test
    fun sessionsOfListsEverySessionForThatPlayer() {
        val registry = SessionRegistry()
        val alice1 = Session(SessionRegistry.newSessionId(), Player(PlayerId("alice"), DeviceId("d1")))
        val alice2 = Session(SessionRegistry.newSessionId(), Player(PlayerId("alice"), DeviceId("d2")))
        val bob = session("bob")

        registry.register(alice1)
        registry.register(alice2)
        registry.register(bob)

        val aliceSessions = registry.sessionsOf(PlayerId("alice"))

        assertEquals(setOf(alice1, alice2), aliceSessions.toSet())
        assertEquals(2, aliceSessions.size)
    }

    @Test
    fun newSessionIdsAreDistinct() {
        val ids = (0 until 10_000).map { SessionRegistry.newSessionId() }.toHashSet()

        assertEquals(10_000, ids.size)
    }

    @Test
    fun concurrentRegisterAndRemoveLeavesTheRegistryEmpty(): Unit = runBlocking {
        val registry = SessionRegistry()
        val sessions = (0 until 1_000).map { session("player-$it") }

        val registrations = sessions.map { s ->
            launch(Dispatchers.Default) { registry.register(s) }
        }
        registrations.forEach { it.join() }

        // Race several removers per session on real threads: only one may ever observe
        // the removed session, proving `remove` is exactly-once under genuine contention.
        val successfulRemovals = AtomicInteger(0)
        val removers = sessions.flatMap { s ->
            (0 until 5).map {
                launch(Dispatchers.Default) {
                    if (registry.remove(s.id) != null) {
                        successfulRemovals.incrementAndGet()
                    }
                }
            }
        }
        removers.forEach { it.join() }

        assertEquals(0, registry.size)
        assertEquals(1_000, successfulRemovals.get())
        assertTrue(removers.size > sessions.size)
    }
}
