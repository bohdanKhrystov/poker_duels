---
schema: 2
id: TASK-040407
title: The handle is judged first, then the password
type: task
status: ready
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, http]
depends_on: [TASK-040406]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.SignUpFieldsTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A pure function turns a decoded `SignUpRequest` into either the folded handle or the one status code
that refuses it, so the two field rules and their order are decided in a function with no I/O in it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/SignUpFields.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpFieldsTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PasswordPolicy.kt` | read — the two predicates |
| `poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt` | read — `loginHandleOrNull`, the fold that is already written and is not to be re-implemented |

## Scope

- In `duels.poker.server.http`:

  ```kotlin
  internal sealed interface SignUpFields {
      data class Accepted(val handle: String) : SignUpFields
      data class Refused(val status: HttpStatusCode) : SignUpFields
  }

  internal fun signUpFieldsOf(request: SignUpRequest): SignUpFields
  ```

- The order is fixed and is part of the contract, not an implementation detail:
  1. `loginHandleOrNull(request.handle)` is `null` → `Refused(BadRequest)` (`ADR-0031` §1's fold,
     `ADR-0048` §6's `400` row).
  2. `passwordIsLongEnough` or `passwordIsWithinTheWorkBound` is `false` →
     `Refused(UnprocessableEntity)` (`ADR-0048` §6). **One status for both bounds** — the body is
     empty and there is exactly one rule to state, so nothing distinguishes them on the wire.
  3. otherwise `Accepted(theFoldedHandle)` — the folded form, never `request.handle`, because
     `Credentials` takes an identifier the caller has already folded.
- A comment recording *why* `422` and not `400`: `ADR-0031` §5 already spends `422` on exactly this
  refusal for `reset-password`, and one meaning per code beats two paths for one refusal. Neither
  code is an oracle — both state a property of the caller's own input.
- No I/O, no suspension, no `Credentials`, no `ProfileReads`. It reads the request and nothing else,
  which is what lets sign-up and `STORY-0416`'s reset apply the identical rule.

## Out of scope

- Identity, the body decode, the `409`s and the `201` — `TASK-040408`.
- Applying the maximum at sign-in. `STORY-0405` calls `passwordIsWithinTheWorkBound` directly;
  this function is the sign-up-and-reset shape, because it also applies the minimum.
- Any refusal reason in a body. `ADR-0048` §6: the body is empty and can be, because there is
  exactly one rule and the client already knows it.

## Tests

`SignUpFieldsTest`. Pure function, no `testApplication`, no `runBlocking`.

| Test | Proves |
| --- | --- |
| `aGoodHandleAndPasswordAreAccepted` | `SignUpRequest("bob", "hunter2222")` gives `Accepted("bob")` |
| `theAcceptedHandleIsTheFoldedOne` | `SignUpRequest("Bob_1", "hunter2222")` gives `Accepted("bob_1")`. **The wrong implementation this must fail against is one that returns `request.handle`**, which would store `Bob_1` under a unique index that has already lower-cased everything else |
| `aHandleTooShortIsFourHundred` | `"ab"` gives `Refused(BadRequest)` |
| `aHandleStartingWithAPunctuationCharacterIsFourHundred` | `"_alice"` gives `Refused(BadRequest)` — a second refused handle, so the `400` is not pinned by one shape of failure |
| `aPasswordOfSevenCodePointsIsFourTwoTwo` | `Refused(UnprocessableEntity)` |
| `aPasswordOfOneHundredAndTwentyNineCodePointsIsFourTwoTwo` | `Refused(UnprocessableEntity)` — the same status as the line above, asserted as the same value |
| `aPasswordOfEightCodePointsIsAccepted` | the boundary passes here too, so the endpoint's copy of the rule cannot drift off by one |
| `theHandleIsJudgedBeforeThePassword` | `SignUpRequest("ab", "short")` — **both** fields bad — gives `Refused(BadRequest)`. **The wrong implementation this must fail against is one that checks the password first**, which answers `422` and would send the form to mark the wrong field |

## Acceptance criteria

- [ ] All eight tests above pass
- [ ] `theHandleIsJudgedBeforeThePassword` uses a request whose handle **and** password are both
      refused, and asserts `BadRequest`
- [ ] `theAcceptedHandleIsTheFoldedOne` asserts the lower-cased value, not the value as typed
- [ ] Both password bounds answer the identical `HttpStatusCode` value
- [ ] `SignUpFields.kt` calls `loginHandleOrNull`, `passwordIsLongEnough` and
      `passwordIsWithinTheWorkBound`, and re-implements none of them
- [ ] `SignUpFields.kt` declares no `suspend` function and names no port
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
