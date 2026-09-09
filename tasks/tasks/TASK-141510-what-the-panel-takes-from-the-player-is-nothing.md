---
schema: 2
id: TASK-141510
title: What the panel takes from the player is nothing
type: task
status: done
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, rematch, notice, test]
depends_on: [TASK-141509]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.screens.test.tsx 2>&1 | grep -qE '^ *Tests +8 passed \(8\)$'
  - git diff --quiet develop -- web-client/src/result/RematchNotice.test.tsx
  - git diff --quiet develop -- web-client/src/App.test.tsx
  - git diff --quiet develop -- web-client/src/App.tsx
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/result/RematchNotice.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - git diff --quiet develop -- web-client/src/account/SignInForm.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0123` §2 is a list of things the surface must not do, and `ADR-0138` §Consequences names the
honest gap: *"Nothing proves the absence of a scrim except a reader."* This ticket writes the four
proofs that **are** mechanical, and says plainly which clause each one does and does not reach.

No source is written. If a test here fails, the failure is the finding.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.screens.test.tsx` | modify |

Read [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§2 and 5,
[`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§1, 5, 6 and 8, `web-client/src/account/SignInForm.tsx` (the form the `Enter` proof uses), and
`web-client/src/account/account-text.ts`. **No file outside the table above is changed** — five
`verify:` lines diff the components against `develop`.

## Scope

Four tests appended to the file, taking it from **4** to **8**. The fixture builder, the
`vi.mock("./main")` factory and the provider stack are reused.

- **`Enter` is proved structurally, not by a keypress.** Measured at `f20d07ed`: **jsdom does not
  perform implicit form submission** — `keyDown`, `keyPress` and `keyUp` with `key: "Enter"` on a
  field inside a `<form onSubmit>` fire the handler **0 times**. So a `keyDown`-based test would
  pass against a panel sitting *inside* the form, which is the defect it exists to catch. The proof
  is instead: the panel's `[role="status"]` root has `closest("form") === null`, plus a real
  `fireEvent.submit` on the sign-in form with a positive control that the form's own submit ran.
  `ADR-0138` §1's claim is about **position**, and position is what is asserted.
- **`ADR-0138` §8's *"the same on the front door's room-code field"* is not written, because it is
  vacuous.** The front door is `shown === "first"`, where the panel never draws — there is no panel
  over that form to press `Enter` into. The proof that carries it is `TASK-141508`'s *one fact never
  has two live surfaces*. Say so in a comment in the test file, so the next reader does not add a
  test that passes for no reason.
- **What is still only a reader's, stated in a comment beside the tests:** no scrim element, no
  `inert`, no `aria-hidden` on anything the panel does not own, no scroll lock on `<body>`, and
  nothing painting over anything outside its own box. `TASK-141504`'s source gates cover the
  attributes; a scrim added later with `pointer-events: none` would pass every assertion here, and
  `ADR-0138` says so about itself.

## Out of scope

- **Repairing anything.** Five `git diff --quiet` gates make that a failure rather than a choice.
- **A browser-driven proof.** `ADR-0088` §1 admits no browser driver into `web-client/package.json`
  and this ticket adds none.

## Tests

| Test | Proves |
| --- | --- |
| `the panel is a descendant of no form` | with the panel up on `#/sign-in`, the `[role="status"]` root's `closest("form")` is `null`. The positive control is in the same test: `getByLabelText(PASSWORD_LABEL).closest("form")` is **not** null, so the assertion is about the panel's position rather than about there being no form on the screen |
| `submitting the form the player was already in sends no OfferRematch` | on `#/sign-in`, type into the password field and `fireEvent.submit` its form: the `send` spy has **0** calls, and the account provider's `signIn` spy has **1** — the second half is what proves the submit really happened |
| `the offer arriving moves no focus and disables nothing` | on `#/sign-in`, focus the password field **before** the offer arrives; apply `RematchOffered` in its own `act()`; `document.activeElement` is still that field, the field is still enabled, and typing into it still changes its value. Pre-focusing is what stops this being vacuous — against a default `activeElement` of `<body>` it would pass on any component |
| `no amount of time retires it` | with `vi.useFakeTimers()`, the offer standing on `#/account`, advance the clock by an hour inside `act()`: the panel is exactly where it was. Restore real timers in the same test |

## What would still pass if this were built wrong

`the panel is a descendant of no form` without its positive control passes on a screen with no form
at all, and on a render where the panel is absent — `closest` on nothing throws, but a
`queryByRole("status")?.closest(...)` written defensively would return `undefined` and satisfy a
loose assertion. Query the root with `getByRole("status")`, which throws when it is missing.

`submitting the form…` asserts **two** counts. `send` at 0 alone passes against a submit that never
fired, which — given jsdom's missing implicit submission — is the failure mode actually in play
here. `signIn` at 1 is what makes the zero mean something.

`the offer arriving moves no focus…` is the one that is vacuous by default. `document.activeElement`
is `<body>` in a fresh jsdom document, and a component that stole focus and gave it back, or one
that never rendered, both leave it there. Focusing a real control first, and asserting that same
element afterwards, is the difference between a proof and a decoration. The *still accepts typing*
half is separate from *still enabled* on purpose: `disabled` is one way to take a control away and
an overlay intercepting pointer and key events is another.

`no amount of time retires it` is green against a component with no timer **and** against one whose
timer has not been reached. There is no way to tell those apart from the outside, which is why
`TASK-141504`'s source gate holds `setTimeout` and `setInterval` at **0** and why this test is the
observable half of a claim whose real proof is structural.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.screens.test.tsx` reports **8 passed (8)**, the four
      new ones being the four named above
- [ ] The test file carries a comment recording that the front-door `Enter` case is vacuous by the
      gate, and a comment recording which of `ADR-0123` §2's prohibitions remain a reader's
- [ ] `src/result/RematchNotice.test.tsx` and `src/App.test.tsx` are **byte-identical to
      `develop`** — `git diff --quiet` on each. The ticket pinned them at 16 and 36 absolute
      tests; both were stale on its own base `11c6ac78` (17 and 38), and both are files this
      ticket does not modify. The five diff guards it already carried covered the **production**
      files — `App.tsx`, `RematchNotice.tsx` — and not their test counterparts, which is the gap
      `TASK-000108` names
- [ ] `git diff --quiet develop` is clean for `App.tsx`, `Lobby.tsx`, `RematchNotice.tsx`,
      `RematchControl.tsx` and `SignInForm.tsx`
- [ ] **Shown red, then reverted, twice.** (a) Move `<RematchNotice />` inside `SignInForm`'s
      `<form>` — reachable by temporarily rendering it there — and confirm `the panel is a
      descendant of no form` fails. (b) Add `autoFocus` to the panel's accept button and confirm
      `the offer arriving moves no focus and disables nothing` fails. Both mutations were run,
      observed red by name, and reverted
- [ ] `npm run check` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
