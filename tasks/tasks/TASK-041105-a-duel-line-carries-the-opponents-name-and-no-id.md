---
schema: 2
id: TASK-041105
title: A duel line carries the opponent's name, and still not their id
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, profile, parse, identity]
depends_on: [TASK-041104]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +387 passed \(387\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries a named opponent and a nameless one from the same body'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when a row says nothing about the opponent name'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps every field a row carries except the opponent id'
  - cd web-client && npm run check
---

## Goal

`RecentDuel` carries `opponentDisplayName: string | null` from `GET /api/me/duels`, and still drops
`opponentPlayerId` — so a screen can name an opponent and cannot name an id.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/recent-duels.ts` | modify — one field, its check, and the KDoc that says why the id is still dropped |
| `web-client/src/profile/recent-duels.test.ts` | modify — every body gains the field, three assertions move, two tests added |
| `web-client/src/profile/profile-fixture.ts` | modify — one default on `aDuelLine` and `duelRowBody` |

Read, not edited: `docs/protocol.md` (the recent-duels table's `opponentDisplayName` row).

## Scope

- `RecentDuel` gains `readonly opponentDisplayName: string | null`, validated as
  `typeof x === "string" || x === null`; a row that fails answers `{ kind: "unavailable" }`, exactly
  as a row missing `finishedAt` already does.
- **`opponentPlayerId` is still dropped**, and the KDoc on `RecentDuel` is rewritten rather than
  deleted: the reason changes from *no display name exists yet* to *the name is the label and the id
  is not the client's business*. `ADR-0021` gives the id as the stable identity for correlation and
  this client correlates nothing; dropping it at the parse is what makes
  `profile-no-derivation.test.tsx`'s guard cheap to keep true.
- The fixture gains `opponentDisplayName: null` on `aDuelLine` and `duelRowBody`.
- **This ticket owns `recent-duels.test.ts`'s existing assertions.** Every body in the file gains
  `opponentDisplayName`, and three `toEqual` expectations gain it in their parsed rows:
  `keeps every field a row carries except the opponent`, `reads every outcome the server can send`
  and `takes the coin delta signed, including the zero of a draw`. Measured by making the parse
  strict and running the suite — those three fail and no others do. **No assertion is weakened**:
  each keeps every field it already checked and gains one.
- The first of those three is **renamed** to `keeps every field a row carries except the opponent
  id`, because after this ticket the row keeps one opponent field and drops the other, and a name
  that says *except the opponent* would be false. Its `expect(JSON.stringify(read)).not.toContain("player-77")`
  stays exactly as it is.

## Out of scope

- Rendering the name on a line — `TASK-041116`, which prints `ADR-0058`'s `No name` where an
  opponent has none.
- Any distinction between an opponent whose name was removed and one who never set a name.
  **A refusal, not an omission:** `ADR-0052` §5 makes the two byte-identical on this endpoint, on
  purpose, and `DuelSummaryResponse` carries nothing to tell them apart. A client that tried would
  be inventing a fact the server declines to send.
- `profile-strip.test.ts` and `profile-no-derivation.test.tsx`: `TASK-041102` already built their
  rows through `duelRowBody`, so they gain the field without an edit. If either needs one, the
  migration was incomplete and that is the bug.

## Tests

`web-client/src/profile/recent-duels.test.ts`, describe block `"the recent duels read"`.

| Test | Proves |
| --- | --- |
| `carries a named opponent and a nameless one from the same body` | **One body, two rows**: `duelRowBody({ duelId: "d-1", opponentDisplayName: "Ada" })` and `duelRowBody({ duelId: "d-2", opponentDisplayName: null })` parse to `"Ada"` and `null` respectively, in that order. Fails against a parse that hardcodes either value and against one that reads the id into the name. Two rows in one read is what makes it non-vacuous: a single row cannot tell a copied field from a constant |
| `answers unavailable when a row says nothing about the opponent name` | A **literal** row carrying every other field and no `opponentDisplayName` answers `unavailable`. Fails against `?? null`, which would silently render every opponent as nameless the day the server stopped sending it |
| `keeps every field a row carries except the opponent id` | The renamed existing test: the parsed row equals its five old fields plus `opponentDisplayName`, and `JSON.stringify(read)` contains no `"player-77"`. Fails against a parse that copies the whole row through |

Two tests added to 385, so the suite reports **387**.

## Acceptance criteria

- [ ] `the recent duels read > carries a named opponent and a nameless one from the same body`
      passes, with both rows in one body
- [ ] `the recent duels read > answers unavailable when a row says nothing about the opponent name`
      passes
- [ ] `the recent duels read > keeps every field a row carries except the opponent id` passes and
      still asserts that no `player-77` survives the parse
- [ ] `reads every outcome the server can send` and `takes the coin delta signed, including the zero
      of a draw` pass, each still asserting every field it asserted before
- [ ] `grep -c 'opponentPlayerId:' web-client/src/profile/recent-duels.ts` returns `0` — the field
      is named in the KDoc's prose and in no expression, so no row can carry it out
- [ ] `npm run --silent test` reports `Tests  387 passed (387)`
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
