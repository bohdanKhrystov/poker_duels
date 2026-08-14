---
schema: 2
id: TASK-030201
title: Vendor the token sheet into the client, byte for byte
type: task
status: done
parent: STORY-0302
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, design, styling]
depends_on: [TASK-030111]
verify:
  - cd web-client && npm ci
  - cmp -s design/tokens/tokens.css web-client/src/styles/tokens.css
  - grep -qx 'src/styles/tokens.css' web-client/.prettierignore
  - cd web-client && ./node_modules/.bin/prettier --write src/styles/tokens.css
  - cmp -s design/tokens/tokens.css web-client/src/styles/tokens.css
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`web-client/src/styles/tokens.css` is a byte-for-byte copy of `design/tokens/tokens.css`, and a test
in the client's own suite fails the moment the two differ.

## Why a copy and not an import

`design/tokens/tokens.css` lives outside `web-client/`, and `web-client/` is the Vite root. A CSS
source outside the root reaches the dev server only through `server.fs.allow`, whose default is
Vite's *workspace root* search — which stops at `web-client/`, because that is where the
`package.json` and the lockfile are. `vite build` and Vitest resolve through the same pipeline, so a
cross-boundary `@import` is one Vite version away from being denied in three places at once. A
vendored copy keeps the client hermetic: everything it builds from is under its own root.

`ADR-0024` already accepts duplication of token values guarded by an automated check — preview cards
inline them because the render surface needs self-contained files. This is the same trade with a
stronger guard: not a name grep but a byte comparison.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | read only — **never edit** |
| `web-client/src/styles/tokens.css` | create (copied, not typed) |
| `web-client/.prettierignore` | modify |
| `web-client/src/styles/tokens.test.ts` | create |

Three files change; the fourth is read. The copy is produced by
`cp design/tokens/tokens.css web-client/src/styles/tokens.css` — it is generated, like the lockfile
in `TASK-030101`, and does not count towards the line estimate.

## Scope

- Copy the file with `cp`. Do not retype it, do not reflow it, do not add a header comment saying it
  is a copy: any byte you add fails this ticket's own `cmp`.
- `.prettierignore` gains a fourth line, `src/styles/tokens.css`, below the three
  `TASK-030102` wrote. That ticket's prose says "exactly three lines"; it is four now, for the same
  reason the third one exists — a file whose bytes belong to someone else is excluded **by path**.
  Prettier 3 really would rewrite this file (it reflows the two font stacks), so the line is
  load-bearing, not decorative.
- `tokens.test.ts` locates both files from `import.meta.url` — `new URL("./tokens.css", import.meta.url)`
  and `new URL("../../../design/tokens/tokens.css", import.meta.url)` — and compares them. Do not use
  `process.cwd()` or `__dirname`.
- Read both with `readFileSync` and assert twice: the decoded strings are equal (so a failure prints
  a readable diff) and `Buffer.equals` is true (so line endings and a stray BOM cannot slip through).

## Out of scope

- Anything under `design/`. `EPIC-06` is being worked in parallel; this ticket reads that directory
  and writes nothing to it.
- Importing the copy from any stylesheet or module — `TASK-030204` wires it into the bundle.
- Tailwind, `@theme`, `.prettierrc` — `TASK-030203` onwards.
- A script that re-copies the file. If drift ever appears, the fix is one `cp`, and the test names
  it.

## Tests

`web-client/src/styles/tokens.test.ts`, describe block `"the vendored token sheet"`

| Test | Proves |
| --- | --- |
| `is byte-identical to design/tokens/tokens.css` | the copy has not drifted from the source, in either direction |

## Proof

| Command | Proves |
| --- | --- |
| `cmp -s design/tokens/tokens.css web-client/src/styles/tokens.css` | the two files are identical right now |
| `grep -qx 'src/styles/tokens.css' web-client/.prettierignore` | the exclusion is by path, the mechanism `ADR-0026` mandates |
| `prettier --write src/styles/tokens.css` then `cmp` again | the *writing* formatter, aimed straight at the file, moved no byte. Remove the ignore line and this pair goes red — Prettier reflows the `--pd-font-ui` and `--pd-font-mono` stacks |
| `Tests 4 passed (4)` | the new test ran. Three tests existed before this ticket |

Watch it fail, and say in the PR what you saw: change one hex digit in
`web-client/src/styles/tokens.css`, run `npm run test`, see the byte-identity test go red, then
restore the file with `cp`. Perturb the **copy**, never the source — `design/` belongs to `EPIC-06`
and has concurrent work in it.

## Acceptance criteria

- [ ] `cmp -s design/tokens/tokens.css web-client/src/styles/tokens.css` exits 0
- [ ] `web-client/src/styles/tokens.css` contains no line that is not in the source
- [ ] `.prettierignore` contains the line `src/styles/tokens.css`
- [ ] `prettier --write src/styles/tokens.css` leaves the file byte-identical to the source
- [ ] `the vendored token sheet > is byte-identical to design/tokens/tokens.css` passes
- [ ] `npm run --silent test` reports `Tests  4 passed (4)`
- [ ] No file under `design/` is modified by this PR
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
