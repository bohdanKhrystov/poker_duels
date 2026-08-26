---
schema: 2
id: TASK-041642
title: No read on Credentials answers with a string
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, auth, security, test]
depends_on: [TASK-041637]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.CredentialsHasNoHandleReadTest.credentialsDeclaresNoMemberReturningAString'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.CredentialsHasNoHandleReadTest.theSweepSeesTheFourMembersItIsChecking'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANullableString'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANonNullString'
  - grep -qF 'a handle read added to some other type passes this gate' poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsHasNoHandleReadTest.kt
  - grep -qF 'reflection reports a @JvmInline return type as the wrapper' poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsHasNoHandleReadTest.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`Credentials` declaring a member that returns `String` or `String?` fails the build, so
`verifyCurrent`'s merged refusal to add a player→identifier reverse lookup is a gate rather than a
KDoc sentence.

## Why this exists

`ADR-0082` §2 promotes an existing refusal to a build failure for the first time. `verifyCurrent`'s
KDoc already says a reverse lookup was deliberately **not** added — *"looking one up just to feed
`verify` would put a reverse lookup from player to identifier into the codebase for no other reason
than this one check"* — and `ADR-0082` §Alternatives records that `Credentials.handleOf(playerId):
String?` is what almost anyone reaches for first, because the handle lives in `credential` and
`PostgresCredentials` is the class that reads that table.

**This ticket runs before the one that adds the port read**, deliberately. `TASK-041643` is the
first ticket in this repository that has to produce a login handle, and the cheap wrong way to do it
is the one this gate reddens on. A gate that lands after the temptation has passed is a gate that
was never tested against it.

The gate is green today and this ticket changes no production code: `verify` returns `PlayerId?`,
`verifyCurrent` and `holdsCredential` return `Boolean`, and `create` returns
`CreateCredentialResult`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsHasNoHandleReadTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — the four members and
`verifyCurrent`'s KDoc paragraph this gate makes executable;
`poker-server/src/test/kotlin/duels/poker/server/auth/RecoveryMailerShapeTest.kt` — the
`declaredMemberFunctions` idiom **and** its `ThreeMemberControl` positive control, both of which
this file follows;
`docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md` §2, which is
the whole of this ticket.

## Scope

- `internal class CredentialsHasNoHandleReadTest` in `duels.poker.server.auth`, over
  `Credentials::class.declaredMemberFunctions` — the `kotlin.reflect.full` view, exactly as
  `RecoveryMailerShapeTest` uses and for the reason its KDoc gives.
- **The predicate is `returnType.classifier == String::class`, never `returnType == typeOf<String>()`.**
  `KType.classifier` discards nullability, so one comparison catches `String` and `String?` both;
  an equality against `typeOf<String>()` would miss `handleOf(playerId): String?`, which is the
  exact signature this gate exists for. The two bait tests below are what hold this.
- One `private` top-level bait class in this file declaring **both** `handleOf(playerId: PlayerId):
  String?` and `identifierOf(playerId: PlayerId): String`. Name it `HandleReadingControl` — a
  private top-level *class* is not scoped to its file the way a private top-level function is, so
  the name must be unique in `duels.poker.server.auth`; `HandleReadingControl` and
  `CredentialsHasNoHandleReadTest` are both free there today, and `ThreeMemberControl` is taken.
- One shared private helper takes a `KClass<*>` and returns the names of its `String`-returning
  members, so the real assertion and both baits run the identical code path. A helper that ignored
  its argument and read `Credentials::class` would pass the real test and fail both baits.
- **The class KDoc states what the gate cannot catch**, in the two exact phrases `verify:` greps
  for, because a limit a reader cannot find is the same as a limit nobody wrote — `TASK-040709`'s
  idiom verbatim:
  1. **`a handle read added to some other type passes this gate`.** It reads one named interface,
     not the package.
  2. **`reflection reports a @JvmInline return type as the wrapper`**, so
     `handleOf(playerId): LoginHandle` would sail through.

  Both stay review matters, and the sentence a reviewer applies is `ADR-0082` §2's: *the only read
  in this system that produces a login handle takes a proven `EmailAddress`.*

## Out of scope

- **Editing `Credentials.kt`, `PostgresCredentials.kt` or any production file.** This ticket is one
  new test file and nothing else; the gate is green against the code as merged. **Gated below** by
  the Files table having exactly one `create` row — a refusal with no assertion of its own.
- **Widening the sweep to a package**, as `PublicApiHasNoHashTest` does over
  `duels.poker.server.auth` and `duels.poker.server.db`. `ADR-0082` §2 specifies one interface, and
  a package sweep would flag every innocent `String`-returning member in two packages. The bait is
  borrowed from that file; the sweep is not. **Gated below.**
- **A `LoginHandle` value class**, which would make limit 2 above moot. `ADR-0082` §Alternatives
  rejects it as scope this decision does not need and names the six declarations it would move.
  Not ticketed.
- Properties. `declaredMemberFunctions` does not enumerate them, so a
  `val handle: (PlayerId) -> String?` is outside what this assertion can see; say so in the same
  KDoc, in whatever words, since only the two phrases above are greped.

## Tests

`CredentialsHasNoHandleReadTest`

| Test | Proves |
| --- | --- |
| `credentialsDeclaresNoMemberReturningAString` | The gate itself: no member of `Credentials` has `returnType.classifier == String::class`. Green against `develop` as merged |
| `theSweepSeesTheFourMembersItIsChecking` | The member-name set of `Credentials` is exactly `verify`, `verifyCurrent`, `create`, `holdsCredential`. The positive control the test above cannot do without: a helper that answered an empty list satisfies *no member returns a `String`* vacuously, forever |
| `theSweepFlagsABaitReturningANullableString` | The same helper, over `HandleReadingControl`, names `handleOf` — so `String?` is caught, which is the signature `ADR-0082` §2 says the gate reddens on |
| `theSweepFlagsABaitReturningANonNullString` | The same helper, over the same control, names `identifierOf` — the non-nullable half, so neither nullability escapes |

## Acceptance criteria

- [ ] `CredentialsHasNoHandleReadTest.credentialsDeclaresNoMemberReturningAString` passes
- [ ] `CredentialsHasNoHandleReadTest.theSweepSeesTheFourMembersItIsChecking` passes, and asserts
      the **set of four names**, not a count
- [ ] `CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANullableString` passes
- [ ] `CredentialsHasNoHandleReadTest.theSweepFlagsABaitReturningANonNullString` passes
- [ ] The three assertions above call **one** helper taking a `KClass<*>`; the file contains no
      second reflection path
- [ ] The file's class KDoc contains both greped phrases, character for character
- [ ] `git status` after the change shows exactly one added file and no modified file

## Proof

1. Change the predicate to `returnType == typeOf<String>()`.
   **`theSweepFlagsABaitReturningANullableString` reddens alone** — `handleOf` is no longer named —
   while `theSweepFlagsABaitReturningANonNullString` and the real gate stay green. This is the
   single most valuable mutation here: it is the difference between a gate that catches
   `handleOf(playerId): String?` and one that does not, and `String?` is the signature the ADR
   names. Revert.
2. Add `public suspend fun handleOf(playerId: PlayerId): String?` to `Credentials` (and a body to
   `PostgresCredentials` so it compiles).
   **`credentialsDeclaresNoMemberReturningAString` reddens**, naming `handleOf`, and
   `theSweepSeesTheFourMembersItIsChecking` reddens too, on the fifth name. Both reds are correct
   and both are the point. Revert both files — they are not in this ticket's budget.
3. Make the helper ignore its argument and always read `Credentials::class`.
   **Both bait tests redden**, *expected [handleOf] / [identifierOf], got []*, while
   `credentialsDeclaresNoMemberReturningAString` **stays green**. That asymmetry is why the baits
   exist: a hard-coded helper gates nothing and looks green.
4. Delete `theSweepSeesTheFourMembersItIsChecking` and make the helper return `emptyList()`.
   **Nothing in the remaining file reddens except the two baits.** Record it: this is the
   vacuous-assertion failure mode, and the reason the name-set test is a criterion above rather
   than a nicety. Restore both.
5. Wrap the bait's return in a `@JvmInline value class`.
   **`theSweepFlagsABaitReturningANullableString` reddens** — and that red is the limit, not a
   defect. Do **not** repair it by unwrapping value classes; revert, and confirm the class KDoc
   already says this in the greped phrase. This step exists so the limit is observed once rather
   than believed.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
