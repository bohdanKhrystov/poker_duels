---
schema: 2
id: TASK-141002
title: The resolver answers whether a device names a player
type: task
status: backlog
parent: STORY-1410
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, account, auth]
depends_on: [TASK-141001]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.IdentityResolverTest' -PrequireDocker=true
  - grep -q 'tests="12" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.auth.IdentityResolverTest.xml
  - grep -c 'public data class \|public data object ' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 5
  - grep -c 'public suspend fun ' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 2
  - grep -c 'players.findOrNull' poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt | grep -qx 2
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`IdentityResolver` can answer one new question — **does this device id resolve, through a live
binding, to this player?** — while `resolve` and all five `Identity` cases stay byte-unchanged.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/auth/IdentityResolverTest.kt` | modify |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§4, `poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt`.

## Scope

- One new function on `IdentityResolver`, exactly as `ADR-0135` §4 writes it:

  ```kotlin
  public suspend fun namesPlayer(deviceId: DeviceId, playerId: PlayerId): Boolean
  ```

  implemented as `players.findOrNull(deviceId)?.id == playerId`, reusing the shipped
  `revoked_at IS NULL` lookup — `PostgresPlayerDirectory.findOrNull` already carries that
  predicate, so a revoked binding resolves to nobody and this function answers `false` for it.
- **`resolve` is not touched**, and neither is any of the five `Identity` cases. `ADR-0135` §4:
  *"`resolve` and all five `Identity` cases are byte-unchanged, so `ADR-0027` §4's precedence is
  untouched, the socket gains no query, and `Identity.Session` keeps the guarantee its KDoc
  makes."*
- KDoc on the new function saying what it is **not**: a second resolution path. It answers a
  comparison and never a `Identity`, it never mints, and `ADR-0135` §Consequences names the
  careless-reader risk out loud — *"a class whose whole KDoc is about precedence now also answers a
  comparison"* — so say so where the next reader is.

## Out of scope

- **The route.** `GET /api/me/device` is `TASK-141003`.
- **Any change to `PlayerDirectory` or its Postgres implementation.** `findOrNull` already answers
  exactly what is needed; a second query would be the enumeration surface `ADR-0135` §4 refuses.
- **Calling `namesPlayer` from anywhere.** Nothing calls it until `TASK-141003`. It is dead code
  for exactly one merge, which is the price of keeping that ticket to three files.
- **The socket.** `DuelSocket` gains no query; `IdentityResolver.resolve` runs on every handshake
  and must not grow one (`ADR-0135` §Alternatives 5).

## Tests

`IdentityResolverTest`, **8 today → 12**. The four new ones:

| Test | Proves |
| --- | --- |
| `aDeviceResolvingToThatPlayerNamesThem` | `namesPlayer(deviceId, playerId)` is `true` when the directory answers a `Player` whose `id` is `playerId` |
| `aDeviceResolvingToAnotherPlayerDoesNotNameThem` | `false` when the directory answers a different player — the one case `ADR-0135` §5 turns into `signOutHandsANewProfile = false`. Uses the **same** directory as the test above with a second device id in it, so the two answers differ on the input and not on the fixture |
| `aDeviceResolvingToNobodyNamesNobody` | `false` for a device id the directory does not know — which is also the revoked case, because `findOrNull` filters on `revoked_at IS NULL` |
| `namesPlayerAsksTheDirectoryOnceAndNeverMints` | wrapped in the file's own `CountingPlayerDirectory`, `findOrNullCallCount` is exactly **1** after one call; and the directory's `resolve` throws, so a call that minted would fail rather than pass quietly. `InMemoryPlayerDirectory` mints on `resolve`, which is why the counting wrapper is the right double here |

The eight merged tests are **not edited**. They are what says `resolve` still behaves, and the
count gate of 12 is 8 + 4.

## What would still pass if the coder were wrong

- **`namesPlayer` implemented as `true`** passes the first test and fails the second and third.
  That is why there are two device ids in one directory rather than one fixture per test: one
  input cannot tell a comparison from a constant.
- **`namesPlayer` implemented via `resolve`** — the natural mistake, since `resolve` is right
  there — would answer `Identity.Device(...)` for a device with no session and could be coerced
  into looking correct. It fails `namesPlayerAsksTheDirectoryOnceAndNeverMints` only if the double
  counts, which is why that test uses the counting wrapper rather than a plain map.
- **Editing `resolve` to share a helper** leaves all twelve tests green. The three `grep -c`
  structural gates are what catch it: five `Identity` cases (**5**, measured at `c6a41e6d`), two
  public suspend functions (**1** today, **2** after — `resolve` and `namesPlayer`, and no third),
  and two `players.findOrNull` call sites (**1** today, **2** after — one in each function, so a
  refactor that routed one through the other drops it back to 1).

## Acceptance criteria

- [ ] `IdentityResolverTest` reports `tests="12" skipped="0" failures="0" errors="0"`
- [ ] `IdentityResolverTest.aDeviceResolvingToThatPlayerNamesThem` passes
- [ ] `IdentityResolverTest.aDeviceResolvingToAnotherPlayerDoesNotNameThem` passes
- [ ] `IdentityResolverTest.aDeviceResolvingToNobodyNamesNobody` passes
- [ ] `IdentityResolverTest.namesPlayerAsksTheDirectoryOnceAndNeverMints` passes
- [ ] The three `grep -c` structural gates on `IdentityResolver.kt` exit 0 (5, 2, 2)
- [ ] `./gradlew :poker-server:ktlintCheck`, `:poker-server:detekt` and
      `check -PrequireDocker=true` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
