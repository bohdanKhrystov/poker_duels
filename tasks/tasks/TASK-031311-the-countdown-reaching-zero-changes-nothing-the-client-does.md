---
schema: 2
id: TASK-031311
title: The countdown reaching zero changes nothing the client does
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, duel, presence, authority]
depends_on: [TASK-031310]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the countdown reaching zero sends nothing and changes nothing'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a window with nothing left of it renders as waiting'
  - cd web-client && npm run check
---

## Goal

The rule `ADR-0028` §3 states rather than infers becomes executable: a countdown that reaches zero
enables no control, sends no frame, enters no state and changes no word on screen.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` | read — §3, and §10's third bullet |
| `web-client/src/table/PresenceNotice.tsx` | read — the anchoring and the interval |

## Scope

- Two tests and nothing else. **No source file changes.** If either test needs a line of production
  code to pass, the client is asserting a game fact and the finding is worth more than the ticket:
  stop, and report it rather than writing the line.
- The first test is the whole point of the story. It is the client-side twin of the server test
  `ADR-0028` §10 calls *"the single most important test in the set"*: a countdown in a browser is a
  permanent invitation to client-side authority, no type prevents it, and only this test does.
- Virtual time throughout. `vi.useFakeTimers()` before `render`, every advance inside `act`, and
  `afterEach(() => vi.useRealTimers())` so the file's promise-driven tests keep their real clock.

## Out of scope

- Withdrawing `YourTurn` while the duel is paused. No frame exists to withdraw it (`ADR-0028` §6),
  and a client that greyed its own bar out at zero would be doing exactly what this ticket forbids.
- Whether the controls *look* disabled during a pause. `ADR-0046` §6 leaves that to the design and
  it is not a state this client enters.
- The server's own sweep. It runs on its own clock and this client never sees it.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

Both seat this client at **seat 1** and use a window of **47 000 ms**, whose `47` and `0` collide
with no number `SNAPSHOT` carries.

`the lobby`

| Test | Proves |
| --- | --- |
| `the countdown reaching zero sends nothing and changes nothing` | with `RoomJoined(seat 1)`, `SNAPSHOT`, a `YourTurn` allowing `["CHECK", "BET"]` so the bar is **live**, and `OpponentPresence(AWAY, 47000)`: record the bar's buttons — their accessible names in order, and each one's `disabled` — then advance virtual time by `120_000`, more than twice the window. Afterwards: `send` was called **zero** times; the buttons' names, order and `disabled` flags are identical to what was recorded; `Your rival is away. The duel is paused.` is still the line; the plate named `Your rival` still reads `Away` and **not** `Timed out`; and the countdown reads `0`. Five assertions, one per clause of `ADR-0028` §3 — *sends nothing*, *enables nothing*, *enters no state*, *assumes no resumption*, *the number stops* |
| `a window with nothing left of it renders as waiting` | `OpponentPresence(AWAY, 0)` — a frame the server legitimately sends when the window has run out but the sweep has not landed. `Your rival is away. The duel is paused.` is on screen, `0` is on screen, `Timed out` is nowhere, `Your rival did not come back.` is nowhere, and `send` was called zero times. `AWAY` with zero remaining is waiting, not an event and not an error |

Two tests. Six hundred and fourteen exist after `TASK-031310`, so the suite reports **616**.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | two ran and every test before them still does |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the `LegalActions` literal in the `YourTurn` typechecks whole |

**Name the edit that makes each assertion red.** Each of these is a real client-side-authority bug
somebody could write, and each must fail a *named* clause of the first test:

1. In `PresenceNotice.tsx`, send nothing but **enter a state**: render
   `secondsRemaining(deadline, now) === 0 ? "" : presenceLine(...)` → the away line disappears at
   zero and the first test fails on `Your rival is away. The duel is paused.`. Revert.
2. In `Lobby.tsx`, disable the bar when the countdown would have expired — pass
   `turn={null}` while `state.rivalPresence === "AWAY"` → the first test fails on the recorded
   buttons, an empty list against the two it recorded, **and** on `Waiting for your rival…`
   appearing. Note this one carefully: it also fails before the advance, so run it after the advance
   only, to prove the assertion is about zero and not about the pause. Revert.
3. Delete the `Math.max(0, …)` clamp in `presence-countdown.ts` → the first test fails on the
   countdown reading `0`, and `TASK-031305`'s `reaches zero and stays there` fails too. Two layers
   guard the clamp, which is why no *single* mutation is enough to say the assertion is redundant.
   Revert.

Quote all three in the PR, and state for each which clause went red.

## Acceptance criteria

- [ ] `the lobby > the countdown reaching zero sends nothing and changes nothing` passes
- [ ] `the lobby > a window with nothing left of it renders as waiting` passes
- [ ] The first test asserts `send` was called `0` times **after** advancing past the window
- [ ] The first test compares the bar's buttons before and after the advance — names, order and
      `disabled` — rather than only asserting that some button exists
- [ ] The first test advances at least twice the window it was given
- [ ] No file outside `web-client/src/lobby/Lobby.test.tsx` differs from `develop`
- [ ] Neither test calls `Thread`-like real waiting: no `await new Promise(setTimeout)`, no
      `vi.useRealTimers()` inside a test body
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
