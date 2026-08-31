# ADR-0101 — `pot` means a pot-sized raise, and the fractions share its base

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-101` — **the product owner's** — what amount does each named sizing preset set:
  what does a chip labelled `pot` (and `⅓`, `½`) promise a player in a heads-up duel? Registered
  open earlier the same day by
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6, which
  settled the technical half and refused this one.
- **Where the answer came from.** Not from the human, who was not asked and stated nothing; it is
  derived, and the sentence that licenses it is the first line of `docs/vision.md`'s *What it is*:
  **"Heads-up Texas Hold'em. Two players. Never three."** The product *is* Texas Hold'em, so the
  words printed on its controls are the game's words, and in Texas Hold'em *pot* names a bet or
  raise the size of the pot. `docs/duel-rules.md` states the rule for the other direction in its
  opening lines — *"Where poker has house variations, the choice made here is the choice the engine
  makes. Ambiguity in this document is a bug"* — a house variation is settled in that document, by
  a ticket, never invented by a label on a chip. `ADR-0100` itself already recorded what the
  audience expects, in a merged sentence: *"the textbook pot-sized raise a poker player expects,
  which is 3,650 there."*
- **Registers:** `DEC-102` — **the product owner's** — what does one press of the sizing row's
  stepper move the dialled total by? Open below. `ADR-0100`'s alternatives assumed the step
  travelled with `DEC-101`; §6 says why it does not, and why it gates none of this.
- **Applies, and does not touch:**
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6 — *a
  named preset computes the quantity it is named for*, and the pot reaches `ActionBar` through
  `Lobby.tsx`. This ADR says **which** quantity and changes none of its text.
  [`ADR-0002`](ADR-0002-server-authoritative.md) — a dialled total is a *proposal* and the player's;
  that line was drawn by `ADR-0100` §6 and this ADR sits on the proposal side of it, asserting no
  game fact and computing no legality.
  [`ADR-0033`](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) — the card is still
  where anatomy is born. §5 puts one card in arrears on **one number in two places**; its anatomy —
  five chips, a stepper, the labels themselves — is untouched and is what this ADR builds on.
- **Touches no module.** No code ships with this ADR. It fixes what `TASK-120908` must implement;
  the ticket is the planner's to rewrite (`ADR-0100` §7), and §7 below says what changes in it.
- **Where the numbers came from.** Every amount below was computed on 2026-08-31 against the merged
  engine, not assumed: `BettingRules.kt` (`allInTo = committed + stack`, `callTo = committed +
  toCall`, `minRaiseTo = minOf(state.minRaiseTo, allInTo)`), `GameState.toCall` (`(betToMatch −
  committedThisStreet).coerceIn(0, stack)`), `BettingProjection.raiseTo` (`minRaiseTo = to + (to −
  betToMatch)`) and `StateProjection` (once the blinds are posted, `minRaiseTo = betToMatch +
  bigBlind`); and against `design/screens/duel-table.html` as merged, read frame by frame.

## Context

`TASK-120908` replaces the action bar's range slider with the five chips
`design/screens/duel-table.html` draws — `min`, `⅓`, `½`, `pot`, `all-in` — and a stepper.
`ADR-0100` §6 settled that **a named preset computes the quantity it is named for** and threaded the
pot to the bar so that it can. It stopped there, because what the three middle chips compute is not
a technical fact.

**Two of the five chips name numbers the server sends.** `min` is `minRaiseTo` (or `minBetTo`);
`all-in` is `allInTo`. Nothing is in tension there. The other three name a computation nobody has
ever written down in this repository, and three different, defensible ones fit what is written.

**The card gives one worked example, and one example cannot separate them.** The hero frame draws
`Pot 2,450`, the rival `committed 400`, `Call 400`, the `pot` chip selected, and `Raise to 3,250` in
two places. The hero has no bet-line, so the hero has committed nothing this street — and that is
exactly the frame in which the two formulas the card admits collapse onto each other:

| Candidate | On the card | On a frame where the hero has already committed 200 and faces a raise to 600, with 600 swept |
| --- | --- | --- |
| `view.pot + 2 × callTo` | 3,250 | 1,800 |
| `(view.pot + both committedThisStreet) + callTo` | 3,250 | 2,000 |
| The pot-sized raise a poker player expects | 3,650 | 2,400 |

Three answers, differing by hundreds of chips at a real table, and the card excludes the third.

**The pull towards the card.** It is a merged source, `ADR-0033` makes a card the birthplace of a
component's anatomy, and `TASK-120908`'s own *Out of scope* says in as many words that the card is
not in arrears and *"the client is the side that moves"*. Contradicting a drawn number is a real
cost: every table ticket since the card merged has read it as true.

**The pull the other way.** The audience is named in the first line of the vision's *What it is*,
and it brings a meaning for *pot* that it did not learn here. A chip that quietly means something
11% smaller is not a display difference — it is chips, in a ranked duel, discovered by losing them.
`ADR-0100` §6 has already ruled that a label naming a game quantity is a **statement**, and that a
false one is the thing `ADR-0002` exists to prevent; a `pot` chip that does not size the pot is that
statement, whatever arithmetic produced it.

**And a third force nobody has stated yet: the two ends.** A computed fraction can fall **below**
`minRaiseTo` — the button's `⅓` preflop does, every single hand — or **above** `allInTo` on a short
stack. `TASK-120908` has a rule for one end (*"a preset the stack cannot afford is not offered"*)
and none for the other, and no source anywhere says whether an out-of-range chip is clamped, hidden
or greyed. That is not a detail: a clamped `⅓` sets the minimum raise while its label says
otherwise, which is the same false statement arriving by a different route.

**Chips are integers.** `docs/duel-rules.md`: *"Chip counts are integers. There are no fractional
chips anywhere in the engine."* A third of a pot usually is not one, so a rounding direction has to
be chosen and stated, or two implementations will differ by a chip and a test will pin whichever
shipped first.

## Decision

### 1. The base is the pot as it will be after the call

Every fraction is a fraction of one quantity, and that quantity is the pot the player would be
playing into if they merely called:

```
P      = view.pot + seats[0].committedThisStreet + seats[1].committedThisStreet   // ADR-0100 §6
toCall = legalActions.callTo − seats[legalActions.seat].committedThisStreet
base   = P + toCall
```

`P` is exactly the pot `ADR-0100` §6 already hands the bar, unchanged. `toCall` is what the call
still **costs**, which is not `callTo`: `callTo` is what the hero's street commitment must *become*.
The difference is zero on the card's frame and is the whole of the disagreement everywhere else.

When no bet is outstanding — the server allows `BET`, not `RAISE` — `toCall` is `0` and the base is
simply the pot. So one rule produces both behaviours a player already knows: *half the pot* when
betting, *half-pot raise* when raising, with no second case.

### 2. What each chip sets

Every chip sets a **street total** — the same number the `Raise to` button prints and the same
number the `Act` frame carries.

| Chip | Sets `to` | What it promises |
| --- | --- | --- |
| `min` | `minRaiseTo`, or `minBetTo` when the server allowed `BET` | the least the server will accept |
| `⅓` | `callTo + floor(base / 3)` | put a third of the pot in, on top of the call |
| `½` | `callTo + floor(base / 2)` | put half the pot in, on top of the call |
| `pot` | `callTo + base` | put a whole pot in, on top of the call |
| `all-in` | `allInTo` | every chip you have |

**Rounding is `floor`.** Chips are integers, and flooring is the direction that never commits more
of a player's stack than the label says; the error is at most one chip, and it is on the side the
player would choose if asked.

The three fractions are one rule with three constants over one base. Adding `¾` or `2×` later costs
a constant and no new thinking; that is a deliberate property of this shape, not an accident.

### 3. A chip that cannot compute its own quantity legally is not offered

`⅓`, `½` and `pot` are rendered **only** when the amount §2 gives them satisfies
`floor ≤ amount ≤ allInTo`, where `floor` is `minRaiseTo` or `minBetTo` as the server allowed.
Otherwise the chip is **absent**. It is never clamped into range, and never rendered dead.

- **`TASK-120908`'s existing rule stands, and widens.** *"A preset the stack cannot afford is not
  offered"* was one end of this; this is the same rule at both ends, and the ticket's third test
  keeps its name and gains a sibling at the bottom end (§7).
- **`min` and `all-in` are never absent** while the sizing row is shown. The engine caps both into
  range itself (`minRaiseTo = minOf(state.minRaiseTo, allInTo)`, `minBetTo` likewise), so they are
  legal by construction. This is also what keeps `ADR-0100` §3 true: every amount-carrying step in
  the committed script is `minRaiseTo` or `allInTo`, so the driver's press always finds its chip and
  **no frame is re-recorded by this answer either**.
- **Hiding costs a shortcut and never an amount**, because a chip is hidden exactly when its own
  amount is one the server would refuse. Nothing legal becomes unreachable.
- **Two chips may print the same total, and both stay.** Preflop at 75/150 the button's `½` and
  `min` are both 300. That is a coincidence of arithmetic between two labels that are each telling
  the truth — unlike a clamp, which makes them agree by making one of them lie. The row's contents
  depend on legality, never on a comparison between chips.
- The sizing row appears only when the server allows `BET` or `RAISE`, which is what the shipped bar
  already does. This ADR does not move it.

### 4. Worked, on the card's own frame — and on two the card does not draw

`design/screens/duel-table.html`, the hero's turn: `Pot 2,450`, rival `committed 400`, hero
committed nothing, `Call 400`, hero holding 13,400, flop, blinds 75/150.

```
P      = 2,450 + 400 + 0 = 2,850
toCall = 400 − 0         = 400
base   = 2,850 + 400     = 3,250
```

| Chip | Amount |
| --- | --- |
| `min` | **800** — the rival's 400 was the first bet of the street, so `minRaiseTo = 400 + 400` |
| `⅓` | 400 + 1,083 = **1,483** |
| `½` | 400 + 1,625 = **2,025** |
| `pot` | 400 + 3,250 = **3,650** |
| `all-in` | **13,400** |

All five are legal, so all five are drawn, exactly as the card draws them. One number differs from
the card, and it is the selected one — see §5.

One frame is what got this wrong the first time, so here are two more:

| Frame | `P` | `toCall` | `base` | `min` | `⅓` | `½` | `pot` |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Preflop, the button to act at 75/150 with 10,000 | 225 | 75 | 300 | 300 | 250 → **not offered** | 300 | 450 |
| Flop: 600 swept, the hero bet 200, the rival raised to 600 | 1,400 | 400 | 1,800 | 1,000 | 1,200 | 1,500 | 2,400 |

The second frame is where the two card-fitting formulas part company: `view.pot + 2 × callTo` gives
1,800 for `pot` — which is the *base*, not a raise — and `(view.pot + both committed) + callTo`
gives 2,000, a number with no name in poker.

The first frame states a fact that will look like a defect the first time somebody meets it, so it
is recorded here deliberately: **the `⅓` chip is never offered to the button preflop.** Once the
blinds are posted the base is exactly two big blinds whatever the small blind is (`P = sb + bb`,
`toCall = bb − sb`), a third of it plus the call is 1⅔ big blinds, and the minimum raise is two. The
chip is not missing; a third-pot open does not exist in heads-up hold'em.

### 5. `design/screens/duel-table.html` is in arrears — one number, two places

In the hero frame, with the `pot` chip selected, both

- the stepper's `<span aria-label="raise to amount">3,250</span>`, and
- the actions row's `Raise to <span class="amt">3,250</span>`

read **3,650** under §2. Nothing else on the card moves: the five chips, their labels, their order,
the stepper, the three drawn action buttons and every other number are correct and are what this ADR
builds on.

The diagnosis is worth recording, because it explains how a careful card landed on a number that
excludes the answer: **3,250 is the base, not the total.** It is precisely `P + toCall` — the pot as
it will be after the call. The card printed the pot it was sizing *against* in the place where the
raise that sizes *to* it belongs.

`TASK-120908`'s *Out of scope* says *"The card is not in arrears here — no ADR merged after it
settles the sizing control — so the client is the side that moves."* That was true when it was
written. This is the ADR that merges after it, and it is the design side that moves on this one
number; the ticket for it is the planner's to write, and it is not the client rewrite. Until both
land, a client built to this ADR and a card at 3,250 disagree, and a UAT round reading the card
would file a correct client as a defect.

### 6. What this does **not** settle: the stepper

`ADR-0100`'s alternatives say the stepper's step size *"belongs with `DEC-101`'s control design"*. It
does not. The question registered was what the **named presets** compute; the stepper is not a named
preset, and nothing in §§1–5 determines what one press of `+` moves the total by. There is no
sentence in the vision or in `duel-rules.md` to derive it from, and deriving it from nothing is the
failure this role exists to prevent. It is registered as **`DEC-102`**, the product owner's.

**`DEC-102` gates nothing in §§1–5.** The chips' arithmetic, their offer rule and `ADR-0100` §1's
driver all stand without it, and the merged gate `whole-duel.test.tsx` needs only `min` and
`all-in`.

One constraint for whoever answers it, stated here because it is the reason the question is not
cosmetic and **not** decided here: `docs/duel-rules.md` §*Betting* says **"No-limit."** The shipped
slider reaches every legal total; five chips reach five. Whether the bar must keep a way to reach
any legal total, and therefore whether the stepper can ship after the chips or must ship with them,
is part of `DEC-102`. This ADR neither requires nor forbids shipping the chips first.

### 7. `TASK-120908`'s file set does not change

It stays `ADR-0100` §7's six files, `atomic:` on `whole-duel.test.tsx`. Three things move **inside**
them:

**`Lobby.tsx` hands the bar one more published number than `ADR-0100` §6 named** — the acting seat's
`committedThisStreet`, or equivalently the `toCall` computed from it. §1's base is not computable
from the summed pot and `callTo` alone once the hero has committed on the street, which is the
frame that separates this answer from both card-fitting ones. Every term is already on the wire and
already in `Lobby.tsx`'s hand (`view.seats[i].committedThisStreet`, `turn.legalActions.callTo`,
`turn.legalActions.seat`): **no wire change, no `PROTOCOL_VERSION` move, no new store field**, and
`ADR-0100` §6's pot term is untouched. Whether it arrives as a second prop or as one derived number
is the ticket's shape, not this ADR's.

**The ticket's second test gets its expected numbers from §4, and at least one of them must be a
frame where the hero has already committed.** `each preset sets the amount its own name states` over
a fixture whose acting seat has `committedThisStreet: 0` cannot tell §1's `toCall` from the simpler
`callTo`, nor its summed `P` from `view.pot + the rival's committed`: both shortcuts are right at
zero and wrong on a re-raise, which is precisely the frame the whole disagreement lived in. §4's
re-raise row is the case that separates them.

**The third test keeps its name and gains a sibling.** `a preset the stack cannot afford is not
offered` stands as written (§3's upper end). The lower end — a preset below the server's minimum is
not offered — has a ready-made fixture in §4's preflop row, where `⅓` is absent and the other four
remain.

And one thing that deliberately does **not** move: `bar-no-derivation.test.tsx`'s *first* test,
*shows no number the turn does not carry*, survives untouched. The chips are labelled by name and
not by amount — the card draws `min ⅓ ½ pot all-in` with no figures on them — and the bar still
opens at the server's floor, so the only numbers on a freshly rendered bar are still the turn's own.
A derived total appears only after a player has asked for it, which is `ADR-0100` §6's proposal side
of the line. **A chip that printed its own amount would redden that merged guard**, and nothing here
asks for one.

## Consequences

**The cost, named plainly: a merged card is now wrong, and every reader of it has to check a date.**
`design/screens/duel-table.html` has been the reference for every table ticket since it merged, and
overruling one of its *amounts* is a different kind of correction from reading one of its states
narrowly, which is all `ADR-0100` §4 had to do. The edit is two nodes; the cost is that the card can no longer be trusted number-by-number
without asking which ADR merged after it. Until the design ticket lands, the repository holds two
sources that disagree by 400 chips — and the one a UAT round reads first is the card (§5).

**A second cost: the sizing row changes shape between turns.** A chip a player pressed last hand may
not be there this hand, and the chips to its right shift under the finger. That is the price of
never lying, paid on a clock in a fast game. If it turns out to hurt more than the honesty helps,
the reversal is one predicate in one component — §3 is the cheapest thing here to be wrong about,
which is why it was chosen over the two alternatives that keep the geometry.

**A third: the chips do not cover the most common decision in the game.** Preflop, the button gets
`min` and `½` at the same 300, no `⅓`, and `pot` at 450 — four chips carrying three distinct totals:
300, 450 and the whole stack. The 2.2–2.5 big blind open that is ordinary in heads-up play is
reachable from no chip at all. That
is a real narrowing against the slider that ships today, it is the sharpest argument that the
stepper is not optional, and this ADR does not answer it (§6, `DEC-102`).

**A fourth: `pot` is not what the card told everyone it was.** Anyone who has read the card — the
coder who has already written a sizing row once, and the next one — carries 3,250. The number
changes under a label that did not.

**What it forecloses.** The chips can no longer be defined without the view: `LegalActions` alone
cannot produce a correct `⅓`, so a later refactor that stops passing the view's numbers to the bar
breaks the *labels*, not merely a display, and `ADR-0100` §6's *"reversed by deleting one prop"* is
now false — deleting it deletes three chips. And the fraction family is fixed to one base: a future
`½` that means half of something else (the stack, the effective stack) contradicts §1 and needs an
ADR, not a constant.

**What it buys.** One rule, five labels, and a player who learned poker anywhere else is right on
their first press — which is the whole of the argument, and it is worth saying that this is the
cheap half: the arithmetic is four lines, and the expensive half was deciding whose meaning wins.
The roadmap's *later* milestone lists decision-quality analysis; a duel whose sizing labels mean what
the literature means is one whose sizes can be reported back to a player without a footnote.

## Alternatives considered

**Keep the card's 3,250 and define `pot` as *raise the total to the pot*.** *Its strongest case:*
the card is a merged source and `ADR-0033` makes cards the birthplace of anatomy; `TASK-120908` was
written on the premise that *"the client is the side that moves"*; the reading is literal — the chip
dials the total **to** the pot, and 3,250 is exactly the pot after the call; and nothing anywhere
would be in arrears. It is also the cheapest arithmetic (`view.pot + 2 × callTo`, no seat lookup,
correct whenever the hero can cover the bet). **Why it lost:** it means the opposite of what the
audience named in the vision's first line of *What it is* learned the phrase to mean, and the
difference is invisible at the moment of pressing — on the card's own frame a player who wanted a
pot-sized raise makes one 400 chips smaller and cannot tell. It also has no honest extension: *a
third of what* has no answer that keeps the same shape, so `⅓` and `½` would have to be defined by
some second rule the label does not hint at. Correcting one number on a card is cheap; teaching a
poker product's audience a private meaning for *pot* is not.

**`(view.pot + both committedThisStreet) + callTo`.** *Its strongest case:* it fits the card too,
and unlike the first it uses the whole pot including the current street, so it looks like the more
careful of the two; it needs only the single number `ADR-0100` §6 already hands the bar, plus
`callTo`, and so requires no seat lookup and no change to that ADR's hand-off. **Why it lost:** it
adds a street *total* to a pot and so double-counts the hero's own chips — `callTo` is what the
hero's commitment must become, not what the call costs. In §4's re-raise frame it prints 2,000: the
post-call pot plus the hero's earlier 200. It is right on the card only because the hero has
committed nothing there, which is the one kind of frame where it and `view.pot + 2 × callTo` are the
same number.

**Clamp an out-of-range preset into `[floor, allInTo]` instead of hiding it.** *Its strongest case:*
the row's geometry never changes, so no chip moves under a finger mid-decision and a press always
produces something; it is what every commercial client does, so it is what the audience expects
there too; and it is one line. **Why it lost:** a clamped chip states a quantity it did not compute,
which is the exact thing `ADR-0100` §6 forbids, and it fails silently — preflop the button would
find `min`, `½` **and** `⅓` all setting 300, with two of the three labelled for amounts they are
not. At the top it is no better: a `pot` clamped to `allInTo` duplicates the `all-in` chip two
places to its right, so the clamp's own best case (a press always does something) is served by a
chip that is already there.

**Grey the out-of-range preset out in place.** *Its strongest case:* it answers the clamp's best
argument — stable geometry — without lying, and it teaches: a player learns *why* a third is
unavailable preflop rather than wondering where the chip went. **Why it lost:** the teaching is the
problem. A dead control asks a question in the middle of a decision on a clock, and the answer is a
fact about this turn's arithmetic that changes nothing the player can do. The vision's positioning
is *"Dark, quiet, fast, minimal"*, and four live chips are quieter than five with one struck
through. This is the alternative most likely to be right if §3 turns out wrong, and it is the same
one predicate away.

**Ship `TASK-120908`'s `floor × 4/3`, `× 1.5`, `× 2` and rename the chips** — `+33%`, `+50%`, `2×`.
*Its strongest case:* it needs no pot at the bar at all, so `ADR-0100` §6's thread and this whole
decision disappear; it cannot misstate a game quantity because it names none; and it would have
shipped the original two-file ticket days ago. **Why it lost:** it turns a poker product's sizing row
into arithmetic about the server's minimum, a quantity no player is thinking about, and the
multiples drift from the pot without bound — deep on the river, `2 × minRaiseTo` can be a fraction
of the pot or several times it, so the row would be useless exactly where sizing matters most. It
also contradicts the card, which names three of its five chips for the pot.

**Answer the stepper's step here too, so the whole control is settled in one ADR.** *Its strongest
case:* `ADR-0100` expected it here, the rewrite may want it, and leaving it open risks a coder
picking `step={1}` inside a ticket where nobody will find it — the failure mode this role exists to
prevent. **Why it lost:** nothing in §§1–5 implies a step, and neither the vision nor the rules names
one; an answer would be invented, and an invented answer dressed as a derived one is worse than an
open row. Registering `DEC-102` (§6) defends against the quiet-invention risk at the cost of one
row, and unlike an invented step it can be answered by whoever writes the stepper, with the
no-limit constraint §6 hands them.
