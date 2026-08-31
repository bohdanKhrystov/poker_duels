---
schema: 2
id: TASK-121109
title: The lobby hands the duel table the hand's events
type: task
status: done
parent: STORY-1211
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: [TASK-121101]
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the table states who took the pot of the hand that just ended"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the statement goes when the next hand is dealt"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The award banner `TASK-121101` built is on a player's screen: `Lobby` passes `state.narration` to
`DuelTable`, so a real duel driven by real server frames states who took the pot when a hand ends.

## Why this is its own ticket

`TASK-121101` builds and gates the banner at the table — `PotStrip` prints the line, `DuelTable`
carries the value down, and `DuelTable.test.tsx` pins every string. Three files, which is the cap.
The remaining hop is the store-to-screen one, and it is here rather than there for a plain reason:
the chain is `Lobby` → `DuelTable` → `PotStrip`, that is three source files plus a test file, and no
merged gate refuses to let it land in two commits — an optional prop compiles, lints and tests green
at every intermediate — so `atomic:` would be a false claim and the split is the honest shape
(`ADR-0068` §4).

**Until this merges the defect round 3 filed is still on the screen.** `TASK-121101` alone changes
nothing a player sees.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `tasks/tasks/TASK-121101-the-table-says-who-won-the-hand-it-just-finished.md` | read |

## Scope

- **One prop.** The single `<DuelTable …/>` in `Lobby.tsx` — the branch guarded by
  `if (state.view !== null)` — gains `narration={state.narration}` beside the `view` and
  `rivalPresence` it already passes. Prettier will break the tag across lines; let it.
- **Nothing else.** No selection, no formatting, no branch and no new state: the window and the
  three lines are `PotStrip`'s, decided by
  [`ADR-0095`](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
  §§1–2 and built by `TASK-121101`.
- **Two tests appended to `Lobby.test.tsx`**, using the `renderLobby(store)` helper and
  `createDuelStore()` that file already has. They drive the real store with real `ServerMessage`
  frames, which is the one thing a table-level fixture cannot do.

## Out of scope

- **`web-client/src/table/no-derivation.test.tsx`** — byte-unchanged, `ADR-0095` §5. Nothing here
  goes near it.
- **Anything about the banner's wording, window or trigger.** If a string looks wrong, that is
  `TASK-121101`'s diff and `ADR-0095` §2, not this one.
- **A reload between hands.** A client that never received the award shows the plain `Pot N` line,
  by decision (`ADR-0095` §4). No store field and no `PlayerView` field is added here either.

## Tests

`Lobby.test.tsx`, two cases. Both build the store before rendering, so no `act()` is needed:

```tsx
const store = createDuelStore();
store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 0 });
store.apply({ type: "Events", events: [
  { type: "HandStarted", sequence: 0, handNumber: 1, buttonSeat: 0,
    smallBlind: 25, bigBlind: 50, stacks: [1500, 1500] },
  { type: "PotAwarded", sequence: 9, seat: 0, amount: 4850 },
]});
```

| Test | Proves |
| --- | --- |
| `the table states who took the pot of the hand that just ended` | with the store above and `store.apply({ type: "Snapshot", view: aView({ viewerSeat: 0, handNumber: 1, street: "COMPLETE", pot: 0 }) })`, `renderLobby(store)` then `screen.getByText("You win 4,850")`. Fails today and fails on any tree where `Lobby` does not pass `narration` |
| `the statement goes when the next hand is dealt` | the same store, then `HandStarted` for hand 2 and a `Snapshot` at `aView({ viewerSeat: 0, handNumber: 2, street: "PREFLOP", pot: 75 })`. `getByText(/Pot 75/)` and `expect(screen.queryByText(/You win/)).toBeNull()` — narration is cumulative, so hand 1's award is still in the store and must not be restated over hand 2 |

Neither test asserts that a node exists or that a prop was passed: each pins a rendered string, and
the second pins the **absence** of one under a store that still holds the event that produced it.

## Acceptance criteria

- [ ] `Lobby.test.tsx` — `the table states who took the pot of the hand that just ended` passes
- [ ] `Lobby.test.tsx` — `the statement goes when the next hand is dealt` passes
- [ ] The diff contains exactly two files, and neither is under `web-client/src/table/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
