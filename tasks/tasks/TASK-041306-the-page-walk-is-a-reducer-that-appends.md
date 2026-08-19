---
schema: 2
id: TASK-041306
title: The page walk is a reducer that appends, and never sorts
type: task
status: backlog
parent: STORY-0413
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, history, state]
depends_on: [TASK-041305]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'starts loading, with nothing read and nowhere to go'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the rows the server sent, in the order it sent them'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'appends the next page under the rows already read'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the rows when the page was asked for with no cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the rows when the server restarted the walk'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers a first query with no cursor, and a next query only while one is named'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a failure keeps what was already read, and says so'
  - cd web-client && npm run check
---

## Goal

The page walk is a pure reducer: rows accumulate in the order the server sent them, a page asked for
with a cursor is appended and one asked for without a cursor replaces, and the walk stops offering to
continue the moment the server stops naming a cursor.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/history-state.ts` | create |
| `web-client/src/history/history-state.test.ts` | create |

Read, not edited: `web-client/src/profile/duels-query.ts`,
`web-client/src/profile/duel-page.ts` (the `DuelPageRead` shape the `page` event mirrors),
`web-client/src/profile/profile-fixture.ts` (`aDuelLine`).

## Scope

- ```ts
  export type HistoryPhase = "loading" | "ready" | "failed";

  export interface HistoryState {
    readonly filter: HistoryFilter;
    readonly rows: readonly RecentDuel[];
    readonly nextCursor: string | null;   // null: the server named no further page
    readonly askedWith: string | null;    // the `after` the request in flight carried
    readonly phase: HistoryPhase;
  }

  export type HistoryEvent =
    | { readonly type: "asked"; readonly after: string | null }
    | { readonly type: "page"; readonly duels: readonly RecentDuel[];
        readonly nextCursor: string | null; readonly restarted: boolean }
    | { readonly type: "failed" };

  export function initialHistory(filter?: HistoryFilter): HistoryState;
  export function historyReducer(state: HistoryState, event: HistoryEvent): HistoryState;
  export function firstPageQuery(filter: HistoryFilter): HistoryQuery;
  export function nextPageQuery(state: HistoryState): HistoryQuery | null;
  ```

- `asked` records the cursor the outstanding request carried and sets `loading`. **`askedWith` is the
  whole mechanism**: the reducer cannot ask the component what kind of request came back, and a
  `page` event carries no memory of what was asked, so the state has to hold it.
- `page` sets `rows` to `[...state.rows, ...event.duels]` when `state.askedWith !== null` **and**
  `event.restarted` is false, and to `event.duels` otherwise; sets `nextCursor` to the server's
  value verbatim; clears `askedWith`; sets `ready`.
- **Nothing sorts, reverses, de-duplicates or re-keys.** The rows are spread in the order they
  arrived, under the rows already held. `TASK-031112` pinned this for the strip and it is the same
  defect one level up.
- `failed` sets `failed`, clears `askedWith`, and **keeps `rows` and `nextCursor` exactly as they
  are**: a player who has read three pages and whose fourth request failed has not un-read the three.
- `firstPageQuery(filter)` is `{ ...filter, after: null }`. `nextPageQuery(state)` is `null` when
  `state.nextCursor === null`, and `{ ...state.filter, after: state.nextCursor }` otherwise — the
  cursor passed through untouched, never parsed, never incremented, and never derived from a row
  count (`ADR-0057` §4).
- The whole file is pure: no React, no `fetch`, no `Date`, no `Storage`, no module-level mutable
  state.

## Out of scope

- The `filtered` event and everything a filter change clears — `TASK-041307`.
- Rendering anything — `TASK-041308` onwards.
- Making the request that produces a `page` event. The reducer never calls a read; the component
  does, and hands it in as an event.
- De-duplicating rows across pages. **A refusal, not an omission:** `STORY-0408` proved server-side
  that pages are total and disjoint, so a duplicate row on this screen means the server broke a
  guarantee, and a client that quietly hid it would delete the evidence.

## Tests

`web-client/src/history/history-state.test.ts`, describe block `"the history walk"`. Rows are built
with `aDuelLine`, and every multi-row fixture is **monotone in no field** — not sorted by
`finishedAt`, not by `duelId`, not by `handsPlayed`, not by `coinDelta` — so a `sort` on any of them
fails rather than passing by luck.

| Test | Proves |
| --- | --- |
| `starts loading, with nothing read and nowhere to go` | `initialHistory()` equals `{ filter: NO_FILTER, rows: [], nextCursor: null, askedWith: null, phase: "loading" }`. Fails against an initial state that starts `ready`, which would flash *"No duels yet."* at a player who has hundreds |
| `keeps the rows the server sent, in the order it sent them` | Three rows monotone in no field, delivered as one `page`, produce `rows` whose mapped `duelId`s `toEqual` the fixture's order. Fails against any `sort` or `reverse` in the reducer |
| `appends the next page under the rows already read` | From a state holding two rows, `asked` with `after: "cur-1"` then a `page` of two more gives four rows in the order first-page-then-second-page. Fails against a reducer that replaces, and against one that prepends |
| `replaces the rows when the page was asked for with no cursor` | From the same two-row state, `asked` with `after: null` then a `page` of two gives **two** rows — the new ones. Fails against an unconditional append, which is how a re-read doubles the list |
| `replaces the rows when the server restarted the walk` | From the same two-row state, `asked` with `after: "stale"` then a `page` carrying `restarted: true` gives **two** rows. Fails against a reducer that decides on `askedWith` alone — the case `ADR-0057` §5 creates, where the answer is the newest page rather than the continuation that was asked for |
| `offers a first query with no cursor, and a next query only while one is named` | Three assertions in one test: `firstPageQuery({ outcome: "WON", opponent: "Ada" })` has `after: null`; `nextPageQuery` on a state whose `nextCursor` is `"cur-7"` returns that filter with `after: "cur-7"`, the string identical; `nextPageQuery` on a state whose `nextCursor` is `null` returns `null`. Fails against a walk that keeps asking past the last page, and against one that mangles the cursor |
| `a failure keeps what was already read, and says so` | From a state holding three rows and a cursor, `failed` gives `phase: "failed"` with those three rows and that cursor intact and `askedWith` cleared. Fails against a reducer that empties the list on a failed *show more*, which would take away three pages a player had already read |

Seven tests, in a new file.

## Acceptance criteria

- [ ] All seven tests above pass, under describe block `the history walk`
- [ ] The multi-row fixtures are monotone in none of `finishedAt`, `duelId`, `handsPlayed` or
      `coinDelta`, so no ordering assertion can pass by luck
- [ ] `grep -cE '\.sort\(|\.reverse\(' web-client/src/history/history-state.ts` returns `0`
- [ ] `grep -cE 'useState\(|useReducer\(|fetch\(|localStorage\.|new Date\(' web-client/src/history/history-state.ts`
      returns `0` — the file is pure
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
