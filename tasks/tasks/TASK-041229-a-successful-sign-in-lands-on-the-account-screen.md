---
schema: 2
id: TASK-041229
title: A successful sign-in starts the next boot on the account screen, with no way back to sign-in
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, routing, wiring]
depends_on: [TASK-041227]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lands the next boot on the account screen after a sign-in that worked'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands the account landing to sign-in and never runs it here'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/sign-in.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - test "$(grep -oF 'reloadAtAccount' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'reloadAtAccount()' web-client/src/main.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'location.hash =' web-client/src/main.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'window.location.reload()' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'hashForScreen' web-client/src/account/sign-in.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'window.fetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 5
  - test "$(grep -oF 'const plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '[\s\S]' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF '\([^}]*' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 9
  - cd web-client && npm run check
---

## Goal

A browser that has just signed in never comes back to the sign-in screen: the reload that carries the
new identity starts at `#/account`.

## The answer this ticket applies

[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§5. *How* it is arranged is this ticket's; *that* it happens is the ADR's. The reason is that the
account screen carries `ADR-0037`'s routes statement (`TASK-041217`, `PASSWORD_ROUTE_LIVE`), which is
**the only confirmation this product has** — `account-text.ts` authors no *you are signed in*
sentence anywhere, so a sign-in that landed on the first screen would be a product that never says it
worked.

## Why this is its own ticket

`TASK-041227` was written at three files — `Lobby.tsx`, `AccountScreen.tsx`, `App.test.tsx` — before
`DEC-077` was answered. §5's landing rule reaches a fourth, `main.tsx`, because `signIn`'s `reload`
is wired there as a module-scope constant (`TASK-041223`). **No merged gate refuses the intermediate
state**: `npm run check` and `npm run build` are both green with the screen and the door but no
landing rule, and green with the landing rule and no screen. Four files with no gate holding them
together is two tickets, not an `atomic:` of four (`ADR-0068`, `ADR-0070`).

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/sign-in.ts` (for the `reload` parameter only);
`web-client/src/account/sign-in.test.ts` (for `reloads the document once a session exists, and not
before`, which this ticket **relies on and must not touch**);
`web-client/src/routing/screen.ts` (for `hashForScreen`);
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§5; [`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §6.

## Scope

- One module-scope constant in `main.tsx`, beside `reload`, and `signIn` is the only call that gets
  it. **This is exactly what PR #1096 already wrote and it was measured sound — carry it over
  verbatim rather than reworking it:**

  ```ts
  // ADR-0083 §5: a successful sign-in starts the next boot at #/account, never
  // back at the screen it has just finished using. A replace rather than an
  // assignment, because a pushed entry would put the Back button on #/sign-in
  // for a browser that is now signed in.
  const reloadAtAccount = (): void => {
    window.history.replaceState(null, "", hashForScreen("account"));
    window.location.reload();
  };
  ```

  It needs `import { hashForScreen } from "./routing/screen";`, and `signIn`'s binding changes from
  `reload,` to `reload: reloadAtAccount,`. Nothing else. That is an eleven-line addition and a
  one-line change, and the whole projected diff was run green at 775 tests plus `npm run check`
  before this amendment was written.
- **`replaceState`, never `window.location.hash = …`.** The assignment adds a history entry
  (`TASK-041202`'s third test measures exactly that), so *Back* would return the player to
  `#/sign-in` — the screen `ADR-0083` §5 says they never come back to. `replaceState` fires neither
  `popstate` nor `hashchange` (`ADR-0076` §5), which is harmless here because the very next statement
  discards the document.
- **Pass `reloadAtAccount`; never call it.** `main.tsx` hands the function to `signIn` and `signIn`
  decides whether to run it. A wiring that awaited `signIn` and then called `reloadAtAccount()`
  itself would land a **refused** player on `#/account`, and that is the one refusal-shaped defect
  `main.tsx`'s source *can* be held to. It is gated — Proof step 4.
- **`signOut` keeps the plain `reload`.** `ADR-0083` §5 is about sign-in and says nothing about
  sign-out, and `TASK-041214` already decided that sign-out leaves the room. Two constants, one
  changed call site. Unlike the previous version of this ticket, this rule is now **an assertion, not
  a reviewer's note** — Proof step 3 reddens two tests.
- **Do not name `reloadAtAccount` in a comment**, in either file. Its occurrence count is exactly
  `2` — one declaration, one binding — and a comment naming it breaks a `verify:` line that has
  nothing to do with the comment. Say *the account landing* in prose. This is the rule
  `TASK-041223`'s `## Scope` already applies to `window.fetch(`, `authorizedFetch(` and
  `const plainFetch` in the same file.
- **The same rule binds the two `App.test.tsx` needles, and it is easy to miss.** A comment in that
  file must not contain the unbounded-span needle `verify:` pins at zero, nor the brace-bounded span
  literal it pins at `9` — a sentence *explaining* either one counts as an occurrence of it. This is
  not hypothetical: the first draft of the test comment above named the unbounded needle twice while
  explaining why it is avoided, and took that count from `0` to `2` — measured. Describe the bound in
  words, never by writing it.
- Nothing else in `main.tsx` moves: `plainFetch`, `accountCalls`, `signedIn` and the four fetch
  bindings stay exactly as `TASK-041223` left them.

## Out of scope

- **Changing `sign-in.ts`.** `signIn` takes `reload` injected and calls it once, on success only;
  that is the whole reason this rule lands in the wiring and not in the module. A criterion greps it.
- **Any edit to `sign-in.test.ts`, including adding a test.** It is `read, do not edit`, and a
  `verify:` line pins it at **exactly 7 passing tests**. That count is deliberate: see `## Notes`.
- **A rendered-tree test driven by a stubbed `window.fetch`.** Not a weakening — **unbuildable in
  `App.test.tsx`**, and this ticket used to demand it. Line 41 of that file is `vi.mock("./main", …)`,
  which replaces the module wholesale for **every** test in it and exports neither `plainFetch` nor
  `apiFetch`. The mock is load-bearing: Node's `localStorage` shadows jsdom's (`DEC-032`) and
  `main.tsx` opens a socket at import. This is the merged precedent from `TASK-041223`'s
  `## Out of scope`, and re-deriving it is what cost this ticket three dispatches.
- **A test asserting that a refused sign-in stays at `#/sign-in`.** **A refusal, not an omission,
  and not a gap.** `sign-in.test.ts`'s `reloads the document once a session exists, and not before`
  already drives a 200, a 401 and a rejecting fetch against injected `reload` doubles and asserts
  `1`, `0` and `0` call counts; `stores nothing when a 200 carries no token` asserts `0` for the
  fourth branch. Both are merged (`TASK-041213`, PR #1065) and both hold. Mutating `sign-in.ts` to
  call `reload` on every outcome reddens **both, by name** — measured, Proof step 5. A copy in
  `App.test.tsx` would assert nothing new, and a source assertion on `main.tsx` cannot see which
  branch calls `reload` anyway.
- **A *you are signed in* sentence.** `ADR-0083` §5 makes the account screen's routes statement the
  confirmation; adding copy would be new player-facing vocabulary and nothing licenses it.
- **Sign-out's landing.** Untouched. It is now asserted (the `signOut` negative) but not otherwise
  observed.
- The screen, the door and the branch — `TASK-041227`, which is `done`.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, appended after the last test.
**Both are source assertions over `readFileSync(resolve(here, "main.tsx"), "utf-8")`, using the
`occurrencesIn` helper the file already has** — the mechanism `TASK-041223` and `TASK-041210` both
landed on, for the reason `TASK-041210`'s `## Notes` records: ***"assert the behaviour, not the text"
inverts when the property under test is a property of the wiring***, because `main.tsx` is
configuration and a test that supplies its own configuration cannot observe it.

Write them exactly as below. The bounds are the mechanism, not an illustration of it:

```ts
it("lands the next boot on the account screen after a sign-in that worked", () => {
  // ADR-0083 §5. The span is brace-bounded for the reason TASK-041223
  // measured: a lazy unbounded span runs past the arrow's own body into the
  // next declaration, and verify: pins this file's unbounded-span count at
  // zero. Naming that needle here in prose would break the count itself.
  const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");

  expect(mainSource).toMatch(
    /=> \{[^}]*history\.replaceState\(null, "", hashForScreen\("account"\)\)/,
  );
  expect(mainSource).toMatch(/signIn\([^}]*reload: reloadAtAccount/);
  // Sign-out leaves the room; ADR-0083 §5 is about sign-in alone.
  expect(mainSource).not.toMatch(/signOut\([^}]*reload: reloadAtAccount/);

  // Two needles, two different answers. The 0 is the replace-not-assign rule;
  // the 2 is one plain reload plus the account landing's own, and it is what
  // keeps a landing that sets the fragment and never reboots from passing.
  expect(occurrencesIn(mainSource, "location.hash =")).toBe(0);
  expect(occurrencesIn(mainSource, "window.location.reload()")).toBe(2);
});

it("hands the account landing to sign-in and never runs it here", () => {
  // main.tsx passes the function; sign-in.ts decides which outcome runs it,
  // and sign-in.test.ts's "reloads the document once a session exists, and
  // not before" is what holds that decision. What this file can see, and the
  // only refusal-shaped defect it can see, is main.tsx invoking the landing
  // itself — which would move a refused player to #/account too.
  const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");

  // Two needles, two different answers: one declaration and one binding,
  // and no call site anywhere.
  expect(occurrencesIn(mainSource, "reloadAtAccount")).toBe(2);
  expect(occurrencesIn(mainSource, "reloadAtAccount()")).toBe(0);
});
```

| Test | Proves |
| --- | --- |
| `lands the next boot on the account screen after a sign-in that worked` | The landing exists, is a **replace**, is bound to `signIn`, is **not** bound to `signOut`, and still reboots. Four of those five are each independently gated — Proof steps 1, 2, 3 and 6. This is the test PR #1096 got right; the `signOut` negative and the two counts are what this amendment adds |
| `hands the account landing to sign-in and never runs it here` | Replaces `leaves a refused sign-in exactly where it was`, whose name promised a behaviour a source assertion cannot see. This name promises only what it checks: the landing is **passed**, never **called**, so which outcome triggers it stays `sign-in.ts`'s decision. Proof step 4 is a real wiring that lands everybody on `#/account` and moves both needles |
| `reloads the document once a session exists, and not before` *(merged, `sign-in.test.ts:173`, **not edited**)* | The refusal branch itself, at the level where it lives: 200 → `reload` once, 401 → never, rejecting fetch → never. Pinned by a `verify:` line that counts the **file's whole test total at 7**, not by its name — see `## Notes` |

## Acceptance criteria

- [ ] **The amendment's own gate: mutating `sign-in.ts` to call the injected `reload` on every
      outcome reddens a test by name.** Measured: it reddens `reloads the document once a session
      exists, and not before` **and** `stores nothing when a 200 carries no token`, both in
      `sign-in.test.ts`, both merged and unedited. Proof step 5 is that mutation and the PR records
      both names
- [ ] `App > lands the next boot on the account screen after a sign-in that worked` passes, asserting
      the bounded `replaceState` span, the `signIn` positive, the `signOut` negative and **both**
      counts
- [ ] `App > hands the account landing to sign-in and never runs it here` passes, asserting **two**
      needles with two different answers
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/sign-in.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — `sign-in.test.ts` still has **exactly seven** passing tests. Neither a deleted test nor an
      added one passes this
- [ ] `test "$(grep -oF 'reloadAtAccount' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2` — one
      declaration, one binding, and **no comment naming it**
- [ ] `test "$(grep -oF 'reloadAtAccount()' web-client/src/main.tsx | wc -l | tr -d ' ')" = 0` — it
      is passed, never called
- [ ] `test "$(grep -oF 'location.hash =' web-client/src/main.tsx | wc -l | tr -d ' ')" = 0` — the
      fragment is set by a replace, not an assignment
- [ ] `test "$(grep -oF 'window.location.reload()' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2`
      — `reload`'s and the account landing's. A landing that set the fragment and never rebooted
      would read `1`
- [ ] `test "$(grep -oF 'hashForScreen' web-client/src/account/sign-in.ts | wc -l | tr -d ' ')" = 0`
      — the rule is in the wiring, not the module
- [ ] `test "$(grep -oF 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1`,
      `test "$(grep -oF 'window.fetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 2`,
      `test "$(grep -oF 'apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 5` and
      `test "$(grep -oF 'const plainFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1` — the
      four `main.tsx` counts `TASK-041210` and `TASK-041223` gate, unmoved by this diff. The fifth,
      `fetch: plainFetch` = 4, is asserted **inside** the merged test `binds each of the four account
      calls to the un-wrapped fetch` rather than here, because a `: ` in a `verify:` line is not a
      plain YAML scalar
- [ ] `test "$(grep -oF '[\s\S]' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 0` — no
      unbounded span anywhere in the file, still. `TASK-041223`'s Proof step 6 measured what one
      costs, and the new `replaceState` span is brace-bounded for that reason
- [ ] `test "$(grep -oF '\([^}]*' web-client/src/App.test.tsx | wc -l | tr -d ' ')" = 9` —
      `TASK-041223`'s `7`, plus the two spans this ticket adds. **This ticket owns that move**: the
      value is a fact about the file, this diff changes it, and leaving it at `7` would leave a stale
      number behind
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged — no expected value moves and none
      is weakened. This diff **appends** two tests and edits nothing
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run, in this worktree, against the projected diff, before this amendment was
written.** The finished state is 98 files / **775 tests** green plus `npm run check`. Record the
number you actually see; a mismatch with a step is a finding worth reporting, not a cell to round
off. Never record the unmutated state as a step's "actual", and never write *would*, *if done* or
*not testable*.

1. In `reloadAtAccount`, swap the replace for `window.location.hash = hashForScreen("account")`.
   **`lands the next boot on the account screen after a sign-in that worked` reddens alone** —
   measured, 774/775. It reddens on the `replaceState` span; its `location.hash =` count would
   redden too, and the first failing assertion is what the reporter names. This is the mutation that
   keeps *Back* off `#/sign-in` for a signed-in browser, which is the sentence `ADR-0083` §5 forbids.
   Revert.
2. Delete the replace line entirely, keeping `window.location.reload()`.
   **The same test reddens alone** — measured, 774/775. A landing that reboots to wherever it already
   was is the null version of this ticket. Revert.
3. Bind `reloadAtAccount` to `signOut` as well as to `signIn`.
   **Two tests redden** — measured, 773/775: `lands the next boot…` on its `signOut` negative, and
   `hands the account landing to sign-in and never runs it here` on `reloadAtAccount` reading `3`
   against `2`. **This is a correction to the previous version of this ticket**, whose Proof step 2
   recorded *"nothing in this ticket reddens"* and left the rule to `## Out of scope` and the
   reviewer. It is now gated twice. Revert.
4. Leave `signIn` on the plain `reload` and call the landing from the wiring instead — make the
   `signIn` arrow `async`, `await` the call, then `reloadAtAccount();` before returning the outcome.
   **Two tests redden** — measured, 773/775: `hands the account landing…` on `reloadAtAccount()`
   reading `1` against `0` and on `reloadAtAccount` reading `3` against `2`, and `lands the next
   boot…` on its `signIn` positive. This is the wiring that lands **everybody** on `#/account`,
   refused players included — the defect the deleted `leaves a refused sign-in exactly where it was`
   was named for, caught here at the level `main.tsx` can actually be held to. Revert.
5. **The amendment's gate.** Mutate `sign-in.ts` to call `request.reload()` on every outcome — the
   `401` branch, the non-`200` branch, the no-token branch and the `catch`.
   **Two merged tests redden by name** — measured, 773/775: `reloads the document once a session
   exists, and not before` and `stores nothing when a 200 carries no token`, both in
   `sign-in.test.ts`. Nothing in `App.test.tsx` reddens, and that is correct: a source assertion on
   `main.tsx` cannot see which branch of another module calls a function. Mutating **only** the `401`
   and non-`200` branches reddens `reloads the document once a session exists, and not before`
   **alone** — 774/775 — which is the narrower measurement and worth recording separately. Revert
   `sign-in.ts` completely; it is `read, do not edit`.
6. Drop `window.location.reload()` from `reloadAtAccount`, keeping only the replace.
   **`lands the next boot…` reddens alone** on its `window.location.reload()` count, `1` against `2`
   — measured, 774/775. **This is the second correction to the previous version**, whose Proof step 4
   recorded *"nothing reddens"* and deferred the guarantee to `sign-in.test.ts` — which counts that
   `signIn` calls the **injected** function, and can say nothing about whether that function performs
   a real navigation. Without the reload the fragment lands and `initialState()` is never rebuilt, so
   `ADR-0075`'s three presence fields survive the identity change. One count closes it. Revert.
7. Add any eighth test to `sign-in.test.ts`.
   **The `Tests +7 passed \(7\)` `verify:` line reddens** — measured, with a trivial
   `expect(true).toBe(true)`. That is the point of counting rather than naming; see `## Notes`.

> **Expect a `Not implemented: navigation (except hash changes)` line on stderr** if you run a
> fixture that reaches a real `window.location.reload()`. jsdom prints it and **does not fail the
> test**. No test in this ticket triggers it, because both are source assertions.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

### The rule this ticket exists to write down

**A `verify:` line that greps for a test which is supposed to already exist is a gate satisfiable by
creating that test.** The grep does not know who wrote the test or when; it sees a name in the
reporter's output and exits 0. This ticket's previous version greped
`sends sign-in with no credential of its own, even holding a session` and labelled it *"(existing,
`TASK-041223`)"*. It existed nowhere — it was `TASK-041223`'s **pre-amendment** name, replaced there
by `refuses to wrap sign-in, the one request that must carry nothing`. So a coder, facing a gate that
could not otherwise pass, wrote the test. The ticket manufactured it; the coder was doing as told.

**If a ticket wants a merged test to keep passing, it must pin something the ticket cannot author —
a count, or the test's file — not just its name.** This ticket pins
`reloads the document once a session exists, and not before` by running its file and requiring
**exactly seven passing tests** in it, with the file listed `read, do not edit`. An added test makes
eight and reddens; a deleted one makes six and reddens; a broken one reddens. A name grep would have
done none of that. Proof step 7 is the measurement.

### Three claims in the previous `## Blocked` section are false, and were re-measured

The block was written after three dispatches and two of its three defects were real. The third was
not, and it is the one that would have sent this ticket to the wrong place:

- **False: *"mutating `sign-in.ts` to call the injected `reload` on every outcome reddens nothing in
  the whole suite — the property is genuinely unguarded."*** It reddens **two** merged tests by name.
  Measured twice: once on `develop` at `604c8ea7` before any change (773 → 771 passing, 2 failed),
  and once against the finished projected diff (775 → 773). Both times:
  `reloads the document once a session exists, and not before` and
  `stores nothing when a 200 carries no token`.
- **False: *"a fixture already injects `reload`"* is offered as a reason to write a new test.** It is
  true that the fixture injects `reload` — all seven tests in `sign-in.test.ts` pass
  `reload: vi.fn()`, and the reload test builds three separate `vi.fn()`s for its three outcomes. But
  the test the route asks for — *"drive a 200 and a 401 against one fixture and assert `reload` was
  called exactly once and then not at all"* — **is that merged test, line for line**, plus a third
  rejecting-fetch case it does not ask for.
- **Imprecise:** `App.test.tsx`'s `vi.mock("./main", …)` is at line **41**, not line 35.

### Route taken, of the three the block recorded: route 2, with route 1 folded in

**Route 1 — delete the third row and its `verify:` line — is not a choice and is taken.** The
guarantee is merged twice over (`refuses to wrap sign-in…` at `App.test.tsx:238` and `sends no device
id and no authorization of its own` at `sign-in.test.ts:135`), the row bought nothing, and its grep
is what manufactured a fake test.

**Route 3 — move both behavioural rows to `sign-in.test.ts` — was rejected on measurement, and it is
the plausible-looking wrong answer.** It rests on the block's false third defect. The test it
describes already exists, merged, at `sign-in.test.ts:173`. Taking route 3 would have this ticket
instruct a coder to write a duplicate of a passing merged test — **the same defect as the deleted
third row, in the opposite direction**: that row named a merged test that did not exist, and route 3
would author a test that already does. It would also grow the ticket to three files for no fact
gained.

**Route 2 is taken: rename the second row to what a source assertion can prove.** Its second half —
*"file the refusal-branch guard as its own ticket against `sign-in.test.ts`"* — is **not** needed and
no ticket is filed, because that guard is merged and holding. What was genuinely uncovered is a
narrower and real fact: **`main.tsx` passes the landing rather than calling it.** Proof step 4 is a
wiring that lands a refused player on `#/account` and is invisible to every other gate in the
repository. `hands the account landing to sign-in and never runs it here` is that fact, and its name
promises exactly it — which is what the old name did not.

### What carries over from PR #1096, and what does not

**Carried over unchanged:** the whole of `main.tsx` — the `hashForScreen` import, the
`reloadAtAccount` constant with its comment, and `reload: reloadAtAccount` on `signIn`'s binding.
Re-measured here: sound, and green. All five `main.tsx` counts `TASK-041210` and `TASK-041223` gate
still hold at `1`, `2`, `5`, `1`, `4`.

**Carried over with additions:** `lands the next boot on the account screen after a sign-in that
worked`, keeping its name and its `replaceState` and `signIn` assertions, and gaining the `signOut`
negative and the two counts — which is what turns the previous version's two *"nothing reddens"*
Proof steps into gates.

**Not carried over:** PR #1096's `[\s\S]*?` span, which would have broken `TASK-041223`'s merged
`[\s\S]` = `0` gate on this file; `leaves a refused sign-in exactly where it was`, which asserted the
`signIn` binding twice and said nothing about refusal; and
`sends sign-in with no credential of its own, even holding a session`, the fabricated test.

### `grep -c` counts lines, not occurrences

Every count in `verify:` is `grep -oF … | wc -l`, and the four zero-expectations are wrapped as
`test "$(…)" = 0` because `grep` exits **1** when it matches nothing — a bare `grep -c … = 0` fails
the step it is supposed to pass. `-F` is on every needle so that `(`, `)`, `[`, `]` and `.` are
literal.

**`NO_COLOR=1` stays on every test-name grep.** Note that this environment also sets `FORCE_COLOR`,
which vitest warns takes precedence — measured here, the reporter still prints test names without
escape codes inside them, so `grep -qF` on a name matches either way. The summary line
`Tests  7 passed (7)` is matched with `-E` and `+` for the same reason.

## Blocked — resolved by this amendment; held at `blocked` only until it lands

**The block was a ticket defect, and the mechanism above replaces it.** Nothing here waits on a
decision: no ADR is missing, `ADR-0083` §5 says what must happen, and the only open question was
where the guarantee is gated — which is a measurement, and it has been made. The driver flips this
ticket to `ready` when this amendment merges.

**What was wrong**, in the words of the three defects the block recorded: the `## Tests` preamble
specified a rendered-tree fixture that `App.test.tsx` forecloses, inherited from `TASK-041223`'s
pre-amendment shape; the third row greped a test that exists nowhere and so manufactured one; and the
second row's name promised a refusal guard that a source assertion on `main.tsx` cannot provide. All
three are fixed above. The block's own diagnosis of the third — that the refusal property is
*unguarded everywhere* — is itself false, and `## Notes` records the re-measurement.
