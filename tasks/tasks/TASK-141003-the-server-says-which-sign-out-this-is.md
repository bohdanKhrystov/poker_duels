---
schema: 2
id: TASK-141003
title: The server says which sign-out this is
type: task
status: backlog
parent: STORY-1410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, account, auth]
depends_on: [TASK-141002]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DeviceRouteTest' -PrequireDocker=true
  - grep -q 'tests="16" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.DeviceRouteTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.ProfileDtosTest' -PrequireDocker=true
  - grep -q 'tests="27" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.protocol.http.ProfileDtosTest.xml
  - grep -q 'public data class DeviceStandingResponse(val signOutHandsANewProfile: Boolean)' poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt
  - sh -c '! grep -q "signOutHandsANewProfile: Boolean = " poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt'
  - sh -c '! grep -rqF "signOutHandsANewProfile" poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt poker-server/src/main/kotlin/duels/poker/server/session/'
  - git diff --exit-code -- web-client/src/protocol/protocol.gen.ts docs/protocol-versions.md poker-server/src/main/resources/db/migration
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`GET /api/me/device` exists, is session-required, and answers one boolean that says which of
`ADR-0131` §1's two sign-outs the caller is about to make.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/DeviceRouteTest.kt` | modify |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§§4–5, `poker-server/src/test/kotlin/duels/poker/server/http/ProfileReadsDoubles.kt` (the
`identitiesFor` / `FixedAuthSessions` doubles this test builds on).

## Scope

- `ProfileDtos.kt` gains, verbatim from `ADR-0135` §4:

  ```kotlin
  @Serializable
  public data class DeviceStandingResponse(val signOutHandsANewProfile: Boolean)
  ```

  **No default value**, for the reason the file's own KDoc states three times: `Application.module()`
  installs `Json` with `encodeDefaults = false`, so a defaulted property is omitted from the wire
  for every caller whose answer is the default — and here that is the caller for whom the answer
  matters. KDoc it with what the boolean means and that it is a fact about the **pair of
  credentials on one request**, never about the player, which is the line `ADR-0135` §Consequences
  draws between this DTO and `ProfileResponse`.
- `deviceRoutes` gains `get("/api/me/device")` beside the `delete` it already installs. It needs no
  new parameter and no wiring change: `Application.kt:103` already passes `components.identities`.
  - Identity is resolved through `identities.resolve(token, call.deviceIdOrNull())`, in the same
    exhaustive `when` shape the `delete` uses. **A session is required**: `Identity.Device`,
    `Identity.UnknownDevice`, `Identity.Refused` and `Identity.Anonymous` all answer
    `401 Unauthorized` with an empty body (`ADR-0049` §5's reasoning, restated by `ADR-0135` §4 —
    a caller with no session has no sign-out to make).
  - **No credential guard.** `ADR-0135` §4: *"a session is issued by `POST /api/auth/sign-in` and
    by nothing else, so a session-identified caller holds a password credential by construction"*,
    which makes `ADR-0131` §2's invariant a property of the identity rule. `credentials` is not
    consulted on this path.
  - The body is `DeviceStandingResponse(signOutHandsANewProfile = !identities.namesPlayer(deviceId, playerId))`
    where `deviceId` is the header the caller presented on **this same request** — and when none
    was presented, the answer is `true` without asking the directory anything. Never a stored
    association: `ADR-0135` §4 says computing it any other way turns the route into an enumeration
    surface.
- If adding the handler pushes `deviceRoutes` past detekt's `LongMethod` budget, extract the body
  into a private suspend function in the same file — the shape `ProfileRoutes.kt` already uses for
  `respondWithDuels`, and named as such in its KDoc. `./gradlew :poker-server:detekt` is in the
  `verify:` block and is the thing that decides.

## Out of scope

- **`docs/protocol.md`.** `TASK-141004` writes the section and re-chains
  `HttpEndpointDocumentationTest`'s section boundaries. Nothing in that file moves here.
- **Any write.** `ADR-0135` §1: no `INSERT`, no `UPDATE`, no `DELETE` against `device_binding`,
  `player`, `credential`, `duel` or `duel_result`, on either branch, at any point.
- **`ProfileResponse`.** No seventh field, no eighth; `ADR-0135` §Alternatives 1 rejected that on a
  measured fact about `PUT /api/me/name`'s second producer.
- **The socket.** `Hello`, `Welcome`, `ProtocolCodec`, `protocol.gen.ts`, `PROTOCOL_VERSION` and
  `docs/protocol-versions.md` are untouched — `git diff --exit-code` says so in `verify:`.
- **`ProfileDtosTest`.** It gains no test: the wire shape is proved through the route, against the
  real `Json` the application installs, which is the configuration a DTO unit test cannot see. Its
  count is pinned at **27** to say it was not edited.

## Tests

`DeviceRouteTest`, **10 today → 16**. Every new test calls `module()` before `deviceRoutes(...)`,
as `ProfileRouteTest` does, because this is the first response on this route with a body and
`ContentNegotiation` lives in `module()`.

| Test | Proves |
| --- | --- |
| `aSessionWhoseDeviceNamesThemIsHandedANewProfile` | token `t-1` plus `X-Device-Id: device-1`, where `device-1` resolves to `player-1` and `t-1` names `player-1`: `200 OK` and a body that is **exactly** `{"signOutHandsANewProfile":true}`. The literal is written out in full and never assembled from a constant — it is the wire, and `encodeDefaults = false` is what it is checking |
| `aSessionWhoseDeviceNamesAnotherPlayerKeepsItsProfile` | the same token beside a second device id resolving to a **different** player: `200 OK` and exactly `{"signOutHandsANewProfile":false}`. The two tests share one fixture map and differ only in the header, so a route that ignored the header could not pass both |
| `aSessionPresentingNoDeviceIsHandedANewProfile` | token alone, no `X-Device-Id`: `200 OK` and `true`. `ADR-0131` §1 row three, and the browser this route must never answer `false` to |
| `aRevokedBindingIsHandedANewProfile` | a device id the directory answers nothing for — which is what a revoked binding is, since `findOrNull` filters on `revoked_at IS NULL`: `200 OK` and `true`. `ADR-0135` §5's own row, the only outcome it changes that `ADR-0131` did not discuss |
| `aDeviceIdAloneIsRefusedOnTheStandingRoute` | `X-Device-Id: device-1` and no `Authorization`: `401 Unauthorized` and an **empty** body. The positive control is the first test above, against the same fixture — `device-1` genuinely resolves, so this is not a `401` for the wrong reason |
| `theStandingRouteNeverAsksAboutCredentials` | `RecordingCredentials(holds = false)` — the fixture that makes `DELETE` answer `409` — still gets `200 OK` here, and `credentials.holdsCalls` is **empty**. `ADR-0135` §4's *no credential guard, and none is needed*, asserted as an absence of a call and not just as a status |

The ten merged tests are **not edited**: all ten are about `DELETE`, this ticket adds a `GET`, and
the count gate of 16 is 10 + 6.

## What would still pass if the coder were wrong

- **A route that always answers `true`** passes four of the six and fails
  `aSessionWhoseDeviceNamesAnotherPlayerKeepsItsProfile`. That is the one row `ADR-0131` §2
  forbids getting wrong, and it is why the two-input pair shares a fixture.
- **A route that reads the device id from anywhere but the request** — a stored association, or the
  session's own player — answers `true` for every caller and fails the same test.
- **A DTO with a default value** serialises correctly under `protocolJson` (which sets
  `encodeDefaults = true`) and is **omitted from the real wire**. Two gates catch it: the exact
  body assertions run through `module()`'s own `Json`, and the `! grep -q "signOutHandsANewProfile:
  Boolean = "` gate refuses a default in the source.
- **Copying `DELETE`'s `409` guard** leaves every status test green for a player who holds a
  password. `theStandingRouteNeverAsksAboutCredentials` is the only thing that sees it, which is
  why it asserts an empty `holdsCalls` and not merely a `200`.
- **A body assertion written as `contains`** would pass for a response carrying extra fields. The
  assertions are `assertEquals` against the whole body string.

## Acceptance criteria

- [ ] `DeviceRouteTest` reports `tests="16" skipped="0" failures="0" errors="0"`
- [ ] All six named tests pass
- [ ] `ProfileDtosTest` still reports `tests="27" skipped="0" failures="0" errors="0"`
- [ ] The `DeviceStandingResponse` declaration gate exits 0 and the no-default gate exits 0
- [ ] The gate proving `signOutHandsANewProfile` appears in no codec and no session source exits 0
- [ ] `git diff --exit-code` over `protocol.gen.ts`, `docs/protocol-versions.md` and the migration
      directory exits 0
- [ ] `./gradlew :poker-server:ktlintCheck`, `:poker-server:detekt` and
      `check -PrequireDocker=true` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
