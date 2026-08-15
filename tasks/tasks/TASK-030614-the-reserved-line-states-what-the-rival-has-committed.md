---
schema: 2
id: TASK-030614
title: The reserved line states what the rival has committed this street
type: task
status: done
parent: STORY-0306
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030613]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +184 passed \(184\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states what your rival has committed on this street'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the committed line empty and present when nothing is out'
  - cd web-client && npm run check
---

## Goal

The last `SeatView` field the story names: `committedThisStreet`, in the design's reserved line
under the rival — present in every state, so nothing below it ever moves.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify — one element in the rival's block, one local component |
| `web-client/src/table/DuelTable.test.tsx` | modify — append two `it` blocks |
| `design/screens/duel-table.html` | read — the `.bet-line` rule. **Read only: never edit anything under `design/`** |

## Scope

- `import { formatChips } from "./chips";` joins the imports, and
  `<BetLine committed={rival.committedThisStreet} />` becomes the last child of the rival's block,
  under the hand row.
- The local component goes at the bottom of the file, below `DuelTable`:

  ```tsx
  /**
   * The chips a seat has out on this street. The word is the field's, not an
   * action's: the view says how much is committed and never says whether it got
   * there by a blind, a call, a bet or a raise. The line keeps its height when
   * there is nothing to say, so nothing below it moves.
   */
  function BetLine(props: { committed: number }): ReactElement {
    return (
      <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-faint">
        {props.committed > 0 && (
          <>
            committed{" "}
            <span className="font-mono text-text tabular-nums">
              {formatChips(props.committed)}
            </span>
          </>
        )}
      </p>
    );
  }
  ```

- **The word is "committed", not the design's "bets".** `committedThisStreet` says how much is out;
  it does not say whether a bet, a raise, a call or a posted blind put it there, and writing "bets"
  would be the client naming an action the server never sent. Say so in the PR — it is a one-word,
  deliberate divergence from `duel-table.html`, and everything else about the line is the design's.
- The `<p>` is always rendered; only its contents are conditional. Its `min-h` is the design's
  reserved height, `calc(var(--pd-fs-small) * var(--pd-lh-body))`, referencing the tokens directly
  because the theme exposes no utility for a computed line box.
- One line, under the rival only. The design reserves it in the `.opp` block and nowhere else; a
  second line under your own seat is a design change, and per `STORY-0306` a value the design system
  lacks is an `EPIC-06` ticket, not a client one.

## Out of scope

- `ImKate mucks` / `ImKate folds`, which the states screen writes in this same slot. Both need to
  know how a hand ended, which no `PlayerView` field says — `STORY-0308`.
- `committedThisHand`. It is in the view, nothing on the design's table shows it, and the pot
  already states where those chips went.

## Tests

`web-client/src/table/DuelTable.test.tsx`, describe block `"the duel table"`. Two `it` blocks
appended after `TASK-030613`'s three, which are not edited.

| Test | Proves |
| --- | --- |
| `states what your rival has committed on this street` | with the rival's `committedThisStreet: 400`, `getByText(/committed/)` resolves and `getByText("400")` resolves |
| `leaves the committed line empty and present when nothing is out` | with the fixture's default view, `container.querySelector("p")` is not `null` and its `textContent` is `""` |

Two tests. One hundred and eighty-two exist, so the suite reports **184**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 184 passed (184)` | the two ran and the hundred-and-eighty-two before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Return `null` from `BetLine` when `committed === 0` → `leaves the committed line empty and
   present when nothing is out` fails with `expected null not to be null`. That is the reserved
   height going away, and everything under it shifting on the first bet. Revert.
2. Pass `rival.committedThisHand` instead → `states what your rival has committed on this street`
   fails with `Unable to find an element with the text: /committed/`. Revert.
3. Pass your own seat's commitment — `view.seats.at(0)?.committedThisStreet ?? 0` → the same
   failure. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel table > states what your rival has committed on this street` passes
- [ ] `the duel table > leaves the committed line empty and present when nothing is out` passes
- [ ] `TASK-030612`'s and `TASK-030613`'s seven `it` blocks are unedited and their assertions are
      byte-identical
- [ ] `DuelTable.tsx` contains exactly one `<BetLine` element
- [ ] `npm run --silent test` reports `Tests  184 passed (184)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
