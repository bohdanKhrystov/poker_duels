---
schema: 2
id: TASK-050309
title: The screen names the season the response carried, and its four states are four sentences
type: task
status: ready
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, leaderboard, ui, season, copy]
depends_on: [TASK-050308]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the season the response carried, and a different one for a different response'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders an empty ladder as an empty ladder that still names its season'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the ladder is loading before the first page answers'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a read that failed from a ladder that is empty'
  - cd web-client && npm run check
---

## Goal

A reader can see which season they are looking at, in ordinary English, taken from the response —
and the three states that are not a page of rows each say what they are.

## The trap this ticket owns

`ADR-0061` §6 and `ADR-0002`: the client never works the season out from the browser's clock. One
fixture cannot catch that — in August 2026 a clock-reading client prints `August 2026` and passes.
**Two responses naming different seasons**, one of them years away from now, is the only shape that
does.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-text.ts` | modify — three sentences |
| `web-client/src/ladder/LadderScreen.tsx` | modify — the season line, and the state sentence |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050307` or `TASK-050308` changes |

## Scope

- `ladder-text.ts` gains three golden strings:

  ```ts
  export const LOADING_LADDER = "Loading the leaderboard…";
  export const EMPTY_LADDER = "No duels have finished this season yet.";
  export const LADDER_FAILED = "The leaderboard did not load. Reload the page to try again.";
  ```

  The ellipsis in `LOADING_LADDER` is U+2026, as `LOADING_RECORD` uses it. They are asserted through
  the screen tests below rather than in `ladder-text.test.ts`, which is not in this ticket's budget.
- `LadderScreen` renders `{seasonName(state.season)}` in its own `<p>`, **directly under the heading
  and above the `<ul>`**, whenever `state.season !== null`. Before the first page answers there is no
  season to name and no element for it.
- One sentence element below the list, chosen the way `HistoryScreen` chooses its own:
  - `loading` → `LOADING_LADDER`, whether or not rows are already held;
  - `failed` → `LADDER_FAILED`, with any rows already read still on screen;
  - `ready` with no rows → `EMPTY_LADDER`;
  - `ready` with rows → no sentence at all.
- **An empty ladder is not an error, not a spinner and not a special case.** It is the routine state
  of the first day of every season (`ADR-0061` §4, `ADR-0064` §6): the heading, the season name, an
  empty list and one sentence.
- The `<ul>` stays rendered in every state, as `TASK-050307` set it.

## Out of scope

- **A *the season is still young* affordance, or any softening of a thin ladder** — `ADR-0064` §5's
  alternative 5 refused exactly that, as *"the kind of eligibility rule `ADR-0063` refused"*
  relocated to the whole screen. `TASK-050313` asserts its absence.
- **Retrying a failed read**, on a timer or on a control. The sentence tells the player to reload.
- **A season selector, a *last season* line, or naming any season but the one served** —
  `ADR-0061` §7, and `DEC-060` is where a finished season is revisited.
- **The self line** — `TASK-050311`. It sits between the season name and the list when it lands.

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, same `describe`, four new tests.

| Test | Proves |
| --- | --- |
| `names the season the response carried, and a different one for a different response` | Two renders. A page carrying `season: "2026-08"` shows `August 2026`; a page carrying `season: "2019-02"` shows `February 2019`. **The first alone would pass for a client reading the browser's clock while this is written; the second is why there are two.** Both assert the exact string, and neither asserts the wire form `2026-08` appears anywhere |
| `renders an empty ladder as an empty ladder that still names its season` | A page with `rows: []`, `season: "2026-09"`, `nextCursor: null`: the heading is there, `September 2026` is there, the list holds zero `<li>`, `EMPTY_LADDER` is there, and `LADDER_FAILED` is not |
| `says the ladder is loading before the first page answers` | A `read` whose promise is not yet settled: `LOADING_LADDER` is on screen, `EMPTY_LADDER` is not, and no season is named. The two empty-looking states are asserted apart, which is the whole point of having two sentences |
| `tells a read that failed from a ladder that is empty` | A `read` answering `{ kind: "unavailable" }`: `LADDER_FAILED` is on screen and `EMPTY_LADDER` is not |

## Acceptance criteria

- [ ] `names the season the response carried, and a different one for a different response` passes
      for both — hardcoding either string, or deriving the month from `new Date()`, reddens it
- [ ] `renders an empty ladder as an empty ladder that still names its season` passes — suppressing
      the season line when there are no rows reddens it
- [ ] `says the ladder is loading before the first page answers` passes — collapsing `loading` and
      the empty `ready` state onto one sentence reddens it
- [ ] `tells a read that failed from a ladder that is empty` passes — answering `EMPTY_LADDER` for a
      failed read reddens it
- [ ] `LOADING_LADDER`, `EMPTY_LADDER` and `LADDER_FAILED` are three different strings, and each is
      asserted literally in at least one test
- [ ] Every test `TASK-050307` and `TASK-050308` wrote still passes, with no assertion in either
      edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
