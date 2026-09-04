---
schema: 2
id: TASK-131111
title: The sign-in screen moves above the room, and the six branches are one block
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-131110]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 91) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - awk '/if \(shown === "sign-in"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - awk '{ n += gsub(/if \(screen === "/, "&") } END { exit (n != 0) }' web-client/src/lobby/Lobby.tsx
  - awk '{ n += gsub(/if \(shown === "/, "&") } END { exit (n != 6) }' web-client/src/lobby/Lobby.tsx
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The last chosen-screen branch joins the other five above the store branches, so that `ADR-0114` §2's
table is the file's actual shape and no branch in `Lobby.tsx` reads `screen` where it should read
`shown`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §2. Nothing
else.

## Scope

- Move the `sign-in` branch — its comment block and its `if` statement — up to sit with the five
  chosen branches already above `if (state.outcome !== null)`, and change its condition from
  `screen === "sign-in"` to `shown === "sign-in"`. Nothing inside its JSX changes, and
  `SignInScreenBody` at the bottom of the file is untouched.
- After this ticket the file reads, top to bottom: the six chosen branches, then `outcome`, then
  `view`, then `roomCode`, then the fall-through. That is `ADR-0114` §2's table, and the ordering
  gate in `verify:` — **six** `if (shown === "` lines and **zero** `if (screen === "` lines — is
  what will keep it that way when the next screen is added.
- Add the one comment `ADR-0114` §2 earns above the block, if it is not already there from an
  earlier move: the only value a chosen-screen branch may test is the one `rulingOn` produced, which
  is the enforcement `ADR-0076`'s own Consequences said did not exist.

## Out of scope

- **`ADR-0083` §4.** The sign-in branch reads only the address — never `signedIn` — and that does not
  change here: `shown` is derived from the address and the room's standing, and from nothing else.
- **`DEC-091`.** Whether *Back* on the sign-in screen should return to the account screen rather
  than to `/` is an open product decision and is not answered by moving a branch.
- **The fall-through.** `TASK-131112` conditions it; here it is untouched.

## Tests

One more `it` block in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `shows the sign-in screen over a room whose duel is not running` | store fed `RoomJoined` alone; address `#/sign-in`; rendered through `renderLobbyWithAccount` so `account !== null` → the `SIGN_IN_HEADING` heading is on screen, the waiting screen's *"Waiting for your rival"* is not, and `window.location.hash` still reads `"#/sign-in"` |

**These merged tests must still pass unchanged, and none of their assertions moves:**
`opens the form and closes it again without touching the address`,
`comes back to the sign-in form after a round trip through the lobby`,
`offers the way to a forgotten password under the sign-in form, refused or not`, and in
`App.test.tsx` `reaches the sign-in screen from the account screen, and comes back`,
`opens the sign-in screen at the address alone, with no click at all` and
`opens the sign-in screen to a browser that already holds a session token`. All boot with no room,
so `standing` is `"none"` and `shown` is `screen`.

## Acceptance criteria

- [ ] The test above exists under that exact name and passes, and `Lobby.test.tsx` reports at least
      91 tests
- [ ] `Lobby.tsx` holds exactly six `if (shown === "` lines and zero `if (screen === "` lines
- [ ] `if (shown === "sign-in"` appears before `if (state.outcome !== null)`
- [ ] `App.test.tsx` still reports at least 36 tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
