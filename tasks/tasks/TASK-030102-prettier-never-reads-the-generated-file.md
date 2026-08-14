---
schema: 2
id: TASK-030102
title: Prettier formats the client and never reads the generated file
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, build, toolchain, protocol]
depends_on: [TASK-030101]
verify:
  - cd web-client && npm run format:check
  - cd web-client && ./node_modules/.bin/prettier --ignore-path /dev/null --list-different src/protocol/protocol.gen.ts | grep -c 'protocol.gen.ts' | grep -qx 1
  - cd web-client && ./node_modules/.bin/prettier --list-different . | grep -c 'protocol.gen.ts' | grep -qx 0
  - grep -qx 'src/protocol/protocol.gen.ts' web-client/.prettierignore
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm run format:check` passes in `web-client/`, and Prettier cannot reach
`src/protocol/protocol.gen.ts` even though it would rewrite it if it could.

## Why this lands before anything else is written

Prettier arrives first so that every file the rest of the story creates is written under its rules.
A formatter introduced late has to reformat files that belong to other tickets, which is exactly the
budget overrun this project splits tickets to avoid.

## Files

| File | Action |
| --- | --- |
| `web-client/.prettierignore` | create |
| `web-client/package.json` | modify |

## Scope

- `web-client/.prettierignore` contains exactly three lines:

  ```
  dist
  package-lock.json
  src/protocol/protocol.gen.ts
  ```

  The generated path is `ADR-0026`'s wording verbatim. `dist` is there because Prettier resolves
  ignore files relative to `web-client/` and never sees the repository's `.gitignore`, so a built
  bundle would otherwise be checked. `package-lock.json` is npm's to format, not Prettier's.
- Add two scripts to `package.json`: `"format": "prettier --write ."` and
  `"format:check": "prettier --check ."`. The second is the exact command `ADR-0026` puts in the
  aggregate `check` script.
- Run `npm run format` once so the committed `package.json` is already Prettier-clean.

## Out of scope

- `prettier-plugin-tailwindcss` and any `.prettierrc`. The plugin needs a Tailwind stylesheet to
  read and `STORY-0302` owns the stylesheet; it brings the plugin and the config file with it.
  Prettier's defaults are the whole configuration here.
- ESLint — `TASK-030107`. The two tools' ignore lists are separate on purpose.
- **Adding any header to the generated file.** No `// prettier-ignore`, no `/* eslint-disable */`.
  `ADR-0026` is explicit: its bytes belong to the Kotlin emitter, and a header it did not emit fails
  `verifyProtocolTypes`. Ignore by path, never by pragma.

## Proof

The trap is an exclusion that proves nothing because the file happens to be formatted the way
Prettier likes anyway. Two verify commands together close it:

| Command | Proves |
| --- | --- |
| `prettier --ignore-path /dev/null --list-different src/protocol/protocol.gen.ts` lists the file | Prettier **would** rewrite it — seven of its lines are past the default 80 columns, so the exclusion is load-bearing rather than decorative |
| `prettier --list-different .` never lists the file | with `.prettierignore` in place, Prettier does not visit it |

Watch it fail before you believe it: delete the `src/protocol/protocol.gen.ts` line from
`.prettierignore`, run `npm run format:check`, and it goes red on the generated file. Restore the
line. Say in the PR that you did this and what the failure said.

If `--ignore-path /dev/null` is rejected by the installed Prettier, point it at any file with no
matching patterns instead, and say so — the assertion is what matters, not the flag.

## Acceptance criteria

- [ ] `cd web-client && npm run format:check` exits 0
- [ ] `.prettierignore` contains the line `src/protocol/protocol.gen.ts`, and `dist` and
      `package-lock.json`
- [ ] Prettier reports the generated file as different when the ignore file is bypassed
- [ ] Prettier does not list the generated file when run over the directory
- [ ] `./gradlew :poker-server:verifyProtocolTypes` passes — no byte of the generated file moved
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
