---
schema: 2
id: TASK-041406
title: One boot of the whole client, over the storage and the server it is handed
type: task
status: backlog
parent: STORY-0414
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, e2e, test, harness]
depends_on: [TASK-041401, TASK-041405]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx --reporter=verbose 2>&1 | grep -qF 'a first boot mints a device id and asks the server nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx --reporter=verbose 2>&1 | grep -qF 'a second boot over the same storage reads the profile the server holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx --reporter=verbose 2>&1 | grep -qF 'two storages reach two different profiles'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx --reporter=verbose 2>&1 | grep -qF 'the account screen is reachable from the first screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'
  - cd web-client && npm run check
---

## Goal

`bootClient` mounts the real lobby over the real providers, bound to a caller-supplied `Storage` and
a caller-supplied `accountServer`, so one browser can be booted, closed and booted again.

## Why this exists

`driveScriptedDuel` renders `<DuelProvider><Lobby /></DuelProvider>` and no account, profile or name
provider, so no screen this story asserts is reachable through it. This ticket builds the tree
`main.tsx` builds, with every seam a test can bind.

**Read this before writing a line of it.** `Lobby.tsx:6` is
`import { useHistory, useLadder, useSignedIn } from "../main";`. Those three come from `main.tsx`
module scope and are **not** props: `HistoryProvider` hands down a module constant bound to
`window.fetch` and the real `localStorage`, and `signedIn` is read once at import. There is no seam to
pass. The merged way past it is `Lobby.test.tsx:40` — a **partial** mock:

```ts
vi.mock("../main", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../main")>();
  return { ...actual, useHistory: () => …, useSignedIn: () => … };
});
```

**Do not copy `App.test.tsx:41`.** That one replaces the module **wholesale** and exports none of its
bindings; a fixture built on it cannot reach `main.tsx`'s real exports at all. Two `STORY-0412`
tickets were blocked and rewritten for citing it — the second because it cited the first's
description instead of its merged shape.

`vi.mock` is hoisted and file-scoped, so **the mock lives in the test file, never in `drive-arc.tsx`**.
The harness therefore cannot own it; it is handed a plain object and writes into it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/drive-arc.tsx` | create |
| `web-client/src/e2e/drive-arc.test.tsx` | create |

Read, and do not edit: `web-client/src/main.tsx`; `web-client/src/lobby/Lobby.test.tsx` (lines 26–48,
the mock and the comment explaining it); `web-client/src/e2e/account-server.ts`;
`web-client/src/e2e/drive-duel.tsx`.

## Scope

- `ArcWiring`: a mutable `{ history: ((q: HistoryQuery) => Promise<DuelPageRead>) | null; signedIn: boolean }`.
  The test file creates it inside `vi.hoisted`, its `vi.mock` factory reads it, and `bootClient`
  writes both fields on every boot.
- `bootClient(options)` takes `storage`, `server: AccountServer`, `wiring: ArcWiring` and
  `welcomeFrame: string`, and returns `{ container, socket, reloads }`.
- It wires exactly what `main.tsx` wires, with `server.fetch` where `main.tsx` has `window.fetch` and
  the given `storage` where `main.tsx` has `localStorage`: `authorizedFetch(server.fetch, storage)`
  for the four `/api/me` reads, and the **unwrapped** `server.fetch` for the four `AccountCalls` —
  `main.tsx:90` calls that one `plainFetch`, and `authorized-fetch.ts`'s contract forbids wrapping
  sign-in.
- The tree is `<AccountProvider><ProfileProvider><SetNameProvider><DuelProvider><Lobby /></DuelProvider></SetNameProvider></ProfileProvider></AccountProvider>`.
  `HistoryProvider`, `LadderProvider` and `SignedInProvider` are **absent** — the wiring object stands
  in for them, because they hand down module constants.
- `reloads()` returns how many times the client asked to reload. Sign-in's `reload` also does
  `window.history.replaceState(null, "", hashForScreen("account"))`, mirroring `main.tsx:98`'s
  `reloadAtAccount`; sign-out's only counts, mirroring `main.tsx:92`. Neither reloads anything: the
  caller boots again, which is what a reload is here.
- The boot replays `welcomeFrame` through the socket after `socket.open()`, so a boot has an identity.

## Out of scope

- Playing a duel. `TASK-041401` made `driveScriptedDuel` take a storage; the arc test uses that for
  the duel and this for the boots afterwards. Do not replay script steps here.
- Wrapping `LadderProvider` or reaching the leaderboard. No criterion in `STORY-0414` names it, and
  `wiring` carries no ladder read.
- Changing `main.tsx`. `STORY-0414` adds no production behaviour — its own *Out of scope* says so.

## Tests

`drive-arc.test.tsx`

| Test | Proves |
| --- | --- |
| `a first boot mints a device id and asks the server nothing` | After the `Welcome`, storage holds the seat's device id and `server.requests` is **empty** — the profile read runs at mount, before the frame arrives, and `readFromApi` short-circuits on a null device id without a request. The arc needs two boots because of this, and this is where that is written down. |
| `a second boot over the same storage reads the profile the server holds` | Booting again over the same storage renders the server's `displayName` and `coinBalance` in the `your profile` region, and `server.requests` now names `/api/me` and `/api/me/duels`. |
| `two storages reach two different profiles` | Seat 0's storage and seat 1's storage render **different** names and balances — asserted not equal, so a harness that ignored its `storage` argument fails. |
| `the account screen is reachable from the first screen` | Clicking `Account` and awaiting `findByLabelText("account")` puts the account screen on screen — the navigation shape every later ticket uses. |

**Awaiting a screen change:** click, then `await screen.findBy…`. jsdom queues `hashchange` as a
task, so flushing microtasks is not enough — measured: the hash reads `#/account` while the old screen
is still rendered. `App.test.tsx` uses `findBy*` for exactly this. **Do not write `setTimeout`**:
`virtual-time.test.ts` is a text scan that fails any test file containing that token without
`vi.useFakeTimers(`.

Reset `window.location.hash = ""` in a `beforeEach`, as `App.test.tsx:136–138` does, and `cleanup()`
between boots — the address and `use-screen.ts`'s subscriber set are module-global, so two mounted
clients share one address.

## Acceptance criteria

- [ ] `drive-arc.test.tsx` `a first boot mints a device id and asks the server nothing` passes
- [ ] `drive-arc.test.tsx` `a second boot over the same storage reads the profile the server holds` passes
- [ ] `drive-arc.test.tsx` `two storages reach two different profiles` passes
- [ ] `drive-arc.test.tsx` `the account screen is reachable from the first screen` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'` exits 0
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'` exits 0
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

Measured in this worktree on `develop` @ `a99222f4` with a scratch probe of exactly this shape. Each
of these is a prediction you should confirm, and report if it fails:

1. The partial `vi.mock("../main", importOriginal)` **works from `src/e2e/`** — the tree mounts, and
   `main.tsx`'s real module-scope boot runs (the `localStorage is not available` warning Node prints
   is that import, and the three merged `src/e2e/` files already trigger it).
2. A first boot over empty storage produced **zero** entries in the request log and rendered
   `No profile yet.` A second boot over the same storage rendered the profile and produced exactly
   two requests, `/api/me` and `/api/me/duels`. If your first boot makes requests, the `Welcome` is
   arriving before the mount and the two-boot structure the arc depends on is not what you built.
3. Make `bootClient` ignore its `storage` argument and build its own: `a second boot over the same
   storage reads the profile the server holds` and `two storages reach two different profiles` must
   both redden.
4. Point the four `AccountCalls` at `authorizedFetch(server.fetch, storage)` instead of the plain
   one. No test here reddens — nothing signs in yet. Say so rather than assuming it is covered:
   `TASK-041409` is where a wrapped sign-in becomes visible.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
