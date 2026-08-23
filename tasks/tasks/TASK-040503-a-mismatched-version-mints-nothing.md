---
schema: 2
id: TASK-040503
title: A version mismatch mints no device id and creates no profile
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [server, socket, handshake]
depends_on: [TASK-040502]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketHandshakeTest'
---

## Goal

The one behaviour `TASK-040501` changed is pinned: a `Hello` whose version this server does not
speak is refused before anything mints a device id or writes a `player` row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketHandshakeTest.kt` | modify |

Read `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectory.kt`
(`profileCount` is already there) and
`poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt` (`fixedDeviceIds`
`error`s when its queue runs out, which is the second half of the assertion). Nothing else.

## Scope

- Two tests added to `DuelSocketHandshakeTest`. **Nothing existing in the file moves** — no
  assertion is edited, renamed or weakened.
- Build the socket with `testDeps(directory = directory, deviceIds = fixedDeviceIds("issued-1"))`
  where `directory` is an `InMemoryPlayerDirectory` the test holds, so both counters are
  observable.
- Send `Hello(deviceId = null, protocolVersion = PROTOCOL_VERSION + 1)`, read the `Failure`, and
  assert `directory.profileCount == 0`. **`deviceId = null` is the discriminating input**: with a
  device id present the socket would resolve rather than mint, and the mint path — the one
  `TASK-040501` moved — would never run, so the test would pass over the change it exists to
  guard.
- The second test sends a *matching* `Hello(deviceId = null)` against the same fixture and asserts
  `directory.profileCount == 1`. Without it, `profileCount == 0` is satisfied by a socket that
  creates a profile for nobody, ever.

## Out of scope

- Any change under `src/main`. This ticket adds tests and only tests.
- The session path: no token is presented here.

## Tests

`DuelSocketHandshakeTest`

| Test | Proves |
| --- | --- |
| `aMismatchedVersionCreatesNoProfileAndSpendsNoDeviceId` | after a refused `Hello(deviceId = null, protocolVersion = PROTOCOL_VERSION + 1)`, `directory.profileCount` is `0` and the `fixedDeviceIds` queue still holds `issued-1` |
| `aMatchingVersionWithNoDeviceIdCreatesExactlyOneProfile` | the same fixture, a matching version, `profileCount` is `1` — the control that stops the first test passing vacuously |

## Acceptance criteria

- [ ] `DuelSocketHandshakeTest.aMismatchedVersionCreatesNoProfileAndSpendsNoDeviceId` passes
- [ ] `DuelSocketHandshakeTest.aMatchingVersionWithNoDeviceIdCreatesExactlyOneProfile` passes
- [ ] Every test that was in the file before this ticket still passes, unedited
- [ ] `git diff --name-only` names exactly one file
- [ ] Every command in `verify:` exits 0

## Proof

Move `deps.directory.resolve(deviceId)` back above the version check in `DuelSocket.serve` and the
first test goes red while the second stays green. Run it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
