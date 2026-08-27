---
schema: 2
id: TASK-041407
title: Claimed here — the duel, the coin the server sent, the name, and the credential
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, e2e, test, auth, identity]
depends_on: [TASK-041406]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +2 passed \(2\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'plays a duel anonymously and reads back the coin the server sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx --reporter=verbose 2>&1 | grep -qF 'names the profile and then claims it, and the claim moves neither'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'
  - cd web-client && npm run check
---

## Goal

The first half of the arc: browser A plays the committed duel to a win, reads the coin the server
sent, names the profile, and attaches a credential to it.

## Why this exists

`STORY-0414`'s criterion 1 is one test carrying the whole arc. This ticket lands the first four
steps of it; `TASK-041408` lands the rest in the same file. Splitting the arc across two tickets is
not a split of the *test* — it is a split of the diff, and the second ticket's test is the one the
criterion names.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | create |

Read, and do not edit: `web-client/src/e2e/drive-arc.tsx`; `web-client/src/e2e/account-server.ts`;
`web-client/src/e2e/drive-duel.tsx`; `web-client/src/profile/NameSurface.tsx`;
`web-client/src/account/SignUpForm.tsx`.

## Scope

- The file carries the partial `vi.mock("../main", importOriginal)` and the `vi.hoisted` `ArcWiring`
  that `TASK-041406` specifies. `beforeEach` resets `window.location.hash = ""`.
- The fake server holds **two** players built from the script's own seats: `player-seat-0` /
  `device-seat-0` and `player-seat-1` / `device-seat-1`. Their balances, names and duel rows differ.
- **The duel row for each player is derived from the script's own `DuelFinished`**, not invented:
  `{"winner":0,"handsPlayed":7,"finalStacks":[3000,0]}` is what both seats' last frame carries, so
  seat 0's row is `outcome: "WON"` with `handsPlayed: 7` and seat 1's is `outcome: "LOST"`. The duel
  the server reports is the duel that was played.
- Step 1 — the duel: `driveScriptedDuel({ viewerSeat: 0, storage: storageA })`. Assert the result
  region is on screen and that `storageA` now holds `device-seat-0`. Then `cleanup()`.
- Step 2 — the coin: `bootClient` over `storageA`, and read the balance out of the `your profile`
  region. Capture it from **the server's own record**, and assert the rendered text carries
  `coinBalanceText` of that number — never a literal typed into the test twice.
- Step 3 — the name: open `#/account`? No — the name surface is on the **first** screen. Type into
  the `your display name` region's textbox and press `Set my name`. Assert the strip then shows the
  new name.
- Step 4 — the claim: click `Account`, await the account screen, fill `Handle` and `Password` on the
  sign-up form, submit, and assert `SIGNED_UP` is on screen.
- After the claim, assert the balance and the name are **unchanged** — `ADR-0030` §1, and the story's
  note that a claim names no column of `player`.

## Out of scope

- Anything browser B does — `TASK-041408`.
- Asserting the duel through the history screen at `#/duels`. The profile strip carries the duel line
  and this half only needs the coin; `TASK-041408` is where a duel is matched by identity.
- Re-proving the duel plays correctly. `STORY-0312`'s eighteen merged tests own that; this ticket
  asserts only that it finished and what it left in storage.

## Tests

`claimed-here-recovered-there.test.tsx`

| Test | Proves |
| --- | --- |
| `plays a duel anonymously and reads back the coin the server sent` | Steps 1 and 2. The rendered balance equals the server's `coinBalance` for `player-seat-0`, and is asserted **not equal** to `player-seat-1`'s, so the strip is showing this player's number rather than the only number there is. |
| `names the profile and then claims it, and the claim moves neither` | Steps 3 and 4. The name reaches the strip; after the claim the name **and** the balance are byte-identical to what they were before it, read from the DOM both times. |

Selectors, all merged and real: the strip is `getByLabelText("your profile")`; the name region is
`getByLabelText("your display name")` and its control is the button `Set my name`; the sign-up form
is `getByLabelText("sign up for an account")` with labels `Handle` and `Password` and the button
`Give this profile a password`. Take the label strings from `account-text.ts` constants, not as
literals — `SIGN_UP_LABEL`, `HANDLE_LABEL`, `PASSWORD_LABEL`, `SIGNED_UP`, `ACCOUNT_HEADING`.

## Acceptance criteria

- [ ] `claimed-here-recovered-there.test.tsx` `plays a duel anonymously and reads back the coin the server sent` passes
- [ ] `claimed-here-recovered-there.test.tsx` `names the profile and then claims it, and the claim moves neither` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qE 'Tests +2 passed \(2\)'` exits 0
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'` exits 0
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

1. Give both fixture players the **same** `coinBalance`: `plays a duel anonymously and reads back the
   coin the server sent` must redden on its not-equal half. A green run means the balance assertion
   cannot tell the two players apart, and `TASK-041408`'s whole claim rests on it being able to.
2. Make the fake sign-up handler set `coinBalance` to a different number: `names the profile and then
   claims it, and the claim moves neither` must redden. That is the only assertion standing between
   this story and a claim that silently moves a coin.
3. Skip step 1 — boot over an empty storage instead of the one the duel wrote — and confirm the test
   reddens with `No profile yet.` rather than passing. If it passes, the boot is not reading the
   device id the duel minted and steps 1 and 2 are not connected.
4. Confirm the balance literal appears in the test **once**, as the fixture's value, and that the
   assertion compares the DOM to that binding. Two literals is the failure mode that survives every
   mutation above.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
