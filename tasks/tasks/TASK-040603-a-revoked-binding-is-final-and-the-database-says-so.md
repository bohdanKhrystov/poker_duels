---
schema: 2
id: TASK-040603
title: A revoked binding is final, and the database is what refuses to undo it
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, schema, trigger, security]
depends_on: [TASK-040602]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DeviceBindingFinalityTest' -PrequireDocker=true
---

## Goal

`V7`'s `device_binding_revocation_final` trigger is pinned by tests: `NULL → a timestamp` succeeds
once, and every later change to `revoked_at` raises `restrict_violation`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DeviceBindingFinalityTest.kt` | create |

Read, and do not edit: `poker-server/src/main/resources/db/migration/V7__device_binding.sql`,
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §2. Nothing else.

## Scope

- One new test class, `duels.poker.server.db.DeviceBindingFinalityTest`, in the shape
  `SchemaConstraintsTest` already uses: `PostgresTestSupport.freshDatabase()` then
  `Migrations.migrate(dataSource)` in `@BeforeEach`, raw JDBC, `assertFailsWith<SQLException>`.
- A private `bind(deviceId: String): UUID` helper that writes one `player` row
  (`INSERT INTO player (id, coin_balance) VALUES (?, 0)`) and one live `device_binding` row
  (`INSERT INTO device_binding (device_id, player_id) VALUES (?, ?)`), returning the player id.
- **Every failure is matched on `SQLException.sqlState == "23001"`**, never on the message, per
  `ADR-0029` §4's rule that `ADR-0049` §2 repeats. The message is asserted separately, once, in
  `unRevokingIsRefused`, so a trigger renamed to raise a different exception still fails loudly.

## Out of scope

- The two partial unique indexes and the primary key — `TASK-040604`.
- Anything that calls `PostgresPlayerDirectory` — `TASK-040605`.
- The Kotlin revoke path. No `DeviceBindings` port exists yet; every statement here is raw SQL.
- Editing `V7__device_binding.sql`. A merged migration is immutable, and this ticket asserts what
  `TASK-040601` already landed. If a test here fails, the finding is a `DEC`, not an edit.

## Tests

`DeviceBindingFinalityTest`

| Test | Proves |
| --- | --- |
| `revokingALiveBindingSucceedsOnce` | The positive control. After `UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL`, `executeUpdate` returns `1` and a `SELECT revoked_at` reads non-null. Without this, the three refusals below could all pass against a trigger that refuses *every* update |
| `unRevokingIsRefused` | On the row just revoked, `UPDATE device_binding SET revoked_at = NULL WHERE player_id = ?` raises. `sqlState` is `23001` and the message contains `a revoked device binding is final` |
| `movingARevocationToADifferentTimestampIsRefused` | On the same row, `UPDATE device_binding SET revoked_at = now() + interval '1 hour' WHERE player_id = ?` raises with `sqlState` `23001`. This is the case `revoked_at IS NOT NULL` alone would let through if the trigger compared against `NULL` instead of `IS DISTINCT FROM OLD.revoked_at` |
| `writingTheSameRevocationTimestampAgainIsAllowed` | Read the row's `revoked_at` back, then `UPDATE device_binding SET revoked_at = ? WHERE player_id = ?` binding that exact value: `executeUpdate` returns `1` and nothing raises. `IS DISTINCT FROM` makes an idempotent rewrite a no-op, and this is the assertion that says so rather than leaving it to a reader |
| `theTriggerIsScopedToTheRevokedAtColumn` | A **catalog** assertion, not a behavioural one, and the ticket is explicit about why. Query `pg_trigger` joined to `pg_attribute` through `unnest(tgattr)` for `device_binding`'s non-internal triggers: it returns exactly one row, `(device_binding_revocation_final, revoked_at)`. Under a plain `BEFORE UPDATE` declaration `tgattr` is empty, `unnest` yields nothing, and the query returns zero rows |

Every test builds its own binding through `bind(...)` with its own device id string. **No test reads
a row another test wrote** — `freshDatabase()` drops the schema per test, so a shared fixture would
be a lie about isolation rather than a saving.

`writingTheSameRevocationTimestampAgainIsAllowed` reads the stored value with
`getObject(1, OffsetDateTime::class.java)` and binds that same object back, so the comparison is
between two values PostgreSQL produced rather than between a value and a re-parsed string.

**Why `theTriggerIsScopedToTheRevokedAtColumn` cannot be behavioural.** The obvious test —
`UPDATE device_binding SET bound_at = now()` on a revoked row, expecting no refusal — **passes under
both declarations**, because without `OF revoked_at` the trigger still fires but its body evaluates
`NEW.revoked_at IS DISTINCT FROM OLD.revoked_at` to false and returns `NEW`. It would be a vacuous
test that reads like a real one, which is exactly why the catalog query replaces it.

## Acceptance criteria

- [ ] `DeviceBindingFinalityTest.revokingALiveBindingSucceedsOnce` passes
- [ ] `DeviceBindingFinalityTest.unRevokingIsRefused` passes
- [ ] `DeviceBindingFinalityTest.movingARevocationToADifferentTimestampIsRefused` passes
- [ ] `DeviceBindingFinalityTest.writingTheSameRevocationTimestampAgainIsAllowed` passes
- [ ] `DeviceBindingFinalityTest.theTriggerIsScopedToTheRevokedAtColumn` passes
- [ ] Both refusal tests assert `exception.sqlState == "23001"`; exactly one of them also asserts a
      substring of the message
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

The mutation is on the trigger, run **locally and reverted** — the migration file itself is
immutable and this ticket edits nothing under `src/main`.

Change `V7`'s trigger declaration from `BEFORE UPDATE OF revoked_at ON device_binding` to
`BEFORE UPDATE ON device_binding`, leaving the function body alone, and re-run this class.
**Exactly one test reddens: `theTriggerIsScopedToTheRevokedAtColumn`**, whose catalog query now
returns zero rows because `tgattr` is empty. The other four are untouched: three of them update
`revoked_at` itself, which fires the trigger under either declaration, and the fourth is the
successful first revocation.

A second mutation, for the function body: change
`NEW.revoked_at IS DISTINCT FROM OLD.revoked_at` to `NEW.revoked_at IS NOT NULL`. Now
`movingARevocationToADifferentTimestampIsRefused` still raises, but
`writingTheSameRevocationTimestampAgainIsAllowed` **also** raises and reddens, and
`unRevokingIsRefused` stops raising and reddens too — `revoked_at = NULL` no longer satisfies the
condition. **Two tests**, in opposite directions, which is what the pair is for. Revert both;
`git status` must be clean of the migration.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
