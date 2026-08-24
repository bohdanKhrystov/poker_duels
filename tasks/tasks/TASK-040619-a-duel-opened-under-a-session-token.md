---
schema: 2
id: TASK-040619
title: A duel can be opened under a session token, not only a device id
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, e2e, test-support, socket, auth]
depends_on: [TASK-040618]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.E2eServerTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketDuelTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The e2e harness can seat a client by presenting a session token instead of a device id, so a duel
played while signed into another account can be written at all.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServerTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/db/SignInDatabaseTest.kt` (its `helloWith`, which
already sends a `Hello` carrying both fields),
`docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §3 and §4.

## Scope

- `completeHandshake` gains a second parameter with a default, so **every existing call site
  compiles and behaves unchanged**:

  ```kotlin
  internal suspend fun DefaultClientWebSocketSession.completeHandshake(
      deviceId: String?,
      sessionToken: String? = null,
  ): ServerMessage.Welcome
  ```

  It still fails loudly on anything but a `Welcome`, and its existing message stays.
- `openSocketDuel` gains `hostToken: String? = null` and `guestToken: String? = null`, threaded into
  the two `completeHandshake` calls. Every existing call site passes neither and is unchanged.
- `SocketClient.deviceId` becomes `String?` **only if the compiler requires it**; prefer keeping it
  non-null by passing `HOST_DEVICE`/`GUEST_DEVICE` alongside the token, which is what a real client
  does (`ADR-0030` §8: the client keeps sending its device id whether or not it holds a token). Say
  in a comment which was chosen and why.
- **No production file changes.** The socket already accepts a `Hello` carrying a session token —
  `STORY-0405` shipped it — and this ticket only lets the harness send one.

## Out of scope

- The scenario itself — `TASK-040620`.
- `SocketDuelTest`, `SocketReconnectTest`, `SocketCoinsTest`, `SocketHistoryTest`, `SocketLadderTest`
  and every other existing e2e class. They are in `verify:` **unmodified**: the defaults are what
  keeps them so, and an edit to any of them means the parameter was not defaulted properly.
- Reconnecting mid-duel under a token — `reconnect(http, seat)` keeps using the device id it always
  did.

## Tests

`E2eServerTest` gains two methods, in the shape the file already uses.

| Test | Proves |
| --- | --- |
| `aHandshakeUnderASessionTokenSeatsTheSessionsPlayer` | Mint a profile for `"d-a"`, sign it up and sign it in for a token; then `completeHandshake(deviceId = "d-b", sessionToken = token)` on a fresh socket. The `Welcome`'s `playerId` is `"d-a"`'s player, **not** `"d-b"`'s. **Two devices, and the answer is the one the token names** — with one device the test could not tell the token from the header |
| `aHandshakeWithNoTokenStillSeatsTheDevice` | The control, on the same database in the same test class: `completeHandshake(deviceId = "d-b")` answers a `Welcome` whose `playerId` is `"d-b"`'s. Without it, a harness that ignored its `deviceId` argument entirely would pass the test above |

## Acceptance criteria

- [ ] `E2eServerTest.aHandshakeUnderASessionTokenSeatsTheSessionsPlayer` passes
- [ ] `E2eServerTest.aHandshakeWithNoTokenStillSeatsTheDevice` passes
- [ ] `./gradlew :poker-server:check -PrequireDocker=true` exits 0 with **no edit** to any e2e test
      class other than `E2eServerTest`
- [ ] Nothing under `poker-server/src/main` is modified
- [ ] `openSocketDuel`'s two new parameters both default to `null`
- [ ] Every command in `verify:` exits 0

## Proof

Drop the `sessionToken` argument from the `Hello` that `completeHandshake` sends, keeping the
parameter. `aHandshakeUnderASessionTokenSeatsTheSessionsPlayer` reddens — the `Welcome` names
`"d-b"`'s player instead — and `aHandshakeWithNoTokenStillSeatsTheDevice` stays green, since it
never passes one. One test, and it is the only one in the repository that would notice.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The asymmetry is in the `Welcome`, not the `Hello`.** A session-borne `Hello` carries **both**
`deviceId` and `sessionToken` — `ADR-0030` §8: the device id always rides along, token or not,
because that is what a real client sends. What distinguishes the session route is the `Welcome`
coming back with `deviceId = null`, which the test asserts alongside the player id matching the
token's owner and differing from the device's own.

**No coalescing, which matters more in a helper than in production.** `completeHandshake` sends
`Hello(deviceId = deviceId, sessionToken = sessionToken)` — exactly what its caller passed, with no
substitution when one is absent. A helper that quietly supplied a device for a missing token would
make every test built on it prove the wrong thing, and no production mutation would catch it.

**`openSocketDuel`'s token parameters are unexercised here, by design.** The ticket's Out of scope
defers the scenario to `TASK-040620`. They are threaded through to `completeHandshake` rather than
dropped, so the next ticket builds on something real — the failure mode for scaffolding is compiling
without ever reaching the wire.

**All three defaults are `null` and change nothing that predates them**, verified by `SocketDuelTest`
passing unmodified.

