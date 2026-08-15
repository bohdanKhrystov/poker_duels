---
schema: 2
id: TASK-030706
title: The amount control is clamped to the bounds the server sent
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030705]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +214 passed \(214\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clamps the amount control to the bounds the server sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no amount control when neither a bet nor a raise is allowed'
  - cd web-client && npm run check
---

## Goal

A bet or a raise gets a control that runs from the server's minimum to the server's all-in and
cannot leave that range, and the button beside it says the total it currently holds.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify — `to` becomes state, and the sizing row appears |
| `web-client/src/table/ActionBar.test.tsx` | modify — four tests added, none changed |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn`, `aLegalActions` |
| `design/components/action-bar.html` | read — `.sizing`, `.stepper`, and the `min-height` note. **Read only: never edit anything under `design/`** |

## Scope

- `useState` joins the React import: `import { useState, type ReactElement } from "react";`
- In `Live`, the constant becomes state and the floor is kept:

  ```tsx
  const actions = props.turn.legalActions;
  const floor = amountFloor(actions);
  // 0 when no amount is on offer: it reaches no frame, because only Bet and
  // Raise carry a total, and neither is allowed here.
  const [to, setTo] = useState(floor ?? 0);
  const filled = filledAction(actions.allowed);
  ```

- The sizing row goes **above** the actions row, inside the same fragment — so `Live` returns a
  fragment wrapping the two rows rather than the bare `<div className="flex gap-3">`:

  ```tsx
  <div className="flex min-h-7 items-center gap-3">
    {floor !== null && (
      <>
        <input
          aria-label={
            actions.allowed.includes("RAISE") ? "raise to" : "bet to"
          }
          className="flex-1"
          max={actions.allInTo}
          min={floor}
          onChange={(event) => setTo(Number(event.target.value))}
          step={1}
          type="range"
          value={to}
        />
        <span className="font-mono tabular-nums">{formatChips(to)}</span>
      </>
    )}
  </div>
  ```

- **Every bound is the server's.** `min` is `minRaiseTo` when a raise is allowed and `minBetTo` when
  a bet is — `amountFloor` already decides that — and `max` is `allInTo`. Nothing here reads a
  stack, a blind or a pot; clamping a control to bounds the server sent is presentation, and
  working those bounds out would be a rule.
- `step={1}` because chips are whole. No other increment is on the wire, and choosing one — a big
  blind, a min-raise increment — would be the client inventing a raising rule. **This is the
  recorded deviation from the design**, whose `.stepper` has `−`/`+` buttons and therefore needs an
  increment nothing sends; a range input reaches both endpoints exactly and needs none.
- The row keeps `min-h-7` whether or not it holds a control, which is the design's reserved sizing
  row: the bar is the same height in every state.

## Out of scope

- The design's five sizing chips (`min`, `⅓`, `½`, `pot`, `all-in`). Three of them are pot
  fractions: the client would have to work a bet size out of the pot, which is a rule, and the story
  puts bet presets out of scope as a product choice nobody has made. The two that are not — `min`
  and `all-in` — are the slider's own endpoints, so they would add a second way to reach a value the
  control already reaches exactly.
- Remembering an amount across turns. `TASK-030707` makes a new turn a new control, deliberately.
- Sending. Still no `onClick` — `TASK-030707`.

## Tests

`web-client/src/table/ActionBar.test.tsx`, describe block `"the action bar"`. Four tests are added.
No existing test changes: `renders one button per action the server allowed, in the order it sent
them` still reads `Raise to 1,200`, because the control starts at the server's minimum.

| Test | Proves |
| --- | --- |
| `clamps the amount control to the bounds the server sent` | the slider's `min` attribute is `"1200"` and its `max` is `"13400"` — `minRaiseTo` and `allInTo`, not a stack or a pot |
| `starts the amount at the server's minimum for the action it allowed` | facing a bet the slider's value is `1200` (`minRaiseTo`) and its name is `raise to`; with `allowed: ["CHECK", "BET", "ALL_IN"]` it is `175` (`minBetTo`) and its name is `bet to` |
| `offers no amount control when neither a bet nor a raise is allowed` | with `allowed: ["FOLD", "CALL", "ALL_IN"]`, `queryByRole("slider")` is `null` |
| `writes the raise button's total from the amount control` | after `fireEvent.change(slider, { target: { value: "3250" } })`, `getByRole("button", { name: "Raise to 3,250" })` is found |

```tsx
it("clamps the amount control to the bounds the server sent", () => {
  bar();
  const slider = screen.getByRole("slider", { name: "raise to" });
  expect(slider.getAttribute("min")).toBe("1200");
  expect(slider.getAttribute("max")).toBe("13400");
});
```

Four tests. Two hundred and ten exist, so the suite reports **214**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 214 passed (214)` | the four ran and the two hundred and ten before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, colour-literal guard |

**Name the edit that makes each assertion red:**

1. `min={actions.callTo}` → `clamps the amount control to the bounds the server sent` fails with
   `expected '400' to be '1200'`. Revert.
2. Render the slider unconditionally, dropping the `floor !== null` guard → `offers no amount
   control when neither a bet nor a raise is allowed` fails, `expected <input …> to be null`.
   Revert.
3. Start the state at `actions.allInTo` → `starts the amount at the server's minimum for the action
   it allowed` fails with `expected '13400' to be '1200'`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the action bar > clamps the amount control to the bounds the server sent` passes
- [ ] `the action bar > starts the amount at the server's minimum for the action it allowed` passes
- [ ] `the action bar > offers no amount control when neither a bet nor a raise is allowed` passes
- [ ] `the action bar > writes the raise button's total from the amount control` passes
- [ ] All six earlier `the action bar` tests still pass, unchanged
- [ ] `ActionBar.tsx` names no `pot`, no `stack`, no `bigBlind` and no `betToMatch`
- [ ] `npm run --silent test` reports `Tests  214 passed (214)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
