---
schema: 2
id: TASK-041103
title: The component tests build their profiles through the fixture
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, profile, test-fixture]
depends_on: [TASK-041102]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +381 passed \(381\)'
  - cd web-client && ! grep -q 'profile: {' src/profile/ProfileStrip.test.tsx
  - cd web-client && ! grep -q 'profile: {' src/profile/profile-provider.test.tsx
  - cd web-client && ! grep -q 'profile: {' src/lobby/Lobby.test.tsx
  - cd web-client && npm run check
---

## Goal

The three files that construct a `ProfileStripState` for a component build its profile with
`aProfile` and its duel lines with `aDuelLine`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify — five profiles, five duel lines |
| `web-client/src/profile/profile-provider.test.tsx` | modify — one profile |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two profiles |

Read, not edited: `web-client/src/profile/profile-fixture.ts`.

## Scope

- These are the three files that break at **`tsc`**, not at runtime: they hold typed
  `ProfileStripState` literals, so a required field added to `PlayerProfile` stops the build. Ten
  errors in `ProfileStrip.test.tsx`, two in `Lobby.test.tsx`, one in `profile-provider.test.tsx` —
  counted by adding the field and running `npm run typecheck`, not estimated.
- Each call names only what its test asserts: the balance tests pass `coinBalance`, the duel-line
  tests pass the `duelId`s and the fields they read back (`outcome`, `coinDelta`, `handsPlayed`,
  `finishedAt`), and nothing passes a `playerId`, because nothing renders one.
- **No test is added, renamed or removed and no assertion is weakened.** The suite reports 381
  before and after.

## Out of scope

- Any new field, and any change to `ProfileStrip.tsx`. **A refusal, not an omission:** the strip
  learns to print a name in `TASK-041115`, against a fixture that already exists by then.
- The e2e files and `App.test.tsx` — measured, they construct no profile and stub no `/api/me`, so
  they neither break nor need touching.

## Tests

**None added.** The gates are the same three as `TASK-041102`, one file at a time.

| Gate | Proves |
| --- | --- |
| `Tests  381 passed (381)` | the thirteen literals were translated, not deleted — a dropped duel line would fail `shows one line per duel` and a dropped profile would fail a balance assertion |
| `! grep -q 'profile: {'` in all three | no typed literal survives. Fails against any file left half-migrated, which is exactly the state that would cost `TASK-041104` its file budget |
| `npm run check` | `tsc` over the three files that would otherwise stop the build in `TASK-041104` |

## Acceptance criteria

- [ ] `npm run --silent test` reports `Tests  381 passed (381)`
- [ ] `grep -c 'profile: {'` returns `0` for each of the three files
- [ ] `grep -c 'aDuelLine(' web-client/src/profile/ProfileStrip.test.tsx` returns `5`
- [ ] All three files import from `../profile/profile-fixture` or `./profile-fixture`
- [ ] `ProfileStrip.tsx`, `profile-provider.tsx` and `Lobby.tsx` are unmodified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
