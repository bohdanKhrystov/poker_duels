---
schema: 2
id: TASK-041312
title: The search box sends the term the player typed, and nothing else
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, history, ui, filters, blocked]
depends_on: [TASK-041311]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the term the player typed, unmodified'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no opponent parameter once the box is emptied'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops the cursor and the rows on a search, and keeps the outcome chosen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks nothing while the player types, and once when the search is submitted'
  - cd web-client && npm run check
---

## Goal

A player can find the duels they played against one opponent by typing part of that opponent's name,
and the server receives exactly the characters they typed.

## Blocked on `DEC-052`

**The product owner's.** `STORY-0413` says searching *"sends the term the player typed, unmodified,
and renders the result"* and does not say **when** it is sent. The two answers build different
screens and neither is derivable from what is merged:

- **As the player types**, debounced — which needs a delay nobody has chosen, puts a timer in the
  client where there is none today, and sends one unindexed `POSITION` scan per pause against a
  read `STORY-0409` recorded as *correct, not yet fast*.
- **On submit** — a control and a keystroke, one request per search, and a box a player can type in
  without the screen moving under them.

The story's own note that *"paging and search are asserted against a fake transport, not a timer"*
reads against a debounce but does not settle it, which is why it is registered rather than assumed.

**Everything else in this ticket is settled and is written below.** The PR that answers `DEC-052`
adds one Scope bullet (how the request fires and what control, if any, fires it), one row to the
Tests table, and one line to `verify:`, and changes nothing already written here.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | modify — the box, and the filter change it dispatches |
| `web-client/src/history/HistoryScreen.test.tsx` | modify — three tests added, plus the one the ADR names |
| `web-client/src/history/history-text.ts` | modify — only if the answer needs a word this story has not written; otherwise untouched and `files_touched` drops to 2 |

Read, not edited: `web-client/src/profile/duels-query.ts`, `docs/protocol.md` line 143.

## Scope

- A `<input type="text">` labelled `OPPONENT_LABEL`, whose value is React state in the component and
  whose committed value becomes `filter.opponent`.
- **The term is sent as typed.** No trim, no case fold, no NFC normalisation, no truncation to 32
  code points, no stripping of `%` or `_`. `duelsPath` percent-encodes it (`TASK-041301`) and the
  server owns every refusal (`docs/protocol.md` line 143); a client that pre-validated would be a
  second copy of rules it cannot keep in step with.
- **An emptied box is not a search**: committing `""` sets `filter.opponent` to `""`, which
  `duelsPath` renders as no `opponent` parameter at all, so the record widens back rather than
  asking the server to match the empty string — which it answers with `400`.
- A search is a filter change: it dispatches `filtered` and then asks `firstPageQuery`, so the cursor
  and the rows of the previous term are dropped by `TASK-041307`'s branch, and the chosen outcome is
  carried in the same filter.
- If a delay is involved at all, its test installs `vi.useFakeTimers()`. `virtual-time.test.ts` is a
  merged sweep over every test file in `src/` and it fails the build otherwise; no new guard is
  needed and none is added.


**Corrected during the work.** This ticket was written while `DEC-052` was open, and its `verify:`
block never gained the line `ADR-0059` §5 named — the grep for
`asks nothing while the player types, and once when the search is submitted`. As written the ticket
could be verified without ever running the test that pins the ADR's whole decision. Added above.

## Out of scope

- Searching for *players* rather than for duels. `ADR-0029` §7 and `STORY-0409` both refuse it: this
  endpoint takes a name and returns duels, and no path here turns a name into an identity.
- Any hint about an opponent who has no name. **A refusal, not an omission:** the server documents
  that such an opponent matches no search, and a client that explained that would be explaining a
  server rule at the exact place `ADR-0052` §5 requires silence.
- Suggestions, autocomplete or a recently-searched list. Each needs data the server does not send.
- Highlighting the matched substring in a row. `EPIC-06` owns what a row looks like.

## Tests

`web-client/src/history/HistoryScreen.test.tsx`, describe block `"the history screen"`. The three
below are trigger-independent — they assert what is sent and what happens to the rows, not what
causes the request — so they stand under either answer to `DEC-052`.

| Test | Proves |
| --- | --- |
| `sends the term the player typed, unmodified` | Two terms in one test — `"  ada  "` and `"100%Sure"` — each reaching `read` as `opponent` **strictly equal** to what was typed. Fails against a `trim()`, a `toLowerCase()`, an NFC normalise and a wildcard strip, and two terms are what separates "sent as typed" from "sent as this one fixture happens to be" |
| `sends no opponent parameter once the box is emptied` | After a search for `"Ada"`, emptying the box asks again with `opponent: ""` — which `duelsPath` renders with no `opponent` parameter, asserted through `duelsPath` rather than by a substring check. Fails against a screen that keeps the last term, and against one that sends `opponent=` and earns a `400` |
| `drops the cursor and the rows on a search, and keeps the outcome chosen` | With `Won` chosen and a first page holding a non-null `nextCursor` and two rows, a search for `"Ada"` asks with `{ outcome: "WON", opponent: "Ada", after: null }` and leaves only the new page's rows on the screen. Fails against a search that carries the cursor — the `400` `ADR-0057` §5 answers — and against one that discards the outcome, which would silently widen the record while the radio still reads *Won* |
| *(named by the ADR that answers `DEC-052`)* | The firing rule: exactly what act sends the request, and that nothing else does |

Three tests written now, plus the one the answer names.

## Acceptance criteria

- [ ] `the history screen > sends the term the player typed, unmodified` passes, with both terms
      compared by strict equality
- [ ] `the history screen > sends no opponent parameter once the box is emptied` passes
- [ ] `the history screen > drops the cursor and the rows on a search, and keeps the outcome chosen`
      passes, asserting the whole query
- [ ] The criterion `DEC-052`'s ADR names passes
- [ ] The fourteen tests `TASK-041308` through `TASK-041311` wrote pass unchanged
- [ ] `grep -cE '\.trim\(|toLowerCase\(|normalize\(' web-client/src/history/HistoryScreen.tsx`
      returns `0`
- [ ] No file outside those listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
