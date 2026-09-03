---
schema: 2
id: TASK-130504
title: The bar carries a typed total, and a refused one sends nothing and says why
type: task
status: done
parent: STORY-1305
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, action-bar]
depends_on: [TASK-130503]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx 2>&1 | grep -qE '^ *Tests +30 passed \(30\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/bar-no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +3 passed \(3\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e/whole-duel.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e/drive-duel.test.tsx 2>&1 | grep -qE '^ *Tests +3 passed \(3\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - sh -c 'grep -q "readTypedAmount" web-client/src/table/ActionBar.tsx && ! grep -qE "Math\.(min|max)|clamp" web-client/src/table/ActionBar.tsx'
  - awk 'index($0, "aria-label=\"the total\"") { n++ } END { exit (n != 1) }' web-client/src/table/ActionBar.tsx
  - awk 'index($0, "data-testid") { n++ } END { exit (n != 0) }' web-client/src/table/ActionBar.tsx
  - awk 'index($0, "type=\"range\"") { n++ } END { exit (n != 0) }' web-client/src/table/ActionBar.tsx
  - awk 'index($0, "Math.floor") { n++ } END { exit (n != 2) }' web-client/src/table/ActionBar.tsx
  - sh -c 'grep -q "the total" web-client/src/table/ActionBar.test.tsx && ! grep -q "querySelector(\"input\")" web-client/src/table/ActionBar.test.tsx'
  - awk 'index($0, "querySelectorAll(\"input\")") { n++ } END { exit (n != 1) }' web-client/src/table/ActionBar.test.tsx
  - sh -c 'grep -q "reachTheAmount" web-client/src/e2e/drive-duel.tsx && ! grep -qE "fireEvent\.change|getByLabelText|\.value =" web-client/src/e2e/drive-duel.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The action bar's sizing row carries a field a player types a total into: a legal total is dialled
and sent exactly as typed, and one the server's standing word already refuses sends **no frame**,
takes **no sent-lock**, leaves the entry **byte-identical**, and says why on the bar's own notice
line.

## The one merged assertion this ticket invalidates, and it owns it

`web-client/src/table/ActionBar.test.tsx:141`, inside
`it("the sizing row offers the card's five presets")`:

```js
expect(container.querySelector("input")).toBeNull();
```

That is `ADR-0100`'s *no slider* guard, and a typed field contradicts it. It **cannot** be left
standing. It is replaced, in place, by the narrower guard it was always trying to be:

```js
expect(container.querySelectorAll("input")).toHaveLength(1);
```

plus, in this ticket's own first test, the assertions that the one input is a `type="text"` field
carrying no `min`, no `max`, no `step` and no `data-testid`. Nothing else in that test moves — its
five chip labels, its no-digit-on-a-chip sweep and its aria sweep all stand unchanged, and no
assertion anywhere in the file is weakened.

**Measured, not assumed.** The field was planted in `ActionBar.tsx` on `develop` `be02155b` and the
whole client suite run: **exactly one** assertion failed, the line above; the other **1033** tests
passed. Nothing in `Lobby.test.tsx`, `whole-duel.test.tsx`, `duel-secrecy.test.tsx`,
`no-derivation.test.tsx` or `null-view.test.tsx` is in the blast radius, and gates pin the ones that
could plausibly have been.

## What the field does when there is no view — settled here, not discovered

**It does not exist.** Three independent reasons, all already true:

1. `Lobby.tsx` mounts `WaitingTable` while `view === null` and never mounts `ActionBar` at all —
   which `null-view.test.tsx` already asserts by name (`queryByLabelText("your move")` is null).
2. Inside the bar, the field lives in `Live`, which is not rendered while `turn === null`.
3. Inside `Live`, the field is rendered only when `amountFloor(actions) !== null`, exactly like the
   sizing chips — so a turn offering neither a bet nor a raise has no field either.

So `EPIC-13`'s null-view contract needs no widening for this surface, and this ticket's gate pins
`null-view.test.tsx` at **6** to prove it was not reached. `TASK-130506` then writes the answer into
that file as an assertion rather than leaving it as three reasons in a ticket.

## The two traps in these files, read them before writing

- **`bar-no-derivation.test.tsx` sweeps `[min]`, `[max]` and `[value]` attributes**, and its second
  test asserts the ceiling reaches the player only *after* they press for it. A field written as
  `<input type="number" min={floor} max={allInTo}>` reddens it — **measured**: planting
  `max={actions.allInTo}` produced `expected [ 175, 175, 13400, 175 ] to not include 13400`. So the
  field is `type="text"` with `inputMode="numeric"` and **no bound attributes at all**. `type="number"`
  is refused for a second reason too: it hands back `""` for `12abc`, destroying the very text
  `ADR-0111` §1 says must stand exactly as typed. Note that `min={floor}` alone does **not** redden
  that file — the floor is a number the turn carries — which is why the field's own test asserts the
  absence of `min` directly.
- **The e2e driver must keep pressing what a player presses.** `ADR-0100` §5 and `ADR-0111` §6:
  `actThroughTheBar` gains no typing branch and sets no field's value.
  `web-client/src/e2e/drive-duel.tsx` is **not opened by this ticket**, and a gate proves it holds
  no `fireEvent.change`, no `getByLabelText` and no `.value =`. The field's own unit tests type into
  it the way a player would — that is testing the control, not a driver reaching past the UI.

## What is already true, measured on `develop` 2026-09-03

- Per-file counts: `ActionBar.test.tsx` **26**, `bar-no-derivation.test.tsx` **3**,
  `null-view.test.tsx` **6**, `whole-duel.test.tsx` **8**, `drive-duel.test.tsx` **3**,
  `Lobby.test.tsx` **80**.
- `ActionBar.tsx` holds `const [to, setTo] = useState(floor ?? 0)` in `Live`, and `Live` is keyed
  `${handNumber}:${actionSequence}:${rejectionCount}` — so a new decision point *or* a rejection
  remounts it and the amount returns to the server's minimum **by construction**. That mechanism is
  the reset for the typed entry too; do not add an effect.
- `<Notice>` is rendered by `ActionBar` as the section's last child, outside the `turn === null`
  ternary. **Moving it inside both branches was probed on this branch and the entire client suite
  stayed green — 1034 of 1034.**
- `Math.floor` appears exactly **2** times, both in `sizingChips`. The gate pins it at 2 so a
  rounding step cannot enter with the field.
- `drive-duel.tsx` contains no `fireEvent.change` today, so the driver gate is real rather than
  vacuous, and `reachTheAmount` is the positive half that keeps it from passing on a deleted file.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify |
| `web-client/src/table/ActionBar.test.tsx` | modify |
| `web-client/src/table/typed-amount.ts` | read |
| `design/components/action-bar.html` | read |
| `web-client/src/table/turn-fixture.ts` | read |

## Scope

- **`Live` holds the entry as a string, and it is the only amount state.** Replace
  `const [to, setTo] = useState(floor ?? 0)` with
  `const [entry, setEntry] = useState(floor === null ? "" : formatChips(floor))`, plus
  `const [entryRefusal, setEntryRefusal] = useState<string | null>(null)`. Derived, not stored:

  ```ts
  const reading = floor === null ? null : readTypedAmount(entry, floor, actions.allInTo);
  const dialled = reading !== null && reading.kind === "amount" ? reading.to : null;
  ```

- **The field**, rendered inside the `aria-label="amount"` group after the chips, only when
  `floor !== null`:
  - `aria-label="the total"` — the screen cards' own name (`TASK-130502`). The group keeps its
    merged name `amount`; do not rename it, three merged files query it.
  - `type="text"`, `inputMode="numeric"`, `disabled={sent}`, and **no** `min`, `max`, `step`,
    `pattern` or `data-testid`.
  - `value={entry}`, `onChange` sets the entry to the raw string the player typed — no filtering, no
    masking, no coercion (`ADR-0111` §§1, 3, and §Alternative 4) — and clears `entryRefusal`.
  - **Layout that cannot overflow the phone**: the field is pushed right (`ml-auto`), has a nominal
    width of about `7ch`, is allowed to shrink (`min-w-0`), and the chips are made `shrink-0` so
    they never lose their labels. The row's height is unchanged and it never wraps. Everything else
    about the field — border, radius, padding, mono tabular figures, right alignment, the faint
    disabled treatment — is **transcribed from the merged card's `.total` rule**, not re-derived.
    The card draws the field inside the unbuilt stepper; the client ships the field alone, so the
    row here is narrower than the row the card fits.
- **Pressing a preset writes into the field**: `setEntry(formatChips(chip.amount))` and
  `setEntryRefusal(null)`. There is no second source of truth for the amount.
- **The action button prints the proposal or nothing, never a different amount** (`ADR-0111` §7,
  `ADR-0100` §2):

  ```ts
  const text = actionText(type, actions, dialled ?? 0);
  const printed =
    (type === "BET" || type === "RAISE") && dialled === null ? null : text.amount;
  ```

  `action-text.ts` is **not** opened; the override lives here.
- **The press**: for `BET` and `RAISE` only, when `dialled === null`, set `entryRefusal` to the
  reading's sentence and **return** — no `setSent`, no `props.send`, no change to the entry. Every
  other button is untouched: a player holding a garbage entry can still fold, check, call and go
  all-in, because none of those carries a total.
- **The notice line says the local sentence first.** `Notice` takes a third prop,
  `entryRefusal: string | null`, and `noticeText` returns it when it is not null before falling
  through to the merged rejection and refusal arms. `ActionBar` passes `props.rejection` and
  `props.refusal` down into `Live`, which renders the `<p>` as its own last child; the
  `turn === null` branch renders `<Notice entryRefusal={null} …>` exactly as today. The `<p>` stays
  the section's last child in both branches, so the DOM and the bar's height are unchanged — and no
  fourth line is added, which is what keeps `ADR-0103` §1's `390 × 664` fit out of this diff.
- **Run `npm run format` before `format:check`** — `prettier-plugin-tailwindcss` reorders class
  lists.
- **Say the fit sentence in the PR.** The bar gains no row and the sizing row cannot wrap or
  overflow, so `ADR-0103` §1 is untouched by construction — write that, or, if the drawing you
  transcribed does change a box, paste the measured `scrollHeight` and `clientHeight` at
  `390 × 664` instead. It cannot be a `verify:` gate: `ADR-0089` §2b forbids a pull request waiting
  on a browser.

## Out of scope

- **The remaining refusal cases and the interval's endpoints.** The over-`allInTo` sentence, the
  plain `0`, the empty field, the negative, the repeated press, and the *button never prints a
  different amount* proof are all `TASK-130505`, which opens only `ActionBar.test.tsx`. They are
  split off for size, not because they are optional: the story is not done without them.
- **`null-view.test.tsx`.** `TASK-130506` writes this surface's answer into the contract; this
  ticket only pins the file at 6 to prove it was not reached.
- **`web-client/src/e2e/drive-duel.tsx` and every scripted-duel suite.** Not opened, not edited
  (`ADR-0100` §5, `ADR-0111` §6). Gates pin `whole-duel` at 8 and `drive-duel` at 3.
- **`action-text.ts`, `rejection-text.ts`, `chips.ts`, `act-frame.ts` and `typed-amount.ts`.** All
  merged and read-only here.
- **A stepper, ± buttons, or a step.** `DEC-102` is open and stays the product owner's.
- **Clamping, keystroke masking, correction on blur, act conversion.** `ADR-0111` §§1, 3, 5.

## Tests

`ActionBar.test.tsx`, `describe("the action bar")` — four added to the twenty-six it has, so the
file reports **30**. All four use the file's existing `bar()` helper and reach the field the way a
player does, through its accessible name; `ADR-0100` §5 holds, so no `data-testid`, no test-only
prop and no exported setter.

| Test | Proves |
| --- | --- |
| `offers a text field holding the dialled total, with no bound of its own` | with the default turn the bar has exactly **one** `input` (the replacement for the deleted line 141 assertion), reachable as `the total`, whose value is `1,200` — the server's own floor. It is `type="text"` with `inputMode="numeric"` and carries **no** `min`, `max`, `step` or `data-testid`. Pressing `pot` puts `3,650` in it and pressing `min` puts `1,200` back — **two** presets, so neither the field's content nor the preset's arithmetic can be a constant. After an action is sent the field is `disabled` like every other control |
| `sends the exact total the player typed` | typing `3000` and pressing `Raise to` sends one `Act` whose action is `{ type: "Raise", seat: 0, to: 3000 }`; a fresh bar typed `5,000` — the table's own grouping — sends `to: 5000`. Two values and two spellings, so neither a constant nor a rejected grouping survives |
| `refuses an entry under the server's minimum, sends nothing, and says which bound` | typing `500` and pressing `Raise to` calls `send` **zero** times, leaves the field's value exactly `"500"` — asserted as a string equality, not a number — puts `500 is under the minimum of 1,200.` on the bar, and leaves the action buttons **not** disabled: no sent-lock was taken, so the bar is still the player's |
| `refuses an entry that is not an amount, and a fold still folds` | typing `1o0` and pressing `Raise to` calls `send` zero times and puts `That is not an amount.` on the bar; then pressing `Fold` on the same bar sends exactly one `Act` carrying `{ type: "Fold", seat: 0 }`. The second half is the guard on the guard: `FOLD`, `CHECK`, `CALL` and `ALL_IN` carry no total, so the refusal may not reach them |

The twenty-six merged tests assert the buttons, the presets, the sent-lock, the remount and the
server's own notices. Exactly one of their assertions moves — line 141, named and replaced above —
and none is weakened.

## Acceptance criteria

- [ ] `src/table/ActionBar.test.tsx` reports `Tests  30 passed (30)`
- [ ] `the action bar.offers a text field holding the dialled total, with no bound of its own` passes
- [ ] `the action bar.sends the exact total the player typed` passes
- [ ] `the action bar.refuses an entry under the server's minimum, sends nothing, and says which
      bound` passes
- [ ] `the action bar.refuses an entry that is not an amount, and a fold still folds` passes
- [ ] `src/table/bar-no-derivation.test.tsx` still reports `Tests  3 passed (3)` — the field
      carries no `min` and no `max`
- [ ] `src/table/null-view.test.tsx` still reports `Tests  6 passed (6)`, `src/e2e/whole-duel.test.tsx`
      still `8`, `src/e2e/drive-duel.test.tsx` still `3`, and `src/lobby/Lobby.test.tsx` still `80`
- [ ] `ActionBar.tsx` mentions `readTypedAmount`, carries exactly one `aria-label="the total"`,
      still has exactly two `Math.floor`, and contains no `Math.min`, `Math.max`, `clamp`,
      `data-testid` or `type="range"`
- [ ] `ActionBar.test.tsx` contains no `querySelector("input")` and exactly one
      `querySelectorAll("input")`
- [ ] `web-client/src/e2e/drive-duel.tsx` contains no `fireEvent.change`, no `getByLabelText` and
      no `.value =`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
