---
schema: 2
id: TASK-041713
title: The form that attaches an address, and says why it asks for the password
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, recovery, ui]
depends_on: [TASK-041711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RecoveryEmailForm.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RecoveryEmailForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed, once, and says why the password is asked for'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing for every reason the server accepted it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps what was typed when the server refuses, and clears the password on success'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never says recovery is on, because a link has only been sent'
  - test "$(grep -oF 'AttachRecoveryOutcome' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'fetch' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 0
  - grep -qF 'from "./recovery-text"' web-client/src/account/RecoveryEmailForm.tsx
  - test "$(grep -oiE 'Recovery is (on|off)|on its way|Try again|does not match|address mail can' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`RecoveryEmailForm` takes an address and the player's current password, sends them once, and renders
one sentence per outcome — the same acknowledgement whatever the server's reason for `202`, and never
a claim that recovery is now on.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/RecoveryEmailForm.tsx` | create |
| `web-client/src/account/RecoveryEmailForm.test.tsx` | create |

Read, and do not edit:

- `web-client/src/account/SignUpForm.tsx` and `SignUpForm.test.tsx` — the merged two-field form this
  is shaped after: controlled inputs, `<label htmlFor>`, one submit, an outcome held in state, and
  `ADR-0056`'s *keep what was typed*. Twelve tests, and the idiom for driving one.
- `web-client/src/account/attach-recovery-email.ts` — `AttachRecoveryOutcome`'s five kinds. Switch
  over them; do not re-derive them from a status.
- `web-client/src/account/recovery-text.ts` — every string this component renders.
- `web-client/src/account/account-text.ts` — `PASSWORD_LABEL` is **not** reused here;
  `CURRENT_PASSWORD_LABEL` is its own constant because the two fields mean different things on one
  screen.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §3 and §5, and
  [`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md)
  §Consequences — *"a `202` may not be rendered as recovery being on"*.

## Scope

- **Props: one call and nothing else.**

  ```tsx
  export function RecoveryEmailForm(props: {
    readonly attach: (
      address: string,
      currentPassword: string,
    ) => Promise<AttachRecoveryOutcome>;
  }): ReactElement;
  ```

  The component knows nothing about navigation, storage or `fetch` — `ADR-0060` §4, the shape every
  screen in this client already has.
- **Two controlled inputs**, `ADDRESS_LABEL` and `CURRENT_PASSWORD_LABEL`, each with a `<label
  htmlFor>` and a matching `id`; the password input is `type="password"` and the address input is
  `type="text"`, **not** `type="email"` — `ADR-0078` §Decision says the browser's own idea of an
  address is stricter than this product's, and a refusal the player cannot see is the one refusal
  that costs an account.
- **`ATTACH_WHY` is rendered whenever the form is**, not behind a control and not after a failure.
  `STORY-0417`: the screen says why it asks, because that is what stops an unattended browser
  becoming permanent ownership.
- **One submit, `ATTACH_LABEL`, disabled while a request is in flight**, so a double click cannot
  spend two of `ADR-0079`'s five attempts a minute.
- **One sentence per outcome**, from `recovery-text.ts`: `accepted` → `ATTACH_ACKNOWLEDGED`;
  `address-refused` → `ATTACH_ADDRESS_REFUSED`; `password-refused` → `ATTACH_PASSWORD_WRONG`;
  `no-profile` and `failed` → `ATTACH_FAILED`. **`no-profile` and `failed` share a sentence
  deliberately**: a browser the server does not recognise and a request that did not arrive are the
  same thing to the player, and neither is their fault.
- **On `accepted` the password field is cleared and the address field is not.** The address is what
  the player will look at to check they typed it right; the password is a secret with no reason to
  stay in the DOM.
- **On every refusal both fields keep what was typed** — `ADR-0056`'s rule, applied to the second
  two-field form in this client.

## Out of scope

- **Placing this on any screen.** `TASK-041714`.
- **Any string literal a player reads.** Every one comes from `recovery-text.ts`, and a `verify:` line
  refuses five phrases lifted from that module's own values, in code or in a comment.
- **Validating the address in the client.** `ADR-0078` puts the rule on the server and has it refuse
  almost nothing; a client-side check would refuse strings the server accepts.
- **Saying recovery is now on.** `ADR-0031` §3 leaves `hasRecoveryEmail` false until the link is
  followed, and a `verify:` grep in `TASK-041712` keeps that sentence in one place.
- **Detaching.** Not ticketed.
- **Re-reading the profile after a `202`.** Nothing changed on the profile, by construction.

## Tests

`web-client/src/account/RecoveryEmailForm.test.tsx`, new. **Seven tests**, in `SignUpForm.test.tsx`'s
idiom. Every player-facing string is queried through the **constant**, never a literal.

```ts
const ADDRESS = "zqx-address-zqx";
const CURRENT = "zqx-current-zqx";
```

| Test | Proves |
| --- | --- |
| `sends what was typed, once, and says why the password is asked for` | Type both fields, submit. The `attach` spy was called **once**, with exactly `(ADDRESS, CURRENT)`. `ATTACH_WHY` is on screen **before** any submit, asserted at mount. Presence before absence for everything that follows |
| `says the same thing for every reason the server accepted it` | Three separate renders, each answering `accepted`, driving three different addresses — the acknowledgement rendered is `ATTACH_ACKNOWLEDGED` in all three and the three rendered `textContent`s of the outcome region are **equal to each other**. The story's *"renders the same acknowledgement whatever the server's reason for `202`"*, asserted as an equality rather than as three separate presence checks |
| `renders one sentence per refusal, and each is its own` | Three renders answering `address-refused`, `password-refused` and `failed`; each shows its own sentence and **not** either of the other two. Nine assertions, and the absences are what stop two arms of a `switch` collapsing unnoticed |
| `treats a browser the server does not know as a failure it does not blame the player for` | One render answering `no-profile`: the sentence is `ATTACH_FAILED`, byte-identical to the `failed` case rendered in the same test. Two outcomes, one sentence, asserted as an equality — a deliberate collapse, so a later reader can see it was chosen |
| `keeps what was typed when the server refuses, and clears the password on success` | Two renders. On `password-refused`, both input values are still `ADDRESS` and `CURRENT` — read off the **DOM value**, since React reflects a controlled input's value there. On `accepted`, the address input still holds `ADDRESS` and the password input is `""`. Both directions in one test, because clearing everything and clearing nothing both pass a one-sided assertion |
| `never says recovery is on, because a link has only been sent` | After an `accepted`, the rendered `textContent` contains neither `RECOVERY_ON` nor the word `on` as a standalone claim — assert it does **not** contain `RECOVERY_ON`, and that it **does** contain `ATTACH_ACKNOWLEDGED`. `ADR-0078` §Consequences' rule, in the one place a client would be tempted to break it |
| `sends nothing twice, however fast the control is pressed` | With a promise that does not settle, click submit three times: the spy was called **once**, and the control is disabled. The count is the assertion; a disabled attribute alone is a claim about the DOM, not about the request |

**No `try` anywhere in the added code, and no `expect()` inside one.** Every asynchronous outcome is
awaited through `findByText` or an `act`-wrapped resolution — no test sleeps on a real clock.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends what was typed, once, and says why the password is asked for'`
      — passes, asserting the call count, the two arguments and `ATTACH_WHY` at mount
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing for every reason the server accepted it'`
      — passes, three renders, with the three outcome texts asserted **equal to each other**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps what was typed when the server refuses, and clears the password on success'`
      — passes, both directions, reading input values off the DOM
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never says recovery is on, because a link has only been sent'`
      — passes, asserting the absence of `RECOVERY_ON` and the presence of `ATTACH_ACKNOWLEDGED`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RecoveryEmailForm.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly seven**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'AttachRecoveryOutcome' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 2`
      — the `import type` and the prop's type annotation
- [ ] `test "$(grep -oF 'fetch' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 0`
      — this component imports no transport module and calls nothing over the network; it is handed a
      function. Reads the whole file, comments included
- [ ] `grep -qF 'from "./recovery-text"' web-client/src/account/RecoveryEmailForm.tsx` and
      `test "$(grep -oiE 'Recovery is (on|off)|on its way|Try again|does not match|address mail can' web-client/src/account/RecoveryEmailForm.tsx | wc -l | tr -d ' ')" = 0`
      — every sentence comes from the copy module and **no fragment of one appears in this file**, in
      code or in a comment. The needles are phrases from `recovery-text.ts`'s own values, so a copied
      sentence fails whether it is rendered or quoted in prose. `className` strings are untouched by
      this gate, which is why it names phrases rather than forbidding spaces
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041712`, `TASK-041716`, `TASK-041718` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Swap the two arguments** in the `attach` call. Predict: `sends what was typed, once, and says
   why the password is asked for` reddens on the argument assertion. The two fixture literals differ
   for exactly this reason — with one shared value the swap is invisible.
2. **Render a different sentence for one of the three `202` drives** — branch on the address length.
   Predict: `says the same thing for every reason the server accepted it` reddens on its equality.
   A three-presence assertion would not see it, which is why that test compares texts.
3. **Collapse `password-refused` into `failed`.** Predict: `renders one sentence per refusal, and
   each is its own` reddens on both the presence and the absence for that arm. Record both.
4. **Clear both fields on `accepted`.** Predict: `keeps what was typed when the server refuses, and
   clears the password on success` reddens on the address half. Then **clear neither**: predict it
   reddens on the password half. Run both — one-sided assertions pass for both mutations.
5. **Render `RECOVERY_ON` after an `accepted`.** Predict: `never says recovery is on…` reddens alone.
   This is the one mutation that tells a player their account is protected when it is not.
6. **Drop the disabled guard** on the submit control. Predict: `sends nothing twice…` reddens on the
   **call count**. Note whether the disabled-attribute assertion alone would have caught it; if it
   would not, that is why the count is there.
7. **Vacuity check on the refusal test.** Make the component render **nothing** for every outcome.
   Predict: the presence halves redden and the absence halves stay green. Confirm that, and confirm
   the test as written has both — an all-absence test passes over an empty screen.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`type="text"` and not `type="email"` is a deliberate refusal, and the reason is `ADR-0078`.** The
browser's built-in validation rejects strings this product accepts — `a@b` is the shortest string
that passes the server's rule — and it does so before any request is sent, with a message no test in
this repository can see. The one refusal this endpoint makes is the server's, and it arrives as a
sentence the player can read.

**Reading input values off the DOM is measured behaviour, not a stylistic choice.** React reflects a
controlled input's `value` into the DOM attribute, which is why a `TASK-041232` proposal to compare
`innerHTML` across two attempts turned out to be **false on the honest component**. Assert
`(input as HTMLInputElement).value`, not the markup.
