---
schema: 2
id: TASK-030104
title: An app root that mounts one trivial component
type: task
status: ready
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, react]
depends_on: [TASK-030103]
verify:
  - cd web-client && npm run typecheck
  - cd web-client && npm run format:check
  - grep -c 'src/main.tsx' web-client/index.html | grep -qx 1
  - grep -c 'id="root"' web-client/index.html | grep -qx 1
  - grep -c 'Poker Duels' web-client/src/App.tsx | grep -qx 1
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`web-client/` has an HTML entry point and a React root that mounts one component rendering the words
`Poker Duels`, and it typechecks.

## Files

| File | Action |
| --- | --- |
| `web-client/index.html` | create |
| `web-client/src/main.tsx` | create |
| `web-client/src/App.tsx` | create |

## Scope

- `index.html` at the package root (Vite's convention), with `<div id="root"></div>` and
  `<script type="module" src="/src/main.tsx"></script>`, `lang="en"`, and the title `Poker Duels`.
- `src/App.tsx` exports a **named** `App` component — `export function App()` — returning
  `<h1>Poker Duels</h1>` and nothing else. The exact string matters: `TASK-030105` greps the built
  bundle for it and `TASK-030106` asserts on the heading.
- `src/main.tsx` imports `{ App }`, finds `#root`, and mounts it in `<StrictMode>`. Do not use a
  non-null assertion; write the null check so it fails loudly:

  ```tsx
  const container = document.getElementById('root');
  if (!container) throw new Error('missing #root');
  ```

- Run `npm run format` before committing, so `format:check` is green.

## Out of scope

- **Any styling.** No `import './index.css'`, no `className`, no colour, no
  `design/tokens/tokens.css`. `STORY-0302` owns the styling layer and a scaffold that quietly
  imports a colour has already started that story.
- Any protocol type, any socket, any `fetch`, any router, any state. `STORY-0303` and `STORY-0304`.
- `vite.config.ts` and the `dev` and `build` scripts — `TASK-030105`. Nothing here runs a server or
  a bundler; the typecheck is what proves this ticket.

## Proof

No test runner exists yet, so the verify block carries it:

| Command | Proves |
| --- | --- |
| `npm run typecheck` | the JSX and the mount compile under `strict` with `jsx: react-jsx` |
| the two `index.html` greps | the page has the mount point and points at the real entry module — the two ways a Vite build silently produces a blank page |
| the `App.tsx` grep | the string the next two tickets assert on is actually there |

`TASK-030106` is what renders this component and asserts what it shows; this ticket only has to
compile.

## Acceptance criteria

- [ ] `cd web-client && npm run typecheck` exits 0
- [ ] `cd web-client && npm run format:check` exits 0
- [ ] `index.html` contains `<div id="root"></div>` and a module script for `/src/main.tsx`
- [ ] `src/App.tsx` exports `App` and renders the heading `Poker Duels`
- [ ] No file created here imports a stylesheet, a protocol type or anything from the network
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
