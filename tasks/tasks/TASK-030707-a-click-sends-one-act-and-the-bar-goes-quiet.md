---
schema: 2
id: TASK-030707
title: A click sends one Act, and the bar goes quiet until the next turn
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +219 passed \(219\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "sends one Act carrying the turn's identity"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing more once an action is sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'comes back to life on the next turn, at the new minimum'
  - cd web-client && npm run check
---

## Goal

Clicking a button sends exactly one `Act` — the turn's identity, the server's seat, the control's
total — and then every control in the bar is dead until the server opens a new turn. Nothing on
screen moves in the meantime.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify — the sent lock, the handler, the key |
| `web-client/src/table/ActionBar.test.tsx` | modify — five tests added, none changed |
| `web-client/src/table/act-frame.ts` | read — `actFrame` |
| `design/components/action-bar.html` | read — the `.bar.disabled` state. **Read only: never edit anything under `design/`** |

## Scope

- `import { actFrame } from "./act-frame";` joins the imports.
- `Live` gains one more piece of state, below the amount:

  ```tsx
  const [sent, setSent] = useState(false);
  ```

- The slider and every button gain `disabled={sent}`, and the button's class string gains the
  design's disabled treatment as its last three classes:

  ```tsx
  } disabled:border-hairline disabled:bg-transparent disabled:text-text-faint`}
  disabled={sent}
  key={type}
  onClick={() => {
    setSent(true);
    props.send(actFrame(props.turn, type, to));
  }}
  type="button"
  ```

- **The `Live` element is keyed by the turn's identity**, which is the whole reset mechanism:

  ```tsx
  {turn === null ? (
    <Waiting />
  ) : (
    // Keyed by the turn's identity, so a new decision point mounts a fresh
    // bar: the amount returns to the server's minimum and the sent lock
    // lifts, by construction rather than by an effect that clears them.
    <Live
      key={`${turn.handNumber}:${turn.actionSequence}`}
      turn={turn}
      send={props.send}
    />
  )}
  ```

- Nothing is applied optimistically: the handler sets one boolean and sends one frame. No chip
  moves, no stack changes, no "you called" text — a `Snapshot` says all of that, or it did not
  happen.
- Still no `useEffect` and no `useRef`. The lock lifts because React unmounts the old `Live`, not
  because anything clears it.

## Out of scope

- Anything the store does with the frames that come back. `Snapshot`, `Rejected` and `DuelFinished`
  each already clear the pending turn (`STORY-0304`), and the bar simply follows.
- Retrying, queueing or de-duplicating on the socket. One click, one frame, no memory.
- Coming back to life after a `Rejected` — the server sends no fresh `YourTurn` after one, so the
  pending turn is gone and this bar correctly shows nothing. That is `DEC-037` and `TASK-030712`.

## Tests

`web-client/src/table/ActionBar.test.tsx`, describe block `"the action bar"`. Five tests are added.
No existing test changes.

| Test | Proves |
| --- | --- |
| `sends one Act carrying the turn's identity` | clicking `Fold` calls `send` once, with `{ type: "Act", handNumber: 14, actionSequence: 27, action: { type: "Fold", seat: 0 } }` |
| `sends the total the amount control holds` | after moving the slider to `3250`, clicking `Raise to 3,250` sends `action: { type: "Raise", seat: 0, to: 3250 }` |
| `disables every control once an action is sent` | after one click every button's `disabled` is `true`, and so is the slider's |
| `sends nothing more once an action is sent` | clicking `Fold` then `Call 400` leaves `send` called **once** |
| `comes back to life on the next turn, at the new minimum` | rerendering with `actionSequence: 28` and `minRaiseTo: 2400` puts the slider at `2400`, and a second click sends a second frame |

```tsx
it("comes back to life on the next turn, at the new minimum", () => {
  const send = vi.fn();
  const { rerender } = render(<ActionBar turn={aTurn()} send={send} />);
  fireEvent.click(screen.getByRole("button", { name: "Fold" }));

  rerender(
    <ActionBar
      turn={aTurn({
        actionSequence: 28,
        legalActions: aLegalActions({ minRaiseTo: 2400 }),
      })}
      send={send}
    />,
  );

  expect(
    (screen.getByRole("slider", { name: "raise to" }) as HTMLInputElement).value,
  ).toBe("2400");
  fireEvent.click(screen.getByRole("button", { name: "Fold" }));
  expect(send).toHaveBeenCalledTimes(2);
});
```

Five tests. Two hundred and fourteen exist, so the suite reports **219**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 219 passed (219)` | the five ran and the two hundred and fourteen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats, colour-literal guard |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Drop the `key` from `<Live>` → `comes back to life on the next turn, at the new minimum` fails
   with `expected '1200' to be '2400'`: the old control survives the new turn, still carrying the
   old minimum and still locked. Revert. This is why the key is the mechanism and not a detail.
2. Drop `disabled={sent}` from the buttons → `disables every control once an action is sent` fails
   with `expected false to be true`, and `sends nothing more once an action is sent` fails with
   `expected "spy" to be called once, but got 2 times`. Revert.
3. Send `to - actions.callTo` instead of `to` → `sends the total the amount control holds` fails on
   the frame comparison. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the action bar > sends one Act carrying the turn's identity` passes
- [ ] `the action bar > sends the total the amount control holds` passes
- [ ] `the action bar > disables every control once an action is sent` passes
- [ ] `the action bar > sends nothing more once an action is sent` passes
- [ ] `the action bar > comes back to life on the next turn, at the new minimum` passes
- [ ] All ten earlier `the action bar` tests still pass, unchanged
- [ ] `ActionBar.tsx` contains no `useEffect` and no `useRef`, and `send` is called from an
      `onClick` and nowhere else
- [ ] `npm run --silent test` reports `Tests  219 passed (219)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
