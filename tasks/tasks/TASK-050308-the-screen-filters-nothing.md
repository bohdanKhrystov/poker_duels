---
schema: 2
id: TASK-050308
title: The screen filters nothing — a nameless row and a negative standing are ordinary rows
type: task
status: backlog
parent: STORY-0503
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, leaderboard, ui, tests]
depends_on: [TASK-050307]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders every row it was sent, including the two a client might drop'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints a negative standing with its sign, in the position the server gave it'
  - cd web-client && npm run check
---

## Goal

The two rows a client is most tempted to treat as a mistake — a player with no name, and a player
below zero — are asserted to be ordinary rows on this screen.

## Why these two, and why now

`ADR-0063` §2 puts a nameless player on the ladder as an ordinary row, `ADR-0058` says it prints
`No name` through `nameOrNone` and through nothing else, and `ADR-0014` calls a first loss at `−1`
*"the case to check first when the display work lands"*. There is **no eligibility rule on this
side of the wire** (`ADR-0063` §1): the screen prints what it was sent, or it is filtering, and
filtering is a rule nobody wrote.

This ticket adds tests and **changes no production file**. `TASK-050307` already renders through
`rowLine`, so these should pass on the first run; if either does not, the defect is in
`LadderScreen.tsx` or `ladder-text.ts` and the fix belongs in this ticket.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050307` changes |

Read, not edited: `web-client/src/profile/name-text.ts`, `web-client/src/profile/profile-text.ts`.

## Scope

- Two tests, on one fixture: a five-row page, `nextCursor: null`, ranks `[1, 1, 3, 4, 4]`, of which
  one row has `displayName: null`, one has `coins: -2`, and one has **both**.
- The minus sign is U+2212, as `coinBalanceText` emits it. Copy the character from
  `profile/profile-text.ts`; a hyphen typed here fails and the failure does not say why.

## Out of scope

- **Changing `LadderScreen.tsx` or `ladder-text.ts`**, unless one of these two tests fails — in
  which case the smallest fix that makes it pass is in scope and nothing else is.
- **A second-person variant of `No name`, or any hint of why a name is missing** — `ADR-0058` §2,
  §3 refuse both.
- **An empty-ladder state** — `TASK-050309`.

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, same `describe`, two new tests.

| Test | Proves |
| --- | --- |
| `renders every row it was sent, including the two a client might drop` | The list holds exactly **five** `<li>`, one per wire row, in wire order. A screen that skipped a null name or a negative standing renders four or three and reddens |
| `prints a negative standing with its sign, in the position the server gave it` | The row with `coins: -2` reads exactly `"4 No name −2"` and is the fourth `<li>`, and the named row beside it reads its own text. Two rows in one list, so the assertion cannot pass on a default |

## Acceptance criteria

- [ ] `renders every row it was sent, including the two a client might drop` passes, asserting a
      count of five — adding `.filter((row) => row.displayName !== null)` to the map reddens it, and
      so does `.filter((row) => row.coins >= 0)`
- [ ] `prints a negative standing with its sign, in the position the server gave it` passes with
      U+2212 — printing `Math.abs(coins)` reddens it
- [ ] `grep -c 'No name' web-client/src/ladder/LadderScreen.tsx` returns `0` — the branch is
      `nameOrNone`, in one place (`ADR-0058`)
- [ ] Every test `TASK-050307` wrote still passes, with no assertion in it edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
