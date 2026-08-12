package duels.poker.server.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * [ConnectionWriter] is the single-writer funnel for a connection: any number of producers may
 * [ConnectionWriter.send] a frame, but only the coroutine running [ConnectionWriter.writeAll]
 * ever touches the sink. These tests append to a plain [MutableList] from inside `writeAll` only
 * — safe precisely because the class under test guarantees a single consumer.
 */
internal class ConnectionWriterTest {

    /** Runs two producers, 500 distinct frames each, against one writer and returns what arrived. */
    private suspend fun runTwoProducers(): List<String> {
        val writer = ConnectionWriter()
        val sink = mutableListOf<String>()

        return coroutineScope {
            val writerJob = launch { writer.writeAll { frame -> sink.add(frame) } }
            val producerA = launch(Dispatchers.Default) {
                repeat(500) { i -> writer.send("a-$i") }
            }
            val producerB = launch(Dispatchers.Default) {
                repeat(500) { i -> writer.send("b-$i") }
            }
            producerA.join()
            producerB.join()
            writer.close()
            writerJob.join()
            sink
        }
    }

    @Test
    @Timeout(30)
    fun everyFrameArrivesWholeAndExactlyOnce(): Unit = runBlocking {
        val collected = runTwoProducers()

        val expected = (0 until 500).flatMap { i -> listOf("a-$i", "b-$i") }.toSet()
        assertEquals(1_000, collected.size)
        assertEquals(expected, collected.toSet())
    }

    @Test
    @Timeout(30)
    fun framesFromOneProducerKeepTheirOrder(): Unit = runBlocking {
        val collected = runTwoProducers()

        val aIndices = collected.filter { it.startsWith("a-") }.map { it.removePrefix("a-").toInt() }
        val bIndices = collected.filter { it.startsWith("b-") }.map { it.removePrefix("b-").toInt() }
        assertEquals((0 until 500).toList(), aIndices)
        assertEquals((0 until 500).toList(), bIndices)
    }

    @Test
    fun writeAllReturnsWhenTheWriterIsClosed(): Unit = runBlocking {
        val writer = ConnectionWriter()
        val sink = mutableListOf<String>()

        val writerJob = launch { writer.writeAll { frame -> sink.add(frame) } }
        writer.close()
        writerJob.join()

        assertTrue(sink.isEmpty())
    }

    @Test
    fun sendingAfterCloseReturnsFalse(): Unit = runBlocking {
        val writer = ConnectionWriter()
        val sink = mutableListOf<String>()

        writer.close()
        val result = writer.send("x")
        writer.writeAll { frame -> sink.add(frame) }

        assertFalse(result)
        assertTrue(sink.isEmpty())
    }

    @Test
    fun closeIsIdempotent(): Unit = runBlocking {
        val writer = ConnectionWriter()
        val sink = mutableListOf<String>()

        val writerJob = launch { writer.writeAll { frame -> sink.add(frame) } }
        writer.close()
        writer.close()
        writerJob.join()

        assertTrue(sink.isEmpty())
    }

    @Test
    @Timeout(30)
    fun sendSuspendsWhenTheBufferIsFull(): Unit = runBlocking {
        val writer = ConnectionWriter(capacity = 1)
        val sink = mutableListOf<String>()

        val producer = launch(Dispatchers.Default) {
            writer.send("1")
            writer.send("2")
            writer.send("3")
        }

        // Capacity 1 with no consumer buffers only the first frame; the second send has nowhere
        // to go and must suspend, so the producer cannot have finished yet.
        delay(100)
        assertFalse(producer.isCompleted)

        val writerJob = launch { writer.writeAll { frame -> sink.add(frame) } }
        producer.join()
        writer.close()
        writerJob.join()

        assertEquals(listOf("1", "2", "3"), sink)
    }
}
