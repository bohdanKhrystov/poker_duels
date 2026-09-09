---
schema: 2
id: TASK-141007
title: Signing out forgets the device id when it is told to, and never otherwise
type: task
status: ready
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 5
atomic:
  - "`cd web-client && npm run check` — its `typecheck` step (`tsc --noEmit`), in `.github/workflows/build.yml`'s `client` job: a **required** field on `signOut`'s request object fails `TS2345 Property 'handsANewProfile' is missing` at every call site in one run. Probed at `c6a41e6d`: `src/main.tsx:133`, `src/e2e/drive-arc.tsx:127`, `src/account/no-secret-in-a-url.test.ts:226` and seven call sites in `src/account/sign-out.test.ts`"
  - "`ADR-0135` §7 — the field is required and may not be optional: *\"an omitted argument is a feature that silently does not exist, and this is a feature whose absence is invisible from the outside\"*. An optional field with a `false` default would make this a three-file ticket and would ship the shape the ADR refuses"
labels: [client, account]
depends_on: [TASK-141006]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/sign-out.test.ts 2>&1 | grep -qF "sign-out.test.ts  (9 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/no-secret-in-a-url.test.ts 2>&1 | grep -qF "no-secret-in-a-url.test.ts  (5 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (36 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - sh -c 'cd web-client && test $(grep -c "forgetDeviceId" src/account/sign-out.ts) -eq 2'
  - sh -c 'cd web-client && test $(grep -c "handsANewProfile: false" src/main.tsx) -eq 1'
  - sh -c 'cd web-client && B=$(mktemp) && cp src/account/sign-out.ts "$B" && perl -0pi -e "s|if \(request.handsANewProfile\) ||" src/account/sign-out.ts && ! cmp -s src/account/sign-out.ts "$B" || { cp "$B" src/account/sign-out.ts; exit 2; }; NO_COLOR=1 npx vitest run src/account/sign-out.test.ts >/dev/null 2>&1; rc=$?; cp "$B" src/account/sign-out.ts; [ "$rc" -ne 0 ]'
  - git diff --exit-code -- web-client/src/protocol/device-id.ts web-client/src/e2e/account-server.ts web-client/src/e2e/claimed-here-recovered-there.test.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`signOut` is told, as a required field, whether this sign-out hands the browser a new profile, and
forgets `pd.deviceId` when and only when the answer is `true` — before `reload()`, and whatever the
server answered.

## Files

Five, and the count is `tsc`'s rather than this ticket's: adding a required field to a function's
request object breaks every call site in the same commit.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/account/sign-out.ts` | modify | the field and the call to `forgetDeviceId`; the change itself |
| `web-client/src/account/sign-out.test.ts` | modify | `TS2345` at **seven** call sites — and these are the tests that pin what the change does, so they would have to move anyway |
| `web-client/src/account/no-secret-in-a-url.test.ts` | modify | `TS2345` at line 226; a merged test of an unrelated property that calls `signOut` once |
| `web-client/src/e2e/drive-arc.tsx` | modify | `TS2345` at line 127, inside the harness's own `AccountCalls` literal |
| `web-client/src/main.tsx` | modify | `TS2345` at line 133, the shipped binding |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§2, `docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md` §4,
`web-client/src/protocol/device-id.ts`.

## Scope

- `signOut`'s request object gains `readonly handsANewProfile: boolean` — **required**, no default.
- In the local half, beside the `forgetSessionToken` and `forgetRoomCode` it already calls and
  **before** `request.reload()`: `if (request.handsANewProfile) forgetDeviceId(request.storage)`.
  It runs **regardless of what `POST /api/auth/sign-out` answered** — a rejected `fetch` and a
  non-`204` run the same local half a `204` runs, which is the rule the token already follows and
  the reason it must be one rule (`ADR-0135` §2).
- With **no token in storage** nothing happens at all: the existing early return stands above the
  forgetting, so a browser that was never signed in cannot lose its device id however it is called.
- The module's KDoc paragraph beginning *"Only `forgetSessionToken` and `forgetRoomCode` run"* is
  rewritten: `ADR-0131` supersedes `ADR-0030` §8's *not on sign-out* and *clears the token and only
  the token* **for one case only**, and §8 governs every other moment in the life of the browser.
  The sentence *"The device id is never read, written or cleared here"* is now false and must go —
  it cites the exact clause that was superseded.
- `main.tsx` and `drive-arc.tsx` pass a **literal `false`**, each with a one-line comment naming
  `TASK-141010` as the ticket that threads the real value. `false` is `ADR-0135` §6's keep, so the
  product's behaviour is byte-identical after this merge and the abandoning branch is unreachable.
- `main.tsx`'s binding keeps `fetch: plainFetch` as its **first** entry: `App.test.tsx` asserts
  `/signOut\([^}]*fetch: plainFetch/` and its `[^}]*` stops at the argument object's closing brace.

## Out of scope

- **Deciding the boolean.** Nothing computes it here; every caller passes `false`.
- **`SignOutControl`, `AccountScreen`, `AccountCalls`.** `TASK-141009` and `TASK-141010`.
- **`device-id.ts`.** `forgetDeviceId` landed in `TASK-141005` and is in the
  `git diff --exit-code` gate.
- **`src/e2e/account-server.ts` and `src/e2e/claimed-here-recovered-there.test.tsx`.** Both are in
  the `git diff --exit-code` gate. The arc they drive is `ADR-0131` §1 **row two** — browser B
  signed in to A's account, pinned by `expect(readDeviceId(storageB)).toBe(PLAYER_SEAT_1.deviceId)`
  — and row two does not move.
- **Sweeping any other key.** `ADR-0135` §2: exactly three keys move; `ADR-0119` §5's skipped bit
  and every later key are kept.

## Tests

`sign-out.test.ts`, **6 today → 9**. Three merged tests change, and exactly how they change is
below; **no assertion is deleted or weakened.**

The three merged tests that move:

| Merged test | What moves, and why |
| --- | --- |
| `clears the token and leaves the device id exactly where it was` | its call gains `handsANewProfile: false`, and its **title** becomes `clears the token and leaves the device id where it was when the profile stays` — the assertion `readDeviceId(storage)` is `"d-before"` is unchanged and is now the negative half of the new behaviour |
| `asks nothing of the server when no session is held` | its call gains `handsANewProfile: **true**`, and its assertion `readDeviceId(storage)` is `"d-untouched"` is unchanged. This **strengthens** it: with no token the early return must happen above the forgetting, and this is the only test that says so |
| `forgets the room this tab remembered`, `presents the session and no device id`, `clears the token even when the server never answers`, `reloads once, after the local half is done` | calls gain `handsANewProfile: false`; no assertion moves |

The three new tests:

| Test | Proves |
| --- | --- |
| `told the sign-out hands a new profile, it forgets the device id` | `handsANewProfile: true` on a storage holding a token and `pd.deviceId`: afterwards `readDeviceId` is `null`, and the raw `getItem(DEVICE_ID_STORAGE_KEY)` is `null` too — so a module that wrote `""` does not pass |
| `the device id goes whatever the server answered` | two arms in one test: a `fetch` that **rejects** and a `500`, both with `handsANewProfile: true`, both leaving `readDeviceId` `null` and both reloading once. `ADR-0135` §2's *"regardless of what `POST /api/auth/sign-out` answered"* |
| `no key beyond the three ever moves` | a storage seeded with `pd.deviceId`, `pd.sessionToken`, `pd.roomCode` **and** `pd.nameAskSkipped`; after `handsANewProfile: true` the skipped bit is read back with its exact value and `storage.length` is **1**; after `handsANewProfile: false` on a fresh copy, both the device id and the skipped bit survive and `storage.length` is **2**. `ADR-0131` §6 rules the skipped bit is not spent by a new profile |

`no-secret-in-a-url.test.ts` gains a field at one call site and stays at **5** tests, with no
assertion touched.

## What would still pass if the coder were wrong

- **Forgetting the device id unconditionally** — `ADR-0135` §Alternatives 6, and the plainest
  misreading of the story title — passes the three new tests and fails
  `clears the token and leaves the device id where it was when the profile stays`. That is
  `ADR-0131` §1 row two, and the one outcome §2 forbids. It is why the `false` arm is asserted in
  three different tests rather than once.
- **Forgetting it only on a `204`** passes every test that uses `noContentResponse` and fails
  `the device id goes whatever the server answered`, which is why that test carries **two**
  failure shapes and not one.
- **Putting the forgetting above the early return** passes everything except
  `asks nothing of the server when no session is held`, whose input this ticket deliberately flips
  to `true` for that purpose.
- **A `verify:` mutation says the same thing without trusting the reading**: the gate strips
  `if (request.handsANewProfile) ` out of `sign-out.ts`, proves with `! cmp -s` that the file
  actually changed, requires `sign-out.test.ts` to **fail**, and restores the file either way. A
  test suite that never depended on the guard would stay green, and that would be the finding.
- **Editing `account-server.ts` to teach it the new route** would look helpful and is refused:
  its unknown-path `500` is what makes `readDeviceStanding` answer `false`, which is the arc's
  correct answer, and `git diff --exit-code` covers it.

## Acceptance criteria

- [ ] `sign-out.test.ts` reports `(9 tests)` and all pass
- [ ] `no-secret-in-a-url.test.ts` reports `(5 tests)`, `App.test.tsx` reports `(36 tests)` and
      `claimed-here-recovered-there.test.tsx` reports `(8 tests)`, all passing
- [ ] `grep -c "forgetDeviceId"` in `sign-out.ts` is 2 (the import and the one call)
- [ ] `grep -c "handsANewProfile: false"` in `main.tsx` is 1
- [ ] The mutation gate exits 0: with the guard removed, `sign-out.test.ts` fails, and the file is
      restored
- [ ] `git diff --exit-code` over `device-id.ts`, `account-server.ts` and
      `claimed-here-recovered-there.test.tsx` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
