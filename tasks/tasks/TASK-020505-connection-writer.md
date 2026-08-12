---
schema: 2
id: TASK-020505
title: One writer per connection, fed by a channel
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, session, concurrency]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ConnectionWriterTest'
  - ./gradlew :poker-server:check
---

## Goal

A connection has one `ConnectionWriter`: any number of coroutines may hand it a frame, exactly one
coroutine writes them out, and a frame is never half-written between two others.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/ConnectionWriter.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/ConnectionWriterTest.kt` | create |

Read, do not modify: nothing else. This class knows about strings and channels, not about Ktor,
sessions or the protocol.

## Scope

- One main file, package `duels.poker.server.session`, KDoc on everything public:

  ```kotlin
  private const val DEFAULT_CAPACITY = 64

  public class ConnectionWriter(capacity: Int = DEFAULT_CAPACITY) {
      private val frames = Channel<String>(capacity)

      public suspend fun send(frame: String): Boolean
      public fun close()
      public suspend fun writeAll(write: suspend (String) -> Unit)
  }
  ```

- `send` offers a frame and suspends when the buffer is full — backpressure, not a dropped frame.
  It returns `false` instead of throwing when the writer is already closed, because a producer
  racing a closing socket is normal and must not blow up its coroutine. Catch
  `ClosedSendChannelException` specifically; do not widen it.
- `writeAll` is the single writer: `for (frame in frames) write(frame)`, returning when the writer
  is closed and the buffer is drained. Its KDoc states that exactly one coroutine may call it per
  writer, and that `write` is the only place a frame reaches the socket.
- `close` is idempotent and non-suspending — it is called from a connection's `finally`.
- No Ktor type appears in this file. The socket is injected as the `write` lambda, which is what
  lets this be tested without a server at all.

## Out of scope

- Installing the writer on a socket — `TASK-020507` launches `writeAll` inside the `/ws` handler.
- Fan-out to another player's connection, broadcast, or a room — `STORY-0207`.
- Retry, replay or an outbound buffer that survives a disconnect — `STORY-0208`.
- Encoding: this class moves already-encoded frames. `ProtocolCodec.encode` stays at the call site.

## Tests

`ConnectionWriterTest`, JUnit 5, package `duels.poker.server.session`, each body in
`kotlinx.coroutines.runBlocking`, `@Timeout(30)` on the concurrency test. The collecting sink is a
`MutableList<String>` appended only from inside `writeAll`, which is safe precisely because the
class under test guarantees a single consumer.

| Test | Proves |
| --- | --- |
| `everyFrameArrivesWholeAndExactlyOnce` | two producers on `Dispatchers.Default` each `send` 500 distinct frames (`"a-0"…"a-499"`, `"b-0"…"b-499"`); after `close()` the collected list has size 1 000 and its `toSet()` equals the 1 000 expected strings — every element is a whole frame, none torn or duplicated |
| `framesFromOneProducerKeepTheirOrder` | in that same collection, the `"a-"` frames appear in ascending index order, and so do the `"b-"` frames |
| `writeAllReturnsWhenTheWriterIsClosed` | a `writeAll` coroutine completes after `close()` with no frames pending |
| `sendingAfterCloseReturnsFalse` | `close()` then `send("x")` returns `false`, throws nothing, and no further frame is collected |
| `closeIsIdempotent` | calling `close()` twice throws nothing and `writeAll` still returns |
| `sendSuspendsWhenTheBufferIsFull` | with `ConnectionWriter(capacity = 1)`, a producer sending three frames has not completed before `writeAll` starts consuming, and all three arrive once it does |

## Acceptance criteria

- [ ] `ConnectionWriterTest.everyFrameArrivesWholeAndExactlyOnce` passes
- [ ] `ConnectionWriterTest.framesFromOneProducerKeepTheirOrder` passes
- [ ] `ConnectionWriterTest.writeAllReturnsWhenTheWriterIsClosed` passes
- [ ] `ConnectionWriterTest.sendingAfterCloseReturnsFalse` passes
- [ ] `ConnectionWriterTest.closeIsIdempotent` passes
- [ ] `ConnectionWriterTest.sendSuspendsWhenTheBufferIsFull` passes
- [ ] `ConnectionWriter.kt` imports nothing from `io.ktor` and nothing from `duels.poker.engine`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
