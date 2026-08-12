---
schema: 2
id: TASK-020512
title: The socket enforces the operator's frame limits, not the codec's defaults
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, protocol, config, robustness]
depends_on: [TASK-020510]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketHostileFrameTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:check
---

## Goal

`DuelSocket` calls `ProtocolCodec.decodeClient(text)` with no limit arguments, so it gets the
codec's defaults — 1 MiB and depth 64. `ServerConfig` already carries `maxFrameLength` and
`maxFrameNestingDepth`, read with the usual env → `application.conf` → default precedence, and
**nothing reads them.**

So the limits are enforced, but they are not the operator's limits. Tuning `ServerConfig` today
changes nothing about what the socket accepts, which is worse than having no setting at all: a
setting that looks live and is not will be trusted.

Recorded as out-of-scope in `TASK-020510` and filed here rather than left as a comment.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/SocketDependencies.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketHostileFrameTest.kt` | modify |

Read `ServerConfig.kt` and `ProtocolCodec.kt`. Modify neither: the codec's defaults stay as they
are, for callers that supply nothing.

## Scope

Thread the configured limits from `ServerConfig` to the two `decodeClient` call sites in
`DuelSocket`.

- Carry them on `SocketDependencies`, which already exists to stop the route growing a parameter
  list. It has **no default values** by deliberate choice — keep it that way, so a caller cannot
  silently get a limit nobody chose.
- `testDeps()` supplies the codec defaults, so every existing socket test keeps its current
  behaviour and passes unedited.
- Both call sites must use them. One updated and one missed is the same bug with a smaller blast
  radius, and it would pass the tests below unless they cover both paths — make sure they do.

Do not widen `decodeClient`'s catch, and do not touch the pre-parse guards.

## Tests

| Name | Asserts |
| --- | --- |
| `theSocketRefusesAtTheConfiguredLength` | a frame under the codec default but over a *configured* smaller limit is refused — proving the configured value is the one in force |
| `theSocketRefusesAtTheConfiguredDepth` | the same for nesting depth |
| `aGenerousConfigurationAcceptsAFrameTheDefaultWouldRefuse` | the limits genuinely move in **both** directions, so the test cannot pass by the default happening to be stricter |
| `theHandshakeFrameIsLimitedToo` | the pre-handshake `readHello` path enforces them as well — an unauthenticated client is exactly who you want limits applied to |

The third test is the one that matters most: without it, a stricter-default implementation that
ignores configuration entirely would pass.

## Done

All three `verify:` commands exit 0, the existing socket suites pass unedited, and a configured
limit demonstrably changes what the socket accepts in both directions.
