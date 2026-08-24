---
schema: 2
id: TASK-040506
title: issue writes one row, a digest, and thirty days from the injected clock
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, db, session]
depends_on: [TASK-040505]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresAuthSessionsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresAuthSessions.issue` inserts exactly one `auth_session` row whose `token_hash` is the
SHA-256 of a token the row does not contain, stamped `issued_at = now` and
`expires_at = now + 30 days` from an injected `java.time.Clock`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresAuthSessionsTest.kt` | create |

Read `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` (the
`withContext(Dispatchers.IO) { dataSource.connection.use { … } }` shape and the `internal`
constructor seam), `poker-server/src/main/resources/db/migration/V4__credential_and_auth_session.sql`
(the table is already on disk — **this ticket adds no migration**), and
`docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md`. Nothing else.

## Scope

- `public class PostgresAuthSessions(dataSource: DataSource, clock: Clock, tokens: SessionTokens)`.
  Three collaborators, all injected; the public constructor may default `tokens` to
  `SessionTokens()` but **never** defaults the clock.
- **`java.time.Clock`, never `ServerClock`.** `ADR-0062` amends `ADR-0027` §1 on exactly this
  clause: `ServerClock` reports elapsed milliseconds from an arbitrary epoch, so a `TIMESTAMPTZ`
  stamped from it lands in 1970 and every session is dead the moment it is written. Say so in one
  KDoc line, because it is the mistake the ADR was written to stop.
- `SESSION_LIFETIME_DAYS = 30L`, a named constant; `expiresAt = clock.instant().plus(30, DAYS)`.
- One statement: `INSERT INTO auth_session (token_hash, player_id, issued_at, expires_at) VALUES (?, ?, ?, ?)`.
  `token_hash` is `MessageDigest.getInstance("SHA-256").digest(token.value.toByteArray(UTF_8))`
  bound with `setBytes`; the instants bind as `OffsetDateTime` at `ZoneOffset.UTC`, the way
  `PostgresDuelResultSink` already writes one.
- The digest is computed by a `private fun` in this file and **is never returned, logged or put in
  an exception message**. `issue` returns the `SessionToken` the minting produced.
- Two sessions for one player both insert. `auth_session` has no uniqueness on `player_id`, and
  `ADR-0027` §2 says a phone and a laptop are both legal.

## Out of scope

- `playerOf` — `TASK-040507`. `delete` — `TASK-040508`. Leave both as
  `TODO("TASK-040507")` / `TODO("TASK-040508")`: the Kotlin compiler will not accept a class that
  implements the port only partly, and a throwing stub is the least dangerous of the three shapes
  available — a real query would be the next ticket's whole scope, and an `emptyResult` stub is a
  wrong answer that no test would notice.
- Wiring this into `ServerComponents` — `TASK-040517`.
- Any migration. `V4` already carries the table and `AuthSessionSchemaTest` already pins it.

## Tests

`PostgresAuthSessionsTest` — Testcontainers, in the shape `PostgresCredentialsTest` already uses.
The fixed clock is `Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`.

| Test | Proves |
| --- | --- |
| `issuingWritesExactlyOneRow` | `SELECT count(*) FROM auth_session` is `1` after one call and `2` after a second call for the **same** player — a second row is legal, and a count of one after two calls would mean an upsert nobody asked for |
| `theRowNamesThePlayerItWasIssuedFor` | with **two** players issued in one test, each row's `player_id` is its own — one player alone agrees with a query that ignores the argument |
| `thePlaintextTokenIsNowhereInTheRow` | the returned token's UTF-8 bytes appear in no column: `token_hash` equals the SHA-256 and not the token, and `encode(token_hash, 'escape')` does not contain it |
| `theDigestIsTheSha256OfTheToken` | `token_hash` equals a SHA-256 the test computes itself from the returned token |
| `expiryIsThirtyDaysAfterIssue` | `expires_at - issued_at` is exactly 30 days, and `issued_at` equals the fixed clock's instant — **both, because a right interval off a wrong origin passes the first alone** |
| `twoClocksGiveTwoExpiries` | the same class built on a clock 10 days later writes an `expires_at` 10 days later — the clock is read, not a constant |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `PostgresAuthSessions` contains no public function returning a `ByteArray` or a digest
- [ ] No file under `src/main/resources/db/migration` changed
- [ ] Every command in `verify:` exits 0

## Proof

Replace `clock.instant()` with `Instant.now()` and `expiryIsThirtyDaysAfterIssue` goes red on the
`issued_at` half. Replace the injected `Clock` with `ServerClock`-style millis and every row lands
in 1970, which the same test catches — run it rather than trusting the sentence.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
