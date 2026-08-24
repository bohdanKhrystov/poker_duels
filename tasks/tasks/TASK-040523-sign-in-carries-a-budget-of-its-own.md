---
schema: 2
id: TASK-040523
title: Sign-in carries a budget of its own
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - the Kotlin compiler — `authRoutes` gains a required second budget parameter, so `Application.kt` and both test files that install it stop compiling in the same commit
  - the Kotlin compiler again — `Application.kt` takes that argument from `serverComponents`, so `ServerComponents.kt` must build the second `AttemptBudget` in the same commit
labels: [server, http, auth, rate-limit]
depends_on: [TASK-040522]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.SignUpSecrecyTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`POST /api/auth/sign-in` admits at most **ten attempts per remote address per rolling sixty
seconds** to the point where Argon2 runs, refunds the slot when the password turns out to be right,
and answers an over-budget request **exactly** as a wrong password does.

Read `ADR-0074` §§1–4 and `ADR-0027` §6. Nothing else.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` | modify | the reserve, the refund, and the new parameter |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify | `compileKotlin` — the second budget has to be built where the composition root can reach it |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify | `compileKotlin` — *No value passed for parameter 'signInBudget'* |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify | `compileTestKotlin` — every `authRoutes(...)` call site, plus this ticket's own tests |
| `poker-server/src/test/kotlin/duels/poker/server/http/SignUpSecrecyTest.kt` | modify | `compileTestKotlin` — the same call, and it must keep passing unchanged otherwise |

The two config values and `signInLimits()` are **`TASK-040520`'s**, and `AttemptBudget.refund` is
**`TASK-040519`'s**; both are merged before this starts.

## Scope

- `authRoutes(reads, credentials, identities, sessions, budget, signInBudget: AttemptBudget)`, and
  `serverComponents` builds `AttemptBudget(config.signInLimits(), clock)` — **a second instance**,
  over the same type and its own limits. One instance shared between the two endpoints would let
  sign-ups spend sign-in's budget and the reverse (`ADR-0074` §1).
- **`ADR-0074` §2's order, and the order is the security property:**
  1. decode and `ADR-0048` §2's 128-code-point maximum — no budget, no hash;
  2. `signInBudget.admit(call.request.origin.remoteAddress)`; over budget answers here, **before**
     the identifier is looked up and **before** anything is hashed;
  3. the credential lookup and the verification, including `ADR-0027` §6's dummy hash;
  4. **on success only**, `signInBudget.refund(...)`, then issue the session.
- The over-budget answer is `401` with an empty body — **byte-identical to the wrong-password
  answer, headers included**. No `429`, no `Retry-After`, no new field (`ADR-0074` §4).
- The key is `call.request.origin.remoteAddress` and **nothing else**. Until `EPIC-07` installs the
  forwarded-header plugin this must not read `X-Forwarded-For`; a client-supplied key is a limiter
  that looks green in every test and stops nothing. One KDoc line, naming `EPIC-07`, as
  `TASK-040521` already carries for sign-up.
- **The refund is load-bearing, not tidiness.** A handler that reserves and forgets to refund turns
  a failure budget into a traffic meter, silently — no compiler catches it and only
  `aSuccessfulSignInSpendsNoBudget` can (`ADR-0074` §Consequences).

## Out of scope

- Anything about sign-up's budget, which is merged and unchanged. `authRoutes` keeps its existing
  `budget` parameter for sign-up exactly as `TASK-040521` left it.
- Any `429` on this endpoint (`ADR-0027` §6, `ADR-0056` §1, `ADR-0074` §4).
- Any client change. `ADR-0056` §1 forbids a throttled state on the sign-in form, so nothing in
  `web-client/` renders this.
- Sweeping expired keys from `ADR-0025`'s ticker, and any per-account or per-identifier budget —
  `ADR-0074` §5 leaves the second one open and it is `ADR-0027` §6's to reopen, not this ticket's.

## Tests

`AuthRouteTest`, on the injected `ServerClock` the suite already advances. **No test sleeps.**

| Test | Proves |
| --- | --- |
| `anOverBudgetSignInIsIndistinguishableFromAWrongPassword` | the two responses' status, body and header name-sets compare equal field by field |
| `aSuccessfulSignInSpendsNoBudget` | eleven correct sign-ins in a row all answer `200` — one more than `signInMaxAttempts`, so a missing `refund` fails on the eleventh |
| `theEleventhWrongPasswordInsideTheWindowIsRefusedWithoutHashing` | ten wrong passwords, then an eleventh with the **right** password, answered `401` — the budget, not the credential, decided it |
| `theWindowRollsForwardAfterSixtySeconds` | advancing the clock past `60000` lets the right password through again |
| `twoAddressesHaveTwoBudgets` | one address exhausted leaves another's first attempt answered on its merits |
| `signUpAndSignInDoNotShareABudget` | exhausting sign-up's budget leaves sign-in answering normally, and the reverse |

## Acceptance criteria

- [ ] Every test method above passes
- [ ] `AuthRoutes.kt` returns no `429` from the sign-in handler
- [ ] The sign-in handler calls `refund` on exactly one path — the successful one
- [ ] `ServerConfig` is read for both numbers; neither `10` nor `60000` appears as a literal in
      `AuthRoutes.kt` or `ServerComponents.kt`
- [ ] Every command in `verify:` exits 0

## Proof

Delete the `refund` call and `aSuccessfulSignInSpendsNoBudget` goes red alone — it is the only test
that can see it, which is why it signs in one more time than the budget allows.

**What no test here can see is whether the hash ran.** Verify first, consult the budget second,
answer third — and every test in the table above still passes while the endpoint pays for exactly
the Argon2 work the budget exists to refuse. Only reading the source catches it, so name in the PR
body which line of `AuthRoutes.kt` the `admit` sits on and which line the lookup sits on. An `admit`
placed after the *verification* rather than after the lookup does fail
`theEleventhWrongPasswordInsideTheWindowIsRefusedWithoutHashing`, which is why that test uses the
right password on the eleventh attempt.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Refunding twice on success is caught by nothing, and that is `TASK-040526`.** The dangerous
direction is gated — an unconditional refund on the *failure* path reddens three tests, so an
attacker cannot hand themselves back every wrong password. The opposite is invisible:
`AttemptBudget.refund` guards underflow, and every sequential request in this suite drains its window
to empty via the first legitimate refund, so a second call finds nothing to remove. Catching it needs
an under-budget success with **earlier attempts still live in the window** — a user who mistypes
twice and then succeeds. The coder wrote the bug, got `BUILD SUCCESSFUL`, and reported it rather than
adding a seventh test outside this ticket's table.

**Metering is gated by `assertEquals(10, credentials.verifyCalls.size)`**, not by a status code.
Moving `admit` after `credentials.verify` reddens only
`theEleventhWrongPasswordInsideTheWindowIsRefusedWithoutHashing`, on that count — the adjacent 401
assertion passes either way, because a 401 is a 401 whether the hash ran or not.

**A pre-existing sign-up test was renamed.** `twoAddressesHaveTwoBudgets` collided with this ticket's
required sign-in test of the same name, so the sign-up one became
`twoAddressesHaveTwoSignUpBudgets`; the reviewer confirmed only the identifier and its comment
changed. `TASK-040521`'s Tests table still names the old identifier, and is now stale in that one
respect.

