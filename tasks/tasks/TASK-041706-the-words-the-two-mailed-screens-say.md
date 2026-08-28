---
schema: 2
id: TASK-041706
title: The words the two mailed screens say, including what a reset costs
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, account, recovery, copy]
depends_on: [TASK-041705]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a dead link and a refused password apart, in words a player can act on'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'
  - test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'RESET_ENDS_EVERY_SESSION' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 1
  - test "$(grep -oiE 'signed in|sessionToken|token is' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

`recovery-text.ts` gains the words the two **mailed** screens say — the verification screen's four
outcomes, and the reset screen's warning, its two refusals and its confirmation — with the `400` and
the `422` reading as two different, actionable things.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/recovery-text.ts` | modify |
| `web-client/src/account/recovery-text.test.ts` | modify |

Read, and do not edit:

- [`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
  §1 — the two rows it adds to `ADR-0076` §1's table, ***Set a new password*** and ***Finish
  verifying an address***. **The two headings below are those rows and are not coined here.** §6 — a
  stale or spent link renders the screen and the refusal arrives from the server.
- [`ADR-0080`](../../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md)
  §Consequences — *"a `422` no longer proves the link is alive, so `STORY-0417`'s form must be able
  to move from *password refused* to *link expired* without having contradicted itself"*. That
  sentence is why the two refusals below are worded the way they are.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §4 — a reset issues no
  session and returns no token; every `auth_session` row for that player is deleted in the same
  transaction.
- [`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
  §1 and §3 — `SIGN_IN_HEADING` is the destination's one spelling. It is **imported** from
  `account-text.ts` by whichever component names the way onward; no constant here holds those words.
- `docs/protocol.md` *Verify email* and *Reset password* — the statuses these sentences answer.

## Scope

- **Nine more exports**, taking `recovery-text.ts` to twenty:

  | Name | Value |
  | --- | --- |
  | `VERIFY_HEADING` | `"Finish verifying an address"` |
  | `VERIFY_DONE` | `"That address is attached. It can now set a new password for this account."` |
  | `VERIFY_LINK_DEAD` | `"That link has expired or has already been used. Ask for a new one from the account screen."` |
  | `VERIFY_ADDRESS_TAKEN` | `"That address is already attached to another account, so it cannot be attached to this one."` |
  | `VERIFY_NO_LINK` | `"Open the link from your mail to finish this. There is nothing on this screen to fill in."` |
  | `RESET_HEADING` | `"Set a new password"` |
  | `NEW_PASSWORD_LABEL` | `"New password"` |
  | `RESET_ENDS_EVERY_SESSION` | `"Setting a new password ends every session on every browser, including this one. You will sign in again with the new password."` |
  | `RESET_LINK_DEAD` | `"That link has expired or has already been used. Ask for a new one and try again."` |

- **The reset screen's `422` is `PASSWORD_REFUSED`**, imported from `account-text.ts` by the
  component. It already reads *"A password is 8 to 128 characters."*, which is the actionable
  sentence, and `ADR-0060` §2's one-spelling rule says a second constant holding the same characters
  is a second thing to keep in step. Nothing is added here for it.
- **`RESET_LINK_DEAD` and `PASSWORD_REFUSED` must be able to follow each other without contradiction**
  — a player who is told the password is too short, fixes it, and is then told the link is dead has
  been told two true things in order, not two conflicting ones. Neither sentence claims anything about
  the other's subject, which is `ADR-0080` §Consequences' constraint discharged in the copy.
- **`VERIFY_NO_LINK` is not an error.** `ADR-0081` §6: `#/verify` with no second segment is what a
  reload lands on, and it renders the same screen with nothing in hand. The sentence must not say
  *invalid*, *error* or *not found*.
- **KDoc on `RESET_ENDS_EVERY_SESSION`** naming `ADR-0031` §4 — the reset ends every session and
  issues none, so a client that expected one is a client that hangs, and the player is told before
  they act rather than after.

## Out of scope

- **Any word the *forgot password* flow says**, including the sentence the player reads after asking
  for a link. `ADR-0087` §1 fixes its four constants for this same module, and they belong to the
  ticket that ADR unblocked; this ticket takes `recovery-text.ts` to twenty exports and no further.
- **Naming the destination.** `SIGN_IN_HEADING` is `account-text.ts`'s and stays there.
- **A second spelling of the password rule.** `PASSWORD_REFUSED` exists; a `verify:` line pins zero
  occurrences of `token is` so a sentence here cannot start explaining tokens either.
- **Any component, any request.** Later tickets.
- **The four `STORY-0416` mails.** `ADR-0031` §7 and `EPIC-07`; nothing in this client writes mail.

## Tests

`web-client/src/account/recovery-text.test.ts`. **3 merged tests become 4**, and two of the three
merged ones move — this ticket owns both, because its change is what invalidates them.

| Test | What happens |
| --- | --- |
| `states every sentence exactly, character for character` | **moves**: its key-set `toEqual` grows from eleven names to twenty, and it gains one `toBe` per new constant. Nothing already in it changes and no assertion is weakened |
| `names no mailbox, no domain and no other account` | **moves in fixture only**: it loops over every export, so it covers the nine new strings with no edit to its assertions. Re-read it and confirm the loop really does enumerate the module rather than a hand-written list; if it holds a list, extending that list is part of this ticket |
| `says recovery is on and recovery is off from one place` | unchanged |
| `tells a dead link and a refused password apart, in words a player can act on` | **new**. Four properties in one test, each an assertion of its own: `RESET_LINK_DEAD` and `PASSWORD_REFUSED` (imported from `account-text.ts`) are different strings; neither contains the other; `RESET_LINK_DEAD` and `VERIFY_LINK_DEAD` are different strings, because the two screens send a player to different places; and `VERIFY_NO_LINK` contains none of the words `invalid`, `error`, `expired` or `used`, since `ADR-0081` §6 makes an absent token an empty input rather than a failure |

**No `try` anywhere in the added code, and no `expect()` inside one.**

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a dead link and a refused password apart, in words a player can act on'`
      — passes, all four properties
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'`
      — still passes, now over twenty strings
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly four**: `TASK-041705`'s three plus
      this one. Both lines, because a collection error prints a *passing* `Tests` count with no
      failure line at all
- [ ] `test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — still no `@` anywhere, comments included
- [ ] `test "$(grep -oF 'RESET_ENDS_EVERY_SESSION' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 1`
      — declared once. A second occurrence means something in this module referenced it, and this
      module holds no logic
- [ ] `test "$(grep -oiE 'signed in|sessionToken|token is' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — no sentence here promises a session after a reset (`ADR-0031` §4 issues none) and none
      explains the token. Reads the whole file. Note `sign in` with no `ed` is permitted and is what
      `RESET_ENDS_EVERY_SESSION` says
- [ ] `cd web-client && npm run check` exits 0
- [ ] The two merged tests named above pass, having gained entries only; the third is untouched
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Set `RESET_LINK_DEAD` equal to `PASSWORD_REFUSED`'s value.** Predict: `tells a dead link and a
   refused password apart…` reddens, **and** `states every sentence exactly…` reddens too. Record
   both — this is the mutation `ADR-0080` §Consequences is worried about, and two tests should see it.
2. **Set `VERIFY_LINK_DEAD` equal to `RESET_LINK_DEAD`.** Predict: the new test reddens on its third
   property alone. If it stays green, that property is comparing a constant with itself.
3. **Append `" That link is invalid."` to `VERIFY_NO_LINK`.** Predict: the new test's fourth property
   reddens. This is the one that stops an absent token being reported as a failure.
4. **Rename `RESET_HEADING` to `RESET_TITLE`, same value.** Predict: `states every sentence exactly…`
   reddens on the key set and on nothing else.
5. **Vacuity check on the sweep, again**: append `" bob@example.test"` to `VERIFY_DONE`. Predict:
   `names no mailbox, no domain and no other account` reddens. `TASK-041705` ran this on a different
   constant; run it here on a **new** one, because the point is that the loop reaches the nine
   strings this ticket added and not only the eleven it was written against.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Neither heading is coined here, and that is load-bearing.** `ADR-0076` §1 permits only *the
lowercase form of a word the product already says*, and `ADR-0083` shows what it costs to coin one:
a `DEC`, an ADR and two blocked tickets. *Set a new password* and *Finish verifying an address* are
the exact rows `ADR-0081` §1 wrote into `ADR-0076` §1's address table, and both are merged. The
slugs behind them — `reset` and `verify` — were fixed by the same ADR because a **server** writes
them into a mail.

**The one flow whose words are still missing is deliberate.** Asking for a reset has no sentence in
this module and no constant reserved for one here, because what the product calls it was `DEC-081`
and the product owner's. `ADR-0087` answered it — four constants, in this file, in a **later**
ticket — so a placeholder added here would still be an export this ticket's key-set `toEqual` does
not expect.
