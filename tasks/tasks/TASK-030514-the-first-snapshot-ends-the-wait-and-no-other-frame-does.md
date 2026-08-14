---
schema: 2
id: TASK-030514
title: The first Snapshot ends the wait, and no other frame does
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby, rooms]
depends_on: [TASK-030513]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +128 passed \(128\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the waiting panel when the first Snapshot arrives'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps waiting through every frame that is not a Snapshot'
  - cd web-client && npm run check
---

## Goal

The story's fifth acceptance criterion: the host learns the guest arrived by receiving the first
`Snapshot` — not from a lobby message, which the protocol does not have and this story must not
ask for.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `PlayerView`, `SeatView`, `Board` field lists |

## Scope

- One early return in `Lobby`, **above** the `state.roomCode` branch and below the hooks:

  ```tsx
  // The first Snapshot is how the host learns the guest arrived: seating the
  // guest starts the duel, and there is no "opponent joined" frame to wait for.
  if (state.view !== null) {
    return <p>The duel has begun.</p>;
  }
  ```

- **The order matters and is the point.** `state.roomCode` is still set when the duel starts — the
  reducer never clears it — so a `view` branch placed second would never be reached and the host
  would wait forever with a live duel behind the panel.
- The placeholder is one line of text on purpose. The table is `STORY-0306`, which replaces this
  return with the real screen; nothing here reads `state.view`'s contents.
- `state.pendingTurn`, `state.narration` and `state.mySeat` are not consulted. `Snapshot` is the
  frame the server sends after every transition, and `view` is the field it writes.

## Out of scope

- Rendering any part of the table, the board or a stack — `STORY-0306`.
- An "opponent joined" frame. Adding one is a protocol change and a version bump, and the story
  forbids it in as many words.
- A different screen for the guest. Both seats reach the duel by the same `Snapshot`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two `it` blocks appended
after `TASK-030513`'s; nothing already there is edited.

`act` joins the `@testing-library/react` import — both tests apply their frames **after** render,
because the transition is what is under test. Two fixtures are added above the helpers, typed
against the wire so `tsc` proves they are complete (`HandStarted` needs four more fields than it
looks like it does; use `ActionOn` as below):

```tsx
function seatView(index: number): SeatView {
  return {
    index,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
  };
}

const SNAPSHOT: ServerMessage = {
  type: "Snapshot",
  view: {
    viewerSeat: 0,
    handNumber: 1,
    buttonSeat: 0,
    street: "PREFLOP",
    board: { cards: [] },
    pot: 30,
    betToMatch: 20,
    minRaiseTo: 40,
    seatToAct: 0,
    smallBlind: 10,
    bigBlind: 20,
    seats: [seatView(0), seatView(1)],
  },
};
```

with `import type { SeatView, ServerMessage } from "../protocol";`.

| Test | Proves |
| --- | --- |
| `leaves the waiting panel when the first Snapshot arrives` | after `ROOM_JOINED` and a render, `"Waiting for your rival"` is on screen; `act(() => { store.apply(SNAPSHOT); })` then leaves it `null` and `"The duel has begun."` findable |
| `keeps waiting through every frame that is not a Snapshot` | after `ROOM_JOINED` and a render, applying an `Events` carrying `{ type: "ActionOn", sequence: 1, seat: 0 }` and a `YourTurn` inside one `act` leaves `"Waiting for your rival"` still on screen |

Two tests. One hundred and twenty-six exist, so the suite reports **128**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 128 passed (128)` | the two ran and the hundred-and-twenty-six before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — the fixtures must satisfy `PlayerView` and `SeatView` exactly — lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Branch on `state.narration.length > 0` instead of `state.view !== null` → `keeps waiting
   through every frame that is not a Snapshot` fails with `Unable to find an element with the
   text: Waiting for your rival`, and `leaves the waiting panel when the first Snapshot arrives`
   fails with `expected <h2></h2> to be null` (a `Snapshot` adds no narration). Revert.
2. Delete the branch → `leaves the waiting panel when the first Snapshot arrives` fails with
   `expected <h2></h2> to be null`. Revert.
3. Move the branch **below** the `state.roomCode` branch → the same failure, `expected <h2></h2>
   to be null`. Revert.

Quote all three in the PR. The third is why the order is written down.

## Acceptance criteria

- [ ] `the lobby > leaves the waiting panel when the first Snapshot arrives` passes
- [ ] `the lobby > keeps waiting through every frame that is not a Snapshot` passes
- [ ] `npm run --silent test` reports `Tests  128 passed (128)`
- [ ] The twelve `it` blocks from `TASK-030510` through `TASK-030513` are unedited, and their
      assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
