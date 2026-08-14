---
schema: 2
id: TASK-030506
title: useSend hands a screen the boot-created send, and a missing provider says so
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, store]
depends_on: [TASK-030505]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +103 passed \(103\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a component the send the client was booted with'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses to render a duel component with no provider above it'
  - cd web-client && npm run check
---

## Goal

Screens get their one way out: `useSend()` returns the function `bootDuelClient` made, whose
identity never changes — and a duel hook used outside the provider fails loudly instead of
silently rendering nothing.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-provider.tsx` | modify |
| `web-client/src/store/duel-provider.test.tsx` | modify |
| `web-client/src/store/boot.ts` | read — `DuelClient.send` |

## Scope

- Append one export to `duel-provider.tsx`:

  ```tsx
  /** The boot-created send. Screens call it from event handlers, never effects. */
  export function useSend(): (message: ClientMessage) => void {
    return useDuelClient().send;
  }
  ```

- It returns the boot-created function **as-is** — no wrapper, no `useCallback`, no
  queue-if-not-ready. Its identity never changes for the tab's life, which is what lets a screen
  put it in a dependency array without re-subscribing anything. `Connection.send` already refuses
  to speak to a server that answered `outdated`, so the surface handed to screens is safe.
- Nothing else in the file changes: `useDuelClient` already throws with a message naming
  `DuelProvider`, and this ticket adds the test that pins that message.

## Out of scope

- Any screen calling it — `TASK-030510` is the first.
- A send that is triggered by a message rather than a click. That is a boot reaction by definition
  (`TASK-030504`), never a screen effect, and this hook is not the way to build one.
- A "sending" or "in flight" state. There is no ack for a `ClientMessage` and nothing waits.

## Tests

`web-client/src/store/duel-provider.test.tsx`, describe block `"the duel provider"`. Two `it`
blocks appended after `TASK-030505`'s three. Those three are not edited.

Three edits to what `TASK-030505` wrote: `fireEvent` joins the `@testing-library/react` import,
`useSend` joins the `./duel-provider` import, and one more probe component appears above
`renderUnder`:

```tsx
function CreateButton(): ReactElement {
  const send = useSend();
  return (
    <button type="button" onClick={() => send({ type: "CreateRoom" })}>
      Create
    </button>
  );
}
```

| Test | Proves |
| --- | --- |
| `hands a component the send the client was booted with` | rendering `<CreateButton />` under a provider given a `vi.fn()` and clicking the button calls that spy exactly once, with `{ type: "CreateRoom" }` |
| `refuses to render a duel component with no provider above it` | `expect(() => render(<RoomCode />)).toThrow(/DuelProvider/)` — the message, not merely some error |

The second test makes React log the thrown error to the console twice. That is expected output,
not a failure; do not silence it with a mock.

Two tests. One hundred and one exist, so the suite reports **103**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 103 passed (103)` | the two tests ran and the hundred-and-one before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Change `useSend` to `useDuelClient(); return () => {};` → `hands a component the send the
   client was booted with` fails with `expected "spy" to be called once, but got 0 times`. Revert.
2. Replace the body of `useDuelClient` with `return useContext(DuelContext) as
   DuelClientContext;` → `refuses to render a duel component with no provider above it` fails with
   ``expected [Function] to throw error matching /DuelProvider/ but got 'Cannot destructure
   property \'store\'…'``. Revert.

Quote both in the PR. The second is why the assertion matches the message: without the check the
component still throws, just uselessly.

## Acceptance criteria

- [ ] `the duel provider > hands a component the send the client was booted with` passes
- [ ] `the duel provider > refuses to render a duel component with no provider above it` passes
- [ ] `npm run --silent test` reports `Tests  103 passed (103)`
- [ ] The three `it` blocks from `TASK-030505` are unedited, and their assertions are
      byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
