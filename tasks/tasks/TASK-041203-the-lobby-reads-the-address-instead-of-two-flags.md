---
schema: 2
id: TASK-041203
title: The lobby reads the address instead of two flags, and Back stops leaving the client
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-041202]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the first screen for the record, and comes back to it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the record at its own address, and the way back at the first one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the screen the address already names, with no click at all'
  - cd web-client && npm run check
---

## Goal

`Lobby.tsx` loses `showHistory` and `showLadder` and reads the address instead, so the record and the
leaderboard have the addresses `ADR-0076` §1 gives them and the browser's *Back* returns to the first
screen in the same document.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

`App.test.tsx` is in the budget because it holds every test of these two doors, and this ticket
changes how they are reached. Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §3, §5 and §6;
`web-client/src/routing/use-screen.ts`.

## Scope

- Delete both `useState` flags. `Lobby.tsx` calls `useScreen()` and branches on `screen` where it
  branched on `showHistory` and `showLadder`, **in the same place and in the same order**: the
  chosen screen stays after `outcome`, `view` and `roomCode` (`ADR-0076` §3).
- The two lobby buttons call `open("duels")` and `open("leaderboard")`. Their labels stay
  `HISTORY_HEADING` and `LADDER_HEADING` — the slug is a separate literal by `ADR-0076` §1's design,
  and the button keeps saying the product's word.
- The in-page *Back* control on each screen calls `leave()`, which **replaces** rather than pushes
  (`ADR-0076` §6, second row). Its label and its position do not change; `HistoryScreen` and
  `LadderScreen` are not touched at all, which is `ADR-0060` §4 paying off.
- The `read !== null` and `readLadder !== null` guards stay exactly as they are: a screen whose read
  is not provided still falls through to the first screen.

## The tests this changes, and how

Three tests in `App.test.tsx` move, and **no assertion is weakened or deleted**:

- `leaves the first screen for the record, and comes back to it` and `leaves the first screen for the
  ladder, and comes back to it` — the click now changes `window.location.hash`, and the re-render
  arrives on the queued `hashchange` rather than synchronously. Each gains `await screen.findBy…`
  where it read `screen.getBy…` **after a click**, and gains one assertion on the address at each
  step. Every existing assertion in both stays, character for character.
- `does not offer the door while a duel is in progress` and `does not offer the ladder door while a
  duel is in progress` are untouched: the doors are still absent under a `view`, for the branch-order
  reason above.
- Every other test in the file is untouched. A `beforeEach` resetting `window.location.hash` is
  added, because these tests now share an address.

## Out of scope

- **The store outranking the address when a frame seats a player mid-screen.** `ADR-0076` §3's
  *replace so the address does not lie* is `TASK-041204`, deliberately separated because it is a new
  effect rather than a rewire.
- **The account screen's address.** `TASK-041222`.
- **Routing `DuelResult`'s `<a href="/">` or the waiting screen's *Back to the lobby*.** `ADR-0076`
  §6's last row keeps both as real page loads, because routing them ships `ADR-0075`'s four-field
  presence leak. **A refusal, not an omission** — a criterion below greps for it.

## Tests

`web-client/src/App.test.tsx`, inside the existing `describe("App")`. Two new tests beside the moved
ones.

| Test | Proves |
| --- | --- |
| `puts the record at its own address, and the way back at the first one` | Click the record door: `window.location.hash` is `"#/duels"`, and the record's heading is on screen. Click the in-page *Back*: `window.location.hash` is `""` and the first screen is back. Both halves in one test, so a `leave` that renders correctly while leaving a stale address cannot pass |
| `opens the screen the address already names, with no click at all` | With `window.location.hash` set to `"#/leaderboard"` **before** the render, the ladder is on screen and the room-code form is not — the reload half of `ADR-0076`'s promise, which no click-driven test can reach |
| `leaves the first screen for the record, and comes back to it` *(moved)* | Unchanged in what it asserts; awaits the settled render after each click, and additionally asserts the address at both ends |
| `leaves the first screen for the ladder, and comes back to it` *(moved)* | The same, for the ladder |

## Acceptance criteria

- [ ] `App > puts the record at its own address, and the way back at the first one` passes
- [ ] `App > opens the screen the address already names, with no click at all` passes
- [ ] `App > leaves the first screen for the record, and comes back to it` passes, with every
      assertion it had before still present
- [ ] `App > leaves the first screen for the ladder, and comes back to it` passes, with every
      assertion it had before still present
- [ ] `App > does not offer the door while a duel is in progress` and `App > does not offer the
      ladder door while a duel is in progress` pass **unchanged**
- [ ] `grep -cE 'showHistory|showLadder' web-client/src/lobby/Lobby.tsx` returns `0`
- [ ] `grep -c 'href="/"' web-client/src/lobby/Lobby.tsx` returns `1` — the waiting screen's way
      back is still a real page load
- [ ] `git diff --stat web-client/src/history/ web-client/src/ladder/` is empty
- [ ] `npm run check` reports the same test count as `develop` plus **2**
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Make the in-page *Back* call `open` on the screen it is leaving instead of `leave()`.
   **`puts the record at its own address, and the way back at the first one` reddens** on the address
   assertion — the record stays on screen and the hash never returns to `""`. Revert.
2. Move the chosen-screen branch **above** the `view` branch in `Lobby.tsx`.
   **`does not offer the door while a duel is in progress` still passes** — the door is a control on
   the first screen and is unreachable either way — while `App > renders the lobby beneath the
   heading` and the duel-table tests in `Lobby.test.tsx` redden. Run this one: it shows that the
   branch-order rule is guarded by the duel tests and **not** by the door tests, which is the
   opposite of what the names suggest and is why `TASK-041204` exists as its own ticket.
3. Delete the `beforeEach` that resets `window.location.hash`.
   **`App > renders the application heading` and every test that follows `opens the screen the
   address already names` redden**, because the leaked address renders the ladder in place of the
   lobby. Record the count in the PR: shared mutable address is the one new hazard this rewire adds.
4. Have the record door call `open("leaderboard")`.
   **`puts the record at its own address…` reddens on both the address and the heading**, and
   `leaves the first screen for the record` reddens with it. Two tests, which is what tells a
   copy-paste slug apart from a broken navigation.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
