---
schema: 2
id: TASK-041504
title: The result screen carries an offer it does not make, and gives nothing up for it
type: task
status: done
parent: STORY-0415
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041501]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/DuelResult.test.tsx 2>&1 | grep -qE 'Tests +16 passed \(16\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/result-no-derivation.test.tsx 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'adds no offer of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the offer it is handed between the rematch and the way back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'disables nothing it already carried when it carries an offer'
  - test "$(grep -oF 'props.offer' web-client/src/result/DuelResult.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'AccountOffer' web-client/src/result/DuelResult.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'AccountOffer' web-client/src/result/DuelResult.test.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`DuelResult` takes an `offer` slot, renders it between the rematch and the way back, and makes no
offer of its own — the same contract it already has for `rematch`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify |
| `web-client/src/result/DuelResult.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/result/outcome-fixture.ts` — `anOutcome`, and the comment explaining why its three
  numbers are mutually independent. The tests below use it unmodified.
- `web-client/src/result/result-no-derivation.test.tsx` — `numbersOnScreen` and
  `shows no number the outcome does not carry`. It renders `DuelResult` with **no** offer, so this
  diff does not move it; a `verify:` line pins that file at **exactly 3** passing tests.
- `web-client/src/result/RematchControl.tsx` — what a real slot occupant looks like. It is **not**
  used by these tests; see `## Scope`.
- [`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md) §Decision —
  *"an offer, not a gate… declining does not degrade anything"*, which is what the third test is.

## Scope

- One optional prop and one line of JSX, and that is the whole source change:

  ```tsx
  /**
   * An optional account offer (`ADR-0036`). The panel does not decide whether one
   * is due — it renders what it is handed, exactly as it does for `rematch`.
   */
  offer?: ReactNode;
  ```

  placed after `rematch?: ReactNode;` in the props type, and `{props.offer}` placed **between**
  `{props.rematch}` and the *Back to the lobby* `<a>`. Measured: six added lines in
  `DuelResult.tsx`.
- **The tests hand it a plain stub, never `AccountOffer`.** `offer={<section aria-label="the
  offer">An offer</section>}` — the same choice the merged `puts the rematch it is handed above the
  way back` makes with `<button type="button">Rematch</button>` rather than `RematchControl`. Two
  reasons: this file's subject is the **slot**, and importing the real component would make this
  ticket depend on `TASK-041503` for nothing. Two `verify:` lines pin `AccountOffer` at zero
  occurrences in both files.
- **`{props.offer}` appears exactly once** — a `verify:` line counts it. Rendering it twice, or in
  two branches, is how a panel ends up showing the offer beside a defeat.
- Nothing else in `DuelResult.tsx` moves: the verdict heading, the coin line, the meta line, the
  rematch slot and the way back are untouched.

## Out of scope

- **Deciding whether an offer is due.** No `verdict` check, no `signedIn`, no `settled`, no import
  from `account-offer.ts`. `DuelResult` renders what it is handed — the contract its own `rematch`
  KDoc already states.
- **Importing `AccountOffer`.** Gated at zero in both files. `DuelResult` knowing the component
  would be the panel making its own offer, which is what test 1 forbids.
- **Wiring `Lobby.tsx` to fill the slot.** That needs a `settled` source, which is `DEC-079`'s and
  `DEC-080`'s; both are open, and the wiring ticket is not written yet.
- **Any change to the eleven merged assertions above the new ones.** This diff **appends** three
  tests and edits none. A `verify:` line requires the file to hold exactly 16.

## Tests

`web-client/src/result/DuelResult.test.tsx`, appended inside the existing `describe("the result
screen")`, after `takes the way back with no onLeave to call`. **13 merged tests become 16.**

| Test | Proves |
| --- | --- |
| `adds no offer of its own` | With no `offer` prop, no `region` named *the offer* exists and the panel's whole `textContent` matches no `/password/i`. The exact shape of the merged `adds no rematch of its own`, which is why the second assertion is there: a `queryByRole` alone cannot see a panel that offers in different words |
| `puts the offer it is handed between the rematch and the way back` | Given both a rematch stub and an offer stub, `compareDocumentPosition` puts the offer **after** the rematch and **before** the way back. Two assertions, because one of them alone permits the other end to be wrong |
| `disables nothing it already carried when it carries an offer` | With an offer present the rematch is not `disabled`, `+1 duel coin` is still printed and the way back still points at `/`. `ADR-0036`'s *"an offer, not a gate"* — `STORY-0415`'s *"Dismissing leaves every capability intact"* at the level this file can see |

**No `try` anywhere in the added code, and no `expect()` inside one.** A failing assertion is itself
a throw; a `try` around one swallows the failure and the test passes green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'adds no offer of its own'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the offer it is handed between the rematch and the way back'`
      — passes, asserting **both** neighbours
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'disables nothing it already carried when it carries an offer'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/DuelResult.test.tsx 2>&1 | grep -qE 'Tests +16 passed \(16\)'`
      — **exactly sixteen**: the thirteen merged tests plus these three. A deleted merged test reads
      15 and fails; a fourth added test reads 17 and fails. This is how the eleven assertions this
      ticket must not touch are pinned — **by a count, not by name**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/result-no-derivation.test.tsx 2>&1 | grep -qE 'Tests +3 passed \(3\)'`
      — the derivation sweep over the same component is untouched and still three
- [ ] `test "$(grep -oF 'props.offer' web-client/src/result/DuelResult.tsx | wc -l | tr -d ' ')" = 1`
      — the slot is rendered in exactly one place
- [ ] `test "$(grep -oF 'AccountOffer' web-client/src/result/DuelResult.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'AccountOffer' web-client/src/result/DuelResult.test.tsx | wc -l | tr -d ' ')" = 0`
      — neither file names the component, **comments included**. Say *the offer* in prose
- [ ] `cd web-client && npm run check` exits 0. With only `TASK-041501` merged the suite reads
      **815 passed (815)**; the four-ticket projection reads **822 passed (822)**
- [ ] Every merged test in `DuelResult.test.tsx` passes unchanged — no expected value moves, none is
      weakened, and this diff appends only
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run in this worktree**, with this ticket's diff applied to `develop` at
`922d57fc` and **nothing else**: **814 tests / 103 files**, green, plus typecheck, lint and
`prettier --check`. Baseline **811 / 103**. Record what you actually measure; never record the
unmutated state as a step's "actual", and never write *would*, *if done* or *not testable*.

1. Move `{props.offer}` to **after** the *Back to the lobby* `<a>`. **`puts the offer it is handed
   between the rematch and the way back` reddens alone** — measured, `1 failed | 813 passed (814)`.
   Revert.
2. Delete the `{props.offer}` line entirely. **The same test reddens alone** — measured,
   `1 failed | 813 passed (814)`. Note what this step also shows: `disables nothing it already
   carried…` does **not** redden, and that is correct — it guards the other direction, and its own
   mutation is step 3. Revert.
3. Suppress the coin line when an offer is present — `{coin !== null && props.offer === undefined &&
   (…)}`. **`disables nothing it already carried when it carries an offer` reddens alone** —
   measured, `1 failed | 813 passed (814)`. This is the realistic version of the defect: a panel
   making room for the offer by dropping something it already said. Revert.
4. Make the panel offer for itself — `{props.offer ?? (<section aria-label="the offer">Keep them
   with a password</section>)}`. **`adds no offer of its own` reddens alone** — measured,
   `1 failed | 813 passed (814)`. Revert.
5. **The same step with the real component instead of a stub**, run outside this ticket's two files
   because the budget governs the diff and not the probe: import `AccountOffer` and use
   `{props.offer ?? <AccountOffer onDismiss={() => {}} />}`. **Four tests redden** — measured on the
   projection where `TASK-041501` and `TASK-041503` are also applied: `adds no offer of its own`
   plus three **merged** tests — `declares a draw, and moves no coin`, `says the duel is over when
   the client holds no seat` and `drops the owner words when the client holds no seat` — because the
   offer's copy contains *"You"* and *"duel coins"*, which those three assert are absent. Worth
   knowing before the wiring ticket: **the panel's existing negatives already constrain what may be
   rendered inside it.** Revert every file the step touched.

> **A red run names a prefix, not a set.** A syntax error in `DuelResult.tsx` was measured here to
> fail **twelve** test files at collection and print `667 passed` with **no failure count at all** —
> the suite never reached them. If a step's output looks unrelated to the mutation, check for a
> collection error before concluding anything about coverage.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** The
> warning is misleading and was checked rather than believed: with `NO_COLOR=1` the summary line is
> plain bytes; **without** it the same line carries escape codes and `grep -qE 'Tests +16 passed
> \(16\)'` stops matching. Keep `NO_COLOR=1` on every grep.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this ticket depends on `TASK-041501` at all.** It does not need it: its diff is green on
`develop` alone, measured. The dependency is **sequencing** — `STORY-0415` starts with one startable
ticket, and once `TASK-041501` merges this one, `TASK-041502` and `TASK-041503` are all startable
with **pairwise disjoint `Files` tables**, so they can run in one batch.

**`grep -c` counts matching lines and exits 1 on zero matches**, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps each needle literal.
