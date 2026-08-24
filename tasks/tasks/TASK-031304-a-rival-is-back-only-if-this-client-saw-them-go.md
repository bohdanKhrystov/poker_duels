---
schema: 2
id: TASK-031304
title: A rival is back only if this client saw them go
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, presence]
depends_on: [TASK-031303]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +596 passed \(596\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rival who was away and is present again has come back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rival who timed out and is present again has come back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a presence that never changed is no return'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the next snapshot ends the return and leaves the presence'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a resume states the presence after its own snapshot'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'going away again is not a return'
  - cd web-client && npm run check
---

## Goal

The store can tell a rival who came back from a rival who never left, so
`ADR-0046` §2's `Your rival is back.` can never be shown to a player whose rival was there all
along.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — one field, two case bodies |
| `web-client/src/store/duel-state.test.ts` | modify — six tests added, one line changed |
| `docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md` | read — §2, the last row and the paragraph under it |

## Scope

- `DuelState` gains one field, after `presenceCount`:

  ```ts
  /**
   * Whether the rival came back from an absence **this client saw**. Client bookkeeping the
   * store accumulates across frames, in the class of `rejectionCount`: no frame carries it.
   *
   * `ADR-0046` §2: a resuming client is always sent its rival's current presence, `PRESENT`
   * included, so the frame alone cannot tell a return from a status quo. Telling a player who
   * reloaded the page that their rival returned from an absence that never happened is the one
   * way this copy can state a falsehood.
   */
  readonly rivalReturned: boolean;
  ```

- `initialState()` gains `rivalReturned: false`.
- The `OpponentPresence` case `TASK-031303` added gains one key:

  ```ts
      rivalReturned:
        message.presence === "PRESENT" &&
        (state.rivalPresence === "AWAY" || state.rivalPresence === "ABSENT"),
  ```

  Read off the presence the store **already held** and the presence the frame carries, and nothing
  else. Any frame that is not `PRESENT` sets it `false`: a rival who has just gone away is not back.

- The `Snapshot` case gains `rivalReturned: false`, and **nothing else in that case changes** — in
  particular `rivalPresence` and `graceRemainingMillis` are untouched, because a snapshot is a fact
  about the hand and not about who is at the keyboard.

  ```ts
      // ADR-0046 §2: `Your rival is back.` clears on the next Snapshot and on nothing else —
      // never on a timer, never on a fade. The presence itself is not cleared here: hands go on
      // being dealt while a seat is ABSENT, and a Snapshot that wiped it would put the table
      // back to normal under a rival who is not there.
      rivalReturned: false,
  ```

- The order the server sends a resume's frames in is what makes this work and it is not this
  client's to choose: `RoomRegistry.resume` returns `resumeFrames(runner, seat) + presence`, so the
  `Snapshot` arrives **before** the `OpponentPresence`. A return that the resume itself carries
  therefore survives the snapshot that preceded it.

## This ticket owns the assertion its change unsettles

`starts with nothing the server has not sent` gains **exactly one line**, after `presenceCount: 0`:

```ts
      rivalReturned: false,
```

The test keeps its name and its whole-object `toEqual`, so nothing is weakened — a reducer that
seeded the flag `true` still fails it. This is the second and last line this story adds to that
literal; `TASK-031303` added the first three.

## Out of scope

- Rendering the line. `presenceLine` already exists (`TASK-031302`) and `TASK-031306` calls it.
- Any other trigger for clearing the flag. `ADR-0046` §2 says `Snapshot` **and nothing else** — no
  timer, no `YourTurn`, no `Events`, no `DuelFinished`.
- `rivalPresence` and `graceRemainingMillis`, which `TASK-031303` settled and which this ticket
  must leave exactly as they are.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Six added, one
modified.

| Test | Proves |
| --- | --- |
| `a rival who was away and is present again has come back` | `AWAY(47000)` then `PRESENT(null)` leaves `rivalReturned` `true` and `rivalPresence` `"PRESENT"` |
| `a rival who timed out and is present again has come back` | `ABSENT(null)` then `PRESENT(null)` leaves `rivalReturned` `true`. Asserted separately from the row above because a condition testing only `"AWAY"` passes that one and fails this |
| `a presence that never changed is no return` | `PRESENT(null)` on a **fresh** state leaves `rivalReturned` `false`, and a second `PRESENT(null)` after it leaves it `false` too. This is the trap `ADR-0046` §2 names: the resuming client is always sent one |
| `the next snapshot ends the return and leaves the presence` | after `AWAY` → `PRESENT`, a `Snapshot` leaves `rivalReturned` `false` **and** `rivalPresence` still `"PRESENT"`. A `Snapshot` applied after `AWAY` alone leaves `rivalPresence` `"AWAY"` and `graceRemainingMillis` `47000` — the snapshot clears the flag and nothing else |
| `a resume states the presence after its own snapshot` | `AWAY(47000)`, then `Snapshot`, then `PRESENT(null)` — the order `RoomRegistry.resume` actually sends — leaves `rivalReturned` `true`. A reducer that cleared the flag on the frame *after* the presence would fail; so would one that read the presence from before the snapshot wrongly |
| `going away again is not a return` | after `AWAY` → `PRESENT` (flag `true`), a further `AWAY(47000)` leaves `rivalReturned` `false` and `rivalPresence` `"AWAY"` |

Six tests. Five hundred and ninety exist after `TASK-031303`, so the suite reports **596**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 596 passed (596)` | six ran, the modified one still runs, and nothing else moved |
| the six `--reporter=verbose` greps | each exists by name |
| `npm run check` | `rivalReturned` typechecks as `boolean` at every read |

**Name the edit that makes each assertion red:**

1. Replace the condition with `message.presence === "PRESENT"` alone → `a presence that never
   changed is no return` fails with `true` against `false`. This is the whole bug the field exists
   to prevent. Revert.
2. Narrow the held-presence test to `state.rivalPresence === "AWAY"` → `a rival who timed out and is
   present again has come back` fails with `false` against `true`, and every other test still
   passes. Revert.
3. Add `rivalPresence: null` to the `Snapshot` case → `the next snapshot ends the return and leaves
   the presence` fails on its second assertion, `null` against `"PRESENT"`. Revert.

Quote all three in the PR: edits 1 and 2 are opposite directions of the same condition, and one of
them alone leaves half of it unpinned.

## Acceptance criteria

- [ ] `the duel state > a rival who was away and is present again has come back` passes
- [ ] `the duel state > a rival who timed out and is present again has come back` passes
- [ ] `the duel state > a presence that never changed is no return` passes
- [ ] `the duel state > the next snapshot ends the return and leaves the presence` passes
- [ ] `the duel state > a resume states the presence after its own snapshot` passes
- [ ] `the duel state > going away again is not a return` passes
- [ ] `the duel state > starts with nothing the server has not sent` passes, and the only line of it
      that differs from the state `TASK-031303` left is the added `rivalReturned: false`
- [ ] The four tests `TASK-031303` added are byte-identical to what it merged
- [ ] `duel-state.ts` sets `rivalReturned` from `message.presence` and `state.rivalPresence` only —
      no literal, no `mySeat`, no clock
- [ ] `npm run --silent test` reports `Tests  596 passed (596)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
