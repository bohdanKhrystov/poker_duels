---
schema: 2
id: TASK-041022
title: docs/operations.md is the takedown's only call site
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, docs, operations, moderation]
depends_on: [TASK-041021]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.OperationsDocumentationTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A new `docs/operations.md` tells the one operator how to find a player, take a name away and curate
the blocklist from `psql`, and a test holds the document to the schema it describes.

## Files

| File | Action |
| --- | --- |
| `docs/operations.md` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/OperationsDocumentationTest.kt` | create |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the function's signature and the registry's columns |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §4's last two bullets and §5 |

## Scope — the document

`ADR-0051` §4: *"It is the only new document, and it is a **call site** rather than a procedure — the
procedure is in the migration."* So the document contains four SQL snippets and the prose around
them, and no steps that could drift:

1. **Find the player and the exact string they hold.**
   `SELECT id, display_name FROM player WHERE display_name IS NOT NULL AND lower(display_name COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu");`
2. **Take the name away.** `SELECT retire_display_name('<player id>', '<the name they hold>');`
   — stating that it returns the name it took, that a mismatch raises and writes nothing, and that
   **it cannot be undone**: nobody can un-retire a name, the victim of a mistake cannot reclaim
   theirs, and the only remedy is that they choose a different one (`ADR-0051` *Consequences*).
3. **Add a blocklist entry.**
   `INSERT INTO name_registry (name, reason) VALUES (normalize(btrim($1), NFC), 'BLOCKED');`
4. **Remove one.** `DELETE FROM name_registry WHERE name = $1 AND reason = 'BLOCKED';`

Four things the prose must say, because each is a decision somebody will otherwise assume the
opposite of:

- **The operator is whoever holds the database credentials.** There is no role, no account, no
  endpoint and no Gradle task, and there will not be one until there is a second operator
  (`ADR-0051` §4, §9).
- **A blocklist entry cannot be added over a name in use** — the insert raises `23505` and the two
  options are to leave it or to retire it from its holder (`ADR-0051` §5).
- **Nothing is re-screened.** A name already held when its string is blocked stays held until an
  operator retires it explicitly (`ADR-0051` §5).
- **The player is told.** They see `ADR-0052` §2's notice the next time they open the name surface,
  and nobody else is told anything.

Link it from the *Where to look* table in `CLAUDE.md`? **No** — that is a repository-root file this
story does not own. Say so here so nobody adds it and then has to remove it.

## Out of scope

- Deployment, backups, migrations-in-production, monitoring, or anything not about display names.
  This is not a general runbook and calling it one invites unrelated additions.
- A blocklist word list, in the document or anywhere else. The contents are the operator's
  (`ADR-0051` §5) and v0.1 ships the table empty.
- Any change to a migration, to production code, or to `CLAUDE.md`.

## Tests

`OperationsDocumentationTest`, a new class, `-PrequireDocker=true`. It locates the document the way
`HttpEndpointDocumentationTest` locates `docs/protocol.md`: walk up from `File("")` until
`docs/operations.md` is found, and `error(...)` if it is not — a document test that silently reads
nothing passes everything.

| Test | Proves |
| --- | --- |
| `theDocumentNamesTheTakedownCall` | The text contains `retire_display_name(` and the words *cannot be undone* (or the sentence the document uses to say it, asserted verbatim) |
| `theDocumentNamesBothBlocklistStatements` | The text contains `'BLOCKED')` and `DELETE FROM name_registry` |
| `everyFunctionTheDocumentNamesExistsInTheDatabase` | For each of `retire_display_name` and `normalize`, `SELECT count(*) FROM pg_proc WHERE proname = ?` against a freshly migrated database is at least `1`. **This is what stops the document drifting from the schema in silence** — `ADR-0051` rejected a hand-written procedure precisely because *"nothing tests a document"*, and this is the smallest thing that does. Assert the list of names checked is non-empty first, so a refactor that empties it fails rather than passes |
| `theDocumentDoesNotDescribeAnEndpointOrATask` | The text contains none of `POST /api`, `PUT /api`, `./gradlew` or `curl`. `ADR-0051` §4 refuses all three paths, and a document that shows one is how a second operator path gets built |

## Acceptance criteria

- [ ] `OperationsDocumentationTest.theDocumentNamesTheTakedownCall` passes
- [ ] `OperationsDocumentationTest.theDocumentNamesBothBlocklistStatements` passes
- [ ] `OperationsDocumentationTest.everyFunctionTheDocumentNamesExistsInTheDatabase` passes and
      asserts its list of names is non-empty before querying
- [ ] `OperationsDocumentationTest.theDocumentDoesNotDescribeAnEndpointOrATask` passes
- [ ] `docs/operations.md` exists and contains all four SQL snippets above
- [ ] `CLAUDE.md`, every migration and every file under `poker-server/src/main/kotlin` are unmodified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
