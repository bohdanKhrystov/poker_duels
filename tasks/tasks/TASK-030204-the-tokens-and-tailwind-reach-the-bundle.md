---
schema: 2
id: TASK-030204
title: The tokens and Tailwind reach the bundle through one stylesheet
type: task
status: done
parent: STORY-0302
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, design, styling]
depends_on: [TASK-030203]
verify:
  - cd web-client && npm ci
  - cd web-client && npm run build
  - grep -rqE -e '--pd-bg: *#131211' web-client/dist/assets
  - grep -rqF -e 'var(--pd-bg)' web-client/dist/assets
  - grep -rqE -e 'box-sizing: *border-box' web-client/dist/assets
  - grep -rl -e 'prefers-color-scheme' web-client/src | grep -c . | grep -qx 0
  - cd web-client && npm run check
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +10 passed \(10\)'
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

One stylesheet, `src/styles/app.css`, pulls in Tailwind and the vendored tokens, the app root
imports it, and both end up in the built CSS.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | create |
| `web-client/src/main.tsx` | modify |

## Scope

- `src/styles/app.css`, in this order — CSS requires every `@import` before any rule:

  ```css
  @import "tailwindcss";
  @import "./tokens.css";

  /* Dark only. STORY-0601 scoped light out epic-wide: no prefers-color-scheme
   * branch, no theme switch, no second palette. */
  :root {
    color-scheme: dark;
  }

  body {
    background: var(--pd-bg);
    color: var(--pd-text);
    font-family: var(--pd-font-ui);
    font-size: var(--pd-fs-body);
    line-height: var(--pd-lh-body);
  }
  ```

  Every value is a `var(--pd-…)`. The colour-literal guard from `TASK-030202` scans this file — it
  is not the token layer, and it gets no exemption.
- `src/main.tsx` gains one line, `import "./styles/app.css";`, above the React imports.
- If the build emits no CSS asset, the cause is the stylesheet not being reachable from
  `index.html` → `main.tsx`; fix the import, do not add a `<link>` to `index.html`.

## Out of scope

- `@theme` and any Tailwind utility class — `TASK-030205` and `TASK-030206`. This ticket proves the
  pipeline, not the palette.
- Styling `App.tsx` — `TASK-030208`.
- A second stylesheet, CSS modules, or any styling in a `.tsx` file. One entry point.
- Touching `src/styles/tokens.css`. It is imported, never edited: `TASK-030201`'s byte comparison
  runs in the same suite and will say so.

## Proof

| Command | Proves |
| --- | --- |
| `--pd-bg: #131211` in `dist/assets` | the vendored token sheet was inlined into the bundle, values and all |
| `var(--pd-bg)` in `dist/assets` | the app's own rules resolve through the tokens rather than restating them |
| `box-sizing: border-box` in `dist/assets` | Tailwind's preflight compiled — the `@tailwindcss/vite` plugin actually processed the file. Nothing this ticket writes contains that declaration |
| no `prefers-color-scheme` under `src/` | dark only, structurally, from the first stylesheet |
| `Tests 10 passed (10)` | the guard from `TASK-030202` still finds no literal, now with a stylesheet to scan |

The build minifies CSS, so the greps are whitespace-tolerant (`: *`) rather than exact.

Watch it fail: comment out `@import "./tokens.css"` and rebuild — the first two greps go red while
the third stays green, which tells you the two imports are proved independently. Restore it.

## Acceptance criteria

- [ ] `cd web-client && npm run build` exits 0 and emits a CSS asset under `web-client/dist/assets`
- [ ] That CSS contains `--pd-bg` with the value from the token sheet
- [ ] That CSS contains `var(--pd-bg)` from the `body` rule
- [ ] That CSS contains `box-sizing: border-box` from Tailwind's preflight
- [ ] `app.css` contains no hex, `rgb()`, `hsl()` or `oklch()` literal
- [ ] No file under `web-client/src` mentions `prefers-color-scheme`
- [ ] `npm run --silent test` reports `Tests  10 passed (10)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
