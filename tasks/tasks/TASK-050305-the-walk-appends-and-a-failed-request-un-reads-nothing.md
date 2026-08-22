---
schema: 2
id: TASK-050305
title: The ladder walk is a reducer that appends, and a failed request un-reads nothing
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, leaderboard, state]
depends_on: [TASK-050304]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'appends the next page under the rows it already holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the rows when the page answers a request that carried no cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'un-reads nothing when a request fails'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no next page when the server named none'
  - cd web-client && npm run check
---

## Goal

The ladder screen has somewhere to keep a walk: rows accumulate in the order they arrived, a first
page replaces and a next page appends, and a failure loses nothing already read.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-state.ts` | create |
| `web-client/src/ladder/ladder-state.test.ts` | create |

Read, not edited: `web-client/src/history/history-state.ts` — the same shape, including why
`askedWith` has to exist: the reducer cannot ask the component what kind of request came back, and
a `page` event carries no memory of what was asked.

## Scope

- Exports:

  ```ts
  export type LadderPhase = "loading" | "ready" | "failed";

  export interface LadderState {
    readonly rows: readonly LadderRow[];
    readonly nextCursor: string | null;
    readonly askedWith: string | null;
    readonly phase: LadderPhase;
  }

  export type LadderEvent =
    | { readonly type: "asked"; readonly after: string | null }
    | { readonly type: "page"; readonly page: LadderPage }
    | { readonly type: "failed" };

  export function initialLadder(): LadderState;
  export function ladderReducer(state: LadderState, event: LadderEvent): LadderState;
  export function nextPageAfter(state: LadderState): string | null;
  ```

- `initialLadder()` is `loading`, no rows, no cursor, nothing outstanding.
- `asked` records `after` in `askedWith` and sets `phase` to `loading`.
- `page` appends `event.page.rows` **under** the rows held when `askedWith !== null`, and replaces
  them when `askedWith === null`. It always takes `event.page.nextCursor`, clears `askedWith` and
  sets `phase` to `ready`.
- **Nothing here sorts, reverses, de-duplicates, re-keys or renumbers.** Rows are spread in the
  order the server sent them. A repeated rank across a page boundary is ordinary and is not a
  duplicate to filter (`ADR-0064` §2).
- `failed` sets `phase` to `failed` and clears `askedWith`. It touches neither `rows` nor
  `nextCursor`: a request that fails does not un-read the pages already read.
- `nextPageAfter(state)` is `state.nextCursor` — `null` when the walk has nowhere left to go. The
  cursor is passed through untouched.
- **No assertion in the test file deep-equals a whole `LadderState`.** `TASK-050306` adds two
  fields to this type and must not have to rewrite an assertion here to do it.

## Out of scope

- **The season and the reader's own standing** — `TASK-050306` adds both to this state, with the
  rule that a later page does not move them. Until then `LadderState` has four fields.
- **Fetching anything** — this module is pure and imports no transport.
- **Rendering anything** — no React import.
- **A filter, a search or an outcome axis.** The ladder is narrowed by nothing (`ADR-0063`).

## Tests

`web-client/src/ladder/ladder-state.test.ts`, `describe("the ladder walk")`. Build `LadderPage`
values with a small local helper; two rows a page is enough.

| Test | Proves |
| --- | --- |
| `appends the next page under the rows it already holds` | From a state holding rows ranked `[1, 1]`, dispatch `asked` with `after: "c1"` then a `page` of rows ranked `[1, 5]`: `rows.map((r) => r.rank)` is `[1, 1, 1, 5]`, in that order. The repeat across the boundary survives, and nothing is dropped for looking like a duplicate |
| `replaces the rows when the page answers a request that carried no cursor` | From the same state, dispatch `asked` with `after: null` then a `page` of two rows: exactly those two rows remain. Two inputs with the one above, which is what tells an append rule from an unconditional spread |
| `un-reads nothing when a request fails` | From a state holding two rows and a `nextCursor`, dispatch `failed`: `rows` and `nextCursor` are unchanged, `phase` is `failed`, `askedWith` is `null` |
| `offers no next page when the server named none` | `nextPageAfter` answers the cursor a page carried, and `null` after a page whose `nextCursor` was `null` |

## Acceptance criteria

- [ ] `appends the next page under the rows it already holds` passes with the boundary repeat
      intact — mutating the `page` case to replace unconditionally reddens it
- [ ] `replaces the rows when the page answers a request that carried no cursor` passes — mutating
      the `page` case to append unconditionally reddens it
- [ ] `un-reads nothing when a request fails` passes — mutating `failed` to clear `rows` reddens it
- [ ] `offers no next page when the server named none` passes — mutating `nextPageAfter` to answer a
      non-null cursor at the end of the walk reddens it
- [ ] `grep -cE '\.sort\(|\.reverse\(|new Set|indexOf' web-client/src/ladder/ladder-state.ts`
      returns `0`
- [ ] No assertion in `ladder-state.test.ts` compares a whole `LadderState` with `toEqual`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
