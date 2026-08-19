---
id: STORY-0503
title: The ladder is a screen, reached from the first screen and left by one control
type: story
status: blocked
parent: EPIC-05
module: web-client
labels: [client, leaderboard, screen]
depends_on: [STORY-0502]
---

## Goal

A player opens the ladder from the first screen, reads an ordered list of rivals with their duel
coins and their positions, walks it a page at a time, and comes back to where they started.

## Why

The endpoint is not the product; this is. It is also the first `EPIC-05` work a human can look at,
which matters for an epic whose other five stories are queries and properties.

## Design notes

**Settled, and the tasks must respect all of it:**

- **It is its own screen, and the first screen is the door.**
  [`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md)
  settled this shape for the duel record and says `EPIC-05`'s leaderboard inherits it: the ladder is
  never rendered beside the lobby, in by one control on the branch holding *Create a duel room*, out
  by one control. **The way back is rendered by the swap, never by the ladder screen itself**, so
  the screen knows nothing about navigation and the affordance is assertable with no transport.
  `HistoryScreen.tsx` and `Lobby.tsx` are the worked example, not a coincidence to be re-derived.
- **The door does not depend on the profile read.** `ADR-0060` again: the strip renders `null` when
  that read fails and the way to a screen may not vanish with it. A ladder unreachable because
  `GET /api/me` was slow is the same bug that decision already refused once.
- **A duel in progress outranks the ladder.** `ADR-0060`'s rule for the record: the door is offered
  only on the lobby branch that offers *Create a duel room*, because a player who opened another
  screen mid-hand would leave their rival at a table nothing ends.
- **The client never sorts, never renumbers, never derives a position.**
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md) and `STORY-0502`'s server-computed
  rank. The screen prints the rank it was given.
- **`nameOrNone` is the only branch on a null display name**, from
  `web-client/src/profile/name-text.ts`
  ([`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)). A ladder
  row that writes its own fallback breaks that decision even if the string happens to match.
- **Every visible string comes from a text module**, the way `HISTORY_HEADING` in
  `web-client/src/history/history-text.ts` gives the door and the heading one spelling. The door's
  label and the screen's heading are the same constant.
- **The store is subscribed to, not owned.**
  [`ADR-0032`](../../docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md): no component
  creates a connection, and boot wiring stays out of the tree.
- **A negative balance renders as a negative number** in its correct position — `ADR-0014` calls a
  first loss at `−1` *"the case to check first when the display work lands"*, and this is that.
- **Composes `design/tokens/tokens.css`; authors no colour.** `EPIC-06` owns the visual language.

**Blocked on `DEC-056`** (whether a nameless player has a row at all, which decides whether
`No name` is ever printed here), **`DEC-058`** (whether tied players share a rank number, which
decides whether one number is printed or two) and **`DEC-059`** (whether the player's own standing
is marked, jumped to, or shown somewhere else entirely).

**Inherited, and worth naming before the ticket split argues about it:** this door is the *fifth*
control on the first screen, after *Create a duel room*, *Join the duel*, the profile strip's name
surface and *Your duels* — and `ADR-0060` predicted exactly this, calling the cost *"the first
screen becomes the only door and will crowd"*. The story does not reopen that decision. It also
inherits `DEC-054`: if URL-addressable routes have landed by then the ladder gets an address like
every other screen, and if not it is one more screen with none.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `DEC-056`, `DEC-058` and `DEC-059`, and on `STORY-0502` merging — run `/plan-story STORY-0503` then.* | — |

## Acceptance criteria

- [ ] From the first screen, one control opens the ladder and one control comes back to it —
      asserted in one test that goes both ways, as `TASK-041313` does for the record.
- [ ] The door is present when the profile read has failed and the strip is rendering `null`, and
      present when it succeeded: **both states asserted, and they render the same door.** Two inputs,
      because one fixture cannot tell a rule from a default.
- [ ] The ladder is not offered while a duel is in progress, asserted against a store state that has
      one.
- [ ] Rows print in the order the server sent them, asserted against a fixture deliberately **not**
      in coin order on the wire — a client that sorts fails this.
- [ ] The rank printed is the rank the response carried, asserted on a page whose first row is not
      rank 1.
- [ ] If `DEC-056` admits nameless players, a row for one prints exactly what `nameOrNone` returns,
      asserted beside a named row in the same list. If it excludes them, this criterion is struck
      when the story is split and the exclusion is `STORY-0502`'s test, not the screen's — the
      screen never filters rows it was sent.
- [ ] A negative balance renders with its sign, in position, asserted against a fixture holding one.
- [ ] Walking to the next page appends rows rather than replacing them, and the end of the ladder
      stops offering more.
- [ ] Every visible string in the screen is imported from the ladder's text module; no string
      literal is rendered from the component.
- [ ] `npm run check` (lint, types, tests) passes in `web-client/`.

## Out of scope

- **Searching or filtering the ladder** — nothing in the vision asks for it, and if it is ever
  wanted it inherits [`ADR-0059`](../../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md)'s
  submit-not-typing rule rather than inventing a second answer.
- **Clicking a row through to a player** — `STORY-0504`, and only if `DEC-057` says so. Until then a
  row is text and the tests assert no link, which is a real assertion rather than an absence.
- **URL addressability and browser *Back*** — `DEC-054`, the architect's, and `EPIC-04`'s
  `STORY-0412` is where it lands. This story uses whatever exists when it starts.
- **Any colour, spacing token or illustration** — `EPIC-06`.
- **Live updates while the screen is open** — the epic's out-of-scope table: no `ServerMessage` is
  added by this epic.
