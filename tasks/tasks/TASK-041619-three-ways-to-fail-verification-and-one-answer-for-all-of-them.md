---
schema: 2
id: TASK-041619
title: Three ways to fail verification, and one answer for all of them
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, auth, security, test]
depends_on: [TASK-041618]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.VerifyEmailRefusalsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The three ways `verify-email` can refuse — unknown, expired, already consumed — are proven
indistinguishable at the wire, and the one case that is *not* a refusal, `409`, is proven to be
reachable only by somebody who already holds the mailbox.

## Why this exists

`ADR-0031` §5 says the three `400` cases are indistinguishable, and §5 separately argues that the
`409` *"is not an oracle, and the distinction is exact"*: its caller has already proven possession
of the mailbox by holding a token that was mailed to it. Both are claims about what a **stranger**
can learn, and neither is checked by a test that merely asserts a status code. Byte-identical
bodies, asserted against each other, are what makes them true.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/VerifyEmailRefusalsTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/http/VerifyEmailRouteTest.kt` — the
`testApplication` fixture, the injected `Clock` and the `recoveryRoutes(...)` install this class
reuses;
`poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §5.

## Scope

- One new test class. **No production change**: every behaviour here already ships with
  `TASK-041618`'s route and `TASK-041609`'s port. This ticket turns three sentences in an ADR into
  assertions, which is the only thing that makes them survive a refactor.
- The refusal comparison collects, for each of the three cases, a triple of `(status, bodyText,
  headerNames)` and asserts all three triples are **equal to each other** — not that each is `400`
  with an empty body, which three separate assertions would satisfy while a `Content-Length` or a
  `WWW-Authenticate` differed.
- Header names are compared as a **set of lowercased names**, excluding `Date`, which varies per
  response and is not a channel a caller can read anything from.

## Out of scope

- Timing. `ADR-0031` §5 closes the timing channel at `forgot-password`, by writing the response
  before any mail work; it makes no timing claim about `verify-email`, whose three refusal paths do
  different amounts of database work by construction. Asserting a timing property here would be
  inventing a requirement, and a flaky one.
- Any change to `RecoveryRoutes.kt`. If a triple turns out to differ, that is a ticket against the
  route, not an assertion softened here.
- `forgot-password`'s four-case `202` — `TASK-041626`.

## Tests

`VerifyEmailRefusalsTest`

| Test | Proves |
| --- | --- |
| `anUnknownAnExpiredAndASpentTokenAreOneAnswer` | Three requests — a token never issued; a token whose 24 hours have passed on the injected clock; a token already used once — produce **byte-identical** `(status, body, header names)` triples, and the status is `400` |
| `theExpiredCaseIsReallyExpired` | Before the clock is advanced, the same token answers `204`. The positive control: without it, an expiry fixture that mints a malformed token passes the test above by being *unknown* rather than expired, and the expiry path is never exercised at all |
| `theSpentCaseIsReallySpent` | The first use of that token answered `204` and `hasRecoveryEmail` is `true`. The same control for the consumed case |
| `theSecondPlayerToProveOneAddressIsToldItIsTaken` | Two players claim one address; the first verifies, the second's verification answers `409`. The second player's `409` is reached only with a token that was minted for *their* claim, which is the whole basis of §5's not-an-oracle argument |
| `aStrangerWithNoTokenCannotReachTheNineOhFour` | For an address already verified to somebody, a **fabricated** token answers `400`, not `409`. The `409` is unreachable without a minted token, so it discloses nothing to a caller who does not already hold one |

## Acceptance criteria

- [ ] All five `VerifyEmailRefusalsTest` tests pass
- [ ] `anUnknownAnExpiredAndASpentTokenAreOneAnswer` asserts the three triples **equal to each
      other**, and the file contains no assertion of the form *each is 400 with an empty body*
      standing in for that comparison
- [ ] The header-name comparison is over a set of lowercased names with `date` excluded
- [ ] `theExpiredCaseIsReallyExpired` and `theSpentCaseIsReallySpent` each assert a `204` **before**
      the refusal they control for
- [ ] The expiry fixture advances an injected `Clock`; the file contains no `Thread.sleep`
- [ ] `aStrangerWithNoTokenCannotReachTheNineOhFour` asserts `400`, and asserts it is **not** `409`
- [ ] No file under `src/main` changes
- [ ] Every command in `verify:` exits 0

## Proof

Each mutation is applied to `RecoveryRoutes.kt` or `PostgresRecoveryEmails.kt`, run, then reverted.

1. In the route, answer the expired case with a distinct body, e.g.
   `call.respond(HttpStatusCode.BadRequest, "expired")` — reachable by having the port return a
   fourth result for expiry. Simpler equivalent: make `Refused` respond with
   `HttpStatusCode.BadRequest` and a body of `recoveryEmails.hashCode().toString()`, which differs
   per instance across the three calls only if instances differ; **it does not**, so use the first
   form. **`anUnknownAnExpiredAndASpentTokenAreOneAnswer` reddens alone**, on the body comparison.
   Revert.
2. Drop `AND expires_at > now()` from the port's consuming `DELETE`.
   **`anUnknownAnExpiredAndASpentTokenAreOneAnswer` reddens** — the expired case now answers `204` —
   **and `theExpiredCaseIsReallyExpired` stays green**, because it asserts the `204` *before*
   expiry. That asymmetry is the point of the control: it proves the fixture reaches the expiry
   branch rather than merely producing a token the server does not recognise. Revert.
3. Make `claimPending` store a digest of a different string, so every minted token is effectively
   unknown. **`theExpiredCaseIsReallyExpired`, `theSpentCaseIsReallySpent` and
   `theSecondPlayerToProveOneAddressIsToldItIsTaken` all redden**, while
   `anUnknownAnExpiredAndASpentTokenAreOneAnswer` **stays green** — all three cases are now
   *unknown*, and their triples still match. This is the exact vacuity this ticket's controls exist
   to catch, and it is the single most important mutation in this file. Run it.
4. Map `AddressTaken` to `400`. **`theSecondPlayerToProveOneAddressIsToldItIsTaken` reddens alone**;
   `aStrangerWithNoTokenCannotReachTheNineOhFour` expects `400` and passes. Revert.
5. Change the route to answer `409` whenever `verifiedOwnerOf(address)` is non-null, before
   checking the token — the oracle §5 forbids. This needs the address in the request, so instead
   make the port return `AddressTaken` for an unknown token when any verified row exists.
   **`aStrangerWithNoTokenCannotReachTheNineOhFour` reddens alone**, *expected 400, got 409*, and
   `anUnknownAnExpiredAndASpentTokenAreOneAnswer` reddens too if its fixture holds a verified row —
   it must not, so keep that fixture free of verified addresses and confirm it stays green. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
