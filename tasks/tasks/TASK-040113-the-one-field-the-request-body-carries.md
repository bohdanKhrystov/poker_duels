---
schema: 2
id: TASK-040113
title: The one field the request body carries
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, http, protocol]
depends_on: [TASK-040112]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest.aBodyWithAnUnknownFieldIsRefused'
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The name-setting request has a declared shape — `{"name": "…"}` — and a body that is not that shape
fails to decode rather than being half-read.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolCodec.kt` | read — `protocolJson` and its strictness settings |

## Scope

- `@Serializable public data class SetNameRequest(val name: String)` in the same file as the
  response DTOs, with KDoc saying it is the body of `PUT /api/me/name` and that the server
  canonicalises what it receives.
- **No default value**, for the same reason the response fields have none, and because a defaulted
  `name` would turn an empty body into a request to set the empty string.
- Three tests added; nothing existing moves.

## Out of scope

- The route that decodes it — `TASK-040115`.
- Any validation. `SetNameRequest` is a shape, not a rule; the rule is
  `canonicalDisplayNameOrNull`.

## Tests

`ProfileDtosTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `aSetNameRequestDecodesItsName` | `{"name":"bob"}` decodes to `SetNameRequest("bob")` |
| `aBodyWithNoNameIsRefused` | `{}` throws rather than decoding to a default |
| `aBodyWithAnUnknownFieldIsRefused` | `{"name":"bob","playerId":"p-1"}` throws — a client may not assert who it is, and the codec is where that is cheapest to enforce |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `SetNameRequest.name` has no default value
- [ ] `aBodyWithAnUnknownFieldIsRefused` decodes with the same `Json` the routes use, so the test
      and the server agree about strictness
- [ ] Every test already in `ProfileDtosTest` passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
