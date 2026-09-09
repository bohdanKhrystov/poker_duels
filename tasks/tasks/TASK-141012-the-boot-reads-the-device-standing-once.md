---
schema: 2
id: TASK-141012
title: The boot reads the device standing once, beside the profile
type: task
status: done
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-141011]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (37 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/device-standing-provider.test.tsx 2>&1 | grep -qF "device-standing-provider.test.tsx  (5 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - sh -c 'cd web-client && test $(grep -c "fetch: plainFetch" src/main.tsx) -eq 8'
  - sh -c 'cd web-client && test $(grep -c "fetch: apiFetch" src/main.tsx) -eq 5'
  - sh -c 'cd web-client && test $(grep -c "readDeviceStanding" src/main.tsx) -eq 2'
  - sh -c 'cd web-client && test $(grep -c "DeviceStandingProvider" src/main.tsx) -eq 3'
  - sh -c 'cd web-client && ! grep -q "readDeviceStanding({ fetch: apiFetch" src/main.tsx'
  - git diff --exit-code -- web-client/src/account/device-standing.ts web-client/src/account/device-standing-provider.tsx web-client/src/lobby/Lobby.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`main.tsx` binds `readDeviceStanding` to the browser's raw fetch and its storage, once at module
scope, and mounts `DeviceStandingProvider` over the tree beside `ProfileProvider`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

`App.test.tsx` is in the budget because it **pins a count of `main.tsx`'s own source** —
`occurrencesIn(mainSource, "fetch: plainFetch")` is **7** at `c6a41e6d`, and this ticket's binding
makes it 8. That is a merged test this change invalidates, and it moves here.

Read, do not edit: `web-client/src/account/device-standing.ts`,
`web-client/src/account/device-standing-provider.tsx`.

## Scope

- A module-scope binding beside `readProfile`, with the same comment about why it is at module
  scope (one stable reference means one mount, one read):

  ```ts
  const readStanding = (): Promise<boolean> =>
    readDeviceStanding({ fetch: plainFetch, storage: localStorage });
  ```

  **`plainFetch`, not `apiFetch`.** `readDeviceStanding` sets `Authorization: Bearer` itself, so
  the wrapper would attach a second one behind its back. That is the same rule `signIn`, `signUp`,
  `signOut` and `revokeThisDevice` already follow, and it is why the `fetch: plainFetch` count goes
  to **8** while `fetch: apiFetch` stays at **5**.
- `<DeviceStandingProvider read={readStanding}>` in the render tree, immediately inside
  `AccountProvider` and outside `ProfileProvider`, so the account screen's two facts arrive from
  two providers with one nesting to read.
- `App.test.tsx`:
  - the `fetch: plainFetch` count becomes **8**, and the comment above it — *"a binding for every
    account call whose endpoint takes no authentication … these four plus the three recovery
    calls"* — gains the device-standing read, because a count with a stale explanation is a count
    nobody will dare change next time;
  - one new test, below.

## Out of scope

- **Reading the value anywhere.** `Lobby` reads the hook in `TASK-141014`; the provider sits above
  a tree that ignores it for one merge. `Lobby.tsx` is in the `git diff --exit-code` gate.
- **`device-standing.ts` and `device-standing-provider.tsx`.** Landed already; same gate.
- **Adding an export to `main.tsx`.** `App.test.tsx`'s `vi.mock("./main")` is an explicit factory,
  so a new export that `Lobby` imports would fail 24 of that file's 36 tests (`TASK-140714`
  measured it). The provider and its hook live in their own module for exactly this reason, and
  nothing here changes it.
- **`nullStorage`.** `readProfile` binds `localStorage` directly and this binding matches it; the
  fallback exists for `askStorage` and `signedIn`, which are read at module scope rather than
  awaited.

## Tests

`App.test.tsx`, **36 today → 37**.

The merged test that moves: `wires every unauthenticated account call through the raw fetch` — its
`expect(occurrencesIn(mainSource, "fetch: plainFetch")).toBe(7)` becomes `.toBe(8)`, and its
comment names the eighth. Its four `toMatch` assertions and its
`expect(occurrencesIn(mainSource, "const plainFetch")).toBe(1)` are **unchanged**.

| Test | Proves |
| --- | --- |
| `reads the device standing through the raw fetch, never the wrapper` | both polarities against `main.tsx`'s source: `toMatch(/readDeviceStanding\([^}]*fetch: plainFetch/)` and `not.toMatch(/readDeviceStanding\([^}]*fetch: apiFetch/)`. The `[^}]*` stops at the argument object's closing brace, so a binding that named `apiFetch` cannot pass by reaching the next binding's `plainFetch` — the file's own comment explains that idiom and this test reuses it |

`device-standing-provider.test.tsx` stays at **5** and
`claimed-here-recovered-there.test.tsx` at **8** — the second because it now boots a tree with one
more provider in it, and its `500`-answering fake server is what makes the answer `false`, which is
that arc's correct answer.

## What would still pass if the coder were wrong

- **Binding `apiFetch`** typechecks, and every request still carries a bearer token, so the arc
  test and the whole suite stay green. Two gates see it: the new test's negative half, and the
  `fetch: apiFetch` count pinned at **5**. Neither alone is enough — the count would also pass if
  the binding named some third fetch.
- **Mounting the provider without binding a read** — `<DeviceStandingProvider read={() => Promise.resolve(false)}>`
  — makes the product answer `false` forever, which no behavioural test in this client can
  distinguish, because nothing reads the hook yet. The `grep -c "readDeviceStanding" -eq 2` gate is
  what refuses it: the import and the one call, and no fewer.
- **Building the read inside a component** rather than at module scope re-runs it on every render.
  Nothing here fails; `TASK-141011`'s `reads once per mount` is the test that would, and it is why
  the KDoc rule lives on the provider.
- **A count gate alone would have been enough for none of this**, which is why the ticket carries
  four counts and a two-sided regex rather than one number.

## Acceptance criteria

- [ ] `App.test.tsx` reports `(37 tests)` and all pass
- [ ] `reads the device standing through the raw fetch, never the wrapper` passes
- [ ] `grep -c "fetch: plainFetch"` in `main.tsx` is 8 and `grep -c "fetch: apiFetch"` is 5
- [ ] `grep -c "readDeviceStanding"` is 2 and `grep -c "DeviceStandingProvider"` is 3
- [ ] `! grep -q "readDeviceStanding({ fetch: apiFetch"` exits 0
- [ ] `device-standing-provider.test.tsx` reports `(5 tests)` and
      `claimed-here-recovered-there.test.tsx` reports `(8 tests)`, all passing
- [ ] `git diff --exit-code` over `device-standing.ts`, `device-standing-provider.tsx` and
      `Lobby.tsx` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
