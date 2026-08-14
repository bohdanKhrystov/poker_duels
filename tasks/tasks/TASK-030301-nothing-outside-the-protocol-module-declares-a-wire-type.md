---
schema: 2
id: TASK-030301
title: Nothing outside src/protocol declares a wire type or touches a raw frame
type: task
status: ready
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol, guard]
depends_on: [TASK-030209]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +19 passed \(19\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'guards the app source and skips the protocol module'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'finds no wire type declared outside the protocol module'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'finds no raw frame handled outside the protocol module'
  - grep -qF 'protocol.gen.ts' web-client/src/protocol/boundary.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run check` goes red when any file outside `src/protocol/` declares a type whose name the
generated protocol file already owns, or mentions `WebSocket` or `MessageEvent`.

## Why it lands first

The story's fifth acceptance criterion is a rule about *every other file in the client*, and a rule
added after the code exists has to fix what it finds. Landing it first means every file this story
and `STORY-0304` onwards write is born under it — the same reasoning that put `TASK-030102`
(Prettier) and `TASK-030202` (the colour guard) at the front of their stories.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/boundary.ts` | create |
| `web-client/src/protocol/boundary.test.ts` | create |
| `web-client/src/styles/color-literals.ts` | read — copy its `import.meta.url` root-finding and its `readdirSync(dir, { recursive: true })` walk; this file is the same shape one directory over |

## Scope

- `boundary.ts` exports exactly four things:

  ```ts
  /** Every type name the generated protocol file exports, sorted. */
  export function reservedWireNames(): string[];

  /** Declared names in `source` that collide with `reserved`, in order. */
  export function redeclaredWireTypes(
    source: string,
    reserved: readonly string[],
  ): string[];

  /** Every mention of a raw-frame API in `source`, in order. */
  export function rawFrameMentions(source: string): string[];

  /** Absolute paths of every file the guard covers, sorted. */
  export function guardedFiles(): string[];
  ```

- Three patterns and nothing else:

  ```ts
  const EXPORTED_TYPE = /^export (?:type|interface) (\w+)/gm;
  const DECLARATION = /\b(?:interface|type|class|enum)\s+(\w+)\s*(?:[=<{]|extends\b)/g;
  const RAW_FRAME = /\b(?:WebSocket|MessageEvent)\b/g;
  ```

  `DECLARATION` requires a `=`, `<`, `{` or `extends` after the name so that prose in a comment —
  *"the type Snapshot comes from the wire"* — is not a violation. It deliberately does not match
  `import type { Snapshot }`: importing a wire type is the behaviour this guard exists to require.

- `reservedWireNames()` reads `protocol.gen.ts` from `join(dirname(fileURLToPath(import.meta.url)),
  "protocol.gen.ts")`. Never `process.cwd()`.
- `guardedFiles()` walks `src/` and keeps every `.ts` and `.tsx`, **excluding every path under
  `protocol/`** — that directory is the one place a raw frame and a wire type belong. Test files are
  *not* excluded: a fixture may import a wire type but may not declare one.
- Compare paths with `entry.split(sep).join("/")` before testing the `protocol/` prefix, so the
  exclusion holds on any separator.
- Run `npm run format` before committing; `npm run check` format-checks both new files.

## Out of scope

- An ESLint rule or a new dependency. A Vitest test rides inside `npm run test`, therefore inside
  `npm run check`, therefore inside the `client` CI job — `TASK-030202` set that precedent for the
  colour guard and this is the same mechanism with no second ignore list to keep in step.
- Fixing a violation. There is none today: no file outside `src/protocol/` declares a type at all,
  and nothing in the client mentions `WebSocket`. If the scan finds one, stop and report it.
- Anything about *how* frames are decoded — `TASK-030303`.
- Banning an import of `protocol.gen.ts` from outside the module. Importing the generated types is
  correct and is the whole point; only re-declaring them is the sin.

## Tests

`web-client/src/protocol/boundary.test.ts`, describe block `"the protocol module boundary"`. Seven
`it` blocks, exactly these, in this order:

| Test | Proves |
| --- | --- |
| `flags a redeclared wire type` | `redeclaredWireTypes("export interface Snapshot { view: number }", ["Snapshot"])` is `["Snapshot"]` |
| `passes an import of a wire type` | `redeclaredWireTypes('import type { Snapshot } from "./protocol.gen";', ["Snapshot"])` is `[]` |
| `flags a raw frame API` | `rawFrameMentions("const s = new WebSocket(url); function f(e: MessageEvent) {}")` has length 2 |
| `reads the wire names out of the generated file` | `reservedWireNames()` contains `ServerMessage`, `Welcome` and `ProtocolVersion`, and has more than 40 entries |
| `guards the app source and skips the protocol module` | `guardedFiles()` has an entry ending `/src/App.tsx` and one ending `/src/styles/color-literals.ts`, and none ending `/src/protocol/protocol.gen.ts` or `/src/protocol/boundary.ts` |
| `finds no wire type declared outside the protocol module` | every guarded file, read and passed through `redeclaredWireTypes(source, reservedWireNames())`, yields nothing |
| `finds no raw frame handled outside the protocol module` | every guarded file, read and passed through `rawFrameMentions`, yields nothing |

Seven tests. Twelve existed before this ticket, so the suite reports **19**.

The `> 40` floor on `reservedWireNames()` is there because a regex that silently matches nothing
would otherwise make the two scanning tests pass forever — the same trap `theme.test.ts` guards with
`expect(declarations.length).toBeGreaterThan(20)`.

The two scanning tests must report *which* file offended when they fail — collect
`{ file, names }` for the offenders and assert the collection is `[]`, so the failure message names
them.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 19 passed (19)` | the seven tests ran and nothing earlier was displaced |
| the three `--reporter=verbose` greps | the tests that carry the guarantee exist by name — a count cannot say which seven ran |
| `grep 'protocol.gen.ts' boundary.ts` | the reserved names are read out of the generated file, not from a list a human keeps |
| `npm run check` | the guard runs inside the aggregate command CI runs |

**Name the edit that makes each assertion red.** Perform these three, one at a time, and quote the
failure message in the PR:

1. Add `interface Snapshot { view: number }` to `src/App.tsx` →
   `finds no wire type declared outside the protocol module` fails and names `src/App.tsx`. Revert.
2. Add `const socket: WebSocket | null = null;` to `src/App.tsx` →
   `finds no raw frame handled outside the protocol module` fails and names `src/App.tsx`. Revert.
3. Delete the `protocol/` exclusion from `guardedFiles()` →
   `guards the app source and skips the protocol module` fails, **and** the wire-type scan fails on
   `protocol.gen.ts` itself, which declares every reserved name. Restore it.

The third is the one that matters: it shows the exclusion is load-bearing on day one, rather than a
line nobody could ever observe. `TASK-030208` recorded what happens when a guard has nothing to
observe — the assertion stays green through the very change it was written to catch.

## Acceptance criteria

- [ ] `the protocol module boundary > flags a redeclared wire type` passes
- [ ] `the protocol module boundary > passes an import of a wire type` passes
- [ ] `the protocol module boundary > flags a raw frame API` passes
- [ ] `the protocol module boundary > reads the wire names out of the generated file` passes
- [ ] `the protocol module boundary > guards the app source and skips the protocol module` passes
- [ ] `the protocol module boundary > finds no wire type declared outside the protocol module` passes
- [ ] `the protocol module boundary > finds no raw frame handled outside the protocol module` passes
- [ ] `npm run --silent test` reports `Tests  19 passed (19)`
- [ ] No byte is added to `src/protocol/protocol.gen.ts`
- [ ] The PR quotes the three failure messages from the Proof section
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
