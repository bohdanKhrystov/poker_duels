---
id: STORY-0414
title: Claimed here, recovered there, end to end
type: story
status: backlog
parent: EPIC-04
module: web-client
labels: [client, e2e, auth, identity]
depends_on: [STORY-0407, STORY-0412, STORY-0413]
---

## Goal

One test plays a duel anonymously, wins the coin, names the profile, claims it with credentials, and
signs in from a **second client bearing a different device id** — which reads back the same balance,
the same name and the same duel.

## Why

This test *is* the epic. Everything else is how it is made to pass, and the epic's definition of done
says so in those words.

## Design notes

- **Two clients, two device ids, no shared storage.** The second client must be a genuinely separate
  storage and connection — the assertion is worthless if both halves read the same key. `TASK-030304`
  owns exactly one storage key for the device id, which is what makes two of them constructible.
- **It runs the client's own machinery**, in the shape `STORY-0312` established: a committed script
  of real server frames replayed through the real store and screens, rather than a browser
  automation this repository has not decided on (`DEC-024`).
- **The coin is asserted as a number the server sent**, at both ends, and the balance is compared for
  equality rather than for being non-zero.
- **The duel is asserted by identity**, not by count: the same `duelId` appears in the second
  client's history, with the same opponent and the same outcome.
- **The name is asserted after the claim**, because `ADR-0030` §1 promises a claimed profile keeps
  it: the permanence trigger fires only on statements naming `display_name`, and a claim names no
  column of `player` at all.
- **The hand-checked receipt is part of the epic, not this story**: `ADR-0012` named the cost in
  advance, and the epic's definition of done keeps one manual pass on a real second device.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0414` once `STORY-0407`, `STORY-0412` and `STORY-0413` have merged.* | — |

## Acceptance criteria

- [ ] One test carries the whole arc: anonymous duel → win → name → sign-up → second client → sign-in
      → same balance, same name, same duel.
- [ ] The two clients hold different device ids, and neither reads the other's storage — asserted, not
      arranged and assumed.
- [ ] The second client never sends a player id, and is *told* who it is.
- [ ] The balance read at the end equals the balance read before the claim, exactly.
- [ ] The duel in the second client's history has the same `duelId` and the same outcome as the one
      played in the first.
- [ ] Signing out on the second client leaves it with no profile, and the first client is unaffected.

## Out of scope

- A browser-driven end-to-end harness — `DEC-024` is open and this story does not answer it.
- Anything the epic's other stories own; this story adds no production behaviour, only proof.
