---
schema: 2
id: TASK-140808
title: The document names the password field
type: task
status: done
parent: STORY-1408
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, protocol, docs]
depends_on: [TASK-140807]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest.theProfileSectionDocumentsHasPassword' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest' -PrequireDocker=true
  - grep -qF 'hasPassword' docs/protocol.md
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/protocol.md`'s *Profile endpoint* field table names `hasPassword`, and a test fails if the
document and `ProfileResponse` ever disagree about it in either direction.

## Why it lands after the field, and separately

`HttpEndpointDocumentationTest.theDocumentedFieldNamesAllExist` checks **documented ⇒ exists**: a
documented field that `ProfileResponse` does not declare fails, and a declared field nobody
documented does not. Measured on the `ADR-0070` probe tree, `./gradlew check -PrequireDocker=true`
was fully green with `hasPassword` on the DTO and absent from the document. So the row **must** land
after `TASK-140805` — it cannot land before, and nothing forced it to land with it. This ticket adds
the missing direction for this one field, in the shape `theProfileSectionDocumentsDeviceRouteLive`
already uses.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` — the KDoc the row
paraphrases;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §1 and §8.

## Scope

- Add one row to the *Profile endpoint* table in `docs/protocol.md`, **last**, after
  `hasRecoveryEmail`, matching the declaration order of `ProfileResponse`. The `Type` cell is
  `boolean`. The `Semantics` cell says: `true` exactly when this player holds a password credential;
  `false` covers a player who never made one. Name `ADR-0132` and say that no other endpoint answers
  it for a player the caller did not resolve to (`ADR-0132` §3).
- **The row must not contain the word `null`, in any case.**
  `theDocumentDoesNotCallANonNullFieldNullable` reflects every non-nullable `ProfileResponse` property
  against its documented row and fails one that claims nullability. `hasPassword` is non-nullable.
- Add `theProfileSectionDocumentsHasPassword` to `HttpEndpointDocumentationTest`, copying the shape of
  `theProfileSectionDocumentsDeviceRouteLive` immediately above it: the field name is in
  `documentedFieldNames(profileSection)`, `rowFor` finds the row and it mentions `boolean`, and
  `ProfileResponse::class.memberProperties` has a `hasPassword`.
- The section markers `sectionBetween` uses are unchanged — the row goes inside the existing table
  between `### Profile endpoint` and `### Set display name`.

## Out of scope

- Any other row, section or endpoint in `docs/protocol.md`. In particular the `/api/me` **section**
  is also edited by `STORY-1410` (`ADR-0135`'s `GET /api/me/device`); the two stories are serialised
  for exactly this reason and whichever runs second rebases.
- `docs/protocol-versions.md`. `ADR-0132` §7: `ProfileResponse` is a plain-HTTP DTO, not emitted from
  a serial descriptor, so `PROTOCOL_VERSION` does not move, no version row is owed and
  `protocol.gen.ts` is not regenerated.
- A general *every declared field is documented* gate. That would be a good thing and it is not this
  ticket's — it would redden for fields other stories own.

## Tests

`HttpEndpointDocumentationTest`

| Test | Proves |
| --- | --- |
| `theProfileSectionDocumentsHasPassword` | The profile section documents a field named `hasPassword`, its row says `boolean`, and `ProfileResponse` actually declares a property by that name |

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theProfileSectionDocumentsHasPassword` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentDoesNotCallANonNullFieldNullable` passes — so the new
      row does not contain the word `null`
- [ ] `HttpEndpointDocumentationTest.theDocumentedFieldNamesAllExist` passes
- [ ] `grep -F 'hasPassword' docs/protocol.md` finds a match
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
