---
schema: 2
id: TASK-040118
title: Document the name endpoint and what each answer means
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, docs, protocol]
depends_on: [TASK-040117]
verify:
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest.theDocumentDescribesTheSetNameEndpoint'
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` contracts `PUT /api/me/name` — its body, its five answers and the permanence
rule — and the documentation test checks that claim against the code rather than trusting the prose.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §5's table is the contract to transcribe |

## Scope

- A `### Set display name` section in the `## HTTP endpoints` part of `docs/protocol.md`, beside the
  two existing ones: method and path, the `X-Device-Id` authentication, the `{"name": "…"}` body,
  the canonicalisation the server applies, and a row per answer — `200`, `400`, `401`, `403`, `409`
  — saying what each means and which are retryable.
- The profile response table gains `displayName`, marked nullable, with `null` meaning *never set*
  and a note that the server fabricates no placeholder.
- **The document must say the choice is permanent**, and that `403` is what a second name gets. A
  client author who reads only this file has to learn that a rename is impossible.
- Two tests added to `HttpEndpointDocumentationTest`, in its existing style — reflecting over the
  DTO rather than substring-matching where it can.

## Out of scope

- `DuelSummaryResponse.opponentDisplayName` — `STORY-0402` documents it when it exists.
- Any behaviour change. If the document and the code disagree, the code is right and the ticket to
  change it is a new one.

## Tests

`HttpEndpointDocumentationTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `theDocumentDescribesTheSetNameEndpoint` | the document contains `PUT /api/me/name` and every one of `400`, `401`, `403`, `409` inside that section — enumerated, not one status standing for the rest |
| `theDocumentMarksTheDisplayNameNullable` | the profile section's `displayName` row says nullable, and the reflection over `ProfileResponse` agrees that the property is nullable |

The existing `theDocumentedFieldNamesAllExist` already fails if the new row names a field the DTO
does not have, so it is the third check and needs no editing.

## Acceptance criteria

- [ ] Both tests above pass
- [ ] `theDocumentDescribesTheSetNameEndpoint` asserts all four failure statuses individually
- [ ] `theDocumentMarksTheDisplayNameNullable` reads nullability from `ProfileResponse` by
      reflection, not from a hard-coded expectation
- [ ] Every test already in `HttpEndpointDocumentationTest` passes unchanged
- [ ] The document states that a display name is permanent once set
- [ ] `./gradlew :poker-server:check -PrequireDocker=true` passes — the story's last ticket is where
      the whole module is asserted green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
