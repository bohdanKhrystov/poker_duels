---
schema: 2
id: TASK-140101
title: The bank of the seat the clock does not name is read in whole seconds
type: task
status: done
parent: STORY-1401
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, table, bug]
depends_on: []
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/turn-clock.test.ts > "${TMPDIR:-/tmp}/turn-clock-after.txt" 2>&1; grep -qF 'Tests  17 passed (17)' "${TMPDIR:-/tmp}/turn-clock-after.txt"
  - cd web-client && cp src/table/turn-clock.ts "${TMPDIR:-/tmp}/turn-clock.fixed.ts" && perl -0pi -e 's/Math\.ceil\((clock\.bankRemainingMillis\[seat\] \/ 1000)\)/$1/' src/table/turn-clock.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/turn-clock.test.ts > "${TMPDIR:-/tmp}/turn-clock-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/turn-clock.fixed.ts" src/table/turn-clock.ts; cmp -s "${TMPDIR:-/tmp}/turn-clock.fixed.ts" src/table/turn-clock.ts && grep -qF "expected '1:58.623999999999995' to be '1:59'" "${TMPDIR:-/tmp}/turn-clock-before.txt" && grep -qF 'Tests  2 failed | 15 passed (17)' "${TMPDIR:-/tmp}/turn-clock-before.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The plate of the seat the turn clock does **not** name draws its timebank as `m:ss` — the photographed
`Timebank 1:58.623999999999995` becomes `Timebank 1:59` — because that call site hands `bankFigure`
the whole seconds its KDoc has always asked for.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/turn-clock.ts` | modify |
| `web-client/src/table/turn-clock.test.ts` | modify |

Read [`STORY-1401`](../stories/STORY-1401-the-timebank-figure-is-whole-seconds-at-every-seat.md)'s
*Design notes* and `web-client/src/table/countdown.ts` (13 lines — it is why `Math.ceil` and not
`Math.floor`). **Nothing else is opened**, and no other file in the repository is changed.

## Scope

- **One expression, at one call site.** In `seatClock`'s `seat !== clock.seat` branch
  (`turn-clock.ts:105`), the argument becomes `Math.ceil(clock.bankRemainingMillis[seat] / 1000)`.
  Write that expression **verbatim**, on one line — the third `verify:` gate re-introduces the defect
  by matching exactly this text, and a differently spelled equivalent makes that gate red. The line
  after the change is:

  ```ts
      bank: bankFigure(Math.ceil(clock.bankRemainingMillis[seat] / 1000)),
  ```

- **`Math.ceil`, not `Math.floor`.** `secondsRemaining` ceils *"so that the last whole second is
  shown for the whole of it"* (`countdown.ts`), and `bankFigure`'s own KDoc says the bank is drawn
  `m:ss` so a player can tell *"a spent clock (which reads as `0`) from an exhausted bank (which
  reads as `0:00`)"*. Flooring would print `0:00` — the shipped way to read an **exhausted** bank —
  for a bank still holding 999 ms. Ceiling also makes the two `bankFigure` call sites agree.
- **The repair is at the call site, not inside `bankFigure`.** `bankFigure` is correct for its
  documented input: it is a formatter shared with the seat the clock *does* name, whose argument is
  already whole. Flooring or ceiling inside it would change a shared formatter's contract to cover
  for one caller, would leave the KDoc describing an input the function no longer requires, and
  would make a `bankFigure`-only test the natural place to prove the fix — where it cannot fail
  before the fix. `bankFigure` and `clockFigure` keep their signatures, their bodies, their KDoc and
  their copied-from-the-card comments untouched.
- **Add two tests to `turn-clock.test.ts`'s existing `describe("seatClock")` block**, exactly as the
  *Tests* section below specifies. Use the existing `aClock(...)` helper; add no new helper.
- Run `npm run format` before `npm run check` — prettier owns the wrapping of the new assertions.

## Out of scope

- **The other three call sites, which were measured and are already correct.** `bankFigure`'s second
  call site (`turn-clock.ts:111`) and both of `clockFigure`'s (`turn-clock.ts:119,126`) pass
  `secondsRemaining(...)`, which is `Math.ceil`ed and therefore whole. This ticket changes one
  argument; if you find a fourth site, it is a new ticket, not a widening of this one.
- **`bankFigure`, `clockFigure` and `secondsRemaining` themselves** — no signature, body, KDoc or
  comment in any of the three changes.
- **Any file outside the two in the table.** No existing assertion anywhere in `web-client/src` is
  deleted, weakened, rewritten or renamed: for a whole number of seconds `Math.ceil(ms / 1000)` is
  `ms / 1000`, and every merged fixture carrying `bankRemainingMillis` holds whole seconds, so every
  merged test renders exactly the string it renders today. If a merged assertion goes red, the change
  did something this ticket did not ask for — stop and report it rather than editing the assertion.
- **The action bar's `off` state** (`DEC-135`, `STORY-1406`), **the pot's chip pile** (`STORY-1413`)
  and **the phone fit** (`DEC-136`, `STORY-1405`), all of which are on the same screenshot.
- **A design card.** `design/components/seat-and-pot.html` already draws `Timebank 3:00`, `1:12` and
  `0:00`; the drawing is right and the client is what fails to transcribe it (`ADR-0091` §2 asks for
  a card only where a story puts a **new** surface in front of a player).

## Tests

Two new `it(...)` blocks inside the existing `describe("seatClock", ...)` in
`web-client/src/table/turn-clock.test.ts`. Both reproduce the photographed frame: the clock names
seat 0, and the assertion under test is read at **seat 1** — the plate of the seat the clock does not
name, which is where the human was looking at their own bank while the rival acted.

`turn-clock.test.ts` → `describe("turn-clock") > describe("seatClock")`

| Test | Proves |
| --- | --- |
| `writes a fractional bank as whole seconds at both seats` | With `aClock({ seat: 0, turnEndsAt: 30_000, expiresAt: 30_000 + 45_678, bankRemainingMillis: [45_678, 118_624] })` read at `nowMillis: 0`: `seatClock(reading, 1, 0, 1).bank` is `"1:59"` and `seatClock(reading, 0, 0, 1).bank` is `"0:46"`. Two **different** fractional values at the two seats, so a constant string and a hard-coded seat index both fail. The seat-1 assertion is the defect's own: it reads `"1:58.623999999999995"` today |
| `writes a bank of one millisecond as 0:01, not as an exhausted 0:00` | At the same unnamed seat, `bankRemainingMillis: [45_678, 1]` gives `"0:01"` and `bankRemainingMillis: [45_678, 0]` gives `"0:00"` — a live bank is never drawn as an exhausted one (`ADR-0108` §5), which is what `Math.floor` would break. Reads `"0:0.001"` today |

**Both tests must be red at `develop` and green after the change, and the `verify:` block proves it
rather than takes your word for it.** The third command copies the fixed source aside, deletes the
`Math.ceil(...)` wrapper from it, re-runs this one test file, restores the file byte-for-byte
(`cmp -s`), and then requires the captured output to contain both `expected '1:58.623999999999995'
to be '1:59'` and `Tests  2 failed | 15 passed (17)`. It goes red if the tests do not detect the
defect, if the mutation did not apply, and if the file was not restored. These are the strings the
planner measured on this exact fixture, not strings computed from one.

The second command pins the file's absolute test count at **17** — 15 merged plus these 2 — so a
collection error or a quietly renamed test cannot pass as a green run.

## Acceptance criteria

- [ ] `writes a fractional bank as whole seconds at both seats` passes, asserting `"1:59"` at seat 1
      and `"0:46"` at seat 0 from the fixture named above
- [ ] `writes a bank of one millisecond as 0:01, not as an exhausted 0:00` passes, asserting `"0:01"`
      for 1 ms and `"0:00"` for 0 ms at seat 1
- [ ] `web-client/src/table/turn-clock.test.ts` holds exactly 17 tests and all 17 pass
- [ ] With `Math.ceil(...)` removed from `turn-clock.ts:105`, those two tests fail and the other 15
      pass — asserted by the third `verify:` command, which restores the file afterwards
- [ ] The diff touches exactly two files, and in `turn-clock.ts` exactly one line
- [ ] `bankFigure`, `clockFigure` and `secondsRemaining` are byte-identical to `develop`
- [ ] Every command in `verify:` exits 0

The PR body records the strings the third command captured: `1:58.623999999999995` and `0:0.001`
before the change, `1:59` and `0:01` after — quoted from
`"${TMPDIR:-/tmp}/turn-clock-before.txt"`, not retyped from this ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
