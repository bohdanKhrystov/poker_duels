---
schema: 2
id: TASK-031108
title: One line per recent duel, and a word when there are none
type: task
status: backlog
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, profile, ui]
depends_on: [TASK-031107]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +350 passed \(350\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows one line per duel, with its outcome, coin, hands and time'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says there are no duels yet when the list is empty'
  - cd web-client && npm run check
---

## Goal

Under the balance, the last few duels: one line each, saying how it ended, what the coin did, how
many hands it ran and when it finished — and one sentence where a new player has none.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.tsx` | modify — the list under the balance |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify — two tests added, none changed |
| `web-client/src/profile/profile-text.ts` | read — `outcomeWord`, `coinDeltaText`, `finishedAtText` |
| `web-client/src/result/DuelResult.tsx` | read — how it pluralises a hand count |

## Scope

- Inside the `kind: "profile"` branch, below the balance:
  - `duels.length === 0` → the sentence *No duels yet.* An empty list is a legitimate answer for a
    new player (`docs/protocol.md` says it is not a `404`), so it reads as a beginning, not a fault.
  - otherwise a `<ul>` with one `<li>` per duel, in the order the server sent them — newest first
    is the server's ordering and the client does not sort.
- Each line carries exactly four things, all from that row: `outcomeWord(duel.outcome)`,
  `coinDeltaText(duel.coinDelta)`, the hand count, and `finishedAtText(duel.finishedAt)` with **no**
  options, so the reader's own locale formats it.
- The hand count pluralises inline the way `DuelResult` already does —
  `{n} {n === 1 ? "hand" : "hands"}` — rather than through a shared helper, because one shared
  pluraliser for two screens is a module that exists to save four characters.
- `key={duel.duelId}` — the one use the row's id has.
- **No opponent, in any form.** `RecentDuel` carries none by construction (`TASK-031103`), so there
  is nothing to print; do not reach back to the raw response for one.

## Out of scope

- Paging, *show more*, filtering, a duel that can be clicked into — `EPIC-04` and `EPIC-08`.
- Colour per outcome. A win and a loss read the same here; the result screen is where a duel gets
  its colour, and no design for this strip exists.
- Changing anything the balance or the no-profile state renders.

## Tests

`web-client/src/profile/ProfileStrip.test.tsx`, in the existing describe block
`"the profile strip"`.

| Test | Proves |
| --- | --- |
| `shows one line per duel, with its outcome, coin, hands and time` | two rows, deliberately different in every field: a `WON` `+1` **one-hand** duel finished in 2026, and a `LOST` `−1` nine-hand duel finished in 2025. Exactly two `listitem`s; the first contains `Won`, `+1`, `1 hand` and `2026`; the second contains `Lost`, `−1`, `9 hands` and `2025`. Two rows because one could not tell a mapped list from a single hardcoded line, and `1 hand` because the singular is the case a naive template gets wrong |
| `says there are no duels yet when the list is empty` | a profile with `duels: []` puts the sentence on screen and renders **zero** `listitem`s |

The year is what the time assertion checks, not a whole formatted date: the row is formatted in the
runner's own locale on purpose, and every locale prints the year in digits. `profile-text.test.ts`
is where the format itself is pinned, against a fixed locale.

Two tests added. Three hundred and forty-eight exist, so the suite reports **350**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 350 passed (350)` | two ran and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks, lints, is formatted, and holds the colour-literal rule |

**Name the edit that makes each assertion red** — run each, quote both in the PR, revert:

1. Render only `duels[0]` → `shows one line per duel, with its outcome, coin, hands and time` fails
   on the count.
2. Print `hands` unconditionally → the same test fails on `1 hand` while everything else passes.
3. Render the empty list as nothing at all → `says there are no duels yet when the list is empty`
   fails on the sentence.

## Acceptance criteria

- [ ] `the profile strip > shows one line per duel, with its outcome, coin, hands and time` passes
- [ ] `the profile strip > says there are no duels yet when the list is empty` passes
- [ ] The three tests `TASK-031107` added still pass, unchanged
- [ ] `ProfileStrip.tsx` still calls no hook, and still contains no `<h1>`…`<h6>`
- [ ] The list is rendered from `state.duels` in the order given, with no `sort` and no `reverse`
- [ ] `npm run --silent test` reports `Tests  350 passed (350)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
