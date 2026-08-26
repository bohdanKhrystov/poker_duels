---
schema: 2
id: TASK-041204
title: The store outranks the address, and a seated player's address stops lying
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-041228]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the duel to a player a frame seats, whatever address they were reading'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces the address a frame overruled, and stacks no entry doing it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the address alone while no frame has seated anybody'
  - cd web-client && npm run check
---

## Goal

A player reading the record when a frame seats them is shown the duel, and the address is replaced
with `/` so it stops claiming they are somewhere they are not.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §2 and §3;
`web-client/src/routing/use-screen.ts`.

## Scope

- When `outcome`, `view` or `roomCode` puts a store-owned screen on the display **and** the address
  names a chosen screen, the client calls `leave()`. The branch order does not move — it is already
  right from `TASK-041203`; this ticket adds the replace, so the address agrees with what is
  rendered.
- It is a **replace**, never a push: the player did not navigate, so no history entry is created.
  `ADR-0076` §3's words are *"replaces the fragment with `/` so the address does not lie about where
  they are."*
- It runs in an effect, not during render — writing to `history` while rendering is a side effect in
  a render path, and React may run that render twice.
- It runs **only** when the address names something other than `"first"`. A player already at `/`
  must not have `replaceState` called on every frame that arrives.
- Carry `ADR-0076` §3's sentence as a comment: *when the two disagree there is one authority, and it
  is the store.*

## Out of scope

- **Giving the table, the waiting screen or the result screen an address.** `ADR-0076` §2 forecloses
  it permanently and by rule: all three are chosen by frames, and an address claiming a seat is a
  client asserting a game fact. **A refusal, not an omission** — a criterion greps `screen.ts` for
  the three words.
- **Restoring the address when the duel ends.** The player is at `/` and reaches a screen by asking
  for it again. Nothing in `ADR-0076` promises a return, and synthesising one would be the client
  deciding where a player wanted to be.
- Anything about the account screen, which does not exist yet.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, inside the existing `describe("the lobby")`. Reset
`window.location.hash` in the existing `beforeEach` (or add one).

| Test | Proves |
| --- | --- |
| `shows the duel to a player a frame seats, whatever address they were reading` | With `window.location.hash` set to `"#/duels"` before the render and a store carrying a `view`, the duel table is on screen and the record's heading is not. `ADR-0076` §3's branch order, asserted from the address side rather than from the click side |
| `replaces the address a frame overruled, and stacks no entry doing it` | The same state: `window.location.hash` settles to `""` and `history.length` is **unchanged** from the value captured before the render. Two assertions in one test, because a `push` would satisfy the first and break *Back* forever |
| `leaves the address alone while no frame has seated anybody` | With `window.location.hash` set to `"#/duels"` and a store carrying no `outcome`, no `view` and no `roomCode`, the address is still `"#/duels"` after the render and the record is on screen. Fails against an effect with no guard, which would send every reader of the record straight back to the lobby |

## Acceptance criteria

- [ ] `the lobby > shows the duel to a player a frame seats, whatever address they were reading`
      passes
- [ ] `the lobby > replaces the address a frame overruled, and stacks no entry doing it` passes,
      asserting the unchanged `history.length` against a value captured before the render
- [ ] `the lobby > leaves the address alone while no frame has seated anybody` passes
- [ ] Every pre-existing test in `Lobby.test.tsx` passes unchanged
- [ ] `grep -cE '"(table|waiting|result)"' web-client/src/routing/screen.ts` returns `0`
- [ ] `grep -c 'pushState' web-client/src/lobby/Lobby.tsx` returns `0`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Delete the replace entirely, keeping the branch order.
   **`replaces the address a frame overruled, and stacks no entry doing it` reddens alone.** The
   first test still passes — the duel table renders either way — which is the reason these are two
   tests and not one, and the reason the second one exists at all. Revert.
2. Make the replace a `push` — call `open` on the first screen, or assign `location.hash = "/"`.
   **`replaces the address a frame overruled…` reddens on `history.length`** while its address
   assertion still passes. Run it: a push is the mutation that looks correct in the address bar and
   silently makes the browser's *Back* retrace into a duel the player has since left.
3. Drop the `screen !== "first"` guard so the effect runs on every render.
   **`leaves the address alone while no frame has seated anybody` reddens alone**, and it is the only
   test in this file that can see it — every other test in `Lobby.test.tsx` renders at `/`, where an
   unguarded replace is indistinguishable from no replace at all. This is the mutation to run first;
   a fixture at the default address cannot detect it.
4. Move the replace out of the effect and into the render body.
   **Nothing reddens** under React 18's test environment. Record that: it is a real defect this
   ticket's tests do not gate, it is caught by review and by `StrictMode` in the browser, and saying
   so is better than implying the suite covers it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
