---
schema: 2
id: TASK-041506
title: The accept control is an answer too, and says so before the page loads
type: task
status: done
parent: STORY-0415
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041504]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onAccept when the offer is taken, and leaves the loading to the link'
  - test "$(grep -oF 'props.onAccept' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'hashForScreen("account")' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF '"#/account"' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'useScreen' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'localStorage' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'preventDefault' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`AccountOffer` gains a required `onAccept`, called from the accept link's click handler — so that
taking the offer can be an answer the way *Not now* already is, without the component learning what
an answer costs.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/AccountOffer.tsx` | modify |
| `web-client/src/result/AccountOffer.test.tsx` | modify |

Read, and do not edit:

- [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  §6 — the whole brief. The write runs from a click handler on the anchor, **before** the browser
  navigates, and **not** on the account screen's load, because the lobby's own account control
  reaches that screen too and would settle an offer nobody was made.
- [`ADR-0085`](../../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)
  §2 — *"Both controls are answers."* That sentence is why this prop exists.
- `web-client/src/result/DuelResult.tsx` — the `onLeave` prop's KDoc, which is the merged precedent
  quoted by `ADR-0086` §6: *"The link stays an `<a href="/">`, so the handler runs and navigation
  stays the browser's. Storage operations are synchronous, so a handler that forgets has finished
  before the page leaves."* Copy that reasoning, and the `onClick={…}`-beside-`href` shape.
- [`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §6 — the result
  screen's links stay real page loads. The handler must not change that.

## Scope

- **One required prop and one attribute.** In the props type, above `onDismiss`:

  ```tsx
  /**
   * Called on the accept control's click, before the browser loads the account
   * screen (`ADR-0086` §6) …
   */
  readonly onAccept: () => void;
  ```

  and `onClick={props.onAccept}` on the existing `<a>`, beside its `href`. Measured: **twelve added
  lines** in `AccountOffer.tsx`, ten of them KDoc.
- **Required, not optional, and that is the design.** A caller that forgot an optional handler would
  spend nothing and no gate anywhere would notice; a missing argument is a typecheck failure, which
  is the cheapest gate there is. It is also why this ticket edits the four merged renders below.
- **The handler must not cancel the click.** No `preventDefault`, no `return false`, no
  `href` change — a `verify:` line pins `preventDefault` at zero, and the new test asserts the
  dispatch was not cancelled. `ADR-0076` §6 needs the page load; `ADR-0075` says why.
- **Nothing else in the component moves.** The four strings still come from `account-offer-text.ts`,
  the section still carries `aria-label="the offer"`, the href is still derived once from
  `hashForScreen("account")`, and `TASK-041503`'s four gates are restated in this ticket's `verify:`
  so the new prop cannot smuggle any of them in.

## Out of scope

- **Writing anything.** No storage, no `markOfferSettled`, no import from `account-offer-settled.ts`.
  The component calls the handler it was given and knows nothing about what the handler does —
  `TASK-041503` §Out of scope, unchanged by `ADR-0085` and `ADR-0086` having since answered where
  the write lands.
- **Deciding whether to render.** Still no `verdict`, no `signedIn`, no `settled` prop.
- **Hiding itself after either control.** The component has no state and gains none; what happens to
  the screen afterwards is `TASK-041507`'s.
- **Filling the slot in `Lobby.tsx`.** `TASK-041507`.
- **Renaming `onDismiss`.** A single `onAnswer` covering both controls would read tidier and would
  break the merged `calls onDismiss when Not now is taken`, which this diff must leave standing.

## Tests

`web-client/src/result/AccountOffer.test.tsx` — **4 merged tests become 5.** The new one is appended
after `calls onDismiss when Not now is taken`.

| Test | Proves |
| --- | --- |
| `calls onAccept when the offer is taken, and leaves the loading to the link` | `fireEvent.click` on the link named `OFFER_ACCEPT` calls `onAccept` exactly once, calls `onDismiss` **zero** times, returns `true` (the dispatch was not cancelled, so navigation stays the browser's), and leaves the `href` at `"/#/account"`. Four assertions, and each is a different defect: no handler, one handler wired to both controls, a handler that swallows the click, and a handler that replaced the link |

**The four merged tests keep every assertion**, and the only edit each takes is the new required
prop in its `render(…)` call — `render(<AccountOffer onAccept={vi.fn()} onDismiss={onDismiss} />)`.
Nothing they assert moves, nothing is weakened, and no test is renamed. Say it that way in the PR:
this diff changes what those four tests *construct*, not what they *observe*.

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green (`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onAccept when the offer is taken, and leaves the loading to the link'`
      — passes, asserting all four things above
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/result/AccountOffer.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly five in exactly one file**. Four
      merged plus this one; a deleted merged test reads 4 and fails. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'props.onAccept' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1`
      — the handler is wired in exactly one place, so it cannot also sit on the dismiss button
- [ ] `test "$(grep -oF 'preventDefault' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      — the click is not cancelled, so `ADR-0076` §6's page load still happens
- [ ] `test "$(grep -oF 'hashForScreen("account")' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 1`,
      `test "$(grep -oF '"#/account"' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`,
      `test "$(grep -oF 'useScreen' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'localStorage' web-client/src/result/AccountOffer.tsx | wc -l | tr -d ' ')" = 0`
      — `TASK-041503`'s four gates, restated. **These read the whole file, comments included**: say
      *the address bar* in prose rather than writing either needle
- [ ] `cd web-client && npm run check` exits 0. With `TASK-041505` merged the suite reads
      **829 passed (829)** over **107** files
- [ ] The four merged tests in `AccountOffer.test.tsx` keep every assertion they had — no expected
      value moves, none is weakened, and none is renamed
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Measured in this worktree.** The suite state each step was run in is named, because this ticket's
change is visible at three levels and only one of them exists at its own merge point. Baseline on
`develop` at `77c61708`: **822 / 106**. With `TASK-041505` and this ticket: **829 / 107**, green,
plus typecheck, lint and `prettier --check`.

1. **Remove `onClick={props.onAccept}` from the anchor.** Run on the **full projection**
   (`TASK-041505`–`TASK-041508` all applied): **three tests redden, one per level** — measured. The
   run's totals are not quoted because that projection carried one more `Lobby.test.tsx` test than
   the one that shipped; the three names below are what the step establishes and they are unchanged:
   - `AccountOffer.test.tsx > calls onAccept when the offer is taken, and leaves the loading to the
     link` — **this ticket's own, and the only one of the three that exists at this ticket's merge
     point.** A coder running this step here sees that one alone.
   - `Lobby.test.tsx > answers from either control, and only Not now takes the offer off the screen`
     — `TASK-041507`'s.
   - `drive-arc.test.tsx > spends the offer on the way to the account screen too` — `TASK-041508`'s.

   Worth writing down rather than discovering: **this one attribute is the whole accept-side path**,
   and nothing above it can compensate for its absence. Revert.
2. **jsdom does not complain.** The click on `<a href="/#/account">` produced no *"Not implemented:
   navigation"* on stderr and no unhandled rejection — measured on the filtered run, `Test Files 1
   passed (1)`, `Tests 5 passed (5)`, clean output. The `fireEvent.click` return value is therefore a
   real signal: it is `true` here and would be `false` for a handler that called `preventDefault`.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.**
> Checked rather than believed: with `NO_COLOR=1` the summary line is plain bytes; **without** it,
> `cat -v` shows `^[[2m      Tests ^[[22m ^[[1m^[[32m5 passed^[[39m^[[22m^[[90m (5)^[[39m` and the
> `grep -qE` stops matching. Keep `NO_COLOR=1` on every grep.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this is its own ticket rather than part of the wiring.** `ADR-0086` §6 states the clause it
fixes as one *for the wiring ticket*, but the two are a split rather than an `atomic:` set: this
diff is green on its own, measured, and `TASK-041507`'s four files already sit at the file count a
merged gate forces. A set of files no gate holds together is two tickets (`ADR-0068`, `ADR-0070`).

**Its `Files` table is disjoint from `TASK-041505`'s**, and both depend only on merged work, so the
two can run in one batch — the same shape `TASK-041502`–`TASK-041504` ran in.

**Measured size: 38 changed lines** — 12 in the component, 26 in its test, of which 4 are the merged
renders gaining the new prop.

`grep -c` counts matching **lines** and exits **1** on zero matches, so every zero-expectation is
wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`. `-F` keeps `(`, `)`, `"` and `#` literal.
