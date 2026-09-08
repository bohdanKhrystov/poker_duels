---
schema: 2
id: TASK-141202
title: A gate says the drawing of a card does not depend on whether it played
type: task
status: done
parent: STORY-1412
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, table, gate]
depends_on: [TASK-141201]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/table/cards-are-drawn-alike.test.tsx 2>&1 | grep -qF "cards-are-drawn-alike.test.tsx  (5 tests)"'
  - sh -c 'cd web-client && [ -f src/table/cards-are-drawn-alike.test.tsx ] || exit 3; B=$(mktemp) && cp src/table/PlayingCard.tsx "$B" && perl -0pi -e "s|aria-label=.text.label.|aria-label={text.label + (props.card === \"As\" ? \" (winning)\" : \"\")}|" src/table/PlayingCard.tsx && ! cmp -s src/table/PlayingCard.tsx "$B" || { cp "$B" src/table/PlayingCard.tsx; exit 2; }; NO_COLOR=1 npx vitest run src/table/cards-are-drawn-alike.test.tsx >/dev/null 2>&1; rc=$?; cp "$B" src/table/PlayingCard.tsx; [ "$rc" -ne 0 ]'
  - sh -c 'cd web-client && [ -f src/table/cards-are-drawn-alike.test.tsx ] || exit 3; B=$(mktemp) && cp src/table/PlayingCard.tsx "$B" && perl -0pi -e "s|text.isRed \? \"text-suit-red\" : \"text-suit-black\"|props.card === \"As\" \? \"text-suit-red ring-2\" : text.isRed \? \"text-suit-red\" : \"text-suit-black\"|" src/table/PlayingCard.tsx && ! cmp -s src/table/PlayingCard.tsx "$B" || { cp "$B" src/table/PlayingCard.tsx; exit 2; }; NO_COLOR=1 npx vitest run src/table/cards-are-drawn-alike.test.tsx >/dev/null 2>&1; rc=$?; cp "$B" src/table/PlayingCard.tsx; [ "$rc" -ne 0 ]'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qF "no-derivation.test.tsx  (8 tests)"'
  - git diff --exit-code -- web-client/src/table/PlayingCard.tsx web-client/src/table/Hand.tsx web-client/src/table/BoardCards.tsx web-client/src/table/card-text.ts web-client/src/table/no-derivation.test.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0126` §1's property — **the drawing of a card does not depend on whether it played** — becomes
a test that fails when a card is picked out, in the client's own suite, where `build.yml`'s `client`
job already runs it and no workflow file has to be edited.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/cards-are-drawn-alike.test.tsx` | create |

Read, do not edit — these are the sources the gate reads and must leave byte-unchanged:
`web-client/src/table/PlayingCard.tsx`, `web-client/src/table/Hand.tsx`,
`web-client/src/table/BoardCards.tsx`, `web-client/src/table/card-text.ts`,
`web-client/src/table/view-fixture.ts`.

## Scope

- One new vitest file. **No production file changes** — the last `verify:` line is
  `git diff --exit-code` over the five it must not touch, and `no-derivation.test.tsx` is among
  them because `ADR-0126` §2 requires it *"byte-unchanged"*.
- Build a completed-showdown `PlayerView` from `aView`/`aSeat` in `view-fixture.ts`:
  `street: "SHOWDOWN"`, a five-card `board.cards`, and **two** `holeCards` on **each** seat, so nine
  cards are face up. Deal a deliberate mix of red and black suits and of ranks — `card-text.ts`
  gives red to `h`/`d` and black to `s`/`c`, so a fixture of one colour could not tell the
  normalisation from a constant.
- Write one helper, `drawingSignature(el: Element): string`, and export nothing from the file.
  The signature is the element's tag name, its attributes, its class tokens and the recursive
  signature of its children, with exactly three normalisations and no others:
  1. the `aria-label` attribute is dropped (it is checked separately, in its own test);
  2. the class tokens `text-suit-red` and `text-suit-black` are dropped, and the rest are sorted;
  3. every text node is replaced by a single placeholder character, so a rank or suit glyph is
     invisible to it.
  Everything else — every other attribute name and value, every other class token, the child
  count and the child order — is part of the signature.
- Collect the face-up cards by `getAllByRole("img")` filtered to elements whose `aria-label` is a
  label `card-text.ts` produces for one of the nine dealt cards. `CardSlot` and the labelled
  `CardBack` also carry `role="img"`, so the filter is what keeps a dashed slot out of the set —
  and there are none in a completed showdown, which is why the count assertion is nine and not
  "at least two".

## Out of scope

- **Face-down cards.** A back is what a card *is* under `ADR-0120` §1's table, and the card's
  `back mucked` treatment is `ADR-0008`'s statement about a hand that never reached showdown,
  already gated per frame by `design/check-frame-cards.sh`. This gate reads face-up cards only.
- **Changing `PlayingCard.tsx`, `Hand.tsx`, `BoardCards.tsx` or `card-text.ts`.** The gate is
  expected to be **green on the day it lands** — `ADR-0126` §Consequences: *"a gate must be written
  to hold a nothing."* A coder who finds themselves editing a component here has misread the
  ticket.
- **`no-derivation.test.tsx`.** `ADR-0126` §2: it *"stands byte-unchanged and stays green without
  being consulted, because §1 renders no text at all."*
- **Any assertion about the hand's name.** That is `ADR-0095` §3's, applied and not reopened.
- The card's margin line — `TASK-141201`.

## Tests

`web-client/src/table/cards-are-drawn-alike.test.tsx`, exactly **5** tests.

| Test | Proves |
| --- | --- |
| `the showdown fixture puts nine face-up cards on the table, from either seat` | the count control: **9** face-up cards found with `viewerSeat: 0` and **9** with `viewerSeat: 1`. Without this, every assertion below passes vacuously against a table that drew nothing |
| `every face-up card at a completed showdown is drawn the same` | with `viewerSeat: 0`, all nine `drawingSignature` values are equal — asserted as `new Set(signatures).size === 1` **and** as an explicit equality against `signatures[0]`, so a helper returning `undefined` for everything is not a pass |
| `the same holds when the viewer is the other seat` | the identical assertion with `viewerSeat: 1`. One seat cannot tell a general rule from a constant |
| `the signature sees a class, a data attribute, a title and a style, and sees nothing in an untouched copy` | the four mutation controls, plus their own control. An untouched `cloneNode(true)` of a real rendered card must have a signature **equal** to the original's — otherwise a later difference could come from cloning — and a clone that gains `classList.add("ring-2")`, `setAttribute("data-winning","true")`, `setAttribute("title","the winning five")` or `setAttribute("style","opacity:0.5")` must each have a signature **different** from it. Four marks from `ADR-0126` §1's own list, one assertion each |
| `no face-up card's label says more than its own rank and suit` | for each of the nine, `el.getAttribute("aria-label")` equals **exactly** `cardText(card)!.label` for the card that place or seat was dealt, and the sorted multiset of the nine labels equals the sorted multiset of the nine dealt cards' labels. This is the one hole the signature cannot close, because two cards legitimately carry different labels, and `ADR-0126` §1 names its own bound: *"anything in an `aria-label` beyond the rank and suit `card-text.ts` already produces"* |

## What would still pass if the coder were wrong

- **A `drawingSignature` that returns a constant** passes tests 2 and 3 and fails test 4, whose
  four sub-assertions all require the value to *change*.
- **A `drawingSignature` sensitive to something legitimate** — the rank glyph, the suit glyph or
  the suit-colour class — fails tests 2 and 3 against the mixed-suit fixture, which is why the
  fixture is mixed.
- **A filter that finds no cards** passes *no two differ* vacuously and fails test 1's count of
  nine.
- **A gate that never runs the real component** would pass all five and fail both `verify:`
  mutation gates, which mutate `PlayingCard.tsx` itself: one adds `" (winning)"` to the
  `aria-label` of a single card, the other adds a `ring-2` class token to a single card. Each gate
  proves with `! cmp -s` that the substitution actually changed the file (`exit 2` otherwise),
  requires the suite to **fail**, and restores the file whether the run passed or failed. Both were
  run in this worktree at `c6a41e6d` against a stub that detects the mark, and both exited 0 with
  `git status` clean afterwards; against a missing test file they exit 3.
- **A gate that passes because it was never collected.** `npx vitest run <path>` exits 1 when the
  path matches no file — measured — and the `grep -qF "cards-are-drawn-alike.test.tsx  (5 tests)"`
  pins the absolute number rather than a suite total.

## Acceptance criteria

- [ ] `cards-are-drawn-alike.test.tsx` reports `(5 tests)` and all five pass
- [ ] The `aria-label` mutation gate exits 0 (the suite fails under the mark, and `PlayingCard.tsx`
      is restored)
- [ ] The class-token mutation gate exits 0, on the same terms
- [ ] `no-derivation.test.tsx` still reports `(8 tests)` and all pass
- [ ] `git diff --exit-code` over the five named production and test files exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
