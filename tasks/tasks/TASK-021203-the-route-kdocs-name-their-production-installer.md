---
schema: 2
id: TASK-021203
title: Both route KDocs name their production installer instead of a story that has landed
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, docs]
depends_on: [TASK-021202]
verify:
  - grep -c 'the only caller is still a test' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt | grep -qx 0
  - grep -c 'the only caller is still a test' poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt | grep -qx 0
  - grep -q 'duelServer' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt
  - grep -q 'duelServer' poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Two KDoc paragraphs stop claiming that only a test installs these routes, which stopped being true
when `TASK-021202` merged.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |

Read, do not modify: `Application.kt` (`duelServer`).

## Scope

- In `duelSocket`'s KDoc, replace the paragraph beginning *"A shipping
  `duels.poker.server.db.PostgresPlayerDirectory` now exists…"* with one sentence saying the route
  is installed in production by `Application.duelServer`, which holds the `DataSource` the real
  collaborators are built from, and that a test may still install it directly with collaborators of
  its own.
- In `profileRoutes`'s KDoc, replace the sentence beginning *"It is installed by the caller rather
  than from `Application.module()`…"* with the same fact, in the same terms.
- Keep every other sentence in both KDocs — in particular `profileRoutes`'s paragraph on why this
  route holds no `PlayerDirectory` (`ADR-0012`), which is unrelated and still true.

## Out of scope

- Any change to code. This ticket edits comments only; not one statement moves.
- `docs/protocol.md` and `docs/architecture.md` — neither makes this claim.

## Tests

None. This ticket adds no behaviour, so it adds no test; the `verify` greps assert the exact claim
that was wrong is gone from both files and that both now name `duelServer`, and
`:poker-server:check` proves the two files still compile and still pass ktlint and detekt.

## Acceptance criteria

- [ ] Neither file contains the string `the only caller is still a test`
- [ ] Both files name `duelServer` in the KDoc of the function they declare
- [ ] `git diff` for this ticket changes only comment lines
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
