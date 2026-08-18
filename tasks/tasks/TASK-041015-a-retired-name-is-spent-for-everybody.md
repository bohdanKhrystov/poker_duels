---
schema: 2
id: TASK-041015
title: A retired name is spent for everybody, including the player it was taken from
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, moderation, identity]
depends_on: [TASK-041014]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RetiredNameIsSpentTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

After a takedown, `PUT /api/me/name`'s write path refuses the retired string to a stranger, to the
player it was taken from, and in a case neither of them typed — and answers each of them the same
way it answers a name somebody else holds.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/RetiredNameIsSpentTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileWrites.kt` | read — `SetNameResult`'s three cases |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §2's answer table and §6 |

## Scope

- One new test class exercising `PostgresProfileWrites.setDisplayName` against a real database, with
  `retire_display_name` called through `SELECT` as an operator would. Both are the real mechanisms;
  no test in this file hand-writes a `RETIRED` row.
- Every refusal asserts the **result type**, `SetNameResult.NameTaken`, and additionally that the
  refused player's `display_name` is still `NULL`. `ADR-0051` §2: blocked, retired and taken are all
  `409` and *"no answer says which source refused"*, so there is nothing else to assert and asserting
  more would be asserting a leak.
- Names in this file are chosen so the fold does work: `"Ann"` retired, `"ann"` and `"ANN"` claimed.

## Out of scope

- The blocklist and the fail-closed screen — `TASK-041016`.
- The HTTP status code. The endpoint maps `NameTaken` to `409` already and nothing about that mapping
  changes (`ADR-0052` §7); a route-level test here would be re-testing merged behaviour.
- What the player is *told* — `TASK-041018`, `TASK-041019` and `STORY-0411`.
- Any assertion that distinguishes a retired refusal from a taken one. That distinction must not
  exist on the wire.

## Tests

`RetiredNameIsSpentTest`, `-PrequireDocker=true`. Five tests. Fixture per test: alice resolves,
sets `"Ann"` through `setDisplayName`, and an operator retires it.

| Test | Proves |
| --- | --- |
| `theFormerHolderCannotTakeTheirOwnNameBack` | Alice, now nameless, calls `setDisplayName(alice, "Ann")` and gets `NameTaken`; her `display_name` is still `NULL`. `ADR-0038`'s *"including the player it was taken from"*, which is the case a naive `WHERE NOT EXISTS (… AND player_id <> me)` would get wrong |
| `nobodyElseCanTakeItEither` | Bob calls `setDisplayName(bob, "Ann")` and gets `NameTaken`, `display_name` still `NULL` |
| `aDifferentCaseIsTheSameSpentString` | Bob calls `setDisplayName(bob, "ann")` — lower case, never the stored form — and gets `NameTaken`. **The wrong implementation this must fail against**: any check comparing the stored `name` with `=` rather than through `name_registry_folded`, which passes the two tests above and lets the very next claimant take the retired name back in another case |
| `theFormerHolderCanTakeADifferentName` | Alice calls `setDisplayName(alice, "Bea")` and gets `NameSet` with `displayName == "Bea"`. `ADR-0052` §4: nothing is withheld and a new name may be set immediately. **Without this test the four refusals above are satisfied by a write path that refuses everything** |
| `aRetiredNameIsStillOneRow` | After all of the above, `SELECT reason, retired_from FROM name_registry WHERE name = 'Ann'` returns exactly one row, `RETIRED`, `retired_from` = alice. A refused claim must not have added a second row under a different case, and must not have promoted or demoted this one |

## Acceptance criteria

- [ ] `RetiredNameIsSpentTest.theFormerHolderCannotTakeTheirOwnNameBack` passes
- [ ] `RetiredNameIsSpentTest.nobodyElseCanTakeItEither` passes
- [ ] `RetiredNameIsSpentTest.aDifferentCaseIsTheSameSpentString` passes
- [ ] `RetiredNameIsSpentTest.theFormerHolderCanTakeADifferentName` passes and asserts the stored
      name is `"Bea"`
- [ ] `RetiredNameIsSpentTest.aRetiredNameIsStillOneRow` passes and asserts exactly one row
- [ ] Every refusal test asserts `SetNameResult.NameTaken` and a still-`NULL` `display_name`
- [ ] No test in the file asserts anything that distinguishes a retired refusal from a taken one
- [ ] No file outside this ticket is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
