---
schema: 2
id: TASK-041115
title: The strip prints the player's own name, or what stands for none
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, profile, ui, identity]
depends_on: [TASK-041114]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +415 passed \(415\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the player, and stands in for a player with no name, in one render'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing different about a name that was removed'
  - cd web-client && npm run check
---

## Goal

The profile strip states who the player is, above their coins: the name the server sent, or — when
there is none — whatever `nameOrNone` answers.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.tsx` | modify — one line in the `profile` branch |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/name-text.ts` (`nameOrNone`),
`web-client/src/profile/profile-fixture.ts`.

## Scope

- The `kind: "profile"` branch renders `nameOrNone(state.profile.displayName)` above the balance
  line. Nothing else in the component moves: the balance, the duel list, the empty state, the
  `no-profile` sentence and the `unavailable` `null` are untouched.
- **The strip never branches on `null` itself.** It calls `nameOrNone` and prints the answer, so the
  treatment lives in one file and the two surfaces cannot drift.
- **No `displayNameRemoved` anywhere in this component.** The strip's job is to say who the player
  is; the notice about a removal is the name surface's, where the form to fix it is
  (`TASK-041109`, `ADR-0052` §1).
- Still no heading element, and still nothing but theme classes.

## Out of scope

- The opponent on a duel line — `TASK-041116`, in this same file, next.
- Making the name a link, a profile page or anything clickable. `EPIC-05` owns what a name links to.
- Any change to `ProfileStripState` or to the read.

## Tests

`web-client/src/profile/ProfileStrip.test.tsx`, describe block `"the profile strip"`.

| Test | Proves |
| --- | --- |
| `names the player, and stands in for a player with no name, in one render` | Two strips in one render: `aProfile({ displayName: "Ada", coinBalance: 5 })` puts `Ada` on screen and `aProfile({ displayName: null, coinBalance: 5 })` puts `nameOrNone(null)` on screen, and neither puts the other's text there. Fails against a strip that prints a constant, against one that hides the name when it is `null` — `ADR-0029` §6 requires `null` to be rendered, not hidden — and against one that prints `playerId` |
| `says nothing different about a name that was removed` | `aProfile({ displayName: null, displayNameRemoved: true })` and `aProfile({ displayName: null, displayNameRemoved: false })`, rendered side by side, produce **identical** strip markup for the name line. Fails against a strip that marks the removed state, which would put a moderation fact next to the coins and out of the one place `ADR-0052` §1 puts it |

Two tests added to 413, so the suite reports **415**.

## Acceptance criteria

- [ ] `the profile strip > names the player, and stands in for a player with no name, in one render`
      passes, with both profiles in the one render
- [ ] `the profile strip > says nothing different about a name that was removed` passes
- [ ] Every merged `ProfileStrip.test.tsx` test passes unchanged
- [ ] `grep -c 'displayNameRemoved' web-client/src/profile/ProfileStrip.tsx` returns `0`
- [ ] `grep -c 'displayName === null\|?? ' web-client/src/profile/ProfileStrip.tsx` returns `0` —
      the strip asks `nameOrNone` rather than deciding
- [ ] `npm run --silent test` reports `Tests  415 passed (415)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
