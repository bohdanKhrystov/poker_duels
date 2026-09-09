---
schema: 2
id: TASK-141018
title: The storage reads sit inside the catch
type: task
status: ready
parent: STORY-1410
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-141011]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/device-standing.test.ts 2>&1 | grep -qF "device-standing.test.ts  (9 tests)"'
  - sh -c 'cd web-client && test $(grep -c "catch" src/account/device-standing.ts) -eq 1'
  - git diff --exit-code -- web-client/src/protocol/session-token.ts web-client/src/protocol/device-id.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`readDeviceStanding` answers `false` when reading storage throws, as it already does for every
other unreadable answer. Today those two reads sit outside its `try`, so that one case rejects
instead.

## Why this exists

`device-standing.ts`'s KDoc enumerates what answers `false`: *"a `401`, any other status, a missing
field, a field that is not a `boolean`, a `json()` that rejects, and a `fetch` that rejects all mean
the same thing (`ADR-0135` §6)."* Every one of those is inside the `try`. But the function opens
with

```ts
const token = readSessionToken(request.storage);
...
const deviceId = readDeviceId(request.storage);
```

and both helpers call `storage.getItem` with no guard. A browser that throws on storage access —
site data blocked, or a private window on some engines — throws there, before the `try`, and the
returned promise **rejects**.

Found while reviewing `TASK-141011`. That ticket's provider calls `read().then(...)` with no
`.catch`, on the stated contract that `readDeviceStanding` never rejects. The contract is true of
everything the KDoc lists and false of this one path, so the provider gets an unhandled rejection.

**The consequence today is benign and that is the reason this is `XS` and not urgent**: the provider
holds `false` until the read lands, and a rejected read never moves it, so the browser keeps the
profile it owns — `ADR-0135` §6's safe direction. What is wrong is that the safety comes from the
initial value rather than from the function honouring its own contract, and an unhandled rejection
is noise in every console it reaches.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/device-standing.ts` | modify |
| `web-client/src/account/device-standing.test.ts` | modify |

Read, do not edit: `web-client/src/protocol/session-token.ts`,
`web-client/src/protocol/device-id.ts`,
`docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md` §6.

## Scope

- Move both storage reads inside the existing `try`. The `catch` already returns `false`; it gains
  no second branch and no logging. The function keeps exactly one `catch`, which the gate pins.
- The short circuit is unchanged: a `null` token still returns `false` without sending a request.
- Two tests, driving a `Storage` double whose `getItem` throws — one for the token read, one for the
  device-id read — each asserting the answer is `false` and that no request was sent. The count gate
  moves 7 → 9.

## Out of scope

- **`session-token.ts` and `device-id.ts`.** Guarding `getItem` at the source would change every
  caller's behaviour, and the callers have not been surveyed. The `git diff --exit-code` above says
  this ticket does not touch them. Whether those helpers should tolerate a throwing `Storage` at all
  is a wider question than this ticket, and is not answered by making one caller safe.
- **A `.catch` on the provider.** `TASK-141011` deliberately has none, on this function's contract.
  Once the contract is true, none is needed; adding one as well would be belt-and-braces against a
  case that can no longer happen.
- **Any other reader of storage.** `revoke-device.ts` has the same shape and is not touched here.

## Tests

| Test | What it pins |
| --- | --- |
| `a storage that throws on the token read is a keep` | the token read is inside the `try` |
| `a storage that throws on the device-id read is a keep` | the device-id read is inside the `try` |

## What would still pass if the coder got it wrong

- Moving only the token read leaves the device-id read outside; the second test is what catches
  that, which is why there are two and not one.
- A `Storage` double that returns `null` instead of throwing tests nothing here — the throw is the
  whole input. Two inputs, or the test is vacuous.
- Asserting only that the promise resolves, without asserting `false`, would pass against a `catch`
  that returned `true` — the wrong direction under `ADR-0135` §6.

## Acceptance

- [ ] `device-standing.test.ts` reports `(9 tests)` and all pass
- [ ] Moving either storage read back outside the `try` reddens one of the two new tests, and the
      file is restored afterwards
- [ ] `session-token.ts` and `device-id.ts` are byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
