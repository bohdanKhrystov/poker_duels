---
schema: 2
id: TASK-030505
title: A component reads the store through useDuelState, and re-renders only when it moved
type: task
status: ready
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030504]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +101 passed \(101\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a component the state the store holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 're-renders a component when a frame moves the state'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'does not re-render when the reducer had no opinion'
  - cd web-client && npm run check
---

## Goal

The one React-aware file in `src/store/`: `DuelProvider` puts a booted client within reach, and
`useDuelState()` subscribes a component to the store through `useSyncExternalStore` from React
core — no store library, per `ADR-0032` §3.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-provider.tsx` | create |
| `web-client/src/store/duel-provider.test.tsx` | create |
| `web-client/src/store/duel-store.ts` | read — `DuelStore` |
| `docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md` | read — §3 and §4 |

## Scope

- Create `web-client/src/store/duel-provider.tsx` with exactly this content:

  ```tsx
  import {
    createContext,
    useContext,
    useMemo,
    useSyncExternalStore,
    type ReactElement,
    type ReactNode,
  } from "react";
  import type { ClientMessage } from "../protocol";
  import type { DuelState } from "./duel-state";
  import type { DuelStore } from "./duel-store";

  interface DuelClientContext {
    readonly store: DuelStore;
    readonly send: (message: ClientMessage) => void;
  }

  const DuelContext = createContext<DuelClientContext | null>(null);

  /** Puts the booted client's store and send within reach of every screen. */
  export function DuelProvider(props: {
    store: DuelStore;
    send: (message: ClientMessage) => void;
    children: ReactNode;
  }): ReactElement {
    const value = useMemo(
      () => ({ store: props.store, send: props.send }),
      [props.store, props.send],
    );
    return (
      <DuelContext.Provider value={value}>{props.children}</DuelContext.Provider>
    );
  }

  function useDuelClient(): DuelClientContext {
    const client = useContext(DuelContext);
    if (client === null) {
      throw new Error("useDuelState and useSend need a DuelProvider above them");
    }
    return client;
  }

  /** The whole of the last state the server's frames folded into. */
  export function useDuelState(): DuelState {
    const { store } = useDuelClient();
    return useSyncExternalStore(store.subscribe, store.getState);
  }
  ```

- `useSyncExternalStore` takes two arguments. There is no third: `ADR-0026` has no SSR, so there
  is no server snapshot.
- `useDuelState` returns the **whole** `DuelState`. No selector parameter, no memo, no destructure
  at this layer — eight fields and a handful of screens do not need it, and the selector variant
  is additive the day profiling says otherwise.
- **No component in this file, or any other, receives or holds a `Connection`.** The provider
  takes a store and a send function, both already made by `bootDuelClient`.

## Out of scope

- `useSend` and the missing-provider test — `TASK-030506`. `useDuelClient` is written here because
  `useDuelState` needs it, and stays private.
- Any screen. `RoomCode` below is a two-line probe that lives in the test file.
- Calling `bootDuelClient`, `connectToDuelServer` or `openConnection` from anywhere in this file.

## Tests

`web-client/src/store/duel-provider.test.tsx`, one `describe("the duel provider")`.

Two one-line fixtures — write each on a single line, which is what Prettier keeps:

```tsx
const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;
const WELCOME = { type: "Welcome", deviceId: "d", protocolVersion: 2 } as const;
```

One probe component and one render helper:

```tsx
function RoomCode(): ReactElement {
  const state = useDuelState();
  return <p>{state.roomCode ?? "no room yet"}</p>;
}

function renderUnder(
  store: DuelStore,
  send: (message: ClientMessage) => void,
  child: ReactElement,
): void {
  render(
    <DuelProvider store={store} send={send}>
      {child}
    </DuelProvider>,
  );
}
```

`send`'s parameter **must** be typed `(message: ClientMessage) => void`; `(message: never) => void`
fails `tsc` with `TS2322`. The store is always one the test built with `createDuelStore()` and
drove with `store.apply(...)` — no socket exists in this file.

| Test | Proves |
| --- | --- |
| `hands a component the state the store holds` | `store.apply(ROOM_JOINED)` **before** rendering leaves `screen.getByText("ABCDEFGH")` findable |
| `re-renders a component when a frame moves the state` | rendering an empty store shows `"no room yet"`; `act(() => { store.apply(ROOM_JOINED); })` then leaves `"ABCDEFGH"` findable |
| `does not re-render when the reducer had no opinion` | a `const rendered = vi.fn()` **called** (never assigned to) in the component body has `toHaveBeenCalledOnce()` before and after `act(() => { store.apply(WELCOME); })` |

The third test counts renders by calling a spy, not by incrementing a variable: reassigning an
outer variable during render is an eslint error here — `react-hooks/globals`, *"Cannot reassign
variables declared outside of the component/hook"* — and `npm run check` fails on it.

Three tests. Ninety-eight exist, so the suite reports **101**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 101 passed (101)` | the three tests ran and the ninety-eight before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints (including the react-hooks rules), formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Replace the body of `useDuelState` with `return useDuelClient().store.getState();` (read once,
   never subscribe) → `re-renders a component when a frame moves the state` fails with
   `TestingLibraryElementError: Unable to find an element with the text: ABCDEFGH`. Revert.
2. In the test file only, delete the `act(() => { store.apply(WELCOME); })` and apply
   `ROOM_JOINED` instead → `does not re-render when the reducer had no opinion` fails, the spy
   having been called twice. Revert. (This one shows the assertion is about the reducer returning
   the same reference, not about `act` doing nothing.)

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel provider > hands a component the state the store holds` passes
- [ ] `the duel provider > re-renders a component when a frame moves the state` passes
- [ ] `the duel provider > does not re-render when the reducer had no opinion` passes
- [ ] `npm run --silent test` reports `Tests  101 passed (101)`
- [ ] `duel-provider.tsx` contains no `useEffect`, no `useState`, and no call to
      `connectToDuelServer`, `openConnection` or `bootDuelClient`
- [ ] `duel-state.ts`, `duel-store.ts` and `boot.ts` are byte-identical to `develop`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
