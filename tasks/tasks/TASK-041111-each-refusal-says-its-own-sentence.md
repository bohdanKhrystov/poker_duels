---
schema: 2
id: TASK-041111
title: Each refusal says its own sentence, and only two leave the form
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, ui, identity, copy]
depends_on: [TASK-041110]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +406 passed \(406\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives every refusal its own sentence, from one render each'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the form for the two refusals a player can act on, and takes it away for the rest'
  - cd web-client && npm run check
---

## Goal

Every answer that is not a name renders its own sentence from `name-text.ts`, and the form survives
exactly the two refusals a player can do something about.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify — the outcome branch |
| `web-client/src/profile/NameSurface.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/name-text.ts` (`refusalSentence`, `mayTryAgain`),
`web-client/src/profile/set-name.ts` (`SetNameOutcome`).

## Scope

- A settled outcome other than `named` renders `refusalSentence(outcome.kind)` inside the section,
  and the sentence is replaced — never appended — when another attempt settles. A player who fails
  twice reads one sentence, not a log.
- `mayTryAgain(kind)` decides whether the field and the button stay: `true` keeps them, with what the
  player typed still in the field so they can edit rather than retype; `false` removes them.
- **`403` removes the form.** `ADR-0029` §5 chose `403` over `409` precisely so the client can tell
  the two apart without a body, and `STORY-0411` says a form that invites a retry it can never
  satisfy is worse than one that refuses.
- The sentence is announced as a `role="status"` region, not `role="alert"`. It is the answer to
  something the player just did, on the screen they are looking at.
- No sentence is composed here. Every string comes from `name-text.ts`, whose test holds the golden
  copies.

## Out of scope

- Distinguishing the three sources of a `409`. **A refusal, not an omission:** `ADR-0051` §2 makes
  taken, blocked and retired one answer with no way to tell them apart, and `ADR-0052` §7 forbids
  the client from implying one — a player whose own name was retired must not be told somebody else
  has it. If a screen could tell them apart, the wire would have leaked.
- Retrying anything automatically, and pre-checking availability. `ADR-0029` §5 refuses an
  availability endpoint on purpose; the `409` answers the question at the only moment it matters.
- Clearing the field on a refusal a player can act on. They typed it; it is theirs to edit.

## Tests

`web-client/src/profile/NameSurface.test.tsx`, describe block `"the name surface"`.

| Test | Proves |
| --- | --- |
| `gives every refusal its own sentence, from one render each` | Driving `rejected`, `conflict`, `permanent`, `no-profile` and `unavailable` puts five distinct sentences on screen, each equal to the literal typed out in this test, and **no** screen contains the word `taken`. Fails against a single *something went wrong* for every case, against two cases sharing a sentence, and against `ADR-0052`'s named defect — rendering `409` as *that name is taken* |
| `keeps the form for the two refusals a player can act on, and takes it away for the rest` | After `rejected` and after `conflict` the textbox is still there **and still holds what was typed**; after `permanent`, `no-profile` and `unavailable` there is no textbox and no button. All five asserted in one test. Fails against a form kept for everything (the `403` case `ADR-0029` §5 argues about by name), against a form removed from everything, and against one that clears the field a player is meant to edit |

Two tests added to 404, so the suite reports **406**.

## Acceptance criteria

- [ ] `the name surface > gives every refusal its own sentence, from one render each` passes, for
      all five outcomes
- [ ] `the name surface > keeps the form for the two refusals a player can act on, and takes it away
      for the rest` passes, for all five outcomes
- [ ] The seven tests from `TASK-041108`–`TASK-041110` pass unchanged
- [ ] `NameSurface.tsx` contains no string literal that is a sentence — every one comes from
      `name-text.ts`
- [ ] `grep -c 'role="alert"' web-client/src/profile/NameSurface.tsx` returns `0`
- [ ] `npm run --silent test` reports `Tests  406 passed (406)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
