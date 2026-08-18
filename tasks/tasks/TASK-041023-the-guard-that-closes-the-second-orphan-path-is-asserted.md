---
schema: 2
id: TASK-041023
title: The guard that closes the second orphan path is asserted, not merely read
type: task
status: dropped
parent: STORY-0410
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, test, identity, regression]
depends_on: [TASK-041013]
verify:
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Dropped: the premise was wrong, and the coverage already exists

This ticket was filed during `TASK-041013` on the strength of a reading of the code, not a
measurement. Both of its claims turned out to be false when measured.

**It claimed** that `ADR-0051` §2's second orphan path — the permanence trigger raising after a
successful registry insert — is closed only by `SET_NAME_SQL`'s `AND display_name IS NULL`, and that
deleting that clause would leave the whole suite green while the orphan returned.

**What actually happens** when the clause is removed, run against a real database:

- **No orphan appears.** `writeName` sets `autoCommit = false` and its `catch (SQLException)` calls
  `connection.rollback()` unconditionally, so the registry insert and the refused `player` update
  are undone together. Measured directly: `registryCountForRemy=0`, `storedNameForPlayer=Quinn`.
- **The suite does not stay green.** Five of fourteen tests in `PostgresProfileWritesTest` fail, all
  on one root cause: the trigger raises `23001`, which is not `23505`, so `writeName` rethrows
  instead of classifying and a raw `SQLException` escapes `setDisplayName`.
- **Two of those five already existed** — `noSqlExceptionEscapes` and
  `aRefusedSecondNameLeavesNoRegistryRow`. The guard was already asserted. What was missing was not
  a test; it was a written-down reason.

The clause is still load-bearing, but for the failure mode those two tests describe, not the one
this ticket named.

## Why no test shipped

The test this ticket specified is `aRefusedSecondNameLeavesNoRegistryRow` (`TASK-041003`) with
different literals. That test already:

- uses a second name never registered anywhere in its fresh database (`"Bea"`);
- asserts the registry row count for it is `0`, by count, not by result type;
- goes through `PostgresProfileWrites.setDisplayName`, not a hand-built pair.

Both tests failed on the identical mutation for the identical reason. Their sensitivity to the one
property this ticket is about is indistinguishable, so shipping the new one would have added a
duplicate under a rationale the evidence contradicts.

Its comment already records the mechanism correctly: *"Without the rollback the count below is 1 and
the result is still `AlreadyNamed` — every other assertion in this file stays green either way."*

## What replaces it

Nothing. `TASK-041024` still stands and is unaffected: it isolates the **Kotlin rethrow** in
`NameBlocklistTest` from the foreign key that covers for it, which is a different guard on a
different call path, and one nothing currently asserts.
