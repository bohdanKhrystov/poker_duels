---
id: EPIC-12
title: Quality and defect repair — the cycle that tests, triages and stops
type: epic
status: ready
labels: [process, meta, qa]
---

## Goal

A repeatable cycle that finds defects, prioritises them, fixes them through the existing ticket
workflow, and **provably stops**.

Every gate in this repository today answers one question: *did this diff do what its ticket said?*
Nothing answers *does the product work?* `ADR-0088` measured that gap precisely and accepted it —
four things that fail green, discovered "at a release, not a pull request". This epic is what turns
"at a release" into a scheduled act with a named owner.

The cycle is five steps:

```
qa (scope)  →  qa-manager (triage)  →  build-epic (repair)  →  qa (retest)  →  qa (smoke)
     ↑                                                                             │
     └────────────────────── only if the manager says PROCEED ─────────────────────┘
```

## Why now

`EPIC-01` through `EPIC-06` are closed. 915 of 919 tickets are `done`. The product runs: a duel was
played end to end through two browser storage partitions on 2026-08-29, and the four gaps
`ADR-0088` named were measured rather than assumed. What is missing is not features — it is any
standing answer to *"is it still working?"* between now and whenever a human next looks.

The human's instruction on 2026-08-29 was explicit about the failure mode to design against:

> This cycle should increase product quality but also we do not want to run infinitely or get
> stuck (each time report more and more bugs). So the manager in the middle should prevent such
> a scenario.

That sentence is the epic's hardest requirement, and §Termination below is the answer to it.

## Scope

- A `qa` agent that takes a **scope** — `epic <ID>`, `smoke`, or `regression` — brings the stack
  up, executes the test catalogue for that scope, and reports findings. It fixes nothing and files
  nothing.
- A `qa-manager` agent that takes one QA report plus the round ledger, dedupes it against what is
  already known, assigns severity, and decides what enters this round's fix set — bounded by
  §Termination's budgets. It is the only thing that writes bug tickets.
- A `qa-cycle` skill that runs the loop and enforces the stopping rules.
- A **test catalogue** — the core suite for the v0.1 spine and a smoke suite, plus a per-epic
  template filled in as each epic is tested.
- **Stack lifecycle** that uses no denied verb: `docker-compose` for the database, CDP
  `Browser.close` for browsers, harness task-stop for the server and dev server. `kill`, `pkill`
  and `killall` are denied in `settings.json` and deny beats allow, so no local override can
  reach them. This is a constraint on the design, not a wish.

## Out of scope

The first three are not this epic's self-restraint. They are the **standing conditions**
[`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
§2 attaches to the permission itself: any one of them failing withdraws the licence and returns the
question as a new `DEC`.

- **Any change to CI, and any second caller.** `build.yml` keeps its two jobs. A cycle is started by
  **the human's own message and nothing else** — not a pull request, not a merge, not a cron, not a
  hook, and not another skill invoking it as a step, whatever started that skill
  ([`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  §2b, heading and sentence amended by
  [`ADR-0090`](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §1).
  **Writing catalogue cases and running them are two commands.** Authoring is licensed as its own
  skill, `qa-cases`, which lands cases through ordinary reviewed PRs and whose terminal act is a
  report naming the command the human types next — it runs no browser and invokes no cycle
  (`ADR-0090` §3).
- **Any browser dependency in `web-client/package.json`**, or in any other module's dependency set.
  `ADR-0088` §1 forbids it by name and `ADR-0089` leaves that sentence byte-unchanged — the harness
  drives Chrome over the DevTools protocol using only Node built-ins, and no dependency list is
  touched (`ADR-0089` §2a).
- **Any claim of coverage.** A round's output is a dated record. Neither it nor `docs/test-plan.md`
  may be cited in an epic's `Metrics`, a Definition of done or a ticket's `verify:` (`ADR-0089` §2c).
- **Writing application state to reach a screen.** The driver reads anything and writes nothing but
  `pd.roomCode`; a case that seeds the store, a socket frame or a row is a client asserting a game
  fact, which `ADR-0002` forbids (`ADR-0089` §3).
- Fixing the defects. Repair runs as `build-epic` over ordinary tickets, which is the whole reason
  bugs are filed as `task`s: the linter knows `epic`/`story`/`task` and nothing else, so a bug that
  is a task needs **no change to a merged gate** and `build-epic` runs it unmodified.
- Performance, load and security testing.

## Open decisions

| ID | Question | Whose | Blocks |
| --- | --- | --- | --- |
| `DEC-102` | What does one press of the sizing row's stepper (`+`/`−`) move the dialled total by, and therefore which legal totals can the bar reach at all? | the **product owner's** | nothing in this epic — explicitly **not** the rewritten `TASK-120908` |

`DEC-102` was registered open on 2026-08-31 by
[`ADR-0101`](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
§6, the ADR that answered `DEC-101`. `ADR-0100`'s alternatives had filed the sizing stepper's step
size under `DEC-101` — *"the step size is undecided and belongs with `DEC-101`'s control design"* —
and `ADR-0101` §6 declines it: the question registered was what the **named presets** compute, a
stepper is not a named preset, and neither `docs/vision.md` nor `docs/duel-rules.md` names a step,
so an answer would be invented rather than derived. **Three things follow for this epic.** (1) It
**blocks nothing here**, and in particular not the rewritten `TASK-120908`: the chips' arithmetic,
their offer rule and `ADR-0100` §1's driver all stand without it, and the merged
`whole-duel.test.tsx` gate needs only the `min` and `all-in` chips. (2) It was raised by an ADR, not
promoted at a triage, so it is **not** one of `ADR-0092` §5's three slots and no cap is touched.
(3) The constraint whoever answers it must weigh is written into the row: `docs/duel-rules.md`
§*Betting* says **No-limit**, the shipped slider reaches every legal total and five chips reach
five, so whether the bar must keep a way to reach any legal total — and therefore whether the
stepper may ship after the chips or must ship with them — is part of that question and not of
`ADR-0101`'s.

`DEC-105` — **the architect's**, raised 2026-09-01 by `TASK-121301` before it wrote a line of
production code, exactly as
[`ADR-0096`](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §2
routes a beat's pacing — was answered the same day by
[`ADR-0102`](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md): **a
hand ends in steps, and the client owns the clock** — see the table below. It was the only decision
this epic has carried that **blocked a member of a round's fix set**, and it blocked it entirely:
`TASK-121301` is now unblocked and re-cut whole, `R1`'s repair can start, and the fix set's order
(`ADR-0096` §5 — top of the rubric first) is preserved rather than skipped over. It was raised by a
ticket, not promoted at a triage, so it is **not** one of `ADR-0092` §5's three slots and no cap is
touched. Nothing returned to the product owner: `DEC-103` and `DEC-104` stand exactly as round 1
routed them.

`DEC-098` — **the architect's**, raised 2026-08-31 by the planner splitting
[`STORY-1212`](../stories/STORY-1212-the-audit-focus-the-observer-the-resize-and-what-a-criterion-costs.md),
which routed rather than guessed — was answered the same day by
[`ADR-0099`](../../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md):
the audit rubric is `ADR-0096` §2 **itself**, grown only by an amending ADR, and **no working
document ever exists** — see the table below. Nothing in this epic moves: the observer and the
manager cite §2 by criterion id, which is what every `STORY-1212` ticket already gates, so no
ticket is rewritten and the first audit round runs exactly as planned.

`DEC-097`, the one this epic carried before it, was answered on 2026-08-31 by
[`ADR-0097`](../../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
— see the table below. Both of its halves landed with the architect and **neither returned anything
to the product owner**: `ADR-0096` §4 is buildable exactly as merged, so nothing about what an audit
round walks, judges against or terminates on has moved. **The first audit round is unblocked**, and
the planner now has what it was missing — a fifth declared agent file for the `Files` table, and a
verb that walks one beat at two shapes.

`DEC-082`, `DEC-083` and `DEC-085` are answered below. The first unblocked
this epic; the second settles how its catalogue gets written; the third adds the `uat` focus and its
arithmetic. (`DEC-086`, raised by `ADR-0092` and blocking nothing here, was answered on
2026-08-30 by
[`ADR-0093`](../../docs/adr/ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md):
the bar is two facts — the proofs of record load the built bundle, and recovery is completable
in the offered deployment — and readiness stays the human's judgment made by reading, so no
round this epic runs may be cited as either fact or as readiness. It raised `DEC-087` — the
architect's, the mechanism that serves the bundle to the proofs — which also blocks nothing
here; until it is answered, every round record honestly describes `npm run dev`.)

(`DEC-092`, raised by `TASK-120907` and gating only that ticket, was answered on 2026-08-30 by
[`ADR-0094`](../../docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md): the join path
ships as it is and its two cards are corrected to it. It was **not** one of `ADR-0092` §5's
three promoted slots — round 1 spent those on `DEC-089`–`DEC-091` — so the cap is untouched;
it was registered and answered in the same PR, and never appeared in an open table.)

(`DEC-096`, raised 2026-08-31 by the human after playing the product in two browsers, was
answered the same day by
[`ADR-0096`](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md):
a **product audit** is a third focus of this one cycle, its unit is the **beat**, and it judges
against a **frozen rubric**. Like `DEC-092` and `DEC-095` it was **not** one of `ADR-0092` §5's
three promoted slots — the cycle had already ended `PASS` — so no cap is touched; it was
registered and answered in the same PR and never appeared in an open table. It registered
`DEC-097`, answered the same day by
[`ADR-0097`](../../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md).)

(`DEC-095`, raised by `TASK-121101` and gating only that ticket, was answered on 2026-08-30 by
[`ADR-0095`](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md):
the table states who took the pot and never names a hand. Like `DEC-092` it was **not** one of
`ADR-0092` §5's three promoted slots — round 3 spent its one slot on `DEC-094` — so the cap is
untouched; it was registered and answered in the same PR, and never appeared in an open table.)

(`DEC-099`, raised 2026-08-31 by `TASK-121004` — the plain `<h1>Poker Duels</h1>` its third scope
item directed dressing lives in `App.tsx` above **every** screen, not on the front door, so the
dressing was a product change in costume, and the coder shipped the other two items (PR #1234)
and refused to guess this one — was answered the same day by
[`ADR-0098`](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md): **the
wordmark belongs to the front door alone**, read off the merged cards themselves — eleven card
files, one `.mark`, on `create-duel.html`'s front-door frame — so the `h1` leaves `App.tsx` and
no card moves. Like `DEC-092` and `DEC-095` it was **not** one of `ADR-0092` §5's three promoted
slots — raised by a ticket, not promoted at a triage — so no cap is touched; it was registered
and answered in the same PR, and never appeared in an open table.)

### Answered since this epic was written

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-106` | [ADR-0103](../../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) | **The table fits the phone, and the cards give before the numbers.** Registered and answered on 2026-09-01, raised by this epic's own round 1 (`STORY-1213`, `R2` `not met` at beats 2/3, 4, 5 and 6) and by `TASK-121302`, which asserted *"No `DEC` is needed here — this is conformance to a merged card"* and could not close it. **The premise was false and measuring it settled the question**: rendered headless at 390 × 664 the merged `design/screens/duel-table.html` is **732 against 664**, with `.bar` ending at 715.7, while at 720 × 900 it is 900/900 — the card was drawn for a wider screen, holds exactly one rule that narrows with the column (`--bw`, read by the board alone), and hardcodes the hero's hole cards and the rival's mini hand at `--w:96px` and `--w:40px`. A client conforming perfectly still fails `R2` by 68 px. **Four things follow for this epic.** (1) **`TASK-121302` is rewritten by the planner, not amended, and it is blocked** — two of its sentences are now false (the card is `read` in its *Files* table; *no `DEC` is needed*), and it gains a predecessor: a `module: design` ticket against `design/screens/duel-table.html`, which is amended so the two hardcoded widths narrow like `--bw` and gains a **second frame at 390 × 664 with the same markup**, boxed in height as well as width. **Design precedes client** — a coder cannot conform to a shape the card does not draw. Its height-budget half (`min-height: 100dvh`, `flex: 1`, one column where `Lobby.tsx` and `DuelTable.tsx` nest two) survives as necessary but not sufficient, and its file set is measured rather than copied. (2) **`R2` will be counted `not met` again if round 2 runs before both tickets land** — `ADR-0096` §5, *filing does not reduce `A(N)`, only repair does* — so `A(2)` does not fall for `R2` on filing alone, and the metric reads worse than the work is. That is the count doing its job, not a defect in it. (3) **One frame answers every beat**: the merged card reserves every slot, so the table's height does not vary by beat. The client's three different heights (885, 868, 866) are named as an input to the rewrite's measurement and decided nowhere. (4) **`DEC-103` and `DEC-104` stay open** and are untouched, though both were observed at phone width; `ADR-0103` §3 makes wrapping legal in general and leaves `DEC-103`'s question about a particular wrap alone. Named costs accepted: the player's own hand is smaller on a phone; the table acquires a **size budget**, so anything added to it later must buy space from something already there or not ship; two frames in one card drift with no gate; and the give order — whitespace, then the rival's face-down hand, then the hole cards (floor: never smaller than a board card), then the board — becomes a merged constraint on design taste |
| `DEC-105` | [ADR-0102](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) | **A hand ends in steps, and the client owns the clock.** Registered open 2026-09-01 by `TASK-121301` — round 1's `R1` repair, which routed before writing a diff exactly as `ADR-0096` §2 directs — and answered the same day by the architect, in both halves and with nothing returned to the product owner. **The pacing lives in `web-client`**: a `Snapshot` that ends a hand is painted as one step per preceding `StreetDealt` plus a final step carrying the whole snapshot and `ADR-0095`'s award line, and every frame arriving while steps remain is **queued in arrival order** and applied when the last step has stood. Two fields lag and no more — the board as a **prefix of the server's own list**, and that event's own street. **A step is 600 ms**, named once in `boot.ts`, and **`0` means synchronous**, so `drive-duel.tsx` boots at `0`. **A reconnecting client jumps to the end** because `resumeFrames` sends it no `Events` to pace against. **Five things follow for this epic.** (1) **`TASK-121301` is unblocked and re-cut whole** — one atomic `module: web-client` ticket (`ADR-0068` §3 as amended by `ADR-0069`/`ADR-0070`), `Files`, `Tests` and `verify:` all replaced, `verify:` gaining the client suite it could not carry while the mechanism was undecided, and the `manual-verify` label kept because the failure is a browser fact and `ADR-0089` §2b forbids a browser gate. Its `status:` flips from `blocked` with that re-cut. It is **decision-born in its final shape but not a new ticket**, so `B(N)` is untouched either way — the audit focus counts no `B(N)` at all (`ADR-0096` §5). (2) **`R1` cannot regress at the other seven beats**, which is the ticket's own second scope line: a `Snapshot` that does not end a hand is applied at once, with no timer and no queue, so ordinary play is byte-for-byte what it is today. (3) **`ADR-0100` §3's evidence survives** — the four recorded-frame e2e suites are not edited and no frame is re-recorded, because the step reaches the store as a parameter and the driver passes `0`. (4) **The round record was right on three points and incomplete on a fourth**: `act` calls `advance` in the same call and both hands' frames go out in one `deliver` (`DuelActionTest.afoldEndsTheHandAndOpensTheNext`), so board-slicing alone would have shipped and fixed nothing — and the same fact means `ADR-0095` §4's banner is today drawn and erased in one delivery, which the final step repairs as a side effect. (5) **`DEC-103` and `DEC-104` are untouched and still the product owner's**; §*Consequences* records that the reveal holds `DEC-104`'s `Pot 0` on screen for 1.8 s instead of a tick, introduces no new falsehood, and deliberately does not answer it. **No card moves, no ticket under `design/` is owed** — `ADR-0091` §3 is not engaged, because what was undecided was temporal and structural and a three-card board is already `duel-table.html`'s anatomy at one fill. Named costs accepted: a runout's stacks settle at the first step, so the outcome is legible from the seat plates 1.8 s early; every hand ends 600 ms later; the screen trails the server by design; and the two seats pace independently |
| `DEC-101` | [ADR-0101](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) | **`pot` means a pot-sized raise, and the fractions share its base.** Registered open hours earlier by `ADR-0100` §6, which settled the technical half and refused this one because the card's one worked example fits two formulas and excludes the 3,650 a poker player expects. The answer is one rule over one base: the base is the pot **as it will be after the call** — `ADR-0100` §6's `view.pot + both seats' committedThisStreet`, plus `callTo − the acting seat's committedThisStreet` — and the chips set `min → minRaiseTo`/`minBetTo`, `⅓ → callTo + floor(base / 3)`, `½ → callTo + floor(base / 2)`, `pot → callTo + base`, `all-in → allInTo`, rounding down. A fraction chip whose amount falls outside `[floor, allInTo]` is **absent**, never clamped and never greyed. **Four things follow for this epic.** (1) **The rewritten `TASK-120908` is unblocked** and keeps `ADR-0100` §7's six files; what moves inside them is that `Lobby.tsx` hands the bar one more published number (the acting seat's `committedThisStreet`), and its *each preset sets the amount its own name states* test needs at least one frame where the hero has **already committed** — at `committedThisStreet: 0` the call's cost and `callTo` are the same number, so a fixture there proves nothing about the term the whole disagreement lived in. (2) **A card moves, and it is `design/screens/duel-table.html`** — the hero frame's stepper and its `Raise to` button read `3,250` with the `pot` chip selected and must read **3,650**; `TASK-120908`'s *Out of scope* sentence *"the card is not in arrears here — no ADR merged after it"* was true when written and this is that ADR. Until the design ticket lands, a UAT round reading the card would file a correct client as a defect. (3) `ADR-0100` §3's *no frame is re-recorded* survives, because `min` and `all-in` are legal by construction and are the only chips the committed script needs. (4) It registered **`DEC-102`** — the stepper's step, above — which gates nothing here. Named cost accepted: the sizing row changes shape between turns, and preflop the button gets `min` and `½` at the same 300 with no `⅓` at all, so an ordinary 2.2–2.5bb open is reachable from no chip |
| `DEC-100` | [ADR-0100](../../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) | **The driver reaches an amount by pressing what a player presses.** Raised 2026-08-31 by `TASK-120908` — round 1's `medium` sizing-control finding — whose coder replaced the range input, got its three new tests and all 25 of `ActionBar.test.tsx` green, then hit **24 failures across four merged e2e files** outside its two-file budget and routed rather than widening scope. `drive-duel.tsx` now finds the action button by the recorded verb as it always has, **reads the total that button prints**, clicks if it matches, otherwise presses each sizing control in document order re-reading after each press, and throws by name when none matches — strictly more than `fireEvent.change` proved, and no test-only door, no driver-only slider, no re-recorded frame. **Four things follow for this epic.** (1) **`TASK-120908` is rewritten, not amended, and its file set is six** — `ActionBar.tsx`, `ActionBar.test.tsx`, `drive-duel.tsx`, `Lobby.tsx`, `bar-no-derivation.test.tsx`, `turn-fixture.ts` — with `whole-duel.test.tsx` named as the merged gate that forbids splitting it (`ADR-0068` §3); as written it cannot go green, because its three-button actions row breaks the one recorded `AllIn` frame independently of the amounts (`BettingRules.kt` offers `ALL_IN` with no `RAISE` to a short stack facing a bet, so a sizing chip can never stand in for it). (2) **No card is in arrears and none moves** — the ADR reads `duel-table.html`'s three-button `.actions` row as *one drawn state, not a law over all states*, which is the sentence a later round quotes when a card and `legalActions` disagree. (3) **The four e2e files are not in the ticket's file set**, and that is the check on the whole answer: a rewrite that finds itself opening one of them is weakening a proof and should stop. (4) It registered **`DEC-101`** — the product owner's, answered the same day by [ADR-0101](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) and struck from the table above — which gated the rewrite and nothing else. Named cost accepted here: no scripted duel can ever again exercise an **interior** bet amount, so a rounding fault correct at both boundaries and wrong between them is invisible to the whole-duel proof |
| `DEC-099` | [ADR-0098](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) | **The wordmark belongs to the front door alone.** Raised 2026-08-31 by `TASK-121004` (round 2's `medium` front-door ticket) and answered the same day by the product owner, reading the merged cards rather than adding a rule: eleven card files hold one `.mark` between them, on `create-duel.html`'s front-door frame, and the four secondary-screen cards this cycle itself filed were composed under the shipped global heading and drew no mark. **Three things follow for this epic.** (1) **`TASK-121004` does not close on PR #1234 alone** — its third scope item is struck and becomes a follow-up client ticket the planner writes over four named files: `App.tsx` (the unconditional `h1` leaves), `Lobby.tsx` (the pre-create branch gains the lockup; `CoinMark` exists), `App.test.tsx` (its four plain-title assertion sites — `renders the application heading`, `gives the heading a token-derived class`, and both repeats inside the record regression guard — cannot stand and are rewritten deliberately) and `Lobby.test.tsx` (gains the coin-plus-two-text-elements assertion; its zero-headings front-door test meets the lockup). The follow-up is **decision-born, so excluded from `B(N)`** (`ADR-0092` §5, the third exclusion) and enters the **ordinary backlog now the cycle has ended** (§Termination rule 1). (2) An answered question becomes a merged source, so a later round asking for a wordmark on any *other* screen — or filing its absence there as a finding — contradicts something merged; the front door remains the one screen where a missing or mangled lockup is a conformance finding. (3) **No design ticket**: no card is in arrears — the shipped client was — so nothing under `design/screens/` moves, and the named cost (the invited player `ADR-0094` seats straight into a dealt hand never reads the product's name on-page) is accepted in the ADR rather than patched by chrome |
| `DEC-098` | [ADR-0099](../../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md) | **The rubric is the ADR section, and a criterion is born merged.** Registered 2026-08-31 by the planner splitting `STORY-1212`; answered the same day by the architect, before any round cited a criterion. **The audit rubric is `ADR-0096` §2 itself, and no working document ever exists.** `ADR-0096` §3 already routes every proposed criterion to a `DEC` whose answer is a merged PR, so every future criterion arrives as merged ADR text whatever this decision said — a working document could only ever have held a **copy**, and a copy of the audit's law is a second register guarded by nothing: the shape `TASK-120705` refused one focus earlier and `ADR-0092` §8 priced as *two copies of a rule drift*. **Nothing in this epic moves**: the observer and the manager cite §2 by criterion id (`R1`…`R5`) — exactly what every `STORY-1212` ticket already gates — so no ticket is rewritten and the first audit round runs as planned; what hardens is that the tickets' refusals to transcribe stop being *pending `DEC-098`* and become merged law. Growth is **one amending ADR**: ids sequential and never reused (`R6` next), the criterion in §2's own three-column form, the resulting priority order restated **as ids only**, and `ADR-0096`'s index row annotated *rubric grown to N* in the same PR — which keeps §Metrics' *criteria added per invocation* countable from two dated registers (growth ADRs in the ADR index, invocations in the round stories) with no archaeology. A round record cites `R<n>` and the ADR section that states it, never a path, and the freeze is a **commit fact**: the rubric in force for an invocation is the chain as merged at the commit its first round names. Named costs: the rubric is never again one table once it grows; the index annotation is a hand-maintained discipline whose precedent lagged once (`ADR-0090`'s row, repaired in the answering PR); a sixth criterion is never cheaper than a full merged ADR — slow on purpose |
| `DEC-097` | [ADR-0097](../../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md) | **A resize is two numbers, and the audit's observer is the fifth declared file.** Registered 2026-08-31 by `ADR-0096` §1 and answered the same day, both halves, by the architect — **nothing returned to the product owner**, so `ADR-0096` §§1–3 and 5–6 are untouched and §4 stands as merged. **Four things change for this epic.** (1) **The audit focus gets its own observer**, `.claude/agents/audit.md`, and `ADR-0090` §2's declared-file set becomes **five** — licensed to *mention* `qa-cycle` in the one stack-lifecycle sentence `qa.md` and `uat.md` both carry, never to invoke it. It is amended because `ADR-0092` §8's own test is met: `ADR-0096` §2 froze `ADR-0092` §3 **byte-unchanged for `qa` and `uat`**, where an observation with no card, token or literal behind it is a **question** capped at three, while under the audit it is a **finding** against a rubric criterion — one file switched by a scope word is the leak §8 built two files to prevent, and round 3 promoting **zero** questions on a product the human called raw is the evidence it is not hypothetical. `ADR-0092` §8 otherwise applies unamended — one manager, one ledger, no new skill, no `Write` on the observer. The check gains `audit` and exits **0** today, **0** at five, **1** at six. (2) **`scripts/qa/drive.mjs` gains one verb**, `size <width> <height>`: `Emulation.setDeviceMetricsOverride` with **`width`, `height`, `deviceScaleFactor: 0`, `mobile: false` and no other field**, reading the viewport back from the page and **exiting 1 if it is not what was asked for**. A viewport resize is an **act** under `ADR-0089` §3 — the third of its three categories, beside *click, type, navigate, reload, clear storage* — not a read and not an application-state write, so §3 stands byte-unchanged. **The classification is a property of the fields**: measured, `mobile: true` widens the layout viewport 390 → 520 by mobile shrink-to-fit and turns an `R2` *not met* into a **false pass**, fabricates `screen` and `devicePixelRatio`, and buys none of `pointer: coarse`, `hover: none` or `maxTouchPoints`. `Browser.setWindowBounds` is rejected on measurement — Chrome clamps a window to 500 px wide (390 × 664 → a **500 × 577** viewport) and 87 px of chrome makes 720 × 900 → **720 × 813** — and §4's numbers are viewport numbers. (3) **§4's two shapes are two measurements of one live tab.** The override survives session detach and navigation and is per-target; across a resize the page's JS identity was byte-identical, one `resize` fired and a `min-height: 100dvh` column re-measured 664 → 900 — so the socket and the seat survive and `ADR-0018`'s re-seat is never approached. **Both tabs move together and the walk restores itself to `phone`**, keeping the shape the only variable. (4) **Portrait only** — the human's *"we are ok to support only one orientation for mobile form factor"*, recorded where `ADR-0096` §4 left the question: no rotation handling, no reflow decision, no second mobile shape, and `screenOrientation` is never sent. `ADR-0089` §§2a, 2b, 2c re-checked one at a time, with the corollary **a shape walked is not a surface supported**; the working copies — the verb, `audit.md`, the `audit` focus in `qa-cycle`'s `SKILL.md` and the fifth check entry — land through the planner's tickets. The costs are named: the frame recorder cannot tell a resize from a product transition, a **forgotten** restore has no catch, the field discipline fails toward a false pass with no CI job permitted to guard it, and device emulation is foreclosed permanently |
| `DEC-096` | [ADR-0096](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) | **The audit judges a whole duel against a frozen rubric, and no round may grow it.** Raised 2026-08-31 by the human after the `/qa-cycle uat regression` that ended `PASS` with `B(3) = 0` left a product they called *raw*. **Six things change for this epic.** (1) A third focus, `/qa-cycle audit <scope>` — same skill, **same manager, same ledger** (`ADR-0092` §§6, 8 applied, not re-argued), the human's own message and the first act of its turn. (2) Its unit is the **beat**, not the screen: eight beats from the opened link to the rematch, both browsers observed, at **two shapes** — the whole walk at `phone` **390 × 664** and `R2`/`R3` again at `laptop` **720 × 900**, on the human's *"we have to support phone size"*, with **one bar checked twice rather than two bars** stated in the round record — and beat 5 is **a hand that goes all-in and runs the board out**, which `docs/test-plan.md` has never walked in any case. (3) `ADR-0092` §3's classifier is amended **for this focus only and by relocation**: a finding contradicts a **criterion** in a merged, closed, five-line **rubric** and needs no other source; §3 stands byte-unchanged for `qa` and `uat`. (4) **Termination is the rubric.** `A(N)` counts *criteria* answered `not met`, so `A(N)` can never exceed the rubric's size, a ceiling known before the round starts; the rubric is **frozen for the invocation** — §Termination rule 1 one level up — and a proposed criterion routes exactly as `ADR-0092` §5 routes a question, reaching the list no earlier than the next invocation. `PASS` at `A(N) = 0` says **the list is satisfied**, never that the product is finished (`ADR-0089` §2c, `ADR-0093` §2); `STOP_DIVERGING` and `STOP_BUDGET` both say *still raw, and here is how*. (5) **No severity and no backlog under this focus**: rule 2 is scoped to `qa` and `uat` on the human's call, and a finding deferred by rule 3's eight-cap **stays counted** — repair by the rubric's own order, top to bottom. An audit round reports `A(N)` and no `B(N)`; a functional defect it stumbles on enters the next `qa` round's count. Where an unmet criterion's repair is already a `status: backlog` ticket, the manager **promotes it rather than filing a second**. (6) §Metrics gains **criteria added per invocation** — the honest proxy for *finished*, since the day the human plays and adds nothing is the day the list caught up with their eye. Registers `DEC-097` for the architect. Its one escalation — **is a phone a supported surface?** — the human answered the same day (*"we have to support phone size"*), so §4 walks **two shapes** and `DEC-097`(b) became the load-bearing half. **`docs/vision.md` is not amended**: *"She opens it in a browser"* and *"Two browsers, one room link"* name a browser and **no device**, so the call resolves a silence rather than contradicting a sentence, and §4 names what would change that. **Phone landscape was left open by §4 and is now answered** — the human's *"we are ok to support only one orientation for mobile form factor"*, recorded by [`ADR-0097`](../../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md) §5 |
| `DEC-095` | [ADR-0095](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) | **The table states who took the pot, and never names a hand.** Raised 2026-08-30 by `TASK-121101`, whose triage refused to guess it (`CLAUDE.md` rule 5) and which was unstartable until this landed. **Three things follow for this epic.** (1) `TASK-121101` becomes an ordinary client ticket the planner rewrites — `Scope`, `Tests` and `verify:` — and it can now carry a real gate instead of its `manual-verify` label, because every string it renders is fixed and every fact it reads is already on the wire. It is **excluded from `B(N)`** as a decision-born ticket (`ADR-0092` §5, the third exclusion) and it enters the **ordinary backlog now the cycle has ended**, never the round that asked (§Termination rule 1). (2) The `medium` conformance finding that round 3 filed against `design/screens/duel-table-states.html`'s two banner frames closes for good in the direction the card moves: both frames lose their second line, in a `module: design` ticket, and `ADR-0092` §5's *an answered question becomes a merged source* makes a later round re-raising it a contradiction of something merged. (3) Nothing in `docs/test-plan.md` moves — no `expect` column, no screen-table row, no path — and `CORE-06` and `CORE-09` describe the blessed product |
| `DEC-092` | [ADR-0094](../../docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md) | **Opening the invite is taking the seat, and the two join cards are corrected to it.** Raised 2026-08-30 by `TASK-120907`, which routed rather than guessed and was unstartable until this landed. The product owner blessed what shipped: the invite path renders **no screen** — presenting the code *is* taking the seat (`ADR-0022`) — and the room-code field stays on the first screen (`ADR-0060` §§1, 4). **Three things follow for this epic.** (1) The `high` conformance finding that `STORY-1205`, `STORY-1209` and `STORY-1210` each filed against `design/screens/join-duel.html` and `enter-code.html` closes for good — `ADR-0092` §5's *an answered question becomes a merged source*, so a fourth round re-raising it would contradict something merged and the suppression is mechanical. (2) `TASK-120907` is rewritten by the planner as a `module: design` ticket, not a client one; it enters the earliest **subsequent** triage or the ordinary backlog, never round 1's frozen set (§Termination rule 1), and it is **excluded from `B(N)`** as a decision-born ticket (`ADR-0092` §5, the third exclusion). (3) `docs/test-plan.md`'s screen table has two rows naming those card paths; they move with the cards in the same ticket, or a later round walks a table pointing at a file that is gone — which would be a **harness** defect against this epic, and is cheaper not to cause. Nothing in the catalogue's `expect` columns moves: `SMK-05`, `CORE-02` and `CORE-05` describe the blessed product |
| `DEC-085` | [ADR-0092](../../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) | **UAT is a second focus of this cycle, not a second cycle.** `/qa-cycle uat <scope>` — the human's own message, first act of its turn; the QA focus never chains into it, neither report prints the other's command, and a preceding QA cycle is practice, never a checked precondition (a check would cite a round as a gate, `ADR-0089` §2c). Raised 2026-08-30 by the human's request for a UAT pass — UX and design conformance over the same catalogue — with two halves settled by the human mid-decision: **a UX question raised during UAT is delegated to the `product-owner`**, whose decision may create a bug ticket through `build-epic`; and **UAT runs now, filing the six missing cards**, rather than waiting for `ADR-0091` §5's retrofit story. **Six things change for this epic.** (1) A `uat` agent joins `qa` as the observer under the second focus — no `Write`, same report shape plus a `QUESTIONS` section capped at three per screen; `qa.md` stands byte-unchanged. (2) `qa-manager` triages both focuses on one ledger — dedupe spans them — and applies the classifier: **a finding must contradict a merged source** (a card, a token, an owned literal, an ADR section, a vision sentence); a judgment with no merged source is a question, and `qa-manager` promotes at most **three** per round (one per screen, each a concrete choice answerable in one sentence and bearing on a player's ability to tell what is going on) as `DEC`s for the product owner, whose answers become merged sources either way; tickets born of answers enter the earliest **subsequent** triage, preserving §Termination rule 1's frozen set. (3) A missing card is a `high` finding whose repair **is** the card (composed per `ADR-0091` §3, the human's verdict trailing; dedupe key the card's own path); the screen is walked for reachability and copy, and only its conformance check reads `BLOCKED — no card`. (4) **`B(N)` gains its second and third exclusions**: missing cards (registered debt) and decision-born tickets (improvement), because counting either trips rule 4 on a cycle doing its job — `B(N)` counts product defects alone. (5) A round in which a screen becomes conformance-judgeable for the first time is a **baseline round** — rule 4 skips its comparison, exactly as it skips round 1's; rule 5's three-round budget binds regardless — and every UAT verdict line is qualified inline (`PASS (conformance unjudged on 6 of 7 screens)`), repeated verbatim in the terminal report. (6) `STOP_BLOCKED` is scoped: a human-only escalation ends the cycle only when it **gates the current fix set**; otherwise `notify.py blocked` carries it while the run is warm and the cycle continues. The catalogue is reused as a route map — existing `do` columns are the walk, `expect` stays functional — and `docs/test-plan.md`'s UAT section (screen inventory + question list) merges before the first UAT round. A `shot` verb (CDP screenshot, Node built-ins, never committed, no image-diff tooling ever) is the one harness addition. `ADR-0090` §2's declared set grows to four files (`agents/uat.md`, mention-only). `DEC-086` — the written bar for *"ready for real users"*, the product owner's — was registered open (since answered by [`ADR-0093`](../../docs/adr/ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md): two facts, and readiness stays the human's reading); no round may be cited as it |
| `DEC-083` | [ADR-0090](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) | **A skill may write the catalogue or run it, never both in one turn.** Raised and answered 2026-08-29, when the human asked for one skill that writes the missing cases and then runs a cycle over them. `ADR-0089` §2b's *"not another skill invoking it as a step"* is read as a rule about **composition**, not only about automation: *"a human's command"* is a condition only if it means the **immediate caller**, and the composite's sole value is that the cycle begins while the human is elsewhere — a cron whose clock is the length of its first half. **§2b's heading becomes *"No gate, and one caller"*** and its sentence becomes *a cycle is started by the human's own message and nothing else*; the rest of §2b and all of `ADR-0089` §§2a, 2c, 3, 4, 5, 6 stand byte-unchanged. **Three things change for this epic.** (1) The catalogue's missing suites are authored by **`qa-cases`**, a licensed skill that plans and lands cases through ordinary reviewed PRs, runs no browser, dispatches neither `qa` nor `qa-manager`, and whose **terminal act is a report naming the command the human types next**. (2) Every case `qa-cases` writes carries a `source` column citing the merged decision its `expect` is transcribed from — the module for player-facing text (`ADR-0089` §5), otherwise an ADR section or a `docs/duel-rules.md` heading; **a case with no merged source is not written**, and the gap is registered as a `DEC` for the product owner, because an invented expectation is a product claim that step 4 of the loop would change production code to satisfy. `SMOKE` and `CORE` are not retrofitted. (3) **A suite `qa-cases` writes is provisional until its first round** — merged sources prove what was *decided*, not what *shipped*, so they cannot show that a screen exists or that a literal has not moved. `docs/test-plan.md` §*Per-epic suites*' *"filled in when an epic is first tested, not before"* is amended in the same PR to admit the authored-then-tested path, a provisional suite carries a line the first round record deletes, and the honest expectation for that round is a pile of **harness** tickets against this epic — which, since `ADR-0089` §4 excludes them from `B(N)`, can end `PASS` with a dozen of its own cases found broken. (4) The condition is checkable by one command over a declared three-file set, and what no grep can catch is stated rather than claimed |
| `DEC-082` | [ADR-0089](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) | **A browser drives this client for a QA round, never for a gate.** The harness may live here. It **was** inside `ADR-0088` §1's words — *"here or in CI"* says *here* — so §1's **heading** is amended in the open and nothing else is: `EPIC-03` still ships no fifteenth story, §2's eleven-step hand-check is still the proof of record, `build.yml` keeps its two jobs, no browser dependency enters `web-client/package.json`, and `ADR-0032` §4's *"still jsdom, still no network"* still holds of every test suite. **Four things change for this epic.** (1) The three constraints in *Out of scope* are no longer this epic's self-restraint but **standing conditions on the permission** — no dependency, no gate, no coverage claim — and any one failing returns the question as a new `DEC`. (2) §3: the driver **reads anything and writes nothing** but `pd.roomCode`; no case may seed store, socket or database state to reach a screen, because that is a client asserting a game fact (`ADR-0002`). (3) §4 adds the rule §Termination lacked — a failing case that does **not reproduce by hand** is a **harness** defect, filed against this epic, **excluded from `B(N)`**, and never repaired in production code. (4) §5: a case quoting player-facing text cites the module that owns the literal. A `PASS` is a dated record of one run on one machine at one commit, and `dist/` stays unproven |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-1201](../stories/STORY-1201-the-qa-harness-two-agents-one-skill-one-catalogue.md) | The QA harness — two agents, one skill, one catalogue | done — a **retrospective record**; the code merged in `#1159` and `#1161` before the story existed |
| `STORY-1202` | Round 1 — the smoke suite passed, and one case did not run as written | done |
| [STORY-1203](../stories/STORY-1203-the-qa-cases-skill-the-authoring-half.md) | The `qa-cases` skill — the authoring half, whose last act is a printed command | done — **not a round story**; it builds the skill `ADR-0090` §3 licensed and runs no round |
| [STORY-1204](../stories/STORY-1204-the-epic-04-and-epic-05-catalogue-suites.md) | The `EPIC-04` and `EPIC-05` catalogue suites, authored from merged sources | done — **not a round story**; it is the first `/qa-cases` pass, and it takes the number the note below had reserved for the first round |
| `STORY-1205`, `STORY-1206` | One story per QA round; the round number lives in the story, not the id | done — the two `qa`-focus round stories. The `uat`-focus rounds are `STORY-1209`–`STORY-1211` below, and every ticket under all five is `done` as of 2026-08-31 |
| [STORY-1207](../stories/STORY-1207-the-uat-focus-the-observer-and-what-it-may-file.md) | The UAT focus — the observer, the harness verb, the route map and what may be filed | done — **not a round story**; it builds what `ADR-0092` §8 names, and runs no round |
| [STORY-1208](../stories/STORY-1208-the-verdict-table-never-checks-for-a-baseline-round.md) | Step 6 stops a healthy cycle — the verdict table never checks for a baseline round | done — **not a round story**; it repairs a defect in this cycle's own machinery, and runs no round |
| [STORY-1209](../stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | Round 1 (UAT) — the front door was never dressed, and four screens have no card | done — **the first round under the `uat` focus**; `B(1) = 1`, verdict `PROCEED (conformance unjudged on 4 of 11 screens)` |
| [STORY-1210](../stories/STORY-1210-round-2-uat-the-four-new-cards-were-not-a-tautology.md) | Round 2 (UAT) — the four new cards were not a tautology, and the screens behind them are undressed | done — **the first baseline round this cycle has run**; `B(2) = 3`, verdict `PROCEED`, unqualified because no cell read `BLOCKED` |
| [STORY-1211](../stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md) | Round 3 (UAT) — the count fell to zero, and what is still wrong is written down | done — **the last round rule 5 permits, and the invocation's exit state**; `B(3) = 0`, verdict `PASS`, fix set empty |
| [STORY-1212](../stories/STORY-1212-the-audit-focus-the-observer-the-resize-and-what-a-criterion-costs.md) | The audit focus — the observer, the resize, and what an unmet criterion costs | done — **not a round story**; it builds what `ADR-0096` §7 and `ADR-0097` §7 name, and runs no round |
| [STORY-1213](../stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md) | Round 1 (audit) — three criteria unmet, and a table that names the wrong winner | done — **the first round under the `audit` focus**; `A(1) = 3` of a five-criterion rubric (`R1`, `R2`, `R4`), `A(0)` **n/a**, verdict `PROCEED`, fix set **3** in the rubric's own order |
| [STORY-1214](../stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md) | A duel played by hand deadlocked on presence, and the catalogue could not have caught it | done — **not a round story**, and the first defect here found *outside* the cycle: the human played a real duel through two browsers and could not play it, on a product three rounds had passed. §Termination rule 1 sends it to the **ordinary backlog**, so `STORY-1213` is not reopened, `A(1)` stays 3 and no verdict is recomputed. It repairs the two catalogue wordings that made the defect invisible — `CORE-06` enumerating three things and omitting presence, `CORE-18` asserting the away mark *appears* and never that it *stays away* — and records that `TASK-120502` was dropped as this cycle's own instrumentation when it was a real defect |
| [STORY-1215](../stories/STORY-1215-the-duel-tables-last-sub-pixel-and-the-headroom-it-buys.md) | The duel table's last sub-pixel, and the headroom it buys | done — **not a round story**, and the first story here that repairs **nothing**: [`ADR-0106`](../../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md) §1 rules the duel table's true 664.90625 against 664 **is a fit**, so its one ticket is filed by §4 for what it buys — 23.09375 px of headroom where the column stands 0.09375 px from the fence, and `ADR-0106` §5's second read retired at every future round. §Termination rule 1 sends it to the **ordinary backlog** with **no due date**: no `A(N)` or `B(N)` moves, and `STORY-1213`'s verdict is not recomputed. It is the **seventh** non-round story, after `STORY-1214`, and it moves the round stories' next free id to **`STORY-1216`** |

Rounds are numbered in the story body rather than encoded in the id, because a round is created
when it is run and the ids stay sequential without arithmetic. **`STORY-1207` is the third story here that
is not a round**, after `STORY-1203` and `STORY-1204`: it builds the `uat` observer, the `shot`
verb, the catalogue's UAT section and `qa-manager`'s second-focus rules, and brings no stack up.
Round stories continue to take the next free id, so the one after `STORY-1206` is `STORY-1209`. **`STORY-1203` and `STORY-1204` are
the two stories here that are not rounds**, the way `STORY-1201` is the one that is a retrospective
record: `STORY-1203` builds `qa-cases`, the authoring skill `ADR-0090` §3 licensed, and `STORY-1204`
is the first pass that skill performs — writing the `EPIC-04` and `EPIC-05` suites from merged
sources. Neither brings a stack up, starts a browser or reports a `B(N)`. The round stories
therefore resume at **`STORY-1205`**, which supersedes `STORY-1203`'s sentence reserving `STORY-1204`
for the first round. That is stated rather than done quietly, because **this is the second time the
convention has moved by one**, and a convention silently shifted twice is one nobody can rely on
afterwards — each shift is written into this table's own rows, so a reader never has to infer it.

**Round 2 is the first **baseline round** this cycle has run**, and `STORY-1210` records the
determination in full: `duels`, `leaderboard`, `account` and `sign-in` became conformance-judgeable
for the first time on cards merged in round 1's repairs, so rule 4's comparison is skipped and
`B(2) = 3` is not compared against `B(1) = 1`. Rule 5's budget binds regardless — round 3 is the
last, and it gets no exemption.

**Round 3 ended the invocation `PASS`**, and `STORY-1211` records it. `B(3) = 0`: no `blocker` and no
`high` survived triage, so the fix set is empty and rule 4's comparison — which applied in full, round
3 being **no** baseline round — could not trip at `0 >= 3`. Rule 5's condition held too (`N == 3`) and
changed nothing, since round 3 was the last round at any `B(3)`; `PASS` is the stronger true statement
and is the one emitted. **The `PASS` is not a coverage or a readiness claim** (`ADR-0089` §2c,
`ADR-0093`): nineteen tickets across the three rounds remain open, eight of them filed by round 3
itself. `STORY-1211` also **refused** to read `ADR-0092` §6's baseline rule onto a second candidate —
two frames made judgeable for the first time by `TASK-121008`'s new harness verbs rather than by a
card — and registered `DEC-093` for the architect instead of widening the rule at the triage the
widening would have saved.

**`STORY-1208` is the fourth non-round story, and the only one that repairs a defect in this
cycle's own machinery.** `qa-manager`'s `## Step 6` and §Termination rule 4 below both special-case
round 1 and nothing else, so read literally they fire `STOP_DIVERGING` on the first round in which
repaired cards became measurable — the outcome `ADR-0092` §6's baseline rule exists to prevent. It
is the **third** time the convention has moved by one, and it moves the round stories' next free id
to `STORY-1209`, which the sentence above now says.

**`STORY-1212` is the fifth non-round story, and it makes the cycle carry a third focus** — the
same shape `STORY-1207` had one focus earlier. It builds the `size` verb `ADR-0097` §2 specifies,
the `audit` observer that `ADR-0097` §4 makes the **fifth** declared file, the `audit` focus of
`qa-cycle`, `qa-manager`'s rubric classifier and `A(N)` arithmetic, and this section's own copy of
the audit's stopping rules. It brings no stack up and reports no `A(N)`. It is the **fourth** time
the convention has moved by one, and it moves the round stories' next free id to **`STORY-1213`**,
which the first audit round will take. It writes **no rubric document and transcribes no
criterion** — `ADR-0096` §2 supplies the rubric merged, closed and citable by id — and it registers
**`DEC-098`** for the architect on whether a working copy should exist at all; that decision
blocked nothing here and was answered the same day by
[`ADR-0099`](../../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md)
— no working copy, ever, and the story's refusal to transcribe became the standing rule.

**`STORY-1213` is the first audit round, and it took the id `STORY-1212` reserved for it** — the
convention has now moved by one five times, and each shift is written into this table's rows rather
than left to be inferred. Three things about it are worth having in the epic rather than only in the
story, because each is the first time a merged rule was exercised rather than only written:

- **`A(1) = 3`, and round 1 has no `A(0)`.** The absence is stated in the round record so nobody
  reads the missing comparison as an exemption somebody granted. Rule 4 applies in full at round 2,
  and the bar is already explicit: **`A(2) ≤ 2` or the invocation ends `STOP_DIVERGING`.**
- **The count excludes one functional defect, on `ADR-0096` §5.** `TASK-121304` — the table
  announcing the previous duel's winner and amount after a rematch — is `high`, is filed to the one
  ledger, is scheduled in round 1's repairs because the eight-cap does not bind, and is **not** in
  `A(1)`: `A(N)` counts criteria, and folding a defect in would break `A(N) ≤ |rubric|`. Repairing
  it here means no `B(N)` ever counts it, which is what that rule wants rather than an evasion of
  it.
- **`ADR-0096` §5's promotion mechanism was searched and came back empty.** There is no
  `status: backlog` ticket anywhere in `tasks/` any more — the seventeen rows that ADR was written
  about were all repaired between 2026-08-30 and 2026-08-31 — so all four tickets are new. The
  mechanism is unexercised in the first round that could have exercised it, and the trail says so.

**No baseline determination is recorded for an audit round, and the report carries no `BASELINE:`
line.** Rule 4's exemption is defined by a *screen* becoming conformance-judgeable on a card merged
in the previous round's repairs; this focus judges **beats against criteria** and has no per-screen
conformance check to unlock, and `ADR-0096` §5 does not list a baseline exemption among this focus's
termination rules. `DEC-093` — whether the exemption extends beyond a card — is open, the
architect's, and gates nothing here.

## Termination

**This is the epic's load-bearing section.** A quality loop that never ends is worse than no loop:
it consumes the budget that would have shipped something, and it trains everyone to ignore its
reports. Five rules, all enforced by `qa-manager`, and all of them measurable rather than
judgemental.

1. **The round's bug set is frozen at triage.** Only defects in round *N*'s QA report are eligible
   for repair in round *N*. Anything found during retest belongs to round *N+1*'s report and
   cannot extend round *N*. Without this rule the loop has no fixed point, because retest always
   finds something.
2. **Only `blocker` and `high` are repaired in-cycle.** `medium` and `low` are filed to the
   backlog and never scheduled by this cycle. This bounds the work per round to the severities
   that justify interrupting the roadmap. **Scoped by
   [`ADR-0096`](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)
   §5 to the `qa` and `uat` focuses**, on the human's call: under the **audit** focus there is no
   severity at all — a criterion is `met` or `not met` — and no backlog, because a finding deferred
   by rule 3's cap stays counted in `A(N)` until it is repaired. That scoping is the repair of the
   seventeen `status: backlog` rows this rule produced; the rule's text is otherwise unchanged and
   still governs both other focuses word for word.
3. **A fix set is at most eight tickets.** A round that would exceed it takes the eight
   highest-priority and files the rest to the backlog. A round is a bounded unit of work, not a
   queue drain. Under the **audit** focus the eight-ticket cap orders repair by the rubric's own
   order, top to bottom — a deterministic tiebreak with no judgment in it — and a finding the cap
   defers stays an unmet criterion and is counted again next round, because filing does not reduce
   `A(N)`, only repair does
   ([`ADR-0096`](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)
   §5).
4. **Convergence.** Let `B(N)` be the count of `blocker` + `high` in round *N*'s report. If
   `B(N) >= B(N-1)` the cycle **stops** and reports non-convergence. This is the direct answer to
   *"each time report more and more bugs"*: the loop is permitted to continue only while it is
   demonstrably winning. Two rounds are exempt from the comparison: round 1, which has no round 0
   to compare against, and a **baseline round** — a round in which a screen becomes
   conformance-judgeable for the first time, its card merged in the previous round's repairs. The
   comparison would score the unlock as decay if both rounds were compared, since the two rounds
   measured differently-sized judgeable sets. See
   [`ADR-0092`](../../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
   §6. **Rule 5's three-round budget binds regardless.** Under the **audit** focus the quantity
   compared is `A(N)`, the number of criteria answered `not met`, so the rule reads
   `A(N) >= A(N-1)` and `A(N)` can never exceed the rubric's size — a ceiling known before the
   round starts. Round 1 has no `A(0)`, exactly as it has no `B(0)`.
5. **At most three rounds per invocation**, whatever else is true.
6. **A failure that does not reproduce by hand is a harness defect, and never enters `B(N)`.**
   Added by [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
   §4. Before `qa-manager` may file a `blocker` or `high`, the failing case must reproduce by hand —
   by the matching step of `ADR-0088` §2 where one exists, or by a stated sequence of player actions
   where it does not. It reproduces → a product defect, counted in `B(N)`. It does not → a **harness**
   defect: filed against this epic, repaired in `scripts/qa/` or `docs/test-plan.md`, excluded from
   `B(N)`, and **no production code may be changed to make it pass**. Rules 1–5 bound how much work
   a round does; this one bounds whether the work is real. Without it a stale catalogue reads as a
   product getting worse and trips rule 4 on a healthy product, or step 4 of the loop merges a diff
   to satisfy a string the client moved.

**Exit states**, all terminal and all reported:

| State | Condition |
| --- | --- |
| `PASS` | a round's report has zero `blocker` and zero `high` — or, under the audit focus, `A(N) = 0` |
| `STOP_BUDGET` | three rounds ran |
| `STOP_DIVERGING` | rule 4 tripped |
| `STOP_BLOCKED` | a `DEC` was raised that only the human can answer |
| `STOP_INFRA` | the stack could not be brought up, twice |

A cycle that ends in any state other than `PASS` is a **successful run** — it stops and says why.
An agent that keeps going because it has not finished is the failure this section exists to
prevent.

## Definition of done

- [x] `DEC-082` is answered by a merged ADR —
      [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md),
      2026-08-29.
- [ ] A harness defect and a product defect have been told apart at least once, on the record: a
      failing case that did not reproduce by hand was filed against this epic and kept out of
      `B(N)` (§Termination rule 6). Until that happens the rule is untested prose.
      **Still open after the first UAT round.** `STORY-1209` aimed rule 6 at its one candidate — a
      silent no-op on *Create a duel room* — reproduced it by hand under a **varied mechanism**
      (trusted CDP input rather than the driver's in-page click), three times in four, and found a
      product defect with an `InvalidStateError` behind it. The rule was exercised; the box needs a
      case that reproduces the *other* way.
- [x] `STORY-1201` is `done`: the two agents, the skill and the catalogue exist, and the skill's
      stack lifecycle uses no denied verb —
      [`STORY-1201`](../stories/STORY-1201-the-qa-harness-two-agents-one-skill-one-catalogue.md),
      2026-08-29, which carries the command for each half of that sentence. It is a **retrospective
      record**: the harness merged in `#1159` and `#1161` with no story file and no tickets, and the
      story was written afterwards to close the gap in the trail. The box is ticked on the
      structural checks passing at commit `5848e529`, not on the story having been planned first.
- [ ] One full cycle has run end to end and terminated in a named exit state, with its round
      ledger committed.
- [ ] The cycle demonstrably stops: a round whose `B(N)` did not decrease ends the run, proved by
      a recorded round rather than by argument.
- [ ] No file under `web-client/` gained a browser-driver dependency.
- [ ] `.github/workflows/build.yml` still has two jobs.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Rounds run | |
| Defects found / repaired / deferred | |
| Exit state distribution | |
| Escaped defects — found by a human after a `PASS` | |
| **Criteria added to the audit rubric, per invocation** (`ADR-0096` §6 — the proxy for *finished*: the curve flattens when the list has caught up with the human's eye) | |
