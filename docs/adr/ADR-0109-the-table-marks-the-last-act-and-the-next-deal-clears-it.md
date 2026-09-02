# ADR-0109 — The table marks the last act, and the next deal clears it

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-117` — what does the table say about **the act just made**, and how long does
  it stand? Raised 2026-09-02 by the human after driving a duel end to end — *"opponent last action
  shoud be visible on the screen; probably some icon plus info(like for check icon will be ok, for
  raise/bet icon plus size info)"* — and registered by
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md), whose item 3 it gates.
- **Where the answer came from:** [`docs/vision.md`](../vision.md), *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal."* Lichess
  marks **the last move made at the board** — one mark, either colour, standing until the next move
  and cleared by the next game. Every clause below is that sentence applied to a poker table. The
  words themselves are not coined here at all: they are `web-client/src/table/action-text.ts`'s,
  already merged, whose own contract — *"no action gains a verb the server did not name, and none
  loses one"* — decides the which-acts half too.
- **What stays the human's:** the drawing. Icon versus text versus both, where the mark sits, and
  whether a mark from an earlier street looks different are taste, given by looking at the rendered
  card (`ADR-0024` §3); `EPIC-13` *Design first* already makes that card the split's first ticket.
  §5 fixes only how many states the card owes: **six**.
- **Builds on:** [`ADR-0075`](ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md)
  (the neighbouring mark, whose duration rule does **not** transfer — *Context* says why — but
  whose grounds against timers, fades and welded-frame clearing do);
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §1–2 (the step
  queue that makes *the next painted deal* a real boundary when the frames' arrival is not);
  [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) §4 (the award
  window this mark stands through); `ADR-0046` §4 and
  [`ADR-0028`](ADR-0028-the-wire-names-an-absent-opponent.md) (the server-action line this mark
  coexists with).
- **Constrains:** the split of `EPIC-13` item 3 — its card and its tickets implement exactly §§1–4
  — and no Kotlin, no wire type, no protocol version and no stored data.
- **Registers no new `DEC`.** The one wire-shaped residual — a resuming client has no mark until
  the next act, because `PlayerView` carries no last-act field — is an accepted cost
  (*Consequences*), not a blocked question; *a `DEC` nobody is working is noise in the open table*
  (`STORY-1211`).

## Context

**The table already remembers every act and reads none of them to a player.** The wire carries all
six acts as their own events — `PlayerFolded`, `PlayerChecked`, `PlayerCalled`, `PlayerBet`,
`PlayerRaised`, `PlayerAllIn`, each with the actor's seat and, for the last four, the server's own
`to` total — and the client appends them to `state.narration`, where (`ADR-0102` aside) nothing
renders them. The human played a duel and named the gap. So the question is not what to send or
what the client knows; it is what one standing mark says, and when it stops standing.

**The vocabulary already exists and is load-bearing.** `action-text.ts` fixes the six verbs a
button says — *Fold, Check, Call, Bet, Raise to, All in* — and which buttons carry a figure:
`Call`, `All in`, `Bet` and `Raise to` do, as totals, and the other two do not. A mark that used
different words, or a different figure convention, would put two names for one act on one screen.

**Part of the table already narrates two of the acts.** `seat-status.ts` keeps *Folded* and *All
in* standing on the plate for the rest of the hand. That is the honest case for marking only the
acts a player could miss: a fold is already told, a check is the act that moves nothing and is the
human's own example. The cost of a subset is what *Decision* §1 weighs.

**Two clearing candidates die on the server's delivery, not on taste.** One applied action becomes,
per seat, one `Events` frame and then the `Snapshot` (`ADR-0102`, confirmed fact 1) — so the call
that closes a street rides in the same frame as its own `BettingRoundEnded` and the next
`StreetDealt`, applied in one tick, because *"only a hand's ending is ever paced"* (`ADR-0102` §1).
*Clears at the street's end* therefore erases exactly the act it uniquely governs — the closing
call — at the instant it is made. And a hand-ending act arrives welded to the **next hand's whole
deal** in one batch (`ADR-0102`, missed fact 4). This is the same welded-frame trap `ADR-0075`
rejected `Snapshot`-clearing on. What rescues the hand boundary — and only the hand boundary — is
`ADR-0102` §1's step queue: frames arriving while ending steps remain are held and applied when the
last step has stood, so *the deal as painted* is a moment a player can see even though the deal as
delivered is not.

**`ADR-0075` is adjacent and does not control.** Its mark annotates the server's stewardship of an
absent seat, so its lifetime hangs on the absence — the state that produced it. An act mark has no
absence to outlive; a rival who is present and playing produces one with every act. What does
transfer is its method: tie the mark's life to what it is *about*, clear it on things a player can
see, and test any pair of standing lines by whether two true sentences read as one false one.

**Quiet is a force against the mark, not just a style for it.** `ADR-0103` froze the phone fit and
listed exhaustively what may give; a new standing element must live inside that fit at both shapes.

## Decision

### 1. One mark, all six acts, at the seat that made it

The table marks **the most recent act of the hand it is painting** — any of the six
(`FOLD`, `CHECK`, `CALL`, `BET`, `RAISE`, `ALL_IN`), at the seat that made it, whichever seat that
is. There is exactly **one** mark at the table, never one per seat; when the other seat acts, the
mark moves. A player's own act is marked the same way — the Lichess mark does not care whose move
it was, and the confirmation is real: a mis-pressed button is learned from the table, not from the
rival's response.

Blind posts, deals, presence changes and rejections are not acts. None of them creates, moves or
clears the mark.

All six rather than a subset, against the plate's standing *Folded* and *All in*, for two reasons.
An absence must mean one thing: under this rule, no mark means *nobody has acted yet in the hand on
screen*; under a subset it also means *an act we chose not to mark*, and the table can only be read
by someone who has memorised the subset. And replacement must be total: an unmarked fold would
leave the rival's earlier *Bet 120* standing beside the plate's *Folded* — two true statements
reading as one false hand — unless a third rule made unmarked acts clear the mark anyway. The
status says what a seat *is*; the mark says what just *happened*; they overlap for two acts and
answer different questions.

### 2. The mark says what the actor's own button said

The verb is `actionVerb`'s, unchanged: *Fold, Check, Call, Bet, Raise to, All in*. A figure rides
with the mark **exactly where the bar's buttons carry one** — `Call`, `Bet`, `Raise to` and
`All in` — and it is the act event's own `to` total, formatted as the table already formats chip
figures. *Fold* and *Check* are bare. The human asked for size on bet and raise; call and all-in
carry theirs for the same reason the buttons do — one vocabulary, and a total is the convention
everywhere a figure appears on this screen (`callTo`, `minRaiseTo`, `Raise to`).

Nothing is netted, priced or worked out: the mark shows the server's verb and the server's number,
which is what keeps `no-derivation.test.tsx`'s invariant true of it by construction. If the engine
named the act `PlayerCalled`, the mark says *Call*; if `PlayerAllIn`, *All in* — the mark translates
the event's own token and invents no case.

### 3. Within a hand it is only ever replaced; the next painted deal removes it

- **The next act replaces it.** That is the only thing that moves it mid-hand.
- **Street boundaries leave it standing.** `BettingRoundEnded`, `StreetDealt`, every `Snapshot`,
  presence frames and rejections touch it not at all. The last act of one street stands until the
  first act of the next — which is also the only rule the delivery permits to be readable
  (*Context*).
- **The next hand's deal, as painted, removes it.** When the table paints the next hand — which,
  by `ADR-0102` §1's queue, happens only after the ending has stood — the mark goes with everything
  else the old hand owned. A fold's mark therefore stands through `ADR-0095` §4's award window,
  telling the *why* beside the award line's *who*.
- **The duel's end retires it with the table.** At `DuelFinished` the result screen replaces the
  table and the mark is cleared, so none survives into a rematch — `ADR-0075`'s boundary guard,
  applied to this field.

### 4. No timer, no fade, no dismiss control

The player this mark exists for is the one who was not looking when it appeared; a mark that
expires serves everyone but its audience. `ADR-0046` §2 settled the neighbouring line as clearing
*never on a timer, never on a fade*, and two lifetimes in one corner of one screen with no stated
reason read as arbitrary (`ADR-0075`, alternative 5). `ADR-0102` §4's pacing clock changes none of
this: that clock schedules *when server-stated facts appear*, never when they are taken away.

### 5. What the card owes, and what stays taste

`EPIC-13` *Design first* requires item 3's card to draw one state per act the rival can make. Under
§1 that is **six states** — *Fold* and *Check* bare, *Call*, *Bet*, *Raise to* and *All in* each
drawn with a figure. The mark is the same mark at either seat, so the second seat multiplies
nothing. Whether it is an icon, a word or both, where it sits, and whether a mark left standing
from an earlier street is visually distinguished are the card's to offer and the human's to accept
(`ADR-0024` §3) — this ADR fixes the content and the lifetime, not the drawing. The card must place
the mark inside `ADR-0103` §1's phone fit; if it cannot without something giving, that re-opens
`ADR-0103`'s list rather than being quietly spent.

### 6. Built from the act events alone, beside the server-action line

The mark is derived from the six act events and from nothing else. `ActedForAbsent` keeps feeding
`ADR-0046` §4's line exactly as `ADR-0075` bounded it; when the server folds for an absent rival,
the game event is a real `PlayerFolded`, so the mark reads *Fold* while the neighbouring line says
*The server folded for your rival.* — both past tense, the act and its submitter, a consistent pair
rather than the tense clash `DEC-070` was raised on.

## Consequences

**What it buys.** At every decision where a player faces a bet, the standing mark is the rival's
act with its size — the thing the human asked for, by construction: a bet you face is the last act
made. A fold's mark stands beside the award line for the whole painted ending, so *who took the
pot* gains its *why*. No wire change, no engine change, no new string, and the mark states only
facts the server sent — `ADR-0002` is untouched.

**What it costs.**

- **Most recent only.** A player away for two acts sees one — the same choice `ADR-0046` §4 made
  for the server's mark (*"showing the most recent one is enough"*, no action log). The stored
  event log and the replay milestone are where history lives; the table is not it.
- **The opening corner.** The seat that opens a street can be the seat that closed the one before.
  Heads-up that seat is always the non-button — it opens every postflop street, since *"the button
  acts first before the flop and last on every street after it"* (`duel-rules.md`) — so whenever
  the non-button's call, or its big-blind check behind a limp, closed the previous street, it opens
  the next under its **own** closing mark, and the rival's latest act stands unmarked, one act
  back. Bounded: in every such case the hidden act's figure is still on screen — a closing call
  carries the same `to` as the raise it answered, and a limp's chips are in the pot — so only its
  verb is hidden, and never a bet the player currently faces.
- **Cross-street staleness.** *Call 240* can stand into a street whose committed row reads 0. Past
  tense and true, and the bar — not the mark — states what a player faces; accepted because the
  alternative blanks the mark at every street boundary or erases the closing act at birth
  (*Context*). Whether a prior-street mark looks different is left to the card.
- **A refresh loses the mark until the next act.** `PlayerView` carries no last-act field, so a
  resuming client rebuilds it from nothing. Deliberately not repaired: the mark is legibility, not
  a fact a player needs to act correctly — `LegalActions` and the bar carry what they face — and a
  `PROTOCOL_VERSION` bump for one line is not worth the step today. If a real player trips on it,
  it is one field on `PlayerView` and an architect's ticket then; registered nowhere, per
  `STORY-1211`.
- **One more standing element on a screen `ADR-0103` just fought onto a phone.** The card pays
  this, and §5 names the escape honestly: re-open the give list, never scroll.

**What it forecloses.** The shape. A client built on *one mark, the last act* is not a per-seat
action ledger, and growing one later is a retraction of this ADR, not an extension. If an action
log ever ships, the mark attaches to its newest line and takes that line's lifetime — superseding
this ADR the way `ADR-0075` forecast for its own. Everything else is two reducer keys and a line of
markup to reverse.

## Alternatives considered

**1. Mark only the acts a player could miss** — check foremost, the human's own example; fold and
all-in already stand on the plate; bet and raise move the committed row. Strongest case: least
furniture on a phone-fit screen, and no duplication with `seat-status.ts`. Rejected because a
subset makes absence unreadable — no mark would mean *no act yet* or *an act off the list*, and the
reader must know the list — and because replacement breaks: an unmarked fold leaves a stale *Bet
120* standing beside *Folded*, the false pair, unless a third rule clears without marking. One
uniform rule costs four more card states and buys a table that can be read cold.

**2. A standing mark per seat, each showing that seat's last act.** Strongest case: the rival's
latest act is *always* visible — the human's sentence read literally — and the opening-corner cost
above vanishes. This is also what the poker rooms the human plays actually do. Rejected on the
vision's own positioning sentence: the reference is Lichess's single last-move mark, not a poker
room's per-seat plaques; two standing lines of different ages ask the reader to order them, and the
pair drifts toward the action log `ADR-0046` §4 refused. Reversal is asymmetric in one-mark's
favour — adding a second placement to a later card is additive, removing one is a retraction — and
with no players yet, the reversible shape wins.

**3. It clears at the street's end.** Strongest case: a mark never outlives the betting it belonged
to, so cross-street staleness never exists. Rejected on the delivery, not on taste: the
street-closing act and its `BettingRoundEnded` arrive in one `Events` frame applied in one tick
(`ADR-0102` §1 paces only a hand's ending), so the one act this rule uniquely governs — *did he
call?* — is erased the instant it is made. The exact trap `ADR-0075` rejected `Snapshot`-clearing
on, one layer up.

**4. It never clears within the duel.** Strongest case: the cheapest rule there is, and the
sentence is past tense, so it is never false — `ADR-0046` §2's *"a line that outlives the moment
costs a player nothing"* is adjacent precedent. Rejected because crossing a deal manufactures the
false pair: hand 2's fresh table with hand 1's *Raise to 480* standing reads as a raise in the hand
on screen, against a pot and a committed row that deny it. And unbounded, it survives into a
rematch (`ADR-0075` recorded the store fact). The next painted deal is the earliest boundary at
which the mark can contradict the table, so it is where the mark stops.

**5. A timer or a fade.** Strongest case: it is what every poker client does with act plaques, it
bounds staleness by attention rather than by frames, and `ADR-0102` §4 has since licensed a
client-side display schedule, so *the client holds no clock* is no longer absolute here. Rejected
because the audience test fails: the mark exists for the player who was not looking, and an
expiring mark serves exactly everyone else. `ADR-0102`'s clock delays the appearance of facts and
never their removal, so the licence does not stretch; and `ADR-0046` §2 fixed the neighbouring
line at *never on a timer, never on a fade*, which two different physics in one corner would make
look arbitrary.

**6. The rival's acts alone** — the ask as literally written. Strongest case: a player knows what
they pressed, so an own-act mark is furniture, and it is one fewer state at one's own seat.
Rejected because the rule becomes *the last act, unless it was yours* — a special case no vision
sentence produces — the drawing is identical either way, and the own-act mark carries the
mis-click confirmation and matches the reference point exactly: Lichess highlights your own last
move too.

**7. The figure as the increment** — *Raise by 140* instead of *Raise to 240*. Strongest case:
"size info" plausibly means what the act added, and some players think in increments. Rejected
because every merged figure on this screen is a total — `callTo`, `minRaiseTo`, the *Raise to*
button this mark echoes — and an increment is a number the server never sent, so the client would
compute it, which `no-derivation` exists to refuse. Two conventions for one figure on one screen
is the false-pair failure in miniature.
