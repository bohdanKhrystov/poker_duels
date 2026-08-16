---
schema: 2
id: TASK-040103
title: One name, whatever case it is typed in
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, schema, tests, identity]
depends_on: [TASK-040102]
verify:
  - ./gradlew :poker-server:test --tests '*DisplayNameUniquenessTest' -PrequireDocker=true
---

## Goal

The unique index folds case across the whole of Unicode, and refuses a second player the name a
first one holds — while leaving genuinely different names, and any number of nameless profiles,
alone.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameUniquenessTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameSchemaTest.kt` | read — the fixture shape this file copies |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §1, and why the fold is `und-x-icu` rather than the cluster default |

## Scope

- A new test class beside `DisplayNameSchemaTest`, same container setup, inserting two players and
  attempting to name them.
- **A collision is `23505` and is asserted on the `sqlState`**, with the index name
  `player_display_name_unique` checked in the message as a secondary, not as the assertion.
- **Each refusal has a near miss beside it.** `Bob` versus `bob` is a collision; `Bob` versus `Bobby`
  is not. Without the second, a test passes against an index that refuses every name.

## Out of scope

- The blocklist and the retired-name set — `STORY-0410`. Uniqueness here has exactly one source of
  truth.
- The `409` the endpoint answers — `TASK-040116`. This is the database's half.
- Two writers racing for one name — `TASK-040112`, which needs two connections and a held
  transaction.

## Tests

`DisplayNameUniquenessTest`

| Test | Proves |
| --- | --- |
| `aSecondPlayerCannotTakeAHeldName` | the same string twice raises `23505` |
| `theFoldIsCaseInsensitive` | `Bob` then `bob` raises `23505` — the impersonation the human named |
| `theFoldReachesBeyondAscii` | `Élodie` then `élodie` raises `23505`, which the cluster default `lower()` might not do |
| `aDifferentNameIsAccepted` | `Bob` then `Bobby` both store — the fold refuses collisions, not names |
| `aHomoglyphIsADifferentName` | Cyrillic `а` in `аce` stores beside Latin `ace`, and **this is recorded as the accepted residual**, not a defect (`ADR-0038`) |
| `anyNumberOfProfilesHaveNoName` | three `NULL` names coexist — a btree unique index admits many nulls |
| `aRefusedNameLeavesTheLoserUnnamed` | after the `23505`, the second player's `display_name` is still `NULL` and it can be named with something else immediately |

## Acceptance criteria

- [ ] All seven tests above pass
- [ ] Every collision asserts `sqlState == "23505"`
- [ ] `theFoldReachesBeyondAscii` uses a non-ASCII pair, so a fold that only handles `a`–`z` fails it
- [ ] `aDifferentNameIsAccepted` and `anyNumberOfProfilesHaveNoName` both pass, so the index is shown
      to permit as well as refuse
- [ ] `aHomoglyphIsADifferentName` carries a comment naming `ADR-0038` as the reason it asserts
      acceptance rather than refusal
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
