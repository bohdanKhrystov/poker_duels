---
schema: 2
id: TASK-041211
title: The words the account screen says, including the refusal that is about nobody
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, copy]
depends_on: [TASK-041210]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-text.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says one thing about the device route in each of its two states'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the same thing about a wrong password and an unknown handle'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses a throttled sign-up without a digit, a mechanism or an accusation'
  - cd web-client && npm run check
---

## Goal

Every word the account screen says lives in one file, the two device-route states are one function
with one `if`, and the throttled refusal is gated against `ADR-0056` §2's five prohibitions rather
than reviewed against them.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-text.ts` | create |
| `web-client/src/account/account-text.test.ts` | create |

Read, and do not edit:
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §3;
[`ADR-0056`](../../docs/adr/ADR-0056-a-throttled-sign-up-says-so-and-keeps-what-was-typed.md) §2;
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md);
[`ADR-0048`](../../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) §7;
`web-client/src/history/history-text.ts` (the shape and the register to follow).

## Scope

- Exactly these exports, and no others:

  ```ts
  export const ACCOUNT_HEADING = "Account";
  export const DEVICE_ROUTE_LIVE = "This device signs in to this account.";
  export const DEVICE_ROUTE_REVOKED = "This device no longer signs in to this account.";
  export const PASSWORD_ROUTE_LIVE = "Your password signs in to this account.";
  export const REVOKE_LABEL = "Stop this device signing in";
  export const REVOKE_PERMANENT = "This device will never sign in to this account again. This cannot be undone.";
  export const REVOKE_OTHER_SESSIONS = "You will be signed out on every other device. You stay signed in here.";
  export const REVOKE_ONLY_WAY_BACK = "Your password becomes the only way back to this account.";
  export const SIGN_OUT_LABEL = "Sign out";
  export const SIGN_OUT_WARNING = "Signing out leaves any duel room this browser is in, and a duel left this way can be lost. This browser goes back to the profile it had before.";
  export const SIGN_UP_LABEL = "Give this profile a password";
  export const HANDLE_LABEL = "Handle";
  export const PASSWORD_LABEL = "Password";
  export const SIGNED_UP = "This profile now has a password. Sign in with it on any other browser.";
  export const HANDLE_REFUSED = "A handle is 3 to 32 of a–z, 0–9, dot, dash or underscore, and starts with a letter or a number.";
  export const HANDLE_UNAVAILABLE = "That handle is taken, or this profile already has a password.";
  export const PASSWORD_REFUSED = "A password is 8 to 128 characters.";
  export const NO_PROFILE_YET = "This browser has no profile yet. Reload the page and try again.";
  export const SIGN_UP_FAILED = "That did not go through. Try again.";
  export const SIGN_UP_THROTTLED = "…";  // §2's three facts, and none of its five prohibitions
  export const SIGN_IN_LABEL = "Sign in";
  export const SIGN_IN_REFUSED = "That handle and password do not match an account.";
  export const CANCEL = "Cancel";
  export function deviceRouteLine(live: boolean): string;
  ```

- `deviceRouteLine(true)` is `DEVICE_ROUTE_LIVE`, `deviceRouteLine(false)` is `DEVICE_ROUTE_REVOKED`.
  It is a function for `emptyLine`'s reason: these are **two different facts about the world**
  (`ADR-0037`), and a component choosing between them inline would be a second place able to get it
  wrong.
- The strings are **golden** — written as literals in this file and nowhere else, asserted character
  for character, full stops and en dashes included.
- `SIGN_UP_THROTTLED` is the coder's to write, against `ADR-0056` §2 and nothing else. It **must**
  state the three facts: sign-up cannot be completed right now and the reason is the connection, not
  the player; nothing typed was refused and no account was created; nothing is lost, the player is
  the same player with the same coins and duels, and they can sign up later. It **must not** contain
  a digit, a duration, a count, a verdict on either field, any claim about the handle's availability,
  any accusation, or any word naming a mechanism or a fault. The gates below are objective; the
  sentence is not.
- `SIGN_UP_THROTTLED` and `SIGN_UP_FAILED` are **different sentences**. `ADR-0056` §1: a deliberate
  refusal must be distinguishable from a broken product, and folding them together is the named
  temptation.
- `SIGN_IN_REFUSED` is the **one** sentence for an unknown handle and for a wrong password. It names
  neither field, so it cannot become an enumeration oracle in words after the server spent a design
  making the statuses identical.
- The three `REVOKE_*` facts are `ADR-0050` §3's, transcribed. `REVOKE_LABEL` is that ADR's string
  verbatim, and the file **never says *revoke*** to a player: §3 puts that word in the schema, the
  register and `ADR-0049`'s title.
- KDoc on `deviceRouteLine` naming `ADR-0037` and why one function decides.

## Out of scope

- **Any sentence about a recovery email.** `ADR-0050` §3's third fact wants one for a player with
  none attached, and that needs `hasRecoveryEmail`, which this client does not parse and which
  `ADR-0050` §4 says the screen does not read. `REVOKE_ONLY_WAY_BACK` is true under both states,
  which is why it is unconditional. `STORY-0417` owns the branch and the field. **A refusal, not an
  omission** — a criterion greps for the words.
- **The sign-in screen's heading.** The product says *account*, *sign in* and *signed out* to a
  player in `ADR-0050` §3's merged text, so those words are not coined here — but it says no **name**
  for a screen whose whole subject is reaching an account from a browser that does not hold it. That
  word is `DEC-077`, the product owner's, and `TASK-041226` adds the constant and the slug together
  once it is answered. `SIGN_IN_LABEL` and `SIGN_IN_REFUSED` ship here because a control's verb and a
  refusal's sentence are not a screen's name.
- Any colour, weight, spacing or type. `EPIC-06` owns the visual language and may letter-fit every
  string here; this file authors sentences.
- `No name`. `nameOrNone` owns it (`ADR-0058` §2) and a second copy is the drift that ADR exists to
  prevent.

## Tests

`web-client/src/account/account-text.test.ts`, describe block `"the account screen's words"`.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` | Every constant asserted against its literal. A change to what a player reads is a change to this test rather than a silent one |
| `says one thing about the device route in each of its two states` | `deviceRouteLine(true)` and `deviceRouteLine(false)` in **one** test: each equal to its constant, and the two asserted **not equal**. Fails against a function that ignores its argument, which two single-input tests could not tell from a constant |
| `says the same thing about a wrong password and an unknown handle` | `SIGN_IN_REFUSED` names neither *handle* alone nor *password* alone as the thing that was wrong: asserted by the sentence not matching `/handle (is|was) (unknown|not)/i` and not matching `/password (is|was) (wrong|incorrect)/i`, and by there being exactly **one** such constant in the module's exports |
| `tells a deliberate refusal from a broken product` | `SIGN_UP_THROTTLED !== SIGN_UP_FAILED`, both non-empty, and `new Set([...])` over the six sign-up outcome sentences has size 6. Fails against a mapping that reuses one sentence for two outcomes |
| `refuses a throttled sign-up without a digit, a mechanism or an accusation` | `SIGN_UP_THROTTLED` matches `/\d/` **zero** times, and contains none of `rate`, `limit`, `throttl`, `budget`, `security`, `error`, `blocked`, `banned`, `suspicious`, `too many`, `try again in`, `minute`, `second`, `hour` — case-insensitively, each asserted so the failure names which word. `ADR-0056` §2's five prohibitions, as far as a string can carry them |
| `never says revoke to a player` | No exported string in the module contains `revoke`, case-insensitively, iterated over every export rather than over a hand-written list. `ADR-0050` §3's last bullet |

Six tests in a new file: `npm run test -- src/account/account-text.test.ts` reports **6**.

## Acceptance criteria

- [ ] `the account screen's words > states every sentence exactly, character for character` passes,
      asserting every exported constant
- [ ] `the account screen's words > says one thing about the device route in each of its two states`
      passes, asserting both inputs and their inequality in one test
- [ ] `the account screen's words > says the same thing about a wrong password and an unknown handle`
      passes
- [ ] `the account screen's words > tells a deliberate refusal from a broken product` passes with a
      set of size 6
- [ ] `the account screen's words > refuses a throttled sign-up without a digit, a mechanism or an
      accusation` passes, with the digit check and **all fourteen** word checks
- [ ] `the account screen's words > never says revoke to a player` passes, iterating the module's
      exports
- [ ] `grep -ci 'recovery\|email' web-client/src/account/account-text.ts` returns `0`
- [ ] `REVOKE_LABEL` is the literal `"Stop this device signing in"`
- [ ] `npm run test -- src/account/account-text.test.ts` reports `Tests  6 passed (6)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Make `deviceRouteLine` return `DEVICE_ROUTE_LIVE` for both inputs.
   **`says one thing about the device route in each of its two states` reddens alone**, on the
   `false` half and on the inequality. A version of that test written as two `it` blocks with one
   input each would still catch the inequality nowhere — write it as one and see. Revert.
2. Set `SIGN_UP_THROTTLED = SIGN_UP_FAILED`.
   **`tells a deliberate refusal from a broken product` reddens** on both the inequality and the set
   size, and `states every sentence exactly` reddens with it. This is `ADR-0056` §1's named default,
   and it is one assignment away at all times. Revert.
3. Add *"Please try again in 15 minutes."* to `SIGN_UP_THROTTLED`.
   **`refuses a throttled sign-up without a digit, a mechanism or an accusation` reddens** on the
   digit check **and** on `minute`, and `states every sentence exactly` reddens with it. Then try
   *"Please try again shortly."* — no digit, no banned word, and it **passes**, which is the honest
   limit of what a string test can gate. Record that: `ADR-0056` §2's *may say it is temporary, may
   not promise when* is a review criterion, and this ticket does not pretend otherwise.
4. Change `SIGN_IN_REFUSED` to *"That handle is unknown."*.
   **`says the same thing about a wrong password and an unknown handle` reddens** on the first
   pattern. Then try *"We could not sign you in."* — it passes, and it is fine. The test refuses the
   oracle, not the register.
5. Rename `REVOKE_LABEL`'s value to *"Revoke this device"*.
   **`never says revoke to a player` reddens**, and `states every sentence exactly` with it. Run it:
   *revoke* is the word every ADR and every schema column uses, so it is the one a coder reaches for.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
