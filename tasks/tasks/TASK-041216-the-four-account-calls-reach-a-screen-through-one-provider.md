---
schema: 2
id: TASK-041216
title: The four account calls reach a screen through one provider
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account, wiring]
depends_on: [TASK-041215]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-provider.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands down the four calls it was given, and the same references twice'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers with nothing where no provider is above'
  - cd web-client && npm run check
---

## Goal

The account screen reaches sign-up, sign-in, sign-out and revocation through one context, so nothing
below the tree knows what a `fetch` or a `Storage` is.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-provider.tsx` | create |
| `web-client/src/account/account-provider.test.tsx` | create |

Read, and do not edit: `web-client/src/profile/set-name-provider.tsx` (the shape to follow, comment
included); `web-client/src/account/sign-up.ts`, `sign-in.ts`, `sign-out.ts`, `revoke-device.ts` (for
the four signatures only).

## Scope

- Exactly these exports:

  ```ts
  export interface AccountCalls {
    readonly signUp: (handle: string, password: string) => Promise<SignUpOutcome>;
    readonly signIn: (handle: string, password: string) => Promise<SignInOutcome>;
    readonly signOut: () => Promise<SignOutOutcome>;
    readonly revokeThisDevice: () => Promise<RevokeOutcome>;
  }
  export function AccountProvider(props: { calls: AccountCalls; children: ReactNode }): ReactElement;
  export function useAccount(): AccountCalls | null;
  ```

- `useAccount` answers `null` where no provider is above, exactly as `useSetName` does, so a
  component can be mounted on its own in a test.
- The provider hands `props.calls` down **as it was given**, with no wrapping and no memo. Carry
  `set-name-provider.tsx`'s comment: the object must be a module-scope constant, because a reference
  built during render appears to change to every consumer.
- One provider rather than four, because four contexts around one screen is four things to nest and
  the four calls are never used apart.

## Out of scope

- Building the calls. `TASK-041223` binds them in `main.tsx` to the real transport.
- Any state. This provider holds none: which screen is showing is the address's (`ADR-0076`), and
  what the server answered is the caller's.
- Any read. The profile arrives through `useProfileStrip`, which already exists.

## Tests

`web-client/src/account/account-provider.test.tsx`, describe block `"the account calls"`.

| Test | Proves |
| --- | --- |
| `hands down the four calls it was given, and the same references twice` | A consumer under the provider receives an object whose four members are `toBe`-identical to the four passed in, and a forced re-render yields the identical object again. Reference identity, not shape — a provider that rebuilt the object would re-run every effect below it |
| `answers with nothing where no provider is above` | `useAccount()` outside a provider is `null`, so a component can be mounted alone |
| `calls the function that was passed, with the arguments it was given` | The consumer calls `signUp("h", "p")` and the recorded double sees exactly those two arguments in that order. Two different values, so a swapped pair is visible |
| `keeps the four apart` | `signOut` and `revokeThisDevice` are different references, and calling one leaves the other's call count at `0`. Fails against a provider that wired the same double to two names |

Four tests in a new file: `npm run test -- src/account/account-provider.test.tsx` reports **4**.

## Acceptance criteria

- [ ] `the account calls > hands down the four calls it was given, and the same references twice`
      passes, asserting with `toBe` across a re-render
- [ ] `the account calls > answers with nothing where no provider is above` passes
- [ ] `the account calls > calls the function that was passed, with the arguments it was given`
      passes, asserting **both** arguments in order
- [ ] `the account calls > keeps the four apart` passes, asserting the untouched call count
- [ ] `grep -cE 'fetch|localStorage|Storage' web-client/src/account/account-provider.tsx` returns `0`
- [ ] `npm run test -- src/account/account-provider.test.tsx` reports `Tests  4 passed (4)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Have the provider pass `{ ...props.calls }` instead of `props.calls`.
   **`hands down the four calls it was given, and the same references twice` reddens** on the object
   identity across the re-render, while the four member identities still pass. That is exactly the
   defect: the members are stable and the container is not, and only the container is what an effect
   depends on. Revert.
2. Swap `signUp`'s two parameters at the call site inside the provider.
   **`calls the function that was passed, with the arguments it was given` reddens**, and only
   because the two fixture values differ. Set both to `"x"` first and watch the mutation pass.
3. Wire `revokeThisDevice` to the `signOut` double.
   **`keeps the four apart` reddens** on both halves; nothing else in the file moves. A copy-paste in
   `main.tsx`'s binding would look identical, which is why `TASK-041223` carries its own version of
   this assertion.
4. Return a default object instead of `null` from `useAccount` when no provider is above.
   **`answers with nothing where no provider is above` reddens alone.** Run it: a default of four
   no-op functions is the friendly-looking version, and it makes a screen mounted without wiring look
   like it works.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
