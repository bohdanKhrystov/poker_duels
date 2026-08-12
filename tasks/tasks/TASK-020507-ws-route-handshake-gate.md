---
schema: 2
id: TASK-020507
title: Open /ws behind a mandatory handshake and one writing coroutine
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, websocket, session, security]
depends_on: [TASK-020501, TASK-020505, TASK-020506]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketHandshakeTest'
  - ./gradlew :poker-server:check
---

## Goal

`/ws` exists, its first frame must be a `Hello` with the right protocol version, and anything else
is refused and closed with a named close reason before it can reach any further logic.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketHandshakeTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/Handshake.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/ConnectionWriter.kt`,
`poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt`,
`poker-server/src/test/kotlin/duels/poker/server/WebSocketTestClientTest.kt` (the `testApplication`
+ client-`WebSockets` shape to copy).

## Scope

- One main file, package `duels.poker.server`, KDoc on the public declarations:

  ```kotlin
  public const val HANDSHAKE_REQUIRED: String = "handshake required"
  public const val PROTOCOL_VERSION_MISMATCH: String = "protocol version mismatch"

  public fun Application.duelSocket(deps: SocketDependencies) {
      routing {
          webSocket("/ws") {
              val writer = ConnectionWriter()
              val pump = launch { writer.writeAll { frame -> outgoing.send(Frame.Text(frame)) } }
              try {
                  serve(deps, writer, pump)
              } finally {
                  writer.close()
              }
          }
      }
  }
  ```

  Plus three private `DefaultWebSocketServerSession` extensions:
  - `serve(deps, writer, pump)` — `readHello(writer, pump) ?: return`, then the device id is
    `hello.deviceId?.let(::DeviceId) ?: deps.deviceIds.newDeviceId()`, then
    `handshake(hello, deviceId.value)`: a `Welcome` is sent and the socket then waits with
    `incoming.consumeEach { }` — this story's socket expects nothing after the handshake and
    `TASK-020509` gives that lambda its body; a `Failure` is sent and the socket is closed with
    `PROTOCOL_VERSION_MISMATCH`.
  - `readHello(writer, pump): Hello?` — takes one frame with
    `incoming.receiveCatching().getOrNull()`, refuses anything that is not `Frame.Text`, runs
    `ProtocolCodec.decodeClient`, refuses a `Decoded.Refused` with its own error and a
    `Decoded.Message` that is not a `Hello` with `ProtocolError.MALFORMED_MESSAGE`.
  - `refuseHandshake(writer, pump, error: ProtocolError?): Hello?` — sends
    `ServerMessage.Failure(error)` when there is one, then `writer.close()`, `pump.join()`,
    `close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, HANDSHAKE_REQUIRED))`, and returns
    `null`. The `join` is load-bearing: the single writer must drain before the socket closes, or
    the client sees a close with no `Failure`.
- **Every outbound frame goes through `writer`.** Nothing in this file calls `send(Frame.Text(…))`
  or touches `outgoing` outside the one `writeAll` lambda; that is what makes the writer single.
- The `finally` does nothing that suspends. A close path must survive cancellation, and a
  suspending `finally` is a close path that may not run.
- A non-text first frame is closed with `HANDSHAKE_REQUIRED` and no `Failure` body: a client
  sending binary has not shown it speaks this protocol at all.
- `main()` and `Application.module()` are **not** touched. `duelSocket` is installed by the caller,
  and the only caller until `STORY-0210` provides a shipping `PlayerDirectory` is a test.

## Out of scope

- Resolving a profile or registering a session — `TASK-020508`.
- Any behaviour for a frame that arrives after the handshake, **including a test asserting it** —
  `TASK-020509` owns that loop, and a test written here would have to be rewritten there.
- A timeout on a client that connects and says nothing. That is a clock, and clocks in this server
  belong to `ADR-0013` and `STORY-0208`; not yet ticketed here.
- Editing `Application.kt`, `main()`, or `module()`.
- Ping/pong or keepalive configuration — Ktor's `WebSockets` plugin already owns it.

## Tests

`DuelSocketHandshakeTest`, JUnit 5, package `duels.poker.server`. Each test is
`fun … () = testApplication { application { module(); duelSocket(testDeps(…)) } … }` with a client
from `createClient { install(WebSockets) }`, and uses `webSocketSession("/ws")` so the close reason
can be read from `session.closeReason.await()`. Wrap each exchange in
`withTimeout(5.seconds)`. Frames are built with `ProtocolCodec.encode(...)` and read back with
`ProtocolCodec` / `protocolJson` decoding of `ServerMessage`.

| Test | Proves |
| --- | --- |
| `aHelloWithNoDeviceIdIsWelcomedWithAnIssuedId` | with `testDeps(deviceIds = fixedDeviceIds("issued-1"))`, `Hello(deviceId = null)` gets `ServerMessage.Welcome("issued-1", PROTOCOL_VERSION)` and the socket stays open |
| `aHelloWithADeviceIdIsWelcomedWithThatId` | `Hello(deviceId = "d1")` gets `Welcome("d1", …)`, and `testDeps(deviceIds = fixedDeviceIds())` proves no id was minted, since minting would have thrown |
| `aFirstFrameThatIsNotHelloIsRefused` | a frame encoding `Act(1, 0, PlayerAction.Fold(0))` yields `Failure(MALFORMED_MESSAGE)` and a close whose `message` is `HANDSHAKE_REQUIRED` |
| `aMalformedFirstFrameIsRefused` | the frame `"{"` yields `Failure(MALFORMED_MESSAGE)` and a close whose `message` is `HANDSHAKE_REQUIRED` |
| `aBinaryFirstFrameIsRefusedWithNoFailureBody` | `Frame.Binary(true, byteArrayOf(1, 2, 3))` closes with `message == HANDSHAKE_REQUIRED` and no `ServerMessage` is received first |
| `aVersionMismatchIsRefused` | `Hello(deviceId = null, protocolVersion = PROTOCOL_VERSION + 1)` yields `Failure(VERSION_MISMATCH)` then a close whose `message` is `PROTOCOL_VERSION_MISMATCH` |
| `aRefusedHandshakeClosesWithViolatedPolicy` | in the `"{"` case, `closeReason.await()?.code` equals `CloseReason.Codes.VIOLATED_POLICY.code` |

## Acceptance criteria

- [ ] `DuelSocketHandshakeTest.aHelloWithNoDeviceIdIsWelcomedWithAnIssuedId` passes
- [ ] `DuelSocketHandshakeTest.aHelloWithADeviceIdIsWelcomedWithThatId` passes
- [ ] `DuelSocketHandshakeTest.aFirstFrameThatIsNotHelloIsRefused` passes
- [ ] `DuelSocketHandshakeTest.aMalformedFirstFrameIsRefused` passes
- [ ] `DuelSocketHandshakeTest.aBinaryFirstFrameIsRefusedWithNoFailureBody` passes
- [ ] `DuelSocketHandshakeTest.aVersionMismatchIsRefused` passes
- [ ] `DuelSocketHandshakeTest.aRefusedHandshakeClosesWithViolatedPolicy` passes
- [ ] `DuelSocket.kt` contains exactly one occurrence of `outgoing` and it is inside the
      `writeAll` lambda
- [ ] `Application.kt` is unmodified, and `HealthRouteTest` and `ServerPluginsTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
