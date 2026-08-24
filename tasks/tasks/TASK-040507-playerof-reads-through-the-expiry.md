---
schema: 2
id: TASK-040507
title: playerOf reads through the expiry, and a clock thirty days on refuses
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, db, session]
depends_on: [TASK-040506]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresAuthSessionsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresAuthSessions.playerOf` answers the player a live session names, and answers `null` — the
same `null` an unknown token gets — for one that has expired, decided by the row and the database's
own `now()` rather than by a sweep.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresAuthSessionsTest.kt` | modify |

## Scope

- One statement, replacing the `TODO`:

  ```sql
  SELECT player_id FROM auth_session WHERE token_hash = ? AND expires_at > now()
  ```

- `token_hash` binds the same digest `issue` computes, through the same private function. There is
  one digest implementation in this file and there is never a second.
- No row → `null`. An expired row → `null`. **The two are indistinguishable to the caller and the
  KDoc says so**, because the alternative tells a stranger which tokens once existed.
- `now()` is SQL's, not the injected clock's: the predicate is evaluated by the database against
  the same `TIMESTAMPTZ` scale the row was written on. The injected clock is what a **test** moves
  to produce an expired row; it is not what the query compares against.

## Out of scope

- `delete` — `TASK-040508`, still a `TODO`.
- Deleting or sweeping expired rows. `ADR-0027` §2: an expired row is garbage, never a hole, and
  housekeeping — if it is ever built — joins `ADR-0025`'s existing ticker.

## Tests

`PostgresAuthSessionsTest` — new methods only. **Nothing already in the file changes.**

| Test | Proves |
| --- | --- |
| `aLiveTokenNamesItsPlayer` | issue for `alice`, `playerOf` answers `alice`; issue for `bob` in the same test, `playerOf` answers `bob` — **two players, so the answer cannot be one row the query happened to find** |
| `anUnknownTokenIsNull` | `playerOf(SessionToken("not-a-token"))` is `null` |
| `anExpiredTokenIsNull` | issue through an instance whose clock is fixed at `Instant.now() - 31 days`, so the row lands already past its own `expires_at`; `playerOf` answers `null`. The row expires, not the reader — **no test sleeps and no test moves the database's clock** |
| `aTokenIssuedThirtyDaysAgoLessAnHourStillReads` | the same construction with a clock at `Instant.now() - 30 days + 1 hour` still answers the player — the boundary from the other side, so `anExpiredTokenIsNull` cannot be satisfied by a predicate that refuses everything |
| `oneExpiredSessionDoesNotHideALiveOne` | the same player holds one expired and one live session; the live token still answers, the expired one still does not |

## Acceptance criteria

- [ ] `PostgresAuthSessionsTest.aLiveTokenNamesItsPlayer` passes
- [ ] `PostgresAuthSessionsTest.anUnknownTokenIsNull` passes
- [ ] `PostgresAuthSessionsTest.anExpiredTokenIsNull` passes
- [ ] `PostgresAuthSessionsTest.aTokenIssuedThirtyDaysAgoLessAnHourStillReads` passes
- [ ] `PostgresAuthSessionsTest.oneExpiredSessionDoesNotHideALiveOne` passes
- [ ] No test in this file calls `Thread.sleep` or `delay`
- [ ] Every test that was in the file before this ticket still passes, unedited
- [ ] Every command in `verify:` exits 0

## Proof

Delete `AND expires_at > now()` from the statement and `anExpiredTokenIsNull` and
`oneExpiredSessionDoesNotHideALiveOne` both go red while the other three stay green. Run it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
