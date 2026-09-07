---
schema: 2
id: TASK-141105
title: The store holds the read beat, and zero silences every beat
type: task
status: done
parent: STORY-1411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, store]
depends_on: [TASK-141104]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-store.test.ts > "${TMPDIR:-/tmp}/store-after.txt" 2>&1; grep -qF 'Tests  17 passed (17)' "${TMPDIR:-/tmp}/store-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt" && grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - cd web-client && cp src/store/duel-store.ts "${TMPDIR:-/tmp}/duel-store.fixed.ts" && perl -0pi -e 's/state\.reveal\?\.steps\[0\]\.hold === "read"\s*\?\s*readMillis\s*:\s*stepMillis/stepMillis/s' src/store/duel-store.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-store.test.ts > "${TMPDIR:-/tmp}/store-one-length.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-store.fixed.ts" src/store/duel-store.ts; cmp -s "${TMPDIR:-/tmp}/duel-store.fixed.ts" src/store/duel-store.ts && grep -qF 'FAIL  src/store/duel-store.test.ts > the duel store > waits a step for every beat but the last, and the read for its own length' "${TMPDIR:-/tmp}/store-one-length.txt" && grep -qF 'Tests  1 failed | 16 passed (17)' "${TMPDIR:-/tmp}/store-one-length.txt"
  - cd web-client && cp src/store/duel-store.ts "${TMPDIR:-/tmp}/duel-store.fixed2.ts" && perl -0pi -e 's/    if \(stepMillis === 0\) \{\n      tick\(\);\n      return;\n    \}\n//s' src/store/duel-store.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-store.test.ts > "${TMPDIR:-/tmp}/store-no-gate.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-no-gate.txt" 2>&1; cp "${TMPDIR:-/tmp}/duel-store.fixed2.ts" src/store/duel-store.ts; cmp -s "${TMPDIR:-/tmp}/duel-store.fixed2.ts" src/store/duel-store.ts && grep -qF 'FAIL  src/store/duel-store.test.ts > the duel store > schedules nothing at a step of zero, even where a read beat stands' "${TMPDIR:-/tmp}/store-no-gate.txt" && grep -qF 'Tests  2 failed | 15 passed (17)' "${TMPDIR:-/tmp}/store-no-gate.txt" && grep -qF 'Test Files  4 failed | 3 passed (7)' "${TMPDIR:-/tmp}/e2e-no-gate.txt" && grep -qF 'Tests  24 failed | 31 passed (55)' "${TMPDIR:-/tmp}/e2e-no-gate.txt"
  - sh -c '! grep -qF readMillis web-client/src/e2e/drive-duel.tsx'
  - sh -c 'test -z "$(git status --porcelain web-client/src/e2e)"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The store owns both millisecond figures: `readMillis`, absent meaning `stepMillis`, mapped in
`armTick` off the kind of the beat now standing — and **`stepMillis === 0` silences every beat**,
gated before the kind is ever read. A hand that turned a hand face up the viewer had not seen now
waits `readMillis` on its last beat; every other beat still waits `stepMillis`; and
`web-client/src/e2e/drive-duel.tsx`, which boots at `0`, is not edited and cannot tell the difference.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-store.ts` | modify |
| `web-client/src/store/duel-store.test.ts` | modify |

Read [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
§§2–3 and 7.4–7.5, and
[`ADR-0102`](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §4.
`RevealStep.hold` was added by [`TASK-141104`](TASK-141104-a-beat-carries-its-kind-and-one-frame-decides-it.md);
you are its first consumer. **No other file is opened** — not `boot.ts` (`TASK-141106`), not
`duel-state.ts`, and nothing under `web-client/src/e2e/`.

## Scope

- **`DuelStoreOptions` gains one optional member**, documented in `ADR-0102` §4's own terms — a
  production seam, not a test door — and saying that absent means `stepMillis`, so a beat is a step
  unless a caller has asked for a longer read, and that it is never reached at `stepMillis === 0`:

  ```ts
  readonly readMillis?: number;
  ```

- **One line beside the other defaults**, in `createDuelStore`:

  ```ts
  const readMillis = options.readMillis ?? stepMillis;
  ```

  `?? stepMillis` and not `?? 0`: it is what keeps all 113 merged call sites — including
  `duel-store.test.ts`'s `stepMillis: 250` cases — behaving exactly as they do today with no edit.

- **`armTick` maps the kind to a number, after the gate**, exactly as `ADR-0136` §2 writes it.
  Reproduce the expression as written, because a `verify:` gate mutates it by literal match:

  ```ts
  schedule(
    tick,
    state.reveal?.steps[0].hold === "read" ? readMillis : stepMillis,
  );
  ```

  A comment says the `why`: the beat now standing is the **head of the queue at the instant the tick
  is armed**, so nothing here remembers which step it is on and nothing is re-derived per tick.

- **The zero gate does not move and is not rewritten.** These four lines stay byte-for-byte where
  they are, above the `schedule` call:

  ```ts
  if (stepMillis === 0) {
    tick();
    return;
  }
  ```

  `ADR-0136` §3 widens `ADR-0102` §4's *zero means synchronous* from a step to **every beat**: *"A
  step of `0` is a reveal with no clock in it at all. Every beat, of every kind, releases in the turn
  that armed it, and `schedule` is never called for a reveal. `readMillis` is not consulted."* It is
  in the store rather than in `boot.ts` because `createDuelStore` is called at 113 sites and
  `bootDuelClient` at 11; a gate in boot would leave every direct construction to a convention nobody
  enforces. **Placing the map above the gate is the one way to break this ticket**, and the second
  `verify:` mutation is what proves the gate is load-bearing.

- **`armClockTick` is untouched.** The turn clock re-arms independently and keeps `tickMillis`;
  `ADR-0113` §6 already anchors a queued `TurnClock` to `arrivedAt`, and `ADR-0136` §8 accepts the
  cost by name. The queue gains no exemption.

- **Three tests are added**, `ADR-0136` §§7.4–7.5, in the existing `describe("the duel store")`, with
  these names exactly — the `verify:` block greps two of them:

  1. `waits a step for every beat but the last, and the read for its own length`
  2. `waits one step for an ending that turned nothing face up`
  3. `schedules nothing at a step of zero, even where a read beat stands`

  The first two need a `Schedule` double that **records `delayMillis` and runs the callback
  immediately**; write it once, beside the file's existing `manualSchedule` and `manualClockSchedule`
  helpers, in the same idiom and with the same kind of KDoc. Both then assert the recorded array.

## Out of scope

- **`REVEAL_READ_MS`, `BootOptions` and `boot.ts`** — `TASK-141106`. After this ticket, nothing in
  production passes `readMillis` at all, and that is correct: this ticket builds the seam and the
  next one names the number.
- **Every file under `web-client/src/e2e/`.** `drive-duel.tsx:229`'s `stepMillis: 0` already means
  what it has to mean, and it is deliberately **not** given a redundant `readMillis: 0` —
  `ADR-0136` §3: *"a redundant zero is a second statement that can drift out of agreement with the
  gate that makes it redundant."* Two gates hold this: `readMillis` appears **nowhere** in that file,
  and `git status --porcelain web-client/src/e2e` is empty — the second is the stronger of the two,
  because it fails on *any* byte moved anywhere under `src/e2e/`. `scripted-duel.gen.json` is
  byte-unchanged and no drive is re-recorded.
- **Every component and every DOM assertion.** `ADR-0136` §4 keeps the DOM **byte-identical** at
  600 ms and at 2,000 ms: no component gains a prop, no element gains an attribute or a
  `data-testid`, and `no-derivation.test.tsx` stays byte-unchanged. That is why this ticket's proof
  is the injected `schedule` and nothing else.
- **A call count as evidence.** `ADR-0136` §7.4, carried verbatim: *"assert the **`delayMillis`
  arguments** — `[600, 600, 600, 2000]` for that runout, `[600]` for a fold's ending. A call
  *count* is identical either way and proves nothing."* No test in this ticket asserts
  `toHaveBeenCalledTimes` for a reveal.
- **`advanceReveal` and `duel-state.ts`.** Byte-unchanged; the classification is `TASK-141104`'s and
  is merged.
- **The wire.** Nothing crosses it, `PROTOCOL_VERSION` does not move, and no Kotlin file is opened.

## Tests

`duel-store.test.ts`. Baseline measured on `develop` at `c54b4a1f`: **14**. After: **17**.

| Test | Proves |
| --- | --- |
| `waits a step for every beat but the last, and the read for its own length` | §7.4: at `stepMillis: 600, readMillis: 2000`, a three-`StreetDealt` runout to a showdown where the rival's seat carries `holeCards` records the `delayMillis` **arguments** `[600, 600, 600, 2000]` — the sequence, in order, not a count |
| `waits one step for an ending that turned nothing face up` | §7.4's second half: the rival `hasFolded: true` with empty `holeCards` records `[600]`. The hero's own `holeCards` are populated, so this cannot pass on a frame that carried no cards at all |
| `schedules nothing at a step of zero, even where a read beat stands` | §7.5: at `stepMillis: 0` with `readMillis: 2000` **explicitly non-zero**, so the test cannot pass for the wrong reason, a showdown ending calls `schedule` **not at all** and the queued next hand's `Snapshot` drains in the same turn — asserted as `reveal === null` **and** as the next hand's `handNumber` on screen. `tickMillis` defaults to `0`, so no clock tick competes for the spy |

**Both mutations were run against a prototype in this worktree and reverted; the numbers below are
measurements.**

| Mutation | Result required |
| --- | --- |
| the `hold === "read" ? readMillis : stepMillis` expression → `stepMillis` | `Tests  1 failed \| 16 passed (17)`, the failure being `waits a step for every beat but the last, and the read for its own length`. The fold test and the zero test stay green, which is what tells you the read length is the only thing they do not depend on |
| the four lines of the zero gate deleted | `Tests  2 failed \| 15 passed (17)` in this file — the new zero test **and** the merged `a step of zero releases in the same turn and schedules nothing` — **and** `src/e2e` falls from `7 passed (7)` / `55 passed (55)` to `Test Files  4 failed \| 3 passed (7)` and `Tests  24 failed \| 31 passed (55)`. This is `ADR-0136`'s sharpest edge made observable: without the gate, the next hand never reaches the screen inside the `act()` that delivered it, and the symptom would read like a flake |

## Acceptance criteria

- [ ] `DuelStoreOptions` declares `readonly readMillis?: number`, and `createDuelStore` defaults it
      to `stepMillis`
- [ ] `armTick` returns from the `stepMillis === 0` branch **before** reading any beat's kind, and
      those four lines are byte-unchanged
- [ ] `duel-store.test.ts` reports `Tests  17 passed (17)`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `Tests  55 passed (55)`
- [ ] With the kind-to-length expression replaced by `stepMillis`, exactly
      `Tests  1 failed | 16 passed (17)`
- [ ] With the zero gate deleted, exactly `Tests  2 failed | 15 passed (17)` here **and**
      `Test Files  4 failed | 3 passed (7)` / `Tests  24 failed | 31 passed (55)` under `src/e2e`
- [ ] Both mutations are restored and each restore is checked with `cmp -s`
- [ ] `readMillis` appears nowhere in `web-client/src/e2e/drive-duel.tsx`, and
      `git status --porcelain web-client/src/e2e` is empty
- [ ] No test in this file asserts a call **count** for a reveal
- [ ] `cd web-client && npm run check` exits 0
- [ ] The diff touches exactly the two files in the table above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
