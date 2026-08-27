---
schema: 2
id: TASK-041226
title: The sign-in screen's word, and the address that word becomes
type: task
status: done
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, account, routing, copy]
depends_on: [TASK-041225]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a screen for every address, and an address for every screen'
  - cd web-client && grep -qF '"#/sign-in"' src/routing/screen.ts
  - cd web-client && npm run check
---

## Goal

The second account screen has a name a player reads and the permanent address that name becomes.

## The answer this ticket applies

`DEC-077` was answered on 2026-08-26 by
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md).
The screen is ***Sign in***, its permanent address is **`#/sign-in`**, and neither word nor slug is
coined: `ADR-0050` §3 says *sign in* to a player twice, and the hyphen is the one
`docs/protocol.md` already writes in `POST /api/auth/sign-in`. **Nothing here is a choice** — every
literal below is quoted from that ADR, and where the ADR is silent this ticket is silent too.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-text.ts` | modify |
| `web-client/src/account/account-text.test.ts` | modify |
| `web-client/src/routing/screen.ts` | modify |

`screen.test.ts` is not in the budget: its four tests enumerate behaviour, not membership, and pass
unchanged — **measured**, by adding the member to `Screen` on `develop` and running the client gate,
which reached green with no edit to that file. Read, and do not edit:
[`ADR-0083`](../../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
§1, §2 and §3;
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §1.

## Scope

- `account-text.ts` gains `SIGN_IN_HEADING = "Sign in"`, beside `ACCOUNT_HEADING`, and the golden
  test asserts it character for character like every other string in that file.
- **`SIGN_IN_HEADING` and `SIGN_IN_LABEL` are two independent literals holding the same six
  characters, and neither is derived from the other.** `ADR-0083` §3 is explicit: a control's verb
  is not a screen's name, and the two are free to diverge the day `EPIC-06` letter-fits one of them.
  `export const SIGN_IN_HEADING = SIGN_IN_LABEL` would pass the golden test and is wrong; a
  criterion counts the literals.
- `Screen` gains the member `"sign-in"`; `screenFromHash("#/sign-in")` returns it and
  `hashForScreen("sign-in")` returns `"#/sign-in"`. The slug is a **literal in `screen.ts`**, not
  derived from the heading at runtime: `ADR-0076` §1's rule, and the same duplication `duels` and
  `leaderboard` already carry with the comment that explains it.
- Carry a comment in `screen.ts` naming where the hyphen came from — `POST /api/auth/sign-in` in
  `docs/protocol.md`, by `ADR-0083` §2 — as `TASK-041222`'s `account` comment cites its own source.

## Out of scope

- The screen, the branch and the door. `TASK-041227`.
- **The landing after a successful sign-in.** `ADR-0083` §5, and `TASK-041229` owns it.
- Changing `#/account`, `#/duels` or `#/leaderboard`. Three addresses that already work.
- `tokenFromHash`, `reset` and `verify` — `ADR-0081` gives all three to `STORY-0417`.
- **Querying anything by role.** Neither test file here renders a DOM, so the duplicate-string trap
  `ADR-0083` §Consequences names cannot bite in this ticket. It bites in `TASK-041227`, which is
  where the by-role criterion and its proof step live. **A deferral, not an omission** — what this
  ticket *can* gate mechanically is that the two literals exist separately, and a criterion does.

## Tests

Two tests in `account-text.test.ts`; no new test file.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` *(existing, gains rows)* | `SIGN_IN_HEADING` **and** `SIGN_IN_LABEL` each asserted against the literal `"Sign in"`, in the same test. Two assertions rather than one, so the duplication is visible where it is created |
| `names a screen for every address, and an address for every screen` *(new, in `account-text.test.ts`)* | A round trip through `screenFromHash`/`hashForScreen` over **all four** members of `Screen`, plus each `hashForScreen` result asserted against its literal. It lives here rather than in `screen.test.ts` so `TASK-041201`'s four tests stay untouched, and it is the test that reddens when a fifth screen arrives with no address |

## Acceptance criteria

- [ ] `the account screen's words > states every sentence exactly, character for character` passes,
      asserting **both** `SIGN_IN_HEADING` and `SIGN_IN_LABEL` against `"Sign in"`
- [ ] `the account screen's words > names a screen for every address, and an address for every
      screen` passes over all four members
- [ ] The four tests in `screen.test.ts` pass unchanged
- [ ] The slug appears as a literal in `screen.ts` and the heading is **not** imported there:
      `grep -c 'account-text' web-client/src/routing/screen.ts` returns `0`
- [ ] `grep -c '"#/sign-in"' web-client/src/routing/screen.ts` returns `1`
- [ ] The slug matches `^[a-z]+(-[a-z]+)*$` — a hyphenated compound, never leading or trailing a
      hyphen (`ADR-0083` §2, which widens `ADR-0076` §1's bare-word examples for this word only)
- [ ] The two constants are two literals, not one aliased to the other:
      `grep -c '= "Sign in"' web-client/src/account/account-text.ts` returns `2`
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Derive the slug from the heading — `hashForScreen` returning
   `"#/" + SIGN_IN_HEADING.toLowerCase()`.
   **Nothing reddens on the sign-in row alone**, because `"Sign in".toLowerCase()` is `"sign in"`
   and the space is not the hyphen — so `names a screen for every address…` reddens on the
   **literal**, `expected '#/sign in' to be '#/sign-in'`. That is the link `ADR-0076` §1 forbids,
   caught here by a character the heading has and the slug does not. Revert.
2. Add the member to `Screen` and to `screenFromHash` but not to `hashForScreen`.
   **`npm run typecheck` reddens** on the non-exhaustive mapping, if `hashForScreen` is written as an
   exhaustive `switch` or a `Record<Screen, string>`. Write it as an `if` chain with a fallback first
   and watch the compiler stay silent while `names a screen for every address…` reddens instead —
   which is why the round-trip test covers all four members rather than the new one.
3. Give the new screen the slug `"account"`.
   **`names a screen for every address, and an address for every screen` reddens**, because two
   screens map to one address and the round trip cannot return both. A test over the new member alone
   would pass in one direction.
4. Spell the slug `"#/sign_in"` — an underscore where the hyphen belongs.
   **`names a screen for every address, and an address for every screen` reddens** on the literal,
   and so does the `verify:` grep for `"#/sign-in"`. The hyphen is now the **correct** spelling
   (`ADR-0083` §2, from `POST /api/auth/sign-in`) and the underscore is the one this product writes
   nowhere; `signin` is the same failure with one fewer character. This is the step that inverted
   when `DEC-077` was answered — the old ticket predicted a hyphen would fail a `^[a-z]+$`
   criterion, and the criterion is what changed.
5. Write `export const SIGN_IN_HEADING = SIGN_IN_LABEL;`.
   **Nothing reddens** — the golden test asserts a value and the value is right. Only the literal
   count criterion catches it. Record it in the PR: `ADR-0083` §3 wants two constants that happen to
   agree today, and *"one string said once"* is the tempting change that quietly makes `EPIC-06`
   unable to letter-fit the button without renaming the screen.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
