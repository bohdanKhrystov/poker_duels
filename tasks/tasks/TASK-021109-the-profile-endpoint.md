---
schema: 2
id: TASK-021109
title: Answer GET /api/me for a known device, refuse anything else
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, profiles, coins]
depends_on: [TASK-021108]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`GET /api/me` returns the calling device's profile and coin balance as JSON, and refuses a request
whose `X-Device-Id` header is absent, blank or unknown.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt`,
`poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` (the
`Application.<route>(deps)`-installed-by-the-caller idiom to copy),
`poker-server/src/test/kotlin/duels/poker/server/HealthRouteTest.kt` (the `testApplication` idiom),
`docs/adr/ADR-0012-device-bound-anonymous-profiles.md`.

## Scope

- Package `duels.poker.server.http`, one file:

  ```kotlin
  public const val DEVICE_ID_HEADER: String = "X-Device-Id"

  public fun Application.profileRoutes(reads: ProfileReads) {
      routing {
          get("/api/me") {
              val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
              if (profile == null) call.respond(HttpStatusCode.Unauthorized) else call.respond(profile)
          }
      }
  }

  private fun ApplicationCall.deviceIdOrNull(): DeviceId? =
      request.headers[DEVICE_ID_HEADER]?.takeIf { it.isNotBlank() }?.let(::DeviceId)
  ```

- The `takeIf { it.isNotBlank() }` is not decoration: `DeviceId`'s `init` rejects a blank value, so
  without it a header of spaces answers `500` instead of `401`. Comment says why.
- **Plain HTTP, not the socket, and it creates nothing.** The route holds a `ProfileReads` and
  nothing else — no `PlayerDirectory`, no `DataSource`, no `resolve`. Profile creation happens on
  the socket handshake only (`ADR-0012`), so a crawler hitting this endpoint mints no rows. KDoc on
  `profileRoutes` says exactly that, and cites `ADR-0011` for why the route never sees SQL.
- Absent, blank and unknown all answer `401 Unauthorized` with an empty body. The device id is the
  only credential v0.1 has, an unknown one is an invalid credential, and answering `404` for
  unknown would tell a caller which device ids exist.
- KDoc on `profileRoutes`, in the shape `duelSocket` uses: it is installed by the caller rather
  than from `Application.module()`, because installing it there means handing `module()` a
  `DataSource`, which `STORY-0212` owns. Until then the only caller is a test.
- `Application.module()` already installs `ContentNegotiation { json() }`, so `call.respond(profile)`
  serialises. Do not install a second `ContentNegotiation`.

## Out of scope

- `GET /api/me/duels` — `TASK-021110` adds it to this file and this test class.
- Changing `Application.kt`, `module()`, `HealthRouteTest`, `PokerServerModuleTest` or
  `ServerPluginsTest`. Wiring is `STORY-0212`'s, and doing it here would put three more files in
  this ticket's budget.
- Any real database in this ticket's tests — `TASK-021111` runs these routes against the container.

## Tests

`ProfileRouteTest`, JUnit 5, package `duels.poker.server.http`, using `testApplication` with
`application { module(); profileRoutes(reads) }`. `reads` is a small fake declared in this file
implementing `ProfileReads` over a `Map<String, ProfileResponse>`, recording every device id it was
asked about in a list; its `recentDuelsOf` returns an empty list for now. Assertions are on
`response.status` and `response.bodyAsText()`.

| Test | Proves |
| --- | --- |
| `aKnownDeviceGetsItsProfile` | `GET /api/me` with `X-Device-Id: alice` answers `200` and a body containing `"playerId":"p-alice"` and `"coinBalance":4` |
| `aNegativeBalanceIsReturnedUnclamped` | a device whose stored profile is `-3` answers `200` with a body containing `"coinBalance":-3` |
| `anAbsentDeviceIdHeaderIsRefused` | `GET /api/me` with no header answers `401`, and the fake was never asked about any device |
| `aBlankDeviceIdHeaderIsRefused` | a header of two spaces answers `401`, and the fake was never asked |
| `anUnknownDeviceIdIsRefused` | `X-Device-Id: ghost` answers `401` and an empty body |

## Acceptance criteria

- [ ] `ProfileRouteTest.aKnownDeviceGetsItsProfile` passes
- [ ] `ProfileRouteTest.aNegativeBalanceIsReturnedUnclamped` passes
- [ ] `ProfileRouteTest.anAbsentDeviceIdHeaderIsRefused` passes
- [ ] `ProfileRouteTest.aBlankDeviceIdHeaderIsRefused` passes
- [ ] `ProfileRouteTest.anUnknownDeviceIdIsRefused` passes
- [ ] `ProfileRoutes.kt` names no `DataSource`, `Connection`, SQL string, `PlayerDirectory` or
      `resolve`
- [ ] `ProfileRoutes.kt` contains no `install(` call
- [ ] No file other than the two listed above is added or changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
