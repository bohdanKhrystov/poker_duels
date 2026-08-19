---
schema: 2
id: TASK-041316
title: The App test hands the history read a Storage, and check goes green again
type: task
status: done
parent: STORY-0413
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [client, history, regression, ci]
depends_on: [TASK-041313]
verify:
  - cd web-client && npm ci
  - cd web-client && npm run check
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Errors' && exit 1 || exit 0
---

## Goal

`npm run check` exits **1** on `develop`. All 480 tests pass; Vitest then reports `Errors 2 errors`
from two unhandled rejections, and that is enough to fail the command:

```
TypeError: Cannot read properties of undefined (reading 'getItem')
 ❯ Module.readDeviceId src/protocol/device-id.ts:11:25
 ❯ Module.readDuelPage src/profile/duel-page.ts:45:20
 ❯ readHistory src/main.tsx:47:3
 ❯ src/history/HistoryScreen.tsx:72:28
This error originated in "src/App.test.tsx"
```

**How it got in.** `TASK-041313` wired `readHistory` into the tree and added a test that presses the
door, which mounts `HistoryScreen`, whose mount effect calls the **real** binding from `main.tsx` —
`storage: localStorage`. Under Vitest, Node's own `localStorage` shadows jsdom's and is `undefined`,
so `readDeviceId` throws. The promise rejects after the test has finished, so no assertion sees it
and the run is still reported as 480 passed.

That is the hazard `TASK-041106` and `TASK-041112` both recorded — *`Storage` is a parameter, never
a global* — arriving by a route neither anticipated: not a module reading the global, but a test
mounting a tree that reaches one.

**CI passed it.** `build.yml` runs `npm run check`, so it should have caught this; it did not,
which means the rejection is timing-dependent and lost a race in CI. It reproduces reliably here.
An intermittent red build from now on is the same defect wearing a different face.

## Scope

- `App.test.tsx` supplies a **fake** history read, as it already supplies fakes for the profile
  reads, so no test in that file reaches `main.tsx`'s real binding.
- Every existing test in the file keeps its assertions. `TASK-041313`'s seven — the door, the swap,
  the heading guard, the duel-in-progress guard, the layering guard — must all still pass, and the
  heading guard must still **mount** `HistoryScreen`, or it stops covering what it was written for.

## Out of scope

- `main.tsx`, `HistoryScreen.tsx`, `duel-page.ts` and `device-id.ts`. **A refusal, not an omission:**
  the production path is correct — `main.tsx` is the one place naming `localStorage`, which
  `TASK-041112` established deliberately. The defect is a test reaching it.
- Making `readDeviceId` tolerate an absent `Storage`. That would hide the next occurrence rather
  than surface it, and the type already says `Storage`, not `Storage | undefined`.
- Any other unhandled rejection. If the sweep below finds one this ticket did not cause, it stops
  and says so rather than widening.

## Tests

`App.test.tsx`. No test added; the fixture changes and every assertion stays.

| Test | Proves |
| --- | --- |
| the seven from `TASK-041313`, unchanged | The door, the swap, the heading count, the duel guard and the layering guard all still hold with a fake read in place — so the fix removes a real dependency, not the coverage |

The real assertion is in `verify:`: **the suite reports no `Errors` line at all.** A count of passing
tests cannot see this defect, which is exactly how it reached `develop` — the run said 480 passed
while exiting 1.

## Acceptance criteria

- [ ] `npm run check` exits 0 on this branch
- [ ] The test run reports **no** `Errors` line
- [ ] `App.test.tsx` names no `localStorage` and reaches `main.tsx`'s binding in no test
- [ ] All seven of `TASK-041313`'s tests pass unchanged, and the heading guard still mounts
      `HistoryScreen`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
