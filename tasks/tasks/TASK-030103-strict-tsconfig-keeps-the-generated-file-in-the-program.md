---
schema: 2
id: TASK-030103
title: A strict tsconfig that keeps the generated file inside the typechecked program
type: task
status: ready
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, build, typescript, protocol]
depends_on: [TASK-030102]
verify:
  - cd web-client && npm run typecheck
  - cd web-client && ./node_modules/.bin/tsc -p tsconfig.json --listFilesOnly | grep -c 'src/protocol/protocol.gen.ts' | grep -qx 1
  - cd web-client && printf 'import type { Street } from "./protocol/protocol.gen";\nexport const probe = "NOT_A_STREET" satisfies Street;\n' > src/typecheck-probe.ts; npm run --silent typecheck; rc=$?; rm -f src/typecheck-probe.ts; test $rc -ne 0
  - test ! -e web-client/src/typecheck-probe.ts
  - cd web-client && npm run format:check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run typecheck` compiles `web-client/` under `strict`, with
`src/protocol/protocol.gen.ts` inside the program — which is what makes it safe for
`TASK-030110` to delete CI's ad-hoc `npx tsc` step.

## Files

| File | Action |
| --- | --- |
| `web-client/tsconfig.json` | create |
| `web-client/package.json` | modify |

## Scope

- One `tsconfig.json`, not the three-file `tsconfig.json` / `tsconfig.app.json` /
  `tsconfig.node.json` split some templates use. Write exactly this:

  ```json
  {
    "compilerOptions": {
      "target": "ES2022",
      "lib": ["ES2022", "DOM", "DOM.Iterable"],
      "module": "ESNext",
      "moduleResolution": "bundler",
      "jsx": "react-jsx",
      "strict": true,
      "noEmit": true,
      "noUnusedLocals": true,
      "noUnusedParameters": true,
      "isolatedModules": true,
      "verbatimModuleSyntax": true,
      "resolveJsonModule": true,
      "skipLibCheck": true
    },
    "include": ["src", "vite.config.ts"]
  }
  ```

  `vite.config.ts` does not exist yet and that is fine — `include` patterns that match nothing are
  ignored, and `src/protocol/protocol.gen.ts` already makes the program non-empty.
  `verbatimModuleSyntax` is there so a protocol type imported without `import type` fails at
  typecheck rather than at bundle time, which every later `EPIC-03` story depends on.
- Add one script: `"typecheck": "tsc --noEmit"` — `ADR-0026`'s first term in the aggregate `check`.

## Out of scope

- Path aliases, project references, `baseUrl`, incremental builds. None is needed by a module with
  one source directory.
- Any edit to the generated file, including an import added to it. `tsc` reads it and never writes
  it; that is the whole reason `ADR-0026` keeps it in `include`.
- Vite, Vitest, ESLint config — later tickets.

## Proof

Three commands, each closing a different way this could look green while covering nothing:

| Command | Proves |
| --- | --- |
| `npm run typecheck` | the program compiles clean today |
| `tsc -p tsconfig.json --listFilesOnly` lists the generated path exactly once | the generated file is genuinely in the program, not merely importable — the property `TASK-030110` trades the CI step for |
| the probe command | a wrong use of a generated type **fails** the typecheck |

The probe writes `src/typecheck-probe.ts`, containing a `Street` value the union does not admit,
runs the typecheck, deletes the probe, and passes only if the typecheck failed. Nothing named
`typecheck-probe.ts` is ever committed — the fourth verify command asserts that.

Run the probe by hand once and read the error. If the typecheck passes with the probe in place, stop
and report it: the generated types are not being read, and no config change should be improvised to
make the number go green.

## Acceptance criteria

- [ ] `cd web-client && npm run typecheck` exits 0
- [ ] `tsc -p tsconfig.json --listFilesOnly` includes `src/protocol/protocol.gen.ts`
- [ ] The typecheck **fails** while `src/typecheck-probe.ts` exists, and the file is not committed
- [ ] `tsconfig.json` sets `"strict": true` and `"noEmit": true`
- [ ] `./gradlew :poker-server:verifyProtocolTypes` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
