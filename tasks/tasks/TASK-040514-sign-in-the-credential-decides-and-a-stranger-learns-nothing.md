---
schema: 2
id: TASK-040514
title: "POST /api/auth/sign-in: the credential decides, and a stranger learns nothing"
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 5
atomic:
  - the Kotlin compiler — authRoutes gains a required `sessions` parameter, so Application.kt and both test files that install it stop compiling in the same commit
  - the Kotlin compiler again — ServerComponents must expose the AuthSessions the composition root passes
labels: [server, http, auth, session]
depends_on: [TASK-040513]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.SignUpSecrecyTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`POST /api/auth/sign-in` verifies a handle and a password, answers `200` with one freshly issued
token, and answers a wrong password and an unknown handle with the identical response — same
status, same body, same headers.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify | the route, and the new `sessions` parameter |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify | `compileKotlin` — the composition root has no `AuthSessions` to pass until this exposes one |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | `compileKotlin` — *No value passed for parameter 'sessions'* |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify | `compileTestKotlin` — every `authRoutes(...)` call site, plus this ticket's own tests |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` | modify | `compileTestKotlin` — the same call, and it must keep passing unchanged in every other respect |

`grep -rn "authRoutes(" poker-server/src` names exactly these three call sites plus the
declaration; there is no fourth.

Read `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §6,
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` (the dummy-hash path is
**already built** and this route must not reimplement it) and
`poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt`. Nothing else.

## Scope

- `authRoutes(reads, credentials, identities, sessions: AuthSessions)`. `serverComponents` builds
  one `PostgresAuthSessions(dataSource, wallClock)` and hands the **same instance** to the
  resolver and to the routes — two instances would be two stores only by accident of both being
  stateless.
- The route, in this order, and the order is the behaviour:
  1. decode `SignInRequest`; every decode failure is `400` with an empty body;
  2. `loginHandleOrNull(request.handle)`; **`null` does not answer `400`** — it answers the same
     `401` a wrong password does, because *"that is not a valid handle"* tells a stranger the shape
     of the namespace and, worse, tells them their target's handle *is* valid;
  3. `credentials.verify(CredentialKind.PASSWORD, handle, PresentedSecret(request.password))`;
  4. `null` → `401`, empty body, no header of any kind added;
  5. a `PlayerId` → `sessions.issue(playerId)` → `200` with `SignInResponse(token.value)`.
- **No identity is resolved.** This endpoint takes no `X-Device-Id` and no `Authorization`, reads
  neither, and works for a browser that has never connected — `ADR-0030` §2's recovery case. It
  writes exactly one row, in `auth_session`, and **nothing at all to `player`**.
- The constant-time property is `PostgresCredentials.verify`'s and is already merged: an unknown
  identifier is verified against `DUMMY_PHC` before answering `null`. This route must not
  short-circuit around it — no *does this handle exist* pre-check, ever.
- `SignUpSecrecyTest` gains the `sessions` argument at its `authRoutes` call and **changes in no
  other way**: no assertion in it moves, because sign-up's behaviour does not.

## Out of scope

- Any rate limit. `ADR-0027` §6's budget is `TASK-040523`, at the end of this chain, and
  `ADR-0074` fixes its numbers and where the check sits. **Do not add it here**: the reserve goes
  before the hash and the refund after the verification, which is a change to this handler's order
  that its own ticket makes with its own tests. Shipping without it is safe only while `EPIC-07`
  hosts nothing, which `ADR-0055`'s *"the deployment wins"* clause already covers.
- Sign-out — `TASK-040515`. The document — `TASK-040516`.
- Rehashing on a raised Argon2 cost — `ADR-0054`, not this story.

## Tests

`AuthRouteTest` — new methods only. The credentials double gains a `verify` that answers from a map
instead of throwing (it currently throws `UnsupportedOperationException`, which was right while
only sign-up existed); that is a widening of a double, not a weakening of a test.

| Test | Proves |
| --- | --- |
| `aCorrectCredentialAnswersTwoHundredAndAToken` | `200`, and the body is `{"sessionToken":"…"}` carrying the exact token the `AuthSessions` double issued |
| `theTokenNamesThePlayerTheCredentialNamed` | the double records which `PlayerId` `issue` was called with, and it is the one `verify` answered — with **two** credentials in the fixture resolving to two different players, and both driven, so the route cannot be issuing for a constant |
| `aWrongPasswordAndAnUnknownHandleAreIndistinguishable` | both answer `401`; the two responses' status, body text **and** header name-sets are compared field by field with `assertEquals`, not eyeballed |
| `anUnusableHandleAnswersTheSameFourHundredAndOne` | `handle = "!!"` — which `loginHandleOrNull` refuses — answers `401` with the same empty body, and `credentials.verify` is still called zero times |
| `aMalformedBodyIsFourHundred` | a body missing `password` answers `400`, and `verify` is called zero times |
| `nothingIsWrittenWhenTheCredentialFails` | on a `401`, the `AuthSessions` double recorded no `issue` |
| `theResponseNeverEchoesWhatWasSent` | the `200` body contains neither the handle nor the password |

## Acceptance criteria

- [ ] All seven test methods above pass
- [ ] `SignUpSecrecyTest` passes, with its diff limited to the added `authRoutes` argument
- [ ] `AuthRoutes.kt` contains no call that asks whether an identifier exists before `verify`
- [ ] `git diff --name-only` lists exactly the five rows of the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Answer `400` for a handle `loginHandleOrNull` refuses and
`anUnusableHandleAnswersTheSameFourHundredAndOne` goes red on its own. Issue the token for a
hard-coded player and only `theTokenNamesThePlayerTheCredentialNamed` goes red — which is why that
test drives two credentials rather than one.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
