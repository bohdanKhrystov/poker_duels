---
schema: 2
id: TASK-031302
title: The line that explains the pause, and the one that says nothing
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui, presence, copy]
depends_on: [TASK-031301]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +586 passed \(586\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the duel is paused while the rival is away'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the duel continues once the rival did not come back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the rival is back only to a client that saw them go'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing before the server has stated a presence'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets the state the server sent outrank the return'
  - cd web-client && npm run check
---

## Goal

One pure function turns a presence, and whether this client saw the rival go, into exactly one of
`ADR-0046` §2's three sentences — or into nothing at all.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/presence-text.ts` | create |
| `web-client/src/table/presence-text.test.ts` | create |
| `web-client/src/table/rejection-text.ts` | read — the shape a copy module takes here |

## Scope

- One exported function, and nothing else in the file:

  ```ts
  import type { SeatPresence } from "../protocol";

  /**
   * The line under the table that explains the rival's presence, in `ADR-0046` §2's words.
   *
   * `returned` is the client's own bookkeeping, not a field of any frame: a resuming client is
   * always sent its rival's presence, `PRESENT` included, so `PRESENT` alone cannot tell a
   * return from a status quo. Telling a player their rival is back when the rival never left is
   * the one way this copy can state a falsehood.
   */
  export function presenceLine(
    presence: SeatPresence | null,
    returned: boolean,
  ): string {
    switch (presence) {
      case "AWAY":
        return "Your rival is away. The duel is paused.";
      case "ABSENT":
        return "Your rival did not come back. The duel continues, and the server acts for them.";
      case "PRESENT":
        return returned ? "Your rival is back." : "";
      default:
        return "";
    }
  }
  ```

- **Every string is `ADR-0046` §2's, character for character**, including both full stops in the
  first line and the comma in the second. They are written as literals here and quoted as literals
  by the test — a named constant shared between the encoder and its test would let a typo pass on
  both sides at once.
- The other player is **your rival** in all three, never *opponent*. The wire type is called
  `OpponentPresence` and the copy does not follow type names (`ADR-0046` §0).
- No fourth string, no ellipsis, no exclamation mark, and no word about *why* the rival is away:
  nothing knows.

## Out of scope

- Deciding `returned`. The store computes it in `TASK-031304`; this function is handed it.
- The seat plate's two words. `TASK-031301` put those in `seat-status.ts`.
- The countdown. `TASK-031305` and `TASK-031306`.
- Anything about an action the server took — `TASK-031313` owns those six sentences.
- Any string `ADR-0046` did not write. A state that seems to need a sixth raises a decision.

## Tests

`web-client/src/table/presence-text.test.ts`, one describe block: `"the presence line"`.

`the presence line`

| Test | Proves |
| --- | --- |
| `says the duel is paused while the rival is away` | `presenceLine("AWAY", false)` is exactly `Your rival is away. The duel is paused.` |
| `says the duel continues once the rival did not come back` | `presenceLine("ABSENT", false)` is exactly `Your rival did not come back. The duel continues, and the server acts for them.` |
| `says the rival is back only to a client that saw them go` | `presenceLine("PRESENT", true)` is `Your rival is back.` **and** `presenceLine("PRESENT", false)` is `""`. Both halves in one test, because either alone is satisfied by a function that ignores `returned` |
| `says nothing before the server has stated a presence` | `presenceLine(null, false)` and `presenceLine(null, true)` are both `""` — a client that has heard nothing says nothing, whatever its own flag holds |
| `lets the state the server sent outrank the return` | `presenceLine("AWAY", true)` is the away line and `presenceLine("ABSENT", true)` is the absent line — a function that tested `returned` before the presence would answer `Your rival is back.` to a rival who is away |

Five tests. Five hundred and eighty-one exist after `TASK-031301`, so the suite reports **586**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 586 passed (586)` | five ran and the five hundred and eighty-one before them still do |
| the five `--reporter=verbose` greps | each exists by name |
| `npm run check` | the module typechecks and Prettier is happy with the long second string |

**Name the edit that makes each assertion red:**

1. Return the `PRESENT` line unconditionally — drop the `returned ? … : ""` — → `says the rival is
   back only to a client that saw them go` fails on its second half, `Your rival is back.` against
   `""`. Revert.
2. Hoist `if (returned) return "Your rival is back.";` to the top of the function → `lets the state
   the server sent outrank the return` fails with the back line against the away line. Revert.

## Acceptance criteria

- [ ] `the presence line > says the duel is paused while the rival is away` passes
- [ ] `the presence line > says the duel continues once the rival did not come back` passes
- [ ] `the presence line > says the rival is back only to a client that saw them go` passes
- [ ] `the presence line > says nothing before the server has stated a presence` passes
- [ ] `the presence line > lets the state the server sent outrank the return` passes
- [ ] `presence-text.ts` exports exactly one name, `presenceLine`
- [ ] `presence-text.ts` contains the word `opponent` nowhere, and no exclamation mark
- [ ] `npm run --silent test` reports `Tests  586 passed (586)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
