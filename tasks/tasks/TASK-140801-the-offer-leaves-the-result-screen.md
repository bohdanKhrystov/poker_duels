---
schema: 2
id: TASK-140801
title: The offer leaves the result screen
type: task
status: done
parent: STORY-1408
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - web-client `npm run typecheck` — deleting `DuelResult`'s `offer` prop *before* its construction sites stop passing one fails every site at once, and `DuelResult.test.tsx` is one of them. This forbids one order, not the grouping; see *Atomicity, corrected at landing*
  - web-client `npm run lint` — `Lobby.tsx`'s five offer imports become unused the moment the prop stops being passed
  - web-client `npm run test` — three merged tests in `Lobby.test.tsx` and three in `drive-arc.test.tsx` assert the offer renders
labels: [client, account, result]
depends_on: []
verify:
  - cd web-client && npm ci
  - sh -c '! grep -qF "offer" web-client/src/result/DuelResult.tsx'
  - sh -c '! grep -qF "offerAccount" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "AccountOffer" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "offerSettledHere" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "settleOfferHere" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "the offer" web-client/src/lobby/Lobby.test.tsx'
  - sh -c '! grep -qF "offerWiring" web-client/src/lobby/Lobby.test.tsx'
  - sh -c '! grep -qF "account-offer" web-client/src/lobby/Lobby.test.tsx'
  - sh -c '! grep -qF "ADR-0116" web-client/src/lobby/Lobby.test.tsx'
  - sh -c '! grep -qF "OFFER_" web-client/src/e2e/drive-arc.test.tsx'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a win shows the verdict with no account offer beside it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the attach form on the account screen, wired to the account seam'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

No screen in this product offers an account after a duel. The result screen shows the verdict, the
coin, `Rematch` and `Back to the lobby`, and nothing else — `ADR-0125` §1 and §2.

## Files

Five, **probed not remembered** (`ADR-0069`, `ADR-0070`): the offer was cut from `Lobby.tsx` and the
prop from `DuelResult.tsx` in one tree, and the client gate set from `.github/workflows/build.yml` —
`npm ci`, `npm run check`, `npm run build` — was run until it exited `0`. `check` runs typecheck,
lint, format and test in that order, so rows 3–5 were invisible until the two before them were
green, which is exactly the prefix `ADR-0070` warns about.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify | The `offer?: ReactNode` prop and `{props.offer}`. Removing the prop is what makes every other row fail |
| `web-client/src/result/DuelResult.test.tsx` | modify | Two tests pass `offer={…}` and fail `TS2322` — *Property 'offer' does not exist* — on `npm run typecheck` |
| `web-client/src/lobby/Lobby.tsx` | modify | The only site that builds the offer. Its five imports (`AccountOffer`, `offerAccount`, `verdictOf`, `offerSettledHere`, `settleOfferHere`) and the `offerSettled` state go with it, or `eslint --max-warnings 0` fails `TS6133`/`no-unused-vars` |
| `web-client/src/lobby/Lobby.test.tsx` | modify | Three merged tests assert the offer renders and would go red; and the file's `puts the attach form…` test fails once the third of them is gone — see *The order dependence* below |
| `web-client/src/e2e/drive-arc.test.tsx` | modify | Three whole-client arc cases assert the offer, and the `OFFER_ACCEPT`/`OFFER_DISMISS` import is unused without them |

Read, and do not edit:
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§1 and §2 — the list of what goes and the refusal to replace it;
`web-client/src/result/AccountOffer.tsx` — the surface, deleted by `TASK-140802` and **not by this
ticket**; `web-client/src/virtual-time.test.ts` — the merged gate named below.

## Scope

- Delete `DuelResult`'s `offer` prop and the `{props.offer}` slot. `rematch`, `onLeave` and every
  other prop are untouched.
- Delete the whole `offer={…}` argument from `Lobby.tsx`'s `<DuelResult …>`, the
  `const [offerSettled, setOfferSettled] = useState(offerSettledHere)` line and its comment, and the
  five imports that then have no reader. `verdictOf` is one of them — it is imported **only** for the
  offer's `verdict` term. Nothing else in `Lobby.tsx` changes.
- In `DuelResult.test.tsx`, delete the two tests that pass an `offer` prop
  (`puts the offer it is handed between the rematch and the way back` and
  `disables nothing it already carried when it carries an offer`). **Keep `adds no offer of its
  own`** — after this ticket it is the standing proof of §2, and its two assertions still hold.
- In `Lobby.test.tsx`, delete the three offer tests
  (`offers an account after a win, and after nothing else`,
  `withholds the offer from a browser that answered, and from one holding a credential`,
  `answers from either control, and only Not now takes the offer off the screen`); reduce the hoisted
  wiring object to its one surviving field and **rename it `signedInWiring`**, since it no longer
  wires an offer; drop the two `../main` mock keys and the two `beforeEach` resets that fed them; and
  rewrite the three comments whose subject is gone — the wiring object's, `renderFinishedDuel`'s
  *"Only the three account-offer tests below call this"* (it still has one caller), and the
  `ADR-0116` §6 block above the held-`FINISHED`-room test. `ADR-0125` supersedes `ADR-0116` **in
  full**; the test it annotates is `ADR-0112`'s and stays.
- In `drive-arc.test.tsx`, delete the three offer cases and the `account-offer-text` import, keep
  `bootAndWin` and rewrite its KDoc (its second sentence is about the offer's three terms), and add
  the one replacement case below.

## Atomicity, corrected at landing

The `atomic:` block above claimed five files that cannot be fewer. **They can be**, and the
review measured it: revert `DuelResult.tsx` to `develop`, keep the other four files' changes,
and `cd web-client && npm run check` still exits 0 — typecheck, lint, format and all 1176 tests.

The reason is one character. `DuelResult`'s prop is declared `offer?: ReactNode` — **optional**.
A commit that stops *passing* the offer therefore leaves `DuelResult.tsx` compiling untouched and
rendering nothing into the slot, so a legal **4-then-1** split exists and the gates named above do
not forbid it.

What those gates *do* forbid is the other order: removing the prop before its construction sites
stop passing one breaks every site in the same commit. That is a constraint on sequence, not on
grouping, and the original bullet stated it as though the two were the same thing.

The work landed as one commit anyway — five files is small, and splitting a removal into a
dead-slot commit and a cleanup commit would put a state on `develop` whose only description is
"halfway through deleting something". That is a **choice**, made here in the open, and no longer a
necessity the ticket asserts. Nobody should cite this ticket as an example of an earned `atomic:`.

## The order dependence this ticket inherits

Measured on `develop` at `9dd8571c` with **production sources untouched**: deleting only
`answers from either control, and only Not now takes the offer off the screen` makes
`puts the attach form on the account screen, wired to the account seam` fail with *Unable to find a
label with the text of: Email address*. The account screen renders — `findByText(/Recovery is/)`
resolves — and then the address drops its `#/account` fragment and the tree returns to the front
door and stays there.

It is jsdom, not the product: an `<a href="/">` click in an earlier test leaves a **refused
navigation that jsdom retires in a later task**, and that task drops a fragment set before rendering.
The deleted test absorbs it today. `window.history.replaceState` is never called during the failing
test — verified with a spy — so nothing in `use-screen.ts` is at fault.

**The repair, verified against the full gate set while this ticket was written:** in that one test,
stop staging `window.location.hash = "#/account"` before `render`, and reach the screen through the
product's own door instead — `await screen.findByRole("button", { name: ACCOUNT_HEADING })`, click
it, then `await screen.findByLabelText("Email address")` rather than `getByLabelText`. Awaiting the
front door is what retires the stray navigation; the press that follows is what puts `#/account` up.
Leave a comment saying so.

**A literal `setTimeout` is not available.** `web-client/src/virtual-time.test.ts` fails any test file
naming `setTimeout`, `setInterval` or `requestAnimationFrame` without a `vi.useFakeTimers(` call, and
`Lobby.test.tsx` has none — the string appears there only inside a comment, which that gate's regex
does not accept.

## Out of scope

- Deleting `AccountOffer.tsx`, `account-offer.ts`, `account-offer-text.ts`,
  `account-offer-settled.ts` or their tests, and removing `offerSettledHere`/`settleOfferHere` from
  `main.tsx` — `TASK-140802`. After this ticket they compile, pass and are simply unreached.
- `ArcWiring`'s `offerSettled`/`settleOffer` fields in `drive-arc.tsx`, and the mock keys in
  `claimed-here-recovered-there.test.tsx` and `App.test.tsx` — `TASK-140802` and `TASK-140803`.
- `ProfileStrip`'s `No profile yet.` — `TASK-140804`.
- Anything that points a player from the result screen at the account screen. `ADR-0125` §2 forbids
  it: *"a pointer on the result screen is the struck block in a smaller font."*

## Tests

`DuelResult.test.tsx` — no new test. `adds no offer of its own` survives unchanged and is what proves
§2 at the panel.

`Lobby.test.tsx` — no new test. The three offer tests go; `puts the attach form on the account
screen, wired to the account seam` keeps every assertion it has, in the same order, and only how it
reaches the account screen changes.

`drive-arc.test.tsx`

| Test | Proves |
| --- | --- |
| `a win shows the verdict with no account offer beside it` | A whole client, booted over a real storage and a stub server, played to a win: the result region is on screen and no region named `the offer` is. Two assertions, not one — the result's presence is what makes the offer's absence a withheld offer rather than an empty screen |

## Acceptance criteria

- [ ] `a win shows the verdict with no account offer beside it` appears in the verbose test report
- [ ] `puts the attach form on the account screen, wired to the account seam` appears in the verbose
      test report, and `npm run check` exits 0 — so it passed
- [ ] `DuelResult.test.tsx` still contains `adds no offer of its own`, with both of its assertions
- [ ] `grep -F "offer" web-client/src/result/DuelResult.tsx` finds nothing
- [ ] `grep -F "offerAccount"`, `"AccountOffer"`, `"offerSettledHere"` and `"settleOfferHere"` each
      find nothing in `web-client/src/lobby/Lobby.tsx`
- [ ] `grep -F "the offer"`, `"offerWiring"`, `"account-offer"` and `"ADR-0116"` each find nothing in
      `web-client/src/lobby/Lobby.test.tsx`
- [ ] `grep -F "OFFER_"` finds nothing in `web-client/src/e2e/drive-arc.test.tsx`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
