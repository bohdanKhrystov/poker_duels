---
schema: 2
id: TASK-031314
title: The store keeps the most recent action the server took, until the absence ends
type: task
status: backlog
parent: STORY-0313
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, presence]
depends_on: [TASK-031313]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the mark exactly as the server sent it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a later mark replaces an earlier one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a mark survives the events that describe the same decision point'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a mark changes nothing a snapshot or a turn established'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a snapshot and a turn after the mark leave it standing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rival still away or still absent keeps the mark'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "a rival's return takes the mark off"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the duel ending takes the mark off'
  - cd web-client && npm run check
---

## Goal

`ActedForAbsent` stops falling through the reducer's `default`: the store holds the most recent
action the server took for an absent seat, exactly as the frame stated it, for as long as the server
is still the thing acting for that seat.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — one field, one reducer case, two keys in two existing cases |
| `web-client/src/store/duel-state.test.ts` | modify — eight tests added, one line changed |
| `web-client/src/protocol/protocol.gen.ts` | read — `ActedForAbsent` is `{ type, seat, handNumber, actionSequence, action }`, already generated |
| `docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md` | read — §2, the two frames that clear it, and §4, the ones that do not |

## Scope

- `DuelState` gains one field, after `rivalReturned`:

  ```ts
  /**
   * The most recent action the server took for an absent seat, or `null` if it has taken
   * none — or if the absence that produced it has ended. The whole frame, kept rather than
   * picked apart: `(handNumber, actionSequence)` identifies the decision point uniquely, so a
   * screen that ever wants to attach the mark to an event can do it by coordinate rather than
   * by the order the frames arrived in (`ADR-0028` §4). `ADR-0046` §4 asks for the most recent
   * one and no log; `ADR-0075` fixes how long it lives.
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

### What takes the mark off, per `ADR-0075` §2

**Exactly two frames, and no others.** The mark is a status, not a notice: while a seat is absent
the server replaces it at nearly every decision point, so it can only go stale once the server stops
acting.

- The `OpponentPresence` case `TASK-031303` wrote gains **one key**:

  ```ts
      // ADR-0075 §2: the mark lives as long as the absence that produced it. Cleared on the
      // frame, not on a transition — unlike `rivalReturned`, this needs no memory of what the
      // client held before, because it is about whether the server is still acting for that
      // seat and not about whether a return happened.
      serverAction: message.presence === "PRESENT" ? null : state.serverAction,
  ```

- The `DuelFinished` case gains **one key**:

  ```ts
      // ADR-0075 §2: a boundary guard, not a statement about absence. At DuelFinished the mark
      // renders nowhere either way; this is what stops one surviving into a rematch, since a
      // Snapshot clears `outcome` and brings the table back (ADR-0044 §4).
      serverAction: null,
  ```

- **`Snapshot` does not clear it, and neither does `YourTurn`.** `ADR-0075` §2 and its *Context*:
  `AbsentSeats.kt` prepends the mark to `next.outbound`, and `act` composes that outbound through
  `framesFor` = `broadcast + turnFor`, so the mark and the `Snapshot` describing the mark's own
  action are **consecutive frames in one delivery**. A reducer that cleared on either would clear
  the mark microseconds after setting it, and nobody would ever read it. Leave both cases alone.
- `Events`, `Rejected`, `RematchOffered`, `Failure` and `RoomJoined` are untouched too.

## This ticket owns the assertion its change unsettles

`starts with nothing the server has not sent` gains **exactly one line**, after `rivalReturned:
false`:

```ts
      serverAction: null,
```

The test keeps its name and its whole-object `toEqual`, so nothing is weakened.

## Out of scope

- Rendering. `TASK-031315`.
- Any attachment of the mark to `narration`. No action log is designed here (`ADR-0046` §4,
  `ADR-0075` §6), and a reducer that walked the event list looking for a matching sequence would be
  building one.
- Choosing a word. The reducer holds a frame; `absentActionText` turns it into a sentence.
- **A hand-boundary rule.** `ADR-0075` alternative 4 considered clearing the mark when a `Snapshot`
  carries a `handNumber` other than the mark's, and rejected it: it does not answer the question and
  it makes the line blink at every hand boundary. Do not add it.
- **Clearing `rivalPresence`, `graceRemainingMillis` or `rivalReturned` at a duel or room boundary.**
  `ADR-0075` names that hole and leaves it open on purpose; it is a separate ticket.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Eight added, one
modified.

| Test | Proves |
| --- | --- |
| `records the mark exactly as the server sent it` | `ActedForAbsent(seat 1, hand 3, sequence 7, FOLD)` leaves `serverAction` deep-equal to that frame — all four fields, so a reducer that kept only the action fails |
| `a later mark replaces an earlier one` | the frame above, then `ActedForAbsent(seat 0, hand 41, sequence 2, CHECK)`, leaves `serverAction` equal to the **second**. Every field differs between the two, so a reducer that merged rather than replaced fails on whichever field it kept |
| `a mark survives the events that describe the same decision point` | mark, then `Events` carrying a `PlayerFolded` at the same sequence, leaves `serverAction` unchanged; and `Events` **then** mark leaves the same `serverAction` as mark then `Events`. Ordering is a courtesy the server offers (`ADR-0028` §4) and the reducer does not depend on it |
| `a mark changes nothing a snapshot or a turn established` | after a `Snapshot` and a `YourTurn`, a mark leaves `view`, `pendingTurn`, `narration`, `rejection`, `rejectionCount`, `outcome`, `refusal`, `rematchOffers`, `rivalPresence`, `graceRemainingMillis`, `presenceCount` and `rivalReturned` all identical |
| `a snapshot and a turn after the mark leave it standing` | mark, **then** a `Snapshot`, **then** a `YourTurn`, leaves `serverAction` deep-equal to the mark. This is the assertion that pins `ADR-0075`'s two rejected candidates out for good: the server sends a `Snapshot` for every applied action in the same delivery as the mark, so a reducer clearing on either would leave `ADR-0046` §4 unreadable. The test above applies the same two frames **before** the mark and cannot see it |
| `a rival still away or still absent keeps the mark` | mark, then `OpponentPresence(AWAY, 60000)`, leaves `serverAction` deep-equal to the mark; and mark, then `OpponentPresence(ABSENT)`, likewise. **Two inputs**, because a reducer that cleared on every `OpponentPresence` whatever it carried passes `a rival's return takes the mark off` and fails only here |
| `a rival's return takes the mark off` | `OpponentPresence(ABSENT)`, mark, `OpponentPresence(PRESENT)` leaves `serverAction` `null` **and `rivalReturned` `true` in the same state**. That single assertion is `DEC-070`: the frame that earns `Your rival is back.` is the frame that takes `The server folded for your rival.` away, so the two sentences can never be on screen together |
| `the duel ending takes the mark off` | mark, then `DuelFinished`, leaves `serverAction` `null` |

Eight tests. Six hundred and twenty-three exist after `TASK-031313`, so the suite reports **631**.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | eight ran, the modified one still runs, and nothing else moved |
| the eight `--reporter=verbose` greps | each exists by name |
| `npm run check` | `serverAction` typechecks as `ActedForAbsent \| null` |

**Name the edit that makes each assertion red:**

1. Store `{ ...state, serverAction: { ...state.serverAction, ...message } }` — merge rather than
   replace → `a later mark replaces an earlier one` fails on the fields the first frame kept.
   Revert.
2. Guard the case with `if (state.serverAction !== null) return state;` — keep the first mark rather
   than the most recent → `a later mark replaces an earlier one` fails with the first frame against
   the second, and the others still pass. Revert.
3. Add `serverAction: null` to the `Snapshot` case → `a snapshot and a turn after the mark leave it
   standing` fails, and `a mark changes nothing a snapshot or a turn established` **still passes**,
   because it applies its snapshot before the mark. Say so in the PR: that pair is why the new test
   exists rather than being folded into the old one. Revert.
4. Make the `OpponentPresence` key an unconditional `serverAction: null` → `a rival still away or
   still absent keeps the mark` fails on **both** halves and `a rival's return takes the mark off`
   still passes. Revert.

Quote all four in the PR.

## Acceptance criteria

- [ ] `the duel state > records the mark exactly as the server sent it` passes
- [ ] `the duel state > a later mark replaces an earlier one` passes
- [ ] `the duel state > a mark survives the events that describe the same decision point` passes
- [ ] `the duel state > a mark changes nothing a snapshot or a turn established` passes
- [ ] `the duel state > a snapshot and a turn after the mark leave it standing` passes
- [ ] `the duel state > a rival still away or still absent keeps the mark` passes, and it asserts
      `AWAY` **and** `ABSENT`
- [ ] `the duel state > a rival's return takes the mark off` passes, and asserts `serverAction ===
      null` **and** `rivalReturned === true` on the same state
- [ ] `the duel state > the duel ending takes the mark off` passes
- [ ] `the duel state > starts with nothing the server has not sent` passes, and the only line of it
      that differs from the state `TASK-031304` left is the added `serverAction: null`
- [ ] `duel-state.ts` reads no field of the `ActedForAbsent` message in the reducer
- [ ] The only cases that clear `serverAction` are `OpponentPresence` (and only on `PRESENT`) and
      `DuelFinished` — `Snapshot` and `YourTurn` are byte-identical to what `TASK-031304` left
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
