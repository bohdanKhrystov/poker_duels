# ADR-0120 — A showdown shows the hands the rules showed, and the beat that shows them stands

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-133` — **the product owner's** — **how much of a showdown does the table
  show?** Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 2a, on the human's
  *"if we went to showdown villan card shoud be show"* and their `edits3.png` annotation — *"villan
  card shown if required"* — after playing a duel to showdown on a laptop and an iPhone against a
  local-network stack. The registered options ran from *draw what is already sent*, which moves no
  wire at all, to a **post-hand disclosure** of the hand that mucked, which
  [`ADR-0008`](ADR-0008-loser-mucks-at-showdown.md) names as the only sanctioned widening.
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  Their message is the report, not the ruling. Two halves, two grounds.
  **What is shown, and for how long,** is licensed by [`docs/vision.md`](../vision.md)'s *On
  variance* — ***"Luck decides a hand.** Skill decides whether you come back tomorrow."* — read
  exactly as [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
  already read it when it licensed the award banner: the vision names the **hand** as the unit at
  which luck lands and asks the player to make peace with it, and *"a table that resolves a hand in
  silence asks them to absorb a variance it never told them about."* A showdown that stands for
  600 ms resolves it in silence as surely as one that prints nothing, and *What it is* seconds it in
  two words — *"Replay and **honest feedback**"*.
  **What is not shown** is licensed by no new sentence, and that is the point. `ADR-0008` is merged,
  it made this trade with its eyes open, and nothing in *What it is* or *What it is not* asks for it
  to be reopened. The human's own annotation says *shown **if required***, which is the rule
  `ADR-0008` already implements rather than a disclosure beyond it. An answer that widened it would
  add a commitment the vision does not make.
- **Applies, and amends nothing:** `ADR-0008` **whole and unamended** — the last aggressor shows
  first, the losing hand is never revealed, a mucked hand appears in no event, `CardSecrecyTest`
  guards both with one rule; [`ADR-0002`](ADR-0002-server-authoritative.md) (no card face is ever a
  client's assertion); `ADR-0095` §3 (**no hand is named**, at a showdown least of all) and §4 (the
  banner lives as long as the hand it describes, *never on a timer, never on a fade*);
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §§1–3 and §5, whole —
  the step queue, what a step paints, what it withholds, and the reconnecting client that jumps to
  the end;
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md) (§3
  below adds a duration, never an animation, so there is nothing here for reduced motion to still);
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 — no card draws a
  showdown today, so `STORY-1411`'s first ticket is that card.
- **Spends** `ADR-0102` §4's own licence — *"600 is a feel number and it is the cheapest thing here
  to be wrong about … If the product owner wants it at 400 or 900, that is a one-line answer that
  amends nothing in §§1–3"* — and spends it on **one beat** rather than on the step (§3). The
  mechanism §4 pins — *a step is 600 ms, named once, at the boot seam* — is the half this ADR does
  not settle and registers instead.
- **Registers `DEC-146`, the architect's:** by what mechanism does the client's step queue give one
  beat a length different from a step, given that `REVEAL_STEP_MS` is named once and reaches the
  store as a single parameter, and that `web-client/src/e2e/drive-duel.tsx` boots it at `0` so that
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §3's four
  recorded-frame suites are neither edited nor re-recorded?
- **Constrains:** `STORY-1411` and its design card. **No wire change, no `PROTOCOL_VERSION` move, no
  server file, no engine file, no new player-facing string, no new control, no new `Screen` member.**
- **Names, and does not register** (`ADR-0105` §6's route — *a `DEC` nobody is working is noise in
  the open table*): whether a hand that is **all in and called** turns both hands face up, as the
  rules of live poker require and as this product does not (§5).

## Context

### What the human saw, and what the source says about it

The report is one clause — *"if we went to showdown villan card shoud be show"* — from a session
whose result screen read `Victory`, `7 hands · You 20,000 · Your rival 0`. Two different things can
produce it, and the source says both are real.

**The rival's hand is already drawn, and it stands for 600 ms.** `EPIC-14`'s *What is already true*
records that **no client file outside `protocol.gen.ts` mentions `HandRevealed`**, and infers from it
that *"the hand that is shown is sent, arrives, and is drawn by nothing."* The first half is exact;
**the inference is wrong**, and a story written on it would build a second path to something already
on the screen. The reveal reaches the table through the **snapshot**, not through the event:

- `Addressed.kt:52,62` — `broadcast` computes `revealedSeats(handEvents)` and passes it into
  `PlayerView.of(state, seat, revealed)` for **every** frame it sends;
- `PlayerView.kt` — `seatView(it, showCards = it.index == seat || it.index in revealed)`, so a
  revealed seat's `holeCards` are populated in the view itself;
- `DuelTable.tsx:76-78` — the rival's seat is drawn with `cards={rival.holeCards}`;
- `Hand.tsx` — *"a place the view carries a card for is drawn face up"*.

And it is pinned by a merged test: `web-client/src/e2e/duel-secrecy.test.tsx`, *"do reach it once the
reveal has arrived"*, over recorded frames, with an explicit guard against passing on a client that
renders no cards at all. So the question is not whether the table draws a shown hand. It is **how
long that drawing stands**, and `ADR-0102` answers that today: a hand called down to the river
carries no `StreetDealt`, so its ending is **one** step, and `advanceReveal` drains the queued next
hand the moment that step expires. `REVEAL_STEP_MS = 600`. Two cards a player has never seen, a
five-card board and an award line, in six tenths of a second — on a rival's hand that `ADR-0103` §3.2
draws at 24–40 px because it is the element that *"narrows furthest of anything on the table"*.

**And in the hands the human won, the rival's cards were never sent at all.** `Showdown.kt:61-76`:
`revealOrder` returns **the winners** — one seat, or both on a split. `StreetProgression.kt:130-131`
emits one `HandRevealed` per seat in that list and no others. That is `ADR-0008` working exactly as
written: *the losing hand is never revealed*. A player who **wins** a showdown therefore learns
nothing about what they beat, and a player who **loses** one always sees the hand that beat them. The
human won every chip in that duel, so the hands they most wanted to see are the ones the rules
withhold.

### What is actually in tension

- **Secrecy is the game.** `ADR-0008`'s Context is unusually direct about it: *"An opponent's range is
  the game. A player who learns what the loser folded or lost with has learned something the game is
  built to withhold, and no amount of 'it is only a duel coin' changes that."* Nothing has changed
  about the product since — there is still no money in it, and there are still no players but two.
- **A showdown is the one moment the table has something to say.** Every other beat of a hand is chips
  and a street name. The showdown is where the variance the vision refuses to hide actually lands, and
  it is the beat this client gives the *least* time to.
- **The route out is named, and it is not free.** `ADR-0008`'s Consequences: if the trade is wrong,
  *"the fix is a **post-hand** disclosure written by the server after the hand is settled — never a
  loosening of what the engine publishes mid-hand."* That is available; it costs a wire field, a
  `PROTOCOL_VERSION` step under `ADR-0047`'s one-branch lock, and a second place outside the engine's
  projection layer where a hole card lives.
- **The evidence is one session, by the author, on a build nobody else has played.** `ADR-0095` §6 set
  the standard for reversing a secrecy-adjacent refusal in this repository: *the first duels played by
  people who are not the author*. That standard is not met today, in either direction.

### The deadline

`STORY-1411` is about to be split, and `ADR-0091` §2 makes its **first** ticket the design card for
the showdown — a card that must draw *"the states `duel-table-states.html` does not have"*. A card
cannot draw frames whose contents are undecided, and a card drawn against the epic's inference would
draw a surface the product does not need. The hold's number is cheapest now too: it is one beat behind
a mechanism that is merged and has exactly one consumer.

## Decision

### 1. A showdown shows exactly the hands the rules showed — and nothing is disclosed after it

`ADR-0008` stands, unamended and unqualified. There is **no post-hand disclosure**, at a showdown or
anywhere else, and the wire does not move.

Stated as the table's behaviour, so that no ticket has to re-derive it from the engine:

| The hand ended | The table shows |
| --- | --- |
| Showdown, one winner | the **winner's** two cards, face up, in the winner's seat |
| Showdown, split pot | **both** hands, face up, in reveal order |
| A fold, at any street | **no** hole cards; `ADR-0095`'s award banner and nothing else |

Said the way a player experiences it, because this is the half that will be argued about: **you always
see the hand that beat you, and you never see what you beat.** That is the rule of the game this
product is a client for, it is what the human's own annotation asks for — *shown **if required*** —
and it is what already ships.

**No line is added to say a hand was mucked.** The cards that stay face down say it. `ADR-0095` §§1–3
fix what the end-of-hand banner states and forbid naming a hand anywhere on this table; this ADR
reopens neither, and a *Your rival mucks* string is not licensed by it.

### 2. It is drawn in the seat that showed it, from the snapshot — and it is drawn there already

The reveal is the rival's own two card places turning face up on the table. **No showdown panel, no
overlay, no second surface, no new string.**

The card faces come from the **snapshot** — `SeatView.holeCards`, as `DuelTable.tsx` already reads
them. `HandRevealed` may be read for *that* a hand was shown; it is never read for *what* was shown.
The reason is player-visible rather than stylistic: `ADR-0102` §5 sends a reconnecting client **no
`Events` frame at all**, so a drawing built on the event is blank for exactly the player who reloaded
during the hand — and `ADR-0118` is the merged rule that a browser shows what the server told it, late
if it must, but never less.

What `STORY-1411` owes for this section is therefore a **test that pins it** and a **design card**,
not a new drawing path.

### 3. The last beat of a showdown stands for **2 s**

A hand that ends with a hand face up the viewer has not been shown before holds its **last** step for
**2,000 ms** before `ADR-0102` §1's queued frames are applied. Every other ending — a fold, a showdown
this viewer won, any beat where nothing new turned over — keeps `ADR-0102` §4's **600 ms** step,
unchanged.

Stated as the client can evaluate it, so that no ticket invents a different test: **the
hand-completing view carries hole cards for the *rival's* seat.** Nothing is computed and no event is
consulted; the two seats can answer it differently for the same hand, and that is deliberate.

- The hold ends when the next hand's frames are applied, which is a **fact arriving**, not a fade and
  not an expiry. `ADR-0095` §4 — *never on a timer, never on a fade* — is untouched: nothing is
  removed from the screen by a clock; the screen is simply not overwritten sooner.
- The non-final steps of a runout keep 600 ms each. A preflop all-in still runs the board out in 2.4 s
  and then holds 2 s.
- Nothing in `ADR-0102` §§1–3 moves: the queue is FIFO, nothing is dropped or reordered, no control is
  offered against a lagged screen, and nothing is computed.
- **2,000 is a feel number**, and by `ADR-0102` §4's own reasoning it is the cheapest thing in this ADR
  to be wrong about. Moving it changes no interface and no test that is not about the number itself.
  The **mechanism** that gives one beat its own length is `DEC-146`, and it is the architect's.

### 4. What this decides nothing about

Named because each is one step away and a ticket will be tempted:

- **Whether the winning five is marked** in hand and on board — `DEC-134`, open, answered against
  `ADR-0095` §3 by its own ADR. This one decides what is **shown**, never what is **marked**.
- **Whether the pot travels to the winner** — `EPIC-14` item 2c, governed by `ADR-0115` and
  `ADR-0102`.
- **How wide the rival's hand is drawn.** At `ADR-0103` §3.2's 24–40 px on a phone, two seconds of a
  card nobody can read is still not showing it. That width is `DEC-136`'s, and §3 is worth what that
  decision leaves it worth.
- **The runout's spoiler.** The winner's cards are face up from the runout's first step, and
  `ADR-0102`'s Consequences already accept the same leak through the settled stacks. This ADR neither
  fixes nor worsens it.
- **A voluntary show** — `ADR-0008`'s own rejected alternative, *"Showing is a `PlayerAction`"*,
  deferred there on sequencing. Nobody has asked for it and nothing here opens it.

### 5. What would reverse §1, stated so the reversal is a decision rather than a drift

Following `ADR-0095` §6's idiom. **The trigger is the first duels played by people who are not the
author, reporting that winning a showdown tells them nothing** — or that a hand played all-in ended
with one hand face down. The most likely first widening is **narrower than the registered option**,
and it is named here rather than answered: in every live poker room and every online client, a player
who is **all in and called** turns their hand face up before the board runs out, because no decision
remains to protect. `docs/duel-rules.md` says only *"The loser may muck"*, and this engine mucks
always; `ADR-0008` did not consider the all-in case separately. That question is an addition to the
written rules, it is not what was reported, and it is not answered here.

When the trigger is met, the fix is `ADR-0008`'s own route — **a post-hand disclosure written by the
server after the hand is settled, never a loosening of what the engine publishes mid-hand** — and it
supersedes this ADR rather than amending it.

## Consequences

**What it buys.** The showdown the human asked for becomes legible, at the cost of one number and no
wire move: the hand that beat you is on the table long enough to read against the board, in the seat
that owns it, in a product where a duel is seven hands and every one of them currently ends in six
tenths of a second. `ADR-0008`'s single secrecy rule keeps its single test, and the projection layer
keeps its single filtering point. `STORY-1411` shrinks to a card, a hold and the tests that pin both —
it touches no server or engine file, which is what `EPIC-14`'s *Out of scope* already demanded of it.

**What it costs.**

- **The winner still learns nothing.** The player who reported this won 7 hands to nil, so on the most
  common shape of the very session that raised `DEC-133`, this ADR changes only the pacing. That is
  stated plainly because it will be noticed, and §5 is the route it takes if it is wrong.
- **A held player pays for the hold out of their own clock.** A queued `YourTurn` anchors its countdown
  to the instant the frame **arrived** (`ADR-0113` §6, `duel-state.ts`'s `arrivedAt`), so a player held
  two seconds to read a showdown starts their next decision with two of their 30 s allowance already
  spent — 6.7%, before their 3 m timebank is touched. At 600 ms this cost exists and is invisible; at
  2 s it is real, and it is accepted.
- **The two screens no longer step together.** The player who lost the hand holds 2 s and the player
  who won holds 600 ms, so one tab paints the next hand about 1.4 s before the other. Both are painting
  frames the server already sent, so no game fact diverges — but two tables side by side will be out of
  phase.
- **A second duration number exists**, where `ADR-0102` §4 deliberately pinned one. That is the price of
  not slowing the runout, and it is why `DEC-146` is registered rather than assumed.
- **`EPIC-14`'s premise for item 2a is corrected downward.** Its measured claim that the shown hand is
  *"drawn by nothing"* is wrong about the screen, and the epic is amended in this PR.

**What it forecloses.** `ADR-0008`'s foreclosure is re-affirmed with a second signature: `EPIC-08`'s
analysis board still cannot show what the loser held, and no equity graph over a real duel can be
complete. A replay viewer (`v0.4`) inherits the same log. Neither becomes harder to change later than
it is today — but neither is licensed by this ADR either.

## Alternatives considered

**Disclose the mucked hand after the hand settles — the registered ceiling.** The strongest case, and
it is strong: it is the plainest reading of the human's own words; `ADR-0008` *names* it as the
sanctioned route, so no non-negotiable is in the way and no engine file is opened; heads-up is where
the usual argument for protecting a range is weakest, since there is no third player to protect the
information from and no money at stake; and it would make the analysis board and the replay viewer
whole in one move. Rejected on three grounds. It changes what the game **is** — hand reading is the
skill the vision's *"Skill decides whether you come back tomorrow"* is about, and a product where every
showdown is public has removed it. It is the **hardest thing here to walk back**, exactly as `ADR-0008`
warned about showing everything: information a player has been given cannot be taken away without the
removal being the story. And the evidence is one session by the author, against `ADR-0095` §6's
standard for reversing a refusal of this kind — while that same human's annotation says *shown **if
required***, which is the rule, not the ceiling.

**Turn both hands face up when a player is all in and called.** The true poker rule, in every room and
every client: once no decision remains, the cards go on their backs. It is the beat where the vision's
*"Variance is not a defect to be engineered away"* has the most to show, and withholding protects
nothing, because neither player can act again. Rejected **here** rather than on merit: it is an
addition to `docs/duel-rules.md`'s written rules, it needs either the engine reveal `ADR-0008` forbids
or the post-hand disclosure route with its wire move, `EPIC-14`'s *Out of scope* refuses the engine
outright, and it is not what was reported. It is §5's named first widening, and it deserves its own
decision rather than a corner of this one.

**Raise `REVEAL_STEP_MS` to 2 s uniformly.** The cheapest possible answer, and explicitly licensed:
`ADR-0102` §4 hands the product owner exactly this one-line move, with no new number, no mechanism
question and therefore no `DEC-146`. Rejected because it prices two different needs at one number: a
preflop all-in becomes four beats of 2 s — **eight seconds** of runout, against *"Dark, quiet, fast,
minimal"* — and every hand that ends in a preflop fold makes both players wait 2 s for a banner with
nothing new on it. The showdown needs the time; the street steps do not.

**A showdown panel or overlay that states both hands.** Unmissable, independent of how wide the rival's
hand is drawn on a phone, and it would need no pacing decision at all. Rejected because the table
already has the two places this fact belongs in; a panel that restates cards drawn elsewhere is a
second source of truth for a game fact; it sits one careless line away from `ADR-0095` §3's *"no hand
is ever named"*; and this product has no modal of any kind — `DEC-138` is open precisely because
introducing the first one is its own decision.

**Hold the showdown until the player dismisses it.** Guarantees the reveal is seen, by anyone, at any
reading speed. Rejected because it puts a control between a player and the next hand of a match the
vision describes as one continuous thing — *"We play a full heads-up match. Someone wins. We hit
Rematch."* — and because it invents a control nobody asked for to solve what a number solves.
