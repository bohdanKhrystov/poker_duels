---
schema: 2
id: TASK-030711
title: The duel screen puts the bar under the table and sends what it built
type: task
status: done
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, lobby]
depends_on: [TASK-030710]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +232 passed \(232\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the action bar under the table'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the Act the bar built from the pending turn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the waiting panel when the first Snapshot arrives'
  - cd web-client && npm run check
---

## Goal

The duel screen becomes the table **and** the bar: the store's `pendingTurn`, `rejection` and
`refusal` reach the bar, and the frame the bar builds goes out through the same `send` the lobby
already holds.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one returned element |
| `web-client/src/lobby/Lobby.test.tsx` | modify — **one assertion is tightened**, two tests added |
| `web-client/src/table/ActionBar.tsx` | read — the `ActionBar` props |

## Scope

- `import { ActionBar } from "../table/ActionBar";` joins the imports, above the `DuelTable` line
  (alphabetical, as `npm run format` leaves them).
- The branch's body, and nothing else about it, changes:

  ```tsx
  if (state.view !== null) {
    return (
      <div className="mx-auto flex max-w-[560px] flex-col gap-5">
        <DuelTable view={state.view} />
        <ActionBar
          turn={state.pendingTurn}
          rejection={state.rejection}
          refusal={state.refusal}
          send={send}
        />
      </div>
    );
  }
  ```

- **The branch stays above the `state.roomCode` branch**, with its comment, for the reason
  `TASK-030514` wrote down and `TASK-030617` re-proved: the reducer never clears `roomCode`, so a
  `view` branch placed second is unreachable and the host waits forever behind a live duel.
- The wrapper repeats the table's own column width so the bar sits under it rather than beside it;
  the bar carries its own `max-w-[460px]`, which is the design's.
- Nothing else in `Lobby.tsx` moves. No hook is added, no `useEffect`, no `useRef`, and the screen
  still sends only from event handlers (`ADR-0032`) — the bar's `onClick` is one.

## This ticket owns the assertion its change unsettles

`Lobby.test.tsx`'s `leaves the waiting panel when the first Snapshot arrives` asserts

```tsx
    expect(screen.queryByText("Waiting for your rival")).toBeNull();
```

and that line becomes

```tsx
    expect(
      screen.queryByRole("heading", { name: "Waiting for your rival" }),
    ).toBeNull();
```

The bar now puts the words *Waiting for your rival* on the same screen, in its off state. The old
assertion still passes — testing-library reads an element's **direct** text children, so the bar's
one-text-node line `Waiting for your rival…` does not match the string exactly — but it passes by a
margin that a later ticket splitting the ellipsis into a span would silently spend. The heading
query says what the assertion always meant: *the lobby's waiting panel is gone*. **Nothing else in
the file changes**: the same `it` keeps its name, its `getByText` above, its `act(...)` and its
`Pot 30` assertion; the other fifteen `it` blocks and both fixtures are untouched; and no assertion
is weakened — the new one names an element the old one only happened to find.

## Out of scope

- Moving the duel screen out of `Lobby.tsx`. There is one screen component today and `STORY-0305`
  put the branch here on purpose; a router is not this story's to invent.
- Anything the bar does. Every one of its behaviours is merged and tested by `TASK-030704`–
  `TASK-030710`.
- Giving `DuelTable` the turn. The table renders a `PlayerView` and derives nothing from it; the
  bar's figures come from `LegalActions`, which is not in the view, and putting the bar inside the
  table would drag them through `no-derivation.test.tsx`'s fixture for no gain.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two tests are added and one
assertion is tightened, as set out above.

| Test | Proves |
| --- | --- |
| `puts the action bar under the table` | after `ROOM_JOINED` and `SNAPSHOT`, `getByRole("region", { name: "your move" })` is found and the bar is in its off state — a snapshot alone opens no turn |
| `sends the Act the bar built from the pending turn` | applying `SNAPSHOT` then a `YourTurn` with `handNumber: 4`, `actionSequence: 9`, `allowed: ["FOLD", "CALL"]` and clicking `Fold` calls the provider's `send` with `{ type: "Act", handNumber: 4, actionSequence: 9, action: { type: "Fold", seat: 0 } }` — the store's turn, through the bar, out of the screen |
| `leaves the waiting panel when the first Snapshot arrives` | unchanged in meaning; the panel is queried as a heading |

```tsx
it("sends the Act the bar built from the pending turn", () => {
  const store = createDuelStore();
  store.apply(ROOM_JOINED);
  const { send } = renderLobby(store);

  act(() => {
    store.apply(SNAPSHOT);
    store.apply({
      type: "YourTurn",
      handNumber: 4,
      actionSequence: 9,
      legalActions: {
        seat: 0,
        allowed: ["FOLD", "CALL"],
        callTo: 400,
        minBetTo: 0,
        minRaiseTo: 0,
        allInTo: 500,
      },
    });
  });
  fireEvent.click(screen.getByRole("button", { name: "Fold" }));

  expect(send).toHaveBeenCalledWith({
    type: "Act",
    handNumber: 4,
    actionSequence: 9,
    action: { type: "Fold", seat: 0 },
  });
});
```

Two tests. Two hundred and thirty exist, so the suite reports **232**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 232 passed (232)` | the two ran and the two hundred and thirty before them still do |
| the three `--reporter=verbose` greps | the two new ones exist, and `TASK-030514`'s still does |
| `npm run check` | typechecks — `state.view` narrows, and every `ActionBar` prop is supplied |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Pass `turn={null}` instead of `state.pendingTurn` → `sends the Act the bar built from the pending
   turn` fails with `Unable to find an element with the role "button" and name "Fold"`. Revert.
2. Move the `state.view` branch below the `state.roomCode` branch → `leaves the waiting panel when
   the first Snapshot arrives` fails with `expected <h2></h2> to be null`. Revert. That is
   `TASK-030514`'s third red edit, still red, and still the reason the order is written down.

Quote both in the PR, and say in the PR body that the branch order is unchanged.

## Acceptance criteria

- [ ] `the lobby > puts the action bar under the table` passes
- [ ] `the lobby > sends the Act the bar built from the pending turn` passes
- [ ] `the lobby > leaves the waiting panel when the first Snapshot arrives` passes
- [ ] In `Lobby.tsx`, `if (state.view !== null)` appears **before** `if (state.roomCode !== null)`
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`
- [ ] In `Lobby.test.tsx`, exactly one pre-existing line differs from `TASK-030617`'s file: the
      assertion named above. The other fifteen `it` blocks and both fixtures are byte-identical
- [ ] `npm run --silent test` reports `Tests  232 passed (232)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
