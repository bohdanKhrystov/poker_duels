---
schema: 2
id: TASK-010617
title: Extend the secrecy suite from the fold to the muck
type: task
status: ready
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, tests, security]
depends_on: [TASK-010614]
verify:
  - ./gradlew :poker-engine:test --tests '*CardSecrecyTest'
  - ./gradlew :poker-engine:check
---

## Goal

`CardSecrecyTest` asserts the rule [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md)
settled — a hand that is not shown appears in no event — for mucked hands as well as folded ones,
so the guarantee is executable *before* any reveal is ever emitted.

## Why this comes first

Nothing emits `HandRevealed` yet, so the muck scan below passes trivially today. That is the
point: it goes in before `TASK-010623` starts emitting reveals, so the emission ticket cannot
loosen the boundary without turning this suite red. The test this replaces
(`noHandIsRevealedAnywhereYet`) encodes today's no-reveal world and would block that ticket
outright.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/CardSecrecyTest.kt` | modify |

Read `RandomHandPlayer.kt`, `DealerEvents.kt` and
`docs/adr/ADR-0008-loser-mucks-at-showdown.md`. Modify none of them.

## Scope

- **Delete `noHandIsRevealedAnywhereYet`.** It asserts a temporary state of the world. Its two
  successors below are the permanent form of the same rule.
- Add `noEventCarriesAMuckedHandsCards`: over seeds `1L..1000L`, for every hand whose log
  contains `ShowdownReached`, the **shown** seats are those named by a `PotAwarded` event in that
  same log, and every other seat is mucked. No event other than a mucked seat's own
  `HoleCardsDealt` may contain either of its cards. Derive the winners from the log, not from
  `showdownWinners` — the test must not restate the logic it guards. (A tied showdown names both
  seats in `PotAwarded`: the smallest showdown pot is two blinds, so each half is at least one
  chip and no award is dropped. A tie therefore has no mucked seat, which is what ADR-0008 wants.)
- Add `aHandWonOnAFoldRevealsNothing`: for every seed whose log has **no** `ShowdownReached`, the
  log contains no `HandRevealed` at all — ADR-0008's "a player who wins because everyone else
  folded shows nothing".
- Add `theSampleContainsShowdowns`: more than 100 of the thousand seeds reach a showdown, so the
  muck scan cannot go vacuous unnoticed.
- Update the class KDoc: it currently says mucked hands are out of scope pending `DEC-004`. They
  are in scope now; cite ADR-0008.
- Keep `@Timeout(30)` on every test and keep each failure message naming its seed.

## Out of scope

- Emitting reveals at all — `TASK-010623`.
- Reveal *order* — `TASK-010622` (the rule) and `TASK-010624` (end to end at a tie).
- Any change to `cardsIn`, whose exhaustive `when` already covers `HandRevealed`.

## Tests

`CardSecrecyTest`

| Test | Proves |
| --- | --- |
| `noEventCarriesAFoldersCards` | unchanged — kept exactly as it stands |
| `theBoardNeverShowsAFoldersCards` | unchanged — kept exactly as it stands |
| `theSampleContainsFolds` | unchanged — kept exactly as it stands |
| `noEventCarriesAMuckedHandsCards` | a showdown seat that was awarded no chips has neither of its cards in any event but its own `HoleCardsDealt` |
| `aHandWonOnAFoldRevealsNothing` | a hand that never reached showdown carries no `HandRevealed` |
| `theSampleContainsShowdowns` | more than 100 of the thousand hands reach a showdown |

## Acceptance criteria

- [ ] `CardSecrecyTest.noEventCarriesAMuckedHandsCards` passes
- [ ] `CardSecrecyTest.aHandWonOnAFoldRevealsNothing` passes
- [ ] `CardSecrecyTest.theSampleContainsShowdowns` passes
- [ ] `CardSecrecyTest.noEventCarriesAFoldersCards` passes with no change to its body
- [ ] `CardSecrecyTest.theBoardNeverShowsAFoldersCards` passes with no change to its body
- [ ] `CardSecrecyTest.theSampleContainsFolds` passes with no change to its body
- [ ] `noHandIsRevealedAnywhereYet` no longer exists in the file
- [ ] `cardsIn` still has no `else` branch
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
