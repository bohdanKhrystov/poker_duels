---
schema: 2
id: TASK-040116
title: The two refusals a client must tell apart
type: task
status: ready
parent: STORY-0401
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, http, routes, identity]
depends_on: [TASK-040115]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest.theTwoRefusalsAreDifferentStatuses'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

A name somebody else holds answers `409`; a player who already has a name answers `403`. The two are
different on purpose, and a client can tell them apart with no response body.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §5, and the paragraph explaining why it is `403` and not a second `409` |

## Scope

- Map `SetNameResult.NameTaken` → `409 Conflict`, empty body; `AlreadyNamed` → `403 Forbidden`,
  empty body.
- A comment saying why they differ, in one sentence taken from the ADR's reasoning: `409` promises a
  conflict with the current state and invites a retry, and no state this client can reach makes the
  request succeed, so `403` is the honest code.
- The idempotent retry is **not** a refusal: a port that answers `NameSet` for a resend of the
  identical name is answered `200` by the code `TASK-040115` already wrote, and this ticket proves
  it at the route level too.

## Out of scope

- Anything the port decides. Which answer a given request produces is `TASK-040111`'s; this ticket
  is the mapping onto status codes.
- The document — `TASK-040118`.

## Tests

`ProfileRouteTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `aNameSomebodyElseHoldsIsAConflict` | port answers `NameTaken` → `409`, and the body is empty |
| `aPlayerWhoAlreadyHasANameIsForbidden` | port answers `AlreadyNamed` → `403`, and the body is empty |
| `theTwoRefusalsAreDifferentStatuses` | the same request shape against the two port answers produces two different codes — asserted in one test, so a mapping that collapsed them fails here even if each test above were written to match it |
| `aResentIdenticalNameIsStillTwoHundred` | port answers `NameSet` on a retry → `200` with the profile |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `409` and `403` both answer with an empty body, asserted
- [ ] The `when` over `SetNameResult` is exhaustive with no `else` branch — a fourth case added later
      must fail to compile rather than fall through to a status
- [ ] Every test already in `ProfileRouteTest` passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
