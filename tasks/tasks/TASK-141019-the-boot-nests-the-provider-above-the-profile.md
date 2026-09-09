---
schema: 2
id: TASK-141019
title: The boot nests the provider above the profile
type: task
status: backlog
parent: STORY-1410
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, account]
depends_on: [TASK-141012]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (38 tests)"'
  - git diff --exit-code -- web-client/src/main.tsx web-client/src/account/device-standing-provider.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`App.test.tsx` asserts that `main.tsx` nests `DeviceStandingProvider` **above** `ProfileProvider`,
not merely that both appear. Today the boot is correct and nothing says so.

## Why this exists

`TASK-141012`'s gates count occurrences: `DeviceStandingProvider` three times, `readDeviceStanding`
twice, `fetch: plainFetch` eight. A provider mounted as a **sibling** — `<DeviceStandingProvider
read={readStanding}>{null}</DeviceStandingProvider>` beside `ProfileProvider` rather than around it
— has exactly those counts. Both the coder and review mutated the boot that way and **nothing
reddened**: all 1264 tests green, all four counts unchanged, the negative-bind check still passing.

The ticket's own *What would still pass if the coder were wrong* section is unusually thorough and
anticipates three defects — binding `apiFetch`, mounting with an inline resolver in place of the
real read, and building the read inside a component — each mapped to a gate. All three assume
correct placement and a defect in *what* is bound or *when* it runs. Placement is the one shape it
does not cover.

`TASK-141014` does not close it either, and that was checked rather than assumed: its tests wrap
`Lobby` in a `DeviceStandingProvider` of their own, around the component under test, and never boot
`main.tsx`'s tree. A misplaced mount in `main.tsx` is invisible to them by construction.

**Named cost of leaving it:** a wrongly-nested provider makes every consumer read the context
default, which is `false` — `ADR-0135` §6's safe direction — so the failure is a feature silently
not working rather than a player losing a profile. That is why this is `XS` and `review: light`, not
why it should be skipped: the whole chain from `TASK-141003` to `TASK-141014` exists to make one
boolean true, and nothing currently notices if the boot drops it on the floor.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.test.tsx` | modify |

Read, do not edit: `web-client/src/main.tsx`,
`web-client/src/account/device-standing-provider.tsx`.

## Scope

- One test, in the idiom this file already uses — `readFileSync` over `main.tsx` and a regex against
  the source text, exactly as its existing structural checks do.
- It asserts **nesting order**: between the opening `<DeviceStandingProvider` and the opening
  `<ProfileProvider` there is no intervening `</DeviceStandingProvider>`. Presence is already
  counted by `TASK-141012`; this pins containment.
- The count gate moves 37 → 38.

## Out of scope

- **`main.tsx`.** The boot is already correct; this ticket observes it and changes nothing. The
  `git diff --exit-code` above says so.
- **A behavioural test.** Nothing reads the hook until `TASK-141014`, and `TASK-141012`'s Out of
  scope forbids reading the value. A source-level assertion is what is available at this point in
  the split, and it is honest about being one.
- **Every other provider in the boot.** `ProfileProvider`'s own placement relative to anything else
  is not this ticket's, and no ordering but this pair's is asserted.

## Tests

| Test | What it pins |
| --- | --- |
| `the device standing provider is above the profile provider` | `DeviceStandingProvider` contains `ProfileProvider` in `main.tsx`'s source |

## What would still pass if the coder got it wrong

- Asserting both names appear, in either order, is what already exists as a count — it passes
  against the sibling mount, which is the whole defect.
- Asserting the opening tags appear in order but ignoring the closing tag also passes against the
  sibling mount, because a sibling's opening tag still comes first. The intervening
  `</DeviceStandingProvider>` is the thing to look for.
- A regex over a formatted source must tolerate newlines and attributes between the tags; one
  written against a single line will pass today and break on the next `prettier` run.

## Acceptance

- [ ] `App.test.tsx` reports `(38 tests)` and all pass
- [ ] Rewriting `main.tsx` so the provider is a sibling reddens the new test, and the file is
      restored afterwards
- [ ] `main.tsx` is byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
