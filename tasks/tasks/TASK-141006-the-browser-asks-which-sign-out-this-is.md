---
schema: 2
id: TASK-141006
title: The browser asks the server which sign-out this is
type: task
status: done
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, account]
depends_on: [TASK-141005]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/device-standing.test.ts 2>&1 | grep -qF "device-standing.test.ts  (7 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qF "one-module-owns-each-storage-key.test.ts  (4 tests)"'
  - sh -c 'cd web-client && ! grep -q "readFromApi" src/account/device-standing.ts'
  - sh -c 'cd web-client && ! grep -q "pd.deviceId" src/account/device-standing.ts'
  - sh -c 'cd web-client && test $(grep -c "/api/me/device" src/account/device-standing.ts) -eq 1'
  - git diff --exit-code -- web-client/src/profile/api.ts web-client/src/profile/profile.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

One new client module reads `GET /api/me/device` and answers a plain `boolean`, `false` for every
form of *not told*, without going anywhere near `readFromApi`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/device-standing.ts` | create |
| `web-client/src/account/device-standing.test.ts` | create |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§6, `web-client/src/account/revoke-device.ts` (the shipped shape this copies),
`web-client/src/profile/api.ts` (`ApiFetch`, `ApiResponse`),
`web-client/src/protocol/device-id.ts`.

## Scope

```ts
export async function readDeviceStanding(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<boolean>;
```

- Reads the session token with `readSessionToken`. **With no token it makes no request and answers
  `false`.** The route is session-required and answers `401` to a caller with no session, and
  `401` is a keep (`ADR-0135` §6), so the short circuit and the round trip give the same answer.
  This is `revoke-device.ts`'s shipped shape, line for line.
- With a token, reads the device id with `readDeviceId` and sends
  `GET /api/me/device` carrying `Authorization: Bearer <token>` **always**, and `X-Device-Id`
  **only when one is held** — never as an empty string, and never omitting the request.
- `200` with a body whose `signOutHandsANewProfile` is a `boolean` answers that boolean.
  **Everything else answers `false`**: a `401`, any other status, a body missing the field, a body
  whose field is not a boolean, a `json()` that rejects, and a `fetch` that rejects. `ADR-0135` §6:
  *"An absent field, a `401`, an *unavailable* read, a rejected `fetch`, a read that has not
  returned yet, and a client too old to ask all mean the same thing."*
- KDoc must state the trap by name: **this read must not go through `readFromApi`**, because that
  helper answers `no-profile` *without making a request* when the browser holds no device id —
  which is exactly the browser whose answer is `true`. The `verify:` block refuses the identifier
  in the source, and the second test refuses the behaviour.
- The module writes to storage **not at all**, exactly as `revoke-device.ts` says of itself.

## Out of scope

- **Calling it.** `main.tsx` binds it in `TASK-141012`; nothing imports it before then except its
  own test.
- **A three-way outcome type.** `revoke-device.ts` answers a union because its four statuses mean
  four different things to a screen; here every not-told collapses to one answer by decision
  (`ADR-0135` §6), and a union would invite a caller to treat *unavailable* as a third case.
- **`readFromApi`, `profile.ts` and `apiFetch`.** `git diff --exit-code` covers the first two;
  the wrapper is not used because it would add the bearer header a second time behind this
  module's back.
- **Any storage write.** `pd.deviceId` is forgotten by `signOut` alone (`TASK-141007`).

## Tests

`web-client/src/account/device-standing.test.ts`, **7 tests**, over a recording `ApiFetch` double
and the in-memory `Storage` the account tests already use.

| Test | Proves |
| --- | --- |
| `a browser holding no session asks nothing and keeps its profile` | `calls.length` is **0** and the answer is `false`, on a storage that **does** hold `pd.deviceId` — so this is the no-session rule and not the no-device one |
| `a browser holding a token but no device id still asks` | `calls.length` is **1**, the path is `/api/me/device`, and the header keys are exactly `["Authorization"]`. This is the `readFromApi` trap inverted, and it is the one test that would go green for the wrong reason if the module reused that helper |
| `a browser holding both presents both` | header keys sorted are exactly `["Authorization", "X-Device-Id"]`, `Authorization` is `Bearer <the stored token>` and `X-Device-Id` is the stored id — two different literal values, so a module echoing one into both slots fails |
| `the answer is the one the server gave` | two calls against two doubles: a body of `{ signOutHandsANewProfile: true }` answers `true`, and `{ signOutHandsANewProfile: false }` answers `false`. Both, in one test, because a module returning a constant satisfies either alone |
| `a refusal is a keep` | `401` answers `false`, and the same double with a `200` and a `true` body answers `true` in the same test — the positive control that says the `false` came from the status and not from the module |
| `an answer this client cannot read is a keep` | four bodies in one test: the field missing, the field a string `"true"`, the field `null`, and a `json()` that rejects. All four answer `false`. A universal claim is a promise to enumerate |
| `a server that never answers is a keep` | a `fetch` that rejects answers `false`, and a `500` answers `false` |

## What would still pass if the coder were wrong

- **`readDeviceStanding` returning a constant `false`** passes six of the seven and fails
  `the answer is the one the server gave`, which asserts both polarities against two doubles.
- **Reusing `readFromApi`** answers `false` for a browser with a token and no device id — the
  `true` case — while making no request at all. Every status test still passes; only
  `a browser holding a token but no device id still asks` sees it, because it asserts a **call
  count** and not an answer.
- **Sending `X-Device-Id: ""`** passes an answer assertion and fails the exact header-key set in
  the second test.
- **Trusting a non-boolean field** — `body.signOutHandsANewProfile` used with `!!` — turns the
  string `"true"` and the string `"false"` both into `true`, and `an answer this client cannot read
  is a keep` enumerates both a string and a `null` for that reason.
- **Writing to storage** leaves every test here green and is caught by
  `one-module-owns-each-storage-key.test.ts`, pinned at **4** (measured at `c6a41e6d`), plus the
  `! grep -q "pd.deviceId"` gate on the new module.

## Acceptance criteria

- [ ] `device-standing.test.ts` reports `(7 tests)` and all pass
- [ ] `one-module-owns-each-storage-key.test.ts` reports `(4 tests)` and all pass
- [ ] `! grep -q "readFromApi"` and `! grep -q "pd.deviceId"` on `device-standing.ts` exit 0
- [ ] `grep -c "/api/me/device"` on `device-standing.ts` is 1
- [ ] `git diff --exit-code` over `profile/api.ts` and `profile/profile.ts` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
