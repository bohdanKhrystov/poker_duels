---
schema: 2
id: TASK-021015
title: The migration tests live with the migrations
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, persistence, test-cohesion]
depends_on: [TASK-021014]
verify:
  - ./gradlew :poker-server:test --tests '*MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`bothMigrationsApplyToAnEmptyDatabase` and `aSecondMigrationRunIsStillANoOp` ended up in
`PostgresDuelResultStoreTest`, because `TASK-021014`'s Files table did not include `MigrationsTest`
and its three-file budget left no room.

They are migration tests sitting in the duel-result-store suite. The next person adding `V3` will
open `MigrationsTest.kt`, not find them, and either duplicate the coverage or assume it does not
exist.

It also made that ticket's own `verify` misleading: `--tests '*MigrationsTest'` does not match
`PostgresDuelResultStoreTest`, so the command named as proving the migration chain did not run
those tests at all. They passed under the second command instead.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

## Scope

- Move both tests into `MigrationsTest`, unchanged in substance. This is a move, not a rewrite: if
  an assertion needs altering to work in its new home, say why in your report rather than quietly
  weakening it.
- Remove them from `PostgresDuelResultStoreTest`, along with any import or helper left unused by
  the move. Leave every other test in that file untouched — it carries the deep-reviewed,
  mutation-proven transaction coverage.
- `MigrationsTest` already has `appliesEveryMigrationToAnEmptyDatabase`. Check the moved tests do
  not duplicate it; if `bothMigrationsApplyToAnEmptyDatabase` subsumes it, keep the stronger one
  and say which you dropped and why.

## Tests

No new behaviour. After the move, `./gradlew :poker-server:test --tests '*MigrationsTest'` must
actually exercise the migration-chain assertions — which is the thing that was not true before.

State in your report how many tests `MigrationsTest` runs before and after, so the move is visibly
a move rather than a loss.

## Done

All three `verify:` commands exit 0, the migration-chain tests run under the `MigrationsTest`
command, and `PostgresDuelResultStoreTest` keeps every transaction test it had.
