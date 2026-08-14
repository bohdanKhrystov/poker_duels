---
schema: 2
id: TASK-030515
title: main.tsx boots the client once and renders the lobby under the provider
type: task
status: ready
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [client, lobby]
depends_on: [TASK-030514]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +129 passed \(129\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the lobby beneath the heading'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the application heading'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives the heading a token-derived class'
  - cd web-client && npm run build
  - cd web-client && npm run check
---

## Goal

The composition root: one `bootDuelClient` per tab, before rendering, with the room code the URL
carried — and the lobby on screen under the provider, inside the `StrictMode` that stays.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |
| `web-client/src/store/boot.ts` | read — `bootDuelClient`, `BootOptions` |

## Scope

- `web-client/src/main.tsx` becomes exactly this:

  ```tsx
  import "./styles/app.css";

  import React from "react";
  import ReactDOM from "react-dom/client";
  import { App } from "./App";
  import { roomCodeFromSearch } from "./lobby/room-link";
  import { connectToDuelServer } from "./protocol";
  import { bootDuelClient } from "./store/boot";
  import { DuelProvider } from "./store/duel-provider";

  const container = document.getElementById("root");
  if (!container) throw new Error("missing #root");

  // One boot per tab, outside the tree (ADR-0032): StrictMode below may mount and
  // unmount as often as it likes without opening a socket or sending a frame.
  const client = bootDuelClient({
    connect: connectToDuelServer,
    joinRoomCode: roomCodeFromSearch(window.location.search),
  });

  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <DuelProvider store={client.store} send={client.send}>
        <App />
      </DuelProvider>
    </React.StrictMode>,
  );
  ```

  `connectToDuelServer` is passed **by reference**, never called here — which is also why this file
  never writes the token `WebSocket`, and why `src/protocol/boundary.ts` stays green.
- `App.tsx` gains `import { Lobby } from "./lobby/Lobby";` and renders `<Lobby />` under the
  existing `<h1>`. Its `<main>` classes are unchanged.
- `<React.StrictMode>` stays. Removing it to quiet a double mount would throw away the only thing
  in development that catches impure renders, and `ADR-0032` exists so it costs nothing.
- `main.tsx` stays composition-only: no branching beyond the existing `#root` check, no state, no
  handler. Logic that drifts here escapes the test net.

## Out of scope

- A test for `main.tsx` itself. It touches `document` and opens a real socket at import time and
  is outside the net by design — `TASK-030516` guards structurally what can be guarded.
- Removing `?room=` from the address bar after joining.
- Any change to `src/protocol/`. `STORY-0303` is `done` and this story does not reopen it.

## Tests

`web-client/src/App.test.tsx`. **This ticket owns that file's existing assertions.** `App` now
calls `useDuelState` through `Lobby`, so a bare `render(<App />)` throws — both existing tests must
render under a provider. Introduce one helper and have all three tests call it:

```tsx
function renderApp(): void {
  render(
    <DuelProvider store={createDuelStore()} send={vi.fn()}>
      <App />
    </DuelProvider>,
  );
}
```

`vi` joins the `vitest` import; `DuelProvider` and `createDuelStore` are new imports.

| Test | Proves |
| --- | --- |
| `renders the application heading` | unchanged assertion: the only heading's `textContent` is `"Poker Duels"` — the lobby's entry screen has no heading of its own, so the query still finds exactly one |
| `gives the heading a token-derived class` | unchanged assertion: that heading's classes contain `text-title` |
| `renders the lobby beneath the heading` | new: `screen.getByRole("button", { name: "Create a duel room" })` is findable, so `App` really mounts `Lobby` |

One new test. One hundred and twenty-eight exist, so the suite reports **129**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 129 passed (129)` | the new test ran and the hundred-and-twenty-eight before it still do |
| the three `--reporter=verbose` greps | the new test exists and both old ones survived by name |
| `npm run build` | `main.tsx` compiles as a real Vite entry point, which no test exercises |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Delete `<Lobby />` (and its import) from `App.tsx` → `renders the lobby beneath the heading`
   fails with `Unable to find an accessible element with the role "button" and name "Create a duel
   room"`. Revert.
2. Change `renderApp` back to a bare `render(<App />)` → all three App tests fail with `Error:
   Uncaught [Error: useDuelState and useSend need a DuelProvider above them]`. Revert.

Quote both in the PR. There is no red edit for `main.tsx`: nothing imports it, which is precisely
why `boot.ts` exists as its own file and why `TASK-030516` follows.

## Acceptance criteria

- [ ] `App > renders the application heading` passes, with its assertion byte-identical to
      `develop` — only the render call moved into `renderApp`
- [ ] `App > gives the heading a token-derived class` passes, with its assertion byte-identical
- [ ] `App > renders the lobby beneath the heading` passes
- [ ] `npm run --silent test` reports `Tests  129 passed (129)`
- [ ] `npm run build` exits 0
- [ ] `main.tsx` calls `bootDuelClient` exactly once, at module scope, before `createRoot(...)`
- [ ] `main.tsx` contains no `WebSocket`, and `React.StrictMode` is still the outermost element
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
