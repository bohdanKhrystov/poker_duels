---
schema: 2
id: TASK-030617
title: The lobby hands the live view to the duel table
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, lobby]
depends_on: [TASK-030616]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +189 passed \(189\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the waiting panel when the first Snapshot arrives'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps waiting through every frame that is not a Snapshot'
  - cd web-client && npm run check
---

## Goal

`TASK-030514`'s placeholder becomes the screen: the branch that read `<p>The duel has begun.</p>`
now renders the table, from the same `state.view`, in the same place in the same order.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one returned element |
| `web-client/src/lobby/Lobby.test.tsx` | modify — **one assertion moves**, see below |
| `web-client/src/table/DuelTable.tsx` | read — the `DuelTable` props |

## Scope

- `import { DuelTable } from "../table/DuelTable";` joins the imports, between the
  `../store/duel-provider` and `./room-link` lines.
- The branch's body, and nothing else about it, changes:

  ```tsx
  // The first Snapshot is how the host learns the guest arrived: seating the
  // guest starts the duel, and there is no "opponent joined" frame to wait for.
  if (state.view !== null) {
    return <DuelTable view={state.view} />;
  }
  ```

- **The branch stays above the `state.roomCode` branch.** `TASK-030514` put it there and wrote down
  why: the reducer never clears `roomCode`, so a `view` branch placed second is unreachable and the
  host waits forever behind a live duel. The comment above it stays, the `!== null` narrowing stays
  (it is what types `state.view` as `PlayerView` rather than `PlayerView | null`), and the order
  stays.
- Nothing else in `Lobby.tsx` moves. No hook is added, no `useEffect`, no `useRef`, and the
  component still sends only from event handlers (`ADR-0032`).

## This ticket owns the one assertion its change invalidates

`Lobby.test.tsx`'s `leaves the waiting panel when the first Snapshot arrives` asserts
`expect(screen.getByText("The duel has begun.")).toBeDefined();` — the text this ticket deletes.
That single line becomes:

```tsx
    expect(screen.getByText("Pot 30")).toBeDefined();
```

`30` is the `pot` in the file's existing `SNAPSHOT` fixture, and `Pot 30` is what `PotStrip` writes
for it. **Nothing else in the file changes**: the same `it` keeps its name, its two lines above
(`getByText("Waiting for your rival")` and the `act(() => store.apply(SNAPSHOT))`), and its
`queryByText("Waiting for your rival")` `toBeNull()` assertion; the other thirteen `it` blocks are
untouched; the `seatView` and `SNAPSHOT` fixtures are untouched; and no assertion is weakened — the
new one is strictly more specific than the old, because it reads a number out of the fixture.

The test count therefore does not move: the suite still reports **189**.

## Out of scope

- Moving the table out of `Lobby.tsx` into a router or an `App`-level switch. There is one screen
  component today and `STORY-0305` put the branch here on purpose; a second screen container is not
  this story's to invent.
- Touching `App.tsx`. The page's background, padding and heading are already `TASK-030208`'s.
- Anything the table does. Every one of its behaviours is already merged and tested by
  `TASK-030601`–`TASK-030616`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. No test is added; one
assertion is rewritten, as set out above.

| Test | Proves |
| --- | --- |
| `leaves the waiting panel when the first Snapshot arrives` | after `ROOM_JOINED` and a render, `Waiting for your rival` is on screen; `act(() => { store.apply(SNAPSHOT); })` then leaves it `null` and puts `Pot 30` on screen — the table, rendering the fixture's pot |
| `keeps waiting through every frame that is not a Snapshot` | **unchanged, from `TASK-030514`** — an `Events` and a `YourTurn` leave the waiting panel up |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 189 passed (189)` | the suite is where `TASK-030616` left it — this ticket adds no test and loses none |
| the two `--reporter=verbose` greps | both `TASK-030514` tests still exist by name |
| `npm run check` | typechecks — `state.view` narrows to `PlayerView` — lints, formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Delete the `if (state.view !== null)` branch → `leaves the waiting panel when the first Snapshot
   arrives` fails with `AssertionError: expected <h2></h2> to be null`. Revert.
2. Move the branch **below** the `state.roomCode` branch → the same failure, `expected <h2></h2> to
   be null`. Revert. This is `TASK-030514`'s third red edit, still red, still the reason the order
   is written down.

Quote both in the PR, and say in the PR body that the branch order is unchanged.

## Acceptance criteria

- [ ] `the lobby > leaves the waiting panel when the first Snapshot arrives` passes
- [ ] `the lobby > keeps waiting through every frame that is not a Snapshot` passes
- [ ] In `Lobby.tsx`, `if (state.view !== null)` appears **before** `if (state.roomCode !== null)`
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`
- [ ] In `Lobby.test.tsx`, exactly one line differs from `TASK-030514`'s file: the assertion named
      above. The other thirteen `it` blocks and both fixtures are byte-identical
- [ ] `npm run --silent test` reports `Tests  189 passed (189)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
