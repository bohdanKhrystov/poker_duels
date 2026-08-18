---
schema: 2
id: TASK-041102
title: The strip's read tests build through the fixture
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, profile, test-fixture]
depends_on: [TASK-041101]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +381 passed \(381\)'
  - cd web-client && ! grep -q 'profile: {' src/profile/profile-strip.test.ts
  - cd web-client && ! grep -q 'playerId:' src/profile/profile-strip.test.ts
  - cd web-client && ! grep -q 'playerId:' src/profile/profile-no-derivation.test.tsx
  - cd web-client && npm run check
---

## Goal

The two files that stub `/api/me` and `/api/me/duels` bodies build them with `meBody` and
`duelRowBody`, and build their expected values with `aProfile` and `aDuelLine`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-strip.test.ts` | modify — five bodies, two expected states, its duel lines |
| `web-client/src/profile/profile-no-derivation.test.tsx` | modify — three bodies and seven duel rows |

Read, not edited: `web-client/src/profile/profile-fixture.ts`.

## Scope

- Every `ok({ ... })` answering `/api/me` becomes `ok(meBody({ ... }))`; every row inside a
  `/api/me/duels` body becomes `duelRowBody({ ... })`; every expected `PlayerProfile` becomes
  `aProfile({ ... })` and every expected `RecentDuel` becomes `aDuelLine({ ... })`.
- **Each builder call names exactly the fields its test asserts**, and no others. A test that checks
  a balance of `-1` passes `{ coinBalance: -1 }` and lets the id default; a test that checks the
  order of three duels passes three `duelId`s and lets everything else default.
- `profile-no-derivation.test.tsx` keeps passing its own `opponentPlayerId` values (`"player-77"`,
  `"player-88"`) explicitly — they are the strings its scan hunts for, so they may not become
  defaults. This is the one place an `opponentPlayerId` override is correct.
- **No test is added, renamed or removed, and no assertion is weakened.** The suite reports the same
  381 before and after; this ticket's whole content is where the objects come from.

## Out of scope

- Any new field. **A refusal, not an omission** — `TASK-041104` and `TASK-041105` add them to the
  builders, and this ticket exists so that those diffs are one production file plus one test file
  plus the fixture.
- `ProfileStrip.test.tsx`, `profile-provider.test.tsx`, `Lobby.test.tsx` — `TASK-041103`.
- `recent-duels.test.ts` — `TASK-041105` owns it whole.

## Tests

**None added.** This ticket's proof is that the suite does not move and that neither file can
construct a profile literal any more.

| Gate | Proves |
| --- | --- |
| `Tests  381 passed (381)` | the eight assertions that would break under a mistranslated fixture call still pass, and nothing was quietly deleted to get there |
| `! grep -q 'profile: {'` | no typed `PlayerProfile` literal survives in `profile-strip.test.ts`. Fails against a half-migration that moved the bodies and left the expectations |
| `! grep -q 'playerId:'` in both files | no wire body names a player id any more; the builder supplies it. Fails against a body left literal |
| `npm run check` | `tsc`, ESLint and Prettier over both files |

## Acceptance criteria

- [ ] `npm run --silent test` reports `Tests  381 passed (381)`
- [ ] `grep -c 'profile: {' web-client/src/profile/profile-strip.test.ts` returns `0`
- [ ] `grep -c 'playerId:' web-client/src/profile/profile-strip.test.ts` returns `0`
- [ ] `grep -c 'playerId:' web-client/src/profile/profile-no-derivation.test.tsx` returns `0`
- [ ] `grep -c 'opponentPlayerId' web-client/src/profile/profile-no-derivation.test.tsx` returns a
      number greater than `0` — its two hunted ids are still passed explicitly
- [ ] Both files import from `./profile-fixture`
- [ ] No file outside those two differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
