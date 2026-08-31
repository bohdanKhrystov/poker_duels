---
schema: 2
id: TASK-120908
title: The table's sizing control is the card's presets, not a range slider
type: task
status: backlog
parent: STORY-1209
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 5
labels: [qa, uat, bug, medium]
depends_on: [TASK-120914]
atomic:
  - web-client/src/e2e/whole-duel.test.tsx — "sends one Act for each YourTurn, and the frame the server recorded" is red for every ordering of a smaller commit
verify:
  - sh -c '! grep -q "<input" web-client/src/table/ActionBar.tsx'
  - sh -c '! grep -q slider web-client/src/table/ActionBar.test.tsx'
  - sh -c '! grep -q slider web-client/src/table/bar-no-derivation.test.tsx'
  - sh -c '! grep -q slider web-client/src/e2e/drive-duel.tsx'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "the sizing row offers the card's five presets"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "each preset sets the amount its own name states"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "a preset the stack cannot afford is not offered"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "a preset under the server's minimum is not offered"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/ActionBar.test.tsx 2>&1 | grep -qE "Tests +26 passed \(26\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/bar-no-derivation.test.tsx 2>&1 | grep -qE "Tests +3 passed \(3\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/claimed-here-recovered-there.test.tsx src/e2e/drive-duel.test.tsx 2>&1 | grep -qE "Tests +24 passed \(24\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The action bar sizes a bet or a raise the way `design/screens/duel-table.html` draws it — five named
presets a player hits in one press, each computing the quantity its own label names — instead of a
native range slider a player has to aim; and the scripted-duel driver reaches every recorded amount
by pressing those presets, through no door a player does not have.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`, against
`design/screens/duel-table.html`.

**Shipped**, read from the running client:

    <input aria-label="raise to" class="flex-1" max="10000" min="200" step="1" type="range" value="200">
    <span class="font-mono tabular-nums">200</span>

**The card** draws `.sizing` holding five `.chip` presets — `min`, `⅓`, `½`, `pot`, `all-in` — with a
`+`/`−` stepper beside them.

## Why this ticket was rewritten, not amended

Its first coder replaced the input, got all three of its own tests green, then hit **24 failures
across four merged e2e files** outside its two-file budget and routed rather than widen scope
(`CLAUDE.md` rules 4 and 5). Both halves are now answered:

- [`ADR-0100`](../../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md)
  — the driver presses the sizing row and reads the action button before it clicks; §5 refuses **by
  name** a driver-only slider, any test-only prop or `data-testid`, and any reach into React state
  or `actFrame`. §4 kills the old ticket's third scope bullet: the actions row keeps **every** action
  the server named.
- [`ADR-0101`](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
  — what each chip computes, and that an illegal chip is **absent**, never clamped, at *both* ends.

**Size, stated honestly.** The diff is larger than schema 2's `S` guide of 120 changed lines, mostly
because two required props reach 23 render sites in two test files. Schema 2 has no value above `S`
(`M` was deleted), so `S` is declared and `atomic:` carries the real claim: this cannot be split,
and the *Files* table below says which gate refuses each smaller commit.

## Files

The set was **measured, not remembered**: the change was stubbed in `ActionBar.tsx` alone and the
whole of `.github/workflows/build.yml`'s pull-request gate set was run — `./gradlew check
-PrequireDocker=true` (green, 3m11s, Docker up, no suite skipped) and, in `web-client/`, `npm ci`,
`npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run test` and `npm run build`, each
on its own so no failing prefix could hide a later gate. Only these five paths were named.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify | the change itself |
| `web-client/src/lobby/Lobby.tsx` | modify | `npm run typecheck` — `TS2739` at `Lobby.tsx(166,10)`: the bar's two new props are missing at the one production render site |
| `web-client/src/e2e/drive-duel.tsx` | modify | `npm run test` — 24 failures across `whole-duel.test.tsx`, `duel-secrecy.test.tsx`, `claimed-here-recovered-there.test.tsx` and `drive-duel.test.tsx`, all from `drive-duel.tsx:125`'s `getByRole("slider")` |
| `web-client/src/table/ActionBar.test.tsx` | modify | `npm run typecheck` — `TS2739` at 20 render sites; `npm run test` — 10 of its 22 tests fail |
| `web-client/src/table/bar-no-derivation.test.tsx` | modify | `npm run typecheck` — `TS2739` at 3 render sites; `npm run test` — *counts the ceiling that reaches the player only as a slider bound* fails at line 92, because the bound it counts is the thing being deleted |
| `design/screens/duel-table.html` | read | the sizing row's anatomy — five chips, their labels, their order. `TASK-120914` corrects its one wrong amount first; that is why this ticket depends on it |
| `docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md` | read | §§1–4 — the formulas, the offer rule and the worked frames |
| `docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md` | read | §1 the driver's algorithm, §4 the actions row, §5 the refusals |

`ADR-0100` §7 lists a sixth file, `web-client/src/table/turn-fixture.ts`, and conditions it: *"only
if the bar's new input needs a default."* It does not. No gate names it, and the two test files
declare their own frames explicitly — which `ADR-0101` §7 requires anyway, because a shared
`committedThisStreet: 0` default is exactly the value that hides the bug the second test exists to
catch. The fixture stays untouched, and so does `turn-fixture.test.ts`.

## Scope

### 1. The sizing row replaces the range input

- The one `<input>` in `ActionBar.tsx` goes. Nothing hidden, disabled or visually-hidden replaces it
  (`ADR-0100` §5).
- In its place, up to five `<button type="button">` chips whose accessible names are exactly the
  card's, in the card's order: `min`, `⅓`, `½`, `pot`, `all-in`. **A chip prints no amount** — the
  card draws no figures on them, and a chip that printed its own total would redden
  `bar-no-derivation.test.tsx`'s first test (`ADR-0101` §7).
- Pressing a chip only sets the dialled total. It sends nothing.
- The row appears exactly when it appears today: when the server allowed `BET` or `RAISE`.
- **Every chip carries `disabled={sent}`.** Measured, not assumed: with the chips left live, mutating
  that one attribute reddens `whole-duel.test.tsx`'s *sends one `Act` for each `YourTurn`* — its
  witness asserts every button inside `aria-label="your move"` is disabled right after a click.

### 2. Two labelled groups, so a row can be addressed

The bar's two existing `<div>`s gain `role="group"` and a stable `aria-label`: the sizing row
`aria-label="amount"`, the actions row `aria-label="actions"`. Neither label contains a digit, so
`bar-no-derivation.test.tsx`'s number scan is unaffected (measured). This is assistive-technology
labelling of rows the card already draws, in the same idiom as the section's own
`aria-label="your move"` — it is not a control, and it is not one of the doors `ADR-0100` §5 refuses.

### 3. The amounts, exactly as `ADR-0101` §§1–3

With `P` the pot including this street and `seat` the acting seat:

```
toCall = legalActions.callTo − committedThisStreet
base   = P + toCall
```

| Chip | Sets `to` |
| --- | --- |
| `min` | `minRaiseTo`, or `minBetTo` when the server allowed `BET` |
| `⅓` | `callTo + floor(base / 3)` |
| `½` | `callTo + floor(base / 2)` |
| `pot` | `callTo + base` |
| `all-in` | `allInTo` |

Rounding is **`floor`**, in integer arithmetic (`Math.floor`, never `Math.round` or `toFixed`).

**A fraction chip is rendered only when `floor ≤ amount ≤ allInTo`**, where `floor` is `minRaiseTo` or
`minBetTo` as the server allowed. Otherwise it is **absent** — never clamped into range, never
rendered dead or greyed. `min` and `all-in` are never absent while the row is shown; the engine caps
both into range itself. Two chips may print the same total and both stay; the row's contents depend
on legality alone, never on a comparison between chips.

### 4. The actions row keeps every action the server named

`ADR-0100` §4. The old ticket's *"the actions row becomes the card's three"* **does not ship**: the
card draws one state, and `BettingRules.kt` reaches states it does not draw. An `ALL_IN` button
appears whenever the server names `ALL_IN`, exactly as today, and the recorded `AllIn` step is
pressed the way it is pressed today. All-in may therefore read twice on one bar — as a chip that
sizes and a button that sends. That is accepted (`ADR-0100` *Consequences*); how it is dressed is a
card's question, not this ticket's.

### 5. The pot reaches the bar from `Lobby.tsx`

`ActionBar` takes two new **required** props beside `turn`, both built from numbers already on the
wire — no protocol change, no `PROTOCOL_VERSION` move, no new store field:

- `potIncludingStreet: number` — `view.pot` plus every seat's `committedThisStreet`. Sum over
  `view.seats`; that equals `ADR-0100` §6's `seats[0] + seats[1]` for the two seats heads-up always
  has, and does not throw on a frame carrying fewer.
- `committedThisStreet: number` — the **acting** seat's, found by
  `seat.index === turn.legalActions.seat`, and `0` when there is no pending turn.

The bar does **not** reach for the store: it is a function of what it is handed. The props are
required so that `tsc` proves the one production call site passes them; an optional prop is the door
through which a silently unlabelled row ships.

### 6. The driver presses what a player presses

In `drive-duel.tsx`'s `actThroughTheBar`, the `Bet`/`Raise` branch replaces
`fireEvent.change(…getByRole("slider")…)` with `ADR-0100` §1:

1. Find the action button whose accessible name starts with the recorded verb — **unchanged, and it
   stays first**, so a missing button still throws today's message.
2. Read the total that button prints — strip non-digits from its `textContent` and compare to
   `action.to`. If they already match, click and stop (they do whenever the recording is the server's
   minimum, because the bar opens at the floor).
3. Otherwise press the sizing row's controls one at a time in **document order** —
   `within(getByRole("group", { name: "amount" })).queryAllByRole("button")` — **re-querying the
   action button after every press**, and click as soon as the printed total equals `action.to`.
4. If no press makes them equal, **throw**, naming the step index, the hand number, the recorded
   action type and amount, **and every amount the row reached**. Both sides, because a chip that
   computes wrongly presents exactly as a script that recorded oddly.

**The trap, found by probing and stated so nobody loses a day to it.** `driveScriptedDuel` wraps each
client step in `act(() => actThroughTheBar(…))`. Inside that wrapper React does not re-render between
presses, so the action button still prints the *old* total and the search reads the same number after
every press — the measured symptom is `no sizing control reached it — the row reached: 200, 200, 200,
200`. Drop the wrapper and call `actThroughTheBar(container, step, index)` directly; Testing
Library's own `fireEvent` is act-wrapped and flushes each press on its own. With that one change all
**24** tests in the four e2e files pass — **measured, with no edit to any of them**.

## Out of scope

- **The stepper.** The card's `+`/`−` and what one press moves the total by is `DEC-102`, the product
  owner's, open. `ADR-0101` §6 says it gates none of §§1–5 and the register says in as many words
  that this ticket is **not** blocked by it. Do not invent a step, do not ship a `+`/`−` pair.
- **The four merged e2e files** — `whole-duel.test.tsx`, `duel-secrecy.test.tsx`,
  `claimed-here-recovered-there.test.tsx`, `drive-duel.test.tsx`. Not in the *Files* table and not to
  be opened. `ADR-0100` §7: if the work finds itself editing one, it is weakening a proof and should
  stop and route.
- **The recorded script.** `scripted-duel.gen.json` is not regenerated and `ScriptedDuel.kt`'s 57/55
  step counts do not move. Every amount-carrying step lands on `minRaiseTo` or `allInTo`, both chips.
- **`web-client/src/table/turn-fixture.ts`** and `turn-fixture.test.ts` — see *Files*.
- **`design/screens/duel-table.html`** — its one wrong amount is `TASK-120914`, which lands first.
  `duel-table-states.html` is in arrears on nothing; its `3,250`s are a different frame.
- **Anything the server decides.** Legal amounts stay the server's. A dialled total is a *proposal*
  (`ADR-0100` §6); the bar computes no legality and asserts no game fact.

## The frames the tests use

Every figure below was computed against `ADR-0101` §§1–2, and the first three rows are the ADR's own
worked frames. `⅓` and `½` are the literal glyphs the card draws.

| Frame | `potIncludingStreet` | `committedThisStreet` | `callTo` | floor | `allInTo` | `base` | `min` | `⅓` | `½` | `pot` | `all-in` |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **A** card hero, `RAISE` | 2850 | 0 | 400 | `minRaiseTo` 800 | 13400 | 3250 | 800 | **1483** | 2025 | **3650** | 13400 |
| **B** re-raise, `RAISE` | 1400 | 200 | 600 | `minRaiseTo` 1000 | 13400 | 1800 | 1000 | 1200 | 1500 | **2400** | 13400 |
| **C** preflop button, `RAISE` | 225 | 75 | 150 | `minRaiseTo` 300 | 10000 | 300 | 300 | **250 — absent** | 300 | 450 | 10000 |
| **D** short stack, `RAISE` | 2850 | 0 | 400 | `minRaiseTo` 800 | 3000 | 3250 | 800 | 1483 | 2025 | **3650 — absent** | 3000 |
| **E** no bet outstanding, `BET` | 9000 | 0 | 0 | `minBetTo` 2750 | 9100 | 9000 | 2750 | 3000 | 4500 | 9000 | 9100 |

Why each frame earns its place:

- **A** exercises `floor`: `base/3` is 1083.33, so `⅓` is 1,483 and not 1,484 — the one frame that
  separates flooring from rounding. Its `pot` is the 3,650 `TASK-120914` puts on the card.
- **B** is the only frame that separates the answer from every near miss, because the hero has
  already committed 200 this street. `pot` is 2,400 there; `view.pot + 2×callTo` gives 1,800,
  `(view.pot + both committed) + callTo` gives 2,000, forgetting to subtract the hero's own 200 from
  `callTo` gives 2,600, and summing only the rival's commitment gives 2,200. **`ADR-0101` §7 requires
  at least one such frame**; a fixture whose acting seat has committed nothing cannot tell them apart.
- **C** is the bottom end, and it is real play, not a contrivance: once the blinds are posted the base
  is two big blinds, a third of it plus the call is 1⅔ big blinds, and the minimum raise is two — so
  **the button is never offered `⅓` preflop**, while `min` and `½` both print 300 and both stay.
- **D** is the top end: the same frame as **A** on a 3,000 stack, where `pot` alone is unaffordable.
- **E** is the `BET` branch, where `toCall` is 0 and the base is simply the pot — one rule, two
  behaviours, no second case.

## Tests

`ActionBar.test.tsx` — **26 tests** after this ticket: 10 untouched, 12 restated, 4 new.

**The four new tests.**

| Test | Proves |
| --- | --- |
| `the sizing row offers the card's five presets` | over frame **A**, the sizing group holds exactly five buttons whose accessible names are `min`, `⅓`, `½`, `pot`, `all-in` in that order; no `<input>` of any kind remains in the bar; and no chip's text or `aria-label` contains a digit |
| `each preset sets the amount its own name states` | over frames **A** and **B**, pressing each chip in turn and reading the `Raise to` button's printed total gives, for **A**, 800 / 1,483 / 2,025 / 3,650 / 13,400 and, for **B**, 1,000 / 1,200 / 1,500 / 2,400 / 13,400 — ten expected amounts, five distinct per frame, so no single hard-coded value and no near-miss formula passes |
| `a preset the stack cannot afford is not offered` | over frame **D**, the sizing group holds `min`, `⅓`, `½`, `all-in` and **no `pot`**, and the four that remain still print 800 / 1,483 / 2,025 / 3,000 |
| `a preset under the server's minimum is not offered` | over frame **C**, the group holds `min`, `½`, `pot`, `all-in` and **no `⅓`**; `min` and `½` both print `Raise to 300` and both are present, which is what a clamp could not produce; `pot` prints 450 |

**The twelve restated tests.** Each keeps its guard; none is weakened, and each is listed with what
moves and why.

| Test | What moves |
| --- | --- |
| `offers no control when there is no turn` | the `queryByRole("slider")` line goes: chips are buttons, so the `queryAllByRole("button")` assertion above it already forbids every control. Nothing else changes |
| `renders one button per action the server allowed, in the order it sent them` | scope the query to the **actions** group; the four expected names are unchanged |
| `renders no button for an action the server withheld` | same scoping; still three buttons and no `Check` |
| `fills the raise and leaves the other buttons ghosts` | same scoping, so `buttons[2]` is the raise again and not a chip |
| `clamps the amount control to the bounds the server sent` → **rename to** `offers only totals inside the bounds the server sent, betting and raising` | there is no bound attribute left to read. Over frames **A** and **E**, press every offered chip, read the action button, and assert each total lies in `[floor, allInTo]`; assert the row is non-empty in both frames; and assert frame **E**'s `½` is exactly 4,500 — half the pot with no call added. Strictly stronger: the old test read two attributes, this reads every amount the control can actually reach |
| `starts the amount at the server's minimum for the action it allowed` | name kept — it is what the driver's step 2 relies on. Read the freshly-rendered action button (`Raise to 1,200`, `Bet 350`) instead of the slider's `value`; the two frames and both numbers are unchanged |
| `offers no amount control when neither a bet nor a raise is allowed` | name kept. The slider assertion is now vacuous, so it is **replaced**, not deleted: over `[FOLD, CALL, ALL_IN]` the sizing group holds **no** buttons, and the bar's buttons are exactly `Fold`, `Call 400`, `All in 13,400` |
| `writes the raise button's total from the amount control` → **rename to** `writes the raise button's total from the preset the player pressed` | over frame **A**, press `pot` and read `Raise to 3,650` |
| `sends the total the amount control holds` | name kept. Press `pot` at the first decision point and `½` at the second, click `Raise to`, and assert the two `Act` frames carry **two different** totals (3,650, then frame **B**'s 1,500) |
| `disables every control once an action is sent` | name kept. After the click, assert **every** button in the `your move` region is disabled and that the count exceeds the action count — i.e. the chips are in the set. This is the assertion `whole-duel.test.tsx` depends on |
| `comes back to life on the next turn, at the new minimum` | name kept. Read `Raise to 2,400` off the action button instead of the slider's `value` |
| `returns the amount control to the minimum the server sent after a rejection` | name kept. Press `pot`, assert `Raise to 3,650`; after the rejection rerender assert `Raise to 1,200` — the server's minimum, reached by a remount and not by a clamp |

The other ten tests keep every assertion they have; they gain only the two new props at their render
sites.

`bar-no-derivation.test.tsx` — **3 tests**, count unchanged.

| Test | What moves |
| --- | --- |
| `shows no number the turn does not carry` | nothing but the two new props. It must stay green **because** a chip prints no amount |
| `counts the ceiling that reaches the player only as a slider bound` → **rename to** `counts the ceiling that reaches the player only after they press for it` | the bound is what goes. On a fresh render with `ALL_IN` withheld, `allInTo` now reaches the player **nowhere at all** — not printed, not spoken, not as an attribute — so assert `not.toContain(ceiling)`; then press the `all-in` chip and assert the ceiling appears as the action button's printed total, and that every number on screen is still one the turn carries. Both frames (`[CHECK, BET]`, `[FOLD, CALL, RAISE]`) stay. The old test forbade printing the ceiling and accounted for it as a bound; the new one forbids printing it *and* forbids its appearing at all before a player asks — nothing the old test forbade becomes allowed |
| `offers no control the turn did not allow` | the two new props, and its `queryByRole("slider")` line goes for the same reason as its twin in `ActionBar.test.tsx`: the `getAllByRole("button").map(…)` equality directly above it already pins the button list to exactly `["Check"]`, which forbids every chip. Leaving the line would leave an assertion that can never fail again |

## Acceptance criteria

- [ ] `ActionBar.test.tsx > the sizing row offers the card's five presets` passes
- [ ] `ActionBar.test.tsx > each preset sets the amount its own name states` passes over frames
      **A** and **B**, asserting ten amounts, and frame **B**'s acting seat has
      `committedThisStreet: 200`
- [ ] `ActionBar.test.tsx > a preset the stack cannot afford is not offered` passes with `pot` absent
      and the other four present
- [ ] `ActionBar.test.tsx > a preset under the server's minimum is not offered` passes with `⅓`
      absent and `min` and `½` both printing 300
- [ ] `ActionBar.test.tsx` reports **26 passed (26)**
- [ ] `bar-no-derivation.test.tsx` reports **3 passed (3)**, with its second test renamed and its
      other two carrying every assertion they carry today
- [ ] The four e2e files report **24 passed (24)** and `git diff` shows **no change to any of them**
- [ ] `ActionBar.tsx` holds no `<input>`, and `ActionBar.test.tsx`, `bar-no-derivation.test.tsx` and
      `drive-duel.tsx` hold the word `slider` nowhere at all — **including in comments and test
      names**, which is what the three `grep` gates check
- [ ] `Lobby.tsx` passes both props from `state.view`, and `tsc` proves it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
