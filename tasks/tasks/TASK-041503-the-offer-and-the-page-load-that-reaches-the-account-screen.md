---
schema: 2
id: TASK-041503
title: The offer itself, and the page load that reaches the account screen
type: task
status: done
parent: STORY-0415
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041501]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the stake before it asks for anything'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leads to the account screen through a page load'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries no form of its own'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onDismiss when Not now is taken'
  - test "$(grep -oF 'hashForScreen("account")' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '"#/account"' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'useScreen' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'localStorage' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

The offer is a component: `TASK-041501`'s four sentences, a link that reaches the account screen by
**loading the document**, and *Not now*, which calls the handler it was given and decides nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/AccountOffer.tsx` | create |
| `web-client/src/result/AccountOffer.test.tsx` | create |

Read, and do not edit:

- `web-client/src/result/account-offer-text.ts` — `TASK-041501`'s four constants. This component
  authors no string of its own.
- `web-client/src/routing/screen.ts` — `hashForScreen`, and the comment above it explaining why the
  slug is a literal there and nowhere else.
- `web-client/src/result/DuelResult.tsx` — the `<a href="/">` *Back to the lobby* link, whose
  `className` this link copies verbatim and whose page-load semantics are the ones this link needs
  (`ADR-0076` §6).
- [`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §3 and §6 — the
  store outranks the address, and the result screen's links stay real page loads. **§3 is why this
  is a link and not `useScreen().open("account")`; see `## Notes`, where it is measured.**
- `web-client/src/result/RematchControl.tsx` — the house shape for a small prop-driven control that
  the result screen is handed.

## Scope

- One component, prop-driven, no hooks, no provider read, no fetch, no storage — the
  `AccountScreen`/`HistoryScreen`/`LadderScreen` shape (`ADR-0060` §4), so it renders in a test
  alone:

  ```tsx
  export function AccountOffer(props: {
    readonly onDismiss: () => void;
  }): ReactElement;
  ```

- **The section carries `aria-label="the offer"`**, which is how every test in this ticket and in
  `TASK-041504` finds it — the same device `DuelResult`'s `aria-label="the result"` already uses.
- **The accept control is `<a href={`/${hashForScreen("account")}`}>`, and the leading `/` is the
  whole point.** `hashForScreen("account")` alone is `#/account`, a same-document fragment change;
  with the leading slash it is `/#/account`, a **request for the root document** that rebuilds
  `initialState()`. `ADR-0076` §6 requires that of the result screen's links, and `ADR-0075` says
  why: three presence fields are cleared at no duel boundary, and those page loads are the only
  reason the hole is unreachable. Two `verify:` lines gate it — the derivation must be present
  exactly once and the bare literal `"#/account"` must not appear at all.
- **`OFFER_ACCEPT` is the link's text**, so `getByRole("link", { name: "Keep them with a
  password" })` finds it. Copy comes from the text module; this file writes no sentence.
- **`Not now` is a `<button type="button">` calling `props.onDismiss`.** Not a link, not a submit:
  it navigates nowhere and posts nothing.
- Tailwind classes only, in the existing vocabulary — copy the way-back link's class string for the
  accept link so the two read alike.

## Out of scope

- **`useScreen`, `open()`, `window.location.hash` and `history` — any of them.** A `verify:` line
  greps `useScreen` at zero. Measured in `## Notes`: from the result screen, an in-page navigation
  to `#/account` is **reverted before it renders**.
- **Deciding whether to render.** The component has no `verdict`, no `signedIn` and no `settled`
  prop; `TASK-041502`'s `offerAccount` decides and a later ticket wires it. A component that
  returned `null` for itself would be a second place able to get the trigger wrong.
- **Doing anything on dismissal beyond calling `onDismiss`.** Where that handler writes was
  `DEC-079`'s and `DEC-080`'s; both are now answered — `ADR-0085` and `ADR-0086`, which put it in
  `result/account-offer-settled.ts` — and **this refusal still stands**, because the write belongs
  to the wiring ticket and not to this component.
- **Placing it on the result screen.** `TASK-041504` adds the slot; the ticket after the decisions
  fills it.
- A heading element (`<h2>`/`<h3>`). `OFFER_HEADING` is a `<p>`: `DuelResult` already owns the one
  `<h2>` in that panel, and a second heading would change what `getByRole("heading", …)` returns in
  four merged tests.

## Tests

`web-client/src/result/AccountOffer.test.tsx` — a new file, four tests. Names must match `verify:`
exactly.

| Test | Proves |
| --- | --- |
| `names the stake before it asks for anything` | `OFFER_HEADING` and `OFFER_BODY` both render. Imported from the text module, never retyped: a literal here would pass while the module said something else |
| `leads to the account screen through a page load` | The accept link's `href` is exactly `"/#/account"`. This is the assertion that separates a page load from a fragment change, and it is the only place the difference is visible |
| `carries no form of its own` | No `<form>`, no `role="textbox"`, no `<input>` inside the offer. `STORY-0415`'s *"Accepting it opens the account screen rather than a form of its own"* |
| `calls onDismiss when Not now is taken` | `fireEvent.click` on the button named `Not now` calls the injected handler exactly once |

**No `try` anywhere in this file, and no `expect()` inside one.** A failing assertion is itself a
throw; a `try` around one swallows it and the test passes green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the stake before it asks for anything'`
      — passes, asserting both constants through an import
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leads to the account screen through a page load'`
      — passes, and the asserted value is `"/#/account"`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries no form of its own'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onDismiss when Not now is taken'`
      — passes
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx 2>&1 | grep -qE 'Tests +4 passed \(4\)'`
      — exactly four tests in the new file
- [ ] `test "$(grep -oF 'hashForScreen("account")' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1`
      — the address is derived from `screen.ts`, once
- [ ] `test "$(grep -oF '"#/account"' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      — the slug is not written out here, in code or in a comment
- [ ] `test "$(grep -oF 'useScreen' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'localStorage' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      — no in-page navigation and no storage. **These read the whole file, comments included**: say
      *the address bar* in prose rather than writing either needle
- [ ] `cd web-client && npm run check` exits 0. With `TASK-041501` merged the suite reads
      **816 passed (816)**; the four-ticket projection reads **822 passed (822)**
- [ ] Every pre-existing test passes unchanged — this diff adds two files and edits none
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Every step below was run in this worktree**, with `TASK-041501`'s two files and this ticket's two
files applied to `develop` at `922d57fc`: **816 tests / 105 files**, green, plus typecheck, lint
and `prettier --check`. Baseline without them is **811 / 103**. Record what you actually measure; a
mismatch is a finding, not a cell to round off. Never record the unmutated state as a step's
"actual", and never write *would*, *if done* or *not testable*.

1. Drop the leading slash — `href={hashForScreen("account")}`. **`leads to the account screen
   through a page load` reddens alone** — measured, `1 failed | 815 passed (816)`. This is the
   defect that matters most in this ticket and the one nothing else in the tree would catch:
   `#/account` from the result screen is a same-document fragment change, and `## Notes` measures
   what happens to it. Revert.
2. Delete the `<p>{OFFER_BODY}</p>` line. **`names the stake before it asks for anything` reddens
   alone** — measured, `1 failed | 815 passed (816)`. Revert.
3. Add a `<form>` with a labelled `<input>` inside the section. **`carries no form of its own`
   reddens alone** — measured, `1 failed | 815 passed (816)`. This step exists because a
   *"queryByRole(…) is null"* test is exactly the shape that can be vacuous; it is not. Revert.
4. Remove `onClick={props.onDismiss}` from the button. **`calls onDismiss when Not now is taken`
   reddens alone** — measured, `1 failed | 815 passed (816)`. Revert.
5. **A cross-ticket check, run outside this ticket's two files** — the budget governs the diff, not
   the probe. Change `OFFER_DISMISS` in `account-offer-text.ts` to `"Later"`. **Two tests redden** —
   measured, `2 failed | 814 passed (816)`: `TASK-041501`'s `states every sentence exactly,
   character for character, and names the stake`, and this ticket's `calls onDismiss when Not now is
   taken`, which queries the button by that literal name. Revert `account-offer-text.ts` completely;
   it is `read, do not edit`.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.**
> The warning is misleading and was checked rather than believed: with `NO_COLOR=1` the reporter's
> summary line is **plain bytes** (`cat -v` shows no escapes), and **without** it the same line
> carries them. Keep `NO_COLOR=1` on every grep in `verify:` or the `Tests +4 passed \(4\)` line
> silently stops matching.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

### Why the accept control is a link and not `open("account")` — measured, not reasoned

`ADR-0076` §3 says the store outranks the address, and `Lobby.tsx` enforces it in an effect: while
`state.outcome !== null` — which is exactly when the result screen is showing — any screen other
than `first` is **replaced back to `/`**. So the obvious implementation, calling `useScreen().open("account")`
from the offer, cannot work.

That was not taken on faith. Rendering `Lobby` with a store holding a `DuelFinished`, then setting
`window.location.hash = "#/account"` and flushing the `hashchange` task, measured:

```
hash=""   account screen rendered = false   "Victory" still on screen = true
```

and the same navigation with **no** outcome in the store measured `hash="#/account"`, account
screen rendered `= true`. The store wins, exactly as §3 says, and the offer would have looked
broken with every gate green.

A page load has no such problem: it discards the store, rebuilds `initialState()`, and the fragment
is read fresh at boot. That is the identical mechanism `main.tsx`'s account landing already uses
after a sign-in (`TASK-041229`, `ADR-0083` §5), and `ADR-0076` §6 requires it of the result screen
anyway.

**One consequence to state rather than discover later:** the accept link does **not** call
`forgetRoom`, so the room code this browser remembers survives into the next boot. Whether it
should is the wiring ticket's question, not this component's — `DuelResult` already owns an
`onLeave` for exactly that and this ticket does not touch it.

### `grep -c` counts lines, not occurrences

Every count in `verify:` is `grep -oF … | wc -l`, and the zero-expectations are wrapped as
`test "$(…)" = 0` because `grep` exits **1** when it matches nothing — a bare `grep -c … = 0` fails
the step it is supposed to pass. `-F` is on every needle so `(`, `)`, `"` and `#` stay literal.
