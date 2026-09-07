---
schema: 2
id: TASK-141106
title: Boot names the read beat's length beside the step's
type: task
status: done
parent: STORY-1411
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, table, store]
depends_on: [TASK-141105]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/boot.test.ts > "${TMPDIR:-/tmp}/boot-after.txt" 2>&1; grep -qF 'Tests  28 passed (28)' "${TMPDIR:-/tmp}/boot-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt" && grep -qF 'Tests  55 passed (55)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - cd web-client && awk 'index($0, "export const REVEAL_READ_MS = 2000;") { n++ } END { exit (n != 1) }' src/store/boot.ts
  - cd web-client && awk 'index($0, "export const REVEAL_STEP_MS = 600;") { n++ } END { exit (n != 1) }' src/store/boot.ts
  - cd web-client && cp src/store/boot.ts "${TMPDIR:-/tmp}/boot.fixed.ts" && perl -0pi -e 's/    readMillis\s*:\s*options\.readMillis \?\? REVEAL_READ_MS,\n//s' src/store/boot.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/boot.test.ts > "${TMPDIR:-/tmp}/boot-no-pass.txt" 2>&1; cp "${TMPDIR:-/tmp}/boot.fixed.ts" src/store/boot.ts; cmp -s "${TMPDIR:-/tmp}/boot.fixed.ts" src/store/boot.ts && grep -qF 'FAIL  src/store/boot.test.ts > booting the duel client > holds a read beat at REVEAL_READ_MS when boot was told nothing' "${TMPDIR:-/tmp}/boot-no-pass.txt" && grep -qF 'FAIL  src/store/boot.test.ts > booting the duel client > holds a read beat at the length boot was told' "${TMPDIR:-/tmp}/boot-no-pass.txt" && grep -qF 'Tests  2 failed | 26 passed (28)' "${TMPDIR:-/tmp}/boot-no-pass.txt"
  - sh -c '! grep -qF readMillis web-client/src/e2e/drive-duel.tsx'
  - sh -c 'test -z "$(git status --porcelain web-client/src/e2e)"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`REVEAL_READ_MS = 2000` is named once, in `boot.ts`, beside `REVEAL_STEP_MS = 600`, and
`bootDuelClient` passes it to the store. The last beat of a hand that turned a hand face up the
viewer had not seen now stands two seconds in a real browser, and moving that number is one line
that changes no interface.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify |
| `web-client/src/store/boot.test.ts` | modify |

Read [`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
§2 and
[`ADR-0102`](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §4, whose
*a step is 600 ms, named once, at the boot seam* is **extended here, not amended**: a second number
joins it at the same seam and `REVEAL_STEP_MS` keeps its name and its value. `readMillis` reached
`DuelStoreOptions` in [`TASK-141105`](TASK-141105-the-store-holds-the-read-beat-and-zero-silences-every-beat.md).
**No other file is opened.**

## Scope

- **The constant, beside its sibling**, with a KDoc in `REVEAL_STEP_MS`'s own voice naming
  `ADR-0120` §3 as where 2,000 comes from and saying it is a production seam:

  ```ts
  export const REVEAL_READ_MS = 2000;
  ```

  `REVEAL_STEP_MS = 600` is not touched. Gates pin both declarations at exactly one occurrence each.

- **`BootOptions` gains `readonly readMillis?: number`**, documented as `REVEAL_STEP_MS`'s sibling
  and as a production seam rather than a test door — the same words `stepMillis` and `tickMillis`
  are already documented with — and noting that it is never reached at `stepMillis: 0`, which
  silences every beat.

- **`bootDuelClient` passes it down**, one line beside the two already there:

  ```ts
  readMillis: options.readMillis ?? REVEAL_READ_MS,
  ```

  Write it exactly so, because the `verify:` mutation deletes it by literal match.

- **Two tests are added**, with these names exactly — the `verify:` block greps both:

  1. `holds a read beat at REVEAL_READ_MS when boot was told nothing`
  2. `holds a read beat at the length boot was told`

  `bootDuelClient` builds its own `schedule` from `setTimeout`, so **`setTimeout` is the only seam a
  duration is observable through**: `vi.useFakeTimers()`, `vi.spyOn(globalThis, "setTimeout")`, boot
  over the file's existing `FakeSocket` and `inMemoryStorage()` helpers, deliver one hand-ending
  `Snapshot` where the **rival's** seat carries `holeCards`, and read the `delayMillis` argument off
  `spy.mock.calls`. Restore the timers and the spy in a `finally`, so a failure cannot leak fake
  timers into the twenty-six merged tests in this file. The first test asserts `[REVEAL_READ_MS]` —
  **the imported constant, not the literal 2000**, so the assertion follows the number if the product
  owner moves it; the second passes a different figure and asserts that one.

  The frame is a `Snapshot` with no preceding `Events`, so its reveal is a single step and exactly
  one `setTimeout` is armed; the store's `tickMillis` clock does not fire because no `TurnClock` has
  arrived.

## Out of scope

- **Every file under `web-client/src/e2e/`.** `drive-duel.tsx` is not edited and gains no
  `readMillis` — `TASK-141105`'s gate is repeated here because this is the ticket that first makes a
  non-zero read length reachable from `bootDuelClient`, which is exactly the shape `ADR-0136` §3
  warns about. Gates: `readMillis` appears nowhere in that file, and
  `git status --porcelain web-client/src/e2e` is empty.
- **`main.tsx` and every other `bootDuelClient` call site.** `readMillis` is optional and absent
  everywhere; production gets `REVEAL_READ_MS` from the `??`. Nothing calls boot differently after
  this.
- **`REVEAL_STEP_MS`'s value.** `ADR-0136` extends `ADR-0102` §4 rather than amending it: 600 stays
  600, and a runout's street steps are not slowed. `ADR-0120`'s *Alternatives considered* rejected
  raising it uniformly, because a preflop all-in would become **eight seconds** of runout.
- **`duel-store.ts` and `duel-state.ts`** — merged by the two tickets before this one, and
  byte-unchanged here.
- **The 2,000 itself.** `ADR-0120` §3 calls it *a feel number* and the cheapest thing in that ADR to
  be wrong about; this ticket's whole job is to make moving it one line. Nobody is asked to justify
  it here.
- **The arithmetic `ADR-0136` §8 names and does not register:** a showdown reached from preflop takes
  **3.8 s** (`600 × 3 + 2000`, the last beat *is* the held one), and the deciding hand's result
  screen arrives 2 s later because `ADR-0102` §1's queue is FIFO for `DuelFinished` too. Both follow
  from the rule as written; neither is changed, carved out or tested here.

## Tests

`boot.test.ts`. Baseline measured on `develop` at `c54b4a1f`: **26**. After: **28**.

| Test | Proves |
| --- | --- |
| `holds a read beat at REVEAL_READ_MS when boot was told nothing` | the production default reaches the store: the `delayMillis` the boot's own schedule hands `setTimeout` is `REVEAL_READ_MS`, read as the imported constant |
| `holds a read beat at the length boot was told` | the option is honoured rather than the constant being reached by another route — **two inputs**, or one fixture default could not tell a pass-through from a hard-coded 2,000 |

**The mutation was run against a prototype in this worktree and reverted; the number below is a
measurement.**

| Mutation | Result required |
| --- | --- |
| the `readMillis: options.readMillis ?? REVEAL_READ_MS,` line deleted | `Tests  2 failed \| 26 passed (28)` — **both** new tests, because with the line gone the store falls back to `stepMillis` and every beat is 600 ms. That both go red is the point: it is the pass-through they depend on, not the constant's existence |

## Acceptance criteria

- [ ] `boot.ts` declares `export const REVEAL_READ_MS = 2000;` exactly once and still declares
      `export const REVEAL_STEP_MS = 600;` exactly once
- [ ] `BootOptions` declares `readonly readMillis?: number` with KDoc naming it a production seam
- [ ] `bootDuelClient` passes `readMillis: options.readMillis ?? REVEAL_READ_MS` to
      `createDuelStore`
- [ ] `boot.test.ts` reports `Tests  28 passed (28)`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `Tests  55 passed (55)`
- [ ] With the pass-through line deleted, exactly `Tests  2 failed | 26 passed (28)`, both failures
      being the two tests added here; the file is restored and the restore is checked with `cmp -s`
- [ ] The first test asserts against the imported `REVEAL_READ_MS`, not against the literal `2000`
- [ ] Fake timers and the `setTimeout` spy are restored in a `finally`, and the file's twenty-six
      merged tests are unedited
- [ ] `readMillis` appears nowhere in `web-client/src/e2e/drive-duel.tsx`, and
      `git status --porcelain web-client/src/e2e` is empty
- [ ] `cd web-client && npm run check` exits 0
- [ ] The diff touches exactly the two files in the table above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
