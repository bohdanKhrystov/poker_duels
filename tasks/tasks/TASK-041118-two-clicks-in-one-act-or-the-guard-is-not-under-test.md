---
schema: 2
id: TASK-041118
title: Two clicks inside one act, or the in-flight guard is not the thing under test
type: task
status: done
parent: STORY-0411
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, test, mutation]
depends_on: [TASK-041117]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/NameSurface.test.tsx 2>&1 | grep -qE 'Tests +9 passed \(9\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what the player typed, once, however many times the button is pressed'
  - test "$(grep -c 'act(() => {' web-client/src/profile/NameSurface.test.tsx)" = 2
  - test "$(grep -c 'submitInFlight' web-client/src/profile/NameSurface.tsx)" = 4
  - cd web-client && npm run check
---

## Goal

`NameSurface`'s double-submit test starts failing when the `useRef` guard is removed — which it does
not do today, so the one line standing between a player and two `PUT /api/me/name` requests is
currently held by nothing.

## Why this exists

`TASK-041110` shipped `sends what the player typed, once, however many times the button is pressed`
with two bare `fireEvent.click()` calls. **That pair gates nothing.**
`@testing-library/react` wraps every `fireEvent` in its own `act()`, so React flushes between the two
calls: by the time the second click is dispatched, `setIsSubmitting(true)` has already committed and
the button carries `disabled`, so jsdom runs no activation behaviour and no second `handleSubmit`
ever starts. The call count is `1` **because of the state guard alone**. Delete
`NameSurface.tsx`'s `submitInFlight` ref entirely and the test still passes.

It was found when the identical mutation reddened nothing in `TASK-041218`'s planning, and confirmed
independently at review against both commits. `TASK-041218`'s `## Notes` records the mechanism.

The ref is not decorative: `NameSurface.tsx`'s own comment gives the reason — *"the guard below must
see the current in-flight status the instant the second submit runs, not after a render has caught
up"* — and `ADR-0029` makes a display name permanent, so the second request is a race whose loser
gets a `403` about a name the player never chose twice.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.test.tsx` | modify |
| `web-client/src/profile/NameSurface.tsx` | modify |

The second row is the **Proof mutation and nothing else**: `NameSurface.tsx` is mutated transiently
in step 1 below, reverted, and is byte-unchanged in the merged diff — the `grep -c 'submitInFlight'`
line in `verify:` is what holds that. It is a *Files* row rather than a read-only citation because
`TASK-041634` recorded a Proof step **refused** for exactly this reason: a coder correctly declines
to edit a file the table does not name, even transiently, and a Proof nobody may run is a Proof
nobody runs.

Read, and do not edit: `web-client/src/store/reconnect.test.tsx` (how an async component test is
written here).

## Scope

- In `sends what the player typed, once, however many times the button is pressed`, wrap **each** of
  the two consecutive-click pairs in a **single** `act(() => { … })` — the pair on the first render
  and the pair on the second render after `unmount()`. Both dispatches go inside one `act` body:

  ```ts
  act(() => {
    fireEvent.click(firstButton);
    fireEvent.click(firstButton);
  });
  ```

- `act` is imported from `@testing-library/react` beside `fireEvent` — it is re-exported there at
  v14, which is the pinned major.
- **Why this is the shape**: `act` is re-entrant and flushes only when the *outermost* call exits, so
  the state update from the first click has not committed while the second is dispatched. The button
  is still enabled, `handleSubmit` runs a second time, and `submitInFlight.current` is the only thing
  that can stop it. Carry that reason as a comment above the first `act`, naming the ref — a reader
  who removes the wrapper for tidiness re-opens the hole.
- **No assertion moves and none is weakened.** Every existing `expect` in the test stays exactly as
  it is, including `expect(firstButton.disabled).toBe(true)`: `act` has exited by the time that line
  runs, so `isSubmitting` has committed and the button is disabled either way.
- Nothing else in the file changes, and no other test gains an `act`.

## Out of scope

- **Any change to `NameSurface.tsx`'s behaviour.** The component is correct; the test is what is
  wrong. The ref, the state, the `disabled` binding and the comments all stay.
- **The other eight tests in the file.** None of them dispatches two events that must not be
  separated by a render, so wrapping them would add noise and gate nothing.
- **`SignUpForm.test.tsx`'s copy of this shape.** `TASK-041218` is amended to require the same
  `act` nesting for its own `sends nothing on a second submit while one is in flight`, in its own
  diff — that file does not exist yet.
- **A lint rule against two adjacent `fireEvent` calls.** Adjacent dispatches are usually fine; it is
  only a pre-render race that needs the batching, and a rule that cannot tell those apart is one
  somebody switches off.

## Tests

`web-client/src/profile/NameSurface.test.tsx`, describe block `"the name surface"`. **No test is
added and none is renamed** — the file still reports **9**. What changes is that one of the nine
becomes able to fail.

| Test | Proves |
| --- | --- |
| `sends what the player typed, once, however many times the button is pressed` *(modified)* | Unchanged in what it asserts — one `setName` call per render, the typed string byte for byte, the button disabled while in flight, and two resolvers pending at the end. Changed in what it **observes**: with both clicks batched into one `act`, the second submit reaches `handleSubmit`, so the count of `1` is now a statement about the ref rather than about the `disabled` attribute |
| the other eight *(unchanged)* | Still pass, byte-unchanged. None of them clicks twice |

## Acceptance criteria

- [ ] `the name surface > sends what the player typed, once, however many times the button is
      pressed` passes, with **both** click pairs each wrapped in a single `act(() => { … })`
- [ ] `grep -c 'act(() => {' web-client/src/profile/NameSurface.test.tsx` returns `2` — one per click
      pair, and no third
- [ ] No `expect(...)` line in that test is added, removed or changed, and no assertion is weakened;
      `git diff` on the test shows the two `act` wrappers, the import and the comment, and nothing
      else
- [ ] The other eight tests in the file are byte-unchanged
- [ ] `grep -c 'submitInFlight' web-client/src/profile/NameSurface.tsx` returns `4` — the mutation
      below was reverted and the production file is as it was
- [ ] `npm run test -- src/profile/NameSurface.test.tsx` reports `Tests  9 passed (9)`
- [ ] The PR body records the **measured** result of Proof step 1 on both sides: the mutation against
      the file as it stands today, and the mutation against the fixed file. A step reported as
      *"would redden"* is a step that was not run
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Step 1 is this ticket's whole subject, and it must be run twice — before the fix and after.** In
`NameSurface.tsx`, delete the ref guard and keep the state guard: remove the
`if (submitInFlight.current) { return; }` block and the `submitInFlight.current = true;` line,
leaving `setIsSubmitting(true)`, the `disabled={isSubmitting}` binding and the `false` reset in the
`then` exactly as they are.

1. **Against the test as `TASK-041110` merged it — nothing reddens.** All nine pass. The second
   `fireEvent.click` lands on a button React has already re-rendered as `disabled`, so no submit
   starts and the count is `1` for a reason that has nothing to do with the ref. **This green run is
   the defect**, and it is the number to paste into the PR first.
2. **Against the fixed test — `sends what the player typed, once, however many times the button is
   pressed` reddens**, on `expect(setNameSpy).toHaveBeenCalledTimes(1)`, *expected 1, received 2*.
   The other eight stay green: none of them submits twice. Revert `NameSurface.tsx` and confirm the
   `grep -c 'submitInFlight'` line reads `4` again.
3. **Wrap only one of the two pairs, and keep the ref deleted.** The test still reddens either way —
   wrap only the first and the failure is at `toHaveBeenCalledTimes(1)`; wrap only the second and it
   moves to `toHaveBeenCalledTimes(2)`, *received 3*. **So the mutation cannot tell one wrapper from
   two**, which is exactly why the criterion counts `act(() => {` rather than trusting step 2. Write
   this down in the PR rather than reasoning past it. Restore both.
4. Revert the fix — put the two bare `fireEvent.click` calls back, with `NameSurface.tsx` intact.
   **The suite is green.** It was green before this ticket and it is green after reverting it, which
   is the point worth stating out loud: on this test a passing run distinguishes nothing, and only
   step 1's pair of runs does. Restore the fix.
5. Replace `act(() => { … })` with `await act(async () => { … })`.
   **Nothing changes** — both flush at the outermost exit, and the test already awaits later.
   Recorded as inert on purpose, so the next reader does not take the async form for a second
   guarantee, and so nobody "improves" it believing they changed something.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The old test asserted the right property and passed for the wrong reason — a failure distinct from
the ones this run has been finding.** It was not a fixture blind to its property, nor an assertion
that could not redden. It measured a **real** effect produced by a **different** mechanism than the
one under test.

The full chain, verified by coder and reviewer independently: `fireEvent` wraps each call in its own
`act()`, so in the merged test the first click dispatches, React flushes, `isSubmitting` commits, the
**button becomes `disabled`**, and jsdom suppresses the second click entirely. The observed call count
of `1` came from the disabled attribute — a fact the **state guard alone** produces. The
`submitInFlight` ref was never exercised, and the test could not distinguish a component holding a ref
from one holding none.

**Both halves of the proof were measured, and both were necessary.** The mutation — delete the ref
guard, keep the state — run against the **merged** test gives **9 passed, green**, which is what
demonstrates the defect was real. The same mutation against the **fixed** test gives *expected "spy"
to be called 1 times, but got 2 times*. A red result alone would not distinguish a repaired gate from
one that was never broken.

**The fix is entirely in the test; `NameSurface.tsx` is byte-unchanged** and `submitInFlight` still
occurs four times. Wrapping both dispatches in **one outer** `act()` defers the inner flushes, so the
button never disables between them, the second click actually runs, and only the ref stops it.

**A criterion counts `act(() => {` at exactly 2, and that count is load-bearing.** The mutation reddens
identically whether one click-pair or both are wrapped, so it **cannot distinguish a half-fix** — the
count is the only thing that does. The coder also measured that `await act(async () => {…})` changes
nothing and reverted to the synchronous form to keep the count exact.

**An environment quirk worth knowing before it wastes an hour.** This session's command-safety checker
refuses the inline form `test "$(grep -c 'act(() => {' …)" = 2`, misreading the nested parentheses;
the identical logic runs fine from a script file. That is a sandbox artefact, not an unsatisfiable
criterion.
