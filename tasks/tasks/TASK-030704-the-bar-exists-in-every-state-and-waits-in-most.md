---
schema: 2
id: TASK-030704
title: The bar exists in every state, and waits in most of them
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030703]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +206 passed \(206\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names itself and waits when there is no turn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no control when there is no turn'
  - cd web-client && npm run check
---

## Goal

The bar itself: the design's surface, its reserved sizing row, and the line that says who is being
waited on. With no turn pending it offers nothing — no button exists to be clicked, so no frame can
leave.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | create |
| `web-client/src/table/ActionBar.test.tsx` | create |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn` |
| `design/components/action-bar.html` | read — `.bar`, `.sizing`, `.waiting`, and the `.off` state. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```tsx
  import type { ReactElement } from "react";
  import type { ClientMessage } from "../protocol";
  import type { PendingTurn } from "../store/duel-state";

  /**
   * The action bar: the one place a player asserts anything.
   *
   * It offers exactly the actions the server named in `YourTurn` and no others —
   * it hides none it thinks bad, adds none it thinks legal, and works out no
   * amount the server did not send. Nothing it does is optimistic: a click sends
   * one `Act` and the bar goes quiet until the server's next frame moves the
   * store on.
   */
  export function ActionBar(props: {
    turn: PendingTurn | null;
    send: (message: ClientMessage) => void;
  }): ReactElement {
    const { turn } = props;
    return (
      <section
        aria-label="your move"
        className="mx-auto flex w-full max-w-[460px] flex-col gap-3 rounded-medium border border-hairline bg-surface p-4"
      >
        {turn === null && <Waiting />}
      </section>
    );
  }

  /**
   * Not your turn: the sizing row's height is reserved and the actions row says
   * who is being waited on, so the bar is the same height in every state and
   * nothing below it moves when a turn opens.
   */
  function Waiting(): ReactElement {
    return (
      <>
        <div className="min-h-7" />
        <p className="py-4 text-center leading-tight text-text-muted">
          {"Waiting for your rival…"}
        </p>
      </>
    );
  }
  ```

- **`send` is declared now and first used in `TASK-030707`.** Fixing the whole contract here is what
  stops three later tickets rewriting the test helper. Destructure only `turn`, as above:
  `const { turn, send } = props` would be an unused local and fails `tsc`.
- The waiting line is **one text node**, ellipsis included. The design puts the dots in a child
  span; here they must not be, because testing-library reads an element's *direct* text children
  only, so `Waiting for your rival` in a child-split line would collide with the lobby's
  `<h2>Waiting for your rival</h2>` in `Lobby.test.tsx`. Measured, not reasoned about: the split
  form makes `queryByText("Waiting for your rival")` return the bar's line instead of `null`.
- `min-h-7` is the design's `min-height: var(--pd-space-7)` on the sizing row, and it compiles to
  `.min-h-7{min-height:var(--spacing-7)}` — checked in the built CSS, not assumed.

## Out of scope

- The live bar. Buttons are `TASK-030705`, the amount control `TASK-030706`, sending
  `TASK-030707`.
- The rejection and refusal line — `TASK-030709`, which adds the two props it needs.
- Naming the rival. No `PlayerView` field carries a name (`STORY-0311`), so the design's
  `Waiting for ImKate` becomes `Waiting for your rival…`, in the vocabulary `TASK-030611` set.
- Rendering the bar anywhere. `TASK-030711` puts it on the screen.

## Tests

`web-client/src/table/ActionBar.test.tsx`, describe block `"the action bar"`. A local helper builds
the component so each test names only what it bends:

```tsx
function bar(props: Partial<Parameters<typeof ActionBar>[0]> = {}) {
  const send = vi.fn();
  const rendered = render(
    <ActionBar
      turn={props.turn === undefined ? aTurn() : props.turn}
      send={props.send ?? send}
    />,
  );
  return { ...rendered, send };
}
```

`props.turn === undefined` rather than `??`: `null` is a turn state this component is *for*, and
`??` would silently replace it with a live turn.

| Test | Proves |
| --- | --- |
| `names itself and waits when there is no turn` | `getByRole("region", { name: "your move" })` is found, and so is the text `Waiting for your rival…` |
| `offers no control when there is no turn` | `queryAllByRole("button")` is `[]` and `queryByRole("slider")` is `null` — nothing to click, so `send` cannot be reached |

The slider half of the second test is trivially true until `TASK-030706` adds one; it is written
now because the off state is precisely the state it must go on being true in.

Two tests. Two hundred and four exist, so the suite reports **206**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 206 passed (206)` | the two ran and the two hundred and four before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, colour-literal guard — every value is a token class |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Split the ellipsis into `Waiting for your rival<span>…</span>` → `names itself and waits when
   there is no turn` fails with `Unable to find an element with the text: Waiting for your rival…`.
   Revert. This is the collision the scope bullet exists for.
2. Drop `aria-label="your move"` → the same test fails on the `region` query. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the action bar > names itself and waits when there is no turn` passes
- [ ] `the action bar > offers no control when there is no turn` passes
- [ ] `ActionBar.tsx` contains no `useEffect` and no `useRef`
- [ ] `ActionBar.tsx` contains no colour literal — `npm run check` proves it
- [ ] `npm run --silent test` reports `Tests  206 passed (206)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
