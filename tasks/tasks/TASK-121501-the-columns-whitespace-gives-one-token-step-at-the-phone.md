---
schema: 2
id: TASK-121501
title: The column's whitespace gives one token step at the phone
type: task
status: done
parent: STORY-1215
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [manual-verify, design, R2, layout]
depends_on: [TASK-121402]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - ./design/check-drift.sh
  - awk 'index($0, "--wgap: clamp(var(--pd-space-2), calc((100cqi - 340px) / 12.5), var(--pd-space-5))") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "[--wgap:clamp(4px,calc((100cqi-340px)/12.5),16px)]") { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - awk 'index($0, "100cqi-220px") || index($0, "100cqi - 220px") { n++ } END { exit (n != 0) }' web-client/src/lobby/Lobby.tsx design/screens/duel-table.html
  - awk '/className=.*min-h-\[100dvh\]/ { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

At 390 × 664 the duel table's **true** document height is `≤ clientHeight` — not 0.09375 px under
it — because the column's `--wgap` steps down one token at the phone, and the card says the same.

## Why this is not a defect, and why it is ordinary backlog

[`ADR-0106`](../../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md)
§1 rules that a true excess **strictly under one CSS pixel is met**: today's 664.90625 against 664
is a fit, `TASK-121402` merged as scoped, and nothing here repairs a defect. §4 files this ticket
anyway, for two things it buys:

- **Headroom.** The column stands **0.09375 px** from the fence. Any fraction added anywhere in
  its five children — a line-height, a padding step, a font metric — tips a *met* reading into a
  filed `R2` defect without anyone touching layout, and no compile-time gate can see it coming.
  After this the clearance is **23.09375 px**.
- **The tolerance's operating cost.** At a true fit no round ever runs `ADR-0106` §5's second
  read: the integers read `≤` and stop.

So it carries **no due date** and belongs to no round's fix set — `EPIC-12` §Termination rule 1
keeps it out of every `A(N)` and `B(N)`, exactly as it kept `STORY-1214` out. That is why it sits
under a new non-round story rather than under `STORY-1214`, which is `done`, and why nothing about
`STORY-1213`'s round is recomputed.

## What was measured, and what the ADR guessed

`ADR-0106` §4 names *"the `--wgap` clamp's floor"* as the lever and says in the same sentence that
the mechanism is **the ticket's to measure, not the ADR's to guess**. Measured in headless Chrome
at 390 × 664, on the column's own declaration, the floor is **inert at the judged shape**:

| probe | reading |
| --- | --- |
| `calc(100cqi / 100)` on the column | **3.9px** — so `100cqi` is **390px**, the container's own width, not its content box, and the padding does not feed back into the unit |
| computed `padding-top` with the floor at `8px`, `4px`, `2px` and **`0px`** | **8px** every time |

`(390 − 220) / 21.25 = 8` exactly, so at 390 the ramp *is* the operative term and the floor never
binds. The floor is what gives below **347.5px** — a width `ADR-0103` §6 promises nothing about.
**The number that has to move is the ramp**, and it is one declaration in each of two files.

**The replacement, measured across widths** (same probe, the card's container shape and the
client's both):

| container width | today | `clamp(var(--pd-space-2), calc((100cqi - 340px) / 12.5), var(--pd-space-5))` |
| --- | --- | --- |
| 320 / 340 / 360 | 8px | **4px** — the floor now genuinely binds below 340 |
| **390** (the phone) | **8px** | **4px** |
| 420 / 480 | 9.41px / 12.24px | 6.4px / 11.2px |
| **540 / 560** (the column's cap) | 15.06px / **16px** | **16px / 16px** |

Both endpoints stay declared tokens (`--pd-space-2` = 4px, `--pd-space-5` = 16px), both constants
stay exact decimals like the card's others, the give stays a continuous function of the column's
own width with no breakpoint (`ADR-0103` §2), and **the laptop shape's whitespace does not move**:
16px at 560, as today.

**The arithmetic, reproduced rather than reasoned.** The column's five children sum to
**616.90625 px** (`TASK-121402` measured 664.90625 with 6 × 8 px of `--wgap` whitespace — top
padding, bottom padding and four gaps). A five-child column carrying that content sum, this
column's own CSS, `min-height: 100dvh` and the centre child at `flex: 1`, rendered at 390 × 664:

| | true `documentElement` height | `scrollHeight` / `clientHeight` |
| --- | --- | --- |
| today's clamp | **664.90625** | **665 / 664** |
| the clamp above | **664** | **664 / 664** |

The first row reproduces the running client's recorded reading to the digit, which is why the
model is quoted rather than assumed: 24 px comes off the whitespace, the natural height falls to
640.90625, `min-h-[100dvh]` holds the column at 664 and the centre block's `flex-1` absorbs the
rest. **That reproduction is a probe, not the product** — the acceptance criteria below are
measured on the running stack.

**The file set was measured too** (`ADR-0069`, `ADR-0070`). `21.25` appears in exactly **two**
files in the repository, the two below. With the change stubbed in both, the whole pull-request
gate set was run in full and separately — `./gradlew check -PrequireDocker=true` (**BUILD
SUCCESSFUL**, 2 416 tests over 217 result files, the Postgres/Testcontainers suites among them, no
suite skipped), `npm run check` (**117 files, 988 tests**), `npm run build`,
`python3 .github/scripts/lint_tickets.py`, both `unittest discover` suites (6 and 81 tests) and
`./design/check-drift.sh` — and **every one exited 0, naming no third path**. No test file
anywhere mentions `--wgap` or a `clamp(`, so no merged assertion moves and no test file is in this
budget. **The gate set cannot see layout**, so its silence bounds the compile-and-test blast
radius and nothing more.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `web-client/src/lobby/Lobby.tsx` | modify |

**The card is in the budget because it owns the number.** `ADR-0106` §4 says the lever is *"shared
by card and client"* and that a number the card owns moves on `ADR-0103` §4's composing path —
which this is: `--pd-space-2` is already declared in `design/tokens/tokens.css`, so this is
**composing from the settled vocabulary, not minting** (`ADR-0091` §3), an ordinary dispatched
change with the human's visual verdict free to trail (`ADR-0024` §3). Both halves land in one diff
because `ADR-0106` §4 says **one** ticket, and because a client that fits while the card still
draws 8 px leaves the card in arrears — the state `ADR-0103` §4 names as a card that *lies*.

## Scope

- `design/screens/duel-table.html:61` reads
  `--wgap: clamp(var(--pd-space-2), calc((100cqi - 340px) / 12.5), var(--pd-space-5));` — the same
  clamp form, the same two token endpoints one step apart, nothing else on the line.
- `web-client/src/lobby/Lobby.tsx:183`'s arbitrary property reads
  `[--wgap:clamp(4px,calc((100cqi-340px)/12.5),16px)]`, the client's literal transcription of it,
  and every other class on that element is byte-unchanged.
- The comment above each says **why the ramp moved and not the floor** — `ADR-0106` §4, and the
  measured fact that at 390 the floor is inert. It must **not** quote the retired `220px` / `21.25`
  constants: the third `awk` gate forbids them in these two files on purpose, because a dead number
  in a comment is the thing that drifts.

## Out of scope

- **Anything below `ADR-0103` §3.1 on the give list.** The rival's mini hand (§3.2), the hero's
  hole cards (§3.3) and the board (§3.4) are not touched, and `--bw`, `--miniw` and `--herow` keep
  every number they have. `ADR-0106` §4 is explicit: a sub-pixel never justifies advancing the give
  order.
- **The three non-integer heights in `web-client/src/table/DuelTable.tsx` and
  `web-client/src/table/ActionBar.tsx`.** They are where the 0.90625 px comes from, they are not a
  defect (`ADR-0106` §1), and rounding them is not whitespace. Not ticketed, and not to be opened
  here — neither file is in this budget.
- **If §3.1's whitespace does not yield the pixel on the running stack, stop.** Do not take the
  next thing on the list. Register the next free `DEC` — `DEC-113` — say what the measurement was,
  and leave the ticket blocked. `ADR-0106` §4 provides for exactly this outcome (*"the stop rule
  fires again and the next `DEC` says so"*), so it is a **legitimate ending for this ticket, not a
  failure**, and it is the same conduct `ADR-0103` §3 commanded of `TASK-121402`.
- **`min-h-[100dvh]`, the `max-w-[560px]` cap, the container-query context and `p-[var(--wgap)]`.**
  Still owed by `ADR-0103` §5; the fourth gate guards the first.
- **A breakpoint, a media query, a `sm:`/`md:` switch, a sticky action bar** — `ADR-0103` §6 and
  its rejected alternative B, unchanged.
- **Widening `ADR-0106` §2's fence, or filing a sub-pixel anything.** The fence does not scale and
  only the human may move it.
- **`DEC-103` and `DEC-104`.** Still open, still the product owner's, still not answered here.

## Tests

**No test can be written, and the reason is a merged rule.**
[`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
§2b — *"No pull request, `verify:` block or ticket waits on a QA case"* — so a `scripts/qa/`
measurement may not appear in `verify:`, and jsdom computes no layout, so the client runner cannot
see this geometry either. **The measurement below is an acceptance criterion with a named runner
and a named number, never a gate.** `TASK-121302` closed a criterion by omitting to run it;
`TASK-121402` did not repeat that, and neither does this.

| Gate | Proves | Today |
| --- | --- | --- |
| `npm run check`, `npm run build` | the diff typechecks, lints, is formatted, and leaves 117 files / 988 tests and the production build green | green — **they cannot fail on this geometry** |
| `./design/check-drift.sh` | the card still names only tokens the sheet declares, at the sheet's values — `--pd-space-2` included | green — a regression guard |
| the card's `--wgap` line is the new clamp, exactly once | the card moved, in the card's own token vocabulary | **red** |
| `Lobby.tsx`'s `[--wgap:…]` is the new clamp, exactly once | the client transcribes the same rule, so the two cannot disagree | **red** |
| `100cqi-220px` / `100cqi - 220px` appear **zero** times in the two files | the retired ramp is gone from both — not left behind, not quoted in a comment | **red** |
| `className` carrying `min-h-[100dvh]` appears exactly once in `Lobby.tsx` | `ADR-0103` §5's floor was not deleted to make the sum fit | green — a regression guard |

The three red gates were run against `develop` and **exited 1**, and against a stub carrying the
change and **exited 0**. They match by literal substring (`awk`'s `index`), so a reformatting that
changes the spacing fails them — which is intended: these two strings are the deliverable.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on the running stack, with
`node scripts/qa/drive.mjs <port> size <w> <h>` and `… eval`, at the **preflop decision beat**.
**Paste every reading into the PR body as text.** Record what the stack says; do **not** predict an
exact integer — `ADR-0106`'s own process lesson is that integer arithmetic on a rounding instrument
writes a cheque the geometry may decline to cash.

- [ ] At **390 × 664**, `document.documentElement.getBoundingClientRect().height` is **≤ 664** —
      the true number to beat is today's **664.90625**, and this is the criterion `ADR-0106` §4
      states
- [ ] At the same beat and shape, `document.documentElement.scrollHeight ≤ clientHeight` — today
      **665 / 664** — so `ADR-0106` §5's second read never runs
- [ ] At the same beat and shape, `getComputedStyle` of the `min-h-[100dvh]` column reports
      `padding-top` **4px** and `row-gap` **4px** — today both read 8px. This is what says the give
      landed, and that the ramp rather than the floor is what moved
- [ ] At **390 × 664**, `getBoundingClientRect().bottom` on the `Fold` button and on the `All in`
      button are both **≤ 664** — `TASK-121402`'s reading, re-checked because the column's height
      budget moved
- [ ] At **720 × 900** the same beat still reads `scrollHeight ≤ clientHeight` — today an exact
      **900 / 900** — and the column's `padding-top` still reads **16px**, unchanged by this ticket
- [ ] The hero's hole card still measures **≥** a board card and the rival's mini card **<** the
      hero's — `ADR-0103` §3's floor and ordering, untouched here and re-checked
- [ ] Every element on the table at 720 is on the table at 390, in the same order, same words
      (`ADR-0103` §2)
- [ ] `design/screens/duel-table.html` opened in a browser: the **phone** frame's `.viewport`
      reads `scrollHeight ≤ clientHeight` and the **laptop** frame's numbers are unchanged —
      the card fit at 664 / 664 before this and must still fit after it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
