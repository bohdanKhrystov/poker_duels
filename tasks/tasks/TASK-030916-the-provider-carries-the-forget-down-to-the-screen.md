---
schema: 2
id: TASK-030916
title: The provider carries the forget down to the screen
type: task
status: done
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, store, ui]
depends_on: [TASK-030915]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +571 passed \(571\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a component the forget the client was booted with'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a component a forget that does nothing where none was given'
  - cd web-client && grep -qF 'return useDuelClient().forgetRoom;' src/store/duel-provider.tsx
  - cd web-client && npm run check
---

## Goal

`useForgetRoom()` sits beside `useSend()`: a screen can reach boot's forget without holding boot,
and reaches a harmless no-op where the provider was given none.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-provider.tsx` | modify — a context member, an optional prop, one hook |
| `web-client/src/store/duel-provider.test.tsx` | modify — two added |
| `docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md` | read — §4, why the prop is optional and what that costs |

## Scope

- `DuelClientContext` gains `readonly forgetRoom: () => void;` — **required** on the context,
  optional on the prop.
- One module-scope constant, for the reason `main.tsx` gives for its own module-scope reads: a
  stable reference, so the memo below does not hand every render a new function.

  ```ts
  const NO_FORGET = (): void => {};
  ```

- `DuelProvider` gains `forgetRoom?: () => void;` and folds it in:

  ```ts
  const value = useMemo(
    () => ({
      store: props.store,
      send: props.send,
      forgetRoom: props.forgetRoom ?? NO_FORGET,
    }),
    [props.store, props.send, props.forgetRoom],
  );
  ```

- One hook, beside `useSend`, with the same one-line body and the same KDoc shape:

  ```ts
  /** The boot-created forget. Screens call it from event handlers, never effects. */
  export function useForgetRoom(): () => void {
    return useDuelClient().forgetRoom;
  }
  ```

  It goes through `useDuelClient()`, so a component with **no provider above it at all** throws the
  message that hook already throws — exactly as `useSend` does. The no-op answers only a provider
  that supplied no `forgetRoom`.
- **The prop is optional on purpose.** `ADR-0072` §4 keeps the fourteen other render sites in seven
  files byte-identical, and §4 and Consequences both record what that buys and what it costs. Do not
  make it required.

## Out of scope

- Passing anything at any render site: `main.tsx` and `Lobby.tsx` are `TASK-030918`, and
  `reconnect.test.tsx` is `TASK-030920`. Every existing `DuelProvider` in the tree stays as it is,
  and this ticket must not edit one.
- Handing `DuelProvider` the whole `DuelClient`. `ADR-0072` §4 names that as the answer if a
  *fourth* prop is ever needed — not now.
- Calling it from an effect anywhere. `ADR-0032` §3 extends to this function verbatim: event
  handlers only.

## Tests

`web-client/src/store/duel-provider.test.tsx`, describe block `"the duel provider"`. Two added.

`renderUnder` gains an optional fourth parameter, `forgetRoom?: () => void`, passed straight to the
provider; every existing call site keeps its three arguments and no existing test changes. One test
component beside `CreateButton`:

```tsx
function LeaveButton(): ReactElement {
  const forgetRoom = useForgetRoom();
  return (
    <button type="button" onClick={() => forgetRoom()}>
      Leave
    </button>
  );
}
```

| Test | Proves |
| --- | --- |
| `hands a component the forget the client was booted with` | rendered under a provider given a `vi.fn()` as `forgetRoom`, one click leaves that spy with `toHaveBeenCalledOnce()` — and `send` with `not.toHaveBeenCalled()`, because forgetting tells the server nothing |
| `hands a component a forget that does nothing where none was given` | rendered under a provider with **no** `forgetRoom` prop, clicking throws nothing (`expect(() => fireEvent.click(button)).not.toThrow()`) and `send` is still `not.toHaveBeenCalled()` |

The spy is what distinguishes *this* provider's function from any function: a hook that returned
`NO_FORGET` unconditionally passes the second test and fails the first.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 571 passed (571)` | two added to 569 |
| the two `--reporter=verbose` greps | both names exist |
| `grep -qF 'return useDuelClient().forgetRoom;'` | the hook goes through `useDuelClient`, so a missing provider throws rather than silently forgetting nothing |

**Name the edit that makes each assertion red:**

1. Return `NO_FORGET` from the hook unconditionally → `hands a component the forget the client was
   booted with` fails. Revert.
2. Make the prop required and drop the `?? NO_FORGET` → `npm run check` fails to typecheck the seven
   files that render a `DuelProvider` without one. Revert; this is why §4 made it optional.

Quote the first in the PR.

## Acceptance criteria

- [ ] `the duel provider > hands a component the forget the client was booted with` passes
- [ ] `the duel provider > hands a component a forget that does nothing where none was given` passes
- [ ] `useForgetRoom` is exported from `duel-provider.tsx` and its body is `return useDuelClient().forgetRoom;`
- [ ] `DuelProvider`'s `forgetRoom` prop is optional (`forgetRoom?:`)
- [ ] No file outside the two in the table differs from `develop` — in particular `main.tsx`, `Lobby.tsx` and `reconnect.test.tsx` are untouched
- [ ] Every pre-existing `it` block in `duel-provider.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  571 passed (571)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
