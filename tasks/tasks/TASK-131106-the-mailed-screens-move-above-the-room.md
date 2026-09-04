---
schema: 2
id: TASK-131106
title: The two mailed screens move above the room and read shown
type: task
status: ready
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-131105]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 81) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - awk '/if \(shown === "verify"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - awk '/if \(shown === "reset"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qF "if (screen === \"verify\"" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "if (screen === \"reset\"" web-client/src/lobby/Lobby.tsx'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A mailed `verify` or `reset` link opens its screen over a room whose duel is not running, and is
withheld — token unread, address untouched — while the server has not yet said what the room is.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §§2 and 5.
Nothing else.

## Scope

- Move the **contiguous block** holding the `verify` and `reset` branches — both KDoc comment blocks
  and both `if` statements, in that order — from below `if (state.roomCode !== null)` to
  **immediately above** `if (state.outcome !== null)`. It is a cut and a paste of one block: nothing
  inside either branch's JSX changes.
- Change each moved condition from `screen ===` to `shown ===`, and nothing else:
  `if (shown === "verify" && account !== null)`, `if (shown === "reset" && account !== null)`.
- Add one line to the comment above the pair recording why they sit here: `ADR-0114` §2's order is
  what makes the rule mechanical — the only value a chosen-screen branch can test is the one
  `rulingOn` produced, so a chosen screen cannot render without consulting it.

## Out of scope

- **The other four chosen branches.** `duels` and `leaderboard` are `TASK-131108`'s, `account` is
  `TASK-131110`'s, `sign-in` is `TASK-131111`'s. Moving one contiguous block per ticket is what
  keeps each diff readable; moving more is a scope widening, not a shortcut.
- **The `hold` proof.** The three call-count assertions `ADR-0114` §7 owes are `TASK-131107`'s, a
  test-only ticket, so that the move and the proof are separately reviewable.
- **`VerifyScreen`, `ResetScreen` and the `account` provider.** Untouched — a screen knows nothing
  about navigation (`ADR-0060` §4).
- **Rearranging the two mailed branches relative to each other.** The address has one second
  segment, so only one of the two can ever be showing; their order between themselves is not a fact
  about anything.

## Tests

One more `it` block in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `opens a mailed link over a room whose duel is not running` | store fed `RoomJoined` **alone**, address `#/verify/zqx-verify-token-zqx`, an `AccountProvider` whose `verifyEmail` is a spy → the `VERIFY_HEADING` heading is on screen, `verifyEmail` was called **once** with `"zqx-verify-token-zqx"`, and `window.location.hash` reads `"#/verify"`. `ADR-0112` §§3 and 5, and `STORY-1310`'s `P6a`, which observed the opposite on the shipped tree |

`renderLobbyWithAccount` and `accountCallsFixture` already exist in this file; use them rather than
building a third fixture.

**These merged tests must still pass unchanged, and none of their assertions moves:**
`lets a frame that seats this tab outrank a mailed link` (the verify branch is now above the store
branches, and `shown` is `"first"` on a refusal, so the table still wins),
`opens a mailed verification link and sends the token behind the slug`,
`takes the token out of the address and leaves the player on the screen`,
`renders the verification screen with nothing in hand at the bare address`,
`opens a mailed reset link and sends the token behind the slug`,
`lands the player on the sign-in screen once the password is set`,
`leaves the player on the reset screen when the server refuses`, and
`reads one token for whichever mailed screen the address named`. All eight boot with no room, so
`standing` is `"none"` and `shown` is `screen`.

## Acceptance criteria

- [ ] The test above exists under that exact name and passes, and `Lobby.test.tsx` reports at least
      81 tests
- [ ] Both moved `if` lines appear **before** `if (state.outcome !== null)` in `Lobby.tsx` — the two
      `awk` order gates in `verify:` are the check
- [ ] Neither `if (screen === "verify"` nor `if (screen === "reset"` remains anywhere in the file
- [ ] `App.test.tsx` still reports at least 36 tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
