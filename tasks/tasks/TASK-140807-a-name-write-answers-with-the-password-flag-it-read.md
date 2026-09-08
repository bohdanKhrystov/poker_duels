---
schema: 2
id: TASK-140807
title: A name write answers with the password flag it read
type: task
status: backlog
parent: STORY-1408
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, db, account]
depends_on: [TASK-140806]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest.aNameWriteAnswersTrueForAPlayerHoldingAPassword' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest.theIdempotentRetryAnswersWithThePasswordFlagToo' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`PUT /api/me/name`'s `200` carries the caller's real `hasPassword`, on **both** statements that can
produce it — so `reportNameWrite`'s adoption of that body can never turn a claimed player anonymous
(`ADR-0132` §2).

## Why this exists

`TASK-140805` puts the same correlated `EXISTS` in `SET_NAME_SQL` and `CURRENT_PROFILE_SQL`, and
nothing in its gate set runs either statement against a database. `PostgresProfileWritesTest`
constructs no `ProfileResponse`, so it compiles and passes untouched through those edits, and the
`ADR-0070` probe reached green without it — a dependency, not a coupling.

**The two statements are two different code paths**, and only one of them is on the happy path:
`SET_NAME_SQL` answers when an unnamed player takes a name, while `CURRENT_PROFILE_SQL` is read by
`resultAfterRegistryConflict` on `ADR-0051` §2's idempotent retry, when the player sends the name
they already hold. A `false` left in either one is a live defect and neither test catches the other.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` — `SET_NAME_SQL`,
`CURRENT_PROFILE_SQL`, `toProfile()` and `resultAfterRegistryConflict`. **The PR diff must contain no
change to it**;
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — `CredentialKind.PASSWORD` and
`Credentials.create`;
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §2 and
§Residuals.

## Scope

- Add a `PostgresCredentials(dataSource)` to `setupDatabase`, beside `profileWrites`.
- Add the two tests below. Each holds **two players in one database asserting opposite values**, so
  a literal in either direction reddens — the same shape `PostgresProfileReadsTest` uses for the read
  statement.
- The existing case `sendingTheSameNameAgainSucceeds` is what proves the retry path is reachable;
  the second test below is that path with a credential attached. Do not edit it.

## Out of scope

- **`hasRecoveryEmail`.** `toProfile()` passes a literal `false` for it, and that is a known,
  reachable defect `ADR-0132` §Residuals names and deliberately leaves — *"a ticket for the planner,
  not a `DEC`"*. Do **not** repair it, and do **not** assert anything about `hasRecoveryEmail` in
  either new test: an assertion that it is `false` would freeze the defect as intended behaviour, and
  one that it is `true` would fail for a reason this ticket does not own.
- `displayNameRemoved`'s literal `false`, which **is** false by construction on every `200` from this
  route and is defended in the code's own comment.
- Editing `PostgresProfileWrites.kt` at all.
- `SetNameResult.AlreadyNamed` and `NameTaken`. Neither carries a profile, so neither can carry this
  field.

## Tests

`PostgresProfileWritesTest`

| Test | Proves |
| --- | --- |
| `aNameWriteAnswersTrueForAPlayerHoldingAPassword` | `SET_NAME_SQL`. A player given a `CredentialKind.PASSWORD` credential sets a name; the `NameSet` profile reads `hasPassword == true`. A second player with no credential sets a different name in the same database and reads `false` |
| `theIdempotentRetryAnswersWithThePasswordFlagToo` | `CURRENT_PROFILE_SQL`. A player with a password sets a name and then **sends the same name again**; the second call is still `NameSet` and its profile reads `hasPassword == true`. A second player with no credential, doing the same, reads `false` — so the retry path is proved on both answers and not only on the one the happy path already covers |

## Acceptance criteria

- [ ] `PostgresProfileWritesTest.aNameWriteAnswersTrueForAPlayerHoldingAPassword` passes
- [ ] `PostgresProfileWritesTest.theIdempotentRetryAnswersWithThePasswordFlagToo` passes
- [ ] Both `--tests` filters name a method and the Gradle task exits 0, so neither method is missing
- [ ] Neither new test mentions `hasRecoveryEmail`
- [ ] Every other test in `PostgresProfileWritesTest` passes, unedited
- [ ] The PR diff touches exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
