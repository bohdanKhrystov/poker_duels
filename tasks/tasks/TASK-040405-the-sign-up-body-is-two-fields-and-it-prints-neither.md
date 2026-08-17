---
schema: 2
id: TASK-040405
title: The sign-up body is two fields, and it prints neither
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, http, protocol]
depends_on: [TASK-040404]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.AuthDtosTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`SignUpRequest` is the whole request body of `POST /api/auth/sign-up` — a handle and a password,
nothing else — and printing one yields a fixed redaction rather than the password.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/AuthDtos.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/AuthDtosTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — `SetNameRequest`, whose *no default value* KDoc explains why a missing field must be refused rather than silently become `""` |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | read — the `protocolJson.decodeFromString(Serializer, json)` idiom these tests copy |

## Scope

- `@Serializable public data class SignUpRequest(val handle: String, val password: String)` in
  `duels.poker.server.protocol.http`.
- **Two fields, and no third.** No `playerId` and no address field: a body carrying a player id is
  a client asserting who it is (`ADR-0002`, `ADR-0030` §1), and `ADR-0031` §5 says no address field
  exists on this endpoint.
- Neither field has a default value, for `SetNameRequest`'s stated reason: a missing field must be
  refused rather than become the empty string, which would sign somebody up with an empty password.
- **Override `toString()`** to return the constant `REDACTION = "SignUpRequest(redacted)"`, on the
  same reasoning `PresentedSecret` carries: a data class prints its constructor arguments, so the
  default `toString` puts a plaintext password into any log line, exception message or debugger
  frame that touches the decoded body. `equals`/`hashCode` are untouched.
- The class is a shape, not a rule: it validates nothing. `TASK-040407` owns the fold and the
  length policy.

## Out of scope

- A response DTO. Every outcome of sign-up answers an empty body (`ADR-0048` §6), so there is
  nothing to declare.
- Making `PresentedSecret` serializable, or holding one here. The wire type is a `String`; the
  endpoint constructs the value class.
- `docs/protocol.md` — `TASK-040414`.

## Tests

`AuthDtosTest`, using `protocolJson`, as `ProfileDtosTest` does.

| Test | Proves |
| --- | --- |
| `bothFieldsDecodeFromAJsonObject` | `{"handle":"bob","password":"hunter2222"}` decodes to a `SignUpRequest` holding exactly those two strings |
| `aMissingPasswordIsRefused` | `{"handle":"bob"}` throws rather than decoding to an empty password |
| `aMissingHandleIsRefused` | `{"password":"hunter2222"}` throws |
| `anUnrecognisedFieldIsRefused` | `{"handle":"bob","password":"hunter2222","playerId":"p-1"}` throws — so a client physically cannot assert its own identity through this body, which is `ADR-0002` enforced by the decoder rather than by a check somebody must remember |
| `printingTheRequestPrintsNeitherField` | `SignUpRequest("bob", "hunter2222").toString()` equals `SignUpRequest.REDACTION`, and `assertFalse` on it containing `"hunter2222"` **and** on it containing `"bob"`. Both, from one instance whose two field values are distinct strings |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `SignUpRequest` declares exactly two properties, `handle` and `password`, neither with a
      default value
- [ ] `anUnrecognisedFieldIsRefused` asserts a throw, not a decoded value with the field dropped
- [ ] `printingTheRequestPrintsNeitherField` asserts equality with the redaction constant **and**
      the absence of both field values
- [ ] `AuthDtos.kt` contains no validation: no `require`, no `init`, no length check, no fold
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
