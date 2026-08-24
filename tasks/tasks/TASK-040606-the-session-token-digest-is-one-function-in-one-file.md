---
schema: 2
id: TASK-040606
title: The session-token digest is one internal function, in one file
type: task
status: ready
parent: STORY-0406
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, auth, db, refactor]
depends_on: [TASK-040605]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SessionTokenDigestTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresAuthSessionsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.PublicApiHasNoHashTest'
---

## Goal

The SHA-256 that turns a `SessionToken` into the `auth_session.token_hash` bytes lives in exactly
one function, so the revocation statement `TASK-040608` writes computes the same digest
`PostgresAuthSessions` does rather than a second copy of it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/SessionTokenDigest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/SessionTokenDigestTest.kt` | create |

Read, and do not edit: `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` §2,
`docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` §1.

## Scope

- A new file holding **one top-level function and no class**:

  ```kotlin
  internal fun sessionTokenDigest(token: SessionToken): ByteArray =
      MessageDigest.getInstance("SHA-256").digest(token.value.toByteArray(UTF_8))
  ```

  `internal`, never `public`: `PublicApiHasNoHashTest` reflects over every public member of
  `duels.poker.server.auth` and `duels.poker.server.db` and fails one that reads as a hash, and
  `ADR-0027` §1's *"no function anywhere returns a hash"* is what that test enforces. A file holding
  only functions is outside ktlint's filename rule, so `SessionTokenDigest.kt` is a legal name for
  it.
- **A fresh `MessageDigest` per call, inside the function.** `MessageDigest` instances are not
  thread-safe, and this is called from `Dispatchers.IO`; hoisting one into a `val` would be a
  concurrency defect that no test in this ticket would catch.
- `PostgresAuthSessions` deletes its `private fun tokenHash` and calls `sessionTokenDigest` at the
  three sites that used it — `issue`'s insert, `playerOf`, and `delete`. **Its behaviour does not
  change**, which is why `PostgresAuthSessionsTest` is in `verify:` unmodified.

## Out of scope

- Any new statement, table or port. `PostgresDeviceBindings` is `TASK-040608`.
- `SessionTokens` in `duels.poker.server.auth`, which mints tokens and hashes nothing.
- Changing `PostgresAuthSessionsTest` — **a named prohibition**. This ticket is behaviour-preserving,
  so a change there would mean the refactor was not one.

## Tests

`SessionTokenDigestTest` — a plain JVM test, no database, no `requireDocker`.

| Test | Proves |
| --- | --- |
| `theDigestOfAKnownTokenIsTheKnownSha256` | `sessionTokenDigest(SessionToken("abc"))` hex-encodes to `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad` — the published SHA-256 of the three bytes `abc`. A literal vector, not a value re-derived from `MessageDigest` inside the test, which would assert only that the function calls itself |
| `twoDifferentTokensDigestDifferently` | `sessionTokenDigest(SessionToken("a"))` and `sessionTokenDigest(SessionToken("b"))` are not content-equal. **Two inputs**, because a constant-returning implementation passes any single-input test that is not a literal vector |
| `theSameTokenDigestsIdentically` | Two calls with `SessionToken("a")` are content-equal. Together with the test above this is what says the function is a function of its argument |

Use `assertContentEquals` / `assertFalse(a.contentEquals(b))` — `ByteArray` equality is identity,
and `assertEquals` on two arrays passes or fails for the wrong reason.

## Acceptance criteria

- [ ] `SessionTokenDigestTest.theDigestOfAKnownTokenIsTheKnownSha256` passes
- [ ] `SessionTokenDigestTest.twoDifferentTokensDigestDifferently` passes
- [ ] `SessionTokenDigestTest.theSameTokenDigestsIdentically` passes
- [ ] `PostgresAuthSessionsTest` passes with **no edit to that file**
- [ ] `PublicApiHasNoHashTest` passes, and `sessionTokenDigest` is declared `internal`
- [ ] `PostgresAuthSessions.kt` contains no occurrence of `MessageDigest` and no function named
      `tokenHash`, and `SessionTokenDigest.kt` contains exactly one occurrence of `MessageDigest`.
      **Not a repository-wide grep**: `Argon2Hasher.kt` and `DuelFilter.kt` each hold a
      `MessageDigest` of their own, for a password hash and a filter fingerprint, and neither is a
      session token
- [ ] Every command in `verify:` exits 0

## Proof

Change `sessionTokenDigest` to `MessageDigest.getInstance("SHA-1")`.
`theDigestOfAKnownTokenIsTheKnownSha256` reddens — it is the only test in the class that pins the
algorithm, since the other two hold for any hash function. `PostgresAuthSessionsTest` stays
**green**: it issues and reads back through the same changed function, so a consistently wrong
digest is invisible to it. That is why the literal vector is the criterion and the round trip is not.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
