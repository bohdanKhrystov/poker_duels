---
schema: 2
id: TASK-040612
title: The document names the device endpoint, and the section markers still chain
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, docs, http, revocation]
depends_on: [TASK-040611]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDocumentationTest'
---

## Goal

`docs/protocol.md` contracts `DELETE /api/me/device` and `ProfileResponse.deviceRouteLive`, and the
test that reads that document section by section still knows where every section starts and stops.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

Read, and do not edit:
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §5,
`docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` §1 and §2.

## Scope

- One new section, `### Revoke this device`, placed **immediately after `### Set display name`** and
  before `### Recent duels endpoint`, so the `/api/me/*` family reads together.
- **`HttpEndpointDocumentationTest` must move with it, and this is the trap.** That file builds each
  section with `sectionBetween(start, end)` where every `end` is the heading that *follows*.
  `sectionBetween` uses `indexOf`, so a section whose end marker is left pointing past a newly
  inserted heading silently grows into a **superset** that swallows the new section — assertions
  about set-name would then be satisfiable by text about revocation. Re-chain: `setNameSection` ends
  at `### Revoke this device`, and the new `deviceSection` ends at `### Recent duels endpoint`.
  **Only the marker strings change; no existing assertion is edited.**
- The section states, and each of these is a criterion below: `DELETE /api/me/device`;
  `Authorization: Bearer <token>` and **no `X-Device-Id` fallback**; no request body; `204 No
  Content` whether or not a binding was live; `401 Unauthorized` for a caller with no valid session,
  **including one presenting only a device id**; `409 Conflict` for a player holding no credential;
  that revocation is **permanent** and cannot be undone; that the caller's own session survives and
  every other session that player holds is deleted; and that no live socket is closed.
- The `### Profile endpoint` response table gains a `deviceRouteLive` row: `boolean`, true exactly
  when this player has a live device binding.
- **No device id appears in any documented response.** The document says so in the new section
  (`ADR-0049` §5's last bullet): a device id is a bearer credential, and a "your devices" listing is
  refused rather than merely absent.

## Out of scope

- `PROTOCOL_VERSION` and `docs/protocol-versions.md`. `ProfileResponse` is reachable from neither
  `ClientMessage` nor `ServerMessage`, and `DELETE /api/me/device` is plain HTTP, so
  `ProtocolVersionLedgerTest`'s fingerprint is byte-identical (`ADR-0049` and `ADR-0050` both say
  the version does not move). `ProtocolDocumentationTest` is in `verify:` to prove it.
- Any file under `poker-server/src/main`.
- The screens' copy — `ADR-0050` §3 assigns the exact strings to `EPIC-06` and the screen to
  `STORY-0412`.

## Tests

`HttpEndpointDocumentationTest` — the section constants are re-chained and one is added.

| Test | Proves |
| --- | --- |
| `theSetNameSectionIsStillWhereItWas` | `setNameSection` still contains `PUT /api/me/name` and its `409` row — the re-chaining did not empty it |
| `theDeviceSectionNamesItsMethodAndPath` | `deviceSection` contains `DELETE /api/me/device` |
| `theDeviceSectionNamesTheBearerHeaderAndRefusesTheDeviceFallback` | it contains `Authorization: Bearer` and says a device id alone is refused |
| `theDeviceSectionNamesAllThreeStatusCodes` | it contains `204`, `401` and `409` |
| `theDeviceSectionSaysRevocationIsPermanent` | it says the revocation cannot be undone |
| `theDeviceSectionSaysTheOtherSessionsEndAndThisOneDoesNot` | it says every other session ends and the calling session survives |
| `theDeviceSectionSaysNoSocketIsClosed` | it says live sockets are not closed |
| `theProfileSectionDocumentsDeviceRouteLive` | `documentedFieldNames(profileSection)` contains `deviceRouteLive`, and the file's existing reflection helper confirms `ProfileResponse` really declares a property of that name — the check that makes this a contract rather than a sentence |
| `noSectionOfTheDocumentPromisesADeviceIdInAResponse` | across the whole document, no response field table row names a field called `deviceId`. **This is a universal claim, so it must say what it rejects**: adding a `deviceId` row to any of the eight response tables reddens it |

## Acceptance criteria

- [ ] All nine test methods above pass
- [ ] Every test that was in `HttpEndpointDocumentationTest` before this ticket still passes, and
      the only edit to an existing line is a `sectionBetween` marker string
- [ ] `theDeviceSectionNamesAllThreeStatusCodes` is scoped to `deviceSection`, never to `doc`
- [ ] `ProtocolDocumentationTest` passes and `docs/protocol-versions.md` is unmodified
- [ ] Every command in `verify:` exits 0

## Proof

Add the new `### Revoke this device` section to `docs/protocol.md` **without** re-chaining
`setNameSection`'s end marker, then run the class.

**Nothing reddens** — and that is the finding, not a failure of the exercise. `sectionBetween`
locates its end marker with `indexOf`, so an unchained `setNameSection` runs from
`### Set display name` all the way to `### Recent duels endpoint` and *contains* the new section
rather than being emptied by it. `TASK-040516` measured exactly this on the sign-up section. So the
re-chaining is not something a red run will remind anyone about; it is a requirement of this ticket
and `theSetNameSectionIsStillWhereItWas` alone does not catch it either. What does catch it is the
criterion above: `deviceSection` must be built by `sectionBetween("### Revoke this device", "### Recent duels endpoint")`
and `setNameSection` must end at `### Revoke this device`, and a reviewer reads those two lines.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
