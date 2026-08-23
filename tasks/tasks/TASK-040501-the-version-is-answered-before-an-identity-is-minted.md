---
schema: 2
id: TASK-040501
title: The version question is answered before any identity is minted
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, protocol, handshake]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.HandshakeTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketHandshakeTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`handshake` stops building a `Welcome` and answers only the question it can actually answer — *does
this client speak our version?* — so the socket, and only the socket, decides who the connection is
and does so **after** the version has been accepted.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Handshake.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/HandshakeTest.kt` | modify |

Read `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §4 (why identity resolution has to
become a step of its own) and nothing else.

## Scope

- Replace `handshake(hello: Hello, deviceId: String): ServerMessage` with

  ```kotlin
  public fun versionRefusalOrNull(hello: Hello): ServerMessage.Failure?
  ```

  returning `ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)` on a mismatch and `null` on a
  match. Still pure: no I/O, no state, no `Welcome`.
- `DuelSocket.serve` calls it first. On a non-null refusal it sends the frame, closes the writer,
  joins the pump and closes with `PROTOCOL_VERSION_MISMATCH` — **the same four statements, in the
  same order, as today's `is ServerMessage.Failure ->` arm** — and returns.
- Only then does it mint or read the device id, call `deps.directory.resolve`, and build
  `ServerMessage.Welcome(deviceId.value)` itself. Everything after that — `Session`,
  `SeatOwnership.adopt`, `connections.register`, the whole `finally` block — is unchanged, moved
  wholesale rather than rewritten.
- The nine-branch `when` over `ServerMessage` and its `error("handshake() returned …")` arm go
  away with the function that made them necessary. That is a deletion, not a replacement.
- **This changes one observable thing and it is deliberate:** a version-mismatched `Hello` no
  longer reaches `deviceIds.newDeviceId()` or `directory.resolve`. `TASK-040503` is the ticket that
  proves it; do not add that test here.

## Out of scope

- Every wire field: `Hello.sessionToken`, `Welcome.playerId`, `ProtocolError.INVALID_SESSION` and
  the version bump are `TASK-040502`. **`Welcome` still takes one argument when this merges.**
- `IdentityResolver` — `TASK-040510`. The socket resolves the device exactly as it does today.
- The socket-level proof that a mismatch mints nothing — `TASK-040503`.

## Tests

`HandshakeTest` — rewritten against the new function. Four methods, and **each names its own
version relative to `PROTOCOL_VERSION`**, never an absolute number (`docs/protocol.md` §Notes).

| Test | Proves |
| --- | --- |
| `aMatchingVersionIsNotRefused` | `versionRefusalOrNull(Hello(deviceId = "d1", protocolVersion = PROTOCOL_VERSION))` is `null` |
| `aDefaultedHelloIsNotRefused` | `versionRefusalOrNull(Hello())` is `null` — the default is the current version |
| `anOlderVersionIsRefused` | `PROTOCOL_VERSION - 1` answers `Failure(VERSION_MISMATCH)` |
| `aNewerVersionIsRefused` | `PROTOCOL_VERSION + 1` answers the same — **both directions, because one direction alone agrees with a `<` that should be a `!=`** |

`DuelSocketHandshakeTest` is **not edited**: it asserts the frames a socket sends, and this ticket
sends the same frames in the same order. It is in `verify:` as the gate that says so.

## Acceptance criteria

- [ ] `HandshakeTest.aMatchingVersionIsNotRefused` passes
- [ ] `HandshakeTest.aDefaultedHelloIsNotRefused` passes
- [ ] `HandshakeTest.anOlderVersionIsRefused` passes
- [ ] `HandshakeTest.aNewerVersionIsRefused` passes
- [ ] `DuelSocketHandshakeTest` passes with no edit to that file
- [ ] `handshake` no longer exists anywhere in `poker-server`
- [ ] Every command in `verify:` exits 0

## Proof

Change `versionRefusalOrNull`'s comparison from `==` to `<=` and `aNewerVersionIsRefused` turns
red while the other three stay green. That is what makes the pair worth having: with only
`anOlderVersionIsRefused`, the mutant survives.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
