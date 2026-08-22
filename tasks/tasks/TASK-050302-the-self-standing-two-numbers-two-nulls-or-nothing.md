---
schema: 2
id: TASK-050302
title: The self standing is two numbers, two nulls, or nothing — and it carries no player id
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, leaderboard, parse, self-standing]
depends_on: [TASK-050301]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a placed reader as a rank and a standing, and keeps no player id on it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads a reader with no place as two nulls, never as a zero'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads an absent reader as no self standing at all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a self object that has a rank but no standing'
  - cd web-client && npm run check
---

## Goal

The three answers `ADR-0065` §4 defines survive the parse as three distinguishable values, and the
reader's own id is dropped at the door.

## Why the id is dropped, and why that is the point

`ADR-0065` §8 and `ADR-0002`: the self line is *"rendered from the field the response carried, never
derived by matching the player's id against the rows on screen"*. A client that holds no id for the
reader **cannot** make that mistake — there is nothing to match with. Dropping `playerId` here is
therefore not tidiness, it is the structural half of the guarantee; the tests in `TASK-050311` are
the other half. It is the same move `recent-duels.ts` made with `opponentPlayerId`
(`TASK-041105`).

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-page.ts` | modify — add `SelfStanding`, add `self` to `LadderPage`, parse it |
| `web-client/src/ladder/ladder-page.test.ts` | modify — **adds tests only**; no assertion written by `TASK-050301` changes, and no fixture body is edited |

Read, not edited: `docs/protocol.md` *The reader's own standing (`self`)*.

## Scope

- Add:

  ```ts
  export interface SelfStanding {
    readonly rank: number | null;
    readonly coins: number | null;
  }
  ```

  and `readonly self: SelfStanding | null` to `LadderPage`.
- `self` must be **present** on the body. Three shapes, and nothing else parses:
  - an object whose `rank` and `coins` are **both numbers** → `{ rank, coins }`;
  - an object whose `rank` and `coins` are **both `null`** → `{ rank: null, coins: null }`;
  - `null` → `null`.
- **A mixed object — one number and one `null` — makes the whole body invalid** and
  `parseLadderPage` answers `null`. The wire never sends one (`docs/protocol.md`: *"`self` carries
  `playerId` with `rank` and `coins` both `null`"*), and a client that quietly half-read one would
  render a rank with no standing beside it.
- `playerId` on the wire's `self` object is **read by nothing and copied to nothing**. `SelfStanding`
  has two fields and only two.
- `{ rank: null, coins: null }` is **not** `{ rank: 0, coins: 0 }` and is not `null`. `ADR-0065` §4:
  `0` is a real standing a drawn duel earns, and *no place* is not one.

## Out of scope

- **The words the self line says, and the branch between its two sentences** — `TASK-050310`, in
  `ladder-text.ts`. This ticket produces the value; nothing here is rendered.
- **Anything that matches the reader against a row**, now or later. `ADR-0065` §5, §8.
- **Editing `TASK-050301`'s fixture bodies.** They already carry `self: null` for exactly this
  reason, and that case is one of the three this ticket must read.

## Tests

`web-client/src/ladder/ladder-page.test.ts`, same `describe`, four new tests.

| Test | Proves |
| --- | --- |
| `reads a placed reader as a rank and a standing, and keeps no player id on it` | Body with `self: { playerId: "me", rank: 5, coins: -1 }` parses to `self.rank === 5`, `self.coins === -1`, and `Object.keys(page.self)` equals `["rank", "coins"]` — the id is gone, so no later code can match it against a row |
| `reads a reader with no place as two nulls, never as a zero` | Body with `self: { playerId: "me", rank: null, coins: null }` parses to a **non-null** `self` whose `rank` is `null` and whose `coins` is `null`. Asserted against `0` explicitly: `expect(page.self.coins).not.toBe(0)`. Distinguishable from the test above and from the one below |
| `reads an absent reader as no self standing at all` | Body with `self: null` parses to `page.self === null`, and the page still holds its rows and its season. This is the state of every first visit |
| `refuses a self object that has a rank but no standing` | Bodies with `self: { rank: 5, coins: null }` and `self: { rank: null, coins: 3 }` both answer `null` for the whole page |

## Acceptance criteria

- [ ] `reads a placed reader as a rank and a standing, and keeps no player id on it` passes, and its
      `Object.keys` assertion reddens if `playerId` is copied onto `SelfStanding`
- [ ] `reads a reader with no place as two nulls, never as a zero` passes — mutating the parse to
      coerce a `null` rank or a `null` standing to `0` reddens it
- [ ] `reads an absent reader as no self standing at all` passes — mutating the parse to answer
      `{ rank: null, coins: null }` for a `self` of `null` reddens it, because the two states are
      asserted apart
- [ ] `refuses a self object that has a rank but no standing` passes — mutating the parse to accept a
      half-filled object reddens it
- [ ] `grep -c 'playerId' web-client/src/ladder/ladder-page.ts` returns exactly `1` — the row's, and
      not the reader's
- [ ] Every test `TASK-050301` wrote still passes, with no assertion in it edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
