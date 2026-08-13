---
schema: 2
id: TASK-021112
title: Document both read endpoints in docs/protocol.md
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, docs]
depends_on: [TASK-021111]
verify:
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest' --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` describes `GET /api/me` and `GET /api/me/duels` beside the socket messages, and
a test fails if either endpoint, the device-id header or the limit rule stops being documented.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolDocumentationTest.kt` (the
existing guard over this document, and the regex it applies to table rows),
`poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt`,
`poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`.

## Scope

- Add one `## HTTP endpoints` section to `docs/protocol.md`, after `## Messages` and before
  `## Notes`, covering for each endpoint: the method and path, that identity is the `X-Device-Id`
  header and is the same device id as the socket handshake (`ADR-0012`), that an absent, blank or
  unknown value answers `401` and creates no profile, the response fields, and — for the duels
  endpoint — that `limit` defaults to `10`, is capped at `50`, that a non-numeric or non-positive
  value answers `400`, and that a player with no duels gets `200` and an empty array.
- Say plainly that these two are **plain HTTP and not socket frames**: they carry no `type`
  discriminator, they are not `ServerMessage`s, and the lobby reads them before any socket exists.
- Say that `coinBalance` and `coinDelta` are signed and that a negative balance is a correct answer
  (`ADR-0014`), and that `handsPlayed` is `null` until `DEC-014` is answered.
- **Do not add a table row whose first cell is a single backticked identifier.**
  `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` reads every line matching
  ``^\| `([A-Za-z]+)` \|`` as a claim that a protocol message of that name exists, and would fail
  on a row beginning `` | `ProfileResponse` | ``. Rows keyed by `` `GET /api/me` `` are safe — the
  space and slash keep them out of that pattern. Name the response types in prose or in a later
  column. This constraint is the reason the ticket carries that test in `verify`.
- No production code changes, and no change to `ProtocolDocumentationTest`: its five assertions
  stay exactly as they are, and the document edit is written so they keep passing.

## Out of scope

- Documenting the socket's duel messages, or `DEC-010`'s room messages.
- Generating TypeScript for these types — `DEC-007`, and this story does not wait on it.
- An OpenAPI document. Two endpoints in the file `EPIC-03` already has to read beats a second
  format nobody generates from.

## Tests

`HttpEndpointDocumentationTest`, JUnit 5, package `duels.poker.server.http`. Locate the document
the way `ProtocolDocumentationTest` does — walking up from the working directory to the first
`docs/protocol.md` — and assert against the constants, never against the numbers spelled again.

| Test | Proves |
| --- | --- |
| `theDocumentDescribesTheProfileEndpoint` | the document contains `GET /api/me` |
| `theDocumentDescribesTheRecentDuelsEndpoint` | the document contains `GET /api/me/duels` |
| `theDocumentNamesTheDeviceIdHeader` | the document contains `DEVICE_ID_HEADER`'s value |
| `theDocumentStatesTheLimitDefaultAndCap` | the document contains `"defaults to \`$DEFAULT_DUEL_LIMIT\`"` and `"capped at \`$MAX_DUEL_LIMIT\`"`, so a change to either constant fails until the prose follows |
| `theDocumentSaysAnUnknownDeviceIsRefused` | the document contains `401` in the same line as `X-Device-Id` or within the endpoints section |

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theDocumentDescribesTheProfileEndpoint` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentDescribesTheRecentDuelsEndpoint` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentNamesTheDeviceIdHeader` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentStatesTheLimitDefaultAndCap` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentSaysAnUnknownDeviceIsRefused` passes
- [ ] `ProtocolDocumentationTest`'s five tests pass, and that file is not edited — the document's
      new lines are written so its assertions do not move
- [ ] No line added to `docs/protocol.md` matches the regex ``^\| `([A-Za-z]+)` \|``
- [ ] No file other than the two listed above is added or changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
