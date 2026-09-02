---
id: EPIC-13
title: The living table — the turn clock, the chips, and the act just made
type: epic
status: ready
labels: [client, design, table, server]
---

## Goal

Make the duel table say what is happening, when it happens, and how long the player has to answer.

Today the table is correct and quiet. Every fact on it is one the server stated, and a player who
already knows Hold'em can follow a hand. What it does not do is *tell* them: nothing marks whose
turn it is beyond two words, nothing counts down, nothing says what the rival just did, the number
labelled `Pot` is not the pot a player is playing for, and a stack is a numeral rather than a pile
of chips. This epic closes that gap — and adds the one control the bar has never had, a typed
amount, and settles what the address says while a player is at the table.

It is opened on the human's raw feedback of **2026-09-02**, quoted verbatim below, after a duel was
driven end to end through two browser partitions on that date. **The feedback is the source, not
the specification**: six of its eight items turn on a product question this epic does not answer,
registered as `DEC-114`–`DEC-119` for the **product owner**, who makes them concrete before any
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

Added by the human later the same day, and taken as an eighth item:

> oh and one more important item: when page is refreshed user shoud stay on the same page (refresh
> duel page shoud not redirect to lobby; and same for all other pages)

## Scope

Eight items. The **Decides it** column is what this epic is blocked on, and the distinction it
draws is the whole reason the epic opens `backlog`: two items need only a card and the human's
eye, six need a merged answer first.

| # | Item | Touches | Decides it |
| --- | --- | --- | --- |
| 1 | The acting seat is marked, and the mark moves | `web-client`, `design` | **A card.** Taste is the human's by `ADR-0024` §3 — *pulsing or running circle* is an option offered, not a decision owed |
| 2 | `Pot` names the pot the player is playing for | `web-client` | **`DEC-114`** — answered by [`ADR-0107`](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md): the total |
| 3 | The rival's last act stands on screen | `web-client`, `design` | **`DEC-117`** — answered 2026-09-02 by [`ADR-0109`](../../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md); splittable |
| 4 | 30 s a move, a 3 m timebank, and a clock that ticks | `poker-server`, `web-client`, `design` | **Answered** — [`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md): an expiry checks or folds the one decision and never forfeits the duel, and the timebank replaces the grace window; the mechanism is `DEC-120`, **answered 2026-09-02 by [`ADR-0113`](../../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md)** — one `TurnClock` frame, a derived deadline, a synthesised act, one sweep; splittable into a card, one `atomic:` wire ticket and one drawing ticket |
| 5 | Creating a duel lands the host at the table | `web-client`, `design` | **`DEC-116`** — answered by [`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md): the host waits at the table |
| 6 | A stack is chips, and chips move | `design`, `web-client` | **A card.** Pacing follows `ADR-0102`'s shape — the client owns it and states no fact the server did not send |
| 7 | A bet amount can be typed | `web-client`, `design` | **`DEC-118`** — answered by [`ADR-0111`](../../docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md): refused, and it says why |
| 8 | A refresh leaves the player where they were | `web-client` | **Answered** — [`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md): only a running duel refuses another screen and the refusal restores the address; a waiting or finished room honors the ask; the mechanism is `DEC-123`, the architect's |

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

**Item 8's reported symptom does not reproduce, and the defect that does is its inverse.** Driven
on 2026-09-02 against this branch's `develop`, on a dev server and an isolated database, with
`scripts/qa/drive.mjs` and `location.reload()`:

| What was refreshed | Result |
| --- | --- |
| Host **in a live duel**, URL bare `/` | **Survives** — same hand, same stacks, no lobby |
| Rival in a live duel, URL `?room=CODE` | **Survives** — URL and table both intact |
| Host on the **waiting screen** | **Survives** — same room code |
| `#/leaderboard` on a browser holding **no** room | **Survives** — hash and screen intact |
| `#/leaderboard` on a browser **holding a room** | **Fragment erased.** `location.hash` reads empty within 2 s and the room's screen stands |

So a refresh already keeps a player where they were: `boot.ts` writes the room code to storage on
`RoomJoined` and re-sends `JoinRoom` on the next socket's `Welcome`, and `use-screen.ts` reads
`window.location.hash` at boot, so both halves of *stay on the same page* are shipped. No lobby
flash was observable at the sampling resolution `drive.mjs` allows.

**What is broken is the other direction: holding a room overrides the address and erases it.** A
player in a duel who asks for `#/duels`, `#/leaderboard` or `#/account` is returned to the table
with an empty fragment, so those three screens are unreachable mid-duel and the address bar stops
naming where the player is. That may well be intended — `ADR-0105` §3 hands a player back *"the
table, never a lobby"* — but **erasing the address is not the same act as refusing the
navigation**, and no merged source settles which one the product performs. That is `DEC-119`.

**The unreproduced report is the story's first job, not a dismissal.** The human saw something; a
localhost dev server with no latency, one machine and a warm socket is the weakest place to look
for it. Paths this run did **not** cover: a duel played to its end and refreshed on the result
screen, a refresh during a runout, a reconnect after the socket has actually dropped, and any
network slow enough to make the rejoin round-trip visible.

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
   four leaves the same debt `ADR-0091` §5 registers, in a smaller shape.
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

**None — all six are answered.** They were the **product owner's**, and all six were settled on
2026-09-02 by the `product-owner` agent; the ADRs are listed under *Answered*, below, and struck in
[`docs/adr/README.md`](../../docs/adr/README.md). The epic was `backlog` until they landed, because
six of the eight items could not be split into tickets without them. It is now splittable.

Two **architect's** decisions were registered by the answering ADRs and gate the wire work rather
than the split — see *What the answers handed the architect*:

| ID | Question | Registered by |
| --- | --- | --- |
| `DEC-123` | By what mechanism is a mid-duel navigation refused and the address restored? | `ADR-0112` |

**And one more, raised by the split on 2026-09-02: `DEC-124`, the product owner's** — does a surface this product animates owe a **still form** for a player whose system asks for reduced motion? Items 1 and 6 are the product's first continuous motion, and no merged source, token or ADR reaches it: `ADR-0102` §4 licensed a client-owned display *schedule* and says in as many words that it *"fixes no duration, no animation and no transition"*. This epic's own line — that items 1 and 6 are *"decided that way entirely"* by the human's eye — is about **which drawing**, and a media query is not a drawing. It blocks the implementing tickets of `STORY-1303` and `STORY-1306`, and nothing else.

### Answered

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-114` | [ADR-0107](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) | `Pot` names the **total**: `view.pot` plus both seats' `committedThisStreet` — `Lobby.tsx:154`'s `potIncludingStreet`, the same `P` the sizing row already uses, so item 2's strip prints the number the `pot` chip sizes against. One figure, one word, and the blinds are in it from the first frame (`Pot 150` at 50/100, never `Pot 0`). Item 2's card correction is measured in the ADR's §6: `duel-table.html`'s two `Pot 2,450` nodes read **2,850**; `duel-table-states.html`'s 3,250 already agrees. The never-derives guard admits exactly this one sum (ADR §5), no wire moves, and whether the bet-lines keep standing under item 6's chips is that card's question, not resettled |
| `DEC-115` | [ADR-0108](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md) | An expiry gives up **one decision** with `ADR-0023`'s conduct — fold facing a bet, check when checking is free — and never ends the duel; the coin moves only on the outcome the engine reaches. 30 s a decision plus one 3 m timebank per duel, and the timebank **replaces** `ADR-0013`'s grace window, so there is one clock and the duel never pauses. The mechanism is `DEC-120`, the architect's |
| `DEC-117` | [ADR-0109](../../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md) | **The table marks the last act, and the next deal clears it.** One mark — the most recent of the six acts, at the seat that made it, never one per seat — saying what that seat's own button said: `actionVerb`'s verb, with the event's own `to` total on `Call`/`Bet`/`Raise to`/`All in`, `Fold` and `Check` bare, nothing computed. Within a hand it is only ever replaced; street ends, timers, fades and presence never touch it; the next hand's deal **as painted** (`ADR-0102` §1's queue) removes it, so a fold's mark stands through the award window, and `DuelFinished` retires it. Item 3's card owes **six states**, one per act, four with a figure — icon-versus-text and placement stay the human's by `ADR-0024` §3 — and item 3 is now splittable. A refresh loses the mark until the next act (`PlayerView` carries no last-act field); accepted in the ADR, registered nowhere. |
| `DEC-116` | [ADR-0110](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) | Creating a duel seats the host **at the table**: the dedicated waiting screen is retired, the rival's empty seat says **`Waiting for your rival`** — the shipped heading relocated, rendered once, at the seat — and the table states **no game fact** before the opening `Snapshot`: no stacks, cards, pot, button or action bar. **Both of `ADR-0073`'s promises move verbatim and keep being made** — `Back to the lobby`, and *"The room stays open. That link still works for your rival, and it brings you back."* — so `ADR-0105` §2's ground is unchanged. The invite moves **whole** (bare code, selectable link box, `Copy the link` with its two feedback lines), the state adds **zero new strings**, the arrival is the `Snapshot` and is silent, and item 5's card owes the host-alone frame in **four named variants** under `ADR-0103`'s budget. `DEC-111` and `DEC-119` untouched |
| `DEC-118` | [ADR-0111](../../docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md) | The table **refuses the press and says why in the server's own numbers — never clamps, never knowingly sends**. Under-floor and over-`allInTo` are one case, refused with `rejection-text.ts`'s merged sentences quoting the violated bound from this turn's `LegalActions`; a non-number is `That is not an amount.`, never coerced; nothing typed is ever rewritten and no act is converted (a typed `callTo` is not a `Call`, an over-stack amount is not an `AllIn`). For item 7's split: the card draws the *outside the interval* and *not an amount* states before the implementing ticket is startable (ADR §7), the action button may print the proposal or nothing but never a different amount, and **`ADR-0100` §5 stands in full** — the driver gains no typing branch, so no story here touches `drive-duel.tsx`'s amount path. `DEC-102` stays open with its ground shifted: every legal total is now typeable |
| `DEC-119` | [ADR-0112](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) | **Only a running duel refuses another screen, and the refusal restores the address.** The address names the screen the player chose and never the room — no `Screen` member for the table, and the address is not expected to name a duel (`ADR-0076` §§1–2 applied). Mid-duel (`PLAYING`, grace included) an ask for `duels`, `leaderboard`, `account` or a mailed screen moves nothing: the table stays and `/` is restored — one act, silent, no new string, which confirms the behaviour item 8 measured on a playing browser as intent. A `WAITING`, `FINISHED` or `ABANDONED` room **honors** the ask with the seat, the tab's memory and the socket untouched, and a frame that seats a duel overrules any chosen screen exactly as `ADR-0076` §3 wrote it — phrased in room states, so `DEC-116` may put the waiting host anywhere. `ADR-0086` §6's accept lands on the account screen it names (the derived bounce is resolved in its favour), and a mailed link refused mid-duel must not spend its token. Item 8's story owes the DoD reproduction attempt on the undriven paths — the result screen, a runout, a dropped socket, real latency, the `AccountOffer` anchor, a mailed link over a held room — and the mechanism is `DEC-123`, the architect's, registered in the ADR's §7 |
| `DEC-120` | [ADR-0113](../../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md) | **The architect's**, and item 4's mechanism. One new `ServerMessage`, **`TurnClock`** (`seat`, `handNumber`, `actionSequence`, `turnRemainingMillis`, `bankRemainingMillis` for both seats), stating **what is left, never an instant** — `Snapshot` and `Events` were disqualified because they carry engine types and no clock may enter `poker-engine`, `YourTurn` because it reaches one seat. One frame per write-back that moves the live decision point, plus one to a resuming seat in the critical section that already builds its presence frames. The deadline is **derived from the decision point, never armed**, so the act/sweep race needs no cancellation: both take the room's own mutex, and whichever loses either finds a restarted clock or gets `Rejected` by the engine's sequence guard. An expiry is a **synthesised act down the ordinary act path** — `foldAbsent`'s single-seat body extracted and reused, so `ADR-0023`'s conduct, `ActedForAbsent`'s mark and the coin's single settle path hold by construction — and `ADR-0025`'s ticker keeps two steps with `expireGracePeriods()` replaced by a one-pass `expireTurnClocks()`. The client **anchors on arrival and paints on apply**, which amends `ADR-0102` §6 by refusing the queue exemption it promised. `isPaused`, the `DUEL_PAUSED` enum entry and `graceRemainingMillis` are deleted; `absentSeats`, `foldAbsent` and `secondsRemaining` are kept verbatim. **For the split**: item 4 is a card, then one `atomic:` ticket sized by `ADR-0070`'s probe carrying the wire, the server and the client's store, then one `web-client` ticket that draws the countdown; the bump is claimed under `ADR-0047`'s lock with neither number nor fingerprint named anywhere in advance, and `docs/test-plan.md`'s `CORE-23` is the one artifact no gate will catch |

### What the answers will hand the architect

Named here rather than registered, following
[`ADR-0105`](../../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6,
which stated what must be true and registered the mechanism as a separate `DEC` from the answering
ADR rather than from the epic. *A `DEC` nobody is working is noise in the open table* (`STORY-1211`)
— these are certain, and `DEC-115` has landed.

**The clock's mechanism is registered: `DEC-120`, the architect's, by
[`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md) §6.** It is
the only item here that moves the wire: a deadline the client can count down to must be a fact the
server sends, which is a `PROTOCOL_VERSION` bump and therefore an `atomic:` ticket sized by
`ADR-0070`'s probe. It also has to settle what the client does between frames — `ADR-0102` licensed
a client-owned clock for pacing, and a ticking countdown is the same shape applied to a
server-stated deadline — and whether a timeout is a server-synthesised act or a new room event.
`ADR-0047`'s one-bumping-branch-at-a-time lock applies, so item 4 serialises against anything else
that moves the version.

## Stories

`DEC-114`–`DEC-119` are merged, so the split ran on 2026-09-02: **eleven stories**, written but not yet split into tickets. The first ticket of each story that puts a new surface in front of a player is its **design card** (`ADR-0091` §2), and the card merges before the ticket that implements it is startable.

**The eight items are the seam, with two exceptions.** Item 4 splits into **three** — its card, its server half and its client half — because the card is the only part of it no decision blocks, and keeping it in one story would have parked the epic's largest item behind `DEC-120`. Item 8 splits into **two** for the same reason: `ADR-0112` §6's reproduction attempt needs no decision, and the guard it feeds waits on `DEC-123`. Nothing merged: each item owns a card, a lifetime and a human's visual verdict of its own.

**The order is a single chain, and it is not arbitrary.** `STORY-1301` first because it changes a pot figure **every later card copies**. `STORY-1302` second because `ADR-0110` makes a table that is not a duel, and every later surface must say what it shows there — a tax paid once, early. Then the two seat marks and the bar, in the order in which each card proves `ADR-0103`'s phone fit **against everything merged before it**. `STORY-1310` runs before `STORY-1302`'s successors touch the waiting state, so its measurement is against the product the human reported on.

| ID | Title | Status |
| --- | --- | --- |
| [STORY-1301](../stories/STORY-1301-pot-names-every-chip-committed-to-the-hand.md) | `Pot` names every chip committed to the hand — *item 2; a two-node card correction, no new surface* | **ready — the one story startable now** |
| [STORY-1302](../stories/STORY-1302-the-host-waits-at-the-table.md) | The host waits at the table, and both promises move with them — *item 5; card owes 4 host-alone variants + the arrival* | ready — waits on `STORY-1301` |
| [STORY-1303](../stories/STORY-1303-the-acting-seat-is-marked-and-the-mark-moves.md) | The acting seat is marked, and the mark moves — *item 1; card owes acting + waiting; raises `DEC-124`* | ready — card startable, implementing tickets blocked on `DEC-124` |
| [STORY-1304](../stories/STORY-1304-the-table-marks-the-last-act.md) | The table marks the last act, and the next deal clears it — *item 3; card owes six states, four with a figure* | ready — waits on `STORY-1303` |
| [STORY-1305](../stories/STORY-1305-a-bet-amount-can-be-typed.md) | A bet amount can be typed, and an illegal one is refused in the server's own numbers — *item 7* | ready — waits on `STORY-1304` |
| [STORY-1306](../stories/STORY-1306-a-stack-is-chips-and-chips-move.md) | A stack is chips, and chips move — *item 6; the chip is **minted** interactively (`ADR-0091` §3); `DEC-124`* | ready — minting and card startable, client tickets blocked on `DEC-124` |
| [STORY-1307](../stories/STORY-1307-the-turn-clocks-card.md) | The turn clock's card — regular, running out, on timebank, expired — *item 4a; the half no decision blocks* | ready — waits on `STORY-1306` |
| [STORY-1308](../stories/STORY-1308-the-server-states-a-deadline-and-plays-the-expired-seat.md) | The server states a deadline and plays the seat whose clock ran out — *item 4b; the **only** wire move in this epic; `atomic:` by `ADR-0070`'s probe, `ADR-0047`'s lock* | **blocked on `DEC-120`** — the architect's |
| [STORY-1309](../stories/STORY-1309-the-table-counts-down-and-the-pause-leaves-the-screen.md) | The table counts down, and the pause leaves the screen — *item 4c* | **blocked on `DEC-120`** — the architect's |
| [STORY-1310](../stories/STORY-1310-the-refresh-paths-nobody-drove.md) | The refresh paths nobody drove, driven and written down — *item 8a; `ADR-0112` §6's six paths* | ready — decision-free, waits on `STORY-1301` |
| [STORY-1311](../stories/STORY-1311-only-a-running-duel-refuses-another-screen.md) | Only a running duel refuses another screen, and the refusal restores the address — *item 8b* | **blocked on `DEC-123`** — the architect's |

**One decision was raised by the split**, and it is the only thing in this epic the answered six did not cover:

| ID | Question | Whose | What it blocks |
| --- | --- | --- | --- |
| `DEC-124` | Does a surface this product animates owe a **still form** for a player whose system asks for reduced motion, and what governs it? Items 1 and 6 introduce the product's **first continuous motion**; nothing in `docs/vision.md`, `docs/adr/` or `design/tokens/tokens.css` says anything about motion preferences, and `ADR-0102` §4 *"fixes no duration, no animation and no transition"*. It is **not** a choice between two drawings, so `ADR-0024` §3 does not place it with the human's eye — a card cannot render a media query | **The product owner's** | The **implementing** tickets of `STORY-1303` and `STORY-1306` only. Not their cards, not the minting, not this epic |

## Definition of done

- [ ] `DEC-114`–`DEC-119` are answered by merged ADRs.
- [ ] `DEC-120`, `DEC-123` and `DEC-124` are answered by merged ADRs.
- [ ] Every story is `done`.
- [ ] Every surface this epic adds is drawn on a card under `design/` **before** its implementing
      ticket is startable, and each card draws every state of what it draws (`ADR-0091` §2).
- [ ] A duel driven end to end shows, without a human reading the code: whose turn it is, how long
      they have, what their rival just did, a pot that matches the sizing row's base, and chips
      that move.
- [ ] A player who stops acting no longer holds their rival's duel open indefinitely.
- [ ] The reported refresh symptom is either reproduced and fixed, or recorded as not reproducible
      with the paths that were tried written down — `EPIC-13` does not close on a symptom nobody
      looked for again.

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
