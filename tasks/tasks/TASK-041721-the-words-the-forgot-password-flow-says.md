---
schema: 2
id: TASK-041721
title: The words the forgot-password flow says, and the second state it refuses to have
type: task
status: done
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, account, recovery, copy]
depends_on: [TASK-041706]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says one thing to everyone who asks for a link, and never that mail was sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'
  - test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'FORGOT_PASSWORD_' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 4
  - test "$(grep -oiE 'no such|not found|check the spelling|does not exist|unknown address' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-text.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && npm run check
---

## Goal

`recovery-text.ts` gains the four sentences `ADR-0087` §1 fixes for the *forgot password* flow —
taking it from **20 exports to 24** — and a test says, in one place, that the flow has no fifth
sentence and never claims mail was sent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/recovery-text.ts` | modify |
| `web-client/src/account/recovery-text.test.ts` | modify |

Read, and do not edit:

- [`ADR-0087`](../../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) §1
  (the four constants, and the two that are **imported rather than authored**), §5 (which sentence
  answers which outcome) and §6 (an address the product does not hold gets exactly what everybody
  else gets — *"no hint, no `check the spelling`, no `no account found`, no count"*).
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §5 — `202` for five
  different situations with an identical empty body.
- [`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md)
  §Consequences — *"the client's copy has to be honest about a pending state rather than
  congratulatory about a `202`"*.
- `web-client/src/account/account-text.ts` — `CANCEL` and `SIGN_UP_FAILED` live here and are **not**
  re-declared. `account-text.ts` is not edited and a `verify:` line pins its suite at seven.

## Scope

- **Four more exports**, appended to `recovery-text.ts`, taking it to **24**:

  | Name | Value |
  | --- | --- |
  | `FORGOT_PASSWORD_LABEL` | `"Forgot your password?"` |
  | `FORGOT_PASSWORD_SUBMIT` | `"Send a link"` |
  | `FORGOT_PASSWORD_ACKNOWLEDGED` | `"If that address is verified on an account here, a link is on its way. Follow it to set a new password."` |
  | `FORGOT_PASSWORD_FAILED` | `"That did not go through. Try again."` |

  Character for character from `ADR-0087` §1's table. Nothing else in the file changes.
- **No fifth constant, and no constant for the address the product does not hold.** `ADR-0087` §6
  fixes that this flow has **one** acknowledgement for every address; a `FORGOT_PASSWORD_UNKNOWN`
  would be the oracle `ADR-0031` §5 closed on the wire, rebuilt in the copy.
- **`ADDRESS_LABEL` and `CANCEL` are reused, not re-authored** (`ADR-0087` §1). `ADDRESS_LABEL` is
  already in this module; `CANCEL` stays in `account-text.ts` and is imported by the component that
  renders it. Nothing is added here for either.
- **`FORGOT_PASSWORD_LABEL` is one literal, not two.** `ADR-0087` §2 departs from `ADR-0083` §3 on
  purpose: the door's words and the form's heading are the same utterance seen one at a time, so
  there is no `FORGOT_PASSWORD_HEADING`.
- **One KDoc block above the four**, naming `ADR-0087` §1 for the coining, `ADR-0031` §5 for why the
  acknowledgement states a rule instead of reporting an outcome, and `ADR-0087` §6 for why there is
  no second sentence. **It must not name any of the four constants**, and must contain no `@` — a
  `verify:` line counts `FORGOT_PASSWORD_` at exactly four, which is the four `export const` lines
  and nothing else.

## Out of scope

- **Any component, any control, any request.** `TASK-041722` writes the form; `TASK-041723` hangs the
  door on the sign-in screen.
- **A slug.** `ADR-0087` §3: `Screen`, `screenFromHash` and `hashForScreen` gain nothing, and nothing
  may write `forgot-password` into `screen.ts` on this ADR's authority. `web-client/src/routing/` is
  not opened by this ticket at all.
- **Re-wording `SIGN_UP_FAILED` or `ATTACH_FAILED` to share one constant with the new one.**
  `ADR-0087` §1 makes three literals holding the same six words a deliberate choice: one flow's
  sentence is not another's, and a rename moves one. All three stay where they are.
- **Anything else in `recovery-text.ts`.** The twenty merged exports keep their names and their
  bytes.

## Tests

`web-client/src/account/recovery-text.test.ts`. **4 merged tests become 5**, and one of the four
moves — this ticket owns it, because its change is what invalidates it.

| Test | What happens |
| --- | --- |
| `states every sentence exactly, character for character` | **moves**: its key-set `toEqual` grows from twenty names to twenty-four, and it gains one `toBe` per new constant, against the literals in the table above. Nothing already in it changes, no existing entry moves and no assertion is weakened |
| `names no mailbox, no domain and no other account` | **unchanged**: it loops over every string export, so it covers the four new ones with no edit at all. **Measured on this worktree**: with the four constants added and no other change, this test stays green — so if it reddens, that is a real finding about the values, not about the loop |
| `says recovery is on and recovery is off from one place` | unchanged |
| `tells a dead link and a refused password apart, in words a player can act on` | unchanged |
| `says one thing to everyone who asks for a link, and never that mail was sent` | **new**, below |

The new test, three properties, each its own assertion:

1. **There are exactly four `FORGOT_PASSWORD_*` exports and they are these four.**
   `Object.keys(recoveryText).filter((key) => key.startsWith("FORGOT_PASSWORD_")).sort()` equals the
   four names sorted. `ADR-0087` §6 in the copy module: a fifth sentence is the unknown-address state
   this flow refuses to have, and this is the assertion a later ticket has to argue with to add one.
2. **`FORGOT_PASSWORD_ACKNOWLEDGED` does not contain `"sent"`**, case-insensitively. It states the
   condition under which mail is sent and never reports that any was — `ADR-0031` §5 answers `202`
   for five situations, and `ADR-0078` §Consequences forbids being congratulatory about it. Scoped to
   this one constant on purpose: **measured**, `sent` occurs four times elsewhere in the module, so a
   whole-module version of this property is false and must not be written.
3. **None of the four values hints at what the product knows about the address**: none contains
   `"check"`, `"spelling"`, `"not found"`, `"does not exist"`, `"unknown"` or `"no such"`,
   case-insensitively, asserted over the four values in a loop that first asserts it has four of
   them. `ADR-0087` §6's *forecloses*, in the place the temptation to be helpful actually shows up.

**No `try` anywhere in the added code, and no `expect()` inside one.** A `try`/`finally` with no
`catch` does **not** swallow a failing `expect()` — only a `catch`, or an assertion helper that
catches internally, does (`TASK-041720` measured this) — but a `try` here is still refused, because
it is the shape a later `catch` gets added to.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'`
      — passes, with the key-set `toEqual` over exactly **twenty-four** names and one `toBe` per new
      constant
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says one thing to everyone who asks for a link, and never that mail was sent'`
      — passes, all three properties above
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no mailbox, no domain and no other account'`
      — still passes, now over twenty-four strings, **with no edit to that test**
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/recovery-text.test.ts 2>&1 | grep -qE 'Tests +5 passed \(5\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly five**: `TASK-041706`'s four plus
      this one. Both lines, because a collection error prints a *passing* `Tests` count with no
      failure line at all
- [ ] `test "$(grep -oE '@' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — still no `@` anywhere in the file, KDoc included
- [ ] `test "$(grep -oF 'FORGOT_PASSWORD_' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 4`
      — **hand-counted, and here are the four occurrences**: the `export const` line of
      `FORGOT_PASSWORD_LABEL`, of `FORGOT_PASSWORD_SUBMIT`, of `FORGOT_PASSWORD_ACKNOWLEDGED` and of
      `FORGOT_PASSWORD_FAILED`. Nothing else in this module may write the prefix — it holds no logic
      and no cross-reference, and the KDoc names none of them. A fifth occurrence is a fifth sentence
      or a comment quoting a constant, and both are refused above
- [ ] `test "$(grep -oiE 'no such|not found|check the spelling|does not exist|unknown address' web-client/src/account/recovery-text.ts | wc -l | tr -d ' ')" = 0`
      — **measured at zero across all twenty-four values and every comment before this ticket
      starts**, so any occurrence is this diff's. `ADR-0087` §6, read off the file rather than off the
      four values a reader remembered
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/account-text.test.ts 2>&1 | grep -qE 'Tests +7 passed \(7\)'`
      — the neighbouring copy module is untouched and still at seven
- [ ] `cd web-client && npm run check` exits 0. Run it **bare**, reading `$?` directly: piping it into
      `tail` makes `$?` the pipe's status and has already shipped a false green on `TASK-041714`
- [ ] The three merged tests other than the key-set one pass unchanged. No assertion moves and none is
      weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget. **A prediction that fails is the finding**: report it as
measured and do not adjust it to match. Seven Proof steps in this story measured something other than
predicted and every one was worth knowing, including one whose ticket's own premise was wrong.

1. **Drop the question mark** from `FORGOT_PASSWORD_LABEL`. Predict: `states every sentence exactly,
   character for character` reddens on that `toBe` alone. This is the product's first question to a
   player (`ADR-0087` §Consequences) and the mark is part of the coined phrase.
2. **Add a fifth export, `FORGOT_PASSWORD_UNKNOWN_ADDRESS`**, with any value. Predict: **two** tests
   redden — the key-set `toEqual`, and the new test's first property. Record both. If only one
   reddens, the new test is not filtering on the prefix and property 1 is asserting nothing.
3. **Rename `FORGOT_PASSWORD_SUBMIT` to `FORGOT_PASSWORD_SEND`**, same value. Predict: `states every
   sentence exactly, character for character` reddens **on its key set**, which is the assertion that
   runs first — a red run names a prefix, so the `toBe` below it is never reached — and the new test's
   first property reddens too. The case a per-constant assertion cannot see.
4. **Rewrite `FORGOT_PASSWORD_ACKNOWLEDGED`** to `"A link has been sent to that address."`. Predict:
   the key-set test reddens on that `toBe`, **and** the new test's second property reddens on `sent`.
   Record both — this is the exact sentence `ADR-0078` §Consequences was written to refuse, and one
   test seeing it is not enough.
5. **Append `" If we cannot find that address, check the spelling."`** to
   `FORGOT_PASSWORD_ACKNOWLEDGED`. Predict: the new test's third property reddens, `states every
   sentence exactly, character for character` reddens on that constant's `toBe`, **and** the needle
   gate exits non-zero. Three signals, and the gate is the one that fires without running a test at
   all.
6. **Vacuity check on the new test's third property.** Empty the four constants to `""` and delete
   the four-value guard. Predict: the loop passes over nothing while the module says nothing. Restore
   the guard and confirm it reddens. A universal claim over an empty collection is what this step
   rules out.
7. **Plant `" bob@example.test"`** on `FORGOT_PASSWORD_LABEL`. Predict: `names no mailbox, no domain
   and no other account` reddens **without being edited by this ticket**, and the `@` gate exits
   non-zero. If the sweep alone stays green, its loop is not reaching the four new exports — say so,
   because that is a defect in a merged test rather than in this diff.

> **A red run names a prefix, not a set.** Vitest stops reporting past its first hard failure.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Everything in this ticket was measured on `develop` at `31894cee` before it was written.** With
only the four constants appended and nothing else changed, the whole client suite went from green to
**one** failing test — `states every sentence exactly, character for character`, on its key-set
`toEqual` — in **one** file. No other test in the client reads this module's export list, `tsc` was
clean, ESLint was clean and Prettier was clean. That is why this ticket's `Files` table is two rows
and why exactly one merged test is declared as moving.

**`TASK-041705`'s `address is` gate is deliberately not carried forward, and it is not this ticket's
casualty.** That gate — `grep -oiE 'address is|your address|…' = 0` — was written when the module had
eleven exports. `TASK-041706` took it to twenty and dropped the gate, because `VERIFY_DONE` and
`VERIFY_ADDRESS_TAKEN` both legitimately say *that address is*: **measured, it already scores 2 on
`develop` today**. `FORGOT_PASSWORD_ACKNOWLEDGED` would make it 3. The `verify:` block above keeps the
gates that still hold — `@` and the §6 needle list — and does not resurrect a superseded one.

**Why the second property is scoped to one constant.** *Measured*: `sent` occurs **four** times
across the merged module, so `grep`ping the whole file for it, or looping the property over every
export, is a gate no correct implementation can satisfy. The property is true of
`FORGOT_PASSWORD_ACKNOWLEDGED` and of nothing else, and it is asserted there.
