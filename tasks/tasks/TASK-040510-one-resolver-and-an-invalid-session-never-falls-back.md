---
schema: 2
id: TASK-040510
title: One resolver, and an invalid session never falls back to the device
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, auth, identity]
depends_on: [TASK-040509]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.IdentityResolverTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`ADR-0027` §4's precedence exists once, in one class, and it creates nothing: a valid session wins
and the device beside it is not even looked at; an invalid one is refused rather than quietly
downgraded; with no session, the device is answered exactly as today.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/IdentityResolverTest.kt` | create |

Read `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §4 in full — it is the whole
specification — and `poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt`.
Nothing else.

## Scope

- One sealed answer and one class, both in `duels.poker.server.auth`:

  ```kotlin
  public sealed interface Identity {
      public data class Session(val playerId: PlayerId) : Identity
      public data class Device(val playerId: PlayerId, val deviceId: DeviceId) : Identity
      public data class UnknownDevice(val deviceId: DeviceId) : Identity
      public data object Refused : Identity
      public data object Anonymous : Identity
  }

  public class IdentityResolver(sessions: AuthSessions, players: PlayerDirectory) {
      public suspend fun resolve(token: SessionToken?, deviceId: DeviceId?): Identity
  }
  ```

- **Five answers, not three, and the reason is that the socket and HTTP disagree about two of
  them.** `ADR-0027` §4 path 2 says *"the socket resolves or creates the profile, HTTP refuses an
  unknown id"*, and path 3 is socket-only. So the resolver decides the rule that must not be
  written twice — which credential wins — and hands the two entry points a value they each act on:
  `UnknownDevice` and `Anonymous` are a `401` over HTTP and a *mint and resolve* on the socket.
  **The resolver itself never creates**, so no HTTP route can mint a row through it.
- The order is the behaviour, and it is exactly this:
  1. `token != null` → `sessions.playerOf(token)`. Non-null → `Session`. Null → **`Refused`**.
     `players` is not touched, `deviceId` is not read, not validated and not compared.
  2. `deviceId == null` → `Anonymous`.
  3. `players.findOrNull(deviceId)` → `Device` when it answers, `UnknownDevice` when it does not.
- KDoc carries `ADR-0027` §4's sentence about *why* step 1 refuses rather than falls back: a silent
  downgrade from *signed in as A* to *anonymous B* would let a player win a coin into an account
  they believe they are not using, and it is the one failure a player can neither detect nor undo.

## Out of scope

- Every caller. No route and no socket calls this yet — `TASK-040511` and `TASK-040518`.
- Reading an `Authorization` header or `Hello.sessionToken`. The resolver takes values, not frames.
- Wiring into `ServerComponents` — `TASK-040511` for HTTP, `TASK-040517` for the socket.

## Tests

`IdentityResolverTest` — a new file. It uses `NoAuthSessions` for the *no session was issued* case
and a small in-file `AuthSessions` double that maps one fixed token to one fixed player for the
rest; `InMemoryPlayerDirectory` supplies the devices.

| Test | Proves |
| --- | --- |
| `aValidSessionAnswersItsPlayer` | a token mapping to `p-session` answers `Identity.Session(p-session)` |
| `aValidSessionBeatsTheDeviceBesideIt` | the **same** token presented with a device id that resolves to a *different* player still answers `Session(p-session)` — and the returned identity carries **no** device id. This is the fixture the story names: two players, two ids, and an assertion that would pass for either if the fixture used one |
| `aValidSessionDoesNotEvenLookAtTheDevice` | the directory double counts `findOrNull` calls; after a valid-session resolve with a device id present, the count is `0`. *"Ignored"* and *"looked up and discarded"* are different rules, and only the counter tells them apart |
| `anUnknownTokenIsRefusedAndNotDowngraded` | an unknown token presented **with a device id that would resolve** answers `Identity.Refused`, never `Device` — the discriminating input, because with no device id a fallback and a refusal look identical |
| `aKnownDeviceWithNoTokenAnswersItsPlayer` | `resolve(null, d1)` answers `Device(p1, d1)`, and a second device in the same test answers `Device(p2, d2)` — two seats, two answers, so neither field can be a constant |
| `anUnknownDeviceWithNoTokenIsUnknownNotAnonymous` | `resolve(null, ghost)` answers `UnknownDevice(ghost)` and carries the device id back |
| `neitherCredentialIsAnonymous` | `resolve(null, null)` answers `Identity.Anonymous` |
| `resolvingCreatesNoProfile` | after all five shapes above, `InMemoryPlayerDirectory.profileCount` is exactly what the fixture seeded |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `IdentityResolver` calls no method that creates a profile — `resolve(` appears nowhere in the
      file
- [ ] `Identity.Session` has no device field of any kind
- [ ] Every command in `verify:` exits 0

## Proof

Change step 1's `?: return Identity.Refused` to fall through to the device branch and
`anUnknownTokenIsRefusedAndNotDowngraded` goes red on its own. Move `players.findOrNull` above the
token check and only `aValidSessionDoesNotEvenLookAtTheDevice` goes red — the other seven still
pass, which is the entire argument for keeping the call counter.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
