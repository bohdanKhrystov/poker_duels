---
schema: 2
id: TASK-050306
title: The season and the self standing are the first page's, and later pages do not move them
type: task
status: backlog
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, leaderboard, state, self-standing]
depends_on: [TASK-050305]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the season and the self standing the first page carried'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'does not move the self standing when a later page carries a different one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the season and the self standing of a page that carried no cursor'
  - cd web-client && npm run check
---

## Goal

The walk holds the season it is showing and the reader's own standing, both taken from the page
that opened the walk, and walking further does not disturb either.

## Why a later page must not overwrite

`ADR-0065` §1: the self line *"does not change as the player walks pages"*, and §3: the standing is
*"required on the request that opens the ladder — the one with no cursor — and is **not** required
on later pages. The screen keeps the one it was given while it walks."* A reducer that assigns
`event.page.self` on every page is correct on the first page and quietly wrong from the second
onward — and if a later page ever answers `null`, it would blank a line the reader was already
reading.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-state.ts` | modify — two fields, and the branch in the `page` case |
| `web-client/src/ladder/ladder-state.test.ts` | modify — **adds tests only**; no assertion written by `TASK-050305` changes |

## Scope

- `LadderState` gains `readonly season: string | null` and `readonly self: SelfStanding | null`.
  `initialLadder()` sets both to `null` — before the first page answers, the screen knows neither.
- In the `page` case, exactly one rule decides all three of `rows`, `season` and `self`: the request
  in flight carried no cursor (`askedWith === null`), or it did.
  - carried no cursor → `rows`, `season` and `self` all come from the page.
  - carried a cursor → rows append, and `season` and `self` are **kept**, whatever the page carried.
- `failed` still touches none of them.
- `season` stays the wire string (`"2026-08"`). It is spelled out by `seasonName` at the point of
  render, not here.

## Out of scope

- **Rendering the self line, or deciding its words** — `TASK-050310` and `TASK-050311`.
- **Matching the reader against a row.** There is nothing to match with: `SelfStanding` carries no
  player id (`TASK-050302`).
- **A season selector, or holding a second season** — `ADR-0061` §7: this screen shows one season.

## Tests

`web-client/src/ladder/ladder-state.test.ts`, same `describe`, three new tests.

| Test | Proves |
| --- | --- |
| `takes the season and the self standing of a page that carried no cursor` | From `initialLadder()`, `asked` with `after: null` then a page with `season: "2026-08"` and `self: { rank: 5, coins: 1 }`: state holds both |
| `keeps the season and the self standing the first page carried` | Continue that walk: `asked` with `after: "c1"` then a page whose `season` is `"2026-09"` and whose `self` is `{ rank: 9, coins: -4 }`. State still reads `"2026-08"` and `{ rank: 5, coins: 1 }`, and the rows appended |
| `does not move the self standing when a later page carries a different one` | The same walk, but the second page carries `self: null`: state still holds `{ rank: 5, coins: 1 }`. The two together are two inputs — one where a later page carries a **different** standing and one where it carries **none** — and a reducer that assigns unconditionally reddens on both |

## Acceptance criteria

- [ ] `takes the season and the self standing of a page that carried no cursor` passes
- [ ] `keeps the season and the self standing the first page carried` passes — mutating the `page`
      case to `season: event.page.season, self: event.page.self` unconditionally reddens it
- [ ] `does not move the self standing when a later page carries a different one` passes — the same
      mutation reddens it, and by blanking rather than by changing, which is the worse failure
- [ ] Every test `TASK-050305` wrote still passes, with no assertion in it edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
