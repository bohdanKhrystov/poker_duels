---
schema: 2
id: TASK-040522
title: The document names sign-up's seventh answer
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, docs, http, auth, rate-limit]
depends_on: [TASK-040521]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
---

## Goal

`docs/protocol.md`'s sign-up section carries `ADR-0055` §3's seventh row, so a client author can
tell a deliberate refusal from a broken server.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

## Scope

- One row added to the sign-up response table: `429 Too Many Requests` — *too many sign-ups have
  recently reached the hashing step from this address; nothing was written and no field was
  refused.*
- One sentence saying there is **no** `Retry-After` and no body, and that the same rule does not
  exist on sign-in, whose over-budget answer is deliberately identical to a wrong password
  (`ADR-0027` §6). A client sharing one error mapper between the two forms must not manufacture a
  throttled state where there is none (`ADR-0056` §1).
- The other six rows are untouched, and so is every section marker — this adds no heading, so
  `sectionBetween`'s chain does not move.

## Out of scope

- The words a screen shows — `ADR-0056` fixes the shape, `EPIC-06` the wording, `STORY-0412` the
  screen.
- Any code under `src/main`.

## Tests

`HttpEndpointDocumentationTest`

| Test | Proves |
| --- | --- |
| `theSignUpSectionNamesTheThrottledAnswer` | `signUpSection` contains `429` |
| `theSignUpSectionPromisesNoRetryAfter` | it says there is no `Retry-After` header |
| `theSignInSectionHasNoThrottledAnswer` | `signInSection` does **not** contain `429` — the pair is the point: one section gains a row and the neighbouring one must not |

## Acceptance criteria

- [ ] All three test methods above pass
- [ ] Every test that was in `HttpEndpointDocumentationTest` before this ticket still passes,
      unedited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
