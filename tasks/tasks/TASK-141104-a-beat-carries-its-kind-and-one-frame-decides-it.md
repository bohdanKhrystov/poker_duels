---
schema: 2
id: TASK-141104
title: A beat carries its kind, and one frame decides it
type: task
status: done
parent: STORY-1411
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [client, table, store]
depends_on: [TASK-141103]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-after.txt" 2>&1; grep -qF 'Tests  92 passed (92)' "${TMPDIR:-/tmp}/state-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/routing/room-standing.test.ts > "${TMPDIR:-/tmp}/standing-after.txt" 2>&1; grep -qF 'Test Files  1 passed (1)' "${TMPDIR:-/tmp}/standing-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - cd web-client && cp src/store/duel-state.ts "${TMPDIR:-/tmp}/duel-state.fixed.ts" && perl -0pi -e 's/rivalShown\s*\?\s*"read"\s*:\s*"step"/"step"/' src/store/duel-state.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-always-step.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-state.fixed.ts" src/store/duel-state.ts; cmp -s "${TMPDIR:-/tmp}/duel-state.fixed.ts" src/store/duel-state.ts && grep -qF 'FAIL  src/store/duel-state.test.ts > the duel state > the same hand-ending view answers read for one seat and step for the other' "${TMPDIR:-/tmp}/state-always-step.txt" && grep -qF 'FAIL  src/store/duel-state.test.ts > the duel state > only the last step of a runout to a showdown is a read' "${TMPDIR:-/tmp}/state-always-step.txt" && grep -qF 'Tests  2 failed | 90 passed (92)' "${TMPDIR:-/tmp}/state-always-step.txt"
  - cd web-client && cp src/store/duel-state.ts "${TMPDIR:-/tmp}/duel-state.fixed2.ts" && perl -0pi -e 's/rivalShown\s*\?\s*"read"\s*:\s*"step"/"read"/' src/store/duel-state.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-state.test.ts > "${TMPDIR:-/tmp}/state-always-read.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-state.fixed2.ts" src/store/duel-state.ts; cmp -s "${TMPDIR:-/tmp}/duel-state.fixed2.ts" src/store/duel-state.ts && grep -qF 's ending is a step, because no rival card came with it' "${TMPDIR:-/tmp}/state-always-read.txt" && grep -qF 'FAIL  src/store/duel-state.test.ts > the duel state > lays out one step per StreetDealt and a final step for the whole snapshot' "${TMPDIR:-/tmp}/state-always-read.txt" && grep -qF 'Tests  4 failed | 88 passed (92)' "${TMPDIR:-/tmp}/state-always-read.txt"
  - sh -c 'test -z "$(git status --porcelain web-client/src/e2e)"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RevealStep` carries **how long it stands, named as a kind** — `hold: "step" | "read"` — and
`layOutReveal` sets it from `ADR-0120` §3's predicate over the one frame it is already given. Nothing
is scheduled differently yet and nothing on the screen changes: this ticket makes the classification
exist and pins that it is a **predicate** rather than a constant.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/routing/room-standing.test.ts` | modify |

Read [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
§§1 and 7.1–7.3, and
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§3. **No other file is opened.** `duel-store.ts` and `boot.ts` are `TASK-141105`'s and
`TASK-141106`'s; nothing under `web-client/src/e2e/` and no component is touched by any ticket in
this story.

**The third file is not optional and was measured**, not guessed: making `hold` required breaks one
object literal that never spells the type, so a grep for `RevealStep` does not find it —
`src/routing/room-standing.test.ts:58`, `error TS2741: Property 'hold' is missing`. `tsc --noEmit`
reports that one error and no other, so this set is exactly three and irreducible. It is the ordinary
cap and **not** `ADR-0068`'s `atomic:`.

## Scope

- **`RevealStep` gains one field**, exactly as `ADR-0136` §1 writes it:

  ```ts
  export interface RevealStep {
    readonly board: readonly string[];
    readonly street: Street;
    /** How long this beat stands, named as a kind — the milliseconds are the boot seam's. */
    readonly hold: "step" | "read";
  }
  ```

- **Every street step is `"step"`.** In `layOutReveal`'s backward loop, the object literal gains
  `hold: "step"` and nothing else changes.

- **The final step asks `ADR-0120` §3's question**, byte-for-byte as `ADR-0136` §1 writes it —
  reproduce this expression exactly, because a `verify:` gate mutates it by literal match:

  ```ts
  const rivalShown = view.seats.some(
    (seat) => seat.index !== view.viewerSeat && seat.holeCards.length > 0,
  );
  steps[streetDealt.length] = {
    board: view.board.cards,
    street: view.street,
    hold: rivalShown ? "read" : "step",
  };
  ```

  Three things about it are load-bearing and `ADR-0136` §1 states each: it reads
  **`view.viewerSeat`**, not `state.mySeat`, so the frame answers for itself and a hand-ending
  `Snapshot` that was **queued** behind another hand's ending still classifies itself correctly when
  `advanceReveal` folds it back through the reducer; it uses `.some` rather than
  `seats[1 - viewerSeat]`, so nothing depends on the array's order or on there being two seats; and
  nothing is computed, no event is consulted and no previous view is remembered.

- **A comment says why the client is allowed to read a hole card here.** One sentence, `why` and not
  `what`: the predicate chooses a schedule and never a face — the faces are `DuelTable.tsx`'s, off
  the same snapshot, and `PlayerView.of`'s `showCards` remains the only place a hole card is ever
  filtered (`ADR-0136` §5).

- **`layOutReveal`'s signature does not change**, `applyServerMessage`'s does not change, and
  **`advanceReveal` is byte-unchanged** — it drops steps and has no opinion about their length.

- **`room-standing.test.ts:58`'s literal gains `hold: "step"`** and nothing else in that file moves.
  `"step"` is the honest value: the fixture's view is `aView()` at `PREFLOP`, whose seats carry no
  hole cards.

- **Two merged assertions in `duel-state.test.ts` gain `hold: "step"` on every step**, and this is
  the whole of what moves in them:

  - `:1471`, *lays out one step per StreetDealt and a final step for the whole snapshot* — four step
    objects;
  - `:1488`, *a snapshot at COMPLETE with no events before it takes one step, not four* — one step
    object.

  Both are built from the file's local `sampleSeat`, whose `holeCards` defaults to `[]`, so
  `"step"` is what the predicate genuinely answers for them and **no assertion is weakened**: they
  now pin the value as well as the shape, and the second `verify:` mutation proves it by reddening
  both. Nothing else in either test changes — not the boards, not the streets, not the ordering, and
  no other test in the file is edited.

- **Three tests are added**, `ADR-0136` §§7.1–7.3, in the existing `describe("the duel state")`, with
  these names exactly — the `verify:` block greps them:

  1. `the same hand-ending view answers read for one seat and step for the other`
  2. `a fold's ending is a step, because no rival card came with it`
  3. `only the last step of a runout to a showdown is a read`

## Out of scope

- **Anything that reads the new field.** `duel-store.ts` still schedules every beat at `stepMillis`
  after this merges; that is `TASK-141105`. Nothing here changes a duration, and no test here
  asserts one.
- **`REVEAL_READ_MS`, `readMillis` and `boot.ts`** — `TASK-141106`.
- **Every component.** `DuelTable`'s `revealStep` prop keeps its declared `{ board; street } | null`
  and gains nothing: `Lobby.tsx:338` passes a **variable**, so no excess-property check reaches it,
  and `DuelTable.test.tsx`'s literal props stay valid. `Lobby.tsx`, `DuelTable.tsx`, `PotStrip.tsx`
  and `no-derivation.test.tsx` are not opened. Measured: the whole client suite is green with only
  the three files above changed.
- **Every file under `web-client/src/e2e/`.** A `verify:` gate requires `git status --porcelain` to
  report nothing there, and the suites are required to stay at **55 passed (55)**.
- **The wire.** `PlayerView.viewerSeat` and `SeatView.holeCards` both already ship on every
  `Snapshot` and are already filtered per recipient by the engine's projection layer. No declaration
  changes, `PROTOCOL_VERSION` does not move, `protocol.gen.ts` is not regenerated, and nothing in
  `poker-server` or `poker-engine` is opened.

## Tests

`duel-state.test.ts` — three added, two merged assertions moved. Baseline measured on `develop` at
`c54b4a1f`: **89**. After: **92**.

| Test | Proves |
| --- | --- |
| `the same hand-ending view answers read for one seat and step for the other` | `ADR-0136` §7.1: **one** seats array where seat 1 carries `holeCards` and seat 0 does not, laid out twice at `viewerSeat: 0` and `viewerSeat: 1`, giving `"read"` and `"step"`. **Two inputs that disagree**, or the test cannot tell a predicate from a constant — and the two answers come from the *same* hand, which is `ADR-0120` §3's own claim |
| `a fold's ending is a step, because no rival card came with it` | §7.2: the rival seat `hasFolded: true` with empty `holeCards`, so nothing passes merely because the hand is over. The viewer's **own** hole cards are populated, so the test cannot pass because the frame carried no cards at all |
| `only the last step of a runout to a showdown is a read` | §7.3: three `StreetDealt` then a showdown snapshot; the `hold` sequence is `["step", "step", "step", "read"]`, asserted as a sequence so a wrong position is a failure |
| `lays out one step per StreetDealt and a final step for the whole snapshot` *(moved)* | the four step objects now pin `hold: "step"` as well as board and street |
| `a snapshot at COMPLETE with no events before it takes one step, not four` *(moved)* | the one step object does the same |

**Both mutations were run against a prototype in this worktree and reverted; the numbers below are
measurements.**

| Mutation | Result required |
| --- | --- |
| `hold: rivalShown ? "read" : "step"` → `hold: "step"` | `Tests  2 failed \| 90 passed (92)` — the two-seat test and the runout test, and **only** those two |
| `hold: rivalShown ? "read" : "step"` → `hold: "read"` | `Tests  4 failed \| 88 passed (92)` — the two-seat test, the fold test, **and both moved merged assertions**, which is the evidence that moving them added an assertion rather than relaxing one |

## Acceptance criteria

- [ ] `RevealStep` declares `readonly hold: "step" | "read"`, and `layOutReveal` is the only place
      that sets it
- [ ] `duel-state.test.ts` reports `Tests  92 passed (92)`
- [ ] `src/routing/room-standing.test.ts` passes with its one literal carrying `hold: "step"` and
      nothing else changed in the file
- [ ] `npx vitest run src/e2e` reports `Tests  55 passed (55)` and
      `git status --porcelain web-client/src/e2e` is empty
- [ ] With the predicate replaced by the constant `"step"`, exactly
      `Tests  2 failed | 90 passed (92)`, the two failures being
      `the same hand-ending view answers read for one seat and step for the other` and
      `only the last step of a runout to a showdown is a read`
- [ ] With the predicate replaced by the constant `"read"`, exactly
      `Tests  4 failed | 88 passed (92)`, including
      `lays out one step per StreetDealt and a final step for the whole snapshot`
- [ ] Both mutations are restored and each restore is checked with `cmp -s`
- [ ] `advanceReveal` is byte-unchanged, and neither `layOutReveal`'s nor `applyServerMessage`'s
      signature changes
- [ ] `cd web-client && npm run check` exits 0
- [ ] The diff touches exactly the three files in the table above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
