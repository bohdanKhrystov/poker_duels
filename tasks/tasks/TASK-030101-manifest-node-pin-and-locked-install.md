---
schema: 2
id: TASK-030101
title: A manifest, a Node pin and a locked install for web-client
type: task
status: ready
parent: STORY-0301
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [client, build, toolchain]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && test -x node_modules/.bin/vite && test -x node_modules/.bin/vitest && test -x node_modules/.bin/tsc && test -x node_modules/.bin/eslint && test -x node_modules/.bin/prettier
  - node -e "const p=require('./web-client/package.json'); if (p.engines.node !== '>=24') process.exit(1)"
  - grep -qx '24' web-client/.nvmrc
  - git check-ignore -q web-client/node_modules
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm ci` in `web-client/` installs the whole toolchain `ADR-0026` names, from a committed lockfile,
on the Node version pinned in one place.

## Files

| File | Action |
| --- | --- |
| `web-client/package.json` | create |
| `web-client/package-lock.json` | create |
| `web-client/.nvmrc` | create |

The lockfile is **generated** by `npm install` and never hand-written or hand-edited; it does not
count towards this ticket's line estimate.

## Scope

- `web-client/.nvmrc` contains exactly one line, `24` — the single Node pin `ADR-0026` mandates,
  which CI later reads through `node-version-file`.
- `web-client/package.json` declares `"name": "web-client"`, `"private": true`, `"version":
  "0.0.0"`, `"type": "module"` (the flat ESLint config and `vite.config.ts` are ESM), and
  `"engines": { "node": ">=24" }`.
- Dependencies: `react`, `react-dom`.
- Dev dependencies, installed with `npm install -D`: `vite`, `@vitejs/plugin-react`, `typescript`,
  `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/dom`, `@types/react`,
  `@types/react-dom`, `@types/node`, `eslint`, `@eslint/js`, `typescript-eslint`,
  `eslint-plugin-react-hooks`, `eslint-config-prettier`, `globals`, `prettier`.
- **Install the current stable of each and commit whatever the lockfile resolves.** Do not
  hand-write version ranges or copy versions out of this ticket — the lockfile is the pin, and
  `ADR-0026` says "current major", not a number.
- No `scripts` block yet. Every script arrives with the ticket that makes it runnable.

## Out of scope

- `tailwindcss`, `@tailwindcss/vite` and `prettier-plugin-tailwindcss`. `ADR-0026` puts them in the
  toolchain, but the stylesheet they need is the styling layer, which is `STORY-0302`'s — and
  `prettier-plugin-tailwindcss` with no stylesheet to read is a warning generator. They install with
  `STORY-0302`, not here.
- Every config file — `tsconfig.json`, `vite.config.ts`, `eslint.config.js`, `.prettierignore` —
  `TASK-030102` through `TASK-030107`.
- Any change to `.gitignore`. `node_modules/` and `dist/` are already ignored; the verify block
  proves it rather than editing anything.
- Any byte of `web-client/src/protocol/protocol.gen.ts`. Nothing in this ticket reads or writes it.

## Proof

There is no test runner yet, so the verify block is the proof and each command is checkable:

| Command | Proves |
| --- | --- |
| `npm ci` | the committed lockfile installs on its own, exactly as CI will install it |
| the `test -x` chain | the five binaries later tickets call are really on disk — an install that silently resolved nothing fails here |
| the `node -e` engines check | `engines.node` is `>=24`, agreeing with `.nvmrc` |
| `grep -qx '24' .nvmrc` | the pin is the single line `actions/setup-node` can read |
| `git check-ignore` | `node_modules/` is not about to be committed |

Confirm `npm ci` fails without the lockfile: `npm ci` errors when `package-lock.json` is missing, so
a run that passes proves the lockfile is committed rather than regenerated.

## Acceptance criteria

- [ ] `cd web-client && npm ci` exits 0 from a clean tree with no `node_modules/`
- [ ] `web-client/package-lock.json` is committed and was produced by npm, not edited by hand
- [ ] `web-client/.nvmrc` is the single line `24`
- [ ] `package.json` has `engines.node` of `>=24`, `"type": "module"` and `"private": true`
- [ ] `package.json` declares no `tailwindcss`, `@tailwindcss/vite` or `prettier-plugin-tailwindcss`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
