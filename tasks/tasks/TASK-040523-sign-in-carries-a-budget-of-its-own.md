---
schema: 2
id: TASK-040523
title: Sign-in carries a budget of its own
type: task
status: blocked
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, auth, rate-limit, blocked]
depends_on: [TASK-040522]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0027` §6's sign-in budget exists on the same `AttemptBudget` sign-up uses: failed sign-ins are
metered by remote address, and an over-budget request answers **exactly** as a wrong password does.

## Blocked

**`DEC-069` — the architect's.** `ADR-0027` §6 fixes the mechanism (a rolling window, keyed by
remote address, state in memory, time from `ServerClock`), fixes the predicate (*failed* sign-ins,
not spent ones — the opposite of `ADR-0055` §1's departure) and fixes the answer (identical to a
wrong secret, so the limiter is not itself an oracle). **It fixes no numbers**, and `ADR-0055` §2's
two config values are sign-up's alone and say so.

The numbers are not a tuning detail here. `ADR-0048` accepts `password` and `12345678`, and records
that what stands between a guesser and one of those accounts is *"Argon2id itself"* plus this
budget. And sign-in, unlike sign-up, is something a player does repeatedly from a shared address:
`ADR-0055` §4 accepted a fifteen-minute shared-address lockout for a once-in-a-lifetime action, and
that acceptance does not transfer to the endpoint a café full of players uses every day. Settle the
two numbers, and settle whether an over-budget sign-in still counts against its own window the way
`ADR-0022` §2 and `ADR-0055` §1 both have it.

Do not start this ticket before `DEC-069` is answered by a **merged** ADR.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify |

`ServerConfigTest` will need the two defaults asserted; if that pushes the count past three, this is
two tickets — the config pair first, the call site second, exactly as `TASK-040520` and
`TASK-040521` were split.

## Scope

- Two config values in `ADR-0055` §2's pattern, with the defaults the answering ADR gives, and a
  `signInLimits(): AttemptLimits`.
- A **second** `AttemptBudget` instance in `serverComponents`, over the same type and its own
  limits. One instance shared between the two endpoints would let sign-ups spend sign-in's budget,
  which is the coupling `ADR-0055` §5 was careful to avoid.
- The check's position follows the answering ADR: `ADR-0027` §6 meters *failures*, so a successful
  sign-in must not spend budget, and an over-budget request answers `401` with an empty body —
  byte-identical to the wrong-password answer, headers included.

## Out of scope

- Anything about sign-up's budget, which is merged and unchanged.
- Any `429` on this endpoint. `ADR-0027` §6 and `ADR-0056` §1 both say the over-budget answer here
  is the ordinary refusal, and a distinguishable status would make the limiter an oracle.

## Tests

To be written against the answering ADR's numbers. The shape is fixed even though the numbers are
not:

| Test | Proves |
| --- | --- |
| `anOverBudgetSignInIsIndistinguishableFromAWrongPassword` | the two responses' status, body and header name-sets compare equal field by field |
| `aSuccessfulSignInSpendsNoBudget` | *N* successes in a row all answer `200` — the predicate is failure, not traffic |
| `twoAddressesHaveTwoBudgets` | one address exhausted leaves another's first attempt answered on its merits |
| `signUpAndSignInDoNotShareABudget` | exhausting sign-up's budget leaves sign-in answering normally, and the reverse |

## Acceptance criteria

- [ ] `DEC-069` is answered by a merged ADR, and this ticket's numbers are that ADR's
- [ ] Every test method above passes
- [ ] `AuthRoutes.kt` returns no `429` from the sign-in handler
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
