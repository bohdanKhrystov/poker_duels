# ADR-0032 — React subscribes to a store it does not own, and the tab's one connection is wired at boot

- **Status:** Accepted
- **Date:** 2026-08-14
- **Resolves:** `DEC-033` — how does React read the duel store, and where does the single
  `Connection` live?
- **Unblocks:** `STORY-0305` (the lobby — the first story that renders from state and sends a
  `ClientMessage`), and gives `STORY-0306`–`STORY-0312` the surfaces they read and send through
- **Builds on:** [`ADR-0026`](ADR-0026-vite-and-npm-drive-the-web-client.md), whose toolchain
  (React 18, Vitest, jsdom, testing-library) this arranges and adds nothing to; `STORY-0303`'s
  `connectToDuelServer` and `STORY-0304`'s pure reducer, which were built deliberately ignorant of
  each other and are joined here
- **Leaves open:** reconnection and what survives a reload (`STORY-0310`), and how HTTP profile
  data reaches screens (`STORY-0311`) — neither travels through this store

## Context

`STORY-0304` shipped `web-client/src/store/duel-state.ts`: a pure reducer
`applyServerMessage(state, message): DuelState` plus `initialState()`, with no React in it, on
purpose. `STORY-0303` shipped `web-client/src/protocol/`, whose entry point
`connectToDuelServer(onMessage): Connection` opens the tab's real `WebSocket`, speaks `Hello`, and
forwards every decoded frame — `Welcome` and `Failure` included — to `onMessage`. Nothing joins
the two, no screen exists, and `STORY-0305` cannot be planned until something does. Five forces
shape how:

1. **StrictMode double-mounts everything, and the requirement is "exactly once".** `main.tsx`
   renders under `<React.StrictMode>` (React `^18` per `web-client/package.json`), which in
   development mounts every component, runs its effects, tears them down, and runs them again.
   `STORY-0305`'s acceptance criterion is: *"Booting with `?room=CODE` sends exactly one
   `JoinRoom`, after `Welcome` and not before."* A design where a screen's effect opens the
   connection or sends the join does both twice on every dev boot — and once a duel is live, an
   effect-owned socket turns every remount into a server-visible disconnect:
   [`ADR-0013`](ADR-0013-disconnect-grace-period.md)'s grace period starts,
   [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md)'s presence frames tell the
   opponent, and [`ADR-0018`](ADR-0018-a-second-socket-adopts-the-seat.md)'s adoption path runs —
   traffic production never produces. Whatever answers this decision must make exactly-once
   structural, not something each screen re-solves with a ref and a cleanup dance.
2. **`Connection.status` cannot be rendered.** It is a mutable getter that changes when frames
   arrive and notifies nobody. A component reading it shows a value React was never told changed —
   stale by construction. Something must translate frames into renderable state, and the codebase
   already has exactly one discipline for that: the reducer, whose story is titled *state is the
   last frame the server sent*.
3. **The reducer is inbound-only, but `ClientMessage` has to leave somehow** — and one send is
   triggered by a *message*, not by a click: `JoinRoom` upon `Welcome`. That send needs a home
   that is not a screen, or force 1 bites.
4. **`ADR-0026` fixed the toolchain.** A store library is a new runtime dependency — a version to
   track and a supply chain to trust — and adding one is a real cost that needs naming. React 18
   ships `useSyncExternalStore` in core, whose contract (`subscribe(listener) => unsubscribe`, a
   *cached* `getSnapshot`) is exactly what a store over an immutable reducer provides for free.
5. **The no-network test discipline is the epic's asset.** The protocol module ships `FakeSocket`
   and takes its socket and `Storage` by injection; the reducer tests render nothing. If joining
   the halves means "mount the whole app and hope", the previous two stories' purchase is spent.

## Decision

**One `DuelStore` and one `Connection` per browser tab, created by framework-free boot wiring that
`main.tsx` calls once, outside the component tree. Components read through `useDuelState()` —
`useSyncExternalStore` from React core, no store library — and send through `useSend()`, from
event handlers only. No component ever holds the `Connection`, and no send is ever issued from an
effect.**

### 1. The store gains a framework-free subscribable shell

`web-client/src/store/duel-store.ts`:

```ts
export interface DuelStore {
  getState(): DuelState;
  subscribe(listener: () => void): () => void;
  apply(message: ServerMessage): void;
}

export function createDuelStore(): DuelStore;
```

- `apply` folds the message through `applyServerMessage`, then notifies every listener — unless
  the reducer returned the identical reference (as it does today for `Welcome`), in which case
  nobody is notified and nothing re-renders.
- `duel-state.ts` is untouched and stays free of React types. Its immutability is what makes
  `getState` a valid cached snapshot for `useSyncExternalStore`: the reference moves only when a
  message changed something, so React never sees a fresh object for unchanged state — the one
  classic way this hook is misused.
- `subscribe` returns the unsubscriber. This is deliberately the exact contract
  `useSyncExternalStore` consumes, and the common subset every store library also speaks.

### 2. Boot wiring owns the connection, outside the tree

`web-client/src/store/boot.ts`, framework-free:

```ts
export interface DuelClient {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
}

export function bootDuelClient(options: {
  connect: (onMessage: (message: ServerMessage) => void) => Connection;
}): DuelClient;
```

- `bootDuelClient` calls `options.connect` **exactly once**, with the one `onMessage` in the
  client: fold the frame into the store, then run the boot reactions.
- **`main.tsx` is the composition root and the only file that passes the real
  `connectToDuelServer`.** It boots before rendering, then renders
  `<DuelProvider store={client.store} send={client.send}>` inside the existing `StrictMode`. The
  connection lives for the tab's life; nothing closes it — closing the tab is the close. No
  component, hook or effect anywhere may call `connectToDuelServer` or `openConnection`.
- **Message-triggered sends are boot reactions, not screen effects.** `STORY-0305` implements the
  first one inside the boot module: the room code read from the URL travels into `bootDuelClient`
  as a plain option (the story chooses its exact shape), and the reaction sends `JoinRoom` when
  `Welcome` arrives. Exactly-once falls out structurally — one boot per tab, one `Welcome` per
  socket — with zero guards, zero refs, zero cleanup functions.
- StrictMode is kept at full strength and becomes harmless: a dev remount only unsubscribes and
  resubscribes a listener. It cannot open a socket, close one, or send a frame, because none of
  those live in the tree.

### 3. Components read through `useDuelState()` and send through `useSend()`

`web-client/src/store/duel-provider.tsx` — the only React-aware file in `src/store/`:

```tsx
export function DuelProvider(props: {
  store: DuelStore;
  send: (message: ClientMessage) => void;
  children: ReactNode;
}): ReactElement;

export function useDuelState(): DuelState;
export function useSend(): (message: ClientMessage) => void;
```

- `useDuelState` is `useSyncExternalStore(store.subscribe, store.getState)` over the context's
  store (no server snapshot — `ADR-0026` has no SSR). It returns the whole `DuelState`: seven
  fields and a handful of screens do not need selectors, and a selector variant is additive the
  day profiling says otherwise.
- `useSend` returns the boot-created function, whose identity never changes. `Connection.send`
  already refuses to speak to a server that answered `outdated`, so the surface handed to screens
  is safe as-is.
- **Screens send from event handlers only** — never from render, never from `useEffect`. Any
  future "send when the state says X" is a boot reaction by definition, not a screen effect.
- **Screens never see the `Connection`** — not `send` on it, not `close`, not `status`. When a
  screen must render a connection fact (`Failure{UNKNOWN_ROOM}`, the outdated-client notice), that
  fact enters `DuelState` through a new reducer case in the ticket that needs it — the exact path
  `STORY-0304` forecast for `Failure`. One render source, no exceptions: if a screen shows it, the
  reducer folded it.

### 4. Every layer tests without a browser

- **The store**: plain Vitest, no DOM — unchanged from `STORY-0304`.
- **The boot wiring**: `bootDuelClient({ connect: (onMessage) => openConnection({ socket:
  fake.asWebSocket(), storage: fakeStorage, onMessage }) })` with the protocol module's
  `FakeSocket`. The test drives `fake.open()` and `fake.receive(...)` and asserts over
  `fake.sent`. *This* is the test that proves "exactly one `JoinRoom`, after `Welcome` and not
  before" — a count over `sent`, with no React mounted and no network touched.
- **Components**: `render(<DuelProvider store={store} send={spy}>…)` with a store the test
  constructed and drives via `store.apply(message)`, asserting markup and that clicks called `spy`
  with the right `ClientMessage`. jsdom and testing-library per `ADR-0026`; no socket exists at
  all.
- **The whole loop, when a story needs it**: the provider fed by a booted client over a
  `FakeSocket` — still jsdom, still no network. The automated ceiling stays where `DEC-024` left
  the question.
- A structural test in the `STORY-0304` tradition asserts the framework-free store modules
  (`duel-state.ts`, `duel-store.ts`, `boot.ts`) import nothing from `react`.

### 5. Where the files land

All of it in `web-client/src/store/`, which becomes "state and its wiring": `duel-state.ts`
(existing, pure, untouched here), `duel-store.ts` and `boot.ts` (new, framework-free), and
`duel-provider.tsx` (new, the single React-aware file). `main.tsx` grows the one
`bootDuelClient(...)` call and the provider element, and nothing else — it stays the untested,
logic-free entry point. Where screen components live is the screen stories' own concern; this ADR
places only the joint.

## Consequences

**What it buys.** "Exactly once" is a property of the architecture, not a guard in every screen:
there is one boot per tab, so there is one connection, one `Hello`, and one reaction to `Welcome`,
and StrictMode's double-mount cannot reach any of them. StrictMode itself is kept at full strength
in development, where it now catches real bugs instead of manufacturing socket churn. The
dependency set of `ADR-0026` is untouched — the subscription primitive ships inside React 18. The
no-network test standard of the previous two stories extends to components unchanged, and the
exactly-once criterion is provable by counting frames on a `FakeSocket`. And `ADR-0002`'s
discipline gets a client-side enforcement point: the only thing a screen can read is the folded
frame stream, so a screen *cannot* render a game fact the server did not send.

**What it costs.**

- **Whole-state subscription re-renders every subscribed component on every frame.** An `Events`
  burst during a hand re-renders whatever else is mounted. Accepted at the scale of seven fields
  and heads-up traffic; the escape hatch (a selector-taking hook) is named and additive, and
  nothing must be unwound to add it.
- **Roughly thirty owned lines of store whose notify-and-cache contract we must keep correct
  ourselves.** The classic `useSyncExternalStore` failure — a `getSnapshot` that fabricates a
  fresh object and loops the render — is prevented today by the reducer's immutability, but only
  review keeps it that way. A library would have carried that correctness for us.
- **"No send from an effect" is discipline, not mechanism.** No lint rule enforces it; reviewers
  must. A custom ESLint rule is possible later and is not bought now.
- **Boot reactions re-fire per `Welcome`.** Today that is once, because there is one socket and
  reconnection does not exist. When `STORY-0310` adds it, every new socket re-`Hello`s and
  re-`Welcome`s, and the reaction set runs again — plausibly exactly what rejoining wants, given
  `ADR-0018` adopts the seat, but `STORY-0310` must decide that on purpose. It is named here so it
  is chosen, not discovered.
- **`main.tsx` stays outside the test net** (it touches `document` and the real socket), which is
  why `boot.ts` exists as its own testable file. Logic that drifts into `main.tsx` escapes
  coverage; keeping it composition-only is a review obligation.

**What it forecloses.** Little on the store side, deliberately: `DuelStore`'s contract is the
subset every store library speaks, so swapping in Zustand later is mechanical, and per-slice
selectors are additive — cheapness to reverse weighed in this shape's favour. The real commitment
is **one connection per tab, owned by boot**: a screen can never own a private socket, and two
simultaneous duels in one tab would mean reworking the wiring layer. That is the product's shape —
a duel is the tab — so it is foreclosed knowingly. On timing: this decision is free today and
expensive after the first screen, because the first screen sets the pattern every later screen
copies; if `STORY-0305` shipped a connect-in-effect with a ref guard, `STORY-0306`–`STORY-0312`
would each copy it and undoing it would cost every screen. That is the reason to decide now — the
same "only cheap moment" argument `STORY-0304` made for the reducer — not a reason it went this
way.

**What this does not settle.** Reconnection and reload survival (`STORY-0310` — the `connect`
parameter on `bootDuelClient` is where it will plug in). How HTTP profile data reaches screens
(`STORY-0311` — it is not a frame and does not enter this store, per `STORY-0304`'s own scope).
Which fields the reducer grows for `Welcome` and `Failure` — this ADR fixes the *path* (through
the reducer, never a second render source), and the tickets that need the fields shape them.

## Alternatives considered

**A `useReducer` in a context provider.** The strongest case: it is the answer React itself
documents, it needs no concept beyond core, `applyServerMessage` drops into
`useReducer(applyServerMessage, undefined, initialState)` unchanged, and it has the largest
training-data corpus of any option here. Rejected because state that must outlive the tree cannot
live inside it. The socket must reach `dispatch`, so either the connection is created in an
effect — and StrictMode then opens and closes a real socket on every dev mount, with the
disconnect theatrics of force 1 — or `dispatch` is smuggled out to module code through a ref,
which hands the boot layer a binding that goes stale on every remount and reinvents this ADR's
store with worse ownership. Either way, the reducer's framework-free testability is spent on the
first story that was supposed to benefit from it.

**A store library — Zustand, Redux Toolkit or Jotai.** The strongest case: subscription code we
do not have to keep correct, selector equality out of the box, devtools, and an enormous corpus —
and Zustand in particular is tiny and itself built over `useSyncExternalStore`, so it is this
design with the sharp edges filed off. Rejected because `ADR-0026` fixed the client's dependency
set and a runtime store library is a real addition — a version to track and a supply chain to
trust — bought here to replace roughly thirty lines whose hard part, the reducer, already exists,
and whose one genuine trap, the uncached snapshot, our immutable state removes by construction.
Selectors and devtools solve problems a seven-field state with a handful of screens does not have.
It is also the cheapest alternative to adopt later, precisely because `DuelStore` speaks the same
contract — which, on thin evidence, is an argument for not spending the dependency now.

**Components import a module-level `connection` singleton.** The strongest case: the shortest
code that works — `import { connection }` then `connection.send(...)` — and one-socket-per-tab
falls out of module semantics with no provider ceremony. Rejected because a singleton whose module
opens a socket makes every test that transitively imports it choose between a real `WebSocket` and
`vi.mock` — which is "mount the whole app and hope" with extra steps — and because it hands every
screen `close()` and the unsubscribable `status`. The provider is the same convenience one seam
later, and that seam is what the component tests are made of.

**Render from `Connection.status`, or make the connection itself subscribable.** The strongest
case: the connection already knows `connecting`/`ready`/`refused`/`outdated`, and folding those
facts into `DuelState` looks like duplicating state the protocol module owns. Rejected because
`status` is a mutable getter with no change notification — a component reading it renders a value
React was never told changed, stale or torn by construction — and making `Connection` subscribable
builds a second store with a second subscription surface, splitting "what may a screen render"
across two sources of truth. `openConnection` already forwards every decoded frame, `Welcome` and
`Failure` included; the reducer is the one translation of frames into renderable state, and
connection facts a screen needs go through it like everything else.

**`JoinRoom` from the lobby's effect, behind a ref guard.** The strongest case: it keeps the
behaviour beside the screen that owns the URL's meaning, and a `useRef` boolean around an effect
is four lines that the React documentation itself shows as the StrictMode idiom. Rejected because
it is exactly the per-screen dance the requirement forbids, and because the guard encodes the
wrong unit: *once per component instance*, where the requirement is *once per `Welcome` per tab*.
It is wrong today under a double-mounted lobby, wrong again if anything remounts the screen after
`Welcome` has passed (the effect sees status as state, not as an event), and it would be
re-invented — independently, each copy subtly different — by every future screen with a
once-per-boot send. Making the send a boot reaction deletes the whole class of bug instead of
guarding one instance of it.
