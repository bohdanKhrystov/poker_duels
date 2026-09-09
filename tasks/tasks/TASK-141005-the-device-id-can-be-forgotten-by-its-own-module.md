---
schema: 2
id: TASK-141005
title: The device id can be forgotten, by its own module and no other
type: task
status: done
parent: STORY-1410
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account]
depends_on: [TASK-141004]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/protocol/device-id.test.ts 2>&1 | grep -qF "device-id.test.ts  (8 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qF "one-module-owns-each-storage-key.test.ts  (4 tests)"'
  - sh -c 'cd web-client && test $(grep -c "export function " src/protocol/device-id.ts) -eq 3'
  - sh -c 'cd web-client && test $(grep -c "removeItem" src/protocol/device-id.ts) -eq 1'
  - git diff --exit-code -- web-client/src/account/sign-out.ts web-client/src/protocol/connection.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`device-id.ts` — the one module in this client that may contain the literal `"pd.deviceId"` — gains
`forgetDeviceId`, beside the `readDeviceId` and `writeDeviceId` it already owns. Nothing calls it
yet.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/device-id.ts` | modify |
| `web-client/src/protocol/device-id.test.ts` | modify |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§2, `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` (the gate that makes this
module the only possible home).

## Scope

- `export function forgetDeviceId(storage: Storage): void` — one `storage.removeItem` of
  `DEVICE_ID_STORAGE_KEY` and nothing else.
- KDoc naming the two rules that bound it: it is reachable from `signOut` and from nowhere else, in
  the abandoning case alone (`ADR-0135` §2), and `ADR-0027` §5's stale-client harm stays
  forbidden — *"no reload, no failed handshake, no refused session, no `Welcome` carrying a null
  `deviceId` and no error path may reach it."* Also that this mechanism **enumerates what it
  forgets and never sweeps**: `storage.clear()` is not what this is.
- Nothing else in the module moves. `readDeviceId`'s blank-string handling and `writeDeviceId`'s
  write-once contract are `ADR-0030` §8's and are untouched.

## Out of scope

- **Calling it.** `signOut` gains its call in `TASK-141007`. For one merge this is an exported
  function with no production caller, which is the price of keeping that ticket's `atomic:` set to
  the five files `tsc` actually names.
- **`connection.ts`.** It writes `pd.deviceId` from a `Welcome` under the write-once rule, and it
  is in the `git diff --exit-code` gate to say it did not move.
- **Sweeping other keys.** `ADR-0135` §2: exactly three keys move on a sign-out and a key added
  later is kept unless an ADR says otherwise — including `ADR-0119` §5's skipped bit, which
  `ADR-0131` §6 rules is **not** spent by a new profile.

## Tests

`device-id.test.ts`, **4 today → 7**.

| Test | Proves |
| --- | --- |
| `forgetting the device id leaves nothing to read` | after `writeDeviceId(storage, "d-1")` then `forgetDeviceId(storage)`, `readDeviceId(storage)` is `null` — and, separately, `storage.getItem(DEVICE_ID_STORAGE_KEY)` is `null`, so a `forgetDeviceId` that wrote `""` (which `readDeviceId` also reports as `null`) does not pass |
| `forgetting the device id leaves every other key where it was` | an in-memory `Storage` holding `pd.deviceId`, `pd.sessionToken`, `pd.roomCode` and `pd.nameAskSkipped`; after one call, the other **three** are read back with their exact values and `storage.length` is **3**. Three survivors rather than one, because one survivor cannot tell a targeted `removeItem` from a `clear()` that happened to miss |
| `forgetting an id this browser never held changes nothing` | on a storage holding only the other three keys, the call throws nothing, `readDeviceId` is `null`, and `storage.length` is still **3** |

Use the in-memory `Storage` double the file already uses — never the global `localStorage`, which
Node 24 shadows and leaves inert under Vitest (`DEC-032`).

## What would still pass if the coder were wrong

- **`forgetDeviceId` implemented as `storage.clear()`** passes the first test entirely and fails
  the second on all three survivors and on `storage.length`. That is the whole reason the second
  test seeds four keys.
- **`forgetDeviceId` implemented as `writeDeviceId(storage, "")`** passes a `readDeviceId(...) ===
  null` assertion, because `readDeviceId` trims to test. The first test's second assertion — the
  raw `getItem` — is what refuses it.
- **A second module writing the key** leaves every test here green and reddens
  `one-module-owns-each-storage-key.test.ts`, whose `pd.deviceId` row asserts the exact file set
  `["device-id.ts"]`. Its count is pinned at **4** (measured at `c6a41e6d`) to say it was neither
  edited nor skipped.
- **Exporting a fourth function, or a second `removeItem`,** is caught by the two `grep -c` gates:
  three `export function` and exactly one `removeItem` in the module.

## Acceptance criteria

- [ ] `device-id.test.ts` reports `(8 tests)` and all pass — the ticket asked for 7, but review
      proved a conditional `removeItem` passed all 7 and the whole 1238-test suite, so an eighth
      test pins `ADR-0135` §2's *unconditional* removal by observing the call with the key absent
- [ ] `one-module-owns-each-storage-key.test.ts` reports `(4 tests)` and all pass
- [ ] `grep -c "export function "` on `device-id.ts` is 3, and `grep -c "removeItem"` is 1
- [ ] `git diff --exit-code` over `sign-out.ts` and `connection.ts` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
