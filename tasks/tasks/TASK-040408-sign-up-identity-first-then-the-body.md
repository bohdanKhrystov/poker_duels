---
schema: 2
id: TASK-040408
title: POST /api/auth/sign-up — identity first, then the body
type: task
status: backlog
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, auth, http, security]
depends_on: [TASK-040407]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`POST /api/auth/sign-up` exists, resolves the caller before it reads a byte of the body, and refuses
everything the rules refuse without touching `Credentials`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify — `deviceIdOrNull` from `private` to `internal`, and nothing else |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/SignUpFields.kt` | read |

## Scope

`public fun Application.authRoutes(reads: ProfileReads, credentials: Credentials)`, installing one
route, in this order — the order **is** the security property:

1. **Identity, before the body is read.** `call.deviceIdOrNull()?.let { reads.profileOf(it) }`;
   `null` → `401` with an empty body, and neither port is touched again. Absent, blank and unknown
   device ids are one answer, exactly as `GET /api/me` treats them.
2. **Decode.** `call.receive<SignUpRequest>()`, wrapped the way `PUT /api/me/name` wraps it —
   rethrow `CancellationException`, catch every other `Exception` → `400`. Empty body, wrong content
   type, malformed JSON, a missing field and an unrecognised field are all `400`; the cause does not
   change the answer.
3. **Fields.** `signUpFieldsOf(request)`; a `Refused` answers its status (`400` or `422`) and stops.
4. **The guard.** `credentials.holdsCredential(PlayerId(profile.playerId), CredentialKind.PASSWORD)`
   → `409`, nothing written (`ADR-0030` §1).
5. **The write.** `credentials.create(PlayerId(profile.playerId), CredentialKind.PASSWORD, handle, PresentedSecret(request.password))`
   → `Created` answers **`201 Created`**, `IdentifierTaken` answers `409`. Both bodies are empty.

Three things the KDoc must say, because each is a decision a reader will otherwise re-open:

- **Why identity is first.** Same reason `PUT /api/me/name` gives: answering `409` or `422` before
  identity is confirmed lets an anonymous caller learn whether a handle is taken.
- **Why `201` and not `204`.** Exactly one row is created and the endpoint's entire purpose is
  creating it; `204` would say nothing happened, which is the misreading `ADR-0030` exists to
  prevent. There is no `Location` header because there is no readable resource to point at —
  nothing reads a credential back (`ADR-0027` §1).
- **Why `player_id` comes from `profile.playerId`.** It is the identity the server resolved. A body
  carrying one is a client asserting who it is (`ADR-0002`), and `SignUpRequest` cannot even hold
  one.

Keep the KDoc to roughly fifteen lines. `ProfileRoutes`' forty-line header is three endpoints'
worth; this is one.

## Out of scope

- Wiring into `ServerComponents` and `duelServer` — `TASK-040411`. Nothing calls `authRoutes` until
  a later ticket does, and the tests install it directly.
- The write-path assertions — `TASK-040409`. This ticket writes branches 4 and 5; that ticket pins
  them. Say so in the PR description so a reviewer is not surprised by an unasserted branch.
- Any rate limiting. `ADR-0055` answers `DEC-048` — budgeted by remote address, `429` — and puts the
  work in `STORY-0405`, not here. Formerly phrased as: no ADR says whether sign-up is budgeted or
  what over budget answers, and `ADR-0048` §6's response table has six rows and no seventh.
- Issuing a session. Sign-up issues none; a client signs in afterwards like anybody else
  (`STORY-0405`).
- Any change to `ProfileRoutes` beyond the visibility keyword. The header rule stays one function
  with one implementation — copying it into `AuthRoutes.kt` is the defect this modification avoids.

## Tests

`AuthRouteTest`, with `testApplication`, `module()` and `authRoutes(reads, credentials)`, using
`TASK-040406`'s doubles. Every request that carries a body sets
`header(HttpHeaders.ContentType, "application/json")`, as `ProfileRouteTest` does.

| Test | Proves |
| --- | --- |
| `anAbsentDeviceIdIsRefused` | a **well-formed, entirely valid** body with no `X-Device-Id` answers `401`, so the `401` can only have come from the identity step, and `credentials.createCalls` and `holdsCalls` are both empty |
| `aBlankDeviceIdIsRefused` | a header of spaces answers `401` — not `500`, which is what a `DeviceId` constructed from a blank string would produce |
| `anUnknownDeviceIdIsRefused` | a device id no profile names answers `401`, the same status and the same empty body as the two above |
| `anUndecodableBodyIsFourHundred` | a known device sending `not json` answers `400`, and neither port function was called |
| `aBodyMissingThePasswordIsFourHundred` | `{"handle":"bob"}` answers `400` |
| `aBodyCarryingAPlayerIdIsFourHundred` | `{"handle":"bob","password":"hunter2222","playerId":"p-mallory"}` answers `400` and `createCalls` is empty — a client cannot assert an identity even by trying |
| `theBodyIsNeverDecodedBeforeIdentity` | `not json` **and** no device id answers `401`, never `400`. **The wrong implementation this must fail against is one that decodes first**, which answers `400` and thereby tells a stranger their body was the problem |

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] `ProfileRouteTest` passes unchanged — the only edit to `ProfileRoutes.kt` is
      `private fun` → `internal fun` on `deviceIdOrNull`, which changes no behaviour
- [ ] `anAbsentDeviceIdIsRefused` sends a body that would otherwise succeed, and asserts both
      recorders are empty
- [ ] `theBodyIsNeverDecodedBeforeIdentity` asserts `401`, and the test would fail against a handler
      that decoded first
- [ ] The three `401` tests assert the same status **and** the same empty body
- [ ] `AuthRoutes.kt` reads `profile.playerId` for the player id and never `request`; `SignUpRequest`
      has no field it could read one from
- [ ] `AuthRoutes.kt` contains no logging call and no `println`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
