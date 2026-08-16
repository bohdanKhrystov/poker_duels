---
schema: 2
id: TASK-031207
title: The result states the outcome the script's last frame carried, from either seat
type: task
status: ready
parent: STORY-0312
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, test, end-to-end]
depends_on: [TASK-031206]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +372 passed \(372\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives the two seats opposite verdicts, off the winner and the seat alone'
  - cd web-client && npm run check
---

## Goal

After a whole duel has been replayed, the result screen states the winner, the hand count and both
final stacks that the script's own `DuelFinished` carried — and states them differently to the two
seats, because the two seats are different.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/whole-duel.test.tsx` | modify |
| `web-client/src/result/DuelResult.tsx` | read — `metaLine`'s words and their order |
| `web-client/src/result/outcome-text.ts` | read — `verdictHeadline`, `coinLine` |
| `web-client/src/table/chips.ts` | read — `formatChips`, the grouping the meta line uses |

## Scope

- Two tests added to `describe("a whole duel through the client")`. No existing test changes: this
  ticket adds claims about the screen the run already reaches, and takes none away.
- Both tests read their expectations off the script's **last server step** — the `DuelFinished`
  frame — never off a constant written into the test, and never off the store.
- No production file changes.

## Out of scope

- Cards on the result screen. There are none, and proving so is `TASK-031208`'s sweep.
- A draw or an unknown seat. The script has a winner and the client holds a seat (`ADR-0015`'s draw
  is already `TASK-030808`'s).
- The coin **balance**. That is the server's and arrives over HTTP (`STORY-0311`); the result screen
  states the one coin the duel moved and nothing else.

## Tests

`web-client/src/e2e/whole-duel.test.tsx`, same describe block.

| Test | Proves |
| --- | --- |
| `states the hand count and every final stack the last frame carried` | for **each** seat, the result region's text contains `${handsPlayed} hands`, `You ${formatChips(finalStacks[viewerSeat])}` and `Your rival ${formatChips(finalStacks[1 - viewerSeat])}`; and contains **neither** `${handsPlayed + 1}` **nor** the swapped labelling `You ${formatChips(finalStacks[1 - viewerSeat])}` — the freezeout leaves the two stacks different, so a screen that put the rival's chips beside *You* fails here rather than passing on a coincidence |
| `gives the two seats opposite verdicts, off the winner and the seat alone` | the same `DuelOutcome`, replayed from the two seats, puts `Victory` and `+1 duel coin` on the winner's screen and `Defeat` and `−1 duel coin` on the loser's; `Victory` appears in exactly one of the two runs, and `Defeat` in exactly one. One outcome and two inputs, so a verdict that came from a constant, from the stacks, or from the first seat's answer cached anywhere cannot produce both words |

Two tests added. Three hundred and seventy exist, so the suite reports **372**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. In `DuelResult.tsx`, label the stacks `seat === mySeat ? "Your rival" : "You"` → `states the hand
   count and every final stack the last frame carried` fails on the swap clause, and **passes** every
   clause above it, which is why the swap clause is in the ticket.
2. In `outcome-text.ts`, return `"win"` unconditionally from `verdictOf` → `gives the two seats
   opposite verdicts, off the winner and the seat alone` fails on the loser's run.
3. In `DuelResult.tsx`, print `outcome.finalStacks.length` instead of `outcome.handsPlayed` → the
   first test fails on the hand count.

## Acceptance criteria

- [ ] `a whole duel through the client > states the hand count and every final stack the last frame carried` passes
- [ ] `a whole duel through the client > gives the two seats opposite verdicts, off the winner and the seat alone` passes
- [ ] Both tests read `handsPlayed`, `winner` and `finalStacks` off the script's last server step
- [ ] Both tests run over both seats, and the second asserts each word appears in exactly one of them
- [ ] The three tests already in the file pass with their bodies unchanged
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  372 passed (372)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
