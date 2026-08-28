---
schema: 2
id: TASK-041705
title: The words the account screen says about recovery, and never the address
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, account, recovery, copy]
depends_on: [TASK-041701]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says recovery is on and recovery is off from one place'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'
  - test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oiE 'address is|your address|that address is (taken|registered)' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-text.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && npm run check
---

## Goal

`recovery-text.ts` holds, as golden constants, every sentence the **account screen** says about
recovery — recovery on, recovery off, the attach form's labels, why it asks for the password, and
its four outcomes — and none of them contains an address or implies one.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/recovery-text.ts` | create |
| `web-client/src/account/recovery-text.test.ts` | create |

Read, and do not edit:

- `web-client/src/account/account-text.ts` — the module this sits beside, and the source of
  `PASSWORD_REFUSED` and `PASSWORD_LABEL`, which are **not** re-declared here. `deviceRouteLine` is
  the shape `recoveryLine` copies: one function, the only place that branches on the fact.
- `web-client/src/account/account-text.test.ts` — the golden-string idiom this file follows: an
  exact key-set `toEqual`, then one `toBe` per constant.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §3 (an unverified
  address recovers nothing, and a player who never follows the link *"has not opted in; they have
  only intended to"*), §5 (the `202` says nothing) and §6.3 (the address is returned by no endpoint).
- [`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md)
  §Consequences — *"the client says one sentence naming no mailbox, no domain and no other account"*,
  and *"a `202` may not be rendered as recovery being on"*.
- [`STORY-0417`](../stories/STORY-0417-the-recovery-screens.md) §Design notes, first and fourth
  bullets.

## Scope

- **One new module, `web-client/src/account/recovery-text.ts`**, exporting exactly these eleven
  names and nothing else:

  | Name | Value |
  | --- | --- |
  | `RECOVERY_ON` | `"Recovery is on. A verified address can set a new password for this account."` |
  | `RECOVERY_OFF` | `"Recovery is off. With no verified address, a forgotten password cannot be replaced and this account is lost."` |
  | `recoveryLine` | a function, below |
  | `ATTACH_LABEL` | `"Attach a recovery address"` |
  | `ADDRESS_LABEL` | `"Email address"` |
  | `CURRENT_PASSWORD_LABEL` | `"Current password"` |
  | `ATTACH_WHY` | `"Your password is asked for here because a browser someone else reaches would otherwise become permanent ownership of this account."` |
  | `ATTACH_ACKNOWLEDGED` | `"If that address can take mail, a link is on its way. Recovery stays off until you follow it."` |
  | `ATTACH_ADDRESS_REFUSED` | `"That is not an address mail can be sent to."` |
  | `ATTACH_PASSWORD_WRONG` | `"That password does not match this account."` |
  | `ATTACH_FAILED` | `"That did not go through. Try again."` |

- **`recoveryLine(has: boolean): string`** returns `RECOVERY_ON` or `RECOVERY_OFF`, with KDoc saying
  it is the **only** place that branches on the fact — `deviceRouteLine`'s reason, applied again: a
  component choosing between two sentences inline is a second place able to get it wrong.
- **KDoc at the top of the module** naming `ADR-0031` §6.3 for why no constant here can hold an
  address, and `ADR-0078` §Consequences for why `ATTACH_ACKNOWLEDGED` says nothing about whether the
  address was known.
- **`ATTACH_ACKNOWLEDGED` is the one sentence for a `202`, whatever caused it** — attached for the
  first time, replacing a pending claim, or an address that already belongs to somebody else. It must
  read truthfully in all three, and it must **not** say recovery is now on, because `ADR-0031` §3
  leaves `hasRecoveryEmail` false until the link is followed.

## Out of scope

- **Any word the verification or reset screen says.** `TASK-041706`, in this same file.
- **Any word the *forgot password* flow says.** `DEC-081` was the product owner's and `ADR-0087` §1
  answered it with four constants for this same module — they belong to the ticket `ADR-0087`
  unblocked, not to this one, and this ticket's eleven exports stay eleven.
- **`PASSWORD_REFUSED`, `PASSWORD_LABEL`, `CANCEL` or anything already in `account-text.ts`.** They
  are imported by the components that need them; a second constant holding the same characters is a
  second spelling. `account-text.ts` is not edited and a `verify:` line pins its suite at seven.
- **Any component, any request, any layout.** Later tickets.
- **A digit anywhere in a player-facing value.** Password length is `PASSWORD_REFUSED`'s sentence and
  it already exists; nothing here restates a number.

## Tests

`web-client/src/account/recovery-text.test.ts`, new, in `account-text.test.ts`'s idiom —
`import * as recoveryText from "./recovery-text";`. **Three tests.**

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` | `Object.keys(recoveryText).sort()` equals the eleven names above, sorted — so an extra export or a missing one fails even though every `toBe` still matches — then one `toBe` per constant against the literal in the table. This is what makes a rename or a re-worded sentence a red test rather than a silent change |
| `says recovery is on and recovery is off from one place` | `recoveryLine(true)` is `RECOVERY_ON` and `recoveryLine(false)` is `RECOVERY_OFF`, **compared against the constants and not against string literals**, and the two are asserted to be different strings. Both directions, because one cannot tell a branch from a constant |
| `names no mailbox, no domain and no other account` | Over **every** exported string value, in a loop: none contains `@`, none contains `.test`, `.com` or `example`, and none contains the word `taken` or `registered`. `ADR-0078` §Consequences' refusal, asserted over the module rather than over the four sentences somebody remembered. The loop asserts the collection is **non-empty** first, so it cannot pass over nothing |

**No `try` anywhere in the added code, and no `expect()` inside one** — a failing assertion is itself
a throw, and a `try` around one turns a red test green.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'`
      — passes, with the key-set `toEqual` over exactly eleven names
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says recovery is on and recovery is off from one place'`
      — passes, both directions, comparing against the constants
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'`
      — passes, over every exported string, having first asserted there is more than one
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly three**. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — **no `@` anywhere in the file, comments and KDoc included.** A plain-text gate cannot tell
      code from prose, so the KDoc may not write an example address either; say *an address* in words
- [ ] `test "$(grep -oiE 'address is|your address|that address is (taken|registered)' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — no sentence asserts a fact about a particular address. Reads the whole file
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-text.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — the neighbouring module is untouched and still at seven
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket may be dispatched in a batch with `TASK-041702` and `TASK-041703`, whose `Files`
      tables are disjoint from this one's
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
every file they touch is inside this ticket's budget. Golden-string discipline runs the mutation on
the **constants**, never on the assertions.

1. **Change one character** of `RECOVERY_OFF` — drop the final full stop. Predict: `states every
   sentence exactly, character for character` reddens. Record the diff the reporter prints.
2. **Rename `ATTACH_LABEL` to `ATTACH_HEADING`**, same value. Predict: the same test reddens on the
   key-set `toEqual`, not on any `toBe`. This is the case a per-constant assertion cannot see.
3. **Add a twelfth export.** Predict: the key-set `toEqual` reddens. Delete one instead: it reddens
   too. Run both.
4. **Invert `recoveryLine`** — return `RECOVERY_OFF` for `true`. Predict: `says recovery is on and
   recovery is off from one place` reddens **alone**.
5. **Make `recoveryLine` a constant** returning `RECOVERY_ON` always. Predict: the same test reddens
   on its `false` half. Steps 4 and 5 together are why that test asserts both directions.
6. **Plant a forbidden string**: append `" bob@example.test"` to `ATTACH_ACKNOWLEDGED`. Predict:
   `names no mailbox, no domain and no other account` reddens, **and** step 1's test reddens too.
   Record both. If the sweep alone stays green, its loop is not reaching every export — fix the loop.
7. **Vacuity check on the sweep**: empty every constant to `""` and delete the non-empty guard.
   Predict: the sweep passes over nothing. Restore the guard and confirm it reddens. A universal
   claim over an empty collection is the failure this step exists to rule out.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why a new module rather than more constants in `account-text.ts`.** That file is 85 lines and holds
one screen's words; recovery spans four surfaces and this story adds roughly as many strings again.
The client already keeps one text module per area — `history-text.ts`, `ladder-text.ts`,
`account-offer-text.ts`, `name-text.ts` — and each has a single golden test. Splitting also keeps
`account-text.test.ts` at its merged seven, which is what the `verify:` line above pins.

**The `@` gate is deliberately over-broad and is a known cost.** `TASK-041501` shipped a zero-digit
gate that forbade the ADR citation its own Scope demanded, because a plain-text gate cannot tell code
from prose. This one is narrower — an `@` has no business in a copy module at all, in a value or in a
comment — but the same rule applies: write the citations without one.

`grep -c` counts matching **lines** and exits **1** on zero matches, so both zero-expectations above
are wrapped as `test "$(… | wc -l | tr -d ' ')" = 0`.
