---
schema: 2
id: TASK-041113
title: The lobby shows the name surface, and the duel table never does
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, profile, ui, wiring]
depends_on: [TASK-041112]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +410 passed \(410\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the name surface beside the strip, and only with a profile to show'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the name surface off the screen once a table is on it'
  - cd web-client && npm run check
---

## Goal

The name surface is on the first screen, beside the profile strip, and is gone the moment a duel is
on the screen.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one conditional render |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/NameSurface.tsx`,
`web-client/src/profile/set-name-provider.tsx`, `web-client/src/profile/profile-fixture.ts`.

## Scope

- In the lobby's last branch — the one that already renders `ProfileStrip` — render
  `<NameSurface>` **after** the strip, and only when the profile state is `kind: "profile"` and
  `useSetName()` is not `null`. Two `null` checks, no fallback: a surface with no way to send is a
  form that cannot work.
- **Nothing changes about the earlier branches.** The result screen, the table and the waiting room
  return before this one, so the surface is structurally impossible over a duel — which is
  `ADR-0052` §1's *never over the duel table*, held by the shape of the function rather than by a
  flag.
- The strip stays first. A player reads their coins, then their name.

## Out of scope

- Any second surface, route or screen. **A refusal, not an omission:** `STORY-0412` owns the account
  screens and `STORY-0413` the history screen; both queue behind this story because all three extend
  this shell, and adding a route here would settle for them a question they own.
- `App.tsx`, and any heading. `App.test.tsx` calls `getByRole("heading")`, which throws on a second
  heading anywhere in the tree.
- Refreshing the strip after a name is set — `TASK-041110`'s out of scope, unchanged.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, in the existing describe block, using its
`renderLobbyWithProfile` helper wrapped in a `SetNameProvider` with a spy.

| Test | Proves |
| --- | --- |
| `shows the name surface beside the strip, and only with a profile to show` | With a profile the region labelled `your display name` is on screen **after** the one labelled `your profile` (document order), and in the same test a lobby rendered with `{ kind: "no-profile" }` has no such region. Fails against a surface rendered unconditionally, against one rendered above the strip, and against one that survives a missing profile |
| `keeps the name surface off the screen once a table is on it` | With a store holding a joined room and a snapshot, `queryByLabelText("your display name")` is `null` while the table is on screen — the sibling of the strip's own merged assertion. Fails against a surface rendered outside the lobby's last branch, which is the one edit that would put a name form over a duel |

Two tests added to 408, so the suite reports **410**.

## Acceptance criteria

- [ ] `shows the name surface beside the strip, and only with a profile to show` passes, asserting
      document order
- [ ] `keeps the name surface off the screen once a table is on it` passes
- [ ] The two merged strip tests in `Lobby.test.tsx` pass unchanged
- [ ] `Lobby.tsx` renders `NameSurface` in exactly one place
- [ ] `App.tsx` is unmodified and `App.test.tsx` passes
- [ ] `npm run --silent test` reports `Tests  410 passed (410)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
