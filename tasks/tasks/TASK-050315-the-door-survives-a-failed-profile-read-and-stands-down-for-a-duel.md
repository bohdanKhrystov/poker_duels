---
schema: 2
id: TASK-050315
title: The door survives a failed profile read, and stands down for a duel in progress
type: task
status: backlog
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, leaderboard, ui, tests]
depends_on: [TASK-050314]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the same ladder door whether the profile read failed or answered'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'does not offer the ladder door while a duel is in progress'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'mounted ladder screen carries exactly one heading'
  - cd web-client && npm run check
---

## Goal

The two conditions `ADR-0060` puts on a door are asserted for this one: it does not depend on the
profile read, and it does not exist while a duel is being played.

## Why these two, and why two fixtures for the first

`ADR-0060`: the strip renders `null` when `GET /api/me` fails, *"and the way to a screen may not
vanish with it"* — a ladder unreachable because the profile read was slow is a bug that decision
already refused once. `ADR-0065` §3 leans on the same rule, since the self line rides on the ladder
response precisely so a failed profile read cannot hide a standing.

**One fixture cannot tell a rule from a default here.** A door rendered unconditionally passes a
single failed-read test, and so does a door rendered only when the read failed. The test therefore
renders both states and asserts the same control in both — which is more than the history door's
equivalent test asserts today, deliberately.

`ADR-0060` again for the second: the door is offered only on the lobby branch that offers *Create a
duel room*, *"because a player who opened another screen mid-hand would leave their rival at a table
nothing ends."*

This ticket adds tests and **changes no production file**. If one fails, the smallest fix that makes
it pass is in scope and nothing else is.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050314` or by any earlier ticket changes |

Read, not edited: `web-client/src/profile/profile-fixture.ts` (`aProfile`),
`web-client/src/profile/profile-provider.tsx` (`ProfileProvider` takes the `read` it acts on).

## Scope

- The first test renders the app twice, each time inside a `ProfileProvider` whose injected `read`
  answers differently:
  - `{ kind: "unavailable" }` — the read failed, and `ProfileStrip` is not on screen;
  - `{ kind: "profile", profile: aProfile(), duels: [] }` — the read answered.
  In **both**, a control named `Leaderboard` is on the first screen, and clicking it in either opens
  the section labelled `leaderboard`.
- The second test uses the store fixture already in this file: apply `RoomJoined`, then a
  `Snapshot`, and assert the door is gone. The existing *does not offer the door while a duel is in
  progress* test is the pattern; the seat-view helper and the snapshot literal are already there and
  are reused rather than re-written.
- The third counts headings inside the mounted ladder screen, the way the history one does.
  `LadderScreen` renders one `<h2>` and the guard is only reachable once the screen is mounted in
  the tree, which `TASK-050314` made possible.

## Out of scope

- **Changing `Lobby.tsx` or `main.tsx`**, unless one of these three tests fails.
- **The history door's own tests.** They are not strengthened here; if the single-fixture
  profile-read test beside this one is worth widening, that is an ordinary ticket against
  `STORY-0413`.
- **Anything about what the ladder shows.** These three tests never look inside the list.

## Tests

`web-client/src/App.test.tsx`, three new tests.

| Test | Proves |
| --- | --- |
| `offers the same ladder door whether the profile read failed or answered` | Two renders, two profile answers, the same control found in both, and the ladder opening from both. A door hung off the strip renders in one and not the other, and reddens |
| `does not offer the ladder door while a duel is in progress` | After `RoomJoined` and a `Snapshot`, no control named `Leaderboard` is on screen. Asserted against a store state that has a duel in it, not against a prop |
| `mounted ladder screen carries exactly one heading` | The section labelled `leaderboard`, opened from the lobby, holds exactly one of `h1…h6`. A second heading added inside the screen reddens it — which is uncatchable until the screen is reachable from the tree |

## Acceptance criteria

- [ ] `offers the same ladder door whether the profile read failed or answered` passes for both
      profile answers — moving the door inside the `profile !== null &&` guard in `Lobby.tsx`
      reddens it
- [ ] `does not offer the ladder door while a duel is in progress` passes — rendering the door
      outside the lobby branch reddens it
- [ ] `mounted ladder screen carries exactly one heading` passes — adding an `<h3>` to
      `LadderScreen.tsx` reddens it
- [ ] Every test already in `App.test.tsx` passes, with no assertion in any of them edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
