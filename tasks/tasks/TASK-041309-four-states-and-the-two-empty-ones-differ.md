---
schema: 2
id: TASK-041309
title: Four states, and the two empty ones say different things
type: task
status: backlog
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, history, ui, copy]
depends_on: [TASK-041308]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says it is loading before the first page lands, and says nothing else'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells an empty record from a filter that matched nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the read failed, and keeps the pages already read'
  - cd web-client && npm run check
---

## Goal

The four states `STORY-0413` names are four sentences on the screen, and the two empty ones are not
the same sentence.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | modify — the sentence each state renders |
| `web-client/src/history/HistoryScreen.test.tsx` | modify — three tests added |

Read, not edited: `web-client/src/history/history-text.ts`.

## Scope

- The screen renders exactly one state sentence, chosen by this table and nothing else:

  | `phase` | rows | What is rendered |
  | --- | --- | --- |
  | `loading` | none | `LOADING_RECORD`, and no list |
  | `loading` | some | the rows, then `LOADING_RECORD` |
  | `failed` | any | the rows it has, then `READ_FAILED` |
  | `ready` | none | `emptyLine(isFiltered(state.filter))`, and no list |
  | `ready` | some | the rows |

- **`emptyLine` is called; the choice is not made here.** `TASK-041305` put the two facts behind one
  function precisely so that a component cannot decide them separately, and this is the caller that
  makes that worth having.
- `isFiltered(state.filter)` from `duels-query.ts` is what says which empty state applies. Nothing
  here inspects the two axes for itself.
- No sentence is authored in this file: every string comes from `history-text.ts`.
- A failed *show more* leaves the rows on the screen and adds the failure sentence beneath them. The
  reducer already keeps them (`TASK-041306`); this is the rendering half.

## Out of scope

- A retry button, a spinner, an alert or a `console` line. `ProfileStrip` announces no failed
  background read at all, and this screen states one sentence and offers no action the story did not
  ask for.
- Any word for a browser holding no profile. `TASK-041308` settled that a `no-profile` answer is an
  empty page, so it renders `NO_DUELS` — true of it, and one state rather than a fifth.
- Distinguishing a filter that matched nothing from a filter that matched nothing *because the server
  refused something*. The read has already turned every refusal into `unavailable` or a restart; by
  the time the screen sees an empty page, an empty page is what it is.

## Tests

`web-client/src/history/HistoryScreen.test.tsx`, describe block `"the history screen"`.

| Test | Proves |
| --- | --- |
| `says it is loading before the first page lands, and says nothing else` | With a `read` whose promise is not yet resolved, the screen shows `LOADING_RECORD`, and shows **neither** `NO_DUELS` nor `NO_MATCH` nor `READ_FAILED`. Fails against a screen that starts in `ready` and flashes *"No duels yet."* at a player with hundreds — the classic first-paint lie — and the three negative assertions are what make it more than a smoke test |
| `tells an empty record from a filter that matched nothing` | **One test, two renders**: an empty page under no filter, and an empty page under `{ outcome: "WON", opponent: "" }`. The first shows `NO_DUELS` and not `NO_MATCH`; the second shows `NO_MATCH` and not `NO_DUELS`. Fails against a screen that prints one sentence for both — which is the whole point of the state, and which two separate single-fixture tests could not distinguish from a constant |
| `says the read failed, and keeps the pages already read` | **One test, two scenarios**: a first read that answers `unavailable` shows `READ_FAILED` and no list; a first read that answers two rows followed by a second read that answers `unavailable` shows `READ_FAILED` **and** both rows still. Fails against a screen that empties the list on a failed *show more*, and against one that hides the failure when it has rows |

Three tests added to the four `TASK-041308` wrote, which pass unchanged: none of them renders an
empty page or a failure.

## Acceptance criteria

- [ ] `the history screen > says it is loading before the first page lands, and says nothing else`
      passes, with all three negative assertions
- [ ] `the history screen > tells an empty record from a filter that matched nothing` passes, with
      both renders and both negative assertions in one test
- [ ] `the history screen > says the read failed, and keeps the pages already read` passes, with both
      scenarios in one test
- [ ] The four tests `TASK-041308` wrote pass unchanged
- [ ] `grep -cE '"No duels|"No duel matches|"Loading|"Your duels did not' web-client/src/history/HistoryScreen.tsx`
      returns `0` — every sentence comes from `history-text.ts`
- [ ] `grep -c 'emptyLine(' web-client/src/history/HistoryScreen.tsx` returns `1` — one call, one
      decision
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
