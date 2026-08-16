---
schema: 2
id: TASK-031110
title: The lobby shows the strip, and a duel in progress does not
type: task
status: ready
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, profile, lobby]
depends_on: [TASK-031109]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +356 passed \(356\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the profile strip under the way into a duel'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the strip off the screen once a table is on it'
  - cd web-client && npm run check
---

## Goal

The strip is on the first screen a player sees, fed by one read wired at boot — and it is gone the
moment a duel is on the table.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one hook call, one conditional render |
| `web-client/src/lobby/Lobby.test.tsx` | modify — one helper and two tests added, nothing changed |
| `web-client/src/main.tsx` | modify — the provider and the browser-bound read |
| `web-client/src/profile/profile-provider.tsx` | read — `ProfileProvider`, `useProfileStrip` |
| `web-client/src/profile/profile-strip.ts` | read — `readProfileStrip`, `ProfileStripState` |

## Scope

- `Lobby.tsx`: `const profile = useProfileStrip();` beside `useDuelState()`, at the **top of the
  component, before every early return** — `eslint-plugin-react-hooks` fails the build on a hook
  under a branch, and the result, table and waiting branches all return early.
- The strip renders in the **last** branch only, the one with the create button and the code box:
  `{profile !== null && <ProfileStrip state={profile} />}`. A duel in progress is not the place for
  a ledger, and the result screen already states the coin that duel moved (`TASK-030803`).
- `main.tsx` wraps the tree in `<ProfileProvider read={readProfile}>`, where `readProfile` is a
  **module-scope constant**, not an inline arrow:

  ```tsx
  // Module scope, so the provider's effect sees one stable reference and one
  // mount means one read. An arrow written inline in the JSX would be a new
  // function on every render.
  const readProfile = (): Promise<ProfileStripState> =>
    readProfileStrip({
      fetch: (path, init) => window.fetch(path, init),
      storage: localStorage,
    });
  ```

  This is the one place in the client that names `window.fetch`, exactly as it is the one place that
  names `localStorage` and the real socket. `main.tsx` stays composition-only — `ADR-0032`'s rule —
  and nothing else moves into it.
- `bootDuelClient` is untouched: one connection per tab, wired where it already is.
  `one-connection.test.ts` keeps passing without an edit, and if it does not, the wiring went in the
  wrong place.

## Out of scope

- `App.tsx`. The strip belongs to the lobby, not to every screen, and `App.test.tsx` must stay
  byte-identical — it calls `getByRole("heading")`, which throws the moment a second heading appears
  anywhere in that tree.
- Re-reading when a duel ends and the lobby comes back. The way back to the lobby is a page load
  (`TASK-030807`), which mounts the provider again.
- Any change to what the other lobby branches render.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, in the existing describe block `"the lobby"`, with one new
helper beside `renderLobby` — which is **not** modified, so all eighteen existing tests render
exactly what they render today:

```tsx
function renderLobbyWithProfile(
  state: ProfileStripState,
  store: DuelStore = createDuelStore(),
): void {
  const read = (): Promise<ProfileStripState> => Promise.resolve(state);
  render(
    <ProfileProvider read={read}>
      <DuelProvider store={store} send={vi.fn()}>
        <Lobby />
      </DuelProvider>
    </ProfileProvider>,
  );
}
```

| Test | Proves |
| --- | --- |
| `shows the profile strip under the way into a duel` | with a profile whose balance is `3`, `await screen.findByLabelText("your profile")` is there and `3` is on screen — and the create button is still there, so the strip was added rather than substituted |
| `keeps the strip off the screen once a table is on it` | the same provider, over a store that has taken `ROOM_JOINED` and `SNAPSHOT`: the table is on screen (`Pot 30`) and `queryByLabelText("your profile")` is `null` |

Resolve with `findBy*`. No timer, in either test — `virtual-time.test.ts` fails the build on a test
file that names one without installing fake ones.

Two tests added. Three hundred and fifty-four exist, so the suite reports **356**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 356 passed (356)` | two ran, and the eighteen tests already in this file, the three in `App.test.tsx` and both whole-surface guards still pass |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks, lints (`react-hooks` included), is formatted |

**Name the edit that makes each assertion red** — run each, quote both in the PR, revert:

1. Render the strip in the `state.view !== null` branch as well → `keeps the strip off the screen
   once a table is on it` fails.
2. Move the `useProfileStrip()` call below the first early return → `npm run check` fails on the
   hooks rule, before any test runs.
3. Pass `read={() => readProfileStrip(…)}` inline in `main.tsx` → nothing fails, because `main.tsx`
   is outside the test net. That is exactly why `TASK-031109` pinned the dependency list and why the
   constant is required here; say in the PR that you checked it by hand.

## Acceptance criteria

- [ ] `the lobby > shows the profile strip under the way into a duel` passes
- [ ] `the lobby > keeps the strip off the screen once a table is on it` passes
- [ ] `renderLobby` is unchanged, and no existing test in `Lobby.test.tsx` is renamed or has an
      assertion changed
- [ ] `web-client/src/App.tsx` and `web-client/src/App.test.tsx` are byte-identical to what they were
- [ ] `web-client/src/store/boot.ts` is byte-identical, and `one-connection.test.ts` passes unedited
- [ ] `main.tsx` gains the provider and the module-scope `readProfile` constant, and nothing else
- [ ] `npm run --silent test` reports `Tests  356 passed (356)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
