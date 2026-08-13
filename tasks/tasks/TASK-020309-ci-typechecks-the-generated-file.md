---
schema: 2
id: TASK-020309
title: CI typechecks the generated file under strict
type: task
status: done
parent: STORY-0203
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [ci, typescript, protocol]
depends_on: [TASK-020308]
verify:
  - npx --yes --package=typescript@5.6.3 tsc --noEmit --strict web-client/src/protocol/protocol.gen.ts
  - grep -c 'tsc --noEmit --strict web-client/src/protocol/protocol.gen.ts' .github/workflows/build.yml | grep -qx 1
  - grep -c 'actions/setup-node' .github/workflows/build.yml | grep -qx 1
---

## Goal

The build workflow typechecks `web-client/src/protocol/protocol.gen.ts` with a pinned TypeScript
compiler under `strict`, so an emitter that produces something TypeScript will not accept fails a
pull request.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/build.yml` | modify |

## Scope

Two steps in the existing `check` job, inserted immediately after `actions/checkout@v4` and before
`actions/setup-java@v4`:

```yaml
      - uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Typecheck the generated protocol types
        run: npx --yes --package=typescript@5.6.3 tsc --noEmit --strict web-client/src/protocol/protocol.gen.ts
```

- `--package=` is required: `npx --yes typescript@5.6.3 tsc` fails with "could not determine
  executable to run", because the package is `typescript` and the binary is `tsc`.
- The version is pinned deliberately. `ADR-0020`: Node belongs in CI and never in a Gradle build,
  and this step folds into `web-client`'s own typecheck once `EPIC-03` gives the module a
  `package.json`.
- No `tsconfig.json`, no `package.json`, no `npm install`. `--strict` on the command line is the
  whole configuration, and `EPIC-03` owns the module's real build.

## Out of scope

- Any change to the emitter or to the generated file. If `tsc` rejects the file, stop and report
  it — the emitter is wrong and that is its own ticket, not an edit to the workflow to make the
  step pass.
- Linting, formatting or bundling TypeScript.

## Acceptance criteria

- [ ] `npx --yes --package=typescript@5.6.3 tsc --noEmit --strict web-client/src/protocol/protocol.gen.ts`
      exits 0 locally
- [ ] `.github/workflows/build.yml` contains exactly one `actions/setup-node` step and exactly one
      line running `tsc --noEmit --strict` on the generated file
- [ ] The `Run ./gradlew check` step is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
