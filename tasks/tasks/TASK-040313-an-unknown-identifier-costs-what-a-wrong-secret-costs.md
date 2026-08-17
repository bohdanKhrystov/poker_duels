---
schema: 2
id: TASK-040313
title: An unknown identifier costs exactly what a wrong secret costs
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, security, tests]
depends_on: [TASK-040312]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresCredentialsEnumerationTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The no-such-account path performs the same one Argon2 verification the wrong-secret path performs,
proven by counting the work rather than by timing it — so an attacker with a stopwatch learns
nothing about which handles exist.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsEnumerationTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` | read — the dummy verification and the internal constructor this test uses |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §6, and why an instant refusal is itself the oracle |

## Scope

- A counting `SecretHasher` written in the test file: it delegates every call to a real
  `Argon2Hasher` and records, per call, the `PresentedSecret` and the `storedPhc` it was given. It
  is constructed into `PostgresCredentials` through the `internal` constructor.
- **Nothing in this ticket asserts on elapsed time.** A timing assertion on a 19 MiB hash is a
  flaky test on a shared CI runner, and a green flaky test is worse than no test. What is asserted
  is that the same work happens: one `matches` call, on both paths, with the presented secret.
- The recorded `storedPhc` from the unknown-identifier path is fed to `parseArgon2PhcOrNull`. This
  is the trap worth catching: a malformed dummy constant makes `matches` return `false` before doing
  any Argon2 work at all, the defence silently disappears, and every other test in the story stays
  green.

## Out of scope

- Rate limiting by remote address — `ADR-0027` §6 puts it with the endpoint, in `STORY-0405`.
- Any change to `PostgresCredentials.kt`. If a change turns out to be needed, that is a finding to
  report, not a widening of this ticket.
- Timing measurement of any kind, including a "roughly equal" assertion with a tolerance.

## Tests

`PostgresCredentialsEnumerationTest`, against the container, with `runBlocking`.

| Test | Proves |
| --- | --- |
| `anUnknownIdentifierRunsTheSameOneVerificationAWrongSecretDoes` | both paths record exactly one `matches` call and zero `hash` calls — the counts are compared to each other **and** to 1, so a version that does nothing on both paths fails |
| `theDummyStringTheUnknownPathVerifiesAgainstIsWellFormed` | `parseArgon2PhcOrNull` accepts the `storedPhc` the unknown path actually passed, so the verification is real work rather than an early `false` |
| `theDummyVerificationIsGivenThePresentedSecret` | the recorded secret on the unknown path equals the one the caller presented — not an empty or constant stand-in |
| `aRowWhoseSecretHashIsNullStillCostsAVerification` | a `credential` row written by raw SQL with `secret_hash = NULL` answers `null` **and** records one `matches` call; the schema permits such a row, and an early return for it would be a second oracle |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `anUnknownIdentifierRunsTheSameOneVerificationAWrongSecretDoes` asserts the count is exactly
      `1` on each path, not merely that the two counts are equal — equal-at-zero is the defect this
      ticket exists to prevent
- [ ] `theDummyStringTheUnknownPathVerifiesAgainstIsWellFormed` asserts on the value the double
      **recorded**, not on a constant re-declared in the test
- [ ] `aRowWhoseSecretHashIsNullStillCostsAVerification` is present and its row is written with raw
      SQL, since the port offers no way to write a null hash
- [ ] No test in this file asserts on `System.nanoTime`, `measureTime`, or any elapsed duration
- [ ] `PostgresCredentials.kt` is unchanged by this ticket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
