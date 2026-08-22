---
schema: 2
id: TASK-050311
title: The self line sits above the rows, and states a standing for a player on no page drawn
type: task
status: ready
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, leaderboard, ui, self-standing]
depends_on: [TASK-050310]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states a standing for a player whose row is on no page drawn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the self line under the season name and over the first row'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders no self line for a response that carried none, and the ladder renders anyway'
  - cd web-client && npm run check
---

## Goal

A reader is told where they stand before they read anybody else's row — from the field the response
carried, on a page their own row is nowhere near.

## The trap this ticket owns

`ADR-0065` §8: the line is *"rendered from the field the response carried, never derived by matching
the player's id against the rows on screen"* — because matching *"would be wrong on every page the
player is not on, which is nearly all of them"*. The fixture below therefore holds a self standing
that **appears on no row**, so a value echoed out of the page cannot produce it. The other half of
the guarantee is structural and already merged: `SelfStanding` carries no player id
(`TASK-050302`), so there is nothing to match with.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | modify — one element, between the season name and the list |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050307`, `TASK-050308` or `TASK-050309` changes |

## Scope

- `LadderScreen` renders `{state.self !== null && <p>{selfLine(state.self)}</p>}`, **below the season
  name and above the `<ul>`** (`ADR-0065` §1). One element, or none.
- The component **branches once**, on `state.self !== null`. Which of the two sentences a non-null
  standing gets is `selfLine`'s and is not re-decided here (`TASK-050310`).
- The component reads `state.self` and `state.rows` and never compares them. No `find`, no
  `filter`, no `some`, no `===` between a row and the standing.
- `state.self` is the value the **first** page of the walk carried (`TASK-050306`), so the line does
  not move as pages are appended. `TASK-050312` asserts that through the screen.

## Out of scope

- **Marking, highlighting or scrolling to the reader's row** — `ADR-0065` §5, and `TASK-050313`
  asserts the absence.
- **A ladder total, a movement line, a streak, or a tie count** — `ADR-0065` §7, same ticket.
- **A rank, a season standing or a season name on the profile strip** — `ADR-0065` §2.
  `ProfileStrip.tsx` is not opened by this story.
- **Treating the reader appearing twice as a defect** — `ADR-0065` §6. A player whose row is on the
  page they are looking at appears once in the self line and once in the list, and that is correct.

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, same `describe`, three new tests.

| Test | Proves |
| --- | --- |
| `states a standing for a player whose row is on no page drawn` | **Two responses, same four rows, different self standings.** The rows carry ranks `[1, 1, 3, 4]` and coins `[2, 2, 1, 0]`. With `self: { rank: 215, coins: -1 }` the screen shows `"You are rank 215 this season, on −1 duel coins."`; with `self: { rank: 7, coins: 4 }` it shows `"You are rank 7 this season, on 4 duel coins."`. In both, **no `<li>` text contains the self line's rank** — neither number is on any row, so the line cannot be an echo of the page — and two fixtures are what tell a rendered field from a hardcoded string (`ADR-0065` §1) |
| `puts the self line under the season name and over the first row` | On the same render, `section.textContent.indexOf` of the season string, of the self sentence, and of the first row's text are strictly increasing. `ADR-0065` §1 places it, and a line under the list is a different product |
| `renders no self line for a response that carried none, and the ladder renders anyway` | Two renders. With `self: null`: the section holds the heading, the season name and all four rows, and neither the placed sentence nor `NO_PLACE_THIS_SEASON` appears anywhere — this is the ordinary state of a first visit, not an error and not a spinner. With `self: { rank: null, coins: null }`: `NO_PLACE_THIS_SEASON` is on screen and the rows are unchanged. Two inputs, because a screen that rendered nothing for both would pass the first alone |

## Acceptance criteria

- [ ] `states a standing for a player whose row is on no page drawn` passes for **both** self
      standings, including the assertion that no row carries the rank the line states — deriving the
      line from any row reddens it, and hardcoding either sentence reddens the other input
- [ ] `puts the self line under the season name and over the first row` passes — moving the element
      below the `<ul>` reddens it
- [ ] `renders no self line for a response that carried none, and the ladder renders anyway` passes
      for both inputs — rendering `NO_PLACE_THIS_SEASON` when `self` is `null` reddens it, and so
      does rendering nothing when `self` is two nulls
- [ ] `grep -cE '\.find\(|\.some\(|\.filter\(' web-client/src/ladder/LadderScreen.tsx` returns `0`
- [ ] Every test `TASK-050307`, `TASK-050308` and `TASK-050309` wrote still passes, with no
      assertion in any of them edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
