---
schema: 2
id: TASK-141016
title: The forget is pinned ahead of the reload
type: task
status: backlog
parent: STORY-1410
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, account]
depends_on: [TASK-141007]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/sign-out.test.ts 2>&1 | grep -qF "sign-out.test.ts  (10 tests)"'
  - git diff --exit-code -- web-client/src/account/sign-out.ts web-client/src/protocol/device-id.ts
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`sign-out.test.ts` gains one test pinning that `forgetDeviceId` runs **before** `request.reload()`
in the abandoning case. `TASK-141007` shipped that order correctly; nothing observed it.

## Why this exists

`TASK-141007`'s coder found that moving `forgetDeviceId` to after `request.reload()` left all nine
tests in the file green, and reported the ordering as unobservable under Vitest because `reload` is
a no-op mock. Review disproved that excuse rather than accepting it: it wrote the assertion using
**the pattern the file already contains** — the `reload` mock body, which is where
`reloads once, after the local half is done` already observes the session token — and the new test
failed against the reordered code. So the mechanism was there all along and simply had not been
extended to the device id.

The shipped code is correct. This is a coverage gap, not a bug, and it is filed rather than folded
into `TASK-141007` because that ticket's Tests table enumerates its tests exactly and its
`atomic:` set is the five files `tsc` names.

Whether a real browser would actually lose the write is not the reason to pin it. Review's reading:
`location.reload()` queues navigation as a task, so a synchronous `localStorage.removeItem` that
follows it runs to completion before teardown in current engines — likely kept, but that is engine
behaviour rather than a guarantee, and `ADR-0135` §6's safe direction should not rest on it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/sign-out.test.ts` | modify |

Read, do not edit: `web-client/src/account/sign-out.ts`,
`docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md` §§2, 6.

## Scope

- One test, in the shape the file already uses: a `reload` mock whose body asserts
  `readDeviceId(storage)` is already `null`, driven with `handsANewProfile: true`.
- The count gate moves 9 → 10. No other test in the file moves.

## Out of scope

- **`sign-out.ts`.** The order it ships is already right; this ticket observes it and changes
  nothing. The `git diff --exit-code` above says so.
- **The keeping case.** `clears the token and leaves the device id where it was when the profile
  stays` already pins that the device id survives; ordering is meaningless when nothing is removed.
- **`device-id.ts`.** `TASK-141005` owns it.

## Tests

| Test | What it pins |
| --- | --- |
| `the device id is already forgotten by the time the page reloads` | `forgetDeviceId` precedes `request.reload()` when told the sign-out hands a new profile |

## What would still pass if the coder got it wrong

- Asserting only that `forgetDeviceId` was called leaves the ordering unpinned — that is what the
  nine existing tests already do, and it is why this ticket exists. The assertion must sit **inside
  the `reload` mock body**, which is the only place that runs at the moment ordering is decidable.
- Driving it with `handsANewProfile: false` makes the test vacuous: nothing is removed in that case,
  so `readDeviceId` is non-null before and after, and the assertion could never fail.

## Acceptance

- [ ] `sign-out.test.ts` reports `(10 tests)` and all pass
- [ ] Moving `forgetDeviceId` after `request.reload()` in `sign-out.ts` reddens the new test, and
      the file is restored afterwards
- [ ] `sign-out.ts` and `device-id.ts` are byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
