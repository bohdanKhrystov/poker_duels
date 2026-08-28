---
schema: 2
id: TASK-041722
title: The one-field form that asks for a reset link, and answers everyone the same way
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, ui]
depends_on: [TASK-041721]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ForgotPasswordForm.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ForgotPasswordForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed, once, exactly as it was typed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing for every address it is given'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the form and what was typed after it is acknowledged'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for an address and never for a password'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing twice, however fast the form is submitted'
  - test "$(grep -oF 'fetch' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF '<input' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 1
  - test "$(grep -oF 'type="password"' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0
  - grep -qF 'from "./recovery-text"' web-client/src/account/ForgotPasswordForm.tsx
  - grep -qF 'from "./account-text"' web-client/src/account/ForgotPasswordForm.tsx
  - test "$(grep -oiE 'Forgot your|Send a link|on its way|Try again|did not go through' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && npm run check
---

## Goal

`ForgotPasswordForm` takes one address, sends it once exactly as it was typed, and renders one
sentence per outcome with the form and what was typed still on screen — the same sentence for every
address, and no password field anywhere.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/ForgotPasswordForm.tsx` | create |
| `web-client/src/account/ForgotPasswordForm.test.tsx` | create |

Read, and do not edit:

- `web-client/src/account/RecoveryEmailForm.tsx` — the merged one-call form this is shaped after: a
  controlled input, `<label htmlFor>`, an in-flight `useRef` guard, an outcome held in state and
  rendered through `role="status"`, and `ADR-0056`'s *keep what was typed*. Copy its shape, not its
  two fields.
- `web-client/src/account/forgot-password.ts` — `ForgotPasswordOutcome`'s **two** kinds, `accepted`
  and `failed`. Switch over them; never re-derive them from a status, and never add a third.
- `web-client/src/account/recovery-text.ts` — `FORGOT_PASSWORD_LABEL`, `FORGOT_PASSWORD_SUBMIT`,
  `FORGOT_PASSWORD_ACKNOWLEDGED`, `FORGOT_PASSWORD_FAILED` and `ADDRESS_LABEL`.
- `web-client/src/account/account-text.ts` — `CANCEL`, imported and never re-authored (`ADR-0087` §1).
- `web-client/src/account/ResetScreen.test.tsx` lines 130–155 — the merged idiom for driving a submit
  guard: `container.querySelector("form")`, a `throw new Error` when it is `null`, then
  `fireEvent.submit(form)`. Test 7 below needs exactly this.
- [`ADR-0087`](../../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) §5
  (what a player sees in each of the four states) and §6 (an address the product does not hold gets
  exactly what everybody else gets).

## Scope

- **Props: one call and one way back, and nothing else.**

  ```tsx
  export function ForgotPasswordForm(props: {
    readonly forgotPassword: (address: string) => Promise<ForgotPasswordOutcome>;
    readonly onCancel: () => void;
  }): ReactElement;
  ```

  The component knows nothing about navigation, storage or `fetch` — `ADR-0060` §4, the shape every
  screen in this client already has. `onCancel` is called and nothing else; what it does is
  `TASK-041723`'s.
- **`FORGOT_PASSWORD_LABEL` is this component's `<h2>` heading.** `ADR-0087` §2: the door's words
  become the heading of what the door opened, from the **one** literal. The door itself is not here.
- **One controlled input**, labelled `ADDRESS_LABEL` through a `<label htmlFor>` with a matching
  `id`, `type="text"` and **not** `type="email"` — `ADR-0078` §Decision, the same refusal
  `RecoveryEmailForm` carries: the browser's own idea of an address is stricter than this product's,
  and a refusal the player cannot see is the one refusal that costs an account.
- **The address is sent exactly as typed.** No `trim`, no `toLowerCase`, no normalisation of any
  kind: `ADR-0078` puts that on the server, and a client that edits what the player typed changes
  what they can see and correct.
- **One submit reading `FORGOT_PASSWORD_SUBMIT`, disabled while a request is in flight** and live
  again once it settles — `ADR-0087` §5's *the submit stays live*, so a player who sees their typo
  can ask again. A `useRef` guard, not the disabled attribute, is what actually stops a second
  request: a disabled control never dispatches its click, so the attribute is a claim about the DOM.
- **One `CANCEL` control**, `type="button"`, calling `onCancel` and **sending nothing**.
- **Two outcomes, two sentences, rendered in a `role="status"` region**: `accepted` →
  `FORGOT_PASSWORD_ACKNOWLEDGED`, `failed` → `FORGOT_PASSWORD_FAILED`. Rendered **with** the form,
  never instead of it, and the address stays in the field for both (`ADR-0087` §5, `ADR-0056`).
- **No branch of any kind on the address**, its length, its shape or its contents. `ADR-0087` §6:
  this flow has no unknown-address case and never will while that section holds.

## Out of scope

- **Placing this on any screen, and holding the open/closed state.** `TASK-041723`.
- **A password field, or `CURRENT_PASSWORD_LABEL`.** `ADR-0087` §5: *"no password field anywhere in
  this flow"*. A `verify:` line pins `type="password"` at zero and `<input` at one.
- **Validating or normalising the address in the client.** `ADR-0078` puts the rule on the server and
  has it refuse almost nothing.
- **Any player-facing string literal.** Every one comes from `recovery-text.ts` or `account-text.ts`,
  and a `verify:` line refuses five phrases lifted from those modules' own values, in code or in a
  comment.
- **Retrying, counting attempts, or timing anything.** `ADR-0079` budgets the endpoint at ten a
  minute behind an answer that never changes, and an over-budget attempt still spends one.
- **Clearing the field on success.** `RecoveryEmailForm` clears its *password* on `accepted`; there
  is no secret in this form to clear, and `ADR-0087` §5 requires what was typed to stay.

## Tests

`web-client/src/account/ForgotPasswordForm.test.tsx`, new, in `RecoveryEmailForm.test.tsx`'s idiom.
**Seven tests.** Every player-facing string is queried through the **constant**, never a literal.

```ts
const ADDRESS = " Zqx-Address-Zqx. ";
```

**That fixture is chosen, not decorative.** It has a leading space, a trailing space, a trailing dot
and capital letters, so it is a fixed point of no transformation this component might apply.
`TASK-041713`'s `"zqx-address-zqx"` was already trimmed and lowercase, and a `.trim().toLowerCase()`
left all seven of its tests green.

| Test | Proves |
| --- | --- |
| `sends what was typed, once, exactly as it was typed` | The heading is found first, by `getByRole("heading", { name: FORGOT_PASSWORD_LABEL })`. Type `ADDRESS`, submit the form. The spy was called **once** and with exactly `ADDRESS` — string-equal, so a `trim` or a `toLowerCase` reddens here and nowhere else |
| `says the same thing for every address it is given` | Three separate renders answering `accepted`, driving three **different** addresses. The rendered `textContent` of `getByRole("status")` is `FORGOT_PASSWORD_ACKNOWLEDGED` in all three **and** the three texts are equal to each other. `ADR-0087` §6 as an equality, not as three presence checks — three presences pass for a component that renders one hint and two acknowledgements |
| `keeps the form and what was typed after it is acknowledged` | One render answering `accepted`. After the acknowledgement arrives: the address input's DOM `value` is still exactly `ADDRESS`; the submit control is present and `disabled` is `false`; the field and the `CANCEL` control are both still there. `ADR-0087` §5's *with the form, not instead of it* |
| `says the request did not go through, and keeps the form and what was typed` | One render answering `failed`. `FORGOT_PASSWORD_FAILED` is on screen, `queryByText(FORGOT_PASSWORD_ACKNOWLEDGED)` is `null`, the input's DOM `value` is still `ADDRESS` and the submit is live. Both arms in one test, because a component rendering one sentence for both outcomes passes either presence check alone |
| `asks for an address and never for a password` | At mount, and again after an `accepted`: `getByLabelText(ADDRESS_LABEL)` finds the one field (the throwing query **is** the presence assertion), `container.querySelectorAll('input')` has length **1**, `container.querySelector('input[type=password]')` is `null`, and `queryByLabelText(PASSWORD_LABEL)` and `queryByLabelText(CURRENT_PASSWORD_LABEL)` are both `null`. `ADR-0087` §5's *no password field anywhere in this flow*, asserted in both states |
| `offers the way back, and sends nothing when it is taken` | The `CANCEL` control is present; click it. The `onCancel` spy was called **once** and the `forgotPassword` spy **zero** times. The way back asks the server for nothing |
| `sends nothing twice, however fast the form is submitted` | With a promise that never settles, `fireEvent.submit(form)` **three times inside one `act`**, driving the form and not the button — a disabled control never dispatches its click, so a click-driven version of this test measures jsdom rather than the guard (`TASK-041718` found exactly this). The spy was called **once**. The count is the assertion |

**Presence before absence, everywhere.** A test whose only assertions are absences passes over a
component that renders nothing.

**`@testing-library/jest-dom` is not a dependency here** — `toBeInTheDocument()` throws
`Invalid Chai property`. Presence goes through a throwing query (`getBy*`/`findBy*`), where the throw
is the assertion; absence goes through `queryBy*` paired with `.toBeNull()`. Never write
`expect(queryByText(x)).toBeDefined()`: it is **always true**, `null` included.

**No `try` anywhere in the added code, and no `expect()` inside one.** The `if (form === null) throw`
guard `ResetScreen.test.tsx` uses is not a `try` and is the idiom to copy. No test sleeps on a real
clock: every settled outcome is awaited through `findByText` or an `act`-wrapped resolution.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed, once, exactly as it was typed'`
      — passes, asserting the call count **and** string equality against a fixture with a leading
      space, a trailing dot and capitals
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing for every address it is given'`
      — passes, three renders, three different addresses, the three status texts asserted **equal to
      each other**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the form and what was typed after it is acknowledged'`
      — passes, with the input value read off the DOM and the submit asserted not disabled
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for an address and never for a password'`
      — passes, in both states, with the input count asserted at one
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing twice, however fast the form is submitted'`
      — passes, driven at the form element, with the call count asserted at one
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/ForgotPasswordForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seven**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'fetch' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0`
      — this component imports no transport module and calls nothing over the network; it is handed a
      function. Reads the whole file, comments included
- [ ] `test "$(grep -oF '<input' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 1`
      and `test "$(grep -oF 'type="password"' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0`
      — **hand-counted against the shape `## Scope` prescribes**: one `<input` opening tag, for the
      address field, and no password input. Neither count reads a closing tag, because an `<input>`
      has none. Two narrow counts rather than one over `input`, which would also match `type` and the
      `id` string
- [ ] `grep -qF 'from "./recovery-text"' web-client/src/account/ForgotPasswordForm.tsx` and
      `grep -qF 'from "./account-text"' web-client/src/account/ForgotPasswordForm.tsx` — the four
      sentences come from the copy module and `CANCEL` comes from `account-text.ts`; neither is
      re-authored here (`ADR-0087` §1)
- [ ] `test "$(grep -oiE 'Forgot your|Send a link|on its way|Try again|did not go through' web-client/src/account/ForgotPasswordForm.tsx | wc -l | tr -d ' ')" = 0`
      — **no fragment of a player-facing sentence appears in this file**, in code or in a comment. The
      needles are phrases from the two copy modules' own values, so a copied sentence fails whether it
      is rendered or quoted in prose. Note the component's own name contains `ForgotPassword` and does
      **not** match `Forgot your`, which is why the needle carries the space and the second word
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      — the copy module `TASK-041721` landed is untouched and still at five
- [ ] `cd web-client && npm run check` exits 0. Run it **bare**, reading `$?` directly: piping it into
      `tail` makes `$?` the pipe's status and has already shipped a false green on `TASK-041714`. The
      whole-suite total is deliberately not pinned — this ticket may be dispatched in a batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget. **A prediction that fails is the finding**: report it as
measured and do not adjust it to match the ticket.

1. **`trim()` the address** before calling `forgotPassword`. Predict: `sends what was typed, once,
   exactly as it was typed` reddens on the argument, **and** `keeps the form and what was typed after
   it is acknowledged` stays green, because the field is controlled by the untrimmed state. Record
   both. Then `toLowerCase()` it and predict the same. **The fixture is what makes either visible** —
   with `"zqx-address-zqx"` neither mutation reddens anything, which is the defect this fixture was
   chosen to close.
2. **Render `FORGOT_PASSWORD_FAILED` for `accepted`.** Predict: `says the same thing for every
   address it is given` reddens, and `says the request did not go through…` stays green on its
   presence half and reddens on its absence half. Record which halves moved.
3. **Branch on the address**: render a second sentence when its length is odd. Predict: `says the
   same thing for every address it is given` reddens on the **equality**, not on the presence. A
   three-presence version of that test would not see it, and this is the mutation `ADR-0087` §6
   exists to forbid.
4. **Clear the address field on `accepted`.** Predict: `keeps the form and what was typed after it is
   acknowledged` reddens on the DOM value. Then **unmount the form** on `accepted` instead: predict
   the same test reddens on the field or the submit being gone. Two different mutations, one test —
   confirm it catches both.
5. **Drop the `useRef` guard**, leaving only `disabled`. Predict: `sends nothing twice, however fast
   the form is submitted` reddens on the **call count**, because the submit is driven at the form and
   `disabled` does not stop a dispatched submit event. If it stays green, the test is clicking the
   button rather than submitting the form — fix the test, and say so on landing.
6. **Add a password field** labelled `CURRENT_PASSWORD_LABEL`. Predict: `asks for an address and
   never for a password` reddens on the input count **and** on the `queryByLabelText` absence, and
   the `type="password"` `verify:` gate exits non-zero. Three signals; record all three.
7. **Vacuity check.** Return `null` from the whole component. Predict: every presence assertion
   reddens and every absence stays green. Confirm each of the seven tests carries a presence half —
   an all-absence test passes over an empty screen.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Every mechanism this ticket prescribes was measured on `develop` at `31894cee` before it was
written**, against a throwaway component of exactly this shape, since reverted:
`getByRole("heading", { name: FORGOT_PASSWORD_LABEL })` finds the `<h2>` including its question mark;
`getByRole("status").textContent` is the acknowledgement; the fixture `" Zqx-Address-Zqx. "` arrives
at the spy byte-identical and is still the input's DOM `value` afterwards; and three
`fireEvent.submit(form)` calls inside one `act` reach the handler once. The whole client suite,
`tsc`, ESLint and Prettier were all green with the component present.

**`ForgotPasswordOutcome`'s occurrence count is deliberately not gated.** `TASK-041713` could pin
`AttachRecoveryOutcome` at two only because `RecoveryEmailForm` declares its own local union for the
five kinds. Here, two equally correct shapes — a local `"accepted" | "failed"` alias, or
`ForgotPasswordOutcome["kind"]` used inline — score **2** and **4** on the same file, measured. A
count that depends on which correct shape was chosen is a gate no correct implementation reliably
satisfies, and this story has already paid for three of those.
