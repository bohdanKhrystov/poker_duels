---
schema: 2
id: TASK-041311
title: The outcome filter is four choices, and choosing one starts a new walk
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, history, ui, filters]
depends_on: [TASK-041310]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers all and the three outcomes, in the words a row already uses'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for the first page of the outcome chosen, dropping the cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the rows of the filter just left'
  - cd web-client && npm run check
---

## Goal

A player can narrow the record to won, lost or drew and widen it back, and each choice starts a fresh
walk of that filter rather than continuing the last one.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | modify — a four-choice control and its handler |
| `web-client/src/history/HistoryScreen.test.tsx` | modify — three tests added |

Read, not edited: `web-client/src/profile/profile-text.ts` (`outcomeWord`),
`web-client/src/history/history-text.ts`.

## Scope

- A `<fieldset>` whose legend is `OUTCOME_LEGEND`, holding four radio inputs in one group: `All`
  (`EVERY_OUTCOME`, checked to begin with) and one per outcome. Radios rather than buttons because
  the four choices are exclusive and exactly one is always in force, which is what a radio group
  already means to a screen reader — and because `getByLabelText` makes the assertion about the
  label a player reads rather than about a class.
- **The three outcome labels are `outcomeWord("WON")`, `outcomeWord("LOST")` and
  `outcomeWord("DREW")`** — called, not copied. A filter labelled with a second spelling could drift
  from the rows it filters.
- Choosing one dispatches `filtered` with `{ outcome, opponent: state.filter.opponent }` and then
  asks `firstPageQuery` of that filter. The cursor is dropped by the reducer
  (`TASK-041307`), so this handler never sees one; `All` dispatches `outcome: null`.
- The search term is carried across an outcome change, because they are two axes of one filter and
  the server composes them (`docs/protocol.md` line 145).
- The wire words are sent, not the reader's: the request carries `WON`, and the label says `Won`.

## Out of scope

- The search box — `TASK-041312`, blocked on `DEC-052`. This ticket reads
  `state.filter.opponent` and never writes it, so nothing here presumes the answer.
- Any filter axis the server does not offer. `docs/protocol.md` lists two; a client control for a
  third would be a control that cannot be answered.
- Remembering the chosen filter across a reload, or putting it in a URL. Not ticketed, and it depends
  on `DEC-053`.
- Disabling a choice that would match nothing. The client cannot know that without asking, and
  `TASK-041309` already has a sentence for the answer.

## Tests

`web-client/src/history/HistoryScreen.test.tsx`, describe block `"the history screen"`.

| Test | Proves |
| --- | --- |
| `offers all and the three outcomes, in the words a row already uses` | Four radios, found by the labels `EVERY_OUTCOME`, `outcomeWord("WON")`, `outcomeWord("LOST")` and `outcomeWord("DREW")` — the three read from the function, not written as literals, so a re-spelling in either place fails here. `All` is checked before anything is clicked. Fails against a control with three choices and no way back to the whole record |
| `asks for the first page of the outcome chosen, dropping the cursor` | A first page answering a non-null `nextCursor`, then a click on `Lost`: `read`'s second call is exactly `{ outcome: "LOST", opponent: "", after: null }`. Fails against a handler that carries the cursor into the new filter — the request `ADR-0057` §5 answers with a `400`, and the one thing `STORY-0413` says the screen must make unreachable — and against one that sends the reader's word `Lost` instead of the wire's `LOST` |
| `replaces the rows of the filter just left` | Two rows under `All`, then a click on `Won` answered by one different row: the list holds exactly **one** `listitem`, and the two rows of the previous filter are gone from the markup. Fails against a screen that appends the new filter's page to the old filter's rows, which would show a player duels their filter excludes |

Three tests added to the eleven the three tickets before wrote, all of which pass unchanged: none of
them touches a filter control.

## Acceptance criteria

- [ ] `the history screen > offers all and the three outcomes, in the words a row already uses`
      passes, finding all four by label
- [ ] `the history screen > asks for the first page of the outcome chosen, dropping the cursor`
      passes, asserting the whole query of the second call
- [ ] `the history screen > replaces the rows of the filter just left` passes, asserting exactly one
      row remains
- [ ] The eleven tests `TASK-041308`, `TASK-041309` and `TASK-041310` wrote pass unchanged
- [ ] `grep -cE '"(Won|Lost|Drew)"' web-client/src/history/HistoryScreen.tsx` returns `0` — the three
      labels are `outcomeWord`'s output
- [ ] The three outcome labels in the test are read from `outcomeWord`, not written as string
      literals
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
