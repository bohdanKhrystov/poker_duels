---
id: STORY-0411
title: The name in the client — shown, and settable
type: story
status: backlog
parent: EPIC-04
module: web-client
labels: [client, ui, profiles, identity]
depends_on: [STORY-0402]
---

## Goal

The client shows the name the server sent — for me, and for the opponent on a result line — offers a
player who has none the chance to set one, and renders each of the server's refusals as something a
person can act on.

## Why

`EPIC-03` renders `opponentPlayerId` and has no way to set a name; it named this story as where both
are fixed. It is also the first client work in this epic, and `STORY-0412` and `STORY-0413` queue
behind it because all three extend the same store and screen shell.

## Design notes

- **The client derives nothing.** It renders `displayName` and `opponentDisplayName` exactly as
  received, and never falls back to `opponentPlayerId`, never builds `Player-3F2A`, never
  title-cases or trims for display. The name it shows is a server fact
  ([`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md)).
- **`null` is rendered, not hidden.** What a nameless player looks like is decided here, inside
  `EPIC-06`'s language, and `ADR-0029` §6 leaves the treatment to the client on the one condition
  that the client never asks the server for a placeholder. Whatever the treatment is, it composes
  `design/tokens/tokens.css` and authors no colour.
- **The set-name form sends what the player typed and shows what came back.** The server trims and
  normalises, and `ADR-0029` §5 returns the whole profile for that reason: the field is repopulated
  from the response, never from the input.
- **Three failures, three sentences**: `400` (a name the rules refuse), `409` (taken), `403` (you
  already have one, and it cannot change). `403` is the one a client must not offer a retry for, and
  the copy has to say *permanent* — a form that invites a retry it can never satisfy is worse than a
  form that refuses.
- **Setting a name is offered once and is permanent** — the screen must say so **before** the send,
  not after. `ADR-0029` costs a typo forever, and a player is entitled to know that at the moment
  they can still avoid it.
- No client test sleeps on a real clock, and no test asserts a value that is only ever the fixture's
  default.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0411` once `STORY-0402` has merged.* | — |

## Acceptance criteria

- [ ] The profile strip shows the player's own name when there is one, and the agreed treatment when
      there is not — both asserted, from two distinct fixtures.
- [ ] A result line shows the opponent's name when there is one, and never shows a player id.
- [ ] Setting a name sends one request, and the field afterwards holds the **canonical** string the
      server returned, asserted with an input the server would change.
- [ ] Each of `400`, `409` and `403` renders its own sentence, and only `400` and `409` leave the
      form retryable.
- [ ] The screen states that the choice is permanent before the request is sent.
- [ ] The client sends nothing derived: no request body contains a player id, and no rendered name
      is computed from one.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Out of scope

- The account screens — `STORY-0412`.
- The history screen — `STORY-0413`.
- The offer to make an account — `STORY-0415`.
- Any colour or type decision — `EPIC-06` owns the language this composes.
