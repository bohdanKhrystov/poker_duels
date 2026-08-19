---
schema: 2
id: TASK-041301
title: A filter and a cursor become exactly one path, and nothing else
type: task
status: ready
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, history, http, parse]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/duels-query.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks the plain path when nothing narrows the record'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names each axis it was given, and only those'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'encodes an opponent term so it cannot forge a parameter'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands the cursor back byte for byte'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no opponent parameter for an empty box, and sends a space unmodified'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for no page size of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says whether a filter narrows anything at all'
  - cd web-client && npm run check
---

## Goal

`web-client/src/profile/duels-query.ts` exists: the two filter axes, the opaque cursor, and one pure
function turning them into the exact path `GET /api/me/duels` is asked for.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/duels-query.ts` | create |
| `web-client/src/profile/duels-query.test.ts` | create |

Read, not edited: `docs/protocol.md` (the `GET /api/me/duels` query-parameter list, lines 138–145),
`web-client/src/profile/recent-duels.ts` (for `DuelOutcomeWord` only).

## Scope

- Declare, all in this one file, because they are one contract:

  ```ts
  export interface HistoryFilter {
    readonly outcome: DuelOutcomeWord | null;
    readonly opponent: string;
  }
  export const NO_FILTER: HistoryFilter = { outcome: null, opponent: "" };
  export function isFiltered(filter: HistoryFilter): boolean;
  export interface HistoryQuery extends HistoryFilter {
    readonly after: string | null;
  }
  export const WHOLE_RECORD: HistoryQuery = { ...NO_FILTER, after: null };
  export function duelsPath(query: HistoryQuery): string;
  ```

- `duelsPath` emits parameters in the fixed order `outcome`, `opponent`, `after`, joins the present
  ones with `&`, and emits **no `?` at all** when none is present — so `duelsPath(WHOLE_RECORD)` is
  exactly `"/api/me/duels"`.
- **Every value goes through `encodeURIComponent`.** A display name may hold `&`, `=`, `#`, `%`, `+`
  and a space; `encodeURI` leaves all but the last two alone, so a name containing `&outcome=WON`
  would forge a second parameter and the server would filter by something the player never asked
  for. `encodeURIComponent` also leaves `-` and `_` untouched — they are in its unreserved set —
  which is what makes a base64url cursor survive the round trip unchanged.
- **A blank box is not a search.** `opponent === ""` emits no `opponent` parameter; every other
  string is emitted exactly as given, **untrimmed**. The server refuses a blank term with `400`
  (`docs/protocol.md`), so an empty box that sent `opponent=` would fail the first render of the
  screen; and trimming would break `STORY-0413`'s rule that the term is sent as the player typed it.
- **No `limit`.** The client asserts no page size: `ADR-0057` §6 makes `limit` the one parameter that
  may vary mid-walk, and varying it buys this screen nothing. The server's default is the page size,
  and `recent-duels.test.ts`'s merged `asks /api/me/duels with no limit of its own` keeps meaning
  what it says.
- `isFiltered` is true when **either** axis narrows the record. It exists here rather than beside the
  screen because the same two axes decide the path and decide which of the two empty states applies
  (`TASK-041305`), and two functions answering that would be two answers.
- KDoc on every export, per the client's existing files.

## Out of scope

- Making any request. This file is pure and imports no `fetch`, no `Storage` and no React —
  `TASK-041302` is the caller.
- Parsing a cursor, or knowing anything about its contents. **A refusal, not an omission:**
  `ADR-0057` §4 makes the payload opaque and says a client that parses one *"has left the
  contract"*. The cursor is a string this file percent-encodes and never inspects.
- Validating an `outcome` spelling or an `opponent` length. The server owns both refusals
  (`docs/protocol.md`), and a second copy of the rules in the client is a second thing to keep in
  step with them.
- Any change to `recent-duels.ts`, `profile-strip.ts` or the lobby. Nothing imports this file yet.

## Tests

`web-client/src/profile/duels-query.test.ts`, describe block `"the duels query"`.

| Test | Proves |
| --- | --- |
| `asks the plain path when nothing narrows the record` | `duelsPath(WHOLE_RECORD)` is exactly `"/api/me/duels"` and contains no `?`. Fails against a builder that always appends `?`, and against one that emits `outcome=null` or `opponent=` for an absent axis |
| `names each axis it was given, and only those` | Four queries in **one** test — outcome alone, opponent alone, cursor alone, all three — each asserted against its exact whole path, with the three-axis case pinning the order `outcome`, `opponent`, `after`. One query could not tell a builder that emits every axis from one that emits the right ones |
| `encodes an opponent term so it cannot forge a parameter` | Three terms in one test: `"a&outcome=WON"`, `"100%Sure"` and `"Ada Lovelace"`, each asserted against its exact whole path. Fails against `encodeURI`, which leaves `&` and `=` alone and would hand the server a filter the player never typed, and against no encoding at all |
| `hands the cursor back byte for byte` | A cursor in the shape `ADR-0057` ships — unpadded base64url holding both `-` and `_`, e.g. `"MjAyNi0wMi0wM-BkLTEx_Qw"` — appears in the path as that exact substring. Fails against any re-encoding that touches `-` or `_`, and against a builder that strips or adds padding |
| `sends no opponent parameter for an empty box, and sends a space unmodified` | `opponent: ""` yields a path with no `opponent`; `opponent: " "` yields `opponent=%20`. Two inputs in one test, because a single empty-string case cannot tell "blank means absent" from `trim()` — and `trim()` is exactly what `STORY-0413` forbids |
| `asks for no page size of its own` | Every path built in this test — the empty query and a fully populated one — contains no `limit`. Fails against a builder that pins a page size the client has no business asserting |
| `says whether a filter narrows anything at all` | `isFiltered` over three filters in one test: `NO_FILTER` false, outcome-only true, opponent-only true. Fails against an implementation that reads only one axis — the failure mode that would make a search returning nothing say *"No duels yet."* |

Seven tests, in a new file: `npm run test -- src/profile/duels-query.test.ts` reports **7**.

## Acceptance criteria

- [ ] `the duels query > asks the plain path when nothing narrows the record` passes
- [ ] `the duels query > names each axis it was given, and only those` passes, asserting four whole
      paths in one test
- [ ] `the duels query > encodes an opponent term so it cannot forge a parameter` passes, with all
      three terms asserted against whole paths
- [ ] `the duels query > hands the cursor back byte for byte` passes
- [ ] `the duels query > sends no opponent parameter for an empty box, and sends a space unmodified`
      passes, asserting both inputs
- [ ] `the duels query > asks for no page size of its own` passes
- [ ] `the duels query > says whether a filter narrows anything at all` passes, asserting all three
      filters
- [ ] `grep -c 'encodeURI(' web-client/src/profile/duels-query.ts` returns `0` — the whole-URI
      encoder is not used anywhere in the file
- [ ] `npm run test -- src/profile/duels-query.test.ts` reports `Tests  7 passed (7)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
