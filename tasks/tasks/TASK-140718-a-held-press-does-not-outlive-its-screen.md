---
schema: 2
id: TASK-140718
title: A held press does not outlive the screen it was made on
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140715]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'a chosen screen clears a press the ask was still holding' src/lobby/Lobby.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

While the name ask is showing, the player navigates to a chosen screen — *Leaderboard*, *Duels*,
*Account*, *Sign in*, *Verify* or *Reset* — and then comes back. The ask must not reappear still
holding the press they made before they left.

Clearing `heldPress` on that navigation is what this ticket adds.

## Why this is its own ticket

Found by the coder implementing `TASK-140715`, which introduced `heldPress`, and traced rather than
guessed.

`Lobby`'s six chosen-screen branches render **above** the ask branch, so navigating to one of them
does not unmount `Lobby` — it renders a different screen while `Lobby` stays mounted and `heldPress`
stays set. Nothing in `Lobby.tsx` clears it on a screen change. Returning to an address where no
chosen screen is honoured brings the ask back, still holding the original frame, with no fresh press
behind it.

**It does not fire on its own** — the player still has to answer the ask — so this is not a duel
started without a press. What is wrong is that an intent silently survives a screen it should not
still be known on, and the player's next answer commits them to something they asked for earlier and
elsewhere.

A full navigation away is already correct: the tab closing or the SPA being left unmounts `Lobby` and
React discards `heldPress` with it. Only the in-app hash-router case leaks.

`TASK-140715` is not at fault. Its `Scope` and `Tests` table cover holding the press across the ask
and releasing it on both answers; nothing there concerns navigation, and `ADR-0119` §5's *"nothing
else sets it"* list says what may **set** the held press, not when it is cleared.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§1, 5 and `web-client/src/lobby/Lobby.tsx`. **Nothing outside the table above is changed.**

## Scope

`heldPress` returns to `null` when a chosen screen is rendered.

Whether that is an effect keyed on the chosen screen, or a clear at the point navigation is
requested, is the implementer's call — but it must hold for **all six** branches, not the one a test
happens to drive.

## Out of scope

- **Changing what sets `heldPress`.** `ADR-0119` §5 owns that list and it is not opened.
- **The unmount case**, which is already correct and needs no code.
- **Any change to the ask itself** — `NameAsk.tsx` is not touched.

## Tests

| Test | What it refuses |
| --- | --- |
| `a chosen screen clears a press the ask was still holding` | an intent that survives a screen change and commits the player on their return |

The test must press to duel, reach the ask, navigate to a chosen screen **through the product's own
control**, come back, and assert that answering the ask now starts nothing — because there is no
press to release. Driving through the control rather than staging `window.location.hash` is what
`TASK-140801` had to repair in this same file.

## Acceptance criteria

1. The new test fails against `develop`'s `Lobby.tsx` and passes with the change.
2. It exercises at least two of the six branches, so a clear wired to one screen does not pass.
3. `npm run check` is green, and `Lobby.test.tsx`'s count is stated as measured, not computed.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] The new test was observed red before the fix.
- [ ] PR merged into `develop`.
