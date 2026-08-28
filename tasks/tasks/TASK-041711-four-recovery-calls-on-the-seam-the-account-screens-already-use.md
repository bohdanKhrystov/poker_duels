---
schema: 2
id: TASK-041711
title: Four recovery calls on the seam the account screens already use
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - tsc --noEmit (in web-client's npm run check) — main.tsx's `accountCalls` object literal stops satisfying `AccountCalls` the moment the interface gains a required member; measured `TS2741 Property … is missing`
  - tsc --noEmit — e2e/drive-arc.tsx builds a second `AccountCalls` literal for the whole-client harness; measured the same `TS2741`
  - tsc --noEmit — App.test.tsx builds a third, typed field by field off `AccountCalls["…"]`; measured the same `TS2741`
  - tsc --noEmit — account-provider.test.tsx builds a fourth in three of its four tests; measured `TS2741` at three separate lines
labels: [client, account, recovery, wiring]
depends_on: [TASK-041707, TASK-041708, TASK-041709, TASK-041710]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-provider.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-provider.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands every recovery call down unchanged, by reference'
  - test "$(grep -oF 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 6
  - test "$(grep -oF 'plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 8
  - test "$(grep -oF 'readonly' web-client/src/account/account-provider.tsx | wc -l | tr -d ' ')" = 8
  - cd web-client && npm run check
  - cd web-client && npm run build
---

## Goal

`AccountCalls` carries the four recovery calls, `main.tsx` binds each to the fetch its endpoint's
authentication requires, and every place that builds an `AccountCalls` is complete again — so a
screen can reach any of them through the `useAccount()` seam the account screens already use.

## Files

Five files, and every one of them is forced by `tsc` on the same commit — see `atomic:` and
`## Proof`, which is a probe that was run in this worktree rather than a file list remembered.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/account/account-provider.tsx` | modify | the four members; this is the ticket |
| `web-client/src/main.tsx` | modify | `tsc` — the one production `AccountCalls` literal, and the only file that may reach for `localStorage` and `window.fetch` |
| `web-client/src/account/account-provider.test.tsx` | modify | `tsc` — three of its four tests build an `AccountCalls` literal by hand |
| `web-client/src/App.test.tsx` | modify | `tsc` — it builds one, typed field by field off `AccountCalls["…"]` |
| `web-client/src/e2e/drive-arc.tsx` | modify | `tsc` — the whole-client harness builds one over its own fake server |

Read, and do not edit:

- `web-client/src/account/authorized-fetch.ts` — its KDoc says which calls must **never** be wrapped
  and why. Two of the four here are in that class and two are not; §Scope says which.
- `docs/protocol.md` *Recovery email*, *Forgot password*, *Verify email*, *Reset password* — the
  **Authentication** line of each is the whole of what decides the binding.
- `web-client/src/account/attach-recovery-email.ts`, `forgot-password.ts`, `verify-email.ts`,
  `reset-password.ts` — the four merged modules, for their signatures and outcome types.
- `web-client/src/App.test.tsx` lines 41–110 — the wholesale `vi.mock("./main", …)` and the
  `accountCalls` literal beneath it. This ticket touches the **literal**, not the mock factory; see
  `## Notes`.

## Scope

- **`AccountCalls` gains four members**, after `revokeThisDevice`, each returning its module's own
  outcome type:

  ```ts
  readonly attachRecoveryEmail: (
    address: string,
    currentPassword: string,
  ) => Promise<AttachRecoveryOutcome>;
  readonly forgotPassword: (address: string) => Promise<ForgotPasswordOutcome>;
  readonly verifyEmail: (token: string) => Promise<VerifyEmailOutcome>;
  readonly resetPassword: (
    token: string,
    newPassword: string,
  ) => Promise<ResetPasswordOutcome>;
  ```

  Four `import type` lines join the four already there. Nothing else in that file changes:
  `AccountProvider` hands the object down as-is and `useAccount` still answers `AccountCalls | null`.
- **`main.tsx` binds each to the right fetch, and the choice is not cosmetic:**

  ```ts
  attachRecoveryEmail: (address, currentPassword) =>
    attachRecoveryEmail({ fetch: apiFetch, storage: localStorage, address, currentPassword }),
  forgotPassword: (address) => forgotPassword({ fetch: plainFetch, address }),
  verifyEmail: (token) => verifyEmail({ fetch: plainFetch, token }),
  resetPassword: (token, newPassword) =>
    resetPassword({ fetch: plainFetch, token, newPassword }),
  ```

  **`attachRecoveryEmail` takes `apiFetch`** — the `authorizedFetch` wrapper — because
  `POST /api/auth/recovery-email` accepts a bearer token **or** a device id, `ADR-0027` makes the
  bearer outrank the device id, and the module itself sends only `X-Device-Id`. Binding it to
  `plainFetch` would authenticate a signed-in browser as its device.
  **The other three take `plainFetch`**, because their endpoints take no authentication at all — the
  same rule that keeps `signIn` unwrapped, written in `authorized-fetch.ts`'s KDoc.
- **The three test files gain the four members on the literals they already build**, as spies or as
  trivial stubs. No test's behaviour changes and no assertion moves: this is the repair `tsc`
  demands, nothing more.
- **`drive-arc.tsx`'s harness gets four stubs**, not four real calls: its fake server serves no
  recovery endpoint, and giving it one is not this ticket's.

## Out of scope

- **Any component reading any of the four.** `TASK-041712` onward.
- **Touching `App.test.tsx`'s `vi.mock("./main", …)` factory.** This diff adds no `../main` import to
  any component, so the factory needs no new binding — measured. If you find otherwise, that is the
  finding: report it before editing, because that mock has already forced three tickets in this epic.
- **Any new provider or context.** Four members on a merged interface, not a fifth provider in a tree
  that already nests seven.
- **A `forgotPassword` caller.** There is none until the ticket `ADR-0087` unblocked is written —
  `DEC-081` is answered, but its form belongs to that ticket. `revokeThisDevice` has sat on this seam
  with no caller since `TASK-041220`, so this is the established state and not a new one.
- **Making any member optional.** An optional call is a call a component silently skips.

## Tests

**This ticket adds one test and moves none.** Its gates are `tsc` — four times, on four different
files — and the three merged suites it repairs.

`web-client/src/account/account-provider.test.tsx`. **4 merged tests become 5.**

| Test | Proves |
| --- | --- |
| `hands every recovery call down unchanged, by reference` | Builds one `AccountCalls` with four distinct spies for the recovery members, renders a consumer under `AccountProvider`, and asserts each received member `toBe` the spy that went in — four `toBe`s, one per member, so a provider that dropped or swapped one reddens. Then asserts the four are pairwise distinct references, which is what stops a factory returning the same function four times from passing. This mirrors the four merged tests' shape exactly |

The four merged tests in that file, all 37 in `App.test.tsx` and all 7 in `drive-arc.test.tsx` pass
with **no assertion moved and none weakened** — their literals gain four fields and nothing else.

**No `try` anywhere in the added code, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands every recovery call down unchanged, by reference'`
      — passes, with four `toBe`s and the pairwise-distinct assertion
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-provider.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly five**: the four merged plus this
      one. Both lines, because a collection error prints a *passing* `Tests` count with no failure
      line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      and `cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-arc.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — both unmoved. These are the two files whose repair is invisible to their own assertions, so
      the count is the only thing that says the repair was a repair
- [ ] `test "$(grep -oF 'readonly' web-client/src/account/account-provider.tsx | wc -l | tr -d ' ')" = 8`
      — eight members on `AccountCalls`: the four merged (measured at 4 on `develop`) and these four.
      A ninth or a seventh is a member added or lost. The word appears nowhere else in that file
      today, so keep it out of any comment you add
- [ ] `test "$(grep -oF 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 6`
      — the **five** occurrences on `develop` (measured: the declaration and four read bindings) plus
      the one this ticket adds, on `attachRecoveryEmail` alone
- [ ] `test "$(grep -oF 'plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 8`
      — the **five** on `develop` (measured: the declaration and the four merged account calls) plus
      the three this ticket adds. Together with the line above, these two
      counts are what pin the binding decision, which no test in this client can observe
- [ ] `cd web-client && npm run check` exits 0 and `cd web-client && npm run build` exits 0
- [ ] Every merged test in the three test files passes; four object literals gain four fields each
      and no assertion moves
- [ ] No file outside the five listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**The five files were measured, not remembered.** The probe below was run in a worktree on `develop`
at `278c56fb` and reverted; `git status` was the file list. Run it again and record what you get.

1. **Stub the declarations**: add the four members to `AccountCalls` with plausible signatures, and
   nothing else anywhere.
2. **Run `npx tsc --noEmit`.** Measured: **four** paths — `src/App.test.tsx`,
   `src/account/account-provider.test.tsx`, `src/e2e/drive-arc.tsx` and `src/main.tsx`, every one
   `TS2741 Property … is missing in type … but required in type 'AccountCalls'`. Adding **one**
   member rather than four named the same four files, so the count is a property of the interface and
   not of how many members arrive.
3. **Apply the minimal repair** — four stub fields per literal, no behaviour — and run `tsc` again.
   Measured: clean.
4. **Run the whole `client` job**: `npm ci`, `npm run check` (typecheck, lint, `prettier --check`,
   `vitest run`) and `npm run build`. Measured: **836 passed (836)** over **107** files, green, and a
   clean production build. `prettier --check` named `src/App.test.tsx` on the way, which was the
   probe's own indentation rather than a new row — format the real diff and it goes away.
5. **`App.test.tsx`'s mock factory did not need a new binding.** That file's `vi.mock("./main", …)`
   at line 41 returns a fixed object with no `importOriginal`, and it has forced three tickets in this
   epic. It is untouched by this diff because no component gains an import from `../main` here — the
   recovery calls arrive through `useAccount()`, whose provider lives in `account-provider.tsx`.
   Measured as part of step 4's green run. If your run says otherwise, stop and report it.
6. **The Kotlin job is unaffected**: no file in this diff is under a Gradle source set.

Then, with the change applied, two mutations — experiments, not changes, both inside the budget:

7. **Bind `attachRecoveryEmail` to `plainFetch`.** Predict: **nothing reddens** — no test in this
   client observes which wrapper a call was bound to. Record the green run. That is why the two
   `grep` counts are in `verify:` and why this ticket is `sonnet`: the one decision it makes is the
   one nothing can test.
8. **Swap two members in `AccountProvider`'s pass-through** — hand `verifyEmail` down where
   `resetPassword` belongs. Predict: `hands every recovery call down unchanged, by reference` reddens
   on two of its four `toBe`s. If it reddens on none, the four spies are not distinct.

> **A red run names a prefix, not a set.** Vitest and `tsc` both stop reporting past their first hard
> failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why `atomic: 5` rather than a split.** Every item is an exit code: `tsc` refuses each of the four
literals on the commit that declares the members required. The alternative — declaring them optional
first — ships a seam where a component can quietly skip a call, which is the shape `AccountCalls` was
built to avoid; and a seam-only commit with stub bodies and no test is a ticket with nothing a
`verify:` line could assert, which `TASK-041507`'s Notes already ruled out.

**Step 7's green run is the sharpest thing in this ticket.** The binding decision — one wrapped
fetch, three plain ones — is derived from the **Authentication** line of four endpoints in
`docs/protocol.md`, and nothing in this client can observe it, because `authorizedFetch` only adds a
header a test double never inspects. Two `grep` counts hold it instead. That is weaker than a test and
it is written down rather than left to be discovered: the day somebody rebinds `attachRecoveryEmail`
to `plainFetch`, a signed-in browser authenticates as its device, and only these counts say so.
