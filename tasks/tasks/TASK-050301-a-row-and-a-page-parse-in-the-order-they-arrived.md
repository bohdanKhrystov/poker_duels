---
schema: 2
id: TASK-050301
title: A ladder row parses, and the page keeps the order it arrived in
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, leaderboard, parse]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the rows in the order the body listed them, and renumbers nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a rank, a player id, a name that may be null, and a standing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a body with no cursor, a season that is not a month, or a row missing a field'
  - cd web-client && npm run check
---

## Goal

The client can turn a `GET /api/standings` body into typed rows, a season and a cursor — in the
order the server listed them, with no sort, no renumber and no derived position.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-page.ts` | create |
| `web-client/src/ladder/ladder-page.test.ts` | create |

Read, not edited: `web-client/src/profile/duel-page.ts` (`parseDuelPageBody` is the shape to
follow — a body that is invalid in any way answers `null`, never a half-page),
`docs/protocol.md` *Standings endpoint* (the wire contract this parses).

## Scope

- `ladder-page.ts` exports exactly these, and nothing else:

  ```ts
  export interface LadderRow {
    readonly rank: number;
    readonly playerId: string;
    readonly displayName: string | null;
    readonly coins: number;
  }

  export interface LadderPage {
    readonly season: string;
    readonly rows: readonly LadderRow[];
    readonly nextCursor: string | null;
  }

  export function parseLadderPage(body: unknown): LadderPage | null;
  ```

- **`rank` is read off the row and copied.** Nothing in this file counts, indexes, increments or
  compares rows: `ADR-0064` §2 — *"the position of a row inside a page is transport, is never
  rendered, and is never used to derive a rank"*. `rows` comes out in the array order it went in.
- `season` must be a string matching `/^\d{4}-\d{2}$/`; `nextCursor` must be **present** and be a
  string or `null` (an absent key answers `null`, as `duel-page.ts` does, because a missing cursor
  would silently stop a walk that has not ended); `rows` must be an array, and every row must carry
  all four fields at the right type. Any failure answers `null` for the whole body.
- `playerId` is kept because it is the list's React key and nothing else. It is never rendered and
  never compared — `TASK-050313` asserts that.
- **Every fixture body in the test file carries `self: null`**, a key this ticket ignores entirely.
  `TASK-050302` gives it meaning, and it must not have to edit a single body here to do so.
- **No assertion in this file deep-equals a whole `LadderPage`.** Assert `page.season`,
  `page.nextCursor` and `page.rows.map(...)` field by field: `TASK-050302` adds a field to this
  type, and a `toEqual` on the whole object would redden then.

## Out of scope

- **The reader's own standing** — `TASK-050302`, in this same file.
- **`fetch`, `Storage`, headers and status codes** — `TASK-050304`. This module is pure: it takes a
  parsed body and answers a value.
- **Rendering anything** — nothing here imports React.
- **Sorting, de-duplicating or re-keying rows**, and any notion of a row's index. `ADR-0064` §2.

## Tests

`web-client/src/ladder/ladder-page.test.ts`, `describe("one page of the ladder, parsed")`.

| Test | Proves |
| --- | --- |
| `keeps the rows in the order the body listed them, and renumbers nothing` | A body whose four rows carry ranks `[1, 2, 2, 4]` and player ids `["d", "a", "c", "b"]` parses to `rows.map((r) => r.rank)` equal to `[1, 2, 2, 4]` and `rows.map((r) => r.playerId)` equal to `["d", "a", "c", "b"]`. **The ranks are deliberately not `1..n`**: a parse that numbered rows from their index would answer `[1, 2, 3, 4]` and redden here, and would have looked right on any fixture where the two happen to agree |
| `reads a rank, a player id, a name that may be null, and a standing` | One body holding a named row with `coins: 3` and a row with `displayName: null` and `coins: -2`: both survive, `displayName` stays `null` (the parse fabricates no placeholder — `ADR-0058` puts that branch in one place, and it is not this one), and `coins` keeps its sign |
| `refuses a body with no cursor, a season that is not a month, or a row missing a field` | Four bodies in one test — no `nextCursor` key, `season: "August"`, a row with no `rank`, a row whose `coins` is a string — each answer `null`. A partially parsed page is never returned |

The fixture bodies are written inline as wire JSON (`unknown`), not as typed `LadderPage` literals:
the point of the parse is that it is fed things the type system did not vet.

## Acceptance criteria

- [ ] `keeps the rows in the order the body listed them, and renumbers nothing` passes against ranks
      `[1, 2, 2, 4]` — mutating `parseLadderPage` to set `rank` from the row's array index reddens it
- [ ] `reads a rank, a player id, a name that may be null, and a standing` passes — mutating the
      parse to replace a `null` `displayName` with any string reddens it
- [ ] `refuses a body with no cursor, a season that is not a month, or a row missing a field` passes —
      mutating any one of the four validations to accept reddens it
- [ ] `grep -c 'self' web-client/src/ladder/ladder-page.ts` returns `0`
- [ ] Every fixture body in `ladder-page.test.ts` carries `self: null`
- [ ] No assertion in `ladder-page.test.ts` compares a whole `LadderPage` with `toEqual`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
