---
schema: 2
id: TASK-131110
title: The account moves above the room, and the offer's accept has somewhere to land
type: task
status: done
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby, account]
depends_on: [TASK-131109]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 90) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/AccountOffer.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 6) }'
  - awk '/if \(shown === "account"/ { a = NR } /if \(state\.outcome !== null\)/ { b = NR } END { exit !(a && b && a < b) }' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qF "if (screen === \"account\"" web-client/src/lobby/Lobby.tsx'
  - awk '{ n += gsub(/settleOfferHere/, "&") } END { exit (n != 3) }' web-client/src/lobby/Lobby.tsx
  - grep -qF 'onAccept={settleOfferHere}' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qF "settleOfferHere" web-client/src/account/AccountScreen.tsx'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`#/account` renders the account screen over a held `WAITING` or `FINISHED` room, which is what makes
the result screen's *"Keep them with a password"* a door rather than a press that takes the offer and
delivers nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0116-the-accept-is-a-door-and-a-door-that-does-not-open-spends-nothing.md` §§1–2,
5–6 and `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §2. Nothing
else.

## Scope

- Move the `account` branch — its comment block and its `if` statement — up to sit with the four
  chosen branches already above `if (state.outcome !== null)`, and change its condition from
  `screen === "account"` to `shown === "account"`. A cut and a paste: nothing inside its JSX
  changes.
- That is the **whole** of this ticket's production change. `ADR-0116` is emphatic that it fixes
  nothing by itself: *"§1's behaviour arrives as a side effect of `ADR-0114`'s branch order landing
  in `STORY-1311`."*

## Out of scope — read this before touching the offer

- **Moving, delaying or re-siting `settleOfferHere`.** `ADR-0116` §2 says the offer is spent where
  the player is *delivered*, and its Consequences say in the same breath that **§2 has no enforcement
  of its own — it is satisfied by construction (§5)**: the offer renders only on the result screen,
  which is a `finished` standing, which `rulingOn` honours, so there is no state in which the offer
  is visible and the ask would be refused or held. `onAccept={settleOfferHere}` stays exactly where
  it is, called from the anchor's click, once.
- **Spending the offer on the account screen's own load.** `ADR-0116` explicitly forecloses it:
  the lobby's own account control reaches the same screen and would settle an offer that was never
  made (`ADR-0086` §6). Three `verify:` gates hold that line — `settleOfferHere` is named exactly
  **three** times in `Lobby.tsx` (the import, `onAccept`, and the dismiss handler, which is the
  count measured on `develop`), `onAccept={settleOfferHere}` survives literally, and the name
  appears nowhere in `AccountScreen.tsx`.
- **A line telling the player the press failed.** `ADR-0116` §Alternatives refuses it as unreachable
  copy on the quietest screen the product has.
- **`sign-in`.** `TASK-131111`.

## Tests

Two more `it` blocks in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `shows the account screen over a room whose duel has finished` | store fed `RoomJoined` + `Snapshot` + `DuelFinished`; address `#/account`; render → the `ACCOUNT_HEADING` heading is on screen, the result screen's own verdict line is **not**, and `window.location.hash` still reads `"#/account"`. This is `ADR-0116` §6's first owed fact and it is a **regression test**: red on the tree at `1a09fb46`, where `STORY-1310`'s `P5` observed the press taking the offer and reaching nothing |
| `shows the account screen over a room that is still waiting` | store fed `RoomJoined` alone; the same address → the same heading, the waiting screen's *"Waiting for your rival"* absent, hash unchanged. `ADR-0112` §5's other half, and the input that stops the first test being satisfied by a rule keyed on `outcome` |

**`ADR-0116` §6's second owed fact is already merged and needs no new test.** The last assertion of
`offers an account after a win, and after nothing else` is
`expect(offerWiring.settle).not.toHaveBeenCalled()` after three renders of the offer — that *is*
the gate that an offer rendered and not pressed is still offered afterwards, and it would go red if
it were false. Cite it in the new tests' comment rather than writing a fourth render of the same
thing.

**These merged tests must still pass unchanged, and none of their assertions moves:**
`answers from either control, and only Not now takes the offer off the screen` (still one settle per
press, and `window.location.hash` still `""` — jsdom does not follow the anchor),
`offers an account after a win, and after nothing else`,
`withholds the offer from a browser that answered, and from one holding a credential`,
`puts the attach form on the account screen, wired to the account seam`, and in `App.test.tsx`
`shows the duel to a player reading the account screen when a frame seats them` — which is the
mid-duel refusal for this screen and already covers it, so no fourth test is written here.

## Acceptance criteria

- [ ] Both tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at least
      90 tests
- [ ] `if (shown === "account"` appears before `if (state.outcome !== null)` and
      `if (screen === "account"` appears nowhere
- [ ] `settleOfferHere` is named exactly three times in `Lobby.tsx`, still as
      `onAccept={settleOfferHere}`, and nowhere in `AccountScreen.tsx`
- [ ] `App.test.tsx` reports at least 36 tests and `AccountOffer.test.tsx` at least 6
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
