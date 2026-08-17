---
schema: 2
id: TASK-040409
title: One create, with the player the server resolved
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, http, security]
depends_on: [TASK-040408]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The write path is pinned: a successful sign-up makes exactly one `create` call, carrying the folded
handle and the player id the server resolved, and every refusal makes none.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify — add tests only |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | read |

## Scope

- Tests only. `TASK-040408` wrote branches 4 and 5 of the handler; this ticket is where they stop
  being unasserted. **If a test below fails, fix `AuthRoutes.kt`** — but no other behaviour changes,
  and nothing in `AuthRouteTest.kt` written by `TASK-040408` is edited or weakened.
- The fixture in every test resolves device `"alice"` to a profile whose `playerId` is `"p-alice"`.
  **The two strings must differ**, or a handler that passed the device id through where a player id
  belongs would pass every test in the file.

## Out of scope

- The database. These are route tests against recording doubles; `TASK-040412` proves the row.
- Empty bodies and log lines — `TASK-040410`.
- Concurrency. Two sign-ups racing for one player is the residual `TASK-040403` records and
  `ADR-0030` §7 declines to close with an index; nothing here tests it, and nothing here should
  add a lock.

## Tests

`AuthRouteTest`, added to the class `TASK-040408` created.

| Test | Proves |
| --- | --- |
| `aSignUpAnswersCreated` | a valid request from `alice` answers `201`, and the body is empty |
| `theCreateCallCarriesTheResolvedPlayerAndTheFoldedHandle` | after one valid request with handle `Bob_1` and password `hunter2222`, `createCalls` has **exactly one** entry and it equals `CreateCall(PlayerId("p-alice"), CredentialKind.PASSWORD, "bob_1", PresentedSecret("hunter2222"))`. Four assertions in one: the identity is the server's (`p-alice`, never `alice` and never anything from the body), the kind is `password`, the identifier is folded, and the secret is passed through untouched — `ADR-0048` §4 trims nothing |
| `aPlayerWhoAlreadyHoldsAPasswordIsRefused` | with the double built `holds = true`, the same valid request answers `409` and `createCalls` is **empty** — nothing is written, and no Argon2 work is spent (`ADR-0030` §1) |
| `aTakenHandleIsRefused` | with the double built to answer `IdentifierTaken`, the request answers `409` — the same status as the line above, from a different cause, because both are `409` in `ADR-0048` §6's table |
| `aRefusedHandleReachesNeitherPortFunction` | handle `"ab"` answers `400`, and `createCalls` **and** `holdsCalls` are both empty: a refusal that costs a round trip is a refusal that leaks work |
| `aPasswordOutsideTheBoundsReachesNeitherPortFunction` | a 7-code-point password and a 129-code-point password each answer `422` with both recorders empty. Two inputs, one per bound |
| `theGuardIsAskedBeforeCreate` | on the success path, `holdsCalls` holds exactly one `(PlayerId("p-alice"), CredentialKind.PASSWORD)` pair. **The wrong implementation this must fail against is one that calls `create` first and inspects the guard afterwards**, which would hash before knowing whether it may write |

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] `theCreateCallCarriesTheResolvedPlayerAndTheFoldedHandle` asserts `createCalls.size == 1` and
      compares all four recorded arguments
- [ ] The device id `"alice"` and the player id `"p-alice"` are different strings in every fixture in
      the file
- [ ] `aPlayerWhoAlreadyHoldsAPasswordIsRefused` and `aRefusedHandleReachesNeitherPortFunction`
      assert `createCalls.isEmpty()`, not merely a status code
- [ ] `aPasswordOutsideTheBoundsReachesNeitherPortFunction` exercises **both** bounds
- [ ] The seven tests `TASK-040408` wrote pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
