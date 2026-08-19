---
schema: 2
id: TASK-041314
title: No player id reaches the history screen, and the suite counts itself
type: task
status: backlog
parent: STORY-0413
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, history, identity, guard]
depends_on: [TASK-041313]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no player id on the history screen, named opponent or nameless'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no player id to the server across a whole walk'
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Test Files +70 passed \(70\)'
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +472 passed \(472\)'
  - cd web-client && npm run check
---

## Goal

The rule `STORY-0413` inherits from `ADR-0058` and `ADR-0029` §6 — nothing a player reads on the
history screen, and nothing the screen sends, is built from a player id — is an assertion in the
suite rather than a promise in a document; and the suite states its own size.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-no-derivation.test.tsx` | modify — one describe block, two tests |

Read, not edited: `web-client/src/history/HistoryScreen.tsx`,
`web-client/src/profile/profile-fixture.ts`,
[`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) §2.

## Scope

- A new describe block, `"the history screen's surface"`, in the file that already owns this rule for
  the strip. It goes **here rather than in a new file** so that `allContentOnScreen` — which reads
  text nodes, `aria-label`, `title` **and every attribute value**, because a value a client worked
  out for itself does not have to be printed to reach a player — is reused rather than copied. A
  second copy is a second thing to keep true, which is the argument `nameOrNone` already won.
- Both tests render `HistoryScreen` with a `read` built from `duelRowBody`, so every row carries a
  real `opponentPlayerId` distinct from every other string in the fixture and the scan has something
  to catch.
- **No new production code.** If either test fails, the bug belongs to `TASK-041308` or
  `TASK-041311` and is fixed there.
- **The suite's own size is asserted here and in no other ticket of this story.** One place, so that
  a test added or dropped anywhere in the story is one correction rather than four. The arithmetic:

  | | Tests |
  | --- | --- |
  | merged before `STORY-0413` | 419 |
  | `TASK-041301` | +7 |
  | `TASK-041302` | +6 |
  | `TASK-041303` | +0 |
  | `TASK-041304` | +4 |
  | `TASK-041305` | +3 |
  | `TASK-041306` | +7 |
  | `TASK-041307` | +3 |
  | `TASK-041308` | +4 |
  | `TASK-041309` | +3 |
  | `TASK-041310` | +4 |
  | `TASK-041311` | +3 |
  | `TASK-041312` | +4 (three written at the split, one named by `DEC-052`'s ADR) |
  | `TASK-041313` | +3 (two written at the split, one named by `DEC-053`'s ADR) |
  | this ticket | +2 |
  | | **472** |

  Five test files are new — `duels-query`, `duel-page`, `history-text`, `history-state`,
  `HistoryScreen` — so **70** files. If the suite reports another number, an earlier ticket landed a
  count its own Tests table did not name, or an unblocked ticket added a file: say which in the PR
  and correct the two numbers here in the same PR. Correcting them silently is the thing this
  arrangement exists to prevent.

## Out of scope

- A second `nameOrNone` sole-decision-point sweep. **A refusal, not an omission:** the merged one in
  this file walks every `.ts`/`.tsx` under `src/`, so `src/history/` is already covered — and this
  ticket asserts that the walk really reaches it rather than assuming so.
- Asserting that a removed name and a never-set name render identically on a history row. **A
  refusal, not an omission:** `DuelSummaryResponse` carries nothing that could tell them apart, so
  the test would be vacuous — it would pass against any implementation, including a bad one. The
  wire-level version is `TASK-041020`'s and the strip-level version is `TASK-041117`'s.
- The engine's and the server's own no-derivation guards.
- Anything about the player's *own* name. This file is about what is derived, not about what a player
  is entitled to be told.

## Tests

`web-client/src/profile/profile-no-derivation.test.tsx`, describe block
`"the history screen's surface"`.

| Test | Proves |
| --- | --- |
| `puts no player id on the history screen, named opponent or nameless` | One read whose two rows carry `opponentPlayerId: "player-81"` with `opponentDisplayName: "Ada"`, and `opponentPlayerId: "player-82"` with `opponentDisplayName: null`. `allContentOnScreen` finds neither id, finds `Ada`, and finds `No name` — the two sanity assertions being what prove the scan is looking. It also asserts `clientSources()` contains `history/HistoryScreen.tsx`, so the merged sole-decision sweep in this file is known to reach the new directory rather than assumed to. Fails against a row that falls back to an id for a nameless opponent, and against a scan that reads text nodes only |
| `sends no player id to the server across a whole walk` | A walk that reads a first page, asks for a second with its cursor, and then narrows to an outcome: every path handed to `read` is asserted to contain no `player-`, and the test asserts it collected **more than two** paths first, so a sweep that recorded nothing cannot pass by saying nothing. Fails against a client that correlates on an id it was told to drop — `TASK-041105` drops it at the parse, and this is what keeps that cheap to keep true |

Two tests added.

## Acceptance criteria

- [ ] `the history screen's surface > puts no player id on the history screen, named opponent or
      nameless` passes, including the assertion that the source walk reaches `history/`
- [ ] `the history screen's surface > sends no player id to the server across a whole walk` passes,
      and asserts the collected path count before asserting anything about the paths
- [ ] The four merged tests in `profile-no-derivation.test.tsx` pass unchanged
- [ ] `npm run --silent test` reports `Test Files  70 passed (70)`
- [ ] `npm run --silent test` reports `Tests  472 passed (472)`
- [ ] No file outside `web-client/src/profile/profile-no-derivation.test.tsx` differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
