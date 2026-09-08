---
schema: 2
id: TASK-141011
title: A provider holds the answer, and `false` until it lands
type: task
status: backlog
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-141010]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/device-standing-provider.test.tsx 2>&1 | grep -qF "device-standing-provider.test.tsx  (5 tests)"'
  - sh -c 'cd web-client && ! grep -q "readDeviceStanding" src/account/device-standing-provider.tsx'
  - sh -c 'cd web-client && ! grep -rq "DeviceStandingProvider" src/main.tsx src/lobby/Lobby.tsx src/account/AccountScreen.tsx'
  - sh -c 'cd web-client && test $(grep -c "useState" src/account/device-standing-provider.tsx) -eq 2'
  - sh -c 'cd web-client && test $(grep -c "createContext<boolean>(false)" src/account/device-standing-provider.tsx) -eq 1'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (36 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

One provider runs a `() => Promise<boolean>` once above the tree and puts its answer within reach of
the lobby, answering **`false`** until it lands and `false` where no provider is above. Nothing
mounts it yet.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/device-standing-provider.tsx` | create |
| `web-client/src/account/device-standing-provider.test.tsx` | create |

Read, do not edit: `web-client/src/profile/profile-provider.tsx` (the shipped shape this mirrors),
`docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md` §§6–7.

## Scope

```tsx
export function DeviceStandingProvider(props: {
  read: () => Promise<boolean>;
  children: ReactNode;
}): ReactElement;

export function useSignOutHandsANewProfile(): boolean;
```

- A `createContext<boolean>(false)` — the default **is** `ADR-0135` §6's rule, written where it
  cannot be forgotten: *"A client that has not been told reads `false`."* A consumer with no
  provider above it therefore keeps its profile, and so does one whose read has not returned.
- `useState<boolean>(false)`, one mount effect that awaits `read()` and adopts the answer, and the
  same `live` ref `ProfileProvider` uses so an answer landing after unmount sets no state. Two
  `useState`/`useRef` sites at most — the `grep -c "useState" -eq 2` gate counts the import and the
  one call.
- `read` must be a **stable reference** — a module-scope constant, not an inline arrow — and the
  KDoc says so in `ProfileProvider`'s own words: it is the effect's only dependency, and a
  reference that changed on every render would re-run the read on every render with it.
- KDoc naming what this provider is **not**: it holds a fact about the **pair of credentials on one
  request**, not about the player, which is the line `ADR-0135` §Consequences draws between this
  and `ProfileResponse` — and the reason it is a second provider rather than a field on
  `ProfileStripState`, whose all-or-nothing contract would blank the front door's strip on an
  unavailable read.
- The read is **injected**, exactly as `ProfileProvider`'s is: this module never imports
  `readDeviceStanding`, and the `! grep -q` gate says so. `main.tsx` binds it in `TASK-141012`.

## Out of scope

- **Mounting it.** `main.tsx` is `TASK-141012`; `Lobby` reads it in `TASK-141014`. The
  `! grep -rq "DeviceStandingProvider"` gate over `main.tsx`, `Lobby.tsx` and `AccountScreen.tsx`
  keeps this ticket to two created files.
- **A refresh function.** `ProfileProvider` has one because `ADR-0132` §6 named exactly one outcome
  that changes a held profile. Nothing changes this answer without a reload — `ADR-0135` §6:
  *"only this browser's own sign-in or sign-out changes which player its session names, and both
  reload the page."*
- **A three-way state.** `null`-until-answered would let a consumer invent a third behaviour;
  `false` until answered **is** the decision (`ADR-0135` §6).
- **`App.test.tsx`.** Pinned at **36**: this ticket adds no export to `./main`, which is the thing
  its explicit `vi.mock` factory would have to learn.

## Tests

`web-client/src/account/device-standing-provider.test.tsx`, **5 tests**, with a tiny consumer
component that renders the hook's value as text.

| Test | Proves |
| --- | --- |
| `answers false before the read has answered` | with a `read` that never resolves, the consumer renders `false` on the first paint. This is the state a browser is in for the whole of its first round trip, and it is the safe one |
| `adopts the answer, either way` | two renders, one with a `read` resolving `true` and one resolving `false`; after the awaited flush the consumer renders `true` and `false` respectively. Two inputs, because a provider hard-wired to its default passes the `false` half alone |
| `reads once per mount` | the injected `read` is a `vi.fn()` and is called exactly **once** after mounting, with a module-scope-stable reference — a count, not a *was called*, because the defect this guards is a read that fires on every render |
| `answers false where no provider is above` | the consumer rendered bare reads `false`, mirroring `useProfileStrip`'s answer for the same case |
| `a read that lands after unmount changes nothing` | a deferred promise resolved **after** `unmount()`; no state update happens and the test completes without a React act warning or an unhandled rejection. `ProfileProvider`'s `live` ref exists for exactly this and it is the one thing a copy of that file is likely to drop |

## What would still pass if the coder were wrong

- **A provider that ignores `read` and always answers `false`** passes three of the five and fails
  the `true` half of `adopts the answer, either way`. That is why the test carries both polarities
  rather than the one the default already gives.
- **A provider whose context default is `true`** passes `adopts the answer, either way` and fails
  `answers false before the read has answered` and `answers false where no provider is above`. The
  `grep -c "createContext<boolean>(false)"` gate names the literal as well, because this default is
  the decision and not an implementation detail.
- **A read fired inside the render body rather than an effect** passes every assertion about the
  value and fails `reads once per mount`, which asserts an exact count.
- **Dropping the `live` guard** passes four of five and produces an act warning in the fifth.
  Vitest does not fail on a warning by itself, so the test must assert the state did **not**
  change — read the consumer's text after the flush and require it still to be the value it had
  before unmount, rather than relying on console noise.

## Acceptance criteria

- [ ] `device-standing-provider.test.tsx` reports `(5 tests)` and all pass
- [ ] `! grep -q "readDeviceStanding"` on the provider exits 0
- [ ] `! grep -rq "DeviceStandingProvider"` over `main.tsx`, `Lobby.tsx` and `AccountScreen.tsx`
      exits 0
- [ ] `grep -c "useState"` is 2 and `grep -c "createContext<boolean>(false)"` is 1
- [ ] `App.test.tsx` reports `(36 tests)` and all pass
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
