---
schema: 2
id: TASK-041628
title: Two budgets that say nothing when they refuse
type: task
status: blocked
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

## Blocked

**`DEC-073` — the architect's.** *What are the two numbers for each of `POST /api/auth/recovery-
email` and `POST /api/auth/forgot-password`, and does an over-budget attempt still count against its
own window?*

`ADR-0031` §5 fixes the mechanism, the key (remote address) and the answer (`202`, identical to
success) and fixes **no numbers**. Neither shipped pair transfers: `ADR-0055`'s five per fifteen
minutes and `ADR-0074`'s ten per sixty seconds were each chosen on an argument that does not apply
here. `ADR-0074`'s turned on shared-address collateral being *visible* — a throttled player is told
their password is wrong and can pace — while here over budget is indistinguishable from success, so
a throttled player is told nothing at all and cannot pace. `ADR-0031` §5 also already carries a
**second, durable** limiter (no mail if a live token was issued in the last fifteen minutes), so the
answer must say what the address budget is still for once that exists.

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
an over-budget attempt is recorded whether or not it is admitted, which `DEC-073` may or may not
keep;
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` — where sign-up's and
sign-in's budgets sit in the order and why;
`docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md`;
the ADR answering `DEC-073`.

## Scope

- `ServerConfig` gains two pairs of numbers with keys, env names and defaults, in the shape the four
  existing auth values use. The env names follow `AUTH_*`.
- **Two separate `AttemptBudget` instances**, one per endpoint, over their own limits. `ADR-0074`
  §1's reason applies verbatim: one instance shared between two endpoints lets one spend the
  other's budget.
- The key is `io.ktor.server.plugins.origin`'s remote address alone. **Until `EPIC-07` installs the
  forwarded-header plugin, this must never honour a client-supplied forwarding header**, which
  would let a client pick its own budget key.
- Over budget answers **`202`** for both endpoints — identical status, identical body, identical
  headers — so the limiter is not itself an oracle. Never `429`: `ADR-0055`'s `429` was chosen for
  sign-up because that endpoint already leaks a `409`, and neither of these does.
- Where in each handler the check sits is `DEC-073`'s, and the answer must say: `ADR-0055` meters
  spending after every other refusal, `ADR-0074` reserves before the hash. These endpoints hash
  nothing on the `forgot-password` path and hash once on `recovery-email`'s.

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

- [ ] `DEC-073` is answered by a merged ADR before this leaves `blocked`
- [ ] All seven `RecoveryBudgetsTest` tests pass
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
