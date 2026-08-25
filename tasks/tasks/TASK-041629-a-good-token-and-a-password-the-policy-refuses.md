---
schema: 2
id: TASK-041629
title: A good token, and a password the policy refuses
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, http, auth, security, blocked]
depends_on: [TASK-041628]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordPolicyTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Unblocked

**`DEC-074` — the architect's — is answered and merged**, so this section is history rather than a
gate. The `blocked` label in the front matter is a historical marker and this ticket's `status:` is
not `blocked`.

[`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md) §1 fixes
the order: **decode, judge the password, then spend the token.** `ADR-0031` §4's single
`DELETE … RETURNING` and its *"no read-then-write window"* stand byte-unchanged, because the step
this ticket adds goes in **front** of `consume`: a `422` takes no connection, executes no statement,
and neither reads nor writes `password_reset` (§4). §5's status table still answers `422`; what was
corrected is its parenthetical, and §2 carries the sentence `TASK-041617` transcribes.

**Two of the tests this ticket was written with moved, and one of those was deleted.**

- `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` resolves to the surviving branch and is renamed
  `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest`, since the decision is now named and a test
  called *as the decision says* asks a reader which decision and what it said. It asserts one
  outcome: **a second request with the same token and an 8-code-point password answers `204`**
  (`ADR-0080` §4). The losing branch is a pure function of the request body, so there is no clock to
  control, no latch, no second connection and nothing to race (§6).
- `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` **cannot be written.** It asserts the
  order this decision reverses: a fabricated token with a 7-code-point password now answers `422`,
  because the token is never looked at. What it defended is worth a test in the other direction, and
  `ADR-0080` §7 names the sharper property — **the `422` for a fabricated token and the `422` for a
  live one are indistinguishable**, which is what keeps the status from reporting on the row. That
  test replaces it below, and Proof steps 1 to 3 are what stop it being a test that two broken
  things are broken alike.

## Goal

`POST /api/auth/reset-password` judges the new password **before** it touches the token: `422` for a
password outside 8 to 128 code points whatever the token is, and a token that a `422` leaves usable.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordPolicyTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/PasswordPolicy.kt` — `passwordIsLongEnough`
and `passwordIsWithinTheWorkBound`, and why they are two functions;
`poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordRouteTest.kt` — the fixture this
class reuses;
`docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md`;
`docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md` §1, §4, §6 and §7.

## Scope

- The handler runs `ADR-0080` §1's three steps and no others, in this order:
  1. **decode the body** — any failure ⇒ `400`, empty body. `TASK-041620` shipped this and it does
     not move;
  2. **`passwordIsLongEnough(newPassword)` and `passwordIsWithinTheWorkBound(newPassword)`** —
     either `false` ⇒ **`422`, empty body, and nothing else runs**: no connection taken, no
     statement executed, `password_reset` neither read nor written;
  3. **`passwordResets.consume(token, newPassword)`** ⇒ `204` on `true`, `400` on `false`.
- `ADR-0031` §4: *"The new password goes through the same policy and the same Argon2id parameters
  as sign-up. This ADR adds no second password rule and no second hashing path."* So both
  `PasswordPolicy` functions are called, and **no new predicate is written**.
- **The two predicates are one conjunction and their relative order is unobservable** — one status,
  an empty body, no message (`ADR-0048` §6, `ADR-0080` §1). No test may pin which of the two fired.
- **Nothing is added to `PasswordResets`.** `consume(token, secret): Boolean` is unchanged and grows
  no third outcome (`ADR-0080` §7); the whole decision is one `if` above one call.
- `ResetPasswordRouteTest` is in `verify:` and must pass **unchanged**: this ticket adds a status
  that file never produced, and moves none of its four assertions. `TASK-041620` was written to make
  that possible — every request in it already carries a `newPassword` of 8 to 128 code points,
  **including the two that expect `400`**. If one does not, that is a defect against `TASK-041620`
  and a ticket of its own, **not an edit here**: say so in the PR and stop.

## Out of scope

- **A liveness pre-check, a consume-then-roll-back, or any read of `password_reset` outside
  `consume`'s own `DELETE`.** `ADR-0080` §Alternatives rejected all three by name; the second and
  third reopen the window `ADR-0031` §4 closed. **Gated below** by
  `aFabricatedTokenAndALiveTokenAnswerTheSameFourHundredAndTwentyTwo`, since a refusal to look at a
  row produces no assertion by itself.
- **Budgeting this endpoint.** `ADR-0031` §5 budgets `recovery-email` and `forgot-password` and no
  more, and `ADR-0080` §5 re-checked it under the new order: the `422` branch does strictly less
  work than the `400` it replaces — one `codePointCount`, no round trip, no Argon2. The one
  condition that reopens it is a policy rule that reads a row or consults a corpus, and this ticket
  adds no rule at all.
- Adding a composition rule, a strength meter or a breach check. `ADR-0048` is one rule and it is
  length; `ADR-0031` §4 says this endpoint adds none.
- Changing sign-up's `422`. `ADR-0048` already fixed it and `TASK-0404xx` shipped it.
- Trimming or normalising the new password beyond the NFC that `nfcNormalisedSecret` already
  applies in the one place a secret becomes bytes.
- Telling the caller **which** bound failed, or whether the token was any good. `ADR-0048` ships one
  rule and one status; two messages would be a second rule in disguise, and a `422` that mentioned
  the token would be the liveness report `ADR-0080` §3 exists to remove.

## Tests

`ResetPasswordPolicyTest`. Every token this fixture mints must have an `expires_at` in the future of
the **database's** clock, not merely of the injected `java.time.Clock` — `ADR-0080` §6 names that as
the one hazard in this file, and it turns every `204` below into a `400` for a reason that has
nothing to do with this ticket.

| Test | Proves |
| --- | --- |
| `aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo` | A live token, a 7-code-point password: `422`, and the old password still verifies |
| `anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo` | The same at the other bound, old password included. Two inputs, both bounds — one alone leaves half the rule unmeasured |
| `anEightCodePointPasswordIsAccepted` | Exactly 8: `204`. The boundary from inside, and the positive control without which a route refusing every password passes both tests above |
| `fourAstralCharactersAreFourCodePointsAndAreRefused` | A password of four astral characters — 8 UTF-16 units, 4 code points — answers `422`. `ADR-0048` counts code points, and `String.length` would accept this |
| `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest` | One token, two requests: a 7-code-point password answers `422`, then the **same** token with an 8-code-point password answers `204`. `ADR-0080` §4 — a refusal leaves `token_hash`, `issued_at` and `expires_at` exactly as they were, so the link survives the most ordinary mistake at a password field |
| `aFabricatedTokenAndALiveTokenAnswerTheSameFourHundredAndTwentyTwo` | Two requests carrying the **same** 7-code-point password: one with a token this fixture minted, one with a fabricated string of the **same length and alphabet**, so the only difference between them is whether the server has a row. In assertion order: the two `(status, body, header names)` triples are **equal**; each status **is `422`**; and the minted token then answers `204` with an 8-code-point password. `ADR-0080` §3 — the branch is chosen entirely by the caller's own password, so `400`-versus-`422` reports nothing about `password_reset` |

The last test's three assertions are three different jobs and Proof steps 1 to 3 break them one at a
time: the equality is the property, the `422` stops two identical wrong answers satisfying it, and
the closing `204` stops the comparison being between two tokens that are both dead.

## Acceptance criteria

- [ ] All six `ResetPasswordPolicyTest` tests pass
- [ ] `ResetPasswordRouteTest` passes **unchanged**, and no assertion in it is weakened
- [ ] The two bound tests use passwords at **both** bounds, 7 and 129 code points, and each asserts
      the old password still verifies
- [ ] `anEightCodePointPasswordIsAccepted` exists and asserts `204`
- [ ] `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest` sends the **same** token twice, asserts
      `422` then `204`, and cites `ADR-0080` §4 in a comment
- [ ] `aFabricatedTokenAndALiveTokenAnswerTheSameFourHundredAndTwentyTwo` asserts, **in this order**,
      that the two triples are equal to each other, that each status **is** `422`, and that the
      minted token afterwards answers `204`
- [ ] That test's fabricated token has the **same length and alphabet** as a minted one, and the
      file contains no assertion that the two tokens differ in shape
- [ ] Every token the fixture mints expires in the future of the **database's** clock
- [ ] `RecoveryRoutes.kt` calls `passwordIsLongEnough` and `passwordIsWithinTheWorkBound` and
      declares no new predicate
- [ ] The policy check sits **above** the `consume` call in the handler, and the handler's reset
      branch references `passwordResets` exactly once
- [ ] Every command in `verify:` exits 0

## Proof

1. **Move the policy check below `consume`**: consume first, `400` on `false`, and judge the
   password only when it returned `true` — `ADR-0080`'s rejected order, and the asymmetry oracle for
   the new test. The fabricated leg finds no row and answers `400`; the live leg spends the token,
   writes the 7-code-point password and then answers `422`. **`aFabricatedTokenAndALiveTokenAnswer
   TheSameFourHundredAndTwentyTwo` reddens on its equality assertion first**, *400 ≠ 422* — exactly
   one of the two legs moved, which is the whole point of the test. Three more redden, on side
   effects rather than on the pair: the two bound tests on *the old password still verifies*,
   because the mutation writes the refused password, and
   `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest` on its `204`, because the token is spent.
   **The equality is the only assertion in the file that sees the two statuses side by side.**
   Revert.
2. **Break both legs the same way**: answer `400` for every policy failure. Both legs now answer
   `400`, both triples are still equal, and **the equality assertion passes** — the new test reddens
   only on the assertion after it, *each status is `422`*. Four others redden with it:
   `aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo`,
   `anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo`,
   `fourAstralCharactersAreFourCodePointsAndAreRefused`, and
   `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest` on its **first** request. Run this one. It
   is the entire hazard of a property phrased as *these two are the same*: an equality is satisfied
   by two identical wrong answers, so without the absolute status assertion beside it this test
   would have reported nothing here. Revert.
3. **Mint every fixture token already expired** — move the fixture's `issue` helper to an instant
   more than an hour behind the database's `now()`, which is `ADR-0080` §6's named hazard arriving
   as a mutation. Both legs of the new test are now effectively fabricated, both still answer `422`
   because the policy runs before the token is touched, and **the equality assertion passes again**
   — the new test reddens only on its **closing `204`**. Two others redden on the same thing:
   `anEightCodePointPasswordIsAccepted`, and
   `aRefusedPasswordLeavesTheTokenUsableOnTheNextRequest` on its **second** request. This is why the
   new test ends with a `204` on the minted token: without it the comparison is between two dead
   tokens, it says nothing about a live one, and it would go on passing while the fixture rotted.
   Revert.
4. Drop `passwordIsWithinTheWorkBound`.
   **`anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo` reddens alone**,
   *expected 422, got 204* — and the 129-code-point password is hashed, which is the work `ADR-0048`
   §2 bounds. The 7-code-point test passes, which is why both bounds are required. Revert.
5. Drop `passwordIsLongEnough`.
   **`aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo` reddens alone.** Revert.
6. Change the length count from `codePointCount` to `String.length` — in the test's expectation
   rather than in `PasswordPolicy`, which is merged and out of scope: assert the astral password is
   *accepted*. **`fourAstralCharactersAreFourCodePointsAndAreRefused` reddens** as written, which
   is the point. Restore the assertion, then verify the production path by confirming the test
   passes against the merged `PasswordPolicy`. Record that this ticket cannot mutate
   `PasswordPolicy` itself without leaving its file budget, so the astral test is a **contract**
   assertion on merged behaviour rather than a gate on new code — and say so in the PR.
7. Change the boundary to `>= 9`.
   **`anEightCodePointPasswordIsAccepted` reddens alone.** The off-by-one that no `422` test can
   see, and the reason the inside-boundary control exists. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
