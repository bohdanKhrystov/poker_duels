---
schema: 2
id: TASK-041223
title: The account calls reach the real transport, and sign-in reaches it carrying nothing
type: task
status: blocked
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, wiring]
depends_on: [TASK-041222]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'binds each of the four account calls to the un-wrapped fetch'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses to wrap sign-in, the one request that must carry nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'wires all four reads through the wrapper and names the browser fetch once'
  - test "$(grep -o 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -o 'window.fetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -o 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 5
  - test "$(grep -o 'const plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '\([^}]*' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 7
  - test "$(grep -oF '[\s\S]' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

The account screen stops being a component nothing can call: sign-up, sign-in, sign-out and
revocation go out through `window.fetch` and `localStorage`, and **none of the four is wrapped** —
which is a fact about `main.tsx`'s text, and is gated as one.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/account-provider.tsx`;
`web-client/src/account/authorized-fetch.ts`; `web-client/src/profile/api.ts` (for `ApiFetch`);
`docs/protocol.md` *Sign up*, *Sign in*, *Sign out*, *Revoke this device*.

## Scope

- Three module-scope constants, placed **after `readLadder` and before `bootDuelClient`**, in this
  order. The placement is not cosmetic: every `fetch: apiFetch` in the file must stay **above** the
  account block, so that no wrapped binding sits below `signIn`'s. *Tests* explains what depends on
  that.

  ```ts
  const plainFetch: ApiFetch = (path, init) => window.fetch(path, init);

  const reload = (): void => window.location.reload();

  const accountCalls: AccountCalls = {
    signUp: (handle, password) =>
      signUp({ fetch: plainFetch, storage: localStorage, handle, password }),
    signIn: (handle, password) =>
      signIn({
        fetch: plainFetch,
        storage: localStorage,
        reload,
        handle,
        password,
      }),
    signOut: () => signOut({ fetch: plainFetch, storage: localStorage, reload }),
    revokeThisDevice: () =>
      revokeThisDevice({ fetch: plainFetch, storage: localStorage }),
  };
  ```

  That block is **prettier-stable at `printWidth` 80 exactly as written** — measured line by line, and
  `signIn`'s call is exploded for that reason, not by taste. Retyping it on one line makes an
  83-column line and `format:check` fails.
- `plainFetch` needs `import type { ApiFetch } from "./profile/api";`. It has no call to be
  contextually typed by, unlike the arrow inside `authorizedFetch(…)`, so `noImplicitAny` refuses it
  un-annotated. `ApiFetch` is capitalised and does not disturb the `apiFetch` count in `verify:`.
- `AccountProvider` wraps the tree beside the other providers — immediately inside `SignedInProvider`
  — as `<AccountProvider calls={accountCalls}>`. The prop is named `calls`.
- **None of the four goes through `authorizedFetch`**, and each for its own documented reason:
  sign-in carries no authentication at all; sign-up authenticates as the device with a header it sets
  itself; sign-out and revocation set `Authorization` themselves from the token they read. A wrapper
  over any of them would put a second `Authorization` on a request that already has one, or a first
  one on the request `docs/protocol.md` says must have none — the case
  `authorized-fetch.ts`'s own KDoc names: *"Must never wrap `POST /api/auth/sign-in`"*.
- **This ticket owns one moved assertion, and it is the only one.** `plainFetch` is a **second**
  `window.fetch(` in the file, so `TASK-041210`'s
  `wires all four reads through the wrapper and names the browser fetch once` — which asserts
  `occurrencesIn(mainSource, "window.fetch(")` is `1` — reddens the moment `plainFetch` lands. Change
  that one expected number from `1` to `2` **in this diff**, and add a comment beside it naming
  `plainFetch` and the reason there are now two: one raw fetch for the reads that are wrapped, one
  for the four calls that must not be. Nothing else in that test moves: its `fetch: apiFetch`
  assertion still expects `4`, because the account calls bind `fetch: plainFetch`.
  `builds that wrapper once, at module scope` is untouched — `authorizedFetch(` stays `1` and the
  column-0 anchor still matches. **No assertion is weakened**: `2` is as exact as `1` was.
- **Do not route `apiFetch` through `plainFetch` to keep the count at `1`.** It would compile,
  behave identically and erase the signal on purpose. `TASK-041210`'s `## Out of scope` says so in
  advance — *"the two counts below are exact, so the day `TASK-041223` adds a fifth binding on a raw
  arrow, [that test] reddens and that ticket must state, in its own diff, which bindings are
  deliberately unauthenticated. That redness is the point; it is not brittleness to design around
  here."* This diff is where that statement gets made, and Proof step 5 is the measurement that shows
  the shortcut is invisible to every other gate in the file.
- **Write the comments this file's style asks for, but keep three strings out of them.** Four counts
  over `main.tsx` are exact and three of them are merged gates, so a comment containing
  `window.fetch(`, `authorizedFetch(` or `const plainFetch` breaks a `verify:` line that has nothing
  to do with the comment. Say *the wrapper* and *the plain fetch* in prose; the code below says the
  rest. (`fetch: apiFetch` and `fetch: plainFetch` are counted inside the tests, where the same rule
  applies for the same reason.)
- `signedIn` is **not** computed here. `TASK-041231` already read it once at module scope and
  `TASK-041222` already consumes it; touching it again would be a second read of a global `DEC-032`
  warns about.
- Nothing else in `main.tsx` moves.

## Out of scope

- **A rendered-tree test driven by a stubbed `window.fetch`.** Not a weakening — **unbuildable in
  `App.test.tsx`**, and this ticket used to demand it. Line 35 of that file is
  `vi.mock("./main", …)`, which replaces the module wholesale for **every** test in it; the factory
  returns fakes for `HistoryProvider`, `useHistory`, `LadderProvider`, `useLadder`,
  `SignedInProvider` and `useSignedIn`, and exports **neither** `plainFetch` **nor** `apiFetch`. Any
  rendered-tree test written there exercises the mock and never `main.tsx`. The mock is load-bearing
  for two reasons its own comment gives: Node's `localStorage` shadows jsdom's (`DEC-032`), and
  `main.tsx` opens a socket at import. Un-mocking `./main` for the whole file is a third file and a
  different ticket, and is not filed.
- **A request-level assertion that sign-in carries no `Authorization` and no `X-Device-Id`.**
  `sign-in.test.ts`'s `sends no device id and no authorization of its own` already owns it, against a
  real invocation, by inspecting `calls[0].init.headers`. A copy here would assert `sign-in.ts`
  again — not `main.tsx`'s wiring, which is the only thing this ticket changes. **A refusal, not an
  omission**, and it is why this ticket no longer carries a test named
  `sends sign-in with no credential of its own, even holding a session`: that name promised a fixture
  this file cannot hold, and the two dispatches that tried it both produced a test that read
  `sign-in.ts`'s source and could not see `main.tsx` at all.
- **A request-level assertion that sign-up authenticates as the device.** `sign-up.test.ts`'s
  `authenticates as the device this browser holds` owns it, for the same reason. **A refusal, not an
  omission** — and the reason the test named `signs up as the device and never under the session` is
  gone from this ticket.
- **Recomputing `signedIn` when the token changes.** Sign-in and sign-out reload the document, so a
  fresh boot is what recomputes it. There is no subscription to storage and no `storage` event
  listener.
- Any change to the four account modules or the provider.
- Any read binding. `TASK-041210` wired those and they stay wrapped.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside the two existing binding
tests, using the `occurrencesIn` helper and the `readFileSync(resolve(here, "main.tsx"), "utf-8")`
that file already has. **Neither new test imports `main.tsx`** — nothing in this file can.

Write them exactly as below. The regexes are the mechanism, not an illustration of it:

```ts
it("binds each of the four account calls to the un-wrapped fetch", () => {
  // The one fact no other suite can see. sign-in.test.ts and sign-up.test.ts
  // already assert the recorded headers of a real invocation, but each calls
  // the function with a fetch the test supplies, so neither can observe which
  // fetch main.tsx binds. That choice is configuration, and a test that
  // supplies its own configuration cannot observe it (TASK-041210).
  //
  // [^}]* stops at the argument object's closing brace, so a binding that
  // named apiFetch cannot pass by reaching the next binding's plainFetch.
  const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");

  expect(mainSource).toMatch(/signUp\([^}]*fetch: plainFetch/);
  expect(mainSource).toMatch(/signIn\([^}]*fetch: plainFetch/);
  expect(mainSource).toMatch(/signOut\([^}]*fetch: plainFetch/);
  expect(mainSource).toMatch(/revokeThisDevice\([^}]*fetch: plainFetch/);

  // Two needles, two different answers: one declaration, four bindings.
  expect(occurrencesIn(mainSource, "const plainFetch")).toBe(1);
  expect(occurrencesIn(mainSource, "fetch: plainFetch")).toBe(4);
});

it("refuses to wrap sign-in, the one request that must carry nothing", () => {
  // authorized-fetch.ts's own KDoc: "Must never wrap POST /api/auth/sign-in".
  // Both polarities, inside sign-in's own argument object: the defect is a
  // one-word edit, and the negative names the two ways of making it.
  const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");

  expect(mainSource).toMatch(/signIn\([^}]*fetch: plainFetch/);
  expect(mainSource).not.toMatch(/signIn\([^}]*fetch: apiFetch/);
  expect(mainSource).not.toMatch(/signIn\([^}]*authorizedFetch/);
});
```

| Test | Proves |
| --- | --- |
| `binds each of the four account calls to the un-wrapped fetch` | Each call **by name**, not by count: the span from `signUp(` / `signIn(` / `signOut(` / `revokeThisDevice(` to the first `}` names `fetch: plainFetch`. A count alone cannot do this — Proof step 3 is a real rebind that leaves every count in the file unchanged. The `1` and the `4` are the vacuity guard `TASK-040709` and `TASK-041210` both use: two needles whose answers differ |
| `refuses to wrap sign-in, the one request that must carry nothing` | The title's security half, in both polarities. `signIn`'s argument object names `plainFetch`, and names neither `apiFetch` nor `authorizedFetch` — so an inline `authorizedFetch(plainFetch, localStorage)` is refused as well as a swap to `apiFetch`. It asserts a **refusal**, which is what the property is, and its name says so rather than promising a fixture the file cannot hold |
| `wires all four reads through the wrapper and names the browser fetch once` *(modified — one number)* | `TASK-041210`'s test, with its `window.fetch(` expectation moved from `1` to `2` and a comment saying why. Its `fetch: apiFetch` expectation stays `4`, which is the half that proves the four reads did not quietly follow the account calls onto the raw arrow |
| `binds the history read to the browser fetch and the browser storage` *(unchanged)* | Still passes, so the read bindings did not move. It asserts only that `main.tsx` *contains* `window.fetch(` and `localStorage`, and a `toMatch` is satisfied by two occurrences as well as by one |
| `builds that wrapper once, at module scope` *(unchanged)* | Still passes: `authorizedFetch(` stays `1` and the column-0 anchor still matches |

**Why `[^}]` and not `[\s\S]*?`, measured.** With a lazy unbounded span, rebinding only `signIn` to
`apiFetch` leaves `/signIn\([\s\S]*?fetch: plainFetch/` **matching** — it runs past `signIn`'s own
argument object and finds `signOut`'s `fetch: plainFetch` two lines later. The first test would then
pass on a source that wraps sign-in. The bound is the whole difference, and it is gated:
`grep -oF '\([^}]*' web-client/src/App.test.tsx | wc -l` is `7` — four spans in the first test, three
in the second — and `grep -oF '[\s\S]'` on that file is `0`, which is what it is on `develop` today
and across all of `web-client/src`.

**And why the placement rule in `## Scope`.** The two negatives are non-vacuous only because no
`fetch: apiFetch` sits below `signIn`'s binding. The `[^}]` bound already enforces that locally; the
placement keeps it true even if someone later widens the span by mistake.

## Acceptance criteria

- [ ] **The amendment's own gate: rebinding *only* `signIn` to `apiFetch` in `main.tsx` reddens a
      test by name, not merely a count.** Measured: it reddens
      `binds each of the four account calls to the un-wrapped fetch` on its `signIn` positive,
      `refuses to wrap sign-in, the one request that must carry nothing` on its `fetch: apiFetch`
      negative, **and** `wires all four reads…` on a count. Proof step 1 is that mutation and the
      PR records all three
- [ ] `App > binds each of the four account calls to the un-wrapped fetch` passes, asserting **four
      separate bounded spans** plus the `1` and the `4`
- [ ] `App > refuses to wrap sign-in, the one request that must carry nothing` passes, asserting one
      positive and **two** negatives
- [ ] `App > wires all four reads through the wrapper and names the browser fetch once` passes with
      its `window.fetch(` expectation changed from `1` to `2` and its `fetch: apiFetch` expectation
      still `4`. That single number is the **only** edit to a pre-existing assertion in the file, and
      a comment beside it names `plainFetch`
- [ ] Every **other** pre-existing test in `App.test.tsx` passes unchanged, `builds that wrapper
      once, at module scope` and both `binds the … read` tests included — no other expected value
      moves and none is weakened
- [ ] `test "$(grep -o 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1` — one
      **construction**, counted with its call parenthesis and by occurrence, not by line
- [ ] `test "$(grep -o 'window.fetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2` — the
      wrapper's, and `plainFetch`'s
- [ ] `test "$(grep -o 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 5` — one
      declaration and the four read bindings. Counted by occurrence: the `grep -c` form this
      criterion used to carry counts **lines**, which is the bug `TASK-041210`'s `## Notes` records
      and which happened to agree here only by accident of line breaks
- [ ] `test "$(grep -o 'const plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1` — one
      declaration, at module scope. The *four bindings* are counted inside the test rather than here,
      on the needle `fetch: plainFetch`: a shell count on the bare word `plainFetch` would be broken
      by any comment that names it, and the comments in this file name everything
- [ ] `test "$(grep -oF '\([^}]*' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 7` — every
      span in both new tests is brace-bounded
- [ ] `test "$(grep -oF '[\s\S]' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 0` — no
      unbounded span anywhere in the file. `grep` exits 1 on zero matches, which is why this is
      written as a `test "$(…)"` comparison and not as a bare `grep -c`
- [ ] `plainFetch`, `reload` and `accountCalls` are all declared at module scope, and the account
      block sits below every `fetch: apiFetch` binding
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

The numbers below were **measured** against the projected `main.tsx` before this amendment was
written, not predicted. Run each step and record the number you actually see; a mismatch with the
table is a finding worth reporting, not a cell to round off. Never record the unmutated state as a
step's "actual", and never write *would*, *if done* or *not testable* — a step you did not run is a
step you report as not run. **No step below claims a test reddens alone unless the measurement says
so**; two of them redden three tests, and saying which is the point.

| `main.tsx` state | `fetch: plainFetch` | `fetch: apiFetch` | `window.fetch(` |
| --- | --- | --- | --- |
| **done** | **4** | **4** | **2** |
| step 1 — only `signIn` on `apiFetch` | 3 | 5 | 2 |
| step 2 — only `signUp` on `apiFetch` | 3 | 5 | 2 |
| step 3 — `revokeThisDevice`'s arrow calls `signOut` | 4 | 4 | 2 |
| step 4 — all four on `apiFetch` | 0 | 8 | 2 |
| step 5 — `apiFetch` built from `plainFetch` | 4 | 4 | 1 |

1. **The amendment's gate.** Rebind **only** `signIn` to `apiFetch`, changing nothing else. A live
   sign-in would then carry `Authorization: Bearer <token>` to the endpoint `authorized-fetch.ts`
   says must never be wrapped.
   **Three tests redden, two of them by name**: `binds each of the four account calls to the
   un-wrapped fetch` on its `signIn` positive; `refuses to wrap sign-in, the one request that must
   carry nothing` on its `fetch: apiFetch` negative; and `wires all four reads through the wrapper
   and names the browser fetch once` on a count, `5` against its expected `4`. Record all three. The
   third alone is what this ticket used to have, and a count that moves does not say *which* call
   moved. Revert.
2. Rebind **only** `signUp` to `apiFetch`.
   **Two tests redden**: the first, on its `signUp` positive and on its `fetch: plainFetch` count
   (`3` against `4`); and `wires all four reads…` on `5` against `4`. **`refuses to wrap sign-in…`
   stays green**, and that is correct — it is about sign-in and says so. Record the non-redness;
   do not describe it as a gap. Revert.
3. Wire `revokeThisDevice` to `signOut` in the `accountCalls` object, **keeping the key name**:
   `revokeThisDevice: () => signOut({ fetch: plainFetch, storage: localStorage, reload }),`.
   **`binds each of the four account calls to the un-wrapped fetch` reddens alone**, on its
   `revokeThisDevice` positive — measured, and this one really is alone. **Every count in the file
   is unchanged**: `fetch: plainFetch` is still `4`, `fetch: apiFetch` still `4`, `window.fetch(`
   still `2`. This is the step that ran green against this ticket's previous test set, 754/754, and
   it is the reason the assertions are per-call rather than counted. Revert.
4. Bind all four to `apiFetch`.
   **Three tests redden**: the first on all four positives and on `fetch: plainFetch` reading `0`;
   `refuses to wrap sign-in…` on its negative; and `wires all four reads…` seeing `fetch: apiFetch`
   **eight** times against `4`. Revert.
5. Build `apiFetch` from `plainFetch` — `const apiFetch = authorizedFetch(plainFetch, localStorage);`
   — and put the `window.fetch(` expectation back to `1`.
   **Everything passes**: `tsc`, eslint, prettier, both new tests and both of `TASK-041210`'s.
   Measured explicitly, because it is the humbling half: **the two new tests do not catch this**.
   All four bounded positives still match, the column-0 anchor still matches, `fetch: plainFetch` is
   still `4`. The only gate against it is `TASK-041210`'s **exact** `window.fetch(` count, which this
   step retires by hand. Run it, and then do not ship it — it is the tidiest-looking version of this
   diff and it silently retires the gate `TASK-041210` built **for this ticket**. Revert, and say in
   the PR that this step was run.
6. Widen both tests' spans from `[^}]*` to `[\s\S]*?`, then apply step 1 again.
   **`binds each of the four account calls to the un-wrapped fetch` goes green on its `signIn`
   positive** while sign-in is wrapped: the lazy span runs past `signIn`'s own argument object into
   `signOut`'s `fetch: plainFetch`. Measured. The count still reddens and so does the second test's
   negative, so the suite is not silent — but the per-call, by-name catch is gone, which is the whole
   mechanism. `verify:`'s `\([^}]*` = `7` and `[\s\S]` = `0` are what hold the bound. Revert.
7. Pass `reload` as `window.location.reload` rather than as an arrow.
   **`npm run check` may pass and the browser throws `Illegal invocation`** — an unbound `reload` has
   no receiver. Run it under `npm run build && node` if you can; if you cannot reproduce it under
   jsdom, say so in the PR rather than claiming the suite covers it.
8. Retype the `accountCalls` block with `signIn`'s call on one line.
   **`npm run format:check` reddens** at 83 columns. It is prettier, not a test, and saying so is
   more useful than pretending the suite covers it. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Blocked — resolved by this amendment; held at `blocked` only until it lands

**The block was a mechanism defect, and the mechanism above replaces it.** Nothing here waits on a
decision; the driver flips this ticket to `ready` when this amendment merges.

**What was wrong.** The `## Tests` table demanded assertions *driven through the rendered tree with a
stubbed `window.fetch`* in a file whose line 35 forecloses exactly that (`## Out of scope`, first
bullet). Two coder dispatches and a `deep` review established the consequence: the tests that got
written read `account/sign-in.ts`'s and `account/revoke-device.ts`'s own source text and never
touched `main.tsx`, so **the security property in this ticket's title was ungated**. Rebinding only
`signIn` to `apiFetch` left the whole suite green but for one count, `4` → `5`; and this ticket's own
Proof step 2 — rebind `revokeThisDevice`'s arrow to call `signOut`, keep the key name — left it
754/754 green.

**Route taken, of the three the review offered: route 1, a targeted source assertion on `main.tsx`.**
It is buildable inside the Files table as it stands, and it is the merged precedent — `TASK-041210`'s
`## Notes` records the same conclusion after the same two failures: ***"assert the behaviour, not the
text" inverts when the property under test is a property of the wiring***, because `main.tsx` is
configuration and a test that supplies its own configuration cannot observe it. Route 2 (a new file
that does not mock `./main`) needs a third file and a solution to the `localStorage`/socket problem
the mock exists to avoid — a different ticket, still unfiled. Route 3 (gate it where the request is
made) is already done and merged: `sign-in.test.ts`'s `sends no device id and no authorization of its
own` and `authorized-fetch.test.ts`'s key-set equality own the request level, and the only fact left
uncovered was **which wrapper `main.tsx` chooses**, which is route 1.

What route 1 buys over the counted version it replaces is per-call, by-name detection — Proof step 3
is a genuine rebind that moves no count in the file at all. What it does not buy is a request-level
observation; the ticket says so in `## Out of scope` rather than implying otherwise in a test name,
which is what the two removed names did.

## Notes

**Three predictions in the previous `## Proof` were false, and are corrected above.** Step 1
predicted that `sends sign-in with no credential of its own, even holding a session` and `signs up as
the device and never under the session` would redden *on the `Authorization` assertion*; measured,
neither did, because neither test ever read `main.tsx`. Step 2 predicted that rebinding
`revokeThisDevice` to `signOut` would redden `binds the four account calls…`; measured, the suite
stayed 754/754 green. Both tests are gone, replaced by ones whose redness was measured before this
was written. Step 3 ("build `accountCalls` inside the render — nothing reddens") is also gone: it
recorded a real design rule with no gate behind it, and a Proof step whose finding is *"nothing
happens"* teaches a reader to trust the prose. The rule survives where it belongs, in
`account-provider.tsx`'s KDoc and in `## Scope`.

**`grep -c` counts lines, not occurrences.** The `apiFetch` criterion answered `5` only because each
occurrence happened to sit on its own line — the same trap `TASK-041210` corrected for
`authorizedFetch`, one file away. Every count in `verify:` is now `grep -o … | wc -l`, and the one
zero-expectation is wrapped as `test "$(…)" = 0` because `grep` exits 1 when it matches nothing.

**Four counts three merged tickets gate are unchanged by this amendment**, because it changes no line
of `main.tsx` that the previous version did not already change: `authorizedFetch(` = 1,
`window.fetch(` = 2 (this ticket's one owned move, from `TASK-041210`'s 1), `fetch: apiFetch` = 4,
`= useSignedIn()` = 0.
