---
schema: 2
id: TASK-041618
title: A token from the mailbox proves the address
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, auth, security]
depends_on: [TASK-041617]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.VerifyEmailRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`POST /api/auth/verify-email` exists: `204` for a good token and `400` for one that is
unknown — unauthenticated, because the token is the proof.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/RecoveryDtos.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/VerifyEmailRouteTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` — the decode-then-refuse shape,
the `CancellationException` rethrow, and the `call.respond(HttpStatusCode.X)` house style;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`;
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/AuthDtos.kt`.

## Scope

- `RecoveryDtos.kt` holding `@Serializable public data class VerifyEmailRequest(val token: String)`
  — **no default value**, so a missing field is a `400` rather than silently the empty string.
- `RecoveryRoutes.kt` with

  ```kotlin
  public fun Application.recoveryRoutes(
      recoveryEmails: RecoveryEmails,
      passwordResets: PasswordResets,
      identities: IdentityResolver,
      credentials: Credentials,
  )
  ```

  installing `post("/api/auth/verify-email")` only. **All four parameters are declared now, even
  though this ticket uses one**, so that `TASK-041620`, `TASK-041623`, `TASK-041625` and
  `TASK-041626` fill handlers without ever editing this signature or its call site — five route
  tickets contending for one line in `Application.kt` is how a sequential chain deadlocks. Mark
  the three unused ones `@Suppress("UNUSED_PARAMETER")` if `detekt` objects, with a comment naming
  the tickets that consume them.
  A **new file**, not an addition to `AuthRoutes.kt`: five endpoints land here across five tickets,
  and growing `authRoutes` to eight routes makes every one of those tickets touch the same file.
- The route is **unauthenticated**. It reads no `X-Device-Id` and no `Authorization` header — the
  token is the proof, and a player who attached an address on a phone must be able to click the
  link on a laptop that has never seen this site.
- The handler decodes, then calls `verifyPending`, then maps exhaustively over `VerifyEmailResult`:
  `Verified` → `204`, `Refused` → `400`, `AddressTaken` → `409`. A `when` with **no `else` branch**,
  so a fourth result value fails the build — this is the first exhaustive `when` over that type and
  the gate `TASK-041607` recorded it was missing.
- Every decode failure is `400`, the cause never changing the answer.


## Out of scope

- Installing the route in `Application.kt`. It is deferred to `TASK-041622`, which is the last
  route ticket in this chain, so `Application.kt` is edited once rather than four times and no two
  route tickets contend for it. Until then `VerifyEmailRouteTest` installs `recoveryRoutes` itself
  inside `testApplication`, which is how `AuthRoutes` was first tested.
- `POST /api/auth/recovery-email`, which creates the pending row this endpoint consumes —
  `TASK-041625`. This ticket's fixture creates the pending row by calling
  `RecoveryEmails.claimPending` directly.
- The `409`, the expired token and the second use of a token — `TASK-041619`, a second test file
  over the same route. They are separated because this ticket is already a DTO, a route file and a
  test, and three refusals with their own fixtures do not fit in `S` beside them.
- `reset-password` — `TASK-041620`.
- Telling the caller *which* of unknown, expired and consumed happened. `ADR-0031` §5 makes the
  three indistinguishable and the port already collapses them into one value.
- Rate-limiting this endpoint. `ADR-0031` §5 budgets `recovery-email` and `forgot-password` and
  **not** this one: its caller already holds a 256-bit token, so there is no search space to
  protect.

## Tests

`VerifyEmailRouteTest`, `testApplication` over a real `PostgresRecoveryEmails`.

| Test | Proves |
| --- | --- |
| `aGoodTokenAnswersTwoHundredAndFour` | Claim, then `POST` the token: `204`, an **empty** body, and `hasRecoveryEmail` for that player is now `true` |
| `anUnknownTokenAnswersFourHundred` | A token never issued: `400`, empty body |
| `aMalformedBodyAnswersFourHundred` | Three requests — empty body, `{}` with no `token`, and `{"token":123}` — all `400`. The missing-field case is what the no-default DTO buys |
| `theRouteReadsNoIdentityHeader` | A `POST` bearing neither `X-Device-Id` nor `Authorization` succeeds with a good token. A guard added later reddens here |

## Acceptance criteria

- [ ] All four `VerifyEmailRouteTest` tests pass
- [ ] The `when` over `VerifyEmailResult` in `RecoveryRoutes.kt` has no `else` branch
- [ ] `VerifyEmailRequest` has no default value on `token`
- [ ] `RecoveryRoutes.kt` contains no read of `X-Device-Id`, no read of `Authorization`, and no
      call to `IdentityResolver`
- [ ] `AuthRoutes.kt` is byte-unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Map `Verified` to `200` instead of `204`.
   **`aGoodTokenAnswersTwoHundredAndFour` reddens alone** on the status; its `hasRecoveryEmail`
   assertion still passes, because the write happened. Revert.
2. Map `Refused` to `404`. **`anUnknownTokenAnswersFourHundred` reddens alone.**
   `aMalformedBodyAnswersFourHundred` fails at decode before reaching the port and **stays green** —
   that asymmetry is the prediction to check, because if it reddens too, a decode failure is being
   routed through the port, which is a defect of its own. Revert.
3. Give `VerifyEmailRequest.token` a default of `""`.
   **`aMalformedBodyAnswersFourHundred` reddens on its `{}` case only** — the request now decodes,
   reaches the port with an empty token, and gets `400` anyway from `Refused`. So it **stays
   green**. Record that result: the no-default rule is not gated by this test, and the criterion
   above is a review criterion. The gate would be a `500` or a `200`, and neither happens. Revert.
4. Add `call.resolvedPlayerOrNull(identities) ?: return@post call.respond(Unauthorized)` at the top
   of the handler. **`theRouteReadsNoIdentityHeader` reddens alone**, *expected 204, got 401* — and
   every other test reddens too, since none of them sends a header. Four reddening is the
   prediction; if only one does, the other tests are sending identity headers they should not.
   Revert.
5. Add an `else -> call.respond(HttpStatusCode.OK)` to the `when`, then add a fourth
   `VerifyEmailResult` implementor. **The build succeeds**, where without the `else` it fails at
   `compileKotlin`. Run both halves — this is the mutation that shows what the exhaustive `when` is
   for, and it is a compile gate rather than a test. Revert both.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
