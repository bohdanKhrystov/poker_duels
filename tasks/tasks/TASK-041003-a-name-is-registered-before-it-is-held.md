---
schema: 2
id: TASK-041003
title: Setting a name registers it first, and a refused claim rolls the whole transaction back
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, write-path, identity, transaction]
depends_on: [TASK-041002]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileWrites.setDisplayName` spends the string in `name_registry` and hands it to the
player in **one transaction**, and a claim that is refused leaves no registry row behind.

## Why this one is `deep`

`ADR-0051` §2 names the defect a competent implementer ships here: *"a registry row left behind by a
refused claim **permanently burns a string nobody holds**, so a player who is already named could
spend the namespace one failed `PUT` at a time."* No single-threaded happy-path test catches it. The
test table below is written so that a missing rollback fails a named test.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesTest.kt` | modify — three new tests, no existing test edited |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileWrites.kt` | read — `SetNameResult`'s three cases |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §2, the whole of it |

## Scope — the two statements

```sql
-- 1. spend the string, or fail: 23505 means the namespace has already spent it.
INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN');

-- 2. hand it to the player, or fail: zero rows means they already hold a different name.
UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL
RETURNING id, coin_balance, display_name;
```

`ADR-0051` §2's answer table, unchanged and complete — **`SetNameResult` gains no fourth case**:

| What happens | Result | Transaction |
| --- | --- | --- |
| One row from statement 2 | `NameSet(profile)` | commit |
| `23505` from statement 1, and this player already holds that exact canonical form | `NameSet(profile)` | roll back, then read |
| `23505` from statement 1, in every other case | `NameTaken` | roll back |
| Zero rows from statement 2 | `AlreadyNamed` | **roll back** |

- **`connection.autoCommit = false` for the pair**, `commit()` only on the first row of the table,
  `rollback()` on every other outcome and on any exception. Restore `autoCommit` before the
  connection returns to the pool, or use a fresh connection per call as the class does today.
- **After a `23505`, the transaction is aborted.** PostgreSQL answers `25P02` (*current transaction is
  aborted*) to any further statement on it. So the idempotent-retry check — *does this player already
  hold that exact canonical form?* — must run **after** `rollback()`, on a clean transaction. The
  existing `CURRENT_PROFILE_SQL` read is the right shape; only its position changes. Getting this
  wrong turns every legitimate retry into a `500`.
- **`SQLSTATE` comes from `SQLException.sqlState`, never from a message** (`ADR-0029` §4's rule,
  already in this file).
- **Statement 2 can no longer raise `23505`** — any colliding name is already a registry row, so
  statement 1 refuses first. Keep `player_display_name_unique` in mind as a second line of defence
  only; do not add a second `catch` for it.
- **The screen fails closed by construction, not by a `catch`.** A `name_registry` that cannot be
  reached is a failed statement 1, so nothing is written and no `SetNameResult` is produced. Do not
  add a fallback path, a retry, or a `catch (SQLException)` that returns a result for anything but
  `23505`.
- detekt's `LongMethod` budget is 60 lines and `writeName` is the method that grows. Split the
  registry insert and the profile read into private helpers rather than spending the margin.
- Update the class KDoc: `ADR-0029` §5's *"the write is one statement"* is spent, and the rollback is
  load-bearing rather than tidy.

## Out of scope

- The foreign key — `TASK-041009`. Between this ticket and that one a `player` row can still hold a
  name with no registry row; that is expected and is what keeps every unconverted fixture green.
- Every test fixture that writes a display name with raw SQL — `TASK-041004` … `TASK-041008`.
- `SetNameResult`, `ProfileWrites`, `ProfileRoutes` and the endpoint's four status codes. Nothing
  about them changes (`ADR-0052` §7).
- `displayNameRemoved` on the returned DTO — `TASK-041017`.
- Any `SELECT` against `name_registry` before the insert. There is no read-then-write here; the index
  is the check.

## Tests

`PostgresProfileWritesTest`, `-PrequireDocker=true`. Three new tests. No existing test in the file is
edited — every one of them passes unchanged, because a player who is handed a name by the raw fixture
and then refused is exactly the case the table above already covers.

| Test | Proves |
| --- | --- |
| `aRefusedSecondNameLeavesNoRegistryRow` | Player holds `"Ann"` (set through `setDisplayName`). A second call with `"Bea"` answers `AlreadyNamed`, **and** `SELECT count(*) FROM name_registry WHERE name = 'Bea'` is `0`. This is the burn-a-string defect: without the rollback the count is `1`, the result is still `AlreadyNamed`, and every other test in this file stays green |
| `theSameNameAgainIsStillTheSameProfile` | The same player calling `setDisplayName` with `"Ann"` twice gets `NameSet` both times with `displayName == "Ann"`, and `SELECT count(*) FROM name_registry` is `1`. Fails against an implementation that reads the profile on the aborted transaction (`25P02` surfaces as a thrown `SQLException`, not a result) and against one that answers `NameTaken` for the retry |
| `aNameHeldByAnotherPlayerIsRefusedAndCostsNothing` | Player A holds `"Cid"`. Player B's claim of `"Cid"` answers `NameTaken`, B's `display_name` is still `NULL`, `name_registry` still holds exactly one row for `"Cid"` with `reason = 'TAKEN'`, and B can then set `"Dot"` and get `NameSet`. The final claim is what makes *"the loser burns nothing"* an assertion rather than a sentence |

Each test asserts the registry **count for a named string**, never `count(*)` over the whole table
alone: a whole-table count cannot tell a leaked `"Bea"` row from a legitimate `"Ann"` row.

## Acceptance criteria

- [ ] `PostgresProfileWritesTest.aRefusedSecondNameLeavesNoRegistryRow` passes
- [ ] `PostgresProfileWritesTest.theSameNameAgainIsStillTheSameProfile` passes
- [ ] `PostgresProfileWritesTest.aNameHeldByAnotherPlayerIsRefusedAndCostsNothing` passes
- [ ] Every test already in `PostgresProfileWritesTest` passes with its assertions unchanged, and no
      existing test method in that file is edited or renamed
- [ ] `PostgresProfileWrites.kt` sets `autoCommit = false` and calls `rollback()` on the
      `AlreadyNamed`, `NameTaken` and idempotent-retry paths
- [ ] `PostgresProfileWrites.kt` reads the current profile only after a `rollback()`, never on the
      aborted transaction
- [ ] `SetNameResult` still has exactly three cases and `ProfileWrites.kt` is unmodified
- [ ] `PostgresProfileWrites.kt` contains no `SELECT` against `name_registry`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
