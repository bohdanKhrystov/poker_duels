---
schema: 2
id: TASK-041302
title: One page of the record, and the cursor that names the next one
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, history, http, parse]
depends_on: [TASK-041301]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks the path the query names, and sends the device id'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers the rows and the cursor that names the next page'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when the body names no cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks nothing when the browser holds no device id'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a 401 as no profile and a 500 as unavailable'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops the opponent id from every row it parses'
  - cd web-client && npm run check
---

## Goal

`readDuelPage` reads one page of `GET /api/me/duels` for a given `HistoryQuery`, and answers with the
rows **and** the `nextCursor` the server sent — the field the strip's read has been discarding since
`STORY-0408` shipped it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/duel-page.ts` | create |
| `web-client/src/profile/duel-page.test.ts` | create |

Read, not edited: `web-client/src/profile/recent-duels.ts` (the row parse this one reproduces, and
the `RecentDuel` type it imports), `web-client/src/profile/set-name.ts` (the shape to follow: an
endpoint whose outcome set is its own maps its own statuses), `docs/protocol.md` lines 149–169.

## Scope

- ```ts
  export type DuelPageRead =
    | { readonly kind: "page"; readonly duels: readonly RecentDuel[]; readonly nextCursor: string | null }
    | { readonly kind: "no-profile" }
    | { readonly kind: "unavailable" };

  export async function readDuelPage(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly query: HistoryQuery;
  }): Promise<DuelPageRead>;
  ```
- **It calls `fetch` itself and maps its own statuses**, rather than going through `readFromApi`.
  `readFromApi` collapses every status but `200` and `401` into `unavailable`, and `TASK-041304` has
  to tell a `400` apart from a `500` to honour `ADR-0057` §5. `setDisplayName` already sets this
  precedent for the same reason. Behaviour is otherwise identical to `readFromApi`'s: no device id →
  `no-profile` with **no** request; `200` → parse; `401` → `no-profile`; anything else →
  `unavailable`; a `fetch` or a `json()` that rejects → `unavailable`, caught and never rethrown.
- The request is `GET duelsPath(request.query)` with the `X-Device-Id` header, and no other header.
- `nextCursor` is **required** on the wire and is `string | null`: `docs/protocol.md` says *"Always
  present"*. A body without the key, or with a number or an object there, answers `unavailable` —
  never `null`. A `?? null` would print *"that was the last page"* to a player with two hundred duels
  left the day the server stopped sending it.
- The row parse reproduces `recent-duels.ts`'s exactly, including dropping `opponentPlayerId`: a row
  missing or mistyping any of the six fields, or carrying an `outcome` outside the three words,
  answers `unavailable` for the whole page. `TASK-041303` deletes the duplicate immediately after;
  it exists for one ticket so that this one does not also rewrite the strip's read.
- `import type { RecentDuel } from "./recent-duels"` — **type-only**, so that when `TASK-041303`
  makes `recent-duels.ts` import `readDuelPage` there is no runtime cycle: a type-only import is
  erased entirely.
- `storage` is a parameter, never the `localStorage` global. Under Vitest, Node's own inert
  `localStorage` shadows jsdom's, and every read in `src/profile/` already takes a `Storage`.

## Out of scope

- Retrying, restarting, or telling a `400` apart — `TASK-041304`. This ticket maps `400` to
  `unavailable` like any other unmodelled status, and no test here asserts anything about a `400`
  carrying `after`, because `TASK-041304` would have to undo it.
- Changing `recent-duels.ts`, `readFromApi`, `profile-strip.ts` or the lobby. The duplicate parse is
  deliberate and short-lived; `TASK-041303` is the ticket that removes it.
- Re-testing the row parse's edge cases here. **A refusal, not an omission:** `TASK-041303` makes
  `readRecentDuels` delegate to this function, at which point `recent-duels.test.ts`'s eight merged
  tests exercise this parse unchanged. Writing a second copy of them now would mean two copies to
  keep true, for one ticket's worth of benefit.

## Tests

`web-client/src/profile/duel-page.test.ts`, describe block `"the duel page read"`. Copy
`recent-duels.test.ts`'s `inMemoryStorage`, `answering`, `ok` and `storageHolding` helpers and its
comment about Node's `localStorage`, and add a `withStatus(code)` helper beside `ok` — this file is
the first to assert on statuses `readFromApi` used to hide.

| Test | Proves |
| --- | --- |
| `asks the path the query names, and sends the device id` | Two reads in one test: `WHOLE_RECORD` records the path `/api/me/duels`, and `{ outcome: "LOST", opponent: "Ada", after: "cur-1" }` records the exact path `duelsPath` builds for it. Both calls carry `X-Device-Id`. Fails against a read that ignores its query and against one that builds a path of its own |
| `answers the rows and the cursor that names the next page` | Two reads in one test: a body with `nextCursor: "cur-9"` and a body with `nextCursor: null`, asserted with `toEqual` over the whole answer. One body cannot tell a copied field from a constant — a read that hardcoded `null` would pass a single-fixture test and stop every page walk at one page |
| `answers unavailable when the body names no cursor` | A **literal** body carrying `duels: []` and no `nextCursor` key answers `unavailable`; so does one carrying `nextCursor: 7`. Fails against `?? null`, which would announce the end of a history that has not ended |
| `asks nothing when the browser holds no device id` | An empty `Storage` answers `no-profile` and `calls` is empty. Fails against a read that spends a round trip on an answer it already knows |
| `reads a 401 as no profile and a 500 as unavailable` | Two statuses in one test. Fails against a read that maps every non-`200` to one outcome, which would render *"your record did not load"* to a browser that simply has no profile |
| `drops the opponent id from every row it parses` | A page built from `duelRowBody({ opponentPlayerId: "player-77" })` parses to a row equal to its six fields, and `JSON.stringify` of the whole answer contains no `player-77`. Fails against a parse that copies the row through |

Six tests, in a new file.

## Acceptance criteria

- [ ] `the duel page read > asks the path the query names, and sends the device id` passes, with both
      queries asserted
- [ ] `the duel page read > answers the rows and the cursor that names the next page` passes, with
      both bodies asserted in one test
- [ ] `the duel page read > answers unavailable when the body names no cursor` passes for both the
      missing key and the wrong type
- [ ] `the duel page read > asks nothing when the browser holds no device id` passes
- [ ] `the duel page read > reads a 401 as no profile and a 500 as unavailable` passes
- [ ] `the duel page read > drops the opponent id from every row it parses` passes
- [ ] `grep -c 'readFromApi' web-client/src/profile/duel-page.ts` returns `0`
- [ ] `grep -c 'localStorage' web-client/src/profile/duel-page.ts` returns `0`
- [ ] Every test in `web-client/src/profile/recent-duels.test.ts` passes unchanged, and that file
      does not differ
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
