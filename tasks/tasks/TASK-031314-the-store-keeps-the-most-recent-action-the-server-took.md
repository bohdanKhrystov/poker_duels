---
schema: 2
id: TASK-031314
title: The store keeps the most recent action the server took
type: task
status: blocked
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store, presence]
depends_on: [TASK-031313]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +627 passed \(627\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the mark exactly as the server sent it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a later mark replaces an earlier one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a mark survives the events that describe the same decision point'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a mark changes nothing a snapshot or a turn established'
  - cd web-client && npm run check
---

## Blocked on `DEC-070`

**The product owner's** — how long does the most recent mark stay on screen, and what takes it off?
`ADR-0046` §4 settles *which* mark (`showing the most recent one is enough`) and settles its words,
and says nothing about its lifetime. The candidates are all defensible and all different products: it
never clears; it clears on the next `Snapshot`, as `ADR-0043` clears a rejection; it clears when the
rival's presence returns to `PRESENT`; it clears on the next `YourTurn`. Left unanswered, the client
ships `The server folded for your rival.` under a table where the rival came back twenty hands ago,
beside a presence line reading `Your rival is back.` — two sentences in different tenses about the
same person.

The reducer case below is the half that is settled and is written out in full. The answering ADR
adds **one clearing rule** to it and **one test** to the file, and moves the suite count in
`verify:` by that test. Nothing already written here changes.

## Goal

`ActedForAbsent` stops falling through the reducer's `default`: the store holds the most recent
action the server took for an absent seat, exactly as the frame stated it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — one field, one reducer case |
| `web-client/src/store/duel-state.test.ts` | modify — four tests added, one line changed |
| `web-client/src/protocol/protocol.gen.ts` | read — `ActedForAbsent` is `{ type, seat, handNumber, actionSequence, action }`, already generated |

## Scope

- `DuelState` gains one field, after `rivalReturned`:

  ```ts
  /**
   * The most recent action the server took for an absent seat, or `null` if it has taken
   * none. The whole frame, kept rather than picked apart: `(handNumber, actionSequence)`
   * identifies the decision point uniquely, so a screen that ever wants to attach the mark to
   * an event can do it by coordinate rather than by the order the frames arrived in
   * (`ADR-0028` §4). `ADR-0046` §4 asks for the most recent one and no log.
   */
  readonly serverAction: ActedForAbsent | null;
  ```

- `initialState()` gains `serverAction: null`.
- One new case, beside the others:

  ```ts
  case "ActedForAbsent":
    return { ...state, serverAction: message };
  ```

- The whole message is stored, not a rebuilt object: nothing here reads `seat`, compares it to
  `mySeat` or decides a word. Turning the mark into a sentence is `absent-action-text.ts`'s job and
  it happens at render.
- `ActedForAbsent` is imported as a type from `../protocol`.

## This ticket owns the assertion its change unsettles

`starts with nothing the server has not sent` gains **exactly one line**, after `rivalReturned:
false`:

```ts
      serverAction: null,
```

The test keeps its name and its whole-object `toEqual`, so nothing is weakened.

## Out of scope

- **What clears the field.** `DEC-070`, and the answering ADR's own PR.
- Rendering. `TASK-031315`.
- Any attachment of the mark to `narration`. No action log is designed here (`ADR-0046` §4), and a
  reducer that walked the event list looking for a matching sequence would be building one.
- Choosing a word. The reducer holds a frame; `absentActionText` turns it into a sentence.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Four added, one
modified.

| Test | Proves |
| --- | --- |
| `records the mark exactly as the server sent it` | `ActedForAbsent(seat 1, hand 3, sequence 7, FOLD)` leaves `serverAction` deep-equal to that frame — all four fields, so a reducer that kept only the action fails |
| `a later mark replaces an earlier one` | the frame above, then `ActedForAbsent(seat 0, hand 41, sequence 2, CHECK)`, leaves `serverAction` equal to the **second**. Every field differs between the two, so a reducer that merged rather than replaced fails on whichever field it kept |
| `a mark survives the events that describe the same decision point` | mark, then `Events` carrying a `PlayerFolded` at the same sequence, leaves `serverAction` unchanged; and `Events` **then** mark leaves the same `serverAction` as mark then `Events`. Ordering is a courtesy the server offers (`ADR-0028` §4) and the reducer does not depend on it |
| `a mark changes nothing a snapshot or a turn established` | after a `Snapshot` and a `YourTurn`, a mark leaves `view`, `pendingTurn`, `narration`, `rejection`, `rejectionCount`, `outcome`, `refusal`, `rematchOffers`, `rivalPresence`, `graceRemainingMillis`, `presenceCount` and `rivalReturned` all identical |

Four tests. Six hundred and twenty-three exist after `TASK-031313`, so the suite reports **627** —
before whatever test `DEC-070`'s answer adds.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 627 passed (627)` | four ran, the modified one still runs, and nothing else moved |
| the four `--reporter=verbose` greps | each exists by name |
| `npm run check` | `serverAction` typechecks as `ActedForAbsent \| null` |

**Name the edit that makes each assertion red:**

1. Store `{ ...state, serverAction: { ...state.serverAction, ...message } }` — merge rather than
   replace → `a later mark replaces an earlier one` fails on the fields the first frame kept.
   Revert.
2. Guard the case with `if (state.serverAction !== null) return state;` — keep the first mark rather
   than the most recent → `a later mark replaces an earlier one` fails with the first frame against
   the second, and the other three still pass. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `DEC-070` is answered by a merged ADR before this ticket is started
- [ ] `the duel state > records the mark exactly as the server sent it` passes
- [ ] `the duel state > a later mark replaces an earlier one` passes
- [ ] `the duel state > a mark survives the events that describe the same decision point` passes
- [ ] `the duel state > a mark changes nothing a snapshot or a turn established` passes
- [ ] `the duel state > starts with nothing the server has not sent` passes, and the only line of it
      that differs from the state `TASK-031304` left is the added `serverAction: null`
- [ ] `duel-state.ts` reads no field of the `ActedForAbsent` message in the reducer
- [ ] The clearing rule and its test are exactly what `DEC-070`'s ADR says, and no more
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
