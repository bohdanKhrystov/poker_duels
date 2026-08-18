---
schema: 2
id: TASK-041005
title: The uniqueness fixtures register every name, and the fold refuses before the index does
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, test, fixtures, identity]
depends_on: [TASK-041004]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DisplayNameUniquenessTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`DisplayNameUniquenessTest`'s two write helpers register the name first, so the collisions it asserts
are refused by `name_registry_folded` rather than by `player_display_name_unique` — and every
assertion in the file stands unchanged.

## Why every use here can register

Unlike the four files after it, this one asserts **only `sqlState`** on its refusals — `"23505"`,
never a constraint name. `name_registry_folded` is `ADR-0029` §1's fold character for character
(`ADR-0051` §1), so it refuses exactly the strings `player_display_name_unique` refuses and answers
with the same `SQLSTATE`. Both helpers can therefore register unconditionally, which is the simplest
correct change and the one that survives `TASK-041010`'s foreign key.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameUniquenessTest.kt` | modify — two helper bodies |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — `name_registry_folded` |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §1's fold bullet and §2's last two bullets |

## Scope

- `insertPlayerWithName(displayName)` — insert `('<displayName>', 'TAKEN')` into `name_registry`,
  then run the existing `INSERT INTO player (id, device_id, coin_balance, display_name)`. Both
  statements on the same connection, no `ON CONFLICT`, no transaction: the registry insert is
  expected to be the statement that raises when the name is already spent, and swallowing that would
  put the test back on `player_display_name_unique`.
- `updatePlayerName(playerId, displayName)` — the same registry insert in front of its existing
  `UPDATE player SET display_name = ? WHERE id = ?`.
- `insertPlayerWithoutName` and `readDisplayName` are unchanged.
- No test method is edited: no name, no assertion, no fixture value.

## Out of scope

- Asserting a constraint *name* anywhere in this file. It asserts `sqlState` today and continues to;
  which index reports the collision is `TASK-041008`'s subject, in the file that does name one.
- The other four fixture files and the foreign key.

## Tests

`DisplayNameUniquenessTest`, `-PrequireDocker=true`. No test is added or edited. Six existing tests
must pass, and these are the ones that could otherwise pass for the wrong reason:

| Test | Proves, after this change |
| --- | --- |
| `aSecondPlayerCannotTakeAHeldName` | `"alice"` twice: the second registry insert raises `23505` and the first player's name is untouched |
| `theFoldIsCaseInsensitive` | `"Bob"` then `"bob"`: `name_registry_folded` refuses the case variant. **The wrong implementation this must fail against** is a helper that registers with `ON CONFLICT DO NOTHING` — the registry then silently accepts, the `player` insert raises `23505` from the old index, the test still passes, and the namespace has not spent the string it was supposed to spend |
| `theFoldReachesBeyondAscii` | `"Élodie"` then `"élodie"`: the ICU collation, not a JVM `lowercase()`, does the folding |
| `aRefusedNameLeavesTheLoserUnnamed` | `"champion"` is refused for the second player, whose `display_name` is still `NULL`, and `"challenger"` is then accepted |
| `aDifferentNameIsAccepted`, `aHomoglyphIsADifferentName` | Two distinct folds both register and both land. Without these, a helper that registered nothing at all would leave the four tests above green |

## Acceptance criteria

- [ ] Every test in `duels.poker.server.db.DisplayNameUniquenessTest` passes with its assertions
      unchanged
- [ ] `insertPlayerWithName` and `updatePlayerName` each issue
      `INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')` before their `player` write
- [ ] Neither registry insert carries an `ON CONFLICT` clause
- [ ] No test method in the file is added, removed, renamed or has an assertion changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
