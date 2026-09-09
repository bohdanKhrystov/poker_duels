---
schema: 2
id: TASK-141503
title: One module holds the words both rematch surfaces say
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, rematch, notice, text]
depends_on: [TASK-141502]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/design/card-text.test.ts 2>&1 | grep -qE '^ *Tests +10 passed \(10\)$'
  - awk 'index($0, "result/rematch-text.ts") { n++ } END { exit (n != 1) }' web-client/src/design/card-text.test.ts
  - awk 'index($0, "rematch-panel.html") { n++ } END { exit (n != 1) }' web-client/src/design/card-text.test.ts
  - awk 'index($0, "export const RIVAL_OFFERS =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const REMATCH_LABEL =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const DEALING_LEAD =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const DEALING_TAIL =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const ROOM_GONE =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const NOT_NOW =") { n++ } END { exit (n != 1) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "export const") { n++ } END { exit (n != 6) }' web-client/src/result/rematch-text.ts
  - sh -c '! grep -qF "account-offer-text" web-client/src/result/rematch-text.ts'
  - awk '$0 ~ /^import /{ n++ } END { exit (n != 0) }' web-client/src/result/rematch-text.ts
  - awk 'index($0, "Your rival offers a rematch") { n++ } END { exit (n != 0) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "That duel room is gone") { n++ } END { exit (n != 0) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "changes sides") { n++ } END { exit (n != 0) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "dealing hand") { n++ } END { exit (n != 0) }' web-client/src/result/RematchControl.tsx
  - awk '$0 ~ /^[[:space:]]*Rematch[[:space:]]*$/ { n++ } END { exit (n != 0) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "{RIVAL_OFFERS}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "{ROOM_GONE}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "{DEALING_LEAD}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "{DEALING_TAIL}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "{REMATCH_LABEL}") { n++ } END { exit (n != 2) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "Rematch offered — waiting for your rival") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - awk 'index($0, "<br />") { n++ } END { exit (n != 1) }' web-client/src/result/RematchControl.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/result/rematch-text.ts` exists and holds the six strings the rematch surfaces say;
`RematchControl` says them by importing them instead of spelling them, with **the same DOM and the
same behaviour**. `ADR-0138` §4: this is the half of `ADR-0123`'s *"only prose holds them together"*
that a compiler can hold instead.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/rematch-text.ts` | create |
| `web-client/src/result/RematchControl.tsx` | modify |
| `web-client/src/design/card-text.test.ts` | modify |

Read [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§4, [`ADR-0142`](../../docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) §6,
and `design/screens/rematch-panel.html` — the card `TASK-141501` and `TASK-141502` merged, which is
where every value below comes from. **Nothing outside the table above is changed**, and in
particular `web-client/src/result/RematchControl.test.tsx` is **not** opened: its 12 tests keep
their literals and are the proof that nothing moved (a test that imported these constants would
assert that a constant equals itself).

## Scope

- **Six exports, and no import.** A gate refuses any line beginning `import ` in the new file — line
  anchored, not a substring, so a comment may use the word: this module depends on nothing.

  | Constant | Value |
  | --- | --- |
  | `RIVAL_OFFERS` | `Your rival offers a rematch` |
  | `REMATCH_LABEL` | `Rematch` |
  | `DEALING_LEAD` | `Rematch. The button changes sides —` |
  | `DEALING_TAIL` | `dealing hand 1…` |
  | `ROOM_GONE` | `That duel room is gone.` |
  | `NOT_NOW` | `Not now` |

  Every one is a **shipped** string: the first five are `RematchControl.tsx`'s own, and `NOT_NOW` is
  the word `ADR-0123` §4 gives the dismiss.
- **The dealing sentence is two constants, not one string with a newline in it.** `RematchControl`
  renders it across a `<br />`, so a single constant could not be rendered without splitting it back
  apart; that is `ADR-0138` §4's own reason and a gate keeps `<br />` at exactly **1**.
- **`NOT_NOW` is declared here and imported from nowhere.** `ADR-0138` §4: *share a constant when it
  is one fact stated twice; copy the word when it is two facts that happen to agree.*
  `account-offer-text.ts`'s `OFFER_DISMISS` holds the same word for the account offer and
  `ADR-0125` deletes that module with the offer it belongs to, so importing it would tie a rematch
  panel's label to a deleted file. A gate refuses the name `account-offer-text` in the new module.
  It lands here rather than in `TASK-141507`, where it is first used, so this file is written once
  and never reopened by this story.
- **`RematchControl`'s edit is imports only.** The same words, in the same nodes, in the same order,
  with the same `<br />`. No JSX node is added, removed or re-nested; no `className` changes; the
  component's five states, its `useState`, its early returns and its KDoc all stand.
- **`Rematch offered — waiting for your rival` does not move.** `ADR-0138` §4's table has four rows
  and that sentence is in none of them: it is the `mine` state, and `ADR-0123` §3 says a player's own
  standing offer follows them nowhere, so the panel never says it and there is no second surface to
  share it with. A gate pins it at **1**, still spelled in `RematchControl.tsx`.
- **`ADR-0142` §6's register gains a third pair, and this is no longer conditional.**
  `TASK-141203` **merged on 2026-09-09** as `f20d07ed`, so `web-client/src/design/card-text.test.ts`
  exists and its last test globs `web-client/src/**/*-text.ts` and fails on any module classified in
  neither `PAIRS` nor `NO_CARD`. **Creating `rematch-text.ts` without registering it reddens the
  suite.** Add, beside the two bootstrapped pairs, an `import * as rematchText from
  "../result/rematch-text";` and:

  ```ts
  {
    modulePath: "result/rematch-text.ts",
    moduleNamespace: rematchText,
    card: "rematch-panel.html",
    carded: [
      "RIVAL_OFFERS",
      "REMATCH_LABEL",
      "DEALING_LEAD",
      "DEALING_TAIL",
      "ROOM_GONE",
      "NOT_NOW",
    ],
    notCarded: {},
  },
  ```

  **All six are `carded`, and all six are on the card by construction**: `TASK-141501` pins each of
  them there with an exact count gate. `NO_CARD` is not touched. The register's third assertion
  matches a value against **one** text unit of the card — which is exactly why `ADR-0138` §4 made the
  dealing sentence two constants rather than one string with a newline in it: the card's `<br>` is a
  tag, `ADR-0142` §2 replaces every tag with the separator, and a single joined constant would match
  no unit at all.

  The file is at **7** tests today and a third pair adds three, so the gate is **10** — measured at
  `f20d07ed`, and derived from the register's own `describe.each` over `PAIRS` (three `it`s per pair
  plus one global), not guessed.

## Out of scope

- **`RematchControl.test.tsx`, `DuelResult.tsx`, `Lobby.tsx` and `design/screens/rematch-states.html`.**
  None is opened. The result screen's stale card is `TASK-141509`'s.
- **`RematchNotice`.** It does not exist yet; `TASK-141504` creates it and is the first importer of
  `RIVAL_OFFERS` and `REMATCH_LABEL` besides the control.
- **Changing any word.** Every value here is transcribed, not chosen (`ADR-0123` §4: *"No new words
  are minted"*).

## Tests

No new test file. The proof is `RematchControl.test.tsx` at **12 passing, unchanged** — measured
green on `develop` at `f20d07ed` — which asserts all four moved strings through the rendered DOM:
`getByText("Your rival offers a rematch")`, `getByRole("button", { name: "Rematch" })`,
`getByText(/The button changes sides.*dealing hand 1…/)` and
`getByText("That duel room is gone.")`.

## What would still pass if this were done wrong

A ticket that declared the six constants and left `RematchControl` spelling its literals would keep
all 12 tests green — which is why the five **zero-count** gates on `RematchControl.tsx` exist, and
why they are paired with five positive gates counting the interpolations that must have replaced
them. Neither half alone is enough: the zeros alone pass on a component that deleted the sentences,
and the interpolations alone pass on a component that has both.

`awk index()` is substring, so `Rematch` cannot be gated that way — the word appears in the KDoc, in
`RematchControl`, in `rematchStand` and in `RematchOffered`. The gate is instead a **line** that is
nothing but the word, which is how JSX renders that button's child, and which was measured at **2**
on `f20d07ed`.

## Acceptance criteria

- [ ] `rematch-text.ts` declares exactly **6** exports, named and valued as the table above, and
      contains no `import` and no reference to `account-offer-text`
- [ ] `RematchControl.tsx` contains **0** occurrences of `Your rival offers a rematch`,
      `That duel room is gone`, `changes sides` and `dealing hand`, and **0** lines that are nothing
      but the word `Rematch`
- [ ] `RematchControl.tsx` contains `{RIVAL_OFFERS}` once, `{ROOM_GONE}` once, `{DEALING_LEAD}`
      once, `{DEALING_TAIL}` once and `{REMATCH_LABEL}` twice
- [ ] `RematchControl.tsx` still contains `Rematch offered — waiting for your rival` once and
      `<br />` once
- [ ] `npx vitest run src/result/RematchControl.test.tsx` reports **12 passed (12)**
- [ ] `card-text.test.ts` carries `result/rematch-text.ts` once and `rematch-panel.html` once, and reports **10 passed (10)**
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
