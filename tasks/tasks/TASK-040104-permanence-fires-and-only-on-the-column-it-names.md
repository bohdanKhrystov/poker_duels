---
schema: 2
id: TASK-040104
title: Permanence fires, and only on the column it names
type: task
status: ready
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, schema, tests, identity, coins]
depends_on: [TASK-040103]
verify:
  - ./gradlew :poker-server:test --tests '*DisplayNamePermanenceTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
---

## Goal

The permanence trigger refuses every transition `ADR-0029` §4 forbids, allows the two it permits, and
**does not fire on the coin-balance write** — which is the failure mode that would break a duel, not
a name.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNamePermanenceTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameUniquenessTest.kt` | read — the fixture shape this file copies |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | read — how a `FinishedDuel` is built and handed to `record`, which is the coin write this ticket must not disturb |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §4, which enumerates every transition |

## Scope

- A new test class beside the other two, same container setup, driving `UPDATE player SET
  display_name = …` directly rather than through any Kotlin port. The trigger has to hold against a
  `psql` session and a future admin tool; the test proves it the same way.
- **`restrict_violation` is `23001`, and it is asserted on the code**, never on the message.
- **The coin write is exercised for real**, through `PostgresDuelResultStore`, on a player who
  *already holds a name* — the `OF display_name` clause is the only thing standing between this
  trigger and every finished duel, and a trigger written without it passes all the name tests.

## Out of scope

- The `403` the endpoint answers, and the idempotent-retry `200` — `TASK-040111` and `TASK-040116`
  do that in Kotlin. The application refuses the write too; this ticket is the guard beneath it.
- The operator force-rename, which `ADR-0038` permits and `STORY-0410` builds. Today, `name → NULL`
  raises, and this ticket pins that.

## Tests

`DisplayNamePermanenceTest`

| Test | Proves |
| --- | --- |
| `anUnnamedProfileCanBeNamed` | `NULL → 'bob'` succeeds — the one transition the human's *chosen once* means |
| `aNamedProfileCannotBeRenamed` | `'bob' → 'robert'` raises `23001` and the stored name is still `bob` |
| `aNamedProfileCannotBeUnnamed` | `'bob' → NULL` raises `23001` |
| `aRenameThatOnlyChangesCaseIsRefused` | `'Bob' → 'bob'` raises `23001` — identity is exact equality of the canonical form |
| `writingTheIdenticalNameChangesNothing` | `'bob' → 'bob'` succeeds, and the row is unchanged, so a retried `PUT` is idempotent |
| `anInsertCarryingANameIsNotAViolation` | a row inserted with a name succeeds — the trigger is `BEFORE UPDATE`, and a profile is still born nameless |
| `theCoinWriteDoesNotFireTheTrigger` | `PostgresDuelResultStore`'s balance write against a **named** player succeeds and moves the balance by exactly one |

## Proof

**Name the edit that makes each guard red** — make it, run the suite, quote the failure in the PR,
revert:

1. Drop `OF display_name` from the trigger definition in `V3` → `theCoinWriteDoesNotFireTheTrigger`
   fails while every other test in this file still passes. That asymmetry is the reason this ticket
   exists; say so in the PR.
2. Change the trigger body to `IF FALSE THEN` → the three refusal tests fail. A guard nobody has
   seen fail is a guard nobody has tested.

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] Every refusal asserts `sqlState == "23001"` — `unique_violation` (`23505`) would pass a test
      that only checked "it threw", and this criterion is what separates them
- [ ] `theCoinWriteDoesNotFireTheTrigger` goes through `PostgresDuelResultStore`, not through a hand
      written `UPDATE`, and asserts the balance afterwards
- [ ] `PostgresDuelResultStoreTest` passes unchanged
- [ ] Both planted edits above were run and reported in the PR
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
