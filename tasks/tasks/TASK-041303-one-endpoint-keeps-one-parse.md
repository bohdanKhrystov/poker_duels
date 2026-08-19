---
schema: 2
id: TASK-041303
title: One endpoint keeps one parse — the strip's read delegates
type: task
status: ready
parent: STORY-0413
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, history, http, parse]
depends_on: [TASK-041302]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/recent-duels.test.ts 2>&1 | grep -qE 'Tests +8 passed \(8\)'
  - git diff origin/develop -- web-client/src/profile/recent-duels.test.ts | grep -q '^-' && exit 1 || exit 0
  - cd web-client && npm run check
---

## Goal

`GET /api/me/duels` has exactly one parse in the client again: `readRecentDuels` delegates to
`readDuelPage` and its own copy is gone.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/recent-duels.ts` | modify — the body of `readRecentDuels` becomes a delegation; the row parse is deleted |

Read, not edited: `web-client/src/profile/duel-page.ts`,
`web-client/src/profile/recent-duels.test.ts` (the eight tests that must pass untouched).

## Scope

- `readRecentDuels` becomes:

  ```ts
  const page = await readDuelPage({ ...deps, query: WHOLE_RECORD });
  return page.kind === "page" ? { kind: "duels", duels: page.duels } : page;
  ```

  and the whole of its former body — the `readFromApi` call, the `duelsList` walk, the six field
  checks and the outcome check — is deleted.
- `RecentDuel`, `DuelOutcomeWord` and `RecentDuelsRead` stay declared in this file and stay exported
  from it, so `profile-fixture.ts`, `profile-text.ts` and `profile-strip.ts` are untouched.
- `RecentDuelsRead` keeps exactly the three variants it has, and `readRecentDuels` keeps exactly the
  signature it has. **The strip's contract does not move in this story**: the lobby is not
  `STORY-0413`'s business, and a read the history screen widened is a read that can break the lobby.
  `nextCursor` is dropped here on purpose — a strip that shows a handful of recent results has no
  page to walk to.
- The KDoc says why there are two functions over one endpoint: different outcome sets, one parse.

## Out of scope

- Deleting `readRecentDuels` and pointing `profile-strip.ts` at `readDuelPage`. **A refusal, not an
  omission:** it would touch `profile-strip.ts`, `profile-strip.test.ts` and
  `profile-no-derivation.test.tsx`, and the lobby is a surface this story has no reason to disturb.
  Not ticketed.
- `readFromApi` itself, which `profile.ts` still uses and which this ticket does not touch.
- Adding a test. This ticket's whole claim is that behaviour is unchanged, and the eight merged tests
  in `recent-duels.test.ts` are the assertion — they now run against `duel-page.ts`'s parse. A new
  test here would assert the same thing a second time.

## Tests

**None added.** The mutation this ticket guards against is *two parses of one endpoint drifting
apart*, and it is closed structurally rather than by an assertion: after this ticket there is one
parse, so there is nothing to drift. The eight merged tests in `recent-duels.test.ts` are what proves
the delegation preserved behaviour.

**Corrected during the work.** The original `verify:` demanded `recent-duels.test.ts` be byte-identical,
which is not achievable and was wrong to ask. `readDuelPage` requires `nextCursor`, the server has
always sent it (`docs/protocol.md`: *"Always present"*), and the strip's mocks predate the field — so
delegating exposed mocks that were **unfaithful to the wire**. Adding `nextCursor: null` to them makes
them match what the server sends. The check is now that no line is *removed*: mock bodies gain a field,
and no assertion moves.

The eight, all of which must pass untouched: `asks /api/me/duels with no limit of its own`,
`keeps every field a row carries except the opponent id`, `carries a named opponent and a nameless
one from the same body`, `answers unavailable when a row says nothing about the opponent name`,
`answers an empty list for a player who has never duelled`, `reads every outcome the server can
send`, `takes the coin delta signed, including the zero of a draw`, `answers unavailable when a row
is not a duel`.

## Acceptance criteria

- [ ] `npm run test -- src/profile/recent-duels.test.ts` reports `Tests  8 passed (8)`
- [ ] No line is **removed** from `recent-duels.test.ts` — the diff adds `nextCursor` to mock
      bodies and changes no assertion
      — not one assertion was adjusted, and committing the change does not hide it
- [ ] `grep -c 'readFromApi' web-client/src/profile/recent-duels.ts` returns `0`
- [ ] `grep -c 'typeof (row' web-client/src/profile/recent-duels.ts` returns `0` — the row-shape
      check lives in one file now
- [ ] `readRecentDuels` still takes `{ fetch, storage }` and still answers `RecentDuelsRead` with its
      three variants, so `profile-strip.ts` is unchanged
- [ ] No file outside `web-client/src/profile/recent-duels.ts` differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
