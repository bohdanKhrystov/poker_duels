---
schema: 2
id: TASK-041021
title: The protocol document contracts the removed-name field
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, docs, protocol]
depends_on: [TASK-041020]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md`'s profile-endpoint table documents `displayNameRemoved`, and a test holds the
document to it.

## Why a test and not just the row

`HttpEndpointDocumentationTest` reflects over `ProfileResponse` and enforces **documented ⇒ exists**;
it does **not** enforce **exists ⇒ documented** (`ADR-0053` §5). So the field can ship undocumented
and green, which is why the document row is a ticket with an assertion rather than a note on
`TASK-041018`.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify — one table row |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify — one new test |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — the field and its KDoc |

## Scope

- The `### Profile endpoint` field table gains one row, after `displayName`, in `ADR-0053` §5's
  words:

  | Field | Type | Semantics |
  | --- | --- | --- |
  | displayNameRemoved | boolean | `true` when the player holds no display name **and** a name has been removed from them by an operator (`ADR-0052`). `false` for a player who never set one, and `false` again once they set a new one. Never says anything about another player. |

- The row goes **inside** the section the test bounds — between `### Profile endpoint` and
  `### Set display name`. `HttpEndpointDocumentationTest` derives that span from the two headings,
  and a row placed outside it is invisible to every assertion in the file.
- One new test in `HttpEndpointDocumentationTest`, beside the ones that already check the profile
  section.
- No other section, heading or row moves. The test file's `sectionBetween` pairing is coupled to
  heading order, and its own comment records that moving a section fails fourteen tests.

## Out of scope

- `PROTOCOL_VERSION`, `docs/protocol-versions.md` and any fingerprint. `ADR-0053` §5 gives the
  mechanism: `ProfileResponse` is reachable from neither protocol root.
- `docs/operations.md` — `TASK-041022`.
- Making `HttpEndpointDocumentationTest` enforce **exists ⇒ documented** in general. That is a
  worthwhile change and a different ticket; widening this one would put a reflection sweep over four
  DTOs into a documentation ticket.

## Tests

`HttpEndpointDocumentationTest`. One test added; nothing existing edited.

| Test | Proves |
| --- | --- |
| `theProfileSectionDocumentsTheRemovedNameField` | The profile section's documented field names contain `displayNameRemoved`, the row for it says `boolean`, and — using the file's existing reflection helper — `ProfileResponse` really has a property of that name. **The wrong implementations it must fail against**: a row added to the *set-name* section or below `### Set display name` (`documentedFieldNames(profileSection)` will not see it), and a row naming a field the DTO does not have |

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theProfileSectionDocumentsTheRemovedNameField` passes
- [ ] Every test already in `HttpEndpointDocumentationTest` passes with its assertions unchanged
- [ ] `docs/protocol.md` gains exactly one table row and no heading in it moves
- [ ] `docs/protocol-versions.md` is unmodified and `ProtocolVersionLedgerTest` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
