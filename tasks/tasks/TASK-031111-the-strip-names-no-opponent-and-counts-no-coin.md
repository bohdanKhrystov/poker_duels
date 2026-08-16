---
schema: 2
id: TASK-031111
title: The strip names no opponent and counts no coin
type: task
status: backlog
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, profile, guard, tests]
depends_on: [TASK-031110]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +358 passed \(358\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no opponent identifier anywhere on the screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states the balance the server sent, never the sum of the deltas'
  - cd web-client && npm run check
---

## Goal

Two whole-surface guards over the finished strip: the opponent's identifier reaches no part of the
DOM, from the raw response all the way to the screen, and the balance on screen is the server's
number rather than one the client added up.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-no-derivation.test.tsx` | create |
| `web-client/src/table/bar-no-derivation.test.tsx` | read — the scan to copy, including the attribute pass `TASK-030715` added |
| `web-client/src/profile/profile-strip.ts` | read — `readProfileStrip`, the whole path under test |
| `web-client/src/profile/ProfileStrip.tsx` | read — what is rendered |

## Scope

- One test file, tests only, in the shape the table and bar guards already set: helpers declared in
  the file, no shipped module, no export.
- Both tests drive the **whole path**, not the component alone: build the two API bodies, run them
  through `readProfileStrip` with a recording `fetch`, and render `<ProfileStrip state={…}>` with
  what comes back. A guard that starts at the component would miss a leak the parse allowed.
- The scan reads everything a player can receive, copied from `bar-no-derivation.test.tsx`: every
  text node, every `aria-label`, every `title`, **and every attribute value of every element** —
  `TASK-030715` was filed because a value can reach the DOM as an attribute with nothing printing
  it.

## Out of scope

- The socket's frames. `table/no-derivation.test.tsx` and `bar-no-derivation.test.tsx` guard those
  surfaces and neither is touched here.
- Asserting that every digit on screen is a number the wire carried. The finishing time is a
  formatted date whose digits are the reader's locale's, so that rule would either be false or would
  have to special-case the one thing it cannot check. The balance rule below is the sharp half of it.
- Any production change. If either test is red on first run, that is a defect found; report it, and
  fix it here only if the fix is one line in `ProfileStrip.tsx`.

## Tests

`web-client/src/profile/profile-no-derivation.test.tsx`, describe block `"the profile strip's
surface"`.

| Test | Proves |
| --- | --- |
| `puts no opponent identifier anywhere on the screen` | two rows whose bodies carry **two distinct** `opponentPlayerId`s (`"player-77"`, `"player-88"`); after the read and the render, neither string appears in any text node, any `aria-label`, any `title`, or any attribute value — and, so the guard cannot pass by scanning nothing, the same scan **does** find the outcome word of each row |
| `states the balance the server sent, never the sum of the deltas` | a balance of `5` beside duels of `+1`, `−1`, `0` — whose sum is `0` — puts `5` on screen; then a balance of `−2` beside duels of `+1`, `+1`, `−1` — whose sum is `+1` — puts `−2` on screen. Two cases, each chosen so a client that added the deltas up would print a different number |

The second test is `EPIC-03`'s constraint made executable for this screen: the balance is the
server's, computed as wins minus losses in `poker-server` and stated here. The fixtures are picked
so the hand counts and the deltas cannot collide with either balance.

Two tests added. Three hundred and fifty-six exist, so the suite reports **358**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 358 passed (358)` | two ran and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote both in the PR, revert:

1. Add `opponentPlayerId` back to `RecentDuel`, carry it through the parse and render it as
   `title={duel.opponentPlayerId}` on the row → `puts no opponent identifier anywhere on the screen`
   fails, and it fails on the **attribute** pass, which is the pass a text-only scan would not have.
2. Render the balance as the sum of the deltas → `states the balance the server sent, never the sum
   of the deltas` fails on both cases.
3. Narrow the scan to text nodes only, then repeat edit 1 → the guard goes quiet, which is why the
   attribute pass is in the ticket. Say in the PR that you ran this one.

## Acceptance criteria

- [ ] `the profile strip's surface > puts no opponent identifier anywhere on the screen` passes
- [ ] `the profile strip's surface > states the balance the server sent, never the sum of the deltas`
      passes
- [ ] The first test asserts against two different opponent identifiers, and asserts the scan found
      the outcome words
- [ ] The second test uses two balances, neither equal to the sum of its own duels
- [ ] The scan covers text nodes, `aria-label`, `title` **and** every attribute value
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  358 passed (358)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
