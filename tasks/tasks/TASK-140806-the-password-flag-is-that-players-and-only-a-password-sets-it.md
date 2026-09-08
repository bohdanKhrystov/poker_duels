---
schema: 2
id: TASK-140806
title: The password flag is that player's, and only a password sets it
type: task
status: done
parent: STORY-1408
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, db, account, security]
depends_on: [TASK-140805]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest.theProfileReadsTrueForAPlayerHoldingAPassword' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest.aCredentialOfAnotherKindIsNotAPassword' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ProfileResponse.hasPassword`, read back out of a real database through `PROFILE_OF_SQL`, describes
**that** player and answers `true` only for a `password` credential — so wiring it to a constant, in
either direction, or correlating the `EXISTS` to nothing, reddens a test.

## Why this exists

`TASK-140805` adds the field and the fourth correlated `EXISTS`, and its own gates cannot see the
statement run: `PostgresProfileReadsTest` constructs no `ProfileResponse`, so it compiles and passes
untouched through that ticket's edits and its probe reached green without it. That is a dependency,
not a coupling — the same reading `TASK-041641` made after `TASK-041616`, where four mutations of the
`hasRecoveryEmail` `EXISTS` (constant `true`, constant `false`, an uncorrelated `EXISTS`, and one
reading the wrong table) all built fully green.

The two `verify:` filters name **methods**, not just the class. A whole-class filter exits 0 whether
or not the methods exist, which is exactly how that omission got through.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` — `PROFILE_OF_SQL` and
`profileOf`'s construction. **The PR diff must contain no change to it**;
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — `CredentialKind` and
`Credentials.create`;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` — the constructor the
setup uses;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §2.

## Scope

- Add a `PostgresCredentials` to the existing `setupDatabase`, beside `recoveryEmails`, built with
  the public `PostgresCredentials(dataSource)` constructor. `alice` and `bob` are already resolved
  there and both tests below use both.
- Add the two tests named below. Each holds **two players in one database asserting opposite
  values**, so a flag wired to a constant in either direction reddens — the property the file's own
  class KDoc already states for `hasRecoveryEmail`. Extend that KDoc paragraph to name the two new
  methods and what they guard.
- `Credentials.create` is a `suspend` function; the tests are `runBlocking`, as the file's others are.

## Out of scope

- Editing `PostgresProfileReads.kt` in any way. If the statement is wrong, that is a finding to
  report, not a repair to make here.
- `PostgresProfileWrites`' two statements — `TASK-140807`.
- Any route-level or DTO-level assertion. `ProfileRouteTest` and `ProfileDtosTest` are not this
  ticket's.
- Asserting anything about a second credential of the **same** kind for one player. `DEC-027` (may
  one player hold several credentials?) is open, and `ADR-0132` §1 freezes nothing about it.

## Tests

`PostgresProfileReadsTest`

| Test | Proves |
| --- | --- |
| `theProfileReadsTrueForAPlayerHoldingAPassword` | Alice is given a `CredentialKind.PASSWORD` credential and Bob is given none; alice's profile reads `hasPassword == true` and bob's reads `false`, in the same database, in the same test. An uncorrelated `EXISTS` — or a constant in either direction — reddens on one of the two |
| `aCredentialOfAnotherKindIsNotAPassword` | Alice is given a credential of a kind that is **not** `password` — `CredentialKind("passphrase")`, constructible at the call site by design — and Bob none; alice's profile reads `false`, and the assertion states that it must **equal bob's** rather than merely also be false, so a statement that ignores the kind reddens. `ADR-0132` §1's whole reason for naming the field after the kind |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.theProfileReadsTrueForAPlayerHoldingAPassword` passes
- [ ] `PostgresProfileReadsTest.aCredentialOfAnotherKindIsNotAPassword` passes
- [ ] Both `--tests` filters above name a method and the Gradle task exits 0, so neither method is
      missing
- [ ] Every other test in `PostgresProfileReadsTest` passes, unedited
- [ ] The PR diff touches exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
