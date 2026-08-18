---
schema: 2
id: TASK-041116
title: A duel line names the opponent it was played against
type: task
status: backlog
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, profile, ui, identity]
depends_on: [TASK-041115]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +416 passed \(416\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a rival who has a name and stands in for one who does not, on two lines of one list'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the rest of the line exactly as it was'
  - cd web-client && npm run check
---

## Goal

Every duel line says who it was played against — the opponent's name, or what stands in for a player
who has none — and `EPIC-03`'s promise that a result line would stop being anonymous is kept.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.tsx` | modify — one span in the list item |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/name-text.ts` (`nameOrNone`),
`web-client/src/profile/recent-duels.ts` (`RecentDuel`).

## Scope

- Each `<li>` renders `nameOrNone(duel.opponentDisplayName)` alongside the outcome word, the coin
  delta, the hand count and the time. Position within the line is the implementer's; presence is
  not.
- **Through `nameOrNone`, never through a local `??`.** A nameless opponent on this list and a
  nameless player on the strip above it must read the same, because `ADR-0052` §5 makes one of them
  a player whose name was removed and the two must be indistinguishable.
- Nothing else about the line changes: the same words, the same order, the same `finishedAtText`.

## Out of scope

- Marking a line whose opponent's name was removed. **A refusal, not an omission:** `ADR-0052` §5
  forbids it in as many words — no badge, no *name removed*, no tooltip, no distinct styling — and
  `DuelSummaryResponse` deliberately carries nothing that could tell. `TASK-041117` is the test that
  keeps it true.
- Linking the name to anything — `EPIC-05`.
- The opponent's id, in any form. It never reaches this component; `TASK-041105` drops it at the
  parse.

## Tests

`web-client/src/profile/ProfileStrip.test.tsx`, describe block `"the profile strip"`.

| Test | Proves |
| --- | --- |
| `names a rival who has a name and stands in for one who does not, on two lines of one list` | One strip, two duel lines: `aDuelLine({ duelId: "d-1", opponentDisplayName: "Ada" })` and `aDuelLine({ duelId: "d-2", opponentDisplayName: null })`. The first list item contains `Ada`, the second contains `nameOrNone(null)`, and the first does not contain the treatment. Fails against a line that prints a constant, against one that drops the name when it is `null`, and against one that puts the same text on both lines |
| `keeps the rest of the line exactly as it was` | The same two lines still carry their outcome word, their signed coin delta, their hand count with the right plural and their formatted time — asserted per line, as the merged test does. Fails against a name that replaced part of the line rather than joining it |

Two tests added to 414, so the suite reports **416**.

## Acceptance criteria

- [ ] `the profile strip > names a rival who has a name and stands in for one who does not, on two
      lines of one list` passes, with both lines in one list
- [ ] `the profile strip > keeps the rest of the line exactly as it was` passes
- [ ] Every merged `ProfileStrip.test.tsx` test passes unchanged, including `shows one line per
      duel, with its outcome, coin, hands and time`
- [ ] `grep -c 'opponentPlayerId' web-client/src/profile/ProfileStrip.tsx` returns `0`
- [ ] `npm run --silent test` reports `Tests  416 passed (416)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
