---
schema: 2
id: TASK-041310
title: Another page is offered until the server names none, and then never asked for
type: task
status: backlog
parent: STORY-0413
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, history, ui, cursor]
depends_on: [TASK-041309]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers another page while the server names one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stops offering on the last page, and asks for nothing more'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks with the cursor the server sent, byte for byte'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'appends the second page under the first, in the order both arrived'
  - cd web-client && npm run check
---

## Goal

A player can walk the whole record: one control asks for the next page while the server names one,
and it is gone — with no request behind it — the moment the server does not.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | modify — one control, driven by `nextPageQuery` |
| `web-client/src/history/HistoryScreen.test.tsx` | modify — four tests added |

Read, not edited: `web-client/src/history/history-state.ts`,
[`ADR-0057`](../../docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) §4.

## Scope

- A `<button type="button">` labelled `MORE`, rendered **if and only if** `nextPageQuery(state)` is
  not `null`. Its handler calls `ask` with exactly that query.
- **The control's existence and the request are the same condition**, read from the same call: a
  screen that rendered the control from one rule and asked from another is a screen where the two can
  disagree. `nextPageQuery` returning `null` on the last page is what makes *"no request is made
  after it"* true by construction rather than by a guard somebody has to remember.
- The cursor reaches the request untouched. The component never reads it, never trims it, never
  counts rows to work out a page number, and never builds one: `ADR-0057` §4 makes the payload
  opaque, and a client that parses one has left the contract.
- The control is not rendered while `phase` is `loading`, so a second click cannot start a second
  walk from the same cursor.

## Out of scope

- Loading the next page when the list is scrolled to its end. **A refusal, not an omission:**
  `STORY-0413` says the last page *"stops offering another"*, which is a control that is there and
  then is not; a scroll listener is a different affordance, needs a decision this story has not
  taken, and would put a real clock in the tests the story forbids.
- A page number, a total, a *"showing 20 of 200"*, or anything else counted from rows on the screen.
  The server sends no total and the client derives none.
- A control to go *back* a page. Keyset paging is forward-only (`STORY-0408`), and the rows already
  read are still on the screen.
- Retrying a failed *show more*. `TASK-041309` renders `READ_FAILED` beneath the rows; a retry
  affordance is not ticketed.

## Tests

`web-client/src/history/HistoryScreen.test.tsx`, describe block `"the history screen"`. The cursor
fixture is unpadded base64url holding both `-` and `_`, so any re-encoding shows up.

| Test | Proves |
| --- | --- |
| `offers another page while the server names one` | A first page answering `nextCursor: "MjAy-Ni0w_Mw"` renders exactly one button whose text is `MORE`. Fails against a screen that never offers one, and against one that offers it before the first page lands |
| `stops offering on the last page, and asks for nothing more` | A first page answering `nextCursor: null` renders **no** button with that text, and `read` has been called exactly **once** after the render settles. The count is what makes it more than a rendering assertion: a screen that hid the control but kept asking would pass the first half and fail this |
| `asks with the cursor the server sent, byte for byte` | Clicking the control calls `read` a second time with `after` **strictly equal** to `"MjAy-Ni0w_Mw"`, and with the same two filter axes as the first call. Fails against any trim, re-encode or reconstruction, and against a request that drops the filter when it pages |
| `appends the second page under the first, in the order both arrived` | Two rows, then two more; after the click the list holds four `listitem`s whose texts `toEqual` first-page-then-second-page order. Fails against a screen that replaces the list on *show more* — the bug that makes a page walk look like a page swap — and against one that prepends |

Four tests added to the seven the two tickets before wrote, all of which pass unchanged: none of them
answers a non-null `nextCursor`, so none of them renders the control.

## Acceptance criteria

- [ ] `the history screen > offers another page while the server names one` passes
- [ ] `the history screen > stops offering on the last page, and asks for nothing more` passes,
      asserting `read` was called exactly once
- [ ] `the history screen > asks with the cursor the server sent, byte for byte` passes, comparing
      the cursor with strict equality
- [ ] `the history screen > appends the second page under the first, in the order both arrived`
      passes, comparing four rows with `toEqual`
- [ ] The seven tests `TASK-041308` and `TASK-041309` wrote pass unchanged
- [ ] `grep -c 'nextPageQuery(' web-client/src/history/HistoryScreen.tsx` returns `1` — it is called
      once and the result is bound, so the control and the request read the same answer
- [ ] `grep -cE 'atob\(|btoa\(|Math\.ceil' web-client/src/history/HistoryScreen.tsx` returns `0` — no
      cursor is decoded and no page number is worked out from a row count
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
