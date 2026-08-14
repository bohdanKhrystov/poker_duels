---
schema: 2
id: TASK-030611
title: The seat plate shows the name, the button and the stack
type: task
status: backlog
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030610]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +175 passed \(175\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the button only on the seat that has it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the status the seat is in on the plate'
  - cd web-client && npm run check
---

## Goal

The design's seat plate: a name, the reserved status line, the dealer badge when this seat has the
button, and the stack in mono figures.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | create |
| `web-client/src/table/SeatPlate.test.tsx` | create |
| `web-client/src/table/seat-status.ts` | read — `seatStatus` |
| `web-client/src/table/view-fixture.ts` | read — `aSeat` |
| `design/screens/duel-table.html` | read — the `.seat`, `.seat .status`, `.dealer` rules. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim. Class strings are already in `prettier-plugin-tailwindcss`'s order;
  run `npm run format` and expect no diff:

  ```tsx
  import type { ReactElement } from "react";
  import type { SeatView } from "../protocol";
  import { formatChips } from "./chips";
  import { seatStatus } from "./seat-status";

  /** A seat plate: who it is, what it is doing, the button, and the stack. */
  export function SeatPlate(props: {
    name: string;
    seat: SeatView;
    hasButton: boolean;
    isToAct: boolean;
    isViewer: boolean;
  }): ReactElement {
    const status = seatStatus(props.seat, props.isToAct, props.isViewer);
    const onTurn = status === "Your turn" || status === "Their turn";
    return (
      <div
        className={`flex items-center gap-4 rounded-medium border border-l-2 border-hairline bg-surface px-5 py-4 ${
          onTurn ? "border-l-accent" : "border-l-transparent"
        }`}
      >
        <span className="min-w-0 flex-1">
          <span className="block truncate font-medium">{props.name}</span>
          <span
            className={`mt-1 block min-h-[1.5em] text-micro leading-body ${
              onTurn
                ? "font-medium tracking-caps text-accent uppercase"
                : "text-text-faint"
            }`}
          >
            {status}
          </span>
        </span>
        {props.hasButton && (
          <span
            aria-label="the button"
            className="rounded-pill border border-hairline px-3 py-1 font-mono text-micro text-text-muted"
          >
            D
          </span>
        )}
        <span className="font-mono tabular-nums">
          {formatChips(props.seat.stack)}
        </span>
      </div>
    );
  }
  ```

- `min-h-[1.5em]` is the design's reserved status height: the line exists in every state so the
  plate never changes height when a status appears.
- The badge is a `D` with `aria-label="the button"` — the letter is the design's, the label is what
  makes it findable and speakable.
- `onTurn` drives the accent left border and the uppercase treatment, exactly the design's
  `.seat.on-turn` and `.status.turn`.
- The `name` is a prop, not a lookup: no `PlayerView` field carries a display name, and
  `STORY-0311` states plainly that no opponent name is rendered until `ADR-0021` and `DEC-017` land.
  `TASK-030612` passes `"You"` and `"Your rival"`.

## Out of scope

- Deciding which seat is which, who has the button, or who is to act. All three are the table's
  (`TASK-030612`); this component is handed booleans.
- A display name, an avatar, a coin count or a rating.
- The action bar beneath the hero plate. `STORY-0307` owns it live and off together — see that
  story and `TASK-030612`'s Out of scope for why its off state is not built here.

## Tests

`web-client/src/table/SeatPlate.test.tsx`, describe block `"a seat plate"`. A local helper builds
the component so each test names only what it bends:

```tsx
function plate(
  overrides: Parameters<typeof aSeat>[0] = {},
  props: { hasButton?: boolean; isToAct?: boolean; isViewer?: boolean } = {},
) {
  return render(
    <SeatPlate
      name="You"
      seat={aSeat(overrides)}
      hasButton={props.hasButton ?? false}
      isToAct={props.isToAct ?? false}
      isViewer={props.isViewer ?? true}
    />,
  );
}
```

| Test | Proves |
| --- | --- |
| `writes the name and the stack the view carries` | `plate({ stack: 13400 })` shows `You` and `13,400` |
| `shows the button only on the seat that has it` | with `hasButton: true`, `getByLabelText("the button")` is found; after `unmount()`, with `hasButton: false` it is `null` |
| `puts the status the seat is in on the plate` | `plate({ hasFolded: true })` shows `Folded` |

Three tests. One hundred and seventy-two exist, so the suite reports **175**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 175 passed (175)` | the three ran and the hundred-and-seventy-two before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, colour-literal guard |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Draw the badge unconditionally — `{true && (` → `shows the button only on the seat that has it`
   fails with `expected <span …(2)></span> to be null`. Revert.
2. Interpolate `{props.seat.stack}` instead of `formatChips(...)` → `writes the name and the stack
   the view carries` fails with `Unable to find an element with the text: 13,400`. Revert.
3. Print the seat index instead of the name — `` {`Seat ${props.seat.index}`} `` → the same test
   fails with `Unable to find an element with the text: You`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a seat plate > writes the name and the stack the view carries` passes
- [ ] `a seat plate > shows the button only on the seat that has it` passes
- [ ] `a seat plate > puts the status the seat is in on the plate` passes
- [ ] `SeatPlate.tsx` contains no `holeCards` and no `seatToAct`
- [ ] `npm run --silent test` reports `Tests  175 passed (175)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
