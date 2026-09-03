---
schema: 2
id: TASK-130503
title: What the bar makes of what the player typed, in the server's own numbers
type: task
status: done
parent: STORY-1305
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, table, action-bar]
depends_on: [TASK-130502]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/typed-amount.test.ts 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/rejection-text.test.ts 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - sh -c 'grep -q "readTypedAmount" web-client/src/table/typed-amount.ts && ! grep -q "under the minimum" web-client/src/table/typed-amount.ts'
  - sh -c 'grep -q "readTypedAmount" web-client/src/table/typed-amount.ts && ! grep -q "over the maximum" web-client/src/table/typed-amount.ts'
  - sh -c 'grep -q "readTypedAmount" web-client/src/table/typed-amount.ts && ! grep -qE "Math\.(min|max)|clamp|parseInt|parseFloat" web-client/src/table/typed-amount.ts'
  - awk 'index($0, "rejectionText") { n++ } END { exit (n < 3) }' web-client/src/table/typed-amount.ts
  - awk 'index($0, "That is not an amount.") { n++ } END { exit (n != 1) }' web-client/src/table/typed-amount.ts
  - awk 'index($0, "That is not an amount.") { n++ } END { exit (n < 2) }' web-client/src/table/typed-amount.test.ts
  - awk 'index($0, "is under the minimum of") { n++ } END { exit (n < 3) }' web-client/src/table/typed-amount.test.ts
  - awk 'index($0, "is over the maximum of") { n++ } END { exit (n < 2) }' web-client/src/table/typed-amount.test.ts
  - awk 'index($0, "rejectionText") { n++ } END { exit (n < 2) }' web-client/src/table/typed-amount.test.ts
  - python3 .github/scripts/lint_tickets.py
---

## Goal

One pure function says whether what a player typed is a total this turn's `LegalActions` would
accept, and when it is not, says why in the sentences the server itself would have used.

## Why this is its own file, and why it is the correctness-critical one

`ADR-0111` §4 makes the client's check **a reading, never a rule**: the entry is sendable iff it
spells a whole number of chips in `[floor, allInTo]`, where `floor` is the bar's merged
`amountFloor` — `minRaiseTo` when the server allowed `RAISE`, `minBetTo` when it allowed `BET`.
Both bounds are literal fields of `YourTurn`, so the bar still *"works out no amount the server did
not send"*.

`ADR-0111` §Consequences names the sharp edge and says nothing in the type system polices it: **if
this reading is ever stricter than the server's rule, a legal amount is wrongly refused and no
server message will ever say so, because nothing is sent.** That is why the reading is a file of its
own with eight named tests over it, and why the review is `deep`: the failure mode here is a
*missing* case, which a test report cannot show you.

## The decisions this ticket makes, and the two bounds on them

`ADR-0111` §3 hands *"what counts as spelling a number"* to this ticket, inside two bounds: the field
may **read** the product's own printed chip format, because reading is not rewriting; and **when the
reading is in doubt it refuses as not an amount**, because a wrong refusal costs a retype while a
wrong reading costs chips. Applied:

- **Accepted:** a plain run of ASCII digits (`1200`), or the same digits in `formatChips`' own
  grouping (`1,200`, `13,400`). Leading and trailing whitespace is trimmed before reading — trimming
  is a reading of the same entry, not a rewrite of it, and the entry itself is never touched.
- **Refused as not an amount, because the reading is in doubt or there is nothing to read:** the
  empty field, whitespace alone, a sign (`-500`, `+500`), a decimal point, an inner space, grouping
  the table would not print (`1,20`, `12,3456`), exponent and hex spellings (`1e3`, `0x10`) and
  anything with a non-digit in it (`12abc`). Non-ASCII digits are refused for free: JavaScript's
  `\d` is `[0-9]`.
- **`parseInt`, `parseFloat` and `Number(entry)` are all refused by name** in the gates. Each reads
  a prefix, an exponent or a hex literal and would turn `1e3` into 1000 and `0x10` into 16 — a
  reading nobody typed. Read the shape first with a regular expression; only then convert.
- **`0` is a number**, so it takes the minimum sentence, not the not-an-amount one (`ADR-0111` §3,
  by name).

**The sentences are shared, not copied.** `rejection-text.ts` already holds the two merged
sentences; this module calls `rejectionText` with a locally built `Rejection`, so the two voices
cannot drift apart even in principle. The built value is an argument to a formatter — it is never
sent, never stored and never reaches a frame — and gates refuse the sentence templates appearing in
this file at all. `rejection-text.ts` is **not narrowed and not retired** (`ADR-0111` §4): it stays
load-bearing for `NotYourTurn`, `HandComplete`, a race, a finer server rule, a bug, and every client
that is not this one.

## What is already true, measured on `develop` 2026-09-03

- `rejection-text.ts` exports `rejectionText(rejection: Rejection): string`; its `AmountTooSmall`
  arm reads `${formatChips(attempted)} is under the minimum of ${formatChips(minimum)}.` and its
  `AmountTooLarge` arm `${formatChips(attempted)} is over the maximum of ${formatChips(maximum)}.`
- `AmountTooSmall` is `{ type, attempted, minimum }` and `AmountTooLarge` is
  `{ type, attempted, maximum }` — `web-client/src/protocol/protocol.gen.ts`.
- `rejection-text.test.ts` reports **6**. Nothing here opens it; the gate pins it.
- `formatChips` in `chips.ts` groups digits in threes and is deliberately not `toLocaleString`.
- No file named `typed-amount.*` exists.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/typed-amount.ts` | create |
| `web-client/src/table/typed-amount.test.ts` | create |
| `web-client/src/table/rejection-text.ts` | read |
| `web-client/src/table/chips.ts` | read |
| `docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md` | read |

## Scope

- **`typed-amount.ts` exports one type and one function**, with KDoc on both:

  ```ts
  export type TypedAmount =
    | { readonly kind: "amount"; readonly to: number }
    | { readonly kind: "refused"; readonly sentence: string };

  export function readTypedAmount(
    entry: string,
    floor: number,
    ceiling: number,
  ): TypedAmount;
  ```

  A discriminated union, so a caller cannot take the number without having handled the refusal.
- **The reading, in order:** trim; refuse as `That is not an amount.` unless the trimmed entry
  matches a plain digit run or the table's own grouping; convert; then compare — below `floor` is
  the minimum sentence with `floor` quoted, above `ceiling` is the maximum sentence with `ceiling`
  quoted, and anything else is `{ kind: "amount", to }`. Both comparisons are **inclusive at the
  bound**: exactly `floor` and exactly `ceiling` are sendable.
- **No arithmetic on the bounds.** The module never adds, subtracts, rounds, steps or clamps: the
  only numbers it can produce are the one the player typed and the two the server sent.
- **The KDoc says why the `Rejection` is built locally** — a formatting argument, never a frame —
  and cites `ADR-0111` §§2–4. It must **not quote** either bound sentence: gates refuse
  `is under the minimum` and `is over the maximum` anywhere in this file, prose included, so that
  the only place those words exist is `rejection-text.ts`.
- **The lower-bound gates on the test file are supplementary, not the guard.** What stops an
  assertion being deleted is the exact test count (`Tests  8 passed (8)`) plus the eight named
  acceptance criteria; the `n < k` greps only refuse a test that assembles its expected sentence
  from the module instead of writing the literal out.

## Out of scope

- **The field, the bar and any React.** `ActionBar.tsx` is `TASK-130504`; this module imports no
  component and no store type.
- **`LegalActions`.** The function takes two numbers, not a turn: `amountFloor` already exists in
  `ActionBar.tsx` and stays there, unchanged.
- **Editing `rejection-text.ts` or its test.** Not narrowed, not extended, not opened
  (`ADR-0111` §4). The gate pins that file's 6 tests.
- **Any act conversion.** `ADR-0111` §5: a typed `callTo` is not a `Call`, an amount at or above
  the stack is not an `AllIn`. This module returns a total or a sentence and never an action type.
- **A stepper, a step, or `DEC-102`.**

## Tests

`typed-amount.test.ts`, `describe("what the bar makes of what the player typed")` — **8** tests, so
the file reports `Tests  8 passed (8)`. Every expected sentence is written as a **literal string**,
never assembled from the module's own constants: the wording is the thing under test.

| Test | Proves |
| --- | --- |
| `reads a plain run of digits as that many chips` | `"1200"` → `{ kind: "amount", to: 1200 }`, `"3650"` → `3650`, and `"  1200  "` → `1200`. Three inputs, so neither the number nor the trim is a constant |
| `reads the grouping the table itself prints` | `"1,200"` → `1200` and `"13,400"` → `13400` — `formatChips`' own output reads back as the number it printed |
| `sends both ends of the interval and one total inside it` | against `floor 1200, ceiling 13400`: `"1200"`, `"13400"` and `"5000"` are all `kind: "amount"` with those exact totals; then repeated against a **second interval** `floor 800, ceiling 4000` with `"800"`, `"4000"`, `"2500"`. This is `ADR-0111` §Consequences' unpoliced direction driven directly: a `<`/`<=` slip at either end reddens here |
| `refuses a total under the floor, quoting this turn's own floor` | `"500"` against floor `1200` → `kind: "refused"`, `sentence` exactly `500 is under the minimum of 1,200.`; `"500"` against floor `800` → `500 is under the minimum of 800.`; and `"1199"` against floor `1200` → refused. Two floors, so the bound cannot be hard-coded |
| `refuses a total over the stack, quoting this turn's own ceiling` | `"20000"` against ceiling `13400` → `20,000 is over the maximum of 13,400.`; `"20000"` against ceiling `4000` → `20,000 is over the maximum of 4,000.`; and `"13401"` against ceiling `13400` → refused |
| `takes a plain zero as a number and quotes the minimum` | `"0"` → `0 is under the minimum of 1,200.`, and **not** `That is not an amount.` — asserted both ways, because this is the one case `ADR-0111` §3 calls out by name |
| `refuses everything that is not a plain or table-grouped run of digits` | every one of `""`, `"   "`, `"-500"`, `"+500"`, `"12abc"`, `"1.5"`, `"1 200"`, `"1,20"`, `"12,3456"`, `"1e3"`, `"0x10"` is `kind: "refused"` with the sentence exactly `That is not an amount.`. `"1e3"` and `"0x10"` are the two a `Number()`-based reading passes as 1000 and 16 |
| `says exactly what the server's own rejection says` | for two number pairs each, the under-floor sentence equals `rejectionText({ type: "AmountTooSmall", attempted, minimum })` and the over-ceiling sentence equals `rejectionText({ type: "AmountTooLarge", attempted, maximum })` — one voice, proved rather than promised |

## Acceptance criteria

- [ ] `src/table/typed-amount.test.ts` reports `Tests  8 passed (8)`
- [ ] `reads a plain run of digits as that many chips` passes
- [ ] `reads the grouping the table itself prints` passes
- [ ] `sends both ends of the interval and one total inside it` passes
- [ ] `refuses a total under the floor, quoting this turn's own floor` passes
- [ ] `refuses a total over the stack, quoting this turn's own ceiling` passes
- [ ] `takes a plain zero as a number and quotes the minimum` passes
- [ ] `refuses everything that is not a plain or table-grouped run of digits` passes
- [ ] `says exactly what the server's own rejection says` passes
- [ ] `src/table/rejection-text.test.ts` still reports `Tests  6 passed (6)`
- [ ] `typed-amount.ts` mentions `rejectionText` at least 3 times, contains
      `That is not an amount.` exactly once, and contains none of `is under the minimum`,
      `is over the maximum`, `Math.min`, `Math.max`, `clamp`, `parseInt` or `parseFloat`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
