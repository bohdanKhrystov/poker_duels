---
schema: 2
id: TASK-041620
title: A reset takes a token in a body, and never in a URL
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 4
atomic:
  - IdentityMovesNoCoinTest.everyApiPathInTheRouteSourcesIsExercisedByTheScenario — scans the four *Routes.kt sources for /api/… literals via :poker-server:test, so a new route and its SCENARIO_ENDPOINTS entry must land in the same commit
labels: [server, http, auth, security]
depends_on: [TASK-041619]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`POST /api/auth/reset-password` exists: `204` for a good token, `400` for a bad one, the token
accepted **only** in a request body, and no session issued.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/RecoveryDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordRouteTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` | modify |

The fourth row is `ADR-0070` §4 propagation, forced by the gate `atomic:` names above:
`SCENARIO_ENDPOINTS` gains `/api/auth/reset-password`, and its shared KDoc is extended to say why
the route moves no coin — `PasswordResets.consume` writes `credential`, deletes from
`password_reset` and `auth_session`, touching neither `player.coin_balance` nor `duel_result`.
Nothing else in the file changes.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` — the decode-then-refuse shape;
`poker-server/src/main/kotlin/duels/poker/server/auth/PasswordResets.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4 and §5;
`docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md` §7 — why this ticket
stands unchanged, and the one fixture constraint it must satisfy.

## Scope

- `RecoveryDtos.kt` gains
  `@Serializable public data class ResetPasswordRequest(val token: String, val newPassword: String)`
  — neither field defaulted, so a missing field is a `400`.
- `recoveryRoutes` gains a `PasswordResets` parameter and installs
  `post("/api/auth/reset-password")`: decode (`400` on any failure), then `consume(token, secret)`,
  then `204` on `true` and `400` on `false`.
- **The token is read from the decoded body only.** The handler contains no
  `call.request.queryParameters` and no `call.parameters`. `ADR-0031` §4 makes this the property
  that keeps the safe path the only path: a fragment never reaches a server, but a query parameter
  reaches every access log, proxy log and `Referer` header between here and the browser.
- The route is unauthenticated and reads no identity header — the token is the proof, and the
  player is resetting precisely because they cannot sign in.
- It issues **no session** and returns **no token**. §4: this keeps the endpoint incapable of
  handing out a credential, so a leaked reset link cannot be exchanged for a live session by
  anything but a full sign-in.
- **Every request in `ResetPasswordRouteTest` carries a `newPassword` of 8 to 128 code points —
  including the two that expect `400`.** This route applies no policy, so the constraint changes
  nothing it asserts today; it is here because `ADR-0080` §1 puts the policy check in **front** of
  the lookup when `TASK-041629` lands, and a shorter password will answer `422` from that day.
  `TASK-041629` requires this file to pass **unchanged**, and a 7-code-point password anywhere in it
  makes that requirement unsatisfiable — the coder there would face a ticket that cannot both
  implement its scope and leave this file standing. One sentence here costs nothing; discovering it
  there costs a stalled dispatch.

## Out of scope

- **The `422`, and the password policy** — `TASK-041629`. `ADR-0080` §1 settled the order and §7
  says this ticket *"stands unchanged and needs no re-cut"*, because the step it adds goes in
  **front** of `consume`, where nothing written here has to move. Until it lands this route applies
  **no policy** and answers `400` where §5 would answer `422`; that is a knowingly incomplete
  endpoint and `TASK-041629` completes it. Do not add a policy check here to "finish" it — a `422`
  from this ticket would break `TASK-041629`'s only oracle, which is that this file passes
  unchanged when the check arrives.
- Single use and concurrency at the wire — `TASK-041621`.
- The session sweep, and installing `recoveryRoutes` in `Application.kt` — `TASK-041622`.
- `forgot-password`, which mints the token this endpoint spends — `TASK-041626`. This ticket's
  fixture mints it by calling `PasswordResets.issue` directly.
- Any rate limit. `ADR-0031` §5 budgets `recovery-email` and `forgot-password` only, and this
  endpoint's caller already holds a 256-bit token.

## Tests

`ResetPasswordRouteTest`

| Test | Proves |
| --- | --- |
| `aGoodTokenAnswersTwoHundredAndFour` | `204`, an empty body, **no `Set-Cookie` header and no `sessionToken` substring anywhere in the response**, and the new password now signs in while the old one does not |
| `aBadTokenAnswersFourHundred` | A fabricated token: `400`, and the player's password still verifies against the original secret. Two inputs against one fixture — a route returning a constant fails one of them |
| `theTokenIsNotAcceptedAsAQueryParameter` | `POST /api/auth/reset-password?token=<good>` with a body carrying an **unknown** token string: `400`, and the password is unchanged. The good token in the query string does nothing |
| `theRouteReadsNoIdentityHeader` | A `POST` bearing neither `X-Device-Id` nor `Authorization` succeeds with a good token |

## Acceptance criteria

- [ ] All four `ResetPasswordRouteTest` tests pass
- [ ] `aGoodTokenAnswersTwoHundredAndFour` asserts the response has no `Set-Cookie` header, that the
      body text contains no `sessionToken`, and that the **old** password no longer verifies
- [ ] `theTokenIsNotAcceptedAsAQueryParameter` puts the good token **only** in the query string and
      a different, unknown token in the body
- [ ] `RecoveryRoutes.kt`'s reset handler contains no `queryParameters` and no `call.parameters`
- [ ] `RecoveryRoutes.kt` contains no call to `passwordIsLongEnough` and no `422` — those arrive
      with `TASK-041629`
- [ ] `ResetPasswordRequest` has no default on either field
- [ ] **Every** request in `ResetPasswordRouteTest` sends a `newPassword` of 8 to 128 code points —
      `aBadTokenAnswersFourHundred` and `theTokenIsNotAcceptedAsAQueryParameter` included, and the
      file contains no password shorter than 8 code points anywhere, in a fixture constant or inline
- [ ] `AuthRoutes.kt` is byte-unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Read the token as `call.request.queryParameters["token"] ?: request.token`.
   **`theTokenIsNotAcceptedAsAQueryParameter` reddens alone**, *expected 400, got 204*. Every other
   test sends its token in the body only and is unaffected. This is the mutation that justifies
   putting a *different* token in the body of that test: had the body carried the same good token,
   the request would answer `204` either way and the test would prove nothing. Revert.
2. Answer `204` unconditionally, ignoring `consume`'s result.
   **`aBadTokenAnswersFourHundred` reddens** on the status **and `theTokenIsNotAcceptedAsAQuery
   Parameter` reddens** too, since its body token is unknown. Two, and that pairing is expected —
   the query-parameter test doubles as a bad-token test, which is fine, because its *own* mutation
   above distinguishes it. Revert.
3. Answer `400` unconditionally.
   **`aGoodTokenAnswersTwoHundredAndFour` and `theRouteReadsNoIdentityHeader` both redden.** The
   positive controls; without them a route that refuses everything passes half this file. Revert.
4. Respond `HttpStatusCode.OK, SignInResponse(sessions.issue(playerId).value)` on success — which
   needs an `AuthSessions` the route does not have, so instead respond
   `HttpStatusCode.NoContent` with the header `Set-Cookie: sessionToken=x`.
   **`aGoodTokenAnswersTwoHundredAndFour` reddens alone**, on the `Set-Cookie` assertion, while its
   status assertion still passes. This is why the criterion names the header as well as the body:
   §4's *"issues no session"* is not a claim about a status code. Revert.
5. Make `consume` write the password but not delete the `password_reset` row — no, that is
   `TASK-041621`'s. Instead make `consume` return `true` without writing.
   **`aGoodTokenAnswersTwoHundredAndFour` reddens alone**, on the assertion that the new password
   signs in. Revert.
6. Shorten `aBadTokenAnswersFourHundred`'s `newPassword` to 7 code points.
   **Nothing reddens.** Record it rather than skipping it: the fixture constraint above is gated by
   **nothing in this ticket**, because this route judges no password. It becomes a failure two
   tickets later, in `TASK-041629`'s `ResetPasswordRouteTest` run, as *expected 400, got 422* — at
   which point the ticket that discovers it is not the ticket that can fix it. That is the whole
   reason the constraint is written here, with a criterion a reviewer checks by reading, and the
   reason to leave the password long even though today it could be anything.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**All six `## Proof` mutations matched exactly** — the second ticket in this story for which that held,
against a run base of twelve wrong or incomplete out of thirty-five examined.

**The ambiguous mutation was disambiguated, not tuned.** Proof step 5 says *"`consume` returns `true`
without writing"*, which admits two readings. The coder's first attempt — ignore the token entirely —
reddened three tests; it narrowed to *spend the token but skip the credential rewrite* and got one. The
reviewer ran **both** readings and observed that the Proof's own text, *"`aGoodTokenAnswersTwoHundredAndFour`
reddens alone"*, is satisfied only by the narrow one. So the ticket's wording decided it. That
distinction matters because narrowing a mutation until it matches a prediction is exactly what tuning
looks like from outside, and this was the opposite.

**The URL refusal is asserted, and structurally impossible besides.** `theTokenIsNotAcceptedAsAQueryParameter`
puts a **real, issued** token in the query string and a different, unknown one in the body — so a route
that preferred the query would answer `204` on a valid token. Adding that fallback reddens it alone,
`400 → 204`. And the route is registered at a literal path with no `{token}` segment, with no
`call.parameters` or `queryParameters` anywhere in the handler, so a path read has nothing to bind.
Both halves checked.

**The ungated fixture constraint was left ungated, deliberately.** Shortening a `newPassword` to seven
code points reddens nothing, exactly as the Proof predicts: `ADR-0080` puts the policy check in front of
the token lookup only when `TASK-041629` lands, and this file must pass **unchanged** that day. The
reviewer scanned every string literal in the test — the only ones under eight characters are three
empty-string body assertions.

**The enumeration gate was handled in the same commit, not discovered by CI.** `TASK-041618` failed
`check` on `everyApiPathInTheRouteSourcesIsExercisedByTheScenario` after adding the story's first route;
this ticket adds the second, and the amendment — a `SCENARIO_ENDPOINTS` entry recording that `consume`
writes `credential` and deletes from `password_reset` and `auth_session` while touching neither
`player.coin_balance` nor `duel_result`, plus a fourth Files row and an `atomic:` item — went in
up front. **Every remaining route ticket in this story has the same shape**: `files_touched: 3` and a
verify block without `check`, so each will trip the same gate.
