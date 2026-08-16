---
schema: 2
id: TASK-040115
title: "PUT /api/me/name: identity first, then the name it accepts"
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, routes, identity]
depends_on: [TASK-040114]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest.theCanonicalNameIsWhatReachesThePort'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`PUT /api/me/name` exists: it refuses an unknown caller before it reads a body, refuses a name the
rules refuse, and otherwise answers `200` with the profile carrying the canonical name.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | modify — its three call sites of `profileRoutes(profileReads)` need the same added argument `ProfileRouteTest` gets, or the module does not compile |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | read — the canonicaliser this route calls |

> **The real count is four, not three.** `lint_tickets.py` caps `files_touched` at 3, so the
> frontmatter cannot say so. The fourth is `ProfileEndpointsDatabaseTest.kt`: `profileRoutes`
> gaining a required second parameter breaks every existing caller that does not pass one, and
> that file is a caller the original scope missed. Recorded here rather than worked around.

## Scope

- `profileRoutes` takes a second parameter, `writes: ProfileWrites`, and `duelServer` passes
  `components.writes`. That is the only change in `Application.kt`.
- The route, in this order — the order is the behaviour:
  1. resolve the device id and its profile; absent, blank or unknown → `401`, empty body, **before
     the body is read**;
  2. decode `SetNameRequest`; a body that fails to decode → `400`;
  3. `canonicalDisplayNameOrNull`; `null` → `400`;
  4. `writes.setDisplayName(...)`; `NameSet` → `200` with the profile.
- `NameTaken` and `AlreadyNamed` are `TASK-040116`'s two answers. Handle the `when` exhaustively
  now — return `409` and `403` — but this ticket's tests cover the first four steps, and the next
  ticket is where those two are proven.
- `ProfileRouteTest` gains a `FakeProfileWrites` beside the existing `FakeProfileReads`, and every
  existing test passes one. **This ticket owns those call sites**: eleven tests gain an argument
  and change in no other way — no assertion moves, no name changes. (The ticket originally said
  nine; the file already held eleven when this ticket started.)
- `ProfileEndpointsDatabaseTest`'s three call sites gain a real `PostgresProfileWrites(dataSource)`
  the same way and for the same reason — they call `profileRoutes` directly and change in no other
  way either.
- KDoc on `profileRoutes` describing the new route the way the existing two are described,
  including why identity comes first.

## Out of scope

- `409` and `403` — `TASK-040116`.
- `docs/protocol.md` — `TASK-040118`.
- Anything against a real database — `TASK-040117`.

## Tests

`ProfileRouteTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `aKnownDeviceSetsItsName` | `200`, and the body is the profile the port returned, with its `displayName` |
| `theCanonicalNameIsWhatReachesThePort` | a body of `"  Bob  "` reaches `setDisplayName` as `"Bob"` — the fake records what it was passed, so this asserts the canonicalisation happened before the port, not after |
| `anAbsentDeviceIdIsRefusedBeforeTheBodyIsRead` | `401`, and the fake writes was never called — asserted on the fake, not inferred from the status |
| `anAbsentDeviceIdIsRefusedBeforeAMalformedBodyIsRead` | no identity, and a body that cannot even decode → `401`, not `400` — the test that tells "identity first" apart from "identity checked before the port, after a body that happened not to matter" |
| `anUnknownDeviceIdIsRefusedBeforeTheNameIsSet` | `401`, and again the port was not called. Named to avoid colliding with the existing `GET /api/me` test `anUnknownDeviceIdIsRefused`, which this ticket's table originally reused by name |
| `aNameTheRulesRefuseIsABadRequest` | a body of `"  "` → `400`, and the port was not called |
| `aBodyThatIsNotTheRequestShapeIsABadRequest` | `{"nickname":"bob"}` → `400`, and the port was not called |

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] Five of them assert the port was **not** called, on the fake — a `400` (or `401`, for the
      malformed-body-with-no-identity case) that still wrote is the defect this catches
- [ ] `theCanonicalNameIsWhatReachesThePort` asserts the exact string the fake received
- [ ] `anAbsentDeviceIdIsRefusedBeforeAMalformedBodyIsRead` answers `401`, not `400` — proof that
      identity is checked before the body is even parsed, not merely before the port is called
- [ ] The eleven tests already in `ProfileRouteTest` pass with only the added argument changed: no
      assertion moves and none is weakened
- [ ] `Application.kt` changes on exactly one line
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
