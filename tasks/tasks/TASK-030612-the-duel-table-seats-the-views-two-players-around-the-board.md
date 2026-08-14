---
schema: 2
id: TASK-030612
title: The duel table seats the view's two players around the board
type: task
status: backlog
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030611]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +179 passed \(179\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'seats you and your rival from the view'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'marks the seat to act and no other'
  - cd web-client && npm run check
---

## Goal

The column the design draws: rival above, pot and board between, you below — with `viewerSeat`
deciding which seat is which, `buttonSeat` deciding who has the button, and `seatToAct` deciding
whose turn it is.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | create |
| `web-client/src/table/DuelTable.test.tsx` | create |
| `web-client/src/table/SeatPlate.tsx` | read — the `SeatPlate` props |
| `web-client/src/table/view-fixture.ts` | read — `aView`, `aSeat` |
| `design/screens/duel-table.html` | read — the `.table` and `.center` rules. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim:

  ```tsx
  import type { ReactElement } from "react";
  import type { PlayerView } from "../protocol";
  import { BoardCards } from "./BoardCards";
  import { PotStrip } from "./PotStrip";
  import { SeatPlate } from "./SeatPlate";

  /**
   * The duel table: one column, rival above, board between, you below.
   *
   * Everything on it is read off the `PlayerView` the server computed. Nothing is
   * worked out here — not the pot, not the street, not whose cards these are, and
   * not what anyone may do next.
   */
  export function DuelTable(props: { view: PlayerView }): ReactElement {
    const { view } = props;
    const you = view.seats.find((seat) => seat.index === view.viewerSeat);
    const rival = view.seats.find((seat) => seat.index !== view.viewerSeat);
    return (
      <div className="[container-type:inline-size] mx-auto flex max-w-[560px] flex-col gap-5">
        {rival !== undefined && (
          <SeatPlate
            name="Your rival"
            seat={rival}
            hasButton={view.buttonSeat === rival.index}
            isToAct={view.seatToAct === rival.index}
            isViewer={false}
          />
        )}
        <div className="flex flex-col items-center gap-4">
          <PotStrip view={view} />
          <BoardCards cards={view.board.cards} />
        </div>
        {you !== undefined && (
          <SeatPlate
            name="You"
            seat={you}
            hasButton={view.buttonSeat === you.index}
            isToAct={view.seatToAct === you.index}
            isViewer
          />
        )}
      </div>
    );
  }
  ```

- **Seats are found by index, never by array position.** `viewerSeat` is the only thing that says
  which seat is the player's, and a server that ever sends the seats in the other order must not
  swap the screen round. The two `find` calls are the whole of it.
- Each seat is rendered only when it is there. A view missing a seat draws the rest of the table
  rather than throwing inside render.
- `[container-type:inline-size]` is what makes `BoardCards`' `100cqi` clamp resolve against this
  column rather than the viewport. `max-w-[560px]` is the design's `.table` width; there is no token
  for it, and the design is the source.
- The page's background, padding and heading are `App.tsx`'s already (`TASK-030208`), so the design's
  `min-height: 100dvh` on `.table` is not repeated here.

## Out of scope

- Either hand of cards — `TASK-030613`, which wraps these two plates in the design's `.opp` and
  `.hero` blocks and adds the card rows.
- The reserved bet line — `TASK-030614`.
- **The action bar, live or off.** The design's `.bar.off` keeps five sizing chips and a stepper
  carrying a raise amount, and every one of those figures is something the client would have to work
  out from `LegalActions` — which lives in `state.pendingTurn` and belongs to `STORY-0307`. That
  story adds the bar in both states at once; nothing here reserves space for it.
- Reading the store. `DuelTable` takes a `view` prop; `TASK-030617` is the only ticket that touches
  `useDuelState`.

## Tests

`web-client/src/table/DuelTable.test.tsx`, describe block `"the duel table"`. One helper, which the
next two tickets also use:

```tsx
/** The seat plate whose name is `name`, so a stack can be pinned to a seat. */
function plateFor(name: string): HTMLElement {
  const plate = screen.getByText(name).closest("div");
  if (plate === null) throw new Error(`no seat plate named ${name}`);
  return plate;
}
```

| Test | Proves |
| --- | --- |
| `seats you and your rival from the view's viewerSeat` | with `viewerSeat: 1` and stacks `4150` (seat 0) and `13400` (seat 1), `within(plateFor("You")).getByText("13,400")` and `within(plateFor("Your rival")).getByText("4,150")` both resolve |
| `gives the button to the seat the view names` | with `viewerSeat: 0, buttonSeat: 1`, the rival's plate has `the button` and yours does not |
| `marks the seat to act and no other` | with `viewerSeat: 0, seatToAct: 1`, `Their turn` is on screen and `queryByText("Your turn")` is `null` |
| `shows the pot and the board the view carries` | `pot: 2450` with a three-card board shows `Pot 2,450`, `ace of spades` and `turn card, not yet dealt` |

Four tests. One hundred and seventy-five exist, so the suite reports **179**.

**`within(plateFor(...))` and not a bare `getByText`.** Asserting only that `"You"`, `"Your rival"`,
`"4,150"` and `"13,400"` are each somewhere on screen was tried, and it stays green when the seats
are swapped — every string is still present, just on the wrong plate. Measured, not reasoned about.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 179 passed (179)` | the four ran and the hundred-and-seventy-five before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Take the seats in array order — `const you = view.seats.at(0); const rival = view.seats.at(1);`
   → `seats you and your rival from the view's viewerSeat` fails with `Unable to find an element
   with the text: 13,400` (inside your plate). Revert.
2. Put the button on seat nought — `hasButton={rival.index === 0}` and `hasButton={you.index === 0}`
   → `gives the button to the seat the view names` fails with `Unable to find a label with the text
   of: the button`. Revert.
3. Make the viewer always the actor — `isToAct={false}` for the rival and `isToAct={true}` for you
   → `marks the seat to act and no other` fails with `Unable to find an element with the text:
   Their turn`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel table > seats you and your rival from the view's viewerSeat` passes
- [ ] `the duel table > gives the button to the seat the view names` passes
- [ ] `the duel table > marks the seat to act and no other` passes
- [ ] `the duel table > shows the pot and the board the view carries` passes
- [ ] `DuelTable.tsx` contains no `useDuelState`, no `useSend`, and no `<button` tag
- [ ] `npm run --silent test` reports `Tests  179 passed (179)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
