---
schema: 2
id: TASK-031204
title: The client reads the committed script, and proves it is a whole duel
type: task
status: backlog
parent: STORY-0312
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, test, fixture, secrecy]
depends_on: [TASK-031203]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +364 passed \(364\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'contains a showdown that revealed the rival, and a hand that revealed nobody'
  - cd web-client && npm run check
---

## Goal

`scriptedDuel()` hands a test the committed script as typed `ServerMessage`s, decoded by the
client's own decoder — and a guard proves the script really is a whole duel with both endings in it,
so every later ticket that leans on that is not leaning on nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/scripted-duel.ts` | create |
| `web-client/src/e2e/scripted-duel.test.ts` | create |

Read, do not modify: `web-client/src/protocol/frames.ts` (`decodeServerMessage`,
`SERVER_MESSAGE_TYPES`), `web-client/src/protocol/boundary.ts` (how a module under `src/` reads a
file beside itself), `web-client/src/e2e/scripted-duel.gen.json` (the fixture — **do not open it in
an editor**; it is one long minified line).

## Scope

- `scripted-duel.ts` exports one function and the types it returns. Nothing else in the client
  imports it; it is source a test drives, in the tradition of `view-fixture.ts` and `fake-socket.ts`.

  ```ts
  export interface RivalHand { readonly handNumber: number; readonly cards: readonly string[] }
  export interface ServerStep { readonly from: "server"; readonly frame: string; readonly message: ServerMessage }
  export interface ClientStep { readonly from: "client"; readonly frame: string; readonly act: Act }
  export type ScriptStep = ServerStep | ClientStep;
  export interface ScriptedSeat {
    readonly viewerSeat: number;
    readonly steps: readonly ScriptStep[];
    readonly rivalHoleCards: readonly RivalHand[];
  }
  export interface ScriptedDuel { readonly roomCode: string; readonly seats: readonly ScriptedSeat[] }
  export function scriptedDuel(): ScriptedDuel;
  ```

- The file is read with `readFileSync` from a path resolved off `import.meta.url`, exactly as
  `boundary.ts` reaches `protocol.gen.ts` — **not** with `import … from "./scripted-duel.gen.json"`.
  A JSON import would make `tsc` infer a literal type for a file of hundreds of thousands of
  characters, and would widen every `"Snapshot"` to `string` anyway.
- Every `"server"` step's `frame` goes through `decodeServerMessage`, the same function the live
  socket uses. A `null` return throws, naming the seat and the step index: a frame this client cannot
  read is a broken fixture, not a step to skip.
- A `"client"` step's `frame` is `JSON.parse`d and typed `Act` with one cast, and the cast carries
  the same comment `decodeServerMessage` carries: `verifyDuelScript` gates the file against the
  server's own descriptors, so a hand-written structural validator here would be a second copy of the
  schema.
- Both `frame` strings are kept verbatim beside the decoded value. `TASK-031207` compares what the
  client sent against `frame`, and a comparison against something re-encoded here would prove
  nothing.

## Out of scope

- Rendering anything, mounting React, or opening a socket — `TASK-031205`.
- Asserting what the client *shows* — `TASK-031206` onwards. This ticket's tests are about the
  fixture only.
- Any change to `frames.ts`, `boundary.ts` or `protocol.gen.ts`.

## Tests

`web-client/src/e2e/scripted-duel.test.ts`, describe block `"the committed duel script"`. Every test
runs over **both** seats, so nothing passes by being right about one of them.

| Test | Proves |
| --- | --- |
| `decodes every server frame it carries` | for each seat, no `"server"` step decoded to `null`, every decoded `type` is in `SERVER_MESSAGE_TYPES`, and each seat has more than 40 server steps — a script of two frames would satisfy the first two clauses saying nothing |
| `carries one Act for every YourTurn, each answering that turn` | per seat, the `"client"` step count equals the `YourTurn` count and is greater than 1; every `"client"` step's predecessor is a `YourTurn`; every act's `handNumber` and `actionSequence` equal that turn's |
| `ends both seats on the same DuelFinished, with a winner` | each seat's last step is a `"server"` step carrying `DuelFinished`; the two `outcome`s are deep-equal; `winner` is `0` or `1`; `handsPlayed` is greater than 5 |
| `names the rival's two cards for every hand, from the rival's own frames` | `rivalHoleCards` covers hand numbers `1..handsPlayed` with no gap and two cards each; and seat 0's `rivalHoleCards` equals what the test itself reads out of **seat 1's** `Snapshot` frames (`view.seats[1].holeCards` per hand), and the mirror for seat 1 — a cross-check between the two halves of the fixture, not a restatement of one |
| `contains a showdown that revealed the rival, and a hand that revealed nobody` | per seat, the hand numbers whose frames carry a `HandRevealed` naming the rival form a non-empty set; the hand numbers whose frames carry no `HandRevealed` at all form a non-empty set; the two sets are disjoint. This is what stops `TASK-031208` and `TASK-031209` being vacuous, so its failure message prints both sets |

Five tests added. Three hundred and fifty-nine exist, so the suite reports **364**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. In `scripted-duel.ts`, drop the last step of each seat before returning → `ends both seats on the
   same DuelFinished, with a winner` fails.
2. Return `rivalHoleCards` with its first entry's `cards` swapped for `["Ah","Ah"]` → `names the
   rival's two cards for every hand, from the rival's own frames` fails on the cross-check.
3. Filter every `HandRevealed` out of the decoded events → `contains a showdown that revealed the
   rival, and a hand that revealed nobody` fails on the first set.

## Acceptance criteria

- [ ] `the committed duel script > decodes every server frame it carries` passes
- [ ] `the committed duel script > carries one Act for every YourTurn, each answering that turn` passes
- [ ] `the committed duel script > ends both seats on the same DuelFinished, with a winner` passes
- [ ] `the committed duel script > names the rival's two cards for every hand, from the rival's own frames` passes
- [ ] `the committed duel script > contains a showdown that revealed the rival, and a hand that revealed nobody` passes
- [ ] Every test asserts over both seats
- [ ] `scripted-duel.ts` declares no type whose name `protocol.gen.ts` exports, and reads the fixture
      with `readFileSync`, not with a JSON import
- [ ] `npm run --silent test` reports `Tests  364 passed (364)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
