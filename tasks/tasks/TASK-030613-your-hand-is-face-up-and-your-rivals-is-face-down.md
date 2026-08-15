---
schema: 2
id: TASK-030613
title: Your hand is face up and your rival's is face down
type: task
status: done
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, duel, ui, secrecy]
depends_on: [TASK-030612]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +182 passed \(182\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'draws your two cards face up and your rival'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'cards face up when the view carries them'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hand two places wide'
  - cd web-client && npm run check
---

## Goal

The story's first two acceptance criteria: your two cards face up, your rival's two card backs — and
four faces the moment a `Snapshot` arrives with the rival's `holeCards` populated, with the table
never asking why.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify — wrap each plate in its block and add a hand row |
| `web-client/src/table/DuelTable.test.tsx` | modify — append three `it` blocks |
| `web-client/src/table/Hand.tsx` | read — the `Hand` props |
| `design/screens/duel-table.html` | read — the `.opp`, `.oppcards` and `.hole` rules. **Read only: never edit anything under `design/`** |

## Scope

- `import { Hand } from "./Hand";` joins the imports.
- The rival's plate is wrapped in the design's `.opp` block, with the hand row beneath it:

  ```tsx
        {rival !== undefined && (
          <div className="flex flex-col gap-2">
            <SeatPlate
              name="Your rival"
              seat={rival}
              hasButton={view.buttonSeat === rival.index}
              isToAct={view.seatToAct === rival.index}
              isViewer={false}
            />
            <div className="flex justify-center gap-2 [--w:40px]">
              <Hand
                cards={rival.holeCards}
                hiddenLabel="your rival's hidden hand"
              />
            </div>
          </div>
        )}
  ```

- **The order is asserted, not just written.** Yours is cards above the plate and your rival's is
  cards below theirs — both hands toward the board. Label and text queries cannot see arrangement:
  moving each hand to the far side of its plate ships `Tests 182 passed (182)` against every content
  assertion in the file. `draws your two cards face up and your rival's face down` therefore also
  asserts document position, with a `contains()` guard on each so a wrapper `div` cannot satisfy it
  by holding the hand. Verified, not predicted.
- Yours is the design's `.hero` block, cards **above** the plate:

  ```tsx
        {you !== undefined && (
          <div className="flex flex-col gap-4">
            <div className="flex justify-center gap-3 [--w:96px]">
              <Hand cards={you.holeCards} hiddenLabel="your hidden hand" />
            </div>
            <SeatPlate
              name="You"
              seat={you}
              hasButton={view.buttonSeat === you.index}
              isToAct={view.seatToAct === you.index}
              isViewer
            />
          </div>
        )}
  ```

- The two reference widths are the design's: `96px` for your hand, `40px` for the rival's minis.
  `--w` is set on the row and inherited, so no card takes a size prop.
- **Both rows go through the same `Hand`.** Your own cards are not a special case: between hands the
  server sends you an empty `holeCards` too, and the rule "a place with no card is a back" is what
  keeps that from looking like a duel with no cards in it.
- Neither row consults `hasFolded` or `isAllIn`. A folded seat is shown folded on its plate; its
  hand keeps its two places, which is why the fold frame in `duel-table-states.html` still draws two
  cards.

## Out of scope

- The dimmed `.back.mucked` treatment the states screen gives a folded or mucked hand. It needs to
  know a hand ended without a reveal, which no `PlayerView` field says — `STORY-0308`.
- A reveal animation or a flip. `STORY-0306` scopes animation out in as many words.
- The bet line — `TASK-030614`.

## Tests

`web-client/src/table/DuelTable.test.tsx`, describe block `"the duel table"`. Three `it` blocks
appended after `TASK-030612`'s four, which are not edited.

| Test | Proves |
| --- | --- |
| `draws your two cards face up and your rival's face down` | with `viewerSeat: 0`, `seatToAct: 1`, your `holeCards` `["Ah", "Ks"]` and the rival's `[]`: `ace of hearts`, `king of spades` and `your rival's hidden hand` are all findable |
| `turns your rival's cards face up when the view carries them` | with the rival's `holeCards` `["7c", "7h"]`, `seven of clubs` and `seven of hearts` are findable and `your rival's hidden hand` is `null` |
| `keeps a folded rival's hand two places wide` | with the rival `hasFolded: true` and `holeCards: []`, `Folded` is on screen **and** `your rival's hidden hand` is still findable |

Three tests. One hundred and seventy-nine exist, so the suite reports **182**.

**`seatToAct: 1` in the first test is load-bearing.** With the fixture's default `seatToAct: 0` the
test passes even against an implementation that hides your own cards unless it is your turn — the
turn happened to be yours. Measured, not reasoned about.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 182 passed (182)` | the three ran and the hundred-and-seventy-nine before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red.** The claim is about *whose* cards are drawn and
*when*, so the edits break each of those separately. All three were run against this exact test file:

1. Draw the rival's hand from the wrong seat — `cards={view.seats.at(0)?.holeCards ?? []}` →
   `draws your two cards face up and your rival's face down` fails with `Found multiple elements
   with the role "img" and name "ace of hearts"`, and `turns your rival's cards face up when the
   view carries them` with `Unable to find an accessible element with the role "img" and name
   "seven of clubs"`. Revert.
2. Show your own cards only on your turn — `cards={view.seatToAct === you.index ? you.holeCards :
   []}` → `draws your two cards face up and your rival's face down` fails with `Unable to find an
   accessible element with the role "img" and name "ace of hearts"`. Revert.
3. Skip the rival's hand when it folded — wrap the `<Hand>` in `{!rival.hasFolded && (…)}` →
   `keeps a folded rival's hand two places wide` fails with `Unable to find an accessible element
   with the role "img" and name "your rival's hidden hand"`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel table > draws your two cards face up and your rival's face down` passes
- [ ] `the duel table > turns your rival's cards face up when the view carries them` passes
- [ ] `the duel table > keeps a folded rival's hand two places wide` passes
- [ ] `TASK-030612`'s four `it` blocks are unedited and their assertions are byte-identical
- [ ] The two `<Hand …>` elements' `cards` props read exactly `rival.holeCards` and `you.holeCards`
      — no ternary, no `&&`, no other field anywhere inside either element
- [ ] `npm run --silent test` reports `Tests  182 passed (182)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
