---
id: EPIC-13
title: The living table — the turn clock, the chips, and the act just made
type: epic
status: backlog
labels: [client, design, table, server]
---

## Goal

Make the duel table say what is happening, when it happens, and how long the player has to answer.

Today the table is correct and quiet. Every fact on it is one the server stated, and a player who
already knows Hold'em can follow a hand. What it does not do is *tell* them: nothing marks whose
turn it is beyond two words, nothing counts down, nothing says what the rival just did, the number
labelled `Pot` is not the pot a player is playing for, and a stack is a numeral rather than a pile
of chips. This epic closes that gap — and adds the one control the bar has never had, a typed
amount.

It is opened on the human's raw feedback of **2026-09-02**, quoted verbatim below, after a duel was
driven end to end through two browser partitions on that date. **The feedback is the source, not
the specification**: five of its seven items turn on a product question this epic does not answer,
registered as `DEC-114`–`DEC-118` for the **product owner**, who makes them concrete before any
story here is written. That is `CLAUDE.md` rule 5 operating on purpose, not a stall.

## Why now

**Two merged sources already point here.**

[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md)
names the missing clock as a cost it accepted with eyes open: *"a player who wants out of a duel is
**stuck** — no resign, **no turn clock**, and a `PLAYING` room is never reaped, so their exit
depends on their rival still acting."* `ADR-0105` §1 then made *Create a duel room* able to refuse,
which means a rival who walks away now blocks their opponent from starting anything else. The clock
is what unsticks that, and it is the largest single item in this epic.

[`ADR-0091`](../../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §5 registers
design debt rather than forgiving it, and its second clause is exactly what this epic is made of:
a reopening `EPIC-06` story for every uncarded `Screen` member *"plus the carded-screen accretions
the planner judges worth a card"*. Nothing here adds a screen — every item adds a surface to one
that already has a card, which is the harder case to notice. `design/screens/duel-table.html` and
`duel-table-states.html` draw the table as it stands and not one of the states below, so every item
owes a card first — see *Design first*.

**And the product is at the point where legibility is what is left.** `EPIC-01` through `EPIC-06`
are closed, `EPIC-12`'s cycle has run three UAT rounds to `PASS`, and the defects that remain are
not correctness — the last two tickets on `develop` moved a duel table by fractions of a pixel.
What a round cannot find is a table that is *right and mute*, because no rubric criterion asks
whether a player can feel the hand happening. The human played it and could.

## The feedback, verbatim

> - I want player to be higlighted when is their turn; it shoud be some animation like pulsing or
>   running circle
> - pot size is not correct; it shoud include all bets including the latest one that was just made
> - opponent last action shoud be visible on the screen; probably some icon plus info(like for
>   check icon will be ok, for raise/bet icon plus size info)
> - player shoud have 30seconds for move + 3m timebank; timebank also work for disconnection case;
>   clock shoud visibly change each second
> - when player create a duel it shoud be redirected to room immidietly; copy link/invite btn shoud
>   be where the table is(on the table, it shoud be drawn) and opponent icon shoud say:"waiting for
>   oppent", after opponent join duel starts;
> - player shoud have visible representation of their stack in chips; when bet is maid chips going
>   to the pot; when player won chips goin to their stack; shoud be animated
> - player shoud be able to make bet using raw text input
>
> Each new functionality have to be added to desings first then implemented; desings shoud include
> all possible states(like time is running out; regular time ect..)

## Scope

Seven items. The **Decides it** column is what this epic is blocked on, and the distinction it
draws is the whole reason the epic opens `backlog`: two items need only a card and the human's
eye, five need a merged answer first.

| # | Item | Touches | Decides it |
| --- | --- | --- | --- |
| 1 | The acting seat is marked, and the mark moves | `web-client`, `design` | **A card.** Taste is the human's by `ADR-0024` §3 — *pulsing or running circle* is an option offered, not a decision owed |
| 2 | `Pot` names the pot the player is playing for | `web-client` | **`DEC-114`** — product owner |
| 3 | The rival's last act stands on screen | `web-client`, `design` | **`DEC-117`** — product owner |
| 4 | 30 s a move, a 3 m timebank, and a clock that ticks | `poker-server`, `web-client`, `design` | **`DEC-115`** — product owner; the mechanism follows |
| 5 | Creating a duel lands the host at the table | `web-client`, `design` | **`DEC-116`** — product owner |
| 6 | A stack is chips, and chips move | `design`, `web-client` | **A card.** Pacing follows `ADR-0102`'s shape — the client owns it and states no fact the server did not send |
| 7 | A bet amount can be typed | `web-client`, `design` | **`DEC-118`** — product owner, for the illegal-amount case only |

## What is already true

Measured on `develop` at `6d2b8430` while this epic was written, so that no story here re-discovers
it and no ticket is sized against a guess.

**Item 2 needs no wire change and no engine change.** `PlayerView` carries each seat's
`committedThisStreet` (`PlayerView.kt:108`), and `web-client/src/lobby/Lobby.tsx:154` **already
sums it** into `potIncludingStreet`, which it hands to the `ActionBar` for the sizing row
(`ADR-0100` §6, `ADR-0101` §1). `PotStrip.tsx` prints `view.pot` instead, deliberately — *"the pot
is `view.pot` and not a sum of what the seats put in"*. So the same screen already computes both
numbers and shows the smaller one, and the `pot` preset is a fraction of a quantity the strip does
not print. `GameState.potTotal` exists in the engine and is not on the view. **This is a display
decision, not a defect** — which is why it is `DEC-114` and not a bug ticket under `EPIC-12`.

**Item 4 has nothing to build on.** No clock, deadline or timebank exists anywhere in the engine,
the server or the client. The only countdown on `develop` is `presence-countdown.ts`, which is
`ADR-0013`'s **disconnect grace window** and a different thing — though the feedback's *"timebank
also work for disconnection case"* deliberately points the two at each other, which is the sharp
half of `DEC-115`. `ADR-0102`'s *"the client owns the clock"* is about **runout pacing** and does
not reach a turn clock: a turn clock decides whether a player forfeits, so `ADR-0002` puts it on
the server, and the engine's purity bars a clock from `poker-engine` outright.

**Item 7 has no input element.** `ActionBar.tsx` renders `<button>` only. `ADR-0100` §5 refuses *by
name* a driver-only **slider**, any test-only prop or `data-testid` — a typed field is the opposite
of all three, a real player control, and is welcome. But `ADR-0100`'s whole point was that the e2e driver
must **press what a player presses** rather than set a value; a text input hands back a settable
value, and the driver may not start using it. Any story here says so.

**Item 6 has no art.** `design/graphics/` holds the coin, the suits and the wordmark. There is no
chip. `web-client/src/table/chips.ts` is digit grouping, not a chip.

**Item 5 sits on top of a shipped promise.**
[`ADR-0073`](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)
§3 tells the host on screen that the room stays open and they may walk away, and `ADR-0105` §2
leans on exactly that sentence to *not* refuse a `WAITING` seat. Moving the host to the table does
not by itself break either, but the waiting screen is where both promises are made, so `DEC-116`
has to say what becomes of them.

## Design first

The human's closing line — *"Each new functionality have to be added to desings first then
implemented; desings shoud include all possible states"* — is **already the merged rule**, and this
epic does not re-legislate it.
[`ADR-0091`](../../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2: a story
that puts a new surface in front of a player names its card in `## Design notes`, and **if no such
card exists, the split's first ticket is the card**. `ADR-0091` §2 also states the extension this
epic lives on — *"A new player-facing surface behind an existing slug is the same rule applied by
the planner's judgment"* — because nothing here adds a member to the `Screen` union. Every item in
this epic is a new surface behind `first`.

Two things this epic does add, both of them applications of the rule rather than amendments:

1. **A card draws every state of what it draws**, named. The human's example is the clock: regular
   time and running out are two drawings, not one drawing with a note. For item 1 that is *acting*
   and *waiting*; for item 4, at least *regular*, *running out*, *on timebank* and *expired*; for
   item 3, one drawing per act the rival can make. A card showing one state of a control that has
   four is the arrears `ADR-0091` §5 was written about.
2. **The card is merged before the ticket that implements it is startable.** `ADR-0091` §2 puts the
   card first in the split; this says the ordinary rule about merged sources applies to it, so a
   coder transcribing a card is never transcribing an unreviewed one.

Taste stays where `ADR-0024` §3 put it: the human's, given by looking at the rendered card. Items 1
and 6 are decided that way entirely and are registered as no `DEC` at all, because *pulsing or
running circle* is a choice between two drawings and the human is the one who looks.

## Out of scope

- **Resigning a duel.** `ADR-0105` named *"no resign"* and *"no turn clock"* in the same breath;
  this epic takes the clock only. A resign button is a product decision nobody has asked for, and a
  clock that folds an absent player reaches most of the same cost.
- **Reaping a `PLAYING` room.** `ADR-0105`'s third named cost. A turn clock changes what a stalled
  room *does*, and may make reaping reachable — but the sweep is `ADR-0025`'s and belongs with it.
- **`DEC-111`** — whether one player may hold several `WAITING` rooms. Item 5 moves the host off
  the waiting screen and will be tempting to widen into that; it is `ADR-0105`'s open question and
  stays there.
- **The engine.** Nothing in this epic opens `poker-engine`. `potTotal` already exists; a clock may
  not enter a pure library; chips and marks are drawings. If a story here finds it needs the engine,
  that finding is a `DEC` and not a wider ticket.
- **A settable value for the e2e driver.** See *What is already true*, item 7.
- **Sound.** Nothing in the feedback asks for it and the vision does not.

## Open decisions

All five are the **product owner's**, and all five are registered open in
[`docs/adr/README.md`](../../docs/adr/README.md). The epic is `backlog` until they are answered
because five of the seven items cannot be split into tickets without them.

| ID | Question | Gates |
| --- | --- | --- |
| `DEC-114` | What does `Pot` name on the table — the collected pot, or the total including this street? | Item 2 |
| `DEC-115` | What happens when a player's clock runs out, and how does the timebank meet `ADR-0013`'s disconnect grace? | Item 4 |
| `DEC-116` | Does creating a duel land the host at the table, what stands in the rival's seat until they arrive, and what becomes of `ADR-0073` §3's promise? | Item 5 |
| `DEC-117` | What does the table say about the act just made, and how long does it stand? | Item 3 |
| `DEC-118` | What becomes of a typed amount that is not a legal raise? | Item 7 |

### What the answers will hand the architect

Named here rather than registered, following
[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6,
which stated what must be true and registered the mechanism as a separate `DEC` from the answering
ADR rather than from the epic. *A `DEC` nobody is working is noise in the open table* (`STORY-1211`)
— these are certain, but nobody works them until `DEC-115` lands.

**The clock's mechanism is an architect's `DEC` and the answering ADR should register it.** It is
the only item here that moves the wire: a deadline the client can count down to must be a fact the
server sends, which is a `PROTOCOL_VERSION` bump and therefore an `atomic:` ticket sized by
`ADR-0070`'s probe. It also has to settle what the client does between frames — `ADR-0102` licensed
a client-owned clock for pacing, and a ticking countdown is the same shape applied to a
server-stated deadline — and whether a timeout is a server-synthesised act or a new room event.
`ADR-0047`'s one-bumping-branch-at-a-time lock applies, so item 4 serialises against anything else
that moves the version.

## Stories

Written once `DEC-114`–`DEC-118` are merged. Splitting before then would be inventing the answers.

| ID | Title | Status |
| --- | --- | --- |
| — | *none yet — blocked on `DEC-114`–`DEC-118`* | — |

## Definition of done

- [ ] `DEC-114`–`DEC-118` are answered by merged ADRs.
- [ ] Every story is `done`.
- [ ] Every surface this epic adds is drawn on a card under `design/` **before** its implementing
      ticket is startable, and each card draws every state of what it draws (`ADR-0091` §2).
- [ ] A duel driven end to end shows, without a human reading the code: whose turn it is, how long
      they have, what their rival just did, a pot that matches the sizing row's base, and chips
      that move.
- [ ] A player who stops acting no longer holds their rival's duel open indefinitely.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
