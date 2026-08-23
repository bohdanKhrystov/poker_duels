---
schema: 2
id: TASK-031310
title: A resumed client renders what it came back to, and invents no return
type: task
status: backlog
parent: STORY-0313
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, duel, ui, presence, resilience]
depends_on: [TASK-031309]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +614 passed \(614\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the pause a resume came back to'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing to a resumed client whose rival never left'
  - cd web-client && npm run check
---

## Goal

The two things a reload can show are pinned: a client that comes back mid-pause renders the pause,
and a client that comes back to a rival who never went renders no return line.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `web-client/src/store/duel-state.ts` | read — the `OpponentPresence` and `Snapshot` cases |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | read — `resume`, for the order it puts its frames in |

## Scope

- Two tests and nothing else. No source file changes: the behaviour was built by `TASK-031304`,
  `TASK-031306` and `TASK-031309`, and this ticket is the assertion that the *resume path* reaches
  it.
- **The frame order is the server's and is copied exactly.** `RoomRegistry.resume` returns
  `resumeFrames(runner, seat) + presence`, so a returning client receives its `Snapshot` **first**
  and the `OpponentPresence` after it. Both tests apply the frames in that order, because the
  opposite order is a different claim about a server that does not behave that way — and because
  `Snapshot` clears `rivalReturned`, an assertion written in the wrong order would pass or fail for
  reasons that have nothing to do with a resume.
- A resumed client is modelled as what it is: a **fresh store**. A reload keeps no state, which is
  precisely why the server sends the presence at all.

## Out of scope

- Any change to `boot.ts`, `reconnecting.ts` or the socket. `STORY-0310` shipped the resume and this
  ticket adds nothing to it.
- Anything a returning player is told about their **own** absence. `ADR-0028` §6 builds no journal,
  so there is no frame and nothing to render (`ADR-0046` §6).
- The countdown reaching zero — `TASK-031311`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

Both seat this client at **seat 1**, so a screen reading the rival off a literal `0` disagrees with
the fixture. The window is **47 000 ms**, whose `47` collides with no number `SNAPSHOT` carries.

`the lobby`

| Test | Proves |
| --- | --- |
| `renders the pause a resume came back to` | on a fresh store: `RoomJoined(seat 1)`, `SNAPSHOT`, then `OpponentPresence(AWAY, 47000)` — the server's own order. `Your rival is away. The duel is paused.` and `47` are on screen and the plate named `Your rival` reads `Away`. A client that showed a normal table here would act and be refused for reasons nothing on its screen explains |
| `says nothing to a resumed client whose rival never left` | on a fresh store: `RoomJoined(seat 1)`, `SNAPSHOT`, then `OpponentPresence(PRESENT, null)` — what a resuming client is **always** sent (`ADR-0028` §5). `Your rival is back.` is **not** on screen, no digit that is not a stack or a pot is, and the plate named `Your rival` carries neither `Away` nor `Timed out`. This is the one way this copy can state a falsehood (`ADR-0046` §2) |

Two tests. Six hundred and twelve exist after `TASK-031309`, so the suite reports **614**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 614 passed (614)` | two ran and the six hundred and twelve before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the frames typecheck against `ServerMessage` |

**Name the edit that makes each assertion red:**

1. In `duel-state.ts`, set `rivalReturned: message.presence === "PRESENT"` — drop the held-presence
   test → `says nothing to a resumed client whose rival never left` fails, `Your rival is back.` on
   screen. Revert.
2. In `duel-state.ts`, add `rivalPresence: null` to the `Snapshot` case → **neither test fails**,
   because in both the snapshot arrives *before* the presence frame. That is the finding, not a
   gap: the assertion that a snapshot leaves the presence standing belongs to
   `TASK-031304`'s `the next snapshot ends the return and leaves the presence`, which does fail on
   this mutation, and these two tests do not duplicate it. Say so in the PR rather than claiming
   coverage this ticket does not have.

## Acceptance criteria

- [ ] `the lobby > renders the pause a resume came back to` passes
- [ ] `the lobby > says nothing to a resumed client whose rival never left` passes
- [ ] Both tests start from a store built by `createDuelStore()` with no prior presence applied
- [ ] Both tests apply `Snapshot` before `OpponentPresence`, matching `RoomRegistry.resume`
- [ ] No file outside `web-client/src/lobby/Lobby.test.tsx` differs from `develop`
- [ ] Every other test in `Lobby.test.tsx` is byte-identical to what `TASK-031309` merged
- [ ] `npm run --silent test` reports `Tests  614 passed (614)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
