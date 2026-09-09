---
schema: 2
id: TASK-141008
title: The confirmation has a sentence for each of the two sign-outs
type: task
status: done
parent: STORY-1410
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, copy]
depends_on: [TASK-141007]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/account-text.test.ts 2>&1 | grep -qF "account-text.test.ts  (8 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/SignOutControl.test.tsx 2>&1 | grep -qF "SignOutControl.test.tsx  (5 tests)"'
  - sh -c 'S=$(sed -n "s/^export const SIGN_OUT_HANDS_A_NEW_PROFILE = \"\(.*\)\";\$/\1/p" web-client/src/account/account-text.ts) && [ -n "$S" ] && grep -qF "$S" design/screens/account.html'
  - sh -c 'test $(grep -c "This browser goes back to the profile it had before.\";" web-client/src/account/account-text.ts) -eq 1'
  - sh -c 'S=$(sed -n "s/^export const SIGN_OUT_HANDS_A_NEW_PROFILE = \"\(.*\)\";\$/\1/p" web-client/src/account/account-text.ts); [ -n "$S" ] && ! printf "%s" "$S" | grep -qF "goes back to the profile it had before"'
  - sh -c 'cd web-client && test $(grep -c "export function signOutWarning" src/account/account-text.ts) -eq 1'
  - sh -c 'cd web-client && ! grep -rq "SIGN_OUT_HANDS_A_NEW_PROFILE" src/account/SignOutControl.tsx src/account/AccountScreen.tsx src/lobby/Lobby.tsx'
  - git diff --exit-code -- design/screens/account.html web-client/src/account/SignOutControl.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`account-text.ts` carries the words for **both** sign-outs, transcribed from
`design/screens/account.html`, and one function decides which of the two a caller gets.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-text.ts` | modify |
| `web-client/src/account/account-text.test.ts` | modify |
| `web-client/src/design/card-text.test.ts` | modify |

Read, do not edit: `design/screens/account.html` (the two frames `TASK-141001` added — this is
where the words come from), `docs/adr/ADR-0131-signing-out-of-your-own-account-hands-the-browser-a-new-profile.md`
§5.

## Scope

- `SIGN_OUT_WARNING` keeps its value, **character for character**. `ADR-0131` §5: in the returning
  case the shipped sentence *"is already exactly right and stays."* What §Consequences calls false
  is the sentence being said **unconditionally**, and that is what this ticket ends.
- A new export, `SIGN_OUT_HANDS_A_NEW_PROFILE`, transcribed **verbatim** from the
  `Signing out — this browser becomes a new profile` frame's `<p class="line">`. Written as **one
  string literal on one source line** — not a `"…" + "…"` concatenation. That is not a style
  preference: `ADR-0142` §Context measured that `grep` for `SIGN_OUT_WARNING`'s value against its
  own source returns **0** occurrences precisely because it is hand-concatenated, and this
  ticket's module-to-card gate is a `sed` extraction followed by a `grep -F` against the card. A
  concatenated literal makes that gate report the string absent.
- A new function beside `deviceRouteLine`, and modelled on it:

  ```ts
  export function signOutWarning(handsANewProfile: boolean): string
  ```

  returning `SIGN_OUT_HANDS_A_NEW_PROFILE` on `true` and `SIGN_OUT_WARNING` on `false`. Its KDoc
  says what `deviceRouteLine`'s says and for the same reason: *"this function is the single place
  that branches on them — a component choosing between the two sentences inline would be a second
  place able to get it wrong."*
- `account-text.test.ts`'s export-name list gains **two** entries and stays exactly sorted; the new
  constant gains an exact-value assertion beside the twenty-odd already there.

## Out of scope

- **Editing the card.** The words are the card's (`ADR-0131` §5, `ADR-0091` §2) and `TASK-141001`
  wrote them. `git diff --exit-code` over `design/screens/account.html` says this ticket only
  reads it. If the card's sentence is wrong, that is a new ticket against the card, not an edit
  here.
- **Using either string.** `SignOutControl` starts calling `signOutWarning` in `TASK-141009`, and
  the `! grep -rq` gate says no component names the new constant yet.
- **`ADR-0142`'s register.** `web-client/src/design/card-text.test.ts` does not exist at
  `c6a41e6d`; this ticket adds no row to a file that is not there. Whichever of the two lands
  second owes the classification of these two exports.
- **`PERMANENCE_LINE`, `ANONYMOUS_*` and the revoke sentences.** Other stories' strings; the
  export-name list is a set equality and will say so if one moves.

## Tests

`account-text.test.ts`, **7 today → 8**.

Inside the existing `states every sentence exactly, character for character`:

- `SIGN_OUT_HANDS_A_NEW_PROFILE` and `signOutWarning` join the sorted `Object.keys` list — the test
  already comments that *"an extra or a missing one fails here even if every literal below still
  matches"*, which is what makes this a set equality and not a subset.
- `expect(accountText.SIGN_OUT_HANDS_A_NEW_PROFILE).toBe("…")` with the sentence written out in
  full, as every other constant in that test is.
- `expect(accountText.SIGN_OUT_WARNING).toBe(…)` is **unchanged**, both halves of the
  concatenation exactly as they are today.

The one new test:

| Test | Proves |
| --- | --- |
| `says one thing about the sign-out in each of its two states` | `signOutWarning(true)` is `SIGN_OUT_HANDS_A_NEW_PROFILE` and `signOutWarning(false)` is `SIGN_OUT_WARNING`; and, separately, that the two are **not equal** to each other. Modelled on the file's own `says one thing about the device route in each of its two states` |

## What would still pass if the coder were wrong

- **`signOutWarning` returning `SIGN_OUT_WARNING` for both** — the shape that ships today's
  behaviour under a new name — passes a one-sided assertion and fails the two-input test. That is
  why both polarities are asserted in one test rather than one each.
- **A `SIGN_OUT_HANDS_A_NEW_PROFILE` that is a copy of `SIGN_OUT_WARNING`** passes both branch
  assertions and fails the inequality assertion beside them, and fails the negative `verify:` gate
  that refuses `goes back to the profile it had before` inside the extracted literal.
- **A sentence typed from memory rather than transcribed** passes the exact-value test — which
  compares the module to itself — and fails the module-to-card gate, which extracts the literal
  with `sed` and `grep -F`s it against `design/screens/account.html`. The two assertions look
  redundant and are not: one pins the string, the other pins where the string came from.
- **Writing the constant as `"…" + "…"`** passes every vitest assertion and makes the `sed`
  extraction match nothing, so the gate fails on `[ -n "$S" ]` rather than passing quietly. This is
  `ADR-0142` §Context's measured trap, and the gate is written to be loud about it rather than
  blind.
- **Changing `SIGN_OUT_WARNING`** is caught twice: by the unchanged exact-value assertion and by
  the `grep -c` gate on its closing literal.

## Acceptance criteria

- [ ] `account-text.test.ts` reports `(8 tests)` and all pass
- [ ] `SignOutControl.test.tsx` still reports `(5 tests)` and all pass
- [ ] The module-to-card gate exits 0: the extracted `SIGN_OUT_HANDS_A_NEW_PROFILE` literal is
      non-empty and appears in `design/screens/account.html`
- [ ] The negative gate exits 0: the extracted literal does not contain
      `goes back to the profile it had before`
- [ ] `grep -c` for `SIGN_OUT_WARNING`'s closing literal is 1, and for
      `export function signOutWarning` is 1
- [ ] `! grep -rq "SIGN_OUT_HANDS_A_NEW_PROFILE"` over `SignOutControl.tsx`, `AccountScreen.tsx`
      and `Lobby.tsx` exits 0
- [ ] `git diff --exit-code` over `design/screens/account.html` and `SignOutControl.tsx` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
