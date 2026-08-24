---
schema: 2
id: TASK-040609
title: DELETE /api/me/device takes a session, or it takes nothing
type: task
status: ready
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, http, route, revocation, security]
depends_on: [TASK-040608]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DeviceRouteTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`DELETE /api/me/device` exists, answers `204 No Content` to a caller holding a valid session, and
answers `401 Unauthorized` — writing nothing — to every caller who does not, **including one whose
device id resolves perfectly well**.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/DeviceRouteTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` (for
`sessionTokenOrNull`, `deviceIdOrNull` and the installer shape),
`poker-server/src/test/kotlin/duels/poker/server/http/ProfileReadsDoubles.kt`
(`identitiesFor`, `FixedAuthSessions`),
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §5.

## Scope

- A **new installer in a new file**, not a fourth parameter on `profileRoutes`:

  ```kotlin
  public fun Application.deviceRoutes(
      identities: IdentityResolver,
      credentials: Credentials,
      bindings: DeviceBindings,
  ) { routing { delete("/api/me/device") { … } } }
  ```

  `profileRoutes`, `authRoutes` and `standingsRoutes` are already three installers in three files;
  a fourth costs one line in `Application.duelServer` and changes no existing signature, where
  widening `profileRoutes` would drag every one of its call sites into this ticket.
- The `credentials` parameter is declared **now**, and `TASK-040610` is what uses it. Declaring it
  later is a second pass over every call site for nothing. It is unused in this ticket and that is
  fine: detekt's default rule set flags an unused **private** member, never a public function's
  parameter, and `./gradlew :poker-server:detekt` is in `verify:` so the claim is checked rather
  than assumed.
- **Identity is `Identity.Session` or it is `401`.** This route does **not** use
  `resolvedPlayerOrNull`, which answers a player for `Identity.Device` as well — that helper is
  right for `GET /api/me` and wrong here. Resolve through `identities.resolve(sessionTokenOrNull(), deviceIdOrNull())`
  and take the player from `Identity.Session` alone; `Device`, `UnknownDevice`, `Refused` and
  `Anonymous` are all `401` with an empty body. The `when` is exhaustive, with no `else` and no
  `as?`, so a sixth `Identity` case fails this file to compile rather than falling through to `401`.
- **Why a session and not a device** (`ADR-0049` §5), in a comment beside the guard: a caller with no
  session has no screen to keep alive, so revoking for them would sign them out of the page they are
  standing on — the hostility `ADR-0037` forbids. It also makes revocation a step-up operation for
  free.
- The request has **no body**, and none is read. Nothing is decoded, so there is no `400` on this
  route.
- On success: `bindings.revoke(playerId, keeping = token)` — the same token the caller presented —
  then `204 No Content` with an empty body.
- `204` is the answer whether or not a row was updated (`ADR-0049` §5). The port returns `Unit`, so
  there is nothing here to branch on, and that is the point.

## Out of scope

- The `409` for a player holding no credential, and any call to `Credentials` — `TASK-040610`.
  Until then this route answers `204` to any session-holder.
- Installing the route in `Application.duelServer` and `ServerComponents` — `TASK-040611`. Nothing
  in production reaches this route yet, which is why shipping it without the `409` guard is safe.
- `docs/protocol.md` — `TASK-040612`.
- `ProfileRoutes.kt` — **a named prohibition.** No signature there changes, and neither does
  `resolvedPlayerOrNull`.

## Tests

`DeviceRouteTest`, in the shape `ProfileRouteTest` uses: `testApplication`, `application { deviceRoutes(…) }`,
a `RecordingDeviceBindings`, a `RecordingCredentials(holds = true)` from `AuthRouteDoubles.kt`, and
`identitiesFor(profiles, FixedAuthSessions(mapOf("t-1" to "player-1")))`.

The fixture is the load-bearing part: `profiles` maps `"device-1"` to a profile whose `playerId` is
`"player-1"`, **so the device id in the refusal test below resolves to exactly the player the
session names.**

| Test | Proves |
| --- | --- |
| `aSessionRevokesAndGetsTwoHundredAndFour` | `DELETE /api/me/device` with `Authorization: Bearer t-1` answers `204`, the body is empty, and `bindings.revokeCalls` has size `1` naming `PlayerId("player-1")` and `SessionToken("t-1")` |
| `aDeviceIdAloneIsRefused` | The same request with `X-Device-Id: device-1` and **no** `Authorization` header answers `401`, and `bindings.revokeCalls` is **empty**. The positive control is the test above, against the same fixture: `device-1` resolves to `player-1`, so a route written with `resolvedPlayerOrNull` would answer `204` here. A fixture whose device did not resolve would make this test pass for the wrong reason |
| `anUnknownTokenIsRefusedEvenBesideAResolvableDevice` | `Authorization: Bearer nope` **and** `X-Device-Id: device-1` together answer `401`, and `revokeCalls` is empty. `ADR-0027` §4's no-fall-back rule, at this route |
| `noCredentialAtAllIsRefused` | Neither header answers `401`, and `revokeCalls` is empty |
| `aValidTokenBesideAnUnknownDeviceStillRevokes` | `Authorization: Bearer t-1` beside `X-Device-Id: no-such-device` answers `204` and records one revoke for `player-1`. The session decides and the device beside it is never consulted — a route that required the device to resolve too would fail here, and no other test in the class would notice |
| `everyRefusalHasAnEmptyBody` | The three refusals above each return `""`. One test, three requests: a `401` carrying a reason would say which of the three it was. It is **not** a substitute for the status assertions — a `204` also has an empty body, so this test alone cannot tell a refusal from a success |

**Counts, not booleans.** Every refusal asserts `revokeCalls.isEmpty()` — `size == 0` — because a
status code alone cannot tell "refused before the write" from "wrote, then answered 401".

## Acceptance criteria

- [ ] All six test methods above pass
- [ ] No test presents an `Authorization` header whose value, once `Bearer ` is stripped, is a token
      the fixture knows — a malformed-header test at this route cannot distinguish *present and
      invalid* from *absent*, because a resolvable device is refused either way, and writing one
      here would be a test that reads like a control and is not
- [ ] `DeviceRoutes.kt` does not contain the string `resolvedPlayerOrNull`
- [ ] The `when` over `Identity` in `DeviceRoutes.kt` has five branches and no `else`
- [ ] `aDeviceIdAloneIsRefused` and `aSessionRevokesAndGetsTwoHundredAndFour` share one `profiles`
      fixture in which `"device-1"` maps to `"player-1"`
- [ ] `ProfileRoutes.kt`, `AuthRoutes.kt` and `Application.kt` are unmodified
- [ ] Every command in `verify:` exits 0

## Proof

Replace the `Identity.Session`-only guard with `resolvedPlayerOrNull(identities)`, the helper the
other three route files use. **`aDeviceIdAloneIsRefused` reddens on both of its assertions** — the
status becomes `204` and `revokeCalls` has size `1`.

**Exactly one test reddens**, and the two near-misses are worth stating. `everyRefusalHasAnEmptyBody`
stays **green**: the device-alone request now answers `204`, and a `204` has an empty body too, so
that test cannot see the change — which is why it is listed above as not a substitute for the status
assertions. `anUnknownTokenIsRefusedEvenBesideAResolvableDevice` also stays green, because
`resolvedPlayerOrNull` answers `null` for `Identity.Refused` as well; `ADR-0027` §4's no-fall-back
rule is enforced inside `IdentityResolver`, not here, and that test guards the resolver rather than
this route.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
