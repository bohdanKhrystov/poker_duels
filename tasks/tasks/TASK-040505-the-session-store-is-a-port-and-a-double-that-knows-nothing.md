---
schema: 2
id: TASK-040505
title: The session store is a port, and a double that has issued nothing
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, auth, session]
depends_on: [TASK-040504]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.NoAuthSessionsTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`AuthSessions` exists as a port with three functions and no implementation in `main`, and the
test sources gain the double every later route and resolver test will record against.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/AuthSessions.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/NoAuthSessions.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/NoAuthSessionsTest.kt` | create |

Read `poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — the port-here,
implementation-there shape and the KDoc register to copy — and `ADR-0027` §2. Nothing else.

## Scope

- The port, in `duels.poker.server.auth`:

  ```kotlin
  public interface AuthSessions {
      public suspend fun issue(playerId: PlayerId): SessionToken
      public suspend fun playerOf(token: SessionToken): PlayerId?
      public suspend fun delete(token: SessionToken)
  }
  ```

- KDoc carries the two facts a later reader will want to undo. **Nothing on this port returns a
  stored form**: `issue` hands back the plaintext token exactly once, because that is the only
  moment it exists (`ADR-0027` §2), and no function anywhere answers a `token_hash`. And
  `playerOf` answers `null` for an expired row as well as an unknown one, **indistinguishably** —
  expiry is enforced in the read, not by a sweep, so an expired row is garbage rather than a hole.
- `internal object NoAuthSessions : AuthSessions` in the test sources: `playerOf` answers `null`,
  `delete` does nothing, `issue` throws. Its own file, because ktlint's filename rule gives a single
  top-level declaration the file name it matches — a `AuthSessionDoubles.kt` holding one object
  fails `ktlintCheck` and cannot be auto-corrected.
- It lives in `duels.poker.server.auth`'s **test** source set so the `db`, `http` and socket suites
  can all reach it; it is the *no session was ever issued* case every pre-sign-in test needs.

## Out of scope

- `PostgresAuthSessions` — `TASK-040506`. Nothing in `main` implements this port yet, and nothing
  has to: no `main` class references `AuthSessions` until `TASK-040510`.
- A recording double that remembers what was issued — the first ticket that needs one adds it.

## Tests

`NoAuthSessionsTest`

| Test | Proves |
| --- | --- |
| `everyTokenIsUnknown` | `playerOf(SessionToken("a"))` and `playerOf(SessionToken("b"))` are both `null` — two different tokens, so the answer is not one memoised value |
| `deletingAnUnknownTokenIsNotAnError` | `delete(SessionToken("a"))` returns normally |
| `issuingThrows` | `issue(PlayerId("p1"))` throws — a test that reaches it is a test using the wrong double |

## Acceptance criteria

- [ ] `NoAuthSessionsTest.everyTokenIsUnknown` passes
- [ ] `NoAuthSessionsTest.deletingAnUnknownTokenIsNotAnError` passes
- [ ] `NoAuthSessionsTest.issuingThrows` passes
- [ ] `grep -rn "token_hash" poker-server/src/main/kotlin/duels/poker/server/auth` finds nothing
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
