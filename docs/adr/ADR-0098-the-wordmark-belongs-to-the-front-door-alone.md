# ADR-0098 — The wordmark belongs to the front door alone

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-099` — where does the product's wordmark belong: on every screen, or on the
  front door alone? **Derived from the merged cards, confirmed by the vision; the human did not
  state this call.** The cards under `design/screens/` already draw the answer — eleven card files
  hold exactly one `.mark` between them, on `create-duel.html`'s front-door frame (line 115) —
  so this ADR mostly *records a reading* rather than adds a rule, and says so, because a decision
  that reads a merged source is cheaper for everyone than one that invents. The vision sentences
  that confirm the reading are *Positioning* — *"Dark, quiet, fast, minimal"* — and the first
  success condition, *"We play a full heads-up match"*, which happens on the screens the mark
  stays off
- **Builds on:** [`ADR-0033`](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) (the
  lockup's anatomy is card-born in `design/graphics/wordmark.html`; that ADR settled what the
  wordmark *is* and deliberately not where it *appears* — this one settles the remainder),
  [`ADR-0060`](ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md) §5 (a duel
  outranks every other surface, and the navigation-bar alternative was refused partly because *"a
  bar rendered above a duel table would be a way out of a hand in progress"* — chrome above the
  table is a shape this project has already declined once),
  [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §3a (a
  shipped screen is judged against its merged card, which is what makes the cards' omissions
  load-bearing rather than accidental), and
  [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md) (opening the invite is taking
  the seat — the merged fact that makes this decision's sharpest cost real)
- **Constrains:** the follow-up client tickets that `TASK-121004`'s struck third scope item
  becomes — §4 names its four-file set — and, until it lands, any reading of
  `web-client/src/App.tsx`'s `<h1>` as intended rather than as arrears
- **Amends nothing:** no card changes (§5), no engine, server or wire change, no
  `PROTOCOL_VERSION` step, no `docs/test-plan.md` `expect`
- **Raises nothing** for the architect: the one choice left open — which element carries the
  lockup, and what a screen reader is given as its accessible name — is ordinary implementation
  under merged guidance (`ADR-0033`'s anatomy gate; `CoinMark` is already `aria-hidden`), decided
  in the follow-up ticket under review, not in a register

## Context

`web-client/src/App.tsx` renders `<h1 className="text-title">Poker Duels</h1>` above `<Lobby />`,
unconditionally. `Lobby` is the client's whole router, so that heading sits on every surface the
product has: the front door, the waiting room, the duel table, the result screen, the four record
and account screens (duels, leaderboard, account, sign-in) and the two mailed screens (verify,
reset).

`TASK-121004`'s third scope item quotes that markup and directs dressing it as
`design/screens/create-duel.html`'s front-door frame draws — the coin, a bold *Poker*, a muted
*Duels*. But the markup does not live where the ticket looked: dressed in place, the front door's
wordmark becomes every screen's wordmark; moved to the front door, ten surfaces lose the only
on-page product naming they have. Either way it changes what a player sees everywhere, which is
not a dressing change on one screen. The coder shipped the ticket's other two scope items
(PR #1234) and refused to guess this one — `CLAUDE.md` rule 5, routed here as `DEC-099`.

The forces in tension:

- **What the merged cards draw.** Across all eleven card files, one frame draws `.mark`:
  `create-duel.html`'s *"Before — the front door"*. The four secondary-screen cards — `duels`,
  `leaderboard`, `account`, `sign-in` — were composed *while the shipped screens rendered the
  global `<h1>` above them*, and none drew it: the omission was made four times over by authors
  looking at screens that had the heading, which makes it a choice, not a gap. Even
  `create-duel.html`'s own second frame — the waiting room — omits the mark: its top is the room
  code, drawn at `1.875rem` mono because the card's lede wants it *"big enough to read across a
  room"*. The table, result, rematch and join cards draw no product name anywhere. Under
  `ADR-0092` §3a these cards are what shipped screens are judged against.
- **Naming persistence pulls the other way, and it is not trivial.** `ADR-0094` made opening the
  invite taking the seat: the invited player — half of everyone who ever plays — lands in a dealt
  hand without crossing the front door. Today the global `<h1>` is the only on-page place the
  product names itself to that player; off the front door, only the tab's `<title>Poker Duels</title>`
  remains. A product that never introduces itself to the person it most wants to convert is a
  real cost, and pretending otherwise would make the rest of this ADR untrustworthy.
- **The vision's screen priorities.** The success condition is played out on the table and result
  screens; *Positioning* asks for *"dark, quiet, fast, minimal"*; and `ADR-0060` §5 has already
  refused persistent chrome above the table once, for navigation.

## Decision

**1. The wordmark renders on the front door alone.** The front door is the first screen's
pre-create branch — `ADR-0060` §5's *"branch with the create button"* — the screen
`create-duel.html`'s front-door frame draws. There it renders as the card draws it: the coin mark
and two separate text elements, a bold *Poker* and a muted *Duels*, anatomy per `ADR-0033`'s
canonical card.

**2. No other screen or state renders it.** Not the waiting room, whose top the card gives to the
room code; not the table or the result; not duels, leaderboard, account or sign-in, whose cards
draw their own headings and no mark; not verify or reset, which have no cards yet (`ADR-0091` §5
registers that debt) and inherit the rule meanwhile. The unconditional
`<h1>Poker Duels</h1>` leaves `App.tsx`, and no product-name chrome replaces it anywhere.

**3. This is a reading of merged sources, and it is recorded as one.** Eleven cards, one mark.
The shipped global heading was the arrears — chrome no card draws — not the cards. Recording the
reading matters mechanically: an answered question becomes a merged source (`ADR-0092` §5), so a
later proposal to put the wordmark anywhere else is an amendment that must supersede this ADR
*and* move the cards, never a quiet addition inside a ticket.

**4. What `TASK-121004`'s struck third scope item becomes — the file set, for the planner.** The
planner writes the ticket; this section is what it needs to know. Four files:

| File | What changes |
| --- | --- |
| `web-client/src/App.tsx` | The `<h1 className="text-title">Poker Duels</h1>` leaves. The `<main>` shell and its classes stay |
| `web-client/src/lobby/Lobby.tsx` | The pre-create branch gains the lockup. `CoinMark` exists (`web-client/src/result/CoinMark.tsx`, already `aria-hidden`); whether it is reused or the coin is drawn at the lockup's own em sizes is the coder's call under `ADR-0033`'s drift gate |
| `web-client/src/App.test.tsx` | **Yes — the plain-title assertions have to change.** Four assertion sites across three tests: `renders the application heading`, `gives the heading a token-derived class`, and the same two assertions repeated inside `leaves the lobby exactly as it was for a player who never opens the record`. Nothing will render a heading whose text is `Poker Duels` with class `text-title` after this lands, so they are rewritten deliberately against the front door's lockup (or retired in favour of the Lobby-level assertions), never mechanically kept |
| `web-client/src/lobby/Lobby.test.tsx` | Gains the assertion `TASK-121004` described — the coin mark and two separate text elements on the front-door branch. Note `renders the lobby with no headings from the name surface` asserts **zero** headings on the bare front door today; if the lockup is carried by a heading element, that expectation changes while its intent (NameSurface adds no headings) is preserved |

The behaviour in §§1–2 is the decision; the mechanism is the ticket's. The natural one — the
lockup lives in the branch that *is* the front door, so nothing needs to know which screen is
showing — needs none of the plumbing PR #1234's report worried about, and the ticket should say
so to spare the coder the same detour.

**5. No card is in arrears.** No card gains or loses a line under this decision; the eleven
merged cards already draw it. The arrears ran the other way — the shipped client drew chrome no
card draws — and the follow-up client tickets retire it. (They are two, not one: `TASK-121011`
takes the `h1` out of `App.tsx` and `TASK-121012` gives the front door the lockup. The planner
probed for a merged gate that would refuse the smaller commit, found none, and split under
`ADR-0068` §4 rather than claim an `atomic:` exemption it had just disproved.) (The rejected *every screen* answer
would have put ten cards in arrears at once, four of them merged this same week.)

## Consequences

**What it costs, plainly:**

- **The invited half of the product never reads the product's name on-page.** `ADR-0094` seats
  them straight into a dealt hand; they can play a whole duel, lose, hit *Rematch* and close the
  tab having met the name only in the browser tab's title. This decision refuses the cheap fix
  (persistent chrome) on purpose, and it does **not** design a substitute — if the product ever
  wants to introduce itself to the player who was challenged, that is a new question for whoever
  asks it, answered somewhere the cards would then draw.
- **The client keeps a level-one heading only where the front door provides one.** Everywhere
  else the top heading becomes each screen's own `h2` (`Your duels`, `Leaderboard`, `Account`,
  `Sign in`, `Verify`, the result's verdict) — the reset screen's own `h1` excepted — and the
  duel table has no heading at all. That is an accessibility wart this ADR accepts and names
  rather than hides; whether any screen's own heading is promoted is the follow-up ticket's
  ordinary review question, not a product rule.
- **Four merged assertion sites that have gated since `EPIC-03` are rewritten.** The plain-title
  tests were the guard that the heading stayed; the guard's subject is now leaving on purpose,
  and the rewrite must be deliberate so the lockup inherits real assertions rather than none.
- **The lockup's accessible name is now a thing the ticket must get right.** The card's markup
  concatenates to `PokerDuels` for a screen reader; the coin is decorative and silent. One text
  node's worth of care, but it did not exist while the title was a plain `h1`.

**What it buys:** the table and the result keep the duel as the only thing on them — the screens
the success condition is played on carry no chrome competing with it; every screen now matches
its merged card at the top, so UAT conformance stops owing this question a finding; the waiting
room's top stays the code the card sized to be read across a room; and the invited player's first
screen remains exactly what `ADR-0094` blessed — a dealt hand, nothing above it.

**What it forecloses:** persistent product chrome of any kind — a header bar, a corner mark, a
watermark — on any screen but the front door, unless a later ADR supersedes this one and moves
the cards with it. It also forecloses the wordmark on the waiting room specifically: that frame's
top belongs to the code, by the card's own trade.

**Cheap to reverse, and that is part of why it is decided this way today:** the product has no
players yet, and the lockup is one JSX block in one branch. Moving it later moves markup, not a
design system. The deadline ran the other direction — PR #1234 is in flight and `TASK-121004`
cannot close without knowing what its third item became — so the cost of deciding *now* was zero
and the cost of stalling was a ticket held open.

## Alternatives considered

**On every screen, as a persistent header — dress the `h1` where it stands.** The strongest case:
it is the cheapest diff (one file, and `App.test.tsx` keeps its shape); it preserves the only
on-page naming the invited player ever sees, which matters precisely because `ADR-0094` seats
them past the front door; and the reference products themselves keep a site header on most
screens — Lichess wears its top bar almost everywhere and offers Zen mode to hide it during play,
so the shape is livable, not casino. Rejected because the merged cards draw the opposite eleven
times over, four of those cards composed under the shipped heading by authors who declined to
draw it — adopting this answer would put ten cards in arrears at once and re-litigate work merged
this week; because chrome above the table is the shape `ADR-0060` §5 already refused, on the
screen where *"We play a full heads-up match"* happens; and because Lichess's own answer to the
duel screen is Zen mode — the quiet table is the part of the reference worth copying, and this
product can simply start there rather than build a toggle to reach it.

**On every screen except the table (and result).** The strongest case: naming persistence where
it is free — a player reading the leaderboard is idling, and branding there distracts nobody —
while the duel stays clean. Rejected because no merged source draws it: the four record-screen
cards omit the mark exactly as firmly as the table's do, so this answer invents a rule where
reading suffices, and it mints a new distinction — screens that carry product chrome and screens
that do not — that every future screen would have to be sorted into by someone. The record
screens also already open with their own headings; a wordmark above *Your duels* is two titles
stacked on a 380px column.

**Front door and waiting room — the whole first screen, both states.** The strongest case: the
waiting host is the one player with idle attention, so branding is free there; and the screen
does not rearrange itself the moment a room is created. Rejected because the card already made
this exact trade and recorded it: `create-duel.html` draws both frames and puts the mark on one —
the waiting frame's top is the code at `1.875rem` mono, sized by the card's lede to be read
across a room, and a wordmark above it demotes the one thing that frame exists to show.

**Nowhere at all.** The strongest case: quieter still, and the tab title already names the
product. Rejected because the front-door card draws it, merged and human-accepted — and a front
door that never says whose door it is fails the one job a front door has that a table does not.
