---
schema: 2
id: TASK-041628
title: Two budgets that say nothing when they refuse
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, http, auth, security, config, blocked]
depends_on: [TASK-041634]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.RecoveryBudgetsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Unblocked

**`DEC-073` — the architect's — is answered and merged**, so this section is history rather than a
gate. The `blocked` label in the front matter is a historical marker and this ticket's `status:` is
not `blocked`.

[`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
fixes all four numbers, both placements and the counting rule:

- **`forgot-password`: `10` attempts / `60000` ms. `recovery-email`: `5` / `60000` ms.** The
  register's premise cut the other way in the end — invisible collateral is a reason to be
  *generous*, because a limiter nobody can perceive is one nobody can work around. `forgot-password`
  is generous because §5's fifteen-minute per-account rule already caps bombing one victim at four
  mails an hour across *every* source address; `recovery-email` is five because it is the only cap
  on mail to a caller-chosen recipient **and** a second door to the current-password guess
  `ADR-0074` priced at ten.
- **An over-budget attempt still counts** — one rule for every limiter in this system, and `202`
  gives a sprayer no reason to pace, so counting caps a hammerer at one window's worth in total.
- **Placement is per endpoint**: `recovery-email` admits **after** the `401`, the decode and
  `ADR-0078`'s syntax `400`, and **before** the Argon2 verify; `forgot-password` admits **after the
  `202` is written**, the only budget in this system consulted after the response, because `admit`
  takes a `Mutex` and `TASK-041626` makes that ordering the timing defence.
- **The key is `ADR-0031` §5's and none of it is the architect's**: `origin.remoteAddress` alone,
  no `X-Forwarded-*` until `EPIC-07` installs the plugin.

## Goal

Both `POST` endpoints are budgeted by remote address, over budget answers exactly as success does,
and the numbers are configuration.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/RecoveryBudgetsTest.kt` | create |

`ServerComponents.kt` and `Application.kt` are **not** listed: `TASK-041618` declares
`recoveryRoutes` with its full parameter list and `TASK-041622` installs it, so if the two
`AttemptBudget` instances were not threaded through then, that is a defect ticket against those and
not a widening here.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/AttemptBudget.kt` — including its warning that
an over-budget attempt is recorded whether or not it is admitted, which `ADR-0079` **keeps**: this
ticket uses the shared type unchanged and forks nothing;
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` — where sign-up's and
sign-in's budgets sit in the order and why;
`docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md`;
`docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md` — the
four numbers, the four key names and the two placements.

## Scope

- `ServerConfig` gains two pairs of numbers with keys, env names and defaults, in the shape the four
  existing auth values use — `auth.forgotPasswordMaxAttempts` / `AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS`
  defaulting to `10`, `auth.forgotPasswordWindowMillis` / `AUTH_FORGOT_PASSWORD_WINDOW_MILLIS` to
  `60000`, `auth.recoveryEmailMaxAttempts` / `AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS` to `5`, and
  `auth.recoveryEmailWindowMillis` / `AUTH_RECOVERY_EMAIL_WINDOW_MILLIS` to `60000`, with
  `forgotPasswordLimits()` and `recoveryEmailLimits()` beside the two existing pairs (`ADR-0079`).
- **Two separate `AttemptBudget` instances**, one per endpoint, over their own limits. `ADR-0074`
  §1's reason applies verbatim: one instance shared between two endpoints lets one spend the
  other's budget.
- The key is `io.ktor.server.plugins.origin`'s remote address alone. **Until `EPIC-07` installs the
  forwarded-header plugin, this must never honour a client-supplied forwarding header**, which
  would let a client pick its own budget key.
- Over budget answers **`202`** for both endpoints — identical status, identical body, identical
  headers — so the limiter is not itself an oracle. Never `429`: `ADR-0055`'s `429` was chosen for
  sign-up because that endpoint already leaks a `409`, and neither of these does.
- **Where in each handler the check sits is `ADR-0079`'s and differs between the two**, so it is
  not one rule copied twice: `recovery-email` admits after the `401`, the decode and the syntax
  `400`, and before the Argon2 verify — budgeting before identity would let unauthenticated traffic
  spend a signed-in player's budget (`ADR-0074` §2's reason). `forgot-password` admits **after** its
  `202` has been written, which is the only budget in this system consulted after the response and
  is required because `admit` takes a `Mutex` and `TASK-041626`'s ordering is the timing defence.
- **`AttemptBudget` is used as it stands.** No variant, no flag, no second recording rule and no
  fork of the shared type: `ADR-0079` keeps *an over-budget attempt still counts* for every limiter
  here.

## Out of scope

- Changing sign-up's or sign-in's budgets, their numbers or their placement.
- The fifteen-minute per-account suppression. It is `ADR-0031` §5's, already built by
  `TASK-041613`, and it is a different mechanism against a different attack.
- Budgeting `verify-email`, `reset-password` or `DELETE /api/auth/recovery-email`. §5 budgets the
  two `POST`s and no more, and each of the other three requires a token or a session.
- A hard cap on the budget map's size. `ADR-0055` recorded that as an accepted cost and nothing
  here changes it.

## Tests

`RecoveryBudgetsTest`, with a `ServerClock` the test controls and limits set low.

| Test | Proves |
| --- | --- |
| `anOverBudgetForgotPasswordAnswersLikeASuccess` | Past the limit from one address, the `(status, body, header names)` triple equals a within-budget success's triple, and it is `202` |
| `anOverBudgetForgotPasswordMintsNothing` | Across those over-budget requests for a **verified** address, no new `password_reset` row appears and nothing is sent. The positive control the triple comparison cannot give: without it, a budget that admitted everything passes the test above |
| `anOverBudgetAttachAnswersLikeASuccess` | The same for `POST /api/auth/recovery-email`, and no `email_verification` row is written |
| `oneAddressBudgetIsNotAnothers` | Two remote addresses; exhausting the first leaves the second admitted. Guards a limiter keyed on a constant |
| `theTwoEndpointsDoNotShareABudget` | Exhausting `forgot-password` from one address leaves `recovery-email` admitted from the same address, and the reverse. `ADR-0074` §1's separation, asserted |
| `theBudgetWindowRollsOnTheInjectedClock` | Advancing the `ServerClock` past the window re-admits. No test sleeps |
| `aForwardedHeaderDoesNotChooseTheKey` | Exhausting from one address, then repeating with `X-Forwarded-For: 10.0.0.1`, is still over budget — asserted through `anOverBudgetForgotPasswordMintsNothing`'s mechanism, since the status cannot show it |

## Acceptance criteria

- [ ] All seven `RecoveryBudgetsTest` tests pass
- [ ] The four defaults are `10` / `60000` and `5` / `60000` under `ADR-0079`'s four key names
- [ ] `RecoveryRoutes.kt` declares **two** `AttemptBudget` instances and no subclass, wrapper or
      copy of that type, and `AttemptBudget.kt` is byte-unchanged
- [ ] Every over-budget assertion compares a triple to a **success** triple, not merely to `202`
- [ ] `anOverBudgetForgotPasswordMintsNothing` and `anOverBudgetAttachAnswersLikeASuccess` assert on
      **database state**, because the wire cannot distinguish the cases
- [ ] `oneAddressBudgetIsNotAnothers` uses **two** remote addresses
- [ ] `theTwoEndpointsDoNotShareABudget` asserts **both** directions
- [ ] `RecoveryRoutes.kt` reads no `X-Forwarded-For` and no `X-Forwarded-Host`
- [ ] Neither endpoint can answer `429`; the string `TooManyRequests` does not appear in
      `RecoveryRoutes.kt`
- [ ] `ServerConfigTest` covers both new pairs' env-then-file-then-default precedence
- [ ] Every command in `verify:` exits 0

## Proof

1. Answer `429` when over budget.
   **`anOverBudgetForgotPasswordAnswersLikeASuccess` and `anOverBudgetAttachAnswersLikeASuccess`
   both redden**, on their triples. This is `ADR-0055`'s shape applied by analogy where §5 forbids
   it, and it turns the limiter into the oracle the whole endpoint is built to avoid. Revert.
2. Use one shared `AttemptBudget` for both endpoints.
   **`theTwoEndpointsDoNotShareABudget` reddens alone.** Every other test uses one endpoint at a
   time and passes. Revert.
3. Key the budget on a constant string.
   **`oneAddressBudgetIsNotAnothers` reddens alone.** Revert.
4. Key it on `call.request.headers["X-Forwarded-For"] ?: origin.remoteAddress`.
   **`aForwardedHeaderDoesNotChooseTheKey` reddens alone**, on its database assertion — the status
   is `202` either way, which is exactly why that test cannot assert on the wire and why the
   criterion says so. Revert.
5. Check the budget but ignore its answer — always proceed.
   **`anOverBudgetForgotPasswordMintsNothing` reddens alone**, on the row count.
   `anOverBudgetForgotPasswordAnswersLikeASuccess` compares two `202` triples and **passes**, since
   both requests now succeed identically. Run this: it is the mutation that proves the triple
   comparison alone gates nothing, and it is the reason every over-budget test here has a database
   assertion beside it.
6. Make the window `Long.MAX_VALUE`.
   **`theBudgetWindowRollsOnTheInjectedClock` reddens alone.** Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The budget numbers themselves are gated by nothing, and this was proved rather than argued.** The
reviewer swapped the two defaults in `ServerConfig.kt` — forgot-password to five, recovery-email to
ten — and ran both `verify:` classes: **build succeeded, zero failures.** Every test here builds its
own explicit low `AttemptLimits` because the Tests intro mandates *limits set low*, which is right for
exercising the mechanism and blind to the policy. So *that a budget applies* is gated; *which budget
applies where* is not.

**That gap is a ticket defect, and it currently has no owner.** The acceptance criterion
*"`ServerConfigTest` covers both new pairs' env-then-file-then-default precedence"* names a file the
*Files* table excludes, with no carve-out — contrast this ticket's own lines 60–63, which explicitly
forward `ServerComponents.kt`/`Application.kt` risk to `TASK-041618`/`TASK-041622`. No such reference
exists for `ServerConfigTest.kt`. The coder refused to widen, correctly: nothing merged was failing,
and `ADR-0070` §4's propagation exception explicitly excludes *adds a test*. **The closing fixture is
known** — `assertEquals(AttemptLimits(10, 60_000L), ServerConfig().forgotPasswordLimits())` and its
recovery-email twin — and needs a fourth Files row or its own ticket. Seventh Scope/Files disagreement
this run.

**Proof step 6 gates nothing in production, for a reason worth recording.** There is no window literal
in `RecoveryRoutes.kt` at all — both windows come from `ServerConfig` constants and every test builds
its own `AttemptLimits`. The only executable form of that step mutates a literal inside
`theBudgetWindowRollsOnTheInjectedClock` itself, so *"reddens alone"* is tautological. It is the same
gap as the missing `ServerConfigTest` coverage seen from the other side.

**Three steps reddened more tests than predicted, and each extra was traced.** Step 1's third failure
is `aForwardedHeaderDoesNotChooseTheKey`'s own setup assertion re-checking the `202`; step 2's second
is `anOverBudgetAttachAnswersLikeASuccess`, which leaves `forgotPasswordBudget` at its default of ten
so a borrowed budget stops being over-budget; step 5's extras both rely on a second verified address
minting nothing. Review confirmed none indicates a coupling worth breaking — intra-test overlap and
correct use of defaults, not fragility.

**The refusal is byte-identical by construction, not by coincidence.** Forgot-password's over-budget
path executes the **same** `call.respond(HttpStatusCode.Accepted)` statement the success path reaches,
and recovery-email's uses the textually identical zero-arg call. Both are asserted on the
`(status, body, headerNames)` triple. And `aForwardedHeaderDoesNotChooseTheKey` proves the budget key
cannot be chosen by a caller: `origin.remoteAddress` is read alone, with zero occurrences of
`X-Forwarded` anywhere in the production file.
