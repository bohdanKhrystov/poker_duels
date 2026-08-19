---
id: STORY-0413
title: The history screen — pages, filters, search
type: story
status: ready
parent: EPIC-04
module: web-client
labels: [client, ui, history]
depends_on: [STORY-0409, STORY-0411]
---

## Goal

A player can read their whole duel record on a screen: page by page, narrowed by outcome, and
searched by opponent — each row naming the opponent, the outcome, the coin it moved, the hands it
lasted and when it finished.

## Why

`STORY-0311`'s strip shows a handful of recent results and was never meant to be the record.
`STORY-0408` and `STORY-0409` make the whole record readable; this is where a person reads it.

## Design notes

- **The client derives nothing and sorts nothing.** Rows render in the order the server sent them —
  a client-side `sort` or `reverse` is exactly the defect `TASK-031112` pinned for the strip, and the
  same fixture discipline applies: a fixture monotone in no field, asserted with `toEqual` over a
  mapped array.
- **The cursor is opaque.** The client stores what the server sent and hands it back; it never
  constructs, parses or increments one, and never derives a page number from a row count.
- **A filter change discards the cursor**, because a cursor belongs to the filter that produced it
  (`STORY-0409`). Requesting page two of a filter with page one's cursor from another filter is a
  bug the screen must make unreachable.
- **Every state is a state**: loading, empty-because-no-duels, empty-because-the-filter-matched-
  nothing, and failed. The last two say different things — *you have played no duels* and *no duel
  matches this* are different facts about the world.
- **Nothing derives the outcome from the coin delta.** The server sends `outcome`; the row prints it.
  `STORY-0311`'s `profile-text` words are reused rather than re-authored.
- No test sleeps on a real clock; paging and search are asserted against a fake transport, not a
  timer.

## Tasks

Split on 2026-08-19, against what `STORY-0409` and `STORY-0411` actually landed. Fourteen tickets,
and the chain is linear on purpose: `duel-page.ts`, `history-state.ts` and `HistoryScreen.tsx` are
each touched by more than one ticket, the run is sequential, and two startable tickets would be two
tickets editing one file.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041301](../tasks/TASK-041301-a-filter-and-a-cursor-become-exactly-one-path.md) | A filter and a cursor become exactly one path, and nothing else | ready |
| [TASK-041302](../tasks/TASK-041302-one-page-of-the-record-and-the-cursor-that-names-the-next.md) | One page of the record, and the cursor that names the next one | backlog |
| [TASK-041303](../tasks/TASK-041303-one-endpoint-keeps-one-parse.md) | One endpoint keeps one parse — the strip's read delegates | backlog |
| [TASK-041304](../tasks/TASK-041304-a-refused-cursor-restarts-the-walk-once.md) | A refused cursor restarts the walk, once, and never reaches the player | backlog |
| [TASK-041305](../tasks/TASK-041305-the-words-the-history-screen-says.md) | The words the history screen says, and the two empties that must differ | backlog |
| [TASK-041306](../tasks/TASK-041306-the-page-walk-is-a-reducer-that-appends.md) | The page walk is a reducer that appends, and never sorts | backlog |
| [TASK-041307](../tasks/TASK-041307-a-new-filter-drops-the-cursor-and-the-rows.md) | A new filter drops the cursor and the rows it belonged to | backlog |
| [TASK-041308](../tasks/TASK-041308-the-screen-renders-the-page-in-the-order-it-arrived.md) | The screen renders the page in the order it arrived, and derives no fact | backlog |
| [TASK-041309](../tasks/TASK-041309-four-states-and-the-two-empty-ones-differ.md) | Four states, and the two empty ones say different things | backlog |
| [TASK-041310](../tasks/TASK-041310-another-page-is-offered-until-there-is-none.md) | Another page is offered until the server names none, and then never asked for | backlog |
| [TASK-041311](../tasks/TASK-041311-the-outcome-filter-is-four-choices.md) | The outcome filter is four choices, and choosing one starts a new walk | backlog |
| [TASK-041312](../tasks/TASK-041312-the-search-box-sends-the-term-the-player-typed.md) | The search box sends the term the player typed, and nothing else | **blocked** (`DEC-052`) |
| [TASK-041313](../tasks/TASK-041313-the-screen-a-player-can-actually-reach.md) | The screen a player can actually reach, reading through the real transport | **blocked** (`DEC-053`) |
| [TASK-041314](../tasks/TASK-041314-no-player-id-reaches-the-history-screen.md) | No player id reaches the history screen, and the suite counts itself | backlog |

### What the split settled, and why none of it was a decision

Six judgements were made while splitting, each written into the ticket that carries it. None is a
product question: every one is derivable from something already merged, and each is named here so
nobody has to rediscover the reasoning.

- **The strip's read does not move.** `readRecentDuels` keeps its signature and its three-variant
  answer, and a new `readDuelPage` beside it carries the cursor, the query and `ADR-0057`'s restart.
  Widening the strip's read instead would have moved five `toEqual`s in `recent-duels.test.ts` and
  put `profile-strip.ts` — a lobby file — inside this story's blast radius, for a `nextCursor` the
  strip has no page to walk to. `TASK-041303` then deletes the duplicate parse immediately, so the
  endpoint keeps **one** parse, and the strip's eight merged tests are what proves the delegation
  kept its behaviour.
- **`readDuelPage` maps its own statuses instead of going through `readFromApi`.** `readFromApi`
  collapses every status but `200` and `401` into `unavailable`, and `ADR-0057` §5 requires a `400`
  carrying `after` to be told apart. `setDisplayName` already sets this precedent for the same
  reason. The alternative — a fourth `ApiRead` kind — breaks the exhaustive handling in `profile.ts`
  and in `profile-strip.ts` and costs five files.
- **`restarted` is a field, not a fourth answer kind.** A restart has to reach the reducer, or the
  newest page is appended to the rows it replaces. A fourth `kind` would break `profile-strip.ts`'s
  narrowing; a field on the `page` variant is invisible to a strip that never sends `after`.
- **The filter, the query and the path live in `src/profile/duels-query.ts`**, not in `src/history/`.
  `src/profile/` is *the HTTP module* the epic's non-negotiables already name, `STORY-0311` settled
  that placement for the same reason, and putting the query beside the screen would make
  `recent-duels.ts` import upward out of a screen directory. The screen's own three files —
  `history-text.ts`, `history-state.ts`, `HistoryScreen.tsx` — go in `src/history/`, following the
  client's screen-per-directory convention.
- **The client sends no `limit`.** The server's default is the page size. `ADR-0057` §6 makes
  `limit` the one parameter a client may vary mid-walk, varying it buys this screen nothing, and
  `recent-duels.test.ts`'s merged *asks `/api/me/duels` with no limit of its own* keeps meaning what
  it says.
- **A `no-profile` answer renders as an empty record, not as a failure.** A browser holding no
  profile has played no duels, so *"No duels yet."* is true of it and *"Your duels did not load."* is
  not — and the screen stays at the four states this story names rather than growing a fifth.

### Three things the split refused to guess, and one it did not have to

**The words are authored, not registered.** The four state sentences are written as golden literals
in `TASK-041305` because everything that constrains them is already merged: this story fixes the two
*facts* the empty states state, `ADR-0058` §4 fixes the register, and `ProfileStrip` has shipped the
voice — *"No profile yet."*, *"No duels yet."* `ADR-0058`'s own test for cheapness applies — one
string, one file, one function, one test literal per caller — and unlike `DEC-051` none of these
strings carries a privacy consequence or is inherited by four surfaces. `NO_DUELS` is deliberately
the strip's own sentence: the same fact on two surfaces should not have two spellings.

**A page boundary is a control, not a decision.** This story's own criteria say the next page
*"appends"* and that the last page *"stops offering another"* — a control that is there and then is
not. `TASK-041310` builds exactly that and refuses a scroll-triggered load, which is a different
affordance and would want a real clock in tests this story forbids.

**Two things were genuinely open and are registered rather than assumed** — `DEC-052` and `DEC-053`,
below. Between them they block the last two tickets and nothing before them, so eleven tickets build
and prove the whole screen while they are answered.

**The no-real-clock criterion needed no ticket.** `virtual-time.test.ts` is a merged sweep over every
test file under `src/`: a file that reaches for `setTimeout`, `setInterval` or
`requestAnimationFrame` without first installing fake ones fails the build. The five test files this
story adds are covered the day they land, and a second guard would be a second thing to keep true.
`TASK-041312` says so, because a debounce is the one answer that would put a timer in the client.

### The suite's own count lives in exactly one ticket

`TASK-041314` asserts `Tests  472 passed (472)` and `Test Files  70 passed (70)`, and no other
ticket in this story hardcodes a whole-suite number — every other ticket proves its own tests ran by
grepping their names out of the verbose reporter, and two prove a **file's** count, which does not
move when a ticket elsewhere adds a test. During `STORY-0411` a mid-story count change forced four
tickets to be corrected at once; here it forces one. `TASK-041314` carries the arithmetic that
produced 472, so whoever unblocks `TASK-041312` or `TASK-041313` with a different number of tests
can see which line moves.

## Acceptance criteria

- [ ] Rows render in the order received, proven with a fixture monotone in no field.
- [ ] Asking for the next page appends the server's next page and asks with the server's cursor,
      byte-identical to what was received.
- [ ] Reaching the last page stops offering another, and no request is made after it.
- [ ] Each of the three outcome filters requests the filter and renders what came back; changing a
      filter drops the cursor and asks for the first page.
- [ ] Searching sends the term the player typed, unmodified, and renders the result.
- [ ] The four states each render their own words, and the two empty states differ.
- [ ] An opponent with no name renders the same treatment `STORY-0411` chose, not a player id.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Open decisions

**None.** Two were raised by this split on 2026-08-19, both **the product owner's**, and both were
answered the same day.

- **`DEC-052` — when does the search fire?** Answered by
  [`ADR-0059`](../../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md): **on
  submit**. The box sits in a `<form>`; Enter and a submit button reading *Search* are the only two
  acts that send a request, and typing sends nothing. Emptying the box is a search like any other,
  and no *Clear* control is added. The design note above — search is *"asserted against a fake
  transport, not a timer"* — now holds by construction rather than by intention.
- **`DEC-053` — how does a player reach the record, and how do they leave it?** Answered by
  [`ADR-0060`](../../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md):
  **its own screen**, which replaces the first screen. In by one control reading *Your duels*,
  beneath the profile strip and outside it, offered whatever the profile read answered and only
  where a player is not in a duel; out by one reading *Back*, rendered by the swap rather than by
  `HistoryScreen`, so this story's screen still knows nothing about navigation. It raised
  **`DEC-054`** — URL routes and a working browser *Back* — which is the architect's and blocks
  nothing here.

`TASK-041312` and `TASK-041313` remain `blocked` until a planner transcribes `ADR-0059` §5 and
`ADR-0060` §7 into them — one Scope bullet, one word, one test and one `verify:` line each. Neither
answer moves anything already written in either ticket, both stay at three files, and **`TASK-041314`'s
472 does not move**: its arithmetic already budgeted one ADR-named test in each.

## Out of scope

- Replaying a duel — the log is not persisted (`DEC-008`), and the viewer is v0.4.
- Another player's history — `EPIC-05`.
- The visual language — `EPIC-06`.
