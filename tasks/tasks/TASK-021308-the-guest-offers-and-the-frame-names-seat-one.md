---
schema: 2
id: TASK-021308
title: The guest offers first, and both frames name seat 1
type: task
status: ready
parent: STORY-0213
module: poker-server
estimate: XS
tier: sonnet
review: standard
labels: [server, protocol, rooms, tests]
files_touched: 1
depends_on: [TASK-021307]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketRematchTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

Prove `RematchOffered.seat` is **read from the offering seat** rather than being the constant `0`.

Every expectation in `STORY-0213` as written is `seat = 0`, because every test drives the *host*
into the offer. `TASK-021302` establishes that the field is not the *recipient's* seat — the guest's
copy reads `0` while the guest is seat `1`, which no recipient-naming implementation produces. What
no ticket in the story reaches is the other wrong implementation: a hard-coded `0`, or anything that
resolves to the host's seat by construction. Both satisfy all seven tickets.

This is the flipped input that separates them. One test, one file.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRematchTest.kt` | modify — one test added |

Read, not edited: the `finishedDuel` fixture created by `TASK-021302` in this same file, which
already returns two live sockets on a `RoomState.FINISHED` room with both drained. This ticket adds
no fixture and changes none.

## Scope

- Add one test to `DuelSocketRematchTest`, reusing `finishedDuel` exactly as `TASK-021303`–`TASK-021306`
  do.
- The **guest** sends the only `OfferRematch` — `ProtocolCodec.encode(OfferRematch)` on the guest's
  socket, with the host sending nothing at all. `OfferRematch` is a `data object` and carries no
  seat, so the seat in the reply can only have come from the socket's own `RoomMembership`; that is
  precisely the thing under test.
- Assert on **both** drained lists, for the same reason `TASK-021302` does: the host's copy is the
  one that cannot be confused with a recipient-named field, since the host is seat `0` and the
  expectation is `1`.

## Out of scope

- Any second offer, agreement, or duel start — the guest offers once and nothing answers it.
  `TASK-021303` owns the agreeing pair.
- Refusals, repeats and reconnects — `TASK-021304` through `TASK-021307`.
- Touching `finishedDuel` or any other test. If the fixture cannot express a guest-side send
  without modification, stop and say so rather than editing it: that is a second file and a
  different ticket.

## Tests

`DuelSocketRematchTest`

| Test | Proves |
| --- | --- |
| `theGuestsOfferNamesSeatOneOnBothSockets` | after the guest's single `OfferRematch`, each socket has received **exactly one** `RematchOffered` and both carry `seat == 1` — so the field tracks the offering seat, and is neither the constant `0` nor the host's seat |

The assertion that carries this ticket is `seat == 1` on the **host's** socket. A hard-coded `0`
fails it, a host-seat-by-construction implementation fails it, and a recipient-named implementation
fails it too — the host would read `0`. State the expectation as the literal `1`; do not compute it
from the guest's membership or from anything the production code also reads.

## Acceptance criteria

- [ ] `DuelSocketRematchTest.theGuestsOfferNamesSeatOneOnBothSockets` passes
- [ ] Both drained frame lists contain exactly one `ServerMessage.RematchOffered`, and both name
      `seat = 1`
- [ ] The expected seat is written as a literal, not derived from the code under test
- [ ] No file outside `DuelSocketRematchTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
