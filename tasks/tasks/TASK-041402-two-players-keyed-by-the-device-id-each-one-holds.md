---
schema: 2
id: TASK-041402
title: Two players, keyed by the device id each one holds, and every request written down
type: task
status: backlog
parent: STORY-0414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, e2e, test, http]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'answers each device id with its own player'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'refuses a device id it has never issued'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts --reporter=verbose 2>&1 | grep -qF 'writes down the path headers and body of every request'
  - cd web-client && npm run check
---

## Goal

An `ApiFetch` double that knows **two** players, tells them apart by the device id the request
carried, and records every request it was given.

## Why this exists

`STORY-0414`'s whole claim is *the second browser reads back the same balance*. A double that
answers one canned body to everybody makes that sentence true by construction and proves nothing.
Two players with different values is what makes a wrong answer **possible**, and therefore what makes
a right one evidence.

The bodies are built with the merged `meBody` from `src/profile/profile-fixture.ts`, not hand-typed.
`STORY-0312`'s rule — *a fixture that can disagree with the protocol is worse than no fixture* — is
answered for the socket by a generated script; for HTTP the merged answer is the fixture builders,
used by ten test files already. One place to change when `GET /api/me` changes.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/account-server.ts` | create |
| `web-client/src/e2e/account-server.test.ts` | create |

Read, and do not edit: `web-client/src/profile/api.ts` (`ApiFetch`, `ApiResponse`);
`web-client/src/profile/profile-fixture.ts` (`meBody`); `web-client/src/profile/profile.ts`.

## Scope

- `ServerPlayer`: `playerId`, `deviceId`, `coinBalance`, `displayName: string | null`. Every field a
  `readonly val`.
- `RecordedRequest`: `path`, `method` (`"GET"` when the caller sent none), `headers`, `body: string | null`.
- `accountServer(players: readonly ServerPlayer[]): AccountServer`, where `AccountServer` carries
  `readonly fetch: ApiFetch` and `readonly requests: readonly RecordedRequest[]`.
- Resolution is **one** private function: the `X-Device-Id` header names a player, or nothing does.
  `TASK-041405` is where a bearer token outranks it; leave the seam, do not build it here.
- `GET /api/me` answers `200` with `meBody({ playerId, coinBalance, displayName, displayNameRemoved: false, deviceRouteLive: true })`
  for the resolved player, and `401` with an empty body when none resolves.
- Every call — resolved or refused, known path or not — appends to `requests` **before** any routing
  decision, so a refused request is still on the record.
- Any other path answers `500`. `TASK-041403`, `TASK-041404` and `TASK-041405` replace that arm.

## Out of scope

- `/api/me/duels` and `/api/me/name` — `TASK-041403`.
- Sign-up and sign-in — `TASK-041404`.
- Bearer tokens and sign-out — `TASK-041405`. **Do not anticipate them**: an `Authorization` branch
  written here has no test that can reach it, and an untested branch in the double that decides who
  a player is is the one defect this story cannot survive.

## Tests

`account-server.test.ts`

| Test | Proves |
| --- | --- |
| `answers each device id with its own player` | Two players, two device ids, two `GET /api/me` calls: each body's `playerId`, `coinBalance` and `displayName` are that device's own. Asserted **both ways** — the second player's balance is also checked *not* to equal the first's, so one shared constant fails. |
| `refuses a device id it has never issued` | `X-Device-Id: "device-nobody"` answers `401`, and the body is not a profile. |
| `refuses a request carrying no device id at all` | Headers with no `X-Device-Id` answer `401` — the case `readFromApi` normally short-circuits, so the double must not fall through to somebody's profile. |
| `writes down the path headers and body of every request` | After one `GET /api/me` and one call to an unknown path, `requests` has length 2, in order, with the paths, the `X-Device-Id` header and `body === null` for a GET. |
| `answers an unknown path with 500` | A path this ticket does not route is a loud failure, not a silent `200`. |

## Acceptance criteria

- [ ] `account-server.test.ts` `answers each device id with its own player` passes
- [ ] `account-server.test.ts` `refuses a device id it has never issued` passes
- [ ] `account-server.test.ts` `refuses a request carrying no device id at all` passes
- [ ] `account-server.test.ts` `writes down the path headers and body of every request` passes
- [ ] `account-server.test.ts` `answers an unknown path with 500` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/account-server.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'` exits 0
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. **The two-player assertion must be able to fail.** Give both fixture players the *same*
   `coinBalance` and re-run: `answers each device id with its own player` must go red on its
   not-equal half. If it stays green, the test is asserting a constant and the story's central
   comparison is worthless. Restore afterwards.
2. **Pick fixture values that are mutually independent**, the rule `profile-fixture.ts`'s own doc
   comment states: no two of the balances add, subtract, double or halve into a third, and neither
   is `0` — a value the bug leaves unchanged cannot detect it.
3. Make the recorder append **after** routing instead of before, and `writes down the path headers
   and body of every request` must redden on the refused call. If it does not, the second call in
   that test is not reaching a refusal.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
