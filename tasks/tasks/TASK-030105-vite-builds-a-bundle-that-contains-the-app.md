---
schema: 2
id: TASK-030105
title: Vite builds a production bundle that contains the app
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, build, toolchain]
depends_on: [TASK-030104]
verify:
  - cd web-client && npm run build
  - test -f web-client/dist/index.html
  - grep -rq 'Poker Duels' web-client/dist/assets
  - cd web-client && npm run typecheck
  - cd web-client && npm run format:check
  - git check-ignore -q web-client/dist
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run build` in `web-client/` emits a static bundle under `dist/` that actually contains the
mounted component, and `npm run dev` starts Vite.

## Files

| File | Action |
| --- | --- |
| `web-client/vite.config.ts` | create |
| `web-client/package.json` | modify |

## Scope

- `vite.config.ts`, exactly this and no more:

  ```ts
  import react from '@vitejs/plugin-react';
  import { defineConfig } from 'vite';

  export default defineConfig({
    plugins: [react()],
  });
  ```

- Two scripts: `"dev": "vite"` and `"build": "vite build"`. `ADR-0026` specifies `build` as
  `vite build` alone — the typecheck is a separate script and CI runs `npm run check` before
  `npm run build`, so do not chain `tsc` into it.
- `vite.config.ts` now enters the typechecked program (`tsconfig.json` already includes it), so
  `npm run typecheck` covers it from this ticket on.

## Out of scope

- The dev-server proxy for `/api` and `/ws` — `TASK-030108`, which lands it together with the test
  that proves it. Write no `server` block here.
- The Vitest `test` block — `TASK-030106`.
- `@tailwindcss/vite` — `STORY-0302`.
- Any `base`, `build.outDir`, chunking or asset tuning. Serving the bundle is `EPIC-07`'s problem;
  this ticket only proves one can be produced.
- Adding `dist/` to `.gitignore`. It is already ignored, and the verify block proves it.

## Proof

The failure this guards against is a build that exits 0 and emits an empty page:

| Command | Proves |
| --- | --- |
| `npm run build` | Vite resolves the entry, the React plugin transforms JSX, and the bundle is written |
| `test -f dist/index.html` | the HTML entry was processed rather than skipped |
| `grep -rq 'Poker Duels' dist/assets` | the component's own text reached a JS chunk — an empty or unreferenced entry fails here even though the build exits 0 |
| `git check-ignore -q web-client/dist` | the bundle is not about to be committed |

`npm run format:check` still passing is not incidental: `.prettierignore` lists `dist`, so a bundle
in the tree does not turn the format check red. If it does, the ignore entry is wrong — that is
`TASK-030102`'s file and a finding, not an edit to make here.

## Acceptance criteria

- [ ] `cd web-client && npm run build` exits 0 and writes `web-client/dist/index.html`
- [ ] A file under `web-client/dist/assets` contains the string `Poker Duels`
- [ ] `cd web-client && npm run typecheck` exits 0 with `vite.config.ts` in the program
- [ ] `vite.config.ts` declares no `server`, no `test` and no Tailwind plugin
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
