---
schema: 2
id: TASK-140813
title: The profile provider can be told to read again
type: task
status: backlog
parent: STORY-1408
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile]
depends_on: [TASK-140812]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads again when asked, and adopts what came back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads once and only once until it is asked'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asking where no provider is above does nothing'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ProfileProvider` can be told to run its `read` again and adopt the answer, so a caller that knows
the server changed the caller's own profile can refresh it without editing a field of the profile it
holds (`ADR-0132` §6).

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-provider.tsx` | modify |
| `web-client/src/profile/profile-provider.test.tsx` | modify |

Read, and do not edit:
[`ADR-0132`](../../docs/adr/ADR-0132-the-profile-says-whether-it-holds-a-password.md) §6 — the rule
and the one outcome it names;
`web-client/src/profile/profile-strip.ts` — `ProfileStripState`.

## Scope

- Add `refresh: () => void` to `ProfileContextValue`, built with `useCallback` over `read` — the same
  dependency the mount effect already takes, so the reference is stable for the same reason.
- `refresh()` calls `read()` and adopts whatever it answers, **including `no-profile` and
  `unavailable`**. `ADR-0132` §6 says *re-reads and adopts the answer*: the client never composes a
  status code with a held profile, and never keeps a stale profile because the fresh answer was worse.
- Export `useRefreshProfile(): () => void`, mirroring `useReportNameWrite` exactly — including its
  no-op fallback where no provider sits above, and a KDoc that says what it is for and that it is the
  **only** way the held profile is replaced other than the mount read and `reportNameWrite`.
- Guard the same way the mount effect does: a component that unmounts between the call and the answer
  must not `setState`. The mount effect's `live` flag is the shape; a `refresh` that resolves after
  unmount is the same hazard.
- `reportNameWrite` is untouched. It still adopts `SetNameOutcome.named`'s profile directly, and this
  ticket neither routes it through `refresh` nor repairs the `hasRecoveryEmail` body it adopts (see
  `STORY-1408`'s *Out of scope*).

## Out of scope

- Calling `refresh` from anywhere. `TASK-140814` wires it to the one outcome `ADR-0132` §6 names, and
  until then nothing calls it — which is why the *no reader* case is one of the three tests.
- Polling, an interval, a focus listener, or a second trigger of any kind. `ADR-0132` §6 enumerates
  **one** outcome and says why there is no other: sign-in and sign-out already `reload`.
- `readProfile`, `readProfileStrip` or anything in `profile-strip.ts`.

## Tests

`profile-provider.test.tsx`

| Test | Proves |
| --- | --- |
| `reads again when asked, and adopts what came back` | A `read` that answers a profile with one coin balance first and a **different** one second: the tree shows the first, `refresh()` is called, and the tree then shows the second. Two different answers, because one answer cannot tell a re-read from a cached value |
| `reads once and only once until it is asked` | The same `read` as a counting spy: after mount it has been called exactly once; after one `refresh()` exactly twice. An absolute count, not a `toHaveBeenCalled` |
| `asking where no provider is above does nothing` | `useRefreshProfile()` rendered outside a `ProfileProvider` answers a function that throws nothing when called — the same contract `useReportNameWrite` already has |

## Acceptance criteria

- [ ] `reads again when asked, and adopts what came back` passes, with two different answers
- [ ] `reads once and only once until it is asked` passes, asserting the absolute counts `1` and `2`
- [ ] `asking where no provider is above does nothing` passes
- [ ] Every existing test in `profile-provider.test.tsx` passes, unedited
- [ ] `npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
