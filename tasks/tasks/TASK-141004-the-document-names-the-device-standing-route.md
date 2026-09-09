---
schema: 2
id: TASK-141004
title: The document names the device-standing route
type: task
status: ready
parent: STORY-1410
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, docs, account]
depends_on: [TASK-141003]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest' -PrequireDocker=true
  - grep -q 'tests="49" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.HttpEndpointDocumentationTest.xml
  - awk 'index($0,"### Device standing"){n++} END{exit (n!=1)}' docs/protocol.md
  - awk 'index($0,"### Device standing"){f=1} index($0,"### Recent duels endpoint"){f=0} f && index($0,"`GET /api/me/device`"){n++} END{exit (n<1)}' docs/protocol.md
  - awk 'index($0,"### Revoke this device"){f=1} index($0,"### Device standing"){f=0} f && index($0,"`DELETE /api/me/device`"){n++} END{exit (n!=1)}' docs/protocol.md
  - sh -c '! awk "/^### Device standing/{f=1} /^### Recent duels endpoint/{f=0} f" docs/protocol.md | grep -qF "409"'
  - sh -c '! awk "/^### Device standing/{f=1} /^### Recent duels endpoint/{f=0} f" docs/protocol.md | grep -qF "PROTOCOL_VERSION"'
  - git diff --exit-code -- web-client/src/protocol/protocol.gen.ts docs/protocol-versions.md
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/protocol.md` documents `GET /api/me/device` — its authentication, its one field and its two
statuses — and `HttpEndpointDocumentationTest` checks the section against the DTO **in both
directions**, which is something no other section in that file gets.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§§4–6, `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`,
`poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt`.

## Scope

- A new `### Device standing` section in `docs/protocol.md`, placed **between** `### Revoke this
  device` and `### Recent duels endpoint`, written in the shape the neighbouring sections use:
  *Method and path*, *Authentication*, *Request body: None*, *Response*, and a responses table.
  It must state:
  - `GET /api/me/device`, in a backticked code span;
  - `Authorization: Bearer <token>`, and that presenting a **device id alone** answers `401`, the
    same words the revoke section uses for the same rule;
  - that `X-Device-Id` travels alongside when the browser holds one, and that the answer is
    computed from the two credentials presented **on that same request** and never from a stored
    association;
  - the one response field, in the same `| Field | Type | Semantics |` table shape the profile
    section uses, so `documentedFieldNames` can read it;
  - `200 OK` and `401 Unauthorized` as table rows in the `| \`200 OK\` |` shape the revoke section
    uses, and the fact that no device id ever appears in the body.
- `HttpEndpointDocumentationTest` re-chains its section boundaries: `deviceSection` becomes
  `sectionBetween("### Revoke this device", "### Device standing")`, and a new
  `deviceStandingSection` is `sectionBetween("### Device standing", "### Recent duels endpoint")`.
  This is not optional — without it every `deviceSection` assertion could be satisfied by the new
  section's text instead of the revoke section's, and all of them are `contains`.

## Out of scope

- **A `409` row.** `ADR-0135` §4: `GET` needs no credential guard, and a document that offered one
  would describe a route this repository does not have. The `verify:` block refuses the string
  inside the new section.
- **Any mention of `PROTOCOL_VERSION` or the socket.** Nothing crosses it (`ADR-0135` §8), and
  `git diff --exit-code` over `protocol.gen.ts` and `docs/protocol-versions.md` says so.
- **The `### Profile endpoint` section.** `STORY-1408` landed `hasPassword` there at `TASK-140808`;
  this ticket does not open it, which is the whole reason `STORY-1408` and `STORY-1410` no longer
  collide.
- **Server or client code.** The route landed in `TASK-141003`.

## Tests

`HttpEndpointDocumentationTest`, **44 today → 49**.

| Test | Proves |
| --- | --- |
| `theDeviceStandingSectionNamesItsMethodAndPath` | `deviceStandingSection` contains the exact code span `` `GET /api/me/device` `` — pinned to the backticks, as `theDeviceSectionNamesItsMethodAndPath` is, so a longer path sharing the prefix does not satisfy it |
| `theDeviceStandingSectionRequiresASessionAndRefusesTheDeviceFallback` | it names `Authorization: Bearer` and says a caller presenting a `device id alone` is refused |
| `theDeviceStandingSectionNamesItsTwoStatusRows` | `` | `200 OK` | `` and `` | `401 Unauthorized` | `` appear as table rows, not as bare digits in prose — the same pinning `theDeviceSectionNamesAllThreeStatusCodes` uses and for the same stated reason |
| `theDeviceStandingSectionAndTheDtoAgreeInBothDirections` | every documented field name in the section is a property of `DeviceStandingResponse`, **and** every property of `DeviceStandingResponse` is documented in the section. `ADR-0142` §Context names this file's asymmetry as a defect — *"No DTO in that file gets both, so for every one of them a staleness of one kind is invisible"* — and this is the first section that closes it |
| `theRevokeSectionIsStillWhereItWas` | after the re-chaining, `deviceSection` still contains `` `DELETE /api/me/device` `` and its `` | `409 Conflict` | `` row. The file already carries `theSetNameSectionIsStillWhereItWas` for exactly this reason, written when the device section was first chained in; this is the same guard for the same kind of move |

The 44 merged tests are **not edited**, and the count gate of 49 is 44 + 5. Their assertions are
`contains` over `deviceSection`, which shrinks rather than moves, and all of what they name stays
inside it.

## What would still pass if the coder were wrong

- **Adding the section without re-chaining** leaves all 44 green and the five new ones green too:
  `deviceStandingSection` would not exist to be wrong, and `deviceSection` would silently span both
  sections. `theRevokeSectionIsStillWhereItWas` is the only thing that would notice — and only
  because it names the revoke section's own `409` row, which the new section is forbidden to carry.
  The forbidden-`409` `verify:` gate is the other half of that pair, and neither is redundant.
- **Documenting a field the DTO does not have, or shipping a field the document does not name**,
  each fails exactly one direction of the fourth test. A single-direction assertion — which is what
  every other DTO in this file gets — would miss one of them.
- **Putting the section after `### Recent duels endpoint`** makes `sectionBetween` throw
  `IllegalArgumentException` rather than read a wrong span, loudly, as the file's own comment
  records having measured.

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest` reports `tests="49" skipped="0" failures="0" errors="0"`
- [ ] All five named tests pass
- [ ] The `### Device standing` heading gate (`n != 1`) exits 0
- [ ] The two frame-scoped path gates exit 0 (`GET` inside the new section, exactly one `DELETE`
      inside the revoke section)
- [ ] The two absence gates (`409`, `PROTOCOL_VERSION`) over the new section exit 0
- [ ] `git diff --exit-code` over `protocol.gen.ts` and `docs/protocol-versions.md` exits 0
- [ ] `./gradlew check -PrequireDocker=true` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
