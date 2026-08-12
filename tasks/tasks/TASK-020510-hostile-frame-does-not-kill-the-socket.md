---
schema: 2
id: TASK-020510
title: A nesting bomb or an oversized frame is answered, not fatal, at the socket
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, websocket, robustness, security]
depends_on: [TASK-020509, TASK-020213]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketHostileFrameTest'
  - ./gradlew :poker-server:test --tests '*FrameLimitTest'
  - ./gradlew :poker-server:check
---

## Goal

The frame limits `TASK-020213` put in the codec are proven where they matter: a hostile frame on a
live connection comes back as a `Failure`, the socket survives it, and no other session notices.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketHostileFrameTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt` (as `TASK-020213` left
it — the limits and their defaults),
`poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`,
`poker-server/src/test/kotlin/duels/poker/server/DuelSocketFrameLoopTest.kt` (the handshake and
client shape to copy),
`poker-server/src/test/kotlin/duels/poker/server/protocol/FrameLimitTest.kt`.

## Scope

- Test sources only. No main file changes: `decodeClient` already refuses these frames, and this
  ticket exists to prove that the connection loop does not turn the refusal into a dead socket.
- Both hostile inputs are built programmatically and their size or depth is stated in the test, so
  the test still means something if a limit changes: a nesting bomb of `"[".repeat(50_000)` and an
  oversized frame of `"\"" + "a".repeat(2_000_000) + "\""`.
- Each test asserts three things: a `ServerMessage.Failure` came back, the socket is still open —
  proven by sending `"{"` afterwards and getting another `Failure` — and `sessions.size` is still
  `1`.
- If `TASK-020213` gave `decodeClient` an extra parameter for the limits, it has a default, because
  `ProtocolCodecTest` passes unedited. The socket calls `decodeClient(text)` and uses those
  defaults; **do not** change `DuelSocket.kt` here.

## Out of scope

- Threading the *configured* limits from `ServerConfig` into the socket, so an operator can tighten
  them per deployment. Real, and deliberately not ticketed yet — the defaults are what ship in
  v0.1 and the socket has no `ServerConfig` today.
- Changing the limits, their defaults, or where they are enforced — `TASK-020213` owns all three.
- Any main-source change at all.

## Tests

`DuelSocketHostileFrameTest`, JUnit 5, package `duels.poker.server`. Both tests complete the
handshake with `Hello(deviceId = "d1")` first, use `withTimeout(10.seconds)`, and build their own
`SessionRegistry` through `testDeps(sessions = sessions)`.

| Test | Proves |
| --- | --- |
| `aNestingBombIsAnsweredAndTheSocketSurvives` | 50 000 nested `[` yields a `Failure` rather than a closed socket or a server error, and a following `"{"` still yields a `Failure` |
| `anOversizedFrameIsAnsweredAndTheSocketSurvives` | a 2 000 000-character frame yields a `Failure`, and a following `"{"` still yields a `Failure` |
| `aHostileFrameDoesNotDisturbAnotherSession` | with two welcomed connections, the bomb on the first leaves the second receiving nothing within 200 ms and `sessions.size == 2` |

## Acceptance criteria

- [ ] `DuelSocketHostileFrameTest.aNestingBombIsAnsweredAndTheSocketSurvives` passes
- [ ] `DuelSocketHostileFrameTest.anOversizedFrameIsAnsweredAndTheSocketSurvives` passes
- [ ] `DuelSocketHostileFrameTest.aHostileFrameDoesNotDisturbAnotherSession` passes
- [ ] `FrameLimitTest` passes with no edit to that file
- [ ] Nothing under `poker-server/src/main/` is modified by this ticket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
