---
schema: 2
id: TASK-040117
title: A name set over HTTP comes back on the next read
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, http, e2e, identity]
depends_on: [TASK-040116]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileEndpointsDatabaseTest.theStoredNameIsTheCanonicalOne' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ProfileEndpointsDatabaseTest' -PrequireDocker=true
---

## Goal

The whole write path is proven against the real database and the real routes: a device sets a name,
reads it back on `GET /api/me`, and the two failures a second attempt can produce are the ones the
story promised.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | read — the route under test and the header it authenticates with |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | read — how the test boots the routes against a container database |

## Scope

- Four tests added to the existing class, using its existing container setup, its `module()` boot
  and its `X-Device-Id` header helper. No fakes anywhere: real routes, real ports, real Postgres.
- **The set-then-read pair is the point.** A test that only asserts the `200` from the `PUT` proves
  the response, not the storage.
- The `409` case needs two devices with two profiles; the `403` case needs one profile and two
  different names.
- Nothing existing moves.

## Out of scope

- Route-level branch coverage — `TASK-040115` and `TASK-040116` did that with fakes. This ticket is
  the join between them and the database.
- The document — `TASK-040118`.

## Tests

`ProfileEndpointsDatabaseTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `aNameSetOverHttpIsReadBackOnTheProfile` | `PUT` a name, then `GET /api/me` returns it — through the database, not from the `PUT`'s own response |
| `theStoredNameIsTheCanonicalOne` | `PUT`s `"  Élodie  "` in decomposed form and reads back the trimmed, composed string — one input that is wrong in all three ways the canonicaliser fixes |
| `aSecondDeviceCannotTakeTheSameName` | a second profile `PUT`ting the same name in another case gets `409`, and its own profile still reads back `displayName: null` |
| `aSecondNameForTheSameProfileIsForbidden` | the first profile `PUT`ting a different name gets `403`, and `GET /api/me` still returns the original |

## Acceptance criteria

- [ ] All four tests above pass against the container
- [ ] `aNameSetOverHttpIsReadBackOnTheProfile` asserts the name on the **`GET`** response body
- [ ] `theStoredNameIsTheCanonicalOne` sends an input that differs from its canonical form in
      leading space, trailing space and normalisation, and asserts the exact stored string
- [ ] Both refusal tests assert the profile afterwards, not only the status code
- [ ] Every test already in `ProfileEndpointsDatabaseTest` passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
