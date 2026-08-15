---
schema: 2
id: TASK-030708
title: A rejection reads from its own fields, in the server's numbers
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030707]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +225 passed \(225\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the action the server refused and the ones it allows'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states the minimum the server sent'
  - cd web-client && npm run check
---

## Goal

Each of the five `Rejection` variants becomes one sentence built from that variant's own fields —
the server's figures, printed, with no rule of the client's added around them.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/rejection-text.ts` | create |
| `web-client/src/table/rejection-text.test.ts` | create |
| `web-client/src/table/action-text.ts` | read — `actionVerb` |
| `web-client/src/protocol/protocol.gen.ts` | read — `Rejection` and its five members. **Read only: this file is generated and byte-compared in CI** |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```ts
  import type { Rejection } from "../protocol";
  import { actionVerb } from "./action-text";
  import { formatChips } from "./chips";

  /**
   * A refused action, in the server's own numbers.
   *
   * Each variant is read off its own fields. The client states what it was told
   * and stops: it does not explain the rule behind the refusal, because it does
   * not know the rule and inventing one would be a game fact.
   */
  export function rejectionText(rejection: Rejection): string {
    switch (rejection.type) {
      case "ActionNotAllowed":
        return `${actionVerb(rejection.attempted)} was refused. The server allows ${rejection.allowed
          .map(actionVerb)
          .join(", ")}.`;
      case "AmountTooSmall":
        return `${formatChips(rejection.attempted)} is under the minimum of ${formatChips(rejection.minimum)}.`;
      case "AmountTooLarge":
        return `${formatChips(rejection.attempted)} is over the maximum of ${formatChips(rejection.maximum)}.`;
      case "NotYourTurn":
        return rejection.seatToAct === null
          ? "The server says it is nobody's turn."
          : `The server says it is seat ${rejection.seatToAct}'s turn.`;
      case "HandComplete":
        return "The server says that hand is already over.";
    }
  }
  ```

- Every number in every sentence is a field of the rejection. Nothing is subtracted to say "you are
  300 short", because the difference is a figure the server did not send.
- `NotYourTurn.seatToAct` is nullable on the wire, and both readings get a sentence. A seat index is
  a poor thing to show a player and it is what the server sent; `STORY-0311` is where seats gain
  names.

## Out of scope

- Rendering it. `TASK-030709` puts the line on the bar.
- Deciding when a rejection stops being shown, or whether the bar can act again after one. The
  store keeps `rejection` set and clears `pendingTurn` (`TASK-030404`), which is `DEC-037` — see
  `TASK-030712`.
- `ProtocolError`. A `Failure` is a refused *frame*, not a refused action, and its wording belongs
  with the component that shows it.

## Tests

`web-client/src/table/rejection-text.test.ts`, describe block `"the rejection text"`. Every variant
of the union gets one test — five variants, six tests, because `NotYourTurn` has two readings.

| Test | Proves |
| --- | --- |
| `names the action the server refused and the ones it allows` | `{ attempted: "BET", allowed: ["CHECK", "ALL_IN"] }` gives `Bet was refused. The server allows Check, All in.` |
| `states the minimum the server sent` | `{ attempted: 900, minimum: 1200 }` gives `900 is under the minimum of 1,200.` |
| `states the maximum the server sent` | `{ attempted: 20000, maximum: 13400 }` gives `20,000 is over the maximum of 13,400.` |
| `names the seat the server says is to act` | `{ seatToAct: 1 }` gives `The server says it is seat 1's turn.` |
| `says so when the server names no seat to act` | `{ seatToAct: null }` gives `The server says it is nobody's turn.` |
| `says a finished hand is finished` | `HandComplete` gives `The server says that hand is already over.` |

Six tests. Two hundred and nineteen exist, so the suite reports **225**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 225 passed (225)` | the six ran and the two hundred and nineteen before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the switch is exhaustive over the generated union, so a sixth `Rejection` variant fails the build |

**Name the edit that makes each assertion red:**

1. Print `rejection.minimum - rejection.attempted` — "you are 300 short" — → `states the minimum
   the server sent` fails on the sentence. Revert: that difference is a figure the server never
   sent, and the client would be doing the arithmetic the server exists to do.
2. Delete the `HandComplete` case → `npm run check` fails: not every code path returns a value.
   Revert.

## Acceptance criteria

- [ ] `the rejection text > names the action the server refused and the ones it allows` passes
- [ ] `the rejection text > states the minimum the server sent` passes
- [ ] `the rejection text > states the maximum the server sent` passes
- [ ] `the rejection text > names the seat the server says is to act` passes
- [ ] `the rejection text > says so when the server names no seat to act` passes
- [ ] `the rejection text > says a finished hand is finished` passes
- [ ] `rejection-text.ts` contains no `+`, `-`, `*` or `/` between two numbers
- [ ] `npm run --silent test` reports `Tests  225 passed (225)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
