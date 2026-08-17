---
schema: 2
id: TASK-040312
title: PostgresCredentials writes one row and reads no hash back
type: task
status: done
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, auth, db, security]
depends_on: [TASK-040311]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresCredentialsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresCredentials` implements the port against the `credential` table: `create` writes exactly
one row holding a PHC string, `verify` answers a `PlayerId` for the right secret and `null` for
everything else, and no `SQLException` escapes `duels.poker.server.db`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read — the `withContext` / `dataSource.connection.use` / `SQLState` shape this file copies |

## Scope

- `public class PostgresCredentials internal constructor(private val dataSource: DataSource, private val hasher: SecretHasher) : Credentials`,
  plus `public constructor(dataSource: DataSource) : this(dataSource, Argon2Hasher())`. The
  primary constructor is `internal` because `SecretHasher` is, and a public constructor cannot
  expose an internal type; the seam exists so `TASK-040313` can count calls.
- `create` is one statement:
  `INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)`,
  with `id` a fresh `UUID.randomUUID()` and `secret_hash` the result of `hasher.hash(secret)`.
  **`SQLSTATE 23505` → `IdentifierTaken`**, matched on the code and never on the message; any other
  `SQLException` is rethrown.
- `verify` is one statement:
  `SELECT player_id, secret_hash FROM credential WHERE kind = ? AND identifier = ?`.
  - a row, and `hasher.matches(presented, secret_hash)` → `PlayerId(playerId.toString())`;
  - a row, and it does not match → `null`;
  - a row whose `secret_hash` is `NULL` → `null`, with the dummy verification below still run;
  - **no row → run `hasher.matches(presented, DUMMY_PHC)` and then return `null`.**
- `DUMMY_PHC` is a `private const val`, exactly:

  ```
  $argon2id$v=19$m=19456,t=2,p=1$DOkn/CUOc626AxtmisFtpA$cXtRt8rAT9o79X3bZokMdJtapvk9646A/0v+Mls0dE4
  ```

  It is well-formed and it is **the hash of nothing** — 48 random bytes, kept by nobody. Its only
  job is to cost the no-such-account path one real Argon2 verification, which `ADR-0027` §6 requires
  because without it Argon2 *is* the enumeration oracle. It must parse: a malformed constant makes
  `matches` return `false` immediately, doing no work, and the defence disappears without a single
  test going red. (`$` opens a template in a Kotlin literal — escape it.)
- No `SELECT` reads `secret_hash` out of this class. It is read inside `verify`, compared, and
  dropped; it is never returned, never logged, never put in an exception message.

## Out of scope

- Proving the two failure paths cost the same — `TASK-040313`, which needs the counting seam this
  ticket builds.
- Rate limiting, `409`s, status codes and the sign-up endpoint — `STORY-0404`, `STORY-0405`.
- Wiring into `ServerComponents`. Nothing calls this until a route does.
- Anything touching `auth_session`. This class knows only about `credential`.

## Tests

`PostgresCredentialsTest`, against the container, with `runBlocking`. Test bodies are block bodies
with **no** explicit `: Unit`.

| Test | Proves |
| --- | --- |
| `aCredentialCreatedThenProvedAnswersThePlayerId` | `create` then `verify` with the same secret returns exactly the `PlayerId` that was written |
| `aWrongSecretAnswersNothing` | the same identifier with a different secret returns `null` |
| `anUnknownIdentifierAnswersNothing` | an identifier no row holds returns `null` — the same value as the line above, from a caller's point of view an identical answer |
| `theSameIdentifierUnderAnotherKindAnswersNothing` | the lookup keys on the pair, so a row written under `password` is not found under another kind |
| `aSecondCredentialWithTheSameIdentifierAnswersIdentifierTaken` | `IdentifierTaken` rather than an exception, and the **first** row still verifies afterwards |
| `theStoredRowHoldsAPhcStringAndNotThePassword` | reading `secret_hash` with raw SQL: it is not the password, it does not contain the password, and `parseArgon2PhcOrNull` accepts it |
| `twoPlayersWithTheSamePasswordGetDifferentStoredHashes` | two rows written from the identical secret hold different strings, and both verify — per-row salt, proven at the storage layer |

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] `anUnknownIdentifierAnswersNothing` and `aWrongSecretAnswersNothing` assert the **same** value,
      `null`; neither returns a distinguishable type, message or exception
- [ ] `IdentifierTaken` is decided from `SQLState == "23505"`; no branch and no assertion reads an
      exception message
- [ ] `theStoredRowHoldsAPhcStringAndNotThePassword` asserts all three things — not equal, does not
      contain, and parses
- [ ] `twoPlayersWithTheSamePasswordGetDifferentStoredHashes` asserts both that the strings differ
      **and** that both verify; either alone permits a defect
- [ ] `PostgresCredentials.kt` contains no function that returns the stored string, and no `log`,
      `println` or exception message mentioning it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
