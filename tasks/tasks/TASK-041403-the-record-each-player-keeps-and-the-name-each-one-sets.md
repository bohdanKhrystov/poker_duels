---
schema: 2
id: TASK-041403
title: The record each player keeps, and the name each one sets
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, e2e, test, http]
depends_on: [TASK-041402]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +9 passed \(9\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'answers each device id with its own duels'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'a name set on one player is not set on the other'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'the name survives into the next profile read'
  - cd web-client && npm run check
---

## Goal

The double answers `GET /api/me/duels` with each player's own duels, and `PUT /api/me/name` changes
the name of exactly one player.

## Why this exists

`STORY-0414` asserts *the same duel* and *the same name* across two browsers. Both need a server that
holds them per player and can be **wrong** — a name written to the wrong record, a duel list shared
between two players — or neither assertion is evidence.

`readDuelPage` (`duel-page.ts:78`) and `setDisplayName` (`set-name.ts:42`) are the merged callers.
`readDuelPage` requires `nextCursor` to be present on the wire: `duel-page.ts:131` answers
`unavailable` for a body lacking it, so a body without it is not a shortcut, it is a broken double.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/account-server.ts` | modify |
| `web-client/src/e2e/account-server.test.ts` | modify |

Read, and do not edit: `web-client/src/profile/duel-page.ts`; `web-client/src/profile/set-name.ts`;
`web-client/src/profile/profile-fixture.ts` (`duelRowBody`).

## Scope

- `ServerPlayer` gains `readonly duels: readonly Record<string, unknown>[]` — rows built by the merged
  `duelRowBody`, never hand-typed object literals.
- `GET /api/me/duels` (any query string) answers `200` with `{ duels, nextCursor: null }` for the
  resolved player, `401` when none resolves. **`nextCursor` is always present**, per the parser above.
- The path is matched with `startsWith("/api/me/duels")` so `duelsPath`'s query string routes.
- `PUT /api/me/name` reads `{ name }` from the body, sets that player's `displayName`, and answers
  `200` with the **whole profile body** — `meBody` again, carrying the new name — because
  `setDisplayName` parses the response through `profileFromBody` and calls a `200` that is not a
  profile `unavailable`.
- The mutation is per player. Nothing about the other player changes.
- `players` therefore holds mutable display names behind the `readonly` array. Keep the mutation in
  one private function so there is one place a name is written.

## Out of scope

- Canonicalising the name (`ADR-0029` §2 is the real server's job), refusing one, or the `403`
  permanence answer. This double answers `200` for every name; `STORY-0414` never sends a bad one,
  and a refusal path no test reaches is a branch that can rot.
- Filtering or paging `/api/me/duels` by the query. `STORY-0413` proved the query is built right;
  this story sends one page and asserts identity, not pagination.
- Sign-up, sign-in, sign-out, bearer tokens — `TASK-041404` and `TASK-041405`.

## Tests

`account-server.test.ts` — four new, on top of `TASK-041402`'s five.

| Test | Proves |
| --- | --- |
| `answers each device id with its own duels` | Two players with different duel rows: each device's read gets its own `duelId` and `outcome`, and the two `duelId`s are asserted **not equal**. |
| `answers the duels read with a cursor field the parser needs` | The `200` body has `nextCursor` present and `null`, and a real `readDuelPage` over the double answers `kind: "page"` — not `unavailable`. Driven through the merged parser, not by reading the body. |
| `a name set on one player is not set on the other` | `PUT /api/me/name` under device A changes A's `displayName` and leaves B's exactly as it was. Both halves asserted. |
| `the name survives into the next profile read` | The `200` from `PUT` is a whole profile carrying the new name, **and** a following `GET /api/me` carries it too — the write reached the record, not just the response. |

## Acceptance criteria

- [ ] `account-server.test.ts` `answers each device id with its own duels` passes
- [ ] `account-server.test.ts` `answers the duels read with a cursor field the parser needs` passes
- [ ] `account-server.test.ts` `a name set on one player is not set on the other` passes
- [ ] `account-server.test.ts` `the name survives into the next profile read` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +9 passed \(9\)'` exits 0
- [ ] `TASK-041402`'s five tests pass unchanged — no assertion in them is edited, weakened or removed
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. Drop `nextCursor` from the duels body and re-run: `answers the duels read with a cursor field the
   parser needs` must redden. It is the only test that goes through `readDuelPage`, so if it stays
   green the double is being read directly and the parser is not in the loop at all.
2. Make `PUT /api/me/name` write the name to **every** player and re-run: `a name set on one player
   is not set on the other` must redden alone. If `the name survives into the next profile read`
   reddens too, its two players are the same object.
3. Make `PUT /api/me/name` return `{ ok: true }` instead of a profile body: `the name survives into
   the next profile read` must redden on its first half while the `GET` half stays green — that is
   what tells the response apart from the record.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
