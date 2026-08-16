---
schema: 2
id: TASK-030809
title: The duel screen shows the result when the duel ends
type: task
status: done
parent: STORY-0308
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, lobby]
depends_on: [TASK-030808]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +275 passed \(275\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the result when the duel finishes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the result over the table it replaces'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps waiting through every frame that neither seats a table nor ends the duel'
  - cd web-client && npm run check
---

## Goal

A `DuelFinished` reaches the player: the screen becomes the result panel, and the table and the bar
it replaces are gone.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one branch |
| `web-client/src/lobby/Lobby.test.tsx` | modify — **one existing test is narrowed and renamed**, two added |
| `web-client/src/result/DuelResult.tsx` | read — the props |

## Scope

- `import { DuelResult } from "../result/DuelResult";` joins the imports, in the order
  `npm run format` leaves them.
- One branch, **above** the `state.view` branch:

  ```tsx
  // The duel is over. This comes first because the reducer clears nothing a
  // frame established: `view` and `roomCode` both outlive the duel, so a result
  // branch placed after either is a branch that never runs.
  if (state.outcome !== null) {
    return <DuelResult outcome={state.outcome} mySeat={state.mySeat} />;
  }
  ```

- Nothing else in `Lobby.tsx` moves: the `view` branch keeps its comment and its position above
  `roomCode`, no hook is added, and the screen still sends only from event handlers (`ADR-0032`).

## This ticket owns the assertion its change unsettles

`Lobby.test.tsx`'s `keeps waiting through every frame that is not a Snapshot` walks every
`ServerMessage` variant and asserts the waiting panel survives each. One of the seven frames it
walks is

```tsx
      {
        type: "DuelFinished",
        outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
      },
```

and that is now false by design: a finished duel leaves the waiting panel for the result. It is also
the frame **second from last** in the array, so once it is applied the assertion fails for the
`Failure` frame after it too — the store keeps `outcome`, and the panel does not come back.

The edit is exactly three things:

1. **Delete that one array entry**, leaving the other six frames and their comment untouched.
2. **Rename the test** to `keeps waiting through every frame that neither seats a table nor ends the
   duel`, and extend its comment with one sentence naming `DuelFinished` as the second exception and
   pointing at the two tests below that cover it.
3. Nothing else. The other fifteen `it` blocks, both fixtures, `seatView`, `SNAPSHOT` and
   `ROOM_JOINED` are byte-identical, and **no assertion is weakened**: the loop keeps
   `getByText("Waiting for your rival")` for every frame it still walks, and the frame it drops gains
   two assertions of its own rather than none.

The claim the test makes stays universal over the frames it names — it is the *set* that shrinks by
one, and the one that leaves is enumerated again immediately below, by name, with the behaviour it
actually has now.

## Out of scope

- The reducer. `TASK-030406` already records the outcome verbatim and clears the pending turn; this
  ticket adds no field and changes no `duel-state.ts`.
- Clearing anything on the way out. `TASK-030807`'s link reloads the page, which is how this client
  returns to an empty store.
- Rematch — `STORY-0309`, blocked on `DEC-023`.
- Moving the duel screen out of `Lobby.tsx`. There is still one screen component and no router.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`.

| Test | Proves |
| --- | --- |
| `shows the result when the duel finishes` | after a `RoomJoined` seating this client at **1** and a `DuelFinished` naming **seat 1** the winner, the region *the result* is on screen, the heading reads `Victory`, and the waiting panel heading is gone. Seat 1 on purpose: a screen passing a literal `mySeat={0}` reads the same frame as a defeat |
| `puts the result over the table it replaces` | after `ROOM_JOINED`, `SNAPSHOT` **and** `DuelFinished`, the region *the result* is on screen while the region *your move* and the text `Pot 30` are both absent — the branch is ahead of the live `view`, not behind it |
| `keeps waiting through every frame that neither seats a table nor ends the duel` | the six remaining variants each leave the waiting panel standing |

```tsx
it("puts the result over the table it replaces", () => {
  const store = createDuelStore();
  store.apply(ROOM_JOINED);
  renderLobby(store);

  act(() => {
    store.apply(SNAPSHOT);
    store.apply({
      type: "DuelFinished",
      outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] },
    });
  });

  expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
  expect(screen.queryByRole("region", { name: "your move" })).toBeNull();
  expect(screen.queryByText("Pot 30")).toBeNull();
});
```

Two tests added, one renamed. Two hundred and seventy-three exist, so the suite reports **275**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 275 passed (275)` | the two ran, the renamed one still does, and the rest are untouched |
| the three `--reporter=verbose` greps | both new names and the renamed one exist |
| `npm run check` | typechecks — `state.outcome` narrows and `mySeat` is `number \| null` on both sides |

**Name the edit that makes each assertion red:**

1. Move the `outcome` branch below the `view` branch → `puts the result over the table it replaces` fails: the table is still up. Revert.
2. Pass `mySeat={0}` as a literal instead of `state.mySeat` → `shows the result when the duel finishes` fails, because that client sat at seat 1 and the panel calls its victory a defeat. Revert.

Quote both in the PR, and say in the PR body that the `view` branch still precedes the `roomCode`
branch.

## Acceptance criteria

- [ ] `the lobby > shows the result when the duel finishes` passes
- [ ] `the lobby > puts the result over the table it replaces` passes
- [ ] `the lobby > keeps waiting through every frame that neither seats a table nor ends the duel` passes
- [ ] In `Lobby.tsx`, `if (state.outcome !== null)` appears **before** `if (state.view !== null)`, which appears before `if (state.roomCode !== null)`
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`
- [ ] In `Lobby.test.tsx`, the only pre-existing lines that differ are the renamed `it`, its comment, and the deleted `DuelFinished` entry
- [ ] `npm run --silent test` reports `Tests  275 passed (275)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
