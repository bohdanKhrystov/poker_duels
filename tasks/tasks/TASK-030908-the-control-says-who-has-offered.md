---
schema: 2
id: TASK-030908
title: The control says who has offered, and reads it from either side
type: task
status: done
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, result, ui]
depends_on: [TASK-030907]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +557 passed \(557\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows your own offer standing, and takes the button away'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the same offer from the other side above a live button'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the button away for seat zero too'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the button while nobody has offered'
  - cd web-client && npm run check
---

## Goal

The control renders the design's three quiet states — nobody, you, your rival — off the seats the
server named and the seat it gave this client.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchControl.tsx` | modify |
| `web-client/src/result/RematchControl.test.tsx` | modify |
| `web-client/src/result/rematch-stand.ts` | read — `rematchStand(offers, mySeat)` |

## Scope

- Props gain `offers: readonly number[]`, and the component calls `rematchStand(props.offers,
  props.mySeat)`. **It compares no seat number itself** — that comparison has exactly one home.
- Three states, from `design/screens/rematch-states.html`:
  - `mine` — the button is **replaced** by the chip `Rematch offered — waiting for your rival`
    (em dash, U+2014). Classes: `rounded-medium border border-accent bg-accent-subtle px-5 py-4
    text-center leading-tight font-medium text-text`. No spinner, no countdown.
  - `theirs` and not `mine` — the line `Your rival offers a rematch`, class
    `text-center text-small font-medium text-accent`, **above** a live `Rematch` button. Accepting
    is the same press it always was.
  - neither — the button alone, exactly as `TASK-030907` left it.
- *Your rival* rather than a name, because no frame on this screen carries one: `DuelOutcome` has
  `winner`, `handsPlayed` and `finalStacks`, and `DuelResult`'s own meta line already says
  `You` / `Your rival`. The design's `ImKate` is a name the wire does not have.
- When `mine` and `theirs` are both true the chip is what shows. Say in a comment that the wire
  cannot produce it — the second offer starts the duel and sends no `RematchOffered` (`ADR-0044`
  §4) — so this is a total function's answer, not a state the screen is built for.

## Out of scope

- Refusals — `TASK-030909`.
- Sending — `TASK-030910`.
- Any word about the window closing. The wire carries no deadline.

## Tests

`web-client/src/result/RematchControl.test.tsx`, describe block `"the rematch control"`. Four added.

The first two hold the **same `offers` array** and move only the viewer, so a component that read
the offer without reading `mySeat` renders the same thing twice and fails one of them.

| Test | Proves |
| --- | --- |
| `shows your own offer standing, and takes the button away` | `offers={[1]}`, `mySeat={1}` ⇒ `Rematch offered — waiting for your rival` is on screen and `queryByRole("button", { name: "Rematch" })` is `null` |
| `shows the same offer from the other side above a live button` | `offers={[1]}`, `mySeat={0}` ⇒ `Your rival offers a rematch` is on screen, the `Rematch` button is on screen, and the line **precedes** the button in document order (`compareDocumentPosition`) |
| `takes the button away for seat zero too` | `offers={[0]}`, `mySeat={0}` ⇒ the chip, no button. With the first test, neither seat number can be a constant anywhere in the component |
| `keeps the button while nobody has offered` | `offers={[]}`, `mySeat={1}` ⇒ the `Rematch` button is on screen, and neither the chip nor the rival line is |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 557 passed (557)` | four added to 553 |
| the four `--reporter=verbose` greps | all four names exist |

**Name the edit that makes each assertion red:**

1. Swap `mine` and `theirs` at the call site → `shows your own offer standing, and takes the button
   away` fails, and so does the test after it. Revert.
2. Render the rival line **below** the button → `shows the same offer from the other side above a
   live button` fails on the position check while both are still on screen. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the rematch control > shows your own offer standing, and takes the button away` passes
- [ ] `the rematch control > shows the same offer from the other side above a live button` passes
- [ ] `the rematch control > takes the button away for seat zero too` passes
- [ ] `the rematch control > keeps the button while nobody has offered` passes
- [ ] `RematchControl.tsx` contains no `includes(`, no `===` between two seat values and no numeric literal — the comparison is `rematchStand`'s alone
- [ ] The four tests from `TASK-030907` are unchanged
- [ ] `npm run --silent test` reports `Tests  557 passed (557)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
