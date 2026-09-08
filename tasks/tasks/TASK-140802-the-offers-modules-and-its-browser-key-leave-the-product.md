---
schema: 2
id: TASK-140802
title: The offer's modules and its browser key leave the product
type: task
status: done
parent: STORY-1408
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 13
atomic:
  - web-client `npm run typecheck` — `main.tsx` and `drive-arc.tsx` import a module that no longer exists, and `ArcWiring` losing two fields fails both harnesses that build one
  - web-client `npm run test` — `one-module-owns-each-storage-key.test.ts` asserts the key's owner is `account-offer-settled.ts`, and a row left behind after its module is deleted matches nothing and goes red naming a file that no longer exists (`ADR-0125` §1)
labels: [client, account, storage]
depends_on: [TASK-140801]
verify:
  - cd web-client && npm ci
  - sh -c 'ls web-client/src/result | grep -qivE "account-offer|AccountOffer"'
  - sh -c '! ls web-client/src/result | grep -qiE "account-offer|AccountOffer"'
  - sh -c '! grep -rqF "accountOfferSettled" web-client/src'
  - sh -c '! grep -rqF "account-offer" web-client/src'
  - sh -c '! grep -rqF "AccountOffer" web-client/src'
  - sh -c '! grep -rqF "offerSettledHere" web-client/src/main.tsx web-client/src/e2e web-client/src/lobby'
  - sh -c '! grep -rqF "settleOfferHere" web-client/src/main.tsx web-client/src/e2e web-client/src/lobby'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The four modules the post-win offer was built from, the browser key that remembered its answer, and
the gate row that owned that key all leave the product in one diff — `ADR-0125` §1.

## Files

Thirteen, **probed not remembered** (`ADR-0069`, `ADR-0070`): the eight modules were deleted in one
tree on top of `TASK-140801`, and the client gate set from `.github/workflows/build.yml` — `npm ci`,
`npm run check`, `npm run build` — was run until it exited `0`. `npm run check` runs typecheck, lint,
format and test in that order, so the two test rows were invisible until the type errors above them
were cleared.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `web-client/src/result/AccountOffer.tsx` | delete | The surface (`ADR-0125` §1) |
| `web-client/src/result/AccountOffer.test.tsx` | delete | Its proof; it imports the deleted module and fails `TS2307` otherwise |
| `web-client/src/result/account-offer.ts` | delete | `offerAccount` and its three terms |
| `web-client/src/result/account-offer.test.ts` | delete | Same `TS2307` |
| `web-client/src/result/account-offer-text.ts` | delete | The four strings, including *Your duel coins are only in this browser* |
| `web-client/src/result/account-offer-text.test.ts` | delete | Same `TS2307` |
| `web-client/src/result/account-offer-settled.ts` | delete | The key `pd.accountOfferSettled` and its `"1"` sentinel |
| `web-client/src/result/account-offer-settled.test.ts` | delete | Same `TS2307` |
| `web-client/src/main.tsx` | modify | Imports `readOfferSettled`/`markOfferSettled` — `TS2307` — and exports the two functions and the `offerStorage` binding that read them |
| `web-client/src/e2e/drive-arc.tsx` | modify | Same `TS2307`; `ArcWiring` loses `offerSettled` and `settleOffer`, and `bootClient` stops writing them |
| `web-client/src/e2e/drive-arc.test.tsx` | modify | Builds an `ArcWiring` literal and fails `TS2353`, and its `../main` mock names two exports that are gone |
| `web-client/src/e2e/claimed-here-recovered-there.test.tsx` | modify | The same `TS2353` and the same two mock keys, in the second harness |
| `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` | modify | `only the account-offer-settled module writes the offer-settled key` scans production sources for `pd.accountOfferSettled` and expects `["account-offer-settled.ts"]`; with the module gone the scan answers `[]` and the row reddens naming a deleted file |

Read, and do not edit:
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§1 — the list, and the sentence about the gate row going in the same diff;
`web-client/src/App.test.tsx` — it also names the two `main.tsx` exports in a mock, but nothing fails
if it does not change, so it is `TASK-140803` and **not this ticket's**.

## Scope

- Delete the eight files above. Nothing is moved, renamed or re-homed.
- In `main.tsx`, delete the `account-offer-settled` import, the `offerStorage` binding and its
  comment, and the two exported functions `offerSettledHere` and `settleOfferHere` with their KDoc.
  The `signedIn` module-scope read beside them is untouched.
- In `drive-arc.tsx`, delete the import, the two `ArcWiring` fields and the two `bootClient`
  assignments, and correct the two counts its KDoc states in words (*"four bindings"*, *"all four
  fields"*) — after this there are two of each.
- In `drive-arc.test.tsx` and `claimed-here-recovered-there.test.tsx`, drop the two wiring fields
  from each `vi.hoisted` literal and the two `../main` mock keys from each factory. Nothing else in
  either file changes.
- In `one-module-owns-each-storage-key.test.ts`, delete the whole
  `only the account-offer-settled module writes the offer-settled key` case. The three other key
  cases stay exactly as they are.

## Atomicity, corrected at landing

The `atomic:` block above claimed thirteen files that cannot be fewer. **They can be**, and the
landing measured it rather than argued it.

- Restore `account-offer-text.ts` alone from `develop`, keep every other change:
  `cd web-client && npm run check` exits **0**. A leaf module whose every consumer is deleted in
  the same commit is inert — nothing imports it, nothing lints it, nothing compiles against it.
  A legal **12-then-1** split therefore exists.
- Restore `account-offer.test.ts` alone: `npm run check` exits **2**, with
  `TS2307: Cannot find module './account-offer'`.

So what the gates forbid is **pairing**, not grouping: a test and the module it imports must move in
the same commit, and a module may not be deleted while a surviving test names it. That is a
constraint on which files travel *together*, and it does not imply that all thirteen must.

This is the second ticket in `STORY-1408` whose `atomic:` justification did not survive being
probed — `TASK-140801` was the first, for the same underlying reason: the block reasons about one
direction of a removal and states the conclusion as though it held in both. The work landed as one
commit anyway, which is a **choice** about not leaving `develop` halfway through a deletion, and is
recorded here as a choice. Nobody should cite this ticket as an example of an earned `atomic:`.

## What is deliberately not cleared

`pd.accountOfferSettled` is **orphaned, not migrated**: nothing reads it, nothing writes it and
**nothing clears it** (`ADR-0125` §1). Browsers already holding a value keep it. Do not add a
removal, a migration or a one-shot cleanup — the ADR names the orphaning as a cost it accepted, and
today every browser holding the key is the driver's own.

## Out of scope

- `web-client/src/App.test.tsx`'s two dead mock keys — `TASK-140803`. Nothing fails while they stand:
  the factory's extra keys typecheck and the file's 36 tests pass, which is why they cannot be a row
  here.
- `ProfileStrip`'s `No profile yet.` — `TASK-140804`.
- Anything under `docs/adr/` — `ADR-0085`, `ADR-0086` and `ADR-0116` stay in the repository as
  superseded records. `ADR-0125` §Consequences names the reader who finds them as the standing cost
  of supersession.

## Tests

No new test. This ticket **removes** proofs — eight modules' worth — and `ADR-0125` §Consequences
says why that is not free and why it is still right: *"what those tests pinned was a behaviour, and
after this ADR there is no behaviour to pin."* What remains standing is `DuelResult.test.tsx`'s
`adds no offer of its own` and `drive-arc.test.tsx`'s
`a win shows the verdict with no account offer beside it`, both landed by `TASK-140801`.

## Acceptance criteria

- [ ] None of `AccountOffer.tsx`, `account-offer.ts`, `account-offer-text.ts` or
      `account-offer-settled.ts` exists under `web-client/src/result/`, and neither does any of their
      four test files
- [ ] `grep -rF "accountOfferSettled" web-client/src` finds nothing
- [ ] `grep -rF "account-offer" web-client/src` finds nothing
- [ ] `grep -rF "AccountOffer" web-client/src` finds nothing
- [ ] `grep -rF "offerSettledHere"` and `grep -rF "settleOfferHere"` find nothing in `main.tsx`,
      `src/e2e/` or `src/lobby/` — `App.test.tsx` still names both, and that is `TASK-140803`'s
- [ ] `one-module-owns-each-storage-key.test.ts` still contains its three other key cases, and
      `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
