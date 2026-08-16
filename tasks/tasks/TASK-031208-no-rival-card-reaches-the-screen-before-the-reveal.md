---
schema: 2
id: TASK-031208
title: No rival card reaches the screen before the frame that reveals it
type: task
status: backlog
parent: STORY-0312
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, test, secrecy, end-to-end]
depends_on: [TASK-031207]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +375 passed \(375\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never reach the screen before the frame that revealed them'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'are found when a frame is doctored to carry them, and when the DOM is'
  - cd web-client && npm run check
---

## Goal

Across a whole duel, at **every** step, no card the rival was actually holding appears anywhere in
the rendered DOM until a frame the viewer received revealed it — and the sweep that says so is proven
to fire.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/duel-secrecy.test.tsx` | create |
| `web-client/src/e2e/drive-duel.tsx` | read — `driveScriptedDuel` and its `onStep` |
| `web-client/src/e2e/scripted-duel.ts` | read — `rivalHoleCards`, the secret being checked |
| `web-client/src/table/card-text.ts` | read — `cardText`, the words a card is spoken in |
| `web-client/src/table/no-derivation.test.tsx` | read — the DOM sweep this one is modelled on |

## Scope

- **The claim is over the rendered output, not over the store.** Every check runs on the `container`
  `onStep` hands it, after the step has been replayed. A store-level assertion would miss the one
  path that matters: something rendering a frame the store merely passed through.
- The secret is `seat.rivalHoleCards` — what the *other* seat actually held, hand by hand, taken from
  the other seat's own frames by `TASK-031202`. The viewer's own script never carries it until the
  reveal, which is exactly why it has to come from the fixture.
- One private checker, so the same code proves the claim and its own falsifiability:

  ```tsx
  function cardsShown(container: HTMLElement, cards: readonly string[]): string[]
  ```

  It returns one entry per card of `cards` that the container names, by any of three routes:
  1. an element with `role="img"` whose `aria-label` is `cardText(card).label`;
  2. that same label anywhere in the joined text nodes, `aria-label`s and `title`s — a card named
     outside a card element is still a card a screen reader says;
  3. the raw two-character card string as a whole word, case-sensitive — belt and braces for a leak
     that printed `"Ah"` without ever going through `cardText`.
- State tracked while walking, from the frames the viewer received and nothing else:
  - the live hand number, from the latest `Snapshot`'s `view.handNumber`; a change resets the flag
    below;
  - whether an `Events` frame in **this hand** has carried a `HandRevealed` naming seat
    `1 - viewerSeat`.

  A step is checked only once a `Snapshot` has established a hand. The reveal flag is set from the
  step being replayed *before* that step's DOM is judged, because the server sends `Events` then
  `Snapshot` and the reveal is legitimate from the `Events` frame onwards.
- Every test runs over **both** seats, and every failure message names the seat, the step index, the
  hand number and the cards found.

## Out of scope

- A hand that never reached a showdown — `TASK-031209`, which is the *existence* claim this one
  cannot make.
- Numbers, hand names and winner talk. `no-derivation.test.tsx`, `bar-no-derivation.test.tsx`,
  `result-no-derivation.test.tsx` and `profile-no-derivation.test.tsx` own those four surfaces and
  none of them is touched here.
- Any production change. If a test here is red, that is a leak found: report it, stop, and file it.

## Tests

`web-client/src/e2e/duel-secrecy.test.tsx`, describe block `"the rival's cards"`.

| Test | Proves |
| --- | --- |
| `never reach the screen before the frame that revealed them` | for each seat, at every step where the rival is unrevealed in the live hand, `cardsShown(container, rivalHoleCards[hand])` is empty; and the number of steps actually judged is greater than 40, asserted, so a walk that checked nothing cannot pass |
| `do reach it once the reveal has arrived` | for each seat there is at least one step at which **both** of the rival's cards are shown, and every such step is at or after the step that revealed them. Without this, the test above would pass on a client that rendered no cards at all; the *set* of hands in which it happens is `TASK-031209`'s claim, not this one's |
| `are found when a frame is doctored to carry them, and when the DOM is` | four plants, each asserted on its own so no route is dead. Three are DOM plants on a container taken at a pre-reveal step, one per route: a `<span role="img" aria-label="…">` carrying the rival card's spoken name; a plain text node carrying the same name; a text node carrying the raw `"Ah"`-style string. The fourth is the real one: replay the script with one pre-reveal `Snapshot` frame **doctored before `socket.receive`** so that `view.seats[1 - viewerSeat].holeCards` holds the fixture's secret — decode it, `copy` the seat, re-encode with `JSON.stringify` — and the run fails the first test's rule. This is `TASK-021207`'s planted card one layer up, and it is the only plant that exercises decoder, store, table and sweep together |

Three tests added. Three hundred and seventy-two exist, so the suite reports **375**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. Delete the doctoring from the fourth plant, so it replays the script untouched → `are found when a
   frame is doctored to carry them, and when the DOM is` fails, because the run it expects to be
   caught is now clean. That is the check that the plant is a plant and not a formality.
2. Narrow `cardsShown` to route 1 only, then plant the plain text node → the same test fails, which
   is why routes 2 and 3 exist.
3. Set the reveal flag on the first step of every hand → `never reach the screen before the frame
   that revealed them` still passes, and `do reach it once the reveal has arrived` fails on the
   "at or after" clause. Say in the PR that you ran this one: it is the check that the ordering, and
   not merely the set of cards, is what is being asserted.

## Acceptance criteria

- [ ] `the rival's cards > never reach the screen before the frame that revealed them` passes
- [ ] `the rival's cards > do reach it once the reveal has arrived` passes
- [ ] `the rival's cards > are found when a frame is doctored to carry them, and when the DOM is` passes
- [ ] Every test runs over both seats
- [ ] The first test asserts the number of steps it judged, and that number exceeds 40
- [ ] `cardsShown` covers all three routes, each is planted against separately, and the fourth plant
      doctors a `Snapshot` frame rather than the DOM
- [ ] Every check reads the `container` `onStep` was given; no test reads `client.store` or a decoded
      frame's `holeCards` in place of the DOM
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  375 passed (375)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
