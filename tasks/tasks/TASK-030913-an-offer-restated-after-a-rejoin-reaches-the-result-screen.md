---
schema: 2
id: TASK-030913
title: An offer restated after a rejoin reaches the result screen, and one stated before it does not
type: task
status: done
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, lobby, tests]
depends_on: [TASK-030912]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +564 passed \(564\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes a rematch offer restated after the rejoins DuelFinished'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes no offer that arrived before the DuelFinished'
  - cd web-client && npm run check
---

## Goal

The order `ADR-0044` §5 commits the server to is the order this screen depends on, and both halves
of that are now assertions rather than prose.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added |
| `docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md` | read — §5, why the restatement comes after |

## Scope

- **No production file changes.** `TASK-030902` cleared the offers on `DuelFinished` and
  `TASK-030910` put the control on the screen; this ticket is the pair of screen-level assertions
  that pins the ordering they assume.
- Both tests drive the frames a **resumed** socket receives, in the server's own order:
  `RoomJoined`, the resumed frames, then one `RematchOffered` per standing offer — and the second
  test moves that last frame ahead of `DuelFinished` to prove the order is what does the work.

## Out of scope

- Whether the tab still remembers the room code after a `DuelFinished` and therefore rejoins at
  all. Today it does not — `boot.ts` forgets it (`TASK-031009`) — and
  [`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) has
  since decided that the branch goes and the way back forgets instead, in a ticket of its own. These
  tests apply frames to the store directly, which is what every other screen test in this file
  does, so they neither depend on that change nor pre-empt it, and they are unchanged by it.
- `boot.ts`, `room-memory.ts` and the reconnecting transport. None of them is opened.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

The claim is an **ordering** claim, so the pair applies the same three frames and differs only in
where `RematchOffered` sits. Presence alone would prove nothing: a screen that showed every offer
it ever saw passes the first and fails the second.

| Test | Proves |
| --- | --- |
| `takes a rematch offer restated after the rejoins DuelFinished` | `RoomJoined(seat 1)`, `DuelFinished`, `RematchOffered(0)` ⇒ the region *the result* is on screen with `Your rival offers a rematch` and a live `Rematch` button |
| `takes no offer that arrived before the DuelFinished` | `RoomJoined(seat 1)`, `RematchOffered(0)`, `DuelFinished` — the same three frames, the offer moved one place earlier ⇒ the region *the result* is on screen, `Your rival offers a rematch` is **absent**, and the `Rematch` button is live and alone |

Seat **1** in both, and the offer is from seat **0**, so a screen holding a literal seat reads the
rival's offer as its own and fails on the chip text.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 564 passed (564)` | two added to 562 |
| the two `--reporter=verbose` greps | both names exist. Write the names exactly as the table spells them, without the apostrophes in *rejoin's* |

**Name the edit that makes each assertion red:**

1. Remove `rematchOffers: []` from the reducer's `DuelFinished` case → `takes no offer that arrived
   before the DuelFinished` fails, because the early offer survives into the screen. Revert.
2. Add it to the `RoomJoined` case as well → `takes a rematch offer restated after the rejoins
   DuelFinished` still passes, but nothing else does — this one is worth trying to see that the
   pair is sensitive to *where* the clearing happens, then reverting.

Quote the first in the PR.

## Acceptance criteria

- [ ] `the lobby > takes a rematch offer restated after the rejoins DuelFinished` passes
- [ ] `the lobby > takes no offer that arrived before the DuelFinished` passes
- [ ] The two tests apply the same three frames and differ only in the position of `RematchOffered`
- [ ] No production file under `web-client/src` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  564 passed (564)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
