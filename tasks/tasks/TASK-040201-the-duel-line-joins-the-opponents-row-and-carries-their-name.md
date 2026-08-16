---
schema: 2
id: TASK-040201
title: The duel line joins the opponent's row and carries their name
type: task
status: done
parent: STORY-0402
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, http, read-path, identity]
depends_on: [TASK-040118]
verify:
  - grep -q 'val opponentDisplayName: String?' poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt
  - grep -q 'JOIN player p ON p.id = o.player_id' poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`DuelSummaryResponse` gains `opponentDisplayName: String?`, and `RECENT_DUELS_SQL` fills it from one
more join rather than from a placeholder.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify |
| `poker-server/src/main/resources/db/migration/V1__initial_schema.sql` | read — `duel_result.player_id` is `NOT NULL REFERENCES player (id)`, which is why the new join is an inner one |

**Three files, and it really is three.** `TASK-040107` landed `ProfileDtoFixtures.kt` for exactly
this moment: every test-side `DuelSummaryResponse` already goes through the builder, so a defaulted
parameter there keeps every call site compiling and no test file enters this ticket's budget. That
is the whole reason four `STORY-0401` tickets needed a fourth file and this one does not.

## Scope

- `DuelSummaryResponse` gains `val opponentDisplayName: String?` — **no default value**.
  `Application.module()` installs `ContentNegotiation { json() }`, whose `Json` has
  `encodeDefaults = false`; a defaulted property would be *absent* from the real response body
  instead of present as `null`. `TASK-040204` is the test that catches that, but the rule belongs
  here.
- Its KDoc says `null` means the opponent never set a name, that the name is read at request time so
  a name set later relabels an older line (`ADR-0021`), and that the server fabricates no
  placeholder (`ADR-0029` §6).
- **Correct the stale sentence in `opponentPlayerId`'s KDoc.** It currently says *"`DEC-016` asks
  whether a human-readable name is added later; adding one would be a new field, not a change to
  this one."* `DEC-016` is answered by `ADR-0021`, and this ticket is that new field. Replace the
  sentence with the split `ADR-0021` states: the id is the stable identity a client correlates on,
  `opponentDisplayName` is the label, and both travel. Do not remove or rename `opponentPlayerId`.
- `RECENT_DUELS_SQL` gains one join and one selected column, and stays a single statement:

  ```sql
  SELECT d.id AS duel_id,
         o.player_id AS opponent_id,
         p.display_name AS opponent_display_name,
         r.coin_delta AS coin_delta,
         d.finished_at AS finished_at,
         d.hands_played AS hands_played
  FROM duel_result r
  JOIN duel d ON d.id = r.duel_id
  JOIN duel_result o ON o.duel_id = r.duel_id AND o.player_id <> r.player_id
  JOIN player p ON p.id = o.player_id
  WHERE r.player_id = ?
  ORDER BY d.finished_at DESC, d.id DESC
  LIMIT ?
  ```

- **An inner join, with a comment saying why**, next to the `ADR-0015` comment already there:
  `duel_result.player_id` is `NOT NULL REFERENCES player (id)` in `V1`, so exactly one `player` row
  matches every opponent result row. The join can neither drop a duel nor duplicate one. A
  `LEFT JOIN` would add nothing except somewhere for a broken foreign key to hide.
- `recentDuelsOf` passes `opponentDisplayName = rows.getString("opponent_display_name")`.
  `ResultSet.getString` answers `null` for a SQL `NULL`, which is exactly the wanted value: **no
  `?: ""`, no `orEmpty()`, no substitute of any kind.**
- The `duelSummaryResponse` builder gains `opponentDisplayName: String? = null` as its last
  parameter. This is the one parameter with a default, and the reason is that most tests are not
  about the name; any test that asserts the name passes it explicitly (`TASK-040204`).

## Out of scope

- Any new test. The gate here is that five existing suites still pass with the field threaded
  through; `TASK-040202`, `TASK-040203` and `TASK-040204` are the proof and land immediately after.
- `docs/protocol.md` — `TASK-040205`, which owns the document and its test together. Documenting the
  field before it exists would fail `HttpEndpointDocumentationTest.theDocumentedFieldNamesAllExist`,
  which is why that ticket comes last rather than first.
- Anything under `web-client/`. `TASK-031103` drops `opponentPlayerId` at the client parse and builds
  `RecentDuel` field by field from five named keys, ignoring every other key, so a seventh key on the
  wire changes nothing there and no client test moves. `STORY-0411` owns adding the name to the
  client type.
- Paging, filtering, search — `STORY-0408`, `STORY-0409`.

## Tests

No new tests in this ticket; it is the widening, and its gate is that the existing suites still pass.
One of them is already an end-to-end presence check worth naming:
`ProfileEndpointsDatabaseTest.aDuelThatJustFinishedAppearsInTheList` decodes the **real response
body** with `protocolJson`, which has `ignoreUnknownKeys = false`. A field with no default that the
route failed to emit makes that decode throw `MissingFieldException`, so the failure is loud rather
than quiet.

## Acceptance criteria

- [ ] `DuelSummaryResponse.opponentDisplayName` is `String?` with **no** default value
- [ ] `RECENT_DUELS_SQL` contains exactly one `SELECT`, and the line added to it is
      `JOIN player p ON p.id = o.player_id`
- [ ] `recentDuelsOf` reads the `opponent_display_name` label, and nothing in
      `PostgresProfileReads.kt` substitutes a value for `null` — no `?:`, no `orEmpty()`
- [ ] `opponentPlayerId` still exists on the DTO, and its KDoc no longer describes `DEC-016` as open
- [ ] Every test in `ProfileDtosTest`, `ProfileRouteTest`, `PostgresProfileReadsTest`,
      `ProfileEndpointsDatabaseTest` and `HttpEndpointDocumentationTest` passes, with no assertion
      changed, removed or weakened
- [ ] The only default value introduced anywhere in this diff is the builder's, in the test tree
- [ ] Every command in `verify:` exits 0 — the last one is `:poker-server:check`, which is what runs
      ktlint and detekt; a `--tests` filter runs neither

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
