---
schema: 2
id: TASK-131108
title: The record and the ladder move above the room
type: task
status: ready
parent: STORY-1311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-131107]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 86) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - awk '/if \(shown === "duels"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - awk '/if \(shown === "leaderboard"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qF "if (screen === \"duels\"" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "if (screen === \"leaderboard\"" web-client/src/lobby/Lobby.tsx'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A player holding a `WAITING` or `FINISHED` room can read the record and the ladder, and the address
names the screen they are on — the confinement `ADR-0073` §3 was already promising against.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §2 and
`docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §3. Nothing else.

## Scope

- Move the **contiguous block** holding the `duels` and `leaderboard` branches — both comment blocks
  and both `if` statements — from below `if (state.roomCode !== null)` to sit with the mailed
  branches already above `if (state.outcome !== null)`. A cut and a paste: nothing inside either
  branch's JSX changes.
- Change both conditions from `screen ===` to `shown ===`, keeping each branch's existing
  read fallback (`&& read !== null`, `&& readLadder !== null`) exactly as it is. `ADR-0114` §2 says
  what that fallback now means and it is worth one comment line: a player whose read is unavailable
  falls through to the room they hold rather than to a lobby that pretends they hold nothing.
- Move the stale comment with them. `"A player is not in a duel (view is null and roomCode is null)"`
  is no longer true of this position and must not survive the move — a comment that lies is worse
  than none.

## Out of scope

- **`account` and `sign-in`.** `TASK-131110` and `TASK-131111`.
- **The look-away's own proofs** — that the seat, the tab's memory and the socket are untouched, and
  that a frame arriving while the player is elsewhere still applies. `TASK-131109`, a test-only
  ticket, so the move and the guarantees are separately reviewable.
- **Redesigning either screen, or adding a way back.** Each already renders its own *Back* here, by
  the swap, and `LadderScreen`/`HistoryScreen` know nothing about navigation (`ADR-0060` §4).
  `ADR-0112` is explicit: no new control exists and none is drawn.

## Tests

Two more `it` blocks in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `shows the ladder over a room that is still waiting, and the address keeps naming it` | store fed `RoomJoined` alone; address `#/leaderboard`; render → the ladder screen is on screen (its own *Back* button is present) and `window.location.hash` still reads `"#/leaderboard"` |
| `shows the record over a room whose duel has finished` | store fed `RoomJoined` + `Snapshot` + `DuelFinished`; address `#/duels`; render → `historyRead` **was** called and the record's *Back* is on screen, and the result screen's own verdict line is **not** |

The second test's `historyRead` assertion is the sharp one: the settled DOM alone cannot tell a
correct first pass from a wrong one this ticket's own effect corrects, and the record's fetch firing
is the witness a wrong branch order leaves behind. That is the same trap the merged test
`shows the duel to a player a frame seats, whatever address they were reading` documents, read from
the other side.

**These merged tests must still pass unchanged, and none of their assertions moves:**
`leaves the address alone while no frame has seated anybody` (no frames, so `standing` is `"none"`
and `shown` is `"duels"` — the record renders exactly as before) and
`shows the duel to a player a frame seats, whatever address they were reading` (a running duel
refuses, `shown` is `"first"`, and `historyRead` is still never called). In `App.test.tsx`,
`does not offer the door while a duel is in progress` and
`does not offer the ladder door while a duel is in progress` likewise stand.

## Acceptance criteria

- [ ] Both tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at least
      86 tests
- [ ] Both moved `if` lines appear before `if (state.outcome !== null)` — the two `awk` order gates
      are the check
- [ ] Neither `if (screen === "duels"` nor `if (screen === "leaderboard"` remains in the file
- [ ] `App.test.tsx` still reports at least 36 tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
