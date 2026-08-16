---
schema: 2
id: TASK-031102
title: The profile read states the balance the server sent, sign and all
type: task
status: ready
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, http, profile, coins]
depends_on: [TASK-031101]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +330 passed \(330\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks /api/me with the device id from the key the socket module owns'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the balance the server sent, sign and all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers no-profile from an empty browser and from a 401'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers unavailable when the body is not a profile'
  - cd web-client && npm run check
---

## Goal

`GET /api/me` becomes a `PlayerProfile` the client can hold: the player id and the **signed** coin
balance, read with the device id stored under the key `src/protocol/` owns — never clamped, never
re-signed, never recomputed.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile.ts` | create |
| `web-client/src/profile/profile.test.ts` | create |
| `web-client/src/profile/api.ts` | read — `readFromApi`, `ApiFetch`, `ApiRead` |
| `web-client/src/protocol/device-id.ts` | read — `DEVICE_ID_STORAGE_KEY`, `readDeviceId` |
| `web-client/src/protocol/device-id.test.ts` | read — copy its `inMemoryStorage()` helper verbatim |

## Scope

- `profile.ts` declares the shape of the endpoint's body — hand-written, and one of the two the
  epic's non-negotiables allow, because `/api/me` is not a `ServerMessage`:

  ```ts
  /** What `GET /api/me` answers. Contracted in `docs/protocol.md`, not generated. */
  export interface PlayerProfile {
    readonly playerId: string;
    /** Signed, and negative is a correct answer (`ADR-0014`). Never clamped. */
    readonly coinBalance: number;
  }

  export type ProfileRead =
    | { readonly kind: "profile"; readonly profile: PlayerProfile }
    | { readonly kind: "no-profile" }
    | { readonly kind: "unavailable" };

  export async function readProfile(deps: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
  }): Promise<ProfileRead>;
  ```

- It reads the device id with `readDeviceId(deps.storage)` — the exported function, so the key is
  named in one place in this client and two identities cannot appear for one player — and hands it
  to `readFromApi` with the path `"/api/me"`.
- `no-profile` and `unavailable` pass straight through from `readFromApi`.
- A `body` that is not `{ playerId: string, coinBalance: number }` is `unavailable`, not a profile
  with holes in it. Build the returned object from those two fields only.
- The `Storage` is a parameter, never the `localStorage` global — `DEC-032` records why the global
  cannot be relied on under Vitest, and `TASK-030304` set this way out.

## Out of scope

- Rendering. `coinBalance` reaches a screen through `TASK-031105` (the words) and `TASK-031107`
  (the strip); nothing here formats anything.
- Adding, subtracting or comparing a balance with anything. The number is the server's; a client
  that arithmetic'd it would be asserting a fact about the economy (`EPIC-03`, `TASK-021102`).
- The recent duels — `TASK-031103`.
- Refreshing after a duel ends. The strip reads once per mount; anything live is `EPIC-04`'s.

## Tests

`web-client/src/profile/profile.test.ts`, describe block `"the profile read"`. Copy
`device-id.test.ts`'s `inMemoryStorage()` helper verbatim — that duplication is deliberate and
already several files deep — and copy `api.test.ts`'s `answering()` / `ok()` helpers.

| Test | Proves |
| --- | --- |
| `asks /api/me with the device id from the key the socket module owns` | with `DEVICE_ID_STORAGE_KEY` (the imported constant, never the literal `"pd.deviceId"`) set to `"d-1"` the header is `d-1`, and with `"d-2"` it is `d-2`; the path is exactly `/api/me` and exactly one call is made. **Two ids**, because one cannot tell a stored value from a constant |
| `takes the balance the server sent, sign and all` | `coinBalance: -1` answers `-1` and `coinBalance: 7` answers `7`. One negative, because `−1` is the answer `ADR-0014` says must survive, and two values, because one cannot tell a read field from a literal |
| `answers no-profile from an empty browser and from a 401` | both doors to the same state, written out: an empty `Storage`, and a stored id the server answers `401` to |
| `answers unavailable when the body is not a profile` | three bodies, each written out: `{}`, `{ playerId: 1, coinBalance: 3 }`, `{ playerId: "p", coinBalance: "x" }` |

Four tests added. Three hundred and twenty-six exist, so the suite reports **330**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 330 passed (330)` | four ran and nothing else moved |
| the four `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Read the device id from `"pd.device-id"` instead of `readDeviceId` → `asks /api/me with the
   device id from the key the socket module owns` fails: nothing is found, so nothing is sent.
2. Clamp with `Math.max(0, coinBalance)` → `takes the balance the server sent, sign and all` fails
   on `-1` while its `7` half still passes. This is the defect the test exists for.
3. Accept any object as a profile → `answers unavailable when the body is not a profile` fails.

## Acceptance criteria

- [ ] `the profile read > asks /api/me with the device id from the key the socket module owns` passes
- [ ] `the profile read > takes the balance the server sent, sign and all` passes
- [ ] `the profile read > answers no-profile from an empty browser and from a 401` passes
- [ ] `the profile read > answers unavailable when the body is not a profile` passes
- [ ] The test imports `DEVICE_ID_STORAGE_KEY` and never spells the key as a literal
- [ ] `profile.ts` contains no `Math.`, no `+`, no `-` applied to `coinBalance`
- [ ] `web-client/src/profile/api.ts` is byte-identical to what it was
- [ ] `npm run --silent test` reports `Tests  330 passed (330)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
