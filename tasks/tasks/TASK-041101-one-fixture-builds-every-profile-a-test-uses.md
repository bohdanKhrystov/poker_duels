---
schema: 2
id: TASK-041101
title: One fixture builds every profile and duel line a test uses
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [client, profile, test-fixture]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +381 passed \(381\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds a profile carrying every field PlayerProfile declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds a duel line carrying every field RecentDuel declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds bodies carrying every field the wire declares, opponent id included'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a test bend any field it names'
  - cd web-client && npm run check
---

## Goal

Four builders exist for the two profile types and the two wire bodies behind them, and
`profile.test.ts` builds through them — so that `TASK-041104` can add a required field to
`PlayerProfile` by editing three files instead of seven.

## Why this comes first

`PlayerProfile` and `RecentDuel` gain required fields in `TASK-041104` and `TASK-041105`. Measured,
not guessed: adding them today breaks `ProfileStrip.test.tsx` (10 type errors), `Lobby.test.tsx` (2)
and `profile-provider.test.tsx` (1) at `tsc`, and breaks eight assertions at runtime across
`profile.test.ts`, `profile-strip.test.ts`, `profile-no-derivation.test.tsx` and
`recent-duels.test.ts`, because a strict parse refuses a body that lacks the field. Seven files is
two and a half tickets' worth of budget spent on mechanical edits. Three tickets move the
construction behind one module first; the field then lands in one place.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-fixture.ts` | create |
| `web-client/src/profile/profile-fixture.test.ts` | create |
| `web-client/src/profile/profile.test.ts` | modify — every body and every expected profile |

Read, not edited: `web-client/src/result/outcome-fixture.ts` (the shape a fixture takes here),
`web-client/src/profile/profile.ts` (`PlayerProfile`), `web-client/src/profile/recent-duels.ts`
(`RecentDuel`).

## Scope

Four builders, each taking an overrides object and spreading it last:

```ts
export function aProfile(overrides: Partial<PlayerProfile> = {}): PlayerProfile
export function aDuelLine(overrides: Partial<RecentDuel> = {}): RecentDuel
export function meBody(overrides: Record<string, unknown> = {}): Record<string, unknown>
export function duelRowBody(overrides: Record<string, unknown> = {}): Record<string, unknown>
```

- **Two kinds, deliberately.** `aProfile`/`aDuelLine` build the *parsed* types a component takes as
  props; `meBody`/`duelRowBody` build the *wire* bodies a fake `fetch` answers with. They are not
  derived from one another: the body carries fields the parse drops, and that difference is the
  thing several tests exist to check.
- **`duelRowBody` carries `opponentPlayerId`.** The parse drops it (`recent-duels.ts`) and
  `profile-no-derivation.test.tsx` exists to catch it reaching a screen. A body builder that omitted
  it would quietly disarm that guard.
- **The bodies are `Record<string, unknown>`**, not the parsed types, so a test can hand the parse a
  wrong-typed field (`meBody({ coinBalance: "x" })`) and still get a body.
- **Defaults are values no test asserts:** `playerId: "p-fixture"`, `coinBalance: 41`,
  `duelId: "duel-fixture"`, `opponentPlayerId: "player-fixture"`, `outcome: "WON"`,
  `coinDelta: 1`, `handsPlayed: 23`, `finishedAt: "2026-02-03T04:05:06Z"`. A test asserts only a
  value it passed itself, even when that value equals the default — `STORY-0411` forbids a test
  whose assertion is only ever the fixture's default.
- `profile.test.ts` migrates: every `ok({ ... })` body becomes `ok(meBody({ ... }))` carrying only
  the fields that test is about, and every expected profile becomes `aProfile({ ... })`.
- **Two literal bodies stay literal in `profile.test.ts`** — the ones proving a wrong-typed
  `playerId` and a wrong-typed `coinBalance` are refused. A builder that always supplies a
  well-typed field cannot express a field's absence or its wrongness, so those two keep their object
  literals, and this ticket says so rather than leaving a reviewer to wonder.
- No production file changes. No test is added to or removed from `profile.test.ts`, and no
  assertion in it is weakened: the same four tests pass with the same expectations, built
  differently.

## Out of scope

- `displayName`, `displayNameRemoved` and `opponentDisplayName`. **A refusal, not an omission:**
  this ticket must be a no-op for the suite's behaviour, so that `TASK-041104`'s diff is the field
  and nothing else. The builders gain the fields there.
- `profile-strip.test.ts`, `profile-no-derivation.test.tsx` — `TASK-041102`.
  `ProfileStrip.test.tsx`, `profile-provider.test.tsx`, `Lobby.test.tsx` — `TASK-041103`.
- `recent-duels.test.ts`. It is the file whose subject changes in `TASK-041105`, and that ticket
  owns it whole rather than half-migrating it here.

## Tests

`web-client/src/profile/profile-fixture.test.ts`, describe block `"the profile fixtures"`, modelled
on `result/outcome-fixture.test.ts`.

| Test | Proves |
| --- | --- |
| `builds a profile carrying every field PlayerProfile declares` | `Object.keys(aProfile()).sort()` is exactly `["coinBalance", "playerId"]`. Fails against a builder that forgets a field, and — the point of it — against `TASK-041104` adding a field to the type and not to the builder |
| `builds a duel line carrying every field RecentDuel declares` | `Object.keys(aDuelLine()).sort()` is exactly `["coinDelta", "duelId", "finishedAt", "handsPlayed", "outcome"]`. Fails the same way for `TASK-041105` |
| `builds bodies carrying every field the wire declares, opponent id included` | `meBody()` has the keys `GET /api/me` documents, and `duelRowBody()` has `opponentPlayerId` among its keys. Fails against a `duelRowBody` built from `aDuelLine`, which is the shortcut that disarms `profile-no-derivation.test.tsx` |
| `lets a test bend any field it names` | Each of the four builders returns the override it was given, asserted with **two different values per builder** (`aProfile({ coinBalance: 5 }).coinBalance` is `5` and `aProfile({ coinBalance: -2 }).coinBalance` is `-2`), and a field it was not given keeps its default. Fails against a builder that spreads its overrides first and against one that ignores them; one value could not tell an override from a constant |

Four tests added to a suite of 377, so it reports **381**.

## Acceptance criteria

- [ ] `the profile fixtures > builds a profile carrying every field PlayerProfile declares` passes
- [ ] `the profile fixtures > builds a duel line carrying every field RecentDuel declares` passes
- [ ] `the profile fixtures > builds bodies carrying every field the wire declares, opponent id
      included` passes
- [ ] `the profile fixtures > lets a test bend any field it names` passes, with two values per
      builder
- [ ] `grep -c 'playerId:' web-client/src/profile/profile.test.ts` returns `2` — the two refusal
      bodies, and nothing else
- [ ] `web-client/src/profile/profile.test.ts` imports `meBody` and `aProfile` from
      `./profile-fixture`
- [ ] No file under `web-client/src/` outside `web-client/src/profile/` differs
- [ ] `profile.ts`, `recent-duels.ts`, `profile-strip.ts` and every `.tsx` file are unmodified
- [ ] `npm run --silent test` reports `Tests  381 passed (381)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
