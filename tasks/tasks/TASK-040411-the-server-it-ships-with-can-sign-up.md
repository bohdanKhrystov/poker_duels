---
schema: 2
id: TASK-040411
title: The server it ships with can sign up
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, auth, http, wiring]
depends_on: [TASK-040410]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`duelServer` installs the sign-up route against a real `PostgresCredentials`, so the server `main`
starts is the server the tests exercise.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | modify |

## Scope

- `ServerComponents` gains `val credentials: Credentials`, and `serverComponents(…)` builds
  `PostgresCredentials(dataSource)` — the public single-argument constructor, which supplies the
  real `Argon2Hasher`.
- `Application.duelServer` gains one line: `authRoutes(components.reads, components.credentials)`,
  beside the existing `profileRoutes(…)` call.
- Nothing else moves. No new configuration value, no new pool, no route order change.

`ServerComponentsTest` needs no edit: it calls the `serverComponents(…)` factory rather than
constructing the data class, so a new property with a value the factory supplies does not reach it.
Check that before assuming — if it does construct one directly, the ticket is over budget and should
be split rather than silently taking a fourth file.

## Out of scope

- Rate limiting and any configuration for it — `DEC-048`, answered by `ADR-0055`, built in
  `STORY-0405`.
- `PostgresCredentials` itself, which is finished and merged.
- Anything about sessions. `serverComponents` gains one collaborator, not two; `auth_session` has no
  reader until `STORY-0405`.

## Tests

`DuelServerRoutesTest`, two tests added beside `theProfileRouteIsInstalled`, whose comment —
*"a route that was never installed answers 404, which makes this falsifiable"* — is the model for
the first of these.

| Test | Proves |
| --- | --- |
| `theSignUpRouteIsInstalled` | `POST /api/auth/sign-up` with no device id answers `401`, not `404`. **The wrong implementation this must fail against is a `duelServer` that never calls `authRoutes`**, which answers `404` |
| `aHandshakeThenASignUpAnswersCreated` | a socket handshake mints the profile for device `wired`, then `POST /api/auth/sign-up` with that device id, a fresh handle and an 8-code-point password answers `201` — the whole chain, through the real `PostgresCredentials`, against the container |

Every existing test in the file passes unchanged.

## Acceptance criteria

- [ ] Both tests above pass
- [ ] `theSignUpRouteIsInstalled` asserts `401` and would fail against `404`
- [ ] `aHandshakeThenASignUpAnswersCreated` goes through `serverComponents(config, dataSource)` and
      `duelServer(…)`, with no double and no directly-installed route
- [ ] `ServerComponents` gains exactly one property, and `serverComponents` gains exactly one
      constructed collaborator
- [ ] `duelServer` gains exactly one call
- [ ] The four existing tests in `DuelServerRoutesTest` and every test in `ServerComponentsTest`
      pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
