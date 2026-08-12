package duels.poker.server.session

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException

/** Default capacity of the outbound frame buffer, chosen to absorb a burst without blocking. */
private const val DEFAULT_CAPACITY = 64

/**
 * A single connection's outbound mailbox.
 *
 * A WebSocket session is not safe for concurrent writes: two coroutines calling `send` on the
 * same socket can interleave frames and corrupt the stream. [ConnectionWriter] makes that
 * impossible by construction — any number of producer coroutines may [send] a frame, but exactly
 * one coroutine, the one running [writeAll], ever touches the socket.
 *
 * This class knows about strings and channels only. It has no dependency on Ktor, a session or
 * the wire protocol; the socket is injected into [writeAll] as a lambda.
 */
public class ConnectionWriter(capacity: Int = DEFAULT_CAPACITY) {
    private val frames = Channel<String>(capacity)

    /**
     * Offers [frame] to the writer, suspending while the buffer is full so that a fast producer
     * is throttled by backpressure rather than dropping frames.
     *
     * Returns `false`, without throwing, if this writer is already [close]d — a producer racing a
     * closing socket is a normal event, not a failure worth crashing its coroutine over.
     */
    public suspend fun send(frame: String): Boolean =
        try {
            frames.send(frame)
            true
        } catch (_: ClosedSendChannelException) {
            false
        }

    /**
     * Marks this writer closed. No further [send] succeeds, but frames already buffered are still
     * delivered to [writeAll] before it returns.
     *
     * Idempotent and non-suspending, so it is safe to call unconditionally from a connection's
     * `finally` block, however many times that block runs.
     */
    public fun close() {
        frames.close()
    }

    /**
     * Drains buffered frames to [write], one at a time, in the order they were sent.
     *
     * Exactly one coroutine may call this per writer — [write] is the only place a frame reaches
     * the socket, and calling it from more than one coroutine at once reopens the interleaving
     * problem this class exists to prevent. Returns once this writer has been [close]d and the
     * buffer is drained; it does not swallow a failure from [write], it propagates it to the
     * caller.
     *
     * **The caller must [close] this writer in a `finally`.** If [write] throws — a socket dying
     * mid-frame is the ordinary case — the exception propagates out of here and this coroutine
     * ends, but the channel stays open. Anything still calling [send] would then suspend forever
     * on a writer nobody is draining, leaking a coroutine per dead connection. [close] is
     * idempotent, so calling it in a `finally` is safe however this function exited.
     */
    public suspend fun writeAll(write: suspend (String) -> Unit) {
        for (frame in frames) {
            write(frame)
        }
    }
}
