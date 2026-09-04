---
schema: 2
id: TASK-131109
title: A look-away takes nothing with it, and a frame that seats a duel overrules it
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, routing, lobby]
depends_on: [TASK-131108]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 88) }'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two promises `ADR-0112` §3 attaches to a look-away — **nothing about the room moves**, and a
frame that seats a running duel **overrules the chosen screen** — become gates instead of prose.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §§3–4 and
`docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §7's *Honoured* and
*Overruled* bullets. Nothing else — **no production file is opened or changed by this ticket.**

## Scope

- Two `it` blocks, both built on a store fed `RoomJoined` alone and a `forgetRoom` spy, which
  `renderLobby` already returns.
- The second test is the one `ADR-0112` §4 asks for and it must arrive by a frame, not by a click:
  a `Snapshot` applied inside `act()` while the ladder is on screen.

## Out of scope

- **Any production change.** A red test here is `TASK-131105`'s or `TASK-131108`'s defect and is a
  new ticket.
- **The socket.** There is none in this file, and `forgetRoom` never touches the open one anyway —
  `boot.ts` says so in as many words: the memory is about the next socket, never the current one. So
  *the socket stays open* is asserted here as *`forgetRoom` was not called*, which is the only part
  of it a component test can see. Say that in the test's comment rather than claiming more.
- **A standing rematch offer.** `ADR-0112` §4 says an offer seats nothing and overrules nothing;
  the merged tests `takes a rematch offer restated after the rejoins DuelFinished` and
  `takes no offer that arrived before the DuelFinished` already stand over it, and nothing here
  changes what they assert.

## Tests

| Test | Proves |
| --- | --- |
| `leaves the seat and the tab's room memory alone while the player looks at the ladder` | `RoomJoined` alone, address `#/leaderboard`, render → the ladder is on screen, `store.getState().roomCode` still reads `"ABCDEFGH"`, `store.getState().mySeat` still reads `0`, and the `forgetRoom` spy was **never called**. `ADR-0112` §3: the seat is kept, the tab's memory is kept, and nothing about the room moves |
| `pulls the player to the table when a frame seats a duel under a chosen screen` | the same tree, then `act(() => store.apply(SNAPSHOT))` → `Pot 30` is on screen, the ladder's *Back* is gone, and `window.location.hash` reads `""`. `ADR-0112` §4 through the same predicate: `standing` moved to `running`, so `ruling` moved to `refuse`, and that is the one line that restores the address |

The second test's three assertions are deliberately not one: a mechanism that swapped the screen but
left `#/leaderboard` standing would pass the first two and is exactly the disagreement `ADR-0112`
§2 exists to prevent.

## Acceptance criteria

- [ ] Both tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at least
      88 tests
- [ ] The second test's frame is applied inside `act()`, with no click and no address assignment —
      the input is a frame and nothing else
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
