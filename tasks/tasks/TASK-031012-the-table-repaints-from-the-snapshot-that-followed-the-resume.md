---
schema: 2
id: TASK-031012
title: The table repaints from the snapshot that followed the resume
type: task
status: backlog
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, store, ui, reconnect]
depends_on: [TASK-031011]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +314 passed \(314\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'repaints from the snapshot that followed the resume, not the one it held'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the duel that ended while the socket was down'
  - cd web-client && npm run check
---

## Goal

What the player sees after a reconnect is the state the server sent on arrival — including a hand,
or a whole duel, that moved on while the socket was down.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/reconnect.test.tsx` | modify — two tests added, none changed |
| `web-client/src/lobby/Lobby.tsx` | read — the screen under test |
| `web-client/src/table/view-fixture.ts` | read — `aView` |
| `web-client/src/store/duel-provider.tsx` | read — `DuelProvider` |
| `web-client/src/table/PotStrip.tsx` | read — the text asserted |

## Scope

- Two tests in the file `TASK-031011` created, using the same `reconnectingClient` helper and the
  same virtual time. The helper, the three tests before them, and every other file are untouched.
- Render the real screen: `render(<DuelProvider store={client.store} send={client.send}><Lobby /></DuelProvider>)`,
  then drive the sockets inside `act(...)` so the store's notification reaches React.
- No production code changes. This ticket asserts that the parts already shipped compose: the
  reducer replaces `view` wholesale on a `Snapshot` (`TASK-030403`), the lobby renders `state.view`
  (`TASK-030617`) and the result ahead of it (`TASK-030809`), and the resume delivers a fresh
  `Snapshot` or a `DuelFinished` (`TASK-020810`).

## Out of scope

- Anything shown *because* the socket was down — a banner, a spinner, a "reconnecting" line. The
  wire carries nothing about it and this story invents nothing.
- Anything shown to the player whose **opponent** is away. `ADR-0028` settles what that is and
  `ADR-0045` puts it in `STORY-0313`, on frames `EPIC-02`'s `STORY-0214` has yet to ship; no frame
  on today's wire carries it.
- Rematch across a reconnect — `STORY-0309`, on `ADR-0044`'s `STORY-0213`.

## Tests

`web-client/src/store/reconnect.test.tsx`, same describe block `"a tab whose socket dropped"`.

| Test | Proves |
| --- | --- |
| `repaints from the snapshot that followed the resume, not the one it held` | socket 1 delivers `Snapshot { view: aView() }` — pot 30 — and the screen shows `Pot 30`; the socket closes; socket 2 resumes and delivers `Snapshot { view: aView({ pot: 260, handNumber: 4 }) }`; the screen now shows `Pot 260` and `Pot 30` is gone. **Two distinct pots**: a table wired to a constant, or one that merged rather than replaced, fails on the second half |
| `shows the duel that ended while the socket was down` | socket 1 delivers a `Snapshot`, the socket closes, and the resume answers with `DuelFinished` and no snapshot at all — the case `resumeFrames` produces for a room that finished while this seat was away. The region *the result* is on screen and `Pot 30` is gone |

```tsx
it("shows the duel that ended while the socket was down", () => {
  const { sockets, client } = reconnectingClient("ABCDEFGH");
  renderDuelScreen(client);

  act(() => {
    sockets[0].open();
    sockets[0].receive(WELCOME);
    sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
    sockets[0].receive(JSON.stringify({ type: "Snapshot", view: aView() }));
    sockets[0].close();
  });

  act(() => {
    vi.advanceTimersByTime(250);
    sockets[1].open();
    sockets[1].receive(WELCOME);
    sockets[1].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
    sockets[1].receive(
      '{"type":"DuelFinished","outcome":{"winner":0,"handsPlayed":9,"finalStacks":[1000,0]}}',
    );
  });

  expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
  expect(screen.queryByText("Pot 30")).toBeNull();
});
```

Two tests added. Three hundred and twelve exist, so the suite reports **314**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 314 passed (314)` | two ran, the three before them still do, and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks — `aView` returns a full `PlayerView` |

**Name the edit that makes each assertion red:**

1. In `duel-state.ts`, make the `Snapshot` case keep what it has — `view: state.view ?? message.view`
   → `repaints from the snapshot that followed the resume, not the one it held` fails with `Pot 30`
   still on screen. Revert.
2. In `Lobby.tsx`, move the `outcome` branch below the `view` branch → `shows the duel that ended
   while the socket was down` fails: the table from before the drop is still up. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `a tab whose socket dropped > repaints from the snapshot that followed the resume, not the one it held` passes
- [ ] `a tab whose socket dropped > shows the duel that ended while the socket was down` passes
- [ ] The three tests `TASK-031011` added are byte-identical, and no assertion in them is weakened
- [ ] No file outside `web-client/src/store/reconnect.test.tsx` differs from what it was — this
      ticket changes no production code
- [ ] `reconnect.test.tsx` still contains no `await new Promise` and no bare `WebSocket`
- [ ] `npm run --silent test` reports `Tests  314 passed (314)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
