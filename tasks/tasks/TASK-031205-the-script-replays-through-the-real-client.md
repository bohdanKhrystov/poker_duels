---
schema: 2
id: TASK-031205
title: The script replays through the real client, from either seat, to the result
type: task
status: ready
parent: STORY-0312
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, test, end-to-end]
depends_on: [TASK-031204]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +367 passed \(367\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'plays every frame of the script and ends on the result screen'
  - cd web-client && npm run check
---

## Goal

Every frame of one seat's whole session goes into the real boot, the real store and the real screens
through a fake socket, and the client comes out the other end on the result screen — from either
seat.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/drive-duel.tsx` | create |
| `web-client/src/e2e/whole-duel.test.tsx` | create |

Read, do not modify: `web-client/src/store/reconnect.test.tsx` (the wiring to copy — `inMemoryStorage`,
`bootDuelClient` over a `FakeSocket`, `DuelProvider` over `Lobby`), `web-client/src/store/boot.ts`,
`web-client/src/e2e/scripted-duel.ts`.

## Scope

`drive-duel.tsx` is **at the S ceiling**. Build exactly this and nothing beside it.

```ts
export interface DuelRun {
  readonly seat: ScriptedSeat;
  readonly container: HTMLElement;
  readonly sent: readonly string[];
}

export function driveScriptedDuel(options: {
  readonly viewerSeat: number;
  readonly onStep?: (step: ScriptStep, index: number, container: HTMLElement) => void;
}): DuelRun;
```

- The wiring is `main.tsx`'s, with one substitution: `bootDuelClient` over
  `openConnection({ socket: new FakeSocket().asWebSocket(), storage, onMessage })`, `joinRoomCode`
  set to the script's `roomCode`, and one in-memory `Storage` shared by boot and connection.
  `openConnection`, not `openReconnectingConnection`: this story is about frames, and the retry loop
  would put a timer in a file that has no business owning one.
- The tree is `<DuelProvider store={client.store} send={client.send}><Lobby /></DuelProvider>` — the
  real screens, and no `ProfileProvider`, whose context already defaults to `null`.
- `inMemoryStorage` is written again here rather than imported from `reconnect.test.tsx`. Exporting it
  from a merged test file would put that file in this ticket's budget to save eighteen lines. Copy the
  comment with it: Node 24+ shadows jsdom's `localStorage` under Vitest.
- The replay: `act(() => socket.open())`, then walk `seat.steps` in order. A `"server"` step is
  `act(() => socket.receive(step.frame))` — the **frame string**, so the client's own decoder runs.
  A `"client"` step is **skipped in this ticket** (`TASK-031206` answers it). After every step,
  whether replayed or skipped, `onStep(step, index, container)` is called if it was given.
- No timer, no `await`, no `vi.useFakeTimers`. `virtual-time.test.ts` flags a test file that names a
  timer without installing fake ones, and the right answer here is to name none.
- If a replay exceeds Vitest's five-second default, give that `it` an explicit
  `{ timeout: 20_000 }` — do not shorten the script, do not skip steps, and record the measured
  duration in the PR.

## Out of scope

- Clicking anything, and every claim about what the client **sent** past the handshake —
  `TASK-031206`.
- The result screen's numbers — `TASK-031207`. This ticket asserts the screen is *there*.
- Cards and secrecy — `TASK-031208`, `TASK-031209`.
- Reconnection, rematch and presence. The script carries no frame for any of them.

## Tests

`web-client/src/e2e/whole-duel.test.tsx`, describe block `"a whole duel through the client"`.

| Test | Proves |
| --- | --- |
| `plays every frame of the script and ends on the result screen` | for **each** seat: every step is replayed, the count replayed equals `seat.steps.length`, and afterwards `getByRole("region", { name: "the result" })` is found and `queryByLabelText("your move")` is `null` — the table is gone, so the result is not merely rendered somewhere alongside it |
| `is on the table, not the lobby, while the duel is running` | driven with an `onStep` that records what is on screen: at the step immediately after the first `Snapshot`, the words `Create a duel room` are nowhere and the pot strip is present; at the last step it is the other way round for the result region. Two observations, so "ends on the result screen" cannot be satisfied by a client that showed the result the whole time |
| `sends the handshake and nothing more, because nothing asked it to act` | `sent`, parsed, equals exactly `[Hello{deviceId: null, protocolVersion: PROTOCOL_VERSION}, JoinRoom{code: roomCode}]` for each seat — the client answered no turn because the driver never clicked, which is what makes `TASK-031206` a real change. **`TASK-031206` replaces this test**, by its own ticket, when the driver starts acting |

Three tests added. Three hundred and sixty-four exist, so the suite reports **367**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. Stop the replay ten steps early → `plays every frame of the script and ends on the result screen`
   fails on the result region.
2. In `Lobby.tsx`, move the `state.outcome !== null` branch below the `state.view !== null` branch →
   the same test fails, which is the composition bug this whole story exists to catch.
3. Pass `joinRoomCode: null` to `bootDuelClient` → `sends the handshake and nothing more, because
   nothing asked it to act` fails.

## Acceptance criteria

- [ ] `a whole duel through the client > plays every frame of the script and ends on the result screen` passes
- [ ] `a whole duel through the client > is on the table, not the lobby, while the duel is running` passes
- [ ] `a whole duel through the client > sends the handshake and nothing more, because nothing asked it to act` passes
- [ ] Every test runs over both seats
- [ ] `drive-duel.tsx` and `whole-duel.test.tsx` name no `setTimeout`, `setInterval`,
      `requestAnimationFrame` or `vi.useFakeTimers`, and each `"server"` step's `frame` string
      reaches `socket.receive` unmodified
- [ ] Neither new file writes the bare token `WebSocket` or `MessageEvent`, and neither declares a
      type `protocol.gen.ts` exports — `boundary.test.ts` guards both files by path, and
      `socket.asWebSocket()` is the seam that keeps it green (`reconnect.test.tsx` does the same)
- [ ] The whole client suite still runs in the ordinary `npm run test`, with no server, database or
      JVM, and the PR states its measured duration
- [ ] `npm run --silent test` reports `Tests  367 passed (367)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
