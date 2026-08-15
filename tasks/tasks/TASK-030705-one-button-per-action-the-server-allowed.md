---
schema: 2
id: TASK-030705
title: One button per action the server allowed, and not one more
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030704]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +210 passed \(210\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders one button per action the server allowed, in the order it sent them'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders no button for an action the server withheld'
  - cd web-client && npm run check
---

## Goal

On your turn the bar draws `legalActions.allowed` and nothing else: one button per action, in the
order the server sent them, with the aggressive line filled the way the design fills it.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify — the live branch and three helpers |
| `web-client/src/table/ActionBar.test.tsx` | modify — four tests added, none changed |
| `web-client/src/table/action-text.ts` | read — `actionText` |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn`, `aLegalActions` |
| `design/components/action-bar.html` | read — `.actions`, `.btn`, `.btn.ghost`, `.btn.fill`, `.amt`. **Read only: never edit anything under `design/`** |

## Scope

- The imports gain `LegalActions` and `ActionType` from `../protocol`, and the two local modules:

  ```tsx
  import type { ActionType, ClientMessage, LegalActions } from "../protocol";
  import { actionText } from "./action-text";
  import { formatChips } from "./chips";
  ```

- The one line `{turn === null && <Waiting />}` becomes the branch:

  ```tsx
  {turn === null ? <Waiting /> : <Live turn={turn} send={props.send} />}
  ```

- `Live`, and the two helpers under it, are new:

  ```tsx
  /**
   * Your turn. One button per action the server allowed, in the order it sent
   * them, and an amount control only when a bet or a raise is on offer.
   */
  function Live(props: {
    turn: PendingTurn;
    send: (message: ClientMessage) => void;
  }): ReactElement {
    const actions = props.turn.legalActions;
    // 0 when no amount is on offer: it reaches no button, because only Bet and
    // Raise print a total, and neither is allowed then.
    const to = amountFloor(actions) ?? 0;
    const filled = filledAction(actions.allowed);

    return (
      <div className="flex gap-3">
        {actions.allowed.map((type) => {
          const text = actionText(type, actions, to);
          return (
            <button
              className={`flex-1 rounded-medium border px-3 py-4 leading-tight font-medium ${
                type === filled
                  ? "border-transparent bg-accent-fill text-on-accent"
                  : "border-hairline text-text"
              }`}
              key={type}
              type="button"
            >
              {text.verb}
              {text.amount !== null && (
                <>
                  {" "}
                  <span className="font-mono tabular-nums">
                    {formatChips(text.amount)}
                  </span>
                </>
              )}
            </button>
          );
        })}
      </div>
    );
  }

  /**
   * The total the amount control starts at: the server's own minimum for whichever
   * of a bet or a raise it allowed, or `null` when it allowed neither.
   */
  function amountFloor(actions: LegalActions): number | null {
    if (actions.allowed.includes("RAISE")) return actions.minRaiseTo;
    if (actions.allowed.includes("BET")) return actions.minBetTo;
    return null;
  }

  /**
   * The one filled button — the design's aggressive line. The last button the
   * server named carries it when neither a bet nor a raise is on offer, which is
   * the only case where the aggressive line is the all-in.
   */
  function filledAction(allowed: readonly ActionType[]): ActionType | undefined {
    if (allowed.includes("RAISE")) return "RAISE";
    if (allowed.includes("BET")) return "BET";
    return allowed[allowed.length - 1];
  }
  ```

- **The list rendered is `actions.allowed`.** Not a client-side table of the six `ActionType`s
  filtered by it, and not a re-sort: the server sent an order and the bar draws that order.
- The amount sits in its own `<span>` so the button's accessible name reads `Call 400` — the
  design's `.amt`, and what every later test queries by.
- `npm run format` is the arbiter of Tailwind class order; take its output if it moves one.

## Out of scope

- The amount control that makes `to` movable — `TASK-030706`. Until then every bet or raise button
  shows the server's minimum, which is exactly what a fresh turn should show.
- Clicking. No `onClick` here: `TASK-030707` adds it together with the disabled state, so that no
  intermediate commit can send a frame it cannot stop sending twice.
- `ALL_IN` as a sizing chip rather than a button. The engine offers `ALL_IN` whenever the opponent
  is contestable (`BettingRules`), so it is an action in almost every legal set, and the story
  forbids hiding one. The design's three-button mocks predate that count; the row is `flex`, so a
  fourth button narrows the row and changes nothing else.

## Tests

`web-client/src/table/ActionBar.test.tsx`, describe block `"the action bar"`. Four tests are added.
Neither existing test changes, and the helper does not move.

| Test | Proves |
| --- | --- |
| `renders one button per action the server allowed, in the order it sent them` | `getAllByRole("button").map((b) => b.textContent)` is exactly `["Fold", "Call 400", "Raise to 1,200", "All in 13,400"]` — four buttons, that order, those figures |
| `renders no button for an action the server withheld` | with `allowed: ["FOLD", "CALL", "RAISE"]`, `queryByRole("button", { name: "Check" })` is `null` and there are three buttons |
| `fills the raise and leaves the other buttons ghosts` | the raise button's `className` contains `bg-accent-fill`; the fold button's does not |
| `fills the last button when the server offers no bet and no raise` | with `allowed: ["FOLD", "CALL", "ALL_IN"]`, `All in 13,400` carries `bg-accent-fill` |

Four tests. Two hundred and six exist, so the suite reports **210**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 210 passed (210)` | the four ran and the two hundred and six before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, colour-literal guard |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Render the six `ActionType`s instead of `actions.allowed` → `renders one button per action the
   server allowed, in the order it sent them` fails with `expected [ 'Fold', 'Check', 'Call 400',
   …(3) ] to deeply equal [ 'Fold', 'Call 400', …(2) ]`, and `renders no button for an action the
   server withheld` fails with `expected <button …(2)></button> to be null`. Revert.
2. Return `"ALL_IN"` first from `filledAction` → `fills the raise and leaves the other buttons
   ghosts` fails on `bg-accent-fill`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the action bar > renders one button per action the server allowed, in the order it sent them` passes
- [ ] `the action bar > renders no button for an action the server withheld` passes
- [ ] `the action bar > fills the raise and leaves the other buttons ghosts` passes
- [ ] `the action bar > fills the last button when the server offers no bet and no raise` passes
- [ ] Both `TASK-030704` tests still pass, unchanged
- [ ] `ActionBar.tsx` contains no literal list of `ActionType`s and no `.sort(`
- [ ] `npm run --silent test` reports `Tests  210 passed (210)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
