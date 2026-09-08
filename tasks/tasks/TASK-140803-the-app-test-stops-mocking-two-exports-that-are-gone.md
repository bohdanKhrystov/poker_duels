---
schema: 2
id: TASK-140803
title: The app test stops mocking two exports that are gone
type: task
status: done
parent: STORY-1408
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, account]
depends_on: [TASK-140802]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -qF "offerSettledHere" web-client/src/App.test.tsx'
  - sh -c '! grep -qF "settleOfferHere" web-client/src/App.test.tsx'
  - sh -c '! grep -rqF "offerSettledHere" web-client/src'
  - sh -c '! grep -rqF "settleOfferHere" web-client/src'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`App.test.tsx`'s `vi.mock("../main", …)` names only exports `main.tsx` actually has.

## Why this is its own ticket

`TASK-140802` deleted `offerSettledHere` and `settleOfferHere` from `main.tsx`. `App.test.tsx` still
overrides both in its mock factory — and **nothing fails**: measured on the probe tree, `npm run
typecheck` was clean and the file's 36 tests passed with the two dead keys standing. No gate names
this file, so it could not be a row in `TASK-140802`'s `atomic:` table (`ADR-0068` §3: a gate is
something that fails an exit code, and *"these belong together"* earns nothing). It is a dependency,
not a coupling — the same shape `TASK-041641` took after `TASK-041616`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/main.tsx` — to confirm the two names are gone from its
exports.

## Scope

- Delete the two keys `offerSettledHere` and `settleOfferHere` from `App.test.tsx`'s `vi.mock`
  factory return, and nothing else in that object.
- If a comment nearby counts the mocked bindings, correct the count.

## Out of scope

- Every other key in that factory. `useHistory`, `useLadder`, `useSignedIn` and the rest are live and
  stay exactly as they are.
- Adding a test. This removes two dead lines from a mock; there is no behaviour here to pin.

## Tests

No new test. `App.test.tsx`'s existing cases are the proof: all of them must still pass, and the
count of tests in that file must not change.

## Acceptance criteria

- [ ] `grep -F "offerSettledHere" web-client/src/App.test.tsx` finds nothing
- [ ] `grep -F "settleOfferHere" web-client/src/App.test.tsx` finds nothing
- [ ] Neither name appears anywhere under `web-client/src`
- [ ] `npm run check` exits 0, so every test in `App.test.tsx` still passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
