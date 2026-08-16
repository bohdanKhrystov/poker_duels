---
schema: 2
id: TASK-031209
title: A hand won without a showdown shows no rival card at all
type: task
status: ready
parent: STORY-0312
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, test, secrecy, end-to-end]
depends_on: [TASK-031208]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +377 passed \(377\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stay hidden for a whole hand that never reached a showdown'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'are shown in exactly the hands a reveal named, and no others'
  - cd web-client && npm run check
---

## Goal

The hands that ended on a fold are named, one by one, and across the whole of each of them the
rival's cards never reached the screen — and the hands in which they *did* are exactly the hands a
`HandRevealed` named, no more and no fewer.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/duel-secrecy.test.tsx` | modify |
| `web-client/src/e2e/scripted-duel.ts` | read — `rivalHoleCards` and the decoded steps |
| `web-client/src/e2e/drive-duel.tsx` | read — `onStep` |

## Scope

- Two tests added to `describe("the rival's cards")`. The three already there keep their bodies,
  their names and their plants; `cardsShown` is reused unchanged, and no assertion in the file is
  weakened.
- The two hand sets are computed from **the frames the viewer received**, in the same walk:
  - *revealed hands* — the hand numbers whose `Events` frames carry a `HandRevealed` naming seat
    `1 - viewerSeat`;
  - *fold-win hands* — the hand numbers whose frames carry no `HandRevealed` at all, for either seat.

  `TASK-031204` already proved both sets are non-empty in the committed script; this ticket asserts
  it again from the *rendered* run, because a driver that stopped early would make a claim about
  hands it never reached.
- The second test's set equality is the whole point of the pair: it closes both directions at once —
  nothing hidden that should have been shown, nothing shown that should have been hidden — and it is
  what stops the first test passing because the client renders no cards at all.

## Out of scope

- Which player won a fold-win hand, and by what. The client is told neither and must ask neither.
- A mucked hand at showdown. `ADR-0008` says the loser mucks and appears in no event, so a showdown
  the viewer lost is a hand with no `HandRevealed` for the rival and falls out of the sets above
  correctly without a special case. Do not add one.
- Any production change.

## Tests

`web-client/src/e2e/duel-secrecy.test.tsx`, same describe block. Both run over **both** seats.

| Test | Proves |
| --- | --- |
| `stay hidden for a whole hand that never reached a showdown` | the fold-win hand set is non-empty and is named in the assertion message; and for every step whose live hand is one of them, `cardsShown(container, rivalHoleCards[hand])` is empty — including the step that carries that hand's `HandFinished`, which is the last moment a client could have decided to show a hand nobody paid to see |
| `are shown in exactly the hands a reveal named, and no others` | the sorted set of hands in which the rival's cards were ever on screen `toEqual`s the sorted set of hands whose frames carried a `HandRevealed` naming the rival; both sets are non-empty and neither is the whole run |

Two tests added. Three hundred and seventy-five exist, so the suite reports **377**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. Doctor the `Snapshot` frames of one fold-win hand to carry the rival's cards, as `TASK-031208`'s
   fourth plant doctors one → `stay hidden for a whole hand that never reached a showdown` fails and
   names that hand.
2. Delete the non-emptiness assertion on the fold-win set, then filter every step out of the walk →
   the first test passes saying nothing. Say in the PR that you ran this one; it is why the
   non-emptiness assertion is a criterion and not a comment.
3. In `Hand.tsx`, draw a `CardBack` in place of a `CardFace` whenever the row is the rival's → `are
   shown in exactly the hands a reveal named, and no others` fails on the shown set being empty,
   which proves the equality is not vacuously true on both sides.

## Acceptance criteria

- [ ] `the rival's cards > stay hidden for a whole hand that never reached a showdown` passes
- [ ] `the rival's cards > are shown in exactly the hands a reveal named, and no others` passes
- [ ] Both tests run over both seats, and both assert their hand sets are non-empty before using them
- [ ] Both sets are computed from the frames the viewer received, never from `rivalHoleCards` alone
      and never from the other seat's script
- [ ] The three tests already in `duel-secrecy.test.tsx` pass with their bodies unchanged
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  377 passed (377)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
