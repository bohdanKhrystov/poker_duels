---
schema: 2
id: TASK-040521
title: Sign-up over budget answers 429, and the budget meters the hash
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - the Kotlin compiler — authRoutes gains a required `budget` parameter, so Application.kt and both test files that install it stop compiling in the same commit
  - the Kotlin compiler again — ServerComponents must build the AttemptBudget the composition root passes
labels: [server, http, auth, rate-limit]
depends_on: [TASK-040520]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.SignUpSecrecyTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`POST /api/auth/sign-up` admits at most five requests per remote address per rolling fifteen
minutes **to the point where Argon2 runs**, and answers `429` with an empty body over budget.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify | the check, and the new `budget` parameter |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify | `compileKotlin` — the budget has to be built where the composition root can reach it |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | `compileKotlin` — *No value passed for parameter 'budget'* |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify | `compileTestKotlin` — every `authRoutes(...)` call site, plus this ticket's own tests |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` | modify | `compileTestKotlin` — the same call, and it must keep passing unchanged otherwise |

Read `ADR-0055` §§1, 3 and 4, and `ADR-0048` §6's response table. Nothing else.

## Scope

- `authRoutes(reads, credentials, identities, sessions, budget: AttemptBudget)`.
  `serverComponents` builds `AttemptBudget(config.signUpLimits(), clock)` — the **`ServerClock`**
  it already holds, not the wall clock.
- **The check goes last, immediately before `credentials.create`,** after identity, after the
  decode, after the field rules and after the `holdsCredential` guard. `ADR-0055` §1 is explicit:
  the budget meters *spending*, not failure, and the four refusals that cost no Argon2 must cost no
  budget either. Getting this order wrong is the whole defect — a budget checked first turns a
  `401` into a `429` and vice versa.
- Over budget → `429 Too Many Requests`, **empty body, no `Retry-After`, nothing written, no hash
  computed** (`ADR-0055` §3).
- The key is `call.request.origin.remoteAddress` and **nothing else**. Until `EPIC-07` installs the
  forwarded-header plugin, this must not read `X-Forwarded-For`: a client-supplied key is a limiter
  that looks green in every test and stops nothing. Say so in the KDoc, in one line, naming
  `EPIC-07`.
- Sign-in gets **no** budget in this ticket. `TASK-040523` builds it, on its own numbers and its own
  instance (`ADR-0074` §1), and it is the only caller that refunds.

## Out of scope

- Any change to the six answers `ADR-0048` §6 already fixes. `401`, `400`, `422` and both `409`s
  keep their exact meanings and their exact ordering.
- The document's seventh row — `TASK-040522`.
- What a client renders for a `429` — `ADR-0056`, `STORY-0412`.

## Tests

`AuthRouteTest` — new methods only; existing sign-up tests are untouched, because every one of them
sends fewer than five requests.

| Test | Proves |
| --- | --- |
| `theSixthSignUpFromOneAddressIsFourHundredAndTwentyNine` | five successful sign-ups, then a sixth answering `429` with an empty body |
| `anOverBudgetSignUpWritesNothing` | on that `429`, the credentials double recorded no `create` call |
| `aRefusedSignUpSpendsNoBudget` | five requests that stop at the `holdsCredential` `409`, then one that reaches `create` — it answers `201`, not `429`. **This is the ticket's real assertion**: a budget in the wrong place passes every other test here |
| `aMalformedBodySpendsNoBudget` | the same shape with five `400`s in front |
| `twoAddressesHaveTwoBudgets` | one address exhausted, a second address's first request still reaches `create` — with the remote address varied through the test client, so the key is proven to be read |
| `anOverBudgetRequestStillCounts` | after the sixth `429`, advancing the clock to just inside the original window still answers `429` — hammering extends rather than resets |

## Acceptance criteria

- [ ] All six test methods above pass
- [ ] `SignUpSecrecyTest` passes, with its diff limited to the added `authRoutes` argument
- [ ] `grep -rn "X-Forwarded-For" poker-server/src/main` finds nothing
- [ ] The `budget.admit(...)` call in `AuthRoutes.kt` appears **after** the `holdsCredential` guard
- [ ] `git diff --name-only` lists exactly the five rows of the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Move `budget.admit(...)` to the top of the handler and `aRefusedSignUpSpendsNoBudget` and
`aMalformedBodySpendsNoBudget` both go red while the other four stay green. That mutation is the
one this ticket exists to make impossible, and it is invisible to a test that only counts to six.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
