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

## Blocked

**`DEC-074` — the architect's.** *Does a good reset token survive a `422`, and if so by what
mechanism?*

`ADR-0031` §5 gives this endpoint a **`422` when the token was good and the new password fails
policy**, which presupposes the endpoint knows the token was good *before* judging the password,
and implies the caller may retry. §4 makes consumption **one** `DELETE … RETURNING` inside the
transaction that writes the password, with *"no read-then-write window"* — under which knowing the
token was good **is** spending it, so a `422` burns the link and the retry §5 implies is impossible.

The three reconciliations each cost something the ADR refuses elsewhere: a non-consuming pre-check
reopens the window §4 closed; consume-then-roll-back keeps the window shut but makes a refusal a
transaction no middleware may retry; judging the policy first answers `422` before the token is
known, which discloses that the token *may* be fine — harmless against somebody already holding 256
bits, but a disclosure that should be chosen rather than fallen into.

This is a **conflict between two sections of a merged ADR**, not an acknowledged gap: `ADR-0031`'s
*What this does not settle* does not mention it.

## Goal

`POST /api/auth/reset-password` answers `422` for a good token and a password that fails policy,
by whatever mechanism `DEC-074` names, with the fate of the token asserted either way.

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
the ADR answering `DEC-074`.

## Scope

- The reset handler gains the policy check, in the position `DEC-074`'s answer fixes.
- `ADR-0031` §4: *"The new password goes through the same policy and the same Argon2id parameters
  as sign-up. This ADR adds no second password rule and no second hashing path."* So both
  `PasswordPolicy` functions are called, and **no new predicate is written**.
- Whatever `DEC-074` decides about the token's fate is asserted directly, in
  `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` below. That test exists **now**, named, so the
  answer cannot land without one — an unasserted answer is the thing `STORY-0417`'s form will get
  wrong.
- `ResetPasswordRouteTest` is in `verify:` and must pass **unchanged**: this ticket adds a status
  that file never produced, and moves none of its four assertions. If one has to move, say which
  and why in the PR.

## Out of scope

- Adding a composition rule, a strength meter or a breach check. `ADR-0048` is one rule and it is
  length; `ADR-0031` §4 says this endpoint adds none.
- Changing sign-up's `422`. `ADR-0048` already fixed it and `TASK-0404xx` shipped it.
- Trimming or normalising the new password beyond the NFC that `nfcNormalisedSecret` already
  applies in the one place a secret becomes bytes.
- Telling the caller **which** bound failed. `ADR-0048` ships one rule and one status; two messages
  would be a second rule in disguise.

## Tests

`ResetPasswordPolicyTest`

| Test | Proves |
| --- | --- |
| `aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo` | A good token, a 7-code-point password: `422`, and the old password still verifies |
| `anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo` | The same at the other bound. Two inputs, both bounds — one alone leaves half the rule unmeasured |
| `anEightCodePointPasswordIsAccepted` | Exactly 8: `204`. The boundary from inside, and the positive control without which a route refusing every password passes both tests above |
| `fourAstralCharactersAreFourCodePointsAndAreRefused` | A password of four astral characters — 8 UTF-16 units, 4 code points — answers `422`. `ADR-0048` counts code points, and `String.length` would accept this |
| `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` | After a `422`, a **second** request with the same token and an acceptable password either succeeds or answers `400` — whichever `DEC-074` decided, asserted explicitly and with the ADR cited in the test's own comment |
| `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` | A fabricated token with a 7-code-point password: `400`, **not** `422`. The two refusals must not collapse, or a stranger learns the password rule from an endpoint they hold no token for |

## Acceptance criteria

- [ ] `DEC-074` is answered by a merged ADR before this leaves `blocked`
- [ ] All six `ResetPasswordPolicyTest` tests pass
- [ ] `ResetPasswordRouteTest` passes **unchanged**, and no assertion in it is weakened
- [ ] The two `422` tests use passwords at **both** bounds, 7 and 129 code points
- [ ] `anEightCodePointPasswordIsAccepted` exists and asserts `204`
- [ ] `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` cites the answering ADR by number in a
      comment and asserts one outcome, not both
- [ ] `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` asserts the status is `400`
      **and** is not `422`
- [ ] `RecoveryRoutes.kt` calls `passwordIsLongEnough` and `passwordIsWithinTheWorkBound` and
      declares no new predicate
- [ ] Every command in `verify:` exits 0

## Proof

1. Drop `passwordIsWithinTheWorkBound`.
   **`anOneHundredAndTwentyNineCodePointPasswordAnswersFourHundredAndTwentyTwo` reddens alone**,
   *expected 422, got 204* — and the 129-code-point password is hashed, which is the work `ADR-0048`
   §2 bounds. The 7-code-point test passes, which is why both bounds are required. Revert.
2. Drop `passwordIsLongEnough`.
   **`aSevenCodePointPasswordAnswersFourHundredAndTwentyTwo` reddens alone.** Revert.
3. Change the length count from `codePointCount` to `String.length` — in the test's expectation
   rather than in `PasswordPolicy`, which is merged and out of scope: assert the astral password is
   *accepted*. **`fourAstralCharactersAreFourCodePointsAndAreRefused` reddens** as written, which
   is the point. Restore the assertion, then verify the production path by confirming the test
   passes against the merged `PasswordPolicy`. Record that this ticket cannot mutate
   `PasswordPolicy` itself without leaving its file budget, so the astral test is a **contract**
   assertion on merged behaviour rather than a gate on new code — and say so in the PR.
4. Change the policy refusal from `422` to `400`.
   **All three `422` tests redden**, and `aBadTokenStillAnswersFourHundredNotFourHundredAndTwenty
   Two` **passes** — the two refusals have collapsed and only the tests expecting `422` notice.
   Revert.
5. Answer `422` for a bad token with a bad password.
   **`aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo` reddens alone.** This is the
   oracle: a stranger with no token would learn the password rule. Revert.
6. Change the boundary to `>= 9`.
   **`anEightCodePointPasswordIsAccepted` reddens alone.** The off-by-one that no `422` test can
   see, and the reason the inside-boundary control exists. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
