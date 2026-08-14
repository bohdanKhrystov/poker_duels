---
schema: 2
id: TASK-030202
title: The client's check fails on a colour literal outside the token layer
type: task
status: backlog
parent: STORY-0302
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, design, styling, protocol]
depends_on: [TASK-030201]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +10 passed \(10\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'scans the app source and skips the token layer and the generated file'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'finds no colour literal in the client source'
  - cd web-client && npm run check
  - grep -q 'protocol.gen.ts' web-client/src/styles/color-literals.ts
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run check` goes red when any client source file outside the token layer contains a hex,
`rgb()`, `hsl()` or `oklch()` colour literal.

## Why it lands this early

The enforcement is the story's point, and a rule added after the styling exists has to fix the files
it finds. Landing it second means every file the rest of the story writes is born under it — the
same reason `TASK-030102` put Prettier before any source.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/color-literals.ts` | create |
| `web-client/src/styles/color-literals.test.ts` | create |

## Scope

- `color-literals.ts` exports exactly two things:

  - `findColorLiterals(source: string): string[]` — every colour literal in a string, in order.
    Match on two patterns and nothing else:

    ```ts
    /#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\b/g
    /\b(?:rgba?|hsla?|lab|lch|oklab|oklch)\s*\(/g
    ```

    `oklch` and friends are in the list because Tailwind's own palette is written in `oklch()`, so
    that is the literal most likely to appear here. `color-mix(` is deliberately **not** matched:
    `color-mix(in oklab, var(--pd-accent) 40%, transparent)` derives from a token and is legitimate.

  - `scannedFiles(): string[]` — absolute paths of every file the guard covers, sorted. Walk
    `src/` with `readdirSync(dir, { recursive: true })` (which returns `string[]`, no `Dirent`
    handling), keep `.ts`, `.tsx`, `.css` and `.html`, and add the single file `index.html` at the
    client root. Locate that root from `import.meta.url`, never from `process.cwd()`.

- Excluded from the walk, by path:
  - `src/protocol/protocol.gen.ts` — its bytes belong to the Kotlin emitter (`ADR-0026`), and it is
    excluded here the same way ESLint and Prettier exclude it: by path, never by a pragma inside it.
  - `src/styles/tokens.css` — this **is** the token layer. It is where the literals are supposed to
    be.
  - any file ending `.test.ts` or `.test.tsx` — nothing in a test reaches the bundle, and this
    ticket's own fixtures must contain colour literals by construction.
- `color-literals.ts` must not contain a string that its own patterns match — the guard scans
  itself, and one of the tests below proves it. Written as the regexes above, it does not: `#` is
  followed by `(`, and the alternation `rgba?|hsla?` contains no `(` after a colour name.

## Out of scope

- An ESLint rule or a stylelint dependency. A Vitest test rides inside `npm run test`, therefore
  inside `npm run check`, therefore inside the `client` CI job, with no new tool to configure and no
  second ignore list to keep in step with the first two.
- Scanning `dist/`. The built CSS is full of colours by design — Tailwind's preflight and our own
  tokens compile into it. The guard is about what a human writes.
- Fixing a violation anywhere. There is none today; if the scan finds one, stop and report it, do
  not edit the offending file — it belongs to another ticket.
- Sizes, spacing and font stacks. This guard is colour only; the theme tickets are what stop an
  invented size, by mapping the scales and clearing Tailwind's defaults.

## Tests

`web-client/src/styles/color-literals.test.ts`, describe block `"the colour literal guard"`

| Test | Proves |
| --- | --- |
| `flags a hex literal` | `findColorLiterals("color: #ece9e3;")` returns `["#ece9e3"]` |
| `flags rgb and hsl functions` | `findColorLiterals("a{color:rgb(1 2 3)}b{color:hsl(0 0% 0%)}")` returns two matches |
| `flags an oklch literal` | `findColorLiterals("color: oklch(63.7% 0.237 25.331)")` returns one match |
| `passes a token reference` | `findColorLiterals("color: var(--pd-text); background: color-mix(in oklab, var(--pd-accent) 40%, transparent)")` returns `[]` |
| `scans the app source and skips the token layer and the generated file` | `scannedFiles()` has an entry ending `/src/App.tsx` and one ending `/src/styles/color-literals.ts`, and none ending `/src/protocol/protocol.gen.ts` or `/src/styles/tokens.css` |
| `finds no colour literal in the client source` | every file from `scannedFiles()`, read and passed through `findColorLiterals`, yields nothing |

Six tests. Three existed before `TASK-030201` and one arrived with it, so the suite reports ten.

The last test must report *which* file offended when it fails — collect `{ file, literals }` for the
offenders and assert the collection is `[]`, so the failure message names them.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 10 passed (10)` | the six tests ran, and nothing earlier was displaced |
| the two `--reporter=verbose` greps | the two tests that carry the guarantee exist by name — a count alone cannot tell which six tests ran |
| `grep 'protocol.gen.ts' color-literals.ts` matches | the generated file is excluded **by path**, in the walker — the mechanism `ADR-0026` mandates, not a pragma inside the file |
| `npm run check` | the guard runs inside the aggregate command CI runs, which is the whole requirement |

Watch it fail: add `style={{ color: "#ff0000" }}` to the `<h1>` in `src/App.tsx`, run
`npm run check`, and `finds no colour literal in the client source` names `src/App.tsx`. Revert.
Then delete the `src/styles/tokens.css` exclusion from the walker and watch the same test go red on
the token sheet itself — that is the exclusion earning its place. Restore it, and say in the PR what
both failures said.

## Acceptance criteria

- [ ] `the colour literal guard > flags a hex literal` passes
- [ ] `the colour literal guard > flags rgb and hsl functions` passes
- [ ] `the colour literal guard > flags an oklch literal` passes
- [ ] `the colour literal guard > passes a token reference` passes
- [ ] `the colour literal guard > scans the app source and skips the token layer and the generated file` passes
- [ ] `the colour literal guard > finds no colour literal in the client source` passes
- [ ] `npm run --silent test` reports `Tests  10 passed (10)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] No `eslint-disable`, `prettier-ignore` or any other byte is added to `src/protocol/protocol.gen.ts`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
