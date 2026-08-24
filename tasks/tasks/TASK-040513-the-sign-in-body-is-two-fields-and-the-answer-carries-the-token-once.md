---
schema: 2
id: TASK-040513
title: The sign-in body is two fields, and its answer carries the token exactly once
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, http, auth, dto]
depends_on: [TASK-040512]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.http.AuthDtosTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`SignInRequest` and `SignInResponse` exist, the request prints neither of its fields, and the
response is the one place in the system where a session token is ever serialised.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/AuthDtos.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/AuthDtosTest.kt` | modify |

Read the `SignUpRequest` already in `AuthDtos.kt` — this copies its `toString` treatment exactly —
and `ADR-0027` §2. Nothing else.

## Scope

- `@Serializable public data class SignInRequest(val handle: String, val password: String)` with
  an overridden `toString()` returning a fixed redaction that names **neither** field.
  `SignUpRequest` already does this and the reason is the same: a handle in a log line beside a
  failure is an enumeration oracle, and a password in one is worse.
- `@Serializable public data class SignInResponse(val sessionToken: String)` — one field, no
  player id, no name, no balance. A client learns who it is from `GET /api/me`, which is one round
  trip and one source of truth.
- `SignInResponse.toString()` is **also** redacted. It is a data class, so the generated
  `toString` would print the token, and this is the one type in the codebase that holds a live
  token in plaintext.
- KDoc on `SignInResponse`: this is the only response body that ever carries the token, it is
  returned exactly once at issue, and nothing reads it back (`ADR-0027` §2).

## Out of scope

- The endpoint — `TASK-040514`.
- Any field for the device id. `ADR-0002`: a client presents a credential and is told who it is; a
  body carrying a player id the server did not resolve is the defect this DTO must not enable.

## Tests

`AuthDtosTest` — new methods only, nothing existing edited.

| Test | Proves |
| --- | --- |
| `aSignInRequestPrintsNeitherField` | `SignInRequest("alice", "hunter2").toString()` contains neither `"alice"` nor `"hunter2"` — **both, checked separately**, because a redaction that keeps the handle passes a password-only check |
| `aSignInRequestStillRoundTrips` | it decodes from `{"handle":"alice","password":"hunter2"}` to an equal value — redaction hides it from logs, not from the codec |
| `aSignInRequestRefusesAnUnknownField` | a body with an extra key fails to decode, matching `SignUpRequest` |
| `aSignInResponseCarriesOnlyTheToken` | encoding `SignInResponse("t")` produces exactly `{"sessionToken":"t"}` — a golden string, so a field added later is a failing test rather than a leak |
| `aSignInResponsePrintsNoToken` | `SignInResponse("supersecret").toString()` does not contain `"supersecret"` |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `SignInResponse` declares exactly one property
- [ ] Every test that was in `AuthDtosTest` before this ticket still passes, unedited
- [ ] Every command in `verify:` exits 0

## Proof

Delete the `toString` override on `SignInResponse` and `aSignInResponsePrintsNoToken` goes red
while the golden encoding test stays green — the two are checking different things and both are
needed.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
