---
schema: 2
id: TASK-040508
title: delete removes the row, and says the same thing twice
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, db, session]
depends_on: [TASK-040507]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresAuthSessionsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresAuthSessions.delete` is one `DELETE` keyed on the digest, it is total and idempotent, and
it touches nothing but `auth_session`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresAuthSessionsTest.kt` | modify |

Read `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §3 — why this returns
nothing and reports nothing.

## Scope

- `DELETE FROM auth_session WHERE token_hash = ?`, replacing the `TODO`, using the same private
  digest function.
- **Returns `Unit`, and does not report whether a row went.** `ADR-0030` §3: sign-out answers `204`
  either way, and a port that reported the count would invite an endpoint to answer `404` and tell
  a caller which tokens exist.
- It writes nothing to `player`, `credential`, `duel` or `duel_result`. `ADR-0030` §2 counts the
  statements that write `player` and this is not one of them.

## Out of scope

- The endpoint — `TASK-040515`.
- Deleting every session a player holds (`DELETE … WHERE player_id = ?`). `ADR-0049`/`ADR-0050`'s
  revoke path is `STORY-0406`'s.

## Tests

`PostgresAuthSessionsTest` — new methods only, nothing existing edited.

| Test | Proves |
| --- | --- |
| `deletingRemovesThatRow` | after `delete`, `playerOf` answers `null` and `SELECT count(*)` is `0` |
| `deletingTwiceIsTheSame` | a second `delete` of the same token returns normally and leaves the count at `0` |
| `deletingOneSessionLeavesTheOther` | the same player holds two sessions; deleting one leaves the other readable — **the discriminating case: a `DELETE` keyed on `player_id` instead of `token_hash` passes every test above and fails this one** |
| `deletingLeavesThePlayerRowAlone` | the player's `id`, `device_id`, `coin_balance` and `display_name` are byte-identical before and after |

## Acceptance criteria

- [ ] `PostgresAuthSessionsTest.deletingRemovesThatRow` passes
- [ ] `PostgresAuthSessionsTest.deletingTwiceIsTheSame` passes
- [ ] `PostgresAuthSessionsTest.deletingOneSessionLeavesTheOther` passes
- [ ] `PostgresAuthSessionsTest.deletingLeavesThePlayerRowAlone` passes
- [ ] `PostgresAuthSessions.kt` contains no `TODO`
- [ ] Every command in `verify:` exits 0

## Proof

Change the predicate to `WHERE player_id = (SELECT player_id FROM auth_session WHERE token_hash = ?)`
and only `deletingOneSessionLeavesTheOther` goes red. That is the mutation the other three cannot
see.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
