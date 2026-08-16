---
schema: 2
id: TASK-031109
title: The strip's read runs once above the tree, and a tree without one asks nothing
type: task
status: done
parent: STORY-0311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, react]
depends_on: [TASK-031108]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +354 passed \(354\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands down whatever the read answered'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands down nothing until the read lands'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers null where no provider is above it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads once for one mount, and not again on a re-render'
  - cd web-client && npm run check
---

## Goal

One place runs the strip's read and puts its answer within reach of the lobby — and a tree mounted
without that place renders no strip and asks the server for nothing, so every test that already
mounts the lobby keeps working untouched.

## Why this shape

`ADR-0032` settled that the socket's state lives in a store outside the tree, and named this exact
question as one it left open: *"how HTTP profile data reaches screens (`STORY-0311`) — it is not a
frame and does not enter this store"*. So the duel store is out, and what is left is the smallest
thing that satisfies the rest:

- **Not `bootDuelClient`.** That boot exists because a socket must outlive every remount and be
  exactly one per tab. A read-only `GET` needs neither, and widening the boot to carry it would put
  a second lifetime into the file `one-connection.test.ts` guards.
- **A context whose default is `null`**, not one that throws the way `useSend` does. Throwing would
  make the lobby unmountable without a provider and would rewrite `Lobby.test.tsx` and
  `App.test.tsx`; answering `null` means those trees render exactly what they render today.
- **One file.** If `EPIC-04` grows a real HTTP data layer, this is the file it replaces.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-provider.tsx` | create |
| `web-client/src/profile/profile-provider.test.tsx` | create |
| `web-client/src/store/duel-provider.tsx` | read — the context shape this mirrors, and how `useSend` differs |
| `web-client/src/profile/profile-strip.ts` | read — `ProfileStripState` |

## Scope

- Two exports and one context:

  ```tsx
  export function ProfileProvider(props: {
    read: () => Promise<ProfileStripState>;
    children: ReactNode;
  }): ReactElement;

  /** The strip's answer, or `null` before it lands and where no provider is above. */
  export function useProfileStrip(): ProfileStripState | null;
  ```

- The effect runs `props.read()` once, with `[read]` as its dependency list, and a cleanup that
  drops an answer arriving after unmount:

  ```tsx
  useEffect(() => {
    let live = true;
    void read().then((answer) => {
      if (live) setState(answer);
    });
    return (): void => {
      live = false;
    };
  }, [read]);
  ```

- `read` must therefore be a **stable reference**. `TASK-031110` passes a module-scope constant from
  `main.tsx`; an inline arrow would re-run the effect on every render, which is what
  `reads once for one mount, and not again on a re-render` is there to catch.
- The provider holds `ProfileStripState | null` in `useState` and provides it unchanged.

## Out of scope

- Deduplicating React's development-only double invoke under `<StrictMode>`. It costs a second
  `GET` in dev; `GET` is idempotent and reaches no room, unlike the `JoinRoom` that made
  `TASK-030507` count frames. Pinning React's double-invoke would pin React's internals.
- Refreshing after a duel, polling, or invalidating. One read per mount, and `EPIC-04` owns
  anything live.
- Reaching for `window.fetch` or `localStorage`. This file takes a function and calls it.
- Mounting the provider — `TASK-031110`.

## Tests

`web-client/src/profile/profile-provider.test.tsx`, describe block `"the profile provider"`. Drive
it with a tiny consumer declared in the file that renders what `useProfileStrip()` answers, and
resolve with `await screen.findByText(...)` — **never a timer**: `virtual-time.test.ts` fails the
build on a test file that names one without installing fake ones.

| Test | Proves |
| --- | --- |
| `hands down whatever the read answered` | **two different answers** in two renders: a profile with a balance of `7` reaches the consumer, and a `no-profile` reaches it in a second render. One answer could not tell a provided value from a constant |
| `hands down nothing until the read lands` | with a promise that has not resolved, the consumer sees `null` — asserted before any `await`, so the strip renders nothing rather than flashing an empty profile |
| `answers null where no provider is above it` | the consumer rendered alone answers `null` and **does not throw** — the property that keeps `Lobby.test.tsx` and `App.test.tsx` working unchanged |
| `reads once for one mount, and not again on a re-render` | a `vi.fn()` read, one mount, then `rerender` with the same props: `toHaveBeenCalledTimes(1)` |

Four tests added. Three hundred and fifty exist, so the suite reports **354**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 354 passed (354)` | four ran and nothing else moved |
| the four `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks, lints (including `react-hooks`), is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Drop the dependency list from the effect → `reads once for one mount, and not again on a
   re-render` fails with two calls.
2. Make the context default `{ kind: "unavailable" }` instead of `null` → `answers null where no
   provider is above it` fails.
3. Set the initial state to `{ kind: "no-profile" }` → `hands down nothing until the read lands`
   fails, and the strip would flash *No profile yet* on every load.

## Acceptance criteria

- [ ] `the profile provider > hands down whatever the read answered` passes, with two answers
- [ ] `the profile provider > hands down nothing until the read lands` passes
- [ ] `the profile provider > answers null where no provider is above it` passes
- [ ] `the profile provider > reads once for one mount, and not again on a re-render` passes
- [ ] `useProfileStrip` throws nowhere and reads no global
- [ ] `profile-provider.tsx` names neither `fetch` nor `localStorage`
- [ ] No test file outside `web-client/src/profile/` differs
- [ ] `npm run --silent test` reports `Tests  354 passed (354)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
