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

A player opens the ladder from the first screen, is told where **they** stand before they read
anybody else's row, reads an ordered list of rivals with their duel coins and their positions, walks
it a page at a time, and comes back to where they started.

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
- **Repeated rank numbers are printed verbatim, and a tie is marked by nothing else.**
  [`ADR-0064`](../../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md)
  answers `DEC-058`: tied players share one rank, so a page reading `1 1 1 1 5 5 5 …` is ordinary
  and the screen neither de-duplicates it, nor collapses it into a group, nor renumbers it, nor
  prints `=`, `T5` or *tied with 189 others* (§5). A page may also begin with the rank the previous
  page ended on, which is correct rather than a repeat to filter (§2). **This is the routine state
  for the first days of every season**, not a corner case: with `ADR-0061`'s monthly window and
  `ADR-0063`'s absent gate, a four-hundred-row September ladder two days in holds about five
  distinct numbers, and the gap from `5` to `195` is a skip rather than missing rows.
- **`nameOrNone` is the only branch on a null display name**, from
  `web-client/src/profile/name-text.ts`
  ([`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md)). A ladder
  row that writes its own fallback breaks that decision even if the string happens to match. **A
  nameless player is on the ladder**, so this screen prints `No name` in ordinary rows —
  [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  §2 answers the question `ADR-0058` parked, and it is the inheritance that ADR predicted.
- **Every visible string comes from a text module**, the way `HISTORY_HEADING` in
  `web-client/src/history/history-text.ts` gives the door and the heading one spelling. The door's
  label and the screen's heading are the same constant.
- **The store is subscribed to, not owned.**
  [`ADR-0032`](../../docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md): no component
  creates a connection, and boot wiring stays out of the tree.
- **A negative standing renders as a negative number** in its correct position — `ADR-0014` calls a
  first loss at `−1` *"the case to check first when the display work lands"*, and this is that.
- **The screen names the season it is showing, and the name comes from the response.**
  [`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §6:
  a player reads the month and the year in ordinary English — `August 2026` — never the identifier
  `2026-08`, which is the wire form. The client does **not** work the season out from the browser's
  clock: that is a client asserting a server fact (`ADR-0002`) and it is wrong for two hours of every
  month in half the world.
- **The number on a row is the season standing, not the number the profile strip prints.**
  `ProfileStrip.tsx` prints the all-time counter from `player.coin_balance`; this screen prints
  `SUM(coin_delta)` inside the season. They differ from the second season onwards, on purpose, and
  the season name is the only thing that keeps that honest — which is why the name is a criterion
  below rather than decoration.
- **Composes `design/tokens/tokens.css`; authors no colour.** `EPIC-06` owns the visual language.

**Gated by no decision.** All three are answered, and the last of them is why this screen has a line
above its rows. `DEC-056` — `ADR-0063`: nothing gates a place, so this screen filters no row it was
sent and `No name` is an ordinary ladder row. `DEC-058` — `ADR-0064`: tied players share one rank,
the screen prints one number per row and prints repeats unchanged. `DEC-059` —
[`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md), which was sharpened
by **both** of the others: with no name to look for a nameless player cannot find themselves by
scanning, and with 190 rows reading `5` neither can anybody else, so the ladder hands a player their
row instead:

- **One self line, above the list and below the season name**, stating the player's own **rank** and
  **season standing** — the same two numbers their row carries (§1). It is rendered whether or not
  their row is in the page on screen, and it does not change as pages are appended.
- **It is rendered from the field the response carried, never derived by matching the player's id
  against the rows on screen** (`ADR-0002`, §8). Matching would be wrong on every page the player is
  not on, which is nearly all of them.
- **Three states, and the third is not a zero** (§4): rank and standing; **no place this season** for
  a player who finished no duel in it, printing no number at all; and **nothing** for a client with
  no profile, where the ladder renders exactly as it otherwise would.
- **No row in the list is marked as the reader's**, no *jump to me*, no scroll-to-my-row, no ladder
  total, no movement line, no tie count (§5, §7). The self line is the whole answer in v0.3.
- **The self line duplicating a row on the page is correct** (§6). A player who is on the page they
  are looking at appears twice, once above the list and once in it, and a test that reads the second
  as a duplicate is asserting the wrong property.
- **Every number on this screen — rows and self line alike — is the season standing**, under the
  season name (§2). The all-time counter stays on the profile strip, on the other screen, and the two
  are never visible at once.

**Inherited, and worth naming before the ticket split argues about it:** this door is the *fifth*
control on the first screen, after *Create a duel room*, *Join the duel*, the profile strip's name
surface and *Your duels* — and `ADR-0060` predicted exactly this, calling the cost *"the first
screen becomes the only door and will crowd"*. The story does not reopen that decision. It also
inherits `DEC-054`: if URL-addressable routes have landed by then the ladder gets an address like
every other screen, and if not it is one more screen with none.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `STORY-0502` merging and on nothing else — run `/plan-story STORY-0503` then. `DEC-056` is answered by `ADR-0063`, `DEC-058` by `ADR-0064`, `DEC-059` by `ADR-0065`.* | — |

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
- [ ] The rank printed is the rank the response carried, asserted on **two** pages: one whose first
      row is not rank 1, and one whose first row repeats the rank the previous page ended on. A tie
      spanning a page boundary is ordinary and the screen must not treat the repeat as a duplicate
      (`ADR-0064` §2).
- [ ] **Repeated ranks are printed verbatim**, asserted against a page whose ranks read
      `1, 1, 1, 4` — every row shows its number, no row is blanked, grouped, de-duplicated or
      renumbered, and the skip from `1` to `4` is rendered as sent (`ADR-0064` §1, §5).
- [ ] **A tied row carries no marker**, asserted by the absence of any tie glyph, count or extra
      class on a page full of equal ranks. `ADR-0064` §5 ships the repeated number and nothing else,
      so this is the criterion that catches a `=` being helpfully invented inside a ticket.
- [ ] **A nearly flat ladder renders as an ordinary ladder** — asserted against the routine second
      day of a season, a page whose rows all carry the same rank and near-identical standings. Not
      an empty state, not a message, not a special case (`ADR-0064` §6).
- [ ] A row for a player with no display name prints exactly what `nameOrNone` returns, asserted
      beside a named row in the same list (`ADR-0063` §2 — `DEC-056` admits them, so this criterion
      is written rather than struck).
- [ ] The screen renders every row it was sent, asserted against a page containing a nameless player
      and a player with a **negative** standing — the two rows a client might be tempted to drop.
      The screen filters nothing; there is no eligibility rule on this side of the wire.
- [ ] A negative standing renders with its sign, in position, asserted against a fixture holding one.
- [ ] **The self line states the rank and the season standing the response carried**, asserted against
      **two** responses carrying different self standings — one fixture cannot tell a rendered field
      from a hardcoded string (`ADR-0065` §1).
- [ ] **The self line renders for a response whose page does not contain the player**, asserted — and
      the numbers it shows come from the response's own field rather than from any row, which is the
      assertion that catches a client matching its player id against the rows on screen (`ADR-0065`
      §8, `ADR-0002`).
- [ ] **A player with no place this season is told so, and no number is printed** — asserted against a
      response carrying that state, beside one carrying a rank and a `0` standing. `0` is a real
      standing and *no place* is not one, and a screen that prints `0` for both fails this
      (`ADR-0065` §4).
- [ ] **A response carrying no self standing renders no self line, and the ladder renders anyway** —
      asserted, because this is the ordinary state of a first visit and it is not an error, not an
      empty state and not a spinner (`ADR-0065` §4).
- [ ] **No row in the list is marked as the reader's**, asserted against a page that **contains** the
      reader's own row: it renders exactly as its neighbours do, and the player appears twice — once
      in the self line, once in the list — which is correct rather than a duplicate (`ADR-0065` §5,
      §6).
- [ ] The screen names the season it is showing, taken **from the response**, asserted against two
      responses naming different seasons — one fixture cannot tell a rendered field from a hardcoded
      string, and a client that reads the browser's clock passes with one and fails with two.
- [ ] An empty ladder — the routine state on the first day of a season, not a corner case — renders
      as an empty ladder that still names its season, not as an error and not as a spinner.
- [ ] Walking to the next page appends rows rather than replacing them, and the end of the ladder
      stops offering more.
- [ ] Every visible string in the screen is imported from the ladder's text module; no string
      literal is rendered from the component.
- [ ] `npm run check` (lint, types, tests) passes in `web-client/`.

## Out of scope

- **Marking, grouping or counting a tie** — `ADR-0064` §5: v0.3 prints the repeated rank and nothing
  else. A `=`, a `T5`, a *tied with 189 others* line or a visual grouping of equal ranks is a change
  to that decision's cheapest sentence and an ordinary ticket if it is ever wanted, not a detail to
  fill in here.
- **Searching or filtering the ladder** — nothing in the vision asks for it, and if it is ever
  wanted it inherits [`ADR-0059`](../../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md)'s
  submit-not-typing rule rather than inventing a second answer.
- **Clicking a row through to a player** — `STORY-0504`, and only if `DEC-057` says so. Until then a
  row is text and the tests assert no link, which is a real assertion rather than an absence.
- **URL addressability and browser *Back*** — `DEC-054`, the architect's, and `EPIC-04`'s
  `STORY-0412` is where it lands. This story uses whatever exists when it starts.
- **A rank, a season standing or a season name on the profile strip** —
  [`ADR-0065`](../../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md) §2. `ProfileStrip.tsx`
  keeps printing the all-time counter and is untouched by this story; the season number lives on this
  screen only, under the season name.
- **A *jump to me* control, a scroll-to-my-row, a highlighted row, a ladder total (*5th of 404*), a
  movement line or a tie count** — `ADR-0065` §5 and §7. The self line is the whole answer in v0.3,
  and each of these is an ordinary ticket if it is ever wanted rather than a detail to fill in here.
- **Any colour, spacing token or illustration** — `EPIC-06`.
- **Live updates while the screen is open** — the epic's out-of-scope table: no `ServerMessage` is
  added by this epic.
- **A season selector, a *last season* line, or anything naming a season other than the one being
  shown** — `ADR-0061` §7 ships the current season only, and whether a finished one is ever reachable
  is `DEC-060`. This screen shows one season and says which; it offers no way to ask for another.
