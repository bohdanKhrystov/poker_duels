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
outcome, so that no endpoint written later has to invent one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — the port + sealed-result
house style, including `CreateCredentialResult`;
`poker-server/src/main/kotlin/duels/poker/server/auth/AuthSessions.kt`;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §2, §3 and §5.

## Scope

- `public interface RecoveryEmails` in `duels.poker.server.auth`, all members `suspend`:
  - `claimPending(playerId: PlayerId, address: EmailAddress, token: VerificationToken)` — writes the
    pending row, replacing any the player already holds. Returns `Unit`: `ADR-0031` §5 answers `202`
    whatever happens, so there is no outcome for a caller to branch on.
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
- KDoc on the interface stating the three properties a reader must not have to derive: the address
  never leaves this package except into `RecoveryMailer`; `verifiedOwnerOf` is the **only** read
  that returns an address's owner and it returns an id, never the address; and expiry is enforced
  in every read by `WHERE expires_at > now()`, so a missed sweep is a retention defect and never a
  security hole.

## Out of scope

- Any implementation — `TASK-041608` onward.
- A method returning an `EmailAddress` to a caller. `verifiedOwnerOf` deliberately answers a
  `PlayerId?`; the address needed to actually send is read inside the storage layer at send time,
  where §6.3 requires it to stay.
- An `isVerified` flag or a method that reports a *pending* address. `ADR-0031` §3: until
  verification the account is exactly an account with no email, and a port that could report the
  half-attached state is how that stops being true.
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

Both results are negative, and writing them down is the point: a ticket whose *Proof* would be
vacuous should say so and say why, rather than inventing a mutation that reddens something
unrelated.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
