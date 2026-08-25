---
schema: 2
id: TASK-041607
title: The port where a pending address and a proven one both live
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, auth, api]
depends_on: [TASK-041606]
verify:
  - ./gradlew :poker-server:compileKotlin
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`RecoveryEmails` declares every operation the two recovery tables need, with a result type per
outcome, so that no endpoint written later has to invent one — including the one that says whether
an attach caused a mail.

## Why the signature moved

[`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
§Context read `ADR-0031` §5's *Budgets* paragraph against what this split actually built and found
that its fifteen-minute resend suppression — *"a mail is sent only if the player has no live token
issued within the last 15 minutes, read from `issued_at` on the existing `UNIQUE (player_id)`
row"* — names both endpoints and reads naturally as covering both, and that the split implemented
it on one. `TASK-041613` puts the check inside `PasswordResets.issue`; this ticket previously fixed
`claimPending` as returning `Unit` — *"there is no outcome for a caller to branch on"* — and
`TASK-041608` wrote unconditionally, so **a verification mail would follow every successful attach,
for ever**, and the attach budget would be the only cap on the mail it causes. That is why
`ADR-0079` §2 set `recovery-email` to five per sixty seconds rather than something looser, and its
§Consequences names the residual as *"a defect against `TASK-041607`, `TASK-041608` and
`TASK-041625` rather than a new question — a ticket for the planner, due before `EPIC-07`
configures a sender."* It raised no `DEC`, because §5 already fixes the mechanism, the number and
the column for the identical problem on the sibling table.

**This ticket is a port declaration** — its `verify:` block is `compileKotlin`, `ktlintCheck` and
`detekt`, with no test — so the result type it declares is what every later ticket branches on.
Enforcement is `TASK-041636`'s and the send condition is `TASK-041637`'s; the *contract* is here,
because a suppression rule that lives only in one implementation's private code is one a second
implementation silently omits.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — the port + sealed-result
house style, including `CreateCredentialResult`;
`poker-server/src/main/kotlin/duels/poker/server/auth/AuthSessions.kt`;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §2, §3 and §5 — in §5 read the
***Budgets*** paragraph, which is the one that moved this signature;
`docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md`
§Consequences, the *residual* bullet only — the rest of that ADR is `TASK-041628`'s.

## Scope

- `public interface RecoveryEmails` in `duels.poker.server.auth`, all members `suspend`:
  - `claimPending(playerId: PlayerId, address: EmailAddress, token: VerificationToken):
    ClaimPendingResult` — writes the pending row, replacing any the player already holds, **unless
    that player already has one issued less than fifteen minutes ago**, in which case it writes
    nothing at all. `ADR-0031` §5 still answers `202` whatever happens, so the outcome is not a
    status code and no caller branches a *response* on it; it is the answer to *did this request
    cause a mail*, and nothing else in the system can answer that.
  - `verifyPending(token: VerificationToken): VerifyEmailResult`
  - `hasRecoveryEmail(playerId: PlayerId): Boolean`
  - `verifiedOwnerOf(address: EmailAddress): PlayerId?` — the `forgot-password` lookup. `null` for
    an address that is unknown **and** for one that is only pending, indistinguishably, because
    `ADR-0031` §3 says the two states are the same state as far as the account is concerned.
  - `detach(playerId: PlayerId)` — returns `Unit`; §5's `DELETE` answers `204` whether or not a row
    existed.
  - `deleteExpiredVerifications(): Int` — the sweep statement, returning the row count for a log
    line and for a test to assert on.
- `public sealed interface VerifyEmailResult` with three objects: `Verified`, `Refused` (the token
  is unknown, expired or already consumed — the three are one case by §5, so they are one value and
  a caller cannot accidentally distinguish them) and `AddressTaken` (§5's `409`).
- `public sealed interface ClaimPendingResult` with exactly two objects, **neither carrying a
  payload**:
  - `Claimed` — a pending row was written, and the `token` the caller passed in is the one now
    stored. This is the only value on which a verification mail may be sent.
  - `Suppressed` — the player already held a pending row issued inside the fifteen-minute window.
    **Nothing was written**, the token the caller passed in was discarded, and the outstanding one
    is still live. §5: inside that window the request is a complete no-op, and *"crucially the
    outstanding token is not invalidated, so a double-click does not destroy the link the player is
    about to use."*
- **Why a sealed type and not a `Boolean`, when `PasswordResets.issue` answers `Boolean` for the
  identical rule.** Recorded so it is chosen rather than fallen into. Inside this file the idiom is
  already *commands that can go more than one way get a sealed result* (`verifyPending`) and
  *queries return a plain value* (`hasRecoveryEmail`, `verifiedOwnerOf`,
  `deleteExpiredVerifications`); `claimPending` becomes a command with two outcomes. `issue` has
  two outcomes and will never have more, but the attach path already has a **second**
  mail-suppressing condition in flight — `TASK-041625`'s address-already-verified-elsewhere skip —
  so its caller decides *send or not* from more than one input, and an exhaustive `when` is what
  stops a third being absorbed silently by an `if`. If anyone later wants the two ports symmetric,
  the cheap direction is to give `issue` a sealed type, not to take this one away.
- KDoc on the interface stating the four properties a reader must not have to derive: the address
  never leaves this package except into `RecoveryMailer`; `verifiedOwnerOf` is the **only** read
  that returns an address's owner and it returns an id, never the address; expiry is enforced
  in every read by `WHERE expires_at > now()`, so a missed sweep is a retention defect and never a
  security hole; and **`claimPending` answering `Suppressed` is `ADR-0031` §5's fifteen-minute rule
  on this table**, is the same rule `PasswordResets.issue` answers `false` for, and is the only
  per-account cap on verification mail — so an implementation that always answers `Claimed`
  satisfies the type and breaks the design.

## Out of scope

- Any implementation — `TASK-041608` onward. **The fifteen-minute check itself is `TASK-041636`'s**,
  inside `claimPending`'s existing transaction; `TASK-041608` implements the write and answers
  `Claimed` unconditionally.
- The send condition. `TASK-041637` makes the handler mail only on `Claimed`; until it merges the
  endpoint mails on every successful attach, exactly as `ADR-0079` §Consequences describes.
- A method returning an `EmailAddress` to a caller. `verifiedOwnerOf` deliberately answers a
  `PlayerId?`; the address needed to actually send is read inside the storage layer at send time,
  where §6.3 requires it to stay.
- An `isVerified` flag or a method that reports a *pending* address. `ADR-0031` §3: until
  verification the account is exactly an account with no email, and a port that could report the
  half-attached state is how that stops being true. **This is also why the fifteen-minute check
  cannot sit in the route**: a handler that judged the window itself would need the `issued_at` of
  a pending row, which is exactly the member this bullet refuses — and it would be a read-then-write
  window, so two concurrent attaches would both find no live row, both write, and both mail. The
  check belongs where §5 puts its sibling: inside the writing transaction.
- A `windowRemaining` or `retryAfter` payload on `Suppressed`. §5 answers `202` identically in every
  case, and a value the caller could surface is the oracle the whole endpoint is built to avoid.
- `PasswordResets` — `TASK-041613`.

## Tests

None. This ticket adds no behaviour: it is an interface and a sealed result type, and the compiler
plus `ktlintCheck` and `detekt` are the whole gate — the same treatment `Credentials.kt` had when it
was first written. Every member is exercised by the ticket that implements it, and inventing a fake
implementation here to assert against would test the fake.

`verify:` therefore runs `compileKotlin` rather than a test task, which is a deliberate exception to
this story's one-criterion-one-test rule and is recorded so it is not read as an omission.

## Acceptance criteria

- [ ] `./gradlew :poker-server:compileKotlin` exits 0
- [ ] `RecoveryEmails` declares exactly the six members named in *Scope*, all `suspend`
- [ ] `VerifyEmailResult` has exactly three implementors, and none of them distinguishes *unknown*
      from *expired* from *already consumed*
- [ ] `ClaimPendingResult` has exactly two implementors, `Claimed` and `Suppressed`, and **neither
      declares a property** — no window remaining, no retry-after, no token, no address
- [ ] `claimPending`'s declared return type is `ClaimPendingResult`, not `Unit` and not `Boolean`
- [ ] The `RecoveryEmails` KDoc states that `Suppressed` is `ADR-0031` §5's fifteen-minute rule and
      that an implementation always answering `Claimed` satisfies the type and breaks the design
- [ ] No member of `RecoveryEmails` returns an `EmailAddress` or a `String` that could be one
- [ ] The file declares no class implementing `RecoveryEmails`
- [ ] Every command in `verify:` exits 0

## Proof

There is no test to redden, so the proof is a **compile-time** one and must be run rather than
argued.

1. Add a fourth implementor to `VerifyEmailResult` — `public object Expired : VerifyEmailResult` —
   and leave everything else alone. `compileKotlin` still succeeds: nothing yet has an exhaustive
   `when` over this type. **Record that result in the PR.** It is the honest statement of what this
   ticket's gate does and does not cover, and it is why the criterion above is a review criterion
   rather than a test: the first exhaustive `when` arrives with `TASK-041618`, and from then on a
   fourth case fails the build. Revert.
2. Change `verifiedOwnerOf` to return `EmailAddress?`. `compileKotlin` still succeeds. **Record
   this too**, and note in the PR that the *only* thing standing between this port and an address
   on the wire, today, is review — the structural gate against that arrives with `TASK-041616`'s
   `ProfileResponse` and `TASK-041606`'s member-set test, neither of which sees this signature.
   Revert.
3. Change `claimPending` back to returning `Unit` and delete `ClaimPendingResult`. `compileKotlin`
   still succeeds: nothing calls `claimPending` yet. **Record this one and say what it means.** The
   first *caller* is `TASK-041608`, which cannot compile without returning a value, and the first
   *branch* is `TASK-041637`'s exhaustive `when`, after which a `Unit` return fails the build — so
   the window in which this signature is free to be wrong closes at the very next ticket and stays
   closed. That is the entire argument for fixing it here rather than after twenty tickets have
   implemented against it, and it is why `ADR-0079` calls the `Unit` version a defect against this
   ticket rather than against the ones downstream. Revert.

All three results are negative, and writing them down is the point: a ticket whose *Proof* would be
vacuous should say so and say why, rather than inventing a mutation that reddens something
unrelated.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
