---
schema: 2
id: TASK-040410
title: No outcome carries a body, and nothing on the path can print a secret
type: task
status: backlog
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, http, security]
depends_on: [TASK-040409]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.SignUpSecrecyTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Every outcome of sign-up answers an empty body, and no file on the sign-up path holds a logging call
that could print the password it was handed — both asserted by a sweep rather than by inspection.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | read — the `testApplication` setup these sweeps reuse |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | read — the "walk up from `File("")` until the repository root is found" idiom the source sweep copies |

## Scope

- One test class, no production change. If a sweep goes red, the fix belongs in the file it names.
- The outcome sweep drives the **real route** through `testApplication`, once per outcome, and
  collects `(name, status, body)` triples. It does not re-derive statuses from a table of
  constants — a sweep that asserts against its own expectations proves nothing.
- The source sweep reads files from disk by path. It is not reflection: what it is looking for is a
  call that does not appear in any signature.

## Out of scope

- Widening the sweep to the whole codebase. `PublicApiHasNoHashTest` already covers
  `duels.poker.server.auth` and `duels.poker.server.db` structurally; this is the four files sign-up
  adds, in `http` and `protocol.http`, which that sweep does not reach.
- Any assertion about `docs/protocol.md` — `TASK-040414`.
- Ktor's own logging. This ticket asserts what this project's files contain, not what a framework
  does with an exception.

## Tests

`SignUpSecrecyTest`.

| Test | Proves |
| --- | --- |
| `everyOutcomeAnswersAnEmptyBody` | drives all **seven** outcomes — `401` (no device id), `400` (undecodable body), `400` (refused handle), `422` (short password), `409` (already holds), `409` (handle taken), `201` (success) — and asserts every collected body is `""`. The test asserts `outcomes.size == 7` **first**, so a sweep that silently drove three cases cannot pass |
| `noOutcomeEchoesTheSecretsItWasSent` | the same seven outcomes, every request carrying the handle `Bob_1` and the password `hunter2222`, asserting no response body contains `"hunter2222"`, `"bob_1"` or `"Bob_1"`. Also asserts the sweep is non-empty. Implied by the line above today, and it is the assertion that survives the day somebody adds a reason field |
| `noFileOnTheSignUpPathNamesALogger` | reads `AuthRoutes.kt`, `SignUpFields.kt`, `AuthDtos.kt` and `PasswordPolicy.kt` from `src/main`, asserting none contains `println`, `LoggerFactory`, `Logger`, `log.info`, `log.debug`, `log.warn` or `log.error`. Asserts all four files were found **and are non-empty** before checking, so a wrong path cannot make the sweep vacuously green |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `everyOutcomeAnswersAnEmptyBody` asserts the sweep drove exactly seven outcomes before it
      asserts anything about their bodies, and the seven statuses collected are
      `401, 400, 400, 422, 409, 409, 201`
- [ ] `noOutcomeEchoesTheSecretsItWasSent` sends the same handle and password in every request and
      asserts on all three literals
- [ ] `noFileOnTheSignUpPathNamesALogger` resolves all four paths, asserts each file's text is
      non-empty, and names the offending file in its failure message
- [ ] The sweeps drive the route installed by `authRoutes`; no test in this file asserts against a
      hand-written table of expected statuses in place of the real handler
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
