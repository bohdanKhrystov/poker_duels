---
schema: 2
id: TASK-041112
title: The write reaches the tree the same way the read already does
type: task
status: ready
parent: STORY-0411
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, profile, wiring, identity]
depends_on: [TASK-041111]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +408 passed \(408\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands the write down to the tree below it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers null where no provider is above, and asks for nothing'
  - cd web-client && npm run check
---

## Goal

A component under the tree can obtain the one function that sets a name, and `main.tsx` binds it to
the browser's `fetch` and `localStorage` — the only place either is named.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/set-name-provider.tsx` | create |
| `web-client/src/profile/set-name-provider.test.tsx` | create |
| `web-client/src/main.tsx` | modify — one binding, one wrapper |

Read, not edited: `web-client/src/profile/profile-provider.tsx` (the shape this copies),
`web-client/src/profile/set-name.ts`.

## Scope

```tsx
export function SetNameProvider(props: {
  setName: (name: string) => Promise<SetNameOutcome>;
  children: ReactNode;
}): ReactElement;

export function useSetName(): ((name: string) => Promise<SetNameOutcome>) | null;
```

- **A provider of its own, not a second prop on `ProfileProvider`.** A required prop there would
  break three merged test files at `tsc` for no gain, and an optional one is how a wiring bug hides.
- `useSetName()` answers `null` where no provider is above it, exactly as `useProfileStrip()` does.
  Every merged test that renders the lobby without this provider keeps passing, and the lobby's rule
  in `TASK-041113` is *no function, no surface*.
- `main.tsx` builds the binding at **module scope**, beside `readProfile`, for the same reason: a
  reference that changed on every render would be a new function on every render.

```ts
const setName = (name: string): Promise<SetNameOutcome> =>
  setDisplayName({
    fetch: (path, init) => window.fetch(path, init),
    storage: localStorage,
    name,
  });
```

- **`localStorage` is named in `main.tsx` and nowhere else in this story.** Under Vitest, Node's own
  `localStorage` global shadows jsdom's and is inert without `--localstorage-file`, so a module that
  reached for it at import time would be undefined in tests while `sessionStorage` worked
  (`DEC-032`). `main.tsx` is imported by no test, which is what makes it the safe place.
- The wrapper nests inside `ProfileProvider`, above `DuelProvider`. Order is not load-bearing; being
  above `App` is.

## Out of scope

- Rendering anything. **A refusal, not an omission:** the lobby decides where the surface goes
  (`TASK-041113`), and a provider that rendered would put the decision in two files.
- Changing `ProfileProvider`, `profile-provider.test.tsx` or `ProfileStripState`.
- Any refresh of the profile after a name is set — see `TASK-041110`'s out of scope.

## Tests

`web-client/src/profile/set-name-provider.test.tsx`, describe block `"the set-name provider"`,
modelled on `profile-provider.test.tsx`'s probe component.

| Test | Proves |
| --- | --- |
| `hands the write down to the tree below it` | A probe under the provider gets the identical function reference it was given, and calling it with `"Ada"` reaches the spy with `"Ada"` — **and a second render with a different function hands down that one instead**. Fails against a provider that wraps its prop in a new closure per render, and against one that ignores the prop; one function could not tell a passthrough from a captured constant |
| `answers null where no provider is above, and asks for nothing` | A probe rendered bare gets `null` and throws nothing. Fails against a hook that throws when unprovided, which would take every merged lobby test down with it |

Two tests added to 406, so the suite reports **408**.

## Acceptance criteria

- [ ] `the set-name provider > hands the write down to the tree below it` passes
- [ ] `the set-name provider > answers null where no provider is above, and asks for nothing` passes
- [ ] `main.tsx` wraps the tree in `SetNameProvider` and builds its binding at module scope
- [ ] `grep -rl 'localStorage' web-client/src/profile/` lists no file
- [ ] `profile-provider.tsx` and `profile-provider.test.tsx` are unmodified
- [ ] `npm run --silent test` reports `Tests  408 passed (408)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
