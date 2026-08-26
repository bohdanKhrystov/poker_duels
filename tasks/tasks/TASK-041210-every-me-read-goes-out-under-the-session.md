---
schema: 2
id: TASK-041210
title: Every read under /api/me goes out under the session, so the strip stops naming the wrong player
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, auth, wiring]
depends_on: [TASK-041209]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'wires all four reads through the wrapper and names the browser fetch once'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds that wrapper once, at module scope'
  - test "$(NO_COLOR=1 grep -o 'authorizedFetch(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(NO_COLOR=1 grep -o 'fetch: apiFetch' web-client/src/main.tsx | wc -l | tr -d ' ')" = 4
  - NO_COLOR=1 grep -qF 'never cleared' web-client/src/main.tsx
  - cd web-client && npm run check
---

## Goal

The four reads `main.tsx` binds go out through `authorizedFetch`, so a signed-in player's strip,
record, ladder row and name all answer for the account and not for the device this browser happens to
be.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/account/authorized-fetch.ts`;
`web-client/src/profile/profile-provider.tsx` (the stable-reference rule this must not break);
`docs/protocol.md` *Sign in*.

## Scope

- One module-scope constant beside the existing four bindings:

  ```ts
  const apiFetch = authorizedFetch(
    (path, init) => window.fetch(path, init),
    localStorage,
  );
  ```

  and `readProfile`, `setName`, `readHistory` and `readLadder` each pass `apiFetch` where they passed
  the inline `window.fetch` arrow. After this, `main.tsx` names `window.fetch(` exactly once — inside
  that constant — and `fetch: apiFetch` exactly four times.
- **Module scope, not inline.** `ProfileProvider` and the two screen providers take the read as their
  only effect dependency, and a reference built during render re-runs the read on every render.
  `main.tsx` already carries that comment four times; this ticket keeps it true.
- Nothing else in `main.tsx` moves: the boot, the provider nesting and `roomCodeFromSearch` are
  untouched. In particular **no new export** — the reads stay module-private.
- The known consequence, recorded in a comment rather than discovered: `readFromApi` still returns
  `no-profile` without a request when no **device id** is stored, so a browser that holds a token and
  has never seen a `Welcome` reads nothing. It cannot arise — the device id is written by the first
  `Welcome` this browser ever receives and is **never cleared** (`ADR-0030` §8) — and the comment says
  which fact makes it unreachable. The comment must contain the literal phrase `never cleared`; a
  `verify:` command greps for it, because a limit nobody can find is the same as a limit nobody wrote.

## Out of scope

- **Sign-in, and sign-up.** Neither goes through this wrapper: sign-in carries no authentication at
  all by `docs/protocol.md`, and sign-up authenticates with `X-Device-Id` it sets itself.
  `TASK-041223` binds both to the plain `window.fetch`. **A refusal, not an omission** — and this
  ticket makes it one that trips: the two counts below are exact, so the day `TASK-041223` adds a
  fifth binding on a raw arrow, `wires all four reads through the wrapper and names the browser fetch
  once` reddens and that ticket must state, in its own diff, which bindings are deliberately
  unauthenticated. That redness is the point; it is not brittleness to design around here.
- Changing `profile.ts`, `duel-page.ts`, `ladder-read.ts` or `set-name.ts`. They each take an
  `ApiFetch` and this ticket hands them a different one; that is the whole reason the wrapper exists
  where it does.
- Refreshing a read when the token changes. Sign-in and sign-out reload the document
  (`TASK-041213`, `TASK-041214`), so every read re-runs from a fresh boot.
- **A test of the anonymous path.** `authorized-fetch.test.ts` already owns it behaviourally:
  `sends exactly the headers it was given when this browser holds none` asserts key-set **equality**
  on the recorded request, which is a stronger claim than "the key is absent" and is made against the
  wrapper itself. A second copy here would assert the wrapper again, not `main.tsx`'s wiring, and
  `main.tsx`'s wiring is the only thing this ticket changes. **A refusal, not an omission.**
- **A behavioural test that renders the real tree.** See *Notes* — it is not writable against
  `main.tsx` as it stands, and making it writable is a different ticket that is not yet filed.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside `binds the history read to
the browser fetch and the browser storage` — which already reads `main.tsx` from disk with
`readFileSync(resolve(here, "main.tsx"), "utf-8")`, the same way these two must.

One private helper in that file, used by both:

```ts
function occurrencesIn(source: string, needle: string): number {
  return source.split(needle).length - 1;
}
```

| Test | Proves |
| --- | --- |
| `wires all four reads through the wrapper and names the browser fetch once` | `occurrencesIn(mainSource, "fetch: apiFetch")` is `4` **and** `occurrencesIn(mainSource, "window.fetch(")` is `1`. Four bindings, and one raw browser fetch — the one the wrapper is built from. Wrapping a single read measures `1` and `4` and reddens both halves. **Two needles, two different expected answers**, which is also the vacuity guard: a helper that matched nothing, or returned a constant, satisfies at most one of them |
| `builds that wrapper once, at module scope` | `occurrencesIn(mainSource, "authorizedFetch(")` is `1` — one **construction**, counted by its call parenthesis, so the `import { authorizedFetch }` line cannot be mistaken for a second one — and `mainSource` matches `/^const apiFetch = authorizedFetch\(/m`, anchored at column 0. A wrapper built inside a component body is indented and fails the anchor |

Both read the file; **neither imports `main.tsx`** (`App.test.tsx` replaces that module wholesale with
`vi.mock("./main", …)` for every test in the file) and **neither builds an `authorizedFetch` of its
own** — a wrapper constructed in the test proves only that `authorized-fetch.ts` works, which
`authorized-fetch.test.ts` already proves.

## Acceptance criteria

- [ ] `App > wires all four reads through the wrapper and names the browser fetch once` passes,
      asserting **both** counts — `4` and `1` — in that one test
- [ ] `App > builds that wrapper once, at module scope` passes, asserting the count `1` **and** the
      column-0 anchored match
- [ ] Both tests reach `main.tsx` through `readFileSync` and the one `occurrencesIn` helper; neither
      imports `main.tsx`, and neither calls `authorizedFetch`
- [ ] `App > binds the history read to the browser fetch and the browser storage` and `App > binds
      the ladder read to the browser fetch and the browser storage` pass **unchanged**: each only
      asserts that `main.tsx` *contains* `window.fetch(` and `localStorage`, and one occurrence still
      satisfies a `toMatch`, so no assertion in either moves and neither is weakened
- [ ] `main.tsx` carries the device-id comment *Scope* names, containing the literal phrase
      `never cleared`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

The numbers below were **measured** against the real `main.tsx` before this ticket was written, not
predicted. Run each step and record the number you actually see; a mismatch with the table is a
finding worth reporting, not a cell to round off. Never record the unmutated state as a step's
"actual", and never write *would*, *if done* or *not testable* — a step you did not run is a step you
report as not run.

| `main.tsx` state | `fetch: apiFetch` | `window.fetch(` | `authorizedFetch(` | column-0 `const apiFetch =` |
| --- | --- | --- | --- | --- |
| before this ticket | 0 | 4 | 0 | no |
| **done** | **4** | **1** | **1** | **yes** |
| step 1 — one read wrapped | 1 | 4 | 1 | yes |
| step 2 — wrapper in a component | 4 | 1 | 1 | no |
| step 3 — wrapper on `window.fetch` | 0 | 5 | 1 | no |

1. Wrap only `readProfile` and leave the other three on the inline arrow.
   **`wires all four reads through the wrapper and names the browser fetch once` reddens on both of
   its assertions** — `1` where it wants `4`, and `4` where it wants `1`.
   `builds that wrapper once, at module scope` is **unaffected**: the construction is still there and
   still at column 0. That split is deliberate — one test answers *"are all four wired?"*, the other
   *"is there one wrapper, and where?"* — and a step that reddened both would mean they are the same
   test twice. Revert.
2. Move the wrapper inside `HistoryProvider`: delete the module-scope constant and build it as the
   first statement of the component body.
   **`builds that wrapper once, at module scope` reddens alone**, on the anchor — every count is
   unchanged, and the indented declaration no longer starts a line. This is the step that used to
   read *"nothing reddens"*; it now has an exit code, and that is why the anchor is in the criterion
   rather than left to a reviewer's eye.
3. Wrap `window.fetch` once, globally — assign the wrapper to `window.fetch` and put the four
   bindings back on the inline arrow.
   **Both tests redden**: `fetch: apiFetch` falls to `0`, `window.fetch(` rises to `5`, and the
   declaration is no longer a `const apiFetch`. This too used to be a shortcut the suite could not
   refuse; it can now. It is wrong for the reason `docs/protocol.md` gives — the day `TASK-041223`
   binds sign-in, a request that carries no authentication would carry a bearer token. Revert.
4. Make `occurrencesIn` ignore its `needle` and `return 4`.
   **Both tests redden**, and neither on its first assertion: `window.fetch(` reads `4` against `1`,
   and `authorizedFetch(` reads `4` against `1`. This is what two different expected answers buy, and
   it is the step to run if you are tempted to collapse the two needles into one. Revert.
5. Drop the storage argument — `authorizedFetch((path, init) => window.fetch(path, init))`.
   **`npm run typecheck` reddens** at the call site, so `npm run check` fails before it reaches
   vitest. It is the compiler, not a test, and saying so is more useful than pretending the suite
   covers it. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why a source assertion and not an integration test.** The obvious mechanism — render the real `App`
against a stubbed fetch and watch each path's request — cannot be written against `main.tsx` as it
stands, for three independent reasons:

1. `App.test.tsx` opens with a module-level `vi.mock("./main", …)` that replaces the whole module for
   **every** test in the file. Its own comment gives the reason: *"Node's own localStorage shadows
   jsdom's and is undefined, so mounting that real tree here throws inside `HistoryScreen`'s mount
   effect and rejects a promise nothing in this file awaits."*
2. `main.tsx` exports only `HistoryProvider`, `useHistory`, `LadderProvider` and `useLadder`.
   `readProfile` and `setName` are module-private, so two of the four bindings are unreachable from
   any test without adding exports — which *Scope* forbids.
3. `main.tsx` boots at import: `bootDuelClient({ connect: connectToDuelServer, … })` opens a socket
   and `ReactDOM.createRoot` renders. Importing it for real is not free.

Reaching the real bindings therefore means changing `main.tsx`'s shape and un-mocking `./main` for
the whole file — more than two files, and a different ticket. Not yet ticketed.

**Two earlier attempts failed on the mechanism, not the wiring.** The first scanned `main.tsx` for
`fetch: apiFetch` alone, which is a proxy for the property; the second replaced it with tests that
call the four reads **directly**, passing a wrapper the test constructs, and so never open `main.tsx`
at all — a reviewer wrapped one read, left the other three raw, and both tests still passed. The
lesson is not *"source assertions are weak"*. It is that a test must observe the artefact the ticket
changes: here the artefact is a wiring decision that lives only in a module nothing can import, so
its text is the only observable, and the guard against a scan that sees nothing is a **second needle
with a different answer** — the `TASK-040709` pattern, whose own note ends *one fixture default
cannot tell a copy from a constant*.

**`grep -c` counts lines, not matches.** The criterion this ticket used to carry asked
`grep -c 'authorizedFetch' web-client/src/main.tsx` to return `1`, glossed *"one wrapper, not four"*.
Any normal import makes that `2` — the `import` line and the call line — so it was unsatisfiable as
written, and two dispatches missed it because it was never in `verify:`. Both halves are fixed: the
count is over `authorizedFetch(` **with its call parenthesis**, `grep -o … | wc -l` counts
occurrences rather than lines so two calls on one line cannot hide, and it now runs in `verify:`.

## Notes

**Two attempts failed against criteria that could not be met, and the code was right both times.**
The old criterion `grep -c 'authorizedFetch' main.tsx` returning `1` is arithmetically impossible —
`grep -c` counts **lines**, and any normal import gives two. It was also **never in `verify:`**, which
is why two dispatches sailed past it. Both are fixed: the replacement counts *constructions*
(`grep -o 'authorizedFetch(' | wc -l` = 1, `grep -o 'fetch: apiFetch' | wc -l` = 4) and both sit in
the block.

**The second attempt is the instructive failure.** Reviewing the first, I judged its source-scan a
proxy rather than the property and asked for behavioural assertions. The rewrite called the read
functions **directly**, passing a wrapper the test constructed itself — so it never touched
`main.tsx`, and a reviewer applying Proof step 1 found it passed while three of four reads were
unwrapped. **"Assert the behaviour, not the text" inverts when the property under test is a property
of the wiring**: `main.tsx` is configuration, and a test that supplies its own configuration cannot
observe it. An integration test is unwritable here for three structural reasons the ticket now
records — a module-level `vi.mock("./main")` (Node's `localStorage` shadows jsdom's), two of four
bindings being module-private, and a socket opened at import.

**The one-character hole, and what actually closes it.** Asked which single character would leave
every assertion green while defeating the property, the coder found
`window.fetch(path, init)` → `window.fetch(path+ init)`: `tsc` exit 0, `eslint` exit 0, all 679 tests
green including both source assertions — and every read then goes to a URL built by concatenating
`init`'s `"[object Object]"`, carrying no headers. What catches it is **`format:check`**, because
Prettier requires a space around a binary `+`. The reviewer confirmed it exits non-zero. So the
property is closed by a formatting rule rather than by an assertion — true, verified, and worth
knowing, because it is not a gate anyone designed. The obvious alternative mutation (dropping `init`)
is caught independently by `noUnusedParameters` and `no-unused-vars`.

**The vacuity guard is two needles with two different answers.** Mutating `occurrencesIn` to return a
constant `4` reddens both tests, which one needle could never establish. Proof step 4's prose is
imprecise about *which* assertion fires first — `builds that wrapper once…` fails on its first
(`authorizedFetch(`: expected 4 to be 1), not its second — but the counts matched exactly and the gate
works.

**A criterion in `## Scope` that nothing ever checked is now gated.** The comment recording the
device-id consequence (`ADR-0030`: *"never cleared and never overwritten — not on sign-in, not on
sign-out"*) was demanded by Scope and absent from both earlier attempts, with nothing to say so. It
is held by `grep -qF 'never cleared'` in `verify:`.
