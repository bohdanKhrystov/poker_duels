---
schema: 2
id: TASK-040518
title: The socket presents the session, and an invalid one is refused rather than downgraded
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, socket, auth, identity, protocol]
depends_on: [TASK-040517]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketSessionIdentityTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketHandshakeTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A `Hello` carrying a session token seats that player; the device id beside it is ignored and not
even validated; an invalid one is refused with `INVALID_SESSION` and the socket closes; and a
`Hello` with a token but no device gets a `Welcome` whose `deviceId` is `null` and mints no
`player` row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketSessionIdentityTest.kt` | create |

Read `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §§3–4 and
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §2's last paragraph. Nothing
else.

## Scope

- After the version check, `serve` calls
  `deps.identities.resolve(hello.sessionToken?.let(::SessionToken), hello.deviceId?.let(::DeviceId))`
  and branches on the five `Identity` cases with an **exhaustive `when`**:
  - `Session(playerId)` → the connection is that player; `Welcome(playerId.value, deviceId = null)`.
    `deps.directory` is not called at all.
  - `Device(playerId, deviceId)` → `Welcome(playerId.value, deviceId.value)`; today's path.
  - `UnknownDevice(deviceId)` → `deps.directory.resolve(deviceId)`, then `Welcome(player.id.value,
    deviceId.value)`. This is `ADR-0027` path 2 for the socket: the socket **creates**, HTTP does
    not, and the split lives here rather than in the resolver.
  - `Anonymous` → `deps.deviceIds.newDeviceId()`, then `resolve`, then `Welcome` with both. Path 3.
  - `Refused` → send `Failure(ProtocolError.INVALID_SESSION)`, close the writer, join the pump,
    close the socket with a new `INVALID_SESSION_PRESENTED` close reason, and return — **the same
    four statements in the same order as the version-mismatch refusal**, because a refusal that
    tears down differently is a refusal that races differently.
- **Nothing downstream of the `Welcome` changes.** `Session`, `SeatOwnership.adopt`,
  `connections.register` and the whole `finally` block already key on `PlayerId`, so a session-borne
  identity flows through them unmodified. Say so; do not restructure them.
- A `Session` connection has no device id and **no `player` row is created for it** — `ADR-0030`
  §2: a recovery sign-in on a fresh browser must litter no orphan profile.

## Out of scope

- Signing in over the socket. `ADR-0027` §3: identity is fixed at `Hello` for the life of the
  connection, there is no sign-in message, and a socket that could change its own player would make
  `SeatOwnership`, `SessionRegistry`, `ConnectionDirectory` and room membership wrong at once.
- The client sending a token — `STORY-0412`.
- Closing a live socket when a session is deleted — `ADR-0030` §3 forbids it.

## Tests

`DuelSocketSessionIdentityTest` — a new file, using `testDeps(identities = …)` with a
`FixedAuthSessions` mapping one token to one player.

| Test | Proves |
| --- | --- |
| `aTokenSeatsItsPlayerAndNamesNoDevice` | `Hello(sessionToken = "t-signed")` answers `Welcome(playerId = "p-signed", deviceId = null)` |
| `aTokenOutranksTheDeviceBesideIt` | `Hello(deviceId = "d-anon", sessionToken = "t-signed")` still answers `Welcome("p-signed", null)`, where `d-anon` is a device the directory **already owns a different profile for** — two players, two ids, and the assertion names both fields |
| `aTokenMeansTheDirectoryIsNotConsulted` | the directory double counts calls; after the test above, both `resolve` and `findOrNull` were called zero times for `d-anon` |
| `aTokenOnAFirstEverBrowserCreatesNoProfile` | `Hello(deviceId = null, sessionToken = "t-signed")` leaves `InMemoryPlayerDirectory.profileCount` at the number the fixture seeded, and `Welcome.deviceId` is `null` |
| `anInvalidTokenIsRefusedNotDowngraded` | `Hello(deviceId = "d-anon", sessionToken = "nonsense")` answers `Failure(INVALID_SESSION)` and the socket closes — **never** a `Welcome` for `p-anon`. The device id must be one that *would* resolve, or the test cannot see a fallback |
| `anInvalidTokenCreatesNoProfile` | `profileCount` is unchanged across that refusal |
| `noTokenWithAKnownDeviceIsUnchanged` | `Hello(deviceId = "d-anon")` answers `Welcome("p-anon", "d-anon")` — the control: today's behaviour, still exactly today's behaviour |
| `noTokenAndNoDeviceStillMintsOne` | `Hello()` answers a `Welcome` naming both an issued device id and a player, and `profileCount` went up by one |

`DuelSocketHandshakeTest` is in `verify:` unedited: every one of its `Hello`s carries no token, so
every one of them must still take the `Device`/`Anonymous` path and produce the identical frame.

## Acceptance criteria

- [ ] All eight test methods above pass
- [ ] `DuelSocketHandshakeTest` passes with no edit to that file
- [ ] `DuelSocket.serve`'s `when` over `Identity` has no `else` branch
- [ ] `git diff --name-only` names exactly two files
- [ ] Every command in `verify:` exits 0

## Proof

Make `Refused` fall through to the device path and `anInvalidTokenIsRefusedNotDowngraded` goes red
alone — the other seven still pass, which is the whole reason its fixture presents a device id that
would have worked. Have the `Session` branch call `deps.directory.resolve` "just to be safe" and
only `aTokenMeansTheDirectoryIsNotConsulted` and `aTokenOnAFirstEverBrowserCreatesNoProfile` go
red.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The seam above the resolver is still ungated, and that is `TASK-040525`.** This ticket's eight
tests do not present a blank token. Mutating the parse to
`hello.sessionToken?.takeIf { it.isNotBlank() }?.let(::SessionToken)` — so a blank credential becomes
*absent* rather than present-and-invalid — leaves all seventeen tests across both socket classes
green. The coder found it, the reviewer reproduced it independently, and neither added a ninth test,
because this ticket's Tests table names exactly eight. Third appearance of the bug class
`TASK-040510`'s deep review predicted.

**The seeded device is not what makes the fall-through mutation catchable here**, contrary to the
pattern at `TASK-040510`. `InMemoryPlayerDirectory.resolve` mints unconditionally, so *any* device id
produces a `Welcome` under that mutation; the reviewer confirmed by deleting the seed and watching
the test still fail. What the seeding buys is the sharper assertion — naming the exact wrong
`Welcome` for `p-anon` rather than merely showing the response is not a `Failure`. At `TASK-040510`
the equivalent line **was** load-bearing, because that path used `findOrNull`, which does not mint.
The difference is whether the fixture's lookup creates.

**`SESSION_PLACEHOLDER_DEVICE_ID` is safe, and this is why.** `Player.deviceId` is non-nullable but a
session-borne connection has no device. The reviewer traced every consumer reachable from a socket:
`ConnectionDirectory`, `SeatOwnership`, `SessionRegistry`, `Room`/`RoomRegistry` and every call site
in `DuelSocket.kt` key on `PlayerId`; `Player`'s data-class `equals` is never invoked in main source;
the sentinel cannot reach the database or a log. Two session-borne connections carrying it cannot
collide, because nothing is keyed on device id once identity is `Session`. The value is
self-describing.

