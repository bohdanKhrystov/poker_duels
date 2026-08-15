---
schema: 2
id: TASK-030802
title: The verdict is read off the winner and your seat, and nothing else
type: task
status: done
parent: STORY-0308
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, result]
depends_on: [TASK-030801]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +255 passed \(255\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is a win when the winner is your seat'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is a draw when the outcome names no winner'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is unknown when the reader has no seat'
  - cd web-client && npm run check
---

## Goal

One function turns `DuelOutcome.winner` and the store's `mySeat` into a verdict, and one turns that
verdict into the word the design prints. No stack is compared, no chip counted.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/outcome-text.ts` | create |
| `web-client/src/result/outcome-text.test.ts` | create |
| `web-client/src/result/outcome-fixture.ts` | read — `anOutcome` |
| `web-client/src/table/action-text.ts` | read — the same shape one story earlier |

## Scope

- The whole of `outcome-text.ts`, verbatim:

  ```ts
  import type { DuelOutcome } from "../protocol";

  /** How the duel ended, from the reader's side of the table. */
  export type Verdict = "win" | "loss" | "draw" | "unknown";

  /**
   * The verdict, read off the two fields that carry it: the winner the server
   * named, and the seat this client was given in `RoomJoined`.
   *
   * Comparing the final stacks would reach the same answer almost always and be
   * a client asserting a game fact — exactly what `EPIC-03` forbids. A draw is
   * decided first, because `winner: null` is a draw for whoever is reading it,
   * seat or no seat (`ADR-0015`).
   */
  export function verdictOf(
    outcome: DuelOutcome,
    mySeat: number | null,
  ): Verdict {
    if (outcome.winner === null) return "draw";
    if (mySeat === null) return "unknown";
    return outcome.winner === mySeat ? "win" : "loss";
  }

  /** The verdict in the design's words (`design/screens/duel-end.html`). */
  export function verdictHeadline(verdict: Verdict): string {
    switch (verdict) {
      case "win":
        return "Victory";
      case "loss":
        return "Defeat";
      case "draw":
        return "Draw";
      case "unknown":
        return "Duel over";
    }
  }
  ```

- `"unknown"` is the honest answer when the client holds no seat, not a fourth guess: nothing in the
  outcome says which side the reader is on. It is unreachable through the lobby today — `RoomJoined`
  precedes every duel frame — and is written because `DuelState.mySeat` is `number | null` and a
  screen that narrows it by assertion would be asserting a game fact.

## Out of scope

- The coin — `TASK-030803`, in this same file.
- Anything rendered. No component imports this until `TASK-030805`.
- Naming the hand, the board, or how the last pot was won. `DuelOutcome` carries none of it and
  `EPIC-08` owns the retelling.

## Tests

`web-client/src/result/outcome-text.test.ts`, describe block `"the verdict"`.

| Test | Proves |
| --- | --- |
| `is a win when the winner is your seat` | `verdictOf(anOutcome({ winner: 1 }), 1)` is `"win"` |
| `is a loss when the winner is the other seat` | `verdictOf(anOutcome({ winner: 1 }), 0)` is `"loss"` |
| `is a draw when the outcome names no winner` | `verdictOf(anOutcome({ winner: null }), 0)` and `verdictOf(anOutcome({ winner: null }), null)` are both `"draw"` — a draw is a draw to a seatless reader too |
| `is unknown when the reader has no seat` | `verdictOf(anOutcome({ winner: 1 }), null)` is `"unknown"`, and is neither `"win"` nor `"loss"` |
| `names each verdict in the words the design uses` | `verdictHeadline` maps `win`/`loss`/`draw`/`unknown` to `"Victory"`, `"Defeat"`, `"Draw"`, `"Duel over"` |

Both loss cases matter: the first fixture has `finalStacks: [19400, 4600]`, so seat `0` holds the
larger stack while seat `1` is named the winner. A verdict read off the stacks answers `"win"` for
seat `0` and fails the second test.

Five tests. Two hundred and fifty exist, so the suite reports **255**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 255 passed (255)` | the five ran and the two hundred and fifty before them still do |
| the three `--reporter=verbose` greps | the win, the draw and the seatless case each exist by name |
| `npm run check` | typechecks — `verdictHeadline`'s `switch` is exhaustive without a `default` |

**Name the edit that makes each assertion red** — run both before opening the PR:

1. Decide the verdict by stacks — `outcome.finalStacks[mySeat] > outcome.finalStacks[1 - mySeat] ? "win" : "loss"` → `is a loss when the winner is the other seat` fails with `expected 'win' to be 'loss'`. Revert.
2. Check the seat before the draw — move `if (mySeat === null) return "unknown"` above the `winner === null` line → `is a draw when the outcome names no winner` fails on its second assertion. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the verdict > is a win when the winner is your seat` passes
- [ ] `the verdict > is a loss when the winner is the other seat` passes
- [ ] `the verdict > is a draw when the outcome names no winner` passes
- [ ] `the verdict > is unknown when the reader has no seat` passes
- [ ] `the verdict > names each verdict in the words the design uses` passes
- [ ] `outcome-text.ts` contains no reference to `finalStacks` and no comparison operator on chips
- [ ] `npm run --silent test` reports `Tests  255 passed (255)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
