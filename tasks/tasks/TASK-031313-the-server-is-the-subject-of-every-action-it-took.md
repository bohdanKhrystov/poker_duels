---
schema: 2
id: TASK-031313
title: The server is the subject of every action it took
type: task
status: backlog
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, presence, copy]
depends_on: [TASK-031312]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +623 passed \(623\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the server acting for your rival, whichever seat the rival is'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the server acting for you, whichever seat you are'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names an absent seat when this client holds none'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'uses the verb the frame carried, and no other'
  - cd web-client && npm run check
---

## Goal

One pure function turns an `ActedForAbsent` and this client's own seat into one of `ADR-0046` §4's
six sentences, every one of which has the server as its subject.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/absent-action-text.ts` | create |
| `web-client/src/table/absent-action-text.test.ts` | create |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read — §4, the table and the four rules under it |

## Scope

- One exported function, and nothing else in the file:

  ```ts
  import type { ActedForAbsent } from "../protocol";

  /**
   * What the server did for an absent seat, in `ADR-0046` §4's words.
   *
   * The subject is always the server. `The server folded…` is not a claim about the rival;
   * `Your rival folded…` is, and a reader who stops after the verb has then been told something
   * false. Nothing here says why the seat is absent — the presence line already says as much as
   * is known, and repeating it on every action turns a fact into an accusation.
   */
  export function absentActionText(
    mark: ActedForAbsent,
    mySeat: number | null,
  ): string
  ```

- The verb is the past tense of the action the frame carried and nothing else: `FOLD` → `folded`,
  `CHECK` → `checked`. Those are the only two the type can carry — the Kotlin `init` refuses the
  rest — so the switch has exactly two arms plus a `default` that returns `""`, never a guess.
- The subject clause is chosen from `mark.seat` against `mySeat`:
  `mySeat === null` → `an absent seat`; `mark.seat === mySeat` → `you`; otherwise → `your rival`.
  **`mySeat === null` is tested first**, because `mark.seat === null` is never true and a null seat
  compared against `0` would silently answer *your rival*.
- The six strings are written as literals: `The server folded for your rival.`,
  `The server checked for your rival.`, `The server folded for you.`,
  `The server checked for you.`, `The server folded for an absent seat.`,
  `The server checked for an absent seat.`
- Refused by name and absent from this file: `Your rival folded`, `(timed out)`, `(away)`,
  `auto-fold`, `auto-check`, `default fold`, `timeout fold`.

## Out of scope

- Where the sentence goes, and how long it stays. `TASK-031314` records the mark and `TASK-031315`
  renders it; **what takes it off the screen is
  [`ADR-0075`](../../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md)**
  — an `OpponentPresence` carrying `PRESENT`, a `DuelFinished`, and nothing else. This file is a
  pure function of a frame and a seat, and knows none of it.
- Matching the mark to an event in `narration`. `ADR-0046` §4 requires no action log and this story
  builds none; the mark carries `(handNumber, actionSequence)` so that a client *can* attach it by
  coordinate, and nothing here depends on the order it arrived in.
- Any presence string. `presence-text.ts` owns those three.

## Tests

`web-client/src/table/absent-action-text.test.ts`, one describe block: `"an action the server
took"`. A small local `aMark(overrides)` builds an `ActedForAbsent` carrying every field the wire
declares, in `view-fixture.ts`'s tradition, so a hand-written literal cannot miss one.

**Every subject case is driven from both seats.** A seat has two wrong answers: a function that
answered *your rival* whenever `mark.seat === 1` agrees with the right one at
`(seat 1, mySeat 0)` and disagrees at `(seat 0, mySeat 1)`, and a pair whose expected values
differ is the only thing that closes both.

`an action the server took`

| Test | Proves |
| --- | --- |
| `names the server acting for your rival, whichever seat the rival is` | `(seat 1, mySeat 0)` and `(seat 0, mySeat 1)` both give `The server folded for your rival.` |
| `names the server acting for you, whichever seat you are` | `(seat 0, mySeat 0)` and `(seat 1, mySeat 1)` both give `The server folded for you.` |
| `names an absent seat when this client holds none` | `(seat 0, mySeat null)` and `(seat 1, mySeat null)` both give `The server folded for an absent seat.` — the null branch wins over the seat comparison, which `seat 0` against a null `mySeat` would otherwise lose |
| `uses the verb the frame carried, and no other` | with `action: "CHECK"`, the same six inputs give `The server checked for your rival.`, `The server checked for you.` and `The server checked for an absent seat.`. And `handNumber` and `actionSequence` are set to `3` and `7` in one case and `41` and `2` in another with the same expected sentence, so a function that leaked a coordinate into the copy fails |

Four tests. Six hundred and nineteen exist after `TASK-031312`, so the suite reports **623**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 623 passed (623)` | four ran and the six hundred and nineteen before them still do |
| the four `--reporter=verbose` greps | each exists by name |
| `npm run check` | `ActedForAbsent` typechecks and `mySeat: number \| null` is honoured at every branch |

**Name the edit that makes each assertion red:**

1. Compare `mark.seat === 0` instead of `mark.seat === mySeat` → `names the server acting for your
   rival, whichever seat the rival is` fails on its `(seat 0, mySeat 1)` half and passes its
   `(seat 1, mySeat 0)` half. **Run both directions**: only one of them fails, and running the one
   that fails on its own would look like a proof the pair is unnecessary.
2. Move the `mySeat === null` branch below the seat comparison → `names an absent seat when this
   client holds none` fails on its `(seat 0, mySeat null)` half with `The server folded for your
   rival.`, because `0 === null` is false. Revert.

Quote both in the PR, with which half survived each.

## Acceptance criteria

- [ ] `an action the server took > names the server acting for your rival, whichever seat the rival is` passes
- [ ] `an action the server took > names the server acting for you, whichever seat you are` passes
- [ ] `an action the server took > names an absent seat when this client holds none` passes
- [ ] `an action the server took > uses the verb the frame carried, and no other` passes
- [ ] Every one of the three subject cases is asserted at `seat 0` **and** at `seat 1`
- [ ] `absent-action-text.ts` exports exactly one name, `absentActionText`
- [ ] `absent-action-text.ts` contains none of `auto`, `timed out`, `(away)` or the word `rival` as
      a grammatical subject — every sentence begins `The server `
- [ ] `npm run --silent test` reports `Tests  623 passed (623)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
