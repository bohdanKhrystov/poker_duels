---
schema: 2
id: TASK-041222
title: The account screen has an address, and the lobby has the door
type: task
status: done
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, account, routing, ui]
depends_on: [TASK-041231]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/routing/screen.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the first screen for the account, and comes back to it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the account screen at the address alone, with no click at all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the account door whether or not the profile read succeeded'
  - test "$(grep -c '"#/account"' web-client/src/routing/screen.ts)" = 1
  - test "$(grep -c 'readSessionToken' web-client/src/lobby/Lobby.tsx)" = 0
  - cd web-client && npm run check
---

## Goal

`#/account` is an address, the lobby has a door to it, and every part built since `TASK-041216`
becomes something a player can actually reach.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/screen.ts` | modify |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

`screen.test.ts` is **not** in the budget: its four tests already enumerate the union and pass
unchanged, because the new member has its own tests below. Read, and do not edit:
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §1 and §3;
[`ADR-0036`](../../docs/adr/ADR-0036-an-account-is-offered-never-required.md);
`web-client/src/account/AccountScreen.tsx`.

## Scope

- `Screen` gains `"account"`, and the slug is the literal `"account"` — `hashForScreen("account")` is
  `"#/account"` and `screenFromHash("#/account")` is `"account"`. **The word is not coined here**: the
  product already says *account* to a player, in `ADR-0050` §3's merged confirmation sentence
  (*"…will never sign in to this account again"*), in `ADR-0036`'s decision, and in `ADR-0056` §2's
  *no account was created*. `ADR-0076` §1's rule is the lowercase ASCII form of a word the product
  already says, and this is one. Carry that citation as a comment.
- `Lobby.tsx` gains one branch, after the ladder's and in the same shape: the account screen with the
  in-page *Back* rendered beside it by the swap, exactly as `HistoryScreen` and `LadderScreen` get
  theirs (`ADR-0060` §4). `AccountScreen` is handed `useProfileStrip()` and a `signedIn` flag.
- `signedIn` comes from **`useSignedIn()`**, called in `Lobby.tsx` beside the existing
  `useHistory()` and `useLadder()` from the same import. `TASK-041231` lands the hook, the context
  and the one module-scope `readSessionToken(localStorage) !== null` in `main.tsx` before this ticket
  starts. **`main.tsx` is not edited here and is not in the budget** — the flag is consumed, never
  computed. It is read at module scope rather than inside a component for the reason `DEC-032`
  records about the shadowed global under Vitest, and that reason lives with the read, in
  `TASK-041231`.
- One more door on the first screen, labelled `ACCOUNT_HEADING`, calling `open("account")`, beside
  the record's and the ladder's.
- **The door is offered whatever the profile read answered.** `ADR-0036`: nothing gates on having an
  account, and `ADR-0060` §3 already keeps the record's door open when the read fails. A player whose
  profile read failed is exactly the player who may want to sign in.

## Out of scope

- **Computing `signedIn`, and every line of `main.tsx`.** `TASK-041231` reads the token once at
  module scope and exports `useSignedIn()`; this ticket calls it. **Gated below**: the *Files* table
  has no `main.tsx` row, and a criterion counts `readSessionToken` in `Lobby.tsx` at `0`.
- **The sign-in screen's slug.** Behind `DEC-077`; `TASK-041225` adds it.
- **`tokenFromHash` and the `reset`/`verify` slugs.** `ADR-0081` gives them to `STORY-0417`.
- Changing `AccountScreen`, `SignUpForm`, `RevokeControl` or `SignOutControl`. They are composed here
  and not edited.
- Any address for the table, the waiting screen or the result screen — `ADR-0076` §2 forecloses all
  three permanently.

## Tests

`web-client/src/App.test.tsx`, in the existing `describe("App")`.

| Test | Proves |
| --- | --- |
| `leaves the first screen for the account, and comes back to it` | Clicking the door puts `ACCOUNT_HEADING` on screen and `window.location.hash` at `"#/account"`; the in-page *Back* returns the first screen and leaves the hash `""`. Both ends, both times |
| `opens the account screen at the address alone, with no click at all` | With `window.location.hash` set to `"#/account"` before the render, the account screen is showing and the room-code form is not |
| `offers the account door whether or not the profile read succeeded` | The door is on the first screen when the read answers a profile **and** when it answers `unavailable`, asserted in one test. `ADR-0036`, gated |
| `does not offer the account door while a duel is in progress` | With a store carrying a `view`, the door is absent and the duel table is on screen — the same rule the record and the ladder already have |
| `shows the duel to a player reading the account screen when a frame seats them` | Address `"#/account"` plus a store carrying a `view`: the duel table is on screen, the account heading is not, and the hash settles to `""`. `ADR-0076` §3 through the real composition rather than through `Lobby.test.tsx`'s fixture |

## Acceptance criteria

- [ ] `App > leaves the first screen for the account, and comes back to it` passes, asserting the
      address at both ends
- [ ] `App > opens the account screen at the address alone, with no click at all` passes
- [ ] `App > offers the account door whether or not the profile read succeeded` passes for **both**
      read outcomes
- [ ] `App > does not offer the account door while a duel is in progress` passes
- [ ] `App > shows the duel to a player reading the account screen when a frame seats them` passes,
      asserting the heading's absence **and** the replaced address
- [ ] The four tests in `screen.test.ts` pass unchanged
- [ ] Every pre-existing test in `App.test.tsx` passes unchanged
- [ ] `grep -c '"#/account"' web-client/src/routing/screen.ts` returns `1`
- [ ] `grep -c 'readSessionToken' web-client/src/lobby/Lobby.tsx` returns `0` — the flag arrives
      through `useSignedIn()`, which `TASK-041231` exports from `main.tsx`
- [ ] No file outside the three listed differs — `main.tsx` in particular is byte-unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Put the account branch **above** the `view` branch in `Lobby.tsx`.
   **`shows the duel to a player reading the account screen when a frame seats them` reddens** on the
   heading, while `does not offer the account door while a duel is in progress` still passes — the
   door is a control on the first screen and is unreachable either way. That asymmetry is the reason
   the fifth test exists at all, and it is the same one `TASK-041203`'s Proof found for the record.
   Revert.
2. Spell the slug `"accounts"`.
   **`leaves the first screen for the account, and comes back to it` reddens on the address
   assertion** and `opens the account screen at the address alone` reddens outright. Nothing else
   moves — the door still works, because the door and the address agree with each other while both
   disagree with the ticket. Run it: a self-consistent wrong slug is the failure mode a round-trip
   test cannot see, which is why the criterion greps for the literal.
3. Gate the door on `profile?.kind === "profile"`.
   **`offers the account door whether or not the profile read succeeded` reddens on the `unavailable`
   half alone.** Render only the success case and the mutation passes. This is `ADR-0036`'s rule and
   the reason that test carries two fixtures.
4. Read `readSessionToken(localStorage)` inside `AccountScreen` instead of receiving `signedIn`.
   **Nothing reddens in `App.test.tsx`**, because the tests set the real storage. Record it: the
   defect is `DEC-032`'s shadowed global, which bites in a different Node version and not in this
   suite, and the criterion above is what makes it checkable.
5. Drop `open("account")` and set `location.hash` directly in `Lobby.tsx`.
   **Nothing reddens.** It is a second navigation authority in a file that has one, it is exactly the
   drift `ADR-0076` §Consequences predicts *"will be caught by review or not at all"*, and saying so
   is better than implying a test covers it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This ticket's `## Proof` step 1 says the inversion "reddens on the heading." It does not — and the
correction is the whole reason this PR gates anything.** Inverting the branch order so the address
outranks the store reddens on `expect(vi.mocked(AccountScreen)).not.toHaveBeenCalled()`. **Remove that
spy line and the heading-only check passes under the same inversion**, measured by coder and reviewer
independently.

The mechanism: the address-correcting effect self-corrects inside `render()`'s own `act()` flush, so
**both branch orders settle on the duel table with the hash cleared**. Only the transient differs. What
a seated player would actually suffer is a flash of the account screen on the first paint — real, and
invisible to any settled-DOM assertion.

**Third instance of the same failure mode in this run, in three unrelated files.** `NameSurface`'s call
count of one came from the `disabled` attribute rather than the ref guard; `TASK-041204`'s first
attempt asserted on DOM that converged either way; and here the heading converges too. In each case an
assertion observed a **genuine effect produced by a different mechanism than the one under test** — the
hardest kind to see, because nothing about reading it looks wrong.

**Two steps reddened more than predicted, and both extras are real catches.** Step 2's misspelled slug
also reddens `shows the duel…seats them`, because that test hardcodes `"#/account"` as its setup hash —
under the mutation `screenFromHash` reads it as `"first"`, the correcting effect's `screen !== "first"`
guard never fires, and the hash is never replaced. Step 3's door gate also reddens two tests using
`renderApp()`'s default non-`"profile"` fixture. Neither was weakened to match the narrower prediction.

**Step 4 was not reproduced, and was not reported as though it had been.** It requires editing
`AccountScreen.tsx`, which this ticket marks *Read, and do not edit*. The coder refused and explicitly
declined to assert the ticket's claim as measured — worth more than a plausible reconstruction, which
is what five coders this run had to be sent back to replace.

**The counts other tickets gate are intact.** `main.tsx` is byte-unchanged, so `TASK-041210`'s
`authorizedFetch(`=1 and `fetch: apiFetch`=4 hold, as does `TASK-041231`'s `= useSignedIn()`=**0**
there — this ticket makes that call in `Lobby.tsx`, which is exactly where the seam was cut for it.
