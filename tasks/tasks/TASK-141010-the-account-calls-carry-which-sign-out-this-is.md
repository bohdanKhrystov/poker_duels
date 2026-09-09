---
schema: 2
id: TASK-141010
title: The account calls carry which sign-out this is
type: task
status: done
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - "`cd web-client && npm run check` — its `typecheck` step (`tsc --noEmit`), in `.github/workflows/build.yml`'s `client` job. Probed at `c6a41e6d`: giving `AccountCalls.signOut` a parameter fails `TS2554 Expected 1 arguments, but got 0` at `src/account/account-provider.test.tsx:198`, and threading the value fails `TS2353` at `src/main.tsx:133` and `src/e2e/drive-arc.tsx:127`. The interface and its three dependents break in the same edit"
  - "`ADR-0135` §7 — the value must reach `signOut`'s **required** field, and `AccountCalls` is the only path from the React tree to `main.tsx`'s module-scope binding. A staged version would have to give the interface member an optional parameter, which is the shape §7 refuses in writing"
labels: [client, account]
depends_on: [TASK-141009]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/account-provider.test.tsx 2>&1 | grep -qF "account-provider.test.tsx  (6 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (36 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - sh -c 'cd web-client && ! grep -q "handsANewProfile: false" src/main.tsx'
  - sh -c 'cd web-client && ! grep -q "handsANewProfile: false" src/e2e/drive-arc.tsx'
  - sh -c 'cd web-client && test $(grep -c "handsANewProfile" src/main.tsx) -eq 2'
  - sh -c 'cd web-client && test $(grep -c "handsANewProfile" src/e2e/drive-arc.tsx) -eq 2'
  - sh -c 'cd web-client && test $(grep -c "fetch: plainFetch" src/main.tsx) -eq 7'
  - git diff --exit-code -- web-client/src/account/sign-out.ts web-client/src/account/SignOutControl.tsx web-client/src/lobby/Lobby.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The boolean the confirmation already holds reaches `signOut`'s required field: `AccountCalls.signOut`
takes it, and both bindings — the product's and the arc harness's — pass it straight through.

## Files

Four, and the count is `tsc`'s: one interface member's signature and its three dependents fail in
the same commit. A fifth was added during implementation under the `ADR-0070` §4 propagation
exception — see that row's "Why it cannot be fewer" column.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/account/account-provider.tsx` | modify | the `AccountCalls.signOut` signature; the change itself |
| `web-client/src/account/account-provider.test.tsx` | modify | `TS2554 Expected 1 arguments, but got 0` at line 198, which calls `receivedCalls!.signOut()` |
| `web-client/src/main.tsx` | modify | `TS2353` — the lambda must take the parameter before it can pass the field |
| `web-client/src/e2e/drive-arc.tsx` | modify | `TS2353` at line 127, the harness's own `AccountCalls` literal, for the same reason |
| `web-client/src/account/AccountScreen.tsx` | modify | `ADR-0070` §4 propagation: `npm run check`'s typecheck step fails at `Lobby.tsx(292,11)` — `TS2322`, "Target signature provides too few arguments. Expected 1 or more, but got 0" — because `AccountScreen`'s own `signOut` prop was left at `() => Promise<SignOutOutcome>` (its comment defers the real value to `TASK-141013`); `Lobby.tsx` is protected by this ticket's own `git diff --exit-code` gate, so the only place to widen the declaration is here. Reverting `account-provider.tsx`'s signature change alone removes the failure, confirming it is this ticket's own edit that causes it. One line, the prop's type annotation; `signOut={signOut}` is unchanged |

Read, do not edit: `docs/adr/ADR-0135-the-server-says-which-sign-out-this-is-and-the-browser-forgets-one-key.md`
§7, `web-client/src/account/sign-out.ts`.

## Scope

- `AccountCalls.signOut` becomes
  `(handsANewProfile: boolean) => Promise<SignOutOutcome>`. Nothing else on the interface moves.
- `main.tsx`:
  `signOut: (handsANewProfile) => signOut({ fetch: plainFetch, storage: localStorage, reload, handsANewProfile })`.
  `fetch: plainFetch` stays the **first** entry of the argument object: `App.test.tsx` asserts
  `/signOut\([^}]*fetch: plainFetch/`, and its `[^}]*` stops at the closing brace.
- `drive-arc.tsx`: the same shape over `server.fetch`.
- Both files lose the literal `handsANewProfile: false` that `TASK-141007` left there, and the
  comments naming this ticket go with it. Two `! grep -q` gates say so, and two `grep -c` gates pin
  `handsANewProfile` at exactly **2** occurrences in each file — the lambda's parameter and the
  field — so a binding that took the parameter and then ignored it fails.
- `Lobby.tsx` needs no edit: it passes `account.signOut` into `AccountScreen`'s `signOut` prop,
  which `TASK-141009` already typed as taking the boolean, and the two signatures now match
  exactly. It is in the `git diff --exit-code` gate.

## Out of scope

- **Computing the boolean.** Every caller still supplies `false` from `AccountScreen`'s literal;
  the abandoning branch stays unreachable until `TASK-141014`.
- **`sign-out.ts` and `SignOutControl.tsx`.** Both landed already and are in the
  `git diff --exit-code` gate.
- **Teaching `drive-arc.tsx`'s server about `/api/me/device`.** Its `account-server.ts` answers
  `500` on an unknown path, which is `ADR-0135` §6's keep and the arc's correct answer.
- **`App.test.tsx`'s `fetch: plainFetch` count.** It stays **7**, measured at `c6a41e6d`: this
  ticket adds no binding. `TASK-141012` is the one that moves it.

## Tests

`account-provider.test.tsx`, **5 today → 6**.

The merged test that moves: the one at line 198 calls `receivedCalls!.signOut()` and becomes
`receivedCalls!.signOut(false)`. Its three assertions — that `signOut` is the same reference it was
given, that it is not `revokeThisDevice`, and that the spy was called once while
`revokeThisDevice` was not — are **unchanged**.

| Test | Proves |
| --- | --- |
| `the sign-out call carries the answer its caller gave it` | one consumer below the provider calls `signOut(true)` and then `signOut(false)`; the spy records `[true]` then `[false]`, asserted with `toHaveBeenNthCalledWith`. Two inputs, because one call cannot tell a threaded argument from a constant |

`App.test.tsx` and `claimed-here-recovered-there.test.tsx` are **not edited** and their counts are
pinned at **36** and **8** — the first because it reads `main.tsx`'s source with four regexes and
two occurrence counts, the second because it is the arc that drives a real sign-out through this
binding.

## What would still pass if the coder were wrong

- **A binding that takes the parameter and passes a literal** — `(handsANewProfile) => signOut({ …,
  handsANewProfile: false })` — typechecks, passes every test in the client, and ships today's
  behaviour forever. The two `! grep -q "handsANewProfile: false"` gates and the two
  `grep -c … -eq 2` gates are the only things that see it, and they are the reason those four
  gates exist rather than one.
- **A provider that rebuilds the calls object** would pass the new test and break the merged
  reference-identity assertions at line 193, which is why those stay untouched.
- **Reordering `main.tsx`'s argument object** so `handsANewProfile` comes first passes `tsc` and
  reddens `App.test.tsx`'s `/signOut\([^}]*fetch: plainFetch/`. The count of `36` is what surfaces
  it as this ticket's problem rather than a mystery two tickets later.

## Acceptance criteria

- [ ] `account-provider.test.tsx` reports `(6 tests)` and all pass
- [ ] `App.test.tsx` reports `(36 tests)` and `claimed-here-recovered-there.test.tsx` reports
      `(8 tests)`, all passing
- [ ] `! grep -q "handsANewProfile: false"` exits 0 for both `main.tsx` and `drive-arc.tsx`
- [ ] `grep -c "handsANewProfile"` is 2 in each of `main.tsx` and `drive-arc.tsx`
- [ ] `grep -c "fetch: plainFetch"` in `main.tsx` is 7
- [ ] `git diff --exit-code` over `sign-out.ts`, `SignOutControl.tsx` and `Lobby.tsx` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
