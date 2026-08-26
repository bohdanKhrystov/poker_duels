---
schema: 2
id: TASK-041231
title: Whether this browser holds a token is read once, above the tree
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, auth, wiring]
depends_on: [TASK-041221]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads whether this browser holds a token once, at module scope'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands that flag to the tree and reads it in no component'
  - test "$(grep -o 'readSessionToken(' web-client/src/main.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -o '= useSignedIn()' web-client/src/main.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`useSignedIn()` exists and answers whether this browser holds a session token, so the account screen
can be handed the flag `AccountScreen` has required since `TASK-041217` merged.

## Why this exists

`TASK-041222` mounts `AccountScreen`, whose `signedIn: boolean` prop is **required** and merged. Its
*Scope* says the flag is *"read once where the other module-scope bindings live in `main.tsx` and
passed down"* — and its *Files* table has no `main.tsx` row. That was read as a register disagreement
to tidy up. It is not: **there is no prop path at all.** `App.tsx` renders `<Lobby />` with no props
and `Lobby()` takes none, so nothing `main.tsx` computes can reach the lobby except through a
context. `TASK-041223` cannot supply it either — it holds `main.tsx` but lands *after* `TASK-041222`,
so `Lobby.tsx` would import a hook that does not yet exist and `tsc` would refuse the merge.

The carrier needs no decision, because the pattern is merged and in use twelve lines above where this
goes: `Lobby.tsx` already opens with `import { useHistory, useLadder } from "../main";`, and both
are contexts whose value is a module-scope constant in `main.tsx`. This ticket is a third one.

Splitting it out rather than making `TASK-041222` a four-file `atomic:` is this story's own settled
remedy, applied a third time: `TASK-041228` and `TASK-041229` were both cut for the same reason, and
`ADR-0068` and `ADR-0070` are the rule — **no merged gate refuses the intermediate state**, because
the client gate is green with the provider and no account screen, and would be green with the
account screen and no provider if the order were reversed. Two things nothing holds together are two
tickets.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read, and do not edit: `web-client/src/protocol/session-token.ts` (`readSessionToken`);
`web-client/src/account/AccountScreen.tsx` (the prop being fed, and nothing else);
[`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) §2.

## Scope

- One module-scope constant beside the existing bindings, at column 0:

  ```ts
  const signedIn = readSessionToken(localStorage) !== null;
  ```

- One context, one provider and one hook, copied member for member from `HistoryProvider` /
  `useHistory` in the same file, including the stable-reference comment: `SignedInProvider` takes the
  flag as `value` and `useSignedIn()` answers `false` where no provider is above — `false`, not
  `null`, because *this browser holds no token* is the honest reading of *nobody told me*, and every
  consumer would otherwise write the same `?? false`.
- `SignedInProvider` wraps the tree beside the others, with `value={signedIn}`.
- **Module scope, not inside a component.** `DEC-032` records that Node 24+ defines an inert
  `localStorage` global that shadows jsdom's under Vitest, so a component that reaches for the global
  is a component whose tests do not test the browser. Carry that reason as a comment naming
  `DEC-032`.
- **Read once, at boot, and never again.** Sign-in and sign-out reload the document
  (`TASK-041213`, `TASK-041214`), so a fresh boot is what recomputes it. Say so in the hook's doc
  comment, because the absence of a subscription is the kind of thing a later reader adds back.
- Nothing else in `main.tsx` moves: the boot, `roomCodeFromSearch`, `apiFetch` and the four read
  bindings are untouched.

## Out of scope

- **Mounting the account screen, or any use of the hook.** `TASK-041222` calls `useSignedIn()` in
  `Lobby.tsx`. This ticket exports it and nothing consumes it yet — which is why one of the two tests
  below asserts a count of **zero**.
- **Recomputing the flag when the token changes.** No `storage` event listener and no subscription;
  the reload is the mechanism. **A refusal, not an omission.**
- **Binding the four account calls.** `TASK-041223`, in this same file, afterwards.
- Any change to `session-token.ts`, `App.tsx` or `AccountScreen.tsx`.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`, beside the two binding tests
`TASK-041210` added and using the same private `occurrencesIn` helper and the same
`readFileSync(resolve(here, "main.tsx"), "utf-8")`.

**Why source assertions and not a rendered tree**: `TASK-041210`'s `## Notes` argues it at length and
nothing here changes it — `App.test.tsx` opens with a module-level `vi.mock("./main", …)` that
replaces the whole module for every test in the file, and `main.tsx` boots a socket at import. A test
that imports the hook gets the mock; one that imports it for real opens a connection. The artefact
this ticket changes is a wiring decision in a module nothing can import, so its text is the only
observable.

| Test | Proves |
| --- | --- |
| `reads whether this browser holds a token once, at module scope` | `occurrencesIn(mainSource, "readSessionToken(")` is `1`, and `mainSource` matches `/^const signedIn = readSessionToken\(/m`, anchored at column 0. A read inside a component body is indented and fails the anchor; a second read anywhere fails the count |
| `hands that flag to the tree and reads it in no component` | `occurrencesIn(mainSource, "value={signedIn}")` is `1` **and** `occurrencesIn(mainSource, "= useSignedIn()")` is `0` — `main.tsx` provides the flag and never consumes its own context. **Two needles with two different expected answers**, which is also the vacuity guard: a helper returning a constant satisfies at most one of them. The second needle carries its `= ` on purpose: the hook's own declaration reads `export function useSignedIn(): boolean`, which **contains** the substring `useSignedIn()`, so a bare needle can never reach `0` and the assertion would be unsatisfiable. Write the comment saying so beside it |

## Acceptance criteria

- [ ] `App > reads whether this browser holds a token once, at module scope` passes, asserting the
      count `1` **and** the column-0 anchored match
- [ ] `App > hands that flag to the tree and reads it in no component` passes, asserting **both**
      counts — `1` for `value={signedIn}` and `0` for `= useSignedIn()` — in that one test. The
      second needle keeps its `= `; `export function useSignedIn(): boolean` contains
      `useSignedIn()`, so a bare needle cannot answer `0` and the assertion would be unsatisfiable
- [ ] Both tests reach `main.tsx` through `readFileSync` and the one existing `occurrencesIn` helper;
      neither imports `main.tsx` and neither declares a second helper
- [ ] `useSignedIn()` returns `false`, not `null` and not `undefined`, where no provider is above
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged, and no assertion in any of them
      moves: the two `TASK-041210` count tests read `fetch: apiFetch`, `window.fetch(` and
      `authorizedFetch(`, and this ticket adds none of those three strings
- [ ] `main.tsx` carries a comment naming `DEC-032` beside the module-scope read
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Move the read inside `SignedInProvider`'s body — `const signedIn = readSessionToken(localStorage)
   !== null;` as the first statement of the component.
   **`reads whether this browser holds a token once, at module scope` reddens alone**, on the anchor:
   the count is still `1` and the declaration is now indented, so it no longer starts a line. Every
   other test in the file is unaffected. This is the mutation `DEC-032` is about and the reason the
   anchor is in the criterion rather than left to a reviewer's eye. Revert.
2. Have `useSignedIn()` fall back to `readSessionToken(localStorage) !== null` when the context is
   absent, instead of `false`.
   **`reads whether this browser holds a token once, at module scope` reddens** on the count — `2`
   against `1` — and nothing else moves. Run it: a friendly-looking default is a second read, in a
   component, of the global `DEC-032` says is shadowed. Revert.
3. Delete `value={signedIn}` and pass `value={true}`.
   **`hands that flag to the tree and reads it in no component` reddens on its first assertion
   alone**, `0` against `1`; the second still reads `0`. The screen would then tell every visitor
   their password signs them in, which is `TASK-041217`'s statement made false — and no rendered test
   in this file can see it, which is why the wiring is asserted textually.
4. Make `occurrencesIn` ignore its needle and return `1`.
   **`hands that flag to the tree and reads it in no component` reddens on its second assertion**,
   `1` against `0`, while its first passes and so does the count in test one. That is what two
   different expected answers buy, and it is the step to run before collapsing the two needles into
   one. Revert.
5. Export the provider but never wrap the tree in it.
   **`hands that flag to the tree and reads it in no component` reddens** on the `value={signedIn}`
   count, because the only place that string appears is the JSX. `npm run check` stays green —
   an unused export is not an error — which is exactly why the count is the gate.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
