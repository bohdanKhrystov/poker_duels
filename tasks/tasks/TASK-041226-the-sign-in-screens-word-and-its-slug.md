---
schema: 2
id: TASK-041226
title: The sign-in screen's word, and the address that word becomes
type: task
status: blocked
parent: STORY-0412
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, account, routing, copy, blocked]
depends_on: [TASK-041225]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names a screen for every address, and an address for every screen'
  - cd web-client && npm run check
---

## Goal

The second account screen has a name a player reads and the permanent address that name becomes.

## Blocked on `DEC-077`

**The product owner's.** `ADR-0076` §1 makes a slug *"the lowercase ASCII form of a word the product
already says to a player"* and says outright that the ADR **coins no player-facing vocabulary** — if
a screen needs a word the product does not yet say, that is a product question. How many account
screens this story has and what they are called is `STORY-0412`'s call, and the count is settled:
**two**. One of the two names is settled too — the product already says *account* to a player, in
`ADR-0050` §3's merged confirmation text, in `ADR-0036` and in `ADR-0056` §2 — and `TASK-041222`
shipped `#/account` on that basis.

Open: **what does the product call the screen a player opens to reach an account from a browser that
does not hold it, and therefore what is that screen's permanent slug?** The product says the *verb*
— `ADR-0050` §3 writes *"You stay signed in here"* and *"This device signs in to this account"* — and
says no **noun** for the screen. A slug wants one word: `duels` came from *Your duels* and
`leaderboard` from *Leaderboard*, and `ADR-0081` fixed `reset` and `verify` the same way.

Why it is not decided in this ticket: an address is player-facing text this product owns **forever**
(`ADR-0076` §Consequences), it is the fourth entry in a table `STORY-0417` and `EPIC-05` both extend,
and `ADR-0081` already fixed two neighbouring slugs before this story named its screens. A word
chosen inside a ticket reads as settled to everyone who arrives later.

**Everything else in `STORY-0412` is unblocked**, deliberately: this ticket and `TASK-041227` are the
only two behind the decision, and they sit at the end of the chain. The PR that answers `DEC-077`
fills in the constant, the slug and the two test literals here, and nothing else in the story moves.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/account-text.ts` | modify |
| `web-client/src/account/account-text.test.ts` | modify |
| `web-client/src/routing/screen.ts` | modify |

`screen.test.ts` is not in the budget: its four tests enumerate behaviour, not membership, and pass
unchanged. Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §1;
[`ADR-0081`](../../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
§1.

## Scope

- `account-text.ts` gains one constant — the heading `DEC-077` names — beside `ACCOUNT_HEADING`, and
  the golden test asserts it character for character like every other string in that file.
- `Screen` gains one member, and `screenFromHash`/`hashForScreen` gain one row each. The slug is a
  **literal in `screen.ts`**, not derived from the heading at runtime: `ADR-0076` §1's rule, and the
  same duplication `duels` and `leaderboard` already carry with the comment that explains it.
- The slug is the lowercase ASCII form of the answered word, and the ticket records in a comment
  which merged document the word came from — as `TASK-041222`'s `account` does.

## Out of scope

- The screen, the branch and the door. `TASK-041227`.
- Changing `#/account`, `#/duels` or `#/leaderboard`. Three addresses that already work.
- `tokenFromHash`, `reset` and `verify` — `ADR-0081` gives all three to `STORY-0417`.

## Tests

Two existing tests gain a row each; no new test file.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` *(in `account-text.test.ts`)* | The new heading asserted against its literal, like every other string in that module |
| `names a screen for every address, and an address for every screen` *(new, in `account-text.test.ts`)* | A round trip through `screenFromHash`/`hashForScreen` over **all four** members of `Screen`, plus each `hashForScreen` result asserted against its literal. It lives here rather than in `screen.test.ts` so `TASK-041201`'s four tests stay untouched, and it is the test that reddens when a fifth screen arrives with no address |

## Acceptance criteria

- [ ] `DEC-077` is answered by a **merged** ADR, and this ticket's status is no longer `blocked`
- [ ] `the account screen's words > states every sentence exactly, character for character` passes
      with the new heading in it
- [ ] `the account screen's words > names a screen for every address, and an address for every
      screen` passes over all four members
- [ ] The four tests in `screen.test.ts` pass unchanged
- [ ] The slug appears as a literal in `screen.ts` and the heading is **not** imported there:
      `grep -c 'account-text' web-client/src/routing/screen.ts` returns `0`
- [ ] The slug is lowercase ASCII: it matches `^[a-z]+$`
- [ ] No file outside the three listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Derive the slug from the heading — `hashForScreen` returning
   `"#/" + SIGN_IN_HEADING.toLowerCase()`.
   **Nothing reddens**, which is the point: the two agree today. Then change the heading's letter
   case and watch `names a screen for every address…` redden on the literal — that is the link
   `ADR-0076` §1 forbids, and the grep criterion is the only mechanical gate on it. Revert both.
2. Add the member to `Screen` and to `screenFromHash` but not to `hashForScreen`.
   **`npm run typecheck` reddens** on the non-exhaustive mapping, if `hashForScreen` is written as an
   exhaustive `switch` or a `Record<Screen, string>`. Write it as an `if` chain with a fallback first
   and watch the compiler stay silent while `names a screen for every address…` reddens instead —
   which is why the round-trip test covers all four members rather than the new one.
3. Give the new screen the slug `"account"`.
   **`names a screen for every address, and an address for every screen` reddens**, because two
   screens map to one address and the round trip cannot return both. A test over the new member alone
   would pass in one direction.
4. Spell the slug with a hyphen or an underscore.
   **The lowercase-ASCII criterion catches it and no test does.** Say so in the PR: `ADR-0076` §1
   says *a word*, and *a word* is the constraint this ticket cannot express as an assertion beyond
   the character class.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
