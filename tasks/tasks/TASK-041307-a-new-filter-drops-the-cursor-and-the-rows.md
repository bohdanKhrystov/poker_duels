---
schema: 2
id: TASK-041307
title: A new filter drops the cursor and the rows it belonged to
type: task
status: ready
parent: STORY-0413
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, history, state, cursor]
depends_on: [TASK-041306]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a new filter clears the cursor, the rows and the request in flight'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clears just as thoroughly when only the search term changed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never offers a query carrying a cursor from another filter'
  - cd web-client && npm run check
---

## Goal

A cursor cannot outlive the filter that produced it: the one event that changes a filter clears the
cursor, the rows and the request in flight in the same step, so there is no state in which the screen
could ask for page two of one filter with page one's cursor from another.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/history-state.ts` | modify — one event and its branch |
| `web-client/src/history/history-state.test.ts` | modify — three tests added |

Read, not edited:
[`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) §5.

## Scope

- `HistoryEvent` gains `{ readonly type: "filtered"; readonly filter: HistoryFilter }`.
- Its branch answers `initialHistory(event.filter)` — the same state a fresh screen starts in, with
  the new filter in it. Written as a call to `initialHistory` rather than as a fresh object literal,
  so that a field added to `HistoryState` later cannot be cleared in one place and forgotten in the
  other.
- This is the *client* half of `ADR-0057`. The server's `400` is the backstop (`TASK-041304`); this
  is the thing that makes the backstop never fire in ordinary use — §5: *"a client that changes a
  filter starts a new page walk"*.
- Both axes count. A change to `opponent` alone is a filter change exactly as a change to `outcome`
  alone is, because `ADR-0057` §2's fingerprint covers both and a cursor drawn under one search term
  is refused under another.

## Out of scope

- Deciding *when* a filter change is dispatched — the outcome controls are `TASK-041311`'s and the
  search box is `TASK-041312`'s.
- Comparing the new filter with the old and doing nothing when they are equal. **A refusal, not an
  omission:** it would be an optimisation whose failure mode is a stale cursor surviving a change the
  comparison judged equal, and the cost it saves is one request. Clearing unconditionally is the
  property the story asks for.
- Anything about the `400` a stale cursor earns. `TASK-041304` owns it.

## Tests

`web-client/src/history/history-state.test.ts`, describe block `"the history walk"`.

| Test | Proves |
| --- | --- |
| `a new filter clears the cursor, the rows and the request in flight` | From a state holding three rows, `nextCursor: "cur-3"`, `askedWith: "cur-3"` and `phase: "ready"`, a `filtered` event for `{ outcome: "LOST", opponent: "" }` gives exactly `initialHistory({ outcome: "LOST", opponent: "" })`, asserted with `toEqual` over the whole state. Fails against a reducer that keeps the cursor — the bug this screen must make unreachable — and against one that keeps the rows of the filter just left |
| `clears just as thoroughly when only the search term changed` | The same starting state, twice in **one** test: once filtered to `{ outcome: null, opponent: "Ada" }` and once to `{ outcome: "WON", opponent: "" }`, both asserted to clear the cursor and the rows. Fails against a reducer that treats only the outcome as a filter change — the half-implementation that leaves a search sending a cursor drawn under no search |
| `never offers a query carrying a cursor from another filter` | Immediately after a `filtered` event, `nextPageQuery` returns `null` and `firstPageQuery(state.filter)` carries `after: null` and the **new** filter's two axes. Fails against a reducer that clears `rows` but not `nextCursor`, which would leave the very query `ADR-0057` §5 answers with a `400` |

Three tests added to the seven `TASK-041306` wrote. Every `page` event literal and every assertion in
those seven is untouched: `filtered` is a new event and changes no existing branch.

## Acceptance criteria

- [ ] `the history walk > a new filter clears the cursor, the rows and the request in flight` passes,
      asserting the whole state with `toEqual`
- [ ] `the history walk > clears just as thoroughly when only the search term changed` passes, with
      both filters asserted in one test
- [ ] `the history walk > never offers a query carrying a cursor from another filter` passes
- [ ] The seven tests `TASK-041306` wrote pass unchanged, and no assertion in them is edited
- [ ] The `filtered` branch is written as a call to `initialHistory`, so no field of `HistoryState`
      is cleared in two places
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
