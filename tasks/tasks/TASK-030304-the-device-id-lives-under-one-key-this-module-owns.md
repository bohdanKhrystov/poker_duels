---
schema: 2
id: TASK-030304
title: The device id lives under one storage key this module owns
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, protocol, identity]
depends_on: [TASK-030303]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +31 passed \(31\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes under the one key the profile endpoint will read'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'treats a blank stored value as no device id'
  - grep -qF 'pd.deviceId' web-client/src/protocol/device-id.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

The device id the server issues is read and written through one exported key, so the socket and
`GET /api/me` can never disagree about who this browser is.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/device-id.ts` | create |
| `web-client/src/protocol/device-id.test.ts` | create |

## Scope

- `device-id.ts` exports exactly three things:

  ```ts
  /**
   * The one key this browser's device id is stored under.
   *
   * Exported because `STORY-0311` sends the same value as the `X-Device-Id`
   * header on `GET /api/me`. Two keys would mean two identities for one player.
   */
  export const DEVICE_ID_STORAGE_KEY = "pd.deviceId";

  /** The device id this browser holds, or `null` on a first visit. */
  export function readDeviceId(storage: Storage): string | null;

  /** Remember the device id the server issued. */
  export function writeDeviceId(storage: Storage, deviceId: string): void;
  ```

- `readDeviceId` returns `null` for an absent value **and** for one that is blank once trimmed. A
  blank id would be sent as a `Hello.deviceId` the server cannot resolve, and one rule in one place
  is cheaper than a guard at every call site.
- `readDeviceId` returns the stored value verbatim otherwise — do not trim what it returns, only
  what it tests.
- `storage` is a parameter, not `localStorage` reached for directly: the caller passes it
  (`TASK-030311` passes `localStorage`) and a test passes jsdom's. No default value.
- `ADR-0012` accepts what this costs — clearing site data or switching browser loses the profile —
  so nothing here tries to be cleverer than a single key in `Storage`.

## Out of scope

- Reading it during a handshake — `TASK-030307`. Writing it on `Welcome` — `TASK-030309`.
- The `X-Device-Id` header and any HTTP call — `STORY-0311`.
- A session token. `ADR-0027` adds `Hello.sessionToken` and a bearer token in `EPIC-04`; it is a
  second credential with its own storage question and none of it is decided by this key.
- Minting an id. The server mints; the client is told. A client that generated its own would be
  asserting an identity, which `ADR-0002` forbids.

## Tests

`web-client/src/protocol/device-id.test.ts`, describe block `"the device id store"`. Four `it`
blocks, with `beforeEach(() => localStorage.clear())`:

| Test | Proves |
| --- | --- |
| `reads nothing on a first visit` | `readDeviceId(localStorage)` is `null` with nothing stored |
| `reads back what was written` | after `writeDeviceId(localStorage, "d-1")`, `readDeviceId(localStorage)` is `"d-1"` |
| `treats a blank stored value as no device id` | after `localStorage.setItem(DEVICE_ID_STORAGE_KEY, "   ")`, `readDeviceId(localStorage)` is `null` |
| `writes under the one key the profile endpoint will read` | after `writeDeviceId(localStorage, "d-1")`, `localStorage.getItem("pd.deviceId")` is `"d-1"` |

Four tests. Twenty-seven exist, so the suite reports **31**.

The last test uses the **literal string** `"pd.deviceId"`, not `DEVICE_ID_STORAGE_KEY`. Asserting a
constant against itself proves nothing; the literal is what `STORY-0311` will have to match.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 31 passed (31)` | the four tests ran and nothing earlier was displaced |
| the two `--reporter=verbose` greps | the key assertion and the blank rule exist by name |
| `grep 'pd.deviceId' device-id.ts` | the key is a literal in this module, not assembled at a call site |
| `npm run check` | typecheck, lint, format and test pass together |

**Name the edit that makes each assertion red:**

1. Change `DEVICE_ID_STORAGE_KEY` to `"pd.device"` → `writes under the one key the profile endpoint
   will read` fails, `expected null to be "d-1"`. Revert.
2. Delete the trim check from `readDeviceId` → `treats a blank stored value as no device id` fails,
   `expected "   " to be null`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the device id store > reads nothing on a first visit` passes
- [ ] `the device id store > reads back what was written` passes
- [ ] `the device id store > treats a blank stored value as no device id` passes
- [ ] `the device id store > writes under the one key the profile endpoint will read` passes
- [ ] `npm run --silent test` reports `Tests  31 passed (31)`
- [ ] `device-id.ts` contains no reference to `localStorage`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
