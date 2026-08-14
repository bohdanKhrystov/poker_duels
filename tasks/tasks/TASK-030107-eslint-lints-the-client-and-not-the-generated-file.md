---
schema: 2
id: TASK-030107
title: ESLint lints the client and never the generated file
type: task
status: ready
parent: STORY-0301
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, build, toolchain, protocol]
depends_on: [TASK-030106]
verify:
  - cd web-client && npm run lint
  - cd web-client && ./node_modules/.bin/eslint . -f json | grep -c 'protocol.gen.ts' | grep -qx 0
  - cd web-client && ./node_modules/.bin/eslint . -f json | grep -c 'App.tsx' | grep -qx 1
  - cd web-client && npm run typecheck
  - cd web-client && npm test
  - cd web-client && npm run format:check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run lint` passes with zero warnings over `web-client/`, and ESLint never opens
`src/protocol/protocol.gen.ts`.

## Files

| File | Action |
| --- | --- |
| `web-client/eslint.config.js` | create |
| `web-client/package.json` | modify |

## Scope

- `eslint.config.js`, ESLint 9 flat config, opening with the global ignore object `ADR-0026`
  mandates. Start from this and adjust only what the installed versions force:

  ```js
  import js from '@eslint/js';
  import prettier from 'eslint-config-prettier';
  import reactHooks from 'eslint-plugin-react-hooks';
  import globals from 'globals';
  import tseslint from 'typescript-eslint';

  export default tseslint.config(
    { ignores: ['dist', 'src/protocol/protocol.gen.ts'] },
    {
      files: ['**/*.{js,ts,tsx}'],
      extends: [
        js.configs.recommended,
        ...tseslint.configs.recommended,
        reactHooks.configs['recommended-latest'],
        prettier,
      ],
      languageOptions: { ecmaVersion: 2022, globals: globals.browser },
    },
  );
  ```

  `eslint-config-prettier` is **last**, per `ADR-0026`, so it can switch off the formatting rules
  Prettier owns. `dist` is ignored because ESLint would otherwise lint a minified bundle.
  `files` covers `.js` so `eslint.config.js` itself has a matching configuration.
- One script: `"lint": "eslint --max-warnings 0 ."` — `ADR-0026`'s exact command, including the
  flag, so a warning is as fatal as an error.
- `eslint-plugin-react-hooks` has renamed its flat-config export more than once. If
  `configs['recommended-latest']` does not exist in the installed version, use whatever that
  version's own documentation names and **say which, and why, in the PR**. That is an
  implementation detail of a settled decision, not a new decision.

## Out of scope

- Type-aware linting (`recommendedTypeChecked`, `parserOptions.project`). `tsc --noEmit` already
  covers the whole program, and the second type-checking pass would double the check's runtime for
  rules nothing here needs.
- Rule tuning, `eslint-plugin-react-refresh`, import ordering. If a recommended rule fires on an
  existing file, **stop and report it** rather than editing that file — it belongs to an earlier
  ticket and is outside this budget.
- `// eslint-disable` anywhere in `src/protocol/protocol.gen.ts`. `ADR-0026` forbids it by name: the
  file's bytes belong to the emitter and any header it did not write fails `verifyProtocolTypes`.
  Ignore by path is the only mechanism.

## Proof

The exclusion is asserted from ESLint's own machine-readable output, not from an exit code:

| Command | Proves |
| --- | --- |
| `eslint . -f json` never mentions `protocol.gen.ts` | the generated file was not linted — the JSON formatter emits an entry for every file ESLint *did* lint, clean or not |
| `eslint . -f json` mentions `App.tsx` | ESLint actually linted real source. Without this, a config that quietly matched nothing would satisfy the first assertion perfectly |
| `npm run lint` | zero errors and zero warnings across the client |

Watch it fail: remove `'src/protocol/protocol.gen.ts'` from the `ignores` array, re-run the first
command, and the count becomes 1. Restore it, and say in the PR that you checked.

## Acceptance criteria

- [ ] `cd web-client && npm run lint` exits 0
- [ ] `eslint.config.js` begins with `{ ignores: ['dist', 'src/protocol/protocol.gen.ts'] }`
- [ ] `eslint-config-prettier` is the last entry in `extends`
- [ ] ESLint's JSON output lists `App.tsx` and does not list `protocol.gen.ts`
- [ ] No `eslint-disable` comment exists anywhere in `web-client/src/protocol/`
- [ ] `npm run typecheck`, `npm test` and `npm run format:check` all still exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
