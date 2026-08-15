---
schema: 2
id: TASK-030607
title: The board is five places wide, whatever the street
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030606]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +158 passed \(158\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reserves five places before a card is dealt'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'draws exactly five places on every street'
  - cd web-client && npm run check
---

## Goal

The community row: five places, always all five, dealt cards face up and the rest as the design's
dashed outlines — so the board's width never changes between streets.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/BoardCards.tsx` | create |
| `web-client/src/table/BoardCards.test.tsx` | create |
| `web-client/src/table/PlayingCard.tsx` | read — `CardFace`, `CardSlot` |
| `design/screens/duel-table.html` | read — the `.board` rule and the slot labels. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim:

  ```tsx
  import type { ReactElement } from "react";
  import { CardFace, CardSlot } from "./PlayingCard";

  // Five places, named by where they sit and not by the street: a place is what
  // the row reserves, and which street the hand is on is `view.street`'s to say.
  const PLACES = [
    "first flop card",
    "second flop card",
    "third flop card",
    "turn card",
    "river card",
  ] as const;

  /**
   * The community cards: five places, every one of them always drawn.
   *
   * A place the server has dealt shows its card; a place it has not shows the
   * design's dashed outline, so the row's width never changes between streets.
   */
  export function BoardCards(props: { cards: readonly string[] }): ReactElement {
    return (
      <div className="flex gap-3 [--w:clamp(48px,calc((100cqi-64px)/5),72px)]">
        {PLACES.map((place, index) => {
          const card = props.cards.at(index);
          return card === undefined ? (
            <CardSlot key={place} label={`${place}, not yet dealt`} />
          ) : (
            <CardFace key={place} card={card} />
          );
        })}
      </div>
    );
  }
  ```

- `--w` is set once on the row and inherited by every card in it — the design writes the same clamp
  as `--bw` on `.board` and then `style="--w:var(--bw)"` on each card. `100cqi` resolves against the
  table column's `container-type: inline-size`, which `TASK-030612` sets.
- `gap-3` is `--spacing-3` → `--pd-space-3` (8px), the design's `.board` gap.
- The last two labels are the design's exactly: `turn card, not yet dealt` and
  `river card, not yet dealt`. The first three are named by position because three identical
  accessible names would be three identical announcements.
- `.at(index)`, not `[index]`, for the reason `TASK-030606` gave.

## Out of scope

- `view.street`. This component never receives it and must not infer it. The street is written in
  the pot strip (`TASK-030609`), read straight off the view.
- Burn cards, a sixth place, or a run-it-twice second board.

## Tests

`web-client/src/table/BoardCards.test.tsx`, describe block `"the board"`.

| Test | Proves |
| --- | --- |
| `reserves five places before a card is dealt` | `[]` renders five `role="img"` elements, including `turn card, not yet dealt` and `river card, not yet dealt` |
| `deals the flop into the first three places` | `["As", "7d", "2c"]` names all three faces and still offers both undealt slots |
| `still draws five places on the river` | five cards render five images and `queryByRole("img", { name: /not yet dealt/ })` is `null` |
| `draws exactly five places on every street` | for card counts 0, 3, 4 and 5, the row element's `childElementCount` is `5` |

Four tests. One hundred and fifty-four exist, so the suite reports **158**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 158 passed (158)` | the four ran and the hundred-and-fifty-four before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Draw only what was dealt: `{PLACES.slice(0, props.cards.length).map(...)` → three tests fail,
   the first with `Unable to find an accessible element with the role "img"` and `draws exactly
   five places on every street` with `expected +0 to be 5 // Object.is equality`. Revert.
2. Name the slots from how many cards arrived — `` label={`${props.cards.length < 3 ? "flop" :
   "next"} card, not yet dealt`} `` → `reserves five places before a card is dealt` and `deals the
   flop into the first three places` both fail with `Unable to find an accessible element with the
   role "img" and name "turn card, not yet dealt"`. This is the "infer the street from the card
   count" bug wearing a label. Revert.
3. Return `null` for an undealt place instead of a `CardSlot` → the same three failures as edit 1.
   Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the board > reserves five places before a card is dealt` passes
- [ ] `the board > deals the flop into the first three places` passes
- [ ] `the board > still draws five places on the river` passes
- [ ] `the board > draws exactly five places on every street` passes
- [ ] `BoardCards`' props type is exactly `{ cards: readonly string[] }` — it is handed no street
      and no view, so it cannot read one, and `props.cards.length` appears nowhere in the file
- [ ] `npm run --silent test` reports `Tests  158 passed (158)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
